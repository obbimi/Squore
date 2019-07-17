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

package com.doubleyellow.scoreboard.prefs;

/**
 * Preference with which user can control appearance of each scoreline in e.g. GameHistoryView.
 * Lines are store like
 * - 'R--1'
 * - '--R2'
 * - '-1L-'
 *
 * To have them appear with digits 'inside', the preference DigitsInside can be used. For presentation the lines are then transformed to
 * - 'R-1-'
 * - '--2R'
 * - '-1-L'
 *
 * For Badminton and Tabletennis the serve side is less interesting (or not applicable). They can/should use the HideServeSide
 */
public enum ScorelineLayout {
    DigitsRight  (false, false, false),
    DigitsInside (false, true , false),
    HideServeSide(false, false, true ),
    ;

    private boolean m_bSwap12 = false;
    private boolean m_bSwap34 = false;
    private boolean m_bHideLR = false;
    ScorelineLayout(boolean bSwap12, boolean bSwap34, boolean bHideLR) {
        m_bSwap12 = bSwap12;
        m_bSwap34 = bSwap34;
        m_bHideLR = bHideLR;
    }
    public boolean swap34() {
        return m_bSwap34;
    }
    public boolean hideServeSide() {
        return m_bHideLR;
    }
}
