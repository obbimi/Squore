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

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import com.doubleyellow.android.view.ViewUtil;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.model.LockState;
import com.doubleyellow.scoreboard.model.Model;

public class LockedMatch extends BaseAlertDialog
{
    public LockedMatch(Context context, Model matchModel, ScoreBoard scoreBoard) {
        super(context, matchModel, scoreBoard);
    }

    @Override public boolean storeState(Bundle outState) {
        return true;
    }

    @Override public boolean init(Bundle outState) {
        return true;
    }

    @Override public void show() {
        LockState lockState    = matchModel.getLockState();
        boolean   bAllowUnlock = lockState.isUnlockable();
        String    sLockedDesc  = ViewUtil.getEnumDisplayValue(context, R.array.LockState_DisplayValues, lockState);
        if ( lockState.isEndMatchManually() ) {
            sLockedDesc += " , Winner " + matchModel.getName(matchModel.m_winnerBecauseOf);
        }
        int       iResId       = bAllowUnlock ? R.string.match_is_locked__unlock_to_allow_modifications : R.string.match_is_locked;
        String    sMsg         = String.format(getString(iResId), sLockedDesc);
        //Toast.makeText(this, sMsg, Toast.LENGTH_LONG).show();
        // TODO: if lockstate is LockedIdleTime, also mention the number of minutes
        adb.setMessage(sMsg)
           .setIcon(android.R.drawable.ic_lock_lock).setTitle(sLockedDesc)
           .setNegativeButton(android.R.string.cancel, dialogClickListener)
           .setNeutralButton(R.string.sb_new_match, dialogClickListener);
        if (bAllowUnlock) {
             adb.setPositiveButton(R.string.unlock, dialogClickListener);
        }
        dialog = adb.show();
    }

    private Dialog.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
        @Override public void onClick(DialogInterface dialog, int which) {
            handleButtonClick(which);
        }
    };

    @Override public void handleButtonClick(int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                scoreBoard.handleMenuItem(R.id.sb_unlock);
                break;
            case DialogInterface.BUTTON_NEUTRAL:
                scoreBoard.handleMenuItem(R.id.dyn_new_match);
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                break;
        }
    }

}
