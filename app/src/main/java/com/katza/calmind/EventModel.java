package com.katza.calmind;

import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.database.PropertyName;

@IgnoreExtraProperties
public class EventModel {
    public String dateKey;
    public String title;
    public String time;
    public String locationName;
    public double lat;
    public double lng;

    // חובה: בנאי ריק עבור Firebase
    public EventModel() {}

    public EventModel(String title, String time, String dateKey, String locationName, double lat, double lng) {
        this.title = title;
        this.time = time;
        this.dateKey = dateKey;
        this.locationName = locationName;
        this.lat = lat;
        this.lng = lng;
    }

    @PropertyName("dateKey")
    public String getDateKey() { return dateKey; }
    @PropertyName("dateKey")
    public void setDateKey(String dateKey) { this.dateKey = dateKey; }

    @PropertyName("title")
    public String getTitle() { return title; }
    @PropertyName("title")
    public void setTitle(String title) { this.title = title; }

    @PropertyName("time")
    public String getTime() { return time; }
    @PropertyName("time")
    public void setTime(String time) { this.time = time; }

    @PropertyName("locationName")
    public String getLocationName() { return locationName; }
    @PropertyName("locationName")
    public void setLocationName(String locationName) { this.locationName = locationName; }

    @PropertyName("lat")
    public double getLat() { return lat; }
    @PropertyName("lat")
    public void setLat(double lat) { this.lat = lat; }

    @PropertyName("lng")
    public double getLng() { return lng; }
    @PropertyName("lng")
    public void setLng(double lng) { this.lng = lng; }
}