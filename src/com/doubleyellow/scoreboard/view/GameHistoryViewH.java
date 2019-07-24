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

package com.doubleyellow.scoreboard.view;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.ColorInt;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.TextView;

import com.doubleyellow.scoreboard.history.MatchGameScoresView;
import com.doubleyellow.scoreboard.model.GameTiming;
import com.doubleyellow.scoreboard.model.HandicapFormat;
import com.doubleyellow.scoreboard.model.Player;
import com.doubleyellow.scoreboard.model.ScoreLine;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.scoreboard.prefs.ScorelineLayout;
import com.doubleyellow.util.DateUtil;
import com.doubleyellow.util.ListUtil;
import com.doubleyellow.util.MapUtil;
import com.doubleyellow.util.StringUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * View that may display the scoring history of a single game. (In horizontal way for badminton)
 *
 * @see MatchGameScoresView
 */
public class GameHistoryViewH extends HorizontalScrollView
{
    private static final String TAG = "SB." + GameHistoryViewH.class.getSimpleName();

    private List<ScoreLine> history           = new ArrayList<ScoreLine>();
    private HandicapFormat  handicapFormat    = HandicapFormat.None;
    private GameTiming      timing            = null;
    private ScorelineLayout m_scorelineLayout = ScorelineLayout.DigitsInside;

    /** constructor used by android platform (if defined in xml?) */
    public GameHistoryViewH(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public GameHistoryViewH(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    public void setScoreLines(List<ScoreLine> lScorelines, HandicapFormat handicapFormat, int iStartScoreA, int iStartScoreB) {
        this.handicapFormat = handicapFormat;

        // add line with the initial score of a game if it has a handicap (not 0-0)
        if ( (handicapFormat.equals(HandicapFormat.None) == false) && (ListUtil.size(lScorelines) != 0) ) {
            lScorelines = new ArrayList<ScoreLine>(lScorelines);
            lScorelines.add(0, new ScoreLine( null, iStartScoreA, null, iStartScoreB));
        }
        this.history = new ArrayList<ScoreLine>(lScorelines); // to prevent 'java.util.ConcurrentModificationException' seen when using chrome cast
    }

    public void setTiming(GameTiming s) {
        this.timing = s;
    }
/*
    private Map<Player,Integer> mGameStandingBefore = null;
    public void setGameStandingBefore(Map<Player, Integer> mGameStanding) {
        this.mGameStandingBefore = new HashMap<Player, Integer>(mGameStanding);
    }
*/

    private Map<Player,Integer> mGameStandingAfter = null;
    public void setGameStandingAfter(Map<Player, Integer> mGameStanding) {
        this.mGameStandingAfter = new HashMap<Player, Integer>(mGameStanding);
    }

    private Map<Player,Integer> mGameEndScore = null;
    public void setGameEndScore(Map<Player, Integer> mGameEndScore) {
        this.mGameEndScore = new HashMap<Player, Integer>(mGameEndScore);
    }

    public void update(boolean bOnlyAddLast)
    {
        if ( bOnlyAddLast ) {
            if ( history.size() > 1 ) {
                history = history.subList(history.size()-1, history.size());
            }
        } else {
            tableLayout.removeAllViews(); // start all over
        }
/*
        if (  MapUtil.isNotEmpty(this.mGameStandingBefore) ) {
            String sValue = MapUtil.getInt(this.mGameStandingBefore, Player.A, 0) + " - " + MapUtil.getInt(this.mGameStandingBefore, Player.B, 0);
            addRowInversedColors(sValue, Gravity.LEFT);
        }
*/
        tableLayout.setColumnCount(ListUtil.size(history) + 2); // + 1 for total score of game, +1 for number of games won
        tableLayout.setRowCount   (m_scorelineLayout.equals(ScorelineLayout.HideServeSide)?2:4);

        int c = -1;
        for (ScoreLine line: history) {
            c++;

            List<String> saScore = line.toStringList(getContext());
            if ( (m_scorelineLayout != null) ) {
                if ( m_scorelineLayout.hideServeSide() ) {
                    saScore.remove(2);
                    saScore.remove(0);
                } else if ( m_scorelineLayout.swap34() ) {
                    ScoreLine.swap(saScore, 3);
                }
            }

            int r = -1;
            for(String sValue: saScore) {
                r++;

                TextView tv = new TextView(getContext());
                if ( line.isCall() || line.isBrokenEquipment() ) {
                    // little smaller because call is double letters YL, NL, ST, CW, CS, CG, CM
                    tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizePx * 2 / 3);
                } else {
                    tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizePx);
                    if ( this.handicapFormat.equals(HandicapFormat.None) == false ) {
                        sValue = sValue.replaceAll("^-$", "."); // because there may be negative numbers, use dots
                    }
                    if ( sValue.length() == 1 ) {
                        sValue = " " + sValue + " "; // single digit or dash: add spaces
                    }
                }

                GridLayout.LayoutParams param =new GridLayout.LayoutParams();
                param.height = LayoutParams.WRAP_CONTENT;
                param.width  = (int) (textSizePx * 1.5);
                param.rightMargin = 2; // to have vertical lines
                param.topMargin   = 1;
                param.setGravity(Gravity.CENTER);
                param.columnSpec = GridLayout.spec(c);
                param.rowSpec    = GridLayout.spec(r);

                tv.setLayoutParams (param);
                tableLayout.addView(tv, param);

                tv.setTextColor(textColor);
                tv.setText(sValue);
                tv.setBackgroundColor(backgroundColor);
              //tv.setGravity(Gravity.CENTER);
            }
        }

        if ( MapUtil.isNotEmpty(this.mGameEndScore) ) {
            for(Player p : Player.values()) {
                GridLayout.LayoutParams param = new GridLayout.LayoutParams();
                param.height = LayoutParams.WRAP_CONTENT;
                param.width = (int) (textSizePx * 3);
                param.rightMargin = 2; // to have vertical lines
                param.topMargin   = 1;
                param.setGravity(Gravity.CENTER);
                param.columnSpec = GridLayout.spec(ListUtil.size(history) + 1);
                if ( m_scorelineLayout.equals(ScorelineLayout.HideServeSide) ) {
                    param.rowSpec    = GridLayout.spec(p.ordinal() * 1, 1);
                } else {
                    param.rowSpec    = GridLayout.spec(p.ordinal() * 2, 2);
                }

                TextView tv = new TextView(getContext());
                tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizePx * 3 / 2);
                tv.setLayoutParams(param);

                tv.setTextColor(backgroundColor);
                tv.setText(" " + MapUtil.getInt(this.mGameEndScore, p, 0) + " ");
                tv.setBackgroundColor(textColor);
                tableLayout.addView(tv, param);
            }
        }

