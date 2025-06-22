// audioService.js - 音频处理服务

import { log } from './websocketService';

// 音频配置
const defaultConfig = {
  sampleRate: 16000,
  channels: 1,
  frameSize: 960 // 60ms @ 16kHz
};

// 状态变量
let audioContext = null;
let audioStream = null;
let audioProcessor = null;
let isRecording = false;
let isPlaying = false;
let opusDecoder = null;
let opusEncoder = null;
let audioBuffers = [];
let isAudioBuffering = false;
let isAudioPlaying = false;
let audioBufferQueue = [];
let streamingContext = null;
let currentPlayingMessageId = null;
let audioContextResumePromise = null;

// 初始化音频上下文
async function initAudioContext() {
  if (audioContext) {
    // 如果上下文已存在但处于暂停状态，返回恢复的Promise
    if (audioContext.state === 'suspended' && !audioContextResumePromise) {
      audioContextResumePromise = new Promise((resolve) => {
        log('音频上下文已暂停。需要用户交互才能恢复。', 'warning');
        
        // 添加一次性事件监听器，在用户交互时恢复
        const resumeAudioContext = async () => {
          try {
            await audioContext.resume();
            log('音频上下文已通过用户交互恢复', 'success');
            resolve(audioContext);
            
            // 清除事件监听器
            ['click', 'touchstart', 'keydown'].forEach(event => {
              document.removeEventListener(event, resumeAudioContext);
            });
            audioContextResumePromise = null;
          } catch (err) {
            log('恢复音频上下文失败: ' + err.message, 'error');
          }
        };
        
        // 添加用户交互事件监听器
        ['click', 'touchstart', 'keydown'].forEach(event => {
          document.addEventListener(event, resumeAudioContext, { once: false });
        });
      });
      
      return audioContextResumePromise;
    }
    return audioContext;
  }
  
  try {
    // 创建新的音频上下文
    const AudioContext = window.AudioContext || window.webkitAudioContext;
    audioContext = new AudioContext({
      sampleRate: defaultConfig.sampleRate,
      latencyHint: 'interactive'
    });
    
    // 检查是否处于暂停状态（自动播放策略）
    if (audioContext.state === 'suspended') {
      log('新创建的音频上下文处于暂停状态。需要用户交互才能启动。', 'warning');
      
      // 创建Promise等待用户交互
      audioContextResumePromise = new Promise((resolve) => {
        const resumeAudioContext = async () => {
          try {
            await audioContext.resume();
            log('音频上下文已通过用户交互启动', 'success');
            resolve(audioContext);
            
            // 清除事件监听器
            ['click', 'touchstart', 'keydown'].forEach(event => {
              document.removeEventListener(event, resumeAudioContext);
            });
            audioContextResumePromise = null;
          } catch (err) {
            log('启动音频上下文失败: ' + err.message, 'error');
          }
        };
        
        // 添加用户交互事件监听器
        ['click', 'touchstart', 'keydown'].forEach(event => {
          document.addEventListener(event, resumeAudioContext, { once: false });
        });
      });
      
      return audioContextResumePromise;
    }
    
    // 将audioContext暴露到全局，以便可视化功能使用
    window.audioContext = audioContext;
    
    return audioContext;
  } catch (error) {
    log('初始化音频上下文失败:' + error.message, 'error');
    return null;
  }
}

// 检查libopus.js是否已加载 - 增强版
function checkOpusLoaded() {
  try {
    // 检查Module是否存在（本地库导出的全局变量）
    if (typeof window.Module === 'undefined') {
      return false;
    }
    
    // 尝试先使用Module.instance（libopus.js最后一行导出方式）
    if (typeof window.Module.instance !== 'undefined' &&
        typeof window.Module.instance._opus_decoder_get_size === 'function') {
      // 使用Module.instance对象替换全局ModuleInstance
      window.ModuleInstance = window.Module.instance;
      log('Opus库加载成功（使用Module.instance）', 'success');
      return true;
    }
    
    // 如果没有Module.instance，检查全局Module函数
    if (typeof window.Module._opus_decoder_get_size === 'function') {
      window.ModuleInstance = window.Module;
      log('Opus库加载成功（使用全局Module）', 'success');
      return true;
    }
    
    // 最后检查ModuleInstance
    if (window.ModuleInstance && typeof window.ModuleInstance._opus_decoder_get_size === 'function') {
      log('Opus库已加载（使用ModuleInstance）', 'success');
      return true;
    }
    
    return false;
  } catch (err) {
    log(`Opus库检查失败: ${err}`, 'error');
    return false;
  }
}

