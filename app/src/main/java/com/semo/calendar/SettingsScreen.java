package com.semo.calendar;

import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;

import java.text.DateFormatSymbols;
import java.util.Locale;
import java.util.function.IntConsumer;

/** Settings tab: iOS inset-grouped lists. */
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

    private View row(CharSequence title, CharSequence value, View.OnClickListener click) {
        LinearLayout r = new LinearLayout(a);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(Ui.dp(a, 16), Ui.dp(a, 13), Ui.dp(a, 16), Ui.dp(a, 13));
        TextView t = Ui.text(a, title, 17, R.color.text_primary, false);
        r.addView(t, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView v = Ui.text(a, value, 17, R.color.text_secondary, false);
        r.addView(v);
        if (click != null) {
            r.setOnClickListener(click);
            Ui.press(r);
        }
        return r;
    }

    private void choose(int titleRes, CharSequence[] items, int checked, IntConsumer pick) {
        new AlertDialog.Builder(a).setTitle(titleRes)
                .setSingleChoiceItems(items, checked, (d, w) -> {
                    pick.accept(w);
                    d.dismiss();
                    refresh();
                })
                .setNegativeButton(R.string.cancel, null).show();
    }

    @Override
    public void refresh() {
        body.removeAllViews();
        TextView title = Ui.text(a, a.getString(R.string.tab_settings), 30, R.color.text_primary, false);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(Ui.dp(a, 20), Ui.dp(a, 10), Ui.dp(a, 20), Ui.dp(a, 0));
        body.addView(title);

        // general
        LinearLayout g = Ui.group(a);
        int first = Prefs.firstDay(a);
        g.addView(row(a.getString(R.string.first_day), dayName(first), v -> {
            CharSequence[] names = {dayName(FIRST[0]), dayName(FIRST[1]), dayName(FIRST[2])};
            int cur = 1;
            for (int i = 0; i < FIRST.length; i++) if (FIRST[i] == Prefs.firstDay(a)) cur = i;
            choose(R.string.first_day, names, cur, w -> Prefs.setFirstDay(a, FIRST[w]));
        }));
        g.addView(Ui.hairline(a, 16));
        g.addView(row(a.getString(R.string.default_reminder),
                a.getString(Prefs.REMIND_LABEL[Prefs.remindIndex(Prefs.defaultRemind(a))]), v -> {
                    CharSequence[] names = new CharSequence[Prefs.REMIND.length];
                    for (int i = 0; i < names.length; i++) names[i] = a.getString(Prefs.REMIND_LABEL[i]);
                    choose(R.string.default_reminder, names, Prefs.remindIndex(Prefs.defaultRemind(a)),
                            w -> Prefs.setDefaultRemind(a, Prefs.REMIND[w]));
                }));
        g.addView(Ui.hairline(a, 16));
        int[] themeRes = {R.string.theme_system, R.string.theme_light, R.string.theme_dark};
        g.addView(row(a.getString(R.string.theme), a.getString(themeRes[Prefs.theme(a)]), v -> {
            CharSequence[] names = {a.getString(themeRes[0]), a.getString(themeRes[1]), a.getString(themeRes[2])};
            choose(R.string.theme, names, Prefs.theme(a), w -> {
                Prefs.setTheme(a, w);
                AppCompatDelegate.setDefaultNightMode(w == 1 ? AppCompatDelegate.MODE_NIGHT_NO
                        : w == 2 ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            });
        }));
        body.addView(g);

        // Semo ecosystem
        TextView h = Ui.text(a, a.getString(R.string.semo_apps), 13, R.color.text_secondary, true);
        h.setPadding(Ui.dp(a, 32), Ui.dp(a, 22), Ui.dp(a, 32), Ui.dp(a, 0));
        body.addView(h);
        LinearLayout s = Ui.group(a);
        java.util.List<Semo.Sibling> sib = Semo.siblings(a);
        if (sib.isEmpty()) {
            s.addView(row(a.getString(R.string.none_installed), "", null));
        } else {
            for (int i = 0; i < sib.size(); i++) {
                final Semo.Sibling x = sib.get(i);
                if (i > 0) s.addView(Ui.hairline(a, 16));
                s.addView(row(x.label, x.version.isEmpty() ? a.getString(R.string.open) : x.version,
                        v -> Semo.open(a, x.pkg)));
            }
        }
        body.addView(s);

        // about
        LinearLayout ab = Ui.group(a);
        ab.addView(row(a.getString(R.string.version), BuildConfig.VERSION_NAME, null));
        body.addView(ab);
    }
}
