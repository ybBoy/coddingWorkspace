package com.monitoring.service;

import com.monitoring.model.AlertRecord;
import com.monitoring.model.ServerConfig;
import com.monitoring.model.ServerStatus;
import com.monitoring.storage.JsonFileStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AlertService {

    private static final Logger logger = LoggerFactory.getLogger(AlertService.class);
    private static final String ALERT_RECORDS_FILE = "alert_records.json";

    @Autowired
    private JsonFileStorage jsonFileStorage;

    @Autowired
    private EmailService emailService;

    private List<AlertRecord> alertRecords;

    @PostConstruct
    public void init() {
        loadAlerts();
    }

    private void loadAlerts() {
        alertRecords = jsonFileStorage.loadList(ALERT_RECORDS_FILE, AlertRecord.class);
        if (alertRecords == null) {
            alertRecords = new ArrayList<>();
        }
    }

    private void saveAlerts() {
        jsonFileStorage.save(ALERT_RECORDS_FILE, alertRecords);
    }

    public void processAlarms(ServerStatus status, ServerConfig config) {
        for (ServerStatus.MonitorStatus monitorStatus : status.getMonitorStatuses()) {
            if (monitorStatus.isAlarming()) {
                boolean isNewAlarm = isNewAlarm(monitorStatus, config);
                if (isNewAlarm) {
                    AlertRecord record = createAlertRecord(monitorStatus, config);
                    alertRecords.add(record);
                    saveAlerts();

                    emailService.sendAlertEmail(record);
                }
            }
        }
    }

    private boolean isNewAlarm(ServerStatus.MonitorStatus monitorStatus, ServerConfig config) {
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);

        return !alertRecords.stream()
                .filter(r -> !r.isAcknowledged())
                .filter(r -> r.getServerId().equals(config.getId()))
                .filter(r -> r.getMonitorType().equals(monitorStatus.getType()))
                .filter(r -> r.getAlertTime().isAfter(fiveMinutesAgo))
                .findFirst()
                .isPresent();
    }

    private AlertRecord createAlertRecord(ServerStatus.MonitorStatus monitorStatus, ServerConfig config) {
        AlertRecord record = new AlertRecord();
        record.setId(UUID.randomUUID().toString());
        record.setServerId(config.getId());
        record.setServerName(config.getName());
        record.setIpAddress(config.getIpAddress());
        record.setMonitorType(monitorStatus.getType());
        record.setMonitorName(monitorStatus.getName());
        record.setValue(monitorStatus.getValue());
        record.setThreshold(monitorStatus.getThreshold());
        record.setUnit(monitorStatus.getUnit());
        record.setMessage(monitorStatus.getMessage());
        record.setAlertTime(LocalDateTime.now());
        record.setAcknowledged(false);
        return record;
    }

    public List<AlertRecord> getAllAlerts() {
        return new ArrayList<>(alertRecords);
    }

    public List<AlertRecord> getActiveAlerts() {
        return alertRecords.stream()
                .filter(r -> !r.isAcknowledged())
                .sorted((a, b) -> b.getAlertTime().compareTo(a.getAlertTime()))
                .collect(Collectors.toList());
    }

    public List<AlertRecord> getHistoricalAlerts() {
        return alertRecords.stream()
                .sorted((a, b) -> b.getAlertTime().compareTo(a.getAlertTime()))
                .collect(Collectors.toList());
    }

    public boolean acknowledgeAlert(String id, String user) {
        for (AlertRecord record : alertRecords) {
            if (record.getId().equals(id) && !record.isAcknowledged()) {
                record.setAcknowledged(true);
                record.setAcknowledgeTime(LocalDateTime.now());
                record.setAcknowledgeUser(user != null ? user : "系统");
                saveAlerts();
                logger.info("告警已确认: {} - 用户: {}", record.getMessage(), record.getAcknowledgeUser());
                return true;
            }
        }
        return false;
    }

    public void acknowledgeAllAlerts(String user) {
        for (AlertRecord record : alertRecords) {
            if (!record.isAcknowledged()) {
                record.setAcknowledged(true);
                record.setAcknowledgeTime(LocalDateTime.now());
                record.setAcknowledgeUser(user != null ? user : "系统");
            }
        }
        saveAlerts();
        logger.info("所有活跃告警已确认");
    }
}
