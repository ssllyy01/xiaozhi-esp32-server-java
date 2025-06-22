<template>
  <a-layout>
    <a-layout-content>
      <div class="layout-content-margin">
        <a-card title="个人设置" :bordered="false">
          <a-row>
            <!-- 信息 -->
            <a-col :md="24" :lg="12">
              <a-form
                layout="vertical"
                :form="infoForm"
                :colon="false"
                @submit="submit"
              >
                <a-form-item label="姓名">
                  <a-input
                    v-decorator="[
                      'name'
                    ]"
                    autocomplete="off"
                    placeholder="请输入自己的姓名"
                  />
                </a-form-item>
                <a-form-item label="手机">
                  <a-input
                    v-decorator="['tel', {rules: [{ required: false, message: '请输入正确的手机号', pattern: /^1[3456789]\d{9}$/ }], validateTrigger: ['change', 'blur']}]"
                    placeholder="请输入手机号码"
                  />
                </a-form-item>
                <a-form-item label="电子邮件">
                  <a-input
                    v-decorator="['email', {rules: [{ required: false, type: 'email', message: '请输入邮箱地址' }], validateTrigger: ['change', 'blur']}]"
                    placeholder="请输入邮箱地址"
                  />
                </a-form-item>
                <a-popover
                  :placement="state.placement"
                  :trigger="['focus']"
                  :getPopupContainer="(trigger) => trigger.parentElement"
                  :visible="state.passwordLevelChecked"
                >
                  <template slot="content">
                    <div :style="{ width: '240px' }" >
                      <div :class="['user-register', passwordLevelClass]">强度：<span>{{ passwordLevelName }}</span></div>
                      <a-progress :percent="state.percent" :showInfo="false" :strokeColor=" passwordLevelColor " />
                      <div style="margin-top: 10px;">
                        <span>请至少输入 6 个字符。请不要使用容易被猜到的密码。</span>
                      </div>
                    </div>
                  </template>
                  <a-form-item label="密码">
                    <a-input-password
                      @click="passwordInputClick"
                      :visibilityToggle="false"
                      placeholder="至少6位密码，区分大小写"
                      v-decorator="['password', {rules: [{ required: state.passwordRequire, message: '至少6位密码，区分大小写'}, { validator: passwordLevel }], validateTrigger: ['change', 'blur']}]"
                    ></a-input-password>
                  </a-form-item>
                </a-popover>
                <a-form-item label="确认密码">
                  <a-input-password
                    placeholder="确认密码"
                    :visibilityToggle="false"
                    v-decorator="['password2', {rules: [{ required: state.passwordRequire, message: '至少6位密码，区分大小写' }, { validator: passwordCheck }], validateTrigger: ['change', 'blur']}]"
                  ></a-input-password>
                </a-form-item>
                <a-form-item>
                  <a-button type="primary" html-type="submit">保存</a-button>
                </a-form-item>
              </a-form>
            </a-col>
            <a-col :md="24" :lg="12">
              <div class="ant-upload-preview" @click="$refs.modal.edit(1)" >
                <a-icon type="cloud-upload-o" class="upload-icon"/>
                <div class="mask">
                  <a-icon type="plus" />
                </div>
                <img :src="user.avatar"/>
              </div>
            </a-col>
          </a-row>
        </a-card>
        <!-- 头像 -->
        <avatar-modal ref="modal" @ok="setavatar" />
      </div>
    </a-layout-content>
  </a-layout>
</template>

<script>
import axios from '@/services/axios'
import api from '@/services/api'
import AvatarModal from './AvatarModal'
import Cookies from 'js-cookie'

const levelNames = {
  0: '低',
  1: '低',
  2: '中',
  3: '强'
}
const levelClass = {
  0: 'error',
  1: 'error',
  2: 'warning',
  3: 'success'
}
const levelColor = {
  0: '#ff0000',
  1: '#ff0000',
  2: '#ff7e05',
  3: '#52c41a'
}

