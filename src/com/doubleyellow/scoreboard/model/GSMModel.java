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

import com.doubleyellow.scoreboard.prefs.PreferenceKeys;
import com.doubleyellow.scoreboard.util.ListWrapper;
import com.doubleyellow.util.ListUtil;
import com.doubleyellow.util.MapUtil;
import com.doubleyellow.util.StringUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Model for basis of Game-Set-Match (GSM) models: Padel and Tennis */
public class GSMModel extends Model
{
    public final static int FS_NR_GAMES_AS_OTHER_SETS   = -1;
    public final static int FS_UNLIMITED_NR_OF_GAMES    = -2;
    public final static int FS_LIMITED_NR_OF_GAMES_12   = -12;
    public final static int NOT_APPLICABLE              = -9;

    private enum JsonKey {
        lastBallChangeOccurredAtStartOfGame,
    }

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
        void OnXPointsPlayedInTiebreak(int iTotalPoints);
    }
    transient private List<OnSetChangeListener> onSetChangeListeners = new ArrayList<OnSetChangeListener>();

    @Override public void registerListener(OnModelChangeListener changedListener) {
        super.registerListener(changedListener);

        if ( changedListener instanceof OnSetChangeListener ) {
            onSetChangeListeners.add((OnSetChangeListener) changedListener);
        }
    }

    @Override public boolean triggerListeners() {
        if ( super.triggerListeners() ) {
            Player[] paSetBallFor = null; // isPossibleGameAndSetBallFor();
            if ( ListUtil.length(paSetBallFor) != 0 ) {
                for(OnSetChangeListener l: onSetChangeListeners) {
                    l.OnSetBallChange(paSetBallFor, true);
                }
            }
            return true;
        }
        return false;
    }

    //------------------------
    // Start tiebreak one game early (E.g. in Youth format in Belgium)
    //------------------------
    boolean m_bStartTiebreakOneGameEarly = false;
    public void setStartTiebreakOneGameEarly(boolean b) {
        m_bStartTiebreakOneGameEarly = b;
    }
    public boolean getStartTiebreakOneGameEarly() {
        return m_bStartTiebreakOneGameEarly;
    }

    public boolean isStartOfTieBreak_nonGSM() {
        return false;
    }

    //------------------------
    // Final set finish
    //------------------------
    private FinalSetFinish m_finalSetFinish = FinalSetFinish.TieBreakTo7;
    public void setFinalSetFinish(FinalSetFinish v) {
        m_finalSetFinish = v;
    }
    public FinalSetFinish getFinalSetFinish() {
        return m_finalSetFinish;
    }

    //------------------------
    // New Balls Announcement after
    //------------------------
    private NewBalls m_newBalls = NewBalls.AfterFirst9ThenEach11;
    public void setNewBalls(NewBalls v) {
        m_newBalls = v;
    }
    public NewBalls getNewBalls() {
        return m_newBalls;
    }

    //------------------------
    // Golden point
    //------------------------
    public GoldenPointFormat getGoldenPointFormat() {
        return m_goldenPointFormat;
    }
    private GoldenPointFormat m_goldenPointFormat = GoldenPointFormat.None;
    public void setGoldenPointFormat(GoldenPointFormat v) {
        m_goldenPointFormat = v;
    }

    //------------------------
    // Match Format / Player/Time Details
    //------------------------

    /**  0-15-30-40-Game */
    public static final int NUMBER_OF_POINTS_TO_WIN_GAME     = 4; // 1=15, 2=30, 3=40, 4=Game
    /** Number of points in a 'normal' tie-break (tiebreak e.g. in final set could be 10 = 'super tiebreak') */
    private static final int NUMBER_OF_POINTS_TO_WIN_TIEBREAK = 7;
    public  static final int NUMBER_OF_GAMES_TO_WIN_SET_DEFAULT = 6;

    /** nr of games needed to win a set  */
    private int                                 m_iNrOfGamesToWinSet    = NUMBER_OF_GAMES_TO_WIN_SET_DEFAULT; // in sync with m_iNrOfPointsToWinGame
    /** nr of sets needed to win a match */
    private int                                 m_iNrOfSetsToWinMatch   = 2; // in sync with m_iNrOfGamesToWinMatch of super class

    /** TODO: playAllGames does not make sense: implement playAllSets */
    @Override public void setPlayAllGames(boolean b) {
        super.setPlayAllGames(false);
    }
    @Override public boolean playAllGames() {
        super.playAllGames();
        return false;
    }

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
        //Log.w(TAG, "setNrOfGamesToWinMatch::Redirecting m_iNrOfSetsToWinMatch");
        return setNrOfSetsToWinMatch(i);
    }
    @Override public int getNrOfGamesToWinMatch() {
        //Log.w(TAG, "getNrOfGamesToWinMatch::Redirecting m_iNrOfSetsToWinMatch");
        return m_iNrOfSetsToWinMatch;
    }

    public int getNrOfGamesToWinSet(int iSet1B) {
        if( iSet1B == m_iNrOfSetsToWinMatch * 2 - 1 ) {
            return getNrOfGamesToWinSet();
        }
        return m_iNrOfGamesToWinSet;
    }
    public int getNrOfGamesToWinSet() {
        // TODO
        if ( isFinalSet() ) {
            int iNrOfGamesToWinSet = m_finalSetFinish.numberOfGamesToWinSet();
            switch (iNrOfGamesToWinSet) {
                case FS_NR_GAMES_AS_OTHER_SETS:
                    return m_iNrOfGamesToWinSet;
                case FS_UNLIMITED_NR_OF_GAMES:
                    return m_iNrOfGamesToWinSet;
                case FS_LIMITED_NR_OF_GAMES_12:
                    Map<Player, Integer> player2GamesWon = getPlayer2GamesWon();
                    int minValue = MapUtil.getMinValue(player2GamesWon);
                    int iReturn = Math.max(Math.min(minValue + 2, Math.abs(iNrOfGamesToWinSet)), m_iNrOfGamesToWinSet);
                    if ( iReturn != NUMBER_OF_GAMES_TO_WIN_SET_DEFAULT ) {
                        Log.w(TAG, "Deviating... normally only in final set: " + iReturn);
                    }
                    return iReturn; // 2 more than the lowest number of games won up till now
                default:
                    return iNrOfGamesToWinSet;
            }
        }
        return m_iNrOfGamesToWinSet;
    }

    /** Does NOT return the static NUMBER_OF_POINTS_TO_WIN_GAME or NUMBER_OF_POINTS_TO_WIN_TIEBREAK, but the nr of games to win a set */
    @Override public int getNrOfPointsToWinGame() {
        return super.getNrOfPointsToWinGame();
    }

    @Override public Map<Player, Integer> getTotalNumberOfPointsScored() {
        Map<Player, Integer> pointsWon = new HashMap<Player, Integer>();
        if ( ListUtil.isEmpty(m_lPlayer2EndPointsOfGames_PerSet)) { return pointsWon; }

        for( List<Map<Player, Integer>> lPlayer2EndPointsOfGames: m_lPlayer2EndPointsOfGames_PerSet) {
            if ( ListUtil.isEmpty(lPlayer2EndPointsOfGames)) { continue; }
            for( Map<Player, Integer> mPlayer2PointsInGame : lPlayer2EndPointsOfGames) {
                if ( MapUtil.isEmpty(mPlayer2PointsInGame)) { continue; }
                for (Player p : Player.values() ) {
                    MapUtil.increaseCounter(pointsWon, p, mPlayer2PointsInGame.get(p));
                }
            }
        }
        return pointsWon;
    }

    private int _getNrOfPointsToWinGame() {
        if ( isTieBreakGame() ) {
            boolean bIsFinalSet = isFinalSet();
            if ( bIsFinalSet ) {
                return m_finalSetFinish.numberOfPointsToWinTiebreak();
/*
                switch ( m_finalSetFinish ) {
                    case TieBreakTo7:
                    case GamesTo12ThenTieBreakTo7:
                    case NoGames_TieBreakTo7:
                        return 7;
                    case TieBreakTo10:
                    case GamesTo12ThenTieBreakTo10:
                    case NoGames_TieBreakTo10:
                        return 10;
                    default:
                        return NUMBER_OF_POINTS_TO_WIN_TIEBREAK;
                }
*/
            } else {
                return NUMBER_OF_POINTS_TO_WIN_TIEBREAK;
            }
        } else {
            return NUMBER_OF_POINTS_TO_WIN_GAME;
        }
    }

    public boolean isFinalSet() {
        if ( matchHasEnded() ) {
            Map<Player, Integer> setsWon = getSetsWon();
            if ( setsWon.get(Player.A) + setsWon.get(Player.B) < m_iNrOfSetsToWinMatch * 2 - 1 ) {
                return false;
            }
        }
        int setNrInProgress = getSetNrInProgress();
        return setNrInProgress == m_iNrOfSetsToWinMatch * 2 - 1;
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

    @Override public void clear() {
        super.clear();

        if ( m_lGamesScorelineHistory_PerSet   != null) { m_lGamesScorelineHistory_PerSet  .clear(); }
        if ( m_lPlayer2GamesWon_PerSet         != null) { m_lPlayer2GamesWon_PerSet        .clear(); }
        if ( m_lPlayer2EndPointsOfGames_PerSet != null) { m_lPlayer2EndPointsOfGames_PerSet.clear(); }
        if ( m_lGamesTiming_PerSet             != null) { m_lGamesTiming_PerSet            .clear(); }
        if ( m_lSetCountHistory                != null) { m_lSetCountHistory               .clear(); }
        if ( m_lSetWinner                      != null) { m_lSetWinner                     .clear(); }
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

    @Override protected void undoBackOneGame() {
        super.undoBackOneGame();
        if ( isDoubles() && getNrOfFinishedGames() % 2 == 0 ) {
            setNextDoubleServe(m_in_out         .getOther());
        }
    }

    @Override void determineServerAndSideForUndoFromPreviousScoreLine(ScoreLine lastValidWithServer, ScoreLine slRemoved) {
        // TODO: improve
        ServeSide serveSideRemoved = slRemoved.getServeSide();
        ServeSide ssNew = null;
        if (serveSideRemoved != null) {
            ssNew = serveSideRemoved.getOther();
        }
        Player pNewServer = null;
        if ( lastValidWithServer != null ) {
            pNewServer = lastValidWithServer.getServingPlayer();
        }
        setServerAndSide(pNewServer, ssNew, null, true);
    }

    @Override Player determineServerForNextGame(int iGameZB, int iScoreA, int iScoreB) {
        // TODO: never invoked for current implementation of GSMModel
        return determineServerForNextGame_TT_RL(iGameZB, true);
    }

    @Override public Object convertServeSideCharacter(String sRLInternational, ServeSide serveSide, String sHandoutChar) {
        return sRLInternational;
    }
    /** Typically for halfway game in Tabletennis, so NO not in tennis/padel */
    @Override public boolean showChangeSidesMessageInGame(int iGameZB) {
        return false;
    }
/*
    @Override public Player getServer() {
        return super.getServer();
    }
*/
    @Override void setServerAndSide(Player server, ServeSide side, DoublesServe doublesServe, boolean bForUndo) {
        int iNrOfPoints = getTotalGamePoints();
        ServeSide serveSideBasedOnPoints = ServeSide.values()[iNrOfPoints % 2];
        if ( isTieBreakGame() ) {
            // swap server every two points (when scoring is odd)
            if ( iNrOfPoints % 2 == 1 ) {
                if ( server == null ) {
                    // prevent NPE
                    server = Player.A;
                } else {
                    server = server.getOther();
                }
                if ( isDoubles() && (doublesServe != null) ) {
                    // swap player within team
                    if ( iNrOfPoints % 4 == 1 ) {
                        doublesServe = doublesServe.getOther();
                    }
                }
            }
        }
        super.setServerAndSide(server, serveSideBasedOnPoints, doublesServe, bForUndo);
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

    /* end scores of already ended sets [ {A=6,B=3},{A=2,B=6}, {A=7, B=6} ] . So does not hold set in progress. */
  //private List<Map<Player, Integer>>       m_endScoreOfPreviousSets = null;
    /** holds array with history of sets won like [0-0, 0-1, 1-1, 2-1] */
    private List<Map<Player, Integer>>       m_lSetCountHistory     = null;
    /** number of sets each player has won: {A=2, B=1} */
    private List<Player>                     m_lSetWinner           = null;

    public Map<Player, Integer> getSetsWon() {
        if ( ListUtil.isEmpty(m_lSetCountHistory) ) {
            if ( m_lSetCountHistory == null ) {
                m_lSetCountHistory = new ArrayList<Map<Player, Integer>>();
            }
            m_lSetCountHistory.add(getZeroZeroMap());
        }
        Map<Player, Integer> last = ListUtil.getLast(m_lSetCountHistory);
        return last;
    }
    public List<Map<Player, Integer>> getSetCountHistory() {
        return m_lSetCountHistory;
    }
    /** Typically [ { A:2, B:6 }, { A:7 B:6 } , { A: 6, B:4} ] */
    public List<Map<Player, Integer>> getGamesWonPerSet() {
        return getGamesWonPerSet(false);
    }
    public List<Map<Player, Integer>> getGamesWonPerSet(boolean bCloneMaps) {
        List<Map<Player, Integer>> lReturn = new ArrayList<>();

        Map<Player, Integer> mPlayer2GamesWonLast = null;
        if ( ListUtil.isNotEmpty(m_lPlayer2GamesWon_PerSet) ) {
            for(List<Map<Player, Integer>> player2GamesWonInSet :m_lPlayer2GamesWon_PerSet) {
                mPlayer2GamesWonLast = ListUtil.getLast(player2GamesWonInSet);
                if ( mPlayer2GamesWonLast != null ) {
                    if ( bCloneMaps ) {
                        lReturn.add(new HashMap<>(mPlayer2GamesWonLast));
                    } else {
                        lReturn.add(mPlayer2GamesWonLast);
                    }
                }
            }
        }
        if ( ListUtil.isEmpty(lReturn) ) {
            lReturn.add(getZeroZeroMap());
        }
        //super.clearPossibleGSM();
        if ( matchHasEnded() ) {
            if ( MapUtil.getMaxValue(ListUtil.getLast(lReturn)) == 0 ) {
                Log.w(TAG, "removing 0-0 for a set that is not to be played");
                ListUtil.removeLast(lReturn);
            }
        }
        return lReturn;
    }

    public int totalNrOfGamesPlayed() {
        int iReturn = 0;
        if ( ListUtil.isNotEmpty(m_lPlayer2GamesWon_PerSet) ) {
            for(List<Map<Player, Integer>> player2GamesWonInSet :m_lPlayer2GamesWon_PerSet) {
                Map<Player, Integer> mPlayer2GamesWonLast = ListUtil.getLast(player2GamesWonInSet);
                if ( mPlayer2GamesWonLast != null ) {
                    iReturn += MapUtil.getInt(mPlayer2GamesWonLast, Player.A, 0);
                    iReturn += MapUtil.getInt(mPlayer2GamesWonLast, Player.B, 0);
                }
            }
        }
        return iReturn;
    }

    private int m_iLastBallChangeOccurredAtStartOfGame = 0;
    private void setLastBallChangeOccurredAtStartOfGame(int i) {
        m_iLastBallChangeOccurredAtStartOfGame = i;
    }
    public int newBallsInXgames() {
        if ( gameHasStarted() || matchHasEnded() ) {
            return GSMModel.NOT_APPLICABLE;
        }
        int iInXGames = -1;

        NewBalls newBalls = getNewBalls();
        final int iEachX = newBalls.afterEachXgames();
        if ( iEachX != GSMModel.NOT_APPLICABLE ) {
            final int iGamesPlayed = totalNrOfGamesPlayed();
            final int iAfterFirstX = newBalls.afterFirstXgames();

            if ( m_iLastBallChangeOccurredAtStartOfGame == 0 ) {
                iInXGames = m_iLastBallChangeOccurredAtStartOfGame + iAfterFirstX - iGamesPlayed;
            } else if ( m_iLastBallChangeOccurredAtStartOfGame >= iGamesPlayed ) {
                // special case AFTER a tiebreak where at start of tiebreak it was time to change balls
                iInXGames = m_iLastBallChangeOccurredAtStartOfGame - iGamesPlayed;
            } else {
                iInXGames = m_iLastBallChangeOccurredAtStartOfGame + iEachX       - iGamesPlayed;
            }
            if ( iInXGames == 0 ) {
                if ( isTieBreakGame() ) {
                    // not before tiebreak itself, but after tiebreak+first game in next set
                    iInXGames = 2;
                    m_iLastBallChangeOccurredAtStartOfGame = iGamesPlayed + iInXGames;
                } else {
                    m_iLastBallChangeOccurredAtStartOfGame = iGamesPlayed;
                }
            }
            if ( false && getSetNrInProgress() == m_iNrOfSetsToWinMatch * 2 - 1 ) {
                // todo: if last ball change can only be at in FINAL set just before the tiebreak, there will never be a ball change
                if ( EnumSet.of(FinalSetFinish.TieBreakTo7, FinalSetFinish.TieBreakTo10).contains(getFinalSetFinish()) ) {
                    if ( getNrOfGamesToWinSet() == NUMBER_OF_GAMES_TO_WIN_SET_DEFAULT ) {
                    }
                } else if ( EnumSet.of(FinalSetFinish.GamesTo12ThenTieBreakTo7, FinalSetFinish.GamesTo12ThenTieBreakTo10).contains(getFinalSetFinish()) ) {

                }
            }
            return iInXGames;
        } else {
            int iAtStartOfSetX = newBalls.atStartOfSetX();
            int iSetInProgress = getSetNrInProgress();
            if ( iSetInProgress == iAtStartOfSetX ) {
                iInXGames = 0;
            }
            return iInXGames;
        }
    }

    /** One-based */
    public int getSetNrInProgress() {
        return Math.max(ListUtil.size(m_lGamesScorelineHistory_PerSet), 1);
    }
    public List<List<ScoreLine>> getGameScoreLinesOfSet(int iSetNrZB) {
        if ( iSetNrZB > ListUtil.size(m_lGamesScorelineHistory_PerSet) ) { return null; }
        return m_lGamesScorelineHistory_PerSet.get(iSetNrZB);
    }

    private void endSet(Player pWinner, int iGamesLeader, int iGamesTrailer, boolean bNotifyListeners) {
        HashMap<Player, Integer> scores = new HashMap<Player, Integer>();
        scores.put(pWinner           , iGamesLeader);
        scores.put(pWinner.getOther(), iGamesTrailer);
        addSetScore(scores);

        addNewSetScoreDetails(false);

        // determineServerForNextSet
        int setNrInProgressZB = getSetNrInProgress() - 1;
        if ( setNrInProgressZB > 0 ) {
            List<Map<Player, Integer>> maps = m_lPlayer2GamesWon_PerSet.get(setNrInProgressZB - 1);
            if ( ListUtil.size(maps) > getNrOfGamesToWinSet() * 2 + 1 ) { // for a tiebreak 7-6 maps contains 14 entries, don't forget 0-0 is the additional entry
                // after a tie-break, determine server based on first server in previous set
                List<List<ScoreLine>> gameScoreLinesOfPreviousSet = getGameScoreLinesOfSet(setNrInProgressZB - 1);
                if ( ListUtil.isNotEmpty(gameScoreLinesOfPreviousSet)) {
                    List<ScoreLine> scoreLinesOfFirstGameOfPreviousSet = gameScoreLinesOfPreviousSet.get(0);
                    if ( ListUtil.isNotEmpty(scoreLinesOfFirstGameOfPreviousSet) ) {
                        ScoreLine scoreLine = scoreLinesOfFirstGameOfPreviousSet.get(0);
                        Player servingPlayerAtStartOfPreviousSet = scoreLine.getServingPlayer();
                        if ( servingPlayerAtStartOfPreviousSet != null ) {
                            Player server = servingPlayerAtStartOfPreviousSet.getOther();
                            setServerAndSide(server, null, null);
                        }
                    }
                }
            }
        }

        clearPossibleGSM();
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

            m_lGamesTiming_PerSet = (new ListWrapper<>(false)).setName("GameTimesPerSet");
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

                ListWrapper<Map<Player, Integer>> l = new ListWrapper<Map<Player, Integer>>(false);
                l.setName("P2GW Set " + (1 + ListUtil.size(m_lPlayer2GamesWon_PerSet)));
                super.setPlayer2GamesWonHistory(l);
                m_lPlayer2GamesWon_PerSet.add(l);

                ListWrapper<Map<Player, Integer>> l2 = new ListWrapper<Map<Player, Integer>>(false);
                l2.setName("P2EPOG Set " + (1 + ListUtil.size(m_lPlayer2EndPointsOfGames_PerSet)));
                super.setPlayer2EndPointsOfGames(l2);
                m_lPlayer2EndPointsOfGames_PerSet.add(l2);

                ListWrapper<GameTiming> l3 = new ListWrapper<GameTiming>(false);
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

        // clone to get current score before adding set
        Map<Player, Integer> player2SetsWon = new HashMap<Player, Integer>();
        Map<Player, Integer> prevSetsWon = getSetsWon();
        player2SetsWon.putAll(prevSetsWon);

        MapUtil.increaseCounter(player2SetsWon, winner);
        m_lSetCountHistory.add(player2SetsWon);

        //m_endScoreOfPreviousSets.add(setScore);
    }


    //-------------------------------
    // Date/Time
    //-------------------------------

    public long getSetStart(int setNr1B) {
/*
        if ( setNr1B == 1 ) {
            return super.getMatchStart();
        }
*/
        if ( ListUtil.size(m_lGamesTiming_PerSet) > setNr1B - 1 ) {
            List<GameTiming> gameTimings = m_lGamesTiming_PerSet.get(setNr1B - 1);
            if ( ListUtil.isNotEmpty(gameTimings) ) {
                GameTiming gameTiming = gameTimings.get(0);
                return gameTiming.getStart();
            }
        }
        return getMatchStart();
    }

    /** Returns set duration per set 'wrapped' in GameTiming object */
    @Override public List<GameTiming> getTimes() {
        List<GameTiming> lSetTimings = new ArrayList<>();
        int iSetNrZB = 0;
        for( List<GameTiming> gameTimingListOfSet : m_lGamesTiming_PerSet ) {
            if ( ListUtil.isEmpty(gameTimingListOfSet) ) { continue; }
            GameTiming gtFirst = gameTimingListOfSet.get(0);
            GameTiming gtLast  = ListUtil.getLast(gameTimingListOfSet);
            GameTiming setTiming = new GameTiming(iSetNrZB, gtFirst.getStart(), gtLast.getEnd());
            lSetTimings.add(setTiming);
            iSetNrZB++;
        }
        return lSetTimings;
    }
    public List<GameTiming> getGameTimes() {
        return super.getTimes();
    }
/*
    public List<GameTiming> getSetTimes() {
        return this.getTimes();
    }
*/

    @Override public int getDurationInMinutes() {
        if ( ListUtil.isEmpty(m_lGamesTiming_PerSet) ) { return 0; }

        List<GameTiming> firstSet = m_lGamesTiming_PerSet.get(0);
        if ( ListUtil.isEmpty(firstSet) ) { return 0; }
        GameTiming gtFirst = firstSet.get(0);

        List<GameTiming> lastSet = null;
        int iSetNrZB = ListUtil.size(m_lGamesTiming_PerSet) -1;
        while( ListUtil.isEmpty(lastSet) && iSetNrZB >=0 ) {
            lastSet = m_lGamesTiming_PerSet.get(iSetNrZB);
            if ( ListUtil.size(lastSet) == 1 ) {
                // do not use if single gametiming with same start as end time
                if ( lastSet.get(0).getDuration() == 0 ) {
                    lastSet = null;
                }
            }
            iSetNrZB--;
        }
        GameTiming gtLast = ListUtil.getLast(lastSet);
        long start = (gtFirst != null) ? gtFirst.getStart() : 0L;
        long end   = (gtLast  != null) ? gtLast .getEnd  () : 0L;
        GameTiming gtTmp = new GameTiming(0, start, end);
        return gtTmp.getDurationMM();
    }

    @Override public long getDuration() {
        try {
            long lStart = m_lGamesTiming_PerSet.get(0).get(0).getStart();
            long lEnd   = ListUtil.getLast(ListUtil.getLast(m_lGamesTiming_PerSet)).getEnd();
            return lEnd - lStart;
        } catch (Exception e) {
            return 0;
        }
    }

    public long getSetDuration(int setNr1B) {
        if ( setNr1B == 1 ) {
            return super.getDuration();
        } else {
            long lDuration = 0; // TODO: use this?
            if ( setNr1B > 1 && setNr1B - 1 < ListUtil.size(m_lGamesTiming_PerSet) ) {
                List<GameTiming> lGameTimingsForSet = m_lGamesTiming_PerSet.get(setNr1B - 1);
                lDuration = getSetDuration(lGameTimingsForSet);
                //return lDuration1;
            }
            Long lStart = getSetStart(setNr1B);
            Long lEnd   = System.currentTimeMillis();
            if ( setNr1B > 1 && setNr1B - 1 < ListUtil.size(m_lGamesTiming_PerSet) ) {
                List<GameTiming> lGameTimingsForSet = m_lGamesTiming_PerSet.get(setNr1B - 1);

                List<GameTiming> gameTimings = lGameTimingsForSet;
                GameTiming last = ListUtil.getLast(gameTimings);
                if ( last != null ) {
                    lEnd = last.getEnd();
                }
            }
            lDuration = lEnd - lStart;

            return lDuration;
        }
    }

    @Override public long getMatchStart() {
        if ( ListUtil.isEmpty(m_lGamesTiming_PerSet) ) {
            Log.w(TAG, "No game timing per set");
            return super.getMatchStart();
        }
        List<GameTiming> gameTimings = m_lGamesTiming_PerSet.get(0);
        if ( ListUtil.isNotEmpty(gameTimings) ) {
            GameTiming gameTiming = gameTimings.get(0);
            return gameTiming.getStart();
        } else {
            return super.getMatchStart();
        }
    }

    //-------------------------------------
    // Scoring
    //-------------------------------------

    @Override protected void handout(Player scorer, boolean bScoreChangeTrue_bNewGameFalse) {
        //Log.d(TAG, "No handout in GSM");
    }

    private static List<String> lTranslatedScores = Arrays.asList("0", "15", "30", "40", "AD");
    /** internally score is still 0,1,2,3. For display/speech, we need it as 0,15,30 */
    public String translateScore(Player player, int iScore) {
        if ( isTieBreakGame() ) {
            return String.valueOf(iScore);
        } else {
            int iScoreOther = getScore(player.getOther());
            if ( iScore < 0 ) {
                return String.valueOf(iScore); // ?? seen this happen according to playstore. bug elsewere when going back into previous set using undo
            } else if ( iScore < NUMBER_OF_POINTS_TO_WIN_GAME ) {
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
            boolean possibleGameVictory = isPossibleGameVictory();
/*
            if (    (maxScore < NUMBER_OF_POINTS_TO_WIN_TIEBREAK)
                 || (maxScore - minScore < 2)
               )
*/
            if ( possibleGameVictory == false )
            {
                for(OnSetChangeListener l: onSetChangeListeners) {
                    l.OnXPointsPlayedInTiebreak(totalGamePoints);
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
                    List<Map<Player, Integer>> player2EndPointsOfGamesLastSet = ListUtil.getLast(m_lPlayer2EndPointsOfGames_PerSet);

                    // 2023-01-15: if a 0-0 was added as last game, remove it
                    Map<Player, Integer> player2EndPointsOfLastGame = ListUtil.getLast(player2EndPointsOfGamesLastSet);
                    if ( MapUtil.getMaxValue(player2EndPointsOfLastGame) == 0 ) {
                        ListUtil.removeLast(player2EndPointsOfGamesLastSet);
                    }
                    setPlayer2EndPointsOfGames(player2EndPointsOfGamesLastSet);

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

    /** for this model: returns end score of sets: e.g. 7-6,3-6,6-4 */
    @Override public String getGameScores() {
        //String gameScores = super.getGameScores();
        StringBuilder sb = new StringBuilder();
        for(List<Map<Player, Integer>> lGamesWonInSet : m_lPlayer2GamesWon_PerSet) {
            Map<Player, Integer> mTotalGamesWonInSet = ListUtil.getLast(lGamesWonInSet);
            if ( MapUtil.getMaxValue(mTotalGamesWonInSet) <=0 ) { continue; }
            if ( sb.length() > 0 ) {
                sb.append(",");
            }
            for ( Player p: Player.values() ) {
                if ( p.ordinal() > 0 ) sb.append("-");
                sb.append(mTotalGamesWonInSet.get(p));
            }
        }
        return sb.toString();
/*
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
*/
    }

    @Override public String getResultShort() {
        StringBuilder sb = new StringBuilder();
        Map<Player, Integer> mSetsWon = ListUtil.getLast(m_lSetCountHistory);
        if ( mSetsWon != null ) {
            for ( Player p: Player.values() ) {
                if ( p.ordinal() > 0 ) sb.append("-");
                sb.append(mSetsWon.get(p));
            }
        }
        if ( false ) {
            sb.append(" : ");
            // include set end scores
            sb.append(getGameScores());
        }

        return sb.toString();
    }

    /** overridden to check at the end of a game that it is also end of a set */
    @Override public void endGame(boolean bNotifyListeners, boolean bStartNewGame) {
        // invokes startNewGame() in the end
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
        if ( iGamesLeader == getNrOfGamesToWinSet() ) {
            bEndSet = iGamesLeader - iGamesTrailer >= 2;
            if ( this.getStartTiebreakOneGameEarly() ) {
                // special case e.g. sets won with 4 games, but tiebreak already played at 3-3
                bEndSet = iGamesLeader - iGamesTrailer >= 1;
            }
        } else if ( iGamesLeader > getNrOfGamesToWinSet() ) {
            bEndSet = iGamesLeader - iGamesTrailer >= 1;
        }
        if ( bEndSet ) {
            endSet(pLeader, iGamesLeader, iGamesTrailer, bNotifyListeners);
        }
        startNewGame();

        // ensure 'R' is indicated as serveside
        super.setServerAndSide(null, ServeSide.R, null);
    }


    @Override protected void setDirty(boolean bScoreRelated) {
        super.setDirty(bScoreRelated);
        if ( false && bScoreRelated ) {
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

    public boolean isTieBreakGame() {
        int iGameInProgress = getGameNrInProgress();
        if ( iGameInProgress % 2 == 0 ) {
            // game in progress is even: can not be a tiebreak
            return false;
        }
        if ( isFinalSet() ) {
            if ( m_finalSetFinish.numberOfGamesToWinSet() == GSMModel.FS_UNLIMITED_NR_OF_GAMES) {
                return false;
            }
        }

        if ( iGameInProgress >= (getNrOfGamesToWinSet() * 2 + 1) ) {
            return true;
        } else {
            if ( m_bStartTiebreakOneGameEarly ) {
                if ( iGameInProgress >= (getNrOfGamesToWinSet() * 2 - 1) ) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override Player[] calculatePossibleMatchVictoryFor(When when, Player[] playersSB) {
        if ( playersSB == null ) {
            Map<Player, Integer> scoreOfGameInProgress = getScoreOfGameInProgress();
            Map<Player, Integer> player2GamesWon       = getPlayer2GamesWon();
            Map<Player, Integer> player2setsWon        = getSetsWon();
            switch (when) {
                case Now:
                    int iMaxSets = MapUtil.getMaxValue(player2setsWon);
                    int iMinSets = MapUtil.getMinValue(player2setsWon);
                    if ( iMaxSets > iMinSets && iMaxSets >= m_iNrOfSetsToWinMatch ) {
                        Player maxKey = MapUtil.getMaxKey(player2setsWon, null);
                        return new Player[] { maxKey };
                    }
                    break;
                case ScoreOneMorePoint:
                    Player[] playersGB = super.calculateIsPossibleGameVictoryFor_SQ_TT_BM_RL(when, scoreOfGameInProgress, _getNrOfPointsToWinGame());
                    if ( ListUtil.length(playersGB) == 1 ) {
                        playersSB = this.calculateIsPossibleSetVictoryFor(playersGB, when, player2GamesWon);
                        if ( ListUtil.length(playersSB) == 1 ) {
                            int iSetsWon = MapUtil.getInt(player2setsWon, playersSB[0], 0);
                            if ( iSetsWon + 1 == m_iNrOfSetsToWinMatch ) {
                                return playersSB;
                            }
                        } else {
                            //Log.d(TAG, String.format("No setball %s, so no possible match victory", when));
                        }
                    }
                    break;
            }
        }

        return getNoneOfPlayers();
    }

    @Override Player[] calculateIsPossibleGameVictoryFor(When when, Map<Player, Integer> gameScore, boolean bFromIsMatchBallFrom) {
        final int iNrOfPointsToWinGame = _getNrOfPointsToWinGame();
        Player[] playersGB = getNoneOfPlayers();

        int onDeuceNumberZB = m_goldenPointFormat.onDeuceNumber();
        if ( onDeuceNumberZB >= 0 ) {
            // e.g Beach Tennis has this option, Padel is known to use it quite regularly
            int maxScore = getMaxScore(gameScore);
            if ( maxScore >= iNrOfPointsToWinGame - 1 ) {
                int diffScore = getDiffScore(gameScore);
                Player pLeader = getLeaderInGivenGame(gameScore); // null if no leader

                switch ( when ) {
                    case Now:
                        if ( maxScore >= iNrOfPointsToWinGame ) {
                            if ( diffScore >= 2 ) {
                                playersGB = new Player[] { pLeader };
                            } else if ( diffScore == 1 ) {
                                if ( isTieBreakGame() ) {
                                    playersGB = getNoneOfPlayers();
                                } else if ( maxScore >= iNrOfPointsToWinGame + onDeuceNumberZB ) {
                                    playersGB =  new Player[] { pLeader };
                                } else {
                                    playersGB = getNoneOfPlayers();
                                }
                            }
                        }
                        break;
                    case ScoreOneMorePoint:
                        if ( maxScore >= iNrOfPointsToWinGame - 1 ) {
                            if ( diffScore >= 1 ) {
                                playersGB = new Player[] { pLeader };
                            } else if ( diffScore == 0 ) {
                                if ( isTieBreakGame() ) {
                                    playersGB = getNoneOfPlayers();
                                } else if ( maxScore >= iNrOfPointsToWinGame - 1 + onDeuceNumberZB ) {
                                    playersGB = getPlayers();
                                }
                            }
                        }
                        break;
                }
            } else {
                // score to low for anyone to have gameball
            }
        } else {
            playersGB = super.calculateIsPossibleGameVictoryFor_SQ_TT_BM_RL(when, gameScore, iNrOfPointsToWinGame);
        }

        Player[] playersSB_Prev = m_possibleSetForPrev.get(when);
        Player[] playersSB = calculateIsPossibleSetVictoryFor(playersGB, when, getPlayer2GamesWon());

        boolean bSetBallFor_Unchanged0 = ListUtil.length(playersSB) == 0
                                      && ListUtil.length(playersSB_Prev) == 0;
        boolean bSetBallFor_Unchanged1 = ListUtil.length(playersSB) == 1
                                      && ListUtil.length(playersSB_Prev) == 1
                                      && playersSB_Prev[0].equals(playersSB[0]);
        boolean bSetBallFor_Unchanged2 = ListUtil.length(playersSB) == 2
                                      && ListUtil.length(playersSB_Prev) == 2;

        boolean bSetBallFor_Unchanged = bSetBallFor_Unchanged0 || bSetBallFor_Unchanged1 || bSetBallFor_Unchanged2;

        switch ( when ) {
            case Now:
                // TODO: check
                if ( bSetBallFor_Unchanged == false ) {
                    if ( ListUtil.isNotEmpty(playersSB) ) {
                        for( OnSetChangeListener l: onSetChangeListeners ) {
                            l.OnSetEnded(playersSB[0]);
                        }
                    } else {
                        Log.d(TAG, "No longer set ball");
                    }
                }
                break;
            case ScoreOneMorePoint:
                if ( bSetBallFor_Unchanged == false ) {
                    for( OnSetChangeListener l: onSetChangeListeners ) {
                        boolean isSetBall = ListUtil.isNotEmpty(playersSB);
                        l.OnSetBallChange(playersSB, isSetBall);
                    }
                }
                break;
        }
        m_possibleSetForPrev.put(when, playersSB);

        return playersGB;
    }

    private Map<When, Player[]> m_possibleSetForPrev  = new HashMap<>();
/*
    private Map<When, Player[]> m_possibleSetFor      = new HashMap<>();

    // Triggers listeners
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
            // now setBall for
            for( OnSetChangeListener l: onSetChangeListeners ) {
                l.OnSetBallChange(setBallForNew, ListUtil.isNotEmpty(setBallForNew));
            }
        }
    }
*/

    public Player[] isPossibleSetVictoryFor(){
        Map<Player, Integer> scoreOfGameInProgress = getScoreOfGameInProgress();
        When scoreOneMorePoint = When.ScoreOneMorePoint;
        Player[] playersGB = super.calculateIsPossibleGameVictoryFor_SQ_TT_BM_RL(scoreOneMorePoint, scoreOfGameInProgress, _getNrOfPointsToWinGame());
        Map<Player, Integer> player2GamesWon       = getPlayer2GamesWon();
        return calculateIsPossibleSetVictoryFor(playersGB, scoreOneMorePoint, player2GamesWon);
    }

    private Player[] calculateIsPossibleSetVictoryFor(Player[] playersWinningGame, When when, Map<Player, Integer> player2GamesWon){
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
            int nrOfGamesToWinSet = getNrOfGamesToWinSet();
            switch (when) {
                case Now:
                    // TODO:
                    if ( iGamesWon >= nrOfGamesToWinSet) {
                        // typically : at least 6 games won in set to 6
                        if ( iGamesWon > iGamesWonOpp ) {
                            if ( iGamesWon - iGamesWonOpp >= 2 ) {
                                return new Player[]{pPossibleSetBallFor};
                            } else if ( this.getStartTiebreakOneGameEarly() && (iGamesWon - iGamesWonOpp >= 1) ) {
                                return new Player[] { pPossibleSetBallFor};
                            } else {
                                if ( iGamesWon == 7 ) {
                                    // tiebreak (TODO: what if no tiebreak is played in final set)
                                    return new Player[] { pPossibleSetBallFor};
                                }
                            }
                        }
                    }
                    break;
                case ScoreOneMorePoint:
                    if ( iGamesWon >= nrOfGamesToWinSet - 1 ) {
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
                    break;
            }
        }
        return getNoneOfPlayers();
    }

/*
    @Override protected void clearPossibleGSM() {
        super.clearPossibleGSM();
        m_possibleSetForPrev .clear();m_possibleSetForPrev .putAll(m_possibleSetFor );
        m_possibleSetFor     .clear();
    }
*/

    @Override protected List getScorelinesRoot() {
        return m_lGamesScorelineHistory_PerSet;
    }
    @Override protected List getTimingsRoot() {
        return m_lGamesTiming_PerSet;
    }

    //-------------------------------
    // JSON
    //-------------------------------

    @Override void addFormatSettings(JSONObject joFormat) throws JSONException {
        super.addFormatSettings(joFormat);
        joFormat.put(PreferenceKeys.finalSetFinish      .toString(), m_finalSetFinish);
        joFormat.put(PreferenceKeys.newBalls            .toString(), m_newBalls);
        joFormat.put(JsonKey.lastBallChangeOccurredAtStartOfGame.toString(), m_iLastBallChangeOccurredAtStartOfGame);
        joFormat.put(PreferenceKeys.goldenPointFormat.toString(), m_goldenPointFormat);
        if ( m_bStartTiebreakOneGameEarly ) {
            joFormat.put(PreferenceKeys.StartTiebreakOneGameEarly.toString(), m_bStartTiebreakOneGameEarly);
        }
    }
    @Override void readFormatSettings(JSONObject joFormat) throws JSONException {
        super.readFormatSettings(joFormat);

        String s = joFormat.optString(PreferenceKeys.finalSetFinish.toString());
        if (StringUtil.isNotEmpty(s) ) {
            setFinalSetFinish(FinalSetFinish.valueOf(s));
        }

        String s2 = joFormat.optString(PreferenceKeys.newBalls.toString());
        if (StringUtil.isNotEmpty(s2) ) {
            setNewBalls(NewBalls.valueOf(s2));
        }
        int i = joFormat.optInt(JsonKey.lastBallChangeOccurredAtStartOfGame.toString(), 0);
        setLastBallChangeOccurredAtStartOfGame(i);

        String s3 = joFormat.optString(PreferenceKeys.goldenPointFormat.toString());
        if (StringUtil.isNotEmpty(s3) ) {
            setGoldenPointFormat(GoldenPointFormat.valueOf(s3));
        }

        boolean b2 = joFormat.optBoolean(PreferenceKeys.StartTiebreakOneGameEarly.toString());
        setStartTiebreakOneGameEarly(b2);
    }

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

    @Override public ScoreLine scoreHistoryFromJSON(boolean bMatchFormatTakenFromJson, JSONArray sets) throws JSONException {
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

            scoreLine = super.scoreHistoryFromJSON(bMatchFormatTakenFromJson, games);
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
