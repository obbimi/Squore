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
import android.util.AttributeSet;
import com.doubleyellow.util.Feature;

import java.util.Map;

/**
 * Preference specifically created to set a few related preferences to improve 'Live Scoring' with squore.
 */
public class LiveScorePrefs extends MultiPrefsDialog
{
    //public static ShareMatchPrefs theOneForLiveScoring = ShareMatchPrefs.LinkWithFullDetailsEachPoint;
    //public static Feature         theFeatureForLiveScoring = Feature.Automatic;

    public LiveScorePrefs(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override public void getPreferencesToSet(Map<PreferenceKeys, Object> mReturn, boolean bPositive) {
        if ( bPositive ) {
            mReturn.put(PreferenceKeys.postEveryChangeToSupportLiveScore, true);
            //mReturn.put(PreferenceKeys.shareAction    , theOneForLiveScoring);
            //mReturn.put(PreferenceKeys.useShareFeature, theFeatureForLiveScoring);
        } else {
            // TODO: back to defaults??
        }
    }
}