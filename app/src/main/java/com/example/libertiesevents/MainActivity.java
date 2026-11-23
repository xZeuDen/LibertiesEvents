package com.example.libertiesevents;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private TextView welcomeText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        welcomeText = findViewById(R.id.welcomeText);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Get the user who is currently logged in
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            welcomeText.setText("Logged in as:\n" + user.getEmail());
        } else {
            // Return to login if there is no signed-in user
            openLogin();
        }
    }

    // Called when the Logout button is clicked
    public void logoutClick(View view) {
        mAuth.signOut();
        openLogin();
    }

    // Return to the authentication screen
    private void openLogin() {
        Intent intent = new Intent(this, AuthActivity.class);
        startActivity(intent);
        finish();
    }
}
