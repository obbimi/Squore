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

package com.doubleyellow.scoreboard;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import com.doubleyellow.android.util.ContentUtil;
import com.doubleyellow.scoreboard.prefs.AnnouncementLanguage;
import com.doubleyellow.scoreboard.prefs.PreferenceKeys;
import com.doubleyellow.scoreboard.prefs.StartupAction;
import com.doubleyellow.scoreboard.prefs.URLsKeys;
import com.doubleyellow.util.ListUtil;
import com.doubleyellow.util.StringUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class that is included in all apps that want to invoke the scoreboard themselves.
 * For now these are
 * - Double Yellow app
 * - SQMS app
 **/
public class Glue
{
    private             String m_sInstallMessage = "Please install Squore Squash Score app first.";
    public static final String COM_DOUBLEYELLOW_SCOREBOARD = "com.doubleyellow.scoreboard";

    private Context m_context = null;
    public Glue(Context context, String sMsg) {
        this.m_context = context;
        if ( sMsg != null ) {
            m_sInstallMessage = sMsg;
        }
    }

    /** Invoked after select e.g. 4 players from two teams of two different clubs by the SQMS app */
    public boolean startSquoreBoardForOfficialMatches( String       sDescription
                                                     , List<String> sPlayersTeam1
                                                     , List<String> sPlayersTeam2
                                                     , String       sSplitter) {
        StringBuilder sbMatches = new StringBuilder();
        sbMatches.append(sSplitter);
        sbMatches.append("-").append(sDescription);
        sbMatches.append(sSplitter);
        for (int i = 0; i < sPlayersTeam1.size(); i++) {
            if ( i >= sPlayersTeam2.size() ) { continue; }
            sbMatches.append(sPlayersTeam1.get(i) + " - " + sPlayersTeam2.get(i));
            sbMatches.append(sSplitter);
        }

        return startSquoreBoardForOfficialMatches(sbMatches.toString());
    }

    public boolean startSquoreBoardForOfficialMatches(String sMatches) {
        Map<PreferenceKeys, String> extraInfo = new HashMap<PreferenceKeys, String>();

        extraInfo.put(PreferenceKeys.matchList, sMatches);

        extraInfo.put(PreferenceKeys.StartupAction, StartupAction.StartNewMatch.toString());
        extraInfo.put(PreferenceKeys.MatchTabbed_defaultTab, (/*com.doubleyellow.scoreboard.match.MatchTabbed.SelectTab.Feed.toString()*/"Feed"));

        extraInfo.put(PreferenceKeys.numberOfPointsToWinGame , String.valueOf(11) );
        extraInfo.put(PreferenceKeys.numberOfGamesToWinMatch , String.valueOf(3)  );
        extraInfo.put(PreferenceKeys.useHandInHandOutScoring , "false");
        extraInfo.put(PreferenceKeys.tieBreakFormat          , "TwoClearPoints");
        //extraInfo.put(PreferenceKeys.showTimersAutomatically , "true");
        extraInfo.put(PreferenceKeys.showLastGameInfoInTimer, "true");
        //extraInfo.put(PreferenceKeys.showOfficalAnnouncements, "true");
        //extraInfo.put(PreferenceKeys.showTieBreakDialog      , "true");
        //extraInfo.put(PreferenceKeys.showWhoServesDialog     , "true");

        return _startScoreBoard(extraInfo);
    }

    public boolean startSquoreBoardForOfficialMatches(List<String> sFeedKeyValue, String sRefereeName) {
        Map<PreferenceKeys, String> extraInfo = new HashMap<PreferenceKeys, String>();

        StringBuilder sb = new StringBuilder();
        for(int i=0; i < sFeedKeyValue.size(); i+=2) {
            sb.append(sFeedKeyValue.get(i) + "=" + sFeedKeyValue.get(i+1) + "\n");
        }
        extraInfo.put(PreferenceKeys.feedPostUrls            , sb.toString());

        extraInfo.put(PreferenceKeys.StartupAction           , StartupAction.StartNewMatch.toString());
        extraInfo.put(PreferenceKeys.MatchTabbed_defaultTab  , (/*com.doubleyellow.scoreboard.match.MatchTabbed.SelectTab.Feed.toString()*/"Feed"));

        extraInfo.put(PreferenceKeys.numberOfPointsToWinGame , String.valueOf(11) );
        extraInfo.put(PreferenceKeys.numberOfGamesToWinMatch , String.valueOf(3)  );
        extraInfo.put(PreferenceKeys.useHandInHandOutScoring , "false");
        extraInfo.put(PreferenceKeys.tieBreakFormat          , "TwoClearPoints");
        //extraInfo.put(PreferenceKeys.showTimersAutomatically , "true");
        extraInfo.put(PreferenceKeys.showLastGameInfoInTimer, "true");
        //extraInfo.put(PreferenceKeys.showOfficalAnnouncements, "true");
        //extraInfo.put(PreferenceKeys.showTieBreakDialog      , "true");
        //extraInfo.put(PreferenceKeys.showWhoServesDialog     , "true");
        if ( StringUtil.isNotEmpty(sRefereeName) ) {
            extraInfo.put(PreferenceKeys.refereeName, sRefereeName);
        }

        return _startScoreBoard(extraInfo);
    }

