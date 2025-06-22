package com.xiaozhi.dialogue.vad;

/**
 * VAD模型接口 - 定义VAD模型的基本功能
 */
public interface VadModel {
    /**
     * 初始化VAD模型
     */
    void initialize();

    /**
     * 获取语音概率
     * 
     * @param samples 音频样本数据
     * @return 语音概率 (0.0-1.0)
     */
    float getSpeechProbability(float[] samples);

    /**
     * 重置模型状态
     */
    void reset();

    /**
     * 关闭模型资源
     */
    void close();
}