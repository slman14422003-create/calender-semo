package com.semo.calendar;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewParent;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.List;

import eightbitlab.com.blurview.BlurTarget;
import eightbitlab.com.blurview.BlurView;

/**
 * iOS-style context menu on long press, available on every screen and on the bottom bar. The finger holds a view
 * for about half a second: the whole window blurs live, the pressed view lifts out of the blur and a frosted menu
 * springs open next to it. Views describe their menu with {@link #set}; views without one get a single "Open".
 */
final class LongPress {
    interface Provider {
        /** Menu entries for a long press at the given screen position; null = default menu, empty = no menu. */
        List<Item> items(float rawX, float rawY);
    }

    static final class Item {
        final CharSequence label;
        final boolean danger;
        final Runnable run;

        Item(CharSequence label, boolean danger, Runnable run) {
            this.label = label;
            this.danger = danger;
            this.run = run;
        }
    }

    static Item item(CharSequence label, Runnable run) {
        return new Item(label, false, run);
    }

    static Item danger(CharSequence label, Runnable run) {
        return new Item(label, true, run);
    }

    private static final class Entry {
        final int radiusDp;
        final Provider provider;

        Entry(int radiusDp, Provider provider) {
            this.radiusDp = radiusDp;
            this.provider = provider;
        }
    }

    /** Gives a view its long-press menu. A negative radius means the view is not lifted (big custom views). */
    static void set(View v, int radiusDp, Provider p) {
        v.setTag(R.id.lp_menu, new Entry(radiusDp, p));
    }

    private static final float BLUR = 22f;
    private static final int[] LOC = new int[2];

    private final BaseActivity a;
    private final Handler h = new Handler(Looper.getMainLooper());
    private final OnBackPressedCallback back;
    private View target;
    private float downRawX, downRawY, lastX, lastY;
    private long downTime;
    private boolean swallow, showing, closing;
    private Runnable fire;

    private FrameLayout veil;
    private BlurView blur, menuView;
    private ImageView lifted;
    private float liftHome;
    private ValueAnimator ramp;
    private ObjectAnimator menuFade;

