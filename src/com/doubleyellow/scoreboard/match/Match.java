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

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import com.doubleyellow.android.view.ViewUtil;
import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.activity.XActivity;
import com.doubleyellow.scoreboard.feed.FeedMatchSelector;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.model.Player;
import com.doubleyellow.scoreboard.prefs.ColorPrefs;
import com.doubleyellow.scoreboard.prefs.NewMatchLayout;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.util.MenuHandler;
import com.doubleyellow.util.StringUtil;

import java.util.List;

/**
 * Activity where the user can provided
 * - the names of the players
 * - the countries of the players
 * - event details
 * - name of the referee
 * - what match format the match is played in
 * - whether or not to use timers
 * - whether or not to official announcements
 * - whether or not to use a handicap system
 *
 * Used after a match is selected from 'Feed' or 'My matches'
 */
public class Match extends XActivity implements MenuHandler
{
    private static boolean m_bShowAfterMatchSelection = true;
    public static boolean showAfterMatchSelection() {
        boolean bReturn = m_bShowAfterMatchSelection;
        if ( Match.m_bShowAfterMatchSelection == false ) {
            Match.m_bShowAfterMatchSelection = true;
        }
        return bReturn;
    }
    public static void dontShow() {
        Match.m_bShowAfterMatchSelection = false;
    }

    private MatchView vMatchView;
    /** where the current match is selected from (source) */
    private String    m_sSource;
    private String    m_sSourceID;

    @Override public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        ScoreBoard.initAllowedOrientation(this);

        ActionBar actionBar = getActionBar();
        if ( actionBar != null ) {
            actionBar.setHomeButtonEnabled(true);
        }

        boolean bIsDoubles = false;
        String sA          = null;
        String sB          = null;
        String sCountryA   = null;
        String sCountryB   = null;
        String sClubA      = null;
        String sClubB      = null;
        String sAvatarA    = null;
        String sAvatarB    = null;
        List<String> lTeamPlayersA = null;
        List<String> lTeamPlayersB = null;
      //Bundle bundleExtra = null;
        Model  model       = null;
        {
            Intent intent = getIntent();
            if (intent != null) {
              //bundleExtra = intent.getBundleExtra(MatchDetails.class.getSimpleName());
                String sJson= intent.getStringExtra(Model.class.getSimpleName());
                if ( StringUtil.isNotEmpty(sJson) ) {
                    model = Brand.getModel();
                    model.fromJsonString(sJson);
                }
            }
        }

        if ( model != null ) {
            sA            = model.getName       (Player.A);
            sB            = model.getName       (Player.B);
            sCountryA     = model.getCountry    (Player.A);
            sCountryB     = model.getCountry    (Player.B);
            sClubA        = model.getClub       (Player.A);
            sClubB        = model.getClub       (Player.B);
            sAvatarA      = model.getAvatar     (Player.A);
            sAvatarB      = model.getAvatar     (Player.B);
            lTeamPlayersA = model.getTeamPlayers(Player.A);
            lTeamPlayersB = model.getTeamPlayers(Player.B);

            bIsDoubles = model.isDoubles();
            if ( bIsDoubles ) {
                //sA += "/" + bundleExtra.getString(MatchDetails.PlayerA2.toString());
                //sB += "/" + bundleExtra.getString(MatchDetails.PlayerB2.toString());
            } else {
                // can still be a doubles from e.g. a feed
                String sRegExp = ".*/[\\s]*[^0-9].*"; // contains forward slash NOT closely followed by a number
                bIsDoubles = StringUtil.isNotEmpty(sA) && sA.matches(sRegExp);
            }
        }
        ViewUtil.setFullScreen(getWindow(), PreferenceValues.showFullScreen(this));
        PreferenceValues.setOverwrites(FeedMatchSelector.mFeedPrefOverwrites);
        vMatchView = new MatchView(this, bIsDoubles, model, NewMatchLayout.AllFields);
        setContentView(vMatchView);
        ColorPrefs.setColors(this, vMatchView);

