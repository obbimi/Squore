package com.doubleyellow.scoreboard.activity;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import com.doubleyellow.android.view.ViewUtil;
import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.prefs.ColorPrefs;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;

import java.util.*;

/**
 * Activity to allow user to give feedback about the app.
 */
public class Feedback extends XActivity implements View.OnClickListener, View.OnLongClickListener
{
    private static final String TAG = "SB" + Feedback.class.getSimpleName();

    @Override public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        ScoreBoard.initAllowedOrientation(this);

        setContentView(R.layout.feedback);

        ViewUtil.setFullScreen(getWindow(), PreferenceValues.showFullScreen(this));

        View root = findViewById(R.id.ll_feedback);
        ColorPrefs.setColors(this, root);

        int[] iaButtons = new int[] { R.id.cmd_yes_i_like
                                    , R.id.cmd_no_there_is_a_problem
                                    , R.id.cmd_rate_the_app
                                    , R.id.cmd_share_with_friends
                                    , R.id.cmd_like_on_facebook
                                    , R.id.cmd_email_the_developer
                                    };
        for(int id: iaButtons) {
            View btn = findViewById(id);
            if ( btn == null ) { continue; }
            btn.setOnClickListener(this);
        }

        findViewById(R.id.ll_feedback)             .setVisibility(View.VISIBLE);
        findViewById(R.id.ll_no_there_is_a_problem).setVisibility(View.GONE);
        findViewById(R.id.ll_yes_i_like           ).setVisibility(View.GONE);

