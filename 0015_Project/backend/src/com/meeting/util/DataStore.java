package com.meeting.util;

import com.meeting.model.Booking;
import com.meeting.model.MeetingRoom;
import com.meeting.model.User;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class DataStore {
    private static DataStore instance;
    private final String meetingsFilePath;
    private final String usersFilePath;

    private List<MeetingRoom> rooms = new ArrayList<MeetingRoom>();
    private List<Booking> bookings = new ArrayList<Booking>();
    private List<User> users = new ArrayList<User>();

    private DataStore(String dataDir) {
        this.meetingsFilePath = dataDir + File.separator + "meetings.json";
        this.usersFilePath = dataDir + File.separator + "users.json";
        load();
    }

    public static synchronized DataStore getInstance(String dataDir) {
        if (instance == null) {
            instance = new DataStore(dataDir);
        }
        return instance;
    }

    public static synchronized DataStore getInstance() {
        return instance;
    }

    private String readFile(String path) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(
                new FileInputStream(path), StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line).append("\n");
        }
        br.close();
        return sb.toString();
    }

    private void writeFile(String path, String content) throws IOException {
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(path), StandardCharsets.UTF_8));
        bw.write(content);
        bw.close();
    }

    public void load() {
        try {
            String meetingContent = readFile(meetingsFilePath);
            JsonUtil.JsonObject meetObj = JsonUtil.parseJsonObject(meetingContent);

            rooms = new ArrayList<MeetingRoom>();
            JsonUtil.JsonArray roomsArr = (JsonUtil.JsonArray) meetObj.get("rooms");
            for (int i = 0; i < roomsArr.size(); i++) {
                JsonUtil.JsonObject roomObj = (JsonUtil.JsonObject) roomsArr.get(i);
                MeetingRoom r = new MeetingRoom();
                r.setId(roomObj.getString("id"));
                r.setName(roomObj.getString("name"));
                r.setCapacity(roomObj.getInt("capacity"));
                rooms.add(r);
            }

            bookings = new ArrayList<Booking>();
            JsonUtil.JsonArray bookingArr = (JsonUtil.JsonArray) meetObj.get("bookings");
            for (int i = 0; i < bookingArr.size(); i++) {
                JsonUtil.JsonObject bObj = (JsonUtil.JsonObject) bookingArr.get(i);
                Booking b = new Booking();
                b.setId(bObj.getString("id"));
                b.setRoomId(bObj.getString("roomId"));
                b.setUserId(bObj.getString("userId"));
                b.setUserName(bObj.getString("userName"));
                b.setDate(bObj.getString("date"));
                b.setStartTime(bObj.getString("startTime"));
                b.setEndTime(bObj.getString("endTime"));
                b.setPurpose(bObj.getString("purpose"));
                if (bObj.has("createdAt")) b.setCreatedAt(bObj.getLong("createdAt"));
                bookings.add(b);
            }

            String userContent = readFile(usersFilePath);
            JsonUtil.JsonObject userObj = JsonUtil.parseJsonObject(userContent);
            users = new ArrayList<User>();
            JsonUtil.JsonArray userArr = (JsonUtil.JsonArray) userObj.get("users");
            for (int i = 0; i < userArr.size(); i++) {
                JsonUtil.JsonObject uObj = (JsonUtil.JsonObject) userArr.get(i);
                User u = new User();
                u.setId(uObj.getString("id"));
                u.setName(uObj.getString("name"));
                u.setRole(uObj.getString("role"));
                users.add(u);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void saveMeetings() {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("{\n  \"rooms\": [\n");
            for (int i = 0; i < rooms.size(); i++) {
                sb.append("    ").append(JsonUtil.toJson(rooms.get(i)));
                if (i < rooms.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("  ],\n  \"bookings\": [\n");
            for (int i = 0; i < bookings.size(); i++) {
                sb.append("    ").append(JsonUtil.toJson(bookings.get(i)));
                if (i < bookings.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("  ]\n}\n");
            writeFile(meetingsFilePath, sb.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<MeetingRoom> getRooms() { return rooms; }
    public List<Booking> getBookings() { return bookings; }
    public List<User> getUsers() { return users; }

    public User getUserById(String id) {
        for (User u : users) {
            if (u.getId().equals(id)) return u;
        }
        return null;
    }

    public MeetingRoom getRoomById(String id) {
        for (MeetingRoom r : rooms) {
            if (r.getId().equals(id)) return r;
        }
        return null;
    }

    public Booking getBookingById(String id) {
        for (Booking b : bookings) {
            if (b.getId().equals(id)) return b;
        }
        return null;
    }

    public synchronized void addBooking(Booking b) {
        bookings.add(b);
        saveMeetings();
    }

    public synchronized void removeBooking(String id) {
        for (int i = 0; i < bookings.size(); i++) {
            if (bookings.get(i).getId().equals(id)) {
                bookings.remove(i);
                break;
            }
        }
        saveMeetings();
    }
}
