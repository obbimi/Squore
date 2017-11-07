package com.doubleyellow.scoreboard.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import com.doubleyellow.android.util.ColorUtil;
import com.doubleyellow.scoreboard.prefs.ColorPrefs;

public class ButtonUpdater implements DialogInterface.OnShowListener {

    private static final String TAG = ButtonUpdater.class.getSimpleName();

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
    }
}
