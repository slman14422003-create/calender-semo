package com.semo.calendar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

import java.util.Calendar;
import java.util.Locale;

/**
 * The five tab icons, drawn on a 24-unit grid like SF Symbols: outlined when idle and filled when selected.
 * Calendar shows today's date, Agenda is a bulleted list, Tasks a check circle, Search a magnifier, Settings a gear.
 */
final class TabIconDrawable extends Drawable {
    static final int CAL = 0, LIST = 1, TASKS = 2, SEARCH = 3, GEAR = 4;

    private final int kind;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint num = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cut = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cutText = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private final Path path = new Path();
    private Path gear;
    private boolean selected;

    TabIconDrawable(Context c, int kind) {
        this.kind = kind;
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        Typeface bold = Typeface.create("sans-serif-medium", Typeface.BOLD);
        num.setTextAlign(Paint.Align.CENTER);
        num.setTypeface(bold);
        num.setTextSize(9.5f);
        cutText.setTextAlign(Paint.Align.CENTER);
        cutText.setTypeface(bold);
        cutText.setTextSize(9.5f);
        PorterDuffXfermode clear = new PorterDuffXfermode(PorterDuff.Mode.CLEAR);
        cut.setXfermode(clear);
        cut.setStrokeCap(Paint.Cap.ROUND);
        cut.setStrokeJoin(Paint.Join.ROUND);
        cutText.setXfermode(clear);
    }

    void setState(boolean sel, int color) {
        selected = sel;
        p.setColor(color);
        num.setColor(color);
        invalidateSelf();
    }

    private Path gearPath() {
        if (gear == null) {
            gear = new Path();
            final int teeth = 8;
            final float rOut = 10.2f, rIn = 7.8f;
            for (int t = 0; t < teeth; t++) {
                double base = Math.toRadians(t * 360.0 / teeth);
                double step = Math.toRadians(360.0 / teeth);
                double[] ang = {base - 0.19, base - 0.12, base + 0.12, base + 0.19};
                float[] rad = {rIn, rOut, rOut, rIn};
                for (int k = 0; k < 4; k++) {
                    float x = 12 + (float) (rad[k] * Math.cos(ang[k]));
                    float y = 12 + (float) (rad[k] * Math.sin(ang[k]));
                    if (t == 0 && k == 0) gear.moveTo(x, y);
                    else gear.lineTo(x, y);
                }
                double from = base + 0.19, to = base + step - 0.19;
                for (int k = 1; k <= 3; k++) {
                    double a = from + (to - from) * k / 4.0;
                    gear.lineTo(12 + (float) (rIn * Math.cos(a)), 12 + (float) (rIn * Math.sin(a)));
                }
            }
            gear.close();
            gear.addCircle(12, 12, 3.3f, Path.Direction.CCW);
            gear.setFillType(Path.FillType.EVEN_ODD);
        }
        return gear;
    }

    private void number(Canvas cv, Paint paint) {
        String day = String.format(Locale.getDefault(), "%d", Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
        Paint.FontMetrics fm = paint.getFontMetrics();
        cv.drawText(day, 12, 15.7f - (fm.ascent + fm.descent) / 2f, paint);
    }

    @Override
    public void draw(@NonNull Canvas cv) {
        Rect b = getBounds();
        if (b.isEmpty()) return;
        float u = b.width() / 24f;
        cv.save();
        cv.translate(b.left, b.top);
        cv.scale(u, u);
        switch (kind) {
            case CAL:
                if (selected) {
                    int layer = cv.saveLayer(0, 0, 24, 24, null);
                    p.setStyle(Paint.Style.FILL);
                    r.set(3, 5, 21, 21);
                    cv.drawRoundRect(r, 3.8f, 3.8f, p);
                    p.setStyle(Paint.Style.STROKE);
                    p.setStrokeWidth(1.9f);
                    cv.drawLine(8, 2.6f, 8, 6.6f, p);
                    cv.drawLine(16, 2.6f, 16, 6.6f, p);
                    cut.setStyle(Paint.Style.STROKE);
                    cut.setStrokeWidth(1.1f);
                    cv.drawLine(3, 10, 21, 10, cut);
                    number(cv, cutText);
                    cv.restoreToCount(layer);
                } else {
                    p.setStyle(Paint.Style.STROKE);
                    p.setStrokeWidth(1.8f);
                    r.set(3, 5, 21, 21);
                    cv.drawRoundRect(r, 3.8f, 3.8f, p);
                    cv.drawLine(3, 10, 21, 10, p);
                    cv.drawLine(8, 2.6f, 8, 6.6f, p);
                    cv.drawLine(16, 2.6f, 16, 6.6f, p);
                    number(cv, num);
                }
                break;
            case LIST:
                float[] ys = {6.5f, 12f, 17.5f};
                for (float y : ys) {
                    p.setStyle(Paint.Style.STROKE);
                    p.setStrokeWidth(selected ? 2.5f : 1.8f);
                    cv.drawLine(9.6f, y, 20.6f, y, p);
                    p.setStyle(Paint.Style.FILL);
                    cv.drawCircle(4.8f, y, selected ? 1.7f : 1.5f, p);
                }
                break;
            case TASKS:
                path.reset();
                path.moveTo(7.7f, 12.4f);
                path.lineTo(10.7f, 15.3f);
                path.lineTo(16.4f, 9f);
                if (selected) {
                    int layer = cv.saveLayer(0, 0, 24, 24, null);
                    p.setStyle(Paint.Style.FILL);
                    cv.drawCircle(12, 12, 9.8f, p);
                    cut.setStyle(Paint.Style.STROKE);
                    cut.setStrokeWidth(2.1f);
                    cv.drawPath(path, cut);
                    cv.restoreToCount(layer);
                } else {
                    p.setStyle(Paint.Style.STROKE);
                    p.setStrokeWidth(1.8f);
                    cv.drawCircle(12, 12, 9f, p);
                    cv.drawPath(path, p);
                }
                break;
            case SEARCH:
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(selected ? 2.6f : 1.9f);
                cv.drawCircle(10.4f, 10.4f, 6.7f, p);
                cv.drawLine(15.4f, 15.4f, 20.7f, 20.7f, p);
                break;
            default:
                if (selected) {
                    p.setStyle(Paint.Style.FILL_AND_STROKE);
                    p.setStrokeWidth(0.9f);
                } else {
                    p.setStyle(Paint.Style.STROKE);
                    p.setStrokeWidth(1.7f);
                }
                cv.drawPath(gearPath(), p);
                break;
        }
        cv.restore();
    }

    @Override
    public void setAlpha(int alpha) {
        p.setAlpha(alpha);
        num.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        p.setColorFilter(cf);
        num.setColorFilter(cf);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
