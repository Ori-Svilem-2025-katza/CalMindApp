package com.katza.calmind;

import android.graphics.Color;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder> {
    private final ArrayList<String> daysOfMonth;
    private final String currentMonthYear; // פורמט צפוי: "M-yyyy"
    private final List<EventModel> allEvents;
    private final OnItemListener onItemListener;

    public interface OnItemListener {
        void onItemClick(String dateKey);
    }

    public CalendarAdapter(ArrayList<String> daysOfMonth, String monthYear, List<EventModel> allEvents, OnItemListener onItemListener) {
        this.daysOfMonth = daysOfMonth;
        this.currentMonthYear = monthYear;
        this.allEvents = allEvents;
        this.onItemListener = onItemListener;
    }

    @NonNull
    @Override
    public CalendarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.calendar_cell, parent, false);
        return new CalendarViewHolder(view, onItemListener);
    }

    @Override
    public void onBindViewHolder(@NonNull CalendarViewHolder holder, int position) {
        String day = daysOfMonth.get(position);
        holder.dayText.setText(day);

        // הגדרת צבע המספרים ללבן
        holder.dayText.setTextColor(Color.WHITE);

        holder.eventContainer.removeAllViews();

        if (day != null && !day.isEmpty()) {
            String dateKey = formatToStandardDate(day, currentMonthYear);

            for (EventModel event : allEvents) {
                if (event.getDateKey() != null && event.getDateKey().equals(dateKey)) {
                    addMiniEvent(holder, event);
                }
            }
            holder.itemView.setOnClickListener(v -> onItemListener.onItemClick(dateKey));
        }
    }

    private String formatToStandardDate(String day, String monthYear) {
        try {
            String[] parts = monthYear.split("-");
            int dayInt = Integer.parseInt(day);
            int monthInt = Integer.parseInt(parts[0]);
            int yearInt = Integer.parseInt(parts[1]);
            return String.format(Locale.getDefault(), "%02d-%02d-%04d", dayInt, monthInt, yearInt);
        } catch (Exception e) {
            return day + "-" + monthYear; // Fallback
        }
    }

    private void addMiniEvent(CalendarViewHolder holder, EventModel event) {
        TextView tv = new TextView(holder.itemView.getContext());
        tv.setText(event.getTitle());
        tv.setTextSize(8f);
        tv.setMaxLines(1);
        tv.setEllipsize(TextUtils.TruncateAt.END);
        tv.setPadding(4, 2, 4, 2);

        int statusColor = getEventStatusColor(event);
        tv.setBackgroundColor(statusColor);
        tv.setTextColor(Color.WHITE);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 2, 0, 0);
        tv.setLayoutParams(params);

        holder.eventContainer.addView(tv);
        if (holder.eventContainer.getChildCount() >= 3) return;
    }

    private int getEventStatusColor(EventModel event) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault());
            Date now = new Date();
            Date start = sdf.parse(event.getDateKey() + " " + event.getTime());

            if (event.getEndTime() != null && !event.getEndTime().isEmpty()) {
                Date end = sdf.parse(event.getDateKey() + " " + event.getEndTime());
                if (now.after(start) && now.before(end)) return Color.parseColor("#6200EE");
            }

            if (now.before(start)) return Color.parseColor("#4CAF50");
            return Color.parseColor("#9E9E9E");
        } catch (Exception e) {
            return Color.parseColor("#9E9E9E");
        }
    }

    @Override
    public int getItemCount() { return daysOfMonth.size(); }

    public static class CalendarViewHolder extends RecyclerView.ViewHolder {
        public final TextView dayText;
        public final LinearLayout eventContainer;
        public CalendarViewHolder(@NonNull View itemView, OnItemListener onItemListener) {
            super(itemView);
            dayText = itemView.findViewById(R.id.cellDayText);
            eventContainer = itemView.findViewById(R.id.eventIndicatorContainer);
        }
    }
}