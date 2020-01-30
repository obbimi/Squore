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

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.SparseArray;
import android.widget.Toast;

import com.doubleyellow.android.view.ViewUtil;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.activity.IntentKeys;
import com.doubleyellow.scoreboard.activity.ScoreSheetOnline;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.model.Model;

/**
 * Presented to the user after an online scoresheet of the match has been created.
 * User has a choice what to do with the link.
 */
public class OnlineSheetAvailableChoice extends BaseAlertDialog
{
    private enum OnlineSheetAction {
        ShareLink,
        Preview,
        ViewInBrowser,
        CopyToClipboard,
        DeleteFromServer,
    }

    public OnlineSheetAvailableChoice(Context context, Model matchModel, ScoreBoard scoreBoard) {
        super(context, matchModel, scoreBoard);
    }

    @Override public boolean storeState(Bundle outState) {
        outState.putString(OnlineSheetAvailableChoice.class.getSimpleName(), sShowURL);
        return true;
    }

    @Override public boolean init(Bundle outState) {
        init(outState.getString(OnlineSheetAvailableChoice.class.getSimpleName()));
        return true;
    }

    private String sShowURL = null;

    public void init(String sShareURL) {
        this.sShowURL = sShareURL;
    }

    @Override public void show() {
        adb.setTitle(context.getString(R.string.sb_online_sheet_available))
           .setOnKeyListener(getOnBackKeyListener())
           .setIcon(android.R.drawable.ic_menu_share);

        mTranslateButtonToType.put(DialogInterface.BUTTON_POSITIVE, OnlineSheetAction.Preview);
        mTranslateButtonToType.put(DialogInterface.BUTTON_NEUTRAL , OnlineSheetAction.ShareLink);
        mTranslateButtonToType.put(DialogInterface.BUTTON_NEGATIVE, OnlineSheetAction.ViewInBrowser);

        for(int i = 0; i < mTranslateButtonToType.size(); i++) {
            int iButton = mTranslateButtonToType.keyAt(i);
            OnlineSheetAction type = mTranslateButtonToType.get(iButton);
            String text = ViewUtil.getEnumDisplayValue(context, R.array.OnlineSheetActionDisplayValues, type);
            switch (iButton) {
                case DialogInterface.BUTTON_POSITIVE: adb.setPositiveButton(text, chooseOnlineSheetAction); break;
                case DialogInterface.BUTTON_NEUTRAL : adb.setNeutralButton (text, chooseOnlineSheetAction); break;
                case DialogInterface.BUTTON_NEGATIVE: adb.setNegativeButton(text, chooseOnlineSheetAction); break;
            }
        }
        dialog = adb.show();
    }

    private final SparseArray<OnlineSheetAction> mTranslateButtonToType = new SparseArray<OnlineSheetAction>();

    private DialogInterface.OnClickListener chooseOnlineSheetAction = new DialogInterface.OnClickListener() {
        @Override public void onClick(DialogInterface dialogInterface, int which) {
            handleButtonClick(which);
        }
    };

    @Override public void handleButtonClick(int which) {
        OnlineSheetAction type = mTranslateButtonToType.get(which);
        if ( type != null ) {
            switch (type) {
                case ShareLink:
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("text/plain");
                    intent.putExtra(Intent.EXTRA_TEXT, sShowURL);
                    context.startActivity(Intent.createChooser(intent, context.getString(R.string.cmd_share_with_friends)));
                    break;
                case Preview:
                    if ( isWearable(context) ) {
                        Toast.makeText(context, "Sorry not supported on wearable", Toast.LENGTH_LONG).show();
                        return;
                    }
                    Intent nm = new Intent(context, ScoreSheetOnline.class);
                    Bundle bundle = new Bundle();
                    bundle.putString(IntentKeys.ScoreSheetOnline.toString(), sShowURL );
                    nm.putExtra(IntentKeys.ScoreSheetOnline.toString(), bundle);
                    context.startActivity(nm);
                    break;
                case ViewInBrowser:
                    Uri uriUrl = ScoreBoard.buildURL(context, sShowURL, false);
                    Intent launchBrowser = new Intent(Intent.ACTION_VIEW, uriUrl);
                    context.startActivity(launchBrowser); // does not (always?) work on wearable
                    break;
                case CopyToClipboard:
                    // TODO:
                    break;
                case DeleteFromServer:
                    // TODO:
                    break;
            }
        }
        dialog.dismiss();
        showNextDialog();
    }
}