    public boolean startSquoreBoard() {
        Map<PreferenceKeys, String> extraInfo = new HashMap<PreferenceKeys, String>();
        return _startScoreBoard(extraInfo);
    }

    /** Eg. invoked from the 'Double Yellow' app. */
    public boolean startSquoreBoard( String  sFeedName
                                   , String  sFeedMatchesURL
                                   , String  sPostMatchResultURL
                                   , int     iNumberOfPointsToWinGame // e.g 15 in stead of the official 11
                                   , int     iNumberOfGamesToWinMatch
                                   , boolean bUseHandInHandOutScoring
                                   , AnnouncementLanguage announcementLanguage
                                   , StartupAction startupAction) {
        return startSquoreBoard(sFeedName
                , sFeedMatchesURL
                , null
                , sPostMatchResultURL
                , "BasicWithJsonDetails" // com.doubleyellow.scoreboard.prefs.PostDataPreference.BasicWithJsonDetails
                , null
                , null
                , null
                , iNumberOfPointsToWinGame
                , iNumberOfGamesToWinMatch
                , bUseHandInHandOutScoring
                , null
                , null
                , null
                , null
                , null
                , announcementLanguage
                , startupAction);
    }
    /** Eg. invoked from the 'Double Yellow' app. */
    public boolean startSquoreBoard( String  sFeedName
                                   , String  sFeedMatchesURL
                                   , String  sFeedPlayersUrl
                                   , String  sPostMatchResultURL
                                   , String  sPostData
                                   , String  sUserKey
                                   , String  sUserName
                                   , String  sMatchLocation
                                   , int     iNumberOfPointsToWinGame // e.g 15 in stead of the official 11
                                   , int     iNumberOfGamesToWinMatch
                                   , Boolean bUseHandInHandOutScoring
                                   , String  sCaptionForEmailMatchResult
                                   , String  sCaptionForMessageMatchResult
                                   , String  sCaptionForPostMatchResultToSite
                                   , String  sMessageToNr            // default telephone number to text the result of a message to
                                   , String  sEmailTo
                                   , AnnouncementLanguage announcementLanguage
                                   , StartupAction startupAction) {
        Map<PreferenceKeys, String> extraInfo = new HashMap<PreferenceKeys, String>();

        // Feed
        String sUrls =
                     URLsKeys.Name        + "=" + sFeedName           + "\n"
                   + URLsKeys.FeedMatches + "=" + sFeedMatchesURL     + "\n";
        if ( sFeedPlayersUrl != null ) {
            sUrls += URLsKeys.FeedPlayers + "=" + sFeedPlayersUrl     + "\n";
        }
        if (sPostMatchResultURL != null) {
            sUrls += URLsKeys.PostResult  + "=" + sPostMatchResultURL + "\n";
            if (sPostData != null) {
                sUrls +=
                     URLsKeys.PostData    + "=" + sPostData + "\n";
            }
        }
        extraInfo.put(PreferenceKeys.feedPostUrls, sUrls);
        if (sUserName != null && sUserName.trim().length() != 0) {
            extraInfo.put(PreferenceKeys.additionalPostKeyValuePairs, sUserKey + "=" + sUserName);
        }

        extraInfo.put(PreferenceKeys.StartupAction, startupAction.toString());
        if ( announcementLanguage != null ) {
            extraInfo.put(PreferenceKeys.officialAnnouncementsLanguage, announcementLanguage.toString());
        }
        if (startupAction.equals(StartupAction.StartNewMatch)) {
            extraInfo.put(PreferenceKeys.MatchTabbed_defaultTab, "Feed");
        }

        // match format (TODO: different for boxen matches and IC matches)
        if ( iNumberOfPointsToWinGame > 0 ) {
            extraInfo.put(PreferenceKeys.numberOfPointsToWinGame, "" + iNumberOfPointsToWinGame);
        }
        if( iNumberOfGamesToWinMatch > 0 ) {
            extraInfo.put(PreferenceKeys.numberOfGamesToWinMatch, "" + iNumberOfGamesToWinMatch);
        }
        if ( bUseHandInHandOutScoring != null ) {
            extraInfo.put(PreferenceKeys.useHandInHandOutScoring, "" + bUseHandInHandOutScoring);
        }
        //extraInfo.put(PreferenceKeys.tieBreakFormat         , "TwoClearPoints"); // com.doubleyellow.scoreboard.model.TieBreakFormat.TwoClearPoints
        if ( StringUtil.isNotEmpty(sUserName) ) {
            extraInfo.put(PreferenceKeys.refereeName, sUserName);
        }
        if ( StringUtil.isNotEmpty(sMatchLocation) ) {
            extraInfo.put(PreferenceKeys.locationLast, sMatchLocation);
        }

        // Behaviour
      //extraInfo.put(PreferenceKeys.autoSuggestToPostResult, "true"); // should be overwritten in matches feed
      //extraInfo.put(PreferenceKeys.showLastGameInfoInTimer, "false");
      //extraInfo.put(PreferenceKeys.keepScreenOn           , "true");

        // appearance
        if ( StringUtil.isNotEmpty(sCaptionForEmailMatchResult) ) {
            extraInfo.put(PreferenceKeys.captionForEmailMatchResult     , sCaptionForEmailMatchResult);
        }
        if ( StringUtil.isNotEmpty(sCaptionForMessageMatchResult) ) {
            extraInfo.put(PreferenceKeys.captionForMessageMatchResult   , sCaptionForMessageMatchResult);
        }
        if ( StringUtil.isNotEmpty(sCaptionForPostMatchResultToSite) ) {
            extraInfo.put(PreferenceKeys.captionForPostMatchResultToSite, sCaptionForPostMatchResultToSite);
        }

        // sharing
        extraInfo.put(PreferenceKeys.smsResultToNr, sMessageToNr); // e.g. webmasters telephone number
        extraInfo.put(PreferenceKeys.mailResultTo , sEmailTo);     // e.g. webmasters email address

        // colors
        //extraInfo.put(PreferenceKeys.colorSchema, "name=DYBoxen|Color1=#181b1e|Color2=#fde800|Color3=#f3f3f5");

        return _startScoreBoard(extraInfo);
    }

