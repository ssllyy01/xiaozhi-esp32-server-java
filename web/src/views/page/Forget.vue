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
              <a-typography-title level="{1}" class="system-title"
                >小智 ESP32</a-typography-title
              >
              <a-typography-paragraph class="system-subtitle"
                >智能物联网管理平台</a-typography-paragraph
              >
            </a-col>

            <!-- 忘记密码表单区 -->
            <a-col :xs="24" :md="12" class="login-form-container">
              <div class="form-header">
                <a-typography-title level="{3}" class="form-title"
                  >找回密码</a-typography-title
                >
                <a-typography-paragraph class="form-subtitle">
                  {{
                    currentStep === 1
                      ? "请输入您的注册邮箱"
                      : currentStep === 2
                      ? "请查看邮箱获取验证码"
                      : "请设置新密码"
                  }}
                </a-typography-paragraph>
              </div>

              <!-- 步骤条 - 改用简化版本 -->
              <div class="custom-steps">
                <div
                  v-for="step in 3"
                  :key="step"
                  :class="[
                    'step-item',
                    currentStep >= step ? 'active' : '',
                    currentStep === step ? 'current' : '',
                  ]"
                >
                  <div class="step-number">{{ step }}</div>
                  <div class="step-title">
                    {{
                      step === 1
                        ? "验证邮箱"
                        : step === 2
                        ? "验证码"
                        : "重置密码"
                    }}
                  </div>
                </div>
              </div>

              <!-- 步骤1：输入邮箱 -->
              <a-form-model
                v-if="currentStep === 1"
                ref="emailForm"
                :model="formData"
                :rules="formRules"
                class="login-form"
              >
                <a-form-model-item prop="email">
                  <a-input
                    v-model="formData.email"
                    size="large"
                    placeholder="请输入您的注册邮箱"
                    class="custom-input"
                  >
                    <a-icon slot="prefix" type="mail" />
                  </a-input>
                </a-form-model-item>

                <a-button
                  type="primary"
                  :loading="loading"
                  class="login-button"
                  block
                  size="large"
                  @click="sendResetEmail"
                >
                  <span>发送验证码</span>
                  <a-icon type="arrow-right" />
                </a-button>
              </a-form-model>

              <!-- 步骤2：输入验证码 -->
              <a-form-model
                v-if="currentStep === 2"
                ref="codeForm"
                :model="formData"
                :rules="formRules"
                class="login-form"
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
                      @click="resendEmail"
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
                  class="login-button"
                  block
                  size="large"
                  @click="verifyCode"
                >
                  <span>验证</span>
                  <a-icon type="arrow-right" />
                </a-button>
              </a-form-model>

              <!-- 步骤3：重置密码 -->
              <a-form-model
                v-if="currentStep === 3"
                ref="passwordForm"
                :model="formData"
                :rules="formRules"
                class="login-form"
              >
                <a-form-model-item prop="password">
                  <a-input-password
                    v-model="formData.password"
                    size="large"
                    placeholder="请输入新密码"
                    class="custom-input"
                  >
                    <a-icon slot="prefix" type="lock" />
                  </a-input-password>
                </a-form-model-item>

                <a-form-model-item prop="confirmPassword">
                  <a-input-password
                    v-model="formData.confirmPassword"
                    size="large"
                    placeholder="请确认新密码"
                    class="custom-input"
                  >
                    <a-icon slot="prefix" type="lock" />
                  </a-input-password>
                </a-form-model-item>

                <a-button
                  type="primary"
                  :loading="loading"
                  class="login-button"
                  block
                  size="large"
                  @click="resetPassword"
                >
                  <span>重置密码</span>
                  <a-icon type="arrow-right" />
                </a-button>
              </a-form-model>

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
import axios from "@/services/axios";
import api from "@/services/api";

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

    return {
      currentStep: 1, // 当前步骤
      loading: false,
      formData: {
        email: "",
        verifyCode: "",
        password: "",
        confirmPassword: "",
      },
      formRules: {
        email: [
          { required: true, message: "请输入邮箱地址", trigger: "blur" },
          { type: "email", message: "请输入有效的邮箱地址", trigger: "blur" },
        ],
        verifyCode: [
          { required: true, message: "请输入验证码", trigger: "blur" },
          { len: 6, message: "验证码长度为6位", trigger: "blur" },
        ],
        password: [
          { required: true, message: "请输入新密码", trigger: "blur" },
          { min: 6, message: "密码长度不能少于6个字符", trigger: "blur" },
        ],
        confirmPassword: [
          { required: true, message: "请确认新密码", trigger: "blur" },
          { validator: validateConfirmPassword, trigger: "blur" },
        ],
      },
      countdown: 0,
      countdownTimer: null,
    };
  },

  beforeDestroy() {
    if (this.countdownTimer) {
      clearInterval(this.countdownTimer);
    }
  },

  methods: {
    // 发送重置邮件
    sendResetEmail() {
      this.$refs.emailForm.validate((valid) => {
        if (valid) {
          this.loading = true;

          // 调用后端API发送重置邮件
          axios
            .jsonPost({
              url: api.user.sendEmailCaptcha,
              data: {
                email: this.formData.email,
                type: "forget",
              },
            })
            .then((res) => {
              this.loading = false;

              if (res.code === 200) {
                this.$message.success("验证码已发送到您的邮箱");
                this.currentStep = 2;
                this.startCountdown();
              } else {
                this.showError(res.message);
              }
            })
            .catch((err) => {
              this.loading = false;
              this.$message.error("服务器错误，请稍后重试");
              console.error(err);
            });
        }
      });
    },

    // 重新发送邮件
    resendEmail() {
      if (this.countdown > 0) return;

      this.loading = true;

      // 调用后端API重新发送重置邮件
      axios
        .jsonPost({
          url: api.user.sendEmailCaptcha,
          data: {
            email: this.formData.email,
            type: "forget",
          },
        })
        .then((res) => {
          this.loading = false;

          if (res.code === 200) {
            this.$message.success("验证码已重新发送到您的邮箱");
            this.startCountdown();
          } else {
            this.showError(res.message);
          }
        })
        .catch((err) => {
          this.loading = false;
          this.$message.error("服务器错误，请稍后重试");
          console.error(err);
        });
    },

    // 验证验证码
    verifyCode() {
      this.$refs.codeForm.validate((valid) => {
        if (valid) {
          this.loading = true;

          // 调用后端API验证验证码
          axios
            .get({
              url: api.user.checkCaptcha,
              data: {
                code: this.formData.verifyCode,
                email: this.formData.email,
                type: "forget",
              },
            })
            .then((res) => {
              this.loading = false;

              if (res.code === 200) {
                this.$message.success("验证成功");
                this.currentStep = 3;
              } else {
                this.showError(res.message);
              }
            })
            .catch((err) => {
              this.loading = false;
              this.$message.error("服务器错误，请稍后重试");
              console.error(err);
            });
        }
      });
    },

    // 重置密码
    resetPassword() {
      this.$refs.passwordForm.validate((valid) => {
        if (valid) {
          this.loading = true;

          // 调用后端API重置密码
          axios
            .jsonPost({
              url: api.user.update,
              data: {
                email: this.formData.email,
                code: this.formData.verifyCode,
                password: this.formData.password,
              },
            })
            .then((res) => {
              this.loading = false;

              if (res.code === 200) {
                this.$message.success("密码重置成功");
                setTimeout(() => {
                  this.$router.push("/login");
                }, 1500);
              } else {
                this.showError(res.message);
              }
            })
            .catch((err) => {
              this.loading = false;
              this.$message.error("服务器错误，请稍后重试");
              console.error(err);
            });
        }
      });
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
  },
};
</script>

