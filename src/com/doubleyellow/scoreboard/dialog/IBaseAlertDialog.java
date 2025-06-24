/*
 * Copyright (C) 2025  Iddo Hoeve
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
import android.os.Bundle;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.doubleyellow.android.view.ViewUtil;
import com.doubleyellow.scoreboard.main.DialogManager;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.prefs.ColorPrefs;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.util.Direction;
import com.doubleyellow.util.StringUtil;

public abstract class IBaseAlertDialog {
    protected final LinearLayout.LayoutParams llpMargin1Weight1;

    protected Context     context    = null;
    protected Model       matchModel = null;
    protected ScoreBoard  scoreBoard = null;

    public IBaseAlertDialog(Context context, Model matchModel, ScoreBoard scoreBoard) {
        this.context    = context;
        this.matchModel = matchModel;
        this.scoreBoard = scoreBoard;

        llpMargin1Weight1 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        llpMargin1Weight1.weight = 1;
        llpMargin1Weight1.setMargins(1, 1, 1, 1);
    }


    /** The method that is invoked to save the state of the dialog for {@link #show(Bundle)} */
    public abstract boolean storeState(Bundle outState);

    /** for newer theme some buttons should be switched for consistency */
    protected boolean swapPosNegButtons(Context context) { return false; }

    public String getString(int resId) {
        return context.getString(resId);
    }
    public String getString(int resId, java.lang.Object... formatArgs) {
        return context.getString(resId, formatArgs);
    }

    public void handleButtonClick(int which){
        this.dismiss();
    }

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

    public abstract void dismiss();

    public abstract boolean isShowing();

/*
    public abstract Dialog.OnKeyListener getOnBackKeyListener();

    public abstract Dialog.OnKeyListener getOnBackKeyListener(int which);
*/

    protected final boolean isLeanback_AndroidTV() {
        return ViewUtil.isLeanback_AndroidTV(context);
    }

    protected final boolean isWearable() {
        return ViewUtil.isWearable(context) /*|| DateUtil.getCurrentYYYYMMDD().equals("20210927")*/;
    }
    protected final boolean isNotWearable() {
        return isWearable() == false;
    }

    protected abstract Button getDialogButton(int iButton);

    public <T extends Enum<T>> String getEnumDisplayValue(int iArrayResId, T value) {
        return ViewUtil.getEnumDisplayValue(context, iArrayResId, value);
    }
    public final String getGameOrSetString(int iResId) {
        return PreferenceValues.getGameOrSetString(context, iResId);
    }
    public final void showNextDialog() {
        DialogManager.getInstance().showNextDialog();
    }

    public Dialog.OnKeyListener getOnBackKeyListener() {
        return getOnBackKeyListener(0); // non-existing button
    }
    public Dialog.OnKeyListener getOnBackKeyListener(int which) {
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

    public TextView getButton(/*AlertDialog alertDialog, */int iButton) {
        if ( m_lButtons.contains(iButton) ) {
            return m_lButtons.get(iButton);
        }
        return getDialogButton(iButton);
    }
    protected SparseArray<TextView> m_lButtons = new SparseArray<TextView>();
    public TextView getActionView(int iCaption, final int iAction) {
        return getActionView(iCaption, iAction, 0, 0, null);
    }
    public TextView getActionView(int iCaption, final int iAction, final int iSize) {
        return getActionView(iCaption, iAction, 0, iSize, null);
    }
    public TextView getActionView(int iCaption, int iAction, int iImage, int iSize, Direction iconDirection) {
        return getActionView(getString(iCaption), iAction, iImage, iSize, iconDirection);
    }

    public TextView getTextView(int resId, java.lang.Object... formatArgs) {
        String s;
        if ( resId == 0 ) {
            s = "";
        } else {
            s = getString(resId, formatArgs);
        }
        return getTextView(s);
    }
    public TextView getTextView(String s) {
        TextView txt = new TextView(context);
        txt.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        txt.setText(s);
        txt.setTextColor(Color.WHITE); // TODO: improve
        return txt;
    }

    /** size for icon in px, if specified test size is calculated accordingly */
    public TextView getActionView(String sCaption, final int iAction, int iImage, int iSize, Direction iconDirection) {
        //TextView txt = new TextView(context);
        TextView txt = new Button(context); // using Button will give it rounded borders
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
        txt.setOnClickListener(v -> {
            dismiss();
            handleButtonClick(v.getId());
        });

        int iPadding = Math.max(iSize / 10, 10);
        txt.setPadding(iPadding, iPadding, iPadding, iPadding);
        txt.setGravity(Gravity.CENTER);

        if ( (iImage != 0) && (iSize != 0) ) {
            Drawable d = null;
            try {
                d = context.getDrawable(iImage);
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

    protected void setIconToPackage(Context ctx, Object oBuilder, String packageName, int iResId) {
        if ( oBuilder instanceof android.app.AlertDialog.Builder ) {
            android.app.AlertDialog.Builder builder = (AlertDialog.Builder) oBuilder;
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
    }

}
