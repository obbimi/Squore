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

package com.doubleyellow.scoreboard.timer;

import android.content.Context;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.util.Log;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.model.GameTiming;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.scoreboard.speech.Speak;
import com.doubleyellow.scoreboard.vico.IBoard;
import com.doubleyellow.util.StringUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * Class that performs the actual 'count down' of a timer.
 * It invokes methods on the currently 'active' timerview to provide feedback to the user.
 */
public class Timer
{
    private static final String TAG = "SB." + Timer.class.getSimpleName();

            static SBCountDownTimer countDownTimer  = null;
    private static int              iSecondsInitial = 0;
    private static int              iReminderAtSecs = 0;
    private static String           sReminderText   = null;
    public  static Type             timerType       = Type.UntilStartOfFirstGame;
    private static ScoreBoard       scoreBoard;
    private boolean                 autoTriggered   = false;

    public int getSecondsLeft() {
        if ( countDownTimer == null ) { return 0; }
        return countDownTimer.secsLeft;
    }

    private static synchronized boolean isAtOrPassedWarning() {
        if ( countDownTimer == null ) { return true; }
        return countDownTimer.secsLeft <= iReminderAtSecs;
    }
    boolean isAutoTriggered() {
        return autoTriggered;
    }

    private long m_lToLateBase = 0L;
    public long getToLateBase() {
        if ( m_lToLateBase == 0L ) {
            m_lToLateBase = SystemClock.elapsedRealtime();
        }
        return m_lToLateBase;
    }
    public void clearToLateBase() {
        m_lToLateBase = 0L;
    }

    /** points to the currently active instance of the TimerView */
    private static Map<String, TimerView> timerViews = new HashMap<>();

    public Timer(final ScoreBoard scoreBoard, final Type timerType, int iSecondsInitial, int iSeconds, int iReminderAt, boolean bAutoTriggered) {
        Timer.iSecondsInitial = iSecondsInitial;
        Timer.iReminderAtSecs = iReminderAt;
        Timer.timerType       = timerType;
        Timer.scoreBoard      = scoreBoard;
        this.autoTriggered   = bAutoTriggered;

        int iReminderMsgId = timerType.getHalftTimeMsgResId();
        if ( iReminderMsgId != 0 ) {
            Timer.sReminderText = PreferenceValues.getOAString(scoreBoard, iReminderMsgId, formatTime(iReminderAt, true)) ;
        } else {
            Timer.sReminderText = "";
        }

        countDownTimer = new SBCountDownTimer(iSeconds);
        countDownTimer.start();
    }

    private static String getTitle(Context scoreBoard, Type timerType) {
        String sTitle        = null;
        int iResId = timerType.getNameResId();
        if ( (iResId == 0) || (scoreBoard == null) ) {
            sTitle           = StringUtil.capitalize(timerType);
        } else {
            sTitle           = scoreBoard.getString(iResId);
        }
        return sTitle;
    }

    /** e.g. to be invoked from MatchHistory activity and cast.Presentation */
    public static boolean addTimerView(boolean bIsPresentation, TimerViewContainer container) {
        if ( container == null ) { return false; }
        return addTimerView(bIsPresentation, container.getTimerView());
    }
    public static boolean addTimerView(boolean bIsPresentation, TimerView view) {
        if ( view == null ) { return false; }
        boolean bAdded = false;
        TimerView tvOld = Timer.timerViews.put(bIsPresentation + ":" + view.getClass().getName(), view);
        boolean bIsNew = tvOld == null;
        if ( bIsNew ) {
            //Log.d(TAG, "Added new " + view.toString());
            String sTitle = getTitle(scoreBoard, timerType);
            view.setTitle(sTitle);
            view.setPausedMessage("");

            //view.setWarnMessage(isPassedWarning() ? sReminderText : "");
            if ( countDownTimer != null ) {
                updateTimerView(countDownTimer, view);
                if ( countDownTimer.secsLeft == 0 ) {
                    IBoard.setBlockToasts(true);
                    view.timeIsUp();
                    IBoard.setBlockToasts(false);
                }
            } else {
                // timer not running: prevent that previous time or 'Time!' is still visible
                view.setTime(formatTime(0L, false));
            }
            bAdded = true;
        } else {
            //Log.d(TAG, "Already there " + view.toString());
            bAdded = false;
        }
        if ( ScoreBoard.timer != null /*&& ScoreBoard.timer.isShowing()*/ ) {
            view.show();
        }
        return bAdded;
    }
    public static boolean removeTimerView(boolean bIsPresentation, TimerViewContainer container) {
        if ( container == null ) { return false; }
        return removeTimerView(bIsPresentation, container.getTimerView());
    }
    public static boolean removeTimerView(boolean bIsPresentation, TimerView view) {
        if ( view == null ) { return false; }
        Class<? extends TimerView> aClass = view.getClass();
        return removeTimerView(bIsPresentation, aClass);
    }

