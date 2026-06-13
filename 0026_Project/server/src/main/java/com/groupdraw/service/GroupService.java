package com.groupdraw.service;

import com.groupdraw.model.ActionLog;
import com.groupdraw.model.Group;
import com.groupdraw.model.Participant;
import com.groupdraw.store.JsonStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class GroupService {
    private JsonStore jsonStore;
    private List<Group> groups;
    private List<Participant> participants;
    private List<ActionLog> actionLogs;

    public GroupService(JsonStore jsonStore) {
        this.jsonStore = jsonStore;
        this.participants = jsonStore.getParticipants();
        this.groups = jsonStore.getGroups();
        this.actionLogs = jsonStore.getActionLogs();

        if (groups.isEmpty() && jsonStore.getGroupCount() > 0) {
            initGroups(jsonStore.getGroupCount());
        }
    }

    private void initGroups(int count) {
        groups.clear();
        for (int i = 0; i < count; i++) {
            Group group = new Group("group-" + (i + 1), "第" + (i + 1) + "组");
            groups.add(group);
        }
        jsonStore.setGroups(groups);
    }

    public String getActivityName() {
        return jsonStore.getActivityName();
    }

    public void setActivityName(String name) {
        jsonStore.setActivityName(name);
    }

    public List<Participant> getParticipants() {
        return participants;
    }

    public List<Group> getGroups() {
        return groups;
    }

    public List<ActionLog> getActionLogs() {
        return actionLogs;
    }

    public int getGroupCount() {
        return jsonStore.getGroupCount();
    }

    public void setGroupCount(int count) {
        if (count <= 0 || count > 20) {
            return;
        }
        jsonStore.setGroupCount(count);

        while (groups.size() < count) {
            int idx = groups.size() + 1;
            groups.add(new Group("group-" + idx, "第" + idx + "组"));
        }
        while (groups.size() > count) {
            Group removed = groups.remove(groups.size() - 1);
            for (String pid : removed.getParticipantIds()) {
                Participant p = findParticipant(pid);
                if (p != null) {
                    p.setGroupId(null);
                }
            }
        }
        jsonStore.setGroups(groups);
    }

    public Participant addParticipant(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        String id = UUID.randomUUID().toString().substring(0, 8);
        Participant p = new Participant(id, name.trim());
        participants.add(p);
        jsonStore.setParticipants(participants);
        return p;
    }

    public List<Participant> addParticipants(List<String> names) {
        List<Participant> added = new ArrayList<Participant>();
        for (String name : names) {
            Participant p = addParticipant(name);
            if (p != null) {
                added.add(p);
            }
        }
        return added;
    }

    public boolean removeParticipant(String participantId) {
        Participant p = findParticipant(participantId);
        if (p == null) {
            return false;
        }
        if (p.getGroupId() != null) {
            Group g = findGroup(p.getGroupId());
            if (g != null) {
                g.removeParticipant(participantId);
            }
        }
        participants.remove(p);
        jsonStore.setParticipants(participants);
        jsonStore.setGroups(groups);
        return true;
    }

    public void clearParticipants() {
        participants.clear();
        for (Group g : groups) {
            g.getParticipantIds().clear();
        }
        jsonStore.setParticipants(participants);
        jsonStore.setGroups(groups);
    }

    public void randomGroup() {
        saveSnapshot("random", "重新随机分组");

        List<String> unassigned = new ArrayList<String>();
        List<Group> unlockedGroups = new ArrayList<Group>();

        for (Group g : groups) {
            if (g.isLocked()) {
                for (String pid : g.getParticipantIds()) {
                    Participant p = findParticipant(pid);
                    if (p != null) {
                        p.setGroupId(g.getId());
                    }
                }
            } else {
                for (String pid : g.getParticipantIds()) {
                    unassigned.add(pid);
                    Participant p = findParticipant(pid);
                    if (p != null) {
                        p.setGroupId(null);
                    }
                }
                g.getParticipantIds().clear();
                unlockedGroups.add(g);
            }
        }

        for (Participant p : participants) {
            if (p.getGroupId() == null && !unassigned.contains(p.getId())) {
                unassigned.add(p.getId());
            }
        }

        Collections.shuffle(unassigned);

        if (unlockedGroups.isEmpty()) {
            return;
        }

        int idx = 0;
        for (String pid : unassigned) {
            Group targetGroup = unlockedGroups.get(idx % unlockedGroups.size());
            targetGroup.addParticipant(pid);
            Participant p = findParticipant(pid);
            if (p != null) {
                p.setGroupId(targetGroup.getId());
            }
            idx++;
        }

        jsonStore.setParticipants(participants);
        jsonStore.setGroups(groups);
    }

    public void toggleGroupLock(String groupId) {
        Group g = findGroup(groupId);
        if (g != null) {
            g.setLocked(!g.isLocked());
            jsonStore.setGroups(groups);
        }
    }

    public boolean moveParticipant(String participantId, String targetGroupId) {
        Participant p = findParticipant(participantId);
        Group targetGroup = findGroup(targetGroupId);

        if (p == null || targetGroup == null) {
            return false;
        }

        Group sourceGroup = null;
        if (p.getGroupId() != null) {
            sourceGroup = findGroup(p.getGroupId());
        }

        if (sourceGroup != null && sourceGroup.isLocked()) {
            return false;
        }
        if (targetGroup.isLocked()) {
            return false;
        }

        if (sourceGroup != null) {
            sourceGroup.removeParticipant(participantId);
        }

        targetGroup.addParticipant(participantId);
        p.setGroupId(targetGroupId);

        jsonStore.setParticipants(participants);
        jsonStore.setGroups(groups);
        return true;
    }

    public boolean undo() {
        if (actionLogs.isEmpty()) {
            return false;
        }

        ActionLog lastLog = actionLogs.remove(actionLogs.size() - 1);
        List<Group> snapshot = lastLog.getGroupsSnapshot();

        groups.clear();
        for (Group g : snapshot) {
            Group newGroup = new Group(g.getId(), g.getName());
            newGroup.setLocked(g.isLocked());
            newGroup.setParticipantIds(new ArrayList<String>(g.getParticipantIds()));
            groups.add(newGroup);
        }

        for (Participant p : participants) {
            p.setGroupId(null);
        }
        for (Group g : groups) {
            for (String pid : g.getParticipantIds()) {
                Participant p = findParticipant(pid);
                if (p != null) {
                    p.setGroupId(g.getId());
                }
            }
        }

        jsonStore.setGroups(groups);
        jsonStore.setParticipants(participants);
        jsonStore.setActionLogs(actionLogs);
        return true;
    }

    private void saveSnapshot(String action, String description) {
        List<Group> snapshot = new ArrayList<Group>();
        for (Group g : groups) {
            Group copy = new Group(g.getId(), g.getName());
            copy.setLocked(g.isLocked());
            copy.setParticipantIds(new ArrayList<String>(g.getParticipantIds()));
            snapshot.add(copy);
        }
        actionLogs.add(new ActionLog(action, description, snapshot));

        while (actionLogs.size() > 20) {
            actionLogs.remove(0);
        }

        jsonStore.setActionLogs(actionLogs);
    }

    private Participant findParticipant(String id) {
        for (Participant p : participants) {
            if (p.getId().equals(id)) {
                return p;
            }
        }
        return null;
    }

    private Group findGroup(String id) {
        for (Group g : groups) {
            if (g.getId().equals(id)) {
                return g;
            }
        }
        return null;
    }

    public void save() {
        jsonStore.save();
    }
}
