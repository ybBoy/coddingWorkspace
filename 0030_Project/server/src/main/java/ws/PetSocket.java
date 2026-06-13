package ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import model.CareRecord;
import model.Pet;
import model.PetStatus;
import model.StatusChange;
import service.PetCareService;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/ws")
public class PetSocket {
    private static final Set<Session> sessions = Collections.newSetFromMap(new ConcurrentHashMap<Session, Boolean>());
    private static PetCareService petCareService;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        objectMapper.setDateFormat(dateFormat);
        objectMapper.setTimeZone(TimeZone.getTimeZone("GMT+8"));
    }

    public static void setPetCareService(PetCareService service) {
        petCareService = service;
    }

    @OnOpen
    public void onOpen(Session session) {
        sessions.add(session);
        sendInitData(session);
    }

    @OnClose
    public void onClose(Session session) {
        sessions.remove(session);
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        try {
            ObjectNode msg = (ObjectNode) objectMapper.readTree(message);
            String type = msg.get("type").asText();

            if ("ADD_PET".equals(type)) {
                handleAddPet(msg);
            } else if ("UPDATE_STATUS".equals(type)) {
                handleUpdateStatus(msg);
            } else if ("ADD_CARE_RECORD".equals(type)) {
                handleAddCareRecord(msg);
            } else if ("UPDATE_PET".equals(type)) {
                handleUpdatePet(msg);
            } else if ("DELETE_CARE_RECORD".equals(type)) {
                handleDeleteCareRecord(msg);
            } else if ("SET_REMINDER_CONFIG".equals(type)) {
                handleSetReminderConfig(msg);
            } else if ("GET_PET_DETAIL".equals(type)) {
                handleGetPetDetail(msg, session);
            } else if ("GET_SHIFT_SUMMARY".equals(type)) {
                handleGetShiftSummary(session);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleAddPet(ObjectNode msg) {
        String name = msg.get("name").asText();
        String breed = msg.get("breed").asText();
        String ownerPhoneLast4 = msg.get("ownerPhoneLast4").asText();
        Pet pet = petCareService.addPet(name, breed, ownerPhoneLast4);
        if (pet != null) {
            broadcastPetAdded(pet);
            broadcastAttentionUpdate();
        }
    }

    private void handleUpdateStatus(ObjectNode msg) {
        String petId = msg.get("petId").asText();
        String statusStr = msg.get("status").asText();
        PetStatus status = PetStatus.valueOf(statusStr);
        String staffName = msg.has("staffName") ? msg.get("staffName").asText() : "";
        StatusChange change = petCareService.updatePetStatus(petId, status, staffName);
        if (change != null) {
            Pet pet = petCareService.getPet(petId);
            broadcastStatusUpdated(pet, change);
            broadcastAttentionUpdate();
        }
    }

    private void handleAddCareRecord(ObjectNode msg) {
        String petId = msg.get("petId").asText();
        String action = msg.get("action").asText();
        String note = msg.has("note") && !msg.get("note").isNull() ? msg.get("note").asText() : "";
        String staffName = msg.has("staffName") ? msg.get("staffName").asText() : "";
        CareRecord record = petCareService.addCareRecord(petId, action, note, staffName);
        if (record != null) {
            broadcastCareRecordAdded(record);
            broadcastAttentionUpdate();
        }
    }

    private void handleUpdatePet(ObjectNode msg) {
        String petId = msg.get("petId").asText();
        String name = msg.get("name").asText();
        String breed = msg.get("breed").asText();
        String ownerPhoneLast4 = msg.get("ownerPhoneLast4").asText();
        Pet pet = petCareService.updatePetInfo(petId, name, breed, ownerPhoneLast4);
        if (pet != null) {
            broadcastPetUpdated(pet);
        }
    }

    private void handleDeleteCareRecord(ObjectNode msg) {
        String recordId = msg.get("recordId").asText();
        boolean deleted = petCareService.deleteCareRecord(recordId);
        if (deleted) {
            broadcastCareRecordDeleted(recordId);
            broadcastAttentionUpdate();
        }
    }

    private void handleSetReminderConfig(ObjectNode msg) {
        String action = msg.get("action").asText();
        int intervalMinutes = msg.get("intervalMinutes").asInt();
        boolean enabled = msg.get("enabled").asBoolean();
        petCareService.updateReminderConfig(action, intervalMinutes, enabled);
        broadcastReminderConfigUpdated();
        broadcastAttentionUpdate();
    }

    private void handleGetPetDetail(ObjectNode msg, Session session) {
        String petId = msg.get("petId").asText();
        Pet pet = petCareService.getPet(petId);
        if (pet == null) return;
        List<CareRecord> records = petCareService.getRecordsByPet(petId);
        List<StatusChange> changes = petCareService.getStatusChangesByPet(petId);
        try {
            ObjectNode detailMsg = objectMapper.createObjectNode();
            detailMsg.put("type", "PET_DETAIL");
            detailMsg.set("pet", objectMapper.valueToTree(pet));
            detailMsg.set("careRecords", objectMapper.valueToTree(records));
            detailMsg.set("statusChanges", objectMapper.valueToTree(changes));
            session.getBasicRemote().sendText(objectMapper.writeValueAsString(detailMsg));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleGetShiftSummary(Session session) {
        Map<String, Object> summary = petCareService.getShiftSummary();
        try {
            ObjectNode summaryMsg = objectMapper.createObjectNode();
            summaryMsg.put("type", "SHIFT_SUMMARY");
            summaryMsg.set("summary", objectMapper.valueToTree(summary));
            session.getBasicRemote().sendText(objectMapper.writeValueAsString(summaryMsg));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendInitData(Session session) {
        try {
            ObjectNode initMsg = objectMapper.createObjectNode();
            initMsg.put("type", "INIT");
            initMsg.set("pets", objectMapper.valueToTree(petCareService.getAllPets()));
            initMsg.set("recentRecords", objectMapper.valueToTree(petCareService.getRecentRecords()));
            initMsg.set("lastCareTimeByPet", objectMapper.valueToTree(petCareService.getLastCareTimeByPet()));
            initMsg.set("lastCareTimeByPetAndAction", objectMapper.valueToTree(petCareService.getLastCareTimeByPetAndAction()));
            initMsg.set("attentionPetIds", objectMapper.valueToTree(petCareService.getAttentionPetIds()));
            initMsg.set("reminderConfigs", objectMapper.valueToTree(petCareService.getReminderConfigs()));
            session.getBasicRemote().sendText(objectMapper.writeValueAsString(initMsg));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void broadcastPetAdded(Pet pet) {
        try {
            ObjectNode msg = objectMapper.createObjectNode();
            msg.put("type", "PET_ADDED");
            msg.set("pet", objectMapper.valueToTree(pet));
            broadcast(objectMapper.writeValueAsString(msg));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void broadcastStatusUpdated(Pet pet, StatusChange change) {
        try {
            ObjectNode msg = objectMapper.createObjectNode();
            msg.put("type", "STATUS_UPDATED");
            msg.set("pet", objectMapper.valueToTree(pet));
            msg.set("statusChange", objectMapper.valueToTree(change));
            broadcast(objectMapper.writeValueAsString(msg));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void broadcastCareRecordAdded(CareRecord record) {
        try {
            ObjectNode msg = objectMapper.createObjectNode();
            msg.put("type", "CARE_RECORD_ADDED");
            msg.set("record", objectMapper.valueToTree(record));
            broadcast(objectMapper.writeValueAsString(msg));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void broadcastPetUpdated(Pet pet) {
        try {
            ObjectNode msg = objectMapper.createObjectNode();
            msg.put("type", "PET_UPDATED");
            msg.set("pet", objectMapper.valueToTree(pet));
            broadcast(objectMapper.writeValueAsString(msg));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void broadcastCareRecordDeleted(String recordId) {
        try {
            ObjectNode msg = objectMapper.createObjectNode();
            msg.put("type", "CARE_RECORD_DELETED");
            msg.put("recordId", recordId);
            broadcast(objectMapper.writeValueAsString(msg));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void broadcastReminderConfigUpdated() {
        try {
            ObjectNode msg = objectMapper.createObjectNode();
            msg.put("type", "REMINDER_CONFIG_UPDATED");
            msg.set("reminderConfigs", objectMapper.valueToTree(petCareService.getReminderConfigs()));
            broadcast(objectMapper.writeValueAsString(msg));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void broadcastAttentionUpdate() {
        try {
            ObjectNode msg = objectMapper.createObjectNode();
            msg.put("type", "ATTENTION_UPDATE");
            msg.set("attentionPetIds", objectMapper.valueToTree(petCareService.getAttentionPetIds()));
            msg.set("lastCareTimeByPet", objectMapper.valueToTree(petCareService.getLastCareTimeByPet()));
            msg.set("lastCareTimeByPetAndAction", objectMapper.valueToTree(petCareService.getLastCareTimeByPetAndAction()));
            broadcast(objectMapper.writeValueAsString(msg));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void broadcast(String message) {
        for (Session session : sessions) {
            if (session.isOpen()) {
                try {
                    session.getBasicRemote().sendText(message);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
