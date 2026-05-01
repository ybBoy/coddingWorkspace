package com.scenic.service;

import com.scenic.entity.Area;
import com.scenic.entity.ScenicSpot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Random;

@Service
public class VisitorSimulatorService {
    private static final Logger logger = LoggerFactory.getLogger(VisitorSimulatorService.class);
    private final Random random = new Random();

    @Autowired
    private DataStoreService dataStoreService;

    @Autowired
    private WebSocketService webSocketService;

    private volatile boolean running = true;

    public void setRunning(boolean running) {
        this.running = running;
    }

    public boolean isRunning() {
        return running;
    }

    @Scheduled(fixedRate = 2000)
    public void simulateVisitorFlow() {
        if (!running) {
            return;
        }

        Collection<Area> areas = dataStoreService.getAllAreas();
        for (Area area : areas) {
            simulateAreaVisitors(area);
        }

        Collection<ScenicSpot> spots = dataStoreService.getAllSpots();
        for (ScenicSpot spot : spots) {
            simulateSpotVisitors(spot);
        }

        logger.debug("模拟数据更新完成");
        webSocketService.broadcastUpdate();
    }

    private void simulateAreaVisitors(Area area) {
        int current = area.getCurrentVisitors();
        int max = area.getMaxCapacity();
        
        int change;
        if (current < max * 0.3) {
            change = random.nextInt(20) - 5;
        } else if (current < max * 0.7) {
            change = random.nextInt(15) - 7;
        } else if (current < max * 0.9) {
            change = random.nextInt(10) - 8;
        } else {
            change = random.nextInt(5) - 10;
        }

        int newVisitors = Math.max(0, Math.min(max, current + change));
        area.setCurrentVisitors(newVisitors);
    }

    private void simulateSpotVisitors(ScenicSpot spot) {
        int current = spot.getCurrentVisitors();
        int max = spot.getMaxCapacity();
        
        int change;
        if (current < max * 0.3) {
            change = random.nextInt(10) - 2;
        } else if (current < max * 0.7) {
            change = random.nextInt(8) - 4;
        } else if (current < max * 0.9) {
            change = random.nextInt(5) - 5;
        } else {
            change = random.nextInt(3) - 6;
        }

        int newVisitors = Math.max(0, Math.min(max, current + change));
        spot.setCurrentVisitors(newVisitors);
    }

    public void resetAllData() {
        Collection<Area> areas = dataStoreService.getAllAreas();
        for (Area area : areas) {
            area.setCurrentVisitors(0);
        }

        Collection<ScenicSpot> spots = dataStoreService.getAllSpots();
        for (ScenicSpot spot : spots) {
            spot.setCurrentVisitors(0);
        }

        webSocketService.broadcastUpdate();
    }
}