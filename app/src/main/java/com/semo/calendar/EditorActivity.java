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

import java.util.Calendar;
import java.util.List;

/**
 * New / edit event, laid out like the iOS "New Event" sheet and extended with the Samsung extras: calendar,
 * repeat with end, two alerts, travel time, availability, URL, emoji and colour.
 */
public final class EditorActivity extends BaseActivity {
    /** Indexed by the repeat code of {@link Event}. */
    static final int[] REPEAT_LABEL = {R.string.rep_none, R.string.rep_daily, R.string.rep_weekly,
            R.string.rep_monthly, R.string.rep_yearly, R.string.rep_biweekly, R.string.rep_weekdays};
    private static final int[] REPEAT_ORDER = {0, 1, 2, 5, 6, 3, 4};
    private static final int[] TRAVEL = {0, 5, 15, 30, 45, 60, 90, 120};
    private static final int[] TIMES = {2, 3, 4, 5, 6, 7, 8, 10, 12, 15, 20, 30, 52};
    private static final int NONE = 0, S_DATE = 1, S_TIME = 2, E_DATE = 3, E_TIME = 4;

    private Event ev;
    private boolean editing;
    private final Calendar start = Calendar.getInstance();
    private final Calendar end = Calendar.getInstance();
    private boolean allDay, syncing, free;
    private int repeat, count, remind, remind2, travel, color, open = NONE;
    private long until;
    private String cal = Cals.PERSONAL, emoji = "";
    private EditText title, place, urlField, notes;
    private TextView sDate, sTime, eDate, eTime, repVal, endVal, remVal, rem2Val, travelVal, calVal, availVal, emojiVal;
    private View endRow, rem2Row, calDot;
    private LinearLayout startPanel, endPanel;
    private DatePicker startDatePicker, endDatePicker;
    private TimePicker startTimePicker, endTimePicker;
    private final View[] rings = new View[Event.COLORS.length + 1];

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
            ev.cal = Prefs.defaultCal(this);
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
        count = ev.count;
        until = ev.until;
        remind = ev.remind;
        remind2 = ev.remind2;
        travel = ev.travel;
        color = ev.color;
        free = ev.free;
        cal = ev.cal;
        emoji = ev.emoji;
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

