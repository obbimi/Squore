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

import android.view.View;

import com.doubleyellow.android.view.TouchBothListener;
import com.doubleyellow.scoreboard.R;

import java.util.Arrays;
import java.util.List;

public class ClickBothListener extends ScoreBoardListener implements TouchBothListener.ClickBothListener {
    ClickBothListener(ScoreBoard scoreBoard) {
        super(scoreBoard);
    }
    private long lLastBothClickServeSideButtons = 0;
    private final List<Integer> bothScoreButtons  = Arrays.asList(R.id.btn_score1 , R.id.btn_score2 );
    private final List<Integer> bothPlayerButtons = Arrays.asList(R.id.txt_player1, R.id.txt_player2);
    private final List<Integer> bothSideButtons   = Arrays.asList(R.id.btn_side1  , R.id.btn_side2  );

    @Override public boolean onClickBoth(View view1, View view2) {
        scoreBoard.dbgmsg("Clicked both", view1.getId(), view2.getId());

        List<Integer> lIds = Arrays.asList(view1.getId(), view2.getId());
        if ( lIds.containsAll(bothPlayerButtons) ) {
            if ( scoreBoard.isLandscape() ) {
                handleMenuItem(R.id.sb_change_sides);
            } else {
                handleMenuItem(R.id.dyn_new_match);
            }
            return true;
        }
        if ( lIds.containsAll(bothScoreButtons) ) {
            if ( scoreBoard.isInPromoMode() == false ) {
                // do not do this in promo mode: allow to tab both score buttons very fast to go to the end of a game
                return handleMenuItem(R.id.sb_adjust_score);
            }
        }
        if ( lIds.containsAll(bothSideButtons) ) {
            // only works decently in portrait mode when side buttons are not 'over' the score buttons
            if ( (System.currentTimeMillis() - lLastBothClickServeSideButtons < 500)) {
                // toggle demo mode if
                handleMenuItem(R.id.sb_toggle_demo_mode);
                return true;
            } else {
                lLastBothClickServeSideButtons = System.currentTimeMillis();
            }
        }
        return false;
    }
}
