package com.semo.calendar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Minimal iCalendar (.ics) writer and reader: title, place, notes, link, start, end, all-day, repeat (with
 * interval, weekdays, until and count), free / busy and up to two alerts.
 */
final class Ics {
    private Ics() {
    }

    private static String esc(String s) {
        return s.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,")
                .replace("\r", "").replace("\n", "\\n");
    }

    private static String unesc(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '\\' && i + 1 < s.length()) {
                char n = s.charAt(++i);
                sb.append(n == 'n' || n == 'N' ? '\n' : n);
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    private static final String[] FREQ = {"", "DAILY", "WEEKLY", "MONTHLY", "YEARLY"};
    private static final String WORK_DAYS = "MO,TU,WE,TH,FR";

    private static void alarm(StringBuilder sb, int minutes) {
        sb.append("BEGIN:VALARM\r\nACTION:DISPLAY\r\nDESCRIPTION:Reminder\r\nTRIGGER:-PT").append(minutes)
                .append("M\r\nEND:VALARM\r\n");
    }

    static String export(List<Event> events) {
        SimpleDateFormat utc = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US);
        utc.setTimeZone(TimeZone.getTimeZone("UTC"));
        SimpleDateFormat day = new SimpleDateFormat("yyyyMMdd", Locale.US);
        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCALENDAR\r\nVERSION:2.0\r\nPRODID:-//Semo//Calendar//EN\r\nCALSCALE:GREGORIAN\r\n");
        String stamp = utc.format(new Date());
        for (Event e : events) {
            sb.append("BEGIN:VEVENT\r\nUID:").append(e.id).append("\r\nDTSTAMP:").append(stamp).append("\r\n");
            if (e.allDay) {
                sb.append("DTSTART;VALUE=DATE:").append(day.format(new Date(e.start))).append("\r\n");
                sb.append("DTEND;VALUE=DATE:").append(day.format(new Date(Recur.nextDay(Recur.dayStart(e.end)))))
                        .append("\r\n");
            } else {
                sb.append("DTSTART:").append(utc.format(new Date(e.start))).append("\r\n");
                sb.append("DTEND:").append(utc.format(new Date(e.end))).append("\r\n");
            }
            sb.append("SUMMARY:").append(esc(e.title)).append("\r\n");
            if (!e.location.isEmpty()) sb.append("LOCATION:").append(esc(e.location)).append("\r\n");
            if (!e.notes.isEmpty()) sb.append("DESCRIPTION:").append(esc(e.notes)).append("\r\n");
            if (!e.url.isEmpty()) sb.append("URL:").append(e.url).append("\r\n");
            if (e.free) sb.append("TRANSP:TRANSPARENT\r\n");
            if (e.repeat > 0) {
                String rule = e.repeat == 5 ? "FREQ=WEEKLY;INTERVAL=2"
                        : e.repeat == 6 ? "FREQ=WEEKLY;BYDAY=" + WORK_DAYS : "FREQ=" + FREQ[e.repeat];
                if (e.until > 0) rule += ";UNTIL=" + utc.format(new Date(e.until));
                else if (e.count > 0) rule += ";COUNT=" + e.count;
                sb.append("RRULE:").append(rule).append("\r\n");
            }
            if (e.remind >= 0) alarm(sb, e.remind);
            if (e.remind2 >= 0) alarm(sb, e.remind2);
            sb.append("END:VEVENT\r\n");
        }
        sb.append("END:VCALENDAR\r\n");
        return sb.toString();
    }

    private static long parseDate(String v) {
        try {
            boolean utc = v.endsWith("Z");
            if (utc) v = v.substring(0, v.length() - 1);
            int y = Integer.parseInt(v.substring(0, 4));
            int mo = Integer.parseInt(v.substring(4, 6));
            int d = Integer.parseInt(v.substring(6, 8));
            int h = 0, mi = 0, s = 0;
            if (v.length() >= 15) {
                h = Integer.parseInt(v.substring(9, 11));
                mi = Integer.parseInt(v.substring(11, 13));
                s = Integer.parseInt(v.substring(13, 15));
            }
            Calendar c = utc ? Calendar.getInstance(TimeZone.getTimeZone("UTC")) : Calendar.getInstance();
            c.clear();
            c.set(y, mo - 1, d, h, mi, s);
            return c.getTimeInMillis();
        } catch (Exception e) {
            return -1;
        }
    }

