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
package com.doubleyellow.scoreboard.util;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import com.doubleyellow.scoreboard.model.JSONKey;

import java.util.HashMap;
import java.util.Map;

/**
 * To optionally
 * - include battery information
 * - publish battery levels on MQTT setup
 */
public class BatteryInfo {

    public static Map<String, Object> getInfo(Context context) {
        try {
            IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = context.registerReceiver(null, filter);
            if ( batteryStatus == null ) { return null; }

            int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING/* || status == BatteryManager.BATTERY_STATUS_FULL*/;
/*
            int chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
            boolean usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
            boolean acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;
*/

            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

            int batteryPct = Math.round(100 * level / (float)scale);

            Map<String, Object> map = new HashMap();
            map.put(JSONKey.batteryPercentage.toString(), batteryPct);
            map.put(JSONKey.batteryCharging.toString(), isCharging);
            return map;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
