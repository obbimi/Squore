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

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import com.doubleyellow.scoreboard.feed.Authentication;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.model.Player;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.prefs.PostDataPreference;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.android.task.URLTask;
import com.doubleyellow.android.util.ContentReceiver;
import com.doubleyellow.util.Base64Util;
import com.doubleyellow.util.ListUtil;
import com.doubleyellow.util.MapUtil;
import com.doubleyellow.util.StringUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

//import android.util.Log;

/**
 * Helper class for posting the match result to a website.
 * Used e.g. to store match results for Double Yellow boxen.
 */
public class ResultPoster implements ContentReceiver
{
    public static final String TAG = "SB." + ResultPoster.class.getSimpleName();

    private String             sPostURL           = null;
    private Authentication     eAuthentication    = Authentication.None;
    private String             sUrlName           = null;
    private PostDataPreference postDataPreference = null;
    private String             sAccountName       = null;
    private ScoreBoard         scoreBoard         = null;

    public ResultPoster(String sURL, String sUrlName, PostDataPreference postDataPreference, String sAccountName, Authentication authentication) {
        sPostURL                = sURL;
        this.sUrlName           = sUrlName;
        this.postDataPreference = postDataPreference;

        this.sAccountName       = sAccountName;
        this.eAuthentication    = authentication;
    }

    public String getURL() {
        return sPostURL;
    }

    public Authentication getAuthentication() {
        return eAuthentication;
    }

    public String getURLName() {
        return sUrlName;
    }

