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
import android.graphics.Color;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;

import com.doubleyellow.android.view.ViewUtil;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.model.Player;
import com.doubleyellow.scoreboard.model.ScoreLine;
import com.doubleyellow.scoreboard.prefs.ColorPrefs;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.scoreboard.prefs.ShowPlayerColorOn;
import com.doubleyellow.util.ListUtil;
import com.doubleyellow.util.MapUtil;
import com.doubleyellow.util.StringUtil;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.GraphViewStyle;
import com.jjoe64.graphview.LineGraphView;

import java.util.*;

/**
 * http://android-graphview.org/
 *
 * wget -O GraphView-3.1.4.jar "https://github.com/jjoe64/GraphView/blob/master/public/GraphView-3.1.4.jar?raw=true"
 *
 * NOT version 4.0.0 - it has changed to much
 * wget -O GraphView-4.0.0.jar "https://github.com/jjoe64/GraphView/blob/master/public/GraphView-4.0.0.jar?raw=true"
 *
 * Used/instanciated by MatchHistory activity.
 */
public class GameGraphView extends LineGraphView
{
    private static final String TAG = "SB." + GameGraphView.class.getSimpleName();

    private int iBgColor     = 0;
    private int iTxtColor    = 0;
    private int iColorA      = 0;
    private int iColorB      = 0;

    private Player winner = Player.A;
    private Player loser  = Player.B;

    /** constructor used by android platform (if defined in xml?) */
    public GameGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    public GameGraphView(Context context) {
        super(context, (String) null);
        init();
    }

    private void init() {
        if ( super.isInEditMode() == false ) {
            initColors();
        }
        initGrapView();
        if ( super.isInEditMode() ) {
            Map<Player, List<GraphViewData>> mGraphData = new HashMap<Player, List<GraphViewData>>();
            List<GraphViewData> graphViewDatasA = new ArrayList<GraphViewData>(); mGraphData.put(Player.A, graphViewDatasA);
            List<GraphViewData> graphViewDatasB = new ArrayList<GraphViewData>(); mGraphData.put(Player.B, graphViewDatasB);
            graphViewDatasA.add(new GraphViewData(0,0));
            graphViewDatasB.add(new GraphViewData(0,0));
            addPointFor(Player.A, mGraphData);
            addPointFor(Player.A, mGraphData);
            addPointFor(Player.B, mGraphData);
            addPointFor(Player.A, mGraphData);
            addPointFor(Player.A, mGraphData);
            addPointFor(Player.B, mGraphData);
            addPointFor(Player.B, mGraphData);
            addPointFor(Player.B, mGraphData);
            addPointFor(Player.B, mGraphData);

            Map<Player, String> mLegendLabels = new HashMap<Player, String>();

            setGraphDataSeries(mGraphData, mLegendLabels);
            List<String> lYLabels = constructYLabels(graphViewDatasA, graphViewDatasB);
            List<String> lXLabels = constructXLabels(graphViewDatasB, graphViewDatasA);
            super.setVerticalLabels  (lYLabels.toArray(new String[0]));
            super.setHorizontalLabels(lXLabels.toArray(new String[0]));

            graphViewStyle.setGridColor(Color.BLUE);
            graphViewStyle.setVerticalLabelsColor(Color.RED);
        }
    }
    /** Only use in edit mode */
    private void addPointFor(Player p, Map<Player, List<GraphViewData>> mGraphData) {
        List<GraphViewData> graphViewDatas = mGraphData.get(p);
        List<GraphViewData> graphViewDatasOther = mGraphData.get(p.getOther());
        GraphViewData last = ListUtil.getLast(graphViewDatas);
        double iX = ListUtil.size(graphViewDatas);
        double iY = (last==null?0:last.getY()) + 1;
        double iYOther = ListUtil.getLast(graphViewDatasOther).getY();
        graphViewDatas     .add(new GraphViewData(iX, iY));
        graphViewDatasOther.add(new GraphViewData(iX, iYOther));
    }

