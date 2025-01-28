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
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.CountDownTimer;
import android.os.SystemClock;
import androidx.percentlayout.widget.PercentLayoutHelper;
import androidx.percentlayout.widget.PercentRelativeLayout;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Space;
import android.widget.TextView;

import com.doubleyellow.android.util.ColorUtil;
import com.doubleyellow.android.view.AutoResizeTextView;
import com.doubleyellow.android.view.FloatingMessage;
import com.doubleyellow.android.view.ViewUtil;

import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.cast.ICastHelper;
import com.doubleyellow.scoreboard.cast.framework.CastHelper;
import com.doubleyellow.scoreboard.history.GSMTieBreakType;
import com.doubleyellow.scoreboard.history.GSMUtil;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.model.Util;
import com.doubleyellow.scoreboard.prefs.*;
import com.doubleyellow.scoreboard.timer.Timer;
import com.doubleyellow.scoreboard.timer.TimerViewContainer;
import com.doubleyellow.scoreboard.timer.Type;
import com.doubleyellow.scoreboard.view.GamesWonButton;
import com.doubleyellow.scoreboard.view.ScorePlusSmallScore;
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


import org.json.JSONArray;
import org.json.JSONObject;

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
    public static Map<Player, Integer> m_player2SetsWonId;
    public static Map<Player, Integer> m_player2PowerPlayIconId;
    public static SparseArray<Player>  m_id2player;
    static {
        m_player2scoreId         = new HashMap<Player , Integer>();
        m_player2serverSideId    = new HashMap<Player , Integer>();
        m_player2nameId          = new HashMap<Player , Integer>();
        m_player2gamesWonId      = new HashMap<Player , Integer>();
        m_player2SetsWonId       = new HashMap<Player , Integer>();
        m_player2PowerPlayIconId = new HashMap<Player , Integer>();
        m_id2player              = new SparseArray<Player>();

        initPlayer2ScreenElements(Player.A);
    }

    public static Player togglePlayer2ScreenElements() {
        return initPlayer2ScreenElements(m_firstPlayerOnScreen.getOther());
    }
    public static Player initPlayer2ScreenElements(Player pFirst) {
        m_firstPlayerOnScreen = pFirst;

        m_player2scoreId     .put(pFirst           , R.id.btn_score1);
        m_player2scoreId     .put(pFirst.getOther(), R.id.btn_score2);

        m_player2serverSideId.put(pFirst           , R.id.btn_side1);
        m_player2serverSideId.put(pFirst.getOther(), R.id.btn_side2);

        m_player2nameId      .put(pFirst           , R.id.txt_player1);
        m_player2nameId      .put(pFirst.getOther(), R.id.txt_player2);

        m_player2gamesWonId  .put(pFirst           , R.id.btn_gameswon1);
        m_player2gamesWonId  .put(pFirst.getOther(), R.id.btn_gameswon2);

        m_player2SetsWonId   .put(pFirst           , R.id.btn_setswon1);
        m_player2SetsWonId   .put(pFirst.getOther(), R.id.btn_setswon2);

        m_player2PowerPlayIconId.put(pFirst           , R.id.sb_powerplay1_icon);
        m_player2PowerPlayIconId.put(pFirst.getOther(), R.id.sb_powerplay2_icon);

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
        Integer id = m_player2scoreId.get(player);
        TextView btnScore = (TextView) findViewById(id);
        if ( btnScore == null ) { return; }
        String sScore = ("" + iScore).trim();
        if ( Brand.isGameSetMatch() && (matchModel instanceof GSMModel) ) {
            GSMModel gsmModel = (GSMModel) matchModel;
            sScore = gsmModel.translateScore(player, iScore);
            if ( iScore >= 3 /* 3 so it also works if going back from AD */ ) {
                // opponent 'display' score may go e.g. from AD back to 40
                Player   opp         = player.getOther();
                String   sScoreOpp   = gsmModel.translateScore(opp, gsmModel.getScore(opp));
                Integer  idOpp       = m_player2scoreId.get(opp);
                TextView btnScoreOpp = (TextView) findViewById(idOpp);
                btnScoreOpp.setText(sScoreOpp);

                castChangeViewTextMessage(idOpp, sScoreOpp);
            }
        }
        btnScore.setText(sScore);
        if ( Player.A.equals(player) && Brand.supportsTimeout() && matchModel.getMaxScore()==0 ) {
            m_lGameXWasPausedDuration.put(matchModel.getGameNrInProgress(), 0L);
        }
        if ( castChangeViewTextMessage(id, sScore) ) {
            // casting is working, also send data to update graph
            if ( Brand.isGameSetMatch() == false && matchModel.gameHasStarted() ) {
                StringBuilder sb = new StringBuilder(ICastHelper.GameGraph_Show);
                sb.append("(");
                sb.append(matchModel.getGameNrInProgress()).append(",");
                sb.append(matchModel.getNrOfPointsToWinGame()).append(",");

                StringBuilder sbScorers = new StringBuilder();
                {
                    List<ScoreLine> scoreHistory = matchModel.getGameScoreHistory();
                    for (ScoreLine scoreLine : scoreHistory) {
                        Player scoringPlayer = scoreLine.getScoringPlayer();
                        if (scoringPlayer == null) { continue; }
                        sbScorers.append(scoringPlayer.toString());
                    }
                }
                sb.append("\"").append(sbScorers.toString()).append("\"");
                sb.append(",").append(matchModel.isPossibleGameVictory());
                sb.append(")");
                castSendFunction(sb.toString());
            }
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
                pb.setPlayers(sName, bIsDoubles, p);
                if ( true || isPresentation() ) { // TODO: make this a enum pref: 1) as big as possible but not bigger than the other, 2) as big as possible
                    pb.addListener(keepSizeInSyncTextResizeListener);
                }
            }
            castChangeViewTextMessage(id, sName);
        }
        return false;
    }

    //-------------------------------------------
    // if casting is ON (old way of casting)
    //-------------------------------------------

    private ShowOnScreen showOnScreen = null;
    /** @Deprecated */
    public boolean isPresentation() {
        if ( showOnScreen == null ) {
            showOnScreen = context.getClass().getName().contains("android.app.Presentation") ? ShowOnScreen.OnChromeCast : ShowOnScreen.OnDevice;
        }
        return showOnScreen.equals(ShowOnScreen.OnChromeCast);
    }

    //-------------------------------------------
    // if casting is ON (new way of casting)
    //-------------------------------------------
    private boolean castChangeViewTextMessage(Integer iBoardResId, Object oValue) {
        return castSendChangeViewMessage(iBoardResId, oValue, ICastHelper.Property_Text);
    }
    private void castSendFunction(String s) {
        if ( castHelper == null ) { return; }
        castHelper.sendFunction(s);
    }

    // sProperty = background-color
    private boolean castSendChangeViewMessage(Integer iBoardResId, Object oValue, String sProperty) {
        if ( castHelper == null ) { return false; }
        return castHelper.sendChangeViewMessage(iBoardResId, oValue, sProperty);
    }
    private boolean castSendChangeViewMessage(String sResName, Object oValue, String sProperty) {
        if ( castHelper == null ) { return false; }
        return castHelper.sendChangeViewMessage(sResName, oValue, sProperty);
    }
    private CastHelper castHelper = null;
    public void setCastHelper(CastHelper castHelper) {
        this.castHelper = castHelper;
        castHelper.setIBoard(this);
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
        updateSetDurationChrono(); // GSM
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
        setVisibility(tvMatchTime, bShowMatchTimer?View.VISIBLE:View.GONE);

        if ( bShowMatchTimer ) {
            String sChar = getOAString(context, R.string.oa_match_firstletter);
            String sFormat = sChar.toUpperCase() + ": %s";
            tvMatchTime.setFormat(sFormat);

            long elapsedRealtime = SystemClock.elapsedRealtime();
            long lBootTime       = System.currentTimeMillis() - elapsedRealtime;
            long lStartTime      = matchModel.getMatchStart();
            long calculatedBase  = lStartTime - lBootTime;
            if ( matchModel.matchHasEnded() /*|| matchModel.isLocked()*/ ) {
                long   duration       = matchModel.getDuration();
                String sDurationMM_SS = DateUtil.convertDurationToHHMMSS_Colon(duration);
                tvMatchTime.setText(String.format(sFormat, sDurationMM_SS));
                tvMatchTime.stop();
                castSendChronosFunction( ICastHelper.MatchChrono_update, DateUtil.convertToSeconds(duration) , false, sFormat, sDurationMM_SS);
            } else if ( (ScoreBoard.timer != null) && ScoreBoard.timer.isShowing() && (ScoreBoard.timer.timerType == Type.Warmup) ) {
                tvMatchTime.stop();
            } else {
                long base = roundToNearest1000(calculatedBase);
                tvMatchTime.setBase(base);
                tvMatchTime.start();
                long   duration       = System.currentTimeMillis() - matchModel.getMatchStart(); // dynamic duration
                String sDurationMM_SS = DateUtil.convertDurationToHHMMSS_Colon(duration);
                castSendChronosFunction(ICastHelper.MatchChrono_update, DateUtil.convertToSeconds(duration), true, sFormat, sDurationMM_SS);
            }
        } else {
            castSendFunction(ICastHelper.MatchChrono_hide + "()");
        }

    }
    public void stopSetDurationChrono() {
        Chronometer tvSetTime = (Chronometer) findViewById(R.id.sb_set_duration);
        if ( tvSetTime == null ) { return; }

        tvSetTime.stop();
        if ( Brand.supportsTimeout() ) {
            m_lStoppedAt = System.currentTimeMillis();
            Log.d(TAG, "[Set] Stopped at " + m_lStoppedAt + " " + new Date(m_lStoppedAt).toString());
        }
        if ( matchModel.matchHasEnded() ) {
            showDurationOfLastSet(tvSetTime);
        }
    }
    /** only for GSMModel matches */
    public void updateSetDurationChrono() {
        Chronometer tvSetTime = (Chronometer) findViewById(R.id.sb_set_duration);
        if ( tvSetTime == null ) { return; }

        if ( Brand.isGameSetMatch() == false ) {
            tvSetTime.setVisibility(View.GONE);
            return;
        }

        // TODO: hide during first set since it has the same value as match duration

        boolean bShowSetTimer = PreferenceValues.showMatchDuration(context, isPresentation());
        tvSetTime.setVisibility(bShowSetTimer?View.VISIBLE:View.GONE);

        if ( bShowSetTimer ) {
            GSMModel matchModel = (GSMModel) this.matchModel;
            int setNrInProgress1B = matchModel.getSetNrInProgress();
            String sFormat = getSetDurationFormat(setNrInProgress1B);
            tvSetTime.setFormat(sFormat);

            long elapsedRealtime = SystemClock.elapsedRealtime();
            long lBootTime       = System.currentTimeMillis() - elapsedRealtime;
            long lStartTime      = matchModel.getSetStart(setNrInProgress1B);
            long calculatedBase  = lStartTime - lBootTime;
            long lSetDuration    = matchModel.getSetDuration(setNrInProgress1B);
            String sDurationHH_MM = DateUtil.convertDurationToHHMMSS_Colon(lSetDuration);
            if ( matchModel.matchHasEnded() /*|| matchModel.isLocked()*/ ) {
                if ( lSetDuration == 0 ) {
                    // assume we just calculate set duration of set that is not to be played. look back to last set
                    setNrInProgress1B--;
                    lSetDuration   = matchModel.getSetDuration(setNrInProgress1B);
                    sDurationHH_MM = DateUtil.convertDurationToHHMMSS_Colon(lSetDuration);
                    sFormat        = getSetDurationFormat(setNrInProgress1B);
                }
                tvSetTime.setText(String.format(sFormat, sDurationHH_MM));
                tvSetTime.stop();
                castSendChronosFunction(ICastHelper.SetChrono_update, DateUtil.convertToSeconds(lSetDuration), false, sFormat, sDurationHH_MM);
            } else if ( ScoreBoard.timer != null && ScoreBoard.timer.isShowing() && (ScoreBoard.timer.timerType == Type.Warmup) ) {
                tvSetTime.stop();
                castSendChronosFunction(ICastHelper.SetChrono_update, DateUtil.convertToSeconds(lSetDuration), false, sFormat, sDurationHH_MM);
            } else {
                tvSetTime.setBase(roundToNearest1000(calculatedBase));
                tvSetTime.start();

                castSendChronosFunction(ICastHelper.SetChrono_update, DateUtil.convertToSeconds(lSetDuration), true, sFormat, sDurationHH_MM);
            }
        } else {
            castSendFunction(ICastHelper.SetChrono_hide + "()");
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
                long base = roundToNearest1000(calculatedBase);
                tvGameTime.setBase(base);
                if ( lPauseInProgress == 0 ) {
                    tvGameTime.start();
                }
                if ( castHelper != null ) {
                    List<GameTiming> times = matchModel.getTimes(); // returns setTimes for GSM
                    if ( Brand.isGameSetMatch() ) {
                        GSMModel gsmModel = (GSMModel) matchModel;
                        times = gsmModel.getGameTimes();
                    }
                    if ( ListUtil.isNotEmpty(times) ) {
                        GameTiming last = ListUtil.getLast(times);
                        long lDuration = System.currentTimeMillis() - last.getStart();
                        long lDurationInSecs = DateUtil.convertToSeconds(lDuration);
                        castSendChronosFunction(ICastHelper.GameChrono_update, lDurationInSecs, true, sFormat, DateUtil.convertDurationToHHMMSS_Colon(lDuration));
                    }
                }
            }

            if ( m_gameTimerTickListener != null ) {
                tvGameTime.setOnChronometerTickListener(m_gameTimerTickListener);
            }
        } else {
            castSendFunction(ICastHelper.GameChrono_hide + "()");
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
        String sDurationHH_MM_SS = DateUtil.convertDurationToHHMMSS_Colon(duration);
        tvGameTime.stop();
        String sFormat = getGameDurationFormat(iGameNrZeroBased);
        tvGameTime.setText(String.format(sFormat, sDurationHH_MM_SS));

        castSendChronosFunction(ICastHelper.GameChrono_update, DateUtil.convertToSeconds(duration), false, sFormat, sDurationHH_MM_SS);
    }

    /** sets set duration to static value using model data, not Chrono data */
    private void showDurationOfLastSet(Chronometer tvSetTime) {
        if ( Brand.isGameSetMatch() == false ) { return; }

        GSMModel matchModel = (GSMModel) this.matchModel;
        int setNrInProgress1B = matchModel.getSetNrInProgress();
        String sFormat = getSetDurationFormat(setNrInProgress1B);
        //tvSetTime.setFormat(sFormat);

/*
        long elapsedRealtime = SystemClock.elapsedRealtime();
        long lBootTime       = System.currentTimeMillis() - elapsedRealtime;
        long lStartTime      = matchModel.getSetStart(setNrInProgress1B);
        long calculatedBase  = lStartTime - lBootTime;
*/
        long lSetDuration    = matchModel.getSetDuration(setNrInProgress1B);
        String sDurationHH_MM_SS = DateUtil.convertDurationToHHMMSS_Colon(lSetDuration);
        tvSetTime.stop();
        tvSetTime.setText(String.format(sFormat, sDurationHH_MM_SS));

        castSendChronosFunction(ICastHelper.SetChrono_update, DateUtil.convertToSeconds(lSetDuration), false, sFormat, sDurationHH_MM_SS);
    }

    private void castSendChronosFunction(String sFunc, long lDurationInSecs, boolean bStartTimer, String sFormat, String sDisplayValue)
    {
        boolean bShow = PreferenceValues.showMatchDuration(context, true);

        //String sCharML = getOAString(context, R.string.oa_match_firstletter);
        if ( sFunc.equals(ICastHelper.SetChrono_update) ) {
            bShow = PreferenceValues.showLastGameDuration(context, true);
            //sCharML = getOAString(context, R.string.oa_set_firstletter_GSM);
        } else if (sFunc.equals(ICastHelper.GameChrono_update)) {
            bShow = PreferenceValues.showLastGameDuration(context, true);
            //sCharML = getOAString(context, R.string.oa_game_firstletter);
        }

        if ( bShow ) {
            castSendFunction(sFunc + "(" + lDurationInSecs + "," + bStartTimer + ",'" + sFormat + "'" + ",'" + sDisplayValue + "'" + ")");
        } else {
            castSendFunction(sFunc.replaceFirst("update", "hide") + "()");
        }
    }

    private String getGameDurationFormat(int iGameNrZeroBased) {
        String sChar = getOAString(context, R.string.oa_game_firstletter);
        if ( Brand.isRacketlon() ) {
            sChar = getOAString(context, R.string.oa_set).substring(0, 1);
        }
        return sChar.toUpperCase() + (iGameNrZeroBased+1) + ": %s";
    }

    /** GSMModel only */
    private String getSetDurationFormat(int iSetNrOneBased) {
        String sChar = getOAString(context, R.string.oa_set).substring(0, 1);
        return sChar.toUpperCase() + (iSetNrOneBased) + ": %s";
    }

    public void updateReceiver(Player player, DoublesServe dsReceiver) {
        //if ( Brand.supportChooseServeOrReceive() == false ) { return; } // continue always: also used to 'clean' any characters
        int iReceiveId = m_player2serverSideId.get(player);
        ServeButton btnSide = ( ServeButton ) findViewById(iReceiveId);
        if ( btnSide == null ) { return; }
        int iTransparencyNonServer = PreferenceValues.getServeButtonTransparencyNonServer(context);
        EnumSet<ShowPlayerColorOn> showPlayerColorOn = PreferenceValues.showPlayerColorOn(context);
        if ( showPlayerColorOn.contains(ShowPlayerColorOn.ServeSideButton) ) {
            if ( StringUtil.hasNonEmpty(matchModel.getColor(Player.A), matchModel.getColor(Player.B)) ) {
                iTransparencyNonServer = 0;
            }
        }
        btnSide.setServeString(" ", iTransparencyNonServer);

        int iNameId = m_player2nameId.get(player);
        PlayersButton pbReceiver = (PlayersButton) findViewById(iNameId);
        View view = findViewById(iNameId);
        if ( view instanceof PlayersButton ) {
            if ( Brand.supportChooseServeOrReceive() ) {
                if ( Brand.isBadminton() && matchModel.isDoubles() ) {
                    if ( player.equals(IBoard.m_firstPlayerOnScreen) == false ) {
                        // DoublesServe of server and receiver are always equal (model)
                        // to always have receiver diagonally of server visually, swap for right side team
                        dsReceiver = dsReceiver.getOther();
                    }
                }
            }
            pbReceiver.setReceiver(dsReceiver, player);
        }
        castChangeViewTextMessage(iReceiveId, ""); // TODO: how about doubles and receiver indication
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

        Object oDisplayValueOverwrite = Util.getServeSideCharacter(context, matchModel, nextServeSide, bIsHandout);
        String sDisplayValueOverwrite = btnSide.setServeString(oDisplayValueOverwrite, iTransparencyNonServer);
        btnSide.setEnabled(true || bIsHandout);

        int iNameId = m_player2nameId.get(player);
        View view = findViewById(iNameId);
        if ( view instanceof PlayersButton ) {
            PlayersButton pbServer = (PlayersButton) view;
            if ( Brand.supportChooseServeOrReceive() ) {
                if ( Brand.isBadminton() && matchModel.isDoubles() ) {
                    if ( player.equals(IBoard.m_firstPlayerOnScreen) == false ) {
                        // for team on right of scoreboard: do the same, but for visual feedback let it seem as if keep R=I and L=O and in sync
                        doublesServe = doublesServe.getOther();
                    }
                }
            }

            pbServer.setServer(doublesServe, nextServeSide, bIsHandout, sDisplayValueOverwrite, player);
        }
        castChangeViewTextMessage(iServeId, sDisplayValueOverwrite);
        if ( player.equals(matchModel.getServer()) ) {
            boolean bSwapAAndB = Player.B.equals(m_firstPlayerOnScreen);
            castSendFunction(ICastHelper.Server_update + "('" + matchModel.getServer() + "'," + bSwapAAndB + ")");
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

        List<ScoreLine> history = matchModel.getGameScoreHistory();
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
    public MatchGameScoresView.ScoresToShow updateSetScoresToShow(boolean bChange) {
        MatchGameScoresView matchGameScores = (MatchGameScoresView) findViewById(R.id.gamescores);
        if ( matchGameScores != null ) {
            matchGameScores.setVisibility(View.VISIBLE);
            MatchGameScoresView.ScoresToShow scoresToShow = matchGameScores.updateSetScoresToShow(bChange);
            castSendFunction(ICastHelper.GameScores_display + "(" + scoresToShow.equals(MatchGameScoresView.ScoresToShow.GamesWonPerSet) + ")");
            matchGameScores.update(matchModel, m_firstPlayerOnScreen);

            return scoresToShow;
        } else {
            int[] iaSetScores = new int[] { R.id.btn_gameset3_detail1, R.id.btn_gameset3_detail2, R.id.space_gameset3_detail
                                          , R.id.btn_gameset2_detail1, R.id.btn_gameset2_detail2, R.id.space_gameset2_detail
                                          , R.id.btn_gameset1_detail1, R.id.btn_gameset1_detail2, R.id.space_gameset1_detail
                                          };
            List<TextView> lSetScoresViews  = new ArrayList<>();
            List<Space>    lSetScoresSpaces = new ArrayList<>();
            for(int iViewId: iaSetScores) {
                View vCheck = findViewById(iViewId);
                if ( vCheck instanceof TextView ) {
                    lSetScoresViews.add((TextView) vCheck);
                } else if ( vCheck instanceof Space ) {
                    lSetScoresSpaces.add((Space) vCheck);
                }
            }
            final int iNrOfSetDetailsPossibleInLayout = lSetScoresViews.size() / 2;

            //final int iVisibilityIfNotUsed = View.INVISIBLE;
            final int iVisibilityIfNotUsed = View.GONE; // TODO

            if ( Brand.isGameSetMatch() ) {
                GSMModel gsmModel = (GSMModel) matchModel;

                final TextView vSetsWon1 = (TextView) findViewById(R.id.btn_setswon1);
                final TextView vSetsWon2 = (TextView) findViewById(R.id.btn_setswon2);

                Map<Player, Integer> setsWon = gsmModel.getSetsWon();
                Integer iSetsFor1 = setsWon.get(m_firstPlayerOnScreen);
                Integer iSetsFor2 = setsWon.get(m_firstPlayerOnScreen.getOther());
                if ( vSetsWon1 != null ) {
                    vSetsWon1.setText(String.valueOf(iSetsFor1));
                    vSetsWon2.setText(String.valueOf(iSetsFor2));
                }

                if ( iSetsFor1 + iSetsFor2 <= iNrOfSetDetailsPossibleInLayout) {
                    // few enough sets played to show details score how the set ended, e.g. 6-4 or 7-6

                    if ( vSetsWon1 != null ) {
                        setVisibility(vSetsWon1, iVisibilityIfNotUsed);
                        setVisibility(vSetsWon2, iVisibilityIfNotUsed);
                    }

                    List<Map<Player, Integer>> gamesWonPerSet = GSMUtil.gsmGamesWonPerSet(gsmModel, true);

                    for(int iSetZb = 0; iSetZb < iNrOfSetDetailsPossibleInLayout; iSetZb++ ) {
                        TextView vSetScore1 = lSetScoresViews.get(iSetZb * 2 + 0);
                        TextView vSetScore2 = lSetScoresViews.get(iSetZb * 2 + 1);
                        Space space = lSetScoresSpaces.get(iSetZb);

                        if ( iSetZb >= iSetsFor1 + iSetsFor2 ) {
                            // hide views we will not be using
                            setVisibility(vSetScore1, iVisibilityIfNotUsed);
                            setVisibility(vSetScore2, iVisibilityIfNotUsed);
                            if ( space != null ) {
                                space.setVisibility(iVisibilityIfNotUsed);
                            }
                        } else {
                            setVisibility(vSetScore1, View.VISIBLE);
                            setVisibility(vSetScore2, View.VISIBLE);
                            if ( space != null ) {
                                space.setVisibility(View.VISIBLE);
                            }
                            vSetScore1.setTag(ScorePlusSmallScore.EMPTY);
                            vSetScore2.setTag(ScorePlusSmallScore.EMPTY);

                            Map<Player, Integer> gamesWonSet = gamesWonPerSet.get(iSetZb);
                            int iGamesFor1 = gamesWonSet.get(m_firstPlayerOnScreen);
                            int iGamesFor2 = gamesWonSet.get(m_firstPlayerOnScreen.getOther());

                            // in case of tiebreak
                            GSMTieBreakType gsmTieBreakType = GSMUtil.getTiebreakType(iGamesFor1, iGamesFor2);
                            if ( gsmTieBreakType != null ) {
                                int[] iaScore1 = GSMUtil.gamesWonAndTiebreakPoints(iGamesFor1, gsmTieBreakType);
                                int[] iaScore2 = GSMUtil.gamesWonAndTiebreakPoints(iGamesFor2, gsmTieBreakType);

                                View vSetScoreWinner   = null;
                                View vSetScoreLoser    = null;
                                if ( iaScore1[1] > iaScore2[1] ) {
                                    vSetScoreWinner = vSetScore1;
                                    vSetScoreLoser  = vSetScore2;
                                } else {
                                    vSetScoreWinner = vSetScore2;
                                    vSetScoreLoser  = vSetScore1;
                                }
                                vSetScoreLoser.setTag(Math.min(iaScore1[1], iaScore2[1]));
                                if ( gsmTieBreakType.equals(GSMTieBreakType.FinalSetNoGames) ) {
                                    vSetScoreWinner.setTag(Math.max(iaScore1[1], iaScore2[1]));
                                } else {
                                    vSetScoreWinner.setTag(ScorePlusSmallScore.EMPTY);
                                }

                                iGamesFor1 = iaScore1[0];
                                iGamesFor2 = iaScore2[0];
                            } else {
                                // no tiebreak
                            }
                            vSetScore1.setText(String.valueOf(iGamesFor1));
                            vSetScore2.setText(String.valueOf(iGamesFor2));
                        }
                    }

                    int[] iViewIds = new int[] { R.id.btn_gameswon1, R.id.btn_gameswon2, R.id.space_scoregame_scorepnt
                                               , R.id.btn_side1    , R.id.btn_side2
                                               };
                    int iGoneOrVisible = gsmModel.matchHasEnded() ? View.GONE : View.VISIBLE;
                    for(int i:iViewIds) {
                        View v = m_vRoot.findViewById(i);
                        if ( v != null ) {
                            v.setVisibility(iGoneOrVisible);
                        }
                    }
                } else {
                    // not all sets details can be displayed, just use one of all possible and show number of sets won
                    for(int iSetZb = 0; iSetZb < iNrOfSetDetailsPossibleInLayout; iSetZb++ ) {
                        TextView vSetScore1 = lSetScoresViews.get(iSetZb * 2 + 0);
                        TextView vSetScore2 = lSetScoresViews.get(iSetZb * 2 + 1);
                        vSetScore1.setVisibility(iVisibilityIfNotUsed);
                        vSetScore2.setVisibility(iVisibilityIfNotUsed);
                    }
                    if ( vSetsWon1 != null ) {
                        setVisibility(vSetsWon1, View.VISIBLE);
                        setVisibility(vSetsWon2, View.VISIBLE);
                    }
                }
            } else {
                Map<Player, Integer> gamesWon = matchModel.getGamesWon();

                TextView vGamesWon1 = (TextView) findViewById(R.id.btn_gameswon1);
                TextView vGamesWon2 = (TextView) findViewById(R.id.btn_gameswon2);

                Integer iGamesFor1 = gamesWon.get(m_firstPlayerOnScreen);
                Integer iGamesFor2 = gamesWon.get(m_firstPlayerOnScreen.getOther());

                if ( iGamesFor1 + iGamesFor2 <= iNrOfSetDetailsPossibleInLayout) {
                    // few enough games played to show details score how they ended, e.g. 11-5, 10-12
                    setVisibility(vGamesWon1, iVisibilityIfNotUsed);
                    setVisibility(vGamesWon2, iVisibilityIfNotUsed);

                    {
                        final TextView vPointsWon1 = (TextView) findViewById(R.id.btn_score1);
                        if ( vPointsWon1 != null ) {
                            final TextView vPointsWon2 = (TextView) findViewById(R.id.btn_score2);
                            if ( iGamesFor1 + iGamesFor2 > 0 ) {
                                vPointsWon1.setTag(iGamesFor1);
                                vPointsWon2.setTag(iGamesFor2);
                            } else {
                                vPointsWon1.setTag(ScorePlusSmallScore.EMPTY);
                                vPointsWon2.setTag(ScorePlusSmallScore.EMPTY);
                            }
                        }
                    }

                    List<Map<Player, Integer>> pointScoredPerGame = matchModel.getEndScoreOfGames();

                    for (int iSetZb = 0; iSetZb < iNrOfSetDetailsPossibleInLayout; iSetZb++) {
                        TextView vGameScore1 = lSetScoresViews.get(iSetZb * 2 + 0);
                        TextView vGameScore2 = lSetScoresViews.get(iSetZb * 2 + 1);
                        Space space = lSetScoresSpaces.get(iSetZb);

                        if (iSetZb >= iGamesFor1 + iGamesFor2) {
                            // hide views we will not be using
                            setVisibility(vGameScore1, iVisibilityIfNotUsed);
                            setVisibility(vGameScore2, iVisibilityIfNotUsed);
                            if (space != null) {
                                space.setVisibility(iVisibilityIfNotUsed);
                            }
                        } else {
                            setVisibility(vGameScore1, View.VISIBLE);
                            setVisibility(vGameScore2, View.VISIBLE);
                            if (space != null) {
                                space.setVisibility(View.VISIBLE);
                            }
                            // to be on the safe side, clear small digits that are only for GSMModel
                            vGameScore1.setTag(ScorePlusSmallScore.EMPTY);
                            vGameScore2.setTag(ScorePlusSmallScore.EMPTY);

                            Map<Player, Integer> pointWonInGame = pointScoredPerGame.get(iSetZb);
                            int iPointsFor1 = pointWonInGame.get(m_firstPlayerOnScreen);
                            int iPointsFor2 = pointWonInGame.get(m_firstPlayerOnScreen.getOther());

                            vGameScore1.setText(String.valueOf(iPointsFor1));
                            vGameScore2.setText(String.valueOf(iPointsFor2));
                            if ( iPointsFor1 > iPointsFor2 ) {
                                ColorUtil.setBackground(vGameScore1, Color.BLACK);
                                ColorUtil.setBackground(vGameScore2, Color.WHITE);
                                vGameScore1.setTextColor      (Color.WHITE);
                                vGameScore2.setTextColor      (Color.BLACK);
                            }
                            if ( iPointsFor1 < iPointsFor2 ) {
                                ColorUtil.setBackground(vGameScore1, Color.WHITE);
                                ColorUtil.setBackground(vGameScore2, Color.BLACK);
                                vGameScore1.setTextColor      (Color.BLACK);
                                vGameScore2.setTextColor      (Color.WHITE);
                            }
                        }
                    }

                    ViewUtil.hideViews(m_vRoot, R.id.btn_setswon1, R.id.btn_setswon2, R.id.space_scoreset_scoregame);
                } else {
                    // not all game details can be displayed: hide element supposed to show details
                    for(int iGameZb = 0; iGameZb < iNrOfSetDetailsPossibleInLayout; iGameZb++ ) {
                        TextView vSetScore1 = lSetScoresViews.get(iGameZb * 2 + 0);
                        TextView vSetScore2 = lSetScoresViews.get(iGameZb * 2 + 1);
                        setVisibility(vSetScore1, iVisibilityIfNotUsed);
                        setVisibility(vSetScore2, iVisibilityIfNotUsed);
                    }
                    // show only number of games won
                    setVisibility(vGamesWon1, View.VISIBLE);
                    setVisibility(vGamesWon2, View.VISIBLE);

                    // remove small digit that also indicates number of games won, since we show it in big digits
                    if ( true ) {
                        TextView vPointsWon1 = (TextView) findViewById(R.id.btn_score1);
                        if ( vPointsWon1 != null ) {
                            TextView vPointsWon2 = (TextView) findViewById(R.id.btn_score2);
                            vPointsWon1.setTag(ScorePlusSmallScore.EMPTY);
                            vPointsWon2.setTag(ScorePlusSmallScore.EMPTY);
                        }
                    }
                }
            }
        }
        return null;
    }
    public void initGameScoreView() {
        GameScoresAppearance appearance = PreferenceValues.getGameScoresAppearance(context);
        setGameScoreView(appearance);
    }
    private void setGameScoreView(GameScoresAppearance appearance) {
        boolean showGamesWon = appearance.showGamesWon(isPresentation());
        MatchGameScoresView matchGameScores = (MatchGameScoresView) findViewById(R.id.gamescores);
        for ( Player player : Player.values() ) {
            int iNameId = m_player2gamesWonId.get(player);
            View vGamesWon = findViewById(iNameId);
            if ( vGamesWon == null ) {
                Log.w(TAG, "No GamesWon buttons to work with in orientation/view " + ViewUtil.getCurrentOrientation(context));
                break;
            }
            vGamesWon.setVisibility(showGamesWon || (matchGameScores == null) ? View.VISIBLE : View.INVISIBLE);
        }

        // update casting screen
        boolean bShowGameScores = appearance.showGamesWon(true) == false;
        castSendFunction(ICastHelper.GameScores_display + "(" + bShowGameScores + ")");

        if ( matchGameScores != null ) {
            if ( showGamesWon ) {
                matchGameScores.setVisibility(View.GONE); // gone ensures, if many games have been played, more room becomes available for timer/oldfahsined paper scoring
            } else {
                matchGameScores.setVisibility(View.VISIBLE);
                matchGameScores.refreshDrawableState();
            }
        }
    }

    public void updateGameScores() {
        MatchGameScoresView matchGameScores = (MatchGameScoresView) findViewById(R.id.gamescores);
        if ( matchGameScores != null) {
            matchGameScores.setOnTextResizeListener(matchGamesScoreSizeListener);
            matchGameScores.update(matchModel, m_firstPlayerOnScreen);
        }

        // update casting screen
        List<Map<Player, Integer>> endScoreOfPreviousGames = matchModel.getEndScoreOfPreviousGames();

        Map<Player, Integer> setsWon = null;
        if ( Brand.isGameSetMatch() ) {
            GSMModel gsmModel = (GSMModel) matchModel;
            endScoreOfPreviousGames = gsmModel.getGamesWonPerSet();
            setsWon = gsmModel.getSetsWon();
        }
        JSONArray lomPlayer2Score = new JSONArray();
        for(Map<Player, Integer> mEndScoreOfPrev: endScoreOfPreviousGames) {
            lomPlayer2Score.put(new JSONObject(MapUtil.keysToString(mEndScoreOfPrev)));
        }
        List<GameTiming> times = matchModel.getTimes();
        JSONArray loiGameDuration = new JSONArray();
        for(GameTiming time: times) {
            loiGameDuration.put(time.getDurationMM());
        }
        boolean bSwapAAndB = Player.B.equals(m_firstPlayerOnScreen);
        castSendFunction(ICastHelper.GameScores_update + "(" + lomPlayer2Score
                                                          + "," + bSwapAAndB
                                                          + "," + loiGameDuration
                                                          + "," + matchModel.matchHasEnded()
                                        + ((setsWon!=null)?("," + (new JSONObject(MapUtil.keysToString(setsWon))).toString() ) :"" )
                                                          + ")");

        if ( (m_player2gamesWonId != null) && (matchModel != null) ) {
            Map<Player, Integer> gamesWon = matchModel.getGamesWon(false);
            if ( Brand.isGameSetMatch() ) {
                GSMModel gsmModel = (GSMModel) matchModel;
                if ( gsmModel.isTieBreakGame() ) {
                    List<Map<Player, Integer>> maps = GSMUtil.gsmGamesWonPerSet(gsmModel, false);
                    gamesWon = ListUtil.removeLast(maps);
                    if ( MapUtil.getMaxValue(gamesWon) == 0
                            && ListUtil.isNotEmpty(maps)
                            && (gsmModel.isFinalSet() == false ) // in final set show we can show 0-0 if finalsetfinish is without playing any games
                       )
                    {
                        gamesWon = ListUtil.removeLast(maps);
                    }
                }
            }
            if ( MapUtil.isNotEmpty(gamesWon) ) {
                for(Player player: Player.values() ) {
                    int iNameId = m_player2gamesWonId.get(player);
                    View view = findViewById(iNameId);
                    Integer iGamesWon = gamesWon.get(player);
                    if ( view instanceof GamesWonButton ) {
                        GamesWonButton v = (GamesWonButton) view;
                        v.setGamesWon(iGamesWon);
                    }
                    castChangeViewTextMessage(iNameId, iGamesWon);
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
            if ( Brand.isGameSetMatch() ) {
                iIds = new int[] {R.id.sb_match_duration, R.id.sb_game_duration, R.id.sb_set_duration};
            }
            for(int iResId: iIds) {
                Chronometer cm = (Chronometer) findViewById(iResId);
                if ( cm != null ) {
                    cm.setTextSize(TypedValue.COMPLEX_UNIT_PX, iChronoTextSize);
                }
            }
        }
    };


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
            setTextColor(btnScore, scoreButtonTxt);
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
        Integer mainDark    = mColors.get(ColorPrefs.ColorTarget.darkest);
        Integer mainLight   = mColors.get(ColorPrefs.ColorTarget.lightest);

        for( Player player: Model.getPlayers() ) {
            if ( m_player2serverSideId != null ) {
                int id = m_player2serverSideId.get(player);
                ServeButton ssView = (ServeButton) findViewById(id);
                Integer iBgColor  = mColors.get(ColorPrefs.ColorTarget.serveButtonBackgroundColor);
                Integer iTxtColor = mColors.get(ColorPrefs.ColorTarget.serveButtonTextColor);
                if ( ssView != null ) {
                    setBackgroundColor(ssView, iBgColor);
                    setTextColor(ssView, iTxtColor);

                    castSendChangeViewMessage(ssView.getId(), iTxtColor, ICastHelper.Property_Color);
                } else {
                    castSendChangeViewMessage(id, iBgColor , ICastHelper.Property_BGColor);
                    castSendChangeViewMessage(id, iTxtColor, ICastHelper.Property_Color);
                }
            }

            Integer scoreBgColor  = mColors.get(ColorPrefs.ColorTarget.scoreButtonBackgroundColor);
            Integer scoreTxtColor = mColors.get(ColorPrefs.ColorTarget.scoreButtonTextColor);
            if ( m_player2scoreId != null ) {
                int id = m_player2scoreId.get(player);
                TextView txtView = (TextView) findViewById(id);
                if ( txtView != null ) {
                    setTextColor(txtView, scoreTxtColor);
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
                    setTextColor(txtView, scoreTxtColor);
                    setBackgroundColor(txtView, scoreBgColor);
                }
            }

            if ( m_player2SetsWonId != null ) {
                int iNameId = m_player2SetsWonId.get(player);
                View view = findViewById(iNameId);
                if ( view instanceof AutoResizeTextView ) {
                    AutoResizeTextView txtView = (AutoResizeTextView) view;
                    setTextColor(txtView, mainLight);
                    setBackgroundColor(txtView, mainDark); // transparent
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
                    v.setBackgroundColorServer(mColors.get(ColorPrefs.ColorTarget.serveButtonBackgroundColor), player);
                    v.setTextColorServer      (mColors.get(ColorPrefs.ColorTarget.serveButtonTextColor), player);

                    castSendChangeViewMessage(id, pbBgColor , ICastHelper.Property_BGColor);
                    castSendChangeViewMessage(id, pbTxtColor, ICastHelper.Property_Color);
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
        if ( Brand.isGameSetMatch() ) {
            iIds = new int[] {R.id.sb_match_duration, R.id.sb_game_duration, R.id.sb_set_duration};
        }
        for(int iResId: iIds) {
            Chronometer cm = (Chronometer) findViewById(iResId);
            if ( cm == null ) { continue; }
            cm.setTextColor      (mColors.get(ColorPrefs.ColorTarget.scoreButtonTextColor));
            cm.setBackgroundColor(context.getResources().getColor(android.R.color.transparent));
        }

        View vRoot = findViewById(R.id.squoreboard_root_view);
        if ( vRoot != null ) {
            Integer clBackGround = mColors.get(ColorPrefs.ColorTarget.backgroundColor);
            setBackgroundColor(vRoot, clBackGround);
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

    public boolean setPowerPlayIconVisibility(int visibility, Player p) {
        int iResId = m_player2PowerPlayIconId.get(p);
        View vIcon = m_vRoot.findViewById(iResId);
        if ( vIcon != null ) {
            vIcon.setVisibility(visibility);
            return true;
        } else {
            //Log.w(TAG, "No powerplay view found ...");
            return false;
        }
    }

    public void setBluetoothIconVisibility(int visibility) {
        View vIcon = m_vRoot.findViewById(R.id.sb_bluetooth_icon);
        if ( vIcon != null ) {
            vIcon.setVisibility(visibility);
        } else {
            Log.w(TAG, "No sb_bluetooth_icon view found ..."  + context.getResources().getResourceName(m_vRoot.getId()));
        }
    }


    public void guiElementColorSwitch(ShowScoreChangeOn guiElementToUseForFocus, Player player, FocusEffect focusEffect, int iInvocationCnt, int iTmpTxtOnElementDuringFeedback) {
        Map<ColorPrefs.ColorTarget, Integer> mColors = ColorPrefs.getTarget2colorMapping(context);

        Map<Player, Integer> player2GuiElement = m_player2nameId;
        Integer iBgColor = mColors.get(ColorPrefs.ColorTarget.playerButtonBackgroundColor);
        Integer iTxColor = mColors.get(ColorPrefs.ColorTarget.playerButtonTextColor);
        switch (guiElementToUseForFocus) {
            case PlayerButton:
                iBgColor = mColors.get(ColorPrefs.ColorTarget.playerButtonBackgroundColor);
                iTxColor = mColors.get(ColorPrefs.ColorTarget.playerButtonTextColor);
                player2GuiElement = m_player2nameId;
                break;
            case ScoreButton:
                iBgColor = mColors.get(ColorPrefs.ColorTarget.scoreButtonBackgroundColor);
                iTxColor = mColors.get(ColorPrefs.ColorTarget.scoreButtonTextColor);
                player2GuiElement = m_player2scoreId;
                break;
            case GamesButton:
                iBgColor = mColors.get(ColorPrefs.ColorTarget.scoreButtonBackgroundColor);
                iTxColor = mColors.get(ColorPrefs.ColorTarget.scoreButtonTextColor);
                player2GuiElement = m_player2gamesWonId;
                break;
            case SetsButton:
                iBgColor = mColors.get(ColorPrefs.ColorTarget.darkest);
                iTxColor = mColors.get(ColorPrefs.ColorTarget.lightest);
                player2GuiElement = m_player2SetsWonId;
                break;
        }
        View view = findViewById(player2GuiElement.get(player));
        if ( view == null ) { return; }

        switch (focusEffect) {
            case BlinkByInverting:
                boolean bInvert = iInvocationCnt % 2 == 1;
                if ( bInvert ) {
                    Integer iTmp = iBgColor;
                    iBgColor = iTxColor;
                    iTxColor = iTmp;
                }
                if ( view instanceof PlayersButton ) {
                    PlayersButton v = (PlayersButton) view;
                    v.setBackgroundColor(iBgColor);
                    v.setTextColor      (iTxColor);
                } else {
                    // typically the score buttons
                    setBackgroundColor(view, iBgColor);
                    setTextColor(view, iTxColor);
                    if ( view instanceof TextView ) {
                        TextView tv = (TextView) view;
                        final int iTagNr = R.string.oa_game;
                        Object tag = tv.getTag(iTagNr);
                        if ( iTmpTxtOnElementDuringFeedback == 0 ) {
                            if ( tag != null ) {
                                tv.setText(tag.toString()); // restore back to original text
                                tv.setTag(iTagNr, null);
                            }
                        } else {
                            String sTmpTxtOnElementDuringFeedback = StringUtil.capitalize(getOAString(iTmpTxtOnElementDuringFeedback));
                            if ( tag == null ) {
                                tv.setTag(iTagNr, tv.getText()); // store original text in tag
                            }
                            tv.setText(sTmpTxtOnElementDuringFeedback);
                        }
                    }
                }
                break;
            case SetTransparency:
                Drawable drawable = view.getBackground();
                if ( drawable instanceof GradientDrawable ) {
                    GradientDrawable gd = (GradientDrawable) drawable;
                    int iTransparency = Math.abs ((20 * iInvocationCnt) % 200 - 100);
                    if ( iInvocationCnt == 0 ) { iTransparency = 0; }
                    //Log.d(TAG, "transparency : " + iTransparency);
                    gd.setAlpha(0xFF - iTransparency);
                }
                break;
        }
    }
    public void showBLEInfoMessage(String sMsg, int iMessageDurationSecs) {
        boolean bShow = PreferenceValues.showFeedBackOnBLEButtonsPressedInfoMessages(context);
        if ( bShow ) {
            showInfoMessage(sMsg, iMessageDurationSecs);
        } else {
            final TextView tvBLEInfo = (TextView) findViewById(R.id.sb_bottom_of_screen_infomessage);
            if ( tvBLEInfo != null && tvBLEInfo.getVisibility() != View.INVISIBLE ) {
                tvBLEInfo.setVisibility(View.INVISIBLE);
            }
        }
    }

    //===================================================================
    // INFO message methods (replacing 'toasts')
    //===================================================================
    private CountDownTimer m_timerInfoMessage;
    public void showInfoMessage(final String sMsg, int iMessageDurationSecs) {
        // hide any 'permanent' BLE message still displaying
        final TextView tvBLEInfo = (TextView) findViewById(R.id.sb_bottom_of_screen_infomessage);
        if ( tvBLEInfo != null ) {
            if ( StringUtil.isNotEmpty(sMsg) ) {
                tvBLEInfo.setText(String.format(sMsg, iMessageDurationSecs));
                tvBLEInfo.setVisibility(View.VISIBLE);

                if ( m_timerInfoMessage != null ) {
                    m_timerInfoMessage.cancel();
                }
                if ( iMessageDurationSecs > 0 ) {
                    // auto hide in x secs
                    m_timerInfoMessage = new CountDownTimer(iMessageDurationSecs * 1000, 1000) {
                        @Override public void onTick(long millisUntilFinished) {
                            if ( sMsg.contains("%d") || sMsg.contains("%1$d") ) {
                                int iSecondsLeft = (int) millisUntilFinished / 1000;
                                tvBLEInfo.setText(String.format(sMsg, iSecondsLeft));
                            }
                        }
                        @Override public void onFinish() {
                            setVisibility(tvBLEInfo, View.INVISIBLE);
                            m_timerInfoMessage = null;
                        }
                    };
                    m_timerInfoMessage.start();
                }
            } else {
                setVisibility(tvBLEInfo, View.INVISIBLE);
            }
        }
    }
    public void appendToInfoMessage(String sMsg) {
        appendToInfoMessage(sMsg, false);
    }
    public void appendToInfoMessage(String sMsg, boolean bBetweenBrackets) {
        final TextView tvBLEInfo = (TextView) findViewById(R.id.sb_bottom_of_screen_infomessage);
        if ( tvBLEInfo == null ) { return; }
        final String sCurrent = tvBLEInfo.getText().toString();
        String sNew = sCurrent;
        if ( bBetweenBrackets ) {
            sNew = sNew.replaceFirst("\\s*\\(.+\\)$", "");
            sNew += " (" + sMsg + ")";
        } else {
            sNew += sMsg;
        }
        tvBLEInfo.setText(sNew);
    }

    //===================================================================
    // BLE methods
    //===================================================================
    public void updateBLEConnectionStatusIcon(int visibility, int nrOfDevicesConnected) {
        TextView vTxt = m_vRoot.findViewById(R.id.sb_bluetoothble_nrofconnected);
        if ( vTxt != null ) {
            vTxt.setText("\u16E1\u16d2:" + nrOfDevicesConnected);
            vTxt.setVisibility(visibility);
        }
    }

    private static final String[] m_aStatusTexts = { "mq", "Mq", "MQ", "mQ" };
    private int m_statusIndex = 0;
    public void updateMQTTConnectionStatusIcon(int visibility, int nrOfWhat) {
        TextView vTxt = m_vRoot.findViewById(R.id.sb_mqtt_connection_info);
        if ( vTxt != null ) {
            if ( nrOfWhat > 0 ) {
                m_statusIndex++;
                if ( m_statusIndex >= m_aStatusTexts.length ) {
                    m_statusIndex = 0;
                }
            } else {
                m_statusIndex = 0;
            }
            vTxt.setText(m_aStatusTexts[m_statusIndex] + ":" + nrOfWhat);
            vTxt.setVisibility(visibility);
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
        if ( /*false && ViewUtil.isWearable(context) &&*/ Brand.isRacketlon() ) { // disabled to have Racketlon app be 'approved' for wearable
            // for racketlon show discipline
            RacketlonModel rm = (RacketlonModel) this.matchModel;
            Sport sportForSet = rm.getSportForSetInProgress();
            sField = String.valueOf(sportForSet); // TODO: internationalize?
            castSendFunction(ICastHelper.Racketlon_updateDiscipline + "(\"" + sField + "\")");
        }
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
                ViewGroup.LayoutParams loParams = (ViewGroup.LayoutParams) view.getLayoutParams();
                // This will currently return null, if it was not constructed from XML.
                if ( loParams instanceof PercentRelativeLayout.LayoutParams ) { // TODO: can be removed in future now we are switching to ConstraintLayout
                    PercentRelativeLayout.LayoutParams plParams = (PercentRelativeLayout.LayoutParams) loParams;
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

            // now that gamescores has more room, timer has to little room if we keep the xml setting: change it so timer is right-aligned to parent
            view = findViewById(R.id.btn_timer);
            if ( view != null ) {
                ViewGroup.LayoutParams loParams = view.getLayoutParams();
                if ( loParams instanceof RelativeLayout.LayoutParams ) { // TODO: can be removed in future now we are switching to ConstraintLayout
                    RelativeLayout.LayoutParams rlParams = (RelativeLayout.LayoutParams) loParams;
                    if ( ViewUtil.isPortraitOrientation(context) ) {
                        rlParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                        rlParams.removeRule(RelativeLayout.ALIGN_RIGHT);
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

        boolean bShowAsTextAbbr = countryPref.contains(ShowCountryAs.AbbreviationAfterName);
        boolean bShowOnCast     = countryPref.contains(ShowCountryAs.FlagNextToNameChromeCast) && (isPresentation() == true);
        boolean bShowOnDevice   = countryPref.contains(ShowCountryAs.FlagNextToNameOnDevice  ) && (isPresentation() == false);
        boolean bShowAsFlagPref = bShowOnCast || bShowOnDevice;
        boolean bHideBecauseSameCountry = false;
        if ( PreferenceValues.hideFlagForSameCountry(context) && StringUtil.isNotEmpty(sCountry) ) {
            String sOtherCountry = matchModel.getCountry(p.getOther());
            bHideBecauseSameCountry = sCountry.equalsIgnoreCase(sOtherCountry);
        }

        boolean bShowFlag = bShowAsFlagPref && (bHideBecauseSameCountry == false);
        if ( view instanceof PlayersButton ) {
            PlayersButton button = (PlayersButton) view;

            button.setCountry(sCountry, bShowAsTextAbbr, bShowFlag);
        }

        // send message to update cast screen
        int iNrOnCast = (p.ordinal() + m_firstPlayerOnScreen.ordinal()) % 2 + 1;
        if ( countryPref.contains(ShowCountryAs.FlagNextToNameChromeCast) && (bHideBecauseSameCountry == false) ) {
            String sFlagURL = PreferenceValues.getFlagURL(sCountry, context);
            castSendChangeViewMessage("img_flag" + iNrOnCast, String.format("url(%s)", sFlagURL), ICastHelper.Property_BGImage);
        } else {
            castSendChangeViewMessage("img_flag" + iNrOnCast, String.format("url(%s)", ""      ), ICastHelper.Property_BGImage);
        }

        boolean bSwapAAndB = Player.B.equals(m_firstPlayerOnScreen);
        castSendFunction(ICastHelper.Country_update + "('" + p + "','" + (sCountry!=null?sCountry:"") + "'," + bSwapAAndB + ")");
    }
    public void updatePlayerAvatar(Player p, String sAvatar) {
        EnumSet<ShowAvatarOn> avatarPref = PreferenceValues.showAvatarOn(context);
        boolean bShowOnCast   = avatarPref.contains(ShowAvatarOn.OnChromeCast) && (isPresentation() == true);
        boolean bShowOnDevice = avatarPref.contains(ShowAvatarOn.OnDevice    ) && (isPresentation() == false);
        boolean bShowAvatar   = bShowOnCast || bShowOnDevice;

        if ( bShowAvatar ) {
            View view = findViewById(m_player2nameId.get(p));
            if (view instanceof PlayersButton) {
                PlayersButton button = (PlayersButton) view;
                button.setAvatar(sAvatar);
            }
        }

        // send message to update cast screen
        int iNrOnCast = (p.ordinal() + m_firstPlayerOnScreen.ordinal()) % 2 + 1;
        if ( avatarPref.contains(ShowAvatarOn.OnChromeCast) ) {
            castSendChangeViewMessage("img_avatar" + iNrOnCast, String.format("url(%s)", sAvatar), ICastHelper.Property_BGImage);
        } else {
            castSendChangeViewMessage("img_avatar" + iNrOnCast, String.format("url(%s)", ""     ), ICastHelper.Property_BGImage);
        }

        boolean bSwapAAndB = Player.B.equals(m_firstPlayerOnScreen);
        castSendFunction(ICastHelper.Avatar_update + "('" + p + "','" + (sAvatar!=null?sAvatar:"") + "'," + bSwapAAndB + ")");
    }
    public void initPerPlayerColors(Player p, String sColor, String sColorPrev) {
        EnumSet<ShowPlayerColorOn> colorOns = PreferenceValues.showPlayerColorOn(context);

        Integer iPlayerColor = null;
        Integer iTxtColor    = null;
        if ( StringUtil.isNotEmpty(sColor) ) {
            try {
                iPlayerColor = Color.parseColor(sColor);
            } catch (IllegalArgumentException e) {
                return;
            }
            // switch color of text to black or white depending on chosen color
            long lPreferWhiteOverBlackStrength = PreferenceValues.getPreferWhiteOverBlackThreshold(context);
            iTxtColor = ColorUtil.getBlackOrWhiteFor(sColor, lPreferWhiteOverBlackStrength);

            if ( p.equals(m_firstPlayerOnScreen) && ViewUtil.isLandscapeOrientation(context) ) {
                Chronometer tvMatchTime = (Chronometer) findViewById(R.id.sb_match_duration);
                if ( tvMatchTime != null ) {
                    tvMatchTime.setTextColor(iTxtColor);
                }
                if ( Brand.isGameSetMatch() ) {
                    Chronometer tvSetTime = (Chronometer) findViewById(R.id.sb_set_duration);
                    if ( tvSetTime != null ) {
                        tvSetTime.setTextColor(iTxtColor);
                    }
                }
            }
        }

        if ( colorOns.contains(ShowPlayerColorOn.ServeSideButton) ) {
            initPerPlayerViewWithColors(p, m_player2serverSideId, iPlayerColor, iTxtColor, ColorPrefs.ColorTarget.serveButtonBackgroundColor, ColorPrefs.ColorTarget.serveButtonTextColor);
        }

        if ( colorOns.contains(ShowPlayerColorOn.ScoreButtonBorder) || colorOns.contains(ShowPlayerColorOn.ScoreButton) ) {
            View view = findViewById(m_player2scoreId.get(p));
            if ( view != null ) {
                Integer iScoreButtonBgColor = mColors.get(ColorPrefs.ColorTarget.scoreButtonBackgroundColor);
                if ( StringUtil.isNotEmpty(sColorPrev) ) {
                    int iPrevColor = Color.parseColor(sColorPrev);
                    mViewId2BorderColor.remove(view.getId());
                }
                if ( iPlayerColor == null ) {
                    //setBackgroundColor(view, iScoreButtonBgColor);
                    setBackgroundAndBorder(view, iScoreButtonBgColor, iScoreButtonBgColor);
                    castSendChangeViewMessage(view.getId(), iScoreButtonBgColor, ICastHelper.Property_BorderColor);
                } else {
                    if ( colorOns.contains(ShowPlayerColorOn.ScoreButton) ) {
                        setBackgroundAndBorder(view, iPlayerColor       , iPlayerColor);
                        setTextColor(view, iTxtColor);
                    } else {
                        setBackgroundAndBorder(view, iScoreButtonBgColor, iPlayerColor);
                    }
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

    private void setTextColor(View view, Integer iTxtColor) {
        if ( view instanceof TextView) {
            ((TextView)view).setTextColor(iTxtColor);
        }
        if ( view instanceof PlayersButton) {
            ((PlayersButton)view).setTextColor(iTxtColor);
        }

        castSendChangeViewMessage(view.getId(), iTxtColor, ICastHelper.Property_Color);
    }
    private void setBackgroundColor(View view, Integer iBgColor) {
        //Log.d(TAG, "change bgcolor of " + view + " = " + iBgColor);
        //ColorUtil.setBackground(view, iBgColor);
        setBackgroundAndBorder(view, iBgColor, mViewId2BorderColor.get(view.getId()));

        castSendChangeViewMessage(view.getId(), iBgColor, ICastHelper.Property_BGColor);
    }

    private Map<Integer, Integer> mViewId2BorderColor = new HashMap<>();
    private void setBackgroundAndBorder(View view, Integer iBgColor, Integer iBorderColor) {
        if ( iBgColor == null ) {
            return;
        }
        //Log.d(TAG, "change color of " + view + " bg=" + iBgColor + ", brdr=" + iBorderColor);
        int[] colors = new int[] {iBgColor, iBgColor};
        GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, colors);
        // gd.setShape(GradientDrawable.RECTANGLE); // RECTANGLE is the default
        //gd.setCornerRadius(getResources().getDimension(R.dimen.sb_button_radius));
        int viewId = view.getId();
        if ( viewId == R.id.squoreboard_root_view ) {
            // do not apply corner radius to root view because 'splashscreenbackground' becomes visible in corners
        } else {
            gd.setCornerRadius(getScreenHeightWidthMinimumFraction(R.fraction.player_button_corner_radius)); // if we do not set this, screen elements no longer have rounded corners
        }
        if ( iBorderColor != null ) {
            gd.setStroke(getScreenHeightWidthMinimumFraction(R.fraction.player_button_colorborder_thickness), iBorderColor);
            mViewId2BorderColor.put(viewId, iBorderColor);
        } else {
            mViewId2BorderColor.remove(viewId);
        }
        view.setBackground(gd);
        view.invalidate();

        castSendChangeViewMessage(view.getId(), iBgColor    , ICastHelper.Property_BGColor);
        castSendChangeViewMessage(view.getId(), iBorderColor, ICastHelper.Property_BorderColor); // TODO: thickness
    }
    /** invoked several times for different elements */
    private void initPerPlayerViewWithColors(Player p, Map<Player, Integer> mPlayer2ViewId, Integer iBgColor, Integer iTxtColor, ColorPrefs.ColorTarget bgColorDefKey, ColorPrefs.ColorTarget txtColorDefKey) {
        if (mPlayer2ViewId == null) { return; }

        if ( iBgColor == null ) {
            iBgColor  = mColors.get(bgColorDefKey);
            iTxtColor = mColors.get(txtColorDefKey);
        }
        Integer id = mPlayer2ViewId.get(p);
        if ( (id == null) || (iBgColor == null) || (iTxtColor == null) ) { return; }

        castSendChangeViewMessage(id, iBgColor , ICastHelper.Property_BGColor);
        castSendChangeViewMessage(id, iTxtColor, ICastHelper.Property_Color);

        View view = findViewById(id);
        if (view == null) { return; }

        if ( view instanceof ServeButton ) {
            ServeButton serveButton = (ServeButton) view;
            setBackgroundColor(serveButton, iBgColor);
            setTextColor(view, iTxtColor);
        } else if ( view instanceof PlayersButton ) {
            PlayersButton button = (PlayersButton) view;
            if (bgColorDefKey.equals(ColorPrefs.ColorTarget.serveButtonBackgroundColor)) {
                button.setTextColorServer(iTxtColor, p);
                button.setBackgroundColorServer(iBgColor, p);
            } else {
                setTextColor(button, iTxtColor);
                button.setBackgroundColor(iBgColor);
            }
        } else if ( view instanceof TextView ) {
            TextView txtView = (TextView) view;
            setBackgroundColor(txtView, iBgColor);
            setTextColor(txtView, iTxtColor);
        }
    }

    // ----------------------------------------------------
    // -----------------game ball/match ball message ------
    // ----------------------------------------------------
    private FloatingMessage gameBallMessage = null;
    public boolean updateGameBallMessage(String sContext) {
        return updateGameBallMessage(sContext, null, null);
    }
    public boolean updateGameBallMessage(String sContext, Player[] gameBallFor, Boolean bShow) {
        if ( gameBallFor == null ) {
            gameBallFor = matchModel.isPossibleGameBallFor();
            bShow       = ListUtil.isNotEmpty(gameBallFor);
            Log.v(TAG, "Update game ball message - recalculated " + Arrays.asList(gameBallFor));
        }
        //Log.d(TAG, "updateGameBallMessage [" + sContext + "] " + Arrays.asList(gameBallFor) + " ==> show : " + bShow);
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

    public boolean showGameBallMessage(boolean bVisible, Player[] pGameBallFor) {
        if ( PreferenceValues.floatingMessageForGameBall(context, isPresentation()) == false ) {
            if ( gameBallMessage != null ) { gameBallMessage.setHidden(true); } // e.g. preferences where changed
            return false;
        }
        if ( (gameBallMessage == null) && (bVisible == false) ) {
            // we no longer return : make the gameball message floater anyway. This ensures that 'decision' floaters are created later and are more on top in the view hierarchy
            //return false;
        }

        int iResId = Brand.getGameSetBallPoint_ResourceId();
        Player[] possibleMatchBallFor = matchModel.isPossibleMatchBallFor();
        if ( ListUtil.isNotEmpty(possibleMatchBallFor) && (matchModel.playAllGames() == false) ) {
            iResId = R.string.oa_matchball;

            if ( bVisible ) {
                if ( Brand.isBadminton() || Brand.isGameSetMatch() ) {
                    iResId = R.string.oa_matchpoint;
                }
                if ( Brand.isRacketlon() ) {
                    // it can be matchball for the OTHER player
                    pGameBallFor = possibleMatchBallFor;

                    // if it is matchball for 2 players at once, it is gummiarm
                    if ( ListUtil.length(pGameBallFor) == 2 ) {
                        iResId = R.string.oa_gummiarm_point;
                    }
                }
            }
        }
        if ( (pGameBallFor != null) && (pGameBallFor.length == 2) && Brand.isGameSetMatch() && bVisible ) {
            iResId = R.string.oa_golden_point;
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
        if ( bVisible ) {
            castSendChangeViewMessage("gameBallMessage", sMsg, ICastHelper.Property_Text);
        }

        EnumSet<ShowPlayerColorOn> colorOns = PreferenceValues.showPlayerColorOn(context);
        if ( colorOns.contains(ShowPlayerColorOn.GameBallMessage) ) {
            if ( pGameBallFor != null && pGameBallFor.length == 1) {
                Player p = pGameBallFor[0];
                String sColor = matchModel.getColor(p);
                if ( StringUtil.isNotEmpty(sColor) ) {
                    try {
                        int iBgColor  = Color.parseColor(sColor);
                        int iTxtColor = ColorUtil.getBlackOrWhiteFor(sColor);
                        gameBallMessage.setColors(iBgColor, iTxtColor);
                        castSendChangeViewMessage("gameBallMessage", iTxtColor, ICastHelper.Property_Color);
                        castSendChangeViewMessage("gameBallMessage", iBgColor , ICastHelper.Property_BGColor);
                    } catch (IllegalArgumentException e) {
                    }
                }
            }
        }

        gameBallMessage.setHidden(bVisible == false);
        castSendChangeViewMessage("gameBallMessage", bVisible?"block":"none", ICastHelper.Property_Display);
        return bVisible;
    }

    // ----------------------------------------------------
    // -----------------decision message             ------
    // ----------------------------------------------------
    private FloatingMessage[] decisionMessages = new FloatingMessage[3]; // one for each player and a 'big' one for conducts (TODO: this last one)
    private void showDecisionMessage(Player pDecisionFor, Call call, String sMsg, int iMessageDurationSecs) {
        if ( getBlockToasts() ) { return; }
        final int fmIdx;
        boolean bCentral = (call != null && call.isConduct()) || (pDecisionFor == null);
        if ( bCentral ) {
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
                if ( bCentral ) {
                    direction = Direction.S;
                } else {
                    // place message over player requesting it
                    direction = pDecisionFor.equals(m_firstPlayerOnScreen) ? Direction.W : Direction.E;
                }
            }
            final int iResId_widthFraction = bCentral ? R.fraction.pt_conduct_width : R.fraction.pt_decision_msg_width;
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
        if ( bHide == false ) {
            // cast should take care of hiding the message itself
            castSendFunction(ICastHelper.Call_showDecision + "(" + "'" + sMsg + "'"
                                                           + ","       + fmIdx
                                                           + "," + "'" + call + "'"
                                                           + ","       + (call!=null ? call.isConduct() : "''")
                                                           + ")");
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

    private static final Call info_message_dummycall = Call.CW;  // use CW here just to let it appear as 'centered'...
    public void showMessage(String sMsg, int iMessageDuration) {
        showDecisionMessage(null, info_message_dummycall, sMsg, iMessageDuration);
    }
    public void hideMessage() {
        showDecisionMessage(null, info_message_dummycall, null, -1); // use CW here as well
    }

    public void showGuidelineMessage_FadeInOut(int iMessageDuration, int iMsgResId, Object ... formats) {
        showDecisionMessage(null, Call.CG, getOAString(iMsgResId, formats), iMessageDuration);
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

    private int getScreenHeightWidthMinimumFraction(int iResIdFraction) {
        return ViewUtil.getScreenHeightWidthMinimumFraction(display, iResIdFraction, context);
    }
    private boolean setVisibility(View v, int iVisibility) {
        if ( v == null ) {
            Log.w(TAG, "View is null");
            return false;
        }
        v.setVisibility(iVisibility);
        return true;
    }
}
