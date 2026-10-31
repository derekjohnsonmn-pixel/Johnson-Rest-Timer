
package com.johnsonstrength.resttimer;

import android.app.*;
import android.content.*;
import android.media.*;
import android.net.Uri;
import android.os.*;

public class RestAlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        long end = context.getSharedPreferences(
            "timer", Context.MODE_PRIVATE
        ).getLong("end_wall", 0);

        if (end == 0 ||
            System.currentTimeMillis() + 2000 < end) {
            return;
        }

        context.getSharedPreferences(
            "timer", Context.MODE_PRIVATE
        ).edit().remove("end_wall").apply();

        NotificationManager manager =
            (NotificationManager) context.getSystemService(
                Context.NOTIFICATION_SERVICE
            );

        String channelId = "rest_alarm_v2";

        NotificationChannel channel =
            new NotificationChannel(
                channelId,
                "Rest Complete",
                NotificationManager.IMPORTANCE_HIGH
            );

        manager.createNotificationChannel(channel);

        Notification notification =
            new Notification.Builder(context, channelId)
                .setSmallIcon(
                    android.R.drawable.ic_lock_idle_alarm
                )
                .setContentTitle("REST COMPLETE!")
                .setContentText("Time for your next set!")
                .setAutoCancel(true)
                .build();

        try {
            manager.notify(42, notification);
        } catch (SecurityException ignored) {
        }

        final PendingResult pending = goAsync();

        PowerManager power =
            (PowerManager) context.getSystemService(
                Context.POWER_SERVICE
            );

        PowerManager.WakeLock wakeLock =
            power.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "JohnsonRest:Alert"
            );

        wakeLock.acquire(8000);

        RestSound.play(context, () -> {
            if (wakeLock.isHeld()) wakeLock.release();
            pending.finish();
        });
    }
}
