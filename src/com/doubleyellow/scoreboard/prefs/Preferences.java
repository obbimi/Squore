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

package com.doubleyellow.scoreboard.prefs;

import android.app.Activity;
import android.app.Dialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.*;
import android.provider.ContactsContract;
import android.util.Log;
import android.util.TypedValue;
import android.widget.Toast;
import com.doubleyellow.android.SystemUtil;
import com.doubleyellow.android.task.DownloadImageTask;
import com.doubleyellow.android.util.ExportImport;
import com.doubleyellow.android.util.SimpleELAdapter;
import com.doubleyellow.prefs.DynamicListPreference;
import com.doubleyellow.prefs.EnumListPreference;
import com.doubleyellow.prefs.EnumMultiSelectPreference;
import com.doubleyellow.prefs.RWValues;
import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.model.Player;
import com.doubleyellow.scoreboard.model.TieBreakFormat;
import com.doubleyellow.util.*;

import java.io.*;
import java.util.*;

/**
 * Activity that allows user to modify preference values.
 * See also:
 * - res/xml/preferences.xml
 */
public class Preferences extends Activity /* using XActivity here crashes the app */ {

  //public static final int TEXTSIZE_UNIT = TypedValue.COMPLEX_UNIT_DIP;
  //public static final int TEXTSIZE_UNIT = TypedValue.COMPLEX_UNIT_SP;
    public static final int TEXTSIZE_UNIT = TypedValue.COMPLEX_UNIT_PX; // 20140325

    private SettingsFragment settingsFragment = new SettingsFragment();

    private static final String TAG = "SB." + Preferences.class.getSimpleName();

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScoreBoard.initAllowedOrientation(this);

