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

    /** iOS inset-grouped card: rounded 10, rows separated by hairlines. */
    static LinearLayout group(Context c) {
        return group(c, 16);
    }

    static LinearLayout group(Context c, int topDp) {
        return group(c, topDp, R.color.surface);
    }

    static LinearLayout group(Context c, int topDp, int fillRes) {
        LinearLayout l = vertical(c);
        l.setBackground(shape(color(c, fillRes), dp(c, 10)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(dp(c, 16), dp(c, topDp), dp(c, 16), 0);
        l.setLayoutParams(lp);
        return l;
    }

    /** Footnote-size section title above a group (upper case like iOS). */
    static TextView sectionHeader(Context c, CharSequence t) {
        TextView v = text(c, t.toString().toUpperCase(java.util.Locale.getDefault()), 13, R.color.text_secondary, false);
        v.setPadding(dp(c, 32), dp(c, 24), dp(c, 32), dp(c, 6));
        return v;
    }

    /** Disclosure chevron shown at the end of rows that open a choice. */
    static android.widget.ImageView chevron(Context c) {
        android.widget.ImageView i = new android.widget.ImageView(c);
        i.setImageResource(R.drawable.ic_chev_next);
        i.setImageTintList(android.content.res.ColorStateList.valueOf(color(c, R.color.text_hint)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(c, 16), dp(c, 16));
        lp.setMarginStart(dp(c, 6));
        i.setLayoutParams(lp);
        return i;
    }

    /** One 44dp cell: label on the leading side; the caller adds value / control views at the end. */
    static LinearLayout cell(Context c, CharSequence label) {
        return cell(c, label, null);
    }

    /** Coloured rounded-square icon tile (30dp) like the ones in iOS Settings. */
    static View tile(Context c, int iconRes, int colorInt) {
        android.widget.FrameLayout f = new android.widget.FrameLayout(c);
        f.setBackground(shape(colorInt, dp(c, 7)));
        android.widget.ImageView i = new android.widget.ImageView(c);
        i.setImageResource(iconRes);
        i.setImageTintList(android.content.res.ColorStateList.valueOf(0xFFFFFFFF));
        f.addView(i, new android.widget.FrameLayout.LayoutParams(dp(c, 18), dp(c, 18), android.view.Gravity.CENTER));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(c, 30), dp(c, 30));
        lp.setMarginEnd(dp(c, 12));
        f.setLayoutParams(lp);
        return f;
    }

    /** An app launcher icon clipped to a 30dp rounded square. */
    static View appIcon(final Context c, android.graphics.drawable.Drawable d) {
        android.widget.ImageView i = new android.widget.ImageView(c);
        i.setImageDrawable(d);
        i.setClipToOutline(true);
        i.setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override
            public void getOutline(View v, android.graphics.Outline o) {
                o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), dp(c, 7));
            }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(c, 30), dp(c, 30));
        lp.setMarginEnd(dp(c, 12));
        i.setLayoutParams(lp);
        return i;
    }

    static LinearLayout cell(Context c, CharSequence label, View leading) {
        LinearLayout r = new LinearLayout(c);
        r.setGravity(android.view.Gravity.CENTER_VERTICAL);
        r.setMinimumHeight(dp(c, 44));
        r.setPadding(dp(c, 16), dp(c, 4), dp(c, 16), dp(c, 4));
        if (leading != null) r.addView(leading);
        r.addView(text(c, label, 17, R.color.text_primary, false), new LinearLayout.LayoutParams(0, -2, 1f));
        return r;
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

    /** The green iOS switch. */
    static androidx.appcompat.widget.SwitchCompat iosSwitch(Context c, boolean on,
                                                            android.widget.CompoundButton.OnCheckedChangeListener l) {
        androidx.appcompat.widget.SwitchCompat sw = new androidx.appcompat.widget.SwitchCompat(c);
        sw.setThumbDrawable(c.getDrawable(R.drawable.switch_thumb));
        sw.setTrackDrawable(c.getDrawable(R.drawable.switch_track));
        sw.setSplitTrack(false);
        sw.setChecked(on);
        sw.setOnCheckedChangeListener(l);
        return sw;
    }

    /** Large title plus the "Calendars" button on the trailing side; tapping the title can jump to a date. */
    static LinearLayout titleRow(MainActivity a, TextView title, View.OnClickListener onTitle) {
        LinearLayout row = new LinearLayout(a);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        title.setOnClickListener(onTitle);
        row.addView(title, new LinearLayout.LayoutParams(0, -2, 1f));
        android.widget.ImageButton b = iconButton(a, R.drawable.ic_layers, R.color.accent, 44);
        b.setContentDescription(a.getString(R.string.calendars));
        b.setOnClickListener(v -> a.startActivity(new android.content.Intent(a, CalendarsActivity.class)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(a, 44), dp(a, 44));
        lp.setMarginEnd(dp(a, 8));
        row.addView(b, lp);
        return row;
    }
}
