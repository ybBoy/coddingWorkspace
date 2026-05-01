package com.monitoring.scheduler;

import com.monitoring.service.MonitoringService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class MonitoringScheduler {

    private static final Logger logger = LoggerFactory.getLogger(MonitoringScheduler.class);

    @Autowired
    private MonitoringService monitoringService;

    @PostConstruct
    public void init() {
        logger.info("监控调度器已初始化");
        logger.info("将在应用启动后立即执行首次监控检查");
        monitoringService.executeMonitoring();
    }

    @Scheduled(fixedRateString = "${monitoring.interval.seconds:60}000")
    public void scheduledMonitoring() {
        logger.debug("执行定时监控任务");
        try {
            monitoringService.executeMonitoring();
        } catch (Exception e) {
            logger.error("监控任务执行失败", e);
        }
    }
}
