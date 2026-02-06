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
import android.util.Log;
import android.view.View;
import android.webkit.WebView;

import com.doubleyellow.scoreboard.R;
import com.doubleyellow.android.view.MarkDownView;
import com.doubleyellow.scoreboard.URLFeedTask;

public abstract class BaseMessageDialog extends BaseAlertDialog
{
    private final String TAG = BaseMessageDialog.class.getSimpleName();

    protected int m_mdWebViewResId = 0;
    protected int m_iMessageId     = 0;
    protected String m_sTitle      = null;
    protected String m_sMessage    = null;
    protected boolean m_bIsAlert   = false;

    public BaseMessageDialog(Context context, boolean bIsAlert) {
        super(context, null, null);
        m_bIsAlert = bIsAlert;
    }
    public BaseMessageDialog(Context context) {
        this(context, false);
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
        if ( m_iMessageId != 0 ) {
            adb.setMessage(m_iMessageId);
        }
        if ( m_sMessage != null && m_sMessage.trim().toLowerCase().endsWith("html>") ) {
            try {
                WebView wv = new WebView(context); // seen this crash because of unclear reasons on android5
                //wv.loadData(sMsg, "text/html; charset=utf-8", "UTF-8"); // css not displayed
                //wv.loadData(sMsg, "text/html", "UTF-8"); // css not displayed
                wv.loadDataWithBaseURL(URLFeedTask.prefixWithBaseIfRequired("/"), m_sMessage, "text/html; charset=utf-8", "UTF-8", null); // allows css
                adb.setView(wv);
            } catch (Exception e) {
                Log.w(TAG, "Could not initialize webview");
                adb.setMessage(m_sMessage);
            }
        } else {
            adb.setMessage(m_sMessage);
        }

        adb.setIcon(m_bIsAlert? android.R.drawable.ic_dialog_alert: android.R.drawable.ic_dialog_info);

        dialog = adb
                .setTitle(m_sTitle)
                .setPositiveButton(R.string.cmd_ok, null)
                .show();

/*
        Object oDialog = dialog;
        if ( oDialog instanceof android.app.AlertDialog ) {
            ViewUtil.setPackageIcon(context, (AlertDialog) oDialog);
        }
*/
    }
}
