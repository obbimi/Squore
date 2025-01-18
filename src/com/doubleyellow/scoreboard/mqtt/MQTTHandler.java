/*
 * Copyright (C) 2025  Iddo Hoeve
 *
 * Squore is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.doubleyellow.scoreboard.mqtt;

import android.view.View;
import android.util.Log;

//import org.eclipse.paho.android.service.MqttAndroidClient;
import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.bluetooth.BTMethods;
import com.doubleyellow.scoreboard.bluetooth.BTRole;
import com.doubleyellow.scoreboard.bluetooth.MessageSource;
import com.doubleyellow.scoreboard.dialog.GenericMessageDialog;
import com.doubleyellow.scoreboard.dialog.MyDialogBuilder;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.scoreboard.vico.IBoard;
import com.doubleyellow.util.MapUtil;
import com.doubleyellow.util.Placeholder;
import com.doubleyellow.util.StringUtil;

import info.mqtt.android.service.Ack;
import info.mqtt.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.*;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Publish to
 * double-yellow/${EventId}/${BrandOrSport}/${deviceid}                                   publish commands (acting as master) that slave should execute keep score in sync
 * double-yellow/${EventId}/${BrandOrSport}/${otherdeviceid}/requestCompleteJsonOfMatch   for mirroring a fixed device (acting as slave), when model appears out of sync
 *
 * Subscribe to
 * double-yellow/${EventId}/${BrandOrSport}/${otherdeviceid}                              for mirroring a fixed device (being slave), receiving commands or complete match model as json
 * double-yellow/${EventId}/${BrandOrSport}/${deviceid}/requestCompleteJsonOfMatch        for mirroring a fixed device (master), to react to a request of a slave to sync the entire match model
 * double-yellow/${EventId}/${BrandOrSport}/+                                             for livescore of multiple matches of single sport
 * double-yellow/${EventId}/#                                                             for livescore of multiple matches
 *
 * 'EventId' (optional) to allow using a public broker like 'tcp://broker.hivemq.com:1883' and ensure you only receive info from a certain subset of matches
 * 'BrandOrSport' (optional) to allow subscribing only to a squash matches or only badminton matches
 *
 */
public class MQTTHandler
{
    private static final String TAG = "SB." + MQTTHandler.class.getSimpleName();
    private static final int MQTT_QOS = 1;
    private final MqttAndroidClient mqttClient;
    private final IMqttActionListener defaultCbPublish     ;
    private final IMqttActionListener defaultCbDisconnect  ;
    private final IMqttActionListener defaultCbUnSubscribe ;
    private final IMqttActionListener defaultSubscribe     ;
    private ScoreBoard m_context = null;
    private IBoard     m_iBoard = null;
    private String     m_sBrokerUrl = null;

    public MQTTHandler(ScoreBoard context, IBoard iBoard, String serverURI, String clientID) {
        m_context    = context;
        m_iBoard     = iBoard;
        m_sBrokerUrl = serverURI;

        //mqttClient = new MqttAndroidClient(context, serverURI, clientID, Ack.AUTO_ACK, null, true, 1); // version 4.3 of https://github.com/hannesa2/paho.mqtt.android
        mqttClient = new MqttAndroidClient(context, serverURI, clientID, Ack.AUTO_ACK);

        defaultCbPublish     = new MQTTActionListener("Publish"    , iBoard);
        defaultCbDisconnect  = new MQTTActionListener("Disconnect" , iBoard);
        defaultCbUnSubscribe = new MQTTActionListener("UnSubscribe", iBoard);
        defaultSubscribe     = new SubscribeCallback();
    }

    private class ConnectCallback implements IMqttActionListener {
        @Override public void onSuccess(IMqttToken token) {
            m_iBoard.updateMQTTConnectionStatusIcon(View.VISIBLE, 1);

            // listen for 'clients' to request the complete json of a match specifically of this device
            final String mqttRespondToTopic = getMQTTSubscribeTopicChange(BTMethods.requestCompleteJsonOfMatch);
            if  ( StringUtil.isNotEmpty(mqttRespondToTopic) ) {
                subscribe(mqttRespondToTopic);
            }

            final String mqttSubScribeToOtherTopic = getMQTTSubscribeTopicChange(null);
            if ( StringUtil.isNotEmpty(mqttSubScribeToOtherTopic) ) {

                // listen for changes on
                subscribe(mqttSubScribeToOtherTopic);
            } else {
                m_iBoard.showInfoMessage(String.format("MQTT Connected to %s OK", m_sBrokerUrl), 10);
            }
            if ( m_context.m_liveScoreShare ) {
                publishMatchOnMQTT(ScoreBoard.getMatchModel(), false, null);
            }
        }

