package com.semo.calendar;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Glue of the Semo ecosystem. Apps stay separate; each declares the ECOSYSTEM activity filter and the
 * app.semo.ROLE meta-data in its manifest, and finds the others through them (no package names hard-coded).
 */
final class Semo {
    private Semo() {
    }

    static final String ACTION_ECO = "app.semo.ECOSYSTEM";
    static final String META_ROLE = "app.semo.ROLE";
    static final String ROLE_NOTES = "notes";

    static final class Sibling {
        String pkg = "";
        String role = "";
        CharSequence label = "";
        String version = "";
    }

    @SuppressWarnings("deprecation")
    static List<Sibling> siblings(Context c) {
        List<Sibling> out = new ArrayList<>();
        try {
            PackageManager pm = c.getPackageManager();
            Intent probe = new Intent(ACTION_ECO).addCategory(Intent.CATEGORY_DEFAULT);
            Set<String> seen = new HashSet<>();
            for (ResolveInfo ri : pm.queryIntentActivities(probe, 0)) {
                if (ri.activityInfo == null) continue;
                String p = ri.activityInfo.packageName;
                if (p == null || p.equals(c.getPackageName()) || !seen.add(p)) continue;
                Sibling s = new Sibling();
                s.pkg = p;
                try {
                    ApplicationInfo ai = pm.getApplicationInfo(p, PackageManager.GET_META_DATA);
                    s.label = pm.getApplicationLabel(ai);
                    Bundle md = ai.metaData;
                    if (md != null && md.getString(META_ROLE) != null) s.role = md.getString(META_ROLE);
                    PackageInfo pi = pm.getPackageInfo(p, 0);
                    s.version = pi.versionName == null ? "" : pi.versionName;
                } catch (Exception ignored) {
                }
                out.add(s);
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    static Sibling byRole(Context c, String role) {
        for (Sibling s : siblings(c)) if (role.equals(s.role)) return s;
        return null;
    }

    static boolean open(Context c, String pkg) {
        try {
            Intent i = c.getPackageManager().getLaunchIntentForPackage(pkg);
            if (i == null) return false;
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(i);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    static boolean hasNotes(Context c) {
        return byRole(c, ROLE_NOTES) != null;
    }

    /** Sends plain text to the Notes app (its public share doorway opens a new note with it). */
    static boolean sendToNotes(Context c, String title, String text) {
        Sibling s = byRole(c, ROLE_NOTES);
        if (s == null || text == null || text.isEmpty()) return false;
        try {
            Intent i = new Intent(Intent.ACTION_SEND).setType("text/plain").setPackage(s.pkg);
            i.putExtra(Intent.EXTRA_TEXT, text);
            if (title != null && !title.isEmpty()) i.putExtra(Intent.EXTRA_SUBJECT, title);
            c.startActivity(i);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
