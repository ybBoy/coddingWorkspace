package core;

import entity.Room;
import entity.RoomLog;
import entity.RoomStatus;
import entity.StayRecord;
import io.RoomJsonStore;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class RoomService {

    private final RoomJsonStore store;
    private final AtomicLong logIdCounter = new AtomicLong(0);
    private final AtomicLong stayIdCounter = new AtomicLong(0);

    public RoomService(RoomJsonStore store) {
        this.store = store;
        initCounters();
    }

    private void initCounters() {
        for (RoomLog log : store.getLogs()) {
            long id = parseIdNumber(log.getId());
            if (id > logIdCounter.get()) {
                logIdCounter.set(id);
            }
        }
        for (StayRecord record : store.getStayRecords()) {
            long id = parseIdNumber(record.getId());
            if (id > stayIdCounter.get()) {
                stayIdCounter.set(id);
            }
        }
    }

    private long parseIdNumber(String id) {
        try {
            int idx = id.lastIndexOf('-');
            if (idx >= 0) {
                return Long.parseLong(id.substring(idx + 1));
            }
            return Long.parseLong(id);
        } catch (Exception e) {
            return 0;
        }
    }

    public List<Room> getAllRooms() {
        updateOverdueStatus();
        return new ArrayList<>(store.getRooms());
    }

    public List<RoomLog> getRecentLogs(int count) {
        List<RoomLog> allLogs = store.getLogs();
        int size = allLogs.size();
        if (size <= count) {
            return new ArrayList<>(allLogs);
        }
        return new ArrayList<>(allLogs.subList(size - count, size));
    }

    public synchronized Room checkIn(String roomId, String guestName, long expectedCheckOutTime) {
        Room room = findRoom(roomId);
        if (room == null) {
            return null;
        }
        if (room.getStatus() != RoomStatus.VACANT) {
            return null;
        }

        String stayId = "stay-" + stayIdCounter.incrementAndGet();
        long now = System.currentTimeMillis();
        StayRecord record = new StayRecord(stayId, roomId, guestName, now, expectedCheckOutTime);
        store.getStayRecords().add(record);

        room.setStatus(RoomStatus.OCCUPIED);
        room.setCurrentStay(record);
        room.setOverdue(false);

        addLog(roomId, room.getRoomNo(), "入住", "前台", guestName + " 入住");
        store.forceSave();

        return room;
    }

    public synchronized Room checkOut(String roomId) {
        Room room = findRoom(roomId);
        if (room == null) {
            return null;
        }
        if (room.getStatus() != RoomStatus.OCCUPIED) {
            return null;
        }

        StayRecord record = room.getCurrentStay();
        if (record != null) {
            record.setActualCheckOutTime(System.currentTimeMillis());
        }

        room.setStatus(RoomStatus.DIRTY);
        room.setCurrentStay(null);
        room.setOverdue(false);

        String remark = record != null ? record.getGuestName() + " 退房" : "退房";
        addLog(roomId, room.getRoomNo(), "退房", "前台", remark);
        store.forceSave();

        return room;
    }

    public synchronized Room cleanRoom(String roomId) {
        Room room = findRoom(roomId);
        if (room == null) {
            return null;
        }
        if (room.getStatus() != RoomStatus.DIRTY) {
            return null;
        }

        room.setStatus(RoomStatus.VACANT);
        room.setOverdue(false);

        addLog(roomId, room.getRoomNo(), "打扫完成", "前台", "房间已打扫");
        store.forceSave();

        return room;
    }

    public synchronized Room markMaintenance(String roomId) {
        Room room = findRoom(roomId);
        if (room == null) {
            return null;
        }
        if (room.getStatus() == RoomStatus.MAINTENANCE) {
            return null;
        }

        RoomStatus prevStatus = room.getStatus();
        room.setStatus(RoomStatus.MAINTENANCE);

        if (prevStatus == RoomStatus.OCCUPIED && room.getCurrentStay() != null) {
            room.getCurrentStay().setActualCheckOutTime(System.currentTimeMillis());
            room.setCurrentStay(null);
        }
        room.setOverdue(false);

        addLog(roomId, room.getRoomNo(), "报修", "前台", "房间标记为维修中");
        store.forceSave();

        return room;
    }

    public synchronized Room repairDone(String roomId) {
        Room room = findRoom(roomId);
        if (room == null) {
            return null;
        }
        if (room.getStatus() != RoomStatus.MAINTENANCE) {
            return null;
        }

        room.setStatus(RoomStatus.DIRTY);
        room.setOverdue(false);

        addLog(roomId, room.getRoomNo(), "解除维修", "前台", "维修完成，待打扫");
        store.forceSave();

        return room;
    }

    private Room findRoom(String roomId) {
        for (Room room : store.getRooms()) {
            if (room.getId().equals(roomId)) {
                return room;
            }
        }
        return null;
    }

    private void addLog(String roomId, String roomNo, String action, String operator, String remark) {
        String logId = "log-" + logIdCounter.incrementAndGet();
        RoomLog log = new RoomLog(logId, roomId, roomNo, action, operator, System.currentTimeMillis());
        log.setRemark(remark);
        store.getLogs().add(log);
    }

    private void updateOverdueStatus() {
        long now = System.currentTimeMillis();
        for (Room room : store.getRooms()) {
            if (room.getStatus() == RoomStatus.OCCUPIED && room.getCurrentStay() != null) {
                boolean overdue = room.getCurrentStay().getExpectedCheckOutTime() < now;
                room.setOverdue(overdue);
            } else {
                room.setOverdue(false);
            }
        }
    }
}
