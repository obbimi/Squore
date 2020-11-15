package com.doubleyellow.scoreboard.model;

import com.doubleyellow.scoreboard.R;

public enum Misc {
    /** Timeout */
    TO(R.string.timeout_string_abbreviation),
    ;

    private int iResIdAbbr;
    Misc(int iResIdAbbr) {
        this.iResIdAbbr = iResIdAbbr;
    }
    public int getResourceIdAbbreviation() {
        return this.iResIdAbbr;
    }
}
