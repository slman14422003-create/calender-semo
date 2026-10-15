package com.semo.calendar;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import androidx.appcompat.app.AppCompatDelegate;

public class App extends Application {
    static final String CHANNEL = "reminders";

    @Override
    public void onCreate() {
        super.onCreate();
        int t = Prefs.theme(this);
        AppCompatDelegate.setDefaultNightMode(t == 1 ? AppCompatDelegate.MODE_NIGHT_NO
                : t == 2 ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CHANNEL, getString(R.string.channel_name),
                    NotificationManager.IMPORTANCE_HIGH);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }
}
