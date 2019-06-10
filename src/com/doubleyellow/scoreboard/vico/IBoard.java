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

package com.doubleyellow.scoreboard.vico;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.support.percent.PercentLayoutHelper;
import android.support.percent.PercentRelativeLayout;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.doubleyellow.android.util.ColorUtil;
import com.doubleyellow.android.view.AutoResizeTextView;
import com.doubleyellow.android.view.FloatingMessage;
import com.doubleyellow.android.view.ViewUtil;

import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.prefs.*;
import com.doubleyellow.scoreboard.timer.Timer;
import com.doubleyellow.scoreboard.timer.TimerViewContainer;
import com.doubleyellow.scoreboard.timer.Type;
import com.doubleyellow.scoreboard.view.GamesWonButton;
import com.doubleyellow.util.*;

import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.history.MatchGameScoresView;
import com.doubleyellow.scoreboard.timer.SBTimerView;
import com.doubleyellow.scoreboard.model.*;
import com.doubleyellow.scoreboard.timer.TimerView;
import com.doubleyellow.scoreboard.util.SBToast;
import com.doubleyellow.scoreboard.view.GameHistoryView;
import com.doubleyellow.scoreboard.view.PlayersButton;
import com.doubleyellow.scoreboard.view.ServeButton;

import java.util.*;

/**
 * Separating the ScoreBoard view as much as possible from the ScoreBoard 'Main' activity.
 * For supporting 'ChromeCast' by using an IBoard instance as well.
 */
public class IBoard implements TimerViewContainer
{
    private static final String TAG = "SB." + IBoard.class.getSimpleName();

    public static Player               m_firstPlayerOnScreen = Player.A;
    public static Map<Player, Integer> m_player2scoreId;
    public static Map<Player, Integer> m_player2serverSideId;
    public static Map<Player, Integer> m_player2nameId;
    public static Map<Player, Integer> m_player2gamesWonId;
    public static SparseArray<Player>  m_id2player;
    static {
        m_player2scoreId      = new HashMap<Player , Integer>();
        m_player2serverSideId = new HashMap<Player , Integer>();
        m_player2nameId       = new HashMap<Player , Integer>();
        m_player2gamesWonId   = new HashMap<Player , Integer>();
        m_id2player           = new SparseArray<Player>();

        initPlayer2ScreenElements(Player.A);
    }

    public static Player togglePlayer2ScreenElements() {
        return initPlayer2ScreenElements(m_firstPlayerOnScreen.getOther());
    }
    private static Player initPlayer2ScreenElements(Player pFirst) {
        m_firstPlayerOnScreen = pFirst;

        m_player2scoreId     .put(pFirst           , R.id.btn_score1);
        m_player2scoreId     .put(pFirst.getOther(), R.id.btn_score2);

        m_player2serverSideId.put(pFirst           , R.id.btn_side1);
        m_player2serverSideId.put(pFirst.getOther(), R.id.btn_side2);

        m_player2nameId      .put(pFirst           , R.id.txt_player1);
        m_player2nameId      .put(pFirst.getOther(), R.id.txt_player2);

        m_player2gamesWonId  .put(pFirst           , R.id.btn_gameswon1);
        m_player2gamesWonId  .put(pFirst.getOther(), R.id.btn_gameswon2);

        m_id2player          .put(R.id.txt_player1 , pFirst);
        m_id2player          .put(R.id.btn_score1  , pFirst);
        m_id2player          .put(R.id.btn_side1   , pFirst);
        m_id2player          .put(R.id.txt_player2 , pFirst.getOther());
        m_id2player          .put(R.id.btn_score2  , pFirst.getOther());
        m_id2player          .put(R.id.btn_side2   , pFirst.getOther());

        return m_firstPlayerOnScreen;
    }

    private Model     matchModel = null;
    private ViewGroup m_vRoot    = null;
    private Context   context    = null;
    private Display   display    = null;
    private Chronometer.OnChronometerTickListener m_gameTimerTickListener;
    public IBoard(Model model, Context ctx, Display dsply, ViewGroup root, Chronometer.OnChronometerTickListener gameTimerTickListener) {
        m_vRoot    = root;
        matchModel = model;
        context    = ctx;
        display    = dsply;
        m_gameTimerTickListener = gameTimerTickListener;
    }
    public void setModel(Model model) {
        matchModel = model;
        updateBrandLogoBasedOnScore();
    }
    public void setView(ViewGroup view) {
        m_vRoot = view;
    }

    public void updateScore(Player player, int iScore) {
        TextView btnScore = (TextView) findViewById(m_player2scoreId.get(player));
        if ( btnScore == null ) { return; }
        String sScore = ("" + iScore).trim();
        btnScore.setText(sScore);
        if ( Player.A.equals(player) && Brand.supportsTimeout() && matchModel.getMaxScore()==0 ) {
            m_lGameXWasPausedDuration.put(matchModel.getGameNrInProgress(), 0L);
        }
    }
    public boolean updatePlayerName(Player p, String sName, boolean bIsDoubles) {
        if ( m_player2nameId == null ) {
            return true;
        }
        Integer id = m_player2nameId.get(p);
        if ( id != null ) {
            View view = findViewById(id);
            if (view instanceof TextView) {
                ((TextView) view).setText(sName);
            } else if (view instanceof PlayersButton) {
                PlayersButton pb = (PlayersButton) view;
                pb.setPlayers(sName, bIsDoubles);
                if ( true || isPresentation() ) { // TODO: make this a enum pref: 1) as big as possible but not bigger than the other, 2) as big as possible
                    pb.addListener(keepSizeInSyncTextResizeListener);
                }
            }
        }
        return false;
    }

    //-------------------------------------------
    // ensure both textview have the same textsize (If one is bigger than the other he may seem more 'important')
    //-------------------------------------------

