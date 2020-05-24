package com.hestonliebowitz.dingdongditch;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        final String firebaseAppId = settings.getString(getString(R.string.firebase_pref_app_id), null);
        if (firebaseAppId == null) {
            startActivity(new Intent(this, SetupActivity.class));
            finish();
            return;
        }

        final FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
        } else {

            startActivity(new Intent(this, PrefActivity.class));
        }
        finish();
    }
}