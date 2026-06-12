package com.kitchen;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket 端点（标准 JSR-356 注解，与 AppServer 的注册方式一致）
 *
 *  注意：这里统一使用 javax.websocket.* 标准注解，
 *       配合 AppServer 中 WebSocketServerContainerInitializer.configureContext() 的 JSR-356 注册方式
 *       不再使用 Jetty 原生的 @WebSocket 注解，避免 API 不兼容导致 /ws 无法连接。
 *
 *  - 维护所有已连接的前端 Session
 *  - 接收前端消息（新增订单、开始制作、完成、重做标记等）并转发给 OrderService
 *  - OrderService 每次变更会回调 broadcastOrders() 推送给所有前端
 *  - 内置一个心跳线程，每 30 秒广播一次全量数据（防止偶发消息丢失导致前后端不一致）
 *
 *  消息协议（全 JSON 文本帧）：
 *  前端 -> 后端:
 *    { "type": "CREATE", "tableNo":"A3", "dishes":[...], "remark":"..." }
 *    { "type": "START",  "orderId":"ORD-1001" }
 *    { "type": "FINISH", "orderId":"ORD-1001" }
 *    { "type": "REDO",   "orderId":"ORD-1001", "dishId":"xxx" }
 *    { "type": "UNREDO", "orderId":"ORD-1001", "dishId":"xxx" }
 *    { "type": "PING" }
 *  后端 -> 前端:
 *    { "type": "ORDERS", "data": [ ... ] }   全量订单列表
 *    { "type": "PONG" }
 */
@ServerEndpoint("/ws")
public class KitchenSocket {
    private static final Queue<Session> sessions = new ConcurrentLinkedQueue<>();
    private static OrderService orderService;
    private static final Gson gson = new Gson();
    private static ScheduledExecutorService heartBeat;

    public static void setOrderService(OrderService svc) { orderService = svc; }

    /** 启动心跳：每 30 秒强制广播一次全量数据 */
    public static void startHeartBeat() {
        if (heartBeat != null) return;
        heartBeat = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ws-heartbeat");
            t.setDaemon(true);
            return t;
        });
        heartBeat.scheduleAtFixedRate(() -> {
            if (orderService != null) broadcastOrders(orderService.listAll());
        }, 30, 30, TimeUnit.SECONDS);
    }

    @OnOpen
    public void onConnect(Session session) {
        sessions.add(session);
        // 新连接上来立即推一次全量数据
        if (orderService != null) {
            send(session, wrap("ORDERS", orderService.listAll()));
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        sessions.remove(session);
    }

    @OnError
    public void onError(Session session, Throwable t) {
        sessions.remove(session);
    }

    @OnMessage
    public void onMessage(Session session, String msg) {
        try {
            JsonObject json = new JsonParser().parse(msg).getAsJsonObject();
            String type = json.get("type").getAsString();
            switch (type) {
                case "CREATE": {
                    String tableNo = json.has("tableNo") ? json.get("tableNo").getAsString() : "";
                    String remark = json.has("remark") ? json.get("remark").getAsString() : "";
                    List<DishItem> dishes = gson.fromJson(json.get("dishes"),
                            new com.google.gson.reflect.TypeToken<List<DishItem>>() {}.getType());
                    if (dishes == null || dishes.isEmpty()) return;
                    // 给每个菜品补一个 UUID
                    for (DishItem d : dishes) {
                        if (d.getId() == null || d.getId().isEmpty()) {
                            d.setId(java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 12));
                        }
                    }
                    orderService.createOrder(tableNo, dishes, remark);
                    break;
                }
                case "START": {
                    String oid = json.get("orderId").getAsString();
                    orderService.startCooking(oid);
                    break;
                }
                case "FINISH": {
                    String oid = json.get("orderId").getAsString();
                    orderService.finishOrder(oid);
                    break;
                }
                case "REDO": {
                    String oid = json.get("orderId").getAsString();
                    String did = json.get("dishId").getAsString();
                    orderService.markDishRedo(oid, did);
                    break;
                }
                case "UNREDO": {
                    String oid = json.get("orderId").getAsString();
                    String did = json.get("dishId").getAsString();
                    orderService.unmarkDishRedo(oid, did);
                    break;
                }
                case "PING": {
                    send(session, "{\"type\":\"PONG\"}");
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** 广播全量订单（被 OrderService 在每次变更后调用） */
    public static void broadcastOrders(List<Order> orders) {
        String payload = wrap("ORDERS", orders);
        for (Session s : sessions) {
            if (s.isOpen()) send(s, payload);
        }
    }

    private static String wrap(String type, Object data) {
        JsonObject obj = new JsonObject();
        obj.addProperty("type", type);
        obj.add("data", new JsonParser().parse(gson.toJson(data)));
        return gson.toJson(obj);
    }

    private static void send(Session s, String text) {
        try {
            // JSR-356 异步发送，等价于之前 Jetty 原生的 sendStringByFuture
            s.getAsyncRemote().sendText(text);
        } catch (Exception e) {
            // ignore
        }
    }
}
