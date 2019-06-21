package com.doubleyellow.scoreboard.cast.framework;

import android.content.Context;

import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.google.android.gms.cast.framework.CastOptions;
import com.google.android.gms.cast.framework.OptionsProvider;
import com.google.android.gms.cast.framework.SessionProvider;

import java.util.List;

/**
 * New casting.
 *
 * This class is referenced from the Manifest file.
 */
public class CastOptionsProvider implements OptionsProvider
{
    @Override public CastOptions getCastOptions(Context context) {
        CastOptions.Builder builder = new CastOptions.Builder();
        int remoteDisplayAppIdResId = Brand.brand.getRemoteDisplayAppIdResId();
        if ( PreferenceValues.isBrandTesting(context) ) {
            remoteDisplayAppIdResId = R.string.CUSTOM_RECEIVER_APP_ID_brand_test;
        }
        String sAppID = context.getString(remoteDisplayAppIdResId);
        return builder.setReceiverApplicationId(sAppID)
             .build();
    }

    @Override public List<SessionProvider> getAdditionalSessionProviders(Context context) {
        return null;
    }
}
