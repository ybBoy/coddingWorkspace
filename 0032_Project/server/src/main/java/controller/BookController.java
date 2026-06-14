package controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import model.Book;
import service.BookService;
import service.BookService.ValidationResult;
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
        String keyword = req.queryParams("keyword");
        String sortBy = req.queryParams("sortBy");
        List<Book> list = service.query(status, keyword, sortBy);
        return toJson(list);
    }

    public String createBook(Request req, Response res) throws Exception {
        res.type("application/json");
        Map<String, String> body = parseBody(req);
        String title = body.get("title");
        String author = body.get("author");
        String status = body.get("status");
        String remark = body.get("remark");

        ValidationResult vr = service.validate(title, author, status, remark);
        if (!vr.valid) {
            res.status(400);
            return errorJson(vr.message);
        }
        try {
            Book book = service.create(title, author, status, remark);
            res.status(201);
            return toJson(book);
        } catch (Exception e) {
            res.status(500);
            return errorJson("保存失败：" + e.getMessage());
        }
    }

    public String updateBook(Request req, Response res) throws Exception {
        res.type("application/json");
        String id = req.params(":id");
        Map<String, String> body = parseBody(req);
        String title = body.get("title");
        String author = body.get("author");
        String status = body.get("status");
        String remark = body.get("remark");

        ValidationResult vr = service.validate(title, author, status, remark);
        if (!vr.valid) {
            res.status(400);
            return errorJson(vr.message);
        }
        try {
            Book book = service.updateBook(id, title, author, status, remark);
            if (book == null) {
                res.status(404);
                return errorJson("书籍不存在");
            }
            return toJson(book);
        } catch (Exception e) {
            res.status(500);
            return errorJson("保存失败：" + e.getMessage());
        }
    }

    public String updateStatus(Request req, Response res) throws Exception {
        res.type("application/json");
        String id = req.params(":id");
        Map<String, String> body = parseBody(req);
        String status = body.get("status");
        if (status == null || !BookService.VALID_STATUSES.contains(status)) {
            res.status(400);
            return errorJson("阅读状态非法");
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

    @SuppressWarnings("unchecked")
    private Map<String, String> parseBody(Request req) throws Exception {
        String bodyStr = req.body();
        if (bodyStr == null || bodyStr.isEmpty()) {
            return new HashMap<String, String>();
        }
        return mapper.readValue(bodyStr, HashMap.class);
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
