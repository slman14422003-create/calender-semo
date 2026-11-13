package com.semo.calendar;

import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Shared frame of the full-screen modal pages (Updates, Privacy): grey grouped background, title bar, Done, scroll body. */
final class Pages {
    private Pages() {
    }

    /** Builds the page frame into {@code act} and returns the scrolling body to fill. */
    static LinearLayout frame(BaseActivity act, CharSequence title) {
        LinearLayout root = Ui.vertical(act);
        root.setBackgroundColor(Ui.color(act, R.color.bg_group));
        act.applyInsetsPublic(root);
        act.setContentView(root);

        LinearLayout nav = new LinearLayout(act);
        nav.setGravity(Gravity.CENTER_VERTICAL);
        nav.setPadding(Ui.dp(act, 4), Ui.dp(act, 6), Ui.dp(act, 4), Ui.dp(act, 4));
        nav.addView(new View(act), new LinearLayout.LayoutParams(Ui.dp(act, 72), 1));
        TextView head = Ui.text(act, title, 17, R.color.text_primary, false);
        head.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        head.setGravity(Gravity.CENTER);
        head.setSingleLine(true);
        nav.addView(head, new LinearLayout.LayoutParams(0, Ui.dp(act, 40), 1f));
        TextView done = Ui.text(act, act.getString(R.string.done), 17, R.color.accent, false);
        done.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        done.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        done.setPaddingRelative(0, 0, Ui.dp(act, 12), 0);
        done.setOnClickListener(v -> act.finish());
        nav.addView(done, new LinearLayout.LayoutParams(Ui.dp(act, 72), Ui.dp(act, 40)));
        root.addView(nav);

        ScrollView sv = new ScrollView(act);
        sv.setClipToPadding(false);
        sv.setPadding(0, 0, 0, Ui.dp(act, 32));
        sv.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout body = Ui.vertical(act);
        sv.addView(body, new ScrollView.LayoutParams(-1, -2));
        root.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1f));
        return body;
    }

    static TextView footnote(BaseActivity a, CharSequence t) {
        TextView v = Ui.text(a, t, 13, R.color.text_secondary, false);
        v.setPadding(Ui.dp(a, 32), Ui.dp(a, 8), Ui.dp(a, 32), 0);
        return v;
    }

    static void open(BaseActivity a, String url) {
        try {
            a.startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)));
        } catch (Exception ignored) {
        }
    }
}
