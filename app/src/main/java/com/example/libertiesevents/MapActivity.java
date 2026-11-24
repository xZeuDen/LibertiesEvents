package com.example.libertiesevents;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MapActivity extends AppCompatActivity
        implements OnMapReadyCallback {

    private GoogleMap map;
    private DatabaseReference eventsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // Connect to Firebase
        eventsRef = FirebaseDatabase.getInstance(
                "https://libertiesevents-3731b-default-rtdb.europe-west1.firebasedatabase.app"
        ).getReference("events");

        // Get the map fragment
        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);
    }

    // Called when the map is ready
    @Override
    public void onMapReady(GoogleMap googleMap) {

        map = googleMap;

        // Centre the map on Dublin
        LatLng dublin = new LatLng(53.344, -6.267);

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(dublin, 11));

        // Show zoom buttons
        map.getUiSettings().setZoomControlsEnabled(true);

        // Open event details when the marker information is clicked
        map.setOnInfoWindowClickListener(marker -> {

            String eventId = (String) marker.getTag();

            Intent intent = new Intent(MapActivity.this,
                    EventDetailsActivity.class);

            intent.putExtra("event_id", eventId);
            startActivity(intent);
        });

        loadEvents();
    }

    // Read events from Firebase
    private void loadEvents() {

        eventsRef.addListenerForSingleValueEvent(
                new ValueEventListener() {

                    @Override
                    public void onDataChange(DataSnapshot snapshot) {

                        // Go through all events
                        for (DataSnapshot eventSnapshot :
                                snapshot.getChildren()) {

                            Event event = eventSnapshot.getValue(Event.class);

                            if (event != null) {

                                // Get the event location
                                LatLng location = new LatLng(
                                        event.latitude, event.longitude);

                                // Add a marker for the event
                                Marker marker = map.addMarker(
                                        new MarkerOptions()
                                                .position(location)
                                                .title(event.title)
                                                .snippet(event.venue));

                                // Store the event ID in the marker
                                if (marker != null) {
                                    marker.setTag(eventSnapshot.getKey());
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(MapActivity.this,
                                "Error loading events",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Return to the main screen
    public void backClick(View view) {
        finish();
    }
}
