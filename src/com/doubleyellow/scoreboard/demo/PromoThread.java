package com.doubleyellow.scoreboard.demo;

import android.app.Activity;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.percentlayout.widget.PercentRelativeLayout;

import com.doubleyellow.android.view.ViewUtil;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.util.Direction;
import com.doubleyellow.util.ListUtil;
import com.doubleyellow.view.DrawArrows;

/**
 * Supports in creating a relatively short promo movie.
 * Shows just several arrows pointing at relevant gui elements while the user performs a short squash scoring sequence.
 **/
public class PromoThread extends Thread
{
    protected static final String TAG = "SB." + PromoThread.class.getSimpleName();

    /**
     * Start settings with
     * - showTimers = Suggest
     * - show announcement = Suggest
     * - auto game/match = Suggest
     * - no feed url
     * - no tips
     * - no post
     *
     * Start match from my list
     * - tiebreak format
     * - change to 'best of 3 games to 9'
     * start timer
     * toss
     * show announcement
     * follow 'waiting for' messages in android log console
     */
    private enum PromoArrow
    {
        BigButtons { // 2 x right, 2 x left : 0-0, 2-0 untill 2-2
            @Override public boolean appliesTo(Model m, Activity a) { return m.getMaxScore() <= 2 && m.getMaxScore() != 0; }
            @Override public int[] focusOn(Model m)                 { return new int[] { R.id.btn_score1, R.id.btn_score2 }; }
            @Override public Direction[] arrowDirection()           { return new Direction[] { Direction.W, Direction.E }; }
        },
        SideButtons1 { // 3 x right : 5-2
            @Override public boolean appliesTo(Model m, Activity a) { return m.getMaxScore()>=3 && m.getMaxScore() <= 5 && m.getDiffScore() < 3; }
            @Override public int[] focusOn(Model m)                 { return new int[] { R.id.btn_side1 }; }
            @Override public Direction[] arrowDirection()           { return new Direction[] { Direction.N }; }
        },
        SideButtons2 { // 3 x left : 5-5
            @Override public boolean appliesTo(Model m, Activity a) { return m.getMinScore() <= 4 ; }
            @Override public int[] focusOn(Model m)                 { return new int[] { R.id.btn_side2 }; }
            @Override public Direction[] arrowDirection()           { return new Direction[] { Direction.N }; }
        },
        SheetGameInProgress { // 1x links, 2 x rechts : 5-6, 7-6
            @Override public boolean appliesTo(Model m, Activity a) { return m.getMinScore() <= 6 && m.getMaxScore() <=7; }
            @Override public int[] focusOn(Model m)                 { return new int[] { R.id.scorehistorytable }; }
            @Override public Direction[] arrowDirection()           { return new Direction[] { Direction.S }; }
        },                   // 1 x rechts: 9-6
        GameBall { // 1 x rechts : 8-6
            @Override public boolean appliesTo(Model m, Activity a) { return ListUtil.isNotEmpty(m.isPossibleGameBallFor()); }
            @Override public int[] focusOn(Model m)                 { return new int[] { R.id.btn_score1 }; }
            @Override public Direction[] arrowDirection()           { return new Direction[] { Direction.W }; }
        },                   // 1 x rechts: 9-6
        FinishedGames {      // show single game graph
            @Override public boolean appliesTo(Model m, Activity a) { return m.getMinScore() == 0; }
            @Override public int[] focusOn(Model m)                 { return new int[] { R.id.gamescores }; }
            @Override public Direction[] arrowDirection()           { return new Direction[] { Direction.N }; }
        },                           ;
        public abstract boolean     appliesTo(Model model, Activity activity);
        public abstract int[]       focusOn(Model m);
        public abstract Direction[] arrowDirection();
    }
    /*
     * start timer
     * show announcement
     * do 2-nd game with a few strokes and lets
     * do 3-d game having a tiebreak (announcement + matchball, gameball alternation)
     * show score details/match history
     *
     * share match (ensure facebook app is already open on my personal page)
     *
     * show settings
     * change color
     *
     * Show world championship match?
     */

    @Override public void run() {
        try {
            Looper.prepare(); // TODO: Only one Looper may be created per thread
        } catch (Throwable e) {
            e.printStackTrace();
        }
        applicableArrow();
    }

