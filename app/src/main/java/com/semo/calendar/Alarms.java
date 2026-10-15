package com.semo.calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.Collections;
import java.util.List;

/** Schedules one inexact alarm per event: always the next occurrence whose alert time is still ahead. */
final class Alarms {
    private Alarms() {
    }

    private static PendingIntent intent(Context c, String id, long at, int flags) {
        Intent i = new Intent(c, ReminderReceiver.class).putExtra("id", id).putExtra("at", at);
        return PendingIntent.getBroadcast(c, id.hashCode(), i, flags | PendingIntent.FLAG_IMMUTABLE);
    }

    static void cancel(Context c, String id) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = intent(c, id, 0, PendingIntent.FLAG_NO_CREATE);
        if (am != null && pi != null) {
            am.cancel(pi);
            pi.cancel();
        }
    }

    static void schedule(Context c, Event e) {
        cancel(c, e.id);
        if (e.remind < 0) return;
        long now = System.currentTimeMillis();
        long lead = e.remind * 60000L;
        List<Recur.Occ> l = Recur.between(Collections.singletonList(e), now, now + 400 * Recur.DAY + lead);
        for (Recur.Occ o : l) {
            long at = o.start - lead;
            if (at <= now) continue;
            AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
            if (am == null) return;
            try {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at,
                        intent(c, e.id, o.start, PendingIntent.FLAG_UPDATE_CURRENT));
            } catch (SecurityException ignored) {
            }
            return;
        }
    }

    static void rescheduleAll(Context c) {
        for (Event e : Store.all(c)) schedule(c, e);
    }
}
