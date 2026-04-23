package com.katza.calmind;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    private List<EventModel> eventList;
    private OnItemClickListener listener; // משתנה ששומר את הפעולה של הלחיצה

    // הממשק שמאפשר ל-homeActivity "להקשיב" ללחיצות
    public interface OnItemClickListener {
        void onItemClick(EventModel event);
    }

    // הקונסטרקטור המעודכן - מקבל גם רשימה וגם לחיצה (2 פרמטרים)
    public EventAdapter(List<EventModel> eventList, OnItemClickListener listener) {
        this.eventList = eventList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // שימוש בעיצוב שורות מובנה של אנדרואיד
        View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        EventModel event = eventList.get(position);

        // חיבור הנתונים (חובה שיהיו Getters ב-EventModel)
        holder.tvTitle.setText(event.getTitle());
        holder.tvTime.setText(event.getTime());

        // כאן קורה הקסם: כשלוחצים על השורה, מפעילים את ה-Listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(event);
            }
        });
    }

    @Override
    public int getItemCount() {
        return eventList == null ? 0 : eventList.size();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvTime;
        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            // text1 ו-text2 הם ה-IDs של simple_list_item_2
            tvTitle = itemView.findViewById(android.R.id.text1);
            tvTime = itemView.findViewById(android.R.id.text2);
        }
    }
}