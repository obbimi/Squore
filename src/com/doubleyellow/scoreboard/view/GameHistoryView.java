package com.doubleyellow.scoreboard.view;

import android.content.Context;
import android.support.annotation.ColorInt;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.doubleyellow.scoreboard.model.*;
import com.doubleyellow.util.DateUtil;
import com.doubleyellow.util.ListUtil;
import com.doubleyellow.util.MapUtil;
import com.doubleyellow.util.StringUtil;

import java.util.*;

/**
 * View that may display the scoring history of a single game. (Old fashioned paper sheet score)
 * This view is used in both
 * - the main view to show the history of the game in progress
 * - instanciated multiple times in the MatchHistory scoreBoard to give the scoring history of an entire match.
 */
public class GameHistoryView extends ScrollView
{
    private static final String TAG = "SB." + GameHistoryView.class.getSimpleName();

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

    private List<ScoreLine> history        = new ArrayList<ScoreLine>();
    private HandicapFormat  handicapFormat = HandicapFormat.None;
    public void setScoreLines(List<ScoreLine> lScorelines, HandicapFormat handicapFormat, int iStartScoreA, int iStartScoreB) {
        this.handicapFormat = handicapFormat;

        // add line with the initial score of a game if it has a handicap (not 0-0)
        if ( handicapFormat.equals(HandicapFormat.None) == false && ListUtil.size(lScorelines) != 0) {
            lScorelines = new ArrayList<ScoreLine>(lScorelines);
            lScorelines.add(0, new ScoreLine( null, iStartScoreA, null, iStartScoreB));
        }
        this.history = new ArrayList<ScoreLine>(lScorelines); // TO prevent 'java.util.ConcurrentModificationException' seen when using chrome cast
    }

    private GameTiming timing = null;
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
    private final ViewGroup.LayoutParams vgLayoutParams = new ViewGroup.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

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
            for(String sValue: saScore) {
                TextView tv = new TextView(getContext());
                if ( line.isCall() || line.isBrokenEquipment() ) {
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

    private void addRow(String sValue, int iGravity) {
        addRow(sValue, iGravity, backgroundColor, textColor);
    }
    private void addRowInversedColors(String sValue, int iGravity) {
        addRow(sValue, iGravity, textColor, backgroundColor);
    }
    private final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
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

        super.setScrollBarStyle(SCROLLBARS_INSIDE_OVERLAY);
        tableLayout = new TableLayout(context);
        tableLayout.setGravity(Gravity.CENTER);
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        this.addView(tableLayout, layoutParams);
    }

    private static final String APPLICATION_NS = "http://double-yellow.be";
    private void setValuesFromXml(AttributeSet attrs) {
        if ( attrs == null ) { return; }
        textSizePx      = getResources().getInteger(attrs.getAttributeResourceValue(APPLICATION_NS, "textSizePx" , textSizePx));

        try {
            backgroundColor = attrs.getAttributeResourceValue(APPLICATION_NS, "backgroundColor", backgroundColor);
            textColor       = attrs.getAttributeResourceValue(APPLICATION_NS, "textColor"      , textColor);

            backgroundColor = getResources().getColor(backgroundColor);
            textColor       = getResources().getColor(textColor);
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

    @Override public void addView(View child, ViewGroup.LayoutParams params) {
      //Log.i(TAG, "adding child");

        super.addView(child, params); // invoke second if a child is defined in the xml layout file
        if ( child.isInEditMode() ) {
            setAutoScrollDown(false);
            tableLayout = (TableLayout) child;
            history = new ArrayList<ScoreLine>();
            history.add(new ScoreLine("R1--"));
            history.add(new ScoreLine("L--1"));
            history.add(new ScoreLine("--R2"));
            history.add(new ScoreLine("-2L-"));

            update(false);
        }
    }

    public void setStretchAllColumns(boolean bValue) {
        tableLayout.setStretchAllColumns(bValue); // not good for MatchHistory
    }
}
