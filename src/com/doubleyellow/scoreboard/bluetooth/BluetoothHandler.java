/*
 * Copyright (C) 2019  Iddo Hoeve
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
package com.doubleyellow.scoreboard.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.dialog.MyDialogBuilder;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.prefs.PreferenceKeys;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;

/**
 * Handler specific for Bluetooth connection/messages.
 */
public class BluetoothHandler extends Handler
{
    private static final String TAG = "SB." + BluetoothHandler.class.getSimpleName();

    private ScoreBoard sb = null;
    public BluetoothHandler(ScoreBoard scoreBoard) {
        super(Looper.getMainLooper());
        sb = scoreBoard;
    }

    @Override public void handleMessage(Message msg) {
        BTMessage btMessage = BTMessage.values()[msg.what];
        switch (btMessage) {
            case STATE_CHANGE:
                BTState btState = BTState.values()[msg.arg1] ;
                storeBTDeviceConnectedTo(msg.obj);
                switch (btState) {
                    case CONNECTED:
                        if ( BTRole.Master.equals(sb.m_blueToothRole) ) {
                            // show dialog to request to pull in match from other device, or push match on this device to other
                            sb.pullOrPushMatchOverBluetooth(m_btDeviceOther.getName());
                        } else {
                            sb.setBluetoothRole(BTRole.Slave, btState);
                        }
                        sb.setBluetoothIconVisibility(View.VISIBLE);
                        break;
                    case CONNECTING:
                        break;
                    case LISTEN:
                    case NONE:
                        sb.setBluetoothRole(BTRole.Equal, btState);
                        sb.setBluetoothIconVisibility(View.INVISIBLE);
                        break;
                }
                break;
            case WRITE:
                byte[] writeBuf = (byte[]) msg.obj;
                String writeMessage = new String(writeBuf);
                Log.d(TAG, "writeMessage: " + writeMessage);
                if ( BTRole.Slave.equals(sb.m_blueToothRole)
                  && writeMessage.trim().matches("(" + BTMethods.Toast + "|" + BTMethods.requestCompleteJsonOfMatch + "|" + BTMethods.jsonMatchReceived + "|" + BTMethods.requestCountryFlag + ").*") == false
                   )
                {
                    // become master
                    sb.setBluetoothRole(BTRole.Master, writeMessage.trim());
                }
                break;
            case READ:
                byte[] readBuf = (byte[]) msg.obj;
                String readMessage = new String(readBuf, 0, msg.arg1); // msg.arg1 contains number of bytes actually having valid info
                Log.d(TAG, "readMessage: (#" + msg.arg1 + "): " + readMessage);
                if ( BTRole.Master.equals(sb.m_blueToothRole)
                  && readMessage.trim().matches("(" + BTMethods.Toast + "|" + BTMethods.requestCompleteJsonOfMatch + "|" + BTMethods.jsonMatchReceived + "|" + BTMethods.requestCountryFlag + ").*") == false
                   )
                {
                    // become slave
                    sb.setBluetoothRole(BTRole.Slave, readMessage.trim());
                }
                m_bHandlingBluetoothMessageInProgress = true;
                sb.interpretReceivedMessageOnUiThread(readMessage, MessageSource.BluetoothMirror);
                m_bHandlingBluetoothMessageInProgress = false;
                break;
            case READ_RESULT_BATTERY:
                break;
            case INFO:
                storeBTDeviceConnectedTo(msg.obj); // normally correct device is already stored in 'case STATE_CHANGE'
                String sMsg = sb.getString(msg.arg1, m_sDeviceNameLastConnected, Brand.getShortName(sb));
                if ( msg.arg2 != 0 ) {
                    String sInfo = sb.getString(msg.arg2, m_sDeviceNameLastConnected, Brand.getShortName(sb));
                    MyDialogBuilder.dialogWithOkOnly(sb, sMsg, sInfo, true);
                } else {
                    Toast.makeText(sb, sMsg, Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }
    private boolean m_bHandlingBluetoothMessageInProgress = false;
    boolean isHandlingMessage() {
        return m_bHandlingBluetoothMessageInProgress;
    }

    public void storeBTDeviceConnectedTo(Object o) {
        if ( o instanceof BluetoothDevice ) {
            m_btDeviceOther = (BluetoothDevice) o;
            m_sDeviceNameLastConnected = m_btDeviceOther.getName();
            PreferenceValues.setString(PreferenceKeys.lastConnectedBluetoothDevice, sb, m_btDeviceOther.getName());
        } else {
            m_btDeviceOther = null;
        }
    }

    public String getOtherDeviceName() {
        if ( m_btDeviceOther == null ) {
            return "";
        }
        return m_btDeviceOther.getName();
    }
    public String getOtherDeviceAddressName() {
        if ( m_btDeviceOther == null ) {
            return "";
        }
        return m_btDeviceOther.getAddress();
    }
    private String                  m_sDeviceNameLastConnected = null;
    private BluetoothDevice         m_btDeviceOther            = null;

}
