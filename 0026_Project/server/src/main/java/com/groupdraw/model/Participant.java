package com.groupdraw.model;

public class Participant {
    private String id;
    private String name;
    private String groupId;
    private String gender;
    private String department;
    private int skill;
    private String tag;
    private boolean selfRegistered;
    private String registerStatus;
    private String fingerprint;

    public Participant() {
        this.registerStatus = "approved";
    }

    public Participant(String id, String name) {
        this.id = id;
        this.name = name;
        this.skill = 0;
        this.selfRegistered = false;
        this.registerStatus = "approved";
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

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public int getSkill() {
        return skill;
    }

    public void setSkill(int skill) {
        this.skill = skill;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public boolean isSelfRegistered() {
        return selfRegistered;
    }

    public void setSelfRegistered(boolean selfRegistered) {
        this.selfRegistered = selfRegistered;
    }

    public String getRegisterStatus() {
        return registerStatus;
    }

    public void setRegisterStatus(String registerStatus) {
        this.registerStatus = registerStatus;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }
}
