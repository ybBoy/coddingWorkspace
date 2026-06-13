package model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;

public class Pet {
    private String id;
    private String name;
    private String breed;
    private String ownerPhoneLast4;
    private PetStatus status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date checkInTime;

    public Pet() {}

    public Pet(String id, String name, String breed, String ownerPhoneLast4, PetStatus status, Date checkInTime) {
        this.id = id;
        this.name = name;
        this.breed = breed;
        this.ownerPhoneLast4 = ownerPhoneLast4;
        this.status = status;
        this.checkInTime = checkInTime;
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

    public String getBreed() {
        return breed;
    }

    public void setBreed(String breed) {
        this.breed = breed;
    }

    public String getOwnerPhoneLast4() {
        return ownerPhoneLast4;
    }

    public void setOwnerPhoneLast4(String ownerPhoneLast4) {
        this.ownerPhoneLast4 = ownerPhoneLast4;
    }

    public PetStatus getStatus() {
        return status;
    }

    public void setStatus(PetStatus status) {
        this.status = status;
    }

    public Date getCheckInTime() {
        return checkInTime;
    }

    public void setCheckInTime(Date checkInTime) {
        this.checkInTime = checkInTime;
    }
}
