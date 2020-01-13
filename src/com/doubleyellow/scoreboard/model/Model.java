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

import android.content.Context;
import android.content.pm.PackageInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;
import com.doubleyellow.prefs.RWValues;
import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.util.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.Serializable;
import java.util.*;

/**
 * The model holding
 * - the scoring history
 * - appeals and decisions made
 * - the format of the match being played
 *
 * For each change in the model several listeners are registered and will be 'notified'.
 *
 * Sub-classes exist for
 * - Squash
 * - Tabletennis
 * - Racketlon
 * - Badminton
 */
public abstract class Model implements Serializable
{
    final String TAG = "SB." + this.getClass().getSimpleName();

    enum When {
        Now,
        ScoreOneMorePoint,
    }

    public abstract SportType getSport();
    /** mainly for Racketlon where each set is a different sport/discipline */
    public abstract Sport getSportForGame(int iGame1B);

    //-----------------------------------------------------
    // Listeners
    //-----------------------------------------------------
    interface OnModelChangeListener {

    }

    public interface OnLockChangeListener extends OnModelChangeListener {
        void OnLockChange(LockState lockStateOld, LockState lockStateNew);
    }
    public interface OnScoreChangeListener extends OnModelChangeListener {
		/** Invoked for every score change */
        void OnScoreChange(Player p, int iTotal, int iDelta, Call call);
    }
    public interface OnPlayerChangeListener extends OnModelChangeListener {
		/** Invoked if the name of the player stored in the model is changed */
        void OnNameChange (Player p, String sName, String sCountry, String sAvatar, String sClub, boolean bIsDoubles);
		/** Invoked if the color stored for a player is changed in the model */
        void OnColorChange(Player p, String sColor, String sColorOld);
        /** Invoked if the country stored for a player is changed in the model */
        void OnCountryChange(Player p, String sCountry);
        /** Invoked if the club stored for a player is changed in the model */
        void OnClubChange(Player p, String sClub);
        /** Invoked if the avatar stored for a player is changed in the model */
        void OnAvatarChange(Player p, String sClub);
    }
    public interface OnServeSideChangeListener extends OnModelChangeListener {
		/** Invoked every time there is a change of where the next serve takes place */
        void OnServeSideChange(Player p, DoublesServe doublesServe, ServeSide serveSide, boolean bIsHandout);
        void OnReceiverChange (Player p, DoublesServe doublesServe);
    }
    public interface OnSpecialScoreChangeListener extends OnModelChangeListener {
		/** invoked each time a the score change implies 'GameBall' change: i.e. now having gameball, or no-longer having gameball */
        void OnGameBallChange(Player[] players, boolean bHasGameBall);
		/** Invoked when both players reach a score one less than the score needed to win the game */
        void OnTiebreakReached(int iOccurrenceCount);
		/** Invoked a score is reached that would mean on player has won the game */
        void OnGameEndReached(Player leadingPlayer);
		/** Invoked as soon as the score 0-0 is not longer there */
        void OnFirstPointOfGame();
        /** Invoked as soon as the max score is half way (or .5 point over half way) */
        void OnGameIsHalfwayChange(int iGameZB, int iScoreA, int iScoreB, Halfway hwStatus); // e.g. change sides for racketlon/last game in tabletennis
    }
    public interface OnGameEndListener extends OnModelChangeListener {
        /** actually ended game and preparing for new one */
        void OnGameEnded(Player winningPlayer);
    }
    public interface OnMatchEndListener extends OnModelChangeListener {
		/** Invoked when a score is reached that means there is a winner */
        void OnMatchEnded(Player winningPlayer, EndMatchManuallyBecause endMatchManuallyBecause);
    }
    public interface OnComplexChangeListener extends OnModelChangeListener {
		/** 
		 * Invoked each time 'complex' change in the model took place.
		 * E.g. undo of score 'back' into a previous game 
		 * setting a 'start score' manually
		 * Undoing a 'call'
		 */
        void OnChanged();
    }
    public interface OnCallChangeListener extends OnModelChangeListener {
		/** Invoked each time a 'call' is recorded into the model */
        void OnCallChanged(Call call, Player appealingOrMisbehaving, Player pointAwardedTo, ConductType conductType);
    }
    public interface OnBrokenEquipmentListener extends OnModelChangeListener {
		/** Invoked each time a 'call' is recorded into the model */
        void OnBrokenEquipmentChanged(BrokenEquipment equipment, Player affectedPlayer);
    }

    transient private List<OnScoreChangeListener>        onScoreChangeListeners          = new ArrayList<OnScoreChangeListener>();
    transient private List<OnPlayerChangeListener>       onPlayerChangeListeners         = new ArrayList<OnPlayerChangeListener>();
    transient private List<OnServeSideChangeListener>    onServeSideChangeListener       = new ArrayList<OnServeSideChangeListener>();
    transient private List<OnSpecialScoreChangeListener> onSpecialScoreChangeListeners   = new ArrayList<OnSpecialScoreChangeListener>();
    transient private List<OnGameEndListener>            onGameEndListeners              = new ArrayList<OnGameEndListener>();
    transient private List<OnMatchEndListener>           onMatchEndListeners             = new ArrayList<OnMatchEndListener>();
    transient private List<OnCallChangeListener>         onCallChangeListeners           = new ArrayList<OnCallChangeListener>();
    transient private List<OnComplexChangeListener>      onComplexChangeListeners        = new ArrayList<OnComplexChangeListener>();
    transient private List<OnBrokenEquipmentListener>    onBrokenEquipmentListeners      = new ArrayList<OnBrokenEquipmentListener>();
    transient private List<OnLockChangeListener>         onLockChangeListeners           = new ArrayList<OnLockChangeListener>();
    transient private List<GameTiming.OnTimingChangedListener> onTimingChangedListeners  = new ArrayList<GameTiming.OnTimingChangedListener>();

    public int clearListeners(String sClassNameFilter) {
        int iCnt = 0;
        iCnt += ListUtil.removeObjects(onScoreChangeListeners       , sClassNameFilter);
        iCnt += ListUtil.removeObjects(onPlayerChangeListeners      , sClassNameFilter);
        iCnt += ListUtil.removeObjects(onServeSideChangeListener    , sClassNameFilter);
        iCnt += ListUtil.removeObjects(onSpecialScoreChangeListeners, sClassNameFilter);
        iCnt += ListUtil.removeObjects(onGameEndListeners           , sClassNameFilter);
        iCnt += ListUtil.removeObjects(onMatchEndListeners          , sClassNameFilter);
        iCnt += ListUtil.removeObjects(onCallChangeListeners        , sClassNameFilter);
        iCnt += ListUtil.removeObjects(onBrokenEquipmentListeners   , sClassNameFilter);
        iCnt += ListUtil.removeObjects(onComplexChangeListeners     , sClassNameFilter);
        iCnt += ListUtil.removeObjects(onLockChangeListeners        , sClassNameFilter);
        iCnt += ListUtil.removeObjects(onTimingChangedListeners     , sClassNameFilter);
        return iCnt;
    }
    public void registerListeners(Model lCopyFrom) {
        if ( lCopyFrom == null ) { return; }
        registerListeners(lCopyFrom.onScoreChangeListeners);
        registerListeners(lCopyFrom.onPlayerChangeListeners);
        registerListeners(lCopyFrom.onServeSideChangeListener);
        registerListeners(lCopyFrom.onSpecialScoreChangeListeners);
        registerListeners(lCopyFrom.onGameEndListeners);
        registerListeners(lCopyFrom.onMatchEndListeners);
        registerListeners(lCopyFrom.onCallChangeListeners);
        registerListeners(lCopyFrom.onBrokenEquipmentListeners);
        registerListeners(lCopyFrom.onComplexChangeListeners);
        registerListeners(lCopyFrom.onLockChangeListeners);
        registerListeners(lCopyFrom.onTimingChangedListeners);
    }
    private void registerListeners(List<? extends OnModelChangeListener> ls) {
        if ( ls == null ) { return; }
        for(OnModelChangeListener l: ls) {
            this.registerListener(l);
        }
    }
    public void registerListener(OnModelChangeListener changedListener) {
        if ( changedListener instanceof OnScoreChangeListener ) {
            OnScoreChangeListener scoreChangeListener = (OnScoreChangeListener) changedListener;
            onScoreChangeListeners.add(scoreChangeListener);
            for(Player p: getPlayers() ) {
                scoreChangeListener.OnScoreChange(p, m_scoreOfGameInProgress.get(p), 0, null);
            }
        }
        if ( changedListener instanceof OnPlayerChangeListener ) {
            final OnPlayerChangeListener playerChangeListener = (OnPlayerChangeListener) changedListener;
            onPlayerChangeListeners.add(playerChangeListener);
            boolean doubles = isDoubles();
            for(Player p: getPlayers() ) {
                playerChangeListener.OnNameChange   (p, getName   (p, true, false), getCountry(p), getAvatar(p), getClub(p), doubles);
                playerChangeListener.OnColorChange  (p, getColor  (p), null);
                playerChangeListener.OnCountryChange(p, getCountry(p));
                playerChangeListener.OnClubChange   (p, getClub   (p));
            }
        }
        if ( changedListener instanceof OnServeSideChangeListener ) {
            OnServeSideChangeListener serveSideChangeListener = (OnServeSideChangeListener) changedListener;
            onServeSideChangeListener.add(serveSideChangeListener);
            serveSideChangeListener.OnServeSideChange(m_pServer           , m_in_out         , m_nextServeSide, false);
            if ( isDoubles() ) {
                DoublesServe dsReceiver = determineDoublesReceiver(m_in_out, m_nextServeSide);
                this.changeDoubleReceiver(dsReceiver, true);
            }
        }
        if ( changedListener instanceof OnSpecialScoreChangeListener ) {
            OnSpecialScoreChangeListener specialScoreChangeListener = (OnSpecialScoreChangeListener) changedListener;
            onSpecialScoreChangeListeners.add(specialScoreChangeListener);
            triggerSpecialScoreListenersIfApplicable(null);
        }
        if ( changedListener instanceof OnGameEndListener ) {
            onGameEndListeners.add((OnGameEndListener) changedListener);
        }
        if ( changedListener instanceof OnMatchEndListener ) {
            onMatchEndListeners.add((OnMatchEndListener) changedListener);
        }
        if ( changedListener instanceof OnCallChangeListener ) {
            onCallChangeListeners.add((OnCallChangeListener) changedListener);
        }
        if ( changedListener instanceof OnBrokenEquipmentListener ) {
            onBrokenEquipmentListeners.add((OnBrokenEquipmentListener) changedListener);
        }
        if ( changedListener instanceof OnLockChangeListener ) {
            OnLockChangeListener lockChangeListener = (OnLockChangeListener) changedListener;
            onLockChangeListeners.add(lockChangeListener);
            lockChangeListener.OnLockChange(m_lockState, m_lockState);
        }
        if ( changedListener instanceof GameTiming.OnTimingChangedListener ) {
            GameTiming.OnTimingChangedListener timingChangedListener = (GameTiming.OnTimingChangedListener) changedListener;
            onTimingChangedListeners.add(timingChangedListener);
            if ( m_gameTimingCurrent != null ) {
                timingChangedListener.OnTimingChanged(ListUtil.size(m_lGameTimings)-1, GameTiming.Changed.Start, m_gameTimingCurrent.getStart(), m_gameTimingCurrent.getEnd(), GameTiming.ChangedBy.ListenerAdded);
            }
        }
        if ( changedListener instanceof OnComplexChangeListener ) {
            OnComplexChangeListener complexChangeListener = (OnComplexChangeListener) changedListener;
            onComplexChangeListeners.add(complexChangeListener);
            complexChangeListener.OnChanged();
        }
    }

    /** for updating ALL screen elements to have correct data from the model */
    public void triggerListeners() {
        for(OnComplexChangeListener l:onComplexChangeListeners) {
            l.OnChanged();
        }
        for(Player p: getPlayers()) {
            for(OnPlayerChangeListener l: onPlayerChangeListeners) {
                l.OnNameChange   (p, getName(p), getCountry(p), getAvatar(p), getClub(p), isDoubles());
              //l.OnClubChange   (p, getClub(p));
              //l.OnCountryChange(p, getCountry(p));
                l.OnColorChange  (p, getColor(p), null);
            }
        }
        Player[] paGameBallFor = isPossibleGameBallFor();
        if ( ListUtil.length(paGameBallFor) != 0 ) {
            for(OnSpecialScoreChangeListener l: onSpecialScoreChangeListeners) {
                l.OnGameBallChange(paGameBallFor, true);
            }
        }
    }

    private static final boolean NEXT_SERVE_SIDE_FROM_COUNT = false;

    //------------------------
    // Match Format / Player/Time Details
    //------------------------

    public final static int UNDEFINED_VALUE = -1;

    /** nr of points needed to win a game */
    private int                                 m_iNrOfPointsToWinGame  = 11;
    /** nr of games needed to win a match */
    private int                                 m_iNrOfGamesToWinMatch  = 3;
    /** If true, in 'best of 5' all 5 games will be played */
    private int m_iTotalNrOfGamesToFinishForMatchToEnd = UNDEFINED_VALUE;

    /** if true: point only if serving (english). If false: rally point (American) */
    private boolean                             m_bEnglishScoring       = false;
    private TieBreakFormat                      m_TieBreakFormat        = TieBreakFormat.TwoClearPoints;
    private HandicapFormat                      m_HandicapFormat        = HandicapFormat.None;

    private Map<Player, String>                 m_player2Name           = new HashMap<Player, String>();
    private Map<Player, String>                 m_player2Id             = new HashMap<Player, String>();
    private Map<Player, String>                 m_player2Color          = new HashMap<Player, String>();
    private Map<Player, String>                 m_player2Country        = new HashMap<Player, String>();
    private Map<Player, String>                 m_player2Club           = new HashMap<Player, String>();
    /** Contains list of player names as the appeared in JSON. e.g. ${seq}:${id}:{LastName}, ${FirstName} */
    private Map<Player, List<String>>           m_team2Players          = new HashMap<Player, List<String>>();
    private Map<Player, String>                 m_player2Avatar         = new HashMap<Player, String>();
    private String                              m_matchDate             = DateUtil.getCurrentYYYY_MM_DD();   // up to apk 202 DateUtil.getCurrentYYYYMMDD()
    private String                              m_matchTime             = DateUtil.getCurrentHHMMSS_Colon() + DateUtil.getTimezoneXXX(); // up to apk 202 DateUtil.getCurrentHHMMSS()

    //------------------------
    // Serve side statistics
    //------------------------

    /** Holds, per player, his last 'choosen' serve side when it was a handout. Only useful for Squash */
    private Map<Player, ServeSide>              m_player2LastServeSide  = null;
    /** Holds, per player, the number of times he has choosen a certain side */
            Map<Player ,Map<ServeSide,Integer>> m_player2ServeSideCount = null;


    //------------------------
    // Serve side/sequence
    //------------------------
            ServeSide                           m_nextServeSide         = ServeSide.R;
    private Player                              m_pServer               = Player.A;
    private boolean                             m_bLastPointWasHandout  = true;

    //------------------------
    // Double Serve side/sequence
    //------------------------
    private boolean                             m_bIsDouble             = false;
    private DoublesServe                        m_in_out/*server*/      = DoublesServe.NA;
    private DoublesServe                        m_in_out_receiver       = DoublesServe.NA;
    // fixed per match in e.g. squash, might vary per set in e.g. racketlon
            DoublesServeSequence                m_doubleServeSequence   = DoublesServeSequence.NA;

    //------------------------
    // Points history
    //------------------------

    /** For drawing 'original' paper scoring. Contains all games including the one in progress */
    private List<List<ScoreLine>>               m_lGameScoreHistory     = null;
    /** scorehistory of the game in progress */
    private List<ScoreLine>                     m_lScoreHistory         = null;

    //------------------------
    // Point scores
    //------------------------

    /** scoring of the game in progress: { A=5, B=8 } */
    private Map<Player, Integer>                m_scoreOfGameInProgress   = null;
    /** end scores of already ended games [ {A=11,B=9},{A=4,B=11}, {A=11, B=8} ] . So does not hold game in progress. */
    private List<Map<Player, Integer>>          m_endScoreOfPreviousGames = null;

    private Map<Player, Integer>                m_startScoreOfGameInProgress = null;
    private List<Map<Player, Integer>>          m_startScoreOfPreviousGames  = null;

    //------------------------
    // Game scores
    //------------------------

    /** number of games each player has won: {A=2, B=1} */
    private Map<Player, Integer>                m_player2GamesWon       = null;
    /** holds array with history of games won like [0-0, 0-1, 1-1, 2-1] */
    private List<Map<Player, Integer>>          m_lGameCountHistory     = null;
    private List<Player>                        m_lGameWinner           = null;

    transient private GameTiming                          m_gameTimingCurrent     = null;
    /** game timing of all games including the one about to start/started */
    transient private List<GameTiming>                    m_lGameTimings          = null;

    //------------------------
    // Match scores
    //------------------------
    public  Player                              m_winnerBecauseOf       = null; // reason is 'encoded' in the m_lockState

    Model() {
        init();

        startNewGame();
    }

    void init() {
        m_player2LastServeSide  = new HashMap<Player, ServeSide>();
        m_player2LastServeSide.put(Player.A, ServeSide.R);
        m_player2LastServeSide.put(Player.B, ServeSide.R);

        m_player2ServeSideCount = new HashMap<Player, Map<ServeSide, Integer>>();
        m_player2ServeSideCount.put(Player.A, new HashMap<ServeSide, Integer>());
        m_player2ServeSideCount.put(Player.B, new HashMap<ServeSide, Integer>());

        m_lGameScoreHistory     = new ArrayList<List<ScoreLine>>();

        m_player2GamesWon       = new HashMap<Player, Integer>();
        m_player2GamesWon.put(Player.A, 0);
        m_player2GamesWon.put(Player.B, 0);

        m_lGameCountHistory     = new ArrayList<Map<Player, Integer>>();
        m_lGameCountHistory.add(m_player2GamesWon);

        addNewGameScoreDetails();

        m_scoreOfGameInProgress = new HashMap<Player, Integer>();
        m_scoreOfGameInProgress.put(Player.A, 0);
        m_scoreOfGameInProgress.put(Player.B, 0);

        m_startScoreOfGameInProgress = new HashMap<Player, Integer>();
        m_startScoreOfGameInProgress.put(Player.A, 0);
        m_startScoreOfGameInProgress.put(Player.B, 0);
        m_startScoreOfPreviousGames = new ArrayList<Map<Player, Integer>>();

        m_endScoreOfPreviousGames = new ArrayList<Map<Player, Integer>>();
        m_lGameWinner             = new ArrayList<Player>();
        m_lGameTimings            = new ArrayList<GameTiming>();
    }

	/** Invoked when model is created and when a game is ended */
    private void startNewGame() {
        m_scoreOfGameInProgress = new HashMap<Player, Integer>();
        int iInitialScoreA = MapUtil.getInt(m_startScoreOfGameInProgress, Player.A, 0);
        int iInitialScoreB = MapUtil.getInt(m_startScoreOfGameInProgress, Player.B, 0);
        m_scoreOfGameInProgress.put(Player.A, iInitialScoreA);
        m_scoreOfGameInProgress.put(Player.B, iInitialScoreB);

        m_gameTimingCurrent = new GameTiming(m_lGameTimings.size(), System.currentTimeMillis(), System.currentTimeMillis(), onTimingChangedListeners);
        m_lGameTimings.add(m_gameTimingCurrent);

        Player pLastScorer = null;
        if ( hasStarted() ) {
            ScoreLine slLast = getLastScoreLine();
            if (slLast != null) { pLastScorer = slLast.getScoringPlayer(); }
        }
        addNewGameScoreDetails();

        m_rallyEndStatsGIP = new JSONArray();
        m_rallyEndStatistics.add(m_rallyEndStatsGIP);

        if ( isDoubles() ) {
            handout(pLastScorer, false);
        } else {
            handout(m_pServer, false);
        }

        for(OnScoreChangeListener l:onScoreChangeListeners) {
            l.OnScoreChange(Player.A, iInitialScoreA, 0, null);
            l.OnScoreChange(Player.B, iInitialScoreB, 0, null);
        }

        m_iTieBreakPlusX = 0; // reset value that might have been set for tie-break
        m_halfwayStatus = Halfway.Before;
    }

