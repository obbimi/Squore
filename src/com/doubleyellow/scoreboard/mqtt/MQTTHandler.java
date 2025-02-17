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

import androidx.annotation.Nullable;

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

//import org.eclipse.paho.android.service.MqttAndroidClient;
import info.mqtt.android.service.Ack;
import info.mqtt.android.service.MqttAndroidClient;
import info.mqtt.android.service.MqttTraceHandler;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Publish to
 * double-yellow/${FeedName}/${BrandOrSport}/${DeviceId}                                   publish commands (acting as master) that slave should execute keep score in sync
 * double-yellow/${FeedName}/${BrandOrSport}/${OtherDeviceId}/requestCompleteJsonOfMatch   for mirroring a fixed device (acting as slave), when model appears out of sync
 *
 * Subscribe to
 * double-yellow/${FeedName}/${BrandOrSport}/${OtherDeviceId}                              for mirroring a fixed device (being slave), receiving commands or complete match model as json
 * double-yellow/${FeedName}/${BrandOrSport}/${DeviceId}/requestCompleteJsonOfMatch        for mirroring a fixed device (master), to react to a request of a slave to sync the entire match model
 * double-yellow/${FeedName}/${BrandOrSport}/+                                             for live-score of multiple matches of single sport
 * double-yellow/${FeedName}/#                                                             for live-score of multiple matches
 *
 * Subscribe to
 * double-yellow/${FeedName}/${BrandOrSport}/${otherdeviceid}                              for mirroring a fixed device (being slave), receiving commands or complete match model as json
 * double-yellow/${FeedName}/${BrandOrSport}/${deviceid}/requestCompleteJsonOfMatch        for mirroring a fixed device (master), to react to a request of a slave to sync the entire match model
 * double-yellow/${FeedName}/${BrandOrSport}/+                                             for livescore of multiple matches of single sport
 * double-yellow/${FeedName}/#                                                             for livescore of multiple matches
 *
 * 'FeedName' (optional) to allow using a public broker like 'tcp://broker.hivemq.com:1883' and ensure you only receive info from a certain subset of matches
 * 'BrandOrSport' (optional) to allow subscribing only to a squash matches or only badminton matches
 *
 */
public class MQTTHandler
{
    private static final String TAG = "SB." + MQTTHandler.class.getSimpleName();
    private static final int MQTT_QOS = 0;
    private static final int MQTT_DEFAULT_PORT  = 1883;
    private static final int MQTTS_DEFAULT_PORT = 8883;
    private final MqttAndroidClient  mqttClient          ;
    private final MQTTActionListener defaultCbPublish    ;
    private final MQTTActionListener defaultCbDisconnect ;
    private final MQTTActionListener defaultCbUnSubscribe;
    private final ConnectCallback    connectCallback     ;
    private final SubscribeCallback  defaultSubscribe    ;
                  ScoreBoard m_context    = null;
                  IBoard     m_iBoard     = null;
            final String     m_sBrokerUrl;

            final String     m_joinerLeaverTopic;
            final String     m_thisDeviceId;

    enum JoinerLeaver {
        join,
        thanksForJoining,
        leave
    }
    enum TopicPlaceholder {
        DeviceId,
        FeedName
    }


    public void reinit(ScoreBoard context, IBoard iBoard) {
        m_context = context;
        m_iBoard = iBoard;

        defaultCbPublish    .reinit(context);
        defaultCbDisconnect .reinit(context);
        defaultCbUnSubscribe.reinit(context);
    }

