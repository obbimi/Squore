package com.doubleyellow.scoreboard.demo;

import android.app.Activity;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import com.doubleyellow.demo.DrawTouch;
import com.doubleyellow.scoreboard.dialog.*;
import com.doubleyellow.scoreboard.main.DialogManager;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.prefs.ColorPrefs;
import com.doubleyellow.scoreboard.view.PlayersButton;
import com.doubleyellow.util.Direction;
import com.doubleyellow.util.ListUtil;
import com.doubleyellow.util.MenuHandler;
import com.doubleyellow.android.view.AutoResizeTextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Class that supports in making a pretty long 'educational movie' about the Squore app.
 *
 * The goal is to be able to run it fully automatic.
 * Downsides
 * - is currently only gives visual feedback about what is 'clicked' for scorebuttons and playernames.
 * - performing swipes are not automated
 */
public class FullDemoThread extends DemoThread {

    public FullDemoThread(ScoreBoard context, Model matchModel) {
        super(context, matchModel);

        //prepareCompleteSimulation();
    }

    private List<ListenerAndView> prepareCompleteSimulation() {
        List<ListenerAndView> lView = new ArrayList<ListenerAndView>();

        ListenerAndView scoreForA = new ListenerAndView(R.id.btn_score1, this.scoreBoard.scoreButtonListener);
        ListenerAndView scoreForB = new ListenerAndView(R.id.btn_score2, this.scoreBoard.scoreButtonListener);
        ListenerAndView doNothing = new ListenerAndView();

        // tmp
/*
        lView.add(new ListenerAndView(DemoMessage.NewMatch_1, 4, 4));
        lView.add(new ListenerAndView(DemoMessage.NewMatch_2, 4, 4));
        lView.add(new ListenerAndView(DemoMessage.NewMatch_3, 4, 4));
        lView.add(new ListenerAndView(DemoMessage.NewMatch_FB_1, 4, 4));
        lView.add(new ListenerAndView(DemoMessage.NewMatch_FB_2, 4, 4));
        lView.add(new ListenerAndView(DemoMessage.NewMatch_FB_3, 4, 4));
        lView.add(new ListenerAndView(DemoMessage.NewMatch_FB_4, 4, 4));
        //lView.add(new ListenerAndView(R.id.dyn_new_match, 4));
        if ( true ) { return lView;}
*/
/*
        lView.add(scoreForA);
        lView.add(new ListenerAndView(R.id.dyn_score_details));
        lView.add(doNothing.setPauseDuration(3));
        lView.add(new ListenerAndView(R.id.close));
        lView.add(doNothing.setPauseDuration(3));
        if ( true ) { return lView;}
*/
/*
        lView.add(new ListenerAndView(R.id.dyn_new_match, 4));
        lView.add(doNothing.setPauseDuration(3));
        lView.add(new ListenerAndView(R.id.close));
        lView.add(doNothing.setPauseDuration(3));
        if ( true ) { return lView;}
*/
/*
        lView.add(new ListenerAndView(R.id.txt_player2, this.scoreBoard.namesButtonListener));
        lView.add(new ListenerAndView(DialogInterface.BUTTON_NEGATIVE));
        lView.add(doNothing.setPauseDuration(5));
        lView.add(new ListenerAndView(R.id.txt_player2, this.scoreBoard.namesButtonListener));
        lView.add(new ListenerAndView(DialogInterface.BUTTON_POSITIVE));
        lView.add(doNothing.setPauseDuration(3));
        lView.add(scoreForA);
        lView.add(doNothing.setPauseDuration(3));
        if ( true ) { return lView;}
*/

        lView.add(new ListenerAndView(DemoMessage.Intro_General) );
        lView.add(new ListenerAndView(DemoMessage.Intro_BigButtons) );
        lView.add(new ListenerAndView(DemoMessage.Intro_ServeSideButtons));
        lView.add(new ListenerAndView(DemoMessage.Intro_PlayerNames));
        lView.add(new ListenerAndView(DemoMessage.Intro_End_See_action    , 4, 4));
        lView.add(new ListenerAndView(DemoMessage.Intro_End_Leave_new     , 4, 4));
        lView.add(new ListenerAndView(DemoMessage.NewMatch_1              , 4, 4));
        lView.add(new ListenerAndView(DemoMessage.NewMatch_2              , 4, 4));
        lView.add(new ListenerAndView(DemoMessage.NewMatch_3              , 4, 4));
        lView.add(new ListenerAndView(DemoMessage.NewMatch_FB_All         , 4, 4));
        lView.add(new ListenerAndView(DemoMessage.NewMatch_FB_Toss        , 4, 4));
        lView.add(new ListenerAndView(DemoMessage.NewMatch_FB_Timer       , 4, 4));
        lView.add(new ListenerAndView(DemoMessage.NewMatch_FB_Announcement, 4, 4));
        lView.add(new ListenerAndView(R.id.float_new_match, 4));
        //lView.add(DemoMessage.NewMatch_2);
        //lView.add(R.id.sb_enter_singles_match);
        //lView.add(DemoMessage.NewMatch_EnterNames);
        lView.add(new ListenerAndView(DemoMessage.First4PointsForA         , 3, 3));
        lView.add(scoreForA.setPauseDuration(3));
        lView.add(scoreForA.setPauseDuration(3));
        lView.add(new ListenerAndView(DemoMessage.Game_ServeSideToggles    , 8, 5));
        lView.add(scoreForA.setPauseDuration(3));
        lView.add(scoreForA.setPauseDuration(3)); // 4-0
        lView.add(new ListenerAndView(DemoMessage.FirstPointsForB          , 3, 3));
        lView.add(new ListenerAndView(DemoMessage.Game_HandoutQuestionMarkB, 6, 4));
        lView.add(scoreForB.setPauseDuration(3));
        lView.add(scoreForB.setPauseDuration(2));
        lView.add(new ListenerAndView(DemoMessage.Game_HandoutQuestionMarkA, 4, 2));
        lView.add(scoreForA);
        lView.add(new ListenerAndView(DemoMessage.Intro_GameHistory        , 4, 2));
        lView.add(scoreForB);
        lView.add(scoreForB);// 5-4
        if ( true ) {
            lView.add(new ListenerAndView(DemoMessage.Game_RecordAppealAndDecision));
            lView.add(new ListenerAndView(R.id.txt_player2, this.scoreBoard.namesButtonListener));
            lView.add(new ListenerAndView(Appeal.BTN_NO_LET));// no let: 6-4
            lView.add(new ListenerAndView(DemoMessage.Game_RecordAppealAndDecision_2));
            lView.add(new ListenerAndView(DemoMessage.Game_RecordAppealAndDecision_3));
        } else {
            lView.add(scoreForA); // 6-4
        }
        lView.add(scoreForB.setPauseDuration(1));
        lView.add(scoreForA.setPauseDuration(1)); // 7-5
        lView.add(scoreForB);
        lView.add(scoreForA); // 8-6
        lView.add(scoreForB);

        lView.add(scoreForB); // 8-8
        lView.add(new ListenerAndView(DemoMessage.Game_UndoScoring, 10, 2));
        lView.add(new ListenerAndView(R.id.dyn_undo_last, 4));
        lView.add(doNothing);
        lView.add(scoreForA.setPauseDuration(3)); // 9-7

        lView.add(scoreForB.setPauseDuration(2));
        lView.add(scoreForB.setPauseDuration(2)); // 9-9
        lView.add(new ListenerAndView(DemoMessage.Game_GameBallButtonColor, 6, 2));
        lView.add(scoreForB.setPauseDuration(4));
        lView.add(new ListenerAndView(DemoMessage.Game_EndGameDialog, 6, 3));
        lView.add(scoreForB);
        lView.add(new ListenerAndView(EndGame.BTN_END_GAME, 5));
        //lView.add(new ListenerAndView(EndGame.BTN_END_GAME_PLUS_TIMER, 5));

        lView.add(new ListenerAndView(DemoMessage.Game_ScoreDetailsButton));
        lView.add(new ListenerAndView(R.id.dyn_score_details));
        lView.add(doNothing.setPauseDuration(3)); //allow time for activity to become active
        lView.add(new ListenerAndView(R.id.close));

        lView.add(scoreForA.setPauseDuration(1));
        lView.add(scoreForA.setPauseDuration(1));
        lView.add(scoreForA.setPauseDuration(1));
        lView.add(scoreForA.setPauseDuration(1));
        lView.add(scoreForA.setPauseDuration(1));
        lView.add(new ListenerAndView(DemoMessage.Game_RecordConductAndDecision));
        lView.add(new ListenerAndView(R.id.txt_player2, this.scoreBoard.namesButtonListener, true).setPauseDuration(5));
        lView.add(new ListenerAndView(Conduct.BTN_CONDUCT_STROKE, 5));
        //lView.add(new ListenerAndView(Conduct.BTN_CONDUCT_WARNING));
        lView.add(doNothing.setPauseDuration(3));

        return lView;
    }

