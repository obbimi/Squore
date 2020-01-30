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

/** TODO rename to DoublePlayer, since it is used to indicate a team player for both Serving as Receiving team */
public enum DoublesServe {
    I, // In =player 1
    O, // Out=player 2
    NA,
    ;
    public DoublesServe getOther() {
        if ( this.equals(I) ) return O;
        if ( this.equals(O) ) return I;
        return NA;
    }
}
