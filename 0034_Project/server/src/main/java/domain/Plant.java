package domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Plant {
    private String id;
    private String name;
    private String location;
    private String lightRequirement;
    private PlantStatus status;
    private int wateringIntervalDays;
    private LocalDateTime lastWateredTime;
    private LocalDateTime createdAt;
    private String photoUrl;
    private List<CareLog> careLogs;

    public Plant() {
        this.careLogs = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        this.status = PlantStatus.HEALTHY;
    }

    public Plant(String id, String name, String location, String lightRequirement,
                 PlantStatus status, int wateringIntervalDays) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.lightRequirement = lightRequirement;
        this.status = status;
        this.wateringIntervalDays = wateringIntervalDays;
        this.careLogs = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
    }

    public boolean needsWatering() {
        if (lastWateredTime == null || wateringIntervalDays <= 0) {
            return true;
        }
        LocalDateTime dueTime = lastWateredTime.plusDays(wateringIntervalDays);
        return LocalDateTime.now().isAfter(dueTime);
    }

    public LocalDateTime getNextWateringTime() {
        if (lastWateredTime == null || wateringIntervalDays <= 0) {
            return LocalDateTime.now();
        }
        return lastWateredTime.plusDays(wateringIntervalDays);
    }

    public long getDaysUntilNextWatering() {
        LocalDateTime nextWatering = getNextWateringTime();
        LocalDateTime now = LocalDateTime.now();
        return java.time.Duration.between(now, nextWatering).toDays();
    }

    public void addCareLog(CareLog log) {
        if (careLogs == null) {
            careLogs = new ArrayList<>();
        }
        careLogs.add(0, log);
        if (log.getType() == CareType.WATERING) {
            this.lastWateredTime = log.getTimestamp();
        }
    }

    public List<CareLog> getRecentCareLogs(int count) {
        if (careLogs == null || careLogs.isEmpty()) {
            return new ArrayList<>();
        }
        return careLogs.subList(0, Math.min(count, careLogs.size()));
    }

    public LocalDateTime getLastCareTime() {
        if (careLogs == null || careLogs.isEmpty()) {
            return createdAt;
        }
        return careLogs.get(0).getTimestamp();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getLightRequirement() {
        return lightRequirement;
    }

    public void setLightRequirement(String lightRequirement) {
        this.lightRequirement = lightRequirement;
    }

    public PlantStatus getStatus() {
        return status;
    }

    public void setStatus(PlantStatus status) {
        this.status = status;
    }

    public int getWateringIntervalDays() {
        return wateringIntervalDays;
    }

    public void setWateringIntervalDays(int wateringIntervalDays) {
        this.wateringIntervalDays = wateringIntervalDays;
    }

    public LocalDateTime getLastWateredTime() {
        return lastWateredTime;
    }

    public void setLastWateredTime(LocalDateTime lastWateredTime) {
        this.lastWateredTime = lastWateredTime;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public List<CareLog> getCareLogs() {
        return careLogs;
    }

    public void setCareLogs(List<CareLog> careLogs) {
        this.careLogs = careLogs;
    }
}