    private boolean _startScoreBoard(Map<PreferenceKeys, String> extraInfo) {
        Bundle bExtraInfo = new Bundle();
        for(PreferenceKeys key: extraInfo.keySet()) {
            bExtraInfo.putString(key.toString(), extraInfo.get(key));
        }
        boolean isIntentSafe = scoreBoardIsInstalled();

        if ( isIntentSafe ) {
            PackageManager packageManager = m_context.getPackageManager();
            Intent scoreBoardIntent = packageManager.getLaunchIntentForPackage(COM_DOUBLEYELLOW_SCOREBOARD);
            scoreBoardIntent.putExtra(PreferenceKeys.class.getSimpleName(), bExtraInfo);
            m_context.startActivity(scoreBoardIntent);
            return true;
        } else {
            showInstallScoreBoardDialog(m_context, m_sInstallMessage);
            return false;
        }
    }

    private boolean scoreBoardIsInstalled() {
        return ContentUtil.isAppInstalled(m_context, COM_DOUBLEYELLOW_SCOREBOARD);
    }

    public void showInstallScoreBoardDialog(final Context context, String sInstallMessage) {
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialog, int choice) {
                switch (choice) {
                    case DialogInterface.BUTTON_POSITIVE:
                        try {
                            Uri uri = Uri.parse("market://details?id=" + COM_DOUBLEYELLOW_SCOREBOARD);
                            context.startActivity(new Intent(Intent.ACTION_VIEW, uri));
                        } catch (ActivityNotFoundException e) {
                            Toast.makeText(context, "Could not launch Play Store!", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        break;
                }
            }
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setMessage(sInstallMessage);
        builder.setPositiveButton(android.R.string.ok, listener);
        builder.setNegativeButton(android.R.string.cancel, listener);
        builder.show();
    }
}
