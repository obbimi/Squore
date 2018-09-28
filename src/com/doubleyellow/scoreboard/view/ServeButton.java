/*
 * Copyright (C) 2018  Iddo Hoeve
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

package com.doubleyellow.scoreboard.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.doubleyellow.android.view.AutoResizeTextView;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.util.StringUtil;

/**
 * Serve button that indicates
 * - side and handout for Squash
 * - number of serves left for tabletennis
 */
public class ServeButton extends /*SBRelativeLayout*/ AutoResizeTextView
{
    /** 1 based */
    private static int i_maxForCountDown = 0;
    /** use to determine if we use special characters for integer count down. Table tennis only */
    public static void setMaxForCountDown(int i) {
        i_maxForCountDown = i;
    }
    // https://en.wikipedia.org/wiki/List_of_Unicode_characters
    //private static final String[] NR_TO_SYMBOL = {"\u204E", "\u2051"}; // 1 stars, 2 star (not decently centered)
    //private static final String[] NR_TO_SYMBOL = {"\u223C", "\u2248"}; // 1 tildes, 2 tilde (not bad)
    private static final String[] NR_TO_SYMBOL   = {"\u2160", "\u2161"}; // 1 vertical stripes, 2 stripe (looks good, but not on e.g. tablet: font?)
    //private static final String[] NR_TO_SYMBOL = {"\u25CB", "\u25CE"}; // white circle (1 single circle), bulls-eye (2 circles)  (looks nice on S7, but looks strange on chromecast somehow and  S4)
    //private static final String[] NR_TO_SYMBOL = {"\u2680", "\u2681"}; // dice 1, dice 2 (on S7, somehow the are colored... what color is it... may not match with)
    //private static final String[] NR_TO_SYMBOL = {"\u26AC", "\u26AF"}; // 1 open dot, 2 connected open dots (S7: colored?)

/*
    public ServeButton(Context context) {
        this(context, null, 0); // not used
    }
*/
    /** Required for usage in layout xml */
    public ServeButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ServeButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        // known instances
        if ( false ) {
            int i1 = R.id.btn_side1;
            int i2 = R.id.btn_side2;
        }
    }
    public String setServeString(Object o, int iTransparencyNonServer) {
        String sValue = String.valueOf(o);
        if ( o instanceof Integer ) {
            int iNrOneBased = (Integer) o;
            if ( NR_TO_SYMBOL != null ) {
                if ( (i_maxForCountDown - 1 < NR_TO_SYMBOL.length) && (iNrOneBased <= NR_TO_SYMBOL.length) ) {
                    // represents number of serves left
                    sValue = NR_TO_SYMBOL[iNrOneBased - 1];
                }
            }
        }
        View v = this;
        if ( v instanceof TextView ) {
            TextView txtView = (TextView) v;
            txtView.setText(sValue);
        } else {
            Toast.makeText(getContext(), "TODO: ServeButton String " + this.getId() + " : " + sValue, Toast.LENGTH_LONG).show();
        }

        // set transparency: if something is displayed: non transparent, if nothing is displayed semi-transparent (user preference)
        int iTransparency = StringUtil.isEmpty(sValue)? iTransparencyNonServer : 0;
        Drawable drawable = this.getBackground();
        if ( drawable instanceof GradientDrawable ) {
            GradientDrawable gd = (GradientDrawable) drawable;
            gd.setAlpha(0xFF - iTransparency);
        }
        return sValue;
    }

    /** For now this is equal to setting color of the text */
    public void setForegroundColor(int iFGColor) {
        View v = this;
        if ( v instanceof TextView ) {
            TextView txtView = (TextView) v;
            txtView.setTextColor(iFGColor);
        } else {
            Toast.makeText(getContext(), "TODO: ServeButton FGColor " + this.getId() + " : " + iFGColor, Toast.LENGTH_LONG).show();
        }
    }
}
