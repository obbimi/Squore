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

import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.model.Player;
import com.doubleyellow.scoreboard.vico.IBoard;
import com.doubleyellow.scoreboard.view.PlayersButton;

abstract class ScoreBoardListener {
    ScoreBoardListener(ScoreBoard scoreBoard) {
        this.scoreBoard = scoreBoard;
    }
    final ScoreBoard scoreBoard;

    boolean handleMenuItem(int id, Object... ctx) {
        return scoreBoard.handleMenuItem(id, ctx);
    }
    //-------------------------------------------------------------------------
    // Controller listeners helper methods
    //-------------------------------------------------------------------------
    int getXmlIdOfParent(View view) {
        int id = view.getId();
        Player player = IBoard.m_id2player.get(id);
        if ( player == null ) {
            String sTag = String.valueOf(view.getTag());
            if ( sTag.contains(PlayersButton.SUBBUTTON)) {
                id = Integer.parseInt(sTag.replaceFirst(PlayersButton.SUBBUTTON + ".*", ""));
                player = IBoard.m_id2player.get(id);
            }
            if ( player == null ) {
                return 0;
            }
        }
        return id;
    }
    Model getMatchModel() {
        return ScoreBoard.getMatchModel();
    }

    boolean warnModelIsLocked() {
        return scoreBoard.warnModelIsLocked();
    }
    void changeSide(Player p) {
        scoreBoard.changeSide(p);
    }
}
