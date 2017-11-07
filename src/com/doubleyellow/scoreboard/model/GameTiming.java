package com.doubleyellow.scoreboard.model;

import com.doubleyellow.util.DateUtil;
import com.doubleyellow.util.ListUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class GameTiming
{
    public enum Changed {
        None,
        Start,
        End,
        Both,
    }
    public enum ChangedBy {
        TimingCreated,
        ListenerAdded,
        TimerStarted,
        TimerEnded,
        FirstScoreOfGameEntered,
        GameEndingScoreReached,
        DialogOpened,
        DialogClosed,
        StartAndEndStillEqual,
    }
    public interface OnTimingChangedListener extends Model.OnModelChangeListener {
        /** Invoked each timer entry is added/adjusted */
        void OnTimingChanged(int iGameNr, Changed changed, long lTimeStart, long lTimeEnd, ChangedBy bEndOfTimer);
    }

    /** start in milliseconds since... */
    private long end;
    /** end in milliseconds since... */
    private long start;
    /** ideally should be true by correct usage by the referee */
    private boolean startTimeIsSetManually = false;

    private int iGameNrZeroBased;

    private List<OnTimingChangedListener> onTimingChangedListeners = null;

    /** version of the constructor for GUI preview mode only */
    public GameTiming(int iGameNrZeroBased, long s, long e) {
        this(iGameNrZeroBased, s,e, null);
    }
    GameTiming(int iGameNrZeroBased, long s, long e, List<OnTimingChangedListener> onTimingChangedListeners) {
        this.start   = s; //convertIfHumanReadable(s);
        this.end     = e; //convertIfHumanReadable(e);
        scoreTimings = new ArrayList<Integer>();
        setTimingListeners(onTimingChangedListeners);
        this.iGameNrZeroBased = iGameNrZeroBased;

        triggerListeners(Changed.Both, ChangedBy.TimingCreated);
    }

    void setTimingListeners(List<OnTimingChangedListener> onTimingChangedListeners) {
        this.onTimingChangedListeners = onTimingChangedListeners;
    }

    /** changedflag: 1: if start changed, 2 if end changed, 3 of both start and end changed */
    private void triggerListeners(Changed iChangedFlag, ChangedBy bEndOfTimer) {
        if ( this.onTimingChangedListeners != null ) {
            for(OnTimingChangedListener l: this.onTimingChangedListeners) {
                l.OnTimingChanged(iGameNrZeroBased, iChangedFlag, this.start, this.end, bEndOfTimer);
            }
        }
    }

    /*
        private long convertIfHumanReadable(long l) {
            String s = String.valueOf(l);
            if ( StringUtil.length(s) == 8+6 && s.startsWith("2") ) {
                l = DateUtil.parseString2Date(s, DateUtil.YYYYMMDDHHMMSS).getTime();
            }
            return l;
        }
    */
    long getStart() {
        return this.start;
    }
    long getEnd() {
        return this.end;
    }
/*
    long getStart(boolean bInMillis) {
        if ( bInMillis ) return this.start;
        return convertToHumanReadable(this.start);
    }
    long getEnd(boolean bInMillis) {
        if ( bInMillis ) return this.end;
        return convertToHumanReadable(this.end);
    }
    private long convertToHumanReadable(long l) {
        return Long.parseLong(DateUtil.formatDate2String(l, DateUtil.YYYYMMDDHHMMSS));
    }
*/
    /** score timings of the game in progress */
    private List<Integer> scoreTimings = null;

    boolean startTimeIsSetManually() {
        return this.startTimeIsSetManually;
    }
    long updateStart(int iOffsetInSeconds, ChangedBy changedBy) {
        this.startTimeIsSetManually = (iOffsetInSeconds == 0);
        return update(true, iOffsetInSeconds, changedBy);
    }
    void updateEnd(ChangedBy changedBy) {
        update(false, 0, changedBy);
    }
    private long update(boolean bStart, int iOffsetInSeconds, ChangedBy changedBy) {
        if ( bStart ) {
            this.start = System.currentTimeMillis() + (iOffsetInSeconds * 1000);
            if ( this.start > this.end ) {
                this.end = this.start;
            }
            triggerListeners(Changed.Start, changedBy);
            return this.start;
        } else {
            this.end = System.currentTimeMillis();
            triggerListeners(Changed.End, changedBy);
            return this.end;
        }
    }

    /** record the 'seconds since start of game' for the scoreline */
    int addTiming() {
        if ( scoreTimings == null ) { return -1; }
        long lMsSinceStartOfGame      = System.currentTimeMillis() - this.start;
        int  iSecondsSinceStartOfGame = DateUtil.convertToSeconds(lMsSinceStartOfGame);
        addTiming(iSecondsSinceStartOfGame);
        return iSecondsSinceStartOfGame;
    }

    void addTiming(int iSecondsSinceStartOfGame) {
        if ( scoreTimings == null ) { return; }

        scoreTimings.add(iSecondsSinceStartOfGame);
    }
    List<Integer> getScoreTimings() {
        return scoreTimings;
    }
    int removeTimings(int iScoreLineNr) {
        if ( scoreTimings == null ) { return -1; }

        int iRemoved = 0;
        while(scoreTimings.size() > iScoreLineNr ) {
            ListUtil.removeLast(scoreTimings);
            iRemoved++;
        }
        return iRemoved;
    }
    /** should only be called for matches stored with version before 3.19 that did not store timings in that much detail */
    void noScoreTimings() {
        scoreTimings = null;
    }

  //private static final int I_NR_OF_SECS_TO_CONSIDER_GAME_DUMMY = 90;

    public String getStartHHMM() {
        return getHHMM(this.start);
    }
    public String getEndHHMM() {
        return getHHMM(this.end);
    }
    public long getDuration() {
        if ( this.start == 0 ) {
            // normally only if from parsed json string, modified manually
            // end time is no real time but a duration in minutes
            return (long) 1000 * 60 * this.end;
        }
        return this.end - this.start;
    }
    /** if game was less than a minute, time is in seconds but negative */
    public int getDurationMM() {
        if ( this.start == 0 ) {
            // normally only if from parsed json string, modified manually
            // end time is no real time but a duration in minutes
            return (int) this.end;
        } else {
            long lDuration = this.end - this.start;
            int i = DateUtil.convertToMinutesCeil(lDuration);

/*
            int i = DateUtil.convertToMinutes(lDuration);
            if ( i <= 1 ) {
                // demo match entered quickly, return the number of seconds (return number smaller than zero to let caller know)
                i = -1 * DateUtil.convertToSeconds(lDuration);
            }
            if ( ScoreBoard.isInSpecialMode() && (i != 0) ) {
                // e.g. same match repeated over and over for screenshots
                i = 5 + (Math.abs(i) % 8);
            }
*/
            return i;
        }
    }
    private String getHHMM(long startOrEnd) {
        if ( this.start == 0 ) {
            // normally only if from parse json string, modified manually
            return null;
        }
        String sFormat = "HH:mm";
/*
        int seconds = DateUtil.convertToSeconds(this.end - this.start);
        if ( seconds < I_NR_OF_SECS_TO_CONSIDER_GAME_DUMMY) {
            // most likely testing the app to see what it looks like
            sFormat = "mm:ss";
        }
*/
        return DateUtil.formatDate2String(new Date(startOrEnd), sFormat);
    }

    @Override public String toString() {
        return DateUtil.formatDate2String(start, DateUtil.YYYYMMDD_HHMMSS) + " - " + DateUtil.formatDate2String(end, DateUtil.HHMMSS) + " (" + getDurationMM() + " min)";
    }
}
