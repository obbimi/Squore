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

package com.doubleyellow.scoreboard.cast;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.view.MenuItemCompat;
import androidx.mediarouter.app.MediaRouteActionProvider;
import androidx.mediarouter.media.MediaRouteSelector;
import androidx.mediarouter.media.MediaRouter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import com.doubleyellow.prefs.RWValues;
import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.prefs.ColorPrefs;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.scoreboard.timer.TimerView;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.cast.CastRemoteDisplayLocalService;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.Status;

import java.util.Map;

/**
 * TO switch between casting
 *
 * - AndroidManifest
 *      - <application @style/Theme.AppCompat  --> @style/SBTheme
 *
 *              <service android:name=".cast.PresentationService" android:exported="false" />
 *              <meta-data android:name="com.google.android.gms.version" android:value="@integer/google_play_services_version" />
 *
 * build.gradle
 *
 *      compile 'com.google.android.gms:play-services-cast:8.4.0'
 *      exclude '* * /cast / * *'
 *
 * ScoreBoard.java
 *
 *      - import android.support.v7.app.AppCompatActivity;
 *      - comment in/out 5 lines with castHelper
 *      - toggle return code in method 'getSquoreActionBar()'
 *
 * res/menu/mainmenu.xml
 *     ChromeCast
 *         xmlns:app="http://schemas.android.com/apk/res-auto"
 *
 *     No ChromeCast
 *         xmlns:app="http://schemas.android.com/apk/res/android"
 *
 * Device                    | Android Version | Google Cast | Squore |
 * ====================================================================
 * samsung s4                | 5.1.1 cm12.1    | OK          | black
 * samsung s2                | 4.4.4 aosb      | NO Conn     | No Conn
 * ====================================================================
 * samsung s4                | 4.4.4 aosb      | OK          | OK
 * samsung J5                | 5.1.1           | ??          | OK
 * samsung s5                | 5.0             | ??          | OK
 * p5110-samsung-tab2.10.1   | 4.4.4 Slim Rom  | ??          | OK
 * ====================================================================
 *
 * Alle apparaten met Android 4.4.2 of hoger bieden ondersteuning voor de functie 'Scherm casten'.
 * Let op: Sommige apparaten zijn geoptimaliseerd voor het casten van schermen. De functionaliteit kan daarom verschillen
 *
 * https://support.google.com/chromecast/answer/6293757
 * http://www.androidpolice.com/2016/03/16/cyanogenmod-bug-broke-chromecast-video-streaming-finally-fixed/
 *
 * https://developers.google.com/android/guides/setup#ensure_devices_have_the_google_play_services_apk
 *
 * @deprecated
 */
public class CastHelper implements ICastHelper
{
    private static final String TAG = "SB.OLD." + CastHelper.class.getSimpleName();

    private static final boolean NOT_SUPPORTED_IN_SDK = false;

    private MediaRouter          mediaRouter         = null;
    private MediaRouteSelector   mediaRouteSelector  = null;
    private MediaRouter.Callback mediaRouterCallback = null;

    private static String APP_ID = null;

    public CastHelper() {
        Log.w(TAG, "Old way of casting... discouraged by Google");
    }

    @Override public void initCasting(Activity activity) {
        if (NOT_SUPPORTED_IN_SDK) { return; }

        Map.Entry<String, String> remoteDisplayAppId2Info = Brand.brand.getRemoteDisplayAppId2Info(activity);
        String sNewAppId = remoteDisplayAppId2Info.getKey();
        if ( (APP_ID != null) && APP_ID.equals(sNewAppId) ) {
            Log.w(TAG, "Changing of APP_ID: from  " + APP_ID + " to " + sNewAppId);
        }
        APP_ID = sNewAppId;

        mediaRouterCallback = new MediaRouterCallback(activity);

        mediaRouter = MediaRouter.getInstance(activity);
        String category = CastMediaControlIntent.categoryForCast(CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID); // com.google.android.gms.cast.CATEGORY_CAST/CC1AD845///ALLOW_IPV6
        //       category = CastMediaControlIntent.categoryForCast(APP_ID);
        mediaRouteSelector = new MediaRouteSelector.Builder()
                .addControlCategory(category)
                .build();
    }

    @Override public void initCastMenu(Activity activity, Menu menu) {
        if (NOT_SUPPORTED_IN_SDK) { return; }

        MenuItem mediaRouteMenuItem = menu.findItem(R.id.media_route_menu_item);
        mediaRouteMenuItem.setVisible(true);
        mediaRouteMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        MediaRouteActionProvider mediaRouteActionProvider = (MediaRouteActionProvider) MenuItemCompat.getActionProvider(mediaRouteMenuItem);
        mediaRouteActionProvider.setRouteSelector(mediaRouteSelector);
    }

    @Override public void onActivityPause_Cast() {

    }