// 加载Opus库 - 增强版
export function loadOpusLibrary() {
  return new Promise(async (resolve) => {
    // 检查是否已加载
    if (checkOpusLoaded()) {
      resolve(true);
      return;
    }
    
    log('尝试加载libopus.js', 'info');
    
    // 尝试多个可能的路径
    const possiblePaths = [
      '/static/js/libopus.js',
      '/js/libopus.js',
      '/libopus.js',
      './libopus.js',
      '../js/libopus.js',
      '../../js/libopus.js'
    ];
    
    // 创建脚本元素
    const script = document.createElement('script');
    script.async = true;
    
    // 设置加载事件
    script.onload = () => {
      log('libopus.js脚本加载成功，等待初始化', 'success');
      
      // 等待Module初始化
      const maxAttempts = 100; // 10秒超时 (100ms * 100)
      let attempts = 0;
      
      const checkModule = () => {
        // 检查多种可能的Module导出方式
        if (checkOpusLoaded()) {
          log('Opus库初始化成功', 'success');
          resolve(true);
          return;
        }
        
        if (attempts >= maxAttempts) {
          log('Opus库初始化超时', 'error');
          resolve(false);
          return;
        }
        
        attempts++;
        setTimeout(checkModule, 100);
      };
      
      checkModule();
    };
    
    script.onerror = () => {
      log('libopus.js加载失败，尝试下一个路径', 'warning');
      tryNextPath();
    };
    
    // 尝试加载下一个路径
    let pathIndex = 0;
    
    function tryNextPath() {
      if (pathIndex >= possiblePaths.length) {
        log('所有路径都尝试失败', 'error');
        resolve(false);
        return;
      }
      
      const path = possiblePaths[pathIndex];
      pathIndex++;
      
      log(`尝试从路径加载: ${path}`, 'info');
      script.src = path;
      document.head.appendChild(script);
    }
    
    // 开始尝试第一个路径
    tryNextPath();
  });
}

// 初始化音频 - 修改版，添加用户交互处理
export async function initAudio() {
  try {
    // 创建音频上下文但不等待它启动
    const context = await initAudioContext();
    
    // 添加一个全局函数用于用户交互后启用音频
    window.enableAudio = async function() {
      try {
        // 尝试恢复音频上下文
        if (audioContext && audioContext.state === 'suspended') {
          await audioContext.resume();
          log('音频上下文已恢复', 'success');
        }
        
        // 加载libopus.js (增加重试机制)
        let opusLoaded = false;
        for (let i = 0; i < 3; i++) {
          try {
            opusLoaded = await loadOpusLibrary();
            if (opusLoaded) {
              log(`Opus库加载成功 (尝试 ${i+1}/3)`, 'success');
              break;
            }
          } catch (err) {
            log(`尝试 ${i+1}/3 加载libopus.js失败，将重试`, 'warning');
          }
        }
        
        if (!opusLoaded) {
          log('所有Opus库加载尝试均失败，音频播放功能将不可用', 'error');
          return false;
        }
        
        // 尝试初始化Opus解码器
        try {
          await initOpusDecoder();
          log('Opus解码器初始化成功', 'success');
          return true;
        } catch (err) {
          log(`Opus解码器初始化失败: ${err.message}，音频播放功能将不可用`, 'error');
          return false;
        }
      } catch (error) {
        log('启用音频失败:' + error.message, 'error');
        return false;
      }
    };
    
    // 设置全局处理函数，避免循环依赖
    window.handleBinaryAudioMessage = handleBinaryAudioMessage;
    
    // 加载libopus.js但不初始化解码器
    await loadOpusLibrary();
    
    log('音频系统已初始化。请通过用户交互启用音频功能。', 'info');
    
    return true;
  } catch (error) {
    log('初始化音频失败:' + error.message, 'error');
    return false;
  }
}

// 重置音频缓冲区
function resetAudioBuffer() {
  audioBufferQueue = [];
  isAudioBuffering = false;
  isAudioPlaying = false;
}

// 添加音频到缓冲区
function addAudioToBuffer(opusData) {
  audioBufferQueue.push(opusData);
  
  // 如果收到的是第一个音频包，开始缓冲过程
  if (audioBufferQueue.length === 1 && !isAudioBuffering && !isAudioPlaying) {
    startAudioBuffering();
  }
  
  return true;
}

