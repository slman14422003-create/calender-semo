package com.semo.calendar;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Events live in one private JSON file; the list is cached in memory. */
final class Store {
    private Store() {
    }

    private static List<Event> cache;

    private static File file(Context c) {
        return new File(c.getApplicationContext().getFilesDir(), "events.json");
    }

    private static void load(Context c) {
        cache = new ArrayList<>();
        File f = file(c);
        if (!f.exists()) return;
        try (InputStream in = new FileInputStream(f)) {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
            JSONArray a = new JSONArray(new String(bo.toByteArray(), StandardCharsets.UTF_8));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.optJSONObject(i);
                if (o != null) cache.add(Event.from(o));
            }
        } catch (Exception ignored) {
        }
    }

    private static void persist(Context c) {
        try {
            JSONArray a = new JSONArray();
            for (Event e : cache) a.put(e.toJson());
            File f = file(c);
            File tmp = new File(f.getPath() + ".tmp");
            try (FileOutputStream out = new FileOutputStream(tmp)) {
                out.write(a.toString().getBytes(StandardCharsets.UTF_8));
                out.getFD().sync();
            }
            if (!tmp.renameTo(f)) {
                f.delete();
                tmp.renameTo(f);
            }
        } catch (Exception ignored) {
        }
    }

    static synchronized List<Event> all(Context c) {
        if (cache == null) load(c);
        return new ArrayList<>(cache);
    }

    static synchronized Event get(Context c, String id) {
        if (cache == null) load(c);
        for (Event e : cache) if (e.id.equals(id)) return e;
        return null;
    }

    static synchronized void save(Context c, Event ev) {
        if (cache == null) load(c);
        for (int i = 0; i < cache.size(); i++) {
            if (cache.get(i).id.equals(ev.id)) {
                cache.set(i, ev);
                persist(c);
                return;
            }
        }
        cache.add(ev);
        persist(c);
    }

    static synchronized void delete(Context c, String id) {
        if (cache == null) load(c);
        for (int i = 0; i < cache.size(); i++) {
            if (cache.get(i).id.equals(id)) {
                cache.remove(i);
                break;
            }
        }
        persist(c);
    }

    /** Occurrences in [from, to) of the events of visible calendars (and the holidays when they are on). */
    static List<Recur.Occ> between(Context c, long from, long to) {
        List<Event> evs = new ArrayList<>();
        for (Event e : all(c)) if (Cals.visible(c, e.cal)) evs.add(e);
        if (Cals.visible(c, Cals.HOLIDAYS)) evs.addAll(Holidays.events(c, from, to));
        return Recur.between(evs, from, to);
    }

    static synchronized void deleteByCal(Context c, String calId) {
        if (cache == null) load(c);
        for (int i = cache.size() - 1; i >= 0; i--) {
            if (cache.get(i).cal.equals(calId)) {
                Alarms.cancel(c, cache.get(i).id);
                cache.remove(i);
            }
        }
        persist(c);
    }
}
