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
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

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
    private static final int MQTT_QOS = 0;
    private final MqttAndroidClient mqttClient;
    private final IMqttActionListener defaultCbPublish     ;
    private final IMqttActionListener defaultCbDisconnect  ;
    private final IMqttActionListener defaultCbUnSubscribe ;
    private final IMqttActionListener defaultSubscribe     ;
                  ScoreBoard m_context    = null;
                  IBoard     m_iBoard     = null;
                  String     m_sBrokerUrl = null;
            final String     m_joinerLeaverTopic;
            final String     m_thisDeviceId;

    enum JoinerLeaver {
        join,
        leave
    }

    public MQTTHandler(ScoreBoard context, IBoard iBoard, String serverURI, String clientID) {
        m_context    = context;
        m_iBoard     = iBoard;
        m_sBrokerUrl = serverURI;

        m_thisDeviceId      = PreferenceValues.getLiveScoreDeviceId(context);
        m_joinerLeaverTopic = doMQTTTopicTranslation(PreferenceValues.getMQTTJoinerLeaverTopicPrefix(context) + "/" + JoinerLeaver.class.getSimpleName(), null) ;

        //mqttClient = new MqttAndroidClient(context, serverURI, clientID, Ack.AUTO_ACK, null, true, 1); // version 4.3 of https://github.com/hannesa2/paho.mqtt.android
        //mqttClient = new MqttAndroidClient(context, serverURI, clientID, Ack.AUTO_ACK); // uses MqttDefaultFilePersistence and seems to throw MqttPersistenceException
        mqttClient = new MqttAndroidClient(context, serverURI, clientID, Ack.AUTO_ACK, new MemoryPersistence());

        defaultCbPublish     = new MQTTActionListener("Publish"    , iBoard, m_sBrokerUrl);
        defaultCbDisconnect  = new MQTTActionListener("Disconnect" , iBoard, m_sBrokerUrl);
        defaultCbUnSubscribe = new MQTTActionListener("UnSubscribe", iBoard, m_sBrokerUrl);
        defaultSubscribe     = new SubscribeCallback();
    }

    private class ConnectCallback implements IMqttActionListener {
        @Override public void onSuccess(IMqttToken token) {
            // start listening for published messages
            ClientCallback callback = ClientCallback.getInstance(MQTTHandler.this);
            mqttClient.setCallback(callback);

            m_iBoard.updateMQTTConnectionStatusIcon(View.VISIBLE, 1);

            if  ( StringUtil.isNotEmpty(m_joinerLeaverTopic) ) {
                subscribe(m_joinerLeaverTopic);
                publish(m_joinerLeaverTopic, JoinerLeaver.join  + "(" + m_thisDeviceId + "," + false + ")");
            }

            // listen for 'clients' to request the complete json of a match specifically of this device
            final String mqttRespondToTopic = getMQTTSubscribeTopicChange(BTMethods.requestCompleteJsonOfMatch.toString());
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
            String sException = String.valueOf(exception);
            if ( sException.equals(MqttException.class.getSimpleName() + "(0)") ) {
                sException = exception.getClass().getSimpleName(); // e.g. MqttPersistenceException
                exception.printStackTrace();
            }
            String sMsg = String.format("ERROR: MQTT Connection to %s failed: %s", m_sBrokerUrl, sException);
            stop();

            m_context.doDelayedMQTTReconnect(sMsg,11);
            //iBoard.updateMQTTConnectionStatusIcon(View.VISIBLE, 0);
        }

    }
    public void connect(String username, String password) {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(username);
        options.setPassword(password.toCharArray());
        options.setConnectionTimeout(5);
        // TODO: finetune ?
        mqttClient.connect(options, null, new ConnectCallback());
    }

    public void stop() {
        m_iBoard.updateMQTTConnectionStatusIcon(View.VISIBLE, 0);
        if ( isConnected() ) {
            publish(m_joinerLeaverTopic, JoinerLeaver.leave + "(" + m_thisDeviceId + ")");
            unsubscribe("#");

            mqttClient.disconnect(null, defaultCbDisconnect); // throw nullpointer exception ?
        }
        m_mJoinedDevices.clear();
    }

    public boolean isConnected() {
        return mqttClient.isConnected();
    }

    private final Map<String, Long> m_lSubscriptions = new TreeMap<>();
    final Map<String, Long> m_mJoinedDevices = new TreeMap<>();
    private void subscribe(String topic) {
        if ( m_lSubscriptions.containsKey(topic) ) {
            Log.w(TAG, "already subscribed to " + shorten(topic));
            return;
        }
        m_lSubscriptions.put(topic, System.currentTimeMillis());
        mqttClient.subscribe(topic, MQTT_QOS, null, defaultSubscribe);
    }

    public void unsubscribe(String topic) {
        if ( topic.equals("#") ) {
            m_lSubscriptions.clear();
        } else {
            m_lSubscriptions.remove(topic);
        }
        mqttClient.unsubscribe(topic, null, defaultCbUnSubscribe);
    }
    public Set<String> getSubscriptionTopics() {
        return m_lSubscriptions.keySet();
    }
    public Set<String> getJoinedDevices() {
        return new LinkedHashSet<>(m_mJoinedDevices.keySet());
    }

    public void publish(String topic, String msg) {
        if ( topic == null ) {
            Log.w(TAG, "Topic can not be null");
            return;
        }
        MqttMessage message = new MqttMessage();
        message.setPayload(msg.getBytes());
        message.setQos(MQTT_QOS);
        message.setRetained(false);
        mqttClient.publish(topic, message, null, defaultCbPublish);
    }


    private BTRole m_MQTTRole = null;
    void changeMQTTRole(BTRole role) {
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

    private String getMQTTSubscribeTopicChange(String sMethod) {
        String sPlaceholder = PreferenceValues.getMQTTSubscribeTopicChange(m_context);
        String sSubTopic = "";

        String sDevice = PreferenceValues.getMQTTOtherDeviceId(m_context);
        if ( BTMethods.requestCompleteJsonOfMatch.toString().equals(sMethod) ) {
            sDevice = "+"; // m_thisDeviceId;
            sSubTopic = MQTT_TOPIC_CONCAT_CHAR + sMethod;
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
            // TODO: only change role if we know someone is listening and therefor will be slave
            //  List<String> lOthers = ListUtil.filter(m_lSubscriptions, "/change", Enums.Match.Keep);
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

        String sDevice = m_thisDeviceId;
        String sValue = doMQTTTopicTranslation(sPlaceholder, sDevice);
        return sValue;
    }

    private String getMQTTPublishTopicChange(boolean bUseOtherDeviceId) {
        String sPlaceholder = PreferenceValues.getMQTTPublishTopicChange(m_context);

        String sDevice = m_thisDeviceId;
        if ( bUseOtherDeviceId ) {
            sDevice = PreferenceValues.getMQTTOtherDeviceId(m_context);
        }
        String sValue = doMQTTTopicTranslation(sPlaceholder, sDevice);
        return sValue;
    }

    private String doMQTTTopicTranslation(String sPlaceholder, String sDeviceId) {
        // subscribe to any message from specific device
        if ( StringUtil.isEmpty(sDeviceId) && sPlaceholder.contains("${DeviceId}") ) {
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
            String sMsg = String.format("MQTT Subscribed to %s on %s", shorten(token.getTopics()), m_sBrokerUrl);
            m_iBoard.showInfoMessage(sMsg, 10);
            m_iBoard.updateMQTTConnectionStatusIcon(View.VISIBLE, 1);
        }

        @Override public void onFailure(IMqttToken token, Throwable exception) {
            String sMsg = String.format("ERROR: MQTT Subscribed to %s on %s failed with %s", shorten(token.getTopics()), m_sBrokerUrl, exception.toString());
            MyDialogBuilder.dialogWithOkOnly(m_context, m_context.getString(R.string.pref_Category_MQTT) , sMsg, true);
        }
    }

    private String shorten(String s) {
        return shorten(new String[] { s });
    }
    private String shorten(String [] sa) {
        return Arrays.toString(sa).replaceAll("[\\[\\]]", "").replaceAll("double-yellow/" + Brand.getShortName(m_context), "");
    }
}

