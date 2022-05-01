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
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.doubleyellow.android.view.ViewUtil;
import com.doubleyellow.scoreboard.main.DialogManager;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.prefs.ColorPrefs;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.util.Direction;
import com.doubleyellow.util.StringUtil;

public abstract class BaseAlertDialog /*extends AlertDialog NOT. TO MUCH hassle*/ {

    protected Context     context    = null;
    protected Model       matchModel = null;
    protected ScoreBoard  scoreBoard = null;

    protected MyDialogBuilder adb    = null;
    protected AlertDialog     dialog = null;

    protected final LinearLayout.LayoutParams llpMargin1Weight1;

    public BaseAlertDialog(Context context, Model matchModel, ScoreBoard scoreBoard) {
        this.context    = context;
        this.matchModel = matchModel;
        this.scoreBoard = scoreBoard;

        llpMargin1Weight1 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        llpMargin1Weight1.weight = 1;
        llpMargin1Weight1.setMargins(1, 1, 1, 1);

        this.adb = new MyDialogBuilder(context);
    }
    protected String getString(int resId) {
        return context.getString(resId);
    }
    protected String getString(int resId, java.lang.Object... formatArgs) {
        return context.getString(resId, formatArgs);
    }
    protected TextView getTextView(int resId, java.lang.Object... formatArgs) {
        TextView txt = new TextView(context);
        txt.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        if ( resId == 0 ) {
            txt.setText("");
        } else {
            txt.setText(getString(resId, formatArgs));
        }
        txt.setTextColor(Color.WHITE); // TODO: improve
        return txt;
    }

