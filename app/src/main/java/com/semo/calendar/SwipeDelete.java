package com.semo.calendar;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

/** iOS-style swipe on an event row: a red "Delete" strip is revealed, release asks for confirmation. */
final class SwipeDelete extends ItemTouchHelper.SimpleCallback {
    private final MainActivity a;
    private final Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);

    SwipeDelete(MainActivity act) {
        super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        a = act;
        bg.setColor(Ui.color(a, R.color.bad));
        tp.setColor(0xFFFFFFFF);
        tp.setTextSize(Ui.sp(a, 16));
        tp.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
    }

    @Override
    public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh,
                          @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public int getSwipeDirs(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh) {
        if (vh instanceof EventAdapter.VH && ((EventAdapter.VH) vh).occ != null) return super.getSwipeDirs(rv, vh);
        return 0;
    }

    @Override
    public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder vh) {
        return 0.35f;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int dir) {
        EventAdapter.VH h = (EventAdapter.VH) vh;
        Recur.Occ o = h.occ;
        RecyclerView.Adapter<?> ad = h.getBindingAdapter();
        int pos = h.getBindingAdapterPosition();
        if (ad != null && pos >= 0) ad.notifyItemChanged(pos);
        if (o != null) a.confirmDelete(o.e);
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh,
                            float dX, float dY, int state, boolean active) {
        if (state == ItemTouchHelper.ACTION_STATE_SWIPE && dX != 0) {
            View v = vh.itemView;
            float l = dX > 0 ? v.getLeft() : v.getRight() + dX;
            float r = dX > 0 ? v.getLeft() + dX : v.getRight();
            c.drawRect(l, v.getTop(), r, v.getBottom(), bg);
            String t = a.getString(R.string.delete);
            float tw = tp.measureText(t);
            if (Math.abs(dX) > tw + Ui.dp(a, 36)) {
                Paint.FontMetrics fm = tp.getFontMetrics();
                float x = dX > 0 ? v.getLeft() + Ui.dp(a, 20) : v.getRight() - Ui.dp(a, 20) - tw;
                c.drawText(t, x, (v.getTop() + v.getBottom()) / 2f - (fm.ascent + fm.descent) / 2f, tp);
            }
        }
        super.onChildDraw(c, rv, vh, dX, dY, state, active);
    }
}
