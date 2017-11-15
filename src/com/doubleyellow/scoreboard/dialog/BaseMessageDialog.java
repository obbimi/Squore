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
import android.view.View;
import com.doubleyellow.android.view.ViewUtil;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.android.view.MarkDownView;

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
