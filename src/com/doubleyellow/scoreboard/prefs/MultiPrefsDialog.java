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
import android.preference.CheckBoxPreference;
import android.preference.DialogPreference;
import android.preference.Preference;
import android.util.AttributeSet;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Preference specifically created to set a few related preferences at once.
 */
abstract class MultiPrefsDialog extends DialogPreference
{
    protected Context context;

    MultiPrefsDialog(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        this.context = context;
    }

    @Override public void onClick(DialogInterface dialog, int iPressed)
    {
        super.onClick(dialog, iPressed);

        Map<PreferenceKeys, Object> preferencesToSet = new HashMap<PreferenceKeys, Object>();
        getPreferencesToSet(preferencesToSet, iPressed == DialogInterface.BUTTON_POSITIVE);
        for(PreferenceKeys key : preferencesToSet.keySet()) {
            Object oValue = preferencesToSet.get(key);
            if ( oValue instanceof Boolean) {
                updatePrefAndTitle(key, (Boolean) oValue);
            } else if ( oValue instanceof String) {
                updatePrefAndTitle(key, (String) oValue);
            } else if ( oValue instanceof Enum) {
                updatePrefAndTitle(key, (Enum) oValue);
            } else if ( oValue instanceof EnumSet) {
                updatePrefAndTitle(key, (EnumSet) oValue);
            }
        }

        if ( preferencesToSet.size() > 0 ) {
            OnPreferenceChangeListener onPreferenceChangeListener = getOnPreferenceChangeListener();
            if (onPreferenceChangeListener != null) {
                onPreferenceChangeListener.onPreferenceChange(this, true);
            }

            // ensure scoreboard elements are redrawn
            Preferences.setModelDirty();
        }
    }

    public abstract void getPreferencesToSet(Map<PreferenceKeys, Object> mReturn, boolean bPositive);

    private <T extends Enum<T>> void updatePrefAndTitle(PreferenceKeys prefKey, Set<T> vals) {
        PreferenceValues.setStringSet(prefKey, vals, context);

        Preference pref = this.findPreferenceInHierarchy(prefKey.toString());
        PreferenceValues.updatePreferenceTitle(pref);
    }
    private <T extends Enum<T>> void updatePrefAndTitle(PreferenceKeys prefKey, T val) {
        PreferenceValues.setEnum(prefKey, context, val);

        Preference pref = this.findPreferenceInHierarchy(prefKey.toString());
        PreferenceValues.updatePreferenceTitle(pref, val);
    }
    private void updatePrefAndTitle(PreferenceKeys prefKey, String val) {
        PreferenceValues.setString(prefKey, context, val);

        Preference pref = this.findPreferenceInHierarchy(prefKey.toString());
        PreferenceValues.updatePreferenceTitle(pref, val, context);
    }
    private void updatePrefAndTitle(PreferenceKeys prefKey, boolean val) {
        PreferenceValues.setBoolean(prefKey, context, val);

        Preference pref = this.findPreferenceInHierarchy(prefKey.toString());
        // actually check/uncheck the pref
        if ( pref instanceof CheckBoxPreference ) {
            CheckBoxPreference boxPreference = (CheckBoxPreference) pref;
            boxPreference.setChecked(val);
        }
    }

}
