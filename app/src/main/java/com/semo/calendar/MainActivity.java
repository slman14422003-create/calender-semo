package com.semo.calendar;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.splashscreen.SplashScreen;

/** Four tabs (Month, Agenda, Search, Settings) in an iPhone-style floating capsule bar, plus an Android-style round add button. */
public final class MainActivity extends BaseActivity {
    private static final int COUNT = 4;
    private final Screen[] screens = new Screen[COUNT];
    private final View[] items = new View[COUNT];
    private final ImageView[] icons = new ImageView[COUNT];
    private final TextView[] names = new TextView[COUNT];
    private final GradientDrawable[] pills = new GradientDrawable[COUNT];
    private FrameLayout content;
    private ImageButton fab;
    private int cur = -1;

    @Override
    protected void onCreate(Bundle b) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(b);
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Ui.color(this, R.color.bg));
        applyInsets(root);
        setContentView(root);

        content = new FrameLayout(this);
        root.addView(content, new FrameLayout.LayoutParams(-1, -1));
        root.addView(buildNav());

        fab = new ImageButton(this);
        fab.setImageResource(R.drawable.ic_add);
        fab.setImageTintList(android.content.res.ColorStateList.valueOf(Ui.color(this, R.color.on_accent)));
        GradientDrawable oval = new GradientDrawable();
        oval.setShape(GradientDrawable.OVAL);
        oval.setColor(Ui.color(this, R.color.accent));
        fab.setBackground(oval);
        fab.setScaleType(ImageView.ScaleType.CENTER);
        fab.setElevation(Ui.dp(this, 6));
        fab.setContentDescription(getString(R.string.new_event));
        FrameLayout.LayoutParams fp = new FrameLayout.LayoutParams(Ui.dp(this, 56), Ui.dp(this, 56),
                Gravity.BOTTOM | Gravity.END);
        fp.setMargins(Ui.dp(this, 20), 0, Ui.dp(this, 20), Ui.dp(this, 92));
        fab.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            long day = (screens[0] instanceof MonthScreen && cur == 0)
                    ? ((MonthScreen) screens[0]).selectedMillis() : 0;
            openEditor(null, day);
        });
        Ui.press(fab);
        root.addView(fab, fp);

        select(0);
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
        final android.content.Context app = getApplicationContext();
        new Thread(() -> Alarms.rescheduleAll(app)).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (cur >= 0 && screens[cur] != null) screens[cur].refresh();
    }

    private View buildNav() {
        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        GradientDrawable bg = Ui.shape(Ui.color(this, R.color.surface), Ui.dp(this, 100));
        bg.setStroke(Math.max(1, Ui.dp(this, 0.5f)), Ui.color(this, R.color.stroke));
        bar.setBackground(bg);
        bar.setPadding(Ui.dp(this, 5), Ui.dp(this, 5), Ui.dp(this, 5), Ui.dp(this, 5));
        bar.setElevation(Ui.dp(this, 8));
        int[] ic = {R.drawable.ic_month, R.drawable.ic_agenda, R.drawable.ic_search, R.drawable.ic_settings};
        int[] lb = {R.string.tab_month, R.string.tab_agenda, R.string.tab_search, R.string.tab_settings};
        for (int i = 0; i < COUNT; i++) {
            final int idx = i;
            LinearLayout it = new LinearLayout(this);
            it.setOrientation(LinearLayout.VERTICAL);
            it.setGravity(Gravity.CENTER);
            GradientDrawable pill = Ui.shape(Ui.color(this, R.color.neutral_soft), Ui.dp(this, 100));
            pill.setAlpha(0);
            it.setBackground(pill);
            pills[i] = pill;
            it.setContentDescription(getString(lb[i]));
            ImageView icon = new ImageView(this);
            icon.setImageResource(ic[i]);
            it.addView(icon, new LinearLayout.LayoutParams(Ui.dp(this, 24), Ui.dp(this, 24)));
            TextView t = Ui.text(this, getString(lb[i]), 10.5f, R.color.text_secondary, true);
            t.setSingleLine(true);
            t.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-2, -2);
            tp.topMargin = Ui.dp(this, 2);
            it.addView(t, tp);
            Ui.press(it);
            it.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                if (idx != cur) select(idx);
                else if (screens[idx] != null) screens[idx].refresh();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, Ui.dp(this, 54), 1f);
            lp.setMargins(Ui.dp(this, 2), 0, Ui.dp(this, 2), 0);
            bar.addView(it, lp);
            items[i] = it;
            icons[i] = icon;
            names[i] = t;
        }
        FrameLayout.LayoutParams blp = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        blp.setMargins(Ui.dp(this, 20), Ui.dp(this, 6), Ui.dp(this, 20), Ui.dp(this, 10));
        bar.setLayoutParams(blp);
        return bar;
    }

    private Screen make(int i) {
        switch (i) {
            case 0:
                return new MonthScreen(this);
            case 1:
                return new AgendaScreen(this);
            case 2:
                return new SearchScreen(this);
            default:
                return new SettingsScreen(this);
        }
    }

    private void select(int i) {
        if (screens[i] == null) {
            screens[i] = make(i);
            content.addView(screens[i].view(), new FrameLayout.LayoutParams(-1, -1));
        }
        cur = i;
        for (int k = 0; k < COUNT; k++) {
            if (screens[k] != null) screens[k].view().setVisibility(k == i ? View.VISIBLE : View.GONE);
            boolean on = k == i;
            int col = Ui.color(this, on ? R.color.accent : R.color.text_secondary);
            pills[k].setAlpha(on ? 255 : 0);
            icons[k].setImageTintList(android.content.res.ColorStateList.valueOf(col));
            names[k].setTextColor(col);
        }
        fab.setVisibility(i == 3 ? View.GONE : View.VISIBLE);
        screens[i].refresh();
    }

    void openEditor(String id, long day) {
        Intent in = new Intent(this, EditorActivity.class);
        if (id != null) in.putExtra("id", id);
        if (day > 0) in.putExtra("day", day);
        startActivity(in);
    }

    /** Event card: details with Edit, Delete and (when the Notes app is installed) Send to Notes. */
    void showDetail(Recur.Occ o) {
        final Event e = o.e;
        StringBuilder sb = new StringBuilder();
        sb.append(Fmt.date("EEEEdMMMM", o.start)).append('\n').append(Fmt.line(this, o, false));
        if (e.repeat > 0) sb.append('\n').append(getString(EditorActivity.REPEAT_LABEL[e.repeat]));
        if (e.remind >= 0) sb.append('\n').append(getString(Prefs.REMIND_LABEL[Prefs.remindIndex(e.remind)]));
        if (!e.notes.isEmpty()) sb.append("\n\n").append(e.notes);
        final String title = e.title.isEmpty() ? getString(R.string.untitled) : e.title;
        AlertDialog.Builder b = new AlertDialog.Builder(this).setTitle(title).setMessage(sb.toString())
                .setPositiveButton(R.string.edit, (d, w) -> openEditor(e.id, 0))
                .setNegativeButton(R.string.delete, (d, w) -> confirmDelete(e));
        if (Semo.hasNotes(this)) {
            final String text = sb.toString();
            b.setNeutralButton(R.string.send_notes, (d, w) -> Semo.sendToNotes(this, title, title + "\n" + text));
        }
        b.show();
    }

    private void confirmDelete(final Event e) {
        String msg = e.repeat > 0 ? getString(R.string.delete_series) : null;
        new AlertDialog.Builder(this).setTitle(R.string.delete_q).setMessage(msg)
                .setPositiveButton(R.string.delete, (d, w) -> {
                    Store.delete(this, e.id);
                    Alarms.cancel(this, e.id);
                    if (cur >= 0 && screens[cur] != null) screens[cur].refresh();
                })
                .setNegativeButton(R.string.cancel, null).show();
    }
}
