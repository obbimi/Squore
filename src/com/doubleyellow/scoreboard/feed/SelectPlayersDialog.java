/*
 * Copyright (C) 2019  Iddo Hoeve
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

package com.doubleyellow.scoreboard.feed;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.doubleyellow.android.view.EnumSpinner;
import com.doubleyellow.android.view.SelectEnumView;
import com.doubleyellow.android.view.SelectObjectView;
import com.doubleyellow.android.view.ViewUtil;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.dialog.BaseAlertDialog;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.model.JSONKey;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.model.Player;
import com.doubleyellow.scoreboard.prefs.NamePart;
import com.doubleyellow.scoreboard.prefs.URLsKeys;
import com.doubleyellow.util.HVD;
import com.doubleyellow.util.JsonUtil;
import com.doubleyellow.util.ListUtil;
import com.doubleyellow.util.Placeholder;
import com.doubleyellow.util.StringUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dialog where user of the app can select a player of each team.
 */
public class SelectPlayersDialog extends BaseAlertDialog
{
    protected static final String TAG = "SB." + SelectPlayersDialog.class.getSimpleName();

    SelectPlayersDialog(FeedMatchSelector feedMatchSelector, Context context, Model matchModel, JSONObject joFeedConfig) {
        super(context, matchModel, null);
        m_joFeedConfig      = joFeedConfig;
        m_feedMatchSelector = feedMatchSelector;
    }

    @Override public boolean storeState(Bundle outState) {
        return false;
    }

    @Override public boolean init(Bundle outState) {
        return false;
    }

    private JSONObject        m_joFeedConfig      = null;
    private FeedMatchSelector m_feedMatchSelector = null;
    private OKButtonActionForMandatoryPostParam m_eOKButtonAction = OKButtonActionForMandatoryPostParam.Disable;

    private static int m_iViewIdOfButtonInSelectPlayersDialog = 0;
    public enum OKButtonActionForMandatoryPostParam {
        Disable,
        EnableWithValidation,
    }

