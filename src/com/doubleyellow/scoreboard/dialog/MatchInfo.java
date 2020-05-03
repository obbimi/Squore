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
import android.os.Bundle;
import com.doubleyellow.android.view.ViewUtil;
import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.model.*;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.util.ListUtil;
import com.doubleyellow.util.StringUtil;

import java.util.List;
import java.util.Map;

/**
 * Displays the match format to the user.
 * No changes allowed.
 *
 * (For changes see com.doubleyellow.scoreboard.dialog.EditFormat
 */
public class MatchInfo extends BaseAlertDialog {

    public MatchInfo(Context context, Model matchModel, ScoreBoard scoreBoard) {
        super(context, matchModel, scoreBoard);
    }

    @Override public boolean storeState(Bundle outState) {
        return true;
    }

    @Override public boolean init(Bundle outState) {
        return true;
    }

    @Override public void show() {
        String[] tieBreakValues = context.getResources().getStringArray(R.array.tiebreakFormatDisplayValues);

        StringBuilder sb = new StringBuilder();

        if ( PreferenceValues.isUnbrandedExecutable(context) && PreferenceValues.isBrandTesting(context) ) {
            sb.append("Sport: ").append(Brand.getSport()).append("\n\n");
        }

        String eventName  = matchModel.getEventName();
        String eventDiv   = matchModel.getEventDivision();
        if ( StringUtil.hasNonEmpty(eventName, eventDiv) ) {
            sb.append("Event: ").append(eventName).append("\n");
            sb.append("Division: ").append(eventDiv).append("\n");
            sb.append("\n");
        }
        String eventRound  = matchModel.getEventRound();
        String eventLoc    = matchModel.getEventLocation();
        String eventCourt  = matchModel.getCourt();
        if ( StringUtil.hasNonEmpty(eventRound, eventLoc, eventCourt) ) {
            sb.append("Round: ").append(eventRound).append("\n");
            sb.append("Location: ").append(eventLoc).append("\n");
            sb.append("Court: ").append(eventCourt).append("\n");
            sb.append("\n");
        }

        StringBuilder sbClubs = new StringBuilder();
        for(Player p: Player.values() ) {
            String sClub = matchModel.getClub(p);
            if ( StringUtil.isNotEmpty(sClub) ) {
                if (sbClubs.length() == 0 ) {
                    sbClubs.append(getString(R.string.lbl_clubs)).append(":");
                } else {
                    sbClubs.append(" - ");
                }
                sbClubs.append(sClub);
            }
        }
        if ( sbClubs.length() != 0 ) {
            sb.append(sbClubs.toString());
            sb.append("\n");
        }

        int iNrOfGamesToWin      = matchModel.getNrOfGamesToWinMatch();
        int iNrOfPointsToWinGame = matchModel.getNrOfPointsToWinGame();
        if ( matchModel.playAllGames() ) {
            sb.append(getString(R.string.total_of_x_games_to_y, (iNrOfGamesToWin * 2 - 1), iNrOfPointsToWinGame));
        } else {
            if ( iNrOfPointsToWinGame == 11 && Brand.isSquash() ) {
                // default is 11. If current format is also 11 do NOT make it part of the announcement
                sb.append(getString(R.string.oa_best_of_x_games     , (iNrOfGamesToWin * 2 - 1)));
            } else {
                sb.append(getString(R.string.oa_best_of_x_games_to_y, (iNrOfGamesToWin * 2 - 1), iNrOfPointsToWinGame));
            }
        }
        sb.append("\n");
        if ( matchModel.isEnglishScoring() ) {
            int iScorePointWhenServingResId = PreferenceValues.getSportTypeSpecificResId(context, R.string.lbl_Scoring_PointWhenServing_Squash);
            sb.append(getString(R.string.lbl_Scoring)).append(": ").append(getString(iScorePointWhenServingResId));
            sb.append("\n");
        } else {
            // point a rally: so common, don't even state it
        }
        if ( Brand.supportsTiebreakFormat() ) {
            sb.append("\n");
            sb.append(getString(R.string.pref_tiebreakFormat)).append(" : ").append(tieBreakValues[ matchModel.getTiebreakFormat().ordinal()]);
            sb.append("\n");
        }
        if ( matchModel instanceof GSMModel ) {
            String[] fsfValues = context.getResources().getStringArray(R.array.finalSetFinishDisplayValues);
            sb.append("\n");
            sb.append(getString(R.string.pref_finalSetFinish)).append(" : ").append(fsfValues[ ((GSMModel) matchModel).getFinalSetFinish().ordinal()]);
            sb.append("\n");
        }
        if ( matchModel.isDoubles() && Brand.supportsDoubleServeSequence() ) {
            sb.append("\n");
            sb.append(getString(R.string.sb_doublesServeSequence)).append(" : ").append(matchModel.getDoubleServeSequence());
            sb.append("\n");
        }

        if ( matchModel.isUsingHandicap() ) {
            HandicapFormat handicapFormat = matchModel.getHandicapFormat();
            sb.append("\n");
            sb.append(getString(R.string.handicap)).append(": ");
            sb.append(ViewUtil.getEnumDisplayValue(context, R.array.handicapFormatDisplayValues, handicapFormat));
            if ( handicapFormat.equals(HandicapFormat.SameForAllGames) ) {
                // show the handicap if it is the same for all games
                List<Map<Player, Integer>> lOffsets = matchModel.getGameStartScoreOffsets();
                Map<Player, Integer> mFirst = lOffsets.get(0);
                sb.append(" (");
                sb.append(mFirst.get(Player.A)).append("-").append(mFirst.get(Player.B));
                sb.append(")");
            }
            sb.append("\n");
        }
        if ( Brand.isRacketlon() ) {
            // show the current sequence that is/was played
            RacketlonModel rm = (RacketlonModel) matchModel;
            List<Sport> disciplines = rm.getDisciplines();
            sb.append("\n");
            sb.append(getString(R.string.discipline_order)).append(": ").append(ListUtil.join(disciplines, ", "));
        }

        adb.setTitle(R.string.pref_MatchFormat)
           .setMessage(sb.toString().trim())
           .setIcon(R.drawable.ic_action_mouse)
           .setPositiveButton(android.R.string.ok, null)
           .show();
    }
}