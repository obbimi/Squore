package com.doubleyellow.scoreboard.model;

import java.util.Map;

import android.content.Context;
import android.os.Build;
import com.doubleyellow.android.util.ContentUtil;
import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.prefs.ShowOnScreen;
import com.doubleyellow.util.StringUtil;

/**
 * Static methods that are not specific to the squash specific 'Model' class.
 * This is to be later re-use these functions in e.g. a scoreboard Model for badminton/tennis and/or tabletennis.
 */
public class Util {

    public static final String MY_DEVICE_MODEL = "GT-I9505,SM-G930F"; // samsung S4, samsung S7

    public static Player getWinner(Map<Player, Integer> scores) {
        // no winner?
        if ( scores.get(Player.A) == scores.get(Player.B) ) { return null; }

        // a winner
        return ( scores.get(Player.A) > scores.get(Player.B) )? Player.A : Player.B;
    }

    public static final String removeSeedingRegExp = "[\\[\\(]" + "[0-9/\\\\]+" + "[\\]\\)]\\s*";
    public static String removeSeeding(String sPlayer) {
        if ( StringUtil.isEmpty(sPlayer) ) { return sPlayer; }
        return sPlayer.replaceFirst(removeSeedingRegExp, "").trim();
    }

    public static final String removeCountryRegExp = "[\\[\\(]" + "[A-Za-z]+" + "[\\]\\)]\\s*";
    public static String removeCountry(String sPlayer) {
        if ( StringUtil.isEmpty(sPlayer) ) { return sPlayer; }
        return sPlayer.replaceFirst(removeCountryRegExp, "").trim();
    }

    /** returns a filename to save screenshot to, if conditions have been met. If not returns null (= don't take automated screenshot) */
    public static String filenameForAutomaticScreenshot(Context ctx, Model model, ShowOnScreen screenType, int iMinScore, int iMaxScore, String sFormat) {
        if ( /*(Brand.brand!=Brand.Squore) &&*/ isMyDevice(ctx) ) {
            if ( sFormat != null ) {
                return String.format(sFormat, Brand.brand, screenType);
            }
            if ( model.getName(Player.A).matches("(Player [AB]|Shaun|.*Matthew.*)") == false ) {
                return null;
            }
            if ( (model.getMaxScore() == iMaxScore || iMaxScore == -1) && (model.getMinScore() == iMinScore || iMinScore == -1)) {
                return String.format("%s.G%s.P%s-%s.%s.png", Brand.brand, model.getResultShort(), model.getScore(Player.A), model.getScore(Player.B), screenType);
            }
        }
        return null;
    }

    private static Boolean bIsMyDevice = null;
    public static boolean isMyDevice(Context ctx) {
        if ( bIsMyDevice == null ) {
            bIsMyDevice = MY_DEVICE_MODEL.contains(Build.MODEL) && ContentUtil.isAppInstalled(ctx, "com.doubleyellow");
        }
        return bIsMyDevice;
    }

}
