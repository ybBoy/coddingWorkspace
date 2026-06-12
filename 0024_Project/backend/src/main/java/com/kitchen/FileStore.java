package com.kitchen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
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
 * 职责：把内存里的订单持久化到 backend/data/orders.json
 *  - save() 由 OrderService 定时调用（每 5 秒）
 *  - load() 在 AppServer 启动时调用，恢复未完成订单
 * 只持久化未完成的订单（NEW / COOKING），已出餐订单不保存
 */
public class FileStore {
    private final Path dataFile;
    private final Gson gson;
    private final ScheduledExecutorService scheduler;

    public FileStore(String baseDir) {
        this.dataFile = Paths.get(baseDir, "data", "orders.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "file-store-scheduler");
            t.setDaemon(true);
            return t;
        });
        try {
            Files.createDirectories(dataFile.getParent());
            if (!Files.exists(dataFile)) {
                Files.write(dataFile, "[]".getBytes(StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** 启动定时保存，每 5 秒把未完成订单刷盘一次 */
    public void startAutoSave(OrderService orderService) {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                save(orderService.listPendingOrders());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 5, 5, TimeUnit.SECONDS);
    }

    /** 把订单列表写入 JSON 文件 */
    public synchronized void save(List<Order> orders) throws IOException {
        String json = gson.toJson(orders);
        Files.write(dataFile, json.getBytes(StandardCharsets.UTF_8));
    }

    /** 从 JSON 文件恢复订单 */
    public synchronized List<Order> load() {
        try {
            if (!Files.exists(dataFile)) return new ArrayList<>();
            String json = new String(Files.readAllBytes(dataFile), StandardCharsets.UTF_8);
            Type listType = new TypeToken<List<Order>>() {}.getType();
            List<Order> list = gson.fromJson(json, listType);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public void shutdown() {
        scheduler.shutdown();
    }
}
