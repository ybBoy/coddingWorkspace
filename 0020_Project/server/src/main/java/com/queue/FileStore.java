/**
 * FileStore 文件持久化模块
 * 职责：将内存中的队列数据定时保存到本地 JSON 文件，服务启动时恢复数据
 *
 * 增强功能：
 * 1. 支持配置数据文件路径（系统属性 queue.data.file > 环境变量 QUEUE_DATA_FILE > 默认）
 * 2. 保存时先写临时文件，成功后原子替换，防止中途写坏数据
 * 3. 默认数据文件位于用户目录下 ~/.queue-system/queue-data.json，避免相对路径不稳定
 *
 * 数据流：QueueService 更新内存队列 -> 定时任务触发 FileStore.save() -> 写入 JSON 文件
 *        服务启动时 -> FileStore.load() -> 从 JSON 文件恢复数据到内存
 */
package com.queue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Timer;
import java.util.TimerTask;

public class FileStore {

    private static final String DATA_FILE_NAME = "queue-data.json";
    private static final String TMP_SUFFIX = ".tmp";
    private static final long SAVE_INTERVAL = 30000;
    private static final String DEFAULT_DIR = ".queue-system";

    private final ObjectMapper objectMapper;
    private final QueueService queueService;
    private final Timer saveTimer;
    private final File dataFile;
    private final File tmpFile;

    public FileStore(QueueService queueService) {
        this.queueService = queueService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.saveTimer = new Timer("QueueSaveTimer", true);
        this.dataFile = resolveDataFile();
        this.tmpFile = new File(dataFile.getParentFile(), dataFile.getName() + TMP_SUFFIX);
        ensureParentDir();
    }

    /**
     * 解析数据文件位置
     * 优先级：
     * 1. 系统属性 -Dqueue.data.file=/path/to/file.json
     * 2. 环境变量 QUEUE_DATA_FILE=/path/to/file.json
     * 3. 用户目录下 ~/.queue-system/queue-data.json
     */
    private File resolveDataFile() {
        String sysPath = System.getProperty("queue.data.file");
        if (sysPath != null && !sysPath.trim().isEmpty()) {
            return new File(sysPath);
        }
        String envPath = System.getenv("QUEUE_DATA_FILE");
        if (envPath != null && !envPath.trim().isEmpty()) {
            return new File(envPath);
        }
        String userHome = System.getProperty("user.home");
        if (userHome == null || userHome.trim().isEmpty()) {
            userHome = ".";
        }
        return new File(new File(userHome, DEFAULT_DIR), DATA_FILE_NAME);
    }

    /**
     * 确保数据文件的父目录存在
     */
    private void ensureParentDir() {
        File parentDir = dataFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            boolean created = parentDir.mkdirs();
            if (created) {
                System.out.println("已创建数据目录: " + parentDir.getAbsolutePath());
            }
        }
    }

    public void startAutoSave() {
        saveTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                save();
            }
        }, SAVE_INTERVAL, SAVE_INTERVAL);
        System.out.println("自动保存已启动，间隔 " + (SAVE_INTERVAL / 1000) + " 秒");
        System.out.println("数据文件位置: " + dataFile.getAbsolutePath());
    }

    public void stopAutoSave() {
        saveTimer.cancel();
        save();
        System.out.println("自动保存已停止，最后一次保存完成");
    }

    /**
     * 安全保存：
     * 1. 先写入临时文件 xxx.json.tmp
     * 2. 确认写入成功后，用 Files.move 原子替换正式文件
     * 3. 这样即使写入过程中断电或崩溃，正式文件仍然保持上一次的完好数据
     */
    public void save() {
        try {
            ensureParentDir();
            QueueState state = queueService.getQueueState();

            if (tmpFile.exists()) {
                if (!tmpFile.delete()) {
                    System.err.println("无法删除旧临时文件: " + tmpFile.getAbsolutePath());
                }
            }

            objectMapper.writeValue(tmpFile, state);

            if (!tmpFile.exists() || tmpFile.length() == 0) {
                System.err.println("临时文件写入失败，跳过替换");
                return;
            }

            Files.move(
                    tmpFile.toPath(),
                    dataFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
            );
        } catch (IOException e) {
            System.err.println("保存队列数据失败: " + e.getMessage());
            if (tmpFile.exists()) {
                try {
                    tmpFile.delete();
                } catch (Exception ignored) {
                }
            }
        }
    }

    /**
     * 加载数据：
     * 优先加载正式文件，如果不存在且临时文件存在（可能上次异常中断），则尝试从临时文件恢复
     */
    public QueueState load() {
        File loadFile = dataFile;
        if (!dataFile.exists()) {
            if (tmpFile.exists()) {
                System.out.println("正式文件不存在，尝试从临时文件恢复: " + tmpFile.getAbsolutePath());
                loadFile = tmpFile;
            } else {
                System.out.println("未找到数据文件，使用空队列");
                System.out.println("数据文件位置: " + dataFile.getAbsolutePath());
                return null;
            }
        }

        if (loadFile.length() == 0) {
            System.err.println("数据文件为空，使用空队列");
            return null;
        }

        try {
            QueueState state = objectMapper.readValue(loadFile, QueueState.class);
            System.out.println("队列数据已从 " + loadFile.getAbsolutePath() + " 恢复");

            if (loadFile == tmpFile) {
                try {
                    Files.move(
                            tmpFile.toPath(),
                            dataFile.toPath(),
                            StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.ATOMIC_MOVE
                    );
                    System.out.println("已将临时文件迁移为正式文件");
                } catch (IOException e) {
                    System.err.println("迁移临时文件失败: " + e.getMessage());
                }
            }
            return state;
        } catch (IOException e) {
            System.err.println("加载队列数据失败: " + e.getMessage());
            return null;
        }
    }

    public String getDataFilePath() {
        return dataFile.getAbsolutePath();
    }
}
