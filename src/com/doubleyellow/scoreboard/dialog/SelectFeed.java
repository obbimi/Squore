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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioGroup;

import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.match.MatchTabbed;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.prefs.ColorPrefs;
import com.doubleyellow.scoreboard.prefs.PreferenceKeys;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.scoreboard.prefs.URLsKeys;
import com.doubleyellow.android.view.SelectObjectView;
import com.doubleyellow.util.ListUtil;
import com.doubleyellow.util.MenuHandler;

import java.util.*;

/**
 * Dialog for switching feeds.
 */
public class SelectFeed extends BaseAlertDialog
{
    private String sNone = null;
    //private String sNewPublicFeed = null;

    public SelectFeed(Context context, Model matchModel, ScoreBoard scoreBoard) {
        super(context, matchModel, scoreBoard);
    }

    @Override public boolean storeState(Bundle outState) {
        return true;
    }

    @Override public boolean init(Bundle outState) {
        return true;
    }

    private SelectObjectView<String> sv;

    @Override public void show() {
        sNone = getString(R.string.lbl_none);
        //sNewPublicFeed = getString(R.string.add_new_feed);
        adb.setTitle(R.string.Switch_feed_elipses)
                .setIcon(R.drawable.ic_action_web_site)
                .setPositiveButton(R.string.cmd_ok    , listener)
                .setNeutralButton (R.string.uc_new  , listener)
                .setNegativeButton(R.string.cmd_delete, listener);

        // add a view with all possible feed names and let user choose one
        boolean bSortNames = PreferenceValues.isBrandTesting(context);
        Set<String> lNames = getFeedNames(bSortNames);
/*
    User must still be able to 'Add new...'
        if ( ListUtil.size(lNames) < 2 ) {
            Toast.makeText(context, ListUtil.size(lNames)==0?"No feeds defined":"Only one feed defined", Toast.LENGTH_LONG).show();
            return;
        }
*/

        final ArrayList<String> alNames = new ArrayList<>(lNames);
        String sCurrentFeedName = PreferenceValues.getMatchesFeedName(context);
        int iIndexPref = PreferenceValues.getInteger(PreferenceKeys.feedPostUrl, context, 0);
        int iIndexList = alNames.indexOf(sCurrentFeedName);
        if ( (iIndexList == -1) && (iIndexPref != -1) ) {
            sCurrentFeedName = ListUtil.size(alNames) > iIndexPref ? alNames.get(iIndexPref): sCurrentFeedName;
        }

        sv = new SelectObjectView<String>(context, alNames, sCurrentFeedName);
        ColorPrefs.setColors(sv, ColorPrefs.Tags.item);
        dialog = adb.setView(sv).create();

        sv.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(RadioGroup group, int checkedId) {
                // disable 'Delete' button if 'None' is selected
                Button btnDelete = dialog.getButton(DELETE);
                btnDelete.setEnabled(checkedId < ListUtil.size(alNames) - 1);
            }
        });

        dialog.show();
    }

    private Set<String> getFeedNames(boolean bSortNames) {
        //String sUrls = PreferenceValues.getString(PreferenceKeys.feedPostUrls, "", context);
        //List<Map<URLsKeys, String>> urlsList = PreferenceValues.getUrlsList(sUrls);
        //return MapUtil.listOfMaps2List(urlsList, URLsKeys.Name);
        Map<String, String> mName2Url = PreferenceValues.getFeedPostDetailMap(context, URLsKeys.Name, URLsKeys.FeedMatches, bSortNames);
        if ( mName2Url == null ) {
            mName2Url = new TreeMap<String, String>();
        }
        // always add the None
        if ( (mName2Url.containsKey(sNone) == false) && (PreferenceValues.isBrandTesting(context) == false) ) {
            mName2Url.put(sNone, "");
        }

        return mName2Url.keySet();
    }

    private DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
        @Override public void onClick(DialogInterface dialog, int which) {
            handleButtonClick(which);
        }
    };

    private static final int DELETE = DialogInterface.BUTTON_NEGATIVE;
    private static final int SELECT = DialogInterface.BUTTON_POSITIVE;
    private static final int NEW    = DialogInterface.BUTTON_NEUTRAL;

    @Override public void handleButtonClick(int which) {
        Object checkedValue  = sv.getChecked();
        String sCheckedValue = String.valueOf(checkedValue);
        switch (which) {
            case SELECT:
                int checkedIndex = sv.getCheckedIndex();
                if ( false /*sCheckedValue.equals(sNewPublicFeed)*/ ) {
                    if ( context instanceof MenuHandler) {
                        MenuHandler menuHandler = (MenuHandler) context;
                        menuHandler.handleMenuItem(R.id.uc_add_new_feed);
                    }
                } else {
                    if (sCheckedValue.equals(sNone)) {
                        checkedIndex = -1;
                    }
                    PreferenceValues.setActiveFeedNr(context, checkedIndex);
                    if (context instanceof MatchTabbed) {
                        MenuHandler menuHandler = (MenuHandler) context;
                        final Boolean useCache = Boolean.TRUE;
                        final Boolean resetFeedStatus = Boolean.TRUE;
                        menuHandler.handleMenuItem(R.id.refresh, useCache, resetFeedStatus);
                    }
                }
                break;
            case DELETE:
                // Delete the selected feed
                //String sActiveFeed = PreferenceValues.getFeedPostName(context);
                //if ( (sActiveFeed != null) && sActiveFeed.equals(checkedValue) ) {
                //    Toast.makeText(context, "You can not delete the currently active feed", Toast.LENGTH_LONG).show();
                //} else {
                    PreferenceValues.deleteFeedURL(context, sCheckedValue);
                //}
                if ( context instanceof MenuHandler) {
                    MenuHandler menuHandler = (MenuHandler) context;
                    menuHandler.handleMenuItem(R.id.uc_switch_feed); // restart this dialog
                }
                break;
            case NEW:
                if ( context instanceof MenuHandler) {
                    MenuHandler menuHandler = (MenuHandler) context;
                    menuHandler.handleMenuItem(R.id.uc_add_new_feed);
                }
                break;
        }
    }
}