// 开始音频缓冲
function startAudioBuffering() {
  if (isAudioBuffering || isAudioPlaying) return false;
  
  isAudioBuffering = true;
  log('开始音频缓冲...', 'info');
  
  // 先尝试初始化解码器，以便在播放时已准备好
  initOpusDecoder().catch(error => {
    log(`预初始化Opus解码器失败: ${error.message}`, 'warning');
    // 继续缓冲，我们会在播放时再次尝试初始化
  });
  
  // 设置超时，如果在一定时间内没有收集到足够的音频包，就开始播放
  setTimeout(() => {
    if (isAudioBuffering && audioBufferQueue.length > 0) {
      log(`缓冲超时，当前缓冲包数: ${audioBufferQueue.length}，开始播放`, 'info');
      playBufferedAudio();
    }
  }, 300); // 300ms超时
  
  // 监控缓冲进度
  const bufferThreshold = 3; // 至少缓冲3个包
  const bufferCheckInterval = setInterval(() => {
    if (!isAudioBuffering) {
      clearInterval(bufferCheckInterval);
      return;
    }
    
    // 当累积了足够的音频包，开始播放
    if (audioBufferQueue.length >= bufferThreshold) {
      clearInterval(bufferCheckInterval);
      log(`已缓冲 ${audioBufferQueue.length} 个音频包，开始播放`, 'info');
      playBufferedAudio();
    }
  }, 50);
  
  return true;
}

