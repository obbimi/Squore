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

package com.doubleyellow.scoreboard.match;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.util.TypedValue;
import android.view.*;
import com.doubleyellow.android.view.ViewUtil;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.activity.XActivity;
import com.doubleyellow.scoreboard.archive.PreviousMatchSelector;
import com.doubleyellow.scoreboard.feed.FeedFeedSelector;
import com.doubleyellow.scoreboard.feed.FeedMatchSelector;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.prefs.ColorPrefs;
import com.doubleyellow.scoreboard.prefs.NewMatchLayout;
import com.doubleyellow.scoreboard.prefs.PreferenceKeys;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.scoreboard.prefs.URLsKeys;
import com.doubleyellow.scoreboard.view.ExpandableListUtil;
import com.doubleyellow.util.MapUtil;
import com.doubleyellow.util.MenuHandler;
import com.doubleyellow.android.util.SimpleELAdapter;
import com.doubleyellow.util.StringUtil;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

/**
 * Activity to present several ways of starting a new match
 * - select one from an internet feed
 * - select one from a locally stored list
 * - enter a singles match manually
 * - enter a doubles match manually
 */
public class MatchTabbed extends XActivity implements NfcAdapter.CreateNdefMessageCallback, MenuHandler, FeedMatchSelector.FeedStatusChangedListerer {

    private static final String TAG = "SB." + MatchTabbed.class.getSimpleName();

    public  MatchTabsAdapter mAdapter;
    private ViewPager        viewPager;

    public enum SelectTab {
        //Previous (PreviousMatchSelector.class, R.string.sb_stored_matches     , android.R.drawable.ic_menu_save),
        Feed     (FeedMatchSelector    .class, R.string.sb_feed               ,         R.drawable.ic_action_web_site),
        Mine     (StaticMatchSelector  .class, R.string.sb_static_match       ,         R.drawable.ic_action_view_as_list),
        Manual   (MatchFragment        .class, R.string.sb_new_singles_match  , R.drawable.circled_plus),
        ManualDbl(MatchFragmentDoubles .class, R.string.sb_new_doubles_match  , R.drawable.circled_plus),
        ;
        private Class clazz;
        private int iIcon;
        private int iName;

        SelectTab(Class clazz, int iName, int iIcon) {
            this.clazz = clazz;
            this.iName = iName;
            this.iIcon = iIcon;
        }

/* Is this causing leaks and thus non responsive menu's ?
        private Fragment matchSelector = null;
        private Fragment createFragment() {
            try {
                this.matchSelector = (Fragment) clazz.newInstance();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return matchSelector;
        }
*/
    }

    private Fragment getFragment(SelectTab tab) {
        Fragment fragment = (Fragment) mAdapter.instantiateItem(viewPager, tab.ordinal());
        return fragment;
    }
/*
    private Map<SelectTab, Fragment> mFragments = new HashMap<>();
    private Fragment getFragment(SelectTab tab) {
        if ( mFragments.containsKey(tab) == false ) {
            try {
                Fragment fragment = (Fragment) tab.clazz.newInstance();
                Fragment fragment2 = (Fragment) mAdapter.instantiateItem(viewPager, tab.ordinal());
                mFragments.put(tab, fragment);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return mFragments.get(tab);
    }
*/

    // ----------------------------------------------------
    // --------------------- NDEF/NFC/AndroidBeam ---------
    // ----------------------------------------------------

    private NfcAdapter mNfcAdapter;
    private void registerNfc() {
        if ( getPackageManager().hasSystemFeature(PackageManager.FEATURE_NFC) == false ) {
            return;
        }
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if ( mNfcAdapter == null ) {
            //Toast.makeText(this, "NFC is not available", Toast.LENGTH_LONG).show();
            return;
        }
        // Register callback
        mNfcAdapter.setNdefPushMessageCallback(this, this);
    }