    @Override public void onActivityResume_Cast() {
        if (NOT_SUPPORTED_IN_SDK) { return; }
        if (mediaRouter == null) { return; }

        mediaRouter.addCallback(mediaRouteSelector, mediaRouterCallback, MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
    }

    @Override public void onActivityStart_Cast() {
        if (NOT_SUPPORTED_IN_SDK) { return; }
        if (mediaRouter == null) { return; }

        mediaRouter.addCallback(mediaRouteSelector, mediaRouterCallback, MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
    }

    @Override public void onActivityStop_Cast() {
        if (NOT_SUPPORTED_IN_SDK) { return; }
        if (mediaRouter == null) { return; }
        mediaRouter.removeCallback(mediaRouterCallback);
    }
    @Override public boolean isCasting() {
        if (NOT_SUPPORTED_IN_SDK) { return false; }
        return PresentationService.isCasting();
    }
    @Override public void setModelForCast(Model matchModel) {
        if (NOT_SUPPORTED_IN_SDK) { return; }
        PresentationService.setModel(matchModel);
    }
    @Override public TimerView getTimerView() {
        if (NOT_SUPPORTED_IN_SDK) { return null; }
        return PresentationService.getTimerView();
    }
    @Override public void castColors(Map<ColorPrefs.ColorTarget, Integer> mColors) {
        if (NOT_SUPPORTED_IN_SDK) { return; }
        PresentationService.refreshColors(mColors);
    }
    @Override public void castDurationChronos() {
        if (NOT_SUPPORTED_IN_SDK) { return; }
        PresentationService.refreshDurationChronos();
    }
    @Override public void castGamesWonAppearance() {
        if (NOT_SUPPORTED_IN_SDK) { return; }
        PresentationService.refreshGamesWonAppearance();
    }

    private static class MediaRouterCallback extends androidx.mediarouter.media.MediaRouter.Callback
    {
        private Activity activity = null;
        private MediaRouterCallback(Activity activity) {
            this.activity = activity;
        }
        /** Invoked as soon as a cast device is selected from the menu */
        @Override public void onRouteSelected(MediaRouter router, MediaRouter.RouteInfo route) {
            Log.d(TAG, "Route selected :" + route.getName());
            if ( RWValues.Permission.Granted.equals(PreferenceValues.doesUserHavePermissionToCast(activity, route.getName(), true)) == false ) {
                return;
            }
            GoogleApiAvailability instance = GoogleApiAvailability.getInstance(); // e.g. Living or Court 1. If e.g. Netflix is playing on the device: mDescription=Netflix
            int iResult = instance.isGooglePlayServicesAvailable(activity);
            switch (iResult) {
                case com.google.android.gms.common.ConnectionResult.SUCCESS:
                    CastDevice device = CastDevice.getFromBundle(route.getExtras());
                    runRemoteDisplayService(activity, device);
                    break;
                default:
                    Dialog errorDialog = instance.getErrorDialog(activity, iResult, 0);
                    errorDialog.show(); // TODO: message is not appropriate
                    //errorDialog.mAlert.mMessage;
                    break;
            }

            //setSelectedDevice(device);
        }
        @Override public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo info) {
            CastRemoteDisplayLocalService.stopService();
            //teardown();
            //mSelectedDevice = null;
        }

/*
        @Override public void onRouteAdded(MediaRouter router, MediaRouter.RouteInfo route) {
            Log.d(TAG, "Route added :" + route.getName());
            super.onRouteAdded(router, route);
        }

        @Override public void onRouteRemoved(MediaRouter router, MediaRouter.RouteInfo route) {
            Log.d(TAG, "Route removed :" + route.getName());
            super.onRouteRemoved(router, route);
        }
*/
    }

    private static void runRemoteDisplayService(Context context, CastDevice selectedDevice) {
        Intent intent = new Intent(context, context.getClass());
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent notificationPendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE); // PendingIntent.FLAG_IMMUTABLE added for targetSdkVersion >= 31

        CastRemoteDisplayLocalService.NotificationSettings.Builder builder = new CastRemoteDisplayLocalService.NotificationSettings.Builder();
        CastRemoteDisplayLocalService.NotificationSettings settings = builder.setNotificationPendingIntent(notificationPendingIntent).build();

        CastRemoteDisplayLocalService.Callbacks callbacks = new CastRemoteDisplayLocalService.Callbacks() {
            @Override public void onRemoteDisplaySessionStarted(CastRemoteDisplayLocalService service) {
                // initialize sender UI
                Log.d(TAG, "onRemoteDisplaySessionStarted " + service);
            }

            @Override public void onRemoteDisplaySessionError(Status errorReason) {
                //initError();
                Log.w(TAG, "onRemoteDisplaySessionError " + errorReason); // 20210628: Status{statusCode=unknown status code: 2202, resolution=null}
            }

            @Override public void onServiceCreated(CastRemoteDisplayLocalService castRemoteDisplayLocalService) {
                Log.d(TAG, "onServiceCreated " + castRemoteDisplayLocalService);
            }

            @Override public void onRemoteDisplaySessionEnded(CastRemoteDisplayLocalService castRemoteDisplayLocalService) {
                Log.d(TAG, "onRemoteDisplaySessionEnded " + castRemoteDisplayLocalService);
            }

            // since com.google.android.gms:play-services-cast-framework:21.0.0
            public void onRemoteDisplayMuteStateChanged(boolean b) {
                Log.d(TAG, "onRemoteDisplayMuteStateChanged " + b);
            }
        };

        try {
            CastRemoteDisplayLocalService.startService(context, PresentationService.class, APP_ID, selectedDevice, settings, callbacks);
        } catch (java.lang.IllegalStateException e) {
            // play console reported this error on 20190728 for app version 217 (android 6 AND 9)
            e.printStackTrace();
        }
    }

/*
    private GoogleApiClient apiClient;
    private boolean applicationStarted;

    private void setSelectedDevice(CastDevice device)
    {
        Log.d(TAG, "setSelectedDevice: " + device);

        selectedDevice = device;

        if (selectedDevice != null) {
            try {
                stopApplication();
                disconnectApiClient();
                connectApiClient();
            } catch (IllegalStateException e) {
                Log.w(TAG, "Exception while connecting API client", e);
                disconnectApiClient();
            }
        } else {
            if (apiClient != null) {
                disconnectApiClient();
            }

            mediaRouter.selectRoute(mediaRouter.getDefaultRoute());
        }
    }

    // ========================
    // Launching the receiver
    // ========================

    private void connectApiClient()
    {
        Cast.CastOptions apiOptions = Cast.CastOptions.builder(selectedDevice, castClientListener).build();
        apiClient = new GoogleApiClient.Builder(this)
                .addApi(Cast.API, apiOptions)
                .addConnectionCallbacks(connectionCallback)
                .addOnConnectionFailedListener(connectionFailedListener)
                .build();
        apiClient.connect();
    }

    private void disconnectApiClient()
    {
        if (apiClient != null) {
            apiClient.disconnect();
            apiClient = null;
        }
    }

    private void stopApplication()
    {
        if (apiClient == null) return;

        if (applicationStarted) {
            Cast.CastApi.stopApplication(apiClient);
            applicationStarted = false;
        }
    }

    private class ConnectionCallbacks implements GoogleApiClient.ConnectionCallbacks {
        boolean mWaitingForReconnect = false;
        @Override public void onConnected(Bundle connectionHint) {
            if (mWaitingForReconnect) {
                mWaitingForReconnect = false;
                reconnectChannels();
            } else {
                try {
                    Cast.CastApi.launchApplication(mApiClient, "YOUR_APPLICATION_ID", false)
                            .setResultCallback(
                                    new ResultCallback<Cast.ApplicationConnectionResult>() {
                                        @Override
                                        public void onResult(Cast.ApplicationConnectionResult result) {
                                            Status status = result.getStatus();
                                            if (status.isSuccess()) {
                                                ApplicationMetadata applicationMetadata = result.getApplicationMetadata();
                                                String sessionId = result.getSessionId();
                                                String applicationStatus = result.getApplicationStatus();
                                                boolean wasLaunched = result.getWasLaunched();
                                            } else {
                                                teardown();
                                            }
                                        }
                                    });

                } catch (Exception e) {
                    Log.e(TAG, "Failed to launch application", e);
                }
            }
        }

        @Override public void onConnectionSuspended(int cause) {
            mWaitingForReconnect = true;
        }
    }
    private class ConnectionFailedListener implements GoogleApiClient.OnConnectionFailedListener {
        @Override public void onConnectionFailed(ConnectionResult result) {
            teardown();
        }
    }

    private final Cast.Listener castClientListener = new Cast.Listener()
    {
        @Override public void onActiveInputStateChanged(int activeInputState) {
        }
        @Override public void onApplicationDisconnected(int statusCode) {
        }

        @Override public void onVolumeChanged() {
        }
    };

    private final GoogleApiClient.ConnectionCallbacks connectionCallback = new GoogleApiClient.ConnectionCallbacks()
    {
        @Override public void onConnected(Bundle bundle)
        {
            try {
                Cast.CastApi.launchApplication(apiClient, APP_ID, false).setResultCallback(connectionResultCallback);
            } catch (Exception e) {
                Log.e(TAG, "Failed to launch application", e);
            }
        }

        @Override public void onConnectionSuspended(int i) {
        }
    };

    private final GoogleApiClient.OnConnectionFailedListener connectionFailedListener = new GoogleApiClient.OnConnectionFailedListener()
    {
        @Override public void onConnectionFailed(ConnectionResult connectionResult) {
            setSelectedDevice(null);
        }
    };

    private final ResultCallback connectionResultCallback = new ResultCallback()
    {
        @Override public void onResult(@NonNull Result result) {
            Status status = result.getStatus();
            if (status.isSuccess()) {
                applicationStarted = true;
            }
        }
    };
*/
}
