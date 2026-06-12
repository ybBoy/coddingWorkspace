package com.booking.store;

import com.booking.model.Booking;
import com.booking.model.Session;
import com.booking.service.BookingService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * FileStore 文件存储类
 * 职责：将内存中的场次和预约数据定时保存到本地 JSON 文件
 *       服务启动时从 JSON 文件恢复数据
 * 存储格式：JSON，包含 sessions、bookings、waitlistQueues 三部分
 * 定时保存：每 30 秒自动保存一次，同时在关键操作后立即保存
 */
public class FileStore {
    private static final String DATA_FILE = "data/booking-data.json";
    private static final long SAVE_INTERVAL_SECONDS = 30;

    private final Gson gson;
    private final ScheduledExecutorService scheduler;
    private final BookingService bookingService;

    public FileStore(BookingService bookingService) {
        this.bookingService = bookingService;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    /**
     * 启动定时保存任务
     */
    public void startAutoSave() {
        scheduler.scheduleAtFixedRate(
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            save();
                        } catch (Exception e) {
                            System.err.println("[FileStore] 定时保存数据失败: " + e.getMessage());
                        }
                    }
                },
                SAVE_INTERVAL_SECONDS,
                SAVE_INTERVAL_SECONDS,
                TimeUnit.SECONDS
        );
        System.out.println("[FileStore] 定时保存已启动，间隔 " + SAVE_INTERVAL_SECONDS + " 秒");
    }

    /**
     * 停止定时保存
     */
    public void stopAutoSave() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }

    /**
     * 保存数据到 JSON 文件
     */
    public synchronized void save() {
        try {
            Path dataPath = Paths.get(DATA_FILE);
            Path parentDir = dataPath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }

            StoreData data = new StoreData();
            data.sessions = bookingService.getAllSessions();
            data.bookings = bookingService.getAllBookings();
            data.waitlistQueues = bookingService.getAllWaitlistQueues();

            String json = gson.toJson(data);

            // 先写入临时文件，再原子替换，防止损坏
            Path tempPath = Paths.get(DATA_FILE + ".tmp");
            Files.write(tempPath, json.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            Files.move(tempPath, dataPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

            System.out.println("[FileStore] 数据已保存: " + dataPath.toAbsolutePath());
        } catch (Exception e) {
            System.err.println("[FileStore] 保存数据失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 从 JSON 文件加载数据
     * @return 是否成功加载了已有数据
     */
    public synchronized boolean load() {
        try {
            Path dataPath = Paths.get(DATA_FILE);
            if (!Files.exists(dataPath)) {
                System.out.println("[FileStore] 未找到数据文件，将使用初始数据");
                return false;
            }

            String json = new String(Files.readAllBytes(dataPath), StandardCharsets.UTF_8);
            StoreData data = gson.fromJson(json, StoreData.class);

            if (data != null) {
                if (data.sessions != null) {
                    bookingService.setSessions(data.sessions);
                }
                if (data.bookings != null) {
                    bookingService.setBookings(data.bookings);
                }
                if (data.waitlistQueues != null) {
                    bookingService.setWaitlistQueues(data.waitlistQueues);
                }
                System.out.println("[FileStore] 数据已加载: " + data.sessions.size() + " 个场次, "
                        + data.bookings.size() + " 条预约");
                return true;
            }
        } catch (Exception e) {
            System.err.println("[FileStore] 加载数据失败: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 用于序列化的数据包装类
     */
    private static class StoreData {
        List<Session> sessions;
        List<Booking> bookings;
        Map<String, List<String>> waitlistQueues;
    }
}