    private void setGraphDataSeries(Map<Player, List<GraphViewData>> mGraphData, Map<Player, String> mLegendLabels) {
        super.removeAllSeries();

        Player[] winnerAndLoser = {winner, loser}; // iterate over the players with winner first so that he appears as first in the 'legend'
        for(Player pl: winnerAndLoser) {
            List<GraphView.GraphViewData> graphViewDatas = mGraphData.get(pl);
            if ( ListUtil.isEmpty(graphViewDatas) ) { continue; }
            GraphView.GraphViewData[] objects = graphViewDatas.toArray(new GraphView.GraphViewData[] {});

            int rgb       = pl.equals(Player.A) ? iColorA : iColorB;
            int thickness = pl.equals(loser) ? 6 : 10;
            GraphViewSeries.GraphViewSeriesStyle style = new GraphViewSeries.GraphViewSeriesStyle(rgb, thickness);
            String sDescription = mLegendLabels.get(pl); // + " x";
            super.addSeries(new GraphViewSeries(sDescription, style, objects));
        }
    }

    public void showGame(Model model, int iShowGame /* 1 based !! */) {

        // overwrite colors from model
        EnumSet<ShowPlayerColorOn> colorOns = PreferenceValues.showPlayerColorOn(getContext());
        if ( colorOns.contains(ShowPlayerColorOn.GameScoreGraph) ) {
            for (Player p : Model.getPlayers()) {
                String sColor = model.getColor(p);
                if (StringUtil.isNotEmpty(sColor)) {
                    int iColor = Color.parseColor(sColor);
                    if ( p.equals(Player.A) ) {
                        iColorA = iColor;
                    } else {
                        iColorB = iColor;
                    }
                }
            }
        }

        // set styles based on last game winner
        int iColorWinner = winner.equals(Player.A)?iColorA:iColorB;
        int iColorLoser  = loser .equals(Player.A)?iColorA:iColorB;
        graphViewStyle.setGridColor(iColorLoser);
        graphViewStyle.setVerticalLabelsColor(iColorWinner);

        List<List<ScoreLine>> lGamesScoreHistory = model.getGamesScoreHistory();
        if ( ListUtil.size(lGamesScoreHistory) < iShowGame || (iShowGame < 1) ) {
            return;
        }
        List<ScoreLine> lGameScoreHistory = lGamesScoreHistory.get(iShowGame - 1);
        if ( ListUtil.size(lGameScoreHistory) > 50) {
            // enlarging labelswidth here does not work... to late?
            //int verticalLabelsWidth = ViewUtil.getScreenWidth(getContext()) / 15;
            //graphViewStyle.setVerticalLabelsWidth(verticalLabelsWidth); // to allow for 3 digits

            // reduce font size to allow for displaying bigger digits
            int   iNrOfDigits = ("" + ListUtil.size(lGameScoreHistory)).length();
            float textSize    = graphViewStyle.getTextSize();
            float newTextSize = textSize * 2 / iNrOfDigits;
            graphViewStyle.setTextSize(newTextSize);

            graphViewStyle.setGridStyle(GraphViewStyle.GridStyle.NONE);
        }

        Map<Player, List<GraphView.GraphViewData>> mGraphData = populateGraphData(model, lGameScoreHistory, iShowGame);

        List<Map<Player, Integer>> endScoreOfGames = model.getEndScoreOfGames();
        Map<Player, Integer> endScoreOfGame = null;
        if ( ListUtil.size(endScoreOfGames) >= iShowGame ) {
            endScoreOfGame = endScoreOfGames.get(iShowGame - 1);
        }
        Map<Player, String> mLegendLabels = new HashMap<Player, String>();
        for ( Player player: Model.getPlayers() ) {
            mLegendLabels.put(player, model.getName(player) + " " + (endScoreOfGame!=null?MapUtil.getInt(endScoreOfGame, player, 0):""));
        }

        setGraphDataSeries(mGraphData, mLegendLabels);
        List<GraphView.GraphViewData> dataWinner = mGraphData.get(winner);
        List<GraphView.GraphViewData> dataLoser  = mGraphData.get(loser);
        List<String> lYLabels = constructYLabels(dataWinner, dataLoser);
        List<String> lXLabels = constructXLabels(dataWinner, dataLoser);
        super.setVerticalLabels  (lYLabels.toArray(new String[0]));
        super.setHorizontalLabels(lXLabels.toArray(new String[0]));

        //graphViewStyle.setNumVerticalLabels(ListUtil.size(lYLabels)); // required ?
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        setDisplayDependentProps();
    }

