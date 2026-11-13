package com.semo.calendar;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/** One fixed view (the large title, the search field) that scrolls together with a list. */
final class HeaderAdapter extends RecyclerView.Adapter<HeaderAdapter.VH> {
    private final View view;

    HeaderAdapter(View v) {
        view = v;
    }

    static final class VH extends RecyclerView.ViewHolder {
        VH(View v) {
            super(v);
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        if (view.getParent() instanceof ViewGroup) ((ViewGroup) view.getParent()).removeView(view);
        view.setLayoutParams(new RecyclerView.LayoutParams(-1, -2));
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
    }

    @Override
    public int getItemCount() {
        return 1;
    }
}
