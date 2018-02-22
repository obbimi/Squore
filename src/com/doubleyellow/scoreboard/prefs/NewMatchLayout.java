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

import com.doubleyellow.scoreboard.R;

public enum NewMatchLayout {
    AllFields(R.layout.match),
    Simple(R.layout.match_simplelayout),
    ;

    private int m_iResLayout = 0;
    NewMatchLayout(int iResId) {
        m_iResLayout = iResId;
    }
    public int getLayoutResId() {
        return m_iResLayout;
    }
}
