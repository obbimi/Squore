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

package com.doubleyellow.scoreboard.main;

import android.content.Context;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import androidx.drawerlayout.widget.DrawerLayout;

import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.prefs.PreferenceKeys;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.util.ListUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

class MenuDrawerAdapter extends BaseAdapter implements ListView.OnItemClickListener, View.OnClickListener, DrawerLayout.DrawerListener {
    public static final boolean m_bHideDrawerItemsFromOldMenu = false; // TODO: maybe later
    private static final String TAG = "SB." + MenuDrawerAdapter.class.getSimpleName();
    private List<Integer> l_idSequence    = new ArrayList<>();
    private final LinkedHashMap<Integer,Integer> id2String = new LinkedHashMap<>();
    private final SparseIntArray id2Image  = new SparseIntArray();
    private LayoutInflater inflater  = null;

    ScoreBoard scoreBoard = null;
    List<Integer> hideMenuItems = null;
    MenuDrawerAdapter(ScoreBoard scoreBoard) {
        this.scoreBoard    = scoreBoard;
        this.hideMenuItems = PreferenceValues.getMenuItemsToHide(scoreBoard);

        inflater = (LayoutInflater)scoreBoard.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        int iOfficialRulesResId = PreferenceValues.getSportTypeSpecificResId(scoreBoard, R.string.sb_official_rules__Squash, 0);
        if ( startSection(R.string.uc_new, R.id.uc_new ) ) {
            addItem(R.id.sb_enter_singles_match , R.string.sb_new_singles_match    ,         R.drawable.circled_plus          , PreferenceKeys.useSinglesMatchesTab       , R.bool.useSinglesMatch__Default   );
            addItem(R.id.sb_select_static_match , R.string.sb_select_static_match  ,         R.drawable.ic_action_view_as_list, PreferenceKeys.useMyListFunctionality     , R.bool.useMyListFunctionality__Default);
            addItem(R.id.sb_select_feed_match   , R.string.sb_select_feed_match    ,         R.drawable.ic_action_web_site    , PreferenceKeys.useFeedAndPostFunctionality, R.bool.useMatchFromFeed__Default  );
            addItem(R.id.sb_enter_doubles_match , R.string.sb_new_doubles_match    ,         R.drawable.circled_plus          , PreferenceKeys.useSinglesMatchesTab       , R.bool.useDoublesMatch__Default );
        }

        if ( startSection(R.string.uc_edit, R.id.uc_edit   ) ) {
            addItem(R.id.sb_clear_score         , R.string.sb_clear_score          ,         R.drawable.circle_2arrows   );
            addItem(R.id.sb_adjust_score        , R.string.sb_adjust_score         , android.R.drawable.ic_menu_edit        );
            addItem(R.id.sb_edit_event_or_player, R.string.sb_edit_event_or_player , android.R.drawable.ic_menu_edit        );
            addItem(R.id.change_match_format    , R.string.pref_MatchFormat        ,         R.drawable.ic_action_mouse     );
        }
        if ( startSection(R.string.uc_show, R.id.uc_show   ) ) {
            addItem(R.id.sb_toss                , R.string.sb_cmd_toss             ,         R.drawable.toss_white          );
            addItem(R.id.sb_timer               , R.string.sb_timer                ,         R.drawable.timer               );
            addItem(R.id.sb_injury_timer        , R.string.sb_injury_timer         ,         R.drawable.timer               , R.bool.useInjuryTimers__Squash);
            addItem(R.id.sb_player_timeout_timer, R.string.sb_player_timeout_timer ,         R.drawable.timer               , R.bool.usePlayerTimeoutTimers__Squash);
            addItem(R.id.sb_score_details       , R.string.sb_score_details        ,         R.drawable.ic_action_chart_line);
        }
        if ( startSection(R.string.goto_help, R.id.sb_sub_help ) ) {
            addItem(R.id.sb_quick_intro         , R.string.Quick_intro             , android.R.drawable.ic_dialog_info         );
            addItem(R.id.sb_help                , R.string.goto_help               , android.R.drawable.ic_menu_help           );
            addItem(R.id.sb_official_rules      , iOfficialRulesResId              , android.R.drawable.ic_menu_search    , R.bool.showLiveScore__Default     );
            addItem(R.id.sb_live_score          , R.string.Live_Score              ,         R.drawable.ic_action_web_site, R.bool.showLiveScore__Default     );
            addItem(R.id.sb_feedback            , R.string.cmd_feedback            ,         R.drawable.ic_action_import_export);
        }
        if ( startSection(R.string.pref_Other, 0 ) ) {
            addItem(R.id.sb_settings            , R.string.settings                , R.drawable.ic_action_settings    );
            addItem(R.id.sb_stored_matches      , R.string.sb_stored_matches       , R.drawable.ic_action_view_as_list);
        }
        if ( startSection(R.string.ImportExport_elipses, R.id.uc_import_export ) ) {
            addItem(R.id.cmd_import_matches     , R.string.import_matches          , android.R.drawable.stat_sys_download);
            addItem(R.id.cmd_export_matches     , R.string.export_matches          , android.R.drawable.ic_menu_upload);
        }

        if (ListUtil.isNotEmpty(hideMenuItems) ) {
            for(Integer iMenuItemId: hideMenuItems ) {
                id2String.remove(iMenuItemId);
            }
        }

        if ( ListUtil.size(l_idSequence) == 0 ) {
            l_idSequence.addAll(id2String.keySet());
        }
    }

