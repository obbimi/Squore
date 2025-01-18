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

import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.model.Player;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.scoreboard.vico.IBoard;

public class PlayerNamesButtonListener extends ScoreBoardListener implements View.OnLongClickListener, View.OnClickListener
{
    PlayerNamesButtonListener(ScoreBoard scoreBoard) {
        super(scoreBoard);
    }

    @Override public boolean onLongClick(View view) {
        int viewId = getXmlIdOfParent(view);
        Player pl = IBoard.m_id2player.get(viewId);
        if ( pl == null ) { return false; }
        Model matchModel = getMatchModel();
        if ( matchModel == null ) { return false; }

        if ( matchModel.isDoubles() ) {
            // toggle player names of the long clicked team
            handleMenuItem(R.id.sb_swap_double_players, pl);
        } else {
            if ( scoreBoard.isWearable() && matchModel.hasStarted()==false ) {
                if ( PreferenceValues.isBrandTesting(scoreBoard) ) {
                    Brand.toggleBrand(scoreBoard);
                } else {
                    // on wearable allow changing name with minimal interface
                    if ( handleMenuItem(R.id.pl_change_name, pl) == false ) {
                        handleMenuItem(R.id.pl_show_conduct, pl);
                    };
                }
            } else {
                handleMenuItem(R.id.pl_show_conduct, pl);
            }
        }

        return true;
    }

    @Override public void onClick(View view) {
        int viewId = getXmlIdOfParent(view);
        Player pl = IBoard.m_id2player.get(viewId);
        if ( pl == null ) { return; }
        if ( Brand.isSquash() ) {
            handleMenuItem(R.id.pl_show_appeal, pl);
        } else {
            if ( scoreBoard.isWearable() ) {
                // for non-squash allow changing name by short click as well
                handleMenuItem(R.id.pl_change_name, pl);
            }
        }
    }
}
