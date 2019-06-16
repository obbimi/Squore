package com.doubleyellow.scoreboard.cast.framework;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.view.Menu;

import com.doubleyellow.android.util.ColorUtil;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.model.Player;
import com.doubleyellow.scoreboard.prefs.ColorPrefs;
import com.doubleyellow.scoreboard.timer.TimerView;

import com.doubleyellow.scoreboard.vico.IBoard;
import com.doubleyellow.util.MapUtil;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.Session;
import com.google.android.gms.cast.framework.SessionManagerListener;

import org.json.JSONObject;

import java.util.Map;

public class CastHelper implements com.doubleyellow.scoreboard.cast.ICastHelper
{
    private static final String TAG = "SB." + CastHelper.class.getSimpleName();

    private ScoreBoard  m_activity = null;
    private CastContext castContext = null; /* has nothing to do with android.content.Context */
    private CastSession castSession = null;
    private String      sPackageName = null; /* serves as Namespace */

    @Override public void initCasting(ScoreBoard activity) {
        m_activity = activity;
        if ( castContext == null ) {
            castContext = CastContext.getSharedInstance(activity); // requires com.google.android.gms.cast.framework.OPTIONS_PROVIDER_CLASS_NAME to be specified in Manifest.xml
        }
        sPackageName = m_activity.getPackageName();
    }

    @Override public void initCastMenu(Activity activity, Menu menu, int iResIdMenuItem) {
        CastButtonFactory.setUpMediaRouteButton(activity, menu, iResIdMenuItem); // TODO: set to visible always in new cast framework?
    }

    @Override public void onActivityStart_Cast() {

    }
    @Override public void onActivityStop_Cast() {
        //cleanup();
    }
    @Override public void onActivityPause_Cast() {
        castContext.getSessionManager()
                   .removeSessionManagerListener(sessionManagerListener, CastSession.class);

    }
    @Override public void onActivityResume_Cast() {
        castContext.getSessionManager()
                   .addSessionManagerListener(sessionManagerListener, CastSession.class);

/*
        if (castSession == null) {
            // Get the current session if there is one
            castSession = castContext.getSessionManager().getCurrentCastSession();

            updateViewWithColorAndScore(m_activity, m_matchModel);
        }
*/
    }

    @Override public boolean isCasting() {
        return castSession != null;
    }

    private Model m_matchModel = null;
    @Override public void setModelForCast(Model matchModel) {
        if ( (matchModel != m_matchModel) || matchModel.isDirty() ) {
            m_matchModel = matchModel;
        }
        updateViewWithColorAndScore(m_activity, m_matchModel);
    }