    @Override public NdefMessage createNdefMessage(NfcEvent nfcEvent) {
        String      packageName       = this.getPackageName();
        String      text              = getNdefJsonString(this);
        NdefRecord  mimeRecord        = ScoreBoard.createMimeRecord("application/" + "json" /*+ packageName*/, text.getBytes());
        NdefRecord  applicationRecord = NdefRecord.createApplicationRecord(packageName);
        NdefMessage msg               = new NdefMessage(new NdefRecord[]{mimeRecord, applicationRecord});
        return msg;
    }

    private String getNdefJsonString(Context context) {
        JSONObject joSetting = new JSONObject();
        try {
            switch (defaultTab) {
                case Feed:
                    int iUrlIndex = PreferenceValues.getInteger(PreferenceKeys.feedPostUrl, context, 0);
                    String sUrls = PreferenceValues.getString(PreferenceKeys.feedPostUrls, "", context);
                    List<Map<URLsKeys, String>> urlsList = PreferenceValues.getUrlsList(sUrls, context);
                    if ( urlsList.size() > iUrlIndex ) {
                        Map<URLsKeys, String> mCommunicate = urlsList.get(iUrlIndex);
                        //JSONObject oFeed = new JSONObject(mCommunicate);
                        //joSetting.put(PreferenceKeys.feedPostUrls.toString(), oFeed);

                        String sTmp = URLsKeys.Name + "=" + mCommunicate.get(URLsKeys.Name)       + "\n"
                                    + URLsKeys.FeedMatches + "=" + mCommunicate.get(URLsKeys.FeedMatches) + "\n";
                        if ( mCommunicate.get(URLsKeys.FeedPlayers) != null ) {
                            sTmp    += URLsKeys.FeedPlayers + "=" + mCommunicate.get(URLsKeys.FeedPlayers) + "\n";
                        }
                        if ( mCommunicate.get(URLsKeys.PostResult) != null ) {
                            sTmp    += URLsKeys.PostResult + "=" + mCommunicate.get(URLsKeys.PostResult) + "\n";
                            if ( mCommunicate.get(URLsKeys.PostData) != null ) {
                                sTmp    += URLsKeys.PostData + "=" + mCommunicate.get(URLsKeys.PostData) + "\n";
                            }
                        }
                        joSetting.put(PreferenceKeys.feedPostUrls.toString(), sTmp);
                    }
                    break;
                case Mine:
                    String sMatches = PreferenceValues.getString(PreferenceKeys.matchList, "",context);
                    joSetting.put(PreferenceKeys.matchList.toString(), "|" + sMatches.replaceAll("\n", "|") + "|");
                    break;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return joSetting.toString();
    }

    private Menu menu = null;
    /** Populates the options menu. */
    @Override public boolean onCreateOptionsMenu(Menu menu)
    {
        //Log.w(TAG, "onCreateOptionsMenu");

        MenuInflater inflater = this.getMenuInflater();
        inflater.inflate(R.menu.matchtabbedmenu, menu);

        toggleMenuItems(menu, defaultTab);

        this.menu = menu;
        ScoreBoard.updateDemoThread(menu);

        return true;
    }

    @Override public void notify(FeedMatchSelector.FeedStatus fsOld, FeedMatchSelector.FeedStatus fsNew) {
        ViewUtil.setMenuItemsVisibility(menu, new int[]{R.id.show_matches_from_feed}, fsNew.equals(FeedMatchSelector.FeedStatus.showingPlayers));

        final boolean bShowingMatches = fsNew.equals(FeedMatchSelector.FeedStatus.showingMatches) || fsNew.equals(FeedMatchSelector.FeedStatus.showingMatchesUncompleted);
        ViewUtil.setMenuItemsVisibility(menu, new int[]{R.id.show_players_from_feed     }, bShowingMatches);
        ViewUtil.setMenuItemsVisibility(menu, new int[]{R.id.uc_hide_matches_with_result}, bShowingMatches);
    }

    /** Toggle visibility of certain menu items based on the active tab */
    private void toggleMenuItems(Menu menu, SelectTab forTab) {
        ViewUtil.setMenuItemsVisibility(menu, new int[] {R.id.new_group, R.id.sort_group_names}, SelectTab.Mine.equals(forTab));
/*
        MenuItem miNewGroup = menu.findItem(R.id.new_group);
        miNewGroup.setVisible(SelectTab.Mine.equals(forTab));

        MenuItem miSortGroup = menu.findItem(R.id.sort_group_names);
        miSortGroup.setVisible(SelectTab.Mine.equals(forTab));
*/

        ViewUtil.setMenuItemsVisibility(menu, new int[] {R.id.uc_switch_feed, R.id.uc_hide_matches_with_result, R.id.uc_add_new_feed}, SelectTab.Feed.equals(forTab));
        boolean bChecked = PreferenceValues.hideCompletedMatchesFromFeed(this);
        ViewUtil.checkMenuItem(menu, R.id.uc_hide_matches_with_result, bChecked);

/*
        MenuItem miSwitchFeed = menu.findItem(R.id.uc_switch_feed);
        miSwitchFeed.setVisible(SelectTab.Feed.equals(forTab));

        MenuItem miHideShowFinished = menu.findItem(R.id.uc_hide_matches_with_result);
        miHideShowFinished.setVisible(SelectTab.Feed.equals(forTab));

        MenuItem miAddNewFeed = menu.findItem(R.id.uc_add_new_feed);
        miAddNewFeed.setVisible(SelectTab.Feed.equals(forTab));

        ViewUtil.checkMenuItem(miHideShowFinished, bChecked);
*/
        //int iNewResId = bChecked?R.string.pref_showCompletedMatchesFromFeed:R.string.pref_hideCompletedMatchesFromFeed;
        //miHideShowFinished.setTitle(iNewResId); // we do this because checking/unchecking is not visible?!

        ViewUtil.setMenuItemsVisibility(menu, new int[]{ R.id.mt_cmd_ok
                                                       , R.id.mt_clear_event_fields
                                                       , R.id.mt_clear_player_fields
                                                       , R.id.mt_clear_referee_fields
                                                       , R.id.mt_clear_club_fields
                                                       , R.id.mt_clear_country_fields
                                                       , R.id.mt_clear_all_fields
                                                       , R.id.mt_matchlayout_all
                                                       , R.id.mt_matchlayout_simple
                                                       }, manualTabs.contains(forTab));
        ViewUtil.setMenuItemsVisibility(menu, new int[]{ R.id.mt_refresh
                                                       , R.id.expand_all
                                                       , R.id.collapse_all
                                                       }, manualTabs.contains(forTab) == false);
        ViewUtil.setMenuItemsVisibility(menu, new int[]{ R.id.show_players_from_feed }, SelectTab.Feed.equals(forTab) && StringUtil.isNotEmpty(PreferenceValues.getPlayersFeedURL(this)));
        ViewUtil.setMenuItemsVisibility(menu, new int[]{ R.id.show_matches_from_feed }, SelectTab.Feed.equals(forTab) && StringUtil.isNotEmpty(PreferenceValues.getMatchesFeedURL(this)));
        ViewUtil.setMenuItemsVisibility(menu, new int[]{ R.id.mt_filter              }, SelectTab.Feed.equals(forTab));

        if ( manualTabs.contains(forTab) ) {
            NewMatchLayout newMatchLayout = PreferenceValues.getNewMatchLayout(this);
            ViewUtil.setMenuItemVisibility(menu, R.id.mt_matchlayout_simple, NewMatchLayout.AllFields.equals(newMatchLayout));
            ViewUtil.setMenuItemVisibility(menu, R.id.mt_matchlayout_all, NewMatchLayout.Simple.equals(newMatchLayout));
        }
/*
        MenuItem miOK = menu.findItem(R.id.mt_cmd_ok);
        miOK.setVisible(manualTabs.contains(forTab));
*/

        //MenuItem miRefresh = menu.findItem(R.id.mt_refresh);
        //miRefresh.setVisible(manualTabs.contains(forTab) == false);

        //ViewUtil.setMenuItemsVisibility(menu, new int[]{R.id.cmd_delete_all, R.id.mt_cmd_import, R.id.mt_cmd_export}, SelectTab.Previous.equals(forTab));
/*
        MenuItem miDeleteAll = menu.findItem(R.id.cmd_delete_all);
        miDeleteAll.setVisible(SelectTab.Previous.equals(forTab));
        MenuItem miImport = menu.findItem(R.id.mt_cmd_import);
        miImport.setVisible(SelectTab.Previous.equals(forTab));
        MenuItem miExport = menu.findItem(R.id.mt_cmd_export);
        miExport.setVisible(SelectTab.Previous.equals(forTab));
*/
        if ( manualTabs.contains(forTab) == false ) {
            ViewUtil.hideKeyboardIfVisible(this);
        }
    }
    private final EnumSet<SelectTab> manualTabs = EnumSet.of(SelectTab.Manual, SelectTab.ManualDbl);

    /** Handles the user's menu selection. */
    @Override public boolean onOptionsItemSelected(MenuItem item) {
        int menuItemId = item.getItemId();
        return handleMenuItem(menuItemId, item);
    }
    /** for now this is only triggered for returning from 'select feed from feed' activity */
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ( data != null ) {
            if ( ( getFragment(defaultTab) instanceof ExpandableMatchSelector)) {
                ExpandableMatchSelector matchSelector = (ExpandableMatchSelector) getFragment(defaultTab);
                if ( matchSelector instanceof FeedMatchSelector ) {
                    FeedMatchSelector fms = (FeedMatchSelector) matchSelector;
                    fms.resetFeedStatus();
                }
            }

            handleMenuItem(R.id.mt_refresh, Boolean.TRUE);
        }
    }

    @Override public boolean handleMenuItem(int menuItemId, Object... item) {
        MatchView matchView = null;
        if ( getFragment(defaultTab) != null ) {
            View view = getFragment(defaultTab).getView();
            matchView = (MatchView) ((view instanceof MatchView) ? view : null);
        }
        switch (menuItemId) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.cmd_ok: case R.id.mt_cmd_ok:
                return _startMatch();
            case R.id.mt_matchlayout_all: {
                PreferenceValues.setEnum(PreferenceKeys.newMatchLayout, this, NewMatchLayout.AllFields);
                return _restart(R.id.dyn_new_match);
            }
            case R.id.mt_matchlayout_simple: {
                PreferenceValues.setEnum(PreferenceKeys.newMatchLayout, this, NewMatchLayout.Simple);
                return _restart(R.id.dyn_new_match);
            }
            case R.id.uc_clear_all_fields: case R.id.mt_clear_all_fields: {
                if (matchView == null) return false;
                return matchView.clearAllFields();
            }
            case R.id.uc_clear_player_fields: case R.id.mt_clear_player_fields: {
                if (matchView == null) return false;
                return matchView.clearPlayerFields();
            }
            case R.id.uc_clear_event_fields: case R.id.mt_clear_event_fields: {
                if (matchView == null) return false;
                return matchView.clearEventFields();
            }
            case R.id.uc_clear_club_fields: case R.id.mt_clear_club_fields: {
                if (matchView == null) return false;
                return matchView.clearClubFields();
            }
            case R.id.uc_clear_country_fields: case R.id.mt_clear_country_fields: {
                if (matchView == null) return false;
                return matchView.clearCountryFields();
            }
            case R.id.uc_clear_referee_fields: case R.id.mt_clear_referee_fields: {
                if (matchView == null) return false;
                return matchView.clearRefereeFields();
            }
            case R.id.uc_add_new_feed: {
                Intent ff = new Intent(this, FeedFeedSelector.class);
                startActivityForResult(ff, 1); // see onActivityResult
                return true;
            }
            case R.id.uc_hide_matches_with_result: {
                if ( item == null || item.length==0 || (item[0] instanceof MenuItem)==false ) { return false; }
                final MenuItem menuItem = (MenuItem) item[0];
                boolean bNewChecked = (menuItem.isChecked() == false);
                //int iNewResId = bNewChecked?R.string.pref_showCompletedMatchesFromFeed:R.string.pref_hideCompletedMatchesFromFeed;
                int iNewIconId= bNewChecked?android.R.drawable.checkbox_on_background:android.R.drawable.checkbox_off_background;
                menuItem.setChecked(bNewChecked);
                //menuItem.setTitle(iNewResId); // we do this because checking/unchecking is not visible?!
                menuItem.setIcon(iNewIconId); // we do this because checking/unchecking is not visible?!
                PreferenceValues.setBoolean(PreferenceKeys.hideCompletedMatchesFromFeed, this, bNewChecked);

                // now refresh: try without reloading data from the URL
                if ( /*(listAdapter == null) &&*/ (getFragment(defaultTab) instanceof FeedMatchSelector)) {
                    FeedMatchSelector fms = (FeedMatchSelector) getFragment(defaultTab);
                    fms.resetFeedStatus();
                    SimpleELAdapter listAdapter = fms.getListAdapter(null);
                    listAdapter.load(true);
                }

                return true;
            }
            case R.id.uc_switch_feed: {
                FeedMatchSelector.switchFeed(this);
                return true;
            }
            case R.id.show_players_from_feed:
            case R.id.show_matches_from_feed: {
                if ( (getFragment(defaultTab) != null) && (getFragment(defaultTab) instanceof ExpandableMatchSelector) ) {
                    ExpandableMatchSelector matchSelector = (ExpandableMatchSelector) getFragment(defaultTab);
                    if ( matchSelector instanceof FeedMatchSelector ) {
                        FeedMatchSelector       fms           = (FeedMatchSelector) matchSelector;
                        SimpleELAdapter         listAdapter   = fms.getListAdapter(null);
                        if ( menuItemId == R.id.show_players_from_feed ) {
                            if ( fms.getFeedStatus().equals(FeedMatchSelector.FeedStatus.showingPlayers) == false ) {
                                listAdapter.load(true);
                            }
                        }
                        if ( menuItemId == R.id.show_matches_from_feed ) {
                            if ( fms.getFeedStatus().equals(FeedMatchSelector.FeedStatus.showingPlayers) ) {
                                fms.resetFeedStatus();
                                listAdapter.load(true);
                            }
                        }
                    }
                }
                return true;
            }
            case R.id.filter: case R.id.mt_filter:
            case R.id.expand_all:
            case R.id.collapse_all: {
                if ( (getFragment(defaultTab) != null) && (getFragment(defaultTab) instanceof ExpandableMatchSelector) ) {
                    ExpandableMatchSelector matchSelector = (ExpandableMatchSelector) getFragment(defaultTab);
                    if ( menuItemId == R.id.expand_all ) {
                        ExpandableListUtil.expandAll(matchSelector.expandableListView);
                    } else if ( menuItemId == R.id.collapse_all ) {
                        ExpandableListUtil.collapseAll(matchSelector.expandableListView);
                        // show totals per header
                        if ( matchSelector instanceof FeedMatchSelector) {
                            FeedMatchSelector fms = (FeedMatchSelector) matchSelector;
                            SimpleELAdapter listAdapter = fms.getListAdapter(null);
                            ScoreBoard.dialogWithOkOnly(this, MapUtil.toNiceString(listAdapter.getHeaderChildCounts()));
                        }
                    } else if ( (menuItemId == R.id.filter) || (menuItemId == R.id.mt_filter) ) {
                        matchSelector.initFiltering(true);
                    }
                }
                return true;
            }
            case R.id.sort_group_names: {
                StaticMatchSelector staticMatchSelector = (StaticMatchSelector) getFragment(SelectTab.Mine);
                staticMatchSelector.sortHeaders(this);
                return true;
            }
            case R.id.new_group: {
                StaticMatchSelector staticMatchSelector = (StaticMatchSelector) getFragment(SelectTab.Mine);
                staticMatchSelector.editHeader("");
                return true;
            }
            case R.id.refresh: case R.id.mt_refresh:
                if ( (listAdapter == null) && (getFragment(defaultTab) instanceof ExpandableMatchSelector)) {
                    ExpandableMatchSelector matchSelector = (ExpandableMatchSelector) getFragment(defaultTab);
                    if ( matchSelector != null ) {
                        listAdapter = matchSelector.getListAdapter(null);
                    }
                }
                if ( listAdapter != null ) {
                    boolean bUseCacheIfPresent = (item.length > 0 && (item[0] instanceof Boolean) && (Boolean) item[0]);
                    if ( item.length > 1 && ((Boolean) item[1]) && defaultTab.equals(SelectTab.Feed) ) {
                        FeedMatchSelector fms = (FeedMatchSelector) getFragment(defaultTab);
                        fms.resetFeedStatus();
                    }
                    listAdapter.load(bUseCacheIfPresent);
                }
                if ( defaultTab.equals(SelectTab.Feed) ) {
                    // to update the title
                    mAdapter.notifyDataSetChanged();
                }

                return true;
            case R.id.cmd_export: case R.id.mt_cmd_export:
                PreviousMatchSelector.selectFilenameForExport(this);
                return true;
            case R.id.cmd_import: case R.id.mt_cmd_import:
                PreviousMatchSelector.selectFilenameForImport(this);
                return true;
            case R.id.cmd_delete_all:
                PreviousMatchSelector.confirmDeleteAllPrevious(this);
                return true;
            case R.id.sb_overflow_submenu:
                return false;
            case R.id.close:
                this.finish();
                return true;
            default:
                tryShowToast("Sorry, don't know what to do for menu item " + menuItemId + "... Trying start match...");
                //_startMatch();
                return false;
        }
    }
    private void tryShowToast(String sMsg) {
        Log.w(TAG, sMsg);
        //Toast.makeText(this, sMsg, Toast.LENGTH_SHORT).show();
    }

