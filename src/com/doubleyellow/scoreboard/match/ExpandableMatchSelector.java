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
    static final String HEADER_PREFIX = "-";
    static final String NAMES_SPLITTER = " - ";

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
    protected String             m_sLastMessage = null;
    private   SwipeRefreshLayout swipeRefreshLayout; // should only contain a single listview/gridview
    protected ExpandableListView expandableListView;

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
        if ( etFilter == null ) {
            etFilter = new SearchView(context);
            etFilter.setIconifiedByDefault(false);
            SearchManager searchManager = (SearchManager) context.getSystemService(Context.SEARCH_SERVICE);
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
     */
    @Override public void onAttach(Activity activity) {
        super.onAttach(activity);
        //this.activity = getActivity();
        this.activity = activity;
        this.context = activity;
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
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR1) {
            activity = null; // MUST be done or e.g. StaticMatchSelector can not be used twice (causes problems since the 'MatchHistory' activity can be started from the PreviousMatchSelector)
        }
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
