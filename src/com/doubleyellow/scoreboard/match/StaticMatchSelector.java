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

package com.doubleyellow.scoreboard.match;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.widget.*;

import com.doubleyellow.android.util.SimpleELAdapter;
import com.doubleyellow.android.view.SelectEnumView;
import com.doubleyellow.android.view.ViewUtil;
import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.archive.PreviousMatchSelector;
import com.doubleyellow.scoreboard.feed.FeedMatchSelector;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.match.fixed.DialogOnClickListener;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.model.ModelFactory;
import com.doubleyellow.scoreboard.model.Player;
import com.doubleyellow.scoreboard.prefs.ColorPrefs;
import com.doubleyellow.scoreboard.prefs.NewMatchesType;
import com.doubleyellow.scoreboard.prefs.PreferenceKeys;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.scoreboard.view.ExpandableListUtil;
import com.doubleyellow.scoreboard.view.PlayerTextView;
import com.doubleyellow.util.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * The Fragment that allows the user to define sets of matches up front for easy selection later on.
 */
public class StaticMatchSelector extends ExpandableMatchSelector
{
    public static final String TAG = "SB." + StaticMatchSelector.class.getSimpleName();

    public static boolean matchIsFrom(String modelSource) {
        return StaticMatchSelector.class.getName().equals(modelSource);
    }
    public static void setMatchIsFrom(Model model) {
        model.setSource(StaticMatchSelector.class.getName(), null);
    }

