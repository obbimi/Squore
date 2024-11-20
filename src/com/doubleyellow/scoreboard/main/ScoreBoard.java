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
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.*;
import android.preference.PreferenceManager;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.splashscreen.*;
import androidx.core.view.InputDeviceCompat;
import androidx.core.view.MotionEventCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.util.Base64;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.*;
import android.widget.*;

import com.anjlab.android.iab.v3.PurchaseInfo;
import com.anjlab.android.iab.v3.BillingProcessor;

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
import com.doubleyellow.scoreboard.bluetooth_le.selectdevice.VerifyConnectedDevices;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.activity.*;
import com.doubleyellow.scoreboard.archive.ArchiveTabbed;
import com.doubleyellow.scoreboard.archive.PreviousMatchSelector;
import com.doubleyellow.scoreboard.bluetooth.BTMethods;
import com.doubleyellow.scoreboard.bluetooth.BTRole;
import com.doubleyellow.scoreboard.bluetooth.BTState;
import com.doubleyellow.scoreboard.bluetooth.BluetoothControlService;
import com.doubleyellow.scoreboard.bluetooth.BluetoothHandler;
import com.doubleyellow.scoreboard.bluetooth.MessageSource;
import com.doubleyellow.scoreboard.bluetooth.SelectDeviceDialog;
import com.doubleyellow.scoreboard.bluetooth_le.BLEDeviceButton;
import com.doubleyellow.scoreboard.bluetooth_le.BLEHandler;
import com.doubleyellow.scoreboard.bluetooth_le.BLEReceiverManager;
import com.doubleyellow.scoreboard.bluetooth_le.BLEUtil;
import com.doubleyellow.scoreboard.bluetooth_le.selectdevice.BLEActivity;
import com.doubleyellow.scoreboard.cast.FullScreenTimer;
import com.doubleyellow.scoreboard.cast.EndOfGameView;
import com.doubleyellow.scoreboard.cast.framework.CastHelper;
import com.doubleyellow.scoreboard.demo.*;
import com.doubleyellow.demo.*;
import com.doubleyellow.scoreboard.dialog.Handicap;
import com.doubleyellow.scoreboard.dialog.announcement.EndGameAnnouncement;
import com.doubleyellow.scoreboard.dialog.announcement.EndMatchAnnouncement;
import com.doubleyellow.scoreboard.dialog.announcement.StartGameAnnouncement;
import com.doubleyellow.scoreboard.feed.Authentication;
import com.doubleyellow.scoreboard.feed.Preloader;
import com.doubleyellow.scoreboard.firebase.PusherHandler;
import com.doubleyellow.scoreboard.firebase.PusherMessagingService;
import com.doubleyellow.scoreboard.model.*;
import com.doubleyellow.scoreboard.model.Util;
import com.doubleyellow.scoreboard.share.MatchModelPoster;
import com.doubleyellow.scoreboard.share.ResultPoster;
import com.doubleyellow.scoreboard.share.ResultSender;
import com.doubleyellow.scoreboard.share.ShareHelper;
import com.doubleyellow.scoreboard.speech.Speak;
import com.doubleyellow.scoreboard.timer.*;
import com.doubleyellow.scoreboard.timer.Timer;
import com.doubleyellow.scoreboard.history.MatchHistory;
import com.doubleyellow.scoreboard.match.*;
import com.doubleyellow.scoreboard.vico.IBoard;
import com.doubleyellow.scoreboard.vico.FocusEffect;
import com.doubleyellow.scoreboard.view.GameHistoryView;
import com.doubleyellow.scoreboard.view.PlayersButton;
import com.doubleyellow.scoreboard.view.ServeButton;
import com.doubleyellow.android.handler.OnBackPressExitHandler;
import com.doubleyellow.scoreboard.dialog.*;
import com.doubleyellow.scoreboard.prefs.*;
import com.doubleyellow.scoreboard.wear.WearRole;
import com.doubleyellow.scoreboard.wear.WearableHelper;
import com.doubleyellow.util.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.doubleyellow.android.showcase.ShowcaseView;
import com.doubleyellow.android.showcase.ShowcaseConfig;
import com.doubleyellow.android.showcase.ShowcaseSequence;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/* ChromeCast */
//import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.ActionBar;

// Wearable
import androidx.wear.input.WearableButtons; // requires api >= 25

/**
 * The main Activity of the scoreboard app.
 */
public class ScoreBoard extends XActivity implements /*NfcAdapter.CreateNdefMessageCallback, NfcAdapter.OnNdefPushCompleteCallback,*/ MenuHandler, DrawTouch
{
    private static final String TAG = "SB." + ScoreBoard.class.getSimpleName();

    private  static         Model                matchModel ;
    public static Model getMatchModel() {
        if ( matchModel == null ) {
            Log.w(TAG, "matchModel is null");
        }
        return matchModel;
    }
    public static void setMatchModel(Model m) {
        if ( (m == null) && (matchModel != null) ) {
            Log.w(TAG, "setting matchModel to null");
        }
        matchModel = m;
    }

    //-------------------------------------------------------------------------
    // Permission granting callback
    //-------------------------------------------------------------------------
    private int      iMenuToRepeatOnPermissionGranted = 0;
    private Object[] oaMenuCtxForRepeat               = null;
    @Override public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PreferenceKeys key = null;
        try {
            key = PreferenceKeys.values()[requestCode];
        } catch (Exception e) {
            e.printStackTrace();
        }

        if ( key != null ) {
            // If request is cancelled, the result arrays are empty.
            if ( (grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED) ) {
                // permission was granted, yay! Do the task you need to do.
                Log.d(TAG, String.format("Permission granted: %s (%s)", key, grantResults[0]));
                if ( false && (iMenuToRepeatOnPermissionGranted != 0) ) {
                    handleMenuItem(iMenuToRepeatOnPermissionGranted, oaMenuCtxForRepeat);
                    iMenuToRepeatOnPermissionGranted = 0;
                    oaMenuCtxForRepeat               = null;
                }

                // for testing purposes, copy a zip file with around 30 matches to the location where backups of matches are stored
                if ( key.equals(PreferenceKeys.targetDirForImportExport) && Brand.isSquash() ) {
                    File file = PreferenceValues.targetDirForImportExport(this, true);
                    try {
                        InputStream inputStream = this.getResources().openRawResource(R.raw.squore_iddo); // TODO: ensure compile does not fail if resource does not exist
                        FileOutputStream fo = new FileOutputStream(new File(file, "Squore.stored.matches.test.zip"));
                        byte[] baRead = new byte[1024];
                        while(inputStream.read(baRead)>0) {
                            fo.write(baRead);
                        }
                        fo.close();
                        inputStream.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
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
                    handleMenuItem(R.id.pl_change_score, player);
                    return true;
                case R.id.txt_player1:
                case R.id.txt_player2:
                    if ( Brand.isSquash() ) {
                        // TODO: for doubles, switch in/out in such a way that touched player becomes the server
                        Log.d(TAG, String.format("Two finger click on player %s", player));
                        if ( matchModel.isDoubles() ) {
                            handleMenuItem(R.id.pl_show_conduct, player);
                        } else {
                            showBrokenEquipment(player);
                        }
                        break;
                    } else {
                        handleMenuItem(R.id.sb_change_sides);
                    }
            }
            return false;
        }

        @Override public boolean onSwipe(View view, Direction direction, float maxD, float percentageOfView) {
            if ( ViewUtil.isWearable(ScoreBoard.this) ) {
                // wearable screen is typically to small to interpret swipe events
                // return false;
                // 2023-09-17: Wear OS guidelines says: swipe must implement 'dismiss'
                return handleMenuItem(R.id.sb_exit);
            }
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
                            // perform undo if swiped horizontally over server score button (= person who scored last)
                            handleMenuItem(R.id.dyn_undo_last);
                        } else {
                            Player pLastScorer = matchModel.getLastScorer();
                            if ( player.equals(pLastScorer) ) {
                                // perform undo if swiped horizontally over last scorer
                                handleMenuItem(R.id.dyn_undo_last);
                            } else {
                                if ( matchModel.getScore(player) > 0 ) {
                                    // present user with dialog to remove last scoreline for other than latest scorer ...
                                    confirmUndoLastForNonScorer(player);
                                }
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
                            changeSide(player);
                            return true;
                        } else if (nextServeSide.equals(ServeSide.R) && direction.equals(Direction.W)) {
                            // change from right to left
                            changeSide(player);
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
                        // just to have a way to get to the settings if no actionbar is visible on Wear OS
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
            if ( matchModel == null ) { return false; }

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
                if ( ViewUtil.isWearable(ScoreBoard.this) ) {
                    // for convenience shortly display the current time on a wearable, so players can consult time by long pressing both score buttons
                    // to see how long players still may remain on court if the have only a limited time to play
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
                    String sCurrentTime = sdf.format(new Date());
                    iBoard.showMessage(sCurrentTime, 3);
                } else {
                    if ( matchModel.hasStarted() ) {
                        return handleMenuItem(R.id.sb_match_format);
                    } else {
                        return handleMenuItem(R.id.change_match_format);
                    }
                }
            }
            // TODO: this only works if side buttons are NOT on top of score buttons (portrait only for now)
            if ( lIds.containsAll(Arrays.asList(R.id.btn_side1, R.id.btn_side2)) ) {
                // Nothing for end user yet
                if ( PreferenceValues.isBrandTesting(ScoreBoard.this) ) {
                    toggleBrand();
                }
            }
            // TODO: if playing double switch player names if clicking the two child view of R.id.txt_player1
            return false;
        }
    };
    /** side1 and side2 simulatiously */
    private void toggleBrand() {
        Brand newBrandForTesting = ListUtil.getNextEnum(Brand.brand);
        RWValues.setEnum(PreferenceKeys.squoreBrand, ScoreBoard.this, newBrandForTesting);
        Brand.setBrandPrefs(ScoreBoard.this);
        Brand.setSportPrefs(ScoreBoard.this);
        DynamicListPreference.deleteCacheFile(ScoreBoard.this, PreferenceKeys.colorSchema.toString());
        //doRestart(ScoreBoard.this);
        Toast.makeText(this, String.format("Restart to make new brand %s effective", newBrandForTesting), Toast.LENGTH_LONG).show();
    }

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
                    handleMenuItem(R.id.sb_change_sides);
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
      //Log.d(TAG, "Re-enabled score button for player " + player);
    }
    void disableScoreButton(View view) {
        view.setEnabled(false);
      //Log.d(TAG, "Disabling score button for model " + matchModel);
    }
    private class ScoreButtonListener implements View.OnClickListener
    {
        @Override public void onClick(View view) {
          //Log.d(TAG, "Received click for model " + matchModel);
            if ( clearBLEConfirmationStatus() ) { return; }

            Player player = IBoard.m_id2player.get(view.getId());
            if ( matchModel.isPossibleGameBallFor(player) && (bGameEndingHasBeenCancelledThisGame == false) ) {
                // score will go to game-end, and most likely a dialog will be build and show. Prevent any accidental score changes while dialog is about to be shown
                // mainly to prevent odd behaviour of the app for when people are 'quickly' entering a score by tapping rappidly on score buttons
                disableScoreButton(view);
            }
            if ( dialogManager.dismissIfTwoTimerView() /*cancelTimer()*/ ) {
                // only possible for inline timer
                if ( isDialogShowing() ) {
                    // e.g DoublesFirstServer may auto-show after timer is cancelled
                    return;
                }
            }
            enableScoreButton(player.getOther());
            handleMenuItem(R.id.pl_change_score, player);
        }
    }

