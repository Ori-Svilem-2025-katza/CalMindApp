package com.katza.calmind;

import android.content.Context;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
                    String title = event.getSummary() != null ? event.getSummary() : "אירוע ללא כותרת";
                    String loc = event.getLocation() != null ? event.getLocation() : "לא צוין";

                    // טיפול בזמן ותאריך
                    DateTime start = event.getStart().getDateTime();
                    if (start == null) start = event.getStart().getDate();

                    java.util.Calendar cal = java.util.Calendar.getInstance();
                    cal.setTimeInMillis(start.getValue());

                    String timeStr = String.format("%02d:%02d", cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE));
                    String dateKey = cal.get(java.util.Calendar.DAY_OF_MONTH) + "-" + (cal.get(java.util.Calendar.MONTH) + 1) + "-" + cal.get(java.util.Calendar.YEAR);

                    // כאן התיקון: יצירת EventModel עם כל 6 הפרמטרים (קואורדינטות יהיו 0 בסינכרון ראשוני)
                    resultList.add(new EventModel(title, timeStr, dateKey, loc, 0.0, 0.0));
                }

                callback.onSuccess(resultList);
            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }
}