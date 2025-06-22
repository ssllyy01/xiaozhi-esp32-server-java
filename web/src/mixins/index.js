import axios from "@/services/axios";
import api from "@/services/api";
const mixin = {
  data() {
    return {
      // 遮罩层
      loading: true,
      exportLoading: false,
      timeRange: [moment().startOf("month"), moment().endOf("month")],
      time: moment(),
      // 分页
      pagination: {
        total: 0,
        page: 1,
        pageSize: 10,
        showTotal: total => `共 ${total} 条`,
        hideOnSinglePage: false,
        showSizeChanger: true,
        showQuickJumper: true,
        pageSizeOptions: ["10", "30", "50", "100", "1000"],
        onChange: (page, pageSize) => {
          this.pageChange(page, pageSize);
        },
        onShowSizeChange: (page, pageSize) => {
          this.pageChange(page, pageSize);
        },
        style: "padding: 0 24px"
      }
    };
  },
  computed: {
    userInfo() {
      return this.$store.getters.USER_INFO;
    }
  },
  mounted() {
  },
  methods: {
    /* 换页 */
    pageChange(page, pageSize) {
      this.pagination.page = page;
      this.pagination.pageSize = pageSize;
      this.getData();
    },
    /* 编辑单元格操作 */
    inputEdit(value, key, column) {
      const data = this.editLine(key);
      data.target[column] = value;
      this.data = data.newData;
    },
    /* 点击编辑操作 */
    edit(key) {
      // 先取消所有行的编辑状态
      this.data.forEach(item => {
        if (item.editable) {
          delete item.editable;
        }
      });
      const data = this.editLine(key);
      this.editingKey = key;
      data.target.editable = true;
      this.data = data.newData;
    },
    /* 取消按钮 */
    cancel(key) {
      const data = this.editLine(key);
      this.updateFailed(data, key);
      delete data.target.editable;
    },
    /* 更新成功 */
    updateSuccess(data) {
      Object.assign(data.targetCache, data.target);
      this.data = data.newData;
      this.cacheData = data.newCacheData;
      this.editingKey = "";
    },
    /* 更新失败 */
    updateFailed(data, key) {
      Object.assign(
        data.target,
        this.cacheData.filter(
          item => key === item.deviceId || key === item.userId
        )[0]
      );
      this.data = data.newData;
      this.editingKey = "";
    },
    /* 获取点击行修改前后数据 */
    editLine(key) {
      let data = [];
      data.newData = [...this.data];
      data.newCacheData = [...this.cacheData];
      data.target = data.newData.filter(
        item => key === item.deviceId || key === item.userId
      )[0];
      data.targetCache = data.newCacheData.filter(
        item => key === item.deviceId || key === item.userId
      )[0];
      return data;
    },
    /**
     * 显示服务器错误消息
     * 使用全局key确保只显示一个错误消息
     * @param {string} errorMsg 错误消息
     */
    showError(errorMsg = "服务器维护/重启中，请稍后再试") {
      // 使用全局key显示错误消息，如果已存在则会更新而不是创建新的
      this.$message.error({
        content: errorMsg,
        key: "error",
        duration: 3
      });
    }
  }
};
export default mixin;