<style lang="scss" scoped>
// 继承登录页面的样式
.login-container {
  position: relative;
  width: 100%;
  height: 100vh;
  background-color: #001529;
  font-family: "PingFang SC", "Microsoft YaHei", sans-serif;
  overflow: hidden;
}

.full-height {
  height: 100vh;
  display: flex;
  align-items: center;
}

// 背景效果
.background-animation {
  position: absolute;
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
  background: radial-gradient(circle at 30% 30%, #1d39c4 0%, transparent 70%),
    radial-gradient(circle at 70% 70%, #722ed1 0%, transparent 70%);
  opacity: 0.6;
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
  background: linear-gradient(
    135deg,
    rgba(29, 57, 196, 0.9) 0%,
    rgba(114, 46, 209, 0.9) 100%
  );
  color: #ffffff;
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
  color: #ffffff;
  background: rgba(255, 255, 255, 0.2);
  padding: 20px;
  border-radius: 50%;
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.2);
}

.system-title,
.system-subtitle {
  color: #ffffff !important;
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
}

.form-title {
  color: #ffffff !important;
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
        color: #1890ff;
        border-color: #1890ff;
      }

      .step-title {
        color: rgba(255, 255, 255, 0.8);
      }
    }

    &.current {
      .step-number {
        background-color: #1890ff;
        color: #ffffff;
      }

      .step-title {
        color: #ffffff;
        font-weight: 500;
      }
    }
  }
}

.login-form {
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
    color: #ffffff !important;
    height: 44px;
    padding-left: 15px !important;

    &::placeholder {
      color: rgba(255, 255, 255, 0.5) !important;
      font-weight: 300;
    }
  }

  ::v-deep .ant-input-prefix {
    color: #13c2c2 !important;
    margin-right: 12px !important;
    font-size: 18px !important;
    opacity: 0.9;
  }

  &:hover {
    border-color: rgba(24, 144, 255, 0.7) !important;
    background: linear-gradient(
      to right,
      rgba(40, 48, 65, 0.7),
      rgba(40, 48, 65, 0.9)
    ) !important;
  }

  &:focus,
  &:focus-within {
    border-color: #1890ff !important;
    box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2) !important;
    background: rgba(40, 48, 65, 0.9) !important;
  }
}

// 表单选项
.form-options {
  margin-bottom: 20px;
  position: relative;
  z-index: 2;
}

// 登录按钮
.login-button {
  height: 46px;
  border-radius: 8px;
  font-size: 16px;
  background: linear-gradient(90deg, #1890ff, #13c2c2);
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
      lighten(#1890ff, 5%),
      lighten(#13c2c2, 5%)
    );
  }
}

// 邮件提示
.email-alert {
  margin-bottom: 24px;

  ::v-deep .ant-alert-message {
    color: #1890ff;
  }
}

// 重新发送按钮
.resend-btn {
  color: #1890ff;
  padding: 0;
  height: auto;

  &:hover:not([disabled]) {
    color: lighten(#1890ff, 10%);
  }

  &[disabled] {
    color: rgba(255, 255, 255, 0.3);
  }
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
    color: #1890ff;
  }
}

// 响应式调整
@media (max-width: 768px) {
  .system-info,
  .login-form-container {
    padding: 30px 20px;
    min-height: auto;
  }

  .system-info {
    border-radius: 16px 16px 0 0;
  }
  .login-form-container {
    border-radius: 0 0 16px 16px;
  }

  .custom-steps {
    .step-title {
      font-size: 10px;
    }
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
        font-size: 10px;
      }
    }
  }
}
</style>