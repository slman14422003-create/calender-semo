package com.semo.calendar;

import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/** Month tab: large month title, the grid, and the selected day's events in a rounded grey sheet below. */
final class MonthScreen implements Screen {
    private final MainActivity a;
    private final LinearLayout root;
    private final TextView title, dayLabel;
    private final MonthView grid;
    private final EventAdapter adapter;
    private int y, m, sel;

    MonthScreen(MainActivity act) {
        a = act;
        Calendar n = Calendar.getInstance();
        y = n.get(Calendar.YEAR);
        m = n.get(Calendar.MONTH);
        sel = n.get(Calendar.DAY_OF_MONTH);

        root = Ui.vertical(a);
        LinearLayout head = new LinearLayout(a);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(Ui.dp(a, 20), Ui.dp(a, 10), Ui.dp(a, 8), Ui.dp(a, 4));
        title = Ui.text(a, "", 30, R.color.text_primary, false);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        head.addView(title, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView today = Ui.text(a, a.getString(R.string.today), 17, R.color.accent, false);
        today.setPadding(Ui.dp(a, 10), Ui.dp(a, 8), Ui.dp(a, 10), Ui.dp(a, 8));
        today.setOnClickListener(v -> goToday());
        Ui.press(today);
        head.addView(today);
        ImageButton prev = Ui.iconButton(a, R.drawable.ic_chev, R.color.accent, 40);
        ImageButton next = Ui.iconButton(a, R.drawable.ic_chev_next, R.color.accent, 40);
        prev.setOnClickListener(v -> move(-1));
        next.setOnClickListener(v -> move(1));
        head.addView(prev);
        head.addView(next);
        root.addView(head);

        grid = new MonthView(a);
        grid.setPadding(Ui.dp(a, 8), 0, Ui.dp(a, 8), 0);
        grid.setListener(new MonthView.Listener() {
            @Override
            public void onDay(int day) {
                sel = day;
                showDay();
            }

            @Override
            public void onSwipe(int dir) {
                move(dir);
            }
        });
        root.addView(grid, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout sheet = Ui.vertical(a);
        sheet.setBackground(Ui.topRounded(Ui.color(a, R.color.bg_group), Ui.dp(a, 24)));
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, 0, 1f);
        sp.topMargin = Ui.dp(a, 8);
        dayLabel = Ui.text(a, "", 13, R.color.text_secondary, true);
        dayLabel.setPadding(Ui.dp(a, 20), Ui.dp(a, 16), Ui.dp(a, 20), Ui.dp(a, 2));
        sheet.addView(dayLabel);
        RecyclerView rv = new RecyclerView(a);
        rv.setLayoutManager(new LinearLayoutManager(a));
        rv.setClipToPadding(false);
        rv.setPadding(0, 0, 0, Ui.dp(a, 160));
        rv.setOverScrollMode(View.OVER_SCROLL_NEVER);
        adapter = new EventAdapter(a, false, R.string.no_events, o -> a.showDetail(o));
        rv.setAdapter(adapter);
        sheet.addView(rv, new LinearLayout.LayoutParams(-1, 0, 1f));
        root.addView(sheet, sp);
    }

    @Override
    public View view() {
        return root;
    }

    long selectedMillis() {
        return dayMillis(sel);
    }

    private long dayMillis(int d) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(y, m, d);
        return c.getTimeInMillis();
    }

    private void goToday() {
        Calendar n = Calendar.getInstance();
        y = n.get(Calendar.YEAR);
        m = n.get(Calendar.MONTH);
        sel = n.get(Calendar.DAY_OF_MONTH);
        refresh();
    }

    private void move(int dir) {
        m += dir;
        if (m < 0) {
            m = 11;
            y--;
        } else if (m > 11) {
            m = 0;
            y++;
        }
        Calendar n = Calendar.getInstance();
        sel = (n.get(Calendar.YEAR) == y && n.get(Calendar.MONTH) == m) ? n.get(Calendar.DAY_OF_MONTH) : 1;
        refresh();
    }

    private static void addDot(int[][] dots, int d, int col) {
        int[] cur = dots[d];
        if (cur == null) {
            dots[d] = new int[]{col};
            return;
        }
        if (cur.length >= 3) return;
        for (int x : cur) if (x == col) return;
        int[] nx = new int[cur.length + 1];
        System.arraycopy(cur, 0, nx, 0, cur.length);
        nx[cur.length] = col;
        dots[d] = nx;
    }

    @Override
    public void refresh() {
        long from = dayMillis(1);
        title.setText(Fmt.date("MMMMy", from));
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(from);
        int dim = c.getActualMaximum(Calendar.DAY_OF_MONTH);
        if (sel > dim) sel = dim;
        c.add(Calendar.MONTH, 1);
        long to = c.getTimeInMillis();
        int[][] dots = new int[32][];
        Calendar t = Calendar.getInstance();
        for (Recur.Occ o : Recur.between(Store.all(a), from, to)) {
            long s = Math.max(o.start, from);
            long e = Math.min(Math.max(o.end - 1, o.start), to - 1);
            t.setTimeInMillis(s);
            int d1 = t.get(Calendar.DAY_OF_MONTH);
            t.setTimeInMillis(e);
            int d2 = t.get(Calendar.DAY_OF_MONTH);
            int col = a.getColor(Event.COLORS[o.e.color]);
            for (int d = d1; d <= d2; d++) addDot(dots, d, col);
        }
        grid.set(y, m, Prefs.firstDay(a), sel, dots);
        showDay();
    }

    private void showDay() {
        long ds = dayMillis(sel);
        List<Object> list = new ArrayList<>(Recur.between(Store.all(a), ds, Recur.nextDay(ds)));
        adapter.set(list);
        String label = Fmt.date("EEEEdMMMM", ds);
        if (ds == Recur.dayStart(System.currentTimeMillis())) label += " · " + a.getString(R.string.today);
        dayLabel.setText(label);
    }
}
