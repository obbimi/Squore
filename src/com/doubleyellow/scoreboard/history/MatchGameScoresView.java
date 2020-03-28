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

import android.graphics.Color;
import android.os.Build;
import android.os.CountDownTimer;
import androidx.annotation.ColorInt;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseIntArray;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.doubleyellow.android.view.AutoResizeTextView;
import com.doubleyellow.android.view.Orientation;
import com.doubleyellow.android.view.ViewUtil;
import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.model.*;
import com.doubleyellow.android.util.ColorUtil;
import com.doubleyellow.scoreboard.prefs.GameScoresAppearance;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.scoreboard.prefs.ShowCountryAs;
import com.doubleyellow.util.ListUtil;
import com.doubleyellow.util.MapUtil;
import com.doubleyellow.util.StringUtil;

import java.util.*;

/**
 * View class to draw the end score of all games that have already ended e.g.
 * 11 -  5
 *  6 - 11
 * 12 - 10
 * 11 -  8
 *
 * The view has two different 'orientations'.
 * One is used in landscape mode of the ScoreBoard scoreBoard, one in portrait orientation.
 *
 * It is used both in the main scoreboard (without names) as well as in the timer popup (with player names and timing info) and 'End Of Game' view
 */
public class MatchGameScoresView extends LinearLayout
{
    private static final String TAG = "SB." + MatchGameScoresView.class.getSimpleName();

/*
    public MatchGameScoresView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs);
    }

    public MatchGameScoresView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        this(context, attrs);
    }
*/

    /** constructor used by android platform (if defined in xml) */
    public MatchGameScoresView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public MatchGameScoresView(Context context) {
        this(context, null);
    }

    private List<GameTiming>           m_gameTimes     = null;
    private String[]                   m_playerNames   = null;
    private String[]                   m_countries     = null;
    private List<Map<Player, Integer>> m_gamesScores   = null;
    private String                     m_eventDivision = null;
    private Map<Player, Integer>       m_pointsDiff    = null;
    private Player[]                   m_players       = Player.values(); // must be initialized with a value in order to let setProperties() succeed

    public ScoresToShow updateSetScoresToShow(boolean bChange) {
        if ( bChange ) {
            switch (m_scoresToShow) {
                case GamesWonPerSet:
                    m_scoresToShow = ScoresToShow.SetsWon_and_GamesWonInLastSet;
                    break;
                case SetsWon_and_GamesWonInLastSet:
                    m_scoresToShow = ScoresToShow.GamesWonPerSet;
                    break;
            }
        }
        if ( m_scoresToShow.equals(ScoresToShow.PointsWonPerGame) ) {
            // switch over to GSM valid value
            m_scoresToShow = ScoresToShow.GamesWonPerSet;
        }
        return m_scoresToShow;
    }
    private ScoresToShow m_scoresToShow = ScoresToShow.PointsWonPerGame; // TODO: initial value from persisted setting
    public enum ScoresToShow {
        /* Non GSM match format */
        PointsWonPerGame,
        /* GSM match format */
        GamesWonPerSet,
        SetsWon_and_GamesWonInLastSet,
    }
    public void update(Model matchModel, Player pFirst) {
        m_checkLayoutCountDownTimer = null; // ensure it will be re-started
        m_players = new Player[] { pFirst, pFirst.getOther() };
        List<Map<Player, Integer>> endScores = matchModel.getEndScoreOfPreviousGames();
        if ( Brand.isGameSetMatch() ) {
            GSMModel gsmModel = (GSMModel) matchModel;
            if ( m_scoresToShow.equals(ScoresToShow.PointsWonPerGame) ) {
                // switch over to GSM valid value
                m_scoresToShow = ScoresToShow.GamesWonPerSet; // TODO: configurable
            }
            if ( m_scoresToShow.equals(ScoresToShow.GamesWonPerSet) ) {
                endScores = gsmModel.getGamesWonPerSet();
            } else {
                endScores = new ArrayList<>();
                Map<Player, Integer> setsWon  = gsmModel.getSetsWon();
                Map<Player, Integer> gamesWon = gsmModel.getGamesWon();
                endScores.add(setsWon);
                endScores.add(gamesWon);
            }
        }
        this.update( m_players
                   , endScores
                   , matchModel.getPlayerNames(true, true, m_players)
                   , matchModel.getCountries(m_players)
                   , matchModel.getTimes()
                   , matchModel.getEventDivision()
                   , matchModel.getPointsDiff(false)
                   );
    }

/*
    @Override public void refreshDrawableState() {
        startCheckLayoutTimer(); // required for decent layout in EndOfGameView ?!

        super.refreshDrawableState();
    }
*/

