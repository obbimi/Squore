/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.doubleyellow.scoreboard.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Message;
import android.util.Log;

import com.doubleyellow.scoreboard.R;
import com.doubleyellow.util.ListUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected.
 *
 * This is for achieving the mirroring the score between two connected devices.
 */
public class BluetoothControlService
{
    private final String TAG = "SB." + this.getClass().getSimpleName();

    private String NAME  = null;

    // Unique UUID for this bluetooth between to instances of this application
    private UUID MY_UUID = null; // https://www.uuidgenerator.net/version4

    // Member fields
    private final BluetoothAdapter mAdapter;
    private       BluetoothHandler mHandler;
    private       AcceptThread     mAcceptThread;
    private       ConnectThread    mConnectThread;
    private       ConnectedThread  mConnectedThread; // to write data onto
    private       BTState          mState = BTState.NONE;

    /**
     * Constructor. Prepares a new BluetoothControlService session.
     */
    public BluetoothControlService(UUID uuid, String sName) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState   = BTState.NONE;
        MY_UUID  = uuid;
        NAME     = "BluetoothControlService" + sName;
    }
    /*
     * @param handler A Handler to send messages back to the UI Activity
     */
    public void setHandler(BluetoothHandler handler) {
        this.mHandler = handler;
    }

    /**
     * Set the current state of the connection
     *
     * @param state An integer defining the current connection state
     */
    private synchronized void setState(BTState state, BluetoothDevice device) {
        mState = state;
        // Give the new state to the Handler so the UI Activity can update
        Message message = mHandler.obtainMessage(BTMessage.STATE_CHANGE.ordinal(), state.ordinal(), 0, device);
        message.sendToTarget();
    }

    /**
     * Return the current connection state.
     */
    public synchronized BTState getState() {
        return mState;
    }

    /**
     * Start the service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     */
    public synchronized void breakConnectionAndListenForNew() {
        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        // Start the thread to listen on a BluetoothServerSocket
        if (mAcceptThread == null) {
            try {
                mAcceptThread = new AcceptThread();
                mAcceptThread.start();
            } catch (SecurityException e) {
                // seen this in playstore only for android 9 (moto g device), but can not reproduce ?!
                e.printStackTrace();
            }
        }
        if ( mAcceptThread == null ) {
            // somehow initialization failed
            setState(BTState.NONE, null);
        } else {
            setState(BTState.LISTEN, null);
        }
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     *
     * @param device The BluetoothDevice to connect
     */
    public synchronized void connect(BluetoothDevice device) {
        // Cancel any thread attempting to make a connection
        if ( mState.equals(BTState.CONNECTING) ) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }
        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(BTState.CONNECTING, null);
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        // Cancel the accept thread because we only want to connect to one device
        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }
        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();
        connectionSucceeded(device);
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }
        setState(BTState.NONE, null);
    }

    private static final String MSG_SEPARATOR = "\n";

    /**
     * Write to the ConnectedThread (in a synchronized manner!)
     */
    public synchronized void write(String s) {
        if ( false
           && mHandler.isHandlingMessage()
           && (s.startsWith(BTMethods.requestCompleteJsonOfMatch.toString()) == false)
           ) {
            Log.d(TAG, String.format("Not sending message %s. In progress of handling message", s));
            return;
        }
        write(s.getBytes());

        if ( m_sbSendDelayed.length() > 0 ) {
            Log.d(TAG, "Sending 'delayed' : " + m_sbSendDelayed);
            write(m_sbSendDelayed.toString().getBytes());
            m_sbSendDelayed.setLength(0);
        }
    }

    private final StringBuffer m_sbSendDelayed = new StringBuffer();
    public synchronized void sendDelayed(String s) {
        m_sbSendDelayed.append(s);
    }
    private void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized ( this ) {
            if (mState.equals(BTState.CONNECTED)==false) return;
            r = mConnectedThread;

            r.write(out);
            r.write(MSG_SEPARATOR.getBytes());
        }
    }

    /**
     * Indicate that the connection attempt was successful.
     */
    private void connectionSucceeded(BluetoothDevice device) {
        setState(BTState.CONNECTED, device);
        // Send the name of the connected device back to the UI Activity
        Message msg = mHandler.obtainMessage(BTMessage.INFO.ordinal(), R.string.bt_connected_to_device_X, 0, device);
        mHandler.sendMessage(msg);

        //writeMethodToBluetooth(BTMethods.updatePreference, PreferenceKeys.BTSync_showFullScreenTimer, PreferenceValues.BTSync_showFullScreenTimer(this));
    }
    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed(BluetoothDevice device) {
        setState(BTState.LISTEN, null);
        Message msg = mHandler.obtainMessage(BTMessage.INFO.ordinal(), R.string.bt_unable_to_connect_device_X, R.string.bt_unable_to_connect_device_info, device);
        mHandler.sendMessage(msg);
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        setState(BTState.LISTEN, null);
        Message msg = mHandler.obtainMessage(BTMessage.INFO.ordinal(), R.string.bt_connection_to_device_X_was_lost, 0);
        mHandler.sendMessage(msg);
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;

        AcceptThread() {
            BluetoothServerSocket tmp = null;
            // Create a new listening server socket
            try {
                tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID); // Needs android.permission.BLUETOOTH_CONNECT permission for android.content.AttributionSource
            } catch (IOException e) {
            }
            mmServerSocket = tmp;
        }

        @Override public void run() {
            setName("AcceptThread");
            BluetoothSocket socket = null;
            // Listen to the server socket if we're not connected
            while (mState.equals(BTState.CONNECTED)== false && (mmServerSocket != null)) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    break;
                }
                // If a connection was accepted
                if (socket != null) {
                    synchronized (BluetoothControlService.this) {
                        switch (mState) {
                            case LISTEN:
                            case CONNECTING:
                                // Situation normal. Start the connected thread.
                                connected(socket, socket.getRemoteDevice()); // set state to CONNECTED
                                break;
                            case NONE:
                            case CONNECTED:
                                // Either not ready or already connected. Terminate new socket.
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                }
                                break;
                        }
                    }
                }
            }
        }

        void cancel() {
            try {
                if ( mmServerSocket != null ) {
                    mmServerSocket.close();
                }
            } catch (IOException e) {
            }
        }
    }

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            // Get a BluetoothSocket for a connection with the given BluetoothDevice
            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID); // requires BLUETOOTH_ADMIN
            } catch (IOException e) {
            }
            mmSocket = tmp;
        }

        @Override public void run() {
            setName("ConnectThread");
            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery(); // requires permission BLUETOOTH_ADMIN
            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                connectionFailed(mmDevice);
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                }
                // Start the service over to restart listening mode
                BluetoothControlService.this.breakConnectionAndListenForNew();
                return;
            }
            // Reset the ConnectThread because we're done
            synchronized (BluetoothControlService.this) {
                mConnectThread = null;
            }
            // Start the connected thread
            connected(mmSocket, mmDevice);
        }

        void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream     mmInStream;
        private final OutputStream    mmOutStream;

        ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream  tmpIn  = null;
            OutputStream tmpOut = null;
            // Get the BluetoothSocket input and output streams
            try {
                tmpIn  = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }
            mmInStream  = tmpIn;
            mmOutStream = tmpOut;
        }

        @Override public void run() {
            byte[] buffer = new byte[1024];
            int bytes;
            StringBuilder sbBufferComplete = new StringBuilder();
            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    Log.v(TAG, "read from input stream: " + bytes);
                    // Send the obtained bytes to the UI Activity
                    String sBuffer = new String(buffer, 0 , bytes);
                    sbBufferComplete.append(sBuffer);
                    int iIdx = sbBufferComplete.indexOf(MSG_SEPARATOR);
                    if ( iIdx > 0 ) {
                        while (iIdx > 0 ) {
                            String sToInterpret = sbBufferComplete.substring(0, iIdx);
                            // reconstruct StringBuilder without message to interpret
                            String sRemaining = sbBufferComplete.substring(iIdx + MSG_SEPARATOR.length());
                            sbBufferComplete.setLength(0);
                            sbBufferComplete.append(sRemaining);

                            byte[] baToInterpret = sToInterpret.getBytes();
                            Message message = mHandler.obtainMessage(BTMessage.READ.ordinal(), baToInterpret.length, -1, baToInterpret);
                            message.sendToTarget();

                            iIdx = sbBufferComplete.indexOf(MSG_SEPARATOR);
                        }
                    } else {
                        Log.v(TAG, "Waiting for more data from input stream: " + sbBufferComplete);
                    }
                } catch (IOException e) {
                    connectionLost();
                    BluetoothControlService.this.breakConnectionAndListenForNew();
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
                mmOutStream.flush();
                // Share the sent message back to the UI Activity
                Message message = mHandler.obtainMessage(BTMessage.WRITE.ordinal(), buffer);
                message.sendToTarget();
            } catch (IOException e) {
            }
        }

        void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }

    public static List<BluetoothDevice> filtered_Network(Set<BluetoothDevice> pairedDevices) {
        return filtered(pairedDevices, BluetoothClass.Service.NETWORKING);
    }
    public static List<BluetoothDevice> filtered_Audio(Set<BluetoothDevice> pairedDevices) {
        return filtered(pairedDevices, BluetoothClass.Service.AUDIO);
    }
    public static List<BluetoothDevice> filtered(Set<BluetoothDevice> pairedDevices, int iBluetoothClass_Service) {
        List<BluetoothDevice> lPairedDevicesFilteredOnNWService = new ArrayList<>();
        // If there are paired devices, check if the device supports networking
        if ( ListUtil.isNotEmpty(pairedDevices) ) {
            for (BluetoothDevice device : pairedDevices) {
                if ( device.getBluetoothClass().hasService(iBluetoothClass_Service) ) {
                    lPairedDevicesFilteredOnNWService.add(device);
                }
            }
        }
        return lPairedDevicesFilteredOnNWService;
    }
}
