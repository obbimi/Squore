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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.*;

import androidx.annotation.ColorInt;

import com.doubleyellow.android.util.ColorUtil;
import com.doubleyellow.android.view.ColorPickerView;
import com.doubleyellow.android.view.CountryTextView;
import com.doubleyellow.android.view.SatValHueColorPicker;
import com.doubleyellow.android.view.ViewUtil;
import com.doubleyellow.android.view.EnumSpinner;
import com.doubleyellow.prefs.RWValues;
import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.activity.IntentKeys;
import com.doubleyellow.scoreboard.dialog.ButtonUpdater;
import com.doubleyellow.scoreboard.dialog.MyDialogBuilder;
import com.doubleyellow.scoreboard.model.*;
import com.doubleyellow.scoreboard.prefs.*;
import com.doubleyellow.scoreboard.view.PlayerTextView;
import com.doubleyellow.scoreboard.view.PreferenceACTextView;
import com.doubleyellow.util.Feature;
import com.doubleyellow.util.JsonUtil;
import com.doubleyellow.util.ListUtil;
import com.doubleyellow.util.StringUtil;
import com.doubleyellow.view.NextFocusDownListener;
import com.doubleyellow.view.SBRelativeLayout;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * MatchView used by both the Match activity (post match selection from a list) and the MatchFragment (MatchTabbed).
 *
 * Used to define the
 * - players
 * - match format
 * - event details
 * - referee details
 * - optionally player colors
 */
public class MatchView extends SBRelativeLayout
{
    private static final String TAG = "SB." + MatchView.class.getSimpleName();

    private NewMatchLayout m_layout = NewMatchLayout.AllFields;

    /**
     * @param model is null when used in MatchFragment
     */
    public MatchView(Context context, boolean bIsDoubles, Model model, NewMatchLayout layout) {
        super(context);
        m_layout = layout;
        init(bIsDoubles, model);
        if ( hideElementsBasedOnSport() == false ) {
            initExpandCollapse();
        }
    }

    void setReferees(String sName, String sMarker) {
        txtRefereeName.setText(sName);
        txtMarkerName .setText(sMarker);
    }

    /** wrapper introduced to be able to quickly turn it off during development */
    private boolean requestFocusFor(View v) {
        //if ( true ) { return false; }
        if ( v == null ) {
            return false;
        }
        return v.requestFocus();
    }

    void setEvent(String sName, String sDivision, String sRound, String sLocation, String sCourt, String sSourceID) {
        boolean bFocusOnIfEmpty = false;
        txtEventName.setText(sName);
        if ( ViewUtil.areAllNonEmpty(txtEventName) ) {
            if (bFocusOnIfEmpty) requestFocusFor(txtEventDivision);
        }
        txtEventDivision.setText(sDivision);
        txtEventDivision.useLastValueAsDefault(StringUtil.isEmpty(sName)); // only fill division with previous if name was also not specified
        if ( ViewUtil.areAllNonEmpty(txtEventName, txtEventDivision) ) {
            if (bFocusOnIfEmpty) requestFocusFor(txtEventRound);
        }
        txtEventRound.setText(sRound);
        txtEventRound.useLastValueAsDefault(StringUtil.isEmpty(sName)); // only fill round with previous if name was also not specified
        if ( ViewUtil.areAllNonEmpty(txtEventName, txtEventDivision, txtEventRound) ) {
            if (bFocusOnIfEmpty) requestFocusFor(txtEventLocation);
        }
        txtEventLocation.setText(sLocation);
        txtEventLocation.useLastValueAsDefault(StringUtil.isEmpty(sName)); // only fill location with previous if name was also not specified
        if ( ViewUtil.areAllNonEmpty(txtEventName, txtEventDivision, txtEventRound, txtEventLocation) ) {
            requestFocusFor(txtPlayerA);
        }
        if ( (ViewUtil.areAllEmpty(txtEventName, txtEventDivision, txtEventRound, txtEventLocation) == false) && bEventCollapsed) {
            findViewById(R.id.lblEvent).callOnClick(); // to expand the parent area by default
            bEventCollapsed = true;
        }

        if ( txtCourt != null ) {
            txtCourt.useLastValueAsDefault(StringUtil.isEmpty(sName)); // only fill court with previous if name was also not specified
            txtCourt.setText(sCourt);
        }
        if ( txtEventID != null ) {
            txtEventID.setText(sSourceID);
            int visibility = StringUtil.isEmpty(sSourceID) ? GONE : VISIBLE;
            txtEventID.setVisibility(visibility);
        }
    }

    //private static final String[] sExpandedCollapsed = {"\u2303", "\u2304"};
    private static final String[] sExpandedCollapsed = {"\u2191", "\u2193"}; // up & down
    //private static final String[] sExpandedCollapsed = {"\u21A5", "\u21A7"}; // arrow with a bar
    //private static final String[] sExpandedCollapsed = {"\u2B06", "\u2B07"}; // pretty fat arrow (maybe a little TO in your face)

    private boolean bEventCollapsed     = false;
    private boolean bClubsCollapsed     = false;
    private boolean bCountriesCollapsed = false;
    private void initExpandCollapse() {
        final int[] iToggleClubs = {R.id.match_clubA, R.id.match_clubB};
        bClubsCollapsed = (ViewUtil.editTextsAreAllEmpty(this, iToggleClubs));
        ViewUtil.installExpandCollapse(this, R.id.lblClubs, iToggleClubs, (bClubsCollapsed ? GONE : VISIBLE), sExpandedCollapsed);

        final int[] iToggleCountries = {R.id.match_countryA, R.id.match_countryB};
        bCountriesCollapsed = (ViewUtil.editTextsAreAllEmpty(this, iToggleCountries));
        ViewUtil.installExpandCollapse(this, R.id.lblCountries, iToggleCountries, (bCountriesCollapsed ? GONE : VISIBLE), sExpandedCollapsed);

        final int[] iToggleEventViews = {R.id.ll_match_event_texts, R.id.ll_match_event_texts1, R.id.ll_match_event_texts2, R.id.ll_match_event_texts3};
        bEventCollapsed = (ViewUtil.editTextsAreAllEmpty(this, iToggleEventViews));
        ViewUtil.installExpandCollapse(this, R.id.lblEvent, iToggleEventViews, (bEventCollapsed ? GONE : VISIBLE), sExpandedCollapsed);

        final int[] iToggleRefViews = {R.id.match_referee, R.id.match_marker, R.id.ll_AnnouncementLanguage};
        final boolean bRefTxtsAreEmpty = ViewUtil.editTextsAreAllEmpty(this, iToggleRefViews);
        final boolean bLanguageDeviates = PreferenceValues.announcementLanguageDeviates(getContext());
        int iInitialState = bRefTxtsAreEmpty && (bLanguageDeviates == false) ? GONE : VISIBLE;
        ViewUtil.installExpandCollapse(this, R.id.lblReferee, iToggleRefViews, iInitialState, sExpandedCollapsed);

        // make list (brand dependant) of what elements to toggle when lblFormat is expanded collapsed
        List<Integer> lToggleFormatViews = new ArrayList<>();
        lToggleFormatViews.add(R.id.llPoints);
        lToggleFormatViews.add(R.id.llTieBreakFormat);
        lToggleFormatViews.add(R.id.llFinalSetFinish);
        lToggleFormatViews.add(R.id.llPauseDuration);
        lToggleFormatViews.add(R.id.llScoringType);
        lToggleFormatViews.add(R.id.llLiveScore);
        if ( m_bIsDoubles ) {
            lToggleFormatViews.add(R.id.ll_doubleServeSequence);
        }
        SportType sportType = Brand.getSport();
        switch (sportType) {
            case Tabletennis:
                lToggleFormatViews.add(R.id.llNumberOfServesPerPlayer);
                break;
            case Racketlon:
                lToggleFormatViews.add(R.id.llDisciplineStart);
                break;
            case Badminton:
                //lToggleFormatViews.remove(R.id.llTieBreakFormat); // TODO: at 20 sudden death, or 2 points difference until 29-all and then sudden death
                break;
            case Squash:
                lToggleFormatViews.add(R.id.useHandInHandOutScoring);
                lToggleFormatViews.add(R.id.llHandicapFormat);
                break;
        }
        if ( Brand.supportsDoubleServeSequence() == false ) {
            lToggleFormatViews.remove((Integer) R.id.ll_doubleServeSequence);
        }
        if ( Brand.supportsTiebreakFormat() == false ) {
            lToggleFormatViews.remove((Integer) R.id.llTieBreakFormat);
        }
        if ( Brand.isGameSetMatch() == false ) {
            lToggleFormatViews.remove((Integer) R.id.llFinalSetFinish );
        }

/*
        int[] iToggleFormatViews = new int[] {R.id.llPoints, R.id.llTieBreakFormat, R.id.llHandicapFormat, R.id.useHandInHandOutScoring, R.id.llPauseDuration, R.id.llDisciplineStart, R.id.llNumberOfServesPerPlayer, R.id.llScoringType, R.id.llLiveScore};
        if ( m_bIsDoubles ) {
            iToggleFormatViews = new int[]   {R.id.llPoints, R.id.llTieBreakFormat, R.id.llHandicapFormat, R.id.useHandInHandOutScoring, R.id.llPauseDuration, R.id.llDisciplineStart, R.id.llNumberOfServesPerPlayer, R.id.llScoringType, R.id.llLiveScore, R.id.ll_doubleServeSequence};
        }
*/
        ViewUtil.installExpandCollapse(this, R.id.lblFormat, lToggleFormatViews, VISIBLE, sExpandedCollapsed);
    }

