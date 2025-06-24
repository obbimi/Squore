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

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.doubleyellow.android.util.ColorUtil;
import com.doubleyellow.android.view.ViewUtil;
import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.model.Player;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.prefs.ColorPrefs;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.util.Direction;

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

        if ( true ) {
            int iIconSize = (int) PreferenceValues.getAppealHandGestureIconSize(context);
            int iSize = iIconSize * 2;
            Direction dIconPosition = Direction.N; // center
            LinearLayout.LayoutParams llParamsPlayerNames = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, iSize);
            LinearLayout.LayoutParams llParamsToss        = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, iSize);

            LinearLayout ll = new LinearLayout(context);
            int       iOrientation  = LinearLayout.VERTICAL;
            int iMargin = iIconSize / 10; // margin between the 3 buttons
            if ( ViewUtil.isLandscapeOrientation(context) ) {
                llParamsPlayerNames = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
                llParamsToss        = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, iSize);
                iOrientation  = LinearLayout.HORIZONTAL;
                llParamsPlayerNames.setMargins(iMargin, iMargin, iMargin, iMargin);
                llParamsToss       .setMargins(iMargin, iMargin, iMargin, iMargin);
            } else {
                llParamsPlayerNames.setMargins(iMargin, iMargin, iMargin, iMargin);
                llParamsToss       .setMargins(iMargin, iMargin, iMargin, iMargin);
            }
            ll.setOrientation(iOrientation);
            llParamsPlayerNames.weight = 1;
            llParamsToss       .weight = 1;

            int iResId_Toss  = R.drawable.toss_black;
            int iResId_None  = 0; //R.drawable.dummy;
            //int iIconSizeNone = 0;
            Integer iBG = ColorPrefs.getTarget2colorMapping(context).get(ColorPrefs.ColorTarget.backgroundColor);
            if ( iBG != null ) {
                // if we use a dark background ... switch to the light gesture icons
                int blackOrWhiteFor = ColorUtil.getBlackOrWhiteFor(iBG);
                if ( blackOrWhiteFor == Color.WHITE ) {
                    iResId_Toss = R.drawable.toss_white;
                }
            }
            final TextView vPlayerA = getActionView(sPlayerA            , BTN_A_WINS_TOSS, iResId_None, iSize, dIconPosition);
            final TextView vTos     = getActionView(R.string.sb_cmd_toss, BTN_DO_TOSS    , iResId_Toss, iIconSize, Direction.N);
            final TextView vPlayerB = getActionView(sPlayerB            , BTN_B_WINS_TOSS, iResId_None, iSize, dIconPosition);
            ll.addView(vPlayerA, llParamsPlayerNames);
            ll.addView(vTos    , llParamsToss);
            ll.addView(vPlayerB, llParamsPlayerNames);
            adb.setView(ll);

            vTos.setOnClickListener(onClickTossListener);

            ColorPrefs.setColor(ll);
        } else {
            adb.setPositiveButton(sPlayerA            , null);
            adb.setNeutralButton (R.string.sb_cmd_toss, null);
            adb.setNegativeButton(sPlayerB            , null);
            adb.setOnKeyListener((dialogI, keyCode, event) -> {
                int action  = event.getAction();
                if (keyCode == KeyEvent.KEYCODE_BACK /* = 4 */ && action == KeyEvent.ACTION_UP) {
                    final TextView btnA = getButton(BTN_A_WINS_TOSS);
                    final TextView btnB = getButton(BTN_B_WINS_TOSS);
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
            });
        }


        adb.setOnDismissListener(dialog -> scoreBoard.triggerEvent(ScoreBoard.SBEvent.tossDialogEnded, ServerToss.this));
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

    private final View.OnClickListener onClickTossListener = view -> simulateToss();


    public              int BTN_SERVE       = DialogInterface.BUTTON_POSITIVE;
    public              int BTN_RECEIVE     = DialogInterface.BUTTON_NEGATIVE;
    public              int BTN_A_WINS_TOSS = DialogInterface.BUTTON_POSITIVE;
    public              int BTN_B_WINS_TOSS = DialogInterface.BUTTON_NEGATIVE;
    public static final int BTN_DO_TOSS     = DialogInterface.BUTTON_NEUTRAL;

            static Player  m_winnerOfToss          = null; // used by SideToss
    private static boolean m_winnerChooseToReceive = false;

    private static final int VISIBILITY_TOSS_BUTTON_FOR_TT_SIDE_RECEIVE = View.INVISIBLE;
    @Override public void handleButtonClick(int which) {
        final TextView btnToss = getButton(BTN_DO_TOSS );
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
                    final TextView btnA = getButton(BTN_A_WINS_TOSS);
                    final TextView btnB = getButton(BTN_B_WINS_TOSS);
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

            final TextView btnToss = getButton(BTN_DO_TOSS);
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
        final TextView btnToss = getButton(BTN_DO_TOSS );
        final TextView btnA    = getButton(BTN_A_WINS_TOSS);
        final TextView btnB    = getButton(BTN_B_WINS_TOSS);
        //m_lButtons
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

    private void gotoStage_ChooseServeReceive(TextView btnToss, TextView btnA, TextView btnB) {
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
    @Override public boolean swapPosNegButtons(Context context) {
        if ( MyDialogBuilder.isUsingNewerTheme(context) == false ) { return false; }
        BTN_SERVE       = DialogInterface.BUTTON_NEGATIVE;
        BTN_RECEIVE     = DialogInterface.BUTTON_POSITIVE;
        BTN_A_WINS_TOSS = DialogInterface.BUTTON_NEGATIVE;
        BTN_B_WINS_TOSS = DialogInterface.BUTTON_POSITIVE;
        return true;
    }
}