        @Override public void onFailure(IMqttToken token, Throwable exception) {
            if ( m_context.isDialogShowing() == false ) {
                // TODO: after a view dialog revert to just toast...
                // TODO: dialog with 'Turn off MQTT'
                String sMsg = String.format("ERROR: MQTT Connection to %s failed: %s", m_sBrokerUrl, exception.toString());
                GenericMessageDialog dialog = new GenericMessageDialog(m_context);
                dialog.init(m_context.getString(R.string.pref_Category_MQTT), sMsg);
                //m_context.addToDialogStack(dialog);
                dialog.show();
            }
            stop();

            String sMsg = String.format("ERROR: MQTT Connection to %s failed", m_sBrokerUrl);
            m_context.doDelayedMQTTReconnect(sMsg,30);
            //iBoard.updateMQTTConnectionStatusIcon(View.VISIBLE, 0);
        }

    }
    public void connect(String username, String password) {
        mqttClient.setCallback(new ClientCallback());

        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(username);
        options.setPassword(password.toCharArray());
        mqttClient.connect(options, null, new ConnectCallback());
    }

    public void stop() {
        m_iBoard.updateMQTTConnectionStatusIcon(View.VISIBLE, 0);
        if ( isConnected() ) {
            unsubscribe("#");
            disconnect();
        }
    }

    public boolean isConnected() {
        return mqttClient.isConnected();
    }

    private final List<String> m_lSubscriptions = new ArrayList<>();
    private void subscribe(String topic) {
        m_lSubscriptions.add(topic);
        mqttClient.subscribe(topic, MQTT_QOS, null, defaultSubscribe);
    }

    public void unsubscribe(String topic) {
        m_lSubscriptions.remove(topic);
        mqttClient.unsubscribe(topic, null, defaultCbUnSubscribe);
    }
    public List<String> getSubscriptionTopics() {
        return m_lSubscriptions;
    }


    public void publish(String topic, String msg) {
        MqttMessage message = new MqttMessage();
        message.setPayload(msg.getBytes());
        message.setQos(MQTT_QOS);
        message.setRetained(false);
        mqttClient.publish(topic, message, null, defaultCbPublish);
    }

    public void disconnect() {
        mqttClient.disconnect(null, defaultCbDisconnect);
    }

    /** keep track of what message was received when, mainly to prevent endless loop when 2 devices are subscribed to each other to mirror each other */
    private final Map<String, Long> m_MQTTmessageReceived = new HashMap<>();

    private class ClientCallback implements MqttCallback {

        @Override public void messageArrived(String topic, MqttMessage message) throws Exception {
            String msg = String.format("MQTT Received: %s [%s]", message.toString(), topic );
            Log.d(TAG, msg);

            if ( topic.endsWith(PreferenceValues.getFCMDeviceId(m_context)) ) {
                // show but ignore for further processing, messages published by this device itself
                m_iBoard.showInfoMessage(msg, 1);
            } else {
                String sPayload = new String(message.getPayload());
                long lNow = System.currentTimeMillis();
                if ( m_MQTTmessageReceived.containsKey(sPayload) ) {
                    long lReceivedPrev = m_MQTTmessageReceived.get(sPayload);
                    if ( lNow - lReceivedPrev < 2000 ) {
                        m_iBoard.showInfoMessage("IGNORE MQTT duplicate: " + sPayload, 2);
                        return;
                    }
                }
                if ( sPayload.startsWith(BTMethods.changeScore.toString()) ) {
                    changeMQTTRole(BTRole.Slave);
                }
                m_MQTTmessageReceived.put(sPayload, lNow);
                m_context.interpretReceivedMessageOnUiThread(sPayload, MessageSource.MQTT);
            }
        }

        @Override public void connectionLost(Throwable cause) {
            m_context.doDelayedMQTTReconnect("WARN: MQTT Connection to broker lost.", 10);
        }
        @Override public void deliveryComplete(IMqttDeliveryToken token) {
            Log.d(TAG, "Delivery complete " + token);
        }
    }

    private BTRole m_MQTTRole = null;
    private void changeMQTTRole(BTRole role) {
        if ( role.equals(m_MQTTRole) ) {
            return;
        }
        if ( role.equals(BTRole.Slave) ) {
            PreferenceValues.setOverwrites(m_context.mBtPrefSlaveSettings);
        }
        if ( role.equals(BTRole.Master) ) {
            PreferenceValues.removeOverwrites(m_context.mBtPrefSlaveSettings.keySet());
        }
        m_MQTTRole = role;
        m_iBoard.showInfoMessage("MQTT role " + m_MQTTRole, 2);
    }

