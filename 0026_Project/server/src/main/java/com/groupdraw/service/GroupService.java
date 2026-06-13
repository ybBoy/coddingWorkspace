package com.groupdraw.service;

import com.groupdraw.model.*;
import com.groupdraw.store.JsonStore;

import java.util.*;

public class GroupService {
    private JsonStore jsonStore;
    private Room room;

    public GroupService(JsonStore jsonStore, Room room) {
        this.jsonStore = jsonStore;
        this.room = room;

        if (room.getGroups().isEmpty() && room.getGroupCount() > 0) {
            initGroups(room.getGroupCount());
        }
    }

    private void initGroups(int count) {
        room.getGroups().clear();
        for (int i = 0; i < count; i++) {
            Group group = new Group("group-" + (i + 1), "第" + (i + 1) + "组");
            room.getGroups().add(group);
        }
        persist();
    }

    public String getActivityName() {
        return room.getActivityName();
    }

    public void setActivityName(String name) {
        room.setActivityName(name);
        persist();
    }

    public List<Participant> getParticipants() {
        return room.getParticipants();
    }

    public List<Group> getGroups() {
        return room.getGroups();
    }

    public List<ActionLog> getActionLogs() {
        return room.getActionLogs();
    }

    public List<GroupRule> getRules() {
        return room.getRules();
    }

    public int getGroupCount() {
        return room.getGroupCount();
    }

    public String getRoomCode() {
        return room.getCode();
    }

    public String getHostToken() {
        return room.getHostToken();
    }

    public boolean verifyHost(String token) {
        return room.getHostToken() != null && room.getHostToken().equals(token);
    }

    public void setGroupCount(int count) {
        if (count <= 0 || count > 20) {
            return;
        }
        room.setGroupCount(count);
        List<Group> groups = room.getGroups();

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
        persist();
    }

    public Participant addParticipant(String name) {
        return addParticipant(name, null, null, 0, null, false);
    }

    public Participant addParticipant(String name, String gender, String department, int skill, String tag, boolean selfRegistered) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        String id = UUID.randomUUID().toString().substring(0, 8);
        Participant p = new Participant(id, name.trim());
        p.setGender(gender);
        p.setDepartment(department);
        p.setSkill(skill);
        p.setTag(tag);
        p.setSelfRegistered(selfRegistered);
        room.getParticipants().add(p);
        persist();
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

    public boolean updateParticipant(String id, String gender, String department, int skill, String tag) {
        Participant p = findParticipant(id);
        if (p == null) return false;
        p.setGender(gender);
        p.setDepartment(department);
        p.setSkill(skill);
        p.setTag(tag);
        persist();
        return true;
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
        room.getParticipants().remove(p);
        persist();
        return true;
    }

    public void clearParticipants() {
        room.getParticipants().clear();
        for (Group g : room.getGroups()) {
            g.getParticipantIds().clear();
        }
        persist();
    }

    public void setRules(List<GroupRule> rules) {
        room.setRules(rules != null ? rules : new ArrayList<GroupRule>());
        persist();
    }

    public void randomGroup() {
        saveSnapshot("random", "重新随机分组");

        List<Group> groups = room.getGroups();
        List<Participant> participants = room.getParticipants();
        List<GroupRule> rules = room.getRules();

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

        if (unlockedGroups.isEmpty()) {
            return;
        }

        boolean hasSeparateRule = false;
        boolean hasGenderBalance = false;
        boolean hasDeptSpread = false;
        boolean hasSkillBalance = false;
        List<String[]> separatePairs = new ArrayList<String[]>();

        for (GroupRule rule : rules) {
            if ("separate".equals(rule.getType())) {
                hasSeparateRule = true;
                String[] parts = rule.getValue().split(",");
                if (parts.length == 2) {
                    separatePairs.add(parts);
                }
            } else if ("gender-balance".equals(rule.getType())) {
                hasGenderBalance = true;
            } else if ("dept-spread".equals(rule.getType())) {
                hasDeptSpread = true;
            } else if ("skill-balance".equals(rule.getType())) {
                hasSkillBalance = true;
            }
        }

        if (hasGenderBalance || hasDeptSpread || hasSkillBalance || hasSeparateRule) {
            ruleBasedGroup(unassigned, unlockedGroups, hasGenderBalance, hasDeptSpread, hasSkillBalance, separatePairs);
        } else {
            Collections.shuffle(unassigned);
            simpleGroup(unassigned, unlockedGroups);
        }

        if (hasSeparateRule) {
            enforceSeparatePairs(separatePairs, unlockedGroups);
        }

        persist();
    }

