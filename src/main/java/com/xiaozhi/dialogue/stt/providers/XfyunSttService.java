package com.xiaozhi.dialogue.stt.providers;

import cn.xfyun.api.IatClient;
import cn.xfyun.model.response.iat.IatResponse;
import cn.xfyun.model.response.iat.IatResult;
import cn.xfyun.model.response.iat.Text;
import cn.xfyun.service.iat.AbstractIatWebSocketListener;
import com.xiaozhi.dialogue.stt.SttService;
import com.xiaozhi.entity.SysConfig;
import com.xiaozhi.utils.AudioUtils;
import okhttp3.Response;
import okhttp3.WebSocket;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Sinks;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.MalformedURLException;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class XfyunSttService implements SttService {
    private static final Logger logger = LoggerFactory.getLogger(XfyunSttService.class);

    private static final String PROVIDER_NAME = "xfyun";
    // 队列等待超时时间
    private static final int QUEUE_TIMEOUT_MS = 60000;
    // 识别超时时间（60秒）
    private static final long RECOGNITION_TIMEOUT_MS = 60000;

    // 存储当前活跃的识别会话
    private final ConcurrentHashMap<String, IatClient> activeRecognizers = new ConcurrentHashMap<>();

    private String secretId;
    private String secretKey;
    private String appId;

    public XfyunSttService(SysConfig config) {
        if (config != null) {
            this.secretId = config.getApiKey();
            this.secretKey = config.getApiSecret();
            this.appId = config.getAppId();
        }
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public boolean supportsStreaming() {
        return true;
    }

    @Override
    public String recognition(byte[] audioData) {
        if (audioData == null || audioData.length == 0) {
            logger.warn("音频数据为空！");
            return null;
        }
        List<Text> resultSegments = new ArrayList<>();
        // 将原始音频数据转换为MP3格式并保存（用于调试）
        String fileName = AudioUtils.saveAsWav(audioData);
        File file = new File(fileName);
        CountDownLatch recognitionLatch = new CountDownLatch(1);
        try {
            // 检查配置是否已设置
            if (secretId == null || secretKey == null) {
                logger.error("讯飞云语音识别配置未设置，无法进行识别");
                return null;
            }

            // 设置听写参数,这里的appid,apiKey,apiSecret是在开放平台控制台获得
            IatClient iatClient = new IatClient.Builder()
                    .signature(appId, secretId, secretKey)
                    // 动态修正功能：值为wpgs时代表开启（包含修正功能的）流式听写
                    .dwa("wpgs")
                    .build();

            iatClient.send(file, new AbstractIatWebSocketListener() {
                @Override
                public void onSuccess(WebSocket webSocket, IatResponse iatResponse) {
                    if (iatResponse.getCode() != 0) {
                        logger.warn("code：{}, error：{}, sid：{}", iatResponse.getCode(), iatResponse.getMessage(), iatResponse.getSid());
                        logger.warn("错误码查询链接：https://www.xfyun.cn/document/error-code");
                        return;
                    }

                    if (iatResponse.getData() != null) {
                        if (iatResponse.getData().getResult() != null) {
                            // 解析服务端返回结果
                            IatResult result = iatResponse.getData().getResult();
                            Text textObject = result.getText();
                            handleResultText(textObject, resultSegments);
                            logger.info("中间识别结果：{}", getFinalResult(resultSegments));
                        }

                        if (iatResponse.getData().getStatus() == 2) {
                            // resp.data.status ==2 说明数据全部返回完毕，可以关闭连接，释放资源
                            logger.info("session end ");
                            iatClient.closeWebsocket();
                            recognitionLatch.countDown();
                        } else {
                            // 根据返回的数据自定义处理逻辑
                        }
                    }
                }

                @Override
                public void onFail(WebSocket webSocket, Throwable t, Response response) {
                    // 自定义处理逻辑
                    recognitionLatch.countDown();
                }
            });
            // 等待识别完成或超时
            boolean recognized = recognitionLatch.await(RECOGNITION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (!recognized) {
                logger.warn("讯飞云识别超时");
            }
            return getFinalResult(resultSegments);
        } catch (Exception e) {
            logger.error("处理音频时发生错误！", e);
            return null;
        }
    }

    /**
     * 处理返回结果（包括全量返回与流式返回（结果修正））
     */
    private void handleResultText(Text textObject, List<Text> resultSegments) {
        // 处理流式返回的替换结果
        if (StringUtils.equals(textObject.getPgs(), "rpl") && textObject.getRg() != null && textObject.getRg().length == 2) {
            // 返回结果序号sn字段的最小值为1
            int start = textObject.getRg()[0] - 1;
            int end = textObject.getRg()[1] - 1;

            // 将指定区间的结果设置为删除状态
            for (int i = start; i <= end && i < resultSegments.size(); i++) {
                resultSegments.get(i).setDeleted(true);
            }
            // logger.info("替换操作，服务端返回结果为：" + textObject);
        }

        // 通用逻辑，添加当前文本到结果列表
        resultSegments.add(textObject);
    }

    /**
     * 获取最终结果
     */
    private String getFinalResult(List<Text> resultSegments) {
        StringBuilder finalResult = new StringBuilder();
        for (Text text : resultSegments) {
            if (text != null && !text.isDeleted()) {
                finalResult.append(text.getText());
            }
        }
        return finalResult.toString();
    }

    @Override
    public String streamRecognition(Sinks.Many<byte[]> audioSink) {
        // 检查配置是否已设置
        if (secretId == null || secretKey == null || appId == null) {
            logger.error("讯飞云语音识别配置未设置，无法进行识别");
            return null;
        }

        // 使用阻塞队列存储音频数据
        BlockingQueue<byte[]> audioQueue = new LinkedBlockingQueue<>();
        AtomicBoolean isCompleted = new AtomicBoolean(false);
        CountDownLatch recognitionLatch = new CountDownLatch(1);
        List<Text> resultSegments = new ArrayList<>();

//        // 订阅Sink并将数据放入队列
//        audioSink.asFlux().subscribe(
//                data -> audioQueue.offer(data),
//                error -> {
//                    logger.error("音频流处理错误", error);
//                    isCompleted.set(true);
//                },
//                () -> isCompleted.set(true)
//        );

        // 处理合并后的完整字节数组
        audioSink.asFlux()
                .reduce((bytes1, bytes2) -> {
                    // 创建新数组并合并两个字节数组
                    byte[] merged = new byte[bytes1.length + bytes2.length];
                    System.arraycopy(bytes1, 0, merged, 0, bytes1.length);
                    System.arraycopy(bytes2, 0, merged, bytes1.length, bytes2.length);
                    return merged;
                })
                .subscribe(audioQueue::offer,
                        error -> {
                            logger.error("音频流处理错误", error);
                            isCompleted.set(true);
                        },
                        () -> isCompleted.set(true)
                );

        // 设置听写参数,这里的appid,apiKey,apiSecret是在开放平台控制台获得
        IatClient iatClient = new IatClient.Builder()
                .signature(appId, secretId, secretKey)
                // 动态修正功能：值为wpgs时代表开启（包含修正功能的）流式听写
                .dwa("wpgs")
                .build();

        // 生成唯一的语音ID
        String voiceId = UUID.randomUUID().toString();
        // 存储到活跃识别器映射中
        activeRecognizers.put(voiceId, iatClient);

        AbstractIatWebSocketListener socketListener = new AbstractIatWebSocketListener() {
            @Override
            public void onSuccess(WebSocket webSocket, IatResponse iatResponse) {
                if (iatResponse.getCode() != 0) {
                    logger.warn("code：{}, error：{}, sid：{}", iatResponse.getCode(), iatResponse.getMessage(), iatResponse.getSid());
                    logger.warn("错误码查询链接：https://www.xfyun.cn/document/error-code");
                    return;
                }

                if (iatResponse.getData() != null) {
                    if (iatResponse.getData().getResult() != null) {
                        // 解析服务端返回结果
                        IatResult result = iatResponse.getData().getResult();
                        Text textObject = result.getText();
                        handleResultText(textObject, resultSegments);
                        logger.info("中间识别结果：{}", getFinalResult(resultSegments));
                    }
                    if (iatResponse.getData().getStatus() == 2) {
                        // resp.data.status ==2 说明数据全部返回完毕，可以关闭连接，释放资源
                        logger.info("session end ");
                        recognitionLatch.countDown();
                        iatClient.closeWebsocket();
                    } else {
                        // 根据返回的数据自定义处理逻辑
                    }
                }
            }

            @Override
            public void onFail(WebSocket webSocket, Throwable t, Response response) {
                // 自定义处理逻辑
                // 释放锁，表示识别完成
                recognitionLatch.countDown();
                iatClient.closeWebsocket();
                logger.error("xfyun stt fail，原因：{}", t.getMessage());
            }
        };


        // 使用虚拟线程处理音频识别
        try {
            Thread.startVirtualThread(() -> {
                while (!isCompleted.get() || !audioQueue.isEmpty()) {

                    byte[] audioChunk = null;
                    try {
                        audioChunk = audioQueue.poll(QUEUE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                        logger.warn("音频数据队列等待被中断", e);
                        Thread.currentThread().interrupt(); // 重新设置中断标志
                        break;
                    }

                    if (audioChunk != null && activeRecognizers.containsKey(voiceId)) {
                        try {
                            iatClient.send(new ByteArrayInputStream(audioChunk), socketListener);
                        } catch (MalformedURLException e) {
                            throw new RuntimeException(e);
                        } catch (SignatureException e) {
                            throw new RuntimeException(e);
                        }

                        // 如果已完成且队列为空，获取最终结果
                        if (isCompleted.get() && audioQueue.isEmpty()) {
                            activeRecognizers.remove(voiceId);
                            break;
                        }
                    }
                }
            }).join(); // 等待虚拟线程完成
        } catch (Exception e) {
            logger.error("启动虚拟线程失败", e);
        }

        try {
            // 等待识别完成或超时
            boolean recognized = recognitionLatch.await(RECOGNITION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (!recognized) {
                logger.warn("讯飞云识别超时 - VoiceId: {}", voiceId);
                // 超时后清理资源
                if (activeRecognizers.containsKey(voiceId)) {
                    try {
                        iatClient.closeWebsocket();
                        activeRecognizers.remove(voiceId);
                    } catch (Exception e) {
                        logger.error("清理超时识别器资源时发生错误 - VoiceId: {}", voiceId, e);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("创建语音识别会话时发生错误", e);
        }

        return getFinalResult(resultSegments);
    }
}