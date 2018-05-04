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
