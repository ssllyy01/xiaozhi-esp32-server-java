package com.xiaozhi.dialogue.service;

import com.xiaozhi.communication.common.SessionManager;
import com.xiaozhi.dialogue.vad.impl.SileroVadModel;
import com.xiaozhi.entity.SysDevice;
import com.xiaozhi.entity.SysRole;
import com.xiaozhi.service.SysRoleService;
import com.xiaozhi.utils.AutomaticGainControl;
import com.xiaozhi.utils.OpusProcessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class VadService {
    private static final Logger logger = LoggerFactory.getLogger(VadService.class);

    @Autowired
    private OpusProcessor opusProcessor;

    @Autowired
    private SileroVadModel vadModel;

    @Autowired
    private SessionManager sessionManager;

    @Autowired
    private SysRoleService roleService;

    @Autowired
    private AutomaticGainControl agc;

    // 语音检测前缓冲时长(毫秒)
    private int preBufferMs = 500;

    // 会话状态和锁
    private final ConcurrentHashMap<String, VadState> states = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();

    // 最小PCM长度阈值(约100ms的16kHz 16bit音频)
    private static final int MIN_PCM_LENGTH = 1600;

    // VAD模型所需的样本大小
    private static final int VAD_SAMPLE_SIZE = 512;

    @PostConstruct
    public void init() {
        try {
            if (vadModel != null) {
                logger.info("VAD服务初始化成功");
            } else {
                logger.error("SileroVadModel未注入，VAD功能不可用");
            }
            
            if (agc != null) {
                logger.info("AGC服务初始化成功");
            } else {
                logger.warn("AGC服务未注入，将跳过自动增益控制");
            }
        } catch (Exception e) {
            logger.error("初始化VAD服务失败", e);
        }
    }

    @PreDestroy
    public void cleanup() {
        logger.info("VAD服务资源已释放");
        states.clear();
        locks.clear();
    }

    /**
     * 简化的会话状态类
     */
    private class VadState {
        // 语音状态
        private boolean speaking = false;
        private long speechTime = 0;
        private long silenceTime = 0;

        // 音频分析
        private float avgEnergy = 0;
        private final List<Float> probs = new ArrayList<>();
        
        // 添加原始VAD概率列表
        private final List<Float> originalProbs = new ArrayList<>();
        
        // 帧计数器（用于每10帧输出一次）
        private int frameCounter = 0;

        // 预缓冲
        private final LinkedList<byte[]> preBuffer = new LinkedList<>();
        private int preBufferSize = 0;
        private final int maxPreBufferSize;

        // 音频数据
        private final List<byte[]> pcmData = new ArrayList<>();
        private final List<byte[]> opusData = new ArrayList<>();

        // 短帧累积
        private final ByteArrayOutputStream pcmAccumulator = new ByteArrayOutputStream();
        private long lastAccumTime = 0;

        // AGC相关统计
        private String detectedDeviceType = "normal";
        private int lowQualityFrameCount = 0;
        private int totalFrameCount = 0;

        public VadState() {
            this.maxPreBufferSize = preBufferMs * 32; // 16kHz, 16bit, mono = 32 bytes/ms
            this.lastAccumTime = System.currentTimeMillis();
        }

        public boolean isSpeaking() {
            return speaking;
        }

        public void setSpeaking(boolean speaking) {
            this.speaking = speaking;
            if (speaking) {
                speechTime = System.currentTimeMillis();
                silenceTime = 0;
            } else if (silenceTime == 0) {
                silenceTime = System.currentTimeMillis();
            }
        }

        public int getSilenceDuration() {
            return silenceTime == 0 ? 0 : (int) (System.currentTimeMillis() - silenceTime);
        }

        public void updateSilence(boolean isSilent) {
            if (isSilent) {
                if (silenceTime == 0) {
                    silenceTime = System.currentTimeMillis();
                }
            } else {
                silenceTime = 0;
            }
        }

        public void updateEnergy(float energy) {
            avgEnergy = (avgEnergy == 0) ? energy : 0.95f * avgEnergy + 0.05f * energy;
        }

        public float getAvgEnergy() {
            return avgEnergy;
        }

        public void addProb(float prob) {
            probs.add(prob);
            if (probs.size() > 10) {
                probs.remove(0);
            }
            
            // 更新设备质量统计
            totalFrameCount++;
            if (prob < 0.3f) {
                lowQualityFrameCount++;
            }
            
            // 每50帧重新评估设备类型
            if (totalFrameCount % 50 == 0) {
                updateDeviceType();
            }
        }
        
        // 添加原始VAD概率
        public void addOriginalProb(float prob) {
            originalProbs.add(prob);
            if (originalProbs.size() > 10) {
                originalProbs.remove(0);
            }
            
            // 增加帧计数器
            frameCounter++;
        }
        
        public float getLastOriginalProb() {
            return originalProbs.isEmpty() ? 0.0f : originalProbs.get(originalProbs.size() - 1);
        }

        public float getLastProb() {
            return probs.isEmpty() ? 0.0f : probs.get(probs.size() - 1);
        }

        public List<Float> getProbs() {
            return probs;
        }
        
        public int getFrameCounter() {
            return frameCounter;
        }

        public String getDetectedDeviceType() {
            return detectedDeviceType;
        }

        private void updateDeviceType() {
            if (totalFrameCount < 10) {
                return; // 样本不足
            }
            
            float lowQualityRatio = (float) lowQualityFrameCount / totalFrameCount;
            float avgProb = 0;
            for (Float prob : probs) {
                avgProb += prob;
            }
            avgProb /= probs.size();
            
            if (lowQualityRatio > 0.7f || avgProb < 0.2f) {
                detectedDeviceType = "low_quality_mic";
            } else if (lowQualityRatio < 0.2f && avgProb > 0.6f) {
                detectedDeviceType = "high_quality_mic";
            } else if (avgEnergy < 0.001f) {
                detectedDeviceType = "weak_signal";
            } else {
                detectedDeviceType = "normal";
            }
        }

        // 预缓冲区管理
        public void addToPreBuffer(byte[] data) {
            if (speaking)
                return;

            preBuffer.add(data.clone());
            preBufferSize += data.length;

            while (preBufferSize > maxPreBufferSize && !preBuffer.isEmpty()) {
                byte[] removed = preBuffer.removeFirst();
                preBufferSize -= removed.length;
            }
        }

        public byte[] drainPreBuffer() {
            if (preBuffer.isEmpty())
                return new byte[0];

            byte[] result = new byte[preBufferSize];
            int offset = 0;

            for (byte[] chunk : preBuffer) {
                System.arraycopy(chunk, 0, result, offset, chunk.length);
                offset += chunk.length;
            }

            preBuffer.clear();
            preBufferSize = 0;
            return result;
        }

        // 累积缓冲区管理
        public void accumulate(byte[] pcm) {
            if (pcm != null && pcm.length > 0) {
                try {
                    pcmAccumulator.write(pcm);
                    lastAccumTime = System.currentTimeMillis();
                } catch (Exception e) {
                    logger.error("累积PCM数据失败", e);
                }
            }
        }

        public byte[] drainAccumulator() {
            byte[] result = pcmAccumulator.toByteArray();
            pcmAccumulator.reset();
            return result;
        }

        public int getAccumSize() {
            return pcmAccumulator.size();
        }

        public boolean isAccumTimedOut() {
            return System.currentTimeMillis() - lastAccumTime > 300;
        }

        // 音频数据管理
        public void addPcm(byte[] pcm) {
            if (pcm != null && pcm.length > 0) {
                pcmData.add(pcm.clone());
            }
        }

        public void addOpus(byte[] opus) {
            if (opus != null && opus.length > 0) {
                opusData.add(opus.clone());
            }
        }

        public List<byte[]> getPcmData() {
            return new ArrayList<>(pcmData);
        }

        public List<byte[]> getOpusData() {
            return new ArrayList<>(opusData);
        }

        public void reset() {
            speaking = false;
            speechTime = 0;
            silenceTime = 0;
            avgEnergy = 0;
            probs.clear();
            originalProbs.clear(); // 重置原始概率列表
            frameCounter = 0;      // 重置帧计数器
            preBuffer.clear();
            preBufferSize = 0;
            pcmData.clear();
            opusData.clear();
            pcmAccumulator.reset();
            lastAccumTime = System.currentTimeMillis();
            
            // 重置AGC相关统计
            detectedDeviceType = "normal";
            lowQualityFrameCount = 0;
            totalFrameCount = 0;
        }
    }

    /**
     * 初始化会话
     */
    public void initSession(String sessionId) {
        Object lock = getLock(sessionId);
        synchronized (lock) {
            VadState state = states.get(sessionId);
            if (state == null) {
                state = new VadState();
                states.put(sessionId, state);
            } else {
                state.reset();
            }
            
            // 重置AGC状态
            if (agc != null) {
                agc.resetSession(sessionId);
            }
            
            logger.info("VAD会话已初始化: {}", sessionId);
        }
    }

    /**
     * 检查会话是否已初始化
     */
    public boolean isSessionInitialized(String sessionId) {
        Object lock = getLock(sessionId);
        synchronized (lock) {
            return states.containsKey(sessionId);
        }
    }

    /**
     * 获取会话锁
     */
    private Object getLock(String sessionId) {
        return locks.computeIfAbsent(sessionId, k -> new Object());
    }

    /**
     * 处理音频数据
     */
    public VadResult processAudio(String sessionId, byte[] opusData) {

        if (!isSessionInitialized(sessionId)) {
            return null;
        }

        Object lock = getLock(sessionId);

        // 获取设备配置
        SysDevice device = sessionManager.getDeviceConfig(sessionId);
        // 添加空值检查，使用默认值
        float speechThreshold = 0.3f;
        float silenceThreshold = 0.2f;
        float energyThreshold = 0.001f;
        int silenceTimeoutMs = 1200;

        if (device != null && device.getRoleId() != null) {
            SysRole role = roleService.selectRoleById(device.getRoleId());
            speechThreshold = Optional.ofNullable(role.getVadSpeechTh()).orElse(speechThreshold);
            silenceThreshold = Optional.ofNullable(role.getVadSilenceTh()).orElse(silenceThreshold);
            energyThreshold = Optional.ofNullable(role.getVadEnergyTh()).orElse(energyThreshold);
            silenceTimeoutMs = Optional.ofNullable(role.getVadSilenceMs()).orElse(silenceTimeoutMs);
        }

        synchronized (lock) {
            try {
                // 获取会话状态
                VadState state = states.computeIfAbsent(sessionId, k -> new VadState());

                // 保存原始Opus数据
                state.addOpus(opusData);

                // 解码Opus数据
                byte[] pcmData;
                try {
                    pcmData = opusProcessor.opusToPcm(sessionId, opusData);
                    if (pcmData == null || pcmData.length == 0) {
                        return new VadResult(VadStatus.NO_SPEECH, null);
                    }
                } catch (Exception e) {
                    logger.error("Opus解码失败: {}", e.getMessage());
                    return new VadResult(VadStatus.ERROR, null);
                }

                // 分析原始音频
                float[] originalSamples = bytesToFloats(pcmData);
                float originalEnergy = calcEnergy(originalSamples);
                
                // 获取原始VAD概率
                float originalSpeechProb = detectSpeech(originalSamples);
                state.addOriginalProb(originalSpeechProb);

                // ========== 应用 AGC ==========
                String deviceType = state.getDetectedDeviceType();
                byte[] originalPcm = pcmData.clone(); // 保留原始数据用于对比
                
                // 检查AGC是否可用
                if (agc != null) {
                    pcmData = agc.process(sessionId, pcmData, deviceType);
                }
                
                // 获取AGC统计信息
                AutomaticGainControl.AgcStats agcStats = agc != null ? agc.getStats(sessionId) : new AutomaticGainControl.AgcStats();
                
                // 根据AGC增益动态调整VAD阈值
                float adjustedSpeechThreshold = adjustVadThreshold(speechThreshold, agcStats);
                float adjustedSilenceThreshold = adjustVadThreshold(silenceThreshold, agcStats);

                // 添加到预缓冲区
                state.addToPreBuffer(pcmData);

                // 处理短帧数据
                if (pcmData.length < MIN_PCM_LENGTH && !state.isSpeaking()) {
                    state.accumulate(pcmData);

                    // 检查是否需要继续累积
                    if (state.getAccumSize() < MIN_PCM_LENGTH && !state.isAccumTimedOut()) {
                        return new VadResult(VadStatus.NO_SPEECH, null);
                    }

                    // 处理累积的数据
                    pcmData = state.drainAccumulator();
                    if (pcmData.length == 0) {
                        return new VadResult(VadStatus.NO_SPEECH, null);
                    }
                    
                }

                // 分析AGC处理后的音频
                float[] samples = bytesToFloats(pcmData);
                float energy = calcEnergy(samples);
                state.updateEnergy(energy);

                // VAD推断（AGC后）
                float speechProb = detectSpeech(samples);
                state.addProb(speechProb);

                // 判断语音状态
                boolean hasEnergy = energy > state.getAvgEnergy() * 1.5 && energy > energyThreshold;
                boolean isSpeech = speechProb > adjustedSpeechThreshold && hasEnergy;
                boolean isSilence = speechProb < adjustedSilenceThreshold;
                state.updateSilence(isSilence);

                // 处理状态转换
                if (!state.isSpeaking() && isSpeech) {
                    // 语音开始
                    state.pcmData.clear();
                    state.setSpeaking(true);
                    
                    // 记录AGC和设备信息
                    String agcInfo = "";
                    agcInfo = String.format(", AGC增益: %.2f, 设备类型: %s", 
                                            agcStats.gain, state.getDetectedDeviceType());
                    
                    logger.info("检测到语音开始 - SessionId: {}, 概率: {}, 原始概率: {}, 能量: {}, " +
                              "调整后阈值: {}{}", 
                              sessionId, speechProb, originalSpeechProb, energy, adjustedSpeechThreshold, agcInfo);

                    // 获取预缓冲数据
                    byte[] preBufferData = state.drainPreBuffer();
                    byte[] result;

                    if (preBufferData.length > 0) {
                        // 使用改进的平滑连接方法
                        List<byte[]> toJoin = new ArrayList<>();
                        toJoin.add(preBufferData);
                        toJoin.add(pcmData);
                        result = opusProcessor.smoothJoinPcm(toJoin);
                        state.addPcm(result);
                    } else {
                        result = pcmData;
                        state.addPcm(pcmData);
                    }

                    return new VadResult(VadStatus.SPEECH_START, result);
                } else if (state.isSpeaking() && isSilence) {
                    // 检查静音时长
                    int silenceDuration = state.getSilenceDuration();
                    if (silenceDuration > silenceTimeoutMs) {
                        // 语音结束
                        state.setSpeaking(false);
                        logger.info("语音结束: {}, 静音: {}ms", sessionId, silenceDuration);
                        return new VadResult(VadStatus.SPEECH_END, pcmData);
                    } else {
                        // 继续收集
                        state.addPcm(pcmData);
                        return new VadResult(VadStatus.SPEECH_CONTINUE, pcmData);
                    }
                } else if (state.isSpeaking()) {
                    // 语音继续
                    state.addPcm(pcmData);
                    return new VadResult(VadStatus.SPEECH_CONTINUE, pcmData);
                } else {
                    // 无语音
                    return new VadResult(VadStatus.NO_SPEECH, null);
                }
            } catch (Exception e) {
                logger.error("处理音频失败: {}, 错误: {}", sessionId, e.getMessage(), e);
                return new VadResult(VadStatus.ERROR, null);
            }
        }
    }

    /**
     * 根据AGC统计信息调整VAD阈值
     */
    private float adjustVadThreshold(float baseThreshold, AutomaticGainControl.AgcStats agcStats) {
        float gainFactor = agcStats.gain;
        float snr = agcStats.snr;
        
        // 基于增益的调整
        float gainAdjustment = 1.0f;
        if (gainFactor > 10.0f) {
            // 非常高的增益，大幅降低阈值
            gainAdjustment = 0.5f;
        } else if (gainFactor > 5.0f) {
            // 高增益，降低阈值
            gainAdjustment = 0.7f;
        } else if (gainFactor > 2.0f) {
            // 中等增益，略微降低阈值
            gainAdjustment = 0.85f;
        }
        
        // 基于信噪比的调整
        float snrAdjustment = 1.0f;
        if (snr < 2.0f) {
            // 低信噪比，进一步降低阈值
            snrAdjustment = 0.8f;
        } else if (snr > 10.0f) {
            // 高信噪比，可以提高阈值
            snrAdjustment = 1.1f;
        }
        
        float adjustedThreshold = baseThreshold * gainAdjustment * snrAdjustment;
        
        // 确保阈值在合理范围内
        return Math.max(0.05f, Math.min(0.8f, adjustedThreshold));
    }

    /**
     * 执行语音检测
     */
    private float detectSpeech(float[] samples) {
        if (vadModel == null || samples == null || samples.length == 0) {
            return 0.0f;
        }

        try {
            // 处理样本大小
            if (samples.length == VAD_SAMPLE_SIZE) {
                return vadModel.getSpeechProbability(samples);
            }

            // 样本不足，需要填充
            if (samples.length < VAD_SAMPLE_SIZE) {
                float[] padded = new float[VAD_SAMPLE_SIZE];
                System.arraycopy(samples, 0, padded, 0, samples.length);
                return vadModel.getSpeechProbability(padded);
            }

            // 样本过长，分段处理
            float maxProb = 0.0f;
            for (int offset = 0; offset <= samples.length - VAD_SAMPLE_SIZE; offset += VAD_SAMPLE_SIZE / 2) {
                float[] chunk = new float[VAD_SAMPLE_SIZE];
                System.arraycopy(samples, offset, chunk, 0, VAD_SAMPLE_SIZE);
                float prob = vadModel.getSpeechProbability(chunk);
                maxProb = Math.max(maxProb, prob);
            }
            return maxProb;
        } catch (Exception e) {
            logger.error("VAD推断失败: {}", e.getMessage());
            return 0.0f;
        }
    }

    /**
     * 字节数组转浮点数组
     */
    private float[] bytesToFloats(byte[] pcmData) {
        int sampleCount = pcmData.length / 2;
        float[] samples = new float[sampleCount];

        ByteBuffer buffer = ByteBuffer.wrap(pcmData).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < sampleCount; i++) {
            short sample = buffer.getShort();
            samples[i] = sample / 32768.0f; // 归一化到[-1,1]
        }

        return samples;
    }

    /**
     * 计算音频能量
     */
    private float calcEnergy(float[] samples) {
        float sum = 0;
        for (float sample : samples) {
            sum += Math.abs(sample);
        }
        return sum / samples.length;
    }

    /**
     * 重置会话
     */
    public void resetSession(String sessionId) {
        Object lock = getLock(sessionId);
        synchronized (lock) {
            VadState state = states.get(sessionId);
            if (state != null) {
                state.reset();
            }
            states.remove(sessionId);
            locks.remove(sessionId);
            
            // 重置AGC状态
            if (agc != null) {
                agc.resetSession(sessionId);
            }
        }
    }

    /**
     * 检查是否正在说话
     */
    public boolean isSpeaking(String sessionId) {
        Object lock = getLock(sessionId);
        synchronized (lock) {
            VadState state = states.get(sessionId);
            return state != null && state.isSpeaking();
        }
    }

    /**
     * 获取当前语音概率
     */
    public float getSpeechProbability(String sessionId) {
        Object lock = getLock(sessionId);
        synchronized (lock) {
            VadState state = states.get(sessionId);
            return state != null ? state.getLastProb() : 0.0f;
        }
    }
    
    /**
     * 获取原始语音概率
     */
    public float getOriginalSpeechProbability(String sessionId) {
        Object lock = getLock(sessionId);
        synchronized (lock) {
            VadState state = states.get(sessionId);
            return state != null ? state.getLastOriginalProb() : 0.0f;
        }
    }

    /**
     * 获取音频数据
     */
    public List<byte[]> getPcmData(String sessionId) {
        Object lock = getLock(sessionId);
        synchronized (lock) {
            VadState state = states.get(sessionId);
            return state != null ? state.getPcmData() : new ArrayList<>();
        }
    }

    /**
     * 获取Opus数据
     */
    public List<byte[]> getOpusData(String sessionId) {
        Object lock = getLock(sessionId);
        synchronized (lock) {
            VadState state = states.get(sessionId);
            return state != null ? state.getOpusData() : new ArrayList<>();
        }
    }

    /**
     * 获取AGC统计信息
     */
    public AutomaticGainControl.AgcStats getAgcStats(String sessionId) {
        return agc != null ? agc.getStats(sessionId) : new AutomaticGainControl.AgcStats();
    }

    /**
     * 获取检测到的设备类型
     */
    public String getDetectedDeviceType(String sessionId) {
        Object lock = getLock(sessionId);
        synchronized (lock) {
            VadState state = states.get(sessionId);
            return state != null ? state.getDetectedDeviceType() : "normal";
        }
    }

    /**
     * 手动设置设备类型（用于测试或特殊情况）
     */
    public void setDeviceType(String sessionId, String deviceType) {
        Object lock = getLock(sessionId);
        synchronized (lock) {
            VadState state = states.get(sessionId);
            if (state != null) {
                state.detectedDeviceType = deviceType;
                logger.info("手动设置设备类型 - SessionId: {}, 类型: {}", sessionId, deviceType);
            }
        }
    }

    /**
     * 获取当前帧计数
     */
    public int getFrameCounter(String sessionId) {
        Object lock = getLock(sessionId);
        synchronized (lock) {
            VadState state = states.get(sessionId);
            return state != null ? state.getFrameCounter() : 0;
        }
    }

    /**
     * VAD状态枚举
     */
    public enum VadStatus {
        NO_SPEECH, // 无语音
        SPEECH_START, // 语音开始
        SPEECH_CONTINUE, // 语音继续
        SPEECH_END, // 语音结束
        ERROR // 处理错误
    }

    /**
     * VAD结果类
     */
    public static class VadResult {
        private final VadStatus status;
        private final byte[] data;

        public VadResult(VadStatus status, byte[] data) {
            this.status = status;
            this.data = data;
        }

        public VadStatus getStatus() {
            return status;
        }

        public byte[] getProcessedData() {
            return data;
        }

        public boolean isSpeechActive() {
            return status == VadStatus.SPEECH_START || status == VadStatus.SPEECH_CONTINUE;
        }

        public boolean isSpeechEnd() {
            return status == VadStatus.SPEECH_END;
        }
    }
}