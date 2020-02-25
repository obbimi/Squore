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
import com.doubleyellow.util.MapUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Model for basis of Game-Set-Match models: Padel and Tennis */
public abstract class GSMModel extends Model
{
    /**  0-15-30-40-Game */
    private static final int NUMBER_OF_POINTS_TO_WIN_GAME     = 4;
    /** Number of points in a 'normal' tie-break (super tiebreak would be 10) */
    private static final int NUMBER_OF_POINTS_TO_WIN_TIEBREAK = 7;

    private final String TAG = "SB." + this.getClass().getSimpleName();

    GSMModel() {
        super();
        init();
        startNewSet();
    }

    @Override void init() {
        m_player2SetsWon       = new HashMap<Player, Integer>();
        m_player2SetsWon.put(Player.A, 0);
        m_player2SetsWon.put(Player.B, 0);

        m_lSetsScoreHistory    = new ArrayList<List<ScoreLine>>();
        m_lSetWinner           = new ArrayList<Player>();

        m_lSetCountHistory     = new ArrayList<Map<Player, Integer>>();
        m_lSetCountHistory.add(m_player2SetsWon);

        m_endScoreOfPreviousSets         = new ArrayList<Map<Player, Integer>>();
        m_endScoreOfPreviousGames_PerSet = new ArrayList<>();

        super.init();
    }

    /** For drawing 'original' paper scoring. Contains all sets including the one in progress */
    private List<List<ScoreLine>>       m_lSetsScoreHistory    = null;
    /** scorehistory of the set in progress */
    private List<ScoreLine>             m_lSetScoreHistory     = null;

    //------------------------
    // Game scores
    //------------------------

    /** scoring of won games in the set in progress: { A=2, B=5 }. Taken from m_endScoreOfPreviousSets when 'undo-ing' into previous set */
    //private Map<Player, Integer>        m_scoreOfSetInProgress   = null; // using m_player2GamesWon
    /** end scores of already ended sets [ {A=6,B=3},{A=2,B=6}, {A=7, B=6} ] . So does not hold set in progress. */
    private List<Map<Player, Integer>>  m_endScoreOfPreviousSets = null;

    private List<List<Map<Player, Integer>>> m_endScoreOfPreviousGames_PerSet = null; // holds list of values of m_endScoreOfPreviousGames

    //------------------------
    // Set scores
    //------------------------

    /** number of sets each player has won: {A=2, B=1} */
    private Map<Player, Integer>        m_player2SetsWon       = null;
    /** holds array with history of sets won like [0-0, 0-1, 1-1, 2-1] */
    private List<Map<Player, Integer>>  m_lSetCountHistory     = null;
    private List<Player>                m_lSetWinner           = null;

    // m_lGameCountHistory to be split into sets

    @Override public void endGame(boolean bNotifyListeners) {
        // invokes startNewGame()
        super.endGame(bNotifyListeners); // updates m_player2GamesWon

        // see if a new Set must be started
        Player pLeader = MapUtil.getMaxKey(m_player2GamesWon, Player.A);
        int iGamesLeader  = MapUtil.getInt(m_player2GamesWon, pLeader           , 0);
        int iGamesTrailer = MapUtil.getInt(m_player2GamesWon, pLeader.getOther(), 0);

        boolean bEndSet = false;
        if ( iGamesLeader == m_iNrOfGamesToWinSet ) {
            bEndSet = iGamesLeader - iGamesTrailer >= 2;
        } else if ( iGamesLeader > m_iNrOfGamesToWinSet ) {
            bEndSet = iGamesLeader - iGamesTrailer >= 1;
        }
        if ( bEndSet ) {
            endSet(pLeader, iGamesLeader, iGamesTrailer, bNotifyListeners);
        }
    }

    public Map<Player, Integer> getSetsWon() {
        return m_player2SetsWon;
    }

    private void endSet(Player pWinner, int iGamesLeader, int iGamesTrailer, boolean bNotifyListeners) {
        HashMap<Player, Integer> scores = new HashMap<Player, Integer>();
        scores.put(pWinner           , iGamesLeader);
        scores.put(pWinner.getOther(), iGamesTrailer);
        addSetScore(scores);


        startNewSet();

        // TODO
        //Player serverForNextSet = determineServerForNextSet(getGameNrInProgress()-1, iScoreA, iScoreB);
        //setServerAndSide(serverForNextSet, null, null);

        if ( bNotifyListeners ) {
            for ( OnSetChangeListener l : onSetChangeListeners ) {
                l.OnSetEnded(pWinner);
            }
            if ( matchHasEnded() ) {
                Player possibleMatchVictoryFor = isPossibleMatchVictoryFor();
                for (OnMatchEndListener l : onMatchEndListeners) {
                    l.OnMatchEnded(possibleMatchVictoryFor, null);
                }
            }
        }
    }

