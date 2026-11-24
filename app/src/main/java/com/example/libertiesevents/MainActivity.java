package com.example.libertiesevents;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
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

    private TextView welcomeText, textEventCount;
    private ListView listEvents;
    private Button buttonAddEvent;
    private Spinner spinnerCategory;

    private ArrayList<Event> events = new ArrayList<>();
    private ArrayList<String> eventNames = new ArrayList<>();
    private ArrayList<String> eventIds = new ArrayList<>();
    private ArrayList<String> categories = new ArrayList<>();

    private ArrayAdapter<String> adapter;
    private ArrayAdapter<String> categoryAdapter;

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
        spinnerCategory = findViewById(R.id.spinnerCategory);
        textEventCount = findViewById(R.id.textEventCount);

        welcomeText.setText("Logged in as: " + user.getEmail());

        // Set up the events list
        adapter = new ArrayAdapter<>(this,
                R.layout.item_event, eventNames);

        listEvents.setAdapter(adapter);

        // Set up the category dropdown
        categories.add("All Categories");

        categoryAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categories);

        categoryAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);

        spinnerCategory.setAdapter(categoryAdapter);

        // Filter events when a category is selected
        spinnerCategory.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(AdapterView<?> parent,
                                               View view, int position,
                                               long id) {

                        String category = parent.getItemAtPosition(position)
                                .toString();

                        displayEvents(category);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                });

        // Open the selected event
        listEvents.setOnItemClickListener((parent, view, position, id) -> {

            String eventId = eventIds.get(position);

            Intent intent = new Intent(MainActivity.this,
                    EventDetailsActivity.class);

            intent.putExtra("event_id", eventId);
            startActivity(intent);
        });

        // Connect to Firebase
        FirebaseDatabase database = FirebaseDatabase.getInstance(
                "https://libertiesevents-3731b-default-rtdb.europe-west1.firebasedatabase.app"
        );

        eventsRef = database.getReference("events");

        loadEvents();

        // Check if the user is an administrator
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

    // Read events and categories from Firebase
    private void loadEvents() {

        eventsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {

                String selectedCategory = "All Categories";

                if (spinnerCategory.getSelectedItem() != null) {
                    selectedCategory = spinnerCategory
                            .getSelectedItem().toString();
                }

                events.clear();
                categories.clear();

                categories.add("All Categories");

                // Go through all events in Firebase
                for (DataSnapshot eventSnapshot : snapshot.getChildren()) {

                    Event event = eventSnapshot.getValue(Event.class);

                    if (event != null) {

                        event.id = eventSnapshot.getKey();
                        events.add(event);

                        // Add each category once
                        if (event.category != null &&
                                !categories.contains(event.category)) {

                            categories.add(event.category);
                        }
                    }
                }

                categoryAdapter.notifyDataSetChanged();

                // Keep the previously selected category
                int position = categories.indexOf(selectedCategory);

                if (position == -1) {
                    position = 0;
                }

                spinnerCategory.setSelection(position);

                displayEvents(categories.get(position));
            }

            @Override
            public void onCancelled(DatabaseError error) {

                textEventCount.setText("Error loading events");

                Toast.makeText(MainActivity.this,
                        "Error loading events: " + error.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    // Display events matching the selected category
    private void displayEvents(String selectedCategory) {

        eventNames.clear();
        eventIds.clear();

        SimpleDateFormat format = new SimpleDateFormat(
                "dd MMM yyyy, HH:mm", Locale.getDefault());

        for (Event event : events) {

            // Check if the category matches
            if (selectedCategory.equals("All Categories") ||
                    selectedCategory.equals(event.category)) {

                String date = format.format(new Date(event.startTime));

                String eventText = event.title + "\n"
                        + event.category + "\n"
                        + date + "\n"
                        + event.venue;

                eventNames.add(eventText);
                eventIds.add(event.id);
            }
        }

        adapter.notifyDataSetChanged();

        textEventCount.setText(eventNames.size() + " events found");
    }

    // Open the administrator form
    public void addEventClick(View view) {
        Intent intent = new Intent(this, AddEventActivity.class);
        startActivity(intent);
    }

    // Open Google Maps
    public void mapClick(View view) {
        Intent intent = new Intent(this, MapActivity.class);
        startActivity(intent);
    }

    // Open the events calendar
    public void calendarClick(View view) {
        Intent intent = new Intent(this, CalendarActivity.class);
        startActivity(intent);
    }

    // Open subscribed events
    public void myEventsClick(View view) {
        Intent intent = new Intent(this, MyEventsActivity.class);
        startActivity(intent);
    }

    // Logout
    public void logoutClick(View view) {
        mAuth.signOut();
        openLogin();
    }

    // Return to login
    private void openLogin() {
        Intent intent = new Intent(this, AuthActivity.class);
        startActivity(intent);
        finish();
    }
}
