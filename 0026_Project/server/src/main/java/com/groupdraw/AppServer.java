package com.groupdraw;

import com.groupdraw.store.JsonStore;
import com.groupdraw.ws.GroupSocket;

public class AppServer {
    private static final int WS_PORT = 8080;

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  活动抽签分组服务器 v2.0");
        System.out.println("========================================");

        JsonStore jsonStore = new JsonStore();
        boolean loaded = jsonStore.load();
        if (loaded) {
            System.out.println("[OK] 已从本地文件加载数据 (" + jsonStore.getAllRooms().size() + " 个活动房间)");
        } else {
            System.out.println("[INFO] 未找到本地数据文件，使用默认配置");
        }

        GroupSocket socketServer = new GroupSocket(WS_PORT, jsonStore);

        jsonStore.startAutoSave();
        System.out.println("[OK] 自动保存已启动 (每30秒)");

        socketServer.start();
        System.out.println("[OK] WebSocket 服务器已启动，端口: " + WS_PORT);
        System.out.println("[INFO] 主持人可在页面中创建活动房间");
        System.out.println("========================================");

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("\n[INFO] 正在关闭服务器...");
                try {
                    jsonStore.stopAutoSave();
                    jsonStore.save();
                    socketServer.stop();
                    System.out.println("[OK] 数据已保存，服务器已关闭");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }));
    }
}