    //-------------------------------
    // serve side/sequence
    //-------------------------------

    public static Map<String, DoublesServeSequence> mOldDSS2New = new HashMap<>(); // apk 127
    static {
        mOldDSS2New.put("ABXY", DoublesServeSequence.A1A2B1B2);
        mOldDSS2New.put("BXYA", DoublesServeSequence.A2B1B2_then_A1A2B1B2);
        mOldDSS2New.put("AXBY", DoublesServeSequence.A1B1A2B2);
        mOldDSS2New.put("AXAX", DoublesServeSequence.A1B1A1B1);
    }

    public abstract boolean setDoublesServeSequence(DoublesServeSequence dsq);
    abstract DoublesServeSequence getDoubleServeSequence(int iGameZB);

    /** for now this method MUST invoke the setServerAndSide method: TODO: improve */
    abstract void determineServerAndSideForUndoFromPreviousScoreLine(ScoreLine lastValidWithServer, ScoreLine slRemoved);

    /** for now this method MUST invoke the setServerAndSide method: TODO: improve. Invoked from endGame() */
    abstract Player determineServerForNextGame(int iGameZB, int iScoreA, int iScoreB);

    /**
     * Racketlon  : What character is returned is based on the discipline in progress
     * Squash     : L or R + a question mark if handout
     * Tabletennis: Returns character to indicate number of serves left
     **/
    public abstract Object convertServeSideCharacter(String sRLInternational, ServeSide serveSide, String sHandoutChar);

    public abstract boolean showChangeSidesMessageInGame(int iGameZB);

    /** e.g. overwritten in Tabletennis */
    DoublesServe determineDoublesReceiver(DoublesServe serverOfOppositeTeam, ServeSide serveSide) {
        return DoublesServe.NA;
    }

    void setServerAndSide(Player player, ServeSide side, DoublesServe doublesServe) {
        boolean bChanged = false;
        if ( (player != null) && player.equals(m_pServer) == false ) {
            m_pServer = player;
            bChanged = true;
        }
        if ( (side != null) && side.equals(m_nextServeSide) == false ) {
            m_nextServeSide = side;
            bChanged = true;
        }
        if ( (doublesServe != null) && doublesServe.equals(m_in_out) == false ) {
            m_in_out = doublesServe;
            bChanged = true;
        }

        if ( bChanged ) {
            // inform listeners
            boolean lastPointHandout = isLastPointHandout();
            for(OnServeSideChangeListener l: onServeSideChangeListener) {
                l.OnServeSideChange(m_pServer, m_in_out, m_nextServeSide, lastPointHandout);
            }
        }
        if ( isDoubles() ) {
            DoublesServe dsReceiver = determineDoublesReceiver(m_in_out, m_nextServeSide);
            this.changeDoubleReceiver(dsReceiver, bChanged);
        }
    }

    /** at start of game it is also the winner of last game */
    public Player getServer() {
        return m_pServer;
    }
    public Player getReceiver() {
        return m_pServer.getOther();
    }

    public boolean isDoubles() {
        return m_bIsDouble;
        //return (m_in_out.equals(DoublesServe.NA) == false);
    }
    public DoublesServe getNextDoubleServe(Player p) {
        if ( p.equals(m_pServer) == false ) {
            return DoublesServe.NA;
        }
        return m_in_out;
    }
    public void changeDoubleServe(Player p) {
        setServerAndSide(null, null, m_in_out.getOther());
    }
    /** should be called only from setServerAndSide */
    void changeDoubleReceiver(DoublesServe doublesServe, boolean bChanged) {
        if ( doublesServe != null ) {
            if ( doublesServe.equals(m_in_out_receiver) == false ) {
                m_in_out_receiver = doublesServe;
                bChanged = true;
            }
        }
        if ( bChanged ) {
            for(OnServeSideChangeListener l: onServeSideChangeListener) {
                l.OnReceiverChange(m_pServer.getOther(), m_in_out_receiver);
            }
        }
    }
    public DoublesServe getDoubleReceiver() {
        return m_in_out_receiver;
    }

    public ServeSide getNextServeSide(Player player) {
        if ( player.equals(m_pServer) ) {
            return m_nextServeSide;
        } else {
            return null;
        }
    }

    final boolean _setDoublesServeSequence(DoublesServeSequence dsq) {
        boolean bChanged = dsq.equals(m_doubleServeSequence) == false;
        DoublesServe new_i_o = m_in_out;
        if ( dsq.equals(DoublesServeSequence.NA) ) {
            m_doubleServeSequence = DoublesServeSequence.NA;
            new_i_o               = DoublesServe.NA;
        } else {
            m_doubleServeSequence = dsq;
            new_i_o               = dsq.playerToServe(DoublesServe.NA, true, m_iHandoutCountDoubles);
        }
        setServerAndSide(m_pServer, m_nextServeSide, new_i_o);
        return bChanged;
    }
    public DoublesServeSequence getDoubleServeSequence() {
        int iGameZB = getGameNrInProgress() - 1;
        return getDoubleServeSequence(iGameZB);
    }

    /** Invoked from GUI only */
    public void changeSide(Player player) {
        boolean bSamePlayer = player.equals(m_pServer);
        if ( bSamePlayer ) {
            setServerAndSide(null, m_nextServeSide.getOther(), null);
        } else {
            setServerAndSide(player, m_player2LastServeSide.get(player), null);
        }
    }

    /** invoked from BrokenWhat dialog */
    public void recordBroken(Player player, BrokenEquipment brokenEquipment) {
        ScoreLine slBroken = new ScoreLine(player, brokenEquipment);
        addScoreLine(slBroken, true);

        setDirty(false);
        for(OnBrokenEquipmentListener l: onBrokenEquipmentListeners) {
            l.OnBrokenEquipmentChanged(brokenEquipment, player);
        }
    }

    public boolean isLastPointHandout() {
        return m_bLastPointWasHandout;
    }
    //-------------------------------
    // conduct/appeal
    //-------------------------------

    /** invoked from Appeal dialog */
    public abstract void recordAppealAndCall(Player appealing, Call call);

    /** invoked from Conduct dialog */
    public abstract void recordConduct(Player pMisbehaving, Call call, ConductType conductType);

    /** Holds conduct types decisions by the referee in sequence */
    private List<String> lConductCalls = new ArrayList<String>();

    public Map<String,String> getConduct(int i) {
        String sCallData = lConductCalls.get(i);
        return MapUtil.parseToMap(sCallData);
    }

    //-------------------------------
    // Score
    //-------------------------------

    public abstract String getResultShort();

    public abstract void changeScore(Player player);

    private String m_sResultFast = null;
    /** @deprecated */
    public void setResult(String s) {
        m_sResultFast = s;
    }
    public String getResult() {
        if ( m_sResultFast == null ) {
            return m_sResultFast = getResultShort();
        }
        return m_sResultFast;
    }
    /** Calculates a short result. For Squash something like 3-1. For Racketlon something like A +24 or B +3 */
    public String getGameScores() {
        StringBuilder sbGames = new StringBuilder();
        List<Map<Player, Integer>> gameEndScores = this.getGameScoresIncludingInProgress();
        for(Map<Player, Integer> mGameScore: gameEndScores) {
            Integer iA = mGameScore.get(Player.A);
            Integer iB = mGameScore.get(Player.B);
            if ( iA + iB > 0 ) {
                if ( sbGames.length() != 0 ) {
                    sbGames.append(",");
                }
                sbGames.append(iA).append("-").append(iB);
            }
        }
        return sbGames.toString();
    }

    /** returns the difference taking all points into account */
    public Map<Player, Integer> getPointsDiff(boolean bIncludeGameInProgress) {
        Map<Player, Integer> pointsWon = _getTotalNumberOfPointsScored(bIncludeGameInProgress);
        int iMax = MapUtil.getMaxValue(pointsWon);
        int iMin = MapUtil.getMinValue(pointsWon);
        if ( iMax > iMin ) {
            Player pLeader = MapUtil.getMaxKey(pointsWon, null);
            pointsWon.put(pLeader           , iMax - iMin);
            pointsWon.put(pLeader.getOther(), iMin - iMax);
        } else {
            pointsWon.put(Player.A, 0);
            pointsWon.put(Player.B, 0);
        }
        return pointsWon;
    }

    private void addScoreLine(ScoreLine slCall, boolean bAddTiming) {
        m_lScoreHistory.add(slCall);

        if ( bAddTiming ) {
            if ( m_gameTimingCurrent == null ) {
                m_gameTimingCurrent = new GameTiming(m_lGameTimings.size(), System.currentTimeMillis(), System.currentTimeMillis(), onTimingChangedListeners);
                m_lGameTimings.add(m_gameTimingCurrent);
            }
            if ( m_gameTimingCurrent.startTimeIsSetManually() == false ) {
                // correct by guessing start time
                if ( ListUtil.size(m_lScoreHistory) == 1 ) {
                    GameTiming gameTimingPrevious = ListUtil.size(m_lGameTimings) > 1 ? m_lGameTimings.get(m_lGameTimings.size() - 2) : null;
                    if (gameTimingPrevious != null) {
                        int iDiffBetweenEndOfPrevAndStartOfCurrent = DateUtil.convertToSeconds(m_gameTimingCurrent.getStart() - gameTimingPrevious.getEnd());
                        if (iDiffBetweenEndOfPrevAndStartOfCurrent < 30) {
                            // adjust start of game
                            int iSecondsPassed = DateUtil.convertToSeconds(System.currentTimeMillis() - m_gameTimingCurrent.getStart());
                            if (iSecondsPassed > I_NR_OF_SECS_CORRECTION) {
                                m_gameTimingCurrent.updateStart(-1 * I_NR_OF_SECS_CORRECTION, GameTiming.ChangedBy.FirstScoreOfGameEntered);
                            }
                        }
                    }
                }
            }

            // also record a timestamp (nr of seconds since start of game)
            m_gameTimingCurrent.addTiming();
        }
    }

    /** also sets the model to dirty */
    private Integer determineNewScoreForPlayer(Player player, int iDelta, boolean bEnglishScoring) {
        Integer iNewScore;
        if ( bEnglishScoring && (iDelta == 1) ) {
            if ( player.equals(m_pServer) ) {
                iNewScore = MapUtil.increaseCounter(m_scoreOfGameInProgress, player, iDelta);
            } else {
                iNewScore = MapUtil.increaseCounter(m_scoreOfGameInProgress, player, 0);
            }
        } else {
            iNewScore = MapUtil.increaseCounter(m_scoreOfGameInProgress, player, iDelta);
        }
        setDirty(true);
        return iNewScore;
    }

    private ScoreLine getScoreLine(Player player, Integer iNewScore, ServeSide sCurrentSide) {
        ScoreLine scoreLine;
        if ( player.equals(m_pServer) ) {
            // score for the server

            // first update history array
            if (m_pServer.equals(Player.A)) scoreLine = new ScoreLine( sCurrentSide, iNewScore, null        , null      );
            else                            scoreLine = new ScoreLine( null        , null     , sCurrentSide, iNewScore );
        } else {
            // hand-out: score for receiver

            // first update history array
            if (m_pServer.equals(Player.A)) scoreLine = new ScoreLine( sCurrentSide, null     , null        , iNewScore );
            else                            scoreLine = new ScoreLine( null        , iNewScore, sCurrentSide, null      );
        }
        return scoreLine;
    }

    private void changeScoreInformListeners(Player player, boolean bTriggerServeSideChange, Call call, int iDelta, Player previousServer, DoublesServe previousDS, Integer iNewScore) {
        for(OnScoreChangeListener l: onScoreChangeListeners) {
            l.OnScoreChange(player, iNewScore, iDelta, call);
        }
        if ( bTriggerServeSideChange ) {
            if ( isDoubles() ) {
                // calculated earlier and set by calling setLastPointWasHandout(). depends in DoubleServe=In or Out
            } else {
                boolean bIsHandout = (m_pServer.equals(previousServer) == false) || (m_in_out.equals(previousDS) == false);
                setLastPointWasHandout(bIsHandout);
            }
        }
        triggerSpecialScoreListenersIfApplicable(player);
    }

    private void triggerSpecialScoreListenersIfApplicable(Player scoredPoint) {
        // just recalculate it if it is not yet known. will trigger additional listeners
        Player[] gameBallFor = isPossibleGameBallFor();

        if ( isStartOfTieBreak() ) {
            m_iNrOfTiebreaks = Math.min(m_iNrOfTiebreaks+1, ListUtil.size(m_lGameWinner)+1);
            for(OnSpecialScoreChangeListener l: onSpecialScoreChangeListeners) {
                l.OnTiebreakReached(m_iNrOfTiebreaks);
            }
        }
        Player possibleGameVictoryFor = isPossibleGameVictoryFor();
        if ( possibleGameVictoryFor != null ) {
            if ( m_lockState.isLocked() == false ) {
                if ( m_gameTimingCurrent != null ) {
                    m_gameTimingCurrent.updateEnd(GameTiming.ChangedBy.GameEndingScoreReached);
                }
            }
            for(OnSpecialScoreChangeListener l: onSpecialScoreChangeListeners) {
                l.OnGameEndReached(possibleGameVictoryFor);
            }
        }

        int     maxScore             = getMaxScore();
        boolean bMaxScoreJustReached = getScore(scoredPoint) == maxScore && (getDiffScore() != 0);
        int     nrOfPointsToWinGame  = getNrOfPointsToWinGame();
        boolean bTotalIsEven         = nrOfPointsToWinGame % 2 == 0;
        switch(m_halfwayStatus) {
            case Before:
            case JustBefore:
                if ( bMaxScoreJustReached == false ) {
                    // BACK to before: e.g in a game to 11: if 5-0 is reached and then 5-1 is scored... we are back in Before untill JustAfter will be reached
                    setGameIsHalfway(Halfway.Before);
                } else /*if (getDiffScore() > 0 )*/ {
                    // max of both players was reached by scoring player
                    if ( bTotalIsEven ) {
                        if ( maxScore == nrOfPointsToWinGame / 2 ) {
                            setGameIsHalfway(Halfway.Exactly);
                        }
                    } else {
                        if ( maxScore == (nrOfPointsToWinGame - 1) / 2 ) {
                            setGameIsHalfway(Halfway.JustBefore);
                        }
                        if ( maxScore == (nrOfPointsToWinGame + 1) / 2 ) {
                            setGameIsHalfway(Halfway.JustAfter);
                        }
                    }
                }
                break;
            case Exactly:
            case JustAfter:
                setGameIsHalfway(Halfway.After);
                break;
            case After:
                // once we are after in a game, no going back in this game
                break;
        }

        boolean bOneRallyPlayed = ListUtil.size(m_lScoreHistory) == 1 || (ListUtil.size(m_lScoreHistory) == 2 && m_lScoreHistory.get(0).isAppealWithPoint());
        if ( isUsingHandicap() ) {
            bOneRallyPlayed = ListUtil.size(m_lScoreHistory) == 1 || (ListUtil.size(m_lScoreHistory) == 2 && m_lScoreHistory.get(0).isAppealWithPoint());
        }
        if ( bOneRallyPlayed ) {
            for(OnSpecialScoreChangeListener l: onSpecialScoreChangeListeners) {
                l.OnFirstPointOfGame();
            }
        }
    }

    private Halfway m_halfwayStatus = Halfway.Before;
    private void setGameIsHalfway(Halfway hwStatus) {
        if ( m_halfwayStatus.equals(hwStatus) ) { return; }

        int iGameZB = getGameNrInProgress() - 1;
        int scoreA  = getScore(Player.A);
        int scoreB  = getScore(Player.B);

        for(OnSpecialScoreChangeListener l: onSpecialScoreChangeListeners) {
            l.OnGameIsHalfwayChange(iGameZB, scoreA, scoreB, hwStatus);
        }
      //Log.d(TAG, String.format("hwStats from %s to %s", m_halfwayStatus, hwStatus));
        m_halfwayStatus = hwStatus;
    }

    /** special undo. Might remove a scoreline that has already followed by a scoreline where the other player scored. Basic implementation for tabletennis. E.g. will not work with conduct calls for Squash */
    public synchronized boolean undoLastForScorer(Player p) {
        if ( m_lScoreHistory.size() == 0 ) {
            return false;
        }

        // find last scoreline that increase the score for the specified player
        ScoreLine lastValidWithScoring = getLastWithValidScorerEquals(p);
        if ( lastValidWithScoring == null ) {
            return false;
        }
        int iIDX = m_lScoreHistory.indexOf(lastValidWithScoring);

        // remove that specific line from ...
        m_lScoreHistory.remove(iIDX);
        m_gameTimingCurrent.removeTimings(iIDX);

        if ( ListUtil.size(m_lScoreHistory) < JsonUtil.size(m_rallyEndStatsGIP) ) {
            // TODO: for now I only remove statistics if the number of statistics is simply to large: this can be improved
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ) {
                m_rallyEndStatsGIP.remove(m_rallyEndStatsGIP.length() - 1);
            }
        }

        // inform listeners
        int iReducedScore = MapUtil.increaseCounter(m_scoreOfGameInProgress, p, -1);
        for (OnScoreChangeListener l : onScoreChangeListeners) {
            l.OnScoreChange(p, iReducedScore, -1, null);
        }
        for(OnComplexChangeListener l:onComplexChangeListeners) {
            l.OnChanged();
        }

