/**
 * FileStore 文件持久化模块
 * 职责：将内存中的队列数据定时保存到本地 JSON 文件，服务启动时恢复数据
 *
 * 迭代新增：支持加载/保存 counters.json 窗口配置文件
 *
 * 增强功能：
 * 1. 支持配置数据文件路径（系统属性 queue.data.file > 环境变量 QUEUE_DATA_FILE > 默认）
 * 2. 保存时先写临时文件，成功后原子替换，防止中途写坏数据
 * 3. 默认数据文件位于用户目录下 ~/.queue-system/，避免相对路径不稳定
 *
 * 数据流：QueueService 更新内存队列 -> 定时任务触发 FileStore.save() -> 写入 JSON 文件
 *        服务启动时 -> FileStore.load() -> 从 JSON 文件恢复数据到内存
 *        服务启动时 -> FileStore.loadCounters() -> 从 counters.json 加载初始窗口配置
 */
package com.queue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class FileStore {

    private static final String DATA_FILE_NAME = "queue-data.json";
    private static final String COUNTERS_FILE_NAME = "counters.json";
    private static final String TMP_SUFFIX = ".tmp";
    private static final long SAVE_INTERVAL = 30000;
    private static final String DEFAULT_DIR = ".queue-system";

    private final ObjectMapper objectMapper;
    private final QueueService queueService;
    private final Timer saveTimer;
    private final File dataFile;
    private final File countersFile;
    private final File tmpFile;

    public FileStore(QueueService queueService) {
        this.queueService = queueService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.saveTimer = new Timer("QueueSaveTimer", true);
        File dataDir = resolveDataDir();
        this.dataFile = new File(dataDir, DATA_FILE_NAME);
        this.countersFile = new File(dataDir, COUNTERS_FILE_NAME);
        this.tmpFile = new File(dataDir, DATA_FILE_NAME + TMP_SUFFIX);
    }

    private File resolveDataDir() {
        String sysPath = System.getProperty("queue.data.dir");
        if (sysPath != null && !sysPath.trim().isEmpty()) {
            File dir = new File(sysPath);
            ensureDir(dir);
            return dir;
        }
        String envPath = System.getenv("QUEUE_DATA_DIR");
        if (envPath != null && !envPath.trim().isEmpty()) {
            File dir = new File(envPath);
            ensureDir(dir);
            return dir;
        }
        String userHome = System.getProperty("user.home");
        if (userHome == null || userHome.trim().isEmpty()) {
            userHome = ".";
        }
        File dir = new File(userHome, DEFAULT_DIR);
        ensureDir(dir);
        return dir;
    }

    private void ensureDir(File dir) {
        if (dir != null && !dir.exists()) {
            boolean created = dir.mkdirs();
            if (created) {
                System.out.println("已创建数据目录: " + dir.getAbsolutePath());
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
        System.out.println("窗口配置位置: " + countersFile.getAbsolutePath());
    }

    public void stopAutoSave() {
        saveTimer.cancel();
        save();
        System.out.println("自动保存已停止，最后一次保存完成");
    }

    public void save() {
        try {
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

    public QueueState load() {
        File loadFile = dataFile;
        if (!dataFile.exists()) {
            if (tmpFile.exists()) {
                System.out.println("正式文件不存在，尝试从临时文件恢复: " + tmpFile.getAbsolutePath());
                loadFile = tmpFile;
            } else {
                System.out.println("未找到队列数据文件，使用空队列");
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

    public List<Counter> loadCounters() {
        if (!countersFile.exists()) {
            System.out.println("未找到窗口配置文件，将使用默认窗口配置");
            return null;
        }
        if (countersFile.length() == 0) {
            System.err.println("窗口配置文件为空，使用默认配置");
            return null;
        }
        try {
            List<Counter> counters = objectMapper.readValue(
                    countersFile,
                    new TypeReference<List<Counter>>() {}
            );
            System.out.println("已从配置文件加载 " + counters.size() + " 个窗口");
            return counters;
        } catch (IOException e) {
            System.err.println("加载窗口配置失败: " + e.getMessage());
            return null;
        }
    }

    public void saveCounters(List<Counter> counters) {
        if (counters == null) return;
        File tmp = new File(countersFile.getParentFile(), countersFile.getName() + TMP_SUFFIX);
        try {
            objectMapper.writeValue(tmp, counters);
            if (!tmp.exists() || tmp.length() == 0) {
                return;
            }
            Files.move(
                    tmp.toPath(),
                    countersFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
            );
        } catch (IOException e) {
            System.err.println("保存窗口配置失败: " + e.getMessage());
            if (tmp.exists()) {
                try { tmp.delete(); } catch (Exception ignored) {}
            }
        }
    }

    public String getDataFilePath() {
        return dataFile.getAbsolutePath();
    }

    public String getCountersFilePath() {
        return countersFile.getAbsolutePath();
    }
}
