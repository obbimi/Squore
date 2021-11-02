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

package com.doubleyellow.scoreboard.prefs;

import com.doubleyellow.scoreboard.R;

/**
 * Share how/when
 * - link
 * - summary
 * - post to site
 * - plain text sheet
 */
public enum ShareMatchPrefs {
    /** Keep values/arrays-xx/string-array[@name='ShareMatchPrefsDisplayValues'] in sync */
    LinkWithFullDetails        (R.id.sb_share_score_sheet             , android.R.drawable.ic_menu_share     , false),
//    LinkWithFullDetailsEachGame(R.id.sb_share_score_sheet             , android.R.drawable.ic_menu_share     , true),
//    LinkWithFullDetailsEachHalf(R.id.sb_share_score_sheet             , android.R.drawable.ic_menu_share     , true),
//    LinkWithFullDetailsEachPoint(R.id.sb_share_score_sheet            , android.R.drawable.ic_menu_share     , true),
    Summary                    (R.id.sb_share_match_summary           , android.R.drawable.ic_menu_share     , false),
    SummaryMultiple            (R.id.sb_share_matches_summary         , android.R.drawable.ic_menu_share     , false),
    SummaryToDefault           (R.id.sb_send_match_result             ,         R.drawable.ic_action_send_now, false), // only if default sms recipient specified
    PostResult                 (R.id.sb_post_match_result             ,         R.drawable.arrow_right       , false), // only if post url specified
    SheetInEmail               (R.id.sb_email_match_result            , android.R.drawable.ic_dialog_email   , false),
    //SummaryToClipboard       (R.id.sb_put_match_summary_on_clipboard, android.R.drawable.ic_menu_share   , false),
    //SummaryToWhatsApp        (R.id.sb_whatsapp_match_summary        , android.R.drawable.ic_menu_share   , false),
    ;

    private int     iMenuOption = 0;
    private int     iDrawableId = 0;
    private boolean bAlsoBeforeMatchEnd = false;
    ShareMatchPrefs(int i, int d, boolean b) {
        this.iMenuOption = i;
        this.iDrawableId = d;
        this.bAlsoBeforeMatchEnd = b;
    }
    public int getMenuId() {
        return iMenuOption;
    }
    public int getDrawableId() {
        return iDrawableId;
    }
    public int getDrawableIdBlack() {
        if ( iDrawableId == R.drawable.arrow_right ) {
            return R.drawable.arrow_right_black;
        }
        return iDrawableId;
    }
    public boolean alsoBeforeMatchEnd() {
        return bAlsoBeforeMatchEnd;
    }
}
