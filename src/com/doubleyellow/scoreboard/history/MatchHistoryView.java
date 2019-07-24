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

package com.doubleyellow.scoreboard.history;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.doubleyellow.android.view.ViewUtil;
import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.model.*;
import com.doubleyellow.scoreboard.prefs.ColorPrefs;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.scoreboard.prefs.Preferences;
import com.doubleyellow.scoreboard.vico.IBoard;
import com.doubleyellow.scoreboard.view.GameHistoryView;
import com.doubleyellow.scoreboard.view.GameHistoryViewH;
import com.doubleyellow.util.ListUtil;
import com.doubleyellow.util.MapUtil;
import com.doubleyellow.util.StringUtil;

import java.util.List;
import java.util.Map;

/**
 * Show game history (old fashioned paper scoring) for all games of a match.
 *
 * Used/instantiated by MatchHistory activity.
 */
public class MatchHistoryView extends LinearLayout
{

    private Model matchModel = null;
    private int   textSize;
    public MatchHistoryView(Context context, Model matchModel) {
        super(context);
        super.setOrientation(VERTICAL);

        textSize = IBoard.iTxtSizePx_PaperScoringSheet; // PreferenceValues.getTextSize(PreferenceValues.TextSize.History, context, R.integer.TextSizeHistory_default);
        textSize = (textSize * 4) / 5; // a bit smaller than on the main score board
        TableLayout llEvent = new TableLayout(context);
        this.addView(llEvent);

        List<Map<Player, Integer>> previousGamesEndScores = matchModel.getEndScoreOfGames();
        Map<Player, Integer>       gamesWon               = matchModel.getGamesWon();
        List<GameTiming>           times                  = matchModel.getTimes();
        if ( ListUtil.size(times) > ListUtil.size(previousGamesEndScores) ) {
            // prevent displaying a zero for not-(yet)-started games
            ListUtil.removeLast(times);
        }

        // event data if present
        String sEvent    = matchModel.getEventName();
        String sDivision = matchModel.getEventDivision();
        String sRound    = matchModel.getEventRound();
        if ( StringUtil.hasNonEmpty(sEvent, sDivision, sRound) ) {
            llEvent.addView(getLabelAndTextView(R.string.lbl_event   , sEvent));
            llEvent.addView(getLabelAndTextView(R.string.lbl_division, sDivision));
            llEvent.addView(getLabelAndTextView(R.string.lbl_round   , sRound));
        }

        // summary
        for(Player p: Model.getPlayers() ) {
            LinearLayout tr = getLabelAndTextView(context.getString(R.string.lbl_player) + " " + p, matchModel.getName(p, true, true));
            llEvent.addView(tr);

            Integer iGames      = gamesWon.get(p);
            Integer iGamesOther = gamesWon.get(p.getOther());
            TextView result = getTextView("" + iGames);
            result.setGravity(Gravity.CENTER);
            result.setTag((iGames > iGamesOther) ? ColorPrefs.Tags.header : ColorPrefs.Tags.item);
            tr.addView(result);

            for(Map<Player, Integer> gameEndScore: previousGamesEndScores) {
                Integer iScore      = gameEndScore.get(p);
                Integer iScoreOther = gameEndScore.get(p.getOther());
                TextView endScore = getTextView("" + iScore);
                endScore.setGravity(Gravity.CENTER);
                endScore.setTag(( iScore > iScoreOther )?ColorPrefs.Tags.header:ColorPrefs.Tags.item);
                tr.addView(endScore);
            }
        }
        LinearLayout trTimes = getLabelAndTextView("", "");
        llEvent.addView(trTimes);
        final int durationInMinutes = matchModel.getDurationInMinutes();
        String sDuration = null;
        if (durationInMinutes > 0) {
            // normal usage
            sDuration = durationInMinutes + "'";
        } else {
            // quickly entered demo match where one game took less then a minute
            sDuration = "0'" + Math.abs(durationInMinutes);
        }
        TextView txtMatchDuration = getTextView(sDuration);
        trTimes.addView(txtMatchDuration);
        for(GameTiming gt: times) {
            int durationMM = gt.getDurationMM();
            String sTime = Math.abs(durationMM) + "'";
/*
            if ( durationMM < 0 ) {
                durationMM = Math.abs(durationMM);
                sTime = "0'" + StringUtil.lpad(durationMM, '0', 2);
            }
*/
            trTimes.addView(getTextView(sTime));
        }

        this.matchModel = matchModel;

        ColorPrefs.setColor(this);

        final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        // draw 'game history' for all games
        List<List<ScoreLine>>      gameScoreHistory       = matchModel.getGameScoreHistory(); // including one in progress
        List<Map<Player, Integer>> gameCountHistory       = matchModel.getGameCountHistory();

        Map<ColorPrefs.ColorTarget, Integer> colorSchema = ColorPrefs.getTarget2colorMapping(context);
        Integer iBgColor  = colorSchema.get(ColorPrefs.ColorTarget.historyBackgroundColor);
        Integer iTxtColor = colorSchema.get(ColorPrefs.ColorTarget.historyTextColor);

        HandicapFormat handicapFormat = matchModel.getHandicapFormat();

        if ( ( Brand.isBadminton() || PreferenceValues.isBrandTesting(context) ) && ViewUtil.isLandscapeOrientation(context) ) {

            layoutParams.setMargins(0,20,0,20);

            // use GameHistoryViewH
            ScrollView svGamesH = new ScrollView(context);
            this.addView(svGamesH);

            LinearLayout llGamesH = new LinearLayout(context);
            llGamesH.setOrientation(VERTICAL);
            llGamesH.setPadding(2, 2, 2, 2);
            svGamesH.addView(llGamesH);

            int iGame = -1;
            for ( List<ScoreLine> gameHistory: gameScoreHistory ) {
                iGame++;

                GameHistoryViewH vGameHistoryHz = new GameHistoryViewH(context, null);
                vGameHistoryHz.setProperties(iBgColor, iTxtColor, textSize);
                llGamesH.addView(vGameHistoryHz, layoutParams);

                vGameHistoryHz.setScoreLines(gameHistory, handicapFormat, matchModel.getGameStartScoreOffset(Player.A, iGame), matchModel.getGameStartScoreOffset(Player.B, iGame));

                // update game score
/*
                if ( iGame     < ListUtil.size(gameCountHistory) ) {
                    vGameHistoryHz.setGameStandingBefore(gameCountHistory.get(iGame    ));
                }
*/
                if ( iGame + 1 < ListUtil.size(gameCountHistory) ) {
                    vGameHistoryHz.setGameStandingAfter (gameCountHistory.get(iGame + 1));
                }
                if ( iGame < ListUtil.size(previousGamesEndScores) ) {
                    vGameHistoryHz.setGameEndScore(previousGamesEndScores.get(iGame));
                }
                if ( (times != null) && ( iGame < ListUtil.size(times) )) {
                    vGameHistoryHz.setTiming(times.get(iGame));
                }
                vGameHistoryHz.update(false);
            }
        } else {
            layoutParams.setMargins(2,0,2,0);

            LinearLayout llGamesV = new LinearLayout(context);
            llGamesV.setOrientation(HORIZONTAL);
            llGamesV.setPadding(2, 2, 2, 2);
            this.addView(llGamesV);

            int iGame = -1;
            for ( List<ScoreLine> gameHistory: gameScoreHistory ) {
                iGame++;

                GameHistoryView vGameHistory = new GameHistoryView(context, null);
                vGameHistory.setProperties(iBgColor, iTxtColor, textSize);
                llGamesV.addView(vGameHistory, layoutParams);

                vGameHistory.setScoreLines(gameHistory, handicapFormat, matchModel.getGameStartScoreOffset(Player.A, iGame), matchModel.getGameStartScoreOffset(Player.B, iGame));
                vGameHistory.setAutoScrollDown(false);

                // update game score
                if ( iGame     < ListUtil.size(gameCountHistory) ) {
                    vGameHistory.setGameStandingBefore(gameCountHistory.get(iGame    ));
                }
                if ( iGame + 1 < ListUtil.size(gameCountHistory) ) {
                    vGameHistory.setGameStandingAfter (gameCountHistory.get(iGame + 1));
                }
                if ( iGame < ListUtil.size(previousGamesEndScores) ) {
                    vGameHistory.setGameEndScore(previousGamesEndScores.get(iGame));
                }
                if ( (times != null) && ( iGame < ListUtil.size(times) )) {
                    vGameHistory.setTiming(times.get(iGame));
                }
                vGameHistory.update(false);
            }
        }
    }

