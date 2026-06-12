package com.kitchen;

/**
 * 菜品条目
 * 一个订单(Order)包含多个 DishItem
 * 被 FileStore 序列化到 JSON，被 OrderService 管理状态
 */
public class DishItem {
    private String id;          // 菜品唯一ID (UUID)
    private String name;        // 菜品名称，如"红烧牛肉面"
    private int quantity;       // 数量
    private String note;        // 单条备注，如"少辣"
    private boolean redo;       // 是否标记为重做（橙色提示）

    public DishItem() {}

    public DishItem(String id, String name, int quantity, String note) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
        this.note = note;
        this.redo = false;
    }

    // === Getter / Setter ===
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public boolean isRedo() { return redo; }
    public void setRedo(boolean redo) { this.redo = redo; }
}
