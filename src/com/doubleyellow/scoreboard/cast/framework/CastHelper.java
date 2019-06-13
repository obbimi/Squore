package com.doubleyellow.scoreboard.cast.framework;

import android.app.Activity;
import android.content.res.Resources;
import android.view.Menu;

import com.doubleyellow.android.util.ColorUtil;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.model.Call;
import com.doubleyellow.scoreboard.model.DoublesServe;
import com.doubleyellow.scoreboard.model.Halfway;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.model.Player;
import com.doubleyellow.scoreboard.model.ServeSide;
import com.doubleyellow.scoreboard.model.Util;
import com.doubleyellow.scoreboard.prefs.ColorPrefs;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.scoreboard.timer.TimerView;

import com.doubleyellow.scoreboard.vico.IBoard;
import com.doubleyellow.util.ListUtil;
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
    private Activity    m_activity = null;
    private CastContext castContext = null; /* has nothing to do with android.content.Context */
    private CastSession castSession = null;
    private String      sPackageName = null; /* serves as Namespace */

    @Override public void initCasting(Activity activity) {
        m_activity = activity;
        if ( castContext == null ) {
            castContext = CastContext.getSharedInstance(activity);
        }
        sPackageName = m_activity.getPackageName();
    }

    @Override public void initCastMenu(Activity activity, Menu menu, int iResIdMenuItem) {
        CastButtonFactory.setUpMediaRouteButton(activity, menu, iResIdMenuItem); // TODO: set to visible always in new cast framework?
    }

    @Override public void startCast() {

    }
    @Override public void stopCast() {
        cleanup();
    }
    @Override public void pauseCast() {
        castContext.getSessionManager()
                .removeSessionManagerListener(sessionManagerListener, CastSession.class);

    }
    @Override public void resumeCast() {
        castContext.getSessionManager()
                   .addSessionManagerListener(sessionManagerListener, CastSession.class);

        if (castSession == null) {
            // Get the current session if there is one
            castSession = castContext.getSessionManager().getCurrentCastSession();
        }
    }

    @Override public boolean isCasting() {
        return castSession != null;
    }

    private Model m_matchModel = null;
    @Override public void setModelForCast(Model matchModel) {
        if ( matchModel != m_matchModel ) {
            if ( m_matchModel != null ) {
                m_matchModel.clearListeners(".*"); // clear old model of listeners
            }

            m_matchModel = matchModel;
            m_matchModel.registerListener(new ScoreChangeListener       ());
            m_matchModel.registerListener(new PlayerChangeListener      ());
            m_matchModel.registerListener(new ServeSideChangeListener   ());
            m_matchModel.registerListener(new SpecialScoreChangeListener());
        }
    }

    private class ScoreChangeListener implements Model.OnScoreChangeListener
    {
        @Override public void OnScoreChange(Player p, int iTotal, int iDelta, Call call) {
            sendMessage(IBoard.m_player2scoreId.get(p), iTotal);
        }
    }

    private class ServeSideChangeListener implements Model.OnServeSideChangeListener {
        @Override public void OnServeSideChange(Player p, DoublesServe doublesServe, ServeSide serveSide, boolean bIsHandout) {
            Object oValue = Util.getServeSideCharacter(m_activity, m_matchModel, serveSide, bIsHandout);
            sendMessage(IBoard.m_player2serverSideId.get(p), oValue);
        }
    }

    private class SpecialScoreChangeListener implements Model.OnSpecialScoreChangeListener {
        @Override public void OnGameBallChange(Player[] players, boolean bHasGameBall) {
            if ( PreferenceValues.indicateGameBall(m_activity) == false ) {
                return;
            }

            Map<ColorPrefs.ColorTarget, Integer> mColors = ColorPrefs.getTarget2colorMapping(m_activity);
            Integer scoreButtonBgd = mColors.get(ColorPrefs.ColorTarget.scoreButtonBackgroundColor);
            Integer scoreButtonTxt = mColors.get(ColorPrefs.ColorTarget.scoreButtonTextColor);
            if ( bHasGameBall ) {
                Integer tmpBgColor  = mColors.get(ColorPrefs.ColorTarget.playerButtonBackgroundColor);
                Integer tmpTxtColor = mColors.get(ColorPrefs.ColorTarget.playerButtonTextColor);
                if ( tmpBgColor.equals(scoreButtonBgd) && tmpTxtColor.equals(scoreButtonTxt) ) {
                    // fallback to 'inverse' if player buttons have the same colors as score buttons
                    tmpBgColor  = scoreButtonTxt;
                    tmpTxtColor = scoreButtonBgd;
                }
                if ( tmpBgColor == null || tmpTxtColor == null ) {
                    return;
                }
                scoreButtonBgd = tmpBgColor;
                scoreButtonTxt = tmpTxtColor;
            }
            if ( bHasGameBall == false && players==null ) {
                players = Player.values();
            }
            for(Player player:players) {
                Integer id = IBoard.m_player2scoreId.get(player);
                sendMessage(id, scoreButtonBgd, "background-color");
                sendMessage(id, scoreButtonTxt, "color");
            }

            int iResId = Brand.isRacketlon()? R.string.oa_set_ball : R.string.oa_gameball;
            Player[] possibleMatchBallFor = m_matchModel.isPossibleMatchBallFor();
            if ( ListUtil.isNotEmpty(possibleMatchBallFor) ) {
                iResId = R.string.oa_matchball;

                if ( Brand.isRacketlon() ) {
                    // it can be matchball for the OTHER player

                    // if it is matchball for 2 players at once, it is gummiarm
                    if ( ListUtil.length(possibleMatchBallFor) == 2 ) {
                        iResId = R.string.oa_gummiarm_point;
                    }
                }
            }
            String sMsg = PreferenceValues.getOAString(m_activity, iResId);

            if ( PreferenceValues.floatingMessageForGameBall(m_activity, true) == false ) {
                sendMessage("gameBallMessage", "none", "display");
            } else {
                sendMessage("gameBallMessage", "block", "display");
                sendMessage("gameBallMessage", sMsg, "text");
            }
        }

        @Override public void OnTiebreakReached(int iOccurrenceCount) {
            // no special action on Cast screen
        }

        @Override public void OnGameEndReached(Player leadingPlayer) {
            //iBoard.updateGameBallMessage();
        }

        @Override public void OnGameIsHalfwayChange(int iGameZB, int iScoreA, int iScoreB, Halfway hwStatus) {
            if ( m_matchModel.showChangeSidesMessageInGame(iGameZB) && hwStatus.isHalfway() && hwStatus.changeSidesFor(m_matchModel.getSport()) ) {
                //iBoard.showMessage(getContext().getString(R.string.oa_change_sides), 5);
            } else {
                //iBoard.hideMessage();
            }
        }
        @Override public void OnFirstPointOfGame() {
//            if ( (endOfGameView != null) && endOfGameView.bIsShowing ) {
//                initBoard();
//            }
//            iBoard.updateGameBallMessage();
//            iBoard.updateBrandLogoBasedOnScore();
//            iBoard.updateFieldDivisionBasedOnScore();
//            iBoard.updateGameAndMatchDurationChronos();
        }
    }

    private class PlayerChangeListener implements Model.OnPlayerChangeListener {
        @Override public void OnNameChange(Player p, String sName, String sCountry, String sAvatar, String sClub, boolean bIsDoubles) {
            sendMessage(IBoard.m_player2nameId.get(p), sName);
/*
            iBoard.updatePlayerAvatar (p, sAvatar);
            iBoard.updatePlayerCountry(p, sCountry);
            iBoard.updatePlayerClub   (p, sClub);
*/
        }
        @Override public void OnColorChange(Player p, String sColor, String sColorPrev) { }
        @Override public void OnCountryChange(Player p, String sCountry) { }
        @Override public void OnClubChange(Player p, String sClub) { }
        @Override public void OnAvatarChange(Player p, String sAvatar) { }
    }

    private void sendMessage(Integer iBoardResId, Object oValue) {
        sendMessage(iBoardResId, oValue, "text");
    }
    // sProperty = background-color
    private void sendMessage(Integer iBoardResId, Object oValue, String sProperty) {
        if ( isCasting() == false ) { return; }
        if ( iBoardResId == null ) {
            return;
        }
        Resources resources = m_activity.getResources();
        try {
            String sResName = resources.getResourceName(iBoardResId);
                   sResName = sResName.replaceFirst(".*/", "");

            if ( sProperty.contains("color") && oValue instanceof Integer ) {
                oValue = ColorUtil.getRGBString((Integer) oValue);
            }
            sendMessage(sResName, oValue, sProperty);
        } catch (Resources.NotFoundException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(String sResName, Object oValue, String sProperty) {
        Map map = MapUtil.getMap("id", sResName, "property", sProperty, "value", oValue);
        JSONObject jsonObject = new JSONObject(map);
        castSession.sendMessage("urn:x-cast:" + sPackageName, jsonObject.toString());
    }


    @Override public TimerView getTimerView() {
        return null;
    }

    @Override public void castColors(Map<ColorPrefs.ColorTarget, Integer> mColors) {

    }

    @Override public void castDurationChronos() {

    }

    @Override public void castGamesWonAppearance() {

    }

    private void cleanup() {
        castSession = null;
        if ( m_matchModel != null ) {
            int iCleared = m_matchModel.clearListeners(".*.cast.framework.*");
            m_matchModel = null;
        }
    }
    private void invalidateOptionsMenu() {
        //getDelegate().invalidateOptionsMenu(); // TODO: required ?
    }

    /** Listener is to get hold of castSession. Cast session is used for sending messages */
    private SessionManagerListener sessionManagerListener = new SessionManagerListener() {
        @Override public void onSessionStarting(Session session) { }
        @Override public void onSessionStartFailed(Session session, int i) { }
        @Override public void onSessionStarted(Session session, String s) {
            castSession = (CastSession) session;
            //invalidateOptionsMenu();
        }

        @Override public void onSessionEnding(Session session) { }
        @Override public void onSessionEnded(Session session, int i) {
            cleanup();
            //invalidateOptionsMenu();
        }

        @Override public void onSessionResuming(Session session, String s) { }
        @Override public void onSessionResumeFailed(Session session, int i) { }
        @Override public void onSessionResumed(Session session, boolean b) {
            castSession = (CastSession) session;
            //invalidateOptionsMenu();
        }

        @Override public void onSessionSuspended(Session session, int i) { }
    };
}
