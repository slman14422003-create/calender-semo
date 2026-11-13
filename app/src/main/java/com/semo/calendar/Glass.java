package com.semo.calendar;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import eightbitlab.com.blurview.BlurView;

/**
 * Liquid Glass, the same material as Clock Studio: translucent cards with a hairline edge, a blurred floating tab
 * bar and glass toasts. One switch in Settings turns it off and every surface falls back to the solid iOS colours.
 */
final class Glass {
    private Glass() {
    }

    static boolean enabled(Context c) {
        return Prefs.glass(c);
    }

    static boolean night(Context c) {
        return (c.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
    }

    /** Blur radius by the strength chosen in Settings (the library multiplies it by its scale factor). */
    static float blurRadius(Context c) {
        int l = Prefs.glassLevel(c);
        return l <= 0 ? 7f : l == 1 ? 11f : 15f;
    }

    static Activity activity(Context c) {
        while (c instanceof ContextWrapper) {
            if (c instanceof Activity) return (Activity) c;
            c = ((ContextWrapper) c).getBaseContext();
        }
        return null;
    }

    /** Fill of floating glass (tab bar, toast): light when there is a real blur behind it, denser when not. */
    static int panelFill(Context c) {
        boolean night = night(c);
        if (enabled(c)) return night ? 0xB81C1C1E : 0xB8F9F9F9;
        return night ? 0xF01C1C1E : 0xF2FFFFFF;
    }

    /** Fill of cards and grouped rows: translucent glass, or the solid iOS surface when Glass is off. */
    static int surface(Context c) {
        return Ui.color(c, enabled(c) ? R.color.glass_surface : R.color.surface);
    }

    /** A card on the backdrop (no blur of its own: cards live inside the blurred content). */
    static Drawable card(Context c, float radiusDp) {
        return card(c, radiusDp, surface(c));
    }

    static Drawable card(Context c, float radiusDp, int fill) {
        if (!enabled(c)) return Ui.shape(fill, Ui.dpf(c, radiusDp));
        return new GlassDrawable(c, Ui.dpf(c, radiusDp), fill, 0.55f);
    }

    /** Round glass button background (header buttons). */
    static Drawable circle(Context c) {
        int fill = Ui.color(c, enabled(c) ? R.color.glass_field : R.color.field);
        Drawable d = enabled(c) ? new GlassDrawable(c, Ui.dpf(c, 100), fill, 0.8f) : Ui.shape(fill, Ui.dpf(c, 100));
        return new InsetDrawable(d, Ui.dp(c, 4));
    }

    static int dim(int color, float k) {
        return Color.argb(Math.round(Color.alpha(color) * k), Color.red(color), Color.green(color), Color.blue(color));
    }

    // ------------------------------------------------------------------ glass toast

    private static View lastToast;

    /** Capsule message in the iOS 26 style; falls back to the system toast outside a main screen. */
    static void toast(Context c, CharSequence text) {
        Activity a = activity(c);
        FrameLayout shell = a instanceof BaseActivity ? ((BaseActivity) a).shell() : null;
        if (shell == null || a.isFinishing() || Looper.myLooper() != Looper.getMainLooper()) {
            android.widget.Toast.makeText(c, text, android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        if (lastToast != null && lastToast.getParent() instanceof ViewGroup) {
            ((ViewGroup) lastToast.getParent()).removeView(lastToast);
        }
        final BlurView pill = new BlurView(a);
        pill.setBackground(new GlassDrawable(a, Ui.dpf(a, 100), panelFill(a), 1f));
        pill.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
        pill.setClipToOutline(true);
        if (enabled(a)) {
            try {
                pill.setupWith(((BaseActivity) a).blurTarget()).setBlurRadius(blurRadius(a));
            } catch (Throwable ignored) {
            }
        }
        TextView t = Ui.text(a, text, 15, R.color.text_primary, true);
        t.setGravity(Gravity.CENTER);
        t.setMaxLines(3);
        pill.addView(t, new FrameLayout.LayoutParams(-2, -2));
        pill.setPadding(Ui.dp(a, 20), Ui.dp(a, 12), Ui.dp(a, 20), Ui.dp(a, 12));
        int bottom = Ui.dp(a, 96);
        WindowInsetsCompat wi = ViewCompat.getRootWindowInsets(a.getWindow().getDecorView());
        if (wi != null) bottom += wi.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        lp.bottomMargin = bottom;
        lp.leftMargin = lp.rightMargin = Ui.dp(a, 24);
        shell.addView(pill, lp);
        lastToast = pill;
        pill.setAlpha(0f);
        pill.setScaleX(0.85f);
        pill.setScaleY(0.85f);
        pill.setTranslationY(Ui.dp(a, 20));
        pill.animate().alpha(1f).scaleX(1f).scaleY(1f).translationY(0f).setDuration(480)
                .setInterpolator(Motion.SPRING).start();
        pill.postDelayed(() -> {
            if (pill.getParent() == null) return;
            pill.animate().alpha(0f).scaleX(0.92f).scaleY(0.92f).setDuration(220).setInterpolator(Motion.IN)
                    .withEndAction(() -> {
                        if (pill.getParent() instanceof ViewGroup) ((ViewGroup) pill.getParent()).removeView(pill);
                        if (lastToast == pill) lastToast = null;
                    }).start();
        }, 2300);
    }
}
