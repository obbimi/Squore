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

package com.doubleyellow.scoreboard.model;

import com.doubleyellow.util.MapUtil;

import java.util.Map;

/**
 * http://www.worldbadminton.com/rules/
 */
public class BadmintonModel extends Model
{
    BadmintonModel() {
        super();
        setTiebreakFormat(TieBreakFormat.TwoClearPoints);
        setNrOfGamesToWinMatch(2);
        setNrOfPointsToWinGame(21);
        setEnglishScoring(false);
    }

    @Override public SportType getSport() {
        return SportType.Badminton;
    }

    @Override public Sport getSportForGame(int iGame1B) {
        return Sport.Badminton;
    }

    //-------------------------------
    // Toweling
    //-------------------------------

    public boolean isTowelingDownScore(int iEveryXpoints, int iIfHighestScoreEquals) {
        return getMaxScore() == iIfHighestScoreEquals && getNrOfPointsToWinGame() == 21;
    }

    //-------------------------------
    // serve side/sequence
    //-------------------------------

    @Override public DoublesServeSequence getDoubleServeSequence(int iGameZB) {
        return DoublesServeSequence.A1B1A2B2;
    }

    @Override void determineServerAndSideForUndoFromPreviousScoreLine(ScoreLine lastValidWithServer, ScoreLine slRemoved) {
        super.determineServerAndSide_Badminton(true, slRemoved.getServingPlayer());
    }

    @Override Player determineServerForNextGame(int iGameZB, int iScoreA, int iScoreB) {
        return determineServerForNextGame_Squash(iScoreA, iScoreB);
    }

    @Override public String convertServeSideCharacter(String sRLInternational, ServeSide serveSide, String sHandoutChar) {
        return sRLInternational /* + sHandoutChar*/;
    }

    @Override public boolean showChangeSidesMessageInGame(int iGameZB) {
        // only in the last game when 11 points are reached (in a game to 21)
        if ( iGameZB + 1 == getNrOfGamesToWinMatch() * 2 + 1 ) {
            return true;
        }
        return false;
    }

    //-------------------------------
    // game/match ball
    //-------------------------------

    @Override Player[] calculateIsPossibleGameVictoryFor(When when, Map<Player, Integer> gameScore, boolean bFromIsMatchBallFrom) {
        if ( getNrOfPointsToWinGame() == 21 ) {
            switch (when) {
                case Now:
                    if ( getMaxScore() == 30 && getDiffScore() != 0 ) {
                        Player pLeadingPlayer = MapUtil.getMaxKey(gameScore, null);
                        return new Player[] { pLeadingPlayer };
                    }
                    break;
                case ScoreOneMorePoint:
                    // At 29 all, the side scoring the 30th point, wins that game.
                    if ( getMaxScore() == 29 && getDiffScore() == 0 ) {
                        return new Player[] { Player.A, Player.B };
                    }
                    break;
            }
        }
        return super.calculateIsPossibleGameVictoryFor_Squash_Tabletennis(when, gameScore);
    }

    @Override Player[] calculatePossibleMatchVictoryFor(When when, Player[] pGameVictoryFor) {
        return super.isPossibleMatchBallFor_Squash_TableTennis(when, pGameVictoryFor);
    }

    //-------------------------------
    // score
    //-------------------------------

    @Override public String getResultShort() {
        return super.getResultShort_Squash_TableTennis();
    }

    @Override public void changeScore(Player player) {
        super.changeScore_Racketlon_Tabletennis(player, getSport());
    }

    //-------------------------------
    // JSON
    //-------------------------------

    //-------------------------------
    // conduct/appeal
    //-------------------------------

    @Override public void recordAppealAndCall(Player appealing, Call call) {
        // not applicable
    }

    @Override public void recordConduct(Player pMisbehaving, Call call, ConductType conductType) {
        // not applicable
    }
}
