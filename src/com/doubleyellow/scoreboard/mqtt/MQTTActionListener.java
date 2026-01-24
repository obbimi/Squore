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

import com.doubleyellow.scoreboard.main.ScoreBoard;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;

class MQTTActionListener implements IMqttActionListener {
    private static final String TAG = "SB." + MQTTActionListener.class.getSimpleName();
    private String m_sPurpose = null;
    private String m_sBrokerUrl = null;
    private ScoreBoard m_context = null;
    public MQTTActionListener(String sPurpose, ScoreBoard scoreBoard, String sBroker) {
        m_sPurpose   = sPurpose;
        m_context    = scoreBoard;
        m_sBrokerUrl = sBroker;
    }

    void reinit(ScoreBoard scoreBoard) {
        m_context = scoreBoard;
    }

    @Override public void onSuccess(IMqttToken token) {
        //m_iBoard.showInfoMessage("MQTT " + sPurpose, 2); // to much
        //m_context.updateMQTTConnectionStatusIcon(View.VISIBLE, m_sPurpose.equalsIgnoreCase("Disconnect")?0:1);
        //Log.d(TAG, "onSuccess: " + m_sPurpose + " t=" + token.getTopics() + " " + token + " " + m_sBrokerUrl);
    }

    @Override public void onFailure(IMqttToken token, Throwable exception) {
        m_context.showInfoMessageOnUiThread("ERROR: MQTT " + m_sPurpose + " " + m_sBrokerUrl, 10);
        Log.d(TAG, "onFailure: " + m_sPurpose + " t=" + token.getTopics() + " " + token + " " + m_sBrokerUrl + " " + exception.getMessage());
    }
}
