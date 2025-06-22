<template>
  <div class="chat-component" :style="{ height: height }">
    <!-- 聊天内容区域 -->
    <div class="chat-content" ref="chatContentRef" :style="{ maxHeight: contentMaxHeight }">
      <div v-if="messages.length === 0" class="empty-chat">
        <a-empty :description="emptyText" />
      </div>
      <div v-else class="chat-messages">
        <div v-for="(message, index) in messages" :key="index">
          <!-- 时间戳 -->
          <div v-if="showTimestamp(message, index)" class="message-timestamp">
            {{ formatTimestamp(message.timestamp) }}
          </div>
          
          <!-- 消息内容 -->
          <div class="message-wrapper" :class="message.isUser ? 'user-message' : 'ai-message'">
            <!-- 头像 -->
            <div class="avatar">
              <a-avatar :src="message.isUser ? userAvatar : aiAvatar" :size="avatarSize" />
            </div>
            
            <!-- 消息气泡 -->
            <div class="message-content">
              <div class="message-bubble" :class="{ 'clickable': messageClickable }" @click="onMessageClick(message)">
                <div v-if="message.type === 'text'" class="message-text">
                  {{ message.content }}
                </div>
                <div v-else-if="message.type === 'audio'" class="audio-message">
                  <a-icon type="sound" />
                  <span class="audio-duration">{{ message.duration || '0:00' }}</span>
                  <div class="audio-wave">
                    <div v-for="i in 4" :key="i" class="wave-bar" :class="{ active: message.isPlaying }"></div>
                  </div>
                </div>
                <div v-else-if="message.type === 'stt'" class="message-text stt-message">
                  <a-icon type="audio" /> {{ message.content }}
                </div>
                <div v-else-if="message.type === 'tts'" class="message-text tts-message">
                  <a-icon type="sound" /> {{ message.content }}
                </div>
                <div v-else-if="message.type === 'system'" class="message-text system-message">
                  {{ message.content }}
                </div>
              </div>
              
              <!-- 加载指示器 -->
              <div v-if="message.isLoading" class="loading-indicator">
                <a-spin size="small" />
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 可选的输入区域 -->
    <div v-if="showInput" class="chat-input-area">
      <!-- 文本输入 -->
      <div class="text-input-wrapper">
        <!-- 切换按钮移到左侧 -->
        <div class="input-left-actions" v-if="showVoiceToggle">
          <a-tooltip title="切换语音/文字输入">
            <a-button 
              shape="circle" 
              class="mode-toggle-button"
              :type="isVoiceMode ? 'primary' : 'default'"
              @click="toggleInputMode"
              :disabled="!isConnectedProp"
            >
              <a-icon :type="isVoiceMode ? 'audio' : 'message'" />
            </a-button>
          </a-tooltip>
        </div>
        
        <a-textarea
          v-if="!isVoiceMode"
          v-model="inputMessage"
          :placeholder="inputPlaceholder"
          :auto-size="{ minRows: 1, maxRows: 4 }"
          :disabled="!isConnectedProp"
          @keypress.enter="handleEnterKey"
        />
        
        <!-- 语音输入按钮 -->
        <a-button 
          v-else
          class="record-button" 
          :class="{ recording: isRecording }"
          type="primary" 
          :disabled="!isConnectedProp"
          @touchstart="startRecording"
          @touchend="stopRecording"
          @mousedown="startRecording"
          @mouseup="stopRecording"
          @mouseleave="isRecording && stopRecording"
        >
          {{ isRecording ? '松开结束录音' : '按住说话' }}
        </a-button>
        
        <!-- 发送按钮 -->
        <div class="input-right-actions">
          <a-button 
            v-if="!isVoiceMode"
            type="primary" 
            class="send-button" 
            shape="circle"
            :disabled="!isConnectedProp || !inputMessage.trim()"
            @click="sendTextMessage"
          >
            <a-icon type="right" style="font-size: 16px;" />
          </a-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { 
  // WebSocket相关
  connectionStatus, 
  isConnectedProp, 
  sendTextMessage as wsSendTextMessage,
  startDirectRecording,
  stopDirectRecording,
} from '@/services/websocketService';

// 引入音频处理服务
import { 
  handleBinaryMessage,
  getAudioState
} from '@/services/audioService';

