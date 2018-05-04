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
import com.doubleyellow.scoreboard.R;

/**
 * Dialog that simply shows privacy and terms of the app. Required by Google Play policies.
 */
public class PrivacyAndTerms extends BaseMessageDialog
{
    public PrivacyAndTerms(Context context) {
        super(context);
        m_mdWebViewResId = R.raw.privacy_and_terms;
        m_sTitle         = context.getString(R.string.privacy_and_terms) ;
    }
}
