import axios from "axios";
const qs = window.Qs;
import { message } from "ant-design-vue";

// 设置axios的基础URL，根据环境变量
axios.defaults.baseURL = process.env.BASE_API;
// 设置携带凭证
axios.defaults.withCredentials = true;

// 创建一个工具函数，用于处理静态资源URL
export const getResourceUrl = (path) => {
  if (!path) return '';

  if (path.startsWith('http://') || path.startsWith('https://')) {
    return path;
  }
  
  // 确保URL以/开头
  if (!path.startsWith('/')) {
    path = '/' + path;
  }
  
  // 开发环境下，需要使用完整的后端地址
  if (process.env.NODE_ENV === 'development') {
    // 开发环境下，我们需要指定后端地址
    // 如果BASE_API为空，则使用默认的localhost:8091
    const backendUrl = process.env.BACKEND_URL || 'http://localhost:8091';
    
    // 移除开头的斜杠，因为我们要将完整的URL传给组件
    if (path.startsWith('/')) {
      path = path.substring(1);
    }
    
    // 构建完整的URL
    return `${backendUrl}/${path}`;
  }
  
  // 生产环境下，直接使用相对路径，由Nginx代理处理
  return path;
};

function Rest() {}
Rest.prototype = {
  jsonPost(opts) {
    return new Promise((resolve, reject) => {
      axios({
        method: "post",
        url: opts.url,
        headers: {
          "Content-Type": "application/json;charset=UTF-8"
        },
        transformRequest: [
          function(data, headers) {
            return JSON.stringify(opts.data);
          }
        ]
      })
        .then(res => {
          commonResponse(res.data, resolve);
        })
        .catch(e => {
          rejectResponse(e, reject);
        });
    });
  },
  post(opts) {
    return new Promise((resolve, reject) => {
      axios({
        method: "post",
        url: opts.url,
        headers: {
          "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8"
        },
        transformRequest: [
          function(data, headers) {
            return qs.stringify(opts.data);
          }
        ]
      })
        .then(res => {
          commonResponse(res.data, resolve);
        })
        .catch(e => {
          rejectResponse(e, reject);
        });
    });
  },
  get(opts) {
    return new Promise((resolve, reject) => {
      axios(opts.url, {
        params: opts.data
      })
        .then(res => {
          commonResponse(res.data, resolve);
        })
        .catch(e => {
          rejectResponse(e, reject);
        });
    });
  }
};
function commonResponse(data, resolve) {
  if (data.code === 403) {
    const key = "error";
    message.error({
      content: "登录过期，请重新登录！",
      type: "error",
      key,
      onClose: () => {
        // 使用环境变量中的BASE_URL，而不是硬编码的URL
        window.location.href = process.env.BASE_URL;
      }
    });
  } else {
    resolve(data);
  }
}

function rejectResponse(e, reject) {
  if (e.response && (e.response.status === 401 || e.response.status === 403)) {
    const key = "error";
    message.error({
      content: "登录过期，请重新登录！",
      type: "error",
      key,
      onClose: () => {
        // 使用环境变量中的BASE_URL，而不是硬编码的URL
        window.location.href = process.env.BASE_URL;
      }
    });
  } else {
    // 修复：这里应该是reject而不是resolve，并且data未定义
    reject(e);
  }
}
export default new Rest();
