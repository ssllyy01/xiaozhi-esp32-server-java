<template>
  <div class="audio-player-container">
    <div class="player-controls">
      <a-button
        type="primary"
        shape="circle"
        size="small"
        @click="togglePlay"
        :loading="loading"
      >
        <a-icon :type="isPlaying ? 'pause' : 'caret-right'" />
      </a-button>
    </div>
    <div class="waveform-container" ref="waveform"></div>
  </div>
</template>

<script>
import WaveSurfer from "wavesurfer.js";
import EventBus from "@/utils/eventBus";
import { getResourceUrl } from "@/services/axios";

export default {
  name: "AudioPlayer",
  props: {
    audioUrl: {
      type: String,
      required: true,
    },
    autoPlay: {
      type: Boolean,
      default: false,
    },
  },
  data() {
    return {
      wavesurfer: null,
      isPlaying: false,
      loading: true,
      playerId: null, // 添加一个唯一标识符
    };
  },
  mounted() {
    this.$nextTick(() => {
      // 生成唯一ID
      this.playerId = `player_${Date.now()}_${Math.floor(
        Math.random() * 1000
      )}`;
      this.initWaveSurfer();

      // 监听其他播放器的播放事件
      EventBus.$on("audio-play", (playerId) => {
        // 如果不是当前播放器触发的事件，则暂停当前播放
        if (playerId !== this.playerId && this.isPlaying) {
          this.wavesurfer.pause();
        }
      });

      // 监听全局停止事件
      EventBus.$on("stop-all-audio", () => {
        if (this.wavesurfer && this.isPlaying) {
          this.wavesurfer.pause();
        }
      });
    });
  },
  beforeDestroy() {
    if (this.wavesurfer) {
      // 在销毁前确保先暂停音频播放
      if (this.isPlaying) {
        this.wavesurfer.pause();
      }
      this.wavesurfer.destroy();
    }
    // 移除事件监听
    EventBus.$off("audio-play");
    EventBus.$off("stop-all-audio");
  },
  watch: {
    audioUrl: {
      handler(newUrl) {
        if (this.wavesurfer && newUrl) {
          this.loading = true;
          this.loadAudio(newUrl);
        }
      },
      immediate: false,
    },
  },
  methods: {
    initWaveSurfer() {
      // 创建wavesurfer实例，使用WebAudio后端
      this.wavesurfer = WaveSurfer.create({
        container: this.$refs.waveform,
        waveColor: "#ddd",
        progressColor: "#1890ff",
        cursorColor: "transparent",
        barWidth: 2,
        barRadius: 2,
        barGap: 1,
        height: 40,
        responsive: true,
        normalize: true,
        backend: "WebAudio", // 使用WebAudio后端而不是MediaElement
      });

      // 事件监听
      this.wavesurfer.on("ready", () => {
        this.loading = false;

        // 如果设置了自动播放，则在音频加载完成后自动播放
        if (this.autoPlay) {
          this.wavesurfer.play();
        }
      });

      this.wavesurfer.on("play", () => {
        this.isPlaying = true;
        // 通知其他播放器，当前播放器正在播放
        EventBus.$emit("audio-play", this.playerId);
      });

      this.wavesurfer.on("pause", () => {
        this.isPlaying = false;
      });

      this.wavesurfer.on("finish", () => {
        this.isPlaying = false;
        // 播放结束后将游标重置到开始位置
        this.wavesurfer.seekTo(0);
      });

      this.wavesurfer.on("error", (err) => {
        console.error("音频加载失败:", err);
        this.$message.error({ content: "音频加载失败", key: "audioError" });
        this.loading = false;
      });

      // 加载音频
      if (this.audioUrl) {
        this.loadAudio(this.audioUrl);
      }
    },
    loadAudio(url) {
      if (!url) return;
      
      // 检查是否为Blob URL或者Data URL
      if (url.startsWith('blob:') || url.startsWith('data:')) {
        // 对于Blob URL和Data URL，直接使用，不需要处理
        this.wavesurfer.load(url);
      } else {
        // 使用统一的资源URL处理函数处理普通URL
        const audioUrl = getResourceUrl(url);
        this.wavesurfer.load(audioUrl);
      }
    },
    togglePlay() {
      if (this.loading) return;

      if (this.wavesurfer) {
        this.wavesurfer.playPause();
      }
    },
  },
};
</script>

<style scoped>
.audio-player-container {
  display: flex;
  align-items: center;
  width: 100%;
  padding: 5px;
}

.player-controls {
  margin-right: 10px;
}

.waveform-container {
  flex: 1;
  height: 40px;
}
</style>