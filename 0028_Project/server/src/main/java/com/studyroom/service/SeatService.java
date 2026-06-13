package com.studyroom.service;

import com.studyroom.model.Seat;
import com.studyroom.model.SeatAction;
import com.studyroom.store.JsonStore;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class SeatService {
    private static final int ROWS = 8;
    private static final int COLS = 6;
    private static final int MAX_ACTIONS = 200;
    private static final int AWAY_TIMEOUT_MINUTES = 15;

    private final List<Seat> seats = new CopyOnWriteArrayList<>();
    private final List<SeatAction> actions = new CopyOnWriteArrayList<>();
    private final JsonStore store;
    private volatile String broadcastMessage = "";
    private volatile long broadcastTimestamp = 0;

    public SeatService(JsonStore store) {
        this.store = store;
        initSeats();
    }

    private String getZoneByPosition(int row, int col) {
        if (col == 0 || col == COLS - 1) return "window";
        if (row >= 0 && row <= 1) return "computer";
        if (row >= 6 && row <= 7) return "quiet";
        return "standard";
    }

    private void initSeats() {
        List<Seat> loaded = store.loadSeats();
        if (loaded != null && !loaded.isEmpty()) {
            for (Seat s : loaded) {
                if (s.getZone() == null || s.getZone().isEmpty()) {
                    s.setZone(getZoneByPosition(s.getRow(), s.getCol()));
                }
            }
            seats.addAll(loaded);
        } else {
            for (int r = 0; r < ROWS; r++) {
                for (int c = 0; c < COLS; c++) {
                    String zone = getZoneByPosition(r, c);
                    seats.add(new Seat(r * COLS + c, r, c, zone));
                }
            }
        }
        List<SeatAction> loadedActions = store.loadActions();
        if (loadedActions != null && !loadedActions.isEmpty()) {
            actions.addAll(loadedActions);
        }
    }

    public void startAutoSave(int intervalSeconds) {
        store.startAutoSave(seats, actions, intervalSeconds);
    }

    public void shutdown() {
        store.shutdown();
    }

    public List<Seat> getAllSeats() {
        return Collections.unmodifiableList(seats);
    }

    public List<SeatAction> getRecentActions(int count) {
        int size = actions.size();
        if (size <= count) return Collections.unmodifiableList(actions);
        return Collections.unmodifiableList(actions.subList(size - count, size));
    }

    public synchronized Seat sit(int seatId, String nickname) {
        Seat seat = findSeat(seatId);
        if (seat == null || !"free".equals(seat.getStatus())) return null;
        for (Seat s : seats) {
            if (!s.equals(seat) && nickname.equals(s.getNickname()) && !"free".equals(s.getStatus())) {
                s.setStatus("free");
                s.setNickname(null);
                s.setAwaySince(0);
                addAction(s.getId(), "leave", nickname);
            }
        }
        seat.setStatus("occupied");
        seat.setNickname(nickname);
        seat.setAwaySince(0);
        addAction(seatId, "sit", nickname);
        return seat;
    }

    public synchronized Seat away(int seatId, String nickname) {
        Seat seat = findSeat(seatId);
        if (seat == null || !"occupied".equals(seat.getStatus()) || !nickname.equals(seat.getNickname())) return null;
        seat.setStatus("away");
        seat.setAwaySince(System.currentTimeMillis());
        addAction(seatId, "away", nickname);
        return seat;
    }

    public synchronized Seat leave(int seatId, String nickname) {
        Seat seat = findSeat(seatId);
        if (seat == null) return null;
        if (!nickname.equals(seat.getNickname()) && !"releasable".equals(seat.getStatus())) return null;
        String prev = seat.getNickname();
        seat.setStatus("free");
        seat.setNickname(null);
        seat.setAwaySince(0);
        addAction(seatId, "leave", prev);
        return seat;
    }

    public synchronized Seat forceRelease(int seatId) {
        Seat seat = findSeat(seatId);
        if (seat == null || "free".equals(seat.getStatus())) return null;
        String prev = seat.getNickname();
        seat.setStatus("free");
        seat.setNickname(null);
        seat.setAwaySince(0);
        addAction(seatId, "forceRelease", prev != null ? prev : "admin");
        return seat;
    }

    public synchronized void checkReleasable() {
        boolean changed = false;
        for (Seat seat : seats) {
            if ("away".equals(seat.getStatus()) && seat.isReleasable()) {
                seat.setStatus("releasable");
                changed = true;
            }
        }
        if (changed) {
            addAction(-1, "system", "已自动标记超时暂离座位为可释放");
        }
    }

    public List<Seat> getReleasableSeats() {
        List<Seat> result = new ArrayList<>();
        for (Seat seat : seats) {
            if ("releasable".equals(seat.getStatus())) {
                result.add(seat);
            }
        }
        return result;
    }

    public List<String> getAllZones() {
        Set<String> zones = new LinkedHashSet<>();
        for (Seat s : seats) {
            if (s.getZone() != null) zones.add(s.getZone());
        }
        return new ArrayList<>(zones);
    }

    public void setBroadcast(String message) {
        this.broadcastMessage = message != null ? message : "";
        this.broadcastTimestamp = System.currentTimeMillis();
    }

    public String getBroadcastMessage() { return broadcastMessage; }
    public long getBroadcastTimestamp() { return broadcastTimestamp; }

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        int total = seats.size();
        int free = 0, occupied = 0, away = 0, releasable = 0;
        Map<String, int[]> zoneStats = new LinkedHashMap<>();

        for (Seat s : seats) {
            String st = s.getStatus();
            String zone = s.getZone() != null ? s.getZone() : "standard";
            zoneStats.computeIfAbsent(zone, k -> new int[4]);
            int[] zs = zoneStats.get(zone);

            if ("free".equals(st)) { free++; zs[0]++; }
            else if ("occupied".equals(st)) { occupied++; zs[1]++; }
            else if ("away".equals(st)) { away++; zs[2]++; }
            else if ("releasable".equals(st)) { releasable++; zs[3]++; }
        }

        stats.put("total", total);
        stats.put("free", free);
        stats.put("occupied", occupied);
        stats.put("away", away);
        stats.put("releasable", releasable);
        stats.put("occupancyRate", total > 0 ? Math.round((occupied + away + releasable) * 100.0 / total) : 0);
        stats.put("zoneStats", zoneStats);

        int forceReleaseCount = 0;
        Set<String> todayUsers = new HashSet<>();
        long todayStart = getTodayStart();
        for (SeatAction a : actions) {
            if (a.getTimestamp() >= todayStart) {
                if ("forceRelease".equals(a.getAction())) forceReleaseCount++;
                if ("sit".equals(a.getAction()) && a.getNickname() != null) todayUsers.add(a.getNickname());
            }
        }
        stats.put("todayForceReleases", forceReleaseCount);
        stats.put("todayUniqueUsers", todayUsers.size());

        int[] hourlyDistribution = new int[24];
        for (SeatAction a : actions) {
            if (a.getTimestamp() >= todayStart && "sit".equals(a.getAction())) {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(a.getTimestamp());
                int hour = cal.get(Calendar.HOUR_OF_DAY);
                hourlyDistribution[hour]++;
            }
        }
        stats.put("hourlyDistribution", hourlyDistribution);

        return stats;
    }

    private long getTodayStart() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    public List<SeatAction> getAllActions() {
        return Collections.unmodifiableList(actions);
    }

    private Seat findSeat(int seatId) {
        for (Seat s : seats) {
            if (s.getId() == seatId) return s;
        }
        return null;
    }

    private void addAction(int seatId, String action, String nickname) {
        actions.add(new SeatAction(seatId, action, nickname));
        while (actions.size() > MAX_ACTIONS) {
            actions.remove(0);
        }
    }
}