    private void startCheckLayoutTimer() {
        if ( m_checkLayoutCountDownTimer == null ) {
          //Log.d(TAG, "Creating new layout timer...");
            m_checkLayoutCountDownTimer = new CheckLayoutCountDownTimer(this);
          //Log.d(TAG, "Starting check layout timer...");
            m_checkLayoutCountDownTimer.start();
        }
    }

    /*
    @Override public void setVisibility(int visibility) {
        super.setVisibility(visibility);

        if ( checkLayoutCountDownTimer != null ) {
            checkLayoutCountDownTimer.m_iLayoutOKCount = 0;
        }
    }
*/

    private void update(Player[] players, List<Map<Player, Integer>> gamesScores, String[] playerNames, String[] countries, List<GameTiming> gameTimes, String sDivisionOrField, Map<Player, Integer> mPointsDiff)
    {
        m_gamesScores   = gamesScores;
        m_playerNames   = playerNames;
        m_countries     = countries;
        m_gameTimes     = gameTimes;
        m_eventDivision = sDivisionOrField;
        m_pointsDiff    = mPointsDiff;

        int iMaxScore = PreferenceValues.numberOfPointsToWinGame(getContext());
        if ( gamesScores != null ) {
            for (Map<Player, Integer> m : gamesScores) {
                iMaxScore = Math.max(iMaxScore, MapUtil.getMaxValue(m));
            }
        }
        iMaxScore *= 2;

        Orientation currentOrientation = ViewUtil.getCurrentOrientation(getContext());
        int instanceKey = this.getId() + (100 * currentOrientation.ordinal()) + (1 * 10000 * ListUtil.size(gamesScores)) + iMaxScore;
        if ( ListUtil.size(gamesScores) == 0 ) {
            // add dummy row if no previous games, to have layout be more as it will be after one or more sets
            gamesScores = new ArrayList<Map<Player, Integer>>();

            HashMap<Player, Integer> map = new HashMap<Player, Integer>();
            map.put(Player.A, 0);
            map.put(Player.B, 0);
            gamesScores.add(map);

            instanceKey = currentOrientation.ordinal();
        }

        if ( m_leftToRight ) {
            updateLeft2Right(players, gamesScores, instanceKey, iMaxScore, 0);
        } else {
            updateTop2Bottom(players, gamesScores, instanceKey, iMaxScore);
        }
/*
        super.addOnLayoutChangeListener(new OnLayoutChangeListener() {
            @Override public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if ( v instanceof MatchGameScoresView ) {
                    checkAutoResizeTextViews(v);
                }
            }
        });
*/
        if ( super.isInEditMode() ) {
            // no layout checking?
            this.setVisibility(VISIBLE);
        } else {
            if ( ( m_checkLayoutCountDownTimer == null) || ( m_checkLayoutCountDownTimer.m_iLayoutOKCount == 0) ) {
                startCheckLayoutTimer();
            }
        }
    }
    private CheckLayoutCountDownTimer m_checkLayoutCountDownTimer = null;

    private static int [] checkAutoResizeTextViews(ViewGroup vg) {
        int[] iaTotal_0NOK_1OK_2NOKCnt = new int[4];
        for(int c=0; c < vg.getChildCount(); c++) {
            View vc = vg.getChildAt(c);
            if ( vc instanceof ViewGroup == false ) {
                Log.d(TAG, "Not counting for view 1 " + vc);
                continue;
            }
            ViewGroup vgc = (ViewGroup) vc;
            // e.g. 11-5, 11-7 score like may be hidden
            for ( int c2=0; c2 < vgc.getChildCount(); c2++) {
                View vcc = vgc.getChildAt(c2);
                if ( vcc instanceof AutoResizeTextView) {
                    AutoResizeTextView artv = (AutoResizeTextView) vcc;
                    int lineCount = artv.getLineCount();
                    iaTotal_0NOK_1OK_2NOKCnt[0]++;
                    switch(lineCount) {
                        case 0:
                          //Log.d(TAG, "Not just 1 line: " + lineCount + " (" + artv + ")");
                            iaTotal_0NOK_1OK_2NOKCnt[1]++;
                            break;
                        case 1:
                            iaTotal_0NOK_1OK_2NOKCnt[2]++;
                            break;
                        default:
                            //Log.d(TAG, "Not just 1 line: " + lineCount + " (" + artv + ")");
                            iaTotal_0NOK_1OK_2NOKCnt[3]++;
                            break;
                    }
                } else {
                    Log.d(TAG, "Not counting for view 2 " + vcc);
                }
            }
        }
        return iaTotal_0NOK_1OK_2NOKCnt;
    }

/*
    @Override public void setVisibility(int visibility) {
        if ( visibility == View.VISIBLE ) {
            Log.d(TAG, "Back to visible");
        }
        super.setVisibility(visibility);
    }
*/

