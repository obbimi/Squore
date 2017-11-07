package com.doubleyellow.scoreboard.model;

/** In case of doubles, Player.A means Team A, and Player.B means Team B */
public enum Player {
    A,
    B, /* TODO: because of double and double sequence rename this Player.B to Player.X */
    //C,
    ;
    public Player getOther() {
        return this.equals(A) ? B : A;
    }
}
