package com.danmaku;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class DanmakuService {
    private static DanmakuService instance;
    private List<Message> pendingMessages;
    private List<Message> approvedMessages;
    private boolean sendingEnabled;
    private List<String> sensitiveWords;
    private static final String[] DEFAULT_COLORS = {
        "#ff6b6b", "#4ecdc4", "#ffe66d", "#95e1d3", "#f38181",
        "#aa96da", "#fcbad3", "#a8d8ea", "#ffd93d", "#6bcb77"
    };

    private DanmakuService() {
        FileStore.Settings settings = FileStore.loadSettings();
        this.sendingEnabled = settings.isSendingEnabled();
        this.sensitiveWords = settings.getSensitiveWords() != null ?
                new ArrayList<String>(settings.getSensitiveWords()) :
                new ArrayList<String>(Arrays.asList("违规", "广告", "赌博"));

        List<Message> allMessages = FileStore.loadMessages();
        this.pendingMessages = new CopyOnWriteArrayList<Message>();
        this.approvedMessages = new CopyOnWriteArrayList<Message>();

        if (allMessages != null) {
            for (Message msg : allMessages) {
                if ("pending".equals(msg.getStatus())) {
                    pendingMessages.add(msg);
                } else if ("approved".equals(msg.getStatus())) {
                    approvedMessages.add(msg);
                }
            }
        }

        startAutoSave();
    }

    public static synchronized DanmakuService getInstance() {
        if (instance == null) {
            instance = new DanmakuService();
        }
        return instance;
    }

    public synchronized Message addMessage(String content, String nickname) {
        if (!sendingEnabled) {
            return null;
        }

        boolean sensitive = containsSensitiveWord(content);
        String id = UUID.randomUUID().toString();
        String color = DEFAULT_COLORS[new Random().nextInt(DEFAULT_COLORS.length)];

        Message message = new Message(
                id,
                content,
                nickname != null ? nickname : "匿名",
                System.currentTimeMillis(),
                "pending",
                sensitive,
                color
        );

        pendingMessages.add(message);
        DanmakuWebSocket.broadcastToModerators(buildMessage("NEW_PENDING", message));
        return message;
    }

    public synchronized Message approveMessage(String id) {
        Message target = null;
        int index = -1;
        for (int i = 0; i < pendingMessages.size(); i++) {
            if (pendingMessages.get(i).getId().equals(id)) {
                target = pendingMessages.get(i);
                index = i;
                break;
            }
        }
        if (target != null && index >= 0) {
            pendingMessages.remove(index);
            target.setStatus("approved");
            approvedMessages.add(target);
            DanmakuWebSocket.broadcast(buildMessage("NEW_MESSAGE", target));
            DanmakuWebSocket.broadcastToModerators(buildMessage("PENDING_UPDATED", target));
            return target;
        }
        return null;
    }

    public synchronized Message rejectMessage(String id) {
        Message target = null;
        int index = -1;
        for (int i = 0; i < pendingMessages.size(); i++) {
            if (pendingMessages.get(i).getId().equals(id)) {
                target = pendingMessages.get(i);
                index = i;
                break;
            }
        }
        if (target != null && index >= 0) {
            pendingMessages.remove(index);
            target.setStatus("rejected");
            DanmakuWebSocket.broadcastToModerators(buildMessage("PENDING_UPDATED", target));
            return target;
        }
        return null;
    }

    public synchronized void clearScreen() {
        approvedMessages.clear();
        DanmakuWebSocket.broadcast(buildSimpleMessage("CLEAR_SCREEN"));
    }

    public synchronized void setSendingEnabled(boolean enabled) {
        this.sendingEnabled = enabled;
        DanmakuWebSocket.broadcast(buildSettingMessage("SETTING_UPDATED"));
    }

    public boolean isSendingEnabled() {
        return sendingEnabled;
    }

    public List<Message> getPendingMessages() {
        return new ArrayList<Message>(pendingMessages);
    }

    public List<Message> getApprovedMessages() {
        return new ArrayList<Message>(approvedMessages);
    }

    public List<String> getSensitiveWords() {
        return new ArrayList<String>(sensitiveWords);
    }

    private boolean containsSensitiveWord(String content) {
        if (content == null || content.isEmpty()) {
            return false;
        }
        for (String word : sensitiveWords) {
            if (content.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private void startAutoSave() {
        Timer timer = new Timer(true);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                saveToFile();
            }
        }, 10000, 30000);
    }

    private synchronized void saveToFile() {
        List<Message> allMessages = new ArrayList<Message>();
        allMessages.addAll(pendingMessages);
        allMessages.addAll(approvedMessages);
        FileStore.saveMessages(allMessages);
        FileStore.saveSettings(new FileStore.Settings(sendingEnabled, sensitiveWords));
    }

    private Map<String, Object> buildMessage(String type, Message msg) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("type", type);
        map.put("data", msg);
        return map;
    }

    private Map<String, Object> buildSimpleMessage(String type) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("type", type);
        return map;
    }

    private Map<String, Object> buildSettingMessage(String type) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("type", type);
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("sendingEnabled", sendingEnabled);
        map.put("data", data);
        return map;
    }
}
