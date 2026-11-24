package com.example.libertiesevents;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference eventsRef;

    private TextView welcomeText;
    private ListView listEvents;
    private Button buttonAddEvent;

    private ArrayList<String> eventNames;
    private ArrayList<String> eventIds;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get the logged-in user
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            openLogin();
            return;
        }

        // Connect the XML views
        welcomeText = findViewById(R.id.welcomeText);
        listEvents = findViewById(R.id.listEvents);
        buttonAddEvent = findViewById(R.id.buttonAddEvent);

        welcomeText.setText("Logged in as: " + user.getEmail());

        // Create the event lists
        eventNames = new ArrayList<>();
        eventIds = new ArrayList<>();

        adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, eventNames);

        listEvents.setAdapter(adapter);

        // Open the event selected by the user
        listEvents.setOnItemClickListener((parent, view, position, id) -> {

            String eventId = eventIds.get(position);

            Intent intent = new Intent(MainActivity.this,
                    EventDetailsActivity.class);

            intent.putExtra("event_id", eventId);
            startActivity(intent);
        });

        // Connect to the Firebase database
        FirebaseDatabase database = FirebaseDatabase.getInstance(
                "https://libertiesevents-3731b-default-rtdb.europe-west1.firebasedatabase.app"
        );

        eventsRef = database.getReference("events");

        loadEvents();

        // Check whether the logged-in user is an administrator
        DatabaseReference adminRef = database.getReference("admins")
                .child(user.getUid());

        adminRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {

                Boolean isAdmin = snapshot.getValue(Boolean.class);

                if (Boolean.TRUE.equals(isAdmin)) {
                    buttonAddEvent.setVisibility(View.VISIBLE);
                } else {
                    buttonAddEvent.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                buttonAddEvent.setVisibility(View.GONE);
            }
        });
    }

    // Read all community events from Firebase
    private void loadEvents() {

        eventsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {

                // Clear the old events
                eventNames.clear();
                eventIds.clear();

                SimpleDateFormat format = new SimpleDateFormat(
                        "dd MMM yyyy, HH:mm", Locale.getDefault());

                // Go through all events in Firebase
                for (DataSnapshot eventSnapshot : snapshot.getChildren()) {

                    Event event = eventSnapshot.getValue(Event.class);

                    if (event != null) {

                        String date = format.format(
                                new Date(event.startTime));

                        String eventText = event.title + "\n"
                                + event.category + "\n"
                                + date + "\n"
                                + event.venue;

                        eventNames.add(eventText);
                        eventIds.add(eventSnapshot.getKey());
                    }
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(MainActivity.this,
                        "Error loading events: " + error.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    // Open the administrator event form
    public void addEventClick(View view) {
        Intent intent = new Intent(this, AddEventActivity.class);
        startActivity(intent);
    }

    // Open the user's subscribed events
    public void myEventsClick(View view) {
        Intent intent = new Intent(this, MyEventsActivity.class);
        startActivity(intent);
    }

    // Logout
    public void logoutClick(View view) {
        mAuth.signOut();
        openLogin();
    }

    // Return to the login screen
    private void openLogin() {
        Intent intent = new Intent(this, AuthActivity.class);
        startActivity(intent);
        finish();
    }
}
