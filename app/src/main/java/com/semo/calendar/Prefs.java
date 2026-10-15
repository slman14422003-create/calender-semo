package com.semo.calendar;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Calendar;

/** Small settings store (first weekday, default alert, theme). */
final class Prefs {
    private Prefs() {
    }

    static final int[] REMIND = {-1, 0, 5, 15, 30, 60, 1440};
    static final int[] REMIND_LABEL = {R.string.rem_none, R.string.rem_0, R.string.rem_5, R.string.rem_15,
            R.string.rem_30, R.string.rem_60, R.string.rem_1440};

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences("calendar", Context.MODE_PRIVATE);
    }

    static int firstDay(Context c) {
        return sp(c).getInt("first", Calendar.getInstance().getFirstDayOfWeek());
    }

    static void setFirstDay(Context c, int v) {
        sp(c).edit().putInt("first", v).apply();
    }

    static int defaultRemind(Context c) {
        return sp(c).getInt("remind", 15);
    }

    static void setDefaultRemind(Context c, int v) {
        sp(c).edit().putInt("remind", v).apply();
    }

    /** 0 = system, 1 = light, 2 = dark */
    static int theme(Context c) {
        return sp(c).getInt("theme", 0);
    }

    static void setTheme(Context c, int v) {
        sp(c).edit().putInt("theme", v).apply();
    }

    static int remindIndex(int minutes) {
        for (int i = 0; i < REMIND.length; i++) if (REMIND[i] == minutes) return i;
        return 0;
    }

    /** Which calendar view was open last: 0 day, 1 week, 2 month. */
    static int view(Context c) {
        return sp(c).getInt("view", 2);
    }

    static void setView(Context c, int v) {
        sp(c).edit().putInt("view", v).apply();
    }

    static final int[] DURATION = {15, 30, 45, 60, 90, 120};

    static int duration(Context c) {
        return sp(c).getInt("duration", 60);
    }

    static void setDuration(Context c, int v) {
        sp(c).edit().putInt("duration", v).apply();
    }

    static boolean weekNumbers(Context c) {
        return sp(c).getBoolean("weeks", false);
    }

    static void setWeekNumbers(Context c, boolean v) {
        sp(c).edit().putBoolean("weeks", v).apply();
    }

    /** 0 = no alternate calendar, 1 = Hijri (Umm al-Qura). */
    static int alt(Context c) {
        return sp(c).getInt("alt", 0);
    }

    static void setAlt(Context c, int v) {
        sp(c).edit().putInt("alt", v).apply();
    }

    static String defaultCal(Context c) {
        return sp(c).getString("defcal", Cals.PERSONAL);
    }

    static void setDefaultCal(Context c, String id) {
        sp(c).edit().putString("defcal", id).apply();
    }
}
