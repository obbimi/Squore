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

import java.util.Map;

/**
 * Not to be confused with Squash 57, a form of squash previously called racketball.
 *
 * Losing the serve is called a side out in singles.
 * In doubles, when the first server loses the serve, it is called a handout and when the second server loses the serve, it is a side out.
 *
 * http://www.racquetballrules.us/basic-racquetball-rules/
 * http://www.teamusa.org/usa-racquetball/how-to-play/rules
 * www.teamusa.org/-/media/USA_Racquetball/Documents/Rules/USAR-Rulebook.pdf
 *
 * Each player or team is entitled to three 30-second timeouts in games to 15 and two 30-second timeouts
 in games to 11. Timeouts may not be called by either side once the service motion has begun. Calling for a timeout when
 none remain or after the service motion has begun will result in the assessment of a technical foul for delay of game. If a
 player takes more than 30 seconds for a single timeout, the referee may automatically charge any remaining timeouts, as
 needed, for any extra time taken.

 The rest period between the first two games of a match is 2 minutes. If a tiebreaker is necessary,
 the rest period between the second and third game is 5 minutes.
 *
 * A single match is made up of three games, where the first two games go until 15 points and the last game only goes until 11.
 */
public class RacquetballModel extends Model
{
    public RacquetballModel() {
        super();
        setEnglishScoring(true); // english scoring used for Racquetball
        setNrOfGamesToWinMatch(2);
        setNrOfPointsToWinGame(15);
    }

    @Override public SportType getSport() {
        return SportType.Racquetball;
    }

    /**
     * In racketlon the last (third) game typically only goes to 11 in stead of 15
     */
    @Override public int getNrOfPointsToWinGame() {
        int nrOfPointsToWinGame = super.getNrOfPointsToWinGame();
        int iGameNrNext1B = getGameNrInProgress();
        if ( (iGameNrNext1B == getNrOfGamesToWinMatch() * 2 - 1) && (nrOfPointsToWinGame == 15) ) {
            return 11;
        } else {
            return nrOfPointsToWinGame;
        }
    }

    //-------------------------------
    // serve side/sequence
    //-------------------------------

    @Override void determineServerAndSideForUndoFromPreviousScoreLine(ScoreLine lastValidWithServer, ScoreLine slRemoved) {
        // TODO:
    }

    @Override Player determineServerForNextGame(int iGameZB, int iScoreA, int iScoreB) {
        return determineServerForNextGame_SQ_BM(iScoreA, iScoreB);
    }

    @Override public boolean showChangeSidesMessageInGame(int iGameZB) {
        return false;
    }

    @Override public boolean setDoublesServeSequence(DoublesServeSequence dsq) {
        return super._setDoublesServeSequence(dsq);
    }
    @Override DoublesServeSequence getDoubleServeSequence(int iGameZB) {
        return m_doubleServeSequence;
    }

    @Override public String convertServeSideCharacter(String sRLInternational, ServeSide serveSide, String sHandoutChar) {
        return sRLInternational + sHandoutChar;
    }

    //-------------------------------
    // game/match ball
    //-------------------------------

    @Override Player[] calculatePossibleMatchVictoryFor(When when, Player[] paGameVictoryFor) {
        return super.isPossibleMatchBallFor_SQ_TT_BM(when, paGameVictoryFor);
    }

    @Override Player[] calculateIsPossibleGameVictoryFor(When when, Map<Player, Integer> gameScore, boolean bFromIsMatchBallFrom) {
        return super.calculateIsPossibleGameVictoryFor_SQ_TT_BM_RL(when, gameScore, getNrOfPointsToWinGame());
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

    @Override public void recordAppealAndCall(Player appealing, Call call) {
        super.recordAppealAndCall_SQ_RL_RB(appealing, call);
    }

    @Override public void recordConduct(Player pMisbehaving, Call call, ConductType conductType) {
        super.recordConduct_SQ_RL_RB(pMisbehaving, call, conductType);
    }
}
