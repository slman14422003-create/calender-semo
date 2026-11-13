package com.semo.calendar;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.splashscreen.SplashScreenViewProvider;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import eightbitlab.com.blurview.BlurTarget;
import eightbitlab.com.blurview.BlurView;

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
    private BlurTarget content;
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

        content = new BlurTarget(this);
        root.setClipChildren(false);
        root.addView(content, new FrameLayout.LayoutParams(-1, -1));
        root.addView(buildNav());

        select(0);
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
        final android.content.Context app = getApplicationContext();
        new Thread(() -> Alarms.rescheduleAll(app)).start();
        // once a day at most: offers the update screen when GitHub has a newer release
        root.postDelayed(() -> Updater.autoCheck(this), 2500);
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
        DynamicIcon.sync(this);
        DynamicIcon.schedule(this);
        refreshCurrent();
    }

    void refreshCurrent() {
        if (cur >= 0 && screens[cur] != null) screens[cur].refresh();
    }

    private View buildNav() {
        BlurView bar = new NavBar(this);
        if (Glass.enabled(this)) {
            bar.setupWith(content).setFrameClearDrawable(new ColorDrawable(Ui.color(this, R.color.bg)))
                    .setBlurRadius(Glass.blurRadius(this));
        }
        bar.setBackground(new GlassDrawable(this, Ui.dpf(this, 100), Glass.panelFill(this), 1f));
        bar.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
        bar.setClipToOutline(true);
        bar.setElevation(0f);
        bar.setPadding(Ui.dp(this, 5), Ui.dp(this, 5), Ui.dp(this, 5), Ui.dp(this, 5));

        pillView = new View(this);
        pillView.setBackground(new GlassDrawable(this, Ui.dpf(this, 100),
                Glass.night(this) ? 0x33FFFFFF : 0x1F000000, 0f));
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
            LongPress.set(it, 26, (x, y) -> tabMenu(idx));
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
        bar.setLayoutParams(navParams());
        return bar;
    }

    private FrameLayout.LayoutParams navParams() {
        FrameLayout.LayoutParams blp = new FrameLayout.LayoutParams(-1, Ui.dp(this, 64), Gravity.BOTTOM);
        blp.setMargins(Ui.dp(this, 18), 0, Ui.dp(this, 18), Ui.dp(this, 10));
        return blp;
    }

    private List<LongPress.Item> tabMenu(final int idx) {
        List<LongPress.Item> l = new ArrayList<>();
        if (idx != cur) l.add(LongPress.item(getString(R.string.open), () -> {
            select(idx);
            Motion.bounce(icons[idx]);
        }));
        switch (idx) {
            case 0:
                l.add(LongPress.item(getString(R.string.today), () -> {
                    if (idx != cur) select(idx);
                    ((CalendarTab) screens[0]).goToday();
                }));
                l.add(LongPress.item(getString(R.string.new_event), () -> openEditor(null, ((CalendarTab) screens[0]).selectedMillis())));
                l.add(LongPress.item(getString(R.string.new_task), () -> TaskSheet.show(this, null, 0)));
                break;
            case 1:
                l.add(LongPress.item(getString(R.string.new_event), () -> openEditor(null, 0)));
                l.add(LongPress.item(getString(R.string.smart_add), () -> QuickAddSheet.show(this)));
                break;
            case 2:
                l.add(LongPress.item(getString(R.string.new_task), () -> TaskSheet.show(this, null, 0)));
                break;
            default:
                break;
        }
        return l;
    }

    private void paintTabs(int on) {
        for (int k = 0; k < COUNT; k++) {
            int col = Ui.color(this, k == on ? R.color.accent : R.color.text_secondary);
            tabIcons[k].setState(k == on, col);
            names[k].setTextColor(col);
        }
    }

    private final class NavBar extends BlurView {
        private final float slop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        private float downX, downY;
        private boolean dragging;
        private int hover;

        NavBar(android.content.Context c) {
            super(c);
        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent e) {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downX = e.getX();
                    downY = e.getY();
                    dragging = false;
                    break;
                case MotionEvent.ACTION_MOVE:
                    float dx = Math.abs(e.getX() - downX);
                    if (!dragging && dx > slop && dx > Math.abs(e.getY() - downY) && pillView != null) {
                        dragging = true;
                        hover = cur;
                        pillView.animate().cancel();
                        pillView.animate().scaleX(1.12f).scaleY(1.1f).setDuration(180)
                                .setInterpolator(Motion.SPRING).start();
                        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                        getParent().requestDisallowInterceptTouchEvent(true);
                        return true;
                    }
                    break;
                default:
                    break;
            }
            return false;
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            if (!dragging) return super.onTouchEvent(e);
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_MOVE:
                    dragTo(e.getX());
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    dragging = false;
                    pillView.animate().scaleX(1f).scaleY(1f).setDuration(260).setInterpolator(Motion.SPRING).start();
                    int target = e.getActionMasked() == MotionEvent.ACTION_UP ? hover : cur;
                    if (target != cur) {
                        select(target);
                        Motion.bounce(icons[target]);
                    } else {
                        paintTabs(cur);
                        placePill(true);
                    }
                    return true;
                default:
                    return true;
            }
        }

        private void dragTo(float x) {
            float min = Float.MAX_VALUE, max = -Float.MAX_VALUE;
            for (View it : items) {
                min = Math.min(min, it.getLeft());
                max = Math.max(max, it.getLeft());
            }
            float w = pillView.getWidth();
            float want = Math.max(min, Math.min(max, x - getPaddingLeft() - w / 2f));
            float now = pillView.getTranslationX();
            pillView.setTranslationX(now + (want - now) * 0.6f);
            float center = pillView.getTranslationX() + w / 2f;
            int best = hover;
            float gap = Float.MAX_VALUE;
            for (int k = 0; k < COUNT; k++) {
                float d = Math.abs(items[k].getLeft() + items[k].getWidth() / 2f - center);
                if (d < gap) {
                    gap = d;
                    best = k;
                }
            }
            if (best != hover) {
                hover = best;
                paintTabs(hover);
                performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            }
        }
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
        }
        paintTabs(i);
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
            Glass.toast(this, getString(R.string.exported));
        } catch (Exception ex) {
            Glass.toast(this, getString(R.string.export_failed));
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
        Glass.toast(this, n > 0 ? getString(R.string.imported, n) : getString(R.string.import_failed));
        refreshCurrent();
    }
}
