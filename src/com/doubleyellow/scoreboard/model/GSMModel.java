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

import com.doubleyellow.scoreboard.util.ListWrapper;
import com.doubleyellow.util.ListUtil;
import com.doubleyellow.util.MapUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Model for basis of Game-Set-Match models: Padel and Tennis */
public class GSMModel extends Model
{
    @Override public SportType getSport() {
        return SportType.TennisPadel;
    }

    //-----------------------------------------------------
    // Listeners
    //-----------------------------------------------------

    /** Padel, tennis like scoring only */
    public interface OnSetChangeListener extends OnModelChangeListener {
        /** invoked each time a the score change implies 'SetBall' change: i.e. now having setball, or no-longer having setball */
        void OnSetBallChange(Player[] players, boolean bHasSetBall);
        /** actually ended set and preparing for new one */
        void OnSetEnded(Player winningPlayer);
        void OnXPointsPlayedInTiebreak();
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

    //------------------------
    // Match Format / Player/Time Details
    //------------------------

    /**  0-15-30-40-Game */
    private static final int NUMBER_OF_POINTS_TO_WIN_GAME     = 4;
    /** Number of points in a 'normal' tie-break (super tiebreak would be 10) */
    private static final int NUMBER_OF_POINTS_TO_WIN_TIEBREAK = 7;

    /** nr of games needed to win a set  */
    private int                                 m_iNrOfGamesToWinSet    = 6; // in sync with m_iNrOfPointsToWinGame
    /** nr of sets needed to win a match */
    private int                                 m_iNrOfSetsToWinMatch   = 2; // in sync with m_iNrOfGamesToWinMatch of super class

