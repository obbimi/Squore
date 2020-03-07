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

package com.doubleyellow.scoreboard.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import com.doubleyellow.scoreboard.main.*;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.model.Player;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;

public class ChangeSides extends BaseAlertDialog
{
    public ChangeSides(Context context, Model matchModel, ScoreBoard scoreBoard) {
        super(context, matchModel, scoreBoard);
    }

    @Override public boolean storeState(Bundle outState) {
        outState.putSerializable(Player.class.getSimpleName(), leadingPlayer);
        return true;
    }

    @Override public boolean init(Bundle outState) {
        init((Player) outState.getSerializable(Player.class.getSimpleName()));
        return true;
    }

    private Player leadingPlayer = null;

    @Override public void show() {

        String sTitle = getOAString(R.string.oa_change_sides);
        adb.setTitle(sTitle)
           .setIcon   (R.drawable.microphone)
           .setMessage(getString(R.string.sb_swap_sides) + "?")
           .setPositiveButton(R.string.cmd_yes, listener)
           .setNegativeButton(R.string.cmd_no , listener);
        adb.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override public void onDismiss(DialogInterface dialog) {
                scoreBoard.triggerEvent(ScoreBoard.SBEvent.sideTossDialogEnded, ChangeSides.this);
            }
        });
        dialog = adb.show();
    }
    private String getOAString(int iResId) {
        return PreferenceValues.getOAString(context, iResId );
    }

    public void init(Player leadingPlayer) {
        this.leadingPlayer = leadingPlayer;
    }

    private DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
        @Override public void onClick(DialogInterface dialog, int which) {
            handleButtonClick(which);
        }
    };

    public static final int BTN_SWAP_PLAYERS        = DialogInterface.BUTTON_POSITIVE;
    public static final int BTN_DO_NOT_SWAP_PLAYERS = DialogInterface.BUTTON_NEGATIVE;
    @Override public void handleButtonClick(int which) {
        switch (which) {
            case BTN_SWAP_PLAYERS :
                scoreBoard.handleMenuItem(R.id.sb_swap_sides);
                break;
            case BTN_DO_NOT_SWAP_PLAYERS   :
                break;
        }
    }
}
