package com.warehouse.entity;

public enum PartCategory {
    BATTERY("电池"),
    MOTOR("电机"),
    TIRE("轮胎"),
    CONTROLLER("控制器"),
    GLASS("玻璃"),
    OIL("机油"),
    OTHER("其他");

    private String description;

    PartCategory(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static PartCategory fromDescription(String description) {
        for (PartCategory category : PartCategory.values()) {
            if (category.getDescription().equals(description)) {
                return category;
            }
        }
        return OTHER;
    }
}
