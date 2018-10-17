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

package com.doubleyellow.android.util;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.doubleyellow.android.view.ViewUtil;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.prefs.ColorPrefs;
import com.doubleyellow.util.FileUtil;
import com.doubleyellow.util.ListUtil;
import com.doubleyellow.util.MapUtil;
import com.doubleyellow.util.SortOrder;
import com.doubleyellow.util.StringComparator;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Base adapter, used for selecting a match or a feed.
 * Subclasses are defined in:
 * - FeedMatchSelector
 * - StaticMatchSelector
 * - PreviousMatchSelector
 * - FeedFeedSelector
 * - ConductInfo
 */
public abstract class SimpleELAdapter extends android.widget.BaseExpandableListAdapter
{
    protected static final String TAG = "SB." + SimpleELAdapter.class.getSimpleName();

    private List<String>                  m_lHeaders           = new ArrayList<String>();     // filtered
    private HashMap<String, List<String>> m_lHeader2Childs     = new LinkedHashMap<String, List<String>>(); // only childs have been filtered
    private List<String>                  m_lHeadersFull       = new ArrayList<String>();
    private HashMap<String, List<String>> m_lHeader2ChildsFull = new LinkedHashMap<String, List<String>>();
    private Map<String, Object>           m_mObjects           = new HashMap<String, Object> ();
    private Map<String, Boolean>          m_mSelected          = new HashMap<String, Boolean>();

    private LayoutInflater                inflater             = null;
    private Context                       ctx                  = null;
    private int                           m_iResId2inflateH    = 0;
    private int                           m_iResId2inflateI    = 0;

    public void copyContentFrom(SimpleELAdapter el) {
        this.clear();
        this.m_lHeadersFull      .addAll(el.m_lHeadersFull      );
        this.m_lHeader2ChildsFull.putAll(el.m_lHeader2ChildsFull);
        this.m_mObjects          .putAll(el.m_mObjects          );
    }
    private static final boolean B_DIFFERENT_TXT_SIZE_PER_ORIENTATION = true;
    private float iTxtSmall  = 0f;
    private float iTxtMedium = 0f;

    public abstract void load(boolean bUseCacheIfPresent);

    protected String sFetchingDataMessage = null;

    protected SimpleELAdapter(LayoutInflater inflater, int iHeaderResId, int iItemResId, String sFetchingDataMessage, boolean bAutoLoad) {
        this.m_iResId2inflateI    = iItemResId;
        this.m_iResId2inflateH    = iHeaderResId;
        this.sFetchingDataMessage = sFetchingDataMessage;
        this.inflater             = inflater;
        if ( bAutoLoad ) {
            load(true);
        }
        if ( B_DIFFERENT_TXT_SIZE_PER_ORIENTATION && (inflater != null) ) {
            ctx = inflater.getContext();
        }
    }

    private   void setHeaderSorter(Comparator<String> headerSorter) {
        this.m_headerSorter = headerSorter;
    }
    private   void setItemSorter(Comparator<String> itemSorter) {
        this.m_itemSorter = itemSorter;
    }
    protected void sortHeaders(SortOrder sortOrder) {
        setHeaderSorter(new StringComparator<String>(sortOrder));
    }
    protected void sortItems(SortOrder sortOrder) {
        setItemSorter(new StringComparator<String>(sortOrder));
    }
    private transient Comparator<String> m_headerSorter = null;
    private transient Comparator<String> m_itemSorter   = null;

    public void addItems(String sGroup, List<String> lItems) {
        if ( (m_itemSorter != null) && (lItems != null)) {
            Collections.sort(lItems, m_itemSorter);
        }
        m_lHeader2ChildsFull.put(sGroup, lItems);
        addHeader(sGroup);
    }

    private void addHeader(String sGroup) {
        if ( sGroup == null ) {
            Log.w("SB.WARN", "**** adding header NULL ***");
        }
        m_lHeadersFull.add(sGroup);
        if ( m_headerSorter != null ) {
            Collections.sort(m_lHeadersFull, m_headerSorter);
        }
    }

    public void clear() {
        m_lHeadersFull.clear();
        m_lHeader2ChildsFull.clear();
        m_mObjects.clear();
        m_mSelected.clear();
        bRefilterRequired = true; // it could be no more items will be added
        filterData("");
    }

