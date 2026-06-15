package service;

import domain.CareLog;
import domain.CareType;
import domain.Plant;
import domain.PlantStatistics;
import domain.PlantStatus;
import domain.PlantTemplate;
import domain.TaskItem;
import persistence.PlantJsonStore;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class PlantCareService {
    private final Map<String, Plant> plants;
    private final PlantJsonStore jsonStore;

    public PlantCareService(PlantJsonStore jsonStore) {
        this.jsonStore = jsonStore;
        this.plants = new ConcurrentHashMap<>();
        loadFromStore();
    }

    private void loadFromStore() {
        List<Plant> loadedPlants = jsonStore.loadPlants();
        for (Plant plant : loadedPlants) {
            plants.put(plant.getId(), plant);
        }
    }

    private void saveToStore() {
        jsonStore.savePlants(new ArrayList<>(plants.values()));
    }

    public List<Plant> getAllPlants() {
        return new ArrayList<>(plants.values());
    }

    public List<Plant> getAllPlantsSortedByUrgency() {
        return plants.values().stream()
                .sorted(Comparator.comparingLong(Plant::getDaysUntilNextWatering))
                .collect(Collectors.toList());
    }

    public List<Plant> filterPlantsSortedByUrgency(String location, String status) {
        return plants.values().stream()
                .filter(p -> location == null || location.isEmpty() || location.equalsIgnoreCase(p.getLocation()))
                .filter(p -> status == null || status.isEmpty() ||
                        p.getStatus().name().equalsIgnoreCase(status) ||
                        p.getStatus().getLabel().equals(status))
                .sorted(Comparator.comparingLong(Plant::getDaysUntilNextWatering))
                .collect(Collectors.toList());
    }

    public Plant getPlantById(String id) {
        return plants.get(id);
    }

    public Plant addPlant(Plant plant) {
        String id = UUID.randomUUID().toString();
        plant.setId(id);
        if (plant.getStatus() == null) {
            plant.setStatus(PlantStatus.HEALTHY);
        }
        plants.put(id, plant);
        saveToStore();
        return plant;
    }

    public Plant updatePlant(String id, Plant updatedPlant) {
        Plant existing = plants.get(id);
        if (existing == null) {
            return null;
        }
        existing.setName(updatedPlant.getName());
        existing.setLocation(updatedPlant.getLocation());
        existing.setLightRequirement(updatedPlant.getLightRequirement());
        existing.setStatus(updatedPlant.getStatus() != null ? updatedPlant.getStatus() : existing.getStatus());
        existing.setWateringIntervalDays(updatedPlant.getWateringIntervalDays());
        if (updatedPlant.getPhotoUrl() != null) {
            existing.setPhotoUrl(updatedPlant.getPhotoUrl());
        }
        saveToStore();
        return existing;
    }

    public Plant updatePlantPhoto(String id, String photoUrl) {
        Plant plant = plants.get(id);
        if (plant == null) {
            return null;
        }
        plant.setPhotoUrl(photoUrl);
        saveToStore();
        return plant;
    }

    public Plant updatePlantStatus(String id, PlantStatus status) {
        Plant plant = plants.get(id);
        if (plant == null) {
            return null;
        }
        plant.setStatus(status);
        saveToStore();
        return plant;
    }

    public boolean deletePlant(String id) {
        if (plants.remove(id) != null) {
            saveToStore();
            return true;
        }
        return false;
    }

    public CareLog addCareLog(String plantId, CareType type, String note) {
        Plant plant = plants.get(plantId);
        if (plant == null) {
            return null;
        }
        CareLog log = new CareLog(UUID.randomUUID().toString(), type, note);
        plant.addCareLog(log);
        saveToStore();
        return log;
    }

    public List<CareLog> getRecentCareLogs(String plantId, int count) {
        Plant plant = plants.get(plantId);
        if (plant == null) {
            return new ArrayList<>();
        }
        return plant.getRecentCareLogs(count);
    }

    public Map<LocalDateTime, List<CareLog>> getCareTimeline(String plantId) {
        Plant plant = plants.get(plantId);
        if (plant == null || plant.getCareLogs() == null) {
            return new LinkedHashMap<>();
        }
        return plant.getCareLogs().stream()
                .collect(Collectors.groupingBy(
                        log -> log.getTimestamp().truncatedTo(ChronoUnit.DAYS),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    public List<Plant> filterPlants(String location, String status) {
        return plants.values().stream()
                .filter(p -> location == null || location.isEmpty() || location.equalsIgnoreCase(p.getLocation()))
                .filter(p -> status == null || status.isEmpty() || 
                        p.getStatus().name().equalsIgnoreCase(status) ||
                        p.getStatus().getLabel().equals(status))
                .collect(Collectors.toList());
    }

    public List<Plant> getPlantsNeedingWater() {
        return plants.values().stream()
                .filter(Plant::needsWatering)
                .collect(Collectors.toList());
    }

    public PlantStatistics getStatistics() {
        PlantStatistics stats = new PlantStatistics();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekAgo = now.minusDays(7);
        LocalDateTime monthAgo = now.minusDays(30);

        stats.setTotalPlants(plants.size());
        stats.setNeedingWaterCount((int) plants.values().stream().filter(Plant::needsWatering).count());

        int wateringCount = 0;
        int fertilizingCount = 0;
        int pruningCount = 0;
        int longNeglected = 0;

        Map<String, Integer> locationCount = new HashMap<>();
        Map<PlantStatus, Integer> statusCount = new HashMap<>();

        for (Plant plant : plants.values()) {
            locationCount.merge(plant.getLocation(), 1, Integer::sum);
            statusCount.merge(plant.getStatus(), 1, Integer::sum);

            if (plant.getLastCareTime().isBefore(monthAgo)) {
                longNeglected++;
            }

            if (plant.getCareLogs() != null) {
                for (CareLog log : plant.getCareLogs()) {
                    if (log.getTimestamp().isAfter(weekAgo)) {
                        switch (log.getType()) {
                            case WATERING: wateringCount++; break;
                            case FERTILIZING: fertilizingCount++; break;
                            case PRUNING: pruningCount++; break;
                        }
                    }
                }
            }
        }

        stats.setWateringCountThisWeek(wateringCount);
        stats.setFertilizingCountThisWeek(fertilizingCount);
        stats.setPruningCountThisWeek(pruningCount);
        stats.setLongNeglectedCount(longNeglected);
        stats.setPlantsByLocation(locationCount);
        stats.setPlantsByStatus(statusCount);

        return stats;
    }

    public void importPlants(List<Plant> importedPlants) {
        for (Plant plant : importedPlants) {
            if (plant.getId() == null || plant.getId().isEmpty()) {
                plant.setId(UUID.randomUUID().toString());
            }
            if (plant.getStatus() == null) {
                plant.setStatus(PlantStatus.HEALTHY);
            }
            if (plant.getCreatedAt() == null) {
                plant.setCreatedAt(LocalDateTime.now());
            }
            if (plant.getCareLogs() == null) {
                plant.setCareLogs(new ArrayList<>());
            }
            plants.put(plant.getId(), plant);
        }
        saveToStore();
    }

    public List<Plant> exportPlants() {
        return new ArrayList<>(plants.values());
    }

    public List<TaskItem> getTodayTasks() {
        List<TaskItem> tasks = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime monthAgo = now.minusDays(30);

        for (Plant plant : plants.values()) {
            if (plant.needsWatering()) {
                long daysOverdue = -plant.getDaysUntilNextWatering();
                String reason;
                if (plant.getLastWateredTime() == null) {
                    reason = "从未浇过水";
                } else if (daysOverdue <= 0) {
                    reason = "今天需要浇水";
                } else {
                    reason = "已逾期 " + daysOverdue + " 天";
                }
                tasks.add(new TaskItem(plant.getId(), plant.getName(), "WATERING", reason, "💧"));
            }

            if (plant.getLastCareTime().isBefore(monthAgo)) {
                long days = ChronoUnit.DAYS.between(plant.getLastCareTime(), now);
                tasks.add(new TaskItem(plant.getId(), plant.getName(), "NEGLECTED",
                        "已 " + days + " 天未养护", "⚠️"));
            }

            if (plant.getStatus() == PlantStatus.SICK || plant.getStatus() == PlantStatus.NEEDS_ATTENTION) {
                tasks.add(new TaskItem(plant.getId(), plant.getName(), "STATUS",
                        "状态：" + plant.getStatus().getLabel(), "🏷️"));
            }
        }
        return tasks;
    }

    public List<CareLog> addCareLogsBatch(List<String> plantIds, CareType type, String note) {
        List<CareLog> created = new ArrayList<>();
        for (String plantId : plantIds) {
            CareLog log = addCareLog(plantId, type, note);
            if (log != null) {
                created.add(log);
            }
        }
        return created;
    }

    public List<PlantTemplate> getPlantTemplates() {
        List<PlantTemplate> templates = new ArrayList<>();
        templates.add(new PlantTemplate("pothos", "绿萝", "散射光", 7, PlantStatus.HEALTHY));
        templates.add(new PlantTemplate("succulent", "多肉", "全日照", 14, PlantStatus.HEALTHY));
        templates.add(new PlantTemplate("monstera", "龟背竹", "散射光", 10, PlantStatus.HEALTHY));
        templates.add(new PlantTemplate("money-tree", "发财树", "半日照", 14, PlantStatus.HEALTHY));
        templates.add(new PlantTemplate("spider-plant", "吊兰", "半日照", 5, PlantStatus.HEALTHY));
        templates.add(new PlantTemplate("snake-plant", "虎皮兰", "耐阴", 20, PlantStatus.HEALTHY));
        return templates;
    }
}
