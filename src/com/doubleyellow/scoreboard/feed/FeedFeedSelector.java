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

package com.doubleyellow.scoreboard.feed;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.*;
import android.widget.*;

import com.doubleyellow.android.util.ContentReceiver;
import com.doubleyellow.android.util.SearchEL;
import com.doubleyellow.android.view.ViewUtil;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.URLFeedTask;
import com.doubleyellow.scoreboard.activity.XActivity;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.prefs.ColorPrefs;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.scoreboard.prefs.URLsKeys;
import com.doubleyellow.scoreboard.view.ExpandableListUtil;
import com.doubleyellow.util.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

/**
 * Activity that allows the user to select a item (in this case a feed) from an internet feed
 * and add this feed the the list of feeds this app can choose from with FeedMatchSelector
 */
public class FeedFeedSelector extends XActivity implements MenuHandler
{
    private static final String TAG = "SB." + FeedFeedSelector.class.getSimpleName();

    private ListView           listView           = null;
    private ExpandableListView expListView        = null;
    private SearchView         etFilter           = null;

    private ShowTypesAdapter   loadTypesAdapter = null;
    //private SimpleELAdapter    loadFeedAdapter  = null;
    private ShowFeedsAdapter   showFeedsAdapter = null;

    //---------------------------------------------------------
    // Progress message
    //---------------------------------------------------------
    private ProgressDialog progressDialog = null;
    private void showProgress(int iResId, Object... oArgs) {
        if ( progressDialog == null ) {
            progressDialog = new ProgressDialog(this);
        }
        progressDialog.setIndeterminate(true);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage(getString(iResId, oArgs));
        try {
            progressDialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void hideProgress() {
        m_srlListView   .setRefreshing(false);
        m_srlExpListView.setRefreshing(false);

        if ( progressDialog != null ) {
            try {
                progressDialog.cancel(); // this fails after a rotate of orientation
                progressDialog.dismiss();
            } catch (Exception e) {
                progressDialog = null;
                //e.printStackTrace();
            }
        }
    }

    private SwipeRefreshLayout m_srlListView    = null; // should only contain a single listview/gridview
    private SwipeRefreshLayout m_srlExpListView = null;
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        etFilter = new SearchView(this);
        etFilter.setIconifiedByDefault(false);
        etFilter.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        etFilter.setVisibility(View.GONE);

        listView    = new ListView(this);
        expListView = new ExpandableListView(this);
        ll.addView(etFilter, 0);

        m_srlListView    = new SwipeRefreshLayout(this);
        m_srlExpListView = new SwipeRefreshLayout(this);
        m_srlListView   .addView(listView);
        m_srlExpListView.addView(expListView);
        final SwipeRefreshLayout.OnRefreshListener listener = new SwipeRefreshLayout.OnRefreshListener() {
            @Override public void onRefresh() {
                FeedFeedSelector.this.handleMenuItem(R.id.refresh);
            }
        };
        m_srlListView   .setOnRefreshListener(listener);
        m_srlExpListView.setOnRefreshListener(listener);
        ll.addView(m_srlListView);
        ll.addView(m_srlExpListView);

        changeStatus(Status.LoadingTypes);

        super.setContentView(ll);

        ViewUtil.setFullScreen(getWindow(), PreferenceValues.showFullScreen(this));

        loadTypesAdapter = new ShowTypesAdapter(this, m_childClicker, m_childClicker);
        listView.setAdapter(loadTypesAdapter);

        //loadFeedAdapter = new LoadFeedAdapter(this, getString(R.string.fetching_data));
        //expListView.setAdapter(loadFeedAdapter);
        expListView.setOnGroupExpandListener  (m_groupStatusRecaller);
        expListView.setOnGroupCollapseListener(m_groupStatusRecaller);
        expListView.setOnChildClickListener   (m_childClicker);
        expListView.setOnItemLongClickListener(m_childClicker);
        //expListView.setVisibility(View.GONE);

        ColorPrefs.setColors(this, expListView);
        ColorPrefs.setColors(this, etFilter);
    }

    @Override public boolean handleMenuItem(int menuItemId, Object... ctx) {
        switch (menuItemId) {
            case android.R.id.home: // contains a 'back arrow' so treat it like the back button
                onBackPressed();
                break;
            case R.id.close:
            case R.id.cmd_cancel:
                finish();
                return true;
            case R.id.refresh:
                switch (m_status) {
                    case SelectType:
                        loadTypesAdapter.load(false);
                        break;
                    case SelectFeed:
                        showFeedsAdapter.load(false);
                        break;
                }

                //expListView.setAdapter(loadFeedAdapter);
                //loadFeedAdapter.load(false);
                return true;
            case R.id.expand_all:
                ExpandableListUtil.expandAll(expListView);
				return true;
            case R.id.collapse_all:
                ExpandableListUtil.collapseAll(expListView);
				return true;
            case R.id.filter:
                initFiltering();
                return true;
        }
        return false;
    }

    private Status m_status = Status.SelectType;
    void changeStatus(Status statusNew) {
        boolean bShowFilter = statusNew.equals(Status.SelectFeed);
        ViewUtil.setMenuItemsVisibility(m_menu, new int[]{R.id.filter}, bShowFilter);

        switch (statusNew) {
            case LoadingTypes:
                showProgress(R.string.loading);
                m_childClicker.setDisabled(true);
                m_srlListView   .setVisibility(View.VISIBLE);
                m_srlExpListView.setVisibility(View.GONE);
                break;
            case LoadingFeeds:
                showProgress(R.string.loading);
                m_childClicker.setDisabled(true);
                m_srlExpListView.setVisibility(View.VISIBLE);
                m_srlListView   .setVisibility(View.GONE);
                break;
            case SelectFeed:
                m_childClicker.setDisabled(false);
                hideProgress();
                break;
            case SelectType:
                ExpandableListUtil.expandAllOrFirst(expListView, 20);
                m_childClicker.setDisabled(false);
                hideProgress();
                break;
        }

        m_status = statusNew;
    }

    void postLoad(String sRoot, List<String> lGroupsWithActive) {
        // adapter is now loaded, let the gui know
        expListView.setAdapter(showFeedsAdapter);
        showFeedsAdapter.notifyDataSetChanged();

        m_childClicker.setDisabled(false);
        m_groupStatusRecaller.setMode(sRoot + "." + m_status);
        int iExpandedAfterRestore = ExpandableListUtil.restoreStatus(expListView, m_groupStatusRecaller);
        if ( iExpandedAfterRestore <= 0 ) {
            ExpandableListUtil.collapseAll(expListView);
            if ( ListUtil.isNotEmpty(lGroupsWithActive) /*&& ListUtil.size(lGroupsWithActive) < 4*/ ) {
                ExpandableListUtil.expandGroups(expListView, lGroupsWithActive);
            } else {
                ExpandableListUtil.expandAllOrFirst(expListView, 4);
            }
        }

        changeStatus(com.doubleyellow.scoreboard.feed.Status.SelectFeed);
    }

    private Menu m_menu = null;
    @Override public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.feedselector, menu);
        m_menu = menu;

