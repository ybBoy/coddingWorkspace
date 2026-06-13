package service;

import model.Book;
import store.JsonBookStore;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class BookService {
    private final JsonBookStore store;
    private List<Book> books;

    public BookService() {
        this.store = new JsonBookStore();
        this.books = new ArrayList<Book>(store.loadAll());
    }

    public List<Book> listAll() {
        return new ArrayList<Book>(books);
    }

    public List<Book> listByStatus(String status) {
        return books.stream()
                .filter(b -> status.equals(b.getStatus()))
                .collect(Collectors.<Book>toList());
    }

    public Book create(String title, String author, String status, String remark) {
        Book book = new Book();
        book.setId(UUID.randomUUID().toString());
        book.setTitle(title);
        book.setAuthor(author);
        book.setStatus(status);
        book.setRemark(remark == null ? "" : remark);
        book.setCreatedAt(System.currentTimeMillis());
        books.add(book);
        persist();
        return book;
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
