/*
 * Copyright (C) 2019  Iddo Hoeve
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
package com.doubleyellow.scoreboard.cast.framework;

import android.content.Context;
import android.util.Log;

import com.doubleyellow.scoreboard.Brand;
import com.google.android.gms.cast.framework.CastOptions;
import com.google.android.gms.cast.framework.OptionsProvider;
import com.google.android.gms.cast.framework.SessionProvider;

import java.util.List;
import java.util.Map;

/**
 * New casting.
 *
 * This class is referenced from the Manifest file.
 * Lazily initialized just once when
 * - CastContext.getSharedInstance(activity), or
 * - CastButtonFactory.setUpMediaRouteButton();
 * is invoked.
 */
public class CastOptionsProvider implements OptionsProvider
{
    private static final String TAG = "SB." + CastOptionsProvider.class.getSimpleName();

    @Override public CastOptions getCastOptions(Context context) {
        Map.Entry<String, String> remoteDisplayAppId2Info = Brand.brand.getRemoteDisplayAppId2Info(context);
        Log.d(TAG, "remoteDisplayAppId2Info:" + remoteDisplayAppId2Info);

        CastOptions.Builder builder = new CastOptions.Builder();
        builder.setReceiverApplicationId(remoteDisplayAppId2Info.getKey());
      //builder.setStopReceiverApplicationWhenEndingSession(true);
        CastOptions options = builder.build();
        return options;
    }

    @Override public List<SessionProvider> getAdditionalSessionProviders(Context context) {
        return null;
    }
}
