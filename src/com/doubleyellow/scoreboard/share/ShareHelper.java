/*
 * Copyright (C) 2018  Iddo Hoeve
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

package com.doubleyellow.scoreboard.share;

import android.content.Context;

import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;

import java.util.HashMap;
import java.util.Map;

public class ShareHelper
{
    public static Map<Integer, String> m_menuResIdToPackage = null;
    static {
        m_menuResIdToPackage = new HashMap<>();
        m_menuResIdToPackage.put(R.id.sb_whatsapp_match_summary     , "com.whatsapp");
        m_menuResIdToPackage.put(R.id.sb_twitter_match_summary      , "com.twitter.android");
        m_menuResIdToPackage.put(R.id.sb_instagram_match_summary    , "com.instagram.android");
        m_menuResIdToPackage.put(R.id.sb_facebook_match_summary     , "com.facebook.katana");
        m_menuResIdToPackage.put(R.id.sb_facebook_lite_match_summary, "com.facebook.lite");
    }

    /** Sending match result as SMS message to e.g. the boxmaster */
    public static void shareMatchSummary(Context context, Model matchModel, String sPackage, String sDefaultRecipient) {
        ResultSender resultSender = new ResultSender();
        resultSender.send(context, matchModel, sPackage, sDefaultRecipient);
    }

    public static void emailMatchResult(Context context, Model matchModel) {
        ResultMailer resultMailer = new ResultMailer();
        boolean bHtml = PreferenceValues.mailFullScoringSheet(context);
        resultMailer.mail(context, matchModel, bHtml);
    }

}
