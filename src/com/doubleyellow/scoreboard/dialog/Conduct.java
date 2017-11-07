package com.doubleyellow.scoreboard.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import com.doubleyellow.scoreboard.main.*;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.model.Call;
import com.doubleyellow.scoreboard.model.ConductType;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.model.Player;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.view.SelectEnumView;

public class Conduct extends BaseAlertDialog
{
    public Conduct(Context context, Model matchModel, ScoreBoard scoreBoard) {
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
    private SelectEnumView<ConductType> sv;

    @Override public void show() {

        String sTitle = getString(R.string.oal_misconduct_by, matchModel.getName_no_nbsp(appealingPlayer, false));
        adb.setTitle(sTitle)
           .setIcon   (R.drawable.microphone)
           .setMessage(PreferenceValues.getOAString(context, R.string.oa_decision_colon));

        // show one or three buttons depending on where what stage of the match we are
        if ( true ) {
            adb.setPositiveButton(getOAString(Call.CW.getResourceIdLabel()), listener);
        }
        if ( matchModel.matchHasEnded() == false ) {
            adb.setNeutralButton (getOAString(Call.CS.getResourceIdLabel()), listener);
            adb.setNegativeButton(getOAString(Call.CG.getResourceIdLabel()), listener);
        }

        LayoutInflater myLayout = LayoutInflater.from(context);
        final View view = myLayout.inflate(R.layout.conduct, null);
        sv = (SelectEnumView<ConductType>) view.findViewById(R.id.selectConductType);

        // add a view with all possible Conducts and let user choose one
        //sv = new SelectEnumView(context, ConductType.class);
        dialog = adb.setView(view)
                    .show();
    }
    private String getOAString(int iResId) {
        return PreferenceValues.getOAString(context, iResId );
    }


    public void init(Player appealingPlayer) {
        this.appealingPlayer = appealingPlayer;
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
        ConductType conductType = sv.getChecked();
        Call call = null;
        switch (which) {
            case BTN_CONDUCT_STROKE : call = Call.CS; break;
            case BTN_CONDUCT_WARNING: call = Call.CW; break;
            case BTN_CONDUCT_GAME   : call = Call.CG; break;
        }
        matchModel.recordConduct(appealingPlayer, call, conductType);
    }
}
