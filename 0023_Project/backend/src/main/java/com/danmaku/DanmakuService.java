package com.danmaku;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class DanmakuService {
    private static DanmakuService instance;
    private List<Message> pendingMessages;
    private List<Message> approvedMessages;
    private List<Message> rejectedMessages;
    private List<OperationLog> operationLogs;
    private FileStore.Settings settings;
    private boolean playbackPaused = false;
    private static final int MAX_RECENT = 30;
    private static final int MAX_LOGS = 500;
    private static final long RATE_LIMIT_MS = 3000;
    private static final int MAX_SAME_CONTENT = 3;
    private Map<String, Long> lastSendTime = new HashMap<String, Long>();
    private Map<String, Integer> contentCount = new HashMap<String, Integer>();

    private static final Map<String, String[]> COLOR_THEMES = new HashMap<String, String[]>();
    static {
        COLOR_THEMES.put("rainbow", new String[]{
            "#ff6b6b", "#4ecdc4", "#ffe66d", "#95e1d3", "#f38181",
            "#aa96da", "#fcbad3", "#a8d8ea", "#ffd93d", "#6bcb77"
        });
        COLOR_THEMES.put("neon", new String[]{
            "#00ff87", "#ff00ff", "#00ffff", "#ffff00", "#ff3366",
            "#66ff33", "#ff6600", "#3366ff", "#ff0066", "#00ffcc"
        });
        COLOR_THEMES.put("warm", new String[]{
            "#ff6b6b", "#ffa07a", "#ffb347", "#ff6348", "#ff4757",
            "#ff7979", "#f8a5c2", "#f78fb3", "#e77f67", "#cf6a87"
        });
        COLOR_THEMES.put("cool", new String[]{
            "#74b9ff", "#81ecec", "#a29bfe", "#55efc4", "#00cec9",
            "#6c5ce7", "#0984e3", "#00b894", "#48dbfb", "#0abde3"
        });
    }

    private DanmakuService() {
        this.settings = FileStore.loadSettings();
        this.pendingMessages = new CopyOnWriteArrayList<Message>();
        this.approvedMessages = new CopyOnWriteArrayList<Message>();
        this.rejectedMessages = new CopyOnWriteArrayList<Message>();
        this.operationLogs = new CopyOnWriteArrayList<OperationLog>();

        List<Message> allMessages = FileStore.loadMessages();
        if (allMessages != null) {
            for (Message msg : allMessages) {
                if ("pending".equals(msg.getStatus())) pendingMessages.add(msg);
                else if ("approved".equals(msg.getStatus())) approvedMessages.add(msg);
                else if ("rejected".equals(msg.getStatus())) rejectedMessages.add(msg);
            }
        }
        List<OperationLog> logs = FileStore.loadLogs();
        if (logs != null) operationLogs.addAll(logs);

        startAutoSave();
    }

    public static synchronized DanmakuService getInstance() {
        if (instance == null) instance = new DanmakuService();
        return instance;
    }

    public synchronized String checkRateLimit(String nickname) {
        Long last = lastSendTime.get(nickname);
        if (last != null && System.currentTimeMillis() - last < RATE_LIMIT_MS) {
            long wait = RATE_LIMIT_MS - (System.currentTimeMillis() - last);
            return "Rate limited, wait " + (wait / 1000 + 1) + "s";
        }
        return null;
    }

    public synchronized String checkDuplicate(String content, String nickname) {
        String key = nickname + ":" + content.trim().toLowerCase();
        Integer count = contentCount.get(key);
        if (count != null && count >= MAX_SAME_CONTENT) {
            return "Duplicate content blocked";
        }
        return null;
    }

    public synchronized Message addMessage(String content, String nickname) {
        if (!settings.isSendingEnabled()) return null;

        String rateErr = checkRateLimit(nickname);
        if (rateErr != null) {
            Message err = new Message("", rateErr, "", System.currentTimeMillis(), "error", false, "", false);
            return err;
        }

        String dupErr = checkDuplicate(content, nickname);
        if (dupErr != null) {
            Message err = new Message("", dupErr, "", System.currentTimeMillis(), "error", false, "", false);
            return err;
        }

        lastSendTime.put(nickname, System.currentTimeMillis());
        String key = nickname + ":" + content.trim().toLowerCase();
        contentCount.put(key, contentCount.containsKey(key) ? contentCount.get(key) + 1 : 1);

        boolean sensitive = containsSensitiveWord(content);
        String id = UUID.randomUUID().toString();
        String color = getRandomColor();

        Message message = new Message(id, content, nickname != null ? nickname : "anonymous",
                System.currentTimeMillis(), "pending", sensitive, color, false);

        pendingMessages.add(message);
        DanmakuWebSocket.broadcastToModerators(buildMessage("NEW_PENDING", message));
        addLog("NEW_MESSAGE", "moderator", "New pending from " + nickname);
        return message;
    }

    private String getRandomColor() {
        String theme = settings.getColorTheme();
        if ("custom".equals(theme) && settings.getCustomColors() != null
                && !settings.getCustomColors().isEmpty()) {
            List<String> colors = settings.getCustomColors();
            return colors.get(new Random().nextInt(colors.size()));
        }
        String[] palette = COLOR_THEMES.containsKey(theme) ? COLOR_THEMES.get(theme) : COLOR_THEMES.get("rainbow");
        return palette[new Random().nextInt(palette.length)];
    }

    public synchronized Message approveMessage(String id) {
        Message target = findAndRemovePending(id);
        if (target != null) {
            target.setStatus("approved");
            approvedMessages.add(target);
            DanmakuWebSocket.broadcast(buildMessage("NEW_MESSAGE", target));
            DanmakuWebSocket.broadcastToModerators(buildMessage("PENDING_UPDATED", target));
            addLog("APPROVE", "moderator", "Approved: " + target.getContent().substring(0, Math.min(20, target.getContent().length())));
            return target;
        }
        return null;
    }

    public synchronized Message rejectMessage(String id) {
        Message target = findAndRemovePending(id);
        if (target != null) {
            target.setStatus("rejected");
            rejectedMessages.add(target);
            DanmakuWebSocket.broadcastToModerators(buildMessage("PENDING_UPDATED", target));
            addLog("REJECT", "moderator", "Rejected: " + target.getContent().substring(0, Math.min(20, target.getContent().length())));
            return target;
        }
        return null;
    }

    public synchronized void approveNormalOnly() {
        List<Message> toApprove = new ArrayList<Message>();
        for (Message msg : pendingMessages) {
            if (!msg.isSensitive()) toApprove.add(msg);
        }
        for (Message msg : toApprove) {
            approveMessage(msg.getId());
        }
        addLog("BATCH_APPROVE_NORMAL", "moderator", "Approved " + toApprove.size() + " normal messages");
    }

    public synchronized void togglePinMessage(String id) {
        for (Message msg : approvedMessages) {
            if (msg.getId().equals(id)) {
                msg.setPinned(!msg.isPinned());
                DanmakuWebSocket.broadcast(buildMessage("PIN_UPDATED", msg));
                addLog("TOGGLE_PIN", "moderator", (msg.isPinned() ? "Pinned" : "Unpinned") + ": " + msg.getContent().substring(0, Math.min(20, msg.getContent().length())));
                return;
            }
        }
    }

    public synchronized void clearScreen() {
        approvedMessages.clear();
        DanmakuWebSocket.broadcast(buildSimpleMessage("CLEAR_SCREEN"));
        addLog("CLEAR_SCREEN", "moderator", "Screen cleared");
    }

    public synchronized void setSendingEnabled(boolean enabled) {
        settings.setSendingEnabled(enabled);
        DanmakuWebSocket.broadcast(buildSettingMessage("SETTING_UPDATED"));
        addLog("TOGGLE_SENDING", "moderator", "Sending " + (enabled ? "enabled" : "disabled"));
    }

    public synchronized void setPlaybackPaused(boolean paused) {
        this.playbackPaused = paused;
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("playbackPaused", paused);
        DanmakuWebSocket.broadcast(buildDataMessage("PLAYBACK_STATE", data));
        addLog("TOGGLE_PLAYBACK", "moderator", "Playback " + (paused ? "paused" : "resumed"));
    }

    public synchronized void updateSettings(FileStore.Settings newSettings) {
        this.settings = newSettings;
        DanmakuWebSocket.broadcast(buildSettingMessage("SETTING_UPDATED"));
        addLog("UPDATE_SETTINGS", "moderator", "Settings updated");
    }

    public boolean isPlaybackPaused() { return playbackPaused; }

    public FileStore.Settings getSettings() { return settings; }

    public boolean isSendingEnabled() { return settings.isSendingEnabled(); }

    public List<Message> getPendingMessages() { return new ArrayList<Message>(pendingMessages); }

    public List<Message> getApprovedMessages() { return new ArrayList<Message>(approvedMessages); }

    public List<Message> getPinnedMessages() {
        List<Message> pinned = new ArrayList<Message>();
        for (Message msg : approvedMessages) {
            if (msg.isPinned()) pinned.add(msg);
        }
        return pinned;
    }

    public List<Message> getRecentApproved() {
        int size = approvedMessages.size();
        if (size <= MAX_RECENT) return new ArrayList<Message>(approvedMessages);
        return new ArrayList<Message>(approvedMessages.subList(size - MAX_RECENT, size));
    }

    public List<OperationLog> getRecentLogs() {
        int size = operationLogs.size();
        if (size <= 100) return new ArrayList<OperationLog>(operationLogs);
        return new ArrayList<OperationLog>(operationLogs.subList(size - 100, size));
    }

    private Message findAndRemovePending(String id) {
        for (int i = 0; i < pendingMessages.size(); i++) {
            if (pendingMessages.get(i).getId().equals(id)) {
                return pendingMessages.remove(i);
            }
        }
        return null;
    }

    private boolean containsSensitiveWord(String content) {
        if (content == null || content.isEmpty()) return false;
        for (String word : settings.getSensitiveWords()) {
            if (content.contains(word)) return true;
        }
        return false;
    }

    private void addLog(String action, String operator, String detail) {
        operationLogs.add(new OperationLog(System.currentTimeMillis(), action, operator, detail));
        if (operationLogs.size() > MAX_LOGS) {
            operationLogs = new CopyOnWriteArrayList<OperationLog>(
                    operationLogs.subList(operationLogs.size() - MAX_LOGS, operationLogs.size()));
        }
    }

    private void startAutoSave() {
        Timer timer = new Timer(true);
        timer.schedule(new TimerTask() {
            @Override
            public void run() { saveToFile(); }
        }, 10000, 30000);
    }

    private synchronized void saveToFile() {
        List<Message> allMessages = new ArrayList<Message>();
        allMessages.addAll(pendingMessages);
        allMessages.addAll(approvedMessages);
        allMessages.addAll(rejectedMessages);
        FileStore.saveMessages(allMessages);
        FileStore.saveSettings(settings);
        FileStore.saveLogs(operationLogs);
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
        map.put("data", settingsToMap());
        return map;
    }

    private Map<String, Object> buildDataMessage(String type, Map<String, Object> data) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("type", type);
        map.put("data", data);
        return map;
    }

    public synchronized Message approveAndPinMessage(String id) {
        Message target = approveMessage(id);
        if (target != null) {
            target.setPinned(true);
            DanmakuWebSocket.broadcast(buildMessage("PIN_UPDATED", target));
        }
        return target;
    }

    public Map<String, Object> settingsToMap() {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("sendingEnabled", settings.isSendingEnabled());
        m.put("playbackPaused", playbackPaused);
        m.put("eventTitle", settings.getEventTitle());
        m.put("welcomeMessage", settings.getWelcomeMessage());
        m.put("colorTheme", settings.getColorTheme());
        m.put("customColors", settings.getCustomColors());
        m.put("sensitiveWords", settings.getSensitiveWords());
        m.put("speedMin", settings.getSpeedMin());
        m.put("speedMax", settings.getSpeedMax());
        m.put("fontSize", settings.getFontSize());
        m.put("trackCount", settings.getTrackCount());
        return m;
    }
}
