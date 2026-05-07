package com.katza.calmind;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.Calendar;
import java.util.Locale;

public class AddEventActivity extends AppCompatActivity {

    private TextInputEditText etTitle, etLocation, etDate, etStartTime, etEndTime;
    private Button btnSave;
    private String selectedDateKey, selectedStartTime, selectedEndTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_event);

        // אתחול הרכיבים לפי ה-IDs ב-XML שלך
        etTitle = findViewById(R.id.etTitle);
        etLocation = findViewById(R.id.etLocation);
        etDate = findViewById(R.id.etDate);
        etStartTime = findViewById(R.id.etStartTime);
        etEndTime = findViewById(R.id.etEndTime);
        btnSave = findViewById(R.id.btnSave);

        // הגדרת לחיצות על תיבות הטקסט לפתיחת הבוררים
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
        String location = etLocation.getText().toString().trim();

        if (title.isEmpty() || selectedDateKey == null || selectedStartTime == null) {
            Toast.makeText(this, "נא למלא כותרת, תאריך ושעת התחלה", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users").child(uid).child("events");

        // יצירת מזהה ייחודי למניעת כפילויות
        String eventId = ref.push().getKey();

        // יצירת האובייקט עם 6 פרמטרים (כולל ה-ID)
        EventModel newEvent = new EventModel(title, selectedStartTime, selectedEndTime, selectedDateKey, location, eventId);

        if (eventId != null) {
            ref.child(eventId).setValue(newEvent).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "האירוע נשמר!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "שגיאה בשמירה", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}