package com.semo.calendar;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/** "Share to Calendar" doorway: reads only the shared text and opens a prefilled new event. */
public class ShareActivity extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        CharSequence cs = getIntent().getCharSequenceExtra(Intent.EXTRA_TEXT);
        String s = cs == null ? "" : cs.toString().trim();
        if (s.length() > 800) s = s.substring(0, 800);
        int nl = s.indexOf('\n');
        String title = nl < 0 ? s : s.substring(0, nl).trim();
        String notes = nl < 0 ? "" : s.substring(nl + 1).trim();
        if (title.length() > 120) {
            notes = title.substring(120) + (notes.isEmpty() ? "" : "\n" + notes);
            title = title.substring(0, 120);
        }
        startActivity(new Intent(this, EditorActivity.class).putExtra("title", title).putExtra("notes", notes));
        finish();
    }
}
