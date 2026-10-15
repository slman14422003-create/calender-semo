package com.semo.calendar;

import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Tasks tab: collapsing large title; Overdue, Today, Upcoming, No date and Completed in grouped cards. */
final class TasksScreen implements Screen {
    private final MainActivity a;
    private final LinearLayout root;
    private final LinearLayout body;

    TasksScreen(MainActivity act) {
        a = act;
        root = Ui.vertical(a);
        root.setBackgroundColor(Ui.color(a, R.color.bg_group));
        ImageButton plus = Ui.iconButton(a, R.drawable.ic_add, R.color.accent, 40);
        plus.setContentDescription(a.getString(R.string.new_task));
        plus.setOnClickListener(v -> TaskSheet.show(a, null, 0));
        final LargeNav nav = new LargeNav(a, a.getString(R.string.tab_tasks), plus);
        root.addView(nav.view(), new LinearLayout.LayoutParams(-1, nav.height()));
        ScrollView sv = new ScrollView(a);
        sv.setClipToPadding(false);
        sv.setPadding(0, 0, 0, Ui.dp(a, 140));
        sv.setOverScrollMode(View.OVER_SCROLL_NEVER);
        sv.setOnScrollChangeListener((v, x, y, ox, oy) -> nav.onScroll(y));
        body = Ui.vertical(a);
        sv.addView(body, new ScrollView.LayoutParams(-1, -2));
        root.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1f));
    }

    @Override
    public View view() {
        return root;
    }

    private View row(final Task t) {
        LinearLayout r = new LinearLayout(a);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(Ui.dp(a, 16), Ui.dp(a, 11), Ui.dp(a, 16), Ui.dp(a, 11));
        FrameLayout ring = new FrameLayout(a);
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        if (t.done) {
            g.setColor(Ui.color(a, R.color.accent));
            ImageView tick = new ImageView(a);
            tick.setImageResource(R.drawable.ic_check);
            tick.setImageTintList(android.content.res.ColorStateList.valueOf(0xFFFFFFFF));
            ring.addView(tick, new FrameLayout.LayoutParams(Ui.dp(a, 15), Ui.dp(a, 15), Gravity.CENTER));
        } else {
            g.setColor(0);
            g.setStroke(Ui.dp(a, 2), Ui.color(a, R.color.accent));
        }
        ring.setBackground(g);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(Ui.dp(a, 24), Ui.dp(a, 24));
        rp.setMarginEnd(Ui.dp(a, 14));
        r.addView(ring, rp);
        ring.setOnClickListener(v -> {
            t.done = !t.done;
            Tasks.save(a, t);
            Alarms.scheduleTask(a, t);
            refresh();
        });
        LinearLayout col = Ui.vertical(a);
        TextView title = Ui.text(a, t.title.isEmpty() ? a.getString(R.string.untitled) : t.title, 17,
                t.done ? R.color.text_secondary : R.color.text_primary, false);
        if (t.done) title.setPaintFlags(title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        col.addView(title);
        String sub = Fmt.taskDue(a, t);
        if (!sub.isEmpty()) col.addView(Ui.text(a, sub, 13, R.color.text_secondary, false));
        r.addView(col, new LinearLayout.LayoutParams(0, -2, 1f));
        r.setOnClickListener(v -> TaskSheet.show(a, t, 0));
        Ui.press(r);
        return r;
    }

    private void section(CharSequence header, int headerColor, List<Task> list, View trailing) {
        if (list.isEmpty()) return;
        LinearLayout hr = new LinearLayout(a);
        hr.setGravity(Gravity.CENTER_VERTICAL);
        TextView h = Ui.sectionHeader(a, header);
        h.setTextColor(Ui.color(a, headerColor));
        hr.addView(h, new LinearLayout.LayoutParams(0, -2, 1f));
        if (trailing != null) hr.addView(trailing);
        body.addView(hr);
        LinearLayout g = Ui.group(a, 0);
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) g.addView(Ui.hairline(a, 54));
            g.addView(row(list.get(i)));
        }
        body.addView(g);
    }

    @Override
    public void refresh() {
        body.removeAllViews();
        TextView title = Ui.text(a, a.getString(R.string.tab_tasks), 34, R.color.text_primary, false);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(Ui.dp(a, 20), Ui.dp(a, 2), Ui.dp(a, 20), Ui.dp(a, 0));
        body.addView(title);

        long now = System.currentTimeMillis();
        long today0 = Recur.dayStart(now);
        long tomorrow0 = Recur.nextDay(today0);
        List<Task> overdue = new ArrayList<>(), today = new ArrayList<>(), upcoming = new ArrayList<>();
        List<Task> nodate = new ArrayList<>(), done = new ArrayList<>();
        List<Task> all = Tasks.all(a);
        Collections.sort(all, (x, y) -> Long.compare(x.due, y.due));
        for (Task t : all) {
            if (t.done) done.add(t);
            else if (t.due == 0) nodate.add(t);
            else if (t.hasTime ? t.due < now : t.due < today0) overdue.add(t);
            else if (t.due < tomorrow0) today.add(t);
            else upcoming.add(t);
        }
        if (all.isEmpty()) {
            TextView none = Ui.text(a, a.getString(R.string.no_tasks), 15, R.color.text_hint, false);
            none.setGravity(Gravity.CENTER);
            none.setPadding(0, Ui.dp(a, 60), 0, 0);
            body.addView(none);
            return;
        }
        section(a.getString(R.string.overdue), R.color.bad, overdue, null);
        section(a.getString(R.string.today), R.color.accent, today, null);
        section(a.getString(R.string.upcoming), R.color.text_secondary, upcoming, null);
        section(a.getString(R.string.no_date), R.color.text_secondary, nodate, null);
        TextView clear = Ui.text(a, a.getString(R.string.clear_completed), 15, R.color.accent, false);
        clear.setPadding(Ui.dp(a, 16), Ui.dp(a, 18), Ui.dp(a, 32), Ui.dp(a, 6));
        clear.setOnClickListener(v -> {
            Tasks.clearDone(a);
            refresh();
        });
        section(a.getString(R.string.completed), R.color.text_secondary, done, clear);
    }
}
