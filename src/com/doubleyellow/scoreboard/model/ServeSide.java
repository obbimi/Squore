package com.doubleyellow.scoreboard.model;

/**
 * Actual L and R for non table tennis.
 *
 * For table tennis we use the ordinal() value: R than means the first serve, L means the second serve, and thus in a teabreak for table tennis it is always serveside R.
 */
public enum ServeSide {
    R(com.doubleyellow.scoreboard.R.string.right_serveside_single_char),
    L(com.doubleyellow.scoreboard.R.string.left_serveside_single_char),
    ;
    ServeSide(int iResId) {
        this.iResId = iResId;
    }
    public ServeSide getOther() {
        return this.equals(L)? R : L;
    }
    private int iResId;
    public int getSingleCharResourceId() {
        return iResId;
    }
}
