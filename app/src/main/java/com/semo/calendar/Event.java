package com.semo.calendar;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

/**
 * One calendar event. Repeats: 0 none, 1 daily, 2 weekly, 3 monthly, 4 yearly, 5 every 2 weeks, 6 weekdays.
 * color -1 means "use the colour of the calendar".
 */
final class Event {
    static final int[] COLORS = {R.color.accent, R.color.ok, R.color.warn, R.color.violet, R.color.bad,
            R.color.cal_teal, R.color.cal_pink, R.color.cal_indigo, R.color.cal_yellow};

    String id = UUID.randomUUID().toString();
    String title = "";
    String location = "";
    String notes = "";
    String url = "";
    String emoji = "";
    String cal = Cals.PERSONAL;
    long start;
    long end;
    long until;
    boolean allDay;
    boolean free;
    boolean readOnly;
    int repeat;
    int count;
    int remind = -1;
    int remind2 = -1;
    int travel;
    int color = -1;

    JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id).put("title", title).put("location", location).put("notes", notes)
                .put("start", start).put("end", end).put("allDay", allDay)
                .put("repeat", repeat).put("remind", remind).put("color", color)
                .put("url", url).put("emoji", emoji).put("cal", cal).put("until", until)
                .put("count", count).put("remind2", remind2).put("travel", travel).put("free", free);
        return o;
    }

    static Event from(JSONObject o) {
        Event e = new Event();
        e.id = o.optString("id", e.id);
        e.title = o.optString("title");
        e.location = o.optString("location");
        e.notes = o.optString("notes");
        e.url = o.optString("url");
        e.emoji = o.optString("emoji");
        e.cal = o.optString("cal", Cals.PERSONAL);
        e.start = o.optLong("start");
        e.end = o.optLong("end", e.start);
        e.until = o.optLong("until");
        e.allDay = o.optBoolean("allDay");
        e.free = o.optBoolean("free");
        e.repeat = o.optInt("repeat");
        e.count = o.optInt("count");
        e.remind = o.optInt("remind", -1);
        e.remind2 = o.optInt("remind2", -1);
        e.travel = o.optInt("travel");
        e.color = Math.max(-1, Math.min(COLORS.length - 1, o.optInt("color", -1)));
        return e;
    }

    /** A copy with a new id (Duplicate). */
    Event copy() {
        Event e = new Event();
        e.title = title;
        e.location = location;
        e.notes = notes;
        e.url = url;
        e.emoji = emoji;
        e.cal = cal;
        e.start = start;
        e.end = end;
        e.until = until;
        e.allDay = allDay;
        e.free = free;
        e.repeat = repeat;
        e.count = count;
        e.remind = remind;
        e.remind2 = remind2;
        e.travel = travel;
        e.color = color;
        return e;
    }

    /** Title with its emoji in front. */
    String label(String fallback) {
        String t = title.isEmpty() ? fallback : title;
        return emoji.isEmpty() ? t : emoji + " " + t;
    }
}
