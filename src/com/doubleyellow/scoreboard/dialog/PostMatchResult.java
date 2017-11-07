package com.doubleyellow.scoreboard.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import com.doubleyellow.scoreboard.URLFeedTask;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.prefs.PreferenceKeys;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.scoreboard.view.PreferenceCheckBox;

/**
 * Dialog to suggest to the user to 'Post' the match result
 */
public class PostMatchResult extends BaseAlertDialog
{
    public PostMatchResult(Context context, Model matchModel, ScoreBoard scoreBoard) {
        super(context, matchModel, scoreBoard);
    }

    private boolean m_bShowWithNoMoreCheckbox;
    @Override public boolean storeState(Bundle outState) {
        outState.putBoolean(PostMatchResult.class.getSimpleName(), m_bShowWithNoMoreCheckbox);
        return true;
    }

    @Override public boolean init(Bundle outState) {
        init(outState.getBoolean(PostMatchResult.class.getSimpleName()));
        return true;
    }
    @Override public void show() {

        LinearLayout ll = null;
        if ( m_bShowWithNoMoreCheckbox ) {
            CheckBox cbNoMore = new PreferenceCheckBox(context, PreferenceKeys.autoSuggestToPostResult, R.bool.suggestToPostResult_default, true);
            ll = new LinearLayout(context);
            ll.setOrientation(LinearLayout.VERTICAL);
            cbNoMore.setText(R.string.sb_post_match_result_no_more);
            cbNoMore.setTextColor(Color.WHITE);
            ll.addView(cbNoMore);
        }

        String sTitle = String.format(this.getString(R.string.sb_post_match_result_q), PreferenceValues.getFeedPostName(context));

        String postResultToURL = PreferenceValues.getPostResultToURL(context);
               postResultToURL = URLFeedTask.prefixWithBaseIfRequired(postResultToURL);
        adb.setIcon   (R.drawable.ic_action_send_now)
           .setTitle(sTitle)
           .setMessage(postResultToURL)
           .setPositiveButton(R.string.cmd_ok    , dialogClickListener)
           .setNegativeButton(R.string.cmd_cancel, dialogClickListener)
           .setOnKeyListener(getOnBackKeyListener());

        if ( ll != null ) {
            adb.setView(ll);
        }

        // in a try catch to prevent crashing if somehow scoreBoard is not showing any longer
        try {
            dialog = adb.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void init(boolean bShowWithNoMoreCheckBox) {
        m_bShowWithNoMoreCheckbox = bShowWithNoMoreCheckBox;
    }

    private DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
        @Override public void onClick(DialogInterface dialog, int which) {
            handleButtonClick(which);
        }
    };

    public static final int DO_POST = DialogInterface.BUTTON_POSITIVE;

    @Override public void handleButtonClick(int which) {
        switch (which){
            case DO_POST:
                scoreBoard.handleMenuItem(R.id.sb_post_match_result);
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                // TODO: show message that this can be turned off in the pref screen
                break;
        }
        scoreBoard.triggerEvent(ScoreBoard.SBEvent.dialogClosed, this);
    }
}
