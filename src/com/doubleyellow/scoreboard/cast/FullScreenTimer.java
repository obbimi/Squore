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

package com.doubleyellow.scoreboard.cast;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Chronometer;
import android.widget.TextView;

import com.doubleyellow.android.util.ColorUtil;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.prefs.ColorPrefs;
import com.doubleyellow.scoreboard.timer.Timer;
import com.doubleyellow.scoreboard.timer.TimerView;
import com.doubleyellow.scoreboard.timer.TimerViewContainer;
import com.doubleyellow.scoreboard.vico.IBoard;

import java.util.Map;

/**
 * View holding
 * - timer
 */
public class FullScreenTimer implements TimerViewContainer
{
    private static final String TAG = "SB." + FullScreenTimer.class.getSimpleName();

    private ViewGroup          root;
    private Context            context     = null;
    private IBoard             iBoard      = null;
    private TextView           txtTimer    = null;
            boolean            bIsShowing  = false;
    private TimerView          timerView   = null;
    private Model              matchModel  = null;

    public FullScreenTimer(Context context, IBoard iBoard, Model model) {
        this.context    = context;
        this.iBoard     = iBoard;
        this.matchModel = model;
    }
    void setModel(Model model) {
        this.matchModel = model;
    }

    private static class EOGTimerView extends com.doubleyellow.scoreboard.timer.SBTimerView {
        EOGTimerView(TextView textView, Chronometer cmToLate, Context context, IBoard iBoard) {
            super(textView, cmToLate, context, iBoard);
        }
    }
    @Override public TimerView getTimerView() {
        if ( timerView == null ) {
            if ( txtTimer != null ) {
                timerView = new EOGTimerView(txtTimer, null, context, iBoard);
            } else {
                Log.w(TAG, "Can not create timerview yet.");
            }
        }
        return timerView;
    }
    public void show(ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        root = (ViewGroup) inflater.inflate(R.layout.fullscreen_timer, parent);
        show();
    }
    public void show(Activity screen) {
        screen.setContentView(R.layout.fullscreen_timer);
        root = (ViewGroup) screen.findViewById(android.R.id.content);
        show();
    }
    private void show() {
        if ( root   == null ) { return; }
        if ( iBoard == null ) { return; }

        bIsShowing = true;

        Map<ColorPrefs.ColorTarget, Integer> mColors = ColorPrefs.getTarget2colorMapping(context);
        ColorUtil.setBackground(root, mColors.get(ColorPrefs.ColorTarget.black));

        // enforce re-create of timerview
        Timer.removeTimerView(false, timerView);
        timerView = null;

                            txtTimer = (TextView           ) root.findViewById(R.id.peog_timer     );
        if ( txtTimer != null ) {
            if ( matchModel.matchHasEnded() ) {
                txtTimer.setVisibility(View.INVISIBLE);
            } else {
                TimerView timerView = this.getTimerView();
                boolean bAdded = Timer.addTimerView(iBoard.isPresentation(), timerView);
                //Log.d(TAG, "Timerview added : " + bAdded);
            }
        }

        bIsShowing = true;
    }
}
