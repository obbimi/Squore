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
package com.doubleyellow.prefs;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import com.doubleyellow.scoreboard.R;

/**
 * Class to have a 'Slider' in order to select a value in a certain range.
 * Used to set 'TextSize' of several elements in the SquoreBoard app. 
 *
 * Needs seek_bar_preference.xml
 *
 * Typical declaration looks like
 *
 *         <com.doubleyellow.prefs.SeekBarPreference
 *             android:key="TextSizePlayerName"
 *             android:title="@string/pref_TextSizePlayerName"
 *             dy:min="15"
 *             dy:interval="3"
 *             android:defaultValue="@integer/TextSizePlayerName_default"
 *             android:max="90">
 */
public class SeekBarPreference extends Preference implements SeekBar.OnSeekBarChangeListener {

    private final String TAG = "SB." + SeekBarPreference.class.getSimpleName();

    private static final String ANDROID_NS     = "http://schemas.android.com/apk/res/android";
    private static final String APPLICATION_NS = "http://double-yellow.be";
    private static final int    DEFAULT_VALUE  = 50;

    private int mMaxValue      = 100;
    private int mMinValue      = 0;
    private int mInterval      = 1;
    private int mCurrentValue;
    private String mUnitsLeft  = "";
    private String mUnitsRight = "";
    private SeekBar mSeekBar;

    private TextView mStatusText;

