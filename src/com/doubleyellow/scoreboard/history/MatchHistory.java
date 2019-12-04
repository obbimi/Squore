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

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.util.TypedValue;
import android.view.*;

import android.widget.LinearLayout;
import com.doubleyellow.android.view.ToggleResult;
import com.doubleyellow.android.view.ViewUtil;
import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.activity.IntentKeys;
import com.doubleyellow.scoreboard.activity.XActivity;
import com.doubleyellow.scoreboard.cast.EndOfGameView;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.model.Player;
import com.doubleyellow.scoreboard.prefs.ColorPrefs;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.scoreboard.timer.Timer;
import com.doubleyellow.scoreboard.timer.TimerView;
import com.doubleyellow.util.ListUtil;
import com.doubleyellow.util.MenuHandler;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/**
 * 'Activity' to show the entire scoring history of a match.
 * - Scoresheet (not for Racketlon)
 * - Statistics (not for Racketlon)
 * - Calls      (not for Racketlon)
 * - Graphs
 */
public class MatchHistory extends XActivity implements MenuHandler
{
    private static final String TAG = "SB." + MatchHistory.class.getSimpleName();
    
    private static Model               matchModel         = null;
    private static MatchHistoryView    matchHistoryView   = null;
    private static MatchCallsView      matchCallsView     = null;
    private static MatchStatisticsView matchStatisticsView= null;

