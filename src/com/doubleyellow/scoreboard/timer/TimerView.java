package com.doubleyellow.scoreboard.timer;

/**
 * Interface that a TimerView must implement.
 * Currently we have
 * - DialogTimerView (modal)
 * - SBTimerView (non-modal)
 * - MHTimerView (in matchhistory)
 */
public interface TimerView
{
    void setTitle(String s);
    void setTime(String s);
    void setTime(int iStartedCountDownAtSecs, int iSecsLeft, int iReminderAtSecs);
    void setWarnMessage(String s);
    void setPausedMessage(String s);
    /** mainly to close the timer */
    void cancel();
    /** e.g. to set a special message, or play a sound */
    void timeIsUp();
    /** called when a view is added to array of timerviews */
    void show();
    boolean isShowing();

    String TIMER_SHOWN          = "timerShown";
  //String TIMER_RESUME_AT      = "timerResumeAt";
  //String TIMER_STATE_STORED_AT= "timerStateStoredAt";
  //String TIMER_VIEW_TYPE      = "timerViewType";
  //String TIMER_PASSED_WARNING = "timerPassedWarning";
    String TIMER_AUTO_TRIGGERED = "timerAutoTriggered";
}