    private boolean hideElementsBasedOnSport() {
        // hide some elements base on sport
        SportType sportType = Brand.getSport();
        boolean bTrueGoneFalseInvisible = ViewUtil.isPortraitOrientation(getContext()) || m_layout.equals(NewMatchLayout.Simple); // in portrait each element is on a single line, layout is not screwed up when GONE is used. In landscape layout IS screwed up
        switch (sportType) {
            case Badminton:
                ViewUtil.hideViewsForEver(this, bTrueGoneFalseInvisible
                        , R.id.llDisciplineStart
                      //, R.id.llScoringType /* hand-in/hand-out was actually used in badminton in the past as well */
                        , R.id.match_marker
                        , R.id.ll_AnnouncementLanguage
                        , R.id.llNumberOfServesPerPlayer
                );
                break;
            case Tabletennis:
                ViewUtil.hideViewsForEver(this, bTrueGoneFalseInvisible
                        , R.id.llDisciplineStart
                        , R.id.llScoringType
                        , R.id.match_marker
                        , R.id.ll_AnnouncementLanguage
                );
                break;
            case Racketlon:
                ViewUtil.hideViewsForEver(this, bTrueGoneFalseInvisible
                        //, R.id.lblMatch_BestOf
                        , R.id.tbBestOf_or_TotalOf
                        , R.id.spNumberOfGamesToWin
                        , R.id.llHandicapFormat
                        , R.id.llScoringType
                        , R.id.llNumberOfServesPerPlayer
                        , R.id.match_marker
                        , R.id.ll_AnnouncementLanguage
                );
                break;
            case Squash:
                ViewUtil.hideViewsForEver(this, bTrueGoneFalseInvisible
                        , R.id.llDisciplineStart
                        , R.id.llNumberOfServesPerPlayer
                );
                break;
            case Racquetball:
                ViewUtil.hideViewsForEver(this
                        , R.id.llHandicapFormat
                        , R.id.llDisciplineStart
                        , R.id.llNumberOfServesPerPlayer
                      //, R.id.llScoringType
                );
                break;
            case Padel:
            case TennisPadel:
                ViewUtil.hideViewsForEver(this, bTrueGoneFalseInvisible
                        //, R.id.lblMatch_BestOf
                        //, R.id.tbBestOf_or_TotalOf
                        , R.id.match_marker
                );
                break;
        }
        if ( PreferenceValues.useReferees(getContext()) == false ) {
            ViewUtil.hideViewsForEver(this, bTrueGoneFalseInvisible
                    , R.id.lblReferee
                    , R.id.ll_referees
            );
        }
        if ( Brand.supportsDoubleServeSequence() == false ) {
            ViewUtil.hideViewsForEver(this, R.id.ll_doubleServeSequence);
        }
        if ( Brand.supportsTiebreakFormat() == false ) {
            ViewUtil.hideViewsForEver(this, R.id.llTieBreakFormat);
        }
        if ( Brand.isGameSetMatch() == false ) {
            ViewUtil.hideViewsForEver(this, R.id.llFinalSetFinish);
            ViewUtil.hideViewsForEver(this, R.id.llChangesSidesWhen);
        } else {
            ViewUtil.hideViewsForEver(this, R.id.llHandicapFormat);
            ViewUtil.hideViewsForEver(this, R.id.llScoringType);
            ViewUtil.hideViewsForEver(this, R.id.llDisciplineStart);
            ViewUtil.hideViewsForEver(this, R.id.llNumberOfServesPerPlayer);
            ViewUtil.hideViewsForEver(this, R.id.ll_AnnouncementLanguage);
        }
        if ( PreferenceValues.useWarmup(getContext()) == false ) {
            ViewUtil.hideViewsForEver(this, R.id.lblWarmupDuration);
            ViewUtil.hideViewsForEver(this, R.id.spWarmupDuration);
            ViewUtil.hideViewsForEver(this, R.id.cbWarmupDuration);
        }
        return true;
    }

    /** invoked from com.doubleyellow.scoreboard.match.Match */
    void setPlayers(String sA, String sB, String sCountryA, String sCountryB, String sAvatarA, String sAvatarB, String sClubA, String sClubB, JSONArray lTeamPlayersA, JSONArray lTeamPlayersB) {
        int iNames = 0;
        if ( StringUtil.isNotEmpty(sA) ) {
            if ( m_bIsDoubles ) {
                String[] saNames = sA.split("/");
                txtPlayerA.setText(saNames[0]);
                if ( saNames.length > 1 ) {
                    txtPlayerA2.setText(saNames[1]);
                }
            } else {
                txtPlayerA.setText(sA);
            }
            iNames++;
        } else {
            if ( JsonUtil.isNotEmpty(lTeamPlayersA) && txtPlayerA instanceof AutoCompleteTextView ) {
                AutoCompleteTextView actv = (AutoCompleteTextView) txtPlayerA;
                actv.setAdapter(getStringArrayAdapter(getContext(), JsonUtil.asListOfStrings(lTeamPlayersA), null));
                actv.setThreshold(0);
            }
        }
        if ( StringUtil.isNotEmpty(sB) ) {
            if ( m_bIsDoubles ) {
                String[] saNames = sB.split("/");
                txtPlayerB.setText(saNames[0]);
                if ( saNames.length > 1 ) {
                    txtPlayerB2.setText(saNames[1]);
                }
            } else {
                txtPlayerB.setText(sB);
            }
            iNames++;
        } else {
            if ( JsonUtil.isNotEmpty(lTeamPlayersB) && txtPlayerB instanceof AutoCompleteTextView ) {
                AutoCompleteTextView actv = (AutoCompleteTextView) txtPlayerB;
                actv.setAdapter(getStringArrayAdapter(getContext(), JsonUtil.asListOfStrings(lTeamPlayersB), null));
                actv.setThreshold(0);
            }
        }

        if ( (txtCountryA != null) && (txtCountryB != null) ) {
            if ( StringUtil.isNotEmpty(sCountryA) ) {
                txtCountryA.setText(sCountryA);
                if ( txtCountryA instanceof CountryTextView) {
                    CountryTextView textView = (CountryTextView) this.txtCountryA;
                    textView.setCountryCode(sCountryA);
                }
                PreferenceValues.downloadImage(getContext(), null, sCountryA);
            }
            if ( StringUtil.isNotEmpty(sCountryB) ) {
                txtCountryB.setText(sCountryB);
                if ( txtCountryB instanceof CountryTextView) {
                    CountryTextView textView = (CountryTextView) this.txtCountryB;
                    textView.setCountryCode(sCountryB);
                }
                PreferenceValues.downloadImage(getContext(), null, sCountryB);
            }
            if ( StringUtil.areAllEmpty(sCountryA, sCountryB) ) {
                if ( PreferenceValues.useCountries(getContext()) == false ) {
                    ViewUtil.hideViews(this, R.id.ll_match_countries);
                }
            } else {
                if ( bCountriesCollapsed ) {
                    findViewById(R.id.lblCountries).callOnClick(); // to expand the parent area by default
                    bCountriesCollapsed = false;
                }
            }
        }

        saAvatars[Player.A.ordinal()] = sAvatarA;
        saAvatars[Player.B.ordinal()] = sAvatarB;
        for(Player p: Player.values() ) {
            if ( StringUtil.isNotEmpty(saAvatars[p.ordinal()]) ) {
                PreferenceValues.downloadAvatar(getContext(), null, saAvatars[p.ordinal()]);
            }
        }

        if ( (txtClubA != null) && (txtClubB != null) ) {
            if ( StringUtil.areAllEmpty(sClubA, sClubB) == false ) {
                if ( StringUtil.isNotEmpty(sClubA) ) {
                    txtClubA.setText(sClubA);
                }
                if ( StringUtil.isNotEmpty(sClubB) ) {
                    txtClubB.setText(sClubB);
                }
                if ( bClubsCollapsed ) {
                    findViewById(R.id.lblClubs).callOnClick(); // to expand the parent area by default
                    bClubsCollapsed = false;
                }
            }
        }

        // try setting the focus to something else than the playertextview elements to prevent keyboard to pop-up if names
        // are already specified
        if ( iNames == 2 ) {
            View viewById = findViewById(R.id.spNumberOfGamesToWin);
            if ( viewById != null ) {
                viewById.setFocusable(true);
                viewById.setFocusableInTouchMode(true);
                requestFocusFor(viewById);
            }
        }
    }
  //private NumberPicker         npGameEndScore = null;
    private Spinner              spGameEndScore; // e.g. used in landscape but not in portrait
    private Spinner              spNumberOfGamesToWin;
    private Spinner              spNumberOfServesPerPlayer;
    private Spinner              spTieBreakFormat;
    private Spinner              spFinalSetFinish;
    private Spinner              spHandicap;
    private Spinner              spDisciplineStart;
    private Spinner              spWarmupDuration;
    private ToggleButton         cbWarmupDuration; /* If only 2 options available */
    private Spinner              spPauseDuration;
    private ToggleButton         cbPauseDuration; /* If only 2 options available */
    private ToggleButton         tbBestOf_or_TotalOf;
    private Spinner              spAnnouncementLanguage;
    private Spinner              spDoublesServeSequence;
    private CompoundButton       cbUseEnglishScoring;
    private CheckBox             cbUseLiveScoring;
    private CompoundButton       cbUseGoldenPoint;
    private PreferenceACTextView txtRefereeName;
    private PreferenceACTextView txtMarkerName;
    private PreferenceACTextView txtEventName;
    private PreferenceACTextView txtEventDivision;
    private PreferenceACTextView txtEventRound;
    private PreferenceACTextView txtEventLocation;
    private PreferenceACTextView txtCourt;
    private TextView             txtEventID;
    private EditText             txtPlayerA;  // singles and doubles
    private EditText             txtPlayerA2; // doubles
    private EditText             txtPlayerB;  // singles and doubles
    private EditText             txtPlayerB2; // doubles
    private TextView             txtCountryA;
    private TextView             txtCountryB;
    private String[]             saAvatars = new String[2];
    private PreferenceACTextView txtClubA;
    private PreferenceACTextView txtClubB;
    private CheckBox[]           cbChangesSidesWhen;

/*
    private TextView.OnEditorActionListener onEditorActionListener = new TextView.OnEditorActionListener() {
        @Override public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if ((actionId == EditorInfo.IME_ACTION_SEARCH) || (actionId == EditorInfo.IME_ACTION_DONE) ) {
                // close a possible open autocomplete
                if ( v instanceof AutoCompleteTextView ) {
                    AutoCompleteTextView acv = (AutoCompleteTextView) v;
                    if ( acv.isPopupShowing() ) {
                        acv.dismissDropDown();
                    }
                }
            }
            return false;
        }
    };
*/

