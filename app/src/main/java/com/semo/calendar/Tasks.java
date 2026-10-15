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
import java.util.Collections;
import java.util.List;

/** Tasks live in their own private JSON file, cached in memory like the events. */
final class Tasks {
    private Tasks() {
    }

    private static List<Task> cache;

    private static File file(Context c) {
        return new File(c.getApplicationContext().getFilesDir(), "tasks.json");
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
                if (o != null) cache.add(Task.from(o));
            }
        } catch (Exception ignored) {
        }
    }

    private static void persist(Context c) {
        try {
            JSONArray a = new JSONArray();
            for (Task t : cache) a.put(t.toJson());
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

    static synchronized List<Task> all(Context c) {
        if (cache == null) load(c);
        return new ArrayList<>(cache);
    }

    static synchronized Task get(Context c, String id) {
        if (cache == null) load(c);
        for (Task t : cache) if (t.id.equals(id)) return t;
        return null;
    }

    static synchronized void save(Context c, Task t) {
        if (cache == null) load(c);
        for (int i = 0; i < cache.size(); i++) {
            if (cache.get(i).id.equals(t.id)) {
                cache.set(i, t);
                persist(c);
                return;
            }
        }
        cache.add(t);
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

    static synchronized void clearDone(Context c) {
        if (cache == null) load(c);
        for (int i = cache.size() - 1; i >= 0; i--) if (cache.get(i).done) cache.remove(i);
        persist(c);
    }

    /** Tasks whose due date falls in [from, to), earliest first. */
    static List<Task> between(Context c, long from, long to) {
        List<Task> out = new ArrayList<>();
        for (Task t : all(c)) if (t.due >= from && t.due < to) out.add(t);
        Collections.sort(out, (a, b) -> Long.compare(a.due, b.due));
        return out;
    }
}
