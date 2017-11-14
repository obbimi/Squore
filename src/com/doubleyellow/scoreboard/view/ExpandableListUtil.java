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

package com.doubleyellow.scoreboard.view;

import android.util.Log;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ListView;
import com.doubleyellow.util.GroupStatusRecaller;
import com.doubleyellow.util.ListUtil;
import com.doubleyellow.android.util.SimpleELAdapter;

import java.util.List;
import java.util.Set;

public class ExpandableListUtil
{
    protected static final String TAG = "SB." + ExpandableListUtil.class.getSimpleName();

    private ExpandableListUtil(){};

    public static boolean expandFirst(ExpandableListView expandableListView) {
        return expand(expandableListView, 0);
    }
    public static boolean expandLast(ExpandableListView expandableListView) {
        return expand(expandableListView, -1);
    }
    public static boolean expand(ExpandableListView expandableListView, int iGroup) {
        if ( expandableListView == null ) return false;

        SimpleELAdapter adapter = (SimpleELAdapter) expandableListView.getExpandableListAdapter();
        if ( adapter == null ) {
            Log.w(TAG, expandableListView.getClass().getSimpleName() + ": no adapter yet (" + iGroup + ")");
            return false;
        }
        int headerViewsCount = adapter.getGroupCount();
        if ( headerViewsCount == 0 ) {
            Log.w(TAG, expandableListView.getClass().getSimpleName() + ": no headers yet (" + iGroup + ")");
            return false;
        }

        iGroup = (iGroup + headerViewsCount) % headerViewsCount; // so that -1 results in last
        if ( iGroup < headerViewsCount ) {
            expandableListView.expandGroup(iGroup);
            Log.w(TAG, expandableListView.getClass().getSimpleName() + ": EXPANDED(" + iGroup + ")");
            return true;
        } else {
            Log.w(TAG, expandableListView.getClass().getSimpleName() + ": not enough headers yet (" + iGroup + ")");
        }
        return false;
    }
    public static boolean expandAllOrFirst(ExpandableListView expandableListView, int iOnlyFirstIfMoreThan) {
        return expandAllOrFirstLast(expandableListView, iOnlyFirstIfMoreThan, true);
    }
    public static boolean expandAllOrLast(ExpandableListView expandableListView, int iOnlyLastIfMoreThan) {
        return expandAllOrFirstLast(expandableListView, iOnlyLastIfMoreThan, false);
    }
    private static boolean expandAllOrFirstLast(ExpandableListView expandableListView, int iOnlyFirstOrLastIfMoreThan, boolean bFirst) {
        if ( expandableListView == null ) return false;

        SimpleELAdapter adapter = (SimpleELAdapter) expandableListView.getExpandableListAdapter();
        if ( adapter == null ) {
          //Log.d(TAG, expandableListView.getClass().getSimpleName() + ": no adapter yet (all)");
            return false;
        }
        int childsCount = adapter.getGroupCount();
        if ( childsCount > iOnlyFirstOrLastIfMoreThan ) {
            if ( bFirst ) {
                expandFirst(expandableListView);
            } else {
                expandLast(expandableListView);
            }
        } else {
            expandAll(expandableListView);
        }
        return true;
    }

    public static boolean expandAll(ListView listView) {
        if ( listView instanceof ExpandableListView ) {
            return expandAll((ExpandableListView) listView);
        }
        return false;
    }
    public static boolean expandAll(ExpandableListView expandableListView) {
        if ( expandableListView == null ) return false;

        SimpleELAdapter adapter = (SimpleELAdapter) expandableListView.getExpandableListAdapter();
        if ( adapter == null ) {
            Log.w(TAG, expandableListView.getClass().getSimpleName() + ": no adapter yet (all)");
            return false;
        }
        int headerViewsCount = adapter.getGroupCount();
        if ( headerViewsCount == 0 ) {
            Log.w(TAG, expandableListView.getClass().getSimpleName() + ": no headers yet (all)");
            return false;
        }
        for(int iGroup=0; iGroup<headerViewsCount;iGroup++) {
            expandableListView.expandGroup(iGroup);
        }
        return true;
    }

    public static void expandGroups(ExpandableListView expListView, List<String> lGroups) {
        ExpandableListAdapter adapter = expListView.getExpandableListAdapter();
        if ( adapter instanceof SimpleELAdapter == false) { return; }
        SimpleELAdapter emsAdapter = (SimpleELAdapter) adapter;

        //collapseAll(expListView);
        for(String sGroup: lGroups) {
            int iGroupIndex = emsAdapter.getGroupIndex(sGroup);
            if ( iGroupIndex < 0 ) { continue; }
            expListView.expandGroup(iGroupIndex);
        }
    }

    public static void collapseAll(ExpandableListView expListView) {
        ExpandableListAdapter adapter = expListView.getExpandableListAdapter();
        if ( adapter == null ) { return; }
        for(int g=0; g < adapter.getGroupCount(); g++) {
            if ( expListView.isGroupExpanded(g) ) {
                expListView.collapseGroup(g);
            }
        }
    }

    public static int restoreStatus(ExpandableListView expListView, GroupStatusRecaller groupStatusRecaller) {
        Set<Integer> lToCollapse = groupStatusRecaller.getExpanded();
        if ( ListUtil.isEmpty(lToCollapse) ) { return 0; }
        Log.i(TAG, String.format("Attempting to restoreStatus %s for status %s ", lToCollapse.toString(), groupStatusRecaller.getMode()));

        collapseAll(expListView);
        int iCnt = 0;
        for(int g: lToCollapse) {
            try {
                expListView.expandGroup(g);
                iCnt++;
            } catch (Exception e) {
                //e.printStackTrace();
            }
        }
        return iCnt;
    }
}