// 播放缓冲的音频 - 修改版，处理用户交互需求
async function playBufferedAudio() {
  if (isAudioPlaying || audioBufferQueue.length === 0) return false;
  
  isAudioPlaying = true;
  isAudioBuffering = false;
  
  try {
    // 初始化音频上下文并确保它已启动
    if (!audioContext) {
      audioContext = await initAudioContext();
    }
    
    // 检查音频上下文状态
    if (audioContext.state === 'suspended') {
      log('音频上下文被暂停，等待用户交互...', 'warning');
      
      // 保存当前缓冲的数据
      const savedFrames = [...audioBufferQueue];
      
      // 创建一个UI提示，通知用户需要交互
      const notifyUserInteraction = () => {
        // 创建一个通知元素
        const notification = document.createElement('div');
        notification.style.position = 'fixed';
        notification.style.bottom = '20px';
        notification.style.left = '50%';
        notification.style.transform = 'translateX(-50%)';
        notification.style.padding = '10px 20px';
        notification.style.backgroundColor = 'rgba(0, 0, 0, 0.7)';
        notification.style.color = 'white';
        notification.style.borderRadius = '5px';
        notification.style.zIndex = '9999';
        notification.style.cursor = 'pointer';
        notification.textContent = '点击此处启用音频播放';
        
        // 添加点击事件
        notification.addEventListener('click', async () => {
          try {
            // 恢复音频上下文
            await audioContext.resume();
            log('音频上下文已通过用户交互恢复', 'success');
            
            // 移除通知
            document.body.removeChild(notification);
            
            // 重新尝试播放
            isAudioPlaying = false;
            audioBufferQueue = [...savedFrames];
            playBufferedAudio();
          } catch (err) {
            log('恢复音频上下文失败: ' + err.message, 'error');
          }
        });
        
        // 添加到文档
        document.body.appendChild(notification);
        
        // 5秒后自动移除
        setTimeout(() => {
          if (document.body.contains(notification)) {
            document.body.removeChild(notification);
          }
        }, 5000);
      };
      
      notifyUserInteraction();
      
      // 暂时停止播放尝试
      isAudioPlaying = false;
      return false;
    }
    
    // 确保解码器已初始化
    if (!opusDecoder) {
      log('初始化Opus解码器...', 'info');
      try {
        opusDecoder = await initOpusDecoder();
        if (!opusDecoder) {
          throw new Error('解码器初始化失败');
        }
        log('Opus解码器初始化成功', 'success');
      } catch (error) {
        log('Opus解码器初始化失败: ' + error.message, 'error');
        isAudioPlaying = false;
        return false;
      }
    }
    
    // 创建流式上下文
    if (!streamingContext) {
      streamingContext = {
        queue: [],      // 已解码的PCM队列
        playing: false, // 是否正在播放
        endOfStream: false, // 是否收到结束信号
        source: null,   // 当前音频源
        totalSamples: 0, // 累积的总样本数
        lastPlayTime: 0, // 上次播放的时间戳
        
        // 将Opus数据解码为PCM
        decodeOpusFrames: async function(opusFrames) {
          if (!opusDecoder) {
            log('Opus解码器未初始化，无法解码', 'error');
            return;
          }
          
          let decodedSamples = [];
          for (const frame of opusFrames) {
            try {
              // 使用Opus解码器解码
              const frameData = opusDecoder.decode(frame);
              if (frameData && frameData.length > 0) {
                // 转换为Float32
                const floatData = convertInt16ToFloat32(frameData);
                decodedSamples.push(...floatData);
              }
            } catch (error) {
              log("Opus解码失败: " + error.message, 'error');
            }
          }
          
          if (decodedSamples.length > 0) {
            // 添加到解码队列
            this.queue.push(...decodedSamples);
            this.totalSamples += decodedSamples.length;
            
            // 如果累积了至少0.1秒的音频，开始播放
            const minSamples = defaultConfig.sampleRate * 0.1; // 最小0.1秒
            if (!this.playing && this.queue.length >= minSamples) {
              this.startPlaying();
            }
          } else {
            log('没有成功解码的样本', 'warning');
          }
        },
        
        // 开始播放音频
        startPlaying: function() {
          if (this.playing || this.queue.length === 0 || !audioContext) return;
          
          // 再次检查音频上下文状态
          if (audioContext.state === 'suspended') {
            log('音频上下文仍处于暂停状态，无法播放', 'warning');
            return;
          }
          
          this.playing = true;
          
          // 创建新的音频缓冲区
          const minPlaySamples = Math.min(this.queue.length, defaultConfig.sampleRate); // 最多播放1秒
          const currentSamples = this.queue.splice(0, minPlaySamples);
          const audioBuffer = audioContext.createBuffer(
            defaultConfig.channels,
            currentSamples.length,
            defaultConfig.sampleRate
          );
          
          // 填充音频数据
          const channelData = audioBuffer.getChannelData(0);
          for (let i = 0; i < currentSamples.length; i++) {
            channelData[i] = currentSamples[i];
          }
          
          // 创建音频源
          this.source = audioContext.createBufferSource();
          this.source.buffer = audioBuffer;
          
          // 创建增益节点用于平滑过渡
          const gainNode = audioContext.createGain();
          
          // 应用淡入淡出效果避免爆音
          const fadeDuration = 0.02; // 20毫秒
          gainNode.gain.setValueAtTime(0, audioContext.currentTime);
          gainNode.gain.linearRampToValueAtTime(1, audioContext.currentTime + fadeDuration);
          
          const duration = audioBuffer.duration;
          if (duration > fadeDuration * 2) {
            gainNode.gain.setValueAtTime(1, audioContext.currentTime + duration - fadeDuration);
            gainNode.gain.linearRampToValueAtTime(0, audioContext.currentTime + duration);
          }
          
          // 创建分析器节点用于可视化
          const analyserNode = audioContext.createAnalyser();
          analyserNode.fftSize = 256;
          analyserNode.smoothingTimeConstant = 0.8;
          
          // 连接节点: 源 -> 分析器 -> 增益 -> 输出
          this.source.connect(analyserNode);
          analyserNode.connect(gainNode);
          gainNode.connect(audioContext.destination);
          
          // 保存分析器节点以便可视化使用
          this.analyser = analyserNode;
          
          this.lastPlayTime = audioContext.currentTime;
          
          log(`开始播放 ${currentSamples.length} 个样本，约 ${(currentSamples.length / defaultConfig.sampleRate).toFixed(2)} 秒`, 'debug');
          
          // 播放结束后的处理
          this.source.onended = () => {
            this.source = null;
            this.analyser = null;
            this.playing = false;
            
            // 如果队列中还有数据或者缓冲区有新数据，继续播放
            if (this.queue.length > 0) {
              setTimeout(() => this.startPlaying(), 10);
            } else if (audioBufferQueue.length > 0) {
              // 缓冲区有新数据，进行解码
              const frames = [...audioBufferQueue];
              audioBufferQueue = [];
              this.decodeOpusFrames(frames);
            } else if (this.endOfStream) {
              // 流已结束且没有更多数据
              log("音频播放完成", 'info');
              isAudioPlaying = false;
              streamingContext = null;
              
              // 将streamingContext从全局移除
              window.streamingContext = null;
            } else {
              // 等待更多数据
              setTimeout(() => {
                // 如果仍然没有新数据，但有更多的包到达
                if (this.queue.length === 0 && audioBufferQueue.length > 0) {
                  const frames = [...audioBufferQueue];
                  audioBufferQueue = [];
                  this.decodeOpusFrames(frames);
                } else if (this.queue.length === 0 && audioBufferQueue.length === 0) {
                  // 真的没有更多数据了
                  log("音频播放完成 (超时)", 'info');
                  isAudioPlaying = false;
                  streamingContext = null;
                  
                  // 将streamingContext从全局移除
                  window.streamingContext = null;
                }
              }, 500); // 500ms超时
            }
          };
          
          this.source.start();
        }
      };
      
      // 将streamingContext暴露到全局，以便可视化功能使用
      window.streamingContext = streamingContext;
    }
    
    // 开始处理缓冲的数据
    const frames = [...audioBufferQueue];
    audioBufferQueue = []; // 清空缓冲队列
    
    // 解码并播放
    await streamingContext.decodeOpusFrames(frames);
    return true;
  } catch (error) {
    log(`播放已缓冲的音频出错:` + error.message, 'error');
    isAudioPlaying = false;
    streamingContext = null;
    
    // 清除全局引用
    window.streamingContext = null;
    
    return false;
  }
}

