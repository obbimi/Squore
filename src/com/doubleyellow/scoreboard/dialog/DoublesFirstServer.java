/*
 * Copyright (C) 2020  Iddo Hoeve
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

import android.content.Context;

import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.model.Model;

/**
 * Dialog in which the referee must indicate which of the two players of a team will actually start serving.
 * Introduced for Tabletennis and Badminton.
 *
 * This dialog will be presented before the start of each doubles games in tabletennis.
 */
public class DoublesFirstServer extends DoublesFirstServerReceiver
{
    public DoublesFirstServer(Context context, Model matchModel, ScoreBoard scoreBoard) {
        super(context, matchModel, scoreBoard, true);
    }
}
