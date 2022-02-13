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
package com.doubleyellow.scoreboard.cast.framework;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.core.view.MenuItemCompat;
import androidx.mediarouter.app.MediaRouteActionProvider;
import androidx.mediarouter.media.MediaRouteSelector;

import com.doubleyellow.android.util.ColorUtil;
import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.URLFeedTask;
import com.doubleyellow.scoreboard.history.MatchGameScoresView;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.model.Player;
import com.doubleyellow.scoreboard.prefs.ColorPrefs;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.scoreboard.timer.Timer;
import com.doubleyellow.scoreboard.timer.TimerView;

import com.doubleyellow.scoreboard.vico.IBoard;
import com.doubleyellow.util.FileUtil;
import com.doubleyellow.util.ListUtil;
import com.doubleyellow.util.MapUtil;
import com.doubleyellow.util.StringUtil;
import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastOptions;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.CastState;
import com.google.android.gms.cast.framework.CastStateListener;
import com.google.android.gms.cast.framework.SessionManager;
import com.google.android.gms.cast.framework.SessionManagerListener;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Takes care of sending JSON messages to the 'webpage' loaded by the chromecast device.
 * - Setting names of players and optionally flags
 * - Setting score
 * - Setting serve side button text
 * - Starting timers (actual countdown implemented on web page itself in javascript)
 *
 * chrome://inspect
 */
public class CastHelper implements com.doubleyellow.scoreboard.cast.ICastHelper
{
    private static final String TAG = "SB." + CastHelper.class.getSimpleName();

    private static final String URN_X_CAST = "urn:x-cast:";

    private Context     m_context      = null;
    private CastContext m_castContext  = null; /* has nothing to do with android.content.Context */
    /** there does NOT need to be a session already for the cast button to show up */
    private CastSession m_castSession  = null;
    public static String m_sMessageNamespace = null; /* serves as Namespace to exchange messages with cast receiver */
    public static String m_sCastingToAppId   = null;

    @Override public void initCasting(Activity activity) {
        m_context      = activity.getApplicationContext();
        if ( m_sMessageNamespace == null ) {
            m_sMessageNamespace = m_context.getPackageName();
        }
        iConstructCastMessagesWhileNotEvenCasting = PreferenceValues.isBrandTesting(m_context)?1:0;

        createCastContext();
        setUpListeners(m_castContext);

        //checkPlayServices();
    }

    private void createCastContext() {
        if ( m_castContext == null ) {
            try {
                m_castContext = CastContext.getSharedInstance(m_context); // requires com.google.android.gms.cast.framework.OPTIONS_PROVIDER_CLASS_NAME to be specified in Manifest.xml
                Log.d(TAG, "[createCastContext] created: " + m_castContext);

                // Initially the ReceiverAppId is set via CastOptionsProvider
                // But fortunately we can change the ID after e.g. user changed different 'cast target' from preferences
                Map.Entry<String, String> remoteDisplayAppId2Info = Brand.brand.getRemoteDisplayAppId2Info(m_context);
                Log.d(TAG, "[createCastContext] remoteDisplayAppId2Info : " + remoteDisplayAppId2Info);

                //CastOptions castOptions = m_castContext.getCastOptions();
                // setReceiverApplicationId() available in 19.0.0, no longer available in com.google.android.gms:play-services-cast-framework:20.0.0
                //castOptions.setReceiverApplicationId(remoteDisplayAppId2Info.getKey());
                // but the 'accidentally?' public method zzb() in 21.0.0 achieves the same ?!
                //castOptions.zzb(remoteDisplayAppId2Info.getKey());
                m_castContext.setReceiverApplicationId(remoteDisplayAppId2Info.getKey());

                if ( false ) {
                    CastOptions.Builder builder = new CastOptions.Builder();
                    builder.setReceiverApplicationId(remoteDisplayAppId2Info.getKey());
                }
            } catch (Exception e) {
                Log.w(TAG, "No casting ..." + e.getMessage());
                //e.printStackTrace(); // com.google.android.gms.dynamite.DynamiteModule$LoadingException: No acceptable module found. Local version is 0 and remote version is 0 (Samsung S4 with custom ROM 8.1)
            }
        }
    }

