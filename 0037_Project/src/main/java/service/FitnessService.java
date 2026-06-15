package service;

import domain.FitnessCheckin;
import storage.CheckinFileStore;
import storage.SettingsStore;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FitnessService {
    private final Map<String, FitnessCheckin> records = new ConcurrentHashMap<>();
    private final CheckinFileStore fileStore;
    private final SettingsStore settingsStore;
    private int weeklyGoal;

    public FitnessService(CheckinFileStore fileStore, SettingsStore settingsStore) {
        this.fileStore = fileStore;
        this.settingsStore = settingsStore;
        loadFromFile();
        this.weeklyGoal = settingsStore.loadWeeklyGoal();
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

    public boolean updateCheckin(String id, LocalDate checkinDate, String exerciseType,
                                 Integer duration, String mood, String note) {
        FitnessCheckin checkin = records.get(id);
        if (checkin == null) {
            return false;
        }
        if (checkinDate != null) {
            checkin.setCheckinDate(checkinDate);
        }
        if (exerciseType != null && !exerciseType.isEmpty()) {
            checkin.setExerciseType(exerciseType);
        }
        if (duration != null) {
            checkin.setDuration(duration);
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

    public int getWeeklyGoal() {
        return weeklyGoal;
    }

    public void setWeeklyGoal(int minutes) {
        if (minutes <= 0 || minutes > 10080) {
            return;
        }
        this.weeklyGoal = minutes;
        settingsStore.saveWeeklyGoal(minutes);
    }

    public int importCheckins(List<FitnessCheckin> importList, boolean overwrite) {
        if (importList == null || importList.isEmpty()) {
            return 0;
        }

        int count = 0;
        if (overwrite) {
            records.clear();
        }

        for (FitnessCheckin checkin : importList) {
            if (checkin.getCheckinDate() == null || checkin.getExerciseType() == null
                    || checkin.getDuration() <= 0) {
                continue;
            }
            String id = checkin.getId();
            if (id == null || id.isEmpty()) {
                id = UUID.randomUUID().toString();
                checkin.setId(id);
            }
            if (checkin.getMood() == null) {
                checkin.setMood("一般");
            }
            records.put(id, checkin);
            count++;
        }

        if (count > 0) {
            saveToFile();
        }
        return count;
    }
}
