package com.doubleyellow.scoreboard.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.doubleyellow.android.view.ViewUtil;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.util.StringUtil;

import static com.doubleyellow.scoreboard.dialog.MyDialogBuilder.iDialogTheme;
import static com.doubleyellow.scoreboard.dialog.MyDialogBuilder.iDialogTheme_Newer;

/**
 * TODO: ensure not to big on e.g. Wearable
 */
public abstract class BaseCustomDialog extends BaseAlertDialog {

    protected AlertDialog.Builder adb;
    private View      customLayout = null;
    private TextView  txtTitle;
    private TextView  txtMessage;
    private ImageView imgIcon;
    private Button    btnPositive;
    private Button    btnNeutral;
    private Button    btnNegative;

    public BaseCustomDialog(Context context, Model matchModel, ScoreBoard scoreBoard) {
        super(context, matchModel, scoreBoard);

        adb = new AlertDialog.Builder(context, ViewUtil.isLeanback_AndroidTV(context)?iDialogTheme_Newer:iDialogTheme);
        customLayout = LayoutInflater.from(context).inflate(R.layout.custom_dialog,null);
        adb.setView(customLayout);

        txtTitle   = customLayout.findViewById(R.id.custom_dialog_title);
        txtMessage = customLayout.findViewById(R.id.custom_dialog_message);
        imgIcon    = customLayout.findViewById(R.id.custom_dialog_icon);

        btnPositive = customLayout.findViewById(R.id.custom_dialog_positive);
        btnNeutral  = customLayout.findViewById(R.id.custom_dialog_neutral );
        btnNegative = customLayout.findViewById(R.id.custom_dialog_negative);

        txtTitle  .setVisibility(View.GONE);
        txtMessage.setVisibility(View.GONE);
        btnPositive.setVisibility(View.INVISIBLE);
        btnNeutral .setVisibility(View.INVISIBLE);
        btnNegative.setVisibility(View.INVISIBLE);
    }

    @Override AlertDialog.Builder setTitle(String s) {
        txtTitle.setText(s);
        txtTitle.setVisibility(StringUtil.isEmpty(s) ? View.GONE: View.VISIBLE);
        return adb; //.setTitle(iRes);
    }
    @Override AlertDialog.Builder setTitle(int iRes) {
        if ( iRes == 0 ) {
            txtTitle.setText("");
            txtTitle.setVisibility(View.GONE);
        } else {
            txtTitle.setText(iRes);
            txtTitle.setVisibility(View.VISIBLE);
        }

        return adb; //.setTitle(iRes);
    }
    @Override AlertDialog.Builder setIcon(final int iRes) {
        if ( iRes == 0 ) {
            //imgIcon.setImageResource(null);
            imgIcon.setVisibility(View.GONE);
        } else {
            int iResUse = iRes;
            Resources resources = context.getResources();
            final String sResName  = resources.getResourceName    (iRes);
            final String sResType  = resources.getResourceTypeName(iRes);
            if ( sResName.endsWith("_white") ) {
                // there should be a black
                iResUse = resources.getIdentifier(sResName.replace("_white", "_black"), sResType, context.getPackageName());
            } else {
                // there might be a black
                iResUse = resources.getIdentifier(sResName + "_black", sResType, context.getPackageName());
            }
            if ( iResUse == 0 ) {
                iResUse = iRes;
            }
            imgIcon.setImageResource(iResUse);
        }
        return adb; //.setIcon(iRes);
    }
    @Override AlertDialog.Builder setMessage(String s) {
        txtMessage.setText(s);
        txtMessage.setVisibility(View.VISIBLE);
        return adb; //.setMessage(s);
    }
    @Override AlertDialog.Builder setPositiveButton(String s, DialogInterface.OnClickListener onClickListener) {
        btnPositive.setText(s);
        btnPositive.setVisibility(View.VISIBLE);
        if ( onClickListener != null ) {
            btnPositive.setOnClickListener(new DialogInterfaceToOnclick(onClickListener));
        }
        return adb; //.setPositiveButton(s, onClickListener);
    }
    @Override AlertDialog.Builder setNegativeButton(String s, DialogInterface.OnClickListener onClickListener) {
        btnNegative.setText(s);
        btnNegative.setVisibility(View.VISIBLE);
        if ( onClickListener != null ) {
            btnNegative.setOnClickListener(new DialogInterfaceToOnclick(onClickListener));
        }
        return adb;
    }
    @Override AlertDialog.Builder setNeutralButton(int i, DialogInterface.OnClickListener onClickListener) {
        btnNeutral.setText(i);
        btnNeutral.setVisibility(View.VISIBLE);
        return adb; //.setNeutralButton(s, onClickListener);
    }

    @Override public AlertDialog create() {
        dialog = adb.create();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        return dialog;
    }

    @Override protected Button getButton(/*AlertDialog alertDialog, */int iButton) {
        switch (iButton) {
            case DialogInterface.BUTTON_POSITIVE:
                return btnPositive;
            case DialogInterface.BUTTON_NEUTRAL:
                return btnNeutral;
            case DialogInterface.BUTTON_NEGATIVE:
                return btnNegative;
        }
        //return alertDialog.getButton(iButton);
        return null;
    }


    private class DialogInterfaceToOnclick implements View.OnClickListener {
        DialogInterface.OnClickListener onClickListener = null;
        DialogInterfaceToOnclick(DialogInterface.OnClickListener onClickListener) {
            this.onClickListener = onClickListener;
        }
        @Override public void onClick(View v) {
            int id = v.getId();
            int which = DialogInterface.BUTTON_POSITIVE;
            if ( id == R.id.custom_dialog_negative) {
                which = DialogInterface.BUTTON_NEGATIVE;
            } else if ( id == R.id.custom_dialog_neutral) {
                which = DialogInterface.BUTTON_NEUTRAL;
            }
            this.onClickListener.onClick(null, which);
        }
    }
}
