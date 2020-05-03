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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.util.Log;
import android.util.TypedValue;
import android.view.*;
import android.widget.Chronometer;
import android.widget.TextView;

import com.doubleyellow.android.view.AutoResizeTextView;
import com.doubleyellow.android.view.ViewUtil;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.dialog.MyDialogBuilder;
import com.doubleyellow.scoreboard.history.MatchGameScoresView;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.model.Player;
import com.doubleyellow.scoreboard.prefs.ColorPrefs;
import com.doubleyellow.scoreboard.prefs.PreferenceKeys;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.scoreboard.vico.IBoard;
import com.doubleyellow.util.StringUtil;
import com.doubleyellow.android.view.CircularProgressBar;

import java.util.EnumSet;
import java.util.Map;

/**
 * TimerView that is an actual dialog (on top of the main scoreboard scoreBoard) with a circular progressbar.
 *
 * An alternative (non-modal) TimerView is defined in the main scoreBoard 'ScoreBoard' itself.
 */
public class DialogTimerView implements TimerView
{
    private AlertDialog alertDialog    = null;
    private ScoreBoard  scoreBoard     = null;

    public DialogTimerView(ScoreBoard scoreBoard) {
        this.scoreBoard     = scoreBoard;
    }

    @Override public void setTitle(String s) {
        this.sTitle1 = s;
        setDBTitle();
    }

    private void setDBTitle() {
        if ( alertDialog == null ) { return; }
        String dbTitle = getDBTitle();
        try {
            alertDialog.setTitle(dbTitle);
        } catch (Exception e) {
            e.printStackTrace(); // java.lang.IllegalArgumentException on PlayStore 20200502
        }
    }

    private String getDBTitle() {
        return this.sTitle1 + " " + this.sTime + (StringUtil.isNotEmpty(this.sPaused) ? " (" + this.sPaused + ")" : "");
    }

    @Override public void setTime(int iStartedCountDownAtSecs, int iSecsLeft, int iReminderAtSecs) {
        if ( cpb != null ) {
            float progress = (float) (/*iStartedCountDownAtSecs -*/ iSecsLeft) / iStartedCountDownAtSecs;
            cpb.setProgress(progress);

            if ( cpb.isMarkerEnabled() ) {
                float markerProgress = (float) (/*iStartedCountDownAtSecs -*/ iReminderAtSecs) / iStartedCountDownAtSecs;
                cpb.setMarkerProgress(markerProgress);
            }
            //Log.d("SB" + this.getClass().getSimpleName(), "updated cpb");
        } else {
            Log.d("SB" + this.getClass().getSimpleName(), "Can not update cpb yet");
        }
    }
    @Override public void setTime(String s) {
        this.sTime = s;
        setDBTitle();
    }

    @Override public void setWarnMessage(String s) {
        this.sMsg1 = s;
        if ( alertDialog == null ) { return; }
        alertDialog.setMessage(this.sMsg1 /* + (StringUtil.isNotEmpty(this.sMsg2)?" (" + this.sMsg2 + ")":"")*/);
        if ( txtTimerMessage != null ) {
            //txtTimerMessage.setVisibility(View.VISIBLE);
            txtTimerMessage.setText(sMsg1);
        }
    }

    @Override public void setPausedMessage(String s) {
        this.sPaused = s;
        setDBTitle();
    }

    @Override public void timeIsUp() {
        if ( alertDialog == null ) { return; }

        if ( txtTimerMessage != null ) {
            txtTimerMessage.setText(R.string.oa_time);
        }
        // hide the 'Hide' and 'Pause' buttons... they are no longer applicable
        for(int iButton: new int[] {BTN_HIDE,BTN_PAUSE}) {
            TextView btn = alertDialog.getButton(iButton);
            if ( btn != null ) {
                btn.setVisibility(View.GONE);
            }
        }
        if ( PreferenceValues.showTimeIsAlreadyUpFor_Chrono(scoreBoard) ) {
            if ( (chronometer != null) && (ScoreBoard.timer != null) && Type.UntillStartOfNextGame.equals(Timer.timerType) ) {
                chronometer.setVisibility(View.VISIBLE);
                chronometer.setBase(ScoreBoard.timer.getToLateBase());
                chronometer.start();
            }
        }
    }

