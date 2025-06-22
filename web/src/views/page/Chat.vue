<template>
  <div class="chat-container">
    <!-- 聊天头部 -->
    <div class="chat-header">
      <!-- 左侧连接按钮 -->
      <div class="header-left">
        <a-button 
          type="text" 
          class="connection-btn" 
          @click="toggleConnection" 
          :disabled="localConnectionStatus === '正在连接...'"
        >
          <a-icon 
            :type="localIsConnected ? 'disconnect' : 'link'" 
            :style="{ color: localIsConnected ? '#52c41a' : '#ff4d4f' }"
          />
        </a-button>
      </div>
      
      <div class="header-title">
        小智助手
        <a-tag :color="connectionStatusColor">{{ localConnectionStatus }}</a-tag>
      </div>
      
      <div class="header-right">
        <a-dropdown>
          <template #overlay>
            <a-menu>
              <a-menu-item key="1" @click="showServerConfig = true">
                <a-icon type="setting" />
                服务器设置
              </a-menu-item>
              <a-menu-item key="2" @click="showDebugPanel = !showDebugPanel">
                <a-icon type="bug" />
                调试面板
              </a-menu-item>
              <a-menu-item key="3" @click="messages = []">
                <a-icon type="delete" />
                清空聊天
              </a-menu-item>
            </a-menu>
          </template>
          <a-button type="text">
            <a-icon type="more" />
          </a-button>
        </a-dropdown>
      </div>
    </div>
    
    <!-- 使用ChatComponent替换原来的聊天内容和输入区域 -->
    <ChatComponent
      ref="chatComponentRef"
      :message-list="messages"
      :show-input="true"
      :show-voice-toggle="true"
      :user-avatar="userAvatar"
      :ai-avatar="aiAvatar"
      :input-placeholder="'输入消息...'"
      :empty-text="'暂无对话，开始聊天吧'"
      :is-connected-prop="localIsConnected"
      @recording-start="handleRecordingStart"
      @recording-stop="handleRecordingStop"
      @recording-error="handleRecordingError"
      @mode-change="handleModeChange"
    />
    
    <!-- 连接提示 -->
    <a-alert
      v-if="connectionAlert.show"
      :message="connectionAlert.title"
      :description="connectionAlert.message"
      :type="connectionAlert.type"
      class="connection-alert"
      closable
      @close="connectionAlert.show = false"
    >
      <template slot="action">
        <div class="connection-actions">
          <a-button size="small" @click="connect">连接</a-button>
          <a-button size="small" type="primary" @click="showServerConfig = true">配置服务器</a-button>
        </div>
      </template>
    </a-alert>
    
    <!-- 服务器配置对话框 -->
    <a-modal
      v-model="showServerConfig"
      title="服务器配置"
      @ok="saveServerConfig"
      okText="保存"
      cancelText="取消"
    >
      <a-form :form="form" layout="vertical">
        <a-form-item label="服务器地址">
          <a-input v-model="serverConfig.url" placeholder="ws://服务器地址:端口/路径" />
        </a-form-item>
        <a-form-item label="设备ID">
          <a-input v-model="serverConfig.deviceId" placeholder="设备唯一标识" />
        </a-form-item>
        <a-form-item label="设备名称">
          <a-input v-model="serverConfig.deviceName" placeholder="设备名称" />
        </a-form-item>
        <a-form-item label="认证令牌">
          <a-input v-model="serverConfig.token" placeholder="认证令牌" />
        </a-form-item>
      </a-form>
    </a-modal>
    
    <!-- 调试面板 -->
    <div class="debug-panel-container" :class="{ show: showDebugPanel }">
      <div class="debug-panel-header">
        <span>调试面板</span>
        <a-button type="text" @click="showDebugPanel = false">
          <a-icon type="close" />
        </a-button>
      </div>
      <div class="debug-panel">
        <a-space direction="vertical" style="width: 100%">
          <a-card size="small" title="连接状态">
            <p><strong>状态:</strong> {{ localConnectionStatus }}</p>
            <p v-if="connectionTime"><strong>连接时间:</strong> {{ formatTimestamp(connectionTime) }}</p>
            <p v-if="sessionId"><strong>会话ID:</strong> {{ sessionId }}</p>
            <a-space>
              <a-button size="small" @click="connectToServer" :disabled="localIsConnected">连接</a-button>
              <a-button size="small" @click="disconnectFromServer" :disabled="!localIsConnected">断开</a-button>
            </a-space>
          </a-card>
          
          <a-card size="small" title="日志">
            <div class="log-container" ref="logContainerRef">
              <div v-for="(entry, index) in logs" :key="index" class="log-entry" :class="`log-${entry.type}`">
                <span class="log-time">{{ formatLogTime(entry.time) }}</span>
                <span class="log-message">{{ entry.message }}</span>
              </div>
            </div>
          </a-card>
          
          <a-card size="small" title="音频可视化">
            <canvas ref="audioVisualizerRef" class="audio-visualizer"></canvas>
          </a-card>
        </a-space>
      </div>
    </div>
    
    <!-- 添加一个悬浮的重连按钮，当连接断开时显示 -->
    <div v-if="!localIsConnected && !connectionAlert.show" class="floating-reconnect-btn">
      <a-button type="primary" shape="circle" @click="connect" title="重新连接">
        <a-icon type="reload" />
      </a-button>
    </div>
  </div>
