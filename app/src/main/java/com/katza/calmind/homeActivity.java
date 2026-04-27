package com.katza.calmind;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.Scope;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class homeActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private RecyclerView rvEvents, calendarRecyclerView;
    private TextView tvName, tvMonthYear;
    private Calendar selectedDate;
    private List<EventModel> masterEventsList = new ArrayList<>();
    private static final int RC_SIGN_IN = 9001;
    private static final String WEB_CLIENT_ID = "1004619012790-v195f7fi1j7ejri8gu2egu6c2sdmtr0f.apps.googleusercontent.com";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
        }

        auth = FirebaseAuth.getInstance();
        tvName = findViewById(R.id.tvName);
        tvMonthYear = findViewById(R.id.tvMonthYear);
        calendarRecyclerView = findViewById(R.id.calendarRecyclerView);
        rvEvents = findViewById(R.id.rvEvents);

        Button btnAdd = findViewById(R.id.btnAdd);
        Button btnLogout = findViewById(R.id.btnLogout);
        Button btnSync = findViewById(R.id.btnSync);
        Button btnPrev = findViewById(R.id.btnPrev);
        Button btnNext = findViewById(R.id.btnNext);

        selectedDate = Calendar.getInstance();
        calendarRecyclerView.setLayoutManager(new GridLayoutManager(this, 7));
        rvEvents.setLayoutManager(new LinearLayoutManager(this));

        if (auth.getCurrentUser() != null) {
            String email = auth.getCurrentUser().getEmail();
            tvName.setText("שלום, " + (email != null ? email.split("@")[0] : "אורח"));
        }

        loadAllEventsFromFirebase();

        btnPrev.setOnClickListener(v -> {
            selectedDate.add(Calendar.MONTH, -1);
            setMonthView();
        });

        btnNext.setOnClickListener(v -> {
            selectedDate.add(Calendar.MONTH, 1);
            setMonthView();
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

        PeriodicWorkRequest reminderRequest = new PeriodicWorkRequest.Builder(
                SmartReminderWorker.class, 1, TimeUnit.HOURS).build();
        WorkManager.getInstance(this).enqueue(reminderRequest);
    }

    private void loadAllEventsFromFirebase() {
        String uid = auth.getUid();
        if (uid == null) return;

        FirebaseDatabase.getInstance().getReference("users").child(uid).child("events")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        masterEventsList.clear();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            EventModel model = ds.getValue(EventModel.class);
                            if (model != null) masterEventsList.add(model);
                        }
                        setMonthView();
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void setMonthView() {
        tvMonthYear.setText(monthYearFromDate(selectedDate));
        ArrayList<String> daysInMonth = daysInMonthArray(selectedDate);

        String month = String.valueOf(selectedDate.get(Calendar.MONTH) + 1);
        String year = String.valueOf(selectedDate.get(Calendar.YEAR));
        String monthYearKey = month + "-" + year;

        CalendarAdapter calendarAdapter = new CalendarAdapter(daysInMonth, monthYearKey, masterEventsList, this::filterEventsByDay);
        calendarRecyclerView.setAdapter(calendarAdapter);
    }

    private void filterEventsByDay(String dateKey) {
        List<EventModel> filtered = new ArrayList<>();
        for (EventModel e : masterEventsList) {
            if (e.getDateKey() != null && e.getDateKey().equals(dateKey)) {
                filtered.add(e);
            }
        }
        updateUI(filtered);
    }

    private void updateUI(List<EventModel> events) {
        EventAdapter adapter = new EventAdapter(events, this::showEventDetails);
        rvEvents.setAdapter(adapter);
    }

    private String monthYearFromDate(Calendar calendar) {
        java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("MMMM yyyy", new Locale("he"));
        return formatter.format(calendar.getTime());
    }

    private ArrayList<String> daysInMonthArray(Calendar calendar) {
        ArrayList<String> daysInMonthArray = new ArrayList<>();
        Calendar tempCal = (Calendar) calendar.clone();
        tempCal.set(Calendar.DAY_OF_MONTH, 1);
        int daysInMonth = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH);
        int dayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK) - 1;

        for (int i = 1; i <= 42; i++) {
            if (i <= dayOfWeek || i > daysInMonth + dayOfWeek) {
                daysInMonthArray.add("");
            } else {
                daysInMonthArray.add(String.valueOf(i - dayOfWeek));
            }
        }
        return daysInMonthArray;
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

    private void showEventDetails(EventModel event) {
        if (event == null) return;

        String title = (event.getTitle() != null) ? event.getTitle() : "אירוע ללא שם";
        String location = (event.getLocationName() != null && !event.getLocationName().isEmpty()) ? event.getLocationName() : "לא צוין";

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage("⏰ שעה: " + event.getTime() + "\n📍 מיקום: " + location)
                .setPositiveButton("סגור", null)
                .setNeutralButton("ניווט", (dialog, which) -> {
                    if (!location.equals("לא צוין")) {
                        // פורמט ניווט ישיר שעובד הכי טוב עם גוגל מפות
                        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + Uri.encode(location));
                        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                        mapIntent.setPackage("com.google.android.apps.maps");

                        try {
                            startActivity(mapIntent);
                        } catch (Exception e) {
                            Toast.makeText(this, "גוגל מפות לא מותקנת", Toast.LENGTH_SHORT).show();
                        }
                    }
                }).show();
    }
}