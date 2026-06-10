package com.meeting.service;

import com.meeting.model.Booking;
import com.meeting.model.User;
import com.meeting.util.DataStore;
import com.meeting.util.JsonUtil;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class BookingService {

    private final DataStore dataStore;

    public BookingService() {
        this.dataStore = DataStore.getInstance();
    }

    public static class BookingException extends Exception {
        public BookingException(String message) { super(message); }
    }

    public List<Booking> getBookingsByRoomAndDate(String roomId, String date) {
        List<Booking> result = new ArrayList<Booking>();
        for (Booking b : dataStore.getBookings()) {
            if (roomId == null || roomId.equals(b.getRoomId())) {
                if (date == null || date.equals(b.getDate())) {
                    result.add(b);
                }
            }
        }
        sortBookings(result);
        return result;
    }

    public List<Booking> getAllBookings() {
        List<Booking> list = new ArrayList<Booking>(dataStore.getBookings());
        sortBookings(list);
        return list;
    }

    private void sortBookings(List<Booking> list) {
        Collections.sort(list, new Comparator<Booking>() {
            @Override
            public int compare(Booking a, Booking b) {
                int d = a.getDate().compareTo(b.getDate());
                if (d != 0) return d;
                return a.getStartTime().compareTo(b.getStartTime());
            }
        });
    }

    private boolean isHalfHour(String time) {
        if (time == null || !time.matches("\\d{1,2}:\\d{2}")) return false;
        String[] parts = time.split(":");
        try {
            int h = Integer.parseInt(parts[0]);
            int m = Integer.parseInt(parts[1]);
            if (h < 0 || h > 23) return false;
            return m == 0 || m == 30;
        } catch (Exception e) {
            return false;
        }
    }

    private int timeToMinutes(String time) {
        String[] parts = time.split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }

    private boolean timeOverlap(String s1, String e1, String s2, String e2) {
        int start1 = timeToMinutes(s1);
        int end1 = timeToMinutes(e1);
        int start2 = timeToMinutes(s2);
        int end2 = timeToMinutes(e2);
        return start1 < end2 && start2 < end1;
    }

    public Booking createBooking(String roomId, String userId, String date,
                                  String startTime, String endTime, String purpose) throws BookingException {
        if (roomId == null || roomId.trim().isEmpty()) {
            throw new BookingException("请选择会议室");
        }
        if (date == null || !date.matches("\\d{4}-\\d{2}-\\d{2}")) {
            throw new BookingException("日期格式不正确，请使用 yyyy-MM-dd");
        }
        if (!isHalfHour(startTime)) {
            throw new BookingException("开始时间必须以半小时为单位，如 9:00、9:30");
        }
        if (!isHalfHour(endTime)) {
            throw new BookingException("结束时间必须以半小时为单位，如 10:00、10:30");
        }
        if (timeToMinutes(startTime) >= timeToMinutes(endTime)) {
            throw new BookingException("开始时间必须早于结束时间");
        }
        if (purpose == null || purpose.trim().isEmpty()) {
            throw new BookingException("请填写事由");
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        try {
            Date bookingStart = sdf.parse(date + " " + startTime);
            if (bookingStart.before(new Date())) {
                throw new BookingException("不能预订过去的时间");
            }
        } catch (ParseException e) {
            throw new BookingException("时间格式错误");
        }

        List<Booking> existing = getBookingsByRoomAndDate(roomId, date);
        for (Booking b : existing) {
            if (timeOverlap(startTime, endTime, b.getStartTime(), b.getEndTime())) {
                throw new BookingException("该时间段已被预订（" + b.getStartTime() + "-" + b.getEndTime() + " " + b.getUserName() + "）");
            }
        }

        User user = dataStore.getUserById(userId);
        if (user == null) throw new BookingException("用户不存在");

        Booking booking = new Booking();
        booking.setId("B" + System.currentTimeMillis());
        booking.setRoomId(roomId);
        booking.setUserId(userId);
        booking.setUserName(user.getName());
        booking.setDate(date);
        booking.setStartTime(startTime);
        booking.setEndTime(endTime);
        booking.setPurpose(purpose);
        booking.setCreatedAt(System.currentTimeMillis());

        dataStore.addBooking(booking);
        return booking;
    }

    public void cancelBooking(String bookingId, String currentUserId) throws BookingException {
        Booking b = dataStore.getBookingById(bookingId);
        if (b == null) throw new BookingException("预订不存在");

        User user = dataStore.getUserById(currentUserId);
        if (user == null) throw new BookingException("用户不存在");

        if (!user.isAdmin() && !b.getUserId().equals(currentUserId)) {
            throw new BookingException("您只能取消自己创建的预订");
        }

        dataStore.removeBooking(bookingId);
    }
}
