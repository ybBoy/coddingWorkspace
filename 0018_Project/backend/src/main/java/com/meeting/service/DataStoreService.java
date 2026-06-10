package com.meeting.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.meeting.model.MeetingRoom;
import com.meeting.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class DataStoreService {

    @Value("${data.file.path:./data/}")
    private String dataPath;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private List<MeetingRoom> meetingRooms;
    private List<User> users;

    @PostConstruct
    public void init() {
        File dataDir = new File(dataPath);
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        loadUsers();
        loadMeetingRooms();
    }

    private void loadUsers() {
        File usersFile = new File(dataPath + "users.json");
        if (usersFile.exists()) {
            try {
                users = objectMapper.readValue(usersFile, new TypeReference<List<User>>() {});
            } catch (IOException e) {
                e.printStackTrace();
                initDefaultUsers();
            }
        } else {
            initDefaultUsers();
        }
    }

    private void initDefaultUsers() {
        users = Arrays.asList(
                new User("u1", "张三", "employee"),
                new User("u2", "李四", "employee"),
                new User("u3", "王五", "employee"),
                new User("admin", "管理员", "admin")
        );
        saveUsers();
    }

    private void loadMeetingRooms() {
        File meetingsFile = new File(dataPath + "meetings.json");
        if (meetingsFile.exists()) {
            try {
                meetingRooms = objectMapper.readValue(meetingsFile, new TypeReference<List<MeetingRoom>>() {});
            } catch (IOException e) {
                e.printStackTrace();
                initDefaultMeetingRooms();
            }
        } else {
            initDefaultMeetingRooms();
        }
    }

    private void initDefaultMeetingRooms() {
        meetingRooms = new ArrayList<>();
        meetingRooms.add(new MeetingRoom("room1", "会议室A", 6));
        meetingRooms.add(new MeetingRoom("room2", "会议室B", 10));
        meetingRooms.add(new MeetingRoom("room3", "会议室C", 4));
        for (MeetingRoom room : meetingRooms) {
            room.setBookings(new ArrayList<>());
        }
        saveMeetingRooms();
    }

    public synchronized void saveUsers() {
        try {
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(new File(dataPath + "users.json"), users);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void saveMeetingRooms() {
        try {
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(new File(dataPath + "meetings.json"), meetingRooms);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<MeetingRoom> getMeetingRooms() {
        return meetingRooms;
    }

    public List<User> getUsers() {
        return users;
    }

    public User getUserById(String userId) {
        for (User user : users) {
            if (user.getId().equals(userId)) {
                return user;
            }
        }
        return null;
    }

    public MeetingRoom getMeetingRoomById(String roomId) {
        for (MeetingRoom room : meetingRooms) {
            if (room.getId().equals(roomId)) {
                return room;
            }
        }
        return null;
    }
}
