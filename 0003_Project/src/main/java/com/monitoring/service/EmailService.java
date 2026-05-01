package com.monitoring.service;

import com.monitoring.model.AlertRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${alert.email.enabled:true}")
    private boolean emailEnabled;

    @Value("${alert.email.recipients:}")
    private String recipientsConfig;

    @Value("${spring.mail.username:monitor@example.com}")
    private String fromEmail;

    private List<String> recipients;

    @PostConstruct
    public void init() {
        if (recipientsConfig != null && !recipientsConfig.trim().isEmpty()) {
            recipients = Arrays.asList(recipientsConfig.split(","));
        } else {
            recipients = Arrays.asList("admin@example.com");
        }
        logger.info("邮件服务初始化完成，状态: {}, 收件人: {}",
                emailEnabled ? "已启用" : "已禁用", recipients);
    }

    @Async
    public void sendAlertEmail(AlertRecord alert) {
        if (!emailEnabled) {
            logger.info("邮件发送已禁用，跳过告警邮件: {}", alert.getMessage());
            return;
        }

        String subject = buildSubject(alert);
        String content = buildContent(alert);

        if (mailSender != null) {
            try {
                sendActualEmail(subject, content);
                logger.info("告警邮件已实际发送至: {}", recipients);
            } catch (Exception e) {
                logger.warn("实际邮件发送失败，切换到模拟模式。错误: {}", e.getMessage());
                logSimulatedEmail(subject, content);
            }
        } else {
            logSimulatedEmail(subject, content);
        }
    }

    private String buildSubject(AlertRecord alert) {
        return String.format("[监控告警] %s - %s",
                alert.getServerName(),
                alert.getMonitorName());
    }

    private String buildContent(AlertRecord alert) {
        StringBuilder sb = new StringBuilder();
        sb.append("服务器监控告警通知\n");
        sb.append("============================================\n\n");
        sb.append("告警信息详情：\n");
        sb.append("--------------------------------------------\n");
        sb.append("服务器名称: ").append(alert.getServerName()).append("\n");
        sb.append("IP地址: ").append(alert.getIpAddress()).append("\n");
        sb.append("监控项目: ").append(alert.getMonitorName()).append("\n");
        sb.append("当前值: ").append(String.format("%.1f%s", alert.getValue(), alert.getUnit())).append("\n");
        sb.append("告警阈值: ").append(String.format("%.1f%s", alert.getThreshold(), alert.getUnit())).append("\n");
        sb.append("告警时间: ").append(alert.getAlertTime().format(DATE_FORMAT)).append("\n");
        sb.append("告警消息: ").append(alert.getMessage()).append("\n");
        sb.append("--------------------------------------------\n\n");
        sb.append("请及时检查服务器状态！\n");
        sb.append("\n此邮件由服务器监控系统自动发送，请勿回复。");
        return sb.toString();
    }

    private void sendActualEmail(String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setSubject(subject);
        message.setText(content);
        message.setTo(recipients.toArray(new String[0]));
        mailSender.send(message);
    }

    private void logSimulatedEmail(String subject, String content) {
        logger.info("================================================");
        logger.info("【模拟邮件发送】");
        logger.info("================================================");
        logger.info("发件人: {}", fromEmail);
        logger.info("收件人: {}", recipients);
        logger.info("主题: {}", subject);
        logger.info("------------------------------------------------");
        logger.info("内容:");
        for (String line : content.split("\n")) {
            logger.info(line);
        }
        logger.info("================================================");
        logger.info("邮件发送动作已完成（模拟模式）");
        logger.info("================================================");
    }
}