    protected void removeHeader(String sGroup) {
        m_lHeadersFull.remove(sGroup);
        m_lHeaders.remove(sGroup); // no need to refilter
        List<String> lItems = m_lHeader2ChildsFull.remove(sGroup);

        if ( lItems != null ) {
            for(String sItem: lItems) {
                Object remove = m_mObjects.remove(getKey(sGroup, sItem));
            }
        }
    }

    protected void addItem(String sGroup, String sItem) {
        addItem(sGroup, sItem, null);
    }
    public Object addItem(String sGroup, String sItem, Object o) {
        List<String> lItems = m_lHeader2ChildsFull.get(sGroup);
        if ( lItems == null ) {
            lItems = new ArrayList<String>();
            m_lHeader2ChildsFull.put(sGroup, lItems);
            addHeader(sGroup);
        }
        if ( sItem == null ) { return null; }

        lItems.add(sItem);
        bRefilterRequired = true;
        if ( m_itemSorter != null ) {
            Collections.sort(lItems, m_itemSorter);
        }

        Object oOld = m_mObjects.put(getKey(sGroup, sItem), o);
        if ( oOld != null ) {
            Log.w(TAG, String.format("%s and %s had the same key fields", o, oOld));
        }
        return oOld;
    }

    private String getKey(String sGroup, String sItem) {
        return sGroup + "__" + sItem;
    }

    public Object getObject(int iGroup, int iItem) {
        String sKey = getKey(iGroup, iItem);
        return getObject(sKey);
    }
    public Object getObject(String sGroup, String sItem) {
        String sKey = getKey(sGroup, sItem);
        return getObject(sKey);
    }

    private Object getObject(String sKey) {
        if ( sKey == null ) { return null; }
        return m_mObjects.get(sKey);
    }

    private String getKey(int iGroup, int iItem) {
        try {
            String sGroup = m_lHeaders.get(iGroup);
            if ( sGroup == null ) { return null; }
            List<String> childs = m_lHeader2Childs.get(sGroup);
            String sItem  = childs.get(iItem);
            return sGroup + "__" + sItem;
        } catch (Exception e) {
            // should e.g. only occur if user clicked on item while list is being refreshed in the background
            e.printStackTrace();
            return null;
        }
    }

    public List<? extends Object> getObjects(int iGroup) {
        String sGroup = m_lHeaders.get(iGroup);
        return getObjects(sGroup);
    }

    public List<? extends Object> getObjects(String sGroup) {
        List<Object> lObjects = new ArrayList<Object>();
        List<String> items = m_lHeader2Childs.get(sGroup);
        if ( ListUtil.isEmpty(items) ) { return lObjects; }
        for(String sItem: items) {
            lObjects.add(getObject(sGroup, sItem));
        }
        return lObjects;
    }

    public void setSelected(View v, int iGroup, int iChild) {
        String sKey = getKey(iGroup, iChild);
        m_mSelected.put(sKey, true);
        setTextViewSelectionVisualFeedback(v, true);
    }
    public boolean toggleSelection(View v, int iGroup, int iChild) {
        String sKey = getKey(iGroup, iChild);
        return toggleSelection(v, sKey);
    }

    private boolean toggleSelection(View v, String sKey) {
        boolean bIsSelected = isSelected(sKey);

        bIsSelected = (bIsSelected==false);
        m_mSelected.put(sKey, bIsSelected);
        setTextViewSelectionVisualFeedback(v, bIsSelected);

        return bIsSelected;
    }

    private boolean isSelected(String sKey) {
        Boolean aBoolean = m_mSelected.get(sKey);
        return aBoolean != null && aBoolean.equals(Boolean.TRUE);
    }

    public void selectAll() {
        for(String sHeader: m_lHeaders) {
            List<String> lItems = m_lHeader2Childs.get(sHeader);
            for(String sItem: lItems) {
                String key = getKey(sHeader, sItem);
                if ( isSelected(key) == false ) {
                    toggleSelection(null, key);
                }
            }
        }
    }
    public void deselectAll() {
        m_mSelected.clear();
    }
    public List removeSelectedItems() {
        ArrayList<Object> list = new ArrayList<Object>();
        for(String sHeader: m_lHeaders) {
            List<String> lItems = m_lHeader2Childs.get(sHeader);
            List<String> lItemsCopy = new ArrayList<String>(lItems); // to iterator over but allow remove to be called on actual list
            for(String sItem: lItemsCopy) {
                String key = getKey(sHeader, sItem);
                if ( isSelected(key) ) {
                    lItems.remove(sItem);
                    Object o = getObject(key);
                    if ( o != null) {
                        list.add(o);
                    } else {
                        list.add(sItem);
                    }
                }
            }
        }
        return list;
    }

