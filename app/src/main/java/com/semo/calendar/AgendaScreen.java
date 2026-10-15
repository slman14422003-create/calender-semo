package com.semo.calendar;

import android.graphics.Typeface;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/** Agenda tab: the next 90 days as one scrolling list, grouped by day (Android-style agenda, iOS-style look). */
final class AgendaScreen implements Screen {
    private final MainActivity a;
    private final LinearLayout root;
    private final EventAdapter adapter;

    AgendaScreen(MainActivity act) {
        a = act;
        root = Ui.vertical(a);
        root.setBackgroundColor(Ui.color(a, R.color.bg_group));
        TextView t = Ui.text(a, a.getString(R.string.tab_agenda), 26, R.color.text_primary, false);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(Ui.dp(a, 20), Ui.dp(a, 10), Ui.dp(a, 20), Ui.dp(a, 6));
        root.addView(t);
        RecyclerView rv = new RecyclerView(a);
        rv.setLayoutManager(new LinearLayoutManager(a));
        rv.setClipToPadding(false);
        rv.setPadding(0, 0, 0, Ui.dp(a, 160));
        rv.setOverScrollMode(View.OVER_SCROLL_NEVER);
        adapter = new EventAdapter(a, false, R.string.no_events, o -> a.showDetail(o));
        rv.setAdapter(adapter);
        root.addView(rv, new LinearLayout.LayoutParams(-1, 0, 1f));
    }

    @Override
    public View view() {
        return root;
    }

    @Override
    public void refresh() {
        long now = System.currentTimeMillis();
        long from = Recur.dayStart(now);
        long to = from + 90 * Recur.DAY;
        List<Object> list = new ArrayList<>();
        long lastKey = -1;
        for (Recur.Occ o : Recur.between(Store.all(a), from, to)) {
            long key = Recur.dayStart(Math.max(o.start, from));
            if (key != lastKey) {
                lastKey = key;
                String h = Fmt.date("EEEEdMMMM", key);
                if (key == from) h += " · " + a.getString(R.string.today);
                list.add(h);
            }
            list.add(o);
        }
        adapter.set(list);
    }
}
