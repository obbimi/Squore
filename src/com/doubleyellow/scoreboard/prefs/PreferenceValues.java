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

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Environment;
import android.preference.*;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;
import android.icu.util.TimeZone;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.doubleyellow.android.task.DownloadImageTask;
import com.doubleyellow.android.task.URLTask;
import com.doubleyellow.android.util.ContentUtil;
import com.doubleyellow.android.view.ViewUtil;
import com.doubleyellow.prefs.OrientationPreference;
import com.doubleyellow.prefs.RWValues;
import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.URLFeedTask;
import com.doubleyellow.scoreboard.archive.GroupMatchesBy;
import com.doubleyellow.scoreboard.dialog.MyDialogBuilder;
import com.doubleyellow.scoreboard.feed.Authentication;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.model.*;
import com.doubleyellow.scoreboard.model.Util;
import com.doubleyellow.scoreboard.speech.Speak;
import com.doubleyellow.scoreboard.timer.ViewType;
import com.doubleyellow.scoreboard.view.PreferenceCheckBox;
import com.doubleyellow.util.*;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.*;

/**
 * Class that programmatically provides Read/Write access to stored preferences.
 */
public class PreferenceValues extends RWValues
{
    private static Map<String, String> mOrgResId2BrandResId = new HashMap<>();

    public static int updatePreferenceTitleResId(Preference preference, Context context) {
        if ( preference == null ) { return 0; }

        if (preference instanceof PreferenceScreen) {
            // extension of PreferenceGroup
        }
        if (preference instanceof PreferenceCategory) {
            // extension of PreferenceGroup
        }
        int iChanged = 0;
        if (preference instanceof PreferenceGroup) {
            PreferenceGroup preferenceGroup = (PreferenceGroup) preference;
            for (int j = 0; j < preferenceGroup.getPreferenceCount(); ++j) {
                iChanged += updatePreferenceTitleResId(preferenceGroup.getPreference(j), context);
            }
        }

        int titleRes = preference.getTitleRes();
        Integer newTitleResId = getSportSpecificSuffixedResId(context, titleRes);
        if ( (newTitleResId != null) && (newTitleResId != 0) && (newTitleResId.equals(titleRes) == false) ) {
            Log.d(TAG, "replace: " + context.getString(titleRes));
            Log.d(TAG, "by     : " + context.getString(newTitleResId));
            preference.setTitle(newTitleResId);
            iChanged++;
        }
/*
        Integer newSummaryRes = getSportSpecificResId(context, preference.getSummaryRes());
        if ( newSummaryRes != null && newSummaryRes != 0 ) {
            preference.setSummary(newSummaryRes);
            iChanged++;
        }
*/
        return iChanged;
    }

    //public static final String removeSeedingRegExp = "[\\[\\]0-9/]+$";
    private static boolean m_restartRequired = false;
    static void setRestartRequired(Context ctx) {
/*
        if ( m_restartRequired == false ) {
            Toast.makeText(ctx, R.string.for_this_to_take_effect_a_restart_is_required, Toast.LENGTH_SHORT).show();
        }
*/
        m_restartRequired = true;
    }
    public static boolean isRestartRequired() {
        boolean bReturn = m_restartRequired;
        m_restartRequired = false;
        return bReturn;
    }

    private static boolean m_castRestartRequired = false;
    public static void setCastRestartRequired(Context ctx) {
        if ( m_castRestartRequired == false ) {
            //Toast.makeText(ctx, R.string.for_this_to_take_effect_a_restart_is_required, Toast.LENGTH_SHORT).show();
        }
        m_castRestartRequired = true;
    }
    public static boolean isCastRestartRequired() {
        boolean bReturn = m_castRestartRequired;
        m_castRestartRequired = false;
        return bReturn;
    }

    private static final String TAG = "SB." + PreferenceValues.class.getSimpleName();

    private PreferenceValues() {}

    private static final String COM_DOUBLEYELLOW_SCOREBOARD = "com.doubleyellow.scoreboard";

    public static boolean isRunningInMainCodeBase(Context ctx) {
        String packageName = ctx.getPackageName();
        return packageName.equals(PreferenceValues.COM_DOUBLEYELLOW_SCOREBOARD);
    }
    public static boolean isUnbrandedExecutable(Context ctx) {
        String packageName = ctx.getPackageName();
        return packageName.matches("com\\.doubleyellow\\..+");
    }
    public static boolean isBrandedExecutable(Context ctx) {
        return isUnbrandedExecutable(ctx) == false;
    }
    /** Uncomment more than 2 other brands than Squore in Brand.java to enter 'brandtesting' mode */
    public static boolean isBrandTesting(Context ctx) {
        if ( isUnbrandedExecutable(ctx) && ( Brand.values().length > 2 ) ) {
            return true;
        }
        return false;
    }

    public static Brand getOverwriteBrand(Context ctx) {
        return RWValues.getEnum(PreferenceKeys.squoreBrand, ctx, Brand.class, Brand.Squore);
    }

    static EnumSet<ShowOnScreen> showBrandLogoOn(Context context) {
        return getEnumSet(PreferenceKeys.showBrandLogoOn, context, ShowOnScreen.class, EnumSet.of(ShowOnScreen.OnChromeCast));
    }
    public static EnumSet<Sport> getDisciplineSequence(Context context) {
        // TODO:
        return getEnumSet(PreferenceKeys.disciplineSequence, context, Sport.class, EnumSet.allOf(Sport.class));
    }
    public static Set<ShowOnScreen> toggleMatchGameDurationChronoVisibility(Context context) {
        EnumSet<ShowOnScreen> showOnScreens = PreferenceValues.showLastGameDurationChronoOn(context);
        Set<ShowOnScreen> next = ListUtil.nextEnumSetBinary(showOnScreens, ShowOnScreen.class);
        PreferenceValues.setStringSet(PreferenceKeys.showMatchDurationChronoOn   , next, context);
        PreferenceValues.setStringSet(PreferenceKeys.showLastGameDurationChronoOn, next, context);
        return next;
    }

    static EnumSet<ShowOnScreen> showMatchDurationChronoOn(Context context) {
        if ( currentDateIsTestDate() ) {
            // to allow monkey testing via adb. on screen time prevents uiautomation to dump screen layout
            return EnumSet.of(ShowOnScreen.OnChromeCast);
        }
        return getEnumSet(PreferenceKeys.showMatchDurationChronoOn, context, ShowOnScreen.class, EnumSet.allOf(ShowOnScreen.class));
    }
    public static EnumSet<ShowOnScreen> showLastGameDurationChronoOn(Context context) {
        if ( currentDateIsTestDate() ) {
            // to allow monkey testing via adb. on screen time prevents uiautomation to dump screen layout
            return EnumSet.of(ShowOnScreen.OnChromeCast);
        }
        return getEnumSet(PreferenceKeys.showLastGameDurationChronoOn, context, ShowOnScreen.class, EnumSet.allOf(ShowOnScreen.class));
    }
    static EnumSet<ShowOnScreen> showFieldDivisionOn(Context context) {
        return getEnumSet(PreferenceKeys.showFieldDivisionOn, context, ShowOnScreen.class, EnumSet.allOf(ShowOnScreen.class));
    }
    public static boolean hideBrandLogoWhenGameInProgress(Context context) {
        return getBoolean(PreferenceKeys.hideBrandLogoWhenGameInProgress, context, R.bool.hideBrandLogoWhenGameInProgress_default);
    }
    public static boolean hideFieldDivisionWhenGameInProgress(Context context) {
        return getBoolean(PreferenceKeys.hideFieldDivisionWhenGameInProgress, context, R.bool.hideFieldDivisionWhenGameInProgress_default);
    }
    public static boolean showBrandLogo(Context context, boolean bIsPresentation) {
        EnumSet<ShowOnScreen> showOnScreens = showBrandLogoOn(context);
        if ( bIsPresentation ) {
            return showOnScreens.contains(ShowOnScreen.OnChromeCast);
        } else {
            return showOnScreens.contains(ShowOnScreen.OnDevice);
        }
    }
    public static boolean showMatchDuration(Context context, boolean bIsPresentation) {
        EnumSet<ShowOnScreen> showMatchDurationChronoOn = showMatchDurationChronoOn(context);
        if ( bIsPresentation ) {
            return showMatchDurationChronoOn.contains(ShowOnScreen.OnChromeCast);
        } else {
            return showMatchDurationChronoOn.contains(ShowOnScreen.OnDevice);
        }
    }

    public static boolean showLastGameDuration(Context context, boolean bIsPresentation) {
        EnumSet<ShowOnScreen> showLastGameDurationChronoOn = showLastGameDurationChronoOn(context);
        if ( bIsPresentation ) {
            return showLastGameDurationChronoOn.contains(ShowOnScreen.OnChromeCast);
        } else {
            return showLastGameDurationChronoOn.contains(ShowOnScreen.OnDevice);
        }
    }

    public static boolean showFieldDivision(Context context, boolean bIsPresentation) {
        EnumSet<ShowOnScreen> showFieldDivisionOn = showFieldDivisionOn(context);
        if ( bIsPresentation ) {
            return showFieldDivisionOn.contains(ShowOnScreen.OnChromeCast);
        } else {
            return showFieldDivisionOn.contains(ShowOnScreen.OnDevice);
        }
    }

/*
    public enum TextSize {
        PlayerName,
        ServeSide,
        Score,
        History,
    }
*/

    /** User may choose for a value between 0 and 100. We translate it to a value between 0 and 255 */
    public static int getServeButtonTransparencyNonServer(Context context) {
        int iResDefault = getSportTypeSpecificResId(context, R.integer.serveButtonTransparencyNonServer_default__Squash);

        // get user preference value ( must be between 0 and 100 )
        int i0To100Stored = getIntegerR(PreferenceKeys.serveButtonTransparencyNonServer, context, iResDefault);
        int i0To100 = i0To100Stored;
            i0To100 = Math.max(  0, i0To100);
            i0To100 = Math.min(100, i0To100);
        if (i0To100 != i0To100Stored ) {
            setNumber(PreferenceKeys.serveButtonTransparencyNonServer, context, i0To100);
        }

        int i0To255 = i0To100 * 255 / 100;
        return i0To255;
    }
    public static int getAppealHandGestureIconSize(Context context) {
        int iIconSize = getIntegerR(PreferenceKeys.AppealHandGestureIconSize, context, R.integer.AppealHandGestureIconSize_default);
        if ( iIconSize == context.getResources().getInteger(R.integer.AppealHandGestureIconSize_default) && (iIconSize != 0) ) {
            iIconSize = ViewUtil.getScreenHeightWidthMinimum(context) / 6;
            setNumber(PreferenceKeys.AppealHandGestureIconSize, context, iIconSize);
        }
        return iIconSize;
    }
    public static boolean showChoosenDecisionShortly(Context context) {
        return getBoolean(PreferenceKeys.showChoosenDecisionShortly, context, R.bool.showChoosenDecisionShortly_default);
    }
/*
    public static boolean getTextSizeScoreAsBigAsPossible(Context context) {
        return getBoolean(PreferenceKeys.TextSizeScoreAsBigAsPossible, context, R.bool.TextSizeScoreAsBigAsPossible_default);
    }
    public static int getServeSideTextSize(Context context) {
        return getTextSize(TextSize.ServeSide, context, R.integer.TextSizeServeSide_default);
    }
    public static int getScoreTextSize(Context context) {
        return getTextSize(TextSize.Score, context, R.integer.TextSizeScore_default);
    }
    public static int getPlayerNameTextSize(Context context) {
        return getTextSize(TextSize.PlayerName, context, R.integer.TextSizePlayerName_default);
    }
    public static int getHistoryTextSize(Context context) {
        return getTextSize(TextSize.History, context, R.integer.TextSizeHistory_default);
    }
*/
    public static int getCallResultMessageTextSize(Context context) {
        return ViewUtil.getScreenHeightWidthMinimumFraction(context, 0.08f);
        //return getTextSize(TextSize.ServeSide, context, R.integer.TextSizeServeSide_default);
    }
/*
    public static int getGameBallMessageTextSize(Context context) {
        return getTextSize(TextSize.PlayerName, context, R.integer.TextSizePlayerName_default);
    }
*/

    public static DetermineTextColor getTextColorDetermination(Context context) {
        return getEnum(PreferenceKeys.textColorDetermination, context, DetermineTextColor.class, DetermineTextColor.AutoChooseBlackOrWhite);
    }

/*
    public static int getTextSize(TextSize key, Context context, int iResourceDefault) {
        int iTextSize = context.getResources().getInteger(iResourceDefault);

        // reload all defaults
        SharedPreferences prefs = null;
        try {
            PreferenceManager.setDefaultValues(context, R.xml.preferences, false);
            prefs = PreferenceManager.getDefaultSharedPreferences(context);
        } catch (Exception e) {
            //e.printStackTrace();
        }
        if ( prefs != null ) {
            String sKey = TextSize.class.getSimpleName() + key.toString();
            try {
                iTextSize = prefs.getInt(sKey, iTextSize);
            } catch (ClassCastException cce) {
                String sValue = prefs.getString(sKey, null);
                try {
                    iTextSize = Integer.parseInt(sValue);
                } catch (Exception e) {
                }
            }
        }
        return iTextSize;
    }
    public static boolean setTextSize(TextSize key, Context context, int iValue) {
        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            prefs.edit().putInt(TextSize.class.getSimpleName() + key.toString(), iValue).apply();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
*/

    public static TieBreakFormat getTiebreakFormat(Context context) {
        return getEnum(PreferenceKeys.tieBreakFormat, context, TieBreakFormat.class, TieBreakFormat.TwoClearPoints);
    }
    public static FinalSetFinish getFinalSetFinish(Context context) {
        return getEnum(PreferenceKeys.finalSetFinish, context, FinalSetFinish.class, FinalSetFinish.TieBreakTo7);
    }
    public static NewBalls getNewBalls(Context context) {
        return getEnum(PreferenceKeys.newBalls, context, NewBalls.class, NewBalls.AfterFirst9ThenEach11);
    }
    public static int newBallsXGamesUpFront(Context context) {
        return getIntegerR(PreferenceKeys.newBallsXGamesUpFront, context, R.integer.newBallsXGamesUpFront__Default);
    }
    public static NewMatchLayout getNewMatchLayout(Context context) {
        if ( ViewUtil.isWearable(context) ) {
            //return NewMatchLayout.Simple; // font to big
        }
        return getEnum(PreferenceKeys.newMatchLayout, context, NewMatchLayout.class, R.string.newMatchLayout_default);
    }
    public static GameScoresAppearance getGameScoresAppearance(Context context) {
        if ( Brand.isRacketlon() || Brand.isGameSetMatch() ) {
            return GameScoresAppearance.ShowFullScore;
        }
        return getEnum(PreferenceKeys.gameScoresAppearance, context, GameScoresAppearance.class, GameScoresAppearance.ShowGamesWon);
    }
    public static LandscapeLayoutPreference getLandscapeLayout(Context context) {
        LandscapeLayoutPreference def = LandscapeLayoutPreference.Default;
        if ( Brand.isRacketlon() || Brand.isGameSetMatch() ) {
            def = LandscapeLayoutPreference.Default;
        }
        return getEnum(PreferenceKeys.LandscapeLayoutPreference, context, LandscapeLayoutPreference.class, def);
    }
    public static DoublesServeSequence getDoublesServeSequence(Context context) {
        DoublesServeSequence dssDefault = DoublesServeSequence.values()[0];
        return getEnum(PreferenceKeys.doublesServeSequence, context, DoublesServeSequence.class, dssDefault, Model.mOldDSS2New);
    }
    public static EnumSet<OrientationPreference> getOrientationPreference(Context context) {
        String[] values = context.getResources().getStringArray(R.array.OrientationPreferenceDefaultValues);
        EnumSet<OrientationPreference> eValues = EnumSet.copyOf(ListUtil.toEnumValues(Arrays.asList(values), OrientationPreference.class));
        return getEnumSet(PreferenceKeys.OrientationPreference, context, OrientationPreference.class, eValues);
    }
    public static EnumSet<ShowOnScreen> showScoringHistoryInMainScreenOn(Context context) {
        return getEnumSet(PreferenceKeys.showScoringHistoryInMainScreenOn, context, ShowOnScreen.class, EnumSet.of(ShowOnScreen.OnDevice));
    }

