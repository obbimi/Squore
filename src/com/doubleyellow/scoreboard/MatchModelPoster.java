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

import android.app.ProgressDialog;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;
import com.doubleyellow.android.task.URLTask;
import com.doubleyellow.android.util.ContentReceiver;
import com.doubleyellow.android.util.ContentUtil;
import com.doubleyellow.android.view.ViewUtil;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.dialog.OnlineSheetAvailableChoice;
import com.doubleyellow.scoreboard.main.DialogManager;
import com.doubleyellow.scoreboard.model.LockState;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.model.Player;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.scoreboard.prefs.ShareMatchPrefs;
import com.doubleyellow.util.*;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;

/**
 * Helper class for posting the match model to a website.
 * To share a match result.
 * Simple php that might receive the post may look like (see bottom of this class)
 */
public class MatchModelPoster implements ContentReceiver
{
    public static final String TAG = "SB." + MatchModelPoster.class.getSimpleName();

    private Context m_context = null;
    private Model   m_model   = null;
    public MatchModelPoster() { }

    private static final String NAME       = "name";

    private static Params  mCategorizedToFromMap = null;
    private        String  sName;
    private        boolean m_bAutoShareTriggeredBeforeEndOfMatch = false;
    public void post(Context context, Model matchModel, JSONObject oSettings)
    {
        m_context = context;
        m_model   = matchModel;
        m_bAutoShareTriggeredBeforeEndOfMatch = autoShareTriggeredBeforeEndOfMatch(matchModel);

        sShowURL = matchModel.getShareURL();
        if ( StringUtil.isNotEmpty(sShowURL) ) {
            // shared before and unchanged
            presentChoice(sShowURL);
        } else {

            if ( m_bAutoShareTriggeredBeforeEndOfMatch == false ) {
                showProgress(R.string.creating_url_for_sharing_match_details);
            }

            Date   date              = matchModel.getMatchDate();
            String sPlayerNames      = matchModel.getName(Player.A) + "_" + matchModel.getName(Player.B);
            if ( matchModel.isDoubles() ) {
                // ensure for each team the names of the players are used in alphabetical order: this to ensure it's name does not change when in a certain game/set the names are swapped
                sPlayerNames = "";
                for(Player p: Model.getPlayers()) {
                    String[] saNames = matchModel.getDoublePlayerNames(p);
                    sPlayerNames += ListUtil.join(Arrays.asList(saNames), "/");
                    if ( p.equals(Player.A) ) {
                        sPlayerNames += "_";
                    }
                }
            }
            String sPlayerNamesAscii = StringUtil.convertNonAscii(sPlayerNames);
                   sPlayerNamesAscii = sPlayerNamesAscii.replaceAll("[^a-zA-Z0-9_]", "");
            if (mCategorizedToFromMap == null) {
                String sURLify = ContentUtil.readRaw(context, R.raw.urlifymap);
                mCategorizedToFromMap = MapUtil.parseToMap(sURLify, Params.class);
            }
            if (sPlayerNamesAscii.length() < 3) {
                sPlayerNamesAscii = sPlayerNames;
                sPlayerNamesAscii = StringUtil.translateCharacters(sPlayerNamesAscii, mCategorizedToFromMap, PreferenceValues.getDeviceLanguage(context));
            }
            sName = DateUtil.formatDate2String(date, DateUtil.YYYYMMDD_HHMM) + "_" + sPlayerNamesAscii;
            sName = sName.replaceAll("[^a-zA-Z0-9_]", "");

            // TODO: allow to take base URL from settings
            String sURL = Brand.getBaseURL() + "/store/" + sName;
/*
        boolean bAddNameToURL = false;
        if ( bAddNameToURL ) {
            sURL += ("?" + NAME + "=" + sName );
        }
*/
            if ( ScoreBoard.isInPromoMode() ) {
                sURL += (sURL.contains("?") ? "&" : "?") + "noemail=1";
            }
            PostTask postTask = new PostTask(context, sURL);
            postTask.setContentReceiver(this);

            LockState lsRestore = matchModel.getLockState();
            if ( matchModel.matchHasEnded() ) {
                matchModel.setLockState(LockState.SharedEndedMatch);
            }
            String sJson = matchModel.toJsonString(context, oSettings);
            postTask.execute(URLTask.__BODY__, sJson);
            matchModel.setLockState(lsRestore);
        }
    }

    private String sShowURL = null;
    @Override public void receive(String sContent, FetchResult result, long lCacheAge, String sLastSuccessfulContent) {
        Log.i(TAG, result + " Content : " + sContent);

        if ( result.equals(FetchResult.OK) ) {
            //Toast.makeText(this.context, sContent, Toast.LENGTH_LONG).show();

            // in facebook only the URL is actually taken from the extra text
            //String sURL = URLFeedTask.SQUORE_BASE_URL + "/" + sShowPage + "?" + NAME + "=" + this.sName;
            // get the name from the response from the server
            Params mReturnValues = MapUtil.parseToMap(sContent, Params.class);
            if ( MapUtil.isNotEmpty(mReturnValues) ) {
                // server might have returned another name, use that one to ensure you have the one actually used on the server
                this.sName = mReturnValues.getOptionalString(NAME, this.sName);
            }
            sShowURL = Brand.getBaseURL() + "/" + this.sName;
            m_model.setShareURL(sShowURL);
            Log.i(TAG, "Match shared. " + sShowURL);
            presentChoice(sShowURL);
        } else {
            if ( m_bAutoShareTriggeredBeforeEndOfMatch == false ) {
                String sTitle = "Sharing failed";
                String sMess  = String.format("Something went wrong while preparing link for sharing. Sorry. Please try again later. (%s)", result);
                DialogManager dialogManager = DialogManager.getInstance();
                dialogManager.showMessageDialog(m_context, sTitle, sMess);
/*
                if ( m_context instanceof ScoreBoard ) {
                    ScoreBoard scoreBoard = (ScoreBoard) m_context;
                    scoreBoard.showMessageDialog(sTitle, sMess);
                } else {
                    ScoreBoard.dialogWithOkOnly(m_context, sTitle, sMess, true);
                }
*/
                Toast.makeText(m_context, sContent, Toast.LENGTH_LONG).show();
            } else {
                Log.w(TAG, "Match not auto shared. No network? " + result);
            }
        }
        hideProgress();
    }

