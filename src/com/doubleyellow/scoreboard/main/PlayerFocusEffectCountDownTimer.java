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
package com.doubleyellow.scoreboard.main;

import android.os.CountDownTimer;
import android.util.Log;

import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.model.Player;
import com.doubleyellow.scoreboard.prefs.ShowScoreChangeOn;
import com.doubleyellow.scoreboard.vico.FocusEffect;
import com.doubleyellow.scoreboard.vico.IBoard;

public abstract class PlayerFocusEffectCountDownTimer extends CountDownTimer
{
    private static final String TAG = "SB." + PlayerFocusEffectCountDownTimer.class.getSimpleName();
    public Player            m_player                  = null;
    public int               m_iInvocationCnt          = 0;
    public ShowScoreChangeOn m_guiElementToUseForFocus = ShowScoreChangeOn.PlayerButton;
           FocusEffect       m_focusEffect             = null;
           int               m_iTmpTxtOnElementDuringFeedback = 0;

           IBoard            m_iBoard = null;
    public PlayerFocusEffectCountDownTimer(FocusEffect focusEffect, int iTotalDuration, int iInvocationInterval, IBoard iBoard) {
        super(iTotalDuration, iInvocationInterval);
        this.m_focusEffect = focusEffect;
        this.m_iBoard      = iBoard;
    }
    public abstract void doOnTick(int m_iInvocationCnt, long millisUntilFinished);
    public abstract void doOnFinish();
    @Override public void onTick(long millisUntilFinished) {
        m_iInvocationCnt++;
        if ( m_iBoard != null ) {
            m_iBoard.guiElementColorSwitch(m_guiElementToUseForFocus, m_player, m_focusEffect, m_iInvocationCnt, m_iTmpTxtOnElementDuringFeedback);
        } else {
            Log.w(TAG, "No iboard. Not switching color");
        }
        doOnTick(m_iInvocationCnt, millisUntilFinished);
    }

    @Override public void onFinish() {
        doOnFinish();
        cancelForPlayer();
    }
    private void cancelForPlayer() {
        m_iInvocationCnt = 0;
        if ( m_player == null ) { return; }
        if ( m_iBoard != null ) {
            m_iBoard.guiElementColorSwitch(m_guiElementToUseForFocus, m_player, m_focusEffect, m_iInvocationCnt, 0);
        }
        doChangeScoreIfRequired(m_player);
    }
    public void myCancel() {
        //doChangeScoreIfRequired();
        cancelForPlayer();
        super.cancel(); // final in parent ... can not be overwritten, hence 'myCancel()'
    }
    private void doChangeScoreIfRequired(Player p) {
        if ( m_iTmpTxtOnElementDuringFeedback != 0 ) {
            if ( m_iTmpTxtOnElementDuringFeedback != R.string.uc_undo ) {
                ScoreBoard.getMatchModel().changeScore(p);
            } else {
                ScoreBoard.getMatchModel().undoLast();
            }
            m_iTmpTxtOnElementDuringFeedback = 0;
        }
    }
}
