package com.semo.calendar;

import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** The two iOS dialogs: centred alert (270 wide, hairline-separated buttons) and the bottom action sheet. */
final class Sheets {
    private Sheets() {
    }

    interface Pick {
        void on(int index);
    }

    private static View vline(Context c) {
        View v = new View(c);
        v.setBackgroundColor(Ui.color(c, R.color.stroke));
        v.setLayoutParams(new LinearLayout.LayoutParams(Math.max(1, Ui.dp(c, 0.5f)), -1));
        return v;
    }

    /** Alert with Cancel and one action; destructive actions are red, others bold blue. */
    static void alert(Context c, CharSequence title, CharSequence message, CharSequence okLabel,
                      boolean destructive, final Runnable ok) {
        final Dialog d = new Dialog(c, R.style.Sheet);
        LinearLayout box = Ui.vertical(c);
        box.setBackground(Ui.shape(Ui.color(c, R.color.alert_bg), Ui.dp(c, 14)));
        TextView t = Ui.text(c, title, 17, R.color.text_primary, false);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setGravity(Gravity.CENTER);
        t.setPadding(Ui.dp(c, 20), Ui.dp(c, 20), Ui.dp(c, 20), Ui.dp(c, message == null ? 20 : 4));
        box.addView(t);
        if (message != null) {
            TextView m = Ui.text(c, message, 13, R.color.text_primary, false);
            m.setGravity(Gravity.CENTER);
            m.setPadding(Ui.dp(c, 20), 0, Ui.dp(c, 20), Ui.dp(c, 20));
            box.addView(m);
        }
        box.addView(Ui.hairline(c, 0));
        LinearLayout row = new LinearLayout(c);
        TextView cancel = Ui.text(c, c.getString(R.string.cancel), 17, R.color.accent, false);
        cancel.setGravity(Gravity.CENTER);
        cancel.setOnClickListener(v -> d.dismiss());
        row.addView(cancel, new LinearLayout.LayoutParams(0, Ui.dp(c, 44), 1f));
        row.addView(vline(c));
        TextView go = Ui.text(c, okLabel, 17, destructive ? R.color.bad : R.color.accent, false);
        go.setTypeface(Typeface.DEFAULT_BOLD);
        go.setGravity(Gravity.CENTER);
        go.setOnClickListener(v -> {
            d.dismiss();
            ok.run();
        });
        row.addView(go, new LinearLayout.LayoutParams(0, Ui.dp(c, 44), 1f));
        box.addView(row);
        d.setContentView(box);
        Window w = d.getWindow();
        if (w != null) {
            w.setLayout(Ui.dp(c, 270), ViewGroup.LayoutParams.WRAP_CONTENT);
            w.setGravity(Gravity.CENTER);
            w.setWindowAnimations(android.R.style.Animation_Dialog);
        }
        d.show();
    }

    /** Bottom action sheet with a check on the current choice and a separate Cancel card. */
    static void actionSheet(Context c, CharSequence title, CharSequence[] items, int checked, final Pick pick) {
        final Dialog d = new Dialog(c, R.style.Sheet);
        LinearLayout root = Ui.vertical(c);
        root.setPadding(Ui.dp(c, 8), 0, Ui.dp(c, 8), Ui.dp(c, 28));
        LinearLayout card = Ui.vertical(c);
        card.setBackground(Ui.shape(Ui.color(c, R.color.alert_bg), Ui.dp(c, 14)));
        if (title != null) {
            TextView t = Ui.text(c, title, 13, R.color.text_secondary, true);
            t.setGravity(Gravity.CENTER);
            t.setPadding(Ui.dp(c, 16), Ui.dp(c, 14), Ui.dp(c, 16), Ui.dp(c, 14));
            card.addView(t);
        }
        for (int i = 0; i < items.length; i++) {
            final int idx = i;
            if (i > 0 || title != null) card.addView(Ui.hairline(c, 0));
            FrameLayout cell = new FrameLayout(c);
            TextView tv = Ui.text(c, items[i], 20, R.color.accent, false);
            tv.setGravity(Gravity.CENTER);
            cell.addView(tv, new FrameLayout.LayoutParams(-1, -1));
            if (i == checked) {
                ImageView ck = new ImageView(c);
                ck.setImageResource(R.drawable.ic_check);
                ck.setImageTintList(ColorStateList.valueOf(Ui.color(c, R.color.accent)));
                FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(Ui.dp(c, 22), Ui.dp(c, 22),
                        Gravity.START | Gravity.CENTER_VERTICAL);
                cp.setMarginStart(Ui.dp(c, 18));
                cell.addView(ck, cp);
            }
            cell.setOnClickListener(v -> {
                d.dismiss();
                pick.on(idx);
            });
            card.addView(cell, new LinearLayout.LayoutParams(-1, Ui.dp(c, 56)));
        }
        root.addView(card);
        TextView cancel = Ui.text(c, c.getString(R.string.cancel), 20, R.color.accent, false);
        cancel.setTypeface(Typeface.DEFAULT_BOLD);
        cancel.setGravity(Gravity.CENTER);
        cancel.setBackground(Ui.shape(Ui.color(c, R.color.alert_bg), Ui.dp(c, 14)));
        cancel.setOnClickListener(v -> d.dismiss());
        LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(-1, Ui.dp(c, 56));
        cl.topMargin = Ui.dp(c, 8);
        root.addView(cancel, cl);
        d.setContentView(root);
        Window w = d.getWindow();
        if (w != null) {
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            w.setGravity(Gravity.BOTTOM);
        }
        d.show();
    }
}
