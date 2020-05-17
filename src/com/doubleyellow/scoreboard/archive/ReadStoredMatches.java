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

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;
import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.PersistHelper;
import com.doubleyellow.scoreboard.match.ExpandableMatchSelector;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.model.Player;
import com.doubleyellow.scoreboard.prefs.PreferenceKeys;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.util.DateUtil;
import com.doubleyellow.util.FileUtil;
import com.doubleyellow.android.util.SimpleELAdapter;
import com.doubleyellow.util.StringUtil;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Used by PreviousMatchSelector to populate the list
 */
class ReadStoredMatches extends AsyncTask<String /* Params: input of execute() */, Void /* Progress */, Integer /*Result: return of doInBackground, input of postExecute*/ >  {
    public static final String TAG = "SB." + ReadStoredMatches.class.getSimpleName();

    private SimpleELAdapter         m_mainThreadAdapter  = null;
    private boolean                 m_bUseCacheIfPresent = true;
    private ExpandableMatchSelector m_ems                = null;

    ReadStoredMatches(ExpandableMatchSelector ems, SimpleELAdapter adapter, boolean bUseCacheIfPresent) {
        m_mainThreadAdapter  = adapter;
        m_bUseCacheIfPresent = bUseCacheIfPresent;
        m_ems                = ems;
    }

    /** Adapter modified outside the main thread */
    private static class StoreOnlyAdapter extends SimpleELAdapter {
        private StoreOnlyAdapter() {
            super(null, 0, 0, null, false);
        }
        @Override public void load(boolean bUseCacheIfPresent) {

        }
    }

    @Override protected void onPreExecute() {
        super.onPreExecute();
        Log.d(TAG, "onPreExecute");
    }