    @Override public void cancel() {
        try {
            if ( chronometer != null ) {
                chronometer.stop();
            }
            alertDialog.cancel();
            alertDialog.dismiss();
        } catch (Exception e) {
            // improve. cancel() method throws exception if dialog 'disappeared' when orientation changed
        }
        IBoard.setBlockToasts(false);
    }

    private static final int    m_iAmount  = 10;   // TODO: preference ?
    private TextView            bAdd;            // to use a 'dull' layout like the rest of the dialog buttons
    private TextView            bRemove;         // to use a 'dull' layout like the rest of the dialog buttons
    private CircularProgressBar cpb      = null;
    /** Used for '15 seconds/halftime' text */
    private TextView            txtTimerMessage = null;
    private Chronometer         chronometer     = null;

    @Override public void show()
    {
        IBoard.setBlockToasts(true);

        final Model matchModel = ScoreBoard.matchModel;
        Map<ColorPrefs.ColorTarget, Integer> colors = scoreBoard.getColors();

      //int iShowTimersCnt = PreferenceValues.getRunCount(scoreBoard, PreferenceKeys.useTimersFeature);
      //boolean bShowWithNoMoreCheckBox = bAutoTriggered && PreferenceValues.showTimersAutomatically(scoreBoard) && (iShowTimersCnt < 5);
        LayoutInflater myLayout = LayoutInflater.from(scoreBoard);
        ViewGroup ll = (ViewGroup) myLayout.inflate(R.layout.timer, null);

        txtTimerMessage = (TextView   ) ll.findViewById(R.id.timerMessage);
        chronometer     = (Chronometer) ll.findViewById(R.id.to_late_timer);
        if ( chronometer != null ) {
            chronometer.setVisibility(View.GONE);
        }

        // because the little chronometer is not an AutoresizeTextView we 'emulate' this by installing a onresizelistener on its 'parent': the actual timer
        if ( txtTimerMessage instanceof AutoResizeTextView && chronometer != null ) {
            AutoResizeTextView arTimer = (AutoResizeTextView) txtTimerMessage;
            arTimer.addOnResizeListener(new AutoResizeTextView.OnTextResizeListener() {
                @Override public void onTextResize(AutoResizeTextView textView, float oldSize, float newSizePx, float requiredWidth, float requiredHeight) {
                    float fRatio = 0.35f;
                    //if ( ViewUtil.isPortraitOrientation(context) ) {
                    //    fRatio = 0.6f;
                    //}
                    float size = newSizePx * fRatio;
                    //float iOOTBsize = btnToLate.getTextSize();
                    chronometer.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
                }
            });
        }

        MatchGameScoresView matchGameScores = (MatchGameScoresView) ll.findViewById(R.id.timer_gamescores);
        final boolean bShowPastGameAndDuration= PreferenceValues.showLastGameInfoInTimer(scoreBoard);
        final boolean bShowPauseButtonOnTimer = PreferenceValues.showPauseButtonOnTimer (scoreBoard);
        final boolean bShowHideButtonOnTimer  = PreferenceValues.showHideButtonOnTimer  (scoreBoard);
        final boolean bShowCirclePB           = PreferenceValues.showCircularCountdownInTimer(scoreBoard);
/*
        if ( txtTimerMessage != null ) {
            txtTimerMessage.setVisibility(bShowPastGameAndDuration?View.VISIBLE:View.GONE);
        }
        if ( bShowPastGameAndDuration ) {
            int iJustEndedGame = matchModel.getEndScoreOfPreviousGames().size();
            List<GameTiming> times = matchModel.getTimes();
            if ( (iJustEndedGame > 0) && (iJustEndedGame < ListUtil.size(times)) ) {
                GameTiming last = times.get(iJustEndedGame - 1);
                int durationMM = last.getDurationMM();
                int iResTimeUnit = R.string.minutes;
                if (durationMM < 0) {
                    durationMM = Math.abs(durationMM);
                    iResTimeUnit = R.string.seconds;
                }
                Player winnerOfLastGame = matchModel.getServer();
                String sGameEndScore = "";
                if ( winnerOfLastGame != null ) {
                    Map<Player, Integer> lastEnd = ListUtil.getLast(matchModel.getEndScoreOfGames());
                    sGameEndScore = lastEnd.get(winnerOfLastGame) + "-" + lastEnd.get(winnerOfLastGame.getOther());
                }

                int iResId = ViewUtil.isLandscapeOrientation(scoreBoard)? R.string.last_game__score_x_induration_y_z__2lines : R.string.last_game__score_x_induration_y_z;
                sMsg1 = scoreBoard.getString(iResId, sGameEndScore, durationMM, scoreBoard.getString(iResTimeUnit));

                if (txtTimerMessage != null) {
                    txtTimerMessage.setText(sMsg1);
                }
            } else {
                if ( txtTimerMessage != null ) {
                    //txtTimerMessage.setVisibility(View.GONE); // do not hide it... we need it for the 'warn' message
                    ViewUtil.emptyField(txtTimerMessage);
                }
            }
        }
*/
        if ( txtTimerMessage != null ) {
            //txtTimerMessage.setVisibility(View.GONE); // do not hide it... we need it for the 'warn' message
            ViewUtil.emptyField(txtTimerMessage);
        }

        cpb = (CircularProgressBar) ll.findViewById(R.id.circular_progress);
        if ( cpb != null ) { cpb.setVisibility(bShowCirclePB?View.VISIBLE:View.GONE); }
        if ( bShowCirclePB ) {
            Integer bgColor     = colors.get(ColorPrefs.ColorTarget.backgroundColor);
            Integer pbColor     = colors.get(ColorPrefs.ColorTarget.playerButtonBackgroundColor);
            Integer pbBackColor = colors.get(ColorPrefs.ColorTarget.playerButtonTextColor);
            if ( pbBackColor.equals(bgColor) ) {
                pbBackColor = colors.get(ColorPrefs.ColorTarget.scoreButtonBackgroundColor);
            }

            cpb.setProgressColor          (pbColor);
            cpb.setProgressBackgroundColor(pbBackColor);
            cpb.setBackgroundColor        (bgColor);
            cpb.setMarkerBackgroundColor  (colors.get(ColorPrefs.ColorTarget.serveButtonBackgroundColor));
            cpb.setMarkerEnabled(true);
        }

        boolean bShowUseAudioCheckboxInTimer = PreferenceValues.showUseAudioCheckboxInTimer(scoreBoard);
        if ( bShowUseAudioCheckboxInTimer == false ) {
            ViewUtil.hideViews(ll, R.id.use_audio);
        }

        boolean bShowWithPlusMin10Secs = PreferenceValues.showAdjustTimeButtonsInTimer(scoreBoard);
        View llParent = ll.findViewById(R.id.ll_timer_add_remove_buttons);
        if ( llParent != null ) llParent.setVisibility(bShowWithPlusMin10Secs?View.VISIBLE:View.GONE);

        if ( bShowWithPlusMin10Secs ) {
            bAdd    = (TextView) ll.findViewById(R.id.add_secs   );
            bRemove = (TextView) ll.findViewById(R.id.remove_secs);

            bAdd   .setOnClickListener(onAddRemoveClickListener);
            bRemove.setOnClickListener(onAddRemoveClickListener);
        }

        AlertDialog.Builder db = new MyDialogBuilder(scoreBoard);
        int iResIDCancelCaption = R.string.cmd_cancel;
        if ( Type.UntillStartOfNextGame.equals(Timer.timerType) ) {
            iResIDCancelCaption = R.string.Start_game;
        } else if (EnumSet.of(Type.TowelingDown, Type.Timeout).contains(Timer.timerType) ) {
            iResIDCancelCaption = R.string.Resume_game;
        }
                                         db.setPositiveButton(PreferenceValues.getGameOrSetString(scoreBoard, iResIDCancelCaption), onCancelListener);
        if ( bShowPauseButtonOnTimer ) { db.setNeutralButton (R.string.cmd_pause , null); }
        if ( bShowHideButtonOnTimer )  { db.setNegativeButton(R.string.cmd_hide  , onHideListener); }

        db.setTitle(getDBTitle());

        ColorPrefs.setColor(ll);

/*
        // attempt (2) to make it fill the width of the screen in landscape
        Point displayPoint = ViewUtil.getDisplayPoint(scoreBoard);
        ll.setMinimumWidth((int)(displayPoint.x * 0.9f));
*/

        db.setView(ll);

        if ( matchGameScores != null ) {
            if ( bShowPastGameAndDuration ) {
                matchGameScores.setVisibility(View.VISIBLE);
                matchGameScores.update(matchModel, Player.A);
                matchGameScores.setProperties(colors.get(ColorPrefs.ColorTarget.scoreButtonTextColor       )
                                            , colors.get(ColorPrefs.ColorTarget.scoreButtonBackgroundColor )
                                            , colors.get(ColorPrefs.ColorTarget.serveButtonBackgroundColor )
                                            , colors.get(ColorPrefs.ColorTarget.playerButtonBackgroundColor)
                                            );
            } else {
                matchGameScores.setVisibility(View.GONE);
            }
        }

        alertDialog = db.create();
        alertDialog.setOnShowListener(onShowTimerListener); // in order to install listener for the 'Pause' button
        alertDialog.setOnKeyListener(new Dialog.OnKeyListener() {
            @Override public boolean onKey(DialogInterface arg0, int keyCode, KeyEvent event) {
                int action  = event.getAction();
                if (keyCode == KeyEvent.KEYCODE_BACK /* = 4 */ && action == KeyEvent.ACTION_UP) {
                    if ( bShowHideButtonOnTimer ) {
                        onHideListener.onClick(alertDialog, BTN_HIDE); // will set the preference to 'Inline' timerview
                        //cancel(); // would actually stop the timer, here we only want to hide the dialog
                    } else {
                        onCancelListener.onClick(alertDialog, BTN_CANCEL);
                    }
                    return true;
                }
                return false;
            }
        });
        //alertDialog.getWindow().setBackgroundDrawable(null); // removes the default border of a dialog
        //alertDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent); // removes the default border of a dialog

        // attempt (2) to make it fill the width of the screen in landscape
        // alertDialog.getWindow().getAttributes().width = WindowManager.LayoutParams.MATCH_PARENT; // does not work on 5.1

        try {
            alertDialog.show();
            PreferenceValues.setEnum(PreferenceKeys.timerViewType, scoreBoard, ViewType.Popup);
        } catch (Exception e) {
            // this may fail if activity has been in the background, seen during casting (android.view.WindowManager$BadTokenException: Unable to add window -- token android.os.BinderProxy@860c8b3 is not valid; is your activity running?)
            e.printStackTrace();
        }
    }

