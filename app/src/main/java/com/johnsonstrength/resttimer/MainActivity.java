package com.johnsonstrength.resttimer;

import android.app.Activity;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.Context;
import android.os.Handler;
import android.media.ToneGenerator;
import android.media.AudioManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.EditText;
import android.text.InputType;

public class MainActivity extends Activity {
    private static final int PICK_SOUND = 201;
    private int seconds;
    private CountDownTimer timer;
    private TextView display, status;
    private Button main;
    private LinearLayout root, presets;
    private boolean running = false;
    private final Handler handler = new Handler(android.os.Looper.getMainLooper());
    private long endWall;
    private boolean completionPlayed = false;
    private final Runnable tick = new Runnable() { @Override public void run() { refresh(); if (running) handler.postDelayed(this, 250); } };
    private final int bg = Color.rgb(5,5,5), fg = Color.WHITE, accent = Color.rgb(239, 23, 27);
    private int dp(float v) { return (int)(getResources().getDisplayMetrics().density*v + .5f); }
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        seconds = getPreferences(0).getInt("rest", 180);
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setGravity(Gravity.CENTER); root.setPadding(dp(22),dp(30),dp(22),dp(30)); root.setBackgroundColor(bg);
        TextView title = new TextView(this); title.setText("JOHNSON STRENGTH COMPANY\nREST TIMER"); title.setTextColor(accent); title.setTextSize(20); title.setTypeface(null,Typeface.BOLD); title.setGravity(Gravity.CENTER); title.setLineSpacing(dp(4), 1f); root.addView(title);
        status = new TextView(this); status.setText("READY FOR YOUR NEXT SET"); status.setTextColor(Color.LTGRAY); status.setTextSize(15); status.setGravity(Gravity.CENTER); LinearLayout.LayoutParams sl = new LinearLayout.LayoutParams(-1,dp(52)); sl.topMargin=dp(20); root.addView(status,sl);
        display = new TextView(this); display.setTextColor(fg); display.setTextSize(80); display.setTypeface(Typeface.DEFAULT,Typeface.BOLD); display.setGravity(Gravity.CENTER); root.addView(display,new LinearLayout.LayoutParams(-1,dp(150)));
        main = new Button(this); main.setText("START REST"); main.setTextSize(27); main.setAllCaps(false); main.setTextColor(Color.WHITE); main.setBackgroundTintList(android.content.res.ColorStateList.valueOf(accent)); LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(-1,dp(145)); mp.topMargin=dp(18); root.addView(main,mp); main.setOnClickListener(v -> { if (!running) start(); });
        TextView hint = new TextView(this); hint.setText("REST DURATION"); hint.setTextColor(Color.LTGRAY); hint.setTextSize(14); hint.setGravity(Gravity.CENTER); LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,dp(40)); hp.topMargin=dp(25); root.addView(hint,hp);
        presets = new LinearLayout(this); presets.setOrientation(LinearLayout.HORIZONTAL); root.addView(presets,new LinearLayout.LayoutParams(-1,dp(55)));
        for(int s:new int[]{60,90,120,180}) { Button p=new Button(this); p.setText(s+"s"); p.setTextSize(14); p.setAllCaps(false); LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(0,-1,1); pp.setMargins(dp(2),0,dp(2),0); presets.addView(p,pp); p.setOnClickListener(v -> choose(s)); }
        Button custom=new Button(this); custom.setText("CUSTOM TIME"); custom.setAllCaps(false); root.addView(custom,new LinearLayout.LayoutParams(-1,dp(52))); custom.setOnClickListener(v -> customTime());
        Button sounds = new Button(this); sounds.setText("ALARM SOUND"); sounds.setAllCaps(false);
        root.addView(sounds, new LinearLayout.LayoutParams(-1, dp(52)));
        sounds.setOnClickListener(v -> soundOptions());
        Button preview = new Button(this); preview.setText("TEST SOUND"); preview.setAllCaps(false);
        root.addView(preview, new LinearLayout.LayoutParams(-1, dp(52)));
        preview.setOnClickListener(v -> RestSound.play(this, null));
        setContentView(root); render(seconds);
        if (android.os.Build.VERSION.SDK_INT >= 33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != android.content.pm.PackageManager.PERMISSION_GRANTED) requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 100);
    }
    private void render(int s) { display.setText(String.format(java.util.Locale.US,"%d:%02d",s/60,s%60)); }
    private void choose(int s) { if(running)return; seconds=s; getPreferences(0).edit().putInt("rest",s).apply(); render(s); }
    private void customTime() { if(running)return; EditText field=new EditText(this); field.setInputType(InputType.TYPE_CLASS_NUMBER); field.setHint("Seconds (e.g. 150)"); new android.app.AlertDialog.Builder(this).setTitle("Custom rest time").setView(field).setNegativeButton("Cancel",null).setPositiveButton("Save",(d,w)-> { try { int n=Integer.parseInt(field.getText().toString()); if(n>=1 && n<=3600)choose(n); }catch(Exception ignored){} }).show(); }
    private void start() {
        completionPlayed = false;
        endWall = System.currentTimeMillis() + seconds * 1000L;
        getSharedPreferences("timer", Context.MODE_PRIVATE).edit().putLong("end_wall", endWall).apply();
        AlarmManager alarms = (AlarmManager)getSystemService(ALARM_SERVICE);
        Intent intent = new Intent(this, RestAlarmReceiver.class);
        PendingIntent alarm = PendingIntent.getBroadcast(this, 12, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        // A denied or unavailable alarm must never crash the timer UI.
        if (alarms != null) {
            try {
                alarms.set(AlarmManager.RTC_WAKEUP, endWall, alarm);
            } catch (SecurityException | IllegalArgumentException ex) {
                android.util.Log.w("JohnsonRest", "Background alarm unavailable", ex);
            }
        }

        refresh(); handler.removeCallbacks(tick); handler.post(tick);
    }
    private void refresh() {
        long end = getSharedPreferences("timer", Context.MODE_PRIVATE).getLong("end_wall", 0);
        running = end > System.currentTimeMillis();
        if (!running && end != 0 && !completionPlayed) {
            completionPlayed = true;
            playCompletionTone();
            getSharedPreferences("timer", Context.MODE_PRIVATE).edit().remove("end_wall").apply();
        }
        if (running) {
            render((int)Math.ceil((end-System.currentTimeMillis())/1000.0));
            status.setText("RESTING — RECOVER STRONG"); main.setText("COUNTING DOWN"); main.setEnabled(false); presets.setAlpha(.4f);
        } else {
            render(seconds); status.setText(end == 0 ? "READY FOR YOUR NEXT SET" : "TIME TO LIFT!");
            main.setText("START REST"); main.setEnabled(true); presets.setAlpha(1f);
        }
    }
    private void soundOptions() {
        int selected = RestSound.selected(this);
        new android.app.AlertDialog.Builder(this).setTitle("Rest alarm sound")
            .setSingleChoiceItems(RestSound.NAMES, selected, (dialog, which) -> {
                dialog.dismiss();
                if (which == 4) {
                    Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    pick.setType("audio/*");
                    pick.addCategory(Intent.CATEGORY_OPENABLE);
                    startActivityForResult(pick, PICK_SOUND);
                } else {
                    getSharedPreferences("timer", Context.MODE_PRIVATE).edit().putInt("sound", which).apply();
                    RestSound.play(this, null);
                }
            }).setNegativeButton("Cancel", null).show();
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == PICK_SOUND && result == RESULT_OK && data != null && data.getData() != null) {
            try {
                getContentResolver().takePersistableUriPermission(data.getData(), Intent.FLAG_GRANT_READ_URI_PERMISSION);
                getSharedPreferences("timer", Context.MODE_PRIVATE).edit()
                    .putString("sound_uri", data.getData().toString()).putInt("sound", 4).apply();
                RestSound.play(this, null);
            } catch (SecurityException ex) {
                android.widget.Toast.makeText(this, "Unable to access that sound", android.widget.Toast.LENGTH_LONG).show();
            }
        }
    }
    private void playCompletionTone() { RestSound.play(this, null); }
    @Override protected void onResume() { super.onResume(); if (display != null) { refresh(); handler.removeCallbacks(tick); if (running) handler.post(tick); } }
    @Override protected void onPause() { handler.removeCallbacks(tick); super.onPause(); }
    @Override protected void onDestroy() { handler.removeCallbacks(tick); super.onDestroy(); }
}
