package com.semo.calendar;

import android.view.View;

/** One tab of the main screen. */
interface Screen {
    View view();

    void refresh();
}
