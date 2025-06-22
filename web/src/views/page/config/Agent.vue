<template>
  <a-layout>
    <a-layout-content>
      <div class="layout-content-margin">
        <!-- 查询框 -->
        <div class="table-search">
          <a-form layout="horizontal" :colon="false" :labelCol="{ span: 6 }" :wrapperCol="{ span: 16 }">
            <a-row class="filter-flex">
              <a-col :xl="8" :lg="12" :xs="24">
                <a-form-item label="平台">
                  <a-select v-model="query.provider" @change="getData()">
                    <a-select-option v-for="item in providerOptions" :key="item.value" :value="item.value">
                      {{ item.label }}
                    </a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :xl="8" :lg="12" :xs="24">
                <a-form-item label="智能体名称">
                  <a-input-search v-model="query.agentName" placeholder="请输入" allow-clear @search="getData()" />
                </a-form-item>
              </a-col>
            </a-row>
          </a-form>
        </div>

        <!-- 表格数据 -->
        <a-card title="智能体管理" :bodyStyle="{ padding: 0 }" :bordered="false">
          <template slot="extra">
            <a-button type="primary" @click="handleAddPlatform" style="margin-right: 8px">
              <a-icon type="plus" />添加平台配置
            </a-button>
          </template>
          <a-table rowKey="configId" :columns="getTableColumns" :data-source="agentList" :loading="loading"
            :pagination="pagination" @change="handleTableChange" size="middle" :scroll="{ x: 1000 }">
            <!-- Icon -->
            <template slot="iconUrl" slot-scope="text, record">
              <a-avatar :src="record.iconUrl" shape="square" size="large" />
            </template>
            <!-- 智能体名称 -->
            <template slot="agentName" slot-scope="text, record">
              <a-tooltip :title="text" :mouseEnterDelay="0.5" placement="leftTop">
                <span v-if="text">{{ text }}</span>
                <span v-else style="padding: 0 50px">&nbsp;&nbsp;&nbsp;</span>
              </a-tooltip>
            </template>
            <!-- 平台 -->
            <template slot="provider" slot-scope="text">
              <a-tag color="blue">{{ text }}</a-tag>
            </template>
            <!-- 描述 -->
            <template slot="agentDesc" slot-scope="text">
              <a-tooltip :title="text" :mouseEnterDelay="0.5" placement="leftTop">
                <span v-if="text">{{ text }}</span>
                <span v-else style="padding: 0 50px">&nbsp;&nbsp;&nbsp;</span>
              </a-tooltip>
            </template>
            <!-- 默认状态 -->
            <template slot="isDefault" slot-scope="text">
              <a-tag v-if="text == 1" color="green">默认</a-tag>
              <span v-else>-</span>
            </template>
            <!-- 操作 -->
            <template slot="operation" slot-scope="text, record">
              <a-space>
                <!-- 添加设为默认按钮 -->
                <a v-if="record.isDefault != 1" href="javascript:" :disabled="record.isDefault == 1" @click="setAsDefault(record)">设为默认</a>
                <a-popconfirm title="确定要删除此智能体吗？" @confirm="handleDelete(record)">
                  <a v-if="record.isDefault != 1" href="javascript:" style="color: #ff4d4f">删除</a>
                </a-popconfirm>
              </a-space>
            </template>
          </a-table>
        </a-card>
      </div>
    </a-layout-content>
    <a-back-top />

    <!-- 添加平台配置对话框 -->
    <a-modal title="添加平台配置" :visible="platformModalVisible" :confirm-loading="platformModalLoading"
      @ok="handlePlatformModalOk" @cancel="handlePlatformModalCancel">
      <a-form-model ref="platformForm" :model="platformForm" :rules="platformRules" :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }">
        <a-form-model-item v-for="item in formItems" :key="item.field" :label="item.label" :prop="item.field"
          v-if="!item.condition || (item.condition && platformForm[item.condition.field] === item.condition.value)">
          <a-select v-if="item.type === 'select'" v-model="platformForm[item.field]" :placeholder="item.placeholder">
            <a-select-option v-for="option in item.options" :key="option.value" :value="option.value">
              {{ option.label }}
            </a-select-option>
          </a-select>
          <a-input v-else v-model="platformForm[item.field]" :placeholder="item.placeholder" >
            <template v-if="item.suffix" slot="suffix">
              <span style="color: #999">{{ item.suffix }}</span>
            </template>
          </a-input>
        </a-form-model-item>
        
        <!-- 添加设为默认选项 -->
        <a-form-model-item label="设为默认">
          <a-switch v-model="platformForm.isDefault" />
          <span style="margin-left: 8px; color: #999;">设为默认后将优先使用此配置</span>
        </a-form-model-item>
      </a-form-model>
    </a-modal>
  </a-layout>
