<template>
  <a-layout class="register-container">
    <!-- 背景效果 -->
    <div class="background-animation">
      <div class="circuit-board"></div>
      <div class="gradient-overlay"></div>
    </div>
    
    <!-- 主要内容区 -->
    <a-row type="flex" justify="center" align="middle" class="full-height">
      <a-col :xs="24" :sm="22" :md="18" :lg="16" :xl="14">
        <a-card class="register-panel" :bordered="false">
          <a-row type="flex">
            <!-- 系统信息区 -->
            <a-col :xs="24" :md="12" class="system-info">
              <div class="system-logo">
                <a-icon type="api" class="logo-icon" />
              </div>
              <a-typography-title level={1} class="system-title">小智 ESP32</a-typography-title>
              <a-typography-paragraph class="system-subtitle">智能物联网管理平台</a-typography-paragraph>
            </a-col>
            
            <!-- 注册表单区 -->
            <a-col :xs="24" :md="12" class="register-form-container">
              <div class="form-header">
                <a-typography-title level={3} class="form-title">注册账号</a-typography-title>
                <a-typography-paragraph class="form-subtitle">
                  {{ currentStep === 1 ? '填写账号信息' : 
                     currentStep === 2 ? '验证邮箱' : '注册完成' }}
                </a-typography-paragraph>
              </div>
              
              <!-- 步骤条 -->
              <div class="custom-steps">
                <div 
                  v-for="step in 3" 
                  :key="step"
                  :class="['step-item', currentStep >= step ? 'active' : '', currentStep === step ? 'current' : '']"
                >
                  <div class="step-number">{{ step }}</div>
                  <div class="step-title">
                    {{ step === 1 ? '填写信息' : step === 2 ? '验证邮箱' : '注册完成' }}
                  </div>
                </div>
              </div>
              
              <!-- 步骤1：填写基本信息 -->
              <a-form-model
                v-if="currentStep === 1"
                ref="baseInfoForm"
                :model="formData"
                :rules="formRules"
                class="register-form"
              >
                <!-- 添加姓名字段（必填） -->
                <a-form-model-item prop="name">
                  <a-input
                    v-model="formData.name"
                    size="large"
                    placeholder="请输入姓名"
                    class="custom-input"
                  >
                    <a-icon slot="prefix" type="user" />
                  </a-input>
                </a-form-model-item>

                <a-form-model-item prop="username">
                  <a-input
                    v-model="formData.username"
                    size="large"
                    placeholder="请输入用户名"
                    class="custom-input"
                  >
                    <a-icon slot="prefix" type="idcard" />
                  </a-input>
                </a-form-model-item>

                <a-form-model-item prop="email">
                  <a-input
                    v-model="formData.email"
                    size="large"
                    placeholder="请输入邮箱"
                    class="custom-input"
                  >
                    <a-icon slot="prefix" type="mail" />
                  </a-input>
                </a-form-model-item>

                <!-- 添加电话字段（选填） -->
                <a-form-model-item prop="tel">
                  <a-input
                    v-model="formData.tel"
                    size="large"
                    placeholder="请输入手机号码（选填）"
                    class="custom-input"
                  >
                    <a-icon slot="prefix" type="phone" />
                  </a-input>
                </a-form-model-item>

                <a-form-model-item prop="password">
                  <a-popover
                    placement="right"
                    trigger="focus"
                    :visible="passwordLevelChecked && !isMobileScreen"
                    @visibleChange="handlePasswordPopover"
                    overlayClassName="password-popover"
                  >
                    <template slot="content">
                      <div
                        style="width: 240px"
                        class="password-strength-container"
                      >
                        <div :class="['password-strength', passwordLevelClass]">
                          强度：<span>{{ passwordLevelName }}</span>
                        </div>
                        <a-progress
                          :percent="passwordPercent"
                          :showInfo="false"
                          :strokeColor="passwordLevelColor"
                        />
                        <div style="margin-top: 10px">
                          <span
                            >请至少输入 6
                            个字符。请不要使用容易被猜到的密码。</span
                          >
                        </div>
                      </div>
                    </template>

                    <a-input-password
                      v-model="formData.password"
                      size="large"
                      placeholder="请输入密码"
                      class="custom-input"
                      @focus="onPasswordFocus"
                      @input="checkPasswordStrength"
                    >
                      <a-icon slot="prefix" type="lock" />
                    </a-input-password>
                  </a-popover>
                  
                  <!-- 移动设备上显示的简化版密码强度指示器 -->
                  <div v-if="isMobileScreen && formData.password" class="mobile-password-strength">
                    <span>密码强度：</span>
                    <span :class="['strength-text', passwordLevelClass]">{{ passwordLevelName }}</span>
                  </div>
                </a-form-model-item>

                <a-form-model-item prop="confirmPassword">
                  <a-input-password
                    v-model="formData.confirmPassword"
                    size="large"
                    placeholder="请确认密码"
                    class="custom-input"
                  >
                    <a-icon slot="prefix" type="safety" />
                  </a-input-password>
                </a-form-model-item>

                <a-button
                  type="primary"
                  :loading="loading"
                  class="register-button"
                  block
                  size="large"
                  @click="goToStep2"
                >
                  <span>下一步</span>
                  <a-icon type="arrow-right" />
                </a-button>
              </a-form-model>

              <!-- 步骤2：邮箱验证 -->
              <a-form-model
                v-if="currentStep === 2"
                ref="verifyForm"
                :model="formData"
                :rules="formRules"
                class="register-form"
              >
                <a-alert
                  class="email-alert"
                  type="info"
                  show-icon
                  :message="`验证码已发送至 ${formData.email}`"
                  banner
                />

                <a-form-model-item prop="verifyCode">
                  <a-input
                    v-model="formData.verifyCode"
                    size="large"
                    placeholder="请输入验证码"
                    class="custom-input"
                  >
                    <a-icon slot="prefix" type="safety-certificate" />
                  </a-input>
                </a-form-model-item>

                <a-row
                  type="flex"
                  justify="space-between"
                  align="middle"
                  class="form-options"
                >
                  <a-col>
                    <a-button
                      type="link"
                      class="resend-btn"
                      @click="sendVerifyCode"
                      :disabled="countdown > 0"
                    >
                      {{
                        countdown > 0 ? `重新发送(${countdown}s)` : "重新发送"
                      }}
                    </a-button>
                  </a-col>
                </a-row>

                <a-button
                  type="primary"
                  :loading="loading"
                  class="register-button"
                  block
                  size="large"
                  @click="verifyCode"
                >
                  <span>验证</span>
                  <a-icon type="arrow-right" />
                </a-button>
              </a-form-model>

              <!-- 步骤3：注册完成 -->
              <div v-if="currentStep === 3" class="register-success">
                <a-icon
                  type="check-circle"
                  theme="filled"
                  class="success-icon"
                />
                <h2 class="success-title">注册成功</h2>
                <p class="success-message">
                  您的账号已创建成功，现在可以登录系统
                </p>

                <a-button
                  type="primary"
                  class="register-button"
                  block
                  size="large"
                  @click="goToLogin"
                >
                  <span>立即登录</span>
                  <a-icon type="login" />
                </a-button>
              </div>

              <a-divider style="margin-top: 25px; margin-bottom: 15px" />

              <div class="form-footer">
                <router-link to="/login" class="back-link">
                  <a-icon type="arrow-left" /> 返回登录
                </router-link>
              </div>
            </a-col>
          </a-row>
        </a-card>
      </a-col>
    </a-row>
  </a-layout>
