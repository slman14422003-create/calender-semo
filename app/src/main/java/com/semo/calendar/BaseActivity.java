package com.semo.calendar;

import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import eightbitlab.com.blurview.BlurTarget;

/** Edge-to-edge screens: system bars are transparent, content is padded by the insets, bar icons stay readable. */
public abstract class BaseActivity extends AppCompatActivity {
    private FrameLayout shell;
    private BlurTarget blurTarget;
    private LongPress longPress;

    /** Window-wide container: the blurred-on-long-press content plus the layer where the context menu lives. */
    final FrameLayout shell() {
        return shell;
    }

    /** Everything the screen draws; the long-press blur (and any glass bar) samples it. */
    final BlurTarget blurTarget() {
        return blurTarget;
    }

    private void wrap(View v) {
        shell = new FrameLayout(this);
        blurTarget = new BlurTarget(this);
        if (v.getParent() instanceof ViewGroup) ((ViewGroup) v.getParent()).removeView(v);
        blurTarget.addView(v, new FrameLayout.LayoutParams(-1, -1));
        shell.addView(blurTarget, new FrameLayout.LayoutParams(-1, -1));
        super.setContentView(shell);
    }

    @Override
    public void setContentView(View v) {
        wrap(v);
    }

    @Override
    public void setContentView(View v, ViewGroup.LayoutParams p) {
        wrap(v);
    }

    @Override
    public void setContentView(int id) {
        wrap(getLayoutInflater().inflate(id, null));
    }

    /** Screens that rise over the app like an iOS modal sheet return true; the one below scales back. */
    protected boolean modal() {
        return false;
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        longPress = new LongPress(this);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        if (modal()) {
            if (Build.VERSION.SDK_INT >= 34) {
                overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, R.anim.modal_in, R.anim.modal_under);
                overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, R.anim.modal_back, R.anim.modal_out);
            } else {
                overridePendingTransition(R.anim.modal_in, R.anim.modal_under);
            }
        }
    }

    @Override
    public void finish() {
        super.finish();
        if (modal() && Build.VERSION.SDK_INT < 34) overridePendingTransition(R.anim.modal_back, R.anim.modal_out);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent e) {
        PressFx.dispatch(getWindow().getDecorView(), e);
        if (longPress != null && longPress.onTouch(e)) return true;
        return super.dispatchTouchEvent(e);
    }

    /** Sends an event straight to the views (used to cancel the touch that started a long press). */
    final void dispatchRaw(MotionEvent e) {
        super.dispatchTouchEvent(e);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (longPress != null) longPress.release();
    }

    /** Same as {@link #applyInsets(View)}, callable from helper classes in this package. */
    final void applyInsetsPublic(View root) {
        applyInsets(root);
    }

    protected void applyInsets(View root) {
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, w) -> {
            Insets i = w.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.ime());
            v.setPadding(i.left, i.top, i.right, i.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    @Override
    protected void onPostCreate(Bundle b) {
        super.onPostCreate(b);
        boolean night = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        WindowInsetsControllerCompat c = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        c.setAppearanceLightStatusBars(!night);
        c.setAppearanceLightNavigationBars(!night);
    }
}
