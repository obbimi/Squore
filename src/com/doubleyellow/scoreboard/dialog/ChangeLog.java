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

package com.doubleyellow.scoreboard.dialog;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.activity.Feedback;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.android.view.MarkDownView;

public class ChangeLog extends BaseAlertDialog
{
    public ChangeLog(Context context) {
        super(context, null, null);
    }

    @Override public boolean storeState(Bundle outState) {
        return true;
    }

    @Override public boolean init(Bundle outState) {
        return true;
    }

    @Override public void show() {
        LayoutInflater myLayout = LayoutInflater.from(context);
        MarkDownView webView = (MarkDownView) myLayout.inflate(R.layout.changelog, null);

        int iOtherResId = Brand.getChangeLogResId();
        if ( iOtherResId != R.raw.changelog ) {
            webView.init(iOtherResId);
        }

        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, String sUrl) {
                Uri uri = Uri.parse(sUrl);
                if (uri.getScheme().equals("market")) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(uri);
                        context.startActivity(intent);
                        return true;
                    } catch (ActivityNotFoundException e) {
                        // Google Play app is not installed, you may want to open the app store link
                        view.loadUrl("http://play.google.com/store/apps/" + uri.getHost() + "?" + uri.getQuery());
                        return false;
                    }
                } else if ( sUrl.contains("facebook") && sUrl.contains("Squore") ) {
                    Uri url = Feedback.getLinkToFacebook(context);
                    context.startActivity(new Intent(Intent.ACTION_VIEW, url));
                    return true;
                } else if ( sUrl.toLowerCase().contains("squore.") || sUrl.toLowerCase().contains("racketlon.") || sUrl.toLowerCase().contains("tabletennis.") ) {
                    // url to the squore server
                    context.startActivity(new Intent(Intent.ACTION_VIEW, uri));
                    return true;
                } else {
                    return super.shouldOverrideUrlLoading(view, sUrl);
                }
            }
        });

        //MarkDownView webView = new MarkDownView(context, R.raw.changelog);
/*
        LinearLayout ll = new LinearLayout(context);
        ll.setOrientation(LinearLayout.VERTICAL);

        Button btnOK = new Button(context);
        btnOK.setText(R.string.cmd_ok);
        btnOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });
        ll.addView(btnOK);
        ll.addView(webView);
        //ll.addView(btnOK);
*/

        dialog = adb
                .setTitle("Changelog")
                .setIcon(android.R.drawable.ic_menu_info_details)
                .setView(webView)
                .setPositiveButton(R.string.cmd_ok, null)
                .setOnKeyListener(getOnBackKeyListener())
                .show();
    }

    @Override public void handleButtonClick(int which) {
        super.handleButtonClick(which);
        if ( scoreBoard == null ) { return; } // only happens if you already press back while changelog is already being loaded?!
        scoreBoard.triggerEvent(ScoreBoard.SBEvent.dialogClosed, this);
    }
}