        findViewById(R.id.cmd_no_there_is_a_problem).setOnLongClickListener(this);
        findViewById(R.id.cmd_yes_i_like           ).setOnLongClickListener(this);
    }

    @Override public boolean onLongClick(View view) {
        String sMsg = getString(R.string.setting_resource_folder) + "\n" + getResources().getDisplayMetrics();
        sMsg +=" DeviceDefaultOrientation:" + ViewUtil.getDeviceDefaultOrientation(this);
        Toast.makeText(this, sMsg, Toast.LENGTH_SHORT).show();
        return false;
    }

    @Override public void onClick(View view) {
        String sMarketURL = "market://details"                           + "?id=" + this.getPackageName();
        String sPlayURL   = "https://play.google.com/store/apps/details" + "?id=" + this.getPackageName();

        view.setEnabled(false);
        switch (view.getId()) {
            case R.id.cmd_yes_i_like:
                findViewById(R.id.ll_do_you_like          ).setVisibility(View.GONE);
                findViewById(R.id.ll_yes_i_like           ).setVisibility(View.VISIBLE);
                break;
            case R.id.cmd_no_there_is_a_problem:
                findViewById(R.id.ll_do_you_like          ).setVisibility(View.GONE);
                findViewById(R.id.ll_no_there_is_a_problem).setVisibility(View.VISIBLE);
                break;
            case R.id.cmd_rate_the_app:
                try {
                    Uri uri = Uri.parse(sMarketURL);
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(this, "Could not launch Play Store!", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.cmd_share_with_friends: {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                // in facebook only the URL is actually taken from the extra text
                intent.putExtra(Intent.EXTRA_TEXT, "Squore - Squash Ref Tool\n" + sPlayURL); // market url does not work in facebook (the most likely target)
                startActivity(Intent.createChooser(intent, getString(R.string.cmd_share_with_friends)));
                break;
            }
            case R.id.cmd_like_on_facebook: {
                Uri url = getLinkToFacebook(this);
                startActivity(new Intent(Intent.ACTION_VIEW, url));

/*
                Intent facebookAppIntent = null;
                try {
                    //facebookAppIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("fb://page/335853576565939"));
                    startActivity(facebookAppIntent);
                } catch (Exception e) {
                    facebookAppIntent = new Intent(Intent.ACTION_VIEW, parse);
                    startActivity(facebookAppIntent);
                }
*/
                break;
            }
            case R.id.cmd_email_the_developer:
                StringBuilder sbMsg = new StringBuilder();

                // put user settings in the email
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                sbMsg.append("========== Debug Info =========================\n");
                try {
                    PackageInfo info = getPackageManager().getPackageInfo(this.getPackageName(), 0);

                    sbMsg.append("App Version: ").append(info.versionName).append(" (").append(info.versionCode).append(")");
                    sbMsg.append("\n");
                    sbMsg.append("Device API Level: ").append(Build.VERSION.SDK_INT);
                    sbMsg.append("\n");
                    sbMsg.append("Device: ").append(Build.MANUFACTURER).append(" ").append(Build.MODEL).append(" ").append(Build.BRAND).append(" ").append(Build.VERSION.RELEASE);
                    sbMsg.append("\n");
                } catch (Exception e) { }
                sbMsg.append("Screen Size HxW: ").append(ViewUtil.getScreenHeightWidthMaximum(this)).append(" x ").append(ViewUtil.getScreenHeightWidthMinimum(this)).append("\n");
                sbMsg.append("DeviceDefaultOrientation:").append(ViewUtil.getDeviceDefaultOrientation(this)).append("\n");
                sbMsg.append("Resource folder: ").append(getString(R.string.setting_resource_folder)).append("\n");

                DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
                sbMsg.append(displayMetrics.toString()).append("\n"); // string like DisplayMetrics{density=1.5, width=480, height= 800, scaledDensity=1.2750001, xdpi=217.713  , ydpi=207.347}
                sbMsg.append("========== Features ===========================\n");
                final FeatureInfo[] featuresList = getPackageManager().getSystemAvailableFeatures();
                for (FeatureInfo f : featuresList) {
                    sbMsg.append(f.name).append("\n");
                }
                sbMsg.append("========== Settings ========================\n");
                Map<String, ?> all = prefs.getAll();
                Set<String> keys = all.keySet();
                SortedSet<String> keysSorted = new TreeSet<String>(keys);
                for(String key: keysSorted) {
                    Object val = all.get(key);
                    if ( key.toLowerCase().matches(".*password$")) {
                        val = "******"; // do not send any passwords in the feedback mail: would piss off the user
                    }
                    if ( key.endsWith("List") ) { // EventList RoundList PlayerList MatchList
                        val = String.format("[list of length %s]", String.valueOf(val).split("\n").length);
                    }
                    sbMsg.append("\n|").append(key).append(":").append(val);
                }
                sbMsg.append("\n");
                sbMsg.append("Become a tester: https://play.google.com/apps/testing/com.doubleyellow.scoreboard");
                sbMsg.append("\n");
                sbMsg.append("===============================================\n");

                String sendMatchResultTo = "iddo.hoeve@gmail.com";
                String sSubject = Brand.getShortName(this) + " Feedback";

                Log.i("SB.Feedback", sbMsg.toString());

                try {
                    Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", sendMatchResultTo, null));
                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, sSubject);
                    emailIntent.putExtra(Intent.EXTRA_TEXT, sbMsg.toString());

                    startActivity(Intent.createChooser(emailIntent, getString(R.string.cmd_feedback)));
                } catch (Exception e) {
                    //Log.e(TAG, "Starting sms or call failed");
                    Toast.makeText(this, "Could not launch email intent!", Toast.LENGTH_SHORT).show();
                }
                break;
        }
        view.setEnabled(true);
    }

    public static Uri getLinkToFacebook(Context context) {
        Uri url = Uri.parse("https://www.facebook.com/SquoreSquashRefTool");
        try {
            ApplicationInfo applicationInfo = context.getPackageManager().getApplicationInfo("com.facebook.katana", 0);
            if (applicationInfo.enabled) {
                // http://stackoverflow.com/a/24547437/1048340
                url = Uri.parse("fb://facewebmodal/f?href=" + url); // this only seems to work for https://www.facebook address (not e.g. http://facebook)
            }
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        Log.d(TAG, "Facebook URL " + url);
        return url;
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        //MenuInflater inflater = getMenuInflater();
        //inflater.inflate(R.menu.closeonlymenu, menu);  // we provide the 'Up' button via PARENT_ACTIVITY in AndroidManifest
        return true;
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        this.finish();
        return true;
    }

/*
    @Override protected void onResume() {
        super.onResume(); // 3
    }
    @Override protected void onRestart() {
        super.onRestart();
    }
    @Override protected void onStart() {
        super.onStart(); // 2 (after onCreate())
    }
*/

}
