package com.katza.calmind;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

public class loginActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private EditText etEmail, etPassword;
    private Button btnLogin, btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);

        auth = FirebaseAuth.getInstance();

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);

        btnLogin.setOnClickListener(v -> loginUser());
        btnRegister.setOnClickListener(v -> registerUser());
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "נא למלא אימייל וסיסמה", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        updateUsersLookup();
                        goToHome();
                    } else {
                        Log.e("AuthError", "Login failed", task.getException());
                        Toast.makeText(this, "התחברות נכשלה: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void registerUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (password.length() < 6) {
            Toast.makeText(this, "סיסמה חייבת להיות לפחות 6 תווים", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        updateUsersLookup();
                        Toast.makeText(this, "נרשמת בהצלחה!", Toast.LENGTH_SHORT).show();
                        goToHome();
                    } else {
                        Log.e("AuthError", "Registration failed", task.getException());
                        Toast.makeText(this, "הרשמה נכשלה", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateUsersLookup() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null && user.getEmail() != null) {
            String uid = user.getUid();
            String safeEmail = user.getEmail().replace(".", ",");
            FirebaseDatabase.getInstance().getReference("users_lookup")
                    .child(safeEmail).setValue(uid);
        }
    }

    private void goToHome() {
        FirebaseUser user = auth.getCurrentUser();
        Intent i = new Intent(this, homeActivity.class);
        if (user != null) {
            i.putExtra("email", user.getEmail());
        }
        startActivity(i);
        finish();
    }
}