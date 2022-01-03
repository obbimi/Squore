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
import androidx.annotation.ColorInt;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import com.doubleyellow.scoreboard.history.MatchGameScoresView;
import com.doubleyellow.scoreboard.model.*;
import com.doubleyellow.scoreboard.prefs.PreferenceKeys;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.scoreboard.prefs.ScorelineLayout;
import com.doubleyellow.util.DateUtil;
import com.doubleyellow.util.ListUtil;
import com.doubleyellow.util.MapUtil;
import com.doubleyellow.util.StringUtil;

import java.util.*;

/**
 * View that may display the scoring history of a single game. (Old fashioned paper sheet score)
 * This view is used in both
 * - the main view to show the history of the game in progress
 * - instantiated multiple times in the MatchHistory scoreBoard to give the scoring history of an entire match.
 *
 * @see MatchGameScoresView
 */
public class GameHistoryView extends ScrollView
{
    private static final String TAG = "SB." + GameHistoryView.class.getSimpleName();

    private List<ScoreLine> history           = new ArrayList<ScoreLine>();
    private HandicapFormat  handicapFormat    = HandicapFormat.None;
    private GameTiming      timing            = null;
    private ScorelineLayout m_scorelineLayout = ScorelineLayout.DigitsInside;

    public static void dontShowForToManyPoints(int iPoints) {
        if ( iPoints > 60 ) {
            PreferenceValues.setOverwrite(PreferenceKeys.showScoringHistoryInMainScreenOn, ""); // prevent OutOfMemoryError in GameHistoryView.update()
            Log.w(TAG, "Disabled GameHistoryView for to many points in a single game " + iPoints);
        }
    }

    /** constructor used by android platform (if defined in xml?) */
    public GameHistoryView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public GameHistoryView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    @Override public void setOnClickListener(OnClickListener l) {
        super.setOnClickListener(l);
        tableLayout.setOnClickListener(l);
    }

    public void setScoreLines(List<ScoreLine> lScorelines, HandicapFormat handicapFormat, int iStartScoreA, int iStartScoreB) {
        this.handicapFormat = handicapFormat;

        // add line with the initial score of a game if it has a handicap (not 0-0)
        if ( handicapFormat.equals(HandicapFormat.None) == false && ListUtil.size(lScorelines) != 0) {
            lScorelines = new ArrayList<ScoreLine>(lScorelines);
            lScorelines.add(0, new ScoreLine( null, iStartScoreA, null, iStartScoreB));
        }
        this.history = new ArrayList<ScoreLine>(lScorelines); // to prevent 'java.util.ConcurrentModificationException' seen when using chrome cast
    }

    public void setTiming(GameTiming s) {
        this.timing = s;
    }

    private Map<Player,Integer> mGameStandingBefore = null;
    public void setGameStandingBefore(Map<Player, Integer> mGameStanding) {
        this.mGameStandingBefore = new HashMap<Player, Integer>(mGameStanding);
    }

    private Map<Player,Integer> mGameStandingAfter = null;
    public void setGameStandingAfter(Map<Player, Integer> mGameStanding) {
        this.mGameStandingAfter = new HashMap<Player, Integer>(mGameStanding);
    }

    private Map<Player,Integer> mGameEndScore = null;
    public void setGameEndScore(Map<Player, Integer> mGameEndScore) {
        this.mGameEndScore = new HashMap<Player, Integer>(mGameEndScore);
    }

