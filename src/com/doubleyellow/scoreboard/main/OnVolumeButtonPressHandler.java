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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.dialog.MyDialogBuilder;
import com.doubleyellow.scoreboard.model.Player;
import com.doubleyellow.scoreboard.prefs.PreferenceKeys;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.scoreboard.prefs.VolumeKeysBehaviour;
import com.doubleyellow.scoreboard.vico.IBoard;

class OnVolumeButtonPressHandler extends ScoreBoardListener {
    ScoreBoard scoreBoard = null;

    OnVolumeButtonPressHandler(ScoreBoard scoreBoard) {
        super(scoreBoard);
    }
    //private long lastPress = 0L;
    private int iDialogPresentedCnt = 0;

    boolean handle(final Context context, boolean bVolumeTrueIsUpFalseIsDown, boolean bActionTrueIsUpFalseIsDown) {

        boolean bUseVolumeButtonsForScoring = false;
        VolumeKeysBehaviour volumeKeysBehaviour = PreferenceValues.volumeKeysBehaviour(context);
        switch (volumeKeysBehaviour) {
            case None:
                break;
            case AdjustScore:
                bUseVolumeButtonsForScoring = true;
                break;
            case AdjustScore__ForPortraitOnly:
                bUseVolumeButtonsForScoring = scoreBoard.isPortrait();
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
        if ( scoreBoard.isLandscape() ) { return; }

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
