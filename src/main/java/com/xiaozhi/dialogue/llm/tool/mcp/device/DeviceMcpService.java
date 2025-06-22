package com.xiaozhi.dialogue.llm.tool.mcp.device;

import com.xiaozhi.communication.common.ChatSession;
import com.xiaozhi.communication.domain.DeviceMcpMessage;
import com.xiaozhi.communication.domain.mcp.device.DeviceMcpPayload;
import com.xiaozhi.communication.domain.mcp.device.initialize.DeviceMcpClientInfo;
import com.xiaozhi.communication.domain.mcp.device.initialize.DeviceMcpInitialize;
import com.xiaozhi.communication.domain.mcp.device.initialize.DeviceMcpVision;
import com.xiaozhi.dialogue.llm.tool.ToolCallStringResultConverter;
import com.xiaozhi.utils.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class DeviceMcpService {
    private static final Logger logger = LoggerFactory.getLogger(DeviceMcpService.class);

    @Value("${xiaozhi.mcp:device:max.tools.count:32}")
    private static final int maxToolsCount = 32; // 最大工具数量限制
    /**
     * 初始化设备端MCP工具列表
     * @param chatSession
     */
    public void initialize(ChatSession chatSession) {
        //1、调用始化命令
        DeviceMcpMessage initResult = sendInitialize(chatSession);
        //根据调用结果进行处理
        if(initResult != null){
            chatSession.getDeviceMcpHolder().setMcpInitialized(true);
        }
        if(chatSession.getDeviceMcpHolder().isMcpInitialized()){
            //2、获取工具列表
            sendToolsList(chatSession);
        }
    }

    /**
     * 发送初始化命令
     * @param chatSession
     * @return
     */
    protected DeviceMcpMessage sendInitialize(ChatSession chatSession) {
        DeviceMcpMessage message = new DeviceMcpMessage();
        message.setSessionId(chatSession.getSessionId());
        DeviceMcpPayload payload = new DeviceMcpPayload();
        payload.setId(chatSession.getDeviceMcpHolder().getMcpRequestId());
        payload.setMethod("initialize");

        // MCP初始化参数
        DeviceMcpInitialize initialize = new DeviceMcpInitialize();
        DeviceMcpVision vision = new DeviceMcpVision();//TODO: 摄像头视觉相关, 根据实际需要设置vision的属性
        initialize.setCapabilities(Map.of(
                "vision", vision
        ));
        initialize.setClientInfo(new DeviceMcpClientInfo());
        payload.setParams(initialize);
        message.setPayload(payload);

        DeviceMcpMessage result = sendMcpRequest(chatSession, message);
        if(result != null) {
            logger.debug("SessionId: {}, MCP initialized successfully", chatSession.getSessionId());
            return result;
        }
        return null;
    }

    /**
     * 发送工具列表请求
     * @param chatSession
     */
    private void sendToolsList(ChatSession chatSession) {
        DeviceMcpMessage message = new DeviceMcpMessage();
        message.setSessionId(chatSession.getSessionId());
        DeviceMcpPayload payload = new DeviceMcpPayload();
        payload.setId(chatSession.getDeviceMcpHolder().getMcpRequestId());
        payload.setMethod("tools/list");
        if(chatSession.getDeviceMcpHolder().getMcpCursor() != null) {
            payload.setParams(Map.of(
                    "cursor", chatSession.getDeviceMcpHolder().getMcpCursor()));
        } else {
            payload.setParams(Map.of(
                    "cursor", "")); // 初始请求时使用空字符串
        }
        message.setPayload(payload);

        DeviceMcpMessage result = sendMcpRequest(chatSession, message);
        if(result != null) {
            //处理工具的注册
            List<Map<String, Object>> tools = (List<Map<String, Object>>) result.getPayload().getResult().get("tools");
            Object nextCursor = result.getPayload().getResult().get("nextCursor");
            int toolsCount = chatSession.getToolCallbacks().size();
            if (tools.isEmpty() || (toolsCount + tools.size()) > maxToolsCount) {//工具数量超过限制，不再添加
                return;
            } else {
                for (Map<String, Object> tool : tools) {
                    //开始注册工具
                    String name = (String) tool.get("name");
                    var funcName = "mcp_" + name;
                    String funcDescription = (String) tool.get("description");
                    Object inputSchema = tool.get("inputSchema");

                    var toolCallback = FunctionToolCallback
                            .builder(funcName, (Map<String, Object> params, ToolContext toolContext) -> {
                                String actFuncName = funcName.substring(4); // 原始方法调用，去掉iot_前缀
                                DeviceMcpMessage request = new DeviceMcpMessage();
                                request.setSessionId(chatSession.getSessionId());

                                DeviceMcpPayload requestPayload = new DeviceMcpPayload();
                                requestPayload.setMethod("tools/call");
                                requestPayload.setId(chatSession.getDeviceMcpHolder().getMcpRequestId());
                                requestPayload.setParams(Map.of(
                                        "name", actFuncName,
                                        "arguments", params
                                ));

                                request.setPayload(requestPayload);
                                DeviceMcpMessage response = sendMcpRequest(chatSession, request);
                                if (response != null) {
                                    logger.debug("SessionId: {},  MCP function call response: {}" , chatSession.getSessionId(), response);
                                    if("false".equals(String.valueOf(response.getPayload().getResult().get("isError")))){
                                        return response.getPayload().getResult().get("content");//返回结果
                                    }else{
                                        return response.getPayload().getError();
                                    }
                                } else {
                                    return "操作失败";
                                }
                            })
                            .toolMetadata(ToolMetadata.builder().returnDirect(false).build())// 设置返回值需要ai再处理
                            .description(funcDescription)
                            .inputSchema(JsonUtil.toJson(inputSchema))
                            .inputType(Map.class)
                            .toolCallResultConverter(ToolCallStringResultConverter.INSTANCE)
                            .build();
                    // 注册到当前会话的函数持有者
                    chatSession.getToolsSessionHolder().registerFunction(funcName, toolCallback);
                }
            }
            // 如果cursor不为空，则迭代调用
            if(nextCursor != null && !nextCursor.toString().isEmpty()) {
                // 如果有下一页游标，继续请求下一页
                chatSession.getDeviceMcpHolder().setMcpCursor(nextCursor.toString());
                sendToolsList(chatSession);
            } else {
                // 所有工具加载完成
                chatSession.getDeviceMcpHolder().setMcpCursor(null);
                logger.debug("SessionId: {}, All tools loaded successfully", chatSession.getSessionId());
            }
        }
    }

    public DeviceMcpMessage sendMcpRequest(ChatSession chatSession, DeviceMcpMessage mcpMessage) {
        Long id = mcpMessage.getPayload().getId();
        CompletableFuture<DeviceMcpMessage> future = new CompletableFuture<>();
        chatSession.sendTextMessage(JsonUtil.toJson(mcpMessage));
        chatSession.getDeviceMcpHolder().getMcpPendingRequests().put(id, future);

        DeviceMcpMessage response = null;
        try {
            // 阻塞并等待异步操作完成
            response = future.get(2, TimeUnit.SECONDS);//等待2秒，没反应则退出
        } catch (Exception e) {
            logger.error("SessionId: {}, Error sending MCP request", chatSession.getSessionId(), e);
            chatSession.getDeviceMcpHolder().getMcpPendingRequests().remove(id);
        }
        return response;
    }

}