    private void simpleGroup(List<String> unassigned, List<Group> unlockedGroups) {
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
    }

    private void ruleBasedGroup(List<String> unassigned, List<Group> unlockedGroups,
                                 boolean genderBalance, boolean deptSpread,
                                 boolean skillBalance, List<String[]> separatePairs) {
        List<Participant> unassignedParts = new ArrayList<Participant>();
        for (String pid : unassigned) {
            Participant p = findParticipant(pid);
            if (p != null) {
                unassignedParts.add(p);
            }
        }

        int groupCount = unlockedGroups.size();

        if (skillBalance) {
            Collections.sort(unassignedParts, new Comparator<Participant>() {
                @Override
                public int compare(Participant a, Participant b) {
                    return Integer.compare(b.getSkill(), a.getSkill());
                }
            });

            for (int i = 0; i < unassignedParts.size(); i++) {
                Participant p = unassignedParts.get(i);
                Group targetGroup = unlockedGroups.get(i % groupCount);
                targetGroup.addParticipant(p.getId());
                p.setGroupId(targetGroup.getId());
            }
        } else {
            Collections.shuffle(unassignedParts);
            simpleGroup(unassigned, unlockedGroups);
            return;
        }

        if (genderBalance) {
            List<String> genders = new ArrayList<String>();
            for (Participant p : unassignedParts) {
                if (p.getGender() != null && !p.getGender().isEmpty()) {
                    genders.add(p.getGender());
                }
            }

            if (!genders.isEmpty()) {
                redistributeByAttribute(unlockedGroups, "gender");
            }
        }

        if (deptSpread) {
            redistributeByAttribute(unlockedGroups, "department");
        }
    }

    private void redistributeByAttribute(List<Group> unlockedGroups, String attribute) {
        Map<String, List<String>> byAttr = new LinkedHashMap<String, List<String>>();
        List<String> allPids = new ArrayList<String>();

        for (Group g : unlockedGroups) {
            allPids.addAll(g.getParticipantIds());
        }

        for (String pid : allPids) {
            Participant p = findParticipant(pid);
            if (p == null) continue;
            String attr = "unknown";
            if ("gender".equals(attribute)) {
                attr = p.getGender() != null ? p.getGender() : "unknown";
            } else if ("department".equals(attribute)) {
                attr = p.getDepartment() != null ? p.getDepartment() : "unknown";
            }
            if (!byAttr.containsKey(attr)) {
                byAttr.put(attr, new ArrayList<String>());
            }
            byAttr.get(attr).add(pid);
        }

        for (Group g : unlockedGroups) {
            g.getParticipantIds().clear();
        }
        for (Participant p : room.getParticipants()) {
            if (!isInLockedGroup(p.getId())) {
                p.setGroupId(null);
            }
        }

        int groupCount = unlockedGroups.size();
        for (Map.Entry<String, List<String>> entry : byAttr.entrySet()) {
            List<String> pids = entry.getValue();
            Collections.shuffle(pids);
            for (int i = 0; i < pids.size(); i++) {
                Group target = unlockedGroups.get(i % groupCount);
                String pid = pids.get(i);
                target.addParticipant(pid);
                Participant p = findParticipant(pid);
                if (p != null) {
                    p.setGroupId(target.getId());
                }
            }
        }
    }

