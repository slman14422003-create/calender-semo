package com.semo.calendar;

import android.animation.LayoutTransition;
import android.animation.TimeInterpolator;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.PathInterpolator;

/** The motion vocabulary of the app: iOS-like curves and a damped spring, shared by every animation. */
final class Motion {
    private Motion() {
    }

    /** Fast start, long soft landing: the curve iOS uses for sheets and pushes. */
    static final TimeInterpolator OUT = new PathInterpolator(0.32f, 0.72f, 0f, 1f);
    /** Used when something leaves the screen. */
    static final TimeInterpolator IN = new PathInterpolator(0.4f, 0f, 1f, 1f);
    /** Alert pop: quick, settles without bouncing. */
    static final TimeInterpolator ALERT = new PathInterpolator(0.2f, 0.9f, 0.3f, 1f);
    static final TimeInterpolator DECEL = new DecelerateInterpolator(1.6f);
    /** Damped spring with a small overshoot, for buttons, icons and selection thumbs. */
    static final TimeInterpolator SPRING = new Spring(6.5f, 1f);
    /** Softer spring with a very small overshoot. */
    static final TimeInterpolator SPRING_SOFT = new Spring(8f, 1f);

    private static String pendingToggle;

    static final class Spring implements TimeInterpolator {
        private final float damping;
        private final float cycles;

        Spring(float damping, float cycles) {
            this.damping = damping;
            this.cycles = cycles;
        }

        @Override
        public float getInterpolation(float t) {
            if (t >= 1f) return 1f;
            return (float) (1.0 - Math.exp(-damping * t) * Math.cos(2.0 * Math.PI * cycles * t));
        }
    }

    /** A screen or a block arriving: fades in while rising a few dp. */
    static void enter(View v, int riseDp) {
        v.animate().cancel();
        v.setAlpha(0f);
        v.setTranslationY(Ui.dp(v.getContext(), riseDp));
        v.animate().alpha(1f).translationY(0f).setDuration(340).setInterpolator(OUT).setUpdateListener(null).start();
    }

    /** Content swapped under a stable header (a new day in the list). */
    static void swap(View v) {
        v.animate().cancel();
        v.setAlpha(0.25f);
        v.setTranslationY(Ui.dp(v.getContext(), 8));
        v.animate().alpha(1f).translationY(0f).setDuration(280).setInterpolator(OUT).setUpdateListener(null).start();
    }

    /** SF Symbols style "bounce" for a selected icon. */
    static void bounce(final View v) {
        v.animate().cancel();
        v.setScaleX(0.78f);
        v.setScaleY(0.78f);
        v.animate().scaleX(1f).scaleY(1f).setDuration(520).setInterpolator(SPRING).setUpdateListener(null).start();
    }

    /** A control appearing in its new state (a ticked circle). */
    static void pop(final View v) {
        v.animate().cancel();
        v.setScaleX(0.55f);
        v.setScaleY(0.55f);
        v.animate().scaleX(1f).scaleY(1f).setDuration(460).setInterpolator(SPRING).setUpdateListener(null).start();
    }

    static void markToggled(String id) {
        pendingToggle = id;
    }

    /** True once for the item that was just toggled, so its new row can play {@link #pop}. */
    static boolean takeToggled(String id) {
        if (id != null && id.equals(pendingToggle)) {
            pendingToggle = null;
            return true;
        }
        return false;
    }

    /** Rows appearing, disappearing or moving inside a container glide instead of jumping. */
    static LayoutTransition layout() {
        LayoutTransition t = new LayoutTransition();
        t.enableTransitionType(LayoutTransition.CHANGING);
        t.setDuration(320);
        int[] types = {LayoutTransition.APPEARING, LayoutTransition.DISAPPEARING,
                LayoutTransition.CHANGE_APPEARING, LayoutTransition.CHANGE_DISAPPEARING, LayoutTransition.CHANGING};
        for (int k : types) {
            t.setInterpolator(k, OUT);
            t.setStartDelay(k, 0);
        }
        t.setDuration(LayoutTransition.DISAPPEARING, 180);
        t.setStartDelay(LayoutTransition.APPEARING, 90);
        return t;
    }

    /** Applies {@link #layout()} to a container and everything inside it that is a vertical group. */
    static void animateChanges(android.view.ViewGroup g) {
        g.setLayoutTransition(layout());
    }
}
