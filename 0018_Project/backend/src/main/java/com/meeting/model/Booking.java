package com.meeting.model;

import lombok.Data;

@Data
public class Booking {
    private String id;
    private String roomId;
    private String date;
    private String startTime;
    private String endTime;
    private String purpose;
    private String bookerId;
    private String bookerName;
    private Long createdAt;

    public Booking() {
    }
}