    private void applicableArrow() {
        for(PromoArrow message: PromoArrow.values()) {

            while ( scoreBoard.isDialogShowing() ) {
                pause(200);
            }
            // loop over them in sequence
            Log.w("#", "##########################################################################################################");
            Log.w("#", "# Waiting to be ready for " + message + ". ");
            Log.w("#", "##########################################################################################################");
            while ( readyForMessage(message) == false ) {
                pause(200);
                if ( bStopLoop ) {
                    break;
                }
            }
            if ( bStopLoop ) {
                break;
            }

            drawArrow(message.focusOn(matchModel), message.arrowDirection());

            // keep showing the arrow
            Log.w("#", "----------------------------------------------------------------------------------------------------------");
            Log.w("#", "# Waiting to stop " + message + ". ");
            Log.w("#", "----------------------------------------------------------------------------------------------------------");
            while ( readyForMessage(message) ) {
                pause(200);
                if ( bStopLoop ) {
                    break;
                }
            }
            Log.w("#", "# Waiting stopped " + message + ". ");
            hideArrow();
        }
        Log.w("#", "##########################################################################################################");
        Log.w("#", "# Done");
        Log.w("#", "##########################################################################################################");
    }

    private boolean readyForMessage(PromoArrow message) {
        boolean bModelOk = message.appliesTo(matchModel, activity);

        boolean bFocusOnOk = true;
        int[] iResId = message.focusOn(matchModel);
        if ( (iResId != null) && (iResId[0] != 0)) {
            View guiItem = activity.findViewById(Math.abs(iResId[0]));
            if ( guiItem != null ) { // Menu Item is returned as View as well: com.android.internal.view.menu.ActionMenuItemView
                bFocusOnOk = (guiItem.getVisibility()== View.VISIBLE);
            } else if (menu != null ) {
                // maybe it is a menu item
                MenuItem menuItem = menu.findItem(iResId[0]);
                if ( menuItem != null ) {
                    bFocusOnOk = menuItem.isVisible();
                } else {
                    bFocusOnOk = false;
                }
            } else {
                bFocusOnOk = false;
            }
            if ( bFocusOnOk == false ) {
                //Log.w("#", message.toString() + ": Not yet visible " + scoreBoard.getResources().getResourceEntryName(iResId[0]) + " for " + activity.getTitle());
            }
        }

        return bModelOk && bFocusOnOk;
    }

    protected ScoreBoard scoreBoard = null;
    protected Model      matchModel = null;
    protected Activity   activity   = null;
    protected Menu       menu       = null;
    private int bgColor = Color.WHITE;

    public PromoThread(ScoreBoard scoreBoard, Model model) {
        this.scoreBoard = scoreBoard;
        this.matchModel = model;
        setActivity(scoreBoard);
        bgColor = PreferenceValues.getDemoBackgroundColor(scoreBoard);
    }

    public void setActivity(Activity activity) {
        hideArrow();
        this.activity = activity;
    }
    public void setModel(Model model) {
        this.matchModel = model;
    }

    protected boolean bStopLoop = false;
    public void stopLoop() {
        bStopLoop = true;
    }
// ---------------------------------------
    // HELPER METHODS
    // ---------------------------------------

    protected void pause(long lMs) {
        try {
            synchronized (this) {
                //Log.d(TAG, "Waiting...");
                wait(lMs);
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "?? " + e); // normally only when thread is deliberatly stopped/interrupted
        }
        //Log.d(TAG, "Resumed");
    }

    private void drawArrow(final int[] iRelatedResId, final Direction[] directions) {
        if ( this.activity == null) { return; }

        Handler handler = new Handler(activity.getMainLooper());
        handler.post( new Runnable(){
            public void run(){
                View rootView = activity.findViewById(android.R.id.content);
                PercentRelativeLayout sbRelativeLayout = ViewUtil.getFirstView(rootView, PercentRelativeLayout .class);
                if ( sbRelativeLayout instanceof DrawArrows ) {
                    ((DrawArrows)sbRelativeLayout).drawArrow(iRelatedResId, directions, bgColor);
                }
            }
        });
    }
    private void hideArrow() {
        if ( this.activity == null) { return; }

        Handler handler = new Handler(activity.getMainLooper());
        handler.post( new Runnable(){
            public void run(){
                View rootView = activity.findViewById(android.R.id.content);
                PercentRelativeLayout sbRelativeLayout = ViewUtil.getFirstView(rootView, PercentRelativeLayout.class);
                if ( sbRelativeLayout instanceof DrawArrows ) {
                    ((DrawArrows)sbRelativeLayout).hideArrows();
                }
            }
        });
    }

}
