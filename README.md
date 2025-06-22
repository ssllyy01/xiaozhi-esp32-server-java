<h1 align="center">Xiaozhi ESP32 Server Java</h1>

<p align="center">
  基于 <a href="https://github.com/78/xiaozhi-esp32">Xiaozhi ESP32</a> 项目开发的 Java 版本服务端，包含完整前后端管理平台<br/>
  为智能硬件设备提供强大的后端支持和直观的管理界面
</p>

<p align="center">
  <a href="https://github.com/joey-zhou/xiaozhi-esp32-server-java/issues">反馈问题</a>
  · <a href="#deployment">部署文档</a>
  · <a href="https://github.com/joey-zhou/xiaozhi-esp32-server-java/blob/main/CHANGELOG.md">更新日志</a>
</p>

<p align="center">
  <a href="https://github.com/joey-zhou/xiaozhi-esp32-server-java/graphs/contributors">
    <img alt="GitHub Contributors" src="https://img.shields.io/github/contributors/joey-zhou/xiaozhi-esp32-server-java?logo=github" />
  </a>
  <a href="https://github.com/joey-zhou/xiaozhi-esp32-server-java/issues">
    <img alt="Issues" src="https://img.shields.io/github/issues/joey-zhou/xiaozhi-esp32-server-java?color=0088ff" />
  </a>
  <a href="https://github.com/joey-zhou/xiaozhi-esp32-server-java/pulls">
    <img alt="GitHub pull requests" src="https://img.shields.io/github/issues-pr/joey-zhou/xiaozhi-esp32-server-java?color=0088ff" />
  </a>
  <a href="https://github.com/joey-zhou/xiaozhi-esp32-server-java/blob/main/LICENSE">
    <img alt="License" src="https://img.shields.io/badge/license-MIT-white?labelColor=black" />
  </a>
  <a href="https://github.com/joey-zhou/xiaozhi-esp32-server-java">
    <img alt="stars" src="https://img.shields.io/github/stars/joey-zhou/xiaozhi-esp32-server-java?color=ffcb47&labelColor=black" />
  </a>
</p>

---

## 项目简介 📝