    private boolean isInLockedGroup(String pid) {
        for (Group g : room.getGroups()) {
            if (g.isLocked() && g.getParticipantIds().contains(pid)) {
                return true;
            }
        }
        return false;
    }

    private void enforceSeparatePairs(List<String[]> separatePairs, List<Group> unlockedGroups) {
        for (String[] pair : separatePairs) {
            if (pair.length != 2) continue;
            Participant p1 = findParticipantByName(pair[0].trim());
            Participant p2 = findParticipantByName(pair[1].trim());
            if (p1 == null || p2 == null) continue;
            if (p1.getGroupId() == null || p2.getGroupId() == null) continue;
            if (p1.getGroupId().equals(p2.getGroupId())) {
                Group g1 = findGroup(p1.getGroupId());
                if (g1 == null || g1.isLocked()) continue;

                Group bestTarget = null;
                int minSize = Integer.MAX_VALUE;
                for (Group g : unlockedGroups) {
                    if (!g.getId().equals(p2.getGroupId()) && !g.isLocked()) {
                        if (g.getParticipantIds().size() < minSize) {
                            minSize = g.getParticipantIds().size();
                            bestTarget = g;
                        }
                    }
                }

                if (bestTarget != null) {
                    g1.removeParticipant(p1.getId());
                    bestTarget.addParticipant(p1.getId());
                    p1.setGroupId(bestTarget.getId());
                }
            }
        }
    }

    public void toggleGroupLock(String groupId) {
        Group g = findGroup(groupId);
        if (g != null) {
            g.setLocked(!g.isLocked());
            persist();
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

        persist();
        return true;
    }

    public boolean undo() {
        List<ActionLog> logs = room.getActionLogs();
        if (logs.isEmpty()) {
            return false;
        }

        ActionLog lastLog = logs.remove(logs.size() - 1);
        restoreFromSnapshot(lastLog.getGroupsSnapshot());

        persist();
        return true;
    }

    public boolean restoreVersion(int versionIndex) {
        List<ActionLog> logs = room.getActionLogs();
        if (versionIndex < 0 || versionIndex >= logs.size()) {
            return false;
        }

        ActionLog targetLog = logs.get(versionIndex);
        saveSnapshot("restore", "恢复到版本: " + targetLog.getDescription());

        restoreFromSnapshot(targetLog.getGroupsSnapshot());

        persist();
        return true;
    }

    private void restoreFromSnapshot(List<Group> snapshot) {
        List<Group> groups = room.getGroups();
        groups.clear();

        for (Group g : snapshot) {
            Group newGroup = new Group(g.getId(), g.getName());
            newGroup.setLocked(g.isLocked());
            newGroup.setParticipantIds(new ArrayList<String>(g.getParticipantIds()));
            groups.add(newGroup);
        }

        for (Participant p : room.getParticipants()) {
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
    }

    private void saveSnapshot(String action, String description) {
        List<Group> groups = room.getGroups();
        List<Group> snapshot = new ArrayList<Group>();
        for (Group g : groups) {
            Group copy = new Group(g.getId(), g.getName());
            copy.setLocked(g.isLocked());
            copy.setParticipantIds(new ArrayList<String>(g.getParticipantIds()));
            snapshot.add(copy);
        }
        room.getActionLogs().add(new ActionLog(action, description, snapshot));

        while (room.getActionLogs().size() > 50) {
            room.getActionLogs().remove(0);
        }
    }

    private Participant findParticipant(String id) {
        for (Participant p : room.getParticipants()) {
            if (p.getId().equals(id)) {
                return p;
            }
        }
        return null;
    }

    private Participant findParticipantByName(String name) {
        for (Participant p : room.getParticipants()) {
            if (p.getName().equals(name)) {
                return p;
            }
        }
        return null;
    }

    private Group findGroup(String id) {
        for (Group g : room.getGroups()) {
            if (g.getId().equals(id)) {
                return g;
            }
        }
        return null;
    }

    private void persist() {
        jsonStore.putRoom(room);
    }

    public void save() {
        persist();
        jsonStore.save();
    }
}
