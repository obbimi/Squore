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

package com.doubleyellow.scoreboard.archive;

import androidx.fragment.app.Fragment;
//import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.share.ResultSender;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.model.ModelFactory;
import com.doubleyellow.scoreboard.model.Player;
import com.doubleyellow.scoreboard.prefs.ColorPrefs;
import com.doubleyellow.util.DateUtil;
import com.doubleyellow.util.ListUtil;
import com.doubleyellow.util.MapUtil;
import com.doubleyellow.util.StringUtil;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Used to present recent matches for multiselect and then share.
 * Use by ArchiveTabbed
 */
public class RecentMatchesMultiSelect extends Fragment {

    private static final String TAG = "SB." + RecentMatchesMultiSelect.class.getSimpleName();

    /** Invoked from ArchiveTabbed */
    boolean shareSelected(final Context context) {
        if ( ListUtil.isEmpty(lSelected) ) {
            Toast.makeText(context, R.string.no_matches_selected, Toast.LENGTH_SHORT).show();
            return false;
        }
        // read the last x matches
        ResultSender resultSender = new ResultSender();
        resultSender.send(context, lSelected, null, null);
        return true;
    }

    StableArrayAdapter adapter;

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LinearLayout ll = new LinearLayout(getActivity());
        ll.setOrientation(LinearLayout.VERTICAL);

        ListView listView = new ListView(getActivity());
        listView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);

        final Map<String, File> mMatches = getMatches();

        adapter = new StableArrayAdapter(getActivity(), android.R.layout.simple_list_item_checked, mMatches);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                CheckedTextView tv = (CheckedTextView) view;
                String sTxt = tv.getText().toString();
                File f = mMatches.get(sTxt);
                if ( f == null ) {
                    Log.w(TAG, "No file found for text " + tv.getText());
                    return;
                }
                if ( tv.isChecked() ) {
                    lSelected.add(f);
                } else {
                    lSelected.remove(f);
                }

                String text = ResultSender.getMatchesSummary(lSelected, getActivity());
                if ( txtTmp != null ) {
                    txtTmp.setText(text.trim());
                }
            }
        });
        ll.addView(listView);

        if ( MapUtil.isEmpty(mMatches) ) {
            // no matches
            TextView tvNoMatches = new TextView(getActivity());
            tvNoMatches.setText(R.string.sb_no_recent_matches_stored);
            ll.addView(tvNoMatches);
        }

        // TODO: temporary
        if ( MapUtil.isNotEmpty(mMatches) ) {
            //txtTmp = new TextView(getActivity());
        }
        if ( txtTmp != null ) {
            txtTmp.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            txtTmp.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | txtTmp.getInputType());
            //txtTmp.setEnabled(false);
            ll.addView(txtTmp);
        }

        //ColorPrefs.setColors(getActivity(), listView);

        return ll;
    }
    public void initMatches() {
        Map<String, File> matches = getMatches();
        if ( adapter == null ) {
            adapter = new StableArrayAdapter(getActivity(), android.R.layout.simple_list_item_checked, matches);
        } else {
            adapter.init(matches);
        }
    }
    private Map<String, File> getMatches() {
        Date dAfter = DateUtil.getCurrent_addDays(-14, false);
        List<File> lMatches = PreviousMatchSelector.getPreviousMatchFiles(getActivity(), dAfter);
        if ( false && ListUtil.size(lMatches) < 2 ) {
            lMatches = PreviousMatchSelector.getAllPreviousMatchFiles(getActivity());
        }
        final Map<String, File> mMatches = new TreeMap<>();
        if ( ListUtil.isNotEmpty(lMatches) ) {
            for(File f: lMatches) {
                Model match = ModelFactory.getTemp();
                try {
                    match.fromJsonString(f);
                } catch (Exception e) {
                    continue;
                }
                Date dMatchDate      = match.getMatchDate();
                DateFormat sdf       = android.text.format.DateFormat.getDateFormat(getActivity());
                if (sdf instanceof SimpleDateFormat) {
                    // remove the year
                    String sPattern = ((SimpleDateFormat) sdf).toPattern();
                    sPattern = sPattern.replaceAll("[Yy]+[^\\w]", "").replaceAll("[^\\w][Yy]+", "");
                    sdf = new SimpleDateFormat(sPattern);
                }
                String sDate      = sdf.format(dMatchDate);
                String sA         = match.getName(Player.A);
                String sB         = match.getName(Player.B);
                String sMatchDesc = sDate + " " + (match.getMatchStartTimeHH_Colon_MM() + " " + sA + " - " + sB).trim();

                String sResult = match.getResult();
                if ( StringUtil.isNotEmpty(sResult) && (sResult.equals("0-0") == false)) {
                    sMatchDesc += " : " + sResult;
                }
                mMatches.put(sMatchDesc, f);
            }
        }
        return mMatches;
    }

    private TextView txtTmp;
    private List<File> lSelected = new ArrayList<>();

    public class StableArrayAdapter extends ArrayAdapter<String>
    {
        StableArrayAdapter(Context context, int textViewResourceId, Map<String, File> objects) {
            super(context, textViewResourceId, new ArrayList<String>(objects.keySet()));
        }

        @Override public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            view.setTag(ColorPrefs.Tags.item);
            ColorPrefs.setColor(view);
            return view;
        }
        public void init(Map<String, File> objects) {
            super.clear();
            super.addAll(new ArrayList<String>(objects.keySet()));
            super.notifyDataSetChanged();
        }
    }
}
