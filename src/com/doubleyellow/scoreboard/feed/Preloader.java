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
import com.doubleyellow.android.util.ContentReceiver;
import com.doubleyellow.android.util.KeyStoreUtil;
import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.URLFeedTask;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.model.Util;
import com.doubleyellow.scoreboard.prefs.PreferenceKeys;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.util.*;
import org.json.JSONObject;

import java.util.*;

/**
 * Takes care of refreshing data that was downloaded from a website earlier.
 * Mainly
 * - data from a feed
 * - all possible feeds
 */
public class Preloader extends AsyncTask implements ContentReceiver
{
    private static String TAG = "SB." + Preloader.class.getSimpleName();
    private static Preloader instance = null;

    private Map<Status, String> mStatus2Url = new HashMap<Status, String>();
    public static Preloader getInstance(Context context) {
        if ( instance == null ) {
            instance = new Preloader();
            instance.m_context = context;
            instance.execute(); // separate thread
            //instance.go(context);
        }
        return instance;
    }

    @Override protected Object doInBackground(Object[] params) {
        go(m_context);
        return null;
    }

/*
    @Override protected void onPostExecute(Object o) {
        // to e.g. allow the preloader to restart after rotation or so... normally not desired
        instance = null;
    }
*/

    private Context m_context = null;
    private void go(Context context) {
        if ( m_bFetching ) { return; }
        m_context = context;

        if ( mStatus2Url.size() == 0 ) {
            mStatus2Url.put(Status.WebConfig        , PreferenceValues.getWebConfigURL  (context));
            mStatus2Url.put(Status.ActiveMatchesFeed, PreferenceValues.getMatchesFeedURL(context));
            mStatus2Url.put(Status.ActivePlayersFeed, PreferenceValues.getPlayersFeedURL(context));
            mStatus2Url.put(Status.FeedOfFeeds      , PreferenceValues.getFeedsFeedURL  (context));
        }

        switch (m_status) {
            case Done:
            case NoConnection:
                return;
            case NotStarted:
                doNext(1);
                break;
        }
    }
    private synchronized void doNext(int iOffSet) {
        m_bFetching = true;
        if ( m_status.ordinal() == Status.values().length - 1 ) {
            return;
        }
        m_status = Status.values()[m_status.ordinal()+iOffSet];
        if ( mStatus2Url.containsKey(m_status) ) {
            String sURL = mStatus2Url.get(m_status);
            if ( StringUtil.isNotEmpty(sURL) ) {
                sURL = URLFeedTask.prefixWithBaseIfRequired(sURL);
                URLFeedTask urlFeedTask = new URLFeedTask(m_context, sURL);
                if ( m_status.equals(Status.FeedOfFeeds) ) {
                    urlFeedTask.setNrForUserAgent(PreferenceValues.getRunCount(m_context, PreferenceKeys.FeedFeedsURL));
                }
                urlFeedTask.setContentReceiver(this);
                urlFeedTask.execute();
            } else {
                doNext(1);
            }
/*
        } else if ( m_status.equals(Status.CountryCache) ) {
            Log.i(TAG, String.format("handling %s... ", m_status));
            try {
                // initialize the country util from json in resources (res folder)
                String sJson = ContentUtil.readRaw(m_context, R.raw.countries_list_01);
                JSONArray jsCountries = new JSONArray(sJson);
                CountryUtil.init(JsonUtil.toListOfMaps(jsCountries), "ISO3166-1-Alpha-2", "ISO3166-1-Alpha-3", "IOC", "FIFA", new String[] { "official_name_en", "name" });
            } catch (JSONException e) {
                e.printStackTrace();
            }
            doNext(1);
*/
        } else if ( m_status.equals(Status.ContactsCache) ) {
            Log.i(TAG, String.format("handling %s... ", m_status));
            PreferenceValues.getPlayerListAndContacts(m_context);
            doNext(1);
        } else if ( m_status.equals(Status.DynamicFeedNames) ) {
            doNext(1);
        } else if ( m_status.equals(Status.NoConnection) ) {
            doNext(1);
        } else if ( m_status.equals(Status.CountryFlags) ) {
            if ( false ) {
                // skip for now: eating to much resources
                doNext(1);
            }
            if ( PreferenceValues.useFlags(m_context) && PreferenceValues.prefetchFlags(m_context) ) {
                Runnable runnable = new DownloadFlags();
                runnable.run();
            }
            doNext(1);
        } else {
            Log.i(TAG, String.format("No next %s ", m_status));
        }
    }
    public Status getPreloadStatus() {
        return m_status;
    }

