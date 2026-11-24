package com.example.libertiesevents;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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
import java.util.Date;
import java.util.Locale;

public class EventDetailsActivity extends AppCompatActivity {

    private TextView textTitle, textCategory, textDate;
    private TextView textVenue, textAddress, textOrganizer;
    private TextView textDescription;

    private Button buttonSubscribe;
    private DatabaseReference subscriptionRef;
    private boolean subscribed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);

        // Connect the views to the XML layout
        textTitle = findViewById(R.id.textTitle);
        textCategory = findViewById(R.id.textCategory);
        textDate = findViewById(R.id.textDate);
        textVenue = findViewById(R.id.textVenue);
        textAddress = findViewById(R.id.textAddress);
        textOrganizer = findViewById(R.id.textOrganizer);
        textDescription = findViewById(R.id.textDescription);
        buttonSubscribe = findViewById(R.id.buttonSubscribe);

        // Get the selected event and logged-in user
        String eventId = getIntent().getStringExtra("event_id");
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (eventId == null || user == null) {
            Toast.makeText(this, "Event not available",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Get the current user's subscription for this event
        subscriptionRef = FirebaseDatabase.getInstance(
                        "https://libertiesevents-3731b-default-rtdb.europe-west1.firebasedatabase.app"
                ).getReference("subscriptions")
                .child(user.getUid())
                .child(eventId);

        loadEvent(eventId);
        checkSubscription();
    }

    // Read the selected event from Firebase
    private void loadEvent(String eventId) {

        DatabaseReference eventRef = FirebaseDatabase.getInstance(
                "https://libertiesevents-3731b-default-rtdb.europe-west1.firebasedatabase.app"
        ).getReference("events").child(eventId);

        eventRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {

                Event event = snapshot.getValue(Event.class);

                if (event != null) {

                    // Format the event date
                    SimpleDateFormat format = new SimpleDateFormat(
                            "dd MMM yyyy, HH:mm", Locale.getDefault());

                    String date = format.format(
                            new Date(event.startTime));

                    // Display the event information
                    textTitle.setText(event.title);
                    textCategory.setText(event.category);
                    textDate.setText("Date: " + date);
                    textVenue.setText("Venue: " + event.venue);
                    textAddress.setText("Address: " + event.address);
                    textOrganizer.setText("Organiser: " + event.organizer);
                    textDescription.setText(event.description);

                } else {
                    Toast.makeText(EventDetailsActivity.this,
                            "Event not found",
                            Toast.LENGTH_SHORT).show();
                    buttonSubscribe.setEnabled(false);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(EventDetailsActivity.this,
                        "Error loading event",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    // Check if the user has already subscribed
    private void checkSubscription() {

        subscriptionRef.addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {

                        subscribed = snapshot.exists();

                        if (subscribed) {
                            buttonSubscribe.setText("Unsubscribe");
                        } else {
                            buttonSubscribe.setText("Subscribe to Event");
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(EventDetailsActivity.this,
                                "Error checking subscription",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Subscribe or unsubscribe when the button is clicked
    public void subscriptionClick(View view) {

        if (subscribed) {

            // Remove the user's subscription
            subscriptionRef.removeValue()
                    .addOnSuccessListener(unused -> {
                        subscribed = false;
                        buttonSubscribe.setText("Subscribe to Event");

                        Toast.makeText(this, "Unsubscribed",
                                Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Could not unsubscribe",
                                Toast.LENGTH_SHORT).show();
                    });

        } else {

            // Save the user's subscription in Firebase
            subscriptionRef.setValue(true)
                    .addOnSuccessListener(unused -> {
                        subscribed = true;
                        buttonSubscribe.setText("Unsubscribe");

                        Toast.makeText(this, "Subscribed successfully",
                                Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Could not subscribe",
                                Toast.LENGTH_SHORT).show();
                    });
        }
    }

    // Return to the events list
    public void backClick(View view) {
        finish();
    }
}
