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
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.LinearLayout;

import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.model.Player;
import com.doubleyellow.scoreboard.main.ScoreBoard;

/**
 * Dialog in which the referee can either
 * - choose the server (after a toss has been performed on court, e.g. by means of spinning a racket)
 * - let the app perform a toss
 */
public class ServerToss extends BaseAlertDialog
{
    public ServerToss(Context context, Model matchModel, ScoreBoard scoreBoard) {
        super(context, matchModel, scoreBoard);
    }

    @Override public boolean storeState(Bundle outState) {
        return true;
    }

    @Override public boolean init(Bundle outState) {
        return true;
    }

    @Override public void show() {
        String sPlayerA = matchModel.getName(Player.A);
        String sPlayerB = matchModel.getName(Player.B);
        adb.setTitle         (R.string.sb_who_will_start_to_serve)
           .setIcon          (R.drawable.toss_white)
           .setPositiveButton(sPlayerA           , dialogClickListener)
           .setNeutralButton(R.string.sb_cmd_toss, null)
           .setNegativeButton(sPlayerB           , dialogClickListener)
           .setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override public boolean onKey(DialogInterface dialogI, int keyCode, KeyEvent event) {
                int action  = event.getAction();
                if (keyCode == KeyEvent.KEYCODE_BACK /* = 4 */ && action == KeyEvent.ACTION_UP) {
                    AlertDialog dialog = (AlertDialog) dialogI;
                    final Button btnA = dialog.getButton(BTN_A_STARTS);
                    final Button btnB = dialog.getButton(BTN_B_STARTS);
                    if ( btnA.isEnabled() == false ) {
                        // toss is performed and B was selected
                        handleButtonClick(BTN_B_STARTS);
                    } else if ( btnB.isEnabled() == false ) {
                        // toss is performed and B was selected
                        handleButtonClick(BTN_A_STARTS);
                    } else {
                        // no toss performed yet
                        ServerToss.this.dismiss();
                    }
                    return true;
                }
                return false;
            }
        });

        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 /* 17 */ ) {
            adb.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override public void onDismiss(DialogInterface dialog) {
                    scoreBoard.triggerEvent(ScoreBoard.SBEvent.tossDialogEnded, ServerToss.this);
                }
            });
        }
        try {
            OnShowListener listener = new OnShowListener(context, ButtonUpdater.iaColorNeutral);
            dialog = adb.show(listener); // have had report that this throws android.view.WindowManager$BadTokenException, everything automated?: warmup timer+toss dialog
        } catch (Exception e) {
            // this may fail if activity has been in the background, seen during casting (android.view.WindowManager$BadTokenException: Unable to add window -- token android.os.BinderProxy@860c8b3 is not valid; is your activity running?)
            e.printStackTrace();
        }
    }

    private DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
        @Override public void onClick(DialogInterface dialog, int which) {
            handleButtonClick(which);
        }
    };

    public static final int BTN_A_STARTS = DialogInterface.BUTTON_POSITIVE;
    public static final int BTN_B_STARTS = DialogInterface.BUTTON_NEGATIVE;
    public static final int BTN_DO_TOSS  = DialogInterface.BUTTON_NEUTRAL;
    @Override public void handleButtonClick(int which) {
        Player server = matchModel.getServer();
        switch (which){
            case BTN_A_STARTS:
                server = Player.A;
                break;
            case BTN_B_STARTS:
                server = Player.B;
                break;
            case BTN_DO_TOSS:
                simulateToss();
                break;
        }
        if ( (server != null) && (server.equals(matchModel.getServer()) == false) ) {
            scoreBoard.changeSide(server);
        }
        if ( server != null ) {
            this.dismiss();
        }
        if ( which != BTN_DO_TOSS ) {
            scoreBoard.triggerEvent(ScoreBoard.SBEvent.tossDialogEnded, this);
        }
    }

    private class OnShowListener extends ButtonUpdater {
        OnShowListener(Context context, int... iButton2Color) {
            super(context, iButton2Color);
        }
        @Override public void onShow(DialogInterface dialogInterface) {
            super.onShow(dialogInterface);

            final Button btnToss = ((AlertDialog)dialogInterface).getButton(BTN_DO_TOSS);
            if (btnToss == null) {
                return;
            }
            ViewParent parent = btnToss.getParent(); // LinearLayout
            if ( parent instanceof LinearLayout ) {
                ViewGroup.LayoutParams layoutParams = btnToss.getLayoutParams();
                //LinearLayout ll = (LinearLayout) parent;
                //ViewGroup.LayoutParams layoutParams = ll.getLayoutParams();
                if ( layoutParams instanceof LinearLayout.LayoutParams ) {
                    LinearLayout.LayoutParams llp = (LinearLayout.LayoutParams) layoutParams;
                    llp.weight = 0.8f;
                    btnToss.setLayoutParams(llp);
                }
            }

            // ensure that when toss button is clicked player buttons are toggled a couple of times then only one remains enabled
            btnToss.setOnClickListener(onClickTossListener);
        }
    }

/*
    private DialogInterface.OnShowListener onShowListener = new DialogInterface.OnShowListener() {
        //We register an onClickListener for the 'neutral'='toss' button. There is already one, but that one closes the dialog no matter what.
        @Override public void onShow(DialogInterface dialogInterface) {
            final Button btnToss = dialog.getButton(BTN_DO_TOSS);
            if (btnToss == null) {
                return;
            }
            // ensure that when toss button is clicked player buttons are toggled a couple of times then only one remains enabled
            btnToss.setOnClickListener(onClickTossListener);
        }
    };
*/

    private View.OnClickListener onClickTossListener = new View.OnClickListener() {
        @Override public void onClick(View view) {
            simulateToss();
        }
    };

    private void simulateToss() {
        final Button btnToss = dialog.getButton(BTN_DO_TOSS );
        final Button btnA    = dialog.getButton(BTN_A_STARTS);
        final Button btnB    = dialog.getButton(BTN_B_STARTS);
        btnToss.setEnabled(false);
        if ( (btnA == null) || (btnB == null) ) return;

        boolean bInitialEnabledA = Math.round(Math.random()) == 0;
        btnA.setEnabled(  bInitialEnabledA );
        btnB.setEnabled( !bInitialEnabledA );
        CountDownTimer countDownTimer = new CountDownTimer(2400, (80 + Math.abs(System.currentTimeMillis() % 40))) {
            @Override public void onTick(long l) {
                btnA.setEnabled( btnA.isEnabled() == false );
                btnB.setEnabled( btnB.isEnabled() == false );
            }

            @Override public void onFinish() {
                btnToss.setEnabled(true);
                if ( (matchModel != null) && (matchModel.hasStarted() == false) ) {
                    // automatically change the serve side in the model already, but without closing the dialog
                    if ( btnA.isEnabled() && matchModel.getServer().equals(Player.B) ) {
                        scoreBoard.changeSide(Player.A);
                    } else if (btnB.isEnabled() && matchModel.getServer().equals(Player.A) ) {
                        scoreBoard.changeSide(Player.B);
                    }
                }
            }
        };
        countDownTimer.start();
    }
}
