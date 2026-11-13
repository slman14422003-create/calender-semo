package com.semo.calendar;

import android.content.Context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Smart add: turns a sentence such as "lunch with Sara tomorrow at 1pm for 1 hour" into an event or task draft.
 * Everything runs on the device with plain rules (no network, no model download). The vocabulary for English, Arabic
 * (including Levantine words), French, Spanish, German and Turkish lives in res/values/nlp.xml.
 */
final class NlParser {
    private NlParser() {
    }

    static final class Draft {
        String title = "";
        String location = "";
        long start;        // 0 = no date or time was found
        long end;          // 0 = no end / length was found
        boolean hasDate, hasTime, allDay, task;
        int repeat;
        int remind = -1;   // minutes before, -1 = not given
        int durationMin;
    }

    private static final String W = "\\p{L}\\p{N}_";
    private static final String NL = "(?<![" + W + "])";
    private static final String NR = "(?![" + W + "])";
    private static final String[] MARKS = {"am", "pm", "morning", "noon", "afternoon", "evening", "night"};
    private static final String[] PERIODS = {"morning", "noon", "afternoon", "evening", "night"};
    private static final int[] PERIOD_HOUR = {9, 12, 15, 18, 21};
    private static final String[] REC_KEYS = {"rec_biweekly", "rec_weekdays", "rec_daily", "rec_weekly",
            "rec_monthly", "rec_yearly"};
    private static final int[] REC_CODE = {5, 6, 1, 2, 3, 4};

    // ---------------------------------------------------------------- vocabulary -> patterns

    private static String quote(String entry) {
        String[] words = entry.trim().split(" +");
        StringBuilder sb = new StringBuilder();
        for (int k = 0; k < words.length; k++) {
            if (k > 0) sb.append("\\s+");
            sb.append(Pattern.quote(words[k]));
        }
        return sb.toString();
    }

    private static String alt(Context c, String... keys) {
        List<String> entries = new ArrayList<>();
        for (String k : keys) entries.addAll(Arrays.asList(NlLex.get(c, k).split("\\|", -1)));
        java.util.Collections.sort(entries, (a, b) -> b.length() - a.length());
        StringBuilder sb = new StringBuilder("(?:");
        boolean first = true;
        for (String e : entries) {
            if (e.trim().isEmpty()) continue;
            if (!first) sb.append('|');
            first = false;
            sb.append(quote(e));
        }
        return sb.append(')').toString();
    }

    private static final class Pats {
        Pattern allDay, cue, recWd, b1, b2, d1, d2, relA, relS, tRange, tColon, tH, tPrep, tMark;
        Pattern dIso, dDm, dMd, dNum, after, tom, tonight, today, wd, loc, fromStart;
        Pattern[] rec = new Pattern[REC_KEYS.length];
        Pattern[] period = new Pattern[PERIODS.length];
        Pattern[] wdP = new Pattern[7];
        Pattern[] moP = new Pattern[12];
        Pattern[] markP = new Pattern[MARKS.length];
        Pattern uMin, uHour, uDay, uWeek, dual, half, quarter, third, qless, tless;
        Set<String> stop = new HashSet<>();
    }

    private static Pats pats;

