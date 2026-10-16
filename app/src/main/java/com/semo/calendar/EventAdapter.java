package com.semo.calendar;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Events and tasks as an iOS inset-grouped list: consecutive rows share one rounded card, rows are separated by
 * hairlines. Events show start / end time and a colour bar, tasks a round check button. Day headers sit between
 * the groups.
 */
final class EventAdapter extends RecyclerView.Adapter<EventAdapter.VH> {
    interface Click {
        void on(Recur.Occ o);
    }

    interface TaskHandler {
        void open(Task t);

        void toggle(Task t);
    }

    /** Prefix that marks the header of today in the list (drawn in the accent colour). */
    static final String TODAY = "\u0001";

    private static final int HEADER = 0, EVENT = 1, EMPTY = 2, TASK = 3;

    private final Context c;
    private final Click click;
    private final boolean withDate;
    private int emptyText;
    private TaskHandler taskHandler;
    private final List<Object> items = new ArrayList<>();

    EventAdapter(Context c, boolean withDate, int emptyText, Click click) {
        this.c = c;
        this.withDate = withDate;
        this.emptyText = emptyText;
        this.click = click;
    }

    void setEmpty(int res) {
        emptyText = res;
    }

    void setTaskHandler(TaskHandler h) {
        taskHandler = h;
    }

    void set(List<Object> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return items.isEmpty() ? 1 : items.size();
    }

    @Override
    public int getItemViewType(int pos) {
        if (items.isEmpty()) return EMPTY;
        Object o = items.get(pos);
        return o instanceof String ? HEADER : o instanceof Task ? TASK : EVENT;
    }

    static final class VH extends RecyclerView.ViewHolder {
        TextView title, sub, t1, t2;
        View bar, content, divider;
        FrameLayout ring;
        ImageView tick;
        Recur.Occ occ;
        Task task;

        VH(View v) {
            super(v);
        }
    }

    private static boolean isRow(Object o) {
        return o instanceof Recur.Occ || o instanceof Task;
    }

