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

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.Toast;
import com.doubleyellow.android.util.AndroidPlaceholder;
import com.doubleyellow.android.util.SimpleELAdapter;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.scoreboard.prefs.URLsKeys;
import com.doubleyellow.util.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

class ShowFeedsAdapter extends SimpleELAdapter {
    private String           m_sName          = null;

    private FeedFeedSelector feedFeedSelector = null;
    private Context          context          = null;

    private final AndroidPlaceholder placeholder = new AndroidPlaceholder(TAG);

    private String sGroupByFormat = "${" + URLsKeys.Country + "} ${" + URLsKeys.Region + "}"; // TODO: configurable: country/ month of start
    private String sDisplayFormat = /*"${DateIsBetween:" + DateUtil.getCurrentYYYYMMDD() + ":${ValidFrom},${ValidTo}," + DateUtil.YYYYMMDD + "} " +*/  "${" + URLsKeys.Name    + "} - ${" + URLsKeys.Organization + "}";
    private String sRangeFormat   = "${DateDiff:${FirstOfList:~${ValidFrom}~${ValidTo}~" + DateUtil.getCurrentYYYYMMDD() + "~}},${DateDiff:${FirstOfList:~${ValidTo}~${ValidFrom}~" + DateUtil.getCurrentYYYYMMDD() + "~}}";

    private int m_iNrToFarInFuture   = 0;
    private int m_iNrAlreadyFinished = 0;
    private int m_iNrRunningToLong   = 0;
    private int m_iNrOfItemsToSelect = 0;

    ShowFeedsAdapter(FeedFeedSelector feedFeedSelector, LayoutInflater inflater, JSONArray array, String sName) {
        super(inflater, R.layout.expandable_match_selector_group, R.layout.expandable_match_selector_item, null, false);
        m_sName = sName;
        m_feeds = array;
        this.context = feedFeedSelector;
        this.feedFeedSelector = feedFeedSelector;

        sortHeaders(SortOrder.Ascending);
        sortItems  (SortOrder.Ascending);
        load(true);
    }

    @Override public void load(boolean bUseCacheIfPresent) {
        this.clear();
        ShowFeedsTask showFeedsTask = new ShowFeedsTask();
        showFeedsTask.execute(m_sName);
    }

    JSONArray m_feeds = null;

    private class ShowFeedsTask extends AsyncTask<String, Void, List<String>> {
        @Override protected List<String> doInBackground(String[] sName) {
            int iEndWasDaysBackMax   = PreferenceValues.getTournamentWasBusy_DaysBack     (context);
            int iStartIsDaysAheadMax = PreferenceValues.getTournamentWillStartIn_DaysAhead(context);
            int iDurationInDaysMax   = PreferenceValues.getTournamentMaxDuration_InDays   (context);

            List<String> lGroupsWithActive = new ArrayList<String>();

            for ( int f = 0; f < m_feeds.length(); f++ ) {

                JSONObject joFeed = null;
                try {
                    joFeed = m_feeds.getJSONObject(f);
                } catch (JSONException e) {
                    e.printStackTrace();
                    continue;
                }

                String sRange = placeholder.translate(sRangeFormat, joFeed);
                       sRange = placeholder.removeUntranslated(sRange);
                Range range = null;
                try {
                    range = Range.parse(sRange);
                } catch (IllegalArgumentException e) {
                    //e.printStackTrace(); // happens if e.g. valid to is empty
                }

                if ( range != null ) {
                    if ( range.getMaximum() < (-1 * iEndWasDaysBackMax)) {
                        m_iNrAlreadyFinished++;
                        Log.d(TAG, "Skipping already finished " + joFeed);
                        continue;
                    }
                    if ( range.getMinimum() > iStartIsDaysAheadMax ) {
                        m_iNrToFarInFuture++;
                        //Log.d(TAG, String.format("Skipping not starting within %s ", iStartIsDaysAheadMax) + joFeed);
                        continue;
                    }
                    if ( range.getSize() > iDurationInDaysMax ) {
                        m_iNrRunningToLong++;
                        //Log.d(TAG, String.format("Skipping running longer than %s ", iDurationInDaysMax) + joFeed);
                        continue;
                    }
                    String sHeader = placeholder.translate(sGroupByFormat, joFeed);
                           sHeader = placeholder.removeUntranslated(sHeader);
                    if ( StringUtil.isEmpty(sHeader) ) {
                        sHeader = context.getString(R.string.pref_Other);
                    }

                    // TODO: filter out those the user has already selected??

                    String sDisplayName = placeholder.translate(sDisplayFormat, joFeed);
                           sDisplayName = placeholder.removeUntranslated(sDisplayName);
                    if ( range.isIn(0) ) {
                        // in progress: also make them appear first in alphabetically sorted list
                        sDisplayName = "* " + sDisplayName;
                        lGroupsWithActive.add(sHeader);
                    }
                    ShowFeedsAdapter.this.addItem(sHeader, sDisplayName, joFeed);
                    m_iNrOfItemsToSelect++;
                } else {
                    // should not happen
                    Log.w(TAG, "could not determine range for " + joFeed);
                }
            }

            return lGroupsWithActive;
        }

        @Override protected void onPostExecute(List<String> lGroupsWithActive) {
            ShowFeedsAdapter.this.feedFeedSelector.postLoad(m_sName, lGroupsWithActive);
            if ( m_iNrOfItemsToSelect == 0 ) {
                if ( m_iNrAlreadyFinished + m_iNrToFarInFuture > 0 ) {
                    int tournamentWillStartIn_daysAhead = PreferenceValues.getTournamentWillStartIn_DaysAhead(context);
                    String sMsg  = String.format("No entries found starting in %s days.", tournamentWillStartIn_daysAhead);
                    if ( m_iNrAlreadyFinished > 0 ) {
                        sMsg += String.format("\n%s entries found that are already finished.", m_iNrAlreadyFinished);
                    }
                    if ( m_iNrToFarInFuture > 0 ) {
                        sMsg += String.format("\n%s entries found starting in more than %s days.", m_iNrAlreadyFinished, tournamentWillStartIn_daysAhead);
                    }
                    Toast.makeText(context, sMsg, Toast.LENGTH_LONG).show();
                }
            }
        }
    }
}
