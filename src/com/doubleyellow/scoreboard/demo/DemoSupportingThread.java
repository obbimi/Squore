package com.doubleyellow.scoreboard.demo;

import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.util.MapUtil;
import com.doubleyellow.util.Params;

/**
 * Class that helps in making full educational movie for the Squore app.
 *
 * Run this while connected via adb.
 * In the Android console, warning message will be printed to guide you through it (.guidance() messages)
 */
public class DemoSupportingThread extends DemoThread
{
    public DemoSupportingThread(ScoreBoard context, Model matchModel) {
        super(context, matchModel);
    }

    @Override public void run() {
    //@Override protected Object doInBackground(Object[] objects) {
        try {
            Looper.prepare(); // TODO: Only one Looper may be created per thread
        } catch (Throwable e) {
            e.printStackTrace();
        }
        applicableMessage();
        //return null;
    }

    public static DemoMessage startAt = null;

    private void applicableMessage() {
        //long lLastToastTime = 0L;

        Params mShown = new Params();
        while ( MapUtil.size(mShown) < DemoMessage.values().length && (bStopLoop == false) ) {
            pause(200);

/*
            if ( System.currentTimeMillis() - lLastToastTime < 3000 ) {
                Log.d(TAG, "Waiting before next info message");
                continue;
            }
*/

            DemoMessage shownMsg = null;
            for(DemoMessage message: DemoMessage.values()) {
                String[] sMessages = message.getMessages(matchModel);

                // if a 'start at' is temporarily given, pretend as if all the others have been shown
                if ( startAt != null ) {
                    if ( startAt.equals(message) ) {
                        startAt = null;
                    } else {
                        mShown.increaseCounter(message, sMessages.length);
                        continue;
                    }
                }

/*
                int iMsgCntShown = mShown.getOptionalInt(message, 0);

                if ( iMsgCntShown >= sMessages.length ) {
                    continue;
                }
*/
                while ( isDemoMessageShowing() && (bStopLoop==false) ) {
                    pause(100);
                }
                while ( scoreBoard.isDialogShowing() ) {
                    pause(1000);
                }
                // loop over them in sequence
                Log.w("#", "##########################################################################################################");
                Log.w("#", "# Waiting to be ready for " + message + ". " + sMessages[0]);
                Log.w("#", "# [Guidance] " + message.guidance());
                Log.w("#", "##########################################################################################################");
                while ( readyForMessage(message) == false ) {
                    pause(1000);
                    if ( bStopLoop ) {
                        break;
                    }
                }
                if ( bStopLoop ) {
                    break;
                }

                shownMsg = message;
                int[] iResId = message.focusOn(matchModel);
                for(int iMsgCntShown=0;iMsgCntShown<sMessages.length;iMsgCntShown++) {
                    //Log.d(TAG, "Showing " + shownMsg + "[" + iMsgCntShown + "]");
                    showInfo(sMessages, iMsgCntShown, iResId, 30);
                    pause(1500);
                    while ( isDemoMessageShowing() && (bStopLoop==false) ) {
                        pause(100);
                    }
                }


                //lLastToastTime = System.currentTimeMillis();
                mShown.increaseCounter(shownMsg);
            }
        }
    }

    private boolean readyForMessage(DemoMessage message) {
        boolean bModelOk = message.appliesTo(matchModel, activity);

        boolean bFocusOnOk = true;
        int[] iResId = message.focusOn(matchModel);
        if ( (iResId != null) && (iResId[0] != 0)) {
            View guiItem = activity.findViewById(Math.abs(iResId[0]));
            if ( guiItem != null ) { // Menu Item is returned as View as well: com.android.internal.view.menu.ActionMenuItemView
                bFocusOnOk = (guiItem.getVisibility()== View.VISIBLE);
            } else if (menu != null ) {
                // maybe it is a menu item
                MenuItem menuItem = menu.findItem(iResId[0]);
                if ( menuItem != null ) {
                    bFocusOnOk = menuItem.isVisible();
                } else {
                    bFocusOnOk = false;
                }
            } else {
                bFocusOnOk = false;
            }
            if ( bFocusOnOk == false ) {
                //Log.w("#", message.toString() + ": Not yet visible " + scoreBoard.getResources().getResourceEntryName(iResId[0]) + " for " + activity.getTitle());
            }
        }

        return bModelOk && bFocusOnOk;
    }
}
