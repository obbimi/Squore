/*
 * Copyright (C) 2017  Iddo Hoeve
 *
 * Squore is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.doubleyellow.scoreboard.main;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.SparseArray;
import android.widget.Toast;

import com.doubleyellow.scoreboard.PersistHelper;
import com.doubleyellow.scoreboard.activity.IntentKeys;
import com.doubleyellow.scoreboard.dialog.MyDialogBuilder;
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
                    PersistHelper.storeAsPrevious(scoreBoard, ScoreBoard.matchModel, false);

                    // put content of url into LAST match sb file
                    File f = PersistHelper.getLastMatchFile(scoreBoard);
                    FileUtil.writeTo(f, model.toJsonString(scoreBoard));

                    ScoreBoard.matchModel = null;
                    scoreBoard.initScoreBoard(f);
                    break;
                }
                case ShowDetails: {
                    // show details of the finished match
                    Intent matchHistory = new Intent(scoreBoard, MatchHistory.class);
                    Bundle b = new Bundle();
                    File fStoredAs = PersistHelper.storeAsPrevious(scoreBoard, model, true);
                    b.putSerializable(IntentKeys.MatchHistory.toString(), fStoredAs);
                    matchHistory.putExtra(IntentKeys.MatchHistory.toString(), b);
                    scoreBoard.startActivity(matchHistory, b);
                    break;
                }
                case SaveToStoredMatches: {
                    File fStoredAs = PersistHelper.storeAsPrevious(scoreBoard, model, true);
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

        AlertDialog.Builder ab = new MyDialogBuilder(scoreBoard);
        ab.setMessage       (sMsg)
                .setNegativeButton(iCaptionNeg    , this)
                .setNeutralButton (iCaptionNeutral, this);
        if ( iCaptionPos != 0 ) {
            ab.setPositiveButton(iCaptionPos, this);
        }
        ab.show();
    }
}
