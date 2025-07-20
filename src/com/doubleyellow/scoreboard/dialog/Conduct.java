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

import com.doubleyellow.android.view.EnumSpinner;
import com.doubleyellow.android.view.SelectEnumView;
import com.doubleyellow.scoreboard.main.*;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.model.Call;
import com.doubleyellow.scoreboard.model.ConductType;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.model.Player;
import com.doubleyellow.scoreboard.prefs.ColorPrefs;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;

import java.util.Map;
public class Conduct extends BaseAlertDialog
{
    public Conduct(Context context, Model matchModel, ScoreBoard scoreBoard) {
        super(context, matchModel, scoreBoard);
    }

    @Override public boolean storeState(Bundle outState) {
        outState.putSerializable(Player.class.getSimpleName(), missbehavingPlayer);
        return true;
    }

    @Override public boolean init(Bundle outState) {
        init((Player) outState.getSerializable(Player.class.getSimpleName()));
        return true;
    }

    private Player missbehavingPlayer = null;
    private SelectEnumView<ConductType> sv;
    private EnumSpinner<ConductType> enumSpinner;

    @Override public void show() {
        String name_no_nbsp = matchModel.getName_no_nbsp(missbehavingPlayer, false);
        String sTitle = getString(R.string.oal_misconduct_by, name_no_nbsp);
        if ( isNotWearable() ) {
            adb.setTitle(sTitle);
            adb.setIcon (R.drawable.microphone);
        }
        adb.setMessage(PreferenceValues.getOAString(context, R.string.oa_decision_colon));

        // show one or three buttons depending on where/what stage of the match we are
        if ( true ) {
            adb.setPositiveButton(getOAString(Call.CW.getResourceIdLabel()), listener);
        }
        if ( matchModel.matchHasEnded() == false ) {
            adb.setNeutralButton (getOAString(Call.CS.getResourceIdLabel()), listener);
            adb.setNegativeButton(getOAString(Call.CG.getResourceIdLabel()), listener);
        }

        LayoutInflater myLayout = LayoutInflater.from(context);
        final View view = myLayout.inflate(R.layout.conduct, null);
        sv          = view.findViewById(R.id.selectConductType);
        enumSpinner = view.findViewById(R.id.selectConductType_spinner);

        // radiobutton has dark edge... on dark background ... hard to see
        // set background color to 'middlest' so that dark circles and white text are visible
        Map<ColorPrefs.ColorTarget, Integer> target2colorMapping = ColorPrefs.getTarget2colorMapping(context);
        Integer color = target2colorMapping.get(ColorPrefs.ColorTarget.middlest);
        if ( sv          != null ) sv.setBackgroundColor(color);
        if ( enumSpinner != null ) enumSpinner.setBackgroundColor(color);

        // add a view with all possible Conducts and let user choose one
        //sv = new SelectEnumView(context, ConductType.class);
        dialog = adb.setView(view)
                    .show();
    }
    private String getOAString(int iResId) {
        return PreferenceValues.getOAString(context, iResId );
    }


    public void init(Player missbehavingPlayer) {
        this.missbehavingPlayer = missbehavingPlayer;
    }

    private DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
        @Override public void onClick(DialogInterface dialog, int which) {
            handleButtonClick(which);
        }
    };

    public static final int BTN_CONDUCT_WARNING = DialogInterface.BUTTON_POSITIVE;
    public static final int BTN_CONDUCT_STROKE  = DialogInterface.BUTTON_NEUTRAL ;
    public static final int BTN_CONDUCT_GAME    = DialogInterface.BUTTON_NEGATIVE;
    @Override public void handleButtonClick(int which) {
        ConductType conductType;
        if ( sv != null ) {
            conductType = sv.getChecked();
        } else if ( enumSpinner != null ) {
            conductType = enumSpinner.getSelectedEnum();
        } else {
            return;
        }
        Call call = null;
        switch (which) {
            case BTN_CONDUCT_STROKE : call = Call.CS; break;
            case BTN_CONDUCT_WARNING: call = Call.CW; break;
            case BTN_CONDUCT_GAME   : call = Call.CG; break;
        }
        scoreBoard.recordConduct(missbehavingPlayer, call, conductType);
    }
}
