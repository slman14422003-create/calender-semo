package com.semo.calendar;

import android.app.Dialog;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Event details as an iOS-style bottom sheet with Edit / Delete / Send to Notes. */
final class DetailSheet {
    private DetailSheet() {
    }

    private static TextView button(MainActivity a, int label, int fillRes, int textRes, View.OnClickListener l) {
        TextView t = Ui.text(a, a.getString(label), 16, textRes, true);
        t.setGravity(Gravity.CENTER);
        t.setBackground(Ui.shape(Ui.color(a, fillRes), Ui.dp(a, 12)));
        t.setOnClickListener(l);
        Ui.press(t);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, Ui.dp(a, 48), 1f);
        lp.setMarginStart(Ui.dp(a, 4));
        lp.setMarginEnd(Ui.dp(a, 4));
        t.setLayoutParams(lp);
        return t;
    }

    static void show(final MainActivity a, final Recur.Occ o) {
        final Event e = o.e;
        final Dialog d = new Dialog(a, R.style.Sheet);
        final String title = e.title.isEmpty() ? a.getString(R.string.untitled) : e.title;

        LinearLayout box = Ui.vertical(a);
        box.setBackground(Ui.topRounded(Ui.color(a, R.color.surface), Ui.dp(a, 24)));
        box.setPadding(Ui.dp(a, 22), Ui.dp(a, 10), Ui.dp(a, 22), Ui.dp(a, 30));

        View grab = new View(a);
        grab.setBackground(Ui.shape(Ui.color(a, R.color.stroke), Ui.dp(a, 3)));
        LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(Ui.dp(a, 36), Ui.dp(a, 5));
        gp.gravity = Gravity.CENTER_HORIZONTAL;
        gp.bottomMargin = Ui.dp(a, 16);
        box.addView(grab, gp);

        LinearLayout head = new LinearLayout(a);
        head.setGravity(Gravity.CENTER_VERTICAL);
        View bar = new View(a);
        bar.setBackground(Ui.shape(Ui.color(a, Event.COLORS[e.color]), Ui.dp(a, 2)));
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(Ui.dp(a, 5), Ui.dp(a, 30));
        bp.setMarginEnd(Ui.dp(a, 12));
        head.addView(bar, bp);
        TextView t = Ui.text(a, title, 22, R.color.text_primary, false);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        head.addView(t, new LinearLayout.LayoutParams(0, -2, 1f));
        box.addView(head);

        TextView when = Ui.text(a, Fmt.date("EEEEdMMMM", o.start), 16, R.color.text_primary, false);
        when.setPadding(0, Ui.dp(a, 12), 0, 0);
        box.addView(when);
        TextView time = Ui.text(a, Fmt.line(a, o, false), 15, R.color.text_secondary, false);
        time.setPadding(0, Ui.dp(a, 2), 0, 0);
        box.addView(time);
        if (e.repeat > 0 || e.remind >= 0) {
            StringBuilder sb = new StringBuilder();
            if (e.repeat > 0) sb.append(a.getString(EditorActivity.REPEAT_LABEL[e.repeat]));
            if (e.remind >= 0) {
                if (sb.length() > 0) sb.append(" · ");
                sb.append(a.getString(Prefs.REMIND_LABEL[Prefs.remindIndex(e.remind)]));
            }
            TextView extra = Ui.text(a, sb.toString(), 14, R.color.text_secondary, false);
            extra.setPadding(0, Ui.dp(a, 2), 0, 0);
            box.addView(extra);
        }
        if (!e.notes.isEmpty()) {
            TextView n = Ui.text(a, e.notes, 15, R.color.text_primary, false);
            n.setBackground(Ui.shape(Ui.color(a, R.color.field), Ui.dp(a, 12)));
            n.setPadding(Ui.dp(a, 14), Ui.dp(a, 12), Ui.dp(a, 14), Ui.dp(a, 12));
            LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(-1, -2);
            np.topMargin = Ui.dp(a, 14);
            box.addView(n, np);
        }

        LinearLayout actions = new LinearLayout(a);
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(-1, -2);
        ap.topMargin = Ui.dp(a, 20);
        actions.addView(button(a, R.string.edit, R.color.accent, R.color.on_accent, v -> {
            d.dismiss();
            a.openEditor(e.id, 0);
        }));
        if (Semo.hasNotes(a)) {
            final String text = title + "\n" + Fmt.date("EEEEdMMMM", o.start) + "\n" + Fmt.line(a, o, false)
                    + (e.notes.isEmpty() ? "" : "\n\n" + e.notes);
            actions.addView(button(a, R.string.send_notes, R.color.neutral_soft, R.color.accent, v -> {
                d.dismiss();
                Semo.sendToNotes(a, title, text);
            }));
        }
        actions.addView(button(a, R.string.delete, R.color.neutral_soft, R.color.bad, v -> {
            d.dismiss();
            a.confirmDelete(e);
        }));
        box.addView(actions, ap);

        d.setContentView(box);
        Window w = d.getWindow();
        if (w != null) {
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            w.setGravity(Gravity.BOTTOM);
        }
        d.show();
    }
}