    private TextView line(float sp, int colorRes, boolean medium) {
        TextView t = Ui.text(c, "", sp, colorRes, medium);
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.END);
        return t;
    }

    private void highlight(View root) {
        Ui.highlight(root);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(-1, -2);
        if (type == EVENT || type == TASK) {
            LinearLayout root = Ui.vertical(c);
            lp.setMargins(Ui.dp(c, 16), 0, Ui.dp(c, 16), 0);
            root.setLayoutParams(lp);
            LinearLayout content = new LinearLayout(c);
            content.setGravity(Gravity.CENTER_VERTICAL);
            content.setPadding(Ui.dp(c, 16), Ui.dp(c, 11), Ui.dp(c, 16), Ui.dp(c, 11));
            VH h;
            if (type == EVENT) {
                LinearLayout times = Ui.vertical(c);
                TextView t1 = line(14, R.color.text_primary, true);
                TextView t2 = line(12, R.color.text_secondary, false);
                times.addView(t1);
                times.addView(t2);
                content.addView(times, new LinearLayout.LayoutParams(Ui.dp(c, 62), -2));
                View bar = new View(c);
                LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(Ui.dp(c, 4), Ui.dp(c, 36));
                bp.setMarginEnd(Ui.dp(c, 12));
                content.addView(bar, bp);
                h = new VH(root);
                h.t1 = t1;
                h.t2 = t2;
                h.bar = bar;
            } else {
                FrameLayout ring = new FrameLayout(c);
                ImageView tick = new ImageView(c);
                tick.setImageResource(R.drawable.ic_check);
                tick.setImageTintList(android.content.res.ColorStateList.valueOf(0xFFFFFFFF));
                ring.addView(tick, new FrameLayout.LayoutParams(Ui.dp(c, 15), Ui.dp(c, 15), Gravity.CENTER));
                LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(Ui.dp(c, 24), Ui.dp(c, 24));
                rp.setMarginEnd(Ui.dp(c, 14));
                content.addView(ring, rp);
                h = new VH(root);
                h.ring = ring;
                h.tick = tick;
            }
            LinearLayout col = Ui.vertical(c);
            TextView title = line(16, R.color.text_primary, false);
            TextView sub = line(13, R.color.text_secondary, false);
            col.addView(title);
            col.addView(sub);
            content.addView(col, new LinearLayout.LayoutParams(0, -2, 1f));
            root.addView(content);
            View divider = Ui.hairline(c, 16);
            root.addView(divider);
            highlight(root);
            h.title = title;
            h.sub = sub;
            h.content = content;
            h.divider = divider;
            return h;
        }
        TextView t = Ui.text(c, "", type == HEADER ? 13 : 15,
                type == HEADER ? R.color.text_secondary : R.color.text_hint, type == HEADER);
        if (type == EMPTY) t.setGravity(Gravity.CENTER);
        t.setPadding(Ui.dp(c, 32), Ui.dp(c, type == HEADER ? 18 : 28), Ui.dp(c, 32), Ui.dp(c, 6));
        t.setLayoutParams(lp);
        VH h = new VH(t);
        h.title = t;
        return h;
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        int type = getItemViewType(pos);
        h.occ = null;
        h.task = null;
        if (type == EMPTY) {
            h.title.setText(emptyText);
            return;
        }
        if (type == HEADER) {
            String hs = (String) items.get(pos);
            boolean isToday = hs.startsWith(TODAY);
            h.title.setText(isToday ? hs.substring(TODAY.length()) : hs);
            h.title.setTextColor(Ui.color(c, isToday ? R.color.accent : R.color.text_secondary));
            return;
        }
        boolean first = pos == 0 || !isRow(items.get(pos - 1));
        boolean last = pos == items.size() - 1 || !isRow(items.get(pos + 1));
        float r = Ui.dp(c, 10);
        float tr = first ? r : 0;
        float br = last ? r : 0;
        float[] radii = {tr, tr, tr, tr, br, br, br, br};
        GradientDrawable g = new GradientDrawable();
        g.setColor(Ui.color(c, R.color.surface));
        g.setCornerRadii(radii);
        h.itemView.setBackground(g);
        GradientDrawable hl = new GradientDrawable();
        hl.setColor(Ui.color(c, R.color.cell_highlight));
        hl.setCornerRadii(radii);
        h.itemView.setTag(hl);
        h.divider.setVisibility(last ? View.GONE : View.VISIBLE);

        if (type == TASK) {
            final Task t = (Task) items.get(pos);
            h.task = t;
            h.title.setText(t.title.isEmpty() ? c.getString(R.string.untitled) : t.title);
            h.title.setTextColor(Ui.color(c, t.done ? R.color.text_secondary : R.color.text_primary));
            h.title.setPaintFlags(t.done ? h.title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
                    : h.title.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            String sub = Fmt.taskDue(c, t);
            h.sub.setText(sub);
            h.sub.setVisibility(sub.isEmpty() ? View.GONE : View.VISIBLE);
            GradientDrawable ring = new GradientDrawable();
            ring.setShape(GradientDrawable.OVAL);
            if (t.done) {
                ring.setColor(Ui.color(c, R.color.accent));
            } else {
                ring.setColor(0);
                ring.setStroke(Ui.dp(c, 2), Ui.color(c, R.color.accent));
            }
            h.ring.setBackground(ring);
            h.tick.setVisibility(t.done ? View.VISIBLE : View.GONE);
            h.content.setAlpha(t.done ? 0.7f : 1f);
            h.ring.setOnClickListener(v -> {
                if (taskHandler != null) taskHandler.toggle(t);
            });
            h.itemView.setOnClickListener(v -> {
                if (taskHandler != null) taskHandler.open(t);
            });
            return;
        }

        final Recur.Occ o = (Recur.Occ) items.get(pos);
        h.occ = o;
        h.title.setText(o.e.label(c.getString(R.string.untitled)));
        String sub = Fmt.sub(o, withDate);
        h.sub.setText(sub);
        h.sub.setVisibility(sub.isEmpty() ? View.GONE : View.VISIBLE);
        if (o.e.allDay) {
            h.t1.setText(R.string.all_day);
            h.t2.setVisibility(View.GONE);
        } else {
            h.t1.setText(Fmt.time(c, o.start));
            h.t2.setText(Fmt.time(c, o.end));
            h.t2.setVisibility(View.VISIBLE);
        }
        h.bar.setBackground(Ui.shape(Cals.color(c, o.e), Ui.dp(c, 2)));
        h.content.setAlpha(o.end < System.currentTimeMillis() ? 0.5f : 1f);
        h.itemView.setOnClickListener(v -> click.on(o));
    }
}
