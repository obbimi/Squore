/*
 * Copyright (C) 2026  Iddo Hoeve
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
package com.doubleyellow.scoreboard.match.emulator;

public enum Keys {
    /** e.g  1 for normal speed, 10 to emulate time is going 10x faster */
    SpeedUpFactor(Integer.class),
    /** e.g  6, 12, or 18 percent */
    LikelihoodAppeal(Integer.class),
    /** e.g  45, 50, 55 percent */
    LikelihoodPlayerAWinsRally(Integer.class),
    /** e.g  20 or 30 seconds */
    RallyDuration_Average(Integer.class),
    /** e.g  10 (around half of the average) */
    RallyDuration_Deviation(Integer.class),
    /** e.g true or 1 to have it emulate a warmup timer */
    StartWarmupTimer(Boolean.class),
    /** */
    LikelihoodSwitchServeSideOnHandout(Integer.class),
    /** */
    LikelihoodUndoRequiredByRef(Integer.class),
    ;

    private Class clazz = String.class;
    Keys(Class c) {
        this.clazz = c;
    }
    public Class getType() {
        return clazz;
    }
}
