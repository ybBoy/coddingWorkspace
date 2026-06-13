package com.kitchen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 菜单服务
 *   内存中维护菜单列表，FileStore 定时刷盘到 menu.json
 *   提供增删改查 API，每次变更通过 KitchenSocket 广播 MENU 消息
 *   被 AppServer 初始化，被 KitchenSocket 的消息处理器调用
 */
public class MenuService {
    private final Map<String, MenuItem> menuMap = new ConcurrentHashMap<>();

    public MenuService() {}

    public void restoreFrom(List<MenuItem> saved) {
        if (saved == null) return;
        for (MenuItem m : saved) menuMap.put(m.getId(), m);
        // 如果是空菜单，写入一些默认的示例菜品，方便直接上手
        if (menuMap.isEmpty()) seedDefault();
    }

    /** 首次启动内置几个常用菜品示例 */
    private void seedDefault() {
        String[][] defaults = new String[][] {
            {"红烧牛肉面", "主食", "28"},
            {"番茄鸡蛋面", "主食", "18"},
            {"酸辣土豆丝", "热菜", "16"},
            {"宫保鸡丁", "热菜", "32"},
            {"可乐", "饮品", "6"},
            {"柠檬茶", "饮品", "10"},
        };
        int sort = 10;
        for (String[] d : defaults) {
            String id = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            MenuItem it = new MenuItem(id, d[0], d[1], Double.parseDouble(d[2]), sort);
            menuMap.put(id, it);
            sort += 10;
        }
        broadcast();
    }

    public List<MenuItem> listAll() {
        List<MenuItem> list = new ArrayList<>(menuMap.values());
        list.sort(Comparator.comparingInt(MenuItem::getSort)
                .thenComparing(MenuItem::getName));
        return list;
    }

    public MenuItem add(MenuItem item) {
        if (item.getId() == null || item.getId().isEmpty()) {
            item.setId(java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        }
        item.setEnabled(true);
        menuMap.put(item.getId(), item);
        broadcast();
        return item;
    }

    public MenuItem update(MenuItem item) {
        if (item.getId() == null || !menuMap.containsKey(item.getId())) return null;
        menuMap.put(item.getId(), item);
        broadcast();
        return item;
    }

    public void remove(String id) {
        menuMap.remove(id);
        broadcast();
    }

    public MenuItem getById(String id) { return menuMap.get(id); }

    private void broadcast() {
        KitchenSocket.broadcastMenu(listAll());
    }

    public String snapshot() {
        return new com.google.gson.Gson().toJson(listAll());
    }
}
