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

import android.util.Log;

import com.doubleyellow.scoreboard.bluetooth.BTMethods;
import com.doubleyellow.scoreboard.model.JSONKey;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.util.MapUtil;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

class ClientCallback implements MqttCallback
{
    private ClientCallback() {

    }
    private static ClientCallback m_instance = new ClientCallback();
    private MQTTHandler m_handler = null;
    static ClientCallback getInstance(MQTTHandler handler) {
        m_instance.m_handler = handler;
        return m_instance;
    }
    private static final String TAG = "SB." + ClientCallback.class.getSimpleName();

    /** keep track of what message was received when, mainly to prevent endless loop when 2 devices are subscribed to each other to mirror each other */
    private final Map<String, Long> m_MQTTmessageReceived = new HashMap<>();
    private int m_iDuplicateCount = 0;

    @Override public void messageArrived(String topic, MqttMessage message) throws Exception {
        String msg = String.format("MQTT Received: %s [%s]", message.toString(), topic );
        //Log.d(TAG, msg);

        final String sThisDeviceId = PreferenceValues.getLiveScoreDeviceId(m_handler.m_context);
        MQTTAction mqttAction = null;
        final String sPayload = new String(message.getPayload());

        String sRemoteControlTopic = m_handler.getMQTTSubscribeTopic_remoteControl();
        String sRemoteControlTopicEnd = sRemoteControlTopic.replaceFirst(".*/", "");
        if ( topic.endsWith(sRemoteControlTopicEnd) ) {
            try {
                JSONObject joPayload = new JSONObject(sPayload);
                if ( joPayload.has(JSONKey.players.toString()) ) {
                    mqttAction = MQTTAction.newMatch;
                } else if ( joPayload.has(JSONKey.Message.toString()) ) {
                    mqttAction = MQTTAction.message;
                }
            } catch (JSONException e) {
                Map<String,String> mMessage = MapUtil.getMap(JSONKey.Message.toString(), "Invalid JSON received on " + sRemoteControlTopicEnd, JSONKey.device.toString(), sThisDeviceId);
                m_handler.publish(sRemoteControlTopic.replace("/" + sThisDeviceId, ""), (new JSONObject(mMessage)).toString(), false);
                return;
            }
        }

        if ( mqttAction == null && topic.matches(".*\\b" + sThisDeviceId + "\\b.*") ) {
            // show but ignore for further processing, messages published by this device itself
            m_handler.m_context.showInfoMessageOnUiThread(msg, 1);
        } else {
            m_handler.updateStats(topic, "receive");

            long lNow = System.currentTimeMillis();
            if ( m_MQTTmessageReceived.containsKey(sPayload) ) {
                long lReceivedPrev = m_MQTTmessageReceived.get(sPayload);
                long lMsAgo = lNow - lReceivedPrev;
                if ( lMsAgo < 2000 ) {
                    m_iDuplicateCount++;
                    Log.w(TAG, String.format("IGNORE MQTT duplicate(%d): %s", m_iDuplicateCount, sPayload));
                    if ( m_iDuplicateCount < 16 ) {
                        m_handler.m_context.showInfoMessageOnUiThread("IGNORE MQTT duplicate: " + sPayload, 2);
                    }
                    return;
                }
            }
            if ( topic.endsWith(MQTTHandler.JoinerLeaver.class.getSimpleName()) ) {
                String[] saMethodNArgs = sPayload.trim().split("[\\(\\),]");
                String sMethod     = saMethodNArgs[0];
                String sFromDevice = saMethodNArgs[1];
                String sOtherArg   = saMethodNArgs.length>2?saMethodNArgs[2]:null;

                if ( sMethod.matches("(" + MQTTHandler.JoinerLeaver.join + "|" + MQTTHandler.JoinerLeaver.thanksForJoining +")" ) ) {
                    if ( sFromDevice.equals(m_handler.m_thisDeviceId) ) { return; }
                    if ( m_handler.m_context.updateJoinedDevices(sFromDevice, true) ) {
                        if ( sMethod.equals(MQTTHandler.JoinerLeaver.join.toString() ) ) {
                            // let new joiner know we joined in the past
                            m_handler.publish(m_handler.m_joinerLeaverTopic, MQTTHandler.JoinerLeaver.thanksForJoining  + "(" + m_handler.m_thisDeviceId + "," + sFromDevice + ")", false);

                            if ( sFromDevice.equals(m_handler.m_otherDeviceId) ) {
                                m_handler.publishOnMQTT(BTMethods.requestCompleteJsonOfMatch, sFromDevice);
                            }
                        }
                    }
                } else if ( sPayload.startsWith(MQTTHandler.JoinerLeaver.leave.toString()) ) {
                    m_handler.m_context.updateJoinedDevices(sFromDevice, false);
                }
                return;
            }

            m_MQTTmessageReceived.put(sPayload, lNow);
            m_handler.m_context.interpretReceivedMessageOnUiThread(sPayload, mqttAction, topic);
        }
    }


    @Override public void connectionLost(Throwable cause) {
        if ( cause == null ) {
            // on simple rotate of the device, we handle reconnect in lifecycle methods of main activity
        } else {
            // e.g.
            // java.io.EOFException    if Broker actually went down
            // java.netSocketException if wifi turned off
            if ( cause.getCause() != null ) {
                cause = cause.getCause();
            }
            m_handler.m_context.doDelayedMQTTReconnect(String.format("W: MQTT tcp to broker %s lost: %s.", m_handler.m_sBrokerUrl, cause), 10, 1, MQTTStatus.RetryConnection);
        }
    }
    @Override public void deliveryComplete(IMqttDeliveryToken token) {
        Log.d(TAG, "Delivery complete " + token + (token.getTopics()==null?null:Arrays.asList(token.getTopics())));
    }
}
