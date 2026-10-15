package com.semo.calendar;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * The collapsing iOS navigation bar: a 44dp bar whose small centred title and hairline fade in once the large
 * title below has scrolled away. Feed it the scroll offset with {@link #onScroll(int)}.
 */
final class LargeNav {
    private final FrameLayout bar;
    private final TextView small;
    private final View line;
    private final Context c;

    LargeNav(Context ctx, CharSequence title, View trailing) {
        c = ctx;
        bar = new FrameLayout(c);
        small = Ui.text(c, title, 17, R.color.text_primary, false);
        small.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        small.setAlpha(0f);
        bar.addView(small, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));
        if (trailing != null) {
            FrameLayout.LayoutParams tp = new FrameLayout.LayoutParams(Ui.dp(c, 40), Ui.dp(c, 40),
                    Gravity.END | Gravity.CENTER_VERTICAL);
            tp.setMarginEnd(Ui.dp(c, 8));
            bar.addView(trailing, tp);
        }
        line = new View(c);
        line.setBackgroundColor(Ui.color(c, R.color.stroke));
        line.setAlpha(0f);
        bar.addView(line, new FrameLayout.LayoutParams(-1, Math.max(1, Ui.dp(c, 0.5f)), Gravity.BOTTOM));
    }

    View view() {
        return bar;
    }

    int height() {
        return Ui.dp(c, 44);
    }

    void onScroll(int offsetPx) {
        float a = Math.max(0f, Math.min(1f, (offsetPx - Ui.dp(c, 18)) / (float) Ui.dp(c, 26)));
        small.setAlpha(a);
        line.setAlpha(a);
    }
}
