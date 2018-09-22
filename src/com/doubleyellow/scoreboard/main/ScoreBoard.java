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

package com.doubleyellow.scoreboard.main;

import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.SensorManager;
import android.net.Uri;
import android.nfc.*;
import android.nfc.tech.NfcF;
import android.os.*;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.widget.DrawerLayout;
import android.text.Html;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.*;
import android.webkit.WebView;
import android.widget.*;

import com.doubleyellow.android.SystemUtil;
import com.doubleyellow.android.handler.OnClickXTimesHandler;
import com.doubleyellow.android.util.ContentReceiver;
import com.doubleyellow.android.util.ContentUtil;
import com.doubleyellow.android.util.RateMeMaybe;
import com.doubleyellow.android.util.ColorUtil;
import com.doubleyellow.android.view.*;
import com.doubleyellow.prefs.DynamicListPreference;
import com.doubleyellow.prefs.OrientationPreference;
import com.doubleyellow.prefs.RWValues;
import com.doubleyellow.scoreboard.*;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.activity.*;
import com.doubleyellow.scoreboard.archive.ArchiveTabbed;
import com.doubleyellow.scoreboard.archive.PreviousMatchSelector;
import com.doubleyellow.scoreboard.cast.EndOfGameView;
import com.doubleyellow.scoreboard.demo.*;
import com.doubleyellow.demo.*;
import com.doubleyellow.scoreboard.dialog.Handicap;
import com.doubleyellow.scoreboard.dialog.announcement.EndGameAnnouncement;
import com.doubleyellow.scoreboard.dialog.announcement.EndMatchAnnouncement;
import com.doubleyellow.scoreboard.dialog.announcement.StartGameAnnouncement;
import com.doubleyellow.scoreboard.feed.Authentication;
import com.doubleyellow.scoreboard.feed.Preloader;
import com.doubleyellow.scoreboard.model.*;
import com.doubleyellow.scoreboard.model.Util;
import com.doubleyellow.scoreboard.timer.*;
import com.doubleyellow.scoreboard.timer.Timer;
import com.doubleyellow.scoreboard.history.MatchHistory;
import com.doubleyellow.scoreboard.match.*;
import com.doubleyellow.scoreboard.vico.IBoard;
import com.doubleyellow.scoreboard.view.GameHistoryView;
import com.doubleyellow.scoreboard.view.PlayersButton;
import com.doubleyellow.scoreboard.view.ServeButton;
import com.doubleyellow.android.handler.OnBackPressExitHandler;
import com.doubleyellow.scoreboard.dialog.*;
import com.doubleyellow.scoreboard.prefs.*;
import com.doubleyellow.util.*;
import com.doubleyellow.view.SBRelativeLayout;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.doubleyellow.android.showcase.ShowcaseView;
import com.doubleyellow.android.showcase.ShowcaseConfig;
import com.doubleyellow.android.showcase.ShowcaseSequence;

import java.io.*;
import java.util.*;

/* ChromeCast */
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.ActionBar;

/**
 * The main Activity of the scoreboard app.
 */
public class ScoreBoard extends XActivity implements NfcAdapter.CreateNdefMessageCallback, NfcAdapter.OnNdefPushCompleteCallback, MenuHandler, DrawTouch
{
    private static final String TAG = "SB." + ScoreBoard.class.getSimpleName();

    public  static         Model                matchModel ;

    //-------------------------------------------------------------------------
    // Permission granting callback
    //-------------------------------------------------------------------------
    private int      iMenuToRepeatOnPermissionGranted = 0;
    private Object[] oaMenuCtxForRepeat               = null;
    @Override public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        PreferenceKeys key = null;
        try {
            key = PreferenceKeys.values()[requestCode];
        } catch (Exception e) {
            e.printStackTrace();
        }

