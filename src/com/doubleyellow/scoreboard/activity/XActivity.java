package com.doubleyellow.scoreboard.activity;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import com.doubleyellow.android.view.Orientation;
import com.doubleyellow.android.view.ViewUtil;

public class XActivity extends /*Activity*/ AppCompatActivity /* For Implementing ChromeCast */ {

    protected static final String TAG = "SB." + XActivity.class.getSimpleName();

    //----------------------------------------------------
    // Orientation methods
    //----------------------------------------------------

    private Orientation m_orientation = null;
    private Orientation getOrientation() {
        if ( m_orientation == null ) {
            m_orientation = ViewUtil.getCurrentOrientation(this);
        }
        return m_orientation;
    }
    protected boolean isLandscape() {
        return getOrientation().equals(Orientation.Landscape);
    }
    protected boolean isPortrait() {
        return getOrientation().equals(Orientation.Portrait);
    }

    //----------------------------------------------------
    // Actionbar methods
    //----------------------------------------------------

    /** Method to be able to quickly switch between  when extending Activity and AppCompatActivity */
    protected ActionBar getXActionBar() {
        return super.getSupportActionBar(); // with ChromeCast
        //return super.getActionBar();      // without Cast
    }

    protected void setActionBarVisibility(boolean bVisible) {
        ActionBar ab = getXActionBar();
        if ( ab != null ) {
            if ( bVisible ) {
                ab.show();
            } else {
                ab.hide();
            }
        }
    }
    public void setActiorBarBGColor(Integer iBgColor) {
        ActionBar bar = getXActionBar();
        if ( (bar != null) && (iBgColor != null)) {
            ColorDrawable cd = new ColorDrawable(iBgColor);
            bar.setBackgroundDrawable(cd);
        }
    }
    protected void setHomeButtonEnabledOnActionBar() {
        // enables the activity icon as a 'home' button. required if "android:targetSdkVersion" > 14
        ActionBar actionBar = getXActionBar();
        if ( actionBar != null ) {
            actionBar.setHomeButtonEnabled(true);
        }
    }

    //----------------------------------------------------
    // Rotation status helper methods
    //----------------------------------------------------

    protected enum OrientationStatus {
        Showing,
        ChangingOrientation,
        Closing,
    }
    protected static OrientationStatus status = OrientationStatus.Showing;
    void changeOrientationStatus(OrientationStatus newStatus) {
        //Log.w(TAG, String.format("Changing status from %s to %s", SplashScreen.status, newStatus));
        XActivity.status = newStatus;
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        changeOrientationStatus(OrientationStatus.ChangingOrientation);
    }

    /** invoked e.g. when orientation switches */
    @Override protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        changeOrientationStatus(OrientationStatus.Showing);
    }
}
