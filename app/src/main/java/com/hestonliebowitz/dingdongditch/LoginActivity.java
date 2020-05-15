package com.hestonliebowitz.dingdongditch;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;

import java.util.Arrays;

public class LoginActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 107;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        setSupportActionBar((Toolbar) findViewById(R.id.app_toolbar));

        final Button loginButton = findViewById(R.id.login);
        if (loginButton != null) {
            loginButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startActivityForResult(
                            // Get an instance of AuthUI based on the default app
                            AuthUI
                                    .getInstance()
                                    .createSignInIntentBuilder()
                                    .setAvailableProviders(
                                            Arrays.asList(
                                                    new AuthUI.IdpConfig.PhoneBuilder().build()
                                            )
                                    )
                                    .build(),
                            RC_SIGN_IN);
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.toolbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_reset:
                FirebaseInit.reset(this);
                return true;
            default:
                return true;
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            // Successfully signed in
            if (resultCode == RESULT_OK) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            } else {
                // Sign in failed
                if (response != null) {
                    int errCode = response.getError().getErrorCode();
                    switch (errCode) {
                        case ErrorCodes.NO_NETWORK:
                            Toast
                                    .makeText(
                                            this,
                                            R.string.no_internet_connection,
                                            Toast.LENGTH_SHORT)
                                    .show();
                            break;
                        case ErrorCodes.UNKNOWN_ERROR:
                            Toast
                                    .makeText(
                                            this,
                                            R.string.unknown_error,
                                            Toast.LENGTH_SHORT)
                                    .show();
                            break;
                    }
                }
            }
        }
    }
}
