package controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import model.Book;
import service.BookService;
import spark.Request;
import spark.Response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BookController {
    private final BookService service;
    private final ObjectMapper mapper;

    public BookController() {
        this.service = new BookService();
        this.mapper = new ObjectMapper();
    }

    public String listBooks(Request req, Response res) {
        res.type("application/json");
        String status = req.queryParams("status");
        List<Book> list = (status == null || status.isEmpty())
                ? service.listAll()
                : service.listByStatus(status);
        return toJson(list);
    }

    public String createBook(Request req, Response res) throws Exception {
        res.type("application/json");
        res.status(201);
        @SuppressWarnings("unchecked")
        Map<String, String> body = mapper.readValue(req.body(), HashMap.class);
        String title = body.get("title");
        String author = body.get("author");
        String status = body.get("status");
        String remark = body.get("remark");
        if (title == null || author == null || status == null) {
            res.status(400);
            return errorJson("缺少必填字段");
        }
        Book book = service.create(title.trim(), author.trim(), status, remark == null ? "" : remark);
        return toJson(book);
    }

    public String updateStatus(Request req, Response res) throws Exception {
        res.type("application/json");
        String id = req.params(":id");
        @SuppressWarnings("unchecked")
        Map<String, String> body = mapper.readValue(req.body(), HashMap.class);
        String status = body.get("status");
        if (status == null) {
            res.status(400);
            return errorJson("缺少 status 字段");
        }
        Book book = service.updateStatus(id, status);
        if (book == null) {
            res.status(404);
            return errorJson("书籍不存在");
        }
        return toJson(book);
    }

    public String deleteBook(Request req, Response res) {
        res.type("application/json");
        String id = req.params(":id");
        boolean removed = service.delete(id);
        if (!removed) {
            res.status(404);
            return errorJson("书籍不存在");
        }
        return "{\"ok\":true}";
    }

    private String toJson(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            return errorJson(e.getMessage());
        }
    }

    private String errorJson(String msg) {
        try {
            Map<String, String> err = new HashMap<String, String>();
            err.put("error", msg);
            return mapper.writeValueAsString(err);
        } catch (Exception e) {
            return "{\"error\":\"" + msg + "\"}";
        }
    }
}
