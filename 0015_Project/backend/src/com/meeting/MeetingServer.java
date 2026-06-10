package com.meeting;

import com.meeting.model.Booking;
import com.meeting.model.MeetingRoom;
import com.meeting.model.User;
import com.meeting.service.BookingService;
import com.meeting.util.DataStore;
import com.meeting.util.JsonUtil;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

public class MeetingServer {

    private static final int PORT = 8088;
    private static BookingService bookingService;

    public static void main(String[] args) throws Exception {
        String dataDir = "backend/data";
        if (args.length > 0) dataDir = args[0];
        DataStore.getInstance(dataDir);
        bookingService = new BookingService();

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        server.createContext("/api/users", new UsersHandler());
        server.createContext("/api/user", new UserHandler());
        server.createContext("/api/rooms", new RoomsHandler());
        server.createContext("/api/bookings", new BookingsHandler());
        server.createContext("/", new StaticFileHandler());

        server.setExecutor(null);
        server.start();
        System.out.println("会议室预订系统启动成功: http://localhost:" + PORT);
    }

    private static void sendCorsHeaders(HttpExchange t) {
        Headers h = t.getResponseHeaders();
        h.add("Access-Control-Allow-Origin", "*");
        h.add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        h.add("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private static String readBody(HttpExchange t) throws IOException {
        InputStream is = t.getRequestBody();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = is.read(buf)) > 0) {
            baos.write(buf, 0, n);
        }
        return new String(baos.toByteArray(), StandardCharsets.UTF_8);
    }

    private static void sendJson(HttpExchange t, String json, int code) throws IOException {
        sendCorsHeaders(t);
        t.getResponseHeaders().add("Content-Type", "application/json;charset=utf-8");
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        t.sendResponseHeaders(code, data.length);
        OutputStream os = t.getResponseBody();
        os.write(data);
        os.close();
    }

    static class UsersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if ("OPTIONS".equals(t.getRequestMethod())) { sendCorsHeaders(t); t.sendResponseHeaders(204, -1); return; }
            try {
                if ("GET".equals(t.getRequestMethod())) {
                    List<User> users = DataStore.getInstance().getUsers();
                    sendJson(t, JsonUtil.wrapSuccess(users), 200);
                } else {
                    sendJson(t, JsonUtil.wrapError("不支持的方法"), 405);
                }
            } catch (Exception e) {
                sendJson(t, JsonUtil.wrapError(e.getMessage()), 500);
            }
        }
    }

    static class UserHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if ("OPTIONS".equals(t.getRequestMethod())) { sendCorsHeaders(t); t.sendResponseHeaders(204, -1); return; }
            try {
                if ("GET".equals(t.getRequestMethod())) {
                    String query = t.getRequestURI().getQuery();
                    String userId = null;
                    if (query != null) {
                        for (String kv : query.split("&")) {
                            String[] parts = kv.split("=", 2);
                            if (parts.length == 2 && "id".equals(parts[0])) {
                                userId = parts[1];
                            }
                        }
                    }
                    User user = null;
                    if (userId != null) user = DataStore.getInstance().getUserById(userId);
                    if (user != null) {
                        sendJson(t, JsonUtil.wrapSuccess(user), 200);
                    } else {
                        sendJson(t, JsonUtil.wrapError("用户不存在"), 404);
                    }
                } else {
                    sendJson(t, JsonUtil.wrapError("不支持的方法"), 405);
                }
            } catch (Exception e) {
                sendJson(t, JsonUtil.wrapError(e.getMessage()), 500);
            }
        }
    }

    static class RoomsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if ("OPTIONS".equals(t.getRequestMethod())) { sendCorsHeaders(t); t.sendResponseHeaders(204, -1); return; }
            try {
                if ("GET".equals(t.getRequestMethod())) {
                    String query = t.getRequestURI().getQuery();
                    String userId = null;
                    if (query != null) {
                        for (String kv : query.split("&")) {
                            String[] parts = kv.split("=", 2);
                            if (parts.length == 2 && "userId".equals(parts[0])) {
                                userId = parts[1];
                            }
                        }
                    }

                    User user = null;
                    if (userId != null) user = DataStore.getInstance().getUserById(userId);
                    boolean isAdmin = user != null && user.isAdmin();

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    String today = sdf.format(new Date());

                    List<MeetingRoom> rooms = DataStore.getInstance().getRooms();
                    StringBuilder sb = new StringBuilder("[");
                    for (int i = 0; i < rooms.size(); i++) {
                        if (i > 0) sb.append(",");
                        MeetingRoom r = rooms.get(i);
                        sb.append("{");
                        sb.append("\"id\":\"").append(JsonUtil.escape(r.getId())).append("\",");
                        sb.append("\"name\":\"").append(JsonUtil.escape(r.getName())).append("\",");
                        sb.append("\"capacity\":").append(r.getCapacity()).append(",");

                        List<Booking> bookings;
                        if (isAdmin) {
                            bookings = bookingService.getBookingsByRoomAndDate(r.getId(), null);
                        } else {
                            bookings = bookingService.getBookingsByRoomAndDate(r.getId(), today);
                        }

                        sb.append("\"bookings\":").append(JsonUtil.listToJson(bookings));
                        sb.append("}");
                    }
                    sb.append("]");
                    sendJson(t, "{\"success\":true,\"data\":" + sb.toString() + ",\"today\":\"" + today + "\"}", 200);
                } else {
                    sendJson(t, JsonUtil.wrapError("不支持的方法"), 405);
                }
            } catch (Exception e) {
                sendJson(t, JsonUtil.wrapError(e.getMessage()), 500);
            }
        }
    }

    static class BookingsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if ("OPTIONS".equals(t.getRequestMethod())) { sendCorsHeaders(t); t.sendResponseHeaders(204, -1); return; }
            try {
                String method = t.getRequestMethod();
                if ("POST".equals(method)) {
                    String body = readBody(t);
                    JsonUtil.JsonObject obj = JsonUtil.parseJsonObject(body);
                    String roomId = obj.getString("roomId");
                    String userId = obj.getString("userId");
                    String date = obj.getString("date");
                    String startTime = obj.getString("startTime");
                    String endTime = obj.getString("endTime");
                    String purpose = obj.getString("purpose");
                    Booking b = bookingService.createBooking(roomId, userId, date, startTime, endTime, purpose);
                    sendJson(t, JsonUtil.wrapSuccess(b), 201);
                } else if ("DELETE".equals(method)) {
                    String path = t.getRequestURI().getPath();
                    String id = null;
                    String[] parts = path.split("/");
                    if (parts.length >= 4) id = parts[3];

                    String query = t.getRequestURI().getQuery();
                    String userId = null;
                    if (query != null) {
                        for (String kv : query.split("&")) {
                            String[] qp = kv.split("=", 2);
                            if (qp.length == 2 && "userId".equals(qp[0])) userId = qp[1];
                        }
                    }
                    if (id == null || userId == null) {
                        sendJson(t, JsonUtil.wrapError("参数不完整"), 400);
                        return;
                    }
                    bookingService.cancelBooking(id, userId);
                    sendJson(t, JsonUtil.wrapSuccess(null), 200);
                } else {
                    sendJson(t, JsonUtil.wrapError("不支持的方法"), 405);
                }
            } catch (BookingService.BookingException e) {
                sendJson(t, JsonUtil.wrapError(e.getMessage()), 400);
            } catch (Exception e) {
                sendJson(t, JsonUtil.wrapError(e.getMessage()), 500);
            }
        }
    }

    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            try {
                String path = t.getRequestURI().getPath();
                if ("/".equals(path)) path = "/index.html";
                String filePath = "frontend" + path;

                File file = new File(filePath);
                if (!file.exists() || !file.isFile()) {
                    sendJson(t, JsonUtil.wrapError("Not Found"), 404);
                    return;
                }

                sendCorsHeaders(t);
                if (filePath.endsWith(".html")) t.getResponseHeaders().add("Content-Type", "text/html;charset=utf-8");
                else if (filePath.endsWith(".css")) t.getResponseHeaders().add("Content-Type", "text/css;charset=utf-8");
                else if (filePath.endsWith(".js")) t.getResponseHeaders().add("Content-Type", "application/javascript;charset=utf-8");
                else if (filePath.endsWith(".png")) t.getResponseHeaders().add("Content-Type", "image/png");
                else if (filePath.endsWith(".jpg") || filePath.endsWith(".jpeg")) t.getResponseHeaders().add("Content-Type", "image/jpeg");

                FileInputStream fis = new FileInputStream(file);
                t.sendResponseHeaders(200, file.length());
                OutputStream os = t.getResponseBody();
                byte[] buf = new byte[4096];
                int n;
                while ((n = fis.read(buf)) > 0) os.write(buf, 0, n);
                fis.close();
                os.close();
            } catch (Exception e) {
                sendJson(t, JsonUtil.wrapError(e.getMessage()), 500);
            }
        }
    }
}
