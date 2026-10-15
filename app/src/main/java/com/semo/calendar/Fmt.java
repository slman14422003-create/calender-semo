package com.semo.calendar;

import android.content.Context;
import android.text.format.DateFormat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

final class Fmt {
    private Fmt() {
    }

    static String date(String skeleton, long t) {
        Locale l = Locale.getDefault();
        return new SimpleDateFormat(DateFormat.getBestDateTimePattern(l, skeleton), l).format(new Date(t));
    }

    static String time(Context c, long t) {
        return DateFormat.getTimeFormat(c).format(new Date(t));
    }

    /** "09:00 – 10:00" or "All-day", followed by the place when there is one. */
    static String line(Context c, Recur.Occ o, boolean withDate) {
        StringBuilder sb = new StringBuilder();
        if (withDate) sb.append(date("EEEdMMM", o.start)).append(" · ");
        if (o.e.allDay) sb.append(c.getString(R.string.all_day));
        else sb.append(time(c, o.start)).append(" – ").append(time(c, o.end));
        if (!o.e.location.isEmpty()) sb.append(" · ").append(o.e.location);
        return sb.toString();
    }
}
