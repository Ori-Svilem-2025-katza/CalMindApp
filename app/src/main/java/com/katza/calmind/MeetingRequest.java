package com.katza.calmind;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class MeetingRequest {
    public String requestId;
    public String senderUid;
    public String senderEmail;
    public String receiverUid;
    public String status;
    public String preferredTime;
    public String location;
    public String startDate;
    public String endDate;

    public MeetingRequest() {}

    public MeetingRequest(String requestId, String senderUid, String senderEmail, String receiverUid, String preferredTime, String location, String startDate, String endDate) {
        this.requestId = requestId;
        this.senderUid = senderUid;
        this.senderEmail = senderEmail;
        this.receiverUid = receiverUid;
        this.preferredTime = preferredTime;
        this.location = location;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = "pending";
    }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }
}