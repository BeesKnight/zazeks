package com.zazeks.websocket;

import com.zazeks.api.MultiplayerService;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class MultiplayerWebSocketHandler extends TextWebSocketHandler {
    private final MultiplayerService multiplayerService;

    public MultiplayerWebSocketHandler(MultiplayerService multiplayerService) {
        this.multiplayerService = multiplayerService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // connection is accepted in handleMessage when join payload received
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        multiplayerService.handleMessage(session, message.getPayload());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        multiplayerService.handleDisconnect(session, status);
    }

    @PreDestroy
    public void shutdown() {
        multiplayerService.shutdown();
    }
}
