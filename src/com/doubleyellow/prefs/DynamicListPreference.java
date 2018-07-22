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
package com.doubleyellow.prefs;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Parcelable;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import com.doubleyellow.scoreboard.R;
import com.doubleyellow.util.ListUtil;
import com.doubleyellow.util.MapUtil;
import com.doubleyellow.util.PersistedListOfMaps;
import com.doubleyellow.util.StringUtil;

import java.io.File;
import java.util.*;

/**
 * To be able to have a preference with list options that can be extended by the user.
 * Used only for the ColorPalette (for now)
 */
public class DynamicListPreference extends ListPreference {

  //private static final String SELECTED_INDEX = "m_selectedIndex";
    private static final String TAG = "SB." + DynamicListPreference.class.getSimpleName();

    public DynamicListPreference(Context context, AttributeSet attrs) {
        super(context, attrs); // invoked when pref screen is created
    }

    public DynamicListPreference(Context context) {
        super(context);
    }

    private int     m_selectedIndex = 0;
    private boolean m_bAllowNew     = true;
    private int     m_iNewIndex     = 0;

    public void setAllowNew(boolean b) {
        m_bAllowNew = b;
    }
    public static boolean deleteCacheFile(Context context, String sPrefKey) {
        File f = getOptionsJsonFile(context, sPrefKey);
        if ( f.exists() == false ) {
            return false;
        }
        return f.delete();
    }

