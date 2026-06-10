package com.meeting.controller;

import com.meeting.common.Result;
import com.meeting.model.Booking;
import com.meeting.model.MeetingRoom;
import com.meeting.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @GetMapping("/meeting-rooms")
    public Result<List<MeetingRoom>> getMeetingRooms(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestParam(value = "scope", required = false, defaultValue = "all") String scope) {
        if ("today".equals(scope)) {
            return Result.success(bookingService.getTodayMeetingRoomsWithBookings());
        }
        return Result.success(bookingService.getAllMeetingRoomsWithBookings());
    }

    @PostMapping("/bookings")
    public Result<Booking> createBooking(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody Map<String, String> request) {
        try {
            String roomId = request.get("roomId");
            String date = request.get("date");
            String startTime = request.get("startTime");
            String endTime = request.get("endTime");
            String purpose = request.get("purpose");

            Booking booking = bookingService.createBooking(roomId, date, startTime, endTime, purpose, userId);
            return Result.success(booking);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/bookings/{bookingId}")
    public Result<Void> cancelBooking(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String bookingId) {
        try {
            bookingService.cancelBooking(bookingId, userId);
            return Result.success();
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }
}
