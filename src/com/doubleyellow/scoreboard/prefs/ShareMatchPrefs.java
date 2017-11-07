package com.doubleyellow.scoreboard.prefs;

import com.doubleyellow.scoreboard.R;

/**
 * Share how/when
 * - link
 * - summary
 * - post to site
 * - plain text sheet
 */
public enum ShareMatchPrefs {
    /** Keep values/arrays-xx/string-array[@name='ShareMatchPrefsDisplayValues'] in sync */
    LinkWithFullDetails        (R.id.sb_share_score_sheet             , android.R.drawable.ic_menu_share     , false),
    LinkWithFullDetailsEachGame(R.id.sb_share_score_sheet             , android.R.drawable.ic_menu_share     , true),
    LinkWithFullDetailsEachHalf(R.id.sb_share_score_sheet             , android.R.drawable.ic_menu_share     , true),
    LinkWithFullDetailsEachPoint(R.id.sb_share_score_sheet            , android.R.drawable.ic_menu_share     , true),
    Summary                    (R.id.sb_share_match_summary           , android.R.drawable.ic_menu_share     , false),
    SummaryMultiple            (R.id.sb_share_matches_summary         , android.R.drawable.ic_menu_share     , false),
    SummaryToDefault           (R.id.sb_send_match_result             ,         R.drawable.ic_action_send_now, false), // only if default sms recipient specified
    PostResult                 (R.id.sb_post_match_result             ,         R.drawable.ic_menu_forward   , false), // only if post url specified
    SheetInEmail               (R.id.sb_email_match_result            , android.R.drawable.ic_dialog_email   , false),
    //SummaryToClipboard       (R.id.sb_put_match_summary_on_clipboard, android.R.drawable.ic_menu_share   , false),
    //SummaryToWhatsApp        (R.id.sb_whatsapp_match_summary        , android.R.drawable.ic_menu_share   , false),
    ;

    private int     iMenuOption = 0;
    private int     iDrawableId = 0;
    private boolean bAlsoBeforeMatchEnd = false;
    ShareMatchPrefs(int i, int d, boolean b) {
        this.iMenuOption = i;
        this.iDrawableId = d;
        this.bAlsoBeforeMatchEnd = b;
    }
    public int getMenuId() {
        return iMenuOption;
    }
    public int getDrawableId() {
        return iDrawableId;
    }
    public boolean alsoBeforeMatchEnd() {
        return bAlsoBeforeMatchEnd;
    }
}