    /** Invoked when model is created and when a set is ended */
    private void startNewSet() {
        m_player2GamesWon = new HashMap<Player, Integer>();
        m_player2GamesWon.put(Player.A, 0);
        m_player2GamesWon.put(Player.B, 0);

        m_endScoreOfPreviousGames_PerSet.add(m_endScoreOfPreviousGames);
        m_endScoreOfPreviousGames = new ArrayList<>();

        addNewSetScoreDetails();
    }

    private void addNewSetScoreDetails() {
        if ( m_lSetScoreHistory == null ) {
            m_lSetScoreHistory = new ArrayList<>();
            m_lSetsScoreHistory.add(m_lSetScoreHistory);
        } else if ( m_lSetScoreHistory.size() > 0 ) {
            m_lSetScoreHistory = new ArrayList<>();
            m_lSetsScoreHistory.add(m_lSetScoreHistory);
        } else {
            if ( m_lSetsScoreHistory.contains(m_lSetScoreHistory) == false ) {
                m_lSetsScoreHistory.add(m_lSetScoreHistory);
            }
        }
        //Log.d(TAG, "new SetScoreDetails: list of size " + m_lSetScoreHistory.size());
    }

    /** One-based */
    public int getSetNrInProgress() {
        return ListUtil.size(m_lSetsScoreHistory);
    }

    @Override public synchronized void undoLast() {
        if ( m_lSetScoreHistory.size() == 0 ) {
            if ( getSetNrInProgress() > 2 ) {

                // go back into the previous set
                ListUtil.removeLast(m_lSetsScoreHistory);
                ListUtil.removeLast(m_lSetCountHistory);
                m_lSetScoreHistory     = ListUtil.getLast(m_lSetsScoreHistory);
                m_player2SetsWon       = ListUtil.getLast(m_lSetCountHistory);
                ListUtil.removeLast(m_lSetWinner);

                m_player2GamesWon = ListUtil.removeLast(m_endScoreOfPreviousSets);

/*
                for(OnComplexChangeListener l:onComplexChangeListeners) {
                    l.OnChanged();
                }
*/
            }
        }

        super.undoLast();
    }

    @Override void startNewGame() {
        // TODO: see if a new Set must be started

        super.startNewGame();
    }

    private void addSetScore(Map<Player, Integer> setScore) {
        //Log.d(TAG, "addGameScore: " + scores + " " + ListUtil.size(m_endScoreOfPreviousGames));
        Player winner = Util.getWinner(setScore);
        m_lSetWinner.add(winner);

        m_player2SetsWon = new HashMap<Player, Integer>(m_player2SetsWon); // clone
        MapUtil.increaseCounter(m_player2SetsWon, winner);
        m_lSetCountHistory.add(m_player2SetsWon);

        m_endScoreOfPreviousSets.add(setScore);
    }

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

