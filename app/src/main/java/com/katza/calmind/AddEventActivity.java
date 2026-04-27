package com.katza.calmind;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import java.io.IOException;
import java.util.*;

public class AddEventActivity extends AppCompatActivity {
    private EditText etDate, etStartTime, etEndTime, etTitle, etLocation;
    private String selectedDateKey, startTime, endTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_event);

        // אתחול רכיבים
        etTitle = findViewById(R.id.etTitle);
        etLocation = findViewById(R.id.etLocation);
        etDate = findViewById(R.id.etDate);
        etStartTime = findViewById(R.id.etStartTime);
        etEndTime = findViewById(R.id.etEndTime);
        Button btnSave = findViewById(R.id.btnSave);

        // בחירת תאריך - פותח דיאלוג
        etDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, day) -> {
                selectedDateKey = day + "-" + (month + 1) + "-" + year;
                etDate.setText(day + "/" + (month + 1) + "/" + year);
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        // בחירת שעת התחלה
        etStartTime.setOnClickListener(v -> {
            new TimePickerDialog(this, (view, hour, minute) -> {
                startTime = String.format("%02d:%02d", hour, minute);
                etStartTime.setText(startTime);
            }, 9, 0, true).show();
        });

        // בחירת שעת סיום
        etEndTime.setOnClickListener(v -> {
            new TimePickerDialog(this, (view, hour, minute) -> {
                endTime = String.format("%02d:%02d", hour, minute);
                etEndTime.setText(endTime);
            }, 10, 0, true).show();
        });

        btnSave.setOnClickListener(v -> {
            if(validateInput()) {
                saveToFirebase(etTitle.getText().toString(), startTime, endTime, selectedDateKey, etLocation.getText().toString());
            }
        });
    }

    private void saveToFirebase(String title, String start, String end, String dateKey, String locationName) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        double latitude = 0.0;
        double longitude = 0.0;

        // המרת כתובת למיקום גיאוגרפי (עבור ההתראה החכמה)
        if (!locationName.isEmpty()) {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocationName(locationName, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    latitude = addresses.get(0).getLatitude();
                    longitude = addresses.get(0).getLongitude();
                }
            } catch (IOException e) {
                Log.e("CalMind_Debug", "Geocoder error: " + e.getMessage());
            }
        }

        // יצירת האובייקט עם שעת הסיום החדשה
        EventModel newEvent = new EventModel(title, start, end, dateKey, locationName, latitude, longitude);

        FirebaseDatabase.getInstance().getReference("users")
                .child(uid)
                .child("events")
                .push()
                .setValue(newEvent)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "האירוע נוסף בהצלחה!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "שגיאה בשמירה: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private boolean validateInput() {
        if (etTitle.getText().toString().trim().isEmpty()) {
            etTitle.setError("חובה להזין כותרת");
            return false;
        }
        if (selectedDateKey == null) {
            Toast.makeText(this, "נא לבחור תאריך", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (startTime == null || endTime == null) {
            Toast.makeText(this, "נא לבחור שעות התחלה וסיום", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }
}