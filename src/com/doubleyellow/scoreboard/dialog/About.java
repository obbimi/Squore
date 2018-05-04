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
import android.os.Build;
import android.os.Bundle;
import com.doubleyellow.android.view.ViewUtil;
import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.main.DialogManager;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;

/**
 * Dialog that simply shows a little info about the app.
 * One of the dialog buttons is configured so it can be used to go to the Change Log of the app.
 */
public class About extends BaseAlertDialog
{
    public About(Context context, Model matchModel, ScoreBoard scoreBoard) {
        super(context, matchModel, scoreBoard);
    }

    @Override public boolean storeState(Bundle outState) {
        return true;
    }

    @Override public boolean init(Bundle outState) {
        return true;
    }
    @Override public void show() {
        String msg = String.format("App Version: %s (%s)\n", PreferenceValues.getAppVersionCode(context), PreferenceValues.getAppVersionNameShort(context))
                   + String.format("By: %s\n", getString(R.string.developer))
                   + String.format("API: %s\n", Build.VERSION.SDK_INT) // does not say anything about the app but the android version the device is running on
                     ;

        dialog = adb
                .setMessage(msg)
                .setTitle         (Brand.getShortName(context))
                .setPositiveButton("Change Log", dialogClickListener)
                .setNeutralButton ("Credits"   , dialogClickListener)
                .setNegativeButton(R.string.cmd_cancel, null)
              //.setIcon(R.drawable.logo)
                .setOnKeyListener(getOnBackKeyListener())
                .show();
        ViewUtil.setPackageIcon(context, dialog);
    }

    private DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
        @Override public void onClick(DialogInterface dialog, int which) {
            handleButtonClick(which);
        }
    };

    @Override public void handleButtonClick(int which) {
        switch (which){
            case DialogInterface.BUTTON_POSITIVE:
                scoreBoard.handleMenuItem(R.id.sb_change_log);
                break;
            case DialogInterface.BUTTON_NEUTRAL:
                scoreBoard.handleMenuItem(R.id.sb_credits);
                break;
        }
        //scoreBoard.triggerEvent(ScoreBoard.SBEvent.dialogClosed, this);
        showNextDialog();
    }
}
