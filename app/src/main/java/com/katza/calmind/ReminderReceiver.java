package com.katza.calmind;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String title = intent.getStringExtra("event_title");
        Log.d("ReminderReceiver", "Alarm received for: " + title);

        NotificationHelper.showNotification(context, "תזכורת לאירוע", title != null ? title : "יש לך אירוע עכשיו");
    }
}