    @Override public void run() {
    //@Override protected Object doInBackground(Object[] objects) {
        Looper.prepare();
        completeSimulate();
        //return null;
    }

    /** This does work e.g. to simulate a click on a score button, but it does not show where the touch even though this is turned on in the debug settings */
    private void test_touchEvent(View viewX) {
        //view = scoreBoard.findViewById(android.R.id.content);

        //Activity view = activity;
        View view = viewX;
        MotionEvent motionEvent = getMotionEvent(MotionEvent.ACTION_DOWN);
        view.dispatchTouchEvent(motionEvent);

        motionEvent = getMotionEvent(MotionEvent.ACTION_UP);
        view.dispatchTouchEvent(motionEvent);
    }

    private MotionEvent getMotionEvent(int iAction) {
        long downTime  = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis() + 100;
        float x = 30.0f;
        float y = 30.0f;
        // List of meta states found here:     developer.android.com/reference/android/view/KeyEvent.html#getMetaState()
        int metaState = 0;
        return MotionEvent.obtain(downTime, eventTime, iAction, x, y, metaState);
    }

    private static DrawTouch       prevDrawTouch   = null;
    private static IBaseAlertDialog prevDialog      = null;
    private static MenuHandler     prevMenuHandler = null;
  //private static ActionBar       prevActionbar   = null;
    private static int             prevActionId    = 0;
    private static int             iMessage        = 0;
    private static boolean         bMoreMessages   = false;
    private void completeSimulate() {

        final Integer scoreButtonBgColor  = ColorPrefs.getTarget2colorMapping(activity).get(ColorPrefs.ColorTarget.scoreButtonBackgroundColor);
        final Integer playerButtonBgColor = ColorPrefs.getTarget2colorMapping(activity).get(ColorPrefs.ColorTarget.playerButtonBackgroundColor);
        final List<ListenerAndView> lView = prepareCompleteSimulation();

/* can not be called here: will touch views not created by this thread
        if ( activity instanceof ScoreBoard ) {
            ((MenuHandler) activity).handleMenuItem(R.id.sb_clear_score);
        }
*/

        int iStep = -1;
        while (iStep < ListUtil.size(lView)-1 && (bStopLoop == false)) {
            //pause(1000);
/*
            if ( System.currentTimeMillis() - lLastMotionEvent < 10000 ) {
                Log.d(TAG, "Waiting before next simulated click");
                continue;
            }
*/
            if ( bMoreMessages ) {
                iMessage++;
            } else {
                iStep++;
                iMessage = 0;
            }
            if (iStep < 0 || iStep >= ListUtil.size(lView) ) {
                return;
            }
            final ListenerAndView lv = lView.get(iStep);
            final View viewById = scoreBoard.findViewById(lv.viewId);

            Handler handler = new Handler(activity.getMainLooper());
            handler.post(new Runnable() {
                public void run() {
                    // clear any 'drawtouch' of a previous view element
                    if ( prevDrawTouch != null ) {
                        int iRestoreColor = Color.WHITE;
                        if (prevDrawTouch instanceof PlayersButton) {
                            iRestoreColor = playerButtonBgColor;
                        }
                        if (prevDrawTouch instanceof AutoResizeTextView) {
                            iRestoreColor = scoreButtonBgColor;
                        }
                        if (prevDrawTouch instanceof Activity) {
                            iRestoreColor = Color.TRANSPARENT;
                        }
                        if ( prevDrawTouch instanceof View ) {
                            View view = (View) prevDrawTouch;
                            if (view.getId() != lv.viewId) {
                                prevDrawTouch.drawTouch(null, prevActionId, iRestoreColor);
                                prevDrawTouch = null;
                            }
                        } else {
                            prevDrawTouch.drawTouch(null, prevActionId, iRestoreColor);
                            prevDrawTouch = null;
                        }
                    }

                    // close a dialog by simulating a button click
                    if ( prevDialog != null ){
                        prevDialog.handleButtonClick(prevActionId);
                        prevDialog.dismiss();
                        prevDialog = null;
                    }

                    if ( prevMenuHandler != null ) {
                        // e.g. used to start child activity to start a new match
                        prevMenuHandler.handleMenuItem(prevActionId);
                        prevMenuHandler = null;
                    }
                    if ( lv.viewId != View.NO_ID ) {
                        if ( viewById instanceof DrawTouch ) {
                            String sViewName = activity.getResources().getResourceName(lv.viewId);
                            DrawTouch artv = (DrawTouch) viewById;
                            artv.drawTouch(sViewName.matches(".*1")?Direction.NW:Direction.NE, lv.viewId, Color.BLUE);

                            prevDrawTouch = artv;
/*
                            try {
                                String sViewName = activity.getResources().getResourceName(lv.viewId);
                                String sOtherView = sViewName.replace("2", "3").replace("1", "2").replace("3", "1");
                                int iOtherId = activity.getResources().getIdentifier(sOtherView, null, null);
                                artv = (DrawTouch) scoreBoard.findViewById(iOtherId);
                                artv.drawTouch(Direction.SE, iRestoreColor);
                            } catch (Resources.NotFoundException e) {
                                e.printStackTrace();
                            }
*/
                        }
                        if ( viewById != null ) {
                            if ( lv.ocl != null ) {
                                //lv.ocl.onClick(viewById);
                                test_touchEvent(viewById);
                            }
                            if ( lv.olcl != null ) {
                                lv.olcl.onLongClick(viewById);
                            }
                        }
/*
                        if ( viewById instanceof AutoResizeTextView ) {
                            synchronized (this) {
                                try {
                                    wait(300);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                            AutoResizeTextView artv = (AutoResizeTextView) viewById;
                            artv.drawTouch(null);
                        }
*/
                    }
                    if (lv.demoMessage != null) {
                        if (lv.demoMessage.appliesTo(matchModel, activity) == false) {
                            Log.w(TAG, "Not the right time for showing " + lv.demoMessage);
                            //return;
                        }
                        cancelDemoMessage();
                        int[] iResId = lv.demoMessage.focusOn(matchModel);
                        String[] messages = lv.demoMessage.getMessages(matchModel);
                        bMoreMessages = ( iMessage < messages.length -1 );
                        showInfo(messages[iMessage], iResId, lv.demoMessageDuration);
                    }
                    if (lv.actionId != null ) {
                        boolean bHandled = false;
                        //prevActionbar = activity.getActionBar();
                        if ( activity instanceof MenuHandler ) {
                            if ( activity instanceof DrawTouch ) {
                                // only ScoreBoard for now
                                prevDrawTouch = (DrawTouch) activity;
                                prevActionId = lv.actionId;
                                prevDrawTouch.drawTouch(null, lv.actionId, Color.BLUE);
                                prevMenuHandler = (MenuHandler) activity;
                            } else {
                                if (((MenuHandler) activity).handleMenuItem(lv.actionId)) {
                                    bHandled = true;
                                }
                            }
                        }
                        if ( bHandled == false ) {
                            if ( scoreBoard.isDialogShowing() ) {
                                prevDialog = DialogManager.getInstance().baseDialog;
                                if ( prevDialog instanceof BaseAlertDialog ) {
                                    ((BaseAlertDialog)prevDialog).drawTouch(lv.actionId, Color.BLUE);
                                }
                                prevActionId = lv.actionId;
                                //scoreBoard.baseDialog.handleButtonClick(lv.actionId);
                                //scoreBoard.baseDialog.dismiss();
                            }
                        }
                    }
/*
                    //pause half a second than clear the 'touch'
                    pause(500);
                    if ( prevDrawTouch != null ) {
                        prevDrawTouch.drawTouch(Direction.SE, iRestoreColor);
                        prevDrawTouch = null;
                    }
*/
                }
            });
            pause(lv.pauseDurationAfter * 1000);

/*
            while ( scoreBoard.isDialogShowing() ) {
                pause(1000);
            }
*/
        }

        scoreBoard.setModus(null, ScoreBoard.Mode.Normal);
    }
}