    public static boolean showScoringHistoryInMainScreen(Context context, boolean bIsPresentation) {
        EnumSet<ShowOnScreen> showOnScreens = showScoringHistoryInMainScreenOn(context);
        if ( bIsPresentation ) {
            return showOnScreens.contains(ShowOnScreen.OnChromeCast);
        } else {
            return showOnScreens.contains(ShowOnScreen.OnDevice);
        }
    }
/*
    public static boolean showScoringHistoryInMainScreen(Context context) {
        return getBoolean(PreferenceKeys.showScoringHistoryInMainScreen, context, R.bool.showScoringHistoryInMainScreen_default);
    }
*/

    public static boolean showActionBar(Context context) {
        boolean bDefault = context.getResources().getBoolean(R.bool.showActionBar_default);
        if ( ViewUtil.isWearable(context) ) {
            bDefault = false;
        }
        return getBoolean(PreferenceKeys.showActionBar, context, bDefault);
//        return getBoolean(PreferenceKeys.showActionBar, context, R.bool.showActionBar_default);
    }

    public static boolean showFullScreen(Context context) {
        return getBoolean(PreferenceKeys.showFullScreen, context, R.bool.showFullScreen_default);
    }

    public static boolean showTextInActionBar(Context context) {
        return getBoolean(PreferenceKeys.showTextInActionBar, context, R.bool.showTextInActionBar_default);
    }

    public static boolean blinkFeedbackPerPoint(Context context) {
        return getBoolean(PreferenceKeys.blinkFeedbackPerPoint, context, R.bool.blinkFeedbackPerPoint_default__Default);
    }
    public static int numberOfBlinksForFeedbackPerPoint(Context context) {
        return getInteger(PreferenceKeys.numberOfBlinksForFeedbackPerPoint, context, R.integer.numberOfBlinksForFeedbackPerPoint_default);
    }
    public static boolean hapticFeedbackPerPoint(Context context) {
        return getBoolean(PreferenceKeys.hapticFeedbackPerPoint, context, R.bool.hapticFeedbackPerPoint_default);
    }

    public static boolean hapticFeedbackOnGameEnd(Context context) {
        return getBoolean(PreferenceKeys.hapticFeedbackOnGameEnd, context, R.bool.hapticFeedbackOnGameEnd_default);
    }

    public static boolean showDetailsAtEndOfGameAutomatically(Context context) {
        return getBoolean(PreferenceKeys.showDetailsAtEndOfGameAutomatically, context, R.bool.showDetailsAtEndOfGameAutomatically_default);
    }

    public static boolean showLastGameInfoInTimer(Context context) {
        return getBoolean(PreferenceKeys.showLastGameInfoInTimer, context, R.bool.showLastGameInfoInTimer_default);
    }

    public static boolean showPauseButtonOnTimer(Context context) {
        return getBoolean(PreferenceKeys.showPauseButtonOnTimer, context, R.bool.showPauseButtonOnTimer_default);
    }

    public static boolean showHideButtonOnTimer(Context context) {
        return getBoolean(PreferenceKeys.showHideButtonOnTimer, context, R.bool.showHideButtonOnTimer_default);
    }

    public static boolean showCircularCountdownInTimer(Context context) {
        return getBoolean(PreferenceKeys.showCircularCountdownInTimer, context, R.bool.showCircularCountdownInTimer_default);
    }

    public static boolean cancelTimerWhenTimeIsUp(Context context) {
        return getBoolean(PreferenceKeys.cancelTimerWhenTimeIsUp, context, R.bool.cancelTimerWhenTimeIsUp_default);
    }
    public static ViewType timerViewType(Context context) {
        return getEnum(PreferenceKeys.timerViewType, context, ViewType.class, R.string.timerViewType_default);
    }
    public static boolean setTimerViewType(Context context, ViewType viewType) {
        return PreferenceValues.setEnum(PreferenceKeys.timerViewType, context, viewType);
    }
    public static Feature useTossFeature(Context context) {
        return getEnum(PreferenceKeys.useTossFeature, context, Feature.class, R.string.useTossFeature_default);
    }
    public static Feature removeMatchFromMyListWhenSelected(Context context) {
        return getEnum(PreferenceKeys.removeMatchFromMyListWhenSelected, context, Feature.class, Feature.DoNotUse);
    }
    public static Feature useShareFeature(Context context) {
        if ( ViewUtil.isWearable(context) ) {
            return Feature.DoNotUse;
        }
        return getEnum(PreferenceKeys.useShareFeature, context, Feature.class, R.string.useShareFeature_default);
    }
    public static Feature useSpeechFeature(Context context) {
        return getEnum(PreferenceKeys.useSpeechFeature, context, Feature.class, R.string.useSpeechFeature_default__Default);
    }
    public static boolean useFeatureYesNo(Feature f) {
        return EnumSet.of(Feature.Suggest, Feature.Automatic).contains(f);
        //return getBoolean(PreferenceKeys.useSpeechFeature, context, false);
    }
    public static float getSpeechPitch(Context context) {
        int iValue0To100 = getIntegerR(PreferenceKeys.speechPitch, context, R.integer.speechPitch_default);
        return zeroToHundredToFloat0To1(iValue0To100);
    }
    public static float getSpeechRate(Context context) {
        int iValue0To100 = getIntegerR(PreferenceKeys.speechRate, context, R.integer.speechRate_default);
        return zeroToHundredToFloat0To1(iValue0To100);
    }
    public static boolean speechOverBT_PlayWhiteNoiseSoundFileToKeepAlive(Context context) {
        return getBoolean(PreferenceKeys.speechOverBT_PlayWhiteNoiseSoundFileToKeepAlive, context, true);
    }
    public static String getSpeech_UrlOfSoundFileToPlayToKeepAlive(Context context) {
        // https://samplefocus.com/samples/fading-noises
        // - Crackle Subtle
        String S_AUDIOURL = null;
               S_AUDIOURL = "android.resource://" + context.getPackageName() + "/" + R.raw.white_noise_2s;
               //S_AUDIOURL = "android.resource://" + context.getPackageName() + "/" + R.raw.white_noise_4s;
               //S_AUDIOURL = "https://squore.double-yellow.be/white_noise_2s.aac";
               //S_AUDIOURL = "https://squore.double-yellow.be/white_noise_4s.mp3";
        return S_AUDIOURL;
    }
    public static int speechOverBT_PauseBetweenPlaysToKeepAlive(Context context) {
        return getIntegerR(PreferenceKeys.speechOverBT_PauseBetweenPlaysToKeepAlive, context, R.integer.speechOverBT_PauseBetweenPlaysToKeepAlive_default);
    }
    public static int speechOverBT_PlayingVolumeToKeepAlive(Context context) {
        return getIntegerR(PreferenceKeys.speechOverBT_PlayingVolumeToKeepAlive, context, R.integer.speechOverBT_speechOverBT_PlayingVolumeToKeepAlive_default);
    }
    public static String getSpeechVoice(Context context) {
        String sVoice = getString(PreferenceKeys.speechVoice, null, context);
        return sVoice;
    }

    public static int getSpeechPauseBetweenWords(Context context) {
        int iValueMS = getIntegerR(PreferenceKeys.speechPauseBetweenParts, context, R.integer.speechPauseBetweenParts_default);
        return iValueMS;
    }

    public static float zeroToHundredToFloat0To1(final int iValue0To100) {
        float fValue0To100 = Math.min((float) iValue0To100, 100f);
        fValue0To100 = Math.max(0f, fValue0To100);
        return (fValue0To100 / 100f);
    }

    public static ShareMatchPrefs getShareAction(Context context) {
        return getEnum(PreferenceKeys.shareAction, context, ShareMatchPrefs.class, ShareMatchPrefs.LinkWithFullDetails);
    }
    public static Feature useOfficialAnnouncementsFeature(Context context) {
        int iRes = getSportTypeSpecificResId(context, R.string.useOfficialAnnouncementsFeature_default__Squash);
        return getEnum(PreferenceKeys.useOfficialAnnouncementsFeature, context, Feature.class, iRes);
    }
    public static String getLiveScoreDeviceId(Context context) {
        return getDeviceId(PreferenceKeys.liveScoreDeviceId, context, true);
    }
    public static boolean isFCMEnabled(Context context) {
        return getBoolean(PreferenceKeys.FCMEnabled, context, R.bool.FCMEnabled_default);
    }
    public static boolean showToastMessageForEveryReceivedFCMMessage(Context context) {
        return getBoolean(PreferenceKeys.showToastMessageForEveryReceivedFCMMessage, context, R.bool.showToastMessageForEveryReceivedFCMMessage_default);
    }
    public static String getFCMDeviceId(Context context) {
        return getDeviceId(PreferenceKeys.liveScoreDeviceId, context, true);
    }
    private static String getDeviceId(PreferenceKeys key, Context context, boolean bGenerateIfNull) {
        String sID = RWValues.getString(key, null, context);
        if ( bGenerateIfNull && StringUtil.isEmpty(sID) ) {
            sID = DeviceIdPref.generateNewId();
            RWValues.setString(key, context, sID);
        }
        return sID;
    }
/*
    public static void initForLiveScoring(Context ctx, boolean bOnlyTemporary) {
        if ( bOnlyTemporary ) {
            // typically only to 'turn it on by default' for NewMatch Activity
            setOverwrite(PreferenceKeys.shareAction    , LiveScorePrefs.theOneForLiveScoring);
            setOverwrite(PreferenceKeys.useShareFeature, LiveScorePrefs.theFeatureForLiveScoring);
        } else {
            if ( FeedMatchSelector.mFeedPrefOverwrites.containsKey(PreferenceKeys.shareAction) == false ) {
                setEnum(PreferenceKeys.shareAction    , ctx, LiveScorePrefs.theOneForLiveScoring);
                setEnum(PreferenceKeys.useShareFeature, ctx, LiveScorePrefs.theFeatureForLiveScoring);
            }
        }
        String sliveScoreDeviceId = PreferenceValues.getLiveScoreDeviceId(ctx);
        if ( StringUtil.isEmpty(sliveScoreDeviceId) ) {
            sliveScoreDeviceId = DeviceIdPref.generateNewLivescoreId();
            setString(PreferenceKeys.liveScoreDeviceId, ctx, sliveScoreDeviceId);
        }
    }
    public static void initForNoLiveScoring(Context ctx) {
        if ( RWValues.isNotOverwritten(PreferenceKeys.shareAction) ) {
            setEnum(PreferenceKeys.shareAction    , ctx, ShareMatchPrefs.LinkWithFullDetails);
        }
        //removeOverwrite(PreferenceKeys.useShareFeature);
        if ( PreferenceValues.useShareFeature(ctx).equals(Feature.Automatic) ) {
            setEnum(PreferenceKeys.useShareFeature, ctx, Feature.Suggest);
        } else {
            // do not change if Feature.DoNotUse
        }
    }
    public static ShareMatchPrefs isConfiguredForLiveScore(Context ctx) {
        Feature shareFeature = useShareFeature(ctx);
        if ( Feature.Automatic.equals(shareFeature) ) {
            ShareMatchPrefs shareAction = getShareAction(ctx);
            if ( shareAction.alsoBeforeMatchEnd() ) {
                return shareAction;
            }
        }
        return null;
    }
*/
    public static void initForLiveScoring(Context ctx, boolean bOnlyTemporary) {
        if ( bOnlyTemporary) {
            setOverwrite(PreferenceKeys.postEveryChangeToSupportLiveScore, true);
        } else {
            setBoolean(PreferenceKeys.postEveryChangeToSupportLiveScore, ctx, true);
        }
    }
    public static void initForNoLiveScoring(Context ctx) {
        removeOverwrite(PreferenceKeys.postEveryChangeToSupportLiveScore);
        setBoolean(PreferenceKeys.postEveryChangeToSupportLiveScore, ctx, false);
    }
    public static boolean isConfiguredForLiveScore(Context ctx) {
        return getBoolean(PreferenceKeys.postEveryChangeToSupportLiveScore, ctx, false);
    }
    public static boolean turnOnLiveScoringForMatchesFromFeed(Context ctx) {
        return getBoolean(PreferenceKeys.turnOnLiveScoringForMatchesFromFeed, ctx, R.bool.turnOnLiveScoringForMatchesFromFeed_default);
    }

    // TODO: add to preferences.xml
    public static Feature switchToPlayerListIfMatchListOfFeedIsEmpty(Context context) {
        return getEnum(PreferenceKeys.switchToPlayerListIfMatchListOfFeedIsEmpty, context, Feature.class, Feature.Suggest);
    }
    public static AnnouncementLanguage officialAnnouncementsLanguage(Context context) {
        return getEnum(PreferenceKeys.officialAnnouncementsLanguage, context, AnnouncementLanguage.class, R.string.officialAnnouncementsLanguage_default);
    }
    public static Locale announcementsLocale(Context context) {
        AnnouncementLanguage language =officialAnnouncementsLanguage(context);
        return new Locale(language.toString());
    }
    public static void setAnnouncementLanguage(AnnouncementLanguage languageNew, Context context) {
        AnnouncementLanguage languageCur = officialAnnouncementsLanguage(context);
        if ( languageCur.equals(languageNew) == false ) {
            PreferenceValues.setEnum(PreferenceKeys.officialAnnouncementsLanguage, context, languageNew);
            PreferenceValues.clearOACache();

            Speak.getInstance().setLocale(new Locale(languageNew.toString()));
        }
    }
    public static boolean announcementLanguageDeviates(Context ctx) {
        AnnouncementLanguage language = officialAnnouncementsLanguage(ctx);
        String deviceLanguage = RWValues.getDeviceLanguage(ctx);
        return language.toString().equals(deviceLanguage) == false;
    }

    /**
     * Try to obtain a more specific value by simply suffixing a resource with the Brand name.
     * Do NOT use for resource strings also used in preferences.xml
     **/
    public static Integer getSportSpecificSuffixedResId(Context context, Integer iResIdNoSuffix) {
        if ( (iResIdNoSuffix == null) || (iResIdNoSuffix == 0) ) { return 0; }

        final String sResName  = context.getResources().getResourceName    (iResIdNoSuffix);
        final String sResType  = context.getResources().getResourceTypeName(iResIdNoSuffix);

        String sNewResName = null;
        if ( mOrgResId2BrandResId.containsKey(sResName) ) {
            sNewResName = mOrgResId2BrandResId.get(sResName);
        } else {
            sNewResName = sResName + "__" + Brand.brand;
            if ( sResType.equals("raw") ) {
                sNewResName = sNewResName.toLowerCase();
            }
        }
        int iNewResId = context.getResources().getIdentifier(sNewResName, sResType, context.getPackageName());
        if ( iNewResId == 0 ) {
            sNewResName = sResName + "__" + Brand.getSport();
            if ( sResType.equals("raw") ) {
                sNewResName = sNewResName.toLowerCase();
            }
            iNewResId = context.getResources().getIdentifier(sNewResName, sResType, context.getPackageName());
        }
        if ( iNewResId != 0 ) {
            mOrgResId2BrandResId.put(sResName, sNewResName);
            return iNewResId;
        } else {
            return iResIdNoSuffix;
        }
    }

