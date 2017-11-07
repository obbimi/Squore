package com.doubleyellow.scoreboard.demo;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.ScrollView;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.dialog.BaseAlertDialog;
import com.doubleyellow.view.SelectEnumView;

public class DemoStartStep extends BaseAlertDialog
{
    public DemoStartStep(Context context, Model matchModel, ScoreBoard scoreBoard) {
        super(context, matchModel, scoreBoard);
    }

    @Override public boolean storeState(Bundle outState) {
        return false;
    }

    @Override public boolean init(Bundle outState) {
        return false;
    }

    private SelectEnumView<DemoThread.DemoMessage> sev;

    @Override public void show() {

        // add a view with all possible Conducts and let user choose one
        ScrollView sv = new ScrollView(context);
        sev = new SelectEnumView<DemoThread.DemoMessage>(context, DemoThread.DemoMessage.class);
        sv.addView(sev);

        dialog = adb
                .setTitle("Start where")
                .setPositiveButton(R.string.cmd_ok    , listener)
                .setNegativeButton(R.string.cmd_cancel, listener)
                .setView(sv)
                .show();
    }

    private DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
        @Override public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    DemoThread.DemoMessage message = sev.getChecked();
                    scoreBoard.handleMenuItem(R.id.sb_toggle_demo_mode, message);
                    break;
                case DialogInterface.BUTTON_NEGATIVE:
                    break;
            }
        }
    };
}
