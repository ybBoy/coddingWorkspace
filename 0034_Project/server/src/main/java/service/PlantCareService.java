package service;

import domain.CareLog;
import domain.Plant;
import persistence.PlantJsonStore;

import java.util.ArrayList;
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

    public Plant getPlantById(String id) {
        return plants.get(id);
    }

    public Plant addPlant(Plant plant) {
        String id = UUID.randomUUID().toString();
        plant.setId(id);
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
        existing.setStatus(updatedPlant.getStatus());
        existing.setWateringIntervalDays(updatedPlant.getWateringIntervalDays());
        saveToStore();
        return existing;
    }

    public Plant updatePlantStatus(String id, String status) {
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

    public CareLog addCareLog(String plantId, String type, String note) {
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

    public List<Plant> getPlantsByLocation(String location) {
        return plants.values().stream()
                .filter(p -> location.equalsIgnoreCase(p.getLocation()))
                .collect(Collectors.toList());
    }

    public List<Plant> getPlantsByStatus(String status) {
        return plants.values().stream()
                .filter(p -> status.equalsIgnoreCase(p.getStatus()))
                .collect(Collectors.toList());
    }

    public List<Plant> filterPlants(String location, String status) {
        return plants.values().stream()
                .filter(p -> location == null || location.isEmpty() || location.equalsIgnoreCase(p.getLocation()))
                .filter(p -> status == null || status.isEmpty() || status.equalsIgnoreCase(p.getStatus()))
                .collect(Collectors.toList());
    }

    public List<Plant> getPlantsNeedingWater() {
        return plants.values().stream()
                .filter(Plant::needsWatering)
                .collect(Collectors.toList());
    }
}
