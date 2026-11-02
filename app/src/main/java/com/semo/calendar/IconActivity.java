package com.semo.calendar;

import android.animation.ValueAnimator;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** App icon picker: a grid of the colour variants, the chosen one gets a ring that springs in. */
public final class IconActivity extends BaseActivity {
    private static final int COLS = 3;
    private final View[] rings = new View[Icons.KEYS.length];
    private final ImageView[] pics = new ImageView[Icons.KEYS.length];
    private final TextView[] names = new TextView[Icons.KEYS.length];
    private int cur;
    private TextView note;

    @Override
    protected boolean modal() {
        return true;
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout body = Pages.frame(this, getString(R.string.app_icon));
        cur = Icons.current(this);

        LinearLayout card = Ui.group(this, 16);
        card.setPadding(Ui.dp(this, 4), Ui.dp(this, 14), Ui.dp(this, 4), Ui.dp(this, 6));
        LinearLayout row = null;
        for (int i = 0; i < Icons.KEYS.length; i++) {
            if (i % COLS == 0) {
                row = new LinearLayout(this);
                card.addView(row, new LinearLayout.LayoutParams(-1, -2));
            }
            row.addView(tile(i), new LinearLayout.LayoutParams(0, -2, 1f));
        }
        body.addView(card);
        note = Pages.footnote(this, getString(R.string.app_icon_note));
        note.setAlpha(0.0f);
        body.addView(note);
        paint(false);
    }

    private View tile(final int i) {
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setGravity(Gravity.CENTER_HORIZONTAL);
        t.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 10));

        FrameLayout box = new FrameLayout(this);
        View ring = new View(this);
        GradientDrawable rg = new GradientDrawable();
        rg.setColor(0);
        rg.setCornerRadius(Ui.dp(this, 26));
        rg.setStroke(Ui.dp(this, 3), Ui.color(this, R.color.accent));
        ring.setBackground(rg);
        box.addView(ring, new FrameLayout.LayoutParams(-1, -1));
        rings[i] = ring;

        ImageView pic = new ImageView(this);
        pic.setImageDrawable(Icons.preview(this, i));
        final float r = Ui.dp(this, 19);
        pic.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View v, Outline o) {
                o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), r);
            }
        });
        pic.setClipToOutline(true);
        pic.setContentDescription(getString(Icons.NAMES[i]));
        FrameLayout.LayoutParams pp = new FrameLayout.LayoutParams(Ui.dp(this, 76), Ui.dp(this, 76), Gravity.CENTER);
        box.addView(pic, pp);
        pics[i] = pic;
        t.addView(box, new LinearLayout.LayoutParams(Ui.dp(this, 92), Ui.dp(this, 92)));

        TextView name = Ui.text(this, getString(Icons.NAMES[i]), 15, R.color.text_secondary, false);
        name.setGravity(Gravity.CENTER);
        name.setSingleLine(true);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(-2, -2);
        np.topMargin = Ui.dp(this, 6);
        t.addView(name, np);
        names[i] = name;

        t.setOnClickListener(v -> choose(i));
        Ui.press(t);
        return t;
    }

    private void choose(int i) {
        if (i == cur) {
            Motion.bounce(pics[i]);
            return;
        }
        final int old = cur;
        cur = i;
        Icons.apply(this, i);
        pics[i].performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        Motion.bounce(pics[i]);
        // the old ring shrinks away, the new one springs in
        rings[old].animate().cancel();
        rings[old].animate().alpha(0f).scaleX(0.9f).scaleY(0.9f).setDuration(200).setInterpolator(Motion.IN).start();
        rings[i].animate().cancel();
        rings[i].setAlpha(0f);
        rings[i].setScaleX(1.18f);
        rings[i].setScaleY(1.18f);
        rings[i].animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(520).setInterpolator(Motion.SPRING).start();
        for (int k = 0; k < names.length; k++) {
            names[k].setTextColor(Ui.color(this, k == i ? R.color.accent : R.color.text_secondary));
            names[k].setTypeface(k == i ? Typeface.create("sans-serif-medium", Typeface.BOLD) : Typeface.DEFAULT);
        }
        note.animate().cancel();
        ValueAnimator va = ValueAnimator.ofFloat(note.getAlpha(), 1f);
        va.setDuration(300);
        va.addUpdateListener(an -> note.setAlpha((Float) an.getAnimatedValue()));
        va.start();
    }

    private void paint(boolean animate) {
        for (int k = 0; k < names.length; k++) {
            boolean on = k == cur;
            rings[k].setAlpha(on ? 1f : 0f);
            names[k].setTextColor(Ui.color(this, on ? R.color.accent : R.color.text_secondary));
            names[k].setTypeface(on ? Typeface.create("sans-serif-medium", Typeface.BOLD) : Typeface.DEFAULT);
        }
    }
}
