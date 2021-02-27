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

package com.doubleyellow.scoreboard.cast;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Display;
import android.view.KeyEvent;
import android.view.ViewGroup;

import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.model.*;
import com.doubleyellow.scoreboard.prefs.ColorPrefs;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.scoreboard.timer.Timer;
import com.doubleyellow.scoreboard.timer.TimerView;
import com.doubleyellow.scoreboard.timer.TimerViewContainer;
import com.doubleyellow.scoreboard.vico.IBoard;
import com.google.android.gms.cast.CastPresentation;

import java.util.Map;

class PresentationScreen extends CastPresentation implements TimerViewContainer
{
    //-----------------------------
    // variables holding views
    //-----------------------------
    private ViewGroup     vRoot                 = null;
    private EndOfGameView endOfGameView         = null;

    private IBoard        iBoard                = null;

    private Model         matchModel            = null;
    private boolean       bShowGraphDuringTimer = true;
    private int           m_iRemoveHandoutCharAfterXSeconds = 5;

    PresentationScreen(Context context, Display display) {
        super(context, display);

        //bShowGraphDuringTimer = PreferenceValues.showGraphDuringTimer(context, true);
        bShowGraphDuringTimer = PreferenceValues.Cast_ShowGraphDuringTimer(context);

        m_iRemoveHandoutCharAfterXSeconds = 5; // TODO: PreferenceValues only for Squash
    }