    private boolean _startMatch() {
        Intent intent = _getSinglesOrDoublesIntent(false);
        if ( intent == null ) {
            tryShowToast("No intent from singles or doubles...");
            return false;
        }
        setResult(RESULT_OK, intent);

        finish();
        return true;
    }

    private boolean _restart(int iAction) {
        Intent intent = new Intent();
        intent.setAction(String.valueOf(iAction));
        setResult(RESULT_OK, intent);

        finish();
        return true;
    }

    private Intent _getSinglesOrDoublesIntent(boolean bBackPressed) {
/*
        if ( ScoreBoard.matchModel.getName(Player.A).equals("Throw E") ) {
            throw new NullPointerException("For player Throw E and " + ScoreBoard.matchModel.getName(Player.B));
        }
*/
        Intent intent = null;

        // check for unresponsive menu items for certain devices apk=123
        if ( defaultTab == null ) {
            tryShowToast("Default tab is null");
            return null;
        }

        switch (defaultTab) {
            case ManualDbl: // fall through
            case Manual: {
                MatchFragment matchFragment = (MatchFragment) getFragment(defaultTab);
                intent = matchFragment.getIntent(bBackPressed);
                Match.dontShow();
                break;
            }
            default: {
                tryShowToast("Sorry, can not start match for tab " + defaultTab);
            }
        }
        return intent;
    }