        if ( true ) {
            return; // TODO: temp
        }
        if ( MapUtil.isNotEmpty(this.mGameStandingAfter) ) {
            String sValue = MapUtil.getInt(this.mGameStandingAfter, Player.A, 0) + " - " + MapUtil.getInt(this.mGameStandingAfter, Player.B, 0);
            addColumnInversedColors(sValue, Gravity.END);
        }

        // add timing
        if ( (history.size() > 0) && (this.timing != null) ) {
            // game start time - end time
            String sValue  = timing.getStartHHMM();
            if ( StringUtil.isNotEmpty(sValue) ) {
                addColumn(sValue, Gravity.START);
                sValue = timing.getEndHHMM();
                addColumn(sValue, Gravity.END);
            }
        }

        // add duration
        if ( (ListUtil.size(history) != 0) && (this.timing != null) ) {
            int durationMM = timing.getDurationMM();
            String sTime;
            if ( durationMM < 3 ) {
                long duration = timing.getDuration();
                sTime = DateUtil.convertDurationToHHMMSS_Colon(duration);
            } else {
                sTime = durationMM + "'";
            }
            addColumnInversedColors(sTime, Gravity.END);
        }
        if ( this.bAutoScrollRight ) {
            scrollRight();
        }
    }

    private boolean bAutoScrollRight = true;

    public void scrollRight() {
        this.post(new Runnable() {
            @Override public void run() {
                GameHistoryViewH.this.fullScroll(View.FOCUS_RIGHT);
            }
        });
    }

    private void addColumn(String sValue, int iGravity) {
        //addColumn(sValue, iGravity, backgroundColor, textColor);
    }
    private void addColumnInversedColors(String sValue, int iGravity) {
        //addColumn(sValue, iGravity, textColor, backgroundColor);
    }

    /**
     * for adding headers and footers with
     * - duration of game
     * - final game score
     */
/*
    private void addColumn(String sValue, int iGravity, int iBgColor, int iTxtColor) {
        GridLayout.LayoutParams param = new GridLayout .LayoutParams();
        param.columnSpec = GridLayout.spec(0);
        param.set
        param.rowSpec    = GridLayout.spec(r);

        //trTimingLayoutParams.span = 4;

        TextView tv = new TextView(getContext());
        tv.setTextColor(iTxtColor);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizePx);
        tv.setText(sValue);
        tv.setGravity(iGravity);
        //tv.setLayoutParams(trTimingLayoutParams);
        //tr.addView(tv, trTimingLayoutParams);

        tableLayout.addView(tv, layoutParams);
    }
*/

    private GridLayout tableLayout = null;
    private void init(Context context, AttributeSet attrs) {
        setValuesFromXml(attrs);
        m_scorelineLayout = PreferenceValues.getScorelineLayout(context);

        super.setScrollBarStyle(SCROLLBARS_INSIDE_OVERLAY);
        tableLayout = new GridLayout(context);
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        this.addView(tableLayout, layoutParams);
    }

    private static final String APPLICATION_NS = "http://double-yellow.be";
    private void setValuesFromXml(AttributeSet attrs) {
        if ( attrs == null ) { return; }
        Resources resources = getResources();
        textSizePx      = resources.getInteger(attrs.getAttributeResourceValue(APPLICATION_NS, "textSizePx" , textSizePx));

        try {
            backgroundColor = attrs.getAttributeResourceValue(APPLICATION_NS, "backgroundColor", backgroundColor);
            textColor       = attrs.getAttributeResourceValue(APPLICATION_NS, "textColor"      , textColor);

            backgroundColor = resources.getColor(backgroundColor);
            textColor       = resources.getColor(textColor);
        } catch (Throwable e) {
        }
    }
    @ColorInt
    private int backgroundColor = -15197410;
    @ColorInt
    private int textColor       = -137216;

    private int textSizePx = 20;
/*
    public void setTextSizePx(int i) {
        if ( i != textSizePx) {
            textSizePx = i;
            this.update(false);
        }
    }
*/

    public void setProperties(int iBgColor, int iColor, int iTextSize) {
        backgroundColor = iBgColor;  // -15197410
        textColor       = iColor;    // -137216
        textSizePx      = iTextSize==0?textSizePx:iTextSize;

        update(false);
    }

    @Override public void addView(View child, ViewGroup.LayoutParams params) {
      //Log.i(TAG, "adding child");

        super.addView(child, params); // invoke second if a child is defined in the xml layout file
        if ( child.isInEditMode() ) {
            tableLayout = (GridLayout) child;
            history = new ArrayList<ScoreLine>();
            history.add(new ScoreLine("R1--"));
            history.add(new ScoreLine("L--1"));
            history.add(new ScoreLine("--R2"));
            history.add(new ScoreLine("-2L-"));

            update(false);
        }
    }
}
