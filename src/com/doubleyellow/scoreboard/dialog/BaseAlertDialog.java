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

package com.doubleyellow.scoreboard.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.doubleyellow.scoreboard.dialog.material.MDBaseAlertDialog;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.main.ScoreBoard;

public abstract class BaseAlertDialog extends IBaseAlertDialog /*MDBaseAlertDialog or IBaseAlertDialog*/ /*extends AlertDialog NOT. TO MUCH hassle*/ {

    protected MyDialogBuilder adb    = null; // NOT if extending MDBaseAlertDialog
    protected AlertDialog     dialog = null; // NOT if extending MDBaseAlertDialog

    public BaseAlertDialog(Context context, Model matchModel, ScoreBoard scoreBoard) {
        super(context, matchModel, scoreBoard);
        this.adb = new MyDialogBuilder(context); // NOT if extending MDBaseAlertDialog

        swapPosNegButtons(context);
    }

    /** For demo purposes only */
    public void drawTouch(int iAction, int iColor) {
        if ( dialog == null ) {
            return;
        }
        final TextView btnTouch = getButton(iAction);
        if ( btnTouch != null ) {
            btnTouch.setBackgroundColor(iColor);
        } else {
            TextView vAction = m_lButtons.get(iAction);
            if ( vAction != null ) {
                vAction.setBackgroundColor(iColor);
            }
        }
    }

    /** in Android 9 - API 28, buttons in dialog are left aligned initially. By just setting the layout parameters the get correctly aligned */
    public void triggerButtonLayoutAPI28(DialogInterface dialogInterface, int iButton) {
        //if ( true ) { return; }

        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ) {
            Button btnMiddle = ((AlertDialog)dialogInterface).getButton(iButton);
            if ( btnMiddle == null ) {
                // even if 'neutral' button is specified, it usually is still there but invisible
                return;
            }
            ViewParent parent = btnMiddle.getParent(); // LinearLayout
            if ( parent instanceof LinearLayout ) {
                ViewGroup.LayoutParams layoutParams = btnMiddle.getLayoutParams();
                if ( layoutParams instanceof LinearLayout.LayoutParams ) {
                    LinearLayout.LayoutParams llp = (LinearLayout.LayoutParams) layoutParams;
                    //llp.weight = llp.weight;
                    btnMiddle.setLayoutParams(llp);
                }
            }

        }
    }
    @Override
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
    @Override
    public boolean isShowing() {
        return (dialog != null) && dialog.isShowing();
    }



/*
    public void setTitle(int iRes) {
        if ( iRes == 0 ) {
            setTitle("");
            return adb.setTitle("");
        }
        if ( dialog != null ) {
            dialog.setTitle(iRes);
        }
        return adb.setTitle(iRes);
    }
    public AlertDialog.Builder setTitle(String s) {
        if ( dialog != null ) {
            dialog.setTitle(s);
        }
        return adb.setTitle(s);
    }
    AlertDialog.Builder setIcon(int iRes) {
        if ( iRes == 0 ) {
            return adb;
        }
        if ( dialog != null ) {
            dialog.setIcon(iRes);
        }
        return adb.setIcon(iRes);
    }
    AlertDialog.Builder setMessage(String s) {
        if ( dialog != null ) {
            dialog.setMessage(s);
        }
        return adb.setMessage(s);
    }
*/
/*
    AlertDialog.Builder setPositiveButton(String s, DialogInterface.OnClickListener onClickListener) {
        if ( swapPosNegButtons(context) ) {
            return adb.setNegativeButton(s, onClickListener);
        }
        return adb.setPositiveButton(s, onClickListener);
    }
    AlertDialog.Builder setNegativeButton(String s, DialogInterface.OnClickListener onClickListener) {
        if ( swapPosNegButtons(context) ) {
            return adb.setPositiveButton(s, onClickListener);
        }
        return adb.setNegativeButton(s, onClickListener);
    }
    AlertDialog.Builder setNeutralButton(int s, DialogInterface.OnClickListener onClickListener) {
        return adb.setNeutralButton(s, onClickListener);
    }
*/

    public DialogInterface create() {
        dialog = adb.create();
        return dialog;
    }
    @Override protected Button getDialogButton(/*AlertDialog alertDialog, */int iButton) {
        return dialog.getButton(iButton);
    }

}
