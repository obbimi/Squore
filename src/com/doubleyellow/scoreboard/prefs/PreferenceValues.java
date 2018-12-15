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
import android.os.Environment;
import android.preference.*;
import android.provider.ContactsContract;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.widget.ImageView;
import android.widget.Toast;
import com.doubleyellow.android.task.DownloadImageTask;
import com.doubleyellow.android.task.URLTask;
import com.doubleyellow.android.view.ViewUtil;
import com.doubleyellow.prefs.OrientationPreference;
import com.doubleyellow.prefs.RWValues;
import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.URLFeedTask;
import com.doubleyellow.scoreboard.archive.GroupMatchesBy;
import com.doubleyellow.scoreboard.feed.Authentication;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.model.*;
import com.doubleyellow.scoreboard.model.Util;
import com.doubleyellow.scoreboard.timer.ViewType;
import com.doubleyellow.scoreboard.view.PreferenceCheckBox;
import com.doubleyellow.util.*;

import java.io.File;
import java.util.*;

/**
 * Class that programmatically provides Read/Write access to stored preferences.
 */
public class PreferenceValues extends RWValues
{
    //public static final String removeSeedingRegExp = "[\\[\\]0-9/]+$";
    private static boolean m_restartRequired = false;
    public static void setRestartRequired(Context ctx) {
        if ( m_restartRequired == false ) {
            Toast.makeText(ctx, R.string.for_this_to_take_effect_a_restart_is_required, Toast.LENGTH_SHORT).show();
        }
        m_restartRequired = true;
    }
    public static boolean isRestartRequired() {
        boolean bReturn = false && m_restartRequired;
        m_restartRequired = false;
        return bReturn;
    }

    private static final String TAG = "SB." + PreferenceValues.class.getSimpleName();

    private PreferenceValues() {}

    public static final String COM_DOUBLEYELLOW_SCOREBOARD = "com.doubleyellow.scoreboard";

    public static boolean isUnbrandedExecutable(Context ctx) {
        return ctx.getPackageName().equals(PreferenceValues.COM_DOUBLEYELLOW_SCOREBOARD);
    }
    public static boolean isBrandedExecutable(Context ctx) {
        return isUnbrandedExecutable(ctx) == false;
    }

