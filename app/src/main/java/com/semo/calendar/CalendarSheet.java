package com.semo.calendar;

import android.app.Dialog;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Add / edit one calendar: name, colour, and delete (own calendars only). */
final class CalendarSheet {
    private CalendarSheet() {
    }

    static void show(final BaseActivity a, final Cals.Cal existing, final Runnable changed) {
        final Cals.Cal cal = new Cals.Cal();
        if (existing != null) {
            cal.id = existing.id;
            cal.name = existing.name;
            cal.color = existing.color;
            cal.visible = existing.visible;
            cal.builtin = existing.builtin;
        } else {
            cal.id = Cals.newId();
            cal.color = 1;
        }
        final Dialog d = new Dialog(a, R.style.Sheet);
        LinearLayout box = Ui.vertical(a);
        box.setBackground(Ui.topRounded(Ui.color(a, R.color.sheet_bg), Ui.dp(a, 24)));
        box.setPadding(0, 0, 0, Ui.dp(a, 28));

        LinearLayout nav = new LinearLayout(a);
        nav.setGravity(Gravity.CENTER_VERTICAL);
        nav.setPadding(Ui.dp(a, 4), Ui.dp(a, 8), Ui.dp(a, 4), Ui.dp(a, 4));
        TextView cancel = Ui.text(a, a.getString(R.string.cancel), 17, R.color.accent, false);
        cancel.setPadding(Ui.dp(a, 12), Ui.dp(a, 10), Ui.dp(a, 12), Ui.dp(a, 10));
        cancel.setOnClickListener(v -> d.dismiss());
        TextView head = Ui.text(a, a.getString(existing != null ? R.string.calendar : R.string.add_calendar), 17,
                R.color.text_primary, false);
        head.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        head.setGravity(Gravity.CENTER);
        TextView save = Ui.text(a, a.getString(R.string.save), 17, R.color.accent, false);
        save.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        save.setPadding(Ui.dp(a, 12), Ui.dp(a, 10), Ui.dp(a, 12), Ui.dp(a, 10));
        nav.addView(cancel);
        nav.addView(head, new LinearLayout.LayoutParams(0, -2, 1f));
        nav.addView(save);
        box.addView(nav);

        LinearLayout g1 = Ui.group(a, 8, R.color.sheet_cell);
        final EditText name = new EditText(a);
        name.setBackground(null);
        name.setHint(R.string.calendar_name);
        name.setHintTextColor(Ui.color(a, R.color.text_hint));
        name.setTextColor(Ui.color(a, R.color.text_primary));
        name.setTextSize(17);
        name.setSingleLine(true);
        name.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        name.setPadding(Ui.dp(a, 16), Ui.dp(a, 12), Ui.dp(a, 16), Ui.dp(a, 12));
        name.setText(existing != null ? Cals.name(a, existing) : "");
        name.setEnabled(existing == null || !existing.builtin);
        g1.addView(name);
        box.addView(g1);

        LinearLayout g2 = Ui.group(a, 20, R.color.sheet_cell);
        LinearLayout swatches = new LinearLayout(a);
        swatches.setPadding(Ui.dp(a, 10), Ui.dp(a, 12), Ui.dp(a, 10), Ui.dp(a, 12));
        final View[] rings = new View[Event.COLORS.length];
        for (int i = 0; i < Event.COLORS.length; i++) {
            final int idx = i;
            FrameLayout ring = new FrameLayout(a);
            View dot = new View(a);
            GradientDrawable g = new GradientDrawable();
            g.setShape(GradientDrawable.OVAL);
            g.setColor(Ui.color(a, Event.COLORS[i]));
            dot.setBackground(g);
            ring.addView(dot, new FrameLayout.LayoutParams(Ui.dp(a, 24), Ui.dp(a, 24), Gravity.CENTER));
            ring.setOnClickListener(v -> {
                cal.color = idx;
                paint(a, rings, cal.color);
            });
            swatches.addView(ring, new LinearLayout.LayoutParams(0, Ui.dp(a, 34), 1f));
            rings[i] = ring;
        }
        paint(a, rings, cal.color);
        g2.addView(swatches);
        box.addView(g2);

        int own = 0;
        for (Cals.Cal x : Cals.all(a)) if (!x.builtin) own++;
        if (existing != null && !existing.builtin && own > 1) {
            LinearLayout g3 = Ui.group(a, 20, R.color.sheet_cell);
            TextView del = Ui.text(a, a.getString(R.string.delete), 17, R.color.bad, false);
            del.setGravity(Gravity.CENTER);
            del.setPadding(0, Ui.dp(a, 13), 0, Ui.dp(a, 13));
            del.setOnClickListener(v -> Sheets.alert(a, a.getString(R.string.delete_cal_q),
                    a.getString(R.string.delete_cal_msg), a.getString(R.string.delete), true, () -> {
                        Store.deleteByCal(a, existing.id);
                        Cals.delete(a, existing.id);
                        d.dismiss();
                        changed.run();
                    }));
            g3.addView(del);
            box.addView(g3);
        }

        save.setOnClickListener(v -> {
            String n = name.getText().toString().trim();
            if (existing == null) cal.name = n.isEmpty() ? a.getString(R.string.new_calendar_default) : n;
            else if (!existing.builtin) cal.name = n;
            Cals.save(a, cal);
            d.dismiss();
            changed.run();
        });

        d.setContentView(box);
        Window w = d.getWindow();
        if (w != null) {
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            w.setGravity(Gravity.BOTTOM);
            w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        d.show();
    }

    private static void paint(BaseActivity a, View[] rings, int selected) {
        for (int i = 0; i < rings.length; i++) {
            GradientDrawable r = new GradientDrawable();
            r.setShape(GradientDrawable.OVAL);
            r.setColor(0);
            if (i == selected) r.setStroke(Ui.dp(a, 2), Ui.color(a, Event.COLORS[i]));
            rings[i].setBackground(r);
        }
    }
}
