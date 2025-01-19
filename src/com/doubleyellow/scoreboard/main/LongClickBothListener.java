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
import android.widget.Toast;

import com.doubleyellow.android.view.TouchBothListener;
import com.doubleyellow.android.view.ViewUtil;
import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.model.LockState;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.util.ListUtil;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;

public class LongClickBothListener extends ScoreBoardListener implements TouchBothListener.LongClickBothListener
{
    LongClickBothListener(ScoreBoard scoreBoard) {
        super(scoreBoard);
    }

    @Override public boolean onLongClickBoth(View view1, View view2) {
        scoreBoard.dbgmsg("Long Clicked both", view1.getId(), view2.getId());

        Model matchModel = getMatchModel();
        if ( matchModel == null ) { return false; }

        List<Integer> lIds = Arrays.asList(view1.getId(), view2.getId());
        if ( lIds.containsAll(Arrays.asList(R.id.txt_player1, R.id.txt_player2)) ) {
            if ( matchModel.isLocked() ) {
                if ( matchModel.isUnlockable() ) {
                    return handleMenuItem(R.id.sb_unlock);
                }
            } else {
                return handleMenuItem(R.id.sb_lock, LockState.LockedManualGUI);
            }
        }
        if ( lIds.containsAll(Arrays.asList(R.id.btn_score1, R.id.btn_score2)) ) {
            if ( ViewUtil.isWearable(scoreBoard) ) {
                // for convenience shortly display the current time on a wearable, so players can consult time by long pressing both score buttons
                // to see how long players still may remain on court if the have only a limited time to play
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
                String sCurrentTime = sdf.format(new Date());
                scoreBoard.iBoard.showMessage(sCurrentTime, 3);
            } else {
                if ( scoreBoard.m_MQTTHandler != null ) {
                    Set<String> subscriptionTopics = scoreBoard.m_MQTTHandler.getSubscriptionTopics();
                    if ( ListUtil.isNotEmpty(subscriptionTopics) ) {
                        for(String sTopic: subscriptionTopics ) {
                            Toast.makeText(scoreBoard, sTopic, Toast.LENGTH_SHORT).show();
                        }
                    }
                    return true;
                }
                if ( matchModel.hasStarted() ) {
                    return handleMenuItem(R.id.sb_match_format);
                } else {
                    return handleMenuItem(R.id.change_match_format);
                }
            }
        }
        // TODO: this only works if side buttons are NOT on top of score buttons (portrait only for now)
        if ( lIds.containsAll(Arrays.asList(R.id.btn_side1, R.id.btn_side2)) ) {
            // Nothing for end user yet
            if ( PreferenceValues.isBrandTesting(scoreBoard) ) {
                Brand.toggleBrand(scoreBoard);
            }
        }
        // TODO: if playing double switch player names if clicking the two child view of R.id.txt_player1
        return false;
    }
}