</template>

<script>
import { 
  // WebSocket相关
  connectionStatus, 
  connectionTime, 
  sessionId, 
  isConnected, 
  connectToServer, 
  disconnectFromServer, 
  startDirectRecording,
  stopDirectRecording,
  isWebSocketConnected,
  registerMessageHandler,
  
  // 日志相关
  log, 
  getLogs,
  
  // 消息相关
  messages,
  addMessage,
  updateMessage,
  addSystemMessage,
  addAudioMessage
} from '@/services/websocketService';

import { 
  initAudio, 
  handleBinaryMessage,
  getAudioState
} from '@/services/audioService';

// 导入ChatComponent
import ChatComponent from '@/components/ChatComponent';

export default {
  name: 'Chat',
  components: {
    ChatComponent
  },
  data() {
    return {
      // 状态变量
      isVoiceMode: false, // 默认为文字输入模式
      messages: messages, // 使用websocketService中的消息列表
      logs: [],
      showDebugPanel: false,
      showServerConfig: false,
      connectionAlert: {
        show: true,
        title: '未连接到服务器',
        message: '请配置并连接到小智服务器',
        type: 'info'
      },
      // 服务器配置
      serverConfig: {
        url: localStorage.getItem('xiaozhi_server_url') || 'ws://localhost:8091/ws/xiaozhi/v1/',
        deviceId: localStorage.getItem('xiaozhi_device_id') || 'web_test',
        deviceName: localStorage.getItem('xiaozhi_device_name') || '网页客户端',
        token: localStorage.getItem('xiaozhi_token') || ''
      },
      // 头像
      userAvatar: '/assets/user-avatar.png',
      aiAvatar: '/assets/ai-avatar.png',
      form: this.$form.createForm(this),
      // 自动重连计时器
      reconnectTimer: null,
      reconnectAttempts: 0,
      maxReconnectAttempts: 5,
      // 本地连接状态副本
      localConnectionStatus: '未连接',
      localIsConnected: false
    };
  },
  computed: {
    // 使用计算属性获取最新的连接状态
    connectionStatusColor() {
      if (this.localIsConnected) return 'green';
      if (this.localConnectionStatus && (
          this.localConnectionStatus.includes('错误') || 
          this.localConnectionStatus.includes('失败')
      )) return 'red';
      if (this.localConnectionStatus === '正在连接...') return 'blue';
      return 'red';
    }
  },
  watch: {
    // 监听外部连接状态变化
    isConnected: {
      handler(newValue) {
        this.localIsConnected = newValue;
        
        if (newValue) {
          // 连接成功
          this.connectionAlert.show = false;
          this.reconnectAttempts = 0;
          if (this.reconnectTimer) {
            clearTimeout(this.reconnectTimer);
            this.reconnectTimer = null;
          }
        } else {
          // 连接断开
          this.connectionAlert.show = true;
          this.connectionAlert.title = '连接已断开';
          this.connectionAlert.message = '与服务器的连接已断开';
          this.connectionAlert.type = 'warning';
          
          // 设置自动重连
          this.setupAutoReconnect();
        }
      },
      immediate: true
    },
    
    // 监听外部连接状态文本变化
    connectionStatus: {
      handler(newValue) {
        this.localConnectionStatus = newValue || '未连接';
      },
      immediate: true
    }
  },
  mounted() {
    // 初始化音频可视化
    this.initAudioVisualizer();

    // 加载日志
    this.logs = getLogs();

    // 注册消息处理函数
    registerMessageHandler(this.handleServerMessage);

    // 设置全局二进制音频处理函数
    window.handleBinaryAudioMessage = this.handleBinaryAudioMessage;

    // 尝试自动连接
    if (this.serverConfig.url) {
      setTimeout(() => {
        this.connect();
      }, 500);
    }

    // 初始化音频服务
    this.initAudioService();

    // 监听窗口大小变化
    window.addEventListener('resize', this.initAudioVisualizer);

    // 监听日志更新
    this.$watch(
      () => getLogs(),
      (newLogs) => {
        this.logs = newLogs;
        
        // 滚动日志到底部
        this.$nextTick(() => {
          if (this.$refs.logContainerRef) {
            this.$refs.logContainerRef.scrollTop = this.$refs.logContainerRef.scrollHeight;
          }
        });
      },
      { deep: true }
    );
    
    // 定期检查连接状态，确保UI更新
    this.startConnectionStatusPoller();
  },
  beforeDestroy() {
    disconnectFromServer();
    window.removeEventListener('resize', this.initAudioVisualizer);
    
    // 清除全局处理函数
    window.handleBinaryAudioMessage = null;
    
    // 清除重连计时器
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
    }
    
    // 清除状态轮询器
    if (this.statusPoller) {
      clearInterval(this.statusPoller);
    }
  },
  methods: {
    // 开始连接状态轮询
    startConnectionStatusPoller() {
      // 每秒检查一次连接状态，确保UI更新
      this.statusPoller = setInterval(() => {
        // 更新本地连接状态副本
        this.localIsConnected = isConnected;
        this.localConnectionStatus = connectionStatus || '未连接';
      }, 1000);
    },
    
    // 切换连接状态
    toggleConnection() {
      if (this.localIsConnected) {
        this.disconnectFromServer();
      } else {
        this.connect();
      }
    },
    
    // 设置自动重连
    setupAutoReconnect() {
      // 清除之前的计时器
      if (this.reconnectTimer) {
        clearTimeout(this.reconnectTimer);
      }
      
      // 如果尝试次数超过最大值，不再尝试
      if (this.reconnectAttempts >= this.maxReconnectAttempts) {
        log('达到最大重连次数，停止自动重连', 'warning');
        return;
      }
      
      // 计算重连延迟（指数退避策略）
      const delay = Math.min(30000, 1000 * Math.pow(2, this.reconnectAttempts));
      
      this.reconnectTimer = setTimeout(() => {
        if (!isConnected) {
          log(`尝试自动重连 (${this.reconnectAttempts + 1}/${this.maxReconnectAttempts})`, 'info');
          this.reconnectAttempts++;
          this.connect();
        }
      }, delay);
    },

    // 初始化音频服务
    async initAudioService() {
      try {
        await initAudio();
      } catch (error) {
        log(`音频初始化失败: ${error.message}`, 'error');
      }
    },

    // 初始化音频可视化
    initAudioVisualizer() {
      if (!this.$refs.audioVisualizerRef) return;
      
      const canvas = this.$refs.audioVisualizerRef;
      canvas.width = canvas.clientWidth;
      canvas.height = canvas.clientHeight;
      
      const ctx = canvas.getContext('2d');
      if (ctx) {
        ctx.fillStyle = '#fafafa';
        ctx.fillRect(0, 0, canvas.width, canvas.height);
      }
    },

    // 处理服务器消息
    handleServerMessage(data) {
      if (!data || !data.type) return;
      // 处理消息逻辑保持不变
    },
    
    // 处理二进制音频消息
    async handleBinaryAudioMessage(data) {
      try {
        // 调用ChatComponent中的处理方法
        if (this.$refs.chatComponentRef) {
          return await this.$refs.chatComponentRef.handleBinaryAudioMessage(data);
        } else {
          // 如果ChatComponent还没准备好，则使用audioService中的方法
          return await handleBinaryMessage(data);
        }
      } catch (error) {
        log(`处理二进制音频消息失败: ${error.message}`, 'error');
        return false;
      }
    },

    // 连接到服务器
    async connect() {
      try {
        const connected = await connectToServer(this.serverConfig);
        if (connected) {
          this.connectionAlert.show = false;
          this.$message.success('已连接到服务器');
          // 立即更新本地状态
          this.localIsConnected = true;
          this.localConnectionStatus = connectionStatus || '已连接';
        } else {
          this.showConnectionError('连接失败', '无法连接到服务器，请检查配置');
        }
      } catch (error) {
        this.showConnectionError('连接错误', error.message);
      }
    },

    // 显示连接错误
    showConnectionError(title, message) {
      this.connectionAlert.show = true;
      this.connectionAlert.title = title;
      this.connectionAlert.message = message;
      this.connectionAlert.type = 'error';
    },

    // 处理录音开始
    handleRecordingStart() {
      log('录音开始', 'info');
    },

    // 处理录音结束
    handleRecordingStop() {
      log('录音结束', 'info');
    },

    // 处理录音错误
    handleRecordingError(error) {
      log(`录音错误: ${error.message}`, 'error');
    },

    // 处理输入模式变化
    handleModeChange(isVoiceMode) {
      this.isVoiceMode = isVoiceMode;
      log(`切换到${isVoiceMode ? '语音' : '文字'}输入模式`, 'info');
    },

    // 保存服务器配置
    saveServerConfig() {
      localStorage.setItem('xiaozhi_server_url', this.serverConfig.url);
      localStorage.setItem('xiaozhi_device_id', this.serverConfig.deviceId);
      localStorage.setItem('xiaozhi_device_name', this.serverConfig.deviceName);
      localStorage.setItem('xiaozhi_token', this.serverConfig.token);
      
      this.showServerConfig = false;
      
      // 如果已连接但配置改变，提示重新连接
      if (isConnected) {
        this.$message.info('配置已保存，需要重新连接才能生效');
      } else {
        // 尝试连接
        this.connect();
      }
    },

    // 格式化时间戳
    formatTimestamp(timestamp) {
      if (!timestamp) return '';
      
      const date = timestamp instanceof Date ? timestamp : new Date(timestamp);
      return date.toLocaleString();
    },

    // 格式化日志时间
    formatLogTime(time) {
      if (!time) return '';
      
      const date = time instanceof Date ? time : new Date(time);
      return date.toLocaleTimeString();
    },

    // 连接到服务器（调用websocketService）
    connectToServer() {
      return this.connect();
    },

    // 断开连接
    disconnectFromServer() {
      disconnectFromServer();
      // 立即更新本地状态
      this.localIsConnected = false;
      this.localConnectionStatus = '已断开';
      this.$message.info('已断开连接');
    },
  }
};
</script>