    private void presentChoice(String sShowURL) {
        if ( m_bAutoShareTriggeredBeforeEndOfMatch ) {
            // presume auto triggered
            ShareMatchPrefs liveScore = PreferenceValues.isConfiguredForLiveScore(m_context);
            if ( ShareMatchPrefs.LinkWithFullDetailsEachPoint.equals(liveScore) == false ) {
                // do not show toast if sheet is updated every point
                Toast.makeText(m_context, R.string.sb_online_sheet_updated, Toast.LENGTH_SHORT).show();
            } else {
                // only show it once in a while
                if ( (m_model.getMaxScore() + m_model.getMinScore()) % 5 == 0  ) {
                    Toast.makeText(m_context, R.string.sb_online_sheet_updated, Toast.LENGTH_SHORT).show();
                }
            }
            return;
        }

        if ( ViewUtil.isWearable(m_context) ) {
            // OnlineSheetAvailableChoice has not many actions well supported on wearables
            return;
        }

        ScoreBoard sb = null;
        if ( m_context instanceof ScoreBoard ) {
            // triggered from match in scoreboard
            sb = (ScoreBoard) m_context;
        }
        // triggered from stored (old) match
        OnlineSheetAvailableChoice sheetAvailableChoice = new OnlineSheetAvailableChoice(m_context, m_model, sb);
        sheetAvailableChoice.init(sShowURL);
        DialogManager dialogManager = DialogManager.getInstance();
        dialogManager.addToDialogStack(sheetAvailableChoice);
    }

    private boolean autoShareTriggeredBeforeEndOfMatch(Model matchModel) {
        ShareMatchPrefs shareHowAndWhen = PreferenceValues.getShareAction(m_context);
        Feature         shareFeature    = PreferenceValues.useShareFeature(m_context);
        return shareFeature.equals(Feature.Automatic) && shareHowAndWhen.alsoBeforeMatchEnd() && (matchModel.matchHasEnded() == false);
    }

    private class PostTask extends URLTask {
        public PostTask(Context context, String sURL) {
            super(context, sURL, URLTask.POST, null);
        }
        @Override public String getBaseUrl(Context context) { return ""; }
        @Override public String getAccountName(Context context) { return ""; }
        @Override public String addParametersToReturnUrl(Context ctx, String sURL, String configuredAccountName, String sReturn) { return null; }
        @Override public int getReadTimeout() { return 10000; }
        @Override public boolean returnContent(String sURL) { return true; }
        @Override public boolean validateContent(String sURL, String sContent, String sValidation) {
            if ( sContent.toLowerCase().contains("<html") ) {
                return false;
            }
            return true;
        }
        @Override public int getMaximumReuseCacheTimeMS(String sCacheFile) { return 0; }
        @Override public Map.Entry<FetchResult, String> downloadHelperFiles(Context ctx, String sURL, Map<String, String> hParams) { return null; }
    }

    //---------------------------------------------------------
    // Progress message
    //---------------------------------------------------------
    private ProgressDialog progressDialog = null;
    private void showProgress(int iResId) {
        if ( progressDialog == null ) {
            progressDialog = new ProgressDialog(m_context);
        }
        progressDialog.setIndeterminate(true);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage(m_context.getString(iResId));
        progressDialog.show();
    }
    private void hideProgress() {
        if ( progressDialog != null ) {
            try {
                progressDialog.cancel();
                progressDialog.dismiss();
            } catch (Exception e) {
/* IH 20170607: try catch to prevent crash (reported for version 144 on android 5.1)
                java.lang.IllegalArgumentException:
                at android.view.WindowManagerGlobal.findViewLocked(WindowManagerGlobal.java:416)
                at android.view.WindowManagerGlobal.removeView(WindowManagerGlobal.java:342)
                at android.view.WindowManagerImpl.removeViewImmediate(WindowManagerImpl.java:116)
                at android.app.Dialog.dismissDialog(Dialog.java:362)
                at android.app.Dialog.dismiss(Dialog.java:345)
                at android.app.Dialog.cancel(Dialog.java:1160)
                at com.doubleyellow.scoreboard.MatchModelPoster.hideProgress(MatchModelPoster.java:247)
*/
            }
        }
    }

}
/*
<?php

$file = $_REQUEST['filename'];
$entityBody = file_get_contents('php://input');
if ( file_put_contents($file, $entityBody, LOCK_EX + FILE_TEXT) ) {
    echo "File $file saved";
} else {
    echo "Error: could not write to $file";
}

?>
*/
