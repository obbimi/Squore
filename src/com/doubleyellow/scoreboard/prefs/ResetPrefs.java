package com.doubleyellow.scoreboard.prefs;

import android.content.Context;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;

import com.doubleyellow.prefs.RWValues;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.main.ScoreBoard;

public class ResetPrefs extends DialogPreference
{
    private Context context;

    public ResetPrefs(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        this.context = context;
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
                ScoreBoard.matchModel.setDirty();

                PreferenceValues.setRestartRequired(context);
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                break;
        }
    }

    static void resetToDefaults(Context context, int iRes) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        SharedPreferences.Editor preferencesEditor = preferences.edit();
        //Clear all the saved preference values.
        preferencesEditor.clear();
        //Commit all changes.
        preferencesEditor.commit();

        //Read the default values and set them as the current values.
        PreferenceManager.setDefaultValues(context, iRes, true);
    }

}