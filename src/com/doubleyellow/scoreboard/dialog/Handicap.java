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
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.doubleyellow.android.util.ColorUtil;
import com.doubleyellow.android.view.ViewUtil;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.model.HandicapFormat;
import com.doubleyellow.scoreboard.model.JSONKey;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.model.Player;
import com.doubleyellow.scoreboard.prefs.ColorPrefs;
import com.doubleyellow.scoreboard.prefs.PreferenceKeys;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.util.StringUtil;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Dialog that allows the user to specify a handicap, either per game or for each game.
 */
public class Handicap extends BaseAlertDialog {

    private final LinearLayout.LayoutParams params;

    public Handicap(Context context, Model matchModel, ScoreBoard scoreBoard) {
        super(context, matchModel, scoreBoard);

        params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.leftMargin = 3;
        params.topMargin  = 3;
    }

    @Override public boolean storeState(Bundle outState) {
        outState.putSerializable(HandicapFormat.class.getSimpleName(), handicapFormat);
        outState.putInt(PreferenceKeys.numberOfPointsToWinGame.toString(), iNrOfPointsToWinGame);
        return true;
    }

    @Override public boolean init(Bundle outState) {
        init((HandicapFormat) outState.getSerializable(HandicapFormat.class.getSimpleName()), outState.getInt(PreferenceKeys.numberOfPointsToWinGame.toString()));
        return true;
    }

    private LinkedHashMap<Integer, EditText> lTexts;
    private int iTotalNrOfPreviousGames = 0;
    private HandicapFormat handicapFormat = null;
    private int iNrOfPointsToWinGame = 11;

    public void init(HandicapFormat handicapFormat, int iNrOfPointsToWinGame) {
        this.handicapFormat       = handicapFormat;
        this.iNrOfPointsToWinGame = iNrOfPointsToWinGame;
    }

    @Override public void show() {
        // later we make the score buttons the same color as the score buttons on the main board
        // and the default background color of the dialog is black. Therefor dark 'score buttons' are hard to see
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
        int iGamesPerRow = 1;
        if (bOrientationIsPortrait) {
            iGamesPerRow = 1;
        } else {
            iGamesPerRow = 2;
        }

        lTexts = new LinkedHashMap<Integer, EditText>();
        iTotalNrOfPreviousGames = matchModel.getNrOfFinishedGames();

        LinearLayout llGameScore = null;
        // first add the non-editable values of previous games
        for ( int iPrevGame=0; iPrevGame < iTotalNrOfPreviousGames; iPrevGame++ ) {

            // check if new linear layout should be used
            if ( iPrevGame % iGamesPerRow == 0 || llGameScore==null ) {
                llGameScore = new LinearLayout(context);
                llGameScore.setOrientation(LinearLayout.HORIZONTAL);
                ll.addView(llGameScore);
            }

            TextView lbl = new TextView(context);
            lbl.setText(getString(R.string.Game) + " " + (iPrevGame+1) + " ");
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

                TextView n = new EditText(context); // we use a disabled EditText to have a consistent view
                n.setEnabled(false);
                n.setLayoutParams(layoutParams);
                n.setMinWidth(60);
                //n.setPadding(5, 5, 0, 0);
                ColorUtil.setBackground(n, iSelectTxt);
                n.setTextColor(iInputTxtColor);

                n.setText("" + matchModel.getGameStartScoreOffset(p, iPrevGame));

                llGameScore.addView(n, params);
            }
        }

        if ( iTotalNrOfPreviousGames % iGamesPerRow == 0 || llGameScore==null ) {
            llGameScore = new LinearLayout(context);
            llGameScore.setOrientation(LinearLayout.HORIZONTAL);
            ll.addView(llGameScore);
        }
        TextView lbl = new TextView(context);
        if ( handicapFormat.equals(HandicapFormat.DifferentForAllGames) ) {
            lbl.setText(getString(R.string.Game) + " " + (iTotalNrOfPreviousGames + 1) + " ");
        } else {
            lbl.setText(R.string.lbl_all_games);
        }
        lbl.setTextColor(iLabelTxt);
        llGameScore.addView(lbl);
        // add offset for game about to start
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        for( Player p: Model.getPlayers() ) {
            // insert a label between 2 scores of a single game
            if ( p.equals(Player.B) ) {
                lbl = new TextView(context);
                lbl.setText(" - ");
                llGameScore.addView(lbl);
            }

            EditText n = new EditText(context);
            int id = iTotalNrOfPreviousGames * 100 + p.ordinal();
            int nextFocusId = p.equals(Player.A) ? iTotalNrOfPreviousGames * 100 + Player.B.ordinal() : (iTotalNrOfPreviousGames + 1) * 100 + Player.A.ordinal();
            n.setNextFocusDownId(nextFocusId);
            n.setId(id);
            n.setInputType(InputType.TYPE_NUMBER_FLAG_SIGNED | InputType.TYPE_CLASS_NUMBER);
            n.setLayoutParams(layoutParams);
            n.setSingleLine();
            n.setMinWidth(60);
            //n.setPadding(5, 5, 0, 0);
            n.setSelectAllOnFocus(true);
            ColorUtil.setBackground(n, iInputBgColor);
            n.setHighlightColor(iSelectTxt);
            n.setTextColor(iInputTxtColor);

            // give
            n.setText("" + matchModel.getGameStartScoreOffset(p, iTotalNrOfPreviousGames));
            llGameScore.addView(n, params);

            // for when user is finished
            lTexts.put(id, n);
        }

