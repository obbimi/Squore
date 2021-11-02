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
import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.prefs.PreferenceKeys;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.util.Feature;
import com.doubleyellow.util.ListUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Dialog that is shown when 'end of a game' menu/button is chosen while the score is not a valid 'enf of game' score.
 */
public class EndGameChoice extends BaseAlertDialog
{
    public EndGameChoice(Context context, Model matchModel, ScoreBoard scoreBoard) {
        super(context, matchModel, scoreBoard);
    }

    @Override public boolean storeState(Bundle outState) {
        return true;
    }

    @Override public boolean init(Bundle outState) {
        return true;
    }
    @Override public void show() {
        //int iEndGameMsgResId = R.string.sb_start_next_game_confirm_message;
        int iResId = PreferenceValues.getSportTypeSpecificResId(context, R.string.end_game__Default);

        adb.setPositiveButton(iResId             , dialogClickListener)
           .setNegativeButton(R.string.cmd_cancel, dialogClickListener)
           .setIcon(R.drawable.microphone);
        List<String> messages = new ArrayList<>();
        messages.add(getGameOrSetString(R.string.sb_not_a_valid_endscore));
        if ( Brand.isSquash() ) {
            messages.add(getGameOrSetString(R.string.sb_did_you_mean_conduct_game));
        }
        adb.setTitle(messages.remove(0))
           .setMessage(ListUtil.join(messages, "\n\n"))
           .setOnKeyListener(getOnBackKeyListener());

        dialog = adb.show();
    }

    private DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
        @Override public void onClick(DialogInterface dialog, int which) {
            handleButtonClick(which);
        }
    };

    public static final int BTN_END_GAME      = DialogInterface.BUTTON_POSITIVE;
    public static final int BTN_CANCEL        = DialogInterface.BUTTON_NEGATIVE;
    @Override public void handleButtonClick(int which) {
        dialog.dismiss();
        switch ( which ) {
            case BTN_END_GAME:
                PreferenceValues.setOverwrite(PreferenceKeys.endGameSuggestion, Feature.DoNotUse.toString());
                scoreBoard.endGame(true);
                PreferenceValues.removeOverwrite(PreferenceKeys.endGameSuggestion);
                break;
            case BTN_CANCEL:
                break;
        }
        scoreBoard.triggerEvent(ScoreBoard.SBEvent.endGameDialogEnded, this);
    }
}
