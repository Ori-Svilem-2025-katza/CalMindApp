package com.katza.calmind;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;

public class SmartReminderWorker extends Worker {

    public SmartReminderWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return Result.success();

        // משיכת כל האירועים כדי למצוא את האירוע הבא עם מיקום
        FirebaseDatabase.getInstance().getReference("users").child(uid).child("events")
                .get().addOnSuccessListener(snapshot -> {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        EventModel event = ds.getValue(EventModel.class);
                        if (event != null && event.getLat() != 0.0) {
                            calculateAndNotify(event);
                            // כרגע בודק אירוע אחד לצורך הבדיקה
                        }
                    }
                });

        return Result.success();
    }

    private void calculateAndNotify(EventModel event) {
        // הגדרת מיקום ידני של ישראל (תל אביב) לצורך הבדיקה באמולטור
        double manualLat = 32.0853;
        double manualLng = 34.7818;

        Log.d("CalMind_Debug", "משתמש במיקום ידני: " + manualLat + ", " + manualLng);
        Log.d("CalMind_Debug", "יעד האירוע: " + event.getLat() + ", " + event.getLng());

        // קריאה ל-Helper עם המיקום הידני
        TravelTimeHelper.getTravelTime(getApplicationContext(),
                manualLat, manualLng,
                event.getLat(), event.getLng(), new TravelTimeHelper.TravelTimeCallback() {
                    @Override
                    public void onTimeRetrieved(int minutes) {
                        NotificationHelper.showNotification(getApplicationContext(),
                                "זמן לצאת ל-" + event.getTitle(),
                                "ייקח לך כ-" + minutes + " דקות נסיעה בדרך הנוכחית.");
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e("CalMind_Debug", "שגיאה בחישוב זמן: " + e.getMessage());
                    }
                });

        /* הערה: קוד ה-GPS המקורי הוסר זמנית כדי לוודא שה-Hardcoded עובד.
        ברגע שהכל יעבוד, תוכל להחזיר את ה-locationClient.
        */
    }
}