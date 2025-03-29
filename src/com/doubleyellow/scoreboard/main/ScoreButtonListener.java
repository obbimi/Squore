/*
 * Copyright (C) 2025  Iddo Hoeve
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
package com.doubleyellow.scoreboard.main;

import android.view.View;

import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.model.Player;
import com.doubleyellow.scoreboard.vico.IBoard;

public class ScoreButtonListener extends ScoreBoardListener implements View.OnClickListener
{
    ScoreButtonListener(ScoreBoard scoreBoard) {
        super(scoreBoard);
    }

    @Override public void onClick(View view) {
        //Log.d(TAG, "Received click for model " + matchModel);
        if ( scoreBoard.m_bleConfigHandler != null && scoreBoard.m_bleConfigHandler.clearBLEConfirmationStatus() ) { return; }

        Player player = IBoard.m_id2player.get(view.getId());
        if ( getMatchModel().isPossibleGameBallFor(player) && (scoreBoard.bGameEndingHasBeenCancelledThisGame == false) ) {
            // score will go to game-end, and most likely a dialog will be build and show.
            // Prevent any accidental score changes while dialog is about to be shown.
            // Mainly to prevent odd behaviour of the app for when people are 'quickly' entering a score by tapping rapidly on score buttons
            scoreBoard.disableScoreButton(view);
        }
        if ( scoreBoard.dialogManager.dismissIfTwoTimerView() /*cancelTimer()*/ ) {
            // only possible for inline timer
            if ( scoreBoard.isDialogShowing() ) {
                // e.g DoublesFirstServer may auto-show after timer is cancelled
                return;
            }
        }
        scoreBoard.enableScoreButton(player.getOther());
        handleMenuItem(R.id.pl_change_score, player);
    }
}