        adb.setTitle(getString(R.string.sb_handicap_score))
         //.setMessage(matchModel.getName(Player.A) + " - " + matchModel.getName(Player.B))
           .setView(sv)
           .setIcon(android.R.drawable.ic_menu_edit)
           .setPositiveButton(R.string.cmd_ok    , dialogClickListener)
           .setNeutralButton(R.string.cmd_cancel, dialogClickListener);
        if ( this.handicapFormat.equals(HandicapFormat.DifferentForAllGames) && (iTotalNrOfPreviousGames==0) ) {
            // maybe referee only now realised he still had 'handicap' selected, allow him to cancel the usage of handicap
            adb.setNegativeButton(R.string.no_handicap, dialogClickListener);
        }
        dialog = adb.show(onShowListener);

        // try showing the keyboard by default (seems not to work in landscape due to lack of space on screen?)
        ViewUtil.showKeyboard(dialog);
    }

    private DialogInterface.OnShowListener onShowListener = new DialogInterface.OnShowListener() {
        @Override public void onShow(DialogInterface dialogInterface) {
            // set focus on last
            int iIdLast = iTotalNrOfPreviousGames * 100 + Player.A.ordinal();
            EditText e = lTexts.get(iIdLast);
            if ( e != null ) {
                //setBackground(e, mColors.get(ColorPrefs.ColorTarget.lightest));
                e.requestFocus();
            }

            final Button btnOK = dialog.getButton(CHANGE_HANDICAP);
            btnOK.setOnClickListener(onChangeHandicapListener);
        }
    };

    private DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
        @Override public void onClick(DialogInterface dialog, int which) {
            handleButtonClick(which);
        }
    };
    private View.OnClickListener onChangeHandicapListener = new View.OnClickListener() {
        @Override public void onClick(View view) {
            handleButtonClick(CHANGE_HANDICAP);
        }
    };

    public static final int CHANGE_HANDICAP = DialogInterface.BUTTON_POSITIVE;

    @Override public void handleButtonClick(int which) {
        int iGameAboutToStart = matchModel.getNrOfFinishedGames();

        switch (which) {
            case CHANGE_HANDICAP:
                int id = iGameAboutToStart * 100 + Player.A.ordinal();
                for( Player p: Model.getPlayers() ) {
                    EditText txtOffset = lTexts.get(id + p.ordinal());
                    if ( txtOffset == null ) { continue; }
                    String sB = txtOffset.getText().toString();
                    int iOffset = 0;
                    if ( StringUtil.isNotEmpty(sB) ) {
                        try {
                            iOffset = Integer.parseInt(sB);
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                    }
                    if ( iOffset >= iNrOfPointsToWinGame ) {
                        Toast.makeText(context, String.format("Handicap 'head start' must be smaller than %s", iNrOfPointsToWinGame), Toast.LENGTH_LONG).show();
                        return;
                    }
                    // player can not start with more points than required to win a game
                    iOffset = Math.min(iOffset, iNrOfPointsToWinGame-1);
                    matchModel.setGameStartScoreOffset(p, iOffset);
                }

                scoreBoard.persist(false);

                break;
            case DialogInterface.BUTTON_NEUTRAL:
                // Do nothing.
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                // do not use handicaps for this match
                this.handicapFormat = HandicapFormat.None;
                PreferenceValues.setEnum(PreferenceKeys.handicapFormat, context, this.handicapFormat);
                matchModel.setHandicapFormat(handicapFormat);
                break;
        }

        //before triggering an event that might open another dialog, dismiss this one
        dialog.dismiss();

        showNextDialog();
    }
}
