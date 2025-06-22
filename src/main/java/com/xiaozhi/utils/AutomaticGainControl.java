package com.xiaozhi.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 自动增益控制 (AGC) 组件
 * 用于自动调整音频信号的增益，使不同设备的音频信号保持在合适的范围
 */
@Component
public class AutomaticGainControl {
    private static final Logger logger = LoggerFactory.getLogger(AutomaticGainControl.class);
    
    // AGC 参数 - 更激进的设置
    private static final float DEFAULT_TARGET_LEVEL = 0.1f;   // 目标电平 - 提高目标电平
    private static final float DEFAULT_MAX_GAIN = 40.0f;      // 最大增益 - 大幅提高最大增益
    private static final float DEFAULT_MIN_GAIN = 2.0f;       // 最小增益 - 提高最小增益
    private static final float ATTACK_TIME = 0.0005f;         // 攻击时间 (秒) - 更快的攻击速度
    private static final float RELEASE_TIME = 0.02f;          // 释放时间 (秒) - 更快的释放速度
    private static final int SAMPLE_RATE = AudioUtils.SAMPLE_RATE; // 采样率
    
    // 每个会话的 AGC 状态
    private final ConcurrentHashMap<String, AgcState> sessionStates = new ConcurrentHashMap<>();
    
    /**
     * AGC 状态类
     */
    private static class AgcState {
        float currentGain = 5.0f;  // 初始增益设为5.0，确保从一开始就有一定增益
        float smoothedLevel = 0.0f;
        float peakLevel = 0.0f;
        long frameCount = 0;
        
        // 设备自适应参数
        float targetLevel;
        float maxGain;
        float minGain;
        
        // 噪声底估计
        float noiseFloor = 0.0001f;  // 初始噪声底设为一个小的非零值
        float[] recentLevels = new float[50];
        int levelIndex = 0;
        
        // 增益调整历史
        float[] recentGains = new float[10];
        int gainIndex = 0;
        
        public AgcState(float targetLevel, float maxGain, float minGain) {
            this.targetLevel = targetLevel;
            this.maxGain = maxGain;
            this.minGain = minGain;
            
            // 初始化增益历史
            for (int i = 0; i < recentGains.length; i++) {
                recentGains[i] = 5.0f;
            }
        }
        
        public void updateNoiseFloor(float level) {
            recentLevels[levelIndex % recentLevels.length] = level;
            levelIndex++;
            
            if (levelIndex >= recentLevels.length) {
                // 计算最低的 20% 作为噪声底
                float[] sorted = recentLevels.clone();
                java.util.Arrays.sort(sorted);
                int count = recentLevels.length / 5;
                float sum = 0;
                for (int i = 0; i < count; i++) {
                    sum += sorted[i];
                }
                noiseFloor = Math.max(0.00001f, sum / count);  // 确保噪声底不会太小
            }
        }
        
        public void updateGainHistory(float gain) {
            recentGains[gainIndex % recentGains.length] = gain;
            gainIndex++;
        }
        
        public float getAverageGain() {
            float sum = 0;
            for (float gain : recentGains) {
                sum += gain;
            }
            return sum / recentGains.length;
        }
    }
    
    /**
     * 处理音频数据
     * @param sessionId 会话ID
     * @param pcmData PCM音频数据
     * @param deviceType 设备类型 (可选，用于设备特定的AGC参数)
     * @return 处理后的PCM数据
     */
    public byte[] process(String sessionId, byte[] pcmData, String deviceType) {
        if (pcmData == null || pcmData.length == 0) {
            return pcmData;
        }
        
        // 获取或创建会话状态
        AgcState state = getOrCreateState(sessionId, deviceType);
        
        // 转换为浮点数组
        float[] samples = bytesToFloats(pcmData);
        
        // 计算当前帧的电平
        float currentLevel = calculateLevel(samples);
        
        // 更新噪声底估计
        state.updateNoiseFloor(currentLevel);
        
        // 更新平滑电平 - 使用更快的平滑系数
        float alpha = 0.4f;  // 进一步提高alpha使响应更快
        state.smoothedLevel = alpha * currentLevel + (1 - alpha) * state.smoothedLevel;
        
        // 更新峰值电平
        if (currentLevel > state.peakLevel) {
            state.peakLevel = currentLevel;
        } else {
            state.peakLevel = 0.95f * state.peakLevel + 0.05f * currentLevel;
        }
        
        // 计算信噪比
        float snr = state.smoothedLevel / (state.noiseFloor + 1e-10f);
        
        // 根据信噪比调整目标增益
        float adjustedTargetLevel = state.targetLevel;
        if (snr < 2.0f) {
            // 低信噪比时，提高目标电平以增强信号
            adjustedTargetLevel *= 1.5f;
        }
        
        // 计算目标增益
        float targetGain;
        if (state.smoothedLevel > 0.00005f) {  // 进一步降低阈值
            targetGain = adjustedTargetLevel / state.smoothedLevel;
        } else {
            // 对于非常低的信号，应用最大增益
            targetGain = state.maxGain;
        }
        
        // 限制增益范围
        targetGain = Math.max(state.minGain, Math.min(state.maxGain, targetGain));
        
        // 平滑增益变化
        float attackCoeff = 1.0f - (float)Math.exp(-1.0 / (ATTACK_TIME * SAMPLE_RATE));
        float releaseCoeff = 1.0f - (float)Math.exp(-1.0 / (RELEASE_TIME * SAMPLE_RATE));
        
        if (targetGain < state.currentGain) {
            // 快速降低增益（攻击）
            state.currentGain += (targetGain - state.currentGain) * attackCoeff;
        } else {
            // 缓慢提高增益（释放）
            state.currentGain += (targetGain - state.currentGain) * releaseCoeff;
        }
        
        // 更新增益历史
        state.updateGainHistory(state.currentGain);
        
        // 应用增益
        float actualGain = state.currentGain;
        
        // 对于非常低的信号，使用更高的增益
        if (currentLevel < 0.0005f) {
            actualGain = Math.min(state.maxGain, actualGain * 2.0f);
        }
        
        float[] processedSamples = applyGain(samples, actualGain);
        
        // 防止削波
        processedSamples = limitSamples(processedSamples);
        
        // 每50帧记录一次AGC状态 (减少日志频率)
        state.frameCount++;
        // if (state.frameCount % 50 == 0) {
        //     String formattedLevel = String.format("%.6f", currentLevel);
        //     String formattedGain = String.format("%.2f", actualGain);
        //     String formattedNoiseFloor = String.format("%.6f", state.noiseFloor);
        //     String formattedSnr = String.format("%.2f", snr);
            
        //     logger.debug("AGC状态 - SessionId: {}, 输入电平: {}, 增益: {}, 噪声底: {}, SNR: {}, 设备类型: {}",
        //             sessionId, formattedLevel, formattedGain, formattedNoiseFloor, formattedSnr, deviceType);
        // }
        
        // 转换回字节数组
        return floatsToBytes(processedSamples);
    }
    
