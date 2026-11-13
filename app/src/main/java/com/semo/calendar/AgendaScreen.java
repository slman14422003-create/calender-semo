package com.semo.calendar;

import android.graphics.Typeface;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.List;

/** Agenda tab: collapsing large title, the next 90 days grouped by day in iOS inset-grouped cards. */
final class AgendaScreen implements Screen {
    private final MainActivity a;
    private final LinearLayout root;
    private final EventAdapter adapter;
    private final LinearLayout glance;

    AgendaScreen(MainActivity act) {
        a = act;
        root = Ui.vertical(a);
        root.setBackgroundColor(Ui.color(a, R.color.bg_group));

        ImageButton plus = Ui.glassButton(a, R.drawable.ic_add, R.color.accent, 40);
        plus.setContentDescription(a.getString(R.string.new_event));
        plus.setOnClickListener(v -> a.openEditor(null, 0));
        final LargeNav nav = new LargeNav(a, a.getString(R.string.tab_agenda), plus);
        root.addView(nav.view(), new LinearLayout.LayoutParams(-1, nav.height()));

        TextView t = Ui.text(a, a.getString(R.string.tab_agenda), 32, R.color.text_primary, false);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(Ui.dp(a, 20), Ui.dp(a, 2), Ui.dp(a, 20), Ui.dp(a, 6));
        LinearLayout head = Ui.vertical(a);
        head.addView(t);
        glance = Ui.group(a, 4);
        glance.setPadding(Ui.dp(a, 16), Ui.dp(a, 12), Ui.dp(a, 16), Ui.dp(a, 12));
        head.addView(glance);

        final RecyclerView rv = new RecyclerView(a);
        rv.setLayoutManager(new LinearLayoutManager(a));
        rv.setClipToPadding(false);
        rv.setPadding(0, 0, 0, Ui.dp(a, 160));
        rv.setOverScrollMode(View.OVER_SCROLL_NEVER);
        adapter = new EventAdapter(a, false, R.string.no_events, o -> a.showDetail(o));
        adapter.setTaskHandler(a.taskHandler());
        rv.setAdapter(new ConcatAdapter(new HeaderAdapter(head), adapter));
        new ItemTouchHelper(new SwipeDelete(a)).attachToRecyclerView(rv);
        rv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView r, int dx, int dy) {
                nav.onScroll(r.computeVerticalScrollOffset());
            }
        });
        root.addView(rv, new LinearLayout.LayoutParams(-1, 0, 1f));
    }

    @Override
    public View view() {
        return root;
    }

    private void fillGlance() {
        glance.removeAllViews();
        if (!Prefs.smart(a)) {
            glance.setVisibility(View.GONE);
            return;
        }
        glance.setVisibility(View.VISIBLE);
        TextView h = Ui.text(a, a.getString(R.string.glance), 13, R.color.accent, true);
        h.setPadding(0, 0, 0, Ui.dp(a, 4));
        glance.addView(h);
        for (String line : Smart.glance(a)) {
            TextView l = Ui.text(a, line, 15, R.color.text_primary, false);
            l.setPadding(0, Ui.dp(a, 2), 0, Ui.dp(a, 2));
            glance.addView(l);
        }
    }

    @Override
    public void refresh() {
        fillGlance();
        long now = System.currentTimeMillis();
        long from = Recur.dayStart(now);
        long to = from + 90 * Recur.DAY;
        TreeMap<Long, List<Object>> byDay = new TreeMap<>();
        for (Recur.Occ o : Store.between(a, from, to)) {
            long key = Recur.dayStart(Math.max(o.start, from));
            List<Object> l = byDay.get(key);
            if (l == null) {
                l = new ArrayList<>();
                byDay.put(key, l);
            }
            l.add(o);
        }
        for (Task t : Tasks.between(a, from, to)) {
            long key = Recur.dayStart(t.due);
            List<Object> l = byDay.get(key);
            if (l == null) {
                l = new ArrayList<>();
                byDay.put(key, l);
            }
            l.add(t);
        }
        List<Object> list = new ArrayList<>();
        for (Map.Entry<Long, List<Object>> en : byDay.entrySet()) {
            long key = en.getKey();
            String h = Fmt.date("EEEEdMMMM", key);
            String rel = Fmt.dayLabel(a, key);
            if (rel != null) h = rel + " \u00b7 " + h;
            list.add(key == from ? EventAdapter.TODAY + h : h);
            list.addAll(en.getValue());
        }
        adapter.set(list);
    }
}
