package com.yourname.goodplanner.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        int id = intent.getIntExtra("id", 0);
        String title = intent.getStringExtra("title");
        String body = intent.getStringExtra("body");
        if (title == null) title = "Reminder";
        if (body == null) body = "";

        Intent serviceIntent = new Intent(context, AlarmForegroundService.class);
        serviceIntent.putExtra("id", id);
        serviceIntent.putExtra("title", title);
        serviceIntent.putExtra("body", body);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }
}
