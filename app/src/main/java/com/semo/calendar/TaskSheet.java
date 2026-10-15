package com.semo.calendar;

import android.app.Dialog;
import android.graphics.Typeface;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Calendar;

/** New / edit task as an iOS sheet: title, notes, Date and Time switches with value chips, alert at due time. */
final class TaskSheet {
    private TaskSheet() {
    }

    private static final class St {
        boolean date, time, remind;
        final Calendar due = Calendar.getInstance();
    }

    private static TextView chip(MainActivity a, View.OnClickListener l) {
        TextView t = Ui.text(a, "", 17, R.color.text_primary, false);
        t.setBackground(Ui.shape(Ui.color(a, R.color.seg_track), Ui.dp(a, 8)));
        t.setPadding(Ui.dp(a, 11), Ui.dp(a, 6), Ui.dp(a, 11), Ui.dp(a, 6));
        t.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.setMarginEnd(Ui.dp(a, 10));
        t.setLayoutParams(lp);
        return t;
    }

    private static EditText field(MainActivity a, int hint, CharSequence text, boolean multi) {
        EditText e = new EditText(a);
        e.setBackground(null);
        e.setHint(hint);
        e.setText(text);
        e.setHintTextColor(Ui.color(a, R.color.text_hint));
        e.setTextColor(Ui.color(a, R.color.text_primary));
        e.setTextSize(17);
        e.setPadding(Ui.dp(a, 16), Ui.dp(a, 12), Ui.dp(a, 16), Ui.dp(a, 12));
        if (multi) {
            e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                    | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            e.setMinLines(3);
            e.setGravity(Gravity.TOP | Gravity.START);
        } else {
            e.setSingleLine(true);
            e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        }
        return e;
    }

