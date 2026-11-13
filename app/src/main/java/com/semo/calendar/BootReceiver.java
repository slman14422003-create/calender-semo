package com.semo.calendar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent in) {
        DynamicIcon.sync(c);
        DynamicIcon.schedule(c);
        if (!DynamicIcon.TICK.equals(in.getAction())) Alarms.rescheduleAll(c);
    }
}
