<template>
  <a-layout class="login-container">
    <!-- 背景效果 -->
    <div class="background-animation">
      <div class="circuit-board"></div>
      <div class="gradient-overlay"></div>
    </div>
    
    <!-- 主要内容区 -->
    <a-row type="flex" justify="center" align="middle" class="full-height">
      <a-col :xs="22" :sm="20" :md="18" :lg="16" :xl="14">
        <a-card class="login-panel" :bordered="false">
          <a-row type="flex">
            <!-- 系统信息区 -->
            <a-col :xs="24" :md="12" class="system-info">
              <div class="system-logo">
                <a-icon type="api" class="logo-icon" />
              </div>
              <a-typography-title level={1} class="system-title">小智 ESP32</a-typography-title>
              <a-typography-paragraph class="system-subtitle">智能物联网管理平台</a-typography-paragraph>
            </a-col>
            
            <!-- 登录表单区 -->
            <a-col :xs="24" :md="12" class="login-form-container">
              <a-form-model
                id="login"
                ref="loginForm"
                :model="loginForm"
                :rules="loginRules"
                class="login-form"
                @submit="handleSubmit"
              >
                <div class="form-header">
                  <a-typography-paragraph class="form-subtitle">欢迎回来，请登录您的账户</a-typography-paragraph>
                </div>
                
                <a-form-model-item prop="username">
                  <a-input
                    v-model="loginForm.username"
                    size="large"
                    placeholder="用户名"
                    class="custom-input"
                  >
                    <a-icon slot="prefix" type="user" />
                  </a-input>
                </a-form-model-item>
                
                <a-form-model-item prop="password">
                  <a-input-password
                    v-model="loginForm.password"
                    size="large"
                    type="password"
                    placeholder="密码"
                    class="custom-input"
                  >
                    <a-icon slot="prefix" type="lock" />
                  </a-input-password>
                </a-form-model-item>
                
                <a-row type="flex" justify="space-between" align="middle" class="form-options">
                  <a-col>
                    <a-checkbox v-model="loginForm.rememberMe" class="remember-me">
                      记住我
                    </a-checkbox>
                  </a-col>
                  <a-col>
                    <router-link to="forget" class="forgot-password">
                      忘记密码?
                    </router-link>
                  </a-col>
                </a-row>
                
                <a-button 
                  type="primary" 
                  html-type="submit" 
                  class="login-button"
                  :loading="loading"
                  block
                  size="large"
                >
                  <span>登录</span>
                  <a-icon type="arrow-right" />
                </a-button>
                
                <!-- 添加注册按钮 -->
                <div class="register-container">
                  <span class="register-text">还没有账户?</span>
                  <router-link to="register" class="register-link">
                    立即注册
                  </router-link>
                </div>
              </a-form-model>
              
              <a-divider style="margin-top: 25px; margin-bottom: 15px;" />
              
              <a-typography-paragraph class="login-footer">
                © {{ new Date().getFullYear() }} 小智ESP32物联网平台
              </a-typography-paragraph>
            </a-col>
          </a-row>
        </a-card>
      </a-col>
    </a-row>
    
    <!-- 技术信息卡片 - 优化尺寸 -->
    <a-row type="flex" justify="center" class="tech-cards-row">
      <a-col :xs="22" :md="16" :xl="12">
        <a-row type="flex" justify="space-around" :gutter="[16, 16]">
          <a-col :xs="7" :sm="7" :md="7">
            <div class="tech-card">
              <a-icon type="dashboard" />
              <span>实时监控</span>
            </div>
          </a-col>
          <a-col :xs="7" :sm="7" :md="7">
            <div class="tech-card">
              <a-icon type="setting" />
              <span>系统配置</span>
            </div>
          </a-col>
          <a-col :xs="7" :sm="7" :md="7">
            <div class="tech-card">
              <a-icon type="cloud-server" />
              <span>云端管理</span>
            </div>
          </a-col>
        </a-row>
      </a-col>
    </a-row>
    
    <!-- 浮动图标 (保留少量装饰性元素) -->
    <div class="floating-icons">
      <div class="icon-item" v-for="(icon, index) in icons" :key="index" 
           :style="{ left: icon.left + '%', top: icon.top + '%', animationDelay: icon.delay + 's' }">
        <a-icon :type="icon.type" />
      </div>
    </div>
  </a-layout>
