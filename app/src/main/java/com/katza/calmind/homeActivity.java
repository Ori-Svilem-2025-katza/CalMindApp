package com.katza.calmind;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

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

        auth = FirebaseAuth.getInstance();
        tvName = findViewById(R.id.tvName);
        tvMonthYear = findViewById(R.id.tvMonthYear);
        calendarRecyclerView = findViewById(R.id.calendarRecyclerView);
        rvEvents = findViewById(R.id.rvEvents);

        // אתחול כפתור הלוגו וכפתורי הניווט בלוח
        ImageButton btnMenu = findViewById(R.id.btnMenu);
        Button btnPrev = findViewById(R.id.btnPrev);
        Button btnNext = findViewById(R.id.btnNext);

        calendarRecyclerView.setLayoutManager(new GridLayoutManager(this, 7));
        rvEvents.setLayoutManager(new LinearLayoutManager(this));

        selectedDate = Calendar.getInstance();

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

        // לוגיקה לתפריט ה-PopupMenu שייפתח בלחיצה על הלוגו
        btnMenu.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(homeActivity.this, v);

            // כאן התיקון: אנחנו מנפחים את קובץ ה-Menu שיצרת (נמצא ב-res/menu/)
            popup.getMenuInflater().inflate(R.menu.home_button_menu, popup.getMenu());

            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == R.id.action_add_regular) {
                    startActivity(new Intent(homeActivity.this, AddEventActivity.class));
                    return true;
                } else if (id == R.id.action_smart_meeting) {
                    startActivity(new Intent(homeActivity.this, SmartMeetingActivity.class));
                    return true;
                } else if (id == R.id.action_requests) {
                    startActivity(new Intent(homeActivity.this, PendingRequestsActivity.class));
                    return true;
                } else if (id == R.id.action_sync) {
                    signInAndSync();
                    return true;
                } else if (id == R.id.action_logout) {
                    auth.signOut();
                    GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN).signOut().addOnCompleteListener(task -> {
                        startActivity(new Intent(this, loginActivity.class));
                        finish();
                    });
                    return true;
                }
                return false;
            });
            popup.show();
        });
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
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("Firebase", "Load failed", error.toException());
                    }
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
            if (e.getDateKey() != null && e.getDateKey().equals(dateKey)) filtered.add(e);
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
            if (i <= dayOfWeek || i > daysInMonth + dayOfWeek) daysInMonthArray.add("");
            else daysInMonthArray.add(String.valueOf(i - dayOfWeek));
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) syncWithGoogleCalendar(account.getEmail());
            } catch (ApiException e) { Log.e("CalMind_Debug", "Google sign in failed", e); }
        }
    }

    private void syncWithGoogleCalendar(String email) {
        GoogleCalendarHelper.fetchEvents(this, email, new GoogleCalendarHelper.CalendarCallback() {
            @Override
            public void onSuccess(List<EventModel> events) {
                String uid = auth.getUid();
                if (uid == null) return;
                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users").child(uid).child("events");
                for (EventModel e : events) if (e.getId() != null) ref.child(e.getId()).setValue(e);
                runOnUiThread(() -> Toast.makeText(homeActivity.this, "הסנכרון הושלם!", Toast.LENGTH_SHORT).show());
            }
            @Override
            public void onError(Exception e) { Log.e("CalMind", "Sync error", e); }
        });
    }

    private void showEventDetails(EventModel event) {
        if (event == null) return;
        new AlertDialog.Builder(this)
                .setTitle(event.getTitle())
                .setMessage("⏰ שעה: " + event.getTime() + "\n📍 מיקום: " + event.getLocationName())
                .setPositiveButton("סגור", null)
                .show();
    }
}