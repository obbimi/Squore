package com.doubleyellow.scoreboard.view;

import com.doubleyellow.android.util.ColorUtil;
import com.doubleyellow.android.view.AutoResizeTextView;
import com.doubleyellow.scoreboard.R;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewParent;

import androidx.constraintlayout.widget.ConstraintLayout;

/**
 * setTag(int) overwritten to allow to specify a small score
 */
public class ScorePlusSmallScore extends AutoResizeTextView {

    private static final String TAG = "SB." + ScorePlusSmallScore.class.getSimpleName();

    private AutoResizeTextView txtSmall = null;

    public ScorePlusSmallScore(Context context) {
        this(context, null);
    }
    public ScorePlusSmallScore(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    public ScorePlusSmallScore(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    private void createSmall() {
        ViewParent parent = this.getParent();
        if ( parent == null ) { return; }

        if ( parent instanceof ConstraintLayout ) {
            txtSmall = new AutoResizeTextView(getContext());
            ConstraintLayout viewGroup = (ConstraintLayout) parent;

            //ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.WRAP_CONTENT);
            ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(0, 0);
            params.topToTop = this.getId();
            params.endToEnd = this.getId();

            //layout_constraintHeight_percent
            Resources resources = getContext().getResources();
            float fHeight = 0.13f;
            float fWidth  = 0.052f;
            try {
                fHeight = resources.getFloat(R.fraction.lsp_fr_small_digit_height);
                fWidth = resources.getFloat(R.fraction.lsp_fr_small_digit_width);
/*
                fHeight = resources.getFraction(R.fraction.lsp_fr_small_digit_height, 0, 0);
                fWidth = resources.getFraction(R.fraction.lsp_fr_small_digit_width, 0, 0);
*/
            } catch (Exception e) {
                Log.w(TAG, e);
            }

            params.matchConstraintPercentHeight = fHeight;
            //params.matchConstraintDefaultHeight = ConstraintLayout.LayoutParams.MATCH_CONSTRAINT_PERCENT;

            params.matchConstraintPercentWidth = fWidth;
            //params.matchConstraintDefaultWidth = ConstraintLayout.LayoutParams.MATCH_CONSTRAINT_PERCENT;

            if ( true ) {
                viewGroup.addView(txtSmall, params);

                //ColorUtil.setBackground(txtSmall, R.color.black);
                ColorUtil.setBackground(txtSmall, Color.TRANSPARENT);
                //txtSmall.setBackground(this.getBackground());
                txtSmall.setTextColor(this.getCurrentTextColor());
            }
        }
    }

    @Override public void setTag(Object tag) {
        if ( tag instanceof Integer ) {
            setSmallScore((int) tag, null);
        } else {
            super.setTag(tag);
        }
    }

    public static final Integer EMPTY = -1;
    private void setSmallScore(int iScore, Integer iTxtColor) {
        if ( txtSmall == null ) {
            createSmall();
        }
        if ( iScore == EMPTY ) {
            txtSmall.setText("");
        } else {
            txtSmall.setText(String.valueOf(iScore));
        }
        if ( iTxtColor != null ) {
            txtSmall.setTextColor(iTxtColor);
        }
    }

}