</template>

<script>
// 脚本部分保持不变
import axios from "@/services/axios";
import api from "@/services/api";

// 密码强度级别
const levelNames = {
  0: "低",
  1: "低",
  2: "中",
  3: "强",
};
const levelClass = {
  0: "error",
  1: "error",
  2: "warning",
  3: "success",
};
const levelColor = {
  0: "#ff0000",
  1: "#ff0000",
  2: "#ff7e05",
  3: "#52c41a",
};

export default {
  data() {
    // 确认密码验证
    const validateConfirmPassword = (rule, value, callback) => {
      if (value !== this.formData.password) {
        callback(new Error("两次输入的密码不一致"));
      } else {
        callback();
      }
    };

    // 密码强度验证
    const validatePassword = (rule, value, callback) => {
      if (!value) {
        this.passwordLevel = 0;
        callback(new Error("请输入密码"));
        return;
      }

      this.checkPasswordStrength(value);

      if (this.passwordLevel >= 2) {
        callback();
      } else {
        callback(new Error("密码强度不够"));
      }
    };

    // 手机号验证
    const validateTel = (rule, value, callback) => {
      if (!value) {
        // 电话是选填项，空值直接通过
        callback();
        return;
      }
      
      // 简单的手机号格式验证
      if (!/^1[3-9]\d{9}$/.test(value)) {
        callback(new Error("请输入有效的手机号码"));
        return;
      }
      
      callback();
    };

    return {
      currentStep: 1, // 当前步骤
      loading: false,
      formData: {
        name: "", // 添加姓名字段
        username: "",
        email: "",
        tel: "", // 添加电话字段
        password: "",
        confirmPassword: "",
        verifyCode: "",
      },
      formRules: {
        name: [ // 添加姓名验证规则
          { required: true, message: "请输入姓名", trigger: "blur" },
          { min: 2, max: 20, message: "姓名长度为2-20个字符", trigger: "blur" },
        ],
        username: [
          { required: true, message: "请输入用户名", trigger: "blur" },
          {
            min: 3,
            max: 20,
            message: "用户名长度为3-20个字符",
            trigger: "blur",
          },
        ],
        email: [
          { required: true, message: "请输入邮箱地址", trigger: "blur" },
          { type: "email", message: "请输入有效的邮箱地址", trigger: "blur" },
        ],
        tel: [ // 添加电话验证规则（选填）
          { validator: validateTel, trigger: "blur" }
        ],
        password: [
          { required: true, message: "请输入密码", trigger: "blur" },
          { validator: validatePassword, trigger: "change" },
        ],
        confirmPassword: [
          { required: true, message: "请确认密码", trigger: "blur" },
          { validator: validateConfirmPassword, trigger: "change" },
        ],
        verifyCode: [
          { required: true, message: "请输入验证码", trigger: "blur" },
          { len: 6, message: "验证码长度为6位", trigger: "blur" },
        ],
      },
      countdown: 0,
      countdownTimer: null,
      passwordLevel: 0,
      passwordLevelChecked: false,
    };
  },

  computed: {
    passwordLevelClass() {
      return levelClass[this.passwordLevel];
    },
    passwordLevelName() {
      return levelNames[this.passwordLevel];
    },
    passwordLevelColor() {
      return levelColor[this.passwordLevel];
    },
    passwordPercent() {
      return this.passwordLevel * 33;
    },
    isMobileScreen() {
      return window.innerWidth <= 768; // 小于等于768px时认为是移动设备
    },
  },

  mounted() {
    window.addEventListener('resize', this.handleResize);
  },

  beforeDestroy() {
    if (this.countdownTimer) {
      clearInterval(this.countdownTimer);
    }
    window.removeEventListener('resize', this.handleResize);
  },

  methods: {
    // 检查密码强度
    checkPasswordStrength(value) {
      const password =
        typeof value === "string" ? value : this.formData.password;

      let level = 0;
      // 判断这个字符串中有没有数字
      if (/[0-9]/.test(password)) {
        level++;
      }
      // 判断字符串中有没有字母
      if (/[a-zA-Z]/.test(password)) {
        level++;
      }
      // 判断字符串中有没有特殊符号
      if (/[^0-9a-zA-Z_]/.test(password)) {
        level++;
      }
      // 判断字符串长度
      if (password.length < 6) {
        level = 0;
      }

      this.passwordLevel = level;
    },

    // 进入第二步
    goToStep2() {
      this.$refs.baseInfoForm.validate((valid) => {
        if (valid) {
          this.loading = true;

          // 检查用户名和邮箱是否已存在
          axios
            .get({
              url: api.user.checkUser,
              data: {
                username: this.formData.username,
                email: this.formData.email,
              },
            })
            .then((res) => {
              if (res.code === 200) {
                // 用户名和邮箱都可用，发送验证码
                return this.sendVerifyCode();
              } else {
                this.showError(res.message);
              }
            })
            .catch((err) => {
              this.$message.error("服务器错误，请稍后重试");
            })
            .finally(() => {
              this.loading = false;
            });
        }
      });
    },

    // 发送验证码
    sendVerifyCode() {
      this.loading = true;

      // 调用后端API发送验证码
      return axios
        .jsonPost({
          url: api.user.sendEmailCaptcha,
          data: {
            email: this.formData.email,
            type: "register",
          },
        })
        .then((res) => {
          if (res.code === 200) {
            this.$message.success("验证码已发送到您的邮箱");
            this.currentStep = 2;
            this.startCountdown();
          } else {
            this.$message.error(res.message || "验证码发送失败");
          }
        })
        .catch((err) => {
          this.$message.error("服务器错误，请稍后重试");
        })
        .finally(() => {
          this.loading = false;
        });
    },

    // 验证验证码
    verifyCode() {
      this.$refs.verifyForm.validate((valid) => {
        if (valid) {
          this.loading = true;

          // 调用后端API验证验证码并注册
          axios
            .jsonPost({
              url: api.user.add,
              data: {
                name: this.formData.name, // 添加姓名字段
                username: this.formData.username,
                email: this.formData.email,
                tel: this.formData.tel, // 添加电话字段
                password: this.formData.password,
                code: this.formData.verifyCode,
              },
            })
            .then((res) => {
              if (res.code === 200) {
                this.$message.success("注册成功");
                this.currentStep = 3;
              } else {
                this.$message.error(res.message || "验证码错误或已过期");
              }
            })
            .catch((err) => {
              this.$message.error("服务器错误，请稍后重试");
            })
            .finally(() => {
              this.loading = false;
            });
        }
      });
    },

    // 跳转到登录页
    goToLogin() {
      this.$router.push("/login");
    },

    // 开始倒计时
    startCountdown() {
      this.countdown = 60;

      if (this.countdownTimer) {
        clearInterval(this.countdownTimer);
      }

      this.countdownTimer = setInterval(() => {
        if (this.countdown > 0) {
          this.countdown--;
        } else {
          clearInterval(this.countdownTimer);
        }
      }, 1000);
    },

    // 处理窗口大小变化
    handleResize() {
      // 如果是移动设备且弹出框正在显示，则关闭弹出框
      if (this.isMobileScreen && this.passwordLevelChecked) {
        this.passwordLevelChecked = false;
      }
      this.$forceUpdate(); // 强制更新视图
    },
    
    // 密码输入框获得焦点
    onPasswordFocus() {
      // 只在非移动设备上显示密码强度弹出框
      if (!this.isMobileScreen) {
        this.passwordLevelChecked = true;
      }
    },

    // 处理密码强度提示框
    handlePasswordPopover(visible) {
      // 只在非移动设备上处理弹出框状态
      if (!this.isMobileScreen && !visible) {
        this.passwordLevelChecked = false;
      }
    },
  },
};
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
.register-container {
  position: relative;
  width: 100%;
  min-height: 100vh; // 改为最小高度
  background-color: $dark-color;
  font-family: "PingFang SC", "Microsoft YaHei", sans-serif;
  overflow: auto; // 改为auto，允许滚动
}

