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

package com.doubleyellow.scoreboard.model;

import java.util.EnumSet;

public enum Halfway
{
    Before(false, null),

    /* 5-2 in a game to 11: tabletennis -> switch sides */
    JustBefore(true, EnumSet.of(SportType.Tabletennis)),
    /** 5-3 in game to 10 (even points)... less common */
    Exactly   (true, EnumSet.of(SportType.Tabletennis
                              , SportType.Racketlon)),
    /* 11-7 in a game to 21: racketlon -> switch sides */
    JustAfter (true, EnumSet.of(SportType.Racketlon)),

    After(false, null),
    ;

    private boolean isHalfway = false;
    private EnumSet<SportType> forSports;

    Halfway(boolean isHalfway, EnumSet<SportType> forSports) {
        this.isHalfway = isHalfway;
        this.forSports = forSports;
    }

    public boolean isHalfway() {
        return isHalfway;
    }

    public boolean changeSidesFor(SportType sport) {
        return this.forSports != null && this.forSports.contains(sport);
    }
}
