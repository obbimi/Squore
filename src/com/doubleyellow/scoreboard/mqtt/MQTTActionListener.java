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

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;

class MQTTActionListener implements IMqttActionListener {
    private static final String TAG = "SB." + MQTTActionListener.class.getSimpleName();
    private String sPurpose = null;
    public MQTTActionListener(String sPurpose) {
        this.sPurpose = sPurpose;
    }
    @Override public void onSuccess(IMqttToken asyncActionToken) {
        Log.d(TAG, "onSuccess: " + sPurpose + " t=" + asyncActionToken.getTopics() + " " + asyncActionToken);
    }

    @Override public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
        Log.d(TAG, "onFailure: " + sPurpose + " t=" + asyncActionToken.getTopics() + " " + asyncActionToken);
    }
}
