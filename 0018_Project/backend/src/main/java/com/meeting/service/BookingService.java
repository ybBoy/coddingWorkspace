package com.meeting.service;

import com.meeting.model.Booking;
import com.meeting.model.MeetingRoom;
import com.meeting.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class BookingService {

    @Autowired
    private DataStoreService dataStoreService;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private int timeToMinutes(String time) {
        if ("24:00".equals(time)) {
            return 24 * 60;
        }
        String[] parts = time.split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }

    public List<MeetingRoom> getAllMeetingRoomsWithBookings() {
        List<MeetingRoom> rooms = dataStoreService.getMeetingRooms();
        for (MeetingRoom room : rooms) {
            if (room.getBookings() != null) {
                room.getBookings().sort(Comparator.comparing(Booking::getDate)
                        .thenComparing(Booking::getStartTime));
            }
        }
        return rooms;
    }

    public List<MeetingRoom> getTodayMeetingRoomsWithBookings() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        List<MeetingRoom> allRooms = getAllMeetingRoomsWithBookings();
        List<MeetingRoom> result = new ArrayList<>();
        for (MeetingRoom room : allRooms) {
            MeetingRoom newRoom = new MeetingRoom(room.getId(), room.getName(), room.getCapacity());
            List<Booking> todayBookings = new ArrayList<>();
            if (room.getBookings() != null) {
                for (Booking booking : room.getBookings()) {
                    if (today.equals(booking.getDate())) {
                        todayBookings.add(booking);
                    }
                }
            }
            newRoom.setBookings(todayBookings);
            result.add(newRoom);
        }
        return result;
    }

    public Booking createBooking(String roomId, String date, String startTime, 
                                  String endTime, String purpose, String bookerId) {
        User booker = dataStoreService.getUserById(bookerId);
        if (booker == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        validateTime(date, startTime, endTime);

        MeetingRoom room = dataStoreService.getMeetingRoomById(roomId);
        if (room == null) {
            throw new IllegalArgumentException("会议室不存在");
        }

        if (room.getBookings() == null) {
            room.setBookings(new ArrayList<>());
        }

        if (isTimeConflict(room, date, startTime, endTime, null)) {
            throw new IllegalArgumentException("该时间段已被预订，请选择其他时间");
        }

        Booking booking = new Booking();
        booking.setId(UUID.randomUUID().toString().replace("-", ""));
        booking.setRoomId(roomId);
        booking.setDate(date);
        booking.setStartTime(startTime);
        booking.setEndTime(endTime);
        booking.setPurpose(purpose);
        booking.setBookerId(bookerId);
        booking.setBookerName(booker.getName());
        booking.setCreatedAt(System.currentTimeMillis());

        room.getBookings().add(booking);
        dataStoreService.saveMeetingRooms();

        return booking;
    }

    public void cancelBooking(String bookingId, String userId) {
        User user = dataStoreService.getUserById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        Booking bookingToCancel = null;
        MeetingRoom roomWithBooking = null;

        for (MeetingRoom room : dataStoreService.getMeetingRooms()) {
            if (room.getBookings() != null) {
                for (Booking booking : room.getBookings()) {
                    if (booking.getId().equals(bookingId)) {
                        bookingToCancel = booking;
                        roomWithBooking = room;
                        break;
                    }
                }
            }
            if (bookingToCancel != null) {
                break;
            }
        }

        if (bookingToCancel == null) {
            throw new IllegalArgumentException("预订不存在");
        }

        if (!user.isAdmin() && !bookingToCancel.getBookerId().equals(userId)) {
            throw new IllegalArgumentException("只能取消自己创建的预订");
        }

        roomWithBooking.getBookings().remove(bookingToCancel);
        dataStoreService.saveMeetingRooms();
    }

    private void validateTime(String date, String startTime, String endTime) {
        try {
            LocalDate bookingDate = LocalDate.parse(date);

            int startMinutes = timeToMinutes(startTime);
            int endMinutes = timeToMinutes(endTime);

            if (startMinutes >= endMinutes) {
                throw new IllegalArgumentException("开始时间必须早于结束时间");
            }

            if (startMinutes % 30 != 0 || endMinutes % 30 != 0) {
                throw new IllegalArgumentException("时间必须以半小时为单位");
            }

            if (startMinutes < 0 || startMinutes > 24 * 60 || endMinutes < 0 || endMinutes > 24 * 60) {
                throw new IllegalArgumentException("时间格式不正确");
            }

            LocalDate today = LocalDate.now();
            LocalTime now = LocalTime.now();
            int nowMinutes = now.getHour() * 60 + now.getMinute();

            if (bookingDate.isBefore(today)) {
                throw new IllegalArgumentException("不能预订过去的时间");
            }

            if (bookingDate.equals(today) && startMinutes < nowMinutes) {
                throw new IllegalArgumentException("不能预订过去的时间");
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("时间格式不正确");
        }
    }

    private boolean isTimeConflict(MeetingRoom room, String date, 
                                    String startTime, String endTime, String excludeBookingId) {
        if (room.getBookings() == null) {
            return false;
        }

        int newStart = timeToMinutes(startTime);
        int newEnd = timeToMinutes(endTime);

        for (Booking booking : room.getBookings()) {
            if (excludeBookingId != null && excludeBookingId.equals(booking.getId())) {
                continue;
            }
            if (date.equals(booking.getDate())) {
                int existStart = timeToMinutes(booking.getStartTime());
                int existEnd = timeToMinutes(booking.getEndTime());

                if (newStart < existEnd && newEnd > existStart) {
                    return true;
                }
            }
        }
        return false;
    }
}
