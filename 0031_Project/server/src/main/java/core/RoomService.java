package core;

import entity.AlertItem;
import entity.Operator;
import entity.Room;
import entity.RoomLog;
import entity.RoomStatus;
import entity.StayRecord;
import io.RoomJsonStore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class RoomService {

    private static final long MAINTENANCE_ALERT_THRESHOLD = 12 * 60 * 60 * 1000L;
    private static final long DIRTY_ALERT_THRESHOLD = 2 * 60 * 60 * 1000L;
    private static final long OVERDUE_ALERT_THRESHOLD = 30 * 60 * 1000L;

    private final RoomJsonStore store;
    private final AtomicLong logIdCounter = new AtomicLong(0);
    private final AtomicLong stayIdCounter = new AtomicLong(0);
    private final AtomicLong roomIdCounter = new AtomicLong(0);
    private final AtomicLong alertIdCounter = new AtomicLong(0);
    private String currentOperatorName = "张经理";

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
        for (Room room : store.getRooms()) {
            long id = parseIdNumber(room.getId());
            if (id > roomIdCounter.get()) {
                roomIdCounter.set(id);
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

    public String getCurrentOperatorName() {
        return currentOperatorName;
    }

    public void setCurrentOperatorName(String name) {
        this.currentOperatorName = name != null ? name : "前台";
    }

    public List<Operator> getOperators() {
        return new ArrayList<>(store.getOperators());
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

    public List<AlertItem> getAlerts() {
        List<AlertItem> alerts = new ArrayList<>();
        long now = System.currentTimeMillis();

        for (Room room : store.getRooms()) {
            if (room.getStatus() == RoomStatus.OCCUPIED
                    && room.getCurrentStay() != null
                    && room.isOverdue()
                    && (now - room.getCurrentStay().getExpectedCheckOutTime()) > OVERDUE_ALERT_THRESHOLD) {
                long overdueMs = now - room.getCurrentStay().getExpectedCheckOutTime();
                long hours = overdueMs / (1000 * 60 * 60);
                long mins = (overdueMs % (1000 * 60 * 60)) / (1000 * 60);
                String msg = hours > 0
                        ? "已超时 " + hours + " 小时 " + mins + " 分钟未退房"
                        : "已超时 " + mins + " 分钟未退房";
                alerts.add(new AlertItem(
                        "alert-overdue-" + room.getId(),
                        "overdue",
                        room.getId(),
                        room.getRoomNo(),
                        room.getRoomNo() + " " + room.getCurrentStay().getGuestName() + " " + msg,
                        room.getCurrentStay().getExpectedCheckOutTime()
                ));
            }

            if (room.getStatus() == RoomStatus.MAINTENANCE) {
                long maintenanceStart = findMaintenanceStartTime(room.getId());
                if (maintenanceStart > 0 && (now - maintenanceStart) > MAINTENANCE_ALERT_THRESHOLD) {
                    long hours = (now - maintenanceStart) / (1000 * 60 * 60);
                    alerts.add(new AlertItem(
                            "alert-maintenance-" + room.getId(),
                            "maintenance",
                            room.getId(),
                            room.getRoomNo(),
                            room.getRoomNo() + " 维修已超过 " + hours + " 小时",
                            maintenanceStart
                    ));
                }
            }

            if (room.getStatus() == RoomStatus.DIRTY) {
                long dirtyStart = findDirtyStartTime(room.getId());
                if (dirtyStart > 0 && (now - dirtyStart) > DIRTY_ALERT_THRESHOLD) {
                    long mins = (now - dirtyStart) / (1000 * 60);
                    alerts.add(new AlertItem(
                            "alert-dirty-" + room.getId(),
                            "dirty",
                            room.getId(),
                            room.getRoomNo(),
                            room.getRoomNo() + " 待打扫已 " + mins + " 分钟",
                            dirtyStart
                    ));
                }
            }
        }

        Collections.sort(alerts, new Comparator<AlertItem>() {
            @Override
            public int compare(AlertItem a1, AlertItem a2) {
                return Long.compare(a2.getTriggerTime(), a1.getTriggerTime());
            }
        });

        return alerts;
    }

    private long findMaintenanceStartTime(String roomId) {
        long startTime = 0;
        for (RoomLog log : store.getLogs()) {
            if (log.getRoomId().equals(roomId) && "报修".equals(log.getAction())) {
                startTime = log.getTimestamp();
            }
            if (log.getRoomId().equals(roomId) && "解除维修".equals(log.getAction())) {
                startTime = 0;
            }
        }
        return startTime;
    }

    private long findDirtyStartTime(String roomId) {
        long startTime = 0;
        for (RoomLog log : store.getLogs()) {
            if (log.getRoomId().equals(roomId)
                    && ("退房".equals(log.getAction()) || "解除维修".equals(log.getAction()))) {
                startTime = log.getTimestamp();
            }
            if (log.getRoomId().equals(roomId) && "打扫完成".equals(log.getAction())) {
                startTime = 0;
            }
        }
        return startTime;
    }

    public synchronized Room checkIn(String roomId, String guestName, long expectedCheckOutTime,
                                      double price, double deposit) {
        Room room = findRoom(roomId);
        if (room == null) {
            return null;
        }
        if (room.getStatus() != RoomStatus.VACANT) {
            return null;
        }

        String stayId = "stay-" + stayIdCounter.incrementAndGet();
        long now = System.currentTimeMillis();
        StayRecord record = new StayRecord(stayId, roomId, guestName, now, expectedCheckOutTime,
                price, deposit, currentOperatorName);
        store.getStayRecords().add(record);

        room.setStatus(RoomStatus.OCCUPIED);
        room.setCurrentStay(record);
        room.setOverdue(false);

        addLog(roomId, room.getRoomNo(), "入住", currentOperatorName,
                guestName + " 入住，房价 " + price + " 元，押金 " + deposit + " 元");
        store.forceSave();

        return room;
    }

    public synchronized Room checkOut(String roomId, boolean settle) {
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
            record.setCheckOutOperator(currentOperatorName);
            if (settle) {
                record.setSettled(true);
            }
        }

        room.setStatus(RoomStatus.DIRTY);
        room.setCurrentStay(null);
        room.setOverdue(false);

        String remark = record != null
                ? record.getGuestName() + " 退房" + (settle ? "，已结清" : "，未结清")
                : "退房";
        addLog(roomId, room.getRoomNo(), "退房", currentOperatorName, remark);
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

        addLog(roomId, room.getRoomNo(), "打扫完成", currentOperatorName, "房间已打扫");
        store.forceSave();

        return room;
    }

    public synchronized Room markMaintenance(String roomId, String remark) {
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
            room.getCurrentStay().setCheckOutOperator(currentOperatorName);
            room.setCurrentStay(null);
        }
        room.setOverdue(false);

        String logRemark = remark != null && !remark.isEmpty()
                ? "房间标记为维修中：" + remark
                : "房间标记为维修中";
        addLog(roomId, room.getRoomNo(), "报修", currentOperatorName, logRemark);
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

        addLog(roomId, room.getRoomNo(), "解除维修", currentOperatorName, "维修完成，待打扫");
        store.forceSave();

        return room;
    }

    public synchronized int batchCleanByFloor(int floor) {
        int count = 0;
        for (Room room : store.getRooms()) {
            if (room.getFloor() == floor && room.getStatus() == RoomStatus.DIRTY) {
                room.setStatus(RoomStatus.VACANT);
                room.setOverdue(false);
                addLog(room.getId(), room.getRoomNo(), "批量打扫", currentOperatorName, "批量打扫完成");
                count++;
            }
        }
        if (count > 0) {
            store.forceSave();
        }
        return count;
    }

    public synchronized int batchMarkDirtyByFloor(int floor) {
        int count = 0;
        for (Room room : store.getRooms()) {
            if (room.getFloor() == floor && room.getStatus() == RoomStatus.VACANT) {
                room.setStatus(RoomStatus.DIRTY);
                addLog(room.getId(), room.getRoomNo(), "批量设为待打扫", currentOperatorName, "批量操作");
                count++;
            }
        }
        if (count > 0) {
            store.forceSave();
        }
        return count;
    }

    public Map<String, Object> getRoomDetail(String roomId) {
        Room room = findRoom(roomId);
        if (room == null) {
            return null;
        }

        updateOverdueStatus();

        List<StayRecord> stayHistory = new ArrayList<>();
        for (StayRecord record : store.getStayRecords()) {
            if (record.getRoomId().equals(roomId) && record.getActualCheckOutTime() != null) {
                stayHistory.add(record);
            }
        }
        Collections.sort(stayHistory, new Comparator<StayRecord>() {
            @Override
            public int compare(StayRecord s1, StayRecord s2) {
                return Long.compare(s2.getCheckInTime(), s1.getCheckInTime());
            }
        });

        List<RoomLog> roomLogs = new ArrayList<>();
        for (RoomLog log : store.getLogs()) {
            if (log.getRoomId().equals(roomId)) {
                roomLogs.add(log);
            }
        }
        Collections.sort(roomLogs, new Comparator<RoomLog>() {
            @Override
            public int compare(RoomLog l1, RoomLog l2) {
                return Long.compare(l2.getTimestamp(), l1.getTimestamp());
            }
        });

        Map<String, Object> detail = new HashMap<>();
        detail.put("room", room);
        detail.put("stayHistory", stayHistory);
        detail.put("logs", roomLogs);
        return detail;
    }

    public String exportStayRecordsCsv() {
        StringBuilder sb = new StringBuilder();
        sb.append("房间号,客人姓名,入住时间,预计离店,实际离店,房价,押金,已结清,入住操作,退房操作\n");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (StayRecord record : store.getStayRecords()) {
            String roomNo = findRoom(record.getRoomId()) != null
                    ? findRoom(record.getRoomId()).getRoomNo()
                    : "";
            sb.append(roomNo).append(",");
            sb.append(escapeCsv(record.getGuestName())).append(",");
            sb.append(sdf.format(new Date(record.getCheckInTime()))).append(",");
            sb.append(sdf.format(new Date(record.getExpectedCheckOutTime()))).append(",");
            sb.append(record.getActualCheckOutTime() != null
                    ? sdf.format(new Date(record.getActualCheckOutTime()))
                    : "").append(",");
            sb.append(record.getPrice()).append(",");
            sb.append(record.getDeposit()).append(",");
            sb.append(record.isSettled() ? "是" : "否").append(",");
            sb.append(escapeCsv(record.getCheckInOperator())).append(",");
            sb.append(escapeCsv(record.getCheckOutOperator())).append("\n");
        }
        return sb.toString();
    }

    public String exportLogsCsv() {
        StringBuilder sb = new StringBuilder();
        sb.append("时间,房间号,操作,操作人,备注\n");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (RoomLog log : store.getLogs()) {
            sb.append(sdf.format(new Date(log.getTimestamp()))).append(",");
            sb.append(log.getRoomNo()).append(",");
            sb.append(escapeCsv(log.getAction())).append(",");
            sb.append(escapeCsv(log.getOperator())).append(",");
            sb.append(escapeCsv(log.getRemark())).append("\n");
        }
        return sb.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    public synchronized Room addRoom(String roomNo, int floor, String type, double defaultPrice) {
        for (Room room : store.getRooms()) {
            if (room.getRoomNo().equals(roomNo)) {
                return null;
            }
        }
        String id = "room-" + roomIdCounter.incrementAndGet();
        Room room = new Room(id, roomNo, floor, RoomStatus.VACANT, type, defaultPrice);
        store.getRooms().add(room);
        addLog(id, roomNo, "新增房间", currentOperatorName, type + "，房价 " + defaultPrice + " 元");
        store.forceSave();
        return room;
    }

    public synchronized Room updateRoom(String roomId, String roomNo, int floor, String type, double defaultPrice) {
        Room room = findRoom(roomId);
        if (room == null) {
            return null;
        }
        if (!room.getRoomNo().equals(roomNo)) {
            for (Room r : store.getRooms()) {
                if (!r.getId().equals(roomId) && r.getRoomNo().equals(roomNo)) {
                    return null;
                }
            }
        }
        room.setRoomNo(roomNo);
        room.setFloor(floor);
        room.setType(type);
        room.setDefaultPrice(defaultPrice);
        addLog(roomId, roomNo, "修改房间信息", currentOperatorName,
                type + "，房价 " + defaultPrice + " 元");
        store.forceSave();
        return room;
    }

    public synchronized boolean deleteRoom(String roomId) {
        Room room = findRoom(roomId);
        if (room == null) {
            return false;
        }
        if (room.getStatus() == RoomStatus.OCCUPIED) {
            return false;
        }
        store.getRooms().remove(room);
        addLog(roomId, room.getRoomNo(), "删除房间", currentOperatorName, "");
        store.forceSave();
        return true;
    }

    public synchronized Room disableRoom(String roomId, String remark) {
        Room room = findRoom(roomId);
        if (room == null) {
            return null;
        }
        if (room.getStatus() == RoomStatus.OCCUPIED) {
            return null;
        }
        if (room.getStatus() == RoomStatus.DISABLED) {
            return room;
        }
        if (room.getStatus() == RoomStatus.OCCUPIED && room.getCurrentStay() != null) {
            room.getCurrentStay().setActualCheckOutTime(System.currentTimeMillis());
            room.getCurrentStay().setCheckOutOperator(currentOperatorName);
            room.setCurrentStay(null);
        }
        room.setStatus(RoomStatus.DISABLED);
        room.setOverdue(false);
        String logRemark = remark != null && !remark.isEmpty()
                ? "房间停用：" + remark
                : "房间停用";
        addLog(roomId, room.getRoomNo(), "停用房间", currentOperatorName, logRemark);
        store.forceSave();
        return room;
    }

    public synchronized Room enableRoom(String roomId) {
        Room room = findRoom(roomId);
        if (room == null) {
            return null;
        }
        if (room.getStatus() != RoomStatus.DISABLED) {
            return room;
        }
        room.setStatus(RoomStatus.DIRTY);
        addLog(roomId, room.getRoomNo(), "启用房间", currentOperatorName, "房间启用，变为待打扫");
        store.forceSave();
        return room;
    }

    public List<StayRecord> getStayRecordsByDateRange(long startTime, long endTime) {
        List<Room> rooms = store.getRooms();
        List<StayRecord> result = new ArrayList<>();
        for (StayRecord record : store.getStayRecords()) {
            if (record.getCheckInTime() >= startTime && record.getCheckInTime() <= endTime) {
                result.add(record);
            }
        }
        return result;
    }

    public int getTodayCheckInCount() {
        long startOfDay = getStartOfDay();
        int count = 0;
        for (StayRecord record : store.getStayRecords()) {
            if (record.getCheckInTime() >= startOfDay) {
                count++;
            }
        }
        return count;
    }

    private long getStartOfDay() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
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
