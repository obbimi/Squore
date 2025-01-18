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

package com.doubleyellow.scoreboard.activity;

import android.app.Activity;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
//import android.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import com.doubleyellow.android.view.Orientation;
import com.doubleyellow.android.view.ViewUtil;

public abstract class XActivity extends /*Activity*/ AppCompatActivity /* For Implementing ChromeCast: just the button?! */ {

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
    public boolean isLandscape() {
        return getOrientation().equals(Orientation.Landscape);
    }
    public boolean isPortrait() {
        return getOrientation().equals(Orientation.Portrait);
    }

    public FragmentManager getSupportFragmentManager() {
        return super.getSupportFragmentManager();
        //return null; // TODO
    }
    //----------------------------------------------------
    // Actionbar methods
    //----------------------------------------------------

    /** Method to be able to quickly switch between when extending Activity and AppCompatActivity */
    public ActionBar getXActionBar() {
        try {
            return super.getSupportActionBar(); // with ChromeCast
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
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
    public void setActionBarBGColor(Integer iBgColor) {
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