.full-height {
  min-height: 100vh; // 改为最小高度
  display: flex;
  align-items: center;
  padding: 20px 0; // 添加上下内边距
}

// 背景效果
.background-animation {
  position: fixed; // 改为fixed，使背景固定
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
}

.circuit-board {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(
      90deg,
      rgba(255, 255, 255, 0.07) 1px,
      transparent 1px
    ),
    linear-gradient(0deg, rgba(255, 255, 255, 0.07) 1px, transparent 1px);
  background-size: 20px 20px;
  background-position: center center;
}

.gradient-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: radial-gradient(
      circle at 30% 30%,
      $gradient-start 0%,
      transparent 70%
    ),
    radial-gradient(circle at 70% 70%, $gradient-end 0%, transparent 70%);
  opacity: 0.6;
}

// 注册面板
.register-panel {
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
  background: linear-gradient(
    135deg,
    rgba(29, 57, 196, 0.9) 0%,
    rgba(114, 46, 209, 0.9) 100%
  );
  color: $light-color;
  position: relative;
  overflow: hidden;
  min-height: 360px;
  border-radius: 16px 0 0 16px;

  &::before {
    content: "";
    position: absolute;
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
  0% {
    transform: translate(0, 0);
  }
  100% {
    transform: translate(50px, 50px);
  }
}

.system-logo {
  margin-bottom: 30px;
  position: relative;
  z-index: 1;
}

.logo-icon {
  font-size: 64px;
  color: $light-color;
  background: rgba(255, 255, 255, 0.2);
  padding: 20px;
  border-radius: 50%;
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.2);
}

