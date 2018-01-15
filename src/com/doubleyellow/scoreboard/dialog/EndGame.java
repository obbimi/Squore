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

package com.doubleyellow.scoreboard.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;

import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.model.Player;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.model.RacketlonModel;
import com.doubleyellow.scoreboard.model.Sport;
import com.doubleyellow.scoreboard.prefs.PreferenceKeys;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.util.Feature;
import com.doubleyellow.util.ListUtil;
import com.doubleyellow.util.MapUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Dialog that is shown at the end of a game.
 * While still in the first game it will allow you to change the format of the game.
 */
public class EndGame extends BaseAlertDialog
{
    private Player  m_winner           = null;
    private boolean bAllowFormatChange = false;

    public EndGame(Context context, Model matchModel, ScoreBoard scoreBoard) {
        super(context, matchModel, scoreBoard);
    }

    @Override public boolean storeState(Bundle outState) {
        outState.putSerializable(Player.class.getSimpleName(), m_winner);
        return true;
    }

    @Override public boolean init(Bundle outState) {
        init((Player) outState.getSerializable(Player.class.getSimpleName()));
        return true;
    }
    @Override public void show() {

        bAllowFormatChange = matchModel.getEndScoreOfGames().size() == 1 && matchModel.getNrOfPointsToWinGame() < 15;

        boolean matchHasEnded = matchModel.matchHasEnded();
//      boolean bShowWithTimerPrefCheckbox        = false && (matchModel.matchHasEnded() == false); // TODO: decide what looks best: a checkbox or an extra button
        boolean bShowWithNeutralButtonToShowTimer = (matchHasEnded == false) && PreferenceValues.useTimersFeature(context).equals(Feature.Suggest);
        boolean bDrawTimerImageOnButton           = bShowWithNeutralButtonToShowTimer;

        int iResIdNeg = R.string.cmd_cancel;
        if ( bAllowFormatChange ) {
            iResIdNeg = R.string.cmd_change_format;
        }
        int iEndGameMsgResId = matchHasEnded ? R.string.sb_end_game_and_match_message : R.string.sb_start_next_game_confirm_message;
        adb.setPositiveButton(R.string.cmd_ok, dialogClickListener)
           .setNegativeButton(iResIdNeg      , dialogClickListener);

        if ( bShowWithNeutralButtonToShowTimer ) {
            boolean bAutoShowTimer = PreferenceValues.useTimersFeature(context).equals(Feature.Automatic);
            int neutralCaptionResId = bAutoShowTimer ? R.string.sb_cmd_ok_and_skip_timer : R.string.sb_cmd_ok_and_start_timer;
            if ( bAutoShowTimer == false ) {
                if ( bDrawTimerImageOnButton ) {
                    neutralCaptionResId = R.string.sb_cmd_ok_plus;
                }
                adb.setNeutralButton(neutralCaptionResId, dialogClickListener);
            }
        }

        boolean bUseAnnouncements = PreferenceValues.useOfficialAnnouncementsFeature(context).equals(Feature.DoNotUse) == false;
        if ( bUseAnnouncements ) {
            // using official announcements, ensure that 'question' belonging to the buttons is presented last:
            boolean bIncludeGameScores = matchModel.isPossibleMatchVictoryFor()!=null;
            List<String> messages = StartEndAnnouncement.getOfficialMessage(context, matchModel, bIncludeGameScores, false, false, false, true);

            if ( ListUtil.isNotEmpty(messages)) {
                adb.setIcon(R.drawable.microphone)
                   .setTitle(messages.remove(0))
                   .setMessage(ListUtil.join(messages, "\n\n"));
            }
        } else {
            adb.setIcon          (R.drawable.ic_menu_forward)
               .setTitle(getGameOrSetString(iEndGameMsgResId));

            if ( Brand.isRacketlon() ) {
                adb.setMessage(ListUtil.join(getRacketlonMessages(matchHasEnded), "\n\n"));
            }
        }

        ButtonUpdater listener = null;
        if ( bDrawTimerImageOnButton ) {
            listener = new ButtonUpdater(context, false
                    , AlertDialog.BUTTON_NEUTRAL, R.drawable.timer
                    //,AlertDialog.BUTTON_NEGATIVE, android.R.drawable.ic_menu_close_clear_cancel
            );
        }

        adb.setOnKeyListener(getOnBackKeyListener(/*BTN_END_GAME*/));
        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 /* 17 */ ) {
            adb.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override public void onDismiss(DialogInterface dialog) {
                    scoreBoard.triggerEvent(ScoreBoard.SBEvent.endGameDialogEnded, EndGame.this);
                    scoreBoard.enableScoreButton(m_winner);
                }
            });
        }
        dialog = adb.show(listener);
    }

    private List<String> getRacketlonMessages(boolean bMatchEnded) {
        List<String> messages = new ArrayList<>();

        int                  iSet1B             = matchModel.getGameNrInProgress();
        Map<Player, Integer> pointsDiff         = matchModel.getPointsDiff(true);
        int                  iDiff              = MapUtil.getMaxValue(pointsDiff      );
        int                  iNrOfPointsPerSet  = matchModel.getNrOfPointsToWinGame();
        Player               pLeader            = MapUtil.getMaxKey  (pointsDiff, null);
        int                  iNextSetZB         = iSet1B;
        boolean bAllowSelectionOfNextDiscipline = iNextSetZB <= 2;

        if ( bMatchEnded ) {
            messages.add(getString(R.string.x_is_winner_Difference_is_y, matchModel.getName(pLeader), iDiff));
        }
        if ( iSet1B < 4 ) {
            Sport lastDiscipline = matchModel.getSportForGame(iSet1B);
            Sport nextDiscipline = matchModel.getSportForGame(iSet1B + 1);

            if ( bMatchEnded ) {
                messages.add(getString(R.string.dicispline_x_does_not_need_to_be_played, nextDiscipline));
            } else {
                // if a player can reach matchball, add message how many more points he needs
                int iNeeded = 0;
                switch (iSet1B) {
                    case 2:
                        if ( iDiff >= iNrOfPointsPerSet - 1 && iDiff <= iNrOfPointsPerSet + 2 ) {
                            // leading with 20,21,22,23: player needs to win set to win
                            messages.add(getString(R.string.if_player_x_wins_discipline_y_he_wins_the_match, matchModel.getName(pLeader), nextDiscipline));
                        }
                        if (                                   iDiff >= iNrOfPointsPerSet + 3 ) {
                            // leading with 24: player needs 19 to win
                            // leading with 25: player needs 18 to win
                            iNeeded = 2 * iNrOfPointsPerSet - iDiff + 1;
                        }
                        break;
                    case 3:
                        if (                                   iDiff >= 3 ) {
                            // leading with 21: player needs 1 to win
                            // leading with 20: player needs 2 to win
                            // leading with  3: player needs 19 to win
                            iNeeded = 1 * iNrOfPointsPerSet - iDiff + 1;
                        }
                        break;
                }
                switch (iNeeded) {
                    case 0: break;
                    case 1:
                        messages.add(getString(R.string.player_x_needs_1_more_point_to_win , matchModel.getName(pLeader)));
                        break;
                    default:
                        messages.add(getString(R.string.player_x_needs_y_more_points_to_win, matchModel.getName(pLeader), iNeeded));
                        break;
                }
                String sMsg = null;
                if ( bAllowSelectionOfNextDiscipline ) {
                    sMsg = getString(R.string.moving_from_displine_x_to  , lastDiscipline);
                } else {
                    sMsg = getString(R.string.moving_from_displine_x_to_y, lastDiscipline, nextDiscipline);
                }
                messages.add(sMsg);
            }

            racketlonModel = (RacketlonModel) matchModel;

            if ( bAllowSelectionOfNextDiscipline ) {
                // more than 2 sets to play: present a choice
                LinearLayout   ll          = new LinearLayout(context); ll.setOrientation(LinearLayout.VERTICAL);
                List<Sport>    disciplines = racketlonModel.getDisciplines();
                rgDiscipline = new RadioGroup(context);
                for (int i = iNextSetZB; i < disciplines.size(); i++ ) {
                    Sport       sport = disciplines.get(i);
                    RadioButton rb    = new RadioButton(context);
                    rb.setId(sport.ordinal()); // for rg.getCheckedRadioButtonId()
                    rb.setText(sport.toString());
                    rgDiscipline.addView(rb);
                }
                rgDiscipline.check(disciplines.get(iNextSetZB).ordinal());
                ll.addView(rgDiscipline);
                adb.setView(ll);
            }
        }

        return messages;
    }

    public void init(Player winner) {
        m_winner = winner;
    }

    private DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
        @Override public void onClick(DialogInterface dialog, int which) {
            handleButtonClick(which);
        }
    };

    public static final int BTN_END_GAME            = DialogInterface.BUTTON_POSITIVE;
    public static final int BTN_END_GAME_PLUS_TIMER = DialogInterface.BUTTON_NEUTRAL;
    public static final int BTN_CHANGE_MATCH_FORMAT = DialogInterface.BUTTON_NEGATIVE;

    private RadioGroup     rgDiscipline   = null;
    private RacketlonModel racketlonModel = null;
    private void setNextRacketlonDiscipline() {
        if ( (racketlonModel != null) && (rgDiscipline != null) ) {
            int   iSetZBJustFinished = matchModel.getNrOfFinishedGames() - 1;
            int   sportOrdinal       = rgDiscipline.getCheckedRadioButtonId();
            Sport sport              = Sport.values()[sportOrdinal];
            racketlonModel.setDiscipline(iSetZBJustFinished + 1, sport);
        }
    }
    @Override public void handleButtonClick(int which) {
        dialog.dismiss();
        switch ( which ) {
            case BTN_END_GAME:
                setNextRacketlonDiscipline();
                matchModel.endGame();
                break;
            case BTN_END_GAME_PLUS_TIMER:
                //boolean bAutoShowTimer = PreferenceValues.showTimersAutomatically(context);
                PreferenceValues.setOverwrite(PreferenceKeys.useTimersFeature, Feature.Automatic.toString());
                setNextRacketlonDiscipline();
                matchModel.endGame(); // this might possibly already start the timer
                PreferenceValues.removeOverwrite(PreferenceKeys.useTimersFeature);
                break;
            case BTN_CHANGE_MATCH_FORMAT:
                if ( bAllowFormatChange ) {
                    scoreBoard.handleMenuItem(R.id.change_match_format);
                } else {
                    scoreBoard.bGameEndingHasBeenCancelledThisGame = true;
                }
                break;
        }
        if ( Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1 /* 17 */ ) {
            // ondismiss not yet supported on current (OLD) device
            scoreBoard.triggerEvent(ScoreBoard.SBEvent.endGameDialogEnded, this);
            scoreBoard.enableScoreButton(m_winner);
        }
    }
}
