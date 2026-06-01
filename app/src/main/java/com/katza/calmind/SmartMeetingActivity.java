package com.katza.calmind;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Pair;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SmartMeetingActivity extends AppCompatActivity {

    private TextInputEditText etPartnerEmail, etLocation;
    private ChipGroup chipGroupTime;
    private Button btnFindTime, btnPickDate;
    private TextView tvSelectedDate;
    private ProgressBar progressBar;

    private String startDate = "";
    private String endDate = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_smart_meeting);

        etPartnerEmail = findViewById(R.id.etPartnerEmail);
        etLocation = findViewById(R.id.etLocation);
        chipGroupTime = findViewById(R.id.chipGroupTime);
        btnFindTime = findViewById(R.id.btnFindTime);
        btnPickDate = findViewById(R.id.btnPickDate);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        progressBar = findViewById(R.id.progressBar);

        btnPickDate.setOnClickListener(v -> {
            MaterialDatePicker<Pair<Long, Long>> dateRangePicker =
                    MaterialDatePicker.Builder.dateRangePicker()
                            .setTitleText("בחר טווח תאריכים לפגישה")
                            .build();

            dateRangePicker.show(getSupportFragmentManager(), "range_picker");

            dateRangePicker.addOnPositiveButtonClickListener(selection -> {
                SimpleDateFormat sdf = new SimpleDateFormat("d-M-yyyy", Locale.getDefault());
                startDate = sdf.format(new Date(selection.first));
                endDate = sdf.format(new Date(selection.second));
                tvSelectedDate.setText("מ-" + startDate + " עד " + endDate);
            });
        });

        btnFindTime.setOnClickListener(v -> findPartnerAndSendRequest());
    }

    private void findPartnerAndSendRequest() {
        String email = etPartnerEmail.getText().toString().trim();
        if (email.isEmpty() || startDate.isEmpty()) {
            Toast.makeText(this, "נא למלא אימייל ולבחור טווח תאריכים", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        String safeEmail = email.replace(".", ",");

        FirebaseDatabase.getInstance().getReference("users_lookup").child(safeEmail)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            sendMeetingRequest(snapshot.getValue(String.class));
                        } else {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(SmartMeetingActivity.this, "משתמש לא נמצא", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override public void onCancelled(DatabaseError error) { progressBar.setVisibility(View.GONE); }
                });
    }

    private void sendMeetingRequest(String partnerUid) {
        String myUid = FirebaseAuth.getInstance().getUid();
        String myEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();

        String pref = "בוקר";
        int idChecked = chipGroupTime.getCheckedChipId();
        if (idChecked == R.id.chipNoon) pref = "צהריים";
        else if (idChecked == R.id.chipEvening) pref = "ערב";

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("meeting_requests");
        String requestId = ref.push().getKey();

        MeetingRequest req = new MeetingRequest(requestId, myUid, myEmail, partnerUid, pref,
                etLocation.getText().toString(), startDate, endDate);

        ref.child(requestId).setValue(req).addOnCompleteListener(task -> {
            progressBar.setVisibility(View.GONE);
            if (task.isSuccessful()) {
                Toast.makeText(this, "בקשת פגישה לטווח התאריכים נשלחה!", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
}