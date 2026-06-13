package model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;

public class StatusChange {
    private String id;
    private String petId;
    private String petName;
    private PetStatus oldStatus;
    private PetStatus newStatus;
    private String staffName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date time;

    public StatusChange() {}

    public StatusChange(String id, String petId, String petName, PetStatus oldStatus, PetStatus newStatus, String staffName, Date time) {
        this.id = id;
        this.petId = petId;
        this.petName = petName;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.staffName = staffName;
        this.time = time;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPetId() { return petId; }
    public void setPetId(String petId) { this.petId = petId; }
    public String getPetName() { return petName; }
    public void setPetName(String petName) { this.petName = petName; }
    public PetStatus getOldStatus() { return oldStatus; }
    public void setOldStatus(PetStatus oldStatus) { this.oldStatus = oldStatus; }
    public PetStatus getNewStatus() { return newStatus; }
    public void setNewStatus(PetStatus newStatus) { this.newStatus = newStatus; }
    public String getStaffName() { return staffName; }
    public void setStaffName(String staffName) { this.staffName = staffName; }
    public Date getTime() { return time; }
    public void setTime(Date time) { this.time = time; }
}
