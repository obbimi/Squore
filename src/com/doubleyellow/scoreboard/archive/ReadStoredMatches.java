package com.doubleyellow.scoreboard.archive;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;
import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.match.ExpandableMatchSelector;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.model.Player;
import com.doubleyellow.scoreboard.prefs.PreferenceKeys;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.util.DateUtil;
import com.doubleyellow.util.FileUtil;
import com.doubleyellow.util.SimpleELAdapter;
import com.doubleyellow.util.StringUtil;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Used by PreviousMatchSelector to populate the list
 */
public class ReadStoredMatches extends AsyncTask<String, Void, String> {
    public static final String TAG = "SB." + ReadStoredMatches.class.getSimpleName();

    private SimpleELAdapter         adapter            = null;
    private boolean                 bUseCacheIfPresent = true;
    private ExpandableMatchSelector ems                = null;
    private Activity                activity           = null;

    public ReadStoredMatches(ExpandableMatchSelector ems, SimpleELAdapter adapter, boolean bUseCacheIfPresent) {
        this.adapter            = adapter;
        this.bUseCacheIfPresent = bUseCacheIfPresent;
        this.ems                = ems;
        this.activity           = ems.getActivity();
    }
    private class TmpAdapter extends SimpleELAdapter {
        private TmpAdapter() {
            super(null, 0, 0, null, false);
        }
        @Override public void load(boolean bUseCacheIfPresent) {

        }
    }

    private SimpleELAdapter tmpAdapter = new TmpAdapter();
    @Override protected String doInBackground(String... params) {

        try {
            tmpAdapter.clear();
            if (activity == null) {
                // happens on android 4.1. (my tablet API 17?) when returning from matchdetails
                return "";
            }
            List<File> lAllMatchFiles = PreviousMatchSelector.getPreviousMatchFiles(activity);
            long lLastModified = FileUtil.getMaxLastModified(lAllMatchFiles);

            if ( lAllMatchFiles.size() == 0 ) {
                tmpAdapter.addItems(PreviousMatchSelector.sNoMatchesStored, null);
                //adapter.notifyDataSetChanged();
            } else {
                boolean bRefreshed = false;
                File fCache = tmpAdapter.getCacheFile(activity);
                if ( fCache.exists() && ( lLastModified > fCache.lastModified() ) ) {
                    fCache.delete();
                }
                if ( bUseCacheIfPresent && fCache.exists() ) {
                    if (tmpAdapter.createFromCache(fCache) == false) {
                        populate(lAllMatchFiles, tmpAdapter);
                        bRefreshed = true;
                    }
                } else {
                    populate(lAllMatchFiles, tmpAdapter);
                    bRefreshed = true;
                }
                if (bRefreshed) {
                    final boolean bCacheCreated = tmpAdapter.createCache(fCache);
                    Log.i(TAG, "Cache created " + fCache.getPath());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        }
        return "";
    }

    @Override protected void onPostExecute(String s) {
        //super.onPostExecute(s);
        // back on the main thread.... we may no modify the actual adapter
        this.adapter.copyContentFrom(tmpAdapter);

        this.adapter.notifyDataSetChanged();
        ems.hideProgress(); // just to be sure, for if swipe is used
    }

    private void populate(List<File> lAllMatchFiles, SimpleELAdapter adapter) {
        boolean bFastRead = (lAllMatchFiles.size() >= 20); // for performance reasons: fast read only tries to read name, date, time after parsing

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
            if ( match.isDirty() ) {
                try {
                    if ( bFastRead ) {
                        // re-read it fully first so nothing gets lost
                        match.fromJsonString(sJson, false);
                    }
                    fMatchModel.delete();
                    ScoreBoard.storeAsPrevious(activity, match, true);
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
                    File fReal = ScoreBoard.storeAsPrevious(activity, match, true);
                    adapter.addItem(sEventName, sDate + " " + sMatchDesc, fReal);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        PreferenceValues.addPlayersToList(activity, mPlayers);
        PreferenceValues.addStringsToList(activity, PreferenceKeys.eventList, mEvents);
        PreferenceValues.addStringsToList(activity, PreferenceKeys.roundList, mRounds);
        PreferenceValues.addStringsToList(activity, PreferenceKeys.clubList , mClubs );
    }
}
