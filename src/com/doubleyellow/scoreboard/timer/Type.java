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

package com.doubleyellow.scoreboard.timer;

import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.prefs.PreferenceKeys;

/**
 * Enumeration that holds all possible timers for a squash/tabletennis match.
 * - Warmup
 * - Pause between games
 * - Injury
 *      - Self Inflicted
 *      - Self Inflicted (Blood)
 *      - Contributed
 *      - Opponent Inflicted
 *
 * Note: for proper livescore support these values are also in 'live.ajax.js' of squore.double-yellow.be
 */
public enum Type {
    Warmup                  (PreferenceKeys.timerWarmup                  , 240 , R.string.oal_warmup                    , R.string.oa_halftime ),
    UntilStartOfFirstGame   (PreferenceKeys.timerPauseBeforeFirstGame    ,  60 , R.string.oal_pause                     , R.string.oa_fifteen_seconds ), // PSA: 120
    UntilStartOfNextGame    (PreferenceKeys.timerPauseBetweenGames       , 120 , R.string.oal_pause                     , R.string.oa_fifteen_seconds ), // PSA: 120

    // squash
    SelfInflictedInjury     (PreferenceKeys.timerSelfInflictedInjury     ,  3*60, R.string.sb_self_inflicted_injury      , R.string.oa_fifteen_seconds ), // 14.3.1
    SelfInflictedBloodInjury(PreferenceKeys.timerSelfInflictedBloodInjury,  5*60, R.string.sb_self_inflicted_blood_injury, R.string.oa_fifteen_seconds ), // 14.4.1 (blood)
    ContributedInjury       (PreferenceKeys.timerContributedInjury       , 15*60, R.string.sb_contributed_injury         , R.string.oa_fifteen_seconds ),
    OpponentInflictedInjury (PreferenceKeys.timerOpponentInflictedInjury , 15*60, R.string.sb_opponent_inflicted_injury  , R.string.oa_fifteen_seconds ),
    // tabletennis
    TowelingDown            (PreferenceKeys.timerTowelingDown            , 60   , R.string.toweling_down                 , R.string.oa_fifteen_seconds ),
    Timeout                 (PreferenceKeys.timerTimeout                 , 60   , R.string.timeout                       , R.string.oa_fifteen_seconds ),
    ;
    private final int            iSecs;
//  private final int[]          iR;
    private final int            iNameResId;
    private final int            iHalftTimeMsgResId;
    private final PreferenceKeys key;
    Type(PreferenceKeys key, int iSecs, int iR1, int iR2) {
      //this.iR    = new int[] { iR1, iR2 };
        this.key   = key;
        this.iSecs = iSecs;
        this.iNameResId = iR1;
        this.iHalftTimeMsgResId = iR2;
    }

    public int getNameResId() {
        return this.iNameResId;
    }
    public int getHalftTimeMsgResId() {
        return this.iHalftTimeMsgResId;
    }
    public int getDefaultSecs() {
        return this.iSecs;
    }
    public PreferenceKeys getPrefKey() {
        return this.key;
    }
}
