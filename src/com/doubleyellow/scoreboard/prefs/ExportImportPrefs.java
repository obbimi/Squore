package com.doubleyellow.scoreboard.prefs;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.widget.Toast;
import com.doubleyellow.android.util.ExportImport;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.util.FileUtil;

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

                ScoreBoard.matchModel.setDirty();

                PreferenceValues.setRestartRequired(context);
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                // export
                exportSettings(context);
                break;
        }
    }

    public static void importSettings(Context context) {
        File fSettings = getSettingsBinaryFile(context, true);
        if ( fSettings.exists() == false ) {
            String sMsg  =        context.getString(R.string.could_not_find_file_x                 , fSettings.getAbsolutePath());
                   sMsg += "\n" + context.getString(R.string.did_you_already_perform_an_export_of_x, context.getString(R.string.sb_preferences));
            Toast.makeText(context, sMsg, Toast.LENGTH_LONG).show();
            return;
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        Map<String, ?> buAll = (Map<String, ?>) FileUtil.readObjectFromFile(fSettings);

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
    }

    public static void exportSettings(Context context) {
        File fSettings = getSettingsBinaryFile(context, false);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        Map<String, ?> buAll = preferences.getAll();

        // TODO: remove the settings that have not changed anyway (so that unchanged properties are not transferred from one device to another with e.g. different resolution)

        boolean bWritten = FileUtil.writeObjectToFile(fSettings, buAll);
        if ( bWritten ) {
            String sMsg = context.getString(R.string.x_stored_in_y, context.getString(R.string.sb_preferences), fSettings.getAbsolutePath());
            Toast.makeText(context, sMsg, Toast.LENGTH_LONG).show();
        }
    }

    public static File getSettingsBinaryFile(Context context, boolean bForImport) {
        File externalStorageDirectory = PreferenceValues.targetDirForImportExport(context, bForImport); // typically '/storage/emulated/0'
        ExportImport.MountState mountStateExternalStorage = ExportImport.getMountStateExternalStorage();
        if ( externalStorageDirectory == null || mountStateExternalStorage.equals(ExportImport.MountState.UnMounted) ) {
            Toast.makeText(context, R.string.could_not_determine_external_storage_location, Toast.LENGTH_LONG).show();
            return null;
        }
        return new File(externalStorageDirectory, "Squore.settings.bin");
    }

}