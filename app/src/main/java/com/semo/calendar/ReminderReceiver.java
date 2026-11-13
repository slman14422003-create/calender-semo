package com.semo.calendar;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

/** Posts event and task alerts, with Snooze (events) and Mark as done (tasks) buttons. */
public class ReminderReceiver extends BroadcastReceiver {
    private static PendingIntent action(Context c, String act, String kind, String id, long at, int requestCode) {
        Intent i = new Intent(c, ReminderReceiver.class).putExtra("act", act).putExtra("kind", kind)
                .putExtra("id", id).putExtra("at", at);
        return PendingIntent.getBroadcast(c, requestCode, i, PendingIntent.FLAG_IMMUTABLE
                | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public void onReceive(Context c, Intent in) {
        String kind = in.getStringExtra("kind");
        String id = in.getStringExtra("id");
        String act = in.getStringExtra("act");
        long at = in.getLongExtra("at", 0);
        int slot = in.getIntExtra("slot", 0);
        if (id == null) return;
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        PendingIntent open = PendingIntent.getActivity(c, 0, new Intent(c, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        if ("task".equals(kind)) {
            Task t = Tasks.get(c, id);
            if (t == null) return;
            int nid = ("task:" + id).hashCode();
            if ("done".equals(act)) {
                t.done = true;
                Tasks.save(c, t);
                Alarms.cancelTask(c, id);
                if (nm != null) nm.cancel(nid);
                return;
            }
            NotificationCompat.Builder b = new NotificationCompat.Builder(c, App.CHANNEL)
                    .setSmallIcon(R.drawable.ic_tasks)
                    .setContentTitle(t.title.isEmpty() ? c.getString(R.string.untitled) : t.title)
                    .setContentText(Fmt.taskDue(c, t))
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .setAutoCancel(true)
                    .setContentIntent(open)
                    .addAction(0, c.getString(R.string.mark_done), action(c, "done", "task", id, at, nid + 7));
            try {
                if (nm != null) nm.notify(nid, b.build());
            } catch (SecurityException ignored) {
            }
            return;
        }

        Event e = Store.get(c, id);
        if (e == null) return;
        int nid = id.hashCode() + (slot == 1 ? 1 : 0);
        if ("snooze".equals(act)) {
            if (nm != null) nm.cancel(nid);
            Alarms.snooze(c, id, at, 10);
            return;
        }
        String when = e.allDay ? c.getString(R.string.all_day) : Fmt.time(c, at);
        if (!e.location.isEmpty()) when = when + " \u00b7 " + e.location;
        NotificationCompat.Builder b = new NotificationCompat.Builder(c, App.CHANNEL)
                .setSmallIcon(R.drawable.ic_month)
                .setContentTitle(e.label(c.getString(R.string.untitled)))
                .setContentText(when)
                .setColor(Cals.color(c, e))
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setAutoCancel(true)
                .setContentIntent(open)
                .addAction(0, c.getString(R.string.snooze), action(c, "snooze", "event", id, at, nid + 3));
        try {
            if (nm != null) nm.notify(nid, b.build());
        } catch (SecurityException ignored) {
        }
        Alarms.schedule(c, e);
    }
}