    /** presetDay > 0 starts the task with that day as its date. */
    static void show(final MainActivity a, final Task existing, long presetDay) {
        final Task t = existing != null ? existing : new Task();
        final St st = new St();
        long now = System.currentTimeMillis();
        st.due.setTimeInMillis(now);
        st.due.set(Calendar.MINUTE, 0);
        st.due.set(Calendar.SECOND, 0);
        st.due.set(Calendar.MILLISECOND, 0);
        st.due.add(Calendar.HOUR_OF_DAY, 1);
        if (existing != null && existing.due > 0) {
            st.date = true;
            st.time = existing.hasTime;
            st.remind = existing.remind;
            st.due.setTimeInMillis(existing.due);
        } else if (presetDay > 0) {
            st.date = true;
            st.due.setTimeInMillis(presetDay);
            st.due.set(Calendar.HOUR_OF_DAY, 9);
        }

        final Dialog d = new Dialog(a, R.style.Sheet);
        LinearLayout box = Ui.vertical(a);
        box.setBackground(Ui.topRounded(Ui.color(a, R.color.sheet_bg), Ui.dp(a, 24)));

        LinearLayout nav = new LinearLayout(a);
        nav.setGravity(Gravity.CENTER_VERTICAL);
        nav.setPadding(Ui.dp(a, 4), Ui.dp(a, 8), Ui.dp(a, 4), Ui.dp(a, 4));
        TextView cancel = Ui.text(a, a.getString(R.string.cancel), 17, R.color.accent, false);
        cancel.setPadding(Ui.dp(a, 12), Ui.dp(a, 10), Ui.dp(a, 12), Ui.dp(a, 10));
        cancel.setOnClickListener(v -> d.dismiss());
        TextView head = Ui.text(a, a.getString(existing != null ? R.string.edit_task : R.string.new_task), 17,
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

        ScrollView sv = new ScrollView(a);
        LinearLayout body = Ui.vertical(a);
        body.setPadding(0, 0, 0, Ui.dp(a, 28));
        sv.addView(body, new ScrollView.LayoutParams(-1, -2));
        box.addView(sv, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout g1 = Ui.group(a, 8, R.color.sheet_cell);
        final EditText title = field(a, R.string.title_hint, t.title, false);
        final EditText notes = field(a, R.string.notes, t.notes, true);
        g1.addView(title);
        g1.addView(Ui.hairline(a, 16));
        g1.addView(notes);
        body.addView(g1);

        LinearLayout g2 = Ui.group(a, 20, R.color.sheet_cell);
        final TextView dateChip = chip(a, v -> {
        });
        final TextView timeChip = chip(a, v -> {
        });
        final LinearLayout dateRow = Ui.cell(a, a.getString(R.string.date_label));
        final LinearLayout timeRow = Ui.cell(a, a.getString(R.string.time_label));
        final LinearLayout remRow = Ui.cell(a, a.getString(R.string.reminder));
        final Runnable[] upd = new Runnable[1];
        final androidx.appcompat.widget.SwitchCompat dateSw = Ui.iosSwitch(a, st.date, (v, on) -> {
            st.date = on;
            if (!on) {
                st.time = false;
                st.remind = false;
            }
            upd[0].run();
        });
        final androidx.appcompat.widget.SwitchCompat timeSw = Ui.iosSwitch(a, st.time, (v, on) -> {
            st.time = on;
            if (on) st.date = true;
            if (!on) st.remind = false;
            upd[0].run();
        });
        final androidx.appcompat.widget.SwitchCompat remSw = Ui.iosSwitch(a, st.remind, (v, on) -> st.remind = on);
        dateChip.setOnClickListener(v -> Sheets.pickDate(a, st.due.getTimeInMillis(), ms -> {
            st.due.setTimeInMillis(ms);
            upd[0].run();
        }));
        timeChip.setOnClickListener(v -> Sheets.pickTime(a, st.due.getTimeInMillis(), ms -> {
            st.due.setTimeInMillis(ms);
            upd[0].run();
        }));
        dateRow.addView(dateChip);
        dateRow.addView(dateSw);
        timeRow.addView(timeChip);
        timeRow.addView(timeSw);
        remRow.addView(remSw);
        g2.addView(dateRow);
        g2.addView(Ui.hairline(a, 16));
        g2.addView(timeRow);
        g2.addView(Ui.hairline(a, 16));
        g2.addView(remRow);
        body.addView(g2);
        upd[0] = () -> {
            dateChip.setVisibility(st.date ? View.VISIBLE : View.GONE);
            timeChip.setVisibility(st.time ? View.VISIBLE : View.GONE);
            remRow.setVisibility(st.time ? View.VISIBLE : View.GONE);
            dateChip.setText(Fmt.date("EEEdMMM", st.due.getTimeInMillis()));
            timeChip.setText(Fmt.time(a, st.due.getTimeInMillis()));
            if (dateSw.isChecked() != st.date) dateSw.setChecked(st.date);
            if (timeSw.isChecked() != st.time) timeSw.setChecked(st.time);
            if (remSw.isChecked() != st.remind) remSw.setChecked(st.remind);
        };
        upd[0].run();

        if (existing != null) {
            LinearLayout g3 = Ui.group(a, 20, R.color.sheet_cell);
            TextView del = Ui.text(a, a.getString(R.string.delete), 17, R.color.bad, false);
            del.setGravity(Gravity.CENTER);
            del.setPadding(0, Ui.dp(a, 13), 0, Ui.dp(a, 13));
            del.setOnClickListener(v -> {
                d.dismiss();
                Tasks.delete(a, existing.id);
                Alarms.cancelTask(a, existing.id);
                a.refreshCurrent();
            });
            g3.addView(del);
            body.addView(g3);
        }

        save.setOnClickListener(v -> {
            t.title = title.getText().toString().trim();
            t.notes = notes.getText().toString().trim();
            if (st.date) {
                if (st.time) {
                    st.due.set(Calendar.SECOND, 0);
                    st.due.set(Calendar.MILLISECOND, 0);
                    t.due = st.due.getTimeInMillis();
                } else {
                    t.due = Recur.dayStart(st.due.getTimeInMillis());
                }
            } else {
                t.due = 0;
            }
            t.hasTime = st.date && st.time;
            t.remind = t.hasTime && st.remind;
            Tasks.save(a, t);
            Alarms.scheduleTask(a, t);
            d.dismiss();
            a.refreshCurrent();
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
}
