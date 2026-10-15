package com.semo.calendar;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

/** One calendar event. Repeats: 0 none, 1 daily, 2 weekly, 3 monthly, 4 yearly. */
final class Event {
    static final int[] COLORS = {R.color.accent, R.color.ok, R.color.warn, R.color.violet, R.color.bad};

    String id = UUID.randomUUID().toString();
    String title = "";
    String location = "";
    String notes = "";
    long start;
    long end;
    boolean allDay;
    int repeat;
    int remind = -1;
    int color;

    JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id).put("title", title).put("location", location).put("notes", notes)
                .put("start", start).put("end", end).put("allDay", allDay)
                .put("repeat", repeat).put("remind", remind).put("color", color);
        return o;
    }

    static Event from(JSONObject o) {
        Event e = new Event();
        e.id = o.optString("id", e.id);
        e.title = o.optString("title");
        e.location = o.optString("location");
        e.notes = o.optString("notes");
        e.start = o.optLong("start");
        e.end = o.optLong("end", e.start);
        e.allDay = o.optBoolean("allDay");
        e.repeat = o.optInt("repeat");
        e.remind = o.optInt("remind", -1);
        e.color = Math.max(0, Math.min(COLORS.length - 1, o.optInt("color")));
        return e;
    }
}
