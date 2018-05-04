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
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.doubleyellow.android.task.HttpUploadFileTask;
import com.doubleyellow.android.task.URLTask;
import com.doubleyellow.android.util.ColorUtil;
import com.doubleyellow.android.util.ContentUtil;
import com.doubleyellow.android.util.ExportImport;
import com.doubleyellow.android.view.ViewUtil;
import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.URLFeedTask;
import com.doubleyellow.scoreboard.archive.PreviousMatchSelector;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.model.Util;
import com.doubleyellow.scoreboard.prefs.ColorPrefs;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.util.DateUtil;

import java.io.*;
import java.util.Map;

public class Export extends BaseAlertDialog implements HttpUploadFileTask.UploadResultHandler
{
    public Export(Context context, Model matchModel, ScoreBoard scoreBoard) {
        super(context, matchModel, scoreBoard);
    }

    @Override public boolean storeState(Bundle outState) {
        return true;
    }

    @Override public boolean init(Bundle outState) {
        return true;
    }

    private File targetDirectory = null;
    private File sourceDirectory = null;

    @Override public void show() {
        sourceDirectory = PreviousMatchSelector.getArchiveDir(context);
        targetDirectory = PreferenceValues.targetDirForImportExport(context, false);
        ExportImport.MountState mountStateExternalStorage = ExportImport.getMountStateExternalStorage();
        if ( targetDirectory == null || mountStateExternalStorage.equals(ExportImport.MountState.UnMounted) ) {
            Toast.makeText(context, R.string.could_not_determine_external_storage_location, Toast.LENGTH_LONG).show();
            return;
        }
        if ( targetDirectory.canWrite() == false ) {
            Toast.makeText(context, getString(R.string.no_write_rights_for_x, targetDirectory.getPath()), Toast.LENGTH_LONG).show();
            return;
        }

        Map<ColorPrefs.ColorTarget, Integer> mColors = ColorPrefs.getTarget2colorMapping(context);
        int iMainBgColor   = mColors.get(ColorPrefs.ColorTarget.playerButtonBackgroundColor);
        int iLabelTxt      = mColors.get(ColorPrefs.ColorTarget.playerButtonTextColor);
        int iInputBgColor  = mColors.get(ColorPrefs.ColorTarget.scoreButtonBackgroundColor);
        int iInputTxtColor = mColors.get(ColorPrefs.ColorTarget.scoreButtonTextColor);

        final LinearLayout ll = new LinearLayout(context);
        ll.setOrientation(LinearLayout.VERTICAL);
        ColorUtil.setBackground(ll, iMainBgColor);

        final LinearLayout llLabelAndText = new LinearLayout(context);
        llLabelAndText.setOrientation(LinearLayout.HORIZONTAL);

        TextView label = new TextView(context);
        label.setText(R.string.filename);
        label.setTextColor(iLabelTxt);
        llLabelAndText.addView(label);

        String sSuggest = Brand.getShortName(context).replaceAll(" ", "") + "." + DateUtil.getCurrentYYYYMMDD();
        String sSuffix = "";
        int iSuffix = 0;
        do {
            File fTst = new File(targetDirectory, sSuggest + sSuffix + ".zip");
            if ( fTst.exists() == false ) break;
            sSuffix = "." + String.valueOf(++iSuffix);
        } while(true);

        try {
            TextView tvPath = getTextView(R.string.Source_x__Target_y, sourceDirectory.getCanonicalPath(), targetDirectory.getCanonicalPath());
            ll.addView(tvPath);
        } catch (Exception e) {
            e.printStackTrace();
        }

        txtFileName = new EditText(context);
        txtFileName.setSingleLine();
        txtFileName.setText(sSuggest + sSuffix);
        //txtFileName.setInputType(InputType.TYPE_NULL); // try to prevent auto adding of spaces during typing
        txtFileName.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        llLabelAndText.addView(txtFileName);

        ll.addView(llLabelAndText);

        doUpload = new CheckBox(context);
        doUpload.setText(R.string.upload_export_to_server);
        doUpload.setEnabled(ContentUtil.isNetworkAvailable(context));
        ll.addView(doUpload);

        ColorUtil.setBackground(txtFileName, iInputBgColor);
        txtFileName.setTextColor(iInputTxtColor);

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialog, int which) {
                handleButtonClick(which);
            }
        };
        adb.setTitle(R.string.Export)
           .setView(ll)
           .setIcon(android.R.drawable.ic_menu_upload)
           .setPositiveButton(android.R.string.ok    , dialogClickListener)
           .setNegativeButton(android.R.string.cancel, dialogClickListener);
        dialog = adb.show(installTheExportClickListener);

        // try showing the keyboard by default (seems not to work in landscape due to lack of space on screen?)
        ViewUtil.showKeyboard(dialog);
    }

    private EditText txtFileName;
    private CheckBox doUpload;

    public static final int BTN_EXPORT = DialogInterface.BUTTON_POSITIVE;
    public static final int BTN_CANCEL = DialogInterface.BUTTON_NEGATIVE;
    @Override public void handleButtonClick(int which) {
        switch (which){
            case BTN_EXPORT:
                doExport();
                break;
            case BTN_CANCEL:
                // Do nothing.
                break;
        }
    }

    private void doExport() {
        String sFilename = txtFileName.getText().toString() + ".zip";
        File file = new File(targetDirectory, sFilename);

        // ensure LAST.sb is excluded from the export
        File fLast = ScoreBoard.getLastMatchFile(context);
        File fLastTmp = new File(fLast.getPath() + ".tmp");
        fLast.renameTo(fLastTmp);

        String sPath = ExportImport.exportData(sourceDirectory, file, ".*\\.sb");
        if ( sPath != null ) {
            Toast.makeText(context, getString(R.string.Exported_to_x, sPath), Toast.LENGTH_LONG).show();
        }

        // rename LAST.sb back to original
        fLastTmp.renameTo(fLast);

        if ( (doUpload != null) && doUpload.isChecked() ) {
            btnExport.setEnabled(false);

            // store file on the server
            String sUrl = URLFeedTask.prefixWithBaseIfRequired("upload.file.php");
            if ( Util.isMyDevice(context) ) {
                //sUrl = "http://192.168.0.114/upload.file.php";
            }
            HttpUploadFileTask task = new HttpUploadFileTask(sUrl, "uploaded_file", URLTask.getMyUserAgentString(context));
            task.setUploadResultHandler(this);
            task.execute(file);
        } else {
            dismiss();
        }
    }

    private Button btnExport;
    private DialogInterface.OnShowListener installTheExportClickListener = new DialogInterface.OnShowListener() {
        //We register an onClickListener for the 'export' button. There is already one, but that one closes the dialog no matter what.
        @Override public void onShow(DialogInterface dialogInterface) {
            btnExport = dialog.getButton(BTN_EXPORT);
            if (btnExport == null) return;
            // ensure that when toss button is clicked player buttons are toggled a couple of times then only one remains enabled
            btnExport.setOnClickListener(onExportClickListener);
        }
    };

    private View.OnClickListener onExportClickListener = new View.OnClickListener() {
        @Override public void onClick(View view) {
            doExport();
        }
    };

    @Override public void handle(final String sContent, boolean bSuccess) {
        String sMsg = null;
        if ( bSuccess ) {
            // place url on clipboard for easy communication
            ContentUtil.placeOnClipboard(context, "backup url", sContent);
            sMsg = context.getString(R.string.uploaded_to_server__x, sContent);
        } else {
            sMsg = context.getString(R.string.upload_to_server_failed__x, sContent);
        }
        final AlertDialog dialog = ScoreBoard.dialogWithOkOnly(context, sMsg);
        if ( bSuccess ) {
            Button button = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            button.setText(R.string.cmd_share);
            button.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("text/plain");
                    intent.putExtra(Intent.EXTRA_TEXT, sContent);
                    context.startActivity(Intent.createChooser(intent, context.getString(R.string.cmd_share_with_friends)));
                    dialog.dismiss();
                }
            });
        }
        dismiss();
    }
}
