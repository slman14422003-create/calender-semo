package com.semo.calendar;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

/** Expands events (including repeating ones) into the occurrences that touch a time range. */
final class Recur {
    private Recur() {
    }

    static final long DAY = 86400000L;

    static final class Occ {
        final Event e;
        final long start;
        final long end;

        Occ(Event e, long start, long end) {
            this.e = e;
            this.start = start;
            this.end = end;
        }
    }

    static long dayStart(long t) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(t);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    static long nextDay(long dayStart) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(dayStart);
        c.add(Calendar.DAY_OF_YEAR, 1);
        return c.getTimeInMillis();
    }

    private static boolean touches(long s, long dur, long from, long to) {
        return s < to && (s + dur > from || s >= from);
    }

    static List<Occ> between(List<Event> events, long from, long to) {
        List<Occ> out = new ArrayList<>();
        for (Event e : events) {
            long dur = Math.max(0, e.end - e.start);
            if (e.repeat == 0) {
                if (touches(e.start, dur, from, to)) out.add(new Occ(e, e.start, e.start + dur));
                continue;
            }
            int field = e.repeat == 1 ? Calendar.DAY_OF_YEAR : e.repeat == 2 ? Calendar.WEEK_OF_YEAR
                    : e.repeat == 3 ? Calendar.MONTH : Calendar.YEAR;
            long unit = e.repeat == 1 ? DAY : e.repeat == 2 ? 7 * DAY : 0;
            int i = 0;
            if (unit > 0 && from - dur > e.start) i = (int) Math.max(0, (from - dur - e.start) / unit - 2);
            Calendar t = Calendar.getInstance();
            for (int n = 0; n < 6000; n++, i++) {
                t.setTimeInMillis(e.start);
                t.add(field, i);
                long s = t.getTimeInMillis();
                if (s >= to) break;
                if (touches(s, dur, from, to)) out.add(new Occ(e, s, s + dur));
            }
        }
        Collections.sort(out, (a, b) -> Long.compare(a.start, b.start));
        return out;
    }
}
