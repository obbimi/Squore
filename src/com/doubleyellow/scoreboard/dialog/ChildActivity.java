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
import android.os.Bundle;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.model.Model;

/**
 * Dialog that is not really a dialog. Just to be able to put a child dialog invocation onto the dialog stack.
 */
public class ChildActivity extends BaseAlertDialog
{
    public ChildActivity(Context context, Model matchModel, ScoreBoard scoreBoard) {
        super(context, matchModel, scoreBoard);
    }

    @Override public boolean storeState(Bundle outState) {
        outState.putInt(ChildActivity.class.getSimpleName(), m_iMenu);

        return true;
    }

    @Override public boolean init(Bundle bundle) {
        int iMenu = bundle.getInt(ChildActivity.class.getSimpleName());
        init(iMenu, m_oCtxValue);
        return true;
    }

    private int    m_iMenu     = 0;
    private Object m_oCtxValue = 0;

    public void init(int iMenu) {
        init(iMenu, null);
    }

    public void init(int iMenu, Object o) {
        this.m_iMenu = iMenu;
        this.m_oCtxValue = o;
    }

    @Override public void show() {
        scoreBoard.handleMenuItem(m_iMenu);
    }

    @Override public boolean isModal() {
        return false;
    }
}
