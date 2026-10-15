package com.semo.calendar;

import android.graphics.Typeface;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatDelegate;

import java.text.DateFormatSymbols;
import java.util.List;
import java.util.Locale;

/** Settings tab: large title, inset-grouped cells with disclosure chevrons, action sheets for choices. */
final class SettingsScreen implements Screen {
    private static final int[] FIRST = {java.util.Calendar.SATURDAY, java.util.Calendar.SUNDAY, java.util.Calendar.MONDAY};

    private final MainActivity a;
    private final ScrollView root;
    private final LinearLayout body;

    SettingsScreen(MainActivity act) {
        a = act;
        root = new ScrollView(a);
        root.setBackgroundColor(Ui.color(a, R.color.bg_group));
        root.setClipToPadding(false);
        root.setPadding(0, 0, 0, Ui.dp(a, 120));
        body = Ui.vertical(a);
        root.addView(body, new ScrollView.LayoutParams(-1, -2));
    }

    @Override
    public View view() {
        return root;
    }

    private String dayName(int weekday) {
        return DateFormatSymbols.getInstance(Locale.getDefault()).getWeekdays()[weekday];
    }

    private View row(CharSequence title, CharSequence value, boolean chevron, View.OnClickListener click) {
        LinearLayout r = Ui.cell(a, title);
        if (value.length() > 0) r.addView(Ui.text(a, value, 17, R.color.text_secondary, false));
        if (chevron) r.addView(Ui.chevron(a));
        if (click != null) {
            r.setOnClickListener(click);
            Ui.press(r);
        }
        return r;
    }

    private void choose(int titleRes, CharSequence[] items, int checked, Sheets.Pick pick) {
        Sheets.actionSheet(a, a.getString(titleRes), items, checked, i -> {
            pick.on(i);
            refresh();
        });
    }

    @Override
    public void refresh() {
        body.removeAllViews();
        TextView title = Ui.text(a, a.getString(R.string.tab_settings), 34, R.color.text_primary, false);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(Ui.dp(a, 20), Ui.dp(a, 56), Ui.dp(a, 20), Ui.dp(a, 6));
        body.addView(title);

        LinearLayout g = Ui.group(a, 8);
        g.addView(row(a.getString(R.string.first_day), dayName(Prefs.firstDay(a)), true, v -> {
            CharSequence[] names = {dayName(FIRST[0]), dayName(FIRST[1]), dayName(FIRST[2])};
            int cur = 1;
            for (int i = 0; i < FIRST.length; i++) if (FIRST[i] == Prefs.firstDay(a)) cur = i;
            choose(R.string.first_day, names, cur, w -> Prefs.setFirstDay(a, FIRST[w]));
        }));
        g.addView(Ui.hairline(a, 16));
        g.addView(row(a.getString(R.string.default_reminder),
                a.getString(Prefs.REMIND_LABEL[Prefs.remindIndex(Prefs.defaultRemind(a))]), true, v -> {
                    CharSequence[] names = new CharSequence[Prefs.REMIND.length];
                    for (int i = 0; i < names.length; i++) names[i] = a.getString(Prefs.REMIND_LABEL[i]);
                    choose(R.string.default_reminder, names, Prefs.remindIndex(Prefs.defaultRemind(a)),
                            w -> Prefs.setDefaultRemind(a, Prefs.REMIND[w]));
                }));
        g.addView(Ui.hairline(a, 16));
        g.addView(row(a.getString(R.string.default_duration),
                a.getString(R.string.minutes_fmt, Prefs.duration(a)), true, v -> {
                    CharSequence[] names = new CharSequence[Prefs.DURATION.length];
                    int cur = 3;
                    for (int i = 0; i < names.length; i++) {
                        names[i] = a.getString(R.string.minutes_fmt, Prefs.DURATION[i]);
                        if (Prefs.DURATION[i] == Prefs.duration(a)) cur = i;
                    }
                    choose(R.string.default_duration, names, cur, w -> Prefs.setDuration(a, Prefs.DURATION[w]));
                }));
        g.addView(Ui.hairline(a, 16));
        int[] themeRes = {R.string.theme_system, R.string.theme_light, R.string.theme_dark};
        g.addView(row(a.getString(R.string.theme), a.getString(themeRes[Prefs.theme(a)]), true, v -> {
            CharSequence[] names = {a.getString(themeRes[0]), a.getString(themeRes[1]), a.getString(themeRes[2])};
            choose(R.string.theme, names, Prefs.theme(a), w -> {
                Prefs.setTheme(a, w);
                AppCompatDelegate.setDefaultNightMode(w == 1 ? AppCompatDelegate.MODE_NIGHT_NO
                        : w == 2 ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            });
        }));
        body.addView(g);

        body.addView(Ui.sectionHeader(a, a.getString(R.string.data)));
        LinearLayout dg = Ui.group(a, 0);
        dg.addView(row(a.getString(R.string.export_ics), "", false, v -> a.exportIcs()));
        dg.addView(Ui.hairline(a, 16));
        dg.addView(row(a.getString(R.string.import_ics), "", false, v -> a.importIcs()));
        body.addView(dg);

        body.addView(Ui.sectionHeader(a, a.getString(R.string.semo_apps)));
        LinearLayout s = Ui.group(a, 0);
        List<Semo.Sibling> sib = Semo.siblings(a);
        if (sib.isEmpty()) {
            s.addView(row(a.getString(R.string.none_installed), "", false, null));
        } else {
            for (int i = 0; i < sib.size(); i++) {
                final Semo.Sibling x = sib.get(i);
                if (i > 0) s.addView(Ui.hairline(a, 16));
                s.addView(row(x.label, x.version, true, v -> Semo.open(a, x.pkg)));
            }
        }
        body.addView(s);

        LinearLayout ab = Ui.group(a, 24);
        ab.addView(row(a.getString(R.string.version), BuildConfig.VERSION_NAME, false, null));
        body.addView(ab);
    }
}