.system-title,
.system-subtitle {
  color: $light-color !important;
  text-align: center;
  position: relative;
  z-index: 1;
}

.system-title {
  margin-bottom: 10px !important;
  font-weight: 600;
}

.system-subtitle {
  color: rgba(255, 255, 255, 0.8) !important;
  margin-bottom: 0;
}

// 注册表单容器
.register-form-container {
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
}

.form-title {
  color: $light-color !important;
  margin-bottom: 10px !important;
  font-weight: 500;
}

.form-subtitle {
  color: rgba(255, 255, 255, 0.6) !important;
  margin-bottom: 0;
}

// 自定义步骤条
.custom-steps {
  display: flex;
  justify-content: space-between;
  margin-bottom: 30px;
  position: relative;

  &::before {
    content: "";
    position: absolute;
    top: 16px;
    left: 30px;
    right: 30px;
    height: 2px;
    background-color: rgba(255, 255, 255, 0.2);
    z-index: 1;
  }

  .step-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    position: relative;
    z-index: 2;
    flex: 1;

    .step-number {
      width: 32px;
      height: 32px;
      border-radius: 50%;
      background-color: rgb(255, 255, 255);
      color: rgb(0, 0, 0);
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: 500;
      margin-bottom: 8px;
      border: 2px solid transparent;
      transition: all 0.3s;
    }

    .step-title {
      color: rgba(255, 255, 255, 0.5);
      font-size: 12px;
      text-align: center;
      transition: all 0.3s;
      white-space: nowrap;
    }

    &.active {
      .step-number {
        background-color: rgb(255, 255, 255);
        color: $primary-color;
        border-color: $primary-color;
      }

      .step-title {
        color: rgba(255, 255, 255, 0.8);
      }
    }

    &.current {
      .step-number {
        background-color: $primary-color;
        color: $light-color;
      }

      .step-title {
        color: $light-color;
        font-weight: 500;
      }
    }
  }
}