    private ViewPager     viewPager  = null;
    private TabsAdapter   mAdapter   = null;
    private PagerTabStrip titleStrip = null;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.match_history);

        matchModel          = getMatchModel();
        matchHistoryView    = new MatchHistoryView   (this, matchModel);
        matchCallsView      = new MatchCallsView     (this, matchModel);
        matchStatisticsView = new MatchStatisticsView(this, matchModel);

        // Initialization
        viewPager  = (ViewPager) findViewById(R.id.mh_pager);
        titleStrip = ViewUtil.getFirstView(viewPager, PagerTabStrip.class);
        mAdapter   = new TabsAdapter(getFragmentManager());

        viewPager.setAdapter(mAdapter);
        titleStrip.setTextSize(TypedValue.COMPLEX_UNIT_DIP, getResources().getInteger(R.integer.TextSizeTabStrip) );
        ColorPrefs.setColor(titleStrip);

        viewPager.setOnPageChangeListener(new OnPageChangeListener());

        // always go to the last tab for current match, to third tab for stored/finished matches
        Player possibleMatchVictoryFor = matchModel.isPossibleMatchVictoryFor();
        int iActiveTab = (bIsStoredMatch || (possibleMatchVictoryFor != null)) ? 2 : mAdapter.getCount();
        viewPager.setCurrentItem(iActiveTab);

        ViewUtil.setFullScreen(getWindow(), PreferenceValues.showFullScreen(this));
        ScoreBoard.initAllowedOrientation(this);

        setHomeButtonEnabledOnActionBar();
        setActionBarVisibility(ScoreBoard.bUseActionBar== ToggleResult.setToTrue);
        Map<ColorPrefs.ColorTarget, Integer> mColors = ColorPrefs.getTarget2colorMapping(this);
        setActiorBarBGColor(mColors.get(ColorPrefs.ColorTarget.actionBarBackgroundColor));

        ScoreBoard.updateDemoThread(this);
        mhTimerView = new MHTimerView();
        Timer.addTimerView(false, mhTimerView);
    }

    MHTimerView mhTimerView = null;

    @Override public void onBackPressed() {
        if ( ScoreBoard.isInDemoMode() ) {
            ScoreBoard.demoThread.cancelDemoMessage();
        } else {
            Timer.removeTimerView(false, mhTimerView);
            super.onBackPressed();
        }
    }

    private class OnPageChangeListener extends ViewPager.SimpleOnPageChangeListener {
        @Override public void onPageSelected(int position) {
            if ( position == PreGraphTabs.MatchHistoryViewTab.ordinal() ) {
                matchHistoryView.scrollDownAll();
            }

            String sTitle = getTitle(position);
            MatchHistory.this.setTitle(sTitle);
            //super.onPageSelected(position);
        }
    }

    private String getTitle(int position) {
        String sTitle = MatchHistory.this.getString(R.string.sb_score_details);
        if ( preGraphTabsToShow().size() > 0 ) {
            if (position == PreGraphTabs.MatchHistoryViewTab.ordinal()) {
                sTitle = matchHistoryView.getTitle();
            } else if (position == PreGraphTabs.CallHistoryViewTab.ordinal()) {
                sTitle = matchCallsView.getTitle();
            } else if (position == PreGraphTabs.StatisticsViewTab.ordinal()) {
                sTitle = matchStatisticsView.getTitle();
            }
        }
        return sTitle;
    }

    private boolean bIsStoredMatch = false;
    private Model getMatchModel() {

        File file = null;

        // get file from intent: if present it is started from 'PreviousMatchSelector'
        Intent intent = getIntent();
        if ( intent != null ) {
            Bundle bundleExtra = intent.getBundleExtra(IntentKeys.MatchHistory.toString());
            if ( bundleExtra != null ) {
                Serializable serializable = bundleExtra.getSerializable(IntentKeys.MatchHistory.toString());
                if ( serializable instanceof File ) {
                    file = (File) serializable;
                    bIsStoredMatch = true;
                }
            }
        }

        // if no file found in the intent presume it is started from the main scoreboard
        if ( file == null ) {
            file = ScoreBoard.getLastMatchFile(this);
            bIsStoredMatch = false;
        }

        Model matchModel = null;
        try {
            matchModel = Brand.getModel();
            matchModel.fromJsonString(file);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return matchModel;
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        //MenuInflater inflater = this.getMenuInflater();
        //inflater.inflate(R.menu.closeonlymenu, menu); // we provide the 'Up' button via PARENT_ACTIVITY in AndroidManifest

        ScoreBoard.updateDemoThread(menu);

        return true;
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        return handleMenuItem(item.getItemId());
    }

    @Override public boolean handleMenuItem(int menuItemId, Object... item) {
        switch (menuItemId) {
            case android.R.id.home:
            case R.id.close:
                // fall through
            default:
                this.finish();
                return true;
        }
    }

    private static List<PreGraphTabs> preGraphTabsToShow() {
        if ( Brand.isSquash() ) {
            return new ArrayList<PreGraphTabs>(EnumSet.allOf(PreGraphTabs.class));
        } else if ( Brand.isBadminton() ) {
            return new ArrayList<PreGraphTabs>(EnumSet.of(PreGraphTabs.MatchHistoryViewTab));
        } else if ( Brand.isTabletennis() ) {
            return new ArrayList<PreGraphTabs>(EnumSet.of(PreGraphTabs.MatchHistoryViewTab));
        } else if ( Brand.isRacketlon() ) {
            return new ArrayList<PreGraphTabs>(EnumSet.of(PreGraphTabs.MatchHistoryViewTab));
        } else {
            return new ArrayList<PreGraphTabs>(EnumSet.noneOf(PreGraphTabs.class));
        }
    }

    public class TabsAdapter extends FragmentPagerAdapter
    {
        TabsAdapter(FragmentManager fm) {
            super(fm);
        }
        /** Invoked only once, even after screen rotation */
        @Override public Fragment getItem(int index) {
            DetailFragment fragment = new DetailFragment();
            fragment.iTab = index;
            return fragment;
        }

        @Override public int getCount() {
            int iCount = matchModel.getNrOfFinishedGames() + ListUtil.size(preGraphTabsToShow());
            if ( matchModel.gameHasStarted() && (matchModel.isPossibleGameVictory() == false)) {
                // add a tab for a game in progress
                iCount++;
            }
/*
            List<List<ScoreLine>> setScoreHistory = matchModel.getGameScoreHistory();
            if ( ListUtil.isEmpty(ListUtil.getLast(setScoreHistory)) ) {
                ListUtil.removeLast(setScoreHistory);
            }
            int iCount = preGraphTabsToShow().size() + ListUtil.size(setScoreHistory);
*/
            return iCount;
        }

        /** This method is also called when user switches tabs */
        @Override public CharSequence getPageTitle(int position) {
            PreGraphTabs pgTab = getPreGraphTab(position);
            if ( PreGraphTabs.MatchHistoryViewTab.equals(pgTab) ) {
                return getString(R.string.score_sheet);
            } else if ( PreGraphTabs.CallHistoryViewTab.equals(pgTab) ) {
                return getString(R.string.Conduct) + "/" + getString(R.string.Appeal) + " " + getString(R.string.overview);
            } else if ( PreGraphTabs.StatisticsViewTab.equals(pgTab) ) {
                return getString(R.string.statistics);
            } else {
                int iGame1B = position + 1 - preGraphTabsToShow().size();
                if ( Brand.isRacketlon() ) {
                    return getString(R.string.graph_x, matchModel.getSportForGame(iGame1B));
                } else {
                    return getString(R.string.graph_game_x, iGame1B);
                }
            }
        }
    }

    private static PreGraphTabs getPreGraphTab(int position) {
        PreGraphTabs pgTab = null;
        List<PreGraphTabs> tabs = preGraphTabsToShow();
        if ( (position >= 0) && ( position < ListUtil.size(tabs) ) ) {
            pgTab = tabs.get(position);
        }
        return pgTab;
    }

    public static class DetailFragment extends Fragment
    {
        // must have empty constructor (and if inner class also the class needs to be static) (noticable when screen rotates)
        public DetailFragment() {
            super();
        }

        private static final String TAB = "Tab";

        private int iTab = -1;
/*
        public DetailFragment(int i) {
            super();
            this.iTab = i;
        }
*/

        @Override public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putInt(TAB, iTab);
        }

        /**
         * Invoked
         * - on create
         * - when user moves tab more than 'one swipe away'
         * - orientation changes
         * */
        @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            if ( iTab == -1 ) {
                if (savedInstanceState != null) {
                    iTab = savedInstanceState.getInt(TAB);
                }
            }
            PreGraphTabs pgTab = getPreGraphTab(iTab);

            if ( PreGraphTabs.MatchHistoryViewTab.equals(pgTab) ) {
                return matchHistoryView;
            } else if ( PreGraphTabs.CallHistoryViewTab.equals(pgTab) ) {
                return matchCallsView;
            } else if ( PreGraphTabs.StatisticsViewTab.equals(pgTab) ) {
                return matchStatisticsView;
            } else {
                Context context = matchHistoryView.getContext();

                int iShowGame = iTab + 1 - preGraphTabsToShow().size(); // 1-based
                boolean bIsLastGame = iShowGame == matchModel.getGameNrInProgress();
                LinearLayout ll = new LinearLayout(context);
                ll.setPadding(10, 10, 10, 10);
              //boolean bShowGraphDuringTimer = PreferenceValues.showGraphDuringTimer(context, false);
                if ( matchModel.getName(Player.A).equals("Iddo T") && bIsLastGame && ViewUtil.isLandscapeOrientation(context) ) {
                    EndOfGameView endOfGameView = new EndOfGameView(context, null /*iBoard*/, matchModel);
                    endOfGameView.show(ll);
                } else {
                    GameGraphView gameGraphView = new GameGraphView(context);
                    gameGraphView.showGame(matchModel, iShowGame);

                    ll.addView(gameGraphView);
                }

                return ll;
            }
        }
    }

    private enum PreGraphTabs {
        StatisticsViewTab,
        CallHistoryViewTab,
        MatchHistoryViewTab,
    }

    private class MHTimerView implements TimerView {
        @Override public void setTitle(String s) {
            //MatchHistory.this.setTitle(s);
        }

        @Override public void setTime(String s) {
            CharSequence sTitle = getTitleWithoutOldTime();
            MatchHistory.this.setTitle(sTitle + " | " + s);
        }

        @Override public void setTime(int iStartedCountDownAtSecs, int iSecsLeft, int iReminderAtSecs) { }
        @Override public void setWarnMessage(String s) { }
        @Override public void setPausedMessage(String s) { }
        @Override public void cancel() {
            MatchHistory.this.handleMenuItem(R.id.close);
        }
        @Override public void timeIsUp() {
            CharSequence sTitle = getTitleWithoutOldTime();
            MatchHistory.this.setTitle(sTitle + " | " + getString(R.string.oa_time)); // TODO: is no longer visible after a swipe
        }
        @Override public void show() { }
        @Override public boolean isShowing() { return false; }

        private CharSequence getTitleWithoutOldTime() {
            CharSequence sTitle = MatchHistory.this.getTitle();
            if ( sTitle != null ) {
                sTitle = sTitle.toString().replaceFirst("\\s*\\|\\s*\\d+:\\d+$", ""); // strip of previous time
            }
            return sTitle;
        }
    }
}
