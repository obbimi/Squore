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

import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.android.view.MarkDownView;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;

public class Credits extends BaseAlertDialog
{
    public Credits(Context context) {
        super(context, null, null);
    }

    @Override public boolean storeState(Bundle outState) {
        return true;
    }

    @Override public boolean init(Bundle outState) {
        return true;
    }

    @Override public void show() {
        MarkDownView webView = new MarkDownView(context, null);
        int iResId = PreferenceValues.getSportSpecificSuffixedResId(context, R.raw.credits);

        webView.init( iResId );

        dialog = adb
                .setTitle("Credits")
                .setIcon(android.R.drawable.ic_menu_info_details)
                .setView(webView)
                .setPositiveButton(R.string.cmd_ok, null)
                .show();
    }
}
