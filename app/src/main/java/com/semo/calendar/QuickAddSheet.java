package com.semo.calendar;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Calendar;

/**
 * Smart add: one sentence in, a ready event or task out. The text is understood on the device by NlParser; the card
 * under the field shows exactly what will be created, and Add saves it right away.
 */
final class QuickAddSheet {
    private QuickAddSheet() {
    }

    private static long nextHour() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        c.add(Calendar.HOUR_OF_DAY, 1);
        return c.getTimeInMillis();
    }

    private static int nearestRemind(int minutes) {
        int best = 0;
        int diff = Integer.MAX_VALUE;
        for (int r : Prefs.REMIND) {
            if (r < 0) continue;
            int d = Math.abs(r - minutes);
            if (d < diff) {
                diff = d;
                best = r;
            }
        }
        return best;
    }

    private static String whenText(MainActivity a, NlParser.Draft d, long start, long end, boolean allDay) {
        String day = Fmt.dayLabel(a, Recur.dayStart(start));
        StringBuilder sb = new StringBuilder(day != null ? day + ", " : "");
        sb.append(Fmt.date("EEEdMMM", start));
        if (allDay) {
            sb.append(" \u00b7 ").append(a.getString(R.string.all_day));
        } else {
            sb.append(" \u00b7 ").append(Fmt.time(a, start));
            if (end > start) sb.append(" \u2013 ").append(Fmt.time(a, end));
        }
        return sb.toString();
    }

    private static LinearLayout row(MainActivity a, CharSequence label, CharSequence value) {
        LinearLayout r = Ui.cell(a, label);
        TextView v = Ui.text(a, value, 16, R.color.text_secondary, false);
        v.setMaxWidth(Ui.dp(a, 230));
        v.setGravity(Gravity.END);
        r.addView(v);
        return r;
    }

    static void show(final MainActivity a) {
        final Dialog dlg = new SheetDialog(a);
        LinearLayout box = Ui.vertical(a);
        box.setBackground(Ui.topRounded(Ui.color(a, R.color.sheet_bg), Ui.dp(a, 28)));

        LinearLayout nav = new LinearLayout(a);
        nav.setGravity(Gravity.CENTER_VERTICAL);
        nav.setPadding(Ui.dp(a, 4), Ui.dp(a, 8), Ui.dp(a, 4), Ui.dp(a, 4));
        TextView cancel = Ui.text(a, a.getString(R.string.cancel), 17, R.color.accent, false);
        cancel.setPadding(Ui.dp(a, 12), Ui.dp(a, 10), Ui.dp(a, 12), Ui.dp(a, 10));
        cancel.setOnClickListener(v -> dlg.dismiss());
        TextView head = Ui.text(a, a.getString(R.string.smart_add), 17, R.color.text_primary, false);
        head.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        head.setGravity(Gravity.CENTER);
        final TextView add = Ui.text(a, a.getString(R.string.add), 17, R.color.accent, false);
        add.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        add.setPadding(Ui.dp(a, 12), Ui.dp(a, 10), Ui.dp(a, 12), Ui.dp(a, 10));
        nav.addView(cancel);
        nav.addView(head, new LinearLayout.LayoutParams(0, -2, 1f));
        nav.addView(add);
        box.addView(nav);

        ScrollView sv = new ScrollView(a);
        sv.setVerticalScrollBarEnabled(false);
        LinearLayout body = Ui.vertical(a);
        body.setPadding(0, 0, 0, Ui.dp(a, 24));
        sv.addView(body, new ScrollView.LayoutParams(-1, -2));
        box.addView(sv, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout g1 = Ui.group(a, 8, R.color.sheet_cell);
        final EditText input = new EditText(a);
        input.setBackground(null);
        input.setHint(R.string.smart_hint);
        input.setHintTextColor(Ui.color(a, R.color.text_hint));
        input.setTextColor(Ui.color(a, R.color.text_primary));
        input.setTextSize(18);
        input.setMinLines(2);
        input.setGravity(Gravity.TOP | Gravity.START);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setPadding(Ui.dp(a, 16), Ui.dp(a, 14), Ui.dp(a, 16), Ui.dp(a, 14));
        g1.addView(input);
        body.addView(g1);

        // Event | Task, chosen from the sentence ("remind me ...") but changeable
        final TextView[] kinds = new TextView[2];
        LinearLayout seg = new LinearLayout(a);
        seg.setBackground(Ui.shape(Ui.color(a, R.color.seg_track), Ui.dp(a, 9)));
        seg.setPadding(Ui.dp(a, 2), Ui.dp(a, 2), Ui.dp(a, 2), Ui.dp(a, 2));
        final int[] kind = {0};
        final boolean[] forced = {false};
        final Runnable[] refresh = new Runnable[1];
        int[] kl = {R.string.kind_event, R.string.kind_task};
        for (int i = 0; i < 2; i++) {
            final int idx = i;
            TextView k = Ui.text(a, a.getString(kl[i]), 13, R.color.text_primary, false);
            k.setGravity(Gravity.CENTER);
            k.setOnClickListener(v -> {
                kind[0] = idx;
                forced[0] = true;
                refresh[0].run();
            });
            seg.addView(k, new LinearLayout.LayoutParams(0, Ui.dp(a, 28), 1f));
            kinds[i] = k;
        }
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2);
        sp.setMargins(Ui.dp(a, 16), Ui.dp(a, 14), Ui.dp(a, 16), 0);
        body.addView(seg, sp);

        final LinearLayout preview = Ui.group(a, 14, R.color.sheet_cell);
        Motion.animateChanges(preview);
        body.addView(preview);
        TextView local = Ui.text(a, a.getString(R.string.smart_local), 12, R.color.text_secondary, false);
        local.setGravity(Gravity.CENTER);
        local.setPadding(Ui.dp(a, 32), Ui.dp(a, 12), Ui.dp(a, 32), 0);
        body.addView(local);

        final NlParser.Draft[] draft = {new NlParser.Draft()};
        refresh[0] = () -> {
            String text = input.getText().toString().trim();
            draft[0] = text.isEmpty() ? new NlParser.Draft() : NlParser.parse(a, text, System.currentTimeMillis());
            NlParser.Draft d = draft[0];
            if (!forced[0]) kind[0] = d.task ? 1 : 0;
            for (int i = 0; i < 2; i++) {
                boolean on = kind[0] == i;
                kinds[i].setTypeface(on ? Typeface.create("sans-serif-medium", Typeface.BOLD) : Typeface.DEFAULT);
                kinds[i].setBackground(on ? Ui.shape(Ui.color(a, R.color.seg_thumb), Ui.dp(a, 7)) : null);
            }
            add.setAlpha(text.isEmpty() ? 0.4f : 1f);
            preview.removeAllViews();
            if (text.isEmpty()) {
                TextView hint = Ui.text(a, a.getString(R.string.smart_empty), 15, R.color.text_secondary, false);
                hint.setPadding(Ui.dp(a, 16), Ui.dp(a, 14), Ui.dp(a, 16), Ui.dp(a, 14));
                preview.addView(hint);
                return;
            }
            String title = d.title.isEmpty() ? a.getString(R.string.untitled) : d.title;
            if (Prefs.smart(a)) {
                String em = Smart.emoji(a, d.title);
                if (!em.isEmpty() && kind[0] == 0) title = em + " " + title;
            }
            preview.addView(row(a, a.getString(R.string.title_hint), title));
            long start = d.start != 0 ? d.start : nextHour();
            if (kind[0] == 0) {
                long end = d.end != 0 ? d.end
                        : start + (d.durationMin > 0 ? d.durationMin : Prefs.duration(a)) * 60000L;
                preview.addView(Ui.hairline(a, 16));
                preview.addView(row(a, a.getString(R.string.date_label), whenText(a, d, start, end, d.allDay)));
            } else if (d.start != 0) {
                preview.addView(Ui.hairline(a, 16));
                preview.addView(row(a, a.getString(R.string.date_label),
                        whenText(a, d, start, 0, !d.hasTime)));
            }
            if (d.repeat > 0) {
                preview.addView(Ui.hairline(a, 16));
                preview.addView(row(a, a.getString(R.string.repeat),
                        a.getString(EditorActivity.REPEAT_LABEL[d.repeat])));
            }
            if (d.remind >= 0 && kind[0] == 0) {
                preview.addView(Ui.hairline(a, 16));
                preview.addView(row(a, a.getString(R.string.reminder),
                        a.getString(Prefs.REMIND_LABEL[Prefs.remindIndex(nearestRemind(d.remind))])));
            }
            if (!d.location.isEmpty() && kind[0] == 0) {
                preview.addView(Ui.hairline(a, 16));
                preview.addView(row(a, a.getString(R.string.location), d.location));
            }
            if (kind[0] == 0 && !d.allDay) {
                long ce = d.end != 0 ? d.end : start + (d.durationMin > 0 ? d.durationMin : Prefs.duration(a)) * 60000L;
                java.util.List<Recur.Occ> clash = Smart.conflicts(a, start, ce, "");
                if (!clash.isEmpty()) {
                    TextView warn = Ui.text(a, a.getString(R.string.conflict_fmt,
                            clash.get(0).e.label(a.getString(R.string.untitled))), 13, R.color.warn, false);
                    warn.setPadding(Ui.dp(a, 16), Ui.dp(a, 8), Ui.dp(a, 16), Ui.dp(a, 10));
                    preview.addView(Ui.hairline(a, 16));
                    preview.addView(warn);
                }
            }
            preview.addView(Ui.hairline(a, 16));
            LinearLayout more = Ui.cell(a, a.getString(R.string.more_details));
            ((TextView) more.getChildAt(0)).setTextColor(Ui.color(a, R.color.accent));
            more.addView(Ui.chevron(a));
            more.setOnClickListener(v -> {
                dlg.dismiss();
                openDetails(a, draft[0], kind[0] == 1);
            });
            Ui.highlight(more);
            preview.addView(more);
        };
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int st, int c, int af) {
            }

            @Override
            public void onTextChanged(CharSequence s, int st, int b, int c) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                refresh[0].run();
            }
        });
        add.setOnClickListener(v -> {
            if (input.getText().toString().trim().isEmpty()) return;
            commit(a, draft[0], kind[0] == 1);
            dlg.dismiss();
        });
        refresh[0].run();

        dlg.setContentView(box);
        Window w = dlg.getWindow();
        if (w != null) {
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            w.setGravity(Gravity.BOTTOM);
            w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        dlg.show();
        input.postDelayed(() -> {
            input.requestFocus();
            InputMethodManager imm = (InputMethodManager) a.getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
        }, 250);
    }

    private static void commit(MainActivity a, NlParser.Draft d, boolean asTask) {
        String title = d.title.isEmpty() ? a.getString(R.string.untitled) : d.title;
        if (asTask) {
            Task t = new Task();
            t.title = title;
            if (d.start != 0 && d.hasDate) {
                t.due = d.hasTime ? d.start : Recur.dayStart(d.start);
                t.hasTime = d.hasTime;
            }
            t.remind = t.hasTime;
            Tasks.save(a, t);
            Alarms.scheduleTask(a.getApplicationContext(), t);
        } else {
            Event e = new Event();
            e.title = d.title;
            e.location = d.location;
            e.cal = Prefs.defaultCal(a);
            e.repeat = d.repeat;
            e.allDay = d.allDay;
            long start = d.start != 0 ? d.start : nextHour();
            if (e.allDay) {
                e.start = Recur.dayStart(start);
                e.end = Recur.nextDay(e.start) - 1;
            } else {
                e.start = start;
                e.end = d.end != 0 ? d.end : start + (d.durationMin > 0 ? d.durationMin : Prefs.duration(a)) * 60000L;
            }
            e.remind = d.remind >= 0 ? nearestRemind(d.remind) : Prefs.defaultRemind(a);
            if (Prefs.smart(a)) e.emoji = Smart.emoji(a, d.title);
            Store.save(a, e);
            Alarms.schedule(a.getApplicationContext(), e);
        }
        a.refreshCurrent();
    }

    private static void openDetails(MainActivity a, NlParser.Draft d, boolean asTask) {
        if (asTask) {
            Task t = new Task();
            t.title = d.title;
            if (d.start != 0 && d.hasDate) {
                t.due = d.hasTime ? d.start : Recur.dayStart(d.start);
                t.hasTime = d.hasTime;
                t.remind = d.hasTime;
            }
            TaskSheet.show(a, t, 0);
            return;
        }
        Intent in = new Intent(a, EditorActivity.class).putExtra("title", d.title);
        if (d.start != 0) in.putExtra("at", d.start);
        if (d.end != 0) in.putExtra("end", d.end);
        else if (d.durationMin > 0 && d.start != 0) in.putExtra("end", d.start + d.durationMin * 60000L);
        in.putExtra("allday", d.allDay).putExtra("repeat", d.repeat);
        if (d.remind >= 0) in.putExtra("remind", nearestRemind(d.remind));
        if (!d.location.isEmpty()) in.putExtra("loc", d.location);
        a.startActivity(in);
    }
}
