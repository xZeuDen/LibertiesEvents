package com.example.libertiesevents;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class MapActivity extends AppCompatActivity
        implements OnMapReadyCallback, LocationListener {

    private GoogleMap map;
    private DatabaseReference eventsRef;

    private LocationManager locationManager;
    private Location userLocation;

    private Spinner spinnerRadius;
    private TextView textResults;

    private ArrayList<Event> events = new ArrayList<>();

    private int[] distances = {1, 5, 10};
    private int radius = 5;
    private boolean waitingForLocation = false;
    private boolean eventsLoaded = false;

    private Handler handler = new Handler(Looper.getMainLooper());

    // Stop waiting if no location is found
    private Runnable locationTimeout = () -> {
        stopLocationUpdates();
        textResults.setText("Location not found. Try Near Me again.");
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        spinnerRadius = findViewById(R.id.spinnerRadius);
        textResults = findViewById(R.id.textResults);

        // Set up the distance dropdown
        String[] options = {"1 km", "5 km", "10 km"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, options);

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);

        spinnerRadius.setAdapter(adapter);
        spinnerRadius.setSelection(1);

        // Get the device location manager
        locationManager = (LocationManager)
                getSystemService(LOCATION_SERVICE);

        // Connect to Firebase
        eventsRef = FirebaseDatabase.getInstance(
                "https://libertiesevents-3731b-default-rtdb.europe-west1.firebasedatabase.app"
        ).getReference("events");

        // Get the Google Maps fragment
        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);
    }

    // Called when Google Maps is ready
    @Override
    public void onMapReady(GoogleMap googleMap) {

        map = googleMap;

        LatLng dublin = new LatLng(53.344, -6.267);

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(dublin, 11));
        map.getUiSettings().setZoomControlsEnabled(true);

        // Open event details when the marker name is clicked
        map.setOnInfoWindowClickListener(marker -> {

            String eventId = (String) marker.getTag();

            if (eventId != null) {
                Intent intent = new Intent(MapActivity.this,
                        EventDetailsActivity.class);

                intent.putExtra("event_id", eventId);
                startActivity(intent);
            }
        });

        loadEvents();
    }

    // Read all events from Firebase
    private void loadEvents() {

        eventsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {

                events.clear();

                for (DataSnapshot eventSnapshot : snapshot.getChildren()) {

                    Event event = eventSnapshot.getValue(Event.class);

                    if (event != null) {
                        event.id = eventSnapshot.getKey();
                        events.add(event);
                    }
                }

                eventsLoaded = true;
                displayEvents();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                textResults.setText("Error loading events");
            }
        });
    }

    // Display events based on the selected distance
    private void displayEvents() {

        if (map == null) {
            return;
        }

        map.clear();

        int count = 0;

        for (Event event : events) {

            // Check the distance if Near Me is active
            if (userLocation != null) {

                float[] results = new float[1];

                Location.distanceBetween(
                        userLocation.getLatitude(),
                        userLocation.getLongitude(),
                        event.latitude,
                        event.longitude,
                        results);

                // Skip events outside the selected radius
                if (results[0] > radius * 1000) {
                    continue;
                }
            }

            LatLng position = new LatLng(
                    event.latitude, event.longitude);

            // Add the event marker
            Marker marker = map.addMarker(new MarkerOptions()
                    .position(position)
                    .title(event.title)
                    .snippet(event.venue));

            if (marker != null) {
                marker.setTag(event.id);
            }

            count++;
        }

        // Add a blue marker for the user's location
        if (userLocation != null) {

            LatLng position = new LatLng(
                    userLocation.getLatitude(),
                    userLocation.getLongitude());

            map.addMarker(new MarkerOptions()
                    .position(position)
                    .title("Your Location")
                    .icon(BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_AZURE)));
        }

        // Display the number of events found
        if (waitingForLocation) {
            textResults.setText("Finding your location...");
        } else if (!eventsLoaded) {
            textResults.setText("Loading events...");
        } else if (userLocation != null) {
            textResults.setText(count + " events within " + radius + " km");
        } else {
            textResults.setText(count + " community events");
        }
    }

    // Near Me button
    public void nearMeClick(View view) {

        if (map == null) {
            return;
        }

        // Ask for location permission if needed
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, 1);

            return;
        }

        getUserLocation();
    }

    // Get the current device location
    private void getUserLocation() {

        boolean fineLocation =
                checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;

        boolean coarseLocation =
                checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;

        if (!fineLocation && !coarseLocation) {
            return;
        }

        stopLocationUpdates();

        boolean gps = fineLocation &&
                locationManager.isProviderEnabled(
                        LocationManager.GPS_PROVIDER);

        boolean network = locationManager.isProviderEnabled(
                LocationManager.NETWORK_PROVIDER);

        if (!gps && !network) {
            Toast.makeText(this, "Please enable device location",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        waitingForLocation = true;
        textResults.setText("Finding your location...");

        // Request location updates
        if (gps) {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 1000, 0, this);
        }

        if (network) {
            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, 1000, 0, this);
        }

        handler.postDelayed(locationTimeout, 20000);
    }

    // Called when the device location is found
    @Override
    public void onLocationChanged(Location location) {

        if (!waitingForLocation) {
            return;
        }

        userLocation = location;
        radius = distances[spinnerRadius.getSelectedItemPosition()];

        stopLocationUpdates();

        // Display only nearby events
        displayEvents();

        LatLng position = new LatLng(
                location.getLatitude(), location.getLongitude());

        // Zoom the map based on the selected distance
        float zoom = 12;

        if (radius == 1) {
            zoom = 14;
        } else if (radius == 5) {
            zoom = 12;
        } else {
            zoom = 11;
        }

        map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(position, zoom));
    }

    // Handle the user's permission response
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults);

        if (requestCode == 1) {

            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                getUserLocation();

            } else {
                Toast.makeText(this, "Location permission denied",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    // Stop location updates when no longer needed
    private void stopLocationUpdates() {
        waitingForLocation = false;
        handler.removeCallbacks(locationTimeout);

        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
    }

    // Remove the distance filter
    public void showAllClick(View view) {

        stopLocationUpdates();

        userLocation = null;

        displayEvents();

        if (map != null) {
            LatLng dublin = new LatLng(53.344, -6.267);

            map.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(dublin, 11));
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (waitingForLocation) {
            stopLocationUpdates();
            displayEvents();
        }
    }

    // Return to the main screen
    public void backClick(View view) {
        finish();
    }
}