    private class CheckLayoutCountDownTimer extends CountDownTimer {
        private MatchGameScoresView gsv    = null;
        private int m_iLayoutOKCount       = 0;
        private int m_iLayoutNOKCount      = 0;
        private int m_iRestoreVisibilityTo = 0;
        private CheckLayoutCountDownTimer(MatchGameScoresView gsv) {
            //super(640, 320); // in racketlon it sometimes does not show...
            super(2000, 300);
          //Log.d(TAG, "MGSV new CheckLayoutCountDownTimer :" + this);
            this.gsv = gsv;
            this.gsv.setVisibility(View.INVISIBLE);
            View parent = (View) gsv.getParent();
            while( parent.getParent() instanceof View ) {
                parent = (View) parent.getParent();
            }
          //Log.d(TAG, "MGSV parent :" + parent);
            View sbRootView = parent.findViewById(R.id.squoreboard_root_view);
          //Log.d(TAG, "MGSV parent :" + sbRootView);
            this.m_iRestoreVisibilityTo = View.VISIBLE;
            GameScoresAppearance gameScoresAppearance = PreferenceValues.getGameScoresAppearance(getContext());
            if ( sbRootView == null ) {
                // not on main scoreboard
                this.m_iRestoreVisibilityTo = View.VISIBLE;
            } else if ( gameScoresAppearance.showGamesWon(isPresentation()) ) {
                this.m_iRestoreVisibilityTo = View.INVISIBLE;
            }
        }
        @Override public void onTick(long millisUntilFinished) {
            int[] iaTotal_0NOK_1OK_2NOKCnt = checkAutoResizeTextViews(MatchGameScoresView.this);
            if ( iaTotal_0NOK_1OK_2NOKCnt[1] + iaTotal_0NOK_1OK_2NOKCnt[3] > 0 ) {
                m_iLayoutNOKCount++;
                Log.w(TAG, String.format("Trigger update, resize textview not all properly layed out : %d are 0NOK, %d are OK, %d are 2NOK of %d (ms left: %d)", iaTotal_0NOK_1OK_2NOKCnt[1], iaTotal_0NOK_1OK_2NOKCnt[2], iaTotal_0NOK_1OK_2NOKCnt[3], iaTotal_0NOK_1OK_2NOKCnt[0], millisUntilFinished));
                gsv.update(m_players, gsv.m_gamesScores, gsv.m_playerNames, m_countries, gsv.m_gameTimes, m_eventDivision, m_pointsDiff);
                //this.cancel();
            } else {
                m_iLayoutOKCount++;
              //Log.d(TAG, String.format("No layout update required : %d are 0NOK, %d are OK, %d are 2NOK of %d (ms left: %d)", iaTotal_0NOK_1OK_2NOKCnt[1], iaTotal_0NOK_1OK_2NOKCnt[2], iaTotal_0NOK_1OK_2NOKCnt[3], iaTotal_0NOK_1OK_2NOKCnt[0], millisUntilFinished));
                if ( m_iLayoutOKCount >= 1 ) {
                    this.gsv.setVisibility(m_iRestoreVisibilityTo); // still makes the 'recalculations' period visible to the eye
                    this.cancel();
                }
            };
        }
        @Override public void onFinish() {
          //Log.d(TAG, String.format("Layout timer finished (%d)", m_iLayoutOKCount));
            if ( this.gsv != null ) {
                this.gsv.setVisibility(m_iRestoreVisibilityTo);

                //Log.d(TAG, String.format("Invalidating gsv as a test "));
                //this.gsv.invalidate();
            }
        }
    };

    private final ViewGroup.LayoutParams lpT2B_wmp_hwc = new ViewGroup.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT);
    private /*static*/ SparseIntArray mInstanceOrientation2TextSize  = new SparseIntArray(); // can not be static... used on different displays
    private /*static*/ SparseIntArray mInstanceOrientation2ColWidth  = new SparseIntArray();
    private /*static*/ SparseIntArray mInstanceOrientation2RowHeight = new SparseIntArray();

