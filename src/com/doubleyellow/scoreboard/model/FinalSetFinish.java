/*
 * Copyright (C) 2020  Iddo Hoeve
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

/** For GSMModel only */
public enum FinalSetFinish
{
    TieBreakTo7              (GSMModel.FS_NR_GAMES_AS_OTHER_SETS, 7),
    TieBreakTo10             (GSMModel.FS_NR_GAMES_AS_OTHER_SETS, 10),
    NoGames_TieBreakTo7      ( 0, 7),
    NoGames_TieBreakTo10     ( 0, 10),
    GamesTo12ThenTieBreakTo7 (GSMModel.FS_LIMITED_NR_OF_GAMES_12, 7),
    GamesTo12ThenTieBreakTo10(GSMModel.FS_LIMITED_NR_OF_GAMES_12, 10),
    NoTieBreak(GSMModel.FS_UNLIMITED_NR_OF_GAMES, GSMModel.NOT_APPLICABLE),
    ;
    private int m_iNrOfGamesToWinSet = GSMModel.FS_NR_GAMES_AS_OTHER_SETS; // not deviating, -2 unlimited
    private int m_iNrOfPointsToWinTiebreak = 7;
    FinalSetFinish(int iNrOfGames, int iNrOfPointsToWinTiebreak) {
        m_iNrOfGamesToWinSet = iNrOfGames;
        m_iNrOfPointsToWinTiebreak = iNrOfPointsToWinTiebreak;
    }
    public int numberOfGamesToWinSet() {
        return m_iNrOfGamesToWinSet;
    }
    public int numberOfPointsToWinTiebreak() {
        return m_iNrOfPointsToWinTiebreak;
    }

}
