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
        int remoteDisplayAppIdResId = Brand.brand.getRemoteDisplayAppIdResId();
        if ( PreferenceValues.isBrandTesting(context) ) {
            remoteDisplayAppIdResId = R.string.CUSTOM_RECEIVER_APP_ID_brand_test;
        }
        String sAppID = context.getString(remoteDisplayAppIdResId);

        return (new CastOptions.Builder())
                .setReceiverApplicationId(sAppID)
                .build()
                ;
    }

    @Override public List<SessionProvider> getAdditionalSessionProviders(Context context) {
        return null;
    }
}
