/*
 * Copyright (C) 2024  Iddo Hoeve
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
package com.doubleyellow.scoreboard.bluetooth_le;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.bluetooth.BTMessage;
import com.doubleyellow.scoreboard.bluetooth.MessageSource;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.R;

/**
 * Handler specific for BluetoothLE messages.
 */
class BLEHandler extends Handler
{
    private static final String TAG = "SB." + BLEHandler.class.getSimpleName();

    private final ScoreBoard sb;
    private final BLEConfigHandler configHandler;
    public BLEHandler(ScoreBoard scoreBoard, BLEConfigHandler bleConfigHandler) {
        super(Looper.getMainLooper());
        sb = scoreBoard;
        configHandler = bleConfigHandler;
    }

    @Override public void handleMessage(Message msg) {
        BTMessage btMessage = BTMessage.values()[msg.what];
        //Log.d(TAG, "msg.what : " + msg.what + " : " + btMessage);
        String sMsg = String.valueOf(msg.obj);
        //Log.d(TAG, "msg.obj  : " + msg.obj);
        //Log.d(TAG, "msg.arg1 : " + msg.arg1);
        //Log.d(TAG, "msg.arg2 : " + msg.arg2);
        switch (btMessage) {
            case READ:
                try {
                    sb.interpretReceivedMessageOnUiThread(sMsg, MessageSource.BluetoothLE);
                } catch (Exception e) {
                    //e.printStackTrace();
                    Log.w(TAG, "Could not understand message :" + sMsg);
                }
                break;
            case READ_RESULT_BATTERY:
                // e.g. battery level
                int iBatteryLevel = msg.arg1;
                int iPlayer       = msg.arg2;
                String sDevice = (String) msg.obj;
                String sMsg2 = sb.getString(R.string.ble_battery_level, iBatteryLevel, sDevice);
                //Toast.makeText(sb, sMsg2, Toast.LENGTH_LONG).show();
                configHandler.updateBLEConnectionStatus(View.VISIBLE, -1, sMsg2, 10);
                break;
            case INFO: {
                int iNrOfDevices = msg.arg2;
                try {
                    String sMsgInfo = sb.getString(msg.arg1, iNrOfDevices, sMsg, Brand.getShortName(sb));
                    //Toast.makeText(sb, sMsg2, Toast.LENGTH_LONG).show();
                    configHandler.updateBLEConnectionStatus(View.VISIBLE, iNrOfDevices, sMsgInfo, -1);
                } catch (Exception e) {
                    Log.w(TAG, "Error while constructing message : " + sMsg);
                    throw new RuntimeException(e);
                }
                break;
            }
            case WRITE:
                break;
            case STATE_CHANGE: {
                BLEState btState = BLEState.values()[msg.arg1];
                int iNrOfDevices = msg.arg2;
                Log.d(TAG, "new state  : " + btState);
                Log.d(TAG, "iNrOfDevices: " + iNrOfDevices);
                switch (btState) {
                    case CONNECTED_DiscoveringServices:
                        break;
                    case CONNECTED_TO_1_of_2:
                        configHandler.updateBLEConnectionStatus(View.VISIBLE, 1, sb.getString(R.string.ble_only_one_of_two_devices_connected), -1);
                        break;
                    case CONNECTED_ALL:
                        String sUIMsg = sb.getResources().getQuantityString(R.plurals.ble_ready_for_scoring_with_devices, iNrOfDevices);
                        configHandler.updateBLEConnectionStatus(View.VISIBLE, iNrOfDevices, sUIMsg, 10);
                        //sb.showBLEVerifyConnectedDevicesDialog(iNrOfDevices);
                        break;
                    case DISCONNECTED:
                        configHandler.updateBLEConnectionStatus(View.INVISIBLE, -1, "Oeps 3", -1);
                        break;
                    case CONNECTING:
                        // iNrOfDevices = number of desired devices
                        String sMsgConnecting = sb.getResources().getQuantityString(R.plurals.ble_connecting_to_devices, iNrOfDevices);
                        configHandler.updateBLEConnectionStatus(View.VISIBLE, iNrOfDevices, sMsgConnecting, -1);
                        break;
                    case DISCONNECTED_Gatt:
                        // if one of the two devices disconnects
                        configHandler.updateBLEConnectionStatus(View.VISIBLE, iNrOfDevices, sb.getString(R.string.ble_Lost_connection_to_a_device), -1);
                        break;
                    default:
                        Toast.makeText(sb, sMsg, Toast.LENGTH_LONG).show();
                        break;
                }
                break;
            }
        }
    }
}
