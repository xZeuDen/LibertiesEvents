package com.example.libertiesevents;

import android.Manifest;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.RatingBar;
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
import java.util.HashMap;
import java.util.Locale;

public class EventDetailsActivity extends AppCompatActivity {

    private TextView textTitle, textCategory, textDate;
    private TextView textVenue, textAddress, textOrganizer;
    private TextView textDescription;

    private Button buttonSubscribe, buttonReminder;
    private Button buttonCancelReminder, buttonFeedback;

    private DatabaseReference subscriptionRef;

    private boolean subscribed = false;

    private String eventId, userId;
    private Event event;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);

        // Connect the XML views
        textTitle = findViewById(R.id.textTitle);
        textCategory = findViewById(R.id.textCategory);
        textDate = findViewById(R.id.textDate);
        textVenue = findViewById(R.id.textVenue);
        textAddress = findViewById(R.id.textAddress);
        textOrganizer = findViewById(R.id.textOrganizer);
        textDescription = findViewById(R.id.textDescription);

        buttonSubscribe = findViewById(R.id.buttonSubscribe);
        buttonReminder = findViewById(R.id.buttonReminder);
        buttonCancelReminder = findViewById(R.id.buttonCancelReminder);
        buttonFeedback = findViewById(R.id.buttonFeedback);

        // Get the selected event and user
        eventId = getIntent().getStringExtra("event_id");

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (eventId == null || user == null) {
            Toast.makeText(this, "Event not available",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        userId = user.getUid();

        // Get the user's subscription
        subscriptionRef = FirebaseDatabase.getInstance(
                        "https://libertiesevents-3731b-default-rtdb.europe-west1.firebasedatabase.app"
                ).getReference("subscriptions")
                .child(userId)
                .child(eventId);

        loadEvent();
    }

    // Read the selected event from Firebase
    private void loadEvent() {

        DatabaseReference eventRef = FirebaseDatabase.getInstance(
                "https://libertiesevents-3731b-default-rtdb.europe-west1.firebasedatabase.app"
        ).getReference("events").child(eventId);

        eventRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {

                event = snapshot.getValue(Event.class);

                if (event != null) {

                    SimpleDateFormat format = new SimpleDateFormat(
                            "dd MMM yyyy, HH:mm", Locale.getDefault());

                    String date = format.format(new Date(event.startTime));

                    // Display event details
                    textTitle.setText(event.title);
                    textCategory.setText(event.category);
                    textDate.setText("Date: " + date);
                    textVenue.setText("Venue: " + event.venue);
                    textAddress.setText("Address: " + event.address);
                    textOrganizer.setText("Organiser: " + event.organizer);
                    textDescription.setText(event.description);

                    buttonFeedback.setEnabled(true);

                    checkSubscription();

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

    // Check if the user is subscribed
    private void checkSubscription() {

        subscriptionRef.addListenerForSingleValueEvent(
                new ValueEventListener() {

                    @Override
                    public void onDataChange(DataSnapshot snapshot) {

                        subscribed = Boolean.TRUE.equals(
                                snapshot.getValue(Boolean.class));

                        buttonSubscribe.setEnabled(true);

                        updateButtons();
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(EventDetailsActivity.this,
                                "Error checking subscription",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Update the subscription buttons
    private void updateButtons() {

        if (subscribed) {
            buttonSubscribe.setText("Unsubscribe");
            buttonReminder.setVisibility(View.VISIBLE);
            buttonCancelReminder.setVisibility(View.VISIBLE);
        } else {
            buttonSubscribe.setText("Subscribe to Event");
            buttonReminder.setVisibility(View.GONE);
            buttonCancelReminder.setVisibility(View.GONE);
        }
    }

    // Subscribe or unsubscribe
    public void subscriptionClick(View view) {

        if (subscribed) {

            subscriptionRef.removeValue()
                    .addOnSuccessListener(unused -> {

                        subscribed = false;

                        cancelReminder();
                        updateButtons();

                        Toast.makeText(this, "Unsubscribed",
                                Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Could not unsubscribe",
                                Toast.LENGTH_SHORT).show();
                    });

        } else {

            subscriptionRef.setValue(true)
                    .addOnSuccessListener(unused -> {

                        subscribed = true;

                        updateButtons();

                        Toast.makeText(this, "Subscribed successfully",
                                Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Could not subscribe",
                                Toast.LENGTH_SHORT).show();
                    });
        }
    }

    // Set Reminder button
    public void reminderClick(View view) {

        if (!subscribed || event == null) {
            return;
        }

        // Ask for notification permission
        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(new String[]{
                    Manifest.permission.POST_NOTIFICATIONS
            }, 2);

            return;
        }

        showReminderOptions();
    }

    // Display reminder options
    private void showReminderOptions() {

        NotificationManager manager = (NotificationManager)
                getSystemService(NOTIFICATION_SERVICE);

        if (!manager.areNotificationsEnabled()) {
            Toast.makeText(this, "Enable notifications in Settings",
                    Toast.LENGTH_LONG).show();
            return;
        }

        AlarmManager alarmManager = (AlarmManager)
                getSystemService(ALARM_SERVICE);

        // Check exact alarm permission
        if (Build.VERSION.SDK_INT >= 31 &&
                !alarmManager.canScheduleExactAlarms()) {

            Toast.makeText(this,
                    "Allow alarms and reminders, then set the reminder again",
                    Toast.LENGTH_LONG).show();

            Intent intent = new Intent(
                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);

            intent.setData(Uri.parse("package:" + getPackageName()));

            startActivity(intent);
            return;
        }

        String[] options = {
                "In 1 minute",
                "10 minutes before the event",
                "1 hour before the event"
        };

        new AlertDialog.Builder(this)
                .setTitle("Set Event Reminder")
                .setItems(options, (dialog, which) -> {

                    long reminderTime;

                    if (which == 0) {
                        reminderTime = System.currentTimeMillis() + 60000L;
                    } else if (which == 1) {
                        reminderTime = event.startTime - 600000L;
                    } else {
                        reminderTime = event.startTime - 3600000L;
                    }

                    scheduleReminder(reminderTime);
                })
                .setNegativeButton("Close", null)
                .show();
    }

    // Create the reminder intent
    private Intent getReminderIntent() {

        Intent intent = new Intent(this, ReminderReceiver.class);

        intent.putExtra("event_id", eventId);
        intent.putExtra("event_title", event.title);
        intent.putExtra("user_id", userId);

        // Identify each user's event reminder
        intent.setData(Uri.parse(
                "libertiesevents://reminder/" + userId + "/" + eventId));

        return intent;
    }

    // Schedule the event reminder
    private void scheduleReminder(long reminderTime) {

        if (reminderTime <= System.currentTimeMillis() ||
                reminderTime >= event.startTime) {

            Toast.makeText(this, "Choose a valid reminder time",
                    Toast.LENGTH_LONG).show();
            return;
        }

        AlarmManager alarmManager = (AlarmManager)
                getSystemService(ALARM_SERVICE);

        if (Build.VERSION.SDK_INT >= 31 &&
                !alarmManager.canScheduleExactAlarms()) {

            Toast.makeText(this, "Allow alarms and reminders first",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                0,
                getReminderIntent(),
                PendingIntent.FLAG_UPDATE_CURRENT |
                        PendingIntent.FLAG_IMMUTABLE);

        // Schedule an exact alarm
        alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminderTime,
                pendingIntent);

        Toast.makeText(this, "Reminder set successfully",
                Toast.LENGTH_SHORT).show();
    }

    // Cancel Reminder button
    public void cancelReminderClick(View view) {

        cancelReminder();

        Toast.makeText(this, "Reminder cleared",
                Toast.LENGTH_SHORT).show();
    }

    // Cancel the scheduled alarm
    private void cancelReminder() {

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                0,
                getReminderIntent(),
                PendingIntent.FLAG_NO_CREATE |
                        PendingIntent.FLAG_IMMUTABLE);

        if (pendingIntent != null) {

            AlarmManager alarmManager = (AlarmManager)
                    getSystemService(ALARM_SERVICE);

            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }

        NotificationManager manager = (NotificationManager)
                getSystemService(NOTIFICATION_SERVICE);

        manager.cancel(userId + "/" + eventId, 1);
    }

    // Handle notification permission
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults);

        if (requestCode == 2) {

            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                showReminderOptions();

            } else {
                Toast.makeText(this, "Notification permission denied",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Open the feedback survey
    public void feedbackClick(View view) {

        DatabaseReference feedbackRef = FirebaseDatabase.getInstance(
                        "https://libertiesevents-3731b-default-rtdb.europe-west1.firebasedatabase.app"
                ).getReference("feedback")
                .child(eventId)
                .child(userId);

        // Check for any previous feedback
        feedbackRef.addListenerForSingleValueEvent(
                new ValueEventListener() {

                    @Override
                    public void onDataChange(DataSnapshot snapshot) {

                        showFeedbackDialog(feedbackRef, snapshot);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {

                        Toast.makeText(EventDetailsActivity.this,
                                "Error loading feedback",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Display the survey form
    private void showFeedbackDialog(DatabaseReference feedbackRef,
                                    DataSnapshot snapshot) {

        View surveyView = getLayoutInflater().inflate(
                R.layout.dialog_feedback, null);

        RatingBar ratingBar = surveyView.findViewById(R.id.ratingBar);
        RadioGroup radioRecommend =
                surveyView.findViewById(R.id.radioRecommend);

        EditText editFeedback =
                surveyView.findViewById(R.id.editFeedback);

        // Display previous answers if they exist
        if (snapshot.exists()) {

            Long rating = snapshot.child("rating").getValue(Long.class);
            Boolean recommend = snapshot.child("recommend")
                    .getValue(Boolean.class);

            String comment = snapshot.child("comment")
                    .getValue(String.class);

            if (rating != null) {
                ratingBar.setRating(rating.floatValue());
            }

            if (recommend != null) {
                if (recommend) {
                    radioRecommend.check(R.id.radioYes);
                } else {
                    radioRecommend.check(R.id.radioNo);
                }
            }

            editFeedback.setText(comment);
        }

        // Create the survey dialog
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Event Feedback")
                .setView(surveyView)
                .setPositiveButton("Submit Feedback", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.show();

        // Submit the feedback
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {

                    int rating = (int) ratingBar.getRating();

                    int selectedAnswer =
                            radioRecommend.getCheckedRadioButtonId();

                    String comment = editFeedback.getText()
                            .toString().trim();

                    // Check that both questions were answered
                    if (rating == 0 || selectedAnswer == -1) {

                        Toast.makeText(this,
                                "Please answer both questions",
                                Toast.LENGTH_SHORT).show();

                        return;
                    }

                    boolean recommend = selectedAnswer == R.id.radioYes;

                    // Store the answers
                    HashMap<String, Object> feedback = new HashMap<>();

                    feedback.put("rating", rating);
                    feedback.put("recommend", recommend);
                    feedback.put("comment", comment);

                    // Save feedback to Firebase
                    feedbackRef.setValue(feedback)
                            .addOnSuccessListener(unused -> {

                                Toast.makeText(this,
                                        "Thank you for your feedback!",
                                        Toast.LENGTH_SHORT).show();

                                dialog.dismiss();
                            })
                            .addOnFailureListener(e -> {

                                Toast.makeText(this,
                                        "Could not save feedback",
                                        Toast.LENGTH_SHORT).show();
                            });
                });
    }

    // Return to the previous screen
    public void backClick(View view) {
        finish();
    }
}
