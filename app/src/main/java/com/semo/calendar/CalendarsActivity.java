package com.semo.calendar;

import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** The iOS "Calendars" sheet: tap a row to show or hide a calendar, the info button edits it, Add calendar. */
public final class CalendarsActivity extends BaseActivity {
    private LinearLayout list;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = Ui.vertical(this);
        root.setBackgroundColor(Ui.color(this, R.color.bg_group));
        applyInsets(root);
        setContentView(root);

        LinearLayout nav = new LinearLayout(this);
        nav.setGravity(Gravity.CENTER_VERTICAL);
        nav.setPadding(Ui.dp(this, 4), Ui.dp(this, 6), Ui.dp(this, 4), Ui.dp(this, 4));
        nav.addView(new View(this), new LinearLayout.LayoutParams(Ui.dp(this, 72), 1));
        TextView head = Ui.text(this, getString(R.string.calendars), 17, R.color.text_primary, false);
        head.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        head.setGravity(Gravity.CENTER);
        nav.addView(head, new LinearLayout.LayoutParams(0, Ui.dp(this, 40), 1f));
        TextView done = Ui.text(this, getString(R.string.done), 17, R.color.accent, false);
        done.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        done.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        done.setPadding(0, 0, Ui.dp(this, 12), 0);
        done.setOnClickListener(v -> finish());
        nav.addView(done, new LinearLayout.LayoutParams(Ui.dp(this, 72), Ui.dp(this, 40)));
        root.addView(nav);

        ScrollView sv = new ScrollView(this);
        sv.setClipToPadding(false);
        sv.setPadding(0, 0, 0, Ui.dp(this, 32));
        LinearLayout body = Ui.vertical(this);
        sv.addView(body, new ScrollView.LayoutParams(-1, -2));
        root.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1f));

        list = Ui.group(this, 0);
        body.addView(Ui.sectionHeader(this, getString(R.string.calendars)));
        body.addView(list);

        LinearLayout add = Ui.group(this, 20);
        LinearLayout row = Ui.cell(this, getString(R.string.add_calendar));
        ((TextView) row.getChildAt(0)).setTextColor(Ui.color(this, R.color.accent));
        row.setOnClickListener(v -> CalendarSheet.show(this, null, this::refresh));
        Ui.highlight(row);
        add.addView(row);
        body.addView(add);
        refresh();
    }

    private View check(Cals.Cal x) {
        FrameLayout f = new FrameLayout(this);
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        int col = Cals.colorOf(this, x);
        if (x.visible) {
            g.setColor(col);
            ImageView tick = new ImageView(this);
            tick.setImageResource(R.drawable.ic_check);
            tick.setImageTintList(android.content.res.ColorStateList.valueOf(0xFFFFFFFF));
            f.addView(tick, new FrameLayout.LayoutParams(Ui.dp(this, 15), Ui.dp(this, 15), Gravity.CENTER));
        } else {
            g.setColor(0);
            g.setStroke(Ui.dp(this, 2), col);
        }
        f.setBackground(g);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(this, 24), Ui.dp(this, 24));
        lp.setMarginEnd(Ui.dp(this, 14));
        f.setLayoutParams(lp);
        return f;
    }

    private void refresh() {
        list.removeAllViews();
        boolean firstRow = true;
        for (final Cals.Cal x : Cals.all(this)) {
            if (!firstRow) list.addView(Ui.hairline(this, 54));
            firstRow = false;
            LinearLayout r = Ui.cell(this, Cals.name(this, x), check(x));
            if (!x.builtin) {
                ImageButton info = Ui.iconButton(this, R.drawable.ic_info, R.color.accent, 40);
                info.setOnClickListener(v -> CalendarSheet.show(this, x, this::refresh));
                r.addView(info);
            }
            r.setOnClickListener(v -> {
                x.visible = !x.visible;
                Cals.save(this, x);
                refresh();
            });
            Ui.highlight(r);
            list.addView(r);
        }
    }
}
