package com.johnsonstrength.resttimer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;

public class RestAlarmReceiver extends BroadcastReceiver {
    public static final String CHANNEL = "rest_finished";
    @Override public void onReceive(Context context, Intent intent) {
        long scheduled = context.getSharedPreferences("timer", Context.MODE_PRIVATE).getLong("end_wall", 0);
        if (scheduled == 0 || System.currentTimeMillis() + 2000 < scheduled) return;
        context.getSharedPreferences("timer", Context.MODE_PRIVATE).edit().remove("end_wall").apply();
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock lock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "JohnsonRest:alarm");
        lock.acquire(10000);
        PendingResult pending = goAsync();
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel(CHANNEL, "Rest timer finished", NotificationManager.IMPORTANCE_HIGH);
        manager.createNotificationChannel(channel);
        PendingIntent open = PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new Notification.Builder(context, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle("Rest complete — time to lift!")
                .setContentText("Johnson Strength rest timer").setContentIntent(open).setAutoCancel(true).build();
        try { manager.notify(42, notification); } catch (SecurityException ignored) {}
        Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (sound == null) sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        try {
            MediaPlayer player = new MediaPlayer();
            player.setDataSource(context, sound);
            player.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
            player.setOnCompletionListener(mp -> { mp.release(); if(lock.isHeld())lock.release(); pending.finish(); });
            player.setOnErrorListener((mp, what, extra) -> { mp.release(); if(lock.isHeld())lock.release(); pending.finish(); return true; });
            player.prepare(); player.start();
            new Handler(Looper.getMainLooper()).postDelayed(() -> { if(player.isPlaying()) { player.stop(); player.release(); if(lock.isHeld())lock.release(); pending.finish(); } }, 3500);
        } catch (Exception e) { if(lock.isHeld())lock.release(); pending.finish(); }
    }
}
