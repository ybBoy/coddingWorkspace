package com.kitchen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 文件存储层
 * 职责：把内存里的订单 + 菜单 持久化到 backend/data/
 *  - saveOrders() / saveMenu() 定时刷盘（每 5 秒）
 *  - loadOrders() / loadMenu() 启动时恢复
 *  - 订单只持久化未完成的（NEW / COOKING），菜单完整持久化
 *
 * 被 AppServer 初始化
 */
public class FileStore {
    private final Path ordersFile;
    private final Path menuFile;
    private final Gson gson;
    private final ScheduledExecutorService scheduler;

    public FileStore(String baseDir) {
        Path dataDir = Paths.get(baseDir, "data");
        this.ordersFile = dataDir.resolve("orders.json");
        this.menuFile = dataDir.resolve("menu.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "file-store-scheduler");
            t.setDaemon(true);
            return t;
        });
        try {
            Files.createDirectories(dataDir);
            if (!Files.exists(ordersFile)) Files.write(ordersFile, "[]".getBytes(StandardCharsets.UTF_8));
            if (!Files.exists(menuFile))   Files.write(menuFile,   "[]".getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** 启动定时保存，每 5 秒把未完成订单和菜单刷盘一次 */
    public void startAutoSave(OrderService orderService, MenuService menuService) {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                saveOrders(orderService.listPendingOrders());
                saveMenu(menuService.listAll());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 5, 5, TimeUnit.SECONDS);
    }

    // ========== 订单 ==========
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

    public void shutdown() { scheduler.shutdown(); }
}
