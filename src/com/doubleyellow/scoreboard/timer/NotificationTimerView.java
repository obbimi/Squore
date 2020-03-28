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

package com.doubleyellow.scoreboard.timer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.util.Log;
import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.model.Util;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.main.ScoreBoard;

/**
 * Helper class for showing a timer in devices notification area as soon as Squore app has moved into the background.
 * E.g. when device user switch to another app while warm-up/pause/injury timer was running.
 * Allows to quickly
 * - Check time left without re-opening Squore
 * - Switch back to Squore by clicking the notification timer
 */
public class NotificationTimerView implements TimerView
{
    private static final String TAG = "SB." + NotificationTimerView.class.getSimpleName();

    private static int m_iNotificationId;
    private        Context ctx = null;
    private Notification.Builder builder;

    public NotificationTimerView(Context context) {
        this.ctx = context;
        m_iNotificationId = Brand.getShortName(ctx).hashCode();

        // Prepare intent which is triggered if the notification is selected
        prepareBuilder();

        Log.i(TAG, "Created");
    }

    /** prepare builder with properties that do not change */
    private void prepareBuilder() {
        Intent intent = new Intent(ctx, ScoreBoard.class);
        PendingIntent pIntent = PendingIntent.getActivity(ctx, 0, intent, 0);

        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.O /*26 */ ) {
            createNotificationChannel(ctx);
            builder = new Notification.Builder(ctx, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(ctx);
        }
        builder.setSmallIcon(R.drawable.timer)
               .setContentIntent(pIntent);

        Bitmap bmIcon = Util.getAppIconAsBitMap(ctx);
        if ( bmIcon != null ) {
            builder.setLargeIcon(bmIcon);
        } else {
            if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M /* 23 */ ) {
                Icon icon = Icon.createWithResource(ctx, R.drawable.logo_brand_squore /*Brand.getLogoResId()*/);
                builder.setLargeIcon(icon);
            }
        }
    }

    @Override public void setTime(String s) {
        // update existing notification
        createUpdateNotification(ctx.getString(R.string.sb_timer), s);
    }

    @Override public void timeIsUp() {
        createUpdateNotification(ctx.getString(R.string.oa_time), "");
    }
    @Override public void cancel() {
        cancelNotification(ctx);
    }

    @Override public void setTitle(String s) { }
    @Override public void setTime(int iStartedCountDownAtSecs, int iSecsLeft, int iReminderAtSecs) { }
    @Override public void setWarnMessage(String s) { }
    @Override public void setPausedMessage(String s) { }
    @Override public void show() { }
    @Override public boolean isShowing() { return true; }

    private void createUpdateNotification(String sTitle, String sMess) {
        updateNotification(sTitle, sMess);
    }

    private void updateNotification(String sTitle, String sMess) {
        //Log.i(TAG, "Notification creation started ...");

        builder.setContentTitle(sTitle)
               .setContentText(sMess);

        Notification notification;
        notification = builder.build();
        notification.priority = Notification.PRIORITY_DEFAULT;

        // Hide the notification after its selected
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
      //notification.flags |= Notification.FLAG_LOCAL_ONLY;      // from API>=20 only
        notification.flags |= Notification.FLAG_ONLY_ALERT_ONCE; // e.g. for API 19... else big notification show on every update
        notification.flags |= Notification.FLAG_ONGOING_EVENT;   // e.g. for API 19... else big notification show on every update

        NotificationManager notificationManager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if ( notificationManager != null ) {
            notificationManager.notify(m_iNotificationId, notification);
        }

        //Log.i(TAG, "Notification created");
    }

    public static void cancelNotification(Context ctx) {
        NotificationManager notificationManager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if ( notificationManager != null ) {
            notificationManager.cancel(m_iNotificationId);
            notificationManager.cancelAll();
        }
    }

    // ----------------------------------------------------
    // --------------------- Android 8 Notifications ------
    // ----------------------------------------------------
    private final String CHANNEL_ID = ScoreBoard.class.getName();
    private void createNotificationChannel(Context ctx) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.O /*26 */ ) {
            NotificationManager notificationManager = ctx.getSystemService(NotificationManager.class);
            int iResIdAppName = PreferenceValues.getSportTypeSpecificResId(ctx, R.string.app_name_short_brand_Squash);
            NotificationChannel chDefault = new NotificationChannel(CHANNEL_ID, ctx.getString(iResIdAppName), NotificationManager.IMPORTANCE_LOW);
            //chDefault.setDescription(getString(R.string.channel_description));
            //chDefault.enableLights(true);
            //chDefault.setLightColor(Color.RED);
            //chDefault.setShowBadge(false);

            //chDefault.enableVibration(true);
            //chDefault.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});

            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            notificationManager.createNotificationChannel(chDefault);
        }
    }
}
