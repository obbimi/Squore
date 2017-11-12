package com.doubleyellow.scoreboard.feed;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.doubleyellow.android.util.AndroidPlaceholder;
import com.doubleyellow.android.util.ContentUtil;
import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.URLFeedTask;
import com.doubleyellow.scoreboard.dialog.SelectFeed;
import com.doubleyellow.scoreboard.main.DialogManager;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.match.ExpandableMatchSelector;
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

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Activity that allows the user to select a match from an internet feed like defined in
 * feedPostUrls/feedPostUrl preferences.
 *
 * Used within MatchTabbed activity.
 */
public class FeedMatchSelector extends ExpandableMatchSelector
{
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

        SETTING_REGEXP = "\\[" + sbAllOpsAsRegExp.toString() + "\\s*=\\s*([^\\]]+)\\]";
    }
    private static final String HEADER_PREFIX_REGEXP = "^\\s*([+-])\\s*(.+)$";

    public static Map<PreferenceKeys, String> mFeedPrefOverwrites = new HashMap<PreferenceKeys, String>();

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

    private ChildClickListener  onChildClickListener = new ChildClickListener();

    private class ChildClickListener implements ExpandableListView.OnChildClickListener {
        @Override public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
            if ( bDisabled ){
                return false;
            }
            Model model = Brand.getModel();
            model.setNrOfPointsToWinGame(Model.UNDEFINED_VALUE); // use to communicate that format still needs to be defined

            SimpleELAdapter listAdapter  = getListAdapter(null);
            String          sGroup       = (String) listAdapter.getGroup(groupPosition);
            Object          oMatch       = emsAdapter.getObject(groupPosition, childPosition);
            String          feedPostName = PreferenceValues.getFeedPostName(context);
            if ( oMatch instanceof JSONObject ) {
                JSONObject joMatch = (JSONObject) oMatch;
                populateModelFromJSON(model, joMatch, sGroup, feedPostName);
            } else {
                String sText = SimpleELAdapter.getText(v);
                if ( populateModelFromString(model, sText, sGroup, feedPostName) ) {
                    return false;
                }
            }

            Intent intent = new Intent();
            intent.putExtra(Model.class.getSimpleName(), model.toJsonString(null)); // this is read by ScoreBoard.onActivityResult
            activity.setResult(Activity.RESULT_OK, intent);
            activity.finish();
            return true;
        }
        private boolean bDisabled = false;
        void setDisabled(boolean b) {
            this.bDisabled = b;
        }
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
                if ( StringUtil.isEmpty(CountryUtil.getIso2(sCountry1)) && StringUtil.isNotEmpty(CountryUtil.getIso2(sClub1))) {
                    // swap country and club
                    String sTmp = sClub1; sClub1 = sCountry1; sCountry1 = sTmp;
                }
                model.setPlayerName   (Player.A, sPlayer1.trim() );
                model.setPlayerCountry(Player.A, sCountry1 ); // can be a club abbreviation too
                model.setPlayerClub   (Player.A, sClub1 ); // can be a country abbreviation too
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
                model.setPlayerCountry(Player.A, sCountry1 ); // can be a club abbreviation too
                model.setPlayerCountry(Player.B, sCountry2 ); // can be a club abbreviation too
                model.setPlayerClub   (Player.A, sClub1 ); // can be a country abbreviation too
                model.setPlayerClub   (Player.B, sClub2 ); // can be a country abbreviation too
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
        return null;
    }

    private boolean populateModelFromString(Model model, String sText, String sGroup, String feedPostName) {
        Map<URLsKeys, String> feedPostDetail = PreferenceValues.getFeedPostDetail(context);

        boolean bIsOnePlayerOnly = feedStatus == null ? false : feedStatus.isShowingPlayers();
        getMatchDetailsFromMatchString( model, sText, context, bIsOnePlayerOnly);
        model.setSource(feedPostDetail.get(URLsKeys.FeedMatches));

        // use feed name and group name for event details
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
        String sEventDivision = null;
        String sEventRound    = null;
        if ( StringUtil.isNotEmpty(sGroup) && (appearsToBeADate(sGroup) == false) ) {
            // make educated guess
            if( appearsToBeARound(sGroup) ) {
                sEventRound = sGroup;
            } else {
                sEventDivision = sGroup;
            }
        }
        model.setEvent(sEventName, sEventDivision, sEventRound, sLocation);
        return false;
    }

    private void populateModelFromJSON(Model model, JSONObject joMatch, String sGroup, String feedPostName) {
        try {
            model.setPlayerName (Player.A, joMatch.getString(Player.A.toString()));
            model.setPlayerName (Player.B, joMatch.getString(Player.B.toString()));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // use feed name and group name for event details
        model.setEvent ( joMatch.optString(JSONKey.event   .toString(), feedPostName)
                       , joMatch.optString(JSONKey.division.toString(), sGroup)
                       , null, null);

    }

    @Override public AdapterView.OnItemLongClickListener getOnItemLongClickListener() {
        return new AdapterView.OnItemLongClickListener() {
            @Override public boolean onItemLongClick(AdapterView<?> adapterView, final View view, int i, final long l) {
                Object itemAtPosition = adapterView.getItemAtPosition(i);
                if ( feedStatus.equals(FeedStatus.showingPlayers) ) {
                    SimpleELAdapter listAdapter = getListAdapter(null);
                    if ( listAdapter != null ) {
                        List<String> lChilds = listAdapter.getChilds((String) itemAtPosition);
                        if ( ListUtil.isNotEmpty(lChilds) ) {
                            ScoreBoard.dialogWithOkOnly(context, String.format("%s : %d", itemAtPosition, ListUtil.size(lChilds)));
                        }
                    }
                    return false;
                }
                if (itemAtPosition instanceof JSONObject) {
                    // TODO:
                } else if (itemAtPosition instanceof String) {
                    Model mDetails = Brand.getModel();
                    getMatchDetailsFromMatchString(mDetails, (String) itemAtPosition, context, feedStatus.isShowingPlayers());
                    if ( mDetails.isDirty() == false ) { return false; }

                    AlertDialog.Builder ab = ScoreBoard.getAlertDialogBuilder(context);
                    String sMessage = /*MapUtil.toNiceString(mDetails)*/ mDetails.toJsonString(null); // TODO: improve
                    ab.setMessage(sMessage)
                      .setIcon(R.drawable.ic_action_web_site);
                    String sResultShort = mDetails.getResult();
                    if ( StringUtil.isNotEmpty(sResultShort) && (sResultShort.equals("0-0") == false) ) {
                        ab.setPositiveButton(android.R.string.cancel, null);
                    } else {
                        ab.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override public void onClick(DialogInterface dialog, int which) {
                                // start the match
                                onChildClickListener.onChildClick(expandableListView, view, -1 , -1 , l);
                            }
                        });
                        ab.setNegativeButton(android.R.string.cancel, null);
                    }
                    ab.show();
                    return true;
                }
                return false;
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
        showingMatchesUncompleted(true ),
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
    private FeedStatus feedStatus = FeedStatus.initializing;
    private String     sLastFetchedURL = null;

    public FeedStatus getFeedStatus() {
        return feedStatus;
    }
    public void resetFeedStatus() {
        changeAndNotify(FeedStatus.initializing);
    }

    private class EMSAdapter extends SimpleELAdapter implements ContentReceiver
    {
        private EMSAdapter(LayoutInflater inflater, String sFetchingMessage)
        {
            super(inflater, R.layout.expandable_match_selector_group, R.layout.expandable_match_selector_item, sFetchingMessage, bAutoLoad);
        }

        URLTask task = null;

        public void load(boolean bUseCacheIfPresent) {
            this.clear();

            if ( context == null ) {
                return;
            }
            String sURLMatches = PreferenceValues.getMatchesFeedURL(context);
            String sURLPlayers = PreferenceValues.getPlayersFeedURL(context);
            if ( bUseCacheIfPresent ) {
                if (feedStatus.toString().startsWith(FeedStatus.showingMatches.toString()) && StringUtil.isNotEmpty(sURLPlayers)) {
                    changeAndNotify(FeedStatus.loadingPlayers);
                    sLastFetchedURL = sURLPlayers;
                } else {
                    changeAndNotify(FeedStatus.loadingMatches);
                    sLastFetchedURL = sURLMatches;
                }
            } else {
                if (feedStatus.toString().startsWith(FeedStatus.showingMatches.toString()) || StringUtil.isEmpty(sURLPlayers) ) {
                    changeAndNotify(FeedStatus.loadingMatches);
                    sLastFetchedURL = sURLMatches;
                } else {
                    changeAndNotify(FeedStatus.loadingPlayers);
                    sLastFetchedURL = sURLPlayers;
                }
            }
            sLastFetchedURL = URLFeedTask.prefixWithBaseIfRequired(sLastFetchedURL);

            if ( StringUtil.isEmpty(sLastFetchedURL) ) {
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
            showProgress(StringUtil.capitalize(feedStatus), onCancelListener); // TODO: use string array and translate
            super.addItem(this.sFetchingDataMessage, sLastFetchedURL);
            FeedMatchSelector.this.onChildClickListener.setDisabled(true);

            this.notifyDataSetChanged();
            if ( StringUtil.isEmpty(sLastFetchedURL) ) {
                this.receive(null, FetchResult.UnexpectedContent, 0, null);
                return;
            }

            task = new URLFeedTask(context, sLastFetchedURL);
            if ( bUseCacheIfPresent == false ) {
                task.setCacheFileToOld(true);
            }

            task.setContentReceiver(this);
            task.execute();
        }

        public void cancel() {
            if ( task != null ) {
                task.cancel(false);
                task = null;
            }
        }

        @Override public void receive(String sContent, FetchResult result, long lCacheAge, String sLastSuccessfulContent)
        {
            Log.i(TAG, String.format("Fetched (from cache %s)", lCacheAge));
            if ( task != null ) {
                task = null;
            }
            if ( context == null ) {
                // long running fetch and user closed activity ??
                return;
            }

            // remove the 'fetching...' message
            this.removeHeader(this.sFetchingDataMessage);

            if ( StringUtil.hasNonEmpty(sContent, sLastSuccessfulContent) ) {
                switch (feedStatus) {
                    case loadingMatches:
                        if ( PreferenceValues.hideCompletedMatchesFromFeed(context) ) {
                            changeAndNotify(FeedStatus.showingMatchesUncompleted);
                        } else {
                            changeAndNotify(FeedStatus.showingMatches);
                        }
                        break;
                    case loadingPlayers:
                        changeAndNotify(FeedStatus.showingPlayers);
                        break;
                }
            } else {
                this.addItem(getString(R.string.No_matches_in_feed), result.toString());
            }

            // use try/catch here because getResources() may fail if user closed the activity before data was retrieved
            List<String> lExpanded   = null;
            String       sUseContent = null;
            try {
                if ( (sContent == null) || (result.equals(FetchResult.OK) == false)) {
                    if ( StringUtil.isNotEmpty(sLastSuccessfulContent) ) {

                        String sMsg = getResources().getString(R.string.Could_not_read_feed_x__y__Using_cached_content_aged_z_minutes, sLastFetchedURL, result, DateUtil.convertToMinutes(lCacheAge));
                        DialogManager dialogManager = DialogManager.getInstance();
                        dialogManager.showMessageDialog(context, "Internet", sMsg);

                        sUseContent = sLastSuccessfulContent;
                    } else {
                        // invalid feed url?
                        String sHeader = getResources().getString(R.string.could_not_load_feed_x, StringUtil.capitalize(result));
                        FeedMatchSelector.this.onChildClickListener.setDisabled(true);
                        if ( StringUtil.isNotEmpty(sLastFetchedURL) ) {
                            super.addItem(sHeader, sLastFetchedURL);
                            if ( ScoreBoard.isInSpecialMode() ) {
                                ContentUtil.placeOnClipboard(context, "squore feed", sLastFetchedURL, "copied feed URL onto clipboard");
                            }
                            if ( FetchResult.UnexpectedContent.equals(result) ) {
                                super.addItem(sHeader, getString(R.string.possible_cause) + getString(R.string.no_fully_functional_connection_error));
                            }
                        } else {
                            super.addItem(sHeader, "No URL defined for '" + PreferenceValues.getFeedPostName(getActivity()) + "' " + feedStatus);
                        }
                    }
                } else {
                    sUseContent = sContent;
                }
                lExpanded = fillList(sUseContent);
                FeedMatchSelector.this.onChildClickListener.setDisabled( feedStatus.allowSelectionForMatch() == false );
            } catch (Exception e) {
                // activity closed before data was received
            }

            boolean bSuggestToShowPlayerList = false;
            if ( this.getChildrenCount() == 0 ) {
                String sName = PreferenceValues.getMatchesFeedName(getActivity());
                if ( StringUtil.isNotEmpty(sName) ) {
                    String sMsg = null;
                    switch (feedStatus ) {
                        case showingMatches:
                            sMsg = getString(R.string.No_matches_in_feed);
                            bSuggestToShowPlayerList = true;
                            break;
                        case showingMatchesUncompleted:
                            sMsg = "Sorry, no uncompleted matches in this feed for the moment";
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

            setGuiDefaults(lExpanded);

            hideProgress();

            if ( showTip() == false ) {
                if ( bSuggestToShowPlayerList && StringUtil.isNotEmpty(sUseContent) ) {
                    suggestToShowPlayerList();
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

        private List<String> fillList(String sContent) throws Exception {
            if ( sContent.startsWith("{") && sContent.endsWith("}")) {
                return fillListJSON(getFormat(), sContent);
            } else if ( sContent.startsWith("[") && sContent.endsWith("]")) {
                return fillListFromJSONArray(getFormat(), "All", new JSONArray(sContent));
            } else {
                return fillListFlat(sContent);
            }
        }

        private String getFormat() {
            String sFormat = m_sDisplayFormat;
            if ( feedStatus.equals(FeedStatus.showingPlayers) ) {
                sFormat = "${Name}";
            }
            return sFormat;
        }

        final AndroidPlaceholder placeholder = new AndroidPlaceholder(TAG);
        final String m_sDisplayFormat = "${" + JSONKey.date + "} ${" + JSONKey.time + "} : ${" + Player.A + "} - ${" + Player.B + "} : ${" + JSONKey.result + "}";

        private List<String> fillListJSON(String sFormat, String sContent) throws Exception {
            JSONObject joRoot = new JSONObject(sContent);

            // if there is a config section, use it
            //JSONObject joConfig = joRoot.optJSONObject(FeedKeys.FeedMetaData.toString());

            int iEntriesCnt = 0;
            Iterator<String> itSections = joRoot.keys(); // Field names and or round names?
            while(itSections.hasNext()) {
                String sSection = itSections.next();
                if ( sSection.equals(FeedKeys.FeedMetaData.toString())) { continue; }

                JSONArray entries = joRoot.getJSONArray(sSection);
                fillListFromJSONArray(sFormat, sSection, entries);
                iEntriesCnt += entries.length();
            }

            if ( iEntriesCnt == 0 ) {
                super.addItem(getString(R.string.No_matches_in_feed), sLastFetchedURL);

                // TODO: ask user if he wants to switch to list of players (only if there actually are players in the feed)
            }
            return null; // TODO: return list of headers that should be expanded
        }

        private List<String> fillListFromJSONArray(String sDisplayFormat, String sSection, JSONArray matches) throws JSONException {
            for(int f=0; f < matches.length(); f++) {

                JSONObject joMatch = matches.getJSONObject(f);
                String sRoundOrDivision = joMatch.optString(JSONKey.division.toString(), sSection);
                if ( StringUtil.isEmpty(sRoundOrDivision) ) {
                    sRoundOrDivision = joMatch.optString(JSONKey.round.toString(), sSection);
                }

                // TODO: filter out those that already have a result

                String sDisplayName = placeholder.translate(sDisplayFormat, joMatch);
                       sDisplayName = placeholder.removeUntranslated(sDisplayName);
                super.addItem(sRoundOrDivision, sDisplayName, joMatch);
            }
            return null;
        }

        private List<String> fillListFlat(String sContent) {
            List<String> lInput = new ArrayList<String>(Arrays.asList(sContent.split("\n")));
            ListUtil.removeEmpty(lInput);

            if ( ListUtil.isEmpty(lInput) ) {
                super.addItem(getString(R.string.No_matches_in_feed), sLastFetchedURL);
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
                List<String> lExpanded = new ArrayList<String>();
                String sHeader = getString(feedStatus.equals(FeedStatus.showingPlayers)?R.string.lbl_players:R.string.sb_matches); // default if no header follows in the feed
                mFeedPrefOverwrites.clear();
                Model mTmp = ModelFactory.getTemp();
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
                    switch (feedStatus) {
                        case showingPlayers: {
                            super.addItem(sHeader, sEntry);
                            break;
                        }
                        case showingMatches: {
/*
                            if ( lExpanded.contains(sHeader) == false ) {
                                Map<MatchDetails, String> matchDetailsFromMatchString = getMatchDetailsFromMatchString(sEntry);
                                if ( matchDetailsFromMatchString.containsKey(MatchDetails.Result) == false ) {
                                    lExpanded.add(sHeader);
                                }
                            }
*/
                            super.addItem(sHeader, sEntry);
                            break;
                        }
                        case showingMatchesUncompleted: {
                            String sResult = getMatchDetailsFromMatchString(mTmp, sEntry, context, false);
                            if ( StringUtil.isEmpty(sResult) ) {
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

    private void suggestToShowPlayerList() {
        if ( activity instanceof MatchTabbed == false) { return; }
        final MatchTabbed mt = (MatchTabbed) activity;

        String sURLPlayers = PreferenceValues.getPlayersFeedURL(context);
        if ( StringUtil.isEmpty(sURLPlayers) ) { return; }

        Feature fShowPlayerList = PreferenceValues.switchToPlayerListIfMatchListOfFeedIsEmpty(context);
        switch (fShowPlayerList) {
            case DoNotUse:
                break;
            case Suggest:
                AlertDialog.Builder ab = ScoreBoard.getAlertDialogBuilder(context);
                ab.setMessage(getString(R.string.No_matches_in_feed) + "\n" + getString(R.string.uc_show) + " " + getString(R.string.pref_playerList) + "?");
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
                String sMsg = getString(R.string.No_matches_in_feed) + "\n" + getString(R.string.uc_showing_x_elipses, getString(R.string.pref_playerList));
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
        void notify(FeedStatus fsOld, FeedStatus fsNew);
    }
    private List<FeedStatusChangedListerer> lChangeListeners = new ArrayList<FeedStatusChangedListerer>();
    public void registerFeedChangeListener(FeedStatusChangedListerer l) {
        lChangeListeners.add(l);
    }
    private void changeAndNotify(FeedStatus fsNew) {
        FeedStatus fsOld = this.feedStatus;
        this.feedStatus = fsNew;
        for(FeedStatusChangedListerer l: lChangeListeners) {
            l.notify(fsOld, this.feedStatus);
        }
    }
}