    @Override public boolean isShowing() {
        if ( alertDialog == null ) { return false; }
        return alertDialog.isShowing();
    }

    private String sMsg1   = "";
    private String sTitle1 = "";
    private String sTime   = "";
    private String sPaused = "";

    private View.OnClickListener onAddRemoveClickListener = new View.OnClickListener() {
        @Override public void onClick(View view) {
            if ( Timer.countDownTimer == null ) {
                return;
            }
            Object tag = view.getTag();
            if ( tag == null && view instanceof TextView) {
                tag = ((TextView)view).getText();
            }
            int iAddOrSubtract = tag.toString().startsWith("-") ? -1 : 1;
            int iAmount = m_iAmount;
            if ( (iAddOrSubtract < 0) && ( iAmount >= Timer.countDownTimer.secsLeft - 1 ) ) {
                iAmount = Timer.countDownTimer.secsLeft / 2;
            }
            int iSecsInFuture = Timer.countDownTimer.secsLeft + iAddOrSubtract * iAmount;
            scoreBoard.restartTimerWithSecondsLeft(iSecsInFuture);
            // do not disable the button, simply substract less
            bRemove.setEnabled(true || (iSecsInFuture > iAmount));
        }
    };

    public static void restartTimerWithSecondsLeft(int iSecsInFuture) {
        if ( Timer.countDownTimer == null ) {
            return;
        }
        if ( iSecsInFuture > 0 ) {
            // replace the countdown time with a new one
            if ( Timer.countDownTimer != null ) {
                Timer.countDownTimer.stop();
            }
            Timer.countDownTimer = Timer.countDownTimer.restart(iSecsInFuture);
        }
    }