    public void sendMessage(Integer iBoardResId, Object oValue) {
        sendMessage(iBoardResId, oValue, "text");
    }
    // sProperty = background-color
    public void sendMessage(Integer iBoardResId, Object oValue, String sProperty) {
        if ( isCasting() == false ) { return; }
        if ( iBoardResId == null ) {
            return;
        }
        Resources resources = m_activity.getResources();
        try {
            String sResName = resources.getResourceName(iBoardResId);
                   sResName = sResName.replaceFirst(".*/", "");

            sendMessage(sResName, oValue, sProperty);
        } catch (Resources.NotFoundException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(String sResName, Object oValue, String sProperty) {
        if ( isCasting() == false ) { return; }
        if ( sProperty.contains("color") && oValue instanceof Integer ) {
            oValue = ColorUtil.getRGBString((Integer) oValue);
        }
        Map map = MapUtil.getMap("id", sResName, "property", sProperty, "value", oValue);
        JSONObject jsonObject = new JSONObject(map);
        String sMsg = jsonObject.toString();
        Log.d(TAG, "sendMessage: " + sMsg);
        castSession.sendMessage("urn:x-cast:" + sPackageName, sMsg);
    }
    public void sendMessage(String sFunction) {
        if ( isCasting() == false ) { return; }
        Map map = MapUtil.getMap("func", sFunction);
        JSONObject jsonObject = new JSONObject(map);
        String sMsg = jsonObject.toString();
        Log.d(TAG, "sendMessage: " + sMsg);
        castSession.sendMessage("urn:x-cast:" + sPackageName, sMsg);
    }


    @Override public TimerView getTimerView() {
        // return a timerview that just sends messages at certain times to update Cast screen
        return new TimerView() {
            private int iStartAt = 0;
            private boolean bIsShowing = false;
            @Override public void setTitle(String s) { }
            @Override public void setTime(String s) { /* not used. Cast will do it's own countdown */ }
            @Override public void setTime(int iStartedCountDownAtSecs, int iSecsLeft, int iReminderAtSecs) {
                if ( (bIsShowing == false)
                   || ( Math.abs(this.iStartAt - iSecsLeft) > 5 && (iSecsLeft > 0)) ) {
                    this.iStartAt = iSecsLeft;
                    show();
                }
            }
            @Override public void setWarnMessage(String s) { }
            @Override public void setPausedMessage(String s) { }
            @Override public void cancel() {
                sendMessage("CountDownTimer.cancel()");
                bIsShowing = false;
            }
            @Override public void timeIsUp() {
                String sTime = m_activity.getString(R.string.oa_time);
                sendMessage(R.id.btn_timer, sTime);
            }
            @Override public void show() {
                if ( iStartAt > 0 ) {
                    sendMessage("CountDownTimer.show(" + this.iStartAt + ")");
                    bIsShowing = true;
                }
            }
            @Override public boolean isShowing() { return bIsShowing; }
        };
    }

    @Override public void castColors(Map<ColorPrefs.ColorTarget, Integer> mColors) {

    }

    @Override public void castDurationChronos() {

    }

    @Override public void castGamesWonAppearance() {

    }

    private void cleanup() {
        castSession = null;
/*
        if ( m_matchModel != null ) {
            int iCleared = m_matchModel.clearListeners(".*.cast.framework.*");
            m_matchModel = null;
        }
*/
    }
    private void invalidateOptionsMenu() {
        //getDelegate().invalidateOptionsMenu(); // TODO: required ?
    }

    private  IBoard      iBoard   = null;
    public void setIBoard(IBoard iBoard) {
        this.iBoard = iBoard;
        updateViewWithColorAndScore(m_activity, m_matchModel);
    }

    /** Listener is to get hold of castSession. Cast session is used for sending messages */
    private SessionManagerListener sessionManagerListener = new SessionManagerListener() {
        @Override public void onSessionStarting(Session session) { }
        @Override public void onSessionStartFailed(Session session, int i) { }
        @Override public void onSessionStarted(Session session, String s) {
            Log.d(TAG, "Cast session started");
            castSession = (CastSession) session;

            updateViewWithColorAndScore(m_activity, m_matchModel);
        }

        @Override public void onSessionEnding(Session session) { }
        @Override public void onSessionEnded(Session session, int i) {
            cleanup();
        }

        @Override public void onSessionResuming(Session session, String s) { }
        @Override public void onSessionResumeFailed(Session session, int i) { }
        @Override public void onSessionResumed(Session session, boolean b) {
            Log.d(TAG, "Cast session resumed");
            castSession = (CastSession) session;

            updateViewWithColorAndScore(m_activity, m_matchModel);
        }

        @Override public void onSessionSuspended(Session session, int i) { }
    };

    private void updateViewWithColorAndScore(Context context, Model matchModel) {
        if ( isCasting() == false ) { return; }
        if ( iBoard == null || matchModel == null ) {
            Log.w(TAG, "Not updating (iBoard=" + iBoard + ", model=" + matchModel + ")");
            return;
        }
        Log.w(TAG, "Updating cast (NEW)");

        Map<ColorPrefs.ColorTarget, Integer> mColors = ColorPrefs.getTarget2colorMapping(context);
        iBoard.initColors(mColors);

/*
        if ( bShowGraphDuringTimer ) {
            iBoard.getTimerView().cancel();
        }
*/

        for(Player p: Model.getPlayers()) {
            iBoard.updateScore(p, matchModel.getScore(p));
            iBoard.updateServeSide    (p, matchModel.getNextDoubleServe(p), matchModel.getNextServeSide(p), matchModel.isLastPointHandout());
            iBoard.updatePlayerName   (p, matchModel.getName   (p), matchModel.isDoubles());
            iBoard.updatePlayerAvatar (p, matchModel.getAvatar (p));
            iBoard.updatePlayerCountry(p, matchModel.getCountry(p));
            iBoard.updatePlayerClub   (p, matchModel.getClub   (p));
        }
        iBoard.updateGameScores();
        iBoard.updateGameBallMessage();
    }
}
