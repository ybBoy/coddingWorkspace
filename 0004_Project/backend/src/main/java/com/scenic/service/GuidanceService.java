package com.scenic.service;

import com.scenic.entity.CrowdSuggestion;
import com.scenic.entity.GuidanceMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class GuidanceService {
    private static final Logger logger = LoggerFactory.getLogger(GuidanceService.class);

    @Autowired
    private DataStoreService dataStoreService;

    @Autowired
    private SuggestionService suggestionService;

    @Autowired
    private WebSocketService webSocketService;

    private boolean autoPublishEnabled = true;

    public boolean isAutoPublishEnabled() {
        return autoPublishEnabled;
    }

    public void setAutoPublishEnabled(boolean enabled) {
        this.autoPublishEnabled = enabled;
        logger.info("自动发布引导信息功能已{}", enabled ? "启用" : "禁用");
    }

    @Scheduled(fixedRate = 10000)
    public void autoPublishGuidance() {
        if (!autoPublishEnabled) {
            return;
        }

        List<CrowdSuggestion> activeSuggestions = suggestionService.getActiveSuggestions();
        if (activeSuggestions.isEmpty()) {
            return;
        }

        List<GuidanceMessage> activeMessages = dataStoreService.getActiveGuidanceMessages();
        boolean hasRecentMessage = false;
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);

        for (GuidanceMessage msg : activeMessages) {
            if (msg.getCreateTime() != null && msg.getCreateTime().isAfter(fiveMinutesAgo)) {
                hasRecentMessage = true;
                break;
            }
        }

        if (!hasRecentMessage) {
            for (CrowdSuggestion suggestion : activeSuggestions) {
                if (suggestion.getPriority() <= 2) {
                    publishGuidanceFromSuggestion(suggestion);
                    break;
                }
            }
        }
    }

    private void publishGuidanceFromSuggestion(CrowdSuggestion suggestion) {
        GuidanceMessage message = new GuidanceMessage();
        message.setId(dataStoreService.getNextId());
        message.setTitle("游客引导提示");
        message.setContent(String.format("亲爱的游客朋友，%s区域当前游客较多，建议您先前往%s区域游览，体验更佳！",
                suggestion.getSourceAreaName(), suggestion.getTargetAreaName()));
        message.setType(GuidanceMessage.MessageType.GUIDANCE_SUGGESTION);
        message.setSource(GuidanceMessage.MessageSource.AUTO);
        message.setTargetDisplay("入口大屏");
        message.setStartTime(LocalDateTime.now());
        message.setEndTime(LocalDateTime.now().plusMinutes(10));

        dataStoreService.addGuidanceMessage(message);
        webSocketService.broadcastGuidanceMessage(message);
        
        logger.info("自动发布引导信息: {}", message.getContent());
    }

    public GuidanceMessage publishManualGuidance(String title, String content, 
                                                   GuidanceMessage.MessageType type, String targetDisplay,
                                                   int durationMinutes) {
        GuidanceMessage message = new GuidanceMessage();
        message.setId(dataStoreService.getNextId());
        message.setTitle(title);
        message.setContent(content);
        message.setType(type);
        message.setSource(GuidanceMessage.MessageSource.MANUAL);
        message.setTargetDisplay(targetDisplay);
        message.setStartTime(LocalDateTime.now());
        message.setEndTime(LocalDateTime.now().plusMinutes(durationMinutes));

        dataStoreService.addGuidanceMessage(message);
        webSocketService.broadcastGuidanceMessage(message);
        
        logger.info("手动发布引导信息: {}", content);
        return message;
    }

    public List<GuidanceMessage> getAllGuidanceMessages() {
        return dataStoreService.getAllGuidanceMessages();
    }

    public List<GuidanceMessage> getActiveGuidanceMessages() {
        return dataStoreService.getActiveGuidanceMessages();
    }

    public boolean deactivateGuidanceMessage(Long id) {
        GuidanceMessage message = dataStoreService.getGuidanceMessageById(id);
        if (message != null) {
            message.setActive(false);
            logger.info("停用引导信息: {}", message.getTitle());
            return true;
        }
        return false;
    }
}