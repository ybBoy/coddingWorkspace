package ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import model.CareRecord;
import model.Pet;
import model.PetStatus;
import service.PetCareService;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
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
        }
    }

    private void handleUpdateStatus(ObjectNode msg) {
        String petId = msg.get("petId").asText();
        String statusStr = msg.get("status").asText();
        PetStatus status = PetStatus.valueOf(statusStr);

        Pet pet = petCareService.updatePetStatus(petId, status);
        if (pet != null) {
            broadcastStatusUpdated(pet);
        }
    }

    private void handleAddCareRecord(ObjectNode msg) {
        String petId = msg.get("petId").asText();
        String action = msg.get("action").asText();
        String note = msg.has("note") && !msg.get("note").isNull() ? msg.get("note").asText() : "";

        CareRecord record = petCareService.addCareRecord(petId, action, note);
        if (record != null) {
            broadcastCareRecordAdded(record);
        }
    }

    private void sendInitData(Session session) {
        try {
            ObjectNode initMsg = objectMapper.createObjectNode();
            initMsg.put("type", "INIT");
            initMsg.set("pets", objectMapper.valueToTree(petCareService.getAllPets()));
            initMsg.set("recentRecords", objectMapper.valueToTree(petCareService.getRecentRecords()));
            initMsg.set("lastCareTimeByPet", objectMapper.valueToTree(petCareService.getLastCareTimeByPet()));
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void broadcastStatusUpdated(Pet pet) {
        try {
            ObjectNode msg = objectMapper.createObjectNode();
            msg.put("type", "STATUS_UPDATED");
            msg.set("pet", objectMapper.valueToTree(pet));
            broadcast(objectMapper.writeValueAsString(msg));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void broadcastCareRecordAdded(CareRecord record) {
        try {
            ObjectNode msg = objectMapper.createObjectNode();
            msg.put("type", "CARE_RECORD_ADDED");
            msg.set("record", objectMapper.valueToTree(record));
            broadcast(objectMapper.writeValueAsString(msg));
        } catch (Exception e) {
            e.printStackTrace();
        }
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