    public static boolean isBrandTesting(Context ctx) {
        if ( isUnbrandedExecutable(ctx) && (Brand.values().length>1) ) {
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
        return getEnumSet(PreferenceKeys.showMatchDurationChronoOn, context, ShowOnScreen.class, EnumSet.allOf(ShowOnScreen.class));
    }
    public static EnumSet<ShowOnScreen> showLastGameDurationChronoOn(Context context) {
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
        int iResDefault = getSportTypeSpecificResId(context, R.integer.serveButtonTransparencyNonServer_default_Squash);

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
    public static NewMatchLayout getNewMatchLayout(Context context) {
        return getEnum(PreferenceKeys.newMatchLayout, context, NewMatchLayout.class, NewMatchLayout.AllFields);
    }
    public static GameScoresAppearance getGameScoresAppearance(Context context) {
        if ( Brand.isRacketlon() ) {
            return GameScoresAppearance.ShowFullScore;
        }
        return getEnum(PreferenceKeys.gameScoresAppearance, context, GameScoresAppearance.class, GameScoresAppearance.ShowGamesWon);
    }
    public static DoublesServeSequence getDoublesServeSequence(Context context) {
        return getEnum(PreferenceKeys.doublesServeSequence, context, DoublesServeSequence.class, DoublesServeSequence.values()[0], Model.mOldDSS2New);
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
        return getBoolean(PreferenceKeys.showActionBar, context, R.bool.showActionBar_default);
    }

    public static boolean showFullScreen(Context context) {
        return getBoolean(PreferenceKeys.showFullScreen, context, R.bool.showFullScreen_default);
    }

    public static boolean showTextInActionBar(Context context) {
        return getBoolean(PreferenceKeys.showTextInActionBar, context, R.bool.showTextInActionBar_default);
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
    public static Feature useTossFeature(Context context) {
        return getEnum(PreferenceKeys.useTossFeature, context, Feature.class, Feature.Suggest);
    }
    public static Feature removeMatchFromMyListWhenSelected(Context context) {
        return getEnum(PreferenceKeys.removeMatchFromMyListWhenSelected, context, Feature.class, Feature.DoNotUse);
    }
    public static Feature useShareFeature(Context context) {
        return getEnum(PreferenceKeys.useShareFeature, context, Feature.class, Feature.Suggest);
    }
    public static ShareMatchPrefs getShareAction(Context context) {
        return getEnum(PreferenceKeys.shareAction, context, ShareMatchPrefs.class, ShareMatchPrefs.LinkWithFullDetails);
    }
    public static Feature useOfficialAnnouncementsFeature(Context context) {
        return getEnum(PreferenceKeys.useOfficialAnnouncementsFeature, context, Feature.class, Feature.Suggest);
    }
    public static void initForLiveScoring(Context ctx, boolean bTmp) {
        if ( bTmp ) {
            setOverwrite(PreferenceKeys.shareAction    , LiveScorePrefs.theOneForLiveScoring);
            setOverwrite(PreferenceKeys.useShareFeature, Feature.Automatic);
        } else {
            removeOverwrite(PreferenceKeys.shareAction);
            removeOverwrite(PreferenceKeys.useShareFeature);
            setEnum(PreferenceKeys.shareAction    , ctx, LiveScorePrefs.theOneForLiveScoring);
            setEnum(PreferenceKeys.useShareFeature, ctx, Feature.Automatic);
        }
    }
    public static void initForNoLiveScoring(Context ctx) {
        removeOverwrite(PreferenceKeys.shareAction);
        removeOverwrite(PreferenceKeys.useShareFeature);
        setEnum(PreferenceKeys.shareAction    , ctx, ShareMatchPrefs.LinkWithFullDetails);
        setEnum(PreferenceKeys.useShareFeature, ctx, Feature.Suggest);
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

    // TODO: add to preferences.xml
    public static Feature switchToPlayerListIfMatchListOfFeedIsEmpty(Context context) {
        return getEnum(PreferenceKeys.switchToPlayerListIfMatchListOfFeedIsEmpty, context, Feature.class, Feature.Suggest);
    }
    public static AnnouncementLanguage officialAnnouncementsLanguage(Context context) {
        return getEnum(PreferenceKeys.officialAnnouncementsLanguage, context, AnnouncementLanguage.class, R.string.officialAnnouncementsLanguage_default);
    }
    public static void setAnnouncementLanguage(AnnouncementLanguage languageNew, Context context) {
        AnnouncementLanguage languageCur = officialAnnouncementsLanguage(context);
        if (languageCur.equals(languageNew) == false ) {
            PreferenceValues.setEnum(PreferenceKeys.officialAnnouncementsLanguage, context, languageNew);
            PreferenceValues.clearOACache();
        }
    }
    public static boolean announcementLanguageDeviates(Context ctx) {
        AnnouncementLanguage language = officialAnnouncementsLanguage(ctx);
        return language.toString().equals(RWValues.getDeviceLanguage(ctx)) == false;
    }

    public static int getSportTypeSpecificResId(Context context, int iResid) {
        if ( Brand.isNotSquash() ) {
            String sResName = context.getResources().getResourceName(iResid);
            String suffix   = "_" + SportType.Squash;
            if ( sResName.endsWith(suffix) ) {
                String sNewResName = sResName.replaceAll(suffix, "_" + Brand.getSport());
                int iNewResId = context.getResources().getIdentifier(sNewResName, "string", context.getPackageName());
                if ( iNewResId == 0 ) {
                    Log.w(TAG, "======================= No specific " + Brand.getSport() + " resource for " + sResName);
                }
                return iNewResId; // still return 0 if no specific resource was found: showcase e.g. decides to skip a step if 0 is returned
            }
        }
        return iResid;
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
            R.string.oa_best_of_x_games_to_y,
            R.string.oa_best_of_x_games,
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
            R.string.oa_n_all,
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
            AnnouncementLanguage language = officialAnnouncementsLanguage(ctx);

            Resources     res    = ctx.getResources();
            Configuration resCfg = res.getConfiguration();
            Locale localeRestore = null;
            if ( resCfg.locale.getLanguage().equals(language.toString()) == false ) {
                localeRestore = resCfg.locale;
                Locale locale = new Locale(language.toString());
                res = newResources(res, locale);
            }

            mOACache = new SparseArray<String>();
            for(int iRes: iaOAResString) {
                mOACache.put(iRes, ctx.getResources().getString(iRes));
            }
            if ( announcementLanguageDeviates(ctx) ) {
                mOACache.put(R.string.left_serveside_single_char , sLeftRight_Symbols[0]);
                mOACache.put(R.string.right_serveside_single_char, sLeftRight_Symbols[1]);
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
        return getEnum(PreferenceKeys.useTimersFeature, context, Feature.class, Feature.Suggest);
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
        return getEnum(PreferenceKeys.scorelineLayout, context, ScorelineLayout.class, R.string.scorelineLayout_default);
    }
    // TODO: add to preferences.xml
    public static boolean useFeedAndPostFunctionality(Context context) {
        return getBoolean(PreferenceKeys.useFeedAndPostFunctionality, context, R.bool.useFeedAndPostFunctionality_default);
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
        return getEnumSet(PreferenceKeys.showPlayerColorOn, context, ShowPlayerColorOn.class, EnumSet.allOf(ShowPlayerColorOn.class));
    }
    public static EnumSet<ShowCountryAs> showCountryAs(Context context) {
        return getEnumSet(PreferenceKeys.showCountryAs, context, ShowCountryAs.class, EnumSet.of(ShowCountryAs.FlagNextToNameOnDevice, ShowCountryAs.FlagNextToNameChromeCast));
    }
    public static EnumSet<ShowAvatarOn> showAvatarOn(Context context) {
        return getEnumSet(PreferenceKeys.showAvatarOn, context, ShowAvatarOn.class, EnumSet.of(ShowAvatarOn.OnDevice, ShowAvatarOn.OnChromeCast));
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
        int iResDefault = getSportTypeSpecificResId(context, R.string.NewMatchesType_default_Squash);
        return getEnum(PreferenceKeys.NewMatchesType, context, NewMatchesType.class, iResDefault);
    }
    public static int maxNumberOfPlayersInGroup(Context context) {
        return getIntegerR(PreferenceKeys.maxNumberOfPlayersInGroup, context, R.integer.maxNumberOfPlayersInGroup_default);
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
        return getEnum(PreferenceKeys.endGameSuggestion, context, Feature.class, Feature.Suggest);
    }
    /** for tabletennis and racketlon, not squash */
    public static boolean swapPlayersOn180DegreesRotationOfDeviceInLandscape(Context context) {
        int iResDefault = getSportTypeSpecificResId(context, R.bool.swapPlayersOn180DegreesRotationOfDeviceInLandscape_default_Squash);
        return getBoolean(PreferenceKeys.swapPlayersOn180DegreesRotationOfDeviceInLandscape, context, iResDefault);
    }
    /** for tabletennis, not squash or racketlon */
    public static boolean swapPlayersBetweenGames(Context context) {
        return getBoolean(PreferenceKeys.swapPlayersBetweenGames, context, R.bool.swapPlayersBetweenGames_default);
    }
    /** for tabletennis, not squash or racketlon */
    public static Feature showGamePausedDialog(Context context) {
        return getEnum(PreferenceKeys.showGamePausedDialog, context, Feature.class, Feature.Suggest);
    }
    public static int autoShowGamePausedDialogAfterXPoints(Context context) {
        int iResDefault = getSportTypeSpecificResId(context, R.integer.autoShowGamePausedDialogAfterXPoints_default);
        return getIntegerR(PreferenceKeys.autoShowGamePausedDialogAfterXPoints, context, iResDefault);
    }
    public static boolean autoShowModeActivationDialog(Context context) {
        return getBoolean(PreferenceKeys.autoShowModeActivationDialog, context, false);
    }
    public static int showModeDialogAfterXMins(Context context) {
        int iResDefault = getSportTypeSpecificResId(context, R.integer.showModeDialogAfterXMins_default_Squash);
        return getIntegerR(PreferenceKeys.showModeDialogAfterXMins, context, iResDefault);
    }

    /** not for squash, for racketlon (all but squash, except for doubles), for tabletennis in last game */
    public static Feature swapPlayersHalfwayGame(Context context) {
        return getEnum(PreferenceKeys.swapPlayersHalfwayGame, context, Feature.class, Feature.Suggest);
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
        return getEnumSet(PreferenceKeys.floatingMessageForGameBallOn, context, ShowOnScreen.class, EnumSet.of(ShowOnScreen.OnDevice, ShowOnScreen.OnChromeCast));
    }
    public static boolean floatingMessageForGameBall(Context context, boolean bIsPresentation) {
        if ( bIsPresentation ) {
            return floatingMessageForGameBallOn(context).contains(ShowOnScreen.OnChromeCast);
        } else {
            return floatingMessageForGameBallOn(context).contains(ShowOnScreen.OnDevice);
        }
    }
    public static boolean Cast_ShowGraphDuringTimer(Context context) {
        return getBoolean(PreferenceKeys.Cast_ShowGraphDuringTimer, context, R.bool.Cast_ShowGraphDuringTimer_default);
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
            Log.d(TAG, "Not reading contacts");
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
            int iResDefault = getSportTypeSpecificResId(context, R.string.matchList_default_Squash);
            String sMatchListDefault = context.getString(iResDefault);
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
        return getIntegerR(PreferenceKeys.numberOfGamesToWinMatch, context, R.integer.numberOfGamesToWin_default);
    }
    public static int numberOfServesPerPlayer(Context context) {
        return getIntegerR(PreferenceKeys.numberOfServesPerPlayer, context, R.integer.numberOfServesPerPlayer_default);
    }
    public static int numberOfPointsToWinGame(Context context) {
        return getIntegerR(PreferenceKeys.numberOfPointsToWinGame, context, R.integer.gameEndScore_default);
    }
    public static int numberOfCharactersBeforeAutocomplete(Context context) {
        return getIntegerR(PreferenceKeys.numberOfCharactersBeforeAutocomplete, context, R.integer.numberOfCharactersBeforeAutocomplete_default);
    }
    public static int numberOfCharactersBeforeAutocompleteCountry(Context context) {
        return getIntegerR(PreferenceKeys.numberOfCharactersBeforeAutocompleteCountry, context, R.integer.numberOfCharactersBeforeAutocompleteCountry_default);
    }
    public static int getPauseDuration(Context context) {
        return getIntegerR(PreferenceKeys.timerPauseBetweenGames, context, R.integer.timerPauseBetweenGames_default);
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
    public static int downloadImage(Context context, Object imageViewOrPreference, String sCountryCode) {
        return downloadImage(context, imageViewOrPreference, sCountryCode, 1);
    }
    public static int downloadImage(Context context, Object imageViewOrPreference, String sCountryCode, int iMaxCacheAgeMultiplier) {
        String sFlagURL              = PreferenceValues.getFlagsURL(context);
        if ( StringUtil.isEmpty(sFlagURL) ) {
            return 0;
        }
        if ( StringUtil.isEmpty(sCountryCode) ) {
            return 0;
        }
        int    iFlagMaxCacheAgeInMin = PreferenceValues.getMaxCacheAgeFlags(context) * iMaxCacheAgeMultiplier;
        String sIso2                 = CountryUtil.getIso2(sCountryCode);
        String sURL                  = null;
        try {
            sURL = String.format(sFlagURL, sIso2, sCountryCode);
        } catch (java.util.UnknownFormatConversionException e) {
            Toast.makeText(context, "Unable to construct URL with " + sFlagURL, Toast.LENGTH_SHORT).show();
            return 0;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
        String sCacheName            = "flag." + sCountryCode + "." + sURL.replaceAll(".*[\\./]", "") + ".png";
        File   fCache                = new File(context.getCacheDir(), sCacheName);
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

    public static String getFlagsURL(Context context) {
        String sUrl = getFirstOfList(PreferenceKeys.FlagsURLs, R.string.FlagsURLs_default, context);
        sUrl = URLFeedTask.prefixWithBaseIfRequired(sUrl);
        return sUrl;
    }
    public static int getMaxCacheAgeFlags(Context context) {
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
        return sUrl;
    }
    public static String getPlayersFeedURL(Context context) {
        String sUrl = getFeedPostDetail(context, URLsKeys.FeedPlayers);
        sUrl = URLFeedTask.prefixWithBaseIfRequired(sUrl);
        return sUrl;
    }
    public static String getPostResultToURL(Context context) {
        return getFeedPostDetail(context, URLsKeys.PostResult);
    }
    private static PostDataPreference getPostDataPreference(Context context) {
        return getEnum(PreferenceKeys.postDataPreference, context, PostDataPreference.class, PostDataPreference.Basic);
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

    public static void addOrReplaceNewFeedURL(Context context, String sName, String sFeedMatches, String sFeedPlayers, String sPostURL, boolean bAllowReplace, boolean bMakeActive) {
        Map<URLsKeys, String> newEntry = new HashMap<URLsKeys, String>();
        newEntry.put(URLsKeys.Name, sName);
        newEntry.put(URLsKeys.FeedMatches, sFeedMatches);
        if ( StringUtil.isNotEmpty(sFeedPlayers)) {
            newEntry.put(URLsKeys.FeedPlayers, sFeedPlayers);
        }
        if ( StringUtil.isNotEmpty(sPostURL)) {
            newEntry.put(URLsKeys.PostResult, sPostURL);
        }
        addOrReplaceNewFeedURL(context, newEntry, bAllowReplace, bMakeActive);
    }
    public static void addOrReplaceNewFeedURL(Context context, Map<URLsKeys, String> newEntry, boolean bAllowReplace, boolean bMakeActive) {
        String sCurrentURLs = getString(PreferenceKeys.feedPostUrls, "", context);
        List<Map<URLsKeys, String>> urlsList = getUrlsList(sCurrentURLs, context);
        if ( urlsList == null ) {
            urlsList = new ArrayList<Map<URLsKeys, String>>();
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
        int iResDefault = getSportTypeSpecificResId(context, R.string.OfficialRulesURLs_default_Squash);
        return getFirstOfList(PreferenceKeys.OfficialSquashRulesURLs, iResDefault, context);
    }

    public static String getFeedPostName(Context context) {
        return getFeedPostDetail(context, URLsKeys.Name);
    }
    public static String getFeedPostCountryRegion(Context context) {
        return getFeedPostDetail(context, URLsKeys.Region);
    }
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
        for(int i=1; i<=3; i++ ) {
            if ( Brand.isSquash() ) {
                PreferenceValues.addOrReplaceNewFeedURL(context, "Demo PSA Matches " + i, "feed/psa.php?nr=" + i, null, null, false, bMakeActive);
            }
            if ( Brand.isRacketlon() ) {
                PreferenceValues.addOrReplaceNewFeedURL(context, "FIR Tournament " + i  , "feed/fir.tournamentsoftware.php?nr=" + i, "feed/fir.tournamentsoftware.php?pm=players&nr=" + i, null, false, bMakeActive);
            }
            // Squash, racketlon and Table Tennis. URL itself knows what sport based on subdomain
            PreferenceValues.addOrReplaceNewFeedURL(context, "TS " + Brand.getSport() + " " + i, "feed/tournamentsoftware.php?nr=" + i, "feed/tournamentsoftware.php?pm=players&nr=" + i, null, false, bMakeActive);
        }
    }

    private static String getFeedPostDetail(Context context, URLsKeys key/*, PreferenceKeys pKey*/) {
        Map<URLsKeys, String> entry = getFeedPostDetail(context);

        String sUrl = null;
        if ( MapUtil.isNotEmpty(entry) ) {
            sUrl = entry.get(key);
        }

        if ( bFeedsAreUnChanged && URLsKeys.FeedMatches.equals(key) ) {
            int iResDefault = getSportTypeSpecificResId(context, R.string.pref_feedPostUrls_default_Squash);
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

    public static String getDefaultMailTo(Context context) {
        return getString(PreferenceKeys.mailResultTo, "", context);
    }

    public static String getAdditionalPostKeyValuePairs(Context context) {
        return getString(PreferenceKeys.additionalPostKeyValuePairs, "", context); // not a preference for now, only used from 'parent' app
    }

    public static boolean mailFullScoringSheet(Context context) {
        return getBoolean(PreferenceKeys.mailFullScoringSheet, context, R.bool.mailFullScoringSheet_default);
    }

    /** returns configured directory. But if external and (no longer) writabe, the internal storage dir is returned */
    public static File targetDirForImportExport(Context context, boolean bForImport) {
        requestPermission(context, PreferenceKeys.targetDirForImportExport, Manifest.permission.WRITE_EXTERNAL_STORAGE, true);

        File storageDirectory = Environment.getExternalStorageDirectory();
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
            Toast.makeText(context, "Returning read only directory for import that can NOT be used for export.", Toast.LENGTH_LONG).show();
        }
        return fDir;
    }

    public static StartupAction getStartupAction(Context context) {
        //current version
        int versionCodeForChangeLogCheck = getVersionCodeForChangeLogCheck(context);
        //version where changelog has been viewed
        int viewedChangelogVersion = getInteger(PreferenceKeys.viewedChangelogVersion, context, 0);

        if ( viewedChangelogVersion < versionCodeForChangeLogCheck ) {
            setNumber(PreferenceKeys.viewedChangelogVersion, context, versionCodeForChangeLogCheck);
            if ( viewedChangelogVersion == 0 ) {
                // very first install/run

                int appVersionCode = RWValues.getAppVersionCode(context);
                final int    NO_SHOWCASE_FOR_VERSION        = 206;
                final String NO_SHOWCASE_FOR_VERSION_BEFORE = "2018-12-18";
                if ( appVersionCode > NO_SHOWCASE_FOR_VERSION ) {
                    // need to adjust the datecheck below
                    Log.w(TAG, "[getStartupAction] Adjust version code check!!");
                    //throw new RuntimeException("[getStartupAction] Adjust version code check!!");
                }
                if ( appVersionCode == NO_SHOWCASE_FOR_VERSION && DateUtil.getCurrentYYYY_MM_DD().compareTo(NO_SHOWCASE_FOR_VERSION_BEFORE) < 0 ) {
                    // to allow google play store to run the app and monkey test it without the showcase/quickintro coming into the way
                    return StartupAction.None;
                }
                return StartupAction.QuickIntro;

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
        if ( ScoreBoard.isInDemoMode() ) { return false; }
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
        return ScoreBoard.getAlertDialogBuilder(context);
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

    static Permission doesUserHavePermissionToReadContacts(Context context, boolean bRequestIfRequired) {
        return requestPermission(context, PreferenceKeys.readContactsForAutoCompletion, Manifest.permission.READ_CONTACTS, bRequestIfRequired);
    }
    public static Permission doesUserHavePermissionToWriteExternalStorage(Context context, boolean bRequestIfRequired) {
        return requestPermission(context, PreferenceKeys.targetDirForImportExport, Manifest.permission.WRITE_EXTERNAL_STORAGE, bRequestIfRequired);
    }

}
