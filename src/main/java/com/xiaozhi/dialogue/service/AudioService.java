package com.xiaozhi.dialogue.service;

import com.xiaozhi.communication.common.ChatSession;
import com.xiaozhi.communication.common.SessionManager;
import com.xiaozhi.utils.AudioUtils;
import com.xiaozhi.utils.OpusProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * 音频服务，负责处理音频的流式和非流式发送
 */
@Service
public class AudioService {
    private static final Logger logger = LoggerFactory.getLogger(AudioService.class);

    // 帧发送时间间隔略小于OPUS_FRAME_DURATION_MS，避免因某些调度原因，导致没能在规定时间内发送，设备出现杂音
    private static final long OPUS_FRAME_SEND_INTERVAL_MS = AudioUtils.OPUS_FRAME_DURATION_MS - 2;

    @Autowired
    private OpusProcessor opusProcessor;

    @Autowired
    private SessionManager sessionManager;

    @Autowired
    private MessageService messageService;

    // 使用虚拟线程池处理定时任务
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(
            Runtime.getRuntime().availableProcessors(),
            Thread.ofVirtual().name("audio-scheduler-", 0).factory());

    // 存储会话的回调函数，用于首句流式处理完成后通知
    private final Map<String, Consumer<String>> streamCompletionCallbacks = new ConcurrentHashMap<>();

    // 存储每个会话最后一次发送帧的时间戳
    private final Map<String, AtomicLong> lastFrameSentTime = new ConcurrentHashMap<>();

    // 存储每个会话当前是否正在播放音频
    private final Map<String, AtomicBoolean> isPlaying = new ConcurrentHashMap<>();
    
    // 存储每个会话的流式音频帧队列
    private final Map<String, LinkedBlockingQueue<byte[]>> streamFrameQueues = new ConcurrentHashMap<>();
    
    // 存储每个会话的流式处理状态
    private final Map<String, AtomicBoolean> streamProcessing = new ConcurrentHashMap<>();
    
    // 存储流式TTS请求开始时间
    private final Map<String, Long> streamStartTimes = new ConcurrentHashMap<>();
    
    // 存储首帧发送状态
    private final Map<String, AtomicBoolean> firstFrameSent = new ConcurrentHashMap<>();
    
    // 存储每个会话的调度任务
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    /**
     * 发送TTS开始消息
     */
    public void sendStart(ChatSession session) {
        messageService.sendTtsMessage(session, null, "start");
    }

    /**
     * 发送TTS句子开始消息
     */
    public void sendSentenceStart(ChatSession session, String text) {
        messageService.sendTtsMessage(session, text, "sentence_start");
    }

    /**
     * 发送停止消息
     */
    public CompletableFuture<Void> sendStop(ChatSession session) {
        String sessionId = session.getSessionId();

        try {

            // 如果在播放音乐，则不停止
            if (sessionManager.isMusicPlaying(sessionId)) {
                return CompletableFuture.completedFuture(null);
            }

            // 标记播放结束
            AtomicBoolean playingState = isPlaying.computeIfAbsent(sessionId, k -> new AtomicBoolean());
            playingState.set(false);
            
            // 停止流式处理
            AtomicBoolean processing = streamProcessing.get(sessionId);
            if (processing != null) {
                processing.set(false);
            }
            
            // 取消调度任务
            cancelScheduledTask(sessionId);
            CompletableFuture<Void> sendTtsMessageFuture = CompletableFuture.runAsync(()->messageService.sendTtsMessage(session, null, "stop"));
            // 检查是否需要关闭会话
            if (sessionManager.isCloseAfterChat(sessionId)) {
                sendTtsMessageFuture.thenRun(() -> {
                    sessionManager.closeSession(sessionId);
                });
            }
            sessionManager.setPlaying(sessionId, false);
            return sendTtsMessageFuture;
        } catch (Exception e) {
            logger.error("发送停止消息失败", e);
            AtomicBoolean playingState = isPlaying.computeIfAbsent(sessionId, k -> new AtomicBoolean());
            playingState.set(false);
            return CompletableFuture.completedFuture(null);
        }
    }

    /**
     * 检查会话是否正在播放音频
     */
    public boolean isPlaying(String sessionId) {
        return isPlaying.containsKey(sessionId) && 
               isPlaying.get(sessionId).get();
    }

