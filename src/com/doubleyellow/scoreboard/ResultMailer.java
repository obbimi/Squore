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

package com.doubleyellow.scoreboard;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.model.GameTiming;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.model.Player;
import com.doubleyellow.scoreboard.model.ScoreLine;
import com.doubleyellow.scoreboard.prefs.PreferenceKeys;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.scoreboard.prefs.ScorelineLayout;
import com.doubleyellow.util.ListUtil;
import com.doubleyellow.util.MapUtil;
import com.doubleyellow.util.StringUtil;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Help class for e-mailing match results with the users preferred mail app.
 */
public class ResultMailer {

    public static final String TAG = "SB." + ResultMailer.class.getSimpleName();

    public void mail(Context context, Model matchModel, boolean bHtml)
    {
        StringBuilder sbSubject = new StringBuilder();
        sbSubject.append(matchModel.getName(Player.A)).append(" - ").append(matchModel.getName(Player.B));
        Map<Player, Integer> gameCount = matchModel.getGamesWon();
        sbSubject.append(" : ").append(gameCount.get(Player.A)).append(" - ").append(gameCount.get(Player.B));

        String sendMatchResultTo = PreferenceValues.getDefaultMailTo(context);
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", sendMatchResultTo, null));

        if ( bHtml ) {
          //Map<ColorPrefs.ColorTarget, Integer> target2colorMapping = ColorPrefs.getTarget2colorMapping(context);
            boolean bLimitedHtml = true;
            StringBuilder sbHtml = new StringBuilder();
            if ( bLimitedHtml ) {
                buildLimitedHtmlContent(context, matchModel, sbHtml);
                Log.d(TAG, "Html: \n" + sbHtml.toString());
            } else {
                // FULL html not yet supported in android: http://www.nowherenearithaca.com/2011/10/some-notes-for-sending-html-email-in.html
                buildFullHtmlContent(context, matchModel, sbHtml);
            }

            Spanned fromHtml = Html.fromHtml(sbHtml.toString());
            emailIntent.putExtra(Intent.EXTRA_TEXT     , fromHtml);
            //emailIntent.putExtra(Intent.EXTRA_HTML_TEXT, sbHtml.toString());
            //emailIntent.putExtra(Intent.EXTRA_HTML_TEXT, fromHtml);
            //emailIntent.putExtra(Intent.EXTRA_SUBJECT, sbSubject.toString());
          //emailIntent.setType("text/html"); // "unable to find application to perform this action"??
        } else {
            StringBuilder sbMsg = new StringBuilder();
            List<Map<Player, Integer>> gameScores = matchModel.getGameScoresIncludingInProgress();
            for(Map<Player, Integer> mGameScore: gameScores) {
                sbMsg.append(mGameScore.get(Player.A)).append(" - ").append(mGameScore.get(Player.B)).append("\n");
            }

            String sendMatchResultFrom = "";
            if ( StringUtil.isNotEmpty(sendMatchResultFrom)) {
                sbMsg.append("\n");
                sbMsg.append(sendMatchResultFrom);
            }

            emailIntent.putExtra(Intent.EXTRA_TEXT, sbMsg.toString());
        }
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, sbSubject.toString());

