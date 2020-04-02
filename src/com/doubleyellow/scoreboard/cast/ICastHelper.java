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
import android.view.Menu;

import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.prefs.ColorPrefs;
import com.doubleyellow.scoreboard.timer.TimerView;

import java.util.Map;

public interface ICastHelper {
    void initCasting(ScoreBoard activity);

    void initCastMenu(Activity activity, Menu menu, int iResIdMenuItem);

    void onActivityStart_Cast();
    void onActivityStop_Cast();
    void onActivityPause_Cast();
    void onActivityResume_Cast();

    boolean isCasting();


    void setModelForCast(Model matchModel);

    TimerView getTimerView();

    void castColors(Map<ColorPrefs.ColorTarget, Integer> mColors);

    void castDurationChronos();

    void castGamesWonAppearance();
}