    @Override public int getGameNrInProgress() {
        return ListUtil.size(m_endScoreOfPreviousGames);
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

    /** Does NOT return the static NUMBER_OF_POINTS_TO_WIN_GAME or NUMBER_OF_POINTS_TO_WIN_TIEBREAK, but the nr of games to win a set */
    @Override public int getNrOfPointsToWinGame() {
        return super.getNrOfPointsToWinGame();
    }

    private int _getNrOfPointsToWinGame() {
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
        // determine server by means of looking who served in a previous game
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

    @Override Player[] calculatePossibleMatchVictoryFor(When when, Player[] pLayersSB) {
        if ( pLayersSB == null ) {
            Player[] playersGB = super.calculateIsPossibleGameVictoryFor_SQ_TT_BM_RL(when, m_scoreOfGameInProgress, _getNrOfPointsToWinGame());
                     pLayersSB = calculateIsPossibleSetVictoryFor(playersGB, when, m_player2GamesWon);
            if ( pLayersSB.length == 1 ) {
                int iSetsWon = MapUtil.getInt(m_player2SetsWon, pLayersSB[0], 0);
                if ( iSetsWon + 1 == m_iNrOfSetsToWinMatch ) {
                    return pLayersSB;
                }
            }
        }

        return getNoneOfPlayers();
    }

    @Override Player[] calculateIsPossibleGameVictoryFor(When when, Map<Player, Integer> gameScore, boolean bFromIsMatchBallFrom) {
        Player[] playersGB = super.calculateIsPossibleGameVictoryFor_SQ_TT_BM_RL(when, gameScore, _getNrOfPointsToWinGame());
        Player[] pLayersSB =       calculateIsPossibleSetVictoryFor(playersGB, when, m_player2GamesWon);

        switch ( when ) {
            case Now:
                // TODO: check
                if ( ListUtil.isNotEmpty(pLayersSB) == true ) {
                    for( OnSetChangeListener l: onSetChangeListeners ) {
                        l.OnSetEnded(pLayersSB[0]);
                    }
                }
                break;
            case ScoreOneMorePoint:
                if ( ListUtil.isNotEmpty(pLayersSB) == true ) {
                    for( OnSetChangeListener l: onSetChangeListeners ) {
                        l.OnSetBallChange(pLayersSB, true);
                    }
                }
                break;
        }
        return playersGB;
    }

    private Map<When, Player[]> m_possibleSetForPrev  = new HashMap<>();
    private Map<When, Player[]> m_possibleSetFor      = new HashMap<>();

    /** Triggers listeners */
    private void setSetVictoryFor(When when, Player[] setBallForNew) {
        // store the new value
        m_possibleSetFor.put(when, setBallForNew);

        final Player[] setBallForOld = m_possibleSetForPrev.get(when);
        if ( setBallForNew == setBallForOld ) {
            return;
        } else {
            m_possibleSetForPrev.remove(when);
        }
        boolean bSetBallFor_Unchanged = ListUtil.length(setBallForNew) == 1
                && ListUtil.length(setBallForOld) == 1
                && setBallForOld[0].equals(setBallForNew[0]);
        if ( bSetBallFor_Unchanged == false ) {
            //Log.d(TAG, String.format("SetBall %s changed from %s to %s", when, getPlayersAsList(setBallForOld), getPlayersAsList(setBallForNew)));
        }

        if ( when.equals(When.ScoreOneMorePoint) ) {
/*
            if ( ListUtil.isNotEmpty(setBallForOld) && (bSetBallFor_Unchanged == false) ) {
                // no longer game ball for...
                for( OnSetChangeListener l: onSetChangeListeners ) {
                    l.OnSetBallChange(setBallForOld, false);
                }
            }
*/

            if ( ListUtil.isNotEmpty(setBallForNew) ) {
                // now setBall for
                for( OnSetChangeListener l: onSetChangeListeners ) {
                    l.OnSetBallChange(setBallForNew, true);
                }
            }
        }
    }

    private Player[] calculateIsPossibleSetVictoryFor(Player[] playersWinningGame, When when, Map<Player, Integer> player2GamesWon){
        switch (when) {
            case Now:
                break;
            case ScoreOneMorePoint:
                Player pPossibleSetBallFor = null;
                if ( playersWinningGame.length == 1 ) {
                    // check if it is possible setball/set victory
                    pPossibleSetBallFor = playersWinningGame[0];
                    //Log.d(TAG, "p2gw : " + m_player2GamesWon);
                    //Log.d(TAG, "sogip: " + m_scoreOfGameInProgress);
                } else if ( playersWinningGame.length == 2 ) {
                    // special game format with 'golden point'.
                    pPossibleSetBallFor = MapUtil.getMaxKey(player2GamesWon, null);
                }
                if ( pPossibleSetBallFor != null ) {
                    int iGamesWon    = player2GamesWon.get(pPossibleSetBallFor);
                    int iGamesWonOpp = player2GamesWon.get(pPossibleSetBallFor.getOther());
                    if ( iGamesWon >= getNrOfGamesToWinSet() - 1 ) {
                        // typically : at least 5 games won in set to 6
                        if ( iGamesWon > iGamesWonOpp ) {
                            return new Player[] { pPossibleSetBallFor};
                        } else if ( iGamesWon == iGamesWonOpp ) {
                            if ( isTieBreakGame() ) {
                                return new Player[] { pPossibleSetBallFor};
                            }
                            // tie break set ball ?
                        } else {
                            // opponent has won more games
                        }
                    }
                }
                break;
        }
        return getNoneOfPlayers();
    }

    @Override public void recordAppealAndCall(Player appealing, Call call) { }
    @Override public void recordConduct(Player pMisbehaving, Call call, ConductType conductType) { }
}
