package com.semo.calendar;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;

/** Alternative launcher icons: one activity-alias per colour, exactly one of them is enabled at a time. */
final class Icons {
    private Icons() {
    }

    static final String[] KEYS = {"Blue", "Violet", "Green", "Pink", "Red", "Orange", "Gold", "Night", "Graphite"};
    static final int[] NAMES = {R.string.icon_blue, R.string.icon_violet, R.string.icon_green, R.string.icon_pink,
            R.string.icon_red, R.string.icon_orange, R.string.icon_gold, R.string.icon_night, R.string.icon_graphite};
    private static final int[] BG = {R.drawable.ic_launcher_background, R.drawable.ic_launcher_bg_violet,
            R.drawable.ic_launcher_bg_green, R.drawable.ic_launcher_bg_pink, R.drawable.ic_launcher_bg_red,
            R.drawable.ic_launcher_bg_orange, R.drawable.ic_launcher_bg_gold, R.drawable.ic_launcher_bg_night,
            R.drawable.ic_launcher_bg_graphite};
    private static final int[] FG = {R.drawable.ic_launcher_foreground, R.drawable.ic_launcher_fg_violet,
            R.drawable.ic_launcher_fg_green, R.drawable.ic_launcher_fg_pink, R.drawable.ic_launcher_fg_red,
            R.drawable.ic_launcher_fg_orange, R.drawable.ic_launcher_fg_gold, R.drawable.ic_launcher_fg_night,
            R.drawable.ic_launcher_fg_graphite};

    private static ComponentName alias(Context c, int i) {
        return new ComponentName(c.getPackageName(), "com.semo.calendar.Icon" + KEYS[i]);
    }

    /** Index of the icon on the home screen. */
    static int current(Context c) {
        PackageManager pm = c.getPackageManager();
        for (int i = 1; i < KEYS.length; i++) {
            if (pm.getComponentEnabledSetting(alias(c, i)) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) return i;
        }
        return 0;
    }

    /** Enables the chosen alias first, then disables the old one, so the app never vanishes from the launcher. */
    static void apply(Context c, int idx) {
        PackageManager pm = c.getPackageManager();
        int now = current(c);
        if (now == idx) return;
        pm.setComponentEnabledSetting(alias(c, idx), PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
        pm.setComponentEnabledSetting(alias(c, now), PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }

    /** The visible 72dp of the adaptive icon (background + foreground), scaled to fill a view. */
    static Drawable preview(Context c, int idx) {
        Drawable bg = c.getDrawable(BG[idx]);
        Drawable fg = c.getDrawable(FG[idx]);
        return new LayerDrawable(new Drawable[]{new InsetDrawable(bg, -0.25f), new InsetDrawable(fg, -0.25f)});
    }
}
