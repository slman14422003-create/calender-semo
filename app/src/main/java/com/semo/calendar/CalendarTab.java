package com.semo.calendar;

import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * First tab. Top bar like an iOS navigation bar: "Today" on the leading side, the Day | Week | Month | Year
 * segmented control in the middle, "+" on the trailing side; below it the large title of the chosen view.
 */
final class CalendarTab implements Screen {
    private static final int COUNT = 4;

    private final MainActivity a;
    private final LinearLayout root;
    private final FrameLayout host;
    private final DayAware[] panes = new DayAware[COUNT];
    private final TextView[] segs = new TextView[COUNT];
    private final View[] divs = new View[COUNT - 1];
    private View thumb;
    private LinearLayout segRow;
    private int mode;

    CalendarTab(MainActivity act) {
        a = act;
        root = Ui.vertical(a);

        LinearLayout nav = new LinearLayout(a);
        nav.setGravity(Gravity.CENTER_VERTICAL);
        nav.setPadding(Ui.dp(a, 4), Ui.dp(a, 6), Ui.dp(a, 4), Ui.dp(a, 4));

        TextView today = Ui.text(a, a.getString(R.string.today), 17, R.color.accent, false);
        today.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        today.setSingleLine(true);
        androidx.core.widget.TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                today, 11, 17, 1, android.util.TypedValue.COMPLEX_UNIT_SP);
        today.setPaddingRelative(Ui.dp(a, 10), 0, 0, 0);
        today.setOnClickListener(v -> pane(mode).goToday());
        Ui.press(today);
        nav.addView(today, new LinearLayout.LayoutParams(Ui.dp(a, 68), Ui.dp(a, 40)));

