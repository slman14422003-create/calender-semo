package com.semo.calendar;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.format.DateFormat;

import androidx.core.app.NotificationCompat;

import java.util.Date;

public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent in) {
        String id = in.getStringExtra("id");
        long at = in.getLongExtra("at", 0);
        if (id == null) return;
        Event e = Store.get(c, id);
        if (e == null) return;
        String when = e.allDay ? c.getString(R.string.all_day)
                : DateFormat.getTimeFormat(c).format(new Date(at));
        if (!e.location.isEmpty()) when = when + " · " + e.location;
        PendingIntent open = PendingIntent.getActivity(c, 0, new Intent(c, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder b = new NotificationCompat.Builder(c, App.CHANNEL)
                .setSmallIcon(R.drawable.ic_month)
                .setContentTitle(e.title.isEmpty() ? c.getString(R.string.untitled) : e.title)
                .setContentText(when)
                .setColor(c.getColor(Event.COLORS[e.color]))
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setAutoCancel(true)
                .setContentIntent(open);
        try {
            NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.notify(id.hashCode(), b.build());
        } catch (SecurityException ignored) {
        }
        Alarms.schedule(c, e);
    }
}
