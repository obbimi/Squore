package com.doubleyellow.scoreboard.dialog;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import com.doubleyellow.android.view.ViewUtil;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.view.MarkDownView;

public abstract class BaseMessageDialog extends BaseAlertDialog
{
    protected int m_mdWebViewResId = 0;
    protected int m_iMessageId     = 0;
    protected String m_sTitle      = null;
    protected String m_sMessage    = null;

    public BaseMessageDialog(Context context) {
        super(context, null, null);
    }

    @Override public boolean storeState(Bundle outState) {
        return true;
    }

    @Override public boolean init(Bundle outState) {
        return true;
    }

    @Override public void show() {
        View view = null;
        if ( m_mdWebViewResId != 0 ) {
            MarkDownView webView = new MarkDownView(context, null);
            webView.init(R.raw.privacy_and_terms);
            view = webView;
        }

        if ( view != null ) {
            adb.setView(view);
        }
        if ( m_sMessage != null ) {
            adb.setMessage(m_sMessage);
        }
        if ( m_iMessageId != 0 ) {
            adb.setMessage(m_iMessageId);
        }
        dialog = adb
                .setTitle(m_sTitle)
                .setPositiveButton(R.string.cmd_ok, null)
                .show();
        ViewUtil.setPackageIcon(context, dialog);
    }
}
