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

package com.doubleyellow.scoreboard.prefs;

/**
 * To indicate what part of a 'full name' of a player should be used.
 * Typically to prevent, that (if full name is used) autoresize will resize the text to very small.
 * If only first, or only last names are used autoresize can resize to bigger font to make it fit.
 */
public enum NamePart {
    First,
    Last,
    Full,
}