export default {
  name: 'ChatComponent',
  props: {
    // 是否显示输入框
    showInput: {
      type: Boolean,
      default: false
    },
    // 是否显示语音切换按钮
    showVoiceToggle: {
      type: Boolean,
      default: true
    },
    // 传入的消息列表
    messageList: {
      type: Array,
      default: () => []
    },
    // 用户头像
    userAvatar: {
      type: String,
      default: '/assets/user-avatar.png'
    },
    // AI头像
    aiAvatar: {
      type: String,
      default: '/assets/ai-avatar.png'
    },
    // 组件高度
    height: {
      type: String,
      default: '100%'
    },
    // 空消息提示文本
    emptyText: {
      type: String,
      default: '暂无对话记录'
    },
    // 输入框占位符
    inputPlaceholder: {
      type: String,
      default: '输入消息...'
    },
    // 头像大小
    avatarSize: {
      type: Number,
      default: 40
    },
    // 消息是否可点击
    messageClickable: {
      type: Boolean,
      default: false
    },
    isConnectedProp: {
      type: Boolean,
      default: false
    },
    // 内容区域最大高度
    contentMaxHeight: {
      type: String,
      default: 'none'
    }
  },
  data() {
    return {
      inputMessage: '',
      messages: [],
      isVoiceMode: false,
      isRecording: false
    };
  },
  watch: {
    // 监听外部消息列表变化
    messageList: {
      handler(newVal) {
        this.messages = newVal;
        this.scrollToBottom();
      },
      immediate: true,
      deep: true
    }
  },
  mounted() {
    this.scrollToBottom();
  },
  methods: {
    // 处理回车键按下事件
    handleEnterKey(e) {
      // 阻止默认行为（换行）
      e.preventDefault();
      
      // 检查是否按下了修饰键
      if (e.shiftKey || e.ctrlKey || e.altKey || e.metaKey) {
        // 如果按下了修饰键，不发送消息
        return;
      }
      
      // 发送消息
      this.sendTextMessage();
    },
    
    // 滚动到底部
    scrollToBottom() {
      this.$nextTick(() => {
        if (this.$refs.chatContentRef) {
          this.$refs.chatContentRef.scrollTop = this.$refs.chatContentRef.scrollHeight;
        }
      });
    },
    
    // 发送文本消息
    sendTextMessage() {
      const text = this.inputMessage.trim();
      if (!text || !this.isConnectedProp) return;
      
      // 发送到服务器
      wsSendTextMessage(text);
      
      // 清空输入框
      this.inputMessage = '';
      
      // 滚动到底部
      this.scrollToBottom();
    },
    
    // 消息点击事件
    onMessageClick(message) {
      if (this.messageClickable) {
        this.$emit('message-click', message);
      }
    },
    
    // 开始录音
    async startRecording() {
      if (this.isRecording || !this.isConnectedProp) return;
      
      try {
        this.isRecording = true;
        await startDirectRecording();
        this.$emit('recording-start');
      } catch (error) {
        this.isRecording = false;
        this.$message.error('无法启动录音，请检查麦克风权限');
        this.$emit('recording-error', error);
      }
    },

    // 停止录音
    async stopRecording() {
      if (!this.isRecording) return;
      
      try {
        this.isRecording = false;
        await stopDirectRecording();
        this.$emit('recording-stop');
      } catch (error) {
        this.$message.error('停止录音失败');
        this.$emit('recording-error', error);
      }
    },

    // 切换输入模式
    toggleInputMode() {
      this.isVoiceMode = !this.isVoiceMode;
      this.$emit('mode-change', this.isVoiceMode);
    },
    
    // 显示时间戳
    showTimestamp(message, index) {
      if (index === 0) return true;
      
      const prevMsg = this.messages[index - 1];
      if (!prevMsg || !prevMsg.timestamp || !message.timestamp) return true;
      
      // 如果与上一条消息时间间隔超过5分钟，显示时间戳
      const prevTime = prevMsg.timestamp instanceof Date ? prevMsg.timestamp.getTime() : new Date(prevMsg.timestamp).getTime();
      const currTime = message.timestamp instanceof Date ? message.timestamp.getTime() : new Date(message.timestamp).getTime();
      
      return currTime - prevTime > 5 * 60 * 1000;
    },
    
    // 格式化时间戳
    formatTimestamp(timestamp) {
      if (!timestamp) return '';
      
      const date = timestamp instanceof Date ? timestamp : new Date(timestamp);
      return date.toLocaleString();
    },

    // 处理二进制音频消息 - 这是一个代理方法，调用audioService中的处理函数
    async handleBinaryAudioMessage(data) {
      return await handleBinaryMessage(data);
    }
  }
};
</script>