    @Override protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder); // Click.1

        interpretEntries();
        setOptionsFromJson(m_persistedListOfMaps);

        String sValue = super.getValue();
        try {
            m_selectedIndex = Integer.parseInt(sValue);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        CharSequence[] entriesFromJson = getEntries();
        builder.setSingleChoiceItems(entriesFromJson, m_selectedIndex, singleChoiceListener);

        builder.setOnItemSelectedListener(onItemSelectedListener);
    }

    private AdapterView.OnItemSelectedListener onItemSelectedListener = new AdapterView.OnItemSelectedListener() {
        @Override public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) { }
        @Override public void onNothingSelected(AdapterView<?> adapterView) { }
    };

    private LinkedHashMap<CharSequence, EditText> m_lTexts;
    private DialogInterface m_dialogInterface = null;

    private DialogInterface.OnClickListener singleChoiceListener = new DialogInterface.OnClickListener()
    {
        @Override public void onClick(final DialogInterface dialogInterface, int i) {
            m_dialogInterface = dialogInterface;

            if ( m_iNewIndex == i ) {
                // user selected item labeled 'New...'

                if ( m_persistedListOfMaps != null ) { // safety precaution... should not happen
                    // build the dialog where user can specify new values
                    final LinearLayout ll = new LinearLayout(getContext());
                    ll.setOrientation(LinearLayout.VERTICAL);
                    m_lTexts = new LinkedHashMap<CharSequence, EditText>();
                    for (CharSequence s : m_persistedListOfMaps.getEntryProperties()) {
                        LinearLayout llLabelText = new LinearLayout(getContext());
                        llLabelText.setOrientation(LinearLayout.HORIZONTAL);

                        TextView lbl = new TextView(getContext());
                        lbl.setText(StringUtil.capitalize(s.toString(), false));
                        llLabelText.addView(lbl);

                        EditText p = new EditText(getContext());
                        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        p.setLayoutParams(layoutParams);
                        p.setSingleLine();
                        llLabelText.addView(p);

                        ll.addView(llLabelText);

                        // for when user is finished
                        m_lTexts.put(s, p);
                    }


                    AlertDialog.Builder adb = new AlertDialog.Builder(getContext());
                    CharSequence title = getTitle();
                    if (StringUtil.isNotEmpty(title)) {
                        title = title.toString().replaceAll("\\([^\\)]*\\)$", "").trim();
                    }
                    adb.setTitle(R.string.uc_new)
                            .setMessage(getContext().getString(R.string.sb_new_item, title))
                            .setView(ll)
                            .setIcon(android.R.drawable.ic_menu_add)
                            .setPositiveButton(R.string.cmd_ok    , onClickCreateOrCancelListener)
                            .setNegativeButton(R.string.cmd_cancel, onClickCreateOrCancelListener);
                    AlertDialog dialog = adb.show();
                } else {
                    Log.w(TAG, "m_persistedListOfMaps is empty");
                }
            } else {
                if (m_selectedIndex == i) {
                    // was already selected: present user with option edit/delete?

                    boolean bAllowEdit = false;
                    int msgId   = R.string.sb_delete_item;
                    int titleId = R.string.uc_delete;
                    int iconId  = android.R.drawable.ic_menu_close_clear_cancel;
                    AlertDialog.Builder adb = new AlertDialog.Builder(getContext());
                    if ( bAllowEdit ) {
                        msgId   = R.string.sb_edit_item;
                        titleId = R.string.uc_edit;
                        iconId  = android.R.drawable.ic_menu_edit;
                    }
                    adb.setTitle(titleId)
                            .setMessage(getContext().getString(msgId, getSelectedName(m_selectedIndex)))
                            .setIcon(iconId)
                            .setPositiveButton(getContext().getString(R.string.cmd_ok    ), deleteDialogClickListener)
                            .setNegativeButton(getContext().getString(R.string.cmd_cancel), deleteDialogClickListener);
                    if (bAllowEdit) {
                         adb.setNeutralButton(getContext().getString(R.string.cmd_edit   ), deleteDialogClickListener);
                    }
                    AlertDialog dialog = adb.show();
                } else {
                    m_selectedIndex = i;
                    DynamicListPreference.super.setValue(String.valueOf(m_selectedIndex));

                    dialogInterface.cancel();
                }
            }
        }
    };

    private DialogInterface.OnClickListener deleteDialogClickListener = new DialogInterface.OnClickListener() {
        @Override public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    // do the delete
                    m_persistedListOfMaps.delete(m_selectedIndex);

                    m_selectedIndex = Math.max(m_selectedIndex - 1, 0);
                    DynamicListPreference.super.setValue(String.valueOf(m_selectedIndex));
                    setOptionsFromJson(m_persistedListOfMaps);

                    m_dialogInterface.cancel();
                    break;
                case DialogInterface.BUTTON_NEUTRAL:
                    // edit the entry: TODO
                    // Sorry, not yet implemented. For now 'delete' the item and re-create it with the desired values.
                case DialogInterface.BUTTON_NEGATIVE:
                    // Do nothing.
                    break;
            }
        }
    };

    private DialogInterface.OnClickListener onClickCreateOrCancelListener = new DialogInterface.OnClickListener() {
        @Override public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    // try and create a new entry

                    // first do some validation of the provided values
                    Map<String, String> mNewEntry = new HashMap<String, String>();
                    for (CharSequence n : m_lTexts.keySet()) {
                        EditText e = m_lTexts.get(n);
                        String sEnteredValue = e.getText().toString();
                        if ( StringUtil.isEmpty(sEnteredValue)) continue;
                        mNewEntry.put(n.toString(), sEnteredValue);
                    }

                    // TODO: more complex validation but in a generic way
                    if (MapUtil.size(mNewEntry) < m_persistedListOfMaps.getEntryProperties().size() ) {
                        // not all values provided
                        Toast toast = Toast.makeText(getContext(), "Sorry, all values need to be specified", Toast.LENGTH_LONG);
                        toast.show();
                        // TODO: keep the dialog open (like with pause in timer)
                        return;
                    }

                    m_selectedIndex = m_persistedListOfMaps.add(mNewEntry);
                    DynamicListPreference.super.setValue     (String.valueOf(m_selectedIndex));
                    DynamicListPreference.super.setValueIndex(m_selectedIndex);
                    setOptionsFromJson(m_persistedListOfMaps);

                    m_dialogInterface.cancel();
                    break;
                case DialogInterface.BUTTON_NEGATIVE:
                    // Do nothing.
                    break;
            }
        }
    };

    @Override public String getValue() {
        return String.valueOf(m_selectedIndex);
    }

    @Override public void setEntries(CharSequence[] sequence) {
        // display values: use only for 'new' entry
        interpretEntries();
        setOptionsFromJson(m_persistedListOfMaps);
    }

    @Override public void setEntryValues(CharSequence[] sequence) {
        interpretEntries();
        setOptionsFromJson(m_persistedListOfMaps);
    }

    private enum Status {
        Uninitilized,
        InitilizedFromFile,
        InitilizationFromFileFailed,
    }

    private Status m_status = Status.Uninitilized;

    /** Populates m_persistedListOfMaps */
    private boolean interpretEntries() {

        if ( m_status.equals(Status.InitilizedFromFile) ) return true;

        File file = getOptionsJsonFile(getContext(), this.getKey());

        m_persistedListOfMaps = new PersistedListOfMaps(file, null);
        if ( m_persistedListOfMaps.read() ) {
            m_status = Status.InitilizedFromFile;
        } else {
            m_status = Status.InitilizationFromFileFailed;
            Log.w(TAG, "Could not read data for " + this.getKey() + " from " + file.getPath());
        }
        return (m_status.equals(Status.InitilizedFromFile));
    }

    private static File getOptionsJsonFile(Context context, String sPrefKey) {
        final File cacheDir = context.getCacheDir();
        File file = new File(cacheDir, sPrefKey );
        if ( file.exists() == false ) {
            file = new File(context.getFilesDir(), sPrefKey + ".json");
        }
        return file;
    }

    @Override protected Parcelable onSaveInstanceState() {
        m_status = Status.Uninitilized;
        return super.onSaveInstanceState();
    }

    private PersistedListOfMaps m_persistedListOfMaps = null;

    private void setOptionsFromJson(PersistedListOfMaps persistedListOfMaps)
    {
        List<String> entryProperties = persistedListOfMaps.getEntryProperties();
        if ( entryProperties == null ) { return; } // seen a nullpointer here once after choosing 'reset to default' from withing 'settings screen' itself
        List<Map<String, String>> content = persistedListOfMaps.getContent();
        List<String> lLetUserSelectFrom = MapUtil.listOfMaps2List(content, entryProperties.get(0));

        if ( m_bAllowNew ) {
            m_iNewIndex = lLetUserSelectFrom.size();
            String sLabel = getContext().getString(R.string.uc_new);
            lLetUserSelectFrom.add(sLabel);
        } else {
            m_iNewIndex = -1;
        }

        CharSequence[] entryValues = lLetUserSelectFrom.toArray(new CharSequence[0]);

        super.setEntryValues(entryValues);
        super.setEntries    (entryValues);
    }

    private String m_sDefault = "{}";
    @Override public void setDefaultValue(Object defaultValue) {
        m_sDefault = defaultValue!=null ? defaultValue.toString() : m_sDefault;
    }

    @Override public CharSequence getEntry() {
        CharSequence entry = super.getEntry();
        if ( entry == null ) {
            return getSelectedName(super.getValue());
        }
        return entry;
    }

    private CharSequence getSelectedName(Object oValue) {
        String sValue = String.valueOf(oValue);
        try {
            m_selectedIndex = Integer.parseInt(sValue);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        if ( interpretEntries() ) {
            setOptionsFromJson(m_persistedListOfMaps);

            List<Map<String,String>> content = m_persistedListOfMaps.getContent();
            List<String> entryProperties = m_persistedListOfMaps.getEntryProperties();
            if (ListUtil.isEmpty(entryProperties)) { return null; }
            String oValueColumn = entryProperties.get(0);
            List<String> lLetUserSelectFrom = MapUtil.listOfMaps2List(content, oValueColumn);
            if ( (m_selectedIndex < 0) || (m_selectedIndex >= ListUtil.size(lLetUserSelectFrom))) {
                m_selectedIndex = Math.max(0, ListUtil.size(lLetUserSelectFrom)-1);
            }
            if ( m_selectedIndex < ListUtil.size(lLetUserSelectFrom) ) {
                return lLetUserSelectFrom.get(m_selectedIndex);
            }
        }
        return null;
    }

    @Override protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
    }
}
