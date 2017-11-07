package com.doubleyellow.scoreboard.prefs;

import android.content.Context;
import android.util.AttributeSet;
import com.doubleyellow.util.Feature;

import java.util.Map;

/**
 * Preference specifically created to set a few related preferences to improve 'Live Scoring' with squore.
 */
public class LiveScorePrefs extends MultiPrefsDialog
{
    static ShareMatchPrefs theOneForLiveScoring = ShareMatchPrefs.LinkWithFullDetailsEachPoint;

    public LiveScorePrefs(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override public void getPreferencesToSet(Map<PreferenceKeys, Object> mReturn, boolean bPositive) {
        if ( bPositive ) {
            mReturn.put(PreferenceKeys.shareAction    , theOneForLiveScoring);
            mReturn.put(PreferenceKeys.useShareFeature, Feature.Automatic);
        } else {
            // TODO: back to defaults??
        }
    }
}