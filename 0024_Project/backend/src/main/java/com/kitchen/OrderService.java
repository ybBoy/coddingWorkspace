package com.kitchen;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 订单业务服务
 *  - 维护内存中的订单 Map (ConcurrentHashMap 保证线程安全)
 *  - 提供增删改查 + 状态流转 API
 *  - 每次数据变更通过 KitchenSocket.broadcast() 推给所有前端
 *  被 AppServer 初始化，被 KitchenSocket 里的消息处理器调用
 */
public class OrderService {
    private final Map<String, Order> orderMap = new ConcurrentHashMap<>();
    private final AtomicInteger seq = new AtomicInteger(1000);

    public OrderService() {}

    /** 启动时从 FileStore 恢复未完成订单 */
    public void restoreFrom(List<Order> savedOrders) {
        if (savedOrders == null) return;
        int maxId = 1000;
        for (Order o : savedOrders) {
            orderMap.put(o.getId(), o);
            // 根据已有订单号推断下一个自增序号
            try {
                int n = Integer.parseInt(o.getId().replaceAll("\\D", ""));
                if (n > maxId) maxId = n;
            } catch (Exception ignored) {}
        }
        seq.set(maxId);
    }

    // ========== 增 ==========
    public Order createOrder(String tableNo, List<DishItem> dishes, String remark) {
        String id = "ORD-" + seq.incrementAndGet();
        Order order = new Order(id, tableNo, dishes, remark);
        orderMap.put(id, order);
        broadcast();
        return order;
    }

    // ========== 状态流转 ==========
    public Order startCooking(String orderId) {
        Order o = orderMap.get(orderId);
        if (o == null || o.getStatus() != Order.Status.NEW) return o;
        o.setStatus(Order.Status.COOKING);
        o.setUpdatedAt(System.currentTimeMillis());
        broadcast();
        return o;
    }

    public Order finishOrder(String orderId) {
        Order o = orderMap.get(orderId);
        if (o == null || o.getStatus() == Order.Status.DONE) return o;
        o.setStatus(Order.Status.DONE);
        o.setUpdatedAt(System.currentTimeMillis());
        // 已出餐可以立即从内存中移除（也可以保留一段时间，这里简单起见保留 10 分钟由前端自己过滤）
        broadcast();
        return o;
    }

    /** 把某个菜品标记为重做（不改变订单整体状态） */
    public Order markDishRedo(String orderId, String dishId) {
        Order o = orderMap.get(orderId);
        if (o == null) return null;
        for (DishItem d : o.getDishes()) {
            if (d.getId().equals(dishId)) {
                d.setRedo(true);
                break;
            }
        }
        o.setUpdatedAt(System.currentTimeMillis());
        broadcast();
        return o;
    }

    /** 取消重做标记 */
    public Order unmarkDishRedo(String orderId, String dishId) {
        Order o = orderMap.get(orderId);
        if (o == null) return null;
        for (DishItem d : o.getDishes()) {
            if (d.getId().equals(dishId)) {
                d.setRedo(false);
                break;
            }
        }
        o.setUpdatedAt(System.currentTimeMillis());
        broadcast();
        return o;
    }

    // ========== 查 ==========
    public List<Order> listAll() {
        return new ArrayList<>(orderMap.values());
    }

    /** 列出所有未出餐的订单（NEW+COOKING），用于 FileStore 持久化 */
    public List<Order> listPendingOrders() {
        return orderMap.values().stream()
                .filter(o -> o.getStatus() != Order.Status.DONE)
                .collect(Collectors.toList());
    }

    /** 搜索：按订单号或桌号模糊匹配 */
    public List<Order> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return listAll();
        String k = keyword.trim().toLowerCase();
        return orderMap.values().stream()
                .filter(o -> o.getId().toLowerCase().contains(k)
                          || (o.getTableNo() != null && o.getTableNo().toLowerCase().contains(k)))
                .collect(Collectors.toList());
    }

    public Order getById(String id) { return orderMap.get(id); }

    // ========== 广播 ==========
    private void broadcast() {
        KitchenSocket.broadcastOrders(listAll());
    }

    /** 用于前端刚连接上时主动拉一次全量数据 */
    public String snapshot() {
        return new com.google.gson.Gson().toJson(listAll());
    }
}
