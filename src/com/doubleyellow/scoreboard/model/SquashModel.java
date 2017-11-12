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

import com.doubleyellow.util.MapUtil;

import java.util.Map;

public class SquashModel extends Model {

    SquashModel() {
        super();
        setNrOfGamesToWinMatch(3);
    }

    SquashModel(boolean bTmp) {
        init(); // todo: smaller init
    }

    @Override public SportType getSport() {
        return SportType.Squash;
    }

    @Override public Sport getSportForGame(int iSet1B) {
        return Sport.Squash;
    }

    //-------------------------------
    // serve side/sequence
    //-------------------------------

    public void setDoublesServeSequence(DoublesServeSequence dsq) {
        super._setDoublesServeSequence(dsq);
    }

    @Override public DoublesServeSequence getDoubleServeSequence(int iGameZB) {
        return m_doubleServeSequence;
    }

    @Override public String convertServeSideCharacter(String sRLInternational, ServeSide serveSide, String sHandoutChar) {
        return sRLInternational + sHandoutChar;
    }
    @Override public boolean showChangeSidesMessageInGame(int iGameZB) { return false; }

    @Override Player determineServerForNextGame(int iGame, int iScoreA, int iScoreB) {
        return determineServerForNextGame_Squash(iScoreA, iScoreB);
    }

    @Override void determineServerAndSideForUndoFromPreviousScoreLine(ScoreLine lastValidWithServer, ScoreLine slRemoved) {
        if ( lastValidWithServer != null ) {
            Player    pServer              = lastValidWithServer.getScoringPlayer();
            ServeSide serversPreferredSide = MapUtil.getMaxKey(m_player2ServeSideCount.get(pServer), ServeSide.R);
            Player    lastServer           = lastValidWithServer.getServingPlayer();
            ServeSide lastServeSide        = lastValidWithServer.getServeSide();
            ServeSide nextServeSide        = pServer.equals(lastServer)? lastServeSide.getOther(): serversPreferredSide;

            setServerAndSide(pServer, nextServeSide, null);
        } else if (slRemoved != null ) {
            Player    removedServingPlayer = slRemoved.getServingPlayer();
            ServeSide removedServeSide     = slRemoved.getServeSide();
            if ( removedServingPlayer != null ) {
                setServerAndSide(removedServingPlayer, null, null);
            }
            if ( removedServeSide != null ) {
                setServerAndSide(null, removedServeSide, null);
            }
        }
        setLastPointWasHandout(false);
    }

    //-------------------------------
    // game/match ball
    //-------------------------------

    @Override Player[] calculateIsPossibleGameVictoryFor(When when, Map<Player, Integer> gameScore, boolean bFromIsMatchBallFrom) {
        return super.calculateIsPossibleGameVictoryFor_Squash_Tabletennis(when, gameScore);
    }

    @Override Player[] calculatePossibleMatchVictoryFor(When when, Player[] paGameVictoryFor) {
        return super.isPossibleMatchBallFor_Squash_TableTennis(when, paGameVictoryFor);
    }

    //-------------------------------
    // score
    //-------------------------------

    @Override public void changeScore(Player player) {
        super.changeScore_Squash_Racketlon(player, true, null);
    }

    @Override public String getResultShort() {
        return super.getResultShort_Squash_TableTennis();
    }

    //-------------------------------
    // JSON
    //-------------------------------

    //-------------------------------
    // conduct/appeal
    //-------------------------------


    @Override public void recordConduct(Player pMisbehaving, Call call, ConductType conductType) {
        super.recordConduct_Squash_Racketlon(pMisbehaving, call, conductType);
    }

    @Override public void recordAppealAndCall(Player appealing, Call call) {
        super.recordAppealAndCall_Squash_Racketlon(appealing, call);
    }
}