    @Override public void show() {
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
        layoutParams.weight = 1;

        boolean bAllowSplitOnComma = true;

        // assume user must select player for each team and that list of players is stored in the model
        final Map<Player, SelectObjectView<String>> m_p2select = new HashMap<>();
        LinearLayout llTeams = new LinearLayout(context);
        llTeams.setOrientation(LinearLayout.HORIZONTAL);

        String sTeamPlayer_Format      = "%1$s. %3$s (%2$s)";
        String sTeamPlayer_Placeholder = "${" + JSONKey.seqNo + "}. ${" + JSONKey.name + "} (${" + JSONKey.id + "})";
        int iTextSizePercentage = 4;
        if ( ViewUtil.isLandscapeOrientation(context) ) {
            iTextSizePercentage = 3;
        }
        if ( m_joFeedConfig != null ) {
            sTeamPlayer_Format      = m_joFeedConfig.optString(URLsKeys.Format_TeamPlayer     .toString(), sTeamPlayer_Format);
            sTeamPlayer_Placeholder = m_joFeedConfig.optString(URLsKeys.Placeholder_TeamPlayer.toString(), sTeamPlayer_Placeholder);
            iTextSizePercentage     = m_joFeedConfig.optInt   (URLsKeys.TextSizePercentage    .toString(), iTextSizePercentage);
        }
        int iTextSizeInPx = ViewUtil.getScreenWidth(context) / 100 * iTextSizePercentage;


        SelectObjectView.RadioButtonDecorator radioButtonDecorator = null;
        SelectObjectView msPlayers = null;
        for ( Player p: Player.values() ) {
            LinearLayout llTeam = new LinearLayout(context);
            llTeam.setOrientation(LinearLayout.VERTICAL);
            llTeam.setLayoutParams(layoutParams);

            TextView txtTeam = new TextView(context);
            txtTeam.setText(matchModel.getClub(p));
            txtTeam.setTextSize(TypedValue.COMPLEX_UNIT_PX, iTextSizeInPx);
            llTeam.addView(txtTeam);

            // create view where user can select player from
            JSONArray aTeamPlayers = FeedMatchSelector.getTeamPlayers(context, p);
            if ( JsonUtil.isNotEmpty(aTeamPlayers) && aTeamPlayers.opt(0) instanceof JSONObject) {
                // each player specified with a smal json object
                List<Map> teamPlayers = JsonUtil.toListOfMaps(aTeamPlayers);
                Map mChecked = teamPlayers.get(0);
                if ( radioButtonDecorator == null ) {
                    radioButtonDecorator = new TeamPlayerRBJsonDecorator(sTeamPlayer_Placeholder, Placeholder.getInstance(TAG));
                }

                msPlayers = new SelectObjectView<Map>(context, teamPlayers, mChecked, radioButtonDecorator);

                JSONObject firstPlayer = (JSONObject) aTeamPlayers.opt(0);
                bAllowSplitOnComma = ( firstPlayer.has(JSONKey.firstName.toString()) && firstPlayer.has      (JSONKey.lastName.toString()) )
                        || (firstPlayer.has(JSONKey.name     .toString()) && firstPlayer.optString(JSONKey.name    .toString()).contains(",") );
            } else {
                // each player specified with ':' separated data
                List<String> teamPlayers = JsonUtil.asListOfStrings(aTeamPlayers);
                String sChecked = ListUtil.isNotEmpty(teamPlayers) ? teamPlayers.get(0): null;
                if ( StringUtil.isNotEmpty(sChecked) && (radioButtonDecorator == null) && sChecked.contains(":") ) {
                    radioButtonDecorator = new TeamPlayerRBDecorator(sTeamPlayer_Format, ":");
                }

                msPlayers = new SelectObjectView<String>(context, teamPlayers, sChecked, radioButtonDecorator);

                bAllowSplitOnComma = bAllowSplitOnComma && sChecked.contains(",");
            }
            msPlayers.setTextSize(TypedValue.COMPLEX_UNIT_PX, iTextSizeInPx);
            llTeam.addView(msPlayers);
            m_p2select.put(p, msPlayers);

            llTeams.addView(llTeam);
        }

        LinearLayout llRoot = new LinearLayout(context);
        llRoot.setOrientation(LinearLayout.VERTICAL);

        OKButtonActionForMandatoryPostParam eOKButtonAction = null;
        final View[] vaPostParams;
        if ( (m_joFeedConfig != null) && m_joFeedConfig.has(URLsKeys.AdditionalPostParams.toString()) ) {
            JSONArray  additionalPostParams = m_joFeedConfig.optJSONArray(URLsKeys.AdditionalPostParams.toString());
            int iNrOfAdditionalPostParams = additionalPostParams.length();
            vaPostParams = new View[iNrOfAdditionalPostParams];
            for (int p = 0; p < iNrOfAdditionalPostParams; p++) {
                JSONObject   joPostParam = additionalPostParams.optJSONObject(p);
                JSONArray    ppValues    = joPostParam.optJSONArray(URLsKeys.AllowedValues.toString());
                String       sDefaultVal = joPostParam.optString   (URLsKeys.DefaultValue .toString()); // no default only works for radio button for now
                boolean      bOptional   = joPostParam.optBoolean  (URLsKeys.Optional     .toString());
                String       ppCaption   = joPostParam.optString   (URLsKeys.Caption      .toString());
                String       ppType      = joPostParam.optString   (URLsKeys.DisplayType  .toString(), "RadioButton");
                String       ppPostAs    = joPostParam.optString   (URLsKeys.PostAs       .toString(), ppCaption.toLowerCase().replaceAll(" ", ""));
                List<String> lValues     = JsonUtil.asListOfStrings(ppValues);

                if ( "SelectList".equalsIgnoreCase(ppType) ) {
                    // select list
                    Spinner spinner = new Spinner(context);
                    if ( StringUtil.isEmpty(sDefaultVal) ) {
                        lValues.add(0, ""); // add empty first value, to enforce user to select another value
                        if ( bOptional == false ) {
                            eOKButtonAction = m_eOKButtonAction;
                            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                @Override public void onItemSelected(AdapterView<?> parent, View spinner, int iPositionZB, long id) {
                                    Log.d(TAG, "Spinner value selected " + iPositionZB);
                                    View v = spinner.getRootView();
                                    if ( m_iViewIdOfButtonInSelectPlayersDialog != 0 ) {
                                        View viewById = v.findViewById(m_iViewIdOfButtonInSelectPlayersDialog);
                                        if ( viewById != null ) {
                                            viewById.setEnabled(iPositionZB > 0); // still disable if first empty value is re-selected
                                        }
                                    }
                                }
                                @Override public void onNothingSelected(AdapterView<?> parent) { }
                            });
                        }
                    }
                    ArrayAdapter<String> dataAdapter = EnumSpinner.getStringArrayAdapter(context, lValues, iTextSizeInPx, null);
                    spinner.setAdapter(dataAdapter);
                    vaPostParams[p] = spinner;
                } else if ( "RadioButton".equalsIgnoreCase(ppType) ) {
                    // radio button
                    SelectObjectView<String> selectObjectView = new SelectObjectView<>(context, lValues, sDefaultVal, null, HVD.Horizontal);
                    selectObjectView.setTextSize(TypedValue.COMPLEX_UNIT_PX, iTextSizeInPx);
                    vaPostParams[p] = selectObjectView;
                    if ( StringUtil.isEmpty(sDefaultVal) && (bOptional == false) ) {
                        // ensure OK button is disabled until value is selected
                        eOKButtonAction = m_eOKButtonAction;

                        // ensure OK button is enabled when value is selected
                        selectObjectView.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                            @Override public void onCheckedChanged(RadioGroup group, int checkedIdZB) {
                                Log.d(TAG, "Value selected " + checkedIdZB);
                                View v = group.getRootView();
                                if ( m_iViewIdOfButtonInSelectPlayersDialog != 0 ) {
                                    View viewById = v.findViewById(m_iViewIdOfButtonInSelectPlayersDialog);
                                    if ( viewById != null ) {
                                        viewById.setEnabled(true);
                                    }
                                }
                            }
                        });
                    }
                } else if ( "ToggleButton".equalsIgnoreCase(ppType) ) {
                    // toggle button
                    ToggleButton toggleButton = new ToggleButton(context);
                    if ( ListUtil.size(lValues) == 2 ) {
                        toggleButton.setTextOff(lValues.get(0)); // default setting is off
                        toggleButton.setTextOn (lValues.get(1));
                        toggleButton.setText(lValues.get(0));
                    }
                    vaPostParams[p] = toggleButton;
                }

                TextView txtCaption = new TextView(context);
                txtCaption.setText(ppCaption);
                txtCaption.setTextSize(TypedValue.COMPLEX_UNIT_PX, iTextSizeInPx);

                LinearLayout llPostParam = new LinearLayout(context);
                llPostParam.setOrientation(LinearLayout.HORIZONTAL); // caption and view

                llPostParam.addView(txtCaption);
                if ( vaPostParams[p] != null ) {
                    vaPostParams[p].setTag(ppPostAs);
                    vaPostParams[p].setTag(R.string.please_select_a_value_for_x, ppCaption);
                    llPostParam.addView(vaPostParams[p]);
                }

                llRoot.addView(llPostParam);
            }
        } else {
            vaPostParams = null;
        }

        final SelectEnumView<NamePart> evNamePart;
        if ( bAllowSplitOnComma ) {
            evNamePart = new SelectEnumView(context, NamePart.class, NamePart.Full, 3);
            llRoot.addView(evNamePart);
        } else {
            evNamePart = null;
        }

        // add the teams last because it may be a long list
        llRoot.addView(llTeams);

        AlertDialog.Builder ab = ScoreBoard.getAlertDialogBuilder(context);
        ab.setTitle(R.string.sb_choose_players)
                .setView(llRoot)
                .setNeutralButton (R.string.cmd_cancel, null)
                .setPositiveButton(R.string.cmd_ok, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        int iPlayersSelected = 0;
                        for(Player p: Player.values()) {
                            SelectObjectView selectObjectView = m_p2select.get(p);
                            Object oData = selectObjectView.getChecked();
                            String sName = null;
                            String sId   = null;
                            if ( oData instanceof String ) {
                                sName = (String) oData;
                                String[] split = sName.split(":");
                                if ( split.length == 3 ) {
                                    String sSeqNo = split[0];
                                    sId           = split[1];
                                    sName         = split[2];
                                } else if ( split.length == 2 ) {
                                    sId           = split[0];
                                    sName         = split[1];
                                }
                            } else if (oData instanceof Map ) {
                                Map mData = (Map) oData;
                                sName = (String) mData.get(JSONKey.name.toString());
                                sId   = (String) mData.get(JSONKey.id.toString());
                                if ( StringUtil.isEmpty(sName) ) {
                                    sName = mData.get(JSONKey.lastName.toString()) + ", " + mData.get(JSONKey.firstName.toString());
                                }
                            }
                            if ( evNamePart != null ) {
                                NamePart namePart = evNamePart.getChecked();
                                switch (namePart) {
                                    case First:
                                        sName = sName.replaceFirst("^.*,\\s*", "");
                                        break;
                                    case Last:
                                        sName = sName.replaceFirst("\\s*,.*$", "");
                                        break;
                                }
                            }
                            matchModel.setPlayerName(p, sName);
                            matchModel.setPlayerId  (p, sId);
                            if ( StringUtil.isNotEmpty(sName) ) {
                                iPlayersSelected++;
                            }
                        }
                        if ( vaPostParams != null ) {
                            final String sSplitter = "|";
                            StringBuffer sbAdditionalPP = new StringBuffer();
                            for(int p=0; p < vaPostParams.length; p++) {
                                View v = vaPostParams[p];
                                String sPostAs = String.valueOf(v.getTag());
                                String sValue = null;
                                if ( v instanceof Spinner ) {
                                    Spinner sp = (Spinner) v;
                                    Object selectedItem = sp.getSelectedItem();
                                    if ( selectedItem != null ) {
                                        sValue = String.valueOf(selectedItem);
                                    } else {
                                        sValue = null;
                                    }
                                } else if ( v instanceof ToggleButton ) {
                                    ToggleButton tb = (ToggleButton) v;
                                    sValue = tb.getText().toString();
                                } else if ( v instanceof SelectObjectView ) {
                                    SelectObjectView<String> sov = (SelectObjectView<String>) v;
                                    sValue = sov.getChecked(); // may be null, not an issue
                                }
                                if ( StringUtil.isNotEmpty(sValue) ) {
                                    if ( sbAdditionalPP.length() != 0 ) {
                                        sbAdditionalPP.append(sSplitter);
                                    }
                                    sbAdditionalPP.append(sPostAs).append("=").append(sValue);
                                }
                            }
                            matchModel.setAdditionalPostParams(sSplitter + sbAdditionalPP + sSplitter);
                        }
                        m_feedMatchSelector.finishWithPopulatedModel(matchModel);
                    }
                });

        final AlertDialog alertDialog = ab.create();
        if ( eOKButtonAction != null ) {
            switch (eOKButtonAction) {
                case Disable:
                    alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                        @Override public void onShow(DialogInterface iDialog) {
                            final Button btnOk = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
                            m_iViewIdOfButtonInSelectPlayersDialog = btnOk.getId(); // 16908313
                            Log.d(TAG, "Ok button id " + btnOk.getId());
                            btnOk.setEnabled(false);
                        }
                    });
                    break;
                case EnableWithValidation:
                    alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                        @Override public void onShow(DialogInterface iDialog) {
                            final Button btnOk = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
                            btnOk.setOnClickListener(new View.OnClickListener() {
                                @Override public void onClick(View v) {
                                    List lNotSpecified = new ArrayList<>();
                                    for ( View vPP : vaPostParams) {
                                        if ( vPP instanceof SelectObjectView ) {
                                            SelectObjectView sov = (SelectObjectView) vPP;
                                            if ( sov.getCheckedIndex() == -1 ) {
                                                lNotSpecified.add(vPP.getTag(R.string.please_select_a_value_for_x));
                                            }
                                        }
                                        if ( vPP instanceof Spinner ) {
                                            Spinner spinner = (Spinner) vPP;
                                            if ( spinner.getSelectedItemPosition() <= 0 ) {
                                                lNotSpecified.add(vPP.getTag(R.string.please_select_a_value_for_x));
                                            }
                                        }
                                    }
                                    if ( ListUtil.isEmpty(lNotSpecified) ) {
                                        alertDialog.cancel();
                                    } else {
                                        String sMsg = context.getString(R.string.please_select_a_value_for_x, lNotSpecified.get(0));
                                        Toast.makeText(context, sMsg, Toast.LENGTH_LONG).show();
                                    }
                                }
                            });
                        }
                    });
                    break;
            }
        }

        alertDialog.show();

    }

    private static class TeamPlayerRBJsonDecorator implements SelectObjectView.RadioButtonDecorator<Map>
    {
        private String      perItemPlaceholder = null;
        private Placeholder placeholder        = null;

        TeamPlayerRBJsonDecorator(String perItemPlaceholder, Placeholder placeholder) {
            this.perItemPlaceholder = perItemPlaceholder;
            this.placeholder = placeholder;
        }
        @Override public void decorateGuiItem(int iSeqNr_1Based, Map mData, RadioButton radioButton) {
            if ( mData.containsKey(JSONKey.seqNo.toString()) == false ) {
                mData.put(JSONKey.seqNo.toString(), iSeqNr_1Based);
            }
            String sText = placeholder.translate(this.perItemPlaceholder, mData);
            sText = placeholder.removeUntranslated(sText);
            radioButton.setText(sText);
        }
    }

    private static class TeamPlayerRBDecorator implements SelectObjectView.RadioButtonDecorator<String>
    {
        private String perItemFormat = null;
        private String splitBy       = null;

        TeamPlayerRBDecorator(String perItemFormat, String splitBy) {
            this.perItemFormat = perItemFormat;
            this.splitBy = splitBy;
        }
        @Override public void decorateGuiItem(int iSeqNr_1Based, String sData, RadioButton radioButton) {
            String[] saData = sData.split(splitBy);
            String sText = null;
            try {
                switch(saData.length) {
                    case 1:
                        // assume only a name
                        sText = String.format(perItemFormat, String.valueOf(iSeqNr_1Based),  ""      , saData[0]);
                        break;
                    case 2:
                        // assume id and name (no seq no in from json)
                        sText = String.format(perItemFormat, String.valueOf(iSeqNr_1Based), saData[0], saData[1]);
                        break;
                    case 3:
                        // assume seqno, id and name
                        sText = String.format(perItemFormat, saData[0]                    , saData[1], saData[2]);
                        break;
                    default:
                        // ??
                        sText = String.format(perItemFormat, (Object) saData);
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
                sText = sData;
            }
            radioButton.setText(sText);
        }
    }
}
