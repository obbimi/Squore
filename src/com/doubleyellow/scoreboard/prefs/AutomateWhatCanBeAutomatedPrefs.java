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