    /**
     * 获取或创建AGC状态
     */
    private AgcState getOrCreateState(String sessionId, String deviceType) {
        return sessionStates.computeIfAbsent(sessionId, k -> {
            // 根据设备类型设置不同的AGC参数
            float targetLevel = DEFAULT_TARGET_LEVEL;
            float maxGain = DEFAULT_MAX_GAIN;
            float minGain = DEFAULT_MIN_GAIN;
            
            if (deviceType != null) {
                switch (deviceType.toLowerCase()) {
                    case "low_quality_mic":
                    case "weak_signal":
                        targetLevel = 0.15f;  // 更高的目标电平
                        maxGain = 60.0f;      // 非常高的最大增益
                        minGain = 5.0f;       // 较高的最小增益
                        break;
                    case "high_quality_mic":
                        targetLevel = 0.08f;  // 高质量麦克风可以用稍低的目标电平
                        maxGain = 20.0f;
                        minGain = 1.5f;
                        break;
                    case "noisy_environment":
                        targetLevel = 0.06f;  // 噪声环境下降低目标电平
                        maxGain = 30.0f;
                        minGain = 2.0f;
                        break;
                    default:  // normal
                        // 对于normal类型，也应用更合理的默认值
                        targetLevel = 0.1f;
                        maxGain = 40.0f;
                        minGain = 3.0f;
                        break;
                }
            }

            return new AgcState(targetLevel, maxGain, minGain);
        });
    }
    
    /**
     * 计算音频电平（RMS）
     */
    private float calculateLevel(float[] samples) {
        if (samples.length == 0) return 0.0f;
        
        float sum = 0.0f;
        for (float sample : samples) {
            sum += sample * sample;
        }
        return (float)Math.sqrt(sum / samples.length);
    }
    
    /**
     * 应用增益
     */
    private float[] applyGain(float[] samples, float gain) {
        float[] result = new float[samples.length];
        for (int i = 0; i < samples.length; i++) {
            result[i] = samples[i] * gain;
        }
        return result;
    }
    
    /**
     * 限制样本值防止削波
     */
    private float[] limitSamples(float[] samples) {
        float[] result = new float[samples.length];
        for (int i = 0; i < samples.length; i++) {
            result[i] = Math.max(-0.95f, Math.min(0.95f, samples[i]));  // 留一点余量
        }
        return result;
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
            samples[i] = sample / 32768.0f;
        }
        
        return samples;
    }
    
    /**
     * 浮点数组转字节数组
     */
    private byte[] floatsToBytes(float[] samples) {
        ByteBuffer buffer = ByteBuffer.allocate(samples.length * 2).order(ByteOrder.LITTLE_ENDIAN);
        
        for (float sample : samples) {
            short pcmSample = (short)(sample * 32767);
            buffer.putShort(pcmSample);
        }
        
        return buffer.array();
    }
    
    /**
     * 获取当前增益值
     */
    public float getCurrentGain(String sessionId) {
        AgcState state = sessionStates.get(sessionId);
        return state != null ? state.currentGain : 1.0f;
    }
    
    /**
     * 获取AGC统计信息
     */
    public AgcStats getStats(String sessionId) {
        AgcState state = sessionStates.get(sessionId);
        if (state == null) {
            return new AgcStats();
        }
        
        return new AgcStats(
            state.currentGain,
            state.smoothedLevel,
            state.noiseFloor,
            state.smoothedLevel / (state.noiseFloor + 1e-10f)
        );
    }
    
    /**
     * 重置会话的AGC状态
     */
    public void resetSession(String sessionId) {
        sessionStates.remove(sessionId);
        logger.info("AGC状态已重置 - SessionId: {}", sessionId);
    }

    /**
     * AGC统计信息
     */
    public static class AgcStats {
        public final float gain;
        public final float level;
        public final float noiseFloor;
        public final float snr;
        
        public AgcStats() {
            this(1.0f, 0.0f, 0.0f, 0.0f);
        }
        
        public AgcStats(float gain, float level, float noiseFloor, float snr) {
            this.gain = gain;
            this.level = level;
            this.noiseFloor = noiseFloor;
            this.snr = snr;
        }
    }
}