    private LinearLayout.LayoutParams llpMargin1Weight1 = null;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        llpMargin1Weight1 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        llpMargin1Weight1.weight = 1;
        llpMargin1Weight1.setMargins(3, 3, 3, 3);
    }

    @Override protected void setGuiDefaults(List<String> lExpanded) {
        ExpandableListUtil.expandAllOrFirst(expandableListView, 4);
    }

    private void editMatch(final String sToHeader, final String sMatch, final boolean bIsReplace, final NewMatchesType newMatchesType, final int iNrOfTxtBoxes ) {
        LayoutInflater myLayout = LayoutInflater.from(context);

        final int iNrOfGroups = newMatchesType==null?1:newMatchesType.getNumberOfGroups();

        final List<List<TextView>> llTextViews = new ArrayList<List<TextView>>();
        llTextViews.add(new ArrayList<TextView>());
        if ( iNrOfGroups == 2 ) {
            // add additional array of txt views
            llTextViews.add(new ArrayList<TextView>());
        }

        for(int g = 1; g <= iNrOfGroups; g++) {
            List<TextView> lTextViews = llTextViews.get(g-1);
            for(int i=1; i<=iNrOfTxtBoxes; i++) {
                // using 'inflation' because it was the only way to set android:textCursorDrawable="@null" properly
                // this allows the cursor to have the color of the text
                // PlayerTextView txt = new PlayerTextView(activity);
                PlayerTextView txt = (PlayerTextView) myLayout.inflate(R.layout.playertextview_default, null);
                txt.setHint(getString(R.string.lbl_player_x, ((iNrOfGroups==2)?(g==1?"A":"B") :"") + String.valueOf(i)));
                txt.setTag(ColorPrefs.Tags.item);
                txt.setText("");
                //int type = txt.getInputType() | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
                //txt.setInputType(type);
                lTextViews.add(txt);
            }
        }

        DialogInterface.OnClickListener dialogClickListener = new DialogOnClickListener(newMatchesType, sToHeader, sMatch, bIsReplace, llTextViews, this);

        int orientation = LinearLayout.VERTICAL;
        if ( (newMatchesType != null) && (newMatchesType.getNumberOfGroups() > 1) && ViewUtil.isLandscapeOrientation(context) ) {
            orientation = LinearLayout.HORIZONTAL;
        }

        LinearLayout llParent = new LinearLayout(context);
        llParent.setOrientation(orientation);

        // derive player names from optional single match passed in
        final String[] saPlayers = StringUtil.isEmpty(sMatch)?new String[]{}:sMatch.split(NAMES_SPLITTER);
        {
            List<TextView> lTextViews = llTextViews.get(0);
            lTextViews.get(0).setText(saPlayers.length < 2 ? "" : saPlayers[0]);
            lTextViews.get(1).setText(saPlayers.length < 2 ? "" : saPlayers[1]);
        }

        // next 2 pieces of code in (true and false) create the same visual layout only with different parent containers.
        // attempt to get nextfocusdown to work withing one team...
        for(int g = 1; g <= iNrOfGroups; g++) {
            LinearLayout ll = new LinearLayout(context);
            ll.setLayoutParams(llpMargin1Weight1);
            ll.setOrientation(LinearLayout.VERTICAL);
            ll.setPadding(1,1,1,1);
            llParent.addView(ll);

            List<TextView> lTextViews = llTextViews.get(g-1);
            // add a 'header' if names for multiple teams/clubs are entered
            if ( iNrOfGroups > 1 ) {
                TextView txtLabel = new TextView(context);
                txtLabel.setText(getString(g==1? R.string.lbl_club_A: R.string.lbl_club_B));
                ll.addView(txtLabel, llpMargin1Weight1);
            }
            for(int i=1; i<=iNrOfTxtBoxes; i++) {
                TextView textView = lTextViews.get(i-1);
                ll.addView(textView, llpMargin1Weight1);
                int iViewId = 100 * g + i;
                textView.setId(iViewId);
                if ( i < iNrOfTxtBoxes ) {
                    textView.setNextFocusDownId(iViewId + 1);
                    textView.setImeOptions(EditorInfo.IME_ACTION_NEXT);
                } else {
                    // i == iNrOfTxtBoxes
                    if ( g < iNrOfGroups ) {
                        textView.setNextFocusDownId(100 * (g+1) + 1);
                        textView.setImeOptions(EditorInfo.IME_ACTION_NEXT);
                    } else {
                        textView.setImeOptions(EditorInfo.IME_ACTION_DONE);
                    }
                }
                Log.d(TAG, String.format("Next focus down for %s set to %d", textView.getId() + ":" + textView.getHint(), textView.getNextFocusDownId()));
            }
        }

        ColorPrefs.setColor(llParent);

        AlertDialog.Builder ab = ScoreBoard.getAlertDialogBuilder(context);
        ab.setMessage       (saPlayers.length<2?R.string.cmd_new_matches_with:R.string.sb_edit_players)
          .setIcon          (R.drawable.circled_plus)
          .setPositiveButton(R.string.cmd_ok    , dialogClickListener)
          .setNegativeButton(R.string.cmd_cancel, dialogClickListener);
        final AlertDialog dialog = ab.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override public void onShow(DialogInterface dialogInterface) {
                llTextViews.get(0).get(0).requestFocus();
                ViewUtil.showKeyboard(dialog);

                final Button btnOk = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                int iMinNonEmpty = ListUtil.size(llTextViews.get(0));
                for(int g=0; g < ListUtil.size(llTextViews); g++) {
                    iMinNonEmpty = Math.min(iMinNonEmpty, ViewUtil.countNonEmpty(llTextViews.get(g)));
                }
                btnOk.setEnabled(iMinNonEmpty >= 2);
            }
        });
        TextWatcher txtWatcher = new BasicTextWatcher() {
            @Override public void afterTextChanged(Editable s) {
                Log.d(TAG, "test changed for " + s);
                toggleOKButton(dialog,llTextViews, newMatchesType);
            }
        };

        // add listener to enable disable OK button
        for(int g = 0; g < iNrOfGroups; g++) {
            List<TextView> lTextViews = llTextViews.get(g);
            for(TextView textView: lTextViews) {
                EditText editText = (EditText) textView;
                editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                    @Override public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        if (actionId == EditorInfo.IME_ACTION_NEXT){
                            View viewById = dialog.findViewById(v.getNextFocusDownId());
                            if ( viewById != null ) {
                                viewById.requestFocus();
                            }
                            return true;
                        }
                        return false;
                    }
                });

                // also required, toggle button not working from other method?
                editText.addTextChangedListener(txtWatcher);
            }
        }

        ScrollView sv = new ScrollView(context);
        sv.addView(llParent);
        dialog.setView(sv);
        dialog.show();
    }

    private void toggleOKButton(AlertDialog dialog, List<List<TextView>> llTextViews, NewMatchesType newMatchesType) {
        final Button btnOk = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        int iMinNonEmpty = ListUtil.size(llTextViews.get(0));
        int iMaxNonEmpty = 0;
        for(int g=0; g < ListUtil.size(llTextViews); g++) {
            int iNonEmpty = ViewUtil.countNonEmpty(llTextViews.get(g));
            iMinNonEmpty = Math.min(iMinNonEmpty, iNonEmpty);
            iMaxNonEmpty = Math.max(iMaxNonEmpty, iNonEmpty);
        }
        btnOk.setEnabled(iMinNonEmpty >= 2);

        if ( newMatchesType == null ) {
            btnOk.setText(R.string.cmd_ok);
        } else {
            switch (newMatchesType) {
                case TeamVsTeam_OneMatchPerPlayer:
                    btnOk.setText(getString(R.string.add_x_matches, iMinNonEmpty));
                    break;
                case TeamVsTeam_XMatchesPlayer:
                    btnOk.setText(getString(R.string.add_x_matches, iMinNonEmpty * iMaxNonEmpty));
                    break;
                case Poule:
                    int iNrOfNonEmpty = iMinNonEmpty;
                    if (iNrOfNonEmpty > 2) {
                        int iTotal = 0;
                        for (int i = 1; i < iNrOfNonEmpty; i++) {
                            iTotal += i;
                        }
                        btnOk.setText(getString(R.string.add_x_matches, iTotal));
                    } else {
                        btnOk.setText(R.string.cmd_ok);
                    }
                    break;
            }
        }
    }

    /** To rename or enter a new header */
    void editHeader(final String sHeader) {
        final boolean bIsEditExistingGroupName = StringUtil.isNotEmpty(sHeader);

        final LinearLayout ll = new LinearLayout(context);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setTag(ColorPrefs.Tags.item);
        ll.setLayoutParams(llpMargin1Weight1);

        final EditText txtName = new EditText(context);
        txtName.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        txtName.setText(sHeader.trim());
        txtName.setTag(ColorPrefs.Tags.item);
        txtName.setHint(R.string.name);
        ll.addView(txtName);

        final EditText txtNrOfPlayers = new EditText(context);
        txtNrOfPlayers.setInputType(InputType.TYPE_CLASS_NUMBER);
        //txtNrOfPlayers.setText(String.valueOf(PreferenceValues.maxNumberOfPlayersInGroup(context)));
        txtNrOfPlayers.setTag(ColorPrefs.Tags.item);
        txtNrOfPlayers.setHint(getString(R.string.pref_maxNumberOfPlayersInGroup) + " (" + PreferenceValues.maxNumberOfPlayersInGroup(context) + ")" );

        // allow to choose between 'poule' vs 'team vs team'
        final SelectEnumView sevType;
        if ( bIsEditExistingGroupName ) {
            sevType = null;
        } else {
            NewMatchesType newMatchesTypeDefault = PreferenceValues.getNewMatchesType(context);
            sevType = new SelectEnumView(context, NewMatchesType.class, newMatchesTypeDefault, 1);
            ll.addView(sevType);

            ll.addView(txtNrOfPlayers);
        }

        AlertDialog.Builder ab = ScoreBoard.getAlertDialogBuilder(context);
        ab.setMessage(bIsEditExistingGroupName ?R.string.cmd_edit:R.string.cmd_new_group)
                .setIcon(bIsEditExistingGroupName ? android.R.drawable.ic_menu_edit : R.drawable.circled_plus)
                .setPositiveButton(R.string.cmd_ok, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialogInterface, int i) {
                        String sNewHeader = txtName.getText().toString().trim();
                        if ( StringUtil.isNotEmpty(sNewHeader) ) {
                            List<String> lFixedMatches = PreferenceValues.getMatchList(context);
                            if ( bIsEditExistingGroupName ) {
                                String sCorrespondingConfiguredHeader = determineConfiguredHeader(sHeader, lFixedMatches, true);
                                List<String> lFixedValuesNew = ListUtil.translateValues(lFixedMatches, "\\Q" + sCorrespondingConfiguredHeader + "\\E", HEADER_PREFIX + sNewHeader);
                                storeAndRefresh(lFixedValuesNew);
                            } else {
                                NewMatchesType newMatchesType = null;
                                if ( sevType != null ) {
                                    newMatchesType = (NewMatchesType) sevType.getChecked();
                                }
                                if ( NewMatchesType.Poule.equals(newMatchesType) ) {
                                    lFixedMatches.add(0, HEADER_PREFIX + sNewHeader);
                                    storeAndRefresh(lFixedMatches);
                                }

                                // after adding a new group assume at least a match will be added
                                String sNrOfMatches = txtNrOfPlayers.getText().toString();
                                if ( StringUtil.isEmpty(sNrOfMatches) ) {
                                    sNrOfMatches = String.valueOf(PreferenceValues.maxNumberOfPlayersInGroup(context));
                                }
                                Integer iNrOfNamesToEnter = 4;
                                if ( StringUtil.isInteger(sNrOfMatches) ) {
                                    iNrOfNamesToEnter = Integer.valueOf(sNrOfMatches);
                                }
                                iNrOfNamesToEnter = Math.max(iNrOfNamesToEnter, 2);

                                PreferenceValues.setEnum(PreferenceKeys.NewMatchesType, context, newMatchesType);
                                PreferenceValues.setNumber(PreferenceKeys.maxNumberOfPlayersInGroup, context, iNrOfNamesToEnter);
                                editMatch(sNewHeader, "", false, newMatchesType, iNrOfNamesToEnter );
                            }
                        }
                    }
                })
                .setNegativeButton(R.string.cmd_cancel, null);
        final AlertDialog dialog = ab.create();
        ColorPrefs.setColor(ll);
        dialog.setView(ll);
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override public void onShow(DialogInterface dialogInterface) {
                txtName.requestFocus();
                ViewUtil.showKeyboard(activity.getWindow());
                //ViewUtil.showKeyboard(dialog.getWindow());

                final Button btnOk = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                btnOk.setEnabled(ViewUtil.areAllNonEmpty(txtName));
            }
        });
        TextWatcher txtWatcher = new BasicTextWatcher() {
            @Override public void afterTextChanged(Editable s) {
                final Button btnOk = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                btnOk.setEnabled(ViewUtil.areAllNonEmpty(txtName));
            }
        };
        txtName.addTextChangedListener(txtWatcher);
        dialog.show();
    }

    private boolean confirmDeleteHeader(final String sHeader) {

        AlertDialog.Builder ab = ScoreBoard.getAlertDialogBuilder(context);
        ab.setIcon          (android.R.drawable.ic_menu_delete)
          .setTitle         (getString(R.string.sb_delete_group_of_matches_confirm, sHeader) )
          .setNegativeButton(R.string.cmd_cancel, null)
          .setPositiveButton(R.string.cmd_delete, new DialogInterface.OnClickListener() {
              @Override public void onClick(DialogInterface dialog, int which) {
                  _deleteHeader(sHeader);
              }
          }).show();

        return true;
    }

