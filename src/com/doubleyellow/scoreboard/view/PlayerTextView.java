package com.doubleyellow.scoreboard.view;

import android.content.Context;
import android.graphics.Rect;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import com.doubleyellow.android.util.ContentReceiver;
import com.doubleyellow.android.util.AutoSuggestAdapter;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.URLFeedTask;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.util.Enums;
import com.doubleyellow.util.ListUtil;
import com.doubleyellow.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Generic extension of an 'auto complete' text view for easy entering of player names.
 * After the first or second character all players stored in either
 * - the 'playerList' preference
 * - or are in the active playerList feed
 * are presented to the user for selection.
 *
 * This makes it much easier for the app user to quickly enter player names.
 */
public class PlayerTextView extends AutoCompleteTextView implements ContentReceiver
{
    private static final String TAG = "SB." + PlayerTextView.class.getSimpleName();

    /** Invoked when created for popup */
    public PlayerTextView(Context context) {
        super(context, null);
        this.setSingleLine();
        this.setPadding(10,0,0,0); // to have the cursor show up better
        this.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
    }

    public PlayerTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PlayerTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
/*
    @Override protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if ( this.getVisibility() == VISIBLE ) {
            initializeAdapter();
        }
    }
*/
    @Override protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        // for performance reasons: only set the 'autocomplete' adapter list if we focus on the gui element
        if ( focused ) {
            initializeAdapter();
        }
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
    }

    public interface Listener {
        void onSelected(String sName, PlayerTextView ptv);
    }
    private List<Listener> listeners = new ArrayList<Listener>();
    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    private boolean bAdapterIsInitialized = false;
    private void initializeAdapter() {
        if ( bAdapterIsInitialized ) {
            return;
        }
        bAdapterIsInitialized = true;
        long iStart = System.currentTimeMillis();
        String playersFeedURL = PreferenceValues.getPlayersFeedURL(getContext());
        if ( StringUtil.isNotEmpty(playersFeedURL) ) {
            URLFeedTask task = new URLFeedTask(getContext(), playersFeedURL);
            task.setContentReceiver(this);
            task.execute();
        } else {
            List<String> playerList = PreferenceValues.getPlayerListAndContacts(getContext());
            setAutoCompleteAdapter(playerList);
        }

        long iFinish = System.currentTimeMillis();
        Log.i(TAG, String.format("Initializing %s adapter for %s took %s ms", (StringUtil.isNotEmpty(playersFeedURL)?"Feed":"Contacts"), this.getId(), (iFinish - iStart)));
    }

    @Override public void receive(String sContent, FetchResult result, long lCacheAge, String sLastSuccessfulContent) {
        List<String> playerList = PreferenceValues.getPlayerListAndContacts(getContext());
        if ( (sContent == null) || (result.equals(FetchResult.OK) == false)) {
            // invalid feed url?
            // revert to list stored in preferences
            setAutoCompleteAdapter(playerList);
        } else {
            List<String> lInput = new ArrayList<String>(Arrays.asList(sContent.split("\n")));
            lInput.addAll(0, playerList);
            lInput = ListUtil.removeDuplicates(lInput);
            lInput = ListUtil.filter(lInput, "^\\[.*\\]$", Enums.Match.Remove); // e.g. to filter out heading(s) containing feed setting(s)
            setAutoCompleteAdapter(lInput);
        }
    }

    private final int iAutoCompleteLayoutResourceId = R.layout.expandable_match_selector_item;
    private void setAutoCompleteAdapter(List<String> playerList) {
        if ( ListUtil.isEmpty(playerList) ) { return; }

        AutoSuggestAdapter.MatchType mt = AutoSuggestAdapter.MatchType.hasCharactersInSequenceCI_singleCharMeansHasWordStartingWith;
        if ( ListUtil.size(playerList) > 300 ) {
            mt = AutoSuggestAdapter.MatchType.containsCI_singleCharMeansHasWordStartingWith;
        }
        if ( ListUtil.size(playerList) > 600 ) {
            mt = AutoSuggestAdapter.MatchType.startWithCI;
        }
        ArrayAdapter<String> adapter = new AutoSuggestAdapter<String>(getContext(), iAutoCompleteLayoutResourceId, playerList, mt);

        // let 'when to suggest' depend on the size of the complete list
        //int iSuggestAfterAtLeast = Math.min(2, Math.max(1, ListUtil.size(playerList)/100));
        int iSuggestAfterAtLeast = PreferenceValues.numberOfCharactersBeforeAutocomplete(getContext());

        this.setAdapter(adapter);
        this.setThreshold(iSuggestAfterAtLeast);
        bAdapterIsInitialized = true;

        if ( ListUtil.isNotEmpty(lSiblings) ) {
            for(PlayerTextView ptv: lSiblings) {
                ptv.setAutoCompleteAdapter(playerList);
            }
        }

        this.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long rowId) {
                String sName = parent.getItemAtPosition(position).toString().trim();
                for(Listener l : listeners) {
                    l.onSelected(sName, PlayerTextView.this);
                }
            }
        });

    }

    //------------------------------------
    // allow defining siblings and quickly initialize them with same data for autocomplete adapter
    //------------------------------------
    private List<PlayerTextView> lSiblings = new ArrayList<>();
    public void addSibling(PlayerTextView p) {
        lSiblings.add(p);
    }
/*
    private static View.OnFocusChangeListener onFocusChangeListener = new View.OnFocusChangeListener() {
        @Override public void onFocusChange(View view, boolean hasFocus) {
            if ( hasFocus && view instanceof AutoCompleteTextView) {
                AutoCompleteTextView autoCompleteTextView = (AutoCompleteTextView) view;
                autoCompleteTextView.showDropDown();
            }
        }
    };
    public void setShowDropDownOnFocus(boolean bActivate) {
        if ( bActivate ) {
            this.setOnFocusChangeListener(onFocusChangeListener);
        } else {
            this.setOnFocusChangeListener(null);
        }
    }
*/

}
