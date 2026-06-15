package domain;

import java.util.List;

public class BatchCareRequest {
    private List<String> plantIds;
    private String type;
    private String note;

    public List<String> getPlantIds() {
        return plantIds;
    }

    public void setPlantIds(List<String> plantIds) {
        this.plantIds = plantIds;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
