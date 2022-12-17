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
import com.doubleyellow.android.view.SatValHueColorPicker;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.model.Player;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.android.view.ColorPickerView;
import com.doubleyellow.android.view.LineColorPicker;

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
        View tl = getColorPickerView();
        if ( isNotWearable() ) {
            String sTitle = getString(R.string.lbl_color) + ": " + matchModel.getName_no_nbsp(targetPlayer, false);
            adb.setTitle(sTitle);
            //.setIcon(R.drawable.microphone) // TODO: a t-shirt maybe?
        }
        adb.setPositiveButton(R.string.cmd_ok    , listener)
           .setNeutralButton (R.string.cmd_none  , listener)
           .setNegativeButton(R.string.cmd_cancel, listener)
           .setView(tl);

        final ButtonUpdater listener;
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
        View view = new SatValHueColorPicker(context);
        if ( false ) {
            view = new LineColorPicker(context, null);
        }

        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        view.setLayoutParams(layoutParams);

        ColorPickerView cpv = (ColorPickerView) view;
        if ( this.sColor != null ) {
            int iColor = 0;
            try {
                iColor = Color.parseColor(this.sColor);
            } catch (IllegalArgumentException e) {
                // unknown color
                this.sColor = "#000000";
                iColor = Color.BLACK;
            }
            cpv.setColor(iColor);
        }
        cpv.setOnColorChangedListener(colorChangedListener);

        return view;
    }

    private ColorPickerView.OnColorChangedListener colorChangedListener = new ColorPickerView.OnColorChangedListener() {
        @Override public void onColorChanged(int newColor) {
            String rgbString = ColorUtil.getRGBString(newColor);
            setColor(rgbString);
        }
    };

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
                    scoreBoard.setPlayerColor(targetPlayer, null);
                    break;
                case DialogInterface.BUTTON_POSITIVE:
                    scoreBoard.setPlayerColor(targetPlayer, sColor);
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