    LongPress(BaseActivity act) {
        a = act;
        back = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                dismiss(null);
            }
        };
        a.getOnBackPressedDispatcher().addCallback(a, back);
    }

    boolean isShowing() {
        return showing;
    }

    // ---- touch tracking ----

    /** Called for every touch event of the window; returns true when the event must not reach the views. */
    boolean onTouch(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                cancelPending();
                swallow = false;
                if (showing) return false;
                View decor = a.getWindow().getDecorView();
                View t = find(decor, e.getRawX(), e.getRawY());
                if (t != null) {
                    target = t;
                    downRawX = e.getRawX();
                    downRawY = e.getRawY();
                    downTime = e.getDownTime();
                    lastX = e.getX();
                    lastY = e.getY();
                    fire = this::fireNow;
                    h.postDelayed(fire, ViewConfiguration.getLongPressTimeout());
                }
                return false;
            }
            case MotionEvent.ACTION_MOVE: {
                lastX = e.getX();
                lastY = e.getY();
                if (target != null) {
                    float s = ViewConfiguration.get(a).getScaledTouchSlop();
                    if (Math.abs(e.getRawX() - downRawX) > s || Math.abs(e.getRawY() - downRawY) > s) cancelPending();
                }
                return swallow;
            }
            case MotionEvent.ACTION_POINTER_DOWN:
                cancelPending();
                return swallow;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                cancelPending();
                boolean s = swallow;
                swallow = false;
                return s;
            }
            default:
                return swallow;
        }
    }

    private void cancelPending() {
        if (fire != null) h.removeCallbacks(fire);
        fire = null;
        target = null;
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
        return v.getTag(R.id.lp_menu) instanceof Entry || (v.isClickable() && v.isEnabled()) ? v : null;
    }

    private static Entry entryOf(View t, View[] owner) {
        View x = t;
        while (x != null) {
            Object o = x.getTag(R.id.lp_menu);
            if (o instanceof Entry) {
                owner[0] = x;
                return (Entry) o;
            }
            ViewParent p = x.getParent();
            x = p instanceof View ? (View) p : null;
        }
        return null;
    }

    private static boolean excluded(View t) {
        return t instanceof EditText || t instanceof CompoundButton || t instanceof SeekBar;
    }

    private void fireNow() {
        final View t0 = target;
        target = null;
        fire = null;
        if (t0 == null || showing || !t0.isAttachedToWindow()) return;
        View[] own = new View[1];
        Entry en = entryOf(t0, own);
        View t = en != null ? own[0] : t0;
        if (excluded(t)) return;
        List<Item> list = null;
        int radius = 14;
        boolean lift = true;
        if (en != null) {
            list = en.provider.items(downRawX, downRawY);
            if (list != null && list.isEmpty()) return;
            radius = Math.max(0, en.radiusDp);
            lift = en.radiusDp >= 0;
        }
        if (list == null) list = defaultItems(t);
        swallow = true;
        long now = SystemClock.uptimeMillis();
        MotionEvent c = MotionEvent.obtain(downTime, now, MotionEvent.ACTION_CANCEL, lastX, lastY, 0);
        a.dispatchRaw(c);
        c.recycle();
        a.getWindow().getDecorView().performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        show(t, list, radius, lift);
    }

    private List<Item> defaultItems(final View t) {
        CharSequence label = t.getContentDescription();
        if ((label == null || label.length() == 0) && t instanceof TextView) {
            CharSequence s = ((TextView) t).getText();
            if (s != null && s.length() > 0 && s.length() <= 28) label = s;
        }
        if (label == null || label.length() == 0) label = a.getString(R.string.open);
        List<Item> l = new ArrayList<>();
        l.add(item(label, t::performClick));
        return l;
    }

    // ---- showing ----

    private Bitmap snap(View t) {
        int w = t.getWidth(), hh = t.getHeight();
        if (w <= 0 || hh <= 0) return null;
        try {
            Bitmap b = Bitmap.createBitmap(w, hh, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(b);
            Drawable fg = t.getForeground();
            if (fg != null) t.setForeground(null);
            t.draw(c);
            if (fg != null) t.setForeground(fg);
            return b;
        } catch (RuntimeException | OutOfMemoryError ex) {
            return null;
        }
    }

    private static ViewOutlineProvider round(final float r) {
        return new ViewOutlineProvider() {
            @Override
            public void getOutline(View v, Outline o) {
                o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), r);
                o.setAlpha(1f);
            }
        };
    }

    private void show(View t, List<Item> items, int radiusDp, boolean lift) {
        final FrameLayout shell = a.shell();
        final BlurTarget bt = a.blurTarget();
        if (shell == null || bt == null || shell.getWidth() == 0) {
            swallow = false;
            return;
        }
        showing = true;
        closing = false;
        back.setEnabled(true);
        boolean night = (a.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        final ColorDrawable clear = new ColorDrawable(Ui.color(a, R.color.bg));

        veil = new FrameLayout(a);
        veil.setClickable(true);
        veil.setOnClickListener(v -> dismiss(null));

        blur = new BlurView(a);
        blur.setupWith(bt).setFrameClearDrawable(clear).setBlurRadius(Build.VERSION.SDK_INT >= 31 ? 1f : BLUR)
                .setOverlayColor(night ? 0x59000000 : 0x33FFFFFF);
        blur.setAlpha(0f);
        veil.addView(blur, new FrameLayout.LayoutParams(-1, -1));

        int W = shell.getWidth(), H = shell.getHeight();
        shell.getLocationOnScreen(LOC);
        int sx = LOC[0], sy = LOC[1];

        Bitmap bmp = null;
        int tw = t.getWidth(), th = t.getHeight();
        if (lift && tw > 0 && th > 0 && (long) tw * th < (long) W * H * 0.4f && th < H * 0.6f) bmp = snap(t);
        float lx, ly;
        int lw, lh;
        if (bmp != null) {
            t.getLocationOnScreen(LOC);
            lx = LOC[0] - sx;
            ly = LOC[1] - sy;
            lw = tw;
            lh = th;
        } else {
            lw = lh = Ui.dp(a, 36);
            lx = downRawX - sx - lw / 2f;
            ly = downRawY - sy - lh / 2f;
        }

        // the frosted menu
        LinearLayout col = new LinearLayout(a);
        col.setOrientation(LinearLayout.VERTICAL);
        for (int i = 0; i < items.size(); i++) {
            final Item it = items.get(i);
            if (i > 0) col.addView(Ui.hairline(a, 0));
            TextView tv = Ui.text(a, it.label, 17, it.danger ? R.color.bad : R.color.text_primary, false);
            tv.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
            tv.setPadding(Ui.dp(a, 16), 0, Ui.dp(a, 16), 0);
            tv.setSingleLine(true);
            tv.setEllipsize(TextUtils.TruncateAt.END);
            tv.setOnClickListener(v -> dismiss(it.run));
            Ui.highlight(tv);
            col.addView(tv, new LinearLayout.LayoutParams(-1, Ui.dp(a, 46)));
        }
        int pad = Ui.dp(a, 12);
        int menuW = Math.min(Ui.dp(a, 250), W - 2 * pad);
        col.measure(View.MeasureSpec.makeMeasureSpec(menuW, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        int menuH = col.getMeasuredHeight();
        menuView = new BlurView(a);
        menuView.setupWith(bt).setFrameClearDrawable(clear).setBlurRadius(30f)
                .setOverlayColor((Ui.color(a, R.color.alert_bg) & 0x00FFFFFF) | 0xC8000000);
        menuView.setOutlineProvider(round(Ui.dp(a, 14)));
        menuView.setClipToOutline(true);
        menuView.setElevation(Ui.dp(a, 16));
        menuView.addView(col, new FrameLayout.LayoutParams(-1, -2));

        // placement: below the lifted view, else above it, else everything moves up
        WindowInsetsCompat wi = ViewCompat.getRootWindowInsets(shell);
        Insets bars = wi == null ? Insets.NONE : wi.getInsets(WindowInsetsCompat.Type.systemBars());
        float topMin = bars.top + pad, botMax = H - bars.bottom - pad;
        float gap = Ui.dp(a, bmp != null ? 10 : 16);
        float top = ly, menuTop;
        boolean below = true;
        if (ly + lh + gap + menuH <= botMax) {
            menuTop = ly + lh + gap;
        } else if (ly - gap - menuH >= topMin) {
            menuTop = ly - gap - menuH;
            below = false;
        } else {
            top = Math.max(topMin, botMax - (lh + gap + menuH));
            menuTop = top + lh + gap;
        }
        boolean left = lx + lw / 2f < W / 2f;
        float menuX = left ? lx : lx + lw - menuW;
        menuX = Math.max(pad, Math.min(W - pad - menuW, menuX));
        float pivotX = Math.max(0f, Math.min(menuW, lx + lw / 2f - menuX));

        liftHome = ly;
        if (bmp != null) {
            lifted = new ImageView(a);
            lifted.setImageBitmap(bmp);
            lifted.setScaleType(ImageView.ScaleType.FIT_XY);
            if (t.getBackground() == null) {
                lifted.setBackground(Ui.shape(Ui.color(a, R.color.surface), Ui.dp(a, radiusDp)));
            }
            lifted.setOutlineProvider(round(Ui.dp(a, radiusDp)));
            lifted.setClipToOutline(true);
            lifted.setElevation(Ui.dp(a, 24));
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(lw, lh, Gravity.TOP | Gravity.LEFT);
            veil.addView(lifted, lp);
            lifted.setTranslationX(lx);
            lifted.setTranslationY(ly);
        } else {
            lifted = null;
        }
        FrameLayout.LayoutParams mp = new FrameLayout.LayoutParams(menuW, menuH, Gravity.TOP | Gravity.LEFT);
        veil.addView(menuView, mp);
        menuView.setTranslationX(menuX);
        menuView.setTranslationY(menuTop);
        menuView.setPivotX(pivotX);
        menuView.setPivotY(below ? 0f : menuH);
        menuView.setAlpha(0f);
        menuView.setScaleX(0.4f);
        menuView.setScaleY(0.4f);

        shell.addView(veil, new FrameLayout.LayoutParams(-1, -1));

        // live blur: radius and opacity ramp up together, the lifted view springs out, the menu unfolds
        ValueAnimator va = ValueAnimator.ofFloat(0f, 1f);
        va.setDuration(380);
        va.setInterpolator(Motion.OUT);
        final BlurView b = blur;
        va.addUpdateListener(an -> {
            float f = (Float) an.getAnimatedValue();
            b.setAlpha(f);
            if (Build.VERSION.SDK_INT >= 31) b.setBlurRadius(Math.max(1f, BLUR * f));
        });
        ramp = va;
        va.start();
        if (lifted != null) {
            lifted.animate().translationY(top).scaleX(1.045f).scaleY(1.045f).setDuration(480)
                    .setInterpolator(Motion.SPRING).start();
        }
        menuView.animate().scaleX(1f).scaleY(1f).setStartDelay(50).setDuration(460)
                .setInterpolator(Motion.SPRING).start();
        menuFade = ObjectAnimator.ofFloat(menuView, View.ALPHA, 0f, 1f);
        menuFade.setDuration(200);
        menuFade.setStartDelay(50);
        menuFade.start();
    }

    // ---- hiding ----

    /** Closes the menu with the reverse animation; {@code after} (the chosen action) runs right away. */
    void dismiss(Runnable after) {
        if (!showing || closing) return;
        closing = true;
        back.setEnabled(false);
        final FrameLayout v = veil;
        final BlurView b = blur;
        if (ramp != null) ramp.cancel();
        if (menuFade != null) menuFade.cancel();
        menuView.animate().cancel();
        menuView.animate().alpha(0f).scaleX(0.85f).scaleY(0.85f).setStartDelay(0).setDuration(170)
                .setInterpolator(Motion.IN).start();
        if (lifted != null) {
            lifted.animate().cancel();
            if (after != null) {
                lifted.animate().alpha(0f).scaleX(0.96f).scaleY(0.96f).setDuration(200)
                        .setInterpolator(Motion.IN).start();
            } else {
                lifted.animate().translationY(liftHome).scaleX(1f).scaleY(1f).setDuration(320)
                        .setInterpolator(Motion.OUT).start();
            }
        }
        float from = b.getAlpha();
        ValueAnimator va = ValueAnimator.ofFloat(from, 0f);
        va.setDuration(280);
        va.setInterpolator(Motion.DECEL);
        va.addUpdateListener(an -> {
            float f = (Float) an.getAnimatedValue();
            b.setAlpha(f);
            if (Build.VERSION.SDK_INT >= 31) b.setBlurRadius(Math.max(1f, BLUR * f));
        });
        va.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                cleanup(v, b);
            }
        });
        va.start();
        if (after != null) h.post(after);
    }

    private void cleanup(FrameLayout v, BlurView b) {
        try {
            b.setBlurEnabled(false);
            if (menuView != null) menuView.setBlurEnabled(false);
        } catch (RuntimeException ignored) {
        }
        ViewGroup p = v.getParent() instanceof ViewGroup ? (ViewGroup) v.getParent() : null;
        if (p != null) p.removeView(v);
        if (veil == v) {
            veil = null;
            blur = null;
            menuView = null;
            lifted = null;
            showing = false;
            closing = false;
        }
    }

    /** Immediate removal (screen paused or destroyed). */
    void release() {
        cancelPending();
        swallow = false;
        if (veil != null) {
            back.setEnabled(false);
            if (ramp != null) ramp.cancel();
            if (menuFade != null) menuFade.cancel();
            if (blur != null) blur.animate().cancel();
            FrameLayout v = veil;
            BlurView b = blur;
            if (b != null) cleanup(v, b);
            else if (v.getParent() instanceof ViewGroup) ((ViewGroup) v.getParent()).removeView(v);
        }
        showing = false;
        closing = false;
        veil = null;
    }
}
