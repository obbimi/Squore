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

package com.doubleyellow.scoreboard;

import com.doubleyellow.scoreboard.R; // don't remove this line, required to compile different 'brands'

import android.content.Context;

import com.doubleyellow.android.view.ViewUtil;
import com.doubleyellow.prefs.RWValues;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.model.ModelFactory;
import com.doubleyellow.scoreboard.model.SportType;
import com.doubleyellow.scoreboard.model.TieBreakFormat;
import com.doubleyellow.scoreboard.prefs.*;
import com.doubleyellow.util.Feature;
import com.doubleyellow.util.JsonUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.AbstractMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.UUID;

public enum Brand
{
    Squore          (SportType.Squash     , R.string.app_name_short_brand__Squash          , 0, 0/*R.drawable.brand_squore*/    , R.id.sb_branded_logo_ar150, R.color.brand_squore_bg_color          , R.string.REMOTE_DISPLAY_APP_ID_brand_squore          , R.array.colorSchema                                                        , 3000, "https://squore.double-yellow.be"   , "8da78f9d-f7dd-437c-8b7d-d5bc231f92ec", R.raw.changelog),
  //Tabletennis     (SportType.Tabletennis, R.string.app_name_short_brand__Tabletennis     , 0, 0/*R.drawable.brand_squore*/            , R.id.sb_branded_logo_ar150, R.color.brand_tabletennis_bg_color     , R.string.CUSTOM_RECEIVER_APP_ID_brand_shared         , R.array.colorSchema__Tabletennis                                            , 3000, "https://tabletennis.double-yellow.be", "e35d8fcb-a7c0-4c2a-9c74-cc3f6e1e6a41", R.raw.changelog_tabletennis),
  //Badminton       (SportType.Badminton  , R.string.app_name_short_brand__Badminton       , 0, 0/*R.drawable.brand_squore*/            , R.id.sb_branded_logo_ar150, R.color.brand_badminton_bg_color       , R.string.CUSTOM_RECEIVER_APP_ID_brand_shared         , R.array.colorSchema__Badminton                                              , 3000, "https://badminton.double-yellow.be", "18be497c-ddfa-4851-9b43-0a5866605252", R.raw.changelog_badminton),
  //Racketlon       (SportType.Racketlon  , R.string.app_name_short_brand__Racketlon       , 0, 0/*R.drawable.brand_squore*/              , R.id.sb_branded_logo_ar150, R.color.brand_racketlon_bg_color       , R.string.REMOTE_DISPLAY_APP_ID_brand_racketlon       , R.array.colorSchema__Racketlon                                              , 3000, "https://racketlon.double-yellow.be", "6ffc7128-6fd5-46d1-b79c-46e4c613cba5", R.raw.changelog_racketlon),
  //TennisPadel     (SportType.TennisPadel, R.string.app_name_short_brand__TennisPadel     , 0, R.drawable.logo_brand_tennispadel         , R.id.sb_branded_logo_ar400, R.color.brand_padel_bg_color           , R.string.CUSTOM_RECEIVER_APP_ID_brand_shared         , R.array.colorSchema__TennisPadel                                    , 4000, "https://tennispadel.double-yellow.be", "239ad8ef-0cdd-490c-8f01-4d50dd4e7c6b", R.raw.changelog_tennispadel),
  //Racquetball     (SportType.Racquetball, R.string.app_name_short_brand__Racquetball     , 0, 0/*R.drawable.brand_squore*/              , R.id.sb_branded_logo_ar150, 0                                      , R.string.CUSTOM_RECEIVER_APP_ID_brand_shared     , R.array.colorSchema__Racquetball                                            , 3000, "https://racquetball.double-yellow.be", R.raw.changelog),

    ;
    Brand( SportType sport
         , int iShortNameResId
         , int iTurnedOffFlags
         , int iLogoResId
         , int imageViewResId
         , int iBgColorResId
         , int iRemoteDisplayAppIdResId
         , int iColorsResId
         , int iSplashDuration
         , String sBaseURL
         , String sUUID
         , int iChangeLogResId
         ) {
        this.eSport                   = sport;
        this.iShortNameResId          = iShortNameResId;
        this.iLogoResId               = iLogoResId;
        this.imageViewResId           = imageViewResId;
        this.iBgColorResId            = iBgColorResId;
        this.iRemoteDisplayAppIdResId = iRemoteDisplayAppIdResId;
        this.iColorsResId             = iColorsResId;
        this.iSplashDuration          = iSplashDuration;
        this.sBaseURL                 = sBaseURL;
        this.uuid                     = UUID.fromString(sUUID); // https://www.uuidgenerator.net/version4
        this.iChangeLogResId          = iChangeLogResId;
        this.iTurnedOffFlags          = iTurnedOffFlags;
    };