    void setModel(Model model) {
        this.matchModel = model;
        this.iBoard.setModel(model);
        if ( endOfGameView != null ) {
            endOfGameView.setModel(this.matchModel);
        }
    }
    @Override public TimerView getTimerView() {
        if ( (endOfGameView != null) && endOfGameView.bIsShowing ) {
            return endOfGameView.getTimerView();
        }
        return iBoard.getTimerView();

    }
    void refreshColors(Map<ColorPrefs.ColorTarget, Integer> mColors) {
        //iBoard.initColors(mColors);
        updateViewWithColorAndScore(getContext());
    }
    void refreshDurationChronos() {
        iBoard.updateGameAndMatchDurationChronos();
    }
    void refreshGamesWonAppearance() {
        iBoard.initGameScoreView();
    }
    @Override public void dismiss() {
        super.dismiss();
        Timer.removeTimerView(true, iBoard);
        Timer.removeTimerView(true, endOfGameView);
    }

    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.P /* 28 */ ) {
            return false;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.percentage);

        vRoot = (ViewGroup) findViewById(android.R.id.content);
        matchModel = ScoreBoard.getMatchModel();
        Context context = getContext();

        iBoard = new IBoard(matchModel, context, getDisplay(), vRoot, null);
        updateViewWithColorAndScore(context);

        matchModel.registerListener(new ScoreChangeListener());
        matchModel.registerListener(new PlayerChangeListener());
        matchModel.registerListener(new ServeSideChangeListener());
        matchModel.registerListener(new SpecialScoreChangeListener());
        matchModel.registerListener(new ComplexChangeListener());
        matchModel.registerListener(new GameEndListener());
        matchModel.registerListener(new MatchEndListener());
        matchModel.registerListener(new CallChangeListener());
        matchModel.registerListener(new BrokenEquipmentListener());
        matchModel.registerListener(new TimingChangedListener());

        iBoard.initGameScoreView();
        iBoard.initBranded();
        iBoard.initFieldDivision();

        TimerView timerView = iBoard.getTimerView();
        Timer.addTimerView(true, timerView);
        if ( (ScoreBoard.timer == null) || (ScoreBoard.timer.isShowing() == false) ) {
            if ( timerView != null ) {
                timerView.cancel();
            }
        }
    }

    private void updateViewWithColorAndScore(Context context) {
        Map<ColorPrefs.ColorTarget, Integer> mColors = ColorPrefs.getTarget2colorMapping(context);
        iBoard.initColors(mColors);

        if ( bShowGraphDuringTimer ) {
            iBoard.getTimerView().cancel();
        }

        for(Player p: Model.getPlayers()) {
            iBoard.updateScore(p, matchModel.getScore(p));
            iBoard.updateServeSide    (p, matchModel.getNextDoubleServe(p), matchModel.getNextServeSide(p), matchModel.isLastPointHandout());
            iBoard.updatePlayerName   (p, matchModel.getName   (p), matchModel.isDoubles());
            iBoard.updatePlayerAvatar (p, matchModel.getAvatar (p));
            iBoard.updatePlayerCountry(p, matchModel.getCountry(p));
            iBoard.updatePlayerClub   (p, matchModel.getClub   (p));
        }
        iBoard.updateGameScores();
    }

    /** invoked to go 'back' to actual scoreboard, coming from the 'pause' screen */
    private void initBoard() {
        setContentView(R.layout.percentage);
        vRoot = (ViewGroup) findViewById(android.R.id.content);
        iBoard.setView(vRoot);
        iBoard.initTimerButton();
        updateViewWithColorAndScore(getContext());
        iBoard.initBranded();
        iBoard.initFieldDivision();
        iBoard.initGameScoreView();

        if ( endOfGameView != null ) {
            Timer.removeTimerView(true, endOfGameView);
            endOfGameView.bIsShowing = false;
        }
        // e.g. for warm-up timer
        TimerView timerView = iBoard.getTimerView();
        Timer.addTimerView(true, timerView);
    }

    // ----------------------------------------------------
    // -----------------model listeners        ------------
    // ----------------------------------------------------

    private class SpecialScoreChangeListener implements Model.OnSpecialScoreChangeListener {
        @Override public void OnGameBallChange(Player[] players, boolean bHasGameBall, boolean bForUndo) {
            iBoard.updateGameBallMessage("PS:OnGameBallChange", players, bHasGameBall);
        }

        @Override public void OnTiebreakReached(int iOccurrenceCount) {
            // no special action on Cast screen
        }

        @Override public void OnGameEndReached(Player leadingPlayer) {
            iBoard.updateGameBallMessage("PS:OnGameEndReached");
        }

        @Override public void OnGameIsHalfwayChange(int iGameZB, int iScoreA, int iScoreB, Halfway hwStatus) {
            if ( matchModel.showChangeSidesMessageInGame(iGameZB) && hwStatus.isHalfway() && hwStatus.changeSidesFor(matchModel.getSport()) ) {
                iBoard.showMessage(getContext().getString(R.string.oa_change_sides), 5);
            } else {
                iBoard.hideMessage();
            }
        }
        @Override public void OnFirstPointOfGame() {
            if ( (endOfGameView != null) && endOfGameView.bIsShowing ) {
                initBoard();
            }
            iBoard.updateGameBallMessage("PS:OnFirstPointOfGame");
            iBoard.updateBrandLogoBasedOnScore();
            iBoard.updateFieldDivisionBasedOnScore();
            iBoard.updateGameAndMatchDurationChronos();
        }
    }
    private class ScoreChangeListener implements Model.OnScoreChangeListener
    {
        @Override public void OnScoreChange(Player p, int iTotal, int iDelta, Call call) {
            iBoard.updateScore(p, iTotal);
            iBoard.updateScoreHistory(iDelta == 1);
            if ( iDelta != 1 ) {
                iBoard.undoGameBallColorSwitch();
                iBoard.updateBrandLogoBasedOnScore();
                iBoard.updateFieldDivisionBasedOnScore();
                iBoard.updateGameAndMatchDurationChronos();
            }
        }
    }
    private class CallChangeListener implements Model.OnCallChangeListener {
        @Override public void OnCallChanged(Call call, Player appealingOrMisbehaving, Player pointAwardedTo, ConductType conductType) {
            // first rally ended in a call
            if ( (endOfGameView != null) && endOfGameView.bIsShowing ) {
                initBoard();
            }
            iBoard.updateScoreHistory(true);
            if ( PreferenceValues.showChoosenDecisionShortly(getContext()) ) {
                iBoard.showChoosenDecision(call, appealingOrMisbehaving, conductType);
            }
        }
    }
    private class BrokenEquipmentListener implements Model.OnBrokenEquipmentListener {
        @Override public void OnBrokenEquipmentChanged(BrokenEquipment equipment, Player affectedPlayer) {
            iBoard.updateScoreHistory(false);
        }
    }
    private class ComplexChangeListener implements Model.OnComplexChangeListener {
        @Override public void OnChanged() {
            if ( (endOfGameView != null) && endOfGameView.bIsShowing ) {
                initBoard();
            }
            // e.g. an undo back into previous game has been done, or score has been adjusted
            iBoard.updateScoreHistory(false);
            iBoard.updateGameScores();
            for(Player p: Model.getPlayers()) {
                iBoard.updateScore    (p, matchModel.getScore(p));
                iBoard.updateServeSide(p, matchModel.getNextDoubleServe(p), matchModel.getNextServeSide(p), matchModel.isLastPointHandout());
            }
            // for restart score and complex undo
            iBoard.updateGameBallMessage("PS:Complex:OnChanged");
        }
    }
    private CountDownTimer m_ctRemoveHandoutChar = null;
    private class ServeSideChangeListener implements Model.OnServeSideChangeListener {
        @Override public void OnServeSideChange(final Player p, final DoublesServe doublesServe, final ServeSide serveSide, boolean bIsHandout, boolean bForUndo) {
            if ( (endOfGameView != null) && endOfGameView.bIsShowing ) {
                initBoard();
            }
            if ( p == null ) { return; } // normally only e.g. for undo's of 'altered' scores
            iBoard.updateServeSide(p, doublesServe, serveSide, bIsHandout);
            if ( Brand.supportChooseServeOrReceive() == false ) {
                // remove any indication on receiver side
                iBoard.updateReceiver(p.getOther(), DoublesServe.NA);
            }

            // code for removing 'handout' char after a few seconds
            if ( m_ctRemoveHandoutChar != null ) {
                m_ctRemoveHandoutChar.cancel();
                m_ctRemoveHandoutChar = null;
            }
            if ( bIsHandout && Brand.isSquash() ) {
                // remove the handout character after a few seconds: so e.g. that WHILE rally is in progress character is now longer visible on cast device
                m_ctRemoveHandoutChar = new CountDownTimer(m_iRemoveHandoutCharAfterXSeconds * 1000, 1000) {
                    @Override public void onTick(long millisUntilFinished) { }
                    @Override public void onFinish() {
                        iBoard.updateServeSide(p, doublesServe, serveSide, false);
                    }
                };
                m_ctRemoveHandoutChar.start();
            }
        }
        @Override public void OnReceiverChange(Player p, DoublesServe doublesServe) {
            iBoard.updateReceiver(p, doublesServe);
        }
    }

    private class MatchEndListener implements Model.OnMatchEndListener {
        @Override public void OnMatchEnded(Player leadingPlayer, EndMatchManuallyBecause endMatchManuallyBecause) {
            // stop the counter
            iBoard.stopMatchDurationChrono();
            //iBoard.updateGameScores();
        }
    }
    private class GameEndListener implements Model.OnGameEndListener {
        @Override public void OnGameEnded(Player winningPlayer) {
            iBoard.updateScoreHistory(false);
            iBoard.updateGameScores();
            if ( PreferenceValues.getTiebreakFormat(getContext()).needsTwoClearPoints() == false ) {
                // uncommon tiebreak format: but do not highlight in this case
                iBoard.undoGameBallColorSwitch();
            }
            iBoard.updateGameBallMessage("PS:OnGameEnded"); // in some cases in racketlon a 0-0 score may be a matchball in set 3 (unlikely) or 4 (more likely)

            if ( bShowGraphDuringTimer ) {
                if (endOfGameView == null) {
                    endOfGameView = new EndOfGameView(getContext(), iBoard, matchModel);
                }
                try {
                    endOfGameView.show(PresentationScreen.this);
/*
                    final String sSSFilename = Util.filenameForAutomaticScreenshot(getContext(), matchModel, ShowOnScreen.OnChromeCast, -1, -1, null);
                    if ( sSSFilename!=null ) {
                        new Handler().postDelayed(new Runnable() {
                            @Override public void run() {
                                ViewUtil.takeScreenShot(getContext(), Brand.brand, sSSFilename, endOfGameView.root);
                            }
                        }, 1000 + (2000 * matchModel.getGameCountHistory().size()));
                    }
*/
                } catch (Exception e) {
                    // sometimes while casting, android.content.res.Resources$NotFoundException was thrown
                    e.printStackTrace();
                }

            }
        }
    }
    private class PlayerChangeListener implements Model.OnPlayerChangeListener {
        @Override public void OnNameChange(Player p, String sName, String sCountry, String sAvatar, String sClub, boolean bIsDoubles) {
            iBoard.updatePlayerName   (p, sName, bIsDoubles);
            iBoard.updatePlayerAvatar (p, sAvatar);
            iBoard.updatePlayerCountry(p, sCountry);
            iBoard.updatePlayerClub   (p, sClub);
        }
        @Override public void OnColorChange(Player p, String sColor, String sColorPrev) {
            iBoard.initPerPlayerColors(p, sColor, sColorPrev);
        }
        @Override public void OnCountryChange(Player p, String sCountry) {
            iBoard.updatePlayerCountry(p, sCountry);
            if ( PreferenceValues.hideFlagForSameCountry(getContext()) ) {
                iBoard.updatePlayerCountry(p.getOther(), matchModel.getCountry(p.getOther()));
            }
        }
        @Override public void OnClubChange(Player p, String sClub) {
            iBoard.updatePlayerClub(p, sClub);
        }
        @Override public void OnAvatarChange(Player p, String sAvatar) {
            iBoard.updatePlayerAvatar(p, sAvatar);
        }
    }
    private class TimingChangedListener implements GameTiming.OnTimingChangedListener  {
        @Override public void OnTimingChanged(int iGameNr, GameTiming.Changed changed, long lTimeStart, long lTimeEnd, GameTiming.ChangedBy changedBy) {
            if ( changed.equals(GameTiming.Changed.Start) ) {
                if (changedBy == GameTiming.ChangedBy.TimerEnded || changedBy == GameTiming.ChangedBy.DialogOpened || changedBy == GameTiming.ChangedBy.DialogClosed) {
                    initBoard();
                }
            }
            if ( (iGameNr == 0) && (changed == GameTiming.Changed.Start) ) {
                // reset duration of match back to 00:00
                iBoard.updateMatchDurationChrono();
            }
            if ( changed == GameTiming.Changed.Start || changed == GameTiming.Changed.Both ) {
                // most likely reset duration of game to 00:00
                iBoard.updateGameDurationChrono();
            }
            if ( changed == GameTiming.Changed.End  || changed == GameTiming.Changed.Both ) {
                if ( matchModel.isPossibleGameVictory() ) {
                    iBoard.stopGameDurationChrono();
                }
            }
            iBoard.updateMatchDurationChrono(); // after screen 'switch' from pause screen to scoreboard screen
        }
    }

}
