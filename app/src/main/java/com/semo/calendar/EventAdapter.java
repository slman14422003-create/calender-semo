package com.semo.calendar;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/** Rows of events (cards with a colour bar) and day headers; shows a friendly empty line when there is nothing. */
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
        TextView a, b;
        View bar;

        VH(View v) {
            super(v);
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(-1, -2);
        if (type == EVENT) {
            LinearLayout row = new LinearLayout(c);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackground(Ui.shape(Ui.color(c, R.color.surface), Ui.dp(c, 12)));
            row.setPadding(Ui.dp(c, 14), Ui.dp(c, 12), Ui.dp(c, 14), Ui.dp(c, 12));
            lp.setMargins(Ui.dp(c, 16), Ui.dp(c, 5), Ui.dp(c, 16), Ui.dp(c, 5));
            row.setLayoutParams(lp);
            View bar = new View(c);
            row.addView(bar, new LinearLayout.LayoutParams(Ui.dp(c, 4), Ui.dp(c, 38)));
            LinearLayout col = Ui.vertical(c);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, -2, 1f);
            cp.setMarginStart(Ui.dp(c, 12));
            TextView t = Ui.text(c, "", 16, R.color.text_primary, false);
            t.setSingleLine(true);
            t.setEllipsize(android.text.TextUtils.TruncateAt.END);
            TextView s = Ui.text(c, "", 13, R.color.text_secondary, false);
            s.setSingleLine(true);
            s.setEllipsize(android.text.TextUtils.TruncateAt.END);
            col.addView(t);
            col.addView(s);
            row.addView(col, cp);
            Ui.press(row);
            VH h = new VH(row);
            h.a = t;
            h.b = s;
            h.bar = bar;
            return h;
        }
        TextView t = Ui.text(c, "", type == HEADER ? 13 : 15,
                type == HEADER ? R.color.text_secondary : R.color.text_hint, type == HEADER);
        if (type == EMPTY) t.setGravity(Gravity.CENTER);
        t.setPadding(Ui.dp(c, 20), Ui.dp(c, type == HEADER ? 14 : 28), Ui.dp(c, 20), Ui.dp(c, 4));
        t.setLayoutParams(lp);
        VH h = new VH(t);
        h.a = t;
        return h;
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        int type = getItemViewType(pos);
        if (type == EMPTY) {
            h.a.setText(emptyText);
        } else if (type == HEADER) {
            h.a.setText((String) items.get(pos));
        } else {
            final Recur.Occ o = (Recur.Occ) items.get(pos);
            h.a.setText(o.e.title.isEmpty() ? c.getString(R.string.untitled) : o.e.title);
            h.b.setText(Fmt.line(c, o, withDate));
            h.bar.setBackground(Ui.shape(Ui.color(c, Event.COLORS[o.e.color]), Ui.dp(c, 2)));
            h.itemView.setOnClickListener(v -> click.on(o));
        }
    }
}
