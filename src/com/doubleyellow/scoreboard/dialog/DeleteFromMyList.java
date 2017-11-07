package com.doubleyellow.scoreboard.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.LinearLayout;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.model.Player;
import com.doubleyellow.scoreboard.prefs.PreferenceKeys;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;

/**
 * Dialog to suggest to the user to 'Delete' the match from my list
 */
public class DeleteFromMyList extends BaseAlertDialog
{
    public DeleteFromMyList(Context context, Model matchModel, ScoreBoard scoreBoard) {
        super(context, matchModel, scoreBoard);
    }

    @Override public boolean storeState(Bundle outState) {
        return true;
    }

    @Override public boolean init(Bundle outState) {
        return true;
    }
    @Override public void show() {

        LinearLayout ll = new LinearLayout(context);
        ll.setOrientation(LinearLayout.VERTICAL);

        String sTitle = String.format(this.getString(R.string.sb_remove_match_from_my_list), matchModel.getName(Player.A, true, true) + " - " + matchModel.getName(Player.B, true, true));

        adb.setIcon   (R.drawable.ic_action_close)
           //.setTitle(sTitle)
           .setMessage(sTitle)
           .setPositiveButton(R.string.cmd_delete, dialogClickListener)
           .setNegativeButton(R.string.cmd_keep  , dialogClickListener)
           .setOnKeyListener(getOnBackKeyListener())
           .setView(ll);

        // in a try catch to prevent crashing if somehow scoreBoard is not showing any longer
        try {
            dialog = adb.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
        @Override public void onClick(DialogInterface dialog, int which) {
            handleButtonClick(which);
        }
    };

    public static final int DELETE_MATCH = DialogInterface.BUTTON_POSITIVE;
    public static final int KEEP_MATCH   = DialogInterface.BUTTON_NEGATIVE;

    @Override public void handleButtonClick(int which) {
        switch (which){
            case DELETE_MATCH:
                deleteMatchFromMyList(context, matchModel);
                break;
            case KEEP_MATCH:
                break;
        }
        scoreBoard.triggerEvent(ScoreBoard.SBEvent.dialogClosed, this);
    }

    public static void deleteMatchFromMyList(Context context, Model matchModel) {
        String sRegExp = String.format("\\Q%s\\E\\s*\\-\\s*\\Q%s\\E", matchModel.getName(Player.A, true, true), matchModel.getName(Player.B, true, true));
        PreferenceValues.removeStringFromList(context, PreferenceKeys.matchList, sRegExp);
    }
}
