package com.doubleyellow.scoreboard.cast.framework;

import android.content.Context;

import com.doubleyellow.scoreboard.Brand;
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
        String sAppID = context.getString(Brand.brand.getRemoteDisplayAppIdResId());
        return builder.setReceiverApplicationId(sAppID)
             .build();
    }

    @Override public List<SessionProvider> getAdditionalSessionProviders(Context context) {
        return null;
    }
}