// 将Int16音频数据转换为Float32音频数据
function convertInt16ToFloat32(int16Data) {
  const float32Data = [];
  for (let i = 0; i < int16Data.length; i++) {
    // 将[-32768,32767]范围转换为[-1,1]
    float32Data.push(int16Data[i] / (int16Data[i] < 0 ? 0x8000 : 0x7FFF));
  }
  return float32Data;
}

// 停止音频播放
export function stopAudioPlayback() {
  try {
    isAudioPlaying = false;
    isAudioBuffering = false;
    
    // 停止当前正在播放的音频
    if (streamingContext && streamingContext.source) {
      try {
        streamingContext.source.stop();
        streamingContext.source = null;
        streamingContext.analyser = null;
      } catch (e) {
        // 忽略已停止的音频源错误
      }
    }
    
    audioBufferQueue = [];
    streamingContext = null;
    
    // 清除全局引用
    window.streamingContext = null;
    
    // 触发一个自定义事件，通知可视化组件播放停止
    if (window.dispatchEvent) {
      window.dispatchEvent(new CustomEvent('audio-playback-stopped'));
    }
    
    log('音频播放已停止', 'info');
    return true;
  } catch (error) {
    log(`停止音频播放失败:` + error.message, 'error');
    return false;
  }
}

// 初始化Opus解码器
export async function initOpusDecoder() {
  if (opusDecoder) {
    return opusDecoder;
  }

  try {
    // 确保libopus.js已加载
    const opusLoaded = await loadOpusLibrary();
    if (!opusLoaded) {
      throw new Error('Opus库未加载');
    }
    
    // 获取ModuleInstance
    const mod = window.ModuleInstance;
    if (!mod) {
      throw new Error('ModuleInstance不可用');
    }
    
    // 创建解码器
    return createOpusDecoder(mod);
  } catch (error) {
    log(`初始化Opus解码器失败:` + error.message, 'error');
    throw error;
  }
}

// 初始化Opus编码器
export async function initOpusEncoder() {
  if (opusEncoder) {
    return opusEncoder;
  }
  
  try {
    // 确保libopus.js已加载
    const opusLoaded = await loadOpusLibrary();
    if (!opusLoaded) {
      throw new Error('Opus库未加载');
    }
    
    // 获取ModuleInstance
    const mod = window.ModuleInstance;
    if (!mod) {
      throw new Error('ModuleInstance不可用');
    }
    
    // 创建编码器
    const encoder = createOpusEncoder(mod);
    opusEncoder = encoder;
    return encoder;
  } catch (error) {
    log(`初始化Opus编码器失败:` + error.message, 'error');
    throw error;
  }
}

