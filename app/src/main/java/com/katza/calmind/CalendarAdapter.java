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
    private final String currentMonthYear;
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
        holder.eventContainer.removeAllViews();

        if (day != null && !day.isEmpty()) {
            String dateKey = day + "-" + currentMonthYear;
            for (EventModel event : allEvents) {
                if (event.getDateKey() != null && event.getDateKey().equals(dateKey)) {
                    addMiniEvent(holder, event);
                }
            }
            holder.itemView.setOnClickListener(v -> onItemListener.onItemClick(dateKey));
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
            // שימוש בפורמט גמיש שמטפל גם ב-1-5-2026 וגם ב-01-05-2026
            SimpleDateFormat sdf = new SimpleDateFormat("d-M-yyyy HH:mm", Locale.getDefault());
            Date now = new Date();

            String startTimeStr = event.getDateKey() + " " + event.getTime();
            Date start = sdf.parse(startTimeStr);

            Date end = null;
            if (event.getEndTime() != null && !event.getEndTime().isEmpty()) {
                String endTimeStr = event.getDateKey() + " " + event.getEndTime();
                end = sdf.parse(endTimeStr);
            }

            // בדיקה: האם אנחנו בתוך טווח הזמן? (סגול)
            if (end != null && now.getTime() >= start.getTime() && now.getTime() <= end.getTime()) {
                return Color.parseColor("#6200EE");
            }
            // בדיקה: האם האירוע עוד לא התחיל? (ירוק)
            else if (now.before(start)) {
                return Color.parseColor("#4CAF50");
            }
            // כל השאר (עבר) - אפור
            else {
                return Color.parseColor("#9E9E9E");
            }
        } catch (Exception e) {
            Log.e("CalendarColor", "Error parsing date: " + e.getMessage());
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