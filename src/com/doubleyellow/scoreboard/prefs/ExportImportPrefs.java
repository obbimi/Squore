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

import com.doubleyellow.android.util.ContentReceiver;
import com.doubleyellow.android.util.ExportImport;
import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.URLFeedTask;
import com.doubleyellow.scoreboard.dialog.MyDialogBuilder;
import com.doubleyellow.scoreboard.dialog.UsernamePassword;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.util.Enums;
import com.doubleyellow.util.FileUtil;
import com.doubleyellow.util.JsonUtil;
import com.doubleyellow.util.ListUtil;
import com.doubleyellow.util.MapUtil;
import com.doubleyellow.util.Params;
import com.doubleyellow.util.StringUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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
                importSettings(context, true);

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

    public static String importSettings(Context context, boolean bShowToast) {

        File fSettingsJSON = getSettingsBinaryFile(context, true, sSettingsExtension_JSON);
        if ( fSettingsJSON != null && fSettingsJSON.exists() ) {
            try {
                JSONObject joSettings = new JSONObject(FileUtil.readFileAsString(fSettingsJSON));
                importSettingsFromJSON(context, joSettings);
                String sMsg = context.getString(R.string.x_restored_from_y, context.getString(R.string.sb_preferences), fSettingsJSON.getAbsolutePath());
                if ( bShowToast ) {
                    Toast.makeText(context, sMsg, Toast.LENGTH_LONG).show();
                }
                return sMsg;
            } catch (Exception e) {
            }
        }
        // legacy : restore from old .bin file if it exists

        File fSettings = getSettingsBinaryFile(context, true, sSettingsExtension_BIN);
        if ( (fSettings == null) || (fSettings.exists() == false) ) {
            String sMsg  =        context.getString(R.string.could_not_find_file_x                 , (fSettings==null?"":fSettings.getAbsolutePath()) );
                   sMsg += "\n" + context.getString(R.string.did_you_already_perform_an_export_of_x, context.getString(R.string.sb_preferences));
            if ( bShowToast ) {
                Toast.makeText(context, sMsg, Toast.LENGTH_LONG).show();
            }
            return sMsg;
        }

        Map<String, ?> buAll = (Map<String, ?>) FileUtil.readObjectFromFile(fSettings);
        if ( MapUtil.isEmpty(buAll) ) {
            return null;
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
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
        if ( bShowToast ) {
            Toast.makeText(context, sMsg, Toast.LENGTH_LONG).show();
        }

        return sMsg;
    }

    public static int importSettingsFromJSONFromURL(Context context, String sContent, String sUrl) throws JSONException {
        sContent = sContent.trim();
        JSONObject joRemoteConfig = new JSONObject(sContent);

        JSONObject joCurrent = getCurrentSettings(context);
        Map mCurrent = JsonUtil.toMap(joCurrent);
          //mCurrent.remove(PreferenceKeys.RemoteSettingsURL.toString()); // maddock uses some subsequent re-set of the settings URL to have slightly different settings per device
            mCurrent = MapUtil.filterKeys(mCurrent, ".+" + UsernamePassword.getSettingsSuffix(context, R.string.username), Enums.Match.Remove);
            mCurrent = MapUtil.filterKeys(mCurrent, ".+" + UsernamePassword.getSettingsSuffix(context, R.string.password), Enums.Match.Remove);

        Map mRemote  = JsonUtil.toMap(joRemoteConfig);
        if ( PreferenceValues.currentDateIsTestDate() ) {
            mCurrent = new TreeMap(mCurrent);
            mRemote  = new TreeMap(mRemote);
        }
        mRemote = MapUtil.filterKeys(mRemote, "^-.+", Enums.Match.Remove); // allow playing around with settings, marking them as ignore by prefixing with a dash
        int iTrimmed = trimValues(mRemote);
        int iRemoved = cleanBrandBased(mRemote);

        Map<MapUtil.mapDiff, Map> mapDiff = MapUtil.getMapDiff(mCurrent, mRemote);
        Map mInserts = mapDiff.get(MapUtil.mapDiff.insert);
        Map mUpdates = mapDiff.get(MapUtil.mapDiff.update);
/*
        if ( MapUtil.isNotEmpty(mUpdates) ) {
            Map mUpdates2 = getUpdates(mRemote, mCurrent);
        }
*/
        Map mToBeOverwritten = MapUtil.filterKeys(mCurrent, mUpdates.keySet(), MapUtil.Overlay.Keep);
        //Map mDeletes = mapDiff.get(MapUtil.mapDiff.delete);
        //Log.w(TAG, "TODO: settings diff " + mapDiff);
        Map mUpserts = new HashMap(mInserts);
        mUpserts.putAll(mUpdates);

        int iChanges = mUpserts.size();

        if ( mUpdates.size() > 0 && mUpdates.size() < 10 ) {
            // dirty trick do NOT report on changes if they are not actually changes but one map contains an int value and the other a string value
            for(Object oKey: mUpdates.keySet() ) {
                Object oValNew = mUpdates.get(oKey);
                Object oValOld = mToBeOverwritten.get(oKey);
                if ( String.valueOf(oValOld).equals(String.valueOf(oValNew))) {
                    iChanges--;
                } else {
                    Log.d(TAG, String.format("Actual change %s ? %s != %s", oKey, oValNew, oValOld));
                }
            }
        }

        String sMsg;
        int iMsgDuration = 10;
        sUrl = URLFeedTask.shortenUrl(sUrl);
        boolean bRestartRequired = false;
        if ( iChanges > 0  ) {
            JSONObject joUpsertSettings = new JSONObject(mUpserts);
            bRestartRequired = importSettingsFromJSON(context, joUpsertSettings);

            // special cases
            if ( joUpsertSettings.has(PreferenceKeys.feedPostUrls.toString()) ) {
                PreferenceValues.setMatchesFeedURLUnchanged(false);
            }
            if ( joUpsertSettings.has(PreferenceKeys.showActionBar.toString()) ) {
                ScoreBoard.bUseActionBar = null;
                if ( context instanceof ScoreBoard) {
                    ((ScoreBoard)context).initShowActionBar();
                }
            }

            sMsg = context.getString(R.string.pref_RemoteConfig_X_SettingChanged, iChanges, sUrl);
            if ( iChanges < 5 ) {
                sMsg = sMsg + "\n\n" + ListUtil.join(mToBeOverwritten.keySet(), "\n");
                if ( MapUtil.size(mToBeOverwritten) < iChanges ) {
                    sMsg = sMsg + "\n" + ListUtil.join(mInserts.keySet(), "\n");
                }
            }
            iMsgDuration = 20;
        } else {
            sMsg = context.getString(R.string.pref_RemoteConfig_NoSettingChanges, sUrl);
            iMsgDuration = 3;
        }
        if ( context instanceof ScoreBoard) {
            ((ScoreBoard)context).showInfoMessageOnUiThread(sMsg, iMsgDuration);
        } else {
            Toast.makeText(context, sMsg, Toast.LENGTH_SHORT).show();
        }
        if ( bRestartRequired ) {
            int restartAppIfChangesDetected = joRemoteConfig.optInt("restartAppIfChangesDetected", 1);
            switch (restartAppIfChangesDetected) {
                case 1:
                    if ( context instanceof ScoreBoard) {
                        List<String> lMessages = new ArrayList<>();
                        lMessages.add(sMsg);
                        lMessages.add("Restart the app manually to have settings take effect");
                        ((ScoreBoard)context).askToRestart(lMessages);
                    }
                    break;
                case 2:
                    if ( context instanceof ScoreBoard) {
                        ((ScoreBoard)context).doRestart();
                    }
                    break;
                default:
                    break;
            }
        }

        // TODO: feedPostUrl=<a number>, if specified feedPostUrls should be there as well
        return iChanges;
    }
    public static boolean importSettingsFromJSON(Context context, JSONObject joSettings) throws JSONException
    {
        boolean bRestartRequired = false;

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();

        List<String> lSettingsToIgnore = getSettingsToIgnore();

        Iterator<String> keys = joSettings.keys();
        while( keys.hasNext() ) {
            String sKey = keys.next();
            if ( sKey.startsWith("-") ) { continue; }
            if ( lSettingsToIgnore.contains(sKey) ) {
                Log.d(TAG, "NOT importing " + sKey);
                continue;
            }

            Object oBUValue = joSettings.get(sKey);
            PreferenceKeys prefKey = null;
            try {
                prefKey = PreferenceKeys.valueOf(sKey);
                bRestartRequired = bRestartRequired || prefKey.restartRequired();

                Class clazz = prefKey.getType();
                if ( clazz != null ) {
                    if ( clazz.equals(Integer.class) ) {
                        //editor.putInt( sKey, Integer.parseInt(String.valueOf(oBUValue)));
                        editor.putString( sKey, String.valueOf(oBUValue)); // to ensure EditTextPreference.onSetInitialValue(), that always seems to get String value even for inputType=number, does not give a ClassCastException
                        continue;
                    } else if ( clazz.equals(Boolean.class) ) {
                        editor.putBoolean( sKey, Boolean.parseBoolean(String.valueOf(oBUValue)));
                        continue;
                    } else if ( clazz.equals(String.class) ) {
                        if ( oBUValue == null ) {
                            editor.putString( sKey, "");
                        } else {
                            editor.putString( sKey, String.valueOf(oBUValue));
                        }
                        continue;
                    } else if ( clazz.isEnum() ) {
                        Class<? extends Enum> eClazz = (Class<Enum>) clazz;
                        Object oValue = Params.getEnumValueFromString(eClazz, String.valueOf(oBUValue));
                        if ( oValue == null && oBUValue instanceof Boolean ) {
                            // assume first enum value turns something 'off'
                            oValue = eClazz.getEnumConstants()[0];
                        }
                        if ( oValue != null ) {
                            editor.putString(sKey, String.valueOf(oValue) );
                        } else {
                            Log.w(TAG, String.format("Invalid enum value in remote settings %s=%s", sKey, oBUValue));
                        }
                        continue;
                    }
                }
            } catch (Exception e) {
                if ( prefKey != null ) {
                    e.printStackTrace(); // TMP
                } else {
                    Log.d(TAG, "Key not one of PreferenceKeys " + sKey);
                }
            }

            if ( oBUValue instanceof String ) {
                editor.putString(sKey, (String) oBUValue);
            } else if ( oBUValue instanceof Boolean ) {
                editor.putBoolean(sKey, (Boolean) oBUValue);
            } else if ( oBUValue instanceof Integer ) {
                editor.putString(sKey, String.valueOf(oBUValue));
                //editor.putInt(sKey, (Integer) oBUValue);
            } else if ( oBUValue instanceof JSONArray) {
                // e.g. EnumSet
                JSONArray ar = (JSONArray) oBUValue;
                Set<String> setValues = new HashSet<>();
                for (int i = 0; i < ar.length(); i++) {
                    setValues.add(ar.getString(i));
                }
                editor.putStringSet(sKey, setValues);
            } else if ( oBUValue instanceof Double ) {
                //editor.putFloat(sKey, Float.parseFloat(oBUValue.toString()));
                editor.putInt(sKey, (int) Math.round((double) oBUValue));
            } else if ( oBUValue instanceof Float ) {
                //editor.putFloat(sKey, (Float) oBUValue);
                editor.putInt(sKey, Math.round((float) oBUValue));
            } else if ( oBUValue instanceof Long ) {
                //editor.putLong(sKey, (Long) oBUValue);
                editor.putInt(sKey, Integer.parseInt(String.valueOf(oBUValue)));
            } else {
                Log.w(TAG, String.format("Could not import %s=%s of type %s", sKey, oBUValue, oBUValue.getClass().getName()));
            }
        }
        editor.commit();

        return bRestartRequired;
    }

    public static JSONObject getCurrentSettings(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        Map<String, ?> buAll = preferences.getAll();

        TreeMap tmSettings = new TreeMap(buAll); // get stuff 'sorted'
        int iRemoved = cleanBrandBased(tmSettings);

        JSONObject joSettings = new JSONObject(tmSettings);
        return joSettings;
    }

    public static int cleanBrandBased(Map tmSettings) {
        int iSizeBefore = MapUtil.size(tmSettings);

        Map mToRemove1 = MapUtil.filterKeys(tmSettings, ".*\\.RunCount", Enums.Match.Keep);
        // some specials
        Map mToRemove2 = MapUtil.filterKeys(tmSettings, ".*(BLE|BluetoothLE).*", Enums.Match.Keep);

        MapUtil.removeAll(tmSettings, mToRemove1);
        MapUtil.removeAll(tmSettings, mToRemove2);

        List<String> lToRemove = getSettingsToIgnore();

        int iRemoved = MapUtil.removeAll(tmSettings, lToRemove);
        Log.d(TAG, String.format("Removed %d settings to ignore", iRemoved));

        return iSizeBefore - MapUtil.size(tmSettings);
    }

    private static List<String> getSettingsToIgnore() {
        List<String> lToRemove = new ArrayList<>();
        if (Brand.isGameSetMatch()) {
            for (PreferenceKeys key : Preferences.NONGameSetMatch_SpecificPrefs) {
                lToRemove.add(key.toString());
            }
        } else {
            for (PreferenceKeys key : Preferences.GameSetMatch_SpecificPrefs) {
                lToRemove.add(key.toString());
            }
        }
        if (Brand.supportsTimeout() == false) {
            lToRemove.add(PreferenceKeys.autoShowGamePausedDialogAfterXPoints.toString());
            lToRemove.add(PreferenceKeys.autoShowModeActivationDialog        .toString());
            lToRemove.add(PreferenceKeys.showModeDialogAfterXMins            .toString());
            lToRemove.add(PreferenceKeys.showGamePausedDialog                .toString());
            lToRemove.add(PreferenceKeys.timerTowelingDown                   .toString());
        }
        if ( Brand.isSquash() ) {
            lToRemove.add(PreferenceKeys.swapPlayersBetweenGames.toString());
            lToRemove.add(PreferenceKeys.swapPlayersHalfwayGame .toString());
            lToRemove.add(PreferenceKeys.useChangeSidesFeature  .toString());
        }
        if ( Brand.isTabletennis() == false ) {
            lToRemove.add(PreferenceKeys.numberOfServesPerPlayer     .toString());
            lToRemove.add(PreferenceKeys.numberOfServiceCountUpOrDown.toString());
        }
        lToRemove.add(PreferenceKeys.targetDirForImportExport.toString());
      //lToRemove.add(PreferenceKeys.OfficialRulesURLs       .toString());
        lToRemove.add(PreferenceKeys.squoreBrand             .toString());
        lToRemove.add(PreferenceKeys.viewedChangelogVersion  .toString());
        lToRemove.add(PreferenceKeys.onlyForContactGroups    .toString());
        lToRemove.add(PreferenceKeys.liveScoreDeviceId       .toString()); // if used for transferring settings, we do not want to devices having the same id
        
        return lToRemove;
    }

    public static void exportSettings(Context context) {
        File fSettingsJson = getSettingsBinaryFile(context, false, sSettingsExtension_JSON);
        try {
            if ( fSettingsJson == null ) {
                String sMsg = String.format("Could not store settings in %s", "No writable location");
                Toast.makeText(context, sMsg, Toast.LENGTH_LONG).show();
                return;
            }
            JSONObject joSettings = getCurrentSettings(context);
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

    public static void remoteSettingsErrorFeedback(Context context, ContentReceiver.FetchResult result, String sUrl) {
        String sDefaultUrl = PreferenceValues.getRemoteSettingsURL_Default(context, true);
        String sMsg = null;
        if ( sUrl.startsWith(sDefaultUrl) ) {
            sMsg = context.getString(R.string.could_not_load_x_settings_y, context.getString(R.string.lbl_default), sDefaultUrl);
            Log.d(TAG, sMsg);
            if ( PreferenceValues.getRemoteSettingsURL_AlwaysShowLoadErrors(context) ) {
                // show message anyways. User configured it like that
            } else {
                if ( PreferenceValues.currentDateIsTestDate() ) {
                    sMsg = "TEMP " + sMsg;
                } else {
                    sMsg = null;
                }
            }
        } else {
            sMsg = context.getString(R.string.could_not_load_x_settings_y, "", sUrl);
        }

        if (StringUtil.isNotEmpty(sMsg) ) {
            if ( context instanceof ScoreBoard) {
                ((ScoreBoard)context).showInfoMessageOnUiThread(sMsg, 10);
            } else {
                Toast.makeText(context, sMsg, Toast.LENGTH_SHORT).show();
            }
        }
    }

    public static int trimValues(Map mInput) {
        if ( MapUtil.isEmpty(mInput) ) {
            return 0;
        }
        Map mTrimmed = new HashMap();
        for(Object oKey: mInput.keySet()) {
            Object oVal = mInput.get(oKey);
            if ( oVal instanceof String ) {
                String sVal = (String) oVal;
                String sValTrimmed = sVal.trim();
                if ( sValTrimmed.length() != sVal.length() ) {
                    mTrimmed.put(oKey, sValTrimmed);
                }
            }
        }
        if ( MapUtil.isNotEmpty(mTrimmed) ) {
            mInput.putAll(mTrimmed);
            return mTrimmed.size();
        }
        return 0;
    }
}