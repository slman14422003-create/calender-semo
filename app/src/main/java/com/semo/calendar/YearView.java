package com.semo.calendar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/** iOS year view: twelve small months (3 x 4) with today highlighted; tap a month to open it. */
final class YearView extends View {
    interface Listener {
        void onMonth(int month);

        void onSwipe(int dir);
    }

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float pad, titleH, rowH, gapY;
    private final GestureDetector gd;
    private int year = Calendar.getInstance().get(Calendar.YEAR);
    private String[] names;
    private Listener listener;

    YearView(Context c) {
        super(c);
        pad = Ui.dp(c, 14);
        titleH = Ui.dp(c, 26);
        rowH = Ui.dp(c, 15);
        gapY = Ui.dp(c, 14);
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

    void setYear(int y) {
        year = y;
        names = null;
        invalidate();
    }

    private float blockH() {
        return titleH + 6 * rowH + gapY;
    }

    @Override
    protected void onMeasure(int w, int h) {
        setMeasuredDimension(MeasureSpec.getSize(w), (int) (pad + 4 * blockH() + Ui.dp(getContext(), 100)));
    }

    private boolean rtl() {
        return getLayoutDirection() == LAYOUT_DIRECTION_RTL;
    }

    @Override
    protected void onDraw(Canvas cv) {
        Context c = getContext();
        boolean rtl = rtl();
        if (names == null) {
            names = new String[12];
            SimpleDateFormat f = new SimpleDateFormat("MMMM", Locale.getDefault());
            Calendar k = Calendar.getInstance();
            for (int m = 0; m < 12; m++) {
                k.clear();
                k.set(year, m, 1);
                names[m] = f.format(k.getTime());
            }
        }
        Calendar now = Calendar.getInstance();
        int ty = now.get(Calendar.YEAR), tm = now.get(Calendar.MONTH), td = now.get(Calendar.DAY_OF_MONTH);
        int first = Prefs.firstDay(c);
        float bw = (getWidth() - 2 * pad) / 3f;
        float cw = bw / 7f;
        p.setStyle(Paint.Style.FILL);
        for (int m = 0; m < 12; m++) {
            int col = m % 3;
            if (rtl) col = 2 - col;
            float x0 = pad + col * bw;
            float y0 = pad + (m / 3) * blockH();
            boolean thisMonth = year == ty && m == tm;
            p.setTypeface(Typeface.DEFAULT_BOLD);
            p.setTextSize(Ui.sp(c, 15));
            p.setColor(Ui.color(c, thisMonth ? R.color.accent : R.color.text_primary));
            p.setTextAlign(rtl ? Paint.Align.RIGHT : Paint.Align.LEFT);
            cv.drawText(names[m], rtl ? x0 + bw - Ui.dp(c, 6) : x0 + Ui.dp(c, 6), y0 + titleH * 0.7f, p);
            p.setTextAlign(Paint.Align.CENTER);
            p.setTypeface(Typeface.DEFAULT);
            p.setTextSize(Ui.sp(c, 9.5f));
            Paint.FontMetrics fm = p.getFontMetrics();
            Calendar k = Calendar.getInstance();
            k.clear();
            k.set(year, m, 1);
            int off = ((k.get(Calendar.DAY_OF_WEEK) - first) + 7) % 7;
            int dim = k.getActualMaximum(Calendar.DAY_OF_MONTH);
            for (int d = 1; d <= dim; d++) {
                int idx = off + d - 1;
                int cc = idx % 7;
                if (rtl) cc = 6 - cc;
                float cx = x0 + (cc + 0.5f) * cw;
                float cy = y0 + titleH + (idx / 7) * rowH + rowH / 2f;
                boolean isToday = thisMonth && d == td;
                if (isToday) {
                    p.setColor(Ui.color(c, R.color.accent));
                    cv.drawCircle(cx, cy, Ui.dp(c, 7.5f), p);
                }
                p.setColor(isToday ? Ui.color(c, R.color.on_accent) : Ui.color(c, R.color.text_primary));
                cv.drawText(String.format(Locale.getDefault(), "%d", d), cx, cy - (fm.ascent + fm.descent) / 2f, p);
            }
        }
    }

    private void hit(float x, float y) {
        float bw = (getWidth() - 2 * pad) / 3f;
        int col = (int) ((x - pad) / bw);
        int row = (int) ((y - pad) / blockH());
        if (col < 0 || col > 2 || row < 0 || row > 3) return;
        if (rtl()) col = 2 - col;
        if (listener != null) listener.onMonth(row * 3 + col);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        return gd.onTouchEvent(e) || super.onTouchEvent(e);
    }
}
