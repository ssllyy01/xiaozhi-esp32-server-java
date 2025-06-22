package com.xiaozhi.dialogue.llm.tool.function;

import com.xiaozhi.communication.common.ChatSession;
import com.xiaozhi.dialogue.llm.ChatService;
import com.xiaozhi.dialogue.llm.tool.ToolsGlobalRegistry;
import com.xiaozhi.dialogue.llm.tool.ToolCallStringResultConverter;
import com.xiaozhi.dialogue.service.MusicService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PlayMusicFunction implements ToolsGlobalRegistry.GlobalFunction {
    private static final Logger logger = LoggerFactory.getLogger(PlayMusicFunction.class);

    @Resource
    private MusicService musicService;

    ToolCallback toolCallback = FunctionToolCallback
            .builder("func_playMusic", (Map<String, String> params, ToolContext toolContext) -> {
                ChatSession chatSession = (ChatSession)toolContext.getContext().get(ChatService.TOOL_CONTEXT_SESSION_KEY);
                String songName = params.get("songName");
                try{
                    if (songName == null || songName.isEmpty()) {
                        return "音乐播放失败";
                    }else{
                        musicService.playMusic(chatSession, songName, null);
                        return "尝试播放歌曲《"+songName+"》";
                    }
                }catch (Exception e){
                    logger.error("device 音乐播放异常，song name: {}", songName, e);
                    return "音乐播放失败";
                }
            })
            .toolMetadata(ToolMetadata.builder().returnDirect(true).build())
            .description("音乐播放助手,需要用户提供歌曲的名称")
            .inputSchema("""
                        {
                            "type": "object",
                            "properties": {
                                "songName": {
                                    "type": "string",
                                    "description": "要播放的歌曲名称"
                                }
                            },
                            "required": ["songName"]
                        }
                    """)
            .inputType(Map.class)
            .toolCallResultConverter(ToolCallStringResultConverter.INSTANCE)
            .build();

    @Override
    public ToolCallback getFunctionCallTool(ChatSession chatSession) {
        return toolCallback;
    }
}
