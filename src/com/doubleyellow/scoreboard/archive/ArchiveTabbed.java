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

package com.doubleyellow.scoreboard.archive;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import android.app.ActionBar;
import android.content.Context;
/*
import android.content.pm.PackageManager;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
*/
import android.os.Bundle;
import androidx.viewpager.widget.PagerTabStrip;
import androidx.viewpager.widget.ViewPager;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.doubleyellow.scoreboard.view.ExpandableListUtil;
import com.doubleyellow.util.ListUtil;
import com.doubleyellow.android.view.ViewUtil;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.activity.XActivity;
import com.doubleyellow.scoreboard.main.DialogManager;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.match.ExpandableMatchSelector;
import com.doubleyellow.scoreboard.prefs.ColorPrefs;
import com.doubleyellow.scoreboard.prefs.PreferenceKeys;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.util.MenuHandler;
import com.doubleyellow.android.util.SimpleELAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Activity to present previous/archived/recent matches
 */
public class ArchiveTabbed extends XActivity implements /*NfcAdapter.CreateNdefMessageCallback,*/ MenuHandler {

    private static final String TAG = "SB." + ArchiveTabbed.class.getSimpleName();

    private ViewPager        viewPager;
    public  MatchTabsAdapter mAdapter;

    public enum SelectTab {
        Previous (PreviousMatchSelector.class, R.string.sb_stored_matches),
        PreviousMultiSelect (RecentMatchesMultiSelect.class, R.string.sb_recent_matches),
        ;
        private Class clazz;
        private int iName;

        SelectTab(Class clazz, int iName) {
            this.clazz = clazz;
            this.iName = iName;
        }
    }

    private Fragment getFragment(SelectTab tab) {
        Fragment fragment = (Fragment) mAdapter.instantiateItem(viewPager, tab.ordinal());
        return fragment;
    }

    // ----------------------------------------------------
    // --------------------- NDEF/NFC/AndroidBeam ---------
    // ----------------------------------------------------

/*
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
        NdefRecord  mimeRecord        = ScoreBoard.createMimeRecord("application/" + "json", text.getBytes());
        NdefRecord  applicationRecord = NdefRecord.createApplicationRecord(packageName);
        NdefMessage msg               = new NdefMessage(new NdefRecord[]{mimeRecord, applicationRecord});
        return msg;
    }

    private String getNdefJsonString(Context context) {
        JSONObject joSetting = new JSONObject();
        try {
            switch (defaultTab) {
                case Previous:
                    break;
                case PreviousMultiSelect:
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return joSetting.toString();
    }
*/

    /** Populates the options menu. */
    private Menu menu = null;
    @Override public boolean onCreateOptionsMenu(Menu menu)
    {
        //Log.w(TAG, "onCreateOptionsMenu");

        MenuInflater inflater = this.getMenuInflater();
        inflater.inflate(R.menu.archivetabbedmenu, menu);

        toggleMenuItems(menu, defaultTab);

        this.menu = menu;
        ScoreBoard.updateDemoThread(menu);

        return true;
    }

    /** Toggle visibility of certain menu items based on the active tab */
    private void toggleMenuItems(Menu menu, SelectTab iDefaultTab) {
        MenuItem miRefresh = menu.findItem(R.id.refresh);
        miRefresh.setVisible(SelectTab.Previous.equals(iDefaultTab));
        MenuItem miFilter = menu.findItem(R.id.filter);
        miFilter.setVisible(SelectTab.Previous.equals(iDefaultTab));
        MenuItem miSortBy = menu.findItem(R.id.sb_sort_by);
        miSortBy.setVisible(SelectTab.Previous.equals(iDefaultTab));

        MenuItem miDeleteAll = menu.findItem(R.id.cmd_delete_all);
        miDeleteAll.setVisible(SelectTab.Previous.equals(iDefaultTab));
        MenuItem miImport = menu.findItem(R.id.cmd_import);
        miImport.setVisible(SelectTab.Previous.equals(iDefaultTab));
        MenuItem miExport = menu.findItem(R.id.cmd_export);
        miExport.setVisible(SelectTab.Previous.equals(iDefaultTab));

        MenuItem miExpandAll = menu.findItem(R.id.expand_all);
        miExpandAll.setVisible(SelectTab.Previous.equals(iDefaultTab));
        MenuItem miCollapseAll = menu.findItem(R.id.expand_all);
        miCollapseAll.setVisible(SelectTab.Previous.equals(iDefaultTab));

        MenuItem miShareSelected = menu.findItem(R.id.cmd_share_selected);
        miShareSelected.setVisible(SelectTab.PreviousMultiSelect.equals(iDefaultTab));

        if ( SelectTab.Previous.equals(iDefaultTab) ) {
            ViewUtil.hideKeyboardIfVisible(this);
        }
    }

