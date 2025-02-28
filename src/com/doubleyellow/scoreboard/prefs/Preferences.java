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
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.EditTextPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.ListPreference;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceScreen;
import android.preference.PreferenceCategory;
import android.provider.ContactsContract;
import android.util.Log;
import android.util.TypedValue;
import android.widget.Toast;
import com.doubleyellow.android.SystemUtil;
import com.doubleyellow.android.task.DownloadImageTask;
import com.doubleyellow.android.util.ExportImport;
import com.doubleyellow.android.util.SimpleELAdapter;
import com.doubleyellow.android.util.KeyStoreUtil;
import com.doubleyellow.prefs.DynamicListPreference;
import com.doubleyellow.prefs.EnumListPreference;
import com.doubleyellow.prefs.EnumMultiSelectPreference;
import com.doubleyellow.prefs.RWValues;
import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.model.GoldenPointFormat;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.dialog.MyDialogBuilder;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.model.FinalSetFinish;
import com.doubleyellow.scoreboard.model.GSMModel;
import com.doubleyellow.scoreboard.model.NewBalls;
import com.doubleyellow.scoreboard.model.Player;
import com.doubleyellow.scoreboard.model.TieBreakFormat;
import com.doubleyellow.scoreboard.speech.Speak;
import com.doubleyellow.util.*;

