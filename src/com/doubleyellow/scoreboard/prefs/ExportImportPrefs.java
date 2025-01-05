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
import android.util.Log;
import android.widget.Toast;
import com.doubleyellow.android.util.ExportImport;
import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.dialog.MyDialogBuilder;
import com.doubleyellow.util.Enums;
import com.doubleyellow.util.FileUtil;
import com.doubleyellow.util.MapUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class ExportImportPrefs extends DialogPreference
{
    private static final String TAG = "SB." + ExportImportPrefs.class.getSimpleName();
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

    private static final String sSettingsExtension_BIN  = ".bin";
    private static final String sSettingsExtension_JSON = ".json";

    public static boolean importSettings(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        File fSettingsJSON = getSettingsBinaryFile(context, true, sSettingsExtension_JSON);
        if ( fSettingsJSON != null && fSettingsJSON.exists() ) {
            try {
                JSONObject joSettings = new JSONObject(FileUtil.readFileAsString(fSettingsJSON));
                SharedPreferences.Editor editor = preferences.edit();
                Iterator<String> keys = joSettings.keys();
                while( keys.hasNext() ) {
                    String sKey = keys.next();
                    Object oBUValue = joSettings.get(sKey);
                    if ( oBUValue instanceof String ) {
                        editor.putString(sKey, (String) oBUValue);
                    } else if ( oBUValue instanceof Boolean ) {
                        editor.putBoolean(sKey, (Boolean) oBUValue);
                    } else if ( oBUValue instanceof Integer ) {
                        editor.putInt(sKey, (Integer) oBUValue);
                    } else if ( oBUValue instanceof JSONArray) {
                        JSONArray ar = (JSONArray) oBUValue;
                        Set<String> setValues = new HashSet<>();
                        for (int i = 0; i < ar.length(); i++) {
                            setValues.add(ar.getString(i));
                        }
                        editor.putStringSet(sKey, setValues);
                    } else if ( oBUValue instanceof Double ) {
                        editor.putFloat(sKey, Float.parseFloat(oBUValue.toString()));
                    } else if ( oBUValue instanceof Float ) {
                        editor.putFloat(sKey, (Float) oBUValue);
                    } else if ( oBUValue instanceof Long ) {
                        editor.putLong(sKey, (Long) oBUValue);
                    } else {
                        Log.w(TAG, String.format("Could not import %s=%s of type %s", sKey, oBUValue, oBUValue.getClass().getName()));
                    }
                }
                editor.commit();
                String sMsg = context.getString(R.string.x_restored_from_y, context.getString(R.string.sb_preferences), fSettingsJSON.getAbsolutePath());
                Toast.makeText(context, sMsg, Toast.LENGTH_LONG).show();
                return true;
            } catch (Exception e) {
            }
        }
        // legacy : restore from old .bin file if it exists

        File fSettings = getSettingsBinaryFile(context, true, sSettingsExtension_BIN);
        if ( (fSettings == null) || (fSettings.exists() == false) ) {
            String sMsg  =        context.getString(R.string.could_not_find_file_x                 , (fSettings==null?"":fSettings.getAbsolutePath()) );
                   sMsg += "\n" + context.getString(R.string.did_you_already_perform_an_export_of_x, context.getString(R.string.sb_preferences));
            Toast.makeText(context, sMsg, Toast.LENGTH_LONG).show();
            return false;
        }

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
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        Map<String, ?> buAll = preferences.getAll();

        File fSettingsJson = getSettingsBinaryFile(context, false, sSettingsExtension_JSON);
        try {
            if ( fSettingsJson == null ) {
                String sMsg = String.format("Could not store settings in %s", "No writable location");
                Toast.makeText(context, sMsg, Toast.LENGTH_LONG).show();
                return;
            }
            TreeMap tmSettings = new TreeMap(buAll); // get stuff 'sorted'
            tmSettings = MapUtil.filterKeys(tmSettings, ".*\\.RunCount", Enums.Match.Remove);
            // some specials
            tmSettings = MapUtil.filterKeys(tmSettings, ".*(BLE|BluetoothLE).*", Enums.Match.Remove);
            if ( Brand.isGameSetMatch() ) {
                for(PreferenceKeys key: Preferences.NONGameSetMatch_SpecificPrefs) {
                    tmSettings.remove(key.toString());
                }
            } else {
                for(PreferenceKeys key: Preferences.GameSetMatch_SpecificPrefs) {
                    tmSettings.remove(key.toString());
                }
            }
            if ( Brand.supportsTimeout() == false ) {
                tmSettings.remove(PreferenceKeys.autoShowGamePausedDialogAfterXPoints.toString());
                tmSettings.remove(PreferenceKeys.autoShowModeActivationDialog.toString());
                tmSettings.remove(PreferenceKeys.showModeDialogAfterXMins.toString());
                tmSettings.remove(PreferenceKeys.showGamePausedDialog.toString());
                tmSettings.remove(PreferenceKeys.timerTowelingDown.toString());
            }
            tmSettings.remove(PreferenceKeys.OfficialSquashRulesURLs.toString());
            tmSettings.remove(PreferenceKeys.squoreBrand.toString());
            tmSettings.remove(PreferenceKeys.viewedChangelogVersion.toString());

            JSONObject joSettings = new JSONObject(tmSettings);
            FileUtil.writeTo(fSettingsJson, joSettings.toString(2));

            String sMsg = context.getString(R.string.x_stored_in_y, context.getString(R.string.sb_preferences), fSettingsJson.getAbsolutePath());
            MyDialogBuilder.dialogWithOkOnly(context, sMsg);
            Toast.makeText(context, sMsg, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            String sMsg = String.format("Could not store settings in %s", fSettingsJson.getAbsolutePath());
            MyDialogBuilder.dialogWithOkOnly(context, sMsg + "\nMaybe try and change the export location. Settings > Archive > Directory for import/export");
            Toast.makeText(context, sMsg, Toast.LENGTH_LONG).show();
        }
        // TODO: remove the settings that have not changed anyway (so that unchanged properties are not transferred from one device to another with e.g. different resolution)
    }

    private static File getSettingsBinaryFile(Context context, boolean bForImport, String sExtension) {
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
        return new File(externalStorageDirectory, Brand.getShortName(context) + ".settings" + sExtension);
    }

}