/*
    public int getTextSizePx() {
        Orientation currentOrientation = ViewUtil.getCurrentOrientation(getContext());
        int instanceKey = this.getId() + (100 * currentOrientation.ordinal()) + (1 * 10000 * ListUtil.size(null)) + 0;
        int i = mInstanceOrientation2TextSize.get(instanceKey);
        if ( i == 0 ) {
            i = mInstanceOrientation2TextSize.indexOfValue(0);
        }
        return i;
    }
*/

    private AutoResizeTextView.OnTextResizeListener onTextResizeListener = null;
    /** iBoard may register a listener */
    public void setOnTextResizeListener(AutoResizeTextView.OnTextResizeListener l) {
        this.onTextResizeListener = l;
    }

    /**
     * 11 |  9
     * -------
     *  8 | 11
     * -------
     *  3 | 11
     */
    private void updateTop2Bottom(final Player[] players, final List<Map<Player, Integer>> gameScores, final int instanceKey, final int iMaxScore) {
        final String sMethod = "updateTop2Bottom: ";

        this.removeAllViews(); // start all over

        int iTxtSizeForInstanceAndOrientation = mInstanceOrientation2TextSize.get(instanceKey);
        boolean bOrientationFirstTime = (iTxtSizeForInstanceAndOrientation == 0);
        if ( isInEditMode() && iTxtSizeForInstanceAndOrientation == 0 ) {
            iTxtSizeForInstanceAndOrientation = 20;
            bOrientationFirstTime = false;
        }

        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        super.setOrientation(VERTICAL);

      //final List<ViewGroup> rows = new ArrayList<ViewGroup>();
        int iRow = 0;
        for ( Map<Player, Integer> scores: gameScores ) {
            iRow++;
            Player gameWinner = Util.getWinner(scores);

            final RelativeLayout tr = (RelativeLayout) inflater.inflate(R.layout.scores_horizontal, null);
            tr.setBackgroundColor(bgColorLoser);

            AutoResizeTextView txtMax = null;
            for (Player p: players) {
                boolean  bLeftColumn = p.equals(players[0]);
                int      iResId      = bLeftColumn ? R.id.score_player_1 : R.id.score_player_2;
                TextView txt         = (TextView) tr.findViewById(iResId);
                boolean bInvert = p.equals(gameWinner);
                if ( Brand.isGameSetMatch() ) {
                    switch (m_scoresToShow) {
                        case GamesWonPerSet:
                            if (iRow == ListUtil.size(gameScores) ) {
                                bInvert = false;
                            }
                            break;
                        case SetsWon_and_GamesWonInLastSet:
                            bInvert = (iRow == ListUtil.size(gameScores)); // do not invert for nr of sets won (row 1), invert for both players for games in current set (row 2)
                            break;
                    }
                }
                setSizeAndColors(txt, bInvert, iTxtSizeForInstanceAndOrientation, instanceKey);
                //txt.setText(StringUtil.pad(String.valueOf(scores.get(p)), ' ', 3, bLeftColumn) );
                txt.setText(String.valueOf(scores.get(p)) );

                if ( txtMax == null || scores.get(p) > scores.get(p.getOther()) ) {
                    txtMax = (AutoResizeTextView) txt;
                }
            }

/*
            TextView dash = (TextView) tr.findViewById(R.id.score_dash);
            setSizeAndColors(dash, false, iTxtSizeForInstanceAndOrientation, instanceKey);
*/
            final float fSpacingFactor = 1.00f;
            if ( bOrientationFirstTime ) {
                // for first row add a listener to determine row height for the rest
                txtMax.setText(String.valueOf(iMaxScore));
                txtMax.addOnResizeListener(new AutoResizeTextView.OnTextResizeListener() {
                    @Override public void onTextResize(AutoResizeTextView textView, float oldSize, float newSize, float requiredWidth, float requiredHeight) {
                        requiredHeight = requiredHeight * fSpacingFactor;
                        mInstanceOrientation2TextSize .put(instanceKey, (int) newSize);
                        //Log.d(TAG, sMethod + String.format("mInstanceOrientation2TextSize: %s %s", instanceKey, newSize));
                        mInstanceOrientation2RowHeight.put(instanceKey, (int) requiredHeight);
                        lpT2B_wmp_hwc.height = (int) requiredHeight; // for subsequent rows to be added

                        // redraw all rows now knowing how 'high' the row must be
                        MatchGameScoresView.this.updateTop2Bottom(players, gameScores, instanceKey, iMaxScore);
                    }
                });
                txtMax.addOnResizeListener(onTextResizeListener);
            } else {
                int height = mInstanceOrientation2RowHeight.get(instanceKey);
                if ( height == 0 && isInEditMode() ) {
                    height = 20;
                }
                lpT2B_wmp_hwc.height = height;
            }

            //Log.d(TAG, sMethod + String.format("lpT2B_wmp_hwc width,height: %s %s", lpT2B_wmp_hwc.width, lpT2B_wmp_hwc.height));

            super.addView(tr, lpT2B_wmp_hwc);
            //rows.add(tr);
            if ( bOrientationFirstTime ) {
                break; // only one row... wait for calculation of rowheight before redrawing other rows
            }
        }

        if ( (bOrientationFirstTime == false)
           && Brand.isRacketlon()
           && MapUtil.isNotEmpty(m_pointsDiff)
           && ListUtil.isNotEmpty(gameScores)
           ) {
            final RelativeLayout tr = (RelativeLayout) inflater.inflate(R.layout.scores_horizontal, null);
            tr.setBackgroundColor(bgColorLoser);

            Player pLeader = MapUtil.getMaxKey(m_pointsDiff, Player.B);
            if ( pLeader != null ) {
                Integer iDiff = m_pointsDiff.get(pLeader);
                if ( iDiff != null ) {
                    boolean  bLeftColumn     = pLeader.equals(players[0]);
                    int      iResId          = bLeftColumn ? R.id.score_player_1 : R.id.score_player_2;
                    TextView txt             = (TextView) tr.findViewById(iResId);
                    float    fScaleDownField = 0.7f;
                    setSizeAndColors(txt, true, iTxtSizeForInstanceAndOrientation * fScaleDownField, instanceKey);
                    txt.setText("+" + String.valueOf(iDiff) );

                    super.addView(tr, lpT2B_wmp_hwc);
                }
            }
        }
    }
    private final ViewGroup.LayoutParams lpL2R_wwc_hmp = new ViewGroup.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT);

    /**
     * ------------
     * | Field 2  |
     * ---------------------------
     * | Harry    | 11 |  4 | 11 |
     * ---------------------------
     * | Potter   |  9 | 11 |  8 |
     * ---------------------------
     * | Division | '8 | '7 | '9 |
     * ---------------------------
     */
    private void updateLeft2Right(final Player[] players, final List<Map<Player, Integer>> gameScores, final int instanceKey, final int iMaxScore, final int iCounter) {
        final String sMethod = "updateLeft2Right: ";

        this.removeAllViews(); // start all over

        int iTxtSizeForInstanceAndOrientation = mInstanceOrientation2TextSize.get(instanceKey);
        boolean bOrientationFirstTime = iTxtSizeForInstanceAndOrientation == 0;
        if ( isInEditMode() && iTxtSizeForInstanceAndOrientation == 0 ) {
            iTxtSizeForInstanceAndOrientation = 20;
            bOrientationFirstTime = false;
        }

        int iResIdImages  = 0;
        int iResIdInflate = R.layout.scores_vertical;
        if ( m_showTimes && ( m_gameTimes != null ) ) {
            iResIdInflate = R.layout.scores_vertical_times;
            iResIdImages  = R.layout.scores_vertical_countryflags;
        }

        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        super.setOrientation(HORIZONTAL);

        final int iTransparent = getResources().getColor(android.R.color.transparent);
        if ( m_showNames && (m_playerNames != null) ) {

            // add flags in separate inflated layout
            EnumSet<ShowCountryAs> countryPref = PreferenceValues.showCountryAs(getContext());
            if ( true && ListUtil.isNotEmpty(countryPref) ) {
                boolean bAddFlags = (countryPref.contains(ShowCountryAs.FlagNextToNameChromeCast) && (isPresentation()==true ))
                                 || (countryPref.contains(ShowCountryAs.FlagNextToNameOnDevice  ) && (isPresentation()==false));
                if ( bAddFlags && ListUtil.length(m_countries) > 0 ) {

                    final RelativeLayout colImg = (RelativeLayout) inflater.inflate(iResIdImages, null);
                    colImg.setBackgroundColor(iTransparent);
                    for (Player p : players) {
                        String sCountryCode = m_countries[p.ordinal()];
                        if (StringUtil.isEmpty(sCountryCode) ) { continue; }

                        boolean   bTopRow = p.equals(players[0]);
                        int       iResId  = bTopRow ? R.id.flag_player_1 : R.id.flag_player_2;
                        ImageView img     = (ImageView) colImg.findViewById(iResId);
                        //img.setImageResource(R.drawable.logo);
                        img.setScaleType(ImageView.ScaleType.FIT_XY);
                        img.setBackgroundResource(R.drawable.image_border);
                        PreferenceValues.downloadImage(getContext(), img, sCountryCode);
                    }
                    super.addView(colImg, new ViewGroup.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.MATCH_PARENT));
                }
            }

            // add the names of the players
            final RelativeLayout colPlayers = (RelativeLayout) inflater.inflate(iResIdInflate, null);
            colPlayers.setBackgroundColor(iTransparent);
            for(Player p: players) {
                boolean            bTopRow = p.equals(players[0]);
                int                iResId  = bTopRow ? R.id.score_player_1 : R.id.score_player_2;
                AutoResizeTextView txt     = (AutoResizeTextView) colPlayers.findViewById(iResId);
                setSizeAndColors(txt, false, iTxtSizeForInstanceAndOrientation, instanceKey);

                String sPlayerName = m_playerNames[p.ordinal()];
                if ( countryPref.contains(ShowCountryAs.AbbreviationAfterName) == false ) {
                    sPlayerName = Util.removeCountry(sPlayerName);
                }
                txt.setText           (sPlayerName);
                txt.setBackgroundColor(bgColorPlayer);
                txt.setTextColor      (txtColorPlayer);

                txt.addOnResizeListener(new AutoResizeTextView.OnTextResizeListener() {
                    @Override public void onTextResize(AutoResizeTextView textView, float oldSize, float newSize, float requiredWidth, float requiredHeight) {
                        ViewGroup.LayoutParams params = colPlayers.getLayoutParams();
                        params.width = Math.max(params.width, (int) (requiredWidth * 1.1) ); // 1.1 for a bit of spacing
                        colPlayers.setLayoutParams(params);
                    }
                });
            }
            // initialize the cell underneath the playernames as 'empty' (no text and transparent)
            TextView time = (TextView) colPlayers.findViewById(R.id.score_time);
            if ( time != null ) {
                if ( StringUtil.isNotEmpty(m_eventDivision) && PreferenceValues.showFieldDivision(getContext(), isPresentation()) && m_showTimes ) {
                    float fScaleDownField = 0.7f;
                    if ( StringUtil.size(m_eventDivision) <= 2 ) {
                        m_eventDivision = getResources().getString(R.string.lbl_division) + " " + m_eventDivision;
                    }
                    time.setText(m_eventDivision);
                    setSizeAndColors(time, true, iTxtSizeForInstanceAndOrientation * fScaleDownField, instanceKey);
                } else {
                    time.setText("");
                    time.setBackgroundColor(iTransparent);
                }
            }
/*
            TextView dash = (TextView) tr.findViewById(R.id.score_dash);
            setSizeAndColors(dash, false, iTxtSizeForInstanceAndOrientation, instanceKey);
*/
            super.addView(colPlayers, new ViewGroup.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT));
        }

        final List<ViewGroup> cols = new ArrayList<ViewGroup>(); // holds all columns

        // show end scores of all games
        int iRow = 0;
        for ( Map<Player, Integer> scores: gameScores ) {
            iRow++;
            Player gameWinner = Util.getWinner(scores);

            final RelativeLayout col = (RelativeLayout) inflater.inflate(iResIdInflate, null);
            col.setBackgroundColor(bgColorLoser);

            AutoResizeTextView txtMax = null;
            for(Player p: players) {
                boolean  bTopRow = p.equals(players[0]);
                int      iResId  = bTopRow ? R.id.score_player_1 : R.id.score_player_2;
                TextView txt     = (TextView) col.findViewById(iResId);
                boolean bInvert = p.equals(gameWinner);
                if ( Brand.isGameSetMatch() ) {
                    switch (m_scoresToShow) {
                        case GamesWonPerSet:
                            if (iRow == ListUtil.size(gameScores) ) {
                                bInvert = false;
                            }
                            break;
                        case SetsWon_and_GamesWonInLastSet:
                            bInvert = (iRow == ListUtil.size(gameScores)); // do not invert for nr of sets won (row 1), invert for both players for games in current set (row 2)
                            break;
                    }
                }
                setSizeAndColors(txt, bInvert, iTxtSizeForInstanceAndOrientation, instanceKey);
                //txt.setText(StringUtil.pad(String.valueOf(scores.get(p)), ' ', 2, bLeftColumn) );
                //txt.setText(" " + scores.get(p) + " "); // add a space just for spacing (on both sides for centering)
                txt.setText(String.valueOf(scores.get(p)));

                if ( txtMax == null || scores.get(p) > scores.get(p.getOther()) ) {
                    txtMax = (AutoResizeTextView) txt;
                }
            }

/*
            TextView dash = (TextView) tr.findViewById(R.id.score_dash);
            setSizeAndColors(dash, false, iTxtSizeForInstanceAndOrientation, instanceKey);
*/

            final float fSpacingFactor = 1.00f;
            if (bOrientationFirstTime && (txtMax != null) ) {
                // for first row add a listener to determine row height for the rest
                txtMax.setText(String.valueOf(iMaxScore));
                //if ( isInEditMode() ) throw new RuntimeException(sMethod + "adding on resize listener");
                txtMax.addOnResizeListener(new AutoResizeTextView.OnTextResizeListener() {
                    @Override public void onTextResize(AutoResizeTextView textView, float oldSize, float newSize, float requiredWidth, float requiredHeight) {
                        //if ( isInEditMode() && iCounter > 1 ) throw new RuntimeException(sMethod + "requiredWidth " + requiredWidth + " counter " + iCounter);
                        requiredWidth = requiredWidth * fSpacingFactor;
                        mInstanceOrientation2TextSize.put(instanceKey, (int) newSize);
                        //Log.d(TAG, sMethod + String.format("mInstanceOrientation2TextSize: %s %s", instanceKey, newSize));
                        mInstanceOrientation2ColWidth.put(instanceKey, (int) requiredWidth);
                        //if ( lpL2R_wwc_hmp.width < 0 ) {
                            lpL2R_wwc_hmp.width = (int) (requiredWidth); // for subsequent rows to be added

                            // redraw all rows now knowing how 'wide' the column must be
                            //if ( isInEditMode() == false ) {
                            updateLeft2Right(players, gameScores, instanceKey, iMaxScore, iCounter + 1);
                            //}
                        //}
                    }
                });
                txtMax.addOnResizeListener(onTextResizeListener);
            } else {
                Integer width = mInstanceOrientation2ColWidth.get(instanceKey);
                if ( width == 0 && isInEditMode() ) {
                    width = 20;
                }
                lpL2R_wwc_hmp.width = width;
            }

            //Log.d(TAG, sMethod + String.format("lpL2R_wwc_hmp width,height: %s %s", lpL2R_wwc_hmp.width, lpL2R_wwc_hmp.height));

            super.addView(col, lpL2R_wwc_hmp);
            cols.add(col);
            if ( bOrientationFirstTime ) {
                break; // only one col... wait for calculation of colwidth before redrawing other cols
            }
        }

        // add times to existing columns ... added before
        float fScaleDownTimes = 0.7f;
        if ( m_showTimes && (m_gameTimes!=null) && bOrientationFirstTime == false ) {
            for(int c=0; c < cols.size(); c++) {
                ViewGroup tr = cols.get(c);
                if (m_gameTimes.size() <= c ) {
                    continue;
                }

                GameTiming timing = m_gameTimes.get(c);
                int durationMM = timing.getDurationMM();
                if ( durationMM == 0 ) {
                    continue;
                }
                TextView tv = (TextView) tr.findViewById(R.id.score_time);
                String text = Math.abs(durationMM) + "'";
                tv.setText(text);
                if ( text.length() > 3 ) {
                    fScaleDownTimes = 0.4f;
                }
                if( tv instanceof AutoResizeTextView ) {
                    AutoResizeTextView artv = (AutoResizeTextView) tv;
                    artv.setMinMaxTextSize(iTxtSizeForInstanceAndOrientation * fScaleDownTimes);
                } else {
                    tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, iTxtSizeForInstanceAndOrientation * fScaleDownTimes);
                }
                tv.setBackgroundColor(bgColorTimes);
                tv.setTextColor(txtColorTimes);
            }
        }

        if ( (bOrientationFirstTime == false) && Brand.isRacketlon() && MapUtil.isNotEmpty(m_pointsDiff) && ListUtil.isNotEmpty(gameScores) ) {
            final RelativeLayout tr = (RelativeLayout) inflater.inflate(iResIdInflate, null);
            tr.setBackgroundColor(bgColorLoser);

            Player pLeader = MapUtil.getMaxKey(m_pointsDiff, Player.B);
            if ( pLeader != null ) {
                Integer iDiff = m_pointsDiff.get(pLeader);
                if ( iDiff != null ) {
                    boolean bLeftColumn = pLeader.equals(players[0]);
                    int iResId = bLeftColumn ? R.id.score_player_1 : R.id.score_player_2;
                    TextView txt = (TextView) tr.findViewById(iResId);
                    float fScaleDownField = 0.7f;
                    setSizeAndColors(txt, true, iTxtSizeForInstanceAndOrientation * fScaleDownField, instanceKey);
                    txt.setText("+" + String.valueOf(iDiff) );

                    super.addView(tr, lpL2R_wwc_hmp);
                }
            }
        }