    private TextView[] tvsPlayers   = null;
    private TextView[] tvsCountries = null;
    private TextView[] tvsClubs     = null;
    private View[]     tvsReferee   = null;
    private TextView[] tvsEvent     = null;
    private Button[]   btnsColor    = null;
    @ColorInt
    private Integer    m_iNoColor   = null;

    boolean clearEventFields() {
        return clearFields(tvsEvent);
    }

    boolean clearRefereeFields() {
        return clearFields(tvsReferee);
    }

    boolean clearAllFields() {
        clearPlayerFields();
        clearClubFields();
        clearCountryFields();
        clearEventFields();
        clearRefereeFields();
        return true;
    }
    boolean clearPlayerFields() {
/*
        for (TextView tv: tvsPlayers ) {
            Log.d(TAG, String.format("%s %d .nfd : %d", tv.getHint(), tv.getId(), tv.getNextFocusDownId()));
        }
*/
        return clearFields(tvsPlayers);
    }

    boolean clearCountryFields() {
        return clearFields(tvsCountries);
    }
    boolean clearClubFields() {
        return clearFields(tvsClubs);
    }

    private static int iForceTextSize = 0; // only used for enum spinners for now
    private static ArrayAdapter<String> getStringArrayAdapter(Context context, List<String> list, TextView tvRefTxtSize) {
        return getStringArrayAdapter(context, list, tvRefTxtSize, null);
    }
    private static ArrayAdapter<String> getStringArrayAdapter(Context context, List<String> list, TextView tvRefTxtSize, int[] iaDisabled) {
        if ( tvRefTxtSize != null ) {
            iForceTextSize = (int) tvRefTxtSize.getTextSize();
        }
        return EnumSpinner.getStringArrayAdapter(context, list, iForceTextSize, iaDisabled);
    }
    private <T extends Enum<T>> void initEnumSpinner(Spinner spinner, Class<T> clazz, T value, T excludeValue, int iResourceDisplayValues) {
        initEnumSpinner(spinner, clazz, value, excludeValue, iResourceDisplayValues, null);
    }
    private <T extends Enum<T>> void initEnumSpinner(Spinner spinner, Class<T> clazz, T value, T excludeValue, int iResourceDisplayValues, int[] iaDisabled) {
        EnumSpinner.init(spinner, getContext(), clazz, value, excludeValue, iResourceDisplayValues, iForceTextSize, iaDisabled);
    }
    private void initTextViews(View[] tvs) {
        for ( View v:tvs ) {
            NextFocusDownListener.mimicPreSdk19(this, v);
            //v.setOnEditorActionListener(onEditorActionListener);
        }
    }

    private boolean clearFields(View[] tvs) {
        if ( tvs == null ) {
            return false;
        }
        for(View v:tvs) {
            if ( v instanceof TextView ) {
                TextView tv = (TextView) v;
                ViewUtil.emptyField(tv);
                if (v instanceof PreferenceACTextView) {
                    PreferenceACTextView v1 = (PreferenceACTextView) v;
                    v1.useLastValueAsDefault(false);
                    v1.getTextAndPersist(true);
                }
                if ( v instanceof CountryTextView ) {
                    CountryTextView ctv = (CountryTextView) v;
                    ctv.setCountryCode("");
                }
            }
        }
        return true;
    }

    private boolean m_bIsDoubles = false;

    /**
     * Is null for when entering match manually from match fragment, else it is used to
     * - pass on 'uneditable' info
     * - pre-set match format preferences passed on from feed
     **/
    private Model   m_model      = null;

