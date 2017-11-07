package com.doubleyellow.scoreboard.main;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import com.doubleyellow.scoreboard.dialog.BaseAlertDialog;
import com.doubleyellow.scoreboard.dialog.ChildActivity;
import com.doubleyellow.scoreboard.dialog.GenericMessageDialog;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.timer.TwoTimerView;
import com.doubleyellow.scoreboard.vico.IBoard;
import com.doubleyellow.util.ListUtil;
import com.doubleyellow.util.StringUtil;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

public class DialogManager {

    private static final String TAG = "SB." + DialogManager.class.getSimpleName();

    private static DialogManager instance = new DialogManager();
    public static DialogManager getInstance() {
        return instance;
    }

    public BaseAlertDialog baseDialog  = null;
    private List<BaseAlertDialog> baseDialogs = new ArrayList<BaseAlertDialog>();

    public void showMessageDialog(Context context, String sTitle, String sMessage) {
        GenericMessageDialog dialog = new GenericMessageDialog(context);
        dialog.init(sTitle, sMessage);
        addToDialogStack(dialog);
    }

    public synchronized void show(BaseAlertDialog dialog) {
        dialog.show();
        baseDialog = dialog;
    }
    public synchronized boolean addToDialogStack(BaseAlertDialog dialog) {
        if ( IBoard.getBlockToasts() ) { return false; }

        boolean bAlreadyOnTheStack = false;
        if ( baseDialogs.size() > 0 ) {
            // check if somehow a second dialog is added to the stack that is already their, do not add it
            for(BaseAlertDialog queuedDialog: baseDialogs) {
                if ( queuedDialog.getClass().equals(dialog.getClass()) ) {
                    Log.w(TAG, "dialog already on the stack " + dialog.getClass());
                    bAlreadyOnTheStack = true;
                    break;
                }
            }
        }
        if ( baseDialogs.size() > 0 ) {
            if ( baseDialogs.get(0).isShowing()== false ) {
                baseDialogs.remove(0);// e.g. for the EndGame dialog or ChildActivity
            }
        }
        if ( bAlreadyOnTheStack == false ) {
            baseDialogs.add(dialog);
        }
        if ( baseDialogs.size() == 1 ) {
            dialog.show();
            baseDialog = dialog;
        } else {
            if ( baseDialog == null || baseDialog.isModal() == false ) { // e.g. to still start the timer if statistics are showing
                showNextDialog();
            }
        }
        return true;
    }
    public void showNextDialogIfChildActivity() {
        if ( baseDialogs.size() > 0 && baseDialogs.get(0) instanceof ChildActivity) {
            showNextDialog();
        }
    }
    public boolean dismissIfTwoTimerView() {
        if ( (baseDialog != null) && (baseDialog instanceof TwoTimerView) ) {
            baseDialog.dismiss();
            return true;
        }
        return false;
    }
    public synchronized void showNextDialog() {
        // remove the one last shown from the stack
        if ( ListUtil.isNotEmpty(baseDialogs) ) {
            baseDialogs.remove(0);
            baseDialog = null;
        }
        if ( ListUtil.isNotEmpty(baseDialogs) ) {
            baseDialog = baseDialogs.get(0);
            baseDialog.show();
            if ( baseDialog.isModal() == false ) {
                showNextDialog();
            }
        }
    }
    /** Used by demo threads only */
    public boolean isDialogShowing() {
        boolean bNonTimerDialogShowing = ListUtil.isNotEmpty(baseDialogs) && baseDialogs.get(0).isShowing();
        if ( bNonTimerDialogShowing == false ) {
            bNonTimerDialogShowing = ((baseDialog != null) && baseDialog.isShowing());
        }
        boolean bTimerIsShowing        = (ScoreBoard.timer != null) && ScoreBoard.timer.isShowing();
        return bNonTimerDialogShowing || bTimerIsShowing;
    }
    public void removeDialog(Object o) {
        if ( o instanceof  BaseAlertDialog ) {
            BaseAlertDialog bad = (BaseAlertDialog) o;
            baseDialogs.remove(bad);
        }
    }
    public void clearDialogs() {
        baseDialogs.clear();
    }
    //private Model      matchModel = null;
    //private ScoreBoard scoreBoard = null;
    public void restoreInstanceState(Bundle savedInstanceState, Context context, Model matchModel, ScoreBoard scoreBoard) {
        //this.matchModel = matchModel;
        //this.scoreBoard = scoreBoard;

        String sShowingDialog = savedInstanceState.getString(SHOWING_DIALOG);
        if ( StringUtil.isNotEmpty(sShowingDialog) ) {
            try {
                Class         aClass       = Class.forName(sShowingDialog);
                Constructor[] constructors = aClass.getConstructors();
                Constructor   constructor  = constructors[0];
                switch(constructor.getParameterTypes().length) {
                    case 3:
                        baseDialog = (BaseAlertDialog) constructor.newInstance(context, matchModel, scoreBoard);
                        break;
                    case 2:
                        baseDialog = (BaseAlertDialog) constructor.newInstance(context, matchModel);
                        break;
                    case 1:
                        baseDialog = (BaseAlertDialog) constructor.newInstance(context);
                        break;
                }

                //Timer.knownTimerViews = null;
                if ( baseDialog.show(savedInstanceState) ) {
                    // showing something
                    //Log.i(TAG, "Re-instated " + baseDialog.getClass().getSimpleName());
                } else {
                    // dialog does not want to be restored
                    baseDialog = null;
                }
            } catch (Exception e) {
                Log.w(TAG, "Could not redraw dialog " + sShowingDialog + ": " + e.toString());
            }
        }
    }
    public void saveInstanceState(Bundle outState) {
        if ( baseDialog != null ) {
            synchronized ( baseDialog ) {
                boolean bDialogIsShowing = baseDialog.isShowing();
                //Log.i(TAG, "Dismissing " + baseDialog.getClass().getSimpleName());
                if ( bDialogIsShowing ) {
                    if ( baseDialog.storeState(outState) ) {
                        outState.putString(SHOWING_DIALOG, baseDialog.getClass().getName());
                    }
                }
                if ( baseDialog instanceof TwoTimerView == false ) {
                    baseDialog.dismiss(); // nicely dismiss the dialog to prevent android.view.WindowLeaked
                }
            }
        }
    }

    private static final String SHOWING_DIALOG       = "showingDialog";

}
