package com.doubleyellow.scoreboard.history;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.model.*;
import com.doubleyellow.scoreboard.prefs.ColorPrefs;
import com.doubleyellow.util.Enums;
import com.doubleyellow.util.ListUtil;
import com.doubleyellow.util.MapUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A view that presents a table with winner/error fh/bh front/back/volley stats
 *
 * Used/instanciated by MatchHistory activity.
 */
public class MatchStatisticsView extends LinearLayout
{
    private TableRow.LayoutParams params = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT);
    private Model matchModel = null;

    public MatchStatisticsView(Context context, Model matchModel) {
        super(context);
        super.setOrientation(VERTICAL);

        this.matchModel = matchModel;

        // draw 'set history' for all sets
        super.setPadding(10, 10, 10, 10);

/*
        Map<Player, Map<Call, Integer>> mPlayerCallCount = new HashMap<Player, Map<Call, Integer>>();
        for ( Player player : Model.getPlayers() ) {
            mPlayerCallCount.put(player, new HashMap<Call, Integer>());
        }
        List<List<ScoreLine>> gameScoreHistory = matchModel.getGameScoreHistory();
        for ( List<ScoreLine> gameHistory : gameScoreHistory ) {
            for ( ScoreLine scoreLine : gameHistory ) {
                if ( scoreLine.isCall() ) {
                    Player             callTargetPlayer = scoreLine.getCallTargetPlayer();
                    Map<Call, Integer> callCount        = mPlayerCallCount.get(callTargetPlayer);
                    MapUtil.increaseCounter(callCount, scoreLine.getCall());
                }
            }
        }
*/

        ScrollView svWinnerErrorStats = new ScrollView(context);
        super.addView(svWinnerErrorStats, params);
        View tlListWinnerErrorStats = getListWinnerErrorStats();
        svWinnerErrorStats.addView(tlListWinnerErrorStats, params);

        ColorPrefs.setColor(this);
    }

    private View getListWinnerErrorStats() {
        TableRow.LayoutParams trParams2 = new TableRow .LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow   .LayoutParams.WRAP_CONTENT);
        trParams2.span = 2;

        TableLayout tl = new TableLayout(getContext());

        List<String> statistics = matchModel.getStatistics();
        if ( ListUtil.isEmpty(statistics) ) {
            TextView textView = new TextView(getContext());
            textView.setText(R.string.no_statistics_recorded_for_this_match);
            return textView;
        }

        // add headers
        TableRow trH = new TableRow(getContext());
        trH.setTag(ColorPrefs.Tags.header.toString());

        trH.addView(newCell(matchModel.getName(Player.A)), trParams2);
        trH.addView(newCell(matchModel.getResult()));
        trH.addView(newCell(matchModel.getName(Player.B)), trParams2);
        tl.addView(trH, 0);

/*
        trH = new TableRow(getContext());
        trH.setTag(ColorPrefs.Tags.header.toString());

        for (RacketSide side : RacketSide.values()) {
            trH.addView(newCell(side.toString()));
        }
        trH.addView(newCell("Position"));
        for (RacketSide side : RacketSide.values()) {
            trH.addView(newCell(side.toString()));
        }
        tl.addView(trH, 1);
*/
        RacketSide[] racketSideValues = RacketSide.values(); // TODO: change order via preferences?

        for(RallyEnd winnerError: RallyEnd.values()) {
            String sRegExpWE = matchModel.joinStats(winnerError, null, null, null, null, null);
            List<String> matchingStatsWE = ListUtil.filter(statistics, ".*" + sRegExpWE + ".*", Enums.Match.Keep);

            // header: bh fh [winners|errors] bh fh
            TableRow trWinnerErrorH = new TableRow(getContext()); tl.addView(trWinnerErrorH);
            trWinnerErrorH.setTag(ColorPrefs.Tags.header.toString());
            for (Player player : Player.values()) {
                for (RacketSide side : racketSideValues) {
                    trWinnerErrorH.addView(newCell(side.toString()));
                }
                if ( player.equals(Player.A) ) {
                    trWinnerErrorH.addView(newCell(winnerError.toString()));
                }
            }

            for (Position position : Position.values()) {
                TableRow trPosition = new TableRow(getContext()); tl.addView(trPosition);

                for (Player player : Player.values()) {
                    for (RacketSide side : racketSideValues) {
                        String sRegExp = matchModel.joinStats(winnerError, player, side, position, null, null);
                        List<String> matchingStats = ListUtil.filter(matchingStatsWE, ".*" + sRegExp + ".*", Enums.Match.Keep);
                        trPosition.addView(newCell(""+ ListUtil.size(matchingStats)));
                    }
                    if ( player.equals(Player.A)) {
                        trPosition.addView(newCell(position.toString()));
                    }
                }
            }

            // header: #A Total [winners|errors] #B
            TableRow trWinnerErrorT = new TableRow(getContext()); tl.addView(trWinnerErrorT);
            trWinnerErrorT.setTag(ColorPrefs.Tags.header.toString());
            for (Player player : Player.values()) {
                if ( true ) {
                    // stats forehand and backhand taken together
                    String sRegExpP = matchModel.joinStats(winnerError, player, null, null, null, null);
                    List<String> matchingStatsP = ListUtil.filter(matchingStatsWE, ".*" + sRegExpP + ".*", Enums.Match.Keep);
                    trWinnerErrorT.addView(newCell("" + ListUtil.size(matchingStatsP)), trParams2);
                } else {
                    for (RacketSide side : racketSideValues) {
                        String sRegExpPRS = matchModel.joinStats(winnerError, player, side, null, null, null);
                        List<String> matchingStatsPRS = ListUtil.filter(matchingStatsWE, ".*" + sRegExpPRS + ".*", Enums.Match.Keep);
                        trWinnerErrorT.addView(newCell("" + ListUtil.size(matchingStatsPRS)));
                    }
                }
                if ( player.equals(Player.A) ) {
                    trWinnerErrorT.addView(newCell("Total "+ winnerError.toString()+ "s"));
                }
            }
        }

        return tl;
    }

    private TextView newCell(int iResId) {
        return newCell(getContext().getString(iResId));
    }
    private TextView newCell(String sMessage) {
        TextView txt = new TextView(getContext());
        txt.setText(sMessage);
        int iTxtSize = getResources().getInteger(R.integer.TextSizeCalls_default);
        int iPaddingLR = iTxtSize / 4;
        txt.setTextSize(iTxtSize);
        txt.setPadding(iPaddingLR,2,iPaddingLR,2);
        txt.setGravity(Gravity.CENTER);
/*
        if ( StringUtil.isInteger(sMessage) ) {
            txt.setGravity(Gravity.RIGHT);
        }
*/
        return txt;
    }

    public String getTitle() {
        Map<Player, Integer> mSetsWon = matchModel.getGamesWon();
        String sPlayers  = matchModel.getName(Player.A) + " - " + matchModel.getName(Player.B);
        String sGameScore = MapUtil.getInt(mSetsWon, Player.A, 0) + "-" + MapUtil.getInt(mSetsWon, Player.B, 0);
        return sPlayers + " : " + sGameScore;
    }
}
