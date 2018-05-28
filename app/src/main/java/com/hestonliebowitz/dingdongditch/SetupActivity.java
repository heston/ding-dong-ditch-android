package com.hestonliebowitz.dingdongditch;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class SetupActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        final Button selectFile = findViewById(R.id.setup);
        selectFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                promptToSelectFile();
            }
        });

        final TextView description = (TextView) findViewById(R.id.textView);
        description.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void promptToSelectFile() {
        Intent contentIntent = new Intent(Intent.ACTION_GET_CONTENT);
        // application/json is not supported on Android
        contentIntent.setType("application/octet-stream");
        Intent intent = Intent.createChooser(
                contentIntent,
                "Select google-services.json file"
        );
        startActivityForResult(intent, 1);
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == 1) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                Uri uri = data.getData();
                JSONObject json = parseJson(uri);
                boolean result = initFirebase(json);

                if (result) {
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                }
            }
        }
    }

    private JSONObject parseJson(Uri uri) {
        InputStream inputStream = null;
        StringBuilder stringBuilder = new StringBuilder();
        try {
            inputStream = getContentResolver().openInputStream(uri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
            inputStream.close();
        } catch (java.io.IOException e) {
            return null;
        }

        JSONObject json = null;
        try {
            json = new JSONObject(stringBuilder.toString());
        } catch (org.json.JSONException e) {
            return null;
        }
        return json;
    }

    private boolean initFirebase(JSONObject json) {
        boolean success = FirebaseInit.initFromGoogleServices(this, json);
        if (!success) {
            Toast toast = Toast.makeText(
                    this,
                    R.string.firebase_init_error,
                    Toast.LENGTH_SHORT
            );
            toast.show();
        }
        return success;
    }
}
