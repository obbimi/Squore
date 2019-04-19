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

import android.content.Context;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.prefs.RWValues;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.model.ModelFactory;
import com.doubleyellow.scoreboard.model.SportType;
import com.doubleyellow.scoreboard.model.TieBreakFormat;
import com.doubleyellow.scoreboard.prefs.*;
import com.doubleyellow.util.Feature;

import java.util.EnumSet;
import java.util.UUID;

public enum Brand
{
    Squore          (SportType.Squash     , R.string.app_name_short_brand_Squash          , 0/*R.drawable.brand_squore*/                               , R.id.sb_branded_logo_ar150, R.color.brand_squore_bg_color          , R.string.REMOTE_DISPLAY_APP_ID_brand_squore          , R.array.colorSchema            , 0                                            , 3000, "http://squore.double-yellow.be"   , "8da78f9d-f7dd-437c-8b7d-d5bc231f92ec", R.raw.changelog),
  //Tabletennis     (SportType.Tabletennis, R.string.app_name_short_brand_Tabletennis     , 0/*R.drawable.brand_squore*/                               , R.id.sb_branded_logo_ar150, R.color.brand_tabletennis_bg_color     , R.string.REMOTE_DISPLAY_APP_ID_brand_tabletennis     , R.array.colorSchema_Tabletennis, 0                                            , 3000, "http://tabletennis.double-yellow.be", "e35d8fcb-a7c0-4c2a-9c74-cc3f6e1e6a41", R.raw.changelog_tabletennis),
  //Racketlon       (SportType.Racketlon  , R.string.app_name_short_brand_Racketlon       , 0/*R.drawable.brand_squore*/                               , R.id.sb_branded_logo_ar150, R.color.brand_racketlon_bg_color       , R.string.REMOTE_DISPLAY_APP_ID_brand_racketlon       , R.array.colorSchema_Racketlon  , 0                                            , 3000, "http://racketlon.double-yellow.be", "6ffc7128-6fd5-46d1-b79c-46e4c613cba5", R.raw.changelog_racketlon),
  //Racquetball     (SportType.Racquetball, R.string.app_name_short_brand_Racquetball     , 0/*R.drawable.brand_squore*/                               , R.id.sb_branded_logo_ar150, 0                                      , R.string.REMOTE_DISPLAY_APP_ID_brand_Racquetball     , R.array.colorSchema_Racquetball, 0                                            , 3000, "http://racquetball.double-yellow.be", R.raw.changelog),
  //ASB             (SportType.Squash     , R.string.app_name_short_brand_asb             , R.drawable.brand_asb_squash_court_logo_black_on_transparent, R.id.sb_branded_logo_ar150, R.color.brand_asb_bg_color             , R.string.REMOTE_DISPLAY_APP_ID_brand_asb             , R.array.colorSchema            , R.string.brand_asb_color_name_re             , 2000, "http://squore.double-yellow.be", R.raw.changelog),
  //CourtCare       (SportType.Squash     , R.string.app_name_short_brand_courtcare       , R.drawable.brand_courtcare                                 , R.id.sb_branded_logo_ar400, R.color.brand_courtcare_bg_color       , R.string.REMOTE_DISPLAY_APP_ID_brand_courtcare       , R.array.colorSchema_CourtCare  , R.string.brand_courtcare_color_name_re       , 4000, "http://squore.double-yellow.be", "060fa379-fc13-448d-b17a-87468ddd02ce", R.raw.changelog_courtcare),
  //XXXOfNotthingham(SportType.Squash     , R.string.app_name_short_brand_uniofnotthingham, R.drawable.brand_uniofnotthingham                          , R.id.sb_branded_logo_ar150, R.color.brand_uniofnotthingham_bg_color, R.string.REMOTE_DISPLAY_APP_ID_brand_uniofnotthingham, R.array.colorSchema_UoN        , R.string.brand_uniofnotthingham_color_name_re, 4000, "http://squore.double-yellow.be", R.raw.changelog_courtcare),
  //UniOfNotthingham(SportType.Squash     , R.string.app_name_short_brand_uniofnotthingham, R.drawable.brand_courtcare/*_uoncolors*/                   , R.id.sb_branded_logo_ar400, R.color.brand_uniofnotthingham_bg_color, R.string.REMOTE_DISPLAY_APP_ID_brand_uniofnotthingham, R.array.colorSchema_UoN        , R.string.brand_uniofnotthingham_color_name_re, 4000, "http://squore.double-yellow.be", R.raw.changelog_courtcare),
    ;
    Brand( SportType sport
         , int iShortNameResId
         , int iLogoResId
         , int imageViewResId
         , int iBgColorResId
         , int iRemoteDisplayAppIdResId
         , int iColorsResId
         , int iREColorPalette
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
      //this.iREColorPalette          = iREColorPalette;
        this.iSplashDuration          = iSplashDuration;
        this.sBaseURL                 = sBaseURL;
        this.uuid                     = UUID.fromString(sUUID);
        this.iChangeLogResId          = iChangeLogResId;
    };

