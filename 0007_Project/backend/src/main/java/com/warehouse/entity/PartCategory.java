package com.warehouse.entity;

/**
 * 零件分类枚举
 * 定义电动汽车库房中常见的零件分类
 * 用于零件的分类管理和查询
 */
public enum PartCategory {
    /**
     * 电池类零件
     * 如：锂离子电池、磷酸铁锂电池等
     */
    BATTERY("电池"),
    
    /**
     * 电机类零件
     * 如：永磁同步电机、异步电机等
     */
    MOTOR("电机"),
    
    /**
     * 轮胎类零件
     * 如：子午线轮胎、防爆轮胎等
     */
    TIRE("轮胎"),
    
    /**
     * 控制器类零件
     * 如：整车控制器、电机控制器、电池管理系统等
     */
    CONTROLLER("控制器"),
    
    /**
     * 玻璃类零件
     * 如：前挡风玻璃、后挡风玻璃、侧窗玻璃等
     */
    GLASS("玻璃"),
    
    /**
     * 油类零件
     * 如：齿轮油、防冻液、刹车油等
     */
    OIL("机油"),
    
    /**
     * 其他分类
     * 用于未归类或特殊零件
     */
    OTHER("其他");

    /**
     * 分类的中文描述
     */
    private String description;

    /**
     * 构造函数
     * @param description 分类的中文描述
     */
    PartCategory(String description) {
        this.description = description;
    }

    /**
     * 获取分类的中文描述
     * @return 中文描述
     */
    public String getDescription() {
        return description;
    }

    /**
     * 根据中文描述获取对应的枚举值
     * 如果找不到匹配的分类，默认返回 OTHER
     * @param description 中文描述
     * @return 对应的枚举值
     */
    public static PartCategory fromDescription(String description) {
        for (PartCategory category : PartCategory.values()) {
            if (category.getDescription().equals(description)) {
                return category;
            }
        }
        return OTHER;
    }
}
