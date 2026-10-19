package com.semo.calendar;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;

import java.util.Calendar;

/**
 * Calendar side of the Calendar-Clock link (the apps stay separate; the clock is found by its app.semo.ROLE).
 * Reads a tiny read-only summary from the clock (next alarm, next prayer) and can open the clock's new-alarm sheet
 * prefilled from an event; the person still has to press Save there.
 */
final class ClockLink {
    private ClockLink() {
    }

    static final String ACTION_NEW_ALARM = "app.semo.clock.NEW_ALARM";

    static final class Summary {
        long nextAlarm;
        String prayerName = "";
        long prayerAt;
    }

    static boolean available(Context c) {
        return Semo.byRole(c, Semo.ROLE_CLOCK) != null;
    }

    /** Never throws; null when the clock is not installed or does not answer. */
    static Summary read(Context c) {
        Semo.Sibling s = Semo.byRole(c, Semo.ROLE_CLOCK);
        if (s == null) return null;
        Cursor cur = null;
        try {
            cur = c.getContentResolver().query(Uri.parse("content://" + s.pkg + ".semo/summary"), null, null, null, null);
            if (cur == null) return null;
            Summary out = new Summary();
            while (cur.moveToNext()) {
                String k = cur.getString(0);
                String v = cur.getString(1);
                if (k == null || v == null) continue;
                try {
                    if (k.equals("next_alarm")) out.nextAlarm = Long.parseLong(v);
                    else if (k.equals("next_prayer_at")) out.prayerAt = Long.parseLong(v);
                    else if (k.equals("next_prayer_name")) out.prayerName = v;
                } catch (NumberFormatException ignored) {
                }
            }
            return out;
        } catch (Throwable e) {
            return null;
        } finally {
            if (cur != null) cur.close();
        }
    }

    /** Opens the clock's new-alarm sheet at the time of {@code whenMs}, labelled {@code label}. */
    static boolean newAlarm(Context c, long whenMs, String label) {
        Semo.Sibling s = Semo.byRole(c, Semo.ROLE_CLOCK);
        if (s == null) return false;
        try {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(whenMs);
            Intent i = new Intent(ACTION_NEW_ALARM).setPackage(s.pkg)
                    .putExtra("hour", cal.get(Calendar.HOUR_OF_DAY))
                    .putExtra("minute", cal.get(Calendar.MINUTE))
                    .putExtra("label", label == null ? "" : label)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(i);
            return true;
        } catch (Throwable e) {
            return false;
        }
    }
}
