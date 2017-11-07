package com.doubleyellow.scoreboard.view;

import android.content.Context;
import android.content.res.Resources;
import android.text.Editable;
import android.text.InputType;
import android.util.AttributeSet;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.prefs.PreferenceKeys;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.util.ListUtil;
import com.doubleyellow.util.StringUtil;

import java.util.Arrays;
import java.util.List;

/**
 * Generic extension of an 'auto complete' text view for easy entering of values from a list while having the option to add new values to the list.
 * After the first character all items stored in the preference are presented to the user for selection.
 *
 * This makes it e.g. much easier for the app user to quickly enter events and rounds for a match.
 */
public class PreferenceACTextView extends AutoCompleteTextView
{
    public static final String APPLICATION_NS    = "http://double-yellow.be";
    public static final String PREFERENCE_KEYS   = "PreferenceKeys";
    public static final String DEFAULT_AC_VALUES = "defaultACValues";

    /** Invoked when created for popup */
    public PreferenceACTextView(Context context) {
        this(context, null);
    }

    private PreferenceKeys prefListKey       = null;
    private PreferenceKeys prefLastKey       = null;
    private int            iResDefaultValues = 0;

    public PreferenceACTextView(Context context, AttributeSet attrs) {
        super(context, attrs);

        if ( attrs != null ) {
            String preferenceKeys = attrs.getAttributeValue(APPLICATION_NS, PREFERENCE_KEYS);
            String[] keys = StringUtil.singleCharacterSplit(preferenceKeys); // must be of length 2
            int iResDefaultACValues = attrs.getAttributeResourceValue(APPLICATION_NS, DEFAULT_AC_VALUES, 0);

            PreferenceKeys prefListKey = PreferenceKeys.valueOf(keys[0]);
            PreferenceKeys prefLastKey = PreferenceKeys.valueOf(keys[1]);
            init(iResDefaultACValues, prefListKey, prefLastKey);
        }

        this.setSingleLine();
        this.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
    }

    public void init(int iResDefaultACValues, PreferenceKeys prefListKey, PreferenceKeys prefLastKey) {
        this.iResDefaultValues = iResDefaultACValues;
        this.prefListKey       = prefListKey;
        this.prefLastKey       = prefLastKey;
    }

/*
    public PreferenceACTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
*/

    @Override protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        initializeAdapter();
    }

    @Override public Editable getText() {
        setDefaultTextIfRequired();
        return super.getText();
    }

    public void setDefaultTextIfRequired() {
        if ( useLastValueAsDefault && (prefLastKey != null) ) {
            useLastValueAsDefault = false;
            String sDefaultValue = PreferenceValues.getString(prefLastKey, "", getContext());
            if (StringUtil.isNotEmpty(sDefaultValue) && StringUtil.isEmpty(super.getText())) {
                super.setText(sDefaultValue);
            }
        }
    }
/*
    @Override public void setText(CharSequence text, BufferType type) {
        super.setText(text, type);
    }
*/

    public Editable getTextAndPersist() {
        return getTextAndPersist(true);
    }
    public Editable getTextAndPersist(boolean bStoreAlsoIfEmpty) {
        Editable text = getText();
        String sText = text.toString().trim();
        if ( (prefListKey != null) ) {
            if ( StringUtil.isNotEmpty(sText) ) {
                PreferenceValues.addStringToList(getContext(), prefListKey, iResDefaultValues, sText);
            }
            if ( StringUtil.isNotEmpty(sText) || bStoreAlsoIfEmpty ) {
                // 20150930: also store if empty: app user might prefer certain fields to be empty
                PreferenceValues.setString(prefLastKey, getContext(), sText);
            }
        }
        return text;
    }

    private boolean bAdapterIsInitialized = false;
    private void initializeAdapter() {
        if ( bAdapterIsInitialized ) {
            return;
        }
        if ( prefListKey != null ) {
            List<String> list = PreferenceValues.getStringAsList(getContext(), prefListKey, iResDefaultValues);
            setAutoCompleteAdapter(list);
            bAdapterIsInitialized = true;
        }
    }

    private boolean useLastValueAsDefault = true;
    public void useLastValueAsDefault(boolean b) {
        this.useLastValueAsDefault = b;
    }

    private final int iAutoCompleteLayoutResourceId = R.layout.expandable_match_selector_item;
    private void setAutoCompleteAdapter(List<String> list) {
        if ( ListUtil.isEmpty(list) ) {
            if ( iResDefaultValues != 0 ) {
                String[] stringArray = new String[0];
                try {
                    stringArray = getContext().getResources().getStringArray(iResDefaultValues);
                } catch (Resources.NotFoundException e) {
                    String s = getContext().getResources().getString(iResDefaultValues);
                    if (StringUtil.isNotEmpty(s) ) {
                        stringArray = s.split("[\\n\\r\\|]");
                    }
                }
                list = Arrays.asList(stringArray);
            }
        }
        if ( ListUtil.isEmpty(list) ) {
            return;
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(), iAutoCompleteLayoutResourceId, list);

        // let 'when to suggest' depend on the size of the complete list
        int iSuggestAfterAtLeast = Math.min(2, Math.max(1, ListUtil.size(list)/100));
        //iSuggestAfterAtLeast = PreferenceValues.numberOfCharactersBeforeAutocomplete(getContext());

        this.setAdapter(adapter);
        this.setThreshold(iSuggestAfterAtLeast);

        setDefaultTextIfRequired();
    }
}
