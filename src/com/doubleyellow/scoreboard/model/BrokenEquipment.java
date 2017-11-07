package com.doubleyellow.scoreboard.model;

import com.doubleyellow.scoreboard.R;

public enum BrokenEquipment {
    BS(R.string.broken_string_abbreviation),
    BR(R.string.broken_racket_abbreviation),
    BB(R.string.broken_ball_abbreviation),
    ;

    private int iResIdAbbr;
    BrokenEquipment(int iResIdAbbr) {
        this.iResIdAbbr = iResIdAbbr;
    }
    /* Invoked via reflection !
    private int iResId = 0;
    public int getResourceId() {
        return this.iResId;
    }
    */
    public int getResourceIdAbbreviation() {
        return this.iResIdAbbr;
    }
}
