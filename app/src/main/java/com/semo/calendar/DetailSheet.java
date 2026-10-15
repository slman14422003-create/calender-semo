package com.semo.calendar;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Event details as an iOS sheet: info card, then a card of actions (Edit, Duplicate, Share, Maps, Delete ...). */
final class DetailSheet {
    private DetailSheet() {
    }

    private static LinearLayout kv(MainActivity a, CharSequence label, CharSequence value) {
        LinearLayout r = Ui.cell(a, label);
        TextView v = Ui.text(a, value, 16, R.color.text_secondary, false);
        v.setMaxWidth(Ui.dp(a, 200));
        v.setGravity(Gravity.END);
        r.addView(v);
        return r;
    }

    private static LinearLayout stacked(MainActivity a, CharSequence text, int colorRes) {
        LinearLayout r = Ui.vertical(a);
        r.setPadding(Ui.dp(a, 16), Ui.dp(a, 12), Ui.dp(a, 16), Ui.dp(a, 12));
        r.addView(Ui.text(a, text, 16, colorRes, false));
        return r;
    }

    private static void action(MainActivity a, LinearLayout card, int labelRes, int colorRes, final Runnable run) {
        if (card.getChildCount() > 0) card.addView(Ui.hairline(a, 16));
        LinearLayout r = Ui.cell(a, a.getString(labelRes));
        ((TextView) r.getChildAt(0)).setTextColor(Ui.color(a, colorRes));
        r.setOnClickListener(v -> run.run());
        card.addView(r);
    }

    private static void line(MainActivity a, LinearLayout card, View row) {
        if (card.getChildCount() > 0) card.addView(Ui.hairline(a, 16));
        card.addView(row);
    }

    static void show(final MainActivity a, final Recur.Occ o) {
        final Event e = o.e;
        final Dialog d = new Dialog(a, R.style.Sheet);
        final String title = e.label(a.getString(R.string.untitled));

        LinearLayout box = Ui.vertical(a);
        box.setBackground(Ui.topRounded(Ui.color(a, R.color.sheet_bg), Ui.dp(a, 24)));
        View grab = new View(a);
        grab.setBackground(Ui.shape(Ui.color(a, R.color.stroke), Ui.dp(a, 3)));
        LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(Ui.dp(a, 36), Ui.dp(a, 5));
        gp.gravity = Gravity.CENTER_HORIZONTAL;
        gp.topMargin = Ui.dp(a, 10);
        gp.bottomMargin = Ui.dp(a, 6);
        box.addView(grab, gp);

        ScrollView sv = new ScrollView(a);
        sv.setVerticalScrollBarEnabled(false);
        LinearLayout body = Ui.vertical(a);
        body.setPadding(0, 0, 0, Ui.dp(a, 32));
        sv.addView(body, new ScrollView.LayoutParams(-1, -2));
        box.addView(sv, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout head = new LinearLayout(a);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(Ui.dp(a, 22), Ui.dp(a, 8), Ui.dp(a, 22), 0);
        View bar = new View(a);
        bar.setBackground(Ui.shape(Cals.color(a, e), Ui.dp(a, 2)));
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(Ui.dp(a, 5), Ui.dp(a, 30));
        bp.setMarginEnd(Ui.dp(a, 12));
        head.addView(bar, bp);
        TextView t = Ui.text(a, title, 22, R.color.text_primary, false);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        head.addView(t, new LinearLayout.LayoutParams(0, -2, 1f));
        body.addView(head);

        // info card
        LinearLayout info = Ui.group(a, 14, R.color.sheet_cell);
        String whenText = Fmt.date("EEEEdMMMM", o.start);
        String rel = Fmt.dayLabel(a, Recur.dayStart(o.start));
        if (rel != null) whenText = rel + " \u00b7 " + whenText;
        LinearLayout when = Ui.vertical(a);
        when.setPadding(Ui.dp(a, 16), Ui.dp(a, 12), Ui.dp(a, 16), Ui.dp(a, 12));
        when.addView(Ui.text(a, whenText, 16, R.color.text_primary, false));
        when.addView(Ui.text(a, Fmt.line(a, o, false), 15, R.color.text_secondary, false));
        info.addView(when);
        if (e.repeat > 0) {
            String rep = a.getString(EditorActivity.REPEAT_LABEL[e.repeat]);
            if (e.until > 0) rep += " \u00b7 " + a.getString(R.string.end_until_fmt, Fmt.date("yMMMd", e.until));
            else if (e.count > 0) rep += " \u00b7 " + a.getString(R.string.end_times_fmt, e.count);
            line(a, info, kv(a, a.getString(R.string.repeat), rep));
        }
        if (e.remind >= 0) {
            line(a, info, kv(a, a.getString(R.string.reminder),
                    a.getString(Prefs.REMIND_LABEL[Prefs.remindIndex(e.remind)])));
        }
        if (e.remind2 >= 0) {
            line(a, info, kv(a, a.getString(R.string.reminder),
                    a.getString(Prefs.REMIND_LABEL[Prefs.remindIndex(e.remind2)])));
        }
        if (e.travel > 0) {
            line(a, info, kv(a, a.getString(R.string.travel_time), a.getString(R.string.minutes_fmt, e.travel)));
        }
        line(a, info, kv(a, a.getString(R.string.calendar), Cals.name(a, Cals.get(a, e.cal))));
        if (e.free) line(a, info, kv(a, a.getString(R.string.availability), a.getString(R.string.free)));
        if (!e.location.isEmpty()) line(a, info, stacked(a, e.location, R.color.text_primary));
        if (!e.url.isEmpty()) line(a, info, stacked(a, e.url, R.color.accent));
        if (!e.notes.isEmpty()) line(a, info, stacked(a, e.notes, R.color.text_primary));
        body.addView(info);

        // actions card
        LinearLayout acts = Ui.group(a, 20, R.color.sheet_cell);
        if (!e.readOnly) {
            action(a, acts, R.string.edit, R.color.accent, () -> {
                d.dismiss();
                a.openEditor(e.id, 0);
            });
            action(a, acts, R.string.duplicate, R.color.accent, () -> {
                d.dismiss();
                a.duplicate(e);
            });
        }
        action(a, acts, R.string.share, R.color.accent, () -> {
            d.dismiss();
            Intent send = new Intent(Intent.ACTION_SEND).setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, Fmt.shareText(a, o));
            a.startActivity(Intent.createChooser(send, null));
        });
        if (!e.location.isEmpty()) {
            action(a, acts, R.string.open_maps, R.color.accent, () -> {
                d.dismiss();
                try {
                    a.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(e.location))));
                } catch (Exception ignored) {
                }
            });
        }
        if (!e.url.isEmpty()) {
            action(a, acts, R.string.open_link, R.color.accent, () -> {
                d.dismiss();
                try {
                    String u = e.url.contains("://") ? e.url : "https://" + e.url;
                    a.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(u)));
                } catch (Exception ignored) {
                }
            });
        }
        if (Semo.hasNotes(a)) {
            final String text = Fmt.shareText(a, o);
            action(a, acts, R.string.send_notes, R.color.accent, () -> {
                d.dismiss();
                Semo.sendToNotes(a, title, text);
            });
        }
        if (!e.readOnly) {
            action(a, acts, R.string.delete, R.color.bad, () -> {
                d.dismiss();
                a.confirmDelete(e);
            });
        }
        body.addView(acts);

        d.setContentView(box);
        Window w = d.getWindow();
        if (w != null) {
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            w.setGravity(Gravity.BOTTOM);
        }
        d.show();
    }
}
