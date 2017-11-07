package com.doubleyellow.scoreboard.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.prefs.PreferenceKeys;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.util.Feature;
import com.doubleyellow.util.ListUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Dialog that is shown when 'end of a game' menu/button is chosen while the score is not a valid 'enf of game' score.
 */
public class EndGameChoice extends BaseAlertDialog
{
    public EndGameChoice(Context context, Model matchModel, ScoreBoard scoreBoard) {
        super(context, matchModel, scoreBoard);
    }

    @Override public boolean storeState(Bundle outState) {
        return true;
    }

    @Override public boolean init(Bundle outState) {
        return true;
    }
    @Override public void show() {
        //int iEndGameMsgResId = R.string.sb_start_next_game_confirm_message;
        adb.setPositiveButton(getGameOrSetString(R.string.end_game_Squash), dialogClickListener)
           .setNegativeButton(R.string.cmd_cancel                  , dialogClickListener)
           .setIcon(R.drawable.microphone);
        List<String> messages = new ArrayList<>();
        messages.add(getGameOrSetString(R.string.sb_not_a_valid_endscore));
        if ( Brand.isSquash() ) {
            messages.add(getGameOrSetString(R.string.sb_did_you_mean_conduct_game));
        }
        adb.setTitle(messages.remove(0))
           .setMessage(ListUtil.join(messages, "\n\n"))
           .setOnKeyListener(getOnBackKeyListener());

        dialog = adb.show();
    }

    private DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
        @Override public void onClick(DialogInterface dialog, int which) {
            handleButtonClick(which);
        }
    };

    public static final int BTN_END_GAME      = DialogInterface.BUTTON_POSITIVE;
    public static final int BTN_CANCEL        = DialogInterface.BUTTON_NEGATIVE;
    @Override public void handleButtonClick(int which) {
        dialog.dismiss();
        switch ( which ) {
            case BTN_END_GAME:
                PreferenceValues.setOverwrite(PreferenceKeys.endGameSuggestion, Feature.DoNotUse.toString());
                matchModel.endGame();
                PreferenceValues.removeOverwrite(PreferenceKeys.endGameSuggestion);
                break;
            case BTN_CANCEL:
                break;
        }
        scoreBoard.triggerEvent(ScoreBoard.SBEvent.endGameDialogEnded, this);
    }
}
