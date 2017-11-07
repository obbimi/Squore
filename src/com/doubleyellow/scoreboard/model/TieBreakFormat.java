package com.doubleyellow.scoreboard.model;

public enum TieBreakFormat
{
    // most common (or even only one if it is NOT squash */
    TwoClearPoints     (null               , true ),
    //--------------------
    // old, now less common squash tiebreak format
    //--------------------
    SelectOneOrTwo     (new int[] {0, 1   }, false),
    //--------------------
    // uncommon squash tiebreak formats
    //--------------------
    SelectOneTwoOrThree(new int[] {0, 1 ,2}, false),
    SelectOneOrThree   (new int[] {0    ,2}, false),
    SuddenDeath        (null               , false),
    ;
    private int[]   iaAdd           = null;
    private boolean bTwoClearPoints = false;
    TieBreakFormat(int[] iaAdd, boolean bTwoClearPoints) {
        this.iaAdd = iaAdd;
        this.bTwoClearPoints = bTwoClearPoints;
    }

    public int[] getAddToEndScoreOptions() {
        return iaAdd;
    }
    public boolean needsTwoClearPoints() {
        return bTwoClearPoints;
    }
}
