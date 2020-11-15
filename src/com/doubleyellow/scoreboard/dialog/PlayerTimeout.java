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
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.doubleyellow.android.view.ViewUtil;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.model.Player;
import com.doubleyellow.scoreboard.prefs.ColorPrefs;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.util.StringUtil;

import java.util.EnumSet;

public class PlayerTimeout extends BaseAlertDialog
{
    public PlayerTimeout(Context context, Model matchModel, ScoreBoard scoreBoard) {
        super(context, matchModel, scoreBoard);
    }

    @Override public boolean storeState(Bundle outState) {
        return true;
    }

    @Override public boolean init(Bundle outState) {
        return true;
    }

    @Override public void show() {

        adb.setTitle(context.getString(R.string.sb_player_timeout_timer))
           .setIcon(R.drawable.timer);

        int screenHeightWidthMinimum = ViewUtil.getScreenHeightWidthMinimum(context);
        llpMargin1Weight1.height       = screenHeightWidthMinimum / 8;
        llpMargin1Weight1.width        = 3 * screenHeightWidthMinimum / 4; // might be fairly long text in language other than English

        llpMargin1Weight1.leftMargin   =
        llpMargin1Weight1.rightMargin  =
        llpMargin1Weight1.topMargin    =
        llpMargin1Weight1.bottomMargin = screenHeightWidthMinimum / 40;

        LinearLayout ll = new LinearLayout(context);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setGravity(Gravity.CENTER);
        for ( Player player: Player.values() ) {
            String sCaption = matchModel.getName(player);
            String sLastTimeoutInfo = matchModel.getLastTimeoutInfo(player);
            if ( StringUtil.isNotEmpty(sLastTimeoutInfo) ) {
                sCaption += " (" + "Last" + " : " + sLastTimeoutInfo + ")";
            }
            TextView view = getActionView(sCaption, player.ordinal(), 0, 0, null);
            ll.addView(view, llpMargin1Weight1);
        }
        adb.setView(ll);

        ColorPrefs.setColor(ll);

        adb.setOnKeyListener(getOnBackKeyListener());
        dialog = adb.show();
    }

    @Override public void handleButtonClick(int which) {
        // TODO: do not accept if this player already has had a timeout
        Player player = Player.values()[which];
        scoreBoard.triggerEvent(ScoreBoard.SBEvent.playerTimeoutClosed, player);
    }
}
