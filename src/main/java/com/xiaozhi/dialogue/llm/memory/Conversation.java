package com.xiaozhi.dialogue.llm.memory;

import com.xiaozhi.entity.SysDevice;
import com.xiaozhi.entity.SysRole;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.List;

/**
 * Conversation 是一个 对应于 sys_message 表的，但高于 sys_message 的一个抽象实体。
 * deviceID, roleID, sessionID, 实质构成了一次Conversation的全局唯一ID。这个ID必须final 的。
 * 在关系型数据库里，可以将deviceID, roleID, sessionID 建一个组合索引，注意顺序sessionID放在最后。
 * 在图数据库里， conversation label的节点，连接 device节点、role节点。
 * deviceID与roleID本质上不是Conversation的真正属性，而是外键，代表连接的2个对象。
 * 只有sessionID是真正挂在Conversation的属性。
 *
 */
public abstract class Conversation {
    private final SysDevice device;
    private final SysRole role;
    private final String sessionId;

    private List<Message> messages;

    public Conversation(SysDevice device, SysRole role, String sessionId, List<Message> messages) {
        this.device = device;
        this.role = role;
        this.sessionId = sessionId;
        this.messages = messages;
    }

    public SysDevice device() {
        return device;
    }
    public SysRole role() {
        return role;
    }

    public String sessionId() {
        return sessionId;
    }

    public List<Message> messages() {
        return messages;
    }

    abstract public void clear();

    abstract public void addMessage(UserMessage message, String audioPath);

    abstract public void addMessage(AssistantMessage message, String audioPath);

    abstract public List<Message> prompt(UserMessage userMessage);
}
