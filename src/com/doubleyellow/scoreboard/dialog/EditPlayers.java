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

package com.doubleyellow.scoreboard.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import com.doubleyellow.android.util.ColorUtil;
import com.doubleyellow.android.view.ViewUtil;
import com.doubleyellow.android.view.EnumSpinner;
import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.PersistHelper;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.archive.PreviousMatchSelector;
import com.doubleyellow.scoreboard.model.JSONKey;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.model.Player;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.prefs.AnnouncementLanguage;
import com.doubleyellow.scoreboard.prefs.ColorPrefs;
import com.doubleyellow.scoreboard.prefs.PreferenceKeys;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.scoreboard.view.PlayerTextView;
import com.doubleyellow.scoreboard.view.PreferenceACTextView;
import com.doubleyellow.util.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @deprecated We will be editing a match with the MatchView in a Match activity
 */
public class EditPlayers extends BaseAlertDialog
{
    public EditPlayers(Context context, Model matchModel, ScoreBoard scoreBoard) {
        super(context, matchModel, scoreBoard);
    }

    @Override public boolean storeState(Bundle outState) {
        return true;
    }

    @Override public boolean init(Bundle outState) {
        return true;
    }

    //private int iFirstViewId = 100;
    private View firstEditText = null;

    static Map<JSONKey, Integer> mCaptions = new HashMap<JSONKey, Integer>();
    static {
        mCaptions.put(JSONKey.event   , R.string.lbl_event);
        mCaptions.put(JSONKey.division, R.string.lbl_division);
        mCaptions.put(JSONKey.location, R.string.lbl_location);
        mCaptions.put(JSONKey.clubs   , R.string.lbl_clubs);
        mCaptions.put(JSONKey.round   , R.string.lbl_round);
        mCaptions.put(JSONKey.court   , R.string.lbl_court__Default);
        mCaptions.put(JSONKey.referee , R.string.lbl_referee);
        mCaptions.put(JSONKey.markers , R.string.lbl_marker);
    }

