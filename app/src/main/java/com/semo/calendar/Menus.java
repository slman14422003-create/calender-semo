package com.semo.calendar;

import android.content.Intent;

import java.util.ArrayList;
import java.util.List;

/** The long-press menus of the app's rows and bars. */
final class Menus {
    private Menus() {
    }

    static List<LongPress.Item> event(final MainActivity a, final Recur.Occ o) {
        List<LongPress.Item> l = new ArrayList<>();
        l.add(LongPress.item(a.getString(R.string.open), () -> a.showDetail(o)));
        if (!o.e.readOnly) {
            l.add(LongPress.item(a.getString(R.string.edit), () -> a.openEditor(o.e.id, 0)));
            l.add(LongPress.item(a.getString(R.string.duplicate), () -> a.duplicate(o.e)));
        }
        l.add(LongPress.item(a.getString(R.string.share), () -> {
            Intent send = new Intent(Intent.ACTION_SEND).setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, Fmt.shareText(a, o));
            a.startActivity(Intent.createChooser(send, null));
        }));
        if (!o.e.readOnly) l.add(LongPress.danger(a.getString(R.string.delete), () -> a.confirmDelete(o.e)));
        return l;
    }

    static List<LongPress.Item> task(final MainActivity a, final Task t) {
        List<LongPress.Item> l = new ArrayList<>();
        l.add(LongPress.item(a.getString(R.string.open), () -> TaskSheet.show(a, t, 0)));
        l.add(LongPress.item(a.getString(t.done ? R.string.mark_undone : R.string.mark_done), () -> {
            t.done = !t.done;
            Tasks.save(a, t);
            Alarms.scheduleTask(a.getApplicationContext(), t);
            a.refreshCurrent();
        }));
        l.add(LongPress.danger(a.getString(R.string.delete), () -> {
            Tasks.delete(a, t.id);
            Alarms.cancelTask(a, t.id);
            a.refreshCurrent();
        }));
        return l;
    }

    static List<LongPress.Item> day(final MainActivity a, final long dayStart) {
        List<LongPress.Item> l = new ArrayList<>();
        l.add(LongPress.item(a.getString(R.string.new_event), () -> a.openEditor(null, dayStart)));
        l.add(LongPress.item(a.getString(R.string.new_task), () -> TaskSheet.show(a, null, dayStart)));
        return l;
    }

    static List<LongPress.Item> slot(final MainActivity a, final long at) {
        List<LongPress.Item> l = new ArrayList<>();
        l.add(LongPress.item(a.getString(R.string.new_event), () -> a.openEditorAt(at)));
        l.add(LongPress.item(a.getString(R.string.new_task), () -> TaskSheet.show(a, null, Recur.dayStart(at))));
        return l;
    }
}