<style scoped>
.chat-component {
  display: flex;
  flex-direction: column;
  background-color: #f5f5f5;
  border-radius: 4px;
  overflow: hidden;
}

.chat-content {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  scroll-behavior: smooth;
}

.empty-chat {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  min-height: 200px;
}

.chat-messages {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.message-timestamp {
  text-align: center;
  margin: 8px 0;
  color: #999;
  font-size: 12px;
}

.message-timestamp::before,
.message-timestamp::after {
  content: '';
  display: inline-block;
  width: 60px;
  height: 1px;
  background-color: #e8e8e8;
  margin: 0 10px;
  vertical-align: middle;
}

.message-wrapper {
  display: flex;
  margin-bottom: 8px;
  width: 100%;
}

.user-message {
  flex-direction: row-reverse;
}

.avatar {
  margin: 0 8px;
  flex-shrink: 0;
}

.message-content {
  max-width: 70%;
  display: flex;
  flex-direction: column;
  min-width: 60px;
}

.message-bubble {
  padding: 10px 14px;
  border-radius: 4px;
  position: relative;
  word-break: break-word;
  width: auto;
  display: inline-block;
  /* 修复消息气泡高度问题 */
  min-height: 0;
  height: auto;
  line-height: 0;
  box-sizing: content-box;
}

.message-bubble.clickable {
  cursor: pointer;
  transition: background-color 0.2s;
}

.user-message .message-bubble {
  background-color: #95ec69;
  color: white;
  border-bottom-right-radius: 4px;
}

.user-message .message-bubble.clickable:hover {
  background-color: #71d970;
}

.ai-message .message-bubble {
  background-color: #fff;
  color: #333;
  border-bottom-left-radius: 4px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.ai-message .message-bubble.clickable:hover {
  background-color: #f9f9f9;
}

.message-text {
  white-space: pre-wrap;
  word-break: break-word;
  min-width: 20px;
  /* 修复文字行高问题 */
  min-height: 0;
  height: auto;
  padding: 0;
  margin: 0;
}

.stt-message {
  color: #232323;
}

.tts-message {
  color: #13c2c2;
}

.system-message {
  color: #fa8c16;
}

.audio-message {
  display: flex;
  align-items: center;
  gap: 8px;
}

.audio-duration {
  font-size: 12px;
}

.audio-wave {
  display: flex;
  align-items: center;
  gap: 2px;
}

.wave-bar {
  width: 3px;
  height: 12px;
  background-color: currentColor;
  opacity: 0.5;
  border-radius: 1px;
}

.wave-bar.active {
  animation: sound-wave 1s infinite ease-in-out;
}

@keyframes sound-wave {
  0%, 100% { height: 4px; }
  50% { height: 16px; }
}

.wave-bar:nth-child(2).active {
  animation-delay: 0.2s;
}

.wave-bar:nth-child(3).active {
  animation-delay: 0.4s;
}

.wave-bar:nth-child(4).active {
  animation-delay: 0.6s;
}

.loading-indicator {
  margin-top: 4px;
  align-self: flex-start;
}

.user-message .loading-indicator {
  align-self: flex-end;
}

.chat-input-area {
  padding: 12px 16px;
  background-color: #f9f9f9;
  border-top: 1px solid #e8e8e8;
}

.text-input-wrapper {
  display: flex;
  width: 100%;
  align-items: center;
}

/* 输入框左侧操作区 */
.input-left-actions {
  margin-right: 8px;
}

/* 输入框右侧操作区 */
.input-right-actions {
  margin-left: 8px;
}

.text-input-wrapper :deep(.ant-input) {
  flex: 1;
  border-radius: 20px;
  padding: 8px 16px;
  background-color: #fff;
}

.mode-toggle-button {
  width: 36px;
  height: 36px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.mode-toggle-button :deep(.anticon) {
  font-size: 16px;
}

.send-button {
  width: 36px;
  height: 36px;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 0;
}

/* 确保图标正确显示 */
.send-button :deep(.anticon) {
  display: inline-flex !important;
  align-items: center;
  justify-content: center;
}

.record-button {
  flex: 1;
  height: 48px;
  border-radius: 24px;
  transition: all 0.3s;
}

.record-button.recording {
  background-color: #ff4d4f;
  border-color: #ff4d4f;
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0% { box-shadow: 0 0 0 0 rgba(255, 77, 79, 0.4); }
  70% { box-shadow: 0 0 0 10px rgba(255, 77, 79, 0); }
  100% { box-shadow: 0 0 0 0 rgba(255, 77, 79, 0); }
}
</style>