// 创建Opus编码器
function createOpusEncoder(mod) {
  try {
    const sampleRate = 16000; // 16kHz采样率
    const channels = 1;       // 单声道
    const application = 2048; // OPUS_APPLICATION_VOIP = 2048
    
    // 创建编码器对象
    const encoder = {
      channels: channels,
      sampleRate: sampleRate,
      frameSize: 960, // 60ms @ 16kHz = 60 * 16 = 960 samples
      maxPacketSize: 4000, // 最大包大小
      module: mod,
      encoderPtr: null,
      
      // 初始化编码器
      init: function() {
        try {
          // 获取编码器大小
          const encoderSize = mod._opus_encoder_get_size(this.channels);
          log('Opus编码器大小:' + encoderSize + '字节', 'debug');
          
          // 分配内存
          this.encoderPtr = mod._malloc(encoderSize);
          if (!this.encoderPtr) {
            throw new Error("无法分配编码器内存");
          }
          
          // 初始化编码器
          const err = mod._opus_encoder_init(
            this.encoderPtr,
            this.sampleRate,
            this.channels,
            application
          );
          
          if (err < 0) {
            throw new Error(`Opus编码器初始化失败: ${err}`);
          }
          
          // 设置位率 (16kbps)
          mod._opus_encoder_ctl(this.encoderPtr, 4002, 16000); // OPUS_SET_BITRATE
          
          // 设置复杂度 (0-10, 越高质量越好但CPU使用越多)
          mod._opus_encoder_ctl(this.encoderPtr, 4010, 5);     // OPUS_SET_COMPLEXITY
          
          // 设置使用DTX (不传输静音帧)
          mod._opus_encoder_ctl(this.encoderPtr, 4016, 1);     // OPUS_SET_DTX
          
          log("Opus编码器初始化成功", 'success');
          return true;
        } catch (error) {
          if (this.encoderPtr) {
            mod._free(this.encoderPtr);
            this.encoderPtr = null;
          }
          log('Opus编码器初始化失败:' + error.message, 'error');
          return false;
        }
      },
      
      // 编码PCM数据为Opus
      encode: function(pcmData) {
        if (!this.encoderPtr) {
          if (!this.init()) {
            return null;
          }
        }
        
        try {
          const mod = this.module;
          
          // 为PCM数据分配内存
          const pcmPtr = mod._malloc(pcmData.length * 2); // 2字节/int16
          
          // 将PCM数据复制到HEAP
          for (let i = 0; i < pcmData.length; i++) {
            mod.HEAP16[(pcmPtr >> 1) + i] = pcmData[i];
          }
          
          // 为输出分配内存
          const outPtr = mod._malloc(this.maxPacketSize);
          
          // 进行编码
          const encodedLen = mod._opus_encode(
            this.encoderPtr,
            pcmPtr,
            this.frameSize,
            outPtr,
            this.maxPacketSize
          );
          
          if (encodedLen < 0) {
            throw new Error(`Opus编码失败: ${encodedLen}`);
          }
          
          // 复制编码后的数据
          const opusData = new Uint8Array(encodedLen);
          for (let i = 0; i < encodedLen; i++) {
            opusData[i] = mod.HEAPU8[outPtr + i];
          }
          
          // 释放内存
          mod._free(pcmPtr);
          mod._free(outPtr);
          
          return opusData;
        } catch (error) {
          log('Opus编码出错:' + error.message, 'error');
          return null;
        }
      },
      
      // 销毁编码器
      destroy: function() {
        if (this.encoderPtr) {
          this.module._free(this.encoderPtr);
          this.encoderPtr = null;
        }
      }
    };
    
    // 初始化编码器
    if (!encoder.init()) {
      throw new Error("Opus编码器初始化失败");
    }
    
    return encoder;
  } catch (error) {
    log('创建Opus编码器失败:' + error.message, 'error');
    throw error;
  }
}

// 创建Opus解码器
function createOpusDecoder(mod) {
  try {
    // 常量定义
    const SAMPLE_RATE = 16000;
    const CHANNELS = 1;
    const FRAME_SIZE = 960; // 60ms @ 16kHz
    
    // 创建解码器对象
    const decoder = {
      channels: CHANNELS,
      rate: SAMPLE_RATE,
      frameSize: FRAME_SIZE,
      module: mod,
      decoderPtr: null,
      
      // 初始化解码器
      init: function() {
        if (this.decoderPtr) return true; // 已经初始化
        
        // 获取解码器大小
        const decoderSize = mod._opus_decoder_get_size(this.channels);
        log('Opus解码器大小:' + decoderSize + '字节', 'debug');
        
        // 分配内存
        this.decoderPtr = mod._malloc(decoderSize);
        if (!this.decoderPtr) {
          throw new Error("无法分配解码器内存");
        }
        
        // 初始化解码器
        const err = mod._opus_decoder_init(
          this.decoderPtr,
          this.rate,
          this.channels
        );
        
        if (err < 0) {
          this.destroy(); // 清理资源
          throw new Error(`Opus解码器初始化失败: ${err}`);
        }
        
        log("Opus解码器初始化成功", 'success');
        return true;
      },
      
      // 解码方法
      decode: function(opusData) {
        if (!this.decoderPtr) {
          if (!this.init()) {
            throw new Error("解码器未初始化且无法初始化");
          }
        }
        
        try {
          const mod = this.module;
          
          // 为Opus数据分配内存
          const opusPtr = mod._malloc(opusData.length);
          mod.HEAPU8.set(opusData, opusPtr);
          
          // 为PCM输出分配内存
          const pcmPtr = mod._malloc(this.frameSize * 2); // Int16 = 2字节
          
          // 解码
          const decodedSamples = mod._opus_decode(
            this.decoderPtr,
            opusPtr,
            opusData.length,
            pcmPtr,
            this.frameSize,
            0 // 不使用FEC
          );
          
          if (decodedSamples < 0) {
            mod._free(opusPtr);
            mod._free(pcmPtr);
            throw new Error(`Opus解码失败: ${decodedSamples}`);
          }
          
          // 复制解码后的数据
          const decodedData = new Int16Array(decodedSamples);
          for (let i = 0; i < decodedSamples; i++) {
            decodedData[i] = mod.HEAP16[(pcmPtr >> 1) + i];
          }
          
          // 释放内存
          mod._free(opusPtr);
          mod._free(pcmPtr);
          
          return decodedData;
        } catch (error) {
          log('Opus解码错误:' + error.message, 'error');
          return new Int16Array(0);
        }
      },
      
      // 销毁方法
      destroy: function() {
        if (this.decoderPtr) {
          this.module._free(this.decoderPtr);
          this.decoderPtr = null;
        }
      }
    };
    
    // 初始化解码器
    if (!decoder.init()) {
      throw new Error("Opus解码器初始化失败");
    }
    
    // 保存到全局引用
    opusDecoder = decoder;
    return decoder;
  } catch (error) {
    log('Opus解码器初始化失败:' + error.message, 'error');
    opusDecoder = null;
    throw error;
  }
}

