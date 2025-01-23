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

import com.doubleyellow.android.view.SimpleGestureListener;
import com.doubleyellow.android.view.ViewUtil;
import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.model.Player;
import com.doubleyellow.scoreboard.model.ServeSide;
import com.doubleyellow.scoreboard.vico.IBoard;
import com.doubleyellow.util.Direction;
import com.doubleyellow.util.HVD;

import java.util.EnumSet;

class ScoreBoardTouchListener extends ScoreBoardListener implements SimpleGestureListener.SwipeListener, SimpleGestureListener.EraseListener, SimpleGestureListener.TwoFingerClickListener
{
    private static final String TAG = "SB." + ScoreBoardTouchListener.class.getSimpleName();

    ScoreBoardTouchListener(ScoreBoard scoreBoard) {
        super(scoreBoard);
    }

    @Override public boolean onTwoFingerClick(View view) {
        int viewId = getXmlIdOfParent(view);
        scoreBoard.dbgmsg("Two finger clicked", viewId, 0);
        Player player = IBoard.m_id2player.get(viewId);
        if (viewId == R.id.btn_score1 || viewId == R.id.btn_score2) {// on tablet, the buttons are so big that two finger touch/click may be performed without user thinking about it
            handleMenuItem(R.id.pl_change_score, player);
            return true;
        } else if (viewId == R.id.txt_player1 || viewId == R.id.txt_player2) {
            if (Brand.isSquash()) {
                // TODO: for doubles, switch in/out in such a way that touched player becomes the server
                Log.d(TAG, String.format("Two finger click on player %s", player));
                if (ScoreBoard.getMatchModel().isDoubles()) {
                    handleMenuItem(R.id.pl_show_conduct, player);
                } else {
                    scoreBoard.showBrokenEquipment(player);
                }
            } else {
                handleMenuItem(R.id.sb_change_sides);
            }
        }
        return false;
    }

    @Override public boolean onSwipe(View view, Direction direction, float maxD, float percentageOfView) {
        if ( ViewUtil.isWearable(scoreBoard) ) {
            // wearable screen is typically to small to interpret swipe events
            // return false;
            // 2023-09-17: Wear OS guidelines says: swipe must implement 'dismiss'
            return handleMenuItem(R.id.sb_exit);
        }
        int viewId = getXmlIdOfParent(view);
        Player player = IBoard.m_id2player.get(viewId);
        scoreBoard.dbgmsg("Swipe to " + direction +" of " + maxD + " (%=" + percentageOfView + ",p=" + player + ")", viewId, 0);
        if ( percentageOfView < 0.50 ) { return false; }
        if ( player          == null ) { return false; }
        boolean isServer = player.equals(ScoreBoard.getMatchModel().getServer());
        if (viewId == R.id.btn_score1 || viewId == R.id.btn_score2) {
            if (EnumSet.of(Direction.E, Direction.W).contains(direction)) {
                if (Brand.isSquash() && isServer) {
                    // perform undo if swiped horizontally over server score button (= person who scored last)
                    handleMenuItem(R.id.dyn_undo_last);
                } else {
                    Player pLastScorer = ScoreBoard.getMatchModel().getLastScorer();
                    if (player.equals(pLastScorer)) {
                        // perform undo if swiped horizontally over last scorer
                        handleMenuItem(R.id.dyn_undo_last);
                    } else {
                        if (ScoreBoard.getMatchModel().getScore(player) > 0) {
                            // present user with dialog to remove last scoreline for other than latest scorer ...
                            scoreBoard.confirmUndoLastForNonScorer(player);
                        }
                    }
                }
                return true;
            }
        } else if (viewId == R.id.txt_player1 || viewId == R.id.txt_player2) {// allow changing sides if last point was a handout
            ServeSide nextServeSide = ScoreBoard.getMatchModel().getNextServeSide(player);
            if (ScoreBoard.getMatchModel().isLastPointHandout() && isServer) {
                if (nextServeSide.equals(ServeSide.L) && direction.equals(Direction.E)) {
                    // change from left to right
                    changeSide(player);
                    return true;
                } else if (nextServeSide.equals(ServeSide.R) && direction.equals(Direction.W)) {
                    // change from right to left
                    changeSide(player);
                    return true;
                }
            }
        }
        return false;
    }

    @Override public boolean onErase(View view, HVD hvd, float maxD, float percentageOfView) {
        int viewId = getXmlIdOfParent(view);
        scoreBoard.dbgmsg("Erase " + hvd + " movement of " + maxD + " (%=" + percentageOfView + ")", viewId, 0);
        if ( percentageOfView < 0.50 ) {
            return false;
        }
        if (viewId == R.id.btn_score1 || viewId == R.id.btn_score2) {
            if (hvd.equals(HVD.Diagonal)) {
                return handleMenuItem(R.id.sb_clear_score);
            }
        } else if (viewId == R.id.txt_player1 || viewId == R.id.txt_player2) {
            if (ViewUtil.getScreenHeightWidthMinimum(scoreBoard) < 320) {
                // just to have a way to get to the settings if no actionbar is visible on Wear OS
                return handleMenuItem(R.id.sb_settings);
            } else {
                return handleMenuItem(R.id.sb_edit_event_or_player);
            }
        }
        return false;
    }
}
