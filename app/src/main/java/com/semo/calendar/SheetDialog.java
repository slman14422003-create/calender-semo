package com.semo.calendar;

import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.Context;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;

/**
 * Base of every dialog in the app. Bottom sheets rise with the iOS curve, can be dragged down by their top edge
 * and fly out when released with enough speed; centred alerts pop in and fade out; the dim behind fades with them.
 */
class SheetDialog extends Dialog {
    private static final float DIM = 0.4f;

    private View content;
    private boolean entered, closing, zone, dragging;
    private float downX, downY;
    private final int slop;
    private VelocityTracker tracker;
    private ValueAnimator dimAnim;

    SheetDialog(Context c) {
        super(c, R.style.Sheet);
        slop = ViewConfiguration.get(c).getScaledTouchSlop();
        Window w = getWindow();
        if (w != null) w.setDimAmount(0f);
    }

    @Override
    public void setContentView(View v) {
        super.setContentView(v);
        content = v;
        Window win = getWindow();
        if (win != null) win.setDimAmount(0f);
        v.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View x, int l, int t, int r, int b, int ol, int ot, int or, int ob) {
                if (entered || r - l <= 0 || b - t <= 0) return;
                entered = true;
                x.removeOnLayoutChangeListener(this);
                playEnter(x);
            }
        });
    }

    private boolean bottom() {
        Window w = getWindow();
        return w != null && (w.getAttributes().gravity & Gravity.VERTICAL_GRAVITY_MASK) == Gravity.BOTTOM;
    }

    private void dimTo(float to, long ms) {
        final Window w = getWindow();
        if (w == null) return;
        if (dimAnim != null) dimAnim.cancel();
        dimAnim = ValueAnimator.ofFloat(w.getAttributes().dimAmount, to);
        dimAnim.setDuration(ms);
        dimAnim.addUpdateListener(a -> w.setDimAmount((Float) a.getAnimatedValue()));
        dimAnim.start();
    }

    private void playEnter(View x) {
        if (bottom()) {
            x.setTranslationY(x.getHeight());
            x.animate().translationY(0f).setDuration(540).setInterpolator(Motion.OUT).start();
            dimTo(DIM, 360);
        } else {
            x.setAlpha(0f);
            x.setScaleX(1.14f);
            x.setScaleY(1.14f);
            x.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(320).setInterpolator(Motion.ALERT).start();
            dimTo(DIM, 240);
        }
    }

    @Override
    public void dismiss() {
        if (closing) return;
        if (content == null || !entered || !isShowing()) {
            super.dismiss();
            return;
        }
        closing = true;
        final View x = content;
        x.animate().cancel();
        if (bottom()) {
            x.animate().translationY(x.getHeight()).setDuration(300).setInterpolator(Motion.IN)
                    .withEndAction(this::finishDismiss).start();
            dimTo(0f, 300);
        } else {
            x.animate().alpha(0f).scaleX(0.95f).scaleY(0.95f).setDuration(170).setInterpolator(Motion.IN)
                    .withEndAction(this::finishDismiss).start();
            dimTo(0f, 170);
        }
    }

    private void finishDismiss() {
        try {
            super.dismiss();
        } catch (RuntimeException ignored) {
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent e) {
        Window w = getWindow();
        if (w != null) PressFx.dispatch(w.getDecorView(), e);
        if (content != null && entered && !closing && bottom() && handleDrag(e)) return true;
        return super.dispatchTouchEvent(e);
    }

    private boolean handleDrag(MotionEvent e) {
        int a = e.getActionMasked();
        if (a == MotionEvent.ACTION_DOWN) {
            dragging = false;
            int[] loc = new int[2];
            content.getLocationOnScreen(loc);
            float y = e.getRawY();
            zone = y >= loc[1] && y <= loc[1] + Ui.dp(getContext(), 64);
            if (tracker != null) tracker.recycle();
            tracker = null;
            if (zone) {
                downX = e.getRawX();
                downY = e.getRawY();
                tracker = VelocityTracker.obtain();
                tracker.addMovement(e);
            }
            return false;
        }
        if (!zone || tracker == null) return false;
        tracker.addMovement(e);
        if (a == MotionEvent.ACTION_MOVE) {
            float dy = e.getRawY() - downY;
            if (!dragging && dy > slop && dy > Math.abs(e.getRawX() - downX)) {
                dragging = true;
                MotionEvent c = MotionEvent.obtain(e);
                c.setAction(MotionEvent.ACTION_CANCEL);
                super.dispatchTouchEvent(c);
                c.recycle();
            }
            if (dragging) {
                float ty = Math.max(0f, dy - slop);
                content.setTranslationY(ty);
                Window w = getWindow();
                if (w != null) {
                    float p = Math.min(1f, ty / Math.max(1, content.getHeight()));
                    w.setDimAmount(DIM * (1f - p));
                }
                return true;
            }
            return false;
        }
        if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) {
            boolean was = dragging;
            zone = false;
            dragging = false;
            tracker.computeCurrentVelocity(1000);
            float vy = tracker.getYVelocity();
            tracker.recycle();
            tracker = null;
            if (was) {
                float ty = content.getTranslationY();
                if (a == MotionEvent.ACTION_UP && (ty > content.getHeight() * 0.3f || vy > 1100f)) {
                    dismiss();
                } else {
                    content.animate().translationY(0f).setDuration(420).setInterpolator(Motion.SPRING).start();
                    dimTo(DIM, 240);
                }
            }
            return was;
        }
        return false;
    }
}
