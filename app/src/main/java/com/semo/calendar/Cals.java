package com.semo.calendar;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** The user's calendars (Personal, Work, own ones) and the built-in Holidays calendar; visibility and colour. */
final class Cals {
    private Cals() {
    }

    static final String PERSONAL = "personal", WORK = "work", HOLIDAYS = "holidays";

    static final class Cal {
        String id = "";
        String name = "";
        int color;
        boolean visible = true;
        boolean builtin;
    }

    private static List<Cal> cache;

    private static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences("calendar", Context.MODE_PRIVATE);
    }

    private static Cal make(String id, int color, boolean visible, boolean builtin) {
        Cal x = new Cal();
        x.id = id;
        x.color = color;
        x.visible = visible;
        x.builtin = builtin;
        return x;
    }

    private static void load(Context c) {
        cache = new ArrayList<>();
        String s = sp(c).getString("cals", null);
        if (s != null) {
            try {
                JSONArray a = new JSONArray(s);
                for (int i = 0; i < a.length(); i++) {
                    JSONObject o = a.getJSONObject(i);
                    Cal x = new Cal();
                    x.id = o.getString("id");
                    x.name = o.optString("name");
                    x.color = Math.max(0, Math.min(Event.COLORS.length - 1, o.optInt("color")));
                    x.visible = o.optBoolean("visible", true);
                    x.builtin = o.optBoolean("builtin");
                    cache.add(x);
                }
            } catch (Exception ignored) {
            }
        }
        if (cache.isEmpty()) {
            cache.add(make(PERSONAL, 0, true, false));
            cache.add(make(WORK, 2, true, false));
        }
        boolean hasHolidays = false;
        for (Cal x : cache) if (HOLIDAYS.equals(x.id)) hasHolidays = true;
        if (!hasHolidays) cache.add(make(HOLIDAYS, 4, false, true));
    }

    private static void persist(Context c) {
        try {
            JSONArray a = new JSONArray();
            for (Cal x : cache) {
                a.put(new JSONObject().put("id", x.id).put("name", x.name).put("color", x.color)
                        .put("visible", x.visible).put("builtin", x.builtin));
            }
            sp(c).edit().putString("cals", a.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    static synchronized List<Cal> all(Context c) {
        if (cache == null) load(c);
        return new ArrayList<>(cache);
    }

    /** The calendar with this id, or the first one when it no longer exists. */
    static synchronized Cal get(Context c, String id) {
        if (cache == null) load(c);
        for (Cal x : cache) if (x.id.equals(id)) return x;
        return cache.get(0);
    }

    static boolean visible(Context c, String id) {
        return get(c, id).visible;
    }

    static synchronized void save(Context c, Cal cal) {
        if (cache == null) load(c);
        for (int i = 0; i < cache.size(); i++) {
            if (cache.get(i).id.equals(cal.id)) {
                cache.set(i, cal);
                persist(c);
                return;
            }
        }
        cache.add(Math.max(0, cache.size() - 1), cal);
        persist(c);
    }

    static synchronized void delete(Context c, String id) {
        if (cache == null) load(c);
        for (int i = 0; i < cache.size(); i++) {
            if (cache.get(i).id.equals(id) && !cache.get(i).builtin) {
                cache.remove(i);
                break;
            }
        }
        persist(c);
        if (Prefs.defaultCal(c).equals(id)) Prefs.setDefaultCal(c, PERSONAL);
    }

    static String newId() {
        return "c" + UUID.randomUUID().toString().substring(0, 8);
    }

    static String name(Context c, Cal x) {
        if (!x.name.isEmpty()) return x.name;
        if (PERSONAL.equals(x.id)) return c.getString(R.string.cal_personal);
        if (WORK.equals(x.id)) return c.getString(R.string.cal_work);
        if (HOLIDAYS.equals(x.id)) return c.getString(R.string.cal_holidays);
        return c.getString(R.string.new_calendar_default);
    }

    static int colorOf(Context c, Cal x) {
        return c.getColor(Event.COLORS[x.color]);
    }

    /** The colour an event is drawn in: its own, or its calendar's. */
    static int color(Context c, Event e) {
        int idx = e.color >= 0 ? e.color : get(c, e.cal).color;
        return c.getColor(Event.COLORS[idx]);
    }
}
