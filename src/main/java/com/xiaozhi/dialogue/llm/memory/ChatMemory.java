package com.xiaozhi.dialogue.llm.memory;

import com.xiaozhi.entity.SysDevice;
import com.xiaozhi.entity.SysMessage;
import com.xiaozhi.entity.SysRole;

import java.util.List;

/**
 * 聊天记忆接口，全局对象，不针对单个会话，而是负责全局记忆的存储策略及针对不同类型数据库的适配。。
 * 不同于SysMessageService，此接口应该是一个更高的抽象层，更多是负责存储策略而并非底层存储的增删改查。
 * 已经参考了spring ai 的ChatMemory接口，暂时放弃spring ai 的ChatMemory。
 * 以后使用ChatClient与Advisor时直接实现一个更本地友好的ChatMemoryAdvisor。
 * Conversation则是参考了 langchain4j 的ChatMemory。
 *
 */
public interface ChatMemory {

    /**
     * 不同的ChatMemory实现类，可以有不同的处理策略，可以初始化不同的Conversation子类。
     *
     * @param device 设备
     * @param role 角色
     * @param sessionId 会话ID
     * @return 会话
     */
    Conversation initConversation(SysDevice device, SysRole role, String sessionId);
    // TODO 考虑将参数以Message对象传递，而不是多个参数。在再下一层Service层转换sparing ai的Message为Mapper需要的SysMessage对象
    void addMessage(String deviceId, String sessionId, String sender, String content, Integer roleId, String messageType, String audioPath);

    List<SysMessage> getMessages(String deviceId, String messageType, Integer limit);

    /**
     * 清除设备的历史记录
     * 
     * @param deviceId 设备ID
     */
    void clearMessages(String deviceId);

}