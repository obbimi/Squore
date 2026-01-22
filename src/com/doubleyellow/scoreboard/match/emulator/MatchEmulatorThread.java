/*
 * Copyright (C) 2026  Iddo Hoeve
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
package com.doubleyellow.scoreboard.match.emulator;

import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.dialog.EndGame;
import com.doubleyellow.scoreboard.dialog.IBaseAlertDialog;
import com.doubleyellow.scoreboard.main.DialogManager;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.model.Call;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.model.Player;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.scoreboard.timer.Timer;
import com.doubleyellow.scoreboard.timer.TwoTimerView;
import com.doubleyellow.scoreboard.timer.Type;
import com.doubleyellow.util.Feature;
import com.doubleyellow.util.Params;

import java.util.Map;
import java.util.Random;

/**
 * Helps in emulating a Match being scored.
 *
 * Mainly to help in testing complex setup with multiple devices, livescore MQTT, tcboard
 */
public class MatchEmulatorThread extends Thread {

    private static final String TAG = "SB." + MatchEmulatorThread.class.getSimpleName();

    private int     iLikelihood_Undo;
    private int     iLikelihood_SwitchServeSideOnHandout;

    private Random     r;
    private ScoreBoard scoreBoard;
    private Model      matchModel;

    private int        iLastRallyDuration;
    private boolean    bSwitchServeSide = false;

    private Params m_settings = null;

/*
    private static MatchEmulatorThread instance = null;
    public static MatchEmulatorThread getInstance() {
        if ( instance == null ) {
            instance = new MatchEmulatorThread();
        }
        return instance;
    }
    private MatchEmulatorThread() {}
*/

    public void init(ScoreBoard scoreBoard, Model model) {
        init(scoreBoard, model, null);
    }
    public void init(ScoreBoard scoreBoard, Model model, Map mSettings) {
        this.scoreBoard = scoreBoard;
        this.matchModel = model;
        if ( mSettings == null ) { return; }

        m_settings = new Params(mSettings);

        Timer.iSpeedUpFactor = m_settings.getOptionalInt(Keys.SpeedUpFactor, 1);

        //bUseWarmupTimer = m_settings.getOptionalBoolean(Keys.StartWarmupTimer      , false);
        boundaries = new int[8];

        int iLikelihoodAppeal           = m_settings.getOptionalInt(Keys.LikelihoodAppeal          , 12);
        int iLikelihoodPlayerAWinsRally = m_settings.getOptionalInt(Keys.LikelihoodPlayerAWinsRally, 60);
        boundaries[1] = (100 - iLikelihoodAppeal);
        boundaries[0] = boundaries[1] * iLikelihoodPlayerAWinsRally / 100;
        for(int i=2; i<=7; i++) {
            boundaries[i] = boundaries[i-1] + iLikelihoodAppeal / 6;
        }

        this.iRallyDuration_AverageAndDeviation = new int[] { m_settings.getOptionalInt(Keys.RallyDuration_Average     , 20)
                                                            , m_settings.getOptionalInt(Keys.RallyDuration_Deviation   , 10)};

        r = new Random(System.currentTimeMillis());


        this.iLikelihood_Undo                     = m_settings.getOptionalInt(Keys.LikelihoodUndoRequiredByRef   , 5);
        this.iLikelihood_SwitchServeSideOnHandout = m_settings.getOptionalInt(Keys.LikelihoodSwitchServeSideOnHandout, 10);
    }

    private int[] iRallyDuration_AverageAndDeviation;

  //private int[] boundaries = new int[] { 35, 70, 75, 80, 85, 90, 95, 100 };
  //private int[] boundaries = new int[] { 41, 82, 85, 88, 91, 94, 97, 100 };
    private int[] boundaries;
    private enum RallyOutcome {
        WinPlayerA,               // 35
        WinPlayerB,               // 70
        AppealPlayerA_Stroke,     // 75
        AppealPlayerA_YesLet,     // 80
        AppealPlayerA_NoLet,      // 85
        AppealPlayerB_Stroke,     // 90
        AppealPlayerB_YesLet,     // 95
        AppealPlayerB_NoLet,      // 100
    }

    @Override public void run() {
        Looper.prepare();
        emulateMatchScore();
    }

    private boolean bKeepLooping = false;

    private static long iHandlerLooperEnteredLast = System.currentTimeMillis();