        if ( StringUtil.isEmpty(sendMatchResultTo) ) {
            String sIn1 = context.getString(R.string.settings);
            String sIn2 = Brand.getShortName(context);
            PreferenceValues.showTip(context, PreferenceKeys.mailResultTo, context.getString(R.string.pref_mailResultTo_not_set, sIn1, sIn2), true);
        }
        try {
            context.startActivity(Intent.createChooser(emailIntent, context.getString(R.string.sb_email_match_result)));
        } catch (Exception e) {
          //Log.e(TAG, "Starting sms or call failed");
            e.printStackTrace();
        }

    }

    private static final int     I_SCORE_LINE_WIDTH      = 6; // 4 + 2 (if score is in the double digits for both)
    private static final int     I_SPACING_BETWEEN_GAMES = 1; // adding more than one does not work well
    private static final int     I_GAME_COLUMN_WIDTH     = I_SCORE_LINE_WIDTH + I_SPACING_BETWEEN_GAMES;
    private static final String  S_PIPES                 = " | ";

    /**
     * Only vere limited set of tags is supported.
     * Within <tt> tags multiple whitespaces are only displayed as one.
     */
    private void buildLimitedHtmlContent(Context context, Model matchModel, StringBuilder sbHtml) {
        sbHtml.append("<tt>");

        // draw 'set history' for all sets
        List<List<ScoreLine>>      gameScoreHistory = matchModel.getGamesScoreHistory();

        int iMaxNrOfScoreLines = 0;
        List<List<ScoreLine>> lTmp = new ArrayList<List<ScoreLine>>();
        for(List<ScoreLine> setHistory: gameScoreHistory ) {
            if ( ListUtil.isEmpty(setHistory)) { break; }
            iMaxNrOfScoreLines = Math.max(iMaxNrOfScoreLines, ListUtil.size(setHistory));
            lTmp.add(setHistory);
        }
        gameScoreHistory = lTmp;

        List<String> lLines = new ArrayList<String>();
        final String DUMMY_SCORELINE = StringUtil.rpad("", '_', I_SCORE_LINE_WIDTH);
        int iMaxLenghtOfLine = 0;
        for ( int iLine = 0; iLine < iMaxNrOfScoreLines; iLine++ ) {
            StringBuilder sbLine = new StringBuilder();
            for (int iGame = 0; iGame < gameScoreHistory.size(); iGame++) {
                List<ScoreLine> setHistory= gameScoreHistory.get(iGame);

                // do not output anything for sets without any points scored
                if ( ListUtil.isEmpty(setHistory)) { continue; }

                String sValue;
                if ( iLine < ListUtil.size(setHistory) ) {
                    ScoreLine line = setHistory.get(iLine);
                    sValue = line.toString6();
                    if ( I_SCORE_LINE_WIDTH == 7 ) {
                        String sValue7 = sValue.substring(0,3) + " " + sValue.substring(3,6);
                        // TODO: does not work well for 'calls'
                        sValue = sValue7;
                    }
                } else {
                    sValue = DUMMY_SCORELINE;
                }

                // separate set data from next set data
                String sValuePadded = finalizeForFixedWidthTableCell(iGame, sValue);
                sbLine.append(sValuePadded);
            }
            String sLine = sbLine.toString().trim();
            iMaxLenghtOfLine = Math.max(iMaxLenghtOfLine, StringUtil.size(sLine));

            lLines.add(sLine);
        }
        String sSplitter  = StringUtil.rpad("", '=', iMaxLenghtOfLine);
        String sSplitterS = StringUtil.rpad("", '-', iMaxLenghtOfLine);
        lLines.add(0, sSplitter);
        lLines.add(sSplitter);

        // add a row with the final score of each game and the game count
        {
            List<Map<Player, Integer>> gamesEndScores   = matchModel.getEndScoreOfGames();
            StringBuilder sbGamesScores = new StringBuilder();
            List<Map<Player, Integer>> gameCountHistory = matchModel.getGameCountHistory();
            StringBuilder sbGameCount = new StringBuilder();
            StringBuilder sbGamesTimes = new StringBuilder();
            List<GameTiming> times = matchModel.getTimes();
            for (int iGame = 0; iGame < gamesEndScores.size(); iGame++) {
                Map<Player, Integer> mGameEndScore = gamesEndScores.get(iGame);
                int pA = MapUtil.getInt(mGameEndScore, Player.A, 0);
                int pB = MapUtil.getInt(mGameEndScore, Player.B, 0);
                String sPoints = StringUtil.lpad(pA, '0', 2) + "-" + StringUtil.lpad(pB, '0', 2);
                // indicate who whon the game

                Map<Player, Integer> mGameCount = gameCountHistory.get(iGame+1);
                int gA = MapUtil.getInt(mGameCount, Player.A, 0);
                int gB = MapUtil.getInt(mGameCount, Player.B, 0);
                String sGames = gA + " - " + gB;

                if (pA > pB) { sPoints = "*" + sPoints + " "; }
                else         { sPoints = " " + sPoints + "*"; }
                if (pA > pB) { sGames  = "*" + sGames  + " "; }
                else         { sGames  = " " + sGames  + "*"; }

                sbGamesScores.append(finalizeForFixedWidthTableCell(iGame, sPoints));
                sbGameCount  .append(finalizeForFixedWidthTableCell(iGame, sGames));

                if ( iGame < ListUtil.size(times) ) {
                    GameTiming timing = times.get(iGame);
                    int iValue = timing.getDurationMM();
                    iValue = Math.abs(iValue); // if game was less than a minute, time is/was in seconds but negative
                    String sValuePadded = finalizeForFixedWidthTableCell(iGame, "T " + iValue + "'");
                    sbGamesTimes.append(sValuePadded);
                }
            }
            // add a row with the final score of each game
            lLines.add(sbGamesScores.toString().trim());
            lLines.add(sSplitterS);
            // add a row with the game count
            lLines.add(sbGameCount.toString().trim());
            lLines.add(sSplitterS);
            // add a row with only the duration of each game
            lLines.add(sbGamesTimes.toString().trim());
            lLines.add(sSplitter);

            // add a row with the date of the match
            DateFormat sdf = android.text.format.DateFormat.getDateFormat(context);
            Date matchDate = matchModel.getMatchDate();
            String sDate      = sdf.format(matchDate);
            lLines.add(context.getString(R.string.date) + " : " + sDate);
            sdf = android.text.format.DateFormat.getTimeFormat(context);
            String sTime      = sdf.format(matchDate);
            lLines.add(context.getString(R.string.time) + " : " + sTime);
        }

        // TODO: add this somewhere: matchModel.getDurationInMinutes()

        sbHtml.append(ListUtil.join(lLines, "</tt><br/>\n<tt>"));
        sbHtml.append("</tt>");
    }

    private String finalizeForFixedWidthTableCell(int iGame, String sValue) {
        //sValue = StringUtil.rpad(sValue, ' ', I_GAME_COLUMN_WIDTH);
        sValue = sValue.trim();
        if ( iGame == 0 ) {
            sValue = S_PIPES + sValue;
        }
        return sValue + S_PIPES;
    }

    private void _appendSplitter(StringBuilder sbHtml, char cSplitter, int iLength) {
        _appendNewLine(sbHtml);
        String sSplitter = StringUtil.rpad("", cSplitter, iLength);
        sbHtml.append(sSplitter);
        _appendNewLine(sbHtml);
    }

    private void _appendNewLine(StringBuilder sb) {
        //sb.append("\n");
        //sb.append("<blockquote/>\n");

        // close tt tag, insert line break, re-open tt again
        sb.append("</tt><br/>\n<tt>");
    }


    private void buildFullHtmlContent(Context ctx, Model matchModel, StringBuilder sbHtml) {
        sbHtml.append("<html><table><tr>");

        ScorelineLayout scorelineLayout = PreferenceValues.getScorelineLayout(ctx);

        // draw 'set history' for all sets
        List<List<ScoreLine>> setScoreHistory = matchModel.getGamesScoreHistory();
        List<GameTiming> times = matchModel.getTimes();
        List<Map<Player, Integer>> setScores = matchModel.getEndScoreOfGames();
        List<Map<Player, Integer>> setCountHistory = matchModel.getGameCountHistory();
        int iCnt = -1;
        for(List<ScoreLine> setHistory: setScoreHistory ) {
            iCnt++;
            sbHtml.append("<td><table>");

            for (ScoreLine line: setHistory)
            {
                sbHtml.append("<tr>"); //.append("style='background-color:").append(target2colorMapping.get(ColorPrefs.ColorTarget.backgroundColor)).append("'>");
                List<String> saScore = line.toStringList(ctx);
                if ( scorelineLayout.swap34() ) {
                    ScoreLine.swap(saScore, 3);
                }
                for(String sValue: saScore) {
                    sbHtml.append("<td> "); //.append("style='color:").append(target2colorMapping.get(ColorPrefs.ColorTarget.mainTextColor)).append("'>");
                    sbHtml.append(sValue).append("</td>");
                }
                sbHtml.append("</tr>").append("\n");
            }

            if ( iCnt < ListUtil.size(setScores) ) {
                Map<Player, Integer> mGameEndScore = setScores.get(iCnt);
                if (MapUtil.isNotEmpty(mGameEndScore)) {
                    String sValue = MapUtil.getInt(mGameEndScore, Player.A, 0) + " - " + MapUtil.getInt(mGameEndScore, Player.B, 0);
                    sbHtml.append("<tr><td rowspan='4'>").append(sValue).append("</td>").append("</tr>").append("\n");
                }
            }
            if ( iCnt + 1 < ListUtil.size(setCountHistory) ) {
                Map<Player, Integer> mGameStandingAfter = setCountHistory.get(iCnt + 1);
                if ( MapUtil.isNotEmpty(mGameStandingAfter) ) {
                    String sValue = MapUtil.getInt(mGameStandingAfter, Player.A, 0) + " - " + MapUtil.getInt(mGameStandingAfter, Player.B, 0);
                    sbHtml.append("<tr><td rowspan='4'>").append(sValue).append("</td>").append("</tr>").append("\n");
                }
            }

            // add timing
            if ( (times != null) && (ListUtil.size(times) > iCnt)) {
                GameTiming timing = times.get(iCnt);
                if ((setHistory.size() > 0) && (timing != null)) {
                    // game start time - end time
                    String sValue = timing.getStartHHMM();
                    if ( StringUtil.isNotEmpty(sValue) ) {
                        sbHtml.append("<tr><td rowspan='4'>").append(sValue).append("</td>").append("</tr>").append("\n");
                        sValue = timing.getEndHHMM();
                        sbHtml.append("<tr><td rowspan='4'>").append(sValue).append("</td>").append("</tr>").append("\n");
                    } else {
                        // add empty row??
                    }
                }

                // add duration
                if ((ListUtil.size(setHistory) != 0) && (timing != null)) {
                    int iValue = timing.getDurationMM();
                    iValue = Math.abs(iValue); // if game was less than a minute, time is/was in seconds but negative
                    sbHtml.append("<tr><td rowspan='4'>").append(iValue).append("'").append("</td>").append("</tr>").append("\n");
                }
            }
            sbHtml.append("</table></td>").append("\n");
        }
        sbHtml.append("</tr></table></html>").append("\n");
    }
}
