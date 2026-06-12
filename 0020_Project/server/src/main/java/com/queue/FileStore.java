/**
 * FileStore 文件持久化模块
 * 职责：将内存中的队列数据定时保存到本地 JSON 文件，服务启动时恢复数据
 * 数据流：QueueService 更新内存队列 -> 定时任务触发 FileStore.save() -> 写入 JSON 文件
 *        服务启动时 -> FileStore.load() -> 从 JSON 文件恢复数据到内存
 */
package com.queue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class FileStore {

    private static final String DATA_FILE = "queue-data.json";
    private static final long SAVE_INTERVAL = 30000;

    private final ObjectMapper objectMapper;
    private final QueueService queueService;
    private final Timer saveTimer;

    public FileStore(QueueService queueService) {
        this.queueService = queueService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.saveTimer = new Timer("QueueSaveTimer", true);
    }

    public void startAutoSave() {
        saveTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                save();
            }
        }, SAVE_INTERVAL, SAVE_INTERVAL);
        System.out.println("自动保存已启动，间隔 " + (SAVE_INTERVAL / 1000) + " 秒");
    }

    public void stopAutoSave() {
        saveTimer.cancel();
        save();
        System.out.println("自动保存已停止，最后一次保存完成");
    }

    public void save() {
        try {
            QueueState state = queueService.getQueueState();
            File dataFile = new File(DATA_FILE);
            objectMapper.writeValue(dataFile, state);
            System.out.println("队列数据已保存到 " + dataFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("保存队列数据失败: " + e.getMessage());
        }
    }

    public QueueState load() {
        File dataFile = new File(DATA_FILE);
        if (!dataFile.exists()) {
            System.out.println("未找到数据文件，使用空队列");
            return null;
        }
        try {
            QueueState state = objectMapper.readValue(dataFile, QueueState.class);
            System.out.println("队列数据已从 " + dataFile.getAbsolutePath() + " 恢复");
            return state;
        } catch (IOException e) {
            System.err.println("加载队列数据失败: " + e.getMessage());
            return null;
        }
    }
}
