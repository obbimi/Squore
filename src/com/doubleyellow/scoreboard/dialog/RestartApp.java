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

package com.doubleyellow.scoreboard.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.util.ListUtil;

import java.util.List;

/**
 * Dialog that is shown to let user confirm to close app for restart.
 */
public class RestartApp extends BaseAlertDialog
{
    public RestartApp(Context context, Model matchModel, ScoreBoard scoreBoard) {
        super(context, matchModel, scoreBoard);
    }

    @Override public boolean storeState(Bundle outState) {
        return true;
    }

    @Override public boolean init(Bundle outState) {
        return true;
    }

    private List<String> lMesages = null;
    public void init(List<String> lMesages) {
        this.lMesages = lMesages;
    }
    @Override public void show() {
        String sMsg = ListUtil.join(this.lMesages, "\n");
        adb.setPositiveButton(R.string.cmd_ok           , dialogClickListener)
           .setNegativeButton(R.string.cmd_no           , dialogClickListener)
           .setIcon          (R.drawable.circle_2arrows)
           .setMessage       (sMsg)
           .setOnKeyListener(getOnBackKeyListener(BTN_NO_RESTART));
        dialog = adb.show();
    }

    private final DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> handleButtonClick(which);

    public static final int BTN_RESTART              = DialogInterface.BUTTON_POSITIVE;
    public static final int BTN_NO_RESTART           = DialogInterface.BUTTON_NEGATIVE;
    @Override public void handleButtonClick(int which) {
        dialog.dismiss();
        switch ( which ) {
            case BTN_RESTART:
                scoreBoard.doRestart();
                break;
            case BTN_NO_RESTART:
                break;
        }
        //scoreBoard.triggerEvent(ScoreBoard.SBEvent.??, this);
    }
}