        // Display the fragment as the main content.
        FragmentManager     mFragmentManager     = getFragmentManager();
        FragmentTransaction mFragmentTransaction = mFragmentManager.beginTransaction();
        mFragmentTransaction.replace(android.R.id.content, settingsFragment);
        mFragmentTransaction.commit();
    }

    @Override protected void onResume() {
        super.onResume();
        // Registers a callback to be invoked whenever a user changes a preference.
        settingsFragment.getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(spChange);
    }

    @Override protected void onPause() {
        super.onPause();
        // Unregisters the listener set in onResume().It's best practice to unregister listeners when your app isn't using.
        settingsFragment.getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(spChange);
    }

    private SPChange spChange = new SPChange();
    private class SPChange implements SharedPreferences.OnSharedPreferenceChangeListener
    {
        /** To be able to change multiple preferences by code without this listener doing anything */
        private boolean bIgnorePrefChanges = false;

        @Override public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
          //Log.d(TAG, "Selected = " + key); // dyaccount
            // text size changes
/*
            if ( key.startsWith("TextSize")) {
                setModelDirty(); // to trigger a redraw
            }
*/

            if ( bIgnorePrefChanges ) {
                return;
            }
            PreferenceKeys eKey = null;
            try {
                eKey = PreferenceKeys.valueOf(key);
            } catch (Exception e) {
                // not an enum value (e.g. textsize setting)
                // return;
            }
            boolean bReinitColors = false;
            if ( eKey != null ) {
                switch (eKey) {
                    case squoreBrand:
                        Brand brand = RWValues.getEnum(PreferenceKeys.squoreBrand, Preferences.this, Brand.class, Brand.Squore);
                        if ( Brand.brand != brand ) {
                            this.bIgnorePrefChanges = true;
                            ResetPrefs.resetToDefaults(Preferences.this, R.xml.preferences);
                            RWValues.setEnum(PreferenceKeys.squoreBrand, Preferences.this, brand);
                            Brand.setBrandPrefs(Preferences.this);
                            Brand.setSportPrefs(Preferences.this);

                            // delete files that may hold preferences
                            //ContentUtil.clearCache(Preferences.this);
                            DynamicListPreference.deleteCacheFile(Preferences.this, PreferenceKeys.colorSchema.toString());

                            this.bIgnorePrefChanges = false;
                            setModelDirty();
                            PreferenceValues.setRestartRequired(Preferences.this);
                        }
                        break;
                    case showBrandLogoOn:
                        final Preference psHideBrandLogoWhenGameInProgress = settingsFragment.findPreference(PreferenceKeys.hideBrandLogoWhenGameInProgress.toString());
                        if ( psHideBrandLogoWhenGameInProgress != null ) {
                            psHideBrandLogoWhenGameInProgress.setEnabled(ListUtil.isNotEmpty(PreferenceValues.showBrandLogoOn(Preferences.this)));
                        }
                        setModelDirty();
                        break;
                    case autoShowModeActivationDialog:
                        final Preference psShowModeDialogAfterXMins = settingsFragment.findPreference(PreferenceKeys.showModeDialogAfterXMins);
                        if ( psShowModeDialogAfterXMins != null ) {
                            psShowModeDialogAfterXMins.setEnabled(PreferenceValues.autoShowModeActivationDialog(Preferences.this));
                        }
                        break;
                    case showGamePausedDialog:
                        Feature f = PreferenceValues.showGamePausedDialog(Preferences.this);
                        final Preference psAutoShowGamePausedDialogAfterXPoints = settingsFragment.findPreference(PreferenceKeys.autoShowGamePausedDialogAfterXPoints);
                        if ( psAutoShowGamePausedDialogAfterXPoints != null ) {
                            psAutoShowGamePausedDialogAfterXPoints.setEnabled(f.equals(Feature.DoNotUse) == false);
                        }
                        final Preference pTimerTowelingDown = settingsFragment.findPreference(PreferenceKeys.timerTowelingDown);
                        if ( pTimerTowelingDown != null ) {
                            pTimerTowelingDown.setEnabled(f.equals(Feature.DoNotUse) == false);
                        }
                    case hideBrandLogoWhenGameInProgress:
                        setModelDirty();
                        break;
                    case showFieldDivisionOn:
                        final Preference pshideFieldDivisionWhenGameInProgress = settingsFragment.findPreference(PreferenceKeys.hideFieldDivisionWhenGameInProgress.toString());
                        if ( pshideFieldDivisionWhenGameInProgress != null ) {
                            pshideFieldDivisionWhenGameInProgress.setEnabled(ListUtil.isNotEmpty(PreferenceValues.showFieldDivisionOn(Preferences.this)));
                        }
                        setModelDirty();
                        break;
                    case hideFieldDivisionWhenGameInProgress:
                        setModelDirty();
                        break;
                    case textColorDetermination:
                        setModelDirty();
                        DetermineTextColor determineTextColor = PreferenceValues.getTextColorDetermination(Preferences.this);
                        PreferenceGroup textColors = (PreferenceGroup) settingsFragment.findPreference(PreferenceKeys.textColors);
                        PreferenceValues.initTextColors(textColors, determineTextColor.equals(DetermineTextColor.Manual));
                        bReinitColors = true;
                        break;
                    case targetDirForImportExport:
                        String sDir = RWValues.getString(eKey, 0, Preferences.this);
                        File fDir = new File(sDir);
                        if ( FileUtil.isWritable(fDir) == false ) {
                            String sMsg = getString(R.string.no_write_rights_for_x, sDir);
                                   sMsg = String.format("Directory %s is only readable to %s. You can use it for imports. Exporting to this directory will NOT be possible", sDir, Brand.getShortName(Preferences.this));
                            Toast.makeText(Preferences.this, sMsg, Toast.LENGTH_LONG).show();
                        }
                        break;
                    case colorSchema:
                        bReinitColors = true;
                        // update value between brackets
                        ColorPrefs.clearColorCache();
                        ColorPrefs.getTarget2colorMapping(Preferences.this);
                        setListPreferenceEntriesForColors(settingsFragment);
                        PreferenceValues.updatePreferenceTitle(settingsFragment.findPreference(PreferenceKeys.backgroundColors));
                        PreferenceValues.updatePreferenceTitle(settingsFragment.findPreference(PreferenceKeys.textColors));
                        break;
/*
                    case textColorDynamically:
                        setModelDirty();
                        boolean bDynamic = prefs.getBoolean(key, true);
                        PreferenceGroup textColors = (PreferenceGroup) settingsFragment.findPreference(PreferenceKeys.textColors);
                        PreferenceValues.initTextColors(textColors, bDynamic);
                        break;
*/
/*
                    case TextSizeScoreAsBigAsPossible:
                        setModelDirty();
                        boolean bAsBigAsPossible = prefs.getBoolean(key, true);
                        String sKey = PreferenceValues.TextSize.class.getSimpleName() + PreferenceValues.TextSize.Score.toString();
                        SeekBarPreference prefScore = (SeekBarPreference) settingsFragment.findPreference(sKey);
                        if ( prefScore != null) {
                            prefScore.setEnabled(bAsBigAsPossible == false);
                        }
                        break;
*/
                    case numberOfServiceCountUpOrDown: // fall through
                    case showPlayerColorOn:      // fall through
                    case hideFlagForSameCountry: // fall through
                    case showAvatarOn:           // fall through
                    case showCountryAs:
                        setModelDirty(); // to trigger a redraw
                        break;
                    case showActionBar:
                        boolean bShowActionBar = prefs.getBoolean(key, true);
                        final Preference prefShowTextInActionBar = settingsFragment.findPreference(PreferenceKeys.showTextInActionBar);
                        if ( prefShowTextInActionBar != null ) {
                            prefShowTextInActionBar.setEnabled(bShowActionBar);
                        }
                        // fall through
                    case showTextInActionBar:      // fall through
                  //case landscapeOrientationOnly: // fall through
                    case OrientationPreference:    // fall through
                    case showFullScreen:           // fall through
                    case prefetchFlags:            // fall through
                    case swapPlayersOn180DegreesRotationOfDeviceInLandscape: // fall through
                        setModelDirty();
                        PreferenceValues.setRestartRequired(Preferences.this);
                        break;
                    case smsResultToNr: break;

                  //case webIntegration: break;
                    case readContactsForAutoCompletion:
                    case readContactsForAutoCompletion_max:
                        PreferenceValues.Permission bHasPermission = PreferenceValues.doesUserHavePermissionToReadContacts(Preferences.this, true);
                        switch(bHasPermission) {
                            case Requested:
                                break;
                            case Granted:
                                boolean bReadContacts = prefs.getBoolean(PreferenceKeys.readContactsForAutoCompletion.toString(), true);
                                ListPreference contactGroup = (ListPreference) settingsFragment.findPreference(PreferenceKeys.onlyForContactGroups);
                                if (contactGroup != null) {
                                    contactGroup.setEnabled(bReadContacts);
                                }
                                EditTextPreference max = (EditTextPreference) settingsFragment.findPreference(PreferenceKeys.readContactsForAutoCompletion_max);
                                if (max != null) {
                                    max.setEnabled(bReadContacts);
                                }
                                if (bReadContacts) {
                                    // show warning if user has more than x contacts... may cause entering players to become slow or even crash
                                    Cursor cursor = Preferences.this.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
                                    int iContactsCount = 0;
                                    if (cursor != null) {
                                        iContactsCount = cursor.getCount();
                                        cursor.close();
                                    }
                                    if (iContactsCount > 300) {
                                        String sTitle = Preferences.this.getString(R.string.pref_readContactsForAutoCompletion);
                                        String sMsg = Preferences.this.getString(R.string.if_you_have_a_lot_of_contacts_dot_dot_dot, iContactsCount);
                                        ScoreBoard.dialogWithOkOnly(Preferences.this, sTitle, sMsg, true);

                                        //Toast.makeText(Preferences.this, sMsg, Toast.LENGTH_LONG).show();
                                    }
                                }
                                PreferenceValues.clearPlayerListCache();
                                break;
                        }
                        break;
                    case onlyForContactGroups:
                        break;
                    case feedPostUrls:
                        String sURLs = prefs.getString(key, "");
                        //LinkedHashMap<String, String> entries = getUrlsMap(sURLs);
                        List<Map<URLsKeys, String>> urlsList = PreferenceValues.getUrlsList(sURLs, Preferences.this);

                        ListPreference matchesFeedUrl = (ListPreference) settingsFragment.findPreference(PreferenceKeys.feedPostUrl);
                        settingsFragment.initFeedURLs(matchesFeedUrl, sURLs);

                        CheckBoxPreference suggestToPost = (CheckBoxPreference) settingsFragment.findPreference(PreferenceKeys.autoSuggestToPostResult);
                        if ( suggestToPost != null ) {
                            suggestToPost.setEnabled(ListUtil.isNotEmpty(urlsList));
                        }

                        // TODO: ensure selected value is still valid
                        break;
                    case feedPostUrl:
                        break;
                    case hideCompletedMatchesFromFeed:
                        break;
                    case autoSuggestToPostResult:
                        break;
                    case showPauseButtonOnTimer:
                        break;
                    case showLastGameInfoInTimer:
                        break;
                    case timerWarmup:
                        break;
                    case timerPauseBetweenGames:
                        syncAndClean_pauseBetweenGamesValues(Preferences.this);
                        break;
                    case timerPauseBetweenGames_values:
                        syncAndClean_pauseBetweenGamesValues(Preferences.this);
                        break;
                    case endGameSuggestion:
                        break;
                    case numberOfGamesToWinMatch:
                        int iGames = PreferenceValues.getInteger(prefs, eKey, 3);
                        if ( ScoreBoard.matchModel != null ) {
                            ScoreBoard.matchModel.setNrOfGamesToWinMatch(iGames);
                        }
                        break;
                    case numberOfPointsToWinGame:
                        int iEndScore = PreferenceValues.getInteger(prefs, eKey, 11);
                        if ( ScoreBoard.matchModel != null ) {
                            List<Map<Player, Integer>> endScoreOfPreviousGames = ScoreBoard.matchModel.getEndScoreOfPreviousGames();
                            if ( ListUtil.size(endScoreOfPreviousGames) == 0 ) {
                                // no finished games yet, assume user also want to change preference for current match
                                ScoreBoard.matchModel.setNrOfPointsToWinGame(iEndScore);
                            } else {
                                // there are finished games: only change if the new value matches already finished games
                                final Map<Player, Integer> firstGame = endScoreOfPreviousGames.get(0);
                                int iMax = MapUtil.getMaxValue(firstGame);
                                if ( iMax == iEndScore) {
                                    ScoreBoard.matchModel.setNrOfPointsToWinGame(iEndScore);
                                }
                            }
                        }
                        break;
                    case useHandInHandOutScoring:
                        boolean bUseEnglishScoring = PreferenceValues.getBoolean(prefs, eKey, false);
                        ScoreBoard.matchModel.setEnglishScoring(bUseEnglishScoring);
                        break;
                    case tieBreakFormat:
                        TieBreakFormat tieBreakFormat = PreferenceValues.getTiebreakFormat(Preferences.this);
                        ScoreBoard.matchModel.setTiebreakFormat(tieBreakFormat);
                        break;
                    case lockMatchMV:
                        Set<String> set = prefs.getStringSet(key, null);
                        Preference pNrOfMins = settingsFragment.findPreference(PreferenceKeys.numberOfMinutesAfterWhichToLockMatch);
                        if ( pNrOfMins != null ) {
                            pNrOfMins.setEnabled(set != null && set.contains(AutoLockContext.WhenMatchIsUnchangeForX_Minutes.toString()));
                        }
                        break;
                    case groupArchivedMatchesBy: // fall through
                    case sortOrderOfArchivedMatches:
                        // clear possible cache
                        SimpleELAdapter.deleteCacheFiles(Preferences.this);
                        break;
                    case shareAction:
                        // show message if selected option requires other value to be set to be most effective
                        ShareMatchPrefs smp = PreferenceValues.getShareAction(Preferences.this);
                        switch (smp) {
                            case PostResult:
                                String sPostUrl = PreferenceValues.getPostResultToURL(Preferences.this);
                                if ( StringUtil.isEmpty(sPostUrl) ) {
                                    showShareWarning("This option will not work unless you have configured a 'Post URL'");
                                } else {
                                    showShareInfo(String.format("This option will post match details to '%s'", sPostUrl));
                                }
                                break;
                            case SummaryToDefault:
                                String sSmsTo = PreferenceValues.getDefaultSMSTo(Preferences.this);
                                if ( StringUtil.isEmpty(sSmsTo) ) {
                                    showShareWarning(String.format("This option works best in combination with specifying a default recipients phone number in '%s'", getString(R.string.pref_smsResultToNr)));
                                } else {
                                    showShareInfo((String.format("This option will prepare a text message with the match summary for number '%s'", sSmsTo)));
                                }
                                break;
                            case LinkWithFullDetailsEachGame:
                                // this feature works best with 'Automatic' sharing
                                break;
                            default:
                                break;
                        }
                        break;
                    case useOfficialAnnouncementsFeature:
                        break;
                    case officialAnnouncementsLanguage:
                        PreferenceValues.clearOACache();
                        break;
                    case hapticFeedbackOnGameEnd:
                    case hapticFeedbackPerPoint:
                        boolean bVibrateOn = prefs.getBoolean(key, true);
                        if ( bVibrateOn ) {
                            SystemUtil.doVibrate(Preferences.this, 200);
                        }
                        break;
                    default:
                        //Log.d(TAG, "Not handling case for " + eKey);
                        break;
                }
            } else {
                //Log.w(TAG, "Not a valueOf() of PreferenceKeys " + key);
            }
            Preference preference = settingsFragment.findPreference(key);
            CharSequence charSequence = RWValues.updatePreferenceTitle(preference);

            try {
                ColorPrefs.ColorTarget colorTarget = ColorPrefs.ColorTarget.valueOf(key);
                String sColor = charSequence.toString();
                ColorPrefs.setColorTargetPreferenceIcon(colorTarget, sColor, preference);
                bReinitColors = true;
            } catch (Exception e) {
                // not a colortarget preference
            }

            // not required... does not change from within app itself
            //setFlagIcon(Preferences.this, settingsFragment.findPreference(PreferenceKeys.showCountryAs));

            if ( bReinitColors ) {
                setModelDirty();
                ColorPrefs.clearColorCache();
                ColorPrefs.getTarget2colorMapping(Preferences.this);
                ColorPrefs.setColorSchemaIcon (settingsFragment.findPreference(PreferenceKeys.Colors));
                ColorPrefs.setColorSchemaIcon (settingsFragment.findPreference(PreferenceKeys.colorSchema));
                ColorPrefs.setColorTargetIcons(settingsFragment);
            }
        }

        private void showShareWarning(String sMsg) {
            ScoreBoard.dialogWithOkOnly(Preferences.this, "Share", sMsg, true);
        }
        private void showShareInfo(String sMsg) {
            ScoreBoard.dialogWithOkOnly(Preferences.this, "Share", sMsg, false);
            //Toast.makeText(Preferences.this, sMsg, Toast.LENGTH_LONG).show();
        }
    }

    static void setFlagIcon(Context ctx, Preference pref) {
        if ( pref == null ) { return; }
        Log.d(TAG, "setFlagIcon for " + pref.getTitle());
        PreferenceValues.downloadImage(ctx, pref, RWValues.getDeviceLocale(ctx).getCountry());
    }

    // --------- preferences

    /* must be public and static or else preferences screen crashes when orientation changes ?! */
    public static class SettingsFragment extends PreferenceFragment {
        public SettingsFragment() {
            super();
        }

        Preference findPreference(PreferenceKeys key) {
            return super.findPreference(key.toString());
        }
        Preference findPreference(ColorPrefs.ColorTarget key) {
            return super.findPreference(key.toString());
        }

        @Override public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            try {
                addPreferencesFromResource(R.xml.preferences);
            } catch (Throwable e) {
                // have seen this happen when switching data type of certain preferences between app versions
                e.printStackTrace();
                ResetPrefs.resetToDefaults(getActivity(), R.xml.preferences);
            }

            // 'hack' to be able to update the Icon of the 'PreferenceScreen key="Colors"' when returning from it
            final PreferenceGroup  psAppearance = (PreferenceGroup ) this.findPreference(PreferenceKeys.Appearance    .toString());
            final PreferenceGroup  psInternet   = (PreferenceGroup ) this.findPreference(PreferenceKeys.webIntegration.toString());
            final PreferenceScreen psColors     = (PreferenceScreen) this.findPreference(PreferenceKeys.Colors        .toString());
            if ( psColors != null ) {
                if (/*Brand.getREColorPalette() !=0 && */ false ) {
                    psAppearance.removePreference(psColors);
                    psColors.setEnabled(false);
                } else {
                    DynamicListPreference colorSchema = (DynamicListPreference) this.findPreference(PreferenceKeys.colorSchema.toString());
                    if ( colorSchema != null ) {
                        colorSchema.setAllowNew( (Brand.brand == Brand.Squore) || Brand.isNotSquash() );
                    }

                    psColors.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override public boolean onPreferenceClick(Preference prefScreen) {
                            Dialog prefScreenDialog = psColors.getDialog();
                            prefScreenDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                @Override public void onDismiss(DialogInterface arg0) {
                                    // new icon has already been set by our SPChange but it is not redrawn on return: hence this hack
                                    psAppearance.removePreference(psColors); // just to redraw it
                                    psAppearance.addPreference(psColors);
                                }
                            });
                            return false;
                        }
                    });
                }
            }

            // remove some brand preferences if required
            final PreferenceGroup psBrandCategory = (PreferenceGroup) this.findPreference(PreferenceKeys.Brand);
            if ( psBrandCategory != null ) {
                if ( PreferenceValues.isBrandedExecutable(getActivity()) && Brand.isNotSquash() ) {
                    // plain racketlon or tabletennis versions
                    psAppearance.removePreference(psBrandCategory);
                } else if ( (Brand.values().length <= 1) && PreferenceValues.isUnbrandedExecutable(getActivity()) ) {
                    // remove option to change anything brand related if unbranded executable and other possible brands commented out
                    //psBrandCategory.removeAll(); // only if removing somehow does not work
                    //this.getPreferenceScreen().removePreference(psBrandCategory);
                    psAppearance.removePreference(psBrandCategory);
                } else {
                    if ( PreferenceValues.isBrandedExecutable(getActivity()) ) {
                        // for a branded version, do not support changing the brand
                        final Preference psBrand = psBrandCategory.findPreference(PreferenceKeys.squoreBrand.toString());
                        if (psBrand != null) {
                            if ((Brand.values().length <= 1) || PreferenceValues.isBrandedExecutable(getActivity())) {
                                psBrandCategory.removePreference(psBrand);
                            }
                        }
                    }

                    final Preference psShowBrandLogoOn = psBrandCategory.findPreference(PreferenceKeys.showBrandLogoOn.toString());
                    if ( psShowBrandLogoOn != null ) {
                        if (PreferenceValues.isUnbrandedExecutable(getActivity()) && (Brand.brand == Brand.Squore || Brand.getLogoResId() == 0)) {
                            psBrandCategory.removePreference(psShowBrandLogoOn);
                        }
                    }
                    final Preference psHideBrandLogoWhenGameInProgress = psBrandCategory.findPreference(PreferenceKeys.hideBrandLogoWhenGameInProgress.toString());
                    if ( psHideBrandLogoWhenGameInProgress != null ) {
                        psHideBrandLogoWhenGameInProgress.setEnabled(ListUtil.isNotEmpty(PreferenceValues.showBrandLogoOn(getActivity())));
                    }
                }
            }

            PreferenceScreen ps     = (PreferenceScreen) this.findPreference(PreferenceKeys.SBPreferences); // root
            PreferenceGroup  psgBeh = (PreferenceGroup)    ps.findPreference(PreferenceKeys.Behaviour.toString());
            if ( Brand.isNotSquash() ) {
                hideRemovePreference(ps, PreferenceKeys.statistics);
                hideRemovePreference(ps, PreferenceKeys.MatchFormat);

                PreferenceGroup psgApp = (PreferenceGroup) ps.findPreference(PreferenceKeys.Appearance.toString());
                hideRemovePreference(psgApp, PreferenceKeys.showScoringHistoryInMainScreenOn); // not really for racketlon and tabletennis
                hideRemovePreference(psgApp, PreferenceKeys.AppealHandGestureIconSize       ); // not really for racketlon and tabletennis
                hideRemovePreference(psgApp, PreferenceKeys.scorelineLayout);                  // not for racketlon and tabletennis

                hideRemovePreference(psgBeh, PreferenceKeys.useOfficialAnnouncementsFeature); // not really for racketlon and tabletennis
                hideRemovePreference(psgBeh, PreferenceKeys.officialAnnouncementsLanguage);   // not really for racketlon and tabletennis
                hideRemovePreference(psgBeh, PreferenceKeys.showChoosenDecisionShortly);      // not really for racketlon and tabletennis
            }
            if ( Brand.isSquash() ) {
                hideRemovePreference(psgBeh, PreferenceKeys.changeSides);                     // only for racketlon and tabletennis
                hideRemovePreference(psgBeh, PreferenceKeys.numberOfServiceCountUpOrDown);    // only for racketlon and tabletennis
            }
            if ( Brand.isTabletennis() ) {
                boolean bAutoShow = PreferenceValues.autoShowModeActivationDialog(getActivity());
                final Preference pShowModeDialogAfterXMins = this.findPreference(PreferenceKeys.showModeDialogAfterXMins);
                if ( pShowModeDialogAfterXMins != null ) {
                    pShowModeDialogAfterXMins.setEnabled(bAutoShow);
                }

                final Preference pShowGamePausedDialogAfterXPoints = this.findPreference(PreferenceKeys.autoShowGamePausedDialogAfterXPoints);
                Feature fShowGamePausedDialog = PreferenceValues.showGamePausedDialog(getActivity());
                if ( pShowGamePausedDialogAfterXPoints != null ) {
                    pShowGamePausedDialogAfterXPoints.setEnabled(fShowGamePausedDialog.equals(Feature.DoNotUse) == false);
                }

                final Preference pTimerTowelingDown = this.findPreference(PreferenceKeys.timerTowelingDown);
                if ( pTimerTowelingDown != null ) {
                    pTimerTowelingDown.setEnabled(fShowGamePausedDialog.equals(Feature.DoNotUse) == false);
                }
            } else {
                hideRemovePreference(ps, PreferenceKeys.modeUsageCategory);
                hideRemovePreference(ps, PreferenceKeys.pauseGame);
            }

            // initialize for downloading flags and setting correct dimensions

            Preference.OnPreferenceClickListener countryFlagInstaller = new Preference.OnPreferenceClickListener() {
                @Override public boolean onPreferenceClick(Preference prefScreen) {
                    DownloadImageTask.initForPrefIcon(prefScreen);
                    if ( DownloadImageTask.iPrefIconHW != null ) {
                        ColorPrefs.ICON_SIZE = DownloadImageTask.iPrefIconHW;
                    }

                    if (prefScreen instanceof PreferenceScreen) {
                        PreferenceScreen screen = (PreferenceScreen) prefScreen;
                        if ( screen == psAppearance) {
                            ColorPrefs.setColorSchemaIcon(screen.findPreference(PreferenceKeys.Colors.toString()));
                            ColorPrefs.setColorSchemaIcon(screen.findPreference(PreferenceKeys.colorSchema.toString()));
                            ColorPrefs.setColorTargetIcons(screen);
                        }
                        EnumSet<PreferenceKeys> flagKeys = EnumSet.of(PreferenceKeys.showCountryAs, PreferenceKeys.maximumCacheAgeFlags, PreferenceKeys.prefetchFlags, PreferenceKeys.hideFlagForSameCountry);
                        for(PreferenceKeys key: flagKeys) {
                            setFlagIcon(getActivity(), screen.findPreference(key.toString()));
                        }
                    }
                    // new icon has already been set by our SPChange but it is not redrawn : hence this hack

                    //psAppearance.removePreference(psCountryAs); // just to redraw it
                    //psAppearance.addPreference(psCountryAs);
                    return false;
                }
            };
            if ( psAppearance != null ) {
                psAppearance.setOnPreferenceClickListener(countryFlagInstaller);
            }
            if ( psInternet != null ) {
                psInternet.setOnPreferenceClickListener(countryFlagInstaller);
            }

            ListPreference     feedPostUrl  = (ListPreference)     this.findPreference(PreferenceKeys.feedPostUrl);
            EditTextPreference feedPostUrls = (EditTextPreference) this.findPreference(PreferenceKeys.feedPostUrls);
            if ( (feedPostUrls != null) && (feedPostUrl != null) ) {
                initFeedURLs(feedPostUrl, feedPostUrls.getText());
            }

            CheckBoxPreference readContactsForAutoCompletion     = (CheckBoxPreference) this.findPreference(PreferenceKeys.readContactsForAutoCompletion);
            EditTextPreference readContactsForAutoCompletion_max = (EditTextPreference) this.findPreference(PreferenceKeys.readContactsForAutoCompletion_max);
            ListPreference onlyForGroup = (ListPreference) this.findPreference(PreferenceKeys.onlyForContactGroups);
            PreferenceValues.Permission bHasContactsPermission = PreferenceValues.doesUserHavePermissionToReadContacts(getActivity(), false);
            if ( bHasContactsPermission == PreferenceValues.Permission.Granted ) {
                initGroups(onlyForGroup, readContactsForAutoCompletion);
                if ( (readContactsForAutoCompletion_max != null) && (readContactsForAutoCompletion!=null) ) {
                    readContactsForAutoCompletion_max.setEnabled(readContactsForAutoCompletion.isChecked());
                }
            } else {
                PreferenceCategory pcContacts = (PreferenceCategory) this.findPreference(PreferenceKeys.Contacts);
                if ( pcContacts != null ) {
                    if ( bHasContactsPermission == PreferenceValues.Permission.Denied ) {
                        // user specifically denied access to contacts
                        pcContacts.setEnabled(false);
                    }
                    pcContacts.setTitle("");
                    this.getPreferenceScreen().removePreference(pcContacts); // does not seem to work
                }
                // disable/hide the contacts related preferences
                if ( readContactsForAutoCompletion     != null ) readContactsForAutoCompletion    .setEnabled(bHasContactsPermission != PreferenceValues.Permission.Denied); // trigger request permission if user checks it and permission was never requested
                if ( readContactsForAutoCompletion_max != null ) readContactsForAutoCompletion_max.setEnabled(false);
                if ( onlyForGroup                      != null ) onlyForGroup                     .setEnabled(false);
            }

            // consistency in ActionBar checkboxes
            boolean bShowActionBar = PreferenceValues.showActionBar(getActivity());
            if ( bShowActionBar == false ) {
                final Preference pShowTextInActionBar = this.findPreference(PreferenceKeys.showTextInActionBar);
                if ( pShowTextInActionBar != null ) {
                    pShowTextInActionBar.setEnabled(bShowActionBar);
                }
            }

            // consistency in timer values
          //EditTextPreference timerPauseBetweenGames        = (EditTextPreference) this.findPreference(PreferenceKeys.timerPauseBetweenGames);
            EditTextPreference timerPauseBetweenGames_values = (EditTextPreference) this.findPreference(PreferenceKeys.timerPauseBetweenGames_values);
            if ( timerPauseBetweenGames_values != null ) {
                syncAndClean_pauseBetweenGamesValues(getActivity());
            }

            //CheckBoxPreference dynamic    = (CheckBoxPreference) this.findPreference(PreferenceKeys.textColorDynamically);
            EnumListPreference determine  = (EnumListPreference) this.findPreference(PreferenceKeys.textColorDetermination);
            PreferenceGroup    textColors = (PreferenceGroup)    this.findPreference(PreferenceKeys.textColors);
            if ( (determine != null) && (textColors != null) ) {
                PreferenceValues.initTextColors(textColors, String.valueOf(determine.getValue()).equals(DetermineTextColor.Manual.toString()));
                //PreferenceValues.initTextColors(textColors, dynamic);
            }

            EnumMultiSelectPreference lockPref = (EnumMultiSelectPreference) this.findPreference(PreferenceKeys.lockMatchMV);
            if ( lockPref != null ) {
                Set set = lockPref.getEnumValues();
                Preference pNrOfMins = this.findPreference(PreferenceKeys.numberOfMinutesAfterWhichToLockMatch);
                if ( pNrOfMins != null ) {
                    pNrOfMins.setEnabled(set != null && set.contains(AutoLockContext.WhenMatchIsUnchangeForX_Minutes));
                }
            }

            ResetPrefs resetPrefs = (ResetPrefs) findPreference(PreferenceKeys.resetPrefs.toString());
            if ( resetPrefs != null ) {
                resetPrefs.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override public boolean onPreferenceChange(Preference preference, Object o) {
                        ColorPrefs.reset(getActivity());
                        return false;
                    }
                });
            }

            setListPreferenceEntriesForColors(this);

            // disable all overwritten settings (TODO: test)