    private final DialogInterface.OnClickListener onHideListener = new DialogInterface.OnClickListener() {
        @Override public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case DialogTimerView.BTN_HIDE:
                    // 'Hide' is pressed: change viewType
                    PreferenceValues.setEnum(PreferenceKeys.timerViewType, scoreBoard, ViewType.Inline);
                    Timer.removeTimerView(false, DialogTimerView.this);
                    dialog.cancel();
                    break;
            }
        }
    };

    private final DialogInterface.OnClickListener onCancelListener = new DialogInterface.OnClickListener() {
        @Override public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case BTN_CANCEL:
                    // cancel was pressed
                    if (Timer.countDownTimer != null ) {
                        Timer.countDownTimer.stop();
                    }
                    cancel();
                    //scoreBoard.triggerEvent(ScoreBoard.SBEvent.timerCancelled, Timer.countDownTimer.type);
                    scoreBoard.cancelTimer();
                    Timer.removeTimerView(false, DialogTimerView.this);
                    break;
            }
        }
    };

    private final View.OnClickListener onPauseClickListener = new View.OnClickListener() {
        @Override public void onClick(View v) {
            TextView button = (TextView) v;
            String sPause = scoreBoard.getString(R.string.cmd_pause);
            String sCaptionOld = button.getText().toString();
            if ( sCaptionOld.equals(sPause) ) {
                if (Timer.countDownTimer != null ) {
                    Timer.countDownTimer.stop();
                }
                button.setText(R.string.cmd_resume);
                setPausedMessage(scoreBoard.getString(R.string.msg_paused));
            } else {
                // resume
                if (Timer.countDownTimer != null ) {
                    Timer.countDownTimer = Timer.countDownTimer.restart(Timer.countDownTimer.secsLeft);
                }
                //show(); // TODO: is show required?
                button.setText(sPause);
                setPausedMessage("");
            }
        }
    };

    public static final int BTN_PAUSE  = DialogInterface.BUTTON_NEUTRAL;
    public static final int BTN_CANCEL = DialogInterface.BUTTON_POSITIVE;
    public static final int BTN_HIDE   = DialogInterface.BUTTON_NEGATIVE;

    private boolean pauseResumeInstalled = false;
    private DialogInterface.OnShowListener onShowTimerListener = new DialogInterface.OnShowListener() {
        /**
         * We register an onPauseClickListener for the 'neutral' button. There is already a listener, but that one closes the dialog no matter what.
         * Our listener ensures we can pause and resume the timer without closing it.
         */
        @Override public void onShow(DialogInterface dialogInterface) {
            if ( pauseResumeInstalled ) { return; }

            final TextView btnPause = alertDialog.getButton(BTN_PAUSE);
            if ( btnPause == null ) { return; }

            btnPause.setOnClickListener(onPauseClickListener);
            pauseResumeInstalled = true;
        }
    };
}

