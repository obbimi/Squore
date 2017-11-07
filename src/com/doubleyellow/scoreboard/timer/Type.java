package com.doubleyellow.scoreboard.timer;

import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.prefs.PreferenceKeys;

/**
 * Enumeration that holds all possible timers for a squash match.
 * - Warmup
 * - Pause between games
 * - Injury
 *      - Self Inflicted
 *      - Contributed
 *      - Opponent Inflicted
 */
public enum Type {
    Warmup                 (PreferenceKeys.timerWarmup                 , 300  , R.string.oal_warmup                  , R.string.oa_halftime ),
    UntillStartOfNextGame  (PreferenceKeys.timerPauseBetweenGames      ,  90  , R.string.oal_pause                   , R.string.oa_fifteen_seconds ), // PSA: 120

    SelfInflictedInjury    (PreferenceKeys.timerSelfInflictedInjury    ,  3*60, R.string.sb_self_inflicted_injury    , R.string.oa_fifteen_seconds ),
    ContributedInjury      (PreferenceKeys.timerContributedInjury      , 15*60, R.string.sb_contributed_injury       , R.string.oa_fifteen_seconds ),
    OpponentInflictedInjury(PreferenceKeys.timerOpponentInflictedInjury, 15*60, R.string.sb_opponent_inflicted_injury, R.string.oa_fifteen_seconds ),
    ;
    private final int            iSecs;
    private final int[]          iR;
    private final PreferenceKeys key;
    Type(PreferenceKeys key, int iSecs, int iR1, int iR2) {
        this.iR    = new int[] { iR1, iR2 };
        this.key   = key;
        this.iSecs = iSecs;
    }

    public int getMsgId(int i) {
        return this.iR[i];
    }
    public int getDefaultSecs() {
        return this.iSecs;
    }
    public PreferenceKeys getPrefKey() {
        return this.key;
    }
}