// 解码Opus数据
export async function decodeOpusData(opusData) {
  try {
    // 确保解码器已初始化
    if (!opusDecoder) {
      opusDecoder = await initOpusDecoder();
    }
    
    if (!opusDecoder) {
      throw new Error('Opus解码器未初始化');
    }
    
    // 解码数据
    return opusDecoder.decode(opusData);
  } catch (error) {
    log('解码Opus数据失败:' + error.message, 'error');
    throw error;
  }
}

// 播放PCM数据 - 直接播放方式，不使用流式播放
export async function playPCMData(pcmData) {
  try {
    if (!audioContext) {
      await initAudioContext();
    }
    
    // 检查音频上下文状态
    if (audioContext.state === 'suspended') {
      log('音频上下文被暂停，需要用户交互才能播放音频', 'warning');
      
      // 创建一个UI提示
      const notification = document.createElement('div');
      notification.style.position = 'fixed';
      notification.style.bottom = '20px';
      notification.style.left = '50%';
      notification.style.transform = 'translateX(-50%)';
      notification.style.padding = '10px 20px';
      notification.style.backgroundColor = 'rgba(0, 0, 0, 0.7)';
      notification.style.color = 'white';
      notification.style.borderRadius = '5px';
      notification.style.zIndex = '9999';
      notification.style.cursor = 'pointer';
      notification.textContent = '点击此处启用音频播放';
      
      // 添加点击事件
      notification.addEventListener('click', async () => {
        try {
          // 恢复音频上下文
          await audioContext.resume();
          log('音频上下文已通过用户交互恢复', 'success');
          
          // 移除通知
          document.body.removeChild(notification);
          
          // 重新尝试播放
          playPCMData(pcmData);
        } catch (err) {
          log('恢复音频上下文失败: ' + err.message, 'error');
        }
      });
      
      // 添加到文档
      document.body.appendChild(notification);
      
      // 5秒后自动移除
      setTimeout(() => {
        if (document.body.contains(notification)) {
          document.body.removeChild(notification);
        }
      }, 5000);
      
      return false;
    }
    
    // 创建音频缓冲区
    const buffer = audioContext.createBuffer(
      defaultConfig.channels,
      pcmData.length,
      defaultConfig.sampleRate
    );
    
    // 将PCM数据填充到缓冲区
    const channelData = buffer.getChannelData(0);
    for (let i = 0; i < pcmData.length; i++) {
      // 将Int16转换为Float32 (-1.0 到 1.0)
      channelData[i] = pcmData[i] / 32768.0;
    }
    
    // 创建音频源并播放
    const source = audioContext.createBufferSource();
    source.buffer = buffer;
    source.connect(audioContext.destination);
    source.start();
    
    isPlaying = true;
    
    // 播放完成后更新状态
    source.onended = () => {
      isPlaying = false;
    };
    
    return true;
  } catch (error) {
    log('播放PCM数据失败:' + error.message, 'error');
    isPlaying = false;
    return false;
  }
}

