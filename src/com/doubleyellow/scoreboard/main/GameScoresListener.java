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

import android.util.Log;
import android.view.View;

import androidx.appcompat.app.ActionBar;

import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.R;

class GameScoresListener extends ScoreBoardListener implements View.OnLongClickListener, View.OnClickListener
{
    private static final String TAG = "SB." + GameScoresListener.class.getSimpleName();

    GameScoresListener(ScoreBoard scoreBoard) {
        super(scoreBoard);
    }

    private long lActionBarToggledAt = 0L;
    @Override public void onClick(View view) {
        if ( Brand.isGameSetMatch() ) {
            scoreBoard.toggleSetScoreView();
        } else if ( Brand.isRacketlon() == false ) {
            scoreBoard.toggleGameScoreView();
        } else {
            long currentTime = System.currentTimeMillis();
            if ( currentTime - lActionBarToggledAt > 1500 ) {
                // prevent single click show history being triggered after a long click
                if ( scoreBoard.isWearable() ) {
                    // score details NOT yet optimized for wearables
                } else {
                    handleMenuItem(R.id.sb_score_details);
                }
            } else {
                Log.d(TAG, "Skip single click for now... ");
            }
        }
    }

    @Override public boolean onLongClick(View view) {
        ActionBar actionBar = scoreBoard.getXActionBar();
        if ( (actionBar != null) && (scoreBoard.isWearable() == false) /*&& (PreferenceValues.showActionBar(ScoreBoard.this) == false)*/ ) {
            scoreBoard.toggleActionBar(actionBar);
            lActionBarToggledAt = System.currentTimeMillis();
        } else {
            this.onClick(view);
        }
        return false;
    }
}
