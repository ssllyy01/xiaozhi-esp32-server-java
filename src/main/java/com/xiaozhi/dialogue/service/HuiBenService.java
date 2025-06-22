package com.xiaozhi.dialogue.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaozhi.communication.common.ChatSession;
import com.xiaozhi.communication.common.SessionManager;
import com.xiaozhi.utils.AudioUtils;
import com.xiaozhi.utils.OpusProcessor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 绘本服务，负责处理绘本播放和文本同步
 * 使用JDK 21虚拟线程和结构化并发实现异步处理
 */
@Service
public class HuiBenService {
    private static final Logger logger = LoggerFactory.getLogger(HuiBenService.class);

    private static final long OPUS_FRAME_INTERVAL_MS = AudioUtils.OPUS_FRAME_DURATION_MS;
    private static final String API_BASE_URL = "https://www.limaogushi.com/huiben/";

    // 使用OkHttp3替代JDK HttpClient
    private static final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OpusProcessor opusProcessor;

    @Autowired
    private SessionManager sessionManager;

    @Autowired
    private AudioService audioService;

    // 使用虚拟线程执行器处理定时任务
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(
            Runtime.getRuntime().availableProcessors(),
            Thread.ofVirtual().name("huiBen-scheduler-", 0).factory());

    // 存储每个会话的当前文本信息
    private final Map<String, List<TextLine>> sessionTexts = new ConcurrentHashMap<>();

    // 存储每个会话的当前播放时间
    private final Map<String, AtomicLong> playTime = new ConcurrentHashMap<>();

    // 存储每个会话的音频文件路径，用于播放完成后删除
    private final Map<String, String> sessionAudioFiles = new ConcurrentHashMap<>();

    // 存储每个会话的调度任务
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    /**
     * 文本行数据结构 - 使用JDK 16+ Record类型
     */
    private record TextLine(long timeMs, String text) {
    }

    /**
     * 播放绘本
     * 使用JDK 21虚拟线程和结构化并发实现异步处理
     * 
     * @param session 会话
     * @param bookId 绘本ID
     */
    public void playHuiBen(ChatSession session, Integer bookId) {
        String sessionId = session.getSessionId();

        // 使用虚拟线程处理异步任务
        Thread.startVirtualThread(() -> {
            try {
                // 设置绘本播放状态
                sessionManager.setMusicPlaying(sessionId, true);

                // 重置播放时间
                playTime.computeIfAbsent(sessionId, k -> new AtomicLong()).set(0);

                // 清理之前的音频文件（如果有）
                cleanupAudioFile(sessionId);

                // 1. 获取绘本信息
                Map<String, String> huiBenInfo = getHuiBenInfo(bookId);
                if (huiBenInfo == null) {
                    throw new RuntimeException("无法找到绘本: " + bookId);
                }

                // 2. 下载音频文件到本地临时目录，使用随机文件名避免冲突
                String audioUrl = huiBenInfo.get("audioUrl");
                String randomName = "huiBen_" + sessionId + "_" + UUID.randomUUID() + ".mp3";
                String audioPath = downloadFile(audioUrl, randomName);

                if (audioPath == null) {
                    throw new RuntimeException("下载音频文件失败");
                }

                // 保存音频文件路径，用于播放完成后删除
                sessionAudioFiles.put(sessionId, audioPath);

                // 发送绘本开始消息
                audioService.sendStart(session);

                // 发送音频和同步文本
                sendAudio(session, audioPath);

            } catch (Exception e) {

                try {
                    audioService.sendSentenceStart(session, "播放绘本时发生错误: " + e.getMessage());
                    audioService.sendStop(session);
                } catch (Exception ex) {
                    logger.error("发送错误消息失败", ex);
                }

            }
        });
    }

    /**
     * 清理之前的音频文件
     */
    private void cleanupAudioFile(String sessionId) {
        String audioPath = sessionAudioFiles.remove(sessionId);
        if (audioPath != null) {
            try {
                Files.deleteIfExists(Path.of(audioPath));
            } catch (Exception e) {
                logger.warn("删除音频文件失败: {}", audioPath, e);
            }
        }
        // 清理会话的文本数据
        sessionTexts.remove(sessionId);
    }

