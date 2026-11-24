package com.example.libertiesevents;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ReminderReceiver extends BroadcastReceiver {

    public static final String CHANNEL_ID = "event_reminders";

    @Override
    public void onReceive(Context context, Intent intent) {

        // Get the event information
        String eventId = intent.getStringExtra("event_id");
        String eventTitle = intent.getStringExtra("event_title");
        String userId = intent.getStringExtra("user_id");

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        // Check that the correct user is signed in
        if (user == null || !user.getUid().equals(userId)
                || eventId == null) {
            return;
        }

        // Check notification permission
        if (Build.VERSION.SDK_INT >= 33 &&
                context.checkSelfPermission(
                        Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        NotificationManager manager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Create the notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Event Reminders",
                    NotificationManager.IMPORTANCE_HIGH);

            manager.createNotificationChannel(channel);
        }

        // Open the event when the notification is clicked
        Intent openIntent = new Intent(context,
                EventDetailsActivity.class);

        openIntent.putExtra("event_id", eventId);
        openIntent.setData(intent.getData());

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT |
                        PendingIntent.FLAG_IMMUTABLE);

        // Build the notification
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle("Event Reminder")
                        .setContentText(eventTitle)
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(eventTitle))
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true);

        // Display the notification
        manager.notify(userId + "/" + eventId, 1, builder.build());
    }
}
