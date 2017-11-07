package com.doubleyellow.scoreboard.demo;

import android.view.View;

/**
 * helper class for demothread
 */
public class ListenerAndView {

    public  View.OnClickListener     ocl  = null;
    public  View.OnLongClickListener olcl = null;
    public  int                      viewId = View.NO_ID;
    public  Integer                  actionId = null;
    public  DemoThread.DemoMessage    demoMessage = null;
    public  int demoMessageDuration = 0;
    public  int pauseDurationAfter  = 4;
    ListenerAndView(int v, Object o) {
        this(v, o, false);
    }
    ListenerAndView(int v, Object o, boolean bLong) {
        this.viewId = v;
        if ( bLong == false && o instanceof View.OnClickListener ) {
            this.ocl = (View.OnClickListener) o;
        }
        if ( bLong == true && o instanceof View.OnLongClickListener ) {
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
    public ListenerAndView setPauseDuration(int iSecs) {
        ListenerAndView lavNew = _clone();

        lavNew.pauseDurationAfter = iSecs;
        return lavNew;
    }

    public ListenerAndView _clone() {
        ListenerAndView lavNew = new ListenerAndView();
        lavNew.viewId   = this.viewId;
        lavNew.ocl      = this.ocl;
        lavNew.olcl     = this.olcl;
        lavNew.actionId = this.actionId;
        return lavNew;
    }
}
