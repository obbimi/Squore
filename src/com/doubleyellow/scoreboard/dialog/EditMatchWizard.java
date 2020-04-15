/*
 * Copyright (C) 2020  Iddo Hoeve
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
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.doubleyellow.android.util.ColorUtil;
import com.doubleyellow.android.view.FloatingActionButton;
import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.model.FinalSetFinish;
import com.doubleyellow.scoreboard.model.GSMModel;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.model.ModelFactory;
import com.doubleyellow.scoreboard.model.Player;
import com.doubleyellow.scoreboard.prefs.ColorPrefs;
import com.doubleyellow.scoreboard.prefs.PreferenceKeys;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.scoreboard.view.PlayerTextView;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.view.SelectObjectToggle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/** For specifying new match details in as-small-as-possible dialog. Introduced for wearables */
public class EditMatchWizard extends BaseAlertDialog
{
    private final static String TAG = "SB." + EditMatchWizard.class.getSimpleName();

    public EditMatchWizard(Context context, Model matchModel, ScoreBoard scoreBoard) {
        super(context, matchModel, scoreBoard);
    }

    @Override public boolean storeState(Bundle outState) {
        return false;
    }

    @Override public boolean init(Bundle outState) {
        return false;
    }

    private LinearLayout m_rootView = null;

