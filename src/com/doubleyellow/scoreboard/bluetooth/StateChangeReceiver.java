/*
 * Copyright (C) 2017  Iddo Hoeve
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

import static com.doubleyellow.scoreboard.main.ScoreBoard.mBluetoothControlService;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.main.ScoreBoard;

public class StateChangeReceiver extends BroadcastReceiver
{
    @Override public void onReceive(Context context, Intent intent)
    {
        final String action = intent.getAction();

        if (action != null && action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            switch (state) {
                case BluetoothAdapter.STATE_OFF:
                case BluetoothAdapter.STATE_TURNING_OFF:
                    if ( mBluetoothControlService != null ) {
                        mBluetoothControlService = null;
                    }
                    break;
                case BluetoothAdapter.STATE_ON:
                    mBluetoothControlService = new BluetoothControlService(Brand.getUUID(), Brand.getShortName(context));
                    if ( context instanceof ScoreBoard ) {
                        mBluetoothControlService.setHandler(((ScoreBoard) context).mBluetoothHandler);
                    }
                    mBluetoothControlService.breakConnectionAndListenForNew();
                    break;
                case BluetoothAdapter.STATE_TURNING_ON:
                    break;
            }
        }
    }
}
