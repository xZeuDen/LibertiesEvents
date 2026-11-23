package com.example.libertiesevents;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
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

    private ArrayList<String> eventNames;
    private ArrayList<String> eventIds;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get the current Firebase user
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        // Return to login if the user is not signed in
        if (user == null) {
            openLogin();
            return;
        }

        // Connect the XML views
        welcomeText = findViewById(R.id.welcomeText);
        listEvents = findViewById(R.id.listEvents);

        welcomeText.setText("Logged in as: " + user.getEmail());

        // Create lists for event names and Firebase IDs
        eventNames = new ArrayList<>();
        eventIds = new ArrayList<>();

        adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, eventNames);

        listEvents.setAdapter(adapter);

        // Open the selected event when clicked
        listEvents.setOnItemClickListener((parent, view, position, id) -> {

            String eventId = eventIds.get(position);

            Intent intent = new Intent(MainActivity.this,
                    EventDetailsActivity.class);

            // Pass the selected event ID to the details screen
            intent.putExtra("event_id", eventId);

            startActivity(intent);
        });

        // Connect to the Firebase events database
        eventsRef = FirebaseDatabase.getInstance(
                "https://libertiesevents-3731b-default-rtdb.europe-west1.firebasedatabase.app"
        ).getReference("events");

        loadEvents();
    }

    // Read events from Firebase
    private void loadEvents() {

        eventsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {

                // Clear the old lists before loading events
                eventNames.clear();
                eventIds.clear();

                // Go through each event in Firebase
                for (DataSnapshot eventSnapshot : snapshot.getChildren()) {

                    Event event = eventSnapshot.getValue(Event.class);

                    if (event != null) {

                        // Format the event date
                        SimpleDateFormat format = new SimpleDateFormat(
                                "dd MMM yyyy, HH:mm", Locale.getDefault());

                        String date = format.format(
                                new Date(event.startTime));

                        // Create the text shown in the ListView
                        String eventText = event.title + "\n"
                                + event.category + "\n"
                                + date + "\n"
                                + event.venue;

                        eventNames.add(eventText);

                        // Store the Firebase ID for the selected event
                        eventIds.add(eventSnapshot.getKey());
                    }
                }

                // Refresh the ListView
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

    // Logout button
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
