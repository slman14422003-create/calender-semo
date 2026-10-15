package com.semo.calendar;

import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Rows of events like the iOS agenda: start / end time on the side, a coloured bar, title and place.
 * Day headers sit between rows; a friendly line shows when there is nothing.
 */
final class EventAdapter extends RecyclerView.Adapter<EventAdapter.VH> {
    interface Click {
        void on(Recur.Occ o);
    }

    private static final int HEADER = 0, EVENT = 1, EMPTY = 2;

    private final Context c;
    private final Click click;
    private final boolean withDate;
    private int emptyText;
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
        return items.get(pos) instanceof String ? HEADER : EVENT;
    }

    static final class VH extends RecyclerView.ViewHolder {
        TextView title, sub, t1, t2;
        View bar;

        VH(View v) {
            super(v);
        }
    }

    private TextView line(float sp, int colorRes, boolean medium) {
        TextView t = Ui.text(c, "", sp, colorRes, medium);
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.END);
        return t;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(-1, -2);
        if (type == EVENT) {
            LinearLayout row = new LinearLayout(c);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackground(Ui.shape(Ui.color(c, R.color.surface), Ui.dp(c, 12)));
            row.setPadding(Ui.dp(c, 14), Ui.dp(c, 11), Ui.dp(c, 14), Ui.dp(c, 11));
            lp.setMargins(Ui.dp(c, 16), Ui.dp(c, 4), Ui.dp(c, 16), Ui.dp(c, 4));
            row.setLayoutParams(lp);
            LinearLayout times = Ui.vertical(c);
            TextView t1 = line(14, R.color.text_primary, true);
            TextView t2 = line(12, R.color.text_secondary, false);
            times.addView(t1);
            times.addView(t2);
            row.addView(times, new LinearLayout.LayoutParams(Ui.dp(c, 62), -2));
            View bar = new View(c);
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(Ui.dp(c, 4), Ui.dp(c, 36));
            bp.setMarginEnd(Ui.dp(c, 12));
            row.addView(bar, bp);
            LinearLayout col = Ui.vertical(c);
            TextView title = line(16, R.color.text_primary, false);
            TextView sub = line(13, R.color.text_secondary, false);
            col.addView(title);
            col.addView(sub);
            row.addView(col, new LinearLayout.LayoutParams(0, -2, 1f));
            Ui.press(row);
            VH h = new VH(row);
            h.title = title;
            h.sub = sub;
            h.t1 = t1;
            h.t2 = t2;
            h.bar = bar;
            return h;
        }
        TextView t = Ui.text(c, "", type == HEADER ? 13 : 15,
                type == HEADER ? R.color.text_secondary : R.color.text_hint, type == HEADER);
        if (type == EMPTY) t.setGravity(Gravity.CENTER);
        t.setPadding(Ui.dp(c, 20), Ui.dp(c, type == HEADER ? 14 : 28), Ui.dp(c, 20), Ui.dp(c, 4));
        t.setLayoutParams(lp);
        VH h = new VH(t);
        h.title = t;
        return h;
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        int type = getItemViewType(pos);
        if (type == EMPTY) {
            h.title.setText(emptyText);
        } else if (type == HEADER) {
            h.title.setText((String) items.get(pos));
        } else {
            final Recur.Occ o = (Recur.Occ) items.get(pos);
            h.title.setText(o.e.title.isEmpty() ? c.getString(R.string.untitled) : o.e.title);
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
            h.bar.setBackground(Ui.shape(Ui.color(c, Event.COLORS[o.e.color]), Ui.dp(c, 2)));
            h.itemView.setOnClickListener(v -> click.on(o));
        }
    }
}
