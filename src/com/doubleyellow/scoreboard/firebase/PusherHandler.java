package com.doubleyellow.scoreboard.firebase;

import android.util.Log;

import com.doubleyellow.scoreboard.bluetooth.MessageSource;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.RemoteMessage;
import com.pusher.pushnotifications.PushNotificationReceivedListener;
import com.pusher.pushnotifications.PushNotifications;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

/*
 * https://console.firebase.google.com/project/squore-76ed0
 *
 * A Beam in Pusher itself needs the 'Server key' from the project in firebase.
 * (Project Settings > Cloud Messaging > Project Credentials)
 *
 * In firebase each app (package) has its own entry in the
 * - google-services.json
 *
 * In Pusher each app can have its own Beam, but a beam can also be shared .
 *   https://dashboard.pusher.com/beams/instances
 * If a beam is shared, the 'interest' must definitely be different or both apps will receive the same messages
 *
 *
 * https://pusher.com/docs/beams/concepts/device-interests/ SAYS
 *      A device can be subscribed to a maximum of 5000 Device Interests.
 *      Device Interest names
 *      - are limited to 164 characters, and
 *      - can only contain ASCII upper/lower-case letters, numbers or one of _-=@,.;
 *
 * We will typically have a single interest per device: e.g. <livescoreid>@<packagename>
 *
 * A message can be send using
 * curl -H "Content-Type: application/json" \
 *      -H "Authorization: Bearer <pusher-primary-key-of-beam>" \
 *      -X POST "https://<pusher-instanceid-of-beam>.pushnotifications.pusher.com/publish_api/v1/instances/<pusher-instanceid-of-beam>/publishes" \
 *      -d '{"interests":["<interest-name>"],"fcm":{"data":{"action":"change_score", "player":"A"}}}'
 *
 */

/** For handling pusher messages when app is in the foreground */
public class PusherHandler implements FCMHandler
{
    private static final String TAG = "SB." + PusherHandler.class.getSimpleName();

    // =================================
    // Singleton
    // =================================

    private PusherHandler() { }
    private static PusherHandler instance = null;
    public static PusherHandler getInstance() {
        if ( instance == null ) {
            instance = new PusherHandler();
        }
        return instance;
    }

    // =================================
    // Foreground only or also in background
    // =================================

    private ScoreBoard m_scoreBoard = null;
    boolean m_bInForegroundOnly = true;
    void setInForegroundOnly(boolean b) {
        m_bInForegroundOnly = b;
        if ( m_bInForegroundOnly == false ) {
            if ( m_scoreBoard != null ) {
                PushNotifications.setOnMessageReceivedListenerForVisibleActivity(m_scoreBoard, m_dummyListener); // neither parameter may be NULL
            }
        }
    }
    private static final PushNotificationReceivedListener m_dummyListener = remoteMessage -> { };

    @Override public void init(ScoreBoard scoreBoard, UUID sPusherUUID, String sMatchPlayerNames)
    {
        m_scoreBoard = scoreBoard;

        String sInterest = PreferenceValues.getFCMDeviceId(scoreBoard) + "@" + scoreBoard.getPackageName();
        Log.d(TAG, "Interest :" + sInterest);

        FirebaseApp.initializeApp(scoreBoard); // required with com.google.gms:google-services:4.3.10 according to error messages, but fails anyway with that version
        PushNotifications.start(scoreBoard, sPusherUUID.toString());
        PushNotifications.clearDeviceInterests();
        PushNotifications.addDeviceInterest(sInterest);
        if ( m_bInForegroundOnly ) {
            PushNotifications.setOnMessageReceivedListenerForVisibleActivity(scoreBoard, remoteMessage -> {
                Map<String, String> data = remoteMessage.getData();
                Log.d(TAG, "Message received in foreground activity. Data " + data);
                if ( data.containsKey(key_action) ) {
                    String sAction = data.get(key_action);
                    handleAction(sAction);
                }
            });
        }
    }

    public boolean handleAction(String sAction) {
        if ( m_scoreBoard == null ) {
            Log.w(TAG, "Discarding action: " + sAction); // service running while app settings told not to use FCM
            return false;
        }
        try {
            m_scoreBoard.interpretReceivedMessageOnUiThread(sAction, MessageSource.FirebaseCloudMessage);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override public void cleanup() {
        if ( m_scoreBoard != null ) {
            Log.d(TAG, "Cleaning up interests ...");
            PushNotifications.clearDeviceInterests();
        }
        m_scoreBoard = null;
    }
}



/*
    Log.d(TAG, "msg.collapseKey()  : " + remoteMessage.getCollapseKey() );
    Log.d(TAG, "msg.from()         : " + remoteMessage.getFrom()        );
    Log.d(TAG, "msg.messageId()    : " + remoteMessage.getMessageId()   );
    Log.d(TAG, "msg.messageType()  : " + remoteMessage.getMessageType() );
    Log.d(TAG, "msg.senderId()     : " + remoteMessage.getSenderId()    );
    Log.d(TAG, "msg.to()           : " + remoteMessage.getTo()          );
    Log.d(TAG, "msg.data()         : " + remoteMessage.getData()        );

    // if a notification is specified and the app is running in the background, it is displayed automatically by the Pusher library code
    RemoteMessage.Notification notification = remoteMessage.getNotification();
    if ( notification != null ) {
        Log.d(TAG, "msg.notification().getBody()      : " + notification.getBody()     );
        Log.d(TAG, "msg.notification().getChannelId() : " + notification.getChannelId());
        Log.d(TAG, "msg.notification().getTitle()     : " + notification.getTitle()    );
        Log.d(TAG, "msg.notification().getTag()       : " + notification.getTag()      );
        Log.d(TAG, "msg.notification().getLink()      : " + notification.getLink()     );
    }
*/
/*
D/SB.PusherUtil: msg.collapseKey()  : com.doubleyellow.scoreboard
    msg.from()         : 577427401473
    msg.messageId()    : 0:1643473131431486%0618a0300618a030
    msg.messageType()  : null
    msg.senderId()     : 577427401473
    msg.to()           : null
D/SB.PusherUtil: msg.data()         : {pusher={"instanceId":"964ea544-59d3-4698-a5dd-2bcdb8b7f93d","hasDisplayableContent":true,"publishId":"pubid-8f9d5eb8-6df8-4457-8b64-6ab7f22086df"}}
    msg.notification().getBody()      : Hello, world!
    msg.notification().getChannelId() : null
    msg.notification().getTitle()     : Hello
    msg.notification().getTag()       : null
    msg.notification().getLink()      : null
*/
