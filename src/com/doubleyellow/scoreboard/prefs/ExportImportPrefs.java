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

package com.doubleyellow.scoreboard.prefs;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.widget.Toast;
import com.doubleyellow.android.util.ExportImport;
import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.dialog.MyDialogBuilder;
import com.doubleyellow.util.FileUtil;
import com.doubleyellow.util.MapUtil;

import java.io.File;
import java.util.Map;
import java.util.Set;

public class ExportImportPrefs extends DialogPreference
{
    private Context context;

    public ExportImportPrefs(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        this.context = context;
    }

    @Override public void onClick(DialogInterface dialog, int iPressed)
    {
        super.onClick(dialog, iPressed);

        switch (iPressed) {
            case DialogInterface.BUTTON_POSITIVE:
                //import
                importSettings(context);

                Preferences.setModelDirty();

                PreferenceValues.setRestartRequired(context);
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                // export
                exportSettings(context);
                break;
        }
    }

    public static boolean importSettings(Context context) {
        File fSettings = getSettingsBinaryFile(context, true);
        if ( (fSettings == null) || (fSettings.exists() == false) ) {
            String sMsg  =        context.getString(R.string.could_not_find_file_x                 , (fSettings==null?"":fSettings.getAbsolutePath()) );
                   sMsg += "\n" + context.getString(R.string.did_you_already_perform_an_export_of_x, context.getString(R.string.sb_preferences));
            Toast.makeText(context, sMsg, Toast.LENGTH_LONG).show();
            return false;
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        Map<String, ?> buAll = (Map<String, ?>) FileUtil.readObjectFromFile(fSettings);
        if ( MapUtil.isEmpty(buAll) ) {
            return false;
        }

        SharedPreferences.Editor editor = preferences.edit();
        for( String sKey: buAll.keySet() ) {
            Object oBUValue = buAll.get(sKey);
            if ( oBUValue instanceof String ) {
                editor.putString(sKey, (String) oBUValue);
            }
            if ( oBUValue instanceof Boolean ) {
                editor.putBoolean(sKey, (Boolean) oBUValue);
            }
            if ( oBUValue instanceof Float ) {
                editor.putFloat(sKey, (Float) oBUValue);
            }
            if ( oBUValue instanceof Integer ) {
                editor.putInt(sKey, (Integer) oBUValue);
            }
            if ( oBUValue instanceof Long ) {
                editor.putLong(sKey, (Long) oBUValue);
            }
            if ( oBUValue instanceof Set ) {
                editor.putStringSet(sKey, (Set<String>) oBUValue);
            }
        }
        editor.commit();

        String sMsg = context.getString(R.string.x_restored_from_y, context.getString(R.string.sb_preferences), fSettings.getAbsolutePath());
        Toast.makeText(context, sMsg, Toast.LENGTH_LONG).show();

        return true;
    }

    public static void exportSettings(Context context) {
        File fSettings = getSettingsBinaryFile(context, false);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        Map<String, ?> buAll = preferences.getAll();

        if ( fSettings == null ) {
            String sMsg = String.format("Could not store settings in %s", "No writable location");
            Toast.makeText(context, sMsg, Toast.LENGTH_LONG).show();
            return;
        }
        // TODO: remove the settings that have not changed anyway (so that unchanged properties are not transferred from one device to another with e.g. different resolution)

        boolean bWritten = FileUtil.writeObjectToFile(fSettings, buAll);
        if ( bWritten ) {
            String sMsg = context.getString(R.string.x_stored_in_y, context.getString(R.string.sb_preferences), fSettings.getAbsolutePath());
            MyDialogBuilder.dialogWithOkOnly(context, sMsg);
            Toast.makeText(context, sMsg, Toast.LENGTH_LONG).show();
        } else {
            String sMsg = String.format("Could not store settings in %s", fSettings.getAbsolutePath());
            MyDialogBuilder.dialogWithOkOnly(context, sMsg + "\nMaybe try and change the export location. Settings > Archive > Directory for import/export");
            Toast.makeText(context, sMsg, Toast.LENGTH_LONG).show();
        }
    }

    public static File getSettingsBinaryFile(Context context, boolean bForImport) {
        File externalStorageDirectory = PreferenceValues.targetDirForImportExport(context, bForImport); // typically '/storage/emulated/0'
        if ( externalStorageDirectory.canWrite() == false ) {
            Toast.makeText(context, context.getString(R.string.no_write_rights_for_x, externalStorageDirectory.getPath()), Toast.LENGTH_LONG).show();
            return null;
        }
        ExportImport.MountState mountStateExternalStorage = ExportImport.getMountStateExternalStorage();
        if ( externalStorageDirectory == null || mountStateExternalStorage.equals(ExportImport.MountState.UnMounted) ) {
            Toast.makeText(context, R.string.could_not_determine_external_storage_location, Toast.LENGTH_LONG).show();
            return null;
        }
        return new File(externalStorageDirectory, Brand.getShortName(context) + ".settings.bin");
    }

}