    private void initGrapView() {
        super.setShowLegend(true);
        super.setLegendAlign(GraphView.LegendAlign.BOTTOM);
        super.setShowHorizontalLabels(false); // e.g. from 0 to 22 if the final score was 12-10: not very interesting

        graphViewStyle.setHorizontalLabelsColor(iTxtColor);
        float txtSize = getResources().getDimension(R.dimen.txt_medium);
              txtSize = ViewUtil.getScreenHeightWidthMinimum(getContext()) / 25;
        graphViewStyle.setTextSize(txtSize);
        //graphViewStyle.setTextSize(getResources().getInteger(R.integer.TextSizeTabStrip) );
        //graphViewStyle.setNumHorizontalLabels(5);
        //graphViewStyle.setNumVerticalLabels(4);
        setDisplayDependentProps();
    }

    private void setDisplayDependentProps() {
        int screenWidth = ViewUtil.getScreenWidth(getContext());
        int screenHWMax = ViewUtil.getScreenHeightWidthMaximum(getContext());

        // value calculated here is different if display is e.g. a presentation screen (chromecast)
        Display display = getDisplay();
        if ( display != null ) {
            screenWidth = ViewUtil.getScreenWidth(display, getContext());
            screenHWMax = ViewUtil.getScreenHeightWidthMaximum(display);
        }

        int verticalLabelsWidth = screenWidth / 20;
        graphViewStyle.setVerticalLabelsWidth(verticalLabelsWidth);
        graphViewStyle.setLegendWidth        (screenHWMax / 3); // a third of the screen width
    }

    private void initColors() {
        Map<ColorPrefs.ColorTarget, Integer> target2colorMapping = ColorPrefs.getTarget2colorMapping(getContext());

        iBgColor     = target2colorMapping.get(ColorPrefs.ColorTarget.white);
        iTxtColor    = target2colorMapping.get(ColorPrefs.ColorTarget.black);
        iColorA      = target2colorMapping.get(ColorPrefs.ColorTarget.darkest);
        iColorB      = target2colorMapping.get(ColorPrefs.ColorTarget.middlest);
        if ( true ) {
            iBgColor     = target2colorMapping.get(ColorPrefs.ColorTarget.black);
            iTxtColor    = target2colorMapping.get(ColorPrefs.ColorTarget.white);
            iColorA      = target2colorMapping.get(ColorPrefs.ColorTarget.middlest);
            iColorB      = target2colorMapping.get(ColorPrefs.ColorTarget.darkest);

            int iLightest = target2colorMapping.get(ColorPrefs.ColorTarget.lightest);

            Long iDistanceOfDarkest2Black     = ColorPrefs.mColor2Distance2Black.get(iColorB);
            Long iDistanceOfMiddlest2Darkest  = ColorPrefs.mColor2Distance2Darker.get(iColorA);
            Long iDistanceOfLightest2Middlest = ColorPrefs.mColor2Distance2Darker.get(iLightest);
            if ( iDistanceOfDarkest2Black != null ) {
                if ( (iDistanceOfDarkest2Black < iDistanceOfMiddlest2Darkest) && (iDistanceOfLightest2Middlest > iDistanceOfMiddlest2Darkest)) {
                    // switch over to make lines a little lighter but still have reasonable contrast
                    iColorB = iColorA;
                    iColorA = iLightest;
                } else if ( iDistanceOfDarkest2Black < 50 ) {
                    // switch anyway if darkest color is very close to black
                    iColorB = iColorA;
                    iColorA = iLightest;
                }
            }

            if ( iColorA == iColorB ) {
                // e.g. for monochrome
                if ( iColorA != iTxtColor ) {
                    iColorA = iTxtColor;
                }
            }
        }
    }

