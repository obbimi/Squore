/*
 * Copyright (C) 2020  Iddo Hoeve
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
package com.doubleyellow.view;

import android.content.Context;
import android.view.View;
import android.widget.Button;

import com.doubleyellow.util.ListUtil;
import com.doubleyellow.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class SelectObjectToggle<T> extends Button /* just to have the same style for now */
{
    public SelectObjectToggle(Context context) {
        super(context);
    }
    private List<T>      m_lObjects       = null;
    private List<String> m_lDisplayValues = null;
    private int          m_iSelected      = 0;

    public SelectObjectToggle(Context context, Class<T> enumClass, int iResIdDisplayValues) {
        super(context);
        if ( enumClass.isEnum() == false ) {
            throw new RuntimeException("This constructor is only for enum classes");
        }
        T[] enumConstants = enumClass.getEnumConstants();

        List<String> lDisplayValues = null;
        if ( iResIdDisplayValues != 0 ) {
            String[] stringArray = getResources().getStringArray(iResIdDisplayValues);
            lDisplayValues = Arrays.asList(stringArray);
        }
        init(context, new ArrayList<T>(Arrays.asList(enumConstants)), lDisplayValues);
    }

    public SelectObjectToggle(Context context, Set<T> objects) {
        this(context, new ArrayList<T>(objects));
    }
    public SelectObjectToggle(Context context, List<T> objects) {
        super(context);
        init(context, objects, null);
    }

    public SelectObjectToggle(Context context, Set<T> objects, List<String> lDisplayValues) {
        this(context,new ArrayList<T>(objects), lDisplayValues);
    }
    public SelectObjectToggle(Context context, List<T> objects, List<String> lDisplayValues) {
        super(context);
        init(context, objects, lDisplayValues);
    }

    private void init(Context context, final List<T> objects, List<String> lDisplayValues) {

        if ( lDisplayValues == null ) {
            lDisplayValues = new ArrayList<>();
        }
        if ( lDisplayValues.size() < objects.size() ) {

            for (int iIdx = 0; iIdx < ListUtil.size(objects); iIdx++ ) {
                String sDisplayValue = null;
                if ( iIdx < lDisplayValues.size() ) {
                    sDisplayValue = lDisplayValues.get(iIdx);
                }
                if ( StringUtil.isEmpty(sDisplayValue) ) {
                    sDisplayValue = StringUtil.capitalize(String.valueOf(objects.get(iIdx)));
                    if ( iIdx < lDisplayValues.size() ) {
                        lDisplayValues.set(iIdx, sDisplayValue);
                    } else {
                        lDisplayValues.add(sDisplayValue);
                    }
                }
            }
        }

        m_lObjects = objects;
        m_lDisplayValues = lDisplayValues;

        setSelectedIndex(0);

        super.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                int iNewSelected = (m_iSelected + 1) % objects.size();
                setSelectedIndex(iNewSelected);
            }
        });
    }
    public void setSelected(Object value) {
        this.setSelectedIndex(m_lObjects.indexOf(value));
    }
    public void setSelectedIndex(int i) {
        m_iSelected = i;
        setText(m_lDisplayValues.get(i));
    }

    public T getSelected() {
        return m_lObjects.get(m_iSelected);
    }
    public int getSelectedIndex() {
        return m_iSelected;
    }
}
