package com.doubleyellow.scoreboard.firebase;

import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.google.firebase.messaging.RemoteMessage;
import com.pusher.pushnotifications.fcm.MessagingService;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Required to receive messages even is app is in the background
 *
 * FCMMessageReceiver is a BroadCastReceiver that seems to receive the message if always
 *
 * D/MessagingService: Received from FCM: com.google.firebase.messaging.RemoteMessage@72bd541
 * D/MessagingService: Received from FCM TITLE: null
 * D/MessagingService: Received from FCM BODY: null
 * I/FCMMessageReceiver: Got a valid pusher message.
 **/
public class PusherMessagingService extends MessagingService
{
    private static final String TAG = "SB." + PusherMessagingService.class.getSimpleName();

    @Override public void onMessageReceived(@NotNull RemoteMessage remoteMessage) {
        Map<String, String> data = remoteMessage.getData();
        Log.d(TAG, "Message received in background service. Data " + data);
        if ( data.containsKey(FCMHandler.key_action) ) {
            String sAction = data.get(FCMHandler.key_action);
            try {
                PusherHandler instance = PusherHandler.getInstance();
                if ( instance.m_bInForegroundOnly == false ) {
                    instance.handleAction(sAction);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /** called when service has been declared in the AndroidManifest and used for the first time (e.g. first notification?! or when startService is called), device wakeup with app in foreground */
    @Override public void onCreate() {
        Log.i(TAG, "Service created");
        PusherHandler.getInstance().setInForegroundOnly(false);
        super.onCreate();
    }
    /** when device goes to sleep */
    @Override public void onDestroy() {
        Log.i(TAG, "Service destroyed");
        PusherHandler.getInstance().setInForegroundOnly(true);
        super.onDestroy();
    }

    /** invoked only if started with startService */
    @Override public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand. intent: " + intent);
        return super.onStartCommand(intent, flags, startId);
    }

    // =============================================
    // TEMP METHODS FOR LOGGING ONLY
    // =============================================

    /** invoked on message received (first one only!) */
    public IBinder onBind(Intent arg) {
        Log.i(TAG, "onBind() : " + arg );
        return super.onBind(arg);
    }

    /** ?? */
    public boolean onUnbind(Intent arg) {
        Log.i(TAG, "onUnBind() : " + arg);
        return super.onUnbind(arg);
    }
}
