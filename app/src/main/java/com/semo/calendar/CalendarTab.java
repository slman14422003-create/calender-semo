package com.semo.calendar;

import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/** First tab: an iOS segmented control (Day | Week | Month) over the three calendar views, sharing one date. */
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
        LinearLayout seg = new LinearLayout(a);
        seg.setBackground(Ui.shape(Ui.color(a, R.color.neutral_soft), Ui.dp(a, 10)));
        seg.setPadding(Ui.dp(a, 2), Ui.dp(a, 2), Ui.dp(a, 2), Ui.dp(a, 2));
        int[] labels = {R.string.seg_day, R.string.seg_week, R.string.seg_month};
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            TextView t = Ui.text(a, a.getString(labels[i]), 13, R.color.text_secondary, true);
            t.setGravity(Gravity.CENTER);
            t.setOnClickListener(v -> {
                if (idx != mode) select(idx, -1);
            });
            seg.addView(t, new LinearLayout.LayoutParams(0, Ui.dp(a, 30), 1f));
            segs[i] = t;
        }
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2);
        sp.setMargins(Ui.dp(a, 16), Ui.dp(a, 8), Ui.dp(a, 16), 0);
        root.addView(seg, sp);
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

    /** Opens a view; {@code anchorMs} &lt; 0 keeps the date of the view that was open. */
    void select(int i, long anchorMs) {
        long anchor = anchorMs >= 0 ? anchorMs
                : (panes[mode] != null ? panes[mode].anchor() : System.currentTimeMillis());
        DayAware p = pane(i);
        p.setAnchor(anchor);
        boolean changed = i != mode || p.view().getVisibility() != View.VISIBLE;
        mode = i;
        Prefs.setView(a, i);
        for (int k = 0; k < 3; k++) {
            if (panes[k] != null) panes[k].view().setVisibility(k == i ? View.VISIBLE : View.GONE);
            boolean on = k == i;
            segs[k].setTextColor(Ui.color(a, on ? R.color.text_primary : R.color.text_secondary));
            segs[k].setBackground(on ? Ui.shape(Ui.color(a, R.color.surface), Ui.dp(a, 8)) : null);
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