</template>

<script>
import axios from '@/services/axios'
import api from '@/services/api'
import Cookies from 'js-cookie'
import { encrypt, decrypt } from '@/utils/jsencrypt'

export default {
  data() {
    return {
      loginForm: {
        username: '',
        password: '',
        rememberMe: false
      },
      loginRules: {
        username: [{ required: true, message: '请输入用户名！', trigger: 'blur' }],
        password: [{ required: true, message: '请输入密码！', trigger: 'blur' }]
      },
      loading: false,
      icons: [
        { type: 'wifi', left: 10, top: 20, delay: 0 },
        { type: 'cloud', left: 85, top: 15, delay: 1.5 },
        { type: 'mobile', left: 20, top: 70, delay: 2.3 },
        { type: 'bulb', left: 95, top: 90, delay: 0.8 },
        { type: 'robot', left: 40, top: 30, delay: 1.2 },
        { type: 'thunderbolt', left: 65, top: 80, delay: 2 }
      ]
    }
  },
  mounted() {
    this.getCookie()
  },
  methods: {
    getCookie() {
      const username = Cookies.get('username')
      const password = Cookies.get('rememberMe')
      this.loginForm = {
        username: username === undefined ? this.loginForm.username : username,
        password: password === undefined ? this.loginForm.password : decrypt(password),
        rememberMe: password === undefined ? false : Boolean(password)
      }
    },
    handleSubmit(e) {
      e.preventDefault()
      this.$refs.loginForm.validate(valid => {
        if (valid) {
          this.loading = true
          axios
            .jsonPost({
              url: api.user.login,
              data: {
                ...this.loginForm
              }
            }).then(res => {
              this.loading = false
              if (res.code === 200) {
                Cookies.set('userInfo', JSON.stringify(res.data), { expires: 30 })
                if (this.loginForm.rememberMe) {
                  Cookies.set('username', this.loginForm.username, { expires: 30 })
                  Cookies.set('rememberMe', encrypt(this.loginForm.password), { expires: 30 })
                } else {
                  Cookies.remove('username')
                  Cookies.remove('rememberMe')
                }
                this.$store.commit('USER_INFO', res.data)
                this.$router.push('/dashboard')
              } else {
                this.$message.error(res.message)
              }
            }).catch(() => {
              this.loading = false
              this.showError();
            })
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
// 全局变量
$primary-color: #1890ff;
$dark-color: #001529;
$light-color: #ffffff;
$accent-color: #13c2c2;
$gradient-start: #1d39c4;
$gradient-end: #722ed1;

// 通用样式
%full-abs {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
}

%flex-center {
  display: flex;
  align-items: center;
  justify-content: center;
}

%z-layer {
  position: relative;
  z-index: 2;
}

// 主容器
.login-container {
  position: relative;
  width: 100%;
  height: 100vh;
  background-color: $dark-color;
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
  overflow: hidden;
}

.full-height {
  height: 100vh;
  display: flex;
  align-items: center;
}

// 背景效果
.background-animation {
  @extend %full-abs;
  z-index: 0;
}

.circuit-board {
  @extend %full-abs;
  background: 
    linear-gradient(90deg, rgba(255,255,255,.07) 1px, transparent 1px),
    linear-gradient(0deg, rgba(255,255,255,.07) 1px, transparent 1px);
  background-size: 20px 20px;
  background-position: center center;
}

.gradient-overlay {
  @extend %full-abs;
  background: radial-gradient(circle at 30% 30%, $gradient-start 0%, transparent 70%),
              radial-gradient(circle at 70% 70%, $gradient-end 0%, transparent 70%);
  opacity: 0.6;
}

// 浮动图标
.floating-icons {
  @extend %full-abs;
  z-index: 1;
  pointer-events: none;
}

.icon-item {
  position: absolute;
  font-size: 24px;
  color: rgba(255, 255, 255, 0.2);
  animation: float 6s ease-in-out infinite;
  
  i {
    font-size: 24px;
  }
}

@keyframes float {
  0%, 100% { transform: translateY(0) rotate(0deg); }
  50% { transform: translateY(-15px) rotate(5deg); }
}

// 登录面板
.login-panel {
  background: transparent !important;
  border-radius: 16px !important;
  overflow: hidden;
  border: none !important;
  z-index: 10;
  position: relative;
  box-shadow: none;
  
  ::v-deep .ant-card-body {
    padding: 0;
  }
}

// 系统信息区
.system-info {
  padding: 40px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, rgba(29, 57, 196, 0.9) 0%, rgba(114, 46, 209, 0.9) 100%);
  color: $light-color;
  position: relative;
  overflow: hidden;
  min-height: 360px;
  border-radius: 16px 0 0 16px;
  
  &::before {
    content: '';
    @extend %full-abs;
    top: -50%;
    left: -50%;
    width: 200%;
    height: 200%;
    background: repeating-linear-gradient(
      45deg,
      rgba(255, 255, 255, 0.05),
      rgba(255, 255, 255, 0.05) 10px,
      transparent 10px,
      transparent 20px
    );
    animation: move-bg 20s linear infinite;
  }
}

@keyframes move-bg {
  0% { transform: translate(0, 0); }
  100% { transform: translate(50px, 50px); }
}

.system-logo {
  margin-bottom: 30px;
  @extend %z-layer;
}

.logo-icon {
  font-size: 64px;
  color: $light-color;
  background: rgba(255, 255, 255, 0.2);
  padding: 20px;
  border-radius: 50%;
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.2);
}

.system-title, .system-subtitle {
  color: $light-color !important;
  text-align: center;
  @extend %z-layer;
}

.system-title {
  margin-bottom: 10px !important;
  font-weight: 600;
}

.system-subtitle {
  color: rgba(255, 255, 255, 0.8) !important;
  margin-bottom: 0;
}

// 登录表单容器
.login-form-container {
  padding: 40px;
  background-color: rgba(18, 24, 38, 0.95);
  position: relative;
  min-height: 360px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  border-radius: 0 16px 16px 0;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
}

.form-header {
  margin-bottom: 25px;
  text-align: center;
  @extend %z-layer;
  
  ::v-deep h2.ant-typography {
    color: $light-color;
    font-size: 24px;
    margin-bottom: 10px;
    font-weight: 500;
    letter-spacing: 1px;
    position: relative;
    display: inline-block;
    
    &::after {
      content: '';
      position: absolute;
      bottom: -5px;
      left: 50%;
      transform: translateX(-50%);
      width: 40px;
      height: 2px;
      background: $accent-color;
    }
  }
}

.form-subtitle {
  color: rgba(255, 255, 255, 0.6) !important;
  text-align: center;
  margin-bottom: 0;
}

.login-form {
  width: 100%;
  @extend %z-layer;
}

// 输入框样式
.custom-input {
  border-radius: 8px !important;
  height: 46px;
  overflow: hidden;
  background: linear-gradient(to right, rgba(40, 48, 65, 0.6), rgba(40, 48, 65, 0.8)) !important;
  border: 1px solid rgba(255, 255, 255, 0.1) !important;
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.1) !important;
  
  input {
    background: transparent !important;
    color: $light-color !important;
    height: 44px;
    padding-left: 15px !important;
    
    &::placeholder {
      color: rgba(255, 255, 255, 0.5) !important;
      font-weight: 300;
    }
  }
  
  .ant-input-prefix {
    color: $accent-color !important;
    margin-right: 12px !important;
    font-size: 18px !important;
    opacity: 0.9;
  }
  
  &:hover {
    border-color: rgba($primary-color, 0.7) !important;
    background: linear-gradient(to right, rgba(40, 48, 65, 0.7), rgba(40, 48, 65, 0.9)) !important;
  }
  
  &:focus, &:focus-within {
    border-color: $primary-color !important;
    box-shadow: 0 0 0 2px rgba($primary-color, 0.2) !important;
    background: rgba(40, 48, 65, 0.9) !important;
  }
  
  .ant-input-password-icon {
    color: rgba(255, 255, 255, 0.7) !important;
    
    &:hover { color: $accent-color !important; }
  }
  
  .ant-input-affix-wrapper {
    background: transparent !important;
    border: none !important;
    
    input { background: transparent !important; }
  }
}

// 表单选项
.form-options {
  margin-bottom: 20px;
  @extend %z-layer;
}

.remember-me {
  color: rgba(255, 255, 255, 0.8);
  
  ::v-deep .ant-checkbox-inner {
    background-color: rgba(30, 38, 55, 0.8);
    border-color: rgba(255, 255, 255, 0.3);
    width: 16px;
    height: 16px;
    
    .ant-checkbox-checked & {
      background-color: $primary-color;
      border-color: $primary-color;
    }
  }
}

.forgot-password {
  color: $accent-color;
  font-size: 14px;
  
  &:hover {
    text-decoration: underline;
    color: lighten($accent-color, 10%);
  }
}

// 登录按钮
.login-button {
  height: 46px;
  border-radius: 8px;
  font-size: 16px;
  background: linear-gradient(90deg, $primary-color, $accent-color);
  border: none;
  @extend %flex-center;
  transition: all 0.3s ease;
  @extend %z-layer;
  
  span {
    margin-right: 8px;
    font-weight: 500;
  }
  
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 5px 15px rgba(24, 144, 255, 0.3);
    background: linear-gradient(90deg, lighten($primary-color, 5%), lighten($accent-color, 5%));
  }
}

// 注册链接容器
.register-container {
  text-align: center;
  margin-top: 20px;
  @extend %z-layer;
}

.register-text {
  color: rgba(255, 255, 255, 0.6);
  font-size: 14px;
  margin-right: 8px;
}

.register-link {
  color: $accent-color;
  font-size: 14px;
  font-weight: 500;
  
  &:hover {
    text-decoration: underline;
    color: lighten($accent-color, 10%);
  }
}

// 页脚
.login-footer {
  color: rgba(255, 255, 255, 0.4) !important;
  font-size: 12px;
  text-align: center;
  margin-bottom: 0;
  @extend %z-layer;
}

// 技术信息卡片
.tech-cards-row {
  position: absolute;
  bottom: 30px;
  width: 100%;
  z-index: 5;
}

.tech-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: rgba(18, 24, 38, 0.75);
  backdrop-filter: blur(10px);
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: $light-color;
  transition: all 0.3s ease;
  padding: 10px;
  height: 80px;
  width: 180px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
  margin: 0 auto;
  
  i {
    font-size: 20px;
    margin-bottom: 5px;
    color: $accent-color;
  }
  
  span { font-size: 12px; }
  
  &:hover {
    transform: translateY(-3px);
    background: rgba(24, 36, 52, 0.85);
    border-color: rgba(255, 255, 255, 0.2);
    box-shadow: 0 6px 16px rgba(0, 0, 0, 0.25);
  }
}

// 响应式调整
@media (max-width: 768px) {
  .system-info, .login-form-container {
    padding: 30px 20px;
    min-height: auto;
  }
  
  .system-info { border-radius: 16px 16px 0 0; }
  .login-form-container { border-radius: 0 0 16px 16px; }
  
  .tech-cards-row {
    position: relative;
    bottom: auto;
    margin-top: -20px;
    margin-bottom: 30px;
  }
  
  .tech-card {
    height: 70px;
    width: 70px;
    
    i { font-size: 18px; }
    span { font-size: 11px; }
  }
}
</style>