package com.semo.calendar;

import android.icu.util.IslamicCalendar;

import java.util.Date;
import java.util.Locale;

/** Hijri (Umm al-Qura) dates for the alternate calendar display. */
final class Hijri {
    private Hijri() {
    }

    static IslamicCalendar cal() {
        IslamicCalendar c = new IslamicCalendar();
        c.setCalendarType(IslamicCalendar.CalendarType.UMALQURA);
        return c;
    }

    static int day(long ms) {
        IslamicCalendar c = cal();
        c.setTimeInMillis(ms);
        return c.get(android.icu.util.Calendar.DAY_OF_MONTH);
    }

    static String monthYear(long ms) {
        android.icu.text.SimpleDateFormat f = new android.icu.text.SimpleDateFormat("MMMM y", Locale.getDefault());
        f.setCalendar(cal());
        return f.format(new Date(ms));
    }

    /** "Rabi II 1448" or "Rabi II 1448 - Jumada I 1448" for a span of days. */
    static String range(long first, long last) {
        String a = monthYear(first);
        String b = monthYear(last);
        return a.equals(b) ? a : a + " \u2013 " + b;
    }
}
