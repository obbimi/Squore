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
 * Enumeration for possible values of for the startup action.
 * See also
 * - res/values/arrays.xml#StartupActionValues (These MUST match enum values)
 * - res/values/arrays.xml#StartupActionDisplayValues (must have same number of entries as StartupActionValues)
 * - res/values-nl/arrays.xml#StartupActionDisplayValues (idem)
 */
public enum StartupAction {
    /* For DYBoxen comment out 'com.doubleyellow.scoreboard.R' and comment in private static class R */
    None              (0                    ),
    StartNewMatch     (R.id.dyn_new_match /* R.id.sb_new_match */   ),
    SelectFeedMatch   (R.id.sb_select_feed_match),
    SelectStaticMatch (R.id.sb_select_static_match),
    StartNewSglsMatch (R.id.sb_enter_singles_match),
    StartNewDblsMatch (R.id.sb_enter_doubles_match),
    EditPlayerNames   (R.id.sb_edit_event_or_player),
    Settings          (R.id.sb_settings     ),
    Feedback          (R.id.sb_feedback     ),
    ScoreDetails      (R.id.sb_score_details),
    QuickIntro        (R.id.sb_quick_intro  ),
    ChangeLog         (R.id.sb_change_log   ),
    ShowTimer         (R.id.sb_timer        ),
    ShowStoredmatches (R.id.sb_stored_matches),
    //PlayDemo          (R.id.sb_demo),
    ;

    private int iMenuOption = 0;
    StartupAction(int i) {
        this.iMenuOption = i;
    }
    public int getMenuId() {
        return iMenuOption;
    }
}
