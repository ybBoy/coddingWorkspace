package service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import model.Booth;
import model.CheckInRecord;
import model.Visitor;
import store.JsonStore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ExpoService {

    private static final long DEDUP_WINDOW_MS = 600000L;
    private static final long AUTO_SAVE_INTERVAL_MS = 30000L;
    private static final int DEFAULT_PEAK_WINDOW_MINUTES = 5;
    private static final int DEFAULT_PEAK_THRESHOLD = 20;

    private static final List<String> DEFAULT_PROJECTS = Collections.unmodifiableList(Arrays.asList(
            "智能对话", "机器视觉", "边缘计算", "智能穿戴",
            "AR/VR", "数字艺术", "互动游戏", "开源硬件"
    ));

    public static final List<String> DEFAULT_PROJECTS_VIEW = DEFAULT_PROJECTS;

    private static ExpoService instance;

    private final List<Booth> booths;
    private final List<CheckInRecord> records;
    private final List<String> projects;
    private final JsonStore jsonStore;
    private final Gson gson;

    private ExpoService() {
        this.jsonStore = new JsonStore();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        JsonStore.StoreData data = jsonStore.load();
        this.booths = Collections.synchronizedList(data.booths);
        this.records = Collections.synchronizedList(data.records);
        this.projects = Collections.synchronizedList(data.projects);
        jsonStore.startAutoSave(new Runnable() {
            @Override
            public void run() {
                saveData();
            }
        }, AUTO_SAVE_INTERVAL_MS);
    }

    public static synchronized ExpoService getInstance() {
        if (instance == null) {
            instance = new ExpoService();
        }
        return instance;
    }

    private void saveData() {
        List<Booth> boothsCopy;
        List<CheckInRecord> recordsCopy;
        List<String> projectsCopy;
        synchronized (booths) {
            boothsCopy = new ArrayList<>(booths);
        }
        synchronized (records) {
            recordsCopy = new ArrayList<>(records);
        }
        synchronized (projects) {
            projectsCopy = new ArrayList<>(projects);
        }
        jsonStore.save(new JsonStore.StoreData(boothsCopy, recordsCopy, projectsCopy));
    }

    public List<Booth> getBooths() {
        synchronized (booths) {
            List<Booth> result = new ArrayList<>();
            for (Booth b : booths) {
                if (!b.isDisabled()) {
                    result.add(b);
                }
            }
            return result;
        }
    }

    public List<Booth> getAllBooths() {
        synchronized (booths) {
            return new ArrayList<>(booths);
        }
    }

    public Booth addBooth(String name, String description) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("展位名称不能为空");
        }
        String newId;
        synchronized (booths) {
            int maxNum = 0;
            for (Booth b : booths) {
                String id = b.getId();
                if (id != null && id.startsWith("B")) {
                    try {
                        int n = Integer.parseInt(id.substring(1));
                        if (n > maxNum) maxNum = n;
                    } catch (Exception ignored) {
                    }
                }
            }
            newId = String.format("B%03d", maxNum + 1);
            Booth booth = new Booth(newId, name.trim(), description != null ? description.trim() : "");
            booths.add(booth);
            saveData();
            return booth;
        }
    }

    public Booth updateBooth(String id, String name, String description, Boolean disabled) {
        if (id == null) {
            throw new IllegalArgumentException("展位ID不能为空");
        }
        synchronized (booths) {
            for (Booth booth : booths) {
                if (id.equals(booth.getId())) {
                    if (name != null) booth.setName(name.trim());
                    if (description != null) booth.setDescription(description.trim());
                    if (disabled != null) booth.setDisabled(disabled);
                    saveData();
                    return booth;
                }
            }
            throw new IllegalArgumentException("展位不存在: " + id);
        }
    }

    public void deleteBooth(String id) {
        if (id == null) {
            throw new IllegalArgumentException("展位ID不能为空");
        }
        synchronized (booths) {
            boolean removed = false;
            for (int i = booths.size() - 1; i >= 0; i--) {
                if (id.equals(booths.get(i).getId())) {
                    booths.remove(i);
                    removed = true;
                    break;
                }
            }
            if (!removed) {
                throw new IllegalArgumentException("展位不存在: " + id);
            }
            saveData();
        }
    }

    public List<String> getProjects() {
        synchronized (projects) {
            return new ArrayList<>(projects);
        }
    }

    public void addProject(String projectName) {
        if (projectName == null || projectName.trim().isEmpty()) {
            throw new IllegalArgumentException("项目名称不能为空");
        }
        String name = projectName.trim();
        synchronized (projects) {
            if (projects.contains(name)) {
                throw new IllegalArgumentException("项目已存在: " + name);
            }
            projects.add(name);
            saveData();
        }
    }

    public void updateProject(String oldName, String newName) {
        if (oldName == null || newName == null || newName.trim().isEmpty()) {
            throw new IllegalArgumentException("项目名称不能为空");
        }
        String newTrim = newName.trim();
        synchronized (projects) {
            int idx = projects.indexOf(oldName);
            if (idx < 0) {
                throw new IllegalArgumentException("项目不存在: " + oldName);
            }
            if (projects.contains(newTrim) && !oldName.equals(newTrim)) {
                throw new IllegalArgumentException("项目名称已被占用: " + newTrim);
            }
            projects.set(idx, newTrim);
            synchronized (records) {
                for (CheckInRecord record : records) {
                    List<String> ps = record.getInterestedProjects();
                    if (ps != null) {
                        for (int i = 0; i < ps.size(); i++) {
                            if (oldName.equals(ps.get(i))) {
                                ps.set(i, newTrim);
                            }
                        }
                    }
                }
            }
            saveData();
        }
    }

    public void deleteProject(String projectName) {
        if (projectName == null) {
            throw new IllegalArgumentException("项目名称不能为空");
        }
        synchronized (projects) {
            boolean removed = projects.remove(projectName);
            if (!removed) {
                throw new IllegalArgumentException("项目不存在: " + projectName);
            }
            synchronized (records) {
                for (CheckInRecord record : records) {
                    List<String> ps = record.getInterestedProjects();
                    if (ps != null) {
                        for (int i = ps.size() - 1; i >= 0; i--) {
                            if (projectName.equals(ps.get(i))) {
                                ps.remove(i);
                            }
                        }
                    }
                }
            }
            saveData();
        }
    }

    public CheckInRecord checkIn(String boothId, Visitor visitor, List<String> interestedProjects) {
        if (boothId == null || visitor == null) {
            throw new IllegalArgumentException("参数不能为空");
        }

        boolean boothExists = false;
        synchronized (booths) {
            for (Booth booth : booths) {
                if (boothId.equals(booth.getId()) && !booth.isDisabled()) {
                    boothExists = true;
                    break;
                }
            }
        }
        if (!boothExists) {
            throw new IllegalArgumentException("展位不存在或已停用: " + boothId);
        }

        String phoneSuffix = visitor.getPhoneSuffix();
        long now = System.currentTimeMillis();
        if (phoneSuffix != null && !phoneSuffix.isEmpty()) {
            synchronized (records) {
                for (CheckInRecord record : records) {
                    if (boothId.equals(record.getBoothId())
                            && record.getVisitor() != null
                            && phoneSuffix.equals(record.getVisitor().getPhoneSuffix())
                            && (now - record.getTimestamp()) < DEDUP_WINDOW_MS) {
                        throw new IllegalStateException("该手机号用户10分钟内已在此展位签到，请稍后再试");
                    }
                }
            }
        }

        String recordId = UUID.randomUUID().toString().replace("-", "");
        CheckInRecord newRecord = new CheckInRecord(
                recordId,
                boothId,
                visitor,
                interestedProjects != null ? interestedProjects : new ArrayList<String>(),
                now
        );

        records.add(newRecord);
        return newRecord;
    }

    public List<CheckInRecord> getAllRecords() {
        List<CheckInRecord> result;
        synchronized (records) {
            result = new ArrayList<>(records);
        }
        Collections.sort(result, new Comparator<CheckInRecord>() {
            @Override
            public int compare(CheckInRecord r1, CheckInRecord r2) {
                return Long.compare(r2.getTimestamp(), r1.getTimestamp());
            }
        });
        return result;
    }

    public List<CheckInRecord> getRecordsByBooth(String boothId) {
        List<CheckInRecord> filtered = new ArrayList<>();
        synchronized (records) {
            for (CheckInRecord record : records) {
                if (boothId.equals(record.getBoothId())) {
                    filtered.add(record);
                }
            }
        }
        Collections.sort(filtered, new Comparator<CheckInRecord>() {
            @Override
            public int compare(CheckInRecord r1, CheckInRecord r2) {
                return Long.compare(r2.getTimestamp(), r1.getTimestamp());
            }
        });
        return filtered;
    }

    public List<CheckInRecord> getRecentRecords(int limit) {
        List<CheckInRecord> all = getAllRecords();
        if (limit <= 0 || all.size() <= limit) {
            return all;
        }
        return all.subList(0, limit);
    }

    public List<CheckInRecord> getRecordsByTimeRange(long startTime, long endTime) {
        List<CheckInRecord> filtered = new ArrayList<>();
        synchronized (records) {
            for (CheckInRecord record : records) {
                if (record.getTimestamp() >= startTime && record.getTimestamp() <= endTime) {
                    filtered.add(record);
                }
            }
        }
        Collections.sort(filtered, new Comparator<CheckInRecord>() {
            @Override
            public int compare(CheckInRecord r1, CheckInRecord r2) {
                return Long.compare(r2.getTimestamp(), r1.getTimestamp());
            }
        });
        return filtered;
    }

    private long getTodayStart() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    public Map<String, Long> getTodayBoothStats() {
        long todayStart = getTodayStart();
        long now = System.currentTimeMillis();
        Map<String, Long> stats = new HashMap<>();
        synchronized (booths) {
            for (Booth booth : booths) {
                if (!booth.isDisabled()) {
                    stats.put(booth.getId(), 0L);
                }
            }
        }
        synchronized (records) {
            for (CheckInRecord record : records) {
                if (record.getTimestamp() >= todayStart && record.getTimestamp() <= now) {
                    String id = record.getBoothId();
                    if (stats.containsKey(id)) {
                        stats.put(id, stats.get(id) + 1);
                    }
                }
            }
        }
        return stats;
    }

    public Map<String, Long> getTodayProjectStats() {
        long todayStart = getTodayStart();
        long now = System.currentTimeMillis();
        Map<String, Long> stats = new HashMap<>();
        synchronized (records) {
            for (CheckInRecord record : records) {
                if (record.getTimestamp() >= todayStart && record.getTimestamp() <= now) {
                    List<String> ps = record.getInterestedProjects();
                    if (ps != null) {
                        for (String p : ps) {
                            if (stats.containsKey(p)) {
                                stats.put(p, stats.get(p) + 1);
                            } else {
                                stats.put(p, 1L);
                            }
                        }
                    }
                }
            }
        }
        return stats;
    }

    public long getTodayTotal() {
        long todayStart = getTodayStart();
        long now = System.currentTimeMillis();
        long count = 0;
        synchronized (records) {
            for (CheckInRecord record : records) {
                if (record.getTimestamp() >= todayStart && record.getTimestamp() <= now) {
                    count++;
                }
            }
        }
        return count;
    }

    public Map<String, Long> getBoothStats() {
        Map<String, Long> stats = new HashMap<>();
        synchronized (booths) {
            for (Booth booth : booths) {
                if (!booth.isDisabled()) {
                    stats.put(booth.getId(), 0L);
                }
            }
        }
        synchronized (records) {
            for (CheckInRecord record : records) {
                String id = record.getBoothId();
                if (stats.containsKey(id)) {
                    stats.put(id, stats.get(id) + 1);
                }
            }
        }
        return stats;
    }

    public Map<String, Long> getProjectStats() {
        Map<String, Long> stats = new HashMap<>();
        synchronized (records) {
            for (CheckInRecord record : records) {
                List<String> ps = record.getInterestedProjects();
                if (ps != null) {
                    for (String p : ps) {
                        if (stats.containsKey(p)) {
                            stats.put(p, stats.get(p) + 1);
                        } else {
                            stats.put(p, 1L);
                        }
                    }
                }
            }
        }
        return stats;
    }

    public List<String> getPeakBooths(int windowMinutes, int threshold) {
        List<String> peakBooths = new ArrayList<>();
        long windowMs = windowMinutes * 60L * 1000L;
        long now = System.currentTimeMillis();

        Map<String, Long> counts = new HashMap<>();
        synchronized (records) {
            for (CheckInRecord record : records) {
                if ((now - record.getTimestamp()) <= windowMs) {
                    String boothId = record.getBoothId();
                    if (counts.containsKey(boothId)) {
                        counts.put(boothId, counts.get(boothId) + 1);
                    } else {
                        counts.put(boothId, 1L);
                    }
                }
            }
        }

        for (Map.Entry<String, Long> entry : counts.entrySet()) {
            if (entry.getValue() > threshold) {
                peakBooths.add(entry.getKey());
            }
        }
        return peakBooths;
    }

    public List<String> getPeakBooths() {
        return getPeakBooths(DEFAULT_PEAK_WINDOW_MINUTES, DEFAULT_PEAK_THRESHOLD);
    }

    public String exportBackup() {
        Map<String, Object> data = new HashMap<>();
        List<Booth> boothsCopy;
        List<CheckInRecord> recordsCopy;
        List<String> projectsCopy;
        synchronized (booths) { boothsCopy = new ArrayList<>(booths); }
        synchronized (records) { recordsCopy = new ArrayList<>(records); }
        synchronized (projects) { projectsCopy = new ArrayList<>(projects); }
        data.put("booths", boothsCopy);
        data.put("records", recordsCopy);
        data.put("projects", projectsCopy);
        data.put("exportAt", System.currentTimeMillis());
        data.put("stats", buildSummary());
        return gson.toJson(data);
    }

    private Map<String, Object> buildSummary() {
        Map<String, Object> s = new HashMap<>();
        s.put("boothCount", getBooths().size());
        s.put("projectCount", getProjects().size());
        s.put("totalRecords", getAllRecords().size());
        s.put("todayTotal", getTodayTotal());
        return s;
    }

    public void clearAllData() {
        synchronized (records) {
            records.clear();
        }
        saveData();
    }

    public String toJson() {
        Map<String, Object> data = new HashMap<>();
        data.put("booths", getBooths());
        data.put("allBooths", getAllBooths());
        data.put("records", getAllRecords());
        data.put("boothStats", getBoothStats());
        data.put("projectStats", getProjectStats());
        data.put("peakBooths", getPeakBooths());
        data.put("projects", getProjects());
        return gson.toJson(data);
    }
}
