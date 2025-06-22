package com.xiaozhi;

import com.xiaozhi.communication.server.websocket.WebSocketConfig;
import com.xiaozhi.utils.CmsUtils;

import java.util.Map;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableCaching
@MapperScan("com.xiaozhi.dao")
public class XiaozhiApplication {

    Logger logger = LoggerFactory.getLogger(XiaozhiApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(XiaozhiApplication.class, args);
    }

    @Bean
    public ApplicationListener<WebServerInitializedEvent> webServerInitializedListener() {
        return event -> {
            int port = event.getWebServer().getPort();
            String contextPath = event.getApplicationContext().getEnvironment()
                    .getProperty("server.servlet.context-path", "");

            // 获取最适合的服务器IP地址
            String serverIp = CmsUtils.getServerIp();

            String wsAddress = "ws://" + serverIp + ":" + port + contextPath + WebSocketConfig.WS_PATH;
            String otaAddress = "http://" + serverIp + ":" + port + contextPath + "/api/device/ota";

            logger.info("==========================================================");
            logger.info("🚀 小智物联网平台服务已成功启动");
            logger.info("==========================================================");
            logger.info("📡 WebSocket服务地址: {}", wsAddress);
            logger.info("📦 OTA升级服务地址: {}", otaAddress);

            // 输出环境详情调试信息
            logger.info("==========================================================");
            logger.info("🔍 环境详情调试信息:");
            Map<String, Object> envDetails = CmsUtils.getEnvironmentDetails();
            for (Map.Entry<String, Object> entry : envDetails.entrySet()) {
                logger.info("   {} = {}", entry.getKey(), entry.getValue());
            }

            logger.info("==========================================================");
            logger.info("祝您使用愉快！");
            logger.info("==========================================================");
        };
    }

}