    public static int getSportTypeSpecificResId(Context context, int iResidSquashSuffixed) {
        return getSportTypeSpecificResId(context, iResidSquashSuffixed, iResidSquashSuffixed);
    }
    /**
     * Translate a __(Squash|Default) suffixed STRING resource id in brand specific resource id.
     * Typically for resource also used as defaults in preferences.xml
     **/
    public static int getSportTypeSpecificResId(Context context, int iResidSquashSuffixed, int iDefault) {
        final String sResName = context.getResources().getResourceName    (iResidSquashSuffixed);
        final String sResType = context.getResources().getResourceTypeName(iResidSquashSuffixed);
        if ( sResName.matches(".+__[A-Z][A-Za-z]+$") ) {
            final String sResNameNoSuffix = sResName.replaceFirst("__[A-Z][A-Za-z]+$", "__");
            final String sResNameSuffix   = sResName.replaceFirst(".+__([A-Z][A-Za-z]+)$", "$1");
            String sNewResName = sResNameNoSuffix + Brand.brand;
            int iNewResId = context.getResources().getIdentifier(sNewResName, sResType, context.getPackageName());
            if ( iNewResId == 0 ) {
                sNewResName = sResNameNoSuffix + Brand.getSport();
                iNewResId = context.getResources().getIdentifier(sNewResName, sResType, context.getPackageName());
            }
            if ( (iNewResId == 0) && Brand.isPadel() ) {
                sNewResName = sResNameNoSuffix + "TennisPadel" /*+ Brand.TennisPadel*/;
                iNewResId = context.getResources().getIdentifier(sNewResName, sResType, context.getPackageName());
            }
            if ( iNewResId == 0 ) {
                sNewResName = sResNameNoSuffix + "Default";
                iNewResId = context.getResources().getIdentifier(sNewResName, sResType, context.getPackageName());
            }
            if ( iNewResId == 0 ) {
                Log.w(TAG, "======================= No specific " + Brand.getSport() + " resource for " + sResName);
                return iDefault;
            }
            return iNewResId;
        } else {
            // no specially formatted resource id, just return as-is
            return iResidSquashSuffixed;
        }
    }

    public static String getGameOrSetString(Context context, int iResid, Object... formatArgs) {
        String sValue = context.getString(iResid, formatArgs);
        String sGame  = context.getString(R.string.oa_game);
        String sSet   = context.getString(R.string.oa_set);
        switch ( Brand.getSport() ) {
            case Racketlon:
                sValue = sValue.replaceAll("\\b" + StringUtil.capitalize(sGame) + "\\b", StringUtil.capitalize(sSet));
                sValue = sValue.replaceAll("\\b" + sGame.toLowerCase() + "\\b", sSet.toLowerCase());
                return sValue;
        }
        return sValue;
    }

    private static SparseArray<String>   mOACache      = null;
    private static SparseArray<String[]> mOAArrayCache = null;
    static void clearOACache() {
        mOAArrayCache = null;
        mOACache      = null;
    }
    public static String[] getOAStringArray(Context ctx, int iResId) {
        if ( mOAArrayCache == null ) {
            getOAString(ctx, R.string.oa_stroke); // just to fill the cache
        }
        if ( MapUtil.isNotEmpty(mOAArrayCache) ) {
            return mOAArrayCache.get(iResId);
        } else {
            return ctx.getResources().getStringArray(iResId);
        }
    }

    private static final int[] iaOAResString = new int[] {
            R.string.oa_bestOfX_or_firstToY_games__to_z,
            R.string.oa_bestOfX_or_firstToY_games,
            R.string.oa_x_th_game,
            R.string.oa_game_to_x,
            R.string.oa_match_to_x,
            R.string.oa_1_game_all,
            //R.string.oa_x_leads_n_against_y,
            //R.string.oa_x_wins_n_against_y,
            R.string.oa_a_leads_xGamesToy,
            R.string.oa_a_wins_xGamesToy,
            R.string.oa_x_games_TO_y,
            R.string.oa_x_games_all,
            R.string.oa_x_to_serve__y_to_receive,
            R.string.oa_x_to_serve,
            R.string.oa_love_all,
            R.string.oa_love,
            R.string.oa_match_firstletter,
            R.string.oa_game_firstletter,
            R.string.oa_game,
            R.string.oa_games,
            R.string.oa_gameball,
            R.string.oa_matchball,
            R.string.oa_n_all__or__n_equal,
            R.string.oa_player_needs_2_clear_points,
            R.string.oa_halftime,
            R.string.oa_change_sides,
            R.string.oa_fifteen_seconds,
            R.string.oa_decision_colon,
            R.string.oa_yes_let,
            R.string.oa_no_let,
            R.string.oa_stroke,
            R.string.oa_stroke_to_x,
            R.string.oa_conduct_warning,
            R.string.oa_conduct_warning_x_for_type_y,
            R.string.oa_conduct_stroke,
            R.string.oa_conduct_x__stroke_to_y_for_type_t,
            R.string.oa_conduct_x__game_to_y_for_type_t,
            R.string.oa_conduct_game,
            R.string.oa_conduct_match,
            R.string.oal_let_requested_by,
            R.string.oal_misconduct_by,
            R.string.oal_warmup,
            R.string.oal_pause,
            R.string.oa_time,
    };
    private static final int[] iaOAResArray = new int[] {
        R.array.FirstSecondThirdFourthFifth
    };
    //private static final String[] sLeftRight_Symbols = {"\u25c4", "\u25ba"}; // filled triangle (gelijkbenig, lang gerekt) (NOT: only one of the 2 shows)
    //private static final String[] sLeftRight_Symbols = {"\u261a", "\u261b"}; // filled pointing finger (does not work)
    //private static final String[] sLeftRight_Symbols = {"\u25c0", "\u25b6"}; // filled triangle (gelijkzijdig)
    private static final String[] sLeftRight_Symbols = {"\u25c0\uFE0E", "\u25b6\uFE0E"}; // filled triangle (gelijkzijdig) escaped with \uFE0E to not use color?? since android 7
    //private static final String[] sLeftRight_Symbols = {"\u21fd", "\u21fe"}; // arrow with open head
    public static String getOAStringFirstLetter(Context ctx, int iResId) {
        String oaString = getOAString(ctx, iResId);
        if ( StringUtil.isNotEmpty(oaString) ) {
            return oaString.substring(0,1).toUpperCase();
        }
        return null;
    }
    public static String getOAString(Context ctx, int iResId, Object ... formats) {
        if ( mOACache == null ) {
            AnnouncementLanguage langAnnouncements = officialAnnouncementsLanguage(ctx);

            String sL_DeviceLanguage = ctx.getString(R.string.left_serveside_single_char);
            String sR_DeviceLanguage = ctx.getString(R.string.right_serveside_single_char);

            Resources     res    = ctx.getResources();
            Configuration resCfg = res.getConfiguration();
            Locale localeRestore = null; // locale to restore to later
            if ( resCfg.locale.getLanguage().equals(langAnnouncements.toString()) == false ) {
                localeRestore = resCfg.locale;
                Locale locale = new Locale(langAnnouncements.toString());
                res = newResources(res, locale);
            }

            mOACache = new SparseArray<String>();
            for(int iRes: iaOAResString) {
                String sCache = res.getString(iRes);
                mOACache.put(iRes, sCache);
            }
            if ( announcementLanguageDeviates(ctx) && (Brand.isGameSetMatch() == false) ) {
                String sL_AnnouncementLanguage = res.getString(R.string.left_serveside_single_char);
                String sR_AnnouncementLanguage = res.getString(R.string.right_serveside_single_char);
                if ( (sL_AnnouncementLanguage + sR_AnnouncementLanguage).equals(sL_DeviceLanguage + sR_DeviceLanguage) ) {
                    // keep using e.g. L and R if L and R of announcement language match those of device language
                    // dutch/german/english al have RL
                } else {
                    mOACache.put(R.string.left_serveside_single_char , sLeftRight_Symbols[0]);
                    mOACache.put(R.string.right_serveside_single_char, sLeftRight_Symbols[1]);
                }
            }

            mOAArrayCache = new SparseArray<String[]>();
            for(int iRes: iaOAResArray) {
                mOAArrayCache.put(iRes, ctx.getResources().getStringArray(iRes));
            }
            res = newResources(res, localeRestore);
        }
        String s = null;
        if ( MapUtil.isNotEmpty(mOACache) ) {
            s = mOACache.get(iResId);
        }
        if ( StringUtil.isNotEmpty(s) ) {
            // we have a cached string, do the formatting
            return String.format(s, formats);
        } else {
            // default android call
            return ctx.getString(iResId, formats);
        }
    }
    public static Resources newResources(Resources res, Locale locale) {
        AssetManager   assets  = res.getAssets();
        DisplayMetrics metrics = res.getDisplayMetrics();
        Configuration  config  = new Configuration(res.getConfiguration());
        config.locale = locale;
        res           = new Resources(assets, metrics, config);
        return res;
    }

    public static Feature useTimersFeature(Context context) {
        return getEnum(PreferenceKeys.useTimersFeature, context, Feature.class, R.string.useTimersFeature_default__Default);
    }
    public static BackKeyBehaviour backKeyBehaviour(Context context) {
        return getEnum(PreferenceKeys.BackKeyBehaviour, context, BackKeyBehaviour.class, R.string.BackKeyBehaviour_default);
    }
    public static VolumeKeysBehaviour volumeKeysBehaviour(Context context) {
        return getEnum(PreferenceKeys.VolumeKeysBehaviour, context, VolumeKeysBehaviour.class, R.string.VolumeKeysBehaviour_default);
    }

    public static boolean showNewMatchFloatButton(Context context) {
        return getBoolean(PreferenceKeys.showNewMatchFloatButton, context, R.bool.showNewMatchFloatButton_default);
    }
    public static boolean useSoundNotificationInTimer(Context context) {
        return getBoolean(PreferenceKeys.useSoundNotificationInTimer, context, R.bool.useSoundNotificationInTimer_default);
    }
    public static boolean useVibrationNotificationInTimer(Context context) {
        return getBoolean(PreferenceKeys.useVibrationNotificationInTimer, context, R.bool.useVibrationNotificationInTimer_default);
    }
    public static boolean showAdjustTimeButtonsInTimer(Context context) {
        return getBoolean(PreferenceKeys.showAdjustTimeButtonsInTimer, context, R.bool.showAdjustTimeButtonsInTimer_default);
    }
    public static boolean showUseAudioCheckboxInTimer(Context context) {
        return getBoolean(PreferenceKeys.showUseAudioCheckboxInTimer, context, R.bool.showUseAudioCheckboxInTimer_default);
    }
    public static boolean showTimeIsAlreadyUpFor_Chrono(Context context) {
        return getBoolean(PreferenceKeys.showTimeIsAlreadyUpFor_Chrono, context, R.bool.showTimeIsAlreadyUpFor_Chrono_default);
    }

    public static boolean saveMatchesForLaterUsage(Context context) {
        return getBoolean(PreferenceKeys.saveMatchesForLaterUsage, context, R.bool.saveMatchesForLaterUsage_default);
    }
/*
    public static boolean archivedMatchesInSeparateActivity(Context context) {
        return true;
    }
*/
    public static SortOrder sortOrderOfArchivedMatches(Context context) {
        return getEnum(PreferenceKeys.sortOrderOfArchivedMatches, context, SortOrder.class, R.string.sortOrderOfArchivedMatches_default);
    }
    public static GroupMatchesBy groupArchivedMatchesBy(Context context) {
        return getEnum(PreferenceKeys.groupArchivedMatchesBy, context, GroupMatchesBy.class, R.string.groupArchivedMatchesBy_default);
    }
    public static ScorelineLayout getScorelineLayout(Context context) {
        int iResDefault = getSportTypeSpecificResId(context, R.string.scorelineLayout_default__Squash);
        return getEnum(PreferenceKeys.scorelineLayout, context, ScorelineLayout.class, iResDefault);
    }
    public static boolean useFeedAndPostFunctionality(Context context) {
        if ( Brand.isGameSetMatch() && currentDateIsTestDate() ) {
            return false;
        }
        int iResBrandSpecific = getSportSpecificSuffixedResId(context, R.bool.useFeedAndPostFunctionality_default);
        return getBoolean(PreferenceKeys.useFeedAndPostFunctionality, context, iResBrandSpecific);
    }
    public static boolean useSinglesMatches(Context context) {
        int iResBrandSpecific = getSportTypeSpecificResId(context, R.bool.useSinglesMatch__Default);
        return context.getResources().getBoolean(iResBrandSpecific);
    }
    public static boolean isPublicApp(Context context) {
        int iResBrandSpecific = getSportTypeSpecificResId(context, R.bool.isPublicApp__Default);
        return context.getResources().getBoolean(iResBrandSpecific);
    }
    public static boolean useMyListFunctionality(Context context) {
        int iResBrandSpecific = getSportTypeSpecificResId(context, R.bool.useMyListFunctionality__Default);
        return context.getResources().getBoolean(iResBrandSpecific);
    }
    public static boolean useWarmup(Context context) {
        int iResBrandSpecific = getSportTypeSpecificResId(context, R.bool.useWarmup__Default);
        return context.getResources().getBoolean(iResBrandSpecific);
    }
    public static boolean useReferees(Context context) {
        int iResBrandSpecific = getSportTypeSpecificResId(context, R.bool.useReferees__Default);
        return context.getResources().getBoolean(iResBrandSpecific);
    }
    public static String getBLEBridge_ClassName(Context context) {
        int iBLEBridge_ClassName_defaultResId = PreferenceValues.getSportTypeSpecificResId(context, R.string.BLEBridge_ClassName__Squash, R.string.BLEBridge_ClassName__Default);
        String sClass = PreferenceValues.getString(PreferenceKeys.BLEBridge_ClassName, iBLEBridge_ClassName_defaultResId, context);
        return sClass;
    }
    public static boolean useBluetoothLE(Context context) {
        int iResBrandSpecific = getSportSpecificSuffixedResId(context, R.bool.UseBluetoothLE_default);
        return getBoolean(PreferenceKeys.UseBluetoothLE, context, iResBrandSpecific);
    }
    public static boolean showFeedBackOnBLEButtonsPressedInfoMessages(Context context) {
        return getBoolean(PreferenceKeys.ShowFeedBackOnBLEButtonsPressedInfoMessages, context, R.bool.ShowFeedBackOnBLEButtonsPressedInfoMessages_default__Default);
    }
    public static int nrOfSecondsBeforeNotifyingBLEDeviceThatConfirmationIsRequired(Context context) {
        return getInteger(PreferenceKeys.NrOfSecondsBeforeNotifyingBLEDeviceThatConfirmationIsRequired, context, R.integer.NrOfSecondsBeforeNotifyingBLEDeviceThatConfirmationIsRequired_default);
    }
    public static int IgnoreAccidentalDoublePress_ThresholdInMilliSeconds(Context context) {
        return getInteger(PreferenceKeys.IgnoreAccidentalDoublePress_ThresholdInMilliSeconds, context, R.integer.IgnoreAccidentalDoublePress_ThresholdInMilliSeconds_default);
    }

    public static boolean useMQTT(Context context) {
        int iResBrandSpecific = getSportTypeSpecificResId(context, R.bool.UseMQTT_default__Default);
        return getBoolean(PreferenceKeys.UseMQTT, context, iResBrandSpecific);
    }
    public static String getMQTTBrokerURL(Context context) {
        String sCustom = getMQTTBrokerURL_Custom(context);
        String sOneOfList = getString(PreferenceKeys.MQTTBrokerURL, null, context);
        String[] saValues = context.getResources().getStringArray(R.array.MQTTBrokerUrls);
        String sCustomSelected = saValues[saValues.length - 1]; // TODO: dangerous assumption
        if ( StringUtil.isEmpty(sOneOfList) ) {
            return saValues[0];
        } else {
            if ( sOneOfList.equals(sCustomSelected) ) {
                return sCustom;
            } else {
                return sOneOfList;
            }
        }
    }

    public static String getMQTTBrokerURL_Custom(Context context) {
        return getString(PreferenceKeys.MQTTBrokerURL_Custom, R.string.MQTTBrokerURL_Custom__Default, context);
    }

