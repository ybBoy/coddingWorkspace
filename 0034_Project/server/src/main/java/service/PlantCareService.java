package service;

import domain.CareLog;
import domain.CareType;
import domain.Plant;
import domain.PlantStatistics;
import domain.PlantStatus;
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
}
