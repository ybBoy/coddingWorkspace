package com.kitchen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 文件存储层（升级版：增加按天历史归档）
 *
 * 职责：
 *  - saveOrders() / loadOrders()     内存订单 <-> orders.json（未完成的）
 *  - saveMenu()   / loadMenu()       菜单 <-> menu.json
 *  - appendDailyArchive()            已出餐/撤销订单 -> archive/YYYY-MM-DD.json（按天追加）
 *  - loadDailyArchive(date)          读取某一天的归档订单（用于历史查询）
 *  - 每 5 秒定时刷盘 + 归档清理（只保留未完成的在内存里）
 *
 * 历史归档策略：
 *  - 每次刷盘前，找出 listAll() 中已经 DONE 或 CANCELLED 且时间 >24h 的，从内存移除并写入对应日期的 archive/
 *  - 这样重启不会丢统计数据，listToday 仍能通过读取当日 archive + 内存数据拼出完整今天
 */
public class FileStore {
    private final Path dataDir;
    private final Path ordersFile;
    private final Path menuFile;
    private final Path archiveDir;
    private final Gson gson;
    private final ScheduledExecutorService scheduler;

    public FileStore(String baseDir) {
        this.dataDir = Paths.get(baseDir, "data");
        this.ordersFile = dataDir.resolve("orders.json");
        this.menuFile = dataDir.resolve("menu.json");
        this.archiveDir = dataDir.resolve("archive");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "file-store-scheduler");
            t.setDaemon(true);
            return t;
        });
        try {
            Files.createDirectories(dataDir);
            Files.createDirectories(archiveDir);
            if (!Files.exists(ordersFile)) Files.write(ordersFile, "[]".getBytes(StandardCharsets.UTF_8));
            if (!Files.exists(menuFile))   Files.write(menuFile,   "[]".getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** 启动定时保存：每 5 秒刷盘 + 归档超过 24h 的已出餐订单 */
    public void startAutoSave(OrderService orderService, MenuService menuService) {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                // 1. 先归档老订单（从内存移除 DONE/CANCELLED 超过 24h 的，写入按天归档文件）
                orderService.archiveOldOrders(this::appendDailyArchive);
                // 2. 保存未完成订单
                saveOrders(orderService.listPendingOrders());
                // 3. 保存菜单
                saveMenu(menuService.listAll());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 5, 5, TimeUnit.SECONDS);
    }

    // ========== 未完成订单 ==========
    public synchronized void saveOrders(List<Order> orders) throws Exception {
        Files.write(ordersFile, gson.toJson(orders).getBytes(StandardCharsets.UTF_8));
    }

    public synchronized List<Order> loadOrders() {
        try {
            String json = new String(Files.readAllBytes(ordersFile), StandardCharsets.UTF_8);
            Type listType = new TypeToken<List<Order>>() {}.getType();
            List<Order> list = gson.fromJson(json, listType);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    // ========== 菜单 ==========
    public synchronized void saveMenu(List<MenuItem> menu) throws Exception {
        Files.write(menuFile, gson.toJson(menu).getBytes(StandardCharsets.UTF_8));
    }

    public synchronized List<MenuItem> loadMenu() {
        try {
            String json = new String(Files.readAllBytes(menuFile), StandardCharsets.UTF_8);
            Type listType = new TypeToken<List<MenuItem>>() {}.getType();
            List<MenuItem> list = gson.fromJson(json, listType);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    // ========== 历史归档 ==========

    /** 按天追加归档订单（DONE/CANCELLED 的） */
    public synchronized void appendDailyArchive(List<Order> orders) throws Exception {
        if (orders == null || orders.isEmpty()) return;
        // 按创建日期分组，然后分别写入对应日期的归档文件
        Map<String, List<Order>> byDate = new HashMap<>();
        for (Order o : orders) {
            String date = dateStr(o.getCreatedAt());
            byDate.computeIfAbsent(date, k -> new ArrayList<>()).add(o);
        }
        for (Map.Entry<String, List<Order>> e : byDate.entrySet()) {
            Path file = archiveDir.resolve(e.getKey() + ".json");
            List<Order> existing = new ArrayList<>();
            if (Files.exists(file)) {
                try {
                    String json = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
                    Type listType = new TypeToken<List<Order>>() {}.getType();
                    List<Order> prev = gson.fromJson(json, listType);
                    if (prev != null) existing = prev;
                } catch (Exception ignored) {}
            }
            // 合并，按订单ID去重
            Map<String, Order> merged = new LinkedHashMap<>();
            for (Order o : existing) merged.put(o.getId(), o);
            for (Order o : e.getValue())  merged.put(o.getId(), o);
            Files.write(file, gson.toJson(new ArrayList<>(merged.values())).getBytes(StandardCharsets.UTF_8));
        }
    }

    /** 读取某天的归档订单 */
    public synchronized List<Order> loadDailyArchive(String dateStr) {
        try {
            Path file = archiveDir.resolve(dateStr + ".json");
            if (!Files.exists(file)) return new ArrayList<>();
            String json = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
            Type listType = new TypeToken<List<Order>>() {}.getType();
            List<Order> list = gson.fromJson(json, listType);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /** 列出所有可用的归档日期（YYYY-MM-DD 列表，从新到旧排序） */
    public synchronized List<String> listArchiveDates() {
        List<String> dates = new ArrayList<>();
        try {
            if (!Files.exists(archiveDir)) return dates;
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(archiveDir, "*.json")) {
                for (Path p : stream) {
                    String name = p.getFileName().toString();
                    if (name.endsWith(".json")) {
                        dates.add(name.substring(0, name.length() - 5));
                    }
                }
            }
            dates.sort(Comparator.reverseOrder());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dates;
    }

    // ========== 工具 ==========
    public static String dateStr(long ts) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setTimeZone(TimeZone.getDefault());
        return sdf.format(new Date(ts));
    }

    public static String todayStr() { return dateStr(System.currentTimeMillis()); }

    public void shutdown() { scheduler.shutdown(); }
}