    public void post(ScoreBoard scoreBoard, Model matchModel, Authentication authentication, String sUserName, String sPassword)
    {
        PostTask postTask = new PostTask(scoreBoard, sPostURL);
        postTask.setContentReceiver(this);

        Map<Player, Integer> pointsWon = matchModel.getTotalNumberOfPointsScored();

        String sHandicapScores = null;
        if ( matchModel.isUsingHandicap() ) {
            StringBuilder sbHandicaps = new StringBuilder();
            List<Map<Player, Integer>> HandicapEndScores = matchModel.getGameStartScoreOffsets();
            for (Map<Player, Integer> mHandicapScore : HandicapEndScores) {
                Integer iA = mHandicapScore.get(Player.A);
                Integer iB = mHandicapScore.get(Player.B);
                sbHandicaps.append(iA).append("-").append(iB).append(",");
            }
            sHandicapScores = sbHandicaps.toString();
        }

        String sBodyAuthentication1 = "";
        String sBodyAuthentication2 = "";
        if ( authentication != null ) {
            switch (authentication) {
                case Basic:
                    postTask.setHeader("Authorization", "Basic " + Base64Util.encode((sUserName + ":" + sPassword).getBytes()));
                    break;
                case BodyParameters:
                    sBodyAuthentication1 = "Username=" + sUserName;
                    sBodyAuthentication2 = "Password=" + sPassword;
                    break;
                case None:
                    break;
            }
        }

        PackageInfo info = null;
        try {
            info = scoreBoard.getPackageManager().getPackageInfo(scoreBoard.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        String sJson = "";
        switch (postDataPreference) {
            case JsonDetailsOnly:
                sJson = matchModel.toJsonString(scoreBoard);
                postTask.execute(URLTask.__BODY__, sJson);
                break;
            case BasicWithJsonDetails:
                sJson = matchModel.toJsonString(scoreBoard);
                // fall through
            case Basic:
                String sPlayers        = matchModel.getName(Player.A)    + "_" + matchModel.getName(Player.B);
                String sResult         = matchModel.getResultShort();
                Player winner          = matchModel.isPossibleMatchVictoryFor();
                String sAdditionalModel= matchModel.getAdditionalPostParams(); // e.g. if match id is for an interclub match, subid can be for position 1 to 4 (or 5 like in leaguemaster)
                String sAdditionalPref = PreferenceValues.getAdditionalPostKeyValuePairs(scoreBoard);

                List<String> lAdditionalModel = new ArrayList<>();
                if ( StringUtil.isNotEmpty(sAdditionalModel) ) {
                    lAdditionalModel = Arrays.asList(StringUtil.singleCharacterSplit(sAdditionalModel, "|")) ;
                    sAdditionalModel = ListUtil.join(lAdditionalModel, ""); // temporary use no splitter to determine splitter
                }
                String sParseToMapLater = "";
                String[] sPossibleSplitters = {"!", "@", "#", "$", "%", "^","|", "^", "~"};
                for(String sSplitter: sPossibleSplitters) {
                    if ( (sAdditionalPref + sAdditionalModel + sBodyAuthentication1 + sBodyAuthentication2).contains(sSplitter) == false ) {
                        sParseToMapLater = sSplitter + sAdditionalPref
                                         + sSplitter + sAdditionalModel
                                         + sSplitter + sBodyAuthentication1
                                         + sSplitter + sBodyAuthentication2
                                         + sSplitter + ListUtil.join(lAdditionalModel, sSplitter)
                                         + sSplitter;
                        break;
                    }
                }


                int    durationInMin = Math.abs(matchModel.getDurationInMinutes());
                final String sWinner = matchModel.getName(winner);
                postTask.execute(sPlayers, sResult
                        , "appversion"         , String.valueOf((info!=null) ? info.versionCode : 0)
                        , "eventname"          , matchModel.getEventName()
                        , "eventdivision"      , matchModel.getEventDivision()
                        , "eventround"         , matchModel.getEventRound()
                        , "eventlocation"      , matchModel.getEventLocation()
                        , "whendate"           , matchModel.getMatchDateYYYYMMDD_DASH()
                        , "whentime"           , matchModel.getMatchStartTimeHHMMSSXXX()
                        , "player1"            , matchModel.getName(Player.A)
                        , "player2"            , matchModel.getName(Player.B)
                        , "id"                 , matchModel.getSourceID()         // match id
                        , "player1id"          , matchModel.getPlayerId(Player.A) // player id when 'team match' is selected from feed, after which team players were selected for both teams
                        , "player2id"          , matchModel.getPlayerId(Player.B)
                        , "country1"           , matchModel.getCountry(Player.A)
                        , "country2"           , matchModel.getCountry(Player.B)
                        , "club1"              , matchModel.getClub(Player.A)
                        , "club2"              , matchModel.getClub(Player.B)
                        , "result"             , sResult
                        , "gamescores"         , matchModel.getGameScores()
                        , "winner"             , (sWinner==null?"":sWinner)
                      //, "winnerAB"           , String.valueOf(winner)
                        , "winner12"           , String.valueOf(winner==null?"":winner.ordinal() + 1)
                        , "duration"           , String.valueOf(durationInMin)
                        , "totalpointsplayer1" , String.valueOf(MapUtil.getInt(pointsWon, Player.A, 0))
                        , "totalpointsplayer2" , String.valueOf(MapUtil.getInt(pointsWon, Player.B, 0))
                        , "tiebreakformat"     , matchModel.getTiebreakFormat().toString()
                        , "handinhandout"      , String.valueOf(matchModel.isEnglishScoring())
                        , "handicaps"          , sHandicapScores
                        , "json"               , sJson
                        , sParseToMapLater
                );
                break;
        }

        this.scoreBoard = scoreBoard;
    }

    @Override public void receive(String sContent, FetchResult result, long lCacheAge, String sLastSuccessfulContent) {
        Log.d(TAG, "Content : " + sContent);

        if ( StringUtil.isEmpty(sContent) ) {
            sContent = "<html><i>No result in body of post to <b>" + sPostURL + "</b></i>. Please ask webmaster to provide a feedback text/html/json.</html>";
        }

        String sMessage   = sContent.trim();
        String sTitle     = null;
        boolean bResultOK = result.equals(FetchResult.OK);

        // if returned content appears to be JSON try and interpret it
        if ( sContent.startsWith("{") && sContent.endsWith("}") ) {
            try {
                JSONObject jo = new JSONObject(sContent);
                sTitle    = jo.optString("title"  , sTitle);
                sMessage  = jo.optString("message", sMessage);
                bResultOK = jo.optString("result" , "OK").equalsIgnoreCase("OK");

/*
{
  "result": "OK",
  "title": "Dunham server says",
  "message": "The result for match xxx was stored."
}

{
  "result": "NOK",
  "title": "Dunham server warning",
  "message": "A result for match X was already stored. Keeping initial result"
}
*/
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        scoreBoard.hideProgress();
        ScoreBoard.dialogWithOkOnly(scoreBoard, sTitle, sMessage, bResultOK==false);
/*
        if ( result.equals(FetchResult.OK) ) {
            if ( sContent.contains("demo.post.") ) {
                // assume demo post url was used
                if ( ! PreferenceValues.showTip(scoreBoard, PreferenceKeys.feedPostUrl, scoreBoard.getString(R.string.posturl_tip) + "\n\n" + sContent, false) ) {
                    Toast.makeText(this.scoreBoard, sContent, Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this.scoreBoard, sContent, Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this.scoreBoard, sContent, Toast.LENGTH_LONG).show();
        }
*/
    }

    private class PostTask extends URLTask {
        public PostTask(Context context, String sURL) {
            super(context, sURL, URLTask.POST, null);
        }
        @Override public String getBaseUrl(Context context) { return ""; }
        @Override public String getAccountName(Context context) { return ResultPoster.this.sAccountName; }
        @Override public String addParametersToReturnUrl(Context ctx, String sURL, String configuredAccountName, String sReturn) { return null; }
        @Override public int getReadTimeout() { return 10000; }
        @Override public boolean returnContent(String sURL) { return true; }
        @Override public boolean validateContent(String sURL, String sContent, String sValidation) { return true; }
        @Override public int getMaximumReuseCacheTimeMS(String sCacheFile) { return 0; }
        @Override public Map.Entry<FetchResult, String> downloadHelperFiles(Context ctx, String sURL, Map<String, String> hParams) { return null; }
    }
}