    private EditText field(int hint, CharSequence text, boolean multi, int inputType) {
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
            e.setInputType(inputType);
        }
        return e;
    }

    /** A cell with a value on the trailing side and a chevron; tapping it runs {@code run}. */
    private LinearLayout choiceRow(CharSequence label, TextView value, Runnable run) {
        LinearLayout r = Ui.cell(this, label);
        r.addView(value);
        r.addView(Ui.chevron(this));
        r.setOnClickListener(v -> run.run());
        Ui.highlight(r);
        return r;
    }

    private TextView value() {
        TextView v = Ui.text(this, "", 17, R.color.text_secondary, false);
        v.setSingleLine(true);
        v.setMaxWidth(Ui.dp(this, 190));
        v.setGravity(Gravity.END);
        return v;
    }

    /** Inline wheel pickers (date and time) for one of the two date rows. */
    private LinearLayout panel(final Calendar c, final boolean isStart) {
        LinearLayout p = Ui.vertical(this);
        p.setGravity(Gravity.CENTER_HORIZONTAL);
        p.setVisibility(View.GONE);
        ContextThemeWrapper w = new ContextThemeWrapper(this, R.style.Wheel);
        final DatePicker datePicker = new DatePicker(w);
        final TimePicker timePicker = new TimePicker(w);
        timePicker.setIs24HourView(DateFormat.is24HourFormat(this));
        datePicker.init(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH),
                (v, y, m, d) -> {
                    if (syncing) return;
                    long old = c.getTimeInMillis();
                    c.set(y, m, d);
                    afterChange(isStart, old);
                });
        timePicker.setOnTimeChangedListener((v, h, mi) -> {
            if (syncing) return;
            long old = c.getTimeInMillis();
            c.set(Calendar.HOUR_OF_DAY, h);
            c.set(Calendar.MINUTE, mi);
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

        // navigation bar: Cancel | title | Save
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
        TextView save = Ui.text(this, getString(R.string.save), 17, R.color.accent, false);
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

        // title, place, link
        LinearLayout g1 = Ui.group(this, 12);
        title = field(R.string.title_hint, ev.title, false, InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        place = field(R.string.location, ev.location, false, InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        urlField = field(R.string.url_hint, ev.url, false, InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_URI);
        g1.addView(title);
        g1.addView(Ui.hairline(this, 16));
        g1.addView(place);
        g1.addView(Ui.hairline(this, 16));
        g1.addView(urlField);
        body.addView(g1);

        // dates
        LinearLayout g2 = Ui.group(this, 24);
        LinearLayout allRow = Ui.cell(this, getString(R.string.all_day));
        allRow.addView(Ui.iosSwitch(this, allDay, (v, on) -> {
            allDay = on;
            if (on && (open == S_TIME || open == E_TIME)) open = NONE;
            refreshChips();
        }));
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
        g2.addView(Ui.hairline(this, 16));
        travelVal = value();
        g2.addView(choiceRow(getString(R.string.travel_time), travelVal, () -> {
            CharSequence[] n = new CharSequence[TRAVEL.length];
            int cur = 0;
            for (int i = 0; i < n.length; i++) {
                n[i] = TRAVEL[i] == 0 ? getString(R.string.none_label) : getString(R.string.minutes_fmt, TRAVEL[i]);
                if (TRAVEL[i] == travel) cur = i;
            }
            Sheets.actionSheet(this, getString(R.string.travel_time), n, cur, w -> {
                travel = TRAVEL[w];
                refreshChips();
            });
        }));
        body.addView(g2);

        // calendar, repeat, alerts, availability
        LinearLayout g3 = Ui.group(this, 24);
        calVal = value();
        LinearLayout calRow = choiceRow(getString(R.string.calendar), calVal, () -> {
            final List<Cals.Cal> own = new java.util.ArrayList<>();
            for (Cals.Cal x : Cals.all(this)) if (!x.builtin) own.add(x);
            CharSequence[] n = new CharSequence[own.size()];
            int cur = 0;
            for (int i = 0; i < n.length; i++) {
                n[i] = Cals.name(this, own.get(i));
                if (own.get(i).id.equals(cal)) cur = i;
            }
            Sheets.actionSheet(this, getString(R.string.calendar), n, cur, w -> {
                cal = own.get(w).id;
                refreshChips();
            });
        });
        calDot = new View(this);
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(Ui.dp(this, 12), Ui.dp(this, 12));
        dp.setMarginEnd(Ui.dp(this, 8));
        calRow.addView(calDot, 1, dp);
        g3.addView(calRow);
        g3.addView(Ui.hairline(this, 16));

        repVal = value();
        g3.addView(choiceRow(getString(R.string.repeat), repVal, () -> {
            CharSequence[] n = new CharSequence[REPEAT_ORDER.length];
            int cur = 0;
            for (int i = 0; i < n.length; i++) {
                n[i] = getString(REPEAT_LABEL[REPEAT_ORDER[i]]);
                if (REPEAT_ORDER[i] == repeat) cur = i;
            }
            Sheets.actionSheet(this, getString(R.string.repeat), n, cur, w -> {
                repeat = REPEAT_ORDER[w];
                if (repeat == 0) {
                    until = 0;
                    count = 0;
                }
                refreshChips();
            });
        }));
        g3.addView(Ui.hairline(this, 16));
        endVal = value();
        endRow = choiceRow(getString(R.string.end_repeat), endVal, this::chooseEnd);
        g3.addView(endRow);
        g3.addView(Ui.hairline(this, 16));

        remVal = value();
        g3.addView(choiceRow(getString(R.string.reminder), remVal, () -> {
            CharSequence[] n = new CharSequence[Prefs.REMIND.length];
            for (int i = 0; i < n.length; i++) n[i] = getString(Prefs.REMIND_LABEL[i]);
            Sheets.actionSheet(this, getString(R.string.reminder), n, Prefs.remindIndex(remind), w -> {
                remind = Prefs.REMIND[w];
                if (remind < 0) remind2 = -1;
                refreshChips();
            });
        }));
        rem2Val = value();
        LinearLayout r2 = choiceRow(getString(R.string.reminder), rem2Val, () -> {
            CharSequence[] n = new CharSequence[Prefs.REMIND.length];
            for (int i = 0; i < n.length; i++) n[i] = getString(Prefs.REMIND_LABEL[i]);
            Sheets.actionSheet(this, getString(R.string.reminder), n, Prefs.remindIndex(remind2), w -> {
                remind2 = Prefs.REMIND[w];
                refreshChips();
            });
        });
        rem2Row = r2;
        LinearLayout remBox = Ui.vertical(this);
        remBox.addView(Ui.hairline(this, 16));
        remBox.addView(r2);
        remBox.setTag("rem2");
        g3.addView(remBox);
        rem2Row = remBox;
        g3.addView(Ui.hairline(this, 16));

        availVal = value();
        g3.addView(choiceRow(getString(R.string.availability), availVal, () -> {
            CharSequence[] n = {getString(R.string.busy), getString(R.string.free)};
            Sheets.actionSheet(this, getString(R.string.availability), n, free ? 1 : 0, w -> {
                free = w == 1;
                refreshChips();
            });
        }));
        body.addView(g3);

        // emoji and colour
        LinearLayout g4 = Ui.group(this, 24);
        emojiVal = value();
        g4.addView(choiceRow(getString(R.string.emoji), emojiVal, () ->
                Sheets.emojiPicker(this, getString(R.string.emoji), s -> {
                    emoji = s;
                    refreshChips();
                })));
        g4.addView(Ui.hairline(this, 16));
        g4.addView(Ui.cell(this, getString(R.string.color)));
        LinearLayout swatches = new LinearLayout(this);
        swatches.setPadding(Ui.dp(this, 10), 0, Ui.dp(this, 10), Ui.dp(this, 10));
        for (int i = 0; i <= Event.COLORS.length; i++) {
            final int value = i - 1;
            FrameLayout ring = new FrameLayout(this);
            View dot = new View(this);
            GradientDrawable d = new GradientDrawable();
            d.setShape(GradientDrawable.OVAL);
            if (value < 0) {
                d.setColor(0);
                d.setStroke(Ui.dp(this, 2), Ui.color(this, R.color.text_secondary));
            } else {
                d.setColor(Ui.color(this, Event.COLORS[value]));
            }
            dot.setBackground(d);
            ring.addView(dot, new FrameLayout.LayoutParams(Ui.dp(this, 20), Ui.dp(this, 20), Gravity.CENTER));
            ring.setOnClickListener(v -> {
                color = value;
                paintRings();
            });
            swatches.addView(ring, new LinearLayout.LayoutParams(0, Ui.dp(this, 32), 1f));
            rings[i] = ring;
        }
        g4.addView(swatches);
        body.addView(g4);
        paintRings();

        LinearLayout g5 = Ui.group(this, 24);
        notes = field(R.string.notes, ev.notes, true, InputType.TYPE_CLASS_TEXT);
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
        }
    }

    private void chooseEnd() {
        CharSequence[] n = {getString(R.string.end_never), getString(R.string.end_on),
                getString(R.string.end_after)};
        int cur = until > 0 ? 1 : count > 0 ? 2 : 0;
        Sheets.actionSheet(this, getString(R.string.end_repeat), n, cur, w -> {
            if (w == 0) {
                until = 0;
                count = 0;
                refreshChips();
            } else if (w == 1) {
                Sheets.pickDate(this, until > 0 ? until : start.getTimeInMillis(), ms -> {
                    until = Recur.nextDay(Recur.dayStart(ms)) - 1;
                    count = 0;
                    refreshChips();
                });
            } else {
                CharSequence[] t = new CharSequence[TIMES.length];
                int c2 = 0;
                for (int i = 0; i < t.length; i++) {
                    t[i] = getString(R.string.end_times_fmt, TIMES[i]);
                    if (TIMES[i] == count) c2 = i;
                }
                Sheets.actionSheet(this, getString(R.string.end_after), t, c2, k -> {
                    count = TIMES[k];
                    until = 0;
                    refreshChips();
                });
            }
        });
    }

    private void paintRings() {
        for (int i = 0; i < rings.length; i++) {
            GradientDrawable r = new GradientDrawable();
            r.setShape(GradientDrawable.OVAL);
            r.setColor(0);
            if (i - 1 == color) {
                r.setStroke(Ui.dp(this, 2), i == 0 ? Ui.color(this, R.color.text_secondary)
                        : Ui.color(this, Event.COLORS[i - 1]));
            }
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

        travelVal.setText(travel == 0 ? getString(R.string.none_label) : getString(R.string.minutes_fmt, travel));
        Cals.Cal cc = Cals.get(this, cal);
        calVal.setText(Cals.name(this, cc));
        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL);
        dot.setColor(Cals.colorOf(this, cc));
        calDot.setBackground(dot);
        repVal.setText(REPEAT_LABEL[repeat]);
        endRow.setVisibility(repeat > 0 ? View.VISIBLE : View.GONE);
        endVal.setText(until > 0 ? getString(R.string.end_until_fmt, Fmt.date("yMMMd", until))
                : count > 0 ? getString(R.string.end_times_fmt, count) : getString(R.string.end_never));
        remVal.setText(Prefs.REMIND_LABEL[Prefs.remindIndex(remind)]);
        rem2Row.setVisibility(remind >= 0 ? View.VISIBLE : View.GONE);
        rem2Val.setText(Prefs.REMIND_LABEL[Prefs.remindIndex(remind2)]);
        availVal.setText(free ? R.string.free : R.string.busy);
        emojiVal.setText(emoji.isEmpty() ? getString(R.string.none_label) : emoji);
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
        ev.url = urlField.getText().toString().trim();
        ev.notes = notes.getText().toString().trim();
        ev.allDay = allDay;
        ev.repeat = repeat;
        ev.count = repeat > 0 ? count : 0;
        ev.until = repeat > 0 ? until : 0;
        ev.remind = remind;
        ev.remind2 = remind >= 0 ? remind2 : -1;
        ev.travel = travel;
        ev.free = free;
        ev.cal = cal;
        ev.emoji = emoji;
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
