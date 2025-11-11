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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.doubleyellow.android.util.AndroidPlaceholder;
import com.doubleyellow.android.util.ContentUtil;
import com.doubleyellow.android.util.SimpleELAdapter;
import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.URLFeedTask;
import com.doubleyellow.scoreboard.activity.IntentKeys;
import com.doubleyellow.scoreboard.dialog.MyDialogBuilder;
import com.doubleyellow.scoreboard.dialog.SelectFeed;
import com.doubleyellow.scoreboard.main.DialogManager;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.match.ExpandableMatchSelector;
import com.doubleyellow.scoreboard.match.Match;
import com.doubleyellow.scoreboard.match.MatchTabbed;
import com.doubleyellow.scoreboard.model.*;
import com.doubleyellow.scoreboard.model.Util;
import com.doubleyellow.scoreboard.prefs.PreferenceKeys;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.android.task.URLTask;
import com.doubleyellow.android.util.ContentReceiver;
import com.doubleyellow.scoreboard.prefs.URLsKeys;
import com.doubleyellow.scoreboard.view.ExpandableListUtil;
import com.doubleyellow.util.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fragment that allows the user to select a match from an internet feed defined in
 * feedPostUrls/feedPostUrl preferences.
 *
 * Used within MatchTabbed activity.
 *
 * If no matches are in the feed, and a 'players' feed is available it allows the user to browse the list of players and to select the first player of a match.
 */
public class FeedMatchSelector extends ExpandableMatchSelector
{
    private static final String TAG = "SB." + FeedMatchSelector.class.getSimpleName();

    private static String SETTING_REGEXP = null;
    static {
        StringBuilder sbAllOpsAsRegExp = new StringBuilder();
        sbAllOpsAsRegExp.append("(");
        for (PreferenceKeys key: PreferenceKeys.values()) {
            if ( sbAllOpsAsRegExp.length() > 1 ) sbAllOpsAsRegExp.append("|");
            sbAllOpsAsRegExp.append(key.toString());
        }
        for (URLsKeys key2: URLsKeys.values()) {
            if ( sbAllOpsAsRegExp.length() > 1 ) sbAllOpsAsRegExp.append("|");
            sbAllOpsAsRegExp.append(key2.toString());
        }
        sbAllOpsAsRegExp.append(")");

        SETTING_REGEXP = "\\[" + sbAllOpsAsRegExp + "\\s*=\\s*([^\\]]+)\\]";
    }
    private static final String HEADER_PREFIX_REGEXP = "^\\s*([+-])\\s*(.+)$";

    public static Map<PreferenceKeys, String> mFeedPrefOverwrites = new HashMap<>();

    private String     m_sNoMatchesInFeed = null;
    /** global list of team players */
    private JSONObject m_joTeamPlayers    = null;

    boolean m_bGroupByCourt     = false;
    final String  m_sIfGroupByCourtUseSectionAs = JSONKey.division.toString();
    //-------------------------------------------------------------------
    // Feed Config
    //-------------------------------------------------------------------

    private       JSONObject m_joFeedConfig           = null;
    private       String     m_sDisplayFormat_Players = null;
    private       String     m_sDisplayFormat_Matches = null;
    public static  final String DisplayFormat_PlayerDefault  = "${" + JSONKey.name + "} [${" + JSONKey.country + "}]";
    private static final String DisplayFormat_MatchDefault   = "${" + JSONKey.date + "} ${" + JSONKey.time + "} : ${FirstOfList:~${" + Player.A + "}~${A.name}~} [${A.country}] [${A.club}] - " +
                                                                                                                 "${FirstOfList:~${" + Player.B + "}~${B.name}~} [${B.country}] [${B.club}] : ${" + JSONKey.result + "} (${" + JSONKey.id + "})";
    private boolean    m_bHideCompletedMatches = false;

    private int readFeedConfig(JSONObject joRoot) throws Exception {
        if ( m_joFeedConfig != null ) {
            // read config section only once per feed, not e.g. when user is refreshing the feed
            return 0;
        }

        // if there is a config section, use it
        m_joFeedConfig = joRoot.optJSONObject(URLsKeys.config.toString());
        if ( m_joFeedConfig != null ) {
            m_sDisplayFormat_Players = m_joFeedConfig.optString(URLsKeys.Placeholder_Player.toString(), DisplayFormat_PlayerDefault);
            m_sDisplayFormat_Matches = m_joFeedConfig.optString(URLsKeys.Placeholder_Match .toString(), DisplayFormat_MatchDefault);
            m_sDisplayFormat_Players = canonicalizeJsonKeys(m_sDisplayFormat_Players);
            m_sDisplayFormat_Matches = canonicalizeJsonKeys(m_sDisplayFormat_Matches);

            Iterator<String> itPrefKeys = m_joFeedConfig.keys();
            while ( itPrefKeys.hasNext() ) {
                String sPref  = itPrefKeys.next();
                String sValue = m_joFeedConfig.getString(sPref);
                if ( sPref.startsWith("__") ) { continue; }
                try {
                    PreferenceKeys key = PreferenceKeys.valueOf(sPref);
                    mFeedPrefOverwrites.put(key, sValue);
                } catch (Exception e) {
                    // in stead of a PreferenceKeys it can also be a subset of URLsKeys
                    Map<URLsKeys, String> feedPostDetail = PreferenceValues.getFeedPostDetail(context);
                    URLsKeys[] keys = new URLsKeys[] { URLsKeys.PostResult, URLsKeys.LiveScoreUrl };
                    for(URLsKeys key: keys) {
                        if ( sPref.equals( key.toString() ) ) {
                            String sCurrent = feedPostDetail.get(key);
                            if ( sValue.equals(sCurrent) == false && StringUtil.isNotEmpty(sValue) ) {
                                feedPostDetail.put(key, sValue);
                                PreferenceValues.addOrReplaceNewFeedURL(context, feedPostDetail, true, true);
                            }
                            if ( key.equals(URLsKeys.LiveScoreUrl) ) {
                                if ( sValue.trim().matches("^(tcp|mqtt|mqtts):.+") ) {
                                    PreferenceValues.setBoolean(PreferenceKeys.UseMQTT, context, true);
                                    PreferenceValues.setString(PreferenceKeys.MQTTBrokerURL_Custom, context, sValue);
                                    PreferenceValues.setString(PreferenceKeys.MQTTBrokerURL, context, getString(R.string.MQTTBrokerURL_Custom));
                                }
                            }
                        }
                    }
                }
            }
            if ( MapUtil.isNotEmpty(mFeedPrefOverwrites) ) {
                PreferenceValues.setOverwrites(mFeedPrefOverwrites);
                m_bHideCompletedMatches = PreferenceValues.hideCompletedMatchesFromFeed(context);
                changeAndNotify(m_feedStatus, true);
            }
        }
        return MapUtil.size(mFeedPrefOverwrites);
    }

