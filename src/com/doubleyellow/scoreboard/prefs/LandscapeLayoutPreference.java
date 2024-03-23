/*
 * Copyright (C) 2024  Iddo Hoeve
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

import com.doubleyellow.scoreboard.R;

public enum LandscapeLayoutPreference {
    Default       (R.layout.constraint ), /* same look and feel as default/portrait */
    Presentation1 (R.layout.presentation1), /* Two lines: | Player X | Sets Won | Games Won | Points Scored | */
    Presentation2 (R.layout.presentation2),
    Presentation3 (R.layout.presentation3), /* Two lines: | Player X | Sets Won | Games Won | Points Scored (BIGGER) | */
    ;
    private int  m_iLayoutResource       = 0;
    LandscapeLayoutPreference(int iLayoutResource) {
        m_iLayoutResource = iLayoutResource;
    }
    public int getLayoutResourceId() {
        return m_iLayoutResource;
    }
}
