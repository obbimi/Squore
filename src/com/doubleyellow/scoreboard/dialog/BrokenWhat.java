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
import android.view.LayoutInflater;
import android.view.View;
import com.doubleyellow.android.view.SelectEnumView;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.model.BrokenEquipment;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.model.Player;

public class BrokenWhat extends BaseAlertDialog
{
    public BrokenWhat(Context context, Model matchModel, ScoreBoard scoreBoard) {
        super(context, matchModel, scoreBoard);
    }

    @Override public boolean storeState(Bundle outState) {
        outState.putSerializable(Player.class.getSimpleName(), affectedPlayer);
        return true;
    }

    @Override public boolean init(Bundle outState) {
        init((Player) outState.getSerializable(Player.class.getSimpleName()));
        return true;
    }

    private Player affectedPlayer = null;
    private SelectEnumView<BrokenEquipment> sv;

    @Override public void show() {
        String sTitle = getString(R.string.lbl_broken_equipment, matchModel.getName_no_nbsp(affectedPlayer, false));
        adb     .setTitle(sTitle)
                .setIcon   (R.drawable.ic_action_bad)
                //.setMessage(R.string.oa_decision_colon)
                .setPositiveButton(R.string.cmd_resume, listener);

        LayoutInflater myLayout = LayoutInflater.from(context);
        final View view = myLayout.inflate(R.layout.broken_equipement, null);
        sv = (SelectEnumView) view.findViewById(R.id.selectBrokenEquipment);

        // add a view with all possible Conducts and let user choose one
        //sv = new SelectEnumView(context, BrokenEquipment.class);
        adb.setView(view);

        dialog = adb.show();
    }

    public void init(Player appealingPlayer) {
        this.affectedPlayer = appealingPlayer;
    }

    private DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
        @Override public void onClick(DialogInterface dialog, int which) {
            handleButtonClick(which);
        }
    };

    public static final int BTN_RESUME_PLAY  = DialogInterface.BUTTON_POSITIVE;
    @Override public void handleButtonClick(int which) {
        BrokenEquipment broken = sv.getChecked();
        switch (which) {
            case BTN_RESUME_PLAY : break;
        }
        matchModel.recordBroken(affectedPlayer, broken);
    }
}