// 处理二进制音频消息
export async function handleBinaryAudioMessage(data) {
  try {
    let arrayBuffer;

    // 根据数据类型进行处理
    if (data instanceof ArrayBuffer) {
      arrayBuffer = data;
      log(`收到ArrayBuffer音频数据，大小: ${data.byteLength}字节`, 'debug');
    } else if (data instanceof Blob) {
      // 如果是Blob类型，转换为ArrayBuffer
      arrayBuffer = await data.arrayBuffer();
      log(`收到Blob音频数据，大小: ${arrayBuffer.byteLength}字节`, 'debug');
    } else {
      log(`收到未知类型的二进制数据: ${typeof data}`, 'warning');
      return false;
    }

    // 创建Uint8Array用于处理
    const opusData = new Uint8Array(arrayBuffer);

    if (opusData.length > 0) {
      // 将数据添加到缓冲队列
      addAudioToBuffer(opusData);
      
      // 触发一个自定义事件，通知可视化组件
      if (window.dispatchEvent) {
        window.dispatchEvent(new CustomEvent('audio-data-received', { 
          detail: { dataLength: opusData.length } 
        }));
      }
      
      return true;
    } else {
      log('收到空音频数据帧，可能是结束标志', 'warning');
      
      // 如果缓冲队列中有数据且没有在播放，立即开始播放
      if (audioBufferQueue.length > 0 && !isAudioPlaying) {
        playBufferedAudio();
      }
      
      // 如果正在播放，发送结束信号
      if (isAudioPlaying && streamingContext) {
        streamingContext.endOfStream = true;
      }
      
      // 触发一个自定义事件，通知可视化组件播放结束
      if (window.dispatchEvent) {
        window.dispatchEvent(new CustomEvent('audio-playback-ended'));
      }
      
      return true;
    }
  } catch (error) {
    log('处理二进制消息出错:' + error.message, 'error');
    return false;
  }
}

// 处理二进制消息 (别名，用于兼容性)
export const handleBinaryMessage = handleBinaryAudioMessage;

// 清理资源
export function cleanupAudio() {
  try {
    // 停止录音
    if (audioProcessor) {
      audioProcessor.disconnect();
      audioProcessor = null;
    }
    
    // 停止当前播放
    stopAudioPlayback();
    
    // 释放麦克风
    if (audioStream) {
      audioStream.getTracks().forEach(track => track.stop());
      audioStream = null;
    }
    
    // 销毁解码器
    if (opusDecoder && opusDecoder.destroy) {
      opusDecoder.destroy();
      opusDecoder = null;
    }
    
    // 销毁编码器
    if (opusEncoder && opusEncoder.destroy) {
      opusEncoder.destroy();
      opusEncoder = null;
    }
    
    // 关闭音频上下文
    if (audioContext && audioContext.state !== 'closed') {
      audioContext.close();
      audioContext = null;
    }
    
    // 清空缓冲区
    resetAudioBuffer();
    audioBuffers = [];
    
    isRecording = false;
    isPlaying = false;
    
    log('音频资源已清理', 'info');
    return true;
  } catch (error) {
    log('清理音频资源失败:' + error.message, 'error');
    return false;
  }
}

// 获取音频状态
export function getAudioState() {
  return {
    isRecording,
    isPlaying,
    audioBuffers,
    isAudioBuffering,
    isAudioPlaying,
    audioBufferQueue,
    currentPlayingMessageId,
    analyser: streamingContext && streamingContext.analyser
  };
}

// 添加一个新的辅助函数，用于在UI中显示音频启用按钮
export function createAudioEnableButton() {
  const button = document.createElement('button');
  button.textContent = '启用音频';
  button.style.padding = '8px 16px';
  button.style.backgroundColor = '#4CAF50';
  button.style.color = 'white';
  button.style.border = 'none';
  button.style.borderRadius = '4px';
  button.style.cursor = 'pointer';
  button.style.margin = '10px';
  
  button.onclick = async () => {
    try {
      if (audioContext && audioContext.state === 'suspended') {
        await audioContext.resume();
        log('音频上下文已恢复', 'success');
        button.textContent = '音频已启用';
        button.disabled = true;
        button.style.backgroundColor = '#888';
      } else if (window.enableAudio) {
        const success = await window.enableAudio();
        if (success) {
          button.textContent = '音频已启用';
          button.disabled = true;
          button.style.backgroundColor = '#888';
        }
      }
    } catch (err) {
      log('启用音频失败: ' + err.message, 'error');
    }
  };
  
  return button;
}