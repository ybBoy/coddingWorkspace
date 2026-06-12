package com.vote;

import java.util.*;
import java.util.concurrent.*;

/**
 * VoteService 职责：
 * - 维护 VoteState 完整状态（线程安全）
 * - 提供投票/改票逻辑：记录 clientId 与 optionId 的关系，已投过则旧减1新加1
 * - 管理员校验：通过 adminToken 验证密码
 * - 管理员操作：删除选项、重命名选项、锁定/解锁投票
 * - 倒计时功能：设置时长、每秒广播剩余时间、到期自动锁定
 * - 启动定时任务：每 10 秒持久化到文件，每秒更新倒计时
 * - 服务启动时从 JSON 文件恢复完整状态
 */
public class VoteService {
    private final VoteState state;
    private final FileStore fileStore;
    private final String adminPassword;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private ScheduledFuture<?> timerTask = null;

    // 广播回调，由 VoteWebSocket 设置
    public interface BroadcastCallback {
        void broadcast(VoteState state);
    }
    private BroadcastCallback broadcastCallback;

    public VoteService(FileStore fileStore, String adminPassword) {
        this.fileStore = fileStore;
        this.adminPassword = adminPassword;
        // 启动时从文件恢复
        VoteState restored = fileStore.loadState();
        this.state = restored;

        // 恢复倒计时任务：如果结束时间还未到
        if (state.getTimerEndTime() > System.currentTimeMillis()) {
            startTimerTask();
        } else if (state.getTimerEndTime() > 0) {
            // 倒计时在服务停机期间已过期，恢复为"已结束/已锁定"状态
            state.setTimerEndTime(0);
            state.setLocked(true);
            System.out.println("倒计时已在停机期间结束，投票已恢复为锁定状态");
        } else {
            state.setTimerEndTime(0);
        }

        // 每 10 秒持久化一次
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    fileStore.saveState(state.copy());
                } catch (Exception e) {
                    System.err.println("定时持久化失败: " + e.getMessage());
                }
            }
        }, 10, 10, TimeUnit.SECONDS);
    }

    public void setBroadcastCallback(BroadcastCallback callback) {
        this.broadcastCallback = callback;
    }

    private void broadcast() {
        if (broadcastCallback != null) {
            broadcastCallback.broadcast(state.copy());
        }
    }

    private void persistAsync() {
        scheduler.submit(new Runnable() {
            @Override
            public void run() {
                fileStore.saveState(state.copy());
            }
        });
    }

    // ---------- 公共查询方法 ----------

    public VoteState getState() {
        return state.copy();
    }

    public boolean isAdmin(String token) {
        return adminPassword.equals(token);
    }

    // ---------- 普通用户操作 ----------

    /**
     * 投票或改票：
     * - 如果用户已投给相同选项：不做操作
     * - 如果用户已投给其他选项：旧选项-1，新选项+1
     * - 如果用户没投过：新选项+1
     * - 锁定状态下不允许投票
     */
    public synchronized VoteState vote(String clientId, String optionId) {
        if (state.isLocked()) {
            return state.copy();
        }
        if (clientId == null || optionId == null) return state.copy();

        String currentVote = state.getClientVotes().get(clientId);
        if (optionId.equals(currentVote)) {
            return state.copy(); // 相同选项不操作
        }

        // 找到新旧选项
        VoteOption oldOpt = null;
        VoteOption newOpt = null;
        for (VoteOption opt : state.getOptions()) {
            if (opt.getId().equals(currentVote)) oldOpt = opt;
            if (opt.getId().equals(optionId)) newOpt = opt;
        }
        if (newOpt == null) return state.copy();

        // 改票逻辑
        if (oldOpt != null) {
            oldOpt.setVotes(oldOpt.getVotes() - 1);
        }
        newOpt.setVotes(newOpt.getVotes() + 1);
        state.getClientVotes().put(clientId, optionId);

        persistAsync();
        broadcast();
        return state.copy();
    }

    /** 新增选项，任何人都可以，但锁定状态下不允许 */
    public synchronized VoteState addOption(String name) {
        if (state.isLocked()) {
            return state.copy();
        }
        if (name == null || name.trim().isEmpty()) return state.copy();
        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        VoteOption newOpt = new VoteOption(id, name.trim(), 0);
        state.getOptions().add(newOpt);
        persistAsync();
        broadcast();
        return state.copy();
    }

    // ---------- 管理员操作 ----------

    /** 管理员登录验证，返回 token（其实就是密码本身，简单实现） */
    public boolean adminLogin(String password) {
        return adminPassword.equals(password);
    }

    /** 清空所有选项和投票记录 */
    public synchronized VoteState clearAll(String adminToken) {
        if (!isAdmin(adminToken)) return state.copy();
        state.getOptions().clear();
        state.getClientVotes().clear();
        state.setLocked(false);
        stopTimer();
        state.setTimerEndTime(0);
        persistAsync();
        broadcast();
        return state.copy();
    }

    /** 删除指定选项 */
    public synchronized VoteState deleteOption(String adminToken, String optionId) {
        if (!isAdmin(adminToken)) return state.copy();
        if (optionId == null) return state.copy();

        // 移除选项（CopyOnWriteArrayList 不支持 iterator.remove，重建列表）
        List<VoteOption> newOptions = new ArrayList<>();
        for (VoteOption opt : state.getOptions()) {
            if (!opt.getId().equals(optionId)) {
                newOptions.add(opt);
            }
        }
        state.setOptions(newOptions);

        // 移除相关投票记录
        Iterator<Map.Entry<String, String>> cvIt = state.getClientVotes().entrySet().iterator();
        while (cvIt.hasNext()) {
            if (cvIt.next().getValue().equals(optionId)) {
                cvIt.remove();
            }
        }
        persistAsync();
        broadcast();
        return state.copy();
    }

    /** 重命名指定选项 */
    public synchronized VoteState renameOption(String adminToken, String optionId, String newName) {
        if (!isAdmin(adminToken)) return state.copy();
        if (optionId == null || newName == null || newName.trim().isEmpty()) return state.copy();

        for (VoteOption opt : state.getOptions()) {
            if (opt.getId().equals(optionId)) {
                opt.setName(newName.trim());
                break;
            }
        }
        persistAsync();
        broadcast();
        return state.copy();
    }

    /** 锁定或解锁投票 */
    public synchronized VoteState setLocked(String adminToken, boolean locked) {
        if (!isAdmin(adminToken)) return state.copy();
        state.setLocked(locked);
        persistAsync();
        broadcast();
        return state.copy();
    }

    // ---------- 倒计时功能 ----------

    /** 设置倒计时（秒），开始倒计时 */
    public synchronized VoteState setTimer(String adminToken, int seconds) {
        if (!isAdmin(adminToken)) return state.copy();
        if (seconds <= 0) {
            stopTimer();
            state.setTimerEndTime(0);
            persistAsync();
            broadcast();
            return state.copy();
        }
        state.setTimerEndTime(System.currentTimeMillis() + seconds * 1000L);
        state.setLocked(false); // 开始投票前先解锁
        startTimerTask();
        persistAsync();
        broadcast();
        return state.copy();
    }

    private void startTimerTask() {
        stopTimer();
        timerTask = scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    int remaining = state.getRemainingSeconds();
                    if (remaining <= 0) {
                        // 倒计时结束，自动锁定
                        synchronized (VoteService.this) {
                            if (state.getTimerEndTime() > 0 && state.getRemainingSeconds() <= 0) {
                                state.setTimerEndTime(0);
                                state.setLocked(true);
                                stopTimer();
                                persistAsync();
                                broadcast();
                                System.out.println("倒计时结束，投票已自动锁定");
                            }
                        }
                    } else {
                        // 每秒广播一次剩余时间
                        broadcast();
                    }
                } catch (Exception e) {
                    System.err.println("倒计时任务异常: " + e.getMessage());
                }
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    private void stopTimer() {
        if (timerTask != null) {
            timerTask.cancel(false);
            timerTask = null;
        }
    }

    /** 关闭服务时保存并退出调度器 */
    public void shutdown() {
        try {
            stopTimer();
            fileStore.saveState(state.copy());
        } finally {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
            }
        }
    }
}
