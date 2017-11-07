package com.doubleyellow.scoreboard.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.match.Match;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.model.Player;

/**
 * Dialog that is shown if user wants to restart the score.
 */
public class RestartScore extends BaseAlertDialog
{
    public RestartScore(Context context, Model matchModel, ScoreBoard scoreBoard) {
        super(context, matchModel, scoreBoard);
    }

    @Override public boolean storeState(Bundle outState) {
        return true;
    }

    @Override public boolean init(Bundle outState) {
        return true;
    }
    @Override public void show() {

        adb.setPositiveButton(R.string.cmd_yes          , dialogClickListener)
           .setNeutralButton (R.string.cmd_change_format, dialogClickListener)
           .setNegativeButton(R.string.cmd_no           , dialogClickListener)
           .setIcon          (R.drawable.ic_action_refresh)
           .setMessage       (R.string.sb_clear_score_confirm_message)
           .setOnKeyListener(getOnBackKeyListener(BTN_NO_RESTART));
        dialog = adb.show();
    }

    private DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
        @Override public void onClick(DialogInterface dialog, int which) {
            handleButtonClick(which);
        }
    };

    public static final int BTN_RESTART              = DialogInterface.BUTTON_POSITIVE;
    public static final int BTN_RESTART_CHANGEFORMAT = DialogInterface.BUTTON_NEUTRAL;
    public static final int BTN_NO_RESTART           = DialogInterface.BUTTON_NEGATIVE;
    @Override public void handleButtonClick(int which) {
        dialog.dismiss();
        switch ( which ) {
            case BTN_RESTART:
                scoreBoard.restartScore();
                break;
            case BTN_RESTART_CHANGEFORMAT:
                //boolean bAutoShowTimer = PreferenceValues.showTimersAutomatically(context);
                Intent nm = new Intent(context, Match.class);
                Model m = Brand.getModel();
                m.setPlayerName(Player.A, matchModel.getName   (Player.A, true, true) );
                m.setPlayerName(Player.B, matchModel.getName   (Player.B, true, true) );
                m.setPlayerCountry(Player.A, matchModel.getCountry(Player.A) );
                m.setPlayerCountry(Player.B, matchModel.getCountry(Player.B) );
                m.setPlayerClub(Player.A, matchModel.getClub   (Player.A) );
                m.setPlayerClub(Player.B, matchModel.getClub   (Player.B) );
                m.setSource(matchModel.getSource() );
                nm.putExtra(Model.class.getSimpleName(), m.toJsonString(null));
/*
                Bundle bundle = new Bundle();
                bundle.putString(MatchDetails.PlayerA .toString(), matchModel.getName   (Player.A, true, true) );
                bundle.putString(MatchDetails.PlayerB .toString(), matchModel.getName   (Player.B, true, true) );
                bundle.putString(MatchDetails.CountryA.toString(), matchModel.getCountry(Player.A) );
                bundle.putString(MatchDetails.CountryB.toString(), matchModel.getCountry(Player.B) );
                bundle.putString(MatchDetails.ClubA   .toString(), matchModel.getClub   (Player.A) );
                bundle.putString(MatchDetails.ClubB   .toString(), matchModel.getClub   (Player.B) );
                bundle.putString(MatchDetails.FeedKey .toString(), matchModel.getSource() );
                nm.putExtra(MatchDetails.class.getSimpleName(), bundle);
*/
                scoreBoard.startActivityForResult(nm, 1);
                break;
            case BTN_NO_RESTART:
                break;
        }
        scoreBoard.triggerEvent(ScoreBoard.SBEvent.restartScoreDialogEnded, this);
    }
}