    private void setTextViewSelectionVisualFeedback(View v, boolean bSelected) {
        if ( v instanceof TextView == false) { return; }

        TextView textView = (TextView) v;
        if ( bSelected ) {
            textView.setText(textView.getText() + " (X)");
        } else {
            textView.setText(textView.getText().toString().replace(" (X)", ""));
        }
    }
    private String getChildText(int groupPosition, int childPosition) {
        Object group = getGroup(groupPosition);
        List<String> childs = m_lHeader2Childs.get(group);

        if ( childPosition < ListUtil.size(childs) ) {
            return childs.get(childPosition);
        } else {
            return null;
        }
    }

    //---------------------------------------
    // Interface ExpandableListAdapter
    //---------------------------------------

    @Override public Object  getChild         (int groupPosition, int childPosition) {
        Object oChild = getObject(groupPosition, childPosition);
        if ( oChild != null ) {
            return oChild;
        }

        return getChildText(groupPosition, childPosition);
    }

    @Override public long    getChildId       (int groupPosition, int childPosition) { return childPosition; }
    @Override public boolean isChildSelectable(int groupPosition, int childPosition) { return true; }
    @Override public Object  getGroup         (int groupPosition) { return (groupPosition>=0 && groupPosition<m_lHeaders.size()) ? m_lHeaders.get(groupPosition): null; }
    @Override public long    getGroupId       (int groupPosition) { return groupPosition; }
    @Override public int     getChildrenCount (int groupPosition) {
        if ( bRefilterRequired ) {
            refreshForFilter();
        }
        return ListUtil.size(m_lHeader2Childs.get(getGroup(groupPosition)));
    }
              public int     getChildrenCount () { return MapUtil.size(m_mObjects); }
    @Override public int     getGroupCount() {
        if ( bRefilterRequired ) {
            refreshForFilter();
        }
        return ListUtil.size(m_lHeaders);
    }
    @Override public boolean hasStableIds() { return false; }

    public int getGroupIndex(String s) { return m_lHeaders.indexOf(s); }

/*
    @Override public int getGroupTypeCount() {
        int groupTypeCount = super.getGroupTypeCount();
        return groupTypeCount;
    }

    @Override public int getGroupType(int groupPosition) {
        int groupType = super.getGroupType(groupPosition);
        return groupPosition;
    }
*/

    @Override public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        if ( (groupPosition == 0) && B_DIFFERENT_TXT_SIZE_PER_ORIENTATION && (ctx != null) ) {
            Resources resources = ctx.getResources();
            iTxtSmall  = resources.getDimension(R.dimen.txt_small);
            iTxtMedium = resources.getDimension(R.dimen.txt_medium);
        }

        String headerTitle = (String) getGroup(groupPosition);
        if ( headerTitle == null ) {
            return null;
        }
        if ( convertView == null ) {
            if ( inflater == null ) { return null; }
            convertView = inflater.inflate(m_iResId2inflateH, null);
            ColorPrefs.setColor(convertView);
        }
        doPostInflateGroup(convertView, headerTitle);

        TextView lblListHeader = ViewUtil.getFirstView(convertView, TextView.class);
        if ( lblListHeader != null ) {
            lblListHeader.setTypeface(null, Typeface.BOLD);
            lblListHeader.setText(" " + headerTitle + " ");
            if ( B_DIFFERENT_TXT_SIZE_PER_ORIENTATION ) {
                lblListHeader.setTextSize(TypedValue.COMPLEX_UNIT_PX, iTxtMedium);
            }
        }

