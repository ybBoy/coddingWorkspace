package com.kitchen;

import java.util.ArrayList;
import java.util.List;

/**
 * 订单实体
 * 状态流转: NEW(新订单) -> COOKING(制作中) -> DONE(已出餐)
 * 单个菜品可以通过 DishItem.redo 标记为重做
 * 被 FileStore 持久化，被 OrderService 操作，被 KitchenSocket 广播到前端
 */
public class Order {
    public enum Status { NEW, COOKING, DONE }
    public enum Priority { NORMAL, HIGH }  // 加急标记

    private String id;              // 订单号，如"ORD-1001"
    private String tableNo;         // 桌号，如"A3"、"12"
    private List<DishItem> dishes;  // 多个菜品
    private String remark;          // 订单整体备注
    private Status status;          // NEW / COOKING / DONE
    private Priority priority;      // 普通 / 加急
    private long createdAt;         // 下单时间戳(ms)
    private long updatedAt;         // 最后更新时间戳
    private long finishedAt;        // 整单出餐时间戳(0 表示未完成)

    public Order() {
        this.dishes = new ArrayList<>();
        this.priority = Priority.NORMAL;
        this.finishedAt = 0L;
    }

    public Order(String id, String tableNo, List<DishItem> dishes, String remark) {
        this.id = id;
        this.tableNo = tableNo;
        this.dishes = dishes != null ? dishes : new ArrayList<>();
        this.remark = remark;
        this.status = Status.NEW;
        this.priority = Priority.NORMAL;
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
        this.finishedAt = 0L;
    }

    /** 超过 15 分钟且未完成 => 超时 */
    public boolean isTimeout() {
        if (status == Status.DONE) return false;
        return System.currentTimeMillis() - createdAt > 15 * 60 * 1000L;
    }

    /** 统计菜品总数量 */
    public int totalDishCount() {
        int n = 0;
        for (DishItem d : dishes) n += d.getQuantity();
        return n;
    }

    /** 统计已完成的菜品数量 */
    public int finishedDishCount() {
        int n = 0;
        for (DishItem d : dishes) if (d.isDone()) n += d.getQuantity();
        return n;
    }

    /** 是否所有菜品都已完成（用于自动推进到 DONE） */
    public boolean allDishesDone() {
        if (dishes.isEmpty()) return false;
        for (DishItem d : dishes) if (!d.isDone()) return false;
        return true;
    }

    /** 整单实际完成用时（毫秒），未完成则返回 0 */
    public long cookDurationMs() {
        if (status != Status.DONE || finishedAt == 0) return 0L;
        return finishedAt - createdAt;
    }

    // === Getter / Setter ===
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTableNo() { return tableNo; }
    public void setTableNo(String tableNo) { this.tableNo = tableNo; }
    public List<DishItem> getDishes() { return dishes; }
    public void setDishes(List<DishItem> dishes) { this.dishes = dishes; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) {
        this.status = status;
        if (status == Status.DONE && this.finishedAt == 0) {
            this.finishedAt = System.currentTimeMillis();
        }
    }
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
    public long getFinishedAt() { return finishedAt; }
    public void setFinishedAt(long finishedAt) { this.finishedAt = finishedAt; }
}
