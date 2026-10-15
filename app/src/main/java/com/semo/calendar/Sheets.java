package com.semo.calendar;

import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.text.format.DateFormat;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.DatePicker;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;

import java.util.Calendar;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

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

    /** Bottom card with Cancel | title | Done above a picker view. */
    private static void pickerSheet(Context c, CharSequence title, View picker, final Runnable done) {
        final Dialog d = new Dialog(c, R.style.Sheet);
        LinearLayout root = Ui.vertical(c);
        root.setPadding(Ui.dp(c, 8), 0, Ui.dp(c, 8), Ui.dp(c, 28));
        LinearLayout card = Ui.vertical(c);
        card.setBackground(Ui.shape(Ui.color(c, R.color.alert_bg), Ui.dp(c, 14)));
        LinearLayout nav = new LinearLayout(c);
        nav.setGravity(Gravity.CENTER_VERTICAL);
        TextView cancel = Ui.text(c, c.getString(R.string.cancel), 17, R.color.accent, false);
        cancel.setPadding(Ui.dp(c, 16), Ui.dp(c, 14), Ui.dp(c, 16), Ui.dp(c, 14));
        cancel.setOnClickListener(v -> d.dismiss());
        TextView t = Ui.text(c, title, 17, R.color.text_primary, false);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setGravity(Gravity.CENTER);
        TextView ok = Ui.text(c, c.getString(R.string.done), 17, R.color.accent, false);
        ok.setTypeface(Typeface.DEFAULT_BOLD);
        ok.setPadding(Ui.dp(c, 16), Ui.dp(c, 14), Ui.dp(c, 16), Ui.dp(c, 14));
        ok.setOnClickListener(v -> {
            d.dismiss();
            done.run();
        });
        nav.addView(cancel);
        nav.addView(t, new LinearLayout.LayoutParams(0, -2, 1f));
        nav.addView(ok);
        card.addView(nav);
        card.addView(Ui.hairline(c, 0));
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(-2, -2);
        pp.gravity = Gravity.CENTER_HORIZONTAL;
        card.addView(picker, pp);
        root.addView(card);
        d.setContentView(root);
        Window w = d.getWindow();
        if (w != null) {
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            w.setGravity(Gravity.BOTTOM);
        }
        d.show();
    }

    /** Wheel date picker in a sheet; the callback gets the chosen day with the time of {@code millis} kept. */
    static void pickDate(Context c, final long millis, final LongConsumer cb) {
        final DatePicker dp = new DatePicker(new ContextThemeWrapper(c, R.style.Wheel));
        final Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millis);
        dp.init(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), null);
        pickerSheet(c, c.getString(R.string.go_to_date), dp, () -> {
            cal.set(dp.getYear(), dp.getMonth(), dp.getDayOfMonth());
            cb.accept(cal.getTimeInMillis());
        });
    }

    /** Wheel time picker in a sheet; the callback gets {@code millis} moved to the chosen time of day. */
    static void pickTime(Context c, final long millis, final LongConsumer cb) {
        final TimePicker tp = new TimePicker(new ContextThemeWrapper(c, R.style.Wheel));
        tp.setIs24HourView(DateFormat.is24HourFormat(c));
        final Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millis);
        tp.setHour(cal.get(Calendar.HOUR_OF_DAY));
        tp.setMinute(cal.get(Calendar.MINUTE));
        pickerSheet(c, c.getString(R.string.time_label), tp, () -> {
            cal.set(Calendar.HOUR_OF_DAY, tp.getHour());
            cal.set(Calendar.MINUTE, tp.getMinute());
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            cb.accept(cal.getTimeInMillis());
        });
    }

    /** Emoji grid (6 per row) with a "None" row on top; the callback gets "" for none. */
    static void emojiPicker(Context c, CharSequence title, final Consumer<String> cb) {
        final Dialog d = new Dialog(c, R.style.Sheet);
        LinearLayout root = Ui.vertical(c);
        root.setPadding(Ui.dp(c, 8), 0, Ui.dp(c, 8), Ui.dp(c, 28));
        LinearLayout card = Ui.vertical(c);
        card.setBackground(Ui.shape(Ui.color(c, R.color.alert_bg), Ui.dp(c, 14)));
        TextView t = Ui.text(c, title, 13, R.color.text_secondary, true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(Ui.dp(c, 16), Ui.dp(c, 14), Ui.dp(c, 16), Ui.dp(c, 8));
        card.addView(t);
        TextView none = Ui.text(c, c.getString(R.string.none_label), 17, R.color.accent, false);
        none.setGravity(Gravity.CENTER);
        none.setOnClickListener(v -> {
            d.dismiss();
            cb.accept("");
        });
        card.addView(none, new LinearLayout.LayoutParams(-1, Ui.dp(c, 44)));
        card.addView(Ui.hairline(c, 0));
        String[] list = c.getResources().getStringArray(R.array.emoji_list);
        LinearLayout row = null;
        for (int i = 0; i < list.length; i++) {
            if (i % 6 == 0) {
                row = new LinearLayout(c);
                card.addView(row, new LinearLayout.LayoutParams(-1, Ui.dp(c, 54)));
            }
            final String em = list[i];
            TextView cell = new TextView(c);
            cell.setText(em);
            cell.setTextSize(28);
            cell.setGravity(Gravity.CENTER);
            cell.setOnClickListener(v -> {
                d.dismiss();
                cb.accept(em);
            });
            row.addView(cell, new LinearLayout.LayoutParams(0, -1, 1f));
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
