/*
 * Copyright (C) 2022  Iddo Hoeve
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
 * For GSMModel only.
 *
 * Note:
 * it's not allowed to change balls before a tiebreak starts, no matter if its a normal set-tiebreak or a match-tiebreak
 **/
public enum NewBalls
{
    AfterFirst7ThenEach9  (7, 9, GSMModel.NOT_APPLICABLE),
    AfterFirst9ThenEach11 (9, 11, GSMModel.NOT_APPLICABLE),
    AfterFirst11ThenEach13(11, 13, GSMModel.NOT_APPLICABLE),
    BeforeSet3            (GSMModel.NOT_APPLICABLE, GSMModel.NOT_APPLICABLE, 3),

    //AfterFirst1ThenEach3_TestingOnly  (1, 3, GSMModel.NOT_APPLICABLE), // TODO: only for testing
    ;
    private int m_iFirstX        = GSMModel.NOT_APPLICABLE;
    private int m_iEachX         = GSMModel.NOT_APPLICABLE;
    private int m_iAtStartOfSetX = GSMModel.NOT_APPLICABLE;
    NewBalls(int iFirstX, int iEachX, int iAtStartOfSetX) {
        m_iFirstX        = iFirstX;
        m_iEachX         = iEachX;
        m_iAtStartOfSetX = iAtStartOfSetX;
    }
    public int afterFirstXgames() {
        return m_iFirstX;
    }
    public int afterEachXgames () {
        return m_iEachX;
    }
    public int atStartOfSetX () {
        return m_iAtStartOfSetX;
    }

}
