package com.semo.calendar;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Privacy policy: what the calendar stores, which services it contacts, why each permission exists, how to delete everything. */
public final class PrivacyActivity extends BaseActivity {
    @Override
    protected boolean modal() {
        return true;
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout body = Pages.frame(this, getString(R.string.privacy_title));
        body.addView(Pages.footnote(this, getString(R.string.privacy_updated)));

        String[] titles = getResources().getStringArray(R.array.privacy_titles);
        String[] bodies = getResources().getStringArray(R.array.privacy_bodies);
        for (int i = 0; i < titles.length && i < bodies.length; i++) {
            body.addView(Ui.sectionHeader(this, titles[i]));
            LinearLayout g = Ui.group(this, 0);
            TextView t = Ui.text(this, bodies[i], 15, R.color.text_primary, false);
            t.setLineSpacing(0f, 1.2f);
            t.setPadding(Ui.dp(this, 16), Ui.dp(this, 14), Ui.dp(this, 16), Ui.dp(this, 14));
            t.setTextIsSelectable(true);
            g.addView(t, new LinearLayout.LayoutParams(-1, -2));
            body.addView(g);
        }

        body.addView(Ui.sectionHeader(this, getString(R.string.privacy_contact)));
        LinearLayout lg = Ui.group(this, 0);
        LinearLayout issues = Ui.cell(this, getString(R.string.privacy_issues),
                Ui.tile(this, R.drawable.ic_info, Ui.color(this, R.color.ok)));
        issues.addView(Ui.chevron(this));
        issues.setOnClickListener(v -> Pages.open(this, Updater.ISSUES));
        Ui.highlight(issues);
        lg.addView(issues);
        lg.addView(Ui.hairline(this, 58));
        LinearLayout src = Ui.cell(this, getString(R.string.privacy_source),
                Ui.tile(this, R.drawable.ic_layers, Ui.color(this, R.color.cal_indigo)));
        src.addView(Ui.chevron(this));
        src.setOnClickListener(v -> Pages.open(this, Updater.SOURCE));
        Ui.highlight(src);
        lg.addView(src);
        body.addView(lg);
    }
}
