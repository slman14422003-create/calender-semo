package com.semo.calendar;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;

import androidx.appcompat.widget.SwitchCompat;

/** The iOS switch: the thumb glides (SwitchCompat) while the track fades from grey to green. */
final class IosSwitch extends SwitchCompat {
    private final GradientDrawable track = new GradientDrawable();
    private final int off, on;
    private float p;
    private ValueAnimator anim;

    IosSwitch(Context c) {
        super(c);
        off = c.getColor(R.color.surface_high);
        on = c.getColor(R.color.ok);
        track.setCornerRadius(Ui.dp(c, 16));
        track.setSize(Ui.dp(c, 51), Ui.dp(c, 31));
        track.setColor(off);
        setThumbDrawable(c.getDrawable(R.drawable.switch_thumb));
        setTrackDrawable(track);
        setSplitTrack(false);
    }

    private static int mix(int a, int b, float t) {
        int r = Math.round(Color.red(a) + (Color.red(b) - Color.red(a)) * t);
        int g = Math.round(Color.green(a) + (Color.green(b) - Color.green(a)) * t);
        int bl = Math.round(Color.blue(a) + (Color.blue(b) - Color.blue(a)) * t);
        return Color.rgb(r, g, bl);
    }

    private void paint() {
        track.setColor(mix(off, on, p));
        invalidate();
    }

    @Override
    public void setChecked(boolean checked) {
        boolean was = isChecked();
        super.setChecked(checked);
        if (track == null) return;
        float target = checked ? 1f : 0f;
        if (anim != null) anim.cancel();
        if (!isLaidOut() || was == checked) {
            p = target;
            paint();
            return;
        }
        anim = ValueAnimator.ofFloat(p, target);
        anim.setDuration(260);
        anim.setInterpolator(Motion.DECEL);
        anim.addUpdateListener(a -> {
            p = (Float) a.getAnimatedValue();
            paint();
        });
        anim.start();
    }
}
