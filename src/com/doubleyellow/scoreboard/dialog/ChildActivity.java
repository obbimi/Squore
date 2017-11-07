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
