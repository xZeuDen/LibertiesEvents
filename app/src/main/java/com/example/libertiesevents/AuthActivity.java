package com.example.libertiesevents;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class AuthActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private EditText editEmail, editPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        // Connect to Firebase Authentication
        mAuth = FirebaseAuth.getInstance();

        // Get the email and password fields
        editEmail = findViewById(R.id.editEmail);
        editPassword = findViewById(R.id.editPassword);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Check if the user is already logged in
        if (mAuth.getCurrentUser() != null) {
            openMainActivity();
        }
    }

    // Login button
    public void loginClick(View view) {
        String email = editEmail.getText().toString().trim();
        String password = editPassword.getText().toString();

        if (!validateForm(email, password)) {
            return;
        }

        // Login using Firebase
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Login successful",
                                Toast.LENGTH_SHORT).show();

                        openMainActivity();
                    } else {
                        Toast.makeText(this, "Login failed. Check your details.",
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    // Register button
    public void registerClick(View view) {
        String email = editEmail.getText().toString().trim();
        String password = editPassword.getText().toString();

        if (!validateForm(email, password)) {
            return;
        }

        // Create a new Firebase account
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Account created",
                                Toast.LENGTH_SHORT).show();

                        openMainActivity();
                    } else {
                        String message = "Registration failed";

                        if (task.getException() != null) {
                            message = task.getException().getMessage();
                        }

                        Toast.makeText(this, message,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    // Check email and password
    private boolean validateForm(String email, String password) {

        if (TextUtils.isEmpty(email)) {
            editEmail.setError("Enter your email");
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editEmail.setError("Enter a valid email");
            return false;
        }

        if (TextUtils.isEmpty(password) || password.length() < 6) {
            editPassword.setError("Password must have 6 characters");
            return false;
        }

        return true;
    }

    // Open the main screen
    private void openMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
