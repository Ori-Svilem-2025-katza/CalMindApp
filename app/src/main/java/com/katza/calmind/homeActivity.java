package com.katza.calmind;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class homeActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private RecyclerView rvEvents;
    private TextView tvName;
    private String selectedDateKey;
    private List<EventModel> allEvents = new ArrayList<>();
    private static final int RC_SIGN_IN = 9001;
    private static final String WEB_CLIENT_ID = "1004619012790-v195f7fi1j7ejri8gu2egu6c2sdmtr0f.apps.googleusercontent.com";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // 1. בקשת הרשאת מיקום בזמן אמת (חיוני להתראה החכמה)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
        }

        auth = FirebaseAuth.getInstance();
        tvName = findViewById(R.id.tvName);
        CalendarView calendarView = findViewById(R.id.calendarView);
        rvEvents = findViewById(R.id.rvEvents);
        Button btnAdd = findViewById(R.id.btnAdd);
        Button btnLogout = findViewById(R.id.btnLogout);
        Button btnSync = findViewById(R.id.btnSync);

        rvEvents.setLayoutManager(new LinearLayoutManager(this));

        if (auth.getCurrentUser() != null) {
            String email = auth.getCurrentUser().getEmail();
            tvName.setText("שלום, " + (email != null ? email.split("@")[0] : "אורח"));
        }

        Calendar today = Calendar.getInstance();
        selectedDateKey = today.get(Calendar.DAY_OF_MONTH) + "-" + (today.get(Calendar.MONTH) + 1) + "-" + today.get(Calendar.YEAR);
        loadEvents(selectedDateKey);

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            selectedDateKey = dayOfMonth + "-" + (month + 1) + "-" + year;
            loadEvents(selectedDateKey);
        });

        btnAdd.setOnClickListener(v -> startActivity(new Intent(this, AddEventActivity.class)));

        btnLogout.setOnClickListener(v -> {
            auth.signOut();
            GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN).signOut().addOnCompleteListener(task -> {
                startActivity(new Intent(this, loginActivity.class));
                finish();
            });
        });

        btnSync.setOnClickListener(v -> signInAndSync());

        // 2. הפעלת מנגנון הבדיקה ברקע (פעם בשעה)
        PeriodicWorkRequest reminderRequest = new PeriodicWorkRequest.Builder(
                SmartReminderWorker.class, 1, TimeUnit.HOURS).build();
        WorkManager.getInstance(this).enqueue(reminderRequest);
    }

    private void signInAndSync() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken(WEB_CLIENT_ID)
                .requestScopes(new Scope("https://www.googleapis.com/auth/calendar.events.readonly"))
                .build();

        GoogleSignInClient client = GoogleSignIn.getClient(this, gso);
        startActivityForResult(client.getSignInIntent(), RC_SIGN_IN);
    }

    private void loadEvents(String dateKey) {
        String uid = auth.getUid();
        if (uid == null) return;

        FirebaseDatabase.getInstance().getReference("users").child(uid).child("events")
                .orderByChild("dateKey").equalTo(dateKey)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        allEvents.clear();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            EventModel model = ds.getValue(EventModel.class);
                            if (model != null) allEvents.add(model);
                        }
                        updateUI(allEvents);
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void updateUI(List<EventModel> events) {
        EventAdapter adapter = new EventAdapter(events, this::showEventDetails);
        rvEvents.setAdapter(adapter);
    }

    private void showEventDetails(EventModel event) {
        String locName = (event.getLocationName() != null && !event.getLocationName().isEmpty()) ? event.getLocationName() : "לא צוין";

        new AlertDialog.Builder(this)
                .setTitle("📅 " + event.getTitle())
                .setMessage("⏰ שעה: " + event.getTime() + "\n📍 מיקום: " + locName)
                .setPositiveButton("סגור", null)
                .setNeutralButton("נווט במפה", (dialog, which) -> {
                    if (!locName.equals("לא צוין")) {
                        Uri mapUri;
                        if (event.getLat() != 0.0 && event.getLng() != 0.0) {
                            mapUri = Uri.parse("geo:" + event.getLat() + "," + event.getLng() + "?q=" + event.getLat() + "," + event.getLng() + "(" + Uri.encode(event.getTitle()) + ")");
                        } else {
                            mapUri = Uri.parse("geo:0,0?q=" + Uri.encode(locName));
                        }
                        Intent mapIntent = new Intent(Intent.ACTION_VIEW, mapUri);
                        mapIntent.setPackage("com.google.android.apps.maps");
                        startActivity(mapIntent);
                    } else {
                        Toast.makeText(this, "אין מיקום מוגדר", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }
}