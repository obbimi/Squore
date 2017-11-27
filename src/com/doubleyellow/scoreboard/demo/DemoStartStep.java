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

package com.doubleyellow.scoreboard.demo;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.ScrollView;
import com.doubleyellow.android.view.SelectEnumView;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.dialog.BaseAlertDialog;

public class DemoStartStep extends BaseAlertDialog
{
    public DemoStartStep(Context context, Model matchModel, ScoreBoard scoreBoard) {
        super(context, matchModel, scoreBoard);
    }

    @Override public boolean storeState(Bundle outState) {
        return false;
    }

    @Override public boolean init(Bundle outState) {
        return false;
    }

    private SelectEnumView<DemoThread.DemoMessage> sev;

    @Override public void show() {

        // add a view with all possible Conducts and let user choose one
        ScrollView sv = new ScrollView(context);
        sev = new SelectEnumView<DemoThread.DemoMessage>(context, DemoThread.DemoMessage.class);
        sv.addView(sev);

        dialog = adb
                .setTitle("Start where")
                .setPositiveButton(R.string.cmd_ok    , listener)
                .setNegativeButton(R.string.cmd_cancel, listener)
                .setView(sv)
                .show();
    }

    private DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
        @Override public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    DemoThread.DemoMessage message = sev.getChecked();
                    scoreBoard.handleMenuItem(R.id.sb_toggle_demo_mode, message);
                    break;
                case DialogInterface.BUTTON_NEGATIVE:
                    break;
            }
        }
    };
}
