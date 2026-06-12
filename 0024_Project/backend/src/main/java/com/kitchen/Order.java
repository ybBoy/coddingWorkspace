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

    private String id;              // 订单号，如"ORD-1001"
    private String tableNo;         // 桌号，如"A3"、"12"
    private List<DishItem> dishes;  // 多个菜品
    private String remark;          // 订单整体备注
    private Status status;          // NEW / COOKING / DONE
    private long createdAt;         // 下单时间戳(ms)
    private long updatedAt;         // 最后更新时间戳

    public Order() {
        this.dishes = new ArrayList<>();
    }

    public Order(String id, String tableNo, List<DishItem> dishes, String remark) {
        this.id = id;
        this.tableNo = tableNo;
        this.dishes = dishes != null ? dishes : new ArrayList<>();
        this.remark = remark;
        this.status = Status.NEW;
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
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
    public void setStatus(Status status) { this.status = status; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