    private String getMatchDetailsLabel(JSONKey md) {
        if ( mCaptions.containsKey(md) ) {
            return context.getString(mCaptions.get(md));
        } else {
            return StringUtil.capitalize(md);
        }
    }
    @Override public void show() {
        //ViewGroup ll = initUsingLayoutXml();

        EnumSet<JSONKey> eEventDetails = EnumSet.of(                        JSONKey.event         ,    JSONKey.division          ,    JSONKey.round          ,    JSONKey.location          , JSONKey.court);
        List<String> eventDetails = new ArrayList<String>(Arrays.asList( matchModel.getEventName(), matchModel.getEventDivision(), matchModel.getEventRound(), matchModel.getEventLocation(), matchModel.getCourt() ));

        EnumSet<JSONKey> eReferees = EnumSet.of(                            JSONKey.referee     ,    JSONKey.markers);
        List<String> refDetails   = new ArrayList<String>(Arrays.asList( matchModel.getReferee(), matchModel.getMarker()));

        Map<ColorPrefs.ColorTarget, Integer> mColors = ColorPrefs.getTarget2colorMapping(context);
        int iMainBgColor   = mColors.get(ColorPrefs.ColorTarget.playerButtonBackgroundColor);
        int iLabelTxt      = mColors.get(ColorPrefs.ColorTarget.playerButtonTextColor);
        int iSelectTxt     = mColors.get(ColorPrefs.ColorTarget.darkest);
        int iInputBgColor  = mColors.get(ColorPrefs.ColorTarget.scoreButtonBackgroundColor);
        int iInputTxtColor = mColors.get(ColorPrefs.ColorTarget.scoreButtonTextColor);

        ViewGroup sv = new ScrollView(context);
        final LinearLayout ll = new LinearLayout(context);
        ll.setOrientation(LinearLayout.VERTICAL);
        ColorUtil.setBackground(ll, iMainBgColor);

        // event details
        ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        for ( JSONKey e: eEventDetails ) {
            final LinearLayout llLabelAndText = new LinearLayout(context);
            llLabelAndText.setOrientation(LinearLayout.HORIZONTAL);

            TextView label = new TextView(context);
            label.setText(getMatchDetailsLabel(e));
            label.setTextColor(iLabelTxt);
            llLabelAndText.addView(label);

            PreferenceACTextView text = new PreferenceACTextView(context);
            text.useLastValueAsDefault(false);
            switch (e) {
                case event:
                    text.init(R.string.eventList_default, PreferenceKeys.eventList, PreferenceKeys.eventLast);
                    break;
                case division:
                    text.init(R.string.divisionList_default, PreferenceKeys.divisionList, PreferenceKeys.divisionLast);
                    break;
                case round:
                    text.init(R.string.roundList_default, PreferenceKeys.roundList, PreferenceKeys.roundLast);
                    break;
                case location:
                    text.init(R.string.emptyList_default, PreferenceKeys.locationList, PreferenceKeys.locationLast);
                    break;
                case court:
                    text.init(R.string.emptyList_default, PreferenceKeys.courtList, PreferenceKeys.courtLast);
                    break;
/*
                case ClubA:
                    text.init(R.string.emptyList_default, PreferenceKeys.clubList, PreferenceKeys.clubListLastA);
                    break;
                case ClubB:
                    text.init(R.string.emptyList_default, PreferenceKeys.clubList, PreferenceKeys.clubListLastX);
                    break;
*/
            }
            text.setImeOptions(EditorInfo.IME_ACTION_NEXT); // using this seems to block the usage of setOnKeyListener()
/*
            text.setId(iViewId);
            if ( txtPrevious != null ) {
                txtPrevious.setNextFocusDownId(text.getId());
            }
            txtPrevious = text; iViewId+=100;
            text.setOnKeyListener(onKeyListener);
*/
            text.setOnEditorActionListener(onEditorActionListener);
            if ( firstEditText == null ) {
                firstEditText = text;
            }
            text.setLayoutParams(lp);
            text.setSingleLine();
            llLabelAndText.addView(text);

            ll.addView(llLabelAndText);

            text.setText(eventDetails.remove(0));
            mEvent2Txt.put(e, text);

            ColorUtil.setBackground(text, iInputBgColor);
            text.setTextColor(iInputTxtColor);
            text.setHighlightColor(iSelectTxt);
            //text.setPadding(0,0,0,10);
        }

        // player details
        final String[] playerNames = matchModel.getPlayerNames(true, false);
        final List<TextView> lPlayers   = new ArrayList<TextView>();
        final List<TextView> lCountries = new ArrayList<TextView>();
        final List<TextView> lClubs     = new ArrayList<TextView>();
        LinearLayout.LayoutParams lpPC = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        for ( Player p: Model.getPlayers() ) {
            final LinearLayout llLabelPlayerAndCountry = new LinearLayout(context);
            llLabelPlayerAndCountry.setOrientation(LinearLayout.HORIZONTAL);

            TextView label = new TextView(context);
            label.setText(getString(R.string.lbl_player) + " " + p + " ");
            label.setTextColor(iLabelTxt);
            llLabelPlayerAndCountry.addView(label);

            EditText txtPlayer = new PlayerTextView(context);
            txtPlayer.setImeOptions(EditorInfo.IME_ACTION_NEXT); // using this seems to block the usage of setOnKeyListener()
/*
            text.setId(iViewId);
            if ( txtPrevious != null ) {
                txtPrevious.setNextFocusDownId(text.getId());
            }
            txtPrevious = text; iViewId+=100;
            text.setOnKeyListener(onKeyListener);
*/
            txtPlayer.setOnEditorActionListener(onEditorActionListener);
            //text.setSingleLine();
            txtPlayer.setLayoutParams(lpPC);
            llLabelPlayerAndCountry.addView(txtPlayer);

            ll.addView(llLabelPlayerAndCountry);

            txtPlayer.setText(playerNames[p.ordinal()]);
            mPlayer2Txt.put(p, txtPlayer);
            lPlayers.add(txtPlayer);

            ColorUtil.setBackground(txtPlayer, iInputBgColor);
            txtPlayer.setTextColor(iInputTxtColor);
            txtPlayer.setHighlightColor(iSelectTxt);
            //text.setPadding(0,0,0,10);

            if ( StringUtil.isEmpty(matchModel.getClub(p) )) {
                TextView txtCountry = new EditText(context);
                //txtCountry.setLayoutParams(lpPC);
                txtCountry.setText(matchModel.getCountry(p));
                txtCountry.setSingleLine();
                mPlayer2Country.put(p, txtCountry);
                lCountries.add(txtCountry);
                if (PreferenceValues.useCountries(context)) {
                    lpPC.weight = 1;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        llLabelPlayerAndCountry.addView(txtCountry, new LinearLayout.LayoutParams(lpPC));
                    } else {
                        llLabelPlayerAndCountry.addView(txtCountry, new LinearLayout.LayoutParams(lpPC.width, lpPC.height));
                    }
                } else {
                    txtCountry.setVisibility(View.GONE);
                }
            } else {
                PreferenceACTextView txtClub = new PreferenceACTextView(context);
                //txtClub.setLayoutParams(lpPC);
                txtClub.setText(matchModel.getClub(p));
                txtClub.setSingleLine();
                mPlayer2Club.put(p, txtClub);
                lClubs.add(txtClub);
                if ( true ) {
                    lpPC.weight = 1;
                    if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ) {
                        llLabelPlayerAndCountry.addView(txtClub, new LinearLayout.LayoutParams(lpPC));
                    } else {
                        llLabelPlayerAndCountry.addView(txtClub, new LinearLayout.LayoutParams(lpPC.width, lpPC.height));
                    }
                } else {
                    txtClub.setVisibility(View.GONE);
                }
            }
        }

        // referee details
        //int iViewId = iFirstViewId;
        for ( JSONKey e: eReferees ) {
            final LinearLayout llLabelAndText = new LinearLayout(context);
            llLabelAndText.setOrientation(LinearLayout.HORIZONTAL);

            TextView label = new TextView(context);
            label.setText(getMatchDetailsLabel(e));
            label.setTextColor(iLabelTxt);
            llLabelAndText.addView(label);

            PreferenceACTextView text = new PreferenceACTextView(context);
            switch (e) {
                case referee:
                    text.init(R.string.emptyList_default, PreferenceKeys.refereeList, PreferenceKeys.refereeName);
                    break;
                case markers:
                    text.init(R.string.emptyList_default, PreferenceKeys.refereeList, PreferenceKeys.markerName);
                    break;
            }
            text.setImeOptions(e.equals(JSONKey.referee) ? EditorInfo.IME_ACTION_NEXT : EditorInfo.IME_ACTION_DONE); // using this seems to block the usage of setOnKeyListener()
/*
            text.setId(iViewId);
            if ( txtPrevious != null ) {
                txtPrevious.setNextFocusDownId(text.getId());
            }
            txtPrevious = text; iViewId+=100;
            text.setOnKeyListener(onKeyListener);
*/
            text.setOnEditorActionListener(onEditorActionListener);
            if ( firstEditText == null ) {
                firstEditText = text;
            }
            text.setLayoutParams(lp);
            text.setSingleLine();
            llLabelAndText.addView(text);

            ll.addView(llLabelAndText);

            text.setText(refDetails.remove(0));
            mReferee2Txt.put(e, text);

            ColorUtil.setBackground(text, iInputBgColor);
            text.setTextColor(iInputTxtColor);
            text.setHighlightColor(iSelectTxt);
            //text.setPadding(0,0,0,10);
        }

        if ( Brand.isSquash() ) {
            if ( PreferenceValues.useOfficialAnnouncementsFeature(context).equals(Feature.DoNotUse) ) {
                // nothing to do
            } else {
                if (scoreBoard != null) {
                    // invoked from main scoreboard
                    //esAnnouncementLang = new EnumSpinner<AnnouncementLanguage>(context);
                    esAnnouncementLang = new Spinner(context);
                    AnnouncementLanguage language = PreferenceValues.officialAnnouncementsLanguage(context);
                    EnumSpinner.init(esAnnouncementLang, context, AnnouncementLanguage.class, language, null, 0);
                    ll.addView(esAnnouncementLang);
                }
            }
        }

        TextWatcher txtWatcher = new BasicTextWatcher() {
            @Override public void afterTextChanged(Editable s) {
                final Button btnOk = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                btnOk.setEnabled(ViewUtil.countNonEmpty(lPlayers) >= 2);
            }
        };
        for(TextView textView: lPlayers) {
            textView.addTextChangedListener(txtWatcher);
        }
        for(TextView textView: lCountries) {
            textView.addTextChangedListener(txtWatcher);
        }
        for(TextView textView: lClubs) {
            textView.addTextChangedListener(txtWatcher);
        }

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialog, int which) {
                handleButtonClick(which);
            }
        };
        sv.addView(ll);
        String s1 = getString(R.string.lbl_event_and_players);
        if ( isNotWearable() ) {
            adb.setTitle(s1);
            adb.setIcon(android.R.drawable.ic_menu_edit);
        }
        adb     .setView(sv)
                .setPositiveButton(R.string.cmd_ok    , dialogClickListener)
                .setNegativeButton(R.string.cmd_cancel, dialogClickListener)
                //.setCancelable(false)
        ;
        dialog = adb.show(onShowListener);

        // try showing the keyboard by default (seems not to work in landscape due to lack of space on screen?)
        ViewUtil.showKeyboard(dialog);
    }

    TextView.OnEditorActionListener onEditorActionListener = new TextView.OnEditorActionListener() {
        @Override public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            dialog.setCancelable(false);
            return false;
        }
    };

