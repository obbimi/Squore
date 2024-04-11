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
package com.doubleyellow.scoreboard.bluetooth_le.selectdevice;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.Button;

import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.bluetooth_le.BLEUtil;
import com.doubleyellow.scoreboard.dialog.BaseAlertDialog;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.model.Player;
import com.doubleyellow.scoreboard.prefs.PreferenceKeys;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;

public class VerifyConnectedDevices extends BaseAlertDialog
{
    public VerifyConnectedDevices(Context context, Model matchModel, ScoreBoard scoreBoard) {
        super(context, matchModel, scoreBoard);
    }

    @Override public boolean storeState(Bundle outState) {
        return false;
    }
    private int m_iNrOfDevices = 0;
    public void init(int iNrOfDevices) {
        m_iNrOfDevices = iNrOfDevices;
    }
    @Override public boolean init(Bundle outState) {
        return false;
    }

    @Override public void show() {

        adb.setTitle(R.string.ble_devices_verify)
                .setMessage(R.string.ble_shortly_vibrate_selected_connected_devices)
                .setIcon(android.R.drawable.stat_sys_data_bluetooth)
                ;
        switch (m_iNrOfDevices) {
            case 1:
                adb.setPositiveButton(R.string.ble_single_device, null );
                adb.setNeutralButton(R.string.cmd_cancel, null);
                break;
            case 2:
                adb.setPositiveButton(matchModel.getName(Player.A), null );
                adb.setNegativeButton(matchModel.getName(Player.B), null );
                adb.setNeutralButton(R.string.ble_swap_devices, null);
                break;
        }
        dialog = adb.show();

        Button button;
        switch (m_iNrOfDevices) {
            case 1:
                button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                button.setOnClickListener(v -> scoreBoard.notifyBLE(null, BLEUtil.Keys.ConfirmationRequiredConfig));
                break;
            case 2:
                button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                button.setOnClickListener(v -> scoreBoard.notifyBLE(Player.A, BLEUtil.Keys.ConfirmationRequiredConfig));
                button = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                button.setOnClickListener(v -> scoreBoard.notifyBLE(Player.B, BLEUtil.Keys.ConfirmationRequiredConfig));
                button = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
                button.setOnClickListener(v -> {
                    // swap
                    String sBluetoothLEDevice1 = PreferenceValues.getString(PreferenceKeys.BluetoothLE_Peripheral1, null, context);
                    String sBluetoothLEDevice2 = PreferenceValues.getString(PreferenceKeys.BluetoothLE_Peripheral2, null, context);
                    PreferenceValues.setString(PreferenceKeys.BluetoothLE_Peripheral1, context, sBluetoothLEDevice2);
                    PreferenceValues.setString(PreferenceKeys.BluetoothLE_Peripheral2, context, sBluetoothLEDevice1);
                    // reinitialize
                    scoreBoard.stopBlueTooth();
                    scoreBoard.onResumeInitBluetoothBLE();
                    //this.dismiss(); not required
                });
                button.setOnLongClickListener(v -> {
                    this.dismiss();
                    return true;
                });
                break;
        }

    }
}
