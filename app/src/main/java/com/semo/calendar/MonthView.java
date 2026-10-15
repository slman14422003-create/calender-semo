package com.semo.calendar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.Locale;

/**
 * The month grid, drawn straight on a Canvas (no per-day views): weekday header, day numbers, a filled blue
 * circle for the selected day, blue text for today and up to three coloured dots for events.
 * Swipe sideways to change month; mirrors itself in right-to-left languages.
 */
final class MonthView extends View {
    interface Listener {
        void onDay(int day);

        void onSwipe(int dir);
    }

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float rowH, headH, textPx, headPx;
    private final GestureDetector gd;
    private int year, month, first = Calendar.SUNDAY, offset, days = 30, rows = 5, selected = 1, today = -1;
    private int[][] dots = new int[32][];
    private Listener listener;

    MonthView(Context c) {
        super(c);
        rowH = Ui.dp(c, 50);
        headH = Ui.dp(c, 26);
        textPx = Ui.sp(c, 18);
        headPx = Ui.sp(c, 12);
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

    int selectedDay() {
        return selected;
    }

    /** dotsByDay[d] = colours (ARGB) of that day's events, index 1..31. */
    void set(int y, int m, int firstWeekday, int sel, int[][] dotsByDay) {
        year = y;
        month = m;
        first = firstWeekday;
        selected = sel;
        dots = dotsByDay;
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(y, m, 1);
        offset = ((c.get(Calendar.DAY_OF_WEEK) - first) + 7) % 7;
        days = c.getActualMaximum(Calendar.DAY_OF_MONTH);
        rows = (offset + days + 6) / 7;
        Calendar t = Calendar.getInstance();
        today = (t.get(Calendar.YEAR) == y && t.get(Calendar.MONTH) == m) ? t.get(Calendar.DAY_OF_MONTH) : -1;
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
        float cw = getWidth() / 7f;
        boolean rtl = rtl();
        p.setStyle(Paint.Style.FILL);
        // weekday header
        String[] names = DateFormatSymbols.getInstance(Locale.getDefault()).getShortWeekdays();
        p.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        p.setTextSize(headPx);
        p.setColor(Ui.color(c, R.color.text_secondary));
        for (int i = 0; i < 7; i++) {
            int wd = ((first - 1 + i) % 7) + 1;
            int col = rtl ? 6 - i : i;
            cv.drawText(names[wd], (col + 0.5f) * cw, headH * 0.72f, p);
        }
        p.setTypeface(Typeface.DEFAULT);
        p.setTextSize(textPx);
        Paint.FontMetrics fm = p.getFontMetrics();
        float r = Ui.dp(c, 17);
        float dotR = Ui.dp(c, 2.5f);
        for (int d = 1; d <= days; d++) {
            int idx = offset + d - 1;
            int col = idx % 7;
            if (rtl) col = 6 - col;
            float cx = (col + 0.5f) * cw;
            float cy = headH + (idx / 7) * rowH + rowH * 0.40f;
            boolean sel = d == selected;
            if (sel) {
                p.setColor(Ui.color(c, R.color.accent));
                cv.drawCircle(cx, cy, r, p);
            }
            p.setColor(sel ? Ui.color(c, R.color.on_accent)
                    : d == today ? Ui.color(c, R.color.accent) : Ui.color(c, R.color.text_primary));
            p.setFakeBoldText(d == today);
            cv.drawText(String.format(Locale.getDefault(), "%d", d), cx, cy - (fm.ascent + fm.descent) / 2f, p);
            p.setFakeBoldText(false);
            int[] ds = d < dots.length ? dots[d] : null;
            if (ds != null && ds.length > 0) {
                float gap = Ui.dp(c, 8);
                float x0 = cx - gap * (ds.length - 1) / 2f;
                for (int k = 0; k < ds.length; k++) {
                    p.setColor(ds[k]);
                    cv.drawCircle(x0 + k * gap, cy + r + Ui.dp(c, 7), dotR, p);
                }
            }
        }
    }

    private void hit(float x, float y) {
        if (y < headH) return;
        int row = (int) ((y - headH) / rowH);
        int col = (int) (x / (getWidth() / 7f));
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
