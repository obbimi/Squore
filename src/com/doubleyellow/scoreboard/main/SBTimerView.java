package com.doubleyellow.scoreboard.main;

import android.content.Context;
import android.view.View;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;
import com.doubleyellow.android.util.ColorUtil;
import com.doubleyellow.android.view.SimpleGestureListener;
import com.doubleyellow.android.view.ViewUtil;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.prefs.ColorPrefs;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.scoreboard.prefs.ShowOnScreen;
import com.doubleyellow.scoreboard.timer.Timer;
import com.doubleyellow.scoreboard.timer.TimerView;
import com.doubleyellow.scoreboard.timer.Type;
import com.doubleyellow.scoreboard.vico.IBoard;
import com.doubleyellow.util.Direction;
import com.doubleyellow.util.ListUtil;
import com.doubleyellow.util.StringUtil;

import java.util.Map;
import java.util.Set;

/**
 * Used when the dialog box is made 'hidden' by the user
 **/
public class SBTimerView implements TimerView
{
    private TextView    txtView      = null;
    private Chronometer cmToLate     = null;
    //private SBToast  toast        = null;
    private boolean  bWarningMode = false;

    private Context context = null;
    private IBoard  iBoard  = null;
    private Map<ColorPrefs.ColorTarget, Integer> mColors;
    public SBTimerView(TextView textView, Chronometer cmToLate, Context context, IBoard iBoard) {
        this.txtView  = textView;
        this.cmToLate = cmToLate;
        if ( context instanceof ScoreBoard ) {
            ClickListener clickListener = new ClickListener((ScoreBoard) context);
            //txtView.setOnClickListener    (clickListener);
            //txtView.setOnLongClickListener(clickListener);
            SimpleGestureListener scoreButtonGestureListener = new SimpleGestureListener(null, null, clickListener, clickListener, clickListener);
            txtView.setOnTouchListener(scoreButtonGestureListener);
        }
        this.context = context;
        this.iBoard  = iBoard;
        mColors = ColorPrefs.getTarget2colorMapping(context);
    }
    @Override public void setTitle(String s) { }
    @Override public void setPausedMessage(String s) { } // no pausing here anymore

    @Override public void setTime(int iStartedCountDownAtSecs, int iSecsLeft, int iReminderAtSecs) {

    }
    @Override public void setTime(String s) {
        txtView.setText(/*sTitle + " " +*/ s); // 20140526: 'inline' Timer is to small to show title
    }

    @Override public void setWarnMessage(String s) {
        // toggle color to have visible feedback
        if ( StringUtil.isEmpty(s) ) return;
        if ( bWarningMode == false ) {
            // only show toast message first time
            Direction d = ViewUtil.isLandscapeOrientation(context)? Direction.S:Direction.E; // ensure toast is not over clock that is still running
            boolean bMatchHistoryIsShowing = false; // TODO: determine

            if ( bMatchHistoryIsShowing ) {
                // assume a 'childactivity' dialog with e.g. the graph of the last game is showing
                d = Direction.NW;
            }
            showToast(s, 3, d);
        }
        bWarningMode = true;
        setColors(txtView, cmToLate, bWarningMode);
    }

    private void showToast(String sMessage, int iDuration, Direction direction) {
        if ( iBoard != null ) {
            iBoard.showToast(sMessage, iDuration, direction);
        }
    }

    @Override public void timeIsUp() {
        String sTime = context.getString(R.string.oa_time);
        showToast(sTime, 3, Direction.None); // not to long for now, can not be easily dismissed yet (floating message can)
        if ( context instanceof ScoreBoard ) {
            // allow user to tap it to start the game
            txtView.setText(PreferenceValues.getGameOrSetString(context, R.string.Start_game));
            if ( (cmToLate != null) && (ScoreBoard.timer != null) && Type.UntillStartOfNextGame.equals(Timer.timerType) ) {
                cmToLate.setVisibility(View.VISIBLE);
                cmToLate.setBase(ScoreBoard.timer.getToLateBase());
                cmToLate.start();
            }
        } else {
            // assuming it is chromecast
            txtView.setText(sTime);
            bWarningMode = false;
            setColors(txtView, cmToLate, bWarningMode);
        }
    }

