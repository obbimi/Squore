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

import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.*;
import android.widget.*;

import com.doubleyellow.android.util.SearchEL;
import com.doubleyellow.android.util.SimpleELAdapter;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.prefs.ColorPrefs;
import com.doubleyellow.util.*;
import android.widget.ExpandableListView.OnChildClickListener;

import java.util.List;

/**
 * Serves as a base for selecting matches from some kind of feed.
 * Each subclass should implement its own SimpleELAdapter and return an instance of that.
 */
public abstract class ExpandableMatchSelector extends Fragment
{
    public static final String HEADER_PREFIX = "-";
    public static final String NAMES_SPLITTER = " - ";

    protected static final String TAG = "SB." + ExpandableMatchSelector.class.getSimpleName();

    public abstract SimpleELAdapter getListAdapter(LayoutInflater inflater);
    //public abstract ListAdapter getSimpleListAdapter(LayoutInflater inflater);

    protected abstract OnChildClickListener                    getOnChildClickListener();
    protected abstract AdapterView.OnItemLongClickListener     getOnItemLongClickListener();
    protected abstract void                                    setGuiDefaults(List<String> lExpanded);

    protected Activity           activity = null;
    protected Context            context  = null;
    private   SearchView         etFilter = null; // http://www.mysamplecode.com/2012/11/android-expandablelistview-search.html

    private   ViewGroup          llContainer;
    /** not inflated, just constructed */
              String             m_sLastMessage = null;
    private   SwipeRefreshLayout swipeRefreshLayout; // should only contain a single listview/gridview
    public    ExpandableListView expandableListView;

    //---------------------------------------------------------
    // Round/Division/Date analysis
    //---------------------------------------------------------
    protected boolean appearsToBeADate(String sGroup) {
        return sGroup.matches("^\\d+\\s*.\\s*\\d+");
    }
    protected boolean appearsToBeARound(String sGroup) {
        final String sgroup = sGroup.toLowerCase();
        String[] saCheckFor = {"qualifi", "round", "final" };
        boolean bLikelyToBeARound = false;
        for(String sCheckFor: saCheckFor) {
            bLikelyToBeARound = bLikelyToBeARound || sgroup.contains(sCheckFor);
        }
        return bLikelyToBeARound;
    }

    //---------------------------------------------------------
    // Progress message
    //---------------------------------------------------------
    private ProgressDialog progressDialog = null;
    protected void showProgress(String sMsg, DialogInterface.OnCancelListener onCancelListener) {
        if ( swipeRefreshLayout != null ) {
            if ( swipeRefreshLayout.isRefreshing() == false ) {
                swipeRefreshLayout.setRefreshing(true);
            }
            if ( m_sLastMessage != null && m_sLastMessage.equals(sMsg) ) {
                // show progressDialog with message only on first refresh
                return;
            }
        }
        m_sLastMessage = sMsg;
        if ( progressDialog == null ) {
            progressDialog = new ProgressDialog(context);
        }
        progressDialog.setIndeterminate(true);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER); // STYLE_HORIZONTAL
        progressDialog.setMessage(sMsg);
        try {
            progressDialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if ( onCancelListener != null ) {
            progressDialog.setCancelable(true);
            progressDialog.setOnCancelListener(onCancelListener);
        } else {
            progressDialog.setCancelable(false);
        }
    }
    public void hideProgress() {
        if ( swipeRefreshLayout != null ) {
            swipeRefreshLayout.setRefreshing(false);
        }
        if ( progressDialog != null ) {
            try {
                //progressDialog.cancel(); // might fail if screen has rotated
                progressDialog.dismiss();
            } catch (Exception e) {
                //e.printStackTrace();
                progressDialog = null;
            }
        }
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Log.w(TAG, this.getClass().getSimpleName() + ": [1] onCreate called for ");

        // Retain this fragment across configuration changes.
        setRetainInstance(true);
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //Log.w(TAG, this.getClass().getSimpleName() + ": [2] onCreateView called for ");
        if ( llContainer == null ) {
            LinearLayout ll = new LinearLayout(context);
            ll.setOrientation(LinearLayout.VERTICAL);
            llContainer = ll;
        }

        SearchManager searchManager = null;
        try {
            searchManager = (SearchManager) context.getSystemService(Context.SEARCH_SERVICE); // does not work on wearable? android.os.ServiceManager$ServiceNotFoundException: No service published for: search
        } catch (Exception e) {
            e.printStackTrace();
        }
        if ( (etFilter == null) && (searchManager != null) ) {
            etFilter = new SearchView(context);
            etFilter.setIconifiedByDefault(false);
            etFilter.setSearchableInfo(searchManager.getSearchableInfo(activity.getComponentName()));
            llContainer.addView(etFilter);
            etFilter.setVisibility(View.GONE);
        }
        if ( expandableListView == null ) {
            swipeRefreshLayout = new SwipeRefreshLayout(context);
            expandableListView = new ExpandableListView(context);
            swipeRefreshLayout.addView(expandableListView);

            swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override public void onRefresh() {
                    Activity activity = ExpandableMatchSelector.this.getActivity();
                    if ( activity instanceof MenuHandler ) {
                        MenuHandler mh = (MenuHandler) activity;
                        mh.handleMenuItem(R.id.refresh);
                    }
                }
            });
            llContainer.addView(swipeRefreshLayout);
            if ( expandableListView instanceof ExpandableListView) {
                initGUI(inflater, expandableListView);
            }
            setGuiDefaults(null);

            ColorPrefs.setColors(activity, llContainer);
        }
        return llContainer;
    }

    private SearchEL searchEL = null;
    public void initFiltering(boolean bRememberLastSearch) {
        if ( searchEL != null ) {
            searchEL.toggleSearchViewVisibility();
            return;
        }

        searchEL = new SearchEL(context, getListAdapter(null), etFilter, expandableListView, bRememberLastSearch );
    }

    /**
     * Hold a reference to the parent Activity so we can report the task's current progress and results.
     * The Android framework will pass us a reference to the newly created Activity after each configuration change.
     * This deprecated method is still used e.g. in android 4.4
     */
    @Override public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = activity;
        this.context  = activity;
        //Log.d(TAG, "Attached " + this.getClass().getSimpleName() + " to the scoreBoard");
    }
    @Override public void onAttach(Context context) {
        super.onAttach(context);
        //this.activity = getActivity();
        this.context = context;
        if ( context instanceof Activity ) {
            this.activity = (Activity) context;
        }
        //Log.d(TAG, "Attached " + this.getClass().getSimpleName() + " to the scoreBoard");
    }

    /**
     * Set the callback to null so we don't accidentally leak the Activity instance.
     */
    @Override public void onDetach() {
        super.onDetach();
        activity = null; // MUST be done or e.g. StaticMatchSelector can not be used twice (causes problems since the 'MatchHistory' activity can be started from the PreviousMatchSelector)
    }

    @Override public Context getContext() {
        //return super.getContext();
        return context;
    }

    protected boolean bAutoLoad = false;
    public void setAutoLoad(boolean b) {
        this.bAutoLoad = b;
    }

    private void initGUI(LayoutInflater inflater, ExpandableListView expListView) {
        //ColorPrefs.setColors(activity, activity.findViewById(android.R.id.content));

        // setting list adapter
        SimpleELAdapter listAdapter = getListAdapter(inflater);
        expListView.setAdapter(listAdapter);

        // Listview Group click listener
        expListView.setOnChildClickListener   (getOnChildClickListener   ());
        expListView.setOnItemLongClickListener(getOnItemLongClickListener());
    }
}
