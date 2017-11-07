package com.doubleyellow.scoreboard.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import com.doubleyellow.scoreboard.main.*;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.model.Player;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;

public class ChangeSides extends BaseAlertDialog
{
    public ChangeSides(Context context, Model matchModel, ScoreBoard scoreBoard) {
        super(context, matchModel, scoreBoard);
    }

    @Override public boolean storeState(Bundle outState) {
        outState.putSerializable(Player.class.getSimpleName(), leadingPlayer);
        return true;
    }

    @Override public boolean init(Bundle outState) {
        init((Player) outState.getSerializable(Player.class.getSimpleName()));
        return true;
    }

    private Player leadingPlayer = null;

    @Override public void show() {

        String sTitle = getOAString(R.string.oa_change_sides);
        adb.setTitle(sTitle)
           .setIcon   (R.drawable.microphone)
           .setMessage(getString(R.string.sb_swap_players) + "?")
           .setPositiveButton(R.string.cmd_yes, listener)
           .setNegativeButton(R.string.cmd_no , listener);
        dialog = adb.show();
    }
    private String getOAString(int iResId) {
        return PreferenceValues.getOAString(context, iResId );
    }

    public void init(Player leadingPlayer) {
        this.leadingPlayer = leadingPlayer;
    }

    private DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
        @Override public void onClick(DialogInterface dialog, int which) {
            handleButtonClick(which);
        }
    };

    public static final int BTN_SWAP_PLAYERS        = DialogInterface.BUTTON_POSITIVE;
    public static final int BTN_DO_NOT_SWAP_PLAYERS = DialogInterface.BUTTON_NEGATIVE;
    @Override public void handleButtonClick(int which) {
        switch (which) {
            case BTN_SWAP_PLAYERS :
                scoreBoard.handleMenuItem(R.id.sb_swap_players);
                break;
            case BTN_DO_NOT_SWAP_PLAYERS   :
                break;
        }
    }
}
