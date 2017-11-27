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

package com.doubleyellow.scoreboard.demo;

import android.view.View;

/**
 * helper class for demothread
 */
class ListenerAndView {

    View.OnClickListener     ocl  = null;
    View.OnLongClickListener olcl = null;
    int                      viewId = View.NO_ID;
    Integer                  actionId = null;
    DemoThread.DemoMessage    demoMessage = null;
    int demoMessageDuration = 0;
    int pauseDurationAfter  = 4;
    ListenerAndView(int v, Object o) {
        this(v, o, false);
    }
    ListenerAndView(int v, Object o, boolean bLong) {
        this.viewId = v;
        if ( (bLong == false) && o instanceof View.OnClickListener ) {
            this.ocl = (View.OnClickListener) o;
        }
        if ( (bLong == true) && o instanceof View.OnLongClickListener ) {
            this.olcl = (View.OnLongClickListener) o;
        }
    }
    ListenerAndView() {
        // does nothing
    }
    ListenerAndView(int iActionId) {
        this(iActionId, 2);
    }
    ListenerAndView(int iActionId, int iPauseDuration) {
        this.actionId = iActionId;
        this.pauseDurationAfter = iPauseDuration;
    }
    ListenerAndView(DemoThread.DemoMessage message) {
        this(message, 5, 5);
    }
    ListenerAndView(DemoThread.DemoMessage message, int iMessageDuration, int iPauseDuration) {
        this.demoMessage = message;
        this.demoMessageDuration = iMessageDuration;
        this.pauseDurationAfter  = iPauseDuration;
    }
    ListenerAndView setPauseDuration(int iSecs) {
        ListenerAndView lavNew = _clone();

        lavNew.pauseDurationAfter = iSecs;
        return lavNew;
    }

    private ListenerAndView _clone() {
        ListenerAndView lavNew = new ListenerAndView();
        lavNew.viewId   = this.viewId;
        lavNew.ocl      = this.ocl;
        lavNew.olcl     = this.olcl;
        lavNew.actionId = this.actionId;
        return lavNew;
    }
}
