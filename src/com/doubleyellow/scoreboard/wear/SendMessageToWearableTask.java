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
import android.os.AsyncTask;
import android.util.Log;

import com.doubleyellow.util.ListUtil;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeClient;
import com.google.android.gms.wearable.Wearable;

import java.util.List;

/**
 * To send message in an asynchronous way to paired device.
 */
class SendMessageToWearableTask extends AsyncTask<Object, Void, String>
{
    private static final String TAG = "SB." + SendMessageToWearableTask.class.getSimpleName();

    private MessageClient m_messageClient = null;
    private NodeClient    m_nodeClient    = null;

    static String lastNodeId = null;

    SendMessageToWearableTask(Context ctx) {
        super();
        m_messageClient = Wearable.getMessageClient(ctx);
        m_nodeClient    = Wearable.getNodeClient(ctx);
    }
    public AsyncTask<Object, Void, String> myExecute(Object... params) {
        //AsyncTask<Object, Void, String> executing = super.execute(params);
        AsyncTask<Object, Void, String> executing = super.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
        return executing;
    }

    @Override protected String doInBackground(Object[] objects) {
        if ( m_nodeClient == null ) {
            Log.d(TAG, "No instance of " + NodeClient.class.getName());
            return null;
        }
        if ( m_messageClient == null ) { return null; }

        try {
            Task<List<Node>> nodeListTask = m_nodeClient.getConnectedNodes();
            List<Node> nodes = Tasks.await(nodeListTask); // this throws exception if API not available

            if ( ListUtil.isEmpty(nodes) ) { return null; }
            for( Node node : nodes ) {
                String sPath    = String.valueOf(objects[0]) ;
                String sMessage = String.valueOf(objects[1]);
                Task<Integer> sendMessageTask = m_messageClient.sendMessage(node.getId(), sPath, sMessage.getBytes());
                Integer requestId = Tasks.await(sendMessageTask);
                Log.v(TAG, String.format("send reqId %d : %s", requestId, sMessage)); // just an (for every call increasing) integer. Same number as messageEvent.getRequestId() on receiving end
                lastNodeId = node.getId();
            }
        } catch (Exception e) {
            Log.w(TAG, "Task failed: " + e); // e.g. java.util.concurrent.ExecutionException: com.google.android.gms.common.api.ApiException: 17: API: Wearable.API is not available on this device. Connection failed with: ConnectionResult{statusCode=SERVICE_INVALID, resolution=null, message=null}
        }
        return null;
    }
}
