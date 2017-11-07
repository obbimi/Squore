package com.doubleyellow.scoreboard.model;

import com.doubleyellow.scoreboard.R;

public enum Call {
    /** No let */
    NL(false, ScoreAffect.LosePoint, R.string.oa_no_let , R.string.oa_no_let     , R.string.no_let_abbreviation),
    /** Yes let */
    YL(false, ScoreAffect.None     , R.string.oa_yes_let, R.string.oa_yes_let    , R.string.yes_let_abbreviation),
    /** Stroke */
    ST(false, ScoreAffect.WinPoint , R.string.oa_stroke , R.string.oa_stroke_to_x, R.string.stroke_abbreviation),

    /** Conduct warning */
    CW(true, ScoreAffect.None      , R.string.oa_conduct_warning, R.string.oa_conduct_warning_x_for_type_y     , R.string.conduct_warning_abbreviation),
    /** Conduct stroke */
    CS(true, ScoreAffect.LosePoint , R.string.oa_conduct_stroke , R.string.oa_conduct_x__stroke_to_y_for_type_t, R.string.conduct_stroke_abbreviation),

    // Conduct game
    CG(true, ScoreAffect.LoseGame  , R.string.oa_conduct_game   , R.string.oa_conduct_x__game_to_y_for_type_t  , R.string.conduct_game_abbreviation),
    // Conduct match
    CM(true, ScoreAffect.LoseMatch , R.string.oa_conduct_match  , R.string.oa_conduct_match                    , R.string.conduct_match_abbreviation),
    ;
    private boolean    bIsConduct      = false;
    private ScoreAffect scoreAffect = ScoreAffect.None;
    private int        iResIdLabel;
    private int        iResIdCall;
    private int        iAbbrResId;
    Call(boolean bIsConduct, ScoreAffect eScoreAffect, int iResIdLabel, int iResIdCall, int iResIdAbbr) {
        this.bIsConduct      = bIsConduct;
        this.scoreAffect     = eScoreAffect;
        this.iResIdLabel     = iResIdLabel;
        this.iResIdCall      = iResIdCall;
        this.iAbbrResId      = iResIdAbbr;
    }

    public ScoreAffect getScoreAffect() {
        return scoreAffect;
    }
    public boolean hasScoreAffect() {
        return scoreAffect.equals(ScoreAffect.None)==false;
    }
    public boolean isConduct() {
        return bIsConduct;
    }
    public int getResourceIdLabel() {
        return iResIdLabel;
    }
    /** e.g. 'Stroke to %s' */
    public int getResourceIdCall() {
        return iResIdCall;
    }

    public int getAbbreviationResourceId() {
        return iAbbrResId;
    }

    public enum ScoreAffect {
        None,
        WinPoint,
        LosePoint,
        LoseGame,
        LoseMatch,
    }
}
