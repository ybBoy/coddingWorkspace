package com.danmaku;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

import javax.servlet.annotation.WebServlet;

public class AppServer {
    private static final int PORT = 8080;

    public static void main(String[] args) throws Exception {
        Server server = new Server(PORT);

        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setResourceBase("frontend/dist");
        resourceHandler.setDirectoriesListed(true);
        resourceHandler.setWelcomeFiles(new String[]{"index.html"});

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        context.addServlet(new ServletHolder(new DanmakuServlet()), "/ws");

        HandlerList handlers = new HandlerList();
        handlers.addHandler(resourceHandler);
        handlers.addHandler(context);

        server.setHandler(handlers);

        System.out.println("Danmaku Server starting on port " + PORT);
        System.out.println("WebSocket endpoint: ws://localhost:" + PORT + "/ws");
        System.out.println("Web interface: http://localhost:" + PORT);

        server.start();
        server.join();
    }

    @WebServlet(name = "Danmaku WebSocket", urlPatterns = {"/ws"})
    public static class DanmakuServlet extends WebSocketServlet {
        @Override
        public void configure(WebSocketServletFactory factory) {
            factory.register(DanmakuWebSocket.class);
        }
    }
}
