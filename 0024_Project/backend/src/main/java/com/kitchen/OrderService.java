package com.kitchen;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 订单业务服务
 *  - 维护内存中的订单 Map (ConcurrentHashMap 保证线程安全)
 *  - 提供增删改查 + 状态流转 + 菜品级完成 + 加急 API
 *  - 每次数据变更通过 KitchenSocket.broadcastOrders() 推给所有前端
 *
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
            try {
                int n = Integer.parseInt(o.getId().replaceAll("\\D", ""));
                if (n > maxId) maxId = n;
            } catch (Exception ignored) {}
        }
        seq.set(maxId);
    }

    // ========== 增 ==========
    public Order createOrder(String tableNo, List<DishItem> dishes, String remark, boolean urgent) {
        String id = "ORD-" + seq.incrementAndGet();
        Order order = new Order(id, tableNo, dishes, remark);
        if (urgent) order.setPriority(Order.Priority.HIGH);
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
        // 把还没勾选的菜品也算完成，保证数据一致性
        for (DishItem d : o.getDishes()) if (!d.isDone()) d.setDone(true);
        broadcast();
        return o;
    }

    /** 菜品级：标记/取消某道菜完成。所有菜都完成时自动把订单推进到 DONE */
    public Order toggleDishDone(String orderId, String dishId, boolean done) {
        Order o = orderMap.get(orderId);
        if (o == null) return null;
        for (DishItem d : o.getDishes()) {
            if (d.getId().equals(dishId)) {
                d.setDone(done);
                // 勾选完成时顺便取消"重做"标记（合理默认：完成了就不算重做）
                if (done) d.setRedo(false);
                break;
            }
        }
        o.setUpdatedAt(System.currentTimeMillis());
        // 若所有菜品已完成，自动推进到 DONE
        if (o.getStatus() != Order.Status.DONE && o.allDishesDone()) {
            o.setStatus(Order.Status.DONE);
        }
        broadcast();
        return o;
    }

    /** 加急标记切换 */
    public Order setPriority(String orderId, boolean urgent) {
        Order o = orderMap.get(orderId);
        if (o == null) return null;
        o.setPriority(urgent ? Order.Priority.HIGH : Order.Priority.NORMAL);
        o.setUpdatedAt(System.currentTimeMillis());
        broadcast();
        return o;
    }

    /** 把某个菜品标记为重做（同时取消已完成，因为要重新做） */
    public Order markDishRedo(String orderId, String dishId) {
        Order o = orderMap.get(orderId);
        if (o == null) return null;
        for (DishItem d : o.getDishes()) {
            if (d.getId().equals(dishId)) {
                d.setRedo(true);
                d.setDone(false);     // 重做 => 重置为未完成
                break;
            }
        }
        // 有菜重做了，订单回退到 COOKING
        if (o.getStatus() == Order.Status.DONE) o.setStatus(Order.Status.COOKING);
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
    public List<Order> listAll() { return new ArrayList<>(orderMap.values()); }

    /** 未出餐订单，用于持久化 */
    public List<Order> listPendingOrders() {
        return orderMap.values().stream()
                .filter(o -> o.getStatus() != Order.Status.DONE)
                .collect(Collectors.toList());
    }

    /** 今日所有订单（含已出餐），用于统计面板 */
    public List<Order> listToday() {
        long startOfDay = startOfDay();
        return orderMap.values().stream()
                .filter(o -> o.getCreatedAt() >= startOfDay)
                .collect(Collectors.toList());
    }

    private static long startOfDay() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

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
    private void broadcast() { KitchenSocket.broadcastOrders(listAll()); }

    public String snapshot() { return new com.google.gson.Gson().toJson(listAll()); }
}
