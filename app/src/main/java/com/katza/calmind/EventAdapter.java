package com.katza.calmind;

import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.ViewHolder> {
    private List<EventModel> events;
    private final OnEventClickListener listener;

    public interface OnEventClickListener {
        void onEventClick(EventModel event);
    }

    public EventAdapter(List<EventModel> events, OnEventClickListener listener) {
        this.events = events;
        this.listener = listener;
    }

    public void updateList(List<EventModel> newList) {
        this.events = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EventModel model = events.get(position);
        if (model == null) return;

        holder.tvTitle.setText(model.getTitle());
        holder.tvTime.setText(model.getTime() + (model.getEndTime() != null ? " - " + model.getEndTime() : ""));

        int color = getEventStatusColor(model);
        holder.tvTitle.setTextColor(color);
        holder.tvTime.setTextColor(color);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onEventClick(model);
        });
    }

    private int getEventStatusColor(EventModel event) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("d-M-yyyy HH:mm", Locale.getDefault());
            Date now = new Date();
            Date start = sdf.parse(event.getDateKey() + " " + event.getTime());

            Date end = null;
            if (event.getEndTime() != null && !event.getEndTime().isEmpty()) {
                end = sdf.parse(event.getDateKey() + " " + event.getEndTime());
            }

            long nowMs = now.getTime();
            long startMs = start.getTime();

            if (end != null) {
                long endMs = end.getTime();
                if (nowMs >= startMs && nowMs <= endMs) {
                    return Color.parseColor("#6200EE");
                }
            }

            if (nowMs < startMs) {
                return Color.parseColor("#4CAF50");
            }

            return Color.parseColor("#9E9E9E");
        } catch (Exception e) {
            return Color.parseColor("#9E9E9E");
        }
    }

    @Override
    public int getItemCount() { return (events != null) ? events.size() : 0; }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvTime;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvEventTitle);
            tvTime = itemView.findViewById(R.id.tvEventTime);
        }
    }
}