    /**
     * @param model is null when used on MatchFragment
     */
    public void init(boolean bIsDoubles, final Model model) {
        final Context context = getContext();

        //iForceTextSize = IBoard.iTxtSizePx_FinishedGameScores;

        m_bIsDoubles = bIsDoubles; // attrs.getAttributeBooleanValue(APPLICATION_NS, "isDoubles", false);
        m_model = model;

        LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        // wrap it into a scroll view so that user with small screens and large fonts.... yada yada
        ScrollView sv = new ScrollView(context);
        inflater.inflate(m_layout.getLayoutResId(), sv, true);
        this.addView(sv);

        List<Integer> lViewsToHide = new ArrayList<Integer>();
        if ( bIsDoubles ) {
            lViewsToHide.add(R.id.ll_match_singles);
            if ( Brand.supportsDoubleServeSequence() == false ) {
                lViewsToHide.add(R.id.ll_doubleServeSequence);
            }
        } else {
            lViewsToHide.add(R.id.ll_match_doubles);
            lViewsToHide.add(R.id.ll_doubleServeSequence);
        }
        ViewUtil.hideViews(this, lViewsToHide);

        // get references for late use
        txtEventName     = (PreferenceACTextView) findViewById(R.id.match_event);
        txtEventDivision = (PreferenceACTextView) findViewById(R.id.match_division);
        txtEventRound    = (PreferenceACTextView) findViewById(R.id.match_round);
        txtEventLocation = (PreferenceACTextView) findViewById(R.id.match_location);
        txtEventID       = (TextView)             findViewById(R.id.match_id);
        txtCourt         = (PreferenceACTextView) findViewById(R.id.match_court);
        tvsEvent = new TextView[]{txtEventName,txtEventDivision,txtEventRound,txtEventLocation, txtCourt};

        txtRefereeName   = (PreferenceACTextView) findViewById(R.id.match_referee);
        txtMarkerName    = (PreferenceACTextView) findViewById(R.id.match_marker);
        spAnnouncementLanguage = (Spinner) findViewById(R.id.spAnnouncementLanguage);
        tvsReferee = new View[]{txtRefereeName,txtMarkerName, spAnnouncementLanguage};

        if ( bIsDoubles ) {
            txtPlayerA  = (EditText) findViewById(R.id.match_playerA1);
            txtPlayerA2 = (EditText) findViewById(R.id.match_playerA2);
            txtPlayerB  = (EditText) findViewById(R.id.match_playerB1);
            txtPlayerB2 = (EditText) findViewById(R.id.match_playerB2);

            if ( txtPlayerA instanceof PlayerTextView) {
                PlayerTextView ptvA = (PlayerTextView) txtPlayerA;
                ptvA.addListener(new DoublesNameCopier(txtPlayerA2));
            }
            if ( txtPlayerB instanceof PlayerTextView) {
                PlayerTextView ptvB = (PlayerTextView) txtPlayerB;
                ptvB.addListener(new DoublesNameCopier(txtPlayerB2));
            }
            tvsPlayers  = new TextView[]{txtPlayerA,txtPlayerA2,txtPlayerB,txtPlayerB2};
            clearPlayerFields();
        } else {
            txtPlayerA = (EditText) findViewById(R.id.match_playerA);
            txtPlayerB = (EditText) findViewById(R.id.match_playerB);
            tvsPlayers = new TextView[]{txtPlayerA,txtPlayerB};
            clearPlayerFields();
        }
        if ( txtPlayerA instanceof PlayerTextView ) {
            PlayerTextView p = (PlayerTextView) txtPlayerA;
            p.addSibling((PlayerTextView) txtPlayerB);
            if ( bIsDoubles ) {
                p.addSibling((PlayerTextView) txtPlayerA2);
                p.addSibling((PlayerTextView) txtPlayerB2);
            }
        }

        txtCountryA  = (EditText)             findViewById(R.id.match_countryA);
        txtCountryB  = (EditText)             findViewById(R.id.match_countryB);
        txtClubA     = (PreferenceACTextView) findViewById(R.id.match_clubA);
        txtClubB     = (PreferenceACTextView) findViewById(R.id.match_clubB);
        tvsCountries = new TextView[]{txtCountryA,txtCountryB};
        tvsClubs     = new TextView[]{txtClubA   ,txtClubB   };
        initCountries(context, txtCountryA, txtPlayerA, bIsDoubles);
        initCountries(context, txtCountryB, txtPlayerB, bIsDoubles);
        initClubs(context, txtClubA, txtPlayerA);
        initClubs(context, txtClubB, txtPlayerB);

        initTextViews(tvsEvent);
        initTextViews(tvsReferee);
        initTextViews(tvsPlayers);
        //initTextViews(tvsCountries);
        //initTextViews(tvsClubs);
        initForEasyClearingFields();

        if ( bIsDoubles ) {
            btnsColor  = new Button[] { findViewById(R.id.match_colorA12), findViewById(R.id.match_colorB12) };
        } else {
            btnsColor  = new Button[] { findViewById(R.id.match_colorA  ), findViewById(R.id.match_colorB  ) };
        }
        m_iNoColor = ColorPrefs.getTarget2colorMapping(context).get(ColorPrefs.ColorTarget.playerButtonBackgroundColor); // TODO : scoreButtonBackgroundColor ?
        for(final Player p: Player.values() ) {
            Button btnColor = btnsColor[p.ordinal()];
            if ( btnColor == null ) { continue; }
            btnColor.setTag(m_iNoColor);
            btnColor.setTag(R.string.lbl_player, p);
            btnColor.setOnClickListener(m_onColorButtonClicker);
        }

        // optionally get player names passed in from a match selection
        requestFocusFor(txtPlayerA); // does not work if focus was impossible (.e.g not the initial tab in MatchTabbed)

        // take values from preferences as default values
        int iGameEndPref        = Model.UNDEFINED_VALUE;
        int iNrOfGamesToWinPref = Model.UNDEFINED_VALUE;
        if ( m_model != null ) {
            iGameEndPref        = m_model.getNrOfPointsToWinGame();
            iNrOfGamesToWinPref = m_model.getNrOfGamesToWinMatch();
            if ( m_model instanceof GSMModel ) {
                GSMModel m = (GSMModel) m_model;
                iGameEndPref = m.getNrOfGamesToWinSet();
            }
        }
        if ( iGameEndPref == Model.UNDEFINED_VALUE ) {
            iGameEndPref = PreferenceValues.numberOfPointsToWinGame(context);
        }
        if ( iNrOfGamesToWinPref == Model.UNDEFINED_VALUE ) {
            iNrOfGamesToWinPref = PreferenceValues.numberOfGamesToWinMatch(context);
        }
/*
        npGameEndScore = null; // (NumberPicker) findViewById(R.id.npGameEndScore);
        if ( npGameEndScore != null ) {
            npGameEndScore.setMinValue(2);
            npGameEndScore.setMaxValue(Math.max(21, iGameEndPref));
            npGameEndScore.setValue(iGameEndPref);
            npGameEndScore.setWrapSelectorWheel(false);
        }
*/

        spGameEndScore = (Spinner) findViewById(R.id.spGameEndScore);
        initGameEndScore(context, spGameEndScore, iGameEndPref, 2, txtPlayerA);

        int max = Math.max(iNrOfGamesToWinPref, 11);
/*
        npNumberOfGamesToWin = (NumberPicker) findViewById(R.id.npNumberOfGamesToWin);

        if ( npNumberOfGamesToWin != null ) {
            String[] saValues = new String[max];
            for(int iIdx=0; iIdx < max;iIdx++ ) {
                saValues[iIdx] = "" + ((iIdx+1) * 2 - 1);
            }
            npNumberOfGamesToWin.setMinValue(0);
            npNumberOfGamesToWin.setMaxValue(saValues.length - 1);
            npNumberOfGamesToWin.setDisplayedValues(saValues);
            npNumberOfGamesToWin.setValue(iNrOfGamesToWinPref-1);
            npNumberOfGamesToWin.setWrapSelectorWheel(false);
        }
*/

        spNumberOfGamesToWin = (Spinner) findViewById(R.id.spNumberOfGamesToWin);
        initNumberOfGamesToWin(context, spNumberOfGamesToWin, iNrOfGamesToWinPref, max, txtPlayerA);

        spNumberOfServesPerPlayer = (Spinner) findViewById(R.id.spNumberOfServesPerPlayer);
        initNumberOfServesPerPlayer(context, spNumberOfServesPerPlayer, PreferenceValues.numberOfServesPerPlayer(context), 1, 5, txtPlayerA);
        {
            TieBreakFormat tbfPref = PreferenceValues.getTiebreakFormat(context);
            spTieBreakFormat = (Spinner) findViewById(R.id.spTieBreakFormat);
            if ( spTieBreakFormat != null ) {
                // Currently instanceof Spinner, not EnumSpinner
                if ( spTieBreakFormat instanceof EnumSpinner ) {
                    EnumSpinner<TieBreakFormat> sp = (EnumSpinner<TieBreakFormat>) spTieBreakFormat;
                    sp.setSelected(tbfPref);
                } else {
                    int[] iaDisabled = null;
                    if ( Brand.isBadminton() ) {
                        // remove options that are not for badminton
                        iaDisabled = new int[] { TieBreakFormat.SelectOneOrTwo.ordinal(), TieBreakFormat.SelectOneOrThree.ordinal(), TieBreakFormat.SelectOneTwoOrThree.ordinal()  };
                    }
                    initEnumSpinner(spTieBreakFormat, TieBreakFormat.class, tbfPref, null, R.array.tiebreakFormatDisplayValues, iaDisabled);
                }
            }
        }
        {
            FinalSetFinish fsfPref = PreferenceValues.getFinalSetFinish(context);
            spFinalSetFinish = (Spinner) findViewById(R.id.spFinalSetFinish);
            if ( spFinalSetFinish != null ) {
                // Currently instanceof Spinner, not EnumSpinner
                if ( spFinalSetFinish instanceof EnumSpinner ) {
                    EnumSpinner<FinalSetFinish> sp = (EnumSpinner<FinalSetFinish>) spFinalSetFinish;
                    sp.setSelected(fsfPref);
                } else {
                    int[] iaDisabled = null;
                    initEnumSpinner(spFinalSetFinish, FinalSetFinish.class, fsfPref, null, R.array.finalSetFinishDisplayValues, iaDisabled);
                }
            }
        }

        int iTotNrOfValuesToSelectFrom = 0;
        {
            List<String> lValues = Preferences.syncAndClean_warmupValues(context);
            int iDuration = PreferenceValues.getWarmupDuration(context);

            cbWarmupDuration = (ToggleButton) findViewById(R.id.cbWarmupDuration);
            spWarmupDuration = (Spinner)      findViewById(R.id.spWarmupDuration);
            iTotNrOfValuesToSelectFrom += initDuration(context, cbWarmupDuration, spWarmupDuration, txtPlayerA, lValues, iDuration);
        }
        {
            List<String> lValues = Preferences.syncAndClean_pauseBetweenGamesValues(context);
            int iDuration = PreferenceValues.getPauseDuration(context);

            cbPauseDuration = (ToggleButton) findViewById(R.id.cbPauseDuration);
            spPauseDuration = (Spinner)      findViewById(R.id.spPauseDuration);
            iTotNrOfValuesToSelectFrom += initDuration(context, cbPauseDuration, spPauseDuration, txtPlayerA, lValues, iDuration);
        }
        if ( iTotNrOfValuesToSelectFrom <= 2 ) {
            ViewParent parent = spPauseDuration.getParent();
            if ( parent instanceof ViewGroup) {
                ((ViewGroup) parent).setVisibility(GONE);
            }
        }

        tbBestOf_or_TotalOf = (ToggleButton) findViewById(R.id.tbBestOf_or_TotalOf);
        if ( tbBestOf_or_TotalOf != null ) {
            if ( m_model != null ) {
                tbBestOf_or_TotalOf.setChecked(m_model.playAllGames());
            } else {
                tbBestOf_or_TotalOf.setChecked(PreferenceValues.getBoolean(PreferenceKeys.playAllGames, context, R.bool.playAllGames_default));
            }
        }

        {
            if ( PreferenceValues.useOfficialAnnouncementsFeature(context).equals(Feature.DoNotUse) ) {
                ViewUtil.hideViewsForEver(this, R.id.ll_AnnouncementLanguage);
            } else {
                spAnnouncementLanguage = (Spinner) findViewById(R.id.spAnnouncementLanguage);
                if (spAnnouncementLanguage != null) {
                    AnnouncementLanguage language = PreferenceValues.officialAnnouncementsLanguage(context);
                    if (spAnnouncementLanguage instanceof EnumSpinner) {
                        EnumSpinner<AnnouncementLanguage> sp = (EnumSpinner<AnnouncementLanguage>) spAnnouncementLanguage;
                        sp.setSelected(language);
                    } else {
                        initEnumSpinner(spAnnouncementLanguage, AnnouncementLanguage.class, language, null, 0);
                    }
                }
            }
        }
        {
            HandicapFormat hdcPref = PreferenceValues.getHandicapFormat(context);
            spHandicap = (Spinner) findViewById(R.id.spHandicapFormat);
            if ( spHandicap != null ) {
                if ( spHandicap instanceof EnumSpinner ) {
                    EnumSpinner<HandicapFormat> sp = (EnumSpinner<HandicapFormat>) spHandicap;
                    sp.setSelected(hdcPref);
                } else {
                    initEnumSpinner(spHandicap, HandicapFormat.class, hdcPref, null, R.array.handicapFormatDisplayValues);
                }
            }
        }
        {
            EnumSet<Sport> sports = PreferenceValues.getDisciplineSequence(context);
            spDisciplineStart = (Spinner) findViewById(R.id.spDisciplineStart);
            Sport first = sports.iterator().next();
            if ( spDisciplineStart != null ) {
                if ( spDisciplineStart instanceof EnumSpinner ) {
                    EnumSpinner<Sport> sp = (EnumSpinner<Sport>) spDisciplineStart;
                    sp.setSelected(first);
                } else {
                    initEnumSpinner(spDisciplineStart, Sport.class, first, null, 0);
                }
            }
        }

        if ( bIsDoubles ) {
            if ( Brand.supportsDoubleServeSequence() ) {
                DoublesServeSequence dssPref = PreferenceValues.getDoublesServeSequence(context);
                spDoublesServeSequence = (Spinner) findViewById(R.id.spDoublesServeSequence);
                if ( spDoublesServeSequence != null ) {
                    if ( spDoublesServeSequence instanceof EnumSpinner ) {
                        EnumSpinner<DoublesServeSequence> sp = (EnumSpinner<DoublesServeSequence>) spDoublesServeSequence;
                        sp.setSelected(dssPref); // TODO: set selected
                    } else {
                        int[] iaDisabled = null;
                        if ( false && Brand.isBadminton() ) {
                            // remove options that are not for badminton
                            iaDisabled = new int[] { DoublesServeSequence.A2B1B2_then_A1A2B1B2.ordinal(), DoublesServeSequence.A1A2B1B2.ordinal(), DoublesServeSequence.A1B1A1B1.ordinal()  };
                        }
                        initEnumSpinner(spDoublesServeSequence, DoublesServeSequence.class, dssPref, DoublesServeSequence.NA, R.array.doublesServeSequence, iaDisabled);
                    }
                }
            }
        }

        cbUseLiveScoring = (CheckBox) findViewById(R.id.cbUseLivescore);
        if ( cbUseLiveScoring != null ) {
            ShareMatchPrefs forLiveScore = PreferenceValues.isConfiguredForLiveScore(context);
            cbUseLiveScoring.setChecked(forLiveScore!=null);
        }

        cbUseEnglishScoring = (CompoundButton) findViewById(R.id.useHandInHandOutScoring);
        if ( cbUseEnglishScoring != null ) {
            boolean useHandInHandOutScoring = PreferenceValues.useHandInHandOutScoring(context);
            cbUseEnglishScoring.setChecked(useHandInHandOutScoring);
        }
        cbUseGoldenPoint = (CompoundButton) findViewById(R.id.useGoldenPoint);
        if ( cbUseGoldenPoint != null ) {
            boolean useGoldenPoint = PreferenceValues.useGoldenPoint(context);
            cbUseGoldenPoint.setChecked(useGoldenPoint);
        }
        // initialize checkboxes array for 'Change Sides When'
        Feature ffChangesSide = PreferenceValues.useChangeSidesFeature(context);
        if ( Feature.DoNotUse.equals(ffChangesSide) == false ) {
            LinearLayout llChangesSidesWhen = findViewById(R.id.llChangesSidesWhen);
            if ( llChangesSidesWhen != null ) {
                LinearLayout llChangesSidesCheckboxes = llChangesSidesWhen.findViewById(R.id.llChangesSidesCheckboxes);
                if ( llChangesSidesCheckboxes != null ) {
                    EnumSet<ChangeSidesWhen_GSM> checkedValues = PreferenceValues.changeSidesWhen_GSM(context);
                    ChangeSidesWhen_GSM[] values = ChangeSidesWhen_GSM.values();
                    String [] saDisplayValues = context.getResources().getStringArray(R.array.changeSidesWhen_GSM_DisplayValues);
                    cbChangesSidesWhen = new CheckBox[values.length];
                    for (int i = 0; i < values.length; i++) {
                        CheckBox cb = new CheckBox(context);
                        cb.setText(saDisplayValues[i]);
                        ChangeSidesWhen_GSM eValue = values[i];
                        boolean bChecked = checkedValues.contains(eValue);
                        cb.setChecked(bChecked);
                        cb.setTag(eValue);

                        cbChangesSidesWhen[i] = cb;
                        llChangesSidesCheckboxes.addView(cb);
                    }
                }
            }
        }
    }

