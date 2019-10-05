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
import android.graphics.Color;
import android.os.Bundle;

import android.widget.LinearLayout;
import android.widget.TextView;
import com.doubleyellow.android.util.ColorUtil;
import com.doubleyellow.android.view.ViewUtil;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.model.Call;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.model.Player;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.prefs.ColorPrefs;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.scoreboard.prefs.ShowPlayerColorOn;
import com.doubleyellow.util.Direction;
import com.doubleyellow.util.StringUtil;

import java.util.EnumSet;

/**
 * Dialog that is displayed when user name is clicked.
 * It assumes that player made an appeal for 'Let'.
 * This dialog allows the ref to choose either
 * - No Let
 * - Let
 * - Stroke
 *
 *  http://image.shutterstock.com/display_pic_with_logo/1816916/169120127/stock-vector-hand-gestures-icons-set-contour-flat-isolated-vector-illustration-169120127.jpg
 *  convert input.png -fuzz 5% -transparent white output.png
 */	
public class Appeal extends BaseAlertDialog
{
    public Appeal(Context context, Model matchModel, ScoreBoard scoreBoard) {
        super(context, matchModel, scoreBoard);
    }

    @Override public boolean storeState(Bundle outState) {
        outState.putSerializable(Player.class.getSimpleName(), appealingPlayer);
        return true;
    }

    @Override public boolean init(Bundle outState) {
        init((Player) outState.getSerializable(Player.class.getSimpleName()));
        return true;
    }

    private Player appealingPlayer = null;
    @Override public void show() {
        String sTitle = getString(R.string.oal_let_requested_by, matchModel.getName_no_nbsp(appealingPlayer, false));
        int iIconSize = PreferenceValues.getAppealHandGestureIconSize(context);

        adb.setTitle(sTitle)
           .setIcon(R.drawable.microphone)
           .setMessage(getOAString(R.string.oa_decision_colon));
        LinearLayout ll = new LinearLayout(context);

        int       iOrientation  = LinearLayout.VERTICAL;
        Direction dIconPosition = Direction.W;
        llpMargin1Weight1.width = LinearLayout.LayoutParams.MATCH_PARENT;
        int iMargin = Math.max(iIconSize / 10, llpMargin1Weight1.leftMargin); // margin between the 3 decision buttons
        llpMargin1Weight1.leftMargin   = iMargin;
        llpMargin1Weight1.rightMargin  = iMargin;
        llpMargin1Weight1.topMargin    = iMargin;
        llpMargin1Weight1.bottomMargin = iMargin;
        if ( ViewUtil.isLandscapeOrientation(context) ) {
            llpMargin1Weight1.width = LinearLayout.LayoutParams.WRAP_CONTENT;
            dIconPosition = Direction.N;
            iOrientation  = LinearLayout.HORIZONTAL;
        }
        ll.setOrientation(iOrientation);

        int iResId_Stroke = R.drawable.appeal_stroke_front_256; // R.drawable.appeal_stroke72;
        int iResId_YesLet = R.drawable.appeal_yeslet_front_256; // R.drawable.appeal_yeslet72;
        int iResId_NoLet  = R.drawable.appeal_nolet_flat_256  ; // R.drawable.appeal_nolet72 ;
        Integer iBG = ColorPrefs.getTarget2colorMapping(context).get(ColorPrefs.ColorTarget.backgroundColor);
        if ( iBG != null ) {
            // if we use a dark background ... switch to the light gesture icons
            int blackOrWhiteFor = ColorUtil.getBlackOrWhiteFor(iBG);
            if ( blackOrWhiteFor == Color.WHITE ) {
                iResId_Stroke = R.drawable.appeal_stroke_front_white;
                iResId_YesLet = R.drawable.appeal_yeslet_front_white;
                iResId_NoLet  = R.drawable.appeal_nolet_flat_white  ;
            }
        }

        final TextView vStroke = getActionView(getOAString(R.string.oa_stroke ), BTN_STROKE , iResId_Stroke, iIconSize, dIconPosition);
        final TextView vYesLet = getActionView(getOAString(R.string.oa_yes_let), BTN_YES_LET, iResId_YesLet, iIconSize, dIconPosition);
        final TextView vNoLet  = getActionView(getOAString(R.string.oa_no_let ), BTN_NO_LET , iResId_NoLet , iIconSize, dIconPosition);
        ll.addView(vStroke, llpMargin1Weight1);
        ll.addView(vYesLet, llpMargin1Weight1);
        ll.addView(vNoLet , llpMargin1Weight1);
        adb.setView(ll);

        ColorPrefs.setColor(ll);

        // if player colors are set, use it
        EnumSet<ShowPlayerColorOn> colorOns = PreferenceValues.showPlayerColorOn(context);
        if ( colorOns.contains(ShowPlayerColorOn.DecisionMessage) ) {
            if ( StringUtil.areAllNonEmpty(matchModel.getColor(Player.A), matchModel.getColor(Player.B)) ) {
                for( Player p: Model.getPlayers() ) {
                    final String sBgColor = matchModel.getColor(p);
                    int iBgColor  = Color.parseColor(sBgColor);
                    int iTxtColor = ColorUtil.getBlackOrWhiteFor(sBgColor);
                    if ( appealingPlayer.equals(p) ) {
                        vStroke.setBackgroundColor(iBgColor);
                        vStroke.setTextColor(iTxtColor);
                    } else {
                        vNoLet.setBackgroundColor(iBgColor);
                        vNoLet.setTextColor(iTxtColor);
                    }
                }
            }
        }
        dialog = adb.show();
    }

    private String getOAString(int iResId) {
        return PreferenceValues.getOAString(context, iResId );
    }

    public void init(Player appealingPlayer) {
        this.appealingPlayer = appealingPlayer;
    }

    public final static int BTN_STROKE  = DialogInterface.BUTTON_POSITIVE;
    public final static int BTN_YES_LET = DialogInterface.BUTTON_NEUTRAL;
    public final static int BTN_NO_LET  = DialogInterface.BUTTON_NEGATIVE;
    @Override public void handleButtonClick(int which) {
        Call call = null;
        switch (which) {
            case BTN_STROKE  : call = Call.ST; break;
            case BTN_YES_LET : call = Call.YL; break;
            case BTN_NO_LET  : call = Call.NL; break;
        }
        scoreBoard.recordAppealAndCall(appealingPlayer, call);
    }
}
