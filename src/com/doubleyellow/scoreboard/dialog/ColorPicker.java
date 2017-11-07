package com.doubleyellow.scoreboard.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.doubleyellow.android.util.ColorUtil;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.model.Player;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.android.view.ColorPickerView;

/**
 * Dialog that is displayed when user 'color' is clicked.
 */
public class ColorPicker extends BaseAlertDialog
{
    public ColorPicker(Context context, Model matchModel, ScoreBoard scoreBoard) {
        super(context, matchModel, scoreBoard);
    }

    @Override public boolean storeState(Bundle outState) {
        outState.putSerializable(Player.class.getSimpleName(), targetPlayer);
        return true;
    }

    @Override public boolean init(Bundle outState) {
        init((Player) outState.getSerializable(Player.class.getSimpleName()), (String) outState.getSerializable(Color.class.getSimpleName()));
        return true;
    }

    private Player targetPlayer = null;
    private String sColor       = null;
    @Override public void show()
    {
        String sTitle = getString(R.string.lbl_color) + ": " + matchModel.getName_no_nbsp(targetPlayer, false);
        View tl = getColorPickerView();
        adb
                .setTitle(sTitle)
              //.setIcon(android.R.drawable.ic_btn_speak_now) // TODO: a t-shirt maybe?
              //.setMessage(R.string.oa_decision)
                .setPositiveButton(R.string.cmd_ok, listener)
                .setNeutralButton(R.string.cmd_none, listener)
                .setNegativeButton(R.string.cmd_cancel, listener)
                .setView(tl);

        ButtonUpdater listener;
        if ( sColor != null ) {
            int iColor = Color.parseColor(this.sColor);
            listener = new ButtonUpdater(context, AlertDialog.BUTTON_NEGATIVE, iColor, AlertDialog.BUTTON_POSITIVE, iColor);
        } else {
            listener = new ButtonUpdater(context); // still a listener to ensure all button colors are reset if color is set to one and not for another player
        }
        dialog = adb.show(listener);
    }

    public void init(Player targetPlayer, String sCurrentColor) {
        this.targetPlayer = targetPlayer;
        this.sColor       = sCurrentColor;
    }

    private View getColorPickerView() {
        ColorPickerView view = new ColorPickerView(context);

        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        view.setLayoutParams(layoutParams);
        view.setOnColorChangedListener(new ColorPickerView.OnColorChangedListener() {
            @Override public void onColorChanged(int newColor) {
                String rgbString = ColorUtil.getRGBString(newColor);
                setColor(rgbString);
            }
        });
        //view.setAlphaSliderVisible(false); // no transparency
        if ( this.sColor != null ) {
            view.setColor(Color.parseColor(this.sColor));
        }
        return view;
    }

    private void setColor(String sRGB) {
        this.sColor = sRGB;

        final Button btnOK = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        //btnOK.setBackgroundColor(Color.parseColor(sColor));  // works but changes the dimensions of the button
        //btnOK.getBackground().setColorFilter(new LightingColorFilter(0xFF000000, Color.parseColor(sColor)));
        ColorUtil.setBackground(btnOK, Color.parseColor(sColor));
        btnOK.setTextColor(ColorUtil.getBlackOrWhiteFor(sColor));
    }

    private DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
        @Override public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case DialogInterface.BUTTON_NEGATIVE:
                    break;
                case DialogInterface.BUTTON_NEUTRAL:
                    matchModel.setPlayerColor(targetPlayer, null);
                    break;
                case DialogInterface.BUTTON_POSITIVE:
                    matchModel.setPlayerColor(targetPlayer, sColor);
                    break;
            }
        }
    };

/*
    private View getColorPickerView_IH() {
        List<String> lColors = new ArrayList<String>();
        final int iStepSize = 0x33;
        for(int r=0; r<=0xff; r+=iStepSize) {
            for(int g=0; g<=0xff; g+=iStepSize) {
                for(int b=0; b<=0xff; b+=iStepSize) {
                    lColors.add(getRGBString(r, g, b));
                }
            }
        }
        if ( true ) {
            // sort the colors by distance to black
            Collections.sort(lColors, new Comparator<String>() {
                @Override
                public int compare(String s, String t1) {
                    return (int) (ColorPrefs.getDistance2Black(s) - ColorPrefs.getDistance2Black(t1));
                }
            });
        }
        Point point = ViewUtil.getDisplayPoint(context);
        int iRows = (int) Math.ceil(Math.sqrt(lColors.size()));
        int iMinWH = Math.min(point.x, point.y) / (iRows + 1);

        TableLayout tl = new TableLayout(context);
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tl.setLayoutParams(layoutParams);
        for(int r=0; r < iRows; r++) {
            TableRow tr = new TableRow(context);
            tl.addView(tr);
            for(int c=0; c < iRows; c++) {
                int iColorNr = r * iRows + c;
                if ( iColorNr >= lColors.size() ) { break; }
                View b = new TextView(context);
                String sColor = lColors.get(iColorNr);
                b.setBackgroundColor(Color.parseColor(sColor));
                if ( false && (b instanceof TextView) ) {
                    TextView textView = (TextView) b;
                    textView.setText(String.valueOf(iColorNr + 1));
                }
                b.setMinimumHeight(iMinWH); // todo: base on screen widht/height?? and iRows
                b.setMinimumWidth(iMinWH);
                b.setTag(sColor);
                tr.addView(b);
                b.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        String sColor = view.getTag().toString();
                        setColor(sColor);
                    }
                });
            }
        }
        ScrollView svColors = new ScrollView(context);
        svColors.addView(tl);
        return svColors;
    }
*/

}
