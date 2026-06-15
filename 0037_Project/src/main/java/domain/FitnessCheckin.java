package domain;

import java.time.LocalDate;

public class FitnessCheckin {
    private String id;
    private LocalDate checkinDate;
    private String exerciseType;
    private int duration;
    private String mood;
    private String note;

    public FitnessCheckin() {
    }

    public FitnessCheckin(String id, LocalDate checkinDate, String exerciseType,
                          int duration, String mood, String note) {
        this.id = id;
        this.checkinDate = checkinDate;
        this.exerciseType = exerciseType;
        this.duration = duration;
        this.mood = mood;
        this.note = note;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LocalDate getCheckinDate() {
        return checkinDate;
    }

    public void setCheckinDate(LocalDate checkinDate) {
        this.checkinDate = checkinDate;
    }

    public String getExerciseType() {
        return exerciseType;
    }

    public void setExerciseType(String exerciseType) {
        this.exerciseType = exerciseType;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public String getMood() {
        return mood;
    }

    public void setMood(String mood) {
        this.mood = mood;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