    public SeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        initPreference(context, attrs);
    }

    public SeekBarPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initPreference(context, attrs);
    }

    private void initPreference(Context context, AttributeSet attrs) {
        setValuesFromXml(attrs);
        mSeekBar = new SeekBar(context, attrs);
        mSeekBar.setMax(mMaxValue - mMinValue);
        mSeekBar.setOnSeekBarChangeListener(this);

        setWidgetLayoutResource(R.layout.seek_bar_preference);
    }

    private void setValuesFromXml(AttributeSet attrs) {
        mMinValue    = attrs.getAttributeIntValue(APPLICATION_NS, "min", 0);
        mMaxValue    = attrs.getAttributeIntValue(ANDROID_NS    , "max", 100);

        mUnitsLeft   = getAttributeStringValue(attrs, APPLICATION_NS, "unitsLeft" , "");
        String units = getAttributeStringValue(attrs, APPLICATION_NS, "units"     , "");
        mUnitsRight  = getAttributeStringValue(attrs, APPLICATION_NS, "unitsRight", units);

        try {
            String newInterval = attrs.getAttributeValue(APPLICATION_NS, "interval");
            if ( newInterval != null ) {
                mInterval = Integer.parseInt(newInterval);
            }
        } catch (Exception e) {
          //Log.e(TAG, "Invalid interval value", e);
        }
    }

    private String getAttributeStringValue(AttributeSet attrs, String namespace, String name, String defaultValue) {
        String value = attrs.getAttributeValue(namespace, name);
        if(value == null) value = defaultValue;

        return value;
    }

    @Override protected View onCreateView(ViewGroup parent) {
        View view = super.onCreateView(parent);

        // The basic preference layout puts the widget frame to the right of the title and summary,
        // so we need to change it a bit - the seekbar should be under them.
        LinearLayout layout = (LinearLayout) view;
        //Log.w(TAG, "BEFORE setOrientation " + this.toString());
        layout.setOrientation(LinearLayout.VERTICAL);
        //Log.w(TAG, "AFTER setOrientation " + this.toString());

        return view;
    }

    @Override public void onBindView(View view) {
        super.onBindView(view);

        try {
            // move our seekbar to the new view we've been given
            ViewGroup oldContainer = (ViewGroup) mSeekBar.getParent();
            ViewGroup  newContainer = (ViewGroup) view.findViewById(R.id.seekBarPrefBarContainer);

            if ( oldContainer != newContainer ) {
                // remove the seekbar from the old view
                if ( oldContainer != null ) {
                    //Log.w(TAG, "BEFORE removeView " + oldContainer.toString());
                    oldContainer.removeView(mSeekBar);
                    //Log.w(TAG, "AFTER removeView " + oldContainer.toString());
                }
                // remove the existing seekbar (there may not be one) and add ours
                //Log.w(TAG, "BEFORE removeAllViews " + newContainer.toString());
                newContainer.removeAllViews();
                //Log.w(TAG, "AFTER removeAllViews " + newContainer.toString());
                newContainer.addView(mSeekBar, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                //Log.w(TAG, "AFTER addView " + newContainer.toString());
            }
        } catch (Exception ex) {
            Log.e(TAG, "Error binding view: " + ex.toString());
        }

        //if dependency is false from the beginning, disable the seek bar
        if ( (view != null) && (view.isEnabled() == false) ) {
            mSeekBar.setEnabled(false);
        }

        updateView(view);
    }

    /**
     * Update a SeekBarPreference view with our current state
     * @param view
     */
    protected void updateView(View view) {
        try {
            mStatusText = (TextView) view.findViewById(R.id.seekBarPrefValue);

            mStatusText.setText(String.valueOf(mCurrentValue));
            mStatusText.setMinimumWidth(30);

            mSeekBar.setProgress(mCurrentValue - mMinValue);

            TextView unitsRight = (TextView)view.findViewById(R.id.seekBarPrefUnitsRight);
            unitsRight.setText(mUnitsRight);

            TextView unitsLeft = (TextView)view.findViewById(R.id.seekBarPrefUnitsLeft);
            unitsLeft.setText(mUnitsLeft);
        } catch (Exception e) {
            Log.e(TAG, "Error updating seek bar preference", e);
        }
    }

    @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        int newValue = progress + mMinValue;

        if ( newValue > mMaxValue ) {
            newValue = mMaxValue;
        } else if ( newValue < mMinValue ) {
            newValue = mMinValue;
        } else if ( (mInterval != 1) && newValue % mInterval != 0) {
            newValue = Math.round(((float)newValue)/mInterval)*mInterval;
        }

        // change rejected, revert to the previous value
        if ( callChangeListener(newValue) == false ){
            seekBar.setProgress(mCurrentValue - mMinValue);
            return;
        }

        // change accepted, store it
        mCurrentValue = newValue;
        mStatusText.setText(String.valueOf(newValue));
        try {
            persistInt(newValue);
        } catch (ClassCastException e) {
            persistString(String.valueOf(newValue));
        }

    }

    @Override public void onStartTrackingTouch(SeekBar seekBar) {}

    @Override public void onStopTrackingTouch(SeekBar seekBar) {
        notifyChanged();
    }

    @Override protected Object onGetDefaultValue(TypedArray ta, int index){
        int defaultValue = ta.getInt(index, DEFAULT_VALUE);
        return defaultValue;
    }

    @Override protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        if(restoreValue) {
            try {
                mCurrentValue = getPersistedInt(mCurrentValue);
            } catch (ClassCastException e) {
                String sTmp = getPersistedString(null);
                mCurrentValue = Integer.parseInt(sTmp);
            }
        } else {
            int temp = 0;
            try {
                temp = (Integer)defaultValue;
            } catch (Exception ex) {
                Log.e(TAG, "Invalid default value: " + defaultValue.toString());
            }

            persistInt(temp);
            mCurrentValue = temp;
        }
    }

    /**
     * make sure that the seekbar is disabled if the preference is disabled
     */
    @Override public void setEnabled(boolean enabled) {
        //Log.w(TAG, "BEFORE super.setEnabled ");
        super.setEnabled(enabled);
        //Log.w(TAG, "AFTER super.setEnabled ");
        mSeekBar.setEnabled(enabled);
        //Log.w(TAG, "AFTER mSeekBar.setEnabled");
    }

    @Override public void onDependencyChanged(Preference dependency, boolean disableDependent) {
        super.onDependencyChanged(dependency, disableDependent);

        //Disable movement of seek bar when dependency is false
        if (mSeekBar != null) {
            mSeekBar.setEnabled(!disableDependent);
        }
    }
}
