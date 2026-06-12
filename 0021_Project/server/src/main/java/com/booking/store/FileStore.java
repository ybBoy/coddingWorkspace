package com.booking.store;

import com.booking.model.Booking;
import com.booking.model.Session;
import com.booking.service.BookingService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.CodeSource;
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
 *
 * 存储路径：基于 JAR/class 文件所在目录的绝对路径（server/data/booking-data.json），
 *          避免从不同工作目录启动时读写到不同位置。
 */
public class FileStore {
    private static final String DATA_DIR_NAME = "data";
    private static final String DATA_FILE_NAME = "booking-data.json";
    private static final long SAVE_INTERVAL_SECONDS = 30;

    private final Path dataFilePath;   // 数据文件的绝对路径
    private final Path tempFilePath;   // 临时写入文件的绝对路径

    private final Gson gson;
    private final ScheduledExecutorService scheduler;
    private final BookingService bookingService;

    public FileStore(BookingService bookingService) {
        this.bookingService = bookingService;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();

        // 解析数据文件的绝对路径
        Path baseDir = resolveBaseDir();
        Path dataDir = baseDir.resolve(DATA_DIR_NAME);
        this.dataFilePath = dataDir.resolve(DATA_FILE_NAME);
        this.tempFilePath = dataDir.resolve(DATA_FILE_NAME + ".tmp");
        System.out.println("[FileStore] 数据存储路径: " + dataFilePath.toAbsolutePath());
    }

    /**
     * 解析基础目录：优先使用 JAR 或 class 所在目录，兜底使用 user.dir
     */
    private Path resolveBaseDir() {
        try {
            CodeSource codeSource = FileStore.class.getProtectionDomain().getCodeSource();
            if (codeSource != null && codeSource.getLocation() != null) {
                File codeFile = new File(codeSource.getLocation().toURI());
                // 如果是 jar，取其父目录；如果是 classes 目录，也取其父目录（到 server/）
                File parent = codeFile.isFile() ? codeFile.getParentFile() : codeFile;
                // 如果在 target/classes 或 target 下，回到项目根（server/）
                String parentName = parent.getName();
                if ("classes".equals(parentName) || "target".equals(parentName)) {
                    parent = parent.getParentFile();
                }
                if (parent != null && parent.exists()) {
                    return parent.toPath();
                }
            }
        } catch (URISyntaxException | SecurityException e) {
            System.err.println("[FileStore] 无法解析代码目录，使用 user.dir: " + e.getMessage());
        }
        return Paths.get(System.getProperty("user.dir"));
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
            Path parentDir = dataFilePath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }

            StoreData data = new StoreData();
            data.sessions = bookingService.getAllSessions();
            data.bookings = bookingService.getAllBookings();
            data.waitlistQueues = bookingService.getAllWaitlistQueues();

            String json = gson.toJson(data);

            // 先写入临时文件，再原子替换，防止损坏
            Files.write(tempFilePath, json.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            Files.move(tempFilePath, dataFilePath,
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

            System.out.println("[FileStore] 数据已保存: " + dataFilePath.toAbsolutePath());
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
            if (!Files.exists(dataFilePath)) {
                System.out.println("[FileStore] 未找到数据文件，将使用初始数据");
                return false;
            }

            String json = new String(Files.readAllBytes(dataFilePath), StandardCharsets.UTF_8);
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
