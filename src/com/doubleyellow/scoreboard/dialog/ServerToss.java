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
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.LinearLayout;

import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.model.Player;
import com.doubleyellow.scoreboard.main.ScoreBoard;

/**
 * Dialog in which the referee can either
 * - choose the server (after a toss has been performed on court, e.g. by means of spinning a racket)
 * - let the app perform a toss
 */
//public class ServerToss extends BaseCustomDialog
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
        if ( matchModel == null ) { return; }
        String sPlayerA = matchModel.getName(Player.A);
        String sPlayerB = matchModel.getName(Player.B);
        if ( Brand.supportChooseServeOrReceive() ) {
            if ( isWearable() ) {
                adb.setMessage(getString(R.string.sb_serve) + " / " + getString(R.string.sb_receive));
            } else {
                adb.setTitle(R.string.sb_cmd_toss);
                adb.setMessage(getString(R.string.sb_serve) + " / " + getString(R.string.sb_receive) + "\n"
                             + getString(R.string.choose_winner_of_toss_or_perform_toss)  ); // for gotoStage_ChooseServeReceive() we need to set something for the message that is set there to be visible
            }
        }
        if ( isNotWearable() ) {
            adb.setIcon          (R.drawable.toss_white);
            adb.setTitle         (R.string.sb_who_will_start_to_serve);
        }
        adb.setPositiveButton(sPlayerA            , null);
        adb.setNeutralButton (R.string.sb_cmd_toss, null);
        adb.setNegativeButton(sPlayerB            , null);
        adb.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override public boolean onKey(DialogInterface dialogI, int keyCode, KeyEvent event) {
                int action  = event.getAction();
                if (keyCode == KeyEvent.KEYCODE_BACK /* = 4 */ && action == KeyEvent.ACTION_UP) {
                    AlertDialog dialog = (AlertDialog) dialogI;
                    final Button btnA = getButton(BTN_A_WINS_TOSS);
                    final Button btnB = getButton(BTN_B_WINS_TOSS);
                    if ( btnA.isEnabled() == false ) {
                        // toss is performed and B was selected
                        handleButtonClick(BTN_B_WINS_TOSS);
                    } else if ( btnB.isEnabled() == false ) {
                        // toss is performed and A was selected
                        handleButtonClick(BTN_A_WINS_TOSS);
                    } else {
                        // no toss performed yet
                        ServerToss.this.dismiss();
                    }
                    return true;
                }
                return false;
            }
        });

        adb.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override public void onDismiss(DialogInterface dialog) {
                scoreBoard.triggerEvent(ScoreBoard.SBEvent.tossDialogEnded, ServerToss.this);
            }
        });
        try {
            OnShowListener listener = new OnShowListener(context, ButtonUpdater.iaColorNeutral);
            //dialog = adb.show(listener, true); // have had report that this throws android.view.WindowManager$BadTokenException, everything automated?: warmup timer+toss dialog
            create();
            dialog.setOnShowListener(listener);
            dialog.show();
        } catch (Exception e) {
            // this may fail if activity has been in the background, seen during casting (android.view.WindowManager$BadTokenException: Unable to add window -- token android.os.BinderProxy@860c8b3 is not valid; is your activity running?)
            e.printStackTrace();
        }
    }

    private class LeftRightButtonClickListener implements View.OnClickListener {
        private int m_which;
        private LeftRightButtonClickListener(int which) {
            m_which = which;
        }
        @Override public void onClick(View view) {
            handleButtonClick(m_which);
        }
    };

    private View.OnClickListener onClickTossListener = new View.OnClickListener() {
        @Override public void onClick(View view) {
            simulateToss();
        }
    };


    public              int BTN_SERVE       = DialogInterface.BUTTON_POSITIVE;
    public              int BTN_RECEIVE     = DialogInterface.BUTTON_NEGATIVE;
    public              int BTN_A_WINS_TOSS = DialogInterface.BUTTON_POSITIVE;
    public              int BTN_B_WINS_TOSS = DialogInterface.BUTTON_NEGATIVE;
    public static final int BTN_DO_TOSS     = DialogInterface.BUTTON_NEUTRAL;

            static Player  m_winnerOfToss          = null; // used by SideToss
    private static boolean m_winnerChooseToReceive = false;

    private static final int VISIBILITY_TOSS_BUTTON_FOR_TT_SIDE_RECEIVE = View.INVISIBLE;
    @Override public void handleButtonClick(int which) {
        final Button btnToss = getButton(BTN_DO_TOSS );
        if (    Brand.supportChooseServeOrReceive()
             && getButton(BTN_DO_TOSS).getVisibility() == VISIBILITY_TOSS_BUTTON_FOR_TT_SIDE_RECEIVE
           )
        {
            // serve or receive
            if ( which == BTN_SERVE ) {
                m_winnerChooseToReceive = false;
            } else if ( which == BTN_RECEIVE) {
                m_winnerChooseToReceive = true;
            }
            if ( m_winnerChooseToReceive ) {
                if ( m_winnerOfToss != null ) {
                    Player pLooserOfToss = m_winnerOfToss.getOther();
                    scoreBoard.changeSide(pLooserOfToss);
                }
            }
            this.dismiss();
        } else {
            final Player server = matchModel.getServer();
            if (which == BTN_A_WINS_TOSS) {
                m_winnerOfToss = Player.A;
            } else if ( which == BTN_B_WINS_TOSS ) {
                m_winnerOfToss = Player.B;
            }
            if ( (m_winnerOfToss != null) && (m_winnerOfToss.equals(server) == false) ) {
                scoreBoard.changeSide(m_winnerOfToss);
            }

            // optionally go to next 'stage' of dialog
            if ( m_winnerOfToss != null ) {
                if ( Brand.supportChooseServeOrReceive() ) {
                    final Button btnA = getButton(BTN_A_WINS_TOSS);
                    final Button btnB = getButton(BTN_B_WINS_TOSS);
                    gotoStage_ChooseServeReceive(btnToss, btnA, btnB);
                } else {
                    this.dismiss();
                }
            }
        }
    }

    /** introduced to install OnClickListeners on the 3 buttons */
    private class OnShowListener extends ButtonUpdater {
        private OnShowListener(Context context, int... iButton2Color) {
            super(context, iButton2Color);
        }
        @Override public void onShow(DialogInterface dialogInterface) {
            super.onShow(dialogInterface);

            final Button btnToss = getButton(BTN_DO_TOSS);
            if ( btnToss == null ) {
                return;
            }
            ViewParent parent = btnToss.getParent();
            if ( parent instanceof LinearLayout ) {
                ViewGroup.LayoutParams layoutParams = btnToss.getLayoutParams();
                if ( layoutParams instanceof LinearLayout.LayoutParams ) {
                    LinearLayout.LayoutParams llp = (LinearLayout.LayoutParams) layoutParams;
                    llp.weight = 0.8f;
                    btnToss.setLayoutParams(llp);
                }
            }

            // ensure that when toss button is clicked player buttons are toggled a couple of times then only one remains enabled
            btnToss.setOnClickListener(onClickTossListener);

            getButton(BTN_A_WINS_TOSS).setOnClickListener(new LeftRightButtonClickListener(BTN_A_WINS_TOSS));
            getButton(BTN_B_WINS_TOSS).setOnClickListener(new LeftRightButtonClickListener(BTN_B_WINS_TOSS));
        }
    }

    private void simulateToss() {
        final Button btnToss = getButton(BTN_DO_TOSS );
        final Button btnA    = getButton(BTN_A_WINS_TOSS);
        final Button btnB    = getButton(BTN_B_WINS_TOSS);
        btnToss.setEnabled(false);
        if ( (btnA == null) || (btnB == null) ) return;

        boolean bInitialEnabledA = Math.round(Math.random()) == 0;
        btnA.setEnabled(  bInitialEnabledA );
        btnB.setEnabled( !bInitialEnabledA );
        CountDownTimer countDownTimer = new CountDownTimer(2400, (80 + Math.abs(System.currentTimeMillis() % 40))) {
            @Override public void onTick(long l) {
                // toggle 'enabled' status of both buttons on every 'tick'
                btnA.setEnabled( btnA.isEnabled() == false );
                btnB.setEnabled( btnB.isEnabled() == false );
            }

            @Override public void onFinish() {
                btnToss.setEnabled(true);
                if ( (matchModel != null) && (matchModel.hasStarted() == false) ) {
                    // automatically change the serve side in the model already, but without closing the dialog
                    final Player pServer = matchModel.getServer();
                    if ( btnA.isEnabled() ) {
                        m_winnerOfToss = Player.A;
                        if ( pServer.equals(Player.B) ) {
                            scoreBoard.changeSide(Player.A);
                        }
                    } else if (btnB.isEnabled() ) {
                        m_winnerOfToss = Player.B;
                        if ( pServer.equals(Player.A) ) {
                            scoreBoard.changeSide(Player.B);
                        }
                    }
                }

                if ( Brand.supportChooseServeOrReceive() ) {
                    gotoStage_ChooseServeReceive(btnToss, btnA, btnB);
                }
            }
        };
        countDownTimer.start();
    }

    private void gotoStage_ChooseServeReceive(Button btnToss, Button btnA, Button btnB) {
        // show message who won the toss
        // change buttons in 'serve' and 'receive', winner may choose to receive
        String sWinnerOfToss = matchModel.getName(m_winnerOfToss);
        if ( isWearable() ) {
            adb.setTitle(0);
            adb.setIcon(0);
            adb.setMessage(getString(R.string.sb_toss_won_by_x, sWinnerOfToss) + "\n" + getString(R.string.sb_players_choice));
        } else {
            adb.setTitle  (getString(R.string.sb_toss_won_by_x, sWinnerOfToss));
            adb.setMessage(getString(R.string.sb_players_choice));
        }
        btnToss.setVisibility(VISIBILITY_TOSS_BUTTON_FOR_TT_SIDE_RECEIVE);
        btnA.setEnabled(true); btnA.setText(R.string.sb_serve);
        btnB.setEnabled(true); btnB.setText(R.string.sb_receive);
    }

    /** for newer theme this should be switched for consistency */
    @Override protected boolean swapPosNegButtons(Context context) {
        if ( MyDialogBuilder.isUsingNewerTheme(context) == false ) { return false; }
        BTN_SERVE       = DialogInterface.BUTTON_NEGATIVE;
        BTN_RECEIVE     = DialogInterface.BUTTON_POSITIVE;
        BTN_A_WINS_TOSS = DialogInterface.BUTTON_NEGATIVE;
        BTN_B_WINS_TOSS = DialogInterface.BUTTON_POSITIVE;
        return true;
    }
}