    private static synchronized Pats pats(Context c) {
        if (pats != null) return pats;
        Pats p = new Pats();
        String wds = alt(c, "wd1", "wd2", "wd3", "wd4", "wd5", "wd6", "wd7");
        String month = alt(c, "mo1", "mo2", "mo3", "mo4", "mo5", "mo6", "mo7", "mo8", "mo9", "mo10", "mo11", "mo12");
        String mark = alt(c, MARKS);
        String unitTime = alt(c, "u_min", "u_hour");
        String unitDay = alt(c, "u_day");
        String unit = alt(c, "u_min", "u_hour", "u_day", "u_week");
        String prep = alt(c, "at");
        String frac = "(?:\\s*(" + alt(c, "half", "quarter", "third", "qless", "tless") + "))?";
        String markOpt = "(" + mark + "(?![" + W + "]))?";

        p.allDay = Pattern.compile(NL + alt(c, "allday") + NR);
        p.cue = Pattern.compile(NL + alt(c, "cue") + NR);
        p.recWd = Pattern.compile(NL + alt(c, "every") + "\\s+(?:" + alt(c, "dayword") + "\\s+)?(" + wds + ")" + NR);
        for (int i = 0; i < REC_KEYS.length; i++) p.rec[i] = Pattern.compile(NL + alt(c, REC_KEYS[i]) + NR);
        p.b1 = Pattern.compile(NL + "(\\d+)\\s*(" + unitTime + "|" + unitDay + ")\\s*" + alt(c, "before") + NR);
        p.b2 = Pattern.compile(NL + alt(c, "before") + "\\s+(?:(\\d+)\\s*)?(" + unitTime + "|" + unitDay + ")" + NR);
        p.d1 = Pattern.compile(NL + alt(c, "for") + "\\s*(?:(\\d+(?:[.,]\\d+)?)\\s*)?(" + unitTime + ")" + NR);
        p.d2 = Pattern.compile(NL + "(\\d+(?:[.,]\\d+)?)\\s*(" + unitTime + ")\\s*" + alt(c, "dursuf") + NR);
        p.relA = Pattern.compile(NL + alt(c, "in") + "\\s*(?:" + alt(c, "an") + "\\s+)?(?:(\\d+)\\s*)?(" + unit + ")" + NR);
        p.relS = Pattern.compile(NL + "(\\d+)\\s*(" + unit + ")\\s*sonra" + NR);
        p.fromStart = Pattern.compile(alt(c, "from"));
        p.tRange = Pattern.compile(NL + "(?:" + alt(c, "from") + "\\s+)?(?:" + prep + "\\s*)?(\\d{1,2})(?::(\\d{2}))?\\s*"
                + markOpt + "\\s*" + alt(c, "to") + "\\s*(\\d{1,2})(?::(\\d{2}))?\\s*" + markOpt + "(?![" + W + ":])");
        p.tColon = Pattern.compile(NL + "(?:" + prep + "\\s*)?(\\d{1,2}):(\\d{2})\\s*" + markOpt + NR);
        p.tH = Pattern.compile(NL + "(?:" + prep + "\\s*)?(\\d{1,2})\\s*(?:h|uhr)\\s*(\\d{2})?\\s*" + markOpt + NR);
        p.tMark = Pattern.compile(NL + "(?:" + prep + "\\s*)?(\\d{1,2})" + frac + "\\s*(" + mark + ")" + NR);
        p.tPrep = Pattern.compile(NL + prep + "\\s*(\\d{1,2})" + frac + "(?:\\s*uhr)?(?:\\s*" + markOpt + ")?(?![\\d:])");
        p.dIso = Pattern.compile(NL + "(\\d{4})-(\\d{1,2})-(\\d{1,2})" + NR);
        p.dDm = Pattern.compile(NL + "(\\d{1,2})(?:st|nd|rd|th|er)?\\s*(?:" + alt(c, "of") + "\\s+)?(" + month
                + ")(?:\\s+(\\d{4}))?" + NR);
        p.dMd = Pattern.compile(NL + "(" + month + ")\\s+(\\d{1,2})(?:st|nd|rd|th)?(?:,?\\s*(\\d{4}))?" + NR);
        p.dNum = Pattern.compile("(?<![" + W + ":./-])(\\d{1,2})[/.\\-](\\d{1,2})(?:[/.\\-](\\d{2,4}))?(?![" + W + ":])");
        p.after = Pattern.compile(NL + alt(c, "aftertomorrow") + NR);
        p.tom = Pattern.compile(NL + alt(c, "tomorrow") + NR);
        p.tonight = Pattern.compile(NL + alt(c, "tonight") + NR);
        p.today = Pattern.compile(NL + alt(c, "today") + NR);
        p.wd = Pattern.compile(NL + "(?:" + alt(c, "next") + "\\s+)?(?:" + alt(c, "dayword") + "\\s+)?(?:" + alt(c, "pfx")
                + ")?(" + wds + ")" + NR);
        p.loc = Pattern.compile("(?:^|\\s)" + alt(c, "locprep") + "\\s+(\\S.*)$");
        for (int i = 0; i < PERIODS.length; i++) p.period[i] = Pattern.compile(NL + alt(c, PERIODS[i]) + NR);
        for (int i = 0; i < 7; i++) p.wdP[i] = Pattern.compile(alt(c, "wd" + (i + 1)));
        for (int i = 0; i < 12; i++) p.moP[i] = Pattern.compile(alt(c, "mo" + (i + 1)));
        for (int i = 0; i < MARKS.length; i++) p.markP[i] = Pattern.compile(alt(c, MARKS[i]));
        p.uMin = Pattern.compile(alt(c, "u_min"));
        p.uHour = Pattern.compile(alt(c, "u_hour"));
        p.uDay = Pattern.compile(alt(c, "u_day"));
        p.uWeek = Pattern.compile(alt(c, "u_week"));
        p.dual = Pattern.compile(alt(c, "dual"));
        p.half = Pattern.compile(alt(c, "half"));
        p.quarter = Pattern.compile(alt(c, "quarter"));
        p.third = Pattern.compile(alt(c, "third"));
        p.qless = Pattern.compile(alt(c, "qless"));
        p.tless = Pattern.compile(alt(c, "tless"));
        p.stop.addAll(Arrays.asList(NlLex.get(c, "stop").split("\\|")));
        pats = p;
        return p;
    }

