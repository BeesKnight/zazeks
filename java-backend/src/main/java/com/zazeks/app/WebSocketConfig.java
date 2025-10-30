package com.zazeks.app;

import com.zazeks.websocket.MultiplayerWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final MultiplayerWebSocketHandler multiplayerHandler;

    public WebSocketConfig(MultiplayerWebSocketHandler multiplayerHandler) {
        this.multiplayerHandler = multiplayerHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(multiplayerHandler, "/ws/multiplayer").setAllowedOrigins("*");
    }
}
