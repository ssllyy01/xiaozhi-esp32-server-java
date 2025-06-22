<template>
  <div class="audio-visualizer-container">
    <canvas ref="canvas" class="audio-visualizer-canvas"></canvas>
  </div>
</template>

<script>
import { getAudioState } from '@/services/audioService';

export default {
  name: 'AudioVisualizer',
  props: {
    width: {
      type: Number,
      default: 300
    },
    height: {
      type: Number,
      default: 100
    },
    barColor: {
      type: String,
      default: 'gradient' // 'gradient' 或特定颜色如 '#1890ff'
    }
  },
  data() {
    return {
      canvas: null,
      ctx: null,
      analyser: null,
      dataArray: null,
      animationId: null,
      isActive: false,
      audioStateCheckInterval: null
    };
  },
  mounted() {
    this.initCanvas();
    this.setupEventListeners();
    this.startAudioStateMonitor();
  },
  beforeDestroy() {
    this.stopVisualization();
    this.removeEventListeners();
    
    if (this.audioStateCheckInterval) {
      clearInterval(this.audioStateCheckInterval);
    }
  },
  methods: {
    initCanvas() {
      this.canvas = this.$refs.canvas;
      if (!this.canvas) return;
      
      this.canvas.width = this.width;
      this.canvas.height = this.height;
      
      this.ctx = this.canvas.getContext('2d');
      if (!this.ctx) return;
      
      // 绘制初始静态波形
      this.drawStaticWaveform();
    },
    
    setupEventListeners() {
      // 监听音频数据接收事件
      window.addEventListener('audio-data-received', this.onAudioDataReceived);
      
      // 监听音频播放结束事件
      window.addEventListener('audio-playback-ended', this.onAudioPlaybackEnded);
      
      // 监听音频播放停止事件
      window.addEventListener('audio-playback-stopped', this.onAudioPlaybackStopped);
      
      // 监听窗口大小变化
      window.addEventListener('resize', this.handleResize);
    },
    
    removeEventListeners() {
      window.removeEventListener('audio-data-received', this.onAudioDataReceived);
      window.removeEventListener('audio-playback-ended', this.onAudioPlaybackEnded);
      window.removeEventListener('audio-playback-stopped', this.onAudioPlaybackStopped);
      window.removeEventListener('resize', this.handleResize);
    },
    
    handleResize() {
      if (this.canvas && this.$el) {
        // 获取容器宽度
        const containerWidth = this.$el.clientWidth;
        const containerHeight = this.height;
        
        // 更新canvas尺寸
        this.canvas.width = containerWidth;
        this.canvas.height = containerHeight;
        
        // 如果不是活跃状态，重绘静态波形
        if (!this.isActive) {
          this.drawStaticWaveform();
        }
      }
    },
    
    startAudioStateMonitor() {
      // 每200ms检查一次音频状态
      this.audioStateCheckInterval = setInterval(() => {
        const audioState = getAudioState();
        
        // 如果音频正在播放但可视化未激活，则启动可视化
        if (audioState.isAudioPlaying && !this.isActive) {
          this.startVisualization();
        } 
        // 如果音频已停止播放但可视化仍在运行，则停止可视化
        else if (!audioState.isAudioPlaying && this.isActive) {
          this.stopVisualization();
        }
      }, 200);
    },
    
    onAudioDataReceived(event) {
      // 当收到音频数据时，检查是否需要启动可视化
      const audioState = getAudioState();
      if (audioState.isAudioPlaying && !this.isActive) {
        this.startVisualization();
      }
    },
    
    onAudioPlaybackEnded() {
      // 音频播放结束时停止可视化
      this.stopVisualization();
    },
    
    onAudioPlaybackStopped() {
      // 音频播放停止时停止可视化
      this.stopVisualization();
    },
    
    startVisualization() {
      if (this.isActive) return;
      
      // 获取音频状态
      const audioState = getAudioState();
      
      // 检查是否有分析器
      const analyser = audioState.analyser || 
                       (window.streamingContext && window.streamingContext.analyser);
      
      if (!analyser) {
        console.log('没有可用的音频分析器，使用模拟数据');
        this.startSimulatedVisualization();
        return;
      }
      
      // 设置分析器
      this.analyser = analyser;
      
      // 创建数据数组
      const bufferLength = analyser.frequencyBinCount;
      this.dataArray = new Uint8Array(bufferLength);
      
      // 标记为活跃
      this.isActive = true;
      
      // 开始动画循环
      this.visualize();
      
      console.log('音频可视化开始');
    },
    
    startSimulatedVisualization() {
      // 如果没有真实的音频分析器，使用模拟数据
      this.isActive = true;
      
      // 创建模拟数据数组
      const bufferLength = 128;
      this.dataArray = new Uint8Array(bufferLength);
      
      // 模拟数据生成函数
      const generateSimulatedData = () => {
        for (let i = 0; i < bufferLength; i++) {
          // 生成随机值，中间频率较高
          const centerFactor = 1 - Math.abs((i / bufferLength) - 0.5) * 2;
          this.dataArray[i] = Math.floor(Math.random() * 100 * centerFactor) + 50;
        }
      };
      
      // 初始生成数据
      generateSimulatedData();
      
      // 设置定时器定期更新模拟数据
      this.simulationInterval = setInterval(generateSimulatedData, 100);
      
      // 开始动画循环
      this.visualize();
      
      console.log('模拟音频可视化开始');
    },
    
    stopVisualization() {
      if (!this.isActive) return;
      
      // 取消动画
      if (this.animationId) {
        cancelAnimationFrame(this.animationId);
        this.animationId = null;
      }
      
      // 清除模拟数据定时器
      if (this.simulationInterval) {
        clearInterval(this.simulationInterval);
        this.simulationInterval = null;
      }
      
      // 标记为非活跃
      this.isActive = false;
      
      // 重置为静态波形
      this.drawStaticWaveform();
      
      console.log('音频可视化停止');
    },
    
    visualize() {
      if (!this.isActive || !this.ctx) {
        return;
      }
      
      // 设置动画帧
      this.animationId = requestAnimationFrame(this.visualize.bind(this));
      
      // 获取频率数据
      if (this.analyser && this.dataArray) {
        this.analyser.getByteFrequencyData(this.dataArray);
      }
      
      // 清除画布
      this.ctx.fillStyle = '#fafafa';
      this.ctx.fillRect(0, 0, this.canvas.width, this.canvas.height);
      
      // 绘制频谱
      this.drawSpectrum();
    },
    
    drawSpectrum() {
      if (!this.ctx || !this.dataArray) return;
      
      const width = this.canvas.width;
      const height = this.canvas.height;
      const bufferLength = this.dataArray.length;
      const barWidth = (width / bufferLength) * 2.5;
      let x = 0;
      
      // 创建渐变
      let gradient = null;
      if (this.barColor === 'gradient') {
        gradient = this.ctx.createLinearGradient(0, height, 0, 0);
        gradient.addColorStop(0, '#1890ff');   // 蓝色底部
        gradient.addColorStop(0.5, '#52c41a'); // 绿色中部
        gradient.addColorStop(1, '#faad14');   // 橙色顶部
      }
      
      // 绘制频谱条
      for (let i = 0; i < bufferLength; i++) {
        const barHeight = (this.dataArray[i] / 255) * height * 0.8;
        
        // 设置填充样式
        if (this.barColor === 'gradient') {
          this.ctx.fillStyle = gradient;
        } else if (this.barColor === 'dynamic') {
          // 根据频率创建动态颜色
          const hue = (i / bufferLength) * 180 + 180; // 从青色到蓝色的渐变
          this.ctx.fillStyle = `hsl(${hue}, 70%, 60%)`;
        } else {
          // 使用指定的颜色
          this.ctx.fillStyle = this.barColor;
        }
        
        // 绘制条形
        const y = height - barHeight;
        this.ctx.fillRect(x, y, barWidth - 1, barHeight);
        
        x += barWidth;
      }
    },
    
    drawStaticWaveform() {
      if (!this.ctx || !this.canvas) return;
      
      const width = this.canvas.width;
      const height = this.canvas.height;
      
      // 清除画布
      this.ctx.fillStyle = '#fafafa';
      this.ctx.fillRect(0, 0, width, height);
      
      // 绘制一个静态的正弦波
      this.ctx.beginPath();
      this.ctx.strokeStyle = '#d9d9d9';
      this.ctx.lineWidth = 2;
      
      const amplitude = height * 0.2; // 波形振幅
      const frequency = 0.05; // 频率
      
      this.ctx.moveTo(0, height / 2);
      
      for (let x = 0; x < width; x++) {
        // 正弦波 + 一点随机性
        const y = height / 2 + Math.sin(x * frequency) * amplitude;
        this.ctx.lineTo(x, y);
      }
      
      this.ctx.stroke();
    }
  }
};
</script>

<style scoped>
.audio-visualizer-container {
  width: 100%;
  height: 100%;
  overflow: hidden;
  border-radius: 4px;
  background-color: #fafafa;
  box-shadow: inset 0 0 3px rgba(0, 0, 0, 0.1);
}

.audio-visualizer-canvas {
  width: 100%;
  height: 100%;
  display: block;
}
</style>