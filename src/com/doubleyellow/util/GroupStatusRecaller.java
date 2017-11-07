package com.doubleyellow.util;

import android.widget.ExpandableListView;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Class for helping to remember/persist expand/collapse status of and expandable list view
 *
 * expListView.setOnGroupExpandListener(groupStatusRecaller);
 * expListView.setOnGroupCollapseListener(groupStatusRecaller);
 *
 */
public class GroupStatusRecaller implements ExpandableListView.OnGroupCollapseListener, ExpandableListView.OnGroupExpandListener  {

    private static Map<String, GroupStatusRecaller> m_instances = new HashMap<String, GroupStatusRecaller>();
    public static GroupStatusRecaller getInstance(String sKey) {
        GroupStatusRecaller instance = m_instances.get(sKey);
        if ( instance == null ) {
            instance = new GroupStatusRecaller();
            instance.setMode("default");
            m_instances.put(sKey, instance);
        }
        return instance;
    }

    private GroupStatusRecaller() {}

    private String m_sMode = null;
    public void setMode(String sMode) {
        m_sMode = sMode;
        mStatus = m_modeStatus.get(m_sMode);
        if ( mStatus == null ) {
            mStatus = new LinkedHashSet<Integer>();
            m_modeStatus.put(m_sMode, mStatus);
        }
    }
    public String getMode() {
        return m_sMode;
    }

    public Set<Integer> getExpanded() {
        return new LinkedHashSet<Integer>(mStatus); // always return a copy when the status is requested
    }

    private Map<String, Set<Integer>> m_modeStatus = new HashMap<String, Set<Integer>>();
    private Set<Integer> mStatus = null;
    @Override public void onGroupCollapse(int groupPosition) {
        mStatus.remove(groupPosition);
    }

    @Override public void onGroupExpand(int groupPosition) {
        mStatus.add(groupPosition);
    }
}
