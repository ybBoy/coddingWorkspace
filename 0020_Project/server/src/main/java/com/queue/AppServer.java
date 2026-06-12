/**
 * AppServer 主服务器启动类
 * 职责：初始化各个模块（QueueService、FileStore、QueueWebSocket），启动服务
 *
 * 迭代新增：
 * 1. 从 counters.json 加载初始窗口配置，替代硬编码的 1/2/3 号窗口
 * 2. 若 counters.json 不存在，创建默认配置
 * 3. 窗口配置变更时通过 FileStore 持久化
 *
 * 启动流程：
 * 1. 创建 QueueService 实例
 * 2. 创建 FileStore
 * 3. 从 counters.json 加载窗口配置
 * 4. 从 queue-data.json 恢复队列数据
 * 5. 启动 FileStore 定时自动保存
 * 6. 启动 WebSocket 服务器
 * 7. 注册 JVM 关闭钩子，确保退出时保存数据
 */
package com.queue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AppServer {

    private static final int WS_PORT = 8080;

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  实时排队叫号系统 - 后端服务启动中");
        System.out.println("========================================");

        QueueService queueService = new QueueService();
        FileStore fileStore = new FileStore(queueService);

        List<Counter> counters = fileStore.loadCounters();
        if (counters == null || counters.isEmpty()) {
            counters = createDefaultCounters();
            queueService.setCounters(counters);
            fileStore.saveCounters(counters);
            System.out.println("已创建默认窗口配置");
        } else {
            queueService.setCounters(counters);
        }

        QueueState savedState = fileStore.load();
        if (savedState != null) {
            queueService.restoreState(savedState);
        }

        fileStore.startAutoSave();

        final QueueWebSocket wsServer = new QueueWebSocket(WS_PORT, queueService);
        wsServer.start();

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("\n正在关闭服务...");
                try {
                    fileStore.saveCounters(queueService.getCounters());
                    fileStore.stopAutoSave();
                    wsServer.stop();
                } catch (Exception e) {
                    System.err.println("关闭服务时出错: " + e.getMessage());
                }
                System.out.println("服务已关闭");
            }
        }));

        System.out.println("========================================");
        System.out.println("  服务启动完成！");
        System.out.println("  WebSocket 端口: " + WS_PORT);
        System.out.println("  数据文件: " + fileStore.getDataFilePath());
        System.out.println("  窗口配置: " + fileStore.getCountersFilePath());
        System.out.println("  按 Ctrl+C 停止服务");
        System.out.println("========================================");

        try {
            while (true) {
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static List<Counter> createDefaultCounters() {
        List<Counter> counters = new ArrayList<>();
        counters.add(new Counter("counter-1", "1号窗口",
                Arrays.asList("咨询", "办理", "售后")));
        counters.add(new Counter("counter-2", "2号窗口",
                Arrays.asList("咨询", "办理")));
        counters.add(new Counter("counter-3", "3号窗口",
                Arrays.asList("售后")));
        return counters;
    }
}
