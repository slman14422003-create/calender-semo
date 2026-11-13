package com.semo.calendar;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

/** A to-do with an optional due date (and time) and an optional alert at the due time. */
final class Task {
    String id = UUID.randomUUID().toString();
    String title = "";
    String notes = "";
    long due;
    boolean hasTime;
    boolean done;
    boolean remind;

    JSONObject toJson() throws JSONException {
        return new JSONObject().put("id", id).put("title", title).put("notes", notes).put("due", due)
                .put("hasTime", hasTime).put("done", done).put("remind", remind);
    }

    static Task from(JSONObject o) {
        Task t = new Task();
        t.id = o.optString("id", t.id);
        t.title = o.optString("title");
        t.notes = o.optString("notes");
        t.due = o.optLong("due");
        t.hasTime = o.optBoolean("hasTime");
        t.done = o.optBoolean("done");
        t.remind = o.optBoolean("remind");
        return t;
    }
}
