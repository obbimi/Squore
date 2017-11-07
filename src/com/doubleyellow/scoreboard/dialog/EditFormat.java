package com.doubleyellow.scoreboard.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.Spinner;
import com.doubleyellow.android.util.ColorUtil;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.match.MatchView;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.model.Player;
import com.doubleyellow.scoreboard.model.TieBreakFormat;
import com.doubleyellow.scoreboard.prefs.ColorPrefs;
import com.doubleyellow.scoreboard.prefs.PreferenceKeys;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.util.ListUtil;
import com.doubleyellow.util.MapUtil;
import com.doubleyellow.util.StringUtil;
import com.doubleyellow.view.EnumSpinner;

import java.util.List;
import java.util.Map;

/**
 * Displays the edit format dialog to the user.
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

    private Spinner                     spNumberOfGamesToWin;
    private Spinner                     spGameEndScore;
    private Spinner                     spPauseDuration;
    private EnumSpinner<TieBreakFormat> spTieBreakFormat;
    private CheckBox                    cbUseEnglishScoring;

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
        MatchView.initNumberOfGamesToWin(context, spNumberOfGamesToWin, matchModel.getNrOfGamesToWinMatch(), max);

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
        MatchView.initGameEndScore(context, spGameEndScore, nrOfPointsToWinGame, Math.max(2,iNewNrOfPointsToWinGame));
        spGameEndScore.setEnabled(ListUtil.size(gameScoresIncludingInProgress)<=1); // only allow editing this if no games have ended yet

        TieBreakFormat tbfPref = PreferenceValues.getTiebreakFormat(context);
        spTieBreakFormat = (EnumSpinner<TieBreakFormat>) vg.findViewById(R.id.spTieBreakFormat);
        spTieBreakFormat.setSelected(tbfPref);
        spTieBreakFormat.setEnabled(true); // TODO: set to false if there has already been a tiebreak format

        spPauseDuration = (Spinner) vg.findViewById(R.id.spPauseDuration);
        MatchView.initPauseDuration(context, spPauseDuration);

        cbUseEnglishScoring = (CheckBox) vg.findViewById(R.id.useHandInHandOutScoring);
        cbUseEnglishScoring.setChecked(PreferenceValues.useHandInHandOutScoring(context));
        cbUseEnglishScoring.setEnabled(iNewNrOfPointsToWinGame==0);

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialog, int which) {
                handleButtonClick(which);
            }
        };

        adb.setView(vg)
                .setTitle(R.string.pref_MatchFormat)
                //.setMessage(sb.toString().trim())
                .setIcon(R.drawable.ic_action_mouse)
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
                int iNrOfServesPerPlayer = Integer.parseInt(spNrOfServees.getSelectedItem().toString().trim());
                bChanged = matchModel.setNrOfServesPerPlayer(iNrOfServesPerPlayer) || bChanged;
*/

                TieBreakFormat tbf = TieBreakFormat.values()[spTieBreakFormat.getSelectedItemPosition()];
                bChanged = matchModel.setTiebreakFormat(tbf) || bChanged;

                String sDuration = (String) spPauseDuration.getSelectedItem();
                if ( StringUtil.isNotEmpty(sDuration) ) {
                    int iDuration = Integer.parseInt(sDuration);
                    PreferenceValues.setNumber(PreferenceKeys.timerPauseBetweenGames, context, iDuration);
                }

                break;
            case DialogInterface.BUTTON_NEGATIVE:
                // Do nothing.
                break;
        }
        scoreBoard.triggerEvent(ScoreBoard.SBEvent.editFormatDialogEnded, this);
    }
}