    //-------------------------------------------------------------------

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        m_sNoMatchesInFeed = getString(R.string.No_matches_in_feed);
    }

    @Override public void onDestroy() {
        Log.d(TAG, "destroying activity");
        super.onDestroy();
        if ( emsAdapter != null ) {
            if ( /*emsAdapter.*/m_task != null ) {
                Log.d(TAG, "Cancelling emsAdapter.m_task");
                /*emsAdapter.*/m_task.cancel(true);
            } else {
                Log.d(TAG, "No m_task to cancel (1)");
            }
        } else {
            Log.d(TAG, "No emsadapter to cancel");
        }
        if ( m_task != null ) {
            Log.d(TAG, "Cancelling emsAdapter.m_task");
            m_task.cancel(true);
        } else {
            Log.d(TAG, "No m_task to cancel (2)");
        }
    }

    @Override protected void setGuiDefaults(List<String> lExpanded) {
        String feedPostName = PreferenceValues.getFeedPostName(context);
        if ( StringUtil.isEmpty(feedPostName) ) {
            // all feeds deleted?
/*
            // do not do that here: is already invoked as soon as the tab is partly visible as 'next' or 'previous' tab
            if ( activity instanceof MenuHandler ) {
                MenuHandler mh = (MenuHandler) activity;
                mh.handleMenuItem(R.id.uc_switch_feed);
            }
*/
        } else {
            GroupStatusRecaller groupStatusRecaller = GroupStatusRecaller.getInstance(feedPostName);
            int iExpandedAfterRestore = ExpandableListUtil.restoreStatus(expandableListView, groupStatusRecaller);
            if ( iExpandedAfterRestore <= 0 ) {
                if (ListUtil.isNotEmpty(lExpanded)) {
                    ExpandableListUtil.expandGroups(expandableListView, lExpanded);
                } else if ( lExpanded != null ) {
                    ExpandableListUtil.expandFirst(expandableListView);
                }
            }
            expandableListView.setOnGroupExpandListener(groupStatusRecaller);
            expandableListView.setOnGroupCollapseListener(groupStatusRecaller);
        }
    }

    @Override public ExpandableListView.OnChildClickListener getOnChildClickListener() {
        return onChildClickListener;
    }

    private ChildClickListener onChildClickListener = new ChildClickListener();

    private class ChildClickListener implements ExpandableListView.OnChildClickListener
    {
        @Override public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
            if ( m_bDisabled ) {
                Log.d(TAG, "On click listener disabled");
                return false;
            }
            final Model model = Brand.getModel();

            SimpleELAdapter listAdapter  = getListAdapter(null);
            String          sGroup       = (String) listAdapter.getGroup(groupPosition); // Note: is court if m_bGroupByCourt is true
            Object          oMatch       = emsAdapter.getObject(groupPosition, childPosition);
            String          feedPostName = PreferenceValues.getFeedPostName(context);
            boolean bNamesPopulated = false;
            if ( oMatch instanceof JSONObject ) {
                JSONObject joMatch = (JSONObject) oMatch;
                bNamesPopulated = populateModelFromJSON(model, joMatch, sGroup, feedPostName);
            } else {
                String sText = SimpleELAdapter.getText(v);
                if ( populateModelFromString(model, sText, sGroup, feedPostName) ) {
                    return false;
                }
                bNamesPopulated = true;
            }

            final String NO_PLAYERS_FOR_TEAM_X = "Not presenting dialog to select players. No players found for %s";
            if ( bNamesPopulated ) {
                // player names already populated
                finishWithPopulatedModel(model);
            } else if ( JsonUtil.isEmpty(getTeamPlayers(context, Player.A)) && JsonUtil.isEmpty(getTeamPlayers(context,Player.B)) ) {
                // not all player names populated, but also no lists to select players from
                finishWithPopulatedModel(model);
            } else if ( JsonUtil.isEmpty(getTeamPlayers(context, Player.A)) && JsonUtil.isNotEmpty(getTeamPlayers(context,Player.B)) ) {
                // player names not populated, but only 1 populated lists to select players from
                Toast.makeText(context, String.format(NO_PLAYERS_FOR_TEAM_X, model.getClub(Player.A)), Toast.LENGTH_SHORT).show();
                finishWithPopulatedModel(model);
            } else if ( JsonUtil.isEmpty(getTeamPlayers(context,Player.B)) && JsonUtil.isNotEmpty(getTeamPlayers(context,Player.A)) ) {
                // player names not populated, but only 1 populated lists to select players from
                Toast.makeText(context, String.format(NO_PLAYERS_FOR_TEAM_X, model.getClub(Player.B)), Toast.LENGTH_SHORT).show();
                finishWithPopulatedModel(model);
            } else {
                // only clubs names were specified, player names specified to select from
                SelectPlayersDialog dialog = new SelectPlayersDialog(FeedMatchSelector.this, context, model, m_joFeedConfig);
                dialog.show();
            }
            return true;
        }
        private boolean m_bDisabled = false;
        void setDisabled(boolean b) {
            m_bDisabled = b;
        }
    }

    public void finishWithPopulatedModel(Model model) {
        if ( (m_joFeedConfig != null) && m_joFeedConfig.optBoolean(URLsKeys.skipMatchSettings.toString(), false) ) {
            // typically a JSON feed with all appropriate settings in the feed, no need to change settings
            if ( FeedStatus.showingPlayers.equals(getFeedStatus()) == false ) {
                Match.dontShow();
                PreferenceValues.setOverwrites(mFeedPrefOverwrites);
            }
        }

        Intent intent = new Intent();
        intent.putExtra(IntentKeys.NewMatch.toString(), model.toJsonString(null)); // this is read by ScoreBoard.onActivityResult
        activity.setResult(Activity.RESULT_OK, intent);
        activity.finish();
    }

    public static void switchFeed(Context context) {
        SelectFeed selectFeed = new SelectFeed(context, null, null);
        selectFeed.show();
    }

    // TODO: allow overwriting these regexp-es in feeds.json
    private static final String sRegExpDateTime = "("
                                                       + "(?:\\w{1,3}\\s)?"
                                                       + "[0-9:/\\s-]*"
                                                       + "(?:PM|AM|pm|am)?"
                                                + ")??";                                                 // group 1 (lazy quantifier so that first part 'Jan' of e.g. 'Jan Rot - Na Li' is not taken as date/time part
    private static final String sSeeding        = "(?:[\\[\\(]\\d+(?:/\\d+)?[\\]\\)])?"; // match seedings with numbers greater and equal to 10 (double digits)
    public  static final String sCountry        = "(?:[\\[\\(](\\w+)[\\]\\)])?";                         // group 3 and 6
    private static final String sClub           = "(?:[\\[\\(]([\\w\\s\\-]+)[\\]\\)])?";                 // group 4 and 7
    private static final String sRegExpPlayer   = "([^\\[\\(:]+" + sSeeding + ")[\\s]*" + sCountry + "[\\s]*" + sClub; // group 2 (+3+4) and 5 (+6+7)
    private static final String sRegExpResult   = "("
                                                   + "("
                                                   + "[0-9,/\\s\\(\\)\\-\\+]{1,}"                        // the plus sign is in there for Racketlon!
                                                   + "|Opgave|Resign|Retired|Penalty|Withdrawn|[Ww]alkover|Not played"
                                                   + ")?"                                                // group 8 (continued)
                                                + "|"
                                                   + "(?:[Ww]alkover)?"
                                                + ")?+";                                                 // group 9
    private static final String sPlayerSplitter = "\\s+-\\s+";

    private static final String sRegExpMatch    = "^" + sRegExpDateTime + "[\\s:]*" + sRegExpPlayer + sPlayerSplitter + sRegExpPlayer + "[\\s:]*+" + sRegExpResult + "(.*?)" + "$";
    private static final Pattern pMatchParsing  = Pattern.compile(sRegExpMatch);
    private static final Pattern pPlayerParsing = Pattern.compile("^" + sRegExpPlayer + "(.*?)" + "$");
    public static String getMatchDetailsFromMatchString(Model model, String sText, Context context, boolean bIsOnePlayerOnly) {
        return getMatchDetailsFromMatchString(model, sText, context, bIsOnePlayerOnly, false);
    }
    private static String getMatchDetailsFromMatchString(Model model, String sText, Context context, boolean bIsOnePlayerOnly, boolean bCleaned) {
        if ( model != null ) {
            model.clear();
        } else {
            model = Brand.getModel();
        }
        //Log.d(TAG, "Using reg exp : " + sRegExp);
        //Log.d(TAG, "Examining     : " + sText);
        Matcher m;
        if ( bIsOnePlayerOnly ) {
            m = pPlayerParsing.matcher(sText);
        } else {
            m = pMatchParsing.matcher(sText);
        }
        if ( m.find() ) {
            String sResult = null;
            if ( bIsOnePlayerOnly ) {
                String sPlayer1       = m.group(1);
                String sCountry1      = m.group(2);
                String sClub1         = m.group(3);
                String sUnknownEnd    = m.group(4);
                //Log.w(TAG, String.format("w: %s, p1: %s, p2: %s, r: %s", sDateTime, sPlayer1, sPlayer2, sResult));
                if ( PreferenceValues.removeSeedingAfterMatchSelection(context) ) {
                    sPlayer1 = Util.removeSeeding(sPlayer1);
                }
                if ( StringUtil.isEmpty(CountryUtil.getIso2(sCountry1)) && StringUtil.isNotEmpty( CountryUtil.getIso2(sClub1)) ) {
                    // swap country and club
                    String sTmp = sClub1;
                                  sClub1 = sCountry1;
                           sCountry1 = sTmp;
                }
                model.setPlayerName   (Player.A, sPlayer1.trim() );
                model.setPlayerCountry(Player.A, sCountry1 );
                model.setPlayerClub   (Player.A, sClub1 );
            } else {
                String sDateTime      = m.group( 1);
                String sPlayer1       = m.group( 2);
                String sCountry1      = m.group( 3);
                String sClub1         = m.group( 4);
                String sPlayer2       = m.group( 5);
                String sCountry2      = m.group( 6);
                String sClub2         = m.group( 7);
                       sResult        = m.group( 8);
                String sResultAndText = m.group( 9);
                String sUnknownEnd    = m.group(10);
                //Log.w(TAG, String.format("w: %s, p1: %s, p2: %s, r: %s", sDateTime, sPlayer1, sPlayer2, sResult));
                if ( PreferenceValues.removeSeedingAfterMatchSelection(context) ) {
                    sPlayer1 = Util.removeSeeding(sPlayer1);
                    sPlayer2 = Util.removeSeeding(sPlayer2);
                }

                // check if found club is actually a country
                String sIsoCC1 = CountryUtil.getIso2(sCountry1);
                String sIsoCC2 = CountryUtil.getIso2(sCountry2);
                boolean bOneOfCountriesIsInvalid = StringUtil.isEmpty(sIsoCC1) || StringUtil.isEmpty(sIsoCC2);
                if ( StringUtil.isEmpty(sIsoCC1) && StringUtil.isNotEmpty(CountryUtil.getIso2(sClub1))) {
                    //  swap country and club
                    String sTmp = sClub1; sClub1 = sCountry1; sCountry1 = sTmp;
                }
                if ( StringUtil.isEmpty(sIsoCC2) && StringUtil.isNotEmpty(CountryUtil.getIso2(sClub2))) {
                    // swap country and club
                    String sTmp = sClub2; sClub2 = sCountry2; sCountry2 = sTmp;
                }

                // check if found countries are most likely actually a clubs
                if ( StringUtil.isEmpty(sClub1) && StringUtil.isNotEmpty(sCountry1) && bOneOfCountriesIsInvalid ) {
                    //  swap country and club
                    sClub1 = sCountry1;
                    sCountry1 = "";
                }
                if ( StringUtil.isEmpty(sClub2) && StringUtil.isNotEmpty(sCountry2) && bOneOfCountriesIsInvalid ) {
                    //  swap country and club
                    sClub2 = sCountry2;
                    sCountry2 = "";
                }

                model.setPlayerName   (Player.A, sPlayer1.trim() );
                model.setPlayerName   (Player.B, sPlayer2.trim() );
                model.setPlayerCountry(Player.A, sCountry1 );
                model.setPlayerCountry(Player.B, sCountry2 );
                model.setPlayerClub   (Player.A, sClub1 );
                model.setPlayerClub   (Player.B, sClub2 );
                if ( StringUtil.isNotEmpty(sDateTime) ) {
                    sDateTime = sDateTime.replaceAll("\\s*:\\s*$", "");
                    model.setUnparsedDate(sDateTime);
                }
                if ( StringUtil.isNotEmpty(sResult) ) {
                    model.setResult(sResult);
                }
            }
            return sResult;
        } else {
            if ( bCleaned == false ) {
                // remove seedings: that usually holds a '/' as well... and retry
                String sCleaned = sText.replaceAll("[\\[\\(]\\d+/\\d+[\\]\\)]", "");
                return getMatchDetailsFromMatchString(model, sCleaned, context, bIsOnePlayerOnly, true);
            }
            Log.w(TAG, "For player: " + bIsOnePlayerOnly);
            Log.w(TAG, "Could not determine details:");
            Log.w(TAG, "String: " + sText);
            Log.w(TAG, "RegExp: " + sRegExpMatch);
            if ( false ) {
                String[] saRetry = { ""
                        , "^" + sRegExpDateTime + "[\\s:]*" + sRegExpPlayer + sPlayerSplitter + sRegExpPlayer + "[\\s:]*+" + sRegExpResult + "(.*?)" + "$"
                        , "^" + sRegExpDateTime + "[\\s:]*" + sRegExpPlayer + sPlayerSplitter + sRegExpPlayer + "[\\s:]*+"
                        , "^" + sRegExpDateTime + "[\\s:]*" + sRegExpPlayer + sPlayerSplitter
                        , "^" + sRegExpDateTime
                };
                for(String sRetryRE: saRetry) {
                    if ( StringUtil.isEmpty(sRetryRE) ) { continue; }
                    m = Pattern.compile(sRetryRE).matcher(sText);
                    if ( m.find() ) {
                        Log.d(TAG, "Did find something for RegExp: " + sRetryRE);
                        break;
                    } else {
                        Log.d(TAG, "Found nothing for RegExp: " + sRetryRE);
                    }
                }
            }
        }
        return null;
    }

    /* for when a match is selected from plain text feed */
    private boolean populateModelFromString(Model model, String sText, String sGroup, String feedPostName) {
        boolean bIsOnePlayerOnly = (m_feedStatus != null) && m_feedStatus.isShowingPlayers();
        getMatchDetailsFromMatchString( model, sText, context, bIsOnePlayerOnly);

        // use feed name and group name for event details
        setModelEvent(model, sGroup, feedPostName, null);

        return false;
    }

    /* for when a match is selected from a JSON feed */
    private boolean populateModelFromJSON(Model model, JSONObject joMatch, String sGroup, String feedPostName) {

        boolean bIsOnePlayerOnly = (m_feedStatus != null) && m_feedStatus.isShowingPlayers();
        if ( bIsOnePlayerOnly && joMatch.has(JSONKey.name.toString() )) {
            String sPlayer1  = joMatch.optString(JSONKey.name   .toString());
            String sCountry1 = joMatch.optString(JSONKey.country.toString());
            model.setPlayerName   (Player.A, sPlayer1.trim() );
            model.setPlayerCountry(Player.A, sCountry1 );
            return false;
        }

        boolean bNamesPopulated = true;
        try {
            for(Player p: Player.values() ) {
                Object oPlayer = joMatch.get(p.toString());
                String sName = String.valueOf(oPlayer);
                if ( oPlayer instanceof JSONObject ) {
                    JSONObject jsonObject = (JSONObject) oPlayer;
                    sName = jsonObject.optString(JSONKey.name.toString());
                    if ( StringUtil.isEmpty(sName) ) {
                        bNamesPopulated = false;

                        // check of list of names of players is available
                        JSONArray aPlayers = jsonObject.optJSONArray(JSONKey.teamPlayers.toString());
                        if ( aPlayers == null ) {
                            String sTeamId = jsonObject.optString(JSONKey.teamId.toString());
                            if ( StringUtil.isNotEmpty(sTeamId) ) {
                                // get teams from global part of feed config
                                if ( m_joTeamPlayers != null ) {
                                    aPlayers = m_joTeamPlayers.optJSONArray(sTeamId);
                                }
                            }
                        }
                        setTeamPlayers(context, p, aPlayers); // to show dialog where ref can select players of both teams
                    }
                    String sClub = jsonObject.optString(JSONKey.club.toString());
                    model.setPlayerClub   (p, sClub);
                    String sAbbreviation = jsonObject.optString(JSONKey.abbreviation.toString());
                    if ( StringUtil.isNotEmpty(sAbbreviation) && StringUtil.isNotEmpty(sClub) && (sClub.matches(".+\\]$") == false) ) {
                        model.setPlayerClub(p, sClub + " [" + sAbbreviation + "]");
                    }
                    String sColor = jsonObject.optString(JSONKey.color.toString());
                    if ( StringUtil.isNotEmpty(sColor) ) {
                        model.setPlayerColor(p, sColor);
                    }
                    model.setPlayerCountry(p, jsonObject.optString(JSONKey.country.toString()));
                    String sAvatar = jsonObject.optString(JSONKey.avatar.toString());

                    // avatars in one feed are often retrieved from the same server
                    if ( StringUtil.isNotEmpty(sAvatar) && (sAvatar.startsWith("http") == false) && (m_joFeedConfig != null) ) {
                        String sAvatarBaseURL = m_joFeedConfig.optString(URLsKeys.avatarBaseURL.toString());
                        if ( StringUtil.isNotEmpty(sAvatarBaseURL) ) {
                            sAvatar = sAvatarBaseURL + sAvatar;
                        }
                    }
                    model.setPlayerAvatar (p, sAvatar);

                }
                model.setPlayerName (p, sName);
            }
            String sResult = joMatch.optString(JSONKey.result.toString());
            if ( StringUtil.isNotEmpty(sResult) ) {
                model.setResult(sResult);
            }

            setMatchFormat(model, joMatch);

            if ( m_bGroupByCourt ) {
                if ( joMatch.has(m_sIfGroupByCourtUseSectionAs) ) {
                    sGroup = joMatch.getString(m_sIfGroupByCourtUseSectionAs);
                }
            }
            // use feed name and group name for event details
            setModelEvent(model, sGroup, feedPostName, joMatch);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        // use feed name and group name for event details
/*
        model.setEvent ( joMatch.optString(JSONKey.event   .toString(), feedPostName)
                       , joMatch.optString(JSONKey.division.toString(), sGroup)
                       , null, null);
*/
        return bNamesPopulated;
    }

    public static JSONArray getTeamPlayers(Context context, Player team) {
        try {
            File fCache = getTeamPlayersCacheFile(context, team);
            if ( fCache.exists() ) {
                JSONArray aPlayers = new JSONArray(FileUtil.readFileAsString(fCache));
                return aPlayers;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    public static void setTeamPlayers(Context context, Player team, JSONArray aPlayers) {
        try {
            File fCache                = getTeamPlayersCacheFile(context, team);
            if ( aPlayers != null ) {
                FileUtil.writeTo(fCache, aPlayers.toString());
            } else {
                if ( fCache.exists() ) {
                    fCache.delete();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static File getTeamPlayersCacheFile(Context context, Player team) {
        return new File(context.getCacheDir(), "teamPlayers." + team + ".json");
    }

    private void setMatchFormat(Model model, JSONObject joMatch) throws JSONException {
        // take match config from feed config (if it exists)
        if ( m_joFeedConfig != null ) {
            if ( m_joFeedConfig.has(PreferenceKeys.numberOfPointsToWinGame.toString() ) ) {
                model.setNrOfPointsToWinGame(m_joFeedConfig.getInt(PreferenceKeys.numberOfPointsToWinGame.toString()));
            }
            if ( m_joFeedConfig.has(PreferenceKeys.numberOfGamesToWinMatch.toString() ) ) {
                model.setNrOfGamesToWinMatch(m_joFeedConfig.getInt(PreferenceKeys.numberOfGamesToWinMatch.toString()));
            }
            if ( m_joFeedConfig.has(JSONKey.useHandInHandOutScoring.toString() ) ) {
                boolean bUseHandInOutScoring = m_joFeedConfig.getBoolean(JSONKey.useHandInHandOutScoring.toString());
                model.setEnglishScoring(bUseHandInOutScoring);
            }
            if ( m_joFeedConfig.has(JSONKey.tiebreakFormat.toString() ) ) {
                String sTBF = m_joFeedConfig.getString(JSONKey.tiebreakFormat.toString());
                try {
                    TieBreakFormat tbf = TieBreakFormat.valueOf(sTBF);
                    model.setTiebreakFormat(tbf);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // optionally overwrite match format by properties specified on match level
        if ( joMatch.has(PreferenceKeys.numberOfPointsToWinGame.toString() ) ) {
            model.setNrOfPointsToWinGame(joMatch.getInt(PreferenceKeys.numberOfPointsToWinGame.toString()));
        }
        if ( joMatch.has(PreferenceKeys.numberOfGamesToWinMatch.toString() ) ) {
            model.setNrOfGamesToWinMatch(joMatch.getInt(PreferenceKeys.numberOfGamesToWinMatch.toString()));
        }
        try {
            if ( joMatch.has(PreferenceKeys.tieBreakFormat.toString() ) ) {
                model.setTiebreakFormat(TieBreakFormat.valueOf(joMatch.getString(PreferenceKeys.tieBreakFormat.toString())));
            }
            if ( joMatch.has(PreferenceKeys.useHandInHandOutScoring.toString() ) ) {
                model.setEnglishScoring(joMatch.getBoolean(PreferenceKeys.useHandInHandOutScoring.toString()));
            }
            if ( joMatch.has(PreferenceKeys.timerPauseBetweenGames.toString() ) ) {
                PreferenceValues.setOverwrite(PreferenceKeys.timerPauseBetweenGames, joMatch.getInt(PreferenceKeys.timerPauseBetweenGames.toString()));
            }
            if ( joMatch.has(PreferenceKeys.timerPauseBeforeFirstGame.toString() ) ) {
                PreferenceValues.setOverwrite(PreferenceKeys.timerPauseBeforeFirstGame, joMatch.getInt(PreferenceKeys.timerPauseBeforeFirstGame.toString()));
            }
        } catch (Exception e) {
        }
    }

    private void setModelEvent(Model model, String sGroup, String feedPostName, JSONObject joMatch) {
        Map<URLsKeys, String> feedPostDetail = PreferenceValues.getFeedPostDetail(context);
        if ( MapUtil.isNotEmpty(feedPostDetail) && feedPostDetail.containsKey(URLsKeys.FeedMatches) ) {
            model.setSource(feedPostDetail.get(URLsKeys.FeedMatches), null);
        }
        String sEventName = null;
        if ( PreferenceValues.useFeedNameAsEventName(context) ) {
            sEventName = feedPostName;
        }
        String sLocation = "";
        if ( MapUtil.isNotEmpty(feedPostDetail) && feedPostDetail.containsKey(URLsKeys.Region) ) {
            sLocation = feedPostDetail.get(URLsKeys.Region);
        }
        if ( MapUtil.isNotEmpty(feedPostDetail) && feedPostDetail.containsKey(URLsKeys.Country) ) {
            sLocation = (StringUtil.isNotEmpty(sLocation)? (sLocation + ", ") :"") + feedPostDetail.get(URLsKeys.Country);
        }
        String sFieldDivision = "";
        String sEventRound    = "";
        if ( StringUtil.isNotEmpty(sGroup) && (appearsToBeADate(sGroup) == false) ) {
            // make educated guess
            if( appearsToBeARound(sGroup) ) {
                sEventRound = sGroup;
            } else {
                sFieldDivision = sGroup;
            }
        }
        if ( joMatch != null ) {
            String sSourceID = joMatch.optString(JSONKey.sourceID.toString()); // not preferred, should be used internally for model only
                   sSourceID = joMatch.optString(JSONKey.id      .toString(), sSourceID);
            if ( StringUtil.isNotEmpty(sSourceID) ) {
                model.setSource(null, sSourceID);
            }
            String sCourt = joMatch.optString(JSONKey.court.toString());
            model.setCourt(sCourt);

            sEventName     = joMatch.optString(JSONKey.name    .toString(), sEventName);
            sFieldDivision = joMatch.optString(JSONKey.division.toString(), sFieldDivision); // field?
            sFieldDivision = joMatch.optString(JSONKey.field   .toString(), sFieldDivision); // field?
            sEventRound    = joMatch.optString(JSONKey.round   .toString(), sEventRound);
            sLocation      = joMatch.optString(JSONKey.location.toString(), sLocation);
        }
        model.setEvent(sEventName, sFieldDivision, sEventRound, sLocation);
    }

    /** will trigger a popup showing the Model data. More for troubleshooting */
    @Override public AdapterView.OnItemLongClickListener getOnItemLongClickListener()
    {
        return new AdapterView.OnItemLongClickListener() {
            @Override public boolean onItemLongClick(AdapterView<?> adapterView, final View view, int iPos, final long id) {
                Object itemAtPosition = adapterView.getItemAtPosition(iPos);
                if ( m_feedStatus.equals(FeedStatus.showingPlayers) ) {
                    SimpleELAdapter listAdapter = getListAdapter(null);
                    if ( listAdapter != null ) {
                        if ( itemAtPosition instanceof String ) {
                            List<String> lChilds = listAdapter.getChilds((String) itemAtPosition);
                            if ( ListUtil.isNotEmpty(lChilds) ) {
                                MyDialogBuilder.dialogWithOkOnly(context, String.format("%s : %d", itemAtPosition, ListUtil.size(lChilds)));
                                return true;
                            }
                        } else if ( itemAtPosition instanceof JSONObject) {
                            JSONObject jo = (JSONObject) itemAtPosition;
                            MyDialogBuilder.dialogWithOkOnly(context, jo.toString() );
                            return true;
                        }
                    }
                    return false;
                }

                Model mDetails = Brand.getModel();
                if ( itemAtPosition instanceof JSONObject ) {
                    boolean bNamesPopulated = populateModelFromJSON(mDetails, (JSONObject) itemAtPosition, null, PreferenceValues.getFeedPostName(context));
                } else if (itemAtPosition instanceof String) {
                    String sText = (String) itemAtPosition;
                    if ( sText.startsWith("http") ) {
                        // TODO: copy to clipboard, or open actual source of matches in browser
                    } else {
                        getMatchDetailsFromMatchString(mDetails, sText, context, m_feedStatus.isShowingPlayers());
                    }
                }
                if ( mDetails.isDirty() == false ) { return false; }
                JSONObject joModel = null;
                try {
                    joModel = mDetails.getJsonObject(null, null, null);

                    // remove keys that have no value if match is not yet started
                    joModel.remove(JSONKey.when     .toString());
                    joModel.remove(JSONKey.server   .toString());
                    joModel.remove(JSONKey.serveSide.toString());
                    joModel.remove(JSONKey.isHandOut.toString());

                    String sResultShort = (String) joModel.remove(JSONKey.result.toString());

                    AlertDialog.Builder ab = new MyDialogBuilder(context);
                    String sMessage = /*MapUtil.toNiceString(mDetails)*/ joModel.toString(); // TODO: improve
                    ab.setMessage(sMessage)
                      .setIcon   (R.drawable.ic_action_web_site);
                    if ( StringUtil.isNotEmpty(sResultShort) && (sResultShort.equals("0-0") == false) ) {
                        ab.setPositiveButton(android.R.string.cancel, null);
                    } else {
                        ab.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override public void onClick(DialogInterface dialog, int which) {
                                // start the match
                                int iGroupPos = ExpandableListView.getPackedPositionGroup(id);
                                int iChildPos = ExpandableListView.getPackedPositionChild(id);
                                onChildClickListener.onChildClick(expandableListView, view, iGroupPos , iChildPos , id);
                            }
                        });
                        ab.setNegativeButton(android.R.string.cancel, null);
                    }
                    ab.show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            }
        };
    }

    private EMSAdapter emsAdapter;
    @Override public SimpleELAdapter getListAdapter(LayoutInflater inflater) {
        if ( emsAdapter == null ) {
            emsAdapter = new EMSAdapter(inflater, getString(R.string.fetching_data));
        }
        return emsAdapter;
    }

    public enum FeedStatus {
        initializing             (false),
        loadingMatches           (false),
        showingMatches           (true ),
        loadingPlayers           (false),
        showingPlayers           (true ),
        ;
        private boolean bAllowSelectionForMatch = false;
        FeedStatus(boolean bAllowSelectionForMatch) {
            this.bAllowSelectionForMatch = bAllowSelectionForMatch;

        }
        public boolean allowSelectionForMatch() {
            return bAllowSelectionForMatch;
        }
        public boolean isShowingPlayers() {
            return this.toString().contains("Players");
        }
        public boolean isShowingMatches() {
            return this.toString().contains("Matches");
        }
    }
    private FeedStatus m_feedStatus            = FeedStatus.initializing;
    private String     m_sLastFetchedURL       = null;

    public FeedStatus getFeedStatus() {
        return m_feedStatus;
    }
    /** Called when switching feed, or switching from players to matches (or vice versa) */
    public void resetFeedStatus(FeedStatus fsNew) {
        if ( fsNew == null ) {
            if ( m_feedStatus.isShowingPlayers() ) {
                fsNew = FeedStatus.loadingPlayers;
            } else {
                fsNew = FeedStatus.loadingMatches;
            }
        }
        changeAndNotify(fsNew, false);

        m_joFeedConfig = null; // ensure feed config ( if present ) is re-read
        m_sDisplayFormat_Matches = DisplayFormat_MatchDefault;
        m_sDisplayFormat_Players = DisplayFormat_PlayerDefault;
    }

    private URLTask m_task = null; // move this object outside of the EMSAdapter: when activity was started for 2nd time with API 27 it was not cancellable because reference was gone

    private class EMSAdapter extends SimpleELAdapter implements ContentReceiver
    {
        private EMSAdapter(LayoutInflater inflater, String sFetchingMessage)
        {
            super(inflater, R.layout.expandable_match_selector_group, R.layout.expandable_match_selector_item, sFetchingMessage, bAutoLoad);
            m_bGroupByCourt = PreferenceValues.groupMatchesInFeedByCourt(context); // if set to true, matches must be sorted by date+time within the 'section', my feeds usually come in 'per field'
        }

        int     m_iMatchesWithCourt = 0;
        int     m_iMatchesWithOutResultWithCourt = 0;
        /** To keep the number of toast message to a certain minimum */
        int     m_iGroupByCourtMsg  = 0;

        @Override public void clear() {
            super.clear();

            // will be 're-increased' by 'receive()'
            m_iMatchesWithCourt              = 0;
            m_iMatchesWithOutResultWithCourt = 0;
            m_bGroupByCourt                  = PreferenceValues.groupMatchesInFeedByCourt(context);
        }

        @Override public void load(boolean bUseCacheIfPresent) {
            this.clear();

            if ( true || m_bGroupByCourt ) {
                sortHeaders(SortOrder.Ascending);
            }

            if ( context == null ) {
                return;
            }
            String sURLMatches = PreferenceValues.getMatchesFeedURL(context);
            String sURLPlayers = PreferenceValues.getPlayersFeedURL(context);
            if ( m_feedStatus.isShowingPlayers() ) {
                changeAndNotify(FeedStatus.loadingPlayers, false);
                m_sLastFetchedURL = sURLPlayers;
            } else {
                changeAndNotify(FeedStatus.loadingMatches, false);
                m_sLastFetchedURL = sURLMatches;
            }
/*
            if ( bUseCacheIfPresent ) {
                if (m_feedStatus.toString().startsWith(FeedStatus.showingMatches.toString()) && StringUtil.isNotEmpty(sURLPlayers)) {
                    changeAndNotify(FeedStatus.loadingPlayers);
                    m_sLastFetchedURL = sURLPlayers;
                } else {
                    changeAndNotify(FeedStatus.loadingMatches);
                    m_sLastFetchedURL = sURLMatches;
                }
            } else {
                if (m_feedStatus.toString().startsWith(FeedStatus.showingMatches.toString()) || StringUtil.isEmpty(sURLPlayers) ) {
                    changeAndNotify(FeedStatus.loadingMatches);
                    m_sLastFetchedURL = sURLMatches;
                } else {
                    changeAndNotify(FeedStatus.loadingPlayers);
                    m_sLastFetchedURL = sURLPlayers;
                }
            }
*/
            m_sLastFetchedURL = URLFeedTask.prefixWithBaseIfRequired(m_sLastFetchedURL);

            if ( StringUtil.isEmpty(m_sLastFetchedURL) ) {
                super.addItem(getString(R.string.No_active_feed), getString(R.string.Select_one_by_pressing_the_globe_button));
                this.notifyDataSetChanged();

                // TODO: dialog with 'select feed', 'hide this tab forever' and 'cancel'
                return;
            }
            final DialogInterface.OnCancelListener onCancelListener = new DialogInterface.OnCancelListener() {
                @Override public void onCancel(DialogInterface dialog) {
                    emsAdapter.cancel(); // not only called if user cancelled the dialog e.g. by pressing back, but also on hideProgress
                }
            };
            showProgress(StringUtil.capitalize(m_feedStatus), onCancelListener); // TODO: use string array and translate
            super.addItem(m_sFetchingDataMessage, m_sLastFetchedURL);
            FeedMatchSelector.this.onChildClickListener.setDisabled(true);

            this.notifyDataSetChanged();
            if ( StringUtil.isEmpty(m_sLastFetchedURL) ) {
                this.receive(null, FetchResult.UnexpectedContent, 0, null, null);
                return;
            }

            m_task = new URLFeedTask(context, m_sLastFetchedURL);
            if ( bUseCacheIfPresent == false ) {
                m_task.setCacheFileToOld(true);
            }
            int iCacheMaxMinutes = PreferenceValues.getMaxCacheAgeFeeds(context);
            if ( iCacheMaxMinutes >= 0 ) {
                m_task.setMaximumReuseCacheTimeMinutes(iCacheMaxMinutes);
            }

            m_task.setContentReceiver(this);
            if ( Build.VERSION.SDK_INT <= Build.VERSION_CODES.P /* 28 */ ) {
                m_task.executeOnExecutor(Executors.newSingleThreadExecutor());
                Log.d(TAG, "Started download task using Executors.newSingleThreadExecutor... ");
            } else {
                m_task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                Log.d(TAG, "Started download task ... ");
            }
        }

        @Override public void cancel() {
            if ( m_task != null ) {
                Log.d(TAG, "Canceling download task ... ");
                m_task.cancel(true);
                m_task = null;
            } else {
                Log.d(TAG, "No download task to cancel ... ");
            }
        }

        @Override public void receive(String sContent, FetchResult result, long lCacheAge, String sLastSuccessfulContent, String sUrl)
        {
            Log.i(TAG, String.format("Fetched (from cache %s) %s", lCacheAge, result));
            if ( m_task != null ) {
                m_task.cancel(true);
                Log.d(TAG, "Setting m_task to null");
                m_task = null;
            }
            if ( context == null ) {
                // long running fetch and user closed activity ??
                return;
            }

            // remove the 'fetching...' message
            boolean bRemoved = this.removeHeader(m_sFetchingDataMessage);

            if ( StringUtil.hasNonEmpty(sContent, sLastSuccessfulContent) ) {
                switch (m_feedStatus) {
                    case loadingMatches:
                        m_bHideCompletedMatches = PreferenceValues.hideCompletedMatchesFromFeed(context);
                        changeAndNotify(FeedStatus.showingMatches, false);
                        break;
                    case loadingPlayers:
                        changeAndNotify(FeedStatus.showingPlayers, false);
                        break;
                }
            } else {
                this.addItem(m_sNoMatchesInFeed, result.toString());
            }

            // use try/catch here because getResources() may fail if user closed the activity before data was retrieved
            List<String> lExpandedGroups = null;
            String       sUseContent     = null;
            try {
                if ( (sContent == null) || (result.equals(FetchResult.OK) == false) ) {
                    if ( StringUtil.isNotEmpty(sLastSuccessfulContent) ) {

                        if ( result.equals(FetchResult.Cancelled) == false ) {
                            String sMsg = getResources().getString(R.string.Could_not_read_feed_x__y__Using_cached_content_aged_z_minutes, m_sLastFetchedURL, result, DateUtil.convertToMinutes(lCacheAge));
                            DialogManager dialogManager = DialogManager.getInstance();
                            dialogManager.showMessageDialog(context, "Internet", sMsg);
                        }
                        sUseContent = sLastSuccessfulContent;
                    } else {
                        // invalid feed url?
                        String sHeader = getResources().getString(R.string.could_not_load_feed_x, StringUtil.capitalize(result));
                        FeedMatchSelector.this.onChildClickListener.setDisabled(true);
                        if ( StringUtil.isNotEmpty(m_sLastFetchedURL) ) {
                            super.addItem(sHeader, m_sLastFetchedURL);
                            if ( ScoreBoard.isInSpecialMode() ) {
                                ContentUtil.placeOnClipboard(context, "squore feed", m_sLastFetchedURL, "copied feed URL onto clipboard");
                            }
                            if ( FetchResult.UnexpectedContent.equals(result) ) {
                                super.addItem(sHeader, getString(R.string.possible_cause) + getString(R.string.no_fully_functional_connection_error));
                            }
                        } else {
                            super.addItem(sHeader, "No URL defined for '" + PreferenceValues.getFeedPostName(getActivity()) + "' " + m_feedStatus);
                        }
                    }
                } else {
                    sUseContent = sContent;
                }
                if ( sUseContent != null ) {
                    lExpandedGroups = fillList(sUseContent.trim());
                    FeedMatchSelector.this.onChildClickListener.setDisabled( m_feedStatus.allowSelectionForMatch() == false );
                }
            } catch (JSONException e) {
                e.printStackTrace();
                String sName = PreferenceValues.getMatchesFeedName(getActivity());
                this.addItem(sName, e.getMessage());
            } catch (Exception e) {
                // e.g. activity closed by user before data was received
                e.printStackTrace();
            }

            boolean bSuggestToShowPlayerList = false;
            if ( this.getChildrenCount() == 0 ) {

                if ( m_feedStatus.isShowingMatches() ) {
                    super.addItem(m_sNoMatchesInFeed, m_sLastFetchedURL);
                }

                String sName = PreferenceValues.getMatchesFeedName(getActivity());
                if ( StringUtil.isNotEmpty(sName) ) {
                    String sMsg = null;
                    switch ( m_feedStatus ) {
                        case showingMatches:
                            sMsg = m_sNoMatchesInFeed;
                            bSuggestToShowPlayerList = true;
                            if ( m_bHideCompletedMatches ) {
                                sMsg = "Sorry, no uncompleted matches in this feed for the moment";
                            }
                            break;
                        case showingPlayers:
                            sMsg = "Sorry, no players in this feed for the moment";
                            break;
                    }
                    this.addItem(sName, sMsg);
                    FeedMatchSelector.this.onChildClickListener.setDisabled(true);
                }
            }

            // since the data was retrieved with a Async task the expandable list view has already been drawn
            // make sure it is redrawn now that data from the feed has been processed
            notifyDataSetChanged();

            setGuiDefaults(lExpandedGroups);

            hideProgress();

            if ( showTip() == false ) {
                if ( bSuggestToShowPlayerList && StringUtil.isNotEmpty(sUseContent) ) {
                    suggestToShowPlayerList();
                }
            }

            if ( result.equals(FetchResult.OK) && m_feedStatus.isShowingMatches() ) {
                if ( m_bGroupByCourt ) {
                    if ( m_bHideCompletedMatches ) {
                        if ( m_iMatchesWithOutResultWithCourt == 0 ) {
                            if ( m_iGroupByCourtMsg < 3 ) {
                                Toast.makeText(context, "None of the uncompleted matches in this feed has a court specified", Toast.LENGTH_LONG).show();
                                m_iGroupByCourtMsg++;
                            }
                        }
                    } else {
                        if ( m_iMatchesWithCourt == 0 ) {
                            if ( m_iGroupByCourtMsg < 3 ) {
                                Toast.makeText(context, "None of the matches in this feed has a court specified", Toast.LENGTH_LONG).show();
                                m_iGroupByCourtMsg++;
                            }
                        }
                    }
                } else {
                    m_iGroupByCourtMsg = 0;
                }
            }
/*
            if ( showTip() == false ) {
                if ( this.getChildrenCount() == 0 )   {
                    if ( feedStatus.toString().startsWith(FeedStatus.showingMatches.toString() ) )  {
                        String sURLPlayers = PreferenceValues.getPlayersFeedURL(activity);
                        if ( StringUtil.isNotEmpty(sURLPlayers) ) {
                            load(true);
                        }
                    }
                }
            }
*/
        }

        /** For matches or players */
        private List<String> fillList(String sContent) throws Exception {
            List<String> lExpandedGroups = null;
            if ( sContent.startsWith("{") && sContent.endsWith("}"))  {
                sContent = canonicalizeJsonKeys(sContent);
                JSONObject joRoot = new JSONObject(sContent);
                readFeedConfig(joRoot);
                lExpandedGroups = fillListJSON(joRoot);
            } else if ( sContent.startsWith("[") && sContent.endsWith("]") && sContent.contains("{") ) { // TODO: check json validity
                sContent = canonicalizeJsonKeys(sContent);
                JSONArray array = new JSONArray(sContent);
                if ( m_feedStatus.isShowingPlayers() ) {
                    fillPlayerListFromJSONArray(getFormat(m_feedStatus), "All", array);
                } else {
                    fillMatchListFromJSONArray(getFormat(m_feedStatus), "All", lExpandedGroups, array);
                }
            } else {
                lExpandedGroups = fillListFlat(sContent);
            }
            return lExpandedGroups;
        }

        private String getFormat(FeedStatus feedStatus) {
            String sFormat = m_sDisplayFormat_Matches;
            if ( feedStatus.equals(FeedStatus.showingPlayers) ) {
                sFormat = m_sDisplayFormat_Players;
            }
            return sFormat;
        }

        private final AndroidPlaceholder placeholder = new AndroidPlaceholder(TAG);

        /** For matches and players */
        private List<String> fillListJSON(JSONObject joRoot) throws Exception {
            List<String> lExpandedGroups = new ArrayList<>();
            String       sDisplayFormat  = getFormat(m_feedStatus);
            mFeedPrefOverwrites.clear();

            if ( JsonUtil.size(joRoot) >= 1 && joRoot.has(JSONKey.Message.toString()) ) {
                String sMessage = (String) joRoot.remove(JSONKey.Message.toString());
                Toast.makeText(context, sMessage, Toast.LENGTH_LONG).show();
            }

            // for feeds where matches between teams are listed, for each team a list of players may be specified
            m_joTeamPlayers = joRoot.optJSONObject(JSONKey.teamPlayers.toString());

            int    iEntriesCnt             = 0;
            String sActualNameFromFeed     = null;
            String sAuthenticationFromFeed = null;

            Iterator<String> itSections = joRoot.keys(); // Field names and or round names?
            while ( itSections.hasNext() ) {
                String sSection = itSections.next();

                boolean bNameOrConfig = false;
                if ( sSection.equalsIgnoreCase(URLsKeys.name.toString()) ) { // old, do not promote this (name as root key) in documentation or so
                    bNameOrConfig = true;
                    sActualNameFromFeed = joRoot.getString(sSection);
                } else if ( sSection.equalsIgnoreCase(URLsKeys.config.toString()) ) {
                    bNameOrConfig = true;
                    JSONObject joConfig = (JSONObject) joRoot.get(sSection);
                    sActualNameFromFeed     = joConfig.optString(URLsKeys.name.toString());
                    sAuthenticationFromFeed = joConfig.optString(URLsKeys.Authentication.toString());
                }

                if ( StringUtil.isNotEmpty(sActualNameFromFeed) || StringUtil.isNotEmpty(sAuthenticationFromFeed) ) {
                    // read the name of the feed
                    Map<URLsKeys, String> feedPostDetail = PreferenceValues.getFeedPostDetail(context);
                    String sCurrentName           = feedPostDetail.get(URLsKeys.Name);
                    String sCurrentAuthentication = feedPostDetail.get(URLsKeys.Authentication);

                    boolean bFeedPropertiesChanged = false;

                    if ( StringUtil.isNotEmpty(sActualNameFromFeed) ) {
                        if ( sActualNameFromFeed.equals(sCurrentName) == false ) {
                            feedPostDetail.put(URLsKeys.Name, sActualNameFromFeed);
                            bFeedPropertiesChanged = true;
                        }
                    }
                    if ( StringUtil.isNotEmpty(sAuthenticationFromFeed) ) {
                        if ( sAuthenticationFromFeed.equals(sCurrentAuthentication) == false ) {
                            feedPostDetail.put(URLsKeys.Authentication, sAuthenticationFromFeed);
                            bFeedPropertiesChanged = true;
                        }
                    }

                    if ( bFeedPropertiesChanged ) {
                        PreferenceValues.addOrReplaceNewFeedURL(context, feedPostDetail, true, true);

                        if ( getActivity() instanceof MatchTabbed ) {
                            MatchTabbed tabbed = (MatchTabbed) getActivity();
                            tabbed.mAdapter.notifyDataSetChanged();
                        }
                    }
                    sActualNameFromFeed     = null;
                    sAuthenticationFromFeed = null;
                }

                if ( bNameOrConfig ) {
                    continue;
                }

                Object values = joRoot.get(sSection);
                JSONArray entries = null;
                if ( values instanceof JSONArray ) {
                    entries = (JSONArray) values;
                } else if ( values instanceof JSONObject ) {
                    JSONObject joChild = (JSONObject) values;
                    entries = joChild.optJSONArray("Matches"); // not required
                    sSection = joChild.optString("Field", sSection);
                    if ( entries == null ) {
                        Log.w(TAG, String.format("Not using %s with object %s as value", sSection, values));
                    }
                } else {
                    Log.w(TAG, String.format("Not using %s with value %s (%s)", sSection, values, values.getClass().getName()));
                }

                if ( entries != null ) {
                    if ( m_feedStatus.isShowingPlayers() ) {
                        fillPlayerListFromJSONArray(sDisplayFormat, sSection, entries);
                    } else {
                        fillMatchListFromJSONArray(sDisplayFormat, sSection, lExpandedGroups, entries);
                    }
                    iEntriesCnt += entries.length();
                }
            }

            return lExpandedGroups; // TODO: return list of headers that should be expanded
        }

        /** Players only */
        private void fillPlayerListFromJSONArray(final String sDisplayFormat, String sSection, JSONArray players) throws JSONException {
            // TODO: to allow list of strings
            for ( int f=0; f < players.length(); f++ ) {
                JSONObject joPlayer = players.getJSONObject(f);
                String sDisplayName = placeholder.translate(sDisplayFormat, joPlayer);
                       sDisplayName = placeholder.removeUntranslated(sDisplayName);
                       sDisplayName = sDisplayName.replaceAll("[^\\w\\s]{2}", ""); // remove brackets around values that are not provided (), [], <>
                //String sName    = joPlayer.getString(JSONKey.name   .toString());
                //String sCountry = joPlayer.getString(JSONKey.country.toString());
                super.addItem(sSection, sDisplayName, joPlayer);
            }
        }
        /** Matches only */
        private void fillMatchListFromJSONArray(final String sDisplayFormat, String sSection, List<String> lExpandedGroups, JSONArray matches) throws JSONException {
            if ( sSection.matches(HEADER_PREFIX_REGEXP) ) {
                String sPrefix = sSection.replaceAll(HEADER_PREFIX_REGEXP, "$1");
                sSection = sSection.replaceAll(HEADER_PREFIX_REGEXP, "$2").trim();
                if ( sPrefix.equals("+") && (lExpandedGroups.contains(sSection) == false) ) {
                    lExpandedGroups.add(sSection);
                }
            }

            if ( m_bGroupByCourt ) {
                super.sortItems(SortOrder.Ascending); // TODO: Improve. This only sorts on 'display', we want to sort more detailed on/by date/time
            }

            final Model matchModel = ScoreBoard.getMatchModel();
            final boolean bAppModelIsFinished = matchModel != null && matchModel.matchHasEnded();
            final String sLastInApp_Url = matchModel == null ? null : (matchModel.getName(Player.A) + "__" + matchModel.getName(Player.B));
            final String sLastInApp_Id  = matchModel == null ? null :matchModel.getSourceID();

            for ( int f=0; f < matches.length(); f++ ) {
                Object o = matches.get(f);
                if ( o instanceof JSONObject == false ) {
                    Log.w(TAG, String.format("Not a JSONObject match at index %d", f));
                    continue;
                }
                JSONObject joMatch = (JSONObject) o;
                if ( joMatch.has(Player.A.toString()) == false ) {
                    // assume JSON array with the player names ==>   "players" : [ "John", "Peter" ]
                    JSONArray players = joMatch.optJSONArray(JSONKey.players.toString());
                    if ( players != null ) {
                        joMatch.put(Player.A.toString(), players.get(0));
                        joMatch.put(Player.B.toString(), players.get(1));
                    }
                }
                if ( joMatch.has(Player.A.toString()) == false ) {
                    Log.w(TAG, "Skipping match without players");
                    continue;
                }
                String sRoundOrDivisionOrCourt = joMatch.optString(JSONKey.division.toString(), sSection);
                if ( StringUtil.isEmpty(sRoundOrDivisionOrCourt) || sRoundOrDivisionOrCourt.equals(sSection) ) {
                    sRoundOrDivisionOrCourt = joMatch.optString(JSONKey.round.toString(), sSection);
                }
                String sResult = joMatch.optString(JSONKey.result.toString());
                if ( joMatch.has(JSONKey.court.toString() ) ) {
                    m_iMatchesWithCourt++;
                    if ( StringUtil.isEmpty(sResult) ) {
                        m_iMatchesWithOutResultWithCourt++;
                    }
                    if ( m_bGroupByCourt ) {
                        sRoundOrDivisionOrCourt = joMatch.optString(JSONKey.court.toString(), sRoundOrDivisionOrCourt);
                        joMatch.put(m_sIfGroupByCourtUseSectionAs, sSection);
                    }
                }

                String sDisplayName = placeholder.translate(sDisplayFormat, joMatch);
                       sDisplayName = placeholder.removeUntranslated(sDisplayName);
                       sDisplayName = sDisplayName.replaceAll("(\\[\\s*\\]|\\(\\s*\\)|<\\s*>)", ""); // remove brackets around values that are not provided (), [], <>
                       sDisplayName = sDisplayName.replaceAll("[:]\\s*$", "");   // remove splitter character(s) at end (there because certain values not provided)
                       sDisplayName = StringUtil.normalize(sDisplayName).trim();

                switch ( m_feedStatus ) {
                    case showingMatches: {
                        if ( m_bHideCompletedMatches ) {
                            if ( StringUtil.isEmpty(sResult) ) {
                                if ( bAppModelIsFinished ) {
                                    String sFeedId = joMatch.optString(JSONKey.id.toString());
                                    if ( StringUtil.isEmpty(sFeedId) ) {
                                        JSONObject joMeta = joMatch.optJSONObject(JSONKey.metadata.toString());
                                        if ( joMeta != null ) {
                                            sFeedId = joMeta.optString(JSONKey.sourceID.toString());
                                        }
                                    }
                                    if ( StringUtil.isNotEmpty(sFeedId) ) {
                                        if ( sFeedId.equals(sLastInApp_Id ) ) {
                                            continue;
                                        }
                                    } else {
                                        String sFeedMatch_Url = joMatch.optString(Player.A.toString()) + "__" + joMatch.optString(Player.B.toString());
                                        if ( sLastInApp_Url.equals(sFeedMatch_Url ) ) {
                                            continue;
                                        }
                                    }
                                }
                                super.addItem(sRoundOrDivisionOrCourt, sDisplayName, joMatch);
                            }
                        } else {
                            super.addItem(sRoundOrDivisionOrCourt, sDisplayName, joMatch);
                        }
                        break;
                    }
                }
            }
        }

        /** Matches or players */
        private List<String> fillListFlat(String sContent) {
            List<String> lInput = new ArrayList<>(Arrays.asList(sContent.split("\n")));
            ListUtil.removeEmpty(lInput);

            if ( ListUtil.isEmpty(lInput) ) {
                super.addItem(m_sNoMatchesInFeed, m_sLastFetchedURL);
                //super.clearFilter();

                // TODO: ask user if he wants to switch to list of players (only if there actually are players in the feed)
            } else {
                String sFirstLineOfFeed = lInput.get(0).trim();
                if ( sFirstLineOfFeed.startsWith("[") && sFirstLineOfFeed.endsWith("]") ) {
                    // assume the first line is the name of the feed: allowing the name of the feed be dynamically updated
                    String sNewName     = lInput.remove(0).trim().substring(1, sFirstLineOfFeed.length() - 1);
                    Map<URLsKeys, String> feedPostDetail = PreferenceValues.getFeedPostDetail(context);
                    String sCurrentName = feedPostDetail.get(URLsKeys.Name);
                    if ( (sNewName.equals(sCurrentName) == false) && (sNewName.trim().length() > 0) ) {
                        // TODO: see if the feed is in the feeds.php, and if so get additional attributes from there (Region,Country!)

                        feedPostDetail.put(URLsKeys.Name, sNewName);
                        PreferenceValues.addOrReplaceNewFeedURL(context, feedPostDetail, true, true);

                        //emsAdapter.notifyDataSetChanged();
                        //notifyDataSetChanged();
                        if ( getActivity() instanceof MatchTabbed ) {
                            MatchTabbed tabbed = (MatchTabbed) getActivity();
                            tabbed.mAdapter.notifyDataSetChanged();
                        }
                    }
                }
                List<String> lExpanded = new ArrayList<>();
                String sHeader = getString(m_feedStatus.equals(FeedStatus.showingPlayers)?R.string.lbl_players:R.string.sb_matches); // default if no header follows in the feed
                mFeedPrefOverwrites.clear();
                Model mTmp = ModelFactory.getTemp("Storing parsing-result of a match from a feed");
                for ( String sEntry : lInput ) {
                    sEntry = sEntry.trim();
                    if ( StringUtil.isEmpty(sEntry) ) {
                        continue;
                    }

                    if ( sEntry.matches(SETTING_REGEXP) ) {
                        Pattern p = Pattern.compile(SETTING_REGEXP);
                        Matcher m = p.matcher(sEntry);
                        while ( m.find() ) {
                            String sKey   = m.group(1);
                            String sValue = m.group(2);
                            try {
                                // allow some lines to actually specify settings. Like game is played until 15 (i.s.o default of 11)
/*
[shareAction=PostResult]
[captionForPostMatchResultToSite=Post uitslag naar DY Boxen site]
[postDataPreference=BasicWithJsonDetails]
[numberOfPointsToWinGame=15]
[locationLast=IHAM, Mechelen]
*/
                                PreferenceKeys key = PreferenceKeys.valueOf(sKey);
                                mFeedPrefOverwrites.put(key, sValue);
                            } catch (Exception e) {
                                //e.printStackTrace();
                                try {
                                    // allow some lines to actually specify settings. Like fixed URL (psa/1234/matches) in stead of dynamic URL (psa.php?nr=1)
/*
[FeedMatches=psa/.../matches]
[FeedPlayers=tournamentsoftware/.../players]
*/
                                    URLsKeys urLsKeys = URLsKeys.valueOf(sKey);
                                    Map<URLsKeys, String> feedPostDetail = PreferenceValues.getFeedPostDetail(context);
                                    String sCurrentVal = feedPostDetail.get(urLsKeys);
                                    if ( (sValue.equals(sCurrentVal) == false) && (sValue.trim().length() > 0) ) {
                                        feedPostDetail.put(urLsKeys, sValue);
                                        PreferenceValues.addOrReplaceNewFeedURL(context, feedPostDetail, true, true);
                                    }
                                } catch (Exception e2) {
                                    e2.printStackTrace();
                                }
                            }
                        }

                        continue;
                    }

                    // sometime special characters are encoded as unicode in the feed: decode them
                    String sEntryUCD = Placeholder.Misc.UnicodeDecode.execute(sEntry, null, null);
                    if ( sEntryUCD.equals(sEntry) == false ) {
                        sEntry = sEntryUCD;
                    }

                    if ( sEntry.matches(HEADER_PREFIX_REGEXP) ) {
                        String sPrefix = sEntry.replaceAll(HEADER_PREFIX_REGEXP, "$1");
                        sHeader = sEntry.replaceAll(HEADER_PREFIX_REGEXP, "$2").trim();
                        if ( sPrefix.equals("+") && (lExpanded.contains(sHeader) == false) ) {
                            lExpanded.add(sHeader);
                        }
                        continue;
                    }
                    switch ( m_feedStatus ) {
                        case showingPlayers: {
                            super.addItem(sHeader, sEntry);
                            break;
                        }
                        case showingMatches: {
                            if ( m_bHideCompletedMatches ) {
                                String sResult = getMatchDetailsFromMatchString(mTmp, sEntry, context, false);
                                if ( StringUtil.isEmpty(sResult) ) {
                                    super.addItem(sHeader, sEntry);
                                }
                            } else {
                                super.addItem(sHeader, sEntry);
                            }
                            break;
                        }
                    }
                }
                return lExpanded;
            }
            return null;
        }
    }

    public static String canonicalizeJsonKeys(String sContent) {
        // allow the feed to specify keys in CamelCase, we will be using camelcase with first letter lowercase
        String sContentFixed = sContent
                .replaceAll("\"Division\"", JSONKey.division.toString())
                .replaceAll("\"Field\""   , JSONKey.field   .toString())
                .replaceAll("\"Round\""   , JSONKey.round   .toString())
                .replaceAll("\"Location\"", JSONKey.location.toString())
                .replaceAll("\"Players\"" , JSONKey.players .toString())
                .replaceAll("\"Court\""   , JSONKey.court   .toString())
                .replaceAll("\"Date\""    , JSONKey.date    .toString())
                .replaceAll("\"Time\""    , JSONKey.time    .toString())
                .replaceAll("\"ID\""      , JSONKey.id      .toString())
                .replaceAll("\"Id\""      , JSONKey.id      .toString())
                .replaceAll("\"Name\""    , JSONKey.name    .toString())
                .replaceAll("\"Club\""    , JSONKey.club    .toString())
                .replaceAll("\"Country\"" , JSONKey.country .toString())
                //translate old (legacy) keys (all lower and/or with underscores) to new more consistent camelCase ones
                .replaceAll("\"team_players\"", JSONKey.teamPlayers.toString())
                .replaceAll("\"teamid\""      , JSONKey.teamId.toString())
        ;
        if ( sContentFixed.equals(sContent) == false ) {
            return sContentFixed;
        }
        return sContent;
    }

    private void suggestToShowPlayerList() {
        if ( (activity instanceof MatchTabbed) == false) { return; }
        final MatchTabbed mt = (MatchTabbed) activity;

        String sURLPlayers = PreferenceValues.getPlayersFeedURL(context);
        if ( StringUtil.isEmpty(sURLPlayers) ) { return; }

        Feature fShowPlayerList = PreferenceValues.switchToPlayerListIfMatchListOfFeedIsEmpty(context);
        switch (fShowPlayerList) {
            case DoNotUse:
                break;
            case Suggest:
                AlertDialog.Builder ab = new MyDialogBuilder(context);
                ab.setMessage(m_sNoMatchesInFeed + "\n" + getString(R.string.uc_show) + " " + getString(R.string.pref_playerList) + "?");
                ab.setIcon(R.drawable.ic_menu_cc);
                ab.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        mt.handleMenuItem(R.id.show_players_from_feed);
                    }
                });
                ab.setNegativeButton(android.R.string.cancel, null);
                ab.show();
                break;
            case Automatic:
                String sMsg = m_sNoMatchesInFeed + "\n" + getString(R.string.uc_showing_x_elipses, getString(R.string.pref_playerList));
                Toast.makeText(context, sMsg, Toast.LENGTH_LONG).show();
                mt.handleMenuItem(R.id.show_players_from_feed);
                break;
        }
    }

    private boolean showTip() {
        if ( PreferenceValues.getMatchesFeedURLUnchanged() ) {
            String sIn1 = context.getString(R.string.pref_web_integration);
            String sIn2 = context.getString(R.string.settings);
            PreferenceValues.showTip(context, PreferenceKeys.feedPostUrl, context.getString(R.string.pref_feedPostUrl_not_set, sIn1, sIn2), false);
            return true;
        }
        return false;
    }

    public interface FeedStatusChangedListerer {
        void notify(FeedStatus fsOld, FeedStatus fsNew, boolean bUpdateCheckableMenuItems);
    }
    private List<FeedStatusChangedListerer> lChangeListeners = new ArrayList<>();
    public void registerFeedChangeListener(FeedStatusChangedListerer l) {
        lChangeListeners.add(l);
    }
    private void changeAndNotify(FeedStatus fsNew, boolean bUpdateCheckableMenuItems) {
        FeedStatus fsOld = m_feedStatus;
        m_feedStatus = fsNew;
        for ( FeedStatusChangedListerer l: lChangeListeners ) {
            l.notify(fsOld, m_feedStatus, bUpdateCheckableMenuItems);
        }
    }
}