/*
            if ( MapUtil.isNotEmpty(PreferenceValues.mOverwrite) ) {
                for( Object pKey: PreferenceValues.mOverwrite.keySet() ) {
                    Preference pref = findPreference(pKey.toString());
                    if ( pref != null ) {
                        Log.d(TAG, "Disabling " + pKey);
                        pref.setEnabled(false);
                    } else {
                        Log.d(TAG, "Nothing to disable for " + pKey);
                    }
                }
            }
*/

            // for devices with no vibration capability, disable vibration preference
            if ( SystemUtil.hasVibrator(getActivity()) == false ) {
                CheckBoxPreference cbVibrate = (CheckBoxPreference) findPreference(PreferenceKeys.useVibrationNotificationInTimer);
                if ( cbVibrate != null ) {
                    cbVibrate.setChecked(false);
                    cbVibrate.setEnabled(false);
                }
            }

            ListPreference lpTargetDir = (ListPreference) findPreference(PreferenceKeys.targetDirForImportExport);
            if ( lpTargetDir != null ) {
                Collection<String> sdCardPaths       = ExportImport.getSDCardPaths(false);
              //Collection<String> sdCardPathsExport = ExportImport.getSDCardPaths(true);
                File storageDirectory = Environment.getExternalStorageDirectory();
                if ( (storageDirectory != null) && (sdCardPaths.contains(storageDirectory.getAbsolutePath()) == false) ) {
                    sdCardPaths.add(storageDirectory.getAbsolutePath());
                }
                if ( ListUtil.size(sdCardPaths) >= 1 ) {
                    String[] entries = sdCardPaths.toArray(new String[0]);
                    lpTargetDir.setEntries    (entries);
                    lpTargetDir.setEntryValues(entries);
                }
                String sCurrentValue = lpTargetDir.getValue();
                if ( StringUtil.isNotEmpty(sCurrentValue) ) {
                    File fCurrentDir = new File(sCurrentValue);
                    if ( FileUtil.isWritable(fCurrentDir) == false ) {
                        sCurrentValue = null;
                    }
                }
                if ( StringUtil.isEmpty(sCurrentValue) && (storageDirectory != null) ) {
                    lpTargetDir.setValue(storageDirectory.getAbsolutePath());
                }
                if (ListUtil.size(sdCardPaths) > 1 ) {
                    lpTargetDir.setEnabled(true);
                } else {
                    lpTargetDir.setEnabled(false);
                }

                PreferenceValues.Permission permission = PreferenceValues.doesUserHavePermissionToWriteExternalStorage(getActivity(), false);
                switch (permission) {
                    case Denied:
                        lpTargetDir.setEnabled(false);
                        break;
                    case Granted:
                    case Requested:
                        lpTargetDir.setEnabled(true);
                        break;
                    case DeniedNotRequested:
                        lpTargetDir.setEnabled(true);
                        lpTargetDir.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                            @Override public boolean onPreferenceClick(Preference preference) {
                                PreferenceValues.doesUserHavePermissionToWriteExternalStorage(getActivity(), true);
                                return false;
                            }
                        });
                        break;
                }
            }

            if ( ScoreBoard.isInSpecialMode() == false ) {
                final PreferenceGroup psFeeds = (PreferenceGroup) this.findPreference(PreferenceKeys.webIntegration.toString());
                if ( psFeeds != null ) {
                    psFeeds.removePreference(findPreference(PreferenceKeys.FeedFeedsURL));
                    //psFeeds.removePreference(findPreference("Technical"));
                }
            }
        }

        @Override public void onResume() {
            super.onResume();
            PreferenceScreen screen = getPreferenceScreen();
            if ( screen == null ) { return; }
            for (int i = 0; i < screen.getPreferenceCount(); ++i) {
                Preference preference = screen.getPreference(i);
                PreferenceValues.updatePreferenceTitle(preference);
            }

            //setFlagIcon(getActivity(), screen.findPreference(PreferenceKeys.showCountryAs.toString()));
            //setFlagIcon(getActivity(), screen.findPreference(PreferenceKeys.maximumCacheAgeFlags.toString()));
            //setFlagIcon(getActivity(), screen.findPreference(PreferenceKeys.prefetchFlags.toString()));
            //setFlagIcon(getActivity(), screen.findPreference(PreferenceKeys.hideFlagForSameCountry.toString()));

            //if ( Brand.isNotSquash() ) {
                Preference pLiveScore = screen.findPreference(PreferenceKeys.LiveScoreOpenURL.toString());
                Intent intent = pLiveScore.getIntent();
                Uri uriOld = intent.getData();
                Uri newUri = Uri.parse(Brand.getBaseURL() + "/live?lang=" + RWValues.getDeviceLanguage(getActivity()));
                intent.setData(newUri);
            //}
        }

