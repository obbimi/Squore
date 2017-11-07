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
 * Used e.g. for the ColorPalette
 */
public class DynamicListPreference extends ListPreference {

  //private static final String SELECTED_INDEX = "selectedIndex";
    private static final String TAG = "SB." + DynamicListPreference.class.getSimpleName();

    public DynamicListPreference(Context context, AttributeSet attrs) {
        super(context, attrs); // invoked when pref screen is created
    }

    public DynamicListPreference(Context context) {
        super(context);
    }

    private int selectedIndex = 0;
    private boolean bAllowNew = true;
    public void setAllowNew(boolean b) {
        this.bAllowNew = b;
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
            selectedIndex = Integer.parseInt(sValue);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        CharSequence[] entriesFromJson = getEntries();
        builder.setSingleChoiceItems(entriesFromJson, selectedIndex, singleChoiceListener);

        builder.setOnItemSelectedListener(onItemSelectedListener);
    }

    private AdapterView.OnItemSelectedListener onItemSelectedListener = new AdapterView.OnItemSelectedListener() {
        @Override public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) { }
        @Override public void onNothingSelected(AdapterView<?> adapterView) { }
    };

    private LinkedHashMap<CharSequence, EditText> lTexts;
    private DialogInterface listDialogInterface = null;

    private DialogInterface.OnClickListener singleChoiceListener = new DialogInterface.OnClickListener()
    {
        @Override public void onClick(final DialogInterface dialogInterface, int i) {
            listDialogInterface = dialogInterface;

            if (i == iNewIndex) {
                // user selected item labeled 'New...'

                // build the dialog where user can specify new values
                final LinearLayout ll = new LinearLayout(getContext());
                ll.setOrientation(LinearLayout.VERTICAL);
                lTexts = new LinkedHashMap<CharSequence, EditText>();
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
                    lTexts.put(s, p);
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
                        .setPositiveButton(getContext().getString(R.string.cmd_ok), onClickCreateOrCancelListener)
                        .setNegativeButton(getContext().getString(R.string.cmd_cancel), onClickCreateOrCancelListener);
                AlertDialog dialog = adb.show();
            } else {
                if (selectedIndex == i) {
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
                            .setMessage(getContext().getString(msgId, getSelectedName(selectedIndex)))
                            .setIcon(iconId)
                            .setPositiveButton(getContext().getString(R.string.cmd_ok    ), deleteDialogClickListener)
                            .setNegativeButton(getContext().getString(R.string.cmd_cancel), deleteDialogClickListener);
                    if (bAllowEdit) {
                         adb.setNeutralButton(getContext().getString(R.string.cmd_edit   ), deleteDialogClickListener);
                    }
                    AlertDialog dialog = adb.show();
                } else {
                    selectedIndex = i;
                    DynamicListPreference.super.setValue(String.valueOf(selectedIndex));

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
                    m_persistedListOfMaps.delete(selectedIndex);

                    selectedIndex = Math.max(selectedIndex - 1, 0);
                    DynamicListPreference.super.setValue(String.valueOf(selectedIndex));
                    setOptionsFromJson(m_persistedListOfMaps);

                    listDialogInterface.cancel();
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
                    for (CharSequence n : lTexts.keySet()) {
                        EditText e = lTexts.get(n);
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

                    selectedIndex = m_persistedListOfMaps.add(mNewEntry);
                    DynamicListPreference.super.setValue     (String.valueOf(selectedIndex));
                    DynamicListPreference.super.setValueIndex(               selectedIndex);
                    setOptionsFromJson(m_persistedListOfMaps);

                    listDialogInterface.cancel();
                    break;
                case DialogInterface.BUTTON_NEGATIVE:
                    // Do nothing.
                    break;
            }
        }
    };

    private int iNewIndex = 0;

    @Override public String getValue() {
        return String.valueOf(selectedIndex);
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

    private Status iInterpretationDone = Status.Uninitilized;

    /** Populates m_persistedListOfMaps */
    private boolean interpretEntries() {

        if ( iInterpretationDone.equals(Status.InitilizedFromFile) ) return true;

        File file = getOptionsJsonFile(getContext(), this.getKey());

        m_persistedListOfMaps = new PersistedListOfMaps(file, null);
        if ( m_persistedListOfMaps.read() ) {
            iInterpretationDone = Status.InitilizedFromFile;
        } else {
            iInterpretationDone = Status.InitilizationFromFileFailed;
            Log.w(TAG, "Could not read data for " + this.getKey() + " from " + file.getPath());
        }
        return (iInterpretationDone.equals(Status.InitilizedFromFile));
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
        iInterpretationDone = Status.Uninitilized;
        return super.onSaveInstanceState();
    }

    private PersistedListOfMaps m_persistedListOfMaps = null;

    private void setOptionsFromJson(PersistedListOfMaps persistedListOfMaps)
    {
        List<String> entryProperties = persistedListOfMaps.getEntryProperties();
        if ( entryProperties == null ) { return; } // seen a nullpointer here once after choosing 'reset to default' from withing 'settings screen' itself
        List<Map<String, String>> content = persistedListOfMaps.getContent();
        List<String> lLetUserSelectFrom = MapUtil.listOfMaps2List(content, entryProperties.get(0));

        if ( this.bAllowNew ) {
            iNewIndex = lLetUserSelectFrom.size();
            String sLabel = getContext().getString(R.string.uc_new);
            lLetUserSelectFrom.add(sLabel);
        } else {
            iNewIndex = -1;
        }

        CharSequence[] entryValues = lLetUserSelectFrom.toArray(new CharSequence[0]);

        super.setEntryValues(entryValues);
        super.setEntries    (entryValues);
    }

    private String sDefault = "{}";
    @Override public void setDefaultValue(Object defaultValue) {
        sDefault = defaultValue!=null?defaultValue.toString():sDefault;
    }

    @Override public CharSequence getEntry() {
        CharSequence entry = super.getEntry();
        if ( entry == null ) {
            return getSelectedName(super.getValue());
        }
        return entry;
    }

    public CharSequence getSelectedName(Object oValue) {
        String sValue = String.valueOf(oValue);
        try {
            selectedIndex = Integer.parseInt(sValue);
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
            if ( (selectedIndex < 0) || (selectedIndex >= ListUtil.size(lLetUserSelectFrom))) {
                selectedIndex = Math.max(0, ListUtil.size(lLetUserSelectFrom)-1);
            }
            if ( selectedIndex < ListUtil.size(lLetUserSelectFrom) ) {
                return lLetUserSelectFrom.get(selectedIndex);
            }
        }
        return null;
    }

    @Override protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
    }
}
