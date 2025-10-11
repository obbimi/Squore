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

package com.doubleyellow.scoreboard.prefs;

import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.AttributeSet;

import com.doubleyellow.scoreboard.R;

import java.util.Map;
import java.util.Random;

/**
 * Preference to generate a new Live Score Device ID, FCM Device ID, or MQTT Device ID.
 */
public class DeviceIdPref extends MultiPrefsDialog
{
    @Override protected void showDialog(Bundle state) {
        String sCurrent = PreferenceValues.getString(getKey(), "", context);
        sDeviceIdNew = generateNewId();

        String sOKButton = context.getString(R.string.pref_DeviceId_use_new__x, sDeviceIdNew);
        this.setNegativeButtonText("Keep current " + sCurrent);
        this.setPositiveButtonText(sOKButton);

        super.showDialog(state);
    }

    private String sDeviceIdNew = null;
    public DeviceIdPref(Context context, AttributeSet attrs) {
        super(context, attrs); // invoked already/only when settings activity is requested
    }

    @Override public void getPreferencesToSet(Map<PreferenceKeys, Object> mReturn, boolean bPositive) {
        if ( bPositive ) {
            PreferenceKeys preferenceKeys = PreferenceKeys.valueOf(getKey());
            mReturn.put(preferenceKeys, sDeviceIdNew);
        }
    }

    public static String generateNewId () {
        Random random = new Random(SystemClock.uptimeMillis());
        StringBuilder sb = new StringBuilder();
        while ( sb.length() < 6 ) {
            int i = random.nextInt(36);
            char c = (char) ('A' + i); // 65 + [0-26]
            if ( i >= 26 ) {
                c = (char) ('0' + (i-26) ); // 48 + [0-10]
            }
            if ( c == '0' || c == 'O' ) {
                continue; // hard to see the difference
            }
            if ( c == '1' || c == 'I' ) {
                continue; // hard to see the difference
            }
            sb.append(c);
        }

        String sLiveScoreDeviceId = sb.toString();
        return sLiveScoreDeviceId;
    }
}