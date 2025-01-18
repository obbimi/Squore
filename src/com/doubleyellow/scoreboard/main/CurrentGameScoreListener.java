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
package com.doubleyellow.scoreboard.main;

import android.view.Gravity;
import android.view.View;
import android.widget.Toast;

import com.doubleyellow.scoreboard.R;

class CurrentGameScoreListener extends ScoreBoardListener implements View.OnClickListener
{
    CurrentGameScoreListener(ScoreBoard scoreBoard) {
        super(scoreBoard);
    }

    private long lLastClickTime;
    private Toast toast = null;

    @Override public void onClick(View view) {
        long currentTime = System.currentTimeMillis();
        if ( currentTime - lLastClickTime > 1500 ) {
            toast = Toast.makeText(scoreBoard, R.string.press_again_to_undo_last_score, Toast.LENGTH_LONG);
            toast.setGravity(Gravity.BOTTOM, 0, 0);
            toast.show();
            lLastClickTime = currentTime;
        } else {
            handleMenuItem(R.id.dyn_undo_last); // TODO: enough 'undo' options, we can use something else here
            lLastClickTime = 0;
            if ( toast != null ) {
                toast.cancel();
            }
        }
    }
}
