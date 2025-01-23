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
import com.doubleyellow.scoreboard.bluetooth.BTRole;
import com.doubleyellow.scoreboard.bluetooth.MessageSource;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.HashMap;
import java.util.Map;

public class ClientCallback implements MqttCallback
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
        Log.d(TAG, msg);

        if ( topic.endsWith(PreferenceValues.getFCMDeviceId(m_handler.m_context)) ) {
            // show but ignore for further processing, messages published by this device itself
            m_handler.m_context.showInfoMessageOnUiThread(msg, 1);
        } else {
            String sPayload = new String(message.getPayload());
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
                String sFromDevice = saMethodNArgs[1];
                boolean bIsResponseToOtherJoin = false;
                if (saMethodNArgs.length==3 ) {
                    bIsResponseToOtherJoin = Boolean.parseBoolean(saMethodNArgs[2]);
                }
                if ( sFromDevice.equals(m_handler.m_thisDeviceId) ) { return; }

                if ( sPayload.startsWith(MQTTHandler.JoinerLeaver.join.toString() ) ) {
                    if ( m_handler.m_mJoinedDevices.containsKey(sFromDevice) ) {
                        Log.w(TAG, "Already know about " + sFromDevice);
                        return;
                    }
                    m_handler.m_mJoinedDevices.put(sFromDevice, System.currentTimeMillis());
                    if ( bIsResponseToOtherJoin == false ) {
                        // let new joiner know we joined in the past
                        m_handler.publish(m_handler.m_joinerLeaverTopic, MQTTHandler.JoinerLeaver.join  + "(" + m_handler.m_thisDeviceId + "," + true + ")");
                    }
                } else if ( sPayload.startsWith(MQTTHandler.JoinerLeaver.leave.toString()) ) {
                    m_handler.m_mJoinedDevices.remove(sFromDevice);
                }
                Log.i(TAG, "I now know about " + m_handler.m_mJoinedDevices);
                return;
            }


            if ( sPayload.startsWith(BTMethods.changeScore.toString()) ) {
                m_handler.changeMQTTRole(BTRole.Slave);
            }
            m_MQTTmessageReceived.put(sPayload, lNow);
            m_handler.m_context.interpretReceivedMessageOnUiThread(sPayload, MessageSource.MQTT);
        }
    }

    @Override public void connectionLost(Throwable cause) {
        if ( cause == null ) {
            // on simple rotate of the device, we handle reconnect in lifecycle methods of main activity
        } else {
            // e.g. java.io.EOFException if Broker actually went down, java.netSocketException if wifi turned off
            if ( cause.getCause() != null ) {
                cause = cause.getCause();
            }
            m_handler.m_context.doDelayedMQTTReconnect(String.format("W: MQTT tcp to broker %s lost: %s.", m_handler.m_sBrokerUrl, cause), 10);
        }
    }
    @Override public void deliveryComplete(IMqttDeliveryToken token) {
        Log.d(TAG, "Delivery complete " + token);
    }
}
