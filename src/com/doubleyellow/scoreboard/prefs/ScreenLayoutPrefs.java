package com.doubleyellow.scoreboard.prefs;

import android.content.Context;
import android.util.AttributeSet;

import java.util.Map;

/**
 * Preference specifically created to set a few related 'Layout' preferences at once.
 */
public class ScreenLayoutPrefs extends MultiPrefsDialog
{
    public ScreenLayoutPrefs(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override public void getPreferencesToSet(Map<PreferenceKeys, Object> mReturn, boolean bPositive) {
        //PreferenceValues.setEnum(PreferenceKeys.endGameSuggestion              , context, feature);
        mReturn.put(PreferenceKeys.showFullScreen, bPositive);
        mReturn.put(PreferenceKeys.showActionBar , bPositive==false);
    }
}