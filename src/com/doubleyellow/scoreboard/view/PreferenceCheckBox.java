package com.doubleyellow.scoreboard.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.prefs.PreferenceKeys;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;

/**
 * Checkbox to toggle a certain preference value.
 * Mainly used to allow the user to disable triggering certain dialogs automatically
 */
public class PreferenceCheckBox extends CheckBox
{
    private PreferenceKeys preferenceKey  = null;
    private boolean        bDefaultValue  = false;
    private boolean        bStoreInverted = false;

    public PreferenceCheckBox(final Context context, final AttributeSet attrs) {
        this(context, attrs, R.attr.preferenceCheckBoxStyle);
    }

    public PreferenceCheckBox(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs);

        final TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.PreferenceCheckBox, defStyle, 0);
        String sPrefKey       = attributes.getString (R.styleable.PreferenceCheckBox_preference_key);
        boolean defaultValue  = attributes.getBoolean(R.styleable.PreferenceCheckBox_preference_key_default_value, bDefaultValue);
        boolean storeInverted = attributes.getBoolean(R.styleable.PreferenceCheckBox_store_inverted              , bStoreInverted);
        init(context, PreferenceKeys.valueOf(sPrefKey), defaultValue , storeInverted);
        attributes.recycle();
    }

    public PreferenceCheckBox(Context context, PreferenceKeys preferenceKey, int iResDefault, boolean bStoreInverted) {
        super(context);
        init(context, preferenceKey, context.getResources().getBoolean(iResDefault), bStoreInverted);
    }

    private void init(Context context, PreferenceKeys preferenceKey, boolean bDefault, boolean bStoreInverted) {
        this.preferenceKey = preferenceKey;
        this.bStoreInverted = bStoreInverted;
        this.bDefaultValue = bDefault;

        boolean bCurrentValue = PreferenceValues.getBoolean(preferenceKey, context, this.bDefaultValue);
        if ( bStoreInverted ) {
            bCurrentValue = !bCurrentValue;
        }
        this.setChecked(bCurrentValue);

        // ensure it is label at as item for custom color preferences
        //setTag(ColorPrefs.Tags.item.toString());
/*
        if ( context instanceof ScoreBoard) {
            Map<ColorPrefs.ColorTarget, Integer> mColors = ColorPrefs.getTarget2colorMapping(context);
            setTextColor(mColors.get(ColorPrefs.ColorTarget.lightest));
        } else {
            setTextColor(Color.WHITE);
        }
*/

        super.setOnCheckedChangeListener(onCheckedChangeListener);
    }

    private OnCheckedChangeListener onCheckedChangeListener = new OnCheckedChangeListener() {
        @Override public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            boolean bStoreValue = bStoreInverted?(!b):(b);
            PreferenceValues.setBoolean(preferenceKey, getContext(), bStoreValue);
        }
    };

/*
    @Override public void setTextColor(int color) {
        super.setTextColor(color);
    }
*/
}
