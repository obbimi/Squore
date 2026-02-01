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
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import com.doubleyellow.android.util.ContentUtil;
import com.doubleyellow.scoreboard.prefs.PreferenceKeys;
import com.doubleyellow.scoreboard.prefs.StartupAction;
import com.doubleyellow.scoreboard.prefs.URLsKeys;
import com.doubleyellow.util.MapUtil;
import com.doubleyellow.util.StringUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class that is included in all apps that want to invoke the scoreboard themselves.
 * For now these are
 * - Double Yellow app
 *
 * winmerge C:\code\gitlab\DYBoxen\src\main\java\com\doubleyellow\scoreboard\Glue.java C:\code\github\Squore\src\com\doubleyellow\scoreboard\Glue.java
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

    public boolean startSquoreBoardForOfficialMatches(List<String> sFeedKeyValue, String sRefereeName) {
        Map<String, Object> extraInfo = new HashMap<>();

        StringBuilder sb = new StringBuilder();
        for(int i=0; i < sFeedKeyValue.size(); i+=2) {
            sb.append(sFeedKeyValue.get(i)).append("=").append(sFeedKeyValue.get(i + 1)).append("\n");
        }
        extraInfo.put(PreferenceKeys.feedPostUrls.toString(), sb.toString());

      //extraInfo.put(PreferenceKeys.StartupAction           , StartupAction.StartNewMatch.toString());
        extraInfo.put(/*PreferenceKeys*/ "MatchTabbed_defaultTab"  , (/*com.doubleyellow.scoreboard.match.MatchTabbed.SelectTab.Feed.toString()*/"Feed"));

      //extraInfo.put(PreferenceKeys.numberOfPointsToWinGame , String.valueOf(11) );
      //extraInfo.put(PreferenceKeys.numberOfGamesToWinMatch , String.valueOf(3)  );
      //extraInfo.put(PreferenceKeys.useHandInHandOutScoring , "false");
      //extraInfo.put(PreferenceKeys.tieBreakFormat          , "TwoClearPoints");
        //extraInfo.put(PreferenceKeys.showTimersAutomatically , "true");
      //extraInfo.put(PreferenceKeys.showLastGameInfoInTimer, "true");
        //extraInfo.put(PreferenceKeys.showOfficialAnnouncements, "true");
        //extraInfo.put(PreferenceKeys.showTieBreakDialog      , "true");
        //extraInfo.put(PreferenceKeys.showWhoServesDialog     , "true");
        if ( StringUtil.isNotEmpty(sRefereeName) ) {
            extraInfo.put(PreferenceKeys.refereeName.toString(), sRefereeName);
        }

        return _startScoreBoard(extraInfo);
    }

    public boolean startSquoreBoard() {
        Map<String, Object> extraInfo = new HashMap<>();
        return _startScoreBoard(extraInfo);
    }

    /** Eg. invoked from the 'Double Yellow' app. See also ScoreBoard.handleStartedFromOtherApp() */
    public boolean startSquoreBoard( String  sFeedName
                                   , String  sFeedMatchesURL
                                   , String  sFeedPlayersUrl
                                   , String  sPostMatchResultURL
                                 //, String  sPostData
                                   , String  sUserKey
                                   , String  sUserName
                                   , String  sCaptionForPostMatchResultToSite
                                   , String  sIconForPostMatchResultToSite
                                 //, String  sMessageToNr            // default telephone number to text the result of a message to
                                 //, String  sEmailTo
                                 //, AnnouncementLanguage announcementLanguage
                                   , StartupAction startupAction) {
        Map<String, String> extraInfo = new HashMap<>();

        // Feed
        String sUrls =
                     URLsKeys.Name        + "=" + sFeedName           + "\n"
                   + URLsKeys.FeedMatches + "=" + sFeedMatchesURL     + "\n";
        if ( sFeedPlayersUrl != null ) {
            sUrls += URLsKeys.FeedPlayers + "=" + sFeedPlayersUrl     + "\n";
        }
        if ( sPostMatchResultURL != null ) {
            sUrls += URLsKeys.PostResult + "=" + sPostMatchResultURL  + "\n";
        }
        extraInfo.put(PreferenceKeys.feedPostUrls.toString(), sUrls);
        if (sUserName != null && !sUserName.trim().isEmpty()) {
            extraInfo.put(PreferenceKeys.additionalPostKeyValuePairs.toString(), sUserKey + "=" + sUserName);
        }

        extraInfo.put(PreferenceKeys.StartupAction.toString(), startupAction.toString());
        if (startupAction.equals(StartupAction.StartNewMatch)) {
            extraInfo.put(/*PreferenceKeys.*/"MatchTabbed_defaultTab", "Feed");
        }

        //extraInfo.put(PreferenceKeys.tieBreakFormat         , "TwoClearPoints"); // com.doubleyellow.scoreboard.model.TieBreakFormat.TwoClearPoints
        if ( StringUtil.isNotEmpty(sUserName) ) {
            extraInfo.put(PreferenceKeys.refereeName.toString(), sUserName);
        }

        // Behaviour
      //extraInfo.put(PreferenceKeys.autoSuggestToPostResult, "true"); // should be overwritten in matches feed
      //extraInfo.put(PreferenceKeys.showLastGameInfoInTimer, "false");
      //extraInfo.put(PreferenceKeys.keepScreenOn           , "true");

        // appearance
        if ( StringUtil.isNotEmpty(sCaptionForPostMatchResultToSite) ) {
            extraInfo.put(PreferenceKeys.captionForPostMatchResultToSite.toString(), sCaptionForPostMatchResultToSite);
        }
        if ( StringUtil.isNotEmpty(sIconForPostMatchResultToSite) ) {
            extraInfo.put(PreferenceKeys.iconForPostMatchResultToSite.toString(), sIconForPostMatchResultToSite);
        }

        Map<String, Object> mStringKeys = MapUtil.keysToString(extraInfo);

        // colors
        //extraInfo.put(PreferenceKeys.colorSchema, "name=DYBoxen|Color1=#181b1e|Color2=#fde800|Color3=#f3f3f5");

        return _startScoreBoard(mStringKeys);
    }

    private boolean _startScoreBoard(Map<String, Object> extraInfo) {
        Bundle bExtraInfo = new Bundle();
        for(String key: extraInfo.keySet()) {
            bExtraInfo.putString(key, String.valueOf(extraInfo.get(key)) );
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
        DialogInterface.OnClickListener listener = (dialog, choice) -> {
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
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setMessage(sInstallMessage);
        builder.setPositiveButton(android.R.string.ok, listener);
        builder.setNegativeButton(android.R.string.cancel, listener);
        builder.show();
    }
}
