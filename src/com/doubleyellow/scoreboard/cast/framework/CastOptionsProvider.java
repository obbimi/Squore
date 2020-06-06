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

import androidx.annotation.NonNull;

import com.doubleyellow.scoreboard.Brand;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.framework.CastOptions;
import com.google.android.gms.cast.framework.OptionsProvider;
import com.google.android.gms.cast.framework.SessionProvider;
import com.google.android.gms.cast.framework.media.CastMediaOptions;
import com.google.android.gms.cast.framework.media.ImageHints;
import com.google.android.gms.cast.framework.media.ImagePicker;
import com.google.android.gms.cast.framework.media.MediaIntentReceiver;
import com.google.android.gms.cast.framework.media.NotificationOptions;
import com.google.android.gms.common.images.WebImage;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * New casting.
 *
 * This class is referenced from the Manifest file.
 * Lazily initialized (just once?) when
 * - CastContext.getSharedInstance(activity), or
 * - CastButtonFactory.setUpMediaRouteButton();
 * is invoked.
 */
public class CastOptionsProvider implements OptionsProvider
{
    private static final String TAG = "SB." + CastOptionsProvider.class.getSimpleName();

    @Override public CastOptions getCastOptions(Context context) {
        Map.Entry<String, String> remoteDisplayAppId2Info = Brand.brand.getRemoteDisplayAppId2Info(context);
        Log.d(TAG, "remoteDisplayAppId2Info : " + remoteDisplayAppId2Info);

        NotificationOptions notificationOptions = new NotificationOptions.Builder()
                .setActions(Arrays.asList(MediaIntentReceiver.ACTION_SKIP_NEXT,
                                          MediaIntentReceiver.ACTION_TOGGLE_PLAYBACK,
                                          MediaIntentReceiver.ACTION_STOP_CASTING), new int[]{1, 2})
                .setTargetActivityClassName(com.doubleyellow.scoreboard.activity.Feedback.class.getName())
                .build();

        CastMediaOptions mediaOptions = new CastMediaOptions.Builder()
                .setImagePicker(new ImagePickerImpl())
                .setNotificationOptions(notificationOptions)
                .setExpandedControllerActivityClassName(com.doubleyellow.scoreboard.activity.Feedback.class.getName())
                .build();

        CastOptions.Builder builder = new CastOptions.Builder();
        String key = remoteDisplayAppId2Info.getKey();
        builder.setReceiverApplicationId(key);
      //builder.setStopReceiverApplicationWhenEndingSession(true);
        builder.setCastMediaOptions(mediaOptions);
        CastOptions options = builder.build();
        return options;
    }

    @Override public List<SessionProvider> getAdditionalSessionProviders(Context context) {
        Log.d(TAG, "getAdditionalSessionProviders ... ?");
        return null;
    }

    private static class ImagePickerImpl extends ImagePicker {

        @Override public WebImage onPickImage(MediaMetadata mediaMetadata, @NonNull ImageHints imageHints) {
            if ((mediaMetadata == null) || !mediaMetadata.hasImages()) {
                return null;
            }
            List<WebImage> images = mediaMetadata.getImages();
            if (images.size() == 1) {
                return images.get(0);
            } else {
                if (imageHints.getType() == ImagePicker.IMAGE_TYPE_MEDIA_ROUTE_CONTROLLER_DIALOG_BACKGROUND) {
                    return images.get(0);
                } else {
                    return images.get(1);
                }
            }
        }
    }
}
