package com.vote;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * VoteState 职责：
 * 保存投票系统的完整状态，包括：
 * - 投票选项列表
 * - 每个 clientId 投给了哪个 optionId（用于改票和高亮）
 * - 投票是否被锁定
 * - 倒计时剩余秒数（<=0 表示未开始或已结束）
 * - 倒计时结束时间戳（用于服务重启后恢复倒计时）
 * 所有字段都是线程安全的容器。
 */
public class VoteState {
    private List<VoteOption> options;
    private Map<String, String> clientVotes; // clientId -> optionId
    private boolean locked;
    private long timerEndTime; // 倒计时结束的毫秒时间戳，0 表示无倒计时

    public VoteState() {
        this.options = new CopyOnWriteArrayList<>();
        this.clientVotes = new ConcurrentHashMap<>();
        this.locked = false;
        this.timerEndTime = 0;
    }

    public List<VoteOption> getOptions() {
        return options;
    }

    public void setOptions(List<VoteOption> options) {
        this.options = new CopyOnWriteArrayList<>(options);
    }

    public Map<String, String> getClientVotes() {
        return clientVotes;
    }

    public void setClientVotes(Map<String, String> clientVotes) {
        this.clientVotes = new ConcurrentHashMap<>(clientVotes);
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public long getTimerEndTime() {
        return timerEndTime;
    }

    public void setTimerEndTime(long timerEndTime) {
        this.timerEndTime = timerEndTime;
    }

    /** 计算当前剩余秒数，<=0 表示未开始或已结束 */
    public int getRemainingSeconds() {
        if (timerEndTime <= 0) return 0;
        long remaining = (timerEndTime - System.currentTimeMillis()) / 1000;
        return remaining > 0 ? (int) remaining : 0;
    }

    /** 深拷贝一份用于广播 */
    public VoteState copy() {
        VoteState copy = new VoteState();
        copy.setOptions(new ArrayList<>(this.options));
        copy.setClientVotes(new HashMap<>(this.clientVotes));
        copy.setLocked(this.locked);
        copy.setTimerEndTime(this.timerEndTime);
        return copy;
    }
}