    /**
     * 发送音频和同步文本
     */
    private void sendAudio(ChatSession session, String audioPath) {
        String sessionId = session.getSessionId();

        try {
            // 读取音频文件
            Path audioFilePath = Path.of(audioPath);
            if (!Files.exists(audioFilePath)) {
                logger.warn("音频文件不存在: {}", audioPath);
                return;
            }

            // 将音频文件转换为PCM格式
            byte[] audioData = AudioUtils.readAsPcm(audioPath);
            if (audioData == null || audioData.length == 0) {
                logger.warn("音频数据为空");
                return;
            }

            // 将PCM转换为Opus帧
            List<byte[]> frames = opusProcessor.pcmToOpus(sessionId, audioData, false);
            if (frames == null || frames.isEmpty()) {
                logger.warn("Opus帧转换失败或为空");
                return;
            }

            // 获取文本
            List<TextLine> texts = sessionTexts.getOrDefault(sessionId, Collections.emptyList());
            AtomicLong currPlayTime = playTime.computeIfAbsent(sessionId, k -> new AtomicLong(0));

            // 预处理文本时间点，将毫秒时间转换为帧索引
            Map<Integer, String> textFrameMap = new HashMap<>();
            for (TextLine line : texts) {
                // 计算文本对应的帧索引
                int frameIndex = (int) (line.timeMs() / OPUS_FRAME_INTERVAL_MS);
                if (frameIndex < frames.size()) {
                    textFrameMap.put(frameIndex, line.text());
                }
            }

            // 创建帧发送任务，使用原子引用替代数组
            AtomicLong frameIndexRef = new AtomicLong(0);
            Runnable frameTask = () -> {
                try {
                    int currentIndex = (int) frameIndexRef.get();
                    if (currentIndex >= frames.size()) {
                        // 所有帧已发送，取消任务
                        audioService.cancelScheduledTask(sessionId);
                        return;
                    }

                    // 更新活跃时间
                    sessionManager.updateLastActivity(sessionId);

                    // 更新当前播放时间
                    currPlayTime.set(currentIndex * OPUS_FRAME_INTERVAL_MS);

                    // 先检查是否有对应这一帧的文本需要发送
                    String textContent = textFrameMap.get(currentIndex);
                    if (textContent != null) {
                        audioService.sendSentenceStart(session, textContent);
                    }

                    // 发送当前帧
                    byte[] frame = frames.get(currentIndex);
                    audioService.sendOpusFrame(session, frame);

                    // 增加帧索引
                    frameIndexRef.incrementAndGet();

                } catch (Exception e) {
                    // 发生错误，取消调度任务
                    logger.error("发送音频帧时发生错误", e);
                    audioService.cancelScheduledTask(sessionId);
                }
            };

            // 启动定时任务，每隔OPUS_FRAME_INTERVAL_MS毫秒执行一次
            ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(
                    frameTask,
                    0,
                    OPUS_FRAME_INTERVAL_MS,
                    TimeUnit.MILLISECONDS);

            // 存储任务引用，以便稍后取消
            scheduledTasks.put(sessionId, task);
        } catch (Exception e) {
            logger.error("处理音频时发生错误 - SessionId: {}", sessionId, e);
            try {
                audioService.sendStop(session);
            } catch (Exception ex) {
                logger.error("发送停止消息失败", ex);
            }
        }
    }

    /**
     * 获取绘本信息（音频URL）
     */
    private Map<String, String> getHuiBenInfo(Integer bookId) {
        try {
            // 构建URL
            String url = API_BASE_URL + bookId + ".html";

            // 使用OkHttp3发送请求
            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            try (Response response = okHttpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    logger.error("获取绘本信息失败，响应码: {}", response.code());
                    return null;
                }

                // 解析响应
                String responseBody = response.body() != null ? response.body().string() : null;
                if (responseBody == null) {
                    logger.error("获取绘本信息失败，响应体为空");
                    return null;
                }

                String audioUrl = extractAudioSrcByRegex(responseBody);
                Map<String, String> result = new HashMap<>();
                result.put("audioUrl", audioUrl);
                return result;
            }
        } catch (Exception e) {
            logger.error("获取绘本信息时发生错误", e);
            return null;
        }
    }

    /**
     * 从HTML中提取音频源URL
     */
    public static String extractAudioSrcByRegex(String html) {
        // 匹配 source 标签中的 src 属性
        Pattern pattern = Pattern.compile("<source\\s+[^>]*src\\s*=\\s*[\"']([^\"']+)[\"'][^>]*>");
        Matcher matcher = pattern.matcher(html);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    /**
     * 下载文件到临时目录
     */
    private String downloadFile(String fileUrl, String fileName) {
        try {
            // 确保音频目录存在
            Path audioDir = Path.of(AudioUtils.AUDIO_PATH);
            Files.createDirectories(audioDir);

            // 将文件保存到音频目录
            Path outputPath = audioDir.resolve(fileName);

            // 使用OkHttp3下载文件
            Request request = new Request.Builder()
                    .url(fileUrl)
                    .get()
                    .build();

            try (Response response = okHttpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    logger.error("下载文件失败，响应码: {}", response.code());
                    return null;
                }

                // 将响应体写入文件
                Files.write(outputPath, response.body().bytes());

                return outputPath.toAbsolutePath().toString();
            }
        } catch (Exception e) {
            logger.error("下载文件时发生错误", e);
            return null;
        }
    }

    /**
     * 停止播放绘本
     * 
     * @param sessionId 会话ID
     */
    public void stopHuiBen(String sessionId) {
        Thread.startVirtualThread(() -> {
            try {
                ScheduledFuture<?> task = scheduledTasks.remove(sessionId);
                if (task != null) {
                    task.cancel(true); // 取消任务
                }

                // 清理音频文件
                cleanupAudioFile(sessionId);

                // 更新绘本播放状态
                sessionManager.setMusicPlaying(sessionId, false);

                // 获取会话
                ChatSession session = sessionManager.getSession(sessionId);
                if (session != null) {
                    // 发送停止消息
                    audioService.sendStop(session);
                }
            } catch (Exception e) {
                logger.error("停止绘本播放时发生错误 - SessionId: {}", sessionId, e);
            }
        });
    }
}