    private LinearLayout getLabelAndTextView(int iLabel, String sText) {
        return getLabelAndTextView(getContext().getString(iLabel), sText);
    }
    private LinearLayout getLabelAndTextView(String sLabel, String sText) {
        LinearLayout llLabelAndText = new TableRow(getContext());
        llLabelAndText.setOrientation(LinearLayout.HORIZONTAL);

        TextView label = getTextView(sLabel);
        label.setTag(ColorPrefs.Tags.header.toString());
        llLabelAndText.addView(label);

        TextView text = getTextView(sText);

        text.setTag(ColorPrefs.Tags.item.toString());
        llLabelAndText.addView(text);

        return llLabelAndText;
    }

    private TextView getTextView(String sLabel) {
        TextView label = new TextView(getContext());
        label.setText(sLabel);
        label.setTextSize(Preferences.TEXTSIZE_UNIT, textSize);
        label.setPadding(5, 1, 5, 1);
        return label;
    }

    public String getTitle() {
        Map<Player, Integer> mSetsWon = matchModel.getGamesWon();
        String sPlayers  = matchModel.getName(Player.A) + " - " + matchModel.getName(Player.B);
        String sGameScore = MapUtil.getInt(mSetsWon, Player.A, 0) + "-" + MapUtil.getInt(mSetsWon, Player.B, 0);
        return sPlayers + " : " + sGameScore;
    }

    /** invoked when it is 'swiped' into view */
    public void scrollDownAll() {
        for(int i=0; i < super.getChildCount(); i++) {
            View v = super.getChildAt(i);
            if ( v instanceof GameHistoryView) {
                GameHistoryView gameHistory = (GameHistoryView) v;
                gameHistory.scrollDown();
            }
        }
    }
/*
    public MatchHistoryView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MatchHistoryView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
*/
}
