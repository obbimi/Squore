package com.doubleyellow.scoreboard.prefs;

import android.content.Context;
import android.content.DialogInterface;
import android.preference.CheckBoxPreference;
import android.preference.DialogPreference;
import android.preference.Preference;
import android.util.AttributeSet;
import com.doubleyellow.scoreboard.main.ScoreBoard;

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
            ScoreBoard.matchModel.setDirty();
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
