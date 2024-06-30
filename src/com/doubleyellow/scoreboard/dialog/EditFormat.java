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

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.ToggleButton;

import com.doubleyellow.android.util.ColorUtil;
import com.doubleyellow.android.view.EnumSpinner;
import com.doubleyellow.android.view.ViewUtil;
import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.model.FinalSetFinish;
import com.doubleyellow.scoreboard.model.GoldenPointFormat;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.match.MatchView;
import com.doubleyellow.scoreboard.model.DoublesServeSequence;
import com.doubleyellow.scoreboard.model.GSMModel;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.model.Player;
import com.doubleyellow.scoreboard.model.TieBreakFormat;
import com.doubleyellow.scoreboard.prefs.ColorPrefs;
import com.doubleyellow.scoreboard.prefs.PreferenceKeys;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.scoreboard.prefs.Preferences;
import com.doubleyellow.util.ListUtil;
import com.doubleyellow.util.MapUtil;


import java.util.List;
import java.util.Map;

/**
 * Displays the edit match format dialog to the user.
 * Holds only limited set op options to edit.
 */
public class EditFormat extends BaseAlertDialog {

    public EditFormat(Context context, Model matchModel, ScoreBoard scoreBoard) {
        super(context, matchModel, scoreBoard);
    }

    @Override public boolean storeState(Bundle outState) {
        return true;
    }

    @Override public boolean init(Bundle outState) {
        return true;
    }

    private Spinner                           spNumberOfGamesToWin;
    private Spinner                           spGameEndScore;
    private Spinner                           spWarmupDuration;
    private ToggleButton                      cbWarmupDuration;
    private Spinner                           spPauseDuration;
    private ToggleButton                      cbPauseDuration;
    private EnumSpinner<GoldenPointFormat>    spGoldenPointFormat;
    private EnumSpinner<FinalSetFinish>       spFinalSetFinish;
    private CompoundButton                    cbStartTiebreakOneGameEarly;
    private ToggleButton                      tbBestOf_or_TotalOf;
    private EnumSpinner<TieBreakFormat>       spTieBreakFormat;
    private EnumSpinner<DoublesServeSequence> spDoublesServeSequence;
    private CompoundButton                    cbUseEnglishScoring;

