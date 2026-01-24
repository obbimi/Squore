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

import java.util.List;

/**
 * Key values to be specified in feeds under Config.emulate_Config
 */
public enum Keys {
    /** e.g  1 for normal speed, 10 to emulate time is going 10x faster, etc */
    SpeedUpFactor(Integer.class),

    /** e.g  Value between 1 and 99. E.g. 6, 12, or 18 percent */
    LikelihoodAppeal(Integer.class),
    /** List of values between 1 and 99 e.g  45, 50, 55 percent */
    LikelihoodPlayerAWinsRallyInGameX(List.class),

    /** e.g Average or Mean, In seconds, e.g. 20 or 30 seconds */
    RallyDuration_Average(Integer.class),
    /** e.g Standard Deviation In seconds. E.g. 10 (typically around half of the average) */
    RallyDuration_Deviation(Integer.class),

    /** */
    LikelihoodSwitchServeSideOnHandout(Integer.class),
    /** */
    LikelihoodUndoRequiredByRef(Integer.class),

    PreferenceKeysToRotatePerMatch(List.class),
    ShowInfoMessagesAboutSimulation(Boolean.class),
    ;

    private Class clazz = String.class;
    Keys(Class c) {
        this.clazz = c;
    }
    public Class getType() {
        return clazz;
    }
}