    private SportType eSport;
    private int iShortNameResId;
    private int iSplashDuration;
    private int iLogoResId;
    private int imageViewResId;
    private int iBgColorResId;
    private int iColorsResId;
  //private int iREColorPalette;
    private int iRemoteDisplayAppIdResId;
    private int iChangeLogResId;
    private String sBaseURL;
    private UUID uuid;

    public SportType getSportType()         { return eSport; }
  //public int getREColorPalette()          { return iREColorPalette;}
    public int getRemoteDisplayAppIdResId() { return iRemoteDisplayAppIdResId;}

    public static final int[] imageViewIds = { R.id.sb_branded_logo_ar400, R.id.sb_branded_logo_ar150 };

    public static void setBrandPrefs(Context ctx) {
        if ( Brand.brand.equals(Brand.Squore) ) { return; }
        //PreferenceValues.setStringSet(PreferenceKeys.OrientationPreference, OrientationPreference.Landscape, ctx);
        //PreferenceValues.setStringSet(PreferenceKeys.floatingMessageForGameBallOn, ShowOnScreen.OnDevice, ctx);

        //PreferenceValues.setStringSet(PreferenceKeys.showBrandLogoOn, ShowOnScreen.OnChromeCast, ctx); // = in line with normal default

        PreferenceValues.setBoolean(PreferenceKeys.showTips, ctx, false);

      //PreferenceValues.setBoolean(PreferenceKeys.showFullScreen, ctx, true);
        PreferenceValues.setBoolean(PreferenceKeys.showActionBar , ctx, true); // to be able to quickly select Cast Button, branded version probably uses ChromeCast

        // if they get in the way of the logo on the device screen, set them to DoNotUse
        PreferenceValues.setEnum(PreferenceKeys.useOfficialAnnouncementsFeature, ctx, Feature.Suggest);
/*
        if ( Brand.isSquash() ) {
            PreferenceValues.setEnum(PreferenceKeys.useShareFeature, ctx, Feature.DoNotUse);
        }
*/

        // do NOT start intro or changelog based on version
        try {
            int logCheck = RWValues.getVersionCodeForChangeLogCheck(ctx);
            PreferenceValues.setNumber(PreferenceKeys.viewedChangelogVersion, ctx, logCheck);
        } catch (Exception e) {
            //e.printStackTrace();
        }

        //ColorPrefs.activeColorSchema(ctx, Brand.brand.getREColorPalette());
    }