    @Override public void show() {
        Map<ColorPrefs.ColorTarget, Integer> mColors = ColorPrefs.getTarget2colorMapping(context);
        int iMainBgColor   = mColors.get(ColorPrefs.ColorTarget.middlest);
        int iLabelTxt      = mColors.get(ColorPrefs.ColorTarget.darkest);
        int iSelectTxt     = mColors.get(ColorPrefs.ColorTarget.darkest);
        int iInputBgColor  = mColors.get(ColorPrefs.ColorTarget.middlest);
        int iInputTxtColor = mColors.get(ColorPrefs.ColorTarget.lightest);

        List<Map<Player, Integer>> gameScoresIncludingInProgress = matchModel.getGameScoresIncludingInProgress();

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ViewGroup vg = (ViewGroup) inflater.inflate(R.layout.editformat, null);

        ColorUtil.setBackground(vg, iMainBgColor);
        ColorPrefs.setColors(vg, ColorPrefs.Tags.item);

        int iNrOfGamesToWinPref = PreferenceValues.numberOfGamesToWinMatch(context);
        int max = Math.max(iNrOfGamesToWinPref, 11);
        spNumberOfGamesToWin = (Spinner) vg.findViewById(R.id.spNumberOfGamesToWin);
        MatchView.initNumberOfGamesToWin(context, spNumberOfGamesToWin, matchModel.getNrOfGamesToWinMatch(), max, null);

        int iNewNrOfPointsToWinGame = 0;
        int nrOfPointsToWinGame = matchModel.getNrOfPointsToWinGame();
        if (ListUtil.size(gameScoresIncludingInProgress) != 0) {
            Map<Player, Integer> score = gameScoresIncludingInProgress.get(0);
            int maxValue = MapUtil.getMaxValue(score);
            iNewNrOfPointsToWinGame = maxValue;
            if ( maxValue - MapUtil.getMinValue(score) == 2 ) {
                if ( nrOfPointsToWinGame == 11 && maxValue > 15 ) {
                    iNewNrOfPointsToWinGame = 15;
                }
            }
        }
        spGameEndScore = (Spinner) vg.findViewById(R.id.spGameEndScore);
        MatchView.initGameEndScore(context, spGameEndScore, nrOfPointsToWinGame, Math.max(2,iNewNrOfPointsToWinGame), null, null);
        spGameEndScore.setEnabled(ListUtil.size(gameScoresIncludingInProgress)<=1); // only allow editing this if no games have ended yet

        TieBreakFormat tbfPref = PreferenceValues.getTiebreakFormat(context);
        spTieBreakFormat = (EnumSpinner<TieBreakFormat>) vg.findViewById(R.id.spTieBreakFormat);
        spTieBreakFormat.setSelected(tbfPref);
        spTieBreakFormat.setEnabled(true); // TODO: set to false if there has already been a tiebreak

        if ( matchModel.isDoubles() && Brand.supportsDoubleServeSequence() ) {
            spDoublesServeSequence = (EnumSpinner<DoublesServeSequence>) vg.findViewById(R.id.spDoublesServeSequence);
            DoublesServeSequence dssPref = PreferenceValues.getDoublesServeSequence(context);
            spDoublesServeSequence.setSelected(dssPref);
            spDoublesServeSequence.setEnabled(true);
            spDoublesServeSequence.setVisibility(View.VISIBLE);
        } else {
            ViewUtil.hideViews(vg, R.id.ll_doubleServeSequence);
        }
        spWarmupDuration = (Spinner)      vg.findViewById(R.id.spWarmupDuration);
        cbWarmupDuration = (ToggleButton) vg.findViewById(R.id.cbWarmupDuration);
        MatchView.initDuration(context, cbWarmupDuration, spWarmupDuration, null, Preferences.syncAndClean_warmupValues(context), PreferenceValues.getWarmupDuration(context));

        spPauseDuration = (Spinner)      vg.findViewById(R.id.spPauseDuration);
        cbPauseDuration = (ToggleButton) vg.findViewById(R.id.cbPauseDuration);
        MatchView.initDuration(context, cbPauseDuration, spPauseDuration, null, Preferences.syncAndClean_pauseBetweenGamesValues(context), PreferenceValues.getPauseDuration(context));

        tbBestOf_or_TotalOf = (ToggleButton) vg.findViewById(R.id.tbBestOf_or_TotalOf);
        if ( tbBestOf_or_TotalOf != null ) {
            tbBestOf_or_TotalOf.setChecked(matchModel.playAllGames());
        }

        cbUseEnglishScoring = (CompoundButton) vg.findViewById(R.id.useHandInHandOutScoring);
        boolean bHandInHandOut = PreferenceValues.useHandInHandOutScoring(context);
        cbUseEnglishScoring.setChecked(bHandInHandOut);
        cbUseEnglishScoring.setEnabled(iNewNrOfPointsToWinGame==0);

        if ( Brand.isGameSetMatch() ) {
            ViewUtil.hideViews(vg, R.id.llScoringType); // hand-in/out
            ViewUtil.hideViews(vg, R.id.llTieBreakFormat); // 2 clear points, choose 1
            ViewUtil.hideViews(vg, R.id.llPauseDuration);
        } else {
            ViewUtil.hideViews(vg, R.id.llGoldenPoint);
            ViewUtil.hideViews(vg, R.id.llFinalSetFinish);
        }

        spGoldenPointFormat = (EnumSpinner<GoldenPointFormat>) vg.findViewById(R.id.goldenPointFormat);
        if ( spGoldenPointFormat != null ) {
            if ( Brand.isGameSetMatch() ) {
                GoldenPointFormat goldenPointFormat = ((GSMModel) matchModel).getGoldenPointFormat();
                spGoldenPointFormat.setSelected(goldenPointFormat);
            }
        }
        spFinalSetFinish = (EnumSpinner<FinalSetFinish>) vg.findViewById(R.id.spFinalSetFinish);
        if ( spFinalSetFinish != null ) {
            if ( Brand.isGameSetMatch() ) {
                FinalSetFinish finalSetFinish = ((GSMModel) matchModel).getFinalSetFinish();
                spFinalSetFinish.setSelected(finalSetFinish);
            }
        }
        cbStartTiebreakOneGameEarly = (CompoundButton) vg.findViewById(R.id.cbStartTieBreakOnGameEarlyGSM); // not yet used/visible

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialog, int which) {
                handleButtonClick(which);
            }
        };

        if ( isNotWearable() ) {
            adb.setTitle(R.string.pref_MatchFormat);
            adb.setIcon(R.drawable.ic_action_mouse);
        }
        adb.setView(vg)
                .setPositiveButton(android.R.string.ok, dialogClickListener)
                .show();
    }

    public void handleButtonClick(int which) {
        switch (which){
            case DialogInterface.BUTTON_POSITIVE:
                int iNrOfGamesToWinMatch = (Integer.parseInt(spNumberOfGamesToWin.getSelectedItem().toString()) + 1) / 2;
                boolean bChanged = matchModel.setNrOfGamesToWinMatch(iNrOfGamesToWinMatch);

                int iNrOfPoints2Win = Integer.parseInt(spGameEndScore.getSelectedItem().toString().trim());
                bChanged = matchModel.setNrOfPointsToWinGame(iNrOfPoints2Win) || bChanged;

/*
                int iNrOfServesPerPlayer = Integer.parseInt(spNrOfServes.getSelectedItem().toString().trim());
                bChanged = matchModel.setNrOfServesPerPlayer(iNrOfServesPerPlayer) || bChanged;
*/

                if ( (cbUseEnglishScoring != null) && cbUseEnglishScoring.isEnabled() ) {
                    boolean bUseEnglishScoring = cbUseEnglishScoring.isChecked();
                    matchModel.setEnglishScoring(bUseEnglishScoring);
                    PreferenceValues.setBoolean(PreferenceKeys.useHandInHandOutScoring, context, bUseEnglishScoring);
                }
                if ( Brand.isGameSetMatch() ) {
                    GSMModel gsmModel = (GSMModel) matchModel;
                    if ( (spGoldenPointFormat != null) && spGoldenPointFormat.isEnabled() ) {
                        GoldenPointFormat goldenPointFormat = GoldenPointFormat.values()[spGoldenPointFormat.getSelectedItemPosition()];
                        //GoldenPointFormat goldenPointFormat = spGoldenPointFormat.getSelectedEnum(); // DNW yet
                        gsmModel.setGoldenPointFormat(goldenPointFormat);
                        PreferenceValues.setEnum(PreferenceKeys.goldenPointFormat, context, goldenPointFormat);
                    }
                    if ( (spFinalSetFinish != null) && spFinalSetFinish.isEnabled() ) {
                        FinalSetFinish finalSetFinish = FinalSetFinish.values()[spFinalSetFinish.getSelectedItemPosition()];
                        gsmModel.setFinalSetFinish(finalSetFinish);
                        PreferenceValues.setEnum(PreferenceKeys.finalSetFinish, context, finalSetFinish);
                    }
                    if ( (cbStartTiebreakOneGameEarly != null) && cbStartTiebreakOneGameEarly.isEnabled() ) {
                        boolean bStartTiebreakOneGameEarly = cbStartTiebreakOneGameEarly.isChecked();
                        gsmModel.setStartTiebreakOneGameEarly(bStartTiebreakOneGameEarly);
                        PreferenceValues.setBoolean(PreferenceKeys.StartTiebreakOneGameEarly, context, bStartTiebreakOneGameEarly);
                    }
                }

                TieBreakFormat tbf = TieBreakFormat.values()[spTieBreakFormat.getSelectedItemPosition()];
                bChanged = matchModel.setTiebreakFormat(tbf) || bChanged;

                if ( matchModel.isDoubles() && Brand.supportsDoubleServeSequence() ) {
                    DoublesServeSequence dss = DoublesServeSequence.values()[spDoublesServeSequence.getSelectedItemPosition()];
                    //DoublesServeSequence dss = spDoublesServeSequence.getSelectedEnum(); // DNW yet
                    bChanged = matchModel.setDoublesServeSequence(dss) || bChanged;
                    PreferenceValues.setEnum(PreferenceKeys.doublesServeSequence, context, dss); // make it the default for next matches
                }

                MatchView.getValueFromSelectListOrToggleAndStoreAsPref(context, cbWarmupDuration, spWarmupDuration, PreferenceKeys.timerWarmup           , PreferenceValues.getWarmupDuration(context));
                MatchView.getValueFromSelectListOrToggleAndStoreAsPref(context, cbPauseDuration , spPauseDuration , PreferenceKeys.timerPauseBetweenGames, PreferenceValues.getPauseDuration(context));

                if ( tbBestOf_or_TotalOf != null ) {
                    matchModel.setPlayAllGames(tbBestOf_or_TotalOf.isChecked());
                }

                break;
            case DialogInterface.BUTTON_NEGATIVE:
                // Do nothing.
                break;
        }
        showNextDialog();
    }
}