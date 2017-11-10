package com.doubleyellow.scoreboard.timer;

import android.content.Context;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.util.Log;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.model.GameTiming;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.scoreboard.vico.IBoard;
import com.doubleyellow.util.StringUtil;

import java.util.HashSet;

/**
 * Class that performs the actual 'count down' of a timer.
 * It invokes methods on the currently 'active' timerview to provide feedback to the user.
 */
public class Timer
{
    private static final String TAG = "SB." + Timer.class.getSimpleName();

            static SBCountDownTimer   countDownTimer  = null;
    private static int        iSecondsInitial = 0;
    private static int        iReminderAtSecs = 0;
    private static String     sReminderText   = null;
    public  static Type       timerType       = Type.UntillStartOfNextGame;
    private static ScoreBoard scoreBoard;
    private boolean           autoTriggered   = false;

    public int getSecondsLeft() {
        return countDownTimer.secsLeft;
    }

    private static boolean isAtOrPassedWarning() {
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

    /** points the the currently active instance of the TimerView */
    public static HashSet<TimerView> timerViews = new HashSet<TimerView>();

    public Timer(final ScoreBoard scoreBoard, final Type timerType, int iSecondsInitial, int iSeconds, int iReminderAt, boolean bAutoTriggered) {
        Timer.iSecondsInitial = iSecondsInitial;
        Timer.iReminderAtSecs = iReminderAt;
        Timer.timerType       = timerType;
        Timer.scoreBoard      = scoreBoard;
        this.autoTriggered   = bAutoTriggered;

        int iReminderMsgId = timerType.getMsgId(1);
        if ( iReminderMsgId != 0 ) {
            this.sReminderText = PreferenceValues.getOAString(scoreBoard, iReminderMsgId, formatTime(iReminderAt, true)) ;
        } else {
            this.sReminderText = "";
        }

        countDownTimer = new SBCountDownTimer(iSeconds);
        countDownTimer.start();
    }

    private static String getTitle(Context scoreBoard, Type timerType) {
        String sTitle        = null;
        int iResId = timerType.getMsgId(0);
        if ( (iResId == 0) || (scoreBoard == null) ) {
            sTitle           = StringUtil.capitalize(timerType);
        } else {
            sTitle           = scoreBoard.getString(iResId);
        }
        return sTitle;
    }

    /** e.g. to be invoked from MatchHistory activity and cast.Presentation */
    public static void addTimerView(TimerViewContainer container) {
        if ( container == null ) { return; }
        addTimerView(container.getTimerView());
    }
    public static void addTimerView(TimerView view) {
        if ( view == null ) { return; }
        boolean bIsNew = Timer.timerViews.add(view);
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
        } else {
            Log.d(TAG, "Already there " + view.toString());
        }
        if ( ScoreBoard.timer != null /*&& ScoreBoard.timer.isShowing()*/ ) {
            view.show();
        }
    }
    public static boolean removeTimerView(TimerViewContainer container) {
        if ( container == null ) { return false; }
        return removeTimerView(container.getTimerView());
    }
    public static boolean removeTimerView(TimerView view) {
        if ( view == null ) { return false; }
        boolean bRemoved = Timer.timerViews.remove(view);
        if ( bRemoved ) {
            view.cancel();
            Log.d(TAG, "Removed " + view.toString());
        } else {
            Log.d(TAG, "Already GONE " + view.toString());
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

    public void cancel() {
        countDownTimer.stop();
        if ( timerViews != null ) {
            for(TimerView timerView: Timer.timerViews) {
                timerView.cancel();
            }
        }
        Timer.countDownTimer = null;
    }

    public boolean isShowing() {
        boolean bIsShowing = false;
        for(TimerView timerView: Timer.timerViews) {
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
        int iAtLeastLeft = Math.max(iReminderAtSecs - 10, iReminderAtSecs / 3 * 2);
        if ( (isAtOrPassedWarning()) && (countDownTimer.secsLeft >= iAtLeastLeft) ) {
            timerView.setWarnMessage(Timer.sReminderText);
        } else {
            //timerView.setWarnMessage("");
        }
    }

    public class SBCountDownTimer extends CountDownTimer
    {
                int    secsLeft  = -1;
      //private int    iCounter  = 0;
        private String TAG       = "SB." + SBCountDownTimer.class.getSimpleName();

        SBCountDownTimer(int iSecsInFuture) {
            super(iSecsInFuture * 1000 / iSpeedUpFactor, 1000 / iSpeedUpFactor);
            for(TimerView timerView:timerViews) {
                if ( timerView.isShowing() == false ) {
                    timerView.show();
                }
            }
            if ( ScoreBoard.matchModel.hasStarted() == false ) {
                // set chrono's to zero and stop counting
                ScoreBoard.matchModel.timestampStartOfGame(GameTiming.ChangedBy.TimerStarted);
            }
        }

        @Override public void onTick(long millisUntilFinished) {
            secsLeft = (int) (millisUntilFinished / (1000 / iSpeedUpFactor));

            for(TimerView timerView:timerViews) {
                updateTimerView(this, timerView);
            }
            if ( secsLeft == iReminderAtSecs ) {
                scoreBoard.triggerEvent(ScoreBoard.SBEvent.timerWarning, Timer.timerType); // currently use to e.g. vibarte and make a noice
            }
        }

        @Override public void onFinish() {
            log("onFinish :");
            this.onTick(0);
            scoreBoard.triggerEvent(ScoreBoard.SBEvent.timerEnded, Timer.timerType);
            for(TimerView timerView:timerViews) {
                timerView.timeIsUp();
                if ( timerView instanceof NotificationTimerView ) { continue; } // do not hide NotificationTimerView when time is up, irrespective of preference

                if ( PreferenceValues.cancelTimerWhenTimeIsUp(scoreBoard) ) {
                    timerView.cancel();
                }
            }
            secsLeft = 0;
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
