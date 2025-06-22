package com.xiaozhi.dialogue.stt.providers;

import com.alibaba.dashscope.audio.asr.recognition.Recognition;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionParam;
import com.xiaozhi.dialogue.stt.SttService;
import com.xiaozhi.entity.SysConfig;
import com.xiaozhi.utils.AudioUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;

public class AliyunSttService implements SttService {
    private static final Logger logger = LoggerFactory.getLogger(AliyunSttService.class);
    private static final String PROVIDER_NAME = "aliyun";
    private static final String DEFAULT_MODEL = "paraformer-realtime-v2"; // 默认模型

    private final String apiKey;

    public AliyunSttService(SysConfig config) {
        this.apiKey = config.getApiKey();
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
        // 单次识别暂未实现，可以根据需要添加
        logger.warn("阿里云单次识别未实现，请使用流式识别");
        return null;
    }

    @Override
    public String streamRecognition(Sinks.Many<byte[]> audioSink) {
        var recognizer = new Recognition();

        // 创建识别参数
        var param = RecognitionParam.builder()
                .model(DEFAULT_MODEL)
                .format("pcm")
                .sampleRate(AudioUtils.SAMPLE_RATE) // 使用16000Hz采样率
                .apiKey(apiKey)
                .build();

        // 使用 Reactor 执行流式识别
        var recognition = Flux.<String>create(sink -> {
            try {
                recognizer.streamCall(param, Flowable.create(emitter -> {
                            audioSink.asFlux().subscribe(
                                    chunk -> emitter.onNext(ByteBuffer.wrap(chunk)),
                                    emitter::onError,
                                    emitter::onComplete
                            );
                        }, BackpressureStrategy.BUFFER))
                        .timeout(5, TimeUnit.SECONDS)
                        .subscribe(result -> {
                                    if (result.isSentenceEnd()) {
                                        logger.info("语音识别结果: {}", result.getSentence().getText());
                                        sink.next(result.getSentence().getText());
                                    }
                                },
                                Throwable::printStackTrace,
                                sink::complete
                        );
            } catch (Exception e) {
                sink.error(e);
                logger.info("语音识别失败: {}", e);
            }
        });

        return recognition.reduce(new StringBuffer(), StringBuffer::append)
                .blockOptional()
                .map(StringBuffer::toString)
                .orElse("");
    }
}