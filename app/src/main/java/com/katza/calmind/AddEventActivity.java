package com.katza.calmind;

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

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_add_event);

        EditText title = findViewById(R.id.etTitle);
        EditText etLocation = findViewById(R.id.etLocation);
        DatePicker dp = findViewById(R.id.datePicker);
        TimePicker tp = findViewById(R.id.timePicker);
        Button save = findViewById(R.id.btnSave);

        save.setOnClickListener(v -> {
            String eventTitle = title.getText().toString().trim();
            String locationStr = etLocation.getText().toString().trim();

            if (eventTitle.isEmpty()) {
                title.setError("נא להזין כותרת");
                return;
            }

            Calendar c = Calendar.getInstance();
            c.set(dp.getYear(), dp.getMonth(), dp.getDayOfMonth(), tp.getHour(), tp.getMinute());

            saveToFirebase(eventTitle, c.getTimeInMillis(),
                    String.format("%02d:%02d", tp.getHour(), tp.getMinute()), locationStr);

            Toast.makeText(this, "האירוע נשמר בהצלחה! ✔", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void saveToFirebase(String title, long timestamp, String timeStr, String locationName) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        double latitude = 0.0;
        double longitude = 0.0;

        // המרת כתובת לקואורדינטות
        if (!locationName.isEmpty()) {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocationName(locationName, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    latitude = addresses.get(0).getLatitude();
                    longitude = addresses.get(0).getLongitude();
                }
            } catch (IOException e) {
                Log.e("CalMind", "Geocoder error", e);
            }
        }

        // יצירת האובייקט באמצעות המודל המעודכן
        EventModel newEvent = new EventModel(title, timeStr, getDateKey(timestamp), locationName, latitude, longitude);

        FirebaseDatabase.getInstance().getReference("users")
                .child(uid)
                .child("events")
                .push()
                .setValue(newEvent);
    }

    private String getDateKey(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        return cal.get(Calendar.DAY_OF_MONTH) + "-" + (cal.get(Calendar.MONTH) + 1) + "-" + cal.get(Calendar.YEAR);
    }
}