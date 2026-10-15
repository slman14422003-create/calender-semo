package com.semo.calendar;

import android.graphics.Typeface;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Search tab: finds events by title, place or notes, two years back and two years ahead. */
final class SearchScreen implements Screen {
    private final MainActivity a;
    private final LinearLayout root;
    private final EditText field;
    private final EventAdapter adapter;

    SearchScreen(MainActivity act) {
        a = act;
        root = Ui.vertical(a);
        root.setBackgroundColor(Ui.color(a, R.color.bg_group));
        TextView t = Ui.text(a, a.getString(R.string.tab_search), 26, R.color.text_primary, false);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(Ui.dp(a, 20), Ui.dp(a, 10), Ui.dp(a, 20), Ui.dp(a, 8));
        root.addView(t);
        field = new EditText(a);
        field.setHint(R.string.search_hint);
        field.setHintTextColor(Ui.color(a, R.color.text_hint));
        field.setTextColor(Ui.color(a, R.color.text_primary));
        field.setTextSize(17);
        field.setSingleLine(true);
        field.setInputType(InputType.TYPE_CLASS_TEXT);
        field.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        field.setBackground(Ui.shape(Ui.color(a, R.color.neutral_soft), Ui.dp(a, 12)));
        field.setPadding(Ui.dp(a, 14), Ui.dp(a, 10), Ui.dp(a, 14), Ui.dp(a, 10));
        LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(-1, -2);
        fp.setMargins(Ui.dp(a, 16), 0, Ui.dp(a, 16), Ui.dp(a, 6));
        root.addView(field, fp);
        field.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int st, int c, int af) {
            }

            @Override
            public void onTextChanged(CharSequence s, int st, int b, int c) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                refresh();
            }
        });
        RecyclerView rv = new RecyclerView(a);
        rv.setLayoutManager(new LinearLayoutManager(a));
        rv.setClipToPadding(false);
        rv.setPadding(0, 0, 0, Ui.dp(a, 160));
        rv.setOverScrollMode(View.OVER_SCROLL_NEVER);
        adapter = new EventAdapter(a, true, R.string.search_empty, o -> a.showDetail(o));
        rv.setAdapter(adapter);
        root.addView(rv, new LinearLayout.LayoutParams(-1, 0, 1f));
    }

    @Override
    public View view() {
        return root;
    }

    @Override
    public void refresh() {
        String q = field.getText().toString().trim().toLowerCase(Locale.getDefault());
        List<Object> list = new ArrayList<>();
        if (q.isEmpty()) {
            adapter.setEmpty(R.string.search_empty);
        } else {
            adapter.setEmpty(R.string.no_events);
            long now = System.currentTimeMillis();
            for (Recur.Occ o : Recur.between(Store.all(a), now - 730 * Recur.DAY, now + 730 * Recur.DAY)) {
                String hay = (o.e.title + "\n" + o.e.location + "\n" + o.e.notes).toLowerCase(Locale.getDefault());
                if (hay.contains(q)) list.add(o);
                if (list.size() >= 150) break;
            }
        }
        adapter.set(list);
    }
}
