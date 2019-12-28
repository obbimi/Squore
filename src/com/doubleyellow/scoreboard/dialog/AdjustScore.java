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
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.doubleyellow.android.util.ColorUtil;
import com.doubleyellow.android.view.ViewUtil;
import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.model.Player;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.prefs.ColorPrefs;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.scoreboard.view.GameHistoryView;
import com.doubleyellow.util.ListUtil;
import com.doubleyellow.util.StringUtil;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Dialog that allows the user to modify the scoring of the match in progress.
 * Scoring can be adapted for all games to be played allowing e.g.
 * - to start reffing a match that was already in progress
 * - just enter a score halfway or at the end of each game just to have the score noted down and possible shared for live scoring
 */
public class AdjustScore extends BaseAlertDialog {

    private static final String TAG = "SB." + AdjustScore.class.getSimpleName();

    public AdjustScore(Context context, Model matchModel, ScoreBoard scoreBoard) {
        super(context, matchModel, scoreBoard);
    }

    @Override public boolean storeState(Bundle outState) {
        return true;
    }

    @Override public boolean init(Bundle outState) {
        return true;
    }

    private LinkedHashMap<Integer, Boolean>  m_lModified;
    private LinkedHashMap<Integer, EditText> m_lTexts;
    private int                              m_iNrOfGames = -1;

    @Override public void show() {
        Map<ColorPrefs.ColorTarget, Integer> mColors = ColorPrefs.getTarget2colorMapping(context);
        int iMainBgColor   = mColors.get(ColorPrefs.ColorTarget.middlest);
        int iLabelTxt      = mColors.get(ColorPrefs.ColorTarget.darkest);
        int iSelectTxt     = mColors.get(ColorPrefs.ColorTarget.darkest);
        int iInputBgColor  = mColors.get(ColorPrefs.ColorTarget.middlest);
        int iInputTxtColor = mColors.get(ColorPrefs.ColorTarget.lightest);

        ScrollView   sv = new ScrollView(context);
        LinearLayout ll = new LinearLayout(context);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setPadding(15, 0, 0, 0);
        sv.addView(ll);

        TextView txtMessage = new TextView(context);
        txtMessage.setText(matchModel.getName(Player.A) + " - " + matchModel.getName(Player.B));
        ll.addView(txtMessage);

        ColorUtil.setBackground(ll, iMainBgColor);

        boolean bOrientationIsPortrait = ViewUtil.isPortraitOrientation(context);
        int iGamesPerRow = bOrientationIsPortrait ? 1 : 2 ;

        m_lTexts    = new LinkedHashMap<Integer, EditText>();
        m_lModified = new LinkedHashMap<Integer, Boolean>();
        if ( Brand.isRacketlon() ) {
            m_iNrOfGames = 4;
        } else {
            m_iNrOfGames = matchModel.getNrOfGamesToWinMatch() * 2 - 1;
        }

        LinearLayout llGameScore = null;
        for ( int s=1; s <= m_iNrOfGames; s++ ) {

            // check if new linear layout should be used
            if ( s % iGamesPerRow == 0 || llGameScore==null ) {
                llGameScore = new LinearLayout(context);
                llGameScore.setOrientation(LinearLayout.HORIZONTAL);
                ll.addView(llGameScore);
            }

            TextView lbl = new TextView(context);
            lbl.setText(getString(R.string.Game) + " " + s + " ");
            lbl.setTextColor(iLabelTxt);
            llGameScore.addView(lbl);

            ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            for( Player p: Model.getPlayers() ) {
                // insert a label between 2 scores of a single game
                if ( p.equals(Player.B) ) {
                    lbl = new TextView(context);
                    lbl.setText(" - ");
                    llGameScore.addView(lbl);
                }

                EditText txtPoints = new EditText(context);
                int id = s * 100 + p.ordinal();
                int nextFocusId = p.equals(Player.A) ? s * 100 + Player.B.ordinal() : (s + 1) * 100 + Player.A.ordinal();
                txtPoints.setNextFocusDownId(nextFocusId);
                txtPoints.setId(id);
                txtPoints.setInputType(/*InputType.TYPE_CLASS_TEXT |*/ InputType.TYPE_CLASS_NUMBER);
                txtPoints.setLayoutParams(layoutParams);
                txtPoints.setSingleLine();
                txtPoints.setMinWidth(60);
                //n.setPadding(5, 5, 0, 0);
                txtPoints.setSelectAllOnFocus(true);
                txtPoints.setOnKeyListener(onKeyListener);
                txtPoints.setOnFocusChangeListener(onFocusChangeListener);
                ColorUtil.setBackground(txtPoints, iInputBgColor);
                txtPoints.setTextColor(iInputTxtColor);
                txtPoints.setHighlightColor(iSelectTxt);

                // give a default value
                if ( matchModel.hasStarted() ) {
                    List<Map<Player, Integer>> gameCountHistory = matchModel.getGameScoresIncludingInProgress();
                    if ( ListUtil.size(gameCountHistory) > s - 1 ) {
                        txtPoints.setText("" + gameCountHistory.get(s-1).get(p));
                    } else {
                        txtPoints.setText("0");
                    }
                } else {
                    txtPoints.setText("0");
/*
                    // dummy content for easy testing
                    if ( (s + p.ordinal()) %2==0 ) {
                        n.setText("" + matchModel.getNrOfPointsToWinGame());
                    } else {
                        n.setText("" + (s * 2 + p.ordinal()) );
                    }
*/
                }
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                params.leftMargin = 3;
                params.topMargin  = 3;
                llGameScore.addView(txtPoints, params);

                // for when user is finished
                m_lTexts   .put(id, txtPoints);
                m_lModified.put(id, false);
            }
        }

        adb.setTitle(R.string.sb_adjust_score)
           //.setMessage(matchModel.getName(Player.A) + " - " + matchModel.getName(Player.B))
           .setView(sv)
           .setIcon(android.R.drawable.ic_menu_edit)
           .setPositiveButton(R.string.cmd_ok    , dialogClickListener)
           .setNegativeButton(R.string.cmd_cancel, dialogClickListener);
        dialog = adb.show(new FocusChooser());

        // try showing the keyboard by default (seems not to work in landscape due to lack of space on screen?)
        ViewUtil.showKeyboard(dialog);
    }

