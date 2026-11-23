package com.example.libertiesevents;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Event {

    // Event information from Firebase
    public String id;
    public String title;
    public String description;
    public String category;
    public String venue;
    public String address;

    public long startTime;
    public double latitude;
    public double longitude;

    // Empty constructor needed by Firebase
    public Event() {
    }
}
