/*
 * Copyright (C) 2020  Iddo Hoeve
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
package com.doubleyellow.scoreboard.wear;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;

import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Regarding battery usage, a WearableListenerService is registered in an app's manifest and can launch the app if it is not already running.
 *
 * If you only need to listen for events when your app is already running, which is often the case with interactive applications,
 * then do not use a WearableListenerService.
 * Instead register a live listener using, for example, the addListener method of the DataClient class.
 * This can reduce the load on the system and reduce battery usage.
 */
public class DataLayerListenerService extends WearableListenerService
{
    private static final String TAG = "SB." + DataLayerListenerService.class.getSimpleName();

    public  static final String START_ACTIVITY_PATH     = "/start-activity";

    @Override public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d(TAG, "onDataChanged: " + dataEvents);

        // Loop through the events and send a message to the node that created the data item.
        for (DataEvent event : dataEvents) {
            Log.d(TAG, "event: " + event);

            DataItem dataItem = event.getDataItem();
            Log.d(TAG, "dataItem: " + dataItem);
            Uri uri = dataItem.getUri();
            Log.d(TAG, "uri: " + uri);

            Context        context        = getApplicationContext();
            PackageManager packageManager = context.getPackageManager();
            Intent         intent         = packageManager.getLaunchIntentForPackage(context.getPackageName());
            context.startActivity(intent);

            // Send back a message to say message was received
            String nodeId = uri.getHost();
            byte[] payload = uri.toString().getBytes();
            MessageClient messageClient = Wearable.getMessageClient(this);
            messageClient.sendMessage(nodeId, WearableHelper.BRAND_PATH, payload);
        }
    }
}