    /** Handles the user's menu selection. */
    @Override public boolean onOptionsItemSelected(MenuItem item) {
        int menuItemId = item.getItemId();
        return handleMenuItem(menuItemId, item);
    }

    public boolean handleMenuItem(int menuItemId, Object... ctx) {
        if ( (menuItemId != R.id.close) && (menuItemId != R.id.sb_overflow_submenu) && ListUtil.isEmpty(actualTabsToShow) ) {
            showDisabledInSettingsMessage();
            return false;
        }
        if (menuItemId == R.id.filter || menuItemId == R.id.mt_filter || menuItemId == R.id.refresh) {
            if (getFragment(SelectTab.Previous) instanceof ExpandableMatchSelector) {
                ExpandableMatchSelector matchSelector = (ExpandableMatchSelector) getFragment(SelectTab.Previous);
                listAdapter = matchSelector.getListAdapter(null);
                if (menuItemId == R.id.filter) {
                    matchSelector.initFiltering(true);
                }
                if (menuItemId == R.id.refresh) {
                    if (listAdapter != null) {
                        listAdapter.load(false);
                    }
                }
            }
            if (getFragment(SelectTab.PreviousMultiSelect) instanceof RecentMatchesMultiSelect) {
                if (menuItemId == R.id.refresh) {
                    RecentMatchesMultiSelect matchSelector = (RecentMatchesMultiSelect) getFragment(SelectTab.PreviousMultiSelect);
                    matchSelector.initMatches();
                }
            }
            return true;
        } else if (menuItemId == R.id.cmd_export) {
            PreviousMatchSelector.selectFilenameForExport(this);
            return true;
        } else if (menuItemId == R.id.cmd_import) {
            PreviousMatchSelector.selectFilenameForImport(this);
            return true;
        } else if (menuItemId == R.id.expand_all || menuItemId == R.id.collapse_all) {
            if ((getFragment(defaultTab) != null) && (getFragment(defaultTab) instanceof ExpandableMatchSelector)) {
                ExpandableMatchSelector matchSelector = (ExpandableMatchSelector) getFragment(defaultTab);
                if (menuItemId == R.id.expand_all) {
                    ExpandableListUtil.expandAll(matchSelector.expandableListView);
                } else if (menuItemId == R.id.collapse_all) {
                    ExpandableListUtil.collapseAll(matchSelector.expandableListView);
                }
            }
            return true;
        } else if (menuItemId == R.id.cmd_delete_all) {
            PreviousMatchSelector.confirmDeleteAllPrevious(this);
            return true;
        } else if (menuItemId == R.id.cmd_share_selected) {
            Fragment fragment = getFragment(defaultTab);
            if (fragment instanceof RecentMatchesMultiSelect) {
                RecentMatchesMultiSelect recentMatchesMultiSelect = (RecentMatchesMultiSelect) fragment;
                return recentMatchesMultiSelect.shareSelected(this);
            }
            return false;
        } else if (menuItemId == R.id.sb_sort_by) {
            PreviousMatchSelector.selectSortingOptions(this);
            return true;
        } else if (menuItemId == R.id.sb_overflow_submenu) {
            return false;
        } else if (menuItemId == android.R.id.home || menuItemId == R.id.close) {// fall through
            this.finish();
            return true;
        }
        return false;
    }

    private PagerTabStrip titleStrip = null;
    private List<SelectTab> actualTabsToShow;