    /**
     * 发送音频消息
     *
     * @param session   WebSocketSession会话
     * @param sentence  句子对象
     * @param isFirst   是否是开始消息
     * @param isLast    是否是结束消息
     * @return 操作完成的CompletableFuture
     */
    public CompletableFuture<Void> sendAudioMessage(
            ChatSession session,
            DialogueService.Sentence sentence,
            boolean isFirst,
            boolean isLast) {

        String sessionId = session.getSessionId();
        String audioPath = sentence.getAudioPath();
        String text = sentence.getText();

        // 标记开始播放
        AtomicBoolean playingState = isPlaying.computeIfAbsent(sessionId, k -> new AtomicBoolean(true));
        playingState.set(true);

        // 创建一个 CompletableFuture 链来处理整个流程
        CompletableFuture<Void> startFuture = isFirst ? CompletableFuture.runAsync(()->sendStart(session))
                : CompletableFuture.completedFuture(null);
        
        if (audioPath == null) {
            // 如果没有音频路径但是结束消息，发送结束标记
            if (isLast) {
                return startFuture.thenCompose(v -> sendStop(session));
            }
            playingState.set(false);
            return startFuture;
        }
        
        // 使用单独的变量存储播放状态引用
        final AtomicBoolean finalPlayingState = playingState;
        
        // 发送句子开始标记
        CompletableFuture<Void> sentenceStartFuture = startFuture.thenRun(() -> sendSentenceStart(session, text));
        
        // 发送句子表情
        CompletableFuture<Void> emotionFuture = sentenceStartFuture.thenRun(() -> sendSentenceEmotion(session, sentence, null));

        // 处理音频文件
        return emotionFuture.thenCompose(v -> CompletableFuture.supplyAsync(() -> {
            String fullPath = audioPath;
            File audioFile = new File(fullPath);
            if (!audioFile.exists()) {
                logger.warn("音频文件不存在: {}", fullPath);
                return null;
            }

            List<byte[]> opusFrames;

            try {
                if (audioPath.contains(".opus")) {
                    // 如果是opus文件，直接读取opus帧数据
                    opusFrames = opusProcessor.readOpus(audioFile);
                } else {
                    // 如果不是opus文件，按照原来的逻辑处理
                    byte[] audioData = AudioUtils.readAsPcm(fullPath);
                    // 将PCM转换为Opus帧
                    opusFrames = opusProcessor.pcmToOpus(sessionId, audioData, false);
                }
                return opusFrames;
            } catch (Exception e) {
                logger.error("处理音频文件失败: {}", fullPath, e);
                return null;
            }
        })).thenCompose(opusFrames -> {
            if (opusFrames == null || opusFrames.isEmpty()) {
                finalPlayingState.set(false);
                if (isLast) {
                    return sendStop(session);
                }
                return CompletableFuture.completedFuture(null);
            }

            // 确保播放状态为true
            finalPlayingState.set(true);
            
            // 创建发送帧的CompletableFuture
            CompletableFuture<Void> sendFramesFuture = new CompletableFuture<>();
            
            // 创建帧发送任务
            final int[] frameIndex = {0};
            Runnable frameTask = new Runnable() {
                @Override
                public void run() {
                    try {
                        if (!finalPlayingState.get() || frameIndex[0] >= opusFrames.size() || !session.isOpen()) {
                            // 取消调度任务
                            cancelScheduledTask(sessionId);
                            
                            // 完成帧发送Future
                            sendFramesFuture.complete(null);
                            return;
                        }
                        
                        // 更新活跃时间
                        sessionManager.updateLastActivity(sessionId);
                        
                        // 发送当前帧
                        byte[] frame = opusFrames.get(frameIndex[0]++);
                        sendOpusFrame(session, frame);
                        
                    } catch (Exception e) {
                        // 发生错误，取消调度任务
                        cancelScheduledTask(sessionId);
                        
                        // 完成帧发送Future（带异常）
                        sendFramesFuture.completeExceptionally(e);
                    }
                }
            };
            
            // 启动定时任务，每隔OPUS_FRAME_SEND_INTERVAL_MS毫秒执行一次
            ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(
                frameTask, 
                0, 
                OPUS_FRAME_SEND_INTERVAL_MS, 
                TimeUnit.MILLISECONDS
            );
            
            // 存储任务引用，以便稍后取消
            scheduledTasks.put(sessionId, task);
            
            // 返回帧发送Future
            return sendFramesFuture;
        }).whenComplete((result, error) -> {
            // 无论成功还是失败，都标记播放结束
            finalPlayingState.set(false);
            
            // 取消调度任务
            cancelScheduledTask(sessionId);
        }).thenCompose(v -> {
            // 发送停止消息（只有在isLast为true时才发送）
            if (isLast) {
                return sendStop(session);
            }
            return CompletableFuture.completedFuture(null);
        }).exceptionally(error -> {
            logger.error("发送音频消息失败", error);
            
            // 如果发生错误但仍然是结束消息，确保发送stop
            if (isLast) {
                try {
                    sendStop(session);
                } catch (Exception e) {
                    logger.error("发送停止消息失败", e);
                }
            }
            return null;
        });
    }

    /**
     * 发送表情信息。如果句子里没有分析出表情，则默认返回 happy
     */
    private void sendSentenceEmotion(ChatSession session, DialogueService.Sentence sentence, String defaultEmotion) {
        List<String> moods = sentence.getMoods();
        if (moods != null && !moods.isEmpty()) {
            messageService.sendEmotion(session, moods.get(0));
        } else if (defaultEmotion != null) {
            messageService.sendEmotion(session, defaultEmotion);
        }
    }

    /**
     * 发送Opus帧数据
     * 
     * @param session WebSocket会话
     * @param opusFrame Opus编码的音频帧
     * @throws IOException 如果发送失败
     */
    public void sendOpusFrame(ChatSession session, byte[] opusFrame) throws IOException {
        messageService.sendBinaryMessage(session, opusFrame);
    }

    /**
     * 取消调度任务
     */
    public void cancelScheduledTask(String sessionId) {
        ScheduledFuture<?> task = scheduledTasks.remove(sessionId);
        if (task != null && !task.isDone()) {
            task.cancel(false);
        }
    }


    /**
     * 清理会话资源
     */
    public void cleanupSession(String sessionId) {
        lastFrameSentTime.remove(sessionId);
        streamCompletionCallbacks.remove(sessionId);
        isPlaying.remove(sessionId);
        streamFrameQueues.remove(sessionId);
        streamProcessing.remove(sessionId);
        streamStartTimes.remove(sessionId);
        firstFrameSent.remove(sessionId);
        cancelScheduledTask(sessionId);
        opusProcessor.cleanup(sessionId);
    }
}