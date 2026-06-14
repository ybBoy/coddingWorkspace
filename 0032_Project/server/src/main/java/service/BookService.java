package service;

import model.Book;
import store.JsonBookStore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class BookService {

    public static final Set<String> VALID_STATUSES = new HashSet<String>(
            Arrays.asList("TO_READ", "READING", "READ"));

    public static final int TITLE_MAX = 200;
    public static final int AUTHOR_MAX = 100;
    public static final int REMARK_MAX = 500;

    private final JsonBookStore store;
    private List<Book> books;

    public BookService() {
        this.store = new JsonBookStore();
        this.books = new ArrayList<Book>(store.loadAll());
    }

    public static class ValidationResult {
        public final boolean valid;
        public final String message;
        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }
    }

    public ValidationResult validate(String title, String author, String status, String remark) {
        if (title == null || title.trim().isEmpty()) {
            return new ValidationResult(false, "书名不能为空");
        }
        if (title.trim().length() > TITLE_MAX) {
            return new ValidationResult(false, "书名长度不能超过 " + TITLE_MAX + " 个字符");
        }
        if (author == null || author.trim().isEmpty()) {
            return new ValidationResult(false, "作者不能为空");
        }
        if (author.trim().length() > AUTHOR_MAX) {
            return new ValidationResult(false, "作者长度不能超过 " + AUTHOR_MAX + " 个字符");
        }
        if (status == null || !VALID_STATUSES.contains(status)) {
            return new ValidationResult(false, "阅读状态非法，只能是 TO_READ / READING / READ");
        }
        if (remark != null && remark.length() > REMARK_MAX) {
            return new ValidationResult(false, "备注长度不能超过 " + REMARK_MAX + " 个字符");
        }
        return new ValidationResult(true, null);
    }

    public List<Book> query(String status, String keyword, String sortBy) {
        List<Book> list = new ArrayList<Book>(books);

        if (status != null && !status.isEmpty()) {
            list = list.stream()
                    .filter(b -> status.equals(b.getStatus()))
                    .collect(Collectors.<Book>toList());
        }

        if (keyword != null && !keyword.trim().isEmpty()) {
            final String kw = keyword.trim().toLowerCase();
            list = list.stream()
                    .filter(b -> (b.getTitle() != null && b.getTitle().toLowerCase().contains(kw))
                              || (b.getAuthor() != null && b.getAuthor().toLowerCase().contains(kw)))
                    .collect(Collectors.<Book>toList());
        }

        Comparator<Book> cmp;
        if ("status".equals(sortBy)) {
            cmp = new Comparator<Book>() {
                private int order(String s) {
                    if ("READING".equals(s)) return 0;
                    if ("TO_READ".equals(s)) return 1;
                    if ("READ".equals(s)) return 2;
                    return 9;
                }
                public int compare(Book a, Book b) {
                    int c = Integer.compare(order(a.getStatus()), order(b.getStatus()));
                    return c != 0 ? c : Long.compare(b.getCreatedAt(), a.getCreatedAt());
                }
            };
        } else if ("title".equals(sortBy)) {
            cmp = new Comparator<Book>() {
                public int compare(Book a, Book b) {
                    String ta = a.getTitle() == null ? "" : a.getTitle();
                    String tb = b.getTitle() == null ? "" : b.getTitle();
                    return ta.compareToIgnoreCase(tb);
                }
            };
        } else {
            cmp = new Comparator<Book>() {
                public int compare(Book a, Book b) {
                    return Long.compare(b.getCreatedAt(), a.getCreatedAt());
                }
            };
        }
        Collections.sort(list, cmp);
        return list;
    }

    public Book create(String title, String author, String status, String remark) {
        Book book = new Book();
        book.setId(UUID.randomUUID().toString());
        book.setTitle(title.trim());
        book.setAuthor(author.trim());
        book.setStatus(status);
        book.setRemark(remark == null ? "" : remark);
        book.setCreatedAt(System.currentTimeMillis());
        books.add(book);
        persist();
        return book;
    }

    public Book updateBook(String id, String title, String author, String status, String remark) {
        for (Book book : books) {
            if (id.equals(book.getId())) {
                book.setTitle(title.trim());
                book.setAuthor(author.trim());
                book.setStatus(status);
                book.setRemark(remark == null ? "" : remark);
                persist();
                return book;
            }
        }
        return null;
    }

    public Book updateStatus(String id, String status) {
        for (Book book : books) {
            if (id.equals(book.getId())) {
                book.setStatus(status);
                persist();
                return book;
            }
        }
        return null;
    }

    public boolean delete(String id) {
        boolean removed = books.removeIf(b -> id.equals(b.getId()));
        if (removed) {
            persist();
        }
        return removed;
    }

    private void persist() {
        store.saveAll(books);
    }
}
