package com.semo.calendar;

import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * First tab. Top bar like an iOS navigation bar: "Today" on the leading side, the Day | Week | Month segmented
 * control in the middle, "+" on the trailing side; below it the large title of the chosen view.
 */
final class CalendarTab implements Screen {
    private final MainActivity a;
    private final LinearLayout root;
    private final FrameLayout host;
    private final DayAware[] panes = new DayAware[3];
    private final TextView[] segs = new TextView[3];
    private int mode;

    CalendarTab(MainActivity act) {
        a = act;
        root = Ui.vertical(a);

        LinearLayout nav = new LinearLayout(a);
        nav.setGravity(Gravity.CENTER_VERTICAL);
        nav.setPadding(Ui.dp(a, 4), Ui.dp(a, 6), Ui.dp(a, 4), Ui.dp(a, 4));

        TextView today = Ui.text(a, a.getString(R.string.today), 17, R.color.accent, false);
        today.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        today.setPadding(Ui.dp(a, 12), 0, 0, 0);
        today.setOnClickListener(v -> pane(mode).goToday());
        Ui.press(today);
        nav.addView(today, new LinearLayout.LayoutParams(Ui.dp(a, 76), Ui.dp(a, 40)));

        LinearLayout seg = new LinearLayout(a);
        seg.setBackground(Ui.shape(Ui.color(a, R.color.seg_track), Ui.dp(a, 9)));
        seg.setPadding(Ui.dp(a, 2), Ui.dp(a, 2), Ui.dp(a, 2), Ui.dp(a, 2));
        int[] labels = {R.string.seg_day, R.string.seg_week, R.string.seg_month};
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            TextView t = Ui.text(a, a.getString(labels[i]), 13, R.color.text_primary, false);
            t.setGravity(Gravity.CENTER);
            t.setSingleLine(true);
            t.setOnClickListener(v -> {
                if (idx != mode) select(idx, -1);
            });
            seg.addView(t, new LinearLayout.LayoutParams(0, Ui.dp(a, 28), 1f));
            segs[i] = t;
        }
        nav.addView(seg, new LinearLayout.LayoutParams(0, -2, 1f));

        FrameLayout right = new FrameLayout(a);
        ImageButton plus = Ui.iconButton(a, R.drawable.ic_add, R.color.accent, 40);
        plus.setContentDescription(a.getString(R.string.new_event));
        plus.setOnClickListener(v -> a.openEditor(null, selectedMillis()));
        right.addView(plus, new FrameLayout.LayoutParams(Ui.dp(a, 40), Ui.dp(a, 40), Gravity.END));
        nav.addView(right, new LinearLayout.LayoutParams(Ui.dp(a, 76), Ui.dp(a, 40)));
        root.addView(nav);

        host = new FrameLayout(a);
        root.addView(host, new LinearLayout.LayoutParams(-1, 0, 1f));
        mode = Prefs.view(a);
        select(mode, System.currentTimeMillis());
    }

    @Override
    public View view() {
        return root;
    }

    private DayAware pane(int i) {
        if (panes[i] == null) {
            if (i == 0) panes[i] = new TimeScreen(a, 1, ms -> {
            });
            else if (i == 1) panes[i] = new TimeScreen(a, 7, ms -> select(0, ms));
            else panes[i] = new MonthScreen(a);
            host.addView(panes[i].view(), new FrameLayout.LayoutParams(-1, -1));
        }
        return panes[i];
    }

    /** Opens a view; {@code anchorMs} below 0 keeps the date of the view that was open. */
    void select(int i, long anchorMs) {
        long anchor = anchorMs >= 0 ? anchorMs
                : (panes[mode] != null ? panes[mode].anchor() : System.currentTimeMillis());
        DayAware p = pane(i);
        p.setAnchor(anchor);
        boolean changed = i != mode;
        mode = i;
        Prefs.setView(a, i);
        for (int k = 0; k < 3; k++) {
            if (panes[k] != null) panes[k].view().setVisibility(k == i ? View.VISIBLE : View.GONE);
            boolean on = k == i;
            segs[k].setTypeface(on ? Typeface.create("sans-serif-medium", Typeface.BOLD) : Typeface.DEFAULT);
            if (on) {
                android.graphics.drawable.GradientDrawable thumb =
                        Ui.shape(Ui.color(a, R.color.seg_thumb), Ui.dp(a, 7));
                segs[k].setBackground(thumb);
                segs[k].setElevation(Ui.dp(a, 1));
            } else {
                segs[k].setBackground(null);
                segs[k].setElevation(0);
            }
        }
        p.refresh();
        if (changed) {
            p.view().setAlpha(0f);
            p.view().animate().alpha(1f).setDuration(160).start();
        }
    }

    long selectedMillis() {
        return Recur.dayStart(pane(mode).anchor());
    }

    @Override
    public void refresh() {
        pane(mode).refresh();
    }
}
