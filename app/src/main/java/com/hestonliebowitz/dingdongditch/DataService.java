package com.hestonliebowitz.dingdongditch;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DataService {
    public static String TAG = "DataService";
    public static String LOGIN_PIN_SETTING_NAME = "loginPin";
    public static String PUSH_NOTIF_TOKEN = "pushNotifToken";
    private static String SETTINGS_BASE_PATH = "/settings/%s";
    private static String SYSTEM_SETTINGS_BASE_PATH = "/systemSettings";
    private static String RECIPIENTS_PATH = "/recipients";
    private static String STRIKE_PATH = "/strike";
    private static String CHIME_PATH = "/chime";
    private static String LAST_UPDATED_PATH = "/lastSeenAt";
    public static String NOTIFICATION_CHANNEL_ID = "dingdongditch_alerts";
    private static long VIBRATION_SHORT = 100;
    private static long VIBRATION_LONG = 400;
    private static long VIBRATION_PAUSE = 100;
    private static String EVENTS_ROOT = "events";
    private static long MAX_IMAGE_SIZE = 1024 * 1024 * 2;  // 2 MB
    private static long IMAGE_FETCH_RETRIES = 3;
    private static long IMAGE_FETCH_RETRY_DELAY = 500;  // milliseconds

    private FirebaseDatabase mDatabase;
    private Context mContext;

    public DataService(Context context) {
        mDatabase = FirebaseDatabase.getInstance();
        mContext = context;
    }

    public String getLoginPin() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
        return settings.getString(LOGIN_PIN_SETTING_NAME, "");
    }

    public void setLoginPin(String loginPin) {
        Log.i(TAG, String.format("setLoginPin:%s", loginPin));
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(LOGIN_PIN_SETTING_NAME, loginPin);
        editor.apply();
    }

    public String getRecipientPath(String id) {
        String loginPin = getLoginPin();
        if(loginPin.isEmpty() || id.isEmpty()) {
            return null;
        }
        return String.format(SETTINGS_BASE_PATH + RECIPIENTS_PATH + "/%s", loginPin, id);
    }

    private String getStrikePath() {
        String loginPin = getLoginPin();
        if(loginPin.isEmpty()) {
            return null;
        }
        return String.format(SETTINGS_BASE_PATH + STRIKE_PATH, loginPin);
    }

    private String getChimePath() {
        String loginPin = getLoginPin();
        if(loginPin.isEmpty()) {
            return null;
        }
        return String.format(SETTINGS_BASE_PATH + CHIME_PATH, loginPin);
    }

    private String getLastUpdatedPath() {
        return SYSTEM_SETTINGS_BASE_PATH + LAST_UPDATED_PATH;
    }

    private String getEventPath(String eventId) {
        String loginPin = getLoginPin();
        if(loginPin.isEmpty() || eventId.isEmpty()) {
            return null;
        }
        return String.format("/" + EVENTS_ROOT + "/%s/%s", loginPin, eventId);
    }


    public void unlockGate() {
        String strikePath = getStrikePath();
        DatabaseReference dbRef = mDatabase.getReference().child(strikePath);

        dbRef.setValue(1)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast toast = Toast.makeText(
                                mContext,
                                R.string.unlock_confirmation,
                                Toast.LENGTH_SHORT
                        );
                        toast.show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast toast = Toast.makeText(
                                mContext,
                                R.string.unlock_failed,
                                Toast.LENGTH_SHORT
                        );
                        toast.show();
                    }
                });
    }

    public DatabaseReference getLastUpdatedAtValue() {
        String lastUpdatedPath = getLastUpdatedPath();

        return mDatabase.getReference().child(lastUpdatedPath);
    }

    public DatabaseReference getPushNotifValue(String token) {
        if (token == null) {
            return null;
        }

        String recipientPath = getRecipientPath(token);

        if (recipientPath == null) {
            return null;
        }

        return mDatabase.getReference().child(recipientPath);
    }

    public DatabaseReference getPushNotifValue() {
        String token = FirebaseInstanceId.getInstance().getToken();
        return getPushNotifValue(token);
    }

    public void setPushNotifValue(boolean b) {
        String token = FirebaseInstanceId.getInstance().getToken();
        if (token == null) {
            return;
        }
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(PUSH_NOTIF_TOKEN, token);
        editor.apply();

        String recipientPath = getRecipientPath(token);
        DatabaseReference dbRef = mDatabase.getReference().child(recipientPath);

        dbRef.setValue(b ? 2 : null);
    }

    public DatabaseReference getChimeValue() {
        String chimePath = getChimePath();

        if (chimePath == null) {
            return null;
        }

        return mDatabase.getReference().child(chimePath);
    }

    public void setChimeValue(boolean b) {
        String chimePath = getChimePath();

        DatabaseReference dbRef = mDatabase.getReference().child(chimePath);

        dbRef.setValue(b ? 1 : 0);
    }

    public DatabaseReference getPhoneCallValue() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            return null;
        }
        String phone = user.getPhoneNumber();
        String recipientPath = getRecipientPath(phone);
        if (recipientPath == null) {
            return null;
        }

        return mDatabase.getReference().child(recipientPath);
    }

    public void setPhoneCallValue(boolean b) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            return;
        }
        String phone = user.getPhoneNumber();
        String recipientPath = getRecipientPath(phone);
        DatabaseReference dbRef = mDatabase.getReference().child(recipientPath);

        dbRef.setValue(b ? 1 : null);
    }

    public DatabaseReference getEvent(String eventId) {
        String eventPath = getEventPath(eventId);

        if (eventPath == null) {
            return null;
        }

        return mDatabase.getReference().child(eventPath);
    }

    public void setupPushNotif() {
        NotificationManager mNotificationManager =
                (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

        // The user-visible name of the channel.
        CharSequence name = mContext.getString(R.string.notification_channel_name);

        // The user-visible description of the channel.
        String description = mContext.getString(R.string.notification_channel_description);
        int importance = NotificationManager.IMPORTANCE_HIGH;
        NotificationChannel mChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance);

        // Configure the notification channel.
        mChannel.setDescription(description);
        mChannel.enableLights(true);
        // Sets the notification light color for notifications posted to this
        // channel, if the device supports this feature.
        mChannel.setLightColor(Color.MAGENTA);
        mChannel.enableVibration(true);
        mChannel.setVibrationPattern(new long[]{
                0,
                VIBRATION_SHORT, VIBRATION_PAUSE,
                VIBRATION_LONG,  VIBRATION_PAUSE,
                VIBRATION_SHORT, VIBRATION_PAUSE,
                VIBRATION_LONG,  VIBRATION_PAUSE,
                VIBRATION_SHORT, VIBRATION_PAUSE,
                VIBRATION_LONG,  VIBRATION_PAUSE,
                VIBRATION_SHORT, VIBRATION_PAUSE,
                VIBRATION_LONG,  VIBRATION_PAUSE,
                VIBRATION_SHORT, VIBRATION_PAUSE,
                VIBRATION_LONG,  VIBRATION_PAUSE,
                VIBRATION_SHORT, VIBRATION_PAUSE,
                VIBRATION_LONG,  VIBRATION_PAUSE,
        });
        mNotificationManager.createNotificationChannel(mChannel);
    }

    public void getImage(String eventId, final OnSuccessListener<Bitmap> success, final OnFailureListener failure) {
        getImage(eventId, success, failure, IMAGE_FETCH_RETRIES);
    }

    private void getImage(final String eventId, final OnSuccessListener<Bitmap> success, final OnFailureListener failure, long tries) {
        if (tries == 0) {
            Exception e = new RuntimeException("Max retries exceeded");
            failure.onFailure(e);
            return;
        }

        String loginPin = getLoginPin();
        FirebaseStorage storage = FirebaseStorage.getInstance();
        final long triesRemaining = tries - 1;

        StorageReference storageRef = storage
                .getReference(EVENTS_ROOT)
                .child(loginPin)
                .child(String.format("%s.jpg", eventId));

        storageRef.getBytes(MAX_IMAGE_SIZE)
                .addOnSuccessListener(new OnSuccessListener<byte[]>() {
                    @Override
                    public void onSuccess(byte[] bytes) {
                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        success.onSuccess(bitmap);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        try {
                            long wait = IMAGE_FETCH_RETRY_DELAY * (IMAGE_FETCH_RETRIES - triesRemaining);
                            Thread.sleep(wait);
                        } catch (InterruptedException e1) { }

                        getImage(eventId, success, failure, triesRemaining);
                    }
                });
    }

    public static String formatTimestamp(Float timestamp) {
        String dateTimeFormat = "EEEE, MMM d, h:mm a";
        SimpleDateFormat dateFormat = new SimpleDateFormat(dateTimeFormat);
        timestamp *= 1000;
        Long longTimestamp = timestamp.longValue();
        return dateFormat.format(new Date(longTimestamp));
    }
}