/*
        @Override public void onHiddenChanged(boolean hidden) {
            super.onHiddenChanged(hidden);
        }

        @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
        }

        @Override public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
        }

        @Override public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
            if ( preference.getKey().equals(PreferenceKeys.Colors.toString())) {
                ColorPrefs.setColorSchemaIcon(preference);
            }
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
*/

        private void initFeedURLs(ListPreference listPreference, String sURLs) {
            if ( listPreference == null ) { return; }

            boolean bURLsAreEntered = StringUtil.isNotEmpty(sURLs);
            listPreference.setEnabled(bURLsAreEntered);
            if (bURLsAreEntered) {

                List<Map<URLsKeys, String>> urlsList = PreferenceValues.getUrlsList(sURLs, getActivity());
                List<String> entries = new ArrayList<String>();
                List<String> keys    = new ArrayList<String>();
                for(Map<URLsKeys, String> urlEntry: urlsList) {
                    entries.add(urlEntry.get(URLsKeys.Name));
                    keys.add(String.valueOf(keys.size()));
                }
                listPreference.setEntries    (entries.toArray(new String[0]));
                listPreference.setEntryValues(keys   .toArray(new String[0]));
            }
        }

        private void initGroups(ListPreference listPreference, CheckBoxPreference useContacts) {
            boolean bUseContacts = (useContacts != null) && useContacts.isChecked();
            if ( listPreference == null ) { return; }
            listPreference.setEnabled(bUseContacts);

            Map<String, Long> contacts = PreferenceValues.getContactGroups(getActivity());
            List<String> entries = new ArrayList<String>(); entries.add(getActivity().getString(R.string.lbl_none));
            List<String> keys    = new ArrayList<String>(); keys   .add("-1");
            for(String sName: contacts.keySet()) {
                String sId = contacts.get(sName).toString();
                entries.add(sName/* + " (" + sId + ")"*/);
                keys.add(sId);
            }
            listPreference.setEntries    (entries.toArray(new String[0]));
            listPreference.setEntryValues(keys   .toArray(new String[0]));
        }

        private void hideRemovePreference(PreferenceGroup pgParent, Object oKey) {
            Preference pgChild = this.findPreference(String.valueOf(oKey));
            hideRemovePreference(pgParent, pgChild);
        }
        private boolean hideRemovePreference(PreferenceGroup pgParent, Preference pgChild) {
            if ( pgParent == null ) { return false; }
            if ( pgChild  == null ) { return false; }
            boolean b = pgParent.removePreference(pgChild);
            if ( b == false ) {
                // intermediate pref group
                for(int p=0; p < pgParent.getPreferenceCount(); p++) {
                    Preference pc = pgParent.getPreference(p);
                    if ( pc instanceof PreferenceGroup == false ) { continue; }
                    b = b || hideRemovePreference((PreferenceGroup) pc, pgChild);
                    if ( b ) { break; }
                }
            }
            return b;
        }
    }

    private static void setListPreferenceEntriesForColors(SettingsFragment settingsFragment) {
        CharSequence[] values = null;
        for(ColorPrefs.ColorTarget colorTarget: ColorPrefs.ColorTarget.values() ) {
            ListPreference lp = (ListPreference) settingsFragment.findPreference(colorTarget);
            values = ColorPrefs.setListPreferenceEntries(lp, values, colorTarget);
        }
    }

    public static List<String> syncAndClean_pauseBetweenGamesValues(Context ctx) {
        int iResDefault = PreferenceValues.getSportTypeSpecificResId(ctx, R.string.timerPauseBetweenGames_values_default_Squash);

        String sValues = PreferenceValues.getString(PreferenceKeys.timerPauseBetweenGames_values, iResDefault, ctx);
        sValues = sValues.replaceAll("[^0-9,;]", "");
        List<String> lValues = new ArrayList<String>(Arrays.asList(sValues.split("[,;]")));
        int iCurrentValue   = PreferenceValues.getPauseDuration(ctx);
        String sIntValue = String.valueOf(iCurrentValue);
        if ( (lValues != null) && (lValues.contains(sIntValue) == false) ) {
            lValues.add(sIntValue);
        }
        ListUtil.removeEmpty(lValues);
        lValues = ListUtil.removeDuplicates(lValues);
        Collections.sort(lValues, new Comparator<String>() {
            @Override public int compare(String lhs, String rhs) {
                try {
                    int ilhs = Integer.parseInt(lhs);
                    int irhs = Integer.parseInt(rhs);
                    int diff = ilhs - irhs;
                    return diff;
                } catch (Exception e) {
                    return lhs.compareTo(rhs);
                }
            }
        });
        String sNewValue = ListUtil.join(lValues, ",");
        if ( sValues.equals(sNewValue) == false ) {
            PreferenceValues.setString(PreferenceKeys.timerPauseBetweenGames_values, ctx, sNewValue);
        }
        return lValues;
    }

    static void setModelDirty() {
        if ( ScoreBoard.matchModel == null) { return; }
        ScoreBoard.matchModel.setDirty();
    }
}
