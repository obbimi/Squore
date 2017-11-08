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
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.util.MenuHandler;
import com.doubleyellow.util.StringUtil;

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
    private MatchView vMatchView;
    private String    sFeedKey;

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

/*
        if ( bundleExtra != null ) {
            sA         = bundleExtra.getString   (MatchDetails.PlayerA .toString());
            sB         = bundleExtra.getString   (MatchDetails.PlayerB .toString());
            sCountryA  = bundleExtra.getString   (MatchDetails.CountryA.toString());
            sCountryB  = bundleExtra.getString   (MatchDetails.CountryB.toString());

            bIsDoubles = bundleExtra.getBoolean  (MatchDetails.IsDoubles.toString(), bIsDoubles);
            if ( bIsDoubles ) {
                sA += "/" + bundleExtra.getString(MatchDetails.PlayerA2.toString());
                sB += "/" + bundleExtra.getString(MatchDetails.PlayerB2.toString());
            } else {
                // can still be a doubles from e.g. a feed
                String sRegExp = ".*//*
[\\s]*[^0-9].*"; // contains forward slash NOT closely followed by a number
                bIsDoubles = StringUtil.isNotEmpty(sA) && sA.matches(sRegExp);
            }
        }
*/
        if ( model != null ) {
            sA         = model.getName(Player.A);
            sB         = model.getName(Player.B);
            sCountryA  = model.getCountry(Player.A);
            sCountryB  = model.getCountry(Player.B);
            sClubA     = model.getClub(Player.A);
            sClubB     = model.getClub(Player.B);

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
        vMatchView = new MatchView(this, bIsDoubles);
        setContentView(vMatchView);
        ColorPrefs.setColors(this, vMatchView);

/*
        if ( bundleExtra != null ) {
            vMatchView.setPlayers(sA, sB, sCountryA, sCountryB);
            // if the names came from selecting a match from another place, do not auto set focus to the player names

            String sEN     = bundleExtra.getString(MatchDetails.EventName    .toString());
            String sED     = bundleExtra.getString(MatchDetails.EventDivision.toString());
            String sER     = bundleExtra.getString(MatchDetails.EventRound   .toString());
            String sEL     = bundleExtra.getString(MatchDetails.EventLocation.toString());
            vMatchView.setEvent(sEN, sED, sER, sEL);

            String sRef    = bundleExtra.getString(MatchDetails.Referee      .toString());
            String sMarker = bundleExtra.getString(MatchDetails.Marker       .toString());
            if ( StringUtil.areAllEmpty(sRef, sMarker) == false ) {
                vMatchView.setReferees(sRef, sMarker);
            }

            sFeedKey = bundleExtra.getString(MatchDetails.FeedKey.toString(), "");
            if ( StaticMatchSelector.class.getSimpleName().equals(sFeedKey) == false ) {
                // selected from some public feed
                vMatchView.clearClubFields();
            }
        }
*/
        if ( model != null ) {
            vMatchView.setPlayers(sA, sB, sCountryA, sCountryB, sClubA, sClubB);
            // if the names came from selecting a match from another place, do not auto set focus to the player names

            String sEN     = model.getEventName();
            String sED     = model.getEventDivision();
            String sER     = model.getEventRound();
            String sEL     = model.getEventLocation();
            vMatchView.setEvent(sEN, sED, sER, sEL);

            String sRef    = model.getReferee();
            String sMarker = model.getMarker();
            if ( StringUtil.hasNonEmpty(sRef, sMarker) ) {
                vMatchView.setReferees(sRef, sMarker);
            }

            sFeedKey = model.getSource();
            if ( StaticMatchSelector.class.getSimpleName().equals(sFeedKey) == false ) {
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
                Intent intent = vMatchView.getIntent(sFeedKey, false);
                if (intent == null) return false;
                setResult(RESULT_OK, intent);

/*
                if ( vMatchView.getDoublesServeSequence() != DoublesServeSequence.NA ) {
                    String sSSFilename = Util.filenameForAutomaticScreenshot(this, null, ShowOnScreen.OnDevice, -1, -1, "%s.DoubleMatch." + vMatchView.getDoublesServeSequence() + ".png");
                    if ( sSSFilename!=null ) {
                        ViewUtil.takeScreenShot(this, Brand.brand, sSSFilename, vMatchView);
                    }
                }
*/

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
        boolean bNeutralButtonIsBackToList = StaticMatchSelector.class.getSimpleName().equals(sFeedKey)
                                          || FeedMatchSelector  .class.getSimpleName().equals(sFeedKey)
                                          || ((sFeedKey != null) && sFeedKey.startsWith("http"));
        if ( bNeutralButtonIsBackToList ) {
            handleMenuItem(R.id.dyn_new_match);
            return;
        }
        Intent intent = vMatchView.getIntent(sFeedKey, true);
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