/*
    private boolean confirmDeleteMatch(final String sHeader, final String sMatch) {

        AlertDialog.Builder ab = ScoreBoard.getAlertDialogBuilder(activity);
        ab.setIcon          (android.R.drawable.ic_menu_delete)
          .setTitle         (getString(R.string.sb_remove_match_from_my_list, sMatch) )
          .setNegativeButton(R.string.cmd_keep, new DialogInterface.OnClickListener() {
              @Override public void onClick(DialogInterface dialog, int which) {
                  activity.finish();
              }
          })
          .setPositiveButton(R.string.cmd_delete, new DialogInterface.OnClickListener() {
              @Override public void onClick(DialogInterface dialog, int which) {
                  _replaceMatch(sHeader, sMatch, null);
                  activity.finish();
              }
          }).show();

        return true;
    }
*/

    private void _deleteHeader(String sHeader) {
        List<String> lFixedMatches = PreferenceValues.getMatchList(context);
        String sCorrespondingConfiguredHeader = determineConfiguredHeader(sHeader, lFixedMatches, true);

        List<String> lFixedNew = new ArrayList<String>();
        boolean bInConfiguredSection = sHeader.contains(getString(R.string.lbl_fixed));
        for(String s: lFixedMatches ) {
            if ( s.startsWith(HEADER_PREFIX) ) {
                bInConfiguredSection = s.equals(sCorrespondingConfiguredHeader);
            }

            if ( bInConfiguredSection ) { continue; }
            lFixedNew.add(s);
        }
        storeAndRefresh(lFixedNew);
    }

    void sortHeaders(Context context) {
        List<String> lFixedMatches = PreferenceValues.getMatchList(context);
                     lFixedMatches = ListUtil.replace(lFixedMatches, "^" + ExpandableMatchSelector.HEADER_PREFIX + "\\s+", ExpandableMatchSelector.HEADER_PREFIX); // remove optional spaces before the actual name of the header
        List<String> lHeaders      = ListUtil.filter (lFixedMatches, "^" + ExpandableMatchSelector.HEADER_PREFIX + ".*", Enums.Match.Keep);
        Collections.sort(lHeaders);

        List<String> lFixedMatchesNew = new ArrayList<String>();
        for(String sHeader: lHeaders) {
            List<String> lChilds = new ArrayList<String>();
            boolean bInConfiguredSection = false;
            for(String s: lFixedMatches ) {
                if ( s.startsWith(ExpandableMatchSelector.HEADER_PREFIX) ) {
                    bInConfiguredSection = s.equals(sHeader);
                }

                if ( bInConfiguredSection ) {
                    lChilds.add(s);
                }
            }
            lFixedMatchesNew.add(sHeader);
            Collections.sort(lChilds);
            lFixedMatchesNew.addAll(lChilds);
        }
        storeAndRefresh(lFixedMatchesNew);
    }

    /** for adding, replacing and deleting a match */
    public void _replaceMatch(String sHeader, String sMatchOld, String sMatchNew) {
        List<String> lFixedMatches = PreferenceValues.getMatchList(context);
        String sCorrespondingConfiguredHeader = determineConfiguredHeader(sHeader, lFixedMatches, true);

        List<String> lFixedNew = new ArrayList<String>();
        boolean bInConfiguredSection = false;
        for(String s: lFixedMatches ) {
            if ( bInConfiguredSection && s.startsWith(HEADER_PREFIX) ) {
                // moving out of the configured 'section'
                if ( StringUtil.isEmpty(sMatchOld) ) {
                    // add new match to the end
                    lFixedNew.add(sMatchNew);
                }
            }
            if ( s.startsWith(HEADER_PREFIX) ) {
                bInConfiguredSection = s.equals(sCorrespondingConfiguredHeader);
                lFixedNew.add(s);
                continue;
            }

            if ( bInConfiguredSection ) {
                if ( s.equals(sMatchOld) ) {
                    if ( StringUtil.isNotEmpty(sMatchNew) ) {
                        lFixedNew.add(sMatchNew);
                    }
                    continue;
                }
            }
            lFixedNew.add(s);
        }

        // add it to the end if match was not added
        if ( lFixedNew.contains(sMatchNew) == false ) {
            lFixedNew.add(sMatchNew);
        }
        storeAndRefresh(lFixedNew);
    }

