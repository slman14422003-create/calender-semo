package com.semo.calendar;

import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.function.LongConsumer;

/** Day (1 column) and Week (7 columns) views: header with range and arrows, day strip, all-day pills, hour grid. */
final class TimeScreen implements DayAware {
    private final MainActivity a;
    private final int days;
    private final LongConsumer dayTap;
    private final LinearLayout root, strip, allDayRow;
    private final HorizontalScrollView allDayScroll;
    private final TextView title;
    private final TimeGridView grid;
    private final ScrollView sv;
    private long anchor = System.currentTimeMillis();
    private boolean scrollPending = true;

    TimeScreen(MainActivity act, int dayCount, LongConsumer onDayTap) {
        a = act;
        days = dayCount;
        dayTap = onDayTap;
        root = Ui.vertical(a);

        title = Ui.text(a, "", 34, R.color.text_primary, false);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setSingleLine(true);
        androidx.core.widget.TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                title, 20, 34, 1, android.util.TypedValue.COMPLEX_UNIT_SP);
        title.setPadding(Ui.dp(a, 20), Ui.dp(a, 2), Ui.dp(a, 20), Ui.dp(a, 6));
        root.addView(title, new LinearLayout.LayoutParams(-1, -2));

        strip = new LinearLayout(a);
        strip.setPadding(0, 0, 0, Ui.dp(a, 4));
        root.addView(strip, new LinearLayout.LayoutParams(-1, -2));

        allDayScroll = new HorizontalScrollView(a);
        allDayScroll.setHorizontalScrollBarEnabled(false);
        allDayRow = new LinearLayout(a);
        allDayRow.setPadding(Ui.dp(a, 12), Ui.dp(a, 2), Ui.dp(a, 12), Ui.dp(a, 8));
        allDayScroll.addView(allDayRow);
        root.addView(allDayScroll, new LinearLayout.LayoutParams(-1, -2));

        View line = Ui.hairline(a, 0);
        root.addView(line);

        sv = new ScrollView(a);
        sv.setVerticalScrollBarEnabled(false);
        grid = new TimeGridView(a);
        grid.setListener(new TimeGridView.Listener() {
            @Override
            public void onEvent(Recur.Occ o) {
                a.showDetail(o);
            }

            @Override
            public void onSlot(long at) {
                a.openEditorAt(at);
            }

            @Override
            public void onSwipe(int dir) {
                move(dir);
            }
        });
        sv.addView(grid, new ScrollView.LayoutParams(-1, -2));
        root.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1f));
    }

    @Override
    public View view() {
        return root;
    }

    @Override
    public long anchor() {
        return anchor;
    }

    @Override
    public void setAnchor(long millis) {
        anchor = millis;
        scrollPending = true;
    }

    @Override
    public void goToday() {
        anchor = System.currentTimeMillis();
        scrollPending = true;
        refresh();
    }

    private void move(int dir) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(anchor);
        c.add(Calendar.DAY_OF_YEAR, dir * days);
        anchor = c.getTimeInMillis();
        refresh();
    }

    private long firstStart() {
        long d = Recur.dayStart(anchor);
        if (days == 1) return d;
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(d);
        int diff = (c.get(Calendar.DAY_OF_WEEK) - Prefs.firstDay(a) + 7) % 7;
        c.add(Calendar.DAY_OF_YEAR, -diff);
        return c.getTimeInMillis();
    }

    private TextView pill(final Recur.Occ o) {
        int col = Ui.color(a, Event.COLORS[o.e.color]);
        TextView t = Ui.text(a, o.e.title.isEmpty() ? a.getString(R.string.untitled) : o.e.title, 13,
                R.color.text_primary, true);
        t.setSingleLine(true);
        android.graphics.drawable.GradientDrawable g = Ui.shape(col, Ui.dp(a, 7));
        g.setAlpha(60);
        t.setBackground(g);
        t.setPadding(Ui.dp(a, 10), Ui.dp(a, 4), Ui.dp(a, 10), Ui.dp(a, 4));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.setMarginEnd(Ui.dp(a, 6));
        t.setLayoutParams(lp);
        t.setOnClickListener(v -> a.showDetail(o));
        Ui.press(t);
        return t;
    }

    @Override
    public void refresh() {
        long first = firstStart();
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(first);
        c.add(Calendar.DAY_OF_YEAR, days - 1);
        long last = c.getTimeInMillis();
        c.add(Calendar.DAY_OF_YEAR, 1);
        long to = c.getTimeInMillis();

        title.setText(days == 1 ? Fmt.date("EEEEdMMMM", first)
                : Fmt.date("dMMM", first) + " – " + Fmt.date("dMMM", last));

        List<Recur.Occ> timed = new ArrayList<>();
        allDayRow.removeAllViews();
        for (Recur.Occ o : Recur.between(Store.all(a), first, to)) {
            if (o.e.allDay) allDayRow.addView(pill(o));
            else timed.add(o);
        }
        allDayScroll.setVisibility(allDayRow.getChildCount() == 0 ? View.GONE : View.VISIBLE);

        strip.removeAllViews();
        if (days > 1) {
            strip.addView(new View(a), new LinearLayout.LayoutParams(Ui.dp(a, 52), 1));
            long today = Recur.dayStart(System.currentTimeMillis());
            Calendar d = Calendar.getInstance();
            d.setTimeInMillis(first);
            for (int i = 0; i < days; i++) {
                final long ds = d.getTimeInMillis();
                LinearLayout cell = Ui.vertical(a);
                cell.setGravity(Gravity.CENTER_HORIZONTAL);
                TextView wd = Ui.text(a, Fmt.date("EEE", ds), 11, R.color.text_secondary, true);
                TextView num = Ui.text(a, Fmt.date("d", ds), 16, ds == today ? R.color.on_accent : R.color.text_primary, false);
                num.setGravity(Gravity.CENTER);
                if (ds == today) num.setBackground(Ui.shape(Ui.color(a, R.color.accent), Ui.dp(a, 15)));
                cell.addView(wd);
                cell.addView(num, new LinearLayout.LayoutParams(Ui.dp(a, 30), Ui.dp(a, 30)));
                cell.setOnClickListener(v -> dayTap.accept(ds));
                Ui.press(cell);
                strip.addView(cell, new LinearLayout.LayoutParams(0, -2, 1f));
                d.add(Calendar.DAY_OF_YEAR, 1);
            }
        }

        grid.set(days, first, timed);
        if (scrollPending) {
            scrollPending = false;
            long now = System.currentTimeMillis();
            Calendar nc = Calendar.getInstance();
            nc.setTimeInMillis(now);
            final int hour = (now >= first && now < to) ? Math.max(0, nc.get(Calendar.HOUR_OF_DAY) - 1) : 7;
            sv.post(() -> sv.scrollTo(0, grid.yForHour(hour)));
        }
    }
}
