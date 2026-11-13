package com.semo.calendar;

import android.content.Context;
import android.text.format.DateFormat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
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

    /** "09:00 \u2013 10:00" or "All-day", followed by the place when there is one. */
    static String line(Context c, Recur.Occ o, boolean withDate) {
        StringBuilder sb = new StringBuilder();
        if (withDate) sb.append(date("EEEdMMM", o.start)).append(" \u00b7 ");
        if (o.e.allDay) sb.append(c.getString(R.string.all_day));
        else sb.append(time(c, o.start)).append(" \u2013 ").append(time(c, o.end));
        if (!o.e.location.isEmpty()) sb.append(" \u00b7 ").append(o.e.location);
        return sb.toString();
    }

    /** Second line of an event row: the place, or the date and place when the list mixes days. */
    static String sub(Recur.Occ o, boolean withDate) {
        String d = withDate ? date("EEEdMMM", o.start) : "";
        if (o.e.location.isEmpty()) return d;
        return d.isEmpty() ? o.e.location : d + " \u00b7 " + o.e.location;
    }

    /** "Today", "Tomorrow" or "Yesterday" for those days, otherwise null. */
    static String dayLabel(Context c, long dayStart) {
        long today = Recur.dayStart(System.currentTimeMillis());
        if (dayStart == today) return c.getString(R.string.today);
        if (dayStart == Recur.nextDay(today)) return c.getString(R.string.tomorrow);
        Calendar y = Calendar.getInstance();
        y.setTimeInMillis(today);
        y.add(Calendar.DAY_OF_YEAR, -1);
        if (dayStart == y.getTimeInMillis()) return c.getString(R.string.yesterday);
        return null;
    }

    /** "Today, 14:30" / "Tue 20 Oct" for a task, or an empty string without a due date. */
    static String taskDue(Context c, Task t) {
        if (t.due <= 0) return "";
        String d = dayLabel(c, Recur.dayStart(t.due));
        if (d == null) d = date("EEEdMMM", t.due);
        return t.hasTime ? d + ", " + time(c, t.due) : d;
    }

    static String shareText(Context c, Recur.Occ o) {
        StringBuilder sb = new StringBuilder(o.e.label(c.getString(R.string.untitled)));
        sb.append('\n').append(date("EEEEdMMMM", o.start)).append('\n').append(line(c, o, false));
        if (!o.e.url.isEmpty()) sb.append('\n').append(o.e.url);
        if (!o.e.notes.isEmpty()) sb.append("\n\n").append(o.e.notes);
        return sb.toString();
    }
}
