package com.hestonliebowitz.dingdongditch;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

public class FirebaseInstanceIDService extends FirebaseInstanceIdService {
    public static String TAG = "FirebaseInstanceIDSrv";

    private DataService mData;

    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.d(TAG, "Refreshed token: " + refreshedToken);

        mData = new DataService(this);
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        String oldPushNotifToken = settings.getString(mData.PUSH_NOTIF_TOKEN, "");

        DatabaseReference dbRef = mData.getPushNotifValue(oldPushNotifToken);
        if (dbRef == null) {
            return;
        }

        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Long snapshot = dataSnapshot.getValue(Long.class);
                Boolean value = snapshot != null && snapshot == 2;
                mData.setPushNotifValue(value);

                Log.i(TAG, String.format("onTokenRefresh:%s", value));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "onTokenRefresh:onCancelled", databaseError.toException());
            }
        });
    }
}
