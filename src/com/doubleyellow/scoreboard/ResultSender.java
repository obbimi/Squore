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

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.widget.Toast;
import com.doubleyellow.scoreboard.model.ModelFactory;
import com.doubleyellow.scoreboard.model.Player;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.util.*;
//import android.util.Log;

import java.io.File;
import java.text.DateFormat;
import java.util.*;

/**
 * Helper class to send/share the match result(s) via sms/text.
 */
public class ResultSender {

    public static final String TAG = "SB." + ResultSender.class.getSimpleName();

    /** Invoked from RecentMatchesMultiSelect */
    public void send(Context context, List<File> lMatches, String sPackageName, String sDefaultRecipient) {
        String sMsg = getMatchesSummary(lMatches, context);
        send(context, sMsg, sPackageName, sDefaultRecipient);
    }

    /** Invoked from ScoreBoard */
    public void send(Context context, Model matchModel, String sPackageName, String sDefaultRecipient) {
        String sMsg = getMatchSummary(context, matchModel);
        send(context, sMsg, sPackageName, sDefaultRecipient);
    }

    private static boolean bHtmlEmailPossible = false;
    private static int m_iRoundRobin = 0;

    private void send(Context context, String sMsg, String sPackageName, String sDefaultRecipient)
    {
        //String sendMatchResultTo   = PreferenceValues.getDefaultSMSTo(context);

        /*
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setClassName("com.android.mms", "com.android.mms.ui.ConversationList");
        */
        // No Activity found to handle Intent { act=android.intent.action.SEND cat=[android.intent.category.LAUNCHER] typ=vnd.android-dir/mms-sms (has extras) }

        Intent intent;
        if ( false ) {
            // this does not even list sms capable applications
            intent = new Intent(Intent.ACTION_SEND);
            intent.setType("vnd.android-dir/mms-sms"); // someone said: Be aware, this will not work for android 4.4 and probably up... "vnd.android-dir/mms-sms" is not longer supported
        }
        if ( false ) {
            // all (and only) sms-capable apps are listed (including Messages+/Contacts+) but no data is passed on into the body (nor with sms_body, nor EXTRA_TEXT)
            intent = new Intent(Intent.ACTION_MAIN);
            intent.setType("vnd.android-dir/mms-sms");
            intent.putExtra(Intent.EXTRA_TEXT, sMsg);
        }
        if ( false ) {
            // lists sms-capable apps (excluding Messages+). Positive: sms_body and address are being picked up!!
            intent = new Intent(Intent.ACTION_VIEW);
            intent.setType("vnd.android-dir/mms-sms");
            intent.putExtra("sms_body", sMsg);
        }
        if ( StringUtil.isNotEmpty(sPackageName) ) {
            sendToPackage(context, sPackageName, sMsg);
            return;
        }

        String sSubject = Brand.getShortName(context) + " - " + context.getString(R.string.Summary);
        if ( true ) {
            // TODO: best one so far
            if ( StringUtil.isNotEmpty(sDefaultRecipient) ) {
                // no chooser: directly goes to default sms-app: works perfectly e.g. for android stock messaging app/hangout/contacts+
                intent = new Intent(Intent.ACTION_SENDTO); // TODO: maybe only works well on 4.4
                intent.setData(Uri.parse("smsto:" + Uri.encode(sDefaultRecipient)));
                intent.putExtra("sms_body", sMsg);
                intent.putExtra("address" , sDefaultRecipient);
            } else {
                intent = new Intent(Intent.ACTION_SEND);
                intent.putExtra(Intent.EXTRA_SUBJECT, sSubject);
                if ( bHtmlEmailPossible && sMsg.indexOf("<") > 0 && sMsg.indexOf(">") > 0 ) {
                    Spanned spanned;
                    //sMsg = "<html><body><b>Iddo<b><i>Hoeve</i><a href='http://squore.double-yellow.be/'>link</a></body></html>";
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ) {
                        spanned = Html.fromHtml(sMsg, Html.FROM_HTML_MODE_COMPACT);
                    } else {
                        spanned = Html.fromHtml(sMsg);
                        // deprecated
                    }
                    switch (m_iRoundRobin) {
                        case 0:
                            intent.setType("text/html"); // must send spanned element, not String
                            intent.putExtra(Intent.EXTRA_TEXT     , spanned); // for whatsapp
                            intent.putExtra(Intent.EXTRA_HTML_TEXT, spanned);
                            intent.putExtra(Intent.EXTRA_STREAM   , spanned);
                            break;
                        case 1:
                            intent.setType("text/plain");
                            intent.putExtra(Intent.EXTRA_TEXT, sMsg); // for whatsapp
                            intent.putExtra(Intent.EXTRA_HTML_TEXT, spanned);
                            break;
                        case 2:
                            String sPlain = sMsg.replaceAll("<[a-z/]", "");
                            intent.setClipData(ClipData.newHtmlText(sSubject, sPlain, sMsg));
                            break;
                        default:
                            m_iRoundRobin = -1; // back to 0
                    }
                    m_iRoundRobin++;
                } else {
                    intent.setType("text/plain");
                    intent.putExtra(Intent.EXTRA_TEXT, sMsg); // for whatsapp
                }
            }
        }
        //intent.putExtra("sms_body", sbMsg.toString());
/*
        if (StringUtil.isEmpty(sDefaultRecipient) ) {
            String sIn1 = context.getString(R.string.settings);
            String sIn2 = context.getString(Brand.getShortNameResId());
            PreferenceValues.showTip(context, PreferenceKeys.smsResultToNr, context.getString(R.string.pref_smsResultToNr_not_set, sIn1, sIn2), true);
        }
*/
        try {
            //context.startActivity(intent);
            context.startActivity(Intent.createChooser(intent,  sSubject + ":")); // This actually becomes the title of the chooser
        } catch (Exception e) {
          //Log.e(TAG, "Starting sms or call failed");
            e.printStackTrace();
        }
    }

    public static String getMatchSummary(Context context, Model matchModel) {
        return getMatchSummary(context, matchModel, " - ", "\n", true, true);
    }
    private static String getMatchSummary(Context context, Model matchModel, String sPointsSeparator, String sGameScoreSeparator, boolean bIncludeTimeStamp, boolean bIncludeSharedURL) {
        StringBuilder sbMsg = new StringBuilder();
        Map<Player, Integer> gameCount = matchModel.getGamesWon();
        List<Map<Player, Integer>> lGameScores = matchModel.getGameScoresIncludingInProgress();

        sbMsg.append(             matchModel.getName(Player.A)).append(" - ").append(matchModel.getName(Player.B));
        sbMsg.append(" : ").append(gameCount.get(    Player.A)).append(" - ").append( gameCount.get    (Player.B));
        sbMsg.append("\n");

        sbMsg.append(sGameScoreSeparator);
        for(Map<Player, Integer> mGameScore: lGameScores) {
            sbMsg.append(mGameScore.get(Player.A)).append(sPointsSeparator).append(mGameScore.get(Player.B)).append(sGameScoreSeparator);
        }

        sbMsg.append(sGameScoreSeparator);
        final int durationInMinutes = Math.abs(matchModel.getDurationInMinutes());
        sbMsg.append("in ").append(durationInMinutes).append(" min").append("\n");

        if ( bIncludeTimeStamp ) {
            sbMsg.append("\n");
            DateFormat sdf = android.text.format.DateFormat.getDateFormat(context);
            Date matchDate = matchModel.getMatchDate();
            String sDate = sdf.format(matchDate);
            sbMsg.append(context.getString(R.string.date)).append(" : ").append(sDate);
            sbMsg.append("\n");
            sdf = android.text.format.DateFormat.getTimeFormat(context);
            String sTime = sdf.format(matchDate);
            sbMsg.append(context.getString(R.string.time)).append(" : ").append(sTime);
        }
        if ( bIncludeSharedURL ) {
            String shareURL = matchModel.getShareURL();
            if ( StringUtil.isNotEmpty(shareURL) ) {
                if ( bHtmlEmailPossible ) {
                    sbMsg.append("<hr><hr/><a href='");
                    sbMsg.append(shareURL);
                    sbMsg.append("'><b>");
                    sbMsg.append(matchModel.getName(Player.A)).append(" - ").append(matchModel.getName(Player.B));
                    sbMsg.append("</b></a><br/>\n");
                } else {
                    sbMsg.append("\n");
                    sbMsg.append(shareURL);
                }
            }
        }
/*
        String sendMatchResultFrom = "";
        if ( StringUtil.isNotEmpty(sendMatchResultFrom)) {
            sbMsg.append("\n");
            sbMsg.append(sendMatchResultFrom);
        }
*/
        return sbMsg.toString().replaceAll("(\\n)\\s+", "$1");
    }

    /**
     * Get a map with key 'filename + A|B' and value 'clubname' for files.
     * Make corrections for matches where no clus are specified.
     */
    private static Params getClubCorrections(List<File> lSelected, Context context) {
        final String sHome = context.getString(R.string.Home);
        final String sAway = context.getString(R.string.Away);
        final String sREHomeOrAway = "(" + sHome + "|" + sAway + ")";

        Params mHAClubs = new Params();

        Params pClubCountCI       = new Params();
        Params pClubCI2CSVariants = new Params();
        Params mPlayer2ClubsNoHA  = new Params();

        for(File fS: lSelected) {
            Model m = ModelFactory.getTemp();
            try {
                m.fromJsonString(fS);
            } catch (Exception e) {
                continue;
            }
            for(Player p: Player.values() ) {
                String sClub = m.getClub(p);
                if ( StringUtil.isEmpty(sClub) ) {
                    sClub = p.equals(Player.A) ? sHome : sAway;
                } else {
                    mPlayer2ClubsNoHA.addToList(p, sClub.toLowerCase(), true);
                }
                mHAClubs.put(fS.getName() + "__" + p, sClub);
                pClubCountCI.increaseCounter(sClub.toLowerCase());
                pClubCI2CSVariants.addToList(sClub.toLowerCase(), sClub, true);
            }
        }

        Params pClubCountCI_NoHA = MapUtil.filterKeys(pClubCountCI, sREHomeOrAway, Enums.Match.Remove);

        if ( MapUtil.size(pClubCountCI) == 2 ) {
            // clubs always (keys are real names) or never (keys are home/away) entered
            switch (MapUtil.size(pClubCountCI_NoHA)) {
                case 0:
                    // no clubs specified in any of the matches
                    break;
                case 2:
                    // clubs always consistently specified
                    break;
                case 1:
                    // one club consistently specified
                    break;
            }
        } else {
            // we need some corrections
            Params pClubsSpecifiedInEachMatch   = MapUtil.filterValues(pClubCountCI_NoHA, "" + ListUtil.size(lSelected), Enums.Match.Keep);
            Params pClubsSpecifiedInSomeMatches = pClubCountCI.clone(); MapUtil.removeAll(pClubsSpecifiedInSomeMatches, pClubsSpecifiedInEachMatch);

            String sClubSpecifiedInAll  = (String) (MapUtil.size(pClubsSpecifiedInEachMatch  )==1?pClubsSpecifiedInEachMatch  .keySet().iterator().next():null);
            String sClubSpecifiedInSome = (String) (MapUtil.size(pClubsSpecifiedInSomeMatches)==1?pClubsSpecifiedInSomeMatches.keySet().iterator().next():null);

            for(File fS: lSelected) {
                if ( fS == null || fS.exists() == false ) {
                    continue;
                }
                String sClubA = mHAClubs.getRequiredString(fS.getName() + "__" + Player.A);
                String sClubB = mHAClubs.getRequiredString(fS.getName() + "__" + Player.B);
                if ( sClubSpecifiedInAll!=null && sClubSpecifiedInSome!=null) {
                    // one club is specified in all, one club specified but not in all
                    if ( sClubA.matches(sREHomeOrAway) ) {
                        // sClubB must be club specified in all
                        sClubA = sClubSpecifiedInSome;
                        mHAClubs.put(fS.getName() + "__" + Player.A, sClubA);
                    }
                    if ( sClubB.matches(sREHomeOrAway) ) {
                        // sClubA must be club specified in all
                        sClubB = sClubSpecifiedInSome;
                        mHAClubs.put(fS.getName() + "__" + Player.B, sClubB);
                    }
                } else {
                    // both clubs specified but neither of them for all matches
                    if ( sClubA.matches(sREHomeOrAway) ) {
                        // correct A to be a club specified as A in another match
                        sClubA = mPlayer2ClubsNoHA.getList(Player.A, null, true).get(0);
                        mHAClubs.put(fS.getName() + "__" + Player.A, sClubA);
                    }
                    if ( sClubB.matches(sREHomeOrAway) ) {
                        // correct B to be a club specified as B in another match
                        sClubB = mPlayer2ClubsNoHA.getList(Player.B, null, true).get(0);
                        mHAClubs.put(fS.getName() + "__" + Player.B, sClubB);
                    }
                }
            }
        }

        return mHAClubs;
    }
    public static String getMatchesSummary(List<File> lSelected, Context context) {
        Params clubCorrections = getClubCorrections(lSelected, context);
        List<String> lClubs = new ArrayList<>();
        TreeMap<String, Integer> mClub2MatchesWon = new TreeMap<>();
        TreeMap<String, Integer> mClub2GamesWon   = new TreeMap<>();
        TreeMap<String, Integer> mClub2PointsWon  = new TreeMap<>();
        Map<String, String> mMatchDates           = new HashMap<>();
        Map<String, String> mMatchEvents          = new HashMap<>();
        Map<String, String> mMatchDivisions       = new HashMap<>();
        StringBuilder       sbMatchLinks          = new StringBuilder();
        StringBuilder sb = new StringBuilder();
        for(File fS: lSelected) {
            if ( fS == null  || fS.exists() == false ) {
                continue;
            }
            Model m = ModelFactory.getTemp();
            try {
                m.fromJsonString(fS);
            } catch (Exception e) {
                continue;
            }
            String s = getMatchSummary(context, m, "/", " ", false, true);
            sb.append(s);
            sb.append("\n");
            sb.append("\n");

            Player winner      = m.isPossibleMatchVictoryFor();
            if ( winner != null ) {
                String sClubWinner = m.getClub(winner);
                       sClubWinner = clubCorrections.getOptionalString(fS.getName() + "__" + winner, sClubWinner);
                String sClubLoser  = m.getClub(winner.getOther());
                       sClubLoser  = clubCorrections.getOptionalString(fS.getName() + "__" + winner.getOther(), sClubLoser);
                if ( StringUtil.isNotEmpty(sClubWinner) ) {
                    MapUtil.increaseCounter(mClub2MatchesWon, sClubWinner);
                    MapUtil.increaseCounter(mClub2MatchesWon, sClubLoser, 0);
                }
            }
            Map<Player, Integer> gamesWon  = m.getGamesWon();
            List<Map<Player, Integer>> gameScores = m.getGameScoresIncludingInProgress();
            for(Player p: Player.values() ) {
                String sClub = m.getClub(p);
                       sClub  = clubCorrections.getOptionalString(fS.getName() + "__" + p, sClub);
                if ( StringUtil.isNotEmpty(sClub) ) {
                    if ( lClubs.contains(sClub) == false ) {
                        lClubs.add(sClub);
                    }
                    MapUtil.increaseCounter(mClub2GamesWon, sClub, gamesWon.get(p));
                    for(Map<Player, Integer> mGameScore: gameScores) {
                        MapUtil.increaseCounter(mClub2PointsWon, sClub, mGameScore.get(p));
                    }
                }
            }
            mMatchDates    .put(m.getMatchDateYYYYMMDD_DASH(), "1");
            mMatchEvents   .put(m.getEventName()    .toLowerCase(), m.getEventName());
            mMatchDivisions.put(m.getEventDivision().toLowerCase(), m.getEventDivision());

            String shareURL = m.getShareURL();
            if ( false && StringUtil.isNotEmpty(shareURL) ) {
                if ( bHtmlEmailPossible ) {
                    sbMatchLinks.append("<hr><hr/><a href='");
                    sbMatchLinks.append(shareURL);
                    sbMatchLinks.append("'><b>");
                    sbMatchLinks.append(m.getName(Player.A)).append(" - ").append(m.getName(Player.B));
                    sbMatchLinks.append("</b></a><br/>\n");
                } else {
                    sbMatchLinks.append("\n");
                    sbMatchLinks.append(shareURL);
                    sbMatchLinks.append("\n");
                }
            }
        }
        Log.d(TAG, "== clubCorrections: \n" + clubCorrections.toString());
        Log.d(TAG, "== mMatchDates    : \n" + MapUtil.toNiceString(mMatchDates));
        Log.d(TAG, "== mMatchEvents   : \n" + MapUtil.toNiceString(mMatchEvents));
        Log.d(TAG, "== mMatchDivisions: \n" + MapUtil.toNiceString(mMatchDivisions));
        Log.d(TAG, "== sbMatchLinks   : \n" + sbMatchLinks);

        StringBuilder sbClubResult = new StringBuilder();
        if ( ListUtil.isNotEmpty(lClubs) ) {
            if ( ListUtil.size(lClubs) == 2 ) {
                String sM = PreferenceValues.getOAStringFirstLetter(context, R.string.sb_matches);
                String sG = PreferenceValues.getOAStringFirstLetter(context, R.string.oa_games  );
                String sP = PreferenceValues.getOAStringFirstLetter(context, R.string.points    );
                       sM = StringUtil.capitalize( PreferenceValues.getOAString(context, R.string.sb_matches) );
                       sG = StringUtil.capitalize( PreferenceValues.getOAString(context, R.string.oa_games  ) );
                       sP = StringUtil.capitalize( PreferenceValues.getOAString(context, R.string.points    ) );

                sbClubResult.                       append(ListUtil.join(lClubs, " vs "));
                sbClubResult.append("\n").append(sM).append(": ").append(joinPoints(lClubs, mClub2MatchesWon));
                sbClubResult.append("\n").append(sG).append(": ").append(joinPoints(lClubs, mClub2GamesWon));
                sbClubResult.append("\n").append(sP).append(": ").append(joinPoints(lClubs, mClub2PointsWon));
            } else {
                // clubs do not match
            }
        }
        StringBuilder sbCommonData = new StringBuilder();
        if ( MapUtil.size(mMatchEvents) == 1 || MapUtil.size(mMatchDivisions) == 1 ) {
            // add 'label'
            sbCommonData.append(PreferenceValues.getOAString(context, R.string.lbl_event)).append(": ");
        }
        if ( MapUtil.size(mMatchEvents) == 1 ) {
            sbCommonData.append(mMatchEvents.values().iterator().next());
            sbCommonData.append(" ");
        }
        if ( MapUtil.size(mMatchDivisions) == 1 ) {
            sbCommonData.append(mMatchDivisions.values().iterator().next());
            sbCommonData.append(" ");
        }
        sbCommonData.append("\n");

        if ( MapUtil.size(mMatchDates) == 2 ) {
            // usually all interclub matches are played on a single date
            // but if the dates are just one day apart, take the first date (most likely one match started after midnight)
            Iterator<String> iterator = mMatchDates.keySet().iterator();
            String     sYYYYMMDD1 = iterator.next();
            Date       date1      = DateUtil.parseString2Date(sYYYYMMDD1, DateUtil.YYYYMMDD);
            String     sYYYYMMDD2 = iterator.next();
            Date       date2      = DateUtil.parseString2Date(sYYYYMMDD2, DateUtil.YYYYMMDD);
            if ( (date1 != null) && (date2 != null) ) {
                int iDaysDiff = DateUtil.convertToDays(date1.getTime() - date2.getTime());
                if ( Math.abs(iDaysDiff) == 1 ) {
                    mMatchDates.remove(iDaysDiff<0 ? sYYYYMMDD1 : sYYYYMMDD2);
                    Log.d(TAG, "== mMatchDates Corrected : \n" + MapUtil.toNiceString(mMatchDates));
                }
            }
        }
        if ( MapUtil.size(mMatchDates) == 1 ) {
            String     sYYYYMMDD = mMatchDates.keySet().iterator().next();
            Date       date      = DateUtil.parseString2Date(sYYYYMMDD, DateUtil.YYYYMMDD);
            DateFormat sdf       = android.text.format.DateFormat.getDateFormat(context);
            sbCommonData.append(PreferenceValues.getOAString(context, R.string.date)).append(": ");
            if ( (sdf != null) && (date != null) ) {
                sbCommonData.append(sdf.format(date));
            } else {
                sbCommonData.append(sYYYYMMDD);
            }
        }
        String sResult = (sbCommonData.append("\n\n").toString() + sb.toString() + "\n" + sbClubResult.toString()).trim() + "\n" + sbMatchLinks;
        Log.d(TAG, "sResult : \n" + sResult);
        return sResult.trim();
    }

    private static String joinPoints(List<String> lClubs, TreeMap<String, Integer> mClubToNumber) {
        return mClubToNumber.get(lClubs.get(0)) + "/" + mClubToNumber.get(lClubs.get(1));
    }

    public void sendToPackage(Context ctx, String packageName, String sMsg) {

        PackageManager pm=ctx.getPackageManager();
        try {
            Intent waIntent = new Intent(Intent.ACTION_SEND);
            waIntent.setType("text/plain");
            String text = sMsg;

            PackageInfo info=pm.getPackageInfo(packageName, PackageManager.GET_META_DATA);
            //Check if package exists or not. If not then code
            //in catch block will be called
            waIntent.setPackage(packageName);

            waIntent.putExtra(Intent.EXTRA_TEXT, text);
            if ( packageName.equals("com.whatsapp") ) {
                String sendMatchResultTo = PreferenceValues.getDefaultSMSTo(ctx); // TODO: separate preference
                if ( StringUtil.isNotEmpty(sendMatchResultTo) ) {
                    // For example if you live in the Netherlands and having the phone number 0612325032 it would be 31612325023@s.whatsapp.net -> +31 for the Netherlands without the 0's or + and the phone number without the 0.
                    waIntent.setData(Uri.parse("smsto:" + Uri.encode(sendMatchResultTo)));
                }
            }
            ctx.startActivity(Intent.createChooser(waIntent, "Share with"));

        } catch (PackageManager.NameNotFoundException e) {
            Toast.makeText(ctx, packageName + " not Installed", Toast.LENGTH_SHORT).show();
        }

    }
}