    /** invoked e.g. when orientation switches and when child activity is launched */
    @Override protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        DialogManager.getInstance().saveInstanceState(outState);
    }
    @Override protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState); // on rotate: handleRelaunchActivity
        DialogManager.getInstance().restoreInstanceState(savedInstanceState, this, ScoreBoard.getMatchModel(), null);
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        //Log.w(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        ScoreBoard.initAllowedOrientation(this);

        actualTabsToShow = new ArrayList<SelectTab>(Arrays.asList(SelectTab.values()));
        if ( PreferenceValues.saveMatchesForLaterUsage(this) == false ) {
            actualTabsToShow.remove(SelectTab.Previous);
            actualTabsToShow.remove(SelectTab.PreviousMultiSelect);
            // TODO: prevent this activity from being able to be called
            showDisabledInSettingsMessage();
        }

        if ( defaultTab == null ) {
            defaultTab = PreferenceValues.getEnum(PreferenceKeys.ArchiveTabbed_defaultTab, this, SelectTab.class, SelectTab.Previous);
        }
        if ( (actualTabsToShow.contains(defaultTab) == false) && (actualTabsToShow.size() > 0)) {
            defaultTab = actualTabsToShow.get(0);
        }

        setContentView(R.layout.match_tabbed);

        ViewUtil.setFullScreen(getWindow(), PreferenceValues.showFullScreen(this));
        ColorPrefs.setColors(this, null);

        // Initialization
        viewPager = (ViewPager) findViewById(R.id.pager);
        titleStrip = ViewUtil.getFirstView(viewPager, PagerTabStrip.class);

        mAdapter = new MatchTabsAdapter(getSupportFragmentManager());

        viewPager.setAdapter(mAdapter);

        ActionBar actionBar = getActionBar();
        if ( actionBar != null ) {
            actionBar.setHomeButtonEnabled(true);
        }

        titleStrip.setTextSize(TypedValue.COMPLEX_UNIT_DIP, getResources().getInteger(R.integer.TextSizeTabStrip) );
        ColorPrefs.setColor(titleStrip);

        viewPager.setOnPageChangeListener(new OnPageChangeListener());
        viewPager.setCurrentItem(actualTabsToShow.indexOf(defaultTab));

        //registerNfc();

        // this is only because in the fragment activity... after rotate listeners are no longer working as expected... so do not allow orientation change
        //initAllowedOrientation();
        //ViewUtil.fixCurrentOrientationAndRotation(this);

        ScoreBoard.updateDemoThread(this);
    }

    private void showDisabledInSettingsMessage() {
        Toast.makeText(this, "In your preferences/settings you have selected NOT to store/archive finished matches", Toast.LENGTH_LONG).show();
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
        super.onBackPressed();
    }

    /** Switches each time the user switches Tab. So it is both the Current tab and Default tab for when user re-enters this activity */
    private static SelectTab defaultTab = null;
    public static void persist(Context context) {
        PreferenceValues.setEnum(PreferenceKeys.ArchiveTabbed_defaultTab, context, defaultTab);
    }
    public static void setDefaultTab(SelectTab t) {
        defaultTab = t;
    }

    /** Switches each time the user switches Tab to represent the adapter of the active tab */
    private SimpleELAdapter listAdapter = null;

    private class OnPageChangeListener extends ViewPager.SimpleOnPageChangeListener {
        @Override public void onPageSelected(int position) {
            // set the static value so that on next invocation user is direct to last tab he used
            defaultTab = actualTabsToShow.get(position);

            if ( menu != null ) {
                toggleMenuItems(menu, defaultTab);
            }

            MatchTabsAdapter adapter = ArchiveTabbed.this.mAdapter;
            Fragment fragment = getFragment(defaultTab);
            if ( fragment instanceof ExpandableMatchSelector ) {
                ExpandableMatchSelector eml = (ExpandableMatchSelector) fragment;
                if ( eml.getActivity() != null ) {
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
            if ( index >= ListUtil.size(actualTabsToShow) ) {
                // prevent NPE
                return null;
            }
            SelectTab selectTab = actualTabsToShow.get(index);
            Fragment fragment = Fragment.instantiate(ArchiveTabbed.this, selectTab.clazz.getName());
            if ( fragment instanceof ExpandableMatchSelector) {
                ExpandableMatchSelector matchSelector = (ExpandableMatchSelector) fragment;
                if ((defaultTab != null) && (actualTabsToShow.indexOf(defaultTab) == index)) {
                    matchSelector.setAutoLoad(true);
                    loaded[index] = true;
                }
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
            return sTitle;
        }
    }
}