    @Override public boolean setNrOfPointsToWinGame(int i) {
        super.setNrOfPointsToWinGame(i);
        //Log.w(TAG, "Redirecting to setNrOfGamesToWinSet");
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

    private final String TAG = "SB." + this.getClass().getSimpleName();

    GSMModel() {
        super();
        setTiebreakFormat(TieBreakFormat.TwoClearPoints); // for games only deuce/advantage/game, TODO: but sometimes 'golden point' is used for games
        //setNrOfGamesToWinSet  (6);
        setNrOfPointsToWinGame  (6);
        //setNrOfSetsToWinMatch (2);
        setNrOfGamesToWinMatch (2);
        init();
    }

    @Override void init() {
        super.init();

        addNewSetScoreDetails(false);

        m_lSetWinner       = new ArrayList<Player>();
        m_lSetCountHistory = new ArrayList<Map<Player, Integer>>();
        m_lSetCountHistory.add(getZeroZeroMap());
    }

    //------------------------
    // Serve side
    //------------------------

    @Override public boolean setDoublesServeSequence(DoublesServeSequence dsq) {
        return false;
    }

    @Override DoublesServeSequence getDoubleServeSequence(int iGameZB) {
        return DoublesServeSequence.A1B1A2B2; // TODO: check always one and the same server in a game
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
        return false;
    }
/*
    @Override public Player getServer() {
        return super.getServer();
    }
*/
    @Override void setServerAndSide(Player player, ServeSide side, DoublesServe doublesServe) {
        int iNrOfPoints = getTotalGamePoints();
        ServeSide serveSideBasedOnPoints = ServeSide.values()[iNrOfPoints % 2];
        super.setServerAndSide(player, serveSideBasedOnPoints, doublesServe);
    }

    @Override public void changeSide(Player player) {
        if ( player.equals(getServer()) == false ) {
            setServerAndSide(player, null, null);
        }
    }

    //------------------------
    // 'Set' containers
    //------------------------

    private List<List<List<ScoreLine>>>      m_lGamesScorelineHistory_PerSet   = null;
    private List<List<Map<Player, Integer>>> m_lPlayer2GamesWon_PerSet         = null;
    private List<List<Map<Player, Integer>>> m_lPlayer2EndPointsOfGames_PerSet = null;
    private List<List<GameTiming>>           m_lGamesTiming_PerSet             = null;

    /** end scores of already ended sets [ {A=6,B=3},{A=2,B=6}, {A=7, B=6} ] . So does not hold set in progress. */
  //private List<Map<Player, Integer>>       m_endScoreOfPreviousSets = null;
    /** holds array with history of sets won like [0-0, 0-1, 1-1, 2-1] */
    private List<Map<Player, Integer>>       m_lSetCountHistory     = null;
    /** number of sets each player has won: {A=2, B=1} */
    private List<Player>                     m_lSetWinner           = null;

    public Map<Player, Integer> getSetsWon() {
        return ListUtil.getLast(m_lSetCountHistory);
    }

    /** One-based */
    public int getSetNrInProgress() {
        return ListUtil.size(m_lGamesScorelineHistory_PerSet);
    }

    private void endSet(Player pWinner, int iGamesLeader, int iGamesTrailer, boolean bNotifyListeners) {
        HashMap<Player, Integer> scores = new HashMap<Player, Integer>();
        scores.put(pWinner           , iGamesLeader);
        scores.put(pWinner.getOther(), iGamesTrailer);
        addSetScore(scores);

        addNewSetScoreDetails(false);

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

    private void addNewSetScoreDetails(boolean bFromJson) {
        if ( bFromJson ) {
            Log.d(TAG, "addNewSetScoreDetails from JSON");
        }

        if ( m_lGamesScorelineHistory_PerSet == null ) {
            // start of a new match
            m_lGamesScorelineHistory_PerSet = new ArrayList<>();
            m_lGamesScorelineHistory_PerSet.add(getGamesScoreHistory());

            m_lPlayer2GamesWon_PerSet = new ArrayList<List<Map<Player, Integer>>>();
            m_lPlayer2GamesWon_PerSet.add(getPlayer2GamesWonHistory());

            m_lPlayer2EndPointsOfGames_PerSet = new ArrayList<>();
            m_lPlayer2EndPointsOfGames_PerSet.add(getPlayer2EndPointsOfGames());

            m_lGamesTiming_PerSet = (new ListWrapper<>()).setName("GameTimesPerSet");
            m_lGamesTiming_PerSet.add(getGamesTiming());
        } else {
            List<List<ScoreLine>> lGamesScoreHistory = getGamesScoreHistory();
            if ( lGamesScoreHistory.size() == 0 ) {
                Log.d(TAG, "addNewSetScoreDetails: no action 1");
                // do not add a new, there is an empty one
            } else if ( lGamesScoreHistory.size() == 1 && ListUtil.isEmpty(lGamesScoreHistory.get(0)) ) {
                // do not add a new, there is an empty one
                Log.d(TAG, "addNewSetScoreDetails: no action 2");
            } else {
                // actually create new set and game
                super.setGamesScoreHistory(new ArrayList<List<ScoreLine>>());
                //super.addNewGameScoreDetails();
                lGamesScoreHistory = super.getGamesScoreHistory();
                //if ( m_lGamesScorelineHistory_PerSet.contains(lGamesScoreHistory) == false ) {
                m_lGamesScorelineHistory_PerSet.add(lGamesScoreHistory);
                //}

                ListWrapper<Map<Player, Integer>> l = new ListWrapper<Map<Player, Integer>>();
                l.setName("Set " + (1 + ListUtil.size(m_lPlayer2GamesWon_PerSet)));
                super.setPlayer2GamesWonHistory(l);
                m_lPlayer2GamesWon_PerSet.add(l);

                ListWrapper<Map<Player, Integer>> l2 = new ListWrapper<Map<Player, Integer>>();
                l2.setName("Set " + (1 + ListUtil.size(m_lPlayer2EndPointsOfGames_PerSet)));
                super.setPlayer2EndPointsOfGames(l2);
                m_lPlayer2EndPointsOfGames_PerSet.add(l2);

                ListWrapper<GameTiming> l3 = new ListWrapper<GameTiming>();
                l3.setName("Timing Set " + (1 + ListUtil.size(m_lGamesTiming_PerSet)));
                super.setGamesTiming(l3);
                m_lGamesTiming_PerSet.add(l3);

            }
        }
    }

    private void addSetScore(Map<Player, Integer> setScore) {
        //Log.d(TAG, "addGameScore: " + scores + " " + ListUtil.size(m_endScoreOfPreviousGames));
        Player winner = Util.getWinner(setScore);
        m_lSetWinner.add(winner);

        Map<Player, Integer> player2SetsWon = new HashMap<Player, Integer>(getSetsWon()); // clone to get current score before adding set
        MapUtil.increaseCounter(player2SetsWon, winner);
        m_lSetCountHistory.add(player2SetsWon);

        //m_endScoreOfPreviousSets.add(setScore);
    }


    //-------------------------------
    // Date/Time
    //-------------------------------

    public long getSetStart(int setNr) {
/*
        if ( setNr == 1 ) {
            return super.getMatchStart();
        }
*/
        if ( ListUtil.size(m_lGamesTiming_PerSet) > setNr - 1 ) {
            List<GameTiming> gameTimings = m_lGamesTiming_PerSet.get(setNr - 1);
            if ( ListUtil.isNotEmpty(gameTimings) ) {
                GameTiming gameTiming = gameTimings.get(0);
                return gameTiming.getStart();
            }
        }
        return getMatchStart();
    }

    public long getSetDuration(int setNr) {
        if ( setNr == 1 ) {
            return super.getDuration();
        } else {
            Long lStart = getSetStart(setNr);
            Long lEnd   = System.currentTimeMillis();
            if ( ListUtil.size(m_lGamesTiming_PerSet) > setNr - 1 ) {
                List<GameTiming> gameTimings = m_lGamesTiming_PerSet.get(setNr - 1);
                GameTiming last = ListUtil.getLast(gameTimings);
                if (last != null) {
                    lEnd = last.getEnd();
                }
            }
            return lEnd - lStart;
        }
    }

    @Override public long getMatchStart() {
        return m_lGamesTiming_PerSet.get(0).get(0).getStart();
    }

    //-------------------------------------
    // Scoring
    //-------------------------------------

    @Override protected void handout(Player scorer, boolean bScoreChangeTrue_bNewGameFalse) {
        Log.d(TAG, "No handout in GSM");
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

    @Override public void changeScore(Player player) {
        int iDelta = 1;
        Integer iNewScore = determineNewScoreForPlayer(player, iDelta,false);
        ScoreLine scoreLine = getScoreLine(player, iNewScore, m_nextServeSide);
      //determineServerAndSide_GSM();
        addScoreLine(scoreLine, true);
        setServerAndSide(getServer(), m_nextServeSide.getOther(), m_in_out);

        if ( isTieBreakGame() ) {
            // multiple times in tiebreak
            int maxScore = getMaxScore();
            int minScore = getMinScore();
            int totalGamePoints = maxScore + minScore;

            // if 6 points are played but tiebreak not yes decided
            if ( totalGamePoints % 6 == 0 ) {
                boolean possibleGameVictory = isPossibleGameVictory();
/*
                if (    (maxScore < NUMBER_OF_POINTS_TO_WIN_TIEBREAK)
                     || (maxScore - minScore < 2)
                   )
*/
                if ( possibleGameVictory == false )
                {
                    for(OnSetChangeListener l: onSetChangeListeners) {
                        l.OnXPointsPlayedInTiebreak();
                    }
                }
            }
        }

        // inform listeners
        changeScoreInformListeners(player, true, null, iDelta, getServer(), m_in_out, iNewScore);
    }

    @Override public synchronized void undoLast() {
        boolean bGoingBackASet = false;
        Map<Player, Integer> scoreOfGameInProgress = getScoreOfGameInProgress();
        if ( MapUtil.getMaxValue(scoreOfGameInProgress) == 0 ) {
            // going back a game
            Map<Player, Integer> player2GamesWon = getPlayer2GamesWon();
            if ( MapUtil.getMaxValue(player2GamesWon) == 0 ) {
                if ( getSetNrInProgress() >= 2 ) {
                    bGoingBackASet = true;

                    // go back into the previous set
                    List<List<ScoreLine>> shouldBeListWithEmptyList = ListUtil.removeLast(m_lGamesScorelineHistory_PerSet);
                    super.setGamesScoreHistory(ListUtil.getLast(m_lGamesScorelineHistory_PerSet));

                    List<Map<Player, Integer>> shouldBeListWithZeroZeroOnly = ListUtil.removeLast(m_lPlayer2GamesWon_PerSet);
                    List<Map<Player, Integer>> lPlayer2GamesWon = ListUtil.getLast(m_lPlayer2GamesWon_PerSet);
                    ListUtil.removeLast(lPlayer2GamesWon);
                    setPlayer2GamesWonHistory(lPlayer2GamesWon);

                    List<Map<Player, Integer>> shouldBeListWithZeroZeroOnly2 = ListUtil.removeLast(m_lPlayer2EndPointsOfGames_PerSet);
                    setPlayer2EndPointsOfGames(ListUtil.getLast(m_lPlayer2EndPointsOfGames_PerSet));

                    List<GameTiming> shouldBeEmptyList2 = ListUtil.removeLast(m_lGamesTiming_PerSet);
                    setGamesTiming(ListUtil.getLast(m_lGamesTiming_PerSet));

                    Map<Player, Integer> lastSetCountChange = ListUtil.removeLast(m_lSetCountHistory);
                    Player lastSetWinner = ListUtil.removeLast(m_lSetWinner);

                    //undoBackOneGame();

                    for(OnComplexChangeListener l:onComplexChangeListeners) {
                        l.OnChanged();
                    }
                }
            }
        }
        if ( bGoingBackASet ) {
            super.undoLast(); // removing last scoreline of last winning game
        } else {
            List<Map<Player, Integer>> player2EndPointsOfGames = getPlayer2EndPointsOfGames();
            Map<Player, Integer> last = ListUtil.getLast(player2EndPointsOfGames);
            if ( (last != null) && (MapUtil.getMaxValue(last) == 0) ) {
                // go back in to previous game
                super.undoLast();
                // remove last scoreline of that previous game
                // trigger a second undo to not have e.g. AD-15 on scoreboard for a game in a finished state
                super.undoLast();
            } else {
                // remove last scoreline of game in progress
                super.undoLast();
            }
        }
    }

    @Override public String getGameScores() {
        //String gameScores = super.getGameScores();

        StringBuilder sbSets = new StringBuilder();
        List<Map<Player, Integer>> setEndScores = m_lSetCountHistory;
        for(Map<Player, Integer> mSetScore: setEndScores) {
            Integer iA = MapUtil.getInt(mSetScore, Player.A, 0);
            Integer iB = MapUtil.getInt(mSetScore, Player.B, 0);
            if ( iA + iB > 0 ) {
                if ( sbSets.length() != 0 ) {
                    sbSets.append(",");
                }
                sbSets.append(iA).append("-").append(iB);
            }
        }
        return sbSets.toString();
    }

    @Override public String getResultShort() {
        return null; // TODO:
    }

    /** overridden to check at the end of a game that it is also end of a set */
    @Override public void endGame(boolean bNotifyListeners, boolean bStartNewGame) {
        // invokes startNewGame()
        super.endGame(bNotifyListeners, false); // updates m_player2GamesWon of super class

        Player server = getServer().getOther();
        DoublesServe doublesServe = m_in_out;
        if ( isDoubles() && getNrOfFinishedGames() % 2 == 1 ) {
            doublesServe = doublesServe.getOther();
        }
        setServerAndSide(server, null, doublesServe);

        // see if a new Set must be started
        Map<Player, Integer> player2GamesWon = getPlayer2GamesWon();
        Player pLeader = MapUtil.getMaxKey(player2GamesWon, Player.A);
        int iGamesLeader  = MapUtil.getInt(player2GamesWon, pLeader           , 0);
        int iGamesTrailer = MapUtil.getInt(player2GamesWon, pLeader.getOther(), 0);

        boolean bEndSet = false;
        if ( iGamesLeader == m_iNrOfGamesToWinSet ) {
            bEndSet = iGamesLeader - iGamesTrailer >= 2;
        } else if ( iGamesLeader > m_iNrOfGamesToWinSet ) {
            bEndSet = iGamesLeader - iGamesTrailer >= 1;
        }
        if ( bEndSet ) {
            endSet(pLeader, iGamesLeader, iGamesTrailer, bNotifyListeners);
        }
        startNewGame();
    }


    @Override protected void setDirty(boolean bScoreRelated) {
        super.setDirty(bScoreRelated);
        if ( bScoreRelated ) {
            Log.d(TAG, ""
            + "\n" + "m_lPlayer2EndPointsOfGames_PerSet: " + ListUtil.toNice(m_lPlayer2EndPointsOfGames_PerSet, false, 4)
            + "\n" + "m_lPlayer2GamesWon_PerSet        : " + ListUtil.toNice(m_lPlayer2GamesWon_PerSet, false, 4)
            //+ "\n" + "m_lGamesScorelineHistory_PerSet  : " + ListUtil.toNice(m_lGamesScorelineHistory_PerSet, false, 4)
            //+ "\n" + "m_lGamesTiming_PerSet            : " + ListUtil.toNice(m_lGamesTiming_PerSet, false, 4)
            );
        }
    }

    @Override public boolean hasStarted() {
        boolean bGamesInCurrentSetHasStarted = super.hasStarted();

        boolean bPreviousSetFinished = false;
        if ( bGamesInCurrentSetHasStarted == false ) {
            // check if a previous set has been played
            bPreviousSetFinished = ListUtil.size(m_lGamesScorelineHistory_PerSet) >= 2;
        }
        return bGamesInCurrentSetHasStarted || bPreviousSetFinished;
    }

    @Override public int getGameNrInProgress() {
        return super.getGameNrInProgress();
      //return ListUtil.size(m_endScoreOfPreviousGames); // TODO: required
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

    @Override Player[] calculatePossibleMatchVictoryFor(When when, Player[] pLayersSB) {
        if ( pLayersSB == null ) {
            Player[] playersGB = super.calculateIsPossibleGameVictoryFor_SQ_TT_BM_RL(when, getScoreOfGameInProgress(), _getNrOfPointsToWinGame());
                     pLayersSB = calculateIsPossibleSetVictoryFor(playersGB, when, getPlayer2GamesWon());
            if ( pLayersSB.length == 1 ) {
                int iSetsWon = MapUtil.getInt(getSetsWon(), pLayersSB[0], 0);
                if ( iSetsWon + 1 == m_iNrOfSetsToWinMatch ) {
                    return pLayersSB;
                }
            }
        }

        return getNoneOfPlayers();
    }

    @Override Player[] calculateIsPossibleGameVictoryFor(When when, Map<Player, Integer> gameScore, boolean bFromIsMatchBallFrom) {
        int iNrOfPointsToWinGame = _getNrOfPointsToWinGame();
        Player[] playersGB = super.calculateIsPossibleGameVictoryFor_SQ_TT_BM_RL(when, gameScore, iNrOfPointsToWinGame);
        Player[] pLayersSB =       calculateIsPossibleSetVictoryFor(playersGB, when, getPlayer2GamesWon());

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

    @Override protected void clearPossibleGSM() {
        super.clearPossibleGSM();
        m_possibleSetForPrev .clear();m_possibleSetForPrev .putAll(m_possibleSetFor );
        m_possibleSetFor     .clear();
    }

    @Override protected List getScorelinesRoot() {
        return m_lGamesScorelineHistory_PerSet;
    }
    @Override protected List getTimingsRoot() {
        return m_lGamesTiming_PerSet;
    }

    //-------------------------------
    // JSON
    //-------------------------------


    @Override protected JSONArray scoreHistoryToJson(List lSetScoreHistory) throws JSONException {
        JSONArray sets = new JSONArray();
        for (int s = 0; s < lSetScoreHistory.size(); s++) {
            List lGamesScoreHistory = (List) lSetScoreHistory.get(s);
            JSONArray games = super.scoreHistoryToJson(lGamesScoreHistory);
            sets.put(games);
        }
        return sets;
    }

    @Override protected JSONArray timingsToJSON(List lSetTimings) throws JSONException {
        JSONArray jaSetTimings = new JSONArray();
        for (int s = 0; s < lSetTimings.size(); s++) {
            List<GameTiming> lGamesTiming = (List<GameTiming>) lSetTimings.get(s);
            JSONArray jaGameTimings = super.timingsToJSON(lGamesTiming);
            jaSetTimings.put(jaGameTimings);
        }
        return jaSetTimings;
    }

    @Override protected ScoreLine scoreHistoryFromJSON(boolean bMatchFormatIsSet, JSONArray sets) throws JSONException {
        ScoreLine scoreLine = null;
        for ( int s=0; s < sets.length(); s++ ) {
            JSONArray games = sets.getJSONArray(s);
            if ( games.length() == 0 ) { continue; }

            clearPossibleGSM();
            if ( isPossibleGameVictory() ) {
                endGame(false, false);
            }

            // add previous game scores to history
            Map<Player, Integer> player2GamesWon = getPlayer2GamesWon();
            if ( ( s != 0 ) && ( MapUtil.isNotEmpty(player2GamesWon) ) ) {
                int max = Math.max( MapUtil.getInt(player2GamesWon, Player.A, 0)
                                  , MapUtil.getInt(player2GamesWon, Player.B, 0)
                                  );
                boolean bSetIsStarted = max != 0;
                if ( bSetIsStarted ) {
                    addSetScore(player2GamesWon);
                }
            }
            // initialize for new set
            addNewSetScoreDetails(true);
            startNewGame();

            scoreLine = super.scoreHistoryFromJSON(bMatchFormatIsSet, games);
        }

        return scoreLine;
    }

    @Override protected List<GameTiming> gameTimingFromJson(JSONArray jaSetTimings) throws JSONException {
        m_lGamesTiming_PerSet.clear();
        List<GameTiming> lGameTimings = null;
        for ( int s=0; s < jaSetTimings.length(); s++ ) {
            JSONArray jaGameTimings = jaSetTimings.getJSONArray(s);
            lGameTimings = super.gameTimingFromJson(jaGameTimings);
            m_lGamesTiming_PerSet.add(lGameTimings);
        }
        return lGameTimings;
    }

    @Override public JSONObject fromJsonString(String sJson, boolean bStopAfterEventNamesDateTimeResult) {
        JSONObject jsonObject = super.fromJsonString(sJson, bStopAfterEventNamesDateTimeResult);

        // to prevent that a score like AD-30 remains on the scoreboard...
        if ( isPossibleGameVictory() ) {
            endGame(true, true);
        }

        return jsonObject;
    }
/*
    @Override public JSONObject getJsonObject(Context context, JSONObject oSettings) throws JSONException {
        JSONObject jsonObject = super.getJsonObject(context, oSettings);
        String s = getSetScores();
        return jsonObject;
    }
*/

    @Override public void recordAppealAndCall(Player appealing, Call call) { }
    @Override public void recordConduct(Player pMisbehaving, Call call, ConductType conductType) { }
}
