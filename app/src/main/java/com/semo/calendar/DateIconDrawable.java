package com.semo.calendar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

import java.util.Calendar;
import java.util.Locale;

/** Calendar tab icon that shows today's day number inside the calendar outline, like the iOS Calendar icon. */
final class DateIconDrawable extends Drawable {
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF box = new RectF();
    private final float unit;

    DateIconDrawable(Context c) {
        unit = Ui.dp(c, 1);
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeCap(Paint.Cap.ROUND);
        line.setStrokeWidth(unit * 1.8f);
        text.setTextAlign(Paint.Align.CENTER);
        text.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        text.setTextSize(Ui.sp(c, 9.5f));
    }

    void setColor(int color) {
        line.setColor(color);
        text.setColor(color);
        invalidateSelf();
    }

    @Override
    public void draw(@NonNull Canvas cv) {
        float u = getBounds().width() / 24f;
        float l = getBounds().left;
        float t = getBounds().top;
        box.set(l + 3 * u, t + 5 * u, l + 21 * u, t + 21 * u);
        cv.drawRoundRect(box, 3 * u, 3 * u, line);
        cv.drawLine(l + 3 * u, t + 10 * u, l + 21 * u, t + 10 * u, line);
        cv.drawLine(l + 8 * u, t + 2.5f * u, l + 8 * u, t + 6.5f * u, line);
        cv.drawLine(l + 16 * u, t + 2.5f * u, l + 16 * u, t + 6.5f * u, line);
        String day = String.format(Locale.getDefault(), "%d", Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
        Paint.FontMetrics fm = text.getFontMetrics();
        cv.drawText(day, l + 12 * u, t + 15.6f * u - (fm.ascent + fm.descent) / 2f, text);
    }

    @Override
    public void setAlpha(int alpha) {
        line.setAlpha(alpha);
        text.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        line.setColorFilter(cf);
        text.setColorFilter(cf);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
