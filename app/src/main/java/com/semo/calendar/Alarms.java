package com.semo.calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.Collections;
import java.util.List;

/**
 * Inexact alarms. Events get up to two alerts (slot 0 and 1, plus travel time) and a snooze slot (2); tasks get one
 * alert at the due time. Each alarm is always the next occurrence whose alert time is still ahead.
 */
final class Alarms {
    private Alarms() {
    }

    /** All-day events alert at 9:00 on the day (minus the lead time), like iOS. */
    static final long ALL_DAY_ALERT = 9 * 3600000L;

    private static PendingIntent intent(Context c, String key, String id, String kind, int slot, long at, int flags) {
        Intent i = new Intent(c, ReminderReceiver.class).putExtra("id", id).putExtra("at", at)
                .putExtra("kind", kind).putExtra("slot", slot);
        return PendingIntent.getBroadcast(c, key.hashCode() * 31 + slot, i, flags | PendingIntent.FLAG_IMMUTABLE);
    }

    private static void cancelKey(Context c, String key, int slot) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = intent(c, key, key, "x", slot, 0, PendingIntent.FLAG_NO_CREATE);
        if (am != null && pi != null) {
            am.cancel(pi);
            pi.cancel();
        }
    }

    private static void fire(Context c, String key, String id, String kind, int slot, long at, long when) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        try {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when,
                    intent(c, key, id, kind, slot, at, PendingIntent.FLAG_UPDATE_CURRENT));
        } catch (SecurityException ignored) {
        }
    }

    static void cancel(Context c, String eventId) {
        for (int slot = 0; slot < 3; slot++) cancelKey(c, eventId, slot);
    }

    static void schedule(Context c, Event e) {
        cancel(c, e.id);
        if (e.readOnly) return;
        long now = System.currentTimeMillis();
        int[] leads = {e.remind, e.remind2};
        for (int slot = 0; slot < 2; slot++) {
            if (leads[slot] < 0) continue;
            long lead = (leads[slot] + e.travel) * 60000L;
            List<Recur.Occ> l = Recur.between(Collections.singletonList(e), now - Recur.DAY,
                    now + 400 * Recur.DAY + lead);
            for (Recur.Occ o : l) {
                long base = e.allDay ? o.start + ALL_DAY_ALERT : o.start;
                long at = base - lead;
                if (at <= now) continue;
                fire(c, e.id, e.id, "event", slot, o.start, at);
                break;
            }
        }
    }

    static void snooze(Context c, String eventId, long at, int minutes) {
        fire(c, eventId, eventId, "event", 2, at, System.currentTimeMillis() + minutes * 60000L);
    }

    static void cancelTask(Context c, String taskId) {
        cancelKey(c, "task:" + taskId, 0);
    }

    static void scheduleTask(Context c, Task t) {
        cancelTask(c, t.id);
        if (t.done || !t.remind || !t.hasTime || t.due <= System.currentTimeMillis()) return;
        fire(c, "task:" + t.id, t.id, "task", 0, t.due, t.due);
    }

    static void rescheduleAll(Context c) {
        for (Event e : Store.all(c)) schedule(c, e);
        for (Task t : Tasks.all(c)) scheduleTask(c, t);
    }
}
