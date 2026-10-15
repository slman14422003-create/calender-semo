package com.semo.calendar;

import android.content.Context;
import android.icu.util.IslamicCalendar;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Built-in read-only Holidays calendar: a few fixed-date days and the main Islamic days (Umm al-Qura dates,
 * which can differ by a day from the local moon sighting).
 */
final class Holidays {
    private Holidays() {
    }

    private static final int[] HIJRI_MONTH = {0, 0, 2, 6, 8, 9, 11, 11};
    private static final int[] HIJRI_DAY = {1, 10, 12, 27, 1, 1, 9, 10};
    private static final int[] HIJRI_NAME = {R.string.h_hijri_new_year, R.string.h_ashura, R.string.h_mawlid,
            R.string.h_isra, R.string.h_ramadan, R.string.h_eid_fitr, R.string.h_arafah, R.string.h_eid_adha};

    private static void add(List<Event> out, Context c, String key, long dayStart, int nameRes, long from, long to) {
        if (dayStart < from - Recur.DAY || dayStart > to + Recur.DAY) return;
        Event e = new Event();
        e.id = key;
        e.title = c.getString(nameRes);
        e.start = dayStart;
        e.end = dayStart + Recur.DAY - 1;
        e.allDay = true;
        e.cal = Cals.HOLIDAYS;
        e.readOnly = true;
        out.add(e);
    }

    private static void fixed(List<Event> out, Context c, int year, int month, int day, int nameRes,
                              long from, long to) {
        Calendar k = Calendar.getInstance();
        k.clear();
        k.set(year, month, day);
        add(out, c, "fx-" + nameRes + "-" + year, k.getTimeInMillis(), nameRes, from, to);
    }

    static List<Event> events(Context c, long from, long to) {
        List<Event> out = new ArrayList<>();
        Calendar k = Calendar.getInstance();
        k.setTimeInMillis(from);
        int y1 = k.get(Calendar.YEAR);
        k.setTimeInMillis(to);
        int y2 = k.get(Calendar.YEAR);
        for (int y = y1; y <= y2; y++) {
            fixed(out, c, y, Calendar.JANUARY, 1, R.string.h_new_year, from, to);
            fixed(out, c, y, Calendar.MAY, 1, R.string.h_labour, from, to);
            fixed(out, c, y, Calendar.DECEMBER, 25, R.string.h_christmas, from, to);
        }
        IslamicCalendar ic = Hijri.cal();
        ic.setTimeInMillis(from);
        int h1 = ic.get(android.icu.util.Calendar.YEAR) - 1;
        ic.setTimeInMillis(to);
        int h2 = ic.get(android.icu.util.Calendar.YEAR) + 1;
        for (int hy = h1; hy <= h2; hy++) {
            for (int i = 0; i < HIJRI_NAME.length; i++) {
                ic.clear();
                ic.set(android.icu.util.Calendar.YEAR, hy);
                ic.set(android.icu.util.Calendar.MONTH, HIJRI_MONTH[i]);
                ic.set(android.icu.util.Calendar.DAY_OF_MONTH, HIJRI_DAY[i]);
                long ms = Recur.dayStart(ic.getTimeInMillis());
                add(out, c, "hj-" + i + "-" + hy, ms, HIJRI_NAME[i], from, to);
            }
        }
        return out;
    }
}
