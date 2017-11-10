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

    public NotificationTimerView(Context context) {
        this.ctx = context;
        Log.i(TAG, "Created");
    }
    @Override public void setTime(String s) {
        // update existing notification
        createUpdateNotification(ctx.getString(R.string.sb_timer), s);
        m_iNotificationId = Brand.getShortName(ctx).hashCode();
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
        createNotification(ctx, R.drawable.timer, sTitle, sMess);
    }

    public static void cancelNotification(Context ctx) {
        NotificationManager notificationManager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(m_iNotificationId);
    }
    private static void createNotification(Context ctx, int iSmallIconId, String sTitle, String sUpdated) {
        //Log.i(TAG, "Notification creation started ...");

        // Prepare intent which is triggered if the notification is selected
        Intent intent = new Intent(ctx, ScoreBoard.class);
        PendingIntent pIntent = PendingIntent.getActivity(ctx, 0, intent, 0);

        Notification.Builder builder = new Notification.Builder(ctx)
                .setContentTitle(sTitle)
                .setContentText(sUpdated)
                .setSmallIcon(iSmallIconId)
                .setContentIntent(pIntent);
        Bitmap bmIcon = ScoreBoard.getAppIconAsBitMap(ctx);
        if ( bmIcon != null) {
            builder.setLargeIcon(bmIcon);
        } else {
            if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ) {
                Icon icon = Icon.createWithResource(ctx, R.drawable.logo_brand_squore /*Brand.getLogoResId()*/);
                builder.setLargeIcon(icon);
            }
        }
        Notification notification;
        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN ) {
            notification = builder.build();
        } else {
            notification = builder.getNotification();
        }
        NotificationManager notificationManager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        // Hide the notification after its selected
        notification.flags |= Notification.FLAG_AUTO_CANCEL;

        notificationManager.notify(m_iNotificationId, notification);

        //Log.i(TAG, "Notification created");
    }
}
