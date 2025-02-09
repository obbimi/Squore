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
package com.doubleyellow.scoreboard.main;

import android.hardware.SensorManager;
import android.util.Log;
import android.view.OrientationEventListener;

/**
 * Swap players on 180 degrees.
 *
 * For all versions except for Squash
 **/
class SwapPlayersOn180Listener extends OrientationEventListener {
    private static final String TAG = "SB." + SwapPlayersOn180Listener.class.getSimpleName();
    private Integer m_previousDegrees = null;
    ScoreBoard scoreBoard = null;

    SwapPlayersOn180Listener(ScoreBoard context) {
        super(context , SensorManager.SENSOR_DELAY_NORMAL);
        this.scoreBoard = context;
    }

    @Override public void onOrientationChanged(int iDegrees) {
        // returns the number of degrees the device is rotated in comparison to its default orientation
        // for phones default portrait means orientation is near 0
        // for tables default landscape means orientation is near 0
        // a value of -1 is return if the phone is more or less horizontal
        // these values still come in nicely even if you set your screen to e.g. landscape only
        if ( iDegrees == ORIENTATION_UNKNOWN ) { return; }
        if ( scoreBoard.isPortrait()                    ) { return; } // no use for this functionality in portrait orientation

        float f0To4 = (float) iDegrees / 90;
        int   i0To4 = Math.round(f0To4) % 4;
        //Log.v(TAG, "Orientation changed to " + i0To4  + " , " + f0To4  + " , " + iDegrees);
        if ( m_previousDegrees != null ) {
            int iPrev = m_previousDegrees;
            if ( Math.abs(iPrev - i0To4) == 2 ) {
                // swapping
                // - from 0 to 2 or back, or
                // - from 1 to 3 or back
                Log.i(TAG, "Swap because going from " + iPrev + " to " + i0To4);
                scoreBoard.swapSides(0, null);
                m_previousDegrees = i0To4;
            }
        } else {
            m_previousDegrees = i0To4;
        }
    }

}
