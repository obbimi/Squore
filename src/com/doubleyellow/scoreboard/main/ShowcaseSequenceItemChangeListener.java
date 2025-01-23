/*
 * Copyright (C) 2025  Iddo Hoeve
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
package com.doubleyellow.scoreboard.main;

import android.util.Log;

import androidx.appcompat.app.ActionBar;

import com.doubleyellow.android.showcase.ShowcaseSequence;
import com.doubleyellow.android.showcase.ShowcaseView;
import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.model.Call;
import com.doubleyellow.scoreboard.model.GSMModel;
import com.doubleyellow.scoreboard.model.LockState;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.model.Player;
import com.doubleyellow.scoreboard.prefs.PreferenceKeys;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.scoreboard.prefs.ShowOnScreen;
import com.doubleyellow.scoreboard.vico.IBoard;
import com.doubleyellow.util.Feature;

public class ShowcaseSequenceItemChangeListener extends ScoreBoardListener implements ShowcaseSequence.OnSequenceItemChangeListener
{
    private static final String TAG = "SB." + ShowcaseSequenceItemChangeListener.class.getSimpleName();
    ShowcaseSequenceItemChangeListener(ScoreBoard scoreBoard) {
        super(scoreBoard);
    }

    @Override public void beforeShow(int position, int iDeltaNotUsed, int iViewId, int iResId) {
        Log.d(TAG, "beforeShow:: highlight view " + scoreBoard.getResourceEntryName(iViewId) + ", display text " + scoreBoard.getResourceEntryName(iResId));

        Model matchModel = getMatchModel();

        if ( /*(position == 1) &&*/ (iViewId == R.id.float_toss) ) {
            PreferenceValues.setOverwrite(PreferenceKeys.useTossFeature                     , Feature.Suggest.toString());
            PreferenceValues.setOverwrite(PreferenceKeys.useTimersFeature                   , Feature.Suggest.toString());
            PreferenceValues.setOverwrite(PreferenceKeys.autoSuggestToPostResult            , "false");
            PreferenceValues.setOverwrite(PreferenceKeys.showDetailsAtEndOfGameAutomatically, "false");
            scoreBoard.restartScore();
            if ( Brand.isRacketlon() ) {
                PreferenceValues.setOverwrite(PreferenceKeys.useOfficialAnnouncementsFeature, Feature.DoNotUse.toString());
                matchModel.setNrOfPointsToWinGame(21);
                matchModel.setNrOfGamesToWinMatch(0);
                matchModel.setPlayerNames("Ricky", "Lonny");
            } else if ( Brand.isTabletennis() ) {
                PreferenceValues.setOverwrite(PreferenceKeys.useOfficialAnnouncementsFeature, Feature.DoNotUse.toString());
                matchModel.setNrOfPointsToWinGame(11);
                matchModel.setNrOfGamesToWinMatch(4);
                matchModel.setPlayerNames("Tabby", "Tenny");
            } else if ( Brand.isBadminton() ) {
                PreferenceValues.setOverwrite(PreferenceKeys.useOfficialAnnouncementsFeature, Feature.DoNotUse.toString());
                matchModel.setNrOfPointsToWinGame(21);
                matchModel.setNrOfGamesToWinMatch(2);
                matchModel.setPlayerNames("Baddy", "Tonny");
            } else if ( Brand.isGameSetMatch() ) {
                PreferenceValues.setOverwrite(PreferenceKeys.useOfficialAnnouncementsFeature, Feature.DoNotUse.toString());
                PreferenceValues.setOverwrite(PreferenceKeys.useTimersFeature               , Feature.DoNotUse.toString());
                GSMModel gsmModel = (GSMModel) matchModel;
                gsmModel.setNrOfPointsToWinGame(6); // = setNrOfGamesToWinSet
                gsmModel.setNrOfGamesToWinMatch(2); // = setNrOfSetsToWinMatch
                gsmModel.setPlayerNames("Paddy", "Tenny");
            } else {
                PreferenceValues.setOverwrite(PreferenceKeys.useOfficialAnnouncementsFeature, Feature.Suggest.toString());
                matchModel.setNrOfPointsToWinGame(11);
                matchModel.setNrOfGamesToWinMatch(3);
                matchModel.setPlayerNames("Shaun", "Casey");
            }
            matchModel.setPlayerAvatar(Player.A, null);
            matchModel.setPlayerAvatar(Player.B, null);
        }
        if ( position == 0 ) {
            PreferenceValues.setOverwrite(PreferenceKeys.showMatchDurationChronoOn   , ShowOnScreen.OnChromeCast.toString());
            PreferenceValues.setOverwrite(PreferenceKeys.showLastGameDurationChronoOn, ShowOnScreen.OnChromeCast.toString());
            scoreBoard.iBoard.updateGameAndMatchDurationChronos();
        }
        final int nrOfPointsToWinGame = matchModel.getNrOfPointsToWinGame();
        if (iViewId == R.id.btn_side1) {
            matchModel.changeScore(Player.B);
            matchModel.changeScore(Player.A); // ensure button for player A is highlighted for squash
            if (Brand.isNotSquash()) {
                while (matchModel.getServer().equals(Player.A) == false) {
                    matchModel.changeScore(Player.A); // ensure button for player A is highlighted
                }
            }
        } else if (iViewId == R.id.btn_score1) {
            if (matchModel.getMaxScore() < 3) {
                matchModel.changeScore(Player.A);
                matchModel.changeScore(Player.A);
                matchModel.changeScore(Player.B);
                matchModel.changeScore(Player.B);
                matchModel.changeScore(Player.B);
                matchModel.changeScore(Player.B);
            }
            if (Brand.isGameSetMatch()) {
                matchModel.changeScore(Player.A);
                matchModel.changeScore(Player.B);
                while (matchModel.getMaxScore() > 0) {
                    // continue until a game is won
                    matchModel.changeScore(Player.B);
                }
                matchModel.changeScore(Player.A);
                matchModel.changeScore(Player.A);
                matchModel.changeScore(Player.B);
            }
        } else if (iViewId == R.id.float_changesides) {// TODO
        } else if (iViewId == R.id.float_timer) {
            PreferenceValues.setOverwrite(PreferenceKeys.showHideButtonOnTimer, false);
            if (matchModel.isPossibleGameVictory() == false) {
                matchModel.setGameScore_Json(0, nrOfPointsToWinGame, nrOfPointsToWinGame - 4, 5, false);
                scoreBoard.endGame(true);
            }
        } else if (iViewId == R.id.sb_official_announcement) {
            matchModel.setGameScore_Json(1, nrOfPointsToWinGame - 1, nrOfPointsToWinGame + 1, 6, false);
            scoreBoard.endGame(true);
        } else if (iViewId == R.id.gamescores) {
        } else if (iViewId == R.id.txt_player1) {
            matchModel.changeScore(Player.B);
            matchModel.changeScore(Player.A);
            if (matchModel.getMaxScore() < 3) {
                IBoard.setBlockToasts(true);
                matchModel.recordAppealAndCall(Player.A, Call.ST);
                matchModel.changeScore(Player.B);
                matchModel.recordAppealAndCall(Player.B, Call.YL);
                IBoard.setBlockToasts(false);
            }
        } else if (iViewId == R.id.dyn_score_details || iViewId == R.id.float_match_share) {
            boolean bDontChangePast = true;
            if (Brand.isNotSquash()) {
                if (Brand.isGameSetMatch()) {
                    // TODO: ensure match is ended
                    GSMModel gsmModel = (GSMModel) matchModel;
                    //matchModel.setSetScore_Json(); // TODO
                } else {
                    // trigger model changes that are not triggered by user step (sb_official_announcement), because some show case screens are skipped for e.g. Racketlon
                    matchModel.setGameScore_Json(1, nrOfPointsToWinGame - 1, nrOfPointsToWinGame + 1, 6, bDontChangePast);
                    scoreBoard.endGame(true);
                }
            }
            if (matchModel.matchHasEnded() == false) {
                IBoard.setBlockToasts(true);
                matchModel.setGameScore_Json(2, nrOfPointsToWinGame, nrOfPointsToWinGame - 5, 5, bDontChangePast);
                if (Brand.isRacketlon()) {
                    // add a score that ends the racketlon match by points
                    matchModel.setGameScore_Json(3, 15, 11, 8, bDontChangePast);
                } else if (Brand.isTabletennis()) {
                    // add a score that ends the tabletennis match
                    matchModel.setGameScore_Json(3, nrOfPointsToWinGame + 2, nrOfPointsToWinGame, 8, bDontChangePast);
                    matchModel.setGameScore_Json(4, nrOfPointsToWinGame, nrOfPointsToWinGame - 4, 7, bDontChangePast);
                } else if (Brand.isBadminton()) {
                    // add a score that ends the badminton match
                    // best of 3, nothing to do
                } else if (Brand.isGameSetMatch()) {
                    // TODO: add a score that ends the tennis/padel match
                } else {
                    int iGameInProgress1B = matchModel.getGameNrInProgress();
                    while (matchModel.matchHasEnded() == false) {
                        matchModel.setGameScore_Json(iGameInProgress1B - 1, nrOfPointsToWinGame + 2, nrOfPointsToWinGame, 8, bDontChangePast);
                        iGameInProgress1B++;
                        if (iGameInProgress1B >= matchModel.getNrOfGamesToWinMatch() * 2) {
                            break;
                        } // additional safety precaution
                    }
                }
                scoreBoard.endGame(true);
                scoreBoard.showShareFloatButton(true, true);
                IBoard.setBlockToasts(false);
                matchModel.setLockState(LockState.LockedEndOfMatch);
            }
        } else if (iViewId == R.id.sb_overflow_submenu || iViewId == R.id.dyn_undo_last || iViewId == android.R.id.home) {
            ActionBar actionBar = scoreBoard.getXActionBar();
            if ((actionBar != null) && (actionBar.isShowing() == false)) {
                // show action bar
                if (scoreBoard.isWearable() == false) {
                    scoreBoard.toggleActionBar(actionBar);
                }
            }
        } else if (iViewId == R.id.float_new_match) {
        }
    }

    @Override public void onDismiss(ShowcaseView itemView, int position, ShowcaseView.DismissReason reason) {
        IBoard.setBlockToasts(false);
        if ( reason == null || reason.equals(ShowcaseView.DismissReason.SkipSequence) ) {
            scoreBoard.cancelShowCase();
        }
    }
}
