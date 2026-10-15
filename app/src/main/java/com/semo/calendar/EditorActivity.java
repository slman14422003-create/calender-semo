package com.semo.calendar;

import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.text.format.DateFormat;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.appcompat.widget.SwitchCompat;

import java.util.Calendar;

/**
 * New / edit event, laid out like the iOS "New Event" sheet: Cancel | title | Add, grouped cells, an iOS switch,
 * date and time chips that open inline wheel pickers, action sheets for Repeat and Alert.
 */
public final class EditorActivity extends BaseActivity {
    static final int[] REPEAT_LABEL = {R.string.rep_none, R.string.rep_daily, R.string.rep_weekly,
            R.string.rep_monthly, R.string.rep_yearly};
    private static final int NONE = 0, S_DATE = 1, S_TIME = 2, E_DATE = 3, E_TIME = 4;

    private Event ev;
    private boolean editing;
    private final Calendar start = Calendar.getInstance();
    private final Calendar end = Calendar.getInstance();
    private boolean allDay, syncing;
    private int repeat, remind, color, open = NONE;
    private EditText title, place, notes;
    private TextView sDate, sTime, eDate, eTime, repVal, remVal;
    private LinearLayout startPanel, endPanel;
    private DatePicker startDatePicker, endDatePicker;
    private TimePicker startTimePicker, endTimePicker;
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
        syncPickers();
        refreshChips();
        if (!editing && ev.title.isEmpty()) {
            title.postDelayed(() -> {
                title.requestFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) imm.showSoftInput(title, InputMethodManager.SHOW_IMPLICIT);
            }, 250);
        }
    }

    private TextView chip(View.OnClickListener l) {
        TextView t = Ui.text(this, "", 17, R.color.text_primary, false);
        t.setBackground(Ui.shape(Ui.color(this, R.color.seg_track), Ui.dp(this, 8)));
        t.setPadding(Ui.dp(this, 11), Ui.dp(this, 6), Ui.dp(this, 11), Ui.dp(this, 6));
        t.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.setMarginStart(Ui.dp(this, 8));
        t.setLayoutParams(lp);
        return t;
    }

    private EditText field(int hint, CharSequence text, boolean multi) {
        EditText e = new EditText(this);
        e.setBackground(null);
        e.setHint(hint);
        e.setText(text);
        e.setHintTextColor(Ui.color(this, R.color.text_hint));
        e.setTextColor(Ui.color(this, R.color.text_primary));
        e.setTextSize(17);
        e.setPadding(Ui.dp(this, 16), Ui.dp(this, 12), Ui.dp(this, 16), Ui.dp(this, 12));
        if (multi) {
            e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                    | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            e.setMinLines(5);
            e.setGravity(Gravity.TOP | Gravity.START);
        } else {
            e.setSingleLine(true);
            e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        }
        return e;
    }

    /** Inline wheel pickers (date and time) for one of the two date rows. */
    private LinearLayout panel(final Calendar cal, final boolean isStart) {
        LinearLayout p = Ui.vertical(this);
        p.setGravity(Gravity.CENTER_HORIZONTAL);
        p.setVisibility(View.GONE);
        ContextThemeWrapper w = new ContextThemeWrapper(this, R.style.Wheel);
        final DatePicker datePicker = new DatePicker(w);
        final TimePicker timePicker = new TimePicker(w);
        timePicker.setIs24HourView(DateFormat.is24HourFormat(this));
        datePicker.init(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH),
                (v, y, m, d) -> {
                    if (syncing) return;
                    long old = cal.getTimeInMillis();
                    cal.set(y, m, d);
                    afterChange(isStart, old);
                });
        timePicker.setOnTimeChangedListener((v, h, mi) -> {
            if (syncing) return;
            long old = cal.getTimeInMillis();
            cal.set(Calendar.HOUR_OF_DAY, h);
            cal.set(Calendar.MINUTE, mi);
            afterChange(isStart, old);
        });
        p.addView(datePicker, new LinearLayout.LayoutParams(-2, -2));
        p.addView(timePicker, new LinearLayout.LayoutParams(-2, -2));
        if (isStart) {
            startDatePicker = datePicker;
            startTimePicker = timePicker;
        } else {
            endDatePicker = datePicker;
            endTimePicker = timePicker;
        }
        return p;
    }

    private void build() {
        LinearLayout root = Ui.vertical(this);
        root.setBackgroundColor(Ui.color(this, R.color.bg_group));
        applyInsets(root);
        setContentView(root);

        // navigation bar: Cancel | title | Add
        LinearLayout nav = new LinearLayout(this);
        nav.setGravity(Gravity.CENTER_VERTICAL);
        nav.setPadding(Ui.dp(this, 4), Ui.dp(this, 4), Ui.dp(this, 4), Ui.dp(this, 4));
        TextView cancel = Ui.text(this, getString(R.string.cancel), 17, R.color.accent, false);
        cancel.setGravity(Gravity.CENTER_VERTICAL);
        cancel.setPadding(Ui.dp(this, 12), 0, Ui.dp(this, 12), 0);
        cancel.setOnClickListener(v -> finish());
        TextView head = Ui.text(this, getString(editing ? R.string.edit_event : R.string.new_event), 17,
                R.color.text_primary, false);
        head.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        head.setGravity(Gravity.CENTER);
        TextView save = Ui.text(this, getString(editing ? R.string.save : R.string.save), 17, R.color.accent, false);
        save.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        save.setGravity(Gravity.CENTER_VERTICAL);
        save.setPadding(Ui.dp(this, 12), 0, Ui.dp(this, 12), 0);
        save.setOnClickListener(v -> save());
        nav.addView(cancel, new LinearLayout.LayoutParams(-2, Ui.dp(this, 40)));
        nav.addView(head, new LinearLayout.LayoutParams(0, Ui.dp(this, 40), 1f));
        nav.addView(save, new LinearLayout.LayoutParams(-2, Ui.dp(this, 40)));
        root.addView(nav);

        ScrollView sv = new ScrollView(this);
        sv.setClipToPadding(false);
        sv.setPadding(0, 0, 0, Ui.dp(this, 32));
        LinearLayout body = Ui.vertical(this);
        sv.addView(body, new ScrollView.LayoutParams(-1, -2));
        root.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1f));

        LinearLayout g1 = Ui.group(this, 12);
        title = field(R.string.title_hint, ev.title, false);
        place = field(R.string.location, ev.location, false);
        g1.addView(title);
        g1.addView(Ui.hairline(this, 16));
        g1.addView(place);
        body.addView(g1);

        LinearLayout g2 = Ui.group(this, 24);
        LinearLayout allRow = Ui.cell(this, getString(R.string.all_day));
        SwitchCompat sw = new SwitchCompat(this);
        sw.setThumbDrawable(getDrawable(R.drawable.switch_thumb));
        sw.setTrackDrawable(getDrawable(R.drawable.switch_track));
        sw.setSplitTrack(false);
        sw.setChecked(allDay);
        sw.setOnCheckedChangeListener((v, on) -> {
            allDay = on;
            if (on && (open == S_TIME || open == E_TIME)) open = NONE;
            refreshChips();
        });
        allRow.addView(sw);
        g2.addView(allRow);
        g2.addView(Ui.hairline(this, 16));
        LinearLayout sRow = Ui.cell(this, getString(R.string.starts));
        sDate = chip(v -> toggle(S_DATE));
        sTime = chip(v -> toggle(S_TIME));
        sRow.addView(sDate);
        sRow.addView(sTime);
        g2.addView(sRow);
        startPanel = panel(start, true);
        g2.addView(startPanel);
        g2.addView(Ui.hairline(this, 16));
        LinearLayout eRow = Ui.cell(this, getString(R.string.ends));
        eDate = chip(v -> toggle(E_DATE));
        eTime = chip(v -> toggle(E_TIME));
        eRow.addView(eDate);
        eRow.addView(eTime);
        g2.addView(eRow);
        endPanel = panel(end, false);
        g2.addView(endPanel);
        body.addView(g2);

        LinearLayout g3 = Ui.group(this, 24);
        LinearLayout repRow = Ui.cell(this, getString(R.string.repeat));
        repVal = Ui.text(this, "", 17, R.color.text_secondary, false);
        repRow.addView(repVal);
        repRow.addView(Ui.chevron(this));
        repRow.setOnClickListener(v -> {
            CharSequence[] n = new CharSequence[REPEAT_LABEL.length];
            for (int i = 0; i < n.length; i++) n[i] = getString(REPEAT_LABEL[i]);
            Sheets.actionSheet(this, getString(R.string.repeat), n, repeat, w -> {
                repeat = w;
                refreshChips();
            });
        });
        Ui.press(repRow);
        g3.addView(repRow);
        g3.addView(Ui.hairline(this, 16));
        LinearLayout remRow = Ui.cell(this, getString(R.string.reminder));
        remVal = Ui.text(this, "", 17, R.color.text_secondary, false);
        remRow.addView(remVal);
        remRow.addView(Ui.chevron(this));
        remRow.setOnClickListener(v -> {
            CharSequence[] n = new CharSequence[Prefs.REMIND.length];
            for (int i = 0; i < n.length; i++) n[i] = getString(Prefs.REMIND_LABEL[i]);
            Sheets.actionSheet(this, getString(R.string.reminder), n, Prefs.remindIndex(remind), w -> {
                remind = Prefs.REMIND[w];
                refreshChips();
            });
        });
        Ui.press(remRow);
        g3.addView(remRow);
        body.addView(g3);

        LinearLayout g4 = Ui.group(this, 24);
        LinearLayout colRow = Ui.cell(this, getString(R.string.color));
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
        g4.addView(colRow);
        body.addView(g4);
        paintRings();

        LinearLayout g5 = Ui.group(this, 24);
        notes = field(R.string.notes, ev.notes, true);
        g5.addView(notes);
        body.addView(g5);

        if (editing) {
            LinearLayout g6 = Ui.group(this, 24);
            TextView del = Ui.text(this, getString(R.string.delete), 17, R.color.bad, false);
            del.setGravity(Gravity.CENTER);
            del.setPadding(0, Ui.dp(this, 13), 0, Ui.dp(this, 13));
            del.setOnClickListener(v -> Sheets.alert(this, getString(R.string.delete_q),
                    repeat > 0 ? getString(R.string.delete_series) : null, getString(R.string.delete), true, () -> {
                        Store.delete(this, ev.id);
                        Alarms.cancel(this, ev.id);
                        finish();
                    }));
            g6.addView(del);
            body.addView(g6);
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

    private void toggle(int which) {
        open = open == which ? NONE : which;
        syncPickers();
        refreshChips();
    }

    private void syncPickers() {
        syncing = true;
        startDatePicker.updateDate(start.get(Calendar.YEAR), start.get(Calendar.MONTH), start.get(Calendar.DAY_OF_MONTH));
        endDatePicker.updateDate(end.get(Calendar.YEAR), end.get(Calendar.MONTH), end.get(Calendar.DAY_OF_MONTH));
        startTimePicker.setHour(start.get(Calendar.HOUR_OF_DAY));
        startTimePicker.setMinute(start.get(Calendar.MINUTE));
        endTimePicker.setHour(end.get(Calendar.HOUR_OF_DAY));
        endTimePicker.setMinute(end.get(Calendar.MINUTE));
        syncing = false;
    }

    private void refreshChips() {
        sDate.setText(Fmt.date("EEEdMMM", start.getTimeInMillis()));
        eDate.setText(Fmt.date("EEEdMMM", end.getTimeInMillis()));
        sTime.setText(Fmt.time(this, start.getTimeInMillis()));
        eTime.setText(Fmt.time(this, end.getTimeInMillis()));
        sTime.setVisibility(allDay ? View.GONE : View.VISIBLE);
        eTime.setVisibility(allDay ? View.GONE : View.VISIBLE);
        sDate.setTextColor(Ui.color(this, open == S_DATE ? R.color.accent : R.color.text_primary));
        sTime.setTextColor(Ui.color(this, open == S_TIME ? R.color.accent : R.color.text_primary));
        eDate.setTextColor(Ui.color(this, open == E_DATE ? R.color.accent : R.color.text_primary));
        eTime.setTextColor(Ui.color(this, open == E_TIME ? R.color.accent : R.color.text_primary));
        startPanel.setVisibility(open == S_DATE || open == S_TIME ? View.VISIBLE : View.GONE);
        endPanel.setVisibility(open == E_DATE || open == E_TIME ? View.VISIBLE : View.GONE);
        startDatePicker.setVisibility(open == S_DATE ? View.VISIBLE : View.GONE);
        startTimePicker.setVisibility(open == S_TIME ? View.VISIBLE : View.GONE);
        endDatePicker.setVisibility(open == E_DATE ? View.VISIBLE : View.GONE);
        endTimePicker.setVisibility(open == E_TIME ? View.VISIBLE : View.GONE);
        repVal.setText(REPEAT_LABEL[repeat]);
        remVal.setText(Prefs.REMIND_LABEL[Prefs.remindIndex(remind)]);
    }

    /** Moving the start moves the end with it (same length); an end before the start is pulled up to it. */
    private void afterChange(boolean isStart, long old) {
        if (isStart) end.setTimeInMillis(end.getTimeInMillis() + (start.getTimeInMillis() - old));
        else if (end.getTimeInMillis() < start.getTimeInMillis()) end.setTimeInMillis(start.getTimeInMillis());
        syncPickers();
        refreshChips();
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
