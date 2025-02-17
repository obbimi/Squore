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

package com.doubleyellow.scoreboard.activity;

//import android.app.Activity;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;
import com.doubleyellow.android.task.URLTask;
import com.doubleyellow.android.view.ViewUtil;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;

public class ScoreSheetOnline extends XActivity {

    private String sShowURL = null;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ViewUtil.setFullScreen(getWindow(), PreferenceValues.showFullScreen(this));

        Intent intent = getIntent();
        Bundle bundleExtra = intent.getBundleExtra(IntentKeys.ScoreSheetOnline.toString());
        sShowURL = bundleExtra.getString(IntentKeys.ScoreSheetOnline.toString());

        WebView wv = new WebView(this); // throws UnsupportedOperationException on wearable
        setContentView(wv);

        WebSettings settings = wv.getSettings();
        //settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        //settings.setUseWideViewPort(true);
        //settings.setLoadWithOverviewMode(true);
        //webView.addJavascriptInterface(new WebViewJavascript(this), "Android");

        // https://groups.google.com/forum/#!topic/tasker/b79YnD5CnVw says
        settings.setJavaScriptEnabled(true); // enable javascript
        settings.setDomStorageEnabled(true);

        // specifically adding the 'Android' string for jquery framework
        String myUserAgentString = URLTask.getMyUserAgentString(this);
        settings.setUserAgentString("Android WebView " + myUserAgentString);
/*
        settings.setBlockNetworkLoads(false);
*/
        settings.setLoadsImagesAutomatically(true);

        wv.loadUrl(sShowURL);
        // This next one is strange. It's the default location for the app's cache. But it didn't work for me without this.
/*
        String appCachePath = getApplicationContext().getCacheDir().getAbsolutePath();
        settings.setAppCachePath(appCachePath);
        settings.setAllowFileAccess(true);
*/
    }

    /** Populates the activity's options menu. */
    @Override public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.scoresheetonline, menu);

        return true;
    }

    /** Handles the user's menu selection. */
    @Override public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home || itemId == R.id.close) {
            this.finish();
        } else if (itemId == R.id.share) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, sShowURL);
            this.startActivity(Intent.createChooser(intent, this.getString(R.string.cmd_share_with_friends)));
        }
        return true;
    }
}
