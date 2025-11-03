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
import android.text.Html;
import android.util.Log;
import android.webkit.WebView;

import com.doubleyellow.android.util.ColorUtil;
import com.doubleyellow.android.view.ViewUtil;
import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.URLFeedTask;
import com.doubleyellow.scoreboard.prefs.ColorPrefs;
import com.doubleyellow.util.StringUtil;

import java.util.Map;

/**
 * Introduced to apply some color to e.g. the Dialog title.
 */
public class MyDialogBuilder extends AlertDialog.Builder {

    private static final String TAG = "SB." + MyDialogBuilder.class.getSimpleName();

    //private static final int iDialogTheme = android.R.style.Theme_Translucent_NoTitleBar_Fullscreen; // this does not work, we need to specify a theme, not a style
    //private static final int iDialogTheme = R.style.SBDialog;
    //private static final int iDialogTheme = android.R.style.Theme_Dialog;
    private static final int iDialogTheme_Depr  = AlertDialog.THEME_TRADITIONAL; // white titles on black background, spacing around buttons
    //public static final int iDialogTheme_Newer = android.R.style.Theme_Material_Dialog_Alert;   // white titles on grey background, Neutral button on left, Pos and Neg buttons together on right
    private static final int iDialogTheme_Newer = android.R.style.Theme_Material_Light_Dialog_Alert; // black titles on white background, Neutral button on left, Pos and Neg buttons together on right
    //private static final int iDialogTheme = AlertDialog.THEME_HOLO_LIGHT;  // blue titles on white background, no spacing around buttons
    //private static final int iDialogTheme = AlertDialog.THEME_HOLO_DARK;   // blue titles on dark grey background, no spacing around buttons
    //private static final int iDialogTheme = AlertDialog.THEME_DEVICE_DEFAULT_LIGHT; // dark titles on light background, no spacing around buttons/ buttons borders not visible
    //private static final int iDialogTheme = AlertDialog.THEME_DEVICE_DEFAULT_DARK; // white titles on dark grey background, no spacing around buttons/ buttons borders not visible

    public static boolean isUsingNewerTheme(Context context) {
        boolean leanback_androidTV = ViewUtil.isLeanback_AndroidTV(context);
        return leanback_androidTV;
    }
    public static int getDialogTheme(Context context) {
        return isUsingNewerTheme(context) ?iDialogTheme_Newer:iDialogTheme_Depr;
    }

    private Map<ColorPrefs.ColorTarget, Integer> target2colorMapping;
    private int iDialogTheme = 0;

    public MyDialogBuilder(Context context) {
        this(context, getDialogTheme(context));
    }
    protected MyDialogBuilder(Context context, int iTheme) {
        super(context, iTheme);
        iDialogTheme  = iTheme;
        target2colorMapping = ColorPrefs.getTarget2colorMapping(getContext());
    }

    @Override public AlertDialog.Builder setTitle(int titleId) {
        Context context = getContext();
        return this.setTitle(context.getString(titleId));
    }

    @Override public AlertDialog.Builder setTitle(CharSequence sTitle) {
        if ( target2colorMapping != null ) {
            Integer newColor = target2colorMapping.get(ColorPrefs.ColorTarget.middlest);
            String  sColor   = ColorUtil.getRGBString(newColor);
            long iDistanceToBlack = ColorUtil.getDistance2Black(sColor);
            if ( iDistanceToBlack < 50 ) {
                // e.g. when using monochrome black
                sColor = "#FFFFFF";
            }
            sTitle = Html.fromHtml("<font color='" + sColor + "'>" + sTitle + "</font>");
        }
        AlertDialog.Builder builder = super.setTitle(sTitle);
        return builder;
    }

    @Override public AlertDialog.Builder setIcon(int iconId) {
        return super.setIcon(iconId);
    }

    @Override public AlertDialog create() {
        Log.w(TAG, "Try to use show() with listener if possible");
        AlertDialog dialog = super.create();
        return dialog;
    }

    @Override public AlertDialog show() {
        ButtonUpdater listener = new ButtonUpdater(getContext());
        return this.show(listener);
    }