    private View.OnFocusChangeListener onFocusChangeListener = new View.OnFocusChangeListener() {
        private int m_lastLoosingFocus = 0;

        @Override public void onFocusChange(View v, boolean hasFocus) {
            Log.d(TAG, "Focus changed for view with id " + v.getId() + " hasFocus:" + hasFocus);
            if ( hasFocus == false ) {
                m_lastLoosingFocus = v.getId();
                return;
            }

            // receiving focus
            if ( v instanceof EditText ) {
                EditText tvFocus = (EditText) v;
                int iGameNr_Zero_0or1 = tvFocus.getId();
                int i0or1 = iGameNr_Zero_0or1 % 100;
                int iGameNr1Based = (iGameNr_Zero_0or1 - i0or1) / 100;

                ViewParent parentView = v.getParent();
                if ( parentView instanceof ViewGroup ) {
                    ViewGroup vg = (ViewGroup) parentView;
                    int iOtherViewId = iGameNr1Based * 100 + (1 - i0or1);
                    if ( iOtherViewId == m_lastLoosingFocus ) {
                        TextView tvOther = vg.findViewById(iOtherViewId);

                        String scoreFocus = tvFocus.getText().toString();
                        String scoreOther = tvOther.getText().toString();
                        if (  ( StringUtil.isEmpty (scoreFocus) || scoreFocus.equals("0"))
                                && StringUtil.isNotEmpty(scoreOther)
                                && StringUtil.isInteger (scoreOther)
                        ) {
                            int iScoreOther = Integer.parseInt(scoreOther);

                            int iNrOfPointsToWinGame = matchModel.getNrOfPointsToWinGame();
                            int iFocusValue = 0;
                            if ( iScoreOther < iNrOfPointsToWinGame ) {
                                // assume focused player won game
                                iFocusValue = Math.max(iNrOfPointsToWinGame, iScoreOther + 2);
                            } else {
                                // assume other won game
                                iFocusValue = Math.min(iNrOfPointsToWinGame, iScoreOther - 2) ;
                            }
                            tvFocus.setText(String.valueOf(iFocusValue));
                            tvFocus.selectAll();
                        }
                    }
                }
            }
        }
    };
    private View.OnKeyListener onKeyListener = new View.OnKeyListener() {
        @Override public boolean onKey(View view, int i, KeyEvent keyEvent) {
            m_lModified.put(view.getId(), true);
            if ( keyEvent.getKeyCode() != KeyEvent.KEYCODE_BACK ) {
                dialog.setCancelable(false); // do not close the dialog for the back key (prevent user losing score he entered). 'Really not using the score' can be done by pressing 'Cancel'
            }
            return false;
        }
    };

