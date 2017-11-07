package com.doubleyellow.scoreboard.archive;

import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
import com.doubleyellow.util.SimpleELAdapter;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Activity to present previous/archived/recent matches
 */
public class ArchiveTabbed extends XActivity implements NfcAdapter.CreateNdefMessageCallback, MenuHandler {

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

/*
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
    /** for now this is only triggered for returning from 'select feed from feed' activity */
/*
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ( data != null ) {
            handleMenuItem(R.id.refresh);
        }
    }
*/

    public boolean handleMenuItem(int menuItemId, Object... ctx) {
        switch (menuItemId) {
            case R.id.filter:
            case R.id.refresh:
                if ( getFragment(SelectTab.Previous) instanceof ExpandableMatchSelector ) {
                    ExpandableMatchSelector matchSelector = (ExpandableMatchSelector) getFragment(SelectTab.Previous);
                    listAdapter = matchSelector.getListAdapter(null);
                    if ( menuItemId == R.id.filter ) {
                        matchSelector.initFiltering(true);
                    }
                    if ( menuItemId == R.id.refresh ) {
                        if ( listAdapter != null ) {
                            listAdapter.load(false);
                        }
                    }
                }
                if ( getFragment(SelectTab.PreviousMultiSelect) instanceof RecentMatchesMultiSelect ) {
                    if ( menuItemId == R.id.refresh ) {
                        RecentMatchesMultiSelect matchSelector = (RecentMatchesMultiSelect) getFragment(SelectTab.PreviousMultiSelect);
                        matchSelector.initMatches();
                    }
                }
                return true;
            case R.id.cmd_export:
                PreviousMatchSelector.selectFilenameForExport(this);
                return true;
            case R.id.cmd_import:
                PreviousMatchSelector.selectFilenameForImport(this);
                return true;
            case R.id.cmd_delete_all:
                PreviousMatchSelector.confirmDeleteAllPrevious(this);
                return true;
            case R.id.cmd_share_selected:
                Fragment fragment = getFragment(defaultTab);
                if ( fragment instanceof RecentMatchesMultiSelect ) {
                    RecentMatchesMultiSelect recentMatchesMultiSelect = (RecentMatchesMultiSelect) fragment;
                    return recentMatchesMultiSelect.shareSelected(this);
                }
                return false;
            case R.id.sb_sort_by:
                PreviousMatchSelector.selectSortingOptions(this);
                return true;
            case R.id.sb_overflow_submenu:
                return false;
            case android.R.id.home:
            case R.id.close:
                // fall through
                this.finish();
                return true;
            default:
                return false;
        }
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
        DialogManager.getInstance().restoreInstanceState(savedInstanceState, this, ScoreBoard.matchModel, null);
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        //Log.w(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        ScoreBoard.initAllowedOrientation(this);

        actualTabsToShow = new ArrayList<SelectTab>(Arrays.asList(SelectTab.values()));
        if ( PreferenceValues.saveMatchesForLaterUsage(this) == false ) {
            actualTabsToShow.remove(SelectTab.Previous);
            actualTabsToShow.remove(SelectTab.PreviousMultiSelect);
            // TODO: show some message or prevent this activity from being able to be called
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

        mAdapter = new MatchTabsAdapter(getFragmentManager());

        viewPager.setAdapter(mAdapter);

        ActionBar actionBar = getActionBar();
        if ( actionBar != null ) {
            actionBar.setHomeButtonEnabled(true);
        }

        titleStrip.setTextSize(TypedValue.COMPLEX_UNIT_DIP, getResources().getInteger(R.integer.TextSizeTabStrip) );
        ColorPrefs.setColor(titleStrip);

        viewPager.setOnPageChangeListener(new OnPageChangeListener());
        viewPager.setCurrentItem(actualTabsToShow.indexOf(defaultTab));

        registerNfc();

        // this is only because in the fragment activity... after rotate listeners are no longer working as expected... so do not allow orientation change
        //initAllowedOrientation();
        //ViewUtil.fixCurrentOrientationAndRotation(this);

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

/*
    private boolean initAllowedOrientation() {
        int orientation = this.getResources().getConfiguration().orientation;
        try {
            setRequestedOrientation(orientation);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            boolean bIsPortrait = isLandscape(this);
            if (bIsPortrait) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE); // 0
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); // 1
                //USER = 2
            }
        }
        return true;
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
