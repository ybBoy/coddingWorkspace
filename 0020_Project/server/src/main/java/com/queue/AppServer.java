/**
 * AppServer 主服务器启动类
 * 职责：初始化各个模块（QueueService、FileStore、QueueWebSocket），启动服务
 * 启动流程：
 * 1. 创建 QueueService 实例
 * 2. 创建 FileStore，从本地 JSON 恢复数据
 * 3. 启动 FileStore 定时自动保存
 * 4. 启动 WebSocket 服务器
 * 5. 注册 JVM 关闭钩子，确保退出时保存数据
 */
package com.queue;

public class AppServer {

    private static final int WS_PORT = 8080;

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  实时排队叫号系统 - 后端服务启动中");
        System.out.println("========================================");

        QueueService queueService = new QueueService();
        FileStore fileStore = new FileStore(queueService);

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
}