    public MQTTHandler(ScoreBoard context, IBoard iBoard, String serverURI) {
        m_context    = context;
        m_iBoard     = iBoard;

        // the library we use does not like 'mqtt://' urls without a port. Convert it to a tcp:// version
        if ( serverURI.startsWith("mqtt") ) {
            java.net.URI uriBroker = java.net.URI.create(serverURI);
            int iPort = uriBroker.getPort();
            if ( iPort == -1 ) {
                if ( uriBroker.getScheme().equals("mqtt") ) {
                    iPort = MQTT_DEFAULT_PORT;
                } else if ( uriBroker.getScheme().equals("mqtts") ) {
                    iPort = MQTTS_DEFAULT_PORT;
                }
            }
            serverURI = "tcp" + "://" + uriBroker.getHost() + ":" + iPort;
        }
        m_sBrokerUrl = serverURI;

        m_thisDeviceId  = PreferenceValues.getLiveScoreDeviceId(context);

        String clientID = /*Brand.getShortName(context) + "." +*/ m_thisDeviceId;
        m_joinerLeaverTopic = doMQTTTopicTranslation(PreferenceValues.getMQTTPublishJoinerLeaverTopic(context), null) ;

        //mqttClient = new MqttAndroidClient(context, serverURI, clientID, Ack.AUTO_ACK, null, true, 1); // version 4.3 of https://github.com/hannesa2/paho.mqtt.android
        //mqttClient = new MqttAndroidClient(context, serverURI, clientID, Ack.AUTO_ACK); // uses MqttDefaultFilePersistence and seems to throw MqttPersistenceException
        mqttClient = new MqttAndroidClient(context, serverURI, clientID, Ack.AUTO_ACK, new MemoryPersistence(), true, 1);

        if ( PreferenceValues.currentDateIsTestDate() ) {
            mqttClient.setTraceCallback(new MqttTraceHandler() {
                @Override public void traceDebug(@Nullable String s) {
                    Log.d("MqttTraceHandler", s);
                }

                @Override public void traceError(@Nullable String s) {
                    Log.e("MqttTraceHandler", s);
                }

                @Override public void traceException(@Nullable String s, @Nullable Exception e) {
                    Log.e("MqttTraceHandler", s, e);
                }
            });
            mqttClient.setTraceEnabled(true);
        }

        defaultCbPublish     = new MQTTActionListener("Publish"    , context, m_sBrokerUrl);
        defaultCbDisconnect  = new MQTTActionListener("Disconnect" , context, m_sBrokerUrl);
        defaultCbUnSubscribe = new MQTTActionListener("UnSubscribe", context, m_sBrokerUrl);
        defaultSubscribe     = new SubscribeCallback();
        connectCallback      = new ConnectCallback();
    }

    /**
     * Subscribe to certain topics when connection was successful.
     * Try reconnecting if connection failed.
     */
    private class ConnectCallback implements IMqttActionListener {
        private int m_iReconnectAttempts = 0;
        @Override public void onSuccess(IMqttToken token) {
            // start listening for published messages
            m_iReconnectAttempts = 0;
            ClientCallback callback = ClientCallback.getInstance(MQTTHandler.this);
            mqttClient.setCallback(callback); // pass on published messages to a topic this device is subscribed to, to this callback
            Log.d(TAG, "Connected");

            m_context.updateMQTTConnectionStatusIconOnUiThread(View.VISIBLE, 1);
            Log.d(TAG, "Connected made visible to user in icon");

            if ( StringUtil.isNotEmpty(m_joinerLeaverTopic) ) {
                subscribe(m_joinerLeaverTopic, JoinerLeaver.join  + "(" + m_thisDeviceId + ")");
                Log.d(TAG, "Subscribing to " + m_joinerLeaverTopic);
                //publish(m_joinerLeaverTopic, JoinerLeaver.join  + "(" + m_thisDeviceId + ")");
            }

            // listen for 'clients' to request the complete json of a match specifically of this device
            final String mqttRespondToTopic = getMQTTSubscribeTopicChange(BTMethods.requestCompleteJsonOfMatch.toString());
            if ( StringUtil.isNotEmpty(mqttRespondToTopic) ) {
                subscribe(mqttRespondToTopic, null);
            }

            final String mqttSubScribeToOtherTopic = getMQTTSubscribeTopicChange(null);
            if ( StringUtil.isNotEmpty(mqttSubScribeToOtherTopic) ) {

                // listen for changes on
                subscribe(mqttSubScribeToOtherTopic, null);
            } else {
                m_context.showInfoMessageOnUiThread(m_context.getString(R.string.sb_MQTT_Connected_to_x, m_sBrokerUrl), 10);
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
            Log.w(TAG, "onFailure " + sException + " " + this);

            String sMsg = m_context.getString(R.string.sb_MQTT_Connection_to_x_failed_y, m_sBrokerUrl, sException);
            stop();

            if ( m_iReconnectAttempts < 10 ) {
                if ( m_context.doDelayedMQTTReconnect(sMsg,11, m_iReconnectAttempts)  ) {
                    m_iReconnectAttempts++;
                }
            } else {
                m_context.stopMQTT();
                m_context.updateMQTTConnectionStatusIconOnUiThread(View.INVISIBLE, -1);
                m_context.showInfoMessageOnUiThread(m_context.getString(R.string.sb_MQTT_TurnedOff_ToManyFailedReconnectAttempts), 10);
            }
        }

    }
    public void connect(String username, String password) {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(username);
        options.setPassword(password.toCharArray());
        options.setConnectionTimeout(5);
        //options.setExecutorServiceTimeout(6000);
        // TODO: finetune ?
        mqttClient.connect(options, m_thisDeviceId, connectCallback);
    }

