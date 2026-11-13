package com.semo.calendar;

import android.content.Context;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Small on-device helpers: an emoji for a title, overlap detection, free-time search and the "today at a glance"
 * summary. Plain rules, no network.
 */
final class Smart {
    private Smart() {
    }

    private static final long QUARTER = 15 * 60000L;

    // ------------------------------------------------------------ emoji from the title

    private static boolean hasArabic(String s) {
        for (int i = 0; i < s.length(); i++) if (s.charAt(i) >= '\u0600' && s.charAt(i) <= '\u06FF') return true;
        return false;
    }

    /** An emoji that fits the title ("dinner" gives a plate), or an empty string. */
    static String emoji(Context c, String title) {
        String t = NlParser.norm(title);
        if (t.trim().length() < 3) return "";
        for (String rule : c.getResources().getStringArray(R.array.smart_emoji_rules)) {
            int eq = rule.indexOf('=');
            if (eq <= 0) continue;
            for (String kw : rule.substring(eq + 1).split("\\|")) {
                if (kw.length() < 3) continue;
                if (hasArabic(kw)) {
                    if (t.contains(kw)) return rule.substring(0, eq);
                } else if (Pattern.compile("(?<![\\p{L}\\p{N}_])" + Pattern.quote(kw) + "(?![\\p{L}\\p{N}_])")
                        .matcher(t).find()) {
                    return rule.substring(0, eq);
                }
            }
        }
        return "";
    }

    // ------------------------------------------------------------ conflicts and free time

    /** Timed events (not free, not holidays) that overlap [start, end). */
    static List<Recur.Occ> conflicts(Context c, long start, long end, String ignoreId) {
        List<Recur.Occ> out = new ArrayList<>();
        if (end <= start) end = start + 1;
        for (Recur.Occ o : Store.between(c, start, end)) {
            Event e = o.e;
            if (e.allDay || e.free || e.readOnly || e.id.equals(ignoreId)) continue;
            if (o.start < end && o.end > start) out.add(o);
        }
        return out;
    }

    /** How many timed events overlap the range (0 for all-day drafts). */
    static int conflictCount(Context c, long start, long end, boolean allDay) {
        return allDay ? 0 : conflicts(c, start, end, "").size();
    }

    private static long roundUp(long t) {
        return (t + QUARTER - 1) / QUARTER * QUARTER;
    }

    private static long atHour(long day, int hour) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(day);
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    /** Up to {@code max} free slots of {@code durMin} minutes between 09:00 and 21:00 in the next two weeks. */
    static List<long[]> freeSlots(Context c, int durMin, long from, int max) {
        List<long[]> out = new ArrayList<>();
        long dur = Math.max(5, durMin) * 60000L;
        long day = Recur.dayStart(from);
        for (int d = 0; d < 14 && out.size() < max; d++) {
            long ws = atHour(day, 9);
            long we = atHour(day, 21);
            List<Recur.Occ> busy = new ArrayList<>();
            for (Recur.Occ o : Store.between(c, ws, we)) {
                if (!o.e.allDay && !o.e.free && !o.e.readOnly) busy.add(o);
            }
            long cursor = Math.max(ws, roundUp(from));
            int guard = 0;
            while (cursor + dur <= we && out.size() < max && guard++ < 300) {
                long next = cursor;
                boolean hit = false;
                for (Recur.Occ o : busy) {
                    if (o.start < cursor + dur && o.end > cursor) {
                        next = Math.max(next, roundUp(o.end));
                        hit = true;
                    }
                }
                if (hit) {
                    cursor = next;
                    continue;
                }
                out.add(new long[]{cursor, cursor + dur});
                cursor += Math.max(dur, 60 * 60000L);
            }
            day = Recur.nextDay(day);
        }
        return out;
    }

    // ------------------------------------------------------------ today at a glance

    /** Lines for the summary card at the top of the agenda. */
    static List<String> glance(Context c) {
        List<String> lines = new ArrayList<>();
        long now = System.currentTimeMillis();
        long d0 = Recur.dayStart(now);
        long d1 = Recur.nextDay(d0);
        int events = 0;
        long first = Long.MAX_VALUE;
        for (Recur.Occ o : Store.between(c, d0, d1)) {
            if (o.e.readOnly) continue;
            events++;
            if (!o.e.allDay) first = Math.min(first, o.start);
        }
        int dueToday = 0, overdue = 0;
        for (Task t : Tasks.all(c)) {
            if (t.done || t.due <= 0) continue;
            if (t.due >= d0 && t.due < d1 && (!t.hasTime || t.due >= now)) dueToday++;
            else if (t.hasTime ? t.due < now : t.due < d0) overdue++;
        }
        ClockLink.Summary clk = ClockLink.read(c);
        if (events == 0 && dueToday == 0 && overdue == 0) {
            lines.add(c.getString(R.string.glance_free));
            addClockLines(c, lines, clk, now);
            return lines;
        }
        if (events > 0) {
            lines.add(first == Long.MAX_VALUE ? c.getString(R.string.glance_events_all, events)
                    : c.getString(R.string.glance_events, events, Fmt.time(c, first)));
            List<long[]> slots = freeSlots(c, 60, now, 1);
            if (!slots.isEmpty()) {
                long s = slots.get(0)[0];
                String when = Fmt.time(c, s);
                if (Recur.dayStart(s) != d0) {
                    String rel = Fmt.dayLabel(c, Recur.dayStart(s));
                    when = (rel != null ? rel : Fmt.date("EEEdMMM", s)) + " " + when;
                }
                lines.add(c.getString(R.string.glance_next_free, when));
            }
        }
        if (dueToday > 0) lines.add(c.getString(R.string.glance_tasks, dueToday));
        if (overdue > 0) lines.add(c.getString(R.string.glance_overdue, overdue));
        addClockLines(c, lines, clk, now);
        return lines;
    }

    /** What the Semo Clock knows: the next alarm and the next prayer (only when that app is installed). */
    private static void addClockLines(Context c, List<String> lines, ClockLink.Summary k, long now) {
        if (k == null) return;
        long limit = now + 24L * 3600 * 1000;
        if (k.nextAlarm > now && k.nextAlarm < limit) {
            lines.add(c.getString(R.string.glance_alarm, Fmt.time(c, k.nextAlarm)));
        }
        if (k.prayerAt > now && k.prayerAt < limit && !k.prayerName.isEmpty()) {
            lines.add(c.getString(R.string.glance_prayer, k.prayerName, Fmt.time(c, k.prayerAt)));
        }
    }
}
