// 调试服务模块

// 日志最大条数
const MAX_LOGS = 1000;

// 日志存储
let logs = [];

// 添加日志
function log(message, type = 'info') {
  // 创建日志条目
  const entry = {
    message,
    type,
    time: new Date()
  };
  
  // 添加到日志数组
  logs.push(entry);
  
  // 如果日志超过最大条数，删除最旧的
  if (logs.length > MAX_LOGS) {
    logs = logs.slice(-MAX_LOGS);
  }
  
  // 控制台输出
  switch (type) {
    case 'error':
      console.error(`[XiaoZhi] ${message}`);
      break;
    case 'warning':
      console.warn(`[XiaoZhi] ${message}`);
      break;
    case 'success':
      console.log(`%c[XiaoZhi] ${message}`, 'color: green');
      break;
    case 'debug':
      console.debug(`[XiaoZhi] ${message}`);
      break;
    default:
      console.log(`[XiaoZhi] ${message}`);
  }
  
  return entry;
}

// 获取所有日志
function getLogs() {
  return [...logs];
}

// 清空日志
function clearLogs() {
  logs = [];
  return [];
}

export {
  log,
  getLogs,
  clearLogs
};