package com.example.libertiesevents;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CalendarView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class CalendarActivity extends AppCompatActivity {

    private CalendarView calendarView;
    private ListView listCalendarEvents;
    private TextView textSelectedDate, textResults;

    private ArrayList<Event> events = new ArrayList<>();
    private ArrayList<String> eventNames = new ArrayList<>();
    private ArrayList<String> eventIds = new ArrayList<>();

    private ArrayAdapter<String> adapter;

    private Calendar selectedDate = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        // Connect the XML views
        calendarView = findViewById(R.id.calendarView);
        listCalendarEvents = findViewById(R.id.listCalendarEvents);
        textSelectedDate = findViewById(R.id.textSelectedDate);
        textResults = findViewById(R.id.textResults);

        // Set up the event list
        adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, eventNames);

        listCalendarEvents.setAdapter(adapter);

        selectedDate.setTimeInMillis(calendarView.getDate());

        // Get the date selected by the user
        calendarView.setOnDateChangeListener((view, year, month, day) -> {

            selectedDate.set(year, month, day);

            displayEvents();
        });

        // Open the selected event
        listCalendarEvents.setOnItemClickListener(
                (parent, view, position, id) -> {

                    Intent intent = new Intent(CalendarActivity.this,
                            EventDetailsActivity.class);

                    intent.putExtra("event_id", eventIds.get(position));

                    startActivity(intent);
                });

        loadEvents();
    }

    // Read events from Firebase
    private void loadEvents() {

        DatabaseReference eventsRef = FirebaseDatabase.getInstance(
                "https://libertiesevents-3731b-default-rtdb.europe-west1.firebasedatabase.app"
        ).getReference("events");

        eventsRef.addListenerForSingleValueEvent(
                new ValueEventListener() {

                    @Override
                    public void onDataChange(DataSnapshot snapshot) {

                        events.clear();

                        for (DataSnapshot eventSnapshot :
                                snapshot.getChildren()) {

                            Event event = eventSnapshot.getValue(Event.class);

                            if (event != null) {
                                event.id = eventSnapshot.getKey();
                                events.add(event);
                            }
                        }

                        displayEvents();
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {

                        textResults.setText("Error loading events");

                        Toast.makeText(CalendarActivity.this,
                                "Could not load events",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Display events matching the selected date
    private void displayEvents() {

        eventNames.clear();
        eventIds.clear();

        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "dd MMM yyyy", Locale.getDefault());

        SimpleDateFormat timeFormat = new SimpleDateFormat(
                "HH:mm", Locale.getDefault());

        String date = dateFormat.format(selectedDate.getTime());

        textSelectedDate.setText("Events on " + date);

        for (Event event : events) {

            String eventDate = dateFormat.format(
                    new Date(event.startTime));

            // Check if the event is on the selected date
            if (eventDate.equals(date)) {

                String time = timeFormat.format(
                        new Date(event.startTime));

                String eventText = event.title + "\n"
                        + event.category + "\n"
                        + time + "\n"
                        + event.venue;

                eventNames.add(eventText);
                eventIds.add(event.id);
            }
        }

        adapter.notifyDataSetChanged();

        if (eventNames.isEmpty()) {
            textResults.setText("No events on this date");
        } else {
            textResults.setText(eventNames.size() + " events found");
        }
    }

    // Return to the main screen
    public void backClick(View view) {
        finish();
    }
}