    public  static/*final*/Brand brand = Brand.Squore;

    private SportType eSport;
    private int iShortNameResId;
    private int iSplashDuration;
    private int iLogoResId;
    private int imageViewResId;
    private int iBgColorResId;
    private int iColorsResId;
    private int iRemoteDisplayAppIdResId;
    private int iChangeLogResId;
    private int iTurnedOffFlags;
    private String sBaseURL;
    private UUID uuid;

    public SportType getSportType()         { return eSport; }
    /* @deprecated */
    //public int getRemoteDisplayAppIdResId() { return iRemoteDisplayAppIdResId;}

    private static JSONObject m_mCastConfig = null;
    public static void setCastConfig(JSONObject mCastConfig) {
        //if ( true ) { return; } // TODO: for now don't use webconfig
        m_mCastConfig = mCastConfig;
    }
    public static boolean hasWebConfig() {
        return ( m_mCastConfig != null ) && ( 0 < m_mCastConfig.length() );
    }
/*
    public static JSONObject getWebConfig() {
        return m_mCastConfig;
    }
*/
    public Map.Entry<String, String> getRemoteDisplayAppId2Info(Context ctx) {
        if ( m_mCastConfig != null ) {
            JSONArray lBrand = getCastListForBrandFromConfig(true);
            int iIdx = PreferenceValues.useCastScreen(ctx);
            iIdx = iIdx % JsonUtil.size(lBrand);
            String sCastId = lBrand.optString(iIdx);
            return new AbstractMap.SimpleEntry<String, String>(sCastId, m_mCastConfig.optString(sCastId, sCastId));
        }

        int iFixedResId = iRemoteDisplayAppIdResId;
        if ( PreferenceValues.isBrandTesting(ctx) ) {
            iFixedResId = R.string.CUSTOM_RECEIVER_APP_ID_brand_test;
            if ( false && this.equals(Squore) ) {
                iFixedResId = R.string.CUSTOM_RECEIVER_APP_ID_brand_shared;
            }
        }

        String sResName = ctx.getResources().getResourceName(iFixedResId);
        return new AbstractMap.SimpleEntry<String, String>(ctx.getString(iFixedResId), sResName);
    }

    public JSONArray getCastListForBrandFromConfig(boolean bReturnKey) {
        if ( m_mCastConfig == null ) { return null; }

        JSONArray lDefaults = m_mCastConfig.optJSONArray("Default");
        JSONArray lBrand    = m_mCastConfig.optJSONArray(this.toString());
        if (JsonUtil.isNotEmpty(lDefaults) && JsonUtil.isNotEmpty(lBrand) ) {
            lBrand = clone(lBrand); // do not modify the array part of the config
            for (int i = 0; i < lDefaults.length(); i++) {
                lBrand.put(lDefaults.optString(i));
            }
            //ListUtil.removeDuplicates(lBrand);
        }
        if ( JsonUtil.isNotEmpty(lDefaults) && JsonUtil.isEmpty(lBrand) ) {
            lBrand = lDefaults;
        }

        // translate to human readable
        if ( bReturnKey ) {

        } else {
            lBrand = translate(lBrand, m_mCastConfig);
        }

        return lBrand;
    }

    private static JSONArray clone(JSONArray ja) {
        JSONArray arReturn = new JSONArray();
        for ( int i = 0; i < ja.length(); i++ ) {
            arReturn.put(ja.opt(i));
        }
        return arReturn;
    }
    private static JSONArray translate(JSONArray ja, JSONObject translations) {
        JSONArray arReturn = new JSONArray();
        for ( int i = 0; i < ja.length(); i++ ) {
            if ( translations == null ) {
                arReturn.put(ja.opt(i));
            } else {
                String sValue = String.valueOf(ja.opt(i));
                arReturn.put(translations.optString(sValue, sValue));
            }
        }
        return arReturn;
    }

    public static final int[] imageViewIds = { R.id.sb_branded_logo_ar400, R.id.sb_branded_logo_ar150 };

