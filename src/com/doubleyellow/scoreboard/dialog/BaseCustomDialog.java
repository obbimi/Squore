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

import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.model.Model;

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

        adb = new AlertDialog.Builder(context);
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

    AlertDialog.Builder setTitle(int iRes) {
        txtTitle.setText(iRes);
        txtTitle.setVisibility(View.VISIBLE);

        return adb; //.setTitle(iRes);
    }
    AlertDialog.Builder setIcon(int iRes) {
        Resources resources = context.getResources();
        final String sResName  = resources.getResourceName    (iRes);
        final String sResType  = resources.getResourceTypeName(iRes);
        if ( sResName.endsWith("_white") ) {
            iRes = resources.getIdentifier(sResName.replace("_white", "_black"), sResType, context.getPackageName());
        }
        imgIcon.setImageResource(iRes);
        return adb; //.setIcon(iRes);
    }
    AlertDialog.Builder setMessage(String s) {
        txtMessage.setText(s);
        txtMessage.setVisibility(View.VISIBLE);
        return adb; //.setMessage(s);
    }
    AlertDialog.Builder setPositiveButton(String s) {
        DialogInterface.OnClickListener onClickListener = null;
        btnPositive.setText(s);
        btnPositive.setVisibility(View.VISIBLE);
        return adb; //.setPositiveButton(s, onClickListener);
    }
    AlertDialog.Builder setNegativeButton(String s) {
        DialogInterface.OnClickListener onClickListener = null;
        btnNegative.setText(s);
        btnNegative.setVisibility(View.VISIBLE);
        return adb; //.setNegativeButton(s, onClickListener);
    }
    AlertDialog.Builder setNeutralButton(int i) {
        DialogInterface.OnClickListener onClickListener = null;
        btnNeutral.setText(i);
        btnNeutral.setVisibility(View.VISIBLE);
        return adb; //.setNeutralButton(s, onClickListener);
    }

    public AlertDialog create() {
        dialog = adb.create();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        return dialog;
    }

    protected Button getButton(/*AlertDialog alertDialog, */int iButton) {
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
}