    private boolean startSection(int iCaptionId, int iIdInMainMenu) {
        if ( this.hideMenuItems.contains(iIdInMainMenu) ) {
            return false;
        }
        id2String.put(iCaptionId, iCaptionId);
        return true;
    }
    private void addItem(int iActionId, int iCaptionId, int iImageId) {
        addItem(iActionId, iCaptionId, iImageId, 0);
    }
    private void addItem(int iActionId, int iCaptionId, int iImageId, int iShowResid) {
        if ( iShowResid != 0 ) {
            iShowResid = PreferenceValues.getSportTypeSpecificResId(scoreBoard, iShowResid, iShowResid);
            boolean bShow = scoreBoard.getResources().getBoolean(iShowResid);
            if ( bShow == false ) {
                Log.d(TAG, "Specifically not showing " + scoreBoard.getResources().getResourceName(iActionId) );
                return;
            }
        }
        if ( iCaptionId == 0 ) {
            // e.g. for Tennis AND Padel rules... menu option
            return;
        }
        id2String.put(iActionId , iCaptionId);
        if ( iImageId != 0 ) {
            id2Image .put(iActionId , iImageId  );
        }
/*
        if ( m_bHideDrawerItemsFromOldMenu && (mainMenu != null) ) {
            // does not work if menu not yet inflated
            ViewUtil.hideMenuItemForEver(mainMenu, iActionId);
        }
*/
    }
    private void addItem(int iActionId, int iCaptionId, int iImageId, PreferenceKeys prefBoolean, int iShowResidDefault) {
        if ( prefBoolean != null ) {
            boolean bShow = PreferenceValues._getBoolean(prefBoolean, scoreBoard, iShowResidDefault);
            if ( bShow == false ) {
                Log.d(TAG, "Specifically not showing " + scoreBoard.getResources().getResourceName(iActionId) );
                return;
            }
        }
        if ( iCaptionId == 0 ) {
            // e.g. for Tennis AND Padel rules... menu option
            return;
        }
        id2String.put(iActionId , iCaptionId);
        if ( iImageId != 0 ) {
            id2Image .put(iActionId , iImageId  );
        }
/*
        if ( m_bHideDrawerItemsFromOldMenu && (mainMenu != null) ) {
            // does not work if menu not yet inflated
            ViewUtil.hideMenuItemForEver(mainMenu, iActionId);
        }
*/
    }

    @Override public int getCount() {
        return l_idSequence.size();
    }

    @Override public Object getItem(int position) {
        return l_idSequence.get(position);
    }

    @Override public long getItemId(int position) {
        return l_idSequence.get(position);
    }

    @Override public View getView(int position, View convertView, ViewGroup parent) {
        View view;

        int iId = l_idSequence.get(position);
        Integer iResImage = id2Image.get(iId);
        int iResIdTxt = id2String.get(iId);

        if ( iResIdTxt == iId ) {
            view = inflater.inflate(R.layout.list_item_section, null);
            view.setOnClickListener(null);
            view.setOnLongClickListener(null);
            view.setLongClickable(false);
            final TextView sectionView = view.findViewById(R.id.list_item_section_text);
            sectionView.setText(iResIdTxt);
        } else {
            view = inflater.inflate(R.layout.image_item, null /*viewGroupParent*/);
            TextView    text = view.findViewById(R.id.image_item_text);
            ImageButton img  = view.findViewById(R.id.image_item_image);
            img.setImageResource(iResImage);
            text.setText(iResIdTxt);
    /*
                text.setOnClickListener(this);
    */
            img.setOnClickListener(this);
            img.setTag(iId);
            view.setOnClickListener(this);
            view.setTag(iId);
        }
        return view;
    }

    @Override public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        scoreBoard.handleDrawerMenuItem(l_idSequence.get(position));
    }

    //
    // OnClickListener
    //
    @Override public void onClick(View v) {
        Object tag = v.getTag();
        if ( tag instanceof Integer ) {
            Integer iId = (Integer) tag;
            scoreBoard.handleDrawerMenuItem(iId);
        }
    }

    //
    // DrawerListener
    //

    @Override public void onDrawerStateChanged(int newState) { }
    @Override public void onDrawerSlide(View drawerView, float slideOffset) { }
    @Override public void onDrawerOpened(View drawerView) {
        //getXActionBar().setTitle(mDrawerTitle);
        //invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
    }
    @Override public void onDrawerClosed(View drawerView) {
        //getXActionBar().setTitle(mTitle);
        //invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
    }
}