    @Override public void show() {
        LinearLayout.LayoutParams lpPC = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        LinearLayout.LayoutParams lpCC = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

/*
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ViewGroup vg = (ViewGroup) inflater.inflate(R.layout.matchwizard, null);
*/

        Map<ColorPrefs.ColorTarget, Integer> mColors = ColorPrefs.getTarget2colorMapping(context);
        int iMainBgColor   = mColors.get(ColorPrefs.ColorTarget.playerButtonBackgroundColor);
        int iLabelTxt      = mColors.get(ColorPrefs.ColorTarget.playerButtonTextColor);
        int iSelectTxt     = mColors.get(ColorPrefs.ColorTarget.darkest);
        int iInputBgColor  = mColors.get(ColorPrefs.ColorTarget.scoreButtonBackgroundColor);
        int iInputTxtColor = mColors.get(ColorPrefs.ColorTarget.scoreButtonTextColor);

        m_rootView = new LinearLayout(context);
        m_rootView.setOrientation(LinearLayout.VERTICAL);
        ColorUtil.setBackground(m_rootView, iMainBgColor);

        float fResizeFactorForWizard = 1.5f;
        m_buttonSizePx = (int) (scoreBoard.getFloatingButtonSizePx() * fResizeFactorForWizard);

        GridLayout llPrevNext = new GridLayout(context);
        llPrevNext.setColumnCount(4);
        llPrevNext.setRowCount(1);
        FrameLayout.LayoutParams lpButtonSize = new FrameLayout.LayoutParams(m_buttonSizePx, m_buttonSizePx);

        //m_rootView.addView(llPrevNext, (int)(ViewUtil.getScreenWidth(context) * fResizeFactorForWizard), ViewGroup.LayoutParams.MATCH_PARENT);
        m_rootView.addView(llPrevNext, lpPC);

        m_fabNext = new FloatingActionButton.Builder(context, m_buttonSizePx)
                .withDrawable(R.drawable.arrow_right)
                .withButtonColor(iInputBgColor)
                .create(false);
        m_fabNext.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                storeAndShowNext(1);
            }
        });

        m_fabPrev = new FloatingActionButton.Builder(context, m_buttonSizePx)
                .withDrawable(R.drawable.arrow_left)
                .withButtonColor(iInputBgColor)
                .create(false);
        m_fabPrev.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                storeAndShowNext(-1);
            }
        });

        GridLayout.LayoutParams lpGrid = getGridLayoutParams();
        lpGrid.columnSpec = GridLayout.spec(0/*, 1, 1.0f*/);
        llPrevNext.addView(m_fabPrev, lpGrid);

        lpGrid = getGridLayoutParams();
        lpGrid.columnSpec = GridLayout.spec(3/*, 1, 1.0f*/);
        llPrevNext.addView(m_fabNext, lpGrid);

        for(Player p: Player.values()) {
            PlayerTextView txtPlayer = new PlayerTextView(context); txtPlayer.setTag(p); txtPlayer.setHint(getString(R.string.lbl_player) + " " + p);
            lControls.add(txtPlayer);

          //txtPlayer.setImeOptions(EditorInfo.IME_ACTION_DONE); // don't --> will close the dialog
          //txtPlayer.setImeOptions(EditorInfo.IME_ACTION_NEXT); // don't --> next not yet available
            txtPlayer.setOnEditorActionListener(onEditorActionListener); // TODO: can we use this?
            txtPlayer.setText(matchModel.getName(p));

            txtPlayer.setTextSize(TypedValue.COMPLEX_UNIT_PX, txtPlayer.getTextSize() * fResizeFactorForWizard);
        }

        {
            LinearLayout llBestOf = new LinearLayout(context);
            llBestOf.setOrientation(LinearLayout.HORIZONTAL);
            {
                List<String> lValues = new ArrayList<>();
                String sBO = getString(R.string.best_of_x_games_to_y_1);
                String sTO = getString(R.string.total_of_x_games_to_y_1);
                lValues.add(sBO);
                lValues.add(sTO);
                SelectObjectToggle<String> tbBestOf_or_TotalOf = new SelectObjectToggle<String>(context, lValues);
                tbBestOf_or_TotalOf.setTag(PreferenceKeys.playAllGames);
                boolean bPlayAll = matchModel.playAllGames();
                tbBestOf_or_TotalOf.setSelectedIndex(bPlayAll?1:0);
                tbBestOf_or_TotalOf.setMinimumHeight(0); // has no effect when called here

                llBestOf.addView(tbBestOf_or_TotalOf, lpCC);
            }
            {
                int iPreferredNrToWin = PreferenceValues.numberOfGamesToWinMatch(context);
                    iPreferredNrToWin = matchModel.getNrOfGamesToWinMatch();

                SortedSet<Integer> lValues = new TreeSet<>();
                lValues.add(1);
                lValues.add(2);
                lValues.add(3);
                if ( Brand.isTabletennis() ) {
                    lValues.add(4);
                }
                lValues.add(iPreferredNrToWin); // typically 4 (best of 7) for tabletennis

                List<String> lDisplayValues = new ArrayList<String>();
                for(Integer iNrToWin: lValues) {
                    int iPreferredBestOf = iNrToWin * 2 - 1;
                    int resId = iPreferredBestOf == 1 ? R.string.oa_game : R.string.oa_games;
                    if ( Brand.isGameSetMatch() ) {
                        resId = iPreferredBestOf == 1 ? R.string.oa_set : R.string.oa_sets;
                    }
                    lDisplayValues.add(iPreferredBestOf + " " + getString(resId));
                }
                SelectObjectToggle<Integer> tbTotalNrOfGames = new SelectObjectToggle<Integer>(context, lValues, lDisplayValues);
                tbTotalNrOfGames.setTag(PreferenceKeys.numberOfGamesToWinMatch);
                tbTotalNrOfGames.setSelected(iPreferredNrToWin);

                llBestOf.addView(tbTotalNrOfGames, lpCC);
            }

            {
                int iPreferredNrToWin = PreferenceValues.numberOfPointsToWinGame(context);
                    iPreferredNrToWin = matchModel.getNrOfPointsToWinGame();
                SortedSet<Integer> lValues = new TreeSet<>();
                int iCommonEndScoresResId = R.array.commonEndScores_Squash;
                    iCommonEndScoresResId = PreferenceValues.getSportTypeSpecificResId(context, iCommonEndScoresResId);
                int[] iaCommonEndScores = context.getResources().getIntArray(iCommonEndScoresResId);
                for(int iEndScore: iaCommonEndScores) {
                    lValues.add(iEndScore);
                }
                lValues.add(iPreferredNrToWin);
                List<String> lDisplayValues = new ArrayList<String>();
                for(Integer iVal: lValues) {
                    lDisplayValues.add(getString(R.string.oa_x_games_TO_y) + " " + iVal);
                }
                SelectObjectToggle<Integer> otNrOfPointsToWinGame = new SelectObjectToggle<Integer>(context, lValues, lDisplayValues);
                otNrOfPointsToWinGame.setSelected(iPreferredNrToWin);
                otNrOfPointsToWinGame.setTag(PreferenceKeys.numberOfPointsToWinGame);
                llBestOf.addView(otNrOfPointsToWinGame, lpCC);
            }
            lControls.add(llBestOf);
        }

        if ( Brand.isGameSetMatch() )
        {
            LinearLayout llGSM = new LinearLayout(context);
            llGSM.setOrientation(LinearLayout.HORIZONTAL);
            {
                String[] fsfDescriptions = context.getResources().getStringArray(R.array.finalSetFinishDisplayValues);
                List<String> lValues = new ArrayList<>();
                for(String sDescription: fsfDescriptions ) {
                    lValues.add(getString(R.string.pref_finalSetFinish) + ": " + sDescription);
                }
                SelectObjectToggle<String> tbBestOf_or_TotalOf = new SelectObjectToggle<String>(context, lValues);
                tbBestOf_or_TotalOf.setTag(PreferenceKeys.finalSetFinish);
                GSMModel gsmModel = (GSMModel) matchModel;
                FinalSetFinish finalSetFinish = gsmModel.getFinalSetFinish();
                tbBestOf_or_TotalOf.setSelectedIndex(finalSetFinish.ordinal());

                llGSM.addView(tbBestOf_or_TotalOf, lpCC);
            }
            lControls.add(llGSM);
        }
        storeAndShowNext(1);

        adb.setView(m_rootView);
        dialog = adb.show();

        dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
    }

    private GridLayout.LayoutParams getGridLayoutParams() {
        GridLayout.LayoutParams lpGrid = new GridLayout.LayoutParams();
        lpGrid.height = m_buttonSizePx;
        lpGrid.width  = m_buttonSizePx;
        lpGrid.setGravity(Gravity.CENTER);
        lpGrid.rowSpec = GridLayout.spec(0/*, 1, 1.0f*/);
        return lpGrid;
    }

    private int  m_iStep   = -1;
    private void storeAndShowNext(int iStep) {
        if ( m_iStep != -1 ) {
            View current = lControls.get(m_iStep);
            if ( current != null ) {
                storeValuesForView(current);
                m_rootView.removeView(current);
            }
        }
        // show next/prev control
        m_iStep+=iStep;
        if ( m_iStep >= 0 && m_iStep < lControls.size() ) {
            View newView = lControls.get(m_iStep);
            m_rootView.addView(newView, 0);
            if ( newView instanceof TextView ) {
                TextView txtView = (TextView) newView;
                txtView.requestFocus();
            }

            if ( m_iStep == lControls.size() - 1 ) {
                // last control: change NEXT into DONE
                m_fabNext.setDrawable(context.getDrawable(R.drawable.checkmark_ok), m_buttonSizePx);
            } else if ( iStep == -1 ) {
                m_fabNext.setDrawable(context.getDrawable(R.drawable.arrow_right ), m_buttonSizePx);
            }
            if ( m_iStep == 0 ) {
                // first control: disable BACK
                m_fabPrev.setVisibility(View.INVISIBLE);
            } else {
                m_fabPrev.setVisibility(View.VISIBLE);
            }
        } else {
            // Done
            String sJson = m_tmp.toJsonString(null);
            Log.d(TAG, "Match : " + sJson);
            Model m = Brand.getModel();
            m.fromJsonString(sJson);
            m.registerListeners(ScoreBoard.matchModel);
            ScoreBoard.matchModel = m;
            scoreBoard.initScoreBoard(null);
            dismiss();
        }
    }

    private void storeValuesForView(View current) {
        if ( current instanceof ViewGroup ) {
            ViewGroup vg = (ViewGroup) current;
            int iChilds = vg.getChildCount();
            for(int i=0; i < iChilds; i++) {
                View v = vg.getChildAt(i);
                storeValuesForView(v);
            }
        } else {
            // store value of current in tmpmodel
            Object tag = current.getTag();
            if ( tag == null ) { return; }

            if ( tag.equals(Player.A) || tag.equals(Player.B) ) {
                TextView txt = (TextView) current;
                String sName = txt.getText().toString();
                       sName = sName.replaceFirst("[&\\+]", "/"); // allow usage of + or & to specify 2 players
                m_tmp.setPlayerName((Player) tag, sName);
            }
            if ( tag instanceof PreferenceKeys ) {
                PreferenceKeys eTag = (PreferenceKeys) tag;
                switch (eTag) {
                    case numberOfGamesToWinMatch: {
                        SelectObjectToggle<Integer> tb = (SelectObjectToggle<Integer>) current;
                        int iNrOfGamesToWin = tb.getSelected();
                        m_tmp.setNrOfGamesToWinMatch(iNrOfGamesToWin);
                        break;
                    }
                    case playAllGames: {
                        SelectObjectToggle<String> tb = (SelectObjectToggle<String>) current;
                        boolean bPlayAll = tb.getSelectedIndex() == 1;
                        m_tmp.setPlayAllGames(bPlayAll);
                        break;
                    }
                    case numberOfPointsToWinGame: {
                        SelectObjectToggle<Integer> tb = (SelectObjectToggle<Integer>) current;
                        m_tmp.setNrOfPointsToWinGame(tb.getSelected());
                        break;
                    }
                    case finalSetFinish: {
                        SelectObjectToggle<String> tb = (SelectObjectToggle<String>) current;
                        FinalSetFinish finalSetFinish = FinalSetFinish.values()[tb.getSelectedIndex()];
                        ((GSMModel) m_tmp).setFinalSetFinish(finalSetFinish);
                        break;
                    }
                }
            }
        }
    }

    private final TextView.OnEditorActionListener onEditorActionListener = new TextView.OnEditorActionListener() {
        @Override public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            //dialog.setCancelable(false);
            Log.d(TAG, String.format("action id %d, keyEvent %s:", actionId, event));
            if ( actionId == EditorInfo.IME_ACTION_DONE ) {
                // done was clicked in popup editor
            }
            return false;
        }
    };

    private FloatingActionButton m_fabNext = null;
    private FloatingActionButton m_fabPrev = null;
    private int m_buttonSizePx = 0;

    private List<View>           lControls = new ArrayList<View>();
    private Model                m_tmp = ModelFactory.getTemp();

    public final static int BTN_NEXT  = DialogInterface.BUTTON_POSITIVE;
    public final static int BTN_PREV  = DialogInterface.BUTTON_NEGATIVE;

    @Override public void handleButtonClick(int which) {
        switch ( which ) {
            case BTN_PREV:
                storeAndShowNext(-1);
                break;
            case BTN_NEXT:
                storeAndShowNext(1);
                break;
        }
        // there is only the 'Next' button
        //super.handleButtonClick(which);
    }
}
