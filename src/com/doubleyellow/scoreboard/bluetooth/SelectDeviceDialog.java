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

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.dialog.BaseAlertDialog;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.prefs.ColorPrefs;
import com.doubleyellow.scoreboard.prefs.PreferenceKeys;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.scoreboard.view.PreferenceCheckBox;
import com.doubleyellow.util.ListUtil;
import com.doubleyellow.util.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class SelectDeviceDialog extends BaseAlertDialog
{
    private static final String TAG = "SB." + SelectDeviceDialog.class.getSimpleName();

    public SelectDeviceDialog(Context context, Model matchModel, ScoreBoard scoreBoard) {
        super(context, matchModel, scoreBoard);
    }

    @Override public boolean storeState(Bundle outState) {
        return true;
    }

    @Override public boolean init(Bundle outState) {
        return true;
    }

    private SelectDeviceView sfv;
  //private CheckBox         cbLRMirror;
    private CheckBox         cbFSTimer;

    @Override public void show() {
        adb     .setTitle  (        getString(R.string.bt_select_device) )
                .setIcon   (android.R.drawable.stat_sys_data_bluetooth)
                .setMessage(R.string.bt_select_device_for_scoreboard_mirroring)
                .setPositiveButton(android.R.string.ok    , listener)
                .setNeutralButton (android.R.string.cancel, listener)
              //.setNegativeButton(R.string.refresh       , listener)
                ;

        if ( StringUtil.isNotEmpty(scoreBoard.getOtherBluetoothDeviceAddressName()) ) {
            adb.setNegativeButton(R.string.bt_disconnect, listener);
        }

        // add a view with all possible devices and let user choose one
        // Get the local Bluetooth adapter
        LinearLayout ll = refreshSelectList(m_lPairedDevicesChecked);
        if (ll == null) return;

        LinearLayout llCb = new LinearLayout(context);
        llCb.setOrientation(LinearLayout.VERTICAL);

        // select how LR is applied on 'slave'
        boolean bKeepLRMirrored = PreferenceValues.BTSync_keepLROnConnectedDeviceMirrored(context);
        if ( Brand.isNotSquash() || bKeepLRMirrored ) {
            //cbLRMirror = new CheckBox(context);
            int iResDefault = PreferenceValues.getSportTypeSpecificResId(context, R.bool.BTSync_keepLROnConnectedDeviceMirrored_default__Squash);
            PreferenceCheckBox cbLRMirror = new PreferenceCheckBox(context, PreferenceKeys.BTSync_keepLROnConnectedDeviceMirrored, iResDefault);
            cbLRMirror.setText(R.string.pref_BTSync_keepLROnConnectedDeviceMirrored);
            cbLRMirror.setChecked(bKeepLRMirrored);
            cbLRMirror.setTag(ColorPrefs.Tags.header);

            llCb.addView(cbLRMirror);
        }

        cbFSTimer = new CheckBox(context);
        cbFSTimer.setText(R.string.pref_BTSync_showFullScreenTimer);
        cbFSTimer.setChecked(PreferenceValues.BTSync_showFullScreenTimer(context));
        cbFSTimer.setTag(ColorPrefs.Tags.header);

        llCb.addView(cbFSTimer);
        ll.addView(llCb);

        adb.setView(ll);
        ColorPrefs.setColor(llCb);

        dialog = adb.show();
    }

    private LinearLayout refreshSelectList(List<BluetoothDevice> lPairedDevicesChecked) {
        LinearLayout ll = new LinearLayout(context);
        ll.setOrientation(LinearLayout.VERTICAL);

        // pre-select
        // - first device, or
        // - previously selected device
        BluetoothDevice fChecked = lPairedDevicesChecked.get(0);
        String sPreviouslyConnected = PreferenceValues.getString(PreferenceKeys.lastConnectedBluetoothDevice, null, context);
        if ( StringUtil.isNotEmpty(sPreviouslyConnected) ) {
            for( BluetoothDevice btd: lPairedDevicesChecked ) {
                if ( sPreviouslyConnected.equals(btd.getName()) ) {
                    fChecked = btd;
                    break;
                }
            }
        }

        sfv = new SelectDeviceView(context, lPairedDevicesChecked, fChecked);
        ll.addView(sfv);
        return ll;
    }

    private List<BluetoothDevice> m_lPairedDevicesChecked = null;

    /** called from main activity before adding dialog to stack. Returns resource ids of messages to display if something went wrong */
    public int[] getBluetoothDevices(boolean bRefresh) {
        if ( bRefresh ) {
            m_lPairedDevicesChecked = null;
        }
        if ( m_lPairedDevicesChecked != null ) {
            return null;
        }

        BluetoothAdapter mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if ( mBtAdapter == null ) {
            return new int[] { R.string.bt_no_bluetooth_on_device, R.string.bt_no_bluetooth_on_device_info };
        }
        if ( mBtAdapter.isEnabled() == false ) {
            return new int[] { R.string.bt_bluetooth_turned_off, R.string.bt_bluetooth_turned_off_info };
        }

        // Get a set of currently paired devices
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
        if ( ListUtil.isEmpty(pairedDevices) ) {
            return new int[] { R.string.bt_no_paired, R.string.bt_how_to_pair_info };
        }
        List<BluetoothDevice> lPairedDevicesFilteredOnNWService = BluetoothControlService.filtered(pairedDevices);
        if ( ListUtil.isEmpty(lPairedDevicesFilteredOnNWService) ) {
            return new int[] { R.string.bt_no_appropriate_paired_devices_found, R.string.bt_how_to_pair_info };
        }

        // to check for GUID
        List<BluetoothDevice> lPairedDevicesChecked = new ArrayList<>();
        for (BluetoothDevice device : lPairedDevicesFilteredOnNWService) {
            ParcelUuid[] uuids = device.getUuids();
            if ( uuids == null ) {
                Log.w(TAG, "No uuid's found for device " + device);
                continue;
            }
            boolean bOK = false;
            for(ParcelUuid uuid: uuids ) {
                UUID sUUUD = uuid.getUuid();
                if ( sUUUD.equals( Brand.getUUID() ) ) {
                    bOK = true;
                    break;
                }
            }
            if ( bOK ) {
                lPairedDevicesChecked.add(device);
            }
        }
        if ( ListUtil.isEmpty(lPairedDevicesChecked) ) {
            //Toast.makeText(context, getString(R.string.bt_no_paired_devices_with_x_running_found, Brand.getShortName(context)), Toast.LENGTH_LONG).show();
            if ( ListUtil.isNotEmpty(lPairedDevicesFilteredOnNWService)  ) {
                lPairedDevicesChecked.addAll(lPairedDevicesFilteredOnNWService);
            }
        }
        m_lPairedDevicesChecked = lPairedDevicesChecked;

        // returning NO error messages, all is OK
        return null;
    }

    private DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
        @Override public void onClick(DialogInterface dialog, int which) {
            handleButtonClick(which);
        }
    };

    @Override public void handleButtonClick(int which) {
        BluetoothDevice dChecked = sfv.getChecked();

        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                //if ( cbLRMirror != null ) {
                //    PreferenceValues.setBoolean(PreferenceKeys.BTSync_keepLROnConnectedDeviceMirrored, context, cbLRMirror.isChecked());
                //}
                if ( cbFSTimer != null ) {
                    // this value is to be communicated to Slave device, so it is actually used there
                    PreferenceValues.setBoolean(PreferenceKeys.BTSync_showFullScreenTimer, context, cbFSTimer.isChecked());
                }

                String sDeviceName    = dChecked.getName();
                String sDeviceAddress = dChecked.getAddress();
                scoreBoard.connectBluetoothDevice(sDeviceAddress);
                break;
            case DialogInterface.BUTTON_NEUTRAL:
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                if ( true ) {
                    scoreBoard.disconnectBluetoothDevice();
                } else {
                    // refresh
                    getBluetoothDevices(true);

                    LinearLayout ll = refreshSelectList(m_lPairedDevicesChecked);
                    if (ll == null) return;
                    adb.setView(ll);
                    break;
                }
        }
        scoreBoard.triggerEvent(ScoreBoard.SBEvent.bluetoothDeviceSelectionClosed, this);
    }
}
