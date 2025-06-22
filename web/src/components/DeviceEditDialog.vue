<template>
  <a-modal v-model="visible" title="设备详情" @ok="handleOk" @cancel="handleClose" width="650px">
    <a-form :form="form" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
      <a-form-item label="设备名称">
        <a-input v-model="form.deviceName"/>
      </a-form-item>
      
      <a-form-item label="绑定角色">
        <a-select v-model="form.roleId">
          <a-select-option v-for="i in roleItems" :key="i.roleId" :value="i.roleId">{{ i.roleName }}</a-select-option>
        </a-select>
      </a-form-item>
    </a-form>
    
    <template slot="footer">
      <a-popconfirm
        title="确定要清除该设备的所有对话记忆吗？此操作不可恢复。"
        ok-text="确定"
        cancel-text="取消"
        @confirm="handleClearMemory"
      >
        <a-button key="clear" type="danger" :loading="clearMemoryLoading">
          清除记忆
        </a-button>
      </a-popconfirm>
      <a-button key="back" @click="handleClose">
        取消
      </a-button>
      <a-button key="submit" type="primary" @click="handleOk">
        确定
      </a-button>
    </template>
  </a-modal>
</template>

<script>
export default {
  name: "DeviceEditDialog",
  props:{
    visible: Boolean,
    roleItems: Array,
    current: Object,
    clearMemoryLoading: Boolean
  },
  data(){
    return {
      form: this.$form.createForm(this, {
        deviceId: "",
        deviceName: "",
        roleId: null
      })
    }
  },
  methods:{
    handleClose(){
      this.$emit("close");
    },
    
    handleOk(){
      this.$emit("submit", this.form)
    },
    
    handleClearMemory() {
      this.$emit("clear-memory", this.form);
    }
  },
  watch:{
    visible(val){
      if(val){
        // 复制当前设备数据到表单
        this.form = Object.assign({}, this.$props.current);
      }
    }
  }
}
</script>

<style scoped>
/* 确保所有下拉框中的文本居中 */
>>> .ant-select-selection__rendered .ant-select-selection-selected-value {
  text-align: center !important;
  width: 100% !important;
}

/* 确保输入框内容居中 */
>>> .ant-input {
  text-align: center !important;
}
</style>