        return true;
    }
    public synchronized void undoLast() {
        setDirty(true);

        switch (m_halfwayStatus) {
            case Exactly:
            case JustAfter:
            case JustBefore:
                setGameIsHalfway(Halfway.Before);
                break;
        }

        if ( m_lScoreHistory.size() == 0 ) {
            if ( getGameNrInProgress() <= 1 ) {
                // nothing to undo. we have not started yet
                return;
            }

            // go back into the previous game
            ListUtil.removeLast(m_lGameScoreHistory);
            ListUtil.removeLast(m_lGameCountHistory);
            ListUtil.removeLast(m_rallyEndStatistics);
            m_lScoreHistory         = ListUtil.getLast(m_lGameScoreHistory);
            m_player2GamesWon       = ListUtil.getLast(m_lGameCountHistory);
            m_rallyEndStatsGIP      = ListUtil.getLast(m_rallyEndStatistics);
            ListUtil.removeLast(m_lGameWinner);

            m_scoreOfGameInProgress = ListUtil.removeLast(m_endScoreOfPreviousGames);
            if ( m_HandicapFormat.equals(HandicapFormat.DifferentForAllGames) ) {
                m_startScoreOfGameInProgress = ListUtil.removeLast(m_startScoreOfPreviousGames);
            }

            if ( m_lGameTimings.size() > getGameNrInProgress() ) {
                ListUtil.removeLast(m_lGameTimings);
                m_gameTimingCurrent = ListUtil.getLast(m_lGameTimings);
            }

            for(OnComplexChangeListener l:onComplexChangeListeners) {
                l.OnChanged();
            }
        } else {
            // remove the last
            ScoreLine slRemoved = ListUtil.removeLast(m_lScoreHistory);
            m_gameTimingCurrent.removeTimings(ListUtil.size(m_lScoreHistory));

            if ( ListUtil.size(m_lScoreHistory) < JsonUtil.size(m_rallyEndStatsGIP) ) {
                // TODO: for now I only remove statistics if the number of statistics is simply to large: this can be improved
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ) {
                    m_rallyEndStatsGIP.remove(m_rallyEndStatsGIP.length() - 1);
                }
            }

            if ( (slRemoved != null) && slRemoved.isCall() ) {
                // usually only a 'just let' or 'conduct warning'
                if ( slRemoved.call.isConduct() ) {
                    String removedConductCall = ListUtil.removeLast(lConductCalls);
                }
                ScoreLine lastValidWithServer = getLastWithValidServer();
                if ( lastValidWithServer != null ) {
                    determineServerAndSideForUndoFromPreviousScoreLine(lastValidWithServer, null);
                }

                if ( slRemoved.call.getScoreAffect().equals(Call.ScoreAffect.LoseGame) ) {
                    // we are undo-ing a conduct-game (CG)
                    Player adjustScoreFor = slRemoved.getCallTargetPlayer().getOther();
                    for(ScoreLine l: m_lScoreHistory) {
                        if ( adjustScoreFor.equals(l.getScoringPlayer()) ) {
                            m_scoreOfGameInProgress.put(adjustScoreFor, l.getScore());
                        }
                    }
                    // TODO: does not work if score went from zero directly to 11 for scoring player
                }
                // inform listeners
                for(OnComplexChangeListener l:onComplexChangeListeners) {
                    l.OnChanged();
                }
            } else {
                int iDelta = -1;
                Player removeScoringPlayer  = slRemoved.getScoringPlayer();
                Player removedServingPlayer = slRemoved.getServingPlayer();
                if ( removeScoringPlayer != null ) {
                    if ( m_bEnglishScoring ) {
                        if (removeScoringPlayer.equals(removedServingPlayer) == false) {
                            iDelta = 0;
                        }
                    }
                    int iReducedScore = MapUtil.increaseCounter(m_scoreOfGameInProgress, removeScoringPlayer, iDelta);
                    for (OnScoreChangeListener l : onScoreChangeListeners) {
                        l.OnScoreChange(removeScoringPlayer, iReducedScore, iDelta, null);
                    }
                } else if ( slRemoved.isBrokenEquipment() ) {
                    for (OnBrokenEquipmentListener l : onBrokenEquipmentListeners) {
                        l.OnBrokenEquipmentChanged(slRemoved.getBrokenEquipment(), null);
                    }
                }

                if ( m_lScoreHistory.size() != 0 ) {
                    ScoreLine lastValid = getLastScoreLine();
                    if ( lastValid.isCall() ) {
                        if ( lastValid.call.hasScoreAffect() ) {
                            undoLast(); // remove the call that came with the score
                        }
                    } else {
                        if ( lastValid.isBrokenEquipment() ) {
                            // as last valid, take one earlier to be able to see who server was
                            lastValid = m_lScoreHistory.get(Math.max(0, m_lScoreHistory.size() - 2));
                        }
                        determineServerAndSideForUndoFromPreviousScoreLine(lastValid, slRemoved);
                        if ( isDoubles() && getSport().equals(SportType.Squash)) {
                            m_iHandoutCountDoubles--;
                        }
                    }
                } else {
                    // last scoreline was removed
                    determineServerAndSideForUndoFromPreviousScoreLine(null, slRemoved);
                    m_iHandoutCountDoubles = -1;
                }

                Player[] gameBallFor = isPossibleGameBallFor();
                //setGameVictoryFor(gameBallFor);
            }
        }
    }

    public Player getLastScorer() {
        Player pLastScorer = null;
        ScoreLine last = this.getLastScoreLine();
        if ( last != null ) {
            pLastScorer = last.getScoringPlayer();
        }
        return pLastScorer;
    }

    public ScoreLine getLastScoreLine() {
        return ListUtil.getLast(m_lScoreHistory);
    }

    public ScoreLine getLastCall() {
        int i = ListUtil.size(m_lScoreHistory) - 1;
        while (   ( i >= 0 )
               && (m_lScoreHistory.get(i).isCall() == false)) {
            i--;
        }
        if ( (i >=0) && m_lScoreHistory.get(i).isCall() ) {
            return m_lScoreHistory.get(i);
        }
        return null;
    }

    private ScoreLine getLastWithValidServer() {
        int iIdx = m_lScoreHistory.size()-1;
        while ( iIdx>=0 && m_lScoreHistory.get(iIdx).getServingPlayer()==null ) {
            iIdx--;
        }
        return iIdx>=0?m_lScoreHistory.get(iIdx):null;
    }
    private ScoreLine getLastWithValidScorerEquals(Player p) {
        int iIdx = m_lScoreHistory.size()-1;
        while ( iIdx>=0 && p.equals(m_lScoreHistory.get(iIdx).getScoringPlayer()) ==false ) {
            iIdx--;
        }
        return iIdx>=0?m_lScoreHistory.get(iIdx):null;
    }

    public void setGameStartScoreOffset(Player player, int iOffset) {
        int iGame = getNrOfFinishedGames();
        while ( ListUtil.size(m_startScoreOfPreviousGames) < iGame  ) {
            m_startScoreOfPreviousGames.add(new HashMap<Player, Integer>(m_startScoreOfGameInProgress));
        }
        m_startScoreOfGameInProgress.put(player, iOffset);
        if ( ListUtil.size(m_lScoreHistory) == 0 ) {
            m_scoreOfGameInProgress.putAll(m_startScoreOfGameInProgress);
            for(OnComplexChangeListener l: onComplexChangeListeners) {
                l.OnChanged();
            }
        }
    }
    public List<Map<Player, Integer>> getGameStartScoreOffsets() {
        List<Map<Player, Integer>> lReturn = new ArrayList<Map<Player, Integer>>(m_startScoreOfPreviousGames);
        lReturn.add(m_startScoreOfGameInProgress);
        return lReturn;
    }
    public int getGameStartScoreOffset(Player player) {
        int iGame = getNrOfFinishedGames();
        return getGameStartScoreOffset(player, iGame);
    }
    public int getGameStartScoreOffset(Player player, int iGame) {
        if ( ListUtil.size(m_startScoreOfPreviousGames) <= iGame ) {
            return MapUtil.getInt(m_startScoreOfGameInProgress, player, 0);
        }
        return MapUtil.getInt(m_startScoreOfPreviousGames.get(iGame), player, 0);
    }
    public int getNrOfPointsToWinGame() {
        return m_iNrOfPointsToWinGame;
    }
    public boolean setNrOfPointsToWinGame(int i) {
        if ( i != m_iNrOfPointsToWinGame ) {
            if ( gameHasStarted() ) {
                // it may all of sudden be gameball or even end of game score
                //Player[] gameBallForBefore = isPossibleGameBallFor();
                Player gameVictoryForBefore = isPossibleGameVictoryFor();
                m_iNrOfPointsToWinGame = i;
                setDirty(true);
                Player gameVictoryForAfter = isPossibleGameVictoryFor();
                Player[] gameBallForAfter  = isPossibleGameBallFor();
                if ( gameVictoryForAfter != null ) {
                    for(OnSpecialScoreChangeListener l: onSpecialScoreChangeListeners) {
                        l.OnGameEndReached(gameVictoryForAfter);
                    }
                }
            } else {
                m_iNrOfPointsToWinGame = i;
                setDirty(true);
            }
            return true;
        }
        return false;
        //Log.d(TAG, "game ending is now set to:" + m_iNrOfPointsToWinGame);
    }
    public boolean setNrOfGamesToWinMatch(int i) {
        if ( (i != m_iNrOfGamesToWinMatch) && (i != 0) ) {
            m_iNrOfGamesToWinMatch = i;
            if ( m_iTotalNrOfGamesToFinishForMatchToEnd != UNDEFINED_VALUE ) {
                m_iTotalNrOfGamesToFinishForMatchToEnd = 2 * m_iNrOfGamesToWinMatch - 1;
            }
            setDirty(true);
            return true;
        }
        return false;
    }
    public int getNrOfGamesToWinMatch() {
        return m_iNrOfGamesToWinMatch;
    }

    private void addNewGameScoreDetails() {
        if ( m_lScoreHistory == null ) {
            m_lScoreHistory = new ArrayList<>();
            m_lGameScoreHistory.add(m_lScoreHistory);
        } else if ( m_lScoreHistory.size() > 0 ) {
            m_lScoreHistory = new ArrayList<>();
            m_lGameScoreHistory.add(m_lScoreHistory);
        } else if ( m_lScoreHistory.size() == 0 ) {
            if ( m_lGameScoreHistory.contains(m_lScoreHistory) == false ) {
                m_lGameScoreHistory.add(m_lScoreHistory);
            }
        }
        //Log.d(TAG, "new GameScoreDetails: list of size " + m_lGameScoreHistory.size());
    }

    /** score of the specified player of the game in progress */
    public int getScore(Player player) {
        final int iScore = MapUtil.getInt(m_scoreOfGameInProgress, player, 0);
        return iScore;
    }
    /** maximum score of player A or B of the game in progress */
    public int getMaxScore() {
        return Math.max( this.getScore(Player.A), this.getScore(Player.B) );
    }
    /** minimum score of player A or B of the game in progress */
    public int getMinScore() {
        return Math.min( this.getScore(Player.A), this.getScore(Player.B) );
    }
    /** score difference between player A and B of the game in progress */
    public int getDiffScore() {
        return Math.abs( this.getScore(Player.A) - this.getScore(Player.B) );
    }
    /** score difference between passed player and opponent */
    int getDiffScore(Player p) {
        return this.getScore(p) - this.getScore(p.getOther());
    }
    Player getLeaderInCurrentGame() {
        int iDiff = getDiffScore();
        if ( iDiff == 0 ) {
            return null;
        }
        if ( getScore(Player.A) == getMaxScore() ) {
            return Player.A;
        } else {
            return Player.B;
        }
    }

    public List<ScoreLine> getScoreHistory() {
        return m_lScoreHistory;
    }

    public List<Map<Player, Integer>> getGameScoresIncludingInProgress() {
        List<Map<Player, Integer>> list = new ArrayList<Map<Player, Integer>>(m_endScoreOfPreviousGames);
        if ( m_scoreOfGameInProgress.get(Player.A) != m_startScoreOfGameInProgress.get(Player.A)
          || m_scoreOfGameInProgress.get(Player.B) != m_startScoreOfGameInProgress.get(Player.B)
           ) {
            list.add(m_scoreOfGameInProgress);
        }
        return list;
    }

    private void addGameScore(Map<Player, Integer> gameScore, boolean bNewStartScore) {
        //Log.d(TAG, "addGameScore: " + scores + " " + ListUtil.size(m_endScoreOfPreviousGames));
        Player winner = Util.getWinner(gameScore);
        m_lGameWinner.add(winner);

        m_player2GamesWon = new HashMap<Player, Integer>(m_player2GamesWon);
        MapUtil.increaseCounter(m_player2GamesWon, winner);
        m_lGameCountHistory.add(m_player2GamesWon);

        m_endScoreOfPreviousGames.add(gameScore);

        if ( m_HandicapFormat.equals(HandicapFormat.DifferentForAllGames) && bNewStartScore ) {
            m_startScoreOfPreviousGames.add(new HashMap<Player, Integer>(m_startScoreOfGameInProgress));
        }
    }

    /** holds array with history of games won like [0-0, 0-1, 1-1, 2-1] */
    public List<Map<Player, Integer>> getGameCountHistory() {
        List<Map<Player, Integer>> lReturn = new ArrayList<Map<Player, Integer>>();
        if ( ListUtil.isNotEmpty(m_lGameCountHistory) ) {
            for(Map<Player, Integer> mTmp: m_lGameCountHistory) {
                lReturn.add(new HashMap<Player, Integer>(mTmp));
            }
        }
        Player gameVictoryFor = isPossibleGameVictoryFor();
        if ( gameVictoryFor != null) {
            HashMap<Player, Integer> mTmp = new HashMap<Player, Integer>(m_player2GamesWon);
            MapUtil.increaseCounter(mTmp, gameVictoryFor);
            lReturn.add(mTmp);
        }
        return lReturn;
    }

    /** end scores of already ended games [ {A=11,B=9},{A=4,B=11}, {A=11, B=8} ] . So does not hold game in progress. */
    public List<Map<Player, Integer>> getEndScoreOfPreviousGames() {
        return m_endScoreOfPreviousGames;
    }

    /** similar to #getEndScoreOfGames() only if game in progress is also appears as actually finished it is included */
    public List<Map<Player, Integer>> getEndScoreOfGames() {
        List<Map<Player, Integer>> lReturn = new ArrayList<Map<Player, Integer>>();
        if ( ListUtil.isNotEmpty(m_endScoreOfPreviousGames) ) {
            for(Map<Player, Integer> mTmp: m_endScoreOfPreviousGames) {
                lReturn.add(new HashMap<Player, Integer>(mTmp));
            }
        }
        Player gameVictoryFor = isPossibleGameVictoryFor();
        if ( gameVictoryFor != null) {
            lReturn.add(new HashMap<Player, Integer>(m_scoreOfGameInProgress) );
        }
        return lReturn;
    }

    public Map<Player, Integer> getGamesWon() {
        return getGamesWon(true);
    }
    public Map<Player, Integer> getGamesWon(boolean bIncludeGameInprogress) {
        Map<Player, Integer> mGamesWon = new HashMap<Player, Integer>();
        mGamesWon.put(Player.A, 0);
        mGamesWon.put(Player.B, 0);
        for(Map<Player, Integer> mGameScore: m_endScoreOfPreviousGames) {
            if (mGameScore.get(Player.A) > mGameScore.get(Player.B)) {
                MapUtil.increaseCounter(mGamesWon, Player.A);
            } else {
                MapUtil.increaseCounter(mGamesWon, Player.B);
            }
        }
        // 20141025: added this logic for better display value in historical games
        if ( bIncludeGameInprogress ) {
            Player gameVictoryFor = isPossibleGameVictoryFor();
            if ( gameVictoryFor != null) {
                MapUtil.increaseCounter(mGamesWon, gameVictoryFor);
            }
        }
        return mGamesWon;
    }
    public Map<Player, Integer> getTotalNumberOfPointsScored() {
        return _getTotalNumberOfPointsScored(true);
    }
    private Map<Player, Integer> _getTotalNumberOfPointsScored(boolean bIncludeGameInProgress) {
        Map<Player, Integer> pointsWon = new HashMap<Player, Integer>();
        List<Map<Player, Integer>> gameEndScores = null;
        if (bIncludeGameInProgress) {
            gameEndScores = this.getGameScoresIncludingInProgress();
        } else {
            gameEndScores = this.getEndScoreOfPreviousGames();
        }
        for(Map<Player, Integer> mGameScore: gameEndScores) {
            Integer iA = mGameScore.get(Player.A);
            Integer iB = mGameScore.get(Player.B);
            MapUtil.increaseCounter(pointsWon, Player.A, iA);
            MapUtil.increaseCounter(pointsWon, Player.B, iB);
        }
        return pointsWon;
    }

    /** For drawing 'original' paper scoring. Contains all games including the one in progress */
    public List<List<ScoreLine>> getGameScoreHistory() {
        return m_lGameScoreHistory;
    }

    /** One-based */
    public int getGameNrInProgress() {
        return ListUtil.size(m_lGameScoreHistory);
    }
    public int getNrOfFinishedGames() {
        List<Map<Player, Integer>> endScoreOfGames = getEndScoreOfGames();
        return ListUtil.size(endScoreOfGames);
    }

    // -----------------------------------------
    // Tiebreak format
    // -----------------------------------------

    public boolean setTiebreakFormat(TieBreakFormat b) {
        if ( b.equals(m_TieBreakFormat) == false ) {
            setDirty(false);
            m_TieBreakFormat = b;
            return true;
        }
        return false;
    }
    public TieBreakFormat getTiebreakFormat() {
        return m_TieBreakFormat;
    }

    /** only for special tiebreaks */
    private int m_iTieBreakPlusX = 0;
    public void setTieBreakPlusX(int i) {
        m_iTieBreakPlusX = i;

        // recalculate game ball
        setDirty(true);
        isPossibleGameBallFor();
    }

    // -----------------------------------------
    // Nr of scores per server (tabletennis, racketlon)
    // -----------------------------------------

    int m_iNrOfServesPerPlayer = 2;
    public boolean setNrOfServesPerPlayer(int i) {
        if ( m_iNrOfServesPerPlayer != i ) {
            m_iNrOfServesPerPlayer = i;
            return true;
        }
        return false;
    }
    public int getNrOfServesPerPlayer() {
        return m_iNrOfServesPerPlayer;
    }

    // -----------------------------------------
    // Best of  vs Total of
    // -----------------------------------------

    /** used for both Squash and Racquetball and Badminton */
    public void setPlayAllGames(boolean b) {
        if ( b ) {
            m_iTotalNrOfGamesToFinishForMatchToEnd = m_iNrOfGamesToWinMatch * 2 - 1;
        } else {
            m_iTotalNrOfGamesToFinishForMatchToEnd = UNDEFINED_VALUE;
        }
    }
    public boolean playAllGames() {
        return m_iTotalNrOfGamesToFinishForMatchToEnd != UNDEFINED_VALUE;
    }
    // -----------------------------------------
    // English scoring/Hand-in-hand-out/HIHO
    // -----------------------------------------

    /** used for both Squash and Racquetball and Badminton */
    public void setEnglishScoring(boolean b) {
        m_bEnglishScoring = b;
    }
    public boolean isEnglishScoring() {
        return m_bEnglishScoring;
    }

    // -----------------------------------------
    // Handicap
    // -----------------------------------------

    public void setHandicapFormat(HandicapFormat b) {
        m_HandicapFormat = b;
    }
    public HandicapFormat getHandicapFormat() {
        return m_HandicapFormat;
    }
    public boolean isUsingHandicap() {
        return (m_HandicapFormat != null) && m_HandicapFormat.equals(HandicapFormat.None) == false;
    }

    //-------------------------------
    // Date/Time
    //-------------------------------

    public String getMatchDateYYYYMMDD_DASH() {
        return m_matchDate;
    }
    public long getDuration() {
        if ( ListUtil.size(m_lGameTimings) == 0 ) { return 0; }
        long lStart = m_lGameTimings.get(0).getStart();
        long lEnd = 0;
        if ( lStart == 0 ) {
            // manually entered game durations
            for (int gameNrZeroBased = 0; gameNrZeroBased < ListUtil.size(m_lGameTimings); gameNrZeroBased++) {
                GameTiming gt = m_lGameTimings.get(gameNrZeroBased);
                long lDuration = gt.getEnd() - gt.getStart();
                if ( gt.getStart() > 0 || lDuration > 100 || lDuration == 0 ) {
                    // mixup of gametimings: actual gametimings after demo game timing
                    lDuration = 6;
                    gt = new GameTiming(gameNrZeroBased, 0, lDuration);
                    m_lGameTimings.set(gameNrZeroBased, gt);
                }
                lEnd += lDuration;
            }
            lEnd += 2 * (m_lGameTimings.size() - 1); // pauses between games (guess 2 minutes)
            // lEnd is now in minutes in demo mode, return in milliseconds
            lEnd = (lEnd * 1000 * 60) + (5 * 1000 /* add 5 seconds to NOT have a nicely rounded nr of minutes */ );
        } else {
            lEnd = ListUtil.getLast(m_lGameTimings).getEnd();
        }
        return lEnd - lStart;
    }
    public int getDurationInMinutes() {
        if ( ListUtil.size(m_lGameTimings) == 0 ) { return 0; }
        long lStart = m_lGameTimings.get(0).getStart();
        long lDuration = getDuration();
        GameTiming gameTiming = new GameTiming(-1, lStart, lStart + lDuration, onTimingChangedListeners);
        return gameTiming.getDurationMM();
    }
    public String getMatchStartTimeHHMMSSXXX() {
        return m_matchTime;
    }
    public String getMatchStartTimeHH_Colon_MM() {
        String matchStartTimeHHMMSSXXX = getMatchStartTimeHHMMSSXXX();
        if ( StringUtil.isEmpty(matchStartTimeHHMMSSXXX) ) {
            return "";
        }
        String sTimeHHMMSSXXX = matchStartTimeHHMMSSXXX.replaceFirst("^([0-2][0-9])(:)?([0-5][0-9]).*", "$1:$3");
        return removeTimezone(sTimeHHMMSSXXX);
    }
    public long getMatchStart() {
        if ( ListUtil.size(m_lGameTimings) == 0 ) {
            // fall back
            Date d = DateUtil.parseString2Date(m_matchDate + "T" + m_matchTime, jsonTimeFormat);
            if ( d == null ) {
                d = DateUtil.parseString2Date(m_matchDate, DateUtil.YYYY_MM_DD);
            }
            if ( d == null ) {
                Log.w(TAG, "Could not parse to a date " + m_matchDate + m_matchTime);
                return System.currentTimeMillis() - 1000 * 60 * 30; // 30 minutes ago??
            }
            m_gameTimingCurrent = new GameTiming(0, d.getTime(), d.getTime(), null); // do not pass listeners here, They might get called ... calling this method again
            m_lGameTimings.add(m_gameTimingCurrent);
            m_gameTimingCurrent.setTimingListeners(onTimingChangedListeners); // only set listeners now
            return m_gameTimingCurrent.getStart();
        }
        return m_lGameTimings.get(0).getStart();
    }
    public long getLastGameStart() {
        if ( ListUtil.size(m_lGameTimings) == 0 ) { return getMatchStart(); }
        return ListUtil.getLast(m_lGameTimings).getStart();
    }
    public long getMatchEnd() {
        GameTiming last = ListUtil.getLast(m_lGameTimings);
        if ( last == null ) { return 0; }
        return last.getEnd();
    }
    public Date getMatchDate() {
        Date dReturn = null;
        try {
            if ( StringUtil.isNotEmpty(m_matchTime) ) {
                dReturn = DateUtil.parseString2Date(m_matchDate + "T" + m_matchTime, jsonTimeFormat);
            } else {
                dReturn = DateUtil.parseString2Date(m_matchDate, DateUtil.YYYY_MM_DD);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if ( dReturn == null ) {
            Log.w(TAG, "Could not parse date and time : " + m_matchDate + " " + m_matchTime);
            // prevent returning null
            return new Date();
        }
        return dReturn;
    }

    private int I_NR_OF_SECS_CORRECTION = 30; // TODO: dynamically based on stats of match in progress and/or previous matches?

    /** Adjust when.date and/or when.time of the match if appropriate */
    private void adjustTheWhenObjectIfAppropriate(GameTiming.ChangedBy changedBy) {
        long lModelInitialized = m_gameTimingCurrent.getStart();
        int  iMinutesPassed    = DateUtil.convertToMinutes(System.currentTimeMillis() - lModelInitialized);
        if ( iMinutesPassed > 2 ) {
            // match was selected/set up long before actual start
            m_matchDate = DateUtil.getCurrentYYYY_MM_DD();
            long lStart = m_gameTimingCurrent.updateStart(-1 * I_NR_OF_SECS_CORRECTION, changedBy); // act as if the match started 30 seconds ago
            m_matchTime = DateUtil.formatDate2String(lStart, DateUtil.HHMMSSXXX_COLON);
        }
    }

    //-------------------------------
    // Source
    //-------------------------------

    private String m_sSource   = "";
    private String m_sSourceID = "";
    private String m_sAdditionalPostParams = "";

    public String getSource() {
        return m_sSource;
    }
    public String getSourceID() {
        return m_sSourceID;
    }
    public void setSource(String sSource, String sSourceID) {
        if ( sSource != null ) {
            m_sSource   = sSource;
        }
        if ( sSourceID != null ) {
            m_sSourceID = sSourceID;
        }
    }

    public String getAdditionalPostParams() {
        return m_sAdditionalPostParams;
    }
    public void setAdditionalPostParams(String s) {
        m_sAdditionalPostParams = s;
    }

    //-------------------------------
    // Player/Color/Country/Club
    //-------------------------------

    public boolean setPlayerColor(Player player, String sColor) {
        String sPrevious = m_player2Color.put(player, sColor);
        if ( (sColor != null && sPrevious == null)
          || (sColor == null && sPrevious != null)
          || (sColor != null && sColor.equals(sPrevious) == false)
           ) {
            setDirty(false);
            for(OnPlayerChangeListener listener: onPlayerChangeListeners) {
                listener.OnColorChange(player, sColor, sPrevious);
            }
            return true;
        }
        return false;
    }
    public String getColor(Player player) {
        String sColor = m_player2Color.get(player);
        return sColor;
    }
    public String getCountry(Player player) {
        String sCountry = m_player2Country.get(player);
        return sCountry;
    }
    public String[] getCountries(Player[] players) {
        int i = 0;
        String[] saReturn = new String[2];
        for(Player player: players) {
            saReturn[i++] = getCountry(player);
        }
        return saReturn;
    }

    public String getClub(Player player) {
        String sClub = m_player2Club.get(player);
        return sClub;
    }
    public String[] getClubs() {
        int i = 0;
        String[] saReturn = new String[2];
        for(Player player: getPlayers()) {
            saReturn[i++] = getClub(player);
        }
        return saReturn;
    }
    public String getAvatar(Player player) {
        String sAvatar = m_player2Avatar.get(player);
        return sAvatar;
    }

    private static final Player[] m_players = new Player[] {Player.A, Player.B};
    private static final Player[] m_noneOfPlayers = new Player[]{};
    public static Player[] getPlayers() {
        return m_players;
    }
           static Player[] getNoneOfPlayers() {
        return m_noneOfPlayers;
    }
    public String getName(Player player) {
        return getName(player, false, false);
    }
    public String getName(Player player, boolean bKeepSeeding, boolean bKeepCountry) {
        if ( player == null ) { return null; }
        String sName = m_player2Name.get(player);
        if ( bKeepSeeding == false ) {
            sName = Util.removeSeeding(sName);
        }
        if ( bKeepCountry == false ) {
            sName = Util.removeCountry(sName);
        } else {
            String sCountry = getCountry(player);
            if ( StringUtil.isNotEmpty(sCountry) ) {
                sName += " (" + sCountry + ")";
            }
        }
        return sName;
    }

    public String getName_no_nbsp(Player player, boolean bKeepCountry) {
        String name = getName(player, false, bKeepCountry);
        if ( name == null ) { return null; }
        return name.replace(" ", "\u00A0");
    }
    public String[] getPlayerNames(boolean bKeepSeeding, boolean bKeepCountry) {
        return getPlayerNames(bKeepSeeding, bKeepCountry, getPlayers());
    }
    public String[] getPlayerNames(boolean bKeepSeeding, boolean bKeepCountry, Player[] players) {
        int i = 0;
        String[] saReturn = new String[2];
        for(Player player: players) {
            saReturn[i++] = getName(player, bKeepSeeding, bKeepCountry);
        }
        return saReturn;
    }

    public boolean swapDoublesPlayerNames(Player pl) {
        String sPlayerNames = this.getName(pl);
        String[] saNames = sPlayerNames.split("/");
        if ( saNames.length != 2 ) { return false; }
        this.setPlayerName(pl, saNames[1] + "/" + saNames[0]);
        return true;
    }

    public static final String REGEXP_SPLIT_DOUBLES_NAMES = "/\\s*(?![0-9])";
    /** return names of single team in alphabetical order */
    public String[] getDoublePlayerNames(Player p) {
        String sName = getName(p);
        String[] saNames = sName.split(REGEXP_SPLIT_DOUBLES_NAMES);
        Arrays.sort(saNames);
        return saNames;
    }
    public void setPlayerNames(String sNameA, String sNameB) {
        setPlayerName(Player.A, sNameA);
        setPlayerName(Player.B, sNameB);
    }
    public boolean setPlayerName(Player p, String sName) {
        if ( sName == null ) {
            sName = "";
        }
        String[] saNames = sName.split(REGEXP_SPLIT_DOUBLES_NAMES);
        m_bIsDouble = saNames.length == 2;
        if ( isDoubles() && m_doubleServeSequence.equals(DoublesServeSequence.NA) ) {
            DoublesServeSequence dssDefault = getDoubleServeSequence(0);
            _setDoublesServeSequence(dssDefault);
        }
        sName = sName.trim();
        String sPrevious = m_player2Name.put(p, sName);
        if ( sName.equals(sPrevious) == false ) {
            setDirty(false);
            for(OnPlayerChangeListener listener: onPlayerChangeListeners) {
                listener.OnNameChange(p, sName, getCountry(p), getAvatar(p), getClub(p), isDoubles());
            }
            return true;
        }
        return false;
    }
    public boolean setPlayerCountry(Player player, String sCountry) {
        if ( StringUtil.isNotEmpty(sCountry) ) {
            sCountry = sCountry.trim().toUpperCase();
        } else {
            sCountry = "";
        }
        String sPrevious = m_player2Country.put(player, sCountry);
        if ( sCountry.equals(sPrevious) == false ) {
            setDirty(false);
            for(OnPlayerChangeListener listener: onPlayerChangeListeners) {
                listener.OnCountryChange(player, sCountry);
            }
            return true;
        }
        return false;
    }
    public void setPlayerId(Player player, String sId) {
        m_player2Id.put(player, sId);
    }
    /**
     * Will only return something if it was e.g. a team match was selected from a feed, and ref did select actual players from a list of team_players.
     * This will 'fail' if player name was modified afterwards...
     */
    public String getPlayerId(Player player) {
        return m_player2Id.get(player);
    }

    public boolean setPlayerClub(Player player, String sClub) {
        if ( StringUtil.isNotEmpty(sClub) ) {
            sClub = sClub.trim();
        } else {
            sClub = "";
        }
        String sPrevious = m_player2Club.put(player, sClub);
        if ( sClub.equals(sPrevious) == false ) {
            setDirty(false);
            for(OnPlayerChangeListener listener: onPlayerChangeListeners) {
                listener.OnClubChange(player, sClub);
            }
            return true;
        }
        return false;
    }

    public boolean setPlayerAvatar(Player player, String sAvatar) {
        if ( StringUtil.isNotEmpty(sAvatar) ) {
            sAvatar = sAvatar.trim();
        } else {
            sAvatar = "";
        }
        String sPrevious = m_player2Avatar.put(player, sAvatar);
        if ( sAvatar.equals(sPrevious) == false ) {
            setDirty(false);
            for(OnPlayerChangeListener listener: onPlayerChangeListeners) {
                listener.OnAvatarChange(player, sAvatar);
            }
            return true;
        }
        return false;
    }

    //-------------------------------
    // Marker
    //-------------------------------

    private String m_sReferee = "";
    private String m_sMarker  = "";
    public boolean setReferees(String sName, String sMarker) {
        if ( sName   == null ) { sName   = ""; }
        if ( sMarker == null ) { sMarker = ""; }
        sName  = sName.trim();
        boolean bNameChanged = m_sReferee.equals(sName)  == false;
                bNameChanged = bNameChanged || m_sMarker.equals(sMarker)  == false;
        m_sReferee = sName;
        m_sMarker  = sMarker;
        if ( bNameChanged ) {
            setDirty(false);
        }
        return bNameChanged;
    }
    public String getReferee() { return m_sReferee; }
    public String getMarker() { return m_sMarker; }

    //-------------------------------
    // Court
    //-------------------------------

    private String m_sCourt  = "";
    public boolean setCourt(String sCourt) {
        if ( sCourt == null ) { sCourt   = ""; }
        sCourt = sCourt.trim();
        boolean bCourtChanged = m_sCourt.equals(sCourt) == false;
        if ( bCourtChanged ) {
            m_sCourt = sCourt;
            setDirty(false);
        }
        return bCourtChanged;
    }
    public String getCourt() { return m_sCourt; }

    //-------------------------------
    // Event
    //-------------------------------

    private String m_sEventName     = "";
    private String m_sEventDivision = "";
    private String m_sEventRound    = "";
    private String m_sEventLocation = "";
    public boolean setEvent(String sName, String sDivision, String sRound, String sLocation) {
        if ( sName     == null ) { sName     = ""; }
        if ( sDivision == null ) { sDivision = ""; }
        if ( sRound    == null ) { sRound    = ""; }
        if ( sLocation == null ) { sLocation = ""; }
        sName     = sName    .trim();
        sDivision = sDivision.trim();
        sRound    = sRound   .trim();
        sLocation = sLocation.trim();
        boolean bNameChanged     = m_sEventName    .equals(sName)     == false;
        boolean bDivisionChanged = m_sEventDivision.equals(sDivision) == false;
        boolean bRoundChanged    = m_sEventRound   .equals(sRound)    == false;
        boolean bLocChanged      = m_sEventLocation.equals(sLocation) == false;
        m_sEventName     = sName;
        m_sEventDivision = sDivision;
        m_sEventRound    = sRound;
        m_sEventLocation = sLocation;
        final boolean bChanged = bNameChanged || bDivisionChanged || bRoundChanged || bLocChanged;
        if ( bChanged ) {
            setDirty(false);
        }
        return bChanged;
    }
    public String getEventLocation() { return m_sEventLocation; }
    public String getEventName    () { return m_sEventName; }
    public String getEventDivision() { return m_sEventDivision; }
    public String getEventRound   () { return m_sEventRound; }
    //-------------------- CONTROLLER ----

    public void lockIfUnchangedFor(int iMinNrOfMinutesUnchanged) {
        if ( isLocked() ) {
            // already locked... don't change to LockedIdleTime if already locked
            return;
        }
        final long iNrOfMinutesUnchanged = DateUtil.convertToMinutes(System.currentTimeMillis() - m_tsLastJsonOperation);
        if ( (iNrOfMinutesUnchanged > iMinNrOfMinutesUnchanged) && hasStarted() ) {
            setLockState(LockState.LockedIdleTime);
        }
    }

    //-------------------------------
    // JSON
    //-------------------------------

    private static String jsonTimeFormat_Old = DateUtil.YYYYMMDD_HHMMSS; /* up to apk 202 */
    private static String jsonTimeFormat = DateUtil.YYYYMMDD_HHMMSSXXX_DASH_T_COLON;

    /** hold timestamp that we compare to, to see if match is unchanged for x minutes */
    private long m_tsLastJsonOperation = 0L;

    private String m_shareUrl = null;
    public String getShareURL() {
        return m_shareUrl;
    }
    public void setShareURL(String url) {
        if ( StringUtil.isNotEmpty(url) ) {
            Log.d(TAG, "Share URL         : " + url);
        } else if (StringUtil.isNotEmpty(m_shareUrl)) {
            Log.w(TAG, "Share URL emptied : " + url);
        }
        m_shareUrl = url;
    }

    public File getStoreAs(File fParentDir) {
        String matchStartTimeHHMMSSXXX = getMatchStartTimeHHMMSSXXX();
        String sTimeHHMMSS =null;
        if (matchStartTimeHHMMSSXXX != null) {
            sTimeHHMMSS = removeTimezone(matchStartTimeHHMMSSXXX);
        }
        String sDateName = m_matchDate.replace("-", "") + "." + (StringUtil.isNotEmpty(sTimeHHMMSS)?(sTimeHHMMSS + "."):"") + getName(Player.A) + "-" + getName(Player.B);
        sDateName = sDateName.replaceAll("[^A-za-z0-9\\-\\.]", "");
        return new File(fParentDir, sDateName + ".sb");
    }

    private String removeTimezone(String sTimeHHMMSSXXX) {
        if ( sTimeHHMMSSXXX == null ) { return null; }
        return sTimeHHMMSSXXX.replaceAll("[+-]\\d\\d:\\d\\d$", "");
    }

    public boolean fromJsonString(File f) {
        if ( (f == null) || (f.exists() == false) ) {
            // normally only when switching brand in DEMO mode
            Log.w(TAG, "Not an existing file : " + f);
            return false;
        }
        String     sJson = null;
        try {
            sJson = FileUtil.readFileAsString(f);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        JSONObject jo    = fromJsonString(sJson, false);
        m_tsLastJsonOperation = f.lastModified();
        return (jo != null);
    }

    /** used for 'adjusting' the score. E.g. starting halfway a match or for demo purposes */
    public void setGameScore_Json(final int iGameZB, final int iPointsA, final int iPointsB, int iGameDuration) {
        boolean bIsSquash         = getSport().equals(SportType.Squash);
        boolean bDummyServeSides  = iGameDuration!=0 && bIsSquash;
        boolean bAddRandomAppeals = bDummyServeSides && bIsSquash; // e.g. no random appeals for racketlon demo
        boolean bAddDeviations    = bDummyServeSides;

        List<ScoreLine> scoreLines;
        int pA, pB;
        if ( getGameNrInProgress() > iGameZB ) {
            // discard part of the already entered score
            scoreLines = m_lGameScoreHistory.get(iGameZB);
            if ( ListUtil.size(m_endScoreOfPreviousGames) > iGameZB ) {
                Map<Player, Integer> mScoreOfGameWeModify = m_endScoreOfPreviousGames.get(iGameZB);
                pA = mScoreOfGameWeModify.get(Player.A);
                pB = mScoreOfGameWeModify.get(Player.B);
            } else {
                pA = m_scoreOfGameInProgress.get(Player.A);
                pB = m_scoreOfGameInProgress.get(Player.B);
            }
            if ( pA > iPointsA || pB > iPointsB ) {
                scoreLines = new ArrayList<ScoreLine>();
                pA = 0;
                pB = 0;
            }
        } else {
            // enter score of a new game
            scoreLines = new ArrayList<ScoreLine>();
            pA = 0;
            pB = 0;
        }

        ServeSide ssA = bDummyServeSides? (getServer().equals(Player.A)? m_player2LastServeSide.get(Player.A):null) : null;
        ServeSide ssB = bDummyServeSides? (getServer().equals(Player.B)? m_player2LastServeSide.get(Player.B):null) : null;
        if ( bIsSquash == false ) {
            // follow 'strict' serve side rules for racketlon/tabletennis
        }

        while ( pA < iPointsA  || pB < iPointsB ) {
            float percentageA = (float) pA / iPointsA;
            float percentageB = (float) pB / iPointsB;
            Player pAddTo = (percentageA < percentageB) ? Player.A : Player.B;
            if ( Math.max(iPointsA, iPointsB) > getNrOfPointsToWinGame() ) {
                if ( pB == pA + 1 && pA < iPointsA ) {
                    pAddTo = Player.A;
                }
                if ( pA == pB + 1 && pB < iPointsB ) {
                    pAddTo = Player.B;
                }
            }

            if ( bAddDeviations && Math.max(pA, pB) < getNrOfPointsToWinGame() - 1 && pA < iPointsA && pB < iPointsB ) {
                // add score to player closer to finish to have some randomness
                int i = (int) Math.round(Math.random() * 10);
                if ( i < 4 ) {
                    pAddTo = pAddTo.getOther();
                }
            }

            ScoreLine slAppeal = null;
            if ( bAddRandomAppeals ) {
                int i = (int) Math.round(Math.random() * 10);
                if ( i < 3 ) {
                    Call call = Call.values()[i];
                    switch (call) {
                        case NL:
                            slAppeal = new ScoreLine(pAddTo.getOther(), call);
                            break;
                        case YL:
                            slAppeal = new ScoreLine(pAddTo, call);
                            break;
                        case ST:
                            slAppeal = new ScoreLine(pAddTo, call);
                            break;
                    }
                    scoreLines.add(slAppeal);
                }
            }
            if ( pAddTo.equals(Player.A) ) {
                pA++;
                scoreLines.add(new ScoreLine(ssA, pA, ssB, null));
                if ( bDummyServeSides ) {
                    ssA = ssA == null? ServeSide.R: ssA.getOther();
                    ssB = null;
                }
            } else {
                pB++;
                scoreLines.add(new ScoreLine(ssA, null, ssB, pB));
                if ( bDummyServeSides ) {
                    ssB = ssB == null? ServeSide.R: ssB.getOther();
                    ssA = null;
                }
            }
        }
        if ( iGameDuration != 0 ) {
            GameTiming gameTiming = new GameTiming(iGameZB, 0, iGameDuration, onTimingChangedListeners); // zero is used to indicate it was not really live timing in milliseconds
            if ( ListUtil.size(m_lGameTimings) > iGameZB ) {
                m_lGameTimings.set(iGameZB, gameTiming);
            } else {
                m_lGameTimings.add(gameTiming);
            }
        }

/*
        int min = Math.min(iPointsA, iPointsB);
        int max = Math.max(iPointsA, iPointsB);

        // add dummy score lines scoring alternating until minimum of points is reached
        for(int p=1; p<= min; p++) {
            if ( iPointsA >= iPointsB ) {
                scoreLines.add(new ScoreLine(ssA, p   , null, null));
                scoreLines.add(new ScoreLine(null, null, ssB, p));
            } else {
                scoreLines.add(new ScoreLine(null, null, ssB, p));
                scoreLines.add(new ScoreLine(ssA, p   , null, null));
            }
        }
        // add dummy score lines for 'leader' until maximum of points is reached
        for(int p=min+1; p<= max; p++) {
            if ( iPointsA > iPointsB ) {
                scoreLines.add(new ScoreLine(null, p   , null, null));
            } else {
                scoreLines.add(new ScoreLine(null, null, null, p));
            }
            if ( bDummyServeSides ) {
                ssA = ssA.getOther();
                ssB = ssB.getOther();
            }
        }
*/
        if ( iGameZB < getGameNrInProgress() ) {
            m_lGameScoreHistory.set(iGameZB, scoreLines);
        } else {
            m_lGameScoreHistory.add(scoreLines);
        }
        String sJson = toJsonString();
        this.fromJsonString(sJson, false);
        setDirty(true);

        for(OnComplexChangeListener l:onComplexChangeListeners) {
            l.OnChanged();
        }
    }

    public JSONObject fromJsonString(String sJson) {
        return fromJsonString(sJson, false);
    }
    public JSONObject fromJsonString(String sJson, boolean bStopAfterEventNamesDateTimeResult) {
        try {
            init();
            setDirty(true); // mainly so that matchball/gameball is recalculated

            JSONObject joMatch = new JSONObject(sJson);

            if ( joMatch.has(JSONKey.event.toString()) ) {
                JSONObject joEvent = joMatch.getJSONObject(JSONKey.event.toString());
                m_sEventName     = joEvent.optString(JSONKey.name    .toString());
                m_sEventDivision = joEvent.optString(JSONKey.division.toString());
                m_sEventRound    = joEvent.optString(JSONKey.round   .toString());
                m_sEventLocation = joEvent.optString(JSONKey.location.toString());
                setEvent(m_sEventName, m_sEventDivision, m_sEventRound, m_sEventLocation); // just for trimming (and setting it to dirty if if trimming was required)
            }

            if ( bStopAfterEventNamesDateTimeResult && joMatch.has(JSONKey.result.toString())) {
                m_sResultFast = joMatch.getString(JSONKey.result.toString());
            }

            // read referee
            JSONObject oRef = joMatch.optJSONObject(JSONKey.referee.toString());
            if ( JsonUtil.isNotEmpty(oRef) ) {
                setReferees(oRef.optString(JSONKey.name.toString()), oRef.optString(JSONKey.markers.toString()));
            }
            setCourt(joMatch.optString(JSONKey.court.toString()));

            // read players
            JSONObject joPlayers = joMatch.optJSONObject(JSONKey.players.toString());
            if ( JsonUtil.isEmpty(joPlayers) ) { return null; } // should not be possible, but we might have imported a corrupt file into our 'stored matches'
            for(Player p: getPlayers() ) {
                String sPlayer = joPlayers.optString(p.toString(), ""); // allow one of players to be empty for communication match details by having only selected single player
                m_player2Name.put(p, sPlayer);
                setPlayerName(p, sPlayer); // just for trimming (and setting it to dirty if if trimming was required)
            }
            // read date
            int iDeviatingDateFormat = 0;
            JSONObject joWhen = joMatch; // old
            if ( joMatch.has(JSONKey.when.toString() ) ) {
                // new in 3.17
                joWhen = joMatch.getJSONObject(JSONKey.when.toString());
            }
            m_matchDate = joWhen.optString(JSONKey.date.toString(), m_matchDate);
            if ( StringUtil.size(m_matchDate) == 8 && m_matchDate.matches("^\\d{8}$")) {
                // up to 202
                m_matchDate = m_matchDate.substring(0,4) + "-" + m_matchDate.substring(4,6) + "-" + m_matchDate.substring(6,8);
            }
            if ( joWhen.has(JSONKey.time.toString()) ) {
                m_matchTime = joWhen.getString(JSONKey.time.toString());
                if ( StringUtil.size(m_matchTime) == 4 ) {
                    m_matchTime += "00"; // add seconds 3.19-4.15 uses HHMMSS format
                    iDeviatingDateFormat+=1;
                }
                if ( StringUtil.size(m_matchTime) == 5 && m_matchTime.matches("^\\d{2}:\\d{2}$") ) {
                    m_matchTime += ":00";
                    iDeviatingDateFormat+=2;
                }
                if ( StringUtil.size(m_matchTime) == 6  && m_matchTime.matches("^\\d{6}$") ) {
                    // up to 202 it was HHMMSS, now add colons
                    m_matchTime = m_matchTime.substring(0,2) + ":" + m_matchTime.substring(2,4) + ":" + m_matchTime.substring(4,6);
                    iDeviatingDateFormat+=4;
                }
                if ( StringUtil.size(m_matchTime) == 8 ) {
                    // add time zone if missing
                    m_matchTime = m_matchTime + DateUtil.getTimezoneXXX();
                    iDeviatingDateFormat+=8;
                }
            } else {
                m_matchTime = "";
            }

            String sSport = joMatch.optString(JSONKey.sport.toString(), SportType.Squash.toString());
            if ( sSport.equals(getSport().toString()) == false ) {
                // model was not created for/with current sport. Better not open it. Might screw up the data
                return null;
            }

            // if only name date and time of the match will be consulted (historical games)
            if ( bStopAfterEventNamesDateTimeResult ) {
                return joMatch;
            }

            boolean bMatchFormatIsSet = false;
            try {
                // read match format
                JSONObject joFormat = joMatch.getJSONObject(JSONKey.format.toString());
                int numberOfPointsToWinGame = joFormat.getInt(JSONKey.numberOfPointsToWinGame.toString());
                int nrOfGamesToWinMatch     = joFormat.optInt(JSONKey.nrOfGamesToWinMatch    .toString()); // old... keep for now for stored matches
                    nrOfGamesToWinMatch     = joFormat.optInt(JSONKey.numberOfGamesToWinMatch.toString(), nrOfGamesToWinMatch); // optional for e.g. racketlon
                setNrOfPointsToWinGame(numberOfPointsToWinGame);
                if ( nrOfGamesToWinMatch > 0 ) {
                    setNrOfGamesToWinMatch(nrOfGamesToWinMatch);
                }
                if ( joFormat.optBoolean(JSONKey.playAllGames.toString()) ) {
                    setPlayAllGames(true);
                }

                bMatchFormatIsSet = true;
                if ( joFormat.has(JSONKey.useHandInHandOutScoring.toString()) ) {
                    // if not specified it may still default to true e.g. for Racquetball. So do not shorten this with using optBoolean()
                    boolean b = joFormat.getBoolean(JSONKey.useHandInHandOutScoring.toString());
                    setEnglishScoring(b);
                }
                String s = joFormat.optString(JSONKey.tiebreakFormat.toString());
                if ( StringUtil.isNotEmpty(s) ) {
                    setTiebreakFormat(TieBreakFormat.valueOf(s));
                }
                if ( joFormat.has(JSONKey.doublesServeSequence.toString()) ) {
                    String ss = joFormat.getString(JSONKey.doublesServeSequence.toString());
                    DoublesServeSequence dsq;
                    if ( Model.mOldDSS2New.containsKey(ss) ) {
                        dsq = Model.mOldDSS2New.get(ss);
                        Log.w(TAG, String.format("Translated old %s to new %s", ss, dsq));
                    } else {
                        dsq = DoublesServeSequence.valueOf(ss);
                    }
                    _setDoublesServeSequence(dsq);
                }

                String h = joFormat.optString(JSONKey.handicapFormat.toString());
                if ( StringUtil.isNotEmpty(h) ) {
                    setHandicapFormat(HandicapFormat.valueOf(h));
                    JSONArray joOffset = joFormat.optJSONArray(JSONKey.gameStartScoreOffset.toString());
                    for(int g=0; g < JsonUtil.size(joOffset); g++) {
                        JSONObject joGame;
                        try {
                            joGame = joOffset.getJSONObject(g);
                        } catch (Exception e) {
                            // this should no longer be necc
                            joGame = new JSONObject(joOffset.optString(g)) ;
                        }
                        if ( g > 0 && m_startScoreOfGameInProgress != null) {
                            m_startScoreOfPreviousGames.add(m_startScoreOfGameInProgress);
                        }
                        m_startScoreOfGameInProgress = new HashMap<Player, Integer>();
                        m_startScoreOfGameInProgress.put(Player.A, joGame.optInt(Player.A.toString(), 0));
                        m_startScoreOfGameInProgress.put(Player.B, joGame.optInt(Player.B.toString(), 0));
                    }
                }

                // read ids
                JSONObject joPlayerIds = joMatch.optJSONObject(JSONKey.playerids.toString());
                if ( JsonUtil.isNotEmpty(joPlayerIds) ) {
                    for (Player p : getPlayers()) {
                        if ( joPlayerIds.has(p.toString()) ) {
                            String sPlayerId = joPlayerIds.getString(p.toString());
                            m_player2Id.put(p, sPlayerId);
                        }
                    }
                }
                // read colors
                JSONObject joColors = joMatch.optJSONObject(JSONKey.colors.toString());
                if ( JsonUtil.isNotEmpty(joColors) ) {
                    for (Player p : getPlayers()) {
                        if ( joColors.has(p.toString()) ) {
                            String sColor = joColors.getString(p.toString());
                            m_player2Color.put(p, sColor);
                        }
                    }
                }
                // read countries
                JSONObject joCountries = joMatch.optJSONObject(JSONKey.countries.toString());
                if ( JsonUtil.isNotEmpty(joCountries) ) {
                    for (Player p : getPlayers()) {
                        if ( joCountries.has(p.toString()) ) {
                            String sCountryCode = joCountries.getString(p.toString());
                            m_player2Country.put(p, sCountryCode.toUpperCase());
                        }
                    }
                }
                // read clubs
                JSONObject joClubs = joMatch.optJSONObject(JSONKey.clubs.toString());
                if ( JsonUtil.isNotEmpty(joClubs) ) {
                    for (Player p : getPlayers()) {
                        if ( joClubs.has(p.toString()) ) {
                            String sClub = joClubs.getString(p.toString());
                            m_player2Club.put(p, sClub);
                        }
                    }
                }
                // read avatars
                JSONObject joAvatars = joMatch.optJSONObject(JSONKey.avatars.toString());
                if ( JsonUtil.isNotEmpty(joAvatars) ) {
                    for (Player p : getPlayers()) {
                        if ( joAvatars.has(p.toString()) ) {
                            String sAvatar = joAvatars.getString(p.toString());
                            m_player2Avatar.put(p, sAvatar);
                        }
                    }
                }
                if ( joFormat.has(JSONKey.mode.toString())) {
                    m_sMode = joFormat.optString(JSONKey.mode.toString());
                }
            } catch (Exception e) {
                // not really an error, most likely last match stored did not use these keys
                e.printStackTrace();
            }


            ScoreLine scoreLine = null;
            JSONArray games = joMatch.optJSONArray(JSONKey.score.toString());
            if ( JsonUtil.isNotEmpty(games) ) {
                for(int g=0; g < games.length(); g++) {
                    // add previous game scores to history
                    if ( (g!=0) && ( m_scoreOfGameInProgress != null ) && ( m_scoreOfGameInProgress.size() != 0 ) ) {
                        int max = Math.max(MapUtil.getInt(m_scoreOfGameInProgress, Player.A, 0), MapUtil.getInt(m_scoreOfGameInProgress, Player.B, 0));
                        boolean bGameIsStarted;
                        if ( isUsingHandicap() ) {
                            Map<Player, Integer> startScore;
                            if (g < ListUtil.size(m_startScoreOfPreviousGames) ) {
                                startScore = m_startScoreOfPreviousGames.get(g);
                            } else {
                                startScore = m_startScoreOfGameInProgress;
                            }
                            bGameIsStarted = m_scoreOfGameInProgress.get(Player.A)!= startScore.get(Player.A)
                                          || m_scoreOfGameInProgress.get(Player.B)!= startScore.get(Player.B);
                        } else {
                            bGameIsStarted = max != 0;
                        }
                        if (bGameIsStarted) {
                            addGameScore(m_scoreOfGameInProgress, false);

                            if ( bMatchFormatIsSet == false ) {
                                if (getNrOfFinishedGames() == 1) {
                                    setNrOfPointsToWinGame(max);
                                } else {
                                    setNrOfPointsToWinGame(Math.min(m_iNrOfPointsToWinGame, max));
                                }
                            }
                            if ( max > getNrOfPointsToWinGame() ) {
                                m_iNrOfTiebreaks++;
                            }
                        }
                    }

                    // initialize for new game
                    addNewGameScoreDetails();

                    m_scoreOfGameInProgress = new HashMap<Player, Integer>();
                    m_scoreOfGameInProgress.put(Player.A, MapUtil.getInt(m_startScoreOfGameInProgress, Player.A, 0));
                    m_scoreOfGameInProgress.put(Player.B, MapUtil.getInt(m_startScoreOfGameInProgress, Player.B, 0));

                    JSONArray game = games.getJSONArray(g);
                    ScoreLine scoreLinePrev = null;
                    for ( int i=0; i < game.length(); i++ ) {
                        String sScoreLine = game.getString(i);
                        scoreLinePrev = scoreLine;
                        scoreLine = new ScoreLine(sScoreLine);
                        boolean bPlayerCouldChooseServeSide =
                                   scoreLinePrev == null                // start of set
                                || scoreLinePrev.isHandout(getSport()); // previous point was handout
                        if ( bPlayerCouldChooseServeSide ) {
                            // player had free choice of where to start. Remember where he started
                            Player    servingPlayer = scoreLine.getServingPlayer();
                            ServeSide serveSide     = scoreLine.getServeSide();
                            if ( servingPlayer != null && serveSide != null ) {
                                m_player2LastServeSide.put(servingPlayer, serveSide);

                                Map<ServeSide, Integer> serveSideCount = m_player2ServeSideCount.get(servingPlayer);
                                MapUtil.increaseCounter(serveSideCount, serveSide);
                            }
                        }
                        addScoreLine(scoreLine, false);

                      //MapUtil.increaseCounter(m_scoreOfGameInProgress, scoreLine.getScoringPlayer()); // this does not work if we 'adjust' the score
                        if ( scoreLine.isCall() == false && scoreLine.isBrokenEquipment() == false ) {
                            m_scoreOfGameInProgress.put(scoreLine.getScoringPlayer(), scoreLine.getScore());
                        }
                    }
                }
            }

            // read conduct types
            JSONArray conductCalls = joMatch.optJSONArray(JSONKey.conductCalls.toString());
            for (int g = 0; g < JsonUtil.size(conductCalls); g++) {
                String conductCall = conductCalls.getString(g);
                lConductCalls.add(conductCall);
            }
            m_lGameTimings = new ArrayList<GameTiming>();
            m_gameTimingCurrent = null;
            try {
                JSONArray timings = joMatch.optJSONArray(JSONKey.timing.toString());
                if ( JsonUtil.isNotEmpty(timings) ) {
                    for ( int g=0; g < timings.length(); g++ ) {
                        Object oTiming = timings.get(g);
                        if ( oTiming instanceof JSONObject == false ) { continue; } // should not happen normally
                        JSONObject timing = (JSONObject) oTiming;
                        if ( JsonUtil.isEmpty(timing) ) { continue; }
                        Object oStart = timing.opt(JSONKey.start.toString());
                        Object oEnd   = timing.opt(JSONKey.end  .toString());
                        if ( oStart instanceof Long || oEnd instanceof Long ) {
                            // pre 3.19
                            m_gameTimingCurrent = new GameTiming(g,(Long) oStart, (Long) oEnd, onTimingChangedListeners);
                        } else if ( oStart instanceof Integer ) {
                            // pre 3.19: start is usually 0: but also for demo matches
                            int iStart = (Integer) oStart;
                            int iEnd   = iStart;
                            if ( oEnd instanceof Integer ) {
                                iEnd = (Integer) oEnd;
                            }
                            m_gameTimingCurrent = new GameTiming(g, iStart, iEnd, onTimingChangedListeners);
                        } else if ( oStart instanceof String ) {
                            // since 3.19
                            String sStart = (String) oStart;
                            String sEnd   = (String) oEnd;
                            Date dStart   = DateUtil.parseString2Date(sStart, jsonTimeFormat, true);
                            Date dEnd     = DateUtil.parseString2Date(sEnd  , jsonTimeFormat, true);
                            if ( ( dStart == null ) && ( StringUtil.size(sStart)== DateUtil.YYYYMMDD_HHMMSS_DASH_T_COLON.length() - 2 /* 21 - 2 single quotes =  19 */ ) ) {
                                iDeviatingDateFormat+=1000;
                                dStart   = DateUtil.parseString2Date(sStart, DateUtil.YYYYMMDD_HHMMSS_DASH_T_COLON, true);
                                dEnd     = DateUtil.parseString2Date(sEnd  , DateUtil.YYYYMMDD_HHMMSS_DASH_T_COLON, true);
                            }
                            if ( ( dStart == null ) && ( StringUtil.size(sStart)== jsonTimeFormat_Old.length() /* 15 */ ) ) {
                                dStart   = DateUtil.parseString2Date(sStart, jsonTimeFormat_Old, true);
                                dEnd     = DateUtil.parseString2Date(sEnd  , jsonTimeFormat_Old, true);
                            }
                            if ( ( dStart == null ) && ( StringUtil.size(sStart)== DateUtil.YYYYMMDD_HHMMSS_SLASH_DASH_COLON.length() /* 19 */ ) ) {
                                dStart   = DateUtil.parseString2Date(sStart, DateUtil.YYYYMMDD_HHMMSS_SLASH_DASH_COLON, true);
                                dEnd     = DateUtil.parseString2Date(sEnd  , DateUtil.YYYYMMDD_HHMMSS_SLASH_DASH_COLON, true);
                            }
                            if ( dStart != null && dEnd != null ) {
                                m_gameTimingCurrent = new GameTiming(g, dStart.getTime(), dEnd.getTime(), onTimingChangedListeners);
                            } else {
                                Log.w(TAG, "Could not parse dates : " + sStart + " to " + sEnd);
                            }
                        }
                        if ( m_gameTimingCurrent != null ) {
                            m_lGameTimings.add(m_gameTimingCurrent);

                            // added in 3.19
                            if ( timing.has(JSONKey.offsets.toString()) ) {
                                JSONArray offsets = timing.getJSONArray(JSONKey.offsets.toString());
                                for(int o=0; o < offsets.length(); o++) {
                                    int iOffset = offsets.getInt(o);
                                    m_gameTimingCurrent.addTiming(iOffset);
                                }
                            } else {
                                // pre 3.19 match... do not add timings afterwards will never be accurate
                                if ( hasStarted() && g==0 ) {
                                    m_gameTimingCurrent.noScoreTimings();
                                }
                            }
                        }
                    }
                }
/*
                if ( m_lGameTimings.size() == 0 ) {
                    // somehow no timings in the json
                    m_gameTimingCurrent = new GameTiming((Long) oStart, (Long) oEnd);
                    m_lGameTimings.add(m_gameTimingCurrent);
                }
*/
            } catch (Exception e) {
                // not really an error, most likely last match stored did not use these keys
                e.printStackTrace();
            }


            if ( joMatch.has(JSONKey.statistics.toString())) {
                try {
                    m_rallyEndStatistics = new ArrayList<JSONArray>();
                    JSONArray joMatchStats = joMatch.getJSONArray(JSONKey.statistics.toString());
                    for ( int g=0; g < joMatchStats.length(); g++ ) {
                        m_rallyEndStatsGIP = joMatchStats.getJSONArray(g);
                        m_rallyEndStatistics.add(m_rallyEndStatsGIP);
                    }
                } catch (Exception e) {
                    // not really an error, most likely last match stored did not use these keys
                    e.printStackTrace();
                }
            }

            // determine server and side based on last score line
            if ( scoreLine != null ) {
                Player scoringPlayer = scoreLine.getScoringPlayer();
                if ( scoringPlayer != null ) {
                    ServeSide nextServeSide = null;
                    if (scoreLine.isHandout(getSport()) || m_lScoreHistory.size() == 0) {
                        setLastPointWasHandout(true);

                        if (NEXT_SERVE_SIDE_FROM_COUNT) {
                            Map<ServeSide, Integer> serveSideCount = m_player2ServeSideCount.get(scoringPlayer);
                            nextServeSide = MapUtil.getMaxKey(serveSideCount, m_player2LastServeSide.get(scoringPlayer));
                        } else {
                            nextServeSide = m_player2LastServeSide.get(scoringPlayer);
                        }
                    } else {
                        setLastPointWasHandout(false);
                        ServeSide serveSide = scoreLine.getServeSide();
                        nextServeSide = serveSide == null ? ServeSide.R : serveSide.getOther();
                    }
                    setServerAndSide(scoringPlayer, nextServeSide, null);
                } else {
                    //call or broken equipment
                }
            }

            if ( joMatch.has(JSONKey.lockState.toString()) ) {
                LockState lockState = JsonUtil.getEnum(joMatch, JSONKey.lockState, LockState.class, matchHasEnded() ? LockState.LockedEndOfMatch : LockState.Unlocked);
                setLockState(lockState);
                if ( joMatch.has( JSONKey.winnerBecauseOf.toString() ) ) {
                    m_winnerBecauseOf = JsonUtil.getEnum(joMatch, JSONKey.winnerBecauseOf, Player.class, null);
                }
            } else if ( matchHasEnded() ) {
                setLockState(LockState.LockedEndOfMatch);
            }

            setClean();
            if ( iDeviatingDateFormat > 0 ) {
                // deviating date format. Ensure it is stored with correct date format
                setDirty();
            }

            if ( joMatch.has(JSONKey.metadata.toString()) ) {
                JSONObject joMetadata = joMatch.getJSONObject(JSONKey.metadata.toString());

                String sSource   = joMetadata.optString(JSONKey.source.toString(), "");
                String sSourceID = joMetadata.optString(JSONKey.sourceID.toString(), "");
                setSource(sSource, sSourceID);

                String sAdditionalPostParams = joMetadata.optString(JSONKey.additionalPostParams.toString(), "");
                setAdditionalPostParams(sAdditionalPostParams);

                String sShareURL = joMetadata.optString(JSONKey.shareURL.toString());
                if ( StringUtil.isNotEmpty(sShareURL) ) {
                    setShareURL(sShareURL);
                }
            }

            return joMatch;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String toJsonString() {
        return toJsonString(null);
    }
    public String toJsonString(Context context) {
        return toJsonString(context, null);
    }
    public String toJsonString(Context context, JSONObject oSettings) {
        try {
            if ( m_gameTimingCurrent != null ) {
                if ( (m_gameTimingCurrent.getStart() == m_gameTimingCurrent.getEnd()) && ListUtil.size(m_lScoreHistory)!=0 ) {
                    m_gameTimingCurrent.updateEnd(GameTiming.ChangedBy.StartAndEndStillEqual);
                }
            }

            JSONObject jsonObject = getJsonObject(context, oSettings);

            String sJson = null;
            if ( ScoreBoard.isInSpecialMode() ) {
                sJson = jsonObject.toString(4); // with indents takes up more than double the space
                Log.w(TAG, "Match: \n" + sJson);
            } else {
                sJson = jsonObject.toString();
            }
            return sJson;
        } catch (Exception e) {
            Log.e(TAG, e.toString(), e);
        }
        return null;
    }

    public JSONObject getJsonObject(Context context, JSONObject oSettings) throws JSONException {
        JSONObject jsonObject = new JSONObject();

        // games
        JSONArray games = new JSONArray();
        for (int s = 0; s < m_lGameScoreHistory.size(); s++) {
            List<ScoreLine> lScoreHistory = m_lGameScoreHistory.get(s);

            JSONArray game = new JSONArray();
            for (int i = 0; i < lScoreHistory.size(); i++) {
                ScoreLine sl = lScoreHistory.get(i);
                String sScoreLine = sl.toString();
                if ( sl.isCall() || sl.isBrokenEquipment() ) {
                    sScoreLine = sScoreLine.replace("  ", "--").replaceAll(" ", "");
                }
                game.put(i, sScoreLine);
            }
            if ( game.length() != 0 ) {
                games.put(s, game);
            }
        }
        if ( games.length() != 0 ) {
            jsonObject.put(JSONKey.score.toString(), games);
        }

        // just the result, mainly for sharing and fast re-read... does only need to be parsed for fast read
        jsonObject.put(JSONKey.result    .toString(), getResult());
        jsonObject.put(JSONKey.gamescores.toString(), getGameScores());

        if ( oSettings == null ) {
            // next few are only for 'livescore' to be able to show server, serveside and optionally handout. Not used when reading back in the model
            jsonObject.put(JSONKey.server   .toString(), getServer());
            jsonObject.put(JSONKey.serveSide.toString(), getNextServeSide(getServer()));
            if ( this instanceof SquashModel ) {
                jsonObject.put(JSONKey.isHandOut.toString(), isLastPointHandout());
            }
        }
        JsonUtil.removeEmpty(jsonObject);

        // conducts
        if ( ListUtil.size(lConductCalls) > 0 ) {
            JSONArray conductCalls = new JSONArray();
            for (int s = 0; s < lConductCalls.size(); s++) {
                String sConductCall = lConductCalls.get(s);
                conductCalls.put(s, sConductCall);
            }
            jsonObject.put(JSONKey.conductCalls.toString(), conductCalls);
        }

        // players
        JSONObject joPlayers     = new JSONObject();
        JSONObject joPlayerIds   = new JSONObject();
        JSONObject joColors      = new JSONObject();
        JSONObject joCountries   = new JSONObject();
        JSONObject joClubs       = new JSONObject();
        JSONObject joAvatars     = new JSONObject();
        JSONObject joTeamPlayers = new JSONObject();
        for ( Player p : getPlayers() ) {
            joPlayers.put(p.toString(), m_player2Name.get(p));
            String sPlayerId = m_player2Id.get(p);
            if ( StringUtil.isNotEmpty(sPlayerId) ) {
                joPlayerIds.put(p.toString(), sPlayerId);
            }
            String sColor = m_player2Color.get(p);
            if ( StringUtil.isNotEmpty(sColor) ) {
                joColors.put(p.toString(), sColor);
            }
            String sCountry = m_player2Country.get(p);
            if ( StringUtil.isNotEmpty(sCountry) ) {
                joCountries.put(p.toString(), sCountry);
            }
            String sClub = m_player2Club.get(p);
            if ( StringUtil.isNotEmpty(sClub) ) {
                joClubs.put(p.toString(), sClub);
            }
            String sAvatar = m_player2Avatar.get(p);
            if ( StringUtil.isNotEmpty(sAvatar) ) {
                joAvatars.put(p.toString(), sAvatar);
            }
            List<String> players = m_team2Players.get(p);
            if ( ListUtil.isNotEmpty(players) ) {
                joTeamPlayers.put(p.toString(), new JSONArray(players));
            }
        }
        jsonObject.put(JSONKey.players.toString(), joPlayers);
        if ( JsonUtil.isNotEmpty(joPlayerIds) ) {
            jsonObject.put(JSONKey.playerids.toString(), joPlayerIds);
        }
        if ( JsonUtil.isNotEmpty(joColors) ) {
            jsonObject.put(JSONKey.colors.toString(), joColors);
        }
        if ( JsonUtil.isNotEmpty(joCountries) ) {
            jsonObject.put(JSONKey.countries.toString(), joCountries);
        }
        if ( JsonUtil.isNotEmpty(joClubs) ) {
            jsonObject.put(JSONKey.clubs.toString(), joClubs);
        }
        if ( JsonUtil.isNotEmpty(joAvatars) ) {
            jsonObject.put(JSONKey.avatars.toString(), joAvatars);
        }
/*
        if ( JsonUtil.isNotEmpty(joTeamPlayers) ) {
            jsonObject.put(JSONKey.team_players.toString(), joTeamPlayers);
        }
*/

        // referee
        if ( StringUtil.hasNonEmpty(m_sReferee, m_sMarker) ) {
            JSONObject joRef = new JSONObject();
            joRef.put(JSONKey.name   .toString(), m_sReferee);
            joRef.put(JSONKey.markers.toString(), m_sMarker);
            JsonUtil.removeEmpty(joRef);
            jsonObject.put(JSONKey.referee.toString(), joRef);
        }
        if ( StringUtil.isNotEmpty(m_sCourt) ) {
            jsonObject.put(JSONKey.court.toString(), m_sCourt);
        }

        // when: date and time
        JSONObject joWhen = new JSONObject();
        joWhen.put(JSONKey.date.toString(), m_matchDate);
        if ( StringUtil.isNotEmpty(m_matchTime) ) {
            joWhen.put(JSONKey.time.toString(), m_matchTime);
        }
        jsonObject.put(JSONKey.when.toString(), joWhen);

        // event
        if ( StringUtil.isNotEmpty(m_sEventName) || StringUtil.isNotEmpty(m_sEventRound) ) {
            JSONObject joEvent = new JSONObject();
            joEvent.put(JSONKey.name    .toString(), m_sEventName);
            joEvent.put(JSONKey.division.toString(), m_sEventDivision);
            joEvent.put(JSONKey.round   .toString(), m_sEventRound);
            joEvent.put(JSONKey.location.toString(), m_sEventLocation);
            JsonUtil.removeEmpty(joEvent);
            jsonObject.put(JSONKey.event.toString(), joEvent);
        }

        // match format
        JSONObject joFormat = new JSONObject();
        joFormat.put(JSONKey.numberOfPointsToWinGame.toString(), m_iNrOfPointsToWinGame);
        if ( EnumSet.of(SportType.Racketlon).contains(getSport()) == false ) {
            joFormat.put(JSONKey.numberOfGamesToWinMatch    .toString(), m_iNrOfGamesToWinMatch);
        }
        if ( m_iTotalNrOfGamesToFinishForMatchToEnd != UNDEFINED_VALUE ) {
            joFormat.put(JSONKey.playAllGames.toString(), true);
        }
        if ( m_sMode != null ) {
            joFormat.put(JSONKey.mode.toString(), m_sMode);
        }

        if ( EnumSet.of(SportType.Squash, SportType.Racquetball, SportType.Badminton).contains(getSport()) ) {
            if ( m_bEnglishScoring ) {
                joFormat.put(JSONKey.useHandInHandOutScoring.toString(), m_bEnglishScoring);
            }
            if ( (m_TieBreakFormat != null) && m_TieBreakFormat.equals(TieBreakFormat.TwoClearPoints) == false ) {
                joFormat.put(JSONKey.tiebreakFormat.toString(), m_TieBreakFormat.toString());
            }
            if ( isDoubles() ) {
                if ( (m_doubleServeSequence != null) && (m_doubleServeSequence.equals(DoublesServeSequence.NA) == false) ) {
                    joFormat.put(JSONKey.doublesServeSequence.toString(), m_doubleServeSequence.toString());
                }
            }
        }
        if ( isUsingHandicap() ) {
            joFormat.put(JSONKey.handicapFormat.toString(), m_HandicapFormat.toString());
            JSONArray joOffset = new JSONArray();
            for ( Map<Player, Integer> ss : m_startScoreOfPreviousGames) {
                joOffset.put(new JSONObject(MapUtil.keysToString(ss)));
            }
            joOffset.put(new JSONObject(MapUtil.keysToString(m_startScoreOfGameInProgress)));
            joFormat.put(JSONKey.gameStartScoreOffset.toString(), joOffset);
        }
        jsonObject.put(JSONKey.format.toString(), joFormat);

        if ( m_lockState.equals(LockState.UnlockedEndOfFinalGame) ) {
            m_lockState = LockState.LockedEndOfMatch;
        }
        if ( hasStarted() ) {
            jsonObject.put(JSONKey.lockState.toString(), m_lockState.toString());
            if ( m_winnerBecauseOf != null ) {
                jsonObject.put(JSONKey.winnerBecauseOf.toString(), m_winnerBecauseOf.toString());
            }
        }

        // timings
        JSONArray timings = new JSONArray();
        for (int s = 0; s < m_lGameTimings.size(); s++) {
            GameTiming gameTiming = m_lGameTimings.get(s);
            JSONObject timing = new JSONObject();
            long lStart = gameTiming.getStart();
            if ( lStart == 0 ) {
                timing.put(JSONKey.start.toString(), 0);
                timing.put(JSONKey.end  .toString(), gameTiming.getEnd());
            } else {
                long lEnd = gameTiming.getEnd();
                if ( (lEnd > lStart) || ListUtil.isNotEmpty(m_lScoreHistory) ) {
                    timing.put(JSONKey.start.toString(), DateUtil.formatDate2String(lStart, jsonTimeFormat));
                    timing.put(JSONKey.end  .toString(), DateUtil.formatDate2String(lEnd  , jsonTimeFormat));
                }
            }

            List<Integer> lScoreTimings = gameTiming.getScoreTimings();
            if ( ListUtil.isNotEmpty(lScoreTimings) ) {
                JSONArray offsets = new JSONArray();
                for (int i = 0; i < lScoreTimings.size(); i++) {
                    int iSecs = lScoreTimings.get(i);
                    offsets.put(i, iSecs);
                }
                if ( JsonUtil.isNotEmpty(offsets) ) {
                    // often empty for internal communication between activities of non-started match
                    timing.put(JSONKey.offsets.toString(), offsets);
                }
            }
            if ( JsonUtil.isNotEmpty(timing) ) {
                timings.put(s, timing);
            }
        }
        if ( JsonUtil.isNotEmpty(timings) ) {
            jsonObject.put(JSONKey.timing.toString(), timings);
        }

        // statistics
        if ( ListUtil.isNotEmpty(m_rallyEndStatistics) ) {
            int iNrOfStats = 0;
            for ( JSONArray tst: m_rallyEndStatistics ) {
                iNrOfStats += tst.length();
            }
            if ( iNrOfStats > 0 ) {
                JSONArray matchStats = new JSONArray(m_rallyEndStatistics);
                jsonObject.put(JSONKey.statistics.toString(), matchStats);
            }
        }

        // if additional setting data is provided, add it as well
        // e.g. color preferences, language preferences
        if ( oSettings != null ) {
            jsonObject.put("settings", oSettings);
        }

        // store sport if it is not Squash
        if ( getSport().equals(SportType.Squash) == false ) {
            jsonObject.put(JSONKey.sport.toString(), getSport().toString());
        }
        if ( context != null ) {
            jsonObject.put(JSONKey.appName   .toString(), Brand.getShortName(context)); // TODO: read this as well, and keep the name while match is locked
            jsonObject.put(JSONKey.appPackage.toString(), context.getPackageName());
        }

        JSONObject metaData = new JSONObject();
        metaData.put(JSONKey.source  .toString(), m_sSource);
        metaData.put(JSONKey.sourceID.toString(), m_sSourceID);
        metaData.put(JSONKey.additionalPostParams.toString(), m_sAdditionalPostParams);
        if ( StringUtil.isNotEmpty(m_shareUrl) ) {
            metaData.put(JSONKey.shareURL.toString(), m_shareUrl);
        }
        if ( context != null ) {
            try {
                String packageName = context.getPackageName();
                PackageInfo info = context.getPackageManager().getPackageInfo(packageName, 0);
                metaData.put("version"    , info.versionCode);
                metaData.put("language"   , RWValues.getDeviceLanguage(context));
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                JSONObject wifi = new JSONObject();
                WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                final WifiInfo wifiInfo = wifiManager.getConnectionInfo(); // ACCESS_WIFI_STATE

                //final String bssid                   = wifiInfo.getBSSID(); // usually very similar if connected to the same SSID but now per definition equal (incemented by 1 or 2 ...)
                final String androidDeviceMacAddress = wifiInfo.getMacAddress();
                String ssid                          = wifiInfo.getSSID(); // ssid of the wifi e.g. TELENETHOMESPOT of telenet-5F3EB
                if ( StringUtil.isNotEmpty(ssid) ) {
                    ssid = ssid.replaceAll("[^\\w\\-\\.]", ""); // may e.g hold '<unknown ssid>'
                }
                final int ipAddress = wifiInfo.getIpAddress(); // ipadress assigned to device by e.g. router
                wifi.put("ipaddress"   , Placeholder.Misc.IntegerToIPAddress.execute(String.valueOf(ipAddress), null, null));
                wifi.put("ssid"        , ssid);
                wifi.put("mac"         , androidDeviceMacAddress);
                JsonUtil.removeEmpty(wifi);
                metaData.put(JSONKey.wifi.toString(), wifi);
            } catch (Exception e) {
                e.printStackTrace();
            }
/*
            try {
                WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                WifiInfo wifiInfo = wifiManager.getConnectionInfo(); // requires ACCESS_WIFI_STATE
                JSONObject internetMetaData = new JSONObject();
                internetMetaData.put("bssid"     , wifiInfo.getBSSID()     ); // usually very similar if connected to the same SSID but now per definition equal (incemented by 1 or 2 ...)
                internetMetaData.put("ssid"      , wifiInfo.getSSID()      ); // ssid of the wifi e.g. TELENETHOMESPOT of telenet-5F3EB
                internetMetaData.put("macaddress", wifiInfo.getMacAddress()); // macaddress of android device
                jsonObject.put("internet", internetMetaData);
            } catch (Exception e) {
                e.printStackTrace();
            }
*/
        }
        JsonUtil.removeEmpty(metaData);
        jsonObject.put(JSONKey.metadata.toString(), metaData);
        return jsonObject;
    }

/* TODO: allow modifying this for stored matches
    public void setDate(String sYYYYMMDD) {
        if ( StringUtil.size(sYYYYMMDD) == 0 ) { return; }
        sYYYYMMDD = sYYYYMMDD.replaceAll("[^0-9]", "");
        if ( StringUtil.size(sYYYYMMDD) != 8 ) { return; }
        m_matchDate = sYYYYMMDD;
    }
    public void setTime(String sHHMMSS) {
        if ( StringUtil.size(sHHMMSS) == 0 ) { return; }
        sHHMMSS = sHHMMSS.replaceAll("[^0-9]", "");
        if ( StringUtil.size(sHHMMSS) == 4 ) {
            sHHMMSS += "00";
        }
        if ( StringUtil.size(sHHMMSS) != 6 ) { return; }
        m_matchTime = sHHMMSS;
    }
*/

    void setLastPointWasHandout(boolean b) {
        if ( m_bLastPointWasHandout != b ) {
            m_bLastPointWasHandout = b;
            // to remove the optional question mark
            for(OnServeSideChangeListener l: onServeSideChangeListener) {
                l.OnServeSideChange(m_pServer, m_in_out, m_nextServeSide, m_bLastPointWasHandout);
            }
        }
    }
    /** called only 'manually forced' via menu item from outside model */
    public void endMatch(EndMatchManuallyBecause endMatchManuallyBecause, Player pWinnerManually) {
        endGame(false);

        m_winnerBecauseOf = pWinnerManually;

        Player possibleMatchVictoryFor = (pWinnerManually!=null) ? pWinnerManually : isPossibleMatchVictoryFor();
        if ( possibleMatchVictoryFor != null ) {
            // match already ended by because of the score
        } else {
            Map<Player, Integer> gamesWon = getGamesWon();
            int iMax = MapUtil.getMaxValue(gamesWon);
            int iMin = MapUtil.getMinValue(gamesWon);
            if ( iMax > iMin ) {
                // end the match by changing nr of games to win
                //setNrOfGamesToWinMatch(iMax);
                possibleMatchVictoryFor = MapUtil.getMaxKey(gamesWon, null);
            } else {
                // each player has the same nr of games won
                possibleMatchVictoryFor = isPossibleMatchVictoryFor_SQ_TT(true); // TODO: this call should not be here
            }
        }
        if ( possibleMatchVictoryFor != null ) {
            for (OnMatchEndListener l : onMatchEndListeners) {
                l.OnMatchEnded(possibleMatchVictoryFor, endMatchManuallyBecause);
            }
        }
    }

    public void endGame() {
        endGame(true);
    }
    public void endGame(boolean bNotifyListeners) {

        int iScoreA = MapUtil.getInt(m_scoreOfGameInProgress, Player.A, 0);
        int iScoreB = MapUtil.getInt(m_scoreOfGameInProgress, Player.B, 0);

        if ( iScoreA == iScoreB ) {
            // can not end game when both players have the same score
            return;
        }

        HashMap<Player, Integer> scores = new HashMap<Player, Integer>();
        scores.put(Player.A, iScoreA);
        scores.put(Player.B, iScoreB);
        addGameScore(scores, true);

        // learn from current game ending to what game ending we are playing
        int max  = Math.max(iScoreA, iScoreB);
        if ( (max < m_iNrOfPointsToWinGame) && (getGameNrInProgress() == 1)) {
            setNrOfPointsToWinGame(max);
        }

        startNewGame();

        Player serverForNextGame = determineServerForNextGame(getGameNrInProgress()-1, iScoreA, iScoreB);
        setServerAndSide(serverForNextGame, null, null);

        setDirty(true);

        if ( bNotifyListeners ) {
            for (OnGameEndListener l : onGameEndListeners) {
                l.OnGameEnded(m_pServer);
            }
            if ( matchHasEnded() ) {
                Player possibleMatchVictoryFor = isPossibleMatchVictoryFor();
                for (OnMatchEndListener l : onMatchEndListeners) {
                    l.OnMatchEnded(possibleMatchVictoryFor, null);
                }
            }
        }
    }

    public void timestampStartOfGame(GameTiming.ChangedBy changedBy) {
        if ( m_gameTimingCurrent == null ) { return; }
        if ( gameHasStarted() == false ) {
            // ref deliberately pressed announcement/timer button while score at 0-0 so first rally is still to start
            int iNrOfFinishedGames = getNrOfFinishedGames();
            if ( ListUtil.size(m_lGameTimings) <= iNrOfFinishedGames) {
                m_gameTimingCurrent = new GameTiming(iNrOfFinishedGames, System.currentTimeMillis(), System.currentTimeMillis(), onTimingChangedListeners);
                m_lGameTimings.add(m_gameTimingCurrent);
            } else {
                m_gameTimingCurrent.updateStart(0, changedBy);
            }
        }
    }

    public List<GameTiming> getTimes() {
        return m_lGameTimings;
    }

    public List<String> getFormattedTimes() {
        List<String> lTimes = new ArrayList<String>();
        for(GameTiming gt: m_lGameTimings) {
            lTimes.add(DateUtil.formatDate2String(gt.getStart(), Model.jsonTimeFormat));
            lTimes.add(DateUtil.formatDate2String(gt.getEnd()  , Model.jsonTimeFormat));
        }
        return lTimes;
    }

    private int m_iNrOfTiebreaks = 0;// TODO: initialize this variable correctly if match is read from json
    public int getTiebreakOccurence() {
        return m_iNrOfTiebreaks;
    }
    public boolean isStartOfTieBreak()
    {
        int iScoreA = MapUtil.getInt(m_scoreOfGameInProgress, Player.A, 0);
        int iScoreB = MapUtil.getInt(m_scoreOfGameInProgress, Player.B, 0);
        int diff    = Math.abs( iScoreA - iScoreB );
        int iNrOfPointsToWinGame = getNrOfPointsToWinGame();

        if (diff != 0 ) {
            return false;
        }

        if ( m_bEnglishScoring ) {
            if ( iScoreA == (iNrOfPointsToWinGame - 1) )  {
                return (isLastPointHandout() == false);
            }
            return false;
        } else {
            return iScoreA == (iNrOfPointsToWinGame - 1);
        }
    }

    //--------------------------------------------------
    // Possible game/match ball
    //--------------------------------------------------

    private Map<When, Player[]> m_possibleMatchForPrev = new HashMap<>();
    private Map<When, Player[]> m_possibleMatchFor     = new HashMap<>();
    private Map<When, Player[]> m_possibleGameForPrev  = new HashMap<>();
    private Map<When, Player[]> m_possibleGameFor      = new HashMap<>();

    abstract Player[] calculatePossibleMatchVictoryFor (When when, Player[] paGameVictoryFor);
    abstract Player[] calculateIsPossibleGameVictoryFor(When when, Map<Player, Integer> gameScore, boolean bFromIsMatchBallFrom);

    /** If game format has no tie-break (sudden death), it can be game ball for both players simultaneously */
    Player[] _isPossibleGameVictoryFor(When when, boolean bFromIsMatchBallFrom) {
        Player[] players = m_possibleGameFor.get(when);
        if ( players == null ) {
            players = calculateIsPossibleGameVictoryFor(when, m_scoreOfGameInProgress, bFromIsMatchBallFrom);
            setGameVictoryFor(when, players);
        }
        return players;
    }

    public Player[] isPossibleGameBallFor() {
        return _isPossibleGameVictoryFor(When.ScoreOneMorePoint, false);
    }

    public boolean isPossibleGameBallFor(Player p) {
        Player[] pa = isPossibleGameBallFor();
        return ( pa.length >= 1 && pa[0].equals(p) )
            || ( pa.length >= 2 && pa[1].equals(p) );
    }

    private void setGameVictoryFor(When when, Player[] gameballForNew) {
        // store the new value
        m_possibleGameFor.put(when, gameballForNew);

        final Player[] gameballForOld = m_possibleGameForPrev.get(when);
        if ( gameballForNew == gameballForOld ) {
            return;
        } else {
            m_possibleGameForPrev.remove(when);
        }
        boolean bGameBallFor_Unchanged = ListUtil.length(gameballForNew) == 1
                                      && ListUtil.length(gameballForOld) == 1
                                      && gameballForOld[0].equals(gameballForNew[0]);
        if ( bGameBallFor_Unchanged == false ) {
            //Log.d(TAG, String.format("Gameball %s changed from %s to %s", when, getPlayersAsList(gameballForOld), getPlayersAsList(gameballForNew)));
        }

        if ( when.equals(When.ScoreOneMorePoint) ) {
            if ( ListUtil.isNotEmpty(gameballForOld) && (bGameBallFor_Unchanged == false) ) {
                // no longer game ball for...
                for (OnSpecialScoreChangeListener l : onSpecialScoreChangeListeners) {
                    l.OnGameBallChange(gameballForOld, false);
                }
            }

            if ( ListUtil.isNotEmpty(gameballForNew) ) {
                // now gameball for
                for(OnSpecialScoreChangeListener l: onSpecialScoreChangeListeners) {
                    l.OnGameBallChange(gameballForNew, true);
                }
            }
        }
    }

    private void setMatchVictoryFor(When when, Player[] matchballForNew) {
        // store the new
        m_possibleMatchFor.put(when, matchballForNew);

        final Player[] matchballForOld = m_possibleMatchForPrev.get(when);
        if ( matchballForNew == matchballForOld ) {
            return;
        } else {
            m_possibleMatchForPrev.remove(when);
        }
        boolean bMatchBallFor_Unchanged = ListUtil.length(matchballForNew) == 1
                && ListUtil.length(matchballForOld) == 1
                && matchballForOld[0].equals(matchballForNew[0]);
        if ( bMatchBallFor_Unchanged == false ) {
            //Log.d(TAG, String.format("Matchball %s changed from %s to %s", when, getPlayersAsList(matchballForOld), getPlayersAsList(matchballForNew)));
        }

        if ( when.equals(When.ScoreOneMorePoint) ) {
            if ( ListUtil.isNotEmpty(matchballForOld) && (bMatchBallFor_Unchanged == false) ) {
                // no longer matchball for...
                for(OnSpecialScoreChangeListener l: onSpecialScoreChangeListeners) {
                    l.OnGameBallChange(matchballForOld, false);
                }
            }

            if ( ListUtil.isNotEmpty(matchballForNew) ) {
                // now matchball for
                for(OnSpecialScoreChangeListener l: onSpecialScoreChangeListeners) {
                    l.OnGameBallChange(matchballForNew, true);
                }
            }
        }
    }

    /** Typically overwritten in tabletennis and Badminton */
    public boolean isTowelingDownScore(int iEveryXpoints, int iIfHighestScoreEquals) {
        return false;
    }

    public Player isPossibleMatchVictoryFor() {
        Player[] players = _isPossibleMatchVictoryFor(When.Now, null);
        return ListUtil.length(players)==1?players[0]:null;
    }

    public Player[] isPossibleMatchBallFor() {
        return isPossibleMatchBallFor(null);
    }
    Player[] isPossibleMatchBallFor(Player[] paGameVictoryFor) {
        return _isPossibleMatchVictoryFor(When.ScoreOneMorePoint, paGameVictoryFor);
    }

    Player[] _isPossibleMatchVictoryFor(When when, Player[] paGameVictoryFor) {
        Player[] players = m_possibleMatchFor.get(when);
        if ( players == null ) {
            players = calculatePossibleMatchVictoryFor(when, paGameVictoryFor);
            setMatchVictoryFor(when, players);
        }
        return players;
    }

    public boolean isPossibleGameVictory() {
        return isPossibleGameVictoryFor()!=null;
    }
    public Player isPossibleGameVictoryFor() {
        Player[] players = _isPossibleGameVictoryFor(When.Now, false);
        return ListUtil.length(players)==1?players[0]:null;
    }

    public boolean matchHasEnded() {
        if ( m_iTotalNrOfGamesToFinishForMatchToEnd > 0 ) {
            return m_iTotalNrOfGamesToFinishForMatchToEnd == this.getNrOfFinishedGames();
        } else {
            return isPossibleMatchVictoryFor()!=null;
        }
    }

    private int m_iHandoutCountDoubles = -1;
    private void handout(Player scorer, boolean bScoreChangeTrue_bNewGameFalse) {
        if ( this.isDoubles() ) {
            // doubles
            if ( bScoreChangeTrue_bNewGameFalse ) {
                m_iHandoutCountDoubles++;

                boolean      bFirstHandout = m_iHandoutCountDoubles == 0;
                DoublesServe in_out_prev   = m_in_out;
                DoublesServe doubleIO      = m_doubleServeSequence.playerToServe(m_in_out, bFirstHandout, m_iHandoutCountDoubles);

                if ( m_doubleServeSequence.switchTeam(in_out_prev, bFirstHandout) ) {
                    setServerAndSide(scorer, m_player2LastServeSide.get(scorer), doubleIO);
                    setLastPointWasHandout(true);
                } else {
                    // serve stayed within the same team
                    boolean bSecondPlayerMayNotChooseSide = true; // TODO: optional
                    ServeSide serveSide = null;
                    if ( bSecondPlayerMayNotChooseSide ) {
                        setLastPointWasHandout(false);
                        serveSide = m_nextServeSide.getOther();
                    } else {
                        setLastPointWasHandout(true);
                        serveSide = m_player2LastServeSide.get(scorer);
                    }
                    setServerAndSide(null, serveSide, doubleIO);
                }
            } else {
                m_iHandoutCountDoubles = -1;
                DoublesServe ds = m_doubleServeSequence.playerToServe(DoublesServe.NA, true, m_iHandoutCountDoubles);
                setServerAndSide(scorer, m_player2LastServeSide.get(scorer), ds); // TODO: option to handle 'looser of previous game starts serving'
                setLastPointWasHandout(true);
            }
        } else {
            // singles
            if (NEXT_SERVE_SIDE_FROM_COUNT) {
                Map<ServeSide, Integer> serveSideCount = m_player2ServeSideCount.get(scorer);
                setServerAndSide(scorer, MapUtil.getMaxKey(serveSideCount, m_player2LastServeSide.get(scorer)), null);
            } else {
                setServerAndSide(scorer, m_player2LastServeSide.get(scorer), null);
            }

            setLastPointWasHandout(true);
        }
    }

    private int m_iDirty = 0;
    public void setDirty() {
        setDirty(false);
    }
    /** ensure certain inner members (player having matchball, player having gameball) are recalculated */
    private void setDirty(boolean bScoreRelated) {
        m_iDirty++;

        if ( bScoreRelated ) {
            m_tsLastJsonOperation = System.currentTimeMillis();
            setShareURL(null);

            m_sResultFast = null;
            m_possibleMatchForPrev.clear(); m_possibleMatchForPrev.putAll(m_possibleMatchFor);
            m_possibleGameForPrev .clear(); m_possibleGameForPrev .putAll(m_possibleGameFor );
            m_possibleMatchFor    .clear();
            m_possibleGameFor     .clear();
        }

        //Log.v(TAG, String.format("Dirty cnt: %s , score related %s", m_iDirty, bScoreRelated));
    }

    public void setClean() {
        m_iDirty = 0;
    }
    public boolean isDirty() {
        return m_iDirty > 0;
    }

    public boolean gameHasStarted() {
        return ListUtil.isNotEmpty(m_lScoreHistory);
    }
    public boolean hasStarted() {
        boolean bHasStarted = getNrOfFinishedGames() > 0 || gameHasStarted();
        return bHasStarted;
    }

/*
    @Override public String toString() {
        return "BO:" + (m_iNrOfGamesToWinMatch*2+1) + ",To:" + m_iNrOfPointsToWinGame + (m_bEnglishScoring?",Eng":"");
    }
*/

    @Override public String toString() {
        String nameA = getName(Player.A);
        String nameB = getName(Player.B);
        String sNvsN = "";
        if ( StringUtil.areAllNonEmpty(nameA, nameB) ) {
            sNvsN = (isDoubles()?"Doubles":"Singles") + ":" + nameA.substring(0,1) + "-" + nameB.substring(0,1);
        }
        String sAvsB = getScore(Player.A) + "-" + getScore(Player.B);
        if ( sAvsB.equals("0-0") ) {
            return "GtwM:" + getNrOfGamesToWinMatch() + ", PtwG:" + getNrOfPointsToWinGame();
        }
        return (sNvsN + " " + getGameScores() + " [" + sAvsB + "]").trim();
    }

    //---------------------------------
    //-------------------- LOCKING ----
    //---------------------------------

    private LockState m_lockState = LockState.Unlocked;
    public boolean isLocked() {
        return m_lockState.isLocked();
    }
    public boolean isUnlockable() {
        return m_lockState.isUnlockable();
    }
    public void setLockState(LockState lockState) {
        // special unchangeable state
        if ( LockState.UnlockedUnchangeable.equals(m_lockState) ) {
            return;
        }

        if (  m_lockState.equals(lockState) == false ) {
            setDirty(false);
        }
        LockState old = m_lockState;
        m_lockState = lockState;
        for ( OnLockChangeListener l : onLockChangeListeners ) {
            l.OnLockChange(old, m_lockState);
        }
    }

    public LockState getLockState() {
        return m_lockState;
    }

    //---------------------------------
    // Statistics
    //---------------------------------

    //-- winnererror recording 'FH Volley Winner PlayerA--
    /** For keeping track of error/winners for presenting statistics. Contains all games including the one in progress */
    transient private List<JSONArray> m_rallyEndStatistics     = new ArrayList<JSONArray>();
    /** statistics of the game in progess */
    transient private JSONArray m_rallyEndStatsGIP         = null;
    public void recordWinnerError( RallyEnd   winnerError
                                 , Player     player
                                 , RacketSide racket
                                 , Position   position
                                 , BallDirection direction, BallTrajectory trajectory ) {
        String sKey = joinStats(winnerError, player, racket, position, direction, trajectory);
        try {
            m_rallyEndStatsGIP.put(m_rallyEndStatsGIP.length(), sKey);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // TODO: for undo reasons keep track of when to undo
    }

    public String joinStats(RallyEnd winnerError, Player player, RacketSide racket, Position position, BallDirection direction, BallTrajectory trajectory) {
        return ListUtil.join("_", winnerError, player, racket, position, direction, trajectory);
    }

    public List<String> getStatistics() {
        List<String> l = new ArrayList<String>();
        for(JSONArray jaGame : m_rallyEndStatistics) {
            l.addAll(JsonUtil.asListOfStrings(jaGame));
        }
        return l;
    }

    //-------------------------------------------
    //-- methods for quick communication between activities using Model with limited data (mainly for player names/event names)
    //-------------------------------------------
    public void clear() {
        m_player2Name   .clear();
        m_player2Country.clear();
        m_player2Club   .clear();
        m_player2Color  .clear();

        this.setEvent(null, null, null, null);

        m_iNrOfGamesToWinMatch = UNDEFINED_VALUE;
        m_iNrOfPointsToWinGame = UNDEFINED_VALUE;
        m_iTotalNrOfGamesToFinishForMatchToEnd = UNDEFINED_VALUE;
        m_sUnparsedDate       = null;
        m_sResultFast         = null;
        setSource(null, null);
        setAdditionalPostParams(null);
      //m_doubleServeSequence = DoublesServeSequence.NA;
        m_bIsDouble           = false;
        m_in_out              = DoublesServe.NA;
        setDirty(true);
        m_iDirty              = 0;
    }
    private String m_sUnparsedDate = null;
    public String getUnparsedDate() { return m_sUnparsedDate; }
    public void setUnparsedDate(String s) { m_sUnparsedDate = s; }

    //-------------------------------------------
    //-- methods used by several sub classes
    //-------------------------------------------

    Player[] isPossibleMatchBallFor_SQ_TT_BM(When when, Player[] pGameVictoryFor)
    {
        Map<Player, Integer> gameInProgressFor = new HashMap<>();
        Player[] possible = null;
        if ( getMaxScore() > 0 ) {
            possible = _isPossibleGameVictoryFor(when, true);
            if ( ListUtil.isEmpty(possible) ) { return possible; }
            for(Player p: possible) {
                gameInProgressFor.put(p, 1);
            }
        } else {
            possible = getPlayers();
        }

        List<Player> lReturn = null;
        for(Player p: possible) {
            int iGames = MapUtil.getInt(m_player2GamesWon, p, 0);
            if ( iGames + MapUtil.getInt(gameInProgressFor, p, 0) >= m_iNrOfGamesToWinMatch ) {
                if ( lReturn == null ) {
                    lReturn  = new ArrayList<Player>();
                }
                lReturn.add(p);
            }
        }
        if ( ListUtil.isEmpty(lReturn) ) {
            return getNoneOfPlayers();
        }
        return lReturn.toArray(new Player[]{});
    }

    String getResultShort_SQ_TT_BM() {
        Map<Player, Integer> gamesWon  = this.getGamesWon();
        String sResult = MapUtil.getInt(gamesWon, Player.A, 0) + "-" + MapUtil.getInt(gamesWon, Player.B, 0);
        return sResult;
    }

    void deterimineServeAndSideFromPrevious_SQ_BT(ScoreLine lastValidWithServer, ScoreLine slRemoved) {
        // TODO: improve for doubles

        if ( lastValidWithServer != null ) {
            Player    pServer              = lastValidWithServer.getScoringPlayer();
            ServeSide serversPreferredSide = MapUtil.getMaxKey(m_player2ServeSideCount.get(pServer), ServeSide.R);
            Player    lastServer           = lastValidWithServer.getServingPlayer();
            ServeSide lastServeSide        = lastValidWithServer.getServeSide();
            // savety precaution... should not occur
            if ( lastServeSide == null ) {
                lastServeSide = ServeSide.R;
            }
            ServeSide nextServeSide        = serversPreferredSide;
            if ( pServer.equals(lastServer) && (lastServeSide != null) ) {
                nextServeSide = lastServeSide.getOther();
            }

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
    /** determines AND sets the new value */
    void determineServerAndSide_BM(final Player pScorer) {

        Player    nextServer    = pScorer;
        if ( pScorer == null ) {
            nextServer = getServer();
        }

        DoublesServe serveIO = m_in_out;

        int       iPointsServer = getScore(pScorer);
        ServeSide serveSide     = ServeSide.values()[iPointsServer % 2];

        if ( isDoubles() ) {
            DoublesServeSequence dss = getDoubleServeSequence();
            if ( dss.equals(DoublesServeSequence.A1B1A1B1) ) {
                serveIO = DoublesServe.I;
            } else {
                if ( pScorer.equals(getServer()) == false ) {
                    // hand-out: score for receiver
                    m_iHandoutCountDoubles++;

                    boolean      bFirstHandout = m_iHandoutCountDoubles == 0;
                    DoublesServe in_out_prev   = m_in_out;
                                 serveIO       = m_doubleServeSequence.playerToServe(m_in_out, bFirstHandout, m_iHandoutCountDoubles);

                    if ( m_doubleServeSequence.switchTeam(in_out_prev, bFirstHandout) ) {
                        //setServerAndSide(pScorer, m_player2LastServeSide.get(pScorer), ds);
                        setLastPointWasHandout(true);
                    } else {
                        nextServer = getServer();

                        // serve stayed within the same team
                        setLastPointWasHandout(false);
                        //serveSide = m_nextServeSide.getOther();
                    }
                } else {
                    // serve stayed within the same team
                    setLastPointWasHandout(false);
                }
            }
        }
        setServerAndSide(nextServer, serveSide, serveIO);
    }
    /** determines AND sets the new value */
    void determineServerAndSide_TT_RL(boolean bForUndo, SportType sportType) {
        DoublesServe dsIO = DoublesServe.NA;
        if ( isDoubles() ) {
            dsIO = calculateDoublesInOut_TT_RL();
        }
        if ( isInTieBreak_TT_RL() ) {
            int    iMaxScore               = getMaxScore();
            int    iDiffScore              = getDiffScore();
            Player serverAtStartOfSet      = determineServerForNextGame(ListUtil.size(m_endScoreOfPreviousGames), iMaxScore, iMaxScore);
            int    iNrOfPointsIntoTieBreak = iMaxScore - (getNrOfPointsToWinGame()-1);
            if ( iNrOfPointsIntoTieBreak % 2 == 0 ) {
                // 20-20,               22-21, 22-22              , 24-23
                if ( iDiffScore == 0 ) {
                    setServerAndSide(serverAtStartOfSet, ServeSide.R, dsIO);
                } else {
                    setServerAndSide(serverAtStartOfSet.getOther(), ServeSide.L, dsIO);
                }
            } else {
                //        21-20, 21-21              , 23-22, 23-23
                if ( iDiffScore == 0 ) {
                    setServerAndSide(serverAtStartOfSet, ServeSide.L, dsIO);
                } else {
                    setServerAndSide(serverAtStartOfSet.getOther(), ServeSide.R, dsIO);
                }
            }
        } else {
            switch (sportType) {
                case Racketlon: {
                    ServeSide nextServeSide = m_nextServeSide.getOther(); // TODO: for discipline tabletennis in racketlon abusing this to switch server when serveSide=R (every 2 serves)
                    Player    server        = getServer();
                    if ( nextServeSide.equals(ServeSide.R) && (bForUndo == false)) {
                        server = server.getOther();
                        if ( isDoubles() ) {
                            setServerAndSide(null, null , dsIO);
                        }
                    }
                    if ( nextServeSide.equals(ServeSide.L) && (bForUndo) ) {
                        server = server.getOther();
                    }
                    setServerAndSide(server, nextServeSide, dsIO);
                    break;
                }
                case Tabletennis:
                    int iA = getScore(Player.A);
                    int iB = getScore(Player.B);
                    int iTotalInGame = iA + iB;
                    int iSetZB = ListUtil.size(m_endScoreOfPreviousGames);
                    Player server = determineServerForNextGame_TT_RL(iSetZB, false);
                    int iServerSwitches = iTotalInGame / m_iNrOfServesPerPlayer;
                    for( int i = 0; i < iServerSwitches; i++ ) {
                        server = server.getOther();
                    }
                    setServerAndSide(server, m_nextServeSide.getOther() /* dirty trick to always ensure listeners are triggered to */, dsIO);
                    break;
            }
        }
    }

    DoublesServe calculateDoublesInOut_TT_RL() {
        DoublesServeSequence dss           = getDoubleServeSequence();
        if ( dss.equals(DoublesServeSequence.A1B1A1B1) ) {
            // serve sequence for squash part of racketlon only: only one player on court
            return DoublesServe.I;
        }
        boolean bIsInTieBreak       = isInTieBreak_TT_RL();
        int     iTotalPointsScored  = getTotalGamePoints();
        int     nrOfServesPerPlayer = getNrOfServesPerPlayer();
        if ( bIsInTieBreak ) {
            nrOfServesPerPlayer = 1;
        }
        int iPointsScoredSinceEveryBodyHadServedSameNrOfTimes = iTotalPointsScored % (nrOfServesPerPlayer * 4 /*nr of players*/);
        DoublesServe ds = ( iPointsScoredSinceEveryBodyHadServedSameNrOfTimes >= nrOfServesPerPlayer * 2 ) ? DoublesServe.O : DoublesServe.I;
        if ( bIsInTieBreak && (getNrOfPointsToWinGame() * 2) % (getNrOfServesPerPlayer() * 4 ) > 0 ) {
            // server of first point in tie-break player is NOT player that started serving in the game
            ds = ds.getOther();
        }
        return ds;
    }

    public int getTotalGamePoints() {
        return getMaxScore() + getMinScore();
    }

    boolean isInTieBreak_TT_RL()
    {
        return getMinScore() >= getNrOfPointsToWinGame() - 1;
    }

    Player determineServerForNextGame_TT_RL(int iSetZB, boolean bSetServer) {
        Player server      = this.getServer();
        Player serverOfSet = null;

        // determine server by means of looking who served in a previous set (preferably the first set)
        for ( int iSetZBTmp = 0; iSetZBTmp < ListUtil.size (m_lGameScoreHistory); iSetZBTmp++ ) {
            List<ScoreLine> scoreLines = m_lGameScoreHistory.get(iSetZBTmp);
            if ( ListUtil.isEmpty(scoreLines) ) { break; }

            ScoreLine firstScoreLineOfGame = scoreLines.get(0);
            serverOfSet = firstScoreLineOfGame.getServingPlayer();
            if ( serverOfSet == null ) { continue; }

            server = ((iSetZB - iSetZBTmp) % 2 == 0) ? serverOfSet : serverOfSet.getOther();
            break;
        }
        if ( bSetServer ) {
            setServerAndSide(server, ServeSide.R, null);
        }
        return server;
    }

    void recordAppealAndCall_SQ_RL_RB(Player appealing, Call call) {
        if ( ( gameHasStarted() == false ) && (ListUtil.size(m_lGameCountHistory) <= 1)) {
            // first thing in the match is a call for a let: adjust date and time if appropriate
            adjustTheWhenObjectIfAppropriate(GameTiming.ChangedBy.FirstScoreOfGameEntered);
        }
        final Player[] wasGameBallFor = _isPossibleGameVictoryFor(When.Now, false);

        ScoreLine slCall = new ScoreLine(appealing, call);
        addScoreLine(slCall, true);

        setDirty(false); // setDirty(true) is called later on for changeScore...

        Player pointAwardedTo = null;
        switch (call) {
            case YL:
                if ( isLastPointHandout() ) {
                    setLastPointWasHandout(false);
                }
                break;
            case NL:
                pointAwardedTo = appealing.getOther();
                break;
            case ST:
                pointAwardedTo = appealing;
                break;
        }
        for(OnCallChangeListener l: onCallChangeListeners) {
            l.OnCallChanged(call, appealing, pointAwardedTo, null);
        }
        if ( pointAwardedTo != null ) {
            changeScore_SQ_RB(pointAwardedTo, true, call);
        } else {
            triggerSpecialScoreListenersIfApplicable(null);
        }
    }

    void recordConduct_SQ_RL_RB(Player pMisbehaving, Call call, ConductType conductType) {
        if ( (gameHasStarted() == false) && ListUtil.size(m_lGameCountHistory) <= 1) {
            // first thing in the match is a conduct: adjust date and time if appropriate
            adjustTheWhenObjectIfAppropriate(GameTiming.ChangedBy.FirstScoreOfGameEntered);
        }

        if ( matchHasEnded() ) {
            if ( ListUtil.isEmpty(m_lScoreHistory) ) {
                // new game was initialized after end of match
                undoLast(); // ensure the conduct warning will be added to end of the final game of the match
            }
        }

        ScoreLine slCall = new ScoreLine(pMisbehaving, call);
        addScoreLine(slCall, true);

        StringBuilder sb = new StringBuilder();
        sb.append("|");
        sb.append(JSONKey.player).append("=").append(pMisbehaving);
        sb.append("|");
        sb.append(JSONKey.call  ).append("=").append(call);
        sb.append("|");
        sb.append(JSONKey.type  ).append("=").append(conductType);
        sb.append("|");
        sb.append(JSONKey.game  ).append("=").append(getGameNrInProgress());
        sb.append("|");
        sb.append(JSONKey.score ).append("=").append(getScore(Player.A)).append("-").append(getScore(Player.B));
        sb.append("|");
        lConductCalls.add(sb.toString());

        setDirty(false);

        Player pointOrGameAwardedTo = call.hasScoreAffect() ? pMisbehaving.getOther() : null;
        for(OnCallChangeListener l: onCallChangeListeners) {
            l.OnCallChanged(call, pMisbehaving, pointOrGameAwardedTo, conductType);
        }
        if ( pointOrGameAwardedTo != null ) {
            changeScore_SQ_RB (pointOrGameAwardedTo, false, call);
        }
    }
    void changeScore_TT_BM_RL(Player player, SportType sportType)
    {
/*
        if ( this.getPlayerNames()[0].equals("Crash") && this.getPlayerNames()[1].equals("Me") && call!=null && call.isConduct() ) {
            throw new RuntimeException("Deliberate crash to see stacktrace of obfuscated code");
        }
*/
        final Call         call            = null;
        final int          iDelta          = 1;
        final Player       previousServer  = getServer();
        final DoublesServe previousDS      = m_in_out;
        final Player[]     wasMatchBallFor = _isPossibleMatchVictoryFor(When.ScoreOneMorePoint, null);

        // determine the new score
        Integer iNewScore = determineNewScoreForPlayer(player, iDelta,(SportType.Badminton.equals(sportType)) ? m_bEnglishScoring: false);

        ScoreLine scoreLine = getScoreLine(player, iNewScore, m_nextServeSide);
        if ( sportType.equals(SportType.Badminton) ) {
            determineServerAndSide_BM(player);
        } else {
            determineServerAndSide_TT_RL(false, sportType);
        }

        if ( m_gameTimingCurrent == null ) {
            m_gameTimingCurrent = new GameTiming(m_lGameTimings.size(), System.currentTimeMillis(), System.currentTimeMillis(), onTimingChangedListeners);
            m_lGameTimings.add(m_gameTimingCurrent);
        }

        if ( (iNewScore == 1) && getMinScore() == 0  ) {
            if ( ListUtil.size(m_lGameCountHistory) <= 1 ) {
                // first point of match: adjust date and time if appropriate
                adjustTheWhenObjectIfAppropriate(GameTiming.ChangedBy.FirstScoreOfGameEntered);
            }
            if ( m_lockState.equals(LockState.UnlockedManual) && matchHasEnded() ) {
                // increase number of games to win
                m_iNrOfGamesToWinMatch++;
            }
        }

        addScoreLine(scoreLine, true);

        // inform listeners
        changeScoreInformListeners(player, true, call, iDelta, previousServer, previousDS, iNewScore);

        if ( (ListUtil.singleElementEquals(wasMatchBallFor, player) ) /* was match ball for scoring player */
           || ListUtil.length(wasMatchBallFor)==2 /* was match ball for both players: e.g. gummiarm */
           ) {
            Player winner = player;
            m_possibleMatchFor.put(When.Now, new Player[] {winner});

            endGame(); // e.g. to have +xx score displayed correctly
        }
    }

    /** also called for racketlon/squash in case of appeal or conduct decision. In that case bTriggerServeSideChange=false */
    void changeScore_SQ_RB(Player player, boolean bTriggerServeSideChange, Call call)
    {
        boolean bConductGame = (call != null) && call.getScoreAffect().equals(Call.ScoreAffect.LoseGame);
        int iConductGamePoints = 0;
        if( bConductGame ) {
            iConductGamePoints = Math.max(getNrOfPointsToWinGame(), (getScore(player.getOther()) + (getTiebreakFormat().needsTwoClearPoints() ? 2 : 1))) - getScore(player);
        }
        final int          iDelta         = bConductGame ? iConductGamePoints : 1;
        final Player       previousServer = getServer();
        final DoublesServe previousDS     = m_in_out;

        // determine the new score
        Integer iNewScore = determineNewScoreForPlayer(player, iDelta, m_bEnglishScoring);

        ServeSide sCurrentSide = null;

        if ( bTriggerServeSideChange ) {
            sCurrentSide = m_nextServeSide;

            // record 'chosen' side
            if ( isLastPointHandout() ) {
                // last serve side displayed was indeed choosen/used: store that fact
                m_player2LastServeSide.put(getServer(), m_nextServeSide);

                Map<ServeSide, Integer> serveSideCount = m_player2ServeSideCount.get(getServer());
                MapUtil.increaseCounter(serveSideCount, sCurrentSide);
            }
        }

        ScoreLine scoreLine = getScoreLine(player, iNewScore, sCurrentSide);
        if ( player.equals( this.getServer() ) ) {
            // score for the server
            if ( bTriggerServeSideChange ) {
                setServerAndSide(null, m_nextServeSide.getOther(), null);
                setLastPointWasHandout(false);
            }
        } else {
            // hand-out: score for receiver
            if ( bTriggerServeSideChange ) {
                handout(player, true); // will invoke setLastPointWasHandout()
            }
        }
        if ( m_gameTimingCurrent == null ) {
            m_gameTimingCurrent = new GameTiming(this.m_lGameTimings.size(), System.currentTimeMillis(), System.currentTimeMillis(), onTimingChangedListeners);
            this.m_lGameTimings.add(m_gameTimingCurrent);
        }

        if ( (iNewScore == 1) && getMinScore() == 0  ) {
            if ( ListUtil.size(m_lGameCountHistory) <= 1 ) {
                // first point of match: adjust date and time if appropriate
                adjustTheWhenObjectIfAppropriate(GameTiming.ChangedBy.FirstScoreOfGameEntered);
            }
            if ( m_lockState.equals(LockState.UnlockedManual) && matchHasEnded() ) {
                // increase number of games to win
                m_iNrOfGamesToWinMatch++;
            }
        }

        addScoreLine(scoreLine, true);

        // inform listeners
        changeScoreInformListeners(player, bTriggerServeSideChange, call, iDelta, previousServer, previousDS, iNewScore);
    }

/*
    Player[] isPossibleGameBallFor_SQ_TT() {
        int iScoreA = MapUtil.getInt(m_scoreOfGameInProgress, Player.A, 0);
        int iScoreB = MapUtil.getInt(m_scoreOfGameInProgress, Player.B, 0);
        int max     = Math.max(iScoreA, iScoreB);
        int diff    = Math.abs(iScoreA - iScoreB);
        int iNrOfPointsToWinGame = getNrOfPointsToWinGame();
        if (max + 1 < iNrOfPointsToWinGame + m_iTieBreakPlusX ) {
            return getNoneOfPlayers();
        }
        if ( diff == 0  ) {
            if ( m_TieBreakFormat.needsTwoClearPoints() ) {
                return getNoneOfPlayers();
            }
            if ( m_bEnglishScoring ) {
                return new Player[] { getServer() };
            } else {
                // game ball for both!!
                return getPlayers();
            }
        }

        int iDiffMustBeAtLeast = m_TieBreakFormat.needsTwoClearPoints()?2:1;
        if ( (diff >= iDiffMustBeAtLeast) && (max >= iNrOfPointsToWinGame + m_iTieBreakPlusX) ) {
            // winner
            return getNoneOfPlayers();
        }

        if ( m_bEnglishScoring ) {
            if ( (iScoreA > iScoreB) && getServer().equals(Player.A)) {
                return new Player[] {Player.A};
            }
            if ( (iScoreB > iScoreA) && getServer().equals(Player.B)) {
                return new Player[] {Player.B};
            }
            return getNoneOfPlayers();
        } else {
            Player leader = (iScoreA > iScoreB) ? Player.A : Player.B;
            return new Player[] { leader };
        }
    }
*/
    Player determineServerForNextGame_SQ_BM(int iScoreA, int iScoreB) {
        Player player = (iScoreA > iScoreB) ? Player.A : Player.B;
        setServerAndSide(player, null, null);
        return player;
    }

    Player[] calculateIsPossibleGameVictoryFor_SQ_TT_BM_RL(When when, Map<Player, Integer> gameScore)
    {
        int iScoreA = MapUtil.getInt(gameScore, Player.A, 0);
        int iScoreB = MapUtil.getInt(gameScore, Player.B, 0);
        int max     = Math.max(iScoreA, iScoreB);
        int diff    = Math.abs(iScoreA - iScoreB);
        int iNrOfPointsToWinGame = getNrOfPointsToWinGame();
        switch(when) {
            case Now: {
                if ( max < iNrOfPointsToWinGame + m_iTieBreakPlusX ) {
                    return getNoneOfPlayers();
                }
                if ( diff == 0 ) {
                    return getNoneOfPlayers();
                }
                if ( m_TieBreakFormat.needsTwoClearPoints() ) {
                    if ( diff < 2 ) {
                        return getNoneOfPlayers();
                    }
                }
                if ( m_bEnglishScoring && diff > 0 ) {
                    // assumption: tiebreak format is SelectOneOrTwo
                    return new Player[]{ getServer() };
                }

                Player pLeader = (iScoreA > iScoreB) ? Player.A : Player.B;
                return new Player[] { pLeader };
            }
            case ScoreOneMorePoint: {
                if (max + 1 < iNrOfPointsToWinGame + m_iTieBreakPlusX ) {
                    // score not high enough for either player to win game with next point
                    return getNoneOfPlayers();
                }
                int iDiffMustBeAtLeast = m_TieBreakFormat.needsTwoClearPoints()?2:1;
                if ( (diff >= iDiffMustBeAtLeast) && (max >= iNrOfPointsToWinGame + m_iTieBreakPlusX) ) {
                    // game ball has passed (game has been won already)
                    return getNoneOfPlayers();
                }
                if ( m_bEnglishScoring ) {
                    if ( diff == 0 ) {
                        if ( m_TieBreakFormat.needsTwoClearPoints() ) {
                            // strange combo: english scoring but tie-break needs to clear points
                            return getNoneOfPlayers();
                        } else {
                            // score for both is high enough to win game on next point. Only server can score
                            return new Player[]{ getServer() }; // IH
                        }
                    } else {
                        // there is a leader with enough points. Only if he is serving there is game ball
                        Player pLeader = (iScoreA > iScoreB) ? Player.A : Player.B;
                        if ( pLeader.equals(getServer())) {
                            return new Player[]{ getServer() };
                        } else {
                            return getNoneOfPlayers();
                        }
                    }
                }
                if ( m_TieBreakFormat.needsTwoClearPoints() ) {
                    if ( diff == 0 ) {
                        return getNoneOfPlayers();
                    }
                    Player pLeader = (iScoreA > iScoreB) ? Player.A : Player.B;
                    return new Player[] { pLeader };
                } else {
                    // no need to have 2 points difference when next point is scored
                    if ( diff == 0 ) {
                        // both players scores are high enough to win the game
                        return getPlayers();
                    } else {
                        // only the leaders score is high enough
                        Player pLeader = (iScoreA > iScoreB) ? Player.A : Player.B;
                        return new Player[] { pLeader };
                    }
                }
            }
        }
        return getNoneOfPlayers();
    }

    /** @deprecated Use isPossibleMatchBallFor_SQ_TT_BM() in stead */
    private Player isPossibleMatchVictoryFor_SQ_TT(boolean bCheckTotalPointsIfGamesAreEqual)
    {
        Map<Player, Integer> gameCount = getGamesWon(); // getGameCount()
        int iGamesA = MapUtil.getInt(gameCount, Player.A, 0);
        int iGamesB = MapUtil.getInt(gameCount, Player.B, 0);

        if ( iGamesA==iGamesB ) {
            if ( bCheckTotalPointsIfGamesAreEqual ) {
                Map<Player, Integer> pointsWon = getTotalNumberOfPointsScored();
                int iMax = MapUtil.getMaxValue(pointsWon);
                int iMin = MapUtil.getMinValue(pointsWon);
                if ( iMax > iMin ) {
                    // player wins by points
                    return MapUtil.getMaxKey(pointsWon, null);
                } else {
                    // even points are equal: no winner
                }
            }
            return null;
        }
        if ( Math.max(iGamesA,iGamesB) < m_iNrOfGamesToWinMatch ) {
            return null;
        }
        return (iGamesA > iGamesB) ? Player.A : Player.B;
    }

    /** For logging only */
    private List<Player> getPlayersAsList(Player[] gameballForOld) {
        if ( gameballForOld == null ) { return null; }
        return Arrays.asList(gameballForOld);
    }

    //-------------------------------
    // Mode (introduced for Expedite - TT)
    //-------------------------------

    private String m_sMode = null;
    public void setMode(Object oMode) {
        m_sMode = (oMode!=null) ? String.valueOf(oMode) : null;
        // notify listeners to e.g. display different 'serve side' symbol
        triggerListeners();
    }
    public boolean isInMode(Object oMode) {
        return oMode.toString().equals(m_sMode);
    }
    public boolean isInNormalMode() {
        return (m_sMode == null);
    }

}