    public static void setBrandPrefs(Context ctx) {
        if ( Brand.brand.equals(Brand.Squore) ) { return; }
        //PreferenceValues.setStringSet(PreferenceKeys.OrientationPreference, OrientationPreference.Landscape, ctx);
        //PreferenceValues.setStringSet(PreferenceKeys.floatingMessageForGameBallOn, ShowOnScreen.OnDevice, ctx);

        //PreferenceValues.setStringSet(PreferenceKeys.showBrandLogoOn, ShowOnScreen.OnChromeCast, ctx); // = in line with normal default

      //PreferenceValues.setBoolean(PreferenceKeys.showFullScreen, ctx, true);
        if ( ViewUtil.isWearable(ctx) == false ) {
            PreferenceValues.setBoolean(PreferenceKeys.showTips, ctx, false);

            PreferenceValues.setBoolean(PreferenceKeys.showActionBar , ctx, true); // to be able to quickly select Cast Button, branded version probably uses ChromeCast

            // if they get in the way of the logo on the device screen, set them to DoNotUse
            PreferenceValues.setEnum(PreferenceKeys.useOfficialAnnouncementsFeature, ctx, Feature.Suggest);
        }
/*
        if ( Brand.isSquash() ) {
            PreferenceValues.setEnum(PreferenceKeys.useShareFeature, ctx, Feature.DoNotUse);
        }
*/

        if ( false ) {
            try {
                // do NOT start intro or changelog based on version
                int logCheck = RWValues.getVersionCodeForChangeLogCheck(ctx);
                PreferenceValues.setNumber(PreferenceKeys.viewedChangelogVersion, ctx, logCheck);
            } catch (Exception e) {
                //e.printStackTrace();
            }
        }

        //ColorPrefs.activeColorSchema(ctx, Brand.brand.getREColorPalette());
    }