    private List<SelectTab> actualTabsToShow;

/*
    @Override protected void onDestroy() {
        persist(this); // no here: context no longer valid to store
    }
    @Override protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        persist(this);
    }
*/

    @Override protected void onStop() {
        super.onStop();
        try {
            persist(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        //Log.w(TAG, "onCreate");
        super.onCreate(savedInstanceState); // when rotating this already re-instantiates several fragments

        int runCount = PreferenceValues.getRunCount(this, MatchTabbed.class.getSimpleName());
        if ( runCount < 2 ) {
            PreferenceValues.addRecentFeedURLsForDemo(this);
        }

        ScoreBoard.initAllowedOrientation(this);

        actualTabsToShow = new ArrayList<SelectTab>(Arrays.asList(SelectTab.values()));
        //String sFeedMatchesUrl = PreferenceValues.getMatchesFeedURL(this);
        //String sFeedPlayersUrl = PreferenceValues.getPlayersFeedURL(this);
        if ( PreferenceValues.useFeedAndPostFunctionality(this) == false /*StringUtil.isEmpty(sFeedUrl)*/ ) {
            actualTabsToShow.remove(SelectTab.Feed);
        }
/*
        if ( PreferenceValues.saveMatchesForLaterUsage(this) == false ) {
            actualTabsToShow.remove(SelectTab.Previous);
        }
        if ( PreferenceValues.archivedMatchesInSeparateActivity(this) ) {
            actualTabsToShow.remove(SelectTab.Previous);
        }
*/

        if ( defaultTab == null ) {
            defaultTab = PreferenceValues.getEnum(PreferenceKeys.MatchTabbed_defaultTab, this, SelectTab.class, SelectTab.Manual);

            if ( defaultTab.equals(SelectTab.Feed) ) {
                boolean bFeedIsNoLongerValid = false; // TODO: if feed no longer valid... e.g. make
                if ( bFeedIsNoLongerValid ) {
                    defaultTab = SelectTab.Manual;
                }
            }
        }
        if ( actualTabsToShow.contains(defaultTab) == false ) {
            defaultTab = actualTabsToShow.get(0);
        }
/*
        if ( defaultTab == null ) {
            // don't let new users think they can only use it to select from feed
            defaultTab = SelectTab.Manual;
            defaultTab = SelectTab.Feed;
            if (PreferenceValues.getMatchesFeedURLUnchanged()) {
                defaultTab = SelectTab.Mine;
            }
        }
*/

        setContentView(R.layout.match_tabbed);

        ViewUtil.setFullScreen(getWindow(), PreferenceValues.showFullScreen(this));

        // Initialization
        viewPager = (ViewPager) findViewById(R.id.pager);
        PagerTabStrip titleStrip = ViewUtil.getFirstView(viewPager, PagerTabStrip.class);

        mAdapter = new MatchTabsAdapter(getFragmentManager());

        viewPager.setAdapter(mAdapter);

        Map<ColorPrefs.ColorTarget, Integer> mColors = ColorPrefs.getTarget2colorMapping(this);
        setActiorBarBGColor(mColors.get(ColorPrefs.ColorTarget.actionBarBackgroundColor));

        setHomeButtonEnabledOnActionBar();

        titleStrip.setTextSize(TypedValue.COMPLEX_UNIT_DIP, getResources().getInteger(R.integer.TextSizeTabStrip) );
        ColorPrefs.setColor(titleStrip);

        viewPager.addOnPageChangeListener(new OnPageChangeListener());
        viewPager.setCurrentItem(actualTabsToShow.indexOf(defaultTab));

        registerNfc();

        ScoreBoard.updateDemoThread(this);
    }

/*
    @Override protected void onResume() {
        super.onResume();
    }
*/

/* Usage of this method is discouraged
    @Override public void onConfigurationChanged(Configuration newConfig) {
        Log.w(TAG, "New config: " + newConfig);
        //super.onConfigurationChanged(newConfig);
    }
*/

    @Override public void onBackPressed() {
        if ( ScoreBoard.isInDemoMode() ) {
            ScoreBoard.demoThread.cancelDemoMessage();
            return;
        }
        Intent intent = _getSinglesOrDoublesIntent(true);
        if ( intent == null ) {
            // not enough data entered... simply leave the activity
            super.onBackPressed();
        } else {
            // user enter valid data to start match with
            AlertDialog.Builder ab = ScoreBoard.getAlertDialogBuilder(this);
            DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface dialog, int choice) {
                    switch (choice) {
                        case DialogInterface.BUTTON_POSITIVE:
                            //handleMenuItem(R.id.mt_cmd_ok);
                            _startMatch();
                            break;
                        case DialogInterface.BUTTON_NEUTRAL:
                            break;
                        case DialogInterface.BUTTON_NEGATIVE:
                            handleMenuItem(R.id.close);
                            break;
                    }

                }
            };
            ab.setMessage(R.string.q_use_entered_match_details)
                    .setPositiveButton(        R.string.cmd_yes, listener)
                    .setNegativeButton(        R.string.cmd_no , listener) // close the dialog and the activity
                    .setNeutralButton (android.R.string.cancel    , listener) // close the dialog only, stay in match activity
                    .show();
        }
    }

