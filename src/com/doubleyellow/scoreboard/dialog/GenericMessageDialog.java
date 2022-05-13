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
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.model.Player;

/**
 * Dialog that simply shows a title and a message.
 * Used e.g. for messages coming 'back' from posting a match result to the internet.
 */
public class GenericMessageDialog extends BaseMessageDialog
{
    public GenericMessageDialog(Context context) {
        super(context);
    }
    public void init(String sTitle, String sMessage) {
        m_sMessage = sMessage;
        m_sTitle   = sTitle;
    }

    @Override public boolean storeState(Bundle outState) {
        outState.putString("T", m_sTitle);
        outState.putString("M", m_sMessage);
        return true;
    }

    @Override public boolean init(Bundle outState) {
        init(outState.getString("T"), outState.getString("M"));
        return true;
    }
}
