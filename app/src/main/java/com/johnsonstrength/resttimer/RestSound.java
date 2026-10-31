package com.johnsonstrength.resttimer;

import android.content.Context;
import android.media.*;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

public final class RestSound {
    public static final String[] NAMES = {"Short beep", "Three beeps", "Alarm", "Long buzzer", "Custom sound"};
    private RestSound() {}
    public static int selected(Context context) {
        return context.getSharedPreferences("timer", Context.MODE_PRIVATE).getInt("sound", 0);
    }
    public static void play(Context context, Runnable finished) {
        int mode = selected(context);
        Handler handler = new Handler(Looper.getMainLooper());
        if (mode == 2 || mode == 4) {
            try {
                Uri uri = mode == 4 ? Uri.parse(context.getSharedPreferences("timer", Context.MODE_PRIVATE).getString("sound_uri", "")) : RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                if (uri == null || uri.toString().isEmpty()) throw new IllegalArgumentException("No sound selected");
                Ringtone ringtone = RingtoneManager.getRingtone(context, uri);
                if (ringtone == null) throw new IllegalArgumentException("Sound unavailable");
                ringtone.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
                ringtone.play();
                handler.postDelayed(() -> {
                    try { ringtone.stop(); } catch (RuntimeException ignored) {}
                    if (finished != null) finished.run();
                }, 3000);
                return;
            } catch (Exception ex) {
                android.util.Log.w("JohnsonRest", "Selected sound unavailable; using beep", ex);
                mode = 0;
            }
        }
        final int chosen = mode;
        try {
            ToneGenerator tone = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
            if (chosen == 1) {
                for (int i = 0; i < 3; i++) {
                    int delay = i * 450;
                    handler.postDelayed(() -> tone.startTone(ToneGenerator.TONE_PROP_BEEP, 250), delay);
                }
                handler.postDelayed(() -> { tone.release(); if (finished != null) finished.run(); }, 1550);
            } else {
                tone.startTone(chosen == 3 ? ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD : ToneGenerator.TONE_PROP_BEEP2, chosen == 3 ? 2200 : 750);
                handler.postDelayed(() -> { tone.release(); if (finished != null) finished.run(); }, chosen == 3 ? 2500 : 1000);
            }
        } catch (RuntimeException ex) {
            android.util.Log.w("JohnsonRest", "Unable to play sound", ex);
            if (finished != null) finished.run();
        }
    }
}
