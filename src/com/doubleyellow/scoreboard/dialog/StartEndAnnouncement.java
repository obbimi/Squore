package com.doubleyellow.scoreboard.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.model.Player;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.util.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StartEndAnnouncement extends BaseAlertDialog
{
    public StartEndAnnouncement(Context context, Model matchModel, ScoreBoard scoreBoard) {
        super(context, matchModel, scoreBoard);
    }

    @Override public boolean storeState(Bundle outState) {
        outState.putSerializable(ScoreBoard.AnnouncementTrigger.class.getSimpleName(), triggeredBy);
        return true;
    }

    @Override public boolean init(Bundle outState) {
        ScoreBoard.AnnouncementTrigger announcementTrigger = (ScoreBoard.AnnouncementTrigger) outState.getSerializable(ScoreBoard.AnnouncementTrigger.class.getSimpleName());
        init(announcementTrigger);
        return true;
    }

    private ScoreBoard.AnnouncementTrigger triggeredBy = null;
    @Override public void show() {

        boolean autoEndGame = PreferenceValues.endGameSuggestion(context).equals(Feature.Automatic);

        boolean bIncludeReceiver = matchModel.hasStarted()  == false;
        boolean bIncludeToServe  =                                     (matchModel.gameHasStarted() == false)               && (this.triggeredBy.equals(ScoreBoard.AnnouncementTrigger.StartOfGame) || this.triggeredBy.equals(ScoreBoard.AnnouncementTrigger.Manual));
        boolean bIncludeGameTo   = matchModel.hasStarted()          && (matchModel.gameHasStarted() == true || autoEndGame) && (this.triggeredBy.equals(ScoreBoard.AnnouncementTrigger.EndOfGame)   || this.triggeredBy.equals(ScoreBoard.AnnouncementTrigger.Manual));
        List<String> messages = getOfficialMessage(context, matchModel, true, bIncludeToServe, bIncludeReceiver, true, bIncludeGameTo);

        // should never be the case... but to prevent strange dialogs
        if ( ListUtil.size(messages) == 0 ) { return; }

        int iResIdCaption = bIncludeToServe && (matchModel.matchHasEnded()==false) ? R.string.cmd_start : R.string.cmd_ok;
        adb.setIcon(R.drawable.microphone)
           .setTitle(messages.remove(0))
           .setMessage(ListUtil.join(messages, "\n\n"))
           .setPositiveButton(iResIdCaption, onClickListener)
           .setOnKeyListener(getOnBackKeyListener(DialogInterface.BUTTON_POSITIVE)); // 20161228 change from NEUTRAL to POSITIVE so that start of game gets 'set' no mather how dialog is dismissed

        // in a try catch to prevent crashing if somehow scoreBoard is not showing any longer
        try {
            dialog = adb.show();
        } catch (Exception e) {
            //Log.e(TAG, "Could not show official announcement");
            e.printStackTrace();
        }
    }

    public void init(ScoreBoard.AnnouncementTrigger trigger) {
        triggeredBy = trigger;
    }

    private DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
        @Override public void onClick(DialogInterface dialogInterface, int i) {
            handleButtonClick(i);
        }
    };

    @Override public void handleButtonClick(int which) {
        dialog.dismiss();
        scoreBoard.triggerEvent(ScoreBoard.SBEvent.officialAnnouncementClosed, this);
    }

    static List<String> getOfficialMessage(Context ctx, Model matchModel
                                                 , boolean bIncludeGameScores
                                                 , boolean bIncludeToServe
                                                 , boolean bIncludeReceiver
                                                 , boolean bIncludeMatchFormat
                                                 , boolean bIncludeWinnerOfLastGame) {
        List<String> lMsgs = new ArrayList<String>();

        Map<Player, Integer> gameCount = matchModel.getGamesWon();
        int iGamesA = MapUtil.getInt(gameCount, Player.A, 0);
        int iGamesB = MapUtil.getInt(gameCount, Player.B, 0);

        Player winnerOfLastGame = matchModel.getServer();
        if ( matchModel.hasStarted() && bIncludeWinnerOfLastGame ) {
            Map<Player, Integer> last = ListUtil.getLast(matchModel.getEndScoreOfGames());
            if ( last != null ) {
                String sPrefix = last.get(winnerOfLastGame) + "-" + last.get(winnerOfLastGame.getOther()) + ", ";
                if ( matchModel.matchHasEnded() ) {
                    lMsgs.add(sPrefix + PreferenceValues.getOAString(ctx, R.string.oa_match_to_x, matchModel.getName_no_nbsp(winnerOfLastGame, false)));
                } else {
                    lMsgs.add(sPrefix + PreferenceValues.getOAString(ctx, R.string.oa_game_to_x , matchModel.getName_no_nbsp(winnerOfLastGame, false)));
                }
            }
        }

        if ( iGamesA == iGamesB ) {
            // scoring is equal
            if ( iGamesA == 0 ) {
                // start of match
                boolean bIncludeEventAndRound = true; // TODO: parameter? configurable?
                if ( bIncludeEventAndRound ) {
                    String sEvent = matchModel.getEventName();
                    if ( StringUtil.isNotEmpty(sEvent) ) {
                        String sDivision = matchModel.getEventDivision();
                        String sRound    = matchModel.getEventRound   ();
                        if ( StringUtil.isNotEmpty(sDivision)) {
                            lMsgs.add(sEvent + " - " + sDivision);
                            if ( StringUtil.isNotEmpty(sRound)) {
                                lMsgs.add(sRound);
                            }
                        } else {
                            if ( StringUtil.isNotEmpty(sRound)) {
                                lMsgs.add(sEvent + " - " + sRound);
                            } else {
                                lMsgs.add(sEvent);
                            }
                        }
                    }
                }
            } else {
                String s = (iGamesA==1) ? PreferenceValues.getOAString(ctx, R.string.oa_1_game_all) : PreferenceValues.getOAString(ctx, R.string.oa_x_games_all, iGamesA, iGamesB);
                lMsgs.add(s);
            }
        } else {
            // there is a leader
            Player pLeader  = Player.A;
            Player pTrailer = Player.B;
            String sGameScore = x_GamesTo_y(iGamesA, iGamesB, ctx);
            if ( iGamesA < iGamesB ) {
                pLeader  = Player.B;
                pTrailer = Player.A;
                sGameScore = x_GamesTo_y(iGamesB, iGamesA, ctx);
            }
            String sLeader  = matchModel.getName_no_nbsp(pLeader , false);
            String sTrailer = matchModel.getName_no_nbsp(pTrailer, false);
            if ( matchModel.matchHasEnded() ) {
                // there is a winner
                // End of a match: 11 – 8, match to Jones, 3 games to 2, 3–11, 11–7, 6–11, 11– 9, 11– 8.
                String s /*= PreferenceValues.getOAString(ctx, R.string.oa_x_wins_n_against_y, sLeader, sGameScore, sTrailer);
                       s   = PreferenceValues.getOAString(ctx, R.string.oa_a_wins_xGamesToy        , sLeader, sGameScore);
                       s */= sGameScore;
                lMsgs.add(s);

                if ( bIncludeGameScores ) {
                    int iLines = ctx.getResources().getInteger(R.integer.maxNumberOfLinesToUseForGameEndScores);
                    List<Map<Player, Integer>> scores = matchModel.getEndScoreOfGames();
                    final double dDiv               = (double) ListUtil.size(scores) / iLines;
                    final double dDivCeil           = Math.ceil(dDiv);
                    final int    iStartNewLineAfter = (int) dDivCeil;
                    StringBuilder sbGamesScores = new StringBuilder();
                    int iCnt = 0;
                    for(Map<Player, Integer> score: scores) {
                        if ( sbGamesScores.length() != 0 ) {
                            if ( iCnt % iStartNewLineAfter == 0 ) {
                                sbGamesScores.append("\n");
                            } else {
                                sbGamesScores.append(", ");
                            }
                        }
                        iCnt++;
                        sbGamesScores.append(score.get(pLeader)).append("-").append(score.get(pTrailer));
                    }
                    lMsgs.add(sbGamesScores.toString());
                }
            } else {
                // no winner yet
                String s /*= PreferenceValues.getOAString(ctx, R.string.oa_x_leads_n_against_y, sLeader, sGameScore, sTrailer);
                       s*/ = PreferenceValues.getOAString(ctx, R.string.oa_a_leads_xGamesToy        , sLeader, sGameScore);
                lMsgs.add(s);
            }
        }

        // show announcements for game about to start
        if ( matchModel.matchHasEnded() == false ) {
            if ( bIncludeToServe && matchModel.hasStarted() ) {
                int iGameAboutToStartZeroBased = iGamesA + iGamesB;
                String[] sGameAboutToStart = PreferenceValues.getOAStringArray(ctx, R.array.FirstSecondThirdFourthFifth);
                if ( iGameAboutToStartZeroBased < sGameAboutToStart.length ) {
                    lMsgs.add(PreferenceValues.getOAString(ctx, R.string.oa_x_th_game, StringUtil.capitalize(sGameAboutToStart[iGameAboutToStartZeroBased])));
                }
            }
            if ( bIncludeToServe && bIncludeReceiver ) {
                boolean bKeepCountry = (matchModel.hasStarted() == false);
                Player receiver      = matchModel.getReceiver();
                String sNameServer   = matchModel.getName_no_nbsp(winnerOfLastGame        , false);
                String sNameReceiver = matchModel.getName_no_nbsp(receiver, false);
                if ( bKeepCountry ) {
                    String sLang  = PreferenceValues.officialAnnouncementsLanguage(ctx).toString(); // RWValues.getDeviceLanguage(ctx)
                    sNameServer   = CountryUtil.addFullCountry("%s (%s)", sNameServer  , matchModel.getCountry(winnerOfLastGame), sLang);
                    sNameReceiver = CountryUtil.addFullCountry("%s (%s)", sNameReceiver, matchModel.getCountry(receiver        ), sLang);
                }
                lMsgs.add(PreferenceValues.getOAString(ctx, R.string.oa_x_to_serve__y_to_receive, sNameServer, sNameReceiver));
            } else if ( bIncludeToServe ) {
                lMsgs.add(PreferenceValues.getOAString(ctx, R.string.oa_x_to_serve, matchModel.getName(winnerOfLastGame)));
            }
            if ( bIncludeMatchFormat && (matchModel.hasStarted() == false)) {
                int iNrOfGamesToWin      = matchModel.getNrOfGamesToWinMatch();
                int iNrOfPointsToWinGame = matchModel.getNrOfPointsToWinGame();
                if ( iNrOfPointsToWinGame == 11 && Brand.isSquash() ) {
                    // default is 11. If current format is also 11 do NOT make it part of the announcement
                    lMsgs.add(PreferenceValues.getOAString(ctx, R.string.oa_best_of_x_games     , (iNrOfGamesToWin * 2 - 1)));
                } else {
                    lMsgs.add(PreferenceValues.getOAString(ctx, R.string.oa_best_of_x_games_to_y, (iNrOfGamesToWin * 2 - 1), iNrOfPointsToWinGame));
                }
            }

            if ( bIncludeToServe && (matchModel.isUsingHandicap() == false ) ) {
                lMsgs.add(PreferenceValues.getOAString(ctx, R.string.oa_love_all));
            }
        }

        // restore locale if required
/*
        if ( localeRestore != null ) {
            newResources(res, localeRestore);
        }
*/

        return lMsgs;
    }

    private static String x_GamesTo_y(int iGamesLeader, int iGamesTrailer, Context ctx) {
        //return iGamesA + "-" + iGamesB;
        return iGamesLeader
        + " " + (iGamesLeader==1?PreferenceValues.getOAString(ctx, R.string.oa_game):PreferenceValues.getOAString(ctx, R.string.oa_games))
        + " " + PreferenceValues.getOAString(ctx, R.string.oa_x_games_TO_y)
        + " " + (iGamesTrailer==0?PreferenceValues.getOAString(ctx, R.string.oa_love):String.valueOf(iGamesTrailer));
    }
}
