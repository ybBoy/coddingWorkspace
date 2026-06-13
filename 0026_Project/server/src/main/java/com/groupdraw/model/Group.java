package com.groupdraw.model;

import java.util.ArrayList;
import java.util.List;

public class Group {
    private String id;
    private String name;
    private boolean locked;
    private List<String> participantIds;

    public Group() {
        this.participantIds = new ArrayList<String>();
    }

    public Group(String id, String name) {
        this.id = id;
        this.name = name;
        this.locked = false;
        this.participantIds = new ArrayList<String>();
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

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public List<String> getParticipantIds() {
        return participantIds;
    }

    public void setParticipantIds(List<String> participantIds) {
        this.participantIds = participantIds;
    }

    public void addParticipant(String participantId) {
        if (!participantIds.contains(participantId)) {
            participantIds.add(participantId);
        }
    }

    public void removeParticipant(String participantId) {
        participantIds.remove(participantId);
    }
}
