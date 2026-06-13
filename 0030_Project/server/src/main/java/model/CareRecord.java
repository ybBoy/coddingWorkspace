package model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;

public class CareRecord {
    private String id;
    private String petId;
    private String petName;
    private String action;
    private String note;
    private String staffName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date time;

    public CareRecord() {}

    public CareRecord(String id, String petId, String petName, String action, String note, String staffName, Date time) {
        this.id = id;
        this.petId = petId;
        this.petName = petName;
        this.action = action;
        this.note = note;
        this.staffName = staffName;
        this.time = time;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPetId() {
        return petId;
    }

    public void setPetId(String petId) {
        this.petId = petId;
    }

    public String getPetName() {
        return petName;
    }

    public void setPetName(String petName) {
        this.petName = petName;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public String getStaffName() {
        return staffName;
    }

    public void setStaffName(String staffName) {
        this.staffName = staffName;
    }
}