/*
    private void addGroupWithMatches(final String sHeader, List<String> lMatches) {
        List<String> lFixedMatches = PreferenceValues.getMatchList(context);

        String sHeaderToAdd = sHeader;
        String sCorrespondingConfiguredHeader = determineConfiguredHeader(sHeaderToAdd, lFixedMatches);
        int i = 0;
        while ( sCorrespondingConfiguredHeader != null ) {
            sHeaderToAdd = sHeader + " " + (++i);
            sCorrespondingConfiguredHeader = determineConfiguredHeader(sHeaderToAdd, lFixedMatches);
        }

        List<String> lFixedNew = new ArrayList<String>();
        lFixedNew.addAll(lFixedMatches);

        lFixedNew.add(sHeaderToAdd);
        lFixedNew.addAll(lMatches);

        storeAndRefresh(lFixedNew);
    }
*/

    public void storeAndRefresh(List<String> lFixedNew) {
        store(lFixedNew);

        emsAdapter.load(false);
    }

    private void store(List<String> lFixedNew) {
        String sNew = ListUtil.join(lFixedNew, "\n").trim();
        PreferenceValues.setString(PreferenceKeys.matchList, context, sNew);
        Log.d(TAG, "Stored as new matchlist: " + sNew);
    }

    @Override public ExpandableListView.OnChildClickListener getOnChildClickListener() {
        return new ExpandableListView.OnChildClickListener() {
            @Override public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                String sGroup = (String) getListAdapter(null).getGroup(groupPosition);
                String sText = SimpleELAdapter.getText(v);
                return startMatch(sGroup, sText);
            }
        };
    }

    public String determineConfiguredHeader(String sHeader, List<String> lFixedMatches, boolean bReturnIfOnlyOneHeaderConfigured) {
        if ( ListUtil.isEmpty(lFixedMatches) ) {
            lFixedMatches = PreferenceValues.getMatchList(context);
        }
        if ( ListUtil.isEmpty(lFixedMatches) ) { return null; }

        List<String> lHeaders = ListUtil.filter(lFixedMatches, "^" + HEADER_PREFIX + ".*", Enums.Match.Keep);
        if ( (ListUtil.size(lHeaders)) == 1 && bReturnIfOnlyOneHeaderConfigured ) {
            return lHeaders.get(0).trim();
        }
        String sRegExp = "^\\Q" + HEADER_PREFIX + "\\E\\s*\\Q" + sHeader.trim() + "\\E$";
        //Pattern p = Pattern.compile(sRegExp);
        for(String sConfiguredHeader: lHeaders) {
            if ( sConfiguredHeader.trim().matches(sRegExp) ) {
                return sConfiguredHeader;
            }
        }
        return null;
    }

    private boolean startMatch(String sGroup, final String sText) {
        Intent intent = new Intent();

        File fCorrespondingRecent = (File) emsAdapter.getObject(sGroup, sText);
        if ( fCorrespondingRecent != null ) {
            intent.putExtra(PreviousMatchSelector.class.getSimpleName(), fCorrespondingRecent);
            activity.setResult(Activity.RESULT_OK, intent);
            activity.finish();
            return true;
        }

        String   sPlayers  = sText.replaceAll("^[^A-Z]*", ""); // remove time/date/day and numbers from start of string (until first capital)
        String[] saPlayers = sPlayers.split("\\Q" + NAMES_SPLITTER + "\\E");
        if ( saPlayers.length < 2 ) {
            saPlayers = sPlayers.split("\\s*-\\s*");
        }
        if ( saPlayers.length < 2 ) {
            activity.finish();
            return false;
        }

        //intent.putExtra(MatchDetails.class.getSimpleName(), getBundle(sGroup, sText, saPlayers)); // this is read by ScoreBoard.onActivityResult
        Model  model = getModel(sGroup, sText, saPlayers);
        String sJson = model.toJsonString(null);
        intent.putExtra(Model.class.getSimpleName(), sJson); // this is read by ScoreBoard.onActivityResult
        activity.setResult(Activity.RESULT_OK, intent);
        activity.finish();
        return true;
    }

    private Model getModel(String sGroup, String sText, String[] saPlayers) {
        Model m = ModelFactory.getTemp();

        FeedMatchSelector.getMatchDetailsFromMatchString(m, sText, context, false);
        if ( m.isDirty() ) {
            //m.setPlayerName   (Player.A, mDetails.get(MatchDetails.PlayerA));
            //m.setPlayerName   (Player.B, mDetails.get(MatchDetails.PlayerB));
            //m.setPlayerCountry(Player.A, mDetails.get(MatchDetails.CountryA));
            //m.setPlayerCountry(Player.B, mDetails.get(MatchDetails.CountryB));
        } else {
            m.setPlayerName (Player.A, saPlayers[0].trim());
            m.setPlayerName (Player.B, saPlayers[1].trim());
        }

        StaticMatchSelector.setMatchIsFrom(m);

        // use feed name and group name for event details
        if ( PreferenceValues.useGroupNameAsEventData(context) ) {
            String[] sSplitChars = { ">", ":", "-" };
            String[] saEvent = sGroup.split(sSplitChars[0]);
            for(String sSplitChar: sSplitChars) {
                String[] saEventTmp = sGroup.split(sSplitChar);
                if ( (saEventTmp.length > saEvent.length) && (saEvent.length < 2) ) {
                    saEvent = saEventTmp;
                }
            }
            if ( saEvent.length >= 2) {
                String sEventName     = saEvent[0].trim();
                String sEventRound    = null;
                String sEventDivision = null;
                if ( saEvent.length == 2 ) {
                    if (appearsToBeARound(saEvent[1])) {
                        sEventRound    = saEvent[1];
                    } else {
                        sEventDivision = saEvent[1].trim();
                    }
                } else {
                    sEventDivision = saEvent[1].trim();
                    sEventRound    = saEvent[2].trim();
                }
                m.setEvent(sEventName, sEventDivision, sEventRound, null);
            }
        }

        if ( MapUtil.isNotEmpty(mHeadersWithRecentMatches) && mHeadersWithRecentMatches.containsKey(sGroup) ) {
            // use the format
            Object oFiles = mHeadersWithRecentMatches.get(sGroup);
            Collection<File> files = null;
            if ( oFiles instanceof Collection ) {
                files = (Collection<File>) oFiles;
            } else {
                files = new ArrayList<>();
                files.add((File) oFiles);
            }
            File f = files.iterator().next();
            Model mTmp = Brand.getModel();
            try {
                mTmp.fromJsonString(f);
                m.setNrOfPointsToWinGame(mTmp.getNrOfPointsToWinGame());
                m.setNrOfGamesToWinMatch(mTmp.getNrOfGamesToWinMatch());
                m.setTiebreakFormat(mTmp.getTiebreakFormat());
                m.setEnglishScoring(mTmp.isEnglishScoring());

                m.setPlayerClub(Player.A, mTmp.getClub(Player.A));
                m.setPlayerClub(Player.B, mTmp.getClub(Player.B));
                m.setEvent     (mTmp.getEventName(), mTmp.getEventDivision(), mTmp.getEventRound(), mTmp.getEventLocation());

                // all relevant match format properties have been set (taken from other match): no need to present activity
                Match.dontShow();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return m;
    }
/*
    private Bundle getBundle(String sGroup, String sText, String[] saPlayers) {
        Bundle bundle = new Bundle();

        Map<MatchDetails, String> mDetails = FeedMatchSelector.getMatchDetailsFromMatchString(sText, activity);
        if ( MapUtil.isNotEmpty(mDetails) ) {
            bundle.putString (MatchDetails.PlayerA .toString(), mDetails.get(MatchDetails.PlayerA));
            bundle.putString (MatchDetails.PlayerB .toString(), mDetails.get(MatchDetails.PlayerB));
            bundle.putString (MatchDetails.CountryA.toString(), mDetails.get(MatchDetails.CountryA));
            bundle.putString (MatchDetails.CountryB.toString(), mDetails.get(MatchDetails.CountryB));
        } else {
            bundle.putString (MatchDetails.PlayerA.toString(), saPlayers[0].trim());
            bundle.putString (MatchDetails.PlayerB.toString(), saPlayers[1].trim());
        }

        bundle.putString (MatchDetails.FeedKey.toString(), StaticMatchSelector.class.getSimpleName());

        // use feed name and group name for event details
        if ( PreferenceValues.useGroupNameAsEventData(activity) ) {
            String[] sSplitChars = { ">", ":", "-" };
            String[] saEvent = sGroup.split(sSplitChars[0]);
            for(String sSplitChar: sSplitChars) {
                String[] saEventTmp = sGroup.split(sSplitChar);
                if ( (saEventTmp.length > saEvent.length) && (saEvent.length < 2) ) {
                    saEvent = saEventTmp;
                }
            }
            if ( saEvent.length >= 2) {
                bundle.putString(MatchDetails.EventName .toString(), saEvent[0].trim());
                if ( saEvent.length == 2 ) {
                    MatchDetails roundOrDivision = MatchDetails.EventDivision;
                    if (appearsToBeARound(saEvent[1])) {
                        roundOrDivision = MatchDetails.EventRound;
                    }
                    bundle.putString(roundOrDivision.toString(), saEvent[1].trim());
                } else {
                    bundle.putString(MatchDetails.EventDivision.toString(), saEvent[1].trim());
                    bundle.putString(MatchDetails.EventRound   .toString(), saEvent[2].trim());
                }
            }
        }
        if ( MapUtil.isNotEmpty(mHeadersWithRecentMatches) && mHeadersWithRecentMatches.containsKey(sGroup) ) {
            // use the format
            File f = mHeadersWithRecentMatches.get(sGroup).iterator().next();
            Model mTmp = new Model();
            try {
                mTmp.fromJsonString(f);
                bundle.putInt    (MatchDetails.NrOfPointsToWinGame.toString(), mTmp.getNrOfPointsToWinGame());
                bundle.putInt    (MatchDetails.NrOfGamesToWinMatch.toString(), mTmp.getNrOfGamesToWinMatch());
                bundle.putString (MatchDetails.TiebreakFormat     .toString(), mTmp.getTiebreakFormat().toString());
                bundle.putBoolean(MatchDetails.UseEnglishScoring  .toString(), mTmp.isEnglishScoring());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return bundle;
    }
*/

    @Override public AdapterView.OnItemLongClickListener getOnItemLongClickListener() {
        return null;
    }

    private Map<String, Object> mHeadersWithRecentMatches = null;
    private EMSAdapter emsAdapter;

    @Override public SimpleELAdapter getListAdapter(LayoutInflater inflater) {
        if ( emsAdapter == null ) {
            emsAdapter = new EMSAdapter(inflater);
        }
        return emsAdapter;
    }

    private class EMSAdapter extends SimpleELAdapter
    {
        private EMSAdapter(LayoutInflater inflater)
        {
            super(inflater, R.layout.expandable_match_selector_group_with_menu, R.layout.expandable_match_selector_item_with_menu, null, bAutoLoad);
        }

        public void load(boolean bUseCacheIfPresent) {
            this.clear();

            Feature continueRecentMatch = PreferenceValues.continueRecentMatch(context);
            mHeadersWithRecentMatches = null;
            Map<String, File> lastFewHoursMatches = null;
            if ( continueRecentMatch != Feature.DoNotUse ) {
                // retrieve matches modified recently top optionally enrich the 'my matches' list with details of these 'in progress' matches
                lastFewHoursMatches = PreviousMatchSelector.getLastFewHoursMatchesAsMap(context, 3 /* TODO: preference */);
                mHeadersWithRecentMatches = new HashMap<String, Object>();
            }

            List<String> lFixedMatches = PreferenceValues.getMatchList(context);

            // remove headers with no matches
            if ( ListUtil.isNotEmpty(lFixedMatches) ) {
                boolean bHeadersRemoved = false;
                boolean bPreviousWasHeader = true;
                for (int i = ListUtil.size(lFixedMatches)-1; i >= 0; i-- ) {
                    String sItem = lFixedMatches.get(i);
                    boolean bIsHeader = sItem.startsWith(ExpandableMatchSelector.HEADER_PREFIX);
                    if ( bIsHeader && bPreviousWasHeader ) {
                        // header with no matches underneath
                        lFixedMatches.remove(i);
                        bHeadersRemoved = true;
                    }
                    bPreviousWasHeader = bIsHeader;
                }
                if ( bHeadersRemoved ) {
                    store(lFixedMatches);
                }
            }

          //Log.d(TAG, "read list of matches: \n" + ListUtil.toNice(lFixedMatches, false));
            if ( ListUtil.isNotEmpty(lFixedMatches) ) {
                String sHeader = context.getString(R.string.lbl_fixed); // for if configured/stored text does not contain a header as the first line
                Model mRecycle = ModelFactory.getTemp();
                for ( String sMatch: lFixedMatches ) {
                    if ( StringUtil.isEmpty(sMatch) ) {
                        continue;
                    }
                    if ( sMatch.startsWith(HEADER_PREFIX) ) {
                        sHeader = sMatch.replaceFirst(HEADER_PREFIX, "").trim();
                        continue;
                    }

                    File fCorrespondingRecentMatch = null;

                    // try to add result of recent match if present
                    if ( MapUtil.isNotEmpty(lastFewHoursMatches) && (ScoreBoard.matchModel != null) ) {
                        String sKeyCurrent = PreviousMatchSelector.getKeyFromNames(ScoreBoard.matchModel.getName(Player.A),ScoreBoard.matchModel.getName(Player.B));
                        FeedMatchSelector.getMatchDetailsFromMatchString(mRecycle, sMatch, context, false);
                      //if ( MapUtil.isNotEmpty(mDetails) ) {
                            String sKey = PreviousMatchSelector.getKeyFromNames(mRecycle.getName(Player.A),mRecycle.getName(Player.B));
                            if ( StringUtil.isEmpty(sKey) ) { continue; }
                            File fMatch = lastFewHoursMatches.get(sKey);
                            if ( fMatch != null ) {
                                fCorrespondingRecentMatch = fMatch;
                                Model mTmp = Brand.getModel();
                                try {
                                    mTmp.fromJsonString(fMatch);
                                    if ( mTmp.hasStarted() ) {
                                        String sResult = mTmp.getGameScores();
                                        if (StringUtil.size(sResult) > 15) {
                                            // to many games already played to display full details... fall back to nr of games only
                                            sResult = mTmp.getResult();
                                        }
                                        if (StringUtil.isNotEmpty(sResult)) {
                                            sMatch += " : " + sResult;
                                            String sTime = DateUtil.formatDate2String(fMatch.lastModified(), DateUtil.HHMM_COLON); // mTmp.getMatchStartTimeHH_Colon_MM();
                                            sMatch = sTime + " : " + sMatch;
                                        }
                                        MapUtil.addToList(mHeadersWithRecentMatches, sHeader, fMatch, true);
                                    } else {
                                        fCorrespondingRecentMatch = null;
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                            if ( (sKeyCurrent != null) && sKeyCurrent.equals(sKey) ) {
                                sMatch = "* " + sMatch;
                            }
                      //}
                    }
                    super.addItem(sHeader, sMatch, fCorrespondingRecentMatch);
                }

                if ( PreferenceValues.getFixedMatchesAreUnChanged() ) {
                    showTip();
                }
            } else {
                showTip();
            }
            this.notifyDataSetChanged();

            hideProgress(); // just to be sure, for if swipe is used

            setGuiDefaults(null);
        }

        @Override protected void doPostInflateItem(final View v, final String sHeader, String sText) {
            ImageButton mImageButtonOverflow = (ImageButton) v.findViewById(R.id.card_item_button_overflow);
            if ( mImageButtonOverflow == null ) { return; }

            final PopupMenu mPopupMenu = new PopupMenu(context, mImageButtonOverflow);
            MenuInflater inflater = mPopupMenu.getMenuInflater();
            inflater.inflate(R.menu.static_match_item_menu, mPopupMenu.getMenu());

            // listener to show popup
            mImageButtonOverflow.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    mPopupMenu.show(); // v is the imagebutton
                }
            });

            // listener to menu items clicks of the popup
            mPopupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override public boolean onMenuItemClick(MenuItem item) {
                    TextView txtMatch = ViewUtil.getFirstView(v, TextView.class);
                    String sMatch = txtMatch.getText().toString();
                    //String msg = "Menu item '" + item.getTitle() + "' clicked for '" + sMatch + "' in '" + sHeader + "'";
                    //Log.d(TAG, msg);
                    //Toast.makeText(StaticMatchSelector.this, msg, Toast.LENGTH_SHORT).show();
                    switch (item.getItemId()) {
                        case R.id.smi_item_select:
                            startMatch(sHeader, sMatch);
                            break;
                        case R.id.smi_item_delete:
                            _replaceMatch(sHeader, sMatch, null);
                            break;
                        case R.id.smi_item_edit:
                            editMatch(sHeader, sMatch, true, null, 2);
                            break;
                        case R.id.smi_item_copy:
                            editMatch(sHeader, sMatch, false, null, 2);
                            break;
                    }
                    return false;
                }
            });
        }

        @Override protected void doPostInflateGroup(final View v, final String sHeader) {
/*
            View btnEditGroup = v.findViewById(R.id.cmd_edit_group);
            if ( btnEditGroup != null )
            btnEditGroup.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    editHeader(sHeader);
                }
            });
            View btnDeleteGroup = v.findViewById(R.id.cmd_delete_group);
            if ( btnDeleteGroup != null )
            btnDeleteGroup.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    confirmDeleteHeader(sHeader);
                }
            });
            View btnAddItem = v.findViewById(R.id.cmd_add_item);
            if ( btnAddItem != null )
            btnAddItem.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    editMatch(sHeader, "", false);
                }
            });
*/

            ImageButton mImageButtonOverflow = (ImageButton) v.findViewById(R.id.card_header_button_overflow);
            if ( mImageButtonOverflow == null ) { return; }

            final PopupMenu mPopupMenu = new PopupMenu(context, mImageButtonOverflow);
            MenuInflater inflater = mPopupMenu.getMenuInflater();
            inflater.inflate(R.menu.static_match_group_menu, mPopupMenu.getMenu());

            // listener to show popup
            mImageButtonOverflow.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    mPopupMenu.show(); // v is the imagebutton
                }
            });

            // listener to menu items clicks of the popup
            mPopupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override public boolean onMenuItemClick(MenuItem item) {
                    String msg = "Menu item '" + item.getTitle() + "' clicked for '" + sHeader + "'";
                    Log.d(TAG, msg);
                    //Toast.makeText(scoreBoard, msg, Toast.LENGTH_SHORT).show();
                    switch ( item.getItemId() ) {
                        case R.id.smi_group_delete:
                            confirmDeleteHeader(sHeader);
                            break;
                        case R.id.smi_group_add_match:
                            editMatch(sHeader, "", false, null, 2);
                            break;
                        case R.id.smi_group_edit:
                            editHeader(sHeader);
                            break;
                    }
                    return false;
                }
            });
        }
    }

    private void showTip() {
        String sIn1 = context.getString(R.string.pref_matchList);
        String sIn2 = context.getString(R.string.settings);
        PreferenceValues.showTip(context, PreferenceKeys.matchList, context.getString(R.string.pref_matchList_not_set, sIn1, sIn2), false);
    }
}