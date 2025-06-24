/*
 * Copyright (C) 2025  Iddo Hoeve
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

package com.doubleyellow.scoreboard.dialog.material;

import androidx.appcompat.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.Button;

import com.doubleyellow.scoreboard.dialog.IBaseAlertDialog;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.model.Model;

public abstract class MDBaseAlertDialog extends IBaseAlertDialog {

    protected MyMDDialogBuilder adb    = null;
    protected AlertDialog       dialog = null;

    public MDBaseAlertDialog(Context context, Model matchModel, ScoreBoard scoreBoard) {
        super(context, matchModel, scoreBoard);
        this.context = context;
        this.matchModel = matchModel;
        this.scoreBoard = scoreBoard;

        this.adb = new MyMDDialogBuilder(context);
    }
    public boolean isShowing() {
        return (dialog != null) && dialog.isShowing();
    }
    public void dismiss() {
        if ( isShowing() ) {
            if ( dialog != null ) {
                try {
                    dialog.dismiss();
                } catch (Exception e) {
/*
                // try catch is for this reported error in play store (API 27)
                    at android.view.WindowManagerGlobal.findViewLocked (WindowManagerGlobal.java:473)
                    at android.view.WindowManagerGlobal.removeView (WindowManagerGlobal.java:382)
                    at android.view.WindowManagerImpl.removeViewImmediate (WindowManagerImpl.java:126)
                    at android.app.Dialog.dismissDialog (Dialog.java:370)
                    at android.app.Dialog.dismiss (Dialog.java:353)
                    at com.doubleyellow.scoreboard.dialog.BaseAlertDialog.dismiss (BaseAlertDialog.java:192)
*/
                    e.printStackTrace();
                }
            }
        }
    }

    public void handleButtonClick(int which){
        //this.dismiss();
    }

    public DialogInterface create() {
        dialog = adb.create();
        return dialog;
    }
    @Override protected Button getDialogButton(/*AlertDialog alertDialog, */int iButton) {
        return dialog.getButton(iButton);
    }
}
