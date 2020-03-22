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

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import androidx.appcompat.app.ActionBar;

import android.view.View;
import android.widget.ImageView;
import com.doubleyellow.android.view.ViewUtil;
import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.prefs.*;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.main.ScoreBoard;

import java.util.Arrays;

public class SplashScreen extends XActivity
{
    private final Class<? extends ScoreBoard> cls = ScoreBoard.class;

    private XActivity activity = null;
/*
    public SplashScreen(XActivity x)
    {
        activity = x;
    }
*/
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.activity = this;
        if ( bMainIsStarted ) {
            // splash screen has already been shown, just start the main activity and close this one
            startMain();
            finish();
        } else {
            initSplash();
        }
    }

    @Override protected void onDestroy() {
        if ( XActivity.status.equals(XActivity.OrientationStatus.Showing) ) {
            activity.changeOrientationStatus(XActivity.OrientationStatus.Closing);

            if ( ct != null ) {
                // ensure main is not started from the timer anymore
                ct.cancel();
                ct = null;
            }
            if ( bMainIsStarted == false ) {
                startMain();
            }
        }
        super.onDestroy();
    }

    private void initSplash() {
        if ( PreferenceValues.isUnbrandedExecutable(activity) ) {
            Brand.brand = PreferenceValues.getOverwriteBrand(activity);
        }

        final int SPLASH_TIME_OUT = Brand.getSplashDuration();

        activity.changeOrientationStatus(XActivity.OrientationStatus.Showing);

        if ( SPLASH_TIME_OUT > 0 ) {
            activity.setContentView(R.layout.splashscreen);

            ActionBar ab = activity.getSupportActionBar();
            if (ab != null) {
                ab.hide();
            }
            ViewUtil.setFullScreen(getWindow(), true);

            View root = activity.findViewById(R.id.splash_root_view);
            root.setBackgroundColor(Brand.getBgColor(activity));
            ImageView logo = (ImageView) activity.findViewById(R.id.splash_logo);

            logo.setImageResource  (Brand.getLogoResId());
            logo.setBackgroundColor(Brand.getBgColor(activity));
        }

        int iRunCount = PreferenceValues.getRunCount(activity, PreferenceKeys.OrientationPreference);
        if ( iRunCount <= 1 ) {
            PreferenceManager.setDefaultValues(activity, R.xml.preferences, false);
        }
        if ( iRunCount <= 10 || PreferenceValues.isUnbrandedExecutable(activity) ) {
            // change the preferences only the first few times the app is used.
            // User might change preferences himself, do not overwrite these
            Brand.setBrandPrefs(activity);
        }
        if ( (iRunCount < 100) && (Brand.brand != Brand.Squore) ) {
            // prefer using no countries (only clubs)
            //PreferenceValues.setNumber(PreferenceKeys.numberOfCharactersBeforeAutocompleteCountry, this, 100);
            //PreferenceValues.useCountries(getApplication());
          //PreferenceValues.setString   (PreferenceKeys.showCountryAs, this                   , ShowCountryAs.FlagNextToNameChromeCast     + "," + ShowCountryAs.FlagNextToNameOnDevice);
            PreferenceValues.setStringSet(PreferenceKeys.showCountryAs.toString(), Arrays.asList(ShowCountryAs.FlagNextToNameChromeCast.toString(), ShowCountryAs.FlagNextToNameOnDevice.toString()), activity);
        }

        // check some preferences after Brand.setBrandPrefs
/*
        if ( false ) {
            Feature featureAnnounce = PreferenceValues.useOfficialAnnouncementsFeature(activity);
            Feature featureShare = PreferenceValues.useShareFeature(activity);

            EnumSet<OrientationPreference> tst = PreferenceValues.getOrientationPreference(activity);
            boolean bTst = PreferenceValues.showFullScreen(activity);
        }
*/

        if ( ct == null ) {
            ct = new StartMain(SPLASH_TIME_OUT);
            ct.start();
        }
    }

    private static boolean bMainIsStarted = false;
    private void startMain() {
        //if ( bMainIsStarted ) { return; }
        if ( XActivity.class.isAssignableFrom(SplashScreen.this.getClass()) ) {
            Intent i = new Intent(activity, cls);
            activity.startActivity(i);
        } else {
            // TODO
        }
        bMainIsStarted = true;
    }

    private static StartMain ct = null;
    private class StartMain extends CountDownTimer {
        private StartMain(int iSplashTimeout) {
            super(iSplashTimeout, Math.min(iSplashTimeout,200));
        }
        @Override public void onTick(long millisUntilFinished) {
            if ( (millisUntilFinished <= 400) && (bMainIsStarted == false) ) {
                // start main before actually closing this splashscreen to have the transition appear more smoothly
                startMain();
            } else {
                int splashDuration = Brand.getSplashDuration();
/*
                if ( millisUntilFinished > (splashDuration - 1000) && millisUntilFinished < (splashDuration - 600)) {
                    String sSSFilename = Util.filenameForAutomaticScreenshot(activity, null, ShowOnScreen.OnDevice, -1, -1, "%s.Splash.%s.png");
                    if ( sSSFilename!=null ) {
                        PreferenceValues.requestPermission(activity, PreferenceKeys.targetDirForImportExport, Manifest.permission.WRITE_EXTERNAL_STORAGE);
                        ViewUtil.takeScreenShot(activity, Brand.brand, sSSFilename, activity.findViewById(R.id.splash_root_view));
                    }
                }
*/
            }
        }
        @Override public void onFinish() {
            if ( XActivity.class.isAssignableFrom(SplashScreen.this.getClass()) ) {
                activity.finish();
            }
        }
    };
}