    public static void setSportPrefs(Context ctx) {
        if ( isRacketlon() ) {
            PreferenceValues.setNumber   (PreferenceKeys.numberOfGamesToWinMatch         , ctx, 4); // TODO: not applicable. only total number of points matter
            PreferenceValues.setNumber   (PreferenceKeys.numberOfPointsToWinGame         , ctx, 21);
            PreferenceValues.setEnum     (PreferenceKeys.useOfficialAnnouncementsFeature , ctx, Feature.DoNotUse);
            PreferenceValues.setEnum     (PreferenceKeys.tieBreakFormat                  , ctx, TieBreakFormat.TwoClearPoints);
            PreferenceValues.setNumber   (PreferenceKeys.timerPauseBetweenGames          , ctx, 180);
            PreferenceValues.setNumber   (PreferenceKeys.timerWarmup                     , ctx, 180);
            PreferenceValues.setBoolean  (PreferenceKeys.showTips                        , ctx, false);
            PreferenceValues.setBoolean  (PreferenceKeys.useHandInHandOutScoring         , ctx, false);
        }
        if ( isTabletennis() ) {
            PreferenceValues.setNumber   (PreferenceKeys.numberOfGamesToWinMatch         , ctx, 4);
            PreferenceValues.setNumber   (PreferenceKeys.numberOfPointsToWinGame         , ctx, 11);
            PreferenceValues.setEnum     (PreferenceKeys.useOfficialAnnouncementsFeature , ctx, Feature.DoNotUse);
            PreferenceValues.setEnum     (PreferenceKeys.tieBreakFormat                  , ctx, TieBreakFormat.TwoClearPoints);
            PreferenceValues.setNumber   (PreferenceKeys.timerPauseBetweenGames          , ctx, 60);
            PreferenceValues.setNumber   (PreferenceKeys.timerWarmup                     , ctx, 120);
            PreferenceValues.setBoolean  (PreferenceKeys.showTips                        , ctx, false);
            PreferenceValues.setStringSet(PreferenceKeys.showScoringHistoryInMainScreenOn, EnumSet.noneOf(ShowOnScreen.class), ctx); // do not show the scoring history
            PreferenceValues.setBoolean  (PreferenceKeys.useHandInHandOutScoring         , ctx, false);
        }
        if ( isRacquetball() ) {
            PreferenceValues.setNumber   (PreferenceKeys.numberOfGamesToWinMatch         , ctx, 2);
            PreferenceValues.setNumber   (PreferenceKeys.numberOfPointsToWinGame         , ctx, 15); // last deicisive game to 11
            PreferenceValues.setEnum     (PreferenceKeys.useOfficialAnnouncementsFeature , ctx, Feature.DoNotUse);
            PreferenceValues.setEnum     (PreferenceKeys.tieBreakFormat                  , ctx, TieBreakFormat.SuddenDeath);
            //PreferenceValues.setNumber   (PreferenceKeys.timerPauseBetweenGames          , ctx, 60);
            //PreferenceValues.setNumber   (PreferenceKeys.timerWarmup                     , ctx, 120);
            PreferenceValues.setBoolean  (PreferenceKeys.showTips                        , ctx, false);
            PreferenceValues.setStringSet(PreferenceKeys.showScoringHistoryInMainScreenOn, EnumSet.allOf(ShowOnScreen.class), ctx);
            PreferenceValues.setBoolean  (PreferenceKeys.useHandInHandOutScoring         , ctx, true);
        }
        if ( isSquash() ) {
            PreferenceValues.setNumberR  (PreferenceKeys.numberOfGamesToWinMatch         , ctx, R.integer.numberOfGamesToWin_default);
            PreferenceValues.setNumberR  (PreferenceKeys.numberOfPointsToWinGame         , ctx, R.integer.gameEndScore_default);
            PreferenceValues.setEnum     (PreferenceKeys.useOfficialAnnouncementsFeature , ctx, Feature.Suggest);
            PreferenceValues.setEnum     (PreferenceKeys.tieBreakFormat                  , ctx, TieBreakFormat.TwoClearPoints);
            PreferenceValues.setNumberR  (PreferenceKeys.timerPauseBetweenGames          , ctx, R.integer.timerPauseBetweenGames_default);
            PreferenceValues.setNumberR  (PreferenceKeys.timerWarmup                     , ctx, R.integer.timerWarmup_default);
            PreferenceValues.setStringSet(PreferenceKeys.showScoringHistoryInMainScreenOn, EnumSet.of(ShowOnScreen.OnDevice), ctx); // show the scoring history
            PreferenceValues.setBoolean  (PreferenceKeys.useHandInHandOutScoring         , ctx, false);
        }
    }

    public  static/*final*/Brand brand = Brand.Squore;

    public static Model getModel() {
        return ModelFactory.getModel(getSport());
    }
    public static boolean isRacketlon() {
        return getSport().equals(SportType.Racketlon);
    }
    public static boolean isTabletennis() {
        return getSport().equals(SportType.Tabletennis);
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
    public static boolean isRacquetball() {
        return getSport().equals(SportType.Racquetball);
    }

    public static String getShortName(Context ctx) { return ctx.getString(brand.iShortNameResId);}
    public static String[] getColorsArray(Context ctx)  { return ctx.getResources().getStringArray(brand.iColorsResId) ;}

    public static String getBaseURL()              { return brand.sBaseURL;}
    public static UUID   getUUID()                 { return brand.uuid;}

    public static int getImageViewResId()          { return brand.imageViewResId;}
    public static int getLogoResId()               { return brand.iLogoResId;}
    public static int getChangeLogResId()          { return brand.iChangeLogResId;}

    /** For now only used for Splash screen. Constructor specifies the color resource id. But this method already returns the actual color */
    public static int getBgColor(Context ctx)      { return brand.iBgColorResId==0?0:ctx.getResources().getColor(brand.iBgColorResId) ;}
    public static int getSplashDuration()          { return brand.iSplashDuration;}
}
