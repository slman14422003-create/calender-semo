package com.semo.calendar;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.util.Locale;

/** Calendar-only updates: checks this project's GitHub releases, downloads the APK and installs it without leaving the app. */
public final class UpdateActivity extends BaseActivity {
    private static final int M_CHECK = 0, M_DOWNLOAD = 1, M_CANCEL = 2, M_INSTALL = 3;

    private TextView status, sub, btn, notes;
    private ProgressBar bar;
    private LinearLayout notesBox;
    private Updater.Info info;
    private Updater.Job job;
    private File ready;
    private int mode = M_CHECK;
    private boolean checking;

    @Override
    protected boolean modal() {
        return true;
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout body = Pages.frame(this, getString(R.string.upd_title));

        LinearLayout about = Ui.group(this, 8);
        LinearLayout cur = Ui.cell(this, getString(R.string.app_name),
                Ui.tile(this, R.drawable.ic_month, Ui.color(this, R.color.accent)));
        cur.addView(Ui.text(this, getString(R.string.upd_current, BuildConfig.VERSION_NAME), 15, R.color.text_secondary, false));
        about.addView(cur);
        body.addView(about);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setPadding(Ui.dp(this, 20), Ui.dp(this, 22), Ui.dp(this, 20), Ui.dp(this, 20));
        box.setBackground(Ui.shape(Ui.color(this, R.color.sheet_cell), Ui.dp(this, 14)));
        status = Ui.text(this, "", 19, R.color.text_primary, false);
        status.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        status.setGravity(Gravity.CENTER);
        box.addView(status, new LinearLayout.LayoutParams(-1, -2));
        sub = Ui.text(this, "", 14, R.color.text_secondary, false);
        sub.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams sl = new LinearLayout.LayoutParams(-1, -2);
        sl.topMargin = Ui.dp(this, 6);
        box.addView(sub, sl);
        bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(1000);
        bar.setProgressTintList(android.content.res.ColorStateList.valueOf(Ui.color(this, R.color.accent)));
        bar.setVisibility(View.GONE);
        LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(-1, Ui.dp(this, 6));
        bl.topMargin = Ui.dp(this, 16);
        box.addView(bar, bl);
        btn = new TextView(this);
        btn.setGravity(Gravity.CENTER);
        btn.setTextSize(17);
        btn.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        btn.setTextColor(0xFFFFFFFF);
        btn.setBackground(Ui.shape(Ui.color(this, R.color.accent), Ui.dp(this, 12)));
        btn.setPadding(Ui.dp(this, 22), 0, Ui.dp(this, 22), 0);
        btn.setOnClickListener(v -> onButton());
        Ui.press(btn);
        LinearLayout.LayoutParams btl = new LinearLayout.LayoutParams(-1, Ui.dp(this, 50));
        btl.topMargin = Ui.dp(this, 18);
        box.addView(btn, btl);
        LinearLayout.LayoutParams boxLp = new LinearLayout.LayoutParams(-1, -2);
        boxLp.setMargins(Ui.dp(this, 16), Ui.dp(this, 20), Ui.dp(this, 16), 0);
        body.addView(box, boxLp);

        notesBox = Ui.vertical(this);
        notesBox.setVisibility(View.GONE);
        notesBox.addView(Ui.sectionHeader(this, getString(R.string.upd_notes)));
        LinearLayout ng = Ui.group(this, 0);
        notes = Ui.text(this, "", 15, R.color.text_primary, false);
        notes.setPadding(Ui.dp(this, 16), Ui.dp(this, 14), Ui.dp(this, 16), Ui.dp(this, 14));
        notes.setLineSpacing(0f, 1.2f);
        ng.addView(notes, new LinearLayout.LayoutParams(-1, -2));
        notesBox.addView(ng);
        body.addView(notesBox);

        LinearLayout opt = Ui.group(this, 24);
        LinearLayout autoRow = Ui.cell(this, getString(R.string.upd_auto));
        autoRow.addView(Ui.iosSwitch(this, Updater.auto(this), (v, on) -> Updater.setAuto(this, on)));
        opt.addView(autoRow);
        opt.addView(Ui.hairline(this, 16));
        LinearLayout page = Ui.cell(this, getString(R.string.upd_open_page));
        page.addView(Ui.chevron(this));
        page.setOnClickListener(v -> Pages.open(this, Updater.PAGE));
        Ui.highlight(page);
        opt.addView(page);
        body.addView(opt);
        body.addView(Pages.footnote(this, getString(R.string.upd_footnote)));

        Motion.enter(box, 18);
        doCheck();
    }

