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

import android.content.Context;
import android.content.DialogInterface;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class MyMDDialogBuilder extends MaterialAlertDialogBuilder {
    public MyMDDialogBuilder(@NonNull Context context) {
        super(context);
    }

    public AlertDialog show(DialogInterface.OnShowListener onShowListener) {
        AlertDialog dialog = super.create();
        dialog.setOnShowListener(onShowListener);
        try {
            dialog.show();
        } catch (Exception e) {
            e.printStackTrace();
/* IH 20180322: try catch to prevent crash for following exception (reported for apk 183 on android 5.0 and 7.0)
                android.view.WindowManager$BadTokenException:
                at android.view.ViewRootImpl.setView (ViewRootImpl.java:922)
                at android.view.WindowManagerGlobal.addView (WindowManagerGlobal.java:377)
                at android.view.WindowManagerImpl.addView (WindowManagerImpl.java:105)
                at android.app.Dialog.show (Dialog.java:404)
                at com.doubleyellow.scoreboard.main.ScoreBoard$MyDialogBuilder.show (ScoreBoard.java:4018)
                at com.doubleyellow.scoreboard.main.ScoreBoard$MyDialogBuilder.show (ScoreBoard.java:4012)
                at com.doubleyellow.scoreboard.dialog.EditFormat.show (EditFormat.java:128)
                at com.doubleyellow.scoreboard.main.DialogManager.showNextDialog (DialogManager.java:116)
                at com.doubleyellow.scoreboard.main.ScoreBoard.showNextDialog (ScoreBoard.java:4061)
                at com.doubleyellow.scoreboard.main.ScoreBoard.triggerEvent (ScoreBoard.java:2812)
                at com.doubleyellow.scoreboard.timer.Timer$SBCountDownTimer.onFinish (Timer.java:242)            }
                at android.os.CountDownTimer$1.handleMessage (CountDownTimer.java:127)
*/
        }
        return dialog;
    }
}