    private String getMQTTSubscribeTopicChange(BTMethods btMethod) {
        String sPlaceholder = PreferenceValues.getMQTTSubscribeTopicChange(m_context);
        String sSubTopic = "";

        String sDevice = PreferenceValues.getMQTTOtherDeviceId(m_context);
        if ( EnumSet.of(BTMethods.requestCompleteJsonOfMatch).contains(btMethod) ) {
            sDevice = "+"; // PreferenceValues.getFCMDeviceId(this);
            sSubTopic = MQTT_TOPIC_CONCAT_CHAR + btMethod;
        }
        String sValue = doMQTTTopicTranslation(sPlaceholder, sDevice);
        if ( StringUtil.isEmpty(sValue) ) {
            return null;
        }
        return sValue + sSubTopic;
    }

    public void publishOnMQTT(BTMethods method, Object... args) {
        if ( isConnected() == false ) {
            return;
        }

        if ( BTMethods.changeScore.equals(method) ) {
            changeMQTTRole(BTRole.Master);
        }

        StringBuilder sb = new StringBuilder();
        m_context.addMethodAndArgs(sb, method, args);
        final String sMessage = sb.toString();

        //Log.d(TAG, "About to write BT message " + sMessage.trim());
        boolean bUseOtherDeviceId = EnumSet.of(BTMethods.requestCompleteJsonOfMatch).contains(method);
        String changeTopic = getMQTTPublishTopicChange(false);
        if ( bUseOtherDeviceId ) {
            changeTopic += MQTT_TOPIC_CONCAT_CHAR + method;
        }
        publish(changeTopic, sMessage);
    }

    public void publishMatchOnMQTT(Model matchModel, boolean bPrefixWithJsonLength, JSONObject oTimerInfo) {
        if ( isConnected() == false ) {
            return;
        }
        List<String> lSkipKeys = bPrefixWithJsonLength ? null : PreferenceValues.getMQTTSkipJsonKeys(m_context);
        String matchTopic = getMQTTPublishTopicMatch();
        String sJson = matchModel.toJsonString(m_context, null, oTimerInfo, lSkipKeys);
        if ( bPrefixWithJsonLength ) {
            // typically so the published data is the same as for bluetooth mirror messages
            sJson = sJson.length() + ":" + sJson;
            matchTopic = getMQTTPublishTopicChange(false);
        }
        publish(matchTopic, sJson);
    }

    private String getMQTTPublishTopicMatch() {
        String sPlaceholder = PreferenceValues.getMQTTPublishTopicMatch(m_context);

        String sDevice = PreferenceValues.getFCMDeviceId(m_context);
        String sValue = doMQTTTopicTranslation(sPlaceholder, sDevice);
        return sValue;
    }

    private String getMQTTPublishTopicChange(boolean bUseOtherDeviceId) {
        String sPlaceholder = PreferenceValues.getMQTTPublishTopicChange(m_context);

        String sDevice = PreferenceValues.getFCMDeviceId(m_context);
        if ( bUseOtherDeviceId ) {
            sDevice = PreferenceValues.getMQTTOtherDeviceId(m_context);
        }
        String sValue = doMQTTTopicTranslation(sPlaceholder, sDevice);
        return sValue;
    }

    private String doMQTTTopicTranslation(String sPlaceholder, String sDeviceId) {
        // subscribe to any message from specific device
        if ( StringUtil.isEmpty(sDeviceId) ) {
            return null;
        }
        Map mValues = MapUtil.getMap
                ("Brand", Brand.getShortName(m_context)
                , "DeviceId", sDeviceId
                );
        Placeholder instance = Placeholder.getInstance(TAG);
        String sValue = instance.translate(sPlaceholder, mValues);
               sValue = instance.removeUntranslated(sValue);
               sValue = sValue.replaceAll("//", "/").replaceAll("^/", "");
               sValue = sValue.replaceAll(" ", "");
        return sValue;
    }
    private static final String MQTT_TOPIC_CONCAT_CHAR = "_";

    private class SubscribeCallback implements IMqttActionListener
    {
        @Override public void onSuccess(IMqttToken token) {
            String sMsg = String.format("MQTT Subscribed to %s on %s", Arrays.toString(token.getTopics()), m_sBrokerUrl);
            m_iBoard.showInfoMessage(sMsg, 10);
            m_iBoard.updateMQTTConnectionStatusIcon(View.VISIBLE, 1);
        }

        @Override public void onFailure(IMqttToken token, Throwable exception) {
            String sMsg = String.format("ERROR: MQTT Subscribed to %s failed %s", Arrays.toString(token.getTopics()), exception.toString());
            MyDialogBuilder.dialogWithOkOnly(m_context, m_context.getString(R.string.pref_Category_MQTT) , sMsg, true);
        }
    }
}