    /** Switches each time the user switches Tab. So it is both the Current tab and Default tab for when user re-enters this activity */
    private static SelectTab defaultTab = null;
    public static void persist(Context context) {
        if ( defaultTab != null ) {
            PreferenceValues.setEnum(PreferenceKeys.MatchTabbed_defaultTab, context, defaultTab);
        }
    }
    public static void setDefaultTab(SelectTab t) {
        defaultTab = t;
    }
    public static SelectTab getDefaultTab() {
        return defaultTab;
    }

    /** Switches each time the user switches Tab to represent the adapter of the active tab */
    private SimpleELAdapter listAdapter = null;

    private class OnPageChangeListener extends ViewPager.SimpleOnPageChangeListener {
        @Override public void onPageSelected(int position) {
            // set the static value so that on next invocation user is directed to last tab he used
            defaultTab = actualTabsToShow.get(position);

            if ( menu != null ) {
                toggleMenuItems(menu, defaultTab);
            }

            MatchTabsAdapter adapter = MatchTabbed.this.mAdapter;
            Fragment fragment = getFragment(defaultTab);
            if ( fragment instanceof ExpandableMatchSelector ) {
                ExpandableMatchSelector eml = (ExpandableMatchSelector) fragment;
                if ( eml.activity != null ) {
                    listAdapter = eml.getListAdapter(null);

                    if (adapter.loaded[position] == false) {
                        listAdapter.load(true);
                        adapter.loaded[position] = true;
                    }
                }
            }
        }
    }

