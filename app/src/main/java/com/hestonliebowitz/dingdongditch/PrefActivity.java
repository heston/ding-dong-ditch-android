package com.hestonliebowitz.dingdongditch;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.IBinder;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;

import org.w3c.dom.Text;

interface DialogTextInput {
    public void onDialogInputChanged(int id, View view);
    public String getDialogInitialValue(int id);
}

public class PrefActivity extends AppCompatActivity implements DialogTextInput {
    private String TAG = "PrefActivity";

    private FirebaseDatabase mDatabase;
    private Switch pushNotifSwitch;
    private Switch phoneCallSwitch;
    private Switch chimeSwitch;
    private Button unlockButton;
    private Button logOutButton;
    private View setLoginPinView;
    private DataService mData;

    private static int LOGIN_PIN_ID = 101;


    private class LoginPinClickHandler implements View.OnClickListener {
        private AlertDialog.Builder dialogBuilder;
        private Context callingContext;
        private DialogTextInput listener;
        private int dialogId;

        public LoginPinClickHandler(Context context, int id) {
            super();
            callingContext = context;
            dialogId = id;

            try {
                // Instantiate the NoticeDialogListener so we can send events to the host
                listener = (DialogTextInput) callingContext;
            } catch (ClassCastException e) {
                // The context doesn't implement the interface, throw exception
                throw new ClassCastException(callingContext.toString()
                        + " must implement DialogTextInput");
            }
        }

        @Override
        public void onClick(View view) {
            dialogBuilder = new AlertDialog.Builder(callingContext);
            dialogBuilder
                    .setView(R.layout.login_pin_dialog)
                    .setNegativeButton(R.string.cancel_button, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.cancel();
                        }
                    });
            final AlertDialog dialog = dialogBuilder.create();

            dialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.save_button), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    EditText textInput = dialog.findViewById(R.id.login_pin_input);
                    Log.i(TAG, String.format("LoginPinClickHandler:onClick:%s", dialogId));
                    if (textInput != null) {
                        Log.i(TAG, String.format("LoginPinClickHandler:onClick:%s:textInput", dialogId));
                        listener.onDialogInputChanged(dialogId, textInput);
                    }
                }
            });

            dialog.show();
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

            EditText textInput = dialog.findViewById(R.id.login_pin_input);
            String initialValue = listener.getDialogInitialValue(dialogId);
            if (textInput != null) {
                textInput.setText(initialValue);
                textInput.setSelection(initialValue.length());
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pref);

        mData = new DataService(this);
        mDatabase = FirebaseDatabase.getInstance();

        setLoginPinView = findViewById(R.id.set_login_pin);
        setLoginPinView.setOnClickListener(new LoginPinClickHandler(this, LOGIN_PIN_ID));

        phoneCallSwitch = (Switch) findViewById(R.id.phone_call_switch);
        phoneCallSwitch.setEnabled(false);
        phoneCallSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                mData.setPhoneCallValue(b);
            }
        });

        pushNotifSwitch = (Switch) findViewById(R.id.push_notification_switch);
        pushNotifSwitch.setEnabled(false);
        pushNotifSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                mData.setPushNotifValue(b);
            }
        });

        chimeSwitch = (Switch) findViewById(R.id.chime_switch);
        chimeSwitch.setEnabled(false);
        chimeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                mData.setChimeValue(b);
            }
        });

        unlockButton = (Button) findViewById(R.id.unlock_button);
        unlockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mData.unlockGate();
            }
        });

        logOutButton = (Button) findViewById(R.id.logout_button);
        logOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                logOut();
            }
        });

        ensureLoginPin();
        bindPhoneCallValue();
        bindPushNotifValue();
        bindChimeValue();
        setCurrentAccount();
        setLoginPinView();
        mData.setupPushNotif();
    }

    private void setCurrentAccount() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        String phone = "Not logged in";
        try {
            phone = auth.getCurrentUser().getPhoneNumber();
        } catch (NullPointerException e) {}
        TextView currentAccount = findViewById(R.id.current_account);
        currentAccount.setText(phone);
    }

    private void logOut() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        auth.signOut();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void ensureLoginPin() {
        String loginPin = mData.getLoginPin();
        if(loginPin.isEmpty()) {
            LoginPinClickHandler handler = new LoginPinClickHandler(this, LOGIN_PIN_ID);
            handler.onClick(null);
        }
    }

    public void onDialogInputChanged(int id, View view) {
        if (id == LOGIN_PIN_ID) {
            EditText loginPinInput = (EditText) view;
            String loginPin = loginPinInput.getText().toString();
            Log.i(TAG, String.format("onDialogInputChanged:%s:%s", id, loginPin));
            mData.setLoginPin(loginPin);

            setLoginPinView();
            bindPhoneCallValue();
            bindPushNotifValue();
        }
    }

    private void setLoginPinView() {
        TextView loginPinView = findViewById(R.id.login_pin);
        if (loginPinView != null) {
            String loginPin = mData.getLoginPin();
            loginPinView.setText(loginPin);
        }
    }

    public String getDialogInitialValue(int id) {
        String initialValue = "";
        if (id == LOGIN_PIN_ID) {
            initialValue = mData.getLoginPin();
        }
        return initialValue;
    }

    private void bindPhoneCallValue() {
        DatabaseReference dbRef = mData.getPhoneCallValue();
        if (dbRef == null) {
            return;
        }

        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Long snapshot = dataSnapshot.getValue(Long.class);
                Boolean switchValue = snapshot != null && snapshot == 1;

                Log.i(TAG, String.format("bindPhoneCallValue:onDataChange:%s", switchValue));
                phoneCallSwitch.setChecked(switchValue);
                if (!phoneCallSwitch.isEnabled()) {
                    phoneCallSwitch.setEnabled(true);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                phoneCallSwitch.setChecked(false);
                Log.w(TAG, "bindPhoneCallValue:onCancelled", databaseError.toException());
            }
        });
    }


    private void bindPushNotifValue() {
        DatabaseReference dbRef = mData.getPushNotifValue();
        if (dbRef == null) {
            return;
        }

        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Long snapshot = dataSnapshot.getValue(Long.class);
                Boolean switchValue = snapshot != null && snapshot == 2;

                Log.i(TAG, String.format("bindPushNotifValue:onDataChange:%s", switchValue));
                pushNotifSwitch.setChecked(switchValue);
                if (!pushNotifSwitch.isEnabled()) {
                    pushNotifSwitch.setEnabled(true);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                pushNotifSwitch.setChecked(false);
                Log.w(TAG, "bindPushNotifValue:onCancelled", databaseError.toException());
            }
        });
    }

    private void bindChimeValue() {
        DatabaseReference dbRef = mData.getChimeValue();
        if (dbRef == null) {
            return;
        }

        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Long snapshot = dataSnapshot.getValue(Long.class);
                Boolean switchValue = snapshot != null && snapshot == 1;

                Log.i(TAG, String.format("bindChimeValue:onDataChange:%s", switchValue));
                chimeSwitch.setChecked(switchValue);
                if (!chimeSwitch.isEnabled()) {
                    chimeSwitch.setEnabled(true);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                chimeSwitch.setChecked(false);
                Log.w(TAG, "bindChimeValue:onCancelled", databaseError.toException());
            }
        });
    }
}
