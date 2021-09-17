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

package com.doubleyellow.scoreboard.timer;

import android.os.Bundle;
import com.doubleyellow.scoreboard.dialog.BaseAlertDialog;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.model.Model;

/**
 * Dialog that is not really a dialog. Just to invoke one of 2 timer views.
 * Introduced to be able to add timers into the 'dialog' stack.
 */
public abstract class TwoTimerView extends BaseAlertDialog
{
    private static String TAG = "SB." + TwoTimerView.class.getSimpleName();
    TwoTimerView(ScoreBoard scoreBoard, Model matchModel) {
        super(scoreBoard, matchModel, scoreBoard);
    }

    @Override public boolean storeState(Bundle outState) {
        outState.putSerializable(TimerView.TIMER_SHOWN    , m_timerType);

        Timer timer = scoreBoard.timer;
        if ( timer == null ) {
            return false;
        }
        outState.putBoolean     (TimerView.TIMER_AUTO_TRIGGERED , timer.isAutoTriggered());

        return true;
    }

    private boolean m_bAutoTriggered  = false;
    @Override public boolean init(Bundle bundle) {
        //show(false); // deliberately not triggered here: when 10-10 is reached and screen is rotated... it will show because onCreate will trigger it again
        Type           timerType = (Type)     bundle.getSerializable(TimerView.TIMER_SHOWN);
      //ViewType       viewType  = (ViewType) bundle.getSerializable(TimerView.TIMER_VIEW_TYPE);
        boolean bAutoTriggered   = bundle.getBoolean(TimerView.TIMER_AUTO_TRIGGERED, false);
        init(timerType, bAutoTriggered);

        return true;
    }
    public abstract void init(boolean bAutoTriggered);

    protected void init(Type timerType, boolean bAutoTriggered) {
        m_timerType      = timerType;
        m_bAutoTriggered = bAutoTriggered;
    }

    Type     m_timerType = null;
    @Override public void show() {
        scoreBoard._showTimer(m_timerType, m_bAutoTriggered, null, null);
    }

    @Override public boolean isShowing() {
        if ( scoreBoard == null ) { return false; }
        if ( scoreBoard.timer == null ) { return false; }
        return scoreBoard.timer.isShowing();
    }

/*
    @Override public boolean isModal() {
        return m_viewType.equals(ViewType.Popup);
    }
*/

    @Override public void dismiss() {
        scoreBoard.cancelTimer();
    }
}
