package com.semo.calendar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

import java.util.Calendar;
import java.util.Locale;

/**
 * The month grid, drawn straight on a Canvas (no per-day views): single-letter weekday header with a hairline,
 * day numbers, up to three coloured dots for events, optional week numbers and optional Hijri day numbers.
 * Today is a blue circle, any other selected day a black / white circle. Swipe sideways to change month;
 * mirrors itself in right-to-left languages.
 */
final class MonthView extends View {
    interface Listener {
        void onDay(int day);

        void onSwipe(int dir);
    }

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float headH, textPx, headPx, smallPx, weekGutter;
    private float rowH;
    private final float rowPlain, rowAlt;
    private final GestureDetector gd;
    private int year, month, first = Calendar.SUNDAY, offset, days = 30, rows = 5, selected = 1, today = -1;
    private int[][] dots = new int[32][];
    private int[] weekNums = new int[6];
    private int[] altDays = new int[32];
    private boolean showWeeks, alt;
    private Listener listener;
    private String[] narrow;

    MonthView(Context c) {
        super(c);
        rowPlain = Ui.dp(c, 52);
        rowAlt = Ui.dp(c, 62);
        rowH = rowPlain;
        headH = Ui.dp(c, 26);
        textPx = Ui.sp(c, 20);
        headPx = Ui.sp(c, 11);
        smallPx = Ui.sp(c, 9.5f);
        weekGutter = Ui.dp(c, 28);
        p.setTextAlign(Paint.Align.CENTER);
        gd = new GestureDetector(c, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                hit(e.getX(), e.getY());
                return true;
            }

            @Override
            public boolean onFling(MotionEvent a, MotionEvent b, float vx, float vy) {
                if (listener == null || Math.abs(vx) < 800 || Math.abs(vx) < Math.abs(vy) * 1.5f) return false;
                boolean rtl = getLayoutDirection() == LAYOUT_DIRECTION_RTL;
                listener.onSwipe(((vx < 0) ^ rtl) ? 1 : -1);
                return true;
            }
        });
    }

    void setListener(Listener l) {
        listener = l;
    }

    /** dotsByDay[d] = colours (ARGB) of that day's events, index 1..31. */
    void set(int y, int m, int firstWeekday, int sel, int[][] dotsByDay, boolean weeks, boolean hijri) {
        year = y;
        month = m;
        first = firstWeekday;
        selected = sel;
        dots = dotsByDay;
        showWeeks = weeks;
        alt = hijri;
        rowH = alt ? rowAlt : rowPlain;
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(y, m, 1);
        offset = ((c.get(Calendar.DAY_OF_WEEK) - first) + 7) % 7;
        days = c.getActualMaximum(Calendar.DAY_OF_MONTH);
        rows = (offset + days + 6) / 7;
        Calendar t = Calendar.getInstance();
        today = (t.get(Calendar.YEAR) == y && t.get(Calendar.MONTH) == m) ? t.get(Calendar.DAY_OF_MONTH) : -1;
        weekNums = new int[rows];
        Calendar w = Calendar.getInstance();
        w.setFirstDayOfWeek(first);
        w.setMinimalDaysInFirstWeek(first == Calendar.MONDAY ? 4 : 1);
        for (int r = 0; r < rows; r++) {
            int d = Math.max(1, Math.min(days, r * 7 - offset + 4));
            w.clear();
            w.set(y, m, d);
            weekNums[r] = w.get(Calendar.WEEK_OF_YEAR);
        }
        altDays = new int[32];
        if (alt) {
            Calendar k = Calendar.getInstance();
            for (int d = 1; d <= days; d++) {
                k.clear();
                k.set(y, m, d, 12, 0, 0);
                altDays[d] = Hijri.day(k.getTimeInMillis());
            }
        }
        narrow = null;
        requestLayout();
        invalidate();
    }

    void select(int d) {
        selected = d;
        invalidate();
    }

    @Override
    protected void onMeasure(int w, int h) {
        setMeasuredDimension(MeasureSpec.getSize(w), (int) (headH + rows * rowH));
    }

    private boolean rtl() {
        return getLayoutDirection() == LAYOUT_DIRECTION_RTL;
    }

    @Override
    protected void onDraw(Canvas cv) {
        Context c = getContext();
        boolean rtl = rtl();
        float gw = showWeeks ? weekGutter : 0;
        float cw = (getWidth() - gw) / 7f;
        float x0 = rtl ? 0 : gw;
        p.setStyle(Paint.Style.FILL);
        if (narrow == null) {
            narrow = new String[8];
            java.text.SimpleDateFormat nf = new java.text.SimpleDateFormat("EEEEE", Locale.getDefault());
            Calendar nc = Calendar.getInstance();
            for (int k = 1; k <= 7; k++) {
                nc.set(Calendar.DAY_OF_WEEK, k);
                narrow[k] = nf.format(nc.getTime());
            }
        }
        // weekday header
        p.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        p.setTextSize(headPx);
        p.setColor(Ui.color(c, R.color.text_secondary));
        for (int i = 0; i < 7; i++) {
            int wd = ((first - 1 + i) % 7) + 1;
            int col = rtl ? 6 - i : i;
            cv.drawText(narrow[wd], x0 + (col + 0.5f) * cw, headH * 0.72f, p);
        }
        p.setColor(Ui.color(c, R.color.stroke));
        p.setStrokeWidth(Math.max(1, Ui.dp(c, 0.5f)));
        cv.drawLine(0, headH, getWidth(), headH, p);

        p.setTypeface(Typeface.DEFAULT);
        p.setTextSize(textPx);
        Paint.FontMetrics fm = p.getFontMetrics();
        float r = Ui.dp(c, alt ? 17 : 18);
        float dotR = Ui.dp(c, 2.5f);
        float numCy = rowH * (alt ? 0.36f : 0.40f);
        for (int d = 1; d <= days; d++) {
            int idx = offset + d - 1;
            int col = idx % 7;
            if (rtl) col = 6 - col;
            float cx = x0 + (col + 0.5f) * cw;
            float cy = headH + (idx / 7) * rowH + numCy;
            boolean sel = d == selected;
            boolean isToday = d == today;
            if (sel) {
                p.setColor(Ui.color(c, isToday ? R.color.accent : R.color.text_primary));
                cv.drawCircle(cx, cy, r, p);
            }
            p.setColor(sel ? Ui.color(c, isToday ? R.color.on_accent : R.color.bg)
                    : isToday ? Ui.color(c, R.color.accent) : Ui.color(c, R.color.text_primary));
            p.setTextSize(textPx);
            p.setFakeBoldText(isToday);
            cv.drawText(String.format(Locale.getDefault(), "%d", d), cx, cy - (fm.ascent + fm.descent) / 2f, p);
            p.setFakeBoldText(false);
            if (alt) {
                p.setTextSize(smallPx);
                p.setColor(Ui.color(c, R.color.text_secondary));
                cv.drawText(String.format(Locale.getDefault(), "%d", altDays[d]), cx, cy + r + Ui.dp(c, 9), p);
            }
            int[] ds = d < dots.length ? dots[d] : null;
            if (ds != null && ds.length > 0) {
                float gap = Ui.dp(c, 8);
                float x1 = cx - gap * (ds.length - 1) / 2f;
                float dy = cy + r + Ui.dp(c, alt ? 17 : 7);
                for (int k = 0; k < ds.length; k++) {
                    p.setColor(ds[k]);
                    cv.drawCircle(x1 + k * gap, dy, dotR, p);
                }
            }
        }
        if (showWeeks) {
            p.setTextSize(Ui.sp(c, 11));
            p.setColor(Ui.color(c, R.color.text_secondary));
            Paint.FontMetrics wf = p.getFontMetrics();
            float wx = rtl ? getWidth() - gw / 2f : gw / 2f;
            for (int rw = 0; rw < rows; rw++) {
                float wy = headH + rw * rowH + numCy - (wf.ascent + wf.descent) / 2f;
                cv.drawText(String.format(Locale.getDefault(), "%d", weekNums[rw]), wx, wy, p);
            }
        }
    }

    private void hit(float x, float y) {
        if (y < headH) return;
        float gw = showWeeks ? weekGutter : 0;
        float x0 = rtl() ? 0 : gw;
        float cw = (getWidth() - gw) / 7f;
        int row = (int) ((y - headH) / rowH);
        int col = (int) ((x - x0) / cw);
        if (col < 0 || col > 6) return;
        if (rtl()) col = 6 - col;
        int d = row * 7 + col - offset + 1;
        if (d < 1 || d > days) return;
        selected = d;
        invalidate();
        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        if (listener != null) listener.onDay(d);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        return gd.onTouchEvent(e) || super.onTouchEvent(e);
    }
}
