package com.katza.calmind;

import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.database.PropertyName;

@IgnoreExtraProperties
public class EventModel {
    public String id; // מזהה ייחודי למניעת כפילויות
    public String dateKey;
    public String title;
    public String time;      // שעת התחלה
    public String endTime;   // שעת סיום
    public String locationName;

    public EventModel() {
        // נדרש עבור Firebase
    }

    public EventModel(String title, String time, String endTime, String dateKey, String locationName, String id) {
        this.title = title;
        this.time = time;
        this.endTime = endTime;
        this.dateKey = dateKey;
        this.locationName = locationName;
        this.id = id;
    }

    @PropertyName("id")
    public String getId() { return id; }
    @PropertyName("id")
    public void setId(String id) { this.id = id; }

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

    @PropertyName("endTime")
    public String getEndTime() { return endTime; }
    @PropertyName("endTime")
    public void setEndTime(String endTime) { this.endTime = endTime; }

    @PropertyName("locationName")
    public String getLocationName() { return locationName; }
    @PropertyName("locationName")
    public void setLocationName(String locationName) { this.locationName = locationName; }
}