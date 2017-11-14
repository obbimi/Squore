package com.doubleyellow.scoreboard.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.archive.GroupMatchesBy;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.prefs.PreferenceKeys;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.util.MenuHandler;
import com.doubleyellow.util.SortOrder;
import com.doubleyellow.android.view.SelectEnumView;

public class SortOptions extends BaseAlertDialog
{
    public SortOptions(Context context, Model matchModel, ScoreBoard scoreBoard) {
        super(context, matchModel, scoreBoard);
    }

    @Override public boolean storeState(Bundle outState) {
        return true;
    }

    @Override public boolean init(Bundle outState) {
        return true;
    }

    private SelectEnumView<GroupMatchesBy> gmb;
    private SelectEnumView<SortOrder>      soam;

    private ViewGroup ll = null;
    @Override public void show() {

        LayoutInflater myLayout = LayoutInflater.from(context);
        ll = (ViewGroup) myLayout.inflate(R.layout.archived_sort_options, null);

        gmb  = (SelectEnumView<GroupMatchesBy>) ll.findViewById(R.id.evGroupMatchesBy);
        soam = (SelectEnumView<SortOrder>     ) ll.findViewById(R.id.evSortOrder);
        gmb .check(PreferenceValues.groupArchivedMatchesBy    (context));
        soam.check(PreferenceValues.sortOrderOfArchivedMatches(context));

        dialog = adb
                .setIcon(android.R.drawable.ic_menu_sort_by_size)
                .setTitle         (R.string.cmd_sort)
                .setPositiveButton(R.string.cmd_ok    , listener)
                .setNeutralButton (R.string.cmd_cancel, listener)
                .setView(ll)
                .show();
    }

    private DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
        @Override public void onClick(DialogInterface dialog, int which) {
            handleButtonClick(which);
        }
    };

    @Override public void handleButtonClick(int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                PreferenceValues.setEnum(PreferenceKeys.groupArchivedMatchesBy    , context, gmb .getChecked());
                PreferenceValues.setEnum(PreferenceKeys.sortOrderOfArchivedMatches, context, soam.getChecked());
                if ( context instanceof MenuHandler) {
                    MenuHandler menuHandler = (MenuHandler) context;
                    menuHandler.handleMenuItem(R.id.refresh);
                }
                break;
            case DialogInterface.BUTTON_NEUTRAL:
                break;
        }
    }
}
