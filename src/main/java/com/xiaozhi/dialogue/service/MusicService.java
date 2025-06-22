package com.xiaozhi.dialogue.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaozhi.communication.common.ChatSession;
import com.xiaozhi.communication.common.SessionManager;
import com.xiaozhi.utils.AudioUtils;
import com.xiaozhi.utils.OpusProcessor;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 音乐服务，负责处理音乐播放和歌词同步
 * 使用JDK 21虚拟线程和结构化并发实现异步处理
 */
@Service
public class MusicService {
    private static final Logger logger = LoggerFactory.getLogger(MusicService.class);

    private static final long OPUS_FRAME_INTERVAL_MS = AudioUtils.OPUS_FRAME_DURATION_MS;
    private static final String API_BASE_URL = "http://www.jsrc.top:5566";

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
            Thread.ofVirtual().name("music-scheduler-", 0).factory());

    // 存储每个会话的当前歌词信息
    private final Map<String, List<LyricLine>> sessionLyrics = new ConcurrentHashMap<>();

    // 存储每个会话的当前播放时间
    private final Map<String, AtomicLong> playTime = new ConcurrentHashMap<>();

    // 存储每个会话的音频文件路径，用于播放完成后删除
    private final Map<String, String> sessionAudioFiles = new ConcurrentHashMap<>();

    // 存储每个会话的调度任务
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    /**
     * 歌词行数据结构 - 使用JDK 16+ Record类型
     */
    private record LyricLine(long timeMs, String text) {
    }

    /**
     * 搜索并播放音乐
     * 使用JDK 21虚拟线程和结构化并发实现异步处理
     * 
     * @param session WebSocketSession会话
     * @param song    歌曲名
     * @param artist  艺术家名（可选）
     */
    public void playMusic(ChatSession session, String song, String artist) {
        String sessionId = session.getSessionId();

        // 使用虚拟线程处理异步任务
        Thread.startVirtualThread(() -> {
            try {
                // 设置音乐播放状态
                sessionManager.setMusicPlaying(sessionId, true);

                // 重置播放时间
                playTime.computeIfAbsent(sessionId, k -> new AtomicLong()).set(0);

                // 清理之前的音频文件（如果有）
                cleanupAudioFile(sessionId);

                // 1. 获取音乐信息
                Map<String, String> musicInfo = getMusicInfo(song, artist);
                if (musicInfo == null) {
                    throw new RuntimeException("无法找到歌曲: " + song + (artist != null ? " - " + artist : ""));
                }

                // 2. 下载音频文件到本地临时目录，使用随机文件名避免冲突
                String audioUrl = musicInfo.get("audioUrl");
                String randomName = "music_" + sessionId + "_" + UUID.randomUUID() + ".mp3";
                String audioPath = downloadFile(audioUrl, randomName);

                if (audioPath == null) {
                    throw new RuntimeException("下载音频文件失败");
                }

                // 保存音频文件路径，用于播放完成后删除
                sessionAudioFiles.put(sessionId, audioPath);

                // 3. 解析歌词
                String lyricUrl = musicInfo.get("lyricUrl");
                List<LyricLine> lyrics = parseLyrics(lyricUrl);
                sessionLyrics.put(sessionId, lyrics);

                // 发送音乐开始消息
                audioService.sendStart(session);

                // 发送音频和同步歌词
                sendAudioWithLyrics(session, audioPath);

            } catch (Exception e) {

                try {
                    audioService.sendSentenceStart(session, "播放音乐时发生错误: " + e.getMessage());
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
        // 清理会话的歌词数据
        sessionLyrics.remove(sessionId);
    }

    /**
     * 发送音频和同步歌词
     */
    private void sendAudioWithLyrics(ChatSession session, String audioPath) {
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

            // 获取歌词
            List<LyricLine> lyrics = sessionLyrics.getOrDefault(sessionId, Collections.emptyList());
            AtomicLong currPlayTime = playTime.computeIfAbsent(sessionId, k -> new AtomicLong(0));

            // 预处理歌词时间点，将毫秒时间转换为帧索引
            Map<Integer, String> lyricFrameMap = new HashMap<>();
            for (LyricLine line : lyrics) {
                // 计算歌词对应的帧索引
                int frameIndex = (int) (line.timeMs() / OPUS_FRAME_INTERVAL_MS);
                if (frameIndex < frames.size()) {
                    lyricFrameMap.put(frameIndex, line.text());
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

                    // 先检查是否有对应这一帧的歌词需要发送
                    String lyricText = lyricFrameMap.get(currentIndex);
                    if (lyricText != null) {
                        audioService.sendSentenceStart(session, lyricText);
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
     * 获取音乐信息（音频URL和歌词URL）
     */
    private Map<String, String> getMusicInfo(String song, String artist) {
        try {
            // 构建URL
            StringBuilder urlBuilder = new StringBuilder(API_BASE_URL + "/stream_pcm?song=" +
                    URLEncoder.encode(song, StandardCharsets.UTF_8));

            if (artist != null && !artist.isEmpty()) {
                urlBuilder.append("&artist=").append(URLEncoder.encode(artist, StandardCharsets.UTF_8));
            }

            // 使用OkHttp3发送请求
            Request request = new Request.Builder()
                    .url(urlBuilder.toString())
                    .get()
                    .build();

            try (Response response = okHttpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    logger.error("获取音乐信息失败，响应码: {}", response.code());
                    return null;
                }

                // 解析JSON响应
                String responseBody = response.body() != null ? response.body().string() : null;
                if (responseBody == null) {
                    logger.error("获取音乐信息失败，响应体为空");
                    return null;
                }

                Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);

                Map<String, String> result = new HashMap<>();

                // 检查API响应格式，支持两种可能的字段名
                String audioPath = (String) responseMap.get("audioPath");
                String audioUrl = (String) responseMap.get("audio_url");

                String lyricPath = (String) responseMap.get("lyricPath");
                String lyricUrl = (String) responseMap.get("lyric_url");

                // 优先使用直接URL，否则构建URL
                if (audioUrl != null && !audioUrl.isEmpty()) {
                    if (!audioUrl.startsWith("http")) {
                        audioUrl = API_BASE_URL + audioUrl;
                    }
                    result.put("audioUrl", audioUrl);
                } else if (audioPath != null && !audioPath.isEmpty()) {
                    result.put("audioUrl", API_BASE_URL + "/get_file?path=" +
                            URLEncoder.encode(audioPath, StandardCharsets.UTF_8) +
                            "&name=" + URLEncoder.encode(song + ".mp3", StandardCharsets.UTF_8));
                } else {
                    logger.error("API响应中缺少音频URL信息");
                    return null;
                }

                if (lyricUrl != null && !lyricUrl.isEmpty()) {
                    if (!lyricUrl.startsWith("http")) {
                        lyricUrl = API_BASE_URL + lyricUrl;
                    }
                    result.put("lyricUrl", lyricUrl);
                } else if (lyricPath != null && !lyricPath.isEmpty()) {
                    result.put("lyricUrl", API_BASE_URL + "/get_file?path=" +
                            URLEncoder.encode(lyricPath, StandardCharsets.UTF_8) +
                            "&name=" + URLEncoder.encode(song + ".lrc", StandardCharsets.UTF_8));
                } else {
                    // 歌词可选，没有歌词也可以播放
                    logger.warn("API响应中缺少歌词URL信息");
                }

                return result;
            }
        } catch (Exception e) {
            logger.error("获取音乐信息时发生错误", e);
            return null;
        }
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
     * 解析LRC格式歌词
     */
    private List<LyricLine> parseLyrics(String lyricUrl) {
        List<LyricLine> result = new ArrayList<>();

        if (lyricUrl == null || lyricUrl.isEmpty()) {
            logger.warn("歌词URL为空，无法解析歌词");
            return result;
        }

        try {

            // 使用OkHttp3发送请求
            Request request = new Request.Builder()
                    .url(lyricUrl)
                    .get()
                    .build();

            try (Response response = okHttpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    logger.error("获取歌词失败，响应码: {}", response.code());
                    return result;
                }

                String responseBody = response.body().string();

                // LRC时间标签正则表达式: [mm:ss.xx]
                Pattern pattern = Pattern.compile("\\[(\\d{2}):(\\d{2})\\.(\\d{2})\\](.*)");

                // 使用Stream API处理每一行
                return responseBody.lines()
                        .map(pattern::matcher)
                        .filter(Matcher::find)
                        .map(matcher -> {
                            int minutes = Integer.parseInt(matcher.group(1));
                            int seconds = Integer.parseInt(matcher.group(2));
                            int hundredths = Integer.parseInt(matcher.group(3));

                            // 计算毫秒时间
                            long timeMs = (minutes * 60 * 1000) + (seconds * 1000) + (hundredths * 10);
                            String text = matcher.group(4).trim();

                            return new LyricLine(timeMs, text);
                        })
                        .sorted(Comparator.comparingLong(LyricLine::timeMs))
                        .toList();
            }

        } catch (Exception e) {
            logger.error("解析歌词时发生错误", e);
        }

        return result;
    }

    /**
     * 停止播放音乐
     * 
     * @param sessionId 会话ID
     */
    public void stopMusic(String sessionId) {
        Thread.startVirtualThread(() -> {
            try {
                ScheduledFuture<?> task = scheduledTasks.remove(sessionId);
                if (task != null) {
                    task.cancel(true); // 取消任务
                }

                // 清理音频文件
                cleanupAudioFile(sessionId);

                // 更新音乐播放状态
                sessionManager.setMusicPlaying(sessionId, false);

                // 获取会话
                ChatSession session = sessionManager.getSession(sessionId);
                if (session != null) {
                    // 发送停止消息
                    audioService.sendStop(session);
                }
            } catch (Exception e) {
                logger.error("停止音乐播放时发生错误 - SessionId: {}", sessionId, e);
            }
        });
    }
}