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

    public int getGroupMinSize() {
        return room.getGroupMinSize();
    }

    public void setGroupMinSize(int size) {
        room.setGroupMinSize(size);
        persist();
    }

    public int getGroupMaxSize() {
        return room.getGroupMaxSize();
    }

    public void setGroupMaxSize(int size) {
        room.setGroupMaxSize(size);
        persist();
    }

    public boolean isRequireApproval() {
        return room.isRequireApproval();
    }

    public void setRequireApproval(boolean v) {
        room.setRequireApproval(v);
        persist();
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

        String fingerprintRaw = name.trim() + "|" + (gender != null ? gender : "") + "|" + (department != null ? department : "");
        String fingerprint = String.valueOf(fingerprintRaw.hashCode());

        for (Participant existing : room.getParticipants()) {
            if (!"rejected".equals(existing.getRegisterStatus()) && fingerprint.equals(existing.getFingerprint())) {
                return null;
            }
        }

        String id = UUID.randomUUID().toString().substring(0, 8);
        Participant p = new Participant(id, name.trim());
        p.setGender(gender);
        p.setDepartment(department);
        p.setSkill(skill);
        p.setTag(tag);
        p.setSelfRegistered(selfRegistered);
        p.setFingerprint(fingerprint);

        String registerStatus = room.isRequireApproval() ? "pending" : "approved";
        p.setRegisterStatus(registerStatus);

        room.getParticipants().add(p);

        List<String> affectedIds = new ArrayList<String>();
        affectedIds.add(id);
        saveSnapshotWithMeta("add-participant", "添加参与者: " + p.getName(), null, null, null, affectedIds);

        persist();
        return p;
    }

    public List<Participant> addParticipants(List<String> names) {
        List<Participant> added = new ArrayList<Participant>();
        List<String> affectedIds = new ArrayList<String>();
        for (String name : names) {
            Participant p = addParticipant(name);
            if (p != null) {
                added.add(p);
                affectedIds.add(p.getId());
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

    public boolean approveParticipant(String id) {
        Participant p = findParticipant(id);
        if (p == null) {
            return false;
        }
        if ("approved".equals(p.getRegisterStatus())) {
            return true;
        }
        p.setRegisterStatus("approved");

        List<String> affectedIds = new ArrayList<String>();
        affectedIds.add(id);
        saveSnapshotWithMeta("approve-participant", "审核通过: " + p.getName(), null, null, null, affectedIds);

        persist();
        return true;
    }

    public boolean rejectParticipant(String id) {
        Participant p = findParticipant(id);
        if (p == null) {
            return false;
        }
        if ("rejected".equals(p.getRegisterStatus())) {
            return true;
        }
        if (p.getGroupId() != null) {
            Group g = findGroup(p.getGroupId());
            if (g != null) {
                g.removeParticipant(id);
            }
            p.setGroupId(null);
        }
        p.setRegisterStatus("rejected");

        List<String> affectedIds = new ArrayList<String>();
        affectedIds.add(id);
        saveSnapshotWithMeta("reject-participant", "拒绝加入: " + p.getName(), null, null, null, affectedIds);

        persist();
        return true;
    }

    public void setRules(List<GroupRule> rules) {
        room.setRules(rules != null ? rules : new ArrayList<GroupRule>());
        persist();
    }

    public ActivityTemplate saveAsTemplate(String templateName) {
        String templateId = UUID.randomUUID().toString().substring(0, 8);
        List<GroupRule> rulesCopy = new ArrayList<GroupRule>();
        for (GroupRule r : room.getRules()) {
            GroupRule rc = new GroupRule(r.getType(), r.getValue());
            rulesCopy.add(rc);
        }
        List<String> customFields = new ArrayList<String>();
        ActivityTemplate template = new ActivityTemplate(
                templateId,
                templateName,
                room.getActivityName(),
                room.getGroupCount(),
                rulesCopy,
                customFields,
                System.currentTimeMillis()
        );
        jsonStore.saveTemplate(template);
        return template;
    }

    public boolean applyTemplate(String templateId) {
        ActivityTemplate template = jsonStore.getTemplate(templateId);
        if (template == null) {
            return false;
        }
        room.setActivityName(template.getActivityName());
        room.setGroupCount(template.getGroupCount());

        List<GroupRule> rulesCopy = new ArrayList<GroupRule>();
        if (template.getRules() != null) {
            for (GroupRule r : template.getRules()) {
                GroupRule rc = new GroupRule(r.getType(), r.getValue());
                rulesCopy.add(rc);
            }
        }
        room.setRules(rulesCopy);

        for (Group g : room.getGroups()) {
            for (String pid : g.getParticipantIds()) {
                Participant p = findParticipant(pid);
                if (p != null) {
                    p.setGroupId(null);
                }
            }
        }
        initGroups(room.getGroupCount());

        saveSnapshotWithMeta("apply-template", "应用模板: " + template.getName(), null, null, null, new ArrayList<String>());
        persist();
        return true;
    }

    public void randomGroup() {
        saveSnapshotWithMeta("random", "重新随机分组", null, null, null, new ArrayList<String>());

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
            if (!"approved".equals(p.getRegisterStatus())) {
                continue;
            }
            if (p.getGroupId() == null && !unassigned.contains(p.getId())) {
                unassigned.add(p.getId());
            }
        }

        if (unlockedGroups.isEmpty()) {
            return;
        }

        List<String[]> assignRules = new ArrayList<String[]>();
        List<List<String>> togetherGroups = new ArrayList<List<String>>();
        boolean hasSeparateRule = false;
        boolean hasGenderBalance = false;
        boolean hasDeptSpread = false;
        boolean hasSkillBalance = false;
        boolean hasTagBalance = false;
        int minSize = room.getGroupMinSize();
        int maxSize = room.getGroupMaxSize();
        List<String[]> separatePairs = new ArrayList<String[]>();

        for (GroupRule rule : rules) {
            if ("assign".equals(rule.getType())) {
                String[] parts = rule.getValue().split(",");
                if (parts.length == 2) {
                    assignRules.add(new String[]{parts[0].trim(), parts[1].trim()});
                }
            } else if ("together".equals(rule.getType())) {
                String[] parts = rule.getValue().split(",");
                if (parts.length >= 2) {
                    List<String> names = new ArrayList<String>();
                    for (String part : parts) {
                        names.add(part.trim());
                    }
                    togetherGroups.add(names);
                }
            } else if ("tag-balance".equals(rule.getType())) {
                hasTagBalance = true;
            } else if ("min-size".equals(rule.getType())) {
                try {
                    minSize = Integer.parseInt(rule.getValue().trim());
                    room.setGroupMinSize(minSize);
                } catch (NumberFormatException ignored) {
                }
            } else if ("max-size".equals(rule.getType())) {
                try {
                    maxSize = Integer.parseInt(rule.getValue().trim());
                    room.setGroupMaxSize(maxSize);
                } catch (NumberFormatException ignored) {
                }
            } else if ("separate".equals(rule.getType())) {
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

        Set<String> processedIds = new HashSet<String>();

        for (String[] assign : assignRules) {
            String targetName = assign[0];
            String targetGroupId = assign[1];
            Participant targetP = findParticipantByName(targetName);
            Group targetG = findGroup(targetGroupId);
            if (targetP == null || targetG == null) continue;
            if (targetG.isLocked()) continue;
            if (!unassigned.contains(targetP.getId())) continue;
            if (!"approved".equals(targetP.getRegisterStatus())) continue;

            targetG.addParticipant(targetP.getId());
            targetP.setGroupId(targetG.getId());
            unassigned.remove(targetP.getId());
            processedIds.add(targetP.getId());
        }

        for (List<String> togetherNames : togetherGroups) {
            List<Participant> togetherParts = new ArrayList<Participant>();
            for (String n : togetherNames) {
                Participant tp = findParticipantByName(n);
                if (tp != null && unassigned.contains(tp.getId()) && "approved".equals(tp.getRegisterStatus())) {
                    togetherParts.add(tp);
                }
            }
            if (togetherParts.isEmpty()) continue;

            Group bestGroup = null;
            int minCount = Integer.MAX_VALUE;
            for (Group g : unlockedGroups) {
                if (!g.isLocked() && g.getParticipantIds().size() < minCount) {
                    minCount = g.getParticipantIds().size();
                    bestGroup = g;
                }
            }
            if (bestGroup != null) {
                for (Participant tp : togetherParts) {
                    bestGroup.addParticipant(tp.getId());
                    tp.setGroupId(bestGroup.getId());
                    unassigned.remove(tp.getId());
                    processedIds.add(tp.getId());
                }
            }
        }

        if (hasGenderBalance || hasDeptSpread || hasSkillBalance || hasTagBalance || hasSeparateRule) {
            ruleBasedGroup(unassigned, unlockedGroups, hasGenderBalance, hasDeptSpread, hasSkillBalance, hasTagBalance, separatePairs);
        } else {
            Collections.shuffle(unassigned);
            simpleGroup(unassigned, unlockedGroups);
        }

        if (hasSeparateRule) {
            enforceSeparatePairs(separatePairs, unlockedGroups);
        }

        if (maxSize > 0 || minSize > 0) {
            enforceGroupSizeConstraints(unlockedGroups, minSize, maxSize);
        }

        persist();
    }

    private void enforceGroupSizeConstraints(List<Group> unlockedGroups, int minSize, int maxSize) {
        if (unlockedGroups == null || unlockedGroups.size() < 2) return;

        if (maxSize > 0) {
            boolean changed = true;
            int iterations = 0;
            while (changed && iterations < 50) {
                changed = false;
                iterations++;
                Group oversized = null;
                int maxCount = -1;
                for (Group g : unlockedGroups) {
                    if (!g.isLocked() && g.getParticipantIds().size() > maxSize && g.getParticipantIds().size() > maxCount) {
                        maxCount = g.getParticipantIds().size();
                        oversized = g;
                    }
                }
                if (oversized != null) {
                    Group smallest = null;
                    int minCount = Integer.MAX_VALUE;
                    for (Group g : unlockedGroups) {
                        if (!g.isLocked() && !g.getId().equals(oversized.getId()) && g.getParticipantIds().size() < minCount) {
                            minCount = g.getParticipantIds().size();
                            smallest = g;
                        }
                    }
                    if (smallest != null) {
                        List<String> opids = oversized.getParticipantIds();
                        if (!opids.isEmpty()) {
                            String movePid = opids.remove(opids.size() - 1);
                            smallest.addParticipant(movePid);
                            Participant pp = findParticipant(movePid);
                            if (pp != null) {
                                pp.setGroupId(smallest.getId());
                            }
                            changed = true;
                        }
                    }
                }
            }
        }

        if (minSize > 0) {
            boolean changed = true;
            int iterations = 0;
            while (changed && iterations < 50) {
                changed = false;
                iterations++;
                Group undersized = null;
                int minCount = Integer.MAX_VALUE;
                for (Group g : unlockedGroups) {
                    if (!g.isLocked() && g.getParticipantIds().size() < minSize && g.getParticipantIds().size() < minCount) {
                        minCount = g.getParticipantIds().size();
                        undersized = g;
                    }
                }
                if (undersized != null) {
                    Group largest = null;
                    int maxCount = -1;
                    for (Group g : unlockedGroups) {
                        if (!g.isLocked() && !g.getId().equals(undersized.getId()) && g.getParticipantIds().size() > maxCount) {
                            if (g.getParticipantIds().size() > minSize || maxSize <= 0 || g.getParticipantIds().size() > 1) {
                                maxCount = g.getParticipantIds().size();
                                largest = g;
                            }
                        }
                    }
                    if (largest != null && largest.getParticipantIds().size() > 1) {
                        List<String> lpids = largest.getParticipantIds();
                        String movePid = lpids.remove(lpids.size() - 1);
                        undersized.addParticipant(movePid);
                        Participant pp = findParticipant(movePid);
                        if (pp != null) {
                            pp.setGroupId(undersized.getId());
                        }
                        changed = true;
                    }
                }
            }
        }
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
                                 boolean skillBalance, boolean tagBalance, List<String[]> separatePairs) {
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
            if (!genderBalance && !deptSpread && !tagBalance) {
                return;
            }
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

        if (tagBalance) {
            redistributeByAttribute(unlockedGroups, "tag");
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
            } else if ("tag".equals(attribute)) {
                attr = p.getTag() != null ? p.getTag() : "unknown";
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
            boolean newLocked = !g.isLocked();
            g.setLocked(newLocked);
            String desc = newLocked ? "锁定分组: " + g.getName() : "解锁分组: " + g.getName();
            List<String> affectedIds = new ArrayList<String>(g.getParticipantIds());
            saveSnapshotWithMeta("toggle-lock", desc, null, null, null, affectedIds);
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

        String desc = "移动 " + p.getName() + " 到 " + targetGroup.getName();
        List<String> affectedIds = new ArrayList<String>();
        affectedIds.add(participantId);
        saveSnapshotWithMeta("move-participant", desc, null, null, null, affectedIds);

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

        saveSnapshotWithMeta("undo", "撤销操作: " + lastLog.getDescription(), null, null, null,
                lastLog.getAffectedParticipantIds() != null ? lastLog.getAffectedParticipantIds() : new ArrayList<String>());

        persist();
        return true;
    }

    public boolean restoreVersion(int versionIndex) {
        List<ActionLog> logs = room.getActionLogs();
        if (versionIndex < 0 || versionIndex >= logs.size()) {
            return false;
        }

        ActionLog targetLog = logs.get(versionIndex);
        saveSnapshotWithMeta("restore", "恢复到版本: " + targetLog.getDescription(), null, null, null, new ArrayList<String>());

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

    private void saveSnapshotWithMeta(String action, String desc, String operatorId, String operatorName,
                                       String operatorType, List<String> affectedIds) {
        List<Group> groups = room.getGroups();
        List<Group> snapshot = new ArrayList<Group>();
        for (Group g : groups) {
            Group copy = new Group(g.getId(), g.getName());
            copy.setLocked(g.isLocked());
            copy.setParticipantIds(new ArrayList<String>(g.getParticipantIds()));
            snapshot.add(copy);
        }

        ActionLog log = new ActionLog(action, desc, snapshot);
        log.setOperatorId(operatorId);
        log.setOperatorName(operatorName);
        log.setOperatorType(operatorType);
        log.setAffectedParticipantIds(affectedIds != null ? affectedIds : new ArrayList<String>());

        room.getActionLogs().add(log);

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
