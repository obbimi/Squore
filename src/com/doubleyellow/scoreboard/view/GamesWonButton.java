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
import android.util.AttributeSet;

import com.doubleyellow.android.view.AutoResizeTextView;

/**
 * GamesWon button that indicates
 * - number of games won on the main board
 * - in GSM only used for the set in progress, for finished sets the number of games is displayed using ScorePlusSmallScore
 */
public class GamesWonButton extends AutoResizeTextView
{
/*
    public GamesWonButton(Context context) {
        this(context, null, 0); // not used
    }
*/
    /** Required for usage in layout xml */
    public GamesWonButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GamesWonButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public String setGamesWon(Integer o) {
        String sValue = String.valueOf(o);
        this.setText(sValue); // final method

        return sValue;
    }

    @Override public void setVisibility(int visibility) {
        int iCur = super.getVisibility();
        if ( iCur != visibility ) {
            super.setVisibility(visibility);
        }
    }
}
