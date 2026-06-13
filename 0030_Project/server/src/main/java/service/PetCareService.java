package service;

import model.CareRecord;
import model.Pet;
import model.PetStatus;
import model.ReminderConfig;
import model.StatusChange;
import store.JsonStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PetCareService {
    private static final long SAVE_INTERVAL_SECONDS = 30;
    private static final int RECENT_RECORDS_LIMIT = 10;
    private static final int DEFAULT_FEED_MINUTES = 360;
    private static final int DEFAULT_WALK_MINUTES = 360;
    private static final int DEFAULT_BATH_MINUTES = 720;

    private final JsonStore jsonStore;
    private final Map<String, Pet> petsMap;
    private final List<CareRecord> careRecords;
    private final List<StatusChange> statusChanges;
    private final Map<String, ReminderConfig> reminderConfigs;
    private final ScheduledExecutorService scheduler;

    public PetCareService(JsonStore jsonStore) {
        this.jsonStore = jsonStore;
        this.petsMap = new ConcurrentHashMap<String, Pet>();
        this.careRecords = Collections.synchronizedList(new ArrayList<CareRecord>());
        this.statusChanges = Collections.synchronizedList(new ArrayList<StatusChange>());
        this.reminderConfigs = new ConcurrentHashMap<String, ReminderConfig>();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();

        loadData();
        initDefaultReminders();
        startAutoSave();
    }

    private void loadData() {
        List<Pet> loadedPets = jsonStore.loadPets();
        for (Pet pet : loadedPets) {
            petsMap.put(pet.getId(), pet);
        }

        List<CareRecord> loadedRecords = jsonStore.loadCareRecords();
        careRecords.addAll(loadedRecords);

        List<StatusChange> loadedChanges = jsonStore.loadStatusChanges();
        statusChanges.addAll(loadedChanges);

        List<ReminderConfig> loadedReminders = jsonStore.loadReminderConfigs();
        for (ReminderConfig rc : loadedReminders) {
            reminderConfigs.put(rc.getAction(), rc);
        }
    }

    private void initDefaultReminders() {
        if (!reminderConfigs.containsKey("FEED")) {
            reminderConfigs.put("FEED", new ReminderConfig("FEED", DEFAULT_FEED_MINUTES, true));
        }
        if (!reminderConfigs.containsKey("WALK")) {
            reminderConfigs.put("WALK", new ReminderConfig("WALK", DEFAULT_WALK_MINUTES, true));
        }
        if (!reminderConfigs.containsKey("BATH")) {
            reminderConfigs.put("BATH", new ReminderConfig("BATH", DEFAULT_BATH_MINUTES, true));
        }
    }

    private void startAutoSave() {
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                saveAll();
            }
        }, SAVE_INTERVAL_SECONDS, SAVE_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    public synchronized void saveAll() {
        jsonStore.savePets(new ArrayList<Pet>(petsMap.values()));
        jsonStore.saveCareRecords(new ArrayList<CareRecord>(careRecords));
        jsonStore.saveStatusChanges(new ArrayList<StatusChange>(statusChanges));
        jsonStore.saveReminderConfigs(new ArrayList<ReminderConfig>(reminderConfigs.values()));
    }

    public synchronized Pet addPet(String name, String breed, String ownerPhoneLast4) {
        String id = UUID.randomUUID().toString();
        Date now = new Date();
        Pet pet = new Pet(id, name, breed, ownerPhoneLast4, PetStatus.NORMAL, now);
        petsMap.put(id, pet);
        return pet;
    }

    public synchronized Pet updatePetInfo(String petId, String name, String breed, String ownerPhoneLast4) {
        Pet pet = petsMap.get(petId);
        if (pet != null) {
            pet.setName(name);
            pet.setBreed(breed);
            pet.setOwnerPhoneLast4(ownerPhoneLast4);
            return pet;
        }
        return null;
    }

    public synchronized StatusChange updatePetStatus(String petId, PetStatus newStatus, String staffName) {
        Pet pet = petsMap.get(petId);
        if (pet == null) {
            return null;
        }
        PetStatus oldStatus = pet.getStatus();
        pet.setStatus(newStatus);
        String id = UUID.randomUUID().toString();
        Date now = new Date();
        StatusChange change = new StatusChange(id, petId, pet.getName(), oldStatus, newStatus, staffName, now);
        statusChanges.add(0, change);
        return change;
    }

    public Pet getPet(String petId) {
        return petsMap.get(petId);
    }

    public List<Pet> getAllPets() {
        List<Pet> result = new ArrayList<Pet>(petsMap.values());
        Collections.sort(result, new Comparator<Pet>() {
            @Override
            public int compare(Pet p1, Pet p2) {
                return p2.getCheckInTime().compareTo(p1.getCheckInTime());
            }
        });
        return result;
    }

    public synchronized CareRecord addCareRecord(String petId, String action, String note, String staffName) {
        Pet pet = petsMap.get(petId);
        if (pet == null) {
            return null;
        }
        String id = UUID.randomUUID().toString();
        Date now = new Date();
        CareRecord record = new CareRecord(id, petId, pet.getName(), action, note, staffName, now);
        careRecords.add(0, record);
        return record;
    }

    public synchronized boolean deleteCareRecord(String recordId) {
        synchronized (careRecords) {
            for (int i = 0; i < careRecords.size(); i++) {
                if (careRecords.get(i).getId().equals(recordId)) {
                    careRecords.remove(i);
                    return true;
                }
            }
        }
        return false;
    }

    public List<CareRecord> getRecentRecords() {
        int limit = Math.min(RECENT_RECORDS_LIMIT, careRecords.size());
        return new ArrayList<CareRecord>(careRecords.subList(0, limit));
    }

    public List<CareRecord> getAllRecordsToday() {
        Date todayStart = getTodayStart();
        List<CareRecord> result = new ArrayList<CareRecord>();
        synchronized (careRecords) {
            for (CareRecord record : careRecords) {
                if (record.getTime().after(todayStart) || record.getTime().equals(todayStart)) {
                    result.add(record);
                }
            }
        }
        return result;
    }

    public Map<String, Date> getLastCareTimeByPet() {
        Map<String, Date> result = new LinkedHashMap<String, Date>();
        synchronized (careRecords) {
            for (CareRecord record : careRecords) {
                String petId = record.getPetId();
                if (!result.containsKey(petId)) {
                    result.put(petId, record.getTime());
                }
            }
        }
        return result;
    }

    public Map<String, Map<String, Date>> getLastCareTimeByPetAndAction() {
        Map<String, Map<String, Date>> result = new LinkedHashMap<String, Map<String, Date>>();
        synchronized (careRecords) {
            for (CareRecord record : careRecords) {
                String petId = record.getPetId();
                String action = record.getAction();
                if (!result.containsKey(petId)) {
                    result.put(petId, new LinkedHashMap<String, Date>());
                }
                Map<String, Date> actionMap = result.get(petId);
                if (!actionMap.containsKey(action)) {
                    actionMap.put(action, record.getTime());
                }
            }
        }
        return result;
    }

    public List<CareRecord> getRecordsByPet(String petId) {
        List<CareRecord> result = new ArrayList<CareRecord>();
        synchronized (careRecords) {
            for (CareRecord record : careRecords) {
                if (record.getPetId().equals(petId)) {
                    result.add(record);
                }
            }
        }
        return result;
    }

    public List<StatusChange> getStatusChangesByPet(String petId) {
        List<StatusChange> result = new ArrayList<StatusChange>();
        synchronized (statusChanges) {
            for (StatusChange change : statusChanges) {
                if (change.getPetId().equals(petId)) {
                    result.add(change);
                }
            }
        }
        return result;
    }

    public Map<String, Object> getShiftSummary() {
        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        Date todayStart = getTodayStart();

        List<Pet> attentionPets = new ArrayList<Pet>();
        List<Pet> pickedUpPets = new ArrayList<Pet>();
        List<CareRecord> todayRecords = new ArrayList<CareRecord>();

        for (Pet pet : petsMap.values()) {
            if (pet.getStatus() == PetStatus.NEED_ATTENTION) {
                attentionPets.add(pet);
            }
            if (pet.getStatus() == PetStatus.PICKED_UP && pet.getCheckInTime().after(todayStart)) {
                pickedUpPets.add(pet);
            }
        }

        synchronized (careRecords) {
            for (CareRecord record : careRecords) {
                if (record.getTime().after(todayStart) || record.getTime().equals(todayStart)) {
                    todayRecords.add(record);
                }
            }
        }

        summary.put("attentionPets", attentionPets);
        summary.put("pickedUpPets", pickedUpPets);
        summary.put("todayRecordCount", todayRecords.size());
        summary.put("todayRecords", todayRecords.size() <= 20 ? todayRecords : todayRecords.subList(0, 20));
        summary.put("totalPetsInStore", petsMap.size() - pickedUpPets.size());

        return summary;
    }

    public void updateReminderConfig(String action, int intervalMinutes, boolean enabled) {
        reminderConfigs.put(action, new ReminderConfig(action, intervalMinutes, enabled));
    }

    public List<ReminderConfig> getReminderConfigs() {
        return new ArrayList<ReminderConfig>(reminderConfigs.values());
    }

    public List<String> getAttentionPetIds() {
        Map<String, Map<String, Date>> lastByAction = getLastCareTimeByPetAndAction();
        List<String> result = new ArrayList<String>();

        for (Pet pet : petsMap.values()) {
            if (pet.getStatus() == PetStatus.PICKED_UP) {
                continue;
            }
            boolean needsAttention = false;

            for (ReminderConfig config : reminderConfigs.values()) {
                if (!config.isEnabled()) {
                    continue;
                }
                Map<String, Date> actionMap = lastByAction.get(pet.getId());
                Date lastTime = actionMap != null ? actionMap.get(config.getAction()) : null;

                if (lastTime == null) {
                    long elapsed = System.currentTimeMillis() - pet.getCheckInTime().getTime();
                    if (elapsed > config.getIntervalMinutes() * 60 * 1000L) {
                        needsAttention = true;
                        break;
                    }
                } else {
                    long elapsed = System.currentTimeMillis() - lastTime.getTime();
                    if (elapsed > config.getIntervalMinutes() * 60 * 1000L) {
                        needsAttention = true;
                        break;
                    }
                }
            }

            if (needsAttention) {
                result.add(pet.getId());
            }
        }
        return result;
    }

    private Date getTodayStart() {
        Date now = new Date();
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTime(now);
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        saveAll();
    }
}
