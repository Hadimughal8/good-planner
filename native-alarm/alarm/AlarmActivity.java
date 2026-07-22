package com.yourname.goodplanner.alarm;

import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.yourname.goodplanner.R;

public class AlarmActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (keyguardManager != null) {
                keyguardManager.requestDismissKeyguard(this, null);
            }
        } else {
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            );
        }

        setContentView(R.layout.activity_alarm);

        final int id = getIntent().getIntExtra("id", 0);
        String titleStr = getIntent().getStringExtra("title");
        String bodyStr = getIntent().getStringExtra("body");
        final String title = titleStr != null ? titleStr : "Reminder";
        final String body = bodyStr != null ? bodyStr : "";

        ((TextView) findViewById(R.id.alarmTitle)).setText(title);
        ((TextView) findViewById(R.id.alarmBody)).setText(body);

        findViewById(R.id.stopButton).setOnClickListener(v -> {
            stopAlarmService(id);
            finish();
        });
        findViewById(R.id.snoozeButton).setOnClickListener(v -> {
            stopAlarmService(id);
            snooze(id, title, body);
            finish();
        });
    }

    private void stopAlarmService(int id) {
        Intent stopIntent = new Intent(this, AlarmForegroundService.class);
        stopIntent.setAction("STOP_ALARM");
        stopIntent.putExtra("id", id);
        startService(stopIntent);
    }

    private void snooze(int id, String title, String body) {
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent snoozeIntent = new Intent(this, AlarmReceiver.class);
        snoozeIntent.putExtra("id", id);
        snoozeIntent.putExtra("title", title);
        snoozeIntent.putExtra("body", body);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pi = PendingIntent.getBroadcast(this, id, snoozeIntent, flags);
        long triggerAt = System.currentTimeMillis() + 5 * 60 * 1000;
        if (am == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && am.canScheduleExactAlarms()) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        }
    }
}
