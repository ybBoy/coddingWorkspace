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
            if (config.isInMaintenance()) {
                logger.info("服务器[{}]处于维护模式，跳过监控", config.getName());
                ServerStatus maintenanceStatus = createMaintenanceStatus(config);
                statusCache.put(config.getId(), maintenanceStatus);
                continue;
            }
            
            ServerStatus status = checkServer(config);
            statusCache.put(config.getId(), status);

            if (status.hasAlarms()) {
                alertService.processAlarms(status, config);
            }
        }
        logger.debug("监控任务执行完成，共检查 {} 台服务器", enabledServers.size());
    }

    private ServerStatus createMaintenanceStatus(ServerConfig config) {
        ServerStatus status = new ServerStatus();
        status.setServerId(config.getId());
        status.setServerName(config.getName());
        status.setIpAddress(config.getIpAddress());
        status.setCheckTime(LocalDateTime.now());
        status.setOnline(true);
        status.setInMaintenance(true);
        status.setMaintenanceEndTime(config.getMaintenanceEndTime());
        return status;
    }

    private ServerStatus checkServer(ServerConfig config) {
        ServerStatus status = new ServerStatus();
        status.setServerId(config.getId());
        status.setServerName(config.getName());
        status.setIpAddress(config.getIpAddress());
        status.setCheckTime(LocalDateTime.now());
        status.setOnline(true);

        Map<String, ServerStatus.MonitorStatus> monitorStatusMap = new ConcurrentHashMap<>();
        
        for (MonitorItem item : config.getMonitorItems()) {
            if (!item.isEnabled()) {
                continue;
            }

            ServerStatus.MonitorStatus monitorStatus = generateMonitorStatus(item, config);
            monitorStatusMap.put(item.getType(), monitorStatus);
            status.getMonitorStatuses().add(monitorStatus);

            historyDataService.saveDataPoint(config.getId(), item.getType(), monitorStatus.getValue());
        }

        if (config.hasCustomAlertConditions()) {
            applyCustomAlertConditions(status, config, monitorStatusMap);
        }

        return status;
    }

    private void applyCustomAlertConditions(ServerStatus status, ServerConfig config, 
            Map<String, ServerStatus.MonitorStatus> monitorStatusMap) {
        
        for (AlertCondition condition : config.getAlertConditions()) {
            if (!condition.isEnabled()) {
                continue;
            }

            AlertLevel resultLevel = evaluateAlertCondition(condition, monitorStatusMap);
            
            if (resultLevel != AlertLevel.NORMAL) {
                String message = buildConditionMessage(condition, resultLevel, config, monitorStatusMap);
                
                for (ServerStatus.MonitorStatus monitorStatus : status.getMonitorStatuses()) {
                    for (AlertCondition.ConditionRule rule : condition.getRules()) {
                        if (monitorStatus.getType().equals(rule.getMonitorType())) {
                            if (resultLevel == AlertLevel.CRITICAL || 
                                (resultLevel == AlertLevel.WARNING && 
                                 monitorStatus.getAlertLevel() == AlertLevel.NORMAL)) {
                                monitorStatus.setAlertLevel(resultLevel);
                                monitorStatus.setAlarming(true);
                                monitorStatus.setMessage(message);
                            }
                        }
                    }
                }
                
                logger.warn("组合条件触发告警: {}", message);
            }
        }
    }

    private AlertLevel evaluateAlertCondition(AlertCondition condition, 
            Map<String, ServerStatus.MonitorStatus> monitorStatusMap) {
        
        List<AlertCondition.ConditionRule> rules = condition.getRules();
        if (rules == null || rules.isEmpty()) {
            return AlertLevel.NORMAL;
        }

        AlertCondition.ConditionOperator operator = condition.getOperator();
        AlertLevel highestLevel = AlertLevel.NORMAL;
        int matchedCount = 0;

        for (AlertCondition.ConditionRule rule : rules) {
            ServerStatus.MonitorStatus monitorStatus = monitorStatusMap.get(rule.getMonitorType());
            if (monitorStatus == null) {
                continue;
            }

            boolean matched = rule.evaluate(monitorStatus.getValue());
            
            if (matched) {
                matchedCount++;
                if (rule.getLevel() == AlertLevel.CRITICAL) {
                    highestLevel = AlertLevel.CRITICAL;
                } else if (rule.getLevel() == AlertLevel.WARNING && highestLevel == AlertLevel.NORMAL) {
                    highestLevel = AlertLevel.WARNING;
                }
            }
        }

        if (operator == AlertCondition.ConditionOperator.AND) {
            return matchedCount == rules.size() ? highestLevel : AlertLevel.NORMAL;
        } else {
            return matchedCount > 0 ? highestLevel : AlertLevel.NORMAL;
        }
    }

    private String buildConditionMessage(AlertCondition condition, AlertLevel level,
            ServerConfig config, Map<String, ServerStatus.MonitorStatus> monitorStatusMap) {
        
        String levelText = level == AlertLevel.CRITICAL ? "【严重】" : "【警告】";
        String operatorText = condition.getOperator() == AlertCondition.ConditionOperator.AND ? " 且 " : " 或 ";
        
        StringBuilder ruleDesc = new StringBuilder();
        for (int i = 0; i < condition.getRules().size(); i++) {
            AlertCondition.ConditionRule rule = condition.getRules().get(i);
            ServerStatus.MonitorStatus monitorStatus = monitorStatusMap.get(rule.getMonitorType());
            String currentValue = monitorStatus != null ? 
                String.format("%.1f%s", monitorStatus.getValue(), monitorStatus.getUnit()) : "N/A";
            
            ruleDesc.append(rule.getMonitorType())
                    .append(rule.getComparison().getSymbol())
                    .append(rule.getThreshold())
                    .append("(当前:").append(currentValue).append(")");
            
            if (i < condition.getRules().size() - 1) {
                ruleDesc.append(operatorText);
            }
        }
        
        return String.format("%s服务器[%s] IP[%s] 组合条件告警: %s",
                levelText, config.getName(), config.getIpAddress(), ruleDesc.toString());
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
