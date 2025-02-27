package com.ygyin.coop.manager.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import javax.annotation.Resource;

/**
 * WebSocket 配置，用于定义连接，为指定的路径配置处理器和拦截器
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Resource
    private ImageEditHandler imageEditHandler;

    @Resource
    private WebSocketHsInterceptor webSocketHsInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(imageEditHandler, "/ws/image/edit")
                .addInterceptors(webSocketHsInterceptor)
                .setAllowedOrigins("*");
    }
}
