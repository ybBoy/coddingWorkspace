package com.meeting.model;

import lombok.Data;
import java.util.List;

@Data
public class MeetingRoom {
    private String id;
    private String name;
    private int capacity;
    private List<Booking> bookings;

    public MeetingRoom() {
    }

    public MeetingRoom(String id, String name, int capacity) {
        this.id = id;
        this.name = name;
        this.capacity = capacity;
    }
}
