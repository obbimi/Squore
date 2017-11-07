package com.doubleyellow.view;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.doubleyellow.android.view.ViewUtil;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * To allow user to select an enum value from a spinner.
 * TODO: WAS Used e.g. in the MatchView.
 * TODO: ensure it can be used in Android designer
 */
public class EnumSpinner<T extends Enum<T>> extends Spinner
{
    private Class<T>   clazz = null;

    public EnumSpinner(final Context context) {
        this(context, null, R.attr.enumSpinnerStyle);
    }
    public EnumSpinner(final Context context, final AttributeSet attrs) {
        this(context, attrs, R.attr.enumSpinnerStyle);
    }

    public EnumSpinner(final Context context, final AttributeSet attrs, int iDefStyle) {
        super(context, attrs, iDefStyle, MODE_DROPDOWN);

        final TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.SelectEnum/*, iDefStyle, 0*/);
        if ( attributes != null ) {
            try {
                String sClass = attributes.getString(R.styleable.SelectEnum_enum_class);
                if ( StringUtil.isNotEmpty(sClass)) {
                    String sValue = attributes.getString(R.styleable.SelectEnum_enum_value);
                    int iResDisplayValues = attributes.getResourceId(R.styleable.SelectEnum_enum_display_values, 0);
                    Class<T> clazz1 = (Class<T>) Class.forName(sClass);
                    T value = null;
                    if (StringUtil.isNotEmpty(sValue)) {
                        value = (T) Enum.valueOf(clazz1, sValue);
                    }
                    init(this, context, clazz1, value, null, iResDisplayValues);
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
                // make sure recycle is always called.
                attributes.recycle();
            }
        }
    }

    public T getSelectedEnum() {
        int selectedItemPosition = this.getSelectedItemPosition();
        return clazz.getEnumConstants()[selectedItemPosition];
    }
    public void setSelected(T value) {
        this.setSelection(value.ordinal());
    }

    @Override public boolean performClick() {
        final boolean b = super.performClick();
        return b;
    }

    public static <T extends Enum<T>> void init(Spinner spinner, Context context, Class<T> clazz, T value, T excludeValue, int iResourceDisplayValues) {
        init(spinner, context, clazz, value, excludeValue, iResourceDisplayValues, 0);
    }
    public static <T extends Enum<T>> void init(Spinner spinner, Context context, Class<T> clazz, T value, T excludeValue, int iResourceDisplayValues, final int iTextSizePx) {
        T[] enumConstants = clazz.getEnumConstants();
        String[] sDisplayValues = null;
        if ( iResourceDisplayValues != 0 ) {
            try {
                sDisplayValues = context.getResources().getStringArray(iResourceDisplayValues);
            } catch (Resources.NotFoundException e) {
                e.printStackTrace();
            }
        }

        if ( value == null ) {
             value = enumConstants[0];
        }
        List<String> list = new ArrayList<String>();
        int iSelectedIndex = 0;
        for ( int iIdx=0; iIdx < enumConstants.length; iIdx++ ) {
            T val = enumConstants[iIdx];
            if ( val.equals(excludeValue) ) {
                // TODO: since we work with index and ordinal() this only works for now if the excluded entry is the last value of the enumeration
                continue;
            }
            if ( val.equals(value) ) {
                iSelectedIndex = iIdx;
            }
            String sDisplayValue = ViewUtil.getDisplayValue(clazz, sDisplayValues, val, context);
            list.add(sDisplayValue);
        }
        //ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, list);
        ArrayAdapter<String> dataAdapter = getStringArrayAdapter(context, list, iTextSizePx);
        spinner.setAdapter(dataAdapter);
        spinner.setSelection(iSelectedIndex);

        //this.setOnItemClickListener(getOnItemClickListener());
    }

    public static ArrayAdapter<String> getStringArrayAdapter(Context context, List<String> list, final int iTxtSizePx)
    {
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(context, R.layout.spinner_item, list)
        {
            @Override public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                if ( (view instanceof TextView) && (iTxtSizePx > 0 ) ) {
                    TextView tv = (TextView) view;
                    tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, iTxtSizePx);
                }
                return view;
            }
        };
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return dataAdapter;
    }

}
