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

import java.util.ArrayList;
import java.util.List;

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
public class MQTTClient
{
    private static final int MQTT_QOS = 1;
    private final MqttAndroidClient mqttClient;
    private final IMqttActionListener defaultCbPublish = new MQTTActionListener("Publish");
    private final IMqttActionListener defaultCbDisconnect = new MQTTActionListener("Disconnect");
    private final IMqttActionListener defaultCbUnSubscribe = new MQTTActionListener("UnSubscribe");
/*
    private IMqttActionListener defaultCbConnect = new MQTTActionListener("Connect");
    private IMqttActionListener defaultCbSubscribe = new MQTTActionListener("Subscribe");
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

    private List<String> m_lSubscriptions = new ArrayList<>();
    public void subscribe(String topic, IMqttActionListener cbSubscribe) {
        m_lSubscriptions.add(topic);
        mqttClient.subscribe(topic, MQTT_QOS, null, cbSubscribe);
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

}