</template>

<script>
import axios from "@/services/axios";
import api from "@/services/api";
import mixin from "@/mixins/index";

export default {
  name: 'Agent',
  mixins: [mixin],
  data() {
    return {
      // 查询参数
      query: {
        agentName: '',
        provider: 'coze'
      },
      // 平台选项
      providerOptions: [
        { label: 'Coze', value: 'coze' },
        { label: 'Dify', value: 'dify' }
      ],
      // 表格列定义
      tableColumns: [
        { title: '头像', dataIndex: 'iconUrl', width: 80, align: 'center', scopedSlots: { customRender: 'iconUrl' }, fixed: 'left' },
        { title: '智能体名称', dataIndex: 'agentName', scopedSlots: { customRender: 'agentName' }, width: 150, align: 'center', fixed: 'left', ellipsis: true },
        { title: '平台', dataIndex: 'provider', scopedSlots: { customRender: 'provider' }, width: 80, align: 'center' },
        { title: '智能体描述', dataIndex: 'agentDesc', align: 'center', scopedSlots: { customRender: 'agentDesc' }, ellipsis: true },
        // 添加默认状态列
        { title: '默认', dataIndex: 'isDefault', width: 80, align: 'center', scopedSlots: { customRender: 'isDefault' } },
        { title: '发布时间', dataIndex: 'publishTime', width: 180, align: 'center' },
        { title: '操作', dataIndex: 'operation', scopedSlots: { customRender: 'operation' }, width: 150, align: 'center', fixed: 'right' }
      ],
      // 表格数据
      agentList: [],
      // 平台配置模态框
      platformModalVisible: false,
      platformModalLoading: false,

      // 平台表单对象
      platformForm: {
        configType: 'agent',
        provider: 'coze',
        configName: '',
        configDesc: '',
        appId: '',
        apiKey: '',
        apiSecret: '',
        isDefault: false
      },

      // 表单项配置
      formItems: [
        {
          field: 'provider',
          label: '平台类型',
          type: 'select',
          placeholder: '请选择平台类型',
          options: [
            { label: 'Coze', value: 'coze' },
            { label: 'Dify', value: 'dify' }
          ]
        },
        {
          field: 'appId',
          label: 'Space ID',
          placeholder: '请输入Coze Space ID',
          condition: { field: 'provider', value: 'coze' }
        },
        {
          field: 'apiSecret',
          label: 'Secret token',
          placeholder: '请输入Secret token',
          condition: { field: 'provider', value: 'coze' }
        },
        {
          field: 'apiUrl',
          label: 'apiUrl',
          placeholder: '请输入apiUrl',
          condition: { field: 'provider', value: 'dify' },
          suffix: '/chat_message'
        },
        {
           field: 'apiKey',
           label: 'apiKey',
           placeholder: '请输入apiKey',
           condition: { field: 'provider', value: 'dify' }
        },
      ],

      // 平台表单验证规则
      platformRules: {
        provider: [{ required: true, message: '请选择平台类型', trigger: 'change' }],
        configName: [{ required: true, message: '请输入配置名称', trigger: 'blur' }],
        appId: [{ required: true, message: '请输入Space ID', trigger: 'blur' }],
        apiSecret: [{ required: true, message: '请输入Secret token', trigger: 'blur' }],
        apiUrl: [{ required: true, message: '请输入URL', trigger: 'blur' }],
        apiKey: [{ required: true, message: '请输入apiKey', trigger: 'blur' }]
      }
    }
  },
  computed: {
    // 根据当前选择的平台动态生成表格列
    getTableColumns() {
      // 创建列数组的副本，以免修改原始数据
      const columns = [...this.tableColumns];
      
      // 如果当前选择的是Coze平台，则插入智能体ID列
      if (this.query.provider === 'coze') {
        const botIdColumn = { 
          title: '智能体ID', 
          dataIndex: 'botId', 
          width: 180, 
          align: 'center', 
          scopedSlots: { customRender: 'botId' } 
        }
        // 在第二列后插入智能体ID列
        columns.splice(2, 0, botIdColumn);
      }
      
      return columns;
    }
  },
  created() {
    this.getData()
  },
  watch: {
    // 监听平台类型变化，根据不同平台设置不同的默认值
    'platformForm.provider': function(newVal) {
      if (newVal === 'dify') {
        // 如果切换到Dify平台，设置默认的apiUrl
        this.platformForm.apiUrl = 'https://api.dify.ai/v1';
      }
    }
  },
  methods: {
    // 获取智能体列表
    getData() {
      this.loading = true;

      // 调用后端API获取智能体列表
      axios.get({
        url: api.agent.query,
        data: {
          provider: this.query.provider,
          agentName: this.query.agentName,
          start: this.pagination.page,
          limit: this.pagination.pageSize
        }
      })
        .then(res => {
          if (res.code === 200) {
            this.agentList = res.data.list;
            this.pagination.total = res.data.total;
          } else {
            this.$message.error(res.msg);
          }
        })
        .catch(error => {
          console.error('Error fetching agents:', error);
          this.$message.error('获取智能体列表失败');
        })
        .finally(() => {
          this.loading = false;
        });
    },

    // 添加平台配置按钮点击
    handleAddPlatform() {
      // 重置表单
      this.platformForm = {
        configType: 'agent',
        provider: 'coze',
        appId: '',
        apiSecret: '',
        apiKey: '',
        isDefault: false
      };

      this.platformModalVisible = true;
    },

    // 平台配置模态框确认
    handlePlatformModalOk() {
      this.$refs.platformForm.validate(valid => {
        if (valid) {
          this.platformModalLoading = true;

          // 如果是Dify平台，确保apiUrl有正确的后缀
          if (this.platformForm.provider === 'dify' && this.platformForm.apiUrl) {
            // 确保URL末尾没有斜杠
            let baseUrl = this.platformForm.apiUrl;
            if (baseUrl.endsWith('/')) {
              baseUrl = baseUrl.slice(0, -1);
            }
            this.platformForm.apiUrl = baseUrl;
          }

          // 调用后端API添加配置
          axios.post({
            url: api.config.add,
            data: {
              ...this.platformForm,
              isDefault: this.platformForm.isDefault ? 1 : 0 // 转换为数字
            }
          })
            .then(res => {
              if (res.code === 200) {
                this.$message.success('添加平台配置成功');
                this.platformModalVisible = false;

                // 切换到新添加的平台并刷新列表
                this.query.provider = this.platformForm.provider;
                this.getData();
              } else {
                this.$message.error(res.msg || '添加平台配置失败');
              }
            })
            .catch(error => {
              console.error('Error adding platform config:', error);
              this.$message.error('添加平台配置失败');
            })
            .finally(() => {
              this.platformModalLoading = false;
            });
        }
      });
    },

    // 平台配置模态框取消
    handlePlatformModalCancel() {
      this.platformModalVisible = false;
    },

    // 设置为默认智能体
    setAsDefault(record) {
      this.$confirm({
        title: '确定要将此智能体设为默认吗？',
        content: '设为默认后，系统将优先使用此智能体，原默认智能体将被取消默认状态。',
        okText: '确定',
        cancelText: '取消',
        onOk: () => {
          this.loading = true;
          
          // 调用后端API更新配置为默认
          axios.post({
            url: api.config.update,
            data: {
              configId: record.configId,
              configType: 'llm',
              isDefault: 1
            }
          })
            .then(res => {
              if (res.code === 200) {
                this.$message.success(`已将"${record.agentName}"设为默认智能体`);
                this.getData();
              } else {
                this.showError(res.message);
              }
            })
            .catch(error => {
              this.$message.error('设置默认智能体失败');
            })
            .finally(() => {
              this.loading = false;
            });
        }
      });
    },

    // 删除智能体
    handleDelete(record) {
      axios.post({
        url: api.agent.delete,
        data: { bot_id: record.bot_id }
      })
        .then(res => {
          if (res.code === 200) {
            this.$message.success('删除成功');
            this.getData();
          } else {
            this.$message.error(res.msg || '删除失败');
          }
        })
        .catch(() => {
          this.$message.error('服务器错误，请稍后再试');
        });
    },
  }
}
</script>
