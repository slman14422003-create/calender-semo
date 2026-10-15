package com.semo.calendar;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Tiny view builders shared by every screen: the iOS look (grouped cards, hairlines, capsules). */
final class Ui {
    private Ui() {
    }

    static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    static float sp(Context c, float v) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, c.getResources().getDisplayMetrics());
    }

    static int color(Context c, int res) {
        return c.getColor(res);
    }

    static GradientDrawable shape(int color, float radiusPx) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radiusPx);
        return g;
    }

    static GradientDrawable topRounded(int color, float r) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
        return g;
    }

    static TextView text(Context c, CharSequence t, float sp, int colorRes, boolean medium) {
        TextView v = new TextView(c);
        v.setText(t);
        v.setTextSize(sp);
        v.setTextColor(color(c, colorRes));
        v.setTypeface(medium ? Typeface.create("sans-serif-medium", Typeface.NORMAL) : Typeface.DEFAULT);
        return v;
    }

    /** A soft press: the view shrinks slightly while a finger is on it. */
    @android.annotation.SuppressLint("ClickableViewAccessibility")
    static void press(View v) {
        v.setOnTouchListener((x, e) -> {
            int a = e.getActionMasked();
            if (a == MotionEvent.ACTION_DOWN) x.animate().scaleX(0.97f).scaleY(0.97f).setDuration(90).start();
            else if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL)
                x.animate().scaleX(1f).scaleY(1f).setDuration(140).start();
            return false;
        });
    }

    static LinearLayout vertical(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    /** iOS inset-grouped card: white (night: dark grey) rounded block that holds rows separated by hairlines. */
    static LinearLayout group(Context c) {
        LinearLayout l = vertical(c);
        l.setBackground(shape(color(c, R.color.surface), dp(c, 14)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(dp(c, 16), dp(c, 16), dp(c, 16), 0);
        l.setLayoutParams(lp);
        return l;
    }

    static View hairline(Context c, int startDp) {
        View v = new View(c);
        v.setBackgroundColor(color(c, R.color.stroke));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, Math.max(1, dp(c, 0.5f)));
        lp.setMarginStart(dp(c, startDp));
        v.setLayoutParams(lp);
        return v;
    }

    static android.widget.ImageButton iconButton(Context c, int res, int tintRes, int sizeDp) {
        android.widget.ImageButton b = new android.widget.ImageButton(c);
        b.setImageResource(res);
        b.setImageTintList(android.content.res.ColorStateList.valueOf(color(c, tintRes)));
        b.setBackground(null);
        b.setScaleType(android.widget.ImageView.ScaleType.CENTER);
        b.setLayoutParams(new LinearLayout.LayoutParams(dp(c, sizeDp), dp(c, sizeDp)));
        press(b);
        return b;
    }
}
