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

import android.content.Context;
//import org.eclipse.paho.android.service.MqttAndroidClient;
import info.mqtt.android.service.Ack;
import info.mqtt.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.*;

/**
 * Publish to
 * double-yellow/${EventIdentifier}/${BrandOrSport}/${deviceid}
 * double-yellow/${EventIdentifier}/${BrandOrSport}/${deviceid}/requestCompleteJsonOfMatch   for mirroring a fixed device (slave)
 *
 * Subscribe to
 * double-yellow/${EventIdentifier}/${BrandOrSport}/${deviceid}                              for mirroring a fixed device (slave)
 * double-yellow/${EventIdentifier}/${BrandOrSport}/${deviceid}/requestCompleteJsonOfMatch   for mirroring a fixed device (master)
 * double-yellow/${EventIdentifier}/${BrandOrSport}/+                                        for livescore of multiple matches of single sport
 * double-yellow/${EventIdentifier}/#                                                        for livescore of multiple matches
 *
 * 'EventIdentifier' to allow using a public broker like 'tcp://broker.hivemq.com:1883' and ensure you only receive info from a certain subset of matches
 * 'BrandOrSport' to allow subscribing only to a squash matches or only badminton matches
 *
 */
public class MQTTClient
{
    private static final int MQTT_QOS = 1;
    private MqttAndroidClient mqttClient;
    private IMqttActionListener defaultCbPublish = new MQTTActionListener("Publish");
    private IMqttActionListener defaultCbDisconnect = new MQTTActionListener("Disconnect");
/*
    private IMqttActionListener defaultCbConnect = new MQTTActionListener("Connect");
    private IMqttActionListener defaultCbSubscribe = new MQTTActionListener("Subscribe");
    private IMqttActionListener defaultCbUnSubscribe = new MQTTActionListener("UnSubscribe");
    private MqttCallback defaultCbClient = new MQTTCallback();
*/

    public MQTTClient(Context context, String serverURI, String clientID) {
        //mqttClient = new MqttAndroidClient(context, serverURI, clientID, Ack.AUTO_ACK, null, true, 1); // version 4.3 of https://github.com/hannesa2/paho.mqtt.android
        mqttClient = new MqttAndroidClient(context, serverURI, clientID, Ack.AUTO_ACK);
    }

    public void connect(String username, String password, IMqttActionListener cbConnect, MqttCallback cbClient) {
        mqttClient.setCallback(cbClient);
        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(username);
        options.setPassword(password.toCharArray());

        mqttClient.connect(options, null, cbConnect);
    }

    public boolean isConnected() {
        return mqttClient.isConnected();
    }

    public void subscribe(String topic, IMqttActionListener cbSubscribe) {
        mqttClient.subscribe(topic, MQTT_QOS, null, cbSubscribe);
    }

    public void unsubscribe(String topic, IMqttActionListener cbUnsubscribe) {
        mqttClient.unsubscribe(topic, null, cbUnsubscribe);
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

}

