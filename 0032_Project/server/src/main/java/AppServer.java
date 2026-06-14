import controller.BookController;

import static spark.Spark.*;

public class AppServer {
    public static void main(String[] args) {
        BookController controller = new BookController();

        port(8080);

        before((req, res) -> {
            res.header("Access-Control-Allow-Origin", "*");
            res.header("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
            res.header("Access-Control-Allow-Headers", "Content-Type,Accept");
        });

        options("/*", (req, res) -> {
            res.status(200);
            return "ok";
        });

        path("/api", () -> {
            path("/books", () -> {
                get("", controller::listBooks);
                post("", controller::createBook);
                put("/:id", controller::updateBook);
                put("/:id/status", controller::updateStatus);
                delete("/:id", controller::deleteBook);
            });
        });

        awaitInitialization();
        System.out.println("读书清单服务已启动: http://localhost:8080");
    }
}
