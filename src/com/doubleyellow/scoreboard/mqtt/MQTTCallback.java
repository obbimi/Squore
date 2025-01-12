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

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MQTTCallback implements MqttCallback
{
    private static final String TAG = "SB." + MQTTCallback.class.getSimpleName();

    @Override public void messageArrived(String topic, MqttMessage message) {
        Log.d(TAG, "Receive message: " + message.toString() + " from topic: " + topic);
    }

    @Override public void connectionLost(Throwable cause) {
        Log.d(TAG, "Connection lost " + cause.toString());
    }

    @Override public void deliveryComplete(IMqttDeliveryToken token) {
        Log.d(TAG, "Delivery completed");
    }
}