    private void emulateMatchScore() {
        bKeepLooping = true;

        while(bKeepLooping) {
            // get ready for next really/scoreboard action
            pause(12);

            Handler handler = new Handler(scoreBoard.getMainLooper());
            handler.post(() -> {
                iHandlerLooperEnteredLast = System.currentTimeMillis();

                if ( scoreBoard.isDialogShowing() ) {
                    try {
                        DialogManager dialogManager = DialogManager.getInstance();

                        IBaseAlertDialog baseDialog = dialogManager.baseDialog;
                        if ( baseDialog == null ) {
                            boolean bTimerIsShowing        = (ScoreBoard.timer != null) && ScoreBoard.timer.isShowing();
                            Log.d(TAG, "bTimerIsShowing : " + bTimerIsShowing);
                        } else {
                            if ( baseDialog instanceof EndGame ) {
                                EndGame endGame = (EndGame) baseDialog;
                                if ( PreferenceValues.useTimersFeature(scoreBoard) == Feature.Suggest ) {
                                    endGame.handleButtonClick(EndGame.BTN_END_GAME_PLUS_TIMER);
                                } else {
                                    baseDialog.handleButtonClick(EndGame.BTN_END_GAME);
                                }
                            } else if ( baseDialog instanceof TwoTimerView /* WarmupTimerView, PauseTimerView, ... */ ) {
                                if ( ScoreBoard.timer.getSecondsLeft() > 0 ) {
                                    // wait for timer to end
                                    return;
                                } else {
                                    Log.d(TAG, "No more seconds left in timer 1");
                                }
                            } else {
                                baseDialog.handleButtonClick(DialogInterface.BUTTON_POSITIVE);
                            }
                            baseDialog.dismiss();
                            return;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, e.getMessage(), e);
                    }
                } else {
                    if ( ( ! matchModel.hasStarted() ) && ScoreBoard.lastTimerType != Type.UntilStartOfFirstGame && PreferenceValues.useTimersFeature(scoreBoard)!= Feature.DoNotUse) {
                        scoreBoard.handleMenuItem(R.id.dyn_timer);
                        //scoreBoard._showTimer(Type.Warmup, false, ViewType.Inline, null);
                        return;
                    }
                }
                if ( ScoreBoard.timer != null && ScoreBoard.timer.isShowing() ) {
                    if ( ScoreBoard.timer.getSecondsLeft() > 0 ) {
                        return;
                    } else {
                        Log.d(TAG, "No more seconds left in timer 2");
                    }
                }
                if ( matchModel.matchHasEnded() ) {
                    stopLoop();
                    return;
                }

                if ( matchModel.hasStarted() && this.iLikelihood_Undo > 0 ) {
                    long iRnd = r.nextInt(100);
                    if ( iRnd < this.iLikelihood_Undo ) {
                        scoreBoard.showInfoMessage("Emulated Undo by ref", 5);
                        matchModel.undoLast();
                        return;
                    }
                }

                if ( matchModel.isLastPointHandout() && (bSwitchServeSide == false) ) {
                    Player server = matchModel.getServer();
                    if ( ( this.iLikelihood_SwitchServeSideOnHandout < 0 && Player.A.equals(server) )
                    ||   ( this.iLikelihood_SwitchServeSideOnHandout > 0 && Player.B.equals(server) )
                    )
                    {
                        long iRnd = r.nextInt(100);
                        if ( iRnd < Math.abs(this.iLikelihood_SwitchServeSideOnHandout) ) {
                            scoreBoard.changeSide(server);
                            bSwitchServeSide = true;
                            return;
                        }
                    }
                }
                bSwitchServeSide = false;

                RallyOutcome outcome = randomRallyOutcome();
                switch (outcome) {
                    case WinPlayerA: {
                        matchModel.changeScore(Player.A);
                        break;
                    }
                    case WinPlayerB: {
                        matchModel.changeScore(Player.B);
                        break;
                    }
                    case AppealPlayerA_Stroke: {
                        matchModel.recordAppealAndCall(Player.A, Call.ST);
                        break;
                    }
                    case AppealPlayerA_YesLet: {
                        matchModel.recordAppealAndCall(Player.A, Call.YL);
                        break;
                    }
                    case AppealPlayerA_NoLet: {
                        matchModel.recordAppealAndCall(Player.A, Call.NL);
                        break;
                    }
                    case AppealPlayerB_Stroke: {
                        matchModel.recordAppealAndCall(Player.B, Call.ST);
                        break;
                    }
                    case AppealPlayerB_YesLet: {
                        matchModel.recordAppealAndCall(Player.B, Call.YL);
                        break;
                    }
                    case AppealPlayerB_NoLet: {
                        matchModel.recordAppealAndCall(Player.B, Call.NL);
                        break;
                    }
                }
                String sMsg = String.format("Emulated %s after rally of %d seconds (speedup factor %d)", outcome, iLastRallyDuration, Timer.iSpeedUpFactor);
                scoreBoard.showInfoMessage(sMsg, 5);

            });

            if ( (System.currentTimeMillis() - iHandlerLooperEnteredLast > 30000) ) {
                Log.w(TAG, "Breaking out of emulator. Something went wrong?!");
                stopLoop();
                break;
            }

            iLastRallyDuration = (int) randomRallyDuration();
            pause(iLastRallyDuration);
        }

        scoreBoard.stopMatchEmulatorMode(matchModel.matchHasEnded());
    }
    public void stopLoop() {
        bKeepLooping = false;
    }

    private double randomRallyDuration() {
        double v = r.nextGaussian();
        double v1 = v * iRallyDuration_AverageAndDeviation[1] + iRallyDuration_AverageAndDeviation[0];
        return Math.max(2, v1);
    }

    private RallyOutcome randomRallyOutcome() {
        int iOutcome = r.nextInt(100);
        for( RallyOutcome ro : RallyOutcome.values() ) {
            if ( iOutcome <= boundaries[ro.ordinal()] ) {
                return ro;
            }
        }
        return RallyOutcome.WinPlayerA;
    }

    private void pause(int lSeconds) {
        try {
            synchronized (this) {
                //Log.d(TAG, "Waiting...");
                wait(lSeconds * 1000L / Timer.iSpeedUpFactor);
            }
        } catch (InterruptedException e) {
            //Log.e(TAG, "?? " + e); // normally only when thread is deliberately stopped/interrupted
        }
        //Log.d(TAG, "Resumed");
    }
}
