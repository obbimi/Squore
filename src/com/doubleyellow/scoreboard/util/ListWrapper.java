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
package com.doubleyellow.scoreboard.util;

import androidx.annotation.Nullable;
import android.util.Log;

import com.doubleyellow.util.StringUtil;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Wrapper class for troubleshooting.
 * Allows more easily to detect when elements are added/remove from a member variable e.g. in the Model class
 */
public class ListWrapper<E> extends ArrayList<E>
{
    private String m_sName = null;
    private boolean m_bLog = false;
    public ListWrapper(boolean bLog) {
        super();
        m_bLog = bLog;
    }
    public ListWrapper(Collection<? extends E> c) {
        super(c);
    }
    public ListWrapper setName(String s) {
        m_sName = s;
        return this;
    }

    @Override public boolean add(E e) {
        if ( m_bLog ) {
            Log.d(m_sName, "adding : " + e);
        }
        return super.add(e);
    }

    @Override public E remove(int index) {
        if ( m_bLog ) {
            Log.d(m_sName, "removing i : " + index);
        }
        return super.remove(index);
    }

    @Override public boolean remove(@Nullable Object o) {
        if ( m_bLog ) {
            Log.d(m_sName, "removing object : " + o);
        }
        return super.remove(o);
    }

    @Override public String toString() {
        return StringUtil.pad(m_sName, ' ', 8, true)  + ": " + super.toString();
    }
}
