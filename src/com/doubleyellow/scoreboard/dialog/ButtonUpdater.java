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

package com.doubleyellow.scoreboard.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.TextView;

import com.doubleyellow.android.util.ColorUtil;
import com.doubleyellow.android.view.ViewUtil;
import com.doubleyellow.scoreboard.prefs.ColorPrefs;

/**
 * Used to give a button
 * - a color
 * - an icon
 *
 * In case of wearables: reduce default text size of buttons and message
 */
public class ButtonUpdater implements DialogInterface.OnShowListener {

    private static final String TAG = "SB." + ButtonUpdater.class.getSimpleName();

    private   static final int[] iaButtonAll    = new int[] { DialogInterface.BUTTON_POSITIVE ,DialogInterface.BUTTON_NEGATIVE, DialogInterface.BUTTON_NEUTRAL };
    protected static       int[] iaColorNeutral = null;
    protected static       int[] iaColorAll     = null; // only color the neutral button since it is always disabled so color is consistent
    private   static       int   iPlayerButtonColor = 0;

    public static void setPlayerColor(Integer iColor) {
        if ( iColor == null ) { return; }
        Object a = ColorPrefs.ColorTarget.playerButtonBackgroundColor;
        iPlayerButtonColor = iColor; //ColorPrefs.getTarget2colorMapping(context).get(ColorPrefs.ColorTarget.playerButtonBackgroundColor);
        iaColorNeutral     = new int[] { DialogInterface.BUTTON_NEUTRAL, iPlayerButtonColor };
        iaColorAll         = new int[] { DialogInterface.BUTTON_POSITIVE, iPlayerButtonColor ,DialogInterface.BUTTON_NEGATIVE, iPlayerButtonColor ,DialogInterface.BUTTON_NEUTRAL, iPlayerButtonColor };
    }
    private int[]   iaButton2ImageId = null;
    private int[]   iaButton2Color   = null;
    private boolean bTLeftFRight     = false;

    private Context context = null;
    // used by e.g. ColorPicker
    public ButtonUpdater(Context ctx, int ... iButton2Color ) {
        if ( iButton2Color == null || iButton2Color.length == 0 ) {
            iButton2Color = iaColorAll;
        }
        this.iaButton2Color = iButton2Color;
        if ( this.iaButton2Color.length % 2 != 0 ) {
            Log.w(TAG, "This will not work!!");
        }
        this.context        = ctx;
    }
    // used by e.g. EndGame
    ButtonUpdater(Context ctx, boolean bTLeftFRight, int ... iButton2ImageId ) {
        this.bTLeftFRight     = bTLeftFRight; // true=Left, False=right
        this.iaButton2ImageId = iButton2ImageId;
        this.context          = ctx;
        this.iaButton2Color   = iaColorAll;
    }
    @Override public void onShow(DialogInterface dialogInterface) {
        AlertDialog dialog = (AlertDialog) dialogInterface;

        if ( iaButton2ImageId != null) {
            for (int i = 0; i < iaButton2ImageId.length; i += 2) {
                Button button = dialog.getButton(iaButton2ImageId[i]);
                if (button == null) {
                    return;
                }
                Drawable image = context.getResources().getDrawable(iaButton2ImageId[i + 1]);
                image.setBounds(0, 0, image.getIntrinsicWidth(), image.getIntrinsicHeight());
                if ( bTLeftFRight ) {
                    button.setCompoundDrawables(image, null, null, null);
                } else {
                    button.setCompoundDrawables(null, null, image, null);
                }
                // could modify the placement more here if desired
                //button.setCompoundDrawablePadding();
            }
        }
        for (int i = 0; i < iaButtonAll.length; i++) {
            Button button = dialog.getButton(iaButtonAll[i]);
            if ( button != null ) {
                ColorUtil.resetBackground(button);
            }
        }
        if ( iaButton2Color != null ) {
            for (int i = 0; i < iaButton2Color.length; i += 2) {
                Button button = dialog.getButton(iaButton2Color[i]);
                if ( button == null ) {
                    continue;
                }
                int iColor = iaButton2Color[i + 1];
                ColorUtil.setBackground(button, iColor);
                button.setTextColor(ColorUtil.getBlackOrWhiteFor(iColor));

                if ( iColor == iPlayerButtonColor ) {
                    // TODO: only set background color of buttonbar to black of iPlayerButtonColor is not to dark
                    ViewParent parent = button.getParent();
                    if (parent instanceof ViewGroup) {
                        ViewGroup vg = (ViewGroup) parent;
                        ColorUtil.setBackground(vg, Color.BLACK);
                    }
                }
            }
        }
        if ( false && ViewUtil.isWearable(context) ) {
            float fMultiplyFactorForWearable = 0.75f;
            for (int i = 0; i < iaButtonAll.length; i++) {
                Button button = dialog.getButton(iaButtonAll[i]);
                if ( button != null ) {
                    reduceTextSize(fMultiplyFactorForWearable, button);
                }
            }
            TextView tv = dialog.findViewById(android.R.id.message);
            if ( tv != null ) {
                reduceTextSize(fMultiplyFactorForWearable, tv);
            }
        }
    }

    private void reduceTextSize(float fMultiplyFactorForWearable, TextView tv) {
        float iNewTextSize = tv.getTextSize() * fMultiplyFactorForWearable;
        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, iNewTextSize);
    }
}