<style scoped>
.chat-container {
  display: flex;
  flex-direction: column;
  height: 80vh;
  background-color: #f5f5f5;
  position: relative;
  max-width: 800px;
  margin: 0 auto;
  box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background-color: #fff;
  border-bottom: 1px solid #e8e8e8;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  z-index: 10;
}

.header-left {
  flex: 0 0 auto;
  width: 40px; /* 与右侧按钮宽度保持一致 */
}

.header-title {
  flex: 1;
  text-align: center;
  font-size: 16px;
  font-weight: 500;
  display: flex;
  align-items: center;
  justify-content: center;
}

.header-title .ant-tag {
  margin-left: 8px;
  font-size: 12px;
  padding: 0 6px;
  height: 22px;
  line-height: 22px;
}

.header-right {
  flex: 0 0 auto;
  width: 40px; /* 与左侧按钮宽度保持一致 */
  display: flex;
  justify-content: flex-end;
}

/* 连接按钮样式 */
.connection-btn {
  height: 32px;
  width: 32px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.connection-btn .anticon {
  font-size: 18px;
}

.connection-alert {
  position: fixed;
  top: 60px;
  left: 50%;
  transform: translateX(-50%);
  width: 90%;
  max-width: 500px;
  z-index: 100;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.connection-actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}

/* 悬浮重连按钮 */
.floating-reconnect-btn {
  position: fixed;
  bottom: 20px;
  right: 20px;
  z-index: 90;
}

.floating-reconnect-btn .ant-btn {
  width: 48px;
  height: 48px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.floating-reconnect-btn .anticon {
  font-size: 20px;
}

.debug-panel-container {
  position: fixed;
  top: 0;
  right: -350px;
  width: 350px;
  height: 100vh;
  background-color: #fff;
  box-shadow: -2px 0 8px rgba(0, 0, 0, 0.15);
  transition: right 0.3s;
  z-index: 1000;
  display: flex;
  flex-direction: column;
}

.debug-panel-container.show {
  right: 0;
}

.debug-panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #e8e8e8;
  font-weight: 500;
}

.debug-panel {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.log-container {
  height: 200px;
  overflow-y: auto;
  background-color: #f5f5f5;
  border-radius: 4px;
  padding: 8px;
  font-family: monospace;
  font-size: 12px;
}

.log-entry {
  margin-bottom: 4px;
  line-height: 1.4;
}

.log-time {
  color: #888;
  margin-right: 6px;
}

.log-info {
  color: #333;
}

.log-error {
  color: #f5222d;
}

.log-warning {
  color: #faad14;
}

.log-success {
  color: #52c41a;
}

.log-debug {
  color: #8c8c8c;
}

.audio-visualizer {
  width: 100%;
  height: 100px;
  background-color: #fafafa;
  border-radius: 4px;
}
</style>