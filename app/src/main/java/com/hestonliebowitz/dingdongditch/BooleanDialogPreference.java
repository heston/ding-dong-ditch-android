package com.hestonliebowitz.dingdongditch;

import android.content.Context;
import android.content.DialogInterface;
import android.preference.DialogPreference;
import android.util.AttributeSet;

import com.google.firebase.auth.FirebaseAuth;

/**
 * Created by heston on 9/28/17.
 */

public class BooleanDialogPreference extends DialogPreference {
    public BooleanDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        // When the user selects "OK", persist the new value
        if (positiveResult) {
            persistBoolean(true);
        } else {
            persistBoolean(false);
        }
    }
}
