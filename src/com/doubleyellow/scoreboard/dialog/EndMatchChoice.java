package com.doubleyellow.scoreboard.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.model.EndMatchManuallyBecause;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.model.Player;
import com.doubleyellow.android.view.SelectEnumView;

/**
 * Dialog that is shown when 'end of a match' menu/button is chosen.
 */
public class EndMatchChoice extends BaseAlertDialog
{
    public EndMatchChoice(Context context, Model matchModel, ScoreBoard scoreBoard) {
        super(context, matchModel, scoreBoard);
    }

    @Override public boolean storeState(Bundle outState) {
        return true;
    }

    @Override public boolean init(Bundle outState) {
        return true;
    }

    @Override public void show() {

        LayoutInflater myLayout = LayoutInflater.from(context);
        ViewGroup ll = (ViewGroup) myLayout.inflate(R.layout.endmatch_reason, null);
        evReason = (SelectEnumView<EndMatchManuallyBecause>) ll.findViewById(R.id.evReason);

        dialog = adb
                .setTitle(R.string.end_match)
                .setView(ll)
                .setPositiveButton(matchModel.getName(Player.A), dialogClickListener)
                .setNeutralButton (matchModel.getName(Player.B), dialogClickListener)
                .setNegativeButton(R.string.cmd_cancel, dialogClickListener)
                // install onkeylistener to ensure endMatchDialogEnded event is triggered if user clicks back button on dialog
                .setOnKeyListener(getOnBackKeyListener(BUTTON_CANCEL))
                .show();
    }

    private SelectEnumView<EndMatchManuallyBecause> evReason = null;

    private DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
        @Override public void onClick(DialogInterface dialog, int which) {
            handleButtonClick(which);
        }
    };

    public static final int BUTTON_WINNER_A = DialogInterface.BUTTON_POSITIVE;
    public static final int BUTTON_WINNER_B = DialogInterface.BUTTON_NEUTRAL;
    public static final int BUTTON_CANCEL   = DialogInterface.BUTTON_NEGATIVE;
    @Override public void handleButtonClick(int which) {
        Player player = null;
        switch (which) {
            case BUTTON_WINNER_A:
                player = Player.A;
                break;
            case BUTTON_CANCEL :
                break;
            case BUTTON_WINNER_B:
                player = Player.B;
                break;
        }
        if ( player != null ) {
            EndMatchManuallyBecause endMatchManuallyBecause = evReason.getChecked();
            matchModel.endMatch(endMatchManuallyBecause, player);
            scoreBoard.persist(false);
        }
        //before triggering an event that might open another dialog, dismiss this one
        dialog.dismiss();

        scoreBoard.triggerEvent(ScoreBoard.SBEvent.endMatchDialogEnded, this);
    }
}