    /** must be invoked after the view is added to its parent, else the color of the button will not show up */
    void initPlayerColors() {
        for(final Player p: Player.values() ) {
            Button btnColor = btnsColor[p.ordinal()];
            if ( btnColor == null ) { continue; }
            btnColor.setTag(R.string.lbl_player, p);

            Integer iInitialColor = m_iNoColor;
            if ( m_model != null ) {
                // e.g. when editing a match in progress, or getting settings from mylist or feed
                String sPlayerColor = m_model.getColor(p);
                if ( StringUtil.isNotEmpty(sPlayerColor) ) {
                    iInitialColor = Color.parseColor(sPlayerColor);
                } else if ( m_model.hasStarted() == false ) {
                    // new manual match
                    PlayerColorsNewMatch playerColorsNewMatch = PreferenceValues.playerColorsNewMatch(getContext());
                    iInitialColor = ColorPrefs.getInitialPlayerColor(getContext(), p, iInitialColor, playerColorsNewMatch);
                }
            } else {
                // new match
                PlayerColorsNewMatch playerColorsNewMatch = PreferenceValues.playerColorsNewMatch(getContext());
                iInitialColor = ColorPrefs.getInitialPlayerColor(getContext(), p, iInitialColor, playerColorsNewMatch);
            }
            btnColor.setTag(iInitialColor);
            ColorUtil.setBackground(btnColor, iInitialColor); // todo: already coming from feed, e.g. a club color
            int blackOrWhiteFor = ColorUtil.getBlackOrWhiteFor(iInitialColor);
            btnColor.setTextColor(blackOrWhiteFor);
            btnColor.invalidate();
            btnColor.setText(btnColor.getHint());
        }
    }

