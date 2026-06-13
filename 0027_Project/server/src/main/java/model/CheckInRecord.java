package model;

import java.util.List;

public class CheckInRecord {
    private String id;
    private String boothId;
    private Visitor visitor;
    private List<String> interestedProjects;
    private long timestamp;

    public CheckInRecord() {
    }

    public CheckInRecord(String id, String boothId, Visitor visitor, List<String> interestedProjects, long timestamp) {
        this.id = id;
        this.boothId = boothId;
        this.visitor = visitor;
        this.interestedProjects = interestedProjects;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBoothId() {
        return boothId;
    }

    public void setBoothId(String boothId) {
        this.boothId = boothId;
    }

    public Visitor getVisitor() {
        return visitor;
    }

    public void setVisitor(Visitor visitor) {
        this.visitor = visitor;
    }

    public List<String> getInterestedProjects() {
        return interestedProjects;
    }

    public void setInterestedProjects(List<String> interestedProjects) {
        this.interestedProjects = interestedProjects;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