/*
        if ( StringUtil.isNotEmpty(m_eventDivision) && PreferenceValues.showFieldDivision(getContext(), isPresentation()) && m_showTimes ) {
            TextView txt = new AutoResizeTextView(getContext());
            txt.setText(m_eventDivision);
            setSizeAndColors(txt, true, iTxtSizeForInstanceAndOrientation, instanceKey);
            RelativeLayout tr = new RelativeLayout(getContext());
            if ( tr != null ) {
                tr.setBackgroundColor(bgColorLoser);
                tr.addView(txt, new ViewGroup.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT));
                super.addView(tr, 0, new ViewGroup.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT));
            } else {
                super.addView(txt, 0, new ViewGroup.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT));
            }
        }
*/
    }

    //private final TableRow.LayoutParams lpL2R_mpwc           = new TableRow   .LayoutParams(TableRow   .LayoutParams.MATCH_PARENT, TableRow   .LayoutParams.WRAP_CONTENT);

    private void setSizeAndColors(TextView tv, boolean bInvert, float textSizePx, int instanceKey) {
        if ( tv == null ) { return; }
        if ( bInvert ) {
            tv.setTextColor      (bgColorLoser);
            tv.setBackgroundColor(bgColorWinner);
        } else {
            tv.setTextColor      (bgColorWinner);
            tv.setBackgroundColor(bgColorLoser);
        }
        if ( tv instanceof AutoResizeTextView ) {
            AutoResizeTextView artv = (AutoResizeTextView) tv;
            int iTextSize = mInstanceOrientation2TextSize.get(instanceKey);
            if ( iTextSize > 0 ) {
                //artv.setMinTextSize(iTextSize);
                //
                //artv.setMaxTextSize(iTextSize);
                //artv.setMinMaxTextSize(iTextSize);
                artv.setMaxTextSize(iTextSize);
            }
        } else {
            tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizePx);
        }
    }

    private void init(Context context, AttributeSet attrs) {
        setValuesFromXml(attrs);
        doIDEPreview();
        //super.setScrollBarStyle(0); // none?
    }

    private static final String APPLICATION_NS = "http://double-yellow.be";
    private void setValuesFromXml(AttributeSet attrs) {
        if ( attrs == null ) { return; }
        //textSizePx      = getResources().getInteger(attrs.getAttributeResourceValue(APPLICATION_NS, "textSizePx" , textSizePx));
        m_leftToRight   = attrs.getAttributeBooleanValue(APPLICATION_NS, "leftToRight", m_leftToRight);
        m_showNames     = attrs.getAttributeBooleanValue(APPLICATION_NS, "showNames"  , m_showNames);
        m_showTimes     = attrs.getAttributeBooleanValue(APPLICATION_NS, "showTimes"  , m_showTimes);

        try {
            bgColorLoser  = attrs.getAttributeResourceValue(APPLICATION_NS, "bgColorLoser" , bgColorLoser);
            bgColorWinner = attrs.getAttributeResourceValue(APPLICATION_NS, "bgColorWinner", bgColorWinner);

            if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M /* 23 */ ) {
                bgColorLoser  = getResources().getColor(bgColorLoser, null);
                bgColorWinner = getResources().getColor(bgColorWinner, null);
            }
        } catch (Throwable e) {
        }
    }

    private boolean m_leftToRight = false;
    private boolean m_showNames   = false;
    private boolean m_showTimes   = false;
    @ColorInt
    private int     bgColorPlayer = -11754535;
    @ColorInt
    private int     txtColorPlayer= Color.WHITE;
    @ColorInt
    private int     bgColorTimes  = -11754535;
    @ColorInt
    private int     txtColorTimes = Color.WHITE;
    private int     bgColorLoser  = -15197410;
    private int     bgColorWinner = -137216;

