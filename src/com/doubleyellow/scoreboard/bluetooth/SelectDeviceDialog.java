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
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.dialog.BaseAlertDialog;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.util.ListUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class SelectDeviceDialog extends BaseAlertDialog
{
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

    @Override public void show() {
        adb     .setTitle  (        getString(R.string.bt_select_device) )
                .setIcon   (android.R.drawable.stat_sys_data_bluetooth)
                //.setMessage(sMsg)
                .setPositiveButton(android.R.string.ok    , listener)
                .setNeutralButton (android.R.string.cancel, listener)
                .setNegativeButton(R.string.refresh       , listener);

        // add a view with all possible devices and let user choose one
        // Get the local Bluetooth adapter
        LinearLayout ll = refreshSelectList(m_lPairedDevicesChecked);
        if (ll == null) return;
        adb.setView(ll);

        dialog = adb.show();
    }

    private LinearLayout refreshSelectList(List<BluetoothDevice> lPairedDevicesChecked) {
        LinearLayout ll = new LinearLayout(context);
        ll.setOrientation(LinearLayout.VERTICAL);
        sfv = new SelectDeviceView(context, lPairedDevicesChecked, lPairedDevicesChecked.get(0));
        ll.addView(sfv);
        return ll;
    }

    private List<BluetoothDevice> m_lPairedDevicesChecked = null;

    /** called from main activity before adding dialog to stack */
    public List<BluetoothDevice> getBluetoothDevices() {
        if ( m_lPairedDevicesChecked != null ) {
            return m_lPairedDevicesChecked;
        }

        BluetoothAdapter mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if ( mBtAdapter == null ) {
            Toast.makeText(context, R.string.bt_no_bluetooth_on_device, Toast.LENGTH_LONG).show();
            return null;
        }
        if ( mBtAdapter.isEnabled() == false ) {
            Toast.makeText(context, R.string.bt_bluetooth_turned_off, Toast.LENGTH_LONG).show();
            return null;
        }

        // Get a set of currently paired devices
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
        if ( ListUtil.isEmpty(pairedDevices) ) {
            Toast.makeText(context, R.string.bt_no_paired, Toast.LENGTH_LONG).show();
            return null;
        }

        List<BluetoothDevice> lPairedDevicesFilteredOnNWService = new ArrayList<>();
        // If there are paired devices, check if the device supports networking
        if ( ListUtil.isNotEmpty(pairedDevices) ) {
            for (BluetoothDevice device : pairedDevices) {
                if ( device.getBluetoothClass().hasService(BluetoothClass.Service.NETWORKING) ) {
                    lPairedDevicesFilteredOnNWService.add(device);
                }
            }
        }
        if ( ListUtil.isEmpty(lPairedDevicesFilteredOnNWService) ) {
            Toast.makeText(context, R.string.bt_no_appropriate_paired_devices_found, Toast.LENGTH_LONG).show();
            return null;
        }

        // to check for GUID
        List<BluetoothDevice> lPairedDevicesChecked = new ArrayList<>();
        for (BluetoothDevice device : lPairedDevicesFilteredOnNWService) {
            ParcelUuid[] uuids = device.getUuids();
            boolean bOK = false;
            for(ParcelUuid uuid: uuids ) {
                UUID sUUUD = uuid.getUuid();
                if ( sUUUD.equals(Brand.getUUID()) ) {
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
            } else {
                return null;
            }
        }
        m_lPairedDevicesChecked = lPairedDevicesChecked;
        return m_lPairedDevicesChecked;
    }

    private DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
        @Override public void onClick(DialogInterface dialog, int which) {
            handleButtonClick(which);
        }
    };

    public static final int BNT_IMPORT  = DialogInterface.BUTTON_POSITIVE;
    public static final int BNT_REFRESH = DialogInterface.BUTTON_NEGATIVE;
    @Override public void handleButtonClick(int which) {
        BluetoothDevice dChecked = sfv.getChecked();

        switch (which) {
            case BNT_IMPORT:
                String sDeviceName    = dChecked.getName();
                String sDeviceAddress = dChecked.getAddress();
                scoreBoard.connectBluetoothDevice(sDeviceAddress);
                break;
            case DialogInterface.BUTTON_NEUTRAL:
                break;
            case BNT_REFRESH:
                m_lPairedDevicesChecked = null;
                getBluetoothDevices();

                LinearLayout ll = refreshSelectList(m_lPairedDevicesChecked);
                if (ll == null) return;
                adb.setView(ll);
                break;
        }
    }
}