    private OnClickListener m_onColorButtonClicker = new OnClickListener() {
        private Player m_forPlayer = null;
        private String m_sColor    = null;
        private int    m_iColor    = 0;
        @Override public void onClick(View btn) {
            final Context context     = getContext();
            final Button  colorButton = (Button) btn;
            m_forPlayer = (Player) colorButton.getTag(R.string.lbl_player);
            m_iColor    = (int) colorButton.getTag();
            m_sColor    = ColorUtil.getRGBString(m_iColor);

            // prepare the view, and if a color is chosen in the view
            ColorPickerView cpv = getColorPickerView(context);
            if ( m_sColor != null ) {
                cpv.setColor(Color.parseColor(m_sColor));
            }

            // update clicked button when dialog is closed
            DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case DialogInterface.BUTTON_NEGATIVE:
                            break;
                        case DialogInterface.BUTTON_NEUTRAL:
                            colorButton.setTag(m_iNoColor);
                            ColorUtil.setBackground(colorButton, m_iNoColor);
                            colorButton.invalidate();
                            colorButton.setText(""); // hint text should still be showing
                            int noColorTxt = ColorUtil.getBlackOrWhiteFor(m_iNoColor);
                            colorButton.setTextColor(noColorTxt);
                            break;
                        case DialogInterface.BUTTON_POSITIVE:
                            colorButton.setTag(m_iColor);
                            ColorUtil.setBackground(colorButton, m_iColor);
                            colorButton.invalidate();
                            colorButton.setText(colorButton.getHint());
                            int colorTxt = ColorUtil.getBlackOrWhiteFor(m_iColor);
                            colorButton.setTextColor(colorTxt);
                            break;
                    }
                }
            };
            MyDialogBuilder adb = new MyDialogBuilder(context);
            if ( ViewUtil.isWearable(context) == false ) {
                String sTitle = context.getString(R.string.lbl_color) + ": " + tvsPlayers[m_forPlayer.ordinal()].getText(); // not for wearable
                adb.setTitle(sTitle);
            }
            adb.setPositiveButton(R.string.cmd_ok    , listener)
               .setNeutralButton (R.string.cmd_none  , listener)
               .setNegativeButton(R.string.cmd_cancel, listener)
               .setView((View) cpv);

            // ensure cancel button will have the current choosen color
            ButtonUpdater buttonUpdater = new ButtonUpdater(context, AlertDialog.BUTTON_NEGATIVE, m_iColor, AlertDialog.BUTTON_POSITIVE, m_iColor);
            final AlertDialog dialog = adb.show(buttonUpdater);

            // update OK button when a color is selected
            cpv.setOnColorChangedListener(new ColorPickerView.OnColorChangedListener() {
                @Override public void onColorChanged(int newColor) {
                    m_iColor = newColor;
                    m_sColor = ColorUtil.getRGBString(m_iColor);
                    Button btnOK = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                    ColorUtil.setBackground(btnOK, Color.parseColor(m_sColor));
                    btnOK.setTextColor(ColorUtil.getBlackOrWhiteFor(m_sColor));
                }
            });
        }
    };

    private ColorPickerView getColorPickerView(Context context) {
        ColorPickerView cpv = new SatValHueColorPicker(context);
        //if ( false ) cpv = new LineColorPicker(context, null);

        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        ((View) cpv).setLayoutParams(layoutParams);
        return cpv;
    }


    public static int initDuration(Context context, ToggleButton cbDuration, Spinner spDuration, TextView tvRefTxtSize, List<String> lValues, int iDuration) {
        if ( (cbDuration != null) && ListUtil.size(lValues) == 2 ) {
            cbDuration.setVisibility(VISIBLE);
            if ( spDuration != null ) {
                spDuration.setVisibility(GONE); // assume a related spinner will allow user to set more than 2 values
            }
            cbDuration.setTextOff(lValues.get(0));
            cbDuration.setTextOn(lValues.get(1));
            cbDuration.setChecked(lValues.get(1).equals(String.valueOf(iDuration)));
            return 2;
        }

        if ( spDuration != null ) {
            spDuration.setVisibility(VISIBLE);
            if ( cbDuration != null ) {
                cbDuration.setVisibility(GONE); // assume a related spinner will allow user to set more than 2 values
            }

            ArrayAdapter<String> dataAdapter = getStringArrayAdapter(context, lValues, tvRefTxtSize);
            spDuration.setAdapter(dataAdapter);
            spDuration.setSelection(lValues.indexOf(String.valueOf(iDuration)));
            return ListUtil.size(lValues);
        }
        return 0;
    }

    private static void initClubs(final Context context, final PreferenceACTextView spClub, final TextView txtPlayer) {
        if ( spClub == null ) { return; }
        if ( txtPlayer != null ) {
            spClub.setOnFocusChangeListener(new OnFocusChangeListener() {
                @Override public void onFocusChange(View v, boolean hasFocus) {
                    if ( (v instanceof AutoCompleteTextView) && (hasFocus == false) && ViewUtil.areAllEmpty(txtPlayer)) {
                        AutoCompleteTextView actv = (AutoCompleteTextView) v;
                        txtPlayer.setText(actv.getText());
                    }
                }
            });
        }
    }

    private static void initCountries(final Context context, final TextView spCountry, final TextView txtPlayer, boolean bIsDoubles) {
        if ( spCountry == null ) { return; }
        if ( spCountry instanceof CountryTextView ) {
            int iSuggestAfterAtLeast = PreferenceValues.numberOfCharactersBeforeAutocompleteCountry(context);

            // tmp: fix old default from 3 to 1
            int iRunCount = PreferenceValues.getRunCount(context, PreferenceKeys.numberOfCharactersBeforeAutocompleteCountry);
            if ( (iRunCount <= 1) && (iSuggestAfterAtLeast == 3) /* Old default */ ) {
                iSuggestAfterAtLeast = 1;
                RWValues.setNumber(PreferenceKeys.numberOfCharactersBeforeAutocompleteCountry, context, iSuggestAfterAtLeast);
            }

            final CountryTextView ac = (CountryTextView) spCountry;
            ac.setThreshold(iSuggestAfterAtLeast);
            ac.setAutoCompleteLayoutResourceId(R.layout.expandable_match_selector_item);
            if ( txtPlayer != null ) {
                ac.addListener(new CountryTextView.Listener() {
                    @Override public void onSelected(String sCode, String sCountryName) {
                        if ( ViewUtil.areAllEmpty(txtPlayer) ) {
                            txtPlayer.setText(sCountryName);
                        }
                    }
                });

                if ( PreferenceValues.useCountries(context) /*&& (bIsDoubles == false)*/ ) {
                    // fill country if player is selected from autocomplete list with country code in it
                    if (txtPlayer instanceof PlayerTextView) {
                        final PlayerTextView ptv = (PlayerTextView) txtPlayer;
                        ptv.addListener(new CountryCodeCopier(ac));
                    }
                }
            }
            ac.addListener(new CountryTextView.Listener() {
                @Override public void onSelected(String sCode, String sCountryName) {
                    PreferenceValues.downloadImage(context, null, sCode);
                }
            });
        }
    }

    private static class CountryCodeCopier implements PlayerTextView.Listener {
        private CountryTextView ac = null;
        CountryCodeCopier(CountryTextView ac) {
            this.ac = ac;
        }
        @Override public void onSelected(String sName, PlayerTextView ptv) {
            if ( sName.matches(CountryTextView.S_EXTRACT_COUNTRYCODE_REGEXP) ) {
                String sCountryCode = sName.replaceAll(CountryTextView.S_EXTRACT_COUNTRYCODE_REGEXP, "$2");
                if ( ViewUtil.areAllEmpty(ac) && StringUtil.isNotEmpty(sCountryCode) ) {
                    ac.setCountryCode(sCountryCode);
                    ptv.setText(sName.replaceAll(CountryTextView.S_EXTRACT_COUNTRYCODE_REGEXP, "$1"));
                }
            }
        }
    }
    /** if for the first player of a doubles team a name like Player A1/Player A2 is choosen, split it and 'prepop' the second player */
    private static class DoublesNameCopier implements PlayerTextView.Listener {
        private EditText etOther = null;
        DoublesNameCopier(EditText etOther) {
            this.etOther = etOther;
        }
        @Override public void onSelected(String sName, PlayerTextView ptv) {
            String[] saNames = sName.split(Model.REGEXP_SPLIT_DOUBLES_NAMES);
            if ( saNames.length == 2 ) {
                ptv.setText(saNames[0]);
                if ( ViewUtil.areAllEmpty(etOther) ) {
                    // only copy if empty
                    etOther.setText(saNames[1]);
                }
            }
        }
    }
    /** Invoked from EditFormat as well */
    public static void initGameEndScore(Context context, final Spinner spGameEndScore, int iGameEndPref, int iValueOfset, TextView refTxtSize) {
        if ( spGameEndScore == null ) { return; }

        List<String> list = new ArrayList<String>();
        int iSelectedIndex = 0;
        for (int iIdx = 0; iIdx <= Math.max(19, iGameEndPref); iIdx++) {
            int iValue = iIdx + iValueOfset;
            if (iValue == iGameEndPref) {
                iSelectedIndex = iIdx;
            }
            list.add(" " + iValue + " ");
        }
        final String sMORE = context.getResources().getString(R.string.uc_more);
        list.add(sMORE);
        final ArrayAdapter<String> dataAdapter = getStringArrayAdapter(context, list, refTxtSize);
        spGameEndScore.setAdapter(dataAdapter);
        spGameEndScore.setSelection(iSelectedIndex);
        spGameEndScore.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (parent == spGameEndScore) {
                    Object selectedItem = spGameEndScore.getSelectedItem();
                    if ( selectedItem.toString().equals(sMORE)) {
                        dataAdapter.remove(sMORE); // add it again after numbers have been added
                        for (int i = position+2; i <= position + 101; i++) {
                            dataAdapter.add("" + i);
                        }
                        dataAdapter.add(sMORE);
                        dataAdapter.notifyDataSetChanged();
                    }
                }
            }

            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    /** Invoked from EditFormat as well */
    public static void initNumberOfGamesToWin(Context context, Spinner spNumberOfGamesToWin, int iNrOfGamesToWinPref, int max, TextView refTxtSize) {
        if ( spNumberOfGamesToWin == null ) { return; }
        List<String> list = new ArrayList<String>();
        int iSelectedIndex = 0;
        for ( int iIdx=0; iIdx < max; iIdx++ ) {
            int iNrOfGamesToWin = (iIdx + 1);
            int iBestOf = iNrOfGamesToWin * 2 - 1;
            if ( iNrOfGamesToWin == iNrOfGamesToWinPref) {
                iSelectedIndex = iIdx;
            }
            list.add("" + iBestOf);
        }
        ArrayAdapter<String> dataAdapter = getStringArrayAdapter(context, list, refTxtSize);
        spNumberOfGamesToWin.setAdapter(dataAdapter);
        spNumberOfGamesToWin.setSelection(iSelectedIndex);
    }

    public static void initNumberOfServesPerPlayer(Context context, Spinner sp, int iCurrent, int iMin, int max, TextView txtRefTxtSize) {
        if ( sp == null ) { return; }
        List<String> list = new ArrayList<String>();
        int iSelectedIndex = 0;
        for ( int iIdx=iMin; iIdx <= max; iIdx++ ) {
            if ( iIdx == iCurrent) {
                iSelectedIndex = iIdx-iMin;
            }
            list.add("" + iIdx);
        }
        ArrayAdapter<String> dataAdapter = getStringArrayAdapter(context, list, txtRefTxtSize);
        sp.setAdapter(dataAdapter);
        sp.setSelection(iSelectedIndex);
    }

    private void initForEasyClearingFields() {
        TextView lblPlayersD = (TextView) findViewById(R.id.lblPlayersD);
        if ( lblPlayersD != null ) {
            lblPlayersD.setOnLongClickListener(new OnLongClickListener() {
                @Override public boolean onLongClick(View view) {
                    clearPlayerFields();
                    return true;
                }
            });
        }
        TextView lblPlayersS = (TextView) findViewById(R.id.lblPlayersS);
        if ( lblPlayersS != null ) {
            lblPlayersS.setOnLongClickListener(new OnLongClickListener() {
                @Override public boolean onLongClick(View view) {
                    clearPlayerFields();
                    return true;
                }
            });
        }
        TextView lblCountries = (TextView) findViewById(R.id.lblCountries);
        if ( lblCountries != null ) {
            lblCountries.setOnLongClickListener(new OnLongClickListener() {
                @Override public boolean onLongClick(View view) {
                    clearCountryFields();
                    return true;
                }
            });
        }
        TextView lblClubs = (TextView) findViewById(R.id.lblClubs);
        if ( lblClubs != null ) {
            lblClubs.setOnLongClickListener(new OnLongClickListener() {
                @Override public boolean onLongClick(View view) {
                    clearClubFields();
                    return true;
                }
            });
        }
        TextView lblEvent = (TextView) findViewById(R.id.lblEvent);
        if ( lblEvent != null ) {
            lblEvent.setOnLongClickListener(new OnLongClickListener() {
                @Override public boolean onLongClick(View view) {
                    clearEventFields();
                    return true;
                }
            });
        }
        TextView lblRef = (TextView) findViewById(R.id.lblReferee);
        if ( lblRef != null ) {
            lblRef.setOnLongClickListener(new OnLongClickListener() {
                @Override public boolean onLongClick(View view) {
                    clearRefereeFields();
                    return true;
                }
            });
        }
    }

    DoublesServeSequence getDoublesServeSequence() {
        if ( (spDoublesServeSequence != null) && (spDoublesServeSequence.getVisibility() != View.GONE) ) {
            return DoublesServeSequence.values()[spDoublesServeSequence.getSelectedItemPosition()];
        }
        return DoublesServeSequence.NA;
    }

    /** uses Model */
    Intent getIntent(String sSource, String sSourceID, boolean bBackPressed, boolean bIsEditMatch) {
        TextView[] textViews = {txtPlayerA, txtPlayerB};
        int msg_enter_player_names = R.string.msg_enter_both_player_names;
        if ( m_bIsDoubles ) {
            textViews = new TextView[] {txtPlayerA, txtPlayerA2, txtPlayerB, txtPlayerB2};
            msg_enter_player_names = R.string.msg_enter_all_player_names;
        } else {
            // use club names as player names if player names are not entered
            TextView[] tvClubs = { txtClubA, txtClubB };
            if ( ViewUtil.areAllNonEmpty(tvClubs) && ViewUtil.areAllEmpty(textViews) ) {
                for(int i=0; i < tvClubs.length; i++) {
                    textViews[i].setText(tvClubs[i].getText());
                }
            }
        }
        for ( TextView txt: textViews ) {
            if ( txt == null ) { continue; }
            if ( txt.getText().toString().trim().length() == 0 ){
                //txt.setError(getString(msg_enter_player_names)); // To aggressive
                txt.setHint(msg_enter_player_names);
                if ( bBackPressed == false ) {
                    Toast.makeText(getContext(), msg_enter_player_names, Toast.LENGTH_SHORT).show();
                }
                return null;
            } else {
                txt.setError(null);
                txt.setHint(null);
            }
        }
        // get values from either a NumberPicker or Spinner element
        int iNrOfPoints2Win = PreferenceValues.numberOfPointsToWinGame(getContext());
/*
        if ( npGameEndScore != null ) {
            iNrOfPoints2Win = npGameEndScore.getValue();
        } else
*/
        if ( spGameEndScore != null ) {
            iNrOfPoints2Win = Integer.parseInt(spGameEndScore.getSelectedItem().toString().trim());
        }

        int iNrOfGamesToWinMatch = PreferenceValues.numberOfGamesToWinMatch(getContext()); // TODO: test for both spinner and numberpicker
        /*if ( npNumberOfGamesToWin != null ) {
            iNrOfGamesToWinMatch = (npNumberOfGamesToWin.getValue() + 1);
        } else*/
        if ( spNumberOfGamesToWin != null ) {
            iNrOfGamesToWinMatch = (Integer.parseInt(spNumberOfGamesToWin.getSelectedItem().toString()) + 1) / 2;
        }
        Intent intent = new Intent();
        Model  model = getModel(/*iNrOfPoints2Win, iNrOfGamesToWinMatch*/);
        if ( model instanceof GSMModel ) {
            GSMModel gsmModel = (GSMModel) model;
            gsmModel.setNrOfPointsToWinGame(iNrOfPoints2Win);
            gsmModel.setNrOfGamesToWinMatch(iNrOfGamesToWinMatch);
            if ( (spFinalSetFinish != null) && (spFinalSetFinish.getVisibility() != View.GONE) && ( spFinalSetFinish.getSelectedItemPosition() != -1) ) {
                FinalSetFinish fsf = FinalSetFinish.values()[spFinalSetFinish.getSelectedItemPosition()];
                gsmModel.setFinalSetFinish(fsf);
            }
            gsmModel.setGoldenPointToWinGame(cbUseGoldenPoint!= null && cbUseGoldenPoint.isChecked());
        } else {
            model.setNrOfPointsToWinGame(iNrOfPoints2Win);
            model.setNrOfGamesToWinMatch(iNrOfGamesToWinMatch);
        }
        if ( StringUtil.isNotEmpty(sSource) ) {
            model.setSource(sSource, sSourceID);
        }
        if ( bIsEditMatch ) {
            intent.putExtra(IntentKeys.EditMatch.toString(), model);
        } else {
            String sJson = model.toJsonString(null);
            intent.putExtra(IntentKeys.NewMatch.toString(), sJson); // this is read by ScoreBoard.onActivityResult
        }

        return intent;
    }

    private Model getModel(/*int iNrOfPoints2Win, int iNrOfGamesToWinMatch*/) {
        Model m = ModelFactory.getTemp();
        if ( m_bIsDoubles == false ) {
            m.setPlayerName(Player.A, txtPlayerA.getText().toString());
            m.setPlayerName(Player.B, txtPlayerB.getText().toString());
        }
        for(Player p : Player.values() ) {
            Button btnColor = btnsColor[p.ordinal()];
            if ( btnColor != null ) {
                Integer iColor = (Integer) btnColor.getTag();
                if ( (iColor != null) && (iColor.equals(m_iNoColor) == false ) ) {
                    String sColor = ColorUtil.getRGBString(iColor);
                    m.setPlayerColor(p, sColor);
                }
            }
        }
        if ( (txtCountryA != null) && (txtCountryB != null) ) {
            String sCountryA = txtCountryA.getText().toString();
            String sCountryB = txtCountryB.getText().toString();
            if ( (txtCountryA instanceof CountryTextView) && (txtCountryB instanceof CountryTextView) ) {
                String sCCA = ((CountryTextView)txtCountryA).getCountryCode();
                String sCCB = ((CountryTextView)txtCountryB).getCountryCode();
                if ( StringUtil.isNotEmpty(sCCA) ) {
                    sCountryA = sCCA;
                }
                if ( StringUtil.isNotEmpty(sCCB) ) {
                    sCountryB = sCCB;
                }
            }
            m.setPlayerCountry(Player.A, sCountryA);
            m.setPlayerCountry(Player.B, sCountryB);
        }
        m.setPlayerAvatar(Player.A, saAvatars[Player.A.ordinal()]);
        m.setPlayerAvatar(Player.B, saAvatars[Player.B.ordinal()]);

        if ( (txtClubA != null) && (txtClubB != null) ) {
            m.setPlayerClub(Player.A, txtClubA.getTextAndPersist().toString());
            m.setPlayerClub(Player.B, txtClubB.getTextAndPersist().toString());
        }
        if ( txtEventName != null ) {
            m.setEvent( txtEventName    .getTextAndPersist().toString()
                      , txtEventDivision.getTextAndPersist().toString()
                      , txtEventRound   .getTextAndPersist().toString()
                      , txtEventLocation.getTextAndPersist().toString()
            );
        }
        if ( txtRefereeName != null ) {
            m.setReferees( txtRefereeName.getTextAndPersist().toString()
                         , txtMarkerName .getTextAndPersist().toString());
        }
/*
        m.setNrOfPointsToWinGame(iNrOfPoints2Win);
        m.setNrOfGamesToWinMatch(iNrOfGamesToWinMatch);
*/
        if ( txtCourt != null ) {
            m.setCourt(txtCourt.getTextAndPersist().toString());
        }
        if ( (spTieBreakFormat != null) && (spTieBreakFormat.getVisibility() != View.GONE) && ( spTieBreakFormat.getSelectedItemPosition() != -1) ) {
            TieBreakFormat tbf = TieBreakFormat.values()[spTieBreakFormat.getSelectedItemPosition()];
            m.setTiebreakFormat(tbf);
        }

        if ( (spHandicap != null) && (spHandicap.getVisibility() != View.GONE) && (spHandicap.getSelectedItemPosition() != -1) ) {
            HandicapFormat hdc = HandicapFormat.values()[spHandicap.getSelectedItemPosition()];
            m.setHandicapFormat(hdc);
        }
        if ( Brand.isRacketlon() && spDisciplineStart != null ) {
            RacketlonModel rm = (RacketlonModel) m;
            Sport sport = Sport.values()[spDisciplineStart.getSelectedItemPosition()];
            if ( sport.equals(Sport.Tabletennis) == false ) {
                rm.setDiscipline(0, sport);
            }
        }
        if ( Brand.isTabletennis() && (spNumberOfServesPerPlayer != null) ) {
            int iNrOfServesPerPlayer = Integer.parseInt(spNumberOfServesPerPlayer.getSelectedItem().toString());
            m.setNrOfServesPerPlayer(iNrOfServesPerPlayer);
        }
        if ( (spAnnouncementLanguage != null) && (spAnnouncementLanguage.getVisibility() != View.GONE) && (spAnnouncementLanguage.getSelectedItemPosition() != -1) ) {
            AnnouncementLanguage languageNew = AnnouncementLanguage.values()[spAnnouncementLanguage.getSelectedItemPosition()];
            PreferenceValues.setAnnouncementLanguage(languageNew, getContext());
        }

        getValueFromSelectListOrToggleAndStoreAsPref(getContext(), cbWarmupDuration, spWarmupDuration, PreferenceKeys.timerWarmup           , PreferenceValues.getWarmupDuration(getContext()));
        getValueFromSelectListOrToggleAndStoreAsPref(getContext(), cbPauseDuration, spPauseDuration  , PreferenceKeys.timerPauseBetweenGames, PreferenceValues.getPauseDuration (getContext()));

        if ( tbBestOf_or_TotalOf != null ) {
            m.setPlayAllGames(tbBestOf_or_TotalOf.isChecked());
        }

        m.setEnglishScoring((cbUseEnglishScoring != null) && cbUseEnglishScoring.isChecked());
        if ( (cbUseLiveScoring != null) && cbUseLiveScoring.isChecked() ) {
            PreferenceValues.initForLiveScoring(getContext(), false);
        } else {
            PreferenceValues.initForNoLiveScoring(getContext());
        }

        // for doubles
        if ( m_bIsDoubles ) {
            if ( (spDoublesServeSequence != null) && (spDoublesServeSequence.getVisibility() != View.GONE) ) {
                if ( Brand.supportsDoubleServeSequence() ) {
                    DoublesServeSequence dss = DoublesServeSequence.values()[spDoublesServeSequence.getSelectedItemPosition()];
                    m.setDoublesServeSequence(dss);
                }
            }
            if ( (txtPlayerA2 != null) && (txtPlayerB2 != null) ) {
                m.setPlayerName(Player.A, txtPlayerA.getText() + "/" + txtPlayerA2.getText());
                m.setPlayerName(Player.B, txtPlayerB.getText() + "/" + txtPlayerB2.getText());
            }
        }
        if ( m_model != null ) {
            m.setSource(m_model.getSource() , m_model.getSourceID() );
            m.setAdditionalPostParams (m_model.getAdditionalPostParams());
            for(Player p: Player.values()) {
                m.setPlayerId(p, m_model.getPlayerId(p));
            }
        }

        if ( ListUtil.isNotEmpty(cbChangesSidesWhen) ) {
            EnumSet<ChangeSidesWhen_GSM> newPrefValues = EnumSet.noneOf(ChangeSidesWhen_GSM.class);
            for(CheckBox cb: cbChangesSidesWhen) {
                if ( cb.isChecked() ) {
                    ChangeSidesWhen_GSM checkedEnum = (ChangeSidesWhen_GSM) cb.getTag();
                    newPrefValues.add(checkedEnum);
                }
            }
            PreferenceValues.setStringSet(PreferenceKeys.changeSidesWhen_GSM, newPrefValues, getContext());
        }

        return m;
    }

    public static void getValueFromSelectListOrToggleAndStoreAsPref(Context ctx, ToggleButton cbDuration, Spinner spDuration, PreferenceKeys prefKey, int iCurrentPrefValue) {
        if ( (spDuration != null) && (spDuration.getVisibility() != View.GONE) ) {
            String sDuration = (String) spDuration.getSelectedItem();
            if ( StringUtil.isNotEmpty(sDuration) ) {
                int iDuration = Integer.parseInt(sDuration);
                if ( iDuration != iCurrentPrefValue) {
                    PreferenceValues.setNumber(prefKey, ctx, iDuration);
                }
            }
        }
        if ((cbDuration != null) && (cbDuration.getVisibility() != View.GONE)) {
            CharSequence sDuration = cbDuration.isChecked() ? cbDuration.getTextOn() : cbDuration.getTextOff();
            if (StringUtil.isNotEmpty(sDuration)) {
                int iDuration = Integer.parseInt(sDuration.toString());
                if (iDuration != iCurrentPrefValue) {
                    PreferenceValues.setNumber(prefKey, ctx, iDuration);
                }
            }
        }
    }
}
