package com.semo.calendar;

import android.graphics.Typeface;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Locale;
import java.util.function.LongConsumer;

/** Year tab of the calendar: large year title and the twelve-month overview. */
final class YearScreen implements DayAware {
    private final MainActivity a;
    private final LinearLayout root;
    private final TextView title;
    private final YearView grid;
    private int year = Calendar.getInstance().get(Calendar.YEAR);

    YearScreen(MainActivity act, final LongConsumer onMonth) {
        a = act;
        root = Ui.vertical(a);
        title = Ui.text(a, "", 34, R.color.text_primary, false);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setSingleLine(true);
        title.setPadding(Ui.dp(a, 20), Ui.dp(a, 2), Ui.dp(a, 12), Ui.dp(a, 6));
        root.addView(Ui.titleRow(a, title, v -> Sheets.pickDate(a, anchor(), ms -> {
            setAnchor(ms);
            refresh();
        })), new LinearLayout.LayoutParams(-1, -2));
        ScrollView sv = new ScrollView(a);
        sv.setVerticalScrollBarEnabled(false);
        sv.setOverScrollMode(View.OVER_SCROLL_NEVER);
        grid = new YearView(a);
        grid.setListener(new YearView.Listener() {
            @Override
            public void onMonth(int month) {
                Calendar c = Calendar.getInstance();
                c.clear();
                c.set(year, month, 1, 12, 0, 0);
                Calendar n = Calendar.getInstance();
                if (n.get(Calendar.YEAR) == year && n.get(Calendar.MONTH) == month) {
                    onMonth.accept(n.getTimeInMillis());
                } else {
                    onMonth.accept(c.getTimeInMillis());
                }
            }

            @Override
            public void onSwipe(int dir) {
                year += dir;
                refresh();
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
        Calendar n = Calendar.getInstance();
        if (n.get(Calendar.YEAR) == year) return n.getTimeInMillis();
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(year, 0, 1, 12, 0, 0);
        return c.getTimeInMillis();
    }

    @Override
    public void setAnchor(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        year = c.get(Calendar.YEAR);
    }

    @Override
    public void goToday() {
        year = Calendar.getInstance().get(Calendar.YEAR);
        refresh();
    }

    @Override
    public void refresh() {
        title.setText(String.format(Locale.getDefault(), "%d", year));
        grid.setYear(year);
    }
}
