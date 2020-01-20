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

package com.doubleyellow.scoreboard.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.model.Player;
import com.doubleyellow.scoreboard.vico.IBoard;
import com.doubleyellow.util.StringUtil;

/**
 * Dialog in which the referee must indicate which of the two players of a team will actually start serving/receiving.
 * Introduced for Tabletennis and Badminton.
 *
 * Subclass used to indicate who is first server.
 * Subclass used to indicate who is first receiver.
 */
public abstract class DoublesFirstServerReceiver extends BaseAlertDialog
{
    DoublesFirstServerReceiver(Context context, Model matchModel, ScoreBoard scoreBoard, boolean bForServer) {
        super(context, matchModel, scoreBoard);
        m_bForServer = bForServer;
    }

    @Override public boolean storeState(Bundle outState) {
        return true;
    }

    @Override public boolean init(Bundle outState) {
        return true;
    }

    private boolean m_bForServer;
    private Player doublesTeam = null;

    @Override public void show() {
        this.doublesTeam = (m_bForServer) ? matchModel.getServer() : matchModel.getReceiver();

        Player pServer   = matchModel.getServer();
        int    iResourceServeOrReceive = pServer.equals(this.doublesTeam) ? R.string.sb_serve : R.string.sb_receive;
        String sTitle    = context.getString(R.string.sb_which_player_of_team_will_start_to_x, getString(iResourceServeOrReceive));
        String sMessage  = null;
        int nrOfFinishedGames = matchModel.getNrOfFinishedGames();
        if ( nrOfFinishedGames > 0 ) {
            sMessage = sTitle;
            sTitle   = StringUtil.capitalize(getGameOrSetString(R.string.oa_game)) + " " + (nrOfFinishedGames+1);
        }

        String   sNames    = matchModel.getName(this.doublesTeam);
        String[] saNames   = sNames.split("/");
        adb.setTitle         (sTitle);
        if (StringUtil.isNotEmpty(sMessage) ) {
            adb.setMessage       (sMessage);
        }
        adb.setIcon          (R.drawable.ic_action_refresh)
           .setPositiveButton(saNames[0], dialogClickListener)
           .setNegativeButton(saNames[1], dialogClickListener)
           ;

        adb.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override public void onDismiss(DialogInterface dialog) {
                scoreBoard.triggerEvent(ScoreBoard.SBEvent.serverReceiverDialogEnded, DoublesFirstServerReceiver.this);
            }
        });
        dialog = adb.show();
    }

    private DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
        @Override public void onClick(DialogInterface dialog, int which) {
            handleButtonClick(which);
        }
    };

    public static final int BTN_PLAYER_0 = DialogInterface.BUTTON_POSITIVE;
    public static final int BTN_PLAYER_1 = DialogInterface.BUTTON_NEGATIVE;
    @Override public void handleButtonClick(int which) {
        boolean bSwapPlayersOrder = false;

        switch(matchModel.getSport()) {
            case Tabletennis:
                // in the model both server and receiver are set to 'In' (first player of team)
                switch (which) {
                    case BTN_PLAYER_0: bSwapPlayersOrder = false;break;
                    case BTN_PLAYER_1: bSwapPlayersOrder = true ;break;
                }
                break;
            case Badminton:
                // in landscape we try to set
                // - serving player from team on the left of the scoreboard
                // - as second if he serves from the right, as first if he serves from the left
                // similar but swapped for the receiver so that serve is always going from first player of one team to second player of other team to simulate 'diagonal' serving
                if ( doublesTeam.equals(IBoard.m_firstPlayerOnScreen) ) {
                    // first server/receiver should be set a second player
                    switch (which) {
                        case BTN_PLAYER_0: bSwapPlayersOrder = true ;break;
                        case BTN_PLAYER_1: bSwapPlayersOrder = false;break;
                    }
                } else {
                    // first server/receiver should be set a first player
                    switch (which) {
                        case BTN_PLAYER_0: bSwapPlayersOrder = false;break;
                        case BTN_PLAYER_1: bSwapPlayersOrder = true ;break;
                    }
                }
                break;
            default:
                break;
        }
        if ( bSwapPlayersOrder ) {
            if ( matchModel.hasStarted() ) {
                if ( Brand.isTabletennis() ) {
                    // DoublesFirstReceiver will not be shown, receiving team needs to be swapped to
                    scoreBoard._swapDoublePlayers( new Player[] { doublesTeam, doublesTeam.getOther() }, true);
                } else {
                    // badminton: receiver may be choosen in every game
                    scoreBoard._swapDoublePlayers(doublesTeam);
                }
            } else {
                scoreBoard._swapDoublePlayers(doublesTeam);
            }
        }
    }

}