    private AutoResizeTextView.OnTextResizeListener keepSizeInSyncTextResizeListener = new AutoResizeTextView.OnTextResizeListener() {
        private Map<AutoResizeTextView, CharSequence> mARTextView2Text = new HashMap<AutoResizeTextView, CharSequence>();
        private Map<CharSequence, Float> mPlayerTxt2CalculatedTxtSize = new HashMap<CharSequence, Float>();
        @Override public void onTextResize(AutoResizeTextView textView, float oldSize, float newSizePx, float requiredWidth, float requiredHeight) {
            final CharSequence text = textView.getText();
            final String sLogPrefix = StringUtil.pad(String.format("[%s.%s] ", showOnScreen, text), ' ', 48, true);

            if ( (mPlayerTxt2CalculatedTxtSize.size() == 2) && (mPlayerTxt2CalculatedTxtSize.containsKey(text) == false) ) {
                // rename of a player, remove current textview
                CharSequence sPreviousText = mARTextView2Text.remove(textView);
                mPlayerTxt2CalculatedTxtSize.remove(sPreviousText); // no longer use old text in size calculations
            }
            mPlayerTxt2CalculatedTxtSize.put(text, newSizePx);
          //Log.d(TAG, sLogPrefix + String.format("resized %s from %s to %s px: ", text, oldSize, newSizePx));

            float fMin = MapUtil.getMinFloatValue(mPlayerTxt2CalculatedTxtSize);
            if ( fMin < newSizePx ) {
                // already size-adjusted player name was 'renamed' but still needs to be adjust for other player name is still 'longer'
                textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, fMin);
              //Log.d(TAG, sLogPrefix + String.format("A resize %s from %s to %s px because of other: ", text, newSizePx, fMin));
            }
            for(AutoResizeTextView tv: mARTextView2Text.keySet()) {
                if (tv != textView) {
                    AutoResizeTextView tvOther = tv;
                    CharSequence sOther = tvOther.getText();
                    Float fOther = tvOther.getTextSize();
                    if ( /*fOther == null ||*/ fOther != fMin ) {
                        tvOther.setTextSize(TypedValue.COMPLEX_UNIT_PX, fMin);
                      //Log.d(TAG, sLogPrefix + String.format("B resize %s from %s to %s px: ", sOther, fOther, fMin));
                    }
                }
            }

          //Log.d(TAG, sLogPrefix + "mPlayerTxt2CalculatedTxtSize: " + mPlayerTxt2CalculatedTxtSize);
            mARTextView2Text.put(textView, text);
          //Log.d(TAG, sLogPrefix + String.format("mARTextView2Text (#%s) %s", mARTextView2Text.size(), mARTextView2Text));
        }
    };

    public void updateGameAndMatchDurationChronos() {
        updateGameDurationChrono();
        updateMatchDurationChrono();
    }

    public void stopMatchDurationChrono() {
        Chronometer tvMatchTime = (Chronometer) findViewById(R.id.sb_match_duration);
        if ( tvMatchTime == null ) { return; }
        tvMatchTime.stop();
    }
    public void updateMatchDurationChrono() {
        Chronometer tvMatchTime = (Chronometer) findViewById(R.id.sb_match_duration);
        if ( tvMatchTime == null ) { return; }

        boolean bShowMatchTimer = PreferenceValues.showMatchDuration(context, isPresentation());
        tvMatchTime.setVisibility(bShowMatchTimer?View.VISIBLE:View.GONE);

        if ( bShowMatchTimer ) {
            String sChar = getOAString(context, R.string.oa_match_firstletter);
            String sFormat = sChar.toUpperCase() + ": %s";
            tvMatchTime.setFormat(sFormat);

            long elapsedRealtime = SystemClock.elapsedRealtime();
            long lBootTime       = System.currentTimeMillis() - elapsedRealtime;
            long lStartTime      = matchModel.getMatchStart();
            long calculatedBase  = lStartTime - lBootTime;
            if ( matchModel.matchHasEnded() /*|| matchModel.isLocked()*/ ) {
                String sDuration = DateUtil.convertDurationToHHMMSS_Colon(matchModel.getDuration());
                tvMatchTime.setText(String.format(sFormat, sDuration));
                tvMatchTime.stop();
            } else if ( ScoreBoard.timer != null && ScoreBoard.timer.isShowing() && (ScoreBoard.timer.timerType == Type.Warmup) ) {
                tvMatchTime.stop();
            } else {
                tvMatchTime.setBase(roundToNearest1000(calculatedBase));
                tvMatchTime.start();
            }
        }
    }

    public void resumeGameDurationChrono() {
        Chronometer tvGameTime = (Chronometer) findViewById(R.id.sb_game_duration);
        if ( tvGameTime == null ) { return; }
        if ( Brand.supportsTimeout() && (m_lStoppedAt != 0L) ) {
            // adjust so no jump in time is visible and e.g. Expedite is not triggered to early.
            // TODO: this adjustment will not be visible in e.g. match overview. There timestamp of start and end of game are used to calculate game duration
            long lPaused                = System.currentTimeMillis() - m_lStoppedAt;
            int  gameNrInProgress       = matchModel.getGameNrInProgress();
            Long lGameWasPausedDuration = m_lGameXWasPausedDuration.get(gameNrInProgress);
            if (lGameWasPausedDuration == null ) {
                lGameWasPausedDuration  = lPaused;
            } else {
                lGameWasPausedDuration += lPaused;
            }
            m_lGameXWasPausedDuration.put(gameNrInProgress, lGameWasPausedDuration);
            Log.d(TAG, "Game " + gameNrInProgress + " has total time paused " + lGameWasPausedDuration + " " + DateUtil.convertDurationToHHMMSS_Colon(lGameWasPausedDuration));
            m_lStoppedAt = 0L;
            updateGameDurationChrono();
            //tvGameTime.setBase(tvGameTime.getBase() + lPaused);
        }
        //tvGameTime.start();
    }
    private static long m_lStoppedAt = 0L;
    private static Map<Integer, Long> m_lGameXWasPausedDuration = new HashMap<Integer,Long>();
    public void stopGameDurationChrono() {
        Chronometer tvGameTime = (Chronometer) findViewById(R.id.sb_game_duration);
        if ( tvGameTime == null ) { return; }
        tvGameTime.stop();
        if ( Brand.supportsTimeout() ) {
            m_lStoppedAt = System.currentTimeMillis();
            Log.d(TAG, "Stopped at " + m_lStoppedAt + " " + new Date(m_lStoppedAt).toString());
        }
        if ( matchModel.matchHasEnded() ) {
            showDurationOfLastGame(tvGameTime);
        }
    }
    public void updateGameDurationChrono() {
        Chronometer tvGameTime = (Chronometer) findViewById(R.id.sb_game_duration);
        if ( tvGameTime == null ) { return; }

        boolean bShowGameTimer = PreferenceValues.showLastGameDuration(context, isPresentation());
        tvGameTime.setVisibility(bShowGameTimer?View.VISIBLE:View.GONE);
        int iGameNrZeroBased = matchModel.getNrOfFinishedGames();

        if ( bShowGameTimer ) {
            String sFormat = getGameDurationFormat(iGameNrZeroBased);
            tvGameTime.setFormat(sFormat);

            if ( matchModel.hasStarted() == false ) {
                m_lGameXWasPausedDuration = new HashMap<>();
            }
            if ( matchModel.getMaxScore() <= 1 ) {
                m_lStoppedAt = 0L;
            }

            if ( matchModel.matchHasEnded() || matchModel.isLocked() ) {
                showDurationOfLastGame(tvGameTime);
            } else if ( (ScoreBoard.timer != null) && ScoreBoard.timer.isShowing() && (Type.TowelingDown.equals(ScoreBoard.timer.timerType)==false)) {
                // show duration of last finished game
                showDurationOfLastGame(tvGameTime);
            } else {
                long lElapsedSinceBoot = SystemClock.elapsedRealtime();
                long lBootTime         = System.currentTimeMillis() - lElapsedSinceBoot;
              //Log.d(TAG, "lBootTime at " + lBootTime + " " + new Date(lBootTime).toString());
                long lStartTime        = matchModel.getLastGameStart();
                long lPauseInProgress = 0L;
                if ( m_lStoppedAt != 0L ) {
                    // if e.g. screen rotates while toweling down timer was running
                    lPauseInProgress  = System.currentTimeMillis() - m_lStoppedAt;
                    Log.d(TAG, "Pause In Progress " + lPauseInProgress + " " + DateUtil.convertDurationToHHMMSS_Colon(lPauseInProgress));
                }
                Long lPaused = m_lGameXWasPausedDuration.get(iGameNrZeroBased+1);
                long calculatedBase    = lStartTime - lBootTime + (lPaused==null?0L:lPaused) + lPauseInProgress ;
                if ( calculatedBase < 0 ) {
                    Log.w(TAG, "calculatedBase < 0 (" + calculatedBase + "). Using 0 ...");
                }
                tvGameTime.setBase(roundToNearest1000(calculatedBase));
                if ( lPauseInProgress == 0 ) {
                    tvGameTime.start();
                }
            }

            if ( m_gameTimerTickListener != null ) {
                tvGameTime.setOnChronometerTickListener(m_gameTimerTickListener);
            }
        }
    }

    /** to ensure after a pause of gametimer, the 'tick' is in sync */
    private long roundToNearest1000(long lBase) {
        long lMod = lBase % 1000;
        lBase -= lMod;
        if ( lMod >= 500L ) {
            lBase += 1000L;
        }
        return Math.max(lBase, 0L);
    }

    /** sets game duration to static value using model data, not Chrono data */
    private void showDurationOfLastGame(Chronometer tvGameTime) {
        List<GameTiming>           lTimes        = matchModel.getTimes();
        int iGameNrZeroBased = Math.max(0, matchModel.getNrOfFinishedGames() - 1); // Math.max for in case no games where finished yet
        if ( matchModel.isPossibleGameVictory() ) {
            iGameNrZeroBased++;
        }
        if ( ListUtil.size(lTimes) < iGameNrZeroBased+1 ) {
            // timing details got out of sync somehow
            return;
        }
        GameTiming gameTiming = lTimes.get(iGameNrZeroBased);
        long duration = gameTiming.getDuration();
        Long lPaused = m_lGameXWasPausedDuration.get(iGameNrZeroBased+1);
        if ( lPaused != null ) {
            duration -= lPaused;
            duration = Math.max(duration, 0L);
        }
        String sDuration = DateUtil.convertDurationToHHMMSS_Colon(duration);
        tvGameTime.stop();
        tvGameTime.setText(String.format(getGameDurationFormat(iGameNrZeroBased), sDuration));
    }

    private String getGameDurationFormat(int iGameNrZeroBased) {
        String sChar = getOAString(context, R.string.oa_game_firstletter);
        if ( Brand.isRacketlon() ) {
            sChar = getOAString(context, R.string.oa_set).substring(0, 1);
        }
        return sChar.toUpperCase() + (iGameNrZeroBased+1) + ": %s";
    }

    public void updateServeSide(Player player, DoublesServe doublesServe, ServeSide nextServeSide, boolean bIsHandout) {
        if ( player == null ) { return; } // normally only e.g. for undo's of 'altered' scores
        int iServeId = m_player2serverSideId.get(player);
        ServeButton btnSide = ( ServeButton ) findViewById(iServeId);
        if ( btnSide == null ) { return; }
        int iTransparencyNonServer = PreferenceValues.getServeButtonTransparencyNonServer(context);
        EnumSet<ShowPlayerColorOn> showPlayerColorOn = PreferenceValues.showPlayerColorOn(context);
        if ( showPlayerColorOn.contains(ShowPlayerColorOn.ServeSideButton) ) {
            if ( StringUtil.hasNonEmpty(matchModel.getColor(Player.A), matchModel.getColor(Player.B)) ) {
                iTransparencyNonServer = 0;
            }
        }

        Object oDisplayValueOverwrite = serverSideDisplayValue(nextServeSide, bIsHandout);
        String sDisplayValueOverwrite = btnSide.setServeString(oDisplayValueOverwrite, iTransparencyNonServer);
        btnSide.setEnabled(true || bIsHandout);

        int iNameId = m_player2nameId.get(player);
        View view = findViewById(iNameId);
        if ( view instanceof PlayersButton ) {
            PlayersButton v = (PlayersButton) view;
            v.setServer(doublesServe, nextServeSide, bIsHandout, sDisplayValueOverwrite);
        }
/*
        if ( matchModel.getDoubleServeSequence().equals(DoublesServeSequence.ABXY) ) {
            String sSSFilename = Util.filenameForAutomaticScreenshot(context, matchModel, showOnScreen, -1, -1, null);
            if ( sSSFilename!=null ) {
                PreferenceValues.requestPermission(context, PreferenceKeys.targetDirForImportExport, Manifest.permission.WRITE_EXTERNAL_STORAGE);
                ViewUtil.takeScreenShot(context, Brand.brand, sSSFilename, vRoot.getRootView());
            }
        } else if (player==m_firstPlayerOnScreen ) {
            String sSSFilename = Util.filenameForAutomaticScreenshot(context, matchModel, showOnScreen, 4, 5, null);
            if ( sSSFilename!=null ) {
                PreferenceValues.requestPermission(context, PreferenceKeys.targetDirForImportExport, Manifest.permission.WRITE_EXTERNAL_STORAGE);
                ViewUtil.takeScreenShot(context, Brand.brand, sSSFilename, vRoot.getRootView());
            }
        }
*/
    }

    /** update the old fashioned paper scoring sheet */
    public void updateScoreHistory(boolean bOnlyAddLast)
    {
        if ( PreferenceValues.showScoringHistoryInMainScreen(context, isPresentation()) == false ) { return; }

        GameHistoryView currentGameScoreLines = (GameHistoryView) findViewById(R.id.scorehistorytable);
        if ( currentGameScoreLines == null) { return; }
        if ( currentGameScoreLines.getVisibility() != View.VISIBLE ) { return; }

        List<ScoreLine> history = matchModel.getScoreHistory();
        currentGameScoreLines.setStretchAllColumns(false);
        //int textSize = PreferenceValues.getHistoryTextSize(this);
        //currentGameScoreLines.setTextSizePx(textSize);
        currentGameScoreLines.setScoreLines(history, matchModel.getHandicapFormat(), matchModel.getGameStartScoreOffset(Player.A), matchModel.getGameStartScoreOffset(Player.B));
        currentGameScoreLines.update(bOnlyAddLast);
    }

    public void toggleGameScoreView() {
        GameScoresAppearance appearance = PreferenceValues.getGameScoresAppearance(context);
                             appearance = ListUtil.getNextEnum(appearance);
        PreferenceValues.setEnum(PreferenceKeys.gameScoresAppearance, context, appearance);
        setGameScoreView(appearance);
    }
    public void initGameScoreView() {
        GameScoresAppearance appearance = PreferenceValues.getGameScoresAppearance(context);
        setGameScoreView(appearance);
    }
    private void setGameScoreView(GameScoresAppearance appearance) {
        for (Player player : Player.values()) {
            int iNameId = m_player2gamesWonId.get(player);
            View vGamesWon = findViewById(iNameId);
            if ( vGamesWon == null ) {
                Log.w(TAG, "No GamesWon buttons to work with in orientation/view " + ViewUtil.getCurrentOrientation(context));
                break;
            }
            boolean showGamesWon = appearance.showGamesWon(isPresentation());
            vGamesWon.setVisibility(showGamesWon ? View.VISIBLE : View.INVISIBLE);
        }

        MatchGameScoresView matchGameScores = (MatchGameScoresView) findViewById(R.id.gamescores);
        if ( matchGameScores != null ) {
            if ( appearance.showGamesWon(isPresentation()) ) {
                matchGameScores.setVisibility(View.INVISIBLE);
            } else {
                matchGameScores.setVisibility(View.VISIBLE);
            }
        }
    }

    public void updateGameScores() {
        MatchGameScoresView matchGameScores = (MatchGameScoresView) findViewById(R.id.gamescores);
        if ( matchGameScores != null) {

            matchGameScores.setOnTextResizeListener(matchGamesScoreSizeListener);

            //int textSize = PreferenceValues.getHistoryTextSize(this);
            //matchGameScores.setTextSizeDp(textSize);

            matchGameScores.update(matchModel, m_firstPlayerOnScreen);
        }

        if ( (m_player2gamesWonId != null) && (matchModel != null) ) {
            Map<Player, Integer> gamesWon = matchModel.getGamesWon(false);
            if ( MapUtil.isNotEmpty(gamesWon) ) {
                for(Player player: Player.values() ) {
                    int iNameId = m_player2gamesWonId.get(player);
                    View view = findViewById(iNameId);
                    if ( view instanceof GamesWonButton) {
                        GamesWonButton v = (GamesWonButton) view;
                        v.setGamesWon(gamesWon.get(player));
                    }
                }
            } else {
                // set both to zero?
                Log.d(TAG, "Games won is empty map");
            }
        }
    }
    private static int iTxtSizePx_FinishedGameScores = 0;
    public  static int iTxtSizePx_PaperScoringSheet  = 0;
    /** For matching the text size of the 'final score' of games with the 'paper score' sheet */
    private AutoResizeTextView.OnTextResizeListener matchGamesScoreSizeListener = new AutoResizeTextView.OnTextResizeListener() {
        @Override public void onTextResize(AutoResizeTextView textView, float oldSize, float newSize, float requiredWidth, float requiredHeight) {

          //Log.d(TAG, String.format("Ols size %f, New size %f, Required width: %f, Required height, %f", oldSize, newSize, requiredWidth, requiredHeight));
            boolean landscapeOrientation = ViewUtil.isLandscapeOrientation(context);

            float fReducePS = landscapeOrientation?0.60f:0.70f;
            int iPaperScoreTextSize = (int) (newSize * fReducePS);

            float fReduceCM = landscapeOrientation ? 0.50f : 0.60f;
            if ( isPresentation() ) {
                fReduceCM = 0.7f;
            }
            int iChronoTextSize = (int) (newSize * fReduceCM);

            if ( isPresentation() == false ) {
                // store size for usage is child activities
                iTxtSizePx_PaperScoringSheet = iPaperScoreTextSize;
                iTxtSizePx_FinishedGameScores = (int) newSize;
            }

            GameHistoryView gameHistoryView = (GameHistoryView) findViewById(R.id.scorehistorytable);
            if ( gameHistoryView != null ) {
                gameHistoryView.setTextSizePx(iPaperScoreTextSize);
            }
            int[] iIds = new int[] {R.id.sb_match_duration, R.id.sb_game_duration};
            for(int iResId: iIds) {
                Chronometer cm = (Chronometer) findViewById(iResId);
                if ( cm != null ) {
                    cm.setTextSize(TypedValue.COMPLEX_UNIT_PX, iChronoTextSize);
                }
            }
        }
    };


    private Object serverSideDisplayValue(ServeSide serveSide, boolean bIsHandout) {
        if ( serveSide == null) { return ""; }
        return getServeSideCharacter(serveSide, bIsHandout);
    }
    private Object getServeSideCharacter(ServeSide serveSide, boolean bIsHandout) {
        String sChar   = PreferenceValues.getOAString(context, serveSide.getSingleCharResourceId());
        String sHOChar = (bIsHandout?"?":"");
        Object o = matchModel.convertServeSideCharacter(sChar, serveSide, sHOChar);
        if ( Brand.isNotSquash() && (o instanceof Integer) ) {
            DownUp downUp = PreferenceValues.numberOfServiceCountUpOrDown(context);
            if ( downUp.equals(DownUp.Up) && (matchModel.isInTieBreak_Racketlon_Tabletennis() == false) ) {
                int iCountDownValue = (int) o;
                int iCountUpValue = matchModel.getNrOfServesPerPlayer() - iCountDownValue + 1;
                o = iCountUpValue;
            }
        }
        return o;
    }

    public boolean undoGameBallColorSwitch() {
        return doGameBallColorSwitch(Player.values(), false);
    }
    private boolean doGameBallColorSwitch(Player[] players, boolean bHasGameBall) {
        if ( PreferenceValues.indicateGameBall(context) == false ) {
            return false;
        }
        if ( StringUtil.hasNonEmpty(matchModel.getColor(Player.A), matchModel.getColor(Player.B) ) ) {
            // do not allow for now, we would loose the colored border
            // TODO: allow but keep the border
            return false;
        }

        Map<ColorPrefs.ColorTarget, Integer> mColors = ColorPrefs.getTarget2colorMapping(context);
        Integer scoreButtonBgd = mColors.get(ColorPrefs.ColorTarget.scoreButtonBackgroundColor);
        Integer scoreButtonTxt = mColors.get(ColorPrefs.ColorTarget.scoreButtonTextColor);
        if ( bHasGameBall ) {
            Integer tmpBgColor  = mColors.get(ColorPrefs.ColorTarget.playerButtonBackgroundColor);
            Integer tmpTxtColor = mColors.get(ColorPrefs.ColorTarget.playerButtonTextColor);
            if ( tmpBgColor.equals(scoreButtonBgd) && tmpTxtColor.equals(scoreButtonTxt) ) {
                // fallback to 'inverse' if player buttons have the same colors as score buttons
                tmpBgColor  = scoreButtonTxt;
                tmpTxtColor = scoreButtonBgd;
            }
            if ( tmpBgColor == null || tmpTxtColor == null ) {
                return true;
            }
            scoreButtonBgd = tmpBgColor;
            scoreButtonTxt = tmpTxtColor;
        }
        if ( bHasGameBall == false && players==null ) {
            players = Player.values();
        }
        for(Player player:players) {
            TextView btnScore = (TextView) findViewById(m_player2scoreId.get(player));
            if ( btnScore == null ) { continue; }
            setBackgroundColor(btnScore, scoreButtonBgd);
            btnScore.setTextColor(scoreButtonTxt);
        }
        return false;
    }

    private View findViewById(int id) {
        return m_vRoot.findViewById(id);
    }
    public Map<ColorPrefs.ColorTarget, Integer> mColors;

    public void initColors(Map<ColorPrefs.ColorTarget, Integer> mColors)
    {
        this.mColors = mColors;

        Integer mainBgColor = mColors.get(ColorPrefs.ColorTarget.backgroundColor);

        for( Player player: Model.getPlayers() ) {
            if ( m_player2serverSideId != null ) {
                int id = m_player2serverSideId.get(player);
                ServeButton ssView = (ServeButton) findViewById(id);
                if ( ssView != null ) {
                    setBackgroundColor(ssView, mColors.get(ColorPrefs.ColorTarget.serveButtonBackgroundColor));
                    ssView.setForegroundColor(mColors.get(ColorPrefs.ColorTarget.serveButtonTextColor));
                }
            }

            Integer scoreBgColor  = mColors.get(ColorPrefs.ColorTarget.scoreButtonBackgroundColor);
            Integer scoreTxtColor = mColors.get(ColorPrefs.ColorTarget.scoreButtonTextColor);
            if ( m_player2scoreId != null ) {
                int id = m_player2scoreId.get(player);
                TextView txtView = (TextView) findViewById(id);
                if ( txtView != null ) {
                    txtView.setTextColor(scoreTxtColor);
                    if ( (scoreBgColor != null) && scoreBgColor.equals(mainBgColor) ) {
                        // set border equal to text color if score bg color is to close to main bgcolor (monochrome)
                        setBackgroundAndBorder(txtView, scoreBgColor, scoreTxtColor);
                    } else {
                        setBackgroundColor(txtView, scoreBgColor);
                    }
                }
            }

            if ( m_player2gamesWonId != null ) {
                int iNameId = m_player2gamesWonId.get(player);
                View view = findViewById(iNameId);
                if ( view instanceof GamesWonButton ) {
                    GamesWonButton txtView = (GamesWonButton) view;
                    txtView.setTextColor(scoreTxtColor);
                    setBackgroundColor(txtView, scoreBgColor);
                }
            }

            if ( m_player2nameId != null ) {
                int id = m_player2nameId.get(player);
                View view = findViewById(id);
                Integer pbBgColor  = mColors.get(ColorPrefs.ColorTarget.playerButtonBackgroundColor);
                Integer pbTxtColor = mColors.get(ColorPrefs.ColorTarget.playerButtonTextColor);
                if ( view instanceof TextView ) {
                    ((TextView) view).setTextColor(pbTxtColor);
                    if ( (pbBgColor != null) && pbBgColor.equals(mainBgColor) ) {
                        // set border color equal to text color if name bg color is to close to main bgcolor (monochrome)
                        setBackgroundAndBorder(view, pbBgColor, pbTxtColor);
                    } else {
                        setBackgroundColor(view, pbBgColor);
                    }
                }
                if ( view instanceof PlayersButton) {
                    PlayersButton v = (PlayersButton) view;
                    v.setBackgroundColor      (pbBgColor);
                    v.setTextColor            (pbTxtColor);
                    v.setBackgroundColorServer(mColors.get(ColorPrefs.ColorTarget.serveButtonBackgroundColor));
                    v.setTextColorServer      (mColors.get(ColorPrefs.ColorTarget.serveButtonTextColor));
                }
            }

            // overwrite color of certain gui elements if colors are specified in the model
            if ( matchModel != null ) {
                String sColor = matchModel.getColor(player);
                initPerPlayerColors(player, sColor, null);
            }
        }

        TextView tvDivision = (TextView) findViewById(R.id.btn_match_field_division);
        if ( tvDivision != null ) {
            setBackgroundColor(tvDivision, mColors.get(ColorPrefs.ColorTarget.playerButtonBackgroundColor));
            tvDivision.setTextColor(mColors.get(ColorPrefs.ColorTarget.playerButtonTextColor));
        }

        GameHistoryView gameHistory = (GameHistoryView) findViewById(R.id.scorehistorytable);
        if ( gameHistory != null ) {
            gameHistory.setProperties( mColors.get(ColorPrefs.ColorTarget.historyBackgroundColor)
                                     , mColors.get(ColorPrefs.ColorTarget.historyTextColor), 0);
        }
        MatchGameScoresView matchGameScores = (MatchGameScoresView) findViewById(R.id.gamescores);
        if ( matchGameScores != null ) {
            Integer iBgColorLoser  = mColors.get(ColorPrefs.ColorTarget.scoreButtonTextColor);
            Integer iBgColorWinner = mColors.get(ColorPrefs.ColorTarget.scoreButtonBackgroundColor);
            Integer ibgColorTimes  = mColors.get(ColorPrefs.ColorTarget.serveButtonBackgroundColor);
            Integer iBgColorPlayer = mColors.get(ColorPrefs.ColorTarget.playerButtonBackgroundColor);

            // if main bg color is closer to bgColorWinner than to bgColorLoser, let winner stand out more by switching colors
            Integer iBgMain = mColors.get(ColorPrefs.ColorTarget.backgroundColor);
            long iDeltaW = ColorPrefs.getColorDistance(iBgColorWinner, iBgMain);
            long iDeltaL = ColorPrefs.getColorDistance(iBgColorLoser , iBgMain);
            if ( iDeltaL > iDeltaW ) {
                Integer iTmp = iBgColorLoser;
                               iBgColorLoser = iBgColorWinner;
                                               iBgColorWinner = iTmp;
            }

            matchGameScores.setProperties( iBgColorLoser
                                         , iBgColorWinner
                                         , ibgColorTimes
                                         , iBgColorPlayer
                                         );
        }
        int[] iIds = new int[] {R.id.sb_match_duration, R.id.sb_game_duration};
        for(int iResId: iIds) {
            Chronometer cm = (Chronometer) findViewById(iResId);
            if ( cm == null ) { continue; }
            cm.setTextColor      (mColors.get(ColorPrefs.ColorTarget.scoreButtonTextColor));
            cm.setBackgroundColor(context.getResources().getColor(android.R.color.transparent));
        }

        View vRoot = findViewById(R.id.squoreboard_root_view);
        if ( vRoot != null ) {
            Integer clBackGround = mColors.get(ColorPrefs.ColorTarget.backgroundColor);
            vRoot.setBackgroundColor(clBackGround);
        }
        if ( gameBallMessage != null ) {
            gameBallMessage.setVisibility(View.GONE);
            gameBallMessage = null; // so that it is re-created with correct colors
        }
        if ( decisionMessages != null ) {
            for( int i = 0 ; i < decisionMessages.length; i++ ) {
                if ( decisionMessages[i] != null ) {
                    decisionMessages[i].setVisibility(View.GONE);
                    decisionMessages[i] = null; // so that it is re-created with correct colors
                }
            }
        }
    }

    public void setBluetoothIconVisibility(int visibility) {
        View vIcon = m_vRoot.findViewById(R.id.sb_bluetooth_icon);
        if ( vIcon != null ) {
            vIcon.setVisibility(visibility);
        } else {
            Log.w(TAG, "No bluetooticon view found ...");
        }
    }

    private SBTimerView sbTimerView = null;
    @Override public TimerView getTimerView() {
        boolean presentation = isPresentation();
        if ( sbTimerView != null ) { return sbTimerView; }

        TextView btnTimer = (TextView) findViewById(R.id.btn_timer);
        if ( btnTimer == null ) { return null; }
        Chronometer cmToLate = (Chronometer) findViewById(R.id.sb_to_late_timer);
        if ( cmToLate != null ) {
            cmToLate.setVisibility(View.INVISIBLE);
        }
        boolean bUseAlreadyUpFor_Chrono = (presentation == false);
        if ( (bUseAlreadyUpFor_Chrono == false) || (PreferenceValues.showTimeIsAlreadyUpFor_Chrono(context) == false) ) {
            cmToLate = null;
        }
        sbTimerView = new SBTimerView(btnTimer, cmToLate, context, this);
        return sbTimerView;
    }

    private TextView m_tvFieldDivision = null;
    public void initFieldDivision() {
        m_tvFieldDivision = (TextView) findViewById(R.id.btn_match_field_division);
        if ( m_tvFieldDivision == null ) { return; }

        if ( matchModel == null ) {
            m_tvFieldDivision.setVisibility(View.GONE);
            return;
        }

        updateFieldDivisionBasedOnScore();
    }
    public void updateFieldDivisionBasedOnScore() {
        if ( m_tvFieldDivision == null ) { return; }

        boolean bShowFieldDivision = PreferenceValues.showFieldDivision(context, isPresentation());
        if ( bShowFieldDivision == false ) {
            m_tvFieldDivision.setText("");
            m_tvFieldDivision.setVisibility(View.GONE);
            return;
        }

        String sField = matchModel.getEventDivision();
        m_tvFieldDivision.setText(sField);
        if ( StringUtil.isNotEmpty(sField) ) {
            m_tvFieldDivision.setVisibility(View.VISIBLE);
            if ( PreferenceValues.hideFieldDivisionWhenGameInProgress(context) ) {
                m_tvFieldDivision.setVisibility(matchModel.gameHasStarted() ? View.INVISIBLE : View.VISIBLE);
            }
        } else {
            m_tvFieldDivision.setVisibility(View.GONE);
        }
    }

    private ImageView m_ivBrandLogo = null;
    public void initBranded()
    {
        final int brandViewId         = Brand.getImageViewResId();
        final int brandLogoDrawableId = Brand.getLogoResId();
        for(int iResId: Brand.imageViewIds ) {
            ImageView ivBrandLogo = (ImageView) findViewById(iResId);
            if ( ivBrandLogo != null ) {
                if ( iResId != brandViewId) {
                    ivBrandLogo.setVisibility(View.GONE);
                    continue;
                }
                if ( PreferenceValues.showBrandLogo(context, isPresentation()) == false ) {
                    // don't show logo on device for branded squore: it clutters with e.g. 'speak' button
                    ivBrandLogo.setVisibility(View.INVISIBLE);
                    continue;
                }
                ivBrandLogo.setVisibility(View.VISIBLE);
                if ( brandLogoDrawableId != 0 ) {
                    m_ivBrandLogo = ivBrandLogo;
                    ivBrandLogo.setImageResource(brandLogoDrawableId);
                    //ivBrandLogo.setBackgroundColor(Brand.getBgColor(context));
                    ivBrandLogo.setBackgroundColor(context.getResources().getColor(android.R.color.transparent));
                    ivBrandLogo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                } else {
                    ivBrandLogo.setVisibility(View.INVISIBLE);
                }
            }
        }
        updateBrandLogoBasedOnScore();

        if ( Brand.isTabletennis() ) {
            // give game scores a little more space for default best of 7 of tabletennis
            View view = findViewById(R.id.gamescores);
            if ( view != null ) {
                PercentRelativeLayout.LayoutParams plParams = (PercentRelativeLayout.LayoutParams) view.getLayoutParams();
                // This will currently return null, if it was not constructed from XML.
                if ( plParams != null ) {
                    PercentLayoutHelper.PercentLayoutInfo info = plParams.getPercentLayoutInfo();
                    if ( ViewUtil.isPortraitOrientation(context) ) {
                        // height is set to fixed percentage: increase w/h aspect ratio from 300% to 450 to make it wider
                        info.aspectRatio = info.aspectRatio * 3 / 2;
                    } else {
                        // width is set to fixed percentage: decrease w/h aspect ratio from 40% to make it higher
                        info.aspectRatio = info.aspectRatio * 2 / 3;
                    }
                    view.requestLayout();
                }
            }

            // now that gamescores has more room, timer has to little if we keep the xml setting: change it so timer is rightalligned to parent
            view = findViewById(R.id.btn_timer);
            if ( view != null ) {
                RelativeLayout.LayoutParams rlParams = (RelativeLayout.LayoutParams) view.getLayoutParams();
                if ( rlParams != null ) {
                    if ( ViewUtil.isPortraitOrientation(context) ) {
                        rlParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 ) {
                            rlParams.removeRule(RelativeLayout.ALIGN_RIGHT);
                        }
                    }
                    view.requestLayout();
                }
            }
        }
    }

    public void updateBrandLogoBasedOnScore() {
        if ( m_ivBrandLogo == null ) { return; }
        if ( matchModel    == null ) { return; }
        final int brandLogoDrawableId = Brand.getLogoResId();
        if ( brandLogoDrawableId == 0 ) { return; }
        if ( PreferenceValues.showBrandLogo( context, isPresentation() ) ) {
            if ( PreferenceValues.hideBrandLogoWhenGameInProgress(context) ) {
                m_ivBrandLogo.setVisibility(matchModel.gameHasStarted() ? View.INVISIBLE : View.VISIBLE);
            }
        }
    }

    public void initTimerButton() {
        View btnTimer = findViewById(R.id.btn_timer);
        if ( btnTimer != null ) {
            btnTimer.setVisibility(View.INVISIBLE);
        }
        final TextView tvToLate = (TextView) findViewById(R.id.sb_to_late_timer);
        if ( tvToLate != null ) {
            tvToLate.setVisibility(View.INVISIBLE);
        }
        // because the little chronometer is not an AutoResizeTextView we 'emulate' this by installing a onResizeListener on its 'parent': the actual timer
        if ( btnTimer instanceof AutoResizeTextView ) {
            AutoResizeTextView arTimer = (AutoResizeTextView) btnTimer;
            arTimer.addOnResizeListener(new AutoResizeTextView.OnTextResizeListener() {
                @Override public void onTextResize(AutoResizeTextView textView, float oldSize, float newSizePx, float requiredWidth, float requiredHeight) {
                    float fRatio = 0.35f;
                    if ( ViewUtil.isPortraitOrientation(context) ) {
                        fRatio = 0.6f;
                    }
                    float size = newSizePx * fRatio;
                    if ( tvToLate != null ) {
                        //float iOOTBsize = tvToLate.getTextSize();
                        tvToLate.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
                    }
                }
            });
        }
        // enforce recreate
        Timer.removeTimerView(isPresentation(), this.sbTimerView);
        this.sbTimerView = null;
    }

    public void updatePlayerClub(Player p, String sClub) {
        // TODO: abbreviate if to long

        View view = findViewById(m_player2nameId.get(p));
        if (view instanceof PlayersButton) {
            PlayersButton button = (PlayersButton) view;
            button.setClub(sClub);
        }
    }
    public void updatePlayerCountry(Player p, String sCountry) {
        EnumSet<ShowCountryAs> countryPref = PreferenceValues.showCountryAs(context);
        View view = findViewById(m_player2nameId.get(p));
        if (view instanceof PlayersButton) {
            PlayersButton button = (PlayersButton) view;

            boolean bHideBecauseSameCountry = false;
            if (PreferenceValues.hideFlagForSameCountry(context) && StringUtil.isNotEmpty(sCountry) ) {
                String sOtherCountry = matchModel.getCountry(p.getOther());
                bHideBecauseSameCountry = sCountry.equalsIgnoreCase(sOtherCountry);
            }
            boolean bShowAsTextAbbr = countryPref.contains(ShowCountryAs.AbbreviationAfterName);
            boolean bShowAsFlagPref = (countryPref.contains(ShowCountryAs.FlagNextToNameChromeCast) && (isPresentation() == true))
                                   || (countryPref.contains(ShowCountryAs.FlagNextToNameOnDevice  ) && (isPresentation() == false));

            button.setCountry(sCountry, bShowAsTextAbbr, bShowAsFlagPref && (bHideBecauseSameCountry == false));
        }
    }
    public void updatePlayerAvatar(Player p, String sAvatar) {
        EnumSet<ShowAvatarOn> avatarPref = PreferenceValues.showAvatarOn(context);
        boolean bShowAvatar = (avatarPref.contains(ShowAvatarOn.OnChromeCast) && (isPresentation() == true))
                           || (avatarPref.contains(ShowAvatarOn.OnDevice    ) && (isPresentation() == false));

        if ( bShowAvatar ) {
            View view = findViewById(m_player2nameId.get(p));
            if (view instanceof PlayersButton) {
                PlayersButton button = (PlayersButton) view;
                button.setAvatar(sAvatar);
            }
        }
    }

    public void initPerPlayerColors(Player p, String sColor, String sColorPrev) {
        EnumSet<ShowPlayerColorOn> colorOns = PreferenceValues.showPlayerColorOn(context);

        Integer iPlayerColor = null;
        Integer iTxtColor    = null;
        if ( StringUtil.isNotEmpty(sColor) ) {
            iPlayerColor  = Color.parseColor(sColor);
            // switch color of text to black or white depending on chosen color
            iTxtColor = ColorUtil.getBlackOrWhiteFor(sColor);
        }

        if ( colorOns.contains(ShowPlayerColorOn.ServeSideButton) ) {
            initPerPlayerViewWithColors(p, m_player2serverSideId, iPlayerColor, iTxtColor, ColorPrefs.ColorTarget.serveButtonBackgroundColor, ColorPrefs.ColorTarget.serveButtonTextColor);
        }

        if ( colorOns.contains(ShowPlayerColorOn.ScoreButtonBorder) ) {
            View view = findViewById(m_player2scoreId.get(p));
            if ( view != null ) {
                Integer iScoreButtonBgColor = mColors.get(ColorPrefs.ColorTarget.scoreButtonBackgroundColor);
                if ( StringUtil.isNotEmpty(sColorPrev) ) {
                    int iPrevColor = Color.parseColor(sColorPrev);
                    mViewId2BorderColor.remove(view.getId());
                }
                if ( iPlayerColor == null ) {
                    setBackgroundColor(view, iScoreButtonBgColor);
                } else {
                    setBackgroundAndBorder(view, iScoreButtonBgColor, iPlayerColor);
                }
            }

            //ColorPrefs.setBackground(view, scoreButtonBgd);

            //initPerPlayerViewWithColors(p, m_player2scoreId, iBgColor, iTxtColor, ColorPrefs.ColorTarget.scoreButtonBackgroundColor, ColorPrefs.ColorTarget.scoreButtonTextColor);
        }

        if ( colorOns.contains(ShowPlayerColorOn.PlayerButton) ) {
            initPerPlayerViewWithColors(p, m_player2nameId, iPlayerColor, iTxtColor, ColorPrefs.ColorTarget.playerButtonBackgroundColor, ColorPrefs.ColorTarget.playerButtonTextColor);
        }
/*
        if ( colorOns.contains(ShowPlayerColorOn.GameBallMessage) ) {
            if ( (iBgColor != null) && (iTxtColor != null) ) {
                if ( this.gameBallMessage  == null ) {
                    showGameBallMessage(true, null);
                }
                this.gameBallMessage.setColors(iBgColor, iTxtColor);
                updateGameBallMessage();
            }
        }
*/
    }

    private void setBackgroundColor(View view, Integer iBgColor) {
        //Log.d(TAG, "change bgcolor of " + view + " = " + iBgColor);
        //ColorUtil.setBackground(view, iBgColor);
        setBackgroundAndBorder(view, iBgColor, mViewId2BorderColor.get(view.getId()));
    }

    private Map<Integer, Integer> mViewId2BorderColor = new HashMap<>();
    private void setBackgroundAndBorder(View view, Integer iBgColor, Integer iBorderColor) {
        if ( iBgColor == null ) {
            return;
        }
        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN ) { /* 16 */
            //Log.d(TAG, "change color of " + view + " bg=" + iBgColor + ", brdr=" + iBorderColor);
            int[] colors = new int[] {iBgColor, iBgColor};
            GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, colors);
            // gd.setShape(GradientDrawable.RECTANGLE); // RECTANGLE is the default
            //gd.setCornerRadius(getResources().getDimension(R.dimen.sb_button_radius));
            gd.setCornerRadius(getScreenHeightWidthMinimumFraction(R.fraction.player_button_corner_radius));
            int viewId = view.getId();
            if ( iBorderColor != null ) {
                gd.setStroke(getScreenHeightWidthMinimumFraction(R.fraction.player_button_colorborder_thickness), iBorderColor);
                mViewId2BorderColor.put(viewId, iBorderColor);
            } else {
                mViewId2BorderColor.remove(viewId);
            }
            view.setBackground(gd);
            view.invalidate();
        }
    }

    private void initPerPlayerViewWithColors(Player p, Map<Player, Integer> mPlayer2ViewId, Integer iBgColor, Integer iTxtColor, ColorPrefs.ColorTarget bgColorDefKey, ColorPrefs.ColorTarget txtColorDefKey) {
        if (mPlayer2ViewId == null) { return; }

        if ( iBgColor == null ) {
            iBgColor  = mColors.get(bgColorDefKey);
            iTxtColor = mColors.get(txtColorDefKey);
        }
        Integer id = mPlayer2ViewId.get(p);
        if ( (id == null) || (iBgColor == null) || (iTxtColor == null) ) { return; }

        View view = findViewById(id);
        if (view == null) { return; }

        if ( view instanceof ServeButton ) {
            ServeButton serveButton = (ServeButton) view;
            setBackgroundColor(serveButton, iBgColor);
            serveButton.setForegroundColor(iTxtColor);
        } else if ( view instanceof PlayersButton ) {
            PlayersButton button = (PlayersButton) view;
            if (bgColorDefKey.equals(ColorPrefs.ColorTarget.serveButtonBackgroundColor)) {
                button.setTextColorServer(iTxtColor);
                button.setBackgroundColorServer(iBgColor);
            } else {
                button.setTextColor(iTxtColor);
                button.setBackgroundColor(iBgColor);
            }
        } else if ( view instanceof TextView ) {
            TextView txtView = (TextView) view;
            setBackgroundColor(txtView, iBgColor);
            txtView.setTextColor(iTxtColor);
        }
    }

    // ----------------------------------------------------
    // -----------------game ball/match ball message ------
    // ----------------------------------------------------
    private FloatingMessage gameBallMessage = null;
    public boolean updateGameBallMessage() {
        return updateGameBallMessage(null, null);
    }
    public boolean updateGameBallMessage(Player[] gameBallFor, Boolean bShow) {
        if ( gameBallFor == null ) {
            gameBallFor = matchModel.isPossibleGameBallFor();
            bShow       = ListUtil.isNotEmpty(gameBallFor);
        }
/*
        if ( bShow ) {
            final String sSSFilename = Util.filenameForAutomaticScreenshot(context, matchModel, showOnScreen, -1, -1, null);
            if (sSSFilename != null) {
                new Handler().postDelayed(new Runnable() {
                    @Override public void run() {
                        ViewUtil.takeScreenShot(context, Brand.brand, sSSFilename, vRoot.getRootView());
                    }
                }, 1000); // little time needed for gameball message to appear completely
            }
        }
*/
        doGameBallColorSwitch(gameBallFor, bShow);

        return showGameBallMessage(bShow, gameBallFor);
    }

    private boolean showGameBallMessage(boolean bVisible, Player[] pGameBallFor) {
        if ( PreferenceValues.floatingMessageForGameBall(context, isPresentation()) == false ) {
            if ( gameBallMessage != null ) { gameBallMessage.setHidden(true); } // e.g. preferences where changed
            return false;
        }
        if ( (gameBallMessage == null) && (bVisible == false) ) {
            // we no longer return : make the gameball message floater anyway. This ensures that 'decision' floaters are created later and are more on top in the view hierarchy
            //return false;
        }

        int iResId = Brand.isRacketlon()? R.string.oa_set_ball : R.string.oa_gameball;
        Player[] possibleMatchBallFor = matchModel.isPossibleMatchBallFor();
        if ( ListUtil.isNotEmpty(possibleMatchBallFor) ) {
            iResId = R.string.oa_matchball;

            if ( Brand.isRacketlon() && bVisible ) {
                // it can be matchball for the OTHER player
                pGameBallFor = possibleMatchBallFor;

                // if it is matchball for 2 players at once, it is gummiarm
                if ( ListUtil.length(pGameBallFor) == 2 ) {
                    iResId = R.string.oa_gummiarm_point;
                }
            }
        }
        String sMsg = PreferenceValues.getOAString(context, iResId);
        if ( gameBallMessage == null ) {
            ColorPrefs.ColorTarget bgColor  = ColorPrefs.ColorTarget.serveButtonBackgroundColor;
            ColorPrefs.ColorTarget txtColor = ColorPrefs.ColorTarget.serveButtonTextColor;
            Direction direction = Direction.S;
            if ( ViewUtil.isPortraitOrientation(context) ) {
                direction = Direction.None; // middle of the screen (not 'over' player names)
            }

            int iWidthPx = getScreenHeightWidthMinimumFraction(R.fraction.pt_gameball_msg_width);
          //Log.d(TAG, String.format("GBM width: %s, (is presentation: %s)", iWidthPx, isPresentation()));
            FloatingMessage.Builder builder = new FloatingMessage.Builder(context, iWidthPx);
            if ( mColors != null ) {
                builder.withButtonColors(mColors.get(bgColor), mColors.get(txtColor));
            }
            gameBallMessage = builder
                    .withMessage(sMsg)
                    .withGravity(direction.getGravity())
                    .withMargins(16, 0)
                    .create(true, m_vRoot);
        } else {
            if ( bVisible ) {
                gameBallMessage.setText(sMsg);
            }
        }
        EnumSet<ShowPlayerColorOn> colorOns = PreferenceValues.showPlayerColorOn(context);
        if ( colorOns.contains(ShowPlayerColorOn.GameBallMessage) ) {
            if ( pGameBallFor != null && pGameBallFor.length == 1) {
                Player p = pGameBallFor[0];
                String sColor = matchModel.getColor(p);
                if ( StringUtil.isNotEmpty(sColor) ) {
                    int iBgColor  = Color.parseColor(sColor);
                    int iTxtColor = ColorUtil.getBlackOrWhiteFor(sColor);
                    gameBallMessage.setColors(iBgColor, iTxtColor);
                }
            }
        }

        gameBallMessage.setHidden(bVisible == false);
        return bVisible;
    }

    // ----------------------------------------------------
    // -----------------decision message             ------
    // ----------------------------------------------------
    private FloatingMessage[] decisionMessages = new FloatingMessage[3]; // one for each player and a 'big' one for conducts (TODO: this last one)
    private void showDecisionMessage(Player pDecisionFor, Call call, String sMsg, int iMessageDurationSecs) {
        if ( getBlockToasts() ) { return; }
        final int fmIdx;
        if ( call.isConduct() ) {
            fmIdx = 2;
        } else {
            fmIdx = pDecisionFor.equals(m_firstPlayerOnScreen)?0:1;
        }
        if ( fmIdx >= decisionMessages.length ) {
            return;
        }
        if ( decisionMessages[fmIdx] == null ) {
            ColorPrefs.ColorTarget bgColor  = ColorPrefs.ColorTarget.serveButtonBackgroundColor;
            ColorPrefs.ColorTarget txtColor = ColorPrefs.ColorTarget.serveButtonTextColor;
            Direction direction = null;
            if ( ViewUtil.isPortraitOrientation(context) ) {
                direction = Direction.None;
            } else {
                if ( call.isConduct() ) {
                    direction = Direction.S;
                } else {
                    // place message over player requesting it
                    direction = pDecisionFor.equals(m_firstPlayerOnScreen) ? Direction.W : Direction.E;
                }
            }
            final int iResId_widthFraction = call.isConduct() ? R.fraction.pt_conduct_width : R.fraction.pt_decision_msg_width;
            int iWidthPx = getScreenHeightWidthMinimumFraction(iResId_widthFraction);
            FloatingMessage.Builder builder = new FloatingMessage.Builder(context, iWidthPx);
            if ( mColors != null ) {
                builder.withButtonColors(mColors.get(bgColor), mColors.get(txtColor));
            }
            decisionMessages[fmIdx] = builder
                    .withGravity(direction.getGravity())
                    .withMargins(16, 0)
                    .create(true, m_vRoot);
        }
        boolean bHide = StringUtil.isEmpty(sMsg);
        if ( bHide == false ) {
            decisionMessages[fmIdx].setText(sMsg);
        }
        EnumSet<ShowPlayerColorOn> colorOns = PreferenceValues.showPlayerColorOn(context);
        if ( colorOns.contains(ShowPlayerColorOn.DecisionMessage) ) {
            if ( pDecisionFor != null ) {
                String sColor = matchModel.getColor(pDecisionFor);
                if ( StringUtil.isNotEmpty(sColor) ) {
                    int iBgColor  = Color.parseColor(sColor);
                    int iTxtColor = ColorUtil.getBlackOrWhiteFor(sColor);
                    decisionMessages[fmIdx].setColors(iBgColor, iTxtColor);
                }
            }
        }

        decisionMessages[fmIdx].setHidden(bHide);
        if ( (bHide == false) && (iMessageDurationSecs > 0) ) {
            // auto hide in x secs
            CountDownTimer countDownTimer = new CountDownTimer(iMessageDurationSecs * 1000, 1000) {
                @Override public void onTick(long millisUntilFinished) { }
                @Override public void onFinish() {
                    if (decisionMessages[fmIdx] != null) {
                        decisionMessages[fmIdx].setHidden(true);
                    }
                }
            };
            countDownTimer.start();
        }
    }

    // ------------------------------------------------------
    // Informative messages (e.g. change sides for Racketlon)
    // ------------------------------------------------------

    private static final Call info_message_dummycall =Call.CW;  // use CW here just to let it appear as 'centered'...
    public void showMessage(String sMsg,int iMessageDuration) {
        showDecisionMessage(null, info_message_dummycall, sMsg, iMessageDuration);
    }
    public void hideMessage() {
        showDecisionMessage(null, info_message_dummycall, null, -1); // use CW here as well
    }

    // ------------------------------------------------------
    // Decisions
    // ------------------------------------------------------

    public void showChoosenDecision(Call call, Player appealingOrMisbehaving, ConductType conductType) {
        String         sMsg                = getCallMessage(call, appealingOrMisbehaving, appealingOrMisbehaving, conductType, matchModel);
        int            iDurationDefaultRes = call.isConduct() ? R.integer.showChoosenDecisionDuration_Conduct_default : R.integer.showChoosenDecisionDuration_Appeal_default;
        PreferenceKeys keys                = call.isConduct() ? PreferenceKeys.showChoosenDecisionDuration_Conduct    : PreferenceKeys.showChoosenDecisionDuration_Appeal;
        int            iMessageDuration    = PreferenceValues.getInteger(keys, context, context.getResources().getInteger(iDurationDefaultRes));
        //int            iSize               = PreferenceValues.getCallResultMessageTextSize(context);

        showDecisionMessage(appealingOrMisbehaving, call, sMsg, iMessageDuration);
        //showToast(sMsg, ColorPrefs.ColorTarget.serveButtonBackgroundColor, ColorPrefs.ColorTarget.serveButtonTextColor, iSize, iMessageDuration, Direction.None);
    }
    private String getCallMessage(Call call, Player asking, Player misbehaving, ConductType conductType, Model model) {
        switch (call) {
            case CW:
                return getOAString(context, R.string.oa_conduct_warning_x_for_type_y, model.getName(misbehaving), ViewUtil.getEnumDisplayValue(context, R.array.ConductType_DisplayValues, conductType));
            case CS:
                return getOAString(context, R.string.oa_conduct_x__stroke_to_y_for_type_t, model.getName(misbehaving), model.getName(misbehaving.getOther()), ViewUtil.getEnumDisplayValue(context, R.array.ConductType_DisplayValues, conductType));
            case CG:
                return getOAString(context, R.string.oa_conduct_x__game_to_y_for_type_t, model.getName(misbehaving), model.getName(misbehaving.getOther()), ViewUtil.getEnumDisplayValue(context, R.array.ConductType_DisplayValues, conductType));
            case NL:
                return getOAString(context, R.string.oa_no_let);
            case ST:
                if ( isPresentation() ) {
                    // in the presentation screen we display the message underneath the player anyway
                    return getOAString(context, R.string.oa_stroke);
                } else {
                    return getOAString(context, R.string.oa_stroke_to_x, model.getName(asking));
                }
            case YL:
                return getOAString(context, R.string.oa_yes_let);
        }
        return "";
    }

    private static String getOAString(Context context, int iResId, Object ... formats) {
        return PreferenceValues.getOAString(context, iResId, formats );
    }
    private String getOAString(int iResId, Object ... formats) {
        return PreferenceValues.getOAString(context, iResId, formats );
    }


    // ------------------------------------------------------
    // Toasts
    // ------------------------------------------------------

    public void showToast(int iResId) {
        showToast(context.getString(iResId, (Object[]) null));
    }
    /** block toast and dialogs. E.g. during showcase */
    private static boolean bBlockToasts = false;
    public static void setBlockToasts(boolean b) {
        bBlockToasts = b;
    }
    public static boolean getBlockToasts() {
        return bBlockToasts;
    }

    public void showToast(String sMessage) {
        showToast(sMessage, 3, Direction.None);
    }
    public void showToast(String sMessage, int iDuration, Direction direction) {
        showToast(sMessage, ColorPrefs.ColorTarget.serveButtonBackgroundColor, ColorPrefs.ColorTarget.serveButtonTextColor, PreferenceValues.getCallResultMessageTextSize(context), iDuration, direction);
    }
    private void showToast(String sMessage, ColorPrefs.ColorTarget ctBg, ColorPrefs.ColorTarget ctTxt, int iSize, int iDuration, Direction direction) {
        //Toast.makeText(context, sMessage, Toast.LENGTH_SHORT).show();

        if ( getBlockToasts()  ) { return; }
        if ( iDuration <= 0    ) { return; }
        if ( direction == null ) { return; }
        Map<ColorPrefs.ColorTarget, Integer> colors = mColors;
        if ( MapUtil.isEmpty(colors) ) { return; }
        Integer bgColor  = colors.get(ctBg);
        Integer txtColor = colors.get(ctTxt);
        if ( bgColor == null || txtColor == null ) {
            return; // prevent NullPointerException when trying to convert Integer being null to an int
        }
        if ( StringUtil.length(sMessage) < 10 ) {
            // add a few non breaking spaces
            sMessage = StringUtil.lrpad(sMessage, (char) 160, 20); // 160 is non breaking space
        }
        if ( isPresentation() ) {
            // TODO: A toast does not seem to work on the presentation: it is displayed on the android device as well
        } else {
            SBToast toast = new SBToast(context, sMessage, direction.getGravity(), bgColor, txtColor, m_vRoot, null, iSize);
            toast.show(iDuration);
        }
    }

    private ShowOnScreen showOnScreen = null;
    public boolean isPresentation() {
        if ( showOnScreen == null ) {
            // Presentation.class here since it was only introducted in API 17 JELLY_BEAN_MR1
            showOnScreen = context.getClass().getName().contains("android.app.Presentation") ? ShowOnScreen.OnChromeCast : ShowOnScreen.OnDevice;
        }
        return showOnScreen.equals(ShowOnScreen.OnChromeCast);
    }

    private int getScreenHeightWidthMinimumFraction(int iResIdFraction) {
        return ViewUtil.getScreenHeightWidthMinimumFraction(display, iResIdFraction, context);
    }
}
