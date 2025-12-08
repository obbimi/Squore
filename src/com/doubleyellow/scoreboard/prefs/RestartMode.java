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
package com.doubleyellow.scoreboard.prefs;

public enum RestartMode {
    /**
     *
     * In debug mode in emulator
     * - asking to restart not really required... seems to nicely work without manual intervention
     *
     **/
    Activity_recreate,
    /**
     *
     * In debug mode in emulator
     * - manual starting the app up again is required
     *
     **/
    System_exit,
}
