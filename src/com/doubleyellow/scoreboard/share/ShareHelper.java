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

import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.scoreboard.share.ResultMailer;
import com.doubleyellow.scoreboard.share.ResultSender;

public class ShareHelper
{
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
