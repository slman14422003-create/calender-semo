package com.semo.calendar;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

import java.util.List;

/**
 * Read-only summary for the other Semo apps (the clock shows "next event"): only times and a count, never titles,
 * places or notes. Rows are (key, value) pairs: next_event (millis, 0 when none in the next 7 days), events_today.
 */
public final class SemoProvider extends ContentProvider {
    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sort) {
        MatrixCursor c = new MatrixCursor(new String[]{"key", "value"});
        try {
            android.content.Context ctx = getContext();
            if (ctx == null) return c;
            long now = System.currentTimeMillis();
            long d0 = Recur.dayStart(now);
            long d1 = Recur.nextDay(d0);
            int today = 0;
            for (Recur.Occ o : Store.between(ctx, d0, d1)) if (!o.e.readOnly && o.end > now) today++;
            long next = 0;
            List<Recur.Occ> week = Store.between(ctx, now, now + 7L * 24 * 3600 * 1000);
            for (Recur.Occ o : week) {
                if (o.e.readOnly || o.e.allDay || o.start <= now) continue;
                if (next == 0 || o.start < next) next = o.start;
            }
            c.addRow(new Object[]{"next_event", String.valueOf(next)});
            c.addRow(new Object[]{"events_today", String.valueOf(today)});
        } catch (Throwable ignored) {
        }
        return c;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] args) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] args) {
        return 0;
    }
}
