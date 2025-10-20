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

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;

import com.doubleyellow.prefs.RWValues;
import com.doubleyellow.scoreboard.R;

public class ResetPrefs extends DialogPreference
{
    private Context context;

    public ResetPrefs(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        this.context = context;
	    super.setDialogIcon(R.drawable.settings_reset);
    }

    @Override public void onClick(DialogInterface dialog, int iPressed)
    {
        super.onClick(dialog, iPressed);

        switch (iPressed) {
            case DialogInterface.BUTTON_POSITIVE:
                //Get this application SharedPreferences editor
                resetToDefaults(context, R.xml.preferences);

                //Call this method to trigger the execution of the setOnPreferenceChangeListener() method at the PrefsActivity
                OnPreferenceChangeListener onPreferenceChangeListener = getOnPreferenceChangeListener();
                if ( onPreferenceChangeListener != null ) {
                    onPreferenceChangeListener.onPreferenceChange(this, true);
                }

                RWValues.clearRunCountCache();
                // ensure scoreboard elements are redrawn
                Preferences.setModelDirty();

                PreferenceValues.setRestartRequired(context);
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                break;
        }
    }

    static void resetToDefaults(Context context, int iRes) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        String sRetainRandomValue = preferences.getString(PreferenceKeys.liveScoreDeviceId.toString(), null);

        SharedPreferences.Editor preferencesEditor = preferences.edit();
        //Clear all the saved preference values.
        preferencesEditor.clear();
        preferencesEditor.putString(PreferenceKeys.liveScoreDeviceId.toString(), sRetainRandomValue);
        //Commit all changes.
        preferencesEditor.commit();

        //Read the default values and set them as the current values.
        PreferenceManager.setDefaultValues(context, iRes, true);
    }

}
