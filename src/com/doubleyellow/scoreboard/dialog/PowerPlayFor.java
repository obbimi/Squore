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

import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.model.Player;

/**
 * For activating powerplay
 */
public class PowerPlayFor extends BaseAlertDialog
{
    public PowerPlayFor(Context context, Model matchModel, ScoreBoard scoreBoard) {
        super(context, matchModel, scoreBoard);
    }

    @Override public boolean storeState(Bundle outState) {
        return true;
    }

    @Override public boolean init(Bundle outState) {
        return true;
    }

    @Override public void show() {
        String sPlayerA = matchModel.getName(Player.A);
        String sPlayerB = matchModel.getName(Player.B);
        int iLeftForA = matchModel.getNrOfPowerPlaysLeftFor(Player.A);
        int iLeftForB = matchModel.getNrOfPowerPlaysLeftFor(Player.B);

        String sTitle = getString(R.string.lbl_power_play);
        adb     .setTitle(sTitle)
                //.setIcon   (R.drawable.ic_action_bad)
                .setMessage(R.string.lbl_activate_for)
                ;
        if ( iLeftForA > 0 ) {
            setPositiveButton(sPlayerA + " (#" + iLeftForA + ")", listener);
        }
        if ( iLeftForA > 0 && iLeftForB > 0 ) {
            setNeutralButton (R.string.lbl_both   , listener);
        } else if ( iLeftForA + iLeftForB == 0 ) {
            setMessage(getString(R.string.no_more_power_plays_left__x, matchModel.getNrOfPowerPlaysPerMatch()));
            setNeutralButton (R.string.cmd_cancel, null);
        }
        if ( iLeftForB > 0 ) {
            setNegativeButton(sPlayerB + " (#" + iLeftForB + ")", listener);
        }

        //LayoutInflater myLayout = LayoutInflater.from(context);
        dialog = adb.show();
    }

    private DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
        @Override public void onClick(DialogInterface dialog, int which) {
            handleButtonClick(which);
        }
    };

    public static final int BTN_A    = DialogInterface.BUTTON_POSITIVE;
    public static final int BTN_B    = DialogInterface.BUTTON_NEGATIVE;
    public static final int BTN_BOTH = DialogInterface.BUTTON_NEUTRAL;
    @Override public void handleButtonClick(int which) {
        switch (which) {
            case BTN_BOTH:
                matchModel.markNextRallyAsPowerPlayFor(Player.A);
                matchModel.markNextRallyAsPowerPlayFor(Player.B);
                break;
            case BTN_A :
                matchModel.markNextRallyAsPowerPlayFor(Player.A);
                break;
            case BTN_B :
                matchModel.markNextRallyAsPowerPlayFor(Player.B);
                break;
        }
    }
}
