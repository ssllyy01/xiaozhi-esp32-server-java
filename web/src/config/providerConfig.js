/**
 * 系统中各类服务提供商配置
 * 统一管理各类服务的提供商信息，便于维护和扩展
 */

// 配置类型信息映射
export const configTypeMap = {
  llm: {
    label: '模型',
    // 模型类别选项
    typeOptions: [
      { label: 'OpenAI', value: 'openai', key: '0' },
      { label: 'Ollama', value: 'ollama', key: '1' },
      { label: 'Spark', value: 'spark', key: '2' },
      { label: 'Zhipu', value: 'zhipu', key: '3' },
      { label: 'AliYun', value: 'aliyun', key: '4' },
      { label: 'Qwen', value: 'qwen', key: '5' },
      { label: 'Doubao', value: 'doubao', key: '6' },
      { label: 'DeepSeek', value: 'deepseek', key: '7' },
      { label: 'ChatGLM', value: 'chatglm', key: '8' },
      { label: 'Gemini', value: 'gemini', key: '9' },
      { label: 'LMStudio', value: 'lmstudio', key: '10' },
      { label: 'Fastgpt', value: 'fastgpt', key: '11' },
      { label: 'Xinference', value: 'xinference', key: '12' },
    ],
    // 各类别对应的参数字段定义
    typeFields: {
      openai: [
        { name: 'apiKey', label: 'API Key', required: true, span: 12 },
        { name: 'apiUrl', label: 'API URL', required: true, span: 12, suffix: '/chat/completions' },
      ],
      ollama: [
        { name: 'apiUrl', label: 'API URL', required: true, span: 12, suffix: '/api/chat', defaultUrl: "http://localhost:11434" }
      ],
      spark: [
        { name: 'apiKey', label: 'API Key', required: true, span: 8 },
        { name: 'apiUrl', label: 'API URL', required: true, span: 12, suffix: '/chat/completions', defaultUrl: "https://spark-api-open.xf-yun.com/v1" }
      ],
      zhipu: [
        { name: 'apiKey', label: 'API Secret', required: true, span: 8 },
        { name: 'apiUrl', label: 'API URL', required: true, span: 12, suffix: 'v4/chat/completions', defaultUrl: "https://open.bigmodel.cn/api/paas" }
      ],
      aliyun: [
        { name: 'apiKey', label: 'API Secret', required: true, span: 8 },
        { name: 'apiUrl', label: 'API URL', required: true, span: 12, suffix: '/chat/completions', defaultUrl: "https://dashscope.aliyuncs.com/compatible-mode/v1" }
      ],
      qwen: [
        { name: 'apiKey', label: 'API Secret', required: true, span: 8 },
        { name: 'apiUrl', label: 'API URL', required: true, span: 12, suffix: '/chat/completions', defaultUrl: "https://dashscope.aliyuncs.com/compatible-mode/v1" }
      ],
      doubao: [
        { name: 'apiKey', label: 'API Secret', required: true, span: 8 },
        { name: 'apiUrl', label: 'API URL', required: true, span: 12, suffix: '/chat/completions', defaultUrl: "https://ark.cn-beijing.volces.com/api/v3" }
      ],
      deepseek: [
        { name: 'apiKey', label: 'API Secret', required: true, span: 8 },
        { name: 'apiUrl', label: 'API URL', required: true, span: 12, suffix: '/chat/completions', defaultUrl: "https://api.deepseek.com" }
      ],
      chatglm: [
        { name: 'apiKey', label: 'API Secret', required: true, span: 8 },
        { name: 'apiUrl', label: 'API URL', required: true, span: 12, suffix: '/chat/completions', defaultUrl: "https://open.bigmodel.cn/api/paas/v4/" }
      ],
      gemini: [
        { name: 'apiKey', label: 'API Secret', required: true, span: 8 },
        { name: 'apiUrl', label: 'API URL', required: true, span: 12, suffix: '/chat/completions', defaultUrl: "https://generativelanguage.googleapis.com/v1beta/" }
      ],
      lmstudio: [
        { name: 'apiUrl', label: 'API URL', required: true, span: 12, suffix: '/chat/completions', defaultUrl: "http://localhost:1234/v1" }
      ],
      fastgpt: [
        { name: 'apiKey', label: 'API Secret', required: true, span: 8 },
        { name: 'apiUrl', label: 'API URL', required: true, span: 12, suffix: '/chat/completions', defaultUrl: "http://localhost:3000/api/v1" }
      ],
      xinference: [
        { name: 'apiKey', label: 'API Secret', required: true, span: 8 },
        { name: 'apiUrl', label: 'API URL', required: true, span: 12, suffix: '/chat/completions', defaultUrl: "http://localhost:9997/v1" }
      ]
    }
  },
  stt: {
    label: '语音识别',
    typeOptions: [
      { label: 'Tencent', value: 'tencent', key: '0' },
      { label: 'Aliyun', value: 'aliyun', key: '1' },
      { label: 'Xfyun', value: 'xfyun', key: '2' },
      { label: 'FunASR', value: 'funasr', key: '3' }
    ],
    typeFields: {
      tencent: [
        { name: 'appId', label: 'App Id', required: true, span: 12 },
        { name: 'apiKey', label: 'Secret Id', required: true, span: 12 },
        { name: 'apiSecret', label: 'Secret Key', required: true, span: 12 },
      ],
      aliyun: [
        { name: 'apiKey', label: 'App Key', required: true, span: 12 }
      ],
      xfyun: [
        { name: 'appId', label: 'App Id', required: true, span: 12 },
        { name: 'apiSecret', label: 'Api Secret', required: true, span: 12 },
        { name: 'apiKey', label: 'Api Key', required: true, span: 12 }
      ],
      funasr: [
        { name: 'apiUrl', label: 'Websocket URL', required: true, span: 12, defaultUrl: "ws://127.0.0.1:10095" }
      ]
    }
  },
  tts: {
    label: '语音合成',
    typeOptions: [
      { label: 'Aliyun', value: 'aliyun', key: '0' },
      { label: 'Volcengine(doubao)', value: 'volcengine', key: '1' },
      { label: 'Xfyun', value: 'xfyun', key: '2' },
      { label: 'Minimax', value: 'minimax', key: '3' }
    ],
    typeFields: {
      aliyun: [
        { name: 'apiKey', label: 'API Key', required: true, span: 12 }
      ],
      volcengine: [
        { name: 'appId', label: 'App Id', required: true, span: 12 },
        { name: 'apiKey', label: 'Access Token', required: true, span: 12 }
      ],
      xfyun: [
        { name: 'appId', label: 'App Id', required: true, span: 12 },
        { name: 'apiSecret', label: 'Api Secret', required: true, span: 12 },
        { name: 'apiKey', label: 'Api Key', required: true, span: 12 }
      ],
      minimax: [
        { name: 'appId', label: 'Group Id', required: true, span: 12 },
        { name: 'apiKey', label: 'API Key', required: true, span: 12 }
      ],
    }
  }
};