        ViewUtil.setMenuItemsVisibility(menu, new int[]{R.id.filter}, m_status.equals(Status.SelectFeed));

        return true;
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        handleMenuItem(item.getItemId());
        return true;
    }

    private GroupStatusRecaller m_groupStatusRecaller = GroupStatusRecaller.getInstance(FeedFeedSelector.class.getSimpleName());
    private ChildClicker m_childClicker = new ChildClicker();

    private class ChildClicker implements ExpandableListView.OnChildClickListener, AdapterView.OnItemLongClickListener, View.OnClickListener, View.OnLongClickListener
    {
        private boolean bDisabled = false;
        void setDisabled(boolean b) {
            this.bDisabled = b;
        }

        private void fetchUrls(final JSONArray aUrls, final String sName, final JSONArray arrayMerged, final JSONArray arrayBackup) {
            String sURL = (String) aUrls.remove(0);
            showProgress(R.string.loading_of_x, sName, sURL);
            URLFeedTask task = new URLFeedTask(FeedFeedSelector.this, sURL);
            task.setContentReceiver(new ContentReceiver() {
                @Override public void receive(String sJson, FetchResult fetchResult, long lCacheAge, String sLastSuccessfulContent) {
                    try {
                        Log.d(TAG, "Fetchresult :" + fetchResult + " (" + aUrls.length() + ")" );
                        if ( fetchResult.equals(FetchResult.OK) == false ) {
                            if ( JsonUtil.isNotEmpty(arrayBackup) ) {
                                JsonUtil.mergeJsonArrays(arrayMerged, arrayBackup);
                            }
                        } else {
                            JSONArray arrayExt = new JSONArray(sJson);
                            JsonUtil.mergeJsonArrays(arrayMerged, arrayExt);
                        }

                        if ( aUrls.length() > 0 ) {
                            fetchUrls(aUrls, sName, arrayMerged, arrayBackup);
                        } else {
                            PreferenceValues.addFeedTypeToMyList(FeedFeedSelector.this, sName);
                            changeStatus(Status.LoadingFeeds);
                            showFeedsAdapter = new ShowFeedsAdapter(FeedFeedSelector.this, arrayMerged, sName, m_sortOrder);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
            task.execute();
        }

        SortOrder m_sortOrder = SortOrder.Ascending;

        @Override public void onClick(View v) {
          //Log.d(TAG, "Implement onClick");
            final String sName = (String) v.getTag(); // this is the technical name like 'squashvlaanderen.toernooi.nl.leagues', not the display name

            JSONObject joType = loadTypesAdapter.joMetaData.optJSONObject(sName);
            if ( (joType != null) && "None".equalsIgnoreCase(joType.optString(FeedKeys.SortOrder.toString())) ) {
                m_sortOrder = null;
            }

            final JSONArray array = loadTypesAdapter.joRoot.optJSONArray(sName);
            if ( loadTypesAdapter.joRoot.has(sName + "." + FeedKeys.URL) ) {
                final Object oUrls = loadTypesAdapter.joRoot.opt(sName + "." + FeedKeys.URL);
                JSONArray aUrls = new JSONArray();
                if ( oUrls instanceof String ) {
                    aUrls.put(oUrls);
                } else if ( oUrls instanceof JSONArray ) {
                    // clone the array
                    String json = ((JSONArray) oUrls).toString();
                    try {
                        aUrls = new JSONArray(json);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                if ( JsonUtil.isNotEmpty(aUrls) ) {
                    final JSONArray arrayMerged = new JSONArray();
                    fetchUrls(aUrls, sName, arrayMerged, array);
                }
            } else if ( JsonUtil.isNotEmpty(array) ) {
                PreferenceValues.addFeedTypeToMyList(FeedFeedSelector.this, sName);
                changeStatus(Status.LoadingFeeds);
                showFeedsAdapter = new ShowFeedsAdapter(FeedFeedSelector.this, array, sName, m_sortOrder);
            } else {
                Toast.makeText(FeedFeedSelector.this, "No feeds in " + sName + " ... yet...", Toast.LENGTH_SHORT).show();
            }
        }

        @Override public boolean onLongClick(View v) {
          //Log.d(TAG, "Implement onLongClick");
            String    sName     = (String) v.getTag();
            JSONArray jsonArray = loadTypesAdapter.joRoot.optJSONArray(sName);
            AlertDialog.Builder ab = ScoreBoard.getAlertDialogBuilder(FeedFeedSelector.this);
            StringBuilder sb = new StringBuilder();
            sb.append(sName).append(" (").append(JsonUtil.size(jsonArray)).append(")").append("\n\n");

            JSONObject joType = loadTypesAdapter.joMetaData.optJSONObject(sName);
            if ( joType != null ) {
                for( FeedKeys key: FeedKeys.values() ) {
                    if ( key.equals(FeedKeys.Image    ) ) { continue; }
                    if ( key.equals(FeedKeys.BGColor  ) ) { continue; }
                    if ( key.equals(FeedKeys.TextColor) ) { continue; }
                    String sValue = joType.optString(key.toString());
                    if ( StringUtil.isNotEmpty(sValue) ) {
                        sb.append(key).append(": ").append(sValue).append("\n\n");
                    }
                }
            }
            ab.setMessage(sb.toString())
                    .setIcon(R.drawable.ic_action_web_site)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return true; // so that onclick is not triggered as well
        }

        @Override public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
            if ( this.bDisabled ) {
                return false;
            }
            switch (m_status) {
                case SelectType:
/*
                    String sName             =             loadFeedAdapter.getGroup(groupPosition).toString();
                    showFeedsAdapter.m_feeds = (JSONArray) loadFeedAdapter.getObject(groupPosition, childPosition);
                    if (showFeedsAdapter.m_feeds == null) {
                        return false;
                    }
                    changeStatus(Status.LoadingFeeds);
                    loadFeedAdapter.clear();
                    loadFeedAdapter.addItem(getString(R.string.loading), sName);
                    loadFeedAdapter.notifyDataSetChanged();
                    ExpandableListUtil.expandFirst(expListView);

                    showFeedsAdapter = new ShowFeedsAdapter(FeedFeedSelector.this, getLayoutInflater(), sName);
*/
                    return false;
                case SelectFeed:
                    JSONObject joFeed = (JSONObject) showFeedsAdapter.getObject(groupPosition, childPosition);
                    if (joFeed == null) {
                        return false;
                    }
                    Map<URLsKeys, String> newEntry = new HashMap<URLsKeys, String>();
                    for ( URLsKeys key : URLsKeys.values() ) {
                        String sValue = joFeed.optString(key.toString());
                        if (StringUtil.isNotEmpty(sValue)) {
                            newEntry.put(key, sValue);
                        }
                    }
                    String sNewURL = newEntry.get(URLsKeys.FeedMatches);
                    sNewURL = URLFeedTask.prefixWithBaseIfRequired(sNewURL);
                    Map<String, String> existingUrls2Name = PreferenceValues.getFeedPostDetailMap(FeedFeedSelector.this, URLsKeys.FeedMatches, URLsKeys.Name, true);
                    if ( existingUrls2Name.containsKey(sNewURL) ) {
                        String sMsg = getString(R.string.feed_x_already_exist_with_name_y, sNewURL, existingUrls2Name.get(sNewURL));
                        ScoreBoard.dialogWithOkOnly(FeedFeedSelector.this, sMsg);
                        return false;
                    }

                    PreferenceValues.addOrReplaceNewFeedURL(FeedFeedSelector.this, newEntry, true, true);

                    Intent intent = new Intent();
                    setResult(RESULT_OK, intent);
                    finish();
                    return true;
                default:
                    return false;
            }
        }

        @Override public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long id) {
            switch (m_status) {
                case SelectType:
                    return false;
                case SelectFeed:
                    Object itemAtPosition = adapterView.getItemAtPosition(i);
                    if (itemAtPosition instanceof JSONObject) {
                        JSONObject joFeed = (JSONObject) itemAtPosition;
                        StringBuilder sb = new StringBuilder(256);
                        for (URLsKeys key : URLsKeys.values()) {
                            String sValue = joFeed.optString(key.toString());
                            if (StringUtil.isNotEmpty(sValue)) {
                                sb.append(StringUtil.capitalize(key)).append(" : ").append(sValue).append("\n\n");
                            }
                        }

                        AlertDialog.Builder ab = ScoreBoard.getAlertDialogBuilder(FeedFeedSelector.this);
                        ab.setMessage(sb.toString())
                                .setIcon(R.drawable.ic_action_web_site)
                                .setPositiveButton(android.R.string.ok, null)
                                .show();
                        return true;
                    }
                    return false;
                default:
                    return false;
            }
        }
    }

    @Override public void onBackPressed() {
        if ( etFilter.getVisibility() == View.VISIBLE ) {
            //etFilter.setVisibility(View.GONE);
            searchEL.toggleSearchViewVisibility();
            return;
        }
        switch (m_status) {
            case SelectType:
                //just leave the activity
                super.onBackPressed();
                break;
            case SelectFeed:
                changeStatus(Status.LoadingTypes);

                m_srlListView   .setVisibility(View.VISIBLE);
                m_srlExpListView.setVisibility(View.GONE);

                listView.setAdapter(loadTypesAdapter);
                loadTypesAdapter.notifyDataSetChanged();

                m_groupStatusRecaller.setMode(m_status.toString());
                int iExpandedAfterRestore = ExpandableListUtil.restoreStatus(expListView, m_groupStatusRecaller);
                if ( iExpandedAfterRestore <= 0 ) {
                    ExpandableListUtil.collapseAll(expListView);
                }
                changeStatus(Status.SelectType);

                break;
        }
    }

    // ------------------------------------------
    // Filtering
    // ------------------------------------------

    private SearchEL searchEL = null;
    private void initFiltering() {
        if ( searchEL != null ) {
            searchEL.toggleSearchViewVisibility();
            return;
        }

        searchEL = new SearchEL(this, showFeedsAdapter, etFilter, expListView, true);
    }

}