    /** "-PT15M", "-PT1H", "-P1D", "PT0S" to minutes before the start; -1 when unknown. */
    private static int parseTrigger(String v) {
        try {
            boolean neg = v.startsWith("-");
            String t = v.replace("-", "").replace("+", "");
            int total = 0;
            int num = 0;
            boolean time = false;
            for (int i = 0; i < t.length(); i++) {
                char ch = t.charAt(i);
                if (ch == 'P') continue;
                if (ch == 'T') {
                    time = true;
                } else if (Character.isDigit(ch)) {
                    num = num * 10 + (ch - '0');
                } else {
                    if (ch == 'W') total += num * 7 * 1440;
                    else if (ch == 'D') total += num * 1440;
                    else if (ch == 'H' && time) total += num * 60;
                    else if (ch == 'M' && time) total += num;
                    num = 0;
                }
            }
            return neg || total == 0 ? total : -1;
        } catch (Exception e) {
            return -1;
        }
    }

    private static void rule(Event e, String val) {
        String freq = "", byday = "";
        int interval = 1, count = 0;
        long until = 0;
        for (String part : val.split(";")) {
            String[] kv = part.split("=", 2);
            if (kv.length < 2) continue;
            switch (kv[0]) {
                case "FREQ":
                    freq = kv[1];
                    break;
                case "INTERVAL":
                    try {
                        interval = Integer.parseInt(kv[1]);
                    } catch (Exception ignored) {
                    }
                    break;
                case "BYDAY":
                    byday = kv[1];
                    break;
                case "UNTIL":
                    until = parseDate(kv[1]);
                    break;
                case "COUNT":
                    try {
                        count = Integer.parseInt(kv[1]);
                    } catch (Exception ignored) {
                    }
                    break;
                default:
                    break;
            }
        }
        boolean work = byday.equals(WORK_DAYS);
        int rep = 0;
        if (freq.equals("DAILY")) rep = work ? 6 : 1;
        else if (freq.equals("WEEKLY")) rep = work ? 6 : interval == 2 ? 5 : 2;
        else if (freq.equals("MONTHLY")) rep = 3;
        else if (freq.equals("YEARLY")) rep = 4;
        e.repeat = rep;
        if (rep > 0) {
            e.until = Math.max(0, until);
            e.count = count;
        }
    }

    static List<Event> parse(String text) {
        List<Event> out = new ArrayList<>();
        // unfold continuation lines
        String[] raw = text.replace("\r\n", "\n").replace('\r', '\n').split("\n");
        List<String> lines = new ArrayList<>();
        for (String l : raw) {
            if ((l.startsWith(" ") || l.startsWith("\t")) && !lines.isEmpty()) {
                lines.set(lines.size() - 1, lines.get(lines.size() - 1) + l.substring(1));
            } else {
                lines.add(l);
            }
        }
        Event e = null;
        boolean allDay = false, hasEnd = false, inAlarm = false;
        long end = 0;
        for (String l : lines) {
            if (l.equals("BEGIN:VEVENT")) {
                e = new Event();
                e.start = -1;
                allDay = false;
                hasEnd = false;
                inAlarm = false;
                continue;
            }
            if (e == null) continue;
            if (l.equals("BEGIN:VALARM")) {
                inAlarm = true;
                continue;
            }
            if (l.equals("END:VALARM")) {
                inAlarm = false;
                continue;
            }
            if (l.equals("END:VEVENT")) {
                if (e.start >= 0) {
                    e.allDay = allDay;
                    if (allDay) {
                        long dayEnd = hasEnd ? end - 1 : e.start + Recur.DAY - 1;
                        e.end = Math.max(dayEnd, e.start);
                    } else {
                        e.end = hasEnd ? Math.max(end, e.start) : e.start + 3600000L;
                    }
                    out.add(e);
                }
                e = null;
                continue;
            }
            int colon = l.indexOf(':');
            if (colon < 0) continue;
            String head = l.substring(0, colon).toUpperCase(Locale.US);
            String val = l.substring(colon + 1);
            String name = head.contains(";") ? head.substring(0, head.indexOf(';')) : head;
            if (inAlarm) {
                if (name.equals("TRIGGER")) {
                    int m = parseTrigger(val);
                    if (e.remind < 0) e.remind = m;
                    else if (e.remind2 < 0) e.remind2 = m;
                }
                continue;
            }
            switch (name) {
                case "UID":
                    if (!val.isEmpty() && val.length() < 200) e.id = val;
                    break;
                case "SUMMARY":
                    e.title = unesc(val);
                    break;
                case "LOCATION":
                    e.location = unesc(val);
                    break;
                case "DESCRIPTION":
                    e.notes = unesc(val);
                    break;
                case "URL":
                    e.url = val;
                    break;
                case "TRANSP":
                    e.free = val.equalsIgnoreCase("TRANSPARENT");
                    break;
                case "DTSTART":
                    allDay = head.contains("VALUE=DATE") && !head.contains("DATE-TIME") || val.length() == 8;
                    e.start = parseDate(val);
                    break;
                case "DTEND":
                    end = parseDate(val);
                    hasEnd = end >= 0;
                    break;
                case "RRULE":
                    rule(e, val);
                    break;
                default:
                    break;
            }
        }
        return out;
    }
}
