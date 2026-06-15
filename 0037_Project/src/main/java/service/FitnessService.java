package service;

import domain.FitnessCheckin;
import storage.CheckinFileStore;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FitnessService {
    private final Map<String, FitnessCheckin> records = new ConcurrentHashMap<>();
    private final CheckinFileStore fileStore;

    public FitnessService(CheckinFileStore fileStore) {
        this.fileStore = fileStore;
        loadFromFile();
    }

    private void loadFromFile() {
        List<FitnessCheckin> loaded = fileStore.loadAll();
        for (FitnessCheckin checkin : loaded) {
            records.put(checkin.getId(), checkin);
        }
    }

    private void saveToFile() {
        fileStore.saveAll(new ArrayList<>(records.values()));
    }

    public FitnessCheckin addCheckin(LocalDate checkinDate, String exerciseType,
                                     int duration, String mood, String note) {
        String id = UUID.randomUUID().toString();
        FitnessCheckin checkin = new FitnessCheckin(id, checkinDate, exerciseType, duration, mood, note);
        records.put(id, checkin);
        saveToFile();
        return checkin;
    }

    public List<FitnessCheckin> getAllCheckins() {
        List<FitnessCheckin> list = new ArrayList<>(records.values());
        list.sort((a, b) -> b.getCheckinDate().compareTo(a.getCheckinDate()));
        return list;
    }

    public List<FitnessCheckin> getCheckinsByType(String exerciseType) {
        List<FitnessCheckin> result = new ArrayList<>();
        for (FitnessCheckin checkin : records.values()) {
            if (exerciseType.equals(checkin.getExerciseType())) {
                result.add(checkin);
            }
        }
        result.sort((a, b) -> b.getCheckinDate().compareTo(a.getCheckinDate()));
        return result;
    }

    public boolean updateCheckin(String id, String mood, String note) {
        FitnessCheckin checkin = records.get(id);
        if (checkin == null) {
            return false;
        }
        if (mood != null && !mood.isEmpty()) {
            checkin.setMood(mood);
        }
        if (note != null) {
            checkin.setNote(note);
        }
        saveToFile();
        return true;
    }

    public boolean deleteCheckin(String id) {
        if (records.remove(id) != null) {
            saveToFile();
            return true;
        }
        return false;
    }

    public FitnessCheckin getCheckinById(String id) {
        return records.get(id);
    }

    public int getWeeklyMinutes() {
        LocalDate today = LocalDate.now();
        int dayOfWeek = today.getDayOfWeek().getValue();
        LocalDate monday = today.minusDays(dayOfWeek - 1);

        int total = 0;
        for (FitnessCheckin checkin : records.values()) {
            if (!checkin.getCheckinDate().isBefore(monday)) {
                total += checkin.getDuration();
            }
        }
        return total;
    }

    public int getStreakDays() {
        if (records.isEmpty()) {
            return 0;
        }

        Set<LocalDate> dates = new TreeSet<>();
        for (FitnessCheckin checkin : records.values()) {
            dates.add(checkin.getCheckinDate());
        }

        List<LocalDate> sortedDates = new ArrayList<>(dates);
        Collections.sort(sortedDates, Collections.reverseOrder());

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        if (sortedDates.isEmpty() ||
                (!sortedDates.get(0).equals(today) && !sortedDates.get(0).equals(yesterday))) {
            return 0;
        }

        int streak = 0;
        LocalDate checkDate = sortedDates.get(0);

        for (LocalDate date : sortedDates) {
            if (date.equals(checkDate)) {
                streak++;
                checkDate = checkDate.minusDays(1);
            } else if (date.isBefore(checkDate)) {
                break;
            }
        }

        return streak;
    }
}