    private SparseArray<TextView> m_lButtons = new SparseArray<TextView>();
    protected TextView getActionView(int iCaption, final int iAction) {
        return getActionView(iCaption, iAction, 0, 0, null);
    }
    protected TextView getActionView(int iCaption, final int iAction, final int iSize) {
        return getActionView(iCaption, iAction, 0, iSize, null);
    }
    private TextView getActionView(int iCaption, final int iAction, int iImage, int iSize, Direction iconDirection) {
        return getActionView(context.getString(iCaption), iAction, iImage, iSize, iconDirection);
    }
    /** size for icon in px, if specified test size is calculated accordingly */
    TextView getActionView(String sCaption, final int iAction, int iImage, int iSize, Direction iconDirection) {
        TextView txt = new TextView(context); // using Button will give it rounded borders
        txt.setText( sCaption );
        if ( iSize > 20 ) {
            int iTxtSize = iSize / 3;
            if ( Direction.W.equals(iconDirection) ) {
                iTxtSize = (int) (iSize / 1.8);
            }
            txt.setTextSize(TypedValue.COMPLEX_UNIT_PX, iTxtSize);
        }
        txt.setId(iAction);
        txt.setTag(ColorPrefs.Tags.item.toString());
        txt.setSingleLine();
        txt.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                dismiss();
                handleButtonClick(v.getId());
            }
        });

        int iPadding = Math.max(iSize / 10, 10);
        txt.setPadding(iPadding, iPadding, iPadding, iPadding);
        txt.setGravity(Gravity.CENTER);

        if ( (iImage != 0) && (iSize != 0) ) {
            Drawable d = null;
            try {
                if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP /* 21 */ ) {
                    d = context.getDrawable(iImage);
                } else {
                    d = context.getResources().getDrawable(iImage);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if ( d != null ) {
                if (iSize > 0) {
                    d = ViewUtil.resize(context, d, iSize);
                }
                switch (iconDirection) {
                    case W:
                        txt.setCompoundDrawablesWithIntrinsicBounds(d, null, null, null);
                        break;
                    case N:
                        txt.setCompoundDrawablesWithIntrinsicBounds(null, d, null, null);
                        break;
                    case E:
                        txt.setCompoundDrawablesWithIntrinsicBounds(null, null, d, null);
                        break;
                    case S:
                        txt.setCompoundDrawablesWithIntrinsicBounds(null, null, null, d);
                        break;
                }
            }
        }
        m_lButtons.put(iAction, txt);

        return txt;
    }
    /** For demo purposes only */
    public void drawTouch(int iAction, int iColor) {
        if ( dialog == null ) {
            return;
        }
        final Button btnTouch = dialog.getButton(iAction);
        if ( btnTouch != null ) {
            btnTouch.setBackgroundColor(iColor);
        } else {
            TextView vAction = m_lButtons.get(iAction);
            if ( vAction != null ) {
                vAction.setBackgroundColor(iColor);
            }
        }
    }

    /** in Android 9 - API 28, buttons in dialog are left aligned initially. By just setting the layout parameters the get correctly aligned */
    protected void triggerButtonLayoutAPI28(DialogInterface dialogInterface, int iButton) {
        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ) {
            Button btnMiddle = ((AlertDialog)dialogInterface).getButton(iButton);
            if ( btnMiddle == null ) {
                // even if 'neutral' button is specified, it usually is still there but invisible
                return;
            }
            ViewParent parent = btnMiddle.getParent(); // LinearLayout
            if ( parent instanceof LinearLayout ) {
                ViewGroup.LayoutParams layoutParams = btnMiddle.getLayoutParams();
                if ( layoutParams instanceof LinearLayout.LayoutParams ) {
                    LinearLayout.LayoutParams llp = (LinearLayout.LayoutParams) layoutParams;
                    //llp.weight = llp.weight;
                    btnMiddle.setLayoutParams(llp);
                }
            }

        }
    }
    protected void setIconToPackage(Context ctx, AlertDialog.Builder builder, String packageName, int iResId) {
        if ( StringUtil.isEmpty(packageName) ) {
            builder.setIcon(iResId);
            return;
        }
        try {
            Drawable drawable = ctx.getPackageManager().getApplicationIcon(packageName);
            builder.setIcon(drawable);
        } catch (PackageManager.NameNotFoundException var6) {
            builder.setIcon(iResId);
        }
    }

    public void handleButtonClick(int which){
        this.dismiss();
    }

    /** The method that is invoked to save the state of the dialog for {@link #show(Bundle)} */
    public abstract boolean storeState(Bundle outState);

    /** The method that is invoked to reinstate the dialog after a screen rotation */
    public final boolean show(Bundle outState) {
        boolean bReturn = init(outState);
        show();
        return bReturn;
    }
    public abstract boolean init(Bundle outState);
    public abstract void show();

    public boolean isModal() {
        // usually a dialog is modal: exceptions: inline timer and dialog that only serves to trigger child activity
        return true;
    }

    public void dismiss() {
        if ( isShowing() ) {
            if ( dialog != null ) {
                try {
                    dialog.dismiss();
                } catch (Exception e) {
/*
                // try catch is for this reported error in play store (API 27)
                    at android.view.WindowManagerGlobal.findViewLocked (WindowManagerGlobal.java:473)
                    at android.view.WindowManagerGlobal.removeView (WindowManagerGlobal.java:382)
                    at android.view.WindowManagerImpl.removeViewImmediate (WindowManagerImpl.java:126)
                    at android.app.Dialog.dismissDialog (Dialog.java:370)
                    at android.app.Dialog.dismiss (Dialog.java:353)
                    at com.doubleyellow.scoreboard.dialog.BaseAlertDialog.dismiss (BaseAlertDialog.java:192)
*/
                    e.printStackTrace();
                }
            }
        }
    }
    public boolean isShowing() {
        return (dialog != null) && dialog.isShowing();
    }

    protected Dialog.OnKeyListener getOnBackKeyListener() {
        return getOnBackKeyListener(0); // non-existing button
    }
    protected Dialog.OnKeyListener getOnBackKeyListener(int which) {
        return new OnBackKeyListener(which);
    }

    private class OnBackKeyListener implements Dialog.OnKeyListener {
        private int m_which;
        public OnBackKeyListener(int i) {
            m_which = i;
        }
        @Override public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
            int action  = event.getAction();
            if (keyCode == KeyEvent.KEYCODE_BACK /* = 4 */ && action == KeyEvent.ACTION_UP) {
                dialog.dismiss();
                handleButtonClick(m_which);
                return true;
            }
            return false;
        }
    }

    public <T extends Enum<T>> String getEnumDisplayValue(int iArrayResId, T value) {
        return ViewUtil.getEnumDisplayValue(context, iArrayResId, value);
    }
    protected String getGameOrSetString(int iResId) {
        return PreferenceValues.getGameOrSetString(context, iResId);
    }
    protected void showNextDialog() {
        DialogManager.getInstance().showNextDialog();
    }

    protected boolean isWearable() {
        return ViewUtil.isWearable(context) /*|| DateUtil.getCurrentYYYYMMDD().equals("20210927")*/;
    }
    protected boolean isLeanback_AndroidTV() {
        return ViewUtil.isLeanback_AndroidTV(context);
    }
    protected boolean isNotWearable() {
        return isWearable() == false;
    }
    AlertDialog.Builder setTitle(int iRes) {
        if ( dialog != null ) {
            dialog.setTitle(iRes);
        }
        return adb.setTitle(iRes);
    }
    AlertDialog.Builder setTitle(String s) {
        if ( dialog != null ) {
            dialog.setTitle(s);
        }
        return adb.setTitle(s);
    }
    AlertDialog.Builder setIcon(int iRes) {
        if ( dialog != null ) {
            dialog.setIcon(iRes);
        }
        return adb.setIcon(iRes);
    }
    AlertDialog.Builder setMessage(String s) {
        if ( dialog != null ) {
            dialog.setMessage(s);
        }
        return adb.setMessage(s);
    }
    AlertDialog.Builder setPositiveButton(String s, DialogInterface.OnClickListener onClickListener) {
        return adb.setPositiveButton(s, onClickListener);
    }
    AlertDialog.Builder setNegativeButton(String s, DialogInterface.OnClickListener onClickListener) {
        return adb.setNegativeButton(s, onClickListener);
    }
    AlertDialog.Builder setNeutralButton(int s, DialogInterface.OnClickListener onClickListener) {
        return adb.setNeutralButton(s, onClickListener);
    }

    public AlertDialog create() {
        dialog = adb.create();
        return dialog;
    }
    protected Button getButton(/*AlertDialog alertDialog, */int iButton) {
        //return alertDialog.getButton(iButton);
        return dialog.getButton(iButton);
    }


}
