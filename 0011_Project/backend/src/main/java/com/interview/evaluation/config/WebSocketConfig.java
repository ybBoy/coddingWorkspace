package com.interview.evaluation.config;

import com.interview.evaluation.interceptor.WebSocketInterceptor;
import com.interview.evaluation.websocket.EvaluationWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import javax.annotation.Resource;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Resource
    private EvaluationWebSocketHandler webSocketHandler;

    @Resource
    private WebSocketInterceptor webSocketInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketHandler, "/ws/evaluation")
                .addInterceptors(webSocketInterceptor)
                .setAllowedOrigins("*");
    }
}
