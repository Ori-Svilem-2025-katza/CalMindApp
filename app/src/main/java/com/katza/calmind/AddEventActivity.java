package com.katza.calmind;

import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.util.Calendar;
import java.util.Locale;

public class AddEventActivity extends AppCompatActivity {

    private TextInputEditText etTitle, etLocation, etDate, etStartTime, etEndTime, etReminderTime;
    private Button btnSave;
    private String selectedDateKey, selectedStartTime, selectedEndTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_event);

        etTitle = findViewById(R.id.etTitle);
        etLocation = findViewById(R.id.etLocation);
        etDate = findViewById(R.id.etDate);
        etStartTime = findViewById(R.id.etStartTime);
        etEndTime = findViewById(R.id.etEndTime);
        etReminderTime = findViewById(R.id.etReminderTime);
        btnSave = findViewById(R.id.btnSave);

        etDate.setOnClickListener(v -> showDatePicker());
        etStartTime.setOnClickListener(v -> showTimePicker(true));
        etEndTime.setOnClickListener(v -> showTimePicker(false));
        btnSave.setOnClickListener(v -> saveEvent());
    }

    private void showDatePicker() {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            selectedDateKey = String.format(Locale.getDefault(), "%02d-%02d-%04d", dayOfMonth, (month + 1), year);
            etDate.setText(selectedDateKey);
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTimePicker(boolean isStart) {
        Calendar c = Calendar.getInstance();
        new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            String time = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
            if (isStart) {
                selectedStartTime = time;
                etStartTime.setText(time);
            } else {
                selectedEndTime = time;
                etEndTime.setText(time);
            }
        }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
    }

    private void saveEvent() {
        String title = etTitle.getText().toString().trim();
        String reminderStr = etReminderTime.getText().toString().trim();

        if (title.isEmpty() || selectedDateKey == null || selectedStartTime == null) {
            Toast.makeText(this, "נא למלא כותרת, תאריך ושעת התחלה", Toast.LENGTH_SHORT).show();
            return;
        }

        // חישוב והגדרת התראה
        int reminderMinutes = reminderStr.isEmpty() ? 0 : Integer.parseInt(reminderStr);
        scheduleAlarm(title, reminderMinutes);

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users").child(uid).child("events");
        String eventId = ref.push().getKey();
        EventModel newEvent = new EventModel(title, selectedStartTime, selectedEndTime, selectedDateKey, etLocation.getText().toString(), eventId);

        ref.child(eventId).setValue(newEvent).addOnCompleteListener(task -> {
            Toast.makeText(this, "האירוע נשמר!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void scheduleAlarm(String title, int minutesBefore) {
        try {
            // פירוק התאריך (dd-MM-yyyy) והשעה (HH:mm)
            String[] dateParts = selectedDateKey.split("-");
            String[] timeParts = selectedStartTime.split(":");

            int day = Integer.parseInt(dateParts[0]);
            int month = Integer.parseInt(dateParts[1]) - 1; // Calendar חודשים מתחילים ב-0
            int year = Integer.parseInt(dateParts[2]);
            int hour = Integer.parseInt(timeParts[0]);
            int minute = Integer.parseInt(timeParts[1]);

            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, day, hour, minute, 0);

            // חיסור הזמן שהמשתמש בחר
            calendar.add(Calendar.MINUTE, -minutesBefore);

            // בדיקה שהזמן לא עבר
            if (calendar.getTimeInMillis() <= System.currentTimeMillis()) return;

            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(this, ReminderReceiver.class);
            intent.putExtra("event_title", title);

            // יצירת ID ייחודי מבוסס זמן נוכחי כדי שה-PendingIntent לא יתנגש עם אחרים
            int uniqueId = (int) System.currentTimeMillis();

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    this,
                    uniqueId,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE
            );

            if (alarmManager != null) {
                // כאן הקריאה שבאמת מפעילה את ההתראה ב-AlarmManager
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}