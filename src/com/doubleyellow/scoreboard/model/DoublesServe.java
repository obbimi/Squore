package com.doubleyellow.scoreboard.model;

public enum DoublesServe {
    I, // In =player 1
    O, // Out=player 2
    NA,
    ;
    public DoublesServe getOther() {
        if ( this.equals(I) ) return O;
        if ( this.equals(O) ) return I;
        return NA;
    }
}