/*
    private int textSizePx = 36;
    public void setTextSizePx(int i) {
        textSizePx = i;
        //getResources().getInteger(R.integer.TextSizeHistory_default);
    }
*/

    public void setProperties(int iBgColorLoser, int iBgColorWinner, int ibgColorTimes, int iBgColorPlayer) {
        bgColorLoser  = iBgColorLoser;  // -15197410
        bgColorWinner = iBgColorWinner; // -137216
        bgColorTimes  = ibgColorTimes;  // -11754535
        bgColorPlayer = iBgColorPlayer;
        txtColorTimes = ColorUtil.getBlackOrWhiteFor(bgColorTimes);
        txtColorPlayer= ColorUtil.getBlackOrWhiteFor(bgColorPlayer);
        //textSizePx    = iTextSize==0? textSizePx :iTextSize;
        update(m_players, m_gamesScores, m_playerNames, m_countries, m_gameTimes, m_eventDivision, m_pointsDiff);
    }

    private void doIDEPreview() {
        if ( super.isInEditMode() ) {
            List<Map<Player, Integer>> gameScores = new ArrayList<Map<Player, Integer>>();

            Map<Player, Integer> pi;

            pi = new HashMap<Player, Integer>();
            pi.put(Player.A, 15);
            pi.put(Player.B, 13);
            gameScores.add(pi);

            pi = new HashMap<Player, Integer>();
            pi.put(Player.A, 3);
            pi.put(Player.B, 15);
            gameScores.add(pi);

            update(Player.values(), gameScores, new String[] { "Squash", "Referee" }, new String[] {"ENG", "EGY"}, Arrays.asList(new GameTiming(0, 0,7), new GameTiming(1, 0,6)), "Field 2", null);
            //throw new RuntimeException("Test only " + gameScores.toString());
            //throw new RuntimeException("Using " + com.doubleyellow.R.color.dy_yellow + " and " + com.doubleyellow.R.color.dy_dark);
        }
    }

    private Boolean bIsPresentation = null;
    private boolean isPresentation() {
        if ( bIsPresentation == null ) {
            bIsPresentation = getContext().getClass().getName().contains("android.app.Presentation");
        }
        return bIsPresentation;
    }
}