    private SimpleELAdapter m_storeOnlyAdapter = new StoreOnlyAdapter();
    @Override protected Integer doInBackground(String... inputParam)
    {
        try {
            m_storeOnlyAdapter.clear();
            Activity activity = m_ems.getActivity();
            if ( activity == null ) {
                // happens on android 4.1. (my tablet API 17?) when returning from matchdetails
                return -1;
            }
            List<File> lAllMatchFiles = PreviousMatchSelector.getAllPreviousMatchFiles(activity);
            long lLastModified = FileUtil.getMaxLastModified(lAllMatchFiles);

            if ( lAllMatchFiles.size() == 0 ) {
                m_storeOnlyAdapter.addItems(PreviousMatchSelector.sNoMatchesStored, null);
                //m_mainThreadAdapter.notifyDataSetChanged();
            } else {
                boolean bRefreshed = false;
                File fCache = m_storeOnlyAdapter.getCacheFile(activity);
                if ( fCache.exists() && ( lLastModified > fCache.lastModified() ) ) {
                    fCache.delete();
                }
                if ( m_bUseCacheIfPresent && fCache.exists() ) {
                    if ( m_storeOnlyAdapter.createFromCache(fCache) == false ) {
                        populate(lAllMatchFiles, m_storeOnlyAdapter);
                        bRefreshed = true;
                    }
                } else {
                    populate(lAllMatchFiles, m_storeOnlyAdapter);
                    bRefreshed = true;
                }
                if (bRefreshed) {
                    final boolean bCacheCreated = m_storeOnlyAdapter.createCache(fCache);
                    Log.i(TAG, "Cache created " + fCache.getPath());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "", e);
        } finally {

        }
        return m_storeOnlyAdapter.getChildrenCount();
    }

    @Override protected void onPostExecute(Integer iCount) {
        //super.onPostExecute(s); // default does nothing

        // back on the main thread.... we may now modify the actual adapter
        m_mainThreadAdapter.copyContentFrom(m_storeOnlyAdapter);

        m_mainThreadAdapter.notifyDataSetChanged();
        m_ems.hideProgress(); // just to be sure, for if swipe is used
    }

    private void populate(List<File> lAllMatchFiles, SimpleELAdapter adapter) {
        boolean bFastRead = (lAllMatchFiles.size() >= 20); // for performance reasons: fast read only tries to read name, date, time after parsing

        Activity activity = m_ems.getActivity();
        GroupMatchesBy sortBy    = PreferenceValues.groupArchivedMatchesBy(activity); // Event, Date
        Collections.sort(lAllMatchFiles);
        final DateFormat sdf = android.text.format.DateFormat.getDateFormat(activity);
        final DateFormat sdfSortable = new SimpleDateFormat(DateUtil.YYYYMMDD_SLASH, PreferenceValues.getDeviceLocale(activity));
        Set<String> mEvents  = new HashSet<String>();
        Set<String> mRounds  = new HashSet<String>();
        Set<String> mPlayers = new HashSet<String>();
        Set<String> mClubs   = new HashSet<String>();

        for (File fMatchModel : lAllMatchFiles) {
            Model match = Brand.getModel();
            //match.setClean();
            String sJson = null;
            try {
                sJson = FileUtil.readFileAsString(fMatchModel);
            } catch (IOException e) {
                continue;
            }
            if ( StringUtil.isEmpty(sJson) ) {
                fMatchModel.delete();
                continue;
            }
            if ( match.fromJsonString(sJson, bFastRead) == null ) {
                // corrupt, or from wrong brand...
                Log.w(TAG, "Skipping " + fMatchModel.getName());
                continue;
            }
            if ( false && match.isDirty() ) { // TODO: why was this again: 20200515 called to many times.. so disabled ?
                try {
                    if ( bFastRead ) {
                        // re-read it fully first so nothing gets lost
                        match.fromJsonString(sJson, false);
                    }
                    fMatchModel.delete();
                    PersistHelper.storeAsPrevious(activity, match, true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            Date   dMatchDate    = match.getMatchDate();
            String sDate         = sdf.format(dMatchDate);
            String sDateSortable = sdfSortable.format(dMatchDate);
            {
                String sA = match.getClub(Player.A); mClubs.add(sA);
                String sB = match.getClub(Player.B); mClubs.add(sB);
            }
            String sA         = match.getName(Player.A); mPlayers.add(sA);
            String sB         = match.getName(Player.B); mPlayers.add(sB);
            String sResult    = match.getResult();
            String sMatchDesc = (match.getMatchStartTimeHH_Colon_MM() + " " + sA + " - " + sB).trim();

            if ( StringUtil.isNotEmpty(sResult) ) {
                if ( sResult.equals("0-0") == false ) {
                    if ( Brand.isRacketlon() ) {
                        if ( sResult.matches("^[AB].+") ) {
                            // sport in which games are not counted but total number of points: result starts with the player with the most points
                            Player pMostPoints = Player.valueOf(sResult.substring(0,1));
                            sResult = sResult.substring(1);
                            if ( pMostPoints.equals(Player.B) ) {
                                // ensure match description starts with the winner first
                                sMatchDesc = (match.getMatchStartTimeHH_Colon_MM() + " " + sB + " - " + sA).trim();
                            }
                        }
                        String sGameScores = match.getGameScores();
                        sResult += " (" + sGameScores + ")";
                    }

                    sMatchDesc += " : " + sResult;
                }
            }
            String sEventName = match.getEventName(); mEvents.add(sEventName);
            File fDuplicate = null;
            if ( sortBy.equals(GroupMatchesBy.Event)==false || StringUtil.isEmpty(sEventName) ) {
                fDuplicate = (File) adapter.addItem(sDateSortable, sMatchDesc, fMatchModel);
            } else {
                String sPrefix = match.getEventDivision();
                if ( StringUtil.isEmpty(sPrefix) ) {
                    sPrefix = match.getEventRound();
                } else {
                    sPrefix += " " + match.getEventRound();
                }
                sPrefix = sPrefix.trim();
                mRounds.add(sPrefix);
                if ( StringUtil.isNotEmpty(sPrefix) ) {
                    sMatchDesc = sPrefix + " : " + sMatchDesc;
                }
                fDuplicate = (File) adapter.addItem(sEventName, sDate + " " + sMatchDesc, fMatchModel);
            }

            // prevent duplicates
            if ( fDuplicate != null ) {
                fDuplicate.delete();
                fMatchModel.delete();
                try {
                    File fReal = PersistHelper.storeAsPrevious(activity, match, true);
                    adapter.addItem(sEventName, sDate + " " + sMatchDesc, fReal);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if ( PreferenceValues.useMyListFunctionality(activity) ) {
            PreferenceValues.addPlayersToList(activity, mPlayers);
            PreferenceValues.addStringsToList(activity, PreferenceKeys.eventList, mEvents);
            PreferenceValues.addStringsToList(activity, PreferenceKeys.roundList, mRounds);
            PreferenceValues.addStringsToList(activity, PreferenceKeys.clubList , mClubs );
        }
    }
}