    private void confirmUndoLastForNonScorer(final Player p) {
        AlertDialog.Builder cfunls = new MyDialogBuilder(this);
        cfunls.setTitle(R.string.uc_undo)
                .setIcon(R.drawable.u_turn)
                .setTitle(getString(R.string.sb_remove_last_score_for_x, matchModel.getName(p) ))
                .setPositiveButton(R.string.cmd_yes, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        handleMenuItem(R.id.sb_undo_last_for_non_scorer, p);
                    }
                })
                .setNegativeButton(R.string.sb_adjust_score, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        handleMenuItem(R.id.sb_adjust_score, p);
                    }
                })
                .setNeutralButton(R.string.cmd_no, null)
                .show();
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
            if ( matchModel.isDoubles() ) {
                if ( player.equals( matchModel.getServer() ) ) {
                    DoublesServe inOutClickedOn = getInOrOut(view);
                    DoublesServe inOut = matchModel.getNextDoubleServe(player);
                    if ( (inOutClickedOn                         != null)
                      && (inOutClickedOn.equals(DoublesServe.NA) == false)
                      && (inOutClickedOn.equals(inOut)           == false)
                       ) {
                        // clicked on serve side button of non-serving doubles player of the same team
                        if ( Brand.isSquash() ) {
                            matchModel.changeDoubleServe(player);
                        }
                    } else {
                        // clicked on serve side button of serving doubles player of the same team
                        if ( Brand.isSquash() ) {
                            changeSide(player);
                        }
                    }
                } else {
                    // toggle receiver
                    //matchModel.changeDoubleReceiver(null); // swap players in stead
                }
            } else {
                if ( onClickXTimesHandler == null ) {
                    onClickXTimesHandler = new OnClickXTimesHandler(300, 10);
                }
                if ( onClickXTimesHandler.handle() ) {
                    if ( isWearable() ) {
                        handleMenuItem(R.id.sb_about);
                        return;
                    }
                    if ( mBluetoothAdapter == null ) {
                        Toast.makeText(ScoreBoard.this, R.string.bt_no_bluetooth_on_device, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int[] iMenuIds = new int[] { R.id.sb_ble_devices
                                               //, R.id.sb_demo
                                               //, R.id.sb_toggle_demo_mode
                                               //, R.id.sb_download_posted_to_squore_matches
                                               //, R.id.android_language
                                               };
                    int iToggled = ViewUtil.setMenuItemsVisibility(mainMenu, iMenuIds, true);
                    if ( iMenuIds[0] == R.id.sb_ble_devices ) {
                        promoteAppToUseBLE();
                    }
                    if ( iToggled > 1 ) {
                        Toast.makeText(ScoreBoard.this, String.format("Additional %d menu items made available", iToggled), Toast.LENGTH_LONG).show();
                        Mode newMode = m_mode; // toggleDemoMode(null);
                        if ( newMode.equals(Mode.ScreenShots) ) {
                            PreferenceValues.setEnum   (PreferenceKeys.BackKeyBehaviour            , ScoreBoard.this, BackKeyBehaviour.UndoScoreNoConfirm); // for adb demo/screenshots script
                            PreferenceValues.setBoolean(PreferenceKeys.showFullScreen              , ScoreBoard.this, true);                         // for adb demo/screenshots script
                            PreferenceValues.setBoolean(PreferenceKeys.showActionBar               , ScoreBoard.this, false);                        // for adb demo/screenshots script
                            PreferenceValues.setBoolean(PreferenceKeys.showAdjustTimeButtonsInTimer, ScoreBoard.this, false);                        // for cleaner screenshots
                            PreferenceValues.setBoolean(PreferenceKeys.showUseAudioCheckboxInTimer , ScoreBoard.this, false);                        // for cleaner screenshots
                        } else {
                            PreferenceValues.setEnum   (PreferenceKeys.BackKeyBehaviour            , ScoreBoard.this, BackKeyBehaviour.PressTwiceToExit);
                            PreferenceValues.setBoolean(PreferenceKeys.showAdjustTimeButtonsInTimer, ScoreBoard.this, R.bool.showAdjustTimeButtonsInTimer_default);
                            PreferenceValues.setBoolean(PreferenceKeys.showUseAudioCheckboxInTimer , ScoreBoard.this, R.bool.showUseAudioCheckboxInTimer_default);
                        }
                        if ( newMode.equals(Mode.Debug) ) {
                            PreferenceValues.setString(PreferenceKeys.FeedFeedsURL, ScoreBoard.this, getString(R.string.feedFeedsURL_default) + "?suffix=.new");
                            //PreferenceValues.setNumber (PreferenceKeys.viewedChangelogVersion, ScoreBoard.this, PreferenceValues.getAppVersionCode(ScoreBoard.this)-1);
                        }
                    }
                }
                if ( Brand.isTabletennis() ) {
                    if ( matchModel.isInMode(TabletennisModel.Mode.Expedite) ) {
                        changeSide(player);
                    } else {
                        // Who will serve at what score is totally determined by who started serving the first point
                        if ( (matchModel.hasStarted() == false) && (player.equals(matchModel.getServer()) == false) ) {
                            changeSide(player);
                        }
                    }
                } else if ( Brand.isBadminton() ) {
                    // Who will serve at what score is totally determined by the score: TODO: not in 'old' rules (double only?)
                    if ( (matchModel.hasStarted() == false) && (player.equals(matchModel.getServer()) == false) ) {
                        changeSide(player);
                    }
                } else if ( Brand.isRacketlon() ) {
                    // Who will serve at what score is totally determined by who started serving the first point
                    if ( (matchModel.hasStarted() == false) && (player.equals(matchModel.getServer()) == false) ) {
                        changeSide(player);
                    }
                } else {
                    changeSide(player);
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
                ScoreBoard.getMatchModel().setDirty();
                ColorPrefs.clearColorCache();
                ScoreBoard.this.onRestart();
                return true;
            }
            showColorPicker(pl);
            return true;
        }
    }

    private class GameScoresListener implements View.OnLongClickListener, View.OnClickListener {
        private long lActionBarToggledAt = 0L;
        @Override public void onClick(View view) {
            if ( Brand.isGameSetMatch() ) {
                toggleSetScoreView();
            } else if ( Brand.isRacketlon() == false ) {
                toggleGameScoreView();
            } else {
                long currentTime = System.currentTimeMillis();
                if ( currentTime - lActionBarToggledAt > 1500 ) {
                    // prevent single click show history being triggered after a long click
                    if ( isWearable() ) {
                        // score details NOT yet optimized for wearables
                    } else {
                        handleMenuItem(R.id.sb_score_details);
                    }
                } else {
                    Log.d(TAG, "Skip single click for now... ");
                }
            }
        }

        @Override public boolean onLongClick(View view) {
            ActionBar actionBar = getXActionBar();
            if ( (actionBar != null) && (isWearable() == false) /*&& (PreferenceValues.showActionBar(ScoreBoard.this) == false)*/ ) {
                toggleActionBar(actionBar);
                lActionBarToggledAt = System.currentTimeMillis();
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
                handleMenuItem(R.id.dyn_undo_last); // TODO: enough 'undo' options, we can use something else here
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
            if ( matchModel == null ) { return false; }

            if ( matchModel.isDoubles() ) {
                // toggle player names of the long clicked team
                handleMenuItem(R.id.sb_swap_double_players, pl);
            } else {
                if ( isWearable() && matchModel.hasStarted()==false ) {
                    if ( PreferenceValues.isBrandTesting(ScoreBoard.this) ) {
                        toggleBrand();
                    } else {
                        // on wearable allow changing name with minimal interface
                        if ( handleMenuItem(R.id.pl_change_name, pl) == false ) {
                            handleMenuItem(R.id.pl_show_conduct, pl);
                        };
                    }
                } else {
                    handleMenuItem(R.id.pl_show_conduct, pl);
                }
            }

            return true;
        }

        @Override public void onClick(View view) {
            int viewId = getXmlIdOfParent(view);
            Player pl = IBoard.m_id2player.get(viewId);
            if ( pl == null ) { return; }
            if ( Brand.isSquash() ) {
                handleMenuItem(R.id.pl_show_appeal, pl);
            } else {
                if ( isWearable() ) {
                    // for non-squash allow changing name by short click as well
                    handleMenuItem(R.id.pl_change_name, pl);
                }
            }
        }
    }

    //-------------------------------------------------------------------------
    // Swap (double) players
    //-------------------------------------------------------------------------

    /** might present a dialog to the user, 'Based-On-Preference'. Returns true if a dialog was presented to the user. */
    private boolean swapSides_BOP(Feature fChangeSides) {
        if ( fChangeSides == null ) {
            fChangeSides = PreferenceValues.useChangeSidesFeature(ScoreBoard.this);
        }
        switch( fChangeSides ) {
            case DoNotUse:
                return false;
            case Suggest:
                if ( Brand.supportChooseSide() /*Brand.isGameSetMatch()*/ ) {
                    if ( isLandscape() || ViewUtil.isWearable(this) ) {
                        showChangeSideFloatButton(true);
                    } else {
                        _confirmChangeSides(null);
                    }
                } else {
                    if ( Brand.isRacketlon() ) {
                        // no change side in squash discipline
                        RacketlonModel racketlonModel = (RacketlonModel) matchModel;
                        Sport sportForSet = racketlonModel.getSportForSetInProgress();
                        if ( Sport.Squash.equals(sportForSet) ) {
                            if ( racketlonModel.isDoubles() == false ) {
                                return false;
                            }
                        }
                    }
                    if ( isWearable() ) {
                        showChangeSideFloatButton(true);
                    } else {
                        if ( matchModel.isUsingHandicap() ) {
                            // TODO: suggest at the right time when to switch sides
                        } else {
                            _confirmChangeSides(null);
                        }
                    }
                }
                return true;
            case Automatic:
                handleMenuItem(R.id.sb_change_sides);
                return false;
        }
        return false;
    }

    /** @Deprecated */
    private void _confirmChangeSides(Player leader) {
        ChangeSides changeSides = new ChangeSides(this, matchModel, this);
        changeSides.init(leader);
        addToDialogStack(changeSides);
    }

    /** Invoked by SideToss */
    public void swapSides(Integer iToastLength, Player pFirst) {
        if ( matchModel == null ) { return; }

        if ( pFirst == null ) {
            pFirst = IBoard.togglePlayer2ScreenElements();
        } else {
            pFirst = IBoard.initPlayer2ScreenElements(pFirst);
        }
        matchModel.triggerListeners();

        if ( (iToastLength != null) && ( iToastLength == Toast.LENGTH_LONG || iToastLength == Toast.LENGTH_SHORT ) ) {
            String sMsg = getString( matchModel.isDoubles()? R.string.teams_have_swapped_sides : R.string.player_names_swapped); // + " (" + pFirst + ")";
            Toast.makeText(this, sMsg, iToastLength).show();
        }
        swapSidesOnBT(pFirst);
    }

    private void swapSidesOnBT(Player pFirst) {
        Player pFirstOnBTConnected = pFirst;
        boolean bKeepLROnConnectedDeviceMirrored = PreferenceValues.BTSync_keepLROnConnectedDeviceMirrored(this);
        if ( bKeepLROnConnectedDeviceMirrored ) {
            pFirstOnBTConnected = pFirstOnBTConnected.getOther();
        }
        writeMethodToBluetooth(BTMethods.swapPlayers, pFirstOnBTConnected);
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

        AlertDialog.Builder ab = new MyDialogBuilder(this);
        ab.setMessage   (R.string.sb_choose_team)
                .setIcon(R.drawable.circle_2arrows)
                .setPositiveButton(Player.A.toString()    , dialogClickListener)
                .setNegativeButton(Player.B.toString()    , dialogClickListener)
                .setNeutralButton (android.R.string.cancel, dialogClickListener)
                .show();
    }

    public void _swapDoublePlayers(Player pl) {
        _swapDoublePlayers(new Player[] {pl}, true);
    }
    public void _swapDoublePlayers(Player[] pls, boolean bShowToast) {
        if ( pls == null ) { return; }
        for(Player pl: pls) {
            matchModel.swapDoublesPlayerNames(pl);
            writeMethodToBluetooth(BTMethods.swapDoublePlayers, pl);
        }
        if ( bShowToast ) {
            String sMsg = getString(R.string.double_player_names_of_team_x_swapped, ListUtil.join(Arrays.asList(pls), " & "));
            Toast.makeText(this, sMsg, Toast.LENGTH_LONG).show();
        }
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

            int iOfficialRulesResId = PreferenceValues.getSportTypeSpecificResId(ScoreBoard.this, R.string.sb_official_rules__Squash, 0);
        startSection(R.string.uc_new   );
            addItem(R.id.sb_enter_singles_match , R.string.sb_new_singles_match    ,         R.drawable.circled_plus          , R.bool.useSinglesMatch__Default   );
            addItem(R.id.sb_select_static_match , R.string.sb_select_static_match  ,         R.drawable.ic_action_view_as_list, R.bool.useMyListFunctionality__Default);
            addItem(R.id.sb_select_feed_match   , R.string.sb_select_feed_match    ,         R.drawable.ic_action_web_site    , R.bool.useMatchFromFeed__Default  );
            addItem(R.id.sb_enter_doubles_match , R.string.sb_new_doubles_match    ,         R.drawable.circled_plus          );
        startSection(R.string.uc_edit   );
            addItem(R.id.sb_clear_score         , R.string.sb_clear_score          ,         R.drawable.circle_2arrows   );
            addItem(R.id.sb_adjust_score        , R.string.sb_adjust_score         , android.R.drawable.ic_menu_edit        );
            addItem(R.id.sb_edit_event_or_player, R.string.sb_edit_event_or_player , android.R.drawable.ic_menu_edit        );
            addItem(R.id.change_match_format    , R.string.pref_MatchFormat        ,         R.drawable.ic_action_mouse     );
        startSection(R.string.uc_show   );
            addItem(R.id.sb_toss                , R.string.sb_cmd_toss             ,         R.drawable.toss_white          );
            addItem(R.id.sb_timer               , R.string.sb_timer                ,         R.drawable.timer               );
            addItem(R.id.sb_injury_timer        , R.string.sb_injury_timer         ,         R.drawable.timer               , R.bool.useInjuryTimers__Squash);
            addItem(R.id.sb_player_timeout_timer, R.string.sb_player_timeout_timer ,         R.drawable.timer               , R.bool.usePlayerTimeoutTimers__Squash);
            addItem(R.id.sb_score_details       , R.string.sb_score_details        ,         R.drawable.ic_action_chart_line);
            if ( false && PreferenceValues.isFCMEnabled(ScoreBoard.this)) {
                // TODO: allow disable/enable on items in menudrawer
                addItem(R.id.sb_fcm_info, R.string.FCM_Info, R.drawable.dummy);
            }
        startSection(R.string.goto_help );
            addItem(R.id.sb_quick_intro         , R.string.Quick_intro             , android.R.drawable.ic_dialog_info         );
            addItem(R.id.sb_help                , R.string.goto_help               , android.R.drawable.ic_menu_help           );
            addItem(R.id.sb_official_rules      , iOfficialRulesResId              , android.R.drawable.ic_menu_search         );
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
            addItem(iActionId, iCaptionId, iImageId, 0);
        }
        private void addItem(int iActionId, int iCaptionId, int iImageId, int iShowResid) {
            if ( iShowResid != 0 ) {
                iShowResid = PreferenceValues.getSportTypeSpecificResId(ScoreBoard.this, iShowResid, iShowResid);
                boolean bShow = getResources().getBoolean(iShowResid);
                if ( bShow == false ) {
                    Log.d(TAG, "Specifically not showing " + getResources().getResourceName(iActionId) );
                    return;
                }
            }
            if ( iCaptionId == 0 ) {
                // e.g. for Tennis AND Padel rules... menu option
                return;
            }
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
    private boolean m_bHapticFeedbackPerPoint  = false;
    private boolean m_bHapticFeedbackOnGameEnd = false;

    /** onCreate() is followed by onstart() onresume(). Also called after orientation change */
    @Override public void onCreate(Bundle savedInstanceState) {
        // Handle the splash screen transition.
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        splashScreen.setOnExitAnimationListener(splashScreenViewProvider -> {
            Log.w(TAG, "setOnExitAnimationListener");
            splashScreenViewProvider.remove();
        });
        super.onCreate(savedInstanceState);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        //onResume_BluetoothMediaControlButtons();

        // one-of correct incorrect default
        if ( Brand.isSquash() ) {
            if ( PreferenceValues.getAppVersionCode(this) == 156 ) {
                int iVersionRunCount = PreferenceValues.getVersionRunCount(this);
                if ( iVersionRunCount < 3 ) {
                    PreferenceValues.setBoolean(PreferenceKeys.swapPlayersOn180DegreesRotationOfDeviceInLandscape, this, false);
                }
            }
        }

        if ( PreferenceValues.isRunningInMainCodeBase(this) ) {
            // if we are running as unbranded but one ore more other brand values are uncommented
            if ( PreferenceValues.isBrandTesting(this) ) {
                Brand overwriteBrand = PreferenceValues.getOverwriteBrand(this);
                if ( overwriteBrand.equals(Brand.Squore) == false ) {
                    Brand.brand = overwriteBrand;
                }
                Brand.setBrandPrefs(this);
                Brand.setSportPrefs(this);
                Model mTmp = Brand.getModel();
                File lastMatchFile = PersistHelper.getLastMatchFile(this);
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

        dialogManager = DialogManager.getInstance();

        handleStartedFromOtherApp();

        ViewUtil.setFullScreen(getWindow(), PreferenceValues.showFullScreen(this));

        boolean bOrientationChangeRequested =   initAllowedOrientation(this);

        //if ( bOrientatienChangeRequested ) return; // DO NOT DO THIS

        initShowActionBar();

        if ( bOrientationChangeRequested == false ) {
            if ( isWearable() ) {
                setContentView(R.layout.percentage); // triggers onContentChanged()

                if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1 /* 25 */ ) {
                    int count = WearableButtons.getButtonCount(this);
                    Log.d(TAG, String.format("Wearable button count : %d ", count)); // always returns at least 1 for the OS? (unless in an emulator?)
                    if ( count >= 3 ) { // we need at least 2 knowing we can not use the one specifically for the OS
                        int[] iCheckButtons = new int[] {KeyEvent.KEYCODE_STEM_1, KeyEvent.KEYCODE_STEM_2, KeyEvent.KEYCODE_STEM_3/*, KeyEvent.KEYCODE_STEM_PRIMARY*/};
                        Params mX2wearableButton = new Params();
                        Player pToChangeScoreFor = Player.A;
                        for( int iCheckButton: iCheckButtons ) {
                            WearableButtons.ButtonInfo buttonInfo = WearableButtons.getButtonInfo(this, iCheckButton);
                            if (buttonInfo != null) {
                                mX2wearableButton.addToList(buttonInfo.getX(), iCheckButton, true);
                                Log.d(TAG, String.format("%d buttonLabel()                : %s", iCheckButton, WearableButtons.getButtonLabel(this, iCheckButton))); // Top right, Bottom right, Center Right
                                m_wearableButtonToPlayer.put(iCheckButton, pToChangeScoreFor);
                                pToChangeScoreFor = pToChangeScoreFor.getOther();
                                // how to ensure OS one is not in here?:
                                // For my fossil count returns 3 and I can actually use stem1 and stem2 in the app, KEYCODE_STEM_PRIMARY is for the OS, stem3 does not exits
                            } else {
                                Log.d(TAG, String.format("No such button %d", iCheckButton));
                            }
                        }
                        if ( m_wearableButtonToPlayer.size() > 2 ) {
                            // more than 2 usable buttons found: atempt to have at least symmetrical placed buttons to opposite players
                            for(Object IXpos: mX2wearableButton.keySet() ) {
                                List lButtonKeys = mX2wearableButton.getList(IXpos, ",", true);
                                if ( (count>2) && lButtonKeys.size() == 2 ) {
                                    // assume we have found 2 buttons on a 'round' wearable: one above and one below the 'main' button in the center
                                    Player player = Player.A;
                                    for(Object oKey: lButtonKeys) {
                                        int iKey = Integer.parseInt(String.valueOf(oKey));
                                        m_wearableButtonToPlayer.put(iKey, player);
                                        player = player.getOther();
                                    }
                                }
                            }
                        }
                    }
                }

            } else {
                // not wearable
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

                FrameLayout frameLayout = findViewById(R.id.content_frame);
                LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View view = null;
                if ( ViewUtil.isLandscapeOrientation(this) ) {
                    LandscapeLayoutPreference llp = PreferenceValues.getLandscapeLayout(this);
                    view = inflater.inflate(llp.getLayoutResourceId(), null);
                    if ( llp.equals(LandscapeLayoutPreference.Default) == false ) {
                        m_bNoFloatingButtons = true;
                        PreferenceValues.setOverwrite(PreferenceKeys.floatingMessageForGameBallOn, "");

                        // hide 'set' related elements: For consistency This code should be moved to iBoard (or maybe it is not required anymore)
                        if ( Brand.isGameSetMatch() == false ) {
                            int[] iViewIds = new int[] {R.id.btn_setswon1, R.id.btn_setswon2, R.id.space_scoreset_scoregame};
                            for(int i: iViewIds) {
                                View vTxt = view.findViewById(i);
                                if ( vTxt == null ) { continue; }
                                vTxt.setVisibility(View.INVISIBLE);
                                ViewGroup.LayoutParams loParams = vTxt.getLayoutParams();
                                if ( loParams instanceof ConstraintLayout.LayoutParams) {
                                    ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) loParams;
                                    layoutParams.matchConstraintPercentWidth = 0.0f;
                                    //layoutParams.width = 0;
                                    vTxt.setLayoutParams(layoutParams);
                                }
                            }
                        }
                    }
                } else {
                    view = inflater.inflate(R.layout.constraint, null);
                }
                frameLayout.addView(view);
            }
        }

        Chronometer.OnChronometerTickListener gameDurationTickListener = null;
        if ( Brand.isTabletennis() ) {
            gameDurationTickListener = new TTGameDurationTickListener();
        }

        Display display = ((WindowManager) this.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        iBoard = new IBoard(matchModel, this, display, (ViewGroup) findViewById(android.R.id.content), gameDurationTickListener);

        ViewGroup tlGameScores = (ViewGroup) findViewById(R.id.gamescores);
        if ( tlGameScores != null ) {
            tlGameScores.setOnTouchListener    (gamesScoresGestureListener);
        }
        for(Player p: Player.values() ) {
            View v = findViewById(IBoard.m_player2gamesWonId.get(p));
            if ( v != null ) {
                v.setOnTouchListener(gamesScoresGestureListener);
            }
        }

        initScoreButtons();

        initServeSideButtons();
        initServeSideButtonListeners();
        iBoard.initGameScoreView();
        iBoard.initTimerButton();
        iBoard.initBranded();
        iBoard.initFieldDivision();

        ViewGroup view   = (ViewGroup) findViewById(android.R.id.content);
        ViewGroup layout = (ViewGroup) view.getChildAt(0); // the relative layout
        while ( (layout != null) &&  (layout.getId()!= R.id.squoreboard_root_view) ) {
            layout = (ViewGroup) layout.getChildAt(0);
        }
        if ( layout != null ) {
            layout.setOnTouchListener(new TouchBothListener(clickBothListener, longClickBothListener));
        }

        m_bHapticFeedbackPerPoint  = PreferenceValues.hapticFeedbackPerPoint(ScoreBoard.this);
        m_bHapticFeedbackOnGameEnd = PreferenceValues.hapticFeedbackOnGameEnd(ScoreBoard.this);

        initColors();
        initCountries();

        initCasting(); // also onresume!!
        setModelForCast(matchModel);

        initScoreBoard(PersistHelper.getLastMatchFile(this));

        initPlayerButtons();

        handleStartupAction();

        rateMeMaybe_init();

        //registerNfc();
        onCreateInitBluetooth();

        //setUpBillingProcessor();
    }
    private boolean m_bNoFloatingButtons = false;

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

    private boolean m_liveScoreShare = false;

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
                keepScreenOn((matchModel != null) && matchModel.matchHasEnded() == false && matchModel.isLocked() == false);
                break;
        }

        updateDemoThread(this);

        m_bHapticFeedbackPerPoint  = PreferenceValues.hapticFeedbackPerPoint(this);
        m_bHapticFeedbackOnGameEnd = PreferenceValues.hapticFeedbackOnGameEnd(this);
        m_liveScoreShare           = PreferenceValues.isConfiguredForLiveScore(this);

        updateMicrophoneFloatButton();
        updatePowerPlayIcons();
        updateTimerFloatButton();
        iBoard.updateGameBallMessage("onResume");
        if ( Brand.isGameSetMatch() ) {
            // to prevent 'set ball' being show while it is gameball: TODO: still show if actual setball
            iBoard.showGameBallMessage(false, null);
        }
        iBoard.updateGameAndMatchDurationChronos();
        if ( matchModel != null ) {
            showShareFloatButton(matchModel.isPossibleGameVictory(), matchModel.matchHasEnded()); // icon may have changed
            String modelSource = matchModel.getSource();
            if ( StringUtil.isNotEmpty(modelSource) ) {
                PreferenceValues.guessShareAction(modelSource, this);
            }
        }

        initModelListeners();

        initScoreHistory();

        //onResumeNFC();
        onResumeFCM();
        onResumeBlueTooth();
        onResumeInitBluetoothBLE();
        onResume_BluetoothMediaControlButtons();
        onResumeWearable();
        if ( PreferenceValues.isCastRestartRequired() ) {
            onActivityStop_Cast();
            castHelper         = null;
            //cdtInitCastDelayed = null;
            initCasting();
        }

        onActivityResume_Cast();
        onResumeSpeak();

        onResumeURL();

        dialogManager.showNextDialogIfChildActivity();
        if ( timer != null ) {
            // e.g. timer was showing before orientation change. Show it again
            Timer.addTimerView(iBoard.isPresentation(), iBoard);
        }
        Timer.removeTimerView(false, NotificationTimerView.class);
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
    }

    private long lLastRotaryEventHandled = 0L;
    private long lMinTimeBetweenHandling2RotaryEvents = 3000L;

    private long lLastRotaryEventAdded = 0L;
    private long lMaxTimeBetweenAdding2Deltas         = 300L;
    private float lRotationDeltaCumulative = 0.0f;

    private float lMinimumRotationToScorePoint = 2.5f; // TODO: sensitivity as setting
    @Override public boolean onGenericMotionEvent(MotionEvent event) {
      //Log.d(TAG, "[onGenericMotionEvent] rotary : event : " + event);
        if ( ViewUtil.isWearable(this) ) {
            if ( PreferenceValues.wearable_allowScoringWithRotary(this) ) {
                // modify score using rotary/bezel
                if ( ( event.getAction() == MotionEvent.ACTION_SCROLL )
                    && event.isFromSource(InputDeviceCompat.SOURCE_ROTARY_ENCODER)
                   ) {
                    long lNow = System.currentTimeMillis();

                    // keep the total rotation in certain direction up to date
                    {
                        float lRotationDelta = event.getAxisValue(MotionEventCompat.AXIS_SCROLL);

                        long lTimeSinceLastRotaryEvent = lNow - lLastRotaryEventAdded;
                        if ( lTimeSinceLastRotaryEvent > lMaxTimeBetweenAdding2Deltas ) {
                            lRotationDeltaCumulative = 0; // restart counting from zero
                        }
                        lRotationDeltaCumulative += lRotationDelta;
                        //Log.d(TAG, "[onGenericMotionEvent] rotary : lRotationDeltaCumulative : " + lRotationDeltaCumulative);
                    }

                    lLastRotaryEventAdded = lNow;

                    long lTimeSinceLastRotaryChangeScore = lNow - lLastRotaryEventHandled;
                    if ( lTimeSinceLastRotaryChangeScore > lMinTimeBetweenHandling2RotaryEvents ) {
                        if ( Math.abs(lRotationDeltaCumulative) > lMinimumRotationToScorePoint ) {
                            Player player = Player.B;
                            if ( lRotationDeltaCumulative > 0 ) {
                                player = Player.A;
                            }
                            boolean bFlipRotaryRotation = false; // TODO: setting, but wearable has no easily accessible setting screen yes
                            if ( bFlipRotaryRotation ) {
                                player = player.getOther();
                            }
                            Log.d(TAG, "Handling rotary event to change score for " + player);
                            handleMenuItem(R.id.pl_change_score, player);
                            lLastRotaryEventHandled = lNow;
                            lRotationDeltaCumulative = 0;

                            // typically this type of scoring is done on a wearable without looking at the device. Give short vibration to indicate the point scoring was registered
                            SystemUtil.doVibrate(ScoreBoard.this, 200);
                            return true;
                        } else {
                          //Log.d(TAG, "Ignoring rotary event: to little deltaCum " + lRotationDeltaCumulative + " < " + lMinimumRotationToScorePoint);
                        }
                    } else {
                      //Log.d(TAG, "Ignoring rotary event: to soon: " + lTimeSinceLastRotaryChangeScore + " < " + lMinTimeBetweenHandling2RotaryEvents);
                    }
                }
            }
        }

        return super.onGenericMotionEvent(event);
    }

    private final Map<Integer,Player> m_wearableButtonToPlayer = new HashMap<>();

    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ( isWearable() ) {
            if ( PreferenceValues.wearable_allowScoringWithHardwareButtons(this) ) {
                // modify score using hardware buttons
                Log.d (TAG,"onKeyDown: " + keyCode + ", repeat count: " + event.getRepeatCount());
                if ( event.getRepeatCount() == 0 ) { // to ensure if long-pressed, we don't react more than once to the same press
                    if ( m_wearableButtonToPlayer.size() >= 2 ) {
                        Player player = m_wearableButtonToPlayer.get(keyCode);
                        if ( player != null ) {
                            handleMenuItem(R.id.pl_change_score, player);
                            // typically this type of scoring is done on a wearable without looking at the device. Give short vibration to indicate the point scoring was registered
                            SystemUtil.doVibrate(ScoreBoard.this, 200);
                            return true;
                        }
                    }
                }
            }
        }
        return super.onKeyDown(keyCode, event);
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
        if ( matchModel == null ) { return; }
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
            // 20191027: first cancel running timer if back is pressed
            boolean bTimerIsShowing = (ScoreBoard.timer != null) && ScoreBoard.timer.isShowing();
            if ( bTimerIsShowing ) {
                cancelTimer();
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

                    if ( isWearable() == false ) {
                        // do not show action bar in wearables... to BIG
                        ActionBar xActionBar = getXActionBar();
                        if ( xActionBar != null) {
                            if ( xActionBar.isShowing() == false ) {
                                xActionBar.show();
                                return;
                            }
                        }
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
                    AlertDialog.Builder cfun = new MyDialogBuilder(this);
                    cfun.setTitle(R.string.uc_undo)
                        .setIcon(R.drawable.u_turn)
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
                    changeSide(matchModel.getServer());
                    break;
                }
            }
        }
    }

    /** using dispatchKeyEvent() seems to work fine for hardware related actions/keys: we check on KeyEvent.ACTION_UP deliberately. KeyEvent.ACTION_DOWN is triggered very often if you HOLD the volume button */
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
            if (action == KeyEvent.ACTION_UP) {
                switch (keyCode) {
                    case KeyEvent.KEYCODE_BACK:
                        handleBackPressed();
                        return true;
                    case KeyEvent.KEYCODE_MENU: // Hardware menu buttons are slowly being phased out of android
                        ToggleResult toggleResult = ScoreBoard.this.toggleActionBar(getXActionBar());
                        if ( toggleResult.equals(ToggleResult.nothing) == false ) {
                            PreferenceValues.setBoolean(PreferenceKeys.showActionBar,this,toggleResult.equals(ToggleResult.setToTrue)?true:false);
                        }
                        return true;
/*
                    case KeyEvent.KEYCODE_HOME: // does not seem to work, at least in android 5.1+
                        return handleHomePressed();
*/
                    case KeyEvent.KEYCODE_A      : // A
                    case KeyEvent.KEYCODE_PAGE_UP:
                    case 16 /* pg up */          : {
                        handleMenuItem(R.id.pl_change_score, Player.A);
                        return true;
                    }

                    case KeyEvent.KEYCODE_B        : // B
                    case KeyEvent.KEYCODE_PAGE_DOWN:
                    case 10  /*pg down*/           : {
                        handleMenuItem(R.id.pl_change_score, Player.B);
                        return true;
                    }

                    case KeyEvent.KEYCODE_R          : /*R(evert)*/
                    case KeyEvent.KEYCODE_U          : /*U(ndo)*/
                    case KeyEvent.KEYCODE_FORWARD_DEL: /*delete*/ {
                        handleMenuItem(R.id.sb_undo_last);
                        return true;
                    }
                }
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
                Player first = IBoard.m_firstPlayerOnScreen;
                if ( bUseVolumeButtonsForScoring ) {
                    Player player = bVolumeTrueIsUpFalseIsDown ? first : first.getOther();
                    handleMenuItem(R.id.pl_change_score, player);
                } else {
                    showActivateDialog(context);
                }
            }
            return bUseVolumeButtonsForScoring;
        }

        private void showActivateDialog(final Context context) {
            if ( iDialogPresentedCnt > 1 ) { return; }
            if ( isLandscape() ) { return; }

            // user pressed dialog button short after one another: present choice to turn on entering score using volume buttons
            AlertDialog.Builder choose = new MyDialogBuilder(context);
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

        if ( (matchModel != null) && matchModel.isDirty() ) {
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
            iBoard.initGameScoreView();
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
                    swapSides(null /*Toast.LENGTH_SHORT*/, null);
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
        if ( this instanceof androidx.appcompat.app.AppCompatActivity ) {
            iSomeActionBarId = R.id.sb_overflow_submenu; // 2131689712
        }
        if ( bFromMenu == false ) {
            // typically for a first install via StartAction
            addSequenceItem(iSomeActionBarId, R.string.scv_first_run, ShowcaseView.ShapeType.None).setDismissText(R.string.cmd_ok);
        }
        addSequenceItemOval(R.id.float_toss              , R.string.scv_toss                             );
        addSequenceItemOval(R.id.btn_side1               , R.string.scv_side_buttons__Squash             );
        addSequenceItemRect(R.id.btn_score1              , R.string.scv_big_buttons                      );
        addSequenceItemRect(R.id.dyn_undo_last           , R.string.scv_undo_last                        );
        addSequenceItemOval(R.id.float_timer             , R.string.scv_timer_button__Squash             );
        if ( Brand.isSquash() ) {
        addSequenceItemOval(R.id.sb_official_announcement, R.string.scv_announcement_button__Squash); // not for Racketlon
        }
        addSequenceItemOval(R.id.btn_side2               , R.string.scv_shirt_color__Squash              );
        addSequenceItemOval(R.id.gamescores_container    , R.string.scv_game_scores__Squash              );
        addSequenceItemRect(R.id.txt_player1             , R.string.scv_player_buttons_appeal__Squash    );// not for Racketlon
        addSequenceItemRect(R.id.txt_player2             , R.string.scv_player_buttons_misconduct__Squash);// not for Racketlon
        addSequenceItemRect(R.id.scorehistorytable       , R.string.scv_old_fashioned_scoring__Squash    );// not for Racketlon

        //addSequenceItem(R.id.dyn_score_details       , R.string.scv_score_details); //.setDelay(1000); // a little extra delay to allow share button to appear
        addSequenceItemOval(R.id.float_match_share       , R.string.scv_share_button); //.setDelay(1000); // a little extra delay to allow share button to appear

        addSequenceItemOval(R.id.sb_overflow_submenu     , R.string.scv_overflow_submenu__Squash);
      //addSequenceItemOval(R.id.gamescores_container    , R.string.scv_toggle_action_bar);
        if ( bFromMenu ) {
            addSequenceItemOval(iSomeActionBarId, R.string.scv_few_more_gui_hints__Squash);
        }
        addSequenceItemOval(iSomeActionBarId, R.string.scv_few_more_good_to_knows);
        addSequenceItemOval(R.id.float_new_match         , R.string.scv_new_match_button__Squash).setDismissText(R.string.cmd_ok);

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
        int iNewResId = PreferenceValues.getSportTypeSpecificResId(this, iResid, 0);
        if ( iNewResId > 0 ) {
            iResid = iNewResId;
        } else {
            // ending with __Squash but no e.g. __Racketlon equivalent available. Skip the item
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
                } else if ( Brand.isBadminton() ) {
                    PreferenceValues.setOverwrite(PreferenceKeys.useOfficialAnnouncementsFeature, Feature.DoNotUse.toString());
                    matchModel.setNrOfPointsToWinGame(21);
                    matchModel.setNrOfGamesToWinMatch(2);
                    matchModel.setPlayerNames("Baddy", "Tonny");
                } else if ( Brand.isGameSetMatch() ) {
                    PreferenceValues.setOverwrite(PreferenceKeys.useOfficialAnnouncementsFeature, Feature.DoNotUse.toString());
                    PreferenceValues.setOverwrite(PreferenceKeys.useTimersFeature               , Feature.DoNotUse.toString());
                    GSMModel gsmModel = (GSMModel) matchModel;
                    gsmModel.setNrOfPointsToWinGame(6); // = setNrOfGamesToWinSet
                    gsmModel.setNrOfGamesToWinMatch(2); // = setNrOfSetsToWinMatch
                    gsmModel.setPlayerNames("Paddy", "Tenny");
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
                    if ( Brand.isGameSetMatch() ) {
                        matchModel.changeScore(Player.A);
                        matchModel.changeScore(Player.B);
                        while ( matchModel.getMaxScore() > 0 ) {
                            // continue until a game is won
                            matchModel.changeScore(Player.B);
                        }
                        matchModel.changeScore(Player.A);
                        matchModel.changeScore(Player.A);
                        matchModel.changeScore(Player.B);
                    }
                    break;
                case R.id.float_changesides:
                    // TODO
                    break;
                case R.id.float_timer:
                    PreferenceValues.setOverwrite(PreferenceKeys.showHideButtonOnTimer, false);
                    if ( matchModel.isPossibleGameVictory() == false ) {
                        matchModel.setGameScore_Json(0, nrOfPointsToWinGame, nrOfPointsToWinGame - 4, 5, false);
                        endGame(true);
                    }
                    break;
                case R.id.sb_official_announcement:
                    matchModel.setGameScore_Json(1, nrOfPointsToWinGame -1, nrOfPointsToWinGame +1, 6, false);
                    endGame(true);
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
                    boolean bDontChangePast = true;
                    if ( Brand.isNotSquash() ) {
                        if ( Brand.isGameSetMatch() ) {
                            // TODO: ensure match is ended
                            GSMModel gsmModel= (GSMModel) matchModel;
                            //matchModel.setSetScore_Json(); // TODO
                        } else {
                            // trigger model changes that are not triggered by user step (sb_official_announcement), because some show case screens are skipped for e.g. Racketlon
                            matchModel.setGameScore_Json(1, nrOfPointsToWinGame -1, nrOfPointsToWinGame +1, 6, bDontChangePast);
                            endGame(true);
                        }
                    }
                    if ( matchModel.matchHasEnded() == false) {
                        IBoard.setBlockToasts(true);
                        matchModel.setGameScore_Json(2, nrOfPointsToWinGame,  nrOfPointsToWinGame -5, 5, bDontChangePast);
                        if ( Brand.isRacketlon() ) {
                            // add a score that ends the racketlon match by points
                            matchModel.setGameScore_Json(3, 15, 11, 8, bDontChangePast);
                        } else if ( Brand.isTabletennis() ) {
                            // add a score that ends the tabletennis match
                            matchModel.setGameScore_Json(3, nrOfPointsToWinGame+2, nrOfPointsToWinGame, 8, bDontChangePast);
                            matchModel.setGameScore_Json(4, nrOfPointsToWinGame, nrOfPointsToWinGame-4, 7, bDontChangePast);
                        } else if ( Brand.isBadminton() ) {
                            // add a score that ends the badminton match
                            // best of 3, nothing to do
                        } else if ( Brand.isGameSetMatch() ) {
                            // TODO: add a score that ends the tennis/padel match
                        } else {
                            int iGameInProgress1B = matchModel.getGameNrInProgress();
                            while ( matchModel.matchHasEnded() == false ) {
                                matchModel.setGameScore_Json(iGameInProgress1B-1, nrOfPointsToWinGame +2, nrOfPointsToWinGame, 8, bDontChangePast);
                                iGameInProgress1B++;
                                if ( iGameInProgress1B >= matchModel.getNrOfGamesToWinMatch() * 2 ) { break; } // additional safety precaution
                            }
                        }
                        endGame(true);
                        showShareFloatButton(true, true);
                        IBoard.setBlockToasts(false);
                        matchModel.setLockState(LockState.LockedEndOfMatch);
                    }
                    break;
                case R.id.sb_overflow_submenu:
                case R.id.dyn_undo_last:
                case android.R.id.home:
                    ActionBar actionBar = getXActionBar();
                    if ( (actionBar != null) && (actionBar.isShowing() == false) ) {
                        // show action bar
                        if ( isWearable() == false ) {
                            toggleActionBar(actionBar);
                        }
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
        if ( startupActionCounter > 1 ) { return; }
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
                List<File> lAllMatchFiles = PreviousMatchSelector.getAllPreviousMatchFiles(this);
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
    /** called onCreate and onRestart */
    private void initColors()
    {
        Map<ColorPrefs.ColorTarget, Integer> mColors = ColorPrefs.getTarget2colorMapping(this);
        if ( MapUtil.isEmpty(mColors) ) {
            ColorPrefs.initDefaultColors(mColors);
        }
        this.mColors = mColors;

        iBoard.initColors(mColors);
        // there is a pretty dark color, for ecstatic reasons, make actionbar the same color
        setActionBarBGColor(mColors.get(ColorPrefs.ColorTarget.actionBarBackgroundColor));

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
                    // presume Wear OS
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
     * - MatchReceivedUtil (nfc match)
     * - receive match via bluetooth
     */
    public boolean initScoreBoard(File fJson) {
        boolean bReadFromJsonOK = false;

        Model previous = matchModel;

        if ( fJson == null || matchModel == null ) { // 20140315: invoked from onCreate() after e.g. a dialog or screen orientation switch
            setMatchModel(null);

            bReadFromJsonOK = initMatchModel(fJson);
        }

        if ( (previous != null) && (fJson == null) ) {
            // use player names from previous
            String[] playerNames = previous.getPlayerNames(true, false);
            setPlayerNames(playerNames);
            for(Player player: Model.getPlayers()) {
                matchModel.setPlayerCountry(player, previous.getCountry (player));
                matchModel.setPlayerClub   (player, previous.getClub    (player));
                matchModel.setPlayerAvatar (player, previous.getAvatar  (player));
                matchModel.setPlayerColor  (player, previous.getColor   (player));
                matchModel.setPlayerId     (player, previous.getPlayerId(player));
            }
            // use event from previous
            matchModel.setEvent               (previous.getEventName(), previous.getEventDivision(), previous.getEventRound(), previous.getEventLocation());
            matchModel.setCourt               (previous.getCourt());

            matchModel.setNrOfPointsToWinGame (previous.getNrOfPointsToWinGame());
            matchModel.setNrOfGamesToWinMatch (previous.getNrOfGamesToWinMatch());
            matchModel.setNrOfServesPerPlayer (previous.getNrOfServesPerPlayer());
            matchModel.setNrOfPowerPlaysPerMatch(previous.getNrOfPowerPlaysPerMatch());
            matchModel.setEnglishScoring      (previous.isEnglishScoring      ());
            matchModel.setPlayAllGames        (previous.playAllGames          ());
            matchModel.setTiebreakFormat      (previous.getTiebreakFormat     ());
            matchModel.setHandicapFormat      (previous.getHandicapFormat     ());
            matchModel.setSource              (previous.getSource() , previous.getSourceID() );
            matchModel.setAdditionalPostParams(previous.getAdditionalPostParams());
            matchModel.setReferees            (previous.getReferee(), previous.getMarker(), previous.getAssessor());
            if ( matchModel instanceof GSMModel ) {
                GSMModel gsmModelPrev = (GSMModel) previous;
                GSMModel gsmModelNew = (GSMModel) ScoreBoard.matchModel;
                FinalSetFinish finalSetFinish = gsmModelPrev.getFinalSetFinish();
                gsmModelNew.setFinalSetFinish(finalSetFinish);
                gsmModelNew.setGoldenPointFormat(gsmModelPrev.getGoldenPointFormat());
                gsmModelNew.setStartTiebreakOneGameEarly(gsmModelPrev.getStartTiebreakOneGameEarly());
                gsmModelNew.setNewBalls(gsmModelPrev.getNewBalls());
            }
/*
            for ( Player p: Model.getPlayers() ) {
                matchModel.setGameStartScoreOffset(p, matchModel.getGameStartScoreOffset(p));
            }
*/
            if ( Brand.supportsDoubleServeSequence() && matchModel.isDoubles() ) {
                matchModel.setDoublesServeSequence(previous.getDoubleServeSequence());
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

        return bReadFromJsonOK;
    }

    private boolean initMatchModel(File fJson) {
        boolean bReadFromJsonOK = false;
        if ( matchModel == null ) {
            setMatchModel(Brand.getModel());

            matchModel.setNrOfPointsToWinGame(PreferenceValues.numberOfPointsToWinGame(this));
            matchModel.setNrOfGamesToWinMatch(PreferenceValues.numberOfGamesToWinMatch(this));
            matchModel.setNrOfServesPerPlayer(PreferenceValues.numberOfServesPerPlayer(this));
            matchModel.setEnglishScoring     (PreferenceValues.useHandInHandOutScoring(this));
            matchModel.setTiebreakFormat     (PreferenceValues.getTiebreakFormat      (this));
            matchModel.setHandicapFormat     (PreferenceValues.getHandicapFormat      (this));
            matchModel.setReferees           (PreferenceValues.getRefereeName         (this), PreferenceValues.getMarkerName(this), PreferenceValues.getAssessorName(this));
            if ( PreferenceValues.usePowerPlay(this) ) {
                matchModel.setNrOfPowerPlaysPerMatch(PreferenceValues.numberOfPowerPlaysPerPlayerPerMatch(this));
            } else {
                matchModel.setNrOfPowerPlaysPerMatch(0);
            }

            if ( fJson != null && fJson.exists() ) {
                try {
                    bReadFromJsonOK = matchModel.fromJsonString(fJson);
                    if ( PreferenceValues.lockMatchMV(this).contains(AutoLockContext.WhenMatchIsUnchangeForX_Minutes)) {
                        final int iMinutes = PreferenceValues.numberOfMinutesAfterWhichToLockMatch(this);
                        if ( iMinutes > 0 ) {
                            matchModel.lockIfUnchangedFor(iMinutes);
                        }
                    }
                    iBoard.setModel(matchModel);

                    setModelForCast(matchModel);
                } catch (Exception e) {
                    Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                    bReadFromJsonOK = false;
                }
            } else {
                String[] saPlayers = { getString(R.string.lbl_player) + " A", getString(R.string.lbl_player) + " B"};
                setPlayerNames(saPlayers);

                iBoard.setModel(matchModel);

                setModelForCast(matchModel);
            }
            bGameEndingHasBeenCancelledThisGame = false;
        }
        return bReadFromJsonOK;
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
                    View btn = findViewById(iId);
                    if ( btn != null ) {
                        btn.setVisibility(View.INVISIBLE); // do not use View.GONE or relative layout is screwed up
                    }
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
        } else {
            Toast.makeText(this, "No posting URL configured.", Toast.LENGTH_SHORT).show();
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
        //onNFCPause();
        onActivityPause_Cast();
        onPauseWearable();
        onPause_BluetoothMediaControlButtons();
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
        onActivityStart_Cast();
        if ( (matchModel != null) && matchModel.hasStarted() == false ) {
            int iMatchStartedXSecsAgo = DateUtil.convertTo(System.currentTimeMillis() - matchModel.getMatchStart(), Calendar.SECOND);
            if (iMatchStartedXSecsAgo > 120 ) {
                timestampStartOfGame(GameTiming.ChangedBy.StartAndEndStillEqual);
            }
        }
    }

    /** Invoked when device is rotated. Invoked before onDestroy(). But also if child scoreBoard activity is created?! */
    @Override protected void onStop() {
        onActivityStop_Cast();
        super.onStop();
        persist(false);
        createNotificationTimer();
    }

    /** is called when the Activity is being destroyed either by the system, or by the user, say by hitting back, until the app exits, device rotate. */
    @Override protected void onDestroy() {
        super.onDestroy();
        persist(true);
        MatchTabbed.persist(this);
        ArchiveTabbed.persist(this);
      //cleanup_Speak();
        stopBlueTooth();

        PusherHandler.getInstance().cleanup();

        destroyBillingProcessor();
    }

    private void createNotificationTimer() {
        if ( (timer != null) && timer.getSecondsLeft() > 5 ) {
            m_notificationTimerView = new NotificationTimerView(this);
            timer.addTimerView(false, m_notificationTimerView);
        }
    }

    private NotificationTimerView m_notificationTimerView = null;

    private void persist(boolean bDestroyModel) {
        if (matchModel == null) {
            return;
        }
        PersistHelper.persist(matchModel, this);
        if ( bDestroyModel ) {
            setMatchModel(null);
        }
    }

    // ----------------------------------------------------
    // --------------- restart ------------------------
    // ----------------------------------------------------

    /** Does not seem to work so well if app started for debugging. But when started from home screen it seems to work fine (android 7) */
    private void doRestart() {
        if ( false ) {
            // Navigating From MainActivity to MainActivity.
            // Navigate from this activity to the activity
            // specified by upIntent,
            // basically finishing this activity in the process.
            // IH: seems to work for my Mi device (except the first time?)
            navigateUpTo(new Intent(ScoreBoard.this, ScoreBoard.class));
            startActivity(getIntent());
        } else if ( true ) {
            // after on CLick we are using finish to close and then just after that
            // we are calling startActivity(getIntent()) to open our application
            // IH: seems to work for my Mi device (except the first time?)
            finish();
            startActivity(getIntent());

            // this basically provides animation
/*
            overridePendingTransition(0, 0);
            long time = System.currentTimeMillis();

            // Showing a toast message at the time when we are capturing screenshot
            Toast.makeText(ScoreBoard.this, "Current time in millisecond after app restart" + time, Toast.LENGTH_SHORT).show();
*/
        } else {
            doRestart(this);
        }
    }
    /** @Deprecated */
    private void doRestart(Context c) {
        if (c == null) return;
        // fetch the package-manager so we can get the default launch activity
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
        //System.exit(0); // on my Mi, android starts complaining app keeps crashing
        finish(); // SAME: on my Mi, android starts complaining app keeps crashing
    }

    // ----------------------------------------------------
    // -----------------speak button           ------------
    // ----------------------------------------------------
    private FloatingActionButton speakButton = null;
    private void updateMicrophoneFloatButton() {
        if ( matchModel == null ) { return; }
        boolean bShowSpeakFAB = false;
        if ( matchModel instanceof GSMModel ) {
            GSMModel gsmModel = (GSMModel) matchModel;
            int newBallsInXgames = gsmModel.newBallsInXgames();
            final int iShowUpFront = PreferenceValues.newBallsXGamesUpFront(this);
            bShowSpeakFAB = (newBallsInXgames >= 0) && (newBallsInXgames <= iShowUpFront);

            boolean tieBreakGame = gsmModel.isTieBreakGame();
            if ( tieBreakGame ) {
                int maxScore = gsmModel.getMaxScore();
                bShowSpeakFAB = maxScore == 0;
            }
        } else {
            bShowSpeakFAB = matchModel.gameHasStarted() == false || matchModel.isStartOfTieBreak() || matchModel.isPossibleGameVictory();
        }
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
            speakButton = getFloatingActionButton(R.id.sb_official_announcement, fMargin, iResImage, ColorPrefs.ColorTarget.speakButtonBackgroundColor, null);
        }
        if ( speakButton != null ) {
            speakButton.setHidden(bVisible == false);
        }
    }

    // ----------------------------------------------------
    // -----------------undo button (mainly for wearable)--
    // ----------------------------------------------------
    private FloatingActionButton undoButton = null;
    private void showUndoFloatButton(boolean bVisible) {
        if ( undoButton == null ) {
            float fMargin = 0;
            int iResImage = R.drawable.u_turn;

            ColorPrefs.ColorTarget colorKey = ColorPrefs.ColorTarget.speakButtonBackgroundColor;
            Integer iBG = mColors.get(colorKey);
            if ( iBG != null ) {
                // if we use a light background for the microphone button... switch to the black icon version
                int blackOrWhiteFor = ColorUtil.getBlackOrWhiteFor(iBG);
                if ( blackOrWhiteFor == Color.BLACK ) {
                    iResImage = R.drawable.u_turn_black;
                }
            }
            undoButton = getFloatingActionButton(R.id.float_undo_last, fMargin, iResImage, ColorPrefs.ColorTarget.speakButtonBackgroundColor, null);
            undoButton.setOnLongClickListener(new View.OnLongClickListener() {
                @Override public boolean onLongClick(View v) {
                    return handleMenuItem(R.id.sb_clear_score);
                }
            });
        }
        if ( undoButton != null ) {
            undoButton.setHidden(bVisible == false);
        }
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
        int nrOfFinishedGames = matchModel.getNrOfFinishedGames();
        if ( bShowTimerFAB && Brand.isGameSetMatch() && (nrOfFinishedGames > 0) && (nrOfFinishedGames % 2 == 0) ) {
            // correction: do not show timer button after even nr of games
            bShowTimerFAB = false;
        }
        if ( (matchModel.hasStarted() == false) && (PreferenceValues.useWarmup(this) == false) ) {
            // do not show if it serves as 'warmup' timer
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
            timerButton = getFloatingActionButton(R.id.float_timer, fMargin, iResImage, ColorPrefs.ColorTarget.timerButtonBackgroundColor, null);
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
                    MyDialogBuilder.dialogWithOkOnly(ScoreBoard.this, sb.toString());
                    return true;
                }
            });
        }

        if ( timerButton != null ) {
            timerButton.setHidden(bVisible == false);
        }
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
            tossButton = getFloatingActionButton(R.id.float_toss, fMargin, iResTossImage, colorKey, null);
        }
        if ( tossButton != null ){
            tossButton.setHidden(bVisible == false);
        }
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
            shareButton = getFloatingActionButton(R.id.float_match_share, fMargin, android.R.drawable.ic_menu_share, ColorPrefs.ColorTarget.shareButtonBackgroundColor, null);
            // ic_menu_share
            //   mdpi = 32x32
            //   ldpi = 36x36
            //   hdpi = 48x48
            //  xhdpi = 64x64 // on android 9 for floating buttons at least this resolution should be available
            // xxhdpi = 96x96
        }
        if ( shareButton == null ) { return; }

        ShareMatchPrefs prefs = PreferenceValues.getShareAction(this);
        boolean bVisible = bMatchHasEnded || (bGameHasEnded && prefs.alsoBeforeMatchEnd());
        shareButton.setHidden(bVisible == false);
        shareButton.setActionId(prefs.getMenuId());
        int iDrawableId = prefs.getDrawableId();

        Integer iBG = mColors.get(ColorPrefs.ColorTarget.shareButtonBackgroundColor);
        if ( iBG != null ) {
            // if we use a light background for the player/plus button... switch to the black icon version
            int blackOrWhiteFor = ColorUtil.getBlackOrWhiteFor(iBG);
            if ( blackOrWhiteFor == Color.BLACK ) {
                iDrawableId = prefs.getDrawableIdBlack();
            }
        }
        shareButton.setDrawable(this.getResources().getDrawable(iDrawableId), getFloatingButtonSizePx());
    }

    // ----------------------------------------------------
    // -----------------changeSide button      ------------
    // ----------------------------------------------------
    private FloatingActionButton changeSideButton = null;
    private void showChangeSideFloatButton(boolean bVisible) {
        if ( PreferenceValues.useChangeSidesFeature(this).equals(Feature.Suggest)==false ) {
            if ( changeSideButton != null ) { changeSideButton.setHidden(true); }
            return;
        }

        if ( changeSideButton == null ) {
            float fMargin = 2.25f;
            int iChangeSidesImage = R.drawable.arrows_left_right;

            ColorPrefs.ColorTarget colorKey = ColorPrefs.ColorTarget.tossButtonBackgroundColor;
            Integer iBG = mColors.get(colorKey);
            if ( iBG != null ) {
                // if we use a light background for the change sides button... switch to the black icon version
                int blackOrWhiteFor = ColorUtil.getBlackOrWhiteFor(iBG);
                if ( blackOrWhiteFor == Color.BLACK ) {
                    iChangeSidesImage = R.drawable.arrows_left_right_black;
                }
            }
            changeSideButton = getFloatingActionButton(R.id.float_changesides, fMargin, iChangeSidesImage, colorKey, null);
        }
        if ( changeSideButton != null ) {
            changeSideButton.setHidden(bVisible == false);
        }
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
        int iDrawableId = R.drawable.circled_plus;

        LockState lockState = matchModel.getLockState();
        if ( lockState.equals(LockState.LockedManualGUI) ) {
            iActionId   = R.id.sb_unlock;
            iDrawableId = android.R.drawable.ic_lock_lock;
        }
        boolean bUsePlayerButtonColor = isLandscape() || isWearable();
        ColorPrefs.ColorTarget colorKey = bUsePlayerButtonColor ? ColorPrefs.ColorTarget.playerButtonBackgroundColor : ColorPrefs.ColorTarget.scoreButtonBackgroundColor;
        Integer iBG = mColors.get(colorKey);
        if ( iBG != null ) {
            // if we use a light background for the player/plus button... switch to the black icon version
            int blackOrWhiteFor = ColorUtil.getBlackOrWhiteFor(iBG);
            if ( blackOrWhiteFor == Color.BLACK ) {
                iDrawableId = R.drawable.circled_plus_black;
            }
        }
        if ( newMatchButton == null ) {
            float fMargin = isPortrait()?0.25f:0.25f; // TODO: simply within score button of player B?
            Direction se = Direction.SE;
            newMatchButton = getFloatingActionButton(iActionId, fMargin, iDrawableId, colorKey, se);
        } else {
            newMatchButton.setActionId(iActionId);
            newMatchButton.setDrawable(this.getResources().getDrawable(iDrawableId), getFloatingButtonSizePx());
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
                //endGame(true); // skip bluetooth because 'changeScore' has not even been send if this method triggered by 'changeScore'
                _endGame(false, false);
                break;
        }
        return bShowDialog;
    }

    // ----------------------------------------------------
    // -----------------float utility          ------------
    // ----------------------------------------------------
    private FloatingActionButton getFloatingActionButton(int iActionId, float fMarginBasedOnButtonSize, int iDrawable, ColorPrefs.ColorTarget colorTarget, Direction direction) {
        if ( m_bNoFloatingButtons ) { return null; }

        if ( direction == null ) {
            direction = isPortrait() ? Direction.E : Direction.S;
            if ( isWearable() ) {
                direction = Direction.S;
            }
        }

        int buttonSizePx = getFloatingButtonSizePx();
        int iMargin = (int) (fMarginBasedOnButtonSize * buttonSizePx); // fMarginBasedOnButtonSize is mainly for nicely lining up buttons in the middle without overlaps
        if ( isWearable() && isScreenRound(this) ) {
            // add a little extra since we use app:boxedEdges="all"
            int iScreenDiagonal   = ViewUtil.getScreenHeightWidthMaximum(this);
            int boxedHeightWidth  = (int) Math.sqrt(Math.pow(iScreenDiagonal, 2) / 2);
            int iAdditionalMargin = (iScreenDiagonal - boxedHeightWidth) / 2;

            // special case: do not add additional margin and have button in free space around the square scoreboard
            switch ( iActionId ) {
                case R.id.float_toss:
                case R.id.float_changesides:
                    direction = Direction.N;
                    iMargin = 0;
                    break;
                case R.id.float_new_match:
                    direction = Direction.E;
                    iMargin = 0;
                    break;
                case R.id.float_undo_last:
                    direction = Direction.S;
                    iMargin = 0;
                    break;
                case R.id.float_timer:
                    direction = Direction.W;
                    iMargin = 0;
                    break;
                default:
                    iMargin += iAdditionalMargin;
                    break;
            }

        }

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

    public int getFloatingButtonSizePx() {
        int buttonSizePx = 0;
        if ( ViewUtil.isPortraitOrientation(this) ) {
            buttonSizePx = ViewUtil.getScreenHeightWidthMaximumFraction(this, R.fraction.pt_gamescores_height);
        } else {
            buttonSizePx = ViewUtil.getScreenHeightWidthMaximumFraction(this, R.fraction.ls_gamescores_width);
        }
        if ( ViewUtil.isWearable(this) ) {
            buttonSizePx = ViewUtil.getScreenHeightWidthMaximumFraction(this, R.fraction.ls_wearable_floating_button_fraction);
            if ( isScreenRound(this) ) {
                int iScreenDiagonal = ViewUtil.getScreenHeightWidthMaximum(this);
                int boxedHeightWidth = (int) Math.sqrt(Math.pow(iScreenDiagonal, 2) / 2);
                float fraction = this.getResources().getFraction(R.fraction.ls_wearable_floating_button_fraction, 1, 1);
                buttonSizePx = (int) (fraction * boxedHeightWidth);
            }
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
        matchModel.registerListener(new OnPowerPlayChangeListener());
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

    private class SpecialScoreChangeListener implements Model.OnSpecialScoreChangeListener, GSMModel.OnSetChangeListener
    {
        @Override public void OnSetBallChange(Player[] players, boolean bHasSetBall) {
            iBoard.updateGameBallMessage("OnSetBallChange", players, bHasSetBall);
        }
        @Override public void OnSetEnded(Player winningPlayer) {
            EnumSet<ChangeSidesWhen_GSM> playersWhen = PreferenceValues.changeSidesWhen_GSM(ScoreBoard.this);
            if ( playersWhen.contains(ChangeSidesWhen_GSM.BetweenSets)
              && playersWhen.contains(ChangeSidesWhen_GSM.AfterEvenGames) == false
              && playersWhen.contains(ChangeSidesWhen_GSM.AfterOddGames ) == false
               ) {
                swapSides_BOP(null);
            }
        }

        @Override public void OnXPointsPlayedInTiebreak(int iTotalPoints) {

            EnumSet<ChangeSidesWhen_GSM> playersWhen = PreferenceValues.changeSidesWhen_GSM(ScoreBoard.this);
            int iCompareTo = playersWhen.contains(ChangeSidesWhen_GSM.AfterFirstPointInTiebreak) ? 1 : 0;
            if ( playersWhen.contains(ChangeSidesWhen_GSM.EveryFourPointsInTiebreak) && (iTotalPoints % 4 == iCompareTo) ) {
                swapSides_BOP(null);
            } else if ( playersWhen.contains(ChangeSidesWhen_GSM.EverySixPointsInTiebreak) && (iTotalPoints % 6 == iCompareTo) ) {
                swapSides_BOP(null);
            } else {
                if ( Brand.isGameSetMatch() ) {
                    showChangeSideFloatButton(false);
                }
            }
        }

        @Override public void OnGameBallChange(Player[] players, boolean bHasGameBall, boolean bForUndo) {
            if ( Brand.isGameSetMatch() ) {
                GSMModel gsmModel= (GSMModel) matchModel;
                int maxScore = gsmModel.getMaxScore();
                if ( bHasGameBall ) {
                    // don't treat GameBall unless it is golden point, as special, wait for SetBall in stead
                    GoldenPointFormat goldenPointFormat = gsmModel.getGoldenPointFormat();
                    if ( goldenPointFormat.onDeuceNumber() >= 0 ) {
                        if ( maxScore == gsmModel.getMinScore() ) {
                            // score is equal
                            // TODO: not yet if goldenPointFormat==OnSecondDeuce and....
                            if ( maxScore >= GSMModel.NUMBER_OF_POINTS_TO_WIN_GAME - 1 + goldenPointFormat.onDeuceNumber() ) {
                                iBoard.updateGameBallMessage("OnGoldenPoint", players, bHasGameBall);
                            }
                        }
                    }
                    return;
                } else {
                    // no more gameball: hence also no more setball: continue
                    boolean bIsStartOfTiebreak = gsmModel.isTieBreakGame() && maxScore == 0;
                    if ( bIsStartOfTiebreak ) {
                        // start of tiebreak
                        // TODO: can be start of tiebreak: show message
                        //iBoard.updateTiebreakMessage("OnTieBreakStart", players, bHasGameBall);
                        showMicrophoneFloatButton(true);
                    } else {
                        iBoard.updateGameBallMessage("OnGameBallChange", players, bHasGameBall);
                        showMicrophoneFloatButton(false); // previous might have been tiebreak
                    }
                }
            } else {
                //iBoard.doGameBallColorSwitch(player, bHasGameBall);
                iBoard.updateGameBallMessage("OnGameBallChange", players, bHasGameBall);

                if ( bHasGameBall ) {
                    showMicrophoneFloatButton(false); // previous might have been tiebreak
                }
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
            if ( m_bHapticFeedbackOnGameEnd ) {
                SystemUtil.doVibrate(ScoreBoard.this, 800);
            }
            updateMicrophoneFloatButton();
            updateTimerFloatButton();
            iBoard.updateGameBallMessage("OnGameEndReached", new Player[] {leadingPlayer}, false);
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
            if ( m_liveScoreShare ) {
                postMatchModel(ScoreBoard.this, matchModel, true, false, null, -1);
            }
        }

        @Override public void OnGameIsHalfwayChange(int iGameZB, int iScoreA, int iScoreB, Halfway hwStatus) {
            if ( matchModel.showChangeSidesMessageInGame(iGameZB) ) {
                if ( hwStatus.isHalfway() && hwStatus.changeSidesFor(matchModel.getSport()) ) {
                    Feature fChangeSidesHW = PreferenceValues.swapSidesHalfwayGame(ScoreBoard.this);
                    boolean bDialogOpened = swapSides_BOP(fChangeSidesHW);
                    if ( bDialogOpened == false ) {
                        iBoard.showMessage(getString(R.string.oa_change_sides), 5);
                    }
                    if ( Brand.isRacketlon()  ) {
                        RacketlonModel rm = (RacketlonModel) matchModel;
                        boolean isSquashDiscipline = rm.getSportForGame(iGameZB + 1).equals(Sport.Squash);
                        if ( isSquashDiscipline && rm.isDoubles() ) {
                            // we use A1B1A1B1 serve sequence: swap players for both teams so that R/L indication is displayed for players actually on court
                            for(Player p: Model.getPlayers() ) {
                                _swapDoublePlayers(p);
                            }
                        }
/*
                        if ( isSquashDiscipline == false ) {
                            swapSides();
                        }
*/
                    }
                } else {
                    iBoard.hideMessage();
                    showChangeSideFloatButton(false);
                }
            }
/*
            if ( hwStatus.isHalfway() && ShareMatchPrefs.LinkWithFullDetailsEachHalf.equals(m_liveScoreShare) ) {
                shareScoreSheet(ScoreBoard.this, matchModel, false);
            }
*/
        }

        @Override public void OnFirstPointOfGame() {
/*
            if ( matchModel.getGameNrInProgress()==1 && ShareMatchPrefs.LinkWithFullDetailsEachHalf.equals(m_liveScoreShare) ) {
                // share if livescoring is 'Semi-On' to let the match appear in the list a.s.a.p.
                shareScoreSheet(ScoreBoard.this, matchModel, false);
            }
*/

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
            if ( Brand.isGameSetMatch() ) {
                EnumSet<ChangeSidesWhen_GSM> playersWhen = PreferenceValues.changeSidesWhen_GSM(ScoreBoard.this);
                if ( playersWhen.contains(ChangeSidesWhen_GSM.AfterFirstPointInTiebreak) == false ) {
                    showChangeSideFloatButton(false);
                }
            } else {
                showChangeSideFloatButton(false);
            }

            iBoard.updateGameBallMessage("OnFirstPointOfGame");
            iBoard.updateBrandLogoBasedOnScore();
            iBoard.updateFieldDivisionBasedOnScore();
        }
    }

    private class ScoreChangeListener implements Model.OnScoreChangeListener
    {
        @Override public void OnNewGameInitialized() {
            if ( matchModel instanceof GSMModel ) {
                autoShowOfficialAnnouncement(AnnouncementTrigger.StartOfGame); // new ball message
            }
        }

        @Override public void OnScoreChange(Player p, int iTotal, int iDelta, Call call) {
            if ( m_bHapticFeedbackPerPoint ) {
                int lDuration = iDelta == 1 ? 200 : 500;
                SystemUtil.doVibrate(ScoreBoard.this, lDuration);
            }
            iBoard.updateScore(p, iTotal);
            iBoard.updateScoreHistory(iDelta == 1);
            if ( bInitializingModelListeners == false ) {
                cancelTimer();
            }
            if ( iDelta != 1 ) {
                updateTimerFloatButton();
                updateTossFloatButton();
                updateMicrophoneFloatButton();
                if ( iDelta == 0 ) {
                    showShareFloatButton(false, matchModel.matchHasEnded());
                } else {
                    showShareFloatButton(false, false);
                }
                iBoard.undoGameBallColorSwitch();
                showAppropriateMenuItemInActionBar();
                iBoard.updateBrandLogoBasedOnScore();
                iBoard.updateFieldDivisionBasedOnScore();
                iBoard.updateGameAndMatchDurationChronos();
                iBoard.updateGameScores();
                iBoard.updateSetScoresToShow(false);
            } else {
                // normal score
                if ( PreferenceValues.recordRallyEndStatsAfterEachScore(ScoreBoard.this).equals(Feature.Automatic)
                 && (call==null || call.equals(Call.NL) ) ) { // if it was a No Let decision it means the opponent scored with a winner
                    showRallyEndStats(p, call);
                }
            }

            if ( (bInitializingModelListeners == false) && (iTotal != 0) && m_liveScoreShare && (matchModel.isLocked() == false) ) {
                //shareScoreSheet(ScoreBoard.this, matchModel, false);
                // start timer to post in e.g. x seconds. Restart this timer as soon as another point is scored
                shareScoreSheetDelayed(600);
            }

            if ( (iDelta == 1) ) {
                // for tabletennis and badminton
                int iEachX = PreferenceValues.autoShowGamePausedDialogAfterXPoints(ScoreBoard.this);
                if ( matchModel.isTowelingDownScore(iEachX, 11) && (matchModel.isPossibleGameVictory() == false)) {
                    Feature showGamePausedDialog = PreferenceValues.showGamePausedDialog(ScoreBoard.this);
                    switch (showGamePausedDialog) {
                        case Automatic: {
                            // show pause dialog
                            _showTimer(Type.TowelingDown, true, null, null);
                            break;
                        }
                        case Suggest: {
                            // show timer floating button
                            showTimerFloatButton(true);
                            break;
                        }
                    }
                } else {
                    showTimerFloatButton(false);
                }
            }
            if ( matchModel.getMaxScore()==0 ) {
                enableScoreButton(p);
            }

            //if ( iDelta == 1 ) {
                speakScore();
            //}
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
    private void showPowerPlayDialog() {
        //if ( Brand.isNotSquash() ) { return; }
        if ( warnModelIsLocked() ) { return; }
        PowerPlayFor powerPlayFor = new PowerPlayFor(this, matchModel, this);
        show(powerPlayFor);
    }

    private class OnPowerPlayChangeListener implements Model.OnPowerPlayChangeListener {
        @Override public void OnPowerPlayChange(Player player, PowerPlayForPlayer powerPlayForPlayer) {
            if ( powerPlayForPlayer.equals(PowerPlayForPlayer.CashedIn)) {
                iBoard.showChoosenDecision(Call.PPW, player, null);
            } else if ( powerPlayForPlayer.equals(PowerPlayForPlayer.Wasted)) {
                iBoard.showChoosenDecision(Call.PPL, player, null);
            } else {
                EnumSet<PowerPlayForPlayer> useOnlyIconFor = EnumSet.of(PowerPlayForPlayer.ActivatedForNextRally, PowerPlayForPlayer.DeActivatedForNextRally);
                if ( useOnlyIconFor.contains(powerPlayForPlayer) == false ) {
                    String msg = "Powerplay : " + matchModel.getName(player) + " : " + StringUtil.capitalize(powerPlayForPlayer);
                    Toast.makeText(ScoreBoard.this, msg, Toast.LENGTH_LONG).show();
                }
                int iVis = powerPlayForPlayer.equals(PowerPlayForPlayer.ActivatedForNextRally) ? View.VISIBLE : View.INVISIBLE ;
                iBoard.setPowerPlayIconVisibility(iVis, player);
            }
        }
    }
    private void updatePowerPlayIcons() {
        if ( matchModel == null ) { return; }
        for(Player p : Player.values()) {
            boolean bNextIsPP = matchModel.nextRallyIsPowerPlayFor(p);
            int iVis = bNextIsPP ? View.VISIBLE : View.INVISIBLE ;
            iBoard.setPowerPlayIconVisibility(iVis, p);

            int iLeft = matchModel.getNrOfPowerPlaysLeftFor(p);
            if ( iLeft > 0 ) {
                // TODO
            }
        }
    }
    private class ComplexChangeListener implements Model.OnComplexChangeListener {
        @Override public void OnChanged() {
            // e.g. an undo back into previous game has been done, or score has been adjusted
            iBoard.updateScoreHistory(false);
            iBoard.updateGameScores();
            for(Player p: Model.getPlayers()) {
                iBoard.updateScore    (p, matchModel.getScore(p));
            }
            Player server = matchModel.getServer();
            iBoard.updateServeSide(matchModel.getServer()  , matchModel.getNextDoubleServe(server), matchModel.getNextServeSide(server), matchModel.isLastPointHandout());
            iBoard.updateReceiver (matchModel.getReceiver(), matchModel.getDoubleReceiver());

            // for restart score and complex undo
            updateTimerFloatButton();
            updateTossFloatButton();
            updateMicrophoneFloatButton();
            showShareFloatButton(matchModel.isPossibleGameVictory(), matchModel.matchHasEnded());
            if ( Brand.isGameSetMatch() ) {
                iBoard.updateSetScoresToShow(true);
                // to prevent 'set ball' being show while it is gameball, if we go back from 0-0 into e.g. 40-30 of previous game
                iBoard.showGameBallMessage(false, null);
            } else {
                iBoard.updateGameBallMessage("ComplexChangeListener.OnChanged");
            }
            iBoard.updateGameAndMatchDurationChronos();

            showAppropriateMenuItemInActionBar();
            updatePowerPlayIcons();
        }
    }
    private class ServeSideChangeListener implements Model.OnServeSideChangeListener {
        @Override public void OnServeSideChange(Player p, DoublesServe doublesServe, ServeSide serveSide, boolean bIsHandout, boolean bForUndo) {
            if ( p == null ) { return; } // normally only e.g. for undo's of 'altered' scores
            iBoard.updateServeSide(p           ,doublesServe   , serveSide, bIsHandout);
            if ( Brand.supportChooseServeOrReceive() == false ) {
                // remove any indication on receiver side
                iBoard.updateReceiver(p.getOther(), DoublesServe.NA);
            }
        }
        @Override public void OnReceiverChange(Player p, DoublesServe doublesServe) {
            iBoard.updateReceiver(p, doublesServe);
        }
    }

    private class GameEndListener implements Model.OnGameEndListener {
        @Override public void OnGameEnded(Player winningPlayer) {
            if ( m_bHapticFeedbackOnGameEnd ) {
                SystemUtil.doVibrate(ScoreBoard.this, 200);
            }
/*
            if ( EnumSet.of(ShareMatchPrefs.LinkWithFullDetailsEachGame, ShareMatchPrefs.LinkWithFullDetailsEachHalf).contains(m_liveScoreShare) ) {
                shareScoreSheet(ScoreBoard.this, matchModel, true);
            }
*/
            showAppropriateMenuItemInActionBar();

            if ( (matchModel.matchHasEnded() == false) ) {
                int iGameNr1B = matchModel.getNrOfFinishedGames();
                boolean bChangeSides = false;
                if ( Brand.isTabletennis() || Brand.isBadminton() ) {
                    bChangeSides = PreferenceValues.swapSidesBetweenGames(ScoreBoard.this);
                } else if ( Brand.isGameSetMatch() ) {
                    EnumSet<ChangeSidesWhen_GSM> playersWhen = PreferenceValues.changeSidesWhen_GSM(ScoreBoard.this);
                    if ( playersWhen.contains(ChangeSidesWhen_GSM.AfterOddGames) ) {
                        if ( (iGameNr1B % 2 == 1) ) {
                            bChangeSides = true;
                        }
                    }
                    if ( playersWhen.contains(ChangeSidesWhen_GSM.AfterEvenGames) ) {
                        if ( (iGameNr1B % 2 == 0) ) {
                            bChangeSides = true;
                        }
                    }
                }

                if ( bChangeSides ) {

                    if ( BTRole.Slave.equals(m_blueToothRole) ) {
                        // swap players only if requested by master
                    } else {
                        Feature fChangeSides = PreferenceValues.useChangeSidesFeature(ScoreBoard.this);
                        swapSides_BOP(fChangeSides);
                        //swapSides(Toast.LENGTH_LONG, null);
                    }
                }
            }

            if ( matchModel.matchHasEnded() == false ) {
                boolean bEndGameDialogWasPresented = PreferenceValues.endGameSuggestion(ScoreBoard.this).equals(Feature.Suggest);
                if ( PreferenceValues.useTimersFeature(ScoreBoard.this).equals(Feature.Automatic) ) {
                    if ( bEndGameDialogWasPresented == false ) {
                        autoShowOfficialAnnouncement(AnnouncementTrigger.EndOfGame);
                    }
                    autoShowGameDetails();
                    autoShowHandicap();
                    autoShowTimer(Type.UntilStartOfNextGame);
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
            if ( Brand.isRacketlon() ) {
                iBoard.updateGameBallMessage("OnGameEnded"); // in rare case in racketlon a 0-0 score may be a matchball in set 3 or 4
            } else {
                iBoard.showGameBallMessage(false, null);
            }

            if ( matchModel.isDoubles() && Brand.supportChooseServeOrReceive() ) {
                if ( matchModel.matchHasEnded() == false ) {
                    DoublesFirstServer firstServer = new DoublesFirstServer(ScoreBoard.this, matchModel, ScoreBoard.this);
                    addToDialogStack(firstServer);

                    if ( Brand.isBadminton() ) {
                        // in badminton receiving team may choose first receiver
                        DoublesFirstReceiver firstReceiver = new DoublesFirstReceiver(ScoreBoard.this, matchModel, ScoreBoard.this);
                        addToDialogStack(firstReceiver);
                    } else if ( Brand.isTabletennis() ) {
                        // in tabletennis receiving player in receiving team depends on serving player in serving team
                    }
                }
            }
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
                showChangeSideFloatButton(matchModel.getLockState().isLocked()==false);
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
            updateNewMatchFloatButton(); // temporary used as 'unlock' button

            if ( PreferenceValues.keepScreenOnWhen(ScoreBoard.this).equals(KeepScreenOnWhen.MatchIsInProgress) ) {
                keepScreenOn(lockStateNew.isLocked() == false);
            }
            EnumSet<LockState> lShowToastFor = EnumSet.of(LockState.LockedManualGUI, LockState.LockedManual);
            if ( lShowToastFor.contains(lockStateNew) || lShowToastFor.contains(lockStateOld) ) {
                int iResId = locked ? R.string.match_is_now_locked : R.string.match_is_now_unlocked;
                iBoard.showToast(getString(iResId), 5, Direction.None);
            }
            if ( lockStateNew.equals(LockState.LockedManualGUI) ) {
                showNewMatchFloatButton(true); // show lock icon
            }
            if ( locked ) {
                iBoard.stopGameDurationChrono();
                if ( Brand.isGameSetMatch() ) {
                    iBoard.stopSetDurationChrono();
                }
                //iBoard.stopMatchDurationChrono();
            } else {
                iBoard.updateGameDurationChrono();
                iBoard.updateSetDurationChrono();
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
                iBoard.updateSetDurationChrono();
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
                    if ( Brand.isGameSetMatch() ) {
                        iBoard.stopSetDurationChrono();
                    }
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
        private       boolean m_bHasBeenPresented = false;

        TTGameDurationTickListener() { }

        /** will be called approximately every second */
        @Override public void onChronometerTick(Chronometer chronometer) {
            if ( m_bHasBeenPresented ) {
                return;
            }
            if ( (matchModel == null) || matchModel.isInMode(TabletennisModel.Mode.Expedite) || matchModel.matchHasEnded() || matchModel.isLocked() ) {
                return;
            }
            long lElapsedMillis = SystemClock.elapsedRealtime() - chronometer.getBase();
            int  lElapsedMin    = Math.round(lElapsedMillis / 1000 / 60);
            if ( lElapsedMin != m_lElapsedMin ) {
                //Log.d(TAG, "Elapsed ms  : " + lElapsedMillis);
                m_lElapsedMin = lElapsedMin;
                int m_ShowDialogAfter = PreferenceValues.showModeDialogAfterXMins(ScoreBoard.this);
                if ( m_ShowDialogAfter < 1 ) {
                    return;
                }
                if ( PreferenceValues.autoShowModeActivationDialog(ScoreBoard.this) == false ) {
                    return;
                }
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
                        showActivateMode(TabletennisModel.Mode.Expedite.toString());
/*
                        if ( isDialogShowing() ) {
                            Toast.makeText(ScoreBoard.this, R.string.activate_mode__Tabletennis, Toast.LENGTH_LONG).show();
                        } else {
                            showActivateMode(TabletennisModel.Mode.Expedite.toString());
                        }
*/
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
        sideTossDialogEnded,
        serverReceiverDialogEnded,
        endGameDialogEnded,
        endMatchDialogEnded,
        //editFormatDialogEnded,
        restartScoreDialogEnded,
        officialAnnouncementClosed,
        injuryTypeClosed,
        playerTimeoutClosed,
        bluetoothDeviceSelectionClosed,
        //rallyEndStatsClosed,
        //specifyHandicapClosed,
        //dialogClosed,
    }

    /** This method is focused at handling gui related events */
    public boolean triggerEvent(SBEvent event, Object ctx) {
        return triggerEvent(event, ctx, -1);
    }
    public boolean triggerEvent(SBEvent event, Object ctx, int iCtx) {
        //Log.d(TAG, "triggerEvent " + event + " (" + ctx + ")");
        switch ( event ) {
            case newMatchStarted:
                lastTimerType = null;
                showAppropriateMenuItemInActionBar();
                dialogManager.clearDialogs();
                autoShowTimer(Type.Warmup);
                autoShowHandicap();
                autoShowTossDialog();
                autoShowTimer(Type.UntilStartOfNextGame);
                autoShowOfficialAnnouncement(AnnouncementTrigger.StartOfGame);
                updatePowerPlayIcons();
                if ( PreferenceValues.keepScreenOnWhen(ScoreBoard.this).equals(KeepScreenOnWhen.MatchIsInProgress) ) {
                    keepScreenOn(true);
                }
                return true;
            case tossDialogEnded:
                showTossFloatButton(false);
                if ( (matchModel != null) && matchModel.hasStarted() == false ) {
                    timestampStartOfGame(GameTiming.ChangedBy.DialogClosed);
                }
                showNextDialog();
                break;
            case serverReceiverDialogEnded:
            case sideTossDialogEnded:
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
                if ( (matchModel != null) && matchModel.gameHasStarted() == false ) {
                    timestampStartOfGame(GameTiming.ChangedBy.TimerStarted);
                }
                if ( (bInitializingModelListeners == false) && m_liveScoreShare ) {
                    Type timerType = (Type) ctx;
                    postMatchModel(ScoreBoard.this, matchModel, true, false, timerType, iCtx);
                }

                return true;
            }
            case timerWarning: {
                Type timerType = (Type) ctx;
                doTimerFeedback(timerType, false);
                if ( m_liveScoreShare ) {
                    postMatchModel(ScoreBoard.this, matchModel, true, false, timerType, iCtx);
                }
                return true;
            }
            case timerEnded:
                Type timerType = (Type) ctx;
                //ViewType viewType  = (ViewType) ctx2;
                doTimerFeedback(timerType, true);
                if ( m_liveScoreShare ) {
                    postMatchModel(ScoreBoard.this, matchModel, true, false, timerType, iCtx);
                }
                if ( EnumSet.of(Type.UntilStartOfNextGame).contains(timerType) && (matchModel != null) && matchModel.isPossibleGameVictory() ) {
                    endGame(false);
                }
                // fall through!!
            case timerCancelled: {
                timerType = (Type) ctx;
                if ( m_liveScoreShare ) {
                    postMatchModel(ScoreBoard.this, matchModel, true, false, timerType, iCtx);
                }
                //viewType  = (ViewType) ctx2;
                if ( EnumSet.of(Type.UntilStartOfNextGame, Type.Warmup).contains(timerType) ) {
                    if ( (matchModel != null) && matchModel.gameHasStarted() == false ) {
                        timestampStartOfGame(GameTiming.ChangedBy.TimerEnded);
                    }
                }
                hidePresentationEndOfGame();
                showAppropriateMenuItemInActionBar();
                if ( matchModel != null ) {
                    showTimerFloatButton(Type.Warmup.equals(timerType) && (matchModel.gameHasStarted() == false));
                    showTossFloatButton (Type.Warmup.equals(timerType) && (matchModel.hasStarted()     == false));
                }
                updateMicrophoneFloatButton();
                showNextDialog();

                if ( Brand.supportsTimeout() && EnumSet.of(Type.TowelingDown, Type.Timeout).contains(timerType) ) {
                    iBoard.resumeGameDurationChrono();
                }
                return false;
            }
            case officialAnnouncementClosed: {
                boolean gameHasStarted = (matchModel != null) && matchModel.gameHasStarted();
                if ( gameHasStarted == false ) {
                    timestampStartOfGame(GameTiming.ChangedBy.DialogClosed);
                    cancelTimer();
                }
                showMicrophoneFloatButton(false);
                showNewMatchFloatButton(false);
                showNextDialog();
                if ( gameHasStarted == false ) {
                    // assume first ball is about to be served
                    showTossFloatButton(false);
                    showTimerFloatButton(false);
                }
                break;
            }
            case playerTimeoutClosed: {
                Player p = (Player) ctx;
                if ( p != null ) {
                    matchModel.recordTimeout(p, true);
                    showTimer(Type.Timeout, false);
                }
                break;
            }
            case injuryTypeClosed: {
                Type injuryType = (Type) ctx;
                if ( injuryType != null ) {
                    showTimer(injuryType, false);
                }
                break;
            }
            case bluetoothDeviceSelectionClosed:
                // communicate FullScreenTimer preference to slave device
                if ( false ) {
                    boolean bShowFS = PreferenceValues.BTSync_showFullScreenTimer(this);
                    writeMethodToBluetoothDelayed(BTMethods.updatePreference, PreferenceKeys.BTSync_showFullScreenTimer, bShowFS);
                }
                // fall through
            case restartScoreDialogEnded:
            case endMatchDialogEnded: // fall through
            case endGameDialogEnded: {
                if ( (matchModel != null) && matchModel.matchHasEnded() ) {
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
    public boolean cancelTimer() {
        if ( timer == null ) { return false; }
        Type type = Timer.timerType;
        timer.cancel();
        timer = null;
        Timer.removeTimerView(false, dialogTimerView);
        dialogTimerView = null;
        this.triggerEvent(SBEvent.timerCancelled, type);

        writeMethodToBluetooth(BTMethods.cancelTimer);
        return true;
    }

    private void doTimerFeedback(Type viewType, boolean bIsEnd) {
        if ( PreferenceValues.useSoundNotificationInTimer    (this) ) {
            SystemUtil.playNotificationSound(this);
        }
        if ( PreferenceValues.useVibrationNotificationInTimer(this) ) {
            SystemUtil.doVibrate(this, 800);
        }
    }
    private Menu       mainMenu                   = null;
    private MenuItem[] menuItemsWithOrWithoutText = null;


    /** Populates the scoreBoard's options menu. Called only once for ScoreBoard (but re-invoked if orientation changes) */
    @Override public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();

        inflater.inflate(R.menu.mainmenu, menu);
      //MenuItem mediaRouteMenuItem = CastButtonFactory.setUpMediaRouteButton(getApplicationContext(), menu, R.id.media_route_menu_item);
        mainMenu = menu;

        initCastMenu();

        if ( m_bHideDrawerItemsFromOldMenu && (id2String.isEmpty() == false) ) {
            for(Integer iId: id2String.keySet() ) {
                boolean b = ViewUtil.hideMenuItemForEver(mainMenu, iId);
                if ( b == false ) {
                    String sCaption = getString(id2String.get(iId));
                    Log.w(TAG, String.format("Failed to hide %s = %s", iId, sCaption));
                }
            }
        }

        updateDemoThread(menu);

        int[] ia_menuIds = {R.id.dyn_undo_last, R.id.dyn_end_game, R.id.dyn_timer, R.id.dyn_match_share };
        menuItemsWithOrWithoutText = new MenuItem[ia_menuIds.length];
        for ( int i=0; i< ia_menuIds.length; i++ ) {
            menuItemsWithOrWithoutText[i] = mainMenu.findItem(ia_menuIds[i]);
        }
        initActionBarSettings(menuItemsWithOrWithoutText);

        showAppropriateMenuItemInActionBar();

        if ( matchModel != null ) {
            setMenuItemEnabled(R.id.sb_swap_double_players, matchModel.isDoubles());
            ViewUtil.toggleMenuItems(mainMenu, R.id.sb_unlock, R.id.sb_lock, matchModel.isLocked());
            if ( matchModel.isUnlockable() == false ) {
                setMenuItemVisibility(R.id.sb_unlock, false);
            }
        }

        String sPostUrl = PreferenceValues.getPostResultToURL(this);
        setMenuItemEnabled(R.id.sb_post_match_result, StringUtil.isNotEmpty(sPostUrl));

        setMenuItemEnabled(R.id.sb_fcm_info, PreferenceValues.isFCMEnabled(this));

        boolean bStoreMatches = PreferenceValues.saveMatchesForLaterUsage(this);
        setMenuItemsEnabled(new int[] { R.id.sb_stored_matches, R.id.cmd_export_matches, R.id.cmd_import_matches }, bStoreMatches);
        //mainMenu.findItem(R.id.sb_overflow_submenu).getSubMenu().findItem(R.id.uc_import_export).getSubMenu().setGroupVisible(R.id.grp_import_export_matches, bStoreMatches);

        // change/overwrite captions
        {
        setMenuItemTitle(menu, R.id.sb_email_match_result, PreferenceValues.ow_captionForEmailMatchResult     (this));
        setMenuItemTitle(menu, R.id.sb_send_match_result , PreferenceValues.ow_captionForMessageMatchResult   (this));
        setMenuItemTitle(menu, R.id.sb_post_match_result , PreferenceValues.ow_captionForPostMatchResultToSite(this));

        setMenuIconToPackage(this, menu, R.id.sb_post_match_result, PreferenceValues.ow_iconForPostMatchResultToSite(this));
        setMenuIconToPackage(this, menu, R.id.sb_email_match_result, "com.google.android.gm");
        }

        if ( ShareHelper.m_menuResIdToPackage != null ) {
            for(Map.Entry<Integer, String> mShare: ShareHelper.m_menuResIdToPackage.entrySet() ) {
                int    iResId   = mShare.getKey();
                String sPackage = mShare.getValue();
                ViewUtil.setPackageIconOrHide(this, menu, iResId, sPackage);
            }
        }
        ViewUtil.setPackageIconOrHide(this, menu, R.id.sb_open_store_on_wearable, "com.android.vending");

        setMenuItemVisibility(R.id.sb_send_match_result, StringUtil.isNotEmpty(PreferenceValues.getDefaultSMSTo(this)));

        setMenuItemVisibility(R.id.sb_open_store_on_wearable, Brand.m_bHasWearable);

        setMenuItemVisibility(R.id.sb_ble_devices, PreferenceValues.useBluetoothLE(this));

        if ( Brand.isNotSquash() ) {
            setMenuItemVisibility(R.id.sb_official_announcement, false);
            setMenuItemVisibility(R.id.sb_possible_conductsA   , false);
            setMenuItemVisibility(R.id.sb_possible_conductsB   , false);
          //setMenuItemVisibility(R.id.sb_swap_double_players  , false);
        }
        if ( Brand.isTabletennis() ) {
            boolean bIsInNormalMode = (matchModel == null) || matchModel.isInNormalMode();
            setMenuItemVisibility(R.id.tt_activate_mode  , bIsInNormalMode == true );
            setMenuItemVisibility(R.id.tt_deactivate_mode, bIsInNormalMode == false);
            if ( PreferenceValues.isBrandTesting(this) ) {
                setMenuItemTitle(menu, R.id.tt_activate_mode  , getString(R.string.activate_mode__Tabletennis));
                setMenuItemTitle(menu, R.id.tt_deactivate_mode, getString(R.string.deactivate_mode__Tabletennis));
            }
        } else {
            setMenuItemVisibility(R.id.tt_activate_mode  , false );
            setMenuItemVisibility(R.id.tt_deactivate_mode, false);
        }

        return true;
    }

    private void setMenuIconToPackage(Context ctx, Menu menu, int menuId, String packageName) {
        if ( StringUtil.isEmpty(packageName) ) { return; }
        MenuItem menuItem = menu.findItem(menuId);
        if ( (menuItem == null) || (menuItem.isVisible() == false) ) { return; }
        try {
            Drawable drawable = ctx.getPackageManager().getApplicationIcon(packageName);
            menuItem.setIcon(drawable);
        } catch (PackageManager.NameNotFoundException var6) {
        }
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
        if ( matchModel == null ) { return; }

        List<Integer> lShowIds = new ArrayList<Integer>(); // the first two in this list will be shown
        List<Integer> lHideIds = new ArrayList<Integer>();
        if ( matchModel.hasStarted() == false ) {
            long lMatchStart = matchModel.getMatchStart();
            int lMinutes = DateUtil.convertToMinutes(System.currentTimeMillis() - lMatchStart);
            lShowIds.add(R.id.dyn_timer);
            if ( lMinutes >= 0 /*40*/ ) {
                lShowIds.add(0, R.id.dyn_new_match);
            }
        } else if ( matchModel.matchHasEnded() ) {
            long lMatchEnd = matchModel.getMatchEnd();
            lShowIds.add((Integer) R.id.dyn_score_details);
            int lMinutes = DateUtil.convertToMinutes(System.currentTimeMillis() - lMatchEnd);
            if ( lMinutes >= 0 /*10*/ ) {
                // old match is still displayed, presume starting a 'new match' is preferred
                lShowIds.add(0, R.id.dyn_new_match);
            }
        } else if ( matchModel.isLocked() ) {
            lShowIds.add(0, R.id.dyn_new_match);
        } else if ( matchModel.gameHasStarted() == false ) {
            if ( PreferenceValues.useTimersFeature(this).equals(Feature.Suggest) ) {
                // timer buttons is already there as floating
                lShowIds.add((Integer) R.id.dyn_score_details);
            } else {
                lShowIds.add((Integer) R.id.dyn_timer);
            }
        } else {
            lHideIds.add((Integer) R.id.dyn_timer);
            lShowIds.add((Integer) R.id.dyn_end_game);
        }
        if ( Brand.isGameSetMatch() ) {
            lShowIds.remove((Integer) R.id.dyn_score_details);
            lShowIds.remove((Integer) R.id.dyn_end_game);
        }
        if ( PreferenceValues.useFeatureYesNo(PreferenceValues.useSpeechFeature(this)) ) {
            // add dyn speak in favor
            lShowIds.add(0, (Integer) R.id.dyn_speak);
            lShowIds.remove((Integer) R.id.dyn_end_game);
        }
        int nrOfPowerPlaysPerMatch = matchModel.getNrOfPowerPlaysPerMatch();
        boolean usePowerPlay = PreferenceValues.usePowerPlay(this);
        if ( usePowerPlay ) {
            if ( matchModel.gameHasStarted() && nrOfPowerPlaysPerMatch > 0 ) {
                if ( matchModel.getNrOfPowerPlaysLeftFor(Player.A) + matchModel.getNrOfPowerPlaysLeftFor(Player.B) > 0 ) {
                    lShowIds.add(0, (Integer) R.id.dyn_power_play);
                }
            }
        }

        // ===================
        // hide and show based on values in different lists
        // ===================
        if ( ListUtil.isNotEmpty(lHideIds) ) {
            for ( Integer iIdHide: lHideIds ) {
                setMenuItemVisibility(iIdHide, false);
            }
        }
        if ( ListUtil.isNotEmpty(lShowIds) ) {
            setMenuItemVisibility(m_idOfVisibleActionBarItem, false);
            if ( lShowIds.contains(R.id.dyn_new_match) ) {
                showNewMatchFloatButton( true );
            } else {
                showNewMatchFloatButton( false );
            }

            // only show dyn menu item for one (else to crowded)
            int iIdShow = lShowIds.remove(0);
            boolean bShowDynNewMatch = iIdShow == R.id.dyn_new_match;
            if ( bShowDynNewMatch ) {
                if ( lShowIds.size() !=0 ) {
                    // we already have the option to start new match with floating button, use another in the action bar
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

        if ( isWearable() ) {
            showUndoFloatButton(bShowUndo);
        }
    }
    private void setMenuItemVisibility(int iId, boolean bVisible) {
        boolean bShowTextInActionBar = PreferenceValues.showTextInActionBar(this);
        ViewUtil.setMenuItemVisibility(mainMenu, iId, bVisible, bShowTextInActionBar);
    }
    private void setMenuItemEnabled(int iId, boolean bEnabled) {
        setMenuItemsEnabled(new int[] { iId }, bEnabled);
    }
    private void setMenuItemsEnabled(int [] iIds, boolean bEnabled) {
        //ViewUtil.setMenuItemsVisibility(mainMenu, iIds, bEnabled);
        ViewUtil.setMenuItemsEnabled(mainMenu, iIds, bEnabled); // the disabled menu items do not appear very disabled visually... so hide them for now
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

    private int m_menuIdBeingHandled = 0;
    public boolean handleMenuItem(int id, Object... ctx) {
        if ( m_menuIdBeingHandled == id ) {
            Log.d(TAG, "Same menu item again: " + id + " " + getResources().getResourceName(id));
            //return false;
        }
        m_menuIdBeingHandled = id;
        try {
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
                if ( isWearable() ) { return false; }
                Intent settingsActivity = new Intent(getBaseContext(), Preferences.class);
                startActivityForResult(settingsActivity, id);
                return true;
            case R.id.dyn_speak:
                Speak.getInstance().playAll(matchModel);
                break;
            case R.id.dyn_end_game:
            case R.id.end_game:
                if ( warnModelIsLocked(id, ctx) ) { return false; }
                if ( matchModel.isPossibleGameVictory() ) {
                    endGame(false);
                } else {
                    areYouSureGameEnding();
                }
                persist(false);
                return true;
            case R.id.end_match:
                // present dialog for reason why: Conduct Match, Retired Because of Injury
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
            case R.id.sb_change_sides:
            case R.id.float_changesides:
                swapSides(Toast.LENGTH_LONG, null);
                if ( Brand.isBadminton() && (matchModel != null) && matchModel.isDoubles() ) {
                    _swapDoublePlayers(Player.values(), false);
                }
                if ( Brand.isTabletennis() && (matchModel != null) && matchModel.isDoubles()
                   && (matchModel.getMaxScore() * 2 + 1 == matchModel.getNrOfPointsToWinGame()) ) {
                    // ensure receiver is swapped
                    _swapDoublePlayers(new Player[] { matchModel.getReceiver() }, true);
                }
                return true;
            case R.id.sb_swap_server:
                // typically only for Tabletennis if initially choosen server was incorrect
                if ( Brand.isTabletennis() ) {
                    TabletennisModel tabletennisModel = (TabletennisModel) matchModel;
                    tabletennisModel.changeInitialServer();

                    //changeScore(Player.A); // NPE if match not yet started
                    handleMenuItem(R.id.pl_change_score, Player.A);
                    undoLast();
                }
                return true;
            case R.id.sb_swap_double_players:
                if ( ctx != null && ctx.length==1 && ctx[0] instanceof Player) {
                    _swapDoublePlayers((Player) ctx[0]);
                } else {
                    swapDoublePlayers();
                }
                return true;
            case R.id.sb_lock:
                lockMatch(ctx);
                return true;
            case R.id.sb_unlock:
                unlockMatch();
                return true;
            case R.id.sb_open_store_on_wearable:
                openPlayStoreOnWearable();
                return true;
            case R.id.sb_bluetooth:
                setupBluetoothControl(true);
                break;
            case R.id.dyn_undo_last:
            case R.id.sb_undo_last:
            case R.id.float_undo_last:
                return undoLast();
            case R.id.sb_undo_last_for_non_scorer:
                Player nonScorer = null;
                if ( ctx != null && ctx.length==1 && ctx[0] instanceof Player) {
                    nonScorer = (Player) ctx[0];
                } else {
                    nonScorer = matchModel.getLastScorer().getOther();
                }
                if ( undoLastForScorer(nonScorer) ) {
                    Toast.makeText(ScoreBoard.this, "Removed last scoreline for " + matchModel.getName(nonScorer), Toast.LENGTH_LONG).show();
                }
                return true;
            case R.id.pl_change_score:
                if ( warnModelIsLocked(id, ctx) ) { return false; }
                _changeScore((Player) ctx[0]);
                break;
            case R.id.pl_show_conduct:
                if ( warnModelIsLocked(true, id, ctx) ) { return false; }
                showConduct((Player) ctx[0]);
                break;
            case R.id.pl_change_name: {
                if ( warnModelIsLocked(true, id, ctx) ) { return false; }
                return showChangeName((Player) ctx[0]);
            }
            case R.id.pl_show_appeal:
                if ( warnModelIsLocked(id, ctx) ) { return false; }
                showAppeal((Player) ctx[0]);
                break;
            case R.id.sb_edit_event_or_player: {
                    Intent nm = new Intent(this, Match.class);
                    nm.putExtra(IntentKeys.EditMatch.toString(), matchModel);
                    startActivityForResult(nm, id);
                }
                return true;
            case R.id.sb_stored_matches: {
                ArchiveTabbed.SelectTab selectTab = ArchiveTabbed.SelectTab.Previous;
                if ( ctx != null && ctx.length==1 && ctx[0] instanceof ArchiveTabbed.SelectTab) {
                    selectTab = (ArchiveTabbed.SelectTab) ctx[0];
                }
                ArchiveTabbed.setDefaultTab(selectTab);
                Intent nm = new Intent(this, ArchiveTabbed.class);
                startActivityForResult(nm, id); // see onActivityResult()
                return true;
            }
            case R.id.sb_select_feed_match: {
                if ( isWearable() ) { return false; }
                MatchTabbed.setDefaultTab(MatchTabbed.SelectTab.Feed);
                Intent nm = new Intent(this, MatchTabbed.class);
                startActivityForResult(nm, id); // see onActivityResult()
                return true;
            }
            case R.id.sb_select_static_match: {
                if ( isWearable() ) { return false; }
                MatchTabbed.setDefaultTab(MatchTabbed.SelectTab.Mine);
                Intent nm = new Intent(this, MatchTabbed.class);
                startActivityForResult(nm, id); // see onActivityResult()
                return true;
            }
            case R.id.sb_enter_singles_match: {
                if ( isWearable() ) { return showNewMatchWizard(); }
                MatchTabbed.setDefaultTab(MatchTabbed.SelectTab.Manual);
                Intent nm = new Intent(this, MatchTabbed.class);
                startActivityForResult(nm, id); // see onActivityResult()
                return true;
            }
            case R.id.sb_enter_doubles_match: {
                if ( isWearable() ) { return showNewMatchWizard(); }
                MatchTabbed.setDefaultTab(MatchTabbed.SelectTab.ManualDbl);
                Intent nm = new Intent(this, MatchTabbed.class);
                startActivityForResult(nm, id); // see onActivityResult()
                return true;
            }
            case R.id.float_new_match:
            case R.id.dyn_new_match: {
                MatchTabbed.SelectTab defaultTab = MatchTabbed.getDefaultTab();
                if ( Brand.isPadel() && ( (defaultTab == null) || MatchTabbed.SelectTab.Manual.equals(defaultTab) ) ) {
                    // usually Padel is NOT singles, go to doubles by default
                    MatchTabbed.setDefaultTab(MatchTabbed.SelectTab.ManualDbl);
                }
                cancelShowCase();
                if ( isWearable() ) {
                    showNewMatchWizard();
                } else {
                    Intent nm = new Intent(this, MatchTabbed.class);
                    startActivityForResult(nm, id); // see onActivityResult
                }

                return true;
            }
            case R.id.sb_put_match_summary_on_clipboard:
                ContentUtil.placeOnClipboard(this, "squore summary", ResultSender.getMatchSummary(this, matchModel));
                return false;
            case R.id.sb_share_matches_summary:
                selectMatchesForSummary();
                return false;
            case R.id.sb_share_match_summary:
                ShareHelper.shareMatchSummary(this, matchModel, null, null);
                return false;
            case R.id.sb_send_match_result:
                ShareHelper.shareMatchSummary(this, matchModel, null, PreferenceValues.getDefaultSMSTo(this));
                return false;
            case R.id.sb_post_match_result:
                postMatchResult();
                return false;
            case R.id.sb_email_match_result:
                ShareHelper.emailMatchResult(this, matchModel);
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
            case R.id.send_settings_to_wearable:
                sendSettingToWearable(ctx);
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
                postMatchModel(this, matchModel, true, true, null, -1);
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
            case R.id.sb_fcm_info:
                openFCMInfoUrl();
                return false;
            case R.id.sb_official_rules:
                showOfficialSquashRules();
                return false;
            case R.id.sb_quick_intro:
                boolean bFromMenu = (ListUtil.length(ctx) == 0);
                startShowCase(bFromMenu);
                return false;
            case R.id.sb_change_log:
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
                if ( warnModelIsLocked(id, ctx) ) { return false; }
                if ( (matchModel != null) && matchModel.hasStarted() ) {
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
                if ( (matchModel != null) && matchModel.hasStarted() ) {
                    lastTimerType = Type.UntilStartOfNextGame;
                    if ( Brand.supportsTimeout() ) {
                        int iEachX = PreferenceValues.autoShowGamePausedDialogAfterXPoints(ScoreBoard.this);
                        if ( matchModel.isTowelingDownScore(iEachX, 11) ) {
                            lastTimerType = Type.TowelingDown;
                        }
                    }
                } else {
                    // toggle between the 2 with Warmup first
                    lastTimerType = Type.Warmup.equals(lastTimerType)?Type.UntilStartOfNextGame:Type.Warmup;
                }
                showTimer(lastTimerType, false);
                return true;
            case R.id.sb_player_timeout_timer:
                _showPlayerTimeoutPlayerChooser();
                break;
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
            case R.id.sb_power_play:
            case R.id.dyn_power_play:
                showPowerPlayDialog();
                return false;
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
            case R.id.sb_purchase_ble:
                String sProductId = getPackageName() + Brand.ENABLE_BLE_PRODUCT_ID;
                purchaseProduct(sProductId);
                return true;
            case R.id.sb_ble_devices:
                selectBleDevices();
                return true;
            case R.id.sb_demo:
                restartScore();
                setModus(null, Mode.FullAutomatedDemo);
                return true;
            case R.id.sb_help:
                showHelp();
                return true;
            case R.id.sb_exit:
                persist(true);
                turnOffBlueToothIfTurnedOnByApp();
                cleanup_Speak();
                System.exit(0);
                return true;
            case android.R.id.home: {
                if ( drawerLayout != null ) {
                    if ( drawerLayout.isDrawerOpen(drawerView) ) {
                        drawerLayout.closeDrawer(drawerView);
                    } else {
                        drawerLayout.openDrawer(drawerView);
                    }
                } else {
                    // so that clicking on icon also allows for selecting new match
                    Intent sm = new Intent(this, MatchTabbed.class);
                    startActivityForResult(sm, id); // see onActivityResult
                }
                return false;
            }
            default:
                String sPackage = ShareHelper.m_menuResIdToPackage.get(id);
                if ( StringUtil.isNotEmpty(sPackage) ) {
                    ShareHelper.shareMatchSummary(this, matchModel, sPackage, null);
                    return false;
                }
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
        } finally {
            m_menuIdBeingHandled = 0;
        }
        return false;
    }

    private boolean showNewMatchWizard() {
        if ( matchModel == null ) { return false; }
        EditMatchWizard editMatchWizard = new EditMatchWizard(this, matchModel, this);
        editMatchWizard.show();
        return true;
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

    private boolean warnModelIsLocked(boolean bIgnoreForConduct, int iBlockingActionId, Object... ctx) {
        if ( (brandedVersionValidation != Evaluation.ValidationResult.OK) && (brandedVersionValidation != Evaluation.ValidationResult.NoDate) ) {
            iBoard.showToast(String.format("Sorry, %s can no longer be used (%s)", Brand.getShortName(this), brandedVersionValidation));
            return true;
        }
        if ( (matchModel == null) || (matchModel.isLocked() == false) ) { return false; }
        if ( bIgnoreForConduct ) {
            if ( matchModel.getLockState().AllowRecordingConduct() ) {
                return false;
            }
        }
        if ( matchModel.getLockState().equals(LockState.LockedManualGUI)) {
            iBoard.showToast(R.string.match_is__locked);
        } else {
            LockedMatch lockedMatch = new LockedMatch(this, matchModel, this);
            if ( matchModel.matchHasEnded() ) {
                // for an ended match allow unlocking by pressing 'score' button, but don't actually change score after unlock
                lockedMatch.init(0, ctx);
            } else {
                lockedMatch.init(iBlockingActionId, ctx);
            }
            show(lockedMatch);
        }
        return true;
    }

    private boolean warnModelIsLocked(int iBlockingActionId, Object... ctx) {
        return warnModelIsLocked(false, iBlockingActionId, ctx);
    }
    private boolean warnModelIsLocked() {
        return warnModelIsLocked(false, 0, (Object[]) null);
    }

    private void showAppeal(Player appealingPlayer) {
        if ( Brand.isNotSquash() ) { return; }
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
        if ( matchModel == null ) { return; }
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
        if ( matchModel == null ) { return; }
        ColorPicker colorPicker = new ColorPicker(this, matchModel, this);
        colorPicker.init(p, matchModel.getColor(p));
        show(colorPicker);
    }

    public static void openInBrowser(Context ctx, String url) {
        try {
            url = URLFeedTask.prefixWithBaseIfRequired(url);
            Uri uri = Util.buildURL(ctx, url,true);
            Intent launchBrowser = new Intent(Intent.ACTION_VIEW, uri);
            ctx.startActivity(launchBrowser);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage()); // e.g. only 'browser' app has been disabled by user
            Toast.makeText(ctx, "Sorry, unable to open browser with \n" + url, Toast.LENGTH_LONG).show();
        }
    }

    private void showOfficialSquashRules() {
        openInBrowser(this, PreferenceValues.getOfficialSquashRulesURL(this));
    }

    private void showLiveScore() {
        String sURL = getString(R.string.pref_live_score_url);
        String sLivescoreId = PreferenceValues.getLiveScoreDeviceId(this);
        openInBrowser(this, sURL);
    }
    private void openFCMInfoUrl() {
        String sFCMDeviceId = PreferenceValues.getFCMDeviceId(this);
        openInBrowser(this, "fcm/" + sFCMDeviceId + "/help");
    }
    private void showHelp() {
        openInBrowser(this, "/help");
    }

    private void autoShare() {
        if ( PreferenceValues.useShareFeature(this).equals(Feature.Automatic) == false ) {
            return;
        }
        ShareMatchPrefs prefs = PreferenceValues.getShareAction(this);
        if ( (prefs != null) && prefs.equals(ShareMatchPrefs.PostResult) && PreferenceValues.autoSuggestToPostResult(this) ) {
            // ensure dialog to 'suggest' posting is NOT also triggered
            PreferenceValues.setOverwrite(PreferenceKeys.autoSuggestToPostResult, false);
        }
        if ( prefs != null ) {
            ChildActivity ca = new ChildActivity(this, null, this);
            ca.init(prefs.getMenuId());
            addToDialogStack(ca);
        }
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
            case UntilStartOfNextGame:
                twoTimerView = new PauseTimerView(this, matchModel);
                break;
            case ContributedInjury:
            case OpponentInflictedInjury:
            case SelfInflictedInjury:
            case SelfInflictedBloodInjury:
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
        if ( (matchModel != null) && ( matchModel.hasStarted() == false ) && m_liveScoreShare && (matchModel.isLocked() == false) ) {
            shareScoreSheetDelayed(1000);
        }
    }
    /** Invoked by TwoTimerView and interpretReceivedMessage() */
    public void _showTimer(Type timerType, boolean bAutoTriggered, ViewType viewType, Integer iInitialSecs) {
        if ( iInitialSecs == null ) {
            iInitialSecs = PreferenceValues.getInteger(timerType.getPrefKey(), this, timerType.getDefaultSecs());
        }
        int iResId       = PreferenceValues.getSportTypeSpecificResId(this, R.integer.timerPauseBetweenGames_reminder_at__Squash);
        int iReminderAt  = getResources().getInteger(iResId);
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
                case UntilStartOfNextGame:
                    if (matchModel.matchHasEnded() /*&& (PreferenceValues.showOfficialAnnouncements(this)==false)*/) {
                        iBoard.showToast(R.string.match_has_finished);
                        return;
                    }
                    timer = new Timer(this, timerType, iInitialSecs, iResumeAt, /*iSeconds/6*/ iReminderAt, bAutoTriggered); // squash: 15, badminton: reminder at 20 seconds
                    break;
                case SelfInflictedInjury:      // fall through
                case SelfInflictedBloodInjury: // fall through
                case ContributedInjury:        // fall through
                case OpponentInflictedInjury:
                    // injury
                    timer = new Timer(this, timerType, iInitialSecs, iResumeAt, iReminderAt /*Math.max(15,iInitialSecs/6)*/, bAutoTriggered);
                    break;
                case Timeout:  // fall through
                case TowelingDown:
                    // toweling
                    timer = new Timer(this, timerType, iInitialSecs, iResumeAt, 0 /*Math.max(15,iInitialSecs/6)*/, bAutoTriggered);
                    if ( iBoard != null ) {
                        iBoard.stopGameDurationChrono();
                    }
                    break;
            }
        }
        if ( viewType == null ) {
            viewType = PreferenceValues.timerViewType(this);
        }
        if ( viewType.equals(ViewType.FullScreen) ) {
            Log.d(TAG, "Showing full screen timer");
            showFullScreenTimer();
        } else {
            if ( fullScreenTimer == null ) {
                Log.d(TAG, "Showing normal timer(s)");
                Timer.addTimerView(iBoard.isPresentation(), iBoard); // always add this one, so it is always up to date if the Popup one is made hidden
                if ( viewType.equals(ViewType.Popup) ) {
                    Timer.addTimerView(false, getDialogTimerView()); // this will actually trigger the dialog to open
                }
            }
        }

        EnumSet<Type> esTimerTypes = EnumSet.of(Type.UntilStartOfNextGame, Type.Timeout);
        if ( (matchModel != null) && "Iddo T".equals(matchModel.getName(Player.A)) ) {
            // TESTING
            if ( isLandscape() && esTimerTypes.contains(timerType) ) {
                boolean bShowBigTimer = PreferenceValues.BTSync_showFullScreenTimer(this);
                if ( bShowBigTimer ) {
                    showFullScreenTimer();
                } else {
                    showPresentationModeOfEndOfGame(); // score, graph and timer
                }
            }
        }

        if ( bIsResume == false ) {
            triggerEvent(SBEvent.timerStarted, timerType, iInitialSecs);
        }
        boolean bShowBigTimer = PreferenceValues.BTSync_showFullScreenTimer(this);
        ViewType vtMirror = bShowBigTimer ? ViewType.FullScreen : viewType;
        writeMethodToBluetooth(BTMethods.startTimer, timerType, bAutoTriggered, vtMirror, iInitialSecs ); // added to ensure Toweling and Injury timeouts show up on connected device (TODO: test)
    }

    private EndOfGameView endOfGameView = null;

    private void showPresentationModeOfEndOfGame() {
        getXActionBar().hide();

        ViewGroup presentationFrame = (ViewGroup) findViewById(R.id.presentation_frame);
        if ( presentationFrame == null ) { return; }
        presentationFrame.removeAllViews();

        endOfGameView = new EndOfGameView(this, iBoard, matchModel);
        endOfGameView.show(presentationFrame);
        presentationFrame.setVisibility(View.VISIBLE);
        findViewById(R.id.content_frame).setVisibility(View.GONE);
    }

    private void hidePresentationEndOfGame() {
        ViewGroup presentationFrame = (ViewGroup) findViewById(R.id.presentation_frame);
        if ( presentationFrame == null ) { return; }
        int presentationFrameVisibility = presentationFrame.getVisibility();

        boolean pEndOfGameViewShowing = (presentationFrameVisibility == View.VISIBLE);
        Log.d(TAG, "[hidePresentationEndOfGame] pEndOfGameViewShowing " + pEndOfGameViewShowing);
        if ( pEndOfGameViewShowing ) {
            presentationFrame.setVisibility(View.GONE);
            findViewById(R.id.content_frame).setVisibility(View.VISIBLE);
            if ( endOfGameView != null ) {
                Timer.removeTimerView(iBoard.isPresentation(), endOfGameView.getTimerView());
                endOfGameView = null;
            }
            if ( fullScreenTimer != null ) {
                Timer.removeTimerView(iBoard.isPresentation(), fullScreenTimer.getTimerView());
                fullScreenTimer = null;
            }
        }
    }

    private FullScreenTimer fullScreenTimer = null;
    private void showFullScreenTimer() {
        getXActionBar().hide();

        ViewGroup presentationFrame = (ViewGroup) findViewById(R.id.presentation_frame);
        if ( presentationFrame == null ) { return; }
        presentationFrame.removeAllViews();

        fullScreenTimer = new FullScreenTimer(this, iBoard, matchModel);
        fullScreenTimer.show(presentationFrame);
        presentationFrame.setVisibility(View.VISIBLE);
        findViewById(R.id.content_frame).setVisibility(View.GONE);
    }

    // ------------------------------------------------------
    // official announcements
    // ------------------------------------------------------

    private void _showPlayerTimeoutPlayerChooser() {
        PlayerTimeout playerTimeout = new PlayerTimeout(this, matchModel, this);
        show(playerTimeout);
    }
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
        if ( matchModel == null ) { return false; }
        ServerToss serverToss = new ServerToss(this, matchModel, this);
        addToDialogStack(serverToss);

        // ensure the match shows up in the list of live score a.s.a.p. so e.g. when toss dialog is started
        if ( ( matchModel.hasStarted() == false ) && m_liveScoreShare && (matchModel.isLocked() == false) ) {
            shareScoreSheetDelayed(1000);
        }

        if ( Brand.supportChooseSide() ) {
            if ( isWearable() == false ) {
                // TODO: dialog has to long texts for wearable
                SideToss sideToss = new SideToss(this, matchModel, this);
                addToDialogStack(sideToss);
            }
        }

        if ( Brand.supportChooseServeOrReceive() ) {
            if ( matchModel.isDoubles() ) {
                DoublesFirstServer firstServer = new DoublesFirstServer(this, matchModel, this);
                addToDialogStack(firstServer);

                DoublesFirstReceiver firstReceiver = new DoublesFirstReceiver(this, matchModel, this);
                addToDialogStack(firstReceiver);
            }
        }

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
        /** @Deprecated */
        Manual,
    }
    private void _showOfficialAnnouncement(AnnouncementTrigger trigger, boolean bManuallyRequested) {
        if ( matchModel == null ) { return; }
        if ( matchModel instanceof GSMModel ) {
            GSMModel gsmModel = (GSMModel) matchModel;
            int newBallsInXgames = gsmModel.newBallsInXgames();
            final int iShowUpFront = PreferenceValues.newBallsXGamesUpFront(this);
            boolean bShowSpeakFAB = (newBallsInXgames >= 0) && (newBallsInXgames <= iShowUpFront);
            if ( bShowSpeakFAB ) {
                _showNewBallsMessage(newBallsInXgames);
            } else {
                boolean bIsStartOfTiebreak = gsmModel.isTieBreakGame() && gsmModel.getMaxScore() == 0;
                if ( bIsStartOfTiebreak ) {
                    _showTiebreakMessage();
                }
            }
        }

        if ( matchModel.gameHasStarted()==false || matchModel.isPossibleGameVictory() ) {
            _showOfficialStartOrEndOfGameAnnouncement(trigger, bManuallyRequested);
        } else if ( matchModel.isStartOfTieBreak() ) {
            _showTieBreakDialog(trigger, matchModel.getTiebreakOccurrence() );
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
        if ( Brand.isGameSetMatch() ) {
            // TODO: not for now. We have to construct message for set-progress as well.
            return;
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
                    startEndAnnouncement = new StartEndAnnouncement(this, matchModel, this); // TODO: also instantiate the proper subclass here and make base class abstract
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
    /** For wearable only */
    private boolean showChangeName(Player p) {
        // TODO:
        return false;
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

    private boolean _showTiebreakMessage() {
        iBoard.showGuidelineMessage_FadeInOut(10, R.string.sb_tiebreak);
        return true;
    }
    private boolean _showNewBallsMessage(int iInXGames) {
        if ( iInXGames < 0 ) {
            return false;
        }
        switch (iInXGames) {
            case 0:
                iBoard.showGuidelineMessage_FadeInOut(10, R.string.oa_new_balls_please);
                break;
            case 1:
                iBoard.showGuidelineMessage_FadeInOut(10, R.string.oa_ballchange_in_1_game);
                break;
            default:
                iBoard.showGuidelineMessage_FadeInOut(10, R.string.oa_ballchange_in_x_games, iInXGames);
                break;
        }
        return true;
    }

    private int m_iChildActivityRequestCode = 0;

    /**
     * {@inheritDoc}
     */
    @Override public void startActivityForResult(Intent intent, int requestCode) {
        m_iChildActivityRequestCode = requestCode;
        super.startActivityForResult(intent, requestCode);
    }

    @Override protected void onActivityResult(int requestCode_MenuId, int resultCode, Intent data) {
        Log.d(TAG, "Returning from activity. requestCode:" + requestCode_MenuId + ", resultCode:" + resultCode + ", " + getResourceEntryName(requestCode_MenuId));

        m_iChildActivityRequestCode = 0;
        super.onActivityResult(requestCode_MenuId, resultCode, data);

        if ( requestCode_MenuId == R.id.sb_ble_devices ) {
            if ( resultCode == RESULT_OK ) {
                Bundle extras = data.getExtras();
                PreferenceValues.setString(PreferenceKeys.BluetoothLE_Peripheral1, this, "");
                PreferenceValues.setString(PreferenceKeys.BluetoothLE_Peripheral2, this, "");
                for(Player p : Player.values() ) {
                    String sAddress = extras.getString(p.toString());
                    PreferenceKeys key = p.equals(Player.A) ? PreferenceKeys.BluetoothLE_Peripheral1 : PreferenceKeys.BluetoothLE_Peripheral2;
                    PreferenceValues.setString(key, this, sAddress);
                }
                m_bBLEDevicesSelected = true;
                if ( PreferenceValues.getLandscapeLayout(this).equals(LandscapeLayoutPreference.Default) ) {
                    if ( PreferenceValues.setEnum(PreferenceKeys.useChangeSidesFeature, this, Feature.Automatic) ) {
                        Toast.makeText(this, R.string.ble_change_sides_automated_in_ble_mode, Toast.LENGTH_LONG).show();
                    }
                }
                ActionBar xActionBar = getXActionBar();
                if ( xActionBar != null && xActionBar.isShowing() && (PreferenceValues.showActionBar(this) == false) ) {
                    // action bar made visible temporary by pressing 'back'
                    xActionBar.hide();
                }

                onResumeInitBluetoothBLE();
            } else {
                m_bBLEDevicesSelected = false;
            }
        }
        if ( requestCode_MenuId == R.id.sb_settings ) {
            // returning from settings
            if ( m_wearableHelper != null ) {
                List<String> lColorSettingChanged = Preferences.getChangedColorSettings();
                if ( ListUtil.isNotEmpty(lColorSettingChanged) ) {
                    if ( PreferenceValues.wearable_syncColorPrefs( this ) ) {
                        handleMenuItem(R.id.send_settings_to_wearable, lColorSettingChanged);
                    }
                }
                List<String> lWearableSettingChanged = Preferences.getWearableSettingsChanged();
                if ( ListUtil.isNotEmpty(lWearableSettingChanged) ) {
                    handleMenuItem(R.id.send_settings_to_wearable, lWearableSettingChanged);
                }
            }
            List<PreferenceKeys> lChangedSetting = Preferences.getChangedSettings();
            if ( ListUtil.isNotEmpty(lChangedSetting) ) {
                for(PreferenceKeys changeKey: lChangedSetting ) {
                    switch (changeKey) {
                        case blinkFeedbackPerPoint:
                        case numberOfBlinksForFeedbackPerPoint:
                            if ( m_timerScoreChangedFeedBack != null ) {
                                m_timerScoreChangedFeedBack.myCancel();
                                m_timerScoreChangedFeedBack = null;
                            }
                            break;
                        case LandscapeLayoutPreference:
                        case showActionBar:            // fall through
                        case showTextInActionBar:      // fall through
                        case OrientationPreference:    // fall through
                        case showFullScreen:           // fall through
                        case prefetchFlags:            // fall through
                        case swapPlayersOn180DegreesRotationOfDeviceInLandscape: // fall through
                            doRestart();
                            break;
                        case BluetoothLE_Config:
                            if ( m_bleReceiverManager != null ) {
                                m_bleReceiverManager.closeConnection();
                                m_bleReceiverManager = null;
                            }
                            onResumeInitBluetoothBLE();
                            break;
                        default:
                            break;
                    }
                }
            }
        }

        // Check which request we're responding to
/*
        if ( requestCode == REQUEST_ENABLE_BT ) {
            // Make sure the request was successful
            Toast.makeText(this, "Bluetooth " + ((resultCode == RESULT_OK)?"Enabled":"Disabled"), Toast.LENGTH_SHORT).show();
        }
*/
        //fbOnActivityResult(requestCode, resultCode, data);
        if ( data == null) { return; }

        if ( StringUtil.isInteger( data.getAction() ) ) {
            // most likely back was pressed after selecting match from list... go back to the list to select a different match
            int iAction = Integer.parseInt(data.getAction());
            handleMenuItem(iAction);
            return;
        }

        Bundle extras = data.getExtras();
        if ( extras != null ) {
            {
                final String sNewMatch  = IntentKeys.NewMatch.toString();
                final String sEditMatch = IntentKeys.EditMatch.toString();
                if ( extras.containsKey(sNewMatch) || extras.containsKey(sEditMatch) ) {
                    final boolean bEditMatchInProgress;
                    // returning from MatchTabbed or Match for new/edit match
                    final Model m;
                    if ( extras.containsKey(sNewMatch) ) {
                        bEditMatchInProgress = false;
                        String sJson = extras.getString(sNewMatch);
                        m = Brand.getModel();
                        m.fromJsonString(sJson);
                        if ( Match.showAfterMatchSelection() ) {
                            // match selected with single click from list of matches
                            String sSource = m.getSource();
                            if ( StringUtil.isNotEmpty(sSource) ) {
                                if ( sSource.startsWith("http") || sSource.contains("tournamentsoftware") ) {
                                    if ( PreferenceValues.turnOnLiveScoringForMatchesFromFeed(this) ) {
                                        // user is allowed to specify to turn this off in a section of MatchView
                                        PreferenceValues.initForLiveScoring(this, true);
                                    } else {
                                        PreferenceValues.initForNoLiveScoring(this);
                                    }
                                }
                            }

                            // now let user specify/change suggested match format
                            Intent nm = new Intent(this, Match.class);
                            nm.putExtra(IntentKeys.NewMatch.toString(), sJson);
                            startActivityForResult(nm, R.id.sb_edit_event_or_player);
                            return;
                        } else {
                            // see if we have to set colors
                            PlayerColorsNewMatch playerColorsNewMatch = PreferenceValues.playerColorsNewMatch(this);
                            for(Player p : Player.values() ) {
                                String sColor = m.getColor(p);
                                if ( StringUtil.isEmpty(sColor) ) {
                                    switch( playerColorsNewMatch ) {
                                        case None:
                                            break;
                                        case TakeFromPreferences:
                                            sColor = PreferenceValues.getString(PreferenceKeys.PlayerColorsNewMatch.toString() + p, sColor, this);
                                            break;
                                        case TakeFromPreviousMatch:
                                            Model mPrev = ScoreBoard.getMatchModel();
                                            if ( mPrev != null ) {
                                                String sColorPrev = mPrev.getColor(p);
                                                if ( StringUtil.isNotEmpty(sColorPrev) ) {
                                                    sColor = sColorPrev;
                                                }
                                            }
                                            break;
                                    }
                                    if ( StringUtil.isNotEmpty(sColor) ) {
                                        m.setPlayerColor(p, sColor);
                                    }
                                }
                            }
                        }
                    } else /*if ( extras.containsKey(sEditMatch) )*/ {
                        bEditMatchInProgress = true;
                        m = (Model) extras.getSerializable(sEditMatch);
                    }

                    int nrOfPointsToWinGame = m.getNrOfPointsToWinGame();
                    if ( nrOfPointsToWinGame != Model.UNDEFINED_VALUE ) {
                        PreferenceValues.setNumber  (PreferenceKeys.numberOfPointsToWinGame, this, nrOfPointsToWinGame);
                    }
                    int nrOfGamesToWinMatch = m.getNrOfGamesToWinMatch();
                    if ( nrOfGamesToWinMatch != Model.UNDEFINED_VALUE ) {
                        PreferenceValues.setNumber(PreferenceKeys.numberOfGamesToWinMatch, this, nrOfGamesToWinMatch);
                    }
                    PreferenceValues.setNumber  (PreferenceKeys.numberOfServesPerPlayer, this, m.getNrOfServesPerPlayer());
                    PreferenceValues.setBoolean (PreferenceKeys.useHandInHandOutScoring, this, m.isEnglishScoring());
                    PreferenceValues.setBoolean (PreferenceKeys.playAllGames           , this, m.playAllGames());
                    PreferenceValues.setEnum    (PreferenceKeys.tieBreakFormat         , this, m.getTiebreakFormat());
                    PreferenceValues.setString  (PreferenceKeys.eventLast              , this, m.getEventName());
                    PreferenceValues.setString  (PreferenceKeys.divisionLast           , this, m.getEventDivision());
                    PreferenceValues.setString  (PreferenceKeys.roundLast              , this, m.getEventRound());
                    PreferenceValues.setString  (PreferenceKeys.courtLast              , this, m.getCourt());
                    PreferenceValues.setString  (PreferenceKeys.locationLast           , this, m.getEventLocation());
                    PreferenceValues.setEnum    (PreferenceKeys.handicapFormat         , this, m.getHandicapFormat());

                    if ( EnumSet.of(PlayerColorsNewMatch.TakeFromPreviousMatch, PlayerColorsNewMatch.TakeFromPreferences).contains(PreferenceValues.playerColorsNewMatch(this)) ) {
                        // if colors are specified and no 'pref colors' yet, take these values as pref colors
                        for ( Player p: Player.values() ) {
                            String sPrefKey = PreferenceKeys.PlayerColorsNewMatch.toString() + p;
                            String sPrefColor = PreferenceValues.getString(sPrefKey, null, this);
                            if ( StringUtil.isEmpty(sPrefColor) ) {
                                String sMatchColor = m.getColor(p);
                                if ( StringUtil.isNotEmpty(sMatchColor) ) {
                                    PreferenceValues.setString(sPrefKey, this, sMatchColor);
                                }
                            }
                        }
                    }
                    if ( Brand.isSquash() && m.isDoubles() ) {
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
                    if ( Brand.isGameSetMatch() ) {
                        GSMModel gsmModel = (GSMModel) m;
                        PreferenceValues.setEnum   (PreferenceKeys.finalSetFinish           , this, gsmModel.getFinalSetFinish());
                        PreferenceValues.setEnum   (PreferenceKeys.goldenPointFormat        , this, gsmModel.getGoldenPointFormat());
                        PreferenceValues.setBoolean(PreferenceKeys.StartTiebreakOneGameEarly, this, gsmModel.getStartTiebreakOneGameEarly());
                        PreferenceValues.setEnum   (PreferenceKeys.newBalls                 , this, gsmModel.getNewBalls());
                    }
                    if ( MapUtil.isNotEmpty(RWValues.getOverwritten() ) ) {
                        Log.w(TAG, "remaining overwrites " + RWValues.getOverwritten());
                        //PreferenceValues.removeOverwrites(FeedMatchSelector.mFeedPrefOverwrites);
                    }

                    if ( bEditMatchInProgress ) {
                        boolean bChanged = false;

                        // copy relevant properties to active matchmodel, but do not change score
                        setPlayerNames( m.getPlayerNames(false, false));
                        for ( Player p: Player.values() ) {
                            bChanged = matchModel.setPlayerColor  (p, m.getColor  (p));
                            bChanged = matchModel.setPlayerCountry(p, m.getCountry(p));
                            bChanged = matchModel.setPlayerClub   (p, m.getClub   (p));

                        }
                        // TODO: announcement language
                        bChanged = matchModel.setEvent(m.getEventName(), m.getEventDivision(), m.getEventRound(), m.getEventLocation());
                        bChanged = matchModel.setCourt(m.getCourt());
                        bChanged = matchModel.setReferees(m.getReferee(), m.getMarker(), m.getAssessor());

                        bChanged = matchModel.setNrOfPointsToWinGame(m.getNrOfPointsToWinGame());
                        bChanged = matchModel.setNrOfGamesToWinMatch(m.getNrOfGamesToWinMatch());
                        bChanged = matchModel.setTiebreakFormat(m.getTiebreakFormat());
                        if ( Brand.isGameSetMatch() ) {
                            GSMModel gsmModel = (GSMModel) m;
                            // only allow if final set is not yet started
                            GSMModel gsmInProgress = (GSMModel) matchModel;
                            if ( gsmInProgress.isFinalSet() == false ) {
                                gsmInProgress.setFinalSetFinish(gsmModel.getFinalSetFinish());
                            }
                        }
                    } else {
                        m.registerListeners(matchModel);
                        setMatchModel(m);
                        setPlayerNames(new String[] { matchModel.getName(Player.A), matchModel.getName(Player.B) });

                        initScoreBoard(null);
                    }

                    //setModelForCast(matchModel);

                    sendMatchToOtherBluetoothDevice(true, 500);
                }
            }
            {
                final String sOldMatch = IntentKeys.PreviousMatch.toString();
                if ( extras.containsKey( sOldMatch ) ) {
                    File f = (File) extras.get(sOldMatch);
                    if ( f == null ) { return; }

                    setMatchModel(null);
                    initScoreBoard(f);

                    // ensure selected match is also LAST_MATCH file
                    FileUtil.copyFile(f, PersistHelper.getLastMatchFile(this));

                    sendMatchToOtherBluetoothDevice(true, 1000);
                }
            }
        }
    }
    /** only public because called from EditPlayers: TODO remove editplayers dialog */
    public boolean setPlayerNames(String[] saPlayers) {
        return setPlayerNames(saPlayers, true);
    }
    private boolean setPlayerNames(String[] saPlayers, boolean bAddToPrefList) {
        if ( matchModel == null ) { return false; }
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

    private void selectMatchesForSummary() {
        handleMenuItem(R.id.sb_stored_matches, ArchiveTabbed.SelectTab.PreviousMultiSelect);
    }

    /** mainly to post actually finished matches to configured target */
    private void postMatchResult() {
        final String feedPostName = PreferenceValues.getFeedPostName(this);
        final Authentication authentication = PreferenceValues.getFeedPostAuthentication(this);
        if ( authentication != null ) {
            switch (authentication) {
                case BodyParameters:
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

    /** mainly to post actually finished matches to configured target */
    public void postMatchResult(Authentication authentication, String sUserName, String sPassword) {
        ResultPoster resultPoster = initResultPoster();
        if ( resultPoster != null ) {
            showProgress(R.string.posting);
            resultPoster.post(this, matchModel, authentication, sUserName, sPassword);
        } else {
            showShareFloatButton(false, false);
        }
    }
    /** for livescore */
    private class DelayedModelPoster extends CountDownTimer {
        private DelayedModelPoster(long iMilliSeconds) {
            super(iMilliSeconds, 250);
        }
        @Override public void onTick(long millisUntilFinished) {
            //Log.d(TAG, "Waiting ... " + millisUntilFinished);
        }
        @Override public void onFinish() {
            Log.d(TAG, "Posting ... ");
            postMatchModel(ScoreBoard.this, matchModel, false, false, null , -1);
        }
    }
    private DelayedModelPoster m_delayedModelPoster = null;
    /** To share to 'Live' score on a regular basis without user interaction */
    private synchronized void shareScoreSheetDelayed(int iMilliSeconds) {
        if ( m_delayedModelPoster != null ) {
            m_delayedModelPoster.cancel();
        }
        m_delayedModelPoster = new DelayedModelPoster(iMilliSeconds);
        m_delayedModelPoster.start();
    }
    private static boolean m_bShareStarted_DemoThread = false;
    /** invoked on: GameEndReached, GameEnded, FirstPointChange, GameIsHalfway, ScoreChange. Mainly for livescore */
    public static void postMatchModel(Context context, Model matchModel, boolean bAllowEndGameIfApplicable, boolean bFromMenu, Type timerType, int iSecsTotal) {
        if ( matchModel == null) { return; }
        if ( timerType != null ) { matchModel.setShareURL(null); } // ensure repost for livescore, even though score has not changed
        Player possibleMatchVictoryFor = matchModel.isPossibleMatchVictoryFor();
        if ( bAllowEndGameIfApplicable ) {
            if ( possibleMatchVictoryFor != null ) {
                matchModel.endGame(true, true); // DO notify listeners (OLD: don't notify listeners: might popup a disturbing dialog like 'Post to site?')
            }
        }
        JSONObject oSettings = null;
        m_bShareStarted_DemoThread = true;
        if ( possibleMatchVictoryFor != null ) {
            // only create settings if match is finished... ensure in between updates are just a little smaller/faster
            oSettings = new JSONObject();

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
        }

        JSONObject oTimerInfo = null;
        if ( (timerType != null) && (iSecsTotal > -1) ) {
            Log.w(TAG, "Posting match with timer info");
            try {
                oTimerInfo = new JSONObject();
                oTimerInfo.put("type"        , timerType.toString());
                oTimerInfo.put("totalSeconds", iSecsTotal);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            Log.w(TAG, "Posting match without timer info");
        }

        MatchModelPoster matchModelPoster = new MatchModelPoster();
        matchModelPoster.post(context, matchModel, oSettings, oTimerInfo, bFromMenu);
    }

    // ------------------------------------------------------
    // dialog boxes
    // ------------------------------------------------------

    DialogManager dialogManager = null;
    private synchronized void showNextDialog() {
        dialogManager.showNextDialog();
    }
    public boolean isDialogShowing() {
        return dialogManager.isDialogShowing();
    }
    public int childActivityRequestCode() {
        return m_iChildActivityRequestCode;
    }
    private synchronized boolean addToDialogStack(BaseAlertDialog dialog) {
/*
        if ( BTRole.Slave.equals(m_blueToothRole) ) {
            Log.w(TAG, "Not adding dialog to stack for BT Slave : " + dialog.getClass().getName());
        }
*/
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

    /** If you start referee-ing a match that was already in progress */
    private void adjustScore() {
        if ( matchModel == null ) { return; }
        AdjustScore adjustScore = new AdjustScore(this, matchModel, this);
        show(adjustScore);
    }

    private void autoShowHandicap() {
        if ( matchModel.isUsingHandicap() == false ) { return; }
        if ( matchModel.matchHasEnded()            ) { return; }
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

        clearBLEWaitForConfirmation();

        initScoreBoard(null);
        matchModel.setDirty();

        writeMethodToBluetooth(BTMethods.restartScore);
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

    private void confirmPostMatchResult() {
        if ( PreferenceValues.autoSuggestToPostResult(ScoreBoard.this) == false ) {
            return;
        }
        if ( StringUtil.isEmpty(PreferenceValues.getPostResultToURL(this)) ) {
            return;
        }

        if ( PreferenceValues.useShareFeature(this).equals(Feature.Automatic) ) {
            ShareMatchPrefs prefs = PreferenceValues.getShareAction(this);
            if ( ShareMatchPrefs.PostResult.equals(prefs) ) {
                // match will be automatically posted, do not show the 'suggest' dialog
                return;
            }
        }
        int     iShowPostToSiteDialogCnt = PreferenceValues.getRunCount(this, PreferenceKeys.autoSuggestToPostResult);
        boolean bIsOverwritten           = PreferenceValues.getOverwritten(PreferenceKeys.autoSuggestToPostResult)!=null;
        boolean bShowWithNoMoreCheckBox  = PreferenceValues.autoSuggestToPostResult(this) && (iShowPostToSiteDialogCnt < 5) && (bIsOverwritten == false);

        PostMatchResult postMatchResult = new PostMatchResult(this, matchModel, this);
        postMatchResult.init(bShowWithNoMoreCheckBox);
        addToDialogStack(postMatchResult);
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
        PreferenceValues.setNumberR  (PreferenceKeys.numberOfGamesToWinMatch          , this, R.integer.numberOfGamesToWin_default__Squash);
        PreferenceValues.setNumberR  (PreferenceKeys.numberOfPointsToWinGame          , this, R.integer.gameEndScore_default__Squash);
        PreferenceValues.setEnum     (PreferenceKeys.tieBreakFormat                   , this, TieBreakFormat.TwoClearPoints);
        PreferenceValues.setEnum     (PreferenceKeys.recordRallyEndStatsAfterEachScore, this, Feature.DoNotUse);
        PreferenceValues.setEnum     (PreferenceKeys.useTimersFeature                 , this, Feature.Suggest);
        PreferenceValues.setEnum     (PreferenceKeys.useOfficialAnnouncementsFeature  , this, Feature.Suggest);
        PreferenceValues.setEnum     (PreferenceKeys.endGameSuggestion                , this, Feature.Suggest);
        PreferenceValues.setBoolean  (PreferenceKeys.useHandInHandOutScoring          , this, false);
      //PreferenceValues.setBoolean  (PreferenceKeys.showScoringHistoryInMainScreen   , this, true);
        PreferenceValues.setStringSet(PreferenceKeys.showScoringHistoryInMainScreenOn , ShowOnScreen.OnDevice, this);
        PreferenceValues.setStringSet(PreferenceKeys.showMatchDurationChronoOn        , ShowOnScreen.OnChromeCast, this);
        PreferenceValues.setStringSet(PreferenceKeys.showLastGameDurationChronoOn     , ShowOnScreen.OnChromeCast, this);

        PreferenceValues.setBoolean  (PreferenceKeys.autoSuggestToPostResult, this, false);
        PreferenceValues.setBoolean  (PreferenceKeys.showTips, this, false);
        if ( PreferenceValues.showFullScreen(this) == false || PreferenceValues.getOrientationPreference(this).contains(OrientationPreference.Landscape) == false ) {
            PreferenceValues.setBoolean(PreferenceKeys.showFullScreen, this, true);
            PreferenceValues.setString(PreferenceKeys.OrientationPreference, this, OrientationPreference.Landscape.toString());
            //System.exit(0);
        }
        PreferenceValues.setEnum(PreferenceKeys.MatchTabbed_defaultTab, this, MatchTabbed.SelectTab.Manual);

        PreferenceValues.addOrReplaceNewFeedURL(this, "Invalid", "http://invalid.url.for.matches", "http://invalid.url.for.players", "http://invalid.url.for.posting.results", "http://invalid.url.for.livescore", false, false);
    }

    // ------------------------------------------------------
    // demo modus
    // - TODO: no rotate during demo modus? or ensure new activity is passed on to demo thread
    // ------------------------------------------------------
    public enum Mode {
        Normal,
        FullAutomatedDemo,
        ScreenShots,
        GuidedDemo,
        Promo,
        Debug,
    }
    private Mode toggleDemoMode(DemoThread.DemoMessage demoMessage) {
        Mode modeNew = Mode.values()[(m_mode.ordinal()+1)%Mode.values().length];

        setModus(demoMessage, modeNew);
        return modeNew;
    }

    public void setModus(DemoThread.DemoMessage demoMessage, Mode mode) {
        ScoreBoard.m_mode = mode;

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
            handleMenuItem(R.id.sb_clear_score);
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
        if ( PreferenceValues.isRunningInMainCodeBase(this) ) {
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
                PersistHelper.storeAsPrevious(this, sJsonDemo, null, true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addDemoMatchesForSport(Map<Integer, SportType> lResources, SportType sport) {
        switch (sport) {
            case Squash:
                lResources.put(R.raw.demomatch1_squash, sport);
                lResources.put(R.raw.demomatch2_squash, sport);
                lResources.put(R.raw.demomatch3_squash, sport);
                break;
            case Racketlon:
                lResources.put(R.raw.demomatch1_racketlon, sport);
                break;
            case Tabletennis:
                lResources.put(R.raw.demomatch1_tabletennis, sport);
                break;
            case Padel:
            case TennisPadel:
                lResources.put(R.raw.demomatch1_tennispadel, sport);
                lResources.put(R.raw.demomatch2_tennispadel, sport);
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

    /** Called from ScoreBoard and several Child activities from ScoreBoard */
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
                Intent fbIntent = pm.getLaunchIntentForPackage(ShareHelper.m_menuResIdToPackage.get(R.id.sb_facebook_match_summary));
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

    @Override public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // onResume gets called after this to handle the intent
        Log.w(TAG, "onNewIntent.. intent = " + intent);
        setIntent(intent);
        //onNFCNewIntent(intent);
        onURLNewIntent(intent);
    }

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
                    String sFeedURL = sURL.replaceAll("(?:https?)://(?:www\\.|esf\\.|vsf\\.|dsqv\\.|squashvlaanderen\\.|fir\\.|rfa\\.|squashcanada\\.|wsf\\.|squashse\\.)?(?:toernooi|turnier|competitions|tournamentsoftware|europeansquash|squash)(?:\\.\\w{2,3}).*id=([A-Za-z0-9-]+).*?$"
                                                     , Brand.getBaseURL() + "/tournamentsoftware/$1");
                    Log.i(TAG, String.format("Feed URL : %s", sFeedURL));
                    PreferenceValues.addOrReplaceNewFeedURL(this, "tournamentsoftware url", sFeedURL + "/matches"
                                                                                          , sFeedURL + "/players", null, null, true, true);
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
        @Override public void receive(String sContent, FetchResult fetchResult, long lCacheAge, String sPreviousContent, String sUrl) {
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
                    MyDialogBuilder.dialogWithOkOnly(context, "something went wrong with fetching " + sURL + "\n" + fetchResult);
                }
            } catch (Exception e) {
                e.printStackTrace();
                MyDialogBuilder.dialogWithOkOnly(context, "something went wrong with loaded content " + StringUtil.size(sContent));
            }
        }
    }

    private void onURLNewIntent(Intent intent) {
        m_bURLReceived = false;
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
        if ( bUseRateMeMaybe && (ViewUtil.isWearable(this) == false) ) {
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
    private BroadcastReceiver m_downloadCompleted = new BroadcastReceiver() {
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

            List<String> lMessages = new ArrayList<String>();

            // clean previous file before copying over it
            File dir = PreferenceValues.targetDirForImportExport(context, false);
            final File fMyFile = new File(dir, String.format("%s.downloaded.zip", ztDownLoadShortname));
            if ( fMyFile.exists() ) {
                fMyFile.delete();
            }

            String sError = null;
            if ( Build.VERSION.SDK_INT > Build.VERSION_CODES.M /* 23 */ ) {
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
                String localFilePath = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME)); // deprecated in android 7
                lMessages.add("Download complete: " + localFilePath + " (download id:" + downloadId + ")");

                File f = new File(localFilePath);
                Log.i(TAG, "Handling " + localFilePath);

                sError = FileUtil.copyFile(f, fMyFile);
            }

            if ( StringUtil.isEmpty(sError) ) {
                handleMenuItem(R.id.cmd_import_matches);
            } else {
                String sMsg = String.format("Copy of file to import/export location %s failed (%s)", dir.getAbsolutePath(), sError);
                lMessages.add(sMsg);
                Log.w(TAG, sMsg);
                MyDialogBuilder.dialogWithOkOnly(context, ztDownLoadShortname.toString(), ListUtil.join(lMessages, "\n\n"), true);
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

            registerReceiver(m_downloadCompleted, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
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

    //===================================================================
    // Draw focus to GUI elements
    //===================================================================
    private PlayerFocusEffectCountDownTimerFeedback m_timerScoreChangedFeedBack = null;
    private abstract class PlayerFocusEffectCountDownTimer extends CountDownTimer {
        Player            m_player                  = null;
        int               m_iInvocationCnt          = 0;
        ShowScoreChangeOn m_guiElementToUseForFocus = ShowScoreChangeOn.PlayerButton;
        FocusEffect       m_focusEffect             = null;
        int               m_iTmpTxtOnElementDuringFeedback = 0;

        PlayerFocusEffectCountDownTimer(FocusEffect focusEffect, int iTotalDuration, int iInvocationInterval) {
            super(iTotalDuration, iInvocationInterval);
            this.m_focusEffect = focusEffect;
        }
        abstract void doOnTick(int m_iInvocationCnt, long millisUntilFinished);
        abstract void doOnFinish();
        @Override public void onTick(long millisUntilFinished) {
            m_iInvocationCnt++;
            iBoard.guiElementColorSwitch(m_guiElementToUseForFocus, m_player, m_focusEffect, m_iInvocationCnt, m_iTmpTxtOnElementDuringFeedback);
            doOnTick(m_iInvocationCnt, millisUntilFinished);
        }

        @Override public void onFinish() {
            doOnFinish();
            cancelForPlayer();
        }
        private void cancelForPlayer() {
            m_iInvocationCnt = 0;
            if ( m_player == null ) { return; }
            iBoard.guiElementColorSwitch(m_guiElementToUseForFocus, m_player, m_focusEffect, m_iInvocationCnt, 0);
            doChangeScoreIfRequired(m_player);
        }
        public void myCancel() {
            //doChangeScoreIfRequired();
            cancelForPlayer();
            super.cancel(); // final in parent ... can not be overwritten, hence 'myCancel()'
        }
        private void doChangeScoreIfRequired(Player p) {
            if ( m_iTmpTxtOnElementDuringFeedback != 0 ) {
                if ( m_iTmpTxtOnElementDuringFeedback != R.string.uc_undo ) {
                    matchModel.changeScore(p);
                } else {
                    matchModel.undoLast();
                }
                m_iTmpTxtOnElementDuringFeedback = 0;
            }
        }
    }
    private static final int I_CONFIRM_COUNTDOWN_INTERVAL = 50;
    private class PlayerFocusEffectCountDownTimerConfirm extends PlayerFocusEffectCountDownTimer {
        private Player m_pNotifyAfterXSecs = null;
        private int    m_iNotifyAfterXSecs = 3;

        PlayerFocusEffectCountDownTimerConfirm() {
            super(FocusEffect.SetTransparency, 60 * 1000, I_CONFIRM_COUNTDOWN_INTERVAL);
        }
        public void start(ShowScoreChangeOn guiElementToUseForFocus, Player p, Player pNotifyAfterXSecs, int iNotifyAfterXSecs) {
            m_iInvocationCnt                 = 0;
            m_guiElementToUseForFocus        = guiElementToUseForFocus;
            m_player                         = p;
            m_pNotifyAfterXSecs              = pNotifyAfterXSecs;
            m_iNotifyAfterXSecs              = iNotifyAfterXSecs;
            super.start();
        }
        @Override void doOnTick(int iInvocationCnt, long millisUntilFinished) {
            if ( iInvocationCnt == m_iNotifyAfterXSecs * (1000 / I_CONFIRM_COUNTDOWN_INTERVAL) && m_pNotifyAfterXSecs != null ) {
                // after 3 seconds let wristband of player to confirm vibrate
                if ( notifyBLE(m_pNotifyAfterXSecs, BLEUtil.Keys.PokeConfig) ) {
                    String sAppend = getBLEMessage(R.string.ble_signalled_x_to_confirm, m_pNotifyAfterXSecs);
                    iBoard.appendToInfoMessage(sAppend, true);
                }
            }
        }

        @Override void doOnFinish() {
            clearBLEWaitForConfirmation();
        }
    }

    private class PlayerFocusEffectCountDownTimerFeedback extends PlayerFocusEffectCountDownTimer {
        PlayerFocusEffectCountDownTimerFeedback(int iNrOfBlinks) {
            super(FocusEffect.BlinkByInverting, 2 * iNrOfBlinks * 400, 400);
        }
        public void start(ShowScoreChangeOn guiElementToUseForFocus, Player p, int iTmpTxtOnElementDuringFeedback) {
            m_iInvocationCnt                 = 0;
            m_guiElementToUseForFocus        = guiElementToUseForFocus;
            m_player                         = p;
            m_iTmpTxtOnElementDuringFeedback = iTmpTxtOnElementDuringFeedback;
            Log.i(TAG, "m_iTmpTxtOnElementDuringFeedback : " + m_iTmpTxtOnElementDuringFeedback);
            super.start();
        }
        @Override void doOnTick(int m_iInvocationCnt, long millisUntilFinished) {
            /* nothing specific for now */
        }

        @Override void doOnFinish() {
            /* nothing specific for now */
        }
    }

    private boolean clearBLEConfirmationStatus() {
        if ( m_blePlayerWaitingForScoreToBeConfirmed != null || m_blePlayerToConfirmOwnScore != null ) {
            stopWaitingForBLEConfirmation();
            m_blePlayerWaitingForScoreToBeConfirmed = null;
            m_blePlayerToConfirmOwnScore = null;
            showInfoMessage(R.string.ble_score_cancelled_via_gui, 5);
            return true;
        }
        return false;
    }

    public void stopWaitingForBLEConfirmation() {
        for(Player p: Player.values() ) {
            waitForBLEConfirmation(p, null, false);
        }
    }
    public void startWaitingForBLEConfirmation(Player pScorer, Player pToConfirm) {
        waitForBLEConfirmation(pScorer, pToConfirm, true);
    }
    private final PlayerFocusEffectCountDownTimerConfirm m_timerBLEConfirm = new PlayerFocusEffectCountDownTimerConfirm();
    private void waitForBLEConfirmation(Player pScorer, Player pToConfirm, boolean bWaiting) {
        ShowScoreChangeOn guiElementToUseForFocus = ShowScoreChangeOn.ScoreButton; // TODO: from options
        if ( bWaiting ) {
            int iNrOfSecs = PreferenceValues.nrOfSecondsBeforeNotifyingBLEDeviceThatConfirmationIsRequired(this);
            m_timerBLEConfirm.start(guiElementToUseForFocus, pScorer, pToConfirm, iNrOfSecs);
        } else {
            iBoard.guiElementColorSwitch(guiElementToUseForFocus, pScorer, FocusEffect.BlinkByInverting, 0, 0);
            iBoard.guiElementColorSwitch(guiElementToUseForFocus, pScorer, FocusEffect.SetTransparency, 0, 0);
            m_timerBLEConfirm.myCancel();
        }
    }
    private void startVisualFeedbackForScoreChange(Player player, int iTmpTxtOnElementDuringFeedback) {
        ShowScoreChangeOn guiElementToUseForFocus = getShowScoreChangeOn(iTmpTxtOnElementDuringFeedback);
        if ( iTmpTxtOnElementDuringFeedback == 0 || (guiElementToUseForFocus.equals(ShowScoreChangeOn.ScoreButton) == false) ) {
            matchModel.changeScore(player);
            iTmpTxtOnElementDuringFeedback = 0;
        } else {
            // score will be changed by timer ending the visual feedback. No longer used... idea was to show 'game' or 'set' for a few seconds when a point ended the game/set
        }
        if ( m_timerScoreChangedFeedBack == null ) {
            int numberOfBlinksForFeedbackPerPoint = PreferenceValues.numberOfBlinksForFeedbackPerPoint(this);
            m_timerScoreChangedFeedBack = new PlayerFocusEffectCountDownTimerFeedback(numberOfBlinksForFeedbackPerPoint);
        }
        m_timerScoreChangedFeedBack.myCancel();
        m_timerScoreChangedFeedBack.start(guiElementToUseForFocus, player, iTmpTxtOnElementDuringFeedback);
    }

    private ShowScoreChangeOn getShowScoreChangeOn(int iTmpTxtOnElementDuringFeedback) {
        ShowScoreChangeOn showScoreChangeOn = ShowScoreChangeOn.ScoreButton;
        switch (iTmpTxtOnElementDuringFeedback) {
            case R.string.oa_game:
                showScoreChangeOn = ShowScoreChangeOn.GamesButton;
                break;
            case R.string.oa_set:
                showScoreChangeOn = ShowScoreChangeOn.SetsButton;
                break;
            case R.string.oa_match:
                if ( Brand.isGameSetMatch() ) {
                    showScoreChangeOn = ShowScoreChangeOn.SetsButton;
                } else {
                    showScoreChangeOn = ShowScoreChangeOn.GamesButton;
                }
                break;
        }
        return showScoreChangeOn;
    }
    @Nullable private int getTxtOnElementDuringFeedback(Player player) {
        int iResIdGameSetOrMatch = 0;
        if ( matchModel.isPossibleGameBallFor(player) ) {
            iResIdGameSetOrMatch = R.string.oa_game;
            if ( matchModel instanceof GSMModel ) {
                GSMModel gsmModel = (GSMModel) matchModel;
                Player[] possibleSetVictoryFor = gsmModel.isPossibleSetVictoryFor();
                if (possibleSetVictoryFor!=null && possibleSetVictoryFor.length!=0) {
                    iResIdGameSetOrMatch = R.string.oa_set;
                }
            }
            Player[] possibleMatchBallFor = matchModel.isPossibleMatchBallFor();
            if (possibleMatchBallFor!=null && possibleMatchBallFor.length!=0) {
                iResIdGameSetOrMatch = R.string.oa_match;
            }
        }
        return iResIdGameSetOrMatch;
    }

    // -----------------------------------------------------
    // --------------------- bluetooth BLE -----------------
    // -----------------------------------------------------
    private void promoteAppToUseBLE() {
        PreferenceValues.setBoolean(PreferenceKeys.UseBluetoothLE                 , ScoreBoard.this, true);
        PreferenceValues.setBoolean(PreferenceKeys.blinkFeedbackPerPoint          , ScoreBoard.this, true);
        PreferenceValues.setBoolean(PreferenceKeys.showActionBar                  , ScoreBoard.this, false);
        PreferenceValues.setEnum   (PreferenceKeys.useTimersFeature               , ScoreBoard.this, Feature.Automatic);
        PreferenceValues.setEnum   (PreferenceKeys.useSpeechFeature               , ScoreBoard.this, Feature.Automatic);
        PreferenceValues.setEnum   (PreferenceKeys.useOfficialAnnouncementsFeature, ScoreBoard.this, Feature.DoNotUse);
        PreferenceValues.setEnum   (PreferenceKeys.endGameSuggestion              , ScoreBoard.this, Feature.Automatic);
        PreferenceValues.setEnum   (PreferenceKeys.useChangeSidesFeature          , ScoreBoard.this, Feature.DoNotUse);
        PreferenceValues.setEnum   (PreferenceKeys.LandscapeLayoutPreference      , ScoreBoard.this, LandscapeLayoutPreference.Presentation1);
        if ( PreferenceValues.currentDateIsTestDate() ) {
            PreferenceValues.setEnum   (PreferenceKeys.StartupAction        , ScoreBoard.this, StartupAction.BLEDevices);
        }
        Toast.makeText(ScoreBoard.this, String.format("D-SCORE BLE option enabled. Use menu option: %s", getString(R.string.pref_BluetoothLE_Devices)), Toast.LENGTH_LONG).show();
        //RWValues.Permission permission = PreferenceValues.doesUserHavePermissionToAccessFineLocation(this, true);
    }

    private void selectBleDevices() {
        if ( mBluetoothAdapter == null ) {
            Toast.makeText(this, R.string.bt_no_bluetooth_on_device, Toast.LENGTH_SHORT).show();
            return;
        }
        if ( PreferenceValues.useBluetoothLE(this) == false ) { return; }

        m_bBLEDevicesSelected = false;
        persist(false);

        //String[] permissions = BLEUtil.getPermissions();
        //ActivityCompat.requestPermissions(this, permissions, PreferenceKeys.UseBluetoothLE.ordinal());

        if ( m_bleReceiverManager != null ) {
            // ensure currently connected devices are disconnected and there for will start broadcasting again to show up in BLEActivity
            m_bleReceiverManager.closeConnection();
            m_bleReceiverManager = null;
        }

        Intent bleActivity = new Intent(this, BLEActivity.class);
        startActivityForResult(bleActivity, R.id.sb_ble_devices);
    }
    private void showBLEVerifyConnectedDevicesDialog(int iNrOfDevices) {
        VerifyConnectedDevices verify = new VerifyConnectedDevices(this, matchModel, this);
        verify.init(iNrOfDevices, m_bleReceiverManager);
        dialogManager.show(verify);
    }
    /** invoked by the BLEHandler */
    public void updateBLEConnectionStatus(int visibility, int nrOfDevicesConnected, String sMsg, int iDurationSecs) {
        if ( iBoard == null ) {
            return;
        }

        if ( 0 <= nrOfDevicesConnected && nrOfDevicesConnected <=2 ) {
            iBoard.updateBLEConnectionStatusIcon(visibility, nrOfDevicesConnected);
            m_nrOfBLEDevicesConnected = nrOfDevicesConnected;
        } else {
            // nrOfDevicesConnected = actually the battery level
        }
        if ( visibility == View.INVISIBLE ) {
            iBoard.showInfoMessage(null, -1);
        } else {
            if ( StringUtil.isNotEmpty(sMsg) ) {
                iBoard.showInfoMessage(sMsg, iDurationSecs);
            }
        }
        TextView vTxt = findViewById(R.id.sb_bluetoothble_nrofconnected);
        if ( vTxt != null && (vTxt.hasOnClickListeners() == false) ) {
            vTxt.setOnClickListener(v -> {
                showBLEDevicesBatteryLevels();
            } );
            vTxt.setOnLongClickListener(v -> {
                showBLEVerifyConnectedDevicesDialog(m_nrOfBLEDevicesConnected);
                return true;
            });
        }
    }

    private Player m_bleRequestBatteryLevelOf = Player.A;
    private void showBLEDevicesBatteryLevels() {
        // actual level will be returned async via an INFO message
        if ( m_nrOfBLEDevicesConnected == 2 ) {
            m_bleReceiverManager.readBatteryLevel(m_bleRequestBatteryLevelOf);
            m_bleRequestBatteryLevelOf = m_bleRequestBatteryLevelOf.getOther(); // for next call
        } else {
            m_bleReceiverManager.readBatteryLevel(null);
        }
    }

    public boolean notifyBLE(Player p, BLEUtil.Keys configKey) {
        if ( m_bleReceiverManager == null ) { return false; }
        return m_bleReceiverManager.writeToBLE(p, configKey, null);
    }
    private boolean clearBLEWaitForConfirmation() {
        if ( m_blePlayerWaitingForScoreToBeConfirmed != null || m_blePlayerToConfirmOwnScore != null ) {
            m_blePlayerWaitingForScoreToBeConfirmed = null;
            m_blePlayerToConfirmOwnScore            = null;
            stopWaitingForBLEConfirmation();
            String sUIMsg = getResources().getQuantityString(R.plurals.ble_ready_for_scoring_with_devices, m_nrOfBLEDevicesConnected);
            updateBLEConnectionStatus(View.VISIBLE, m_nrOfBLEDevicesConnected, sUIMsg, 10);
            return true;
        }
        return false;
    }
    private String getBLEMessage(int iResId, Object... formatArgs) {
        Object[] newFormatArgs = new Object[formatArgs.length];
        for(int i=0; i<formatArgs.length;i++) {
            Object arg = formatArgs[i];
            if ( arg instanceof Player ) {
                newFormatArgs[i] = matchModel.getName((Player) arg);
            } else if ( arg instanceof BLEDeviceButton ) {
                newFormatArgs[i] = getBLEButtonDescription((BLEDeviceButton) arg);
            } else {
                newFormatArgs[i] = arg;
            }
        }
        return getString(iResId, newFormatArgs);
    }
    private String getBLEButtonDescription(BLEDeviceButton b) {
        if ( b == null ) { return "" ; }
        String s = m_bleConfig.optString(b.toString(), b.toString());
        if ( s.startsWith("R.string.") ) {
            // TODO: if in form of R.string.xxx in bluetooth_le_config.json
            String sName = s.replace("R.string.", "");
            int stringId = this.getResources().getIdentifier(sName, "string", getPackageName());
            if ( stringId != 0 ) {
                s = getString(stringId);
            } else {
                s = StringUtil.capitalize(sName);
            }
        }

        return s;
    }

    private        JSONObject         m_bleConfig                             = null;
    private        boolean            m_bSingleDevice_ConfirmWithSameButton   = false;
    private        BLEDeviceButton    m_eInitiateSelfScoreChangeButton        = BLEDeviceButton.PRIMARY_BUTTON;
    private        BLEDeviceButton    m_eInitiateOpponentScoredChangeButton   = null;
    private        BLEDeviceButton    m_eConfirmScoreBySelfButton             = null;
    private        BLEDeviceButton    m_eConfirmScoreByOpponentButton         = m_eInitiateSelfScoreChangeButton.getOther();
    private        BLEDeviceButton    m_eCancelScoreByOpponentButton          = null;
    private        BLEDeviceButton    m_eCancelScoreByInitiatorButton         = m_eInitiateSelfScoreChangeButton.getOther();
    private        BLEReceiverManager m_bleReceiverManager                    = null;
    private static boolean            m_bBLEDevicesSelected                   = false;
    private static int                m_nrOfBLEDevicesConnected               = 0;
    private static Player             m_blePlayerWaitingForScoreToBeConfirmed = null;
    private static Player             m_blePlayerToConfirmOwnScore            = null;
    public void onResumeInitBluetoothBLE()
    {
        final String sMethod = "SB.onCreateInitBLE";
        updateBLEConnectionStatus(View.INVISIBLE, 0, null, -1);

        boolean bInitBLE = PreferenceValues.useBluetoothLE(this);
        if ( bInitBLE == false ) {
            Log.i(sMethod, "Don't use BLE. Period");
            return;
        }
        if ( m_bBLEDevicesSelected == false ) {
            Log.i(sMethod, "Don't use BLE. First select devices");
            return;
        }

        if ( m_bleReceiverManager == null ) {
            String sBluetoothLEDevice1 = PreferenceValues.getString(PreferenceKeys.BluetoothLE_Peripheral1, null, this);
            String sBluetoothLEDevice2 = PreferenceValues.getString(PreferenceKeys.BluetoothLE_Peripheral2, null, this);
            if ( StringUtil.hasEmpty(sBluetoothLEDevice1, sBluetoothLEDevice2) ) {
                Log.w(sMethod, "Don't use BLE. No 2 devices specified");
                return;
            }
            Log.i(sMethod, String.format("Scanning for devices %s, %s", sBluetoothLEDevice1, sBluetoothLEDevice2));

            m_bleConfig = BLEUtil.getActiveConfig(this);
            if ( m_bleConfig == null ) {
                Toast.makeText(this, "Could not obtain config for BLE", Toast.LENGTH_LONG).show();
                return;
            }
            if ( (sBluetoothLEDevice1 != null) && (sBluetoothLEDevice1.equalsIgnoreCase(sBluetoothLEDevice2) == false) ) {
                // settings only relevant when using 2 wristbands with confirmation mechanism
                m_eInitiateSelfScoreChangeButton      = BLEUtil.getButtonFor(m_bleConfig, BLEUtil.Keys.InitiateScoreChangeButton        , m_eInitiateSelfScoreChangeButton);
                m_eInitiateOpponentScoredChangeButton = BLEUtil.getButtonFor(m_bleConfig, BLEUtil.Keys.InitiateOpponentScoreChangeButton, m_eInitiateOpponentScoredChangeButton);
                m_eConfirmScoreByOpponentButton       = BLEUtil.getButtonFor(m_bleConfig, BLEUtil.Keys.ConfirmScoreByOpponentButton     , m_eConfirmScoreByOpponentButton);
                m_eCancelScoreByOpponentButton        = BLEUtil.getButtonFor(m_bleConfig, BLEUtil.Keys.CancelScoreByOpponentButton      , m_eCancelScoreByOpponentButton);
                m_eConfirmScoreBySelfButton           = BLEUtil.getButtonFor(m_bleConfig, BLEUtil.Keys.ConfirmScoreBySelfButton         , m_eConfirmScoreBySelfButton);
                m_eCancelScoreByInitiatorButton       = BLEUtil.getButtonFor(m_bleConfig, BLEUtil.Keys.CancelScoreByInitiatorButton     , m_eCancelScoreByInitiatorButton);
            }
            if ( (sBluetoothLEDevice1 != null) && (sBluetoothLEDevice1.equalsIgnoreCase(sBluetoothLEDevice2)) ) {
                // settings only relevant when using 1 wristbands with 2 buttons and confirmation mechanism
                m_bSingleDevice_ConfirmWithSameButton = m_bleConfig.optBoolean(BLEUtil.Keys.SingleDevice_ConfirmWithSameButton.toString(), m_bSingleDevice_ConfirmWithSameButton);
            }

            m_bleReceiverManager = new BLEReceiverManager(this, mBluetoothAdapter, sBluetoothLEDevice1, sBluetoothLEDevice2, m_bleConfig);
        }
        if ( m_bleReceiverManager != null ) {
            BLEHandler bleHandler = new BLEHandler(this);
            m_bleReceiverManager.setHandler(bleHandler);
        }
        m_bleReceiverManager.startReceiving();
    }
    // ----------------------------------------------------
    // --------------------- bluetooth --------------------
    // ----------------------------------------------------

    // Get the default adapter
    private static final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    private void onCreateInitBluetooth() {
        //mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if ( mBluetoothAdapter != null ) {
            ViewUtil.setMenuItemsVisibility(mainMenu, new int[] { R.id.sb_bluetooth }, mBluetoothAdapter.isEnabled());
            if ( mBluetoothAdapter.isEnabled() ) {
                if ( mBluetoothControlService == null ) {
                    mBluetoothControlService = new BluetoothControlService(Brand.getUUID(), Brand.getShortName(this));
                }
                mBluetoothControlService.setHandler(mBluetoothHandler);
                if ( mBluetoothControlService.getState().equals(BTState.CONNECTED) ) {
                    setBluetoothIconVisibility(View.VISIBLE);
                    Set<BluetoothDevice> bondedDevices = mBluetoothAdapter.getBondedDevices();
                    List<BluetoothDevice> lPairedDevicesFilteredOnNWService = BluetoothControlService.filtered_Network(bondedDevices);
                    if ( ListUtil.size(lPairedDevicesFilteredOnNWService) == 1 ) {
                        mBluetoothHandler.storeBTDeviceConnectedTo(bondedDevices.iterator().next());
                    }
                } else {
                    setBluetoothIconVisibility(View.INVISIBLE);
                }
            } else {
                Log.d(TAG, "Bluetooth not turned on");
                setBluetoothIconVisibility(View.INVISIBLE);
                // request to turn on bluetooth: to 'intrusive'
                //startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), BluetoothAdapter.ACTION_REQUEST_ENABLE.hashCode()); // requires android.permission.BLUETOOTH_CONNECT
            }
        } else {
            // no bluetooth: hide menu option
            ViewUtil.hideMenuItemForEver(mainMenu, R.id.sb_bluetooth);
            setBluetoothIconVisibility(View.INVISIBLE);
        }
    }

    /** Listens to state changes in Bluetooth setting of device ON/OFF */
    private final BroadcastReceiver mBTStateChangeReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        if ( mBluetoothControlService != null ) {
                            mBluetoothControlService = null;
                        }
                        break;
                    case BluetoothAdapter.STATE_ON:
                        mBluetoothControlService = new BluetoothControlService(Brand.getUUID(), Brand.getShortName(ScoreBoard.this));
                        mBluetoothControlService.setHandler(mBluetoothHandler);
                        mBluetoothControlService.breakConnectionAndListenForNew();
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        break;
                }
            }
        }
    };

    private void onResumeBlueTooth() {
        if ( mBluetoothAdapter == null ) {
            return; // no bluetooth on device
        }
        PreferenceValues.Permission hasPermission = PreferenceValues.doesUserHavePermissionToBluetoothConnect(this, false);
        if ( PreferenceValues.Permission.Granted.equals(hasPermission) == false ) {
            return;
        }

        if ( mBluetoothControlService != null ) {
            if ( mBluetoothControlService.getState().equals(BTState.NONE) ) {
                // requires android.permission.BLUETOOTH_CONNECT
                mBluetoothControlService.breakConnectionAndListenForNew();
            }
        }
        registerReceiver(mBTStateChangeReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
    }
    public void stopBlueTooth() {
        if ( mBluetoothAdapter == null ) {
            return; // no bluetooth on device
        }
        if ( mBluetoothControlService != null ) {
            //mBluetoothControlService.stop();
        }
        if ( m_bleReceiverManager != null ) {
            m_bleReceiverManager.closeConnection();
            m_bleReceiverManager = null;
        }
        try {
            unregisterReceiver(mBTStateChangeReceiver);
        } catch (Exception e) { //have seen java.lang.IllegalArgumentException: Receiver not registered:
            Log.w(TAG, e.getMessage());
            //e.printStackTrace();
        }
    }
    //----------------------------------------------------
    // WRAPPER methods to be able to intercept method call to model and communicate it to connected BT device
    //----------------------------------------------------
    private void _changeScore(Player player) {
        //Log.d(TAG, "Changing score for for model " + matchModel);
        if ( PreferenceValues.blinkFeedbackPerPoint(this) ) {
            int iTmpTxtOnElementDuringFeedback = getTxtOnElementDuringFeedback(player);
            startVisualFeedbackForScoreChange(player, iTmpTxtOnElementDuringFeedback);
        } else {
            matchModel.changeScore(player);
        }
        writeMethodToBluetooth(BTMethods.changeScore, player);
    }

    public void setPlayerColor(Player player, String sColor) {
        matchModel.setPlayerColor( player, sColor);

        writeMethodToBluetooth(BTMethods.changeColor, player, sColor);
    }

    public void changeSide(Player player) {
        matchModel.changeSide(player);

        writeMethodToBluetooth(BTMethods.changeSide, player);
    }

    private void lockMatch(Object... ctx) {
        LockState lockState = LockState.LockedManual;
        if ( ListUtil.length(ctx) != 0 ) {
            lockState = (LockState) ctx[0];
        }
        if ( matchModel != null ) {
            matchModel.setLockState(lockState);

            writeMethodToBluetooth(BTMethods.lock);
        }
    }
    private void unlockMatch() {
        if ( matchModel == null ) { return; }
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

        writeMethodToBluetooth(BTMethods.unlock);
    }

    private boolean undoLast() {
        if ( warnModelIsLocked() ) { return false; }
        if ( matchModel == null ) { return false; }
        enableScoreButton(matchModel.getServer());
        matchModel.undoLast();
        bGameEndingHasBeenCancelledThisGame = false;

        writeMethodToBluetooth(BTMethods.undoLast);
        return true;
    }
    private boolean undoLastForScorer(Player nonScorer) {
        boolean bOK = matchModel.undoLastForScorer(nonScorer);
        if ( bOK ) {
            writeMethodToBluetooth(BTMethods.undoLastForScorer, nonScorer);
        }
        return bOK;
    }
    public boolean endGame(boolean bFromDialog) {
        return _endGame(bFromDialog, false);
    }
    private boolean _endGame(boolean bFromDialog, boolean bBTDelayed) {
        if ( (bFromDialog == false) && warnModelIsLocked() ) { return false; }
        if ( matchModel == null ) { return false; }

        // first send endgame (because 'startNewGame' may stop a possible timer to be started)
        if ( BTRole.Master.equals(m_blueToothRole) ) {
            if ( bBTDelayed ) {
                writeMethodToBluetoothDelayed(BTMethods.endGame);
            } else {
                writeMethodToBluetooth(BTMethods.endGame);
            }
        }

        matchModel.endGame();

        return true;
    }

    /** Invoked from Appeal dialog */
    public void recordAppealAndCall(Player appealingPlayer, Call call) {
        if ( warnModelIsLocked() ) { return; }
        matchModel.recordAppealAndCall(appealingPlayer, call);

        writeMethodToBluetooth(BTMethods.recordAppealAndCall, appealingPlayer, call);
    }
    /** Invoked from Conduct dialog */
    public void recordConduct(Player pMisbehaving, Call call, ConductType conductType) {
        if ( warnModelIsLocked() ) { return; }
        matchModel.recordConduct(pMisbehaving, call, conductType);

        writeMethodToBluetooth(BTMethods.recordConduct, pMisbehaving, call, conductType);
    }
    public void timestampStartOfGame(GameTiming.ChangedBy changedBy) {
        if ( matchModel == null ) { return; }
        matchModel.timestampStartOfGame(changedBy);

        writeMethodToBluetooth(BTMethods.timestampStartOfGame, changedBy);
    }
    /**
     * Called from
     * - dialog where 'amount of time' left can be changed
     * - 'toweling down' timer is actually started for TT version of app
     **/
    public void restartTimerWithSecondsLeft(int iSecs) {
        DialogTimerView.restartTimerWithSecondsLeft(iSecs);
        if ( m_liveScoreShare ) {
            postMatchModel(ScoreBoard.this, matchModel, true, false, Timer.timerType, iSecs);
        }

        writeMethodToBluetooth(BTMethods.restartTimerWithSecondsLeft, iSecs);
    }

    private void toggleGameScoreView() {
        iBoard.toggleGameScoreView();
        castGamesWonAppearance();

        writeMethodToBluetooth(BTMethods.toggleGameScoreView);
    }

    private void toggleSetScoreView() {
        iBoard.updateSetScoresToShow(true);

        writeMethodToBluetooth(BTMethods.toggleGameScoreView);
    }

    public void showInfoMessage(String sMsg, int iMessageDurationSecs) {
        iBoard.showInfoMessage(sMsg, iMessageDurationSecs);
    }
    public void showInfoMessage(int iResId, int iMessageDurationSecs) {
        iBoard.showInfoMessage(getString(iResId), iMessageDurationSecs);
    }

    private boolean m_bRequestPullPushDone = false;
    public synchronized void pullOrPushMatchOverBluetoothWearable(String sDeviceName) {
        if ( m_bRequestPullPushDone ) {
            return;
        }
        m_bRequestPullPushDone = true;
        pullOrPushMatchOverBluetooth(sDeviceName);
    }
    public synchronized void pullOrPushMatchOverBluetooth(String sDeviceName) {
        AlertDialog.Builder cfpop = new MyDialogBuilder(this);
        if ( isWearable() == false ) {
            cfpop.setTitle(R.string.bt_pull_or_push)
                 .setIcon(android.R.drawable.stat_sys_data_bluetooth);
        }
        cfpop   .setMessage(getString(R.string.bt_pull_in_match_from_device_x_or_push_to, sDeviceName ))
                .setPositiveButton(R.string.bt_pull, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        writeMethodToBluetooth(BTMethods.requestCompleteJsonOfMatch);
                    }
                })
                .setNegativeButton(R.string.bt_push, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        sendMatchToOtherBluetoothDevice( false, 200);
                    }
                })
                .setNeutralButton(R.string.cmd_cancel, null)
                .show();
    }

    /**
     * The Handler that gets information back from the BluetoothControlService
     */
    private final BluetoothHandler mBluetoothHandler = new BluetoothHandler(this);

    public void setBluetoothIconVisibility(int visibility) {
        if ( iBoard == null ) {
            return;
        }

        iBoard.setBluetoothIconVisibility(visibility);
    }
    private static Map<PreferenceKeys, String> mBtPrefSlaveSettings = new HashMap<>();
    static {
        mBtPrefSlaveSettings.put(PreferenceKeys.useOfficialAnnouncementsFeature, String.valueOf(Feature.DoNotUse ));
        mBtPrefSlaveSettings.put(PreferenceKeys.useShareFeature                , String.valueOf(Feature.DoNotUse ));
        mBtPrefSlaveSettings.put(PreferenceKeys.useTimersFeature               , String.valueOf(Feature.DoNotUse ));
        mBtPrefSlaveSettings.put(PreferenceKeys.endGameSuggestion              , String.valueOf(Feature.DoNotUse ));
        mBtPrefSlaveSettings.put(PreferenceKeys.useSoundNotificationInTimer    , String.valueOf(false            ));
        mBtPrefSlaveSettings.put(PreferenceKeys.useVibrationNotificationInTimer, String.valueOf(false            ));
        mBtPrefSlaveSettings.put(PreferenceKeys.timerViewType                  , String.valueOf(ViewType.Inline  ));
    }
    public  static BTRole                  m_blueToothRole            = BTRole.Equal;
    public void setBluetoothRole(BTRole role, Object oReason) {
        switch (role) {
            case Slave:
                // disable automatic showing of dialogs
                PreferenceValues.setOverwrites(mBtPrefSlaveSettings);
                break;
            default:
                PreferenceValues.removeOverwrites(mBtPrefSlaveSettings);
                break;
        }
        if ( role.equals(m_blueToothRole) == false ) {
            String sMsg = String.format("Bluetooth connection. Device switched from %s to %s (%s)", m_blueToothRole, role, oReason);
            //Toast.makeText(this, sMsg, Toast.LENGTH_SHORT).show();
            Log.v(TAG, sMsg);
            m_blueToothRole = role;
        }
    }

    private static CountDownTimer m_cdtSendMatchToOther = null;
    public void sendMatchToOtherBluetoothDevice(boolean bOnlyAsMaster, int iDelayMs) {
        if ( mBluetoothControlService != null) {
            if  ( mBluetoothControlService.getState().equals(BTState.CONNECTED)
             && ( (bOnlyAsMaster == false) || BTRole.Master.equals(m_blueToothRole) )
                ) {

                if ( m_cdtSendMatchToOther!= null) {
                    m_cdtSendMatchToOther.cancel();
                }
                m_cdtSendMatchToOther = new CountDownTimer(iDelayMs, 500) {
                    @Override public void onTick(long millisUntilFinished) { }
                    @Override public void onFinish() {
                        if ( matchModel != null ) {
                            String sJson = matchModel.toJsonString(ScoreBoard.this);
                            mBluetoothControlService.write(sJson.length() + ":" + sJson);
                        }
                    }
                };
                m_cdtSendMatchToOther.start();
            } else {
              //Log.d(TAG, "Bluetooth: Not sending complete match");
            }
        }

        if ( matchModel != null ) {
            String sJson = matchModel.toJsonString(this);
            sendMatchFromToWearable(sJson);
        }
    }

    public void sendFlagToOtherBluetoothDevice(Context ctx, String sCountryCode) {
        if ( mBluetoothControlService == null) {
            return;
        }

        if  ( mBluetoothControlService.getState().equals(BTState.CONNECTED) ) {
            File fCache   = PreferenceValues.getFlagCacheName(sCountryCode, ctx);
            if ( fCache.exists() ) {
                byte[] baFileContent = FileUtil.readFileToByteArray(fCache);
                final String sBase64 = Base64.encodeToString(baFileContent, 0);
                if ( true ) {
                    String sMessage = BTFileType.CountryFlag + ":" + sCountryCode + ":" + sBase64;
                    mBluetoothControlService.write( sMessage.length() + ":" + sMessage );
                } else {
                    String sMessage = BTFileType.CountryFlag + ":" + sCountryCode + ":" + sBase64.replaceAll("[\\s\\n\\t]", "");
                    mBluetoothControlService.write( sMessage.length() + ":" + sMessage );
                }
                //Toast.makeText(ctx, String.format("Requested file %s send to paired device as requested", fCache), Toast.LENGTH_LONG).show();
            } else {
                //Toast.makeText(ctx, String.format("Requested file %s by paired device is not available", fCache), Toast.LENGTH_LONG).show();
                writeMethodToBluetooth(BTMethods.Toast, String.format("File %s not available on paired device", fCache) );
            }
        }
    }

    private void writeMethodToBluetoothDelayed(BTMethods method, Object... args) {
        StringBuilder sbBTDelayed = new StringBuilder();
        addMethodAndArgs(sbBTDelayed, method, args);
        mBluetoothControlService.sendDelayed(sbBTDelayed.toString());
    }

    private synchronized void writeMethodToBluetooth(BTMethods method, Object... args) {
        boolean bDoSend = true;
        // DO not send some if Slave
        switch (method) {
            // do not invoke for 'Slave' connection
            case updatePreference:
            case swapPlayers:
            case swapDoublePlayers:
            case cancelTimer:
            case startTimer:
            case restartScore:
            case timestampStartOfGame:
          //case toggleSetScoreView:
            case toggleGameScoreView:
                if ( BTRole.Slave.equals(m_blueToothRole) ) {
                    bDoSend = false;
                }
                break;
            case changeScoreBLE:
                bDoSend = false; // command for bluetooth LIGHT EDITION (BLE) only
                break;
            case changeScoreBLEConfirm:
                bDoSend = false; // command for bluetooth LIGHT EDITION (BLE) only
                break;
            default:
                if ( BTRole.Slave.equals(m_blueToothRole) ) {
                    Log.d(TAG, "Sending as (currently being) slave " + method);
                }
        }

        if ( bDoSend == false ) {
            //Log.d(TAG, "Not sending " + method + " (as Slave)");
            return;
        }

        // construct function-call-like string
        StringBuilder sb = new StringBuilder();
        addMethodAndArgs(sb, method, args);
        final String sMessage = sb.toString();

        if ( mBluetoothControlService != null) {
            if ( mBluetoothControlService.getState().equals(BTState.CONNECTED) ) {
                //Log.d(TAG, "About to write BT message " + sMessage.trim());
                mBluetoothControlService.write(sMessage);
            }
        }

        sendMessageToWearables(sMessage);
    }

    private void addMethodAndArgs(StringBuilder sb, BTMethods method, Object[] args) {
        sb.append(method);
        sb.append("(");

        // comma separated arguments
        if ( args != null ) {
            for (int i = 0; i < args.length; i++) {
                if (i > 0) {
                    sb.append(",");
                }
                if (args[i] != null) {
                    sb.append(args[i]);
                }
            }

            // for score changing simple actions, also send score to be verified on target (if out of sync target should request complete matchmodel to get back in sync)
            if ( method.verifyScore() ) {
                sb.append(",");
                sb.append(matchModel.getScore(Player.A) + "-" + matchModel.getScore(Player.B));
            }
        }

        sb.append(")");
    }

    /** length of file to be read */
    private int           iReceivingBTFileLength = 0;
    /** Buffer with file content */
    private StringBuilder sbReceivingBTFile      = new StringBuilder();
    public synchronized void interpretReceivedMessageOnUiThread(String readMessage, MessageSource source) {
        runOnUiThread(() -> interpretReceivedMessage(readMessage, source));
    }
    /** Invoked by BluetoothHandler, BLEHandler, WearableHelper.MessageListener and PusherHandler */
    public synchronized void interpretReceivedMessage(String readMessage, MessageSource msgSource) {

        if ( readMessage.startsWith(BTMethods.requestCompleteJsonOfMatch.toString())) {
            // don't blindly pass on this type of message to wearable
        } else {
            if ( msgSource.equals(MessageSource.Wearable) ) {
                // message came from wearable, if also connected manually via bluetooth
                if ( mBluetoothControlService != null) {
                    mBluetoothControlService.write(readMessage);
                }
            } else {
                // message did not come from wearable: also pass on to wearable
                sendMessageToWearables(readMessage);
            }
        }

        // read file if json is being sent (identified by first sending the length of the json string)
        {
            int    iSubStrLength = Math.min(20, readMessage.length());
            String readMessage20 = readMessage.substring(0, iSubStrLength);
            if ( (iReceivingBTFileLength <= 0) && readMessage20.matches("^\\d+:.*") ) {
                // first part of a long message being received
                String sLength = readMessage20.replaceFirst(":.*", "");
                iReceivingBTFileLength = Integer.parseInt(sLength);
                Log.d(TAG, "Receiving file content with length:" + iReceivingBTFileLength);
                readMessage = readMessage.substring(sLength.length() + 1);
            }
            if ( iReceivingBTFileLength > 0 ) {
                // in the process of reading a file
                sbReceivingBTFile.append(readMessage);
                if (sbReceivingBTFile.length() >= iReceivingBTFileLength ) {
                    // whole file received
                    final String sFileContent = sbReceivingBTFile.toString();

                    Log.d(TAG, String.format("Full file related message received (%d) : %s", sFileContent.length(), sFileContent ));

                    // reset for next file communication
                    sbReceivingBTFile      = new StringBuilder();
                    iReceivingBTFileLength = 0;

                    if ( sFileContent.startsWith("{") && sFileContent.endsWith("}") ) {
                        Log.d(TAG, "Parsing received match...");
                        if ( msgSource.equals(MessageSource.FirebaseCloudMessage) ) {
                            Model m = Brand.getModel();
                            m.fromJsonString(sFileContent);
                            boolean bStartNewMatchDialog = StringUtil.areAllNonEmpty(m.getName(Player.A), m.getName(Player.B));
                            if ( bStartNewMatchDialog ) {
                                Intent intent = new Intent();
                                intent.putExtra(IntentKeys.NewMatch.toString(), sFileContent); // this is read by ScoreBoard.onActivityResult
                                PreferenceValues.initForLiveScoring(this, true);

                                if ( StringUtil.isEmpty(m.getSource()) ) {
                                    String sSourceId = null; // DateUtil.getCurrentYYYYMMDDTHHMMSS();  // TODO
                                    m.setSource(MessageSource.FirebaseCloudMessage.toString(), sSourceId);
                                    String sFileContentEnriched = m.toJsonString(null);
                                    intent.putExtra(IntentKeys.NewMatch.toString(), sFileContentEnriched);
                                }

                                // assume json of a Model of a 'new match' to be reffed is being sent via an FCM message
                                // act as if the match was selected from a feed
                                final int iRequestCode = R.id.sb_select_feed_match;
                                if ( childActivityRequestCode() == iRequestCode ) {
                                    Toast.makeText(this, "Ignoring new match to ref received by FCM message: " + sFileContent + ".\nRelated child activity already open", Toast.LENGTH_LONG).show();
                                } else {
                                    onActivityResult(iRequestCode, 0 , intent);
                                    if ( PreferenceValues.showToastMessageForEveryReceivedFCMMessage(this)) {
                                        Toast.makeText(this, "New match to ref received by FCM message:\n" + sFileContent, Toast.LENGTH_LONG).show();
                                    }
                                }
                            } else {
                                // TODO: implement other options. e.g. populate 'My List' and open 'My List'
                                Toast.makeText(this, "JSON received via FCM is not valid to represent a match.\n" + sFileContent, Toast.LENGTH_LONG).show();
                            }
                        } else {
                            try {
                                PersistHelper.storeAsPrevious(this, matchModel, false);
                                //persist(true);
                                File fJson = PersistHelper.getLastMatchFile(this);
                                FileUtil.writeTo(fJson, sFileContent);

                                setMatchModel(null);
                                boolean bReadOK = this.initScoreBoard(fJson);
                                Log.d(TAG, "Parsing: " + bReadOK);
                                if ( bReadOK ) {
                                    matchModel.triggerListeners();
                                    if ( BTRole.Slave.equals(m_blueToothRole) ) {
                                        String bluetooth_name = Settings.Secure.getString(getContentResolver(), "bluetooth_name");
                                        if ( StringUtil.isEmpty(bluetooth_name) && mBluetoothAdapter != null ) {
                                            bluetooth_name = mBluetoothAdapter.getName();
                                        }
                                        if ( StringUtil.isEmpty(bluetooth_name) ) {
                                            bluetooth_name = Build.MODEL;
                                        }
                                        writeMethodToBluetooth(BTMethods.jsonMatchReceived, true);
                                        writeMethodToBluetooth(BTMethods.Toast, getString(R.string.bt_match_received_by_X, bluetooth_name));
                                    }
                                    bluetoothRequestCountryFile(2000);
                                } else {
                                    writeMethodToBluetooth(BTMethods.Toast, "Receiver could not read json...");
                                    if ( BTRole.Slave.equals(m_blueToothRole) ) {
                                        writeMethodToBluetooth(BTMethods.requestCompleteJsonOfMatch);
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                writeMethodToBluetooth(BTMethods.Toast, "Could not read json. Exception: " + e.getMessage());

                                // re-requesting complete match if exception occurred
                            }
                        }
                    } else {
                        // assume base64 encoded image
                        if ( sFileContent.startsWith(BTFileType.CountryFlag + ":") ) {
                            Log.d(TAG, "Parsing flag...");
                            int    iFirstColon  = sFileContent.indexOf(":");
                            int    iSecondColon = sFileContent.indexOf(":", iFirstColon + 1);
                            String sCountryCode = null;
                            try {
                                       sCountryCode = sFileContent.substring(iFirstColon+1, iSecondColon);
                                String sBase64      = sFileContent.substring(iSecondColon+1);

                                File   fCache = PreferenceValues.getFlagCacheName(sCountryCode, this);
                                byte[] decodedByte    = Base64.decode(sBase64, 0);
                                FileOutputStream fileOutputStream = new FileOutputStream(fCache);
                                fileOutputStream.write(decodedByte);
                                fileOutputStream.close();
                                Log.d(TAG, String.format("Created flag file %s via bluetooth", fCache));

                                // to refresh PlayerButton with the image
                                for(Player p: Player.values() ) {
                                    String sCountry = matchModel.getCountry(p);
                                    if ( sCountry.equals(sCountryCode) ) {
                                        iBoard.updatePlayerCountry(p, sCountry);
                                    }
                                }

                                // we only request one file at a time, if one is received, request the other if required
                                bluetoothRequestCountryFile(2000);
                            } catch (Exception e) {
                                e.printStackTrace();
                                Toast.makeText(this, String.format("Could not interpret file as image for %s", sCountryCode), Toast.LENGTH_LONG).show();
                                writeMethodToBluetooth(BTMethods.Toast, "Receiver could not interpret image for " + sCountryCode);
                            }
                        } else {
                            Log.w(TAG, "Parsing what ??...");
                        }
                    }
                } else {
                    Log.d(TAG, String.format("Waiting for more file content...(%d < %d)", sbReceivingBTFile.length(), iReceivingBTFileLength));
                }
                return;
            }
        }

        // if messages were queued on the 'Master'/sender, ensure we split them back here
        String[] saReadMethods = readMessage.trim().split("\n");
        if ( saReadMethods.length > 1 ) {
            Log.w(TAG, "Multiple methods received: " + saReadMethods.length);
        }

        for(String sMethodNArgs: saReadMethods) {

            // read what model.method() to invoke with what arguments
            String[]  saMethodNArgs = sMethodNArgs.trim().split("[\\(\\),]");
            String    sMethod       = saMethodNArgs[0];
            BTMethods btMethod      = null;
            try {
                btMethod = BTMethods.valueOf(sMethod);
            } catch (Exception e) {
                Log.w(TAG, "Could not derive method from " + sMethod);
                // might happen if connection is broken unexpectedly
                // or old version communicating with new version with new method
                //e.printStackTrace();
            }
            if ( matchModel == null ) {
                Log.w(TAG, "Matchmodel is null"); // should not happen normally
            } else if ( btMethod == null ) {
                Log.w(TAG, String.format("Could not derive btMethod from message %s [#%d]", sMethodNArgs.substring(0, Math.min(20, sMethodNArgs.length())) + "...", sMethodNArgs.length()));
                //Toast.makeText(this, String.format("Could not derive btMethod from message %s", sMethod), Toast.LENGTH_LONG).show();
            } else {
                if ( sMethodNArgs.trim().contains("(") && sMethodNArgs.trim().endsWith(")") == false ) {
                    Log.w(TAG, "method received but with incomplete arguments: " + sMethodNArgs); // should not happen normally
                    if ( BTRole.Slave.equals(m_blueToothRole) ) {
                        //Toast.makeText(this, "method received but with incomplete arguments: " + sMethodNArgs, Toast.LENGTH_LONG).show();
                    }
                }
                switch (btMethod) {
                    case updatePreference: {
                        if ( saMethodNArgs.length >= 3 ) {
                            try {
                                PreferenceKeys key    = PreferenceKeys.valueOf(saMethodNArgs[1]);
                                String         sValue = saMethodNArgs[2];
                                if ( sValue.matches("true|false") ) {
                                    PreferenceValues.setBoolean(key, this, Boolean.parseBoolean(sValue));
                                } else if ( StringUtil.isInteger(sValue) ) {
                                    PreferenceValues.setNumber(key, this, Integer.parseInt(sValue));
                                } else {
                                    PreferenceValues.setString(key, this, sValue);
                                }
                                Toast.makeText(this, String.format("Updated pref over bluetooth %s=%s", key, sValue), Toast.LENGTH_LONG).show();
                                if (saMethodNArgs[1].toLowerCase().contains("color") ) {
                                    ColorPrefs.clearColorCache();
                                    initColors();
                                }
                            } catch (IllegalArgumentException e) {
                                // most likely com.doubleyellow.scoreboard.prefs.ColorPrefs.ColorTarget
                                try {
                                    ColorPrefs.ColorTarget key    = ColorPrefs.ColorTarget.valueOf(saMethodNArgs[1]);
                                    String         sValue = saMethodNArgs[2];
                                    PreferenceValues.setString(key, this, sValue);
                                    Toast.makeText(this, String.format("Updated color pref over bluetooth %s=%s", key, sValue), Toast.LENGTH_LONG).show();
                                    ColorPrefs.clearColorCache();
                                    initColors();
                                } catch (IllegalArgumentException e2) {
                                    // most likely com.doubleyellow.scoreboard.prefs.ColorPrefs.ColorTarget
                                    // PlayerColorsNewMatchA , PlayerColorsNewMatchB
                                }
                            }
                        } else {
                            Toast.makeText(this, String.format("Could not handle %s,%s,%s", (Object[]) saMethodNArgs), Toast.LENGTH_LONG).show();
                        }
                        break;
                    }
                    case changeScoreBLEConfirm: {
                        Player          playerWristBand      = Player         .valueOf(saMethodNArgs[1].toUpperCase().trim());
                        BLEDeviceButton eButtonPressed       = BLEDeviceButton.valueOf(saMethodNArgs[2].toUpperCase().trim());
                        int             iNrOfDevicesRequired = m_bleConfig.optInt   (BLEUtil.Keys.NrOfDevices.toString(), 2);
                        Log.i(TAG, String.format("[interpretReceivedMessage] changeScoreBLEConfirm: %s, player:%s, button:%s", m_blePlayerWaitingForScoreToBeConfirmed, playerWristBand, eButtonPressed));
                        int iTmpTxtOnElementDuringFeedback = getTxtOnElementDuringFeedback(m_blePlayerWaitingForScoreToBeConfirmed);
                        if ( iNrOfDevicesRequired == 1 ) {
                            if ( m_blePlayerWaitingForScoreToBeConfirmed != null ) {
                                String sDoChangeScore = null;
                                String sDoCancelScore = null;
                                if ( playerWristBand.equals(m_blePlayerWaitingForScoreToBeConfirmed) ) {
                                    // button for same player pressed again
                                    if ( m_bSingleDevice_ConfirmWithSameButton ) {
                                        sDoChangeScore = getBLEMessage(R.string.ble_score_confirmed_by_pressing_y, eButtonPressed);
                                    } else {
                                        sDoCancelScore = getBLEMessage(R.string.ble_score_cancelled_by_pressing_y, eButtonPressed);
                                    }
                                } else {
                                    // button for other player pressed
                                    if ( m_bSingleDevice_ConfirmWithSameButton ) {
                                        sDoCancelScore = getBLEMessage(R.string.ble_score_cancelled_by_pressing_y, eButtonPressed);
                                    } else {
                                        sDoChangeScore = getBLEMessage(R.string.ble_score_confirmed_by_pressing_y, eButtonPressed);
                                    }
                                }
                                if ( sDoChangeScore != null ) {
                                    Log.i(TAG, "sDoChangeScore : " + sDoChangeScore);

                                    stopWaitingForBLEConfirmation();
                                    iBoard.showBLEInfoMessage(sDoChangeScore, 10);
                                    startVisualFeedbackForScoreChange(m_blePlayerWaitingForScoreToBeConfirmed, iTmpTxtOnElementDuringFeedback);
                                    m_blePlayerWaitingForScoreToBeConfirmed = null;
                                } else if ( sDoCancelScore != null ) {
                                    Log.i(TAG, "sDoCancelScore : " + sDoCancelScore);
                                    m_blePlayerWaitingForScoreToBeConfirmed = null;
                                    stopWaitingForBLEConfirmation();
                                    iBoard.showBLEInfoMessage(sDoCancelScore, 10);
                                } else {
                                    // should not happen... with single device one button should confirm, the other cancel (third button?)
                                    Log.w(TAG, "[Single device] Score still waiting confirmation? " + m_blePlayerWaitingForScoreToBeConfirmed + " " + m_bSingleDevice_ConfirmWithSameButton);
                                    iBoard.appendToInfoMessage(".");
                                }
                            } else {
                                BLEDeviceButton eButtonOther     = BLEDeviceButton.values()[1 - eButtonPressed.ordinal()];
                                BLEDeviceButton eButtonToConfirm = m_bSingleDevice_ConfirmWithSameButton ? eButtonPressed : eButtonOther;
                                Log.w(TAG, String.format("Score for %s entered with button %s now waiting for confirmation by pressing %s", playerWristBand, eButtonPressed, eButtonToConfirm));
                                m_blePlayerWaitingForScoreToBeConfirmed = playerWristBand;

                                String sToConfirmMsg = getBLEMessage(R.string.ble_pressed_X__confirm_score_for_Y_by_pressing_Z, eButtonPressed, m_blePlayerWaitingForScoreToBeConfirmed, eButtonToConfirm);
                                iBoard.showBLEInfoMessage(sToConfirmMsg, -1);
                                startWaitingForBLEConfirmation(m_blePlayerWaitingForScoreToBeConfirmed, null);
                            }

                        } else if ( iNrOfDevicesRequired == 2 ) {
                            if ( m_blePlayerWaitingForScoreToBeConfirmed != null ) {
                                String sDoChangeScore = null;
                                String sDoCancelScore = null;
                                if (playerWristBand.getOther().equals(m_blePlayerWaitingForScoreToBeConfirmed)) {
                                    // check the confirmation by other player/team
                                    Log.i(TAG, "Check BLE confirmation by " + playerWristBand + " with button " + m_eConfirmScoreByOpponentButton);
                                    if (eButtonPressed.equals(m_eConfirmScoreByOpponentButton)) {
                                        sDoChangeScore = getBLEMessage(R.string.ble_score_confirmed_by_opponent_x_by_pressing_y, playerWristBand, eButtonPressed);
                                    } else if ( eButtonPressed.equals(m_eCancelScoreByOpponentButton) ) {
                                        sDoCancelScore = getBLEMessage(R.string.ble_score_cancelled_by_opponent_x_by_pressing_y, playerWristBand, eButtonPressed);
                                    }
                                } else {
                                    // same player/team pressed a button...
                                    Log.w(TAG, "Player " + m_blePlayerWaitingForScoreToBeConfirmed + " waiting for confirmation by opponent pressing " + m_eConfirmScoreByOpponentButton);
                                    if (eButtonPressed.equals(m_eCancelScoreByInitiatorButton)) {
                                        sDoCancelScore = getBLEMessage(R.string.ble_score_cancelled_by_initiator_x_by_pressing_y, playerWristBand, eButtonPressed);
                                    }
                                }
                                if (sDoChangeScore != null) {
                                    Log.i(TAG, "sDoChangeScore : " + sDoChangeScore);

                                    stopWaitingForBLEConfirmation();
                                    iBoard.showBLEInfoMessage(sDoChangeScore, 10);
                                    startVisualFeedbackForScoreChange(m_blePlayerWaitingForScoreToBeConfirmed, iTmpTxtOnElementDuringFeedback);
                                    m_blePlayerWaitingForScoreToBeConfirmed = null;
                                } else if (sDoCancelScore != null) {
                                    Log.i(TAG, "sDoCancelScore : " + sDoCancelScore);
                                    m_blePlayerWaitingForScoreToBeConfirmed = null;
                                    stopWaitingForBLEConfirmation();
                                    iBoard.showBLEInfoMessage(sDoCancelScore, 10);
                                } else {
                                    Log.w(TAG, "Score still waiting confirmation? " + m_blePlayerWaitingForScoreToBeConfirmed);
                                    iBoard.appendToInfoMessage(".");
                                }
                            } else if ( m_blePlayerToConfirmOwnScore != null ) {
                                String sDoChangeScore = null;
                                String sDoCancelScore = null;
                                if ( playerWristBand.equals(m_blePlayerToConfirmOwnScore) ) {
                                    // check the confirmation by scoring player/team
                                    Log.i(TAG, "Check BLE confirmation by " + playerWristBand + " with button " + m_eConfirmScoreBySelfButton);
                                    if (eButtonPressed.equals(m_eConfirmScoreBySelfButton)) {
                                        sDoChangeScore = getBLEMessage(R.string.ble_score_confirmed_by_scoring_team_x_by_pressing_y, playerWristBand, eButtonPressed);
                                    } else {
                                        sDoCancelScore = getBLEMessage(R.string.ble_score_cancelled_by_scoring_team_x_by_pressing_y, playerWristBand, eButtonPressed);
                                    }
                                } else {
                                    // same player/team pressed a button...
                                    if ( eButtonPressed.equals(m_eInitiateOpponentScoredChangeButton.getOther()) ) {
                                        sDoCancelScore = getBLEMessage(R.string.ble_score_cancelled_by_initiator_x_by_pressing_y, playerWristBand, eButtonPressed);
                                    } else {
                                        Log.w(TAG, "Player " + m_blePlayerToConfirmOwnScore + " to confirm own score by pressing " + m_eConfirmScoreBySelfButton);
                                        iBoard.appendToInfoMessage(".");
                                    }
                                }
                                if (sDoChangeScore != null) {
                                    Log.i(TAG, "sDoChangeScore : " + sDoChangeScore);

                                    stopWaitingForBLEConfirmation();
                                    iBoard.showBLEInfoMessage(sDoChangeScore, 10);
                                    startVisualFeedbackForScoreChange(m_blePlayerToConfirmOwnScore, iTmpTxtOnElementDuringFeedback);
                                    m_blePlayerToConfirmOwnScore = null;
                                } else if (sDoCancelScore != null) {
                                    Log.i(TAG, "sDoCancelScore : " + sDoCancelScore);
                                    m_blePlayerToConfirmOwnScore = null;
                                    stopWaitingForBLEConfirmation();
                                    iBoard.showBLEInfoMessage(sDoCancelScore, 10);
                                } else {
                                    Log.w(TAG, "Score still waiting confirmation? " + m_blePlayerToConfirmOwnScore);
                                    iBoard.appendToInfoMessage(".");
                                }
                            } else {
                                if ( eButtonPressed.equals(m_eInitiateSelfScoreChangeButton) ) {
                                    Log.w(TAG, String.format("Score for %s entered with button %s now waiting for confirmation by opponent %s pressing %s", playerWristBand, eButtonPressed, playerWristBand.getOther(), m_eConfirmScoreByOpponentButton));
                                    m_blePlayerWaitingForScoreToBeConfirmed = playerWristBand;

                                    String sToConfirmMsg = getBLEMessage(R.string.ble_player_x_confirm_score_for_y_by_pressing_z, playerWristBand.getOther(), playerWristBand, m_eConfirmScoreByOpponentButton);
                                    iBoard.showBLEInfoMessage(sToConfirmMsg, -1);
                                    startWaitingForBLEConfirmation(m_blePlayerWaitingForScoreToBeConfirmed, playerWristBand.getOther());
                                } else if ( eButtonPressed.equals(m_eInitiateOpponentScoredChangeButton) ) {
                                    Log.w(TAG, String.format("Score for opponent entered by %s with button %s now waiting for confirmation by scoring player %s pressing %s", playerWristBand, eButtonPressed, playerWristBand.getOther(), m_eConfirmScoreBySelfButton));
                                    m_blePlayerToConfirmOwnScore = playerWristBand.getOther();

                                    String sToConfirmMsg = getBLEMessage(R.string.ble_player_x_confirm_you_scored_by_pressing_y, playerWristBand.getOther(), m_eConfirmScoreBySelfButton);
                                    iBoard.showBLEInfoMessage(sToConfirmMsg, -1);
                                    startWaitingForBLEConfirmation(m_blePlayerToConfirmOwnScore, playerWristBand.getOther());
                                } else {
                                    Log.w(TAG, String.format("In state waiting for initiate-score-change, button %s does nothing ", eButtonPressed));
                                    if ( PreferenceValues.currentDateIsTestDate() ) {
                                        String sInfoMsg = getBLEMessage(R.string.ble_waiting_initiate_score_change_message, m_eInitiateSelfScoreChangeButton);
                                        iBoard.showBLEInfoMessage(sInfoMsg, 4);
                                    }
                                }
                            }
                        }
                        break;
                    }
                    case changeScore: {
                        if ( (saMethodNArgs.length > 1) && (matchModel != null) ) {
                            String sAorB = saMethodNArgs[1].toUpperCase();
                            Player player;
                            if ( sAorB.matches("[0-1]") ) {
                                int i0isA1IsB = Integer.parseInt(sAorB);
                                player = Player.values()[i0isA1IsB];
                            } else {
                                player = Player.valueOf(sAorB);
                            }
                            String sScoreReceived = saMethodNArgs[saMethodNArgs.length - 1];
                            String sModelScore    = matchModel.getScore(Player.A) + "-" + matchModel.getScore(Player.B);
                            if ( sScoreReceived.equals("0-0") && sModelScore.equals("0-0") ) {
                                // if endGame=automatic changeScore might be send to set score to 0-0, but if slave already changed to 0-0, ignore
                                Log.w(TAG, String.format("Ignoring %s", btMethod));
                            } else {
                                matchModel.changeScore(player);
                            }
                        }
                        break;
                    }
                    case undoScoreForInitiatorBLE:
                        if ( (saMethodNArgs.length > 1) && (matchModel != null) ) {
                            String sAorB = saMethodNArgs[1].toUpperCase().trim();
                            Player pUndoTriggeredBy;
                            if ( sAorB.matches("[0-1]") ) {
                                int i0isA1IsB = Integer.parseInt(sAorB);
                                pUndoTriggeredBy = Player.values()[i0isA1IsB];
                            } else {
                                pUndoTriggeredBy = Player.valueOf(sAorB);
                            }
                            Player lastScorer = matchModel.getLastScorer();

                            String sInfoMsg = getBLEMessage(R.string.ble_last_score_for_X_undone_by_ble, pUndoTriggeredBy);
                            if ( pUndoTriggeredBy.equals(lastScorer) ) {
                                //matchModel.undoLast(); // triggered by timer after blinking 'undo'
                                startVisualFeedbackForScoreChange(lastScorer, R.string.uc_undo);
                                iBoard.showBLEInfoMessage(sInfoMsg, 10);
                            } else {
                                if ( lastScorer != null ) {
                                    sInfoMsg = getBLEMessage(R.string.ble_last_score_for_X_can_not_be_undone_by_ble_of_Y, lastScorer, pUndoTriggeredBy);
                                    iBoard.showBLEInfoMessage(sInfoMsg, 10);
                                }
                            }
                        }
                        break;
                    case changeScoreBLE:
                        if ( (saMethodNArgs.length > 1) && (matchModel != null) ) {
                            // derive score to change from first parameter
                            String sAorB = saMethodNArgs[1].toUpperCase().trim();
                            Player pScored;
                            if ( sAorB.matches("[0-1]") ) {
                                int i0isA1IsB = Integer.parseInt(sAorB);
                                pScored = Player.values()[i0isA1IsB];
                            } else {
                                pScored = Player.valueOf(sAorB);
                            }

                            // derive initiated-by-player and by-button-pressed from optional 2nd and 3th parameter(s)
                            Player pInitiatedBy = pScored;
                            BLEDeviceButton buttonPressed = null;
                            int[] iaPosition = {2,3};
                            for(int iParamPos: iaPosition) {
                                if ( saMethodNArgs.length > iParamPos ) {
                                    sAorB = saMethodNArgs[iParamPos].toUpperCase().trim();
                                    if ( sAorB.length() == 1 ) {
                                        if ( sAorB.matches("[0-1]") ) {
                                            int i0isA1IsB = Integer.parseInt(sAorB);
                                            pInitiatedBy = Player.values()[i0isA1IsB];
                                        } else {
                                            pInitiatedBy = Player.valueOf(sAorB);
                                        }
                                    } else {
                                        String sPrimaryOrSecondary = sAorB;
                                        buttonPressed = BLEDeviceButton.valueOf(sPrimaryOrSecondary);
                                    }
                                }
                            }
                            int iTmpTxtOnElementDuringFeedback = getTxtOnElementDuringFeedback(pScored);
                            String sInfoMsg = getBLEMessage(R.string.ble_score_for_X_changed_by_Y_ble_button_of_Z, pScored, buttonPressed, pInitiatedBy);
                            if ( m_nrOfBLEDevicesConnected == 1 ) {
                                sInfoMsg = getBLEMessage(R.string.ble_score_for_X_changed_by_Y_ble_button, pScored, buttonPressed);
                            }
                            iBoard.showBLEInfoMessage(sInfoMsg, 10);
                            startVisualFeedbackForScoreChange(pScored, iTmpTxtOnElementDuringFeedback);
                        }
                        break;
                    case changeSide: {
                        if ( (saMethodNArgs.length > 1) && (matchModel != null) ) {
                            Player player = Player.valueOf(saMethodNArgs[1].toUpperCase());
                            matchModel.changeSide(player);
                        }
                        break;
                    }
                    case changeColor: {
                        if ( saMethodNArgs.length > 1 && (matchModel != null) ) {
                            Player player = Player.valueOf(saMethodNArgs[1].toUpperCase());
                            matchModel.setPlayerColor(player, saMethodNArgs.length>2?saMethodNArgs[2]:null);
                        }
                        break;
                    }
                    case undoLast: {
                        matchModel.undoLast();
                        break;
                    }
                    case undoLastForScorer: {
                        if ( saMethodNArgs.length > 1 && (matchModel != null) ) {
                            Player nonScorer = Player.valueOf(saMethodNArgs[1].toUpperCase());
                            matchModel.undoLastForScorer(nonScorer);
                        }
                        break;
                    }
                    case endGame: {
                        matchModel.endGame();
                        break;
                    }
                    case timestampStartOfGame: {
                        if ( saMethodNArgs.length > 1 && (matchModel != null) ) {
                            GameTiming.ChangedBy changedBy = GameTiming.ChangedBy.valueOf(saMethodNArgs[1]);
                            matchModel.timestampStartOfGame(changedBy);
                        }
                        break;
                    }
                    case cancelTimer: {
                        cancelTimer();
                        break;
                    }
                    case startTimer: {
                        if ( saMethodNArgs.length > 1 ) {
                            //Type timerType = Type.valueOf(saMethodNArgs[1]); // might be empty string in rare cases
                            Type timerType = Params.getEnumValueFromString(Type.class,saMethodNArgs[1]);
                            boolean  bAutoStarted = (saMethodNArgs.length>2) ? Boolean.valueOf(saMethodNArgs[2]) : false;
                            ViewType viewType     = (saMethodNArgs.length>3) ? Params.getEnumValueFromString(ViewType.class,saMethodNArgs[3]): null;
                            Integer  iInitialSecs = (saMethodNArgs.length>4) ? Integer.parseInt(saMethodNArgs[4]) : null;

                            _showTimer(timerType, bAutoStarted, viewType, iInitialSecs);
                        } else {
                            Log.d(TAG, "Method needs arguments: " + btMethod);
                        }
                        break;
                    }
                    case restartTimerWithSecondsLeft: {
                        if ( saMethodNArgs.length > 1 ) {
                            DialogTimerView.restartTimerWithSecondsLeft(Integer.parseInt(saMethodNArgs[1]));
                            break;
                        }
                    }
                    case recordAppealAndCall: {
                        if ( saMethodNArgs.length > 2 && (matchModel != null) ) {
                            Player player = Player.valueOf(saMethodNArgs[1]);
                            Call   call   = Call  .valueOf(saMethodNArgs[2]);
                            matchModel.recordAppealAndCall(player, call);
                        }
                        break;
                    }
                    case recordConduct: {
                        if ( saMethodNArgs.length > 3 && (matchModel != null) ) {
                            Player      player      = Player     .valueOf(saMethodNArgs[1].toUpperCase());
                            Call        call        = Call       .valueOf(saMethodNArgs[2]);
                            ConductType conductType = ConductType.valueOf(saMethodNArgs[3]);
                            matchModel.recordConduct(player, call, conductType);
                        }
                        break;
                    }
                    case restartScore: {
                        restartScore();
                        break;
                    }
                    case jsonMatchReceived: {
                        // do not actually swap sides, but make sure mirrored device displays LR as desired
                        swapSidesOnBT(iBoard.m_firstPlayerOnScreen); // ??
                        break;
                    }
                    case requestCompleteJsonOfMatch: {
                        sendMatchToOtherBluetoothDevice(false, 2000);
                        break;
                    }
                    case requestCountryFlag: {
                        if ( saMethodNArgs.length > 1 ) {
                            sendFlagToOtherBluetoothDevice(this, saMethodNArgs[1]);
                        }
                        break;
                    }
                    case swapPlayers: {
                        if ( saMethodNArgs.length > 1 ) {
                            Player pFirst = Player.valueOf(saMethodNArgs[1].toUpperCase());
                            swapSides(Toast.LENGTH_LONG, pFirst);
                        }
                        break;
                    }
                    case swapDoublePlayers: {
                        if ( saMethodNArgs.length > 1 ) {
                            Player player = Player.valueOf(saMethodNArgs[1].toUpperCase());
                            _swapDoublePlayers(player);
                        }
                        break;
                    }
                    case toggleGameScoreView: {
                        if ( Brand.isGameSetMatch() ) {
                            toggleSetScoreView();
                        } else {
                            toggleGameScoreView();
                        }
                        break;
                    }
                    case Toast: {
                        if ( saMethodNArgs.length > 1 ) {
                            String sMsg = saMethodNArgs[1];
                            if ( StringUtil.isInteger(sMsg) ) {
                                // a resource id was send
                                try {
                                    sMsg = getString(Integer.parseInt(sMsg), (mBluetoothHandler.getOtherDeviceName()) );
                                } catch (Exception e) {
                                    //e.printStackTrace();
                                }
                            }
                            Toast.makeText(this, sMsg, Toast.LENGTH_LONG).show();
                        }
                        break;
                    }
                    case resume: // fall through
                    case resume_confirmed: {
                        String sJson = matchModel.toJsonString(null, null, null);
                        int iJsonLengthHere = sJson.length();

                        setWearableRole(WearRole.AppRunningOnBoth);

                        Log.d(TAG, "[resume|resume_confirmed] Received " + btMethod);
                        if ( btMethod.equals(BTMethods.resume) ) {
                            String sMessage = BTMethods.resume_confirmed + "(" + matchModel.getMatchStartTimeHHMMSSXXX() + "," + iJsonLengthHere + ")";
                            Log.d(TAG, "Send " + sMessage);
                            sendMessageToWearablesUnchecked(sMessage);
                        } else {
                            // btMethod = resume confirmed
                            String sMatchStartTimeHere        = matchModel.getMatchStartTimeHHMMSSXXX();
                            String sMatchStartTimeCounterPart = ( saMethodNArgs.length > 1 ) ? saMethodNArgs[1]: null;
                            int    iMatchJsonLengthOther      = ( saMethodNArgs.length > 2 ) ? Integer.parseInt(saMethodNArgs[2]) : 0;
                            Log.d(TAG, String.format("Match times (here - counterpart): %s - %s", sMatchStartTimeHere, sMatchStartTimeCounterPart));
                            if ( sMatchStartTimeHere.equals(sMatchStartTimeCounterPart) ) {
                                Log.d(TAG, String.format("Match json length (here - counterpart) : %d - %d", iJsonLengthHere, iMatchJsonLengthOther));
                                // sync has happened before, keep them in sync. Take json from device that is 'biggest'
                                if ( iJsonLengthHere > iMatchJsonLengthOther ) {
                                    sendMatchFromToWearable(sJson);
                                } else if ( iJsonLengthHere < iMatchJsonLengthOther ) {
                                    sendMessageToWearablesUnchecked(BTMethods.requestCompleteJsonOfMatch);
                                } else {
                                    // assume matches are still in sync
                                }
                            } else if ( isWearable() == false ) {
                                pullOrPushMatchOverBluetoothWearable( isWearable() ? "Handheld" : "Wearable");
                            } else {
                                Log.d(TAG, "Not auto syncing for wearable");
                            }
                        }
                        break;
                    }
                    case paused: {
                        setWearableRole(WearRole.PausedOnOther);
                        break;
                    }
    /*
                    case openSuggestMatchSyncDialogOnOtherPaired: {
                        pullOrPushMatchOverBluetoothWearable( isWearable() ? "Handheld" : "Wearable");
                        break;
                    }
    */
                    case lock: {
                        handleMenuItem(R.id.sb_lock);
                        break;
                    }
                    case unlock: {
                        handleMenuItem(R.id.sb_unlock);
                        break;
                    }
                    default:
                        Log.w(TAG, "Not handling method " + btMethod);
                        break;
                }
                switch (msgSource) {
                    case Wearable:
                        // message is coming from paired wearable
                        if ( btMethod.verifyScore() ) {
                            String sScoreReceived = saMethodNArgs[saMethodNArgs.length - 1];
                            String sModelScore    = matchModel.getScore(Player.A) + "-" + matchModel.getScore(Player.B);
                            if ( sModelScore.equals(sScoreReceived) == false ) {
                                Log.d(TAG, String.format("Scores don't match: received %s , here %s", sScoreReceived, sModelScore));
                                boolean bRequestModel = true;
                                if ( matchModel.isPossibleGameVictory() && sScoreReceived.equals("0-0") ) {
                                    if ( dialogManager.isDialogShowing() ) {
                                        if ( dialogManager.baseDialog instanceof EndGame ) {
                                            EndGame endGame = (EndGame) dialogManager.baseDialog;
                                            endGame.handleButtonClick(EndGame.BTN_END_GAME_PLUS_TIMER);
                                            bRequestModel = false;
                                        }
                                    }
                                }
                                if ( bRequestModel ) {
                                    sendMessageToWearablesUnchecked(BTMethods.requestCompleteJsonOfMatch);
                                }
                            }
                        } else {
                            //Log.d(TAG, "[WEAR] verify score not required for " + btMethod);
                        }
                        break;
                    case BluetoothMirror:
                        if ( BTRole.Slave.equals(m_blueToothRole) && btMethod.verifyScore() ) {
                            // verify score of model against score received. If not equal request complete matchmodel to get in sync
                            String sScoreReceived = saMethodNArgs[saMethodNArgs.length - 1];
                            String sModelScore    = matchModel.getScore(Player.A) + "-" + matchModel.getScore(Player.B);
                            if ( sModelScore.equals(sScoreReceived) == false ) {
                                Log.d(TAG, String.format("Scores don't match: received %s , here %s", sScoreReceived, sModelScore));
                                writeMethodToBluetooth(BTMethods.requestCompleteJsonOfMatch);
                            }
                            //hidePresentationEndOfGame();
                        }
                        break;
                    case FirebaseCloudMessage:
                        if ( PreferenceValues.showToastMessageForEveryReceivedFCMMessage(this)) {
                            Toast.makeText(this, "Score changed by FCM message: " + readMessage, Toast.LENGTH_SHORT).show();
                        }
                        break;
                }
            }
        }
    }

    private boolean bluetoothRequestCountryFile(int imsDelay) {
        if ( PreferenceValues.hasInternetConnection(this) == false ) {
            // if there are country codes, and this device can not download them, ask the other device to send them
            for (Player p: Player.values() ) {
                final String sCountryCode = matchModel.getCountry(p);
                if ( StringUtil.isNotEmpty(sCountryCode) ) {
                    File fCache = PreferenceValues.getFlagCacheName(sCountryCode, this);
                    if ( fCache.exists() == false ) {
                        CountDownTimer cdt = new CountDownTimer(imsDelay, 500) {
                            @Override public void onTick(long millisUntilFinished) { }
                            @Override public void onFinish() {
                                writeMethodToBluetooth(BTMethods.requestCountryFlag, sCountryCode);
                            }
                        };
                        cdt.start();
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private enum BTFileType {
        CountryFlag,
    }

    private Boolean m_bAppTurnedOnBlueTooth = null;
    private void setupBluetoothControl(boolean bStartTimer) {
        if ( mBluetoothAdapter == null ) {
            Toast.makeText(this, R.string.bt_no_bluetooth_on_device, Toast.LENGTH_SHORT).show();
            return;
        }

        // try to enable
        try {
            if ( mBluetoothAdapter.isEnabled() == false ) {
                Toast.makeText(this, R.string.bt_bluetooth_turning_on_elipses, Toast.LENGTH_LONG).show();

                // we actually do the turning of in a countdowntimer. If we don't the message above is not shown...
                if ( bStartTimer ) {
                    CountDownTimer ct = new CountDownTimer(300, 100) {
                        @Override public void onTick(long millisUntilFinished) { }
                        @Override public void onFinish() {
                            setupBluetoothControl(false);
                        }
                    };
                    ct.start();
                    return;
                } else {
                    if ( mBluetoothAdapter.enable() ) {
                        int iCountDown = 5;
                        while ((mBluetoothAdapter.isEnabled() == false) && (iCountDown > 0) ) {
                            Log.d(TAG, "Waiting for bluetooth to become available: " + iCountDown);
                            synchronized (this) {
                                wait(1000);
                            }
                            iCountDown--;
                        }
                        m_bAppTurnedOnBlueTooth = true;
                        Toast.makeText(this, R.string.bt_bluetooth_has_been_turned_on, Toast.LENGTH_LONG).show();
                    }
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }

        String[] permissions = BLEUtil.getPermissions();
        Set<RWValues.Permission> lPerms = new LinkedHashSet<>();
        for( String sPermName: permissions ) {
            RWValues.Permission permission = PreferenceValues.getPermission(this, PreferenceKeys.enableScoringByBluetoothConnection, sPermName);
            lPerms.add(permission);
        }
        if ( lPerms.size() == 1 && lPerms.iterator().next().equals(RWValues.Permission.Granted) ) {
            // all good
        } else if ( lPerms.contains(RWValues.Permission.Denied) ) {
            if ( PreferenceValues.currentDateIsTestDate() ) {
                ActivityCompat.requestPermissions(this, permissions, PreferenceKeys.enableScoringByBluetoothConnection.ordinal()); // API28 : Can request only one set of permissions at a time
                return;
            }
            (new AlertDialog.Builder(this)).setTitle("Permissions").setIcon(android.R.drawable.stat_sys_data_bluetooth).setMessage(R.string.ble_permission_not_granted).show();
            return;
        } else {
            // request missing
            Log.w(TAG, "Trying to request permission for scanning");
            ActivityCompat.requestPermissions(this, permissions, PreferenceKeys.UseBluetoothLE.ordinal()); // API28 : Can request only one set of permissions at a time
            return;
        }


        SelectDeviceDialog selectDevice = new SelectDeviceDialog(this, matchModel, this);
        int[] iResIds = selectDevice.getBluetoothDevices(true);
        if ( iResIds == null ) {
            addToDialogStack(selectDevice);
        } else {
            // show dialog with info about how to solve/help/why
            MyDialogBuilder.dialogWithOkOnly(this, iResIds[0], iResIds[1], false);
        }
    }

    private void turnOffBlueToothIfTurnedOnByApp() {
        try {
            if ( Boolean.TRUE.equals(m_bAppTurnedOnBlueTooth) && (mBluetoothAdapter != null) ) {
                if ( mBluetoothAdapter.disable() ) {
                    m_bAppTurnedOnBlueTooth = false;
                    Toast.makeText(this, String.format(getString(R.string.bt_bluetooth_turning_off_elipses), Brand.getShortName(this)), Toast.LENGTH_LONG).show();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static BluetoothControlService mBluetoothControlService   = null;
    /**
     * Establish connection with other device
     */
    public void connectBluetoothDevice(String address) {
        if ( (mBluetoothControlService != null) && mBluetoothControlService.getState().equals(BTState.CONNECTED) ) {
            if ( mBluetoothHandler.getOtherDeviceAddressName().equals(address) ) {
                Toast.makeText(this, getString(R.string.bt_already_connected_to_X, address), Toast.LENGTH_SHORT).show();
                return;
            } else {
                // do not disconnectTODO: not if same device was selected and still connected
                mBluetoothControlService.stop();
            }
        }
        if ( mBluetoothControlService == null ) {
            Log.w(TAG, "mBluetoothControlService = null");
            return;
            //mBluetoothControlService = new BluetoothControlService(mBluetoothHandler, Brand.getUUID(), Brand.getShortName(this));
        }
        // Get the BluetoothDevice object
        BluetoothDevice btDeviceOther = mBluetoothAdapter.getRemoteDevice(address);

        setBluetoothRole(BTRole.Master, "Connecting...");
        // Attempt to connect to the device
        mBluetoothControlService.connect(btDeviceOther);
    }
    public String getOtherBluetoothDeviceAddressName() {
        return mBluetoothHandler.getOtherDeviceAddressName();
    }
    public void disconnectBluetoothDevice() {
        mBluetoothControlService.stop();
    }
    // ----------------------------------------------------
    // --------------------- Wearable ---------------------
    // ----------------------------------------------------
    private static boolean isScreenRound(Context context) {
        if ( Build.VERSION.SDK_INT < Build.VERSION_CODES.M /* 23 */ ) { return false; }
        return context.getResources().getConfiguration().isScreenRound();
    }

    private boolean isWearable() {
        return ViewUtil.isWearable(this);
    }
    private void onPauseWearable() {
        if ( m_wearableHelper == null ) { return; }
        m_wearableHelper.onPause(this);
    }
    private WearableHelper m_wearableHelper = null;
    private static boolean m_bNoWearablePossible = false;
    private void onResumeWearable() {
        if ( Build.VERSION.SDK_INT < Build.VERSION_CODES.M /* 23 */ ) { return; }

        if ( m_wearableHelper == null ) {
            try {
                Class.forName("com.google.android.gms.wearable.Wearable");
            } catch (Throwable e) {
                String msg = "Installed on a device with no wearable support";
                if ( m_bNoWearablePossible == false ) {
                    m_bNoWearablePossible = true;
                    //Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                    //e.printStackTrace();
                }
                Log.w(TAG, msg);
                return;
            }
            m_wearableHelper = new WearableHelper(this);
        }
        m_wearableHelper.onResume(this);

    }
    public void setWearableRole(WearRole role) {
        if ( m_wearableHelper == null ) { return; }
        m_wearableHelper.setWearableRole(role);
    }
    private boolean sendMatchFromToWearable(String sJson) {
        if ( m_wearableHelper == null ) { return false; }
        return m_wearableHelper.sendMatchFromToWearable(this, sJson);
    }
    private boolean sendMessageToWearables(String sMessage) {
        if ( m_wearableHelper == null ) { return false; }
        return m_wearableHelper.sendMessageToWearables(this, sMessage);
    }
    private boolean sendMessageToWearablesUnchecked(Object sMessage) {
        if ( m_wearableHelper == null ) { return false; }
        return m_wearableHelper.sendMessageToWearablesUnchecked(this, sMessage);
    }
    private void openPlayStoreOnWearable() {
        if ( m_wearableHelper == null ) { return; }
        m_wearableHelper.openPlayStoreOnWearable(this);
    }

    private void sendSettingToWearable(Object[] ctx) {
        List<String> lPrefKeys = null;
        if ( (ctx != null) && (ctx.length > 0) ) {
            lPrefKeys = (List<String>) ctx[0];
        } else {
            lPrefKeys = Arrays.asList(PreferenceKeys.colorSchema.toString()
                                     , PreferenceKeys.wearable_allowScoringWithHardwareButtons.toString()
                                     , PreferenceKeys.wearable_allowScoringWithRotary.toString()
                                     , PreferenceKeys.wearable_keepScreenOnWhen.toString()
                                     );
        }
        for(String sKey: lPrefKeys) {
            String sValue = PreferenceValues.getString(sKey, null, this); // default 'false' is for preferences that are boolean
            if ( sValue == null ) {
                boolean aBoolean = PreferenceValues.getBoolean(sKey, this, false);
                sValue = String.valueOf(aBoolean);
            }
            // send not wearable_ prefixed key to wearable if without the prefix it is also a preference
            final String prefix = PreferenceKeys.wearable_.toString();
            if ( sKey.startsWith(prefix) ) {
                try {
                    PreferenceKeys pKeyNonPrefixed = PreferenceKeys.valueOf(sKey.substring(prefix.length()));
                    sKey = pKeyNonPrefixed.toString();
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }
            }
            writeMethodToBluetooth(BTMethods.updatePreference, sKey, sValue);
        }
    }

    // ----------------------------------------------------
    // --------------------- casting ----------------------
    // ----------------------------------------------------
    private com.doubleyellow.scoreboard.cast.ICastHelper castHelper = null;
    private void initCasting() {
        if ( ViewUtil.isWearable(this) ) { return; }
        createCastHelper();
    }

    private void createCastHelper() {
        if ( castHelper == null ) {
            Map.Entry<String, String> remoteDisplayAppId2Info = Brand.brand.getRemoteDisplayAppId2Info(this);
            String sResInfo = remoteDisplayAppId2Info.getValue();
            if ( (sResInfo.contains("REMOTE_DISPLAY") || sResInfo.matches(".*Presentation\\s*Screen.*")) ) { // e.g com.doubleyellow.scoreboard:string/REMOTE_DISPLAY_APP_ID_brand_squore
                castHelper = new com.doubleyellow.scoreboard.cast.CastHelper(); // old
            } else {
                // R.string.CUSTOM_RECEIVER_xxxx
                castHelper = new CastHelper(); // new
            }
            castHelper.initCasting(this);
            Log.d(TAG, "Cast helper created " + remoteDisplayAppId2Info);
        }
        if ( (iBoard != null) && (castHelper instanceof CastHelper) ) {
            iBoard.setCastHelper( (CastHelper) castHelper);
        }
        setModelForCast(matchModel);
    }

    public void handleMessageFromCast(JSONObject joMessage) {
        // TODO: present choice: e.g for layout, or simply show a message
    }
    private void initCastMenu() {
        if ( castHelper == null ) {
            Log.w(TAG, "initCastMenu SKIPPED");
            return;
        }

        castHelper.initCastMenu(this, mainMenu);
        PreferenceValues.doesUserHavePermissionToCast(this, "Any device", true);
    }
    private void setModelForCast(Model matchModel) {
        if ( castHelper == null || matchModel == null ) {
            //Log.w(TAG, "[setModelForCast] castHelper:" + castHelper + ", matchModel:" + matchModel);
            return;
        }
        //Log.v(TAG, "setModelForCast");
        castHelper.setModelForCast(matchModel);
    }

    private void castColors(Map<ColorPrefs.ColorTarget, Integer> mColors) {
        if ( castHelper == null ) { return; }
        castHelper.castColors(mColors);
    }
    public void castDurationChronos() {
        if ( castHelper == null ) { return; }
        castHelper.castDurationChronos();
    }
    public void castGamesWonAppearance() {
        if ( castHelper == null ) { return; }
        castHelper.castGamesWonAppearance();
    }

    private void onActivityStart_Cast() {
        if ( castHelper == null ) { return; }
        castHelper.onActivityStart_Cast();
    }
    private void onActivityStop_Cast() {
        if ( castHelper == null ) { return; }
        castHelper.onActivityStop_Cast();
    }
    private void onActivityResume_Cast() {
        if ( castHelper == null || matchModel == null ) {
            Log.w(TAG, String.format("onActivityResume_Cast SKIPPED (helper=%s, model=%s)", castHelper, matchModel));
            return;
        }
        castHelper.initCasting(this);
        castHelper.onActivityResume_Cast();
    }
    private void onActivityPause_Cast() {
        if ( castHelper == null ) { return; }
        castHelper.onActivityPause_Cast();
    }

    // ----------------------------------------------------
    // --------------------- speech -----------------------
    // ----------------------------------------------------

    private Speak m_speak = null;
    private void onResumeSpeak() {
        if ( m_speak != null ) { return; }
        m_speak = Speak.getInstance();
        m_speak.start(this); // invokes listener.onInit()
    }

    private void cleanup_Speak() {
        if ( m_speak != null ) {
            m_speak.stop();
        }
    }
    private void speakScore() {
        if ( (m_speak != null) && (matchModel != null) ) {
            m_speak.playAllDelayed(-1);
        }
    }

    // ----------------------------------------------------
    // --- control via bluetooth media player buttons -----
    // ----------------------------------------------------
    private static Map<Integer, String> msMediaPlayBackDesc = new HashMap<>();
    static {
        msMediaPlayBackDesc.put(KeyEvent.KEYCODE_MEDIA_PLAY        , "KEYCODE_MEDIA_PLAY          ");
        msMediaPlayBackDesc.put(KeyEvent.KEYCODE_MEDIA_PAUSE       , "KEYCODE_MEDIA_PAUSE         ");
        msMediaPlayBackDesc.put(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE  , "KEYCODE_MEDIA_PLAY_PAUSE    ");
        msMediaPlayBackDesc.put(KeyEvent.KEYCODE_MEDIA_STOP        , "KEYCODE_MEDIA_STOP          ");
        msMediaPlayBackDesc.put(KeyEvent.KEYCODE_MEDIA_CLOSE       , "KEYCODE_MEDIA_CLOSE         ");
        msMediaPlayBackDesc.put(KeyEvent.KEYCODE_MEDIA_EJECT       , "KEYCODE_MEDIA_EJECT         ");
        msMediaPlayBackDesc.put(KeyEvent.KEYCODE_MEDIA_RECORD      , "KEYCODE_MEDIA_RECORD        ");
        msMediaPlayBackDesc.put(KeyEvent.KEYCODE_MEDIA_AUDIO_TRACK , "KEYCODE_MEDIA_AUDIO_TRACK   ");
        msMediaPlayBackDesc.put(KeyEvent.KEYCODE_MEDIA_TOP_MENU    , "KEYCODE_MEDIA_TOP_MENU      ");
        msMediaPlayBackDesc.put(KeyEvent.KEYCODE_MUTE              , "KEYCODE_MUTE                ");
        msMediaPlayBackDesc.put(KeyEvent.KEYCODE_HEADSETHOOK       , "KEYCODE_HEADSETHOOK         ");

        msMediaPlayBackDesc.put(KeyEvent.KEYCODE_MEDIA_REWIND       , "KEYCODE_MEDIA_REWIND       ");
        msMediaPlayBackDesc.put(KeyEvent.KEYCODE_MEDIA_SKIP_BACKWARD, "KEYCODE_MEDIA_SKIP_BACKWARD");
        msMediaPlayBackDesc.put(KeyEvent.KEYCODE_MEDIA_STEP_BACKWARD, "KEYCODE_MEDIA_STEP_BACKWARD");
        msMediaPlayBackDesc.put(KeyEvent.KEYCODE_MEDIA_PREVIOUS     , "KEYCODE_MEDIA_PREVIOUS     ");

        msMediaPlayBackDesc.put(KeyEvent.KEYCODE_MEDIA_SKIP_FORWARD , "KEYCODE_MEDIA_SKIP_FORWARD ");
        msMediaPlayBackDesc.put(KeyEvent.KEYCODE_MEDIA_STEP_FORWARD , "KEYCODE_MEDIA_STEP_FORWARD ");
        msMediaPlayBackDesc.put(KeyEvent.KEYCODE_MEDIA_NEXT         , "KEYCODE_MEDIA_NEXT         ");
        msMediaPlayBackDesc.put(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD , "KEYCODE_MEDIA_FAST_FORWARD ");
    }
    private MediaSession ms = null;
    private void onPause_BluetoothMediaControlButtons() {
        if ( ms != null ) {
            ms.release();
            ms = null;
        }
    }
    private boolean onResume_BluetoothMediaControlButtons() {
        if ( isWearable() ) { return false; }

        boolean bInitialize = PreferenceValues.initializeForScoringWithMediaControlButtons(this);
        if ( bInitialize == false ) { return false; }

        // https://stackoverflow.com/questions/54414333/mediasession-onmediabuttonevent-works-for-a-few-seconds-then-quits-android
        // work if bluetooth connection is already established

        if ( ms == null ) {
            ms = new MediaSession(getApplicationContext(), getPackageName());

            // this is required or else some for some devices some buttons presses don't make it here (e.g. Plantronic headphone)
            PlaybackState.Builder mStateBuilder = new PlaybackState.Builder()
                    .setActions( PlaybackState.ACTION_PLAY               | PlaybackState.ACTION_PAUSE
                               | PlaybackState.ACTION_STOP               | PlaybackState.ACTION_PLAY_PAUSE
                               | PlaybackState.ACTION_SKIP_TO_PREVIOUS   | PlaybackState.ACTION_REWIND
                               | PlaybackState.ACTION_SKIP_TO_NEXT       | PlaybackState.ACTION_FAST_FORWARD
                               | PlaybackState.ACTION_SEEK_TO
//                             | PlaybackState.ACTION_PLAY_FROM_MEDIA_ID | PlaybackState.ACTION_PREPARE_FROM_MEDIA_ID
                               );
//          mStateBuilder.setState(PlaybackState.STATE_PLAYING, 0, 1);
            PlaybackState playbackState = mStateBuilder.build();
            ms.setPlaybackState(playbackState);
//          ms.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS); // no longer required according to documentation

            ms.setCallback(new MediaSession.Callback() {
                boolean bHandleNextDown = true;
                @Override public boolean onMediaButtonEvent(Intent mediaButtonIntent) {
                    Bundle   extras   = mediaButtonIntent.getExtras();
                    KeyEvent keyEvent = (KeyEvent) extras.get(Intent.EXTRA_KEY_EVENT);
                    int      keyCode  = keyEvent.getKeyCode();

                    String sUpDown = keyEvent.getAction() == KeyEvent.ACTION_UP ? "up" : "down";
                    String sDesc   = msMediaPlayBackDesc.get(keyCode);
                    String sMsg    = String.format("[onMediaButtonEvent] %s %s (%d)", sDesc, sUpDown, keyCode);
                    Log.i(TAG, sMsg);
                    if ( PreferenceValues.currentDateIsTestDate() ) {
                        //Toast.makeText(ScoreBoard.this, sMsg,Toast.LENGTH_LONG ).show();
                    }
                    if ( keyEvent.getAction() == KeyEvent.ACTION_DOWN ) {
                        switch (keyCode) {
                            case KeyEvent.KEYCODE_MEDIA_STOP:
                            case KeyEvent.KEYCODE_MEDIA_PLAY:
                            case KeyEvent.KEYCODE_MEDIA_PAUSE: // only triggered for down not for up? Seen e.g. on my Trust music cube
                            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                                // often the same button
                                if ( isDialogShowing() ) {
                                    try {
                                        //dialogManager.baseDialog.handleButtonClick(DialogInterface.BUTTON_NEGATIVE); // don't: sometimes result in yet another dialog: e.g. MatchFormat
                                        dialogManager.baseDialog.dismiss();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    return true;
                                } else {
                                    handleMenuItem(R.id.dyn_undo_last);
                                }
                                return true;
                            case KeyEvent.KEYCODE_MEDIA_REWIND:       // can often be triggered by long pressing 'previous'
                            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                            case KeyEvent.KEYCODE_MEDIA_SKIP_BACKWARD:
                            case KeyEvent.KEYCODE_MEDIA_STEP_BACKWARD:
                            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD: // can often be triggered by long pressing 'next'
                            case KeyEvent.KEYCODE_MEDIA_NEXT:
                            case KeyEvent.KEYCODE_MEDIA_SKIP_FORWARD:
                            case KeyEvent.KEYCODE_MEDIA_STEP_FORWARD:
                                if ( bHandleNextDown ) {
                                    bHandleNextDown = false;
                                    if ( isDialogShowing() ) {
                                        try {
                                            dialogManager.baseDialog.handleButtonClick(DialogInterface.BUTTON_POSITIVE);
                                            dialogManager.baseDialog.dismiss();
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                        return true;
                                    } else {
                                        boolean bIsBack = (keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS) || (keyCode == KeyEvent.KEYCODE_MEDIA_REWIND)|| (keyCode == KeyEvent.KEYCODE_MEDIA_SKIP_BACKWARD)|| (keyCode == KeyEvent.KEYCODE_MEDIA_STEP_BACKWARD);
                                        Player player = bIsBack ? Player.A : Player.B;
                                        handleMenuItem(R.id.pl_change_score, player);
                                        return true;
                                    }
                                }
                            default:
                                Log.i(TAG, "[onMediaButtonEvent] Not handling keycode " + keyCode);
                        }
                    } else if ( keyEvent.getAction() == KeyEvent.ACTION_UP ) {
                        bHandleNextDown = true;
                        // up is only triggered for 'short' press. Long press means something different?!
                    }
                    return super.onMediaButtonEvent(mediaButtonIntent);
                }

/*
                @Override public void onSkipToNext() {
                    Log.i(TAG, "[onSkipToNext]"); // typically invoked between single-down-followed-by-an-up sequence
                    super.onSkipToNext();
                }
                @Override public void onSkipToPrevious() {
                    Log.i(TAG, "[onSkipToPrevious]");
                    super.onSkipToPrevious();
                }
                @Override public void onPlay() {
                    Log.i(TAG, "[onPlay]");
                    super.onPlay();
                }
                @Override public void onPause() {
                    Log.i(TAG, "[onPause]");
                    super.onPause();
                }
                @Override public void onFastForward() {
                    Log.i(TAG, "[onFastForward]");
                    super.onFastForward();
                }
                @Override public void onRewind() {
                    Log.i(TAG, "[onRewind]");
                    super.onRewind();
                }
*/
            });

            ms.setActive(true);
        }

        try {
            // play dummy audio: todo: redo this every x seconds?
            AudioTrack at = new AudioTrack( AudioManager.STREAM_MUSIC
                    , 48000
                    , AudioFormat.CHANNEL_OUT_STEREO
                    , AudioFormat.ENCODING_PCM_16BIT
                    , AudioTrack.getMinBufferSize(48000, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT)
                    , AudioTrack.MODE_STREAM
            );
            at.play();

            // a little sleep
            at.stop();
            at.release();
        } catch (Exception e) {
            Log.e(TAG, "Why was this again?", e);
        }

        return true;
    }

    // ----------------------------------------------------
    // --- control via Firebase/Pusher messages       -----
    // ----------------------------------------------------
    private void onResumeFCM() {
        if ( ViewUtil.isWearable(this)  ) { return; }

        boolean bStopStartServicesIfRequired = true;
        if ( bStopStartServicesIfRequired ) {
            Intent intent = new Intent(this, PusherMessagingService.class);
            try {
                if ( PreferenceValues.isFCMEnabled(this) ) {
                    Log.d(TAG, "Starting service " + PusherMessagingService.class.getName());
                    ComponentName componentName = this.startService(intent);
                } else {
                    boolean bStopping = this.stopService(intent);
                    Log.d(TAG, "Stopping service " + PusherMessagingService.class.getName() + " " + bStopping); // typically false on startup of app
                }
            } catch (Exception e) {
                Log.w(TAG, "Unable to start/stop service via 'start/stopService()");
                e.printStackTrace();
            }
        }

        PusherHandler pusherHandler = PusherHandler.getInstance();
        if ( PreferenceValues.isFCMEnabled(this) && (Brand.getUUIDPusher() != null) ) {
            try {
                pusherHandler.init(this, Brand.getUUIDPusher(), matchModel.getName(Player.A) + matchModel.getName(Player.B));
                // TODO: for brands with dialogs... suppress them as much as possible, auto-end game etc...
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "FCM initialization failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            pusherHandler.cleanup();
        }
    }
    // ----------------------------------------------------
    // --- in-app purchases / Billing                 -----
    // ----------------------------------------------------


    private BillingProcessor m_billingProcessor = null;
    private String m_sProductToByAfterInit = null;

    private boolean setUpBillingProcessor() {
        String sBrandBillingLicenseKey = Brand.getBillingPublicKey();
        if ( StringUtil.isEmpty(sBrandBillingLicenseKey) ) { return false; }
        if ( m_billingProcessor == null ) {
            m_billingProcessor = new BillingProcessor(this, sBrandBillingLicenseKey, new BillingProcessor.IBillingHandler() {
                /** Called when requested PRODUCT ID was successfully purchased */
                @Override public void onProductPurchased(@NonNull String productId, @Nullable PurchaseInfo details) {
                    Log.d(TAG, String.format("product id %s was successfully purchased", productId));
                }

                /** Called when purchase history was restored and the list of all owned PRODUCT ID's was loaded from Google Play */
                @Override public void onPurchaseHistoryRestored() {
                    Log.d(TAG, "onPurchaseHistoryRestored has been called");
                }

                /**
                 * Called when some error occurred. See Constants class for more details.
                 * Note - this includes handling the case where the user canceled the buy dialog:
                 * errorCode = Constants.BILLING_RESPONSE_RESULT_USER_CANCELED
                 */
                @Override public void onBillingError(int errorCode, @Nullable Throwable error) {
                    Log.d(TAG, "onBillingError has been called: " + errorCode);
                    if ( error != null ) {
                        // 5 returned twice if still 'agree' is required
                        // 1 returned if buy dialog cancelled
                        // 1 returned if 'agree' dialog cancelled
                        Log.e(TAG, String.format("error %d : %s", errorCode, error) );
                        //Toast.makeText(ScoreBoard.this, error.toString(), Toast.LENGTH_LONG).show();
                    }
                }

                /** Called when BillingProcessor was initialized and it's ready to purchase */
                @Override public void onBillingInitialized() {
                    Log.d(TAG, "onBillingInitialized has been called");
                    m_billingProcessor.loadOwnedPurchasesFromGoogleAsync(new BillingProcessor.IPurchasesResponseListener() {
                        @Override public void onPurchasesSuccess() {
                            Log.i(TAG, "loadOwnedPurchasesFromGoogleAsync onPurchasesSuccess");
                            if ( m_sProductToByAfterInit != null ) {
                                purchaseProduct(m_sProductToByAfterInit);
                            }
                        }

                        @Override public void onPurchasesError() {
                            Log.w(TAG, "loadOwnedPurchasesFromGoogleAsync onPurchasesError");
                        }
                    });
                }
            });
            m_billingProcessor.initialize();
            return true;
        }
        return false;
    }
    private void purchaseProduct(String sProductId) {
        if ( m_sProductToByAfterInit == null ) {
            m_sProductToByAfterInit = sProductId;
            setUpBillingProcessor();
            return;
        }
        if ( m_billingProcessor == null ) {
            Toast.makeText(this, "Billing not configured for this app", Toast.LENGTH_LONG).show();
            return;
        }
        if ( m_billingProcessor.purchase(this, sProductId) ) {
            Log.d(TAG, String.format("Purchase of %s initiated", sProductId));
        } else {
            Log.w(TAG, String.format("Could not start of purchasing %s", sProductId));
        }
    }
    private boolean hasPurchasedProduct(String sProductId) {
        if ( m_billingProcessor == null ) { return false; }
        return m_billingProcessor.isPurchased(sProductId);
    }
    /** call this method to undo the payment and restore the free version */
    private void resetPayment(String sProductId) {
        if ( m_billingProcessor == null ) { return; }
        m_billingProcessor.consumePurchaseAsync(sProductId, new BillingProcessor.IPurchasesResponseListener() {
            @Override public void onPurchasesSuccess() {
                Log.i(TAG, "consumePurchaseAsync onPurchasesSuccess");
            }

            @Override public void onPurchasesError() {
                Log.w(TAG, "consumePurchaseAsync onPurchasesError");
            }
        });
    }
    private void destroyBillingProcessor() {
        if ( m_billingProcessor != null ) {
            m_billingProcessor.release();
            m_billingProcessor = null;
        }
    }
}
