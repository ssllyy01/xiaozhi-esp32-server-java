package com.xiaozhi.communication.server.websocket;

import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketConfig.class);

    // 定义为public static以便其他类可以访问
    public static final String WS_PATH = "/ws/xiaozhi/v1/";

    @Resource
    private WebSocketHandler webSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketHandler, WS_PATH)
                .setAllowedOrigins("*");
        logger.info("WebSocket 处理器注册到路径: {}", WS_PATH);
    }
    
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(8192);
        container.setMaxBinaryMessageBufferSize(1024 * 1024); // 1MB
        container.setMaxSessionIdleTimeout(60000L); // 60 seconds
        container.setAsyncSendTimeout(5000L); // 5 seconds
        return container;
    }
}