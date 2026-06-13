package service;

import model.CareRecord;
import model.Pet;
import model.PetStatus;
import store.JsonStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
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

    private final JsonStore jsonStore;
    private final Map<String, Pet> petsMap;
    private final List<CareRecord> careRecords;
    private final ScheduledExecutorService scheduler;

    public PetCareService(JsonStore jsonStore) {
        this.jsonStore = jsonStore;
        this.petsMap = new ConcurrentHashMap<String, Pet>();
        this.careRecords = Collections.synchronizedList(new ArrayList<CareRecord>());
        this.scheduler = Executors.newSingleThreadScheduledExecutor();

        loadData();
        startAutoSave();
    }

    private void loadData() {
        List<Pet> loadedPets = jsonStore.loadPets();
        for (Pet pet : loadedPets) {
            petsMap.put(pet.getId(), pet);
        }

        List<CareRecord> loadedRecords = jsonStore.loadCareRecords();
        careRecords.addAll(loadedRecords);
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
    }

    public synchronized Pet addPet(String name, String breed, String ownerPhoneLast4) {
        String id = UUID.randomUUID().toString();
        Date now = new Date();
        Pet pet = new Pet(id, name, breed, ownerPhoneLast4, PetStatus.NORMAL, now);
        petsMap.put(id, pet);
        return pet;
    }

    public synchronized Pet updatePetStatus(String petId, PetStatus status) {
        Pet pet = petsMap.get(petId);
        if (pet != null) {
            pet.setStatus(status);
            return pet;
        }
        return null;
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

    public synchronized CareRecord addCareRecord(String petId, String action, String note) {
        Pet pet = petsMap.get(petId);
        if (pet == null) {
            return null;
        }
        String id = UUID.randomUUID().toString();
        Date now = new Date();
        CareRecord record = new CareRecord(id, petId, pet.getName(), action, note, now);
        careRecords.add(0, record);
        return record;
    }

    public List<CareRecord> getRecentRecords() {
        int limit = Math.min(RECENT_RECORDS_LIMIT, careRecords.size());
        return new ArrayList<CareRecord>(careRecords.subList(0, limit));
    }

    public Map<String, Date> getLastCareTimeByPet() {
        Map<String, Date> result = new ConcurrentHashMap<String, Date>();
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