    public class MatchTabsAdapter extends FragmentPagerAdapter
    {
        private boolean[] loaded = null;

        public MatchTabsAdapter(FragmentManager fm) {
            super(fm);
            loaded = new boolean[actualTabsToShow.size()];
        }

        /** This all is only invoked once, even when screen rotates */
        @Override public Fragment getItem(int index) {
            //Log.w(TAG, "getItem(" + index + ")");
            SelectTab selectTab = actualTabsToShow.get(index);
            Fragment fragment = Fragment.instantiate(MatchTabbed.this, selectTab.clazz.getName());
            if ( fragment instanceof ExpandableMatchSelector ) {
                ExpandableMatchSelector matchSelector = (ExpandableMatchSelector) fragment;
                if ((defaultTab != null) && (actualTabsToShow.indexOf(defaultTab) == index)) {
                    matchSelector.setAutoLoad(true);
                    loaded[index] = true;
                }
            }
            if ( fragment instanceof FeedMatchSelector ) {
                FeedMatchSelector fms = (FeedMatchSelector) fragment;
                fms.registerFeedChangeListener(MatchTabbed.this);
            }
            return fragment;
        }

        @Override public int getCount() {
            // get item count - equal to number of tabs
            //Log.w(TAG, "getCount");
            return actualTabsToShow.size();
        }

        /** This method is also called when user switches tabs */
        @Override public CharSequence getPageTitle(int position) {
            SelectTab selectTab = actualTabsToShow.get(position);
            String sTitle = getString(selectTab.iName);
            if ( selectTab.equals(SelectTab.Feed) ) {
                String feedPostName = PreferenceValues.getFeedPostName(MatchTabbed.this);
                if ( StringUtil.isEmpty(feedPostName) ) {
                    sTitle += " " + getString(R.string.lbl_none);
                } else {
                    sTitle += " " + feedPostName;
                }
            }
            return sTitle;
        }
    }

}