    public static void setSportPrefs(Context ctx) {
        if ( isRacketlon() ) {
            PreferenceValues.setNumber   (PreferenceKeys.numberOfGamesToWinMatch         , ctx, 4); // TODO: not applicable. only total number of points matter
            PreferenceValues.setNumberR  (PreferenceKeys.numberOfPointsToWinGame         , ctx, R.integer.gameEndScore_default__Racketlon);
            PreferenceValues.setEnum     (PreferenceKeys.useOfficialAnnouncementsFeature , ctx, Feature.DoNotUse);
            PreferenceValues.setEnum     (PreferenceKeys.tieBreakFormat                  , ctx, TieBreakFormat.TwoClearPoints);
            PreferenceValues.setNumber   (PreferenceKeys.timerPauseBetweenGames          , ctx, 180);
            PreferenceValues.setNumber   (PreferenceKeys.timerWarmup                     , ctx, 180);
            PreferenceValues.setBoolean  (PreferenceKeys.showTips                        , ctx, false);
            PreferenceValues.setBoolean  (PreferenceKeys.useHandInHandOutScoring         , ctx, false);
        }
        if ( isTabletennis() ) {
            PreferenceValues.setNumberR  (PreferenceKeys.numberOfGamesToWinMatch         , ctx, R.integer.numberOfGamesToWin_default__Tabletennis);
            PreferenceValues.setNumberR  (PreferenceKeys.numberOfPointsToWinGame         , ctx, R.integer.gameEndScore_default__Tabletennis);
            PreferenceValues.setEnum     (PreferenceKeys.useOfficialAnnouncementsFeature , ctx, Feature.DoNotUse);
            PreferenceValues.setEnum     (PreferenceKeys.tieBreakFormat                  , ctx, TieBreakFormat.TwoClearPoints);
            PreferenceValues.setNumber   (PreferenceKeys.timerPauseBetweenGames          , ctx, 60);
            PreferenceValues.setNumber   (PreferenceKeys.timerWarmup                     , ctx, 120);
            PreferenceValues.setBoolean  (PreferenceKeys.showTips                        , ctx, false);
            PreferenceValues.setStringSet(PreferenceKeys.showScoringHistoryInMainScreenOn, EnumSet.noneOf(ShowOnScreen.class), ctx); // do not show the scoring history
            PreferenceValues.setBoolean  (PreferenceKeys.useHandInHandOutScoring         , ctx, false);
        }
        if ( isBadminton() ) {
            PreferenceValues.setNumberR  (PreferenceKeys.numberOfGamesToWinMatch         , ctx, R.integer.numberOfGamesToWin_default__Badminton);
            PreferenceValues.setNumberR  (PreferenceKeys.numberOfPointsToWinGame         , ctx, R.integer.gameEndScore_default__Badminton);
            PreferenceValues.setEnum     (PreferenceKeys.useOfficialAnnouncementsFeature , ctx, Feature.DoNotUse);
            PreferenceValues.setEnum     (PreferenceKeys.tieBreakFormat                  , ctx, TieBreakFormat.TwoClearPoints);
            PreferenceValues.setNumber   (PreferenceKeys.timerPauseBetweenGames          , ctx, 60);
            PreferenceValues.setNumber   (PreferenceKeys.timerWarmup                     , ctx, 120);
            PreferenceValues.setBoolean  (PreferenceKeys.showTips                        , ctx, false);
            PreferenceValues.setStringSet(PreferenceKeys.showScoringHistoryInMainScreenOn, EnumSet.noneOf(ShowOnScreen.class), ctx); // do not show the scoring history
            PreferenceValues.setBoolean  (PreferenceKeys.useHandInHandOutScoring         , ctx, false);
        }
        if ( isRacquetball() ) {
            PreferenceValues.setNumberR  (PreferenceKeys.numberOfGamesToWinMatch         , ctx, R.integer.numberOfGamesToWin_default__Racquetball);
            PreferenceValues.setNumberR  (PreferenceKeys.numberOfPointsToWinGame         , ctx, R.integer.gameEndScore_default__Racquetball); // last decisive game to 11
            PreferenceValues.setEnum     (PreferenceKeys.useOfficialAnnouncementsFeature , ctx, Feature.DoNotUse);
            PreferenceValues.setEnum     (PreferenceKeys.tieBreakFormat                  , ctx, TieBreakFormat.SuddenDeath);
            //PreferenceValues.setNumber   (PreferenceKeys.timerPauseBetweenGames          , ctx, 60);
            //PreferenceValues.setNumber   (PreferenceKeys.timerWarmup                     , ctx, 120);
            PreferenceValues.setBoolean  (PreferenceKeys.showTips                        , ctx, false);
            PreferenceValues.setStringSet(PreferenceKeys.showScoringHistoryInMainScreenOn, EnumSet.allOf(ShowOnScreen.class), ctx);
            PreferenceValues.setBoolean  (PreferenceKeys.useHandInHandOutScoring         , ctx, true);
        }
        if ( isSquash() ) {
            PreferenceValues.setNumberR  (PreferenceKeys.numberOfGamesToWinMatch         , ctx, R.integer.numberOfGamesToWin_default__Squash);
            PreferenceValues.setNumberR  (PreferenceKeys.numberOfPointsToWinGame         , ctx, R.integer.gameEndScore_default__Squash);
            PreferenceValues.setEnum     (PreferenceKeys.useOfficialAnnouncementsFeature , ctx, Feature.Suggest);
            PreferenceValues.setEnum     (PreferenceKeys.tieBreakFormat                  , ctx, TieBreakFormat.TwoClearPoints);
            PreferenceValues.setNumberR  (PreferenceKeys.timerPauseBetweenGames          , ctx, R.integer.timerPauseBetweenGames_default__Squash);
            PreferenceValues.setNumberR  (PreferenceKeys.timerWarmup                     , ctx, R.integer.timerWarmup_default__Squash);
            PreferenceValues.setStringSet(PreferenceKeys.showScoringHistoryInMainScreenOn, EnumSet.of(ShowOnScreen.OnDevice), ctx); // show the scoring history
            PreferenceValues.setBoolean  (PreferenceKeys.useHandInHandOutScoring         , ctx, false);
        }
        if ( isGameSetMatch() ) {
            PreferenceValues.setNumberR  (PreferenceKeys.numberOfPointsToWinGame         , ctx, R.integer.gameEndScore_default__TennisPadel);
            PreferenceValues.setNumberR  (PreferenceKeys.numberOfGamesToWinMatch         , ctx, R.integer.numberOfGamesToWin_default__TennisPadel);
            PreferenceValues.setEnum     (PreferenceKeys.useOfficialAnnouncementsFeature , ctx, Feature.DoNotUse);
            PreferenceValues.setEnum     (PreferenceKeys.tieBreakFormat                  , ctx, TieBreakFormat.TwoClearPoints);
            PreferenceValues.setStringSet(PreferenceKeys.showScoringHistoryInMainScreenOn, EnumSet.noneOf(ShowOnScreen.class), ctx);
            PreferenceValues.setBoolean  (PreferenceKeys.useHandInHandOutScoring         , ctx, false);
          //PreferenceValues.setStringSet(PreferenceKeys.floatingMessageForGameBallOn    , EnumSet.noneOf(ShowOnScreen.class), ctx);
            PreferenceValues.setEnum     (PreferenceKeys.endGameSuggestion               , ctx, Feature.Automatic);
        }
        if ( ViewUtil.isWearable(ctx) ) {
            PreferenceValues.setEnum (PreferenceKeys.endGameSuggestion               , ctx, Feature.Automatic);
            PreferenceValues.setEnum (PreferenceKeys.useOfficialAnnouncementsFeature , ctx, Feature.DoNotUse);
            PreferenceValues.setEnum (PreferenceKeys.useTimersFeature                , ctx, Feature.Suggest);
        }
    }

