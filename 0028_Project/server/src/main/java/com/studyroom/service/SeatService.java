package com.studyroom.service;

import com.studyroom.model.Seat;
import com.studyroom.model.SeatAction;
import com.studyroom.store.JsonStore;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class SeatService {
    private static final int ROWS = 8;
    private static final int COLS = 6;
    private static final int MAX_ACTIONS = 50;

    private final List<Seat> seats = new CopyOnWriteArrayList<>();
    private final List<SeatAction> actions = new CopyOnWriteArrayList<>();
    private final JsonStore store;

    public SeatService(JsonStore store) {
        this.store = store;
        initSeats();
    }

    private void initSeats() {
        List<Seat> loaded = store.loadSeats();
        if (loaded != null && !loaded.isEmpty()) {
            seats.addAll(loaded);
        } else {
            for (int r = 0; r < ROWS; r++) {
                for (int c = 0; c < COLS; c++) {
                    seats.add(new Seat(r * COLS + c, r, c));
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
        for (Seat seat : seats) {
            if ("away".equals(seat.getStatus()) && seat.isReleasable()) {
                seat.setStatus("releasable");
            }
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