    private class FocusChooser implements DialogInterface.OnShowListener {
        @Override public void onShow(DialogInterface dialogInterface) {
            // set focus on first empty field or field with a zero
            for(EditText txt : m_lTexts.values() ) {
                if ( txt.getText() == null || StringUtil.isEmpty(txt.getText()) || txt.getText().toString().equals("0") ) {
                    txt.requestFocus();
                    break;
                }
            }
            triggerButtonLayoutAPI28(dialogInterface, DialogInterface.BUTTON_NEUTRAL);
        }
    }

    private DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
        @Override public void onClick(DialogInterface dialog, int which) {
            handleButtonClick(which);
        }
    };

    @Override public void handleButtonClick(int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                //initScoreBoard(null); // allow modifying when score is already in progress
                for ( int s=1; s <= m_iNrOfGames; s++ ) {
                    int id = s * 100 + Player.A.ordinal();
                    if ( m_lModified.get(id) ==false && m_lModified.get(id+1) == false ) { continue; }

                    String sA = m_lTexts.get(id    ).getText().toString();
                    String sB = m_lTexts.get(id + 1).getText().toString();
                    if ( StringUtil.hasEmpty(sA, sB) ) { continue; }

                    int iPointsA = 0;
                    int iPointsB = 0;
                    try {
                        iPointsA = Integer.parseInt(sA);
                        iPointsB = Integer.parseInt(sB);
                    } catch (NumberFormatException e) {
                    }
                    if ( iPointsA + iPointsB == 0 ) { continue; }
                    int iNrOfPointsToWinGame = PreferenceValues.numberOfPointsToWinGame(context);
                    int iUpper = Math.max(iNrOfPointsToWinGame * 6, 60);
                    iUpper = iUpper - (iUpper % 10); // rounding down to closes multiple of 10
                    if ( iPointsA + iPointsB > iUpper) {
                        // to prevent causing 'OutOfMemory' exceptions to easily by someone just testing the app
                        Toast.makeText(context, "This does not look like a realistic score for game " + s + ". Skipping score (" + iUpper + ")", Toast.LENGTH_LONG).show();
                        break;
                    }
                    GameHistoryView.dontShowForToManyPoints(iPointsA + iPointsB);
                    matchModel.setGameScore_Json(s-1, iPointsA, iPointsB, 0);
                }
                scoreBoard.persist(false);

                // if last game entered was also a victory for someone: end the game
                if ( matchModel.isPossibleGameVictory() ) {
                    scoreBoard.endGame();
                }
                ScoreBoard.sendMatchToOtherBluetoothDevice(context, true);
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                // Do nothing.
                break;
        }
    }
}