    private void setMode(int m, int labelRes) {
        mode = m;
        btn.setText(labelRes);
        btn.setAlpha(1f);
        btn.setEnabled(true);
    }

    private void doCheck() {
        if (checking) return;
        checking = true;
        bar.setVisibility(View.GONE);
        status.setText(R.string.upd_checking);
        sub.setText("");
        btn.setEnabled(false);
        btn.setAlpha(0.5f);
        btn.setText(R.string.upd_checking);
        Updater.check(this, (i, err) -> {
            checking = false;
            if (isFinishing() || isDestroyed()) return;
            if (i == null) {
                info = null;
                status.setText(err == 2 ? R.string.upd_err_none : R.string.upd_err_net);
                sub.setText("");
                setMode(M_CHECK, R.string.upd_check);
                return;
            }
            info = i;
            if (!i.newer) {
                Updater.clean(this);
                status.setText(R.string.upd_latest);
                sub.setText(getString(R.string.upd_current, BuildConfig.VERSION_NAME));
                setMode(M_CHECK, R.string.upd_check);
                return;
            }
            status.setText(getString(R.string.upd_available, i.version));
            if (!i.notes.isEmpty()) {
                notes.setText(i.notes);
                notesBox.setVisibility(View.VISIBLE);
                Motion.enter(notesBox, 12);
            }
            if (i.apkUrl == null) {
                sub.setText(R.string.upd_no_apk);
                setMode(M_CHECK, R.string.upd_check);
                return;
            }
            sub.setText(i.size > 0 ? getString(R.string.upd_size, String.format(Locale.US, "%.1f MB", i.size / 1048576.0)) : "");
            setMode(M_DOWNLOAD, R.string.upd_download);
        });
    }

    private void onButton() {
        if (mode == M_CHECK) {
            doCheck();
        } else if (mode == M_DOWNLOAD) {
            startDownload();
        } else if (mode == M_CANCEL) {
            if (job != null) job.cancel();
        } else if (mode == M_INSTALL) {
            install();
        }
    }

    private void startDownload() {
        if (info == null || info.apkUrl == null) return;
        bar.setProgress(0);
        bar.setVisibility(View.VISIBLE);
        status.setText(R.string.upd_downloading);
        setMode(M_CANCEL, R.string.upd_cancel);
        job = Updater.download(this, info, (done, total) -> {
            if (isFinishing() || isDestroyed()) return;
            int per = total > 0 ? (int) (done * 1000L / total) : 0;
            bar.setProgress(per);
            sub.setText(total > 0 ? getString(R.string.upd_progress, per / 10) : "");
        }, (f, err) -> {
            job = null;
            if (isFinishing() || isDestroyed()) return;
            bar.setVisibility(View.GONE);
            if (f != null) {
                ready = f;
                status.setText(R.string.upd_ready);
                sub.setText(R.string.upd_ready_sub);
                setMode(M_INSTALL, R.string.upd_install);
                Motion.pop(status);
                install();
            } else {
                status.setText(err == 3 ? R.string.upd_cancelled : err == 2 ? R.string.upd_err_verify : R.string.upd_err_net);
                sub.setText("");
                setMode(M_DOWNLOAD, R.string.upd_download);
            }
        });
    }

    private void install() {
        if (ready == null || !ready.exists()) {
            setMode(M_DOWNLOAD, R.string.upd_download);
            return;
        }
        if (!Updater.canInstall(this)) {
            Sheets.alert(this, getString(R.string.upd_perm_title), getString(R.string.upd_perm_msg),
                    getString(R.string.upd_perm_open), false, () -> Updater.openInstallPermission(this));
            return;
        }
        Updater.install(this, ready);
    }

    @Override
    protected void onDestroy() {
        if (job != null) job.cancel();
        super.onDestroy();
    }
}
