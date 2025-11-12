/*
 * Copyright (C) 2025  Iddo Hoeve
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

import java.util.Arrays;
import java.util.List;

/**
 * Use {@link PreferenceKeys#kioskMode}
 * setting to control that certain menu items/buttons are made invisible in order to not overwhelm the end user
 * <br/>
 * It should be set to one of the enum values of {@link KioskMode}
 * <br/>
 * Fine-tuning of visible menu items/buttons can be done by specifying additional settings
 * <ul>
 *     <li>{@link PreferenceKeys#hideMenuItems}, hiding additional menu items</li>
 *     <li>{@link PreferenceKeys#showMenuItems}, showing menu items you DON'T want to be hidden by the kiosk mode setting</li>
 * </ul>
 * <br/>
 * Values in hide/show Menu items should be an array of strings referring to 'id' values of mainmenu.xml (main activity) or matchtabbedmenu.xml (new match activity)
 * E.g. "showMenuItems": [ "sb_settings" ]
 * E.g. "hideMenuItems": [ "sb_exit" ]
 **/
public enum KioskMode {
    NotUsed(new Integer[]{}),
    MatchesFromSingleFeed_1( new Integer[]
          { R.id.media_route_menu_item
          , R.id.dyn_end_game
          , -1 * R.id.uc_new
          ,      R.id.sb_select_static_match
          ,      R.id.sb_enter_singles_match
          ,      R.id.sb_enter_doubles_match
          , -1 * R.id.uc_edit
          ,      R.id.change_match_format
          ,      R.id.sb_edit_event_or_player
          , R.id.uc_show
          ,      R.id.sb_match_format
          ,      R.id.sb_stored_matches
          ,      R.id.sb_possible_conductsA
          ,      R.id.sb_official_rules
          ,      R.id.sb_official_announcement
          ,      R.id.sb_live_score
          , R.id.sb_mqtt_mirror
          , R.id.sb_bluetooth
          , R.id.sb_sub_share_match
          , R.id.sb_settings
          , R.id.uc_import_export
          , R.id.sb_sub_help
          , R.id.sb_feedback

          , R.id.uc_switch_feed
          , R.id.uc_add_new_feed
          , R.id.mt_open_feed_url
          , R.id.mt_close
          }),
    PublishedMQTTMatchesOnly_1( new Integer[]
          { R.id.media_route_menu_item
          , R.id.dyn_end_game
          , R.id.dyn_new_match
          , R.id.uc_new
          ,      R.id.sb_clear_score
          ,      R.id.sb_select_feed_match
          ,      R.id.sb_select_static_match
          ,      R.id.sb_enter_singles_match
          ,      R.id.sb_enter_doubles_match
          , -1 * R.id.uc_edit
          ,      R.id.change_match_format
          ,      R.id.sb_edit_event_or_player
          , R.id.uc_show
          ,      R.id.sb_match_format
          ,      R.id.sb_stored_matches
          ,      R.id.sb_possible_conductsA
          ,      R.id.sb_official_rules
          ,      R.id.sb_official_announcement
          ,      R.id.sb_live_score
          , R.id.sb_mqtt_mirror
          , R.id.sb_bluetooth
          , R.id.sb_sub_share_match
          , R.id.sb_settings
          , R.id.uc_import_export
          , R.id.sb_sub_help
          , R.id.sb_feedback

          , R.id.uc_switch_feed
          , R.id.uc_add_new_feed
          , R.id.mt_open_feed_url
          , R.id.mt_close
          }),
    ManualMatchesOnly1( new Integer[]
          { R.id.media_route_menu_item
          , R.id.dyn_end_game
          , -1 * R.id.uc_new
          ,      R.id.sb_select_static_match
          ,      R.id.sb_select_feed_match
          , R.id.uc_show
          ,      R.id.sb_match_format
          ,      R.id.sb_live_score
          , R.id.sb_mqtt_mirror
          , R.id.sb_bluetooth
          , R.id.uc_import_export
          , R.id.sb_sub_help
          , R.id.sb_feedback
          }),
    ;
    private List<Integer> m_iHideMenuItems = null;
    KioskMode(Integer[] iHideMenuItems) {
        m_iHideMenuItems = Arrays.asList(iHideMenuItems) ;
    }
    public boolean hideMenuItem(int i) {
        return m_iHideMenuItems.contains(i);
    }
    public List<Integer> hideMenuItems() {
        return m_iHideMenuItems;
    }
}
