/*
 * Copyright (C) 2023  Iddo Hoeve
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

import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TimePicker;

import com.doubleyellow.android.util.ColorUtil;
import com.doubleyellow.scoreboard.PersistHelper;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.archive.PreviousMatchSelector;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.prefs.ColorPrefs;
import com.doubleyellow.util.MenuHandler;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Map;

/**
 * To change the date/time of a match from 'stored matches'
 */
public class EditMatchDate extends BaseAlertDialog
{
    public EditMatchDate(Context context, Model matchModel, ScoreBoard scoreBoard) {
        super(context, matchModel, scoreBoard);
    }
    private TimePicker timePicker = null;
    private DatePicker datePicker = null;

    @Override public boolean storeState(Bundle outState) {
        return true;
    }

    @Override public boolean init(Bundle outState) {
        return true;
    }

    @Override public void show() {
        Map<ColorPrefs.ColorTarget, Integer> mColors = ColorPrefs.getTarget2colorMapping(context);
        int iMainBgColor   = mColors.get(ColorPrefs.ColorTarget.playerButtonBackgroundColor);

        View view;
        Date matchDate = matchModel.getMatchDate();
        if ( true ) {
            LayoutInflater myLayout = LayoutInflater.from(context);
            view = myLayout.inflate(R.layout.edit_match_date, null);
            datePicker = view.findViewById(R.id.datePicker);
            timePicker = view.findViewById(R.id.timePicker);
            timePicker.setIs24HourView(true);
            timePicker.setHour(matchDate.getHours());
            timePicker.setMinute(matchDate.getMinutes());
        } else {
            ScrollView sv = new ScrollView(context);
            final LinearLayout ll = new LinearLayout(context);
            ll.setOrientation(LinearLayout.VERTICAL);
            ColorUtil.setBackground(ll, iMainBgColor);

            datePicker = new DatePicker(context);
            ll.addView(datePicker);
            timePicker = new TimePicker(context);
            timePicker.setHour(matchDate.getHours());
            timePicker.setMinute(matchDate.getMinutes());
            ll.addView(timePicker);
            sv.addView(ll);
            view = sv;
        }
        datePicker.init(matchDate.getYear() + 1900, matchDate.getMonth(), matchDate.getDate(), null);

        DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> handleButtonClick(which);
        String s1 = getString(R.string.date);
        if ( isNotWearable() ) {
            adb.setTitle(s1);
            adb.setIcon(android.R.drawable.ic_menu_edit);
        }
            adb.setView(view)
               .setPositiveButton(R.string.cmd_ok    , dialogClickListener)
               .setNegativeButton(R.string.cmd_cancel, dialogClickListener)
                //.setCancelable(false)
        ;
        dialog = adb.show();
    }

    public void handleButtonClick(int which) {
        switch (which){
            case DialogInterface.BUTTON_POSITIVE:
                if ( scoreBoard != null ) {
                    // invoked from main scoreboard
                } else {
                    // from previous matches
                    File fWasStoredAs = matchModel.getStoreAs(PreviousMatchSelector.getArchiveDir(context));

                    Date matchDate = matchModel.getMatchDate();
                    int iYear       = datePicker.getYear() - 1900;
                    int iMonth      = datePicker.getMonth();
                    int iDayOfMonth = datePicker.getDayOfMonth();
                    if ( timePicker != null ) {
                        matchDate = new Date(iYear, iMonth, iDayOfMonth, timePicker.getHour(), timePicker.getMinute());
                    } else {
                        matchDate = new Date(iYear, iMonth, iDayOfMonth);
                    }
                    boolean bDateChanged   = matchModel.setMatchDate(matchDate);
                    try {
                        if ( bDateChanged ) {
                            // if date has changed the filename will have changed... so need to remove the old .sb file
                            fWasStoredAs.delete();
                        }
                        PersistHelper.storeAsPrevious(context, matchModel, true);
                        if ( context instanceof MenuHandler) {
                            MenuHandler menuHandler = (MenuHandler) context;
                            menuHandler.handleMenuItem(R.id.refresh);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                // Do nothing.
                break;
        }
    }
}
