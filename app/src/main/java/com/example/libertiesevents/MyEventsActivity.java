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

public class MyEventsActivity extends AppCompatActivity {

    private DatabaseReference subscriptionsRef, eventsRef;

    private ListView listMyEvents;
    private TextView textEmpty;

    private ArrayList<String> eventNames;
    private ArrayList<String> eventIds;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_events);

        // Get the logged-in user
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            finish();
            return;
        }

        // Connect the XML views
        listMyEvents = findViewById(R.id.listMyEvents);
        textEmpty = findViewById(R.id.textEmpty);

        // Set up the ListView
        eventNames = new ArrayList<>();
        eventIds = new ArrayList<>();

        adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, eventNames);

        listMyEvents.setAdapter(adapter);

        // Connect to Firebase
        FirebaseDatabase database = FirebaseDatabase.getInstance(
                "https://libertiesevents-3731b-default-rtdb.europe-west1.firebasedatabase.app"
        );

        subscriptionsRef = database.getReference("subscriptions")
                .child(user.getUid());

        eventsRef = database.getReference("events");

        // Open an event when selected
        listMyEvents.setOnItemClickListener((parent, view, position, id) -> {

            String eventId = eventIds.get(position);

            Intent intent = new Intent(MyEventsActivity.this,
                    EventDetailsActivity.class);

            intent.putExtra("event_id", eventId);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Refresh the list when returning to this screen
        if (subscriptionsRef != null) {
            loadMyEvents();
        }
    }

    // Read the user's subscriptions from Firebase
    private void loadMyEvents() {

        subscriptionsRef.addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot subscriptions) {

                        // Get the event details for subscribed events
                        eventsRef.addListenerForSingleValueEvent(
                                new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot events) {

                                        eventNames.clear();
                                        eventIds.clear();

                                        SimpleDateFormat format =
                                                new SimpleDateFormat(
                                                        "dd MMM yyyy, HH:mm",
                                                        Locale.getDefault());

                                        // Go through the user's subscriptions
                                        for (DataSnapshot saved :
                                                subscriptions.getChildren()) {

                                            Boolean isSubscribed =
                                                    saved.getValue(Boolean.class);

                                            if (!Boolean.TRUE.equals(isSubscribed)) {
                                                continue;
                                            }

                                            // Find the matching event
                                            Event event = events.child(saved.getKey())
                                                    .getValue(Event.class);

                                            if (event != null) {

                                                String date = format.format(
                                                        new Date(event.startTime));

                                                String eventText = event.title + "\n"
                                                        + event.category + "\n"
                                                        + date + "\n"
                                                        + event.venue;

                                                eventNames.add(eventText);
                                                eventIds.add(saved.getKey());
                                            }
                                        }

                                        // Update the list
                                        adapter.notifyDataSetChanged();

                                        if (eventNames.isEmpty()) {
                                            textEmpty.setVisibility(View.VISIBLE);
                                        } else {
                                            textEmpty.setVisibility(View.GONE);
                                        }
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError error) {
                                        Toast.makeText(MyEventsActivity.this,
                                                "Error loading events",
                                                Toast.LENGTH_LONG).show();
                                    }
                                });
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(MyEventsActivity.this,
                                "Error loading subscriptions",
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    // Return to the main screen
    public void backClick(View view) {
        finish();
    }
}