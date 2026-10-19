package com.semo.calendar;

import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/** Edge-to-edge screens: system bars are transparent, content is padded by the insets, bar icons stay readable. */
public abstract class BaseActivity extends AppCompatActivity {
    /** Screens that rise over the app like an iOS modal sheet return true; the one below scales back. */
    protected boolean modal() {
        return false;
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
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
        return super.dispatchTouchEvent(e);
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
