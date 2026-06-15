package domain;

public class PlantTemplate {
    private String key;
    private String name;
    private String lightRequirement;
    private int wateringIntervalDays;
    private PlantStatus defaultStatus;

    public PlantTemplate() {}

    public PlantTemplate(String key, String name, String lightRequirement,
                         int wateringIntervalDays, PlantStatus defaultStatus) {
        this.key = key;
        this.name = name;
        this.lightRequirement = lightRequirement;
        this.wateringIntervalDays = wateringIntervalDays;
        this.defaultStatus = defaultStatus;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLightRequirement() {
        return lightRequirement;
    }

    public void setLightRequirement(String lightRequirement) {
        this.lightRequirement = lightRequirement;
    }

    public int getWateringIntervalDays() {
        return wateringIntervalDays;
    }

    public void setWateringIntervalDays(int wateringIntervalDays) {
        this.wateringIntervalDays = wateringIntervalDays;
    }

    public PlantStatus getDefaultStatus() {
        return defaultStatus;
    }

    public void setDefaultStatus(PlantStatus defaultStatus) {
        this.defaultStatus = defaultStatus;
    }
}
