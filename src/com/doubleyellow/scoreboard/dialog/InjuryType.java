package com.doubleyellow.scoreboard.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.SparseArray;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.timer.Type;
import com.doubleyellow.util.StringUtil;

public class InjuryType extends BaseAlertDialog
{
    public InjuryType(Context context, Model matchModel, ScoreBoard scoreBoard) {
        super(context, matchModel, scoreBoard);
    }

    @Override public boolean storeState(Bundle outState) {
        return true;
    }

    @Override public boolean init(Bundle outState) {
        return true;
    }

    final SparseArray<Type> mTranslateButtonToType = new SparseArray<Type>();
    @Override public void show() {

        adb.setTitle(context.getString(R.string.sb_injury_type))
           .setIcon(R.drawable.microphone)
         //.setMessage(context.getString(R.string.sb_tiebreak_receiver_chooses_winning_score, iPointsEach, sReceiverName))
        ;

        mTranslateButtonToType.put(DialogInterface.BUTTON_POSITIVE, Type.SelfInflictedInjury);
        mTranslateButtonToType.put(DialogInterface.BUTTON_NEUTRAL , Type.ContributedInjury);
        mTranslateButtonToType.put(DialogInterface.BUTTON_NEGATIVE, Type.OpponentInflictedInjury);

        for(int i = 0; i < mTranslateButtonToType.size(); i++) {
            int iButton = mTranslateButtonToType.keyAt(i);
            Type   type = mTranslateButtonToType.get(iButton);
            String text = StringUtil.capitalize(type); // TODO: internationalize
            switch (iButton) {
                case DialogInterface.BUTTON_POSITIVE: adb.setPositiveButton(text, chooseInjuryType); break;
                case DialogInterface.BUTTON_NEUTRAL : adb.setNeutralButton (text, chooseInjuryType); break;
                case DialogInterface.BUTTON_NEGATIVE: adb.setNegativeButton(text, chooseInjuryType); break;
            }
        }
        adb.setOnKeyListener(getOnBackKeyListener());
        dialog = adb.show();
    }

    private DialogInterface.OnClickListener chooseInjuryType = new DialogInterface.OnClickListener() {
        @Override public void onClick(DialogInterface dialogInterface, int which) {
            handleButtonClick(which);
        }
    };

    @Override public void handleButtonClick(int which) {
        Type type = mTranslateButtonToType.get(which);
        scoreBoard.triggerEvent(ScoreBoard.SBEvent.injuryTypeClosed, type);
    }
}
