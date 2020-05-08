/*
 * Copyright (C) 2018  Iddo Hoeve
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

import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;

/** Introduced for the Expedite system */
public class ActivateMode extends BaseAlertDialog
{
    public ActivateMode(Context context, Model matchModel, ScoreBoard scoreBoard) {
        super(context, matchModel, scoreBoard);
    }

    @Override public boolean storeState(Bundle outState) {
        //outState.putSerializable(ActivateMode.class.getSimpleName(), m_sMode);
        return true;
    }

    @Override public boolean init(Bundle outState) {
        //init((String) outState.getSerializable(ActivateMode.class.getSimpleName()));
        return true;
    }

    //private String m_sMode = null;

    @Override public void show() {
        String sTitle = getString(R.string.activate_mode__Tabletennis);
        String sDesc  = getString(R.string.activate_mode_description__Tabletennis, PreferenceValues.showModeDialogAfterXMins(context));
        adb.setTitle(sTitle + "?")
           .setMessage(sDesc)
           .setIcon   (R.drawable.microphone)
           .setPositiveButton(R.string.cmd_yes, listener)
           .setNegativeButton(R.string.cmd_no , listener);
        dialog = adb.show();
    }

/*
    public void init(String sMode) {
        this.m_sMode = sMode;
    }
*/

    private DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
        @Override public void onClick(DialogInterface dialog, int which) {
            handleButtonClick(which);
        }
    };

    public static final int BTN_ACTIVATE        = DialogInterface.BUTTON_POSITIVE;
    public static final int BTN_DO_NOT_ACTIVATE = DialogInterface.BUTTON_NEGATIVE;
    @Override public void handleButtonClick(int which) {
        switch (which) {
            case BTN_ACTIVATE :
                scoreBoard.handleMenuItem(R.id.tt_activate_mode/*, m_sMode*/);
                break;
            case BTN_DO_NOT_ACTIVATE   :
                break;
        }
    }
}