    @Override public void cancel() {
        txtView.setVisibility(View.GONE);
        if ( cmToLate != null ) {
            cmToLate.setVisibility(View.GONE);
        }
        bWarningMode = false;
    }

    @Override public void show() {
        txtView.setVisibility(View.VISIBLE);
        setColors(txtView, cmToLate, bWarningMode);
    }

    @Override public boolean isShowing() {
        return txtView.getVisibility()==View.VISIBLE;
    }

    private void setColors(TextView txtView, Chronometer cmToLate, boolean bWarningMode) {
        if ( txtView == null ) { return; }
        if ( mColors == null ) { return; }
        ColorPrefs.ColorTarget colorTarget = bWarningMode ? ColorPrefs.ColorTarget.timerButtonBackgroundColor_Warn : ColorPrefs.ColorTarget.timerButtonBackgroundColor;
        Integer iColorBG  = mColors.get(colorTarget);
        if ( iColorBG == null ) { return; }
        int iColorTxt = ColorUtil.getBlackOrWhiteFor(iColorBG);
        ColorUtil.setBackground(txtView, iColorBG);
        txtView.setTextColor (iColorTxt);
        if ( cmToLate != null ) {
            cmToLate.setBackgroundColor(context.getResources().getColor(android.R.color.transparent));
            cmToLate.setTextColor (iColorTxt);
        }
    }

    private class ClickListener implements View.OnClickListener, View.OnLongClickListener, SimpleGestureListener.TwoFingerClickListener
    {
        private ScoreBoard scoreBoard = null;
        private ClickListener(ScoreBoard scoreBoard) {
            this.scoreBoard = scoreBoard;
        }

        @Override public boolean onTwoFingerClick(View v) {
            Set<ShowOnScreen> newPrefValue = PreferenceValues.toggleMatchGameDurationChronoVisibility(context);
            String sSetting = context.getString(R.string.pref_showLastGameDurationChronoOn);
            String sNewValue = ListUtil.join(newPrefValue, ",");
            if ( StringUtil.isEmpty(sNewValue) ) {
                sNewValue = context.getString(R.string.cmd_none);
            }
            String sMgs = context.getString(R.string.pref_x_changed_to_y, sSetting, sNewValue);
            Toast.makeText(context, sMgs, Toast.LENGTH_SHORT).show();
            if ( iBoard != null ) {
                iBoard.updateGameAndMatchDurationChronos();
            }
            scoreBoard.castDurationChronos();
            return true;
        }

        @Override public boolean onLongClick(View view) {
            if ( ScoreBoard.timer == null ) {
                return false;
            }
            if ( true ) {
                SBTimerView.this.cancel();
                if ( scoreBoard != null ) {
                    scoreBoard.triggerEvent(ScoreBoard.SBEvent.timerCancelled, ScoreBoard.timer.timerType);
                }
            } else {
                onClick(view);
            }
            return true;
        }

        @Override public void onClick(View view) {
            if ( ScoreBoard.timer == null ) {
                return;
            }
            int secondsLeft = ScoreBoard.timer.getSecondsLeft();
            if ( secondsLeft > 0 ) {
                ScoreBoard.timer.addTimerView(scoreBoard.getDialogTimerView());
            } else {
                SBTimerView.this.cancel();
                if ( scoreBoard != null ) {
                    scoreBoard.triggerEvent(ScoreBoard.SBEvent.timerCancelled, ScoreBoard.timer.timerType);
                }
            }
        }
    }

    @Override public String toString() {
        return super.toString() + " (" + String.valueOf(context) + ")";
    }
}