.register-form {
  width: 100%;
  position: relative;
  z-index: 2;
}

// 输入框样式
.custom-input {
  border-radius: 8px !important;
  height: 46px;
  overflow: hidden;
  background: linear-gradient(
    to right,
    rgba(40, 48, 65, 0.6),
    rgba(40, 48, 65, 0.8)
  ) !important;
  border: 1px solid rgba(255, 255, 255, 0.1) !important;
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.1) !important;

  ::v-deep input {
    background: transparent !important;
    color: $light-color !important;
    height: 44px;
    padding-left: 15px !important;

    &::placeholder {
      color: rgba(255, 255, 255, 0.5) !important;
      font-weight: 300;
    }
  }

  ::v-deep .ant-input-prefix {
    color: $accent-color !important;
    margin-right: 12px !important;
    font-size: 18px !important;
    opacity: 0.9;
  }

  &:hover {
    border-color: rgba($primary-color, 0.7) !important;
    background: linear-gradient(
      to right,
      rgba(40, 48, 65, 0.7),
      rgba(40, 48, 65, 0.9)
    ) !important;
  }

  &:focus,
  &:focus-within {
    border-color: $primary-color !important;
    box-shadow: 0 0 0 2px rgba($primary-color, 0.2) !important;
    background: rgba(40, 48, 65, 0.9) !important;
  }
}

