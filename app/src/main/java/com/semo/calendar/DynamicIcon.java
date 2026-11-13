package com.semo.calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import java.util.Calendar;

final class DynamicIcon {
    static final String TICK = "com.semo.calendar.ICON_TICK";
    private static final String DEFAULT = "6_20";

    private DynamicIcon() {
    }

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences("calendar", Context.MODE_PRIVATE);
    }

    private static String key(Calendar c) {
        return (c.get(Calendar.DAY_OF_WEEK) - 1) + "_" + c.get(Calendar.DAY_OF_MONTH);
    }

    private static ComponentName alias(Context c, String key) {
        return new ComponentName(c.getPackageName(), "com.semo.calendar.Ic" + key);
    }

    static void sync(Context c) {
        String now = key(Calendar.getInstance());
        String old = sp(c).getString("icon_key", DEFAULT);
        if (now.equals(old)) return;
        PackageManager pm = c.getPackageManager();
        pm.setComponentEnabledSetting(alias(c, now), PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
        pm.setComponentEnabledSetting(alias(c, old), PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
        sp(c).edit().putString("icon_key", now).apply();
    }

    static void schedule(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        Calendar next = Calendar.getInstance();
        next.add(Calendar.DAY_OF_MONTH, 1);
        next.set(Calendar.HOUR_OF_DAY, 0);
        next.set(Calendar.MINUTE, 0);
        next.set(Calendar.SECOND, 5);
        next.set(Calendar.MILLISECOND, 0);
        Intent in = new Intent(c, BootReceiver.class).setAction(TICK);
        PendingIntent pi = PendingIntent.getBroadcast(c, 7001, in,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        am.set(AlarmManager.RTC, next.getTimeInMillis(), pi);
    }
}
