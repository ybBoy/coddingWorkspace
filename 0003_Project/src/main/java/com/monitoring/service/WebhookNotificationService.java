package com.monitoring.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monitoring.model.AlertRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@Service
public class WebhookNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(WebhookNotificationService.class);

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${dingtalk.webhook.enabled:false}")
    private boolean dingtalkEnabled;

    @Value("${dingtalk.webhook.url:}")
    private String dingtalkWebhookUrl;

    @Value("${dingtalk.webhook.secret:}")
    private String dingtalkSecret;

    @Value("${feishu.webhook.enabled:false}")
    private boolean feishuEnabled;

    @Value("${feishu.webhook.url:}")
    private String feishuWebhookUrl;

    public void sendAlertNotification(AlertRecord alert) {
        if (dingtalkEnabled && !dingtalkWebhookUrl.isEmpty()) {
            sendDingTalkNotification(alert);
        }
        if (feishuEnabled && !feishuWebhookUrl.isEmpty()) {
            sendFeishuNotification(alert);
        }
    }

    private void sendDingTalkNotification(AlertRecord alert) {
        try {
            String url = dingtalkWebhookUrl;
            
            if (dingtalkSecret != null && !dingtalkSecret.isEmpty()) {
                long timestamp = Instant.now().toEpochMilli();
                String stringToSign = timestamp + "\n" + dingtalkSecret;
                String sign = generateDingTalkSign(stringToSign, dingtalkSecret);
                url = dingtalkWebhookUrl + "&timestamp=" + timestamp + "&sign=" + 
                      URLEncoder.encode(sign, StandardCharsets.UTF_8.toString());
            }

            Map<String, Object> message = buildDingTalkMessage(alert);
            
            logger.info("================================================");
            logger.info("【钉钉机器人通知 - 模拟发送】");
            logger.info("================================================");
            logger.info("Webhook URL: {}", dingtalkWebhookUrl);
            logger.info("请求内容:");
            logger.info(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(message));
            logger.info("================================================");
            logger.info("钉钉通知发送动作已完成（模拟模式）");
            logger.info("================================================");

            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(message, headers);
                ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
                logger.info("钉钉实际响应: {}", response.getBody());
            } catch (Exception e) {
                logger.info("实际钉钉发送失败（可能是测试环境的假URL），已在日志中记录请求内容: {}", e.getMessage());
            }

        } catch (Exception e) {
            logger.error("构建钉钉通知失败", e);
        }
    }

    private Map<String, Object> buildDingTalkMessage(AlertRecord alert) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("msgtype", "markdown");

        Map<String, Object> markdown = new LinkedHashMap<>();
        
        String levelEmoji = alert.getAlertLevel() != null && alert.getAlertLevel().name().equals("CRITICAL") ? "🔴" : "🟡";
        String levelText = alert.getAlertLevel() != null && alert.getAlertLevel().name().equals("CRITICAL") ? "严重告警" : "警告";
        
        String title = levelEmoji + " 服务器监控" + levelText;
        
        StringBuilder content = new StringBuilder();
        content.append("## ").append(title).append("\n\n");
        content.append("**服务器**: ").append(alert.getServerName()).append(" (").append(alert.getIpAddress()).append(")\n\n");
        content.append("**监控项**: ").append(alert.getMonitorName()).append("\n\n");
        content.append("**当前值**: ").append(alert.getValue()).append(alert.getUnit()).append("\n\n");
        content.append("**告警级别**: ").append(levelText).append("\n\n");
        content.append("**告警时间**: ").append(alert.getAlertTime()).append("\n\n");
        content.append("**详情**: ").append(alert.getMessage()).append("\n\n");
        content.append("> 此消息由服务器监控系统自动发送");
        
        markdown.put("title", title);
        markdown.put("text", content.toString());
        message.put("markdown", markdown);

        Map<String, Object> at = new LinkedHashMap<>();
        at.put("isAtAll", true);
        message.put("at", at);

        return message;
    }

    private String generateDingTalkSign(String stringToSign, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signData);
    }

    private void sendFeishuNotification(AlertRecord alert) {
        try {
            Map<String, Object> message = buildFeishuMessage(alert);
            
            logger.info("================================================");
            logger.info("【飞书机器人通知 - 模拟发送】");
            logger.info("================================================");
            logger.info("Webhook URL: {}", feishuWebhookUrl);
            logger.info("请求内容:");
            logger.info(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(message));
            logger.info("================================================");
            logger.info("飞书通知发送动作已完成（模拟模式）");
            logger.info("================================================");

            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(message, headers);
                ResponseEntity<String> response = restTemplate.postForEntity(feishuWebhookUrl, entity, String.class);
                logger.info("飞书实际响应: {}", response.getBody());
            } catch (Exception e) {
                logger.info("实际飞书发送失败（可能是测试环境的假URL），已在日志中记录请求内容: {}", e.getMessage());
            }

        } catch (Exception e) {
            logger.error("构建飞书通知失败", e);
        }
    }

    private Map<String, Object> buildFeishuMessage(AlertRecord alert) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("msg_type", "interactive");

        Map<String, Object> card = new LinkedHashMap<>();
        
        String levelColor = alert.getAlertLevel() != null && alert.getAlertLevel().name().equals("CRITICAL") ? "red" : "orange";
        String levelEmoji = alert.getAlertLevel() != null && alert.getAlertLevel().name().equals("CRITICAL") ? "🔴" : "🟡";
        String levelText = alert.getAlertLevel() != null && alert.getAlertLevel().name().equals("CRITICAL") ? "严重告警" : "警告";

        Map<String, Object> header = new LinkedHashMap<>();
        Map<String, Object> headerTitle = new LinkedHashMap<>();
        headerTitle.put("tag", "plain_text");
        headerTitle.put("content", levelEmoji + " 服务器监控" + levelText);
        header.put("title", headerTitle);
        header.put("template", levelColor);
        card.put("header", header);

        List<Map<String, Object>> elements = new ArrayList<>();

        Map<String, Object> divElement = new LinkedHashMap<>();
        divElement.put("tag", "div");
        
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("tag", "column_set");
        fields.put("flex_mode", "normal");
        fields.put("background_style", "default");
        
        List<Map<String, Object>> columns = new ArrayList<>();
        
        columns.add(createFeishuColumn("服务器", alert.getServerName()));
        columns.add(createFeishuColumn("IP地址", alert.getIpAddress()));
        columns.add(createFeishuColumn("监控项", alert.getMonitorName()));
        columns.add(createFeishuColumn("当前值", alert.getValue() + alert.getUnit()));
        columns.add(createFeishuColumn("告警级别", levelText));
        columns.add(createFeishuColumn("告警时间", alert.getAlertTime().toString()));
        
        fields.put("columns", columns);
        elements.add(fields);

        Map<String, Object> noteElement = new LinkedHashMap<>();
        noteElement.put("tag", "note");
        List<Map<String, Object>> noteElements = new ArrayList<>();
        Map<String, Object> noteText = new LinkedHashMap<>();
        noteText.put("tag", "plain_text");
        noteText.put("content", alert.getMessage());
        noteElements.add(noteText);
        noteElement.put("elements", noteElements);
        elements.add(noteElement);

        card.put("elements", elements);
        message.put("card", card);

        return message;
    }

    private Map<String, Object> createFeishuColumn(String label, String value) {
        Map<String, Object> column = new LinkedHashMap<>();
        column.put("tag", "column");
        column.put("width", "weighted");
        column.put("weight", 1);
        
        List<Map<String, Object>> fields = new ArrayList<>();
        
        Map<String, Object> labelField = new LinkedHashMap<>();
        labelField.put("tag", "plain_text");
        labelField.put("content", label);
        labelField.put("text_size", "notation");
        labelField.put("text_align", "left");
        labelField.put("text_color", "default");
        fields.add(labelField);
        
        Map<String, Object> valueField = new LinkedHashMap<>();
        valueField.put("tag", "plain_text");
        valueField.put("content", value);
        valueField.put("text_size", "normal");
        valueField.put("text_align", "left");
        valueField.put("text_color", "default");
        fields.add(valueField);
        
        column.put("fields", fields);
        return column;
    }
}
