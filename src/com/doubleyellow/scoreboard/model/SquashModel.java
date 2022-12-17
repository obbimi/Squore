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

    //-------------------------------
    // serve side/sequence
    //-------------------------------

    public boolean setDoublesServeSequence(DoublesServeSequence dsq) {
        return super._setDoublesServeSequence(dsq);
    }

    @Override public DoublesServeSequence getDoubleServeSequence(int iGameZB) {
        if ( DoublesServeSequence.NA.equals(m_doubleServeSequence) ) {
            m_doubleServeSequence = DoublesServeSequence.A1B1A2B2;
        }
        return m_doubleServeSequence;
    }

    @Override public String convertServeSideCharacter(String sRLInternational, ServeSide serveSide, String sHandoutChar) {
        return sRLInternational + sHandoutChar;
    }
    @Override public boolean showChangeSidesMessageInGame(int iGameZB) { return false; }

    @Override Player determineServerForNextGame(int iGame, int iScoreA, int iScoreB) {
        return determineServerForNextGame_SQ_BM(iScoreA, iScoreB);
    }

    @Override void determineServerAndSideForUndoFromPreviousScoreLine(ScoreLine lastValidWithServer, ScoreLine slRemoved) {
        if ( lastValidWithServer != null ) {
            Player    pServer              = lastValidWithServer.getScoringPlayer();
            ServeSide serversPreferredSide = MapUtil.getMaxKey(m_player2ServeSideCount.get(pServer), ServeSide.R);
            Player    lastServer           = lastValidWithServer.getServingPlayer();
            ServeSide lastServeSide        = lastValidWithServer.getServeSide();
            // safety precaution... should not occur
            if ( lastServeSide == null ) {
                lastServeSide = ServeSide.R;
            }
            ServeSide nextServeSide        = serversPreferredSide;
            if ( pServer.equals(lastServer) && (lastServeSide != null) ) {
                nextServeSide = lastServeSide.getOther();
            }

            setServerAndSide(pServer, nextServeSide, null, true);
        } else if (slRemoved != null ) {
            Player    removedServingPlayer = slRemoved.getServingPlayer();
            ServeSide removedServeSide     = slRemoved.getServeSide();
            if ( removedServingPlayer != null ) {
                setServerAndSide(removedServingPlayer, null, null, true);
            }
            if ( removedServeSide != null ) {
                setServerAndSide(null, removedServeSide, null, true);
            }
        }
        setLastPointWasHandout(false);
    }

    //-------------------------------
    // game/match ball
    //-------------------------------

    @Override Player[] calculateIsPossibleGameVictoryFor(When when, Map<Player, Integer> gameScore, boolean bFromIsMatchBallFrom) {
        return super.calculateIsPossibleGameVictoryFor_SQ_TT_BM_RL(when, gameScore, getNrOfPointsToWinGame());
    }

    @Override Player[] calculatePossibleMatchVictoryFor(When when, Player[] paGameVictoryFor) {
        return super.isPossibleMatchBallFor_SQ_TT_BM(when, paGameVictoryFor);
    }

    //-------------------------------
    // score
    //-------------------------------

    @Override public void changeScore(Player player) {
        super.changeScore_SQ_RB(player, true, null);
    }

    @Override public String getResultShort() {
        return super.getResultShort_SQ_TT_BM();
    }

    //-------------------------------
    // JSON
    //-------------------------------

    //-------------------------------
    // conduct/appeal
    //-------------------------------


    @Override public void recordConduct(Player pMisbehaving, Call call, ConductType conductType) {
        super.recordConduct_SQ_RL_RB(pMisbehaving, call, conductType);
    }

    @Override public void recordAppealAndCall(Player appealing, Call call) {
        if ( call == null ) { return; }
        super.recordAppealAndCall_SQ_RL_RB(appealing, call);
    }
}
