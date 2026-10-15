package com.semo.calendar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Hour grid for the Day and Week views, drawn on a Canvas: hour lines and labels, a tinted column for today,
 * a red "now" line, and event blocks (overlapping events share the column width). Tap an event to open it, tap an
 * empty slot to create an event there, swipe sideways to move by a day / week. Mirrors in right-to-left languages.
 */
final class TimeGridView extends View {
    interface Listener {
        void onEvent(Recur.Occ o);

        void onSlot(long at);

        void onSwipe(int dir);
    }

    private static final long HALF_HOUR = 1800000L;

    private static final class Item {
        Recur.Occ o;
        int day, col = 0, cols = 1;
        long s, e, d0;
    }

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rf = new RectF();
    private final float hourH, gutter, topPad;
    private final GestureDetector gd;
    private final List<Item> items = new ArrayList<>();
    private int days = 1;
    private long[] dayStarts = new long[1];
    private String[] hourLabels = new String[24];
    private Listener listener;

    TimeGridView(Context c) {
        super(c);
        hourH = Ui.dp(c, 56);
        gutter = Ui.dp(c, 52);
        topPad = Ui.dp(c, 12);
        gd = new GestureDetector(c, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                tap(e.getX(), e.getY());
                return true;
            }

            @Override
            public boolean onFling(MotionEvent a, MotionEvent b, float vx, float vy) {
                if (listener == null || Math.abs(vx) < 900 || Math.abs(vx) < Math.abs(vy) * 2f) return false;
                boolean rtl = getLayoutDirection() == LAYOUT_DIRECTION_RTL;
                listener.onSwipe(((vx < 0) ^ rtl) ? 1 : -1);
                return true;
            }
        });
    }

    void setListener(Listener l) {
        listener = l;
    }

    int yForHour(int h) {
        return (int) (topPad + h * hourH);
    }

    void set(int dayCount, long first, List<Recur.Occ> timed) {
        days = dayCount;
        dayStarts = new long[days];
        items.clear();
        long d0 = first;
        for (int d = 0; d < days; d++) {
            dayStarts[d] = d0;
            long d1 = Recur.nextDay(d0);
            List<Item> col = new ArrayList<>();
            for (Recur.Occ o : timed) {
                if (o.start < d1 && (o.end > d0 || o.start >= d0)) {
                    Item it = new Item();
                    it.o = o;
                    it.day = d;
                    it.d0 = d0;
                    it.s = Math.max(o.start, d0);
                    it.e = Math.min(Math.max(o.end, o.start + HALF_HOUR), d1);
                    if (it.e <= it.s) it.e = Math.min(d1, it.s + HALF_HOUR);
                    col.add(it);
                }
            }
            int i = 0;
            while (i < col.size()) {
                List<Item> cl = new ArrayList<>();
                long clEnd = col.get(i).e;
                cl.add(col.get(i));
                int j = i + 1;
                while (j < col.size() && col.get(j).s < clEnd) {
                    cl.add(col.get(j));
                    clEnd = Math.max(clEnd, col.get(j).e);
                    j++;
                }
                List<Long> colEnds = new ArrayList<>();
                for (Item it : cl) {
                    int c = 0;
                    while (c < colEnds.size() && colEnds.get(c) > it.s) c++;
                    if (c == colEnds.size()) colEnds.add(it.e);
                    else colEnds.set(c, it.e);
                    it.col = c;
                }
                for (Item it : cl) it.cols = colEnds.size();
                i = j;
            }
            items.addAll(col);
            d0 = d1;
        }
        boolean h24 = DateFormat.is24HourFormat(getContext());
        Calendar c = Calendar.getInstance();
        for (int h = 0; h < 24; h++) {
            c.setTimeInMillis(first);
            c.set(Calendar.HOUR_OF_DAY, h);
            hourLabels[h] = Fmt.date(h24 ? "H" : "ha", c.getTimeInMillis());
        }
        invalidate();
    }

    @Override
    protected void onMeasure(int w, int h) {
        setMeasuredDimension(MeasureSpec.getSize(w), (int) (topPad + 24 * hourH + Ui.dp(getContext(), 90)));
    }

    private boolean rtl() {
        return getLayoutDirection() == LAYOUT_DIRECTION_RTL;
    }

    private float colX(int i, float cw) {
        return rtl() ? getWidth() - gutter - (i + 1) * cw : gutter + i * cw;
    }

    private float yOf(long t, long d0) {
        return topPad + (t - d0) / 3600000f * hourH;
    }

    private void rectOf(Item it, float cw, RectF out) {
        float pad = Ui.dp(getContext(), 1.5f);
        float x = colX(it.day, cw) + pad;
        float w = (cw - 2 * pad) / it.cols;
        out.left = x + w * it.col;
        out.right = out.left + w - pad;
        out.top = yOf(it.s, it.d0) + 1;
        out.bottom = Math.max(yOf(it.e, it.d0) - 1, out.top + Ui.dp(getContext(), 20));
    }

    @Override
    protected void onDraw(Canvas cv) {
        Context c = getContext();
        float cw = (getWidth() - gutter) / days;
        boolean rtl = rtl();
        float gx0 = rtl ? 0 : gutter;
        float gx1 = rtl ? getWidth() - gutter : getWidth();
        float bottom = topPad + 24 * hourH;
        long now = System.currentTimeMillis();
        long today = Recur.dayStart(now);

        // today column tint
        p.setStyle(Paint.Style.FILL);
        for (int d = 0; d < days; d++) {
            if (dayStarts[d] == today) {
                p.setColor(Ui.color(c, R.color.accent));
                p.setAlpha(18);
                cv.drawRect(colX(d, cw), topPad, colX(d, cw) + cw, bottom, p);
            }
        }
        p.setAlpha(255);

        // hour lines and labels
        p.setStrokeWidth(Math.max(1, Ui.dp(c, 0.5f)));
        p.setColor(Ui.color(c, R.color.stroke_soft));
        tp.setTextSize(Ui.sp(c, 11));
        tp.setColor(Ui.color(c, R.color.text_secondary));
        tp.setTextAlign(rtl ? Paint.Align.LEFT : Paint.Align.RIGHT);
        Paint.FontMetrics fm = tp.getFontMetrics();
        for (int h = 0; h <= 24; h++) {
            float y = topPad + h * hourH;
            cv.drawLine(gx0, y, gx1, y, p);
            if (h > 0 && h < 24) {
                float lx = rtl ? getWidth() - gutter + Ui.dp(c, 6) : gutter - Ui.dp(c, 6);
                cv.drawText(hourLabels[h], lx, y - (fm.ascent + fm.descent) / 2f, tp);
            }
        }
        if (days > 1) {
            for (int d = 0; d <= days; d++) {
                float x = rtl ? getWidth() - gutter - d * cw : gutter + d * cw;
                cv.drawLine(x, topPad, x, bottom, p);
            }
        }

        // events
        for (Item it : items) {
            rectOf(it, cw, rf);
            int col = Ui.color(c, Event.COLORS[it.o.e.color]);
            float r = Ui.dp(c, 5);
            p.setColor(col);
            p.setAlpha(48);
            cv.drawRoundRect(rf, r, r, p);
            p.setAlpha(255);
            float bw = Ui.dp(c, 3);
            RectF bar = new RectF(rtl ? rf.right - bw : rf.left, rf.top, rtl ? rf.right : rf.left + bw, rf.bottom);
            cv.drawRoundRect(bar, bw / 2, bw / 2, p);
            cv.save();
            cv.clipRect(rf);
            float tx = rtl ? rf.right - bw - Ui.dp(c, 5) : rf.left + bw + Ui.dp(c, 5);
            float avail = rf.width() - bw - Ui.dp(c, 8);
            tp.setTextAlign(rtl ? Paint.Align.RIGHT : Paint.Align.LEFT);
            tp.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            tp.setTextSize(Ui.sp(c, 12));
            tp.setColor(Ui.color(c, R.color.text_primary));
            String title = it.o.e.title.isEmpty() ? c.getString(R.string.untitled) : it.o.e.title;
            Paint.FontMetrics f2 = tp.getFontMetrics();
            float ty = rf.top + Ui.dp(c, 5) - f2.ascent;
            cv.drawText(TextUtils.ellipsize(title, tp, Math.max(1, avail), TextUtils.TruncateAt.END).toString(), tx, ty, tp);
            if (rf.height() >= Ui.dp(c, 38)) {
                tp.setTypeface(Typeface.DEFAULT);
                tp.setTextSize(Ui.sp(c, 11));
                tp.setColor(Ui.color(c, R.color.text_secondary));
                String sub = Fmt.time(c, it.o.start) + " – " + Fmt.time(c, it.o.end);
                cv.drawText(TextUtils.ellipsize(sub, tp, Math.max(1, avail), TextUtils.TruncateAt.END).toString(),
                        tx, ty + Ui.dp(c, 15), tp);
            }
            cv.restore();
        }
        tp.setTypeface(Typeface.DEFAULT);

        // now line
        for (int d = 0; d < days; d++) {
            if (dayStarts[d] == today) {
                float y = yOf(now, today);
                p.setColor(Ui.color(c, R.color.bad));
                p.setStrokeWidth(Ui.dp(c, 1.5f));
                cv.drawLine(gx0, y, gx1, y, p);
                cv.drawCircle(rtl ? colX(d, cw) + cw : colX(d, cw), y, Ui.dp(c, 4), p);
            }
        }
    }

    private void tap(float x, float y) {
        if (listener == null) return;
        float cw = (getWidth() - gutter) / days;
        for (int i = items.size() - 1; i >= 0; i--) {
            rectOf(items.get(i), cw, rf);
            if (rf.contains(x, y)) {
                listener.onEvent(items.get(i).o);
                return;
            }
        }
        float gx = rtl() ? getWidth() - gutter - x : x - gutter;
        if (gx < 0) return;
        int d = Math.min(days - 1, (int) (gx / cw));
        float hours = (y - topPad) / hourH;
        if (hours < 0 || hours >= 24) return;
        long at = dayStarts[d] + (long) (Math.floor(hours * 2) * 30) * 60000L;
        listener.onSlot(at);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        return gd.onTouchEvent(e) || super.onTouchEvent(e);
    }
}
