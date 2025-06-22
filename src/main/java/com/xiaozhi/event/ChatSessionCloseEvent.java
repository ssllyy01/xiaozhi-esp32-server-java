package com.xiaozhi.event;

import com.xiaozhi.communication.common.ChatSession;
import org.springframework.context.ApplicationEvent;

import java.time.Clock;

/**
 * Session 关闭事件
 */
public class ChatSessionCloseEvent extends ApplicationEvent {

    public ChatSessionCloseEvent(Object source) {
        super(source);
    }

    public ChatSessionCloseEvent(Object source, Clock clock) {
        super(source, clock);
    }

    public ChatSession getSession() {
        return (ChatSession) getSource();
    }

}
