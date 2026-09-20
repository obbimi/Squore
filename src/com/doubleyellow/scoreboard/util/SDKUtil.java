package com.doubleyellow.scoreboard.util;

import android.app.Activity;
import android.os.Build;
import android.view.View;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SDKUtil {
    private SDKUtil() {}

    public static void doSdk36FixForActionBar(Activity activity) {
        if ( Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA /* 36 */ ) {
            return;
        }

        // attempt at solving
        // https://medium.com/@dileepapeiris5/resolve-layout-overlap-issues-after-upgrading-to-android-target-sdk-35-required-by-google-from-cd6c5f18fa25
        final View rootView = activity.findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            int typeMask = WindowInsetsCompat.Type.navigationBars() | WindowInsetsCompat.Type.statusBars();
            androidx.core.graphics.Insets innerPadding = insets.getInsets(typeMask);
            rootView.setPadding(innerPadding.left, innerPadding.top, innerPadding.right, innerPadding.bottom);
            return insets;
        });
    }

}
