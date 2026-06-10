package com.interview.evaluation.websocket;

import com.alibaba.fastjson.JSON;
import com.interview.evaluation.dto.WsMessage;
import com.interview.evaluation.enums.Role;
import com.interview.evaluation.model.EvaluationForm;
import com.interview.evaluation.model.EvaluationVersion;
import com.interview.evaluation.service.EvaluationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.Resource;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class EvaluationWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(EvaluationWebSocketHandler.class);

    @Resource
    private EvaluationService evaluationService;

    private final Map<String, Map<String, WebSocketSession>> formSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Map<String, Object> attrs = session.getAttributes();
        String formId = (String) attrs.getOrDefault("formId", "default");
        String userName = URLDecoder.decode((String) attrs.getOrDefault("userName", "anonymous"), StandardCharsets.UTF_8.name());
        String roleStr = (String) attrs.getOrDefault("role", "CANDIDATE");

        session.getAttributes().put("formId", formId);
        session.getAttributes().put("userName", userName);
        session.getAttributes().put("role", Role.valueOf(roleStr));

        formSessions.computeIfAbsent(formId, k -> new ConcurrentHashMap<>()).put(session.getId(), session);

        EvaluationForm form = evaluationService.getOrCreateForm(formId);
        WsMessage initMsg = new WsMessage();
        initMsg.setType("INIT");
        initMsg.setFormId(formId);
        initMsg.setScores(form.getScores());
        initMsg.setTimestamp(form.getUpdatedAt());
        sendMessage(session, initMsg);

        broadcastUserList(formId);
        log.info("WebSocket connected: user={}, formId={}, role={}", userName, formId, roleStr);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            WsMessage msg = JSON.parseObject(message.getPayload(), WsMessage.class);
            String formId = (String) session.getAttributes().get("formId");
            String userName = (String) session.getAttributes().get("userName");
            Role role = (Role) session.getAttributes().get("role");

            if (role != Role.INTERVIEWER && !"GET_VERSIONS".equals(msg.getType())) {
                WsMessage err = new WsMessage();
                err.setType("ERROR");
                err.setTimestamp(System.currentTimeMillis());
                sendMessage(session, err);
                return;
            }

            switch (msg.getType()) {
                case "SCORE_UPDATE":
                    if (msg.getScore() != null) {
                        EvaluationForm updated = evaluationService.updateScore(formId, msg.getScore(), userName);
                        WsMessage broadcast = new WsMessage();
                        broadcast.setType("SCORE_UPDATE");
                        broadcast.setFormId(formId);
                        broadcast.setUserId(session.getId());
                        broadcast.setUserName(userName);
                        broadcast.setScore(msg.getScore());
                        broadcast.setTimestamp(updated.getUpdatedAt());
                        broadcastToForm(formId, broadcast, session.getId());
                    }
                    break;
                case "COMMIT_VERSION":
                    EvaluationVersion version = evaluationService.saveVersion(formId, userName);
                    WsVersionMsg versionMsg = new WsVersionMsg();
                    versionMsg.setType("VERSION_SAVED");
                    versionMsg.setFormId(formId);
                    versionMsg.setUserName(userName);
                    versionMsg.setVersion(version);
                    versionMsg.setTimestamp(System.currentTimeMillis());
                    broadcastToForm(formId, versionMsg, null);
                    broadcastVersions(formId);
                    break;
                case "GET_VERSIONS":
                    List<EvaluationVersion> versions = evaluationService.getVersions(formId);
                    WsVersionsMsg vMsg = new WsVersionsMsg();
                    vMsg.setType("VERSIONS_LIST");
                    vMsg.setFormId(formId);
                    vMsg.setVersions(versions);
                    vMsg.setTimestamp(System.currentTimeMillis());
                    sendMessage(session, vMsg);
                    break;
                case "ROLLBACK":
                    if (msg.getVersionId() != null) {
                        EvaluationForm rolled = evaluationService.rollbackToVersion(formId, msg.getVersionId(), userName);
                        WsMessage rbMsg = new WsMessage();
                        rbMsg.setType("ROLLBACK");
                        rbMsg.setFormId(formId);
                        rbMsg.setUserId(session.getId());
                        rbMsg.setUserName(userName);
                        rbMsg.setScores(rolled.getScores());
                        rbMsg.setVersionId(msg.getVersionId());
                        rbMsg.setTimestamp(rolled.getUpdatedAt());
                        broadcastToForm(formId, rbMsg, null);
                    }
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            log.error("Error handling WebSocket message", e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String formId = (String) session.getAttributes().get("formId");
        String userName = (String) session.getAttributes().get("userName");
        Map<String, WebSocketSession> sessions = formSessions.get(formId);
        if (sessions != null) {
            sessions.remove(session.getId());
            if (sessions.isEmpty()) {
                formSessions.remove(formId);
            }
        }
        broadcastUserList(formId);
        log.info("WebSocket disconnected: user={}, formId={}", userName, formId);
    }

    private void broadcastToForm(String formId, Object msg, String excludeSessionId) {
        Map<String, WebSocketSession> sessions = formSessions.get(formId);
        if (sessions == null) return;
        String json = JSON.toJSONString(msg);
        TextMessage textMsg = new TextMessage(json);
        for (Map.Entry<String, WebSocketSession> entry : sessions.entrySet()) {
            if (excludeSessionId != null && excludeSessionId.equals(entry.getKey())) {
                continue;
            }
            WebSocketSession s = entry.getValue();
            if (s.isOpen()) {
                try {
                    s.sendMessage(textMsg);
                } catch (IOException e) {
                    log.error("Failed to send message", e);
                }
            }
        }
    }

    private void broadcastUserList(String formId) {
        Map<String, WebSocketSession> sessions = formSessions.get(formId);
        if (sessions == null) return;
        WsUsersMsg usersMsg = new WsUsersMsg();
        usersMsg.setType("USERS_UPDATE");
        usersMsg.setFormId(formId);
        usersMsg.setTimestamp(System.currentTimeMillis());
        Map<String, String> users = new ConcurrentHashMap<>();
        for (Map.Entry<String, WebSocketSession> entry : sessions.entrySet()) {
            String name = (String) entry.getValue().getAttributes().get("userName");
            Role role = (Role) entry.getValue().getAttributes().get("role");
            users.put(entry.getKey(), name + "(" + (role == Role.INTERVIEWER ? "面试官" : "候选人") + ")");
        }
        usersMsg.setUsers(users);
        broadcastToForm(formId, usersMsg, null);
    }

    private void broadcastVersions(String formId) {
        Map<String, WebSocketSession> sessions = formSessions.get(formId);
        if (sessions == null) return;
        List<EvaluationVersion> versions = evaluationService.getVersions(formId);
        WsVersionsMsg vMsg = new WsVersionsMsg();
        vMsg.setType("VERSIONS_LIST");
        vMsg.setFormId(formId);
        vMsg.setVersions(versions);
        vMsg.setTimestamp(System.currentTimeMillis());
        broadcastToForm(formId, vMsg, null);
    }

    private void sendMessage(WebSocketSession session, Object msg) {
        if (!session.isOpen()) return;
        try {
            session.sendMessage(new TextMessage(JSON.toJSONString(msg)));
        } catch (IOException e) {
            log.error("Failed to send message", e);
        }
    }

    public static class WsVersionMsg {
        private String type;
        private String formId;
        private String userName;
        private EvaluationVersion version;
        private Long timestamp;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getFormId() { return formId; }
        public void setFormId(String formId) { this.formId = formId; }
        public String getUserName() { return userName; }
        public void setUserName(String userName) { this.userName = userName; }
        public EvaluationVersion getVersion() { return version; }
        public void setVersion(EvaluationVersion version) { this.version = version; }
        public Long getTimestamp() { return timestamp; }
        public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
    }

    public static class WsVersionsMsg {
        private String type;
        private String formId;
        private List<EvaluationVersion> versions;
        private Long timestamp;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getFormId() { return formId; }
        public void setFormId(String formId) { this.formId = formId; }
        public List<EvaluationVersion> getVersions() { return versions; }
        public void setVersions(List<EvaluationVersion> versions) { this.versions = versions; }
        public Long getTimestamp() { return timestamp; }
        public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
    }

    public static class WsUsersMsg {
        private String type;
        private String formId;
        private Map<String, String> users;
        private Long timestamp;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getFormId() { return formId; }
        public void setFormId(String formId) { this.formId = formId; }
        public Map<String, String> getUsers() { return users; }
        public void setUsers(Map<String, String> users) { this.users = users; }
        public Long getTimestamp() { return timestamp; }
        public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
    }
}
