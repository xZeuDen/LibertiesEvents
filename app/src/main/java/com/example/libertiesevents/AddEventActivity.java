package com.example.libertiesevents;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AddEventActivity extends AppCompatActivity {

    private EditText editTitle, editDescription, editCategory;
    private EditText editOrganizer, editVenue, editAddress;
    private EditText editLatitude, editLongitude;
    private EditText editDuration, editCapacity;
    private CheckBox checkFree;
    private Button buttonDate, buttonTime;

    private Calendar eventDate = Calendar.getInstance();
    private boolean dateSelected = false;
    private boolean timeSelected = false;

    private DatabaseReference eventsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_event);

        // Connect the form fields
        editTitle = findViewById(R.id.editTitle);
        editDescription = findViewById(R.id.editDescription);
        editCategory = findViewById(R.id.editCategory);
        editOrganizer = findViewById(R.id.editOrganizer);
        editVenue = findViewById(R.id.editVenue);
        editAddress = findViewById(R.id.editAddress);
        editLatitude = findViewById(R.id.editLatitude);
        editLongitude = findViewById(R.id.editLongitude);
        editDuration = findViewById(R.id.editDuration);
        editCapacity = findViewById(R.id.editCapacity);
        checkFree = findViewById(R.id.checkFree);

        buttonDate = findViewById(R.id.buttonDate);
        buttonTime = findViewById(R.id.buttonTime);

        // Connect to Firebase events
        eventsRef = FirebaseDatabase.getInstance(
                "https://libertiesevents-3731b-default-rtdb.europe-west1.firebasedatabase.app"
        ).getReference("events");
    }

    // Choose the event date
    public void dateClick(View view) {

        int year = eventDate.get(Calendar.YEAR);
        int month = eventDate.get(Calendar.MONTH);
        int day = eventDate.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog picker = new DatePickerDialog(this,
                (datePicker, y, m, d) -> {

                    eventDate.set(y, m, d);
                    dateSelected = true;

                    SimpleDateFormat format = new SimpleDateFormat(
                            "dd MMM yyyy", Locale.getDefault());

                    buttonDate.setText(format.format(eventDate.getTime()));
                }, year, month, day);

        picker.show();
    }

    // Choose the event start time
    public void timeClick(View view) {

        int hour = eventDate.get(Calendar.HOUR_OF_DAY);
        int minute = eventDate.get(Calendar.MINUTE);

        TimePickerDialog picker = new TimePickerDialog(this,
                (timePicker, h, m) -> {

                    eventDate.set(Calendar.HOUR_OF_DAY, h);
                    eventDate.set(Calendar.MINUTE, m);
                    timeSelected = true;

                    buttonTime.setText(String.format(
                            Locale.getDefault(), "%02d:%02d", h, m));

                }, hour, minute, true);

        picker.show();
    }

    // Save the new event to Firebase
    public void saveEventClick(View view) {

        String title = editTitle.getText().toString().trim();
        String description = editDescription.getText().toString().trim();
        String category = editCategory.getText().toString().trim();
        String organizer = editOrganizer.getText().toString().trim();
        String venue = editVenue.getText().toString().trim();
        String address = editAddress.getText().toString().trim();

        String latText = editLatitude.getText().toString().trim();
        String lngText = editLongitude.getText().toString().trim();
        String durationText = editDuration.getText().toString().trim();
        String capacityText = editCapacity.getText().toString().trim();

        // Check that the administrator completed the form
        if (title.isEmpty() || description.isEmpty() ||
                category.isEmpty() || organizer.isEmpty() ||
                venue.isEmpty() || address.isEmpty() ||
                latText.isEmpty() || lngText.isEmpty() ||
                durationText.isEmpty() || capacityText.isEmpty() ||
                !dateSelected || !timeSelected) {

            Toast.makeText(this, "Please complete all fields",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        double latitude, longitude;
        int duration, capacity;

        // Check the numeric values
        try {
            latitude = Double.parseDouble(latText);
            longitude = Double.parseDouble(lngText);
            duration = Integer.parseInt(durationText);
            capacity = Integer.parseInt(capacityText);

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Check the numbers entered",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Double.isFinite(latitude) || !Double.isFinite(longitude) ||
                latitude < -90 || latitude > 90 ||
                longitude < -180 || longitude > 180 ||
                duration <= 0 || capacity <= 0) {

            Toast.makeText(this, "Check coordinates, duration and capacity",
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Get the event start and end times
        eventDate.set(Calendar.SECOND, 0);
        eventDate.set(Calendar.MILLISECOND, 0);

        long startTime = eventDate.getTimeInMillis();
        long endTime = startTime + duration * 60000L;

        if (startTime < System.currentTimeMillis()) {
            Toast.makeText(this, "Choose a future date and time",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Please log in again",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Generate a unique Firebase event ID
        DatabaseReference newEventRef = eventsRef.push();
        String eventId = newEventRef.getKey();

        if (eventId == null) {
            Toast.makeText(this, "Could not create event ID",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Store event information using the same fields as our JSON
        Map<String, Object> event = new HashMap<>();
        event.put("id", eventId);
        event.put("title", title);
        event.put("description", description);
        event.put("category", category);
        event.put("organizer", organizer);
        event.put("venue", venue);
        event.put("address", address);
        event.put("latitude", latitude);
        event.put("longitude", longitude);
        event.put("startTime", startTime);
        event.put("endTime", endTime);
        event.put("capacity", capacity);
        event.put("isFree", checkFree.isChecked());

        // Add the new event to Firebase
        newEventRef.setValue(event)
                .addOnSuccessListener(unused -> {

                    Toast.makeText(this, "Event added successfully",
                            Toast.LENGTH_SHORT).show();

                    finish();
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(this,
                            "Error adding event: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    // Return to the main screen
    public void backClick(View view) {
        finish();
    }
}