        FrameLayout seg = new FrameLayout(a);
        seg.setBackground(Ui.shape(Ui.color(a, R.color.seg_track), Ui.dp(a, 9)));
        seg.setPadding(Ui.dp(a, 2), Ui.dp(a, 2), Ui.dp(a, 2), Ui.dp(a, 2));
        thumb = new View(a);
        thumb.setBackground(Ui.shape(Ui.color(a, R.color.seg_thumb), Ui.dp(a, 7)));
        thumb.setElevation(Ui.dp(a, 1));
        thumb.setVisibility(View.INVISIBLE);
        seg.addView(thumb, new FrameLayout.LayoutParams(Ui.dp(a, 60), Ui.dp(a, 28), Gravity.LEFT | Gravity.TOP));
        segRow = new LinearLayout(a);
        int[] labels = {R.string.seg_day, R.string.seg_week, R.string.seg_month, R.string.seg_year};
        for (int i = 0; i < COUNT; i++) {
            final int idx = i;
            TextView t = Ui.text(a, a.getString(labels[i]), 13, R.color.text_primary, false);
            t.setGravity(Gravity.CENTER);
            t.setSingleLine(true);
            androidx.core.widget.TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                    t, 9, 13, 1, android.util.TypedValue.COMPLEX_UNIT_SP);
            t.setOnClickListener(v -> {
                if (idx != mode) select(idx, -1);
            });
            segRow.addView(t, new LinearLayout.LayoutParams(0, Ui.dp(a, 28), 1f));
            segs[i] = t;
            if (i < COUNT - 1) {
                View dv = new View(a);
                dv.setBackgroundColor(Ui.color(a, R.color.stroke));
                LinearLayout.LayoutParams dl = new LinearLayout.LayoutParams(Math.max(1, Ui.dp(a, 1)), Ui.dp(a, 14));
                dl.gravity = Gravity.CENTER_VERTICAL;
                segRow.addView(dv, dl);
                divs[i] = dv;
            }
        }
        seg.addView(segRow, new FrameLayout.LayoutParams(-1, -2));
        segRow.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            if (r - l != or - ol) moveThumb(false);
        });
        nav.addView(seg, new LinearLayout.LayoutParams(0, -2, 1f));

        FrameLayout right = new FrameLayout(a);
        ImageButton plus = Ui.iconButton(a, R.drawable.ic_add, R.color.accent, 40);
        plus.setContentDescription(a.getString(R.string.new_event));
        plus.setOnClickListener(v -> a.openEditor(null, selectedMillis()));
        right.addView(plus, new FrameLayout.LayoutParams(Ui.dp(a, 40), Ui.dp(a, 40), Gravity.END));
        nav.addView(right, new LinearLayout.LayoutParams(Ui.dp(a, 68), Ui.dp(a, 40)));
        root.addView(nav);

        host = new FrameLayout(a);
        root.addView(host, new LinearLayout.LayoutParams(-1, 0, 1f));
        mode = Math.min(COUNT - 1, Prefs.view(a));
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
            else if (i == 2) panes[i] = new MonthScreen(a);
            else panes[i] = new YearScreen(a, ms -> select(2, ms));
            host.addView(panes[i].view(), new FrameLayout.LayoutParams(-1, -1));
        }
        return panes[i];
    }

    /** Slides the white thumb under the chosen segment. */
    private void moveThumb(boolean animate) {
        TextView t = segs[mode];
        if (t == null || t.getWidth() == 0) return;
        android.view.ViewGroup.LayoutParams lp = thumb.getLayoutParams();
        if (lp.width != t.getWidth()) {
            lp.width = t.getWidth();
            thumb.setLayoutParams(lp);
        }
        thumb.setVisibility(View.VISIBLE);
        thumb.animate().cancel();
        float x = t.getLeft();
        if (animate) {
            thumb.animate().translationX(x).setDuration(440).setInterpolator(Motion.SPRING_SOFT).start();
        } else {
            thumb.setTranslationX(x);
        }
    }

    /** Opens a view; {@code anchorMs} below 0 keeps the date of the view that was open. */
    void select(int i, long anchorMs) {
        long anchor = anchorMs >= 0 ? anchorMs
                : (panes[mode] != null ? panes[mode].anchor() : System.currentTimeMillis());
        DayAware p = pane(i);
        p.setAnchor(anchor);
        final int old = mode;
        boolean changed = i != old;
        mode = i;
        Prefs.setView(a, i);
        for (int k = 0; k < COUNT; k++) {
            if (panes[k] != null) {
                View pv = panes[k].view();
                pv.animate().cancel();
                pv.setAlpha(1f);
                pv.setScaleX(1f);
                pv.setScaleY(1f);
            }
        }
        for (int k = 0; k < COUNT; k++) {
            if (panes[k] != null) {
                boolean keep = k == i || (changed && k == old);
                panes[k].view().setVisibility(keep ? View.VISIBLE : View.GONE);
            }
            boolean on = k == i;
            segs[k].setTypeface(on ? Typeface.create("sans-serif-medium", Typeface.BOLD) : Typeface.DEFAULT);
        }
        for (int k = 0; k < COUNT - 1; k++) {
            divs[k].setVisibility(k == i || k + 1 == i ? View.INVISIBLE : View.VISIBLE);
        }
        p.refresh();
        moveThumb(changed);
        if (changed) {
            boolean zoomOut = i > old;
            final View in = p.view();
            in.setAlpha(0f);
            in.setScaleX(zoomOut ? 1.06f : 0.94f);
            in.setScaleY(zoomOut ? 1.06f : 0.94f);
            in.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(380).setInterpolator(Motion.OUT).start();
            if (panes[old] != null) {
                final View out = panes[old].view();
                out.animate().alpha(0f).scaleX(zoomOut ? 0.94f : 1.06f).scaleY(zoomOut ? 0.94f : 1.06f)
                        .setDuration(200).setInterpolator(Motion.IN).withEndAction(() -> {
                            if (mode != old) {
                                out.setVisibility(View.GONE);
                                out.setAlpha(1f);
                                out.setScaleX(1f);
                                out.setScaleY(1f);
                            }
                        }).start();
            }
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