/*
    private View.OnKeyListener onKeyListener = new View.OnKeyListener() {
        @Override public boolean onKey(View view, int i, KeyEvent keyEvent) {
            if ( keyEvent.getKeyCode() != KeyEvent.KEYCODE_BACK ) {
                dialog.setCancelable(false); // do not close the dialog for the back key (prevent user losing changes he entered). Really not using the changees can be done by pressing 'Cancel'
                return true;
            }
            return false;
        }
    };
*/

    private DialogInterface.OnShowListener onShowListener = new DialogInterface.OnShowListener() {
        @Override public void onShow(DialogInterface dialogInterface) {
            // set focus on first
            if ( firstEditText != null ) {
                //setBackground(e, mColors.get(ColorPrefs.ColorTarget.lightest));
                firstEditText.requestFocus();
            }
        }
    };

    final Map<JSONKey, PreferenceACTextView> mEvent2Txt   = new HashMap<JSONKey, PreferenceACTextView>();
    final Map<JSONKey, PreferenceACTextView> mReferee2Txt = new HashMap<JSONKey, PreferenceACTextView>();
    final Map<Player, PreferenceACTextView>       mPlayer2Club = new HashMap<Player, PreferenceACTextView>();
    final Map<Player, EditText> mPlayer2Txt     = new HashMap<Player, EditText>();
    final Map<Player, TextView> mPlayer2Country = new HashMap<Player, TextView>();
    Spinner esAnnouncementLang = null;

    public void handleButtonClick(int which) {
        switch (which){
            case DialogInterface.BUTTON_POSITIVE:
                String[] saPlayers = new String[] { mPlayer2Txt.get(Player.A).getText().toString()
                                                  , mPlayer2Txt.get(Player.B).getText().toString()
                                                  };
                String[] saCountries = null;
                if (mPlayer2Country.size() == 2) {
                    saCountries = new String[] { mPlayer2Country.get(Player.A).getText().toString()
                                               , mPlayer2Country.get(Player.B).getText().toString()
                                               };
                }
                String[] saClubs = null;
                if (mPlayer2Club.size() == 2) {
                    saClubs = new String[] { mPlayer2Club.get(Player.A).getTextAndPersist().toString()
                                                    , mPlayer2Club.get(Player.B).getTextAndPersist().toString()
                    };
                }
                String[] saEvent = new String[] { mEvent2Txt.get(JSONKey.event   ).getTextAndPersist().toString()
                                                , mEvent2Txt.get(JSONKey.division).getTextAndPersist().toString()
                                                , mEvent2Txt.get(JSONKey.round   ).getTextAndPersist().toString()
                                                , mEvent2Txt.get(JSONKey.location).getTextAndPersist().toString()
                                                };
                String[] saReferee = new String[] { mReferee2Txt.get(JSONKey.referee).getTextAndPersist().toString()
                                                  , mReferee2Txt.get(JSONKey.markers).getTextAndPersist().toString()
                                                  };
                if ( scoreBoard != null ) {
                    // invoked from main scoreboard
                    boolean bPlayersModified = scoreBoard.setPlayerNames(saPlayers);
                    boolean bModified = bPlayersModified;

                    bModified =  matchModel.setReferees     (saReferee[0], saReferee[1])                           || bModified;
                    bModified =  matchModel.setEvent        (saEvent[0]  , saEvent[1]  , saEvent[2], saEvent[3])   || bModified;
                    bModified =  matchModel.setCourt(mEvent2Txt.get(JSONKey.court).getTextAndPersist().toString()) || bModified;
                    if ( saCountries != null ) {
                        bModified =  matchModel.setPlayerCountry(Player.A, saCountries[0])                         || bModified;
                        bModified =  matchModel.setPlayerCountry(Player.B, saCountries[1])                         || bModified;
                    }
                    if ( saClubs != null ) {
                        bModified =  matchModel.setPlayerClub   (Player.A, saClubs[0])                             || bModified;
                        bModified =  matchModel.setPlayerClub   (Player.B, saClubs[1])                             || bModified;
                    }

                    // if new player names were actually entered optionally restart the game
                    if (bPlayersModified && matchModel.hasStarted()) {
                        scoreBoard.confirmRestart();
                    }
                    if ( esAnnouncementLang != null ) {
                        AnnouncementLanguage languageNew = AnnouncementLanguage.values()[esAnnouncementLang.getSelectedItemPosition()];
                        PreferenceValues.setAnnouncementLanguage(languageNew, context);
                    }
                } else {
                    // from previous matches
                    File fWasStoredAs = matchModel.getStoreAs(PreviousMatchSelector.getArchiveDir(context));

                    boolean bRefChanged   = matchModel.setReferees(saReferee[0], saReferee[1]);
                    boolean bEventChanged = matchModel.setEvent(saEvent[0], saEvent[1], saEvent[2], saEvent[3]);
                    boolean bNameChanged = false;
                    for ( Player player: Model.getPlayers() ) {
                        String sPlayer = saPlayers[player.ordinal()].trim();
                               sPlayer = PreferenceValues.capitalizePlayerName(sPlayer);
                        bNameChanged = bNameChanged || matchModel.setPlayerName(player, sPlayer);
                    }
                    try {
                        if ( bNameChanged || bEventChanged ) {
                            // if names have changed the filename will have changed... so need to remove the old .sb file
                            fWasStoredAs.delete();
                        }
                        PersistHelper.storeAsPrevious(context, matchModel, true);
                        if ( context instanceof MenuHandler) {
                            MenuHandler menuHandler = (MenuHandler) context;
                            menuHandler.handleMenuItem(R.id.refresh);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                // Do nothing.
                break;
        }
    }
}
