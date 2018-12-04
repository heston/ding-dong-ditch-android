package com.hestonliebowitz.dingdongditch;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class MessageService extends FirebaseMessagingService {
    private static String TAG = "MessageService";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a data payload.

        Map<String, String> messageData = remoteMessage.getData();

        if (messageData.size() > 0) {
            Log.d(TAG, "Message data payload: " + messageData);
            String title = messageData.get("title");
            String body = messageData.get("body");
            sendNotification(title, body);
        }
    }

    /**
     * Create and show a simple notification containing the received FCM message.
     *
     * @param messageBody FCM message body received.
     */
    private void sendNotification(String messageTitle, String messageBody) {
        Intent mainIntent = new Intent(this, MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent mainPendingIntent = PendingIntent.getActivity(
                this,
                0 /* Request code */,
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        Intent unlockIntent = new Intent(this, ActionReceiver.class);
        unlockIntent.putExtra(
                getString(R.string.action_label),
                getString(R.string.unlock_action_id)
        );
        PendingIntent unlockPendingIntent = PendingIntent.getBroadcast(
                this,
                1 /* Request code */,
                unlockIntent,
                PendingIntent.FLAG_ONE_SHOT
        );

        NotificationCompat.Action action = new NotificationCompat.Action(
                R.drawable.ic_key_black_24dp,
                getString(R.string.unlock_action),
                unlockPendingIntent
        );
        Uri sound = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.doorbell);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, DataService.NOTIFICATION_CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_notifications_active_black_24dp)
                        .setContentTitle(messageTitle)
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(sound)
                        .setContentIntent(mainPendingIntent)
                        .addAction(action);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(
                0 /* ID of notification */,
                notificationBuilder.build()
        );
    }
}
