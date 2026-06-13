package com.kitchen;

/**
 * 菜单条目（常用菜品，供前台快速点选）
 * 被 FileStore 持久化到 backend/data/menu.json，被 MenuService 管理
 */
public class MenuItem {
    private String id;         // UUID
    private String name;       // 菜名，如"红烧牛肉面"
    private String category;   // 分类，如"主食"/"饮品"/"小吃"（可选，用于分组显示）
    private String station;    // 制作工位：如"热菜"/"饮品"/"主食"（下单时自动带入 DishItem）
    private double price;      // 价格（暂不参与计算，仅供展示）
    private int sort;          // 排序号，小的在前
    private boolean enabled;   // 是否启用（下架/上架）

    public MenuItem() {}

    public MenuItem(String id, String name, String category, double price, int sort) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.station = null;
        this.price = price;
        this.sort = sort;
        this.enabled = true;
    }

    // === Getter / Setter ===
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getStation() { return station; }
    public void setStation(String station) { this.station = station; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public int getSort() { return sort; }
    public void setSort(int sort) { this.sort = sort; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
