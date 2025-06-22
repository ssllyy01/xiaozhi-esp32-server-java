package com.xiaozhi.communication.server.websocket;

import com.xiaozhi.communication.common.ChatSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;

import java.io.IOException;

public class WebSocketSession extends ChatSession {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketSession.class);
    /**
     * 当前会话的链接 session
     */
    protected org.springframework.web.socket.WebSocketSession session;

    public WebSocketSession(String sessionId) {
        super(sessionId);
    }

    public WebSocketSession(org.springframework.web.socket.WebSocketSession session) {
        super(session.getId());
        this.session = session;
    }

    @Override
    public String getSessionId() {
        return session.getId();
    }

    public org.springframework.web.socket.WebSocketSession getSession() {
        return null;
    }

    @Override
    public void close() {
        if(session != null){
            try {
                session.close();
            } catch (IOException e) {
                logger.error("关闭WebSocket会话时发生错误 - SessionId: {}", getSessionId(), e);
            }
        }
    }

    @Override
    public boolean isOpen() {
        return session.isOpen();
    }

    @Override
    public boolean isAudioChannelOpen() {
        return session.isOpen();
    }

    @Override
    public void sendTextMessage(String message) {
        try {
            session.sendMessage(new TextMessage(message));
        } catch (IOException e) {
            logger.error("发送Text消息失败, message: {}", message, e);
        }
    }

    @Override
    public void sendBinaryMessage(byte[] message) {
        try {
            session.sendMessage(new BinaryMessage(message));
        } catch (IOException e) {
            logger.error("发送Binary消息失败", e);
        }
    }
}