        if ( model != null ) {
            vMatchView.setPlayers(sA, sB, sCountryA, sCountryB, sAvatarA, sAvatarB, sClubA, sClubB, lTeamPlayersA, lTeamPlayersB);

            String sEN     = model.getEventName();
            String sED     = model.getEventDivision();
            String sER     = model.getEventRound();
            String sEL     = model.getEventLocation();
            vMatchView.setEvent(sEN, sED, sER, sEL, model.getCourt(), model.getSourceID());

            String sRef    = model.getReferee();
            String sMarker = model.getMarker();
            if ( StringUtil.hasNonEmpty(sRef, sMarker) ) {
                vMatchView.setReferees(sRef, sMarker);
            }

            m_sSource   = model.getSource();
            m_sSourceID = model.getSourceID();
            if ( StaticMatchSelector.matchIsFrom(m_sSource) == false ) {
                // selected from some public feed
                if ( StringUtil.areAllEmpty(sClubA, sClubB) ) {
                    vMatchView.clearClubFields();
                }
            }
        }
        ScoreBoard.updateDemoThread(this);
    }

    public boolean handleMenuItem(int menuItemId, Object... ctx) {
        switch (menuItemId) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.dyn_new_match: {
                Intent intent = new Intent();
                intent.setAction(String.valueOf(R.id.dyn_new_match));
                setResult(RESULT_OK, intent);
                finish();
                return true;
            }
            case R.id.cmd_ok:
                Intent intent = vMatchView.getIntent(m_sSource, m_sSourceID, false);
                if (intent == null) return false;
                setResult(RESULT_OK, intent);
                Match.dontShow();

                finish();
                return true;
            case R.id.cmd_cancel:
                finish();
                return true;
            case R.id.uc_clear_all_fields:
                return vMatchView.clearAllFields();
            case R.id.uc_clear_player_fields:
                return vMatchView.clearPlayerFields();
            case R.id.uc_clear_club_fields:
                return vMatchView.clearClubFields();
            case R.id.uc_clear_country_fields:
                return vMatchView.clearCountryFields();
            case R.id.uc_clear_event_fields:
                return vMatchView.clearEventFields();
            case R.id.uc_clear_referee_fields:
                vMatchView.clearRefereeFields();
                return true;
        }
        return false;
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.newmatchmenu, menu);
        return true;
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        handleMenuItem(item.getItemId());
        return true;
    }

    @Override public void onBackPressed() {
        boolean bNeutralButtonIsBackToList = StaticMatchSelector.matchIsFrom(m_sSource)
                                          || FeedMatchSelector  .class.getSimpleName().equals(m_sSource)
                                          || ((m_sSource != null) && m_sSource.startsWith("http"));
        if ( bNeutralButtonIsBackToList ) {
            handleMenuItem(R.id.dyn_new_match);
            return;
        }
        Intent intent = vMatchView.getIntent(m_sSource, m_sSourceID, true);
        if ( intent == null ) {
            // not enough data entered... simply leave the activity
            super.onBackPressed();
        } else {
            // user enter valid data to start match with
            AlertDialog.Builder ab = ScoreBoard.getAlertDialogBuilder(this);
            DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface dialog, int choice) {
                    switch (choice) {
                        case DialogInterface.BUTTON_POSITIVE:
                            handleMenuItem(R.id.cmd_ok);
                            break;
                        case DialogInterface.BUTTON_NEUTRAL:
/*
                            if ( bNeutralButtonIsBackToList ) {
                                handleMenuItem(R.id.dyn_new_match);
                            }
*/
                            break;
                        case DialogInterface.BUTTON_NEGATIVE:
                            handleMenuItem(R.id.cmd_cancel);
                            break;
                    }

                }
            };

            int iResIdCancel = android.R.string.cancel; // close the dialog only, stay in match activity
/*
            if (bNeutralButtonIsBackToList) {
                iResIdCancel = R.string.back_to_list_of_matches;
            }
*/
            ab.setMessage(R.string.q_use_entered_match_details)
                    .setPositiveButton(R.string.cmd_yes, listener)
                    .setNegativeButton(R.string.cmd_no , listener) // close the dialog and the activity
                    .setNeutralButton (iResIdCancel    , listener)
                    .show();
        }
    }
}