    private boolean m_bFetching = false;
    private Status m_status = Status.NotStarted;
    public enum Status {
        NotStarted,
        WebConfig,
        ActiveMatchesFeed,
      //CountryCache,
        ActivePlayersFeed,
        FeedOfFeeds,
        /** TODO: this next one should actually update the 'Name' of some of the defined feeds if they match on URL: ones like tournamentsoftware.com.001.php, tournamentsoftware.com.002.php */
        DynamicFeedNames,
        /** just a bookmark we can jump to if first feedfetching already fails */
        NoConnection,
        ContactsCache,
        CountryFlags,
        Done,
    }

    private static final String SHARED_SECRET = "YourSquore1h03v3";

    @Override public void receive(String sContent, FetchResult result, long lCacheAge, String sLastSuccessfulContent) {
        m_bFetching = false;
        switch (result) {
            case OK:
                Log.i(TAG, String.format("Fetched %s (cache age %s)", m_status, lCacheAge));
                if ( m_status == Status.WebConfig ) {
                    try {
                        JSONObject config = new JSONObject(sContent);
                        if ( Brand.brand != Brand.Squore ) {
                            Evaluation.ValidationResult eval = Evaluation.check( Brand.brand.toString()
                                                                               , config.optString(Evaluation.Config.ValidUntil          + "-" + Brand.brand)
                                                                               , config.optString(Evaluation.Config.BrandValidUntilHash + "-" + Brand.brand)
                                                                               , SHARED_SECRET
                                                                               , Util.isMyDevice(m_context)  );
                            Log.d(TAG, "Evaluation result " + eval);
                            ScoreBoard.brandedVersionValidation = eval;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if ( m_status.equals(Status.ActiveMatchesFeed) ) {
                    // if we only receive a header for the feed, or empty content, try another
                    sContent = sContent.trim();
                    if (StringUtil.isEmpty(sContent) || sContent.matches("^\\[.+\\]$")) {
                        // no matches from the current feed
                        final int iFeedPostUrlIdx = PreferenceValues.getInteger(PreferenceKeys.feedPostUrl, m_context, 0);
                        if ( iFeedPostUrlIdx == 0) {
                            if ( PreferenceValues.getMatchesFeedURLUnchanged() ) {
                                PreferenceValues.setActiveFeedNr(m_context, 1);
                                doNext(0);
                                break;
                            }
                        }
                    }
                }
                doNext(1);
                break;
            case FileNotFound:
                doNext(1); // continue with other internet resources that may be available
                break;
            case LoginToNetworkFirst:
            case NoCacheAndNetwork:
            case TimeoutError:
                doNext(1);
                break;
            case SSLHandshakeError: {
                    if ( PreferenceValues.allowTrustAllCertificatesAndHosts(m_context) ) {
                        try {
                            KeyStoreUtil.trustAllHttpsCertificates();
                            KeyStoreUtil.trustAllHostnames();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {

                    }
                    doNext(0);
                    break;
                }
            case NoNetwork:
            case UnexpectedContent:
            case UnexpectedError:
                m_status = Status.NoConnection;
                doNext(1); // do stuff that does not require an internet connection like caching contacts
                break;
            default:
                m_status = Status.NoConnection;
                break;
        }
    }

    private class DownloadFlags implements Runnable
    {
        @Override public void run() {
/*
            String sFlagURL              = PreferenceValues.getFlagsURL(m_context);
            int    iFlagMaxCacheAgeInMin = PreferenceValues.getMaxCacheAgeFlags(m_context);
            int iDownloads = CountryUtil.downloadFlags(m_context.getCacheDir(), sFlagURL, iFlagMaxCacheAgeInMin);
            Log.d(TAG, String.format("Number of country flag downloaded/refresh attempts %s", iDownloads));
*/

            final Params xxx3ToName = CountryUtil.getXXX3ToName();
            if ( MapUtil.isEmpty(xxx3ToName) ) { return; }
            List<String> lCountryCodesToFetch = new ArrayList(xxx3ToName.keySet());
            ListUtil.removeEmpty(lCountryCodesToFetch);
            while ( ListUtil.isNotEmpty(lCountryCodesToFetch) ) {
                String sCountryCode = lCountryCodesToFetch.remove(0);
                PreferenceValues.downloadImage(m_context, null, sCountryCode, 100);
            }
        }
    }
}