    public AlertDialog show(DialogInterface.OnShowListener onShowListener) {
        AlertDialog dialog = super.create();
        dialog.setOnShowListener(onShowListener);
        try {
            dialog.show();
        } catch (Exception e) {
            e.printStackTrace();
/* IH 20180322: try catch to prevent crash for following exception (reported for apk 183 on android 5.0 and 7.0)
                android.view.WindowManager$BadTokenException:
                at android.view.ViewRootImpl.setView (ViewRootImpl.java:922)
                at android.view.WindowManagerGlobal.addView (WindowManagerGlobal.java:377)
                at android.view.WindowManagerImpl.addView (WindowManagerImpl.java:105)
                at android.app.Dialog.show (Dialog.java:404)
                at com.doubleyellow.scoreboard.main.ScoreBoard$MyDialogBuilder.show (ScoreBoard.java:4018)
                at com.doubleyellow.scoreboard.main.ScoreBoard$MyDialogBuilder.show (ScoreBoard.java:4012)
                at com.doubleyellow.scoreboard.dialog.EditFormat.show (EditFormat.java:128)
                at com.doubleyellow.scoreboard.main.DialogManager.showNextDialog (DialogManager.java:116)
                at com.doubleyellow.scoreboard.main.ScoreBoard.showNextDialog (ScoreBoard.java:4061)
                at com.doubleyellow.scoreboard.main.ScoreBoard.triggerEvent (ScoreBoard.java:2812)
                at com.doubleyellow.scoreboard.timer.Timer$SBCountDownTimer.onFinish (Timer.java:242)            }
                at android.os.CountDownTimer$1.handleMessage (CountDownTimer.java:127)
*/
        }
        return dialog;
    }

    public static AlertDialog dialogWithOkOnly(Context context, String sMsg) {
        return dialogWithOkOnly(context, null, sMsg, false);
    }
    public static AlertDialog dialogWithOkOnly(Context context, int iResTitle, int iResMsg, boolean bAlert) {
        return dialogWithOkOnly(context, context.getString(iResTitle), context.getString(iResMsg, Brand.getShortName(context)), bAlert);
    }
    public static AlertDialog dialogWithOkOnly(Context context, String sTitle, String sMsg, boolean bAlert) {
        AlertDialog.Builder ab = new MyDialogBuilder(context);
        ab.setPositiveButton(android.R.string.ok, null);

        if ( StringUtil.isNotEmpty(sTitle) ) {
            ab.setTitle(sTitle);
            ab.setIcon(bAlert? android.R.drawable.ic_dialog_alert: android.R.drawable.ic_dialog_info);
        }

        if ( StringUtil.isNotEmpty(sMsg) ) {
            if ( sMsg.trim().toLowerCase().endsWith("html>") ) {
                try {
                    WebView wv = new WebView(context); // seen this crash because of unclear reasons on android5
                    //wv.loadData(sMsg, "text/html; charset=utf-8", "UTF-8"); // css not displayed
                    //wv.loadData(sMsg, "text/html", "UTF-8"); // css not displayed
                    wv.loadDataWithBaseURL(URLFeedTask.prefixWithBaseIfRequired("/"), sMsg, "text/html; charset=utf-8", "UTF-8", null); // allows css
                    ab.setView(wv);
                } catch (Exception e) {
                    Log.w(TAG, "Could not initialize webview");
                    return null;
                }
            } else {
                ab.setMessage(sMsg);
            }
        }

        try {
            return ab.show();
        } catch (Exception e) {
/* IH 20170607: try catch to prevent crash for following exception (reported for apk 144 on android 5.0 and 7.0)
            android.view.WindowManager$BadTokenException:
            at android.view.ViewRootImpl.setView(ViewRootImpl.java:570)
            at android.view.WindowManagerGlobal.addView(WindowManagerGlobal.java:272)
            at android.view.WindowManagerImpl.addView(WindowManagerImpl.java:69)
            at android.app.Dialog.show(Dialog.java:298)
            at android.app.AlertDialog$Builder.show(AlertDialog.java:987)
            at com.doubleyellow.scoreboard.main.ScoreBoard.dialogWithOkOnly(ScoreBoard.java:3547)
*/
            return null;
        }
    }

}
