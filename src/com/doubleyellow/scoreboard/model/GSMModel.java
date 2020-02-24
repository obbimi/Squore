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

import android.util.Log;

import com.doubleyellow.util.ListUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Model for basis of Game-Set-Match models: Padel and Tennis */
public abstract class GSMModel extends Model
{
    /**  0-15-30-40-Game */
    public static final int NUMBER_OF_POINTS_TO_WIN_GAME = 4;
    public static final int NUMBER_OF_POINTS_TO_WIN_TIEBREAK = 7;

    final String TAG = "SB." + this.getClass().getSimpleName();

    // TODO: Tiebreak in All but last set,All Sets,All sets, supertiebreak in last set (or even AS deciding without last set)

    // TODO: Timer not between ALL games
    // TODO: no game ball message, only set ball message
    // DONE: Auto change sides every odd game
    // DONE: doubles is the default in padel (not for tennis)


    /** nr of games needed to win a set  */
    private int                                 m_iNrOfGamesToWinSet    = 6; // in sync with m_iNrOfPointsToWinGame
    /** nr of sets needed to win a match */
    private int                                 m_iNrOfSetsToWinMatch   = 2; // in sync with m_iNrOfGamesToWinMatch of super class

    /** Padel, tennis like scoring only */
    public interface OnSetChangeListener extends OnModelChangeListener {
        /** invoked each time a the score change implies 'SetBall' change: i.e. now having setball, or no-longer having setball */
        void OnSetBallChange(Player[] players, boolean bHasSetBall);
        /** actually ended set and preparing for new one */
        void OnSetEnded(Player winningPlayer);
    }
    transient private List<OnSetChangeListener> onSetChangeListeners = new ArrayList<OnSetChangeListener>();

    @Override public void registerListener(OnModelChangeListener changedListener) {
        super.registerListener(changedListener);

        if ( changedListener instanceof OnSetChangeListener ) {
            onSetChangeListeners.add((OnSetChangeListener) changedListener);
        }
    }

    @Override public void triggerListeners() {
        super.triggerListeners();

        Player[] paSetBallFor = null; // isPossibleGameAndSetBallFor();
        if ( ListUtil.length(paSetBallFor) != 0 ) {
            for(OnSetChangeListener l: onSetChangeListeners) {
                l.OnSetBallChange(paSetBallFor, true);
            }
        }
    }

    @Override public boolean setNrOfPointsToWinGame(int i) {
        super.setNrOfPointsToWinGame(i);
        Log.w(TAG, "Redirecting to setNrOfGamesToWinSet");
        return setNrOfGamesToWinSet(i);
    }

    private boolean setNrOfGamesToWinSet(int i) {
        if ( (i != m_iNrOfGamesToWinSet) && (i != UNDEFINED_VALUE) ) {
            m_iNrOfGamesToWinSet = i;
          //setDirty(true);
            return true;
        }
        return false;
    }

    private boolean setNrOfSetsToWinMatch(int i) {
        if ( i != m_iNrOfSetsToWinMatch ) {
            m_iNrOfSetsToWinMatch = i;
          //setDirty(true);
            return true;
        }
        return false;
    }

    @Override public boolean setNrOfGamesToWinMatch(int i) {
        super.setNrOfGamesToWinMatch(i);
        Log.w(TAG, "setNrOfGamesToWinMatch::Redirecting m_iNrOfSetsToWinMatch");
        return setNrOfSetsToWinMatch(i);
    }
    @Override public int getNrOfGamesToWinMatch() {
        Log.w(TAG, "getNrOfGamesToWinMatch::Redirecting m_iNrOfSetsToWinMatch");
        return m_iNrOfSetsToWinMatch;
    }

    private boolean isTieBreakGame() {
        int iGameInProgress = getGameNrInProgress();
        if ( iGameInProgress % 2 == 0 ) {
            return false;
        }
        if ( iGameInProgress >= (m_iNrOfGamesToWinSet * 2 + 1) ) {
            return true;
        } else {
            return false;
        }
    }

    public int getNrOfGamesToWinSet() {
        return m_iNrOfGamesToWinSet;
    }
    /** Does NOT return the static NUMBER_OF_POINTS_TO_WIN_GAME or 7, but the nr of games to win a set */
    @Override public int getNrOfPointsToWinGame() {
        return super.getNrOfPointsToWinGame();
    }