    private final TableRow.LayoutParams  trLayoutParams = new TableRow .LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow   .LayoutParams.WRAP_CONTENT);
    private final ViewGroup.LayoutParams vgLayoutParams = new ViewGroup.LayoutParams(LayoutParams         .WRAP_CONTENT,             LayoutParams.WRAP_CONTENT);

    private boolean bAutoScrollDown = true;
    public void setAutoScrollDown(boolean b) {
        this.bAutoScrollDown = b;
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
        for (ScoreLine line: history)
        {
            TableRow tr = new TableRow(getContext());
            tr.setBackgroundColor(backgroundColor);
            tr.setLayoutParams(vgLayoutParams);

            List<String> saScore = line.toStringList(getContext());
            if ( (m_scorelineLayout != null) ) {
                if ( m_scorelineLayout.hideServeSide() ) {
                    saScore.remove(2);
                    saScore.remove(0);
                } else if ( m_scorelineLayout.swap34() ) {
                    ScoreLine.swap(saScore, 3);
                }
            }
            for(String sValue: saScore) {
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
                tv.setTextColor(textColor);
                tv.setText(sValue);
                tv.setGravity(Gravity.CENTER);
              //tv.setLayoutParams(vgLayoutParams);
                tv.setLayoutParams(trLayoutParams);
                tr.addView(tv, trLayoutParams);
            }

            tableLayout.addView(tr, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        }

        if ( MapUtil.isNotEmpty(this.mGameEndScore) ) {
            String sValue = MapUtil.getInt(this.mGameEndScore, Player.A, 0) + " - " + MapUtil.getInt(this.mGameEndScore, Player.B, 0);
            addRowInversedColors(sValue, Gravity.START);
        }

        if ( MapUtil.isNotEmpty(this.mGameStandingAfter) ) {
            String sValue = MapUtil.getInt(this.mGameStandingAfter, Player.A, 0) + " - " + MapUtil.getInt(this.mGameStandingAfter, Player.B, 0);
            addRowInversedColors(sValue, Gravity.END);
        }

        // add timing
        if ( (history.size() > 0) && (this.timing != null) ) {
            // game start time - end time
            String sValue  = timing.getStartHHMM();
            if ( StringUtil.isNotEmpty(sValue) ) {
                addRow(sValue, Gravity.START);
                sValue = timing.getEndHHMM();
                addRow(sValue, Gravity.END);
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
            addRowInversedColors(sTime, Gravity.END);
        }

        if ( this.bAutoScrollDown ) {
            scrollDown();
        }
    }

    public void scrollDown() {
        this.post(new Runnable() {
            @Override public void run() {
                GameHistoryView.this.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    private final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

    private void addRow(String sValue, int iGravity) {
        addRow(sValue, iGravity, backgroundColor, textColor);
    }
    private void addRowInversedColors(String sValue, int iGravity) {
        addRow(sValue, iGravity, textColor, backgroundColor);
    }

    /**
     * for adding headers and footers with
     * - duration of game
     * - final game score
     */
    private void addRow(String sValue, int iGravity, int iBgColor, int iTxtColor) {
        TableRow.LayoutParams trTimingLayoutParams = new TableRow .LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow   .LayoutParams.WRAP_CONTENT);
        trTimingLayoutParams.span = 4;

        TableRow tr = new TableRow(getContext());
        tr.setBackgroundColor(iBgColor);
        tr.setLayoutParams(vgLayoutParams);

        TextView tv = new TextView(getContext());
        tv.setTextColor(iTxtColor);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizePx);
        tv.setText(sValue);
        tv.setGravity(iGravity);
        tv.setLayoutParams(trTimingLayoutParams);
        tr.addView(tv, trTimingLayoutParams);

        tableLayout.addView(tr, layoutParams);
    }

    private TableLayout tableLayout = null;
    private void init(Context context, AttributeSet attrs) {
        setValuesFromXml(attrs);
        m_scorelineLayout = PreferenceValues.getScorelineLayout(context);

        super.setScrollBarStyle(SCROLLBARS_INSIDE_OVERLAY);
        tableLayout = new TableLayout(context);
        tableLayout.setGravity(Gravity.CENTER);
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        this.addView(tableLayout, layoutParams);
        if ( isInEditMode() ) {
            initPreview(tableLayout);
            //throw new RuntimeException("Temp test");
        }
    }

    private static final String APPLICATION_NS = "http://double-yellow.be";
    private void setValuesFromXml(AttributeSet attrs) {
        if ( attrs == null ) { return; }
        Resources resources = getResources();

        try {
            iNrOfPreviewLines=attrs.getAttributeIntValue(APPLICATION_NS, "nrOfPreviewLines" , iNrOfPreviewLines);
            textSizePx      = resources.getInteger(attrs.getAttributeResourceValue(APPLICATION_NS, "textSizePx" , textSizePx));
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
    public void setTextSizePx(int i) {
        if ( i != textSizePx) {
            textSizePx = i;
            this.update(false);
        }
    }

    public void setProperties(int iBgColor, int iColor, int iTextSize) {
        backgroundColor = iBgColor;  // -15197410
        textColor       = iColor;    // -137216
        textSizePx      = iTextSize==0?textSizePx:iTextSize;

        update(false);
    }

    private int iNrOfPreviewLines = 10;
    private void initPreview(TableLayout child) {
        setAutoScrollDown(false);
        tableLayout = child;
        history = new ArrayList<ScoreLine>();
        int[] iaScores = new int[] {0,0};
        Player pPrev = Player.A;
        ServeSide[] serveSide = new ServeSide[] { ServeSide.R, null};
        for ( int i=0; i< iNrOfPreviewLines; i++ ) {
            int i0or1 = (int) Math.round(Math.random());
            Player pScorer = Player.values()[i0or1];
            iaScores[i0or1]++;
            ScoreLine sl = new ScoreLine(serveSide[0], i0or1==0?iaScores[0]:null, serveSide[1], i0or1==1?iaScores[1]:null);
            history.add(sl);

            serveSide[i0or1] = serveSide[i0or1]==null?ServeSide.R:serveSide[i0or1].getOther();
            if ( pScorer.equals(pPrev) == false ) {
                serveSide[pScorer.getOther().ordinal()] = null;
            }
            pPrev = pScorer;
        }

        update(false);
    }

    public void setStretchAllColumns(boolean bValue) {
        tableLayout.setStretchAllColumns(bValue); // not good for MatchHistory
    }
}