    public static boolean removeTimerView(boolean bIsPresentation, Class<? extends TimerView> aClass) {
        TimerView tvOld = Timer.timerViews.remove(bIsPresentation + ":" + aClass.getName());
        boolean bRemoved = tvOld != null;
        if ( bRemoved ) {
            //view.cancel();
            tvOld.cancel();
            Log.d(TAG, "Removed " + tvOld.toString());
        } else {
            //Log.d(TAG, "Already No more TimerView of class " + aClass.getName());
        }
        return bRemoved;
    }

/*
    public void show() {
        for(TimerView timerView: Timer.timerViews) {
            timerView.show();
        }
    }
*/

    public synchronized void cancel() {
        if ( countDownTimer != null ) {
            countDownTimer.stop();
        }
        if ( timerViews != null ) {
            for(TimerView timerView: Timer.timerViews.values()) {
                timerView.cancel();
            }
        }
        countDownTimer = null;
    }

    public boolean isShowing() {
        boolean bIsShowing = false;
        for(TimerView timerView: Timer.timerViews.values()) {
            bIsShowing = bIsShowing || timerView.isShowing();
        }
        return bIsShowing;
    }

    private static String formatTime(long secs, boolean bSkipMinutesIfZero) {
        long mins = secs/60;
        secs = secs - 60 * mins;
        String sReturn = StringUtil.lpad(secs, '0', 2);
        if ( (mins > 0) || (bSkipMinutesIfZero == false) ) {
            sReturn = StringUtil.lpad(mins, '0', 2) + ":" + sReturn;
        }
        return sReturn;
    }

    public static int iSpeedUpFactor = 1;

    private static void updateTimerView(SBCountDownTimer countDownTimer, TimerView timerView) {
        timerView.setTime(Timer.formatTime(countDownTimer.secsLeft, false));
        timerView.setTime(Timer.iSecondsInitial, countDownTimer.secsLeft, Timer.iReminderAtSecs);
        int iAtLeastLeft = Math.max(iReminderAtSecs - 10, iReminderAtSecs / 3 * 2); // safety region mainly for when having a speedup value
        if ( isAtOrPassedWarning() ) {
            if ( countDownTimer.secsLeft >= iAtLeastLeft ) {
                timerView.setWarnMessage(Timer.sReminderText);
            }
            if ( countDownTimer.secsLeft == iReminderAtSecs ) {
                Speak.getInstance().setTimerMessage(Timer.sReminderText);
            }
        } else {
            //timerView.setWarnMessage("");
        }
    }

    public class SBCountDownTimer extends CountDownTimer
    {
                int    secsLeft  = -1;
      //private int    iCounter  = 0;
        private String TAG       = "SB." + SBCountDownTimer.class.getSimpleName();

        private SBCountDownTimer(int iSecsInFuture) {
            super(iSecsInFuture * 1000 / iSpeedUpFactor, 1000 / iSpeedUpFactor);
            for(TimerView timerView:timerViews.values()) {
                if ( timerView.isShowing() == false ) {
                    timerView.show();
                }
            }
            Model matchModel = ScoreBoard.getMatchModel();
            if ( (matchModel != null) && matchModel.hasStarted() == false ) {
                // set chrono's to zero and stop counting
                matchModel.timestampStartOfGame(GameTiming.ChangedBy.TimerStarted);
            }
        }

        @Override public void onTick(long millisUntilFinished) {
            secsLeft = (int) (millisUntilFinished / (1000 / iSpeedUpFactor));

            for(TimerView timerView:timerViews.values()) {
                updateTimerView(this, timerView);
            }
            if ( secsLeft == iReminderAtSecs ) {
                scoreBoard.triggerEvent(ScoreBoard.SBEvent.timerWarning, Timer.timerType, Timer.iReminderAtSecs );
            }
        }

        @Override public void onFinish() {
            log("onFinish :");
            this.onTick(0);
            scoreBoard.triggerEvent(ScoreBoard.SBEvent.timerEnded, Timer.timerType);
            for(TimerView timerView:timerViews.values()) {
                timerView.timeIsUp();
                if ( timerView instanceof NotificationTimerView ) { continue; } // do not hide NotificationTimerView when time is up, irrespective of preference

                if ( PreferenceValues.cancelTimerWhenTimeIsUp(scoreBoard) ) {
                    timerView.cancel();
                }
            }
            secsLeft = 0;

            Speak.getInstance().setTimerMessage(null);
        }

        public SBCountDownTimer restart(int iSecondsLeft) {
            SBCountDownTimer newCounter = new SBCountDownTimer(iSecondsLeft);
            log("Restarting with " + iSecondsLeft + " secs");
          //newCounter.iCounter = this.iCounter+1;
            newCounter.start();
            return newCounter;
        }

        void stop() {
            log("Stopping timer with " + this.secsLeft + " secs left");
            super.cancel();
        }

        private void log(String sMessage) {
            //Log.d(TAG + "." + iCounter, sMessage);
        }
    }
}
