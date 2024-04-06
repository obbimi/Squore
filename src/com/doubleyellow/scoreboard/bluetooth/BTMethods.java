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
    /** = ServeSide */
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
    jsonMatchReceived(false),
    requestCountryFlag(false),

    /* mainly used to be send from wearable to more user-friendly handheld */
    //openSuggestMatchSyncDialogOnOtherPaired(false),
    /** to check when app opens if it also open on counterpart device */
    resume(false),
    resume_confirmed(false),
    /** to let counter part app know this one is losing focus */
    paused(false),

    lock(false),
    unlock(false),
    updatePreference(false),

    /** Typically used only in BLE setup with single device allowing to change the score.
     * Single parameter specifying the player whoos score should be changed: A or B */
    changeScoreBLE(false),
    /**
     * Typically used only in BLE setup with each player/team having a device allowing to change the score but confirmation by opponent is required.
     * Two parameters
     * - first the player identifying what wristband a button was pressed on
     * - second identifying what button was pressed on the device (@see com.doubleyellow.scoreboard.bluetooth_le.BLEDeviceButton)
     **/
    changeScoreBLEConfirm(false),
    /**
     * Typically used only in BLE setup with each player/team having a device allowing to undo the score but only for 'self' to prevent cheating.
     * one parameter
     * - first the player identifying what wristband a button was pressed on
     **/
    undoScoreForInitiatorBLE(false),
    ;
    private boolean bVerifyScore = false;
    BTMethods(boolean verifyScore) {
        this.bVerifyScore = verifyScore;
    }
    public boolean verifyScore() {

        return this.bVerifyScore;
    }
}
