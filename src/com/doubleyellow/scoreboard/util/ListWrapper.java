package com.doubleyellow.scoreboard.util;

import android.support.annotation.Nullable;
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
    public ListWrapper() {
        super();
    }
    public ListWrapper(Collection<? extends E> c) {
        super(c);
    }
    public ListWrapper setName(String s) {
        m_sName = s;
        return this;
    }

    @Override public boolean add(E e) {
        Log.d(m_sName, "adding : " + e);
        return super.add(e);
    }

    @Override public E remove(int index) {
        Log.d(m_sName, "removing i : " + index);
        return super.remove(index);
    }

    @Override public boolean remove(@Nullable Object o) {
        Log.d(m_sName, "removing object : " + o);
        return super.remove(o);
    }

    @Override public String toString() {
        return StringUtil.pad(m_sName, ' ', 8, true)  + ": " + super.toString();
    }
}
