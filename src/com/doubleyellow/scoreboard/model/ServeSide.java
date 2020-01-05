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

package com.doubleyellow.scoreboard.model;

/**
 * Actual L and R for non table tennis.
 *
 * For table tennis we use the ordinal() value: R than means the first serve, L means the second serve, and thus in a tiebreak for table tennis it is always serveside R.
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