    public static String getMQTTPublishTopicMatch(Context context) {
        return getString(PreferenceKeys.MQTTPublishTopicMatch, R.string.MQTTTopicMatchTemplate__Default, context);
    }
    public static List<String> getMQTTSkipJsonKeys(Context context) {
        return getStringAsList(context, PreferenceKeys.MQTTSkipJsonKeys, 0);
    }
    public static String getMQTTPublishTopicChange(Context context) {
        return getString(PreferenceKeys.MQTTPublishTopicChange, R.string.MQTTTopicChangeTemplate__Default, context);
    }
    public static String getMQTTSubscribeTopicChange(Context context) {
        return getString(PreferenceKeys.MQTTSubscribeTopicChange, R.string.MQTTTopicChangeTemplate__Default, context);
    }
    public static String getMQTTPublishJoinerLeaverTopic(Context context) {
        return getString(PreferenceKeys.MQTTPublishJoinerLeaverTopic, R.string.MQTTPublishJoinerLeaverTopic__Default, context);
    }
    public static String getMQTTOtherDeviceId(Context context) {
        return getString(PreferenceKeys.MQTTOtherDeviceId, "", context).toUpperCase();
    }

    public static boolean allowTrustAllCertificatesAndHosts(Context context) {
        return getBoolean(PreferenceKeys.allowTrustAllCertificatesAndHosts, context, R.bool.allowTrustAllCertificatesAndHosts_default);
    }
    public static Feature recordRallyEndStatsAfterEachScore(Context context) {
        return getEnum(PreferenceKeys.recordRallyEndStatsAfterEachScore, context, Feature.class, Feature.DoNotUse);
    }
    // TODO: add to preference screen if more options become available
/*
    public static boolean sortMatchesByEvent(Context context) {
        return getBoolean(PreferenceKeys.sortMatchesByEvent, context, R.bool.sortMatchesByEvent_default);
    }
*/
/*
    public static LockPreference lockMatch(Context context) {
        return getEnum(PreferenceKeys.lockMatch, context, LockPreference.class, LockPreference.AutoLockWhenMatchIsFinished);
    }
*/
    public static EnumSet<AutoLockContext> lockMatchMV(Context context) {
        return getEnumSet(PreferenceKeys.lockMatchMV, context, AutoLockContext.class, EnumSet.allOf(AutoLockContext.class));
    }
    public static EnumSet<ShowPlayerColorOn> showPlayerColorOn(Context context) {
        return getEnumSet(PreferenceKeys.showPlayerColorOn, context, ShowPlayerColorOn.class, EnumSet.allOf(ShowPlayerColorOn.class)); // ShowPlayerColorOn__Default
    }
    public static PlayerColorsNewMatch playerColorsNewMatch(Context context) {
        return getEnum(PreferenceKeys.PlayerColorsNewMatch, context, PlayerColorsNewMatch.class, R.string.PlayerColorsNewMatch__Default);
    }
    public static boolean showPlayerColorOn_Text(Context context) {
        return getBoolean(PreferenceKeys.showPlayerColorOn_Text, context, R.bool.showPlayerColorOn_Text_default);
    }
    public static EnumSet<ShowCountryAs> showCountryAs(Context context) {
        EnumSet<ShowCountryAs> esDefault = EnumSet.of(ShowCountryAs.FlagNextToNameOnDevice, ShowCountryAs.FlagNextToNameChromeCast); // ShowCountryAs_DefaultValues in xml takes precedence
        return getEnumSet(PreferenceKeys.showCountryAs, context, ShowCountryAs.class, esDefault);
    }
    public static EnumSet<ShowAvatarOn> showAvatarOn(Context context) {
        return getEnumSet(PreferenceKeys.showAvatarOn, context, ShowAvatarOn.class, EnumSet.of(ShowAvatarOn.OnDevice, ShowAvatarOn.OnChromeCast));
    }
    public static int getPreferWhiteOverBlackThreshold(Context context) {
        int iResDef = getSportSpecificSuffixedResId(context, R.integer.preferWhiteOverBlackThreshold);
        return getIntegerR(ColorPrefs.ColorTarget.preferWhiteOverBlackThreshold, context, iResDef);
    }
    public static boolean hideFlagForSameCountry(Context context) {
        return getBoolean(PreferenceKeys.hideFlagForSameCountry, context, R.bool.hideFlagForSameCountry_default);
    }
    public static boolean hideAvatarForSameImage(Context context) {
        return getBoolean(PreferenceKeys.hideAvatarForSameImage, context, R.bool.hideAvatarForSameImage_default);
    }
    public static boolean prefetchFlags(Context context) {
        return getBoolean(PreferenceKeys.prefetchFlags, context, R.bool.prefetchFlags_default);
    }
    public static boolean useFlags(Context context) {
        final EnumSet<ShowCountryAs> showCountryAs = showCountryAs(context);
        return showCountryAs.contains(ShowCountryAs.FlagNextToNameChromeCast) || showCountryAs.contains(ShowCountryAs.FlagNextToNameOnDevice);
    }
    public static boolean useCountries(Context context) {
        final EnumSet<ShowCountryAs> showCountryAs = showCountryAs(context);
        return ListUtil.isNotEmpty(showCountryAs);
    }
    public static boolean useAvatars(Context context) {
        final EnumSet<ShowAvatarOn> showAvatarOn = showAvatarOn(context);
        return ListUtil.isNotEmpty(showAvatarOn);
    }

    public static EnumSet<RallyEndStatsPrefs> recordRallyEndStatsDetails(Context context) {
        String[] values = context.getResources().getStringArray(R.array.RallyEndStatsPrefsDefaultValues);
        EnumSet<RallyEndStatsPrefs> eValues = EnumSet.copyOf(ListUtil.toEnumValues(Arrays.asList(values), RallyEndStatsPrefs.class));
        return getEnumSet(PreferenceKeys.recordRallyEndStatsDetails, context, RallyEndStatsPrefs.class, eValues);
    }
    public static int numberOfMinutesAfterWhichToLockMatch(Context context) {
        return getIntegerR(PreferenceKeys.numberOfMinutesAfterWhichToLockMatch, context, R.integer.numberOfMinutesAfterWhichToLockMatch_default);
    }
    public static NewMatchesType getNewMatchesType(Context context) {
        int iResDefault = getSportTypeSpecificResId(context, R.string.NewMatchesType_default__Squash);
        return getEnum(PreferenceKeys.NewMatchesType, context, NewMatchesType.class, iResDefault);
    }
    public static int maxNumberOfPlayersInGroup(Context context) {
        int iResDefault = getSportTypeSpecificResId(context, R.integer.maxNumberOfPlayersInGroup_default__Default);
        return getIntegerR(PreferenceKeys.maxNumberOfPlayersInGroup, context, iResDefault);
    }
    public static int getTournamentWasBusy_DaysBack(Context context) {
        return getIntegerR(PreferenceKeys.tournamentWasBusy_DaysBack, context, R.integer.tournamentWasBusy_DaysBack_default);
    }
    public static int getTournamentWillStartIn_DaysAhead(Context context) {
        return getIntegerR(PreferenceKeys.tournamentWillStartIn_DaysAhead, context, R.integer.tournamentWillStartIn_DaysAhead_default);
    }
    public static int getTournamentMaxDuration_InDays(Context context) {
        return getIntegerR(PreferenceKeys.tournamentMaxDuration_InDays, context, R.integer.tournamentMaxDuration_InDays_default);
    }
    public static Feature endGameSuggestion(Context context) {
        int iResDefault = getSportTypeSpecificResId(context, R.string.endGameSuggestion_default__Squash);
        return getEnum(PreferenceKeys.endGameSuggestion, context, Feature.class, iResDefault);
    }
    /** for tabletennis and racketlon, not squash */
    public static boolean swapPlayersOn180DegreesRotationOfDeviceInLandscape(Context context) {
        int iResDefault = getSportTypeSpecificResId(context, R.bool.swapPlayersOn180DegreesRotationOfDeviceInLandscape_default__Squash);
        return getBoolean(PreferenceKeys.swapPlayersOn180DegreesRotationOfDeviceInLandscape, context, iResDefault);
    }
    /** @Deprecated for tabletennis, not squash or racketlon */
    public static boolean swapSidesBetweenGames(Context context) {
        return getBoolean(PreferenceKeys.swapPlayersBetweenGames, context, R.bool.swapPlayersBetweenGames_default);
    }
    public static EnumSet<ChangeSidesWhen_GSM> changeSidesWhen_GSM(Context context) {
        EnumSet<ChangeSidesWhen_GSM> defaultValues = EnumSet.of( ChangeSidesWhen_GSM.AfterOddGames
                                                               , ChangeSidesWhen_GSM.EverySixPointsInTiebreak
                                                               );
        return getEnumSet(PreferenceKeys.changeSidesWhen_GSM, context, ChangeSidesWhen_GSM.class, defaultValues);
    }
    public static Feature useChangeSidesFeature(Context context) {
        return getEnum(PreferenceKeys.useChangeSidesFeature, context, Feature.class, R.string.useChangeSidesFeature_default__Default);
    }
    /** for tabletennis, not squash or racketlon */
    public static Feature showGamePausedDialog(Context context) {
        return getEnum(PreferenceKeys.showGamePausedDialog, context, Feature.class, R.string.showGamePausedDialog_default);
    }
    public static int autoShowGamePausedDialogAfterXPoints(Context context) {
        int iResDefault = getSportTypeSpecificResId(context, R.integer.autoShowGamePausedDialogAfterXPoints_default);
        return getIntegerR(PreferenceKeys.autoShowGamePausedDialogAfterXPoints, context, iResDefault);
    }
    public static boolean autoShowModeActivationDialog(Context context) {
        return getBoolean(PreferenceKeys.autoShowModeActivationDialog, context, false);
    }
    public static int showModeDialogAfterXMins(Context context) {
        int iResDefault = getSportTypeSpecificResId(context, R.integer.showModeDialogAfterXMins_default__Squash);
        return getIntegerR(PreferenceKeys.showModeDialogAfterXMins, context, iResDefault);
    }

    /** not for squash, for racketlon (all but squash, except for doubles), for tabletennis in last game */
    public static Feature swapSidesHalfwayGame(Context context) {
        return getEnum(PreferenceKeys.swapPlayersHalfwayGame, context, Feature.class, R.string.swapPlayersHalfwayGame_default);
    }
    public static DownUp numberOfServiceCountUpOrDown(Context context) {
        return getEnum(PreferenceKeys.numberOfServiceCountUpOrDown, context, DownUp.class, DownUp.Down);
    }
    public static Feature continueRecentMatch(Context context) {
        return getEnum(PreferenceKeys.continueRecentMatch, context, Feature.class, Feature.Automatic);
    }
    public static boolean useHandInHandOutScoring(Context context) {
        return getBoolean(PreferenceKeys.useHandInHandOutScoring, context, R.bool.useEnglishScoring_default);
    }
    public static GoldenPointFormat goldenPointFormat(Context context) {
        // for backwards compatibility
        GoldenPointFormat goldenPointFormatDefault = useGroupNameAsEventData(context) ? GoldenPointFormat.OnFirstDeuce : GoldenPointFormat.None;
        return getEnum(PreferenceKeys.goldenPointFormat, context, GoldenPointFormat.class, goldenPointFormatDefault);
    }
    public static boolean startTiebreakOneGameEarly(Context context) {
        // TODO: add to Preferences.xml ?
        return getBoolean(PreferenceKeys.StartTiebreakOneGameEarly, context, false);
    }
    public static KeepScreenOnWhen keepScreenOnWhen(Context context) {
        return getEnum(PreferenceKeys.keepScreenOnWhen, context, KeepScreenOnWhen.class, KeepScreenOnWhen.MatchIsInProgress);
    }
/*
    private static boolean keepScreenOn(Context context) {
        return getBoolean(PreferenceKeys.keepScreenOn, context, R.bool.keepScreenOn_default);
    }
*/
    public static boolean indicateGameBall(Context context) {
        return getBoolean(PreferenceKeys.indicateGameBall, context, R.bool.indicateGameBall_default);
    }
/*
    public static boolean floatingMessageForGameBall(Context context) {
        return getBoolean(PreferenceKeys.floatingMessageForGameBall, context, R.bool.floatingMessageForGameBall_default);
    }
*/
    private static EnumSet<ShowOnScreen> floatingMessageForGameBallOn(Context context) {
        int iRes = R.array.floatingMessageForGameBallOn_DefaultValues__Squash; // value used in preferences.xml and therefor used
        return getEnumSet(PreferenceKeys.floatingMessageForGameBallOn, context, ShowOnScreen.class, EnumSet.of(ShowOnScreen.OnDevice, ShowOnScreen.OnChromeCast));
    }
    public static boolean floatingMessageForGameBall(Context context, boolean bIsPresentation) {
        if ( bIsPresentation ) {
            return floatingMessageForGameBallOn(context).contains(ShowOnScreen.OnChromeCast);
        } else {
            return floatingMessageForGameBallOn(context).contains(ShowOnScreen.OnDevice);
        }
    }
    public static int useCastScreen(Context context) {
        return RWValues.getInteger(PreferenceKeys.useCastScreen, context, 0);
    }
    public static String castScreenLogoUrl(Context context) {
        return RWValues.getString(PreferenceKeys.castScreenLogoUrl, "", context);
    }
    public static boolean castScreenShowLogo(Context context) {
        return RWValues.getBoolean(PreferenceKeys.castScreenShowLogo, context, R.bool.castScreenShowLogo_default);
    }
    public static String castScreenSponsorUrl(Context context) {
        return RWValues.getString(PreferenceKeys.castScreenSponsorUrl, "", context);
    }
    public static boolean castScreenShowSponsor(Context context) {
        return RWValues.getBoolean(PreferenceKeys.castScreenShowSponsor, context, R.bool.castScreenShowSponsor_default);
    }
/*
    public static boolean Cast_ShowGraphDuringTimer(Context context) {
        return getBoolean(PreferenceKeys.Cast_ShowGraphDuringTimer, context, R.bool.Cast_ShowGraphDuringTimer_default);
    }
*/

    public static boolean BTSync_keepLROnConnectedDeviceMirrored(Context context) {
        int iResDefault = getSportTypeSpecificResId(context, R.bool.BTSync_keepLROnConnectedDeviceMirrored_default__Squash);
        return getBoolean(PreferenceKeys.BTSync_keepLROnConnectedDeviceMirrored, context, iResDefault);
    }
    public static boolean BTSync_showFullScreenTimer(Context context) {
        int iResDefault = getSportTypeSpecificResId(context, R.bool.BTSync_showFullScreenTimer_default__Squash);
        return getBoolean(PreferenceKeys.BTSync_showFullScreenTimer, context, iResDefault);
    }
    public static boolean wearable_syncColorPrefs(Context context) {
        int iResDefault = getSportTypeSpecificResId(context, R.bool.wearable_syncColorPrefs_default);
        return getBoolean(PreferenceKeys.wearable_syncColorPrefs, context, iResDefault);
    }
    public static boolean wearable_allowScoringWithHardwareButtons(Context context) {
        int iResDefault = getSportTypeSpecificResId(context, R.bool.wearable_allowScoringWithHardwareButtons_default);
        return getBoolean(PreferenceKeys.wearable_allowScoringWithHardwareButtons, context, iResDefault);
    }
    public static boolean wearable_allowScoringWithRotary(Context context) {
        int iResDefault = getSportTypeSpecificResId(context, R.bool.wearable_allowScoringWithRotary_default);
        return getBoolean(PreferenceKeys.wearable_allowScoringWithRotary, context, iResDefault);
    }
/*
    public static boolean showGraphDuringTimer(Context context, boolean bIsPresentation) {
        EnumSet<ShowOnScreen> showOnScreens = showGraphDuringTimerOn(context);
        if ( bIsPresentation ) {
            return showOnScreens.contains(ShowOnScreen.OnChromeCast);
        } else {
            return showOnScreens.contains(ShowOnScreen.OnDevice);
        }
    }
    static EnumSet<ShowOnScreen> showGraphDuringTimerOn(Context context) {
        return getEnumSet(PreferenceKeys.showGraphDuringTimerOn, context, ShowOnScreen.class, EnumSet.of(ShowOnScreen.OnChromeCast));
    }
*/

