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
import com.doubleyellow.scoreboard.dialog.MyDialogBuilder;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.model.JSONKey;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.model.Player;
import com.doubleyellow.scoreboard.prefs.PreferenceKeys;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.scoreboard.util.BatteryInfo;
import com.doubleyellow.scoreboard.vico.IBoard;
import com.doubleyellow.util.DateUtil;
import com.doubleyellow.util.MapUtil;
import com.doubleyellow.util.Params;
import com.doubleyellow.util.Placeholder;
import com.doubleyellow.util.StringUtil;

//import org.eclipse.paho.android.service.MqttAndroidClient;
import info.mqtt.android.service.Ack;
import info.mqtt.android.service.MqttAndroidClient;
import info.mqtt.android.service.MqttTraceHandler;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONException;
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
 * double-yellow/${FeedName}/${BrandOrSport}/${DeviceId}/match                             for live-score support
 *
 * Subscribe to
 * double-yellow/${FeedName}/${BrandOrSport}/${OtherDeviceId}                              for mirroring a fixed device (being slave), receiving commands or complete match model as json
 * double-yellow/${FeedName}/${BrandOrSport}/${DeviceId}/requestCompleteJsonOfMatch        for mirroring a fixed device (master), to react to a request of a slave to sync the entire match model
 * double-yellow/${FeedName}/${BrandOrSport}/+                                             for live-score of multiple matches of single sport
 * double-yellow/${FeedName}/#                                                             for live-score of multiple matches
 *
 * Subscribe to
 * double-yellow/${BrandOrSport}/${DeviceId}/remoteControl                                      to allow 'send' new match to this device from tournament table
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
                  ScoreBoard m_scoreboard = null;
                  IBoard     m_iBoard     = null;
            final String     m_sBrokerUrl;

            final String     m_joinerLeaverTopic;
            String     m_thisDeviceId;
            String     m_otherDeviceId;
    private int m_iPublishDeviceInfoEveryXSeconds = 60;

    enum JoinerLeaver {
        join,             // 'publish' : first parameter 'this device id'
        thanksForJoining, // 'publish' : first parameter 'this device id', second parameter 'just joined other device'
        leave
    }
    enum TopicPlaceholder {
        DeviceId,
        FeedName
    }


    private MQTTStatus m_status = null;
    private MQTTRole   m_role   = null;

    public void reinit(ScoreBoard context, IBoard iBoard, MQTTStatus status) {
        m_scoreboard = context;
        m_iBoard  = iBoard;
        m_status  = status;
        deriveRoleFromSettings(context);

        defaultCbPublish    .reinit(context);
        defaultCbDisconnect .reinit(context);
        defaultCbUnSubscribe.reinit(context);

        addActionReceiver(context);
    }

    public MQTTHandler(ScoreBoard context, IBoard iBoard, String serverURI, MQTTStatus status) {
        m_scoreboard = context;
        m_iBoard     = iBoard;
        m_status     = status;

        addActionReceiver(context);

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

        deriveRoleFromSettings(context);

        m_iPublishDeviceInfoEveryXSeconds = PreferenceValues.mqttPublishDeviceInfoEveryXSeconds(m_scoreboard);

        String clientID = /*Brand.getShortName(context) + "." +*/ m_thisDeviceId;
        m_joinerLeaverTopic = doMQTTTopicTranslation(PreferenceValues.getMQTTPublishJoinerLeaverTopic(context), null) ;

        //mqttClient = new MqttAndroidClient(context, serverURI, clientID, Ack.AUTO_ACK, null, true, 1); // version 4.3 of https://github.com/hannesa2/paho.mqtt.android
        //mqttClient = new MqttAndroidClient(context, serverURI, clientID, Ack.AUTO_ACK); // uses MqttDefaultFilePersistence and seems to throw MqttPersistenceException
        mqttClient = new MqttAndroidClient(context, serverURI, clientID, Ack.AUTO_ACK, new MemoryPersistence(), true, 1);

        if ( false && PreferenceValues.currentDateIsTestDate() ) {
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

    private void deriveRoleFromSettings(ScoreBoard context) {
        m_thisDeviceId  = PreferenceValues.getLiveScoreDeviceId(context);
        m_otherDeviceId = PreferenceValues.getMQTTOtherDeviceId(context);
        if ( EnumSet.of(MQTTStatus.OnActivityResume, MQTTStatus.CloseSelectDeviceDialog).contains(m_status) ) {
            if ( StringUtil.isNotEmpty(m_otherDeviceId) ) {
                m_role = MQTTRole.Slave;
            } else {
                m_role = MQTTRole.Master;
            }
            PreferenceValues.setRole(m_role);
        }
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

            updateMQTTConnectionStatus(View.VISIBLE, 1);
            Log.d(TAG, "Connected made visible to user in icon");

            if ( StringUtil.isNotEmpty(m_joinerLeaverTopic) ) {
                subscribe(m_joinerLeaverTopic, JoinerLeaver.join  + "(" + m_thisDeviceId + "," + m_status + ")");
                Log.d(TAG, "Subscribing to " + m_joinerLeaverTopic);
                //publish(m_joinerLeaverTopic, JoinerLeaver.join  + "(" + m_thisDeviceId + ")");
            }

            if ( m_status.equals(MQTTStatus.OpenSelectDeviceDialog) ) {
                // not interested in other actions just yet
            } else {
                if ( true /*EnumSet.of(MQTTRole.Slave, MQTTRole.Master).contains(m_role)*/ ) {
                    // listen for 'clients' to request the complete json of a match specifically of this device
                    // for now do this for both master and slave
                    final String mqttRespondToTopic = getMQTTSubscribeTopic_Change(BTMethods.requestCompleteJsonOfMatch.toString());
                    if ( StringUtil.isNotEmpty(mqttRespondToTopic) ) {
                        Log.d(TAG, "Subscribing to " + mqttRespondToTopic);
                        subscribe(mqttRespondToTopic, null);
                    }
                }

                if ( EnumSet.of(MQTTRole.Master).contains(m_role) ) {
                    final String mqttRespondToTopic_remoteControl = getMQTTSubscribeTopic_remoteControl();
                    if ( StringUtil.isNotEmpty(mqttRespondToTopic_remoteControl) ) {
                        Log.d(TAG, "Subscribing to " + mqttRespondToTopic_remoteControl);
                        subscribe(mqttRespondToTopic_remoteControl, null);
                    }
                }

                final String mqttSubScribeToTopic_Change = getMQTTSubscribeTopic_Change(null);
                if ( StringUtil.isNotEmpty(mqttSubScribeToTopic_Change) ) {

                    if ( EnumSet.of(MQTTRole.Slave).contains(m_role) ) {
                        // listen for changes on
                        Log.d(TAG, "Subscribing to " + mqttSubScribeToTopic_Change);
                        subscribe(mqttSubScribeToTopic_Change, null);

                        //changeMQTTRole(MQTTRole.Slave);
                        publishOnMQTT(BTMethods.requestCompleteJsonOfMatch, m_otherDeviceId);
                    }
                } else {
                    m_scoreboard.showInfoMessageOnUiThread(m_scoreboard.getString(R.string.sb_MQTT_Connected_to_x, m_sBrokerUrl), 10);
                }
                if ( m_scoreboard.m_liveScoreShare ) {
                    publishMatchOnMQTT(ScoreBoard.getMatchModel(), false, null);
                }
            }
        }

        @Override public void onFailure(IMqttToken token, Throwable exception) {
            String sException = String.valueOf(exception);
            if ( sException.equals(MqttException.class.getSimpleName() + "(0)") ) {
                sException = exception.getClass().getSimpleName(); // e.g. MqttPersistenceException
                exception.printStackTrace();
            }
            Log.w(TAG, "onFailure " + sException + " " + this);

            String sMsg = m_scoreboard.getString(R.string.sb_MQTT_Connection_to_x_failed_y, m_sBrokerUrl, sException);
            stop();

            if ( m_iReconnectAttempts < 10 ) {
                if ( m_scoreboard.doDelayedMQTTReconnect(sMsg,11, m_iReconnectAttempts, MQTTStatus.RetryConnection)  ) {
                    m_iReconnectAttempts++;
                }
            } else {
                PreferenceValues.setOverwrite(PreferenceKeys.UseMQTT, false);
                m_scoreboard.stopMQTT();
                updateMQTTConnectionStatus(View.INVISIBLE, -1);
                m_scoreboard.showInfoMessageOnUiThread(m_scoreboard.getString(R.string.sb_MQTT_TurnedOff_ToManyFailedReconnectAttempts), 10);
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
        updateMQTTConnectionStatus(View.VISIBLE, 0);
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
    public Params getStats() {
        return stats;
    }

    private final Params stats = new Params();
    public void updateStats(String sTopic, String sPublishOrReceive) {
        this.stats.increaseCounter(sTopic + "." + sPublishOrReceive + ".count");
        this.stats.put            (sTopic + "." + sPublishOrReceive +  ".last", DateUtil.getCurrentHHMMSS());

        this.publishBatteryStatus();
    }

    private long lBatteryStatusLastSend = 0L;
    private void publishBatteryStatus() {
        long lNow = System.currentTimeMillis();
        if ( lNow - lBatteryStatusLastSend > m_iPublishDeviceInfoEveryXSeconds * 1000L ) {
            lBatteryStatusLastSend = lNow;
            Map<String, Object> info = BatteryInfo.getInfo(m_scoreboard);
            if  ( MapUtil.isNotEmpty(info) ) {
                String sTopicPH = PreferenceValues.getMQTTPublishTopicDeviceInfo(m_scoreboard);
                String sTopic = doMQTTTopicTranslation(sTopicPH, m_thisDeviceId);
                info.put(JSONKey.device.toString(), m_thisDeviceId);
                publish(sTopic, (new JSONObject(info)).toString(), false);
            }
        }
    }

    public boolean publish(String topic, String msg, boolean bRetain) {
        if ( topic == null ) {
            Log.w(TAG, "Topic can not be null");
            return false;
        }
        this.updateStats(topic, "publish");

        MqttMessage message = new MqttMessage();
        message.setPayload(msg.getBytes());
        message.setQos(MQTT_QOS);
        message.setRetained(bRetain);
        try {
            mqttClient.publish(topic, message, null, defaultCbPublish);
            return true;
        } catch (Exception e) {
            // Seen in playstore: java.lang.IllegalArgumentException
            Log.e(TAG, e.getMessage(), e);
            return false;
        }
    }

/*
    void changeMQTTRole(MQTTRole role) {
        if ( role.equals(m_MQTTRole) ) {
            return;
        }
        if ( role.equals(MQTTRole.Slave) ) {
            PreferenceValues.setOverwrites(ScoreBoard.mBtPrefSlaveSettings);
        }
        if ( role.equals(MQTTRole.Master) ) {
            PreferenceValues.removeOverwrites(ScoreBoard.mBtPrefSlaveSettings.keySet());
        }
        m_role = role;
        m_scoreboard.showInfoMessageOnUiThread("MQTT role " + m_MQTTRole, 2);
    }
*/

    public MQTTRole getRole() {
        return m_role;
    }

    private String getMQTTSubscribeTopic_Change(String sMethod) {
        String sPlaceholder = PreferenceValues.getMQTTSubscribeTopic_Change(m_scoreboard);
        String sSubTopic = "";

        String sDevice = null;
        if ( BTMethods.requestCompleteJsonOfMatch.toString().equals(sMethod) ) {
            sDevice = "+"; // m_thisDeviceId;
            sSubTopic = MQTT_TOPIC_CONCAT_CHAR + sMethod;
        } else {
            sDevice = m_otherDeviceId;
        }
        String sValue = doMQTTTopicTranslation(sPlaceholder, sDevice);
        if ( StringUtil.isEmpty(sValue) ) {
            Log.w(TAG, "Error in translation? " + sPlaceholder + " " + sDevice);
            return null;
        }
        return sValue + sSubTopic;
    }

    public String getMQTTSubscribeTopic_remoteControl() {
        String sPlaceholder = PreferenceValues.getMQTTSubscribeTopic_remoteControl(m_scoreboard);

        return doMQTTTopicTranslation(sPlaceholder, m_thisDeviceId);
    }

    public boolean publishOnMQTT(BTMethods method, Object... args) {
        if ( isConnected() == false ) {
            return false;
        }

        if ( MQTTRole.Slave.equals(m_role) ) {
            // do only publish subset of methods
            if ( EnumSet.of(BTMethods.changeScore, BTMethods.timestampStartOfGame, BTMethods.startTimer, BTMethods.cancelTimer).contains(method) ) {
                Log.d(TAG, "Not publishing as slave : " + method);
                return false;
            }
        }

        if ( BTMethods.changeScore.equals(method) ) {
            // TODO: only change role if we know someone is listening and therefor will be slave
            //  List<String> lOthers = ListUtil.filter(m_lSubscriptions, "/change", Enums.Match.Keep);
            //changeMQTTRole(MQTTRole.Master);
        }

        StringBuilder sb = new StringBuilder();
        m_scoreboard.addMethodAndArgs(sb, method, args);
        final String sMessage = sb.toString();

        //Log.d(TAG, "About to write BT message " + sMessage.trim());
        boolean bUseOtherDeviceId = EnumSet.of(BTMethods.requestCompleteJsonOfMatch).contains(method);
        String changeTopic = getMQTTPublishTopicChange(false);
        if ( bUseOtherDeviceId ) {
            changeTopic += MQTT_TOPIC_CONCAT_CHAR + method;
        }
        return publish(changeTopic, sMessage, false);
    }

    public void publishUnloadMatchOnMQTT(Model matchModel) {
        String sJson = matchModel.toJsonString(m_scoreboard);
        String matchTopic = getMQTTPublishTopicUnloadMatch();
        publish(matchTopic, sJson, true);
    }

    public MQTTRole publishMatchOnMQTT(Model matchModel, boolean bPrefixWithJsonLength, JSONObject oTimerInfo) {
        if ( isConnected() == false ) {
            return null;
        }
        if ( MQTTRole.Slave.equals(m_role) ) {
            Log.d(TAG, "Not publishing match as slave : " + bPrefixWithJsonLength + " " + oTimerInfo);
            return m_role;
        }
        List<String> lSkipKeys = bPrefixWithJsonLength ? null : PreferenceValues.getMQTTSkipJsonKeys(m_scoreboard);
        String matchTopic = getMQTTPublishTopicMatch();
        String sJson = matchModel.toJsonString(m_scoreboard, null, oTimerInfo, lSkipKeys);
        if ( bPrefixWithJsonLength ) {
            // typically so the published data is the same as for bluetooth mirror messages
            sJson = sJson.length() + ":" + sJson;
            matchTopic = getMQTTPublishTopicChange(false);
        }
        if ( publish(matchTopic, sJson, true) ) {
            return m_role;
        } else {
            return null;
        }
    }

    private String getMQTTPublishTopicMatch() {
        String sPlaceholder = PreferenceValues.getMQTTPublishTopicMatch(m_scoreboard);

        String sDevice = m_thisDeviceId;
        String sValue = doMQTTTopicTranslation(sPlaceholder, sDevice);
        return sValue;
    }
    private String getMQTTPublishTopicUnloadMatch() {
        String sPlaceholder = PreferenceValues.getMQTTPublishTopicUnloadMatch(m_scoreboard);

        String sDevice = m_thisDeviceId;
        String sValue = doMQTTTopicTranslation(sPlaceholder, sDevice);
        return sValue;
    }

    private String getMQTTPublishTopicChange(boolean bUseOtherDeviceId) {
        String sPlaceholder = PreferenceValues.getMQTTPublishTopicChange(m_scoreboard);

        String sDevice = m_thisDeviceId;
        if ( bUseOtherDeviceId ) {
            sDevice = m_otherDeviceId;
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
            sEvent = PreferenceValues.getMatchesFeedName(m_scoreboard);
            if ( StringUtil.isNotEmpty(sEvent) ) {
                sEvent = sEvent.replaceAll("[^0-9A-Za-z_-]", "");
            }
        }

        Map mValues = MapUtil.getMap
                ("Brand", Brand.getShortName(m_scoreboard)
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

    private final static Map<String, MQTTRemoteActionReceiver> m_actionReceivers = new HashMap<>();
    public static void addActionReceiver(MQTTRemoteActionReceiver actionReceiver) {
        m_actionReceivers.put(actionReceiver.getClass().getSimpleName(), actionReceiver);
    }
    public static void removeActionReceiver(MQTTRemoteActionReceiver actionReceiver) {
        m_actionReceivers.remove(actionReceiver.getClass().getSimpleName());
    }
    public void interpretReceivedMessageOnUiThread(String readMessage, MQTTAction mqttAction, String sTopic) {
        for(MQTTRemoteActionReceiver actionReceiver:m_actionReceivers.values()) {
            actionReceiver.interpretMQTTReceivedMessage(readMessage, mqttAction, sTopic);
        }
    }
    private void updateMQTTConnectionStatus(int visibility, int nrOfWhat) {
        for(MQTTRemoteActionReceiver actionReceiver:m_actionReceivers.values()) {
            actionReceiver.updateMQTTConnectionStatus(visibility, nrOfWhat);
        }
    }

    /** returns a string message if NOT accepted, a JSONObject if accepted */
    public static Object acceptAction(MQTTAction mqttAction, String message) {
        if ( mqttAction.equals(MQTTAction.message) ) {
            JSONObject joMessage = null;
            try {
                joMessage = new JSONObject(message);
                if ( joMessage.has(JSONKey.Message.toString()) == false ) {
                    return String.format("To send a message your JSON should at least have a key %1$s. Optionally also a key %2$s", JSONKey.Message.toString(), JSONKey.Duration.toString());
                }
            } catch (JSONException e) {
                return e.getMessage();
            }
            return joMessage;
        }
        boolean bCheckMessageIsValidMatch = false;
        if ( mqttAction.equals(MQTTAction.newMatch_Force) ) {
            bCheckMessageIsValidMatch = true;
        }
        if ( mqttAction.equals(MQTTAction.newMatch) ) {
            bCheckMessageIsValidMatch = true;
            Model matchModel = ScoreBoard.getMatchModel();
            boolean bMatchInProgress = matchModel != null && (matchModel.hasStarted()) && (matchModel.matchHasEnded() == false);
            if ( bMatchInProgress ) {
                return "Match in progress: " + matchModel.getName(Player.A) + "-" + matchModel.getName(Player.B) + ". "
                     + "Current score: " + matchModel.getGameScores() + ". "
                     + "Started: " + matchModel.getMatchStartTimeHH_Colon_MM();
            }
        }
        if ( bCheckMessageIsValidMatch ) {
            try {
                JSONObject joMatch = new JSONObject(message);

                boolean bInvalid = false;

                // check if most important key exists
                if ( joMatch.has(JSONKey.players.toString()) ) {
                    JSONObject joPlayers = joMatch.getJSONObject(JSONKey.players.toString());
                    if ( joPlayers.has(Player.A.toString()) && joPlayers.has(Player.B.toString()) ) {

                    } else {
                        bInvalid = true;
                    }
                } else {
                    bInvalid = true;
                }
                if ( bInvalid ) {
                    return String.format("JSON of %1$s must at least specify '%2$s', "
                                    + "optionally also '%3$s', '%4$s' and '%5$s', all with subkeys A and B. Additionally allowed: 'court', 'event.(name|division|round|location)'  "
                            , mqttAction
                            , JSONKey.players
                            , JSONKey.clubs, JSONKey.colors, JSONKey.countries);
                }

                return joMatch;
            } catch (JSONException e) {
                return e.getMessage();
            }
        }
        return null;
    }

    private class SubscribeCallback implements IMqttActionListener
    {
        Map<String,String> m_mOnSubscribeSuccessPublish = new HashMap<>();

        @Override public void onSuccess(IMqttToken token) {
            String[] topics = token.getTopics();
            String sMsg = m_scoreboard.getString(R.string.sb_MQTT_Subscribed_to_x, shorten(topics));
            m_scoreboard.showInfoMessageOnUiThread(sMsg, 10);
            updateMQTTConnectionStatus(View.VISIBLE, 1);
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
            String sMsg = m_scoreboard.getString(R.string.sb_MQTT_Subscription_to_x_failed_y, shorten(token.getTopics()), exception.toString());
            MyDialogBuilder.dialogWithOkOnly(m_scoreboard, m_scoreboard.getString(R.string.pref_Category_MQTT) , sMsg, true);
        }
    }

    private String shorten(String s) {
        return shorten(new String[] { s });
    }
    private String shorten(String [] sa) {
        return Arrays.toString(sa).replaceAll("[\\[\\]]", "").replaceAll("double-yellow/" + Brand.getShortName(m_scoreboard), "");
    }
}