Xiaozhi ESP32 Server Java 是基于 [Xiaozhi ESP32](https://github.com/78/xiaozhi-esp32) 项目开发的 Java 版本服务端，包含完整的前后端管理平台。该项目旨在为用户提供一个功能丰富、操作便捷的管理界面，帮助用户更好地管理设备、配置等。

考虑到企业级应用场景的需求，Java 作为一种成熟的企业级开发语言，具备更完善的生态系统支持和更强大的并发处理能力，因此我们选择开发这个 Java 版本的服务端，为项目提供更多可能性和扩展空间。

- **后端框架**：Spring Boot + Spring MVC
- **前端框架**：Vue.js + Ant Design
- **数据存储**：MySQL + Redis
- **全局响应式**：适配各种设备及分辨率

---

## 适用人群 👥

如果您已经购买了 ESP32 相关硬件，且希望通过一个功能完善、界面友好的管理平台来控制和管理您的设备，那么本项目非常适合您。特别适合：

- 需要企业级稳定性的用户
- 个人开发者，希望快速搭建使用的用户
- 希望有完整前端管理界面的用户
- 需要更强大数据管理和分析能力的用户
- 对系统扩展性有较高要求的用户
- 需要支持大量设备并发连接的场景
- 对实时数据处理有高要求的应用场景

---

## 功能模块 ✨(部分内容未开源，有需求请通过下方联系方式沟通)

### 已完成功能 ✅

| 功能模块 | 状态 | 描述 |
|---------|------|------|
| **设备管理** | ✅ | 查看已接入的所有设备列表、设备状态实时监控、添加/编辑/删除设备信息、设备绑定自动应用默认设置 |
| **音色选择** | ✅ | 提供多种音色模板、预览音色效果、为不同设备分配不同音色配置 |
| **音色克隆** | ✅ | 支持火山引擎与阿里云音色克隆，实现个性化声音定制 |
| **聊天记录** | ✅ | 查看历史聊天记录、按日期/关键词搜索聊天内容、删除消息、清空记忆功能 |
| **智能体** | ✅ | 对接Coze与Dify等智能体平台，实现复杂场景对话能力 |
| **角色切换** | ✅ | 预设角色切换（AI老师、男/女朋友、智能家居助手等）支持语音切换角色 |
| **持久化对话** | ✅ | 支持持久化对话记录，方便查看历史对话内容 |
| **LLM多平台支持** | ✅ | 支持OpenAI、智谱AI、讯飞星火、Ollama等多种大语言模型 |
| **默认配置管理** | ✅ | 支持设置默认配置，新绑定设备自动应用默认设置 |
| **IoT设备控制** | ✅ | 支持通过语音指令管理IoT设备，实现智能家居控制 |
| **智能功能调用** | ✅ | 支持音乐播放（音乐服务由第三方提供，仅供个人娱乐使用，本项目不承担任何版权责任）、角色切换等功能的智能调用
| **多语音识别服务** | ✅ | 支持Funasr、阿里、腾讯、Vosk等多种语音识别服务 |
| **双向流式交互** | ✅ | 支持实时语音输入和实时回复输出，提高对话流畅度 |
| **多模态交互** | ✅ | 支持图像识别和处理，实现更丰富的交互方式 |
| **Function Call** | ✅ | 支持LLM函数调用功能，实现复杂任务处理和智能决策 |
| **实时打断** | ✅ | 支持实时打断功能，提高对话流畅度 |
| **本地离线识别** | ✅ | 支持Vosk本地离线语音识别，无需联网即可使用 |
| **WebSocket通信** | ✅ | 高性能WebSocket通信，支持设备实时状态更新和控制 |
| **MQTT通信** | ✅ | 支持MQTT通信协议，长连接、服务端主动唤醒 |
| **自动语音唤醒** | ✅ | 支持自定义唤醒词唤醒，无需按键即可激活设备 |
| **多设备同时接入** | ✅ | 支持多设备同时接入，实现全屋语音覆盖 |
| **TTS多引擎支持** | ✅ | 支持微软、阿里、火山等多种TTS引擎 |
| **多用户支持** | ✅ | 支持多用户配置，满足家庭多成员使用需求 |
| **用户端** | ✅ | 原生卡片方式用户端设备管理页面，方便用户简洁配置 |


### 开发中功能 🚧

| 功能模块 | 状态 | 描述 |
|---------|------|------|
| **聊天数据可视化** | 🚧 | 聊天频率统计图表等数据可视化功能 |
| **混合模式角色** | 🚧 | 支持多角色混合模式，通过不同唤醒词唤醒不同角色（自动切换） |
| **记忆管理** | 🚧 | 自定义记忆对话条数、历史对话总结/摘要功能、手动操作对话记录 |
| **声纹识别** | 🚧 | 支持声纹识别功能，实现个性化语音助手 |
| **多语言支持** | 🚧 | 支持多语言界面，满足不同地区用户需求 |
| **Home Assistant** | 🚧 | 支持智能家居设备控制，通过语音指令管理Home Assistant设备 |
| **情感分析** | 🚧 | 通过语音情感分析，提供更人性化的回复 |
| **多设备协同** | 🚧 | 支持多设备协同工作，实现全屋覆盖的语音助手系统 |
| **自定义插件系统** | 🚧 | 支持自定义插件开发，扩展系统功能 |
| **知识库集成** | 🚧 | 支持接入外部知识库，增强问答能力 |
| **语音提醒与闹钟** | 🚧 | 支持设置语音提醒和闹钟功能 |
| **远程控制** | 🚧 | 支持远程控制设备，实现外出时的设备管理 |


---

## UI 展示 🎨

<div align="center">
    <img src="docs/images/device.jpg" alt="设备管理" width="600" style="margin: 10px;" />
    <p><strong>设备管理</strong> - 全面管理和监控所有连接设备</p>
</div>

<details>
  <summary style="cursor: pointer; font-size: 1.2em; color: #0366d6; text-align: center; display: block; margin: 20px 0; padding: 10px; background-color:rgb(48, 48, 48); border-radius: 5px;">
    <strong>👉 点击查看更多界面截图 👈</strong>
  </summary>
  
  <div align="center">
    <img src="docs/images/login.jpg" alt="登录界面" width="600" style="margin: 10px;" />
    <p><strong>登录界面</strong> - 安全访问系统的入口</p>
    <img src="docs/images/dashboard.jpg" alt="仪表盘" width="600" style="margin: 10px;" />
    <p><strong>仪表盘</strong> - 系统概览和关键数据展示</p>
    <img src="docs/images/user.jpg" alt="用户管理" width="600" style="margin: 10px;" />
    <p><strong>用户管理</strong> - 管理用户信息和权限</p>
    <img src="docs/images/message.jpg" alt="消息记录" width="600" style="margin: 10px;" />
    <p><strong>消息记录</strong> - 查看和搜索历史对话内容</p>
    <img src="docs/images/model.jpg" alt="模型管理" width="600" style="margin: 10px;" />
    <p><strong>模型管理</strong> - 配置和管理AI模型</p>
    <img src="docs/images/agent.jpg" alt="智能体管理" width="600" style="margin: 10px;" />
    <p><strong>智能体管理</strong> - 设置和切换智能体，Coze/Dify</p>
    <img src="docs/images/role.jpg" alt="角色管理" width="600" style="margin: 10px;" />
    <p><strong>角色管理</strong> - 设置和切换AI角色</p>
    <img src="docs/images/voiceClone.jpg" alt="音色克隆" width="600" style="margin: 10px;" />
    <p><strong>音色克隆</strong> - 克隆自己的声音，实现个性化语音助手</p>
  </div>
</details>

---

<a id="deployment"></a>
## 部署文档 📚

我们提供了多种部署方式，以满足不同用户的需求：

### 1. 本地源码运行（该文档基于旧版本JDK8编写，暂未更新，目前项目依赖JDK21）

- [Windows部署文档](./docs/WINDOWS_DEVELOPMENT.md) - 适合Windows环境开发和测试 - 由群友"汇合"提供
- [CentOS部署文档](./docs/CENTOS_DEVELOPMENT.md) - 适合Linux服务器环境部署 - 由群友"汇合"提供

成功运行后，控制台会输出 OTA 和 WebSocket 连接地址，根据固件编译文档使设备接入服务使用。

### 2. Docker部署

- [Docker部署文档](./docs/DOCKER.md) - 快速容器化部署方案 - 由群友"💍Mr_li"提供

成功启动后，WebSocket 连接需要通过宿主IP进行通信，例如：`ws://192.168.31.100:8091/ws/xiaozhi/v1/`

### 3. 视频教程

- [视频部署教程](https://doc.sivitacraft.com/article/xiaozhiai-javaserver/) - 由群友成员"苦瓜"录制

### 4. 固件编译

- [固件编译文档](./docs/FIRMWARE-BUILD.md) - 详细的固件编译和烧录过程

烧录成功且联网成功后，通过唤醒词唤醒小智，留意 server 端输出的控制台信息。

---

## 开发路线图 🗺️

根据我们的[项目开发需求清单](https://github.com/users/joey-zhou/projects/1)，未来我们计划实现以下功能：

### 近期计划 (2025 Q2)
- 完善Function Call功能，支持更多复杂任务处理
- 实现多角色混合模式，支持不同唤醒词唤醒不同角色
- 优化记忆管理系统，提供更灵活的历史对话管理
- 实现聊天数据可视化功能，提供数据分析能力

### 中期计划 (2025 Q3-Q4)
- 实现声纹识别功能，支持个性化语音助手
- 完善Home Assistant集成，提供更全面的智能家居控制能力
- 开发多模态交互功能，支持图像识别和处理
- 实现自定义插件系统，支持功能扩展

### 长期计划 (2026+)
- 开发多设备协同工作机制，实现全屋覆盖的语音助手系统
- 实现情感分析功能，提供更人性化的交互体验
- 开发知识库集成功能，增强问答能力
- 实现多用户支持，满足家庭多成员使用需求

我们将根据社区反馈和技术发展不断调整开发计划，确保项目持续满足用户需求。

---

## 贡献指南 👐

欢迎任何形式的贡献！如果您有好的想法或发现问题，请通过以下方式联系我们：

### 微信

一群已满，扫码加入二群

<img src="docs/images/wechat_group.jpg" alt="微信" width="200" />

### QQ

欢迎加入我们的QQ群一起交流讨论，QQ群号：790820705

<img src="./web/static/img/qq.jpg" alt="QQ群" width="200" />

### 定制开发

我们接受各种定制化开发项目，如果您有特定需求，欢迎通过微信联系洽谈。

<img src="./web/static/img/wechat.jpg" alt="微信" width="200" />

---

## 免责声明 ⚠️

本项目仅提供音乐和绘本播放的技术实现代码，不提供任何媒体内容。用户在使用相关功能时应确保拥有合法的使用权或版权许可，并遵守所在地区的版权法律法规。

项目中可能涉及的示例内容或资源均来自网络或由用户投稿提供，仅用于功能演示和技术测试。如有任何内容侵犯了您的权益，请立即联系我们，我们将在核实后立即采取删除等处理措施。

本项目开发者不对用户使用本项目代码获取或播放的任何内容承担法律责任。使用本项目即表示您同意自行承担使用过程中的全部法律风险和责任。

---

## Star History 📈

<a href="https://www.star-history.com/#joey-zhou/xiaozhi-esp32-server-java&Date">
 <picture>
   <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/svg?repos=joey-zhou/xiaozhi-esp32-server-java&type=Date&theme=dark" />
   <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/svg?repos=joey-zhou/xiaozhi-esp32-server-java&type=Date" />
   <img alt="Star History Chart" src="https://api.star-history.com/svg?repos=joey-zhou/xiaozhi-esp32-server-java&type=Date" />
 </picture>
</a>