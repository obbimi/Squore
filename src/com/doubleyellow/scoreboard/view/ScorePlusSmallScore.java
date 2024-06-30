package com.doubleyellow.scoreboard.view;

import com.doubleyellow.android.util.ColorUtil;
import com.doubleyellow.android.view.AutoResizeTextView;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.ViewParent;

import androidx.constraintlayout.widget.ConstraintLayout;

/**
 * setTag(int) overwritten to allow to specify a small score
 */
public class ScorePlusSmallScore extends AutoResizeTextView {

    private static final String TAG = "SB." + ScorePlusSmallScore.class.getSimpleName();

    private static float fRatioOfSmall = 0.4f;

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

            ConstraintLayout.LayoutParams lpThis = (ConstraintLayout.LayoutParams) this.getLayoutParams();
            if ( lpThis.matchConstraintPercentHeight < 1 ) {
                params.matchConstraintPercentHeight = lpThis.matchConstraintPercentHeight * fRatioOfSmall;
            }
            if ( lpThis.matchConstraintPercentWidth < 1 ) {
                params.matchConstraintPercentWidth = lpThis.matchConstraintPercentWidth * fRatioOfSmall;
            }
            if ( lpThis.dimensionRatio != null ) {
                // use same ratio for small digit
                params.dimensionRatio = lpThis.dimensionRatio;
            }

            //layout_constraintHeight_percent

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
        if ( iScore == EMPTY  ) {
            if ( txtSmall.getText().length() != 0 ) {
                txtSmall.setText("");
            }
        } else {
            String sScore = String.valueOf(iScore);
            if ( sScore.equals(txtSmall.getText()) == false ) {
                txtSmall.setText(sScore);
            }
        }
        if ( iTxtColor != null ) {
            txtSmall.setTextColor(iTxtColor);
        }
    }

}
