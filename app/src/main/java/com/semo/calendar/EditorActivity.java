package com.semo.calendar;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;

import java.util.Calendar;

/** New / edit event: iOS grouped form (title, dates, repeat, alert, colour, notes). */
public final class EditorActivity extends BaseActivity {
    static final int[] REPEAT_LABEL = {R.string.rep_none, R.string.rep_daily, R.string.rep_weekly,
            R.string.rep_monthly, R.string.rep_yearly};

    private Event ev;
    private boolean editing;
    private final Calendar start = Calendar.getInstance();
    private final Calendar end = Calendar.getInstance();
    private boolean allDay;
    private int repeat, remind, color;
    private EditText title, place, notes;
    private TextView sDate, sTime, eDate, eTime, repVal, remVal;
    private final View[] rings = new View[Event.COLORS.length];

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        String id = getIntent().getStringExtra("id");
        ev = id == null ? null : Store.get(this, id);
        editing = ev != null;
        if (!editing) {
            ev = new Event();
            long now = System.currentTimeMillis();
            long day = getIntent().getLongExtra("day", 0);
            long at = getIntent().getLongExtra("at", 0);
            if (at > 0) {
                start.setTimeInMillis(at);
            } else if (day <= 0 || Recur.dayStart(day) == Recur.dayStart(now)) {
                start.setTimeInMillis(now);
                start.set(Calendar.MINUTE, 0);
                start.set(Calendar.SECOND, 0);
                start.set(Calendar.MILLISECOND, 0);
                start.add(Calendar.HOUR_OF_DAY, 1);
            } else {
                start.setTimeInMillis(day);
                start.set(Calendar.HOUR_OF_DAY, 9);
            }
            end.setTimeInMillis(start.getTimeInMillis() + Prefs.duration(this) * 60000L);
            ev.remind = Prefs.defaultRemind(this);
            String t = getIntent().getStringExtra("title");
            String n = getIntent().getStringExtra("notes");
            if (t != null) ev.title = t;
            if (n != null) ev.notes = n;
        } else {
            start.setTimeInMillis(ev.start);
            end.setTimeInMillis(ev.end);
        }
        allDay = ev.allDay;
        repeat = ev.repeat;
        remind = ev.remind;
        color = ev.color;
        build();
        refreshChips();
    }

    private TextView chip(View.OnClickListener l) {
        TextView t = Ui.text(this, "", 16, R.color.text_primary, false);
        t.setBackground(Ui.shape(Ui.color(this, R.color.neutral_soft), Ui.dp(this, 8)));
        t.setPadding(Ui.dp(this, 10), Ui.dp(this, 6), Ui.dp(this, 10), Ui.dp(this, 6));
        t.setOnClickListener(l);
        Ui.press(t);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.setMarginStart(Ui.dp(this, 8));
        t.setLayoutParams(lp);
        return t;
    }

    private LinearLayout row(CharSequence label) {
        LinearLayout r = new LinearLayout(this);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setMinimumHeight(Ui.dp(this, 48));
        r.setPadding(Ui.dp(this, 16), Ui.dp(this, 6), Ui.dp(this, 16), Ui.dp(this, 6));
        r.addView(Ui.text(this, label, 17, R.color.text_primary, false), new LinearLayout.LayoutParams(0, -2, 1f));
        return r;
    }

    private EditText field(int hint, CharSequence text, boolean multi) {
        EditText e = new EditText(this);
        e.setBackground(null);
        e.setHint(hint);
        e.setText(text);
        e.setHintTextColor(Ui.color(this, R.color.text_hint));
        e.setTextColor(Ui.color(this, R.color.text_primary));
        e.setTextSize(17);
        e.setPadding(Ui.dp(this, 16), Ui.dp(this, 13), Ui.dp(this, 16), Ui.dp(this, 13));
        if (multi) {
            e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                    | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            e.setMinLines(4);
            e.setGravity(Gravity.TOP | Gravity.START);
        } else {
            e.setSingleLine(true);
            e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        }
        return e;
    }

    private void build() {
        LinearLayout root = Ui.vertical(this);
        root.setBackgroundColor(Ui.color(this, R.color.bg_group));
        applyInsets(root);
        setContentView(root);

        // navigation bar: Cancel | title | Save
        LinearLayout nav = new LinearLayout(this);
        nav.setGravity(Gravity.CENTER_VERTICAL);
        nav.setPadding(Ui.dp(this, 8), Ui.dp(this, 6), Ui.dp(this, 8), Ui.dp(this, 6));
        TextView cancel = Ui.text(this, getString(R.string.cancel), 17, R.color.accent, false);
        cancel.setPadding(Ui.dp(this, 12), Ui.dp(this, 10), Ui.dp(this, 12), Ui.dp(this, 10));
        cancel.setOnClickListener(v -> finish());
        TextView head = Ui.text(this, getString(editing ? R.string.edit_event : R.string.new_event), 17,
                R.color.text_primary, false);
        head.setTypeface(Typeface.DEFAULT_BOLD);
        head.setGravity(Gravity.CENTER);
        TextView save = Ui.text(this, getString(R.string.save), 17, R.color.accent, true);
        save.setTypeface(Typeface.DEFAULT_BOLD);
        save.setPadding(Ui.dp(this, 12), Ui.dp(this, 10), Ui.dp(this, 12), Ui.dp(this, 10));
        save.setOnClickListener(v -> save());
        nav.addView(cancel);
        nav.addView(head, new LinearLayout.LayoutParams(0, -2, 1f));
        nav.addView(save);
        root.addView(nav);

        ScrollView sv = new ScrollView(this);
        sv.setClipToPadding(false);
        sv.setPadding(0, 0, 0, Ui.dp(this, 24));
        LinearLayout body = Ui.vertical(this);
        sv.addView(body, new ScrollView.LayoutParams(-1, -2));
        root.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1f));

        LinearLayout g1 = Ui.group(this);
        title = field(R.string.title_hint, ev.title, false);
        place = field(R.string.location, ev.location, false);
        g1.addView(title);
        g1.addView(Ui.hairline(this, 16));
        g1.addView(place);
        body.addView(g1);

        LinearLayout g2 = Ui.group(this);
        LinearLayout allRow = row(getString(R.string.all_day));
        SwitchCompat sw = new SwitchCompat(this);
        sw.setChecked(allDay);
        sw.setOnCheckedChangeListener((v, on) -> {
            allDay = on;
            refreshChips();
        });
        allRow.addView(sw);
        g2.addView(allRow);
        g2.addView(Ui.hairline(this, 16));
        LinearLayout sRow = row(getString(R.string.starts));
        sDate = chip(v -> pickDate(start, true));
        sTime = chip(v -> pickTime(start, true));
        sRow.addView(sDate);
        sRow.addView(sTime);
        g2.addView(sRow);
        g2.addView(Ui.hairline(this, 16));
        LinearLayout eRow = row(getString(R.string.ends));
        eDate = chip(v -> pickDate(end, false));
        eTime = chip(v -> pickTime(end, false));
        eRow.addView(eDate);
        eRow.addView(eTime);
        g2.addView(eRow);
        body.addView(g2);

        LinearLayout g3 = Ui.group(this);
        LinearLayout repRow = row(getString(R.string.repeat));
        repVal = Ui.text(this, "", 17, R.color.text_secondary, false);
        repRow.addView(repVal);
        repRow.setOnClickListener(v -> {
            CharSequence[] n = new CharSequence[REPEAT_LABEL.length];
            for (int i = 0; i < n.length; i++) n[i] = getString(REPEAT_LABEL[i]);
            choose(R.string.repeat, n, repeat, w -> repeat = w);
        });
        g3.addView(repRow);
        g3.addView(Ui.hairline(this, 16));
        LinearLayout remRow = row(getString(R.string.reminder));
        remVal = Ui.text(this, "", 17, R.color.text_secondary, false);
        remRow.addView(remVal);
        remRow.setOnClickListener(v -> {
            CharSequence[] n = new CharSequence[Prefs.REMIND.length];
            for (int i = 0; i < n.length; i++) n[i] = getString(Prefs.REMIND_LABEL[i]);
            choose(R.string.reminder, n, Prefs.remindIndex(remind), w -> remind = Prefs.REMIND[w]);
        });
        g3.addView(remRow);
        g3.addView(Ui.hairline(this, 16));
        LinearLayout colRow = row(getString(R.string.color));
        for (int i = 0; i < Event.COLORS.length; i++) {
            final int idx = i;
            FrameLayout ring = new FrameLayout(this);
            View dot = new View(this);
            GradientDrawable d = new GradientDrawable();
            d.setShape(GradientDrawable.OVAL);
            d.setColor(Ui.color(this, Event.COLORS[i]));
            dot.setBackground(d);
            ring.addView(dot, new FrameLayout.LayoutParams(Ui.dp(this, 22), Ui.dp(this, 22), Gravity.CENTER));
            ring.setOnClickListener(v -> {
                color = idx;
                paintRings();
            });
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(Ui.dp(this, 32), Ui.dp(this, 32));
            rp.setMarginStart(Ui.dp(this, 4));
            colRow.addView(ring, rp);
            rings[i] = ring;
        }
        g3.addView(colRow);
        body.addView(g3);
        paintRings();

        LinearLayout g4 = Ui.group(this);
        notes = field(R.string.notes, ev.notes, true);
        g4.addView(notes);
        body.addView(g4);

        if (editing) {
            LinearLayout g5 = Ui.group(this);
            TextView del = Ui.text(this, getString(R.string.delete), 17, R.color.bad, false);
            del.setGravity(Gravity.CENTER);
            del.setPadding(0, Ui.dp(this, 14), 0, Ui.dp(this, 14));
            del.setOnClickListener(v -> new AlertDialog.Builder(this).setTitle(R.string.delete_q)
                    .setMessage(repeat > 0 ? getString(R.string.delete_series) : null)
                    .setPositiveButton(R.string.delete, (d, w) -> {
                        Store.delete(this, ev.id);
                        Alarms.cancel(this, ev.id);
                        finish();
                    })
                    .setNegativeButton(R.string.cancel, null).show());
            g5.addView(del);
            body.addView(g5);
            if (repeat > 0) {
                TextView n = Ui.text(this, getString(R.string.repeats_note), 13, R.color.text_secondary, false);
                n.setPadding(Ui.dp(this, 32), Ui.dp(this, 8), Ui.dp(this, 32), 0);
                body.addView(n);
            }
        }
    }

    private void paintRings() {
        for (int i = 0; i < rings.length; i++) {
            GradientDrawable r = new GradientDrawable();
            r.setShape(GradientDrawable.OVAL);
            r.setColor(0);
            if (i == color) r.setStroke(Ui.dp(this, 2), Ui.color(this, Event.COLORS[i]));
            rings[i].setBackground(r);
        }
    }

    private interface Pick {
        void on(int index);
    }

    private void choose(int titleRes, CharSequence[] items, int checked, Pick pick) {
        new AlertDialog.Builder(this).setTitle(titleRes)
                .setSingleChoiceItems(items, checked, (d, w) -> {
                    pick.on(w);
                    d.dismiss();
                    refreshChips();
                })
                .setNegativeButton(R.string.cancel, null).show();
    }

    private void refreshChips() {
        sDate.setText(Fmt.date("EEEdMMM", start.getTimeInMillis()));
        eDate.setText(Fmt.date("EEEdMMM", end.getTimeInMillis()));
        sTime.setText(Fmt.time(this, start.getTimeInMillis()));
        eTime.setText(Fmt.time(this, end.getTimeInMillis()));
        sTime.setVisibility(allDay ? View.GONE : View.VISIBLE);
        eTime.setVisibility(allDay ? View.GONE : View.VISIBLE);
        repVal.setText(REPEAT_LABEL[repeat]);
        remVal.setText(Prefs.REMIND_LABEL[Prefs.remindIndex(remind)]);
    }

    /** Moving the start moves the end with it, so the duration stays the same. */
    private void afterChange(Calendar cal, boolean isStart, long old) {
        if (isStart) end.setTimeInMillis(end.getTimeInMillis() + (start.getTimeInMillis() - old));
        else if (end.getTimeInMillis() < start.getTimeInMillis()) end.setTimeInMillis(start.getTimeInMillis());
        refreshChips();
    }

    private void pickDate(final Calendar cal, final boolean isStart) {
        new DatePickerDialog(this, (v, y, m, d) -> {
            long old = cal.getTimeInMillis();
            cal.set(y, m, d);
            afterChange(cal, isStart, old);
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void pickTime(final Calendar cal, final boolean isStart) {
        new TimePickerDialog(this, (v, h, mi) -> {
            long old = cal.getTimeInMillis();
            cal.set(Calendar.HOUR_OF_DAY, h);
            cal.set(Calendar.MINUTE, mi);
            afterChange(cal, isStart, old);
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), DateFormat.is24HourFormat(this)).show();
    }

    private void save() {
        ev.title = title.getText().toString().trim();
        ev.location = place.getText().toString().trim();
        ev.notes = notes.getText().toString().trim();
        ev.allDay = allDay;
        ev.repeat = repeat;
        ev.remind = remind;
        ev.color = color;
        if (allDay) {
            ev.start = Recur.dayStart(start.getTimeInMillis());
            long e = Math.max(Recur.dayStart(end.getTimeInMillis()), ev.start);
            ev.end = Recur.nextDay(e) - 1;
        } else {
            ev.start = start.getTimeInMillis();
            ev.end = Math.max(end.getTimeInMillis(), ev.start);
        }
        Store.save(this, ev);
        Alarms.schedule(getApplicationContext(), ev);
        finish();
    }
}
