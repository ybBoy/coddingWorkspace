package domain;

import java.util.Map;

public class PlantStatistics {
    private int totalPlants;
    private int needingWaterCount;
    private int wateringCountThisWeek;
    private int fertilizingCountThisWeek;
    private int pruningCountThisWeek;
    private int longNeglectedCount;
    private Map<String, Integer> plantsByLocation;
    private Map<PlantStatus, Integer> plantsByStatus;

    public int getTotalPlants() {
        return totalPlants;
    }

    public void setTotalPlants(int totalPlants) {
        this.totalPlants = totalPlants;
    }

    public int getNeedingWaterCount() {
        return needingWaterCount;
    }

    public void setNeedingWaterCount(int needingWaterCount) {
        this.needingWaterCount = needingWaterCount;
    }

    public int getWateringCountThisWeek() {
        return wateringCountThisWeek;
    }

    public void setWateringCountThisWeek(int wateringCountThisWeek) {
        this.wateringCountThisWeek = wateringCountThisWeek;
    }

    public int getFertilizingCountThisWeek() {
        return fertilizingCountThisWeek;
    }

    public void setFertilizingCountThisWeek(int fertilizingCountThisWeek) {
        this.fertilizingCountThisWeek = fertilizingCountThisWeek;
    }

    public int getPruningCountThisWeek() {
        return pruningCountThisWeek;
    }

    public void setPruningCountThisWeek(int pruningCountThisWeek) {
        this.pruningCountThisWeek = pruningCountThisWeek;
    }

    public int getLongNeglectedCount() {
        return longNeglectedCount;
    }

    public void setLongNeglectedCount(int longNeglectedCount) {
        this.longNeglectedCount = longNeglectedCount;
    }

    public Map<String, Integer> getPlantsByLocation() {
        return plantsByLocation;
    }

    public void setPlantsByLocation(Map<String, Integer> plantsByLocation) {
        this.plantsByLocation = plantsByLocation;
    }

    public Map<PlantStatus, Integer> getPlantsByStatus() {
        return plantsByStatus;
    }

    public void setPlantsByStatus(Map<PlantStatus, Integer> plantsByStatus) {
        this.plantsByStatus = plantsByStatus;
    }
}
