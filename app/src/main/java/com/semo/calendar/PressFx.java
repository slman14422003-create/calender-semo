package com.semo.calendar;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * Touch feedback in the iOS manner, applied to the whole app from one place.
 * <ul>
 * <li>Small icon-like buttons shrink and dim while pressed and spring back on release.</li>
 * <li>Text buttons (Cancel, Save, Done ...) dim like iOS navigation bar buttons.</li>
 * <li>List rows turn grey after a short delay (so scrolling does not flash them) and fade out after release.</li>
 * </ul>
 * Activities and dialogs forward their touch events to {@link #dispatch}; views that need something special call
 * {@link Ui#press} or {@link Ui#highlight}, which mark them so the global handler leaves them alone.
 */
final class PressFx {
    private PressFx() {
    }

    private static final int SCALE = 1, DIM = 2;
    private static final Handler H = new Handler(Looper.getMainLooper());
    private static final int[] LOC = new int[2];
    private static View active;
    private static float downX, downY;
    private static long downAt;

    // ---- global handler ----

    static void dispatch(View decor, MotionEvent e) {
        if (decor == null) return;
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                release();
                View t = find(decor, e.getRawX(), e.getRawY());
                int mode = t == null ? 0 : modeOf(t);
                if (mode != 0) {
                    active = t;
                    downX = e.getRawX();
                    downY = e.getRawY();
                    downAt = SystemClock.uptimeMillis();
                    down(t, mode == SCALE);
                }
                break;
            }
            case MotionEvent.ACTION_MOVE:
                if (active != null) {
                    float s = ViewConfiguration.get(active.getContext()).getScaledTouchSlop() * 2f;
                    if (Math.abs(e.getRawX() - downX) > s || Math.abs(e.getRawY() - downY) > s) release();
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                release();
                break;
            default:
                break;
        }
    }

    private static void release() {
        final View v = active;
        if (v == null) return;
        active = null;
        long wait = 80 - (SystemClock.uptimeMillis() - downAt);
        if (wait > 0) H.postDelayed(() -> up(v), wait);
        else up(v);
    }

    private static View find(View v, float rx, float ry) {
        if (v.getVisibility() != View.VISIBLE) return null;
        v.getLocationOnScreen(LOC);
        if (rx < LOC[0] || rx >= LOC[0] + v.getWidth() || ry < LOC[1] || ry >= LOC[1] + v.getHeight()) return null;
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = g.getChildCount() - 1; i >= 0; i--) {
                View r = find(g.getChildAt(i), rx, ry);
                if (r != null) return r;
            }
        }
        return v.isClickable() ? v : null;
    }

    private static int modeOf(View t) {
        if (t.getTag(R.id.fx_skip) != null || !t.isEnabled()) return 0;
        if (t instanceof EditText || t instanceof CompoundButton || t instanceof SeekBar) return 0;
        int lim = Ui.dp(t.getContext(), 72);
        boolean small = t.getWidth() <= lim && t.getHeight() <= lim;
        if (t instanceof TextView) {
            return small && ((TextView) t).getText().length() <= 2 ? SCALE : DIM;
        }
        return small ? SCALE : 0;
    }

    // ---- buttons ----

    static void down(View v, boolean scale) {
        v.animate().cancel();
        if (v.getTag(R.id.fx_alpha) == null) v.setTag(R.id.fx_alpha, v.getAlpha());
        float base = (Float) v.getTag(R.id.fx_alpha);
        if (scale) {
            float s = Math.max(v.getWidth(), v.getHeight()) > Ui.dp(v.getContext(), 120) ? 0.97f : 0.88f;
            v.animate().scaleX(s).scaleY(s).alpha(base * 0.7f).setDuration(110).setInterpolator(Motion.DECEL)
                    .setUpdateListener(null).start();
        } else {
            v.animate().alpha(base * 0.3f).setDuration(70).setInterpolator(Motion.DECEL)
                    .setUpdateListener(null).start();
        }
    }

    static void up(final View v) {
        Object o = v.getTag(R.id.fx_alpha);
        final float base = o instanceof Float ? (Float) o : 1f;
        v.setTag(R.id.fx_alpha, null);
        v.animate().cancel();
        boolean scaled = v.getScaleX() != 1f || v.getScaleY() != 1f;
        if (scaled) {
            v.animate().scaleX(1f).scaleY(1f).alpha(base).setDuration(460).setInterpolator(Motion.SPRING)
                    .setUpdateListener(a -> {
                        if (v.getAlpha() > base) v.setAlpha(base);
                    })
                    .withEndAction(() -> {
                        v.setAlpha(base);
                        v.animate().setUpdateListener(null);
                    }).start();
        } else {
            v.animate().alpha(base).setDuration(260).setInterpolator(Motion.DECEL).setUpdateListener(null).start();
        }
    }

    // ---- list rows ----

    private static Drawable rowDrawable(View x) {
        Object t = x.getTag();
        return t instanceof Drawable ? (Drawable) t
                : new ColorDrawable(x.getContext().getColor(R.color.cell_highlight));
    }

    private static void cancelRow(View x) {
        Object r = x.getTag(R.id.fx_run);
        if (r instanceof Runnable) H.removeCallbacks((Runnable) r);
        x.setTag(R.id.fx_run, null);
        Object a = x.getTag(R.id.fx_anim);
        if (a instanceof ValueAnimator) ((ValueAnimator) a).cancel();
        x.setTag(R.id.fx_anim, null);
    }

    private static void showRow(View x) {
        Drawable d = rowDrawable(x);
        d.setAlpha(255);
        x.setForeground(d);
    }

    @SuppressLint("ClickableViewAccessibility")
    static void rowTouch(final View x, MotionEvent e) {
        int a = e.getActionMasked();
        if (a == MotionEvent.ACTION_DOWN) {
            cancelRow(x);
            Runnable show = () -> {
                x.setTag(R.id.fx_run, null);
                showRow(x);
            };
            x.setTag(R.id.fx_run, show);
            H.postDelayed(show, ViewConfiguration.getTapTimeout());
        } else if (a == MotionEvent.ACTION_UP) {
            boolean shown = x.getTag(R.id.fx_run) == null;
            cancelRow(x);
            if (!shown) showRow(x);
            fadeRow(x);
        } else if (a == MotionEvent.ACTION_CANCEL) {
            boolean shown = x.getTag(R.id.fx_run) == null;
            cancelRow(x);
            if (shown) fadeRow(x);
        }
    }

    private static void fadeRow(final View x) {
        final Drawable d = x.getForeground();
        if (d == null) return;
        ValueAnimator va = ValueAnimator.ofInt(255, 0);
        va.setDuration(320);
        va.setStartDelay(40);
        va.setInterpolator(Motion.DECEL);
        va.addUpdateListener(an -> {
            d.setAlpha((Integer) an.getAnimatedValue());
            x.invalidate();
        });
        va.addListener(new android.animation.AnimatorListenerAdapter() {
            private boolean cancelled;

            @Override
            public void onAnimationCancel(android.animation.Animator animation) {
                cancelled = true;
            }

            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (!cancelled) {
                    x.setForeground(null);
                    d.setAlpha(255);
                }
            }
        });
        x.setTag(R.id.fx_anim, va);
        va.start();
    }
}