        return convertView;
    }

    // e.g. invoked each time an group is inflated
    @Override public View getChildView(int groupPosition, final int childPosition, boolean isLastChild, View view, ViewGroup parent) {

        final String headerTitle = (String) getGroup(groupPosition);
        final String childText = getChildText(groupPosition, childPosition);

        ViewHolder viewHolder = null;

        if ( view == null ) {
            if ( inflater == null ) { return null; }
            view = inflater.inflate(this.m_iResId2inflateI, null);

            TextView txtView = ViewUtil.getFirstView(view, TextView.class);
            ColorPrefs.setColor(view);

            // create viewholder for freshly inflated view
            viewHolder = new ViewHolder();
            viewHolder.matchDesc = txtView;

            view.setTag(viewHolder);

        } else {
            viewHolder = (ViewHolder) view.getTag();
            if ( B_DIFFERENT_TXT_SIZE_PER_ORIENTATION ) {
                viewHolder.matchDesc.setTextSize(TypedValue.COMPLEX_UNIT_PX, iTxtSmall);
            }
        }

        viewHolder.matchDesc.setText(childText);
/*
        if ( ViewUtil.isLandscapeOrientation() ) {
            viewHolder.matchDesc.setTextSize(TypedValue.COMPLEX_UNIT_PX, IBoard.iTxtSizePx_PaperScoringSheet);
        }
*/

        boolean selected = isSelected(getKey(groupPosition, childPosition));
        setTextViewSelectionVisualFeedback(view, selected);
        doPostInflateItem(view, headerTitle, childText);

        viewHolder.iGroup = groupPosition;
        viewHolder.iChild = childPosition;

        return view;
    }

    protected void doPostInflateGroup(final View v, final String sHeader) {

    }
    protected void doPostInflateItem(final View v, final String sHeader, String sText) {

    }

    public static String getText(View v) {
        Object tag = v.getTag();
        if ( tag instanceof ViewHolder ) {
            ViewHolder viewHolder = (ViewHolder) tag;
            return viewHolder.matchDesc.getText().toString();
        } else {
            Log.w(TAG, "v.getTag() did not return a ViewHolder but " + (tag==null?null:tag.getClass().getName()));
            return "";
        }
    }

    public static class ViewHolder {
        TextView matchDesc; // can also be subclass Button
        int iGroup;
        int iChild;
    }

    //------------------------------------------------------------
    // Filtering
    //------------------------------------------------------------

    private boolean bRefilterRequired = false;
    private String m_sQuery = "";
    private String[] m_sQueryWords = null;
    private Pattern m_sQueryPatter = null;
    public void clearFilter(){
        filterData("");
    }
    void filterData(final String query){

        final String sQueryNew = query.toLowerCase();
        if ( sQueryNew.equals(m_sQuery) ) {
            //Log.d(TAG, "Filter did not change...");
            return;
        }

        m_sQuery = sQueryNew;

        m_queryTypeDerived = this.m_queryType;
        if ( m_queryType.equals(QueryType.Dynamic) ) {
            m_queryTypeDerived = QueryType.Contains;
            if ( m_sQuery.contains("|") ) {
                m_sQueryWords = m_sQuery.split("\\|");
                if ( m_sQueryWords.length > 1 ) {
                    m_queryTypeDerived = QueryType.OneOfWords;
                }
            }
            if ( m_sQuery.contains("/") ) {
                m_sQueryWords = m_sQuery.split("/");
                if ( m_sQueryWords.length > 1 ) {
                    m_queryTypeDerived = QueryType.OneOfWords;
                }
            }
            if ( m_sQuery.contains("&") ) {
                m_sQueryWords = m_sQuery.split("&");
                if ( m_sQueryWords.length > 1 ) {
                    m_queryTypeDerived = QueryType.AllWords;
                }
            }
            if ( m_sQuery.matches(".*([\\]\\.][\\*\\+]|\\(.+\\|.+\\)).*") ) {
                m_sQueryPatter = null;
                try {
                    m_sQuery = query.trim(); // no only lower case
                    m_sQueryPatter = Pattern.compile(m_sQuery);
                    m_queryTypeDerived = QueryType.RegExp;
                } catch (Exception e) {
                    //e.printStackTrace();
                }
            }
        }

        refreshForFilter();

        notifyDataSetChanged();
    }

    private enum QueryType {
        Contains,
        Dynamic,
        OneOfWords,
        AllWords,
        RegExp,
    }
    private QueryType m_queryType        = QueryType.Dynamic;
    private QueryType m_queryTypeDerived = QueryType.Contains;

    private void refreshForFilter() {
        m_lHeaders.clear();
        m_lHeader2Childs.clear();

        if ( m_sQuery.isEmpty() ) {
            m_lHeaders.addAll(m_lHeadersFull);
            m_lHeader2Childs.putAll(m_lHeader2ChildsFull);
        } else {

            // TODO: split into multiple words? allow & and | signs?
            // TODO: if string is found in a header, keep that header AND all its childs

            for ( String sHeader: m_lHeadersFull ) {
                List<String> childsFull = m_lHeader2ChildsFull.get(sHeader);
                if ( ListUtil.isEmpty(childsFull) ) { continue; }
                List<String> newList = new ArrayList<>();
                for ( String child: childsFull ) {
                    if( matchesQuery(child) ) {
                        newList.add(child);
                    }
                }
                if ( newList.size() > 0 ) {
                    m_lHeaders.add(sHeader);
                    if ( m_itemSorter != null ) {
                        Collections.sort(newList, m_itemSorter);
                    }
                    m_lHeader2Childs.put(sHeader, newList);
                }
            }
        }

        if ( m_headerSorter != null ) {
            Collections.sort(m_lHeaders, m_headerSorter);
        }

        bRefilterRequired = false;
    }

    private boolean matchesQuery(String child) {
        switch (m_queryTypeDerived) {
            case Contains:
                return child.toLowerCase().contains(m_sQuery);
            case AllWords: {
                boolean bMatches = true;
                for (String w : m_sQueryWords) {
                    bMatches = bMatches && child.toLowerCase().contains(w);
                    if (!bMatches) {
                        break;
                    }
                }
                return bMatches;
            }
            case OneOfWords: {
                boolean bMatches = false;
                for (String w : m_sQueryWords) {
                    bMatches = bMatches || child.toLowerCase().contains(w);
                    if (bMatches) {
                        break;
                    }
                }
                return bMatches;
            }
            case RegExp: {
                if ( m_sQueryPatter != null ) {
                    Matcher m = m_sQueryPatter.matcher(child);
                    final boolean bMatchFound = m.find();
                    return bMatchFound;
                }
            }
        }
        return true;
    }
    //------------------------------------------------------------
    // Caching
    //------------------------------------------------------------

    public boolean createFromCache(File f) {
        try {
            if ( f.exists() == false ) {
                return false;
            }
            List recovered = (List) FileUtil.readObjectFromFile(f);
            this.m_lHeader2ChildsFull = (HashMap<String, List<String>>) recovered.get(0);
            this.m_mObjects           = (Map<String, Object>) recovered.get(1);
            // to populate m_lHeadersFull
            for(String sHeader: m_lHeader2ChildsFull.keySet()) {
                addHeader(sHeader);
            }
            this.refreshForFilter();
            return true;
        } catch (Exception e){
            e.printStackTrace();
            f.delete();
        }
        return false;
    }

    public boolean createCache(File f) {
        List lObject = Arrays.asList(this.m_lHeader2ChildsFull, this.m_mObjects);
        return FileUtil.writeObjectToFile(f, lObject);
    }

    public File getCacheFile(Context context) {
        return new File(context.getCacheDir(), this.getClass().getName() + "." + getChildrenCount() + ".cache");
    }
/*
    protected boolean deleteCacheFile(Context context) {
        File f = getCacheFile(context);
        if ( (f != null) && f.exists() ) {
            return f.delete();
        }
        return false;
    }
*/
    public static int deleteCacheFiles(Context context) {
        final List<File> cacheFiles = ContentUtil.getFilesRecursive(context.getCacheDir(), ".*\\.cache$", null, null);
        return ContentUtil.deleteFiles(cacheFiles, null);
    }

    public List<String> getChilds(String sHeader) {
        return m_lHeader2ChildsFull.get(sHeader);
    }
    public LinkedHashMap<String, Integer> getHeaderChildCounts() {
        LinkedHashMap<String, Integer> mHeader2Count = new LinkedHashMap<String, Integer>();
        for(String sHeader: m_lHeader2ChildsFull.keySet() ) {
            mHeader2Count.put(sHeader, ListUtil.size(getChilds(sHeader)));
        }
        return mHeader2Count;
    }
}