    // ---------------------------------------------------------------- helpers

    /** Same length as the input: digits, Arabic letter variants and case are folded one character to one. */
    static String norm(String s) {
        char[] a = s.toCharArray();
        for (int i = 0; i < a.length; i++) {
            char ch = a[i];
            if (ch >= '\u0660' && ch <= '\u0669') ch = (char) ('0' + ch - '\u0660');
            else if (ch >= '\u06F0' && ch <= '\u06F9') ch = (char) ('0' + ch - '\u06F0');
            else if (ch == '\u0623' || ch == '\u0625' || ch == '\u0622') ch = '\u0627';
            else if (ch == '\u0649') ch = '\u064A';
            else if (ch == '\u0629') ch = '\u0647';
            else ch = Character.toLowerCase(ch);
            a[i] = ch;
        }
        return new String(a);
    }

    private interface Ok {
        boolean test(Matcher m);
    }

    private static final class Work {
        final StringBuilder w;
        final boolean[] cut;

        Work(String n) {
            w = new StringBuilder(n);
            cut = new boolean[n.length()];
        }

        Matcher find(Pattern p, Ok ok) {
            Matcher m = p.matcher(w.toString());
            while (m.find()) {
                if (ok == null || ok.test(m)) return m;
            }
            return null;
        }

        Matcher find(Pattern p) {
            return find(p, null);
        }

        void take(Matcher m) {
            for (int i = m.start(); i < m.end(); i++) {
                w.setCharAt(i, ' ');
                cut[i] = true;
            }
        }
    }

    private static int wdIndex(Pats p, String word) {
        for (int i = 0; i < 7; i++) if (p.wdP[i].matcher(word).matches()) return i + 1;
        return 0;
    }

    private static int moIndex(Pats p, String word) {
        for (int i = 0; i < 12; i++) if (p.moP[i].matcher(word).matches()) return i + 1;
        return 0;
    }

    private static int unitMinutes(Pats p, String word) {
        if (p.uMin.matcher(word).matches()) return 1;
        if (p.uHour.matcher(word).matches()) return 60;
        if (p.uDay.matcher(word).matches()) return 1440;
        if (p.uWeek.matcher(word).matches()) return 10080;
        return 1;
    }

    private static int implicit(Pats p, String word) {
        return p.dual.matcher(word).matches() ? 2 : 1;
    }

    private static String classify(Pats p, String mk) {
        if (mk == null) return null;
        for (int i = 0; i < MARKS.length; i++) if (p.markP[i].matcher(mk).matches()) return MARKS[i];
        return null;
    }

    private static int hourWith(int h, String g) {
        switch (g) {
            case "am":
            case "morning":
                return h == 12 ? 0 : h;
            case "noon":
                return h <= 5 ? h + 12 : h;
            case "pm":
            case "afternoon":
            case "evening":
                return h < 12 ? h + 12 : h;
            case "night":
                return h == 12 ? 0 : (h >= 6 && h < 12 ? h + 12 : h);
            default:
                return h;
        }
    }

    private static int fracMin(Pats p, String f) {
        if (f == null) return 0;
        if (p.half.matcher(f).matches()) return 30;
        if (p.quarter.matcher(f).matches()) return 15;
        if (p.third.matcher(f).matches()) return 20;
        if (p.qless.matcher(f).matches()) return -15;
        if (p.tless.matcher(f).matches()) return -20;
        return 0;
    }

    private static int leadMinutes(Pats p, String count, String unit) {
        int c = count != null ? Integer.parseInt(count) : implicit(p, unit);
        return c * unitMinutes(p, unit);
    }