    private static boolean readContactsForAutoCompletion(Context context) {
        boolean bReadContactsPref = getBoolean(PreferenceKeys.readContactsForAutoCompletion, context, R.bool.readContactsForAutoCompletion_default);
        Permission bHasPermission  = doesUserHavePermissionToReadContacts(context, false);
/*
        if ( bReadContactsPref && (bHasPermission == null) ) {
            doesUserHavePermissionToReadContacts(context, true);
        }
*/

/*
        if ( (bHasPermission == false) && bReadContactsPref ) {
            int runCount = getRunCount(context, PreferenceKeys.readContactsForAutoCompletion, 1);
            String sMsg = "Permission to read contacts not granted. Contacts not used for autocompletion of player names";
            if ( runCount < 3 ) {
                Toast.makeText(context, sMsg, Toast.LENGTH_LONG).show();
            }
            Log.d(TAG, sMsg);
        }
*/
        return (bHasPermission==Permission.Granted) && bReadContactsPref;
    }

    public static int getDemoBackgroundColor(Context context) {
        return context.getResources().getColor(R.color.translucent_orange);
    }

/*
    public static List<String> getEventList(Context context) {
        return getStringAsList(context, PreferenceKeys.eventList, R.string.eventList_default);
    }
*/
    /** Returns the players as stored in the preferences of the app */
    private static List<String> getPlayerList(Context context) {
        return getStringAsList(context, PreferenceKeys.playerList, R.string.playerList_default);
    }
    private static List<String> m_playerList = null;
    public static List<String> getPlayerListAndContacts(Context context) {
        if ( ListUtil.isNotEmpty(m_playerList) ) {
            return m_playerList;
        }
        m_playerList = getPlayerList(context);
        if ( readContactsForAutoCompletion(context) ) {
            List<String> contacts = getAllowedContacts(context);
            if ( ListUtil.isNotEmpty(contacts) ) {
                m_playerList.addAll(contacts);
            }
        } else {
            //Log.d(TAG, "Not reading contacts");
        }
        if ( ListUtil.size(m_playerList) < 500 ) {
            // to expensive operation for large number of players
            m_playerList = ListUtil.removeDuplicates(m_playerList);
        }
        return m_playerList;
    }
    static void clearPlayerListCache() {
        if ( m_playerList == null ) { return; }
        m_playerList.clear();
    }

    public static boolean allowNegativeHandicap(Context context) {
        return getBoolean(PreferenceKeys.allowNegativeHandicap, context, R.bool.allowNegativeHandicap_default);
    }
    private static List<String> getAllowedContacts(Context context) {
        if ( readContactsForAutoCompletion(context) == false ) { return null; }

        int iGroupId = getInteger(PreferenceKeys.onlyForContactGroups, context, -1);
        int iMaxNrOfContacts = getIntegerR(PreferenceKeys.readContactsForAutoCompletion_max, context, R.integer.readContactsForAutoCompletion_max_default);
        List<String> contacts = null;
        if ( iGroupId <= 0 ) {
            contacts = getContacts(context, 500, iMaxNrOfContacts);
        } else {
            contacts = getContactsFromGroup(context, iGroupId);
        }
        return contacts;
    }

    public static int addFeedTypeToMyList(Context context, String sType) {
        return addStringToTopOfList(context, PreferenceKeys.myUsedFeedTypes, sType);
    }
    public static List<String> getUsedFeedTypes(Context context) {
        return getStringAsList(context, PreferenceKeys.myUsedFeedTypes, 0);
    }

    public static int addPlayersToList(Context context, Collection<String> players) {
        players = ListUtil.replace(players, Util.removeSeedingRegExp, "");
        List<String> lCurrent = getPlayerListAndContacts(context);
        players.removeAll(lCurrent);
        m_playerList.addAll(players); // add it to the cached version as well so it won't get added twice accidentally
        return addStringsToList(context, PreferenceKeys.playerList, players);
    }

    public static boolean addPlayerToList(Context context, String sPlayer) {
        // do not add default labels of player name buttons
        if ( sPlayer.matches(context.getString(R.string.lbl_player) + "\\s*[AB]") ) {
            return false;
        }
        if ( StringUtil.size(sPlayer) < 2 ) {
            return false;
        }
        sPlayer = Util.removeSeeding(sPlayer);
        if ( useCountries(context) == false ) {
            sPlayer = Util.removeCountry(sPlayer);
        }

        // if player is most likely coming from users Contact List, don't add him/her either?!
        List<String> contacts = getAllowedContacts(context);
        if ( ListUtil.isNotEmpty(contacts) && contacts.contains(sPlayer) ) {
            return false;
        }

        if ( m_playerList != null ) {
            m_playerList.add(sPlayer); // add it to the cached version as well so it won't get added twice accidentally
        }
        return addStringToList(context, PreferenceKeys.playerList, R.string.playerList_default, sPlayer);
    }

    public static List<String> getMatchList(Context context) {
        return getStringAsList(context, PreferenceKeys.matchList, 0);
    }

    public static String capitalizePlayerName(String sName) {
        if (StringUtil.isEmpty(sName)) return sName;
        if (sName.toLowerCase().equals(sName) == false) return sName;
        return StringUtil.capitalize(sName, true);
    }

    public static List<String> getStringAsList(Context context, PreferenceKeys key, int iDefault) {
        String values = getString(key, iDefault, context);
        if ( StringUtil.isEmpty(values) ) {
            return new ArrayList<String>();
        }
        if ( key.equals(PreferenceKeys.matchList) ) {
            int iResDefault = getSportTypeSpecificResId(context, R.string.matchList_default__Squash);
            String sMatchListDefault = context.getString(iResDefault);
            if ( isBrandTesting(context) ) {
                values = sMatchListDefault;
            }

            if ( bFixedMatchesAreUnChanged && values.equals(sMatchListDefault) == false ) {
                bFixedMatchesAreUnChanged = false;
            }
        }

        List<String> lValues = Arrays.asList(values.split("[\\n\\r\\|]"));
        lValues = ListUtil.removeDuplicates(lValues);
        lValues = ListUtil.translateValues(lValues, "^\\s*(.*?)\\s*$", "$1");
        ListUtil.removeEmpty(lValues);
        return lValues;
    }