    public static Model getModel() {
        return ModelFactory.getModel(getSport());
    }
    public static boolean isRacketlon() {
        return getSport().equals(SportType.Racketlon);
    }
    public static boolean isTabletennis() {
        return getSport().equals(SportType.Tabletennis);
    }
    public static boolean isBadminton() {
        return getSport().equals(SportType.Badminton);
    }
    public static boolean supportsTimeout() {
        return isTabletennis() /* toweling down after each 6 points */
            || isBadminton()   /* short interval at 11 points in game to 21: TODO: implement */
        ;
    }
    public static boolean supportChooseServeOrReceive() {
        return isTabletennis() || isBadminton()
        ;
    }
    public static boolean supportsDoubleServeSequence() {
        return isSquash() || isBadminton();
    }
    public static boolean supportsTiebreakFormat() {
        return isSquash() || isBadminton() || isRacquetball();
    }

    public static SportType getSport() {
        return Brand.brand.getSportType();
    }

    public static boolean isNotSquash() {
        return isSquash() == false;
    }
    public static boolean isSquash() {
        return getSport().equals(SportType.Squash);
    }
    public static boolean isGameSetMatch() {
        return getSport().equals(SportType.TennisPadel) || isPadel();
    }
    public static boolean isPadel() {
        // return true ONLY for Padel, and NOT for tennis/padel
        return getSport().equals(SportType.Padel);
    }
    public static boolean isRacquetball() {
        return getSport().equals(SportType.Racquetball);
    }

    /*
    private static int I_FLAG_WARMUP_OFF   = 1;
    private static int I_FLAG_SINGLES_OFF  = 2;
    private static int I_FLAG_MYLIST_OFF   = 4;
    private static int I_FLAG_REFEREES_OFF = 8;

    public static boolean useWarmup() {
        return trueUnlessFlagIsOn(I_FLAG_WARMUP_OFF);
    }
    public static boolean useSinglesMatches() {
        return trueUnlessFlagIsOn(I_FLAG_SINGLES_OFF);
    }
    public static boolean useMyListMatches() {
        return trueUnlessFlagIsOn(I_FLAG_MYLIST_OFF);
    }
    public static boolean useReferees() {
        return trueUnlessFlagIsOn(I_FLAG_REFEREES_OFF);
    }
    */
    private static boolean trueUnlessFlagIsOn(int iFlag) {
        return (brand.iTurnedOffFlags & iFlag) != iFlag;
    }

    public static String getShortName(Context ctx) { return ctx.getString(brand.iShortNameResId);}
    public static String[] getColorsArray(Context ctx)  { return ctx.getResources().getStringArray(brand.iColorsResId) ;}

    public static String getBaseURL()              { return brand.sBaseURL;}
    /** Only used for BlueTooth connections for now */
    public static UUID   getUUID()                 { return brand.uuid;}

    public static int getImageViewResId()          { return brand.imageViewResId;}
    public static int getLogoResId()               { return brand.iLogoResId;}
    public static int getChangeLogResId()          { return brand.iChangeLogResId;}

    /** For now only used for Splash screen. Constructor specifies the color resource id. But this method already returns the actual color */
    public static int getBgColor(Context ctx)      { return brand.iBgColorResId==0?0:ctx.getResources().getColor(brand.iBgColorResId) ;}
    public static int getSplashDuration()          { return brand.iSplashDuration;}

    public static int getGameSetBallPoint_ResourceId() {
        return ( Brand.isRacketlon() || Brand.isGameSetMatch() ) ? R.string.oa_set_ball : ( Brand.isBadminton()? R.string.oa_gamepoint : R.string.oa_gameball );
    }
}
