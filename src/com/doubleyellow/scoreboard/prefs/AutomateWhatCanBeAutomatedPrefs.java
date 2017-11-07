package com.doubleyellow.scoreboard.prefs;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.util.Feature;

import java.util.EnumSet;
import java.util.Map;

/**
 * Preference specifically created to set a few related 'Automatable' preferences at once.
 */
public class AutomateWhatCanBeAutomatedPrefs extends MultiPrefsDialog
{
    public AutomateWhatCanBeAutomatedPrefs(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override public void getPreferencesToSet(Map<PreferenceKeys, Object> mReturn, boolean bPositive) {
        Feature feature = bPositive ? Feature.Automatic : Feature.Suggest;

        mReturn.put(PreferenceKeys.useTossFeature                 , feature);
        mReturn.put(PreferenceKeys.useTimersFeature               , feature);
        mReturn.put(PreferenceKeys.useOfficialAnnouncementsFeature, feature);
        mReturn.put(PreferenceKeys.endGameSuggestion              , feature);

        if ( feature.equals(Feature.Automatic) ) {
            mReturn.put(PreferenceKeys.indicateGameBall          , true);
            //mReturn.put(PreferenceKeys.floatingMessageForGameBall, true);
            mReturn.put(PreferenceKeys.floatingMessageForGameBallOn, EnumSet.allOf(ShowOnScreen.class));
            mReturn.put(PreferenceKeys.showNewMatchFloatButton   , true);
        }
    }

}