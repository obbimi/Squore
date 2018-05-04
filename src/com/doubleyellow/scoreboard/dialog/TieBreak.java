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

import android.util.SparseIntArray;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.model.Player;
import com.doubleyellow.scoreboard.model.TieBreakFormat;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.util.StringUtil;

/**
 * Dialog presented to the user to
 * - just let him anounce the tie-break
 * - or, if required, allowes referee to enter the choice of one of the players (usually the receiver) about how many more points to play
 */
public class TieBreak extends BaseAlertDialog
{
    public TieBreak(Context context, Model matchModel, ScoreBoard scoreBoard) {
        super(context, matchModel, scoreBoard);
    }

    @Override public boolean storeState(Bundle outState) {
        outState.putInt(TieBreak.class.getSimpleName(), m_iOccurrenceCount);
        return true;
    }

    @Override public boolean init(Bundle outState) {
        //show(false); // deliberatly not triggered here: when 10-10 is reached and screen is rotated... it will show because onCreate will triggers it again
        init(outState.getInt(TieBreak.class.getSimpleName(), 1));
        return true;
    }

    public void init(int iOccurrenceCount) {
        m_iOccurrenceCount = iOccurrenceCount;
    }

    final SparseIntArray mTranslateButtonToAmount = new SparseIntArray();
    private int m_iOccurrenceCount = 0;
    @Override public void show() {
        TieBreakFormat tiebreakFormat = matchModel.getTiebreakFormat();

        int iPointsEach = matchModel.getScore(Player.A);
        if ( tiebreakFormat.needsTwoClearPoints() ) {
            // simple announcement : x-all, player needs 2 clear points
            adb.setTitle  (PreferenceValues.getOAString(context, R.string.oa_n_all, iPointsEach))
               .setIcon   (R.drawable.microphone)
               .setPositiveButton(R.string.cmd_ok, onClickListener);
            if ( m_iOccurrenceCount <= 1 ) {
                adb.setMessage(PreferenceValues.getOAString(context, R.string.oa_player_needs_2_clear_points));
            }
        } else {
            // let ref indicate the end score (may be choosen by the last player that was ahead before tie-break was reached)
            int    iGameEndsAt   = matchModel.getNrOfPointsToWinGame();
            String sReceiverName = matchModel.getName_no_nbsp(matchModel.getReceiver(), false);
            adb.setTitle  (PreferenceValues.getOAString(context, R.string.sb_tiebreak))
               .setIcon   (R.drawable.microphone);

            int[] iaAdd_SelectOptions = tiebreakFormat.getAddToEndScoreOptions();
            if ( iaAdd_SelectOptions != null ) {
                adb.setMessage(PreferenceValues.getOAString(context, R.string.sb_tiebreak_receiver_chooses_winning_score, iPointsEach, sReceiverName));

                int[] iaButton = new int[] { DialogInterface.BUTTON_POSITIVE, DialogInterface.BUTTON_NEUTRAL, DialogInterface.BUTTON_NEGATIVE };
                for (int i = 0; i < iaAdd_SelectOptions.length; i++) {
                    mTranslateButtonToAmount.put(iaButton[i], iaAdd_SelectOptions[i]);
                }
                for (int i = 0; i < iaAdd_SelectOptions.length; i++) {
                    String text = String.valueOf(iGameEndsAt + iaAdd_SelectOptions[i]);
                    switch (iaButton[i]) {
                        case DialogInterface.BUTTON_POSITIVE: adb.setPositiveButton(text, onClickListener); break;
                        case DialogInterface.BUTTON_NEUTRAL : adb.setNeutralButton (text, onClickListener); break;
                        case DialogInterface.BUTTON_NEGATIVE: adb.setNegativeButton(text, onClickListener); break;
                    }
                }
            } else {
                // show message that it is sudden death??
                adb.setMessage(StringUtil.capitalize(tiebreakFormat))
                   .setNeutralButton (android.R.string.ok, onClickListener);
            }
        }
        if ( tiebreakFormat.needsTwoClearPoints() ) {
            adb.setOnKeyListener(getOnBackKeyListener(DialogInterface.BUTTON_NEUTRAL));
        }
        dialog = adb.show();
    }

    private DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
        @Override public void onClick(DialogInterface dialogInterface, int which) {
            handleButtonClick(which);
        }
    };

    @Override public void handleButtonClick(int which) {
        if ( mTranslateButtonToAmount.size() != 0 ) {
            int iAdd = mTranslateButtonToAmount.get(which);
            matchModel.setTieBreakPlusX(iAdd);
        }
        dialog.dismiss();
        scoreBoard.triggerEvent(ScoreBoard.SBEvent.officialAnnouncementClosed, this);
    }
}