// 表单选项
.form-options {
  margin-bottom: 20px;
  position: relative;
  z-index: 2;
}

// 注册按钮
.register-button {
  height: 46px;
  border-radius: 8px;
  font-size: 16px;
  background: linear-gradient(90deg, $primary-color, $accent-color);
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  position: relative;
  z-index: 2;

  span {
    margin-right: 8px;
    font-weight: 500;
  }

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 5px 15px rgba(24, 144, 255, 0.3);
    background: linear-gradient(
      90deg,
      lighten($primary-color, 5%),
      lighten($accent-color, 5%)
    );
  }
}

// 邮件提示
.email-alert {
  margin-bottom: 24px;

  ::v-deep .ant-alert-message {
    color: $primary-color;
  }
}

// 重新发送按钮
.resend-btn {
  color: $primary-color;
  padding: 0;
  height: auto;

  &:hover:not([disabled]) {
    color: lighten($primary-color, 10%);
  }

  &[disabled] {
    color: rgba(255, 255, 255, 0.3);
  }
}

// 密码强度
.password-strength {
  margin-bottom: 8px;

  &.error span {
    color: #ff4d4f;
  }

  &.warning span {
    color: #faad14;
  }

  &.success span {
    color: #52c41a;
  }
}

// 移动设备上的密码强度指示器
.mobile-password-strength {
  margin-top: 8px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
  
  .strength-text {
    font-weight: 500;
    
    &.error {
      color: #ff4d4f;
    }
    
    &.warning {
      color: #faad14;
    }
    
    &.success {
      color: #52c41a;
    }
  }
}

// 注册成功
.register-success {
  text-align: center;
  padding: 20px 0;
}

.success-icon {
  font-size: 64px;
  color: #52c41a;
  margin-bottom: 20px;
}

.success-title {
  font-size: 24px;
  color: $light-color;
  margin-bottom: 10px;
}

.success-message {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.7);
  margin-bottom: 30px;
}

// 页脚
.form-footer {
  text-align: center;
  margin-top: 0;
  position: relative;
  z-index: 2;
}

.back-link {
  color: rgba(255, 255, 255, 0.7);
  transition: all 0.3s ease;

  &:hover {
    color: $primary-color;
  }
}

// 响应式调整
@media (max-width: 768px) {
  .system-info,
  .register-form-container {
    padding: 20px 15px; // 减小内边距
    min-height: auto;
  }

  .system-info {
    border-radius: 16px 16px 0 0;
    padding-top: 30px;
    padding-bottom: 30px;
  }
  
  .register-form-container {
    border-radius: 0 0 16px 16px;
  }

  .custom-steps {
    .step-title {
      font-size: 10px;
    }
  }
  
  // 减小移动端的Logo大小
  .logo-icon {
    font-size: 48px;
    padding: 15px;
  }
  
  // 减小移动端的标题大小
  .system-title {
    font-size: 24px !important;
  }
  
  .system-subtitle {
    font-size: 14px !important;
  }
  
  // 减小表单标题大小
  .form-title {
    font-size: 20px !important;
  }
  
  // 调整表单项间距
  ::v-deep .ant-form-item {
    margin-bottom: 16px;
  }
}

@media (max-width: 480px) {
  .custom-steps {
    &::before {
      left: 16px;
      right: 16px;
    }

    .step-item {
      .step-number {
        width: 28px;
        height: 28px;
        font-size: 12px;
      }

      .step-title {
        font-size: 9px; // 更小的字体
      }
    }
  }
  
  // 更小屏幕上进一步减小内边距
  .system-info,
  .register-form-container {
    padding: 15px 12px;
  }
  
  // 更小的Logo
  .logo-icon {
    font-size: 40px;
    padding: 12px;
    margin-bottom: 20px;
  }
  
  // 更小的标题
  .system-title {
    font-size: 20px !important;
  }
}

// 添加滚动条样式
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.1);
  border-radius: 3px;
}

::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.3);
}
</style>