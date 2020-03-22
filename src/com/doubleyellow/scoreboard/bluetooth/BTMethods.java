/*
 * Copyright (C) 2019  Iddo Hoeve
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
package com.doubleyellow.scoreboard.bluetooth;

public enum BTMethods {
    Toast(false)       ,

    changeScore        (true),
    recordAppealAndCall(true),
    recordConduct      (true),
    undoLast           (true),
    undoLastForScorer  (true),
    changeSide         (false),
    swapDoublePlayers  (false),
    /* TODO: rename to switchSides */
    swapPlayers        (false),
    endGame            (false),

    timestampStartOfGame       (false),
    startTimer                 (false),
    restartTimerWithSecondsLeft(false),
    cancelTimer                (false),

    changeColor (false),

    restartScore(false),
    toggleGameScoreView(false),

    requestCompleteJsonOfMatch(false),
    requestCountryFlag(false),

    lock(false),
    unlock(false),
    ;
    private boolean bVerifyScore = false;
    BTMethods(boolean verifyScore) {
        this.bVerifyScore = verifyScore;
    }
    public boolean verifyScore() {

        return this.bVerifyScore;
    }
}