    private static int addStringToTopOfList(Context context, PreferenceKeys key, String value) {
        if ( StringUtil.isEmpty(value) ) { return -1; }
        List<String> lValues = getStringAsList(context, key, 0);

        int iCurrentSize = ListUtil.size(lValues);

        ListUtil.removeEmpty(lValues);
        lValues = ListUtil.removeDuplicates(lValues);
        ListUtil.removeEmpty(lValues);
        int iIdxOld = lValues.indexOf(value);
        if ( iIdxOld != 0 ) {
            lValues.remove(value);
            lValues.add(0, value);
            try {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(key.toString(), ListUtil.join(lValues, "\n"));
                editor.apply();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return iIdxOld;
    }

    public static int addStringsToList(Context context, PreferenceKeys key, Collection<String> values) {
        if ( ListUtil.isEmpty(values) ) { return 0; }
        List<String> lValues = getStringAsList(context, key, 0);

        int iCurrentSize = ListUtil.size(values);

        lValues.addAll(values);
        ListUtil.removeEmpty(lValues);
        lValues = ListUtil.translateValues(lValues, "^\\s+", "");
        lValues = ListUtil.translateValues(lValues, "\\s+$", "");
        lValues = ListUtil.removeDuplicates(lValues);
        ListUtil.removeEmpty(lValues);
        Collections.sort(lValues);
        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(key.toString(), ListUtil.join(lValues, "\n"));
            editor.apply();
            return ListUtil.size(lValues) - iCurrentSize;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static boolean addStringToList(Context context, PreferenceKeys key, int iDefault, String sValue) {
        if (StringUtil.isEmpty(sValue)) { return false; }
        sValue = sValue.trim();
        List<String> lValues = getStringAsList(context, key, iDefault);
        if ( lValues.contains(sValue)) { return false; }

        lValues.add(sValue);
        lValues = ListUtil.translateValues(lValues, "^\\s+", "");
        lValues = ListUtil.translateValues(lValues, "\\s+$", "");
        lValues = ListUtil.removeDuplicates(lValues);
        ListUtil.removeEmpty(lValues);
        Collections.sort(lValues);
        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(key.toString(), ListUtil.join(lValues, "\n"));
            editor.apply();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean removeStringFromList(Context context, PreferenceKeys key, String sRegExp) {
        if (StringUtil.isEmpty(sRegExp)) { return false; }
        sRegExp = sRegExp.trim();
        List<String> lValues = getStringAsList(context, key, 0);
        lValues = ListUtil.filter(lValues, sRegExp, Enums.Match.Remove);

        lValues = ListUtil.translateValues(lValues, "^\\s+", "");
        lValues = ListUtil.translateValues(lValues, "\\s+$", "");
        lValues = ListUtil.removeDuplicates(lValues);
        ListUtil.removeEmpty(lValues);
        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(key.toString(), ListUtil.join(lValues, "\n"));
            editor.apply();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

	/*
    public static boolean ow_hideStartNewMatch(Context context) {
        return getBoolean(PreferenceKeys.hideStartNewMatch, context, false);
    }
    public static boolean ow_hideSelectFromMyMatches(Context context) {
        return getBoolean(PreferenceKeys.hideSelectFromMyMatches, context, false);
    }
    public static boolean ow_disableEditPlayerNames(Context context) {
        return getBoolean(PreferenceKeys.disableEditPlayerNames, context, false);
    }
    public static String ow_captionForSelectNewMatch(Context context) {
        return getString(PreferenceKeys.captionForSelectNewMatch, R.string.sb_select_feed_match, context);
    }
	*/
    public static String ow_captionForMessageMatchResult(Context context) {
        return getString(PreferenceKeys.captionForMessageMatchResult, R.string.sb_send_match_result, context);
    }
    public static String ow_captionForPostMatchResultToSite(Context context) {
        return getString(PreferenceKeys.captionForPostMatchResultToSite, R.string.sb_post_match_result, context);
    }
    public static String ow_iconForPostMatchResultToSite(Context context) {
        return getString(PreferenceKeys.iconForPostMatchResultToSite, null, context);
    }
    public static String ow_captionForEmailMatchResult(Context context) {
        return getString(PreferenceKeys.captionForEmailMatchResult, R.string.sb_email_match_result, context);
    }

    /*  invoked via getInt with the appropriate key
        public static int timerWarmup(Context context) {
            return getInteger(PreferenceKeys.timerWarmup, context, 300);
        }
        public static int timerPauseBetweenGames(Context context) {
            return getInteger(PreferenceKeys.timerPauseBetweenGames, context, 90);
        }
    */
    public static HandicapFormat getHandicapFormat(Context context) {
        return getEnum(PreferenceKeys.handicapFormat, context, HandicapFormat.class, R.string.handicapFormat_default);
    }
    public static int numberOfGamesToWinMatch(Context context) {
        int iMax = getIntegerR(PreferenceKeys.numberOfGamesToWinMatch, context, R.integer.numberOfGamesToWin_default__Squash);
        if ( iMax > 999 || (iMax < 1 /*&& iMax != -1*/ ) ) { // -1 means not used, like in racketlon
            iMax = 999;
            setNumber(PreferenceKeys.numberOfGamesToWinMatch, context, iMax);
        }
        return iMax;
    }
    public static int numberOfServesPerPlayer(Context context) {
        return getIntegerR(PreferenceKeys.numberOfServesPerPlayer, context, R.integer.numberOfServesPerPlayer_default);
    }
    public static int numberOfPointsToWinGame(Context context) {
        int iMax = getIntegerR(PreferenceKeys.numberOfPointsToWinGame, context, R.integer.gameEndScore_default__Squash);
        if ( iMax > 9999 ) {
            iMax = 9999;
            setNumber(PreferenceKeys.numberOfPointsToWinGame, context, iMax);
        } else if ( iMax < 1 ) {
            int iResIdDefault = getSportTypeSpecificResId(context, R.integer.gameEndScore_default__Squash);
            iMax = context.getResources().getInteger(iResIdDefault);
            setNumber(PreferenceKeys.numberOfPointsToWinGame, context, iMax);
        }
        return iMax;
    }
    public static boolean usePowerPlay(Context context) {
        return getBoolean(PreferenceKeys.usePowerPlay, context, R.bool.usePowerPlay_default);
    }
    public static int numberOfPowerPlaysPerPlayerPerMatch(Context context) {
        return getIntegerR(PreferenceKeys.numberOfPowerPlaysPerPlayerPerMatch, context, R.integer.numberOfPowerPlaysPerPlayerPerMatch_default__Squash);
    }
    public static int numberOfCharactersBeforeAutocomplete(Context context) {
        return getIntegerR(PreferenceKeys.numberOfCharactersBeforeAutocomplete, context, R.integer.numberOfCharactersBeforeAutocomplete_default);
    }
    public static int numberOfCharactersBeforeAutocompleteCountry(Context context) {
        return getIntegerR(PreferenceKeys.numberOfCharactersBeforeAutocompleteCountry, context, R.integer.numberOfCharactersBeforeAutocompleteCountry_default);
    }
    public static int getPauseDuration(Context context) {
        return getIntegerR(PreferenceKeys.timerPauseBetweenGames, context, R.integer.timerPauseBetweenGames_default__Squash);
    }
    public static int getWarmupDuration(Context context) {
        return getIntegerR(PreferenceKeys.timerWarmup, context, R.integer.timerWarmup_default__Squash);
    }

    private static boolean bFixedMatchesAreUnChanged = true;
    private static boolean bFeedsAreUnChanged        = true;
    public static String getMatchesFeedName(Context context) {
        return getFeedPostDetail(context, URLsKeys.Name);
    }
    public static boolean removeSeedingAfterMatchSelection(Context context) {
        return getBoolean(PreferenceKeys.removeSeedingAfterMatchSelection, context, R.bool.removeSeedingAfterMatchSelection_default);
    }
    public static String getFeedsFeedURL(Context context) {
        return getString(PreferenceKeys.FeedFeedsURL, R.string.feedFeedsURL_default, context);
    }

    public static boolean hasInternetConnection(Context context) {
        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if ( cm == null ) {
            return true; // typically only for 'preview' in GUI designer of PlayerButton
        }
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        return isConnected;
    }

    public static String getFlagURL(String sCountryCode, Context context) {
        String sFlagURL              = PreferenceValues.getFlagsURL(context);
        if ( StringUtil.isEmpty(sFlagURL) ) {
            return null;
        }
        if ( StringUtil.isEmpty(sCountryCode) ) {
            return null;
        }
        if ( StringUtil.length(sCountryCode) <= 1 ) {
            return null;
        }
        String sIso2                 = CountryUtil.getIso2(sCountryCode);
        String sURL                  = null;
        try {
            sURL = String.format(sFlagURL, sIso2, sCountryCode);
        } catch (java.util.UnknownFormatConversionException e) {
            Toast.makeText(context, "Unable to construct URL with " + sFlagURL, Toast.LENGTH_SHORT).show();
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return sURL;
    }
    public static File getFlagCacheName(String sCountryCode, Context ctx) {
        String sURL      = getFlagURL(sCountryCode, ctx);
        String sFileName = "flag." + sCountryCode + "." + sURL.replaceAll(".*[\\./]", "") + ".png";
        return new File(ctx.getCacheDir(), sFileName);
    }
    public static int downloadImage(Context context, Object imageViewOrPreference, String sCountryCode) {
        return downloadImage(context, imageViewOrPreference, sCountryCode, 1);
    }
    /** called e.g. by preloader */
    public static int downloadImage(Context context, Object imageViewOrPreference, String sCountryCode, int iMaxCacheAgeMultiplier) {
        String sURL                  = getFlagURL(sCountryCode, context);
        if ( StringUtil.isEmpty(sURL) ) {
            return 0;
        }

        File   fCache                = getFlagCacheName(sCountryCode, context);
        if (  (fCache.exists()                == false)
           && (hasInternetConnection(context) == false)) {
            // do not even attempt a download
            Log.d(TAG, "Not even attempting downloading for " + sCountryCode);
            return 0;
        }

        int    iFlagMaxCacheAgeInMin = PreferenceValues.getMaxCacheAgeFlags(context) * iMaxCacheAgeMultiplier;
        DownloadImageTask imageTask = null;
        if ( imageViewOrPreference != null ) {
            // display it after download
            imageTask = new DownloadImageTask(context.getResources(), imageViewOrPreference, sURL, sCountryCode, ImageView.ScaleType.FIT_XY, fCache, iFlagMaxCacheAgeInMin);
            imageTask.setUserAgentString(URLTask.getMyUserAgentString(context));
          //Log.d(TAG, "Starting image download task for " + imageViewOrPreference);
            imageTask.execute();
            return 1;
        } else {
            if ( FileUtil.exists_notEmpty_youngerThan(fCache, iFlagMaxCacheAgeInMin) ) {
                return 0;
            } else {
                // just download or refresh because it will most likely be used in the future
                imageTask = new DownloadImageTask(context.getResources(), sURL, fCache, iFlagMaxCacheAgeInMin);
                imageTask.setUserAgentString(URLTask.getMyUserAgentString(context));
                imageTask.execute();
                return 1;
            }
        }
    }
    public static int downloadAvatar(Context context, Object imageViewOrPreference, String sURL) {
        int    iFlagMaxCacheAgeInMin = PreferenceValues.getMaxCacheAgeFlags(context) * 1;
        String sCacheName            = "avatar." + sURL.replaceAll("[^\\w\\.\\-]", "_");
        File   fCache                = new File(context.getCacheDir(), sCacheName);
        DownloadImageTask imageTask = null;
        if ( imageViewOrPreference != null ) {
            // display it after download
            imageTask = new DownloadImageTask(context.getResources(), imageViewOrPreference, sURL, sURL, ImageView.ScaleType.FIT_START, fCache, iFlagMaxCacheAgeInMin);
            imageTask.setUserAgentString(URLTask.getMyUserAgentString(context));
            imageTask.execute();
            return 1;
        } else {
            if ( FileUtil.exists_notEmpty_youngerThan(fCache, iFlagMaxCacheAgeInMin) ) {
                return 0;
            } else {
                // just download or refresh because it will most likely be used in the future
                imageTask = new DownloadImageTask(context.getResources(), sURL, fCache, iFlagMaxCacheAgeInMin);
                imageTask.setUserAgentString(URLTask.getMyUserAgentString(context));
                imageTask.execute();
                return 1;
            }
        }
    }

    private static String getFlagsURL(Context context) {
        String sUrl = getFirstOfList(PreferenceKeys.FlagsURLs, R.string.FlagsURLs_default, context);
        sUrl = URLFeedTask.prefixWithBaseIfRequired(sUrl);
        return sUrl;
    }
    private static int getMaxCacheAgeFlags(Context context) {
        return getInteger(PreferenceKeys.maximumCacheAgeFlags, context, R.integer.maximumCacheAgeFlags_default);
    }
    public static int getMaxCacheAgeFeeds(Context context) {
        return getInteger(PreferenceKeys.maximumCacheAgeFeeds, context, R.integer.maximumCacheAgeFeeds_default);
    }
    public static int getFeedReadTimeout(Context context) {
        return getInteger(PreferenceKeys.feedReadTimeout, context, R.integer.feedReadTimeout_default); // in seconds
    }
    public static String getWebConfigURL(Context context) {
        String sUrl = "config.php";
        sUrl = URLFeedTask.prefixWithBaseIfRequired(sUrl);
        return sUrl;
    }
    public static String getMatchesFeedURL(Context context) {
        String sUrl = getFeedPostDetail(context, URLsKeys.FeedMatches);
               sUrl = URLFeedTask.prefixWithBaseIfRequired(sUrl);
               sUrl = URLFeedTask.addCountryCodeAsParameter(sUrl, context);
        return sUrl;
    }
    public static String getPlayersFeedURL(Context context) {
        String sUrl = getFeedPostDetail(context, URLsKeys.FeedPlayers);
               sUrl = URLFeedTask.prefixWithBaseIfRequired(sUrl);
               sUrl = URLFeedTask.addCountryCodeAsParameter(sUrl, context);
        return sUrl;
    }
    public static String getPostResultToURL(Context context) {
        return getFeedPostDetail(context, URLsKeys.PostResult);
    }
    public static String getPostLiveScoreToURL(Context context) {
        return getFeedPostDetail(context, URLsKeys.LiveScoreUrl);
    }
    private static PostDataPreference getPostDataPreference(Context context) {
        return getEnum(PreferenceKeys.postDataPreference, context, PostDataPreference.class, PostDataPreference.Basic);
    }

    public static boolean guessShareAction(String sModelSource, Context context) {
        String sPostURL = getPostResultToURL(context);
        if ( StringUtil.isEmpty(sPostURL) ) {
            // e.g. tournamentsoftware has no post URL
            boolean bChanged = setEnum(PreferenceKeys.shareAction, context, ShareMatchPrefs.LinkWithFullDetails);
            return bChanged;
        }
        final int iLengthToCheck = Math.max(20, sPostURL.indexOf('/', Math.min(10, sPostURL.length())));
        int iMinLength = Math.min(StringUtil.size(sModelSource), StringUtil.size(sPostURL));
        if ( iMinLength < iLengthToCheck ) {
            // prevent StringIndexOutOfBounds
            return false;
        }
        String sStartPostURL     = sPostURL.substring(0, iLengthToCheck);
        String sStartModelSource = sModelSource.substring(0, iLengthToCheck);
        if ( sStartPostURL.equalsIgnoreCase(sStartModelSource)) {
            // assume match is from a feed that contains a post URL
            boolean bChanged = setEnum(PreferenceKeys.shareAction, context, ShareMatchPrefs.PostResult);
            return bChanged;
        }
        return false;
    }

    /**
     * Actually persist some passed in values
     */
    public static void interpretOverwrites(Context context) {
        if ( mOverwrite == null ) { return; }
        if ( mOverwrite.containsKey(PreferenceKeys.feedPostUrls.toString()) ) {
            String sFeedUrlsEntryNew = mOverwrite.remove(PreferenceKeys.feedPostUrls.toString());
            List<Map<URLsKeys, String>> urlsNew = getUrlsList(sFeedUrlsEntryNew, context);
            for( Map<URLsKeys, String> newEntry: urlsNew ) {
                boolean bAllowReplace = true;
                boolean bMakeActive   = true;
                addOrReplaceNewFeedURL(context, newEntry, bAllowReplace, bMakeActive);
            }
        }
        if ( mOverwrite.containsKey(PreferenceKeys.matchList.toString()) ) {
            String sMatches = mOverwrite.remove(PreferenceKeys.matchList.toString());
            prependMatchesToList(context, Arrays.asList(StringUtil.singleCharacterSplit(sMatches)));
        }
        if ( mOverwrite.containsKey(PreferenceKeys.showLastGameInfoInTimer.toString()) ) {
            setBoolean(PreferenceKeys.showLastGameInfoInTimer, context, Boolean.valueOf(mOverwrite.remove(PreferenceKeys.showLastGameInfoInTimer.toString())));
            //setString(PreferenceKeys.showTimersAutomatically, context, mOverwrite.get(PreferenceKeys.showTimersAutomatically));
        }
/*
        if ( mOverwrite.containsKey(PreferenceKeys.showTimersAutomatically.toString()) ) {
            setBoolean(PreferenceKeys.showTimersAutomatically, context, Boolean.valueOf(mOverwrite.get(PreferenceKeys.showTimersAutomatically)));
            //setString(PreferenceKeys.showTimersAutomatically, context, mOverwrite.get(PreferenceKeys.showTimersAutomatically));
        }
        if ( mOverwrite.containsKey(PreferenceKeys.showOfficalAnnouncements.toString()) ) {
            setBoolean(PreferenceKeys.showOfficalAnnouncements, context, Boolean.valueOf(mOverwrite.get(PreferenceKeys.showOfficalAnnouncements)));
            //setString(PreferenceKeys.showOfficalAnnouncements, context, mOverwrite.get(PreferenceKeys.showOfficalAnnouncements));
        }
        if ( mOverwrite.containsKey(PreferenceKeys.showTieBreakDialog.toString()) ) {
            setBoolean(PreferenceKeys.showTieBreakDialog, context, Boolean.valueOf(mOverwrite.get(PreferenceKeys.showTieBreakDialog)));
            //setString(PreferenceKeys.showTieBreakDialog, context, mOverwrite.get(PreferenceKeys.showTieBreakDialog));
        }
*/
    }

    public static void addOrReplaceNewFeedURL(Context context, String sName, String sFeedMatches, String sFeedPlayers, String sPostURL, String sLiveScoreURL, boolean bAllowReplace, boolean bMakeActive) {
        Map<URLsKeys, String> newEntry = new HashMap<URLsKeys, String>();
        newEntry.put(URLsKeys.Name, sName);
        newEntry.put(URLsKeys.FeedMatches, sFeedMatches);
        if ( StringUtil.isNotEmpty(sFeedPlayers)) {
            newEntry.put(URLsKeys.FeedPlayers, sFeedPlayers);
        }
        if ( StringUtil.isNotEmpty(sPostURL)) {
            newEntry.put(URLsKeys.PostResult, sPostURL);
        }
        if ( StringUtil.isNotEmpty(sLiveScoreURL)) {
            newEntry.put(URLsKeys.LiveScoreUrl, sLiveScoreURL);
        }
        addOrReplaceNewFeedURL(context, newEntry, bAllowReplace, bMakeActive);
    }
    public static void addOrReplaceNewFeedURL(Context context, Map<URLsKeys, String> newEntry, boolean bAllowReplace, boolean bMakeActive) {
        String sCurrentURLs = getString(PreferenceKeys.feedPostUrls, "", context);
        List<Map<URLsKeys, String>> urlsList = getUrlsList(sCurrentURLs, context);
        if ( urlsList == null ) {
            urlsList = new ArrayList<>();
        }

        int iNewActiveFeedIndex = 0;
        String sNewName = newEntry.get(URLsKeys.Name);
        String sNewFeed = newEntry.get(URLsKeys.FeedMatches);
        boolean bAlreadyExists = false;
        for(int i = 0; i < urlsList.size(); i++) {
            Map<URLsKeys, String> existing = urlsList.get(i);
            String sName = existing.get(URLsKeys.Name);
            if ( sName == null ) { continue; }
            String sFeed = existing.get(URLsKeys.FeedMatches);
                   sFeed = URLFeedTask.prefixWithBaseIfRequired(sFeed);
            if ( sName.equals(sNewName) || ((sFeed!=null) && sFeed.equals(sNewFeed)) ) {
                bAlreadyExists = true;
                if ( bAllowReplace ) {
                    iNewActiveFeedIndex = i;
                    urlsList.set(iNewActiveFeedIndex, newEntry);
                    Log.d(TAG, "Replaced feedPostUrls entry " + i);
                } else {
                    Log.d(TAG, "NOT Replaced feedPostUrls entry " + i);
                }
            }
        }

        if ( bAlreadyExists == false ) {
            iNewActiveFeedIndex = 0;
            urlsList.add(iNewActiveFeedIndex, newEntry);
            Log.d(TAG, "Inserted as first feedPostUrls entry: " + newEntry);
        }

        // create new value to store in preferences
        _storeFeedURLsConfig(context, urlsList);
        if ( bMakeActive ) {
            //setString(PreferenceKeys.feedPostUrl, context, sNewName);
            setActiveFeedNr(context, iNewActiveFeedIndex);
        }
    }

    private static int _storeFeedURLsConfig(Context context, List<Map<URLsKeys, String>> urlsList) {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < urlsList.size(); i++) {
            Map<URLsKeys, String> existing = urlsList.get(i);
            sb.append(feedMap2String(existing)).append("\n");
        }
        setString(PreferenceKeys.feedPostUrls, context, sb.toString().trim());
        return sb.toString().length();
    }

    public static boolean deleteFeedURL(Context context, String sName2Delete) {
        boolean bDeleted = false;

        String sCurrentURLs = getString(PreferenceKeys.feedPostUrls, "", context);
        List<Map<URLsKeys, String>> urlsList = getUrlsList(sCurrentURLs, context);
        if ( urlsList == null ) {
            urlsList = new ArrayList<Map<URLsKeys, String>>();
        }

        // create new value to store in preferences
        int iActive = getInteger(PreferenceKeys.feedPostUrl, context, -1);
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < urlsList.size(); i++) {
            Map<URLsKeys, String> existing = urlsList.get(i);
            String sNameTmp = existing.get(URLsKeys.Name);
            if ( sNameTmp != null && sNameTmp.equals(sName2Delete)) {
                // do not add it to the new string
                if ( i < iActive ) {
                    iActive--;
                    setActiveFeedNr(context, iActive);
                }
                bDeleted = true;
                continue;
            }
            sb.append(feedMap2String(existing)).append("\n");
        }
        if ( bDeleted ) {
            setString(PreferenceKeys.feedPostUrls, context, sb.toString().trim());
            mActiveFeedValues = null;
        }
        return bDeleted;
    }

    public static boolean prependMatchesToList(Context context, List<String> newMatches) {
        List<String> lMatches = new ArrayList<String>(newMatches);
        List<String> lCurrent = getMatchList(context);
        if ( ListUtil.isNotEmpty(lCurrent) ) {
            lMatches.addAll(lCurrent);
        }
        boolean bSuccess = setString(PreferenceKeys.matchList, context, ListUtil.join(lMatches, "\n").trim());
        return bSuccess;
    }

    private static String feedMap2String(Map<URLsKeys, String> map) {
        StringBuilder sb = new StringBuilder();
        for(URLsKeys key: URLsKeys.values() ) {
            if ( map.containsKey(key) ) {
                sb.append(key).append("=").append(map.get(key)).append("\n");
            }
        }

        return sb.toString();
    }

    public static String getOfficialSquashRulesURL(Context context) {
        int iResDefault = getSportTypeSpecificResId(context, R.string.OfficialRulesURLs_default__Squash);
        return getFirstOfList(PreferenceKeys.OfficialSquashRulesURLs, iResDefault, context);
    }

    public static String getFeedPostName(Context context) {
        return getFeedPostDetail(context, URLsKeys.Name);
    }
/*
    public static String getFeedPostCountryRegion(Context context) {
        return getFeedPostDetail(context, URLsKeys.Region);
    }
*/
    public static Authentication getFeedPostAuthentication(Context context) {
        final String sAuth = getFeedPostDetail(context, URLsKeys.Authentication);
        if ( StringUtil.isEmpty(sAuth)) { return Authentication.None; }
        try {
            return Authentication.valueOf(sAuth);
        } catch (Exception e) {
            return Authentication.None;
        }
    }
    public static PostDataPreference getFeedPostDataPreference(Context context) {
        String sValue = getFeedPostDetail(context, URLsKeys.PostData);
        PostDataPreference eReturn = getPostDataPreference(context);
        if ( StringUtil.isNotEmpty(sValue) ) {
            try {
                eReturn = PostDataPreference.valueOf(sValue);
            } catch (Exception e) {
            }
        }
        return eReturn;
    }

    public static void setActiveFeedNr(Context context, int iNr) {
        mActiveFeedValues = null;
        PreferenceValues.setNumber(PreferenceKeys.feedPostUrl, context, iNr);
    }

    private static Map<URLsKeys, String> mActiveFeedValues = null;

    public static Map<URLsKeys, String> getFeedPostDetail(Context context) {
        if ( mActiveFeedValues != null && mActiveFeedValues.size() > 2 ) {
            return mActiveFeedValues;
        }

        Map<URLsKeys, String> entry = null;
        if ( context == null ) {
            Log.w(TAG, "Could not get preference for feed. No context");
            return null;
        }
/*
        String sURL = getOverwritten(pKey);
        if ( StringUtil.isNotEmpty(sURL) ) {
            return sURL;
        }
*/

        String sUrls = getString(PreferenceKeys.feedPostUrls, "", context);
        List<Map<URLsKeys, String>> urlsList = getUrlsList(sUrls, context);

        int iStringSize = StringUtil.size(sUrls);
        if ( iStringSize > 200 * ListUtil.size(urlsList) ) {
            // clean up a little
            int iNewStringSize = _storeFeedURLsConfig(context, urlsList);
            Log.d(TAG, "Refreshing config for feeds. From size " + iStringSize + " to " + iNewStringSize);
        }

        int iIndex = getInteger(PreferenceKeys.feedPostUrl, context, 0);
        if ( (iIndex >= 0) && ListUtil.size(urlsList) > iIndex ) {
            entry = urlsList.get(iIndex);
        }
        mActiveFeedValues = entry;
        return entry;
    }

    public static void addRecentFeedURLsForDemo(Context context) {
        final boolean bMakeActive = PreferenceValues.getMatchesFeedURLUnchanged();
        // TODO: trigger this code e.g. weekly somehow
        for ( int i=1; i<=3; i++ ) {
            if ( Brand.isSquash() ) {
                PreferenceValues.addOrReplaceNewFeedURL(context, "Demo PSA Matches " + i, "feed/psa.php?nr=" + i, null, null, null, false, bMakeActive);
            }
            if ( Brand.isRacketlon() ) {
                PreferenceValues.addOrReplaceNewFeedURL(context, "FIR Tournament " + i  , "feed/fir.tournamentsoftware.php?nr=" + i, "feed/fir.tournamentsoftware.php?pm=players&nr=" + i, null, null, false, bMakeActive);
            }
            // Squash, racketlon and Table Tennis. URL itself knows what sport based on subdomain
            PreferenceValues.addOrReplaceNewFeedURL(context, "TS " + Brand.getSport() + " " + i, "feed/tournamentsoftware.php?nr=" + i, "feed/tournamentsoftware.php?pm=players&nr=" + i, null, null, false, bMakeActive);
        }
    }

    private static String getFeedPostDetail(Context context, URLsKeys key/*, PreferenceKeys pKey*/) {
        Map<URLsKeys, String> entry = getFeedPostDetail(context);

        String sUrl = null;
        if ( MapUtil.isNotEmpty(entry) ) {
            sUrl = entry.get(key);
        }

        if ( bFeedsAreUnChanged && URLsKeys.FeedMatches.equals(key) ) {
            int iResDefault = getSportTypeSpecificResId(context, R.string.pref_feedPostUrls_default__Squash);
            if ( iResDefault != 0 ) {
                String sDefaultUrls = context.getString(iResDefault);
                List<Map<URLsKeys, String>> urlsListDefault = getUrlsList(sDefaultUrls, context);
                String sDefaultUrl1 = urlsListDefault.get(0).get(key);
                String sDefaultUrl2 = urlsListDefault.get(1).get(key);
                bFeedsAreUnChanged = (sUrl!=null) && ( (sDefaultUrl1!=null) && sDefaultUrl1.equals(sUrl)
                                                    || (sDefaultUrl2!=null) && sDefaultUrl2.equals(sUrl)
                                                     ) ;
                //LinkedHashMap<String, String> entries = getUrlsMap(sDefaultUrls);
                //bFeedsAreUnChanged = entries.values().iterator().next().equals(s);
            }
        }

        return sUrl;
    }

    public static Map<String,String> getFeedPostDetailMap(Context context, URLsKeys key, URLsKeys value, boolean bSortKey) {

        String sUrls = getString(PreferenceKeys.feedPostUrls, "", context);
        List<Map<URLsKeys, String>> urlsList = getUrlsList(sUrls, context);

        Class<? extends Map> c = LinkedHashMap.class;
        if ( bSortKey ) {
            c = TreeMap.class;
        }
        Map<String, String> map = MapUtil.listOfMaps2Map(urlsList, key.toString(), value.toString(), c);
        return map;
    }

    public static void removeOverwrites(Map<PreferenceKeys, String> values) {
        if ( values == null ) { return; }
        removeOverwrites(values.keySet());
    }
    public static void removeOverwrites(Collection<PreferenceKeys> values) {
        if ( values == null ) { return; }
        for(PreferenceKeys key: values ) {
            removeOverwrite(key);
        }
    }
/*
    private static boolean bHasFeedOverwrites = false;
    public static void setFeedOverwrites(Map<PreferenceKeys, String> values) {
        bHasFeedOverwrites = MapUtil.isNotEmpty(values);
        setOverwrites(values);
    }
*/
    public static void setOverwrites(Map<PreferenceKeys, String> values) {
        if ( values == null ) { return; }
        for(PreferenceKeys key: values.keySet() ) {
            setOverwrite(key, values.get(key));
        }
    }
    public static void setOverwrite(PreferenceKeys key, String sValue) {
        RWValues.setOverwrite(key, sValue);
        bFeedsAreUnChanged = false;
    }
    public static <T extends Enum<T>> void setOverwrite(PreferenceKeys key, T eValue) {
        RWValues.setOverwrite(key, String.valueOf(eValue));
        bFeedsAreUnChanged = false;
    }

    public static boolean getMatchesFeedURLUnchanged() {
        return bFeedsAreUnChanged;
    }

    public static boolean getFixedMatchesAreUnChanged() {
        return bFixedMatchesAreUnChanged;
    }

    public static String getDefaultSMSTo(Context context) {
        return getString(PreferenceKeys.smsResultToNr, "", context);
    }

    public static String getRefereeName(Context context) {
        return getString(PreferenceKeys.refereeName, "", context);
    }
    public static String getMarkerName(Context context) {
        return getString(PreferenceKeys.markerName, "", context);
    }
    public static String getAssessorName(Context context) {
        return getString(PreferenceKeys.assessorName, "", context);
    }

    public static String getDefaultMailTo(Context context) {
        return getString(PreferenceKeys.mailResultTo, "", context);
    }

    public static String getAdditionalPostKeyValuePairs(Context context) {
        return getString(PreferenceKeys.additionalPostKeyValuePairs, "", context); // not a preference for now, only used from 'parent' app
    }

    public static boolean mailFullScoringSheet(Context context) {
        return getBoolean(PreferenceKeys.mailFullScoringSheet, context, R.bool.mailFullScoringSheet_default);
    }

    /** returns configured directory. But if external and (no longer) writable, the internal storage dir is returned */
    public static File targetDirForImportExport(Context context, boolean bForImport) {
        requestPermission(context, PreferenceKeys.targetDirForImportExport, Manifest.permission.WRITE_EXTERNAL_STORAGE, true);

      //File storageDirectory = Environment.getExternalStorageDirectory();
        File storageDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        String sDir = getString(PreferenceKeys.targetDirForImportExport, (storageDirectory != null ? storageDirectory.getAbsolutePath() : null), context);
        if ( StringUtil.isEmpty(sDir) ) {
            return null;
        }
        File fDir = new File(sDir);
        // double check existence: maybe user removed sdcard from his device
        boolean bPreferredDirIsReadOnly = FileUtil.isWritable(fDir) == false;
        if ( bPreferredDirIsReadOnly && (storageDirectory != null) && (bForImport == false) ) {
            fDir = storageDirectory;

            // change the setting to read AND writable directory? May surprise the user
            //setString(PreferenceKeys.targetDirForImportExport, context, fDir.getAbsolutePath());
        }
        if ( bForImport && bPreferredDirIsReadOnly ) {
            Toast.makeText(context, String.format("Using %s read only directory, NOT suitable for export.", fDir.getAbsolutePath()), Toast.LENGTH_LONG).show();
        }
        return fDir;
    }

    private static final String NO_SHOWCASE_FOR_VERSION_BEFORE = "2025-02-19"; // auto adjusted by shell script 'clean.and.assemble.sh'
    public static boolean currentDateIsTestDate() {
        return DateUtil.getCurrentYYYY_MM_DD().compareTo(NO_SHOWCASE_FOR_VERSION_BEFORE) <= 0;
    }


    public static StartupAction getStartupAction(Context context) {
        //current version
        int versionCodeForChangeLogCheck = getVersionCodeForChangeLogCheck(context); // actually uses version name with e.g. dots removed
        //version where changelog has been viewed
        int viewedChangelogVersion = getInteger(PreferenceKeys.viewedChangelogVersion, context, 0);

        if ( viewedChangelogVersion < versionCodeForChangeLogCheck ) {
            setNumber(PreferenceKeys.viewedChangelogVersion, context, versionCodeForChangeLogCheck);
            if ( viewedChangelogVersion == 0 ) {
                // very first install/run

                if ( currentDateIsTestDate() ) {
                    // to allow adb monkey test it without the showcase/quickintro coming into the way
                    return StartupAction.None;
                }
                if ( ViewUtil.isWearable(context) ) {
                    return StartupAction.None;
                }

                return StartupAction.QuickIntro;

            }
            if ( (versionCodeForChangeLogCheck == 436) && Brand.isNotSquash() ) {
                // spanish introduced: set announcement language to spanish
                AnnouncementLanguage language = officialAnnouncementsLanguage(context);
                String deviceLanguage = RWValues.getDeviceLanguage(context);
                Log.d(TAG, "CURRENT AnnouncementLanguage: " + language +"CURRENT deviceLanguage: " + deviceLanguage);
                if ( announcementLanguageDeviates(context) ) {
                    if ( "es".equals(deviceLanguage) ) {
                        Log.d(TAG, "Changing announcement language");
                        setAnnouncementLanguage(AnnouncementLanguage.es, context);
                    }
                }
            }

            //setNumber(PreferenceKeys.viewedChangelogVersion, context, packageInfo.versionCode);
            return StartupAction.ChangeLog;
        }

        String s = getString(PreferenceKeys.StartupAction, StartupAction.None.toString(), context);
        StartupAction startupAction = null;
        try {
            startupAction = StartupAction.valueOf(s);
        } catch (IllegalArgumentException e) {
            startupAction = StartupAction.None;
        }
        return startupAction;
    }

    public static boolean hideCompletedMatchesFromFeed(Context context) {
        return getBoolean(PreferenceKeys.hideCompletedMatchesFromFeed, context, R.bool.hideCompletedMatchesFromFeed_default);
    }
    public static boolean autoSuggestToPostResult(Context context) {
        return getBoolean(PreferenceKeys.autoSuggestToPostResult, context, R.bool.suggestToPostResult_default);
    }
    public static boolean useFeedNameAsEventName(Context context) {
        return getBoolean(PreferenceKeys.useFeedNameAsEventName, context, R.bool.useFeedNameAsEventName_default);
    }
    public static boolean groupMatchesInFeedByCourt(Context context) {
        return getBoolean(PreferenceKeys.groupMatchesInFeedByCourt, context, R.bool.groupMatchesInFeedByCourt_default);
    }
    // TODO: in pref screen
    public static boolean useGroupNameAsEventData(Context context) {
        return getBoolean(PreferenceKeys.useGroupNameAsEventData, context, R.bool.useGroupNameAsEventData_default);
    }
    public static boolean savePasswords(Context context) {
        return getBoolean(PreferenceKeys.savePasswords, context, R.bool.savePasswords_default);
    }

    static void initTextColors(PreferenceGroup textColors, boolean bEnable) {
        if ( textColors == null ) { return; }
        textColors.setEnabled(bEnable);
    }

    public static List<Map<URLsKeys, String>> getUrlsList(String s, Context context) {
        if ( StringUtil.isEmpty(s) || "-".equals(s.trim())) { return null; }

        String[] sa = s.split("[\r\n]"); // remove the comma from the splitter regexp because feed values may contain a comma (e.g. 'Malaysia, Kuala Lumpur')

        final Map<String, Integer> mName2SizeOfStoredAlready = new HashMap<>();
        final String sLineMatch = "^(" + ListUtil.join(Arrays.asList(URLsKeys.values()), "|").toLowerCase() + ")\\s*=.*";
        List<Map<URLsKeys, String>> entries = new ArrayList<Map<URLsKeys, String>>();
        Map<URLsKeys, String> entry = null;
        for (int i = 0; i < sa.length; i++) {
            String sLine = sa[i].trim();
            if ( StringUtil.isEmpty(sLine) ) {
                // assume starting a new entry after an empty line
                addEntryToList(entry, entries, mName2SizeOfStoredAlready);
                entry = new HashMap<URLsKeys, String>();
                continue;
            }
            if ( entry == null ) {
                addEntryToList(entry, entries, mName2SizeOfStoredAlready);
                entry = new HashMap<URLsKeys, String>();
            }

            // (Name|FeedMatches|post)=.... like lines
            if ( sLine.toLowerCase().matches(sLineMatch) ) {
                int    iIndex = sLine.trim().indexOf("=");
                String sKey   = sLine.substring(0, iIndex).trim();
                String sValue = sLine.substring(iIndex+1).trim();
                URLsKeys eKey = null;
                try {
                    eKey = URLsKeys.valueOf(sKey);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }
                if ( eKey != null ) {
                    if (eKey.equals(URLsKeys.Name) && entry.containsKey(eKey)) {
                        // blank lines between entries deleted manually? Assume that a Name=... line represents start of a new entry
                        addEntryToList(entry, entries, mName2SizeOfStoredAlready);
                        entry = new HashMap<URLsKeys, String>();
                    }
                    if (eKey.equals(URLsKeys.FeedMatches) || eKey.equals(URLsKeys.FeedPlayers)) {
                        sValue = URLFeedTask.prefixWithBaseIfRequired(sValue);
                    }
                    entry.put(eKey, sValue);
                }
            } else {
                // assume name line is specified without key= prefix and feedmatches url is given as only value
                if ( sLine.startsWith("http") && MapUtil.isNotEmpty(entry) ) {
                    entry.put(URLsKeys.FeedMatches, sLine);
                } else {
                    entry = new HashMap<URLsKeys, String>();
                    entry.put(URLsKeys.Name, sLine);
                    //entries.add(entry);
                }
            }
        }
        addEntryToList(entry, entries, mName2SizeOfStoredAlready);

        if ( mName2SizeOfStoredAlready.containsKey(__REFUSED__) ) {
            _storeFeedURLsConfig(context, entries);
        }

        return entries;
    }

    private static final String __REFUSED__ = "__REFUSED__";

    private static boolean addEntryToList(Map<URLsKeys, String> entry, List<Map<URLsKeys, String>> entries, Map<String, Integer> mName2SizeOfStoredAlready) {
        if ( entry == null ) { return false; }
        if ( entry.containsKey(URLsKeys.Name) ) {
            entry.remove(URLsKeys.name); // remove optional lowercase name
        }
        if ( entry.size() < 1 ) {
            MapUtil.increaseCounter(mName2SizeOfStoredAlready, __REFUSED__);
            return false;
        }
        if ( entry.containsKey(URLsKeys.FeedPlayers) == false && entry.containsKey(URLsKeys.FeedMatches) == false ) {
            MapUtil.increaseCounter(mName2SizeOfStoredAlready, __REFUSED__);
            return false;
        }
        String sName = entry.get(URLsKeys.Name);
        if ( StringUtil.isNotEmpty(sName) ) {
            if ( mName2SizeOfStoredAlready.containsKey(sName) ) {
                MapUtil.increaseCounter(mName2SizeOfStoredAlready, __REFUSED__);
                return false;
            } else {
                mName2SizeOfStoredAlready.put(sName, MapUtil.size(entry));
            }
        }
        entries.add(entry);
        return true;
    }

    public static boolean showTip(Context context, PreferenceKeys preferenceKey, String sMessage, boolean bAsToast) {
        // do not show popup tips while we are in demo mode
        if ( ViewUtil.isWearable(context) ) { return false; }
        if ( ScoreBoard.isInDemoMode()    ) { return false; }
        if ( StringUtil.isEmpty(sMessage) ) { return false; } // should not happen but maybe a translation is missing/empty

        if ( getBoolean(PreferenceKeys.showTips, context, R.bool.showTips_default) == false ) {
            return false;
        }

        int iRunCount = getRunCount(context, preferenceKey);
        if ( iRunCount > 3 ) return false;

        if ( bAsToast ) {
            Toast.makeText(context, sMessage, Toast.LENGTH_LONG).show();
        } else {
            try {
                AlertDialog.Builder adb = getAlertDialogBuilder(context);

                final PreferenceCheckBox cbNoMore = new PreferenceCheckBox(context, PreferenceKeys.showTips, R.bool.showTips_default, true);
                cbNoMore.setText(R.string.pref_showTips_no_more);
                cbNoMore.setTextColor(Color.WHITE);

                adb.setTitle(context.getString(R.string.tip))
                   .setIcon(android.R.drawable.ic_dialog_info)
                   .setMessage(sMessage)
                   .setPositiveButton(R.string.cmd_ok, null)
                   .setView(cbNoMore)
                   .show();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    private static AlertDialog.Builder getAlertDialogBuilder(Context context) {
        return new MyDialogBuilder(context);
    }


    public static boolean isTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    //-------------------------------------------------------------
    // Contacts
    //-------------------------------------------------------------
    private static Map<String, Long> m_lAllGroups = new TreeMap<String, Long>();
    public static Map<String, Long> getContactGroups(Context context) {
        if (MapUtil.isNotEmpty(m_lAllGroups)) {
            return m_lAllGroups;
        }

        try {
            final String[] GROUP_PROJECTION = new String[] {
                      ContactsContract.Groups._ID
                    , ContactsContract.Groups.SYSTEM_ID
                    , ContactsContract.Groups.SUMMARY_COUNT
                    , ContactsContract.Groups.TITLE
                    , ContactsContract.Groups.DELETED
                    , ContactsContract.Groups.GROUP_VISIBLE
            };
            String selection = ContactsContract.Groups.DELETED + "!='1' AND " + ContactsContract.Groups.GROUP_VISIBLE + "!='0' ";
                   selection = ContactsContract.Groups.DELETED + "!='1'"; // somehow some of my own groups are 'not visible' and I have now idea why: I can see them in my phone contacts!
            Cursor cursor = context.getContentResolver().query(ContactsContract.Groups.CONTENT_SUMMARY_URI, GROUP_PROJECTION, selection, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int columnTitle = cursor.getColumnIndex(ContactsContract.Groups.TITLE);
                int summaryCountColumn    = cursor.getColumnIndex(ContactsContract.Groups.SUMMARY_COUNT);
                int columnId    = cursor.getColumnIndex(ContactsContract.Groups._ID);
                while (cursor.moveToNext()) {
                    //logCurrentRecord(cursor);

                    int iCnt = cursor.getInt(summaryCountColumn);
                    if ( iCnt == 0 ) { continue; }

                    String sDisplayName = cursor.getString(columnTitle) + " [#" + iCnt + "]";
                    Long   lId = cursor.getLong(columnId);

                    if (StringUtil.isNotEmpty(sDisplayName)) {
                        Long lOld = m_lAllGroups.put(sDisplayName, lId);
                        if ( lOld != null ) {
                            Log.w(TAG, "Group " + sDisplayName + " occurs multiple times"); // if seen this for CoWorkers group... (30 + 4 members)
                        }
                    }
                }
                cursor.close();
            }
        } catch (Throwable e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return m_lAllGroups;
    }

    private static void logCurrentRecord(Cursor cursor) {
        StringBuilder sb = new StringBuilder("\n");
        for(int i=0; i< cursor.getColumnCount(); i++) {
            sb.append(cursor.getColumnName(i) + ":" + cursor.getString(i)).append("\n");
        }
        Log.d(TAG, sb.toString());
    }

    private static List<String> m_lAllContactNames = new ArrayList<String>();
    private static List<String> getContacts(Context context, int iDisableOnFirstRunIfMoreThan, int iAlwaysStopAfterCnt ) {
        if ( ListUtil.isNotEmpty(m_lAllContactNames)) { return m_lAllContactNames; }

        long iStart = System.currentTimeMillis();

        boolean bOnlyWithPhoneNr = true; // TODO: preference (with phonenr : #161 in 327 ms, else 1076 in 288ms)
        try {
            int runCount = getRunCount(context, PreferenceKeys.readContactsForAutoCompletion);

            Cursor cursor = context.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, null, null, null, ContactsContract.Contacts.DISPLAY_NAME);
          //Log.d(TAG, String.format("Retrieving %s contacts by query took %s", cursor.getCount(), System.currentTimeMillis() - iStart));
            if ( (cursor != null) && (cursor.getCount() > iDisableOnFirstRunIfMoreThan) ) {
                //Log.w(TAG, String.format("To many contacts %s > %s. Skip ALL", cursor.getCount(), iDisableOnFirstRunIfMoreThan));
                if ( runCount <= 1 ) {
                    RWValues.setBoolean(PreferenceKeys.readContactsForAutoCompletion, context, false);
                }
            }
            if ( cursor != null && cursor.moveToFirst() ) {
                int iHasPhoneNumberIndex = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER);
                int iNameColumn          = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
                int iCnt = 0;
                do {
                    if ( (bOnlyWithPhoneNr ==false) || cursor.getInt(iHasPhoneNumberIndex) > 0) {
                        String sDisplayName = cursor.getString(iNameColumn);

                        if ( StringUtil.isNotEmpty(sDisplayName) ) {
                            m_lAllContactNames.add(sDisplayName);
                            if ( (m_lAllContactNames.size() >= iAlwaysStopAfterCnt) && (iAlwaysStopAfterCnt >= 0) ) {
                                Log.w(TAG, String.format("To many contacts %s. Stopping after %s", cursor.getCount(), iAlwaysStopAfterCnt));
                                break;
                            }
                        }

                    }
                    if ( (++iCnt) % 400 == 0 ) {
                        Log.d(TAG, String.format("Contacts processed %s ...", iCnt));
                    }
                } while (cursor.moveToNext());
                cursor.close();
                Log.d(TAG, String.format("Contacts processed %s !", iCnt));
            }

            //m_lAllContactNames = ListUtil.removeDuplicates(m_lAllContactNames);
        } catch (Throwable e) {
            Log.e(TAG, e.getMessage(), e);
        }
        long iFinish = System.currentTimeMillis();
        Log.w(TAG, "Reading all " + m_lAllContactNames.size() + " contacts took " + (iFinish - iStart) + " ms " + (bOnlyWithPhoneNr?"(Only with phonenr)":""));
        return m_lAllContactNames;
    }

    private static Map<Long, List<String>> m_membersPerGroup = new HashMap<Long, List<String>>();
    private static  List<String> getContactsFromGroup(Context context, long iGroupId) {
        List<String> lReturn = m_membersPerGroup.get(iGroupId);

        long iStart = System.currentTimeMillis();
        if ( ListUtil.isNotEmpty(lReturn)) {
            return lReturn;
        }

        lReturn = new ArrayList<String>();

        String[] cProjection = { ContactsContract.Contacts.DISPLAY_NAME
                               , ContactsContract.CommonDataKinds.GroupMembership.CONTACT_ID
                               , ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID
                               , ContactsContract.CommonDataKinds.GroupMembership.GROUP_SOURCE_ID
                               , ContactsContract.CommonDataKinds.GroupMembership._ID
                               };

        String selection = ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID      + "= ?" + " AND "
                         + ContactsContract.CommonDataKinds.GroupMembership.MIMETYPE          + "='"
                         + ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE + "'";
        String[] selectionArgs = {String.valueOf(iGroupId)};
        Cursor groupCursor = context.getContentResolver().query(
                ContactsContract.Data.CONTENT_URI,
                cProjection, selection,
                selectionArgs, null);
        if (groupCursor != null && groupCursor.moveToFirst())
        {
            do {
                int idColumnIndex = groupCursor.getColumnIndex(ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID);
                long aLong = groupCursor.getLong(idColumnIndex);

                int nameColumnIndex = groupCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                String name = groupCursor.getString(nameColumnIndex);

                long contactId = groupCursor.getLong(groupCursor.getColumnIndex(ContactsContract.CommonDataKinds.GroupMembership.CONTACT_ID));
                Log.d(TAG, "contact " + name + " " + aLong + " (" + contactId + ")");
                lReturn.add(name);
            } while (groupCursor.moveToNext());
            groupCursor.close();
        }
/*
        if ( ListUtil.isEmpty(lReturn) ) {
            lReturn = getAllNumbersFromGroupId(context, "Double Yellow", iGroupId);
        }
*/
        if ( ListUtil.isNotEmpty(lReturn) ) {
            m_membersPerGroup.put(iGroupId, lReturn);
        }

        long iFinish = System.currentTimeMillis();
        Log.w(TAG, "Reading all contacts from group " + iGroupId + " took " + (iFinish - iStart) + " ms");
        return lReturn;
    }

    //-------------------------------------------------------------
    // Permissions
    //-------------------------------------------------------------

    public static Permission doesUserHavePermissionToCast(Context context, String sCastDeviceName, boolean bRequestIfRequired) {
        Permission permission;
        if ( Build.VERSION.SDK_INT < Build.VERSION_CODES.P /* = 28 */ ) {
            permission = Permission.Granted;
        } else {
            permission = requestPermission(context, PreferenceKeys.useCastScreen, Manifest.permission.FOREGROUND_SERVICE, bRequestIfRequired);
        }

        if ( RWValues.Permission.Granted.equals(permission) == false ) {
            Toast.makeText(context, "Need foreground permission for casting to work on " + sCastDeviceName, Toast.LENGTH_LONG).show();
        }
        return permission;
    }
    static Permission doesUserHavePermissionToReadContacts(Context context, boolean bRequestIfRequired) {
        return requestPermission(context, PreferenceKeys.readContactsForAutoCompletion, Manifest.permission.READ_CONTACTS, bRequestIfRequired);
    }
    public static Permission doesUserHavePermissionToWriteExternalStorage(Context context, boolean bRequestIfRequired) {
        return requestPermission(context, PreferenceKeys.targetDirForImportExport, Manifest.permission.WRITE_EXTERNAL_STORAGE, bRequestIfRequired);
    }
    /** should automatically be granted according to documentation, but I have seen cases with SecurityException being thrown */
/*
    public static Permission doesUserHavePermissionToBluetooth(Context context, boolean bRequestIfRequired) {
        return requestPermission(context, PreferenceKeys.enableScoringByBluetoothConnection, Manifest.permission.BLUETOOTH, bRequestIfRequired);
    }
*/
    @RequiresApi(api = Build.VERSION_CODES.S)
    public static Permission doesUserHavePermissionToBluetoothConnect(Context context, boolean bRequestIfRequired) {
        return requestPermission(context, PreferenceKeys.enableScoringByBluetoothConnection, Manifest.permission.BLUETOOTH_CONNECT, bRequestIfRequired);
    }
    @RequiresApi(api = Build.VERSION_CODES.S)
    public static Permission doesUserHavePermissionToBluetoothScan(Context context, boolean bRequestIfRequired) {
        return requestPermission(context, PreferenceKeys.UseBluetoothLE, Manifest.permission.BLUETOOTH_SCAN, bRequestIfRequired);
    }
    @RequiresApi(api = Build.VERSION_CODES.S)
    public static Permission doesUserHavePermissionToBluetoothAdvertise(Context context, boolean bRequestIfRequired) {
        return requestPermission(context, PreferenceKeys.enableScoringByBluetoothConnection, Manifest.permission.BLUETOOTH_ADVERTISE, bRequestIfRequired);
    }
    @RequiresApi(api = Build.VERSION_CODES.S)
    public static Permission doesUserHavePermissionToAccessFineLocation(Context context, boolean bRequestIfRequired) {
        return requestPermission(context, PreferenceKeys.UseBluetoothLE, Manifest.permission.ACCESS_FINE_LOCATION, bRequestIfRequired);
    }
    public static <T extends Enum<T>> Permission getPermission(Context ctx, T key, String sPermission) {
        return requestPermission(ctx, key, sPermission, false);
    }

    public static Permission doesUserHavePermissionToBluetoothAdmin(Context context, boolean bRequestIfRequired) {
        return requestPermission(context, PreferenceKeys.enableScoringByBluetoothConnection, Manifest.permission.BLUETOOTH_ADMIN, bRequestIfRequired);
    }
    public static boolean initializeForScoringWithMediaControlButtons(Context context) {
        return getBoolean(PreferenceKeys.allowForScoringWithBlueToothConnectedMediaControlButtons, context, false);
    }

    public static String getCountryFromTelephonyOrTimeZone(Context context) {
        String networkCountryIso = null;
        try {
            TelephonyManager telephonyManager = context.getSystemService(TelephonyManager.class);
            if ( telephonyManager != null ) {
                networkCountryIso = telephonyManager.getNetworkCountryIso();
            }
            if ( StringUtil.isEmpty(networkCountryIso) ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    TimeZone aDefault = TimeZone.getDefault();
                    networkCountryIso = TimeZone.getRegion(aDefault.getID());
                }
            }
            Log.w(TAG, "networkCountryIso : " + networkCountryIso);
        } catch (Exception e) {
        }
        return networkCountryIso;
    }

    /** To present list of possible configs to user */
    public static List<CharSequence> getConfigs(Context context, int iResIdOfJson, int iWhat1KeyOnly2Description3Both) {
        iResIdOfJson = getSportSpecificSuffixedResId(context, iResIdOfJson);

        String sJson = ContentUtil.readRaw(context, iResIdOfJson);
        try {
            JSONObject config = new JSONObject(sJson);
            //String sBLEConfig = PreferenceValues.getString(PreferenceKeys.BluetoothLE_Config       , R.string.pref_BluetoothLE_Config_default__Default      , context);

            List<CharSequence> lReturn = new ArrayList<>();
            Iterator<String> keys = config.keys();
            while(keys.hasNext()) {
                String sKey = keys.next();
                if ( sKey.startsWith("-") ) { continue; }
                if ( sKey.startsWith(sKey.substring(0,3).toUpperCase())) {
                    // for now only list entries with first few characters uppercase
                    switch (iWhat1KeyOnly2Description3Both) {
                        case 1:
                            lReturn.add(sKey);
                            break;
                        case 2:
                        case 3:
                            JSONObject joDetails = config.getJSONObject(sKey);
                            String sShortDescription = joDetails.getString(PreferenceKeys.ShortDescription.toString());
                            if ( iWhat1KeyOnly2Description3Both == 2) {
                                lReturn.add(sShortDescription);
                            } else {
                                lReturn.add(sKey + " : " + sShortDescription);
                            }
                            break;
                    }
                }
            }
            return lReturn;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static JSONObject getActiveConfig(Context context, int iResIdJson, Object oKeyActive, int iResIdActiveDefault, String sKeySharedConfig) {
        String sJson = ContentUtil.readRaw(context, iResIdJson);
        try {
            final JSONObject config = new JSONObject(sJson);
            String sBLEConfig = PreferenceValues.getString(oKeyActive /*PreferenceKeys.BluetoothLE_Config*/, R.string.pref_BluetoothLE_Config_default__Default, context);
            JSONObject configActive = config.optJSONObject(sBLEConfig);
            if ( configActive == null ) {
                sBLEConfig = context.getString(iResIdActiveDefault /*R.string.pref_BluetoothLE_Config_default__Default*/);
                configActive = config.optJSONObject(sBLEConfig);
            }
            if ( (configActive != null) && configActive.has(sKeySharedConfig /*BLEUtil.Keys.SharedConfig.toString()*/ ) ) {
                String sShareConfig = (String) configActive.remove(sKeySharedConfig);
                JSONObject sharedConfig = config.getJSONObject(sShareConfig);
                Iterator<String> keys = sharedConfig.keys();
                while(keys.hasNext()) {
                    String sKey = keys.next();
                    if ( configActive.has(sKey) ) { continue; }
                    Object oValue = sharedConfig.get(sKey);
                    configActive.put(sKey, oValue);
                }
            }
            return configActive;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