import org.json.JSONArray;

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

    private final SPChange spChange = new SPChange();
    private class SPChange implements SharedPreferences.OnSharedPreferenceChangeListener
    {
        /** To be able to change multiple preferences by code without this listener doing anything */
        private boolean bIgnorePrefChanges = false;

        @Override public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
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
                m_lSettingsChanged.add(eKey);
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
                    case LandscapeLayoutPreference:
                        //PreferenceValues.setRestartRequired(Preferences.this);
                        break;
                    case showBrandLogoOn: {
                            boolean bEnabled = ListUtil.isNotEmpty(PreferenceValues.showBrandLogoOn(Preferences.this));
                            settingsFragment.setEnabledForPrefKeys(bEnabled, PreferenceKeys.hideBrandLogoWhenGameInProgress);
                            setModelDirty();
                        }
                        break;
                    case autoShowModeActivationDialog:
                        settingsFragment.setEnabledForPrefKeys(PreferenceValues.autoShowModeActivationDialog(Preferences.this), PreferenceKeys.showModeDialogAfterXMins);
                        break;
                    case showGamePausedDialog: {
                            Feature f = PreferenceValues.showGamePausedDialog(Preferences.this);
                            boolean bEnabled = f.equals(Feature.DoNotUse) == false;
                            settingsFragment.setEnabledForPrefKeys(bEnabled, PreferenceKeys.autoShowGamePausedDialogAfterXPoints, PreferenceKeys.timerTowelingDown);
                        }
                        break;
                    case hideBrandLogoWhenGameInProgress:
                        setModelDirty();
                        break;
                    case showFieldDivisionOn: {
                            boolean bEnabled = ListUtil.isNotEmpty(PreferenceValues.showFieldDivisionOn(Preferences.this));
                            settingsFragment.setEnabledForPrefKeys(bEnabled, PreferenceKeys.hideFieldDivisionWhenGameInProgress);
                            setModelDirty();
                        }
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
                        m_lColorSettingsChanged.add(eKey.toString());
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
                    case showPlayerColorOn_Text:
                    case colorSchema:
                        m_lColorSettingsChanged.add(eKey.toString());
                        bReinitColors = true;
                        // update value between brackets
                        ColorPrefs.clearColorCache();
                        ColorPrefs.getTarget2colorMapping(Preferences.this);
                        setListPreferenceEntriesForColors(settingsFragment);
                        RWValues.updatePreferenceTitle(settingsFragment.findPreference(PreferenceKeys.backgroundColors));
                        RWValues.updatePreferenceTitle(settingsFragment.findPreference(PreferenceKeys.textColors));
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
                        settingsFragment.setEnabledForPrefKeys(bShowActionBar, PreferenceKeys.showTextInActionBar);
                        // fall through
                    case showTextInActionBar:      // fall through
                    case OrientationPreference:    // fall through
                    case showFullScreen:           // fall through
                    case prefetchFlags:            // fall through
                    case swapPlayersOn180DegreesRotationOfDeviceInLandscape: // fall through
                        setModelDirty();
                        //PreferenceValues.setRestartRequired(Preferences.this);
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
                                settingsFragment.setEnabledForPrefKeys(bReadContacts, PreferenceKeys.onlyForContactGroups, PreferenceKeys.readContactsForAutoCompletion_max);
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
                                        MyDialogBuilder.dialogWithOkOnly(Preferences.this, sTitle, sMsg, true);

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

                        boolean bEnabled1 = ListUtil.isNotEmpty(urlsList);
                        settingsFragment.setEnabledForPrefKeys(bEnabled1, PreferenceKeys.autoSuggestToPostResult);

                        // TODO: ensure selected value is still valid
                        break;
                    case feedPostUrl:
                        break;
                    case allowTrustAllCertificatesAndHosts:
                        boolean bAllowTrustAllCertificatesAndHosts = PreferenceValues.getBoolean(prefs, eKey, false);
                        if ( bAllowTrustAllCertificatesAndHosts ) {
                            try {
                                KeyStoreUtil.trustAllHttpsCertificates();
                                KeyStoreUtil.trustAllHostnames();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            // TODO: undo
                        }
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
                        syncAndClean_warmupValues(Preferences.this);
                        break;
                    case timerWarmup_values:
                        syncAndClean_warmupValues(Preferences.this);
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
                        if ( ScoreBoard.getMatchModel() != null ) {
                            ScoreBoard.getMatchModel().setNrOfGamesToWinMatch(iGames);
                        }
                        break;
                    case numberOfPointsToWinGame:
/*
                        if ( ScoreBoard.getMatchModel() instanceof GSMModel ) {
                            // should always be 4
                            break;
                        }
*/
                        int iEndScore = PreferenceValues.getInteger(prefs, eKey, 11);
                        if ( ScoreBoard.getMatchModel() != null ) {
                            List<Map<Player, Integer>> endScoreOfPreviousGames = ScoreBoard.getMatchModel().getEndScoreOfPreviousGames();
                            if ( ListUtil.size(endScoreOfPreviousGames) == 0 ) {
                                // no finished games yet, assume user also want to change preference for current match
                                ScoreBoard.getMatchModel().setNrOfPointsToWinGame(iEndScore);
                            } else {
                                // there are finished games: only change if the new value matches already finished games
                                final Map<Player, Integer> firstGame = endScoreOfPreviousGames.get(0);
                                int iMax = MapUtil.getMaxValue(firstGame);
                                if ( iMax == iEndScore) {
                                    ScoreBoard.getMatchModel().setNrOfPointsToWinGame(iEndScore);
                                }
                            }
                        }
                        break;
                    case useHandInHandOutScoring:
                        boolean bUseEnglishScoring = PreferenceValues.getBoolean(prefs, eKey, false);
                        ScoreBoard.getMatchModel().setEnglishScoring(bUseEnglishScoring);
                        break;
                    case tieBreakFormat:
                        TieBreakFormat tieBreakFormat = PreferenceValues.getTiebreakFormat(Preferences.this);
                        ScoreBoard.getMatchModel().setTiebreakFormat(tieBreakFormat);
                        break;
                    case finalSetFinish:
                        if ( ScoreBoard.getMatchModel() instanceof GSMModel) {
                            FinalSetFinish finalSetFinish = PreferenceValues.getFinalSetFinish(Preferences.this);
                            ((GSMModel) ScoreBoard.getMatchModel()).setFinalSetFinish(finalSetFinish);
                        }
                        break;
                    case newBalls:
                        if ( ScoreBoard.getMatchModel() instanceof GSMModel) {
                            NewBalls newBalls = PreferenceValues.getNewBalls(Preferences.this);
                            ((GSMModel) ScoreBoard.getMatchModel()).setNewBalls(newBalls);
                        }
                        break;
                    //case goldenPointToWinGame:
                    case goldenPointFormat:
                        if ( ScoreBoard.getMatchModel() instanceof GSMModel) {
                            GoldenPointFormat goldenPointFormat = PreferenceValues.goldenPointFormat(Preferences.this);
                            ((GSMModel) ScoreBoard.getMatchModel()).setGoldenPointFormat(goldenPointFormat);
                        }
                        break;
                    case StartTiebreakOneGameEarly:
                        if ( ScoreBoard.getMatchModel() instanceof GSMModel) {
                            boolean bStartTiebreakOneGameEarly = PreferenceValues.startTiebreakOneGameEarly(Preferences.this);
                            ((GSMModel) ScoreBoard.getMatchModel()).setStartTiebreakOneGameEarly(bStartTiebreakOneGameEarly);
                        }
                        break;
                    case indicateGoldenPoint:
                        // simply keep 'indicateGameBall' in sync. Different pref key for different descriptions
                        boolean bIndicateGoldenPoint = RWValues.getBoolean(eKey, Preferences.this, false);
                        RWValues.setBoolean(PreferenceKeys.indicateGameBall, Preferences.this, bIndicateGoldenPoint);
                        break;
                    case lockMatchMV:
                        Set<String> set = prefs.getStringSet(key, null);
                        boolean bEnabled2 = set != null && set.contains(AutoLockContext.WhenMatchIsUnchangeForX_Minutes.toString());
                        settingsFragment.setEnabledForPrefKeys(bEnabled2, PreferenceKeys.numberOfMinutesAfterWhichToLockMatch);
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
                            default:
                                break;
                        }
                        break;
                    case useOfficialAnnouncementsFeature:
                        break;
                    case officialAnnouncementsLanguage:
                        PreferenceValues.clearOACache();
                        Locale locale = PreferenceValues.announcementsLocale(Preferences.this);
                        Speak.getInstance().setLocale(locale);
                        settingsFragment.initVoices();
                        playSpeechSample();
                        break;
                    case hapticFeedbackOnGameEnd:
                    case hapticFeedbackPerPoint:
                        boolean bVibrateOn = prefs.getBoolean(key, true);
                        if ( bVibrateOn ) {
                            SystemUtil.doVibrate(Preferences.this, 200);
                        }
                        break;
                    case blinkFeedbackPerPoint: {
                            boolean ppEnabled = PreferenceValues.blinkFeedbackPerPoint(Preferences.this);
                            settingsFragment.setEnabledForPrefKeys(ppEnabled, PreferenceKeys.numberOfBlinksForFeedbackPerPoint);
                        }
                        break;
                    case numberOfBlinksForFeedbackPerPoint:
                        break;
                    case wearable_allowScoringWithHardwareButtons:
                    case wearable_allowScoringWithRotary:
                    case wearable_keepScreenOnWhen:
                        m_lWearableSettingsChanged.add(eKey.toString());
                        break;
                    case useCastScreen:
                        PreferenceValues.setCastRestartRequired(Preferences.this);
                        break;
                    case speechVoice:
                        String sVoice = PreferenceValues.getSpeechVoice(Preferences.this);
                        Speak.getInstance().setVoice(sVoice);
                        playSpeechSample();
                        break;
                    case useSpeechFeature:
                        Feature fUseSpeech = PreferenceValues.useSpeechFeature(Preferences.this);
                        boolean bUseSpeech = PreferenceValues.useFeatureYesNo(fUseSpeech);
                        Speak.getInstance().setFeature(fUseSpeech);
                        settingsFragment.setEnabledForPrefKeys(bUseSpeech, PreferenceKeys.speechPitch, PreferenceKeys.speechRate, PreferenceKeys.speechPauseBetweenParts);
                        settingsFragment.setEnabledForPrefKeys(bUseSpeech, PreferenceKeys.speechOverBT_PauseBetweenPlaysToKeepAlive, PreferenceKeys.speechOverBT_PlayWhiteNoiseSoundFileToKeepAlive);
                        if ( bUseSpeech ) {
                            playSpeechSample();

                            boolean ppEnabled = PreferenceValues.speechOverBT_PlayWhiteNoiseSoundFileToKeepAlive(Preferences.this);
                            stopStartWhiteNoise(ppEnabled);
                        } else {
                            stopStartWhiteNoise(false);
                        }
                        break;
                    case speechPitch:
                    case speechRate:
                    case speechPauseBetweenParts:
                        // speak a small piece of text to allow user to fine tune voice right here
                        playSpeechSample();
                        break;
                    case speechOverBT_PlayWhiteNoiseSoundFileToKeepAlive: {
                        boolean ppEnabled = PreferenceValues.speechOverBT_PlayWhiteNoiseSoundFileToKeepAlive(Preferences.this);
                        stopStartWhiteNoise(ppEnabled);
                        break;
                    }
                    case speechOverBT_PauseBetweenPlaysToKeepAlive:
                    case speechOverBT_PlayingVolumeToKeepAlive: {
                        stopStartWhiteNoise(false);
                        boolean ppEnabled = PreferenceValues.speechOverBT_PlayWhiteNoiseSoundFileToKeepAlive(Preferences.this);
                        stopStartWhiteNoise(ppEnabled);
                    }
                    case usePowerPlay: {
                        boolean ppEnabled = PreferenceValues.usePowerPlay(Preferences.this);
                        settingsFragment.setEnabledForPrefKeys(ppEnabled, PreferenceKeys.numberOfPowerPlaysPerPlayerPerMatch);
                        break;
                    }
                    case FCMEnabled:
                        // if no FCM device id yet, generate one
                        String sFCMDeviceId = PreferenceValues.getFCMDeviceId(Preferences.this);
                        Preference pFCMId = settingsFragment.findPreference(PreferenceKeys.liveScoreDeviceId);
                        if ( pFCMId != null ) {
                            PreferenceValues.updatePreferenceTitleResId(pFCMId, Preferences.this);
                        }
                        boolean fcmEnabled = PreferenceValues.isFCMEnabled(Preferences.this);
                        if (fcmEnabled) {
                            // assume score is fully controlled remotely by limited options so prevent any dialogs
                            for(PreferenceKeys aKey : AutomateWhatCanBeAutomatedPrefs.dialog_AutomateOrSuggest_prefKeys) {
                                PreferenceValues.setEnum(aKey, Preferences.this, Feature.Automatic);
                            }
                        }
                        settingsFragment.setEnabledForPrefKeys(fcmEnabled, PreferenceKeys.showToastMessageForEveryReceivedFCMMessage, PreferenceKeys.liveScoreDeviceId);
                        break;
                    case UseBluetoothLE:
                        boolean bUse = PreferenceValues.useBluetoothLE(Preferences.this);
                        break;
                    default:
                        //Log.d(TAG, "Not handling case for " + eKey);
                        break;
                }
            } else {
                //Log.w(TAG, "Not a valueOf() of PreferenceKeys " + key);
            }
            Preference preference = settingsFragment.findPreference(key);

            // update title with value the preference currently has
            CharSequence charSequence = RWValues.updatePreferenceTitle(preference);

            try {
                ColorPrefs.ColorTarget colorTarget = ColorPrefs.ColorTarget.valueOf(key);
                String sColor = charSequence.toString();
                ColorPrefs.setColorTargetPreferenceIcon(colorTarget, sColor, preference);
                bReinitColors = true;
                m_lColorSettingsChanged.add(key);
            } catch (Exception e) {
                // not a colortarget preference
            }
            if ( (key != null) && key.startsWith(PreferenceKeys.PlayerColorsNewMatch.toString()) && preference instanceof EditTextPreference) {
                String sColor = RWValues.getString(key, null, Preferences.this);
                ColorPrefs.setColorTargetPreferenceIcon(sColor, preference);
                m_lColorSettingsChanged.add(key); // PlayerColorsNewMatchA and/or PlayerColorsNewMatchB
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

        public void stopStartWhiteNoise(boolean ppEnabled) {
            if ( ppEnabled ) {
                Speak.getInstance().startWhiteNoise();
            } else {
                Speak.getInstance().stopWhiteNoise();
            }
        }
        public void playSpeechSample() {
            Speak instance = Speak.getInstance();
            if ( instance.isStarted() == false ) {
                instance.start(Preferences.this);
            }
            instance.setPitch            (PreferenceValues.getSpeechPitch(Preferences.this));
            instance.setSpeechRate       (PreferenceValues.getSpeechRate(Preferences.this));
            instance.setPauseBetweenParts(PreferenceValues.getSpeechPauseBetweenWords(Preferences.this));
            Feature fSpeech = PreferenceValues.useOfficialAnnouncementsFeature(Preferences.this);
            if ( PreferenceValues.useFeatureYesNo(fSpeech) ) {
                //don't call to easily. Specified TTS voice seems to be lost if
                //instance.setLocale(PreferenceValues.announcementsLocale(Preferences.this));
            }

            // feed current score
            instance.playAllDelayed(200);
        }

        private void showShareWarning(String sMsg) {
            MyDialogBuilder.dialogWithOkOnly(Preferences.this, "Share", sMsg, true);
        }
        private void showShareInfo(String sMsg) {
            MyDialogBuilder.dialogWithOkOnly(Preferences.this, "Share", sMsg, false);
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

            final PreferenceGroup  psAppearance = (PreferenceGroup ) this.findPreference(PreferenceKeys.Appearance    );
            final PreferenceGroup  psInternet   = (PreferenceGroup ) this.findPreference(PreferenceKeys.webIntegration);
            final PreferenceScreen psColors     = (PreferenceScreen) this.findPreference(PreferenceKeys.Colors        );
            // 'hack' to be able to update the Icon of the 'PreferenceScreen key="Colors"' when returning from it
            if ( psColors != null ) {
                if (/*Brand.getREColorPalette() !=0 && */ false ) {
                    psAppearance.removePreference(psColors);
                    psColors.setEnabled(false);
                } else {
                    DynamicListPreference colorSchema = (DynamicListPreference) this.findPreference(PreferenceKeys.colorSchema);
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
            final PreferenceGroup psBLE = (PreferenceGroup) this.findPreference(PreferenceKeys.BluetoothLE);
            if ( psBLE != null ) {
                if ( PreferenceValues.useBluetoothLE(getContext()) ) {
                    ListPreference lpBLEconfig = ( ListPreference) this.findPreference(PreferenceKeys.BluetoothLE_Config);
                    if ( lpBLEconfig != null ) {
                        List<CharSequence> lKeys        = PreferenceValues.getConfigs(getContext(), R.raw.bluetooth_le_config, 1);
                        List<CharSequence> lKeysAndDesc = PreferenceValues.getConfigs(getContext(), R.raw.bluetooth_le_config, 3);
                        lpBLEconfig.setEntryValues(lKeys       .toArray(new CharSequence[0]));
                        lpBLEconfig.setEntries    (lKeysAndDesc.toArray(new CharSequence[0]));
                    }
                } else {
                    if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ) {
                        psBLE.getParent().removePreference(psBLE);
                    }
                }
            }
            final PreferenceGroup psCastLiveScore = (PreferenceGroup) this.findPreference(PreferenceKeys.ChromeCast);
            if ( psCastLiveScore != null ) {
                ListPreference castScreen = (ListPreference) psCastLiveScore.findPreference(PreferenceKeys.useCastScreen.toString());

                List<CharSequence> lLetUserSelectFrom   = new ArrayList<>();
                List<CharSequence> lLetUserSelectFromHR = new ArrayList<>();
                JSONArray jaDisplayIds = Brand.brand.getCastListForBrandFromConfig(false);
                if ( jaDisplayIds != null ) {
                    castScreen.setEnabled(true);
                    for (int i = 0; i < jaDisplayIds.length(); i++) {
                        lLetUserSelectFrom  .add(String.valueOf(i));
                        lLetUserSelectFromHR.add(jaDisplayIds.optString(i));
                    }

                    castScreen.setEntryValues(lLetUserSelectFrom  .toArray(new CharSequence[0])); // actual values
                    castScreen.setEntries    (lLetUserSelectFromHR.toArray(new CharSequence[0])); // human readable
                } else {
                    castScreen.setEnabled(false);
                }
            }

            initVoices();

            // remove some brand preferences if required
            final PreferenceGroup psBrandCategory   = (PreferenceGroup) this.findPreference(PreferenceKeys.Brand);
            final Preference      psBrandSelectList = psBrandCategory.findPreference(PreferenceKeys.squoreBrand.toString());
            if ( psBrandCategory != null ) {
                if ( PreferenceValues.isBrandedExecutable(getActivity()) && Brand.isNotSquash() ) {
                    // plain racketlon or tabletennis versions
                    //psAppearance.removePreference(psBrandCategory);
                    psAppearance.removePreference(psBrandSelectList);
                } else if ( (Brand.values().length <= 1) && PreferenceValues.isUnbrandedExecutable(getActivity()) ) {
                    // remove option to change anything brand related if unbranded executable and other possible brands commented out
                    //psBrandCategory.removeAll(); // only if removing somehow does not work
                    //this.getPreferenceScreen().removePreference(psBrandCategory);
                    //psAppearance.removePreference(psBrandCategory);
                    psAppearance.removePreference(psBrandSelectList);
                } else {
                    if ( PreferenceValues.isBrandedExecutable(getActivity()) ) {
                        // for a branded version, do not support changing the brand
                        if (psBrandSelectList != null) {
                            if ((Brand.values().length <= 1) || PreferenceValues.isBrandedExecutable(getActivity())) {
                                psBrandCategory.removePreference(psBrandSelectList);
                            }
                        }
                    }

                    final Preference psShowBrandLogoOn = psBrandCategory.findPreference(PreferenceKeys.showBrandLogoOn.toString());
                    if ( psShowBrandLogoOn != null ) {
                        Context context = getActivity();
                        String sImageURL = PreferenceValues.castScreenSponsorUrl(context);
                        if ( StringUtil.isEmpty(sImageURL) ) {
                            sImageURL = PreferenceValues.castScreenLogoUrl(context);
                        }
                        if (PreferenceValues.isUnbrandedExecutable(getActivity()) && (Brand.brand == Brand.Squore || Brand.getLogoResId() == 0)) {
                            //psBrandCategory.removePreference(psShowBrandLogoOn);
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
              //hideRemovePreference(psgApp, PreferenceKeys.showScoringHistoryInMainScreenOn); // not really for racketlon and tabletennis
                hideRemovePreference(psgApp, PreferenceKeys.AppealHandGestureIconSize       ); // not really for racketlon and tabletennis
                hideRemovePreference(psgApp, PreferenceKeys.scorelineLayout);                  // not interesting enough for racketlon, badminton and tabletennis

                hideRemovePreference(psgBeh, PreferenceKeys.showChoosenDecisionShortly);      // not really for racketlon and tabletennis
                if ( PreferenceValues.announcementLanguageDeviates(getActivity()) == false ) {
                    hideRemovePreference(psgBeh, PreferenceKeys.officialAnnouncementsLanguage);   // not really for racketlon and tabletennis
                }
            }
            if ( Brand.isTabletennis() || Brand.isRacketlon() || Brand.isBadminton() ) {
                hideRemovePreference(psgBeh, PreferenceKeys.useOfficialAnnouncementsFeature); // Squash full blown, Tennis: 'New balls' only for now
            }
            if ( Brand.isSquash() ) {
                hideRemovePreference(psgBeh, PreferenceKeys.changeSides);                     // several 'change sides' options: not for squash
            }
            if ( Brand.isSquash() || Brand.isBadminton() || Brand.isGameSetMatch() ) {
                hideRemovePreference(psgBeh, PreferenceKeys.numberOfServiceCountUpOrDown);    // only for racketlon and tabletennis
            }
            if ( Brand.isGameSetMatch() ) {
                for(PreferenceKeys key: NONGameSetMatch_SpecificPrefs ) {
                    hideRemovePreference(psgBeh, key);
                }
                hideRemovePreference(psgBeh, PreferenceKeys.indicateGameBall); // always at AD/40 very common. In squash games may be to 9,11 or 15
                hideRemovePreference(psgBeh, PreferenceKeys.endGameSuggestion); // should be always Automatic. very common score in tennispadel/tennis
                hideRemovePreference(psgBeh, PreferenceKeys.showDetailsAtEndOfGameAutomatically); // TODO: make details about sets, not games
                hideRemovePreference(psgBeh, PreferenceKeys.hapticFeedbackOnGameEnd);
                hideRemovePreference(psgBeh, PreferenceKeys.AutomateWhatCanBeAutomatedPrefs);

                hideRemovePreference(psgBeh, PreferenceKeys.swapPlayersBetweenGames);
                hideRemovePreference(psgBeh, PreferenceKeys.swapPlayersHalfwayGame);
            } else {
                PreferenceGroup psgMF = (PreferenceGroup) ps.findPreference(PreferenceKeys.MatchFormat.toString());
                for(PreferenceKeys key: GameSetMatch_SpecificPrefs ) {
                    hideRemovePreference(psgMF, key);
                    hideRemovePreference(psgBeh, key);
                }
            }
/*
            if ( false && Brand.isGameSetMatch() ) {
                hideRemovePreference(psgBeh, PreferenceKeys.numberOfPointsToWinGame);         // always 4
            }
*/
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

                            for(Player p: Player.values()) {
                                String key = PreferenceKeys.PlayerColorsNewMatch.toString() + p;
                                Preference preference = findPreference(key);
                                if ( preference != null ) {
                                    String sColor = RWValues.getString(key, null, preference.getContext());
                                    if ( StringUtil.isNotEmpty(sColor) ) {
                                        ColorPrefs.setColorTargetPreferenceIcon(sColor, preference);
                                    }
                                }
                            }
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

            // disable 'speech' controllers if 'use speech = false'
            Feature fUseSpeech = PreferenceValues.useSpeechFeature(getActivity());
            boolean bUseSpeech = PreferenceValues.useFeatureYesNo(fUseSpeech);
            PreferenceKeys [] keys = new PreferenceKeys[] { PreferenceKeys.speechPitch, PreferenceKeys.speechRate, PreferenceKeys.speechPauseBetweenParts };
            for(PreferenceKeys keyOfPrefToToggle: keys) {
                final Preference pref = this.findPreference(keyOfPrefToToggle);
                if ( pref != null ) {
                    pref.setEnabled(bUseSpeech);
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

            ResetPrefs resetPrefs = (ResetPrefs) findPreference(PreferenceKeys.resetPrefs);
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

            // FCM
            if ( PreferenceValues.isFCMEnabled(getActivity()) == false ) {
                setEnabledForPrefKeys(false, PreferenceKeys.showToastMessageForEveryReceivedFCMMessage, PreferenceKeys.liveScoreDeviceId);
            }

            ListPreference lpTargetDir = (ListPreference) findPreference(PreferenceKeys.targetDirForImportExport);
            if ( lpTargetDir != null ) {
                Collection<String> sdCardPaths       = ExportImport.getSDCardPaths(false);
              //Collection<String> sdCardPathsExport = ExportImport.getSDCardPaths(true);

                File[] storageDirectories = new File[]
                        { Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                        , Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        , getActivity().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) // this directory is not ideal. Files are deleted if app is uninstalled...
                        , Environment.getExternalStorageDirectory()
                        };
                for(File storageDirectory2: storageDirectories) {
                    if ( (storageDirectory2 != null) && (sdCardPaths.contains(storageDirectory2.getAbsolutePath()) == false) ) {
                        sdCardPaths.add(storageDirectory2.getAbsolutePath());
                    }
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
                if ( StringUtil.isEmpty(sCurrentValue) && (storageDirectories[0] != null) ) {
                    lpTargetDir.setValue(storageDirectories[0].getAbsolutePath());
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

            // update titles with more sport specific titles
            Preference preference = this.findPreference(PreferenceKeys.SBPreferences);
            PreferenceValues.updatePreferenceTitleResId(preference, getActivity());

            if ( ScoreBoard.isInSpecialMode() == false ) {
                final PreferenceGroup psFeeds = (PreferenceGroup) this.findPreference(PreferenceKeys.webIntegration);
                if ( psFeeds != null ) {
                    psFeeds.removePreference(findPreference(PreferenceKeys.FeedFeedsURL));
                    //psFeeds.removePreference(findPreference("Technical"));
                }
            }
        }

        public void initVoices() {
            ListPreference lpSpeechVoice = ( ListPreference) this.findPreference(PreferenceKeys.speechVoice);
            if ( lpSpeechVoice != null ) {
                Map<String, String> mDisplayName2Name = Speak.getInstance().getVoices();
                List<String> lDisplayValues = new ArrayList<>(mDisplayName2Name.keySet());
                lpSpeechVoice.setEntries(lDisplayValues.toArray(new CharSequence[0]));
                List<String> lVoiceNames = new ArrayList<>();
                for(String sDPName: lDisplayValues) {
                    lVoiceNames.add(mDisplayName2Name.get(sDPName));
                }
                lpSpeechVoice.setEntryValues(lVoiceNames.toArray(new CharSequence[0]));

                if ( ListUtil.isNotEmpty(lVoiceNames) ) {
                    String sVoicePref = PreferenceValues.getSpeechVoice(getContext());
                    if ( StringUtil.isEmpty(sVoicePref) ) {
                        sVoicePref = lVoiceNames.get(0).toString();
                    }
                    lpSpeechVoice.setValue(sVoicePref);

                    Speak.getInstance().setVoice(sVoicePref);
                }
            }
        }

        @Override public void onResume() {
            super.onResume();
            PreferenceScreen screen = getPreferenceScreen();
            if ( screen == null ) { return; }
            RWValues.updatePreferenceTitle(screen, null, screen.getContext());

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
        private int setEnabledForPrefKeys(boolean bEnabled, PreferenceKeys... keys) {
            int iFoundAndSet = 0;
            for( PreferenceKeys key: keys ) {
                Preference pref = findPreference(key);
                if ( pref != null ) {
                    pref.setEnabled(bEnabled);
                    iFoundAndSet++;
                }
            }
            return iFoundAndSet;
        }

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

        private boolean hideRemovePreference(PreferenceGroup pgParent, Object oKey) {
            Preference pgChild = this.findPreference(String.valueOf(oKey));
            boolean bHidden = hideRemovePreference(pgParent, pgChild);
            if ( bHidden == false ) {
                Log.w(TAG, "Failed to hide " + oKey);
            }
            return bHidden;
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
        for ( ColorPrefs.ColorTarget colorTarget: ColorPrefs.ColorTarget.values() ) {
            ListPreference lp = (ListPreference) settingsFragment.findPreference(colorTarget);
            values = ColorPrefs.setListPreferenceEntries(lp, values, colorTarget);
        }
    }

    public static List<String> syncAndClean_warmupValues(Context ctx) {
        return syncAndClean_durationValues(ctx, PreferenceKeys.timerWarmup       , R.integer.timerWarmup_default__Squash
                                              , PreferenceKeys.timerWarmup_values, R.string.timerWarmup_values_default__Squash);
    }
    public static List<String> syncAndClean_pauseBetweenGamesValues(Context ctx) {
        return syncAndClean_durationValues(ctx, PreferenceKeys.timerPauseBetweenGames       , R.integer.timerPauseBetweenGames_default__Squash
                                              , PreferenceKeys.timerPauseBetweenGames_values, R.string.timerPauseBetweenGames_values_default__Squash);
    }
    private static List<String> syncAndClean_durationValues(Context ctx, PreferenceKeys pref, int iResDefaultValue, PreferenceKeys prefMV, int iResDefaultValues) {
        int iResDefault = PreferenceValues.getSportTypeSpecificResId(ctx, iResDefaultValues);

        String sValues = PreferenceValues.getString(prefMV, iResDefault, ctx);
        sValues = sValues.replaceAll("[^0-9,;]", "");
        List<String> lValues = new ArrayList<String>(Arrays.asList(sValues.split("[,;]")));
        int iCurrentValue   = PreferenceValues.getIntegerR(pref, ctx, iResDefaultValue); //
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
            PreferenceValues.setString(prefMV, ctx, sNewValue);
        }
        return lValues;
    }

    static void setModelDirty() {
        if ( ScoreBoard.getMatchModel() == null) { return; }
        ScoreBoard.getMatchModel().setDirty();
    }

    private static List<String> m_lColorSettingsChanged = new ArrayList<>();
    public static List<String> getChangedColorSettings() {
        List<String> lReturn = new ArrayList<>(m_lColorSettingsChanged);
        m_lColorSettingsChanged.clear();
        return lReturn;
    }

    private static List<PreferenceKeys> m_lSettingsChanged = new ArrayList<>();
    public static List<PreferenceKeys> getChangedSettings() {
        List<PreferenceKeys> lReturn = new ArrayList<>(m_lSettingsChanged);
        m_lSettingsChanged.clear();
        return lReturn;
    }

    private static List<String> m_lWearableSettingsChanged = new ArrayList<>();
    public static List<String> getWearableSettingsChanged() {
        List<String> lReturn = new ArrayList<>(m_lWearableSettingsChanged);
        m_lWearableSettingsChanged.clear();
        return lReturn;
    }

    private void clearPPA() {
        getPackageManager().clearPackagePreferredActivities(getPackageName());
    }

    public static Set<PreferenceKeys> GameSetMatch_SpecificPrefs = Set.of
            ( PreferenceKeys.changeSidesWhen_GSM
            , PreferenceKeys.finalSetFinish
            , PreferenceKeys.newBalls
            , PreferenceKeys.indicateGoldenPoint
            );
    public static Set<PreferenceKeys> NONGameSetMatch_SpecificPrefs = Set.of
            ( PreferenceKeys.indicateGameBall
            , PreferenceKeys.endGameSuggestion
            , PreferenceKeys.showDetailsAtEndOfGameAutomatically
            , PreferenceKeys.hapticFeedbackOnGameEnd
            , PreferenceKeys.AutomateWhatCanBeAutomatedPrefs
            , PreferenceKeys.swapPlayersBetweenGames
            , PreferenceKeys.swapPlayersHalfwayGame
            );
}
