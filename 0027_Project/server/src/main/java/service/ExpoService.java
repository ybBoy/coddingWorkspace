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

    public static final List<String> DEFAULT_PROJECTS = Collections.unmodifiableList(Arrays.asList(
            "智能对话", "机器视觉", "边缘计算", "智能穿戴",
            "AR/VR", "数字艺术", "互动游戏", "开源硬件"
    ));

    private static ExpoService instance;

    private final List<Booth> booths;
    private final List<CheckInRecord> records;
    private final JsonStore jsonStore;
    private final Gson gson;

    private ExpoService() {
        this.jsonStore = new JsonStore();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        JsonStore.StoreData data = jsonStore.load();
        this.booths = Collections.synchronizedList(data.booths);
        this.records = Collections.synchronizedList(data.records);
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
        synchronized (booths) {
            boothsCopy = new ArrayList<>(booths);
        }
        synchronized (records) {
            recordsCopy = new ArrayList<>(records);
        }
        jsonStore.save(new JsonStore.StoreData(boothsCopy, recordsCopy));
    }

    public List<Booth> getBooths() {
        synchronized (booths) {
            return new ArrayList<>(booths);
        }
    }

    public CheckInRecord checkIn(String boothId, Visitor visitor, List<String> interestedProjects) {
        if (boothId == null || visitor == null) {
            throw new IllegalArgumentException("参数不能为空");
        }

        boolean boothExists = false;
        synchronized (booths) {
            for (Booth booth : booths) {
                if (boothId.equals(booth.getId())) {
                    boothExists = true;
                    break;
                }
            }
        }
        if (!boothExists) {
            throw new IllegalArgumentException("展位不存在: " + boothId);
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
                stats.put(booth.getId(), 0L);
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
                    List<String> projects = record.getInterestedProjects();
                    if (projects != null) {
                        for (String project : projects) {
                            if (stats.containsKey(project)) {
                                stats.put(project, stats.get(project) + 1);
                            } else {
                                stats.put(project, 1L);
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
                stats.put(booth.getId(), 0L);
            }
        }
        synchronized (records) {
            for (CheckInRecord record : records) {
                String id = record.getBoothId();
                if (stats.containsKey(id)) {
                    stats.put(id, stats.get(id) + 1);
                } else {
                    stats.put(id, 1L);
                }
            }
        }
        return stats;
    }

    public Map<String, Long> getProjectStats() {
        Map<String, Long> stats = new HashMap<>();
        synchronized (records) {
            for (CheckInRecord record : records) {
                List<String> projects = record.getInterestedProjects();
                if (projects != null) {
                    for (String project : projects) {
                        if (stats.containsKey(project)) {
                            stats.put(project, stats.get(project) + 1);
                        } else {
                            stats.put(project, 1L);
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

    public String toJson() {
        Map<String, Object> data = new HashMap<>();
        data.put("booths", getBooths());
        data.put("records", getAllRecords());
        data.put("boothStats", getBoothStats());
        data.put("projectStats", getProjectStats());
        data.put("peakBooths", getPeakBooths());
        return gson.toJson(data);
    }
}
