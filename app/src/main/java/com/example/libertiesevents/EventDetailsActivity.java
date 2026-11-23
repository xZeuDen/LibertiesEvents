package com.example.libertiesevents;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);

        // Connect the TextViews to the XML layout
        textTitle = findViewById(R.id.textTitle);
        textCategory = findViewById(R.id.textCategory);
        textDate = findViewById(R.id.textDate);
        textVenue = findViewById(R.id.textVenue);
        textAddress = findViewById(R.id.textAddress);
        textOrganizer = findViewById(R.id.textOrganizer);
        textDescription = findViewById(R.id.textDescription);

        // Get the event ID from MainActivity
        String eventId = getIntent().getStringExtra("event_id");

        if (eventId != null) {
            loadEvent(eventId);
        } else {
            Toast.makeText(this, "Event not found",
                    Toast.LENGTH_SHORT).show();
            finish();
        }
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

                    // Display the event details
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

    // Go back to the events list
    public void backClick(View view) {
        finish();
    }
}
