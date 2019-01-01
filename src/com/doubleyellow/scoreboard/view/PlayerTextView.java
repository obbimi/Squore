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

package com.doubleyellow.scoreboard.view;

import android.content.Context;
import android.graphics.Rect;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.support.v7.widget.AppCompatAutoCompleteTextView;
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
public class PlayerTextView extends AppCompatAutoCompleteTextView implements ContentReceiver
{
    private static final String TAG = "SB." + PlayerTextView.class.getSimpleName();

    /** Invoked when created for popup EditPlayers */
    public PlayerTextView(Context context) {
        super(context, null);
        init();
    }

    /** Invoked when using inflate e.g. from StaticMatchSelector */
    public PlayerTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        //init();
    }

    private void init() {
        this.setSingleLine();
        this.setPadding(10,0,0,0); // to have the cursor show up better
        int type = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS/* | InputType.TYPE_TEXT_VARIATION_PERSON_NAME*/;
        this.setInputType(type);
    }

/*
    @Override public boolean isSuggestionsEnabled() {
        return false;
    }
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
        Log.d(TAG, String.format("Focus changed: %s %d : focused %s, nextdown %d, nextforward %d", this.getHint(), this.getId(), focused, this.getNextFocusDownId(), this.getNextFocusForwardId()));
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
            List<String> lInput = new ArrayList<String>(Arrays.asList(sContent.split("[\r\n]+")));
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
        Context context = getContext();
        ArrayAdapter<String> adapter = new AutoSuggestAdapter<String>(context, iAutoCompleteLayoutResourceId, playerList, mt);

        // let 'when to suggest' depend on the size of the complete list
        //int iSuggestAfterAtLeast = Math.min(2, Math.max(1, ListUtil.size(playerList)/100));
        int iSuggestAfterAtLeast = PreferenceValues.numberOfCharactersBeforeAutocomplete(context);

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
		        Object oItem = parent.getItemAtPosition(position);
		        if ( oItem == null ) { return; }
                String sName = oItem.toString().trim();
                for ( Listener l : listeners ) {
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
