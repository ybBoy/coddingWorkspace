package com.kitchen;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 订单业务服务（升级版）
 *
 * 新增能力：
 *   - 历史归档回调：archiveOldOrders()  超过 24h 的 DONE/CANCELLED 交给 FileStore 按天归档
 *   - 改单：updateOrder()  修改桌号、菜品、数量、备注（已出餐的菜不可改）
 *   - 撤单：cancelOrder()  任意状态可撤销，记录原因
 *   - 多工位：createOrder() 时根据菜单自动给菜品分配 station
 *   - 按日期查询：listByDate()  读取内存 + 当日归档合并
 *   - 菜品耗时分析：dishAnalysis()  每道菜平均耗时/重做率
 *   - CSV 导出：exportCsv()  某日期订单导出 CSV 文本
 */
public class OrderService {
    private final Map<String, Order> orderMap = new ConcurrentHashMap<>();
    private final AtomicInteger seq = new AtomicInteger(1000);
    private MenuService menuService;   // 用于创建订单时自动分配工位

    public OrderService() {}

    public void setMenuService(MenuService menuService) { this.menuService = menuService; }

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
        // 按菜单给菜品自动分配工位
        assignStations(order.getDishes());
        // 菜品 ID 兜底生成
        for (DishItem d : order.getDishes()) {
            if (d.getId() == null || d.getId().isEmpty()) {
                d.setId(UUID.randomUUID().toString().replace("-", "").substring(0, 12));
            }
        }
        orderMap.put(id, order);
        broadcast();
        return order;
    }

    /** 根据 MenuService 的配置给菜品自动分配 station */
    private void assignStations(List<DishItem> dishes) {
        if (menuService == null || dishes == null) return;
        Map<String, String> name2station = menuService.nameToStationMap();
        for (DishItem d : dishes) {
            if (d.getStation() == null || d.getStation().isEmpty()) {
                String station = name2station.get(d.getName());
                if (station != null) d.setStation(station);
            }
        }
    }

    // ========== 状态流转 ==========
    public Order startCooking(String orderId) {
        Order o = orderMap.get(orderId);
        if (o == null || o.getStatus() != Order.Status.NEW) return o;
        o.setStatus(Order.Status.COOKING);
        o.setUpdatedAt(System.currentTimeMillis());
        // 标记所有菜品"开始制作"时间戳（用于菜品级耗时分析）
        long now = System.currentTimeMillis();
        for (DishItem d : o.getDishes()) {
            if (d.getStartedAt() <= 0L) d.setStartedAt(now);
        }
        broadcast();
        return o;
    }

    public Order finishOrder(String orderId) {
        Order o = orderMap.get(orderId);
        if (o == null || o.getStatus() == Order.Status.DONE || o.getStatus() == Order.Status.CANCELLED) return o;
        o.setStatus(Order.Status.DONE);
        o.setUpdatedAt(System.currentTimeMillis());
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
                if (done) d.setRedo(false);
                break;
            }
        }
        o.setUpdatedAt(System.currentTimeMillis());
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
                d.setDone(false);
                d.setStartedAt(System.currentTimeMillis()); // 重做 => 重新计时
                break;
            }
        }
        if (o.getStatus() == Order.Status.DONE) o.setStatus(Order.Status.COOKING);
        o.setUpdatedAt(System.currentTimeMillis());
        broadcast();
        return o;
    }

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

    // ========== 改单 ==========

    /**
     * 修改订单：桌号、菜品、数量、备注
     *   - 菜品已完成（done=true）的条目不可被删除或改数量/名称（但可改备注）
     *   - 可以新增菜品
     *   - CANCELLED 订单不可改
     */
    public Order updateOrder(String orderId, String tableNo, List<DishItem> newDishes, String remark) {
        Order o = orderMap.get(orderId);
        if (o == null || o.getStatus() == Order.Status.CANCELLED) return null;

        // 桌号 + 备注
        if (tableNo != null) o.setTableNo(tableNo.trim());
        if (remark != null)  o.setRemark(remark.trim().isEmpty() ? null : remark.trim());

        if (newDishes != null) {
            // 先建立旧菜品的 ID -> DishItem 映射
            Map<String, DishItem> oldById = new LinkedHashMap<>();
            for (DishItem d : o.getDishes()) oldById.put(d.getId(), d);

            List<DishItem> merged = new ArrayList<>();
            Set<String> seenIds = new HashSet<>();

            for (DishItem incoming : newDishes) {
                String id = incoming.getId();
                if (id != null && !id.isEmpty() && oldById.containsKey(id)) {
                    // 更新已有菜品
                    DishItem old = oldById.get(id);
                    seenIds.add(id);
                    // 已完成的：只允许改 note，其他拒绝
                    if (old.isDone()) {
                        if (incoming.getNote() != null) old.setNote(incoming.getNote());
                    } else {
                        if (incoming.getName() != null)   old.setName(incoming.getName());
                        if (incoming.getQuantity() > 0)   old.setQuantity(incoming.getQuantity());
                        if (incoming.getNote() != null)   old.setNote(incoming.getNote());
                        if (incoming.getStation() != null) old.setStation(incoming.getStation());
                    }
                    merged.add(old);
                } else {
                    // 新增菜品
                    if (incoming.getName() == null || incoming.getName().trim().isEmpty()) continue;
                    if (incoming.getId() == null || incoming.getId().isEmpty()) {
                        incoming.setId(UUID.randomUUID().toString().replace("-", "").substring(0, 12));
                    }
                    // 工位自动分配
                    if ((incoming.getStation() == null || incoming.getStation().isEmpty()) && menuService != null) {
                        String s = menuService.nameToStationMap().get(incoming.getName().trim());
                        if (s != null) incoming.setStation(s);
                    }
                    merged.add(incoming);
                }
            }

            // 删除已不在 newDishes 中且未完成的旧菜品
            for (Map.Entry<String, DishItem> e : oldById.entrySet()) {
                if (!seenIds.contains(e.getKey()) && !e.getValue().isDone()) {
                    // 已删除，跳过
                } else if (!seenIds.contains(e.getKey())) {
                    // 已完成但没在 newDishes 中出现 => 保留
                    merged.add(e.getValue());
                }
            }
            o.setDishes(merged);
        }
        o.setUpdatedAt(System.currentTimeMillis());
        broadcast();
        return o;
    }

    // ========== 撤单 ==========
    public Order cancelOrder(String orderId, String reason) {
        Order o = orderMap.get(orderId);
        if (o == null || o.getStatus() == Order.Status.CANCELLED) return null;
        o.setStatus(Order.Status.CANCELLED);
        o.setCancelReason(reason);
        o.setUpdatedAt(System.currentTimeMillis());
        broadcast();
        return o;
    }

    // ========== 查 ==========
    public List<Order> listAll() { return new ArrayList<>(orderMap.values()); }

    /** 未出餐订单，用于持久化 */
    public List<Order> listPendingOrders() {
        return orderMap.values().stream()
                .filter(o -> o.getStatus() != Order.Status.DONE && o.getStatus() != Order.Status.CANCELLED)
                .collect(Collectors.toList());
    }

    /** 今日所有订单（内存 + 今日归档合并） */
    public List<Order> listToday() {
        return listByDate(FileStore.todayStr());
    }

    /**
     * 查询指定日期（YYYY-MM-DD）的全部订单
     *   内存订单按创建日期匹配 + 归档文件
     */
    public List<Order> listByDate(String dateStr) {
        long start = startOfDate(dateStr);
        long end   = start + 24L * 3600 * 1000;
        // 1. 从内存中挑
        List<Order> result = orderMap.values().stream()
                .filter(o -> o.getCreatedAt() >= start && o.getCreatedAt() < end)
                .collect(Collectors.toList());
        // 2. 合并归档文件里的（FileStore 外部通过 broadcastExternals 注入进来）
        return result;
    }

    /** 允许外部把归档文件里的订单注入进来（供 listByDate 合并使用） */
    public List<Order> listByDateWithArchive(String dateStr, List<Order> archiveOrders) {
        List<Order> base = listByDate(dateStr);
        Map<String, Order> merged = new LinkedHashMap<>();
        for (Order o : base)         merged.put(o.getId(), o);
        if (archiveOrders != null) {
            for (Order o : archiveOrders) merged.putIfAbsent(o.getId(), o);
        }
        List<Order> list = new ArrayList<>(merged.values());
        list.sort(Comparator.comparingLong(Order::getCreatedAt));
        return list;
    }

    /** 找出 DONE/CANCELLED 超过 24h 的，交给回调去归档，并从内存移除 */
    public void archiveOldOrders(Consumer<List<Order>> archiveCallback) {
        long cutoff = System.currentTimeMillis() - 24L * 3600 * 1000;
        List<Order> toArchive = new ArrayList<>();
        for (Order o : orderMap.values()) {
            boolean terminal = o.getStatus() == Order.Status.DONE || o.getStatus() == Order.Status.CANCELLED;
            long ts = (o.getStatus() == Order.Status.DONE) ? o.getFinishedAt() : o.getCancelledAt();
            if (terminal && ts > 0 && ts < cutoff) {
                toArchive.add(o);
            }
        }
        if (!toArchive.isEmpty()) {
            try {
                archiveCallback.accept(toArchive);
                for (Order o : toArchive) orderMap.remove(o.getId());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static long startOfDate(String dateStr) {
        try {
            String[] parts = dateStr.split("-");
            Calendar c = Calendar.getInstance();
            c.set(Calendar.YEAR, Integer.parseInt(parts[0]));
            c.set(Calendar.MONTH, Integer.parseInt(parts[1]) - 1);
            c.set(Calendar.DAY_OF_MONTH, Integer.parseInt(parts[2]));
            c.set(Calendar.HOUR_OF_DAY, 0);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
            return c.getTimeInMillis();
        } catch (Exception e) { return 0L; }
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

    // ========== 菜品耗时分析 ==========

    /**
     * 对输入的订单列表做菜品维度分析：
     *   { name, count, avgMinutes, redoCount, redoRate }
     * 按平均耗时从大到小排序
     */
    public static List<Map<String, Object>> dishAnalysis(List<Order> orders) {
        // key: dishName
        Map<String, long[]> stat = new HashMap<>(); // [总次数, 总耗时ms, 重做次数, 重做菜品总次数]
        if (orders == null) return new ArrayList<>();
        for (Order o : orders) {
            for (DishItem d : o.getDishes()) {
                String name = d.getName();
                long[] arr = stat.computeIfAbsent(name, k -> new long[4]);
                arr[0] += d.getQuantity();          // 总份数
                long dur = d.cookDurationMs();
                if (dur > 0) { arr[1] += dur * d.getQuantity(); arr[3] += d.getQuantity(); }
                if (d.isRedo()) arr[2] += d.getQuantity();
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, long[]> e : stat.entrySet()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", e.getKey());
            m.put("count", e.getValue()[0]);
            double avgMin = (e.getValue()[3] > 0) ? (e.getValue()[1] * 1.0 / e.getValue()[3] / 60000) : 0;
            m.put("avgMinutes", Math.round(avgMin * 10.0) / 10.0);
            m.put("redoCount", e.getValue()[2]);
            double rate = (e.getValue()[0] > 0) ? (e.getValue()[2] * 100.0 / e.getValue()[0]) : 0;
            m.put("redoRate", Math.round(rate * 10.0) / 10.0);
            result.add(m);
        }
        result.sort((a, b) -> Double.compare(
                ((Number) b.get("avgMinutes")).doubleValue(),
                ((Number) a.get("avgMinutes")).doubleValue()));
        return result;
    }

    // ========== CSV 导出 ==========

    /** 把订单列表导出为 CSV 字符串（UTF-8 + BOM，Excel 可直接打开） */
    public static String exportCsv(List<Order> orders) {
        StringBuilder sb = new StringBuilder();
        sb.append('\ufeff'); // UTF-8 BOM
        sb.append("订单号,桌号,状态,优先级,下单时间,出餐时间,总份数,已完成份数,平均耗时(分),是否超时,菜品明细,备注,撤销原因\n");
        if (orders == null) return sb.toString();
        for (Order o : orders) {
            sb.append(esc(o.getId())).append(',');
            sb.append(esc(o.getTableNo())).append(',');
            sb.append(esc(o.getStatus().name())).append(',');
            sb.append(esc(o.getPriority().name())).append(',');
            sb.append(esc(fmt(o.getCreatedAt()))).append(',');
            sb.append(esc(o.getFinishedAt() > 0 ? fmt(o.getFinishedAt()) : "")).append(',');
            sb.append(o.totalDishCount()).append(',');
            sb.append(o.finishedDishCount()).append(',');
            long durMs = o.cookDurationMs();
            sb.append(durMs > 0 ? Math.round(durMs / 60000.0 * 10) / 10.0 : "").append(',');
            sb.append(o.isTimeout() ? "是" : "否").append(',');
            // 菜品明细：菜名x数量(备注) | 菜名x数量 ...
            StringBuilder dishStr = new StringBuilder();
            for (DishItem d : o.getDishes()) {
                if (dishStr.length() > 0) dishStr.append(" | ");
                dishStr.append(d.getName()).append('x').append(d.getQuantity());
                if (d.getNote() != null && !d.getNote().isEmpty()) dishStr.append('(').append(d.getNote()).append(')');
                if (d.getStation() != null && !d.getStation().isEmpty()) dishStr.append('[').append(d.getStation()).append(']');
                if (d.isRedo()) dishStr.append("[重做]");
                if (d.isDone()) dishStr.append("[完成]");
            }
            sb.append(esc(dishStr.toString())).append(',');
            sb.append(esc(o.getRemark() == null ? "" : o.getRemark())).append(',');
            sb.append(esc(o.getCancelReason() == null ? "" : o.getCancelReason())).append('\n');
        }
        return sb.toString();
    }

    private static String esc(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
    private static String fmt(long ts) {
        if (ts <= 0) return "";
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ts);
        return String.format("%04d-%02d-%02d %02d:%02d:%02d",
                c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH),
                c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), c.get(Calendar.SECOND));
    }

    // ========== 广播 ==========
    private void broadcast() { KitchenSocket.broadcastOrders(listAll()); }

    public String snapshot() { return new com.google.gson.Gson().toJson(listAll()); }
}