    private Map<Player, List<GraphView.GraphViewData>> populateGraphData(Model model, List<ScoreLine> gameHistory, int iGame /* 1-based */ ) {

        Map<Player, List<GraphView.GraphViewData>> mGraphData = new HashMap<Player, List<GraphViewData>>();
        for(Player pl: Model.getPlayers() ) {
            ArrayList<GraphViewData> graphViewDatas = new ArrayList<GraphView.GraphViewData>();
            int iStartAt = (model==null) ? 0 : model.getGameStartScoreOffset(pl, iGame - 1);
            graphViewDatas.add(new GraphView.GraphViewData(0, iStartAt));
            mGraphData.put(pl, graphViewDatas);
        }

        int iX = 0;
        for(ScoreLine scoreLine: gameHistory) {
            if ( scoreLine.isCall() || scoreLine.isBrokenEquipment() ) {
                // not a line with a score but with a decision of an appeal or a conduct call
                continue;
            }
            Player scoringPlayer = scoreLine.getScoringPlayer();
            if ( scoringPlayer == null ) {
                // e.g. first scoreline of handicap score
                continue;
            }
            winner = scoringPlayer;

            Integer score = scoreLine.getScore();
            iX++;
            List<GraphView.GraphViewData> datas = mGraphData.get(winner);
            GraphView.GraphViewData data = new GraphView.GraphViewData((double) iX, (double) score);
            datas.add(data);

            // add score of player that lost the point
            loser = winner.getOther();
            datas = mGraphData.get(loser);
            data = new GraphView.GraphViewData((double) iX, ListUtil.getLast(datas).valueY);
            datas.add(data);
        }
        return mGraphData;
    }

    // construct rang from 0-11 or 15 (or other maximum number of points scored) to be displayed on y-ax
    private List<String> constructYLabels(List<GraphView.GraphViewData> dataWinner, List<GraphView.GraphViewData> dataLoser) {
        final GraphViewData last = ListUtil.getLast(dataWinner);
        int iMax = 1;
        if ( last != null ) {
            iMax = (int) last.valueY;
        } else {
            Log.w(TAG, "no last??");
        }
        int iMin = 0;
        if ( ListUtil.size(dataWinner) > 0 ) {
            iMin = (int) dataWinner.get(0).valueY;
        }
        if ( ListUtil.size(dataLoser) > 0 ) {
            iMin = Math.min (iMin, (int) dataLoser.get(0).valueY);
        }

        // determine stepsize based on number of scorelines
        int iStepSize = 1;
        int iNrOfSteps = (iMax - iMin) / iStepSize;
        while ( iNrOfSteps > 12 ) {
            iStepSize++;
/*
            switch (iStepSize ) {
                case 1:  iStepSize  = 2; break;
                case 2:  iStepSize  = 5; break;
                default: iStepSize *= 2; break;
            }
*/
            iNrOfSteps = (iMax - iMin) / iStepSize;
        }
        List<String> lYLabels = new ArrayList<String>();
        for(int i=iMax; i>=iMin; i-=1) {
            if ( i == iMax ) {
                // always add 'max' label
                lYLabels.add(String.valueOf(i));
                continue;
            }
/*
            if ( iMax < 8 ) {
                lYLabels.add(String.valueOf(i));
                continue;
            }
*/
            if ( ( i % iStepSize != 0 ) || ( i == iMax - 1 ) ) {
                //lYLabels.add(""); // no labels if not modulo stepsize, and no label for 'max minus one'
                continue;
            }
            lYLabels.add(String.valueOf(i));

        }
        //Log.d(TAG, "Labels to show on Y ax: " + lYLabels);
        return lYLabels;
    }


    private List<String> constructXLabels(List<GraphView.GraphViewData> dataWinner, List<GraphView.GraphViewData> dataLoser) {
        final GraphViewData last = ListUtil.getLast(dataWinner);
        int iMax = ListUtil.size(dataWinner);
        List<String> lXLabels = new ArrayList<String>();
        for(int i=iMax-1; i>=0; i-=1) {
            lXLabels.add("");
        }
        return lXLabels;
    }

/*
        graphView.setCustomLabelFormatter(new CustomLabelFormatter() {
            @Override public String formatLabel(double value, boolean isValueX) {
                if (isValueX) { return null; } // let graphview generate Y-axis label for us
                int iValue = (int) value;
                return String.valueOf(iValue);
            }
        });
*/

}
