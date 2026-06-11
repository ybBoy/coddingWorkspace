package com.vote;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * VoteService 职责：
 * - 维护内存中的投票选项列表（线程安全）
 * - 提供投票、新增选项、清空等操作
 * - 启动定时任务，每隔一段时间将内存数据写入本地 JSON 文件
 * - 服务启动时从 JSON 文件恢复数据
 */
public class VoteService {
    private final AtomicReference<List<VoteOption>> optionsRef = new AtomicReference<>(new CopyOnWriteArrayList<>());
    private final FileStore fileStore;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public VoteService(FileStore fileStore) {
        this.fileStore = fileStore;
        // 启动时从文件恢复
        List<VoteOption> restored = fileStore.load();
        if (restored != null && !restored.isEmpty()) {
            optionsRef.set(new CopyOnWriteArrayList<>(restored));
            System.out.println("已从文件恢复 " + restored.size() + " 个投票选项");
        }
        // 每 10 秒持久化一次
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    fileStore.save(new ArrayList<>(optionsRef.get()));
                } catch (Exception e) {
                    System.err.println("定时持久化失败: " + e.getMessage());
                }
            }
        }, 10, 10, TimeUnit.SECONDS);
    }

    /** 获取当前所有选项 */
    public List<VoteOption> getAll() {
        return new ArrayList<>(optionsRef.get());
    }

    /** 给指定 id 的选项加一票，返回操作后的完整列表 */
    public List<VoteOption> vote(String id) {
        List<VoteOption> current = optionsRef.get();
        for (VoteOption opt : current) {
            if (opt.getId().equals(id)) {
                opt.setVotes(opt.getVotes() + 1);
                break;
            }
        }
        persistAsync();
        return new ArrayList<>(current);
    }

    /** 新增一个选项，自动生成 UUID */
    public List<VoteOption> addOption(String name) {
        if (name == null || name.trim().isEmpty()) return getAll();
        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        VoteOption newOpt = new VoteOption(id, name.trim(), 0);
        List<VoteOption> current = optionsRef.get();
        current.add(newOpt);
        persistAsync();
        return new ArrayList<>(current);
    }

    /** 清空所有选项和票数 */
    public List<VoteOption> clearAll() {
        optionsRef.set(new CopyOnWriteArrayList<VoteOption>());
        persistAsync();
        return getAll();
    }

    /** 立即触发一次持久化 */
    private void persistAsync() {
        scheduler.submit(new Runnable() {
            @Override
            public void run() {
                fileStore.save(new ArrayList<>(optionsRef.get()));
            }
        });
    }

    /** 关闭服务时保存并退出调度器 */
    public void shutdown() {
        try {
            fileStore.save(new ArrayList<>(optionsRef.get()));
        } finally {
            scheduler.shutdown();
        }
    }
}
