package com.semo.calendar;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.splashscreen.SplashScreenViewProvider;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Five tabs (Calendar, Agenda, Tasks, Search, Settings) in an iPhone-style floating capsule bar. The Calendar tab
 * icon shows today's date.
 */
public final class MainActivity extends BaseActivity {
    private static final int COUNT = 5;
    private final Screen[] screens = new Screen[COUNT];
    private final View[] items = new View[COUNT];
    private final ImageView[] icons = new ImageView[COUNT];
    private final TextView[] names = new TextView[COUNT];
    private final TabIconDrawable[] tabIcons = new TabIconDrawable[COUNT];
    private FrameLayout content;
    private FrameLayout appRoot;
    private View pillView;
    private int cur = -1;

    private final ActivityResultLauncher<String> exportLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("text/calendar"), uri -> {
                if (uri != null) writeIcs(uri);
            });
    private final ActivityResultLauncher<String[]> importLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) readIcs(uri);
            });

    @Override
    protected void onCreate(Bundle b) {
        final long born = SystemClock.uptimeMillis();
        SplashScreen splash = SplashScreen.installSplashScreen(this);
        splash.setKeepOnScreenCondition(() -> SystemClock.uptimeMillis() - born < 520);
        splash.setOnExitAnimationListener(this::leaveSplash);
        super.onCreate(b);
        FrameLayout root = new FrameLayout(this);
        appRoot = root;
        root.setBackgroundColor(Ui.color(this, R.color.bg));
        applyInsets(root);
        setContentView(root);

        content = new FrameLayout(this);
        root.addView(content, new FrameLayout.LayoutParams(-1, -1));
        root.addView(buildNav());

        select(0);
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
        final android.content.Context app = getApplicationContext();
        new Thread(() -> Alarms.rescheduleAll(app)).start();
    }

    /** The splash icon grows and fades while the app settles in underneath it, like opening an app on iOS. */
    private void leaveSplash(final SplashScreenViewProvider p) {
        final View sv = p.getView();
        View icon = p.getIconView();
        if (icon != null) {
            icon.animate().scaleX(1.3f).scaleY(1.3f).alpha(0f).setDuration(320).setInterpolator(Motion.IN).start();
        }
        if (appRoot != null) {
            appRoot.setScaleX(1.06f);
            appRoot.setScaleY(1.06f);
            appRoot.setAlpha(0.4f);
            appRoot.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(480).setInterpolator(Motion.OUT).start();
        }
        sv.animate().alpha(0f).setDuration(300).setStartDelay(60).setInterpolator(Motion.DECEL)
                .withEndAction(p::remove).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (tabIcons[0] != null) tabIcons[0].invalidateSelf();
        refreshCurrent();
    }

    void refreshCurrent() {
        if (cur >= 0 && screens[cur] != null) screens[cur].refresh();
    }

    private View buildNav() {
        FrameLayout bar = new FrameLayout(this);
        GradientDrawable bg = Ui.shape(Ui.color(this, R.color.surface), Ui.dp(this, 100));
        bg.setStroke(Math.max(1, Ui.dp(this, 0.5f)), Ui.color(this, R.color.stroke));
        bar.setBackground(bg);
        bar.setPadding(Ui.dp(this, 5), Ui.dp(this, 5), Ui.dp(this, 5), Ui.dp(this, 5));
        bar.setElevation(Ui.dp(this, 8));

        pillView = new View(this);
        pillView.setBackground(Ui.shape(Ui.color(this, R.color.neutral_soft), Ui.dp(this, 100)));
        pillView.setVisibility(View.INVISIBLE);
        bar.addView(pillView, new FrameLayout.LayoutParams(Ui.dp(this, 60), Ui.dp(this, 54),
                Gravity.LEFT | Gravity.TOP));

        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        int[] lb = {R.string.tab_month, R.string.tab_agenda, R.string.tab_tasks, R.string.tab_search,
                R.string.tab_settings};
        for (int i = 0; i < COUNT; i++) {
            final int idx = i;
            LinearLayout it = new LinearLayout(this);
            it.setOrientation(LinearLayout.VERTICAL);
            it.setGravity(Gravity.CENTER);
            it.setContentDescription(getString(lb[i]));
            ImageView icon = new ImageView(this);
            tabIcons[i] = new TabIconDrawable(this, i);
            icon.setImageDrawable(tabIcons[i]);
            it.addView(icon, new LinearLayout.LayoutParams(Ui.dp(this, 24), Ui.dp(this, 24)));
            TextView t = Ui.text(this, getString(lb[i]), 10, R.color.text_secondary, true);
            t.setLetterSpacing(0.01f);
            t.setSingleLine(true);
            t.setGravity(Gravity.CENTER);
            androidx.core.widget.TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                    t, 7, 10, 1, android.util.TypedValue.COMPLEX_UNIT_SP);
            LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-1, -2);
            tp.topMargin = Ui.dp(this, 2);
            tp.setMargins(Ui.dp(this, 2), Ui.dp(this, 2), Ui.dp(this, 2), 0);
            it.addView(t, tp);
            Ui.press(it);
            it.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                if (idx != cur) {
                    select(idx);
                    Motion.bounce(icons[idx]);
                } else if (screens[idx] != null) {
                    screens[idx].refresh();
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, Ui.dp(this, 54), 1f);
            lp.setMargins(Ui.dp(this, 1), 0, Ui.dp(this, 1), 0);
            row.addView(it, lp);
            items[i] = it;
            icons[i] = icon;
            names[i] = t;
        }
        bar.addView(row, new FrameLayout.LayoutParams(-1, -2));
        row.addOnLayoutChangeListener((v, l, t, r, bt, ol, ot, or, ob) -> {
            if (r - l != or - ol) placePill(false);
        });
        FrameLayout.LayoutParams blp = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        blp.setMargins(Ui.dp(this, 14), Ui.dp(this, 6), Ui.dp(this, 14), Ui.dp(this, 10));
        bar.setLayoutParams(blp);
        return bar;
    }

    /** Moves the selection capsule under the chosen tab; it glides with a spring when {@code animate} is true. */
    private void placePill(boolean animate) {
        if (cur < 0 || pillView == null || items[cur] == null) return;
        View it = items[cur];
        if (it.getWidth() == 0) return;
        android.view.ViewGroup.LayoutParams lp = pillView.getLayoutParams();
        if (lp.width != it.getWidth()) {
            lp.width = it.getWidth();
            pillView.setLayoutParams(lp);
        }
        pillView.setVisibility(View.VISIBLE);
        pillView.animate().cancel();
        float x = it.getLeft();
        if (animate) {
            pillView.animate().translationX(x).setDuration(480).setInterpolator(Motion.SPRING_SOFT).start();
        } else {
            pillView.setTranslationX(x);
        }
    }

    private Screen make(int i) {
        switch (i) {
            case 0:
                return new CalendarTab(this);
            case 1:
                return new AgendaScreen(this);
            case 2:
                return new TasksScreen(this);
            case 3:
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
        int prev = cur;
        cur = i;
        for (int k = 0; k < COUNT; k++) {
            if (screens[k] != null) {
                screens[k].view().animate().cancel();
                screens[k].view().setVisibility(k == i ? View.VISIBLE : View.GONE);
            }
            boolean on = k == i;
            int col = Ui.color(this, on ? R.color.accent : R.color.text_secondary);
            tabIcons[k].setState(on, col);
            names[k].setTextColor(col);
        }
        screens[i].refresh();
        if (prev >= 0) Motion.enter(screens[i].view(), 10);
        placePill(prev >= 0);
    }

    void openEditor(String id, long day) {
        Intent in = new Intent(this, EditorActivity.class);
        if (id != null) in.putExtra("id", id);
        if (day > 0) in.putExtra("day", day);
        startActivity(in);
    }

    void openEditorAt(long at) {
        startActivity(new Intent(this, EditorActivity.class).putExtra("at", at));
    }

    void showDetail(Recur.Occ o) {
        DetailSheet.show(this, o);
    }

    void duplicate(Event e) {
        Event n = e.copy();
        Store.save(this, n);
        Alarms.schedule(getApplicationContext(), n);
        refreshCurrent();
    }

    /** Open / toggle behaviour of task rows shown inside the Month and Agenda lists. */
    EventAdapter.TaskHandler taskHandler() {
        return new EventAdapter.TaskHandler() {
            @Override
            public void open(Task t) {
                TaskSheet.show(MainActivity.this, t, 0);
            }

            @Override
            public void toggle(Task t) {
                t.done = !t.done;
                Tasks.save(MainActivity.this, t);
                Alarms.scheduleTask(getApplicationContext(), t);
                refreshCurrent();
            }
        };
    }

    void confirmDelete(final Event e) {
        Sheets.alert(this, getString(R.string.delete_q), e.repeat > 0 ? getString(R.string.delete_series) : null,
                getString(R.string.delete), true, () -> {
                    Store.delete(this, e.id);
                    Alarms.cancel(this, e.id);
                    refreshCurrent();
                });
    }

    void exportIcs() {
        exportLauncher.launch("semo-calendar.ics");
    }

    void importIcs() {
        importLauncher.launch(new String[]{"text/calendar", "text/plain", "application/octet-stream"});
    }

    private void writeIcs(Uri uri) {
        try (OutputStream o = getContentResolver().openOutputStream(uri)) {
            if (o == null) throw new IllegalStateException();
            o.write(Ics.export(Store.all(this)).getBytes(StandardCharsets.UTF_8));
            Toast.makeText(this, R.string.exported, Toast.LENGTH_SHORT).show();
        } catch (Exception ex) {
            Toast.makeText(this, R.string.export_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void readIcs(Uri uri) {
        int n = 0;
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            if (in == null) throw new IllegalStateException();
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) > 0) {
                bo.write(buf, 0, r);
                if (bo.size() > 5 * 1024 * 1024) throw new IllegalStateException();
            }
            List<Event> list = Ics.parse(new String(bo.toByteArray(), StandardCharsets.UTF_8));
            for (Event e : list) {
                Event old = Store.get(this, e.id);
                e.cal = old != null ? old.cal : Prefs.defaultCal(this);
                if (old != null) {
                    e.color = old.color;
                    e.emoji = old.emoji;
                    e.travel = old.travel;
                }
                Store.save(this, e);
                Alarms.schedule(getApplicationContext(), e);
                n++;
            }
        } catch (Exception ex) {
            n = 0;
        }
        Toast.makeText(this, n > 0 ? getString(R.string.imported, n) : getString(R.string.import_failed),
                Toast.LENGTH_SHORT).show();
        refreshCurrent();
    }
}
