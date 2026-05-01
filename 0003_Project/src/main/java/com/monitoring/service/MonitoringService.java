package com.monitoring.service;

import com.monitoring.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MonitoringService {

    private static final Logger logger = LoggerFactory.getLogger(MonitoringService.class);

    @Autowired
    private ServerConfigService serverConfigService;

    @Autowired
    private AlertService alertService;

    @Autowired
    private HistoryDataService historyDataService;

    private final Map<String, ServerStatus> statusCache = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public Map<String, ServerStatus> getAllServerStatuses() {
        return new ConcurrentHashMap<>(statusCache);
    }

    public ServerStatus getServerStatus(String serverId) {
        return statusCache.get(serverId);
    }

    public void executeMonitoring() {
        logger.debug("开始执行监控任务...");
        List<ServerConfig> enabledServers = serverConfigService.getEnabledServers();

        for (ServerConfig config : enabledServers) {
            ServerStatus status = checkServer(config);
            statusCache.put(config.getId(), status);

            if (status.hasAlarms()) {
                alertService.processAlarms(status, config);
            }
        }
        logger.debug("监控任务执行完成，共检查 {} 台服务器", enabledServers.size());
    }

    private ServerStatus checkServer(ServerConfig config) {
        ServerStatus status = new ServerStatus();
        status.setServerId(config.getId());
        status.setServerName(config.getName());
        status.setIpAddress(config.getIpAddress());
        status.setCheckTime(LocalDateTime.now());
        status.setOnline(true);

        for (MonitorItem item : config.getMonitorItems()) {
            if (!item.isEnabled()) {
                continue;
            }

            ServerStatus.MonitorStatus monitorStatus = generateMonitorStatus(item, config);
            status.getMonitorStatuses().add(monitorStatus);

            historyDataService.saveDataPoint(config.getId(), item.getType(), monitorStatus.getValue());
        }

        return status;
    }

    private ServerStatus.MonitorStatus generateMonitorStatus(MonitorItem item, ServerConfig config) {
        ServerStatus.MonitorStatus status = new ServerStatus.MonitorStatus();
        status.setType(item.getType());
        status.setName(item.getName());
        status.setWarningThreshold(item.getWarningThreshold());
        status.setCriticalThreshold(item.getCriticalThreshold());
        status.setThreshold(item.getCriticalThreshold());
        status.setUnit(item.getUnit());

        double value = generateSimulatedValue(item, config);
        status.setValue(value);

        AlertLevel alertLevel = checkAlertLevel(item, value);
        status.setAlertLevel(alertLevel);
        status.setAlarming(alertLevel != AlertLevel.NORMAL);

        if (alertLevel != AlertLevel.NORMAL) {
            String levelText = alertLevel == AlertLevel.CRITICAL ? "【严重】" : "【警告】";
            String thresholdText = alertLevel == AlertLevel.CRITICAL 
                    ? String.format("%.1f%s", item.getCriticalThreshold(), item.getUnit())
                    : String.format("%.1f%s", item.getWarningThreshold(), item.getUnit());
            
            String message = String.format("%s服务器[%s] IP[%s] %s异常！当前值: %.1f%s，阈值: %s",
                    levelText,
                    config.getName(), config.getIpAddress(),
                    item.getName(), value, item.getUnit(), thresholdText);
            
            status.setMessage(message);
            logger.warn("检测到告警: {}", message);
        }

        return status;
    }

    private double generateSimulatedValue(MonitorItem item, ServerConfig config) {
        double baseValue;
        double fluctuation;

        switch (item.getType()) {
            case MonitorItem.CPU:
                baseValue = 50 + (config.getName().hashCode() % 20);
                fluctuation = random.nextDouble() * 40;
                break;
            case MonitorItem.MEMORY:
                baseValue = 60 + (config.getName().hashCode() % 15);
                fluctuation = random.nextDouble() * 30;
                break;
            case MonitorItem.DISK:
                baseValue = 15 + (config.getName().hashCode() % 10);
                fluctuation = random.nextDouble() * 15;
                break;
            default:
                baseValue = 50;
                fluctuation = random.nextDouble() * 20;
        }

        double value = baseValue + fluctuation;
        if (value > 99) {
            value = 99;
        }
        return Math.round(value * 10.0) / 10.0;
    }

    private AlertLevel checkAlertLevel(MonitorItem item, double value) {
        switch (item.getType()) {
            case MonitorItem.DISK:
                if (value <= item.getCriticalThreshold()) {
                    return AlertLevel.CRITICAL;
                } else if (value <= item.getWarningThreshold()) {
                    return AlertLevel.WARNING;
                }
                return AlertLevel.NORMAL;

            case MonitorItem.CPU:
            case MonitorItem.MEMORY:
            default:
                if (value >= item.getCriticalThreshold()) {
                    return AlertLevel.CRITICAL;
                } else if (value >= item.getWarningThreshold()) {
                    return AlertLevel.WARNING;
                }
                return AlertLevel.NORMAL;
        }
    }

    @Deprecated
    private boolean checkThreshold(MonitorItem item, double value) {
        switch (item.getType()) {
            case MonitorItem.DISK:
                return value <= item.getCriticalThreshold();
            case MonitorItem.CPU:
            case MonitorItem.MEMORY:
            default:
                return value >= item.getCriticalThreshold();
        }
    }
}
