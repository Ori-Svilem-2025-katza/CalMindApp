package com.katza.calmind;

import android.content.Context;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class GoogleCalendarHelper {

    public interface CalendarCallback {
        void onSuccess(List<EventModel> events);
        void onError(Exception e);
    }

    public static void fetchEvents(Context context, String accountName, CalendarCallback callback) {
        new Thread(() -> {
            try {
                GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                        context, Collections.singleton(CalendarScopes.CALENDAR_EVENTS_READONLY));
                credential.setSelectedAccountName(accountName);

                Calendar service = new Calendar.Builder(
                        new com.google.api.client.http.javanet.NetHttpTransport(),
                        new com.google.api.client.json.gson.GsonFactory(),
                        credential)
                        .setApplicationName("CalMind")
                        .build();

                DateTime now = new DateTime(System.currentTimeMillis());
                Events events = service.events().list("primary")
                        .setTimeMin(now)
                        .setOrderBy("startTime")
                        .setSingleEvents(true)
                        .execute();

                List<Event> items = events.getItems();
                List<EventModel> resultList = new ArrayList<>();

                for (Event event : items) {
                    String googleId = event.getId();
                    String title = event.getSummary() != null ? event.getSummary() : "אירוע ללא כותרת";
                    String loc = event.getLocation() != null ? event.getLocation() : "לא צוין";

                    DateTime start = event.getStart().getDateTime();
                    if (start == null) start = event.getStart().getDate();

                    java.util.Calendar calStart = java.util.Calendar.getInstance();
                    calStart.setTimeInMillis(start.getValue());

                    String timeStr = String.format(Locale.getDefault(), "%02d:%02d",
                            calStart.get(java.util.Calendar.HOUR_OF_DAY),
                            calStart.get(java.util.Calendar.MINUTE));

                    String dateKey = String.format(Locale.getDefault(), "%02d-%02d-%04d",
                            calStart.get(java.util.Calendar.DAY_OF_MONTH),
                            (calStart.get(java.util.Calendar.MONTH) + 1),
                            calStart.get(java.util.Calendar.YEAR));

                    DateTime end = event.getEnd().getDateTime();
                    if (end == null) end = event.getEnd().getDate();

                    java.util.Calendar calEnd = java.util.Calendar.getInstance();
                    calEnd.setTimeInMillis(end.getValue());
                    String endTimeStr = String.format(Locale.getDefault(), "%02d:%02d",
                            calEnd.get(java.util.Calendar.HOUR_OF_DAY),
                            calEnd.get(java.util.Calendar.MINUTE));

                    resultList.add(new EventModel(title, timeStr, endTimeStr, dateKey, loc, googleId));
                }

                callback.onSuccess(resultList);
            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }
}