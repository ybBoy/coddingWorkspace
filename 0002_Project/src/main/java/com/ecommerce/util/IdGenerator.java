package com.ecommerce.util;

import java.util.UUID;

/**
 * ID生成器工具类
 * 使用工厂模式生成唯一ID
 */
public class IdGenerator {

    /**
     * 生成UUID
     * @return 32位UUID字符串
     */
    public static String generateUUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 生成带前缀的ID
     * @param prefix 前缀
     * @return 带前缀的ID
     */
    public static String generateId(String prefix) {
        return prefix + generateUUID();
    }

    /**
     * 生成订单编号
     * 格式：ORD + 时间戳 + 随机数
     * @return 订单编号
     */
    public static String generateOrderNo() {
        long timestamp = System.currentTimeMillis();
        int random = (int) (Math.random() * 10000);
        return String.format("ORD%d%04d", timestamp, random);
    }

    /**
     * 生成商品ID
     * @return 商品ID
     */
    public static String generateProductId() {
        return "PRD" + generateUUID().substring(0, 8);
    }

    /**
     * 生成购物车项ID
     * @return 购物车项ID
     */
    public static String generateCartItemId() {
        return "CIT" + generateUUID().substring(0, 8);
    }

    /**
     * 生成订单项ID
     * @return 订单项ID
     */
    public static String generateOrderItemId() {
        return "OIT" + generateUUID().substring(0, 8);
    }
}
