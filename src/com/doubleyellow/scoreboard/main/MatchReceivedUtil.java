package com.doubleyellow.scoreboard.main;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.SparseArray;
import android.widget.Toast;
import com.doubleyellow.scoreboard.history.MatchHistory;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.util.FileUtil;

import java.io.File;
import java.io.IOException;

/**
 * Help class to let user decide what to do with a match received via
 * - NFC message
 * - a URL download
 */
public class MatchReceivedUtil implements DialogInterface.OnClickListener
{
    private ScoreBoard scoreBoard = null;
    private Model      model      = null;

    private SparseArray<MatchAction> mButton2Action = new SparseArray<MatchAction>();

    public MatchReceivedUtil(ScoreBoard scoreBoard, Model model) {
        this.scoreBoard = scoreBoard;
        this.model   = model;
    }
    @Override public void onClick(DialogInterface dialog, int which) {
        try {
            MatchAction matchAction = mButton2Action.get(which);
            switch (matchAction){
                case ContinueInScoreBoard: {
                    // load the received match in scoreboard for continuation

                    // first store currently loaded match
                    ScoreBoard.storeAsPrevious(scoreBoard, ScoreBoard.matchModel, false);

                    // put content of url into LAST match sb file
                    File f = ScoreBoard.getLastMatchFile(scoreBoard);
                    FileUtil.writeTo(f, model.toJsonString(scoreBoard));

                    ScoreBoard.matchModel = null;
                    scoreBoard.initScoreBoard(f);
                    break;
                }
                case ShowDetails: {
                    // show details of the finished match
                    Intent matchHistory = new Intent(scoreBoard, MatchHistory.class);
                    Bundle b = new Bundle();
                    File fStoredAs = ScoreBoard.storeAsPrevious(scoreBoard, model, true);
                    b.putSerializable(MatchHistory.class.getSimpleName(), fStoredAs);
                    matchHistory.putExtra(MatchHistory.class.getSimpleName(), b);
                    if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN ) {
                        scoreBoard.startActivity(matchHistory, b);
                    }
                    break;
                }
                case SaveToStoredMatches: {
                    File fStoredAs = ScoreBoard.storeAsPrevious(scoreBoard, model, true);
                    break;
                }
                case Nothing:
                    break;
            }
        } catch (IOException e) {
            Toast.makeText(scoreBoard, "An error occurred: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    public void init(String sMsg  , int iCaptionNeg    , MatchAction iActionNeg
            , int iCaptionNeutral, MatchAction iActionNeutral
            , int iCaptionPos    , MatchAction iActionPos )
    {
        mButton2Action.put(DialogInterface.BUTTON_NEGATIVE, iActionNeg);
        mButton2Action.put(DialogInterface.BUTTON_NEUTRAL , iActionNeutral);
        mButton2Action.put(DialogInterface.BUTTON_POSITIVE, iActionPos);

        AlertDialog.Builder ab = ScoreBoard.getAlertDialogBuilder(scoreBoard);
        ab.setMessage       (sMsg)
                .setNegativeButton(iCaptionNeg    , this)
                .setNeutralButton (iCaptionNeutral, this);
        if ( iCaptionPos != 0 ) {
            ab.setPositiveButton(iCaptionPos, this);
        }
        ab.show();
    }
}
