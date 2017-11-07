package com.doubleyellow.view;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.*;
import com.doubleyellow.android.view.ViewUtil;
import com.doubleyellow.scoreboard.R;

/**
 * To allow user to select an enum value from a set of radio buttons.
 * Used e.g. in the Conduct dialog.
 */
public class SelectEnumView<T extends Enum<T>> extends RadioGroup /* ScrollView */
{
    private Class<T>   clazz = null;
    private RadioGroup rg    = null;

    public SelectEnumView(final Context context, final AttributeSet attrs) {
        super(context, attrs/*, 0 /*R.style.SBTextView*/);
        final TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.SelectEnum, 0, 0);
        if ( attributes != null ) {
            try {
                int    iColumns = attributes.getInt   (R.styleable.SelectEnum_enum_columns, 1);
                String sClass   = attributes.getString(R.styleable.SelectEnum_enum_class);
                String sValue   = attributes.getString(R.styleable.SelectEnum_enum_value);
                int iResDisplayValues = attributes.getResourceId(R.styleable.SelectEnum_enum_display_values, 0);
                Class<T> clazz1 = null;
                try {
                    clazz1 = (Class<T>) Class.forName(sClass);
                } catch (Exception e) {
                    Log.e("SelectEnumView", "Could not load class " + sClass, e);
                    e.printStackTrace();
                }
                init(clazz1, sValue, iColumns, context, iResDisplayValues);
            } finally {
                // make sure recycle is always called.
                attributes.recycle();
            }
        }
    }
    public SelectEnumView(Context context, Class<T> clazz) {
        this(context, clazz, (T)null , 1);
    }
    public SelectEnumView(Context context, Class<T> clazz, T sValue, int iNrOfColumns) {
        this(context, clazz, (sValue==null?null:sValue.toString()), iNrOfColumns);
    }
    public SelectEnumView(Context context, Class<T> clazz, String sValue, int iNrOfColumns) {
        super(context);
        init(clazz, sValue, iNrOfColumns, null, 0);
    }

    private LinearLayout.LayoutParams layoutParams = null;//new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

    private void init(Class<T> clazz, String sSelectedValue, int iNrOfColumns, Context context, int iResourceDisplayValues) {
        this.clazz = clazz;

        T[] enumConstants = clazz.getEnumConstants();
        String[] sDisplayValues = null;
        if ( iResourceDisplayValues != 0 ) {
            try {
                sDisplayValues = context.getResources().getStringArray(iResourceDisplayValues);
            } catch (Resources.NotFoundException e) {
                e.printStackTrace();
            }
        }

        if ( iNrOfColumns == 0 ) {
            iNrOfColumns = enumConstants.length;
        }

        if ( this instanceof RadioGroup ) {
            rg = (RadioGroup) this;
        } else {
            rg = new RadioGroup(getContext());
        }
        rg.setOrientation(iNrOfColumns == 1 ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        //rg.setGravity(Gravity.CENTER_HORIZONTAL);

        int iSelected = "__NONE__".equalsIgnoreCase(sSelectedValue)?-1:0;
        int iColumn = -1;
        for (T value : enumConstants) {
            if ( value.toString().equalsIgnoreCase(sSelectedValue) ) {
                iSelected = value.ordinal();
            }
            String sValue = ViewUtil.getDisplayValue(clazz, sDisplayValues, value, context);
            RadioButton rb = new RadioButton(getContext());
            rb.setId(value.ordinal()); // for rg.getCheckedRadioButtonId()
            rb.setText(sValue);
            //rb.setGravity(Gravity.CENTER);
            iColumn = (iColumn+1) % iNrOfColumns;
            // adding nested linear layout does seem to break the functionality of being able to select just one of the group
            if ( iNrOfColumns==1 && layoutParams!=null ) {
                rg.addView(rb, layoutParams);
            } else {
                rg.addView(rb);
            }
        }
        rg.check(iSelected);
        if ( rg != this ) {
            super.addView(rg);
        }
    }

    public void check(T value) {
        rg.check(value.ordinal());
    }

    public T getChecked() {
        int checkedRadioButtonId = rg.getCheckedRadioButtonId();
        T[] enumConstants = clazz.getEnumConstants();
        if ( checkedRadioButtonId >=0 && (checkedRadioButtonId<enumConstants.length)) {
            return enumConstants[checkedRadioButtonId];
        }
        return null;
    }
}
