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
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;

import com.doubleyellow.util.ListUtil;
import com.doubleyellow.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import com.doubleyellow.base.R;

public class SelectObjectToggle<T> extends AppCompatButton /* just to have the same style for now */
{
    public SelectObjectToggle(Context context) {
        super(context);
    }
    private List<T>      m_lObjects       = null;
    private List<String> m_lDisplayValues = null;
    private int          m_iSelected      = 0;

    public SelectObjectToggle(Context context, AttributeSet attrs/*, int defStyleAttr*/) {
        super(context, attrs/*, defStyleAttr*/);

        List<String> lDisplayValues = null;
        ArrayList objects = null;

        TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.SelectEnum);
        try {
            String sClass = attributes.getString(R.styleable.SelectEnum_enum_class);
            int iResDisplayValues = attributes.getResourceId(R.styleable.SelectEnum_enum_display_values, 0);
            lDisplayValues = getStrings(iResDisplayValues);
            if ( sClass.matches("^\\w+\\.") ) {
                Class enumClass = Class.forName(sClass);
                objects = new ArrayList(Arrays.asList(enumClass.getEnumConstants()));
            } else {
                String[] a = StringUtil.singleCharacterSplit(sClass);
                objects = new ArrayList();
                for(String sVal: a ) {
                    if ( StringUtil.isInteger(sVal) ) {
                        // add integer
                        objects.add(sVal);
                    } else if ( sVal.matches(".*\\w+\\s\\w+.*") ) {
                        // add string
                        objects.add(sVal);
                    } else {
                        // assume it is a name of resource
                        int iResId = getResources().getIdentifier(sVal, "string", context.getPackageName());
                        if ( iResId != 0 ) {
                            String s = getResources().getString(iResId);
                            objects.add(s);
                        } else {
                            iResId = getResources().getIdentifier(sVal, "array", context.getPackageName());
                            if ( iResId != 0 ) {
                                int[] intArray = getResources().getIntArray(iResId);
                                for (int i = 0; i < intArray.length; i++) {
                                    objects.add(String.valueOf(intArray[i]));
                                }
                            } else {
                                objects.add(sVal);
                            }
                        }
                    }
                }
            }
        } catch (ClassNotFoundException var13) {
            var13.printStackTrace();
        } finally {
            attributes.recycle();
        }
        setText(objects.get(0).toString());
        init(context, objects, lDisplayValues);
    }

    public SelectObjectToggle(Context context, Class<T> enumClass, int iResIdDisplayValues) {
        super(context);
        if ( enumClass.isEnum() == false ) {
            throw new RuntimeException("This constructor is only for enum classes");
        }
        T[] enumConstants = enumClass.getEnumConstants();

        List<String> lDisplayValues = getStrings(iResIdDisplayValues);
        init(context, new ArrayList<T>(Arrays.asList(enumConstants)), lDisplayValues);
    }

    public List<String> getStrings(int iResIdDisplayValues) {
        List<String> lDisplayValues = null;
        if ( iResIdDisplayValues != 0 ) {
            String[] stringArray = getResources().getStringArray(iResIdDisplayValues);
            lDisplayValues = Arrays.asList(stringArray);
        }
        return lDisplayValues;
    }

    /*
        public SelectObjectToggle(Context context, Set<T> objects) {
            this(context, new ArrayList<T>(objects));
        }
    */
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

        this.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                int iNewSelected = (m_iSelected + 1) % objects.size();
                setSelectedIndex(iNewSelected);
            }
        });
    }

    @Override public void setOnClickListener(@Nullable OnClickListener l) {
        super.setOnClickListener(l);
        //super.setClickable(true);
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
