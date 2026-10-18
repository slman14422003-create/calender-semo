package com.semo.calendar;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * "Share to Calendar" doorway: reads only the shared text, lets the on-device parser pick the date, time, repeat,
 * alert and place out of it, and opens a prefilled new event.
 */
public class ShareActivity extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        CharSequence cs = getIntent().getCharSequenceExtra(Intent.EXTRA_TEXT);
        String s = cs == null ? "" : cs.toString().trim();
        if (s.length() > 800) s = s.substring(0, 800);
        NlParser.Draft d = s.isEmpty() ? new NlParser.Draft()
                : NlParser.parse(this, s.replace('\n', ' '), System.currentTimeMillis());
        int nl = s.indexOf('\n');
        String title = !d.title.isEmpty() ? d.title : (nl < 0 ? s : s.substring(0, nl).trim());
        if (title.length() > 120) title = title.substring(0, 120);
        Intent in = new Intent(this, EditorActivity.class).putExtra("title", title).putExtra("notes", s);
        if (d.start != 0) in.putExtra("at", d.start);
        if (d.end != 0) in.putExtra("end", d.end);
        else if (d.start != 0 && d.durationMin > 0) in.putExtra("end", d.start + d.durationMin * 60000L);
        in.putExtra("allday", d.allDay).putExtra("repeat", d.repeat);
        if (d.remind >= 0) in.putExtra("remind", d.remind);
        if (!d.location.isEmpty()) in.putExtra("loc", d.location);
        startActivity(in);
        finish();
    }
}