    private static long midnight(long t) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(t);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private static long addDays(long t, int n) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(t);
        c.add(Calendar.DAY_OF_YEAR, n);
        return c.getTimeInMillis();
    }

    private static long at(long day, int h, int m) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(day);
        c.set(Calendar.HOUR_OF_DAY, h);
        c.set(Calendar.MINUTE, m);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    /** Midnight of a real calendar date, or -1 when the date does not exist. */
    private static long date(int y, int mo, int da) {
        Calendar c = Calendar.getInstance();
        c.setLenient(false);
        c.clear();
        try {
            c.set(y, mo - 1, da, 0, 0, 0);
            return c.getTimeInMillis();
        } catch (RuntimeException e) {
            return -1;
        }
    }

    private static int fixYear(int mo, int da, int y, long now, long today0) {
        if (y > 0) return y > 99 ? y : 2000 + y;
        Calendar n = Calendar.getInstance();
        n.setTimeInMillis(now);
        int year = n.get(Calendar.YEAR);
        long cand = date(year, mo, da);
        return cand != -1 && cand < today0 ? year + 1 : year;
    }

    private static boolean isStopWord(Pats p, String word) {
        return p.stop.contains(norm(word));
    }

    private static String trimEnds(String s) {
        int a = 0, b = s.length();
        String junk = " ,.:;-\u2013";
        while (a < b && junk.indexOf(s.charAt(a)) >= 0) a++;
        while (b > a && junk.indexOf(s.charAt(b - 1)) >= 0) b--;
        return s.substring(a, b);
    }

    // ---------------------------------------------------------------- parser

    static Draft parse(Context ctx, String text, long now) {
        try {
            return parseInner(pats(ctx.getApplicationContext()), text, now);
        } catch (RuntimeException e) {
            Draft d = new Draft();
            d.title = text.trim();
            return d;
        }
    }

    private static Draft parseInner(Pats P, String text, long now) {
        Draft d = new Draft();
        Work work = new Work(norm(text));
        boolean us = "US".equals(Locale.getDefault().getCountry());
        long today0 = midnight(now);
        Calendar nowC = Calendar.getInstance();
        nowC.setTimeInMillis(now);
        int nowMin = nowC.get(Calendar.HOUR_OF_DAY) * 60 + nowC.get(Calendar.MINUTE);

        Matcher m = work.find(P.allDay);
        if (m != null) {
            d.allDay = true;
            work.take(m);
        }
        m = work.find(P.cue);
        if (m != null) {
            d.task = true;
            work.take(m);
        }

        // repeat
        int wdTarget = 0;
        m = work.find(P.recWd);
        if (m != null) {
            d.repeat = 2;
            wdTarget = wdIndex(P, m.group(1));
            work.take(m);
        } else {
            for (int i = 0; i < REC_KEYS.length; i++) {
                m = work.find(P.rec[i]);
                if (m != null) {
                    d.repeat = REC_CODE[i];
                    work.take(m);
                    break;
                }
            }
        }

        // alert lead time
        m = work.find(P.b1);
        if (m == null) m = work.find(P.b2);
        if (m != null) {
            d.remind = leadMinutes(P, m.group(1), m.group(2));
            work.take(m);
        }

        // length
        m = work.find(P.d1);
        if (m == null) m = work.find(P.d2);
        if (m != null) {
            String cnt = m.group(1);
            String unit = m.group(2);
            double val = cnt != null ? Double.parseDouble(cnt.replace(',', '.')) : implicit(P, unit);
            d.durationMin = (int) Math.round(val * unitMinutes(P, unit));
            work.take(m);
        }

        // relative: "in 2 hours", "after a week"
        long relExact = 0;
        long relDay = -1;
        m = work.find(P.relA);
        if (m == null) m = work.find(P.relS);
        if (m != null) {
            String cnt = m.group(1);
            String unit = m.group(2);
            int k = cnt != null ? Integer.parseInt(cnt) : implicit(P, unit);
            int mins = unitMinutes(P, unit);
            if (mins <= 60) {
                relExact = now + k * mins * 60000L;
                d.hasDate = true;
                d.hasTime = true;
            } else {
                relDay = addDays(today0, (int) (k * (long) mins / 1440));
                d.hasDate = true;
            }
            work.take(m);
        }

        // time of day
        int tmH = -1, tmM = 0, endH = -1, endM = 0;
        m = work.find(P.tRange, mm -> {
            boolean marked = mm.group(2) != null || mm.group(5) != null || mm.group(3) != null || mm.group(6) != null
                    || P.fromStart.matcher(mm.group(0)).lookingAt();
            return marked && Integer.parseInt(mm.group(1)) <= 24 && Integer.parseInt(mm.group(4)) <= 24;
        });
        if (m != null) {
            int h1 = Integer.parseInt(m.group(1));
            int m1 = m.group(2) != null ? Integer.parseInt(m.group(2)) : 0;
            String k1 = classify(P, m.group(3));
            int h2 = Integer.parseInt(m.group(4));
            int m2 = m.group(5) != null ? Integer.parseInt(m.group(5)) : 0;
            String k2 = classify(P, m.group(6));
            if (k1 == null) k1 = k2;
            if (k2 == null) k2 = k1;
            tmH = k1 != null ? hourWith(h1, k1) : h1;
            tmM = m1;
            endH = k2 != null ? hourWith(h2, k2) : h2;
            endM = m2;
            work.take(m);
        }
        if (tmH < 0) {
            Pattern[] ps = {P.tColon, P.tH, P.tPrep, P.tMark};
            boolean done = false;
            for (int pi = 0; pi < ps.length && !done; pi++) {
                boolean plain = pi < 2;
                Matcher mm = ps[pi].matcher(work.w.toString());
                while (mm.find()) {
                    int hh = Integer.parseInt(mm.group(1));
                    int mi = plain && mm.group(2) != null ? Integer.parseInt(mm.group(2)) : 0;
                    String mk = mm.group(3);
                    String fr = plain ? null : mm.group(2);
                    if (hh > 24 || mi > 59) continue;
                    String k = classify(P, mk);
                    if (k != null) hh = hourWith(hh, k);
                    else if (!plain && hh >= 1 && hh <= 6) hh += 12;
                    int f = fracMin(P, fr);
                    if (f < 0) {
                        hh -= 1;
                        mi = 60 + f;
                    } else {
                        mi += f;
                    }
                    if (hh < 0) hh += 24;
                    if (hh > 23) hh = hh % 24;
                    tmH = hh;
                    tmM = mi % 60;
                    work.take(mm);
                    done = true;
                    break;
                }
            }
        }
        if (tmH >= 0) d.hasTime = true;

        // day words
        long day = -1;
        m = work.find(P.after);
        if (m != null) {
            day = addDays(today0, 2);
            work.take(m);
        }
        if (day == -1) {
            m = work.find(P.tom);
            if (m != null) {
                day = addDays(today0, 1);
                work.take(m);
            }
        }
        if (day == -1) {
            m = work.find(P.tonight);
            if (m != null) {
                day = today0;
                work.take(m);
                if (tmH < 0) {
                    tmH = 20;
                    tmM = 0;
                    d.hasTime = true;
                }
            }
        }
        if (day == -1) {
            m = work.find(P.today);
            if (m != null) {
                day = today0;
                work.take(m);
            }
        }
        if (day == -1 && relDay != -1) day = relDay;

        // explicit dates
        if (day == -1) {
            m = work.find(P.dIso);
            if (m != null) {
                long x = date(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)));
                if (x != -1) {
                    day = x;
                    work.take(m);
                }
            }
        }
        if (day == -1) {
            Matcher mm = P.dDm.matcher(work.w.toString());
            while (mm.find()) {
                int mo = moIndex(P, mm.group(2));
                int da = Integer.parseInt(mm.group(1));
                int y = fixYear(mo, da, mm.group(3) != null ? Integer.parseInt(mm.group(3)) : 0, now, today0);
                long x = mo > 0 ? date(y, mo, da) : -1;
                if (x != -1) {
                    day = x;
                    work.take(mm);
                    break;
                }
            }
        }
        if (day == -1) {
            Matcher mm = P.dMd.matcher(work.w.toString());
            while (mm.find()) {
                int mo = moIndex(P, mm.group(1));
                int da = Integer.parseInt(mm.group(2));
                int y = fixYear(mo, da, mm.group(3) != null ? Integer.parseInt(mm.group(3)) : 0, now, today0);
                long x = mo > 0 ? date(y, mo, da) : -1;
                if (x != -1) {
                    day = x;
                    work.take(mm);
                    break;
                }
            }
        }
        if (day == -1) {
            Matcher mm = P.dNum.matcher(work.w.toString());
            while (mm.find()) {
                int a = Integer.parseInt(mm.group(1));
                int b = Integer.parseInt(mm.group(2));
                int ys = mm.group(3) != null ? Integer.parseInt(mm.group(3)) : 0;
                int da, mo;
                if (a > 12) {
                    da = a;
                    mo = b;
                } else if (b > 12) {
                    mo = a;
                    da = b;
                } else if (us) {
                    mo = a;
                    da = b;
                } else {
                    da = a;
                    mo = b;
                }
                if (mo < 1 || mo > 12 || da < 1 || da > 31) continue;
                long x = date(fixYear(mo, da, ys, now, today0), mo, da);
                if (x != -1) {
                    day = x;
                    work.take(mm);
                    break;
                }
            }
        }

        // weekday name
        Matcher mw = work.find(P.wd);
        int target = 0;
        if (mw != null && day == -1) {
            target = wdTarget == 0 ? wdIndex(P, mw.group(1)) : wdTarget;
        } else if (wdTarget != 0 && day == -1) {
            target = wdTarget;
            mw = null;
        }
        if (target != 0 && day == -1) {
            int curDow = nowC.get(Calendar.DAY_OF_WEEK);
            int ahead = (target - curDow + 7) % 7;
            if (ahead == 0) {
                boolean later = tmH >= 0 && tmH * 60 + tmM > nowMin;
                ahead = later ? 0 : 7;
            }
            day = addDays(today0, ahead);
            if (mw != null) work.take(mw);
        } else if (mw != null && day != -1 && wdTarget == 0) {
            work.take(mw);
        }

        // "morning", "evening" ... when no clock time was given
        if (tmH < 0) {
            for (int i = 0; i < PERIODS.length; i++) {
                m = work.find(P.period[i]);
                if (m != null) {
                    tmH = PERIOD_HOUR[i];
                    tmM = 0;
                    d.hasTime = true;
                    work.take(m);
                    break;
                }
            }
        }

        // place: "... at the office"
        String left = work.w.toString();
        Matcher ml = P.loc.matcher(left);
        if (ml.find() && left.substring(0, ml.start()).trim().length() > 0) {
            StringBuilder loc = new StringBuilder();
            for (int i = ml.start(1); i < text.length(); i++) if (!work.cut[i]) loc.append(text.charAt(i));
            d.location = loc.toString().replaceAll("\\s+", " ").trim();
            for (int i = ml.start(); i < work.cut.length; i++) {
                work.w.setCharAt(i, ' ');
                work.cut[i] = true;
            }
        }

        // title: what is left of the sentence
        StringBuilder t = new StringBuilder();
        for (int i = 0; i < text.length(); i++) t.append(work.cut[i] ? ' ' : text.charAt(i));
        String title = trimEnds(t.toString().replaceAll("\\s+", " ").trim());
        List<String> words = new ArrayList<>(Arrays.asList(title.isEmpty() ? new String[0] : title.split(" ")));
        while (words.size() > 1 && isStopWord(P, words.get(0))) words.remove(0);
        while (words.size() > 1 && isStopWord(P, words.get(words.size() - 1))) words.remove(words.size() - 1);
        StringBuilder tb = new StringBuilder();
        for (String s : words) tb.append(tb.length() > 0 ? " " : "").append(s);
        d.title = trimEnds(tb.toString());

        // dates
        long start = 0;
        if (relExact != 0) {
            start = relExact;
        } else if (day != -1) {
            d.hasDate = true;
            start = tmH >= 0 ? at(day, tmH, tmM) : day;
        } else if (tmH >= 0) {
            long tdy = at(today0, tmH, tmM);
            start = tdy > now ? tdy : addDays(tdy, 1);
            d.hasDate = true;
        }
        d.start = start;
        if (start != 0 && endH >= 0 && tmH >= 0) {
            long e = at(midnight(start), endH, endM);
            if (e <= start) e = addDays(e, 1);
            d.end = e;
            d.durationMin = (int) ((e - start) / 60000L);
        } else if (start != 0 && d.durationMin > 0 && d.hasTime) {
            d.end = start + d.durationMin * 60000L;
        }
        if (start != 0 && !d.hasTime && d.durationMin == 0) d.allDay = true;
        if (d.task && d.remind >= 0) d.task = false;
        if (start != 0 && d.repeat == 6) {
            Calendar sc = Calendar.getInstance();
            sc.setTimeInMillis(start);
            int dow = sc.get(Calendar.DAY_OF_WEEK);
            if (dow == Calendar.SATURDAY) d.start = addDays(start, 2);
            else if (dow == Calendar.SUNDAY) d.start = addDays(start, 1);
            if (d.end != 0 && d.start != start) d.end += d.start - start;
        }
        return d;
    }
}
