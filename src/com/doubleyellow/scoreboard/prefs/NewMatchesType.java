/*
 * Copyright (C) 2018  Iddo Hoeve
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

import com.doubleyellow.scoreboard.R;

public enum NewMatchesType {
    Poule                       (1, R.string.Poule                       ),
    TeamVsTeam_OneMatchPerPlayer(2, R.string.TeamVsTeam_OneMatchPerPlayer), /* Squash */
    TeamVsTeam_XMatchesPlayer   (2, R.string.TeamVsTeam_XMatchesPlayer   ), /* Tabletennis */
    ;
    private int m_iGroups = 1;
    private int m_iResId  = 0;
    NewMatchesType(int iGroups, int iResId) {
        m_iGroups = iGroups;
        m_iResId  = iResId;
    }
    public int getNumberOfGroups() {
        return m_iGroups;
    }
    /** invoked via reflection by e.g. SelectEnumView */
    public int getResourceId() {
        return m_iResId;
    }
}

