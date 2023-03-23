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
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.doubleyellow.android.util.ExportImport;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.archive.PreviousMatchSelector;
import com.doubleyellow.scoreboard.main.*;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.util.ListUtil;
import com.doubleyellow.android.view.SelectFileView;
import com.doubleyellow.util.MenuHandler;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Import extends BaseAlertDialog
{
    public Import(Context context, Model matchModel, ScoreBoard scoreBoard) {
        super(context, matchModel, scoreBoard);
    }

    @Override public boolean storeState(Bundle outState) {
        return true;
    }

    @Override public boolean init(Bundle outState) {
        return true;
    }

    private File sourceDirectory = null;
    private File targetDirectory = null;
    private SelectFileView sfv;

    @Override public void show() {
        sourceDirectory = PreferenceValues.targetDirForImportExport(context, true);
        targetDirectory = PreviousMatchSelector.getArchiveDir(context);

        ExportImport.MountState mountStateExternalStorage = ExportImport.getMountStateExternalStorage();
        if ( sourceDirectory == null || mountStateExternalStorage.equals(ExportImport.MountState.UnMounted) ) {
            Toast.makeText(context, R.string.could_not_determine_external_storage_location, Toast.LENGTH_LONG).show();
            return;
        }

        adb     .setTitle  (        getString(R.string.Import) + " > " + getString(R.string.Select_file) )
                .setIcon   (android.R.drawable.ic_input_get)
                //.setMessage(sMsg)
                .setPositiveButton(android.R.string.ok    , listener)
                .setNeutralButton (android.R.string.cancel, listener)
                .setNegativeButton(R.string.cmd_delete , listener);

        // add a view with all possible zip files and let user choose one
        String sFilesInZipMatchingRegExp = ".+\\.(sb|json)";
        List<File> lFiles = ExportImport.listZipFiles(sourceDirectory, sFilesInZipMatchingRegExp);
        if ( ListUtil.isEmpty(lFiles) ) {
            Collection<String> sdCardPaths = ExportImport.getSDCardPaths(false);
            if ( ListUtil.isNotEmpty(sdCardPaths) ) {
                sdCardPaths.remove(sourceDirectory);
            }
            if ( ListUtil.isNotEmpty(sdCardPaths) ) {
                File fBuSourceDirectory = new File(sdCardPaths.iterator().next());
                lFiles = ExportImport.listZipFiles(fBuSourceDirectory, sFilesInZipMatchingRegExp);
                if ( ListUtil.isNotEmpty(lFiles) ) {
                    sourceDirectory = fBuSourceDirectory;
                }
            }
        }
        if ( ListUtil.isEmpty(lFiles) ) {
            String sMsg = getString(R.string.no_appropriate_zip_files_found_in_x, sourceDirectory.getPath());
            if ( sourceDirectory.canRead() == false ) {
                sMsg = getString(R.string.no_read_rights_for_x, sourceDirectory.getPath());
            }
            Toast.makeText(context, sMsg, Toast.LENGTH_LONG).show();
            return;
        }
        // present last exported files first
        Collections.sort(lFiles, new Comparator<File>() {
            @Override
            public int compare(File file, File t1) {
                return (file.lastModified() > t1.lastModified()) ? -1 : 1;
            }
        });

        LinearLayout ll = new LinearLayout(context);
        ll.setOrientation(LinearLayout.VERTICAL);
        sfv = new SelectFileView(context, lFiles, lFiles.get(0));
        ll.addView(sfv);

        boolean bAddSourceNTargetDirs = true; // TODO: use resource default or even configurable
        if ( bAddSourceNTargetDirs ) {
            try {
                ll.addView(getTextView(R.string.Source_x, sourceDirectory.getCanonicalPath()), 0);
                ll.addView(getTextView(R.string.Target_y, targetDirectory.getCanonicalPath()));
            } catch (Exception e) {
            }
        }
        adb.setView(ll);

        dialog = adb.show();
    }

    private DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
        @Override public void onClick(DialogInterface dialog, int which) {
            handleButtonClick(which);
        }
    };

    public static final int BNT_IMPORT = DialogInterface.BUTTON_POSITIVE;
    public static final int BNT_DELETE = DialogInterface.BUTTON_NEGATIVE;
    @Override public void handleButtonClick(int which) {
        File fChecked = sfv.getChecked();

        switch (which) {
            case BNT_IMPORT:
                String sFilename = fChecked.getName();
                File file = new File(sourceDirectory, sFilename);
                int iCnt = ExportImport.importData(targetDirectory, file);
                if ( iCnt > 0 ) {
                    Toast.makeText(context, getString(R.string.File_x_has_been_imported__Cnt_y, sFilename, iCnt), Toast.LENGTH_LONG).show();
                    if ( context instanceof MenuHandler) {
                        MenuHandler menuHandler = (MenuHandler) context;
                        menuHandler.handleMenuItem(R.id.refresh);
                    }
                }
                break;
            case DialogInterface.BUTTON_NEUTRAL:
                break;
            case BNT_DELETE:
                // Delete the selected file
                if ( fChecked.delete() == false ) {
                    Toast.makeText(context, "Delete failed... Only read rights?", Toast.LENGTH_SHORT).show();
                }
                if (context instanceof MenuHandler) {
                    MenuHandler menuHandler = (MenuHandler) context;
                    boolean bHandled = menuHandler.handleMenuItem(R.id.cmd_import) || menuHandler.handleMenuItem(R.id.cmd_import_matches); // restart this dialog
                }
                break;
        }
    }
}
