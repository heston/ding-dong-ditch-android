package com.hestonliebowitz.dingdongditch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ActionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getStringExtra(context.getString(R.string.action_label));
        DataService dataService = new DataService(context);

        if (action.equals(context.getString(R.string.unlock_action_id))) {
            dataService.unlockGate();
        }
    }
}