    public void stop() {
        m_context.updateMQTTConnectionStatusIconOnUiThread(View.VISIBLE, 0);
        if ( isConnected() ) {
            publish(m_joinerLeaverTopic, JoinerLeaver.leave + "(" + m_thisDeviceId + ")", false);
            //unsubscribe("#");

            mqttClient.disconnect(m_thisDeviceId, defaultCbDisconnect); // throws nullpointer exception ?
        }
    }

    public boolean isConnected() {
        return mqttClient.isConnected();
    }

    private final Map<String, Long> m_lSubscriptions = new TreeMap<>();
    private void subscribe(String topic, String sPublishOnSuccess) {
        if ( m_lSubscriptions.containsKey(topic) ) {
            Log.w(TAG, "already subscribed to " + shorten(topic));
            return;
        }
        if ( StringUtil.isNotEmpty(sPublishOnSuccess) ) {
            defaultSubscribe.m_mOnSubscribeSuccessPublish.put(topic, sPublishOnSuccess);
        }
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


    public void publish(String topic, String msg, boolean bRetain) {
        if ( topic == null ) {
            Log.w(TAG, "Topic can not be null");
            return;
        }
        MqttMessage message = new MqttMessage();
        message.setPayload(msg.getBytes());
        message.setQos(MQTT_QOS);
        message.setRetained(bRetain);
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
        m_context.showInfoMessageOnUiThread("MQTT role " + m_MQTTRole, 2);
    }

    public BTRole getRole() {
        return m_MQTTRole;
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
        publish(changeTopic, sMessage, false);
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
        publish(matchTopic, sJson, true);
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
        if ( StringUtil.isEmpty(sDeviceId) && sPlaceholder.contains("${" + TopicPlaceholder.DeviceId + "}") ) {
            return null;
        }
        String sEvent = "";
        if ( sPlaceholder.contains("${" + TopicPlaceholder.FeedName + "}") ) {
            sEvent = PreferenceValues.getMatchesFeedName(m_context);
            if ( StringUtil.isNotEmpty(sEvent) ) {
                sEvent = sEvent.replaceAll("[^0-9A-Za-z_-]", "");
            }
        }

        Map mValues = MapUtil.getMap
                ("Brand", Brand.getShortName(m_context)
                , TopicPlaceholder.DeviceId.toString(), sDeviceId
                , TopicPlaceholder.FeedName.toString(), sEvent
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
        Map<String,String> m_mOnSubscribeSuccessPublish = new HashMap<>();

        @Override public void onSuccess(IMqttToken token) {
            String[] topics = token.getTopics();
            String sMsg = m_context.getString(R.string.sb_MQTT_Subscribed_to_x, shorten(topics));
            m_context.showInfoMessageOnUiThread(sMsg, 10);
            m_context.updateMQTTConnectionStatusIconOnUiThread(View.VISIBLE, 1);
            for (int i = 0; i < topics.length; i++) {
                String topic = topics[i];
                m_lSubscriptions.put(topic, System.currentTimeMillis());
                if ( m_mOnSubscribeSuccessPublish.containsKey(topic)) {
                    String sPublishMsg = m_mOnSubscribeSuccessPublish.remove(topic);
                    publish(topic, sPublishMsg, false);
                }
            }
        }

        @Override public void onFailure(IMqttToken token, Throwable exception) {
            String sMsg = m_context.getString(R.string.sb_MQTT_Subscription_to_x_failed_y, shorten(token.getTopics()), exception.toString());
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