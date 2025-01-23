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

package com.doubleyellow.scoreboard.activity;

import android.content.Context;
import android.os.Bundle;
import android.view.*;
import android.widget.ExpandableListView;

import com.doubleyellow.android.util.SimpleELAdapter;
import com.doubleyellow.android.view.ViewUtil;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.prefs.ColorPrefs;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.util.*;

import java.util.*;

/**
 * Activity that allows user to select an conduct type to view more info about when this conduct is applicable.
 */
public class ConductInfo extends XActivity
{
    @Override protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        ScoreBoard.initAllowedOrientation(this);

        ExpandableListView expandableListView = new ExpandableListView(this);
        setContentView(expandableListView);

        ViewUtil.setFullScreen(getWindow(), PreferenceValues.showFullScreen(this));

        ColorPrefs.setColors(this, expandableListView);

        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final SimpleELAdapter listAdapter = getListAdapter(inflater);
        expandableListView.setAdapter(listAdapter);
        // open the first (and probably most applied) conduct: abuse of equipment
        expandableListView.expandGroup(0);
    }

    /** Populates the scoreBoard's options menu. */
    @Override public boolean onCreateOptionsMenu(Menu menu) {
        //MenuInflater inflater = getMenuInflater();
        //inflater.inflate(R.menu.closeonlymenu, menu); // we provide the 'Up' button via PARENT_ACTIVITY in AndroidManifest

        return true;
    }

    /** Handles the user's menu selection. */
    @Override public boolean onOptionsItemSelected(MenuItem item) {
        this.finish();
        return true;
    }

    private EMSAdapter emsAdapter;
    private SimpleELAdapter getListAdapter(LayoutInflater inflater) {
        if ( emsAdapter == null ) {
            emsAdapter = new EMSAdapter(inflater);
        }
        return emsAdapter;
    }

    public static final String HEADER_PREFIX = "-";

    private class EMSAdapter extends SimpleELAdapter
    {
        private EMSAdapter(LayoutInflater inflater)
        {
            super(inflater, R.layout.expandable_match_selector_group, R.layout.expandable_match_selector_item, null, true);
        }

        public void load(boolean bUseCacheIfPresent) {
            String sContent = getString(R.string.conducts_guidelines_default);

            List<String> lInput = new ArrayList<String>(Arrays.asList(sContent.split("\n")));
            ListUtil.removeEmpty(lInput);
            String sHeader = null;
            for(String sOffence: lInput) {
                sOffence = sOffence.trim();
                if ( StringUtil.isEmpty(sOffence)) { continue; }
                if ( sOffence.startsWith(HEADER_PREFIX) ) {
                    sHeader = sOffence.replaceFirst(HEADER_PREFIX, "").trim();
                    sHeader = sHeader.replaceAll("\\(.*\\)", ""); // for header in app remove info between brackets
                    sHeader = sHeader.replaceAll(":$", "").trim();
                    continue;
                }
                super.addItem(sHeader, sOffence);
            }
        }
    }
}
