package com.hestonliebowitz.dingdongditch;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

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
            String eventId = messageData.get("event_id");
            if (eventId != null) {
                sendImageNotification(title, body, eventId);
            } else {
                sendNotification(title, body);
            }
        }
    }

    private PendingIntent getUnlockPendingIntent() {
        Intent unlockIntent = new Intent(this, ActionReceiver.class);
        unlockIntent.putExtra(
                getString(R.string.action_label),
                getString(R.string.unlock_action_id)
        );
        return PendingIntent.getBroadcast(
                this,
                1 /* Request code */,
                unlockIntent,
                PendingIntent.FLAG_ONE_SHOT
        );
    }

    private PendingIntent getMainPendingIntent() {
        Intent mainIntent = new Intent(this, MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(
                this,
                0 /* Request code */,
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );
    }

    private PendingIntent getNotificationPendingIntent(String eventId) {
        Intent notificationIntent = new Intent(this, NotificationActivity.class);
        notificationIntent.putExtra("eventId", eventId);
        return PendingIntent.getActivity(
                this,
                0 /* Request code */,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );
    }

    private NotificationCompat.Builder getNotificationBuilder() {
        PendingIntent mainPendingIntent = getMainPendingIntent();
        PendingIntent unlockPendingIntent = getUnlockPendingIntent();

        NotificationCompat.Action action = new NotificationCompat.Action(
                R.drawable.ic_key_black_24dp,
                getString(R.string.unlock_action),
                unlockPendingIntent
        );
        Uri sound = Uri.parse(
                ContentResolver.SCHEME_ANDROID_RESOURCE + "://" +
                        getPackageName() + "/" + R.raw.doorbell
        );
        return new NotificationCompat.Builder(this, DataService.NOTIFICATION_CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_notifications_active_black_24dp)
                        .setAutoCancel(true)
                        .setSound(sound)
                        .setContentIntent(mainPendingIntent)
                        .addAction(action);
    }

    /**
     * Create and show a simple notification containing the received FCM message.
     *
     * @param messageBody FCM message body received.
     */
    private void sendNotification(String messageTitle, String messageBody) {
        NotificationCompat.Builder notificationBuilder = getNotificationBuilder();
        notificationBuilder
                .setContentTitle(messageTitle)
                .setContentText(messageBody);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(
                0 /* ID of notification */,
                notificationBuilder.build()
        );
    }

    private void sendImageNotification(
            final String messageTitle,
            final String messageBody,
            final String eventId
    ) {
        DataService mData = new DataService(this);
        mData.getImage(eventId, new OnSuccessListener<Bitmap>() {
            @Override
            public void onSuccess(Bitmap image) {
                PendingIntent notificationIntent = getNotificationPendingIntent(eventId);
                NotificationCompat.Builder notificationBuilder = getNotificationBuilder();
                notificationBuilder
                        .setContentIntent(notificationIntent)
                        .setContentTitle(messageTitle)
                        .setContentText(messageBody)
                        .setStyle(new NotificationCompat.BigPictureStyle()
                                .bigPicture(image));

                NotificationManager notificationManager =
                        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

                notificationManager.notify(
                        0 /* ID of notification */,
                        notificationBuilder.build()
                );
            }
        }, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                sendNotification(messageTitle, messageBody);
            }
        });


    }
}
