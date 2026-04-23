package com.katza.calmind;

import android.content.Context;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
// פתרון לשגיאה ב-image_7bd029.png: שימוש ב-NetHttpTransport במקום AndroidHttp
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.client.util.DateTime;

import java.util.Collections;
import java.util.Date;

public class CalendarHelper {

    public static void addEvent(Context ctx, String title, Date date) throws Exception {

        // הגדרת הרשאות הגישה ליומן
        GoogleAccountCredential cred = GoogleAccountCredential.usingOAuth2(
                ctx,
                Collections.singleton("https://www.googleapis.com/auth/calendar"));

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(ctx);

        // פתרון לאזהרת ה-Null Pointer:
        if (account != null && account.getAccount() != null) {
            cred.setSelectedAccount(account.getAccount());
        } else {
            throw new Exception("משתמש לא מחובר לחשבון גוגל");
        }

        // בניית השירות באמצעות NetHttpTransport (פותר את ה-Cannot resolve AndroidHttp)
        com.google.api.services.calendar.Calendar service =
                new com.google.api.services.calendar.Calendar.Builder(
                        new NetHttpTransport(),
                        GsonFactory.getDefaultInstance(),
                        cred
                ).setApplicationName("CalMind").build();

        // יצירת האירוע
        Event event = new Event().setSummary(title);

        DateTime startDateTime = new DateTime(date);
        event.setStart(new EventDateTime().setDateTime(startDateTime));

        // סיום האירוע שעה אחרי ההתחלה
        DateTime endDateTime = new DateTime(new Date(date.getTime() + 3600000));
        event.setEnd(new EventDateTime().setDateTime(endDateTime));

        // הוספה בפועל ליומן של המשתמש
        service.events().insert("primary", event).execute();
    }
}