package com.yourname.goodplanner.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin

@CapacitorPlugin(name = "AlarmScheduler")
class AlarmSchedulerPlugin : Plugin() {

    @PluginMethod
    fun schedule(call: PluginCall) {
        val id = call.getInt("id")
        val atMillis = call.getDouble("atMillis")
        if (id == null || atMillis == null) {
            call.reject("id and atMillis are required")
            return
        }
        val title = call.getString("title") ?: "Reminder"
        val body = call.getString("body") ?: ""

        val ctx = context
        val intent = Intent(ctx, AlarmReceiver::class.java).apply {
            putExtra("id", id)
            putExtra("title", title)
            putExtra("body", body)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(ctx, id, intent, flags)

        val alarmManager = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val whenMillis = atMillis.toLong()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMillis, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMillis, pendingIntent)
            }
            call.resolve()
        } catch (e: Exception) {
            call.reject("Failed to schedule alarm: " + e.message)
        }
    }

    @PluginMethod
    fun cancel(call: PluginCall) {
        val id = call.getInt("id")
        if (id == null) {
            call.reject("id is required")
            return
        }
        val ctx = context
        val intent = Intent(ctx, AlarmReceiver::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(ctx, id, intent, flags)
        val alarmManager = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()

        // Also stop the alarm immediately if it happens to be ringing right now for this id.
        val stopIntent = Intent(ctx, AlarmForegroundService::class.java).apply {
            action = "STOP_ALARM"
            putExtra("id", id)
        }
        ctx.startService(stopIntent)

        call.resolve()
    }

    @PluginMethod
    fun canScheduleExact(call: PluginCall) {
        val ctx = context
        val alarmManager = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val result = com.getcapacitor.JSObject()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            result.put("value", alarmManager.canScheduleExactAlarms())
        } else {
            result.put("value", true)
        }
        call.resolve(result)
    }

    @PluginMethod
    fun openExactAlarmSettings(call: PluginCall) {
        val ctx = context
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = android.net.Uri.parse("package:" + ctx.packageName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            ctx.startActivity(intent)
        }
        call.resolve()
    }
}