    private boolean checkPlayServices() {
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int result = googleAPI.isGooglePlayServicesAvailable(m_context);
        if ( result != ConnectionResult.SUCCESS ) {
            //Google Play Services app is not available or version is not up to date. Error the error condition here
            Log.w(TAG, "[checkPlayServices] isGooglePlayServicesAvailable != 0 : " + result);
            return false;
        }
        return true;
    }
    @Override public void initCastMenu(Activity activity, Menu menu) {
        MenuItem m_mediaRouteMenuItem = CastButtonFactory.setUpMediaRouteButton(activity.getApplicationContext(), menu, R.id.media_route_menu_item);
        Log.d(TAG, String.format("initCastMenu: %s (visible: %s)", m_mediaRouteMenuItem, m_mediaRouteMenuItem.isVisible()));

        if ( true ) {
            // just testing some stuff
            MediaRouteActionProvider actionProvider = (MediaRouteActionProvider) MenuItemCompat.getActionProvider(m_mediaRouteMenuItem);
            Log.d(TAG, String.format("initCastMenu actionProvider: %s ", actionProvider));
            MediaRouteSelector routeSelector = actionProvider.getRouteSelector();
            Log.d(TAG, String.format("initCastMenu routeSelector: %s ", routeSelector));
        }

        m_mediaRouteMenuItem.setVisible(true);
        m_mediaRouteMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    @Override public void onActivityStart_Cast() {

    }
    @Override public void onActivityStop_Cast() {
        //cleanup();
    }

    @Override public void onActivityPause_Cast() {
        if ( m_castContext == null ) { return; }
        removeListeners(m_castContext);
    }

    @Override public void onActivityResume_Cast() {
        if ( m_castContext == null ) {
            Log.w(TAG, "onActivityResume_Cast SKIPPED");
            return;
        }
        Log.d(TAG, "onActivityResume_Cast");

        // e.g. after screen rotation
        getCastSessionFromCastContext();
    }

    /** must be called after rotation */
    private void getCastSessionFromCastContext() {
        if ( m_castSession == null ) {
            // Get the current session if there is one
            SessionManager sessionManager = m_castContext.getSessionManager();
            m_castSession = sessionManager.getCurrentCastSession();
            Log.d(TAG, "Cast session from cast context: " + m_castSession); // returns null of user has not started a session yet
        }
        updateViewWithColorAndScore(m_context, m_matchModel);
    }

    private void setUpListeners(CastContext castContext) {
        if ( castContext == null ) {
            Log.w(TAG, "[setUpListeners] Castcontext = null. Not setting up cast listeners");
            return;
        }
        Log.d(TAG, "[setUpListeners] setting up cast listener on castContext :" + castContext);
        castContext.addCastStateListener(m_castStateListener);

        SessionManager sessionManager = castContext.getSessionManager();
        sessionManager.addSessionManagerListener(m_sessionManagerListener, CastSession.class); // must be called from the main thread
    }
    private void removeListeners(CastContext castContext) {
        if ( castContext == null ) { return; }
        Log.d(TAG, "[removeListeners] removing cast listener from castContext : " + castContext);
        castContext.removeCastStateListener(m_castStateListener);

        SessionManager sessionManager = castContext.getSessionManager();
        sessionManager.removeSessionManagerListener(m_sessionManagerListener, CastSession.class); // must be called from the main thread
    }

    /** only for the timerview for now */
    private CastStateListener m_castStateListener = new CastStateListener() {
        @Override public void onCastStateChanged(int iStatus) {
            String sState = CastState.toString(iStatus);
            switch ( iStatus ) {
                case CastState.NO_DEVICES_AVAILABLE: break;
                case CastState.NOT_CONNECTED: {
                    dumpMessages();
                    Timer.removeTimerView(true, getTimerView());
                    break;
                }
                case CastState.CONNECTING:
                    break;
                case CastState.CONNECTED : {
                    Timer.addTimerView(true, getTimerView());
                    break;
                }
                default:
                    sState = iStatus + "?";
                    break;
            }
            Log.d(TAG, "onCastStateChanged: " + sState); // CONNECTING, CONNECTED, NOT_CONNECTED
        }
    };

    private int iConstructCastMessagesWhileNotEvenCasting = 0;

    @Override public boolean isCasting() {
        return (m_castSession != null) || ( iConstructCastMessagesWhileNotEvenCasting != 0 ); // temporary to see if we an only LOG message being send to cast without actually being in a cast session
    }

    private Model m_matchModel = null;
    @Override public void setModelForCast(Model matchModel) {
      //if ( isCasting() == false ) { return; }
      //Log.d(TAG, "New model for cast passed in : " + matchModel);
        if ( (matchModel != m_matchModel) && (matchModel != null)) {
            m_matchModel = matchModel;
        } else {
            Log.w(TAG, "[setModelForCast] not setting model : " + matchModel );
        }
        updateViewWithColorAndScore(m_context, m_matchModel);
    }

    private void sendMessage(Integer iBoardResId, Object oValue) {
        sendChangeViewMessage(iBoardResId, oValue, "text");
    }
    // sProperty = background-color
    public boolean sendChangeViewMessage(Integer iBoardResId, Object oValue, String sProperty) {
        if ( isCasting() == false ) { return false; }
        if ( iBoardResId == null ) {
            return false;
        }
        Resources resources = m_context.getResources();
        try {
            String sResName = resources.getResourceName(iBoardResId);
                   sResName = sResName.replaceFirst(".*/", "");

            return sendChangeViewMessage(sResName, oValue, sProperty);
        } catch (Resources.NotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    private final Map<String, Map> m_mSendChangeViewDelayed = new LinkedHashMap<>(); // make null if you do not want to use delayed

    public boolean sendChangeViewMessage(String sResName, Object oValue, String sProperty) {
        if ( isCasting() == false ) { return false; }
        if ( sProperty.contains("color") && oValue instanceof Integer ) {
            oValue = ColorUtil.getRGBString((Integer) oValue);
        }
        Map map = MapUtil.getMap("id", sResName, "property", sProperty, "value", oValue);
        if ( m_mSendChangeViewDelayed != null ) {
            synchronized (m_mSendChangeViewDelayed) {
                if ( m_cdtDelayedSendChangeViewMessages != null ) {
                    m_cdtDelayedSendChangeViewMessages.cancel();
                }
                Map mReplaced = m_mSendChangeViewDelayed.put(sResName + sProperty, map);
                if ( mReplaced != null ) {
                    //Log.d(TAG, "Discarding : " + mReplaced);
                }
                m_cdtDelayedSendChangeViewMessages = new DelayedChangeViewCountDown(300);
                m_cdtDelayedSendChangeViewMessages.start();
            }
            return true;
        } else {
            return sendMapAsJsonMessage(map);
        }
    }
    private DelayedChangeViewCountDown m_cdtDelayedSendChangeViewMessages = null;

    private class DelayedChangeViewCountDown extends CountDownTimer {
        private DelayedChangeViewCountDown(long iMilliSeconds) {
            super(iMilliSeconds, iMilliSeconds);
        }
        @Override public void onTick(long millisUntilFinished) {
            //Log.d(TAG, "Waiting ... " + millisUntilFinished);
        }
        @Override public void onFinish() {
            Log.d(TAG, "Posting ... with name space " + m_sMessageNamespace + " to device id " + m_sCastingToAppId);
            synchronized (m_mSendChangeViewDelayed) {
                sendListAsJsonMessage(m_mSendChangeViewDelayed);
                m_mSendChangeViewDelayed.clear();
            }
        }
    }

    /** Used e.g. to show/cancel/reset timer and send data for a graph */
    public void sendFunction(String sFunction) {
        if ( isCasting() == false ) { return; }
        Map map = MapUtil.getMap("func", sFunction);
        sendMapAsJsonMessage(map);
    }

    private boolean sendMapAsJsonMessage(Map map) {
        JSONObject jsonObject = new JSONObject(map);
        String sMsg = jsonObject.toString();
        return sendJsonMessage(sMsg);
    }

    private boolean sendListAsJsonMessage(Map<String, Map> mMaps) {
        Collection<Map> lMaps = new ArrayList<>(mMaps.values());
        JSONArray jsonArray = new JSONArray(lMaps);
        String sMsg = jsonArray.toString();
        return sendJsonMessage(sMsg);
    }

    private static List<String> m_lMessages = new ArrayList<>();
    private boolean sendJsonMessage(String sMsg) {
        Log.v(TAG, "sendMessage: " + sMsg);
        try {
            if ( ListUtil.isEmpty(m_lMessages) || (m_lMessages.get(0).equals(sMsg) == false) ) {
                m_lMessages.add(sMsg);
            } else {
                Log.w(TAG, "Not adding same message again :" + sMsg);
            }
            if ( iConstructCastMessagesWhileNotEvenCasting != 0 ) {
                iConstructCastMessagesWhileNotEvenCasting++;
                if ( iConstructCastMessagesWhileNotEvenCasting % 5 == 1 ) {
                    Log.w(TAG, "NOT CASTING JUST CONSTRUCTING MESSAGES ANYWAY");
                }
            } else {
                m_castSession.sendMessage(URN_X_CAST + m_sMessageNamespace, sMsg);
            }
            return true;
        } catch (Exception e) {
            // seen IllegalStateException crashing the app reported in PlayStore
            e.printStackTrace();
        }
        return false;
    }
    private void dumpMessages() {
        if (PreferenceValues.isBrandTesting(m_context) && ListUtil.isNotEmpty(m_lMessages) ) {
            // dump messages for easy replay testing
            //Log.d(TAG, "" + ListUtil.join(m_lMessages, "\n"));
            try {
                File dir = m_context.getFilesDir();
                File fMessages = new File(dir, "cast_messages.txt");
                FileUtil.writeTo(fMessages, ListUtil.join(m_lMessages, "\n"));
                m_lMessages.clear();

                Log.w(TAG, "adb pull " + fMessages.getAbsolutePath() );
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
                sendFunction(CountDownTimer_cancel + "()");
                bIsShowing = false;
            }
            @Override public void timeIsUp() {
                String sTime = m_context.getString(R.string.oa_time);
                sendMessage(R.id.btn_timer, sTime);
            }
            @Override public void show() {
                if ( iStartAt > 0 ) {
                    sendFunction(CountDownTimer_show + "(" + this.iStartAt + ")");
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
        // already taken care of by updateViewWithColorAndScore()
    }

    private void cleanup() {
        m_castSession = null;
/*
        if ( m_matchModel != null ) {
            int iCleared = m_matchModel.clearListeners(".*.cast.framework.*");
            m_matchModel = null;
        }
*/
    }

    private  IBoard      iBoard   = null;
    public void setIBoard(IBoard iBoard) {
        this.iBoard = iBoard;
        updateViewWithColorAndScore(m_context, m_matchModel);
    }

    /** Listener is to get hold of castSession. Cast session is used for sending messages */
    private SessionManagerListener<CastSession> m_sessionManagerListener = new SessionManagerListener<CastSession>() {
        private static final String TAG = "SB.SessionMngListener";
        @Override public void onSessionStarted(CastSession session, String s) {
            Log.d(TAG, "[onSessionStarted] " + session + " " + s);
            m_castSession = session;
            iConstructCastMessagesWhileNotEvenCasting = 0;

            updateViewWithColorAndScore(m_context, m_matchModel);
        }

        @Override public void onSessionResumed(CastSession session, boolean b) {
            Log.d(TAG, "[onSessionResumed] " + session + " " + b);
            m_castSession = session;

            updateViewWithColorAndScore(m_context, m_matchModel);
        }

        @Override public void onSessionSuspended(CastSession session, int i) { }
        @Override public void onSessionStarting(CastSession session) {
            Log.d(TAG, "[onSessionStarting] Cast session starting..." + session);
        }
        @Override public void onSessionResuming(CastSession session, String s) { }
        @Override public void onSessionEnding(CastSession session) { }
        @Override public void onSessionEnded(CastSession session, int i) {
            iConstructCastMessagesWhileNotEvenCasting = PreferenceValues.isBrandTesting(m_context) ? 1 : 0;
            cleanup();
        }
        @Override public void onSessionStartFailed(CastSession session, int i) {
            Log.w(TAG, "Cast session failed to start: " + i);
        }
        @Override public void onSessionResumeFailed(CastSession session, int i) {
            Log.w(TAG, "[onSessionResumeFailed] Cast session: " + session + ", i=" + i); // i=2005
        }
    };

    private void updateViewWithColorAndScore(Context context, Model matchModel) {
        if ( isCasting() == false || iBoard == null || matchModel == null ) {
            Log.w(TAG, "[updateViewWithColorAndScore] Not updating (isCasting=" + isCasting() + ", iBoard=" + iBoard + ", model=" + matchModel + ")");
            return;
        }
        CastSession castSession = m_castSession;
        if ( castSession != null ) {
            castSession.addCastListener(new CastListener());
            try {
                castSession.setMessageReceivedCallbacks(URN_X_CAST + m_sMessageNamespace, new Cast.MessageReceivedCallback() {
                    @Override public void onMessageReceived(CastDevice castDevice, String sNamespace, String sMsg) {
                        Log.d(TAG, String.format("received message back from %s [%s] : %s", castDevice.getFriendlyName(), sNamespace, sMsg));

                        try {
                            JSONObject mMessage = new JSONObject(sMsg);
                            if ( m_context instanceof ScoreBoard ) {
                                ScoreBoard sb = (ScoreBoard) m_context;
                                sb.handleMessageFromCast(mMessage);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }


            try {
                CastDevice castDevice = castSession.getCastDevice();
                if ( castDevice != null ) {
                    String sFriendlyName = castDevice.getFriendlyName();
                    Log.d(TAG, "castDevice.getFriendlyName(): " + sFriendlyName); // e.g. Living
                }
                Cast.ApplicationConnectionResult applicationConnectionResult = castSession.getApplicationConnectionResult();

                ApplicationMetadata applicationMetadata = castSession.getApplicationMetadata();
                if ( applicationMetadata != null ) {
                    List<String> supportedNamespaces = applicationMetadata.getSupportedNamespaces(); // the namespaces added by the receiver using m_crContext.addCustomMessageListener(namespace)
                    Log.d(TAG, "Supported namespaces: " + ListUtil.join(supportedNamespaces, "\n") );
                    // [ urn:x-cast:com.google.cast.cac
                    // , urn:x-cast:com.google.cast.debugoverlay
                    // , urn:x-cast:com.google.cast.debuglogger
                    // , urn:x-cast:com.doubleyellow.scoreboard
                    // , urn:x-cast:com.doubleyellow.badminton
                    // , urn:x-cast:com.doubleyellow.tennispadel
                    // , urn:x-cast:com.doubleyellow.tabletennis
                    // , urn:x-cast:com.google.cast.broadcast
                    // , urn:x-cast:com.google.cast.media
                    // ]
                }

                RemoteMediaClient remoteMediaClient = castSession.getRemoteMediaClient();
                if ( remoteMediaClient != null ) {
                    Log.d(TAG, "remoteMediaClient.getNamespace(): " + remoteMediaClient.getNamespace()); // urn:x-cast:com.google.cast.media
    /*
                    remoteMediaClient.registerCallback(new RemoteMediaClient.Callback() { });
    */
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //GoogleCast.EventEmitter.addListener();
        // W CDC|API|500: [API] Ignoring message. Namespace 'urn:x-cast:com.doubleyellow.scoreboard' has not been registered.

        Log.d(TAG, "Updating cast (CAF)");

        Map<ColorPrefs.ColorTarget, Integer> mColors = ColorPrefs.getTarget2colorMapping(context);
        iBoard.initColors(mColors);

/*
        if ( bShowGraphDuringTimer ) {
            iBoard.getTimerView().cancel();
        }
*/
        iBoard.updateGameScores();
        iBoard.updateGameBallMessage("PS:updateViewWithColorAndScore");
        if ( Brand.isGameSetMatch() ) {
            MatchGameScoresView.ScoresToShow scoresToShow = iBoard.updateSetScoresToShow(false);
            sendFunction(GameScores_display + "(" + scoresToShow.equals(MatchGameScoresView.ScoresToShow.GamesWonPerSet) + ")");
        }
        iBoard.updateGameAndMatchDurationChronos();

        for(Player p: Model.getPlayers()) {
            iBoard.updateScore        (p, matchModel.getScore(p));
            iBoard.updateServeSide    (p, matchModel.getNextDoubleServe(p), matchModel.getNextServeSide(p), matchModel.isLastPointHandout());
            iBoard.updatePlayerAvatar (p, matchModel.getAvatar (p));
            iBoard.updatePlayerCountry(p, matchModel.getCountry(p));
            iBoard.updatePlayerClub   (p, matchModel.getClub   (p));
            iBoard.updatePlayerName   (p, matchModel.getName   (p), matchModel.isDoubles()); // player name last... if both are communicate cast screen will display screen elements
        }

        updateLogo(context);
        updateSponsor(context);
    }

    private void updateSponsor(Context context) {
        String sSponsorURL = PreferenceValues.castScreenSponsorUrl(context);
        boolean bShow   = PreferenceValues.castScreenShowSponsor(context);
        if ( bShow && StringUtil.isNotEmpty(sSponsorURL) ) {
            sSponsorURL = URLFeedTask.prefixWithBaseIfRequired(sSponsorURL);
            sSponsorURL = String.format(sSponsorURL, Brand.brand);
        } else {
            sSponsorURL = "";
        }
        sendFunction(String.format(LogoSponsor_setSponsor + "('%s')", sSponsorURL));
    }

    private void updateLogo(Context context) {
        String sLogoURL = PreferenceValues.castScreenLogoUrl(context);
        boolean bShow   = PreferenceValues.castScreenShowLogo(context);
        if ( bShow && StringUtil.isNotEmpty(sLogoURL) ) {
            sLogoURL = URLFeedTask.prefixWithBaseIfRequired(sLogoURL);
            try {
                sLogoURL = String.format(sLogoURL, Brand.brand);
            } catch (Exception e) {
            }
        } else {
            sLogoURL = "";
        }
        sendFunction(String.format(LogoSponsor_setLogo + "('%s')", sLogoURL));
    }

    /** Not invoked very consistently... it seems */
    private static class CastListener extends Cast.Listener
    {
        private CastListener() {
            super();
        }

        @Override public void onApplicationStatusChanged() {
            Log.d(TAG, "onApplicationStatusChanged");
            super.onApplicationStatusChanged();
        }

        @Override public void onApplicationMetadataChanged(ApplicationMetadata applicationMetadata) {
            Log.d(TAG, "onApplicationMetadataChanged(" + applicationMetadata + ")");
            super.onApplicationMetadataChanged(applicationMetadata); // e.g. holds "My Test Receiver" and value of CUSTOM_RECEIVER_APP_ID_brand_test
        }

        @Override public void onApplicationDisconnected(int i) {
            Log.d(TAG, "onApplicationDisconnected(" + i + ")");
            super.onApplicationDisconnected(i);
        }

        @Override public void onActiveInputStateChanged(int i) {
            Log.d(TAG, "onActiveInputStateChanged(" + i + ")");
            super.onActiveInputStateChanged(i); // seen: -1
        }

        @Override public void onStandbyStateChanged(int i) {
            Log.d(TAG, "onStandbyStateChanged(" + i + ")");
            super.onStandbyStateChanged(i); // seen -1
        }

        @Override public void onVolumeChanged() {
            Log.d(TAG, "onVolumeChanged");
            super.onVolumeChanged();
        }
    }
}