        if ( key != null ) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission was granted, yay! Do the task you need to do.
                Log.d(TAG, String.format("Permission granted: %s (%s)", key, grantResults[0]));
                if ( false && (iMenuToRepeatOnPermissionGranted != 0) ) {
                    handleMenuItem(iMenuToRepeatOnPermissionGranted, oaMenuCtxForRepeat);
                    iMenuToRepeatOnPermissionGranted = 0;
                    oaMenuCtxForRepeat               = null;
                }
            } else {
                // permission denied. Disable the functionality that depends on this permission.
                Log.d(TAG, String.format("Permission denied: %s (%s)", key, grantResults));
            }
        }
    }

    //-------------------------------------------------------------------------
    // Controller listeners helper methods
    //-------------------------------------------------------------------------
    private int getXmlIdOfParent(View view) {
        int id = view.getId();
        Player player = IBoard.m_id2player.get(id);
        if ( player == null ) {
            String sTag = String.valueOf(view.getTag());
            if ( sTag.contains(PlayersButton.SUBBUTTON)) {
                id = Integer.parseInt(sTag.replaceFirst(PlayersButton.SUBBUTTON + ".*", ""));
                player = IBoard.m_id2player.get(id);
            }
            if ( player == null ) {
                return 0;
            }
        }
        return id;
    }
    private DoublesServe getInOrOut(View view) {
        String sTag = String.valueOf(view.getTag());
        if ( sTag.contains(PlayersButton.SUBBUTTON)) {
            int iSeq = Integer.parseInt(sTag.replaceFirst(".*" + PlayersButton.SUBBUTTON, ""));
            return DoublesServe.values()[iSeq];
        } else {
            return DoublesServe.NA;
        }
    }

    //-------------------------------------------------------------------------
    // Controller listeners
    //-------------------------------------------------------------------------
    public  final ScoreButtonListener       scoreButtonListener        = new ScoreButtonListener();
    private final ServerSideButtonListener  serverSideButtonListener   = new ServerSideButtonListener();
    private final GameScoresListener        gameScoresListener         = new GameScoresListener();
    public  final PlayerNamesButtonListener namesButtonListener        = new PlayerNamesButtonListener();
    private final ScoreBoardTouchListener   scoreBoardTouchListener    = new ScoreBoardTouchListener();

    private final SimpleGestureListener     scoreButtonGestureListener = new SimpleGestureListener(scoreBoardTouchListener, scoreBoardTouchListener, scoreButtonListener     , null /*scoreButtonListener*/, scoreBoardTouchListener);
    private final SimpleGestureListener     namesButtonGestureListener = new SimpleGestureListener(scoreBoardTouchListener, scoreBoardTouchListener, namesButtonListener     , namesButtonListener         , scoreBoardTouchListener);
    private final SimpleGestureListener     serveButtonGestureListener = new SimpleGestureListener(scoreBoardTouchListener, scoreBoardTouchListener, serverSideButtonListener, serverSideButtonListener    , scoreBoardTouchListener);
    private final SimpleGestureListener     gamesScoresGestureListener = new SimpleGestureListener(scoreBoardTouchListener, scoreBoardTouchListener, gameScoresListener      , gameScoresListener          , scoreBoardTouchListener);

    private final CurrentGameScoreListener  currentGameScoreListener   = new CurrentGameScoreListener();

    private class ScoreBoardTouchListener implements SimpleGestureListener.SwipeListener, SimpleGestureListener.EraseListener, SimpleGestureListener.TwoFingerClickListener
    {
        @Override public boolean onTwoFingerClick(View view) {
            int viewId = getXmlIdOfParent(view);
            dbgmsg("Two finger clicked", viewId, 0);
            Player player = IBoard.m_id2player.get(viewId);
            switch(viewId) {
                case R.id.btn_score1:
                case R.id.btn_score2:
                    // on tablet, the buttons are so big that two finger touch/click may be performed without user thinking about it
                    matchModel.changeScore(player);
                    return true;
                case R.id.txt_player1:
                case R.id.txt_player2:
                    if ( Brand.isSquash() ) {
                        // TODO: for doubles, switch in/out in such a way that touched player becomes the server
                        Log.d(TAG, String.format("Two finger click on player %s", player)); // TODO: start broken ball/racket dialog
                        showBrokenEquipment(player);
                        break;
                    } else {
                        handleMenuItem(R.id.sb_swap_players);
                    }
            }
            return false;
        }

        @Override public boolean onSwipe(View view, Direction direction, float maxD, float percentageOfView) {
            int viewId = getXmlIdOfParent(view);
            Player player = IBoard.m_id2player.get(viewId);
            dbgmsg("Swipe to " + direction +" of " + maxD + " (%=" + percentageOfView + ",p=" + player + ")", viewId, 0);
            if ( percentageOfView < 0.50 ) { return false; }
            if ( player          == null ) { return false; }
            boolean isServer = player.equals(matchModel.getServer());
            switch( viewId ) {
                case R.id.btn_score1:
                case R.id.btn_score2:
                    if ( EnumSet.of(Direction.E, Direction.W).contains(direction) ) {
                        if ( Brand.isSquash() && isServer ) {
                            // perform undo if swiped horizontally over server score button (= person who scored last) TODO: only true for Squash!!
                            handleMenuItem(R.id.sb_undo_last);
                        } else {
                            Player pLastScorer = matchModel.getLastScorer();
                            if ( player.equals(pLastScorer) ) {
                                // perform undo if swiped horizontally over last scorer
                                handleMenuItem(R.id.sb_undo_last);
                            }
                        }
                        return true;
                    }
                    break;
                case R.id.txt_player1:
                case R.id.txt_player2:
                    // allow changing sides if last point was a handout
                    ServeSide nextServeSide = matchModel.getNextServeSide(player);
                    if ( matchModel.isLastPointHandout() && isServer) {
                        if (nextServeSide.equals(ServeSide.L) && direction.equals(Direction.E)) {
                            // change from left to right
                            matchModel.changeSide(player);
                            return true;
                        } else if (nextServeSide.equals(ServeSide.R) && direction.equals(Direction.W)) {
                            // change from right to left
                            matchModel.changeSide(player);
                            return true;
                        }
                    }
                    break;
                default:
                    break;
            }
            return false;
        }

        @Override public boolean onErase(View view, HVD hvd, float maxD, float percentageOfView) {
            int viewId = getXmlIdOfParent(view);
            dbgmsg("Erase " + hvd + " movement of " + maxD + " (%=" + percentageOfView + ")", viewId, 0);
            if ( percentageOfView < 0.50 ) {
                return false;
            }
            switch(viewId) {
                case R.id.btn_score1:
                case R.id.btn_score2:
                    if ( hvd.equals(HVD.Diagonal) ) {
                        return handleMenuItem(R.id.sb_clear_score);
                    }
                    break;
                case R.id.txt_player1:
                case R.id.txt_player2:
                    if ( ViewUtil.getScreenHeightWidthMinimum(ScoreBoard.this) < 320 ) {
                        // just to have a way to get to the settings of no actionbar is visible on android wear
                        return handleMenuItem(R.id.sb_settings);
                    } else {
                        return handleMenuItem(R.id.sb_edit_event_or_player);
                    }
                default:
                    break;
            }
            return false;
        }
    }

    private TouchBothListener.LongClickBothListener longClickBothListener = new TouchBothListener.LongClickBothListener() {
        @Override public boolean onLongClickBoth(View view1, View view2) {
            dbgmsg("Long Clicked both", view1.getId(), view2.getId());

            List<Integer> lIds = Arrays.asList(view1.getId(), view2.getId());
            if ( lIds.containsAll(Arrays.asList(R.id.txt_player1, R.id.txt_player2)) ) {
                if ( matchModel.isLocked() ) {
                    if ( matchModel.isUnlockable() ) {
                        return handleMenuItem(R.id.sb_unlock);
                    }
                } else {
                    return handleMenuItem(R.id.sb_lock, LockState.LockedManualGUI);
                }
            }
            if ( lIds.containsAll(Arrays.asList(R.id.btn_score1, R.id.btn_score2)) ) {
                return handleMenuItem(R.id.sb_match_format);
            }
            if ( lIds.containsAll(Arrays.asList(R.id.btn_side1, R.id.btn_side2)) ) {
                // Nothing for end user yet
                if ( PreferenceValues.isBrandTesting(ScoreBoard.this) ) {
                    Brand newBrandForTesting = ListUtil.getNextEnum(Brand.brand);
                    RWValues.setEnum(PreferenceKeys.squoreBrand, ScoreBoard.this, newBrandForTesting);
                    Brand.setBrandPrefs(ScoreBoard.this);
                    Brand.setSportPrefs(ScoreBoard.this);
                    DynamicListPreference.deleteCacheFile(ScoreBoard.this, PreferenceKeys.colorSchema.toString());
                    doRestart(ScoreBoard.this);
                }
            }
            // TODO: if playing double switch player names if clicking the two child view of R.id.txt_player1
            return false;
        }
    };

    private TouchBothListener.ClickBothListener clickBothListener = new TouchBothListener.ClickBothListener() {
        private long lLastBothClickServeSideButtons = 0;
        private final List<Integer> bothScoreButtons  = Arrays.asList(R.id.btn_score1 , R.id.btn_score2 );
        private final List<Integer> bothPlayerButtons = Arrays.asList(R.id.txt_player1, R.id.txt_player2);
        private final List<Integer> bothSideButtons   = Arrays.asList(R.id.btn_side1  , R.id.btn_side2  );

        @Override public boolean onClickBoth(View view1, View view2) {
            dbgmsg("Clicked both", view1.getId(), view2.getId());

            List<Integer> lIds = Arrays.asList(view1.getId(), view2.getId());
            if ( lIds.containsAll(bothPlayerButtons) ) {
                if ( isLandscape() ) {
                    handleMenuItem(R.id.sb_swap_players);
                } else {
                    handleMenuItem(R.id.dyn_new_match);
                }
                return true;
            }
            if ( lIds.containsAll(bothScoreButtons) ) {
                if ( isInPromoMode() == false ) {
                    // do not do this in promo mode: allow to tab both score buttons very fast to go to the end of a game
                    return handleMenuItem(R.id.sb_adjust_score);
                }
            }
            if ( lIds.containsAll(bothSideButtons) ) {
                // only works decently in portrait mode when side buttons are not 'over' the score buttons
                if ( (System.currentTimeMillis() - lLastBothClickServeSideButtons < 500)) {
                    // toggle demo mode if
                    handleMenuItem(R.id.sb_toggle_demo_mode);
                    return true;
                } else {
                    lLastBothClickServeSideButtons = System.currentTimeMillis();
                }
            }
            return false;
        }
    };

    //-------------------------------------------------------------------------
    // Helper methods to prevent score change while EndGame dialog was in about/in progress to be presented (fast double click on scorebutton on gameball)
    //-------------------------------------------------------------------------

    public void enableScoreButtons() {
        for ( Player p: Player.values() ){
           enableScoreButton(p);
        }
    }
    private void enableScoreButton(Player player) {
        View view = findViewById(IBoard.m_player2scoreId.get(player));
        if ( view == null ) { return; }
        if ( view.isEnabled() ) { return; }
        view.setEnabled(true);
        Log.d(TAG, "Re-enabled score button for player " + player);
    }
    void disableScoreButton(View view) {
        view.setEnabled(false);
        Log.d(TAG, "Disabling score button for model " + matchModel);
    }
    private class ScoreButtonListener implements View.OnClickListener
    {
        @Override public void onClick(View view) {
          //Log.d(TAG, "Received click for model " + matchModel);
            Player player = IBoard.m_id2player.get(view.getId());
            if ( matchModel.isPossibleGameBallFor(player) && (bGameEndingHasBeenCancelledThisGame == false) ) {
                if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 /* 17 */ ) {
                    // score will go to game-end, and most likely a dialog will be build and show. Prevent any accidental score changes while dialog is about to be shown
                    disableScoreButton(view);
                }
            }
            enableScoreButton(player.getOther());
            changeScore(player);
        }
    }

    private void changeScore(Player player) {
        if ( warnModelIsLocked() ) { return; }
      //Log.d(TAG, "Changing score for for model " + matchModel);
        matchModel.changeScore(player);
    }

    private ToggleResult toggleActionBar(ActionBar actionBar) {
        if ( actionBar == null ) { return ToggleResult.nothing; }

        if ( actionBar.isShowing() ) {
            ScoreBoard.bUseActionBar = ToggleResult.setToFalse;
            actionBar.hide();
        } else {
            ScoreBoard.bUseActionBar = ToggleResult.setToTrue;
            //RWValues.setBoolean(PreferenceKeys.showActionBar, ScoreBoard.this, true);
            RWValues.setOverwrite(PreferenceKeys.showActionBar, true); // temporary
            actionBar.show();
            initActionBarSettings(menuItemsWithOrWithoutText);
            showAppropriateMenuItemInActionBar();
            PreferenceValues.removeOverwrite(PreferenceKeys.showActionBar);
        }
        return bUseActionBar;
    }

    private class ServerSideButtonListener implements View.OnClickListener, View.OnLongClickListener {
        private OnClickXTimesHandler onClickXTimesHandler = null;
        @Override public void onClick(View view) {
            if ( warnModelIsLocked() ) { return; }
            int viewId = getXmlIdOfParent(view);
            Player player = IBoard.m_id2player.get(viewId);
            if ( player == null ) { return; }
            if ( matchModel.isDoubles() && player.equals( matchModel.getServer() ) ) {
                DoublesServe inOutClickedOn = getInOrOut(view);
                DoublesServe inOut = matchModel.getNextDoubleServe(player);
                if ( (inOutClickedOn != null) && inOutClickedOn.equals(DoublesServe.NA) == false && inOutClickedOn.equals(inOut) == false ) {
                    // clicked on serve side button of non-serving doubles player of the same team
                    matchModel.changeDoubleServe(player);
                } else {
                    matchModel.changeSide(player);
                }
            } else {
                if ( onClickXTimesHandler == null ) {
                    onClickXTimesHandler = new OnClickXTimesHandler(300, 10);
                }
                if ( onClickXTimesHandler.handle() ) {
                    mainMenu.findItem(R.id.sb_demo                             ).setVisible(true);
                    mainMenu.findItem(R.id.sb_toggle_demo_mode                 ).setVisible(true);
                    mainMenu.findItem(R.id.sb_download_posted_to_squore_matches).setVisible(true);
                    mainMenu.findItem(R.id.android_language                    ).setVisible(true);
                    if ( toggleDemoMode(null).equals(Mode.ScreenShots) ) {
                        PreferenceValues.setEnum   (PreferenceKeys.BackKeyBehaviour            , ScoreBoard.this, BackKeyBehaviour.UndoScoreNoConfirm); // for adb demo/screenshots script
                        PreferenceValues.setBoolean(PreferenceKeys.showFullScreen              , ScoreBoard.this, true);                                // for adb demo/screenshots script
                        PreferenceValues.setBoolean(PreferenceKeys.showActionBar               , ScoreBoard.this, false);                               // for adb demo/screenshots script
                        PreferenceValues.setBoolean(PreferenceKeys.showAdjustTimeButtonsInTimer, ScoreBoard.this, false);                               // for cleaner screenshots
                        PreferenceValues.setBoolean(PreferenceKeys.showUseAudioCheckboxInTimer , ScoreBoard.this, false);                               // for cleaner screenshots
                    } else {
                        PreferenceValues.setEnum   (PreferenceKeys.BackKeyBehaviour            , ScoreBoard.this, BackKeyBehaviour.PressTwiceToExit);
                        PreferenceValues.setBoolean(PreferenceKeys.showAdjustTimeButtonsInTimer, ScoreBoard.this, R.bool.showAdjustTimeButtonsInTimer_default);
                        PreferenceValues.setBoolean(PreferenceKeys.showUseAudioCheckboxInTimer , ScoreBoard.this, R.bool.showUseAudioCheckboxInTimer_default);
                    }
                    if ( m_mode.equals(Mode.Debug) ) {
                        PreferenceValues.setString(PreferenceKeys.FeedFeedsURL, ScoreBoard.this, getString(R.string.feedFeedsURL_default) + "?suffix=.new");
                        //PreferenceValues.setNumber (PreferenceKeys.viewedChangelogVersion, ScoreBoard.this, PreferenceValues.getAppVersionCode(ScoreBoard.this)-1);
                    }
                }
                if ( Brand.isTabletennis() ) {
                    if ( matchModel.isInMode(TabletennisModel.Mode.Expedite) ) {
                        matchModel.changeSide(player);
                    } else {
                        // Who will serve at what score is totally determined by who started serving the first point
                    }
                } else if ( Brand.isRacketlon() ) {
                    // Who will serve at what score is totally determined by who started serving the first point
                } else {
                    matchModel.changeSide(player);
                }
            }
        }

        @Override public boolean onLongClick(View view) {
            int viewId = getXmlIdOfParent(view);
            Player pl = IBoard.m_id2player.get(viewId);
            if ( pl == null ) { return false; }

            if ( m_mode.equals(ScoreBoard.Mode.ScreenShots) && pl.equals(Player.B) ) {
                // switch to a different color schema
                int i = PreferenceValues.getInteger(PreferenceKeys.colorSchema, ScoreBoard.this, 0);
                PreferenceValues.setNumber(PreferenceKeys.colorSchema, ScoreBoard.this, i + 1);
                ScoreBoard.matchModel.setDirty();
                ColorPrefs.clearColorCache();
                ScoreBoard.this.onRestart();
                return true;
            }
/*
            EnumSet<ShowPlayerColorOn> colorOns = PreferenceValues.showPlayerColorOn(ScoreBoard.this);
            if (  colorOns.contains(ShowPlayerColorOn.ServeSideButton)
               || (colorOns.contains(ShowPlayerColorOn.ScoreButton) == false)
               ) {
*/
                showColorPicker(pl);
/*
            }
*/
            return true;
        }
    }

    private class GameScoresListener implements View.OnLongClickListener, View.OnClickListener {
        private long lActionBarToggled = 0L;
        @Override public void onClick(View view) {
            long currentTime = System.currentTimeMillis();
            if ( currentTime - lActionBarToggled > 1500 ) {
                showScoreHistory();
            }
        }
        @Override public boolean onLongClick(View view) {
            ActionBar actionBar = getXActionBar();
            if ( (actionBar != null) /*&& (PreferenceValues.showActionBar(ScoreBoard.this) == false)*/ ) {
                toggleActionBar(actionBar);
                lActionBarToggled = System.currentTimeMillis();
            } else {
                this.onClick(view);
            }
            return false;
        }
    }

    private class CurrentGameScoreListener implements View.OnClickListener {
        private long lLastClickTime;
        private Toast toast = null;

        @Override public void onClick(View view) {
            long currentTime = System.currentTimeMillis();
            if ( currentTime - lLastClickTime > 1500 ) {
                toast = Toast.makeText(ScoreBoard.this, R.string.press_again_to_undo_last_score, Toast.LENGTH_LONG);
                toast.setGravity(Gravity.BOTTOM, 0, 0);
                toast.show();
                lLastClickTime = currentTime;
            } else {
                handleMenuItem(R.id.sb_undo_last);
                lLastClickTime = 0;
                if ( toast != null ) {
                    toast.cancel();
                }
            }
        }
    }

    private class PlayerNamesButtonListener implements View.OnLongClickListener, View.OnClickListener
    {
        @Override public boolean onLongClick(View view) {
            int viewId = getXmlIdOfParent(view);
            Player pl = IBoard.m_id2player.get(viewId);
            if ( pl == null ) { return false; }

            if ( matchModel.isDoubles() ) {
                // toggle player names of the long clicked team
                _swapDoublePlayers(pl);
            } else {
                showConduct(pl);
            }

            return true;
        }

        @Override public void onClick(View view) {
            int viewId = getXmlIdOfParent(view);
            Player pl = IBoard.m_id2player.get(viewId);
            if ( pl == null ) { return; }
            showAppeal(pl);
        }
    }

    //-------------------------------------------------------------------------
    // Swap (double) players
    //-------------------------------------------------------------------------

    /** might present a dialog to the user, 'Based-On-Preference'. Returns true if a dialog was presented to the user. */
    private boolean swapPlayers_BOP() {
        Feature swapPlayersFeature = PreferenceValues.swapPlayersHalfwayGame(ScoreBoard.this);
        switch(swapPlayersFeature) {
            case DoNotUse:
                return false;
            case Suggest:
                _confirmSwapPlayers(null);
                return true;
            case Automatic:
                handleMenuItem(R.id.sb_swap_players);
                return false;
        }
        return false;
    }

    private void _confirmSwapPlayers(Player leader) {
        ChangeSides changeSides = new ChangeSides(this, matchModel, this);
        changeSides.init(leader);
        addToDialogStack(changeSides);
    }

    private void swapPlayers(Integer iToastLength) {
        Player pFirst = IBoard.togglePlayer2ScreenElements();
        matchModel.triggerListeners();

        if ( (iToastLength != null) && ( iToastLength == Toast.LENGTH_LONG || iToastLength == Toast.LENGTH_SHORT ) ) {
            String sMsg = getString(R.string.player_names_swapped); // + " (" + pFirst + ")";
            Toast.makeText(this, sMsg, iToastLength).show();
        }
    }

    private void swapDoublePlayers() {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        _swapDoublePlayers(Player.A);
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        _swapDoublePlayers(Player.B);
                        break;
                    case DialogInterface.BUTTON_NEUTRAL:
                        break;
                }
            }
        };

        AlertDialog.Builder ab = getAlertDialogBuilder(this);
        ab.setMessage   (R.string.sb_choose_team)
                .setIcon(R.drawable.ic_action_refresh)
                .setPositiveButton(Player.A.toString()    , dialogClickListener)
                .setNegativeButton(Player.B.toString()    , dialogClickListener)
                .setNeutralButton (android.R.string.cancel, dialogClickListener)
                .show();
    }

    private void _swapDoublePlayers(Player pl) {
        if ( pl == null ) { return; }
        String sPlayerNames = matchModel.getName(pl);
        String[] saNames = sPlayerNames.split("/");
        if ( saNames.length != 2 ) { return; }
        matchModel.setPlayerName(pl, saNames[1] + "/" + saNames[0]);
        String sMsg = getString(R.string.double_player_names_swapped, pl);
        Toast.makeText(this, sMsg, Toast.LENGTH_LONG).show();
    }

	//-------------------------------------------------------------------------
	// Adapter for menu drawer
	//-------------------------------------------------------------------------
    private DrawerLayout drawerLayout;
    private ListView     drawerView;
    //private ActionBarDrawerToggle mDrawerToggle;

    private static final boolean m_bHideDrawerItemsFromOldMenu = false; // TODO: maybe later
    private static final LinkedHashMap<Integer,Integer>  id2String  = new LinkedHashMap<Integer,Integer>();
    private class MenuDrawerAdapter extends BaseAdapter implements ListView.OnItemClickListener, View.OnClickListener, DrawerLayout.DrawerListener {
        private List<Integer>  id2Seq    = new ArrayList<Integer>();
      //private SparseIntArray id2String = new SparseIntArray();
        private SparseIntArray id2Image  = new SparseIntArray();
        private LayoutInflater inflater  = null;

        private MenuDrawerAdapter() {
            inflater = (LayoutInflater)ScoreBoard.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

/*
        id2Seq.add(R.id.uc_new );
            id2Seq.add(R.id.sb_enter_singles_match );
            id2Seq.add(R.id.sb_select_static_match );
            id2Seq.add(R.id.sb_select_feed_match   );
        id2Seq.add(R.id.uc_edit );
            id2Seq.add(R.id.sb_adjust_score        );
            id2Seq.add(R.id.sb_edit_event_or_player);
        id2Seq.add(R.id.uc_show );
            id2Seq.add(R.id.sb_toss                );
            id2Seq.add(R.id.sb_timer               );
            id2Seq.add(R.id.sb_injury_timer        );
        id2Seq.add(R.id.sb_sub_help );
            id2Seq.add(R.id.sb_quick_intro         );
            id2Seq.add(R.id.sb_help                );
            id2Seq.add(R.id.sb_feedback            );
        id2Seq.add(R.id.sb_overflow_submenu );
            id2Seq.add(R.id.sb_stored_matches      );
            id2Seq.add(R.id.sb_settings            );
*/

        startSection(R.string.uc_new   );
            addItem(R.id.sb_enter_singles_match , R.string.sb_new_singles_match    , android.R.drawable.ic_menu_add           );
            addItem(R.id.sb_select_static_match , R.string.sb_select_static_match  ,         R.drawable.ic_action_view_as_list);
            addItem(R.id.sb_select_feed_match   , R.string.sb_select_feed_match    ,         R.drawable.ic_action_web_site    );
            addItem(R.id.sb_enter_doubles_match , R.string.sb_new_doubles_match    , android.R.drawable.ic_menu_add           );
        startSection(R.string.uc_edit   );
            addItem(R.id.sb_clear_score         , R.string.sb_clear_score          ,         R.drawable.ic_action_refresh);
            addItem(R.id.sb_adjust_score        , R.string.sb_adjust_score         , android.R.drawable.ic_menu_edit     );
            addItem(R.id.sb_edit_event_or_player, R.string.sb_edit_event_or_player , android.R.drawable.ic_menu_edit     );
            addItem(R.id.change_match_format    , R.string.pref_MatchFormat        ,         R.drawable.ic_action_mouse  );
        startSection(R.string.uc_show   );
            addItem(R.id.sb_toss                , R.string.sb_cmd_toss             , R.drawable.toss_white          );
            addItem(R.id.sb_timer               , R.string.sb_timer                , R.drawable.timer               );
            addItem(R.id.sb_injury_timer        , R.string.sb_injury_timer         , R.drawable.timer               );
            addItem(R.id.sb_score_details       , R.string.sb_score_details        , R.drawable.ic_action_chart_line);
        startSection(R.string.goto_help );
            addItem(R.id.sb_quick_intro         , R.string.Quick_intro             , android.R.drawable.ic_dialog_info         );
            addItem(R.id.sb_help                , R.string.goto_help               , android.R.drawable.ic_menu_help           );
            addItem(R.id.sb_official_rules      , R.string.sb_official_rules_Squash,android.R.drawable.ic_menu_search          );
            addItem(R.id.sb_live_score          , R.string.Live_Score              ,         R.drawable.ic_action_web_site     );
            addItem(R.id.sb_feedback            , R.string.cmd_feedback            ,         R.drawable.ic_action_import_export);
        startSection(R.string.pref_Other );
            addItem(R.id.sb_settings            , R.string.settings                , R.drawable.ic_action_settings    );
            addItem(R.id.sb_stored_matches      , R.string.sb_stored_matches       , R.drawable.ic_action_view_as_list);
        startSection(R.string.ImportExport_elipses );
            addItem(R.id.cmd_import_matches     , R.string.import_matches          , android.R.drawable.stat_sys_download);
            addItem(R.id.cmd_export_matches     , R.string.export_matches          , android.R.drawable.ic_menu_upload);

            if ( ListUtil.size(id2Seq) == 0 ) {
                id2Seq.addAll(id2String.keySet());
            }
        }

        private void startSection(int iCaptionId) {
            id2String.put(iCaptionId, iCaptionId);
        }
        private void addItem(int iActionId, int iCaptionId, int iImageId) {
            id2String.put(iActionId , iCaptionId);
            if ( iImageId != 0 ) {
                id2Image .put(iActionId , iImageId  );
            }
            if ( m_bHideDrawerItemsFromOldMenu && (mainMenu != null) ) {
                // does not work if menu not yet inflated
                ViewUtil.hideMenuItemForEver(mainMenu, iActionId);
            }
        }

        @Override public int getCount() {
            return id2Seq.size();
        }

        @Override public Object getItem(int position) {
            return id2Seq.get(position);
        }

        @Override public long getItemId(int position) {
            return id2Seq.get(position);
        }

        @Override public View getView(int position, View convertView, ViewGroup parent) {
            View view;

            int iId = id2Seq.get(position);
            Integer iResImage = id2Image.get(iId);
            int iResIdTxt = id2String.get(iId);

            if ( iResIdTxt == iId ) {
                view = inflater.inflate(R.layout.list_item_section, null);
                view.setOnClickListener(null);
                view.setOnLongClickListener(null);
                view.setLongClickable(false);
                final TextView sectionView = (TextView) view.findViewById(R.id.list_item_section_text);
                sectionView.setText(iResIdTxt);
            } else {
                view = inflater.inflate(R.layout.image_item, null /*viewGroupParent*/);
                TextView text = (TextView) view.findViewById(R.id.image_item_text);
                ImageButton img = (ImageButton) view.findViewById(R.id.image_item_image);
                img.setImageResource(iResImage);
                text.setText(iResIdTxt);
    /*
                text.setOnClickListener(this);
    */
                img.setOnClickListener(this);
                img.setTag(iId);
                view.setOnClickListener(this);
                view.setTag(iId);
            }
            return view;
        }

        @Override public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            drawerLayout.closeDrawer(drawerView);
            handleMenuItem(id2Seq.get(position));
        }

        //
        // OnClickListener
        //
        @Override public void onClick(View v) {
            Object tag = v.getTag();
            if ( tag instanceof Integer ) {
                Integer iId = (Integer) tag;
                drawerLayout.closeDrawer(drawerView);
                handleMenuItem(iId);
            }
        }

        //
        // DrawerListener
        //

        @Override public void onDrawerStateChanged(int newState) { }
        @Override public void onDrawerSlide(View drawerView, float slideOffset) { }
        @Override public void onDrawerOpened(View drawerView) {
            //getXActionBar().setTitle(mDrawerTitle);
            //invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
        }
        @Override public void onDrawerClosed(View drawerView) {
            //getXActionBar().setTitle(mTitle);
            //invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
        }
    }
    //-------------------------------------------------------------------------
	// Activity method overwrites
	//-------------------------------------------------------------------------

    public static   ToggleResult  bUseActionBar   = ToggleResult.nothing;
    private boolean bHapticFeedbackPerPoint  = false;
    private boolean bHapticFeedbackOnGameEnd = false;

    /** onCreate() is followed by onstart() onresume(). Also called after orientation change */
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        // one-of correct incorrect default
        if ( Brand.isSquash() ) {
            if ( PreferenceValues.getAppVersionCode(this) == 156 ) {
                int iVersionRunCount = PreferenceValues.getVersionRunCount(this);
                if ( iVersionRunCount < 3 ) {
                    PreferenceValues.setBoolean(PreferenceKeys.swapPlayersOn180DegreesRotationOfDeviceInLandscape, this, false);
                }
            }
        }

        if ( PreferenceValues.isUnbrandedExecutable(this) ) {
            // if we are running as unbranded but one ore more other brand values are uncommented
            if ( PreferenceValues.isBrandTesting(this) ) {
                Brand.brand = PreferenceValues.getOverwriteBrand(this);
                Brand.setBrandPrefs(this);
                Brand.setSportPrefs(this);
                Model mTmp = Brand.getModel();
                File lastMatchFile = getLastMatchFile(this);
                try {
                    boolean bModelRead = mTmp.fromJsonString(lastMatchFile);
                    if ( bModelRead == false ) {
                        // assume it was for another sport
                        Log.w(TAG, "Deleting " + lastMatchFile.getName());
                        lastMatchFile.delete();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            int iRunCount = PreferenceValues.getRunCount(this, PreferenceKeys.OrientationPreference);
            if ( iRunCount <= 1 ) {
                if ( Util.isMyDevice(this) ) {
                    RWValues.setStringIfCurrentlyNotSet(PreferenceKeys.refereeName, this, "Iddo H");
                }
            }
        } else {
            int iRunCount = PreferenceValues.getRunCount(this, PreferenceKeys.OrientationPreference);
            if ( iRunCount <= 1 ) {
                // change the preferences only the first few times the app is used.
                // User might change preferences himself, do not overwrite these
                Brand.setSportPrefs(this); // only once?
            }
        }

        // initialize the country util from json in resources (res folder)
        initCountryList();

        if ( PreferenceValues.useFeedAndPostFunctionality(this) ) {
            Preloader preloader = Preloader.getInstance(this);
        }

        initCasting(this);

        dialogManager = DialogManager.getInstance();

        handleStartedFromOtherApp();

        ViewUtil.setFullScreen(getWindow(), PreferenceValues.showFullScreen(this));

        boolean bOrientationChangeRequested =   initAllowedOrientation(this);

        //if ( bOrientatienChangeRequested ) return; // DO NOT DO THIS

        initShowActionBar();

        if ( bOrientationChangeRequested == false ) {
            boolean bUseLeftDrawer = true;
            if ( bUseLeftDrawer == false ) {
                setContentView(R.layout.percentage); // triggers onContentChanged()
            } else {
                setContentView(R.layout.mainwithmenudrawer);
                drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
                drawerView   = (ListView)     findViewById(R.id.left_drawer);

                MenuDrawerAdapter adapter = new MenuDrawerAdapter();
                drawerView.setAdapter(adapter);
                drawerView.setOnItemClickListener(adapter);
                drawerLayout.addDrawerListener(adapter);

                // displays a small 'arrow to left' on the left of the home icon
                ActionBar actionBar = getXActionBar();
                if ( actionBar != null ) {
                    actionBar.setDisplayHomeAsUpEnabled(true);
                }

                FrameLayout frameLayout = (FrameLayout) findViewById(R.id.content_frame);
                LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View view = inflater.inflate(R.layout.percentage, null);
                frameLayout.addView(view);
            }
        }

        Chronometer.OnChronometerTickListener gameDurationTickListener = null;
        if ( PreferenceValues.autoShowModeActivationDialog(this) ) {
            if ( Brand.isTabletennis() ) {
                gameDurationTickListener = new TTGameDurationTickListener(PreferenceValues.showModeDialogAfterXMins(this));
            }
        }

        Display display = ((WindowManager) this.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        iBoard = new IBoard(matchModel, this, display, (ViewGroup) findViewById(android.R.id.content), gameDurationTickListener);
        Timer.addTimerView(true, getCastTimerView());

        ViewGroup tlGameScores = (ViewGroup) findViewById(R.id.gamescores);
        if ( tlGameScores != null ) {
            tlGameScores.setOnTouchListener    (gamesScoresGestureListener);
        }

        initScoreButtons();

        initServeSideButtons();
        initServeSideButtonListeners();
        iBoard.initTimerButton();
        iBoard.initBranded();
        iBoard.initFieldDivision();

        ViewGroup view   = (ViewGroup) findViewById(android.R.id.content);
        ViewGroup layout = (ViewGroup) view.getChildAt(0); // the relative layout
        while ( (layout != null) &&  (layout instanceof SBRelativeLayout)==false ) {
            layout = (ViewGroup) layout.getChildAt(0);
        }
        if ( (layout != null) && (layout instanceof SBRelativeLayout) ) {
            layout.setOnTouchListener(new TouchBothListener(clickBothListener, longClickBothListener));
        }

        bHapticFeedbackPerPoint  = PreferenceValues.hapticFeedbackPerPoint(ScoreBoard.this);
        bHapticFeedbackOnGameEnd = PreferenceValues.hapticFeedbackOnGameEnd(ScoreBoard.this);

        initColors();
        initCountries();

        initScoreBoard(getLastMatchFile(this));

        initPlayerButtons();

        handleStartupAction();

        rateMeMaybe_init();

        registerNfc();
    }

    private void initCountryList() {
        if ( CountryUtil.isInitialized()==false ) {
            try {
                String sJson = ContentUtil.readRaw(this, R.raw.countries_list_01);
                JSONArray jsCountries = new JSONArray(sJson);
                CountryUtil.init(JsonUtil.toListOfMaps(jsCountries), "iso2", "iso3", "ioc", "fifa", new String[]{"name-" + RWValues.getDeviceLanguage(this), "name"});
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private ShareMatchPrefs m_liveScoreShare = null;

    /** e.g. after settings screen has been entered and closed, matchdetails have been viewed. also after orientation switch (but onrestoreinstance is called first) */
    @Override protected void onResume() {
        super.onResume();

        if ( PreferenceValues.isRestartRequired() ) {
            persist(false);
            //System.exit(0); // todo in android 6 and 7, app does not restart automatically... i think it did in 5 (and 4)
            //doRestart(this);
            return;
        }

        switch ( PreferenceValues.keepScreenOnWhen(this) ) {
            case Always:
                keepScreenOn(true);
                break;
            case Never:
                keepScreenOn(false);
                break;
            case MatchIsInProgress:
                keepScreenOn(matchModel.matchHasEnded() == false && matchModel.isLocked() == false);
                break;
        }

        updateDemoThread(this);

        bHapticFeedbackPerPoint  = PreferenceValues.hapticFeedbackPerPoint(this);
        bHapticFeedbackOnGameEnd = PreferenceValues.hapticFeedbackOnGameEnd(this);
        m_liveScoreShare         = PreferenceValues.isConfiguredForLiveScore(this);

        updateMicrophoneFloatButton();
        updateTimerFloatButton();
        iBoard.updateGameBallMessage();
        iBoard.updateGameAndMatchDurationChronos();
        showShareFloatButton(matchModel.isPossibleGameVictory(), matchModel.matchHasEnded()); // icon may have changed

        initModelListeners();

        initScoreHistory();

        onResumeNFC();

        onResumeURL();

        dialogManager.showNextDialogIfChildActivity();
        if ( timer != null ) {
            // e.g. timer was showing before orientation change. Show it again
            Timer.addTimerView(iBoard.isPresentation(), iBoard);
        }
        timer.removeTimerView(false, NotificationTimerView.class);
        NotificationTimerView.cancelNotification(this); // notification is always just a timer. Just there for switching back to Squore. Use is switching back... so remove notification

        if ( scSequence != null ) {
            scSequence.setActivity(this);
            ShowcaseView showcaseView = scSequence.showItem(0); // passing in zero is specifically for onResume
            if ( showcaseView == null ) {
                // not started yet, presume is 'first run of app': start delay to ensure all views are found with findViewById();
                CountDownTimer countDownTimer = new CountDownTimer(1200, 300) {
                    @Override public void onTick(long millisUntilFinished) { }
                    @Override public void onFinish() {
                        if ( scSequence != null ) {
                            scSequence.start();
                        }
                    }
                };
                countDownTimer.start();
            }
        }

        if ( PreferenceValues.swapPlayersOn180DegreesRotationOfDeviceInLandscape(this) && isLandscape() ) {
            initForSwapPlayersOn180Rotation();
        }

        //Log.d(TAG, "onResume DONE");
    }

    private void keepScreenOn(boolean bOn) {
        ViewUtil.keepScreenOn(getWindow(), bOn);
    }

    private void handleStartedFromOtherApp() {
        // optionally overwrite preferences by using values that 'parent' app passes in
        {
            Intent intent = getIntent();
            Bundle bundle = null;
            if ( intent != null ) {
                bundle = intent.getBundleExtra(PreferenceKeys.class.getSimpleName());
            }
/*
            boolean bTestInvocation = false; // TODO: temp should be false
            if ( bundle == null && bTestInvocation ) {
                List<String> lFixedMatches = PreferenceValues.getMatchList(this);
                if ( ListUtil.isEmpty(lFixedMatches)) {
                    bundle = new Bundle();
                    bundle.putString(PreferenceKeys.matchList.toString(), "|-De Vaart|Bogaarts, Jolan - Van Parys, Matthias|");
                    bundle.putString(PreferenceKeys.StartupAction.toString(), StartupAction.ForceSelectMatch.toString());
                }
            }
*/
            // Get values from the intent/scoreBoard that started this scoreBoard
            if ( bundle != null ) {
                for ( PreferenceKeys key: PreferenceKeys.values() ) {
                    String value = bundle.getString(key.toString());
                    //Log.d(TAG, String.format("Parent app passed in ? %s", key));
                    if ( StringUtil.isNotEmpty(value) ) {
                        Log.d(TAG, String.format("Parent app DID pass in %s = %s", key, value));

                        // for key that are not really preferences... actually store them for usage when app is started 'alone'
                        switch(key) {
                            case additionalPostKeyValuePairs:
                                PreferenceValues.setString(key, this, value);
                                break;
                            case refereeName:
                                final String sCurrentRef = PreferenceValues.getRefereeName(this);
                                if ( StringUtil.isEmpty(sCurrentRef) ) {
                                    PreferenceValues.setString(key, this, value);
                                }
                                PreferenceValues.addStringToList(this, PreferenceKeys.refereeList, 0, value);
                                break;
                        }
                        PreferenceValues.setOverwrite(key, value);
                    }
                }
                PreferenceValues.interpretOverwrites(this);
            }
        }
    }

    private static OnBackPressExitHandler onBackPressHandler = null;
    private void handleBackPressed() {
        if ( scSequence != null ) {
            if (onBackPressHandler == null) {
                onBackPressHandler = new OnBackPressExitHandler();
            }
            if (onBackPressHandler.handle(this, getString(R.string.press_back_again_to_exit_intro))) {
                cancelShowCase();
                RWValues.setNumber(PreferenceKeys.viewedChangelogVersion, ScoreBoard.this, RWValues.getVersionCodeForChangeLogCheck(ScoreBoard.this));
                PreferenceValues.removeOverwrite(PreferenceKeys.showHideButtonOnTimer);
                onBackPressHandler = null;
            }
            return;
        }
        if ( isInDemoMode() ) {
            demoThread.cancelDemoMessage();
        } else {
            if ( (drawerLayout != null) && drawerLayout.isDrawerOpen(drawerView) ) {
                handleMenuItem(android.R.id.home); // will close the drawer
                return;
            }
            BackKeyBehaviour backKeyBehaviour = PreferenceValues.backKeyBehaviour(this);

            // if there is nothing to undo, fall back to 'default'
            if ( backKeyBehaviour.toString().startsWith(BackKeyBehaviour.UndoScore.toString()) ) {
                if ( matchModel.hasStarted() == false ) {
                    backKeyBehaviour = BackKeyBehaviour.PressTwiceToExit;
                }
                if ( matchModel.isLocked() ) {
                    backKeyBehaviour = BackKeyBehaviour.PressTwiceToExit;
                }
            }
            switch (backKeyBehaviour) {
                case PressOnceToExit: {
                    super.onBackPressed();
                    break;
                }
                case PressTwiceToExit: {

                    ActionBar xActionBar = getXActionBar();
                    if ( xActionBar != null) {
                        if ( xActionBar.isShowing() == false ) {
                            xActionBar.show();
                            return;
                        };
                    }
                    if (onBackPressHandler == null) {
                        onBackPressHandler = new OnBackPressExitHandler();
                    }
                    if (onBackPressHandler.handle(this, getString(R.string.press_back_again_to_exit))) {
                        super.onBackPressed();
                        m_bURLReceived = false;
                    }
                    break;
                }
                case UndoScore: {
                    AlertDialog.Builder cfun = getAlertDialogBuilder(this);
                    cfun.setTitle(R.string.uc_undo)
                        .setIcon(R.drawable.ic_action_undo)
                        .setPositiveButton(R.string.cmd_yes, new DialogInterface.OnClickListener() {
                            @Override public void onClick(DialogInterface dialog, int which) {
                                handleMenuItem(R.id.dyn_undo_last);
                            }
                        })
                        .setNeutralButton(R.string.cmd_no, null)
                        .show();
                    break;
                }
                case UndoScoreNoConfirm: {
                    handleMenuItem(R.id.dyn_undo_last);
                    break;
                }
                case ToggleServeSide: {
                    matchModel.changeSide(matchModel.getServer());
                    break;
                }
            }
        }
    }

// This works as well, but something on the volume is still triggered. My phone kept making a noise */
/*
    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ( bUseVolumebuttonsForScoring ) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_VOLUME_UP: {
                    matchModel.changeScore(Player.A);
                    return true;
                }
                case KeyEvent.KEYCODE_VOLUME_DOWN: {
                    matchModel.changeScore(Player.B);
                    return true;
                }
                default: {
                    return super.onKeyDown(keyCode, event);
                }
            }
        }
        return super.onKeyDown(keyCode, event);
    }
*/

    /** using dispatchKeyEvent() seems to work fine: we check on KeyEvent.ACTION_UP deliberately. KeyEvent.ACTION_DOWN is triggered very often if you HOLD the volume button */
    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        int action  = event.getAction();

        if ( keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if ( onVolumePressHandler == null ) {
                onVolumePressHandler = new OnVolumeButtonPressHandler();
            }
            boolean bHandled = onVolumePressHandler.handle(this, keyCode == KeyEvent.KEYCODE_VOLUME_UP, action == KeyEvent.ACTION_UP);
            return bHandled;
        } else {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    if (action == KeyEvent.ACTION_UP) {
                        handleBackPressed();
                        return true;
                    }
                case KeyEvent.KEYCODE_MENU: // Hardware menu buttons are slowly being phased out of android
                    if ( action == KeyEvent.ACTION_UP ) {
                        ToggleResult toggleResult = ScoreBoard.this.toggleActionBar(getXActionBar());
                        if ( toggleResult.equals(ToggleResult.nothing) == false ) {
                            PreferenceValues.setBoolean(PreferenceKeys.showActionBar,this,toggleResult.equals(ToggleResult.setToTrue)?true:false);
                        }
                        return true;
                    }
/*
                case KeyEvent.KEYCODE_HOME: // does not seem to work, at least in android 5.1+
                    return handleHomePressed();
*/
            }
        }

        return super.dispatchKeyEvent(event);
    }

    private OnVolumeButtonPressHandler onVolumePressHandler = null;
    private class OnVolumeButtonPressHandler {
        //private long lastPress = 0L;
        private int iDialogPresentedCnt = 0;

        private boolean handle(final Context context, boolean bVolumeTrueIsUpFalseIsDown, boolean bActionTrueIsUpFalseIsDown) {

            boolean bUseVolumeButtonsForScoring = false;
            VolumeKeysBehaviour volumeKeysBehaviour = PreferenceValues.volumeKeysBehaviour(context);
            switch (volumeKeysBehaviour) {
                case None:
                    break;
                case AdjustScore:
                    bUseVolumeButtonsForScoring = true;
                    break;
                case AdjustScore__ForPortraitOnly:
                    bUseVolumeButtonsForScoring = isPortrait();
                    break;
            }

            if ( bActionTrueIsUpFalseIsDown ) {
                // we only do something for 'up' action. If a user long presses a volume key a lot of 'down' events are triggered
                if ( bUseVolumeButtonsForScoring ) {
                    changeScore(bVolumeTrueIsUpFalseIsDown ? Player.A : Player.B);
                } else {
                    showActivateDialog(context);
                }
            }
            return bUseVolumeButtonsForScoring;
        }

        private void showActivateDialog(final Context context) {
            if ( iDialogPresentedCnt > 1 ) { return; }
            if ( ViewUtil.isLandscapeOrientation(context) ) { return; }

            //long currentTime = System.currentTimeMillis();
            //long lInterval = currentTime - this.lastPress;
            //this.lastPress = currentTime;
            //if ( lInterval > 1500L ) { return; }

            // user pressed dialog button short after one another: present choice to turn on entering score using volume buttons
            AlertDialog.Builder choose = getAlertDialogBuilder(context);
            choose.setMessage(R.string.pref_VolumeKeysBehaviour_question)
                    .setIcon(R.drawable.dummy)
                    .setPositiveButton(R.string.cmd_yes, new DialogInterface.OnClickListener() {
                        @Override public void onClick(DialogInterface dialog, int which) {
                            PreferenceValues.setEnum(PreferenceKeys.VolumeKeysBehaviour, context, VolumeKeysBehaviour.AdjustScore__ForPortraitOnly);
                        }
                    })
                    .setNeutralButton(R.string.cmd_no, new DialogInterface.OnClickListener() {
                        @Override public void onClick(DialogInterface dialog, int which) {
                            iDialogPresentedCnt += 100; // ensure it is not presented again
                        }
                    })
                    .show();

            iDialogPresentedCnt++;
        }
    }

    /** e.g. when match details is closed */
    @Override protected void onRestart() {
        super.onRestart();    //e.g. after preferences change or match selection
        invalidateOptionsMenu(); // this is to trigger showing/hiding menu items base on e.g. specified URLs

        if ( matchModel.isDirty() ) { // set by Settings
            persist(false);

            initColors();
            castColors(this.mColors);

            initAllowedOrientation(this);
            //initShowActionBar(); // does not work: requestFeature() must be called before adding content
            initActionBarSettings(menuItemsWithOrWithoutText);

            initPlayerButtons();
            initCountries();
            initScoreButtons();
            initServeSideButtonListeners();
            initServeSideButtons();
            iBoard.initTimerButton();
            iBoard.initBranded();
            iBoard.initFieldDivision();
        }
    }

    // ------------------------------------------------------
    // Swap players on 180 degrees
    // ------------------------------------------------------
    private class SwapPlayersOn180Listener extends OrientationEventListener {
        private Integer m_previousDegrees = null;

        SwapPlayersOn180Listener(Context context) {
            super(context , SensorManager.SENSOR_DELAY_NORMAL);
        }

        @Override public void onOrientationChanged(int iDegrees) {
            // returns the number of degrees the device is rotated in comparison to its default orientation
            // for phones default portrait means orientation is near 0
            // for tables default landscape means orientation is near 0
            // a value of -1 is return if the phone is more or less horizontal
            // these values still come in nicely even if you set your screen to e.g. landscape only
            if ( iDegrees == ORIENTATION_UNKNOWN ) { return; }
            if ( isPortrait()                    ) { return; } // no use for this functionality in portrait orientation

            float f0To4 = (float) iDegrees / 90;
            int   i0To4 = Math.round(f0To4) % 4;
            //Log.v(TAG, "Orientation changed to " + i0To4  + " , " + f0To4  + " , " + iDegrees);
            if ( m_previousDegrees != null ) {
                int iPrev = m_previousDegrees;
                if ( Math.abs(iPrev - i0To4) == 2 ) {
                    // swapping
                    // - from 0 to 2 or back, or
                    // - from 1 to 3 or back
                    Log.i(TAG, "Swap because going from " + iPrev + " to " + i0To4);
                    swapPlayers(null /*Toast.LENGTH_SHORT*/);
                    m_previousDegrees = i0To4;
                }
            } else {
                m_previousDegrees = i0To4;
            }
        }

    }
    private OrientationEventListener m_orientationListener = null;
    private void initForSwapPlayersOn180Rotation() {
        if ( m_orientationListener != null ) { return; }

        m_orientationListener = new SwapPlayersOn180Listener(this);
        //Log.d(TAG, "Created " + m_orientationListener);

        if ( m_orientationListener.canDetectOrientation() == true ) {
            //Log.d(TAG, "Can detect orientation");
            m_orientationListener.enable(); // registers the listener
        } else {
            Log.w(TAG, "Cannot detect orientation");
            m_orientationListener.disable();// unregisters the listener
        }
    }
    private void cleanUpForOrientationInDegreesChanges() {
        if ( m_orientationListener != null ) {
            m_orientationListener.disable();
            Log.i(TAG, "Disabled " + m_orientationListener);
            m_orientationListener = null;
        }
    }

    // ------------------------------------------------------
    // Showcase
    // ------------------------------------------------------
    private static ShowcaseSequence scSequence = null; // static so it still is in progress after rotate
    private void startShowCase(boolean bFromMenu) {
        //if ( scSequence != null ) { return; }
        int iTxtSize = getResources().getInteger(R.integer.TextSizeShowCase);

        ShowcaseConfig config = new ShowcaseConfig();
        config.setDelay(500); // time between each showcase view after 'got it' is clicked
        //config.setDismissTextColor(mColors.get(ColorPrefs.ColorTarget.playerButtonTextColor));
        //config.setContentTextColor(mColors.get(ColorPrefs.ColorTarget.playerButtonTextColor));
        //config.setMaskColor(mColors.get(ColorPrefs.ColorTarget.playerButtonBackgroundColor));
        //config.setShape(new CircleShape());
        config.setShapePadding(iTxtSize);
        config.setFadeDuration(300);
        config.setDismissText(getString(R.string.scv_got_it));
        //config.setContentTextSize(iTxtSize);

        int iTxtSizeShowCase = ViewUtil.getScreenHeightWidthMinimum(this) / 24;
        config.setContentTextSizePx(iTxtSizeShowCase);
        config.setDismissOnTouch(false, Direction.W); // only allow forward swiping, for now this makes touches on e.g. serve buttons not clickable during the showcase

        scSequence = new ShowcaseSequence(this, mainMenu);
        scSequence.setConfig(config);
        int iSomeActionBarId = android.R.id.home; // does not work for AppCompatActivity (16908332)
        if ( this instanceof AppCompatActivity ) {
            iSomeActionBarId = R.id.sb_overflow_submenu; // 2131689712
        }
        if ( bFromMenu == false ) {
            // typically for a first install via StartAction
            addSequenceItem(iSomeActionBarId, R.string.scv_first_run, ShowcaseView.ShapeType.None).setDismissText(R.string.cmd_ok);
        }
        addSequenceItemOval(R.id.float_toss              , R.string.scv_toss                            );
        addSequenceItemOval(R.id.btn_side1               , R.string.scv_side_buttons_Squash             );
        addSequenceItemRect(R.id.btn_score1              , R.string.scv_big_buttons                     );
        addSequenceItemRect(R.id.dyn_undo_last           , R.string.scv_undo_last                       );
        addSequenceItemOval(R.id.float_timer             , R.string.scv_timer_button_Squash             );
        if ( Brand.isSquash() ) {
        addSequenceItemOval(R.id.sb_official_announcement, R.string.scv_announcement_button_Squash); // not for Racketlon
        }
        addSequenceItemOval(R.id.btn_side2               , R.string.scv_shirt_color_Squash              );
        addSequenceItemOval(R.id.gamescores              , R.string.scv_game_scores_Squash              );
        addSequenceItemRect(R.id.txt_player1             , R.string.scv_player_buttons_appeall_Squash   );// not for Racketlon
        addSequenceItemRect(R.id.txt_player2             , R.string.scv_player_buttons_misconduct_Squash);// not for Racketlon
        addSequenceItemRect(R.id.scorehistorytable       , R.string.scv_old_fashioned_scoring_Squash    );// not for Racketlon

        //addSequenceItem(R.id.dyn_score_details       , R.string.scv_score_details); //.setDelay(1000); // a little extra delay to allow share button to appear
        addSequenceItemOval(R.id.float_match_share       , R.string.scv_share_button); //.setDelay(1000); // a little extra delay to allow share button to appear

        addSequenceItemOval(R.id.sb_overflow_submenu     , R.string.scv_overflow_submenu_Squash);
        addSequenceItemOval(R.id.gamescores              , R.string.scv_toggle_action_bar);
        if ( bFromMenu ) {
            addSequenceItemOval(iSomeActionBarId, R.string.scv_few_more_gui_hints_Squash);
        }
        addSequenceItemOval(iSomeActionBarId, R.string.scv_few_more_good_to_knows);
        addSequenceItemOval(R.id.float_new_match         , R.string.scv_new_match_button_Squash).setDismissText(R.string.cmd_ok);

        scSequence.setOnItemChangeListener(showcaseListener);
        if ( bFromMenu ) {
            scSequence.start(); // onResume will also start the actual displaying (e.g. for after screen rotation, or as startup action on first run)
        }
    }
    private ShowcaseSequence addSequenceItemOval(int iViewId, int iResid) {
        return addSequenceItem(iViewId, iResid, ShowcaseView.ShapeType.Oval);
    }
    private ShowcaseSequence addSequenceItemRect(int iViewId, int iResid) {
        return addSequenceItem(iViewId, iResid, ShowcaseView.ShapeType.Rectangle);
    }
    private ShowcaseSequence addSequenceItem(int iViewId, int iResid, ShowcaseView.ShapeType shapeType) {
        int iNewResId = PreferenceValues.getSportTypeSpecificResId(this, iResid);
        if ( iNewResId > 0 ) {
            iResid = iNewResId;
        } else {
            // ending with _Squash but no e.g. _Racketlon equivalent. Skip the item
            return scSequence;
        }
        return scSequence.addSequenceItem(iViewId, iResid, shapeType);
    }

    private ShowcaseSequence.OnSequenceItemChangeListener showcaseListener = new ShowcaseSequence.OnSequenceItemChangeListener() {
        @Override public void beforeShow(int position, int iDeltaNotUsed, int iViewId, int iResId) {
            Log.d(TAG, "beforeShow:: highlight view " + getResourceEntryName(iViewId) + ", display text " + getResourceEntryName(iResId));

            if ( /*(position == 1) &&*/ (iViewId == R.id.float_toss) ) {
                PreferenceValues.setOverwrite(PreferenceKeys.useTossFeature                     , Feature.Suggest.toString());
                PreferenceValues.setOverwrite(PreferenceKeys.useTimersFeature                   , Feature.Suggest.toString());
                PreferenceValues.setOverwrite(PreferenceKeys.autoSuggestToPostResult            , "false");
                PreferenceValues.setOverwrite(PreferenceKeys.showDetailsAtEndOfGameAutomatically, "false");
                restartScore();
                if ( Brand.isRacketlon() ) {
                    PreferenceValues.setOverwrite(PreferenceKeys.useOfficialAnnouncementsFeature, Feature.DoNotUse.toString());
                    matchModel.setNrOfPointsToWinGame(21);
                    matchModel.setNrOfGamesToWinMatch(0);
                    matchModel.setPlayerNames("Ricky", "Lonny");
                } else if ( Brand.isTabletennis() ) {
                    PreferenceValues.setOverwrite(PreferenceKeys.useOfficialAnnouncementsFeature, Feature.DoNotUse.toString());
                    matchModel.setNrOfPointsToWinGame(11);
                    matchModel.setNrOfGamesToWinMatch(4);
                    matchModel.setPlayerNames("Tabby", "Tenny");
                } else {
                    PreferenceValues.setOverwrite(PreferenceKeys.useOfficialAnnouncementsFeature, Feature.Suggest.toString());
                    matchModel.setNrOfPointsToWinGame(11);
                    matchModel.setNrOfGamesToWinMatch(3);
                    matchModel.setPlayerNames("Shaun", "Casey");
                }
                matchModel.setPlayerAvatar(Player.A, null);
                matchModel.setPlayerAvatar(Player.B, null);
            }
            if ( position == 0 ) {
                PreferenceValues.setOverwrite(PreferenceKeys.showMatchDurationChronoOn   , ShowOnScreen.OnChromeCast.toString());
                PreferenceValues.setOverwrite(PreferenceKeys.showLastGameDurationChronoOn, ShowOnScreen.OnChromeCast.toString());
                iBoard.updateGameAndMatchDurationChronos();
            }
            final int nrOfPointsToWinGame = matchModel.getNrOfPointsToWinGame();
            switch (iViewId) {
                case R.id.btn_side1:
                    matchModel.changeScore(Player.B);
                    matchModel.changeScore(Player.A); // ensure button for player A is highlighted for squash
                    if ( Brand.isNotSquash() ) {
                        while ( matchModel.getServer().equals(Player.A) == false ) {
                            matchModel.changeScore(Player.A); // ensure button for player A is highlighted
                        }
                    }
                    break;
                case R.id.btn_score1:
                    if ( matchModel.getMaxScore() < 3 ) {
                        matchModel.changeScore(Player.A);
                        matchModel.changeScore(Player.A);
                        matchModel.changeScore(Player.B);
                        matchModel.changeScore(Player.B);
                        matchModel.changeScore(Player.B);
                        matchModel.changeScore(Player.B);
                    }
                    break;
                case R.id.float_timer:
                    PreferenceValues.setOverwrite(PreferenceKeys.showHideButtonOnTimer, false);
                    if ( matchModel.isPossibleGameVictory() == false ) {
                        matchModel.setGameScore_Json(0, nrOfPointsToWinGame, nrOfPointsToWinGame - 4, 5);
                        matchModel.endGame();
                    }
                    break;
                case R.id.sb_official_announcement:
                    matchModel.setGameScore_Json(1, nrOfPointsToWinGame -1, nrOfPointsToWinGame +1, 6);
                    matchModel.endGame();
                    break;
                case R.id.gamescores:
                    break;
                case R.id.txt_player1:
                    matchModel.changeScore(Player.B);
                    matchModel.changeScore(Player.A);
                    if ( matchModel.getMaxScore() < 3 ) {
                        IBoard.setBlockToasts(true);
                        matchModel.recordAppealAndCall(Player.A, Call.ST);
                        matchModel.changeScore(Player.B);
                        matchModel.recordAppealAndCall(Player.B, Call.YL);
                        IBoard.setBlockToasts(false);
                    }
                    break;
                case R.id.dyn_score_details:
                case R.id.float_match_share:
                    if ( Brand.isNotSquash() ) {
                        // trigger model changes that are not triggered by user step (sb_official_announcement), because some show case screens are skipped for e.g. Racketlon
                        matchModel.setGameScore_Json(1, nrOfPointsToWinGame -1, nrOfPointsToWinGame +1, 6);
                        matchModel.endGame();
                    }
                    if ( matchModel.matchHasEnded() == false) {
                        IBoard.setBlockToasts(true);
                        matchModel.setGameScore_Json(2, nrOfPointsToWinGame,  nrOfPointsToWinGame -5, 5);
                        if ( Brand.isRacketlon() ) {
                            // add a score that ends the racketlon match by points
                            matchModel.setGameScore_Json(3, 15, 11, 8);
                        } else if ( Brand.isTabletennis() ) {
                            // add a score that ends the tabletennis match
                            matchModel.setGameScore_Json(3, nrOfPointsToWinGame+2, nrOfPointsToWinGame, 8);
                            matchModel.setGameScore_Json(4, nrOfPointsToWinGame, nrOfPointsToWinGame-4, 7);
                        } else {
                            matchModel.setGameScore_Json(3, nrOfPointsToWinGame +2, nrOfPointsToWinGame, 8);
                        }
                        matchModel.endGame();
                        showShareFloatButton(true, true);
                        IBoard.setBlockToasts(false);
                        matchModel.setLockState(LockState.LockedEndOfMatch);
                    }
                    break;
                case R.id.sb_overflow_submenu:
                case R.id.dyn_undo_last:
                case android.R.id.home:
                    ActionBar actionBar = getXActionBar();
                    if ( actionBar != null && actionBar.isShowing() == false ) {
                        toggleActionBar(actionBar);
                    }
                    break;
                case R.id.float_new_match:
                    break;
            }
        }

        @Override public void onDismiss(ShowcaseView itemView, int position, ShowcaseView.DismissReason reason) {
            IBoard.setBlockToasts(false);
            if ( reason == null || reason.equals(ShowcaseView.DismissReason.SkipSequence) ) {
                cancelShowCase();
            }
        }
    };

    private String getResourceEntryName(int iViewId) {
        try {
            return getResources().getResourceEntryName(iViewId) + " " + iViewId;
        } catch (Resources.NotFoundException e) {
            Log.w(TAG, "No resource entry found for " + iViewId);
            //e.printStackTrace();
        }
        return "-?- " + iViewId;
    }

    private void cancelShowCase() {
        if ( scSequence != null ) {
            scSequence.stop();
            scSequence = null;
            RWValues.setNumber(PreferenceKeys.viewedChangelogVersion, this, RWValues.getVersionCodeForChangeLogCheck(this));
            PreferenceValues.clearOverwrites();
        }
    }

    // ------------------------------------------------------
    // StartUp action
    // ------------------------------------------------------
    private static int startupActionCounter = 0;
    private void handleStartupAction() {

        startupActionCounter++;
        if ( startupActionCounter > 1 ) return;
        StartupAction startupAction = PreferenceValues.getStartupAction(this);
        switch (startupAction) {
          //case SelectMatch    :
            case StartNewMatch    :
                if ( (matchModel == null) || matchModel.isLocked() || ( matchModel.hasStarted() == false ) || matchModel.matchHasEnded() ) {
                    handleMenuItem(startupAction.getMenuId());
                } else {
                    //Log.d(TAG, "Not starting select match because last one is still in progress");
                }
                break;
            case None           :
                break;
            case QuickIntro: // fall through
            case ChangeLog:
                // first install or upgrade
                List<File> lAllMatchFiles = PreviousMatchSelector.getPreviousMatchFiles(this);
                if ( ListUtil.size(lAllMatchFiles) == 0 ) {
                    createPreviousMatchesFromRaw();
                }
                // fall through
            default:
                try {
                    handleMenuItem(startupAction.getMenuId(), startupAction);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    private void addRecentFeedURLs() {
        final boolean bMakeActive = PreferenceValues.getMatchesFeedURLUnchanged();
        // TODO: trigger this code e.g. weekly somehow
        for(int i=1; i<=3; i++ ) {
            if ( Brand.isSquash() ) {
                PreferenceValues.addOrReplaceNewFeedURL(this, "Demo PSA Matches " + i, "feed/psa.php?nr=" + i, null, null, false, bMakeActive);
            }
            if ( Brand.isRacketlon() ) {
                PreferenceValues.addOrReplaceNewFeedURL(this, "FIR Tournament " + i  , "feed/fir.tournamentsoftware.php?nr=" + i, "feed/fir.tournamentsoftware.php?pm=players&nr=" + i, null, false, bMakeActive);
            }
            // Squash, racketlon and Table Tennis. URL itself knows what sport based on subdomain
            PreferenceValues.addOrReplaceNewFeedURL(this, "TS " + Brand.getSport() + " " + i, "feed/tournamentsoftware.php?nr=" + i, "feed/tournamentsoftware.php?pm=players&nr=" + i, null, false, bMakeActive);
        }
    }

    // ------------------------------------------------------
    // Colors
    // ------------------------------------------------------
    private Map<ColorPrefs.ColorTarget, Integer> mColors = new HashMap<ColorPrefs.ColorTarget, Integer>();
    public Map<ColorPrefs.ColorTarget, Integer> getColors() {
        if ( MapUtil.isEmpty(mColors) ) {
            initColors();
        }
        return mColors;
    }

    private IBoard iBoard = null;

    private void initCountries() {
        if ( matchModel == null ) {return; }
        for(Player p: Model.getPlayers()) {
            iBoard.updatePlayerCountry(p, matchModel.getCountry(p));
            iBoard.updatePlayerClub   (p, matchModel.getClub   (p));
        }
    }
    private void initColors()
    {
        Map<ColorPrefs.ColorTarget, Integer> mColors = ColorPrefs.getTarget2colorMapping(this);
        if ( MapUtil.isEmpty(mColors) ) {
            ColorPrefs.initDefaultColors(mColors);
        }
        this.mColors = mColors;

        iBoard.initColors(mColors);

        // there is a pretty dark color, for ecstatic reasons, make actionbar the same color
        setActiorBarBGColor(mColors.get(ColorPrefs.ColorTarget.actionBarBackgroundColor));

        initFloaterColors();
    }

    private void initFloaterColors() {
        if ( speakButton != null ) {
            speakButton.setColor(mColors.get(ColorPrefs.ColorTarget.speakButtonBackgroundColor));
        }
        if ( tossButton != null ) {
            tossButton.setColor(mColors.get(ColorPrefs.ColorTarget.tossButtonBackgroundColor));
        }
        if ( timerButton != null ) {
            timerButton.setColor(mColors.get(ColorPrefs.ColorTarget.timerButtonBackgroundColor));
        }
        if ( newMatchButton != null ) {
            ColorPrefs.ColorTarget color = isLandscape()? ColorPrefs.ColorTarget.playerButtonBackgroundColor : ColorPrefs.ColorTarget.scoreButtonBackgroundColor;
            newMatchButton.setColor(mColors.get(color));
        }
        if ( shareButton != null ) {
            shareButton.setColor(mColors.get(ColorPrefs.ColorTarget.shareButtonBackgroundColor));
        }

    }
    // ------------------------------------------------------
    // refresh GUI elements
    // ------------------------------------------------------
    private void initShowActionBar() {
        if ( bUseActionBar == null || bUseActionBar.equals(ToggleResult.nothing) ) {
            bUseActionBar = PreferenceValues.showActionBar(this)? ToggleResult.setToTrue : ToggleResult.setToFalse;
        }
        final ActionBar actionBar = getXActionBar();
        if ( actionBar != null ) {
            switch (bUseActionBar) {
                case setToTrue:
                    //final Point displayPoint = ViewUtil.getDisplayPoint(this);
                    //if ( displayPoint.x > 320 && displayPoint.y > 320 ) {
                    actionBar.show();
                    //if ( requestWindowFeature(Window.FEATURE_ACTION_BAR) ) {
                    //}
                    //} else {
                    // presume android wear
                    //}
                    break;
                case setToFalse:
                    actionBar.hide();
                    break;
            }
        }
        setHomeButtonEnabledOnActionBar();
    }

    public static boolean initAllowedOrientation(Activity activity) {
        return ViewUtil.initAllowedOrientation(activity, PreferenceValues.getOrientationPreference(activity));
    }

    /**
     * Invoked:
     * - onCreate(LAST.db)
     * - clearScore(null) / restartScore(null)
     * - onActivityResult(null) (for newly constructed match)
     * - onActivityResult(fFile) (for stored matches)
     */
    void initScoreBoard(File fJson) {
        Model previous = matchModel;

        if ( fJson == null || matchModel == null ) { // 20140315: invoked from onCreate() after e.g. a dialog or screen orientation switch
            matchModel = null;

            initMatchModel(fJson);
        }

        if ( (previous != null) && (fJson == null) ) {
            // use player names from previous
            String[] playerNames = previous.getPlayerNames(true, false);
            setPlayerNames(playerNames);
            for(Player player: Model.getPlayers()) {
                matchModel.setPlayerCountry(player, previous.getCountry(player));
                matchModel.setPlayerClub   (player, previous.getClub   (player));
                matchModel.setPlayerAvatar (player, previous.getAvatar (player));
            }
            // use event from previous
            matchModel.setEvent               (previous.getEventName(), previous.getEventDivision(), previous.getEventRound(), previous.getEventLocation());
            matchModel.setCourt               (previous.getCourt());

            matchModel.setNrOfPointsToWinGame (previous.getNrOfPointsToWinGame());
            matchModel.setNrOfGamesToWinMatch (previous.getNrOfGamesToWinMatch());
            matchModel.setNrOfServesPerPlayer (previous.getNrOfServesPerPlayer());
            matchModel.setEnglishScoring      (previous.isEnglishScoring      ());
            matchModel.setTiebreakFormat      (previous.getTiebreakFormat     ());
            matchModel.setHandicapFormat      (previous.getHandicapFormat     ());
            matchModel.setSource              (previous.getSource() , previous.getSourceID() );
            matchModel.setReferees            (previous.getReferee(), previous.getMarker());
/*
            for ( Player p: Model.getPlayers() ) {
                matchModel.setGameStartScoreOffset(p, matchModel.getGameStartScoreOffset(p));
            }
*/
            if ( Brand.isSquash() ) {
                ((SquashModel)matchModel).setDoublesServeSequence(((SquashModel)previous).getDoubleServeSequence());
            } else if (Brand.isRacketlon() ) {
                ((RacketlonModel)matchModel).setDisciplines(((RacketlonModel)previous).getDisciplines());
            }
        }
        if ( (previous != null) && (previous != matchModel) ) {
            matchModel.registerListeners(previous); // mainly for listeners for in progress ChromeCast...
        } else {
            bInitializedModelListeners = false;
            initModelListeners();
        }

        if ( fJson == null ) {
            triggerEvent(SBEvent.newMatchStarted, null);
        }

        updateDemoThread(this);
    }

    private void initMatchModel(File fJson) {
        if ( matchModel == null ) {
            matchModel = Brand.getModel();
            iBoard.setModel(matchModel);

            setModelForCast(matchModel);

            matchModel.setNrOfPointsToWinGame(PreferenceValues.numberOfPointsToWinGame(this));
            matchModel.setNrOfGamesToWinMatch(PreferenceValues.numberOfGamesToWinMatch(this));
            matchModel.setNrOfServesPerPlayer(PreferenceValues.numberOfServesPerPlayer(this));
            matchModel.setEnglishScoring     (PreferenceValues.useHandInHandOutScoring(this));
            matchModel.setTiebreakFormat     (PreferenceValues.getTiebreakFormat      (this));
            matchModel.setHandicapFormat     (PreferenceValues.getHandicapFormat      (this));
            matchModel.setReferees           (PreferenceValues.getRefereeName         (this), PreferenceValues.getMarkerName(this));

            if ( fJson != null && fJson.exists() ) {
                try {
                    matchModel.fromJsonString(fJson);
                    if ( PreferenceValues.lockMatchMV(this).contains(AutoLockContext.WhenMatchIsUnchangeForX_Minutes)) {
                        final int iMinutes = PreferenceValues.numberOfMinutesAfterWhichToLockMatch(this);
                        if ( iMinutes > 0 ) {
/*
sed 's~20170104~20170103~g' LAST.sb > LAST.sb.NEW
cat LAST.sb.NEW > LAST.sb
touch -t 01030000 LAST.sb
*/
                            matchModel.lockIfUnchangedFor(iMinutes);
                        }
                    }
                } catch (Exception e) {
                    Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                    return;
                }
            } else {
                String[] saPlayers = { getString(R.string.lbl_player) + " A", getString(R.string.lbl_player) + " B"};
                setPlayerNames(saPlayers);
            }
            bGameEndingHasBeenCancelledThisGame = false;
        }
    }

    /** install listeners */
    private void initScoreButtons() {
        for( Player player: Model.getPlayers() ) {
            Integer iId = IBoard.m_player2scoreId.get(player);
            if ( iId == null ) { continue; }
            int id = iId.intValue();
            TextView btn = ( TextView ) findViewById(id);
            if ( btn == null) { continue; }

            btn.setOnTouchListener(scoreButtonGestureListener);
        }
    }

    /** install listeners */
    private void initScoreHistory() {
        GameHistoryView gameHistoryView = (GameHistoryView) findViewById(R.id.scorehistorytable);
        if ( gameHistoryView == null ) { return; }
        if ( PreferenceValues.showScoringHistoryInMainScreen(this, false) == false ) {
            gameHistoryView.setVisibility(View.GONE);
        } else {
            gameHistoryView.setVisibility(View.VISIBLE);
            if ( bUseActionBar != ToggleResult.setToTrue ) {
                gameHistoryView.setOnClickListener(currentGameScoreListener);
            }
        }
    }

    /** install listeners */
    private void initPlayerButtons() {
        for( Player player : Model.getPlayers() ) {
            int id = IBoard.m_player2nameId.get(player);
            View view = findViewById(id);
            if ( view == null ) { continue; }

            if ( view instanceof PlayersButton) {
                PlayersButton pb = (PlayersButton) view;
                //pb.setTextSize(Preferences.TEXTSIZE_UNIT, textSize);
                //pb.setServer(matchModel.getNextDoubleServe(player), matchModel.getNextServeSide(player), matchModel.isLastPointHandout());  //TODO: not here?
                pb.setServeSideButtonListener(serveButtonGestureListener);
                pb.setOnTouchListener(namesButtonGestureListener);

                if ( matchModel.isDoubles() ) {
                    Integer iId = IBoard.m_player2serverSideId.get(player);
                    if ( iId == null ) { continue; }
                    View btn = ( View ) findViewById(iId);
                    btn.setVisibility(View.INVISIBLE); // do not use GONE or relative layout is screwed up
                }
            } else {
                view.setOnTouchListener(namesButtonGestureListener);
            }
        }
    }

    /** install listeners */
    private void initServeSideButtonListeners() {
        for( Player player: Model.getPlayers() ) {
            Integer iId = IBoard.m_player2serverSideId.get(player);
            if ( iId == null ) { continue; }
            View btn = ( View ) findViewById(iId);
            if ( btn == null ) { continue; }
            btn.setOnTouchListener(serveButtonGestureListener);
        }

    }
    private void initServeSideButtons() {
        //final int textSize = PreferenceValues.getServeSideTextSize(this);
        for( Player player: Model.getPlayers() ) {
            Integer iId = IBoard.m_player2serverSideId.get(player);
            if ( iId == null ) { continue; }
            View btn = ( View ) findViewById(iId);
            if ( btn == null ) { continue; }

            // do not show for doubles: serve side buttons shown directly next to player names
            boolean bHide = (matchModel != null) && matchModel.isDoubles();
            btn.setVisibility(bHide ?View.INVISIBLE:View.VISIBLE); // do not use GONE or relative layout is screwed up
        }
    }

    private ResultPoster initResultPoster() {
        String sPostURL = PreferenceValues.getPostResultToURL(this);
        sPostURL = URLFeedTask.prefixWithBaseIfRequired(sPostURL);

        String sUrlName = PreferenceValues.getFeedPostName(this);
        final Authentication authentication = PreferenceValues.getFeedPostAuthentication(this);
        PostDataPreference postDataPreference = PreferenceValues.getFeedPostDataPreference(this);
        if ( StringUtil.isNotEmpty(sPostURL) ) {
            return new ResultPoster(sPostURL, sUrlName, postDataPreference, null, authentication);
        }
        return null;
    }

    private void initActionBarSettings(MenuItem[] menuitems) {
        if ( menuitems == null ) { return; }
        //this.bUseActionBar = PreferenceValues.showActionBar(this);
        if ( this.bUseActionBar != ToggleResult.setToTrue ) { return; }
        boolean bShowTextInActionBar = PreferenceValues.showTextInActionBar(this);
        for( MenuItem menuItem: menuitems ) {
            if ( menuItem == null ) { continue; }
            if ( bShowTextInActionBar ) {
                menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS + MenuItem.SHOW_AS_ACTION_WITH_TEXT);
            } else {
                menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            }
        }
    }

    // ----------------------------------------------------
    // --------------- Persistence ------------------------
    // ----------------------------------------------------

    /**  is called when the user receives an event like a call or a text message or simply leaves the app himself, when onPause() is called the Activity may be partially or completely hidden. */
    @Override protected void onPause() {
        super.onPause(); // onstop, ondestroy oncontentchanged
        persist(false);
        onNFCPause();
/*
        if ( baseDialog instanceof TwoTimerView ) {
            Log.w(TAG, "onPause: A timer is running");
            TwoTimerView ttv = (TwoTimerView) baseDialog;
            ttv.markViewPaused();
        }
*/
    }
    /** also called after rotation */
    @Override protected void onStart() {
        super.onStart();
        startCast();
        if ( (matchModel != null) && matchModel.hasStarted() == false ) {
            int iMatchStartedXSecsAgo = DateUtil.convertTo(System.currentTimeMillis() - matchModel.getMatchStart(), Calendar.SECOND);
            if (iMatchStartedXSecsAgo > 120 ) {
                matchModel.timestampStartOfGame(GameTiming.ChangedBy.StartAndEndStillEqual);
            }
        }
    }

    /** also invoked if child scoreBoard is activated */
    @Override protected void onStop() {
        stopCast();
        super.onStop();
        persist(false);
    }

    /** is called when the Activity is being destroyed either by the system, or by the user, say by hitting back, until the app exits. But also if child scoreBoard is created?! */
    @Override protected void onDestroy() {
        super.onDestroy();
        persist(true);
        MatchTabbed.persist(this);
        ArchiveTabbed.persist(this);

        Log.d(TAG, "XActivity.status: " + XActivity.status);
        boolean bChangeOrientation = OrientationStatus.ChangingOrientation.equals(XActivity.status);
        if ( /*(bChangeOrientation == false) &&*/ (timer != null) && timer.getSecondsLeft() > 5 ) {
            m_notificationTimerView = new NotificationTimerView(this);
            timer.addTimerView(false, m_notificationTimerView);
        }
    }

    private NotificationTimerView m_notificationTimerView = null;

    public static File getLastMatchFile(Context context) {
        File file = new File(PreviousMatchSelector.getArchiveDir(context), "LAST." + Brand.getSport() + ".sb");
        if ( file.exists() == false ) {
            File fPrevVersion = new File(PreviousMatchSelector.getArchiveDir(context), "LAST.sb");
            if ( fPrevVersion.exists() ) {
                fPrevVersion.renameTo(file);
            }
        }
        return file;
    }

    public void persist(boolean bDestroyModel) {
        try {
            if (matchModel == null) {
                return;
            }
            if (matchModel.isDirty() == false) {
                return;
            }

            String sJson = matchModel.toJsonString(this); // for 5 games match around 1400 characters
            //Log.d(TAG, "persist:" + sJson);
            File fLastMatch = getLastMatchFile(this);
            FileUtil.writeTo(fLastMatch, sJson);

            // save named version only if it has progressed a little already
            // store name only when at least a game has been played so that 'restarting' a named games does not overwrite the stored result
            if ( PreferenceValues.saveMatchesForLaterUsage(this) ) {
                storeAsPrevious(this, sJson, matchModel, false);
            }
            if ( bDestroyModel ) {
                matchModel = null;
            } else {
                matchModel.setClean();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static File storeAsPrevious(Context context, Model matchModel, boolean bForceStore) throws IOException {
        return storeAsPrevious(context, null, matchModel, bForceStore);
    }
    private static File storeAsPrevious(Context context, String sJson, Model matchModel, boolean bForceStore) throws IOException {
        if ( matchModel == null ) {
            matchModel = Brand.getModel();
            if ( sJson != null ) {
                matchModel.fromJsonString(sJson);
            }
        }
        if ( sJson == null ) {
            sJson = matchModel.toJsonString(context);
        }
        File fStore = matchModel.getStoreAs(PreviousMatchSelector.getArchiveDir(context));
        boolean bAtLeastOneGameFinished = matchModel.getNrOfFinishedGames() > 0;

        Feature continueRecentMatch = PreferenceValues.continueRecentMatch(context);
        boolean bPossiblyContinueThisMatchAsRecent = ( (continueRecentMatch != Feature.DoNotUse) && StaticMatchSelector.matchIsFrom(matchModel.getSource()));

        if ( bForceStore || bAtLeastOneGameFinished || bPossiblyContinueThisMatchAsRecent ) {
            FileUtil.writeTo(fStore, sJson);
        }
        return fStore;
    }

    // ----------------------------------------------------
    // --------------- restart ------------------------
    // ----------------------------------------------------

    /** Does not seem to work so well if app started for debuging. But when started from home screen it seems to work fine (android 7) */
    private static void doRestart(Context c) {
        if (c == null) return;
        // fetch the packagemanager so we can get the default launch activity
        // (you can replace this intent with any other activity if you want
        PackageManager pm = c.getPackageManager();
        //check if we got the PackageManager
        if (pm == null) return;

        //create the intent with the default start activity for your application
        Intent mStartActivity = pm.getLaunchIntentForPackage(c.getPackageName());
        if (mStartActivity == null) return;

        mStartActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        // create a pending intent so the application is restarted after System.exit(0) was called.
        // We use an AlarmManager to call this intent in 100ms
        int mPendingIntentId = 223344;
        PendingIntent mPendingIntent = PendingIntent.getActivity(c, mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager mgr = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 1500, mPendingIntent);
        //kill the application
        System.exit(0);
    }

    // ----------------------------------------------------
    // -----------------speak button           ------------
    // ----------------------------------------------------
    private FloatingActionButton speakButton = null;
    private void updateMicrophoneFloatButton() {
        if ( matchModel == null ) { return; }
        boolean bShowSpeakFAB = matchModel.gameHasStarted() == false || matchModel.isStartOfTieBreak() || matchModel.isPossibleGameVictory();
        showMicrophoneFloatButton(bShowSpeakFAB);
    }
    private void showMicrophoneFloatButton(boolean bVisible) {
        if ( PreferenceValues.useOfficialAnnouncementsFeature(this).equals(Feature.DoNotUse) ) {
            if ( speakButton != null ) { speakButton.setHidden(true); }
            return;
        }

        if ( speakButton == null ) {
            float fMargin = 0;
            if ( isPortrait() ) {
                fMargin = .125f; // to NOT cover the Inline timer
            }
            int iResImage = R.drawable.microphone;

            ColorPrefs.ColorTarget colorKey = ColorPrefs.ColorTarget.speakButtonBackgroundColor;
            Integer iBG = mColors.get(colorKey);
            if ( iBG != null ) {
                // if we use a light background for the microphone button... switch to the black icon version
                int blackOrWhiteFor = ColorUtil.getBlackOrWhiteFor(iBG);
                if ( blackOrWhiteFor == Color.BLACK ) {
                    iResImage = R.drawable.microphone_black;
                }
            }
            speakButton = getFloatingActionButton(R.id.sb_official_announcement, fMargin, iResImage, ColorPrefs.ColorTarget.speakButtonBackgroundColor);
/*
            speakButton.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    _showOfficialAnnouncement(AnnouncementTrigger.Manual);
                }
            });
*/
        }
        speakButton.setHidden(bVisible == false);
    }

    // ----------------------------------------------------
    // -----------------timer button           ------------
    // ----------------------------------------------------

    private FloatingActionButton timerButton = null;
    private void updateTimerFloatButton() {
        if ( matchModel == null ) { return; }
        boolean bShowTimerFAB = ( matchModel.gameHasStarted() == false || matchModel.isPossibleGameVictoryFor() != null ) && (matchModel.matchHasEnded() == false) && (timer == null);
        if ( bShowTimerFAB && Brand.isRacketlon() && (ListUtil.length(matchModel.isPossibleMatchBallFor()) == 2) ) {
            // do not show timer button when it is gummiarm point
            bShowTimerFAB = false;
        }
        showTimerFloatButton(bShowTimerFAB);
    }
    private void showTimerFloatButton(boolean bVisible) {
        if ( PreferenceValues.useTimersFeature(this).equals(Feature.DoNotUse) ) {
            if ( timerButton != null ) { timerButton.setHidden(true); }
            return;
        }

        if ( timerButton == null ) {
            float fMargin = 1.25f;
            int iResImage = R.drawable.timer;

            ColorPrefs.ColorTarget colorKey = ColorPrefs.ColorTarget.timerButtonBackgroundColor;
            Integer iBG = mColors.get(colorKey);
            if ( iBG != null ) {
                // if we use a light background for the timer button... switch to the black icon version
                int blackOrWhiteFor = ColorUtil.getBlackOrWhiteFor(iBG);
                if ( blackOrWhiteFor == Color.BLACK ) {
                    iResImage = R.drawable.timer_black;
                }
            }
            timerButton = getFloatingActionButton(R.id.float_timer, fMargin, iResImage, ColorPrefs.ColorTarget.timerButtonBackgroundColor);
        }

        if ( m_mode.equals(Mode.Normal) == false ) {
            timerButton.setOnLongClickListener(new View.OnLongClickListener() {
                @Override public boolean onLongClick(View v) {
                    StringBuilder sb = new StringBuilder(256);
                    List<String> times = matchModel.getFormattedTimes();
                    int i = 0;
                    for (String sTime : times) {
                        sb.append(sTime).append("\n");
                        i++;
                        if (i % 2 == 0) {
                            sb.append("\n");
                        }
                    }
                    dialogWithOkOnly(ScoreBoard.this, sb.toString());
                    return true;
                }
            });
        }

        timerButton.setHidden(bVisible == false);
    }

    // ----------------------------------------------------
    // -----------------toss button            ------------
    // ----------------------------------------------------
    private FloatingActionButton tossButton = null;
    private void updateTossFloatButton() {
        boolean bShowTossFAB = (matchModel.hasStarted() == false) && (timer == null);
        showTossFloatButton(bShowTossFAB);
    }
    private void showTossFloatButton(boolean bVisible) {
        if ( PreferenceValues.useTossFeature(this).equals(Feature.DoNotUse) ) {
            if ( tossButton != null ) { tossButton.setHidden(true); }
            return;
        }

        if ( tossButton == null ) {
            float fMargin = 2.25f;
            int iResTossImage = R.drawable.toss_white;

            ColorPrefs.ColorTarget colorKey = ColorPrefs.ColorTarget.tossButtonBackgroundColor;
            Integer iBG = mColors.get(colorKey);
            if ( iBG != null ) {
                // if we use a light background for the toss button... switch to the black icon version
                int blackOrWhiteFor = ColorUtil.getBlackOrWhiteFor(iBG);
                if ( blackOrWhiteFor == Color.BLACK ) {
                    iResTossImage = R.drawable.toss_black;
                }
            }
            tossButton = getFloatingActionButton(R.id.float_toss, fMargin, iResTossImage, colorKey);
        }
        tossButton.setHidden(bVisible == false);
    }

    // ----------------------------------------------------
    // -----------------share button           ------------
    // ----------------------------------------------------
    private FloatingActionButton shareButton = null;
    private void showShareFloatButton(boolean bGameHasEnded, boolean bMatchHasEnded) {
        if ( PreferenceValues.useShareFeature(this).equals(Feature.Suggest)==false ) {
            if ( shareButton != null ) { shareButton.setHidden(true); }
            return;
        }

        if ( shareButton == null ) {
            float fMargin = 1.25f; // same as 'Timer' button (not over toss button, because than it may hide the score)
            shareButton = getFloatingActionButton(R.id.float_match_share, fMargin, android.R.drawable.ic_menu_share, ColorPrefs.ColorTarget.shareButtonBackgroundColor);
            // ic_menu_share
            //   mdpi = 32x32
            //   ldpi = 36x36
            //   hdpi = 48x48
            //  xhdpi = 64x64 // on android 9 for floating buttons at least this resolution should be available
            // xxhdpi = 96x96
        }
        ShareMatchPrefs prefs = PreferenceValues.getShareAction(this);
        boolean bVisible = bMatchHasEnded || (bGameHasEnded && prefs.alsoBeforeMatchEnd());
        shareButton.setHidden(bVisible == false);
        shareButton.setActionId(prefs.getMenuId());
        shareButton.setDrawable(this.getResources().getDrawable(prefs.getDrawableId()), getFloatingButtonSizePx(this));
    }

    // ----------------------------------------------------
    // -----------------new match button       ------------
    // ----------------------------------------------------
    private FloatingActionButton newMatchButton = null;
    private void updateNewMatchFloatButton() {
        boolean bShowNewMatchFAB =    ( (matchModel.hasStarted() == false) && (timer == null))
                                   || (  matchModel.isLocked()             && (matchModel.getLockState().equals(LockState.LockedManualGUI) == false))
                                   ||    matchModel.matchHasEnded()
                                 ;
        showNewMatchFloatButton(bShowNewMatchFAB);
    }
    private void showNewMatchFloatButton(boolean bVisible) {
        if ( PreferenceValues.showNewMatchFloatButton(this) == false ) {
            if ( newMatchButton != null ) { newMatchButton.setHidden(true); }
        }

        int iActionId   = R.id.float_new_match;
        int iDrawableId = android.R.drawable.ic_menu_add;
        if ( matchModel.getLockState().equals(LockState.LockedManualGUI) ) {
            iActionId   = R.id.sb_unlock;
            iDrawableId = android.R.drawable.ic_lock_lock;
        }
        if ( newMatchButton == null ) {
            final float fMargin = isPortrait()?0.25f:0.25f; // TODO: simply within score button of player B?
            ColorPrefs.ColorTarget color = isLandscape()? ColorPrefs.ColorTarget.playerButtonBackgroundColor : ColorPrefs.ColorTarget.scoreButtonBackgroundColor;
            newMatchButton = getFloatingActionButton(iActionId, fMargin, iDrawableId, color, Direction.SE);
        } else {
            newMatchButton.setActionId(iActionId);
            newMatchButton.setDrawable(this.getResources().getDrawable(iDrawableId), getFloatingButtonSizePx(this));
        }
        if ( newMatchButton != null ) {
            newMatchButton.setHidden(bVisible == false);
        }
    }

    /** might present a dialog to the user, Based-On-Preference. Returns true if dialog will be shown */
    private boolean endGame_BOP(Player leadingPlayer) {
        boolean bShowDialog = false;
        Feature endGameSuggestion = PreferenceValues.endGameSuggestion(ScoreBoard.this);
        switch(endGameSuggestion) {
            case DoNotUse:
                break;
            case Suggest:
                bShowDialog = _confirmGameEnding(leadingPlayer);
                break;
            case Automatic:
                //nonIntrusiveGameEnding(leadingPlayer);// TODO: only enable if no automatic dialog follows (no official, no timer...)
                matchModel.endGame();
                break;
        }
        return bShowDialog;
    }

    // ----------------------------------------------------
    // -----------------float utility          ------------
    // ----------------------------------------------------
    private FloatingActionButton getFloatingActionButton(int iActionId, float fMargin, int iDrawable, ColorPrefs.ColorTarget colorTarget) {
        return getFloatingActionButton(iActionId, fMargin, iDrawable, colorTarget, isPortrait() ? Direction.E : Direction.S);
    }
    private FloatingActionButton getFloatingActionButton(int iActionId, float fMargin, int iDrawable, ColorPrefs.ColorTarget colorTarget, Direction direction) {
        int buttonSizePx = getFloatingButtonSizePx(this);
        int iMargin = (int) fMargin * buttonSizePx; // 64=floatingActionButtonSize

        int iMarginLeft   = 0;
        int iMarginTop    = 0;
        int iMarginRight  = 0;
        int iMarginBottom = 0;

        switch (direction) {
            case SW: case NW: case W: iMarginLeft   = iMargin; break;
            case SE: case NE: case E: iMarginRight  = iMargin; break;
        }
        switch (direction) {
            case NE: case NW: case N: iMarginTop    = iMargin; break;
            case SE: case SW: case S: iMarginBottom = iMargin; break;
        }
        int iRootId = (ScoreBoard.this.drawerLayout == null) ? android.R.id.content : R.id.content_frame;
        Integer color = Color.BLACK;
        if ( ( mColors != null ) && mColors.containsKey(colorTarget) ) {
            color = mColors.get(colorTarget);
        }
        FloatingActionButton view = new FloatingActionButton.Builder(ScoreBoard.this, iRootId, buttonSizePx)
                .withDrawable(iDrawable)
                .withButtonColor(color)
                .withGravity(direction.getGravity())
                .withMargins(iMarginLeft, iMarginTop, iMarginRight, iMarginBottom)
                .create(true);

        view.setActionId(iActionId);
        view.setId      (iActionId);
        view.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                int iActionId = v.getId();
                if ( v instanceof FloatingActionButton ) {
                    FloatingActionButton fab = (FloatingActionButton) v;
                    iActionId = fab.getActionId();
                }
                handleMenuItem(iActionId);
            }
        });
        return view;
    }

    private static int getFloatingButtonSizePx(Context context) {
        int buttonSizePx = 0;
        if ( ViewUtil.isPortraitOrientation(context) ) {
            buttonSizePx = ViewUtil.getScreenHeightWidthMaximumFraction(context, R.fraction.pt_gamescores_height);
        } else {
            buttonSizePx = ViewUtil.getScreenHeightWidthMaximumFraction(context, R.fraction.ls_gamescores_width);
        }
        return buttonSizePx;
    }

    // ----------------------------------------------------
    // -----------------model change listeners ------------
    // ----------------------------------------------------
    private boolean bInitializedModelListeners  = false;
    private boolean bInitializingModelListeners = false;
    private void initModelListeners() {
        if ( matchModel == null ) { return; }
        if ( bInitializedModelListeners ) { return; }
        bInitializingModelListeners = true;
        GameHistoryView.dontShowForToManyPoints(matchModel.getMaxScore() + matchModel.getMinScore());
        matchModel.clearListeners(".*" + this.getClass().getSimpleName() + ".*"); // remove listeners e.g. from this activity instance that was created for another orientation
        matchModel.registerListener(new ScoreChangeListener());
        matchModel.registerListener(new PlayerChangeListener());
        matchModel.registerListener(new ServeSideChangeListener());
        matchModel.registerListener(new SpecialScoreChangeListener());
        matchModel.registerListener(new ComplexChangeListener());
        matchModel.registerListener(new GameEndListener());
        matchModel.registerListener(new MatchEndListener());
        matchModel.registerListener(new CallChangeListener());
        matchModel.registerListener(new BrokenEquipmentListener());
        matchModel.registerListener(new LockChangeListener());
        matchModel.registerListener(new TimingChangedListener());
      //matchModel.registerListener(new ScoreLinesChangeListener());

        //dbgmsg("Model listeners registered", 0, 0);
        bInitializingModelListeners = false;
        bInitializedModelListeners  = true;
    }

    // ----------------------------------------------------
    // -----------------model listeners        ------------
    // ----------------------------------------------------

    private class SpecialScoreChangeListener implements Model.OnSpecialScoreChangeListener {
        @Override public void OnGameBallChange(Player[] players, boolean bHasGameBall) {
            //iBoard.doGameBallColorSwitch(player, bHasGameBall);
            iBoard.updateGameBallMessage(players, bHasGameBall);

            if ( bHasGameBall ) {
                showMicrophoneFloatButton(false); // previous might have been tiebreak
            }
        }

        @Override public void OnTiebreakReached(int iOccurrenceCount) {
            if ( bInitializingModelListeners ) { return; }
            if ( autoShowTieBreakDialog(iOccurrenceCount) == false ) {
                showMicrophoneFloatButton(true);
            }
        }
        @Override public void OnGameEndReached(Player leadingPlayer) {
            if ( bInitializingModelListeners ) { return; }
            if ( bHapticFeedbackOnGameEnd ) {
                SystemUtil.doVibrate(ScoreBoard.this, 800);
            }
            updateMicrophoneFloatButton();
            updateTimerFloatButton();
            iBoard.updateGameBallMessage();
            showShareFloatButton(true, matchModel.matchHasEnded());
            updateNewMatchFloatButton();

            // automatically semi-lock the model (unless it was already manually unlocked)
            if ( matchModel.matchHasEnded() ) {
                if (PreferenceValues.lockMatchMV(ScoreBoard.this).contains(AutoLockContext.AtEndOfMatch)) {
                    if (matchModel.getLockState().equals(LockState.UnlockedManual) == false) {
                        matchModel.setLockState(LockState.UnlockedEndOfFinalGame);
                    }
                }
            }

            boolean bTryToAutoEndGame = true;
            if ( Brand.isRacketlon() ) {
                // do not end the set if at the end of set 4 and diff=0: gummiarm
                if ( matchModel.getGameNrInProgress() == 4 ) {
                    Map<Player, Integer> pointsDiff = matchModel.getPointsDiff(true);
                    if ( MapUtil.getMaxValue(pointsDiff) == 0 ) {
                        bTryToAutoEndGame = false;
                    }
                }
            }
            if ( bTryToAutoEndGame ) {
                if ( endGame_BOP(leadingPlayer) == false ) {
                    // no dialog, enable score button now (in stead of in 'onDismiss' of dialog)
                    enableScoreButton(leadingPlayer);
                };
            } else {
                enableScoreButton(leadingPlayer); // we only disabled if button of leader was pressed on gameball
            }
            if ( m_liveScoreShare != null ) {
                shareScoreSheet(ScoreBoard.this, matchModel, true);
            }
        }

        @Override public void OnGameIsHalfwayChange(int iGameZB, int iScoreA, int iScoreB, Halfway hwStatus) {
            if ( matchModel.showChangeSidesMessageInGame(iGameZB) ) {
                if ( hwStatus.isHalfway() && hwStatus.changeSidesFor(matchModel.getSport()) ) {
                    boolean bDialogOpened = swapPlayers_BOP();
                    if ( bDialogOpened == false ) {
                        iBoard.showMessage(getString(R.string.oa_change_sides), 5);
                    }
                    if ( Brand.isRacketlon()  ) {
                        boolean isSquashDiscipline = matchModel.getSportForGame(iGameZB + 1).equals(Sport.Squash);
                        if ( isSquashDiscipline && matchModel.isDoubles() ) {
                            // we use A1B1A1B1 serve sequence: swap players for both teams so that R/L indication is displayed for players actually on court
                            for(Player p: Model.getPlayers() ) {
                                _swapDoublePlayers(p);
                            }
                        }
/*
                        if ( isSquashDiscipline == false ) {
                            swapPlayers();
                        }
*/
                    }
                } else {
                    iBoard.hideMessage();
                }
            }
            if ( hwStatus.isHalfway() && ShareMatchPrefs.LinkWithFullDetailsEachHalf.equals(m_liveScoreShare) ) {
                shareScoreSheet(ScoreBoard.this, matchModel, false);
            }
        }

        @Override public void OnFirstPointOfGame() {
            ShareMatchPrefs liveScoreShare = PreferenceValues.isConfiguredForLiveScore(ScoreBoard.this);
            if ( matchModel.getGameNrInProgress()==1 && ShareMatchPrefs.LinkWithFullDetailsEachHalf.equals(liveScoreShare) ) {
                // share if livescoring is 'Semi-On' to let the match appear in the list a.s.a.p.
                shareScoreSheet(ScoreBoard.this, matchModel, false);
            }

            showAppropriateMenuItemInActionBar();
          //bNonIntrusiveGameEndingPerformed = false;
            bGameEndingHasBeenCancelledThisGame = false;
            dialogManager.clearDialogs(); // e.g. timer was 'hidden' and than first point scored... ensure no more official announcements are in the queue
            cancelTimer();
            iBoard.updateGameAndMatchDurationChronos();

            showMicrophoneFloatButton(false);
            showNewMatchFloatButton(false);
            showTossFloatButton(false);
            showTimerFloatButton(false);

            // just to be on the save side: hide the following to as well
            showShareFloatButton(false, false);
            iBoard.updateGameBallMessage();
            iBoard.updateBrandLogoBasedOnScore();
            iBoard.updateFieldDivisionBasedOnScore();
        }
    }

    private class ScoreChangeListener implements Model.OnScoreChangeListener
    {
        @Override public void OnScoreChange(Player p, int iTotal, int iDelta, Call call) {
            if ( bHapticFeedbackPerPoint ) {
                int lDuration = iDelta == 1 ? 200 : 500;
                SystemUtil.doVibrate(ScoreBoard.this, lDuration);
            }
            iBoard.updateScore(p, iTotal);
            iBoard.updateScoreHistory(iDelta == 1);
            if ( bInitializingModelListeners == false ) {
                cancelTimer(); // do not cancel
            }
            if ( iDelta != 1 ) {
                updateTimerFloatButton();
                updateTossFloatButton();
                updateMicrophoneFloatButton();
                showShareFloatButton(false, false);
                iBoard.undoGameBallColorSwitch();
                showAppropriateMenuItemInActionBar();
                iBoard.updateBrandLogoBasedOnScore();
                iBoard.updateFieldDivisionBasedOnScore();
                iBoard.updateGameAndMatchDurationChronos();
            } else {
                // normal score
                if ( PreferenceValues.recordRallyEndStatsAfterEachScore(ScoreBoard.this).equals(Feature.Automatic)
                 && (call==null || call.equals(Call.NL) ) ) { // if it was a No Let decision it means the opponent scored with a winner
                    showRallyEndStats(p, call);
                }
            }

            ShareMatchPrefs liveScoreShare = PreferenceValues.isConfiguredForLiveScore(ScoreBoard.this);
            if ( (bInitializingModelListeners == false) && (iTotal != 0) && ShareMatchPrefs.LinkWithFullDetailsEachPoint.equals(liveScoreShare) && (matchModel.isLocked() == false) ) {
                //shareScoreSheet(ScoreBoard.this, matchModel, false);
                // start timer to post in e.g. 2 seconds. Restart this timer as soon as another point is scored
                shareScoreSheetDelayed(2000);
            }

            // for table tennis only
            if ( (iDelta == 1) && Brand.isTabletennis() ) {
                int iEachX = PreferenceValues.autoShowGamePausedDialogAfterXPoints(ScoreBoard.this);
                if ( (iEachX > 0) && (matchModel.getTotalGamePoints() % iEachX == 0) && (matchModel.isPossibleGameVictory() == false)) {
                    Feature showGamePausedDialog = PreferenceValues.showGamePausedDialog(ScoreBoard.this);
                    switch (showGamePausedDialog) {
                        case Automatic: {
                            // show pause dialog
                            _showTimer(Type.TowelingDown, true);
                        }
                        case Suggest: {
                            // show timer floating button
                            showTimerFloatButton(true);
                        }
                    }
                } else {
                    showTimerFloatButton(false);
                }
            }
            if ( matchModel.getMaxScore()==0 ) {
                enableScoreButton(p);
            }
        }
    }
    private class CallChangeListener implements Model.OnCallChangeListener {
        @Override public void OnCallChanged(Call call, Player appealingOrMisbehaving, Player pointAwardedTo, ConductType conductType) {
            iBoard.updateScoreHistory(true);

            if ( PreferenceValues.showChoosenDecisionShortly(ScoreBoard.this) ) {
                iBoard.showChoosenDecision(call, appealingOrMisbehaving, conductType);
            }
            if ( pointAwardedTo != null ) {
                //showToast(R.string.point_awarded_to_p, matchModel.getName(pointAwardedTo));
            } else {
                // a let or a warning: ensure floating buttons are hidden (e.g. if score was still 0-0)
                showTimerFloatButton(false);
                showTossFloatButton(false);
                showMicrophoneFloatButton(false);
                showNewMatchFloatButton(false);
                showAppropriateMenuItemInActionBar();
            }
        }
    }

    private class BrokenEquipmentListener implements Model.OnBrokenEquipmentListener {
        @Override public void OnBrokenEquipmentChanged(BrokenEquipment equipment, Player affectedPlayer) {
            iBoard.updateScoreHistory(false);
        }
    }
    private class ComplexChangeListener implements Model.OnComplexChangeListener {
        @Override public void OnChanged() {
            // e.g. an undo back into previous game has been done, or score has been adjusted
            iBoard.updateScoreHistory(false);
            iBoard.updateGameScores();
            for(Player p: Model.getPlayers()) {
                iBoard.updateScore    (p, matchModel.getScore(p));
                iBoard.updateServeSide(p, matchModel.getNextDoubleServe(p), matchModel.getNextServeSide(p), matchModel.isLastPointHandout());
            }
            // for restart score and complex undo
            updateTimerFloatButton();
            updateTossFloatButton();
            updateMicrophoneFloatButton();
            showShareFloatButton(matchModel.isPossibleGameVictory(), matchModel.matchHasEnded());
            iBoard.updateGameBallMessage();
            iBoard.updateGameAndMatchDurationChronos();

            showAppropriateMenuItemInActionBar();
        }
    }
    private class ServeSideChangeListener implements Model.OnServeSideChangeListener {
        @Override public void OnServeSideChange(Player p, DoublesServe doublesServe, ServeSide serveSide, boolean bIsHandout) {
            if ( p == null ) { return; } // normally only e.g. for undo's of 'altered' scores
            iBoard.updateServeSide(p           ,doublesServe   , serveSide, bIsHandout);
            iBoard.updateServeSide(p.getOther(),DoublesServe.NA, null     , false);
        }
    }

    private class GameEndListener implements Model.OnGameEndListener {
        @Override public void OnGameEnded(Player winningPlayer) {
            if ( bHapticFeedbackOnGameEnd ) {
                SystemUtil.doVibrate(ScoreBoard.this, 200);
            }
            if ( EnumSet.of(ShareMatchPrefs.LinkWithFullDetailsEachGame, ShareMatchPrefs.LinkWithFullDetailsEachHalf).contains(m_liveScoreShare) ) {
                shareScoreSheet(ScoreBoard.this, matchModel, true);
            }

            showAppropriateMenuItemInActionBar();

            if ( Brand.isTabletennis() && (matchModel.matchHasEnded() == false) && PreferenceValues.swapPlayersBetweenGames(ScoreBoard.this) ) {
                swapPlayers(Toast.LENGTH_LONG);
            }

            if ( matchModel.matchHasEnded() == false ) {
                boolean bEndGameDialogWasPresented = PreferenceValues.endGameSuggestion(ScoreBoard.this).equals(Feature.Suggest);
                if ( PreferenceValues.useTimersFeature(ScoreBoard.this).equals(Feature.Automatic) ) {
                    if ( bEndGameDialogWasPresented == false ) {
                        autoShowOfficialAnnouncement(AnnouncementTrigger.EndOfGame);
                    }
                    autoShowGameDetails();
                    autoShowHandicap();
                    autoShowTimer(Type.UntillStartOfNextGame);
                    autoShowOfficialAnnouncement(AnnouncementTrigger.StartOfGame);
                } else {
                    autoShowGameDetails();
                    autoShowHandicap();
                    autoShowOfficialAnnouncement(AnnouncementTrigger.StartOfGame);
                }
            }

            iBoard.updateScoreHistory(false);
            iBoard.updateGameScores();

            updateMicrophoneFloatButton();
            updateTimerFloatButton();
            showShareFloatButton(true, matchModel.matchHasEnded());

            if ( PreferenceValues.getTiebreakFormat(ScoreBoard.this).needsTwoClearPoints() == false ) {
                // uncommon tiebreak format: but do not highlight in this case
                iBoard.undoGameBallColorSwitch();
            }
            //iBoard.showGameBallMessage(false, null);
            iBoard.updateGameBallMessage(); // in rare case in racketlon a 0-0 score may be a matchball in set 3 or 4
        }
    }
    private class MatchEndListener implements Model.OnMatchEndListener {
        @Override public void OnMatchEnded(Player leadingPlayer, EndMatchManuallyBecause endMatchManuallyBecause) {
            autoShowOfficialAnnouncement(AnnouncementTrigger.EndOfMatch);
            confirmPostMatchResult();
            autoShare();
            deleteFromMyList_BOP();
            autoShowGameDetails();
            showMicrophoneFloatButton(true); // allow to request the 'match conclusion' announcement
            showShareFloatButton(true, true); // allow to share the match result

            showTimerFloatButton(false);
            cancelTimer(); // normally only required for 'End match' menu option
            iBoard.stopMatchDurationChrono();

            if ( rmm != null ) {
                rmm.showDialogIfApplicable();
            }
            if ( PreferenceValues.keepScreenOnWhen(ScoreBoard.this).equals(KeepScreenOnWhen.MatchIsInProgress) ) {
                keepScreenOn(false);
            }

            // automatically lock the model (unless it was already manually unlocked)
            boolean bLockStateChanged = false;
            EnumSet<AutoLockContext> autoLockContexts = PreferenceValues.lockMatchMV(ScoreBoard.this);
            if ( autoLockContexts.contains(AutoLockContext.AtEndOfMatch) ) {
                LockState lockState = matchModel.getLockState();
                if ( lockState.equals(LockState.UnlockedManual) == false || matchModel.getName(Player.A).equals("Shaun") || Brand.isNotSquash() ) {
                    LockState newLS = endMatchManuallyBecause!=null
                                    ? (endMatchManuallyBecause.equals(EndMatchManuallyBecause.ConductMatch)?LockState.LockedEndOfMatchConduct:LockState.LockedEndOfMatchRetired)
                                    : LockState.LockedEndOfMatch;
                    matchModel.setLockState(newLS);
                    bLockStateChanged = true;
                }
            }
            if ( bLockStateChanged == false ) {
                updateNewMatchFloatButton();
            }
        }
    }
    private class LockChangeListener implements Model.OnLockChangeListener {
        @Override public void OnLockChange(LockState lockStateOld, LockState lockStateNew) {
            boolean locked = lockStateNew.isLocked();
            ViewUtil.toggleMenuItems(mainMenu, R.id.sb_unlock, R.id.sb_lock, locked);
            if ( locked && (lockStateNew.isUnlockable() == false) ) {
                setMenuItemVisibility(R.id.sb_lock  , false);
                setMenuItemVisibility(R.id.sb_unlock, false);
            }
            setMenuItemVisibility(R.id.dyn_undo_last, locked == false);
            updateNewMatchFloatButton();

            if ( PreferenceValues.keepScreenOnWhen(ScoreBoard.this).equals(KeepScreenOnWhen.MatchIsInProgress) ) {
                keepScreenOn(lockStateNew.isLocked() == false);
            }
            if ( lockStateNew.equals(LockState.LockedManualGUI) || lockStateOld.equals(LockState.LockedManualGUI) ) {
                int iResId = locked ? R.string.match_is_now_locked : R.string.match_is_now_unlocked;
                iBoard.showToast(iResId);
            }
            if ( lockStateNew.equals(LockState.LockedManualGUI) ) {
                showNewMatchFloatButton(true); // show lock icon
            }
            if ( locked ) {
                iBoard.stopGameDurationChrono();
                //iBoard.stopMatchDurationChrono();
            } else {
                iBoard.updateGameDurationChrono();
                //iBoard.updateMatchDurationChrono();
            }
        }
    }
    private class TimingChangedListener implements GameTiming.OnTimingChangedListener  {
        @Override public void OnTimingChanged(int iGameNr, GameTiming.Changed changed, long lTimeStart, long lTimeEnd, GameTiming.ChangedBy changedBy) {
            if ( (iGameNr == 0) && (changed == GameTiming.Changed.Start) ) {
                // reset duration of match back to 00:00
                iBoard.updateMatchDurationChrono();
            }
            if ( changed == GameTiming.Changed.Start || changed == GameTiming.Changed.Both ) {
                // most likely reset duration of game to 00:00
                iBoard.updateGameDurationChrono();
            }
            if ( changed == GameTiming.Changed.End   || changed == GameTiming.Changed.Both ) {
                if ( matchModel.isPossibleGameVictory() ) {
                    iBoard.stopGameDurationChrono();
                }
            }
            if ( (matchModel.gameHasStarted() == false) && (changedBy== GameTiming.ChangedBy.TimerStarted) ) {
                // new timer just started
                iBoard.stopGameDurationChrono();
                if ( matchModel.hasStarted() == false ) {
                    iBoard.stopMatchDurationChrono();
                }
            }
        }
    }
    private class PlayerChangeListener implements Model.OnPlayerChangeListener {
        @Override public void OnNameChange(Player p, String sName, String sCountry, String sAvatar, String sClub, boolean bIsDoubles) {
            iBoard.updatePlayerName   (p, sName, bIsDoubles);
            iBoard.updatePlayerAvatar (p, sAvatar);
            iBoard.updatePlayerCountry(p, sCountry);
            iBoard.updatePlayerClub   (p, sClub);
            setMenuItemEnabled(R.id.sb_swap_double_players, bIsDoubles);
        }
        @Override public void OnColorChange(Player p, String sColor, String sColorPrev) {
            iBoard.initPerPlayerColors(p, sColor, sColorPrev);
        }
        @Override public void OnCountryChange(Player p, String sCountry) {
            iBoard.updatePlayerCountry(p, sCountry);
            if ( PreferenceValues.hideFlagForSameCountry(ScoreBoard.this) && StringUtil.isNotEmpty(sCountry) ) {
                iBoard.updatePlayerCountry(p.getOther(), matchModel.getCountry(p.getOther()));
            }
        }
        @Override public void OnClubChange(Player p, String sCountry) {
            iBoard.updatePlayerClub(p, sCountry);
        }
        @Override public void OnAvatarChange(Player p, String sAvatar) {
            iBoard.updatePlayerAvatar(p, sAvatar);
        }
    }

    private class TTGameDurationTickListener implements Chronometer.OnChronometerTickListener {

        private       int     m_lElapsedMin = -1;
        private final int     m_ShowDialogAfter;
        private       boolean m_bHasBeenPresented = false;

        TTGameDurationTickListener(int iShowAfter) {
            m_ShowDialogAfter = iShowAfter;
        }

        /** will be called approximately every second */
        @Override public void onChronometerTick(Chronometer chronometer) {
            if ( m_bHasBeenPresented || (m_ShowDialogAfter < 1) ) {
                return;
            }
            if ( matchModel.isInMode(TabletennisModel.Mode.Expedite) || matchModel.matchHasEnded() || matchModel.isLocked() ) {
                return;
            }
            long lElapsedMillis = SystemClock.elapsedRealtime() - chronometer.getBase();
            int  lElapsedMin    = Math.round(lElapsedMillis / 1000 / 60);
            if ( lElapsedMin > m_lElapsedMin ) {
                //Log.d(TAG, "Elapsed ms  : " + lElapsedMillis);
                m_lElapsedMin = lElapsedMin;
                if ( m_lElapsedMin != m_ShowDialogAfter ) {
                    Log.d(TAG, "Elapsed min : " + lElapsedMin + " ( != " + m_ShowDialogAfter + ")");
                } else {
                    if ( matchModel.getTotalGamePoints() < 18 ) {
                        m_bHasBeenPresented = true;

                        // let device vibrate anyways, to notify user
                        if ( PreferenceValues.useVibrationNotificationInTimer(ScoreBoard.this) ) {
                            SystemUtil.doVibrate(ScoreBoard.this, 1000);
                        }

                        // show dialog (or toast if another dialog is already showing)
                        if ( isDialogShowing() ) {
                            Toast.makeText(ScoreBoard.this, R.string.activate_mode_Tabletennis, Toast.LENGTH_LONG).show();
                        } else {
                            showActivateMode(TabletennisModel.Mode.Expedite.toString());
                        }
                    }
                }
            }
        }
    }

    // ----------------------------------------------------
    // ------------------------- events -------------------
    // ----------------------------------------------------

    public enum SBEvent {
        newMatchStarted,
        timerStarted,
        timerCancelled,
        timerWarning,
        timerEnded,
        timerViewChanged,
        tossDialogEnded,
        endGameDialogEnded,
        endMatchDialogEnded,
        //editFormatDialogEnded,
        restartScoreDialogEnded,
        officialAnnouncementClosed,
        injuryTypeClosed,
        //rallyEndStatsClosed,
        //specifyHandicapClosed,
        //dialogClosed,
    }

    /** This method is focused at handling gui related events */
    public boolean triggerEvent(SBEvent event, Object ctx) {
        //Log.d(TAG, "triggerEvent " + event + " (" + ctx + ")");
        switch ( event ) {
            case newMatchStarted:
                lastTimerType = null;
                showAppropriateMenuItemInActionBar();
                dialogManager.clearDialogs();
                autoShowTimer(Type.Warmup);
                autoShowHandicap();
                autoShowTossDialog();
                autoShowTimer(Type.UntillStartOfNextGame);
                autoShowOfficialAnnouncement(AnnouncementTrigger.StartOfGame);
                if ( PreferenceValues.keepScreenOnWhen(ScoreBoard.this).equals(KeepScreenOnWhen.MatchIsInProgress) ) {
                    keepScreenOn(true);
                }
                return true;
            case tossDialogEnded:
                showTossFloatButton(false);
                if ( matchModel.hasStarted() == false ) {
                    matchModel.timestampStartOfGame(GameTiming.ChangedBy.DialogClosed);
                }
                showNextDialog();
                break;
            case timerViewChanged:
                showTimerFloatButton(false);
                showTossFloatButton(false);
                //showMicrophoneFloatButton(false);
                return true;
            case timerStarted: {
                showTimerFloatButton(false);

                if ( PreferenceValues.timerViewType(this).equals(ViewType.Inline) ) {
                    // assume only the inline timer is shown: hide it so timer is better visible
                    showTossFloatButton(false);
                }
                if ( matchModel.gameHasStarted() == false ) {
                    matchModel.timestampStartOfGame(GameTiming.ChangedBy.TimerStarted);
                }
                return true;
            }
            case timerWarning: {
                Type timerType = (Type) ctx;
                doTimerFeedback(timerType, false);
                return true;
            }
            case timerEnded:
                Type timerType = (Type) ctx;
                //ViewType viewType  = (ViewType) ctx2;
                doTimerFeedback(timerType, true);
                if ( timerType.equals(Type.UntillStartOfNextGame) && matchModel.isPossibleGameVictory() ) {
                    matchModel.endGame();
                }
                // fall through!!
            case timerCancelled: {
                timerType = (Type) ctx;
                //viewType  = (ViewType) ctx2;
                if ( EnumSet.of(Type.UntillStartOfNextGame, Type.Warmup).contains(timerType) ) {
                    if ( matchModel.gameHasStarted() == false ) {
                        matchModel.timestampStartOfGame(GameTiming.ChangedBy.TimerEnded);
                    }
                }
                boolean pEndOfGameViewShowing = (m_endOfGameView != null);
                if ( pEndOfGameViewShowing ) {
                    findViewById(R.id.presentation_frame).setVisibility(View.GONE);
                    findViewById(R.id.content_frame     ).setVisibility(View.VISIBLE);
                }
                showAppropriateMenuItemInActionBar();
                showTimerFloatButton(Type.Warmup.equals(timerType) && (matchModel.gameHasStarted() == false));
                showTossFloatButton (Type.Warmup.equals(timerType) && (matchModel.hasStarted()     == false));
                updateMicrophoneFloatButton();
                showNextDialog();

                if ( Brand.isTabletennis() && EnumSet.of(Type.TowelingDown, Type.Timeout).contains(timerType) ) {
                    iBoard.resumeGameDurationChrono();
                }
                return false;
            }
            case officialAnnouncementClosed: {
                if ( matchModel.gameHasStarted() == false ) {
                    matchModel.timestampStartOfGame(GameTiming.ChangedBy.DialogClosed);
                    cancelTimer();
                }
                showMicrophoneFloatButton(false);
                showNewMatchFloatButton(false);
                showNextDialog();
                if ( matchModel.gameHasStarted() == false ) {
                    // assume first ball is about to be served
                    showTossFloatButton(false);
                    showTimerFloatButton(false);
                }
                break;
            }
            case injuryTypeClosed: {
                Type injuryType = (Type) ctx;
                if (injuryType != null) {
                    showTimer(injuryType, false);
                }
                break;
            }
            case restartScoreDialogEnded:
            case endMatchDialogEnded: // fall through
            case endGameDialogEnded: {
                if ( matchModel.matchHasEnded() ) {
                    // if the match has ended the OnlineSheetAvailableChoice dialog may be on the stack
                    showNextDialog();
                } else {
                    dialogManager.removeDialog(ctx);
                    //showNextDialog(); // not here: while the EndGame dialog is showing there will not be dialogs on the stack,
                    // only by pressing Yes, the game is actually ended and new dialogs (e.g. timers) may be added based on user preferences
                    // but if that happens the EndGame dialog itself is already dismissed, so the new dialog will be opened anyway
                }

                //enableScoreButton();
                break;
            }
/*
            case editFormatDialogEnded: {
                showNextDialog();
                break;
            }
            case rallyEndStatsClosed: {
                showNextDialog();
                break;
            }
            case specifyHandicapClosed: {
                showNextDialog();
                break;
            }
            case dialogClosed: {
                showNextDialog();
                break;
            }
*/
        }
        return false;
    }

    /** e.g. called from timerview, before another timer is started */
    public void cancelTimer() {
        if ( timer != null ) {
            Type type = Timer.timerType;
            timer.cancel();
            timer = null;
            Timer.removeTimerView(false, dialogTimerView);
            dialogTimerView = null;
            this.triggerEvent(SBEvent.timerCancelled, type);
        }
    }

    private void doTimerFeedback(Type viewType, boolean bIsEnd) {
        if ( PreferenceValues.useSoundNotificationInTimer    (this) ) {
/*
            int iStreamForTimer = AudioManager.STREAM_NOTIFICATION; // TODO: configurable: different for 15 secs and actual end
            int iRingToneType = RingtoneManager.TYPE_NOTIFICATION;
            if ( bIsEnd ) {
                if ( Type.Warmup.equals(viewType) ) {
                    // TODO: no option for user to stop the sound??
                    iStreamForTimer = AudioManager.STREAM_ALARM;
                    iRingToneType = RingtoneManager.TYPE_ALARM;
                } else {
                    // TODO: no option for user to stop the sound??
                    iStreamForTimer = AudioManager.STREAM_RING;
                    iRingToneType = RingtoneManager.TYPE_RINGTONE;
                }
            }
            SystemUtil.playSound(this, iStreamForTimer, iRingToneType);
*/
            SystemUtil.playNotificationSound(this);
        }
        if ( PreferenceValues.useVibrationNotificationInTimer(this) ) {
            SystemUtil.doVibrate(this, 800);
        }
    }
    private Menu       mainMenu                   = null;
    private MenuItem[] menuItemsWithOrWithoutText = null;


    private static Bitmap m_appIconAsBitMap = null;
    public static Bitmap getAppIconAsBitMap(Context ctx) {
        if ( m_appIconAsBitMap == null) {
            Drawable icon = null;
            try {
                icon = ctx.getPackageManager().getApplicationIcon(ctx.getPackageName());
            } catch (PackageManager.NameNotFoundException e) {
            }
            if ( icon instanceof BitmapDrawable) {
                BitmapDrawable bmd = (BitmapDrawable) icon;
                m_appIconAsBitMap = bmd.getBitmap();
            }
        }
        return m_appIconAsBitMap;
    }

    /** Populates the scoreBoard's options menu. Called only once for ScoreBoard (but re-invoked if orientation changes) */
    @Override public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();

        inflater.inflate(R.menu.mainmenu, menu);

        initCastMenu(menu);

        mainMenu = menu;
        if ( m_bHideDrawerItemsFromOldMenu && (id2String.isEmpty() == false) ) {
            for(Integer iId: id2String.keySet() ) {
                boolean b = ViewUtil.hideMenuItemForEver(mainMenu, iId);
                if ( b == false ) {
                    String sCaption = getString(id2String.get(iId));
                    Log.w(TAG, String.format("Failed to hide %s = %s", iId, sCaption));
                }
            }
        }
/*
        if ( scSequence != null ) {
            scSequence.setMenu(mainMenu);
        }
*/

        updateDemoThread(menu);

        int[] ia_menuIds = {R.id.dyn_undo_last, R.id.dyn_end_game, R.id.dyn_timer, R.id.dyn_match_share };
        menuItemsWithOrWithoutText = new MenuItem[ia_menuIds.length];
        for ( int i=0; i< ia_menuIds.length; i++ ) {
            menuItemsWithOrWithoutText[i] = mainMenu.findItem(ia_menuIds[i]);
        }
        initActionBarSettings(menuItemsWithOrWithoutText);

        showAppropriateMenuItemInActionBar();

        setMenuItemEnabled(R.id.sb_swap_double_players, matchModel.isDoubles());
        ViewUtil.toggleMenuItems(mainMenu, R.id.sb_unlock, R.id.sb_lock, matchModel.isLocked());
        if ( matchModel.isUnlockable() == false ) {
            setMenuItemVisibility(R.id.sb_unlock, false);
        }

        String sPostUrl = PreferenceValues.getPostResultToURL(this);
        setMenuItemEnabled(R.id.sb_post_match_result, StringUtil.isNotEmpty(sPostUrl));

        boolean bStoreMatches = PreferenceValues.saveMatchesForLaterUsage(this);
        setMenuItemsEnabled(new int[] { R.id.sb_stored_matches, R.id.cmd_export_matches, R.id.cmd_import_matches }, bStoreMatches);
        //mainMenu.findItem(R.id.sb_overflow_submenu).getSubMenu().findItem(R.id.uc_import_export).getSubMenu().setGroupVisible(R.id.grp_import_export_matches, bStoreMatches);

        // change/overwrite captions
        {
        setMenuItemTitle(menu, R.id.sb_email_match_result, PreferenceValues.ow_captionForEmailMatchResult     (this));
        setMenuItemTitle(menu, R.id.sb_send_match_result , PreferenceValues.ow_captionForMessageMatchResult   (this));
        setMenuItemTitle(menu, R.id.sb_post_match_result , PreferenceValues.ow_captionForPostMatchResultToSite(this));
        }

        ViewUtil.setPackageIconOrHide(this, menu, R.id.sb_whatsapp_match_summary, "com.whatsapp");
        ViewUtil.setPackageIconOrHide(this, menu, R.id.sb_twitter_match_summary , "com.twitter.android");

        setMenuItemVisibility(R.id.sb_send_match_result, StringUtil.isNotEmpty(PreferenceValues.getDefaultSMSTo(this)));

        if ( Brand.isNotSquash() ) {
            setMenuItemVisibility(R.id.sb_official_announcement, false);
            setMenuItemVisibility(R.id.sb_possible_conductsA   , false);
            setMenuItemVisibility(R.id.sb_possible_conductsB   , false);
          //setMenuItemVisibility(R.id.sb_swap_double_players  , false);
        }
        if ( Brand.isTabletennis() ) {
            boolean bIsInNormalMode = matchModel.isInNormalMode();
            setMenuItemVisibility(R.id.tt_activate_mode  , bIsInNormalMode == true );
            setMenuItemVisibility(R.id.tt_deactivate_mode, bIsInNormalMode == false);
        }

        return true;
    }

    private void setMenuItemTitle(Menu menu, int iId, String s) {
        MenuItem mi = menu.findItem(iId);
        if ( mi != null ) {
            mi.setTitle(s);
        }
    }
    /** Handles the user's menu selection. */
    @Override public boolean onOptionsItemSelected(MenuItem item)
    {
        SubMenu subMenu = item.getSubMenu();
        if ( subMenu == null && handleMenuItem(item.getItemId()) ) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private int m_idOfVisibleActionBarItem = 0;
    private void showAppropriateMenuItemInActionBar() {
        List<Integer> lShowIds = new ArrayList<Integer>();
        if ( matchModel.hasStarted() == false ) {
            long lMatchStart = matchModel.getMatchStart();
            int lMinutes = DateUtil.convertToMinutes(System.currentTimeMillis() - lMatchStart);
            lShowIds.add(R.id.dyn_timer);
            if ( lMinutes >= 0 /*40*/ ) {
                lShowIds.add(0, R.id.dyn_new_match);
            }
        } else if ( matchModel.matchHasEnded() ) {
            long lMatchEnd = matchModel.getMatchEnd();
            lShowIds.add(R.id.dyn_score_details);
            int lMinutes = DateUtil.convertToMinutes(System.currentTimeMillis() - lMatchEnd);
            if ( lMinutes >= 0 /*10*/ ) {
                // old match is still displayed, presume 'new match' is preferred
                lShowIds.add(0, R.id.dyn_new_match);
            }
        } else if ( matchModel.isLocked() ) {
            lShowIds.add(0, R.id.dyn_new_match);
        } else if ( matchModel.gameHasStarted() == false ) {
            if (PreferenceValues.useTimersFeature(this).equals(Feature.Suggest)) {
                // timer buttons is already there as floating
                lShowIds.add(R.id.dyn_score_details);
            } else {
                lShowIds.add(R.id.dyn_timer);
            }
        } else {
            lShowIds.add(R.id.dyn_end_game);
        }
        if ( ListUtil.isNotEmpty(lShowIds) ) {
            setMenuItemVisibility(m_idOfVisibleActionBarItem, false);
            int iIdShow = lShowIds.remove(0);
            showNewMatchFloatButton(iIdShow == R.id.dyn_new_match);
            if ( iIdShow == R.id.dyn_new_match ) {
                if ( lShowIds.size() !=0 ) {
                    iIdShow = lShowIds.remove(0);
                }
            }

            setMenuItemVisibility(iIdShow, true);
            m_idOfVisibleActionBarItem = iIdShow;
        } else {
            m_idOfVisibleActionBarItem = 0;
        }

        boolean bShowUndo = (matchModel.isLocked() == false) && matchModel.hasStarted();
        setMenuItemVisibility(R.id.dyn_undo_last, bShowUndo);
    }
    private void setMenuItemVisibility(int iId, boolean bVisible) {
        boolean bShowTextInActionBar = PreferenceValues.showTextInActionBar(this);
        ViewUtil.setMenuItemVisibility(mainMenu, iId, bVisible, bShowTextInActionBar);
    }
    private void setMenuItemEnabled(int iId, boolean bEnabled) {
        setMenuItemsEnabled(new int[] { iId }, bEnabled);
    }
    private void setMenuItemsEnabled(int [] iIds, boolean bEnabled) {
        ViewUtil.setMenuItemsVisibility(mainMenu, iIds, bEnabled);
        //ViewUtil.setMenuItemsEnabled(mainMenu, iIds, bEnabled); // the disabled menu items do not appear very disabled visually... so hide them for now
    }

    @Override public void drawTouch(Direction direction, int id, int iColor) {
        MenuItem item = mainMenu.findItem(id);
        if ( item == null ) {
            return;
        }
        Drawable icon = item.getIcon();
        if ( icon == null ){
            return;
        }
        if ( iColor == Color.TRANSPARENT ) {
            icon.clearColorFilter();
        } else {
            ColorFilter colorFilter = new PorterDuffColorFilter(Color.BLUE, PorterDuff.Mode.SRC_ATOP);
            icon.setColorFilter(colorFilter);
        }
    }

    public boolean handleMenuItem(int id, Object... ctx) {
        switch (id) {
/*
            case R.id.sb_display_settings:
                this.startActivity(new Intent(android.provider.Settings.ACTION_DISPLAY_SETTINGS)); // api level 8 (android 3.1?)
                return true;
*/
            case R.id.android_language:
                Intent intent = new Intent(Settings.ACTION_LOCALE_SETTINGS);
                startActivity(intent);
                return true;
            case R.id.sb_settings:
                Intent settingsActivity = new Intent(getBaseContext(), Preferences.class);
                startActivity(settingsActivity);
                return true;
            case R.id.dyn_end_game:
            case R.id.end_game:
                if ( warnModelIsLocked() ) { return false; }
                if ( matchModel.isPossibleGameVictory() ) {
                    matchModel.endGame();
                } else {
                    areYouSureGameEnding();
                }
                persist(false);
                return true;
            case R.id.end_match:
                // TODO: present dialog for reason why: Conduct Match, Retired Because of Injury
                selectMatchEndReason();
                //matchModel.endMatch(EndMatchManuallyBecause.RetiredBecauseOfInjury);
                //persist(false);
                return true;
            case R.id.sb_toggle_demo_mode:
                DemoThread.DemoMessage demoMessage = null;
                if ( ListUtil.length (ctx) > 0  ) {
                    demoMessage = (DemoThread.DemoMessage) ctx[0];
                }
                toggleDemoMode(demoMessage);
                return true;
            case R.id.sb_swap_players:
                swapPlayers(Toast.LENGTH_LONG);
                return true;
            case R.id.sb_swap_double_players:
                swapDoublePlayers();
                return true;
            case R.id.sb_lock:
                LockState lockState = LockState.LockedManual;
                if ( ListUtil.length(ctx) != 0 ) {
                    lockState = (LockState) ctx[0];
                }
                matchModel.setLockState(lockState);
                return true;
            case R.id.sb_unlock:
                LockState current = matchModel.getLockState();
                LockState lsNew   = LockState.UnlockedManual;
                if ( (
                         (LockState.LockedIdleTime == current)
                      || (LockState.LockedEndOfMatchRetired == current)
                      || (LockState.LockedEndOfMatchConduct == current)
                     )
                     &&
                     (matchModel.matchHasEnded()==false)
                   ) {
                    lsNew = LockState.Unlocked;
                }
                matchModel.setLockState(lsNew);
                return true;
            case R.id.dyn_undo_last:
            case R.id.sb_undo_last:
                if ( warnModelIsLocked() ) { return false; }
                enableScoreButton(matchModel.getServer());
                matchModel.undoLast();
                bGameEndingHasBeenCancelledThisGame = false;
                return true;
            case R.id.sb_edit_event_or_player:
                editEventAndPlayers();
                return true;
            case R.id.sb_stored_matches: {
                ArchiveTabbed.SelectTab selectTab = ArchiveTabbed.SelectTab.Previous;
                if ( ctx != null && ctx.length==1 && ctx[0] instanceof ArchiveTabbed.SelectTab) {
                    selectTab = (ArchiveTabbed.SelectTab) ctx[0];
                }
                ArchiveTabbed.setDefaultTab(selectTab);
                Intent nm = new Intent(this, ArchiveTabbed.class);
                startActivityForResult(nm, 1); // see onActivityResult()
                return true;
            }
            case R.id.sb_select_feed_match: {
                MatchTabbed.setDefaultTab(MatchTabbed.SelectTab.Feed);
                Intent nm = new Intent(this, MatchTabbed.class);
                startActivityForResult(nm, 1); // see onActivityResult()
                return true;
            }
            case R.id.sb_select_static_match: {
                MatchTabbed.setDefaultTab(MatchTabbed.SelectTab.Mine);
                Intent nm = new Intent(this, MatchTabbed.class);
                startActivityForResult(nm, 1); // see onActivityResult()
                return true;
            }
            case R.id.sb_enter_singles_match: {
                MatchTabbed.setDefaultTab(MatchTabbed.SelectTab.Manual);
                Intent nm = new Intent(this, MatchTabbed.class);
                startActivityForResult(nm, 1); // see onActivityResult()
                return true;
            }
            case R.id.sb_enter_doubles_match: {
                MatchTabbed.setDefaultTab(MatchTabbed.SelectTab.ManualDbl);
                Intent nm = new Intent(this, MatchTabbed.class);
                startActivityForResult(nm, 1); // see onActivityResult()
                return true;
            }
            case R.id.float_new_match:
            case R.id.dyn_new_match: {
                cancelShowCase();
                Intent nm = new Intent(this, MatchTabbed.class);
                startActivityForResult(nm, 1); // see onActivityResult

                return true;
            }
            case R.id.sb_put_match_summary_on_clipboard:
                ContentUtil.placeOnClipboard(this, "squore summary", ResultSender.getMatchSummary(this, matchModel));
                return false;
            case R.id.sb_whatsapp_match_summary:
                shareMatchSummary(this, matchModel, "com.whatsapp", null);
                return false;
            case R.id.sb_twitter_match_summary:
                shareMatchSummary(this, matchModel, "com.twitter.android", null);
                return false;
            case R.id.sb_share_matches_summary:
                selectMatchesForSummary(this);
                return false;
            case R.id.sb_share_match_summary:
                shareMatchSummary(this, matchModel, null, null);
                return false;
            case R.id.sb_send_match_result:
                shareMatchSummary(this, matchModel, null, PreferenceValues.getDefaultSMSTo(this));
                return false;
            case R.id.sb_post_match_result:
                postMatchResult();
                return false;
            case R.id.sb_email_match_result:
                emailMatchResult(this, matchModel);
                return false;
            case R.id.cmd_import_settings:
                ExportImportPrefs.importSettings(this);
                // TODO: is a restart required, or only for the colors
                ColorPrefs.clearColorCache();
                initColors();
                initCountries();
                return true;
            case R.id.cmd_export_settings:
                ExportImportPrefs.exportSettings(this);
                return true;
            case R.id.cmd_import_matches:
                selectFilenameForImport();
                return true;
            case R.id.cmd_export_matches:
                selectFilenameForExport();
                return true;
            case R.id.float_match_share:
            case R.id.dyn_match_share:
            case R.id.sb_share_score_sheet:
                shareScoreSheet(this, matchModel, true);
                if ( shareButton != null ) {
                    shareButton.setHidden(true);
                }
                //shareMatchResult();
                return false;
            case R.id.dyn_score_details:
            case R.id.sb_score_details:
                showScoreHistory();
                return false;
            case R.id.sb_live_score:
                showLiveScore();
                return false;
            case R.id.sb_official_rules:
                showOfficialSquashRules();
                return false;
            case R.id.sb_quick_intro:
                boolean bFromMenu = (ListUtil.length(ctx) == 0);
                startShowCase(bFromMenu);
                return false;
            case R.id.sb_change_log:
                addRecentFeedURLs();
                showChangeLog();
                return false;
            case R.id.sb_credits:
                showCredits();
                return false;
            case R.id.sb_adjust_score:
                if ( warnModelIsLocked() ) { return false; }
                adjustScore();
                return true;
            case R.id.sb_clear_score:
                if ( warnModelIsLocked() ) { return false; }
                if ( matchModel.hasStarted() ) {
                    confirmRestart();
                } else {
                    initScoreBoard(null);
                }
                return true;
            case R.id.change_match_format:
                editMatchFormatDialog();
                break;
            case R.id.sb_match_format:
                showMatchFormatDialog();
                break;
            case R.id.float_timer:
            case R.id.dyn_timer:
            case R.id.sb_timer:
                cancelTimer();
                if ( matchModel.hasStarted() ) {
                    lastTimerType = Type.UntillStartOfNextGame;
                    if ( Brand.isTabletennis() ) {
                        int iScore = matchModel.getTotalGamePoints();
                        if ( matchModel.gameHasStarted() && (matchModel.isPossibleGameVictory() == false) ) {
                            if ( iScore % PreferenceValues.autoShowGamePausedDialogAfterXPoints(this) == 0 ) {
                                lastTimerType = Type.TowelingDown;
                            }
                        }
                    }
                } else {
                    // toggle between the 2 with Warmup first
                    lastTimerType = Type.Warmup.equals(lastTimerType)?Type.UntillStartOfNextGame:Type.Warmup;
                }
                showTimer(lastTimerType, false);
                return true;
            case R.id.sb_injury_timer:
                _showInjuryTypeChooser();
                return true;
            case R.id.tt_activate_mode: // fall through
            case R.id.tt_deactivate_mode:
                if ( Brand.isTabletennis() ) {
                    boolean bActivate = id == R.id.tt_activate_mode;
                    matchModel.setMode(bActivate ? TabletennisModel.Mode.Expedite : null);

                    // hide just clicked menu item
                    setMenuItemVisibility(id, false);
                    // make 'counter-part' menu visible
                    setMenuItemVisibility(bActivate ? R.id.tt_deactivate_mode : R.id.tt_activate_mode, true);

                    if ( bActivate ) {
                        // we will no respond to clicks on the serve side button
                        PreferenceValues.setOverwrite(PreferenceKeys.serveButtonTransparencyNonServer, "0");
                    } else {
                        PreferenceValues.removeOverwrite(PreferenceKeys.serveButtonTransparencyNonServer);
                    }
                }
                return true;
            case R.id.sb_toss:
            case R.id.float_toss:
                _showWhoServesDialog();
                return false;
            case R.id.sb_official_announcement:
                _showOfficialAnnouncement(AnnouncementTrigger.Manual, true);
                return false;
            case R.id.sb_privacy_and_terms:
                showPrivacyAndTerms();
                return false;
            case R.id.sb_about:
                showAbout();
                return false;
            case R.id.sb_feedback:
                Intent feedBack = new Intent(getBaseContext(), Feedback.class);
                startActivity(feedBack);
                return false;
            case R.id.sb_possible_conductsA:
            case R.id.sb_possible_conductsB:
                Intent conducts = new Intent(getBaseContext(), ConductInfo.class);
                startActivity(conducts);
                return false;
            case R.id.sb_download_zip:
                downloadMatchesInZip((String) ctx[0], (ZipType) ctx[1]);
                return false;
            case R.id.sb_download_posted_to_squore_matches:
                // http://squore.double-yellow.be/make.matches.zip.php
                handleMenuItem(R.id.sb_download_zip, URLFeedTask.prefixWithBaseIfRequired("/matches.zip"), ZipType.SquoreAll);
                break;
            case R.id.sb_demo:
                restartScore();
                setModus(null, Mode.FullAutomatedDemo);
                return true;
            case R.id.sb_help:
                showHelp();
                return true;
            case R.id.sb_exit:
                persist(true);
                System.exit(0);
                return true;
            case android.R.id.home: {
                if ( drawerLayout != null ) {
                    if (drawerLayout.isDrawerOpen(drawerView)) {
                        drawerLayout.closeDrawer(drawerView);
                    } else {
                        drawerLayout.openDrawer(drawerView);
                    }
                } else {
                    // so that clicking on icon also allows for selecting new match
                    Intent sm = new Intent(this, MatchTabbed.class);
                    startActivityForResult(sm, 1); // see onActivityResult
                }
                return false;
            }
            default:
                Log.w(TAG, "Unhandled int " + id);
                if ( id != 0) {
                    try {
                        String s = getResources().getResourceName(id);
                        Log.w(TAG, "Unhandled " + s);
                    } catch (Resources.NotFoundException e) {
                    }
                }
                break;
        }
        return false;
    }

    public static Uri buildURL(Context context, String helpUrl, boolean bAddLocaleAsParam) {
        if ( bAddLocaleAsParam ) {
            boolean bUseAmpersand = helpUrl.contains("?");
            return Uri.parse(helpUrl + (bUseAmpersand ? "&" : "?") + "lang=" + RWValues.getDeviceLanguage(context));
        }
        return Uri.parse(helpUrl);
    }

    private void selectFilenameForExport() {
        Export export = new Export(this, null, null);
        show(export);
    }
    private void selectFilenameForImport() {
        Import dImport = new Import(this, null, null);
        show(dImport);
    }
    private void showScoreHistory() {
        persist(false);

        Intent matchHistory = new Intent(this, MatchHistory.class);
        startActivity(matchHistory);
    }

    public static Evaluation.ValidationResult brandedVersionValidation = Evaluation.ValidationResult.OK;

    private boolean warnModelIsLocked(boolean bIgnoreForConduct) {
        if ( (brandedVersionValidation != Evaluation.ValidationResult.OK) && (brandedVersionValidation != Evaluation.ValidationResult.NoDate) ) {
            iBoard.showToast(String.format("Sorry, %s can no longer be used (%s)", Brand.getShortName(this), brandedVersionValidation));
            return true;
        }
        if ( matchModel.isLocked() == false ) { return false; }
        if ( bIgnoreForConduct ) {
            if ( matchModel.getLockState().AllowRecordingConduct() ) {
                return false;
            }
        }
        if ( matchModel.getLockState().equals(LockState.LockedManualGUI)) {
            iBoard.showToast(R.string.match_is__locked);
        } else {
            LockedMatch lockedMatch = new LockedMatch(this, matchModel, this);
            show(lockedMatch);
        }
        return true;
    }

    private boolean warnModelIsLocked(){
        return warnModelIsLocked(false);
    }

    private void showAppeal(Player appealingPlayer) {
        if ( Brand.isNotSquash() ) { return; }
        if ( warnModelIsLocked() ) { return; }
        Appeal appeal = new Appeal(this, matchModel, this);
        appeal.init(appealingPlayer);
        show(appeal);
    }

    private void showRallyEndStats(Player scoringPlayer, Call call) {
        RallyEndStats rallyEndStats = new RallyEndStats(this, matchModel, this);
        rallyEndStats.init(scoringPlayer, call);
        addToDialogStack(rallyEndStats);
    }

    private void showActivateMode(String sMode) {
        ActivateMode activateMode = new ActivateMode(this, matchModel, this);
        //activateMode.init(sMode);
        addToDialogStack(activateMode);
    }

    private void showConduct(Player misbehavingPlayer) {
        if ( Brand.isNotSquash() ) { return; }
        if ( warnModelIsLocked(true) ) { return; }
        Conduct conduct = new Conduct(this, matchModel, this);
        conduct.init(misbehavingPlayer);
        show(conduct);
    }

    private void showBrokenEquipment(Player asking) {
        //if ( Brand.isNotSquash() ) { return; }
        if ( warnModelIsLocked() ) { return; }
        BrokenWhat brokenWhat = new BrokenWhat(this, matchModel, this);
        brokenWhat.init(asking);
        show(brokenWhat);
    }

    private void showColorPicker(Player p) {
        ColorPicker colorPicker = new ColorPicker(this, matchModel, this);
        colorPicker.init(p, matchModel.getColor(p));
        show(colorPicker);
    }

    private void openInBrowser(String url) {
        try {
            url = URLFeedTask.prefixWithBaseIfRequired(url);
            Uri uri = buildURL(this, url,true);
            Intent launchBrowser = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(launchBrowser);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage()); // e.g. only 'browser' app has been disabled by user
            Toast.makeText(this, "Sorry, unable to open browser with \n" + url, Toast.LENGTH_LONG).show();
        }
    }

    private void showOfficialSquashRules() {
        openInBrowser(PreferenceValues.getOfficialSquashRulesURL(this));
    }

    private void showLiveScore() {
        openInBrowser(getString(R.string.pref_live_score_url));
    }
    private void showHelp() {
        openInBrowser("/help");
    }

    /** To share to 'Live' score on a regular basis without user interaction */
    private void autoShare() {
        if ( PreferenceValues.useShareFeature(this).equals(Feature.Automatic) == false ) {
            return;
        }
        ShareMatchPrefs prefs = PreferenceValues.getShareAction(this);
        if ( prefs.equals(ShareMatchPrefs.PostResult) && PreferenceValues.autoSuggestToPostResult(this) ) {
            // ensure dialog to 'suggest' posting is NOT also triggered
            PreferenceValues.setOverwrite(PreferenceKeys.autoSuggestToPostResult, false);
        }
        ChildActivity ca = new ChildActivity(this, null, this);
        ca.init(prefs.getMenuId());
        addToDialogStack(ca);
    }
    private void autoShowGameDetails() {
        if ( PreferenceValues.showDetailsAtEndOfGameAutomatically(this) == false ) { return; }
        ChildActivity ca = new ChildActivity(this, null, this);
        ca.init(R.id.sb_score_details);
        addToDialogStack(ca);
    }

    private void autoShowTimer(Type timerType) {
        if ( PreferenceValues.useTimersFeature(this).equals(Feature.Automatic) == false) {
            return;
        }
        TwoTimerView twoTimerView = getTwoTimerView(timerType);
        twoTimerView.init(true);
        addToDialogStack(twoTimerView);
    }

    private TwoTimerView getTwoTimerView(Type timerType) {
        TwoTimerView twoTimerView = null;
        switch (timerType) {
            case Warmup:
                twoTimerView = new WarmupTimerView(this,matchModel);
                break;
            case Timeout:
            case TowelingDown:
                twoTimerView = new GamePausedTimerView(this, matchModel);
                break;
            case UntillStartOfNextGame:
                twoTimerView = new PauseTimerView(this, matchModel);
                break;
            case ContributedInjury:
            case OpponentInflictedInjury:
            case SelfInflictedInjury:
                twoTimerView = new InjuryTimerView(this, matchModel, timerType);
                break;
        }
        return twoTimerView;
    }

    private static Type lastTimerType = null;

    /** invoked for menu item, or closing injury type dialog */
    private void showTimer(Type timerType, boolean bAutoTriggered) {
        TwoTimerView twoTimerView = getTwoTimerView(timerType);
        twoTimerView.init(bAutoTriggered);
        addToDialogStack(twoTimerView);

        // ensure the match shows up in the list of live score a.s.a.p. so e.g. when warmup timer is started
        ShareMatchPrefs liveScoreShare = PreferenceValues.isConfiguredForLiveScore(ScoreBoard.this);
        if ( ( matchModel.hasStarted() == false ) && ShareMatchPrefs.LinkWithFullDetailsEachPoint.equals(liveScoreShare) && (matchModel.isLocked() == false) ) {
            shareScoreSheetDelayed(1000);
        }
    }

    public void _showTimer(Type timerType, boolean bAutoTriggered) {
        int iInitialSecs = PreferenceValues.getInteger(timerType.getPrefKey(), this, timerType.getDefaultSecs());
        boolean bIsResume = (timer != null);
        int iResumeAt = 0;
        if ( bIsResume ) {
            iResumeAt = timer.getSecondsLeft();
        } else {
            // it is not a resume
            bIsResume = false;
            iResumeAt = iInitialSecs;
        }
        if ( timer == null ) {
            switch (timerType) {
                case Warmup:
                    timer = new Timer(this, timerType, iInitialSecs, iResumeAt, iInitialSecs / 2, bAutoTriggered);
                    break;
                case UntillStartOfNextGame:
                    if (matchModel.matchHasEnded() /*&& (PreferenceValues.showOfficialAnnouncements(this)==false)*/) {
                        iBoard.showToast(R.string.match_has_finished);
                        return;
                    }
                    timer = new Timer(this, timerType, iInitialSecs, iResumeAt, /*iSeconds/6*/ 15, bAutoTriggered);
                    break;
                case SelfInflictedInjury: // fall through
                case ContributedInjury: // fall through
                case OpponentInflictedInjury:
                    // injury
                    timer = new Timer(this, timerType, iInitialSecs, iResumeAt, 15 /*Math.max(15,iInitialSecs/6)*/, bAutoTriggered);
                    break;
                case Timeout:  // fall through
                case TowelingDown:
                    // injury
                    timer = new Timer(this, timerType, iInitialSecs, iResumeAt, 0 /*Math.max(15,iInitialSecs/6)*/, bAutoTriggered);
                    iBoard.stopGameDurationChrono();
                    break;
            }
        }
        ViewType viewType = PreferenceValues.timerViewType(this);
        Timer.addTimerView(iBoard.isPresentation(), iBoard); // always add this one, so it is always up to date if the Popup one is made hidden
        if ( viewType.equals(ViewType.Popup) ) {
            Timer.addTimerView(false, getDialogTimerView()); // this will actually trigger the dialog to open
        }

        if ( matchModel.getName(Player.A).equals("Iddo T") && isLandscape() ) {
            // for now this is just for me to be able to view the layout on the device while no chromecast available
            getXActionBar().hide();
            if ( m_endOfGameView == null ) {
                m_endOfGameView = new EndOfGameView(this, null /*iBoard*/, matchModel);
            }
            ViewGroup presentationFrame = (ViewGroup) findViewById(R.id.presentation_frame);
            m_endOfGameView.show(presentationFrame);
            presentationFrame.setVisibility(View.VISIBLE);
            findViewById(R.id.content_frame     ).setVisibility(View.GONE);
        }

        //timer.show();

        if ( bIsResume == false ) {
            triggerEvent(SBEvent.timerStarted, viewType);
        }
    }
    private EndOfGameView m_endOfGameView = null;

    // ------------------------------------------------------
    // official announcements
    // ------------------------------------------------------

    private void _showInjuryTypeChooser() {
        InjuryType injuryType = new InjuryType(this, matchModel, this);
        show(injuryType);
    }

    private void autoShowTossDialog() {
        if ( matchModel.hasStarted() )                                                  { return; }
        if ( PreferenceValues.useTossFeature(this).equals(Feature.Automatic) == false ) { return; }

        _showWhoServesDialog();
    }

    private boolean _showWhoServesDialog() {
        ServerToss serverToss = new ServerToss(this, matchModel, this);
        addToDialogStack(serverToss);
        return true;
    }

    private void autoShowOfficialAnnouncement(AnnouncementTrigger trigger) {
        if ( PreferenceValues.useOfficialAnnouncementsFeature(this).equals(Feature.Automatic) == false ) {
            return;
        }
        _showOfficialAnnouncement(trigger, false);
    }

    /** Official Announcement Auto Triggered When */
    public enum AnnouncementTrigger {
        StartOfGame,
        EndOfGame,
        EndOfMatch,
        TieBreakDecisionRequired,
        TieBreak,
        /** @deprecated */
        Manual,
    }
    private void _showOfficialAnnouncement(AnnouncementTrigger trigger, boolean bManuallyRequested) {
        if ( matchModel.gameHasStarted()==false || matchModel.isPossibleGameVictory() ) {
            _showOfficialStartOrEndOfGameAnnouncement(trigger, bManuallyRequested);
        } else if ( matchModel.isStartOfTieBreak() ) {
            _showTieBreakDialog(trigger, matchModel.getTiebreakOccurence() );
        } else {
            if ( trigger == null ) {
                // show toast that the score is so that no official announcement is applicable
                iBoard.showToast("Sorry. No official announcement applicable for current score…");
            }
        }
    }

    private void _showOfficialStartOrEndOfGameAnnouncement(AnnouncementTrigger trigger, boolean bManuallyRequested) {
        //boolean bShowWithNoMoreCheckBox = false; //bAutoTriggered && PreferenceValues.showOfficialAnnouncements(this) && (PreferenceValues.getRunCount(this, PreferenceKeys.showOfficalAnnouncements) < 5);

        if ( bManuallyRequested /*AnnouncementTrigger.Manual.equals(trigger)*/ ) {
            if ( dialogManager.dismissIfTwoTimerView() ){
                cancelTimer();
            }
        }

        StartEndAnnouncement startEndAnnouncement;
        switch (trigger) {
            case StartOfGame:
                startEndAnnouncement = new StartGameAnnouncement(this, matchModel, this);
                break;
            case EndOfGame:
                startEndAnnouncement = new EndGameAnnouncement(this, matchModel, this);
                break;
            case EndOfMatch:
                startEndAnnouncement = new EndMatchAnnouncement(this, matchModel, this);
                break;
            default:
                if ( bManuallyRequested ) {
                    startEndAnnouncement = new StartEndAnnouncement(this, matchModel, this); // TODO: also instantiate the proper subclass here in make base class abstract
                } else {
                    // should not happen
                    return;
                }
        }
        startEndAnnouncement.init(trigger);
        addToDialogStack(startEndAnnouncement);
    }

    private void showMatchFormatDialog() {
        MatchInfo matchInfo = new MatchInfo(this, matchModel, this);
        show(matchInfo);
    }
    private void editMatchFormatDialog() {
        EditFormat editFormat = new EditFormat(this, matchModel, this);
        addToDialogStack(editFormat);
    }

    private boolean autoShowTieBreakDialog(int iOccurrenceCount) {
        TieBreakFormat tbf = PreferenceValues.getTiebreakFormat(this);

        AnnouncementTrigger when = AnnouncementTrigger.TieBreak;
        if ( tbf.needsTwoClearPoints() ) {
            // no decision required: only show if official announcements
            if ( PreferenceValues.useOfficialAnnouncementsFeature(this).equals(Feature.Automatic)== false ) {
                return false;
            }
        } else {
            // a decision is need, so show automatic no mather what
            when = AnnouncementTrigger.TieBreakDecisionRequired;
        }
        return _showTieBreakDialog(when, iOccurrenceCount);
    }


    private boolean _showTieBreakDialog(AnnouncementTrigger trigger, int iOccurrenceCount) {
        TieBreak tieBreak = new TieBreak(this, matchModel, this);
        tieBreak.init(iOccurrenceCount);
        addToDialogStack(tieBreak);
        return true;
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //fbOnActivityResult(requestCode, resultCode, data);
        if ( data == null) { return; }

        if ( StringUtil.isInteger(data.getAction()) ) {
            // most likely back was pressed after selecting match from list... go back to the list to select a different match
            int iAction = Integer.parseInt(data.getAction());
            handleMenuItem(iAction);
            return;
        }

        Bundle extras = data.getExtras();
        if ( extras != null ) {
            {
                // returning from MatchTabbed or Match
                final String sNewMatch = Model.class.getSimpleName();
                if ( extras.containsKey(sNewMatch) ) {
                    String sJson = extras.getString(sNewMatch);
                    Model m = Brand.getModel();
                    m.fromJsonString(sJson);

                    if ( Match.showAfterMatchSelection() ) {
                        // match selected with single click from list of matches
                        String sSource = m.getSource();
                        if ( StringUtil.isNotEmpty(sSource) ) {
                            if ( sSource.startsWith("http") || sSource.contains("tournamentsoftware") ) {
                                // user is allowed to specify to turn this off in a section of MatchView
                                PreferenceValues.initForLiveScoring(this, true);
                            }
                        }

                        // now let user to specify match format
                        Intent nm = new Intent(this, Match.class);
                        nm.putExtra(Model.class.getSimpleName(), sJson);
                        startActivityForResult(nm, 1);
                        return;
                    }
                    PreferenceValues.setNumber  (PreferenceKeys.numberOfPointsToWinGame, this, m.getNrOfPointsToWinGame());
                    PreferenceValues.setNumber  (PreferenceKeys.numberOfGamesToWinMatch, this, m.getNrOfGamesToWinMatch());
                    PreferenceValues.setNumber  (PreferenceKeys.numberOfServesPerPlayer, this, m.getNrOfServesPerPlayer());
                    PreferenceValues.setBoolean (PreferenceKeys.useHandInHandOutScoring, this, m.isEnglishScoring());
                    PreferenceValues.setEnum    (PreferenceKeys.tieBreakFormat         , this, m.getTiebreakFormat());
                    PreferenceValues.setString  (PreferenceKeys.eventLast              , this, m.getEventName());
                    PreferenceValues.setString  (PreferenceKeys.divisionLast           , this, m.getEventDivision());
                    PreferenceValues.setString  (PreferenceKeys.roundLast              , this, m.getEventRound());
                    PreferenceValues.setString  (PreferenceKeys.courtLast              , this, m.getCourt());
                    PreferenceValues.setString  (PreferenceKeys.locationLast           , this, m.getEventLocation());
                    PreferenceValues.setEnum    (PreferenceKeys.handicapFormat         , this, m.getHandicapFormat());
                    if ( Brand.isSquash() ) {
                        DoublesServeSequence eDSS = m.getDoubleServeSequence();
                        if ( eDSS.equals(DoublesServeSequence.NA) == false ) {
                            PreferenceValues.setEnum(PreferenceKeys.doublesServeSequence, this, eDSS);
                        }
                    }
                    if ( Brand.isTabletennis() ) {
                        ServeButton.setMaxForCountDown(m.getNrOfServesPerPlayer());
                        if ( Player.B.equals(IBoard.m_firstPlayerOnScreen) ) {
                            IBoard.togglePlayer2ScreenElements();
                        }
                    }
                    if ( MapUtil.isNotEmpty(RWValues.getOverwritten() ) ) {
                        Log.w(TAG, "remaining overwrites " + RWValues.getOverwritten());
                        //PreferenceValues.removeOverwrites(FeedMatchSelector.mFeedPrefOverwrites);
                    }
                    m.registerListeners(matchModel);
                    matchModel = m;
                    setPlayerNames(new String[] { matchModel.getName(Player.A), matchModel.getName(Player.B) });

                    initScoreBoard(null);
                }
            }
            {
                final String sOldMatch = PreviousMatchSelector.class.getSimpleName();
                if ( extras.containsKey( sOldMatch ) ) {
                    File f = (File) extras.get(sOldMatch);
                    if ( f == null ) { return; }

                    matchModel = null;
                    initScoreBoard(f);

                    // ensure selected match is also LAST_MATCH file
                    FileUtil.copyFile(f, getLastMatchFile(this));
                }
            }
        }
    }

    public boolean setPlayerNames(String[] saPlayers) {
        return setPlayerNames(saPlayers, true);
    }
    private boolean setPlayerNames(String[] saPlayers, boolean bAddToPrefList) {
        int i = 0;
        boolean bChanged = false;
        for(Player player: Model.getPlayers()) {
            String sPlayer = saPlayers[i++];
            if ( StringUtil.isEmpty(sPlayer) ) {
                // most likely parsing names from some feed failed
                continue;
            }
            sPlayer = PreferenceValues.capitalizePlayerName(sPlayer.trim());
            bChanged = matchModel.setPlayerName(player, sPlayer) || bChanged;
            if ( bAddToPrefList ) {
                PreferenceValues.addPlayerToList(this, sPlayer);
            }
        }
        initPlayerButtons();
        return bChanged;
    }

    // ------------------------------------------------------
    // share match result
    // ------------------------------------------------------

    /** Sending match result as SMS message to e.g. the boxmaster */
    public static void shareMatchSummary(Context context, Model matchModel, String sPackage, String sDefaultRecipient) {
        ResultSender resultSender = new ResultSender();
        resultSender.send(context, matchModel, sPackage, sDefaultRecipient);
    }
    private void selectMatchesForSummary(Context context) {
        handleMenuItem(R.id.sb_stored_matches, ArchiveTabbed.SelectTab.PreviousMultiSelect);
    }

    public static void emailMatchResult(Context context, Model matchModel) {
        ResultMailer resultMailer = new ResultMailer();
        boolean bHtml = PreferenceValues.mailFullScoringSheet(context);
        resultMailer.mail(context, matchModel, bHtml);
    }

    private void postMatchResult() {
        final String feedPostName = PreferenceValues.getFeedPostName(this);
        final Authentication authentication = PreferenceValues.getFeedPostAuthentication(this);
        if ( authentication != null ) {
            switch (authentication) {
                case Basic:
                    UsernamePassword usernamePassword = new UsernamePassword(this, this);
                    usernamePassword.init(feedPostName, authentication);
                    dialogManager.show(usernamePassword);
                    break;
                case None:
                    postMatchResult(null, null, null);
                    break;
            }
        } else {
            postMatchResult(null, null, null);
        }
    }
    public void postMatchResult(Authentication authentication, String sUserName, String sPassword) {
        ResultPoster resultPoster = initResultPoster();
        if ( resultPoster != null ) {
            showProgress(R.string.posting);
            resultPoster.post(this, matchModel, authentication, sUserName, sPassword);
        }
    }

    private class DelayedModelPoster extends CountDownTimer {
        private DelayedModelPoster(long iMilliSeconds) {
            super(iMilliSeconds, 600);
        }
        @Override public void onTick(long millisUntilFinished) {
            //Log.d(TAG, "Waiting ... " + millisUntilFinished);
        }
        @Override public void onFinish() {
            Log.d(TAG, "Posting ... ");
            shareScoreSheet(ScoreBoard.this, matchModel, false);
        }
    }
    private DelayedModelPoster m_delayedModelPoster = null;
    private synchronized void shareScoreSheetDelayed(int iMilliSeconds) {
        if ( m_delayedModelPoster != null ) {
            m_delayedModelPoster.cancel();
        }
        m_delayedModelPoster = new DelayedModelPoster(iMilliSeconds);
        m_delayedModelPoster.start();
    }
    private static boolean m_bShareStarted_DemoThread = false;
    /** invoked on: GameEndReached, GameEnded, FirstPointChange, GameIsHalfway, ScoreChange */
    public static void shareScoreSheet(Context context, Model matchModel, boolean bAllowEndGameIfApplicable) {
        Player possibleMatchVictoryFor = matchModel.isPossibleMatchVictoryFor();
        if ( bAllowEndGameIfApplicable ) {
/*
            if ( matchModel.isPossibleGameVictory() ) {
                // why is this done again? Comment out for now 20170713, only do for last game (code below)
                matchModel.endGame(false); // don't notify listeners: might popup a disturbing dialog like 'Post to site?'
            }
*/
            if ( possibleMatchVictoryFor != null ) {
                matchModel.endGame(false); // don't notify listeners: might popup a disturbing dialog like 'Post to site?'
            }
        }
        MatchModelPoster matchModelPoster = new MatchModelPoster();
        m_bShareStarted_DemoThread = true;
        if ( possibleMatchVictoryFor != null ) {
            // only create settings if match is finished... ensure in between updates are just a little smaller/faster
            JSONObject oSettings = new JSONObject();

            // add colors section to the settings
            try {
                Map<ColorPrefs.ColorTarget, Integer> target2colorMapping = ColorPrefs.getTarget2colorMapping(context);
                JSONObject oColors = new JSONObject();
                for(ColorPrefs.ColorTarget ct: ColorPrefs.ColorTarget.values()) {
                    switch (ct) {
                        case black:
                        case white:
                            // do not put the obvious in the posted json string
                            continue;
                        default:
                            break;
                    }
                    String  colorKey = ct.toString().replaceAll("[Cc]olor", "");
                    Integer iColor   = target2colorMapping.get(ct);
                    oColors.put(colorKey, ColorUtil.getRGBString(iColor));

                }
                oSettings.put("colors", oColors);

                oSettings.put("css", ColorPrefs.getColorPrefsAsCss(context));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            matchModelPoster.post(context, matchModel, oSettings);
        } else {
            matchModelPoster.post(context, matchModel, null);
        }
    }

    // ------------------------------------------------------
    // dialog boxes
    // ------------------------------------------------------

    //private static final int iDialogTheme = android.R.style.Theme_Translucent_NoTitleBar_Fullscreen; // this does not work, we need to specify a theme, not a style
    //private static final int iDialogTheme = R.style.SBDialog;
    //private static final int iDialogTheme = android.R.style.Theme_Dialog;
    private static final int iDialogTheme = AlertDialog.THEME_TRADITIONAL;   // white titles on black background, spacing around buttons
    //private static final int iDialogTheme = AlertDialog.THEME_HOLO_LIGHT;  // blue titles on white background, no spacing around buttons
    //private static final int iDialogTheme = AlertDialog.THEME_HOLO_DARK;   // blue titles on dark grey background, no spacing around buttons
    //private static final int iDialogTheme = AlertDialog.THEME_DEVICE_DEFAULT_LIGHT; // dark titles on light background, no spacing around buttons/ buttons borders not visible
    //private static final int iDialogTheme = AlertDialog.THEME_DEVICE_DEFAULT_DARK; // white titles on dark grey background, no spacing around buttons/ buttons borders not visible
    public static MyDialogBuilder getAlertDialogBuilder(Context context) {
        //context = new ContextThemeWrapper(context, iDialogTheme);
        //return new AlertDialog.Builder(context, iDialogTheme);
        return new MyDialogBuilder(context);
        //return new AlertDialog.Builder(context/*, iDialogTheme*/);
    }

    /**
     * Introduced to apply some color to e.g. the Dialog title.
     */
    public static class MyDialogBuilder extends AlertDialog.Builder {
        private Map<ColorPrefs.ColorTarget, Integer> target2colorMapping;

        MyDialogBuilder(Context context) {
            super(context, iDialogTheme);
            target2colorMapping = ColorPrefs.getTarget2colorMapping(getContext());
        }

        @Override public AlertDialog.Builder setTitle(int titleId) {
            return this.setTitle(getContext().getString(titleId));
        }

        @Override public AlertDialog.Builder setTitle(CharSequence sTitle) {
            if ( target2colorMapping != null ) {
                Integer newColor = target2colorMapping.get(ColorPrefs.ColorTarget.middlest);
                String  sColor   = ColorUtil.getRGBString(newColor);
                long iDistanceToBlack = ColorUtil.getDistance2Black(sColor);
                if ( iDistanceToBlack < 50 ) {
                    // e.g. when using monochrome black
                    sColor = "#FFFFFF";
                }
                sTitle = Html.fromHtml("<font color='" + sColor + "'>" + sTitle + "</font>");
            }
            AlertDialog.Builder builder = super.setTitle(sTitle);
            return builder;
        }

        @Override public AlertDialog.Builder setIcon(int iconId) {
            AlertDialog.Builder builder = super.setIcon(iconId);
            return builder;
        }

        @Override public AlertDialog create() {
            Log.w(TAG, "Try to use show() with listener if possible");
            AlertDialog dialog = super.create();
            return dialog;
        }

        @Override public AlertDialog show() {
            ButtonUpdater listener = new ButtonUpdater(getContext());
            return this.show(listener);
        }

        public AlertDialog show(DialogInterface.OnShowListener onShowListener) {
            AlertDialog dialog = super.create();
            dialog.setOnShowListener(onShowListener);
            try {
                dialog.show();
            } catch (Exception e) {
                e.printStackTrace();
/* IH 20180322: try catch to prevent crash for following exception (reported for apk 183 on android 5.0 and 7.0)
                android.view.WindowManager$BadTokenException:
                at android.view.ViewRootImpl.setView (ViewRootImpl.java:922)
                at android.view.WindowManagerGlobal.addView (WindowManagerGlobal.java:377)
                at android.view.WindowManagerImpl.addView (WindowManagerImpl.java:105)
                at android.app.Dialog.show (Dialog.java:404)
                at com.doubleyellow.scoreboard.main.ScoreBoard$MyDialogBuilder.show (ScoreBoard.java:4018)
                at com.doubleyellow.scoreboard.main.ScoreBoard$MyDialogBuilder.show (ScoreBoard.java:4012)
                at com.doubleyellow.scoreboard.dialog.EditFormat.show (EditFormat.java:128)
                at com.doubleyellow.scoreboard.main.DialogManager.showNextDialog (DialogManager.java:116)
                at com.doubleyellow.scoreboard.main.ScoreBoard.showNextDialog (ScoreBoard.java:4061)
                at com.doubleyellow.scoreboard.main.ScoreBoard.triggerEvent (ScoreBoard.java:2812)
                at com.doubleyellow.scoreboard.timer.Timer$SBCountDownTimer.onFinish (Timer.java:242)            }
                at android.os.CountDownTimer$1.handleMessage (CountDownTimer.java:127)
*/
            }
            return dialog;
        }
    }
    public static AlertDialog dialogWithOkOnly(Context context, String sMsg) {
        return dialogWithOkOnly(context, null, sMsg, false);
    }
    public static AlertDialog dialogWithOkOnly(Context context, String sTitle, String sMsg, boolean bAlert) {
        AlertDialog.Builder ab = getAlertDialogBuilder(context);
        ab.setPositiveButton(android.R.string.ok, null);

        if ( StringUtil.isNotEmpty(sTitle) ) {
            ab.setTitle(sTitle);
            ab.setIcon(bAlert? android.R.drawable.ic_dialog_alert: android.R.drawable.ic_dialog_info);
        }
        if ( StringUtil.isNotEmpty(sMsg) ) {
            if( sMsg.trim().toLowerCase().endsWith("html>") ) {
                WebView wv = new WebView(context);
                wv.loadData(sMsg, "text/html; charset=utf-8", "UTF-8");
                ab.setView(wv);
            } else {
                ab.setMessage(sMsg);
            }
        }

        try {
            return ab.show();
        } catch (Exception e) {
/* IH 20170607: try catch to prevent crash for following exception (reported for apk 144 on android 5.0 and 7.0)
            android.view.WindowManager$BadTokenException:
            at android.view.ViewRootImpl.setView(ViewRootImpl.java:570)
            at android.view.WindowManagerGlobal.addView(WindowManagerGlobal.java:272)
            at android.view.WindowManagerImpl.addView(WindowManagerImpl.java:69)
            at android.app.Dialog.show(Dialog.java:298)
            at android.app.AlertDialog$Builder.show(AlertDialog.java:987)
            at com.doubleyellow.scoreboard.main.ScoreBoard.dialogWithOkOnly(ScoreBoard.java:3547)
*/
            return null;
        }
    }

    DialogManager dialogManager = null;
    private synchronized void showNextDialog() {
        dialogManager.showNextDialog();
    }
    public boolean isDialogShowing() {
        return dialogManager.isDialogShowing();
    }
    private synchronized boolean addToDialogStack(BaseAlertDialog dialog) {
        return dialogManager.addToDialogStack(dialog);
    }
    private synchronized void show(BaseAlertDialog dialog) {
        dialogManager.show(dialog);
    }
    private void showChangeLog() {
        ChangeLog changeLog = new ChangeLog(this);
        show(changeLog);
    }

    private void showCredits() {
        Credits credits = new Credits(this);
        show(credits);
    }

    private void editEventAndPlayers() {
        EditPlayers editPlayers = new EditPlayers(this, matchModel, this);
        show(editPlayers);
    }

    /** If you start referee-ing a match that was already in progress */
    private void adjustScore() {
        AdjustScore adjustScore = new AdjustScore(this, matchModel, this);
        show(adjustScore);
    }

    private void autoShowHandicap() {
        if ( matchModel.isUsingHandicap() == false ) { return; }
        HandicapFormat handicapFormat = matchModel.getHandicapFormat();
        if ( handicapFormat.equals(HandicapFormat.SameForAllGames)      && matchModel.hasStarted()    ) { return; }
        if ( handicapFormat.equals(HandicapFormat.DifferentForAllGames) && matchModel.gameHasStarted()) { return; }
        adjustHandicap();
    }

    private void adjustHandicap() {
        Handicap handicap = new Handicap(this, matchModel, this);
        handicap.init(matchModel.getHandicapFormat(), matchModel.getNrOfPointsToWinGame());
        addToDialogStack(handicap);
    }

    private void showAbout() {
        About about = new About(this, matchModel, this);
        show(about);
    }

    private void showPrivacyAndTerms() {
        PrivacyAndTerms about = new PrivacyAndTerms(this);
        show(about);
    }

    public void confirmRestart() {
        RestartScore d = new RestartScore(this, matchModel, this);
        show(d);
    }

    public void restartScore() {
        cancelTimer();
        initScoreBoard(null);
        matchModel.setDirty();
    }

    /** If it has been cancelled and no 'undo' has been done, assume that match format 'end score' was accidentally chosen to low */
    public boolean bGameEndingHasBeenCancelledThisGame = false;
    private boolean _confirmGameEnding(Player winner) {
        // only suggest once per game
        if ( bGameEndingHasBeenCancelledThisGame == true ) { return false; }

        EndGame endGame = new EndGame(this, matchModel, this);
        endGame.init(winner);
        return addToDialogStack(endGame);
    }
    private void areYouSureGameEnding() {
        EndGameChoice endGame = new EndGameChoice(this, matchModel, this);
        addToDialogStack(endGame);
    }
    private void selectMatchEndReason() {
        if ( warnModelIsLocked() ) { return; }
        EndMatchChoice endMatch = new EndMatchChoice(this, matchModel, this);
        addToDialogStack(endMatch);
    }

/*
    private boolean bNonIntrusiveGameEndingPerformed = false;
    private boolean nonIntrusiveGameEnding(Player winner) {
        // only show toast and vibration once per game
        if ( bNonIntrusiveGameEndingPerformed == true ) { return false; }

        bNonIntrusiveGameEndingPerformed = true;

        int iResId = R.string.game_ended;
        if ( matchModel.isPossibleMatchVictoryFor() != null ) {
            iResId = R.string.match_ended;
        }
        iBoard.showToast(iResId);
        return true;
    }
*/

    private void confirmPostMatchResult() {
        if ( PreferenceValues.autoSuggestToPostResult(ScoreBoard.this) == false ) {
            return;
        }
        if ( StringUtil.isEmpty(PreferenceValues.getPostResultToURL(this)) ) {
            return;
        }

        if ( PreferenceValues.useShareFeature(this).equals(Feature.Automatic) ) {
            ShareMatchPrefs prefs = PreferenceValues.getShareAction(this);
            if ( prefs.equals(ShareMatchPrefs.PostResult) ) {
                // match will be automatically posted, do not show the 'suggest' dialog
                return;
            }
        }
        int iShowPostToSiteDialogCnt = PreferenceValues.getRunCount(this, PreferenceKeys.autoSuggestToPostResult);
        boolean bShowWithNoMoreCheckBox = PreferenceValues.autoSuggestToPostResult(this) && (iShowPostToSiteDialogCnt < 5);

        PostMatchResult postMatchResult = new PostMatchResult(this, matchModel, this);
        postMatchResult.init(bShowWithNoMoreCheckBox);
        addToDialogStack(postMatchResult);
    }

    public void showOnlineSheetAvailableChoice(String sURL) {
        OnlineSheetAvailableChoice sheetAvailableChoice = new OnlineSheetAvailableChoice(this, matchModel, this);
        sheetAvailableChoice.init(sURL);
        addToDialogStack(sheetAvailableChoice);
    }
    /** Delete 'Based-On-Preference' */
    private void deleteFromMyList_BOP() {
        if ( MatchTabbed.SelectTab.Mine.equals(MatchTabbed.getDefaultTab() ) == false ) {
            return;
        }
        Feature removeMatchFromMyList = PreferenceValues.removeMatchFromMyListWhenSelected(ScoreBoard.this);
        switch (removeMatchFromMyList) {
            case DoNotUse:
                return;
            case Suggest:
                DeleteFromMyList deleteFromMyList = new DeleteFromMyList(this, matchModel, this);
                addToDialogStack(deleteFromMyList);
                return;
            case Automatic:
                DeleteFromMyList.deleteMatchFromMyList(this, matchModel);
                return;
        }
    }

    // made static in preparation to be able to have feedback about the timer status child activity like MatchHistory
    public static Timer timer = null;

    private DialogTimerView dialogTimerView;
    public DialogTimerView getDialogTimerView() {
        if ( dialogTimerView == null ) {
            dialogTimerView = new DialogTimerView(this);
        }
        return dialogTimerView;
    }

    // ------------------------------------------------------
    // promo modus
    // ------------------------------------------------------
    private void initPreferencesForPromoDemoThread() {
        PreferenceValues.setNumber   (PreferenceKeys.numberOfGamesToWinMatch, this, 3);
        PreferenceValues.setNumber   (PreferenceKeys.numberOfPointsToWinGame, this, 11);
        PreferenceValues.setEnum     (PreferenceKeys.tieBreakFormat, this, TieBreakFormat.TwoClearPoints);
        PreferenceValues.setEnum     (PreferenceKeys.recordRallyEndStatsAfterEachScore, this, Feature.DoNotUse);
        PreferenceValues.setEnum     (PreferenceKeys.useTimersFeature, this, Feature.Suggest);
        PreferenceValues.setEnum     (PreferenceKeys.useOfficialAnnouncementsFeature, this, Feature.Suggest);
        PreferenceValues.setEnum     (PreferenceKeys.endGameSuggestion, this, Feature.Suggest);
        PreferenceValues.setBoolean  (PreferenceKeys.useHandInHandOutScoring, this, false);
      //PreferenceValues.setBoolean  (PreferenceKeys.showScoringHistoryInMainScreen, this, true);
        PreferenceValues.setStringSet(PreferenceKeys.showScoringHistoryInMainScreenOn, ShowOnScreen.OnDevice, this);
        PreferenceValues.setStringSet(PreferenceKeys.showMatchDurationChronoOn       , ShowOnScreen.OnChromeCast, this);
        PreferenceValues.setStringSet(PreferenceKeys.showLastGameDurationChronoOn    , ShowOnScreen.OnChromeCast, this);

        PreferenceValues.setBoolean  (PreferenceKeys.autoSuggestToPostResult, this, false);
        PreferenceValues.setBoolean  (PreferenceKeys.showTips, this, false);
        if ( PreferenceValues.showFullScreen(this) == false || PreferenceValues.getOrientationPreference(this).contains(OrientationPreference.Landscape) == false ) {
            PreferenceValues.setBoolean(PreferenceKeys.showFullScreen, this, true);
            PreferenceValues.setString(PreferenceKeys.OrientationPreference, this, OrientationPreference.Landscape.toString());
            //System.exit(0);
        }
        PreferenceValues.setEnum(PreferenceKeys.MatchTabbed_defaultTab, this, MatchTabbed.SelectTab.Manual);

        PreferenceValues.addOrReplaceNewFeedURL(this, "Invalid", "http://invalid.url.for.matches", "http://invalid.url.for.players", "http://invalid.url.for.posting.results", false, false);
    }

    // ------------------------------------------------------
    // demo modus
    // - TODO: no rotate during demo modus? or ensure new activity is passed on to demo thread
    // ------------------------------------------------------
    public enum Mode {
        Normal,
        ScreenShots,
        Debug,
        FullAutomatedDemo,
        GuidedDemo,
        Promo,
    }
    private Mode toggleDemoMode(DemoThread.DemoMessage demoMessage) {
        m_mode = Mode.values()[(m_mode.ordinal()+1)%Mode.values().length];

        setModus(demoMessage, m_mode);
        return m_mode;
    }

    public void setModus(DemoThread.DemoMessage demoMessage, Mode mode) {
        this.m_mode = mode;

        stopDemoMode();
        stopPromoMode();
        switch (mode) {
            case Normal:
                Timer.iSpeedUpFactor = 1;
                stopDemoMode();
                stopPromoMode();
                break;
            case ScreenShots:
                //PreferenceValues.setOverwrite(PreferenceKeys.showChoosenDecisionDuration_Conduct, 17); // make it show a little longer for taking screenshots
                //PreferenceValues.setOverwrite(PreferenceKeys.showChoosenDecisionDuration_Appeal , 17); // make it show a little longer for taking screenshots
                createPreviousMatchesFromRaw();
                break;
            case Promo:
                startPromoMode();
                break;
            case GuidedDemo:
                startDemoMode(demoMessage, mode);
                break;
            case FullAutomatedDemo:
                startDemoMode(demoMessage, mode);
                break;

        }
        if ( mode != Mode.Normal ){
            List<String> clubs = Arrays.asList("University of Notthingham (UoN)", "University Of Southampton [UoSH]", "Squash 22", "Double Squash Skills - DBY");
            PreferenceValues.setOverwrite(PreferenceKeys.clubListLastA, clubs.get(0));
            PreferenceValues.setOverwrite(PreferenceKeys.clubListLastX, clubs.get(1));
            PreferenceValues.addStringsToList(this, PreferenceKeys.clubList, clubs);
        }
        Toast.makeText(ScoreBoard.this, String.format("Your in %s mode now", m_mode), Toast.LENGTH_LONG).show();
    }

    public static DemoThread demoThread = null;
    private void startDemoMode(DemoThread.DemoMessage demoMessage, Mode mode) {
        // start demo in separate thread
        if ( demoThread != null ) { return; }
        if ( mode.equals(Mode.GuidedDemo)) {
            if ( demoMessage == null ) {
                DemoStartStep startStep = new DemoStartStep(this, matchModel, this);
                startStep.show();
                return;
            }
            DemoSupportingThread.startAt = demoMessage;
            demoThread = new DemoSupportingThread(this, matchModel);
        } else {
            demoThread = new FullDemoThread(this, matchModel);
            Timer.iSpeedUpFactor = 5;
        }
        demoThread.start();
        createPreviousMatchesFromRaw();

        initPreferencesForPromoDemoThread();
    }

    private void stopDemoMode() {
        if (demoThread != null) {
            //demoThread.interrupt();
            demoThread.stopLoop();
            demoThread.interrupt();
            demoThread = null;
            Toast.makeText(this, "Demo thread stopped", Toast.LENGTH_SHORT).show();
        }
    }

    private void createPreviousMatchesFromRaw() {
        // read matches from raw resources and store them in history
        Map<Integer, SportType> lResources = new HashMap<>();
        addDemoMatchesForSport(lResources, Brand.getSport());
        if ( PreferenceValues.isUnbrandedExecutable(this) ) {
            // if we are running as unbranded but one ore more other brand values are uncommented
            if ( PreferenceValues.isBrandTesting(this) ) {
                for(Brand brand: Brand.values() ) {
                    addDemoMatchesForSport(lResources, brand.getSportType());
                }
            }
        }
        try {
            for(int iResInt: lResources.keySet()) {
                SportType sportType = lResources.get(iResInt);
                String sJsonDemo = ContentUtil.readRaw(this, iResInt);
                storeAsPrevious(this, sJsonDemo, ModelFactory.getModel(sportType), true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addDemoMatchesForSport(Map<Integer, SportType> lResources, SportType sport) {
        switch (sport) {
            case Squash:
                lResources.put(R.raw.demomatch1, sport);
                lResources.put(R.raw.demomatch2, sport);
                lResources.put(R.raw.demomatch3, sport);
                break;
            case Racketlon:
                lResources.put(R.raw.demomatch1_racketlon, sport);
                break;
            case Tabletennis:
                lResources.put(R.raw.demomatch1_tabletennis, sport);
                break;
        }
    }

    public static boolean isInDemoMode() {
        return demoThread != null;
    }

    public static boolean isInPromoMode() {
        return promoThread != null;
    }
    public static boolean isInSpecialMode() {
        return ScoreBoard.m_mode.equals(ScoreBoard.Mode.Normal) == false;
    }

    private static PromoThread promoThread = null;
    private void startPromoMode() {
        // start Promo in separate thread
        if ( promoThread != null ) { return; }
        promoThread = new PromoThread(this, matchModel);
        promoThread.start();

        Toast.makeText(this, "Promo thread started", Toast.LENGTH_SHORT).show();
        Timer.iSpeedUpFactor = 15;

        createPreviousMatchesFromRaw();

        matchModel.setPlayerName(Player.A, "Player A");
        matchModel.setPlayerName(Player.B, "Player B");
        restartScore();
        initPreferencesForPromoDemoThread();
    }

    private void stopPromoMode() {
        if ( promoThread != null) {
            //demoThread.interrupt();
            promoThread.stopLoop();
            promoThread.interrupt();
            promoThread = null;
            Toast.makeText(this, "Promo thread stopped", Toast.LENGTH_SHORT).show();
            Timer.iSpeedUpFactor = 1;
        }
    }

    public static void updateDemoThread(Activity activity) {
        if ( isInDemoMode() ) {
            demoThread.setActivity(activity);
            demoThread.setModel   (matchModel);
        }
        if ( isInPromoMode() ) {
            promoThread.setActivity(activity);
            promoThread.setModel   (matchModel);
            if ( m_bShareStarted_DemoThread ) {
                m_bShareStarted_DemoThread = false;
                // auto start facebook app
                PackageManager pm = activity.getPackageManager();
                Intent fbIntent = pm.getLaunchIntentForPackage("com.facebook.katana");
                activity.startActivity(fbIntent);
            }
        }
    }
    public static void updateDemoThread(Menu menu) {
        if ( isInDemoMode() ) {
            demoThread.setMenu(menu);
        }
    }
    private static Mode m_mode = Mode.Normal;

    // ------------------------------------------------------
    // workaround to keep timer or baseDialog alive when orientation changes
    // ------------------------------------------------------

    /** invoked e.g. when orientation switches and when child activity is launched */
    @Override protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        dialogManager.saveInstanceState(outState);

        ViewUtil.SIS_Type trigger = ViewUtil.getStoreInstanceStateTrigger(outState);
        if ( trigger.equals(ViewUtil.SIS_Type.ScreenRotate) ) {
            Timer.removeTimerView(iBoard.isPresentation(), iBoard);
        }
        Timer.removeTimerView(false, dialogTimerView);
        dialogTimerView = null;

        if ( (scSequence != null) && trigger.equals(ViewUtil.SIS_Type.ChildActivityLaunch) == false ) {
            scSequence.setActivity(null); // prevent memory leak
        }
        cleanUpForOrientationInDegreesChanges();
    }

    /** invoked e.g. when orientation switches */
    @Override protected void onRestoreInstanceState(Bundle savedInstanceState) {
        try {
            super.onRestoreInstanceState(savedInstanceState); // on rotate: handleRelaunchActivity
        } catch (IllegalStateException e) {
            // seen Caused by: java.lang.IllegalStateException: Bad magic number for Bundle: 0x3
            // on Nexus 5 hammerhead
            e.printStackTrace();
        }

        if ( savedInstanceState == null ) { return; }

        dialogManager.restoreInstanceState(savedInstanceState, this, matchModel, this);
        updateDemoThread(this);
        //initModelListeners(); // 20141201: removed here because onResume is always invoked
    }

    // ----------------------------------------------------
    // --------------------- NDEF/NFC/AndroidBeam ---------
    // ----------------------------------------------------

    private PendingIntent  pendingIntent      = null;
    private IntentFilter[] intentFiltersArray = null;
    private String[][]     techListsArray     = null;
    private static final boolean B_ONE_INSTANCE_FROM_NFC = true; // foreground dispatch http://stackoverflow.com/questions/19450661/nfc-detection-either-start-activity-or-display-dialog

    private NfcAdapter     mNfcAdapter        = null;
    private void registerNfc() {
        if ( getPackageManager().hasSystemFeature(PackageManager.FEATURE_NFC) == false ) {
            return;
        }
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if ( mNfcAdapter == null ) {
            //Toast.makeText(this, "NFC is not available", Toast.LENGTH_LONG).show();
            return;
        }
        // Register callback
        mNfcAdapter.setNdefPushMessageCallback(this, this);

        // to be able to intercept message send via NFC on an already running app
        if ( B_ONE_INSTANCE_FROM_NFC ) {
            pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
            IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
            try {
                ndef.addDataType("application/json");
            } catch (IntentFilter.MalformedMimeTypeException e) {
                throw new RuntimeException("fail", e);
            }
            intentFiltersArray = new IntentFilter[]{ndef,};
            techListsArray     = new String[][]{new String[]{NfcF.class.getName()}};
        }
    }

    @Override public NdefMessage createNdefMessage(NfcEvent nfcEvent) {
        String packageName    = this.getPackageName();
        String text           = matchModel.toJsonString(this);
        NdefRecord mimeRecord = createMimeRecord("application/" + "json" /*+ packageName*/, text.getBytes()); // matching mimetype should exist in AndroidManifest.xml in the following form
/*
        <intent-filter>
            <action android:name="android.nfc.action.NDEF_DISCOVERED"/>
            <category android:name="android.intent.category.DEFAULT"/>
            <data android:mimeType="application/json"/>
        </intent-filter>
*/
        NdefRecord applicationRecord = NdefRecord.createApplicationRecord(packageName);
        NdefMessage msg = new NdefMessage(new NdefRecord[]{mimeRecord
                /**
                 * Next parameter is the Android Application Record (AAR).
                 * When a device receives a push with an AAR in it, the application specified in the AAR is guaranteed to run.
                 * The AAR overrides the tag dispatch system.
                 * We add it to guarantee that our ScoreBoard starts when receiving a beamed message (or download of the app from google-play is started if app is not yet installed).
                 */
                , applicationRecord
        });
        return msg;
    }

    /** Creates a custom MIME type encapsulated in an NDEF record */
    public static NdefRecord createMimeRecord(String mimeType, byte[] payload) {
        byte[] mimeBytes = mimeType.getBytes(/*Charset.forName("US-ASCII")*/);
        NdefRecord mimeRecord = new NdefRecord(NdefRecord.TNF_MIME_MEDIA, mimeBytes, new byte[0], payload);
        return mimeRecord;
    }

    @Override public void onNewIntent(Intent intent) {
        // onResume gets called after this to handle the intent
        Log.w(TAG, "onNewIntent.. intent = " + intent);
        setIntent(intent);
        onNFCNewIntent(intent);
        onURLNewIntent(intent);
    }

    private static final int MESSAGE_SENT = 1;

    @Override public void onNdefPushComplete(NfcEvent nfcEvent) {
        // A handler is needed to send messages to the activity when this callback occurs, because it happens from a binder thread
        if ( mHandler == null ) {
            mHandler = new Handler() {
                @Override public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case MESSAGE_SENT:
                            Toast.makeText(getApplicationContext(), R.string.nfc_match_send, Toast.LENGTH_LONG).show();
                            break;
                    }
                }
            };
        }
        mHandler.obtainMessage(MESSAGE_SENT).sendToTarget();
    }
    /** This handler receives a message from onNdefPushComplete */
    private static Handler mHandler = null;

    private static boolean m_bURLReceived = false;
    private void onResumeURL() {
        if ( m_bURLReceived ) { return; }

        Intent intent= getIntent();
        if ( intent == null) { return; }
        String action = intent.getAction();
        //Log.d(TAG, "Resume with action " + action);

        if ( Intent.ACTION_VIEW.equals( action ) ) {
            Uri url = intent.getData();
            if ( url != null ) {
                // app was started by clicking an url
                final String sURL = url.toString();
                final String isShowMatchPageRegExp = "(show([Mm]atch)?\\.php|/20[0-9]{6})";
                boolean bUploadedZipfileURL = sURL.matches(".*/uploads/.+\\.zip[^\\w]*$");
                boolean bSharedmatchURL = sURL.matches(".*" + isShowMatchPageRegExp + ".+");

                String sAdditionalInfo = bUploadedZipfileURL ? "Zip" : (bSharedmatchURL ? "Shared match" : "feed url");
                final String sMsg = getString(R.string.loading_of_x, sURL, sAdditionalInfo);
                Toast.makeText(this, sMsg, Toast.LENGTH_LONG).show();
                Log.i(TAG, sMsg);

                if ( bUploadedZipfileURL ) {
                    // backup of matches zip url
                    PreferenceValues.Permission hasPermission = PreferenceValues.doesUserHavePermissionToWriteExternalStorage(this, true);
                    if ( hasPermission == PreferenceValues.Permission.Granted ) {
                        handleMenuItem(R.id.sb_download_zip, sURL, ZipType.SquoreUpload);
                    }
                    //downloadMatchesInZip(sURL, ZipType.SquoreUpload);
                } else if ( bSharedmatchURL ) {
                    String sGetMatchUrl = sURL.replaceAll(".*[=/](20)", Brand.getBaseURL() + "/getmatch.php?name=20");
                    Log.i(TAG, String.format("Get match URL : %s", sGetMatchUrl));
                    URLFeedTask fetchMatch = new URLFeedTask(this, sGetMatchUrl);
                    fetchMatch.setCacheFileToOld(true);
                    fetchMatch.setContentReceiver(new OnResumeWithUrlContentReceiver(sGetMatchUrl));
                    fetchMatch.execute();
                } else {
                    // assume it is a feed url like
                    String sFeedURL = sURL.replaceAll("(?:https?)://(?:www\\.|esf\\.|vsf\\.|fir\\.|rfa\\.|squashcanada\\.|wsf\\.|squashse\\.)?(?:toernooi|competitions|tournamentsoftware|europeansquash|squash)(?:\\.\\w{2,3}).*id=([A-Za-z0-9-]+).*?$"
                                                     , Brand.getBaseURL() + "/tournamentsoftware/$1");
                    Log.i(TAG, String.format("Feed URL : %s", sFeedURL));
                    PreferenceValues.addOrReplaceNewFeedURL(this, "tournamentsoftware url", sFeedURL + "/matches"
                                                                                          , sFeedURL + "/players", null, true, true);
                    handleMenuItem(R.id.sb_select_feed_match);
                }
            }
        }
        m_bURLReceived = true;
    }
    private class OnResumeWithUrlContentReceiver implements ContentReceiver {
        private String sURL = null;
        private OnResumeWithUrlContentReceiver(String sUrl) {
            this.sURL = sUrl;
        }
        @Override public void receive(String sContent, FetchResult fetchResult, long lCacheAge, String sPreviousContent) {
            ScoreBoard context = ScoreBoard.this;
            try {
                if (fetchResult.equals(FetchResult.OK)) {
                    Model mIn = Brand.getModel();
                    if ( mIn.fromJsonString(sContent) != null ) {
                        int iContinueMatchCaption  = mIn.matchHasEnded() ? 0 : R.string.cmd_open_match_and_continue;
                        MatchReceivedUtil mr = new MatchReceivedUtil(context, mIn);
                        String sMsg = getString( R.string.s1_match_s2_retrieved_from_the_web
                                               , getString(mIn.matchHasEnded() ? R.string.Completed : R.string.Uncompleted)
                                               , mIn.getName(Player.A) + " - " + mIn.getName(Player.B)
                                               );
                        mr.init(sMsg + "\n" + getString(R.string.what_do_you_want_to_do_with_it)
                               , R.string.cmd_store_for_later_usage  , MatchAction.SaveToStoredMatches
                               , R.string.cmd_show_detail            , MatchAction.ShowDetails
                               , iContinueMatchCaption               , MatchAction.ContinueInScoreBoard
                        );
                    }
                } else {
                    dialogWithOkOnly(context, "something went wrong with fetching " + sURL + "\n" + fetchResult);
                }
            } catch (Exception e) {
                e.printStackTrace();
                dialogWithOkOnly(context, "something went wrong with loaded content " + StringUtil.size(sContent));
            }
        }
    }

    private void onNFCNewIntent(Intent intent) {
        m_bNFCReceived = false;
        Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
    }
    private void onURLNewIntent(Intent intent) {
        m_bURLReceived = false;
    }

    private void onNFCPause() {
        if ( B_ONE_INSTANCE_FROM_NFC && (mNfcAdapter != null) ) {
            try {
                mNfcAdapter.disableForegroundDispatch(this); // added try-catch because of crash reported in PlayStore (Android 8 on 20180804)
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }
    }

    private static boolean m_bNFCReceived = false;
    private void onResumeNFC() {
        if ( B_ONE_INSTANCE_FROM_NFC && (mNfcAdapter != null) ) {
            mNfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, techListsArray);
        }

        if ( m_bNFCReceived ) { return; }

        // Check to see that the Activity started due to an Android Beam
        Intent intent= getIntent();
        if ( intent == null) { return; }
        String action = intent.getAction();
        //Log.d(TAG, "Resume with action " + action);
        if ( NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action) ) {
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if ( rawMsgs == null ) { return; }
            NdefMessage msg = (NdefMessage) rawMsgs[0];
            // record 0 contains the MIME type, record 1 is the AAR, if present
            NdefRecord[] records = msg.getRecords();
            NdefRecord recMimeType = records[0];
            byte[] baMessage = recMimeType.getPayload();
            String sMessage = new String(baMessage);
            if ( sMessage.trim().startsWith("{") ) {
                intent.setAction(Intent.ACTION_MAIN); // attempt to ensure that the beam data is not re-parsed over and over again (TODO: works in API 19, not in 17)
                m_bNFCReceived = true;

                // TODO: use (or totally abandon) PreferenceValues.saveMatchesForLaterUsage(this)
                try {
                    Model mIn = Brand.getModel();
                    if ( mIn.fromJsonString(sMessage) != null ) {
                        int iContinueMatchCaption  = mIn.matchHasEnded() ? 0 : R.string.cmd_open_match_and_continue;
                        MatchReceivedUtil mr = new MatchReceivedUtil(this, mIn);
                        String sMsg = getString( R.string.s1_match_s2_received_via_nfc
                                , getString(mIn.matchHasEnded() ? R.string.Completed : R.string.Uncompleted)
                                , mIn.getName(Player.A) + " - " + mIn.getName(Player.B)
                        );
                        mr.init(sMsg + "\n" + getString(R.string.what_do_you_want_to_do_with_it)
                                , R.string.cmd_store_for_later_usage  , MatchAction.SaveToStoredMatches
                                , R.string.cmd_show_detail            , MatchAction.ShowDetails
                                , iContinueMatchCaption               , MatchAction.ContinueInScoreBoard
                        );
                    } else {
                        JSONObject joSetting = new JSONObject(sMessage);
                        // TODO: handle settings being transferred
/*
                        Log.w(TAG, joSetting.toString(4));
                        if ( joSetting.has(PreferenceKeys.feedPostUrls.toString())) {

                        }
                        if ( joSetting.has(PreferenceKeys.matchList.toString())) {
                            String sMatchList = joSetting.getString(PreferenceKeys.matchList.toString());
                            PreferenceValues.prependMatchesToList(this, Arrays.asList(StringUtil.singleCharacterSplit(sMatchList)));
                        }
*/
                        for ( PreferenceKeys key: PreferenceKeys.values() ) {
                            if ( joSetting.has(key.toString())) {
                                String value = joSetting.getString(key.toString());
                                if ( StringUtil.isNotEmpty(value) ) {
                                    PreferenceValues.setOverwrite(key, value);
                                }
                            }
                        }
                        PreferenceValues.interpretOverwrites(this);
                    }
                } catch (Exception e) {
                    String text = getString(R.string.nfc_something_went_wrong, e.getMessage());
                    Toast.makeText(this, text, Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            } else {
                try {
                    Toast.makeText(this, sMessage, Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //---------------------------------------------------------
    // Progress message
    //---------------------------------------------------------
    private ProgressDialog progressDialog = null;
    private void showProgress(int iResId) {
        if ( progressDialog == null ) {
            progressDialog = new ProgressDialog(this);
        }
        progressDialog.setIndeterminate(true);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        if ( iResId != 0 ) {
            progressDialog.setMessage(getString(iResId));
        }
        progressDialog.show();
    }
    public void hideProgress() {
        if ( progressDialog != null ) {
            try {
                progressDialog.cancel();
                progressDialog.dismiss();
            } catch (Exception e) {
                // error might occur if screen was rotated
                e.printStackTrace();
            }
        }
    }

    // ------------------------------------------------------------------------
	// RateMeMaybe
	// ------------------------------------------------------------------------
    private RateMeMaybe rmm = null;
    //private static boolean rateMeMaybeInvoked = false;
    private void rateMeMaybe_init() {
        //if ( rateMeMaybeInvoked ) { return; }
        //rateMeMaybeInvoked = true;

        boolean bUseRateMeMaybe   = true;
        boolean bShowAtEndOfMatch = true;
        rmm = new RateMeMaybe(this, R.string.rmm_title, R.string.rmm_message, R.string.rmm_later, R.string.rmm_never);
        if ( RateMeMaybe.class.getSimpleName().equalsIgnoreCase(matchModel.getName(Player.A)) ) {
            if ( matchModel.getName(Player.B).equalsIgnoreCase("reset")) {
                rmm.resetData(this);
            }
            rmm.setPromptMinimums(2, 0, 2, 0);
        } else {
            rmm.setPromptMinimums(20, 15, 20, 20);
        }
        if ( bUseRateMeMaybe ) {
            bShowAtEndOfMatch = rmm.increaseCountersAndCheckIfShowingIsApplicable();
        }
        if ( bShowAtEndOfMatch ) {
            Log.d(TAG, "Showing 'rate me' at end of match");
        } else {
            //Log.d(TAG, "NOT Showing 'rate me'");
            rmm = null;
        }
    }

    // ----------------------------------------------------
    // --------------------- download a zip with matches --
    // ----------------------------------------------------
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ( DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action) == false ) {
                Log.w(TAG, "Download not completed?");
                return;
            }

            long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(enqueue);
            Cursor c = dm.query(query);
            if ( c.moveToFirst() == false ) {
                Log.w(TAG, "No downloads found?");
                return;
            }

            int columnIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
            if ( DownloadManager.STATUS_SUCCESSFUL != c.getInt(columnIndex) ) {
                Log.w(TAG, "Last download found was unsuccessful");
                return;
            }

            String localFilePath = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME)); // deprecated in anroid 7
            List<String> lMessages = new ArrayList<String>();
            lMessages.add("Download complete: " + localFilePath + " (download id:" + downloadId + ")");

            File f = new File(localFilePath);

            // clean previous file before copying over it
            File dir = PreferenceValues.targetDirForImportExport(context, false);
            final File fMyFile = new File(dir, String.format("%s.downloaded.zip", ztDownLoadShortname));
            if ( fMyFile.exists() ) {
                if ( fMyFile.length() == f.length() && (ztDownLoadShortname == ZipType.SquoreAll) ) {
                    lMessages.add(String.format("File %s has same size (%s) as previous download for %s", fMyFile.getAbsolutePath(), f.length(), ztDownLoadShortname));
                }
                fMyFile.delete();
            }

            String sError = null;
            if ( Build.VERSION.SDK_INT > Build.VERSION_CODES.M ) {
                String uriLocal      = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)); // not the original but uri on device
              //lMessages.add("local uri:" + uriLocal);
                ContentResolver cs   = getContentResolver();
                Uri             uri  = Uri.parse(uriLocal);
                try {
                    ParcelFileDescriptor fileDescriptor = cs.openFileDescriptor(uri, "r");
                    FileInputStream stream = new ParcelFileDescriptor.AutoCloseInputStream(fileDescriptor);
                    sError = FileUtil.copyFile(stream, fMyFile);
                } catch (Exception e) {
                    e.printStackTrace();
                    sError = e.getMessage();
                }
            } else {
                Log.i(TAG, "Handling " + localFilePath);

                sError = FileUtil.copyFile(f, fMyFile);
            }

            if ( StringUtil.isEmpty(sError) ) {
                handleMenuItem(R.id.cmd_import_matches);
            } else {
                String sMsg = String.format("Copy of file to import/export location %s failed (%s)", dir.getAbsolutePath(), sError);
                lMessages.add(sMsg);
                Log.w(TAG, sMsg);
                dialogWithOkOnly(context, ztDownLoadShortname.toString(), ListUtil.join(lMessages, "\n\n"), true);
            }

            if ( ztDownLoadShortname == ZipType.SquoreAll ) {
                // http://boxen.double-yellow.be/make.json.zip.php
                downloadMatchesInZip("http://boxen.double-yellow.be/changes/json.zip", ZipType.DyBoxen);
            }
        }
    };
    private enum ZipType {
        SquoreUpload,
        SquoreAll,
        DyBoxen,
    }
    private long            enqueue             = 0L;
    private ZipType         ztDownLoadShortname = null;
    private DownloadManager dm                  = null;
    private void downloadMatchesInZip(String sURL, ZipType ztDownLoadShortname) {
        if ( dm == null ) {
            dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);

            registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
            iMenuToRepeatOnPermissionGranted = R.id.sb_download_zip;
            oaMenuCtxForRepeat = new Object[] { sURL, ztDownLoadShortname };
        }
        this.ztDownLoadShortname = ztDownLoadShortname;
        Uri uri = Uri.parse(sURL);
        DownloadManager.Request request = new DownloadManager.Request(uri);
        enqueue = dm.enqueue(request);
    }

    // ----------------------------------------------------
    // --------------------- debug mode -------------------
    // ----------------------------------------------------
    private void dbgmsg(String s, int iResId, int iResId2) {
        if ( m_mode.equals(Mode.Normal) ) { return; }

        String sMsg = s + " on " + (iResId<9999?iResId:getResourceEntryName(iResId));
        if ( iResId2 !=0 ) {
            sMsg += " and "+ (iResId2<9999?iResId2:getResourceEntryName(iResId2));
        }
        if ( m_mode.equals(Mode.Debug) ) {
            Toast.makeText(ScoreBoard.this, sMsg, Toast.LENGTH_SHORT).show();
        } else {
            Log.d(TAG, sMsg);
        }
    }

    // ----------------------------------------------------
    // --------------------- casting ----------------------
    // ----------------------------------------------------
    private com.doubleyellow.scoreboard.cast.CastHelper castHelper = new com.doubleyellow.scoreboard.cast.CastHelper();
    private void initCasting(Activity context) {
        castHelper.initCasting(context);
    }
    private void initCastMenu(Menu menu) {
        castHelper.initCastMenu(menu);
    }
    private void setModelForCast(Model matchModel) {
        castHelper.setModelForCast(matchModel);
    }
    private TimerView getCastTimerView() {
        return castHelper.getTimerView();
    }
    private void castColors(Map<ColorPrefs.ColorTarget, Integer> mColors) {
        castHelper.castColors(mColors);
    }
    public void castDurationChronos() {
        castHelper.castDurationChronos();
    }
    private void startCast() {
        castHelper.startCast();
    }
    private void stopCast() {
        castHelper.stopCast();
    }
}