export default {
  components: {
    AvatarModal
  },
  data () {
    return {
      // 密码状态
      state: {
        placement: 'rightTop',
        passwordLevel: 0,
        passwordLevelChecked: false,
        percent: 10,
        progressColor: '#FF0000',
        passwordRequire: false
      },
      // 用户信息
      user: {},
      infoForm: this.$form.createForm(this)
    }
  },
  computed: {
    /* 判断设备 */
    isMobile () {
      return this.$store.getters.MOBILE_TYPE
    },
    /* 人员信息 */
    info () {
      return this.$store.getters.USER_INFO
    },
    /* 密码状态 */
    passwordLevelClass () {
      return levelClass[this.state.passwordLevel]
    },
    passwordLevelName () {
      return levelNames[this.state.passwordLevel]
    },
    passwordLevelColor () {
      return levelColor[this.state.passwordLevel]
    }
  },
  created () {
    this.user = this.info
  },
  methods: {
    /* 自定义验证规则 */
    passwordLevel (rules, value, callback) {
      let level = 0
      if (!value) {
        this.state.passwordRequire = false
        return callback()
      } else {
        this.state.passwordRequire = true
      }
      // 判断这个字符串中有没有数字
      if (/[0-9]/.test(value)) {
        level++
      }
      // 判断字符串中有没有字母
      if (/[a-zA-Z]/.test(value)) {
        level++
      }
      // 判断字符串中有没有特殊符号
      if (/[^0-9a-zA-Z_]/.test(value)) {
        level++
      }
      // 判断字符串长度
      if (value.length < 6) {
        level = 0
      }
      this.state.passwordLevel = level
      this.state.percent = level * 30
      if (level >= 2) {
        if (level >= 3) {
          this.state.percent = 100
        }
        callback()
      } else {
        if (level === 0) {
          this.state.percent = 10
        }
        callback(new Error('密码强度不够'))
      }
    },
    /* 二次密码确认 */
    passwordCheck (rule, value, callback) {
      const password = this.infoForm.getFieldValue('password')
      if (value && password && value.trim() !== password.trim()) {
        callback(new Error('两次密码不一致'))
      }
      callback()
    },
    /* 手机端则不显示密码强度框 */
    passwordInputClick () {
      if (this.isMobile) {
        this.state.passwordLevelChecked = false
      } else {
        this.state.passwordLevelChecked = true
      }
    },
    /* 提交按钮 */
    submit (e) {
      e.preventDefault()
      this.infoForm.validateFields((err, values) => {
        if (!err && this.infoForm.isFieldsTouched()) {
          axios
            .jsonPost({
              url: api.user.update,
              data: {
                username: this.user.username,
                ...values
              }
            }).then(res => {
              if (res.code === 200) {
                Cookies.set('userInfo', JSON.stringify(res.data), { expires: 30 })
                this.$store.commit('USER_INFO', res.data)
                this.$message.success(res.message)
              } else {
                this.$message.error(res.message)
              }
            }).catch(() => {
              this.showError();
            })
        }
      })
    },
    setavatar (url) {
      this.user.avatar = url
      Cookies.set('userInfo', JSON.stringify(this.user), { expires: 30 })
      this.$store.commit('USER_INFO', this.user)
    }
  }
}
</script>

<style lang="scss" scoped>
.ant-form-vertical >>> .ant-form-item {
  margin-bottom: 24px;
}

.ant-upload-preview {
    position: relative;
    margin: 0 auto;
    width: 100%;
    max-width: 180px;
    border-radius: 50%;
    box-shadow: 0 0 4px #ccc;

    .upload-icon {
      position: absolute;
      top: 0;
      right: 10px;
      font-size: 1.4rem;
      padding: 0.5rem;
      background: rgba(222, 221, 221, 0.7);
      border-radius: 50%;
      border: 1px solid rgba(0, 0, 0, 0.2);
    }
    .mask {
      opacity: 0;
      position: absolute;
      background: rgba(0,0,0,0.4);
      cursor: pointer;
      transition: opacity 0.4s;

      &:hover {
        opacity: 1;
      }

      i {
        font-size: 2rem;
        position: absolute;
        top: 50%;
        left: 50%;
        margin-left: -1rem;
        margin-top: -1rem;
        color: #d6d6d6;
      }
    }

    img, .mask {
      width: 100%;
      max-width: 180px;
      height: 100%;
      border-radius: 50%;
      overflow: hidden;
    }
  }

.user-register {

  &.error {
    color: #ff0000;
  }

  &.warning {
    color: #ff7e05;
  }

  &.success {
    color: #52c41a;
  }

}
</style>