    private int _getNrOfPointToWinGame() {
        if ( isTieBreakGame() ) {
            return NUMBER_OF_POINTS_TO_WIN_TIEBREAK; // TODO: 10 for super tiebreak
        } else {
            return NUMBER_OF_POINTS_TO_WIN_GAME; // 1=15, 2=30, 3=40, 4=Game
        }
    }
    private static List<String> lTranslatedScores = Arrays.asList("0", "15", "30", "40", "AD");
    public String translateScore(Player player, int iScore) {
        if ( isTieBreakGame() ) {
            return String.valueOf(iScore);
        } else {
            int iScoreOther = getScore(player.getOther());
            if ( iScore < NUMBER_OF_POINTS_TO_WIN_GAME ) {
                return lTranslatedScores.get(iScore);
            } else {
                // compare with opponent
                if ( iScoreOther < iScore ) {
                    return lTranslatedScores.get(NUMBER_OF_POINTS_TO_WIN_GAME);
                }
                // scores are equal or opponent has AD
                return lTranslatedScores.get(NUMBER_OF_POINTS_TO_WIN_GAME - 1);
            }
        }
    }

    @Override public boolean setDoublesServeSequence(DoublesServeSequence dsq) {
        return false;
    }

    @Override DoublesServeSequence getDoubleServeSequence(int iGameZB) {
        return DoublesServeSequence.NA;
    }

    @Override void determineServerAndSideForUndoFromPreviousScoreLine(ScoreLine lastValidWithServer, ScoreLine slRemoved) { }

    @Override Player determineServerForNextGame(int iGameZB, int iScoreA, int iScoreB) {
        return determineServerForNextGame_TT_RL(iGameZB, true);
    }

    @Override public Object convertServeSideCharacter(String sRLInternational, ServeSide serveSide, String sHandoutChar) {
        return sRLInternational;
    }

    @Override public boolean showChangeSidesMessageInGame(int iGameZB) {
        return false; // TODO: multiple times in tiebreak
    }

    @Override public String getResultShort() {
        return null;
    }

    @Override public void changeScore(Player player) {
        int iDelta = 1;
        Integer iNewScore = determineNewScoreForPlayer(player, iDelta,false);
        ScoreLine scoreLine = getScoreLine(player, iNewScore, m_nextServeSide);
        determineServerAndSide_GSM();
        addScoreLine(scoreLine, true);

        // inform listeners
        changeScoreInformListeners(player, true, null, iDelta, getServer(), m_in_out, iNewScore);
    }
    private void determineServerAndSide_GSM() {
        int iNrOfGames = getGameNrInProgress(); // TODO: spanning all sets

        Player server = getServer();
        List<ScoreLine> scoreLines = getGameScoreHistory().get(0);
        if ( ListUtil.isNotEmpty(scoreLines) ) {
            ScoreLine scoreLine = scoreLines.get(0);
            server = scoreLine.getServingPlayer();
        }
        for( int i = 0; i < iNrOfGames - 1; i++ ) {
            server = server.getOther();
        }
        int iNrOfPoints = getTotalGamePoints();
        setServerAndSide(server, ServeSide.values()[iNrOfPoints%2], null);
    }

    @Override Player[] calculatePossibleMatchVictoryFor(When when, Player[] paGameVictoryFor) {
        return new Player[] {}; // TODO:
    }

    @Override Player[] calculateIsPossibleGameVictoryFor(When when, Map<Player, Integer> gameScore, boolean bFromIsMatchBallFrom) {
        Player[] players = super.calculateIsPossibleGameVictoryFor_SQ_TT_BM_RL(when, gameScore, _getNrOfPointToWinGame());
        if ( players.length == 1 ) {
            // check if it is possible setball/set victory
            Player pGameBallFor = players[0];
            //Log.d(TAG, "p2gw : " + m_player2GamesWon);
            //Log.d(TAG, "sogip: " + m_scoreOfGameInProgress);
            int iGamesWon    = m_player2GamesWon.get(pGameBallFor);
            int iGamesWonOpp = m_player2GamesWon.get(pGameBallFor.getOther());
            if ( iGamesWon >= getNrOfGamesToWinSet() - 1 ) {
                // typically : at least 5 games won in set to 6
                boolean bIsSetBall = false;
                if ( iGamesWon > iGamesWonOpp ) {
                    bIsSetBall = true;
                } else if ( iGamesWon == iGamesWonOpp ) {
                    if ( isTieBreakGame() ) {
                        bIsSetBall = true;
                    }
                    // tie break set ball ?
                } else {
                    // opponent has won more games
                }
                for(OnSetChangeListener l: onSetChangeListeners) {
                    l.OnSetBallChange(players, bIsSetBall);
                }
            }
        } else if ( players.length == 2 ) {
            // TODO: special tie-break format
        }
        return players;
    }

    @Override public void recordAppealAndCall(Player appealing, Call call) { }
    @Override public void recordConduct(Player pMisbehaving, Call call, ConductType conductType) { }
}
