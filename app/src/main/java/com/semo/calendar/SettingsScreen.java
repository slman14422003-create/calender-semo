package com.semo.calendar;

import android.graphics.Typeface;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import java.text.DateFormatSymbols;
import java.util.List;
import java.util.Locale;

/** Settings tab: large title, inset-grouped cells with disclosure chevrons, action sheets for choices. */
final class SettingsScreen implements Screen {
    private static final int[] FIRST = {java.util.Calendar.SATURDAY, java.util.Calendar.SUNDAY, java.util.Calendar.MONDAY};

    private final MainActivity a;
    private final LinearLayout root;
    private final LinearLayout body;

    SettingsScreen(MainActivity act) {
        a = act;
        root = Ui.vertical(a);
        root.setBackgroundColor(Ui.color(a, R.color.bg_group));
        final LargeNav nav = new LargeNav(a, a.getString(R.string.tab_settings), null);
        root.addView(nav.view(), new LinearLayout.LayoutParams(-1, nav.height()));
        ScrollView sv = new ScrollView(a);
        sv.setClipToPadding(false);
        sv.setPadding(0, 0, 0, Ui.dp(a, 120));
        sv.setOverScrollMode(View.OVER_SCROLL_NEVER);
        sv.setOnScrollChangeListener((v, x, y, ox, oy) -> nav.onScroll(y));
        body = Ui.vertical(a);
        sv.addView(body, new ScrollView.LayoutParams(-1, -2));
        root.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1f));
    }

    @Override
    public View view() {
        return root;
    }

    private String dayName(int weekday) {
        return DateFormatSymbols.getInstance(Locale.getDefault()).getWeekdays()[weekday];
    }

    private View row(CharSequence title, CharSequence value, boolean chevron, View.OnClickListener click) {
        return row(title, value, chevron, click, null);
    }

    private View row(CharSequence title, CharSequence value, boolean chevron, View.OnClickListener click,
                     View leading) {
        LinearLayout r = Ui.cell(a, title, leading);
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
        title.setPadding(Ui.dp(a, 20), Ui.dp(a, 2), Ui.dp(a, 20), Ui.dp(a, 6));
        body.addView(title);

        LinearLayout g = Ui.group(a, 8);
        final String[] tags = {"", "en", "ar", "fr", "es", "de", "tr"};
        final String[] langNames = a.getResources().getStringArray(R.array.lang_names);
        langNames[0] = a.getString(R.string.lang_system);
        int langNow = 0;
        LocaleListCompat appLocales = AppCompatDelegate.getApplicationLocales();
        if (!appLocales.isEmpty() && appLocales.get(0) != null) {
            String lang = appLocales.get(0).getLanguage();
            for (int i = 1; i < tags.length; i++) if (tags[i].equals(lang)) langNow = i;
        }
        final int langChecked = langNow;
        g.addView(row(a.getString(R.string.language), langNames[langNow], true,
                v -> choose(R.string.language, langNames, langChecked, w -> AppCompatDelegate.setApplicationLocales(
                        w == 0 ? LocaleListCompat.getEmptyLocaleList() : LocaleListCompat.forLanguageTags(tags[w]))),
                Ui.tile(a, R.drawable.ic_globe, Ui.color(a, R.color.accent))));
        g.addView(Ui.hairline(a, 58));
        g.addView(row(a.getString(R.string.first_day), dayName(Prefs.firstDay(a)), true, v -> {
            CharSequence[] names = {dayName(FIRST[0]), dayName(FIRST[1]), dayName(FIRST[2])};
            int cur = 1;
            for (int i = 0; i < FIRST.length; i++) if (FIRST[i] == Prefs.firstDay(a)) cur = i;
            choose(R.string.first_day, names, cur, w -> Prefs.setFirstDay(a, FIRST[w]));
        }, Ui.tile(a, R.drawable.ic_month, Ui.color(a, R.color.bad))));
        g.addView(Ui.hairline(a, 58));
        g.addView(row(a.getString(R.string.default_reminder),
                a.getString(Prefs.REMIND_LABEL[Prefs.remindIndex(Prefs.defaultRemind(a))]), true, v -> {
                    CharSequence[] names = new CharSequence[Prefs.REMIND.length];
                    for (int i = 0; i < names.length; i++) names[i] = a.getString(Prefs.REMIND_LABEL[i]);
                    choose(R.string.default_reminder, names, Prefs.remindIndex(Prefs.defaultRemind(a)),
                            w -> Prefs.setDefaultRemind(a, Prefs.REMIND[w]));
                }, Ui.tile(a, R.drawable.ic_bell, Ui.color(a, R.color.warn))));
        g.addView(Ui.hairline(a, 58));
        g.addView(row(a.getString(R.string.default_duration),
                a.getString(R.string.minutes_fmt, Prefs.duration(a)), true, v -> {
                    CharSequence[] names = new CharSequence[Prefs.DURATION.length];
                    int cur = 3;
                    for (int i = 0; i < names.length; i++) {
                        names[i] = a.getString(R.string.minutes_fmt, Prefs.DURATION[i]);
                        if (Prefs.DURATION[i] == Prefs.duration(a)) cur = i;
                    }
                    choose(R.string.default_duration, names, cur, w -> Prefs.setDuration(a, Prefs.DURATION[w]));
                }, Ui.tile(a, R.drawable.ic_clock, Ui.color(a, R.color.ok))));
        g.addView(Ui.hairline(a, 58));
        int[] themeRes = {R.string.theme_system, R.string.theme_light, R.string.theme_dark};
        g.addView(row(a.getString(R.string.theme), a.getString(themeRes[Prefs.theme(a)]), true, v -> {
            CharSequence[] names = {a.getString(themeRes[0]), a.getString(themeRes[1]), a.getString(themeRes[2])};
            choose(R.string.theme, names, Prefs.theme(a), w -> {
                Prefs.setTheme(a, w);
                AppCompatDelegate.setDefaultNightMode(w == 1 ? AppCompatDelegate.MODE_NIGHT_NO
                        : w == 2 ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            });
        }, Ui.tile(a, R.drawable.ic_moon, Ui.color(a, R.color.violet))));
        body.addView(g);

        body.addView(Ui.sectionHeader(a, a.getString(R.string.data)));
        LinearLayout dg = Ui.group(a, 0);
        dg.addView(row(a.getString(R.string.export_ics), "", false, v -> a.exportIcs(),
                Ui.tile(a, R.drawable.ic_upload, Ui.color(a, R.color.accent))));
        dg.addView(Ui.hairline(a, 58));
        dg.addView(row(a.getString(R.string.import_ics), "", false, v -> a.importIcs(),
                Ui.tile(a, R.drawable.ic_download, Ui.color(a, R.color.ok))));
        body.addView(dg);

        body.addView(Ui.sectionHeader(a, a.getString(R.string.semo_apps)));
        LinearLayout s = Ui.group(a, 0);
        List<Semo.Sibling> sib = Semo.siblings(a);
        if (sib.isEmpty()) {
            s.addView(row(a.getString(R.string.none_installed), "", false, null));
        } else {
            for (int i = 0; i < sib.size(); i++) {
                final Semo.Sibling x = sib.get(i);
                if (i > 0) s.addView(Ui.hairline(a, 58));
                s.addView(row(x.label, x.version, true, v -> Semo.open(a, x.pkg),
                        x.icon == null ? null : Ui.appIcon(a, x.icon)));
            }
        }
        body.addView(s);

        LinearLayout ab = Ui.group(a, 24);
        ab.addView(row(a.getString(R.string.version), BuildConfig.VERSION_NAME, false, null));
        body.addView(ab);
    }
}
