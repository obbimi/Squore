package com.doubleyellow.scoreboard.history;

import android.content.Context;
import android.view.Gravity;
import android.widget.*;
import com.doubleyellow.android.view.ViewUtil;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.model.*;
import com.doubleyellow.scoreboard.prefs.ColorPrefs;
import com.doubleyellow.util.MapUtil;
import com.doubleyellow.util.StringUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A view that presents 3 tables in which all calls that have been made in a match can be consulted.
 *
 * Used/instantiated by MatchHistory activity.
 */
public class MatchCallsView extends LinearLayout
{
    private TableRow.LayoutParams params = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT);
    private Model matchModel = null;

    public MatchCallsView(Context context, Model matchModel) {
        super(context);
        super.setOrientation(VERTICAL);

        this.matchModel = matchModel;

        // draw 'set history' for all sets
        super.setPadding(10,10,10,10);

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

        TableLayout tlCount = getCallCount(context, matchModel, mPlayerCallCount);
        super.addView(tlCount, params);

        ScrollView svConduct = new ScrollView(context);
        super.addView(svConduct, params);
        TableLayout tlListConduct = getListConducts(matchModel);
        svConduct.addView(tlListConduct, params);

        ScrollView svAppeal = new ScrollView(context);
        super.addView(svAppeal, params);
        TableLayout tlListAppeal = getListAppealsAndCalls(matchModel);
        svAppeal.addView(tlListAppeal, params);

        ColorPrefs.setColor(this);
    }

    private TableLayout getCallCount(Context context, Model matchModel, Map<Player, Map<Call, Integer>> mPlayerCallCount) {
        // prepare child tablelayout that spans
        TableLayout tlCount = new TableLayout(context);

        // add total counts (header)
        {
            TableRow trH = new TableRow(context);
            trH.setTag(ColorPrefs.Tags.header.toString());
            trH.addView(newCell(R.string.name));
            for(Call call: Call.values() ) {
                trH.addView( newCell( call.getAbbreviationResourceId() ) );
            }
            tlCount.addView(trH);
        }

        // add total counts (rows)
        for(Player player : Model.getPlayers() ) {
            TableRow trCnt = new TableRow(context);
            trCnt.setTag(ColorPrefs.Tags.item.toString());
            trCnt.addView(newCell(matchModel.getName(player)));

            for(Call call: Call.values() ) {
                Map<Call, Integer> callCount = mPlayerCallCount.get(player);
                trCnt.addView(newCell(String.valueOf(MapUtil.getInt(callCount, call, 0))));
            }

            tlCount.addView(trCnt);
        }
        return tlCount;
    }

    private TableLayout getListAppealsAndCalls(Model matchModel) {
        TableLayout tlListAppeal = new TableLayout(getContext());

        List<List<ScoreLine>> gameScoreHistory = matchModel.getGameScoreHistory();
        int iGame = 0;
        for (List<ScoreLine> gameHistory : gameScoreHistory) {
            iGame++;
            Map<Player, Integer> scoreOfGameInProgress = new HashMap<Player, Integer>();
            for (ScoreLine scoreLine : gameHistory) {
                if (scoreLine.isCall()) {
                    if ( scoreLine.getCall().isConduct() ) { continue; }

                    Player callTargetPlayer = scoreLine.getCallTargetPlayer();

                    TableRow tr = new TableRow(getContext());
                    tr.setTag(ColorPrefs.Tags.item.toString());

                    tr.addView(newCell(String.valueOf(iGame)));
                    tr.addView(newCell(MapUtil.getInt(scoreOfGameInProgress, Player.A, 0) + "-" + MapUtil.getInt(scoreOfGameInProgress, Player.B, 0)));
                    tr.addView(newCell(matchModel.getName(callTargetPlayer)));
                    tr.addView(newCell(scoreLine.getCall().getAbbreviationResourceId()));

                    tlListAppeal.addView(tr);
                } else {
                    Player scoringPlayer = scoreLine.getScoringPlayer();
                    if ( scoringPlayer != null ) {
                        scoreOfGameInProgress.put(scoringPlayer, scoreLine.getScore());
                    } else {
                        // broken equipment
                    }
                }
            }
        }

        int childCount = tlListAppeal.getChildCount();
        if ( childCount > 0 ) {
            TableRow trH = new TableRow(getContext());
            trH.setTag(ColorPrefs.Tags.header.toString());

            trH.addView(newCell(R.string.Game));
            trH.addView(newCell(R.string.Score));
            trH.addView(newCell(R.string.Appeal));
            trH.addView(newCell(R.string.Call));
            tlListAppeal.addView(trH, 0);
        }
        return tlListAppeal;
    }

    private TableLayout getListConducts(Model matchModel) {
        TableLayout tlListConduct = new TableLayout(getContext());

        List<List<ScoreLine>> gameScoreHistory = matchModel.getGameScoreHistory();
        int iGame = 0;
        int iConduct = -1;
        for (List<ScoreLine> gameHistory : gameScoreHistory) {
            iGame++;
            Map<Player, Integer> scoreOfGameInProgress = new HashMap<Player, Integer>();
            for (ScoreLine scoreLine : gameHistory) {
                if ( scoreLine.isCall() ) {
                    if ( scoreLine.getCall().isConduct()==false) { continue; }

                    Player callTargetPlayer = scoreLine.getCallTargetPlayer();

                    TableRow tr = new TableRow(getContext());
                    tr.setTag(ColorPrefs.Tags.item.toString());

                    tr.addView(newCell(String.valueOf(iGame)));
                    tr.addView(newCell(MapUtil.getInt(scoreOfGameInProgress, Player.A, 0) + "-" + MapUtil.getInt(scoreOfGameInProgress, Player.B, 0)));
                    tr.addView(newCell(matchModel.getName(callTargetPlayer)));
                    tr.addView(newCell(scoreLine.getCall().getAbbreviationResourceId()));

                    iConduct++;
                    Map<String, String> mConduct = matchModel.getConduct(iConduct);
                    String type = mConduct.get(JSONKey.type.toString());
                    if (StringUtil.isNotEmpty(type)) {
                        ConductType conductType = ConductType.valueOf(type);
                        tr.addView(newCell(ViewUtil.getEnumDisplayValue(getContext(), R.array.ConductType_DisplayValues, conductType)));
                    }

                    tlListConduct.addView(tr);
                } else {
                    Player scoringPlayer = scoreLine.getScoringPlayer();
                    if ( scoringPlayer != null ) {
                        scoreOfGameInProgress.put(scoringPlayer, scoreLine.getScore());
                    }
                }
            }
        }
        int childCount = tlListConduct.getChildCount();
        if ( childCount > 0 ) {
            TableRow trH = new TableRow(getContext());
            trH.setTag(ColorPrefs.Tags.header.toString());

            trH.addView(newCell(R.string.Game));
            trH.addView(newCell(R.string.Score));
            trH.addView(newCell(getContext().getString(R.string.oal_misconduct_by, "")));
            trH.addView(newCell(R.string.Type));
            trH.addView(newCell(R.string.Detail));
            tlListConduct.addView(trH, 0);
        }
        return tlListConduct;
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
