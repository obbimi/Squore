package com.doubleyellow.scoreboard.view;

import android.content.Context;
import android.text.InputType;
import android.util.AttributeSet;
import android.widget.EditText;

/**
 * TODO: delete?
 */
public class CountryView extends EditText
{
    private static final String TAG = "SB." + CountryView.class.getSimpleName();

    /** Invoked when created for popup */
    public CountryView(Context context) {
        super(context, null);
        this.setSingleLine();
        this.setPadding(10,0,0,0); // to have the cursor show up better
        this.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
    }

    public CountryView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CountryView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
/*
    @Override protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if ( this.getVisibility() == VISIBLE ) {
            initializeAdapter();
        }
    }
*/
}
