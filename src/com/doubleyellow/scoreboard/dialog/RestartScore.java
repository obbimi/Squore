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
import android.content.Intent;
import android.os.Bundle;
import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.activity.IntentKeys;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.match.Match;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.model.Player;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;

/**
 * Dialog that is shown if user wants to restart the score.
 */
public class RestartScore extends BaseAlertDialog
{
    public RestartScore(Context context, Model matchModel, ScoreBoard scoreBoard) {
        super(context, matchModel, scoreBoard);
    }

    @Override public boolean storeState(Bundle outState) {
        return true;
    }

    @Override public boolean init(Bundle outState) {
        return true;
    }
    @Override public void show() {
        if ( isNotWearable()
            && ! scoreBoard.m_iMenuItemsToHide.contains(R.id.change_match_format)
        ) {
            adb.setNeutralButton (R.string.cmd_change_format, dialogClickListener);
        }
        String sMessage = getString(R.string.sb_clear_score_confirm_message);
        if ( isNotWearable() ) {
            sMessage += " " + getString(R.string.sb_clear_score_confirm_message_2);
        }
        adb.setPositiveButton(R.string.cmd_yes          , dialogClickListener)
           .setNegativeButton(R.string.cmd_no           , dialogClickListener)
           .setIcon          (R.drawable.circle_2arrows)
           .setMessage       (sMessage)
           .setOnKeyListener(getOnBackKeyListener(BTN_NO_RESTART));
        dialog = adb.show();
    }

    private DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> handleButtonClick(which);

    public static final int BTN_RESTART              = DialogInterface.BUTTON_POSITIVE;
    public static final int BTN_RESTART_CHANGEFORMAT = DialogInterface.BUTTON_NEUTRAL;
    public static final int BTN_NO_RESTART           = DialogInterface.BUTTON_NEGATIVE;
    @Override public void handleButtonClick(int which) {
        dialog.dismiss();
        switch ( which ) {
            case BTN_RESTART:
                scoreBoard.restartScore();
                break;
            case BTN_RESTART_CHANGEFORMAT:
                //boolean bAutoShowTimer = PreferenceValues.showTimersAutomatically(context);
                Intent nm = new Intent(context, Match.class);
                Model m = Brand.getModel();
                for(Player p: Player.values()) {
                    m.setPlayerName   (p, matchModel.getName    (p, true, true) );
                    m.setPlayerCountry(p, matchModel.getCountry (p) );
                    m.setPlayerClub   (p, matchModel.getClub    (p) );
                    m.setPlayerColor  (p, matchModel.getColor   (p) );
                    m.setPlayerId     (p, matchModel.getPlayerId(p) );
                }

                m.setSource(matchModel.getSource(), matchModel.getSourceID() );
                m.setAdditionalPostParams(matchModel.getAdditionalPostParams());

                nm.putExtra(IntentKeys.NewMatch.toString(), m.toJsonString(null));

                scoreBoard.startActivityForResult(nm, 1);
                break;
            case BTN_NO_RESTART:
                break;
        }
        scoreBoard.triggerEvent(ScoreBoard.SBEvent.restartScoreDialogEnded, this);
    }
}
