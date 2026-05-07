package com.katza.calmind;

import android.os.Bundle;
import android.util.Log;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class PendingRequestsActivity extends AppCompatActivity {
    private ListView lvRequests;
    private List<MeetingRequest> requestList = new ArrayList<>();
    private List<String> displayList = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    // פורמט התאריך כפי שמופיע בצילום המסך שלך (למשל 6-5-2026)
    private SimpleDateFormat sdf = new SimpleDateFormat("d-M-yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_requests);

        lvRequests = findViewById(R.id.lvRequests);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, displayList);
        lvRequests.setAdapter(adapter);

        loadRequests();
        lvRequests.setOnItemClickListener((p, v, pos, id) -> showConfirmDialog(requestList.get(pos)));
    }

    private void loadRequests() {
        String myUid = FirebaseAuth.getInstance().getUid();
        FirebaseDatabase.getInstance().getReference("meeting_requests")
                .orderByChild("receiverUid").equalTo(myUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        requestList.clear(); displayList.clear();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            MeetingRequest r = ds.getValue(MeetingRequest.class);
                            if (r != null && "pending".equals(r.status)) {
                                requestList.add(r);
                                displayList.add("מאת: " + r.senderEmail + "\nטווח: " + r.startDate + " עד " + r.endDate);
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                    @Override public void onCancelled(DatabaseError error) {}
                });
    }

    private void showConfirmDialog(MeetingRequest req) {
        new AlertDialog.Builder(this)
                .setTitle("אישור פגישה חכמה")
                .setMessage("האם למצוא זמן פנוי בין ה-" + req.startDate + " ל-" + req.endDate + "?")
                .setPositiveButton("מצא זמן", (d, w) -> startSmartScheduling(req))
                .setNegativeButton("דחה", (d, w) -> updateStatus(req.requestId, "declined"))
                .show();
    }

    private void startSmartScheduling(MeetingRequest req) {
        List<String> datesInRange = getDatesBetween(req.startDate, req.endDate);
        checkNextDay(req, datesInRange, 0);
    }

    private void checkNextDay(MeetingRequest req, List<String> dates, int index) {
        if (index >= dates.size()) {
            Toast.makeText(this, "לא נמצא זמן פנוי לשניכם בטווח זה.", Toast.LENGTH_LONG).show();
            return;
        }

        String currentDate = dates.get(index);
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");

        usersRef.child(req.senderUid).child("events").orderByChild("dateKey").equalTo(currentDate)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot s1) {
                        usersRef.child(req.receiverUid).child("events").orderByChild("dateKey").equalTo(currentDate)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot s2) {
                                        String freeTime = findCommonSlot(s1, s2, req.preferredTime);
                                        if (freeTime != null) {
                                            finalizeMeeting(req, currentDate, freeTime);
                                        } else {
                                            checkNextDay(req, dates, index + 1);
                                        }
                                    }
                                    @Override public void onCancelled(DatabaseError error) {}
                                });
                    }
                    @Override public void onCancelled(DatabaseError error) {}
                });
    }

    private String findCommonSlot(DataSnapshot s1, DataSnapshot s2, String pref) {
        List<String> possibleSlots = new ArrayList<>();
        if ("בוקר".equals(pref)) possibleSlots.addAll(Arrays.asList("09:00", "10:00", "11:00"));
        else if ("צהריים".equals(pref)) possibleSlots.addAll(Arrays.asList("13:00", "14:00", "15:00"));
        else possibleSlots.addAll(Arrays.asList("19:00", "20:00", "21:00"));

        Set<String> busyTimes = new HashSet<>();
        for (DataSnapshot ds : s1.getChildren()) {
            EventModel e = ds.getValue(EventModel.class);
            if (e != null) busyTimes.add(e.time);
        }
        for (DataSnapshot ds : s2.getChildren()) {
            EventModel e = ds.getValue(EventModel.class);
            if (e != null) busyTimes.add(e.time);
        }

        for (String slot : possibleSlots) {
            if (!busyTimes.contains(slot)) return slot;
        }
        return null;
    }

    private void finalizeMeeting(MeetingRequest req, String date, String time) {
        // חישוב שעת סיום (שעה אחת אחרי)
        String endTime = (Integer.parseInt(time.split(":")[0]) + 1) + ":00";
        if (endTime.length() == 4) endTime = "0" + endTime;

        addEventToUser(req.senderUid, req, date, time, endTime);
        addEventToUser(req.receiverUid, req, date, time, endTime);
        updateStatus(req.requestId, "approved");

        new AlertDialog.Builder(this)
                .setTitle("הצלחה!")
                .setMessage("נקבעה פגישה ב-" + date + " בשעה " + time)
                .setPositiveButton("סגור", (d, w) -> finish())
                .show();
    }

    private void addEventToUser(String uid, MeetingRequest req, String date, String time, String endTime) {
        DatabaseReference eventRef = FirebaseDatabase.getInstance().getReference("users").child(uid).child("events");
        String eventId = eventRef.push().getKey();

        // 1. נחשב את התאריך לתוך משתנה זמני
        String tempDate;
        try {
            Date parsedDate = sdf.parse(date);
            // שים לב: אם ביומן שלך לא רואים כלום, נסה להחליף ל "d-M-yyyy"
            SimpleDateFormat calendarFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
            tempDate = calendarFormat.format(parsedDate);
        } catch (Exception e) {
            tempDate = date;
        }

        // 2. נעביר אותו למשתנה final עבור ה-Lambda
        final String finalDateForFirebase = tempDate;

        HashMap<String, Object> eventData = new HashMap<>();
        eventData.put("id", eventId);
        eventData.put("title", "פגישה עם " + req.senderEmail);
        eventData.put("time", time);
        eventData.put("endTime", endTime);
        eventData.put("locationName", req.location);
        eventData.put("dateKey", finalDateForFirebase);

        if (eventId != null) {
            eventRef.child(eventId).setValue(eventData).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d("CalMind", "האירוע נשמר בהצלחה תחת התאריך: " + finalDateForFirebase);
                }
            });
        }
    }

    private void updateStatus(String requestId, String status) {
        FirebaseDatabase.getInstance().getReference("meeting_requests").child(requestId).child("status").setValue(status);
    }

    private List<String> getDatesBetween(String start, String end) {
        List<String> dates = new ArrayList<>();
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(sdf.parse(start));
            Date endDateObj = sdf.parse(end);
            while (!calendar.getTime().after(endDateObj)) {
                dates.add(sdf.format(calendar.getTime()));
                calendar.add(Calendar.DATE, 1);
            }
        } catch (ParseException e) { e.printStackTrace(); }
        return dates;
    }
}