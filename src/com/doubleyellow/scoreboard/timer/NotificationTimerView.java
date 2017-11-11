package com.doubleyellow.scoreboard.timer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.util.Log;
import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.main.ScoreBoard;

public class NotificationTimerView implements TimerView
{
    private static final String TAG = "SB." + NotificationTimerView.class.getSimpleName();

    private static int m_iNotificationId = "Squore".hashCode();
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

        builder = new Notification.Builder(ctx);
        builder.setSmallIcon(R.drawable.timer)
               .setContentIntent(pIntent);

        Bitmap bmIcon = ScoreBoard.getAppIconAsBitMap(ctx);
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
        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN /* 16 */ ) {
            notification = builder.build();
            notification.priority = Notification.PRIORITY_DEFAULT;
        } else {
            notification = builder.getNotification();
        }
        // Hide the notification after its selected
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
      //notification.flags |= Notification.FLAG_LOCAL_ONLY; // from API>=20 only
        notification.flags |= Notification.FLAG_ONLY_ALERT_ONCE; // e.g. for apk 19... else big notification show on every update
        notification.flags |= Notification.FLAG_ONGOING_EVENT; // e.g. for apk 19... else big notification show on every update

        NotificationManager notificationManager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(m_iNotificationId, notification);

        //Log.i(TAG, "Notification created");
    }

    public static void cancelNotification(Context ctx) {
        NotificationManager notificationManager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(m_iNotificationId);
        notificationManager.cancelAll();
    }
}
