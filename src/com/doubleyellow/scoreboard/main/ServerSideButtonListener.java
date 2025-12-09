/*
 * Copyright (C) 2025  Iddo Hoeve
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

import android.view.View;
import android.widget.Toast;

import com.doubleyellow.android.handler.OnClickXTimesHandler;
import com.doubleyellow.android.view.ViewUtil;
import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.model.DoublesServe;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.model.Player;
import com.doubleyellow.scoreboard.model.TabletennisModel;
import com.doubleyellow.scoreboard.prefs.BackKeyBehaviour;
import com.doubleyellow.scoreboard.prefs.ColorPrefs;
import com.doubleyellow.scoreboard.prefs.KioskMode;
import com.doubleyellow.scoreboard.prefs.PreferenceKeys;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.scoreboard.vico.IBoard;
import com.doubleyellow.scoreboard.view.PlayersButton;

class ServerSideButtonListener extends ScoreBoardListener implements View.OnClickListener, View.OnLongClickListener
{
    ServerSideButtonListener(ScoreBoard scoreBoard) {
        super(scoreBoard);
    }

    private OnClickXTimesHandler onClickXTimesHandler = null;
    @Override public void onClick(View view) {
        if ( warnModelIsLocked() ) { return; }
        int viewId = getXmlIdOfParent(view);
        Player player = IBoard.m_id2player.get(viewId);
        if ( player == null ) { return; }
        Model matchModel = getMatchModel();
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
                if ( ViewUtil.isWearable(scoreBoard) ) {
                    handleMenuItem(R.id.sb_about);
                    return;
                }
                if ( scoreBoard.mBluetoothAdapter == null ) {
                    Toast.makeText(scoreBoard, R.string.bt_no_bluetooth_on_device, Toast.LENGTH_SHORT).show();
                    return;
                }

                int[] iMenuIds = new int[] { R.id.sb_ble_devices
                        //, R.id.sb_demo
                        //, R.id.sb_toggle_demo_mode
                        //, R.id.sb_download_posted_to_squore_matches
                        //, R.id.android_language
                };
                int iToggled = ViewUtil.setMenuItemsVisibility(scoreBoard.mainMenu, iMenuIds, true);

                if ( scoreBoard.m_bleConfigHandler != null ) {
                    scoreBoard.m_bleConfigHandler.promoteAppToUseBLE();
                }
                KioskMode kioskMode = PreferenceValues.getKioskMode(scoreBoard);
                if ( ! KioskMode.NotUsed.equals(kioskMode) ) {
                    if ( PreferenceValues.hasRemoteSetting(PreferenceKeys.kioskMode) ) {
                        // don't restart if e.g. RemoteSettings url is used. It might go straight back in kioskMode.
                        //PreferenceValues.setKioskMode(scoreBoard, KioskMode.NotUsed);
                        PreferenceValues.setOverwrite(PreferenceKeys.kioskMode, KioskMode.NotUsed.toString());
                        scoreBoard.showInfoMessage("Abandoning kiosk mode...", 5);
                        scoreBoard.reinitMenu();
                    } else {
                        scoreBoard.doRestart();
                    }
                    return;
                }

                if ( iToggled > 1 ) {
                    Toast.makeText(scoreBoard, String.format("Additional %d menu items made available", iToggled), Toast.LENGTH_LONG).show();
                    ScoreBoard.Mode newMode = ScoreBoard.m_mode; // toggleDemoMode(null);
                    if ( newMode.equals(ScoreBoard.Mode.ScreenShots) ) {
                        PreferenceValues.setEnum   (PreferenceKeys.BackKeyBehaviour            , scoreBoard, BackKeyBehaviour.UndoScoreNoConfirm); // for adb demo/screenshots script
                        PreferenceValues.setBoolean(PreferenceKeys.showFullScreen              , scoreBoard, true);                         // for adb demo/screenshots script
                        PreferenceValues.setBoolean(PreferenceKeys.showActionBar               , scoreBoard, false);                        // for adb demo/screenshots script
                        PreferenceValues.setBoolean(PreferenceKeys.showAdjustTimeButtonsInTimer, scoreBoard, false);                        // for cleaner screenshots
                        PreferenceValues.setBoolean(PreferenceKeys.showUseAudioCheckboxInTimer , scoreBoard, false);                        // for cleaner screenshots
                    } else {
                        PreferenceValues.setEnum   (PreferenceKeys.BackKeyBehaviour            , scoreBoard, BackKeyBehaviour.PressTwiceToExit);
                        PreferenceValues.setBoolean(PreferenceKeys.showAdjustTimeButtonsInTimer, scoreBoard, R.bool.showAdjustTimeButtonsInTimer_default);
                        PreferenceValues.setBoolean(PreferenceKeys.showUseAudioCheckboxInTimer , scoreBoard, R.bool.showUseAudioCheckboxInTimer_default);
                    }
                    if ( newMode.equals(ScoreBoard.Mode.Debug) ) {
                        PreferenceValues.setString(PreferenceKeys.FeedFeedsURL, scoreBoard, scoreBoard.getString(R.string.feedFeedsURL_default) + "?suffix=.new");
                        //PreferenceValues.setNumber (PreferenceKeys.viewedChangelogVersion, scoreBoard, PreferenceValues.getAppVersionCode(scoreBoard)-1);
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

        if ( scoreBoard.m_mode.equals(ScoreBoard.Mode.ScreenShots) && pl.equals(Player.B) ) {
            // switch to a different color schema
            int i = PreferenceValues.getInteger(PreferenceKeys.colorSchema, scoreBoard, 0);
            PreferenceValues.setNumber(PreferenceKeys.colorSchema, scoreBoard, i + 1);
            ScoreBoard.getMatchModel().setDirty();
            ColorPrefs.clearColorCache();
            scoreBoard.onRestart();
            return true;
        }
        scoreBoard.showColorPicker(pl);
        return true;
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
}
