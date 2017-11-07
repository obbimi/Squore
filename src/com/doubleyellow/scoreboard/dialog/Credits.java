package com.doubleyellow.scoreboard.dialog;

import android.content.Context;
import android.os.Bundle;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.view.MarkDownView;

public class Credits extends BaseAlertDialog
{
    public Credits(Context context) {
        super(context, null, null);
    }

    @Override public boolean storeState(Bundle outState) {
        return true;
    }

    @Override public boolean init(Bundle outState) {
        return true;
    }

    @Override public void show() {
        MarkDownView webView = new MarkDownView(context, null);
        webView.init(R.raw.credits);

        dialog = adb
                .setTitle("Credits")
                .setIcon(android.R.drawable.ic_menu_info_details)
                .setView(webView)
                .setPositiveButton(R.string.cmd_